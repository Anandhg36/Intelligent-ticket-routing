import json
import os
import re
import string
import math
from collections import Counter, defaultdict

import numpy as np
import spacy
from loguru import logger
from spacy import Language
from sentence_transformers import SentenceTransformer, CrossEncoder
import faiss

DATA_DIR = "data/faiss"
os.makedirs(DATA_DIR, exist_ok=True)
class VCEmbedding:
    def __init__(self, model_name="all-mpnet-base-v2", sim_threshold=0.75):
        # Core models
        self.model = SentenceTransformer(model_name)
        self.reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")
        self.sim_threshold = sim_threshold
        self.index = None
        self.chunks = None
        self.weights = {}
        self.team_indices = {}  # team -> faiss index
        self.team_chunks = {}
        self.team_weights = {}

        # self.suggestion_service = suggestion_service

        # NLP pipeline
        self.nlp = spacy.load("en_core_web_sm")

        @Language.component("prevent_split_on_k8s_keys")
        def prevent_split_on_k8s_keys(doc):
            for token in doc[:-1]:
                if "." in token.text and doc[token.i + 1].is_lower:
                    doc[token.i + 1].is_sent_start = False
            return doc

        self.nlp.add_pipe("prevent_split_on_k8s_keys", before="parser")

    # =========================
    # TEXT CHUNKING & NORMALIZATION
    # =========================
    def safe_sentence_split(self, text):
        doc = self.nlp(text)
        return [sent.text.strip() for sent in doc.sents if sent.text.strip()]

    def normalize_http_codes(self, text):
        text = re.sub(
            r'returns a status code\s*\(HTTP\s*"Accepted"\)\s*.*?(\b202\b)',
            r'returns a 202 status code (HTTP "Accepted")',
            text,
            flags=re.IGNORECASE
        )
        return text

    def normalize_for_exact_match(self, text):
        text = text.replace("\n", " ").replace("\r", " ")
        allowed = string.ascii_letters + string.digits + ".-_"
        return "".join([c.lower() if c in allowed else " " for c in text]).split()

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

    def save_team_index(self, team):
        index_path = os.path.join(DATA_DIR, f"{team}.index")
        chunks_path = os.path.join(DATA_DIR, f"{team}_chunks.npy")

        faiss.write_index(self.team_indices[team], index_path)
        np.save(chunks_path, self.team_chunks[team], allow_pickle=True)

    def load_team_index(self, team):
        index_path = os.path.join(DATA_DIR, f"{team}.index")
        chunks_path = os.path.join(DATA_DIR, f"{team}_chunks.npy")

        if not os.path.exists(index_path) or not os.path.exists(chunks_path):
            return False

        self.team_indices[team] = faiss.read_index(index_path)
        self.team_chunks[team] = np.load(chunks_path, allow_pickle=True).tolist()
        return True

    def build_all_indexes(self, team_chunks_map):
        self.team_indices = {}
        self.team_chunks = {}

        for team, chunks in team_chunks_map.items():
            logger.info(f"Building FAISS index for team: {team}")
            self.build_vector_store(team, chunks)

    # =========================
    # TEAM-AWARE FAISS VECTOR STORE
    # =========================
    def build_vector_store(self, team, chunks):
        """
        Builds FAISS index for a single team.
        """
        dim = self.model.get_sentence_embedding_dimension()
        index = faiss.IndexFlatL2(dim)

        embeddings = self.model.encode([c["text"] for c in chunks], show_progress_bar=False)
        index.add(np.array(embeddings))

        self.team_indices[team] = index
        self.team_chunks[team] = chunks

    # =========================
    # WEIGHT CALCULATION
    # =========================
    def calculate_weights(self):
        if not self.team_chunks:
            print("No chunks available to calculate weights.")
            return

        self.team_weights = {}

        for team, chunks in self.team_chunks.items():
            chunk_word_sets = []

            for c in chunks:
                tokens = set(self.normalize_for_exact_match(c["text"]))
                chunk_word_sets.append(tokens)

            total_chunks = len(chunks)
            word_appearance_count = Counter()

            for word_set in chunk_word_sets:
                for word in word_set:
                    word_appearance_count[word] += 1

            self.team_weights[team] = {
                word: math.log(total_chunks / count)
                for word, count in word_appearance_count.items()
            }

    def save_team_weights(self, team):
        path = os.path.join(DATA_DIR, f"{team}_weights.json")
        with open(path, "w") as f:
            json.dump(self.team_weights[team], f)

    def load_team_weights(self, team):
        path = os.path.join(DATA_DIR, f"{team}_weights.json")
        if not os.path.exists(path):
            return False

        with open(path, "r") as f:
            self.team_weights[team] = json.load(f)
        return True

    def initialize_indexes(self, team_chunks_map):
        loaded_all = True

        for team in team_chunks_map:
            if not self.load_team_index(team) or not self.load_team_weights(team):
                loaded_all = False
                break

        if loaded_all:
            logger.info("Loaded FAISS indexes and weights from disk.")
            return

        logger.info("Indexes or weights missing. Rebuilding...")
        self.build_all_indexes(team_chunks_map)
        self.calculate_weights()

        for team in self.team_indices:
            self.save_team_index(team)
            self.save_team_weights(team)

    # =========================
    # TOKEN EXTRACTION
    # =========================
    def extract_exact_tokens(self, text):
        doc = self.nlp(text.lower())
        tokens = [t.text for t in doc if not t.is_stop and not t.is_punct and not t.is_space]
        return tokens

    def process_root_nodes(self, node_list):
        """Accepts a list of root_node bundles and generates the final chunks."""
        team_chunks = defaultdict(list)

        for item in node_list:
            pdf_chunks = self.collect_chunks(item["node"])

            for c in pdf_chunks:
                c["team"] = item["team"]
                c["pdf_path"] = item["path"]
                team_chunks[item["team"]].append(c)

        self.chunks = team_chunks
        return dict(team_chunks)

    def collect_chunks(self, node, path=None):
        if path is None:
            path = []

        chunks = []

        if node.title != "Root":
            path = path + [node.title]
            if node.content:
                full_text = " ".join(node.content)
                full_text = self.normalize_http_codes(full_text)
                for chunk_text in self.semantic_chunk_sentences(full_text):
                    chunks.append({
                        "path": " > ".join(path),
                        "text": chunk_text
                    })

        for child in node.children:
            chunks.extend(self.collect_chunks(child, path))

        return chunks

    def decide_auto_assign(self,teams, results):
        """
        Decide auto-assignment based on dominance rules
        """

        if not teams or not results:
            return False

        # Teams should already be sorted desc by confidence
        top_team = teams[0]
        second_team = teams[1] if len(teams) > 1 else None

        top_team_name = top_team["team"]
        top_team_conf = top_team["confidence"]

        # 1️⃣ Floor check (avoid garbage auto-assign)
        MIN_CONFIDENCE = 40.0
        if top_team_conf < MIN_CONFIDENCE:
            return False

        # 2️⃣ Dominance check
        if second_team:
            if top_team_conf < 1.5 * second_team["confidence"]:
                return False

        # 3️⃣ Top chunk ownership check
        top_chunk_team = results[0]["team"]
        if top_chunk_team != top_team_name:
            return False

        return True

    def search_weighted2(self, query, top_k=3, window=2, alpha=0.7):

        # =========================
        # STEP 1 — TEAM ROUTING (RERANK BASED)
        # =========================
        q_emb = self.model.encode([query])

        team_scores = []

        for team, index in self.team_indices.items():
            chunks = self.team_chunks[team]

            # Get top-N chunks for this team
            distances, indices = index.search(np.array(q_emb), min(5, len(chunks)))
            valid_idxs = [i for i in indices[0] if i != -1]

            if not valid_idxs:
                continue

            texts = [chunks[i]["text"] for i in valid_idxs]
            sentence_pairs = [[query, t] for t in texts]

            rerank_scores = self.reranker.predict(sentence_pairs)

            # Team score = avg of top 2 reranker scores
            top_scores = sorted(rerank_scores, reverse=True)[:2]
            team_score = float(sum(top_scores) / len(top_scores))

            team_scores.append((team, team_score))

        if not team_scores:
            return {"auto_assign": False, "teams": [], "results": []}

        team_scores.sort(key=lambda x: x[1], reverse=True)
        top_teams_raw = team_scores[:3]

        max_score = top_teams_raw[0][1]
        teams = []
        for team, score in top_teams_raw:
            confidence = (score / max_score) * 100
            confidence = max(5.0, round(confidence, 2))  # floor for UX
            teams.append({
                "team": team,
                "confidence": confidence
            })

        best_team = teams[0]["team"]

        # =========================
        # STEP 2 — SEARCH INSIDE TOP TEAM
        # =========================
        index = self.team_indices[best_team]
        chunks = self.team_chunks[best_team]

        distances, indices = index.search(np.array(q_emb), top_k * 15)

        query_tokens = set(self.extract_exact_tokens(query))
        exact_words = query.split()

        scored_chunks = []
        added_indices = set()

        # =========================
        # STEP 3 — HYBRID SCORING
        # =========================
        team_weights = self.team_weights.get(best_team, {})

        for idx, dist in zip(indices[0], distances[0]):
            if idx == -1:
                continue

            chunk_text = chunks[idx]["text"]
            chunk_tokens = set(self.normalize_for_exact_match(chunk_text))

            boost = sum(team_weights.get(t, 1.0) for t in query_tokens if t in chunk_tokens)
            boost /= max(sum(team_weights.get(t, 1.0) for t in query_tokens), 1.0)

            semantic = 1 / (1 + dist)
            keyword = len(query_tokens & chunk_tokens) / max(1, len(query_tokens))

            total = (alpha * semantic) + ((1 - alpha) * keyword) + boost

            scored_chunks.append({
                "idx": idx,
                "text": chunk_text,
                "total_score": total,
                "exact_boost": boost
            })
            added_indices.add(idx)

        # =========================
        # STEP 4 — RERANK CHUNKS
        # =========================
        scored_chunks = sorted(scored_chunks, key=lambda x: x["total_score"], reverse=True)[:top_k * 5]

        sentence_pairs = [[query, sc["text"]] for sc in scored_chunks]
        rerank_scores = self.reranker.predict(sentence_pairs)

        for i, r in enumerate(rerank_scores):
            scored_chunks[i]["final_score"] = (r * 0.7) + (scored_chunks[i]["total_score"] * 0.3)

        scored_chunks.sort(key=lambda x: x["final_score"], reverse=True)

        # =========================
        # STEP 5 — CONTEXT EXPANSION
        # =========================
        all_sentences = []
        for i, c in enumerate(chunks):
            for s in self.safe_sentence_split(c["text"]):
                all_sentences.append({"sentence": s, "chunk_idx": i, "path": c["path"]})

        results = []
        for sc in scored_chunks[:top_k]:
            idx = sc["idx"]
            positions = [i for i, s in enumerate(all_sentences) if s["chunk_idx"] == idx]
            if not positions:
                continue

            start = max(0, positions[0] - window)
            end = min(len(all_sentences), positions[-1] + window + 1)

            para = " ".join(all_sentences[i]["sentence"] for i in range(start, end))

            results.append({
                "path": chunks[idx]["path"],
                "team": best_team,
                "text": para,
                "score": float(sc["final_score"]),
                "boost_contribution": float(sc["exact_boost"])
            })

        # =========================
        # STEP 6 — AUTO ASSIGN
        # =========================
        auto_assign = self.decide_auto_assign(teams, results)

        return {
            "auto_assign": auto_assign,
            "teams": teams,
            "results": results
        }

        # top_chunk_text = results[0]["text"] if results else ""
        #
        # ai_suggested_message = self.suggestion_service.generate_suggestion(
        #     ticket_subject=query,
        #     top_chunk_text=top_chunk_text
        # )
        #
        # return {
        #     "results": results,
        #     "ai_suggested_message": ai_suggested_message
        # }
