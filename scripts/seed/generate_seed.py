#!/usr/bin/env python3
"""Generate seed SQL for 100K+ game item SKUs.

Produces a multi-database SQL script that populates:
  * game_item.items  (SPU) and game_item.skus (sellable variants)
  * game_inventory.stock (authoritative stock keyed by sku id)

The item/sku primary keys are explicit and stable so inventory stock rows can
reference them. Run against the docker-compose MySQL:

    python scripts/seed/generate_seed.py --skus 100000 -o scripts/db/seed-items.sql
    docker compose exec -T mysql mysql -uroot -proot < scripts/db/seed-items.sql
"""
from __future__ import annotations

import argparse
import random
from pathlib import Path

GAMES = {
    "CS2": ["knife-skin", "rifle-skin", "pistol-skin", "gloves", "sticker"],
    "Dota2": ["arcana", "immortal", "courier", "ward"],
    "Valorant": ["knife-skin", "rifle-skin", "bundle"],
    "LoL": ["skin", "chroma", "icon"],
    "Genshin": ["account", "top-up"],
}
WEARS = ["Factory New", "Minimal Wear", "Field-Tested", "Well-Worn", "Battle-Scarred"]
ADJECTIVES = ["Dragon", "Phantom", "Neon", "Crimson", "Frost", "Golden", "Shadow",
              "Vortex", "Inferno", "Azure", "Emerald", "Obsidian"]
NOUNS = ["Blade", "Fang", "Guardian", "Reaper", "Whisper", "Talon", "Ember",
         "Sentinel", "Howl", "Mirage", "Relic", "Crest"]

SELLER_COUNT = 50
BATCH = 1000


def rand_title() -> str:
    return f"{random.choice(ADJECTIVES)} {random.choice(NOUNS)}"


def generate(skus_target: int, out_path: Path) -> tuple[int, int]:
    item_rows: list[str] = []
    sku_rows: list[str] = []
    stock_rows: list[str] = []

    item_id = 0
    sku_id = 0
    while sku_id < skus_target:
        item_id += 1
        game = random.choice(list(GAMES.keys()))
        category = random.choice(GAMES[game])
        seller = random.randint(1, SELLER_COUNT)
        title = rand_title()
        n_skus = random.randint(3, 8)

        min_price = None
        pending_skus: list[str] = []
        pending_stock: list[str] = []
        for _ in range(n_skus):
            if sku_id >= skus_target:
                break
            sku_id += 1
            spec = random.choice(WEARS)
            price = round(random.uniform(5, 1500), 2)
            min_price = price if min_price is None else min(min_price, price)
            pending_skus.append(f"({sku_id},{item_id},'{spec}',{price})")
            total = random.randint(1, 200)
            pending_stock.append(f"({sku_id},{total},0,0)")

        item_rows.append(
            f"({item_id},{seller},'{title}','{game}','{category}',"
            f"'{game} {category}','ON_SHELF',{min_price},NOW())")
        sku_rows.extend(pending_skus)
        stock_rows.extend(pending_stock)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", encoding="utf-8") as f:
        f.write("-- Auto-generated seed data. Do not edit by hand.\n")
        f.write("SET autocommit=0;\n\n")

        f.write("USE game_item;\n")
        _write_batches(f, "INSERT INTO items (id,seller_id,title,game,category,"
                          "description,status,min_price,created_at) VALUES", item_rows)
        _write_batches(f, "INSERT INTO skus (id,item_id,spec,price) VALUES", sku_rows)

        f.write("\nUSE game_inventory;\n")
        _write_batches(f, "INSERT INTO stock (sku_id,total,frozen,version) VALUES", stock_rows)

        f.write("\nCOMMIT;\n")

    return item_id, sku_id


def _write_batches(f, prefix: str, rows: list[str]) -> None:
    for i in range(0, len(rows), BATCH):
        chunk = rows[i:i + BATCH]
        f.write(prefix + "\n")
        f.write(",\n".join(chunk))
        f.write(";\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate seed SQL for game item SKUs.")
    parser.add_argument("--skus", type=int, default=100_000, help="target number of SKUs")
    parser.add_argument("-o", "--output", default="scripts/db/seed-items.sql")
    parser.add_argument("--seed", type=int, default=42, help="RNG seed for reproducibility")
    args = parser.parse_args()

    random.seed(args.seed)
    items, skus = generate(args.skus, Path(args.output))
    print(f"Generated {items} items and {skus} SKUs -> {args.output}")


if __name__ == "__main__":
    main()
