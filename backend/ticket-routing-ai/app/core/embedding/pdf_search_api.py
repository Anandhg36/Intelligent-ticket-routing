from urllib.parse import unquote

from loguru import logger

from app.core.embedding.embedding import VCEmbedding


class PDFSearchAPI:
    def __init__(self, vc: VCEmbedding, top_k=3):
        self.vc = vc
        self.top_k = top_k

    def clean_query(self,text) -> str:
        prev = text
        logger.debug(f"Previous search text: {prev}")
        for _ in range(2):  # decode at most twice (safe)
            decoded = unquote(prev)
            if decoded == prev:
                break
            prev = decoded
        return prev.strip()

    def pdf_query(self, text):
        """
        Runs a query against the vector store
        Returns top_k results
        """
        results = self.vc.search_weighted2(text, top_k=self.top_k)

        # Convert numpy.float32 to float
        clean_results = []
        for res in results:
            clean_res = {
                "path": res["path"],
                "team": res["team"],
                "text": res["text"],
                "score": float(res["score"]),
                "boost_contribution": float(res["boost_contribution"])
            }
            clean_results.append(clean_res)

        return clean_results

