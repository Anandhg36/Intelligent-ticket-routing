import numpy as np
from sentence_transformers import CrossEncoder


class SearchService:
    def __init__(self, embedding_service):
        self.embedding_service = embedding_service
        self.reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")

    def extract_exact_tokens(self, text):
        """
        Use the embedding service's NLP pipeline for token extraction.
        """
        return self.embedding_service.extract_exact_tokens(text)

    def search_weighted(self, query, top_k=3, window=2, alpha=0.7):
        """
        Perform a weighted search using embeddings, keywords, and exact token boosts.
        """
        # Get query embedding
        q_emb = self.embedding_service.model.encode([query])

        # FAISS search
        distances, indices = self.embedding_service.index.search(np.array(q_emb), top_k * 10)

        query_tokens = set(self.extract_exact_tokens(query))
        exact_words = query.split()

        scored_chunks = []
        added_indices = set()

        for idx, dist in zip(indices[0], distances[0]):
            if idx == -1:
                continue
            chunk = self.embedding_service.chunks[idx]
            chunk_tokens = set(self.embedding_service.normalize_for_exact_match(chunk["text"]))

            # Boost contribution from exact token matches
            current_boost_sum = sum(
                self.embedding_service.weights.get(t, 1.0) for t in query_tokens if t in chunk_tokens)
            normalized_boost = current_boost_sum / max(
                sum(self.embedding_service.weights.get(t, 1.0) for t in query_tokens), 1.0)

            # Semantic + keyword scoring
            semantic_score = 1 / (1 + dist)
            keyword_score = len(query_tokens & chunk_tokens) / max(1, len(query_tokens))
            total_score = (alpha * semantic_score) + ((1 - alpha) * keyword_score) + normalized_boost

            scored_chunks.append({
                "idx": idx,
                "semantic_score": semantic_score,
                "keyword_score": keyword_score,
                "exact_boost": normalized_boost,
                "total_score": total_score
            })
            added_indices.add(idx)

        # Include exact matches missed by FAISS
        for idx, chunk in enumerate(self.embedding_service.chunks):
            if idx in added_indices:
                continue
            if any(w in chunk["text"] for w in exact_words):
                chunk_tokens = set(self.embedding_service.normalize_for_exact_match(chunk["text"]))
                current_boost_sum = sum(
                    self.embedding_service.weights.get(t, 1.0) for t in query_tokens if t in chunk_tokens)
                normalized_boost = current_boost_sum / max(
                    sum(self.embedding_service.weights.get(t, 1.0) for t in query_tokens), 1.0)
                chunk_emb = self.embedding_service.model.encode([chunk["text"]])[0]
                semantic_score = np.dot(chunk_emb, q_emb[0]) / (np.linalg.norm(chunk_emb) * np.linalg.norm(q_emb[0]))
                keyword_score = len(query_tokens & chunk_tokens) / max(1, len(query_tokens))
                total_score = (alpha * semantic_score) + ((1 - alpha) * keyword_score) + normalized_boost

                scored_chunks.append({
                    "idx": idx,
                    "semantic_score": semantic_score,
                    "keyword_score": keyword_score,
                    "exact_boost": normalized_boost,
                    "total_score": total_score
                })

        # Sort top chunks
        scored_chunks = sorted(scored_chunks, key=lambda x: x["total_score"], reverse=True)
        print(f"\n--- Top {top_k} Chunks by Filtered Semantic Score ---")
        for sc in scored_chunks[:top_k]:
            idx = sc["idx"]
            print(f"\nPath: {self.chunks[idx]['path']}")
            print(f"Semantic Score: {sc['semantic_score']:.4f}")
            print(f"Keyword Score: {sc['keyword_score']:.4f}")
            print(f"Total Score: {sc['total_score']:.4f}")
            print(f"Text preview: {self.chunks[idx]['text'][:300]}...")
        # Expand results to include nearby sentences
        all_sentences = []
        for i, chunk in enumerate(self.embedding_service.chunks):
            for sent in self.embedding_service.safe_sentence_split(chunk["text"]):
                all_sentences.append({"sentence": sent, "chunk_idx": i, "path": chunk["path"]})

        results = []
        for sc in scored_chunks[:top_k]:
            idx = sc["idx"]
            positions = [i for i, s in enumerate(all_sentences) if s["chunk_idx"] == idx]
            if not positions:
                continue
            start = max(0, positions[0] - window)
            end = min(len(all_sentences), positions[-1] + window + 1)
            para = " ".join([all_sentences[i]["sentence"] for i in range(start, end)])
            results.append({
                "path": self.embedding_service.chunks[idx]["path"],
                "text": para,
                "score": sc.get("total_score"),
                "boost_contribution": sc.get("exact_boost")
            })

        return results
