"""Send simultaneous assigned-seat orders from a JMeter user CSV."""

import argparse
import concurrent.futures
import csv
import json
import statistics
import threading
import time
import urllib.error
import urllib.request
import uuid
from collections import Counter
from pathlib import Path


def request_json(url, method="GET", body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = token
    request = urllib.request.Request(
        url, data=json.dumps(body).encode() if body is not None else None,
        headers=headers, method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--users", type=Path, default=Path("perf-output/jmeter-users.csv"))
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--session-id", required=True)
    parser.add_argument("--seat-id", required=True)
    parser.add_argument("--output", type=Path, default=Path("perf-output/assigned-seat-result.json"))
    args = parser.parse_args()
    with args.users.open(newline="", encoding="utf-8") as file:
        users = list(csv.DictReader(file))
    if not users:
        parser.error("User CSV is empty")
    base = args.base_url.rstrip("/")
    seats_url = f"{base}/api/v1/sessions/{args.session_id}/seats"
    _, before = request_json(seats_url)
    seat_before = next((seat for seat in before["data"] if seat["id"] == args.seat_id), None)
    if not seat_before or seat_before["status"] != "FREE":
        parser.error("Target seat must exist and be FREE before the test")

    barrier = threading.Barrier(len(users))
    run_id = uuid.uuid4().hex[:10]

    def buy(index_user):
        index, user = index_user
        body = {"requestId": f"seat-load-{run_id}-{index:03d}",
                "sessionId": args.session_id, "seatIds": [args.seat_id]}
        barrier.wait(timeout=30)
        started = time.perf_counter()
        try:
            http_status, payload = request_json(
                f"{base}/api/v1/orders/assigned", "POST", body, user["token"]
            )
            return {"index": index, "httpStatus": http_status,
                    "code": payload.get("code"),
                    "orderId": (payload.get("data") or {}).get("id"),
                    "elapsedMs": round((time.perf_counter() - started) * 1000, 2)}
        except Exception as error:
            return {"index": index, "code": "CLIENT_ERROR",
                    "error": type(error).__name__,
                    "elapsedMs": round((time.perf_counter() - started) * 1000, 2)}

    with concurrent.futures.ThreadPoolExecutor(max_workers=len(users)) as pool:
        results = list(pool.map(buy, enumerate(users, 1)))
    _, after = request_json(seats_url)
    seat_after = next(seat for seat in after["data"] if seat["id"] == args.seat_id)
    durations = sorted(result["elapsedMs"] for result in results)
    report = {
        "sessionId": args.session_id, "seatId": args.seat_id,
        "userCount": len(users), "seatBefore": seat_before["status"],
        "seatAfter": seat_after["status"], "seatVersionAfter": seat_after["version"],
        "codes": dict(Counter(result["code"] for result in results)),
        "httpStatuses": dict(Counter(str(result.get("httpStatus")) for result in results)),
        "successfulOrderIds": [result["orderId"] for result in results if result["code"] == "OK"],
        "meanMs": round(statistics.mean(durations), 2),
        "p95Ms": durations[min(len(durations) - 1, int(len(durations) * .95))],
        "maxMs": durations[-1], "results": results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "results"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
