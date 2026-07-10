"""
Requirements:
  pip install -r scripts/requirements.txt
  docker compose up -d --build

Usage:
  python scripts/demo_script.py
  python scripts/demo_script.py --dry-run
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from dataclasses import dataclass

import requests

GATEWAY = "http://localhost:8080"
ADMIN_EMAIL = "admin@demo.local"
ADMIN_PASSWORD = "DemoAdmin1!"


BULK_ORDER = [
    ("Wireless Headphones", 8),
    ("Mechanical Keyboard", 6),
    ("4K Monitor", 4),
    ("Classic Hoodie", 15),
    ("Running Shoes", 10),
    ("Ceramic Coffee Mug", 25),
    ("Clean Code", 8),
    ("Designing Data-Intensive Applications", 8),
    ("Yoga Mat", 10),
    ("Moisturizing Face Cream", 20),
]


SMALL_ORDERS = [
    ("Ceramic Coffee Mug", 1),
    ("Clean Code", 1),
    ("Moisturizing Face Cream", 1),
    ("Mechanical Keyboard", 1),
    ("Yoga Mat", 1),
]


@dataclass(frozen=True)
class OrderLine:
    name: str
    quantity: int


class EcomClient:
    """Login, catalog and purchase from API gateway."""

    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.http = requests.Session()
        self.http.headers["Accept"] = "application/json"

    def login(self, email: str, password: str) -> None:
        response = self.http.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password},
            timeout=30,
        )
        response.raise_for_status()
        token = response.json()["access_token"]
        self.http.headers["Authorization"] = f"Bearer {token}"
        print(f"Giriş OK: {email}")

    def load_catalog(self) -> dict[str, str]:
        """Product name → productId map (Postgres catalog list, all categories)."""
        catalog: dict[str, str] = {}
        page = 0
        size = 100

        while True:
            response = self.http.get(
                f"{self.base_url}/api/catalog/products",
                params={"page": page, "size": size},
                timeout=30,
            )
            response.raise_for_status()
            payload = response.json()
            for product in payload.get("items", []):
                catalog[product["name"]] = product["productId"]

            total = int(payload.get("total", 0))
            page += 1
            if page * size >= total or not payload.get("items"):
                break

        if not catalog:
            raise RuntimeError("Catalog is empty. Is the stack running? Is the projection syncing?")

        print(f"Loaded {len(catalog)} products from catalog")
        return catalog

    def checkout(self, lines: list[OrderLine], catalog: dict[str, str]) -> dict:
        payload = [self._to_item(line, catalog) for line in lines]
        response = self.http.post(
            f"{self.base_url}/api/products/checkout",
            json=payload,
            timeout=30,
        )
        response.raise_for_status()
        print(f"  checkout → {response.status_code}")
        return response.json()

    def purchase(self, lines: list[OrderLine], catalog: dict[str, str]) -> dict:
        payload = {"items": [self._to_item(line, catalog) for line in lines]}
        response = self.http.post(
            f"{self.base_url}/api/products/purchase",
            json=payload,
            timeout=30,
        )
        response.raise_for_status()
        print(f"  purchase → {response.status_code}")
        return response.json()

    @staticmethod
    def _to_item(line: OrderLine, catalog: dict[str, str]) -> dict:
        if line.name not in catalog:
            raise KeyError(f"Product not found in catalog: {line.name!r}")
        return {"productId": catalog[line.name], "quantity": line.quantity}


def lines_from_tuples(pairs: list[tuple[str, int]]) -> list[OrderLine]:
    return [OrderLine(name, qty) for name, qty in pairs]


def ensure_seed_products_exist(catalog: dict[str, str]) -> None:
    needed = {line.name for line in lines_from_tuples(BULK_ORDER + SMALL_ORDERS)}
    missing = sorted(needed - catalog.keys())
    if missing:
        raise RuntimeError(
            "Expected demo products not found in catalog: "
            + ", ".join(missing)
            + ". For clean seed: docker compose down -v && docker compose up -d --build"
        )


def pretty_print(label: str, data: object) -> None:
    print(f"  {label}:")
    print(json.dumps(data, indent=2, ensure_ascii=False))


def run_bulk_purchase(client: EcomClient, catalog: dict[str, str]) -> None:
    lines = lines_from_tuples(BULK_ORDER)
    print(f"\n=== Bulk order ({len(lines)} items) ===")

    quote = client.checkout(lines, catalog)
    pretty_print("checkout", quote)

    if not quote.get("readyToPurchase", True):
        print("  WARNING: Stock/price is not suitable — purchase may fail")

    result = client.purchase(lines, catalog)
    pretty_print("purchase", result)


def run_small_purchases(
    client: EcomClient,
    catalog: dict[str, str],
    pause_seconds: float,
) -> None:
    lines = lines_from_tuples(SMALL_ORDERS)
    print(f"\n=== Small orders ({len(lines)} items) ===")

    for index, line in enumerate(lines, start=1):
        print(f"\nSmall #{index}: {line.name} x{line.quantity}")
        result = client.purchase([line], catalog)
        pretty_print("purchase", result)

        if index < len(lines) and pause_seconds > 0:
            time.sleep(pause_seconds)


def main() -> None:
    parser = argparse.ArgumentParser(description="Bulk + small order demo script")
    parser.add_argument("--base-url", default=GATEWAY, help="Gateway URL")
    parser.add_argument("--email", default=ADMIN_EMAIL)
    parser.add_argument("--password", default=ADMIN_PASSWORD)
    parser.add_argument("--dry-run", action="store_true", help="Only login + catalog")
    parser.add_argument("--skip-bulk", action="store_true", help="Skip bulk, run small orders")
    parser.add_argument("--sleep-between", type=float, default=2.0, help="Small orders sleep between (s)")
    parser.add_argument("--pause-after-bulk", type=float, default=3.0, help="Bulk after sleep (s)")
    args = parser.parse_args()

    print(f"Gateway: {args.base_url}")

    try:
        client = EcomClient(args.base_url)
        client.login(args.email, args.password)
        catalog = client.load_catalog()
        ensure_seed_products_exist(catalog)

        if args.dry_run:
            print("\n[dry-run] Bulk:")
            for name, qty in BULK_ORDER:
                print(f"  - {name} x{qty}")
            print("\n[dry-run] Small orders:")
            for name, qty in SMALL_ORDERS:
                print(f"  - {name} x{qty}")
            return

        if not args.skip_bulk:
            run_bulk_purchase(client, catalog)
            if args.pause_after_bulk > 0:
                print(f"\n{args.pause_after_bulk}s waiting (next)…")
                time.sleep(args.pause_after_bulk)

        run_small_purchases(client, catalog, args.sleep_between)
        print("\nDone.")

    except requests.ConnectionError as exc:
        print(f"Connection error: {exc}", file=sys.stderr)
        print("Is the stack running? → docker compose up -d --build", file=sys.stderr)
        sys.exit(1)
    except requests.HTTPError as exc:
        body = exc.response.text if exc.response is not None else ""
        print(f"HTTP error: {exc}", file=sys.stderr)
        if body:
            print(body, file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
