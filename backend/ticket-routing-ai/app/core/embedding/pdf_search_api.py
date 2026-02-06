from urllib.parse import unquote

import numpy as np
from loguru import logger

from app.core.embedding.embedding import VCEmbedding


def to_python(obj):
    if isinstance(obj, np.generic):
        return obj.item()
    if isinstance(obj, dict):
        return {k: to_python(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [to_python(v) for v in obj]
    return obj

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
        cleaned_text = self.clean_query(text)
        response = self.vc.search_weighted2(cleaned_text, top_k=self.top_k)

        raw_results = response.get("results", [])
        teams = response.get("teams", [])
        auto_assign = response.get("auto_assign", False)

        clean_results = []
        for res in raw_results:
            clean_results.append({
                "path": res["path"],
                "team": res["team"],
                "text": res["text"],
                "score": float(res["score"]),
                "boost_contribution": float(res["boost_contribution"])
            })

        return to_python({
            "auto_assign": auto_assign,
            "teams": teams,
            "results": clean_results
        })


