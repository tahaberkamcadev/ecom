"""
Concurrent purchase / traffic simulator for the ecom backend.

Where demo_script.py walks through a *single* user's happy path, this script
generates a **burst of concurrent purchases** to exercise the system under load.
Every worker thread is parked on a threading.Barrier and released at the exact
same instant, so N purchase requests hit the API gateway simultaneously. This
surfaces real concurrency behavior: optimistic stock reservation, the
choreography saga, and (now) parallel payment processing across Kafka partitions.

Requirements:
  docker compose up -d --build            # the backend must be running locally
  python3 -m venv scripts/.venv && source scripts/.venv/bin/activate
  pip install -r scripts/requirements.txt

Usage:
  python scripts/concurrent_purchases.py                       # 20 simultaneous purchases
  python scripts/concurrent_purchases.py -n 50                 # 50 at once
  python scripts/concurrent_purchases.py -n 30 --rounds 3      # 3 synchronized bursts
  python scripts/concurrent_purchases.py --product "4K Monitor" -n 25   # hammer one SKU
  python scripts/concurrent_purchases.py --dry-run             # login + catalog + plan only

Heads-up on rate limiting: the gateway limits /api/products to ~100 requests /
60s per client IP. Keep (concurrency * rounds) under that, or start the stack
with RATE_LIMIT_ENABLED=false. HTTP 429 responses are reported, not fatal.
"""

from __future__ import annotations

import argparse
import statistics
import sys
import threading
import time
from dataclasses import dataclass

try:
    import requests
except ImportError:
    print(
        "Missing dependency: requests\n"
        "  python3 -m venv scripts/.venv && source scripts/.venv/bin/activate\n"
        "  pip install -r scripts/requirements.txt",
        file=sys.stderr,
    )
    sys.exit(1)

GATEWAY = "http://localhost:8080"
ADMIN_EMAIL = "admin@demo.local"
ADMIN_PASSWORD = "DemoAdmin1!"

BARRIER_TIMEOUT_SECONDS = 30.0


@dataclass
class RequestResult:
    worker: int
    round_no: int
    product: str
    quantity: int
    released_at: float = 0.0          # wall-clock instant the barrier released this worker
    status: int | None = None         # HTTP status, or None if the request never completed
    ok: bool = False                  # True for 2xx
    latency_ms: float = 0.0
    detail: str = ""                  # error text or short response body for non-2xx


# --------------------------------------------------------------------------- #
# Setup: authentication and catalog (done once, sequentially, before the burst)
# --------------------------------------------------------------------------- #

def login(base_url: str, email: str, password: str, timeout: float) -> str:
    response = requests.post(
        f"{base_url}/api/v1/auth/login",
        json={"email": email, "password": password},
        headers={"Accept": "application/json"},
        timeout=timeout,
    )
    response.raise_for_status()
    token = response.json()["access_token"]
    print(f"Login OK: {email}")
    return token


def load_catalog(base_url: str, token: str, timeout: float) -> list[tuple[str, str]]:
    """Return an ordered list of (product name, productId) across all categories."""
    catalog: list[tuple[str, str]] = []
    seen: set[str] = set()
    page = 0
    size = 100
    headers = {"Accept": "application/json", "Authorization": f"Bearer {token}"}

    while True:
        response = requests.get(
            f"{base_url}/api/catalog/products",
            params={"page": page, "size": size},
            headers=headers,
            timeout=timeout,
        )
        response.raise_for_status()
        payload = response.json()
        items = payload.get("items", [])
        for product in items:
            pid = product["productId"]
            if pid not in seen:
                seen.add(pid)
                catalog.append((product["name"], pid))

        total = int(payload.get("total", 0))
        page += 1
        if not items or page * size >= total:
            break

    if not catalog:
        raise RuntimeError("Catalog is empty. Is the stack up and the projection synced?")

    print(f"Loaded {len(catalog)} products from catalog")
    return catalog


# --------------------------------------------------------------------------- #
# Burst execution
# --------------------------------------------------------------------------- #

def build_tasks(
    catalog: list[tuple[str, str]],
    concurrency: int,
    product: str | None,
    quantity: int,
) -> list[tuple[dict, str]]:
    """One task per worker: ({"productId", "quantity"}, product_name)."""
    if product is not None:
        match = next((c for c in catalog if c[0] == product), None)
        if match is None:
            raise KeyError(f"Product not found in catalog: {product!r}")
        name, pid = match
        return [({"productId": pid, "quantity": quantity}, name) for _ in range(concurrency)]

    # No fixed product: spread purchases round-robin across the catalog so different
    # SKUs are hit; repeats (when concurrency > catalog size) create real contention.
    tasks: list[tuple[dict, str]] = []
    for i in range(concurrency):
        name, pid = catalog[i % len(catalog)]
        tasks.append(({"productId": pid, "quantity": quantity}, name))
    return tasks


def _worker(
    *,
    worker_id: int,
    round_no: int,
    url: str,
    token: str,
    item: dict,
    product_name: str,
    barrier: threading.Barrier,
    timeout: float,
    results: list[RequestResult],
    lock: threading.Lock,
) -> None:
    # A requests.Session is not thread-safe, so every worker gets its own. The token
    # is a shared, read-only string. Nothing here can raise before the barrier, which
    # guarantees all workers reach the release point.
    session = requests.Session()
    session.headers.update({"Accept": "application/json", "Authorization": f"Bearer {token}"})
    payload = {"items": [item]}
    result = RequestResult(
        worker=worker_id,
        round_no=round_no,
        product=product_name,
        quantity=item["quantity"],
    )

    try:
        # Park here until every worker has arrived, then all fire together.
        barrier.wait(timeout=BARRIER_TIMEOUT_SECONDS)
    except threading.BrokenBarrierError:
        result.detail = "barrier broken / timed out before release"
        with lock:
            results.append(result)
        session.close()
        return

    result.released_at = time.time()
    start = time.perf_counter()
    try:
        response = session.post(url, json=payload, timeout=timeout)
        result.latency_ms = (time.perf_counter() - start) * 1000.0
        result.status = response.status_code
        result.ok = response.ok
        if not response.ok:
            result.detail = _short_body(response)
    except requests.RequestException as exc:
        result.latency_ms = (time.perf_counter() - start) * 1000.0
        result.detail = f"{type(exc).__name__}: {exc}"
    finally:
        session.close()
        with lock:
            results.append(result)


def run_burst(
    url: str,
    token: str,
    tasks: list[tuple[dict, str]],
    round_no: int,
    timeout: float,
) -> list[RequestResult]:
    concurrency = len(tasks)
    barrier = threading.Barrier(concurrency)
    results: list[RequestResult] = []
    lock = threading.Lock()

    threads = [
        threading.Thread(
            target=_worker,
            name=f"buyer-r{round_no}-{worker_id}",
            kwargs=dict(
                worker_id=worker_id,
                round_no=round_no,
                url=url,
                token=token,
                item=item,
                product_name=name,
                barrier=barrier,
                timeout=timeout,
                results=results,
                lock=lock,
            ),
        )
        for worker_id, (item, name) in enumerate(tasks)
    ]

    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()

    return results


# --------------------------------------------------------------------------- #
# Reporting
# --------------------------------------------------------------------------- #

def _short_body(response: requests.Response, limit: int = 180) -> str:
    text = " ".join(response.text.split())
    return text[:limit] + ("…" if len(text) > limit else "")


def _percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (len(ordered) - 1) * pct
    low = int(rank)
    high = min(low + 1, len(ordered) - 1)
    return ordered[low] + (ordered[high] - ordered[low]) * (rank - low)


def print_summary(title: str, results: list[RequestResult], is_aggregate: bool = False) -> None:
    responded = [r for r in results if r.status is not None]
    released = [r.released_at for r in results if r.released_at > 0.0]
    latencies = [r.latency_ms for r in responded]
    accepted = sum(1 for r in results if r.ok)

    status_counts: dict[str, int] = {}
    for r in results:
        key = str(r.status) if r.status is not None else "ERR"
        status_counts[key] = status_counts.get(key, 0) + 1

    print(f"\n{title}")
    print("-" * len(title))
    print(f"  requests fired : {len(results)}")
    print(f"  accepted (2xx) : {accepted}")

    non_accepted = len(results) - accepted
    if non_accepted:
        breakdown = ", ".join(f"{code}×{count}" for code, count in sorted(status_counts.items()) if code != "202")
        print(f"  non-accepted   : {non_accepted}  ({breakdown})")

    # Barrier-release spread and single-window throughput only describe one burst; for an
    # aggregate across rounds they would fold in the inter-round pause, so they are skipped.
    if released and not is_aggregate:
        spread_ms = (max(released) - min(released)) * 1000.0
        print(f"  barrier release: {len(released)} workers within {spread_ms:.2f} ms (simultaneous)")

    if latencies:
        print(
            "  latency (ms)   : "
            f"min {min(latencies):.0f} | "
            f"p50 {_percentile(latencies, 0.50):.0f} | "
            f"p90 {_percentile(latencies, 0.90):.0f} | "
            f"p95 {_percentile(latencies, 0.95):.0f} | "
            f"max {max(latencies):.0f} | "
            f"mean {statistics.fmean(latencies):.0f}"
        )
        if not is_aggregate:
            ends = [r.released_at + r.latency_ms / 1000.0 for r in responded if r.released_at > 0.0]
            window_s = (max(ends) - min(released)) if (ends and released) else 0.0
            throughput = (len(responded) / window_s) if window_s > 0 else float(len(responded))
            print(f"  throughput     : {throughput:.1f} req/s (wall {window_s * 1000:.0f} ms)")

    failures = [r for r in results if not r.ok]
    if failures:
        print(f"  sample failures (first {min(5, len(failures))}):")
        for r in failures[:5]:
            code = r.status if r.status is not None else "ERR"
            print(f"    - worker {r.worker:>3} [{code}] {r.product} x{r.quantity}: {r.detail}")


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Fire N concurrent purchases at the ecom gateway using a release barrier.",
    )
    parser.add_argument("-n", "--concurrency", type=int, default=20,
                        help="simultaneous purchases per burst (default: 20)")
    parser.add_argument("--rounds", type=int, default=1,
                        help="number of synchronized bursts (default: 1)")
    parser.add_argument("-q", "--quantity", type=int, default=1,
                        help="units per purchase (default: 1)")
    parser.add_argument("--product", default=None,
                        help="buy this exact product name in every request (contention demo)")
    parser.add_argument("--sleep", type=float, default=1.0,
                        help="seconds to wait between rounds (default: 1.0)")
    parser.add_argument("--base-url", default=GATEWAY, help="gateway URL")
    parser.add_argument("--email", default=ADMIN_EMAIL)
    parser.add_argument("--password", default=ADMIN_PASSWORD)
    parser.add_argument("--timeout", type=float, default=30.0, help="per-request timeout (s)")
    parser.add_argument("--dry-run", action="store_true",
                        help="login + catalog + print the plan, but send no purchases")
    args = parser.parse_args()

    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")
    if args.rounds < 1:
        parser.error("--rounds must be >= 1")

    base_url = args.base_url.rstrip("/")
    total_requests = args.concurrency * args.rounds

    print(f"Gateway     : {base_url}")
    print(f"Plan        : {args.concurrency} concurrent × {args.rounds} round(s) = {total_requests} purchases")
    print(f"Quantity    : {args.quantity} per purchase")
    print(f"Target      : {args.product if args.product else 'round-robin across catalog'}")
    if total_requests > 100:
        print("WARNING: >100 total requests may hit the gateway rate limit (HTTP 429). "
              "Run the stack with RATE_LIMIT_ENABLED=false for heavy load.")

    try:
        token = login(base_url, args.email, args.password, args.timeout)
        catalog = load_catalog(base_url, token, args.timeout)
        tasks = build_tasks(catalog, args.concurrency, args.product, args.quantity)

        if args.dry_run:
            distribution: dict[str, int] = {}
            for _, name in tasks:
                distribution[name] = distribution.get(name, 0) + 1
            print("\n[dry-run] per-burst product distribution:")
            for name, count in sorted(distribution.items(), key=lambda kv: (-kv[1], kv[0])):
                print(f"  - {name} x{args.quantity}  ×{count} workers")
            print("\n[dry-run] no purchases sent.")
            return

        url = f"{base_url}/api/products/purchase"
        all_results: list[RequestResult] = []
        for round_no in range(1, args.rounds + 1):
            results = run_burst(url, token, tasks, round_no, args.timeout)
            all_results.extend(results)
            print_summary(f"Round {round_no}/{args.rounds}", results)
            if round_no < args.rounds and args.sleep > 0:
                time.sleep(args.sleep)

        if args.rounds > 1:
            print_summary("GRAND TOTAL", all_results, is_aggregate=True)

        print("\nDone.")

    except requests.ConnectionError as exc:
        print(f"Connection error: {exc}", file=sys.stderr)
        print("Is the stack running? → docker compose up -d --build", file=sys.stderr)
        sys.exit(1)
    except requests.HTTPError as exc:
        body = exc.response.text if exc.response is not None else ""
        print(f"HTTP error during setup: {exc}", file=sys.stderr)
        if body:
            print(body, file=sys.stderr)
        sys.exit(1)
    except (KeyError, RuntimeError) as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
