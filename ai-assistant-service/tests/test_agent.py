"""Offline tests for the deterministic fallback planner (no LLM, no network)."""
from __future__ import annotations

from app.agent import TradingAssistant
from app.rag import KnowledgeBase
from app.tools import ToolRegistry


class FakeGateway:
    """In-memory stand-in for the Java gateway."""

    def search_items(self, keyword=None, game=None, max_price=None, size=10):
        item = {"id": 1, "title": "AK-47 Redline", "game": "CS2", "minPrice": 42.5}
        if max_price is not None and item["minPrice"] > max_price:
            return []
        return [item]

    def get_item(self, item_id):
        return {"id": item_id, "sellerId": 2, "skus": [{"id": 1, "price": 42.5}]}

    def get_order(self, order_id):
        return {"id": order_id, "status": "PAID"}


def build_assistant() -> TradingAssistant:
    kb = KnowledgeBase(path="data/knowledge.md")
    tools = ToolRegistry(gateway=FakeGateway(), kb=kb)
    return TradingAssistant(tools=tools)


def test_order_status_query_is_resolved():
    result = build_assistant().chat("What is the status of order #7?")
    assert "get_order_status" in result.tools_used
    assert "PAID" in result.answer
    assert result.resolved_autonomously


def test_pricing_query_uses_rag():
    result = build_assistant().chat("What price is fair for a CS2 knife skin?")
    assert result.tools_used == ["pricing_guidance"]
    assert "knife" in result.answer.lower()
    assert result.resolved_autonomously


def test_search_query_with_price_filter():
    result = build_assistant().chat("Find me a CS2 Redline under 500")
    assert result.tools_used == ["search_items"]
    assert "1 matching" in result.answer
    assert result.resolved_autonomously


def test_search_returns_none_when_over_budget():
    result = build_assistant().chat("Find me a Redline under 10")
    assert result.tools_used == ["search_items"]
    assert not result.resolved_autonomously
