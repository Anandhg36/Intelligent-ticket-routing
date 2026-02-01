from fastapi import Depends, Request

def pdf_query(query: str, request: Request):
    return request.app.state.pdf_search_api.pdf_query(query)
