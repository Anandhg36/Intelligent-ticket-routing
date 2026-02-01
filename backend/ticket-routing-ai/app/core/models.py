from pydantic import BaseModel


class DocumentNode:
    def __init__(self, title="", level=0):
        self.title = title
        self.level = level
        self.content = []
        self.children = []

    def add_child(self, node):
        self.children.append(node)



class QueryRequest(BaseModel):
    text: str

class QueryResult(BaseModel):
    path: str
    score: float
    boost_contribution: float
    text: str
