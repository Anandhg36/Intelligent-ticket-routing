from fastapi import APIRouter
from app.router import pdf_search

api_router = APIRouter()

api_router.include_router(pdf_search.pdf_search)
