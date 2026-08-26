"""FastAPI entrypoint for the AI trading assistant service."""
from __future__ import annotations

from fastapi import FastAPI
from pydantic import BaseModel

from app.agent import TradingAssistant
from app.config import settings

app = FastAPI(title="Game Item AI Trading Assistant", version="1.0.0")
assistant = TradingAssistant()


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    answer: str
    tools_used: list[str]
    resolved_autonomously: bool
    mode: str


@app.get("/actuator/health")
def health() -> dict[str, str]:
    """Health endpoint mirroring the Java services for uniform monitoring."""
    return {"status": "UP", "llm": "enabled" if settings.llm_enabled else "fallback"}


@app.post("/api/assistant/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    result = assistant.chat(request.message)
    return ChatResponse(
        answer=result.answer,
        tools_used=result.tools_used,
        resolved_autonomously=result.resolved_autonomously,
        mode=result.mode,
    )
