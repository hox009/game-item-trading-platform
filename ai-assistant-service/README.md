# AI Trading Assistant (Python microservice)

A LangChain-based assistant that resolves catalog, pricing and order questions by
calling the Java platform's REST APIs through the gateway (function calling) and
grounding pricing answers in a knowledge base (RAG).

This is the only non-Java service in the platform. Keeping it as an independent
Python microservice lets us use the mature LangChain ecosystem while the business
services remain pure Spring Cloud.

## Design
- **Function calling**: business capabilities (search catalog, get item, get order,
  pricing guidance) are exposed as tools. The LLM chooses which to call.
- **RAG**: `data/knowledge.md` is embedded into a Chroma vector store (OpenAI
  embeddings). Pricing questions retrieve relevant guidance before answering.
- **Graceful degradation**: with no `OPENAI_API_KEY`, a deterministic intent
  planner routes queries to the same tools, so the service runs fully offline.

## Run locally
```bash
cd ai-assistant-service
python -m venv .venv && .venv\Scripts\activate      # Windows
pip install -r requirements.txt
copy .env.example .env                               # then edit as needed
uvicorn app.main:app --host 0.0.0.0 --port 8087
```

## Try it
```bash
curl -X POST http://localhost:8087/api/assistant/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"What is the status of order #7?\"}"
```

## Test
```bash
pytest -q
```
