"""Agent orchestration.

Two execution paths share the same tool layer:

* **LLM path** (when an OpenAI key is configured): the model decides which tools
  to call via function calling, we execute them, feed results back, and let the
  model compose the final answer — a classic autonomous agent loop.
* **Fallback path** (offline): a deterministic intent planner routes the query to
  the right tool. This keeps the service demoable without any LLM credentials and
  provides a reproducible baseline.
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from typing import Any

from app.config import settings
from app.tools import ToolRegistry


@dataclass
class AgentResult:
    answer: str
    tools_used: list[str] = field(default_factory=list)
    resolved_autonomously: bool = True
    mode: str = "fallback"


SYSTEM_PROMPT = (
    "You are the trading assistant for a game item marketplace. "
    "Use the provided tools to answer catalog, pricing and order questions. "
    "Prefer calling tools over guessing. Be concise and cite concrete items/prices."
)


class TradingAssistant:
    def __init__(self, tools: ToolRegistry | None = None) -> None:
        self.tools = tools or ToolRegistry()

    def chat(self, message: str) -> AgentResult:
        if settings.llm_enabled:
            try:
                return self._chat_llm(message)
            except Exception:  # noqa: BLE001 - degrade to fallback on any LLM/tool error
                pass
        return self._chat_fallback(message)

    # --- LLM function-calling loop ---

    def _chat_llm(self, message: str, max_iters: int = 4) -> AgentResult:
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url or None,
            temperature=0,
        ).bind_tools([{"type": "function", "function": s} for s in self.tools.specs()])

        history: list[Any] = [SystemMessage(content=SYSTEM_PROMPT), HumanMessage(content=message)]
        used: list[str] = []

        for _ in range(max_iters):
            ai: AIMessage = llm.invoke(history)
            history.append(ai)
            if not ai.tool_calls:
                return AgentResult(answer=ai.content or "", tools_used=used,
                                   resolved_autonomously=bool(used), mode="llm")
            for call in ai.tool_calls:
                used.append(call["name"])
                try:
                    result = self.tools.dispatch(call["name"], call.get("args", {}))
                    content = json.dumps(result, default=str)
                except Exception as exc:  # noqa: BLE001
                    content = f"error: {exc}"
                history.append(ToolMessage(content=content, tool_call_id=call["id"]))

        return AgentResult(answer="I need more information to fully resolve this.",
                           tools_used=used, resolved_autonomously=False, mode="llm")

    # --- Deterministic fallback planner ---

    def _chat_fallback(self, message: str) -> AgentResult:
        text = message.lower()

        order_id = self._extract_order_id(text)
        if order_id is not None:
            data = self.tools.get_order_status(order_id)
            status = data.get("status") if isinstance(data, dict) else None
            answer = (f"Order #{order_id} is currently '{status}'." if status
                      else f"I couldn't find order #{order_id}.")
            return AgentResult(answer=answer, tools_used=["get_order_status"],
                               resolved_autonomously=status is not None)

        if any(kw in text for kw in ("price", "worth", "pricing", "价", "值", "定价")):
            guidance = self.tools.pricing_guidance(message)
            return AgentResult(answer=guidance, tools_used=["pricing_guidance"],
                               resolved_autonomously=guidance != "No pricing guidance found.")

        max_price = self._extract_max_price(text)
        keyword = self._extract_keyword(message)
        items = self.tools.search_items(keyword=keyword, max_price=max_price)
        count = len(items) if isinstance(items, list) else (
            len(items.get("content", [])) if isinstance(items, dict) else 0)
        answer = f"Found {count} matching item(s)." if count else "No matching items found."
        return AgentResult(answer=answer, tools_used=["search_items"],
                           resolved_autonomously=count > 0)

    @staticmethod
    def _extract_order_id(text: str) -> int | None:
        m = re.search(r"(?:order|订单)\s*#?(\d+)", text)
        return int(m.group(1)) if m else None

    @staticmethod
    def _extract_max_price(text: str) -> float | None:
        m = re.search(r"(?:under|below|<|以内|不超过|低于)\s*\$?(\d+(?:\.\d+)?)", text)
        return float(m.group(1)) if m else None

    @staticmethod
    def _extract_keyword(message: str) -> str | None:
        stop = {"find", "search", "show", "me", "a", "the", "for", "please", "under",
                "below", "with", "some", "any", "buy", "want", "looking", "i", "to"}
        tokens = [t for t in re.findall(r"[A-Za-z0-9]+", message)
                  if t.lower() not in stop and not t.isdigit()]
        return " ".join(tokens[:3]) if tokens else None
