class AISuggestionService:
    def __init__(self, llm_client):
        self.llm_client = llm_client

    def generate_suggestion(self, ticket_subject: str, top_chunk_text: str) -> str:
        prompt = f"""
You are a Kubernetes support engineer.

Using ONLY the documentation below, suggest what the user could try next.

Rules:
- Be concise (3–5 sentences max)
- Use technical language
- Do not invent steps
- Do not reference external knowledge
- Phrase it as a suggestion, not a final answer

Ticket:
{ticket_subject}

Documentation:
{top_chunk_text}
"""

        return self.llm_client.generate(prompt)
