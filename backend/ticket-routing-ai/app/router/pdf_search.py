from fastapi import APIRouter
from app.core.embedding.pdf_search_api_service import pdf_query
pdf_search = APIRouter(prefix="/pdf_search" ,tags=["PdfSearch"])

pdf_search.add_api_route("/query",pdf_query,methods=["GET"],summary="Get results")