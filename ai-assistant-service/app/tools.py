"""Tool layer exposed to the LLM as callable functions (function calling).

Each tool is a small, deterministic capability grounded in the platform's own
APIs or knowledge base — the LLM decides *when* to call them, but the tools
themselves are code, keeping answers trustworthy.
"""
from __future__ import annotations

from typing import Any, Callable

from app.gateway_client import GatewayClient
from app.rag import KnowledgeBase


class ToolRegistry:
    def __init__(self, gateway: GatewayClient | None = None, kb: KnowledgeBase | None = None) -> None:
        self.gateway = gateway or GatewayClient()
        self.kb = kb or KnowledgeBase()

    # --- Tool implementations ---

    def search_items(self, keyword: str | None = None, game: str | None = None,
                     max_price: float | None = None) -> Any:
        """Search the catalog by keyword, game and/or maximum price."""
        return self.gateway.search_items(keyword=keyword, game=game, max_price=max_price)

    def get_item(self, item_id: int) -> Any:
        """Fetch full detail (SKUs and prices) for a single item by id."""
        return self.gateway.get_item(int(item_id))

    def get_order_status(self, order_id: int) -> Any:
        """Look up the current status of an order by id."""
        return self.gateway.get_order(int(order_id))

    def pricing_guidance(self, query: str) -> str:
        """Retrieve pricing/catalog guidance from the knowledge base (RAG)."""
        chunks = self.kb.retrieve(query, k=3)
        return "\n\n".join(chunks) if chunks else "No pricing guidance found."

    # --- Schema advertised to the LLM ---

    def specs(self) -> list[dict[str, Any]]:
        return [
            {
                "name": "search_items",
                "description": "Search the game item catalog by keyword, game and/or maximum price.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "keyword": {"type": "string", "description": "free-text search term"},
                        "game": {"type": "string", "description": "game name, e.g. CS2"},
                        "max_price": {"type": "number", "description": "maximum price filter"},
                    },
                },
            },
            {
                "name": "get_item",
                "description": "Get full detail and SKU prices for a specific item id.",
                "parameters": {
                    "type": "object",
                    "properties": {"item_id": {"type": "integer"}},
                    "required": ["item_id"],
                },
            },
            {
                "name": "get_order_status",
                "description": "Get the current status of an order by id.",
                "parameters": {
                    "type": "object",
                    "properties": {"order_id": {"type": "integer"}},
                    "required": ["order_id"],
                },
            },
            {
                "name": "pricing_guidance",
                "description": "Retrieve pricing/catalog guidance from the knowledge base.",
                "parameters": {
                    "type": "object",
                    "properties": {"query": {"type": "string"}},
                    "required": ["query"],
                },
            },
        ]

    def dispatch(self, name: str, arguments: dict[str, Any]) -> Any:
        func: Callable[..., Any] | None = {
            "search_items": self.search_items,
            "get_item": self.get_item,
            "get_order_status": self.get_order_status,
            "pricing_guidance": self.pricing_guidance,
        }.get(name)
        if func is None:
            raise ValueError(f"unknown tool: {name}")
        return func(**arguments)
