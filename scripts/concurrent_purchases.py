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
  python scripts/concurrent_purchases.py --all-stock           # one request per seed stock unit
  python scripts/concurrent_purchases.py --dry-run             # login + catalog + plan only

Heads-up on rate limiting: the gateway limits /api/products to ~100 requests /
60s per client IP. Keep (concurrency * rounds) under that, or start the stack
with RATE_LIMIT_ENABLED=false. HTTP 429 responses are reported, not fatal.

Large bursts (hundreds of workers) open one TCP socket each. If you see
Errno 24 / "Too many open files", raise the process limit first:
  ulimit -n 4096

--all-stock mirrors inventory-service DevDataSeeder stock levels (fresh stack assumed).
"""

from __future__ import annotations

import argparse
import statistics
import sys
import threading
import time
from dataclasses import dataclass

try:
    import resource  # POSIX only — unavailable on Windows
except ImportError:
    resource = None  # type: ignore[assignment]

try:
    import requests
    from requests.adapters import HTTPAdapter
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

# Keep in sync with inventory-service DevDataSeeder.PRODUCT_SEEDS stock values.
SEED_STOCKS: dict[str, int] = {
    "Wireless Headphones": 40,
    "Mechanical Keyboard": 35,
    "4K Monitor": 20,
    "Classic Hoodie": 60,
    "Running Shoes": 45,
    "Ceramic Coffee Mug": 100,
    "Clean Code": 25,
    "Designing Data-Intensive Applications": 30,
    "Yoga Mat": 50,
    "Moisturizing Face Cream": 80,
}


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
        raise RuntimeError(
            "Catalog is empty (projection has no products yet).\n"
            "  1) Is the stack up? → docker compose ps\n"
            "  2) Inventory may have seeded while Debezium connectors were missing.\n"
            "     Re-register connectors, then retry in a few seconds:\n"
            "       docker compose run --rm connect-init\n"
            "  3) Fresh start: docker compose down -v && docker compose up -d --build"
        )

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
    all_stock: bool = False,
) -> list[tuple[dict, str]]:
    """One task per worker: ({"productId", "quantity"}, product_name)."""
    if all_stock:
        by_name = {name: pid for name, pid in catalog}
        missing = sorted(name for name in SEED_STOCKS if name not in by_name)
        if missing:
            raise KeyError(f"Seed products missing from catalog: {missing}")
        # One qty=1 purchase per seed stock unit → drains full seed inventory in one burst.
        tasks: list[tuple[dict, str]] = []
        for name, stock in SEED_STOCKS.items():
            pid = by_name[name]
            tasks.extend((({"productId": pid, "quantity": 1}, name) for _ in range(stock)))
        return tasks

    if product is not None:
        match = next((c for c in catalog if c[0] == product), None)
        if match is None:
            raise KeyError(f"Product not found in catalog: {product!r}")
        name, pid = match
        return [({"productId": pid, "quantity": quantity}, name) for _ in range(concurrency)]

    # No fixed product: spread purchases round-robin across the catalog so different
    # SKUs are hit; repeats (when concurrency > catalog size) create real contention.
    tasks = []
    for i in range(concurrency):
        name, pid = catalog[i % len(catalog)]
        tasks.append(({"productId": pid, "quantity": quantity}, name))
    return tasks


def _nofile_soft_limit() -> int | None:
    """POSIX RLIMIT_NOFILE soft limit, or None when unavailable (e.g. Windows)."""
    if resource is None:
        return None
    soft, _hard = resource.getrlimit(resource.RLIMIT_NOFILE)
    return soft


def ensure_open_file_budget(concurrency: int) -> None:
    """Fail fast when the OS file-descriptor soft limit cannot host the burst.

    Each worker opens one TCP socket (plus a few FDs for the interpreter / libs).
    Hitting the limit shows up as Errno 24 'Too many open files' on the client —
    not as a backend failure. Skipped on Windows (no ``resource`` module).
    """
    soft = _nofile_soft_limit()
    if soft is None:
        return
    # Leave headroom for the interpreter, loaded libs, and Docker Desktop sockets.
    needed = concurrency + 256
    if soft >= needed:
        return
    raise RuntimeError(
        f"Open-file soft limit is {soft}, but this burst needs about {needed} FDs "
        f"({concurrency} workers + headroom). Raise it in this shell, then retry:\n"
        f"  ulimit -n 4096\n"
        f"Current failure mode would be client-side ConnectionError / Errno 24, "
        f"not an API/gateway problem."
    )


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
    # Prepare the immutable request bits before the barrier. Do NOT open a Session
    # yet — creating hundreds of sessions early wastes FDs and can trip ulimit
    # before a single purchase is sent. Token is shared read-only.
    headers = {"Accept": "application/json", "Authorization": f"Bearer {token}"}
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
        return

    result.released_at = time.time()
    start = time.perf_counter()
    session = requests.Session()
    # One connection per worker is enough for a single POST; default pool sizes
    # multiply FD usage under large -n bursts for no benefit.
    session.mount("http://", HTTPAdapter(pool_connections=1, pool_maxsize=1))
    session.mount("https://", HTTPAdapter(pool_connections=1, pool_maxsize=1))
    try:
        response = session.post(url, json=payload, headers=headers, timeout=timeout)
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
        if any("Too many open files" in r.detail or "Errno 24" in r.detail for r in failures):
            print(
                "  tip: client hit the OS open-file limit (not a backend 5xx). "
                "Run `ulimit -n 4096` in this shell and retry."
            )


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Fire N concurrent purchases at the ecom gateway using a release barrier.",
    )
    parser.add_argument("-n", "--concurrency", type=int, default=20,
                        help="simultaneous purchases per burst (default: 20; ignored with --all-stock)")
    parser.add_argument("--rounds", type=int, default=1,
                        help="number of synchronized bursts (default: 1)")
    parser.add_argument("-q", "--quantity", type=int, default=1,
                        help="units per purchase (default: 1; ignored with --all-stock)")
    parser.add_argument("--product", default=None,
                        help="buy this exact product name in every request (contention demo)")
    parser.add_argument("--all-stock", action="store_true",
                        help="buy every seed stock unit at once (1 qty=1 request per unit)")
    parser.add_argument("--sleep", type=float, default=1.0,
                        help="seconds to wait between rounds (default: 1.0)")
    parser.add_argument("--base-url", default=GATEWAY, help="gateway URL")
    parser.add_argument("--email", default=ADMIN_EMAIL)
    parser.add_argument("--password", default=ADMIN_PASSWORD)
    parser.add_argument("--timeout", type=float, default=30.0, help="per-request timeout (s)")
    parser.add_argument("--dry-run", action="store_true",
                        help="login + catalog + print the plan, but send no purchases")
    args = parser.parse_args()

    if args.all_stock and args.product is not None:
        parser.error("--all-stock and --product cannot be used together")
    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")
    if args.rounds < 1:
        parser.error("--rounds must be >= 1")

    base_url = args.base_url.rstrip("/")
    seed_units = sum(SEED_STOCKS.values())
    concurrency = seed_units if args.all_stock else args.concurrency
    quantity = 1 if args.all_stock else args.quantity
    total_requests = concurrency * args.rounds

    print(f"Gateway     : {base_url}")
    print(f"Plan        : {concurrency} concurrent × {args.rounds} round(s) = {total_requests} purchases")
    print(f"Quantity    : {quantity} per purchase")
    if args.all_stock:
        print(f"Target      : all seed stock ({seed_units} units across {len(SEED_STOCKS)} SKUs)")
    else:
        print(f"Target      : {args.product if args.product else 'round-robin across catalog'}")
    if total_requests > 100:
        print("WARNING: >100 total requests may hit the gateway rate limit (HTTP 429). "
              "Run the stack with RATE_LIMIT_ENABLED=false for heavy load.")
    print(f"Open files  : soft ulimit {_nofile_soft_limit()} (need ~{concurrency + 256} for this burst)")

    try:
        token = login(base_url, args.email, args.password, args.timeout)
        catalog = load_catalog(base_url, token, args.timeout)
        tasks = build_tasks(catalog, args.concurrency, args.product, args.quantity, args.all_stock)

        if args.dry_run:
            distribution: dict[str, int] = {}
            for _, name in tasks:
                distribution[name] = distribution.get(name, 0) + 1
            print("\n[dry-run] per-burst product distribution:")
            for name, count in sorted(distribution.items(), key=lambda kv: (-kv[1], kv[0])):
                print(f"  - {name} x{quantity}  ×{count} workers")
            print("\n[dry-run] no purchases sent.")
            return

        ensure_open_file_budget(len(tasks))
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
