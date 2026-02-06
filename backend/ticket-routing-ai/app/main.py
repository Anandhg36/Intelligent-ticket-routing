from __future__ import annotations

from fastapi import FastAPI
from contextlib import asynccontextmanager
from loguru import logger

from app.core.embedding.ai_suggestion_service import AISuggestionService
from app.core.embedding.llm_client import LLMClient
from app.core.embedding.pdf_search_api import PDFSearchAPI
from app.core.ingestion.pdf_parser import PDFParseService
from app.core.embedding.embedding import VCEmbedding
from app.core.embedding.search_service import SearchService
from app.router.api import api_router

pdf_service: PDFParseService | None = None
embedding_service: VCEmbedding | None = None
search_service: SearchService | None = None
pdf_service_api : PDFSearchAPI | None = None
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifecycle manager.
    Runs once at startup and once at shutdown.
    """
    global pdf_service, embedding_service, search_service

    logger.info("Initializing PDF parser service...")
    pdf_service = PDFParseService()

    logger.info("Initializing embedding service...")
    # llm_client = LLMClient()
    # ai_suggestion_service = AISuggestionService(llm_client)

    embedding_service = VCEmbedding()

    # logger.info("Initializing search service...")
    # search_service = SearchService(embedding_service,ai_suggestion_service)

    pdf_folder_path = "/Users/anandh/Documents/Kubernetes"
    logger.info(f"Parsing PDFs from folder: {pdf_folder_path}")

    root_node_list = pdf_service.collect_nodes_from_teams(pdf_folder_path)

    logger.info("Building FAISS vector store...")
    team_chunks_map = embedding_service.process_root_nodes(root_node_list)
    embedding_service.initialize_indexes(team_chunks_map)
    embedding_service.initialize_indexes(team_chunks_map)


    logger.info("Bootstrap complete. Search service ready!")
    app.state.pdf_search_api = PDFSearchAPI(embedding_service)

    app.include_router(api_router)

    yield

    logger.info("Shutting down application...")


app = FastAPI(
    title="Intelligent Ticket Routing API",
    lifespan=lifespan
)


@app.get("/health")
def health():
    return {"status": "ok"}
