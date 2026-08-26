"""Retrieval-Augmented Generation over a small pricing/catalog knowledge base.

When an OpenAI key is available the knowledge is embedded into a Chroma vector
store for semantic retrieval. Without a key it falls back to a dependency-free
keyword-overlap retriever so the service still answers offline.
"""
from __future__ import annotations

import re
from pathlib import Path

from app.config import settings


def _load_chunks(path: str) -> list[str]:
    file = Path(path)
    if not file.exists():
        return []
    text = file.read_text(encoding="utf-8")
    # Split on blank lines into paragraph-sized chunks.
    chunks = [c.strip() for c in re.split(r"\n\s*\n", text) if c.strip()]
    return chunks


class KnowledgeBase:
    def __init__(self, path: str | None = None) -> None:
        self._chunks = _load_chunks(path or settings.knowledge_path)
        self._vector_store = None
        if settings.llm_enabled and self._chunks:
            self._vector_store = self._build_vector_store()

    def _build_vector_store(self):
        # Imported lazily so the offline fallback needs no heavy dependencies.
        from langchain_community.vectorstores import Chroma
        from langchain_openai import OpenAIEmbeddings

        embeddings = OpenAIEmbeddings(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url or None,
        )
        return Chroma.from_texts(self._chunks, embedding=embeddings)

    def retrieve(self, query: str, k: int = 3) -> list[str]:
        if not self._chunks:
            return []
        if self._vector_store is not None:
            docs = self._vector_store.similarity_search(query, k=k)
            return [d.page_content for d in docs]
        return self._keyword_retrieve(query, k)

    def _keyword_retrieve(self, query: str, k: int) -> list[str]:
        terms = {t for t in re.findall(r"\w+", query.lower()) if len(t) > 2}
        scored: list[tuple[int, str]] = []
        for chunk in self._chunks:
            chunk_terms = set(re.findall(r"\w+", chunk.lower()))
            score = len(terms & chunk_terms)
            if score:
                scored.append((score, chunk))
        scored.sort(key=lambda x: x[0], reverse=True)
        return [chunk for _, chunk in scored[:k]]
