import concurrent
import logging
import os
import re
from concurrent.futures import ThreadPoolExecutor

import numpy as np
import pdfplumber
from loguru import logger

from app.core.models import DocumentNode
logging.getLogger("pdfminer").setLevel(logging.ERROR)
class PDFParseService:
    def parse_pdf_structure(self, file_path):
        root = DocumentNode(title="Root", level=0)
        stack = [root]

        with pdfplumber.open(file_path) as pdf:
            for page in pdf.pages:
                text = page.extract_text()
                if not text:
                    continue

                lines = [l.strip() for l in text.split("\n") if l.strip()]
                for line in lines:
                    level = self.get_heading_level(line)
                    if level > 0:
                        node = DocumentNode(title=line, level=level)
                        while stack and stack[-1].level >= level:
                            stack.pop()
                        stack[-1].add_child(node)
                        stack.append(node)
                    else:
                        stack[-1].content.append(line)

        return root

    def process_single_pdf(self, pdf_info):
        """Helper for parallel execution"""
        pdf_path, team_name = pdf_info
        print(f"Processing PDF: {pdf_path}")
        try:
            root_node = self.parse_pdf_structure(pdf_path)
            return {
                "node": root_node,
                "team": team_name,
                "path": pdf_path
            }
        except Exception as e:
            logger.error(f"Error parsing {pdf_path}: {e}")
            return None

    def collect_nodes_from_teams(self, base_folder):
        """Uses a ThreadPool to parse multiple PDFs at once."""
        tasks = []
        for team_name in os.listdir(base_folder):
            team_path = os.path.join(base_folder, team_name)
            if not os.path.isdir(team_path): continue

            for root, _, files in os.walk(team_path):
                for file in files:
                    if file.lower().endswith(".pdf"):
                        tasks.append((os.path.join(root, file), team_name))

        node_list = []
        with ThreadPoolExecutor(max_workers=os.cpu_count()) as executor:
            results = list(executor.map(self.process_single_pdf, tasks))

        return [r for r in results if r is not None]


    def get_heading_level(self, text):
        if text.strip().isdigit() or re.match(r"^\d{1,2}/\d{1,2}/\d{4}", text):
            return 0

        match = re.match(r"^(\d+(\.\d+)*)", text)
        if match:
            stripped = text.strip()
            if len(stripped) <= 3:
                return 0
            return len(match.group(1).split("."))
        return 0

    def normalize_http_codes(self, text):
        text = re.sub(
            r'returns a status code\s*\(HTTP\s*"Accepted"\)\s*.*?(\b202\b)',
            r'returns a 202 status code (HTTP "Accepted")',
            text,
            flags=re.IGNORECASE
        )
        return text

    def semantic_chunk_sentences(self, text):
        text = text.replace("\u200b", "").replace("\n", " ")
        text = re.sub(r"\s+", " ", text)
        sentences = self.safe_sentence_split(text)
        if not sentences:
            return []

        chunks = []
        current_chunk = [sentences[0]]
        current_emb = self.model.encode([sentences[0]])[0]

        for sentence in sentences[1:]:
            sent_emb = self.model.encode([sentence])[0]
            sim = np.dot(current_emb, sent_emb) / (np.linalg.norm(current_emb) * np.linalg.norm(sent_emb))
            if sim >= self.sim_threshold:
                current_chunk.append(sentence)
                current_emb = np.mean([self.model.encode([s])[0] for s in current_chunk], axis=0)
            else:
                chunks.append(" ".join(current_chunk))
                current_chunk = [sentence]
                current_emb = sent_emb

        if current_chunk:
            chunks.append(" ".join(current_chunk))

        return chunks