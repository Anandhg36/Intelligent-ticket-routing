import re
import string
import math
from collections import Counter

import numpy as np
import spacy
from spacy import Language
from sentence_transformers import SentenceTransformer, CrossEncoder
import faiss


class VCEmbedding:
    def __init__(self, model_name="all-mpnet-base-v2", sim_threshold=0.75):
        # Core models
        self.model = SentenceTransformer(model_name)
        self.reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")
        self.sim_threshold = sim_threshold
        self.index = None
        self.chunks = None
        self.weights = {}
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

    # =========================
    # FAISS VECTOR STORE
    # =========================
    def build_vector_store(self, chunks):
        texts = [c["text"] for c in chunks]
        embeddings = self.model.encode(texts, show_progress_bar=True)
        dim = embeddings.shape[1]
        self.index = faiss.IndexFlatL2(dim)
        self.index.add(np.array(embeddings))
        self.chunks = chunks
        return self.index

    # =========================
    # WEIGHT CALCULATION
    # =========================
    def calculate_weights(self):
        if not self.chunks:
            print("No chunks available to calculate weights.")
            return

        chunk_word_sets = []
        for c in self.chunks:
            tokens = set(self.normalize_for_exact_match(c["text"]))
            chunk_word_sets.append(tokens)

        total_chunks = len(self.chunks)
        word_appearance_count = Counter()
        for word_set in chunk_word_sets:
            for word in word_set:
                word_appearance_count[word] += 1

        self.weights = {word: math.log(total_chunks / count) for word, count in word_appearance_count.items()}

    # =========================
    # TOKEN EXTRACTION
    # =========================
    def extract_exact_tokens(self, text):
        doc = self.nlp(text.lower())
        tokens = [t.text for t in doc if not t.is_stop and not t.is_punct and not t.is_space]
        return tokens

    def process_root_nodes(self, node_list):
        """Accepts a list of root_node bundles and generates the final chunks."""
        all_chunks = []

        for item in node_list:
            # Call the chunking logic on the passed node
            pdf_chunks = self.collect_chunks(item["node"])

            # Re-attach the metadata to each individual chunk
            for c in pdf_chunks:
                c["team"] = item["team"]
                c["pdf_path"] = item["path"]

            all_chunks.extend(pdf_chunks)

        self.chunks = all_chunks
        return all_chunks

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

    def search_weighted2(self, query, top_k=3, window=2, alpha=0.7):
        q_emb = self.model.encode([query])
        distances, indices = self.index.search(np.array(q_emb), top_k * 15)
        query_tokens = set(self.extract_exact_tokens(query))
        exact_words = query.split()

        scored_chunks = []
        added_indices = set()

        # Hybrid scoring loop
        for idx, dist in zip(indices[0], distances[0]):
            if idx == -1: continue
            chunk_text = self.chunks[idx]["text"]
            chunk_tokens = set(self.normalize_for_exact_match(chunk_text))
            current_boost_sum = sum(self.weights.get(t, 1.0) for t in query_tokens if t in chunk_tokens)
            normalized_boost = current_boost_sum / max(sum(self.weights.get(t, 1.0) for t in query_tokens), 1.0)
            semantic_score = 1 / (1 + dist)
            keyword_score = len(query_tokens & chunk_tokens) / max(1, len(query_tokens))
            total_score = (alpha * semantic_score) + ((1 - alpha) * keyword_score) + normalized_boost
            scored_chunks.append({
                "idx": idx,
                "text": chunk_text,  # Added for reranker
                "total_score": total_score,
                "semantic_score": semantic_score,
                "keyword_score": keyword_score,
                "exact_boost": normalized_boost
            })
            added_indices.add(idx)

        # Force-include exact matches
        for idx, c in enumerate(self.chunks):
            if idx in added_indices: continue
            if any(w in c["text"] for w in exact_words):
                chunk_tokens = set(self.normalize_for_exact_match(c["text"]))
                current_boost_sum = sum(self.weights.get(t, 1.0) for t in query_tokens if t in chunk_tokens)
                normalized_boost = current_boost_sum / max(sum(self.weights.get(t, 1.0) for t in query_tokens), 1.0)
                chunk_emb = self.model.encode([c["text"]])[0]
                semantic_score = np.dot(chunk_emb, q_emb[0]) / (np.linalg.norm(chunk_emb) * np.linalg.norm(q_emb[0]))
                keyword_score = len(query_tokens & chunk_tokens) / max(1, len(query_tokens))
                total_score = (alpha * semantic_score) + ((1 - alpha) * keyword_score) + normalized_boost
                scored_chunks.append({
                    "idx": idx,
                    "text": c["text"],  # Added for reranker
                    "total_score": total_score,
                    "semantic_score": semantic_score,
                    "keyword_score": keyword_score,
                    "exact_boost": normalized_boost
                })

        if scored_chunks:
            # Sort by total_score first to get the best candidates for reranking
            scored_chunks = sorted(scored_chunks, key=lambda x: x["total_score"], reverse=True)[:top_k * 5]

            # Use Cross-Encoder to rerank the top candidates
            sentence_pairs = [[query, sc["text"]] for sc in scored_chunks]
            rerank_scores = self.reranker.predict(sentence_pairs)

            for i, r_score in enumerate(rerank_scores):
                hybrid_score = scored_chunks[i]["total_score"]

                # Fusion Formula: 70% Reranker + 30% Hybrid
                # We normalize the rerank_score slightly if it's very large/small
                scored_chunks[i]["rerank_score"] = r_score
                scored_chunks[i]["final_blend"] = (r_score * 0.7) + (hybrid_score * 0.3)

            # Sort by rerank_score
            scored_chunks = sorted(scored_chunks, key=lambda x: x["final_blend"], reverse=True)

        print(f"\n--- Top {top_k} Chunks after Reranking ---")
        for sc in scored_chunks[:top_k]:
            idx = sc["idx"]
            print(f"\nPath: {self.chunks[idx]['path']}")
            print(f"Rerank Score: {sc.get('rerank_score', 0):.4f}")
            print(f"Hybrid Score: {sc['total_score']:.4f}")
            print(f"Text preview: {self.chunks[idx]['text'][:300]}...")

        # Context expansion logic
        all_sentences = []
        for i, c in enumerate(self.chunks):
            sents = self.safe_sentence_split(c["text"])
            for s in sents:
                all_sentences.append({"sentence": s, "chunk_idx": i, "path": c["path"]})

        results = []
        for sc in scored_chunks[:top_k]:
            idx = sc["idx"]
            positions = [i for i, s in enumerate(all_sentences) if s["chunk_idx"] == idx]
            if not positions: continue
            start = max(0, positions[0] - window)
            end = min(len(all_sentences), positions[-1] + window + 1)
            para = " ".join([all_sentences[i]["sentence"] for i in range(start, end)])
            results.append({
                "path": self.chunks[idx]["path"],
                "team": self.chunks[idx].get("team"),
                "text": para,
                "score": float(sc.get("rerank_score", sc["total_score"])),
                "boost_contribution": sc.get("exact_boost")
            })

        return results

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
