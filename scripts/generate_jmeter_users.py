"""Register load-test users and write one JMeter CSV row per account."""

import argparse
import csv
import json
import secrets
import time
import urllib.error
import urllib.request
from pathlib import Path


def post_json(base_url: str, path: str, body: dict) -> dict:
    request = urllib.request.Request(
        base_url.rstrip("/") + path,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = json.load(response)
    except urllib.error.HTTPError as error:
        payload = json.load(error)
        raise RuntimeError(f"{path}: {payload.get('code')}: {payload.get('message')}") from error
    if payload.get("code") != "OK":
        raise RuntimeError(f"{path}: {payload.get('code')}: {payload.get('message')}")
    return payload["data"]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--tier-id", required=True)
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--output", type=Path, default=Path("perf-output/jmeter-users.csv"))
    args = parser.parse_args()
    if args.count < 1:
        parser.error("--count must be positive")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Refuse to overwrite a prior run's credentials or request IDs.
    with args.output.open("x", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(["phone", "token", "requestId", "tierId", "quantity"])
        file.flush()
        run_id = f"{int(time.time())}-{secrets.token_hex(4)}"
        for index in range(args.count):
            for _ in range(10):
                phone = "19" + "".join(str(secrets.randbelow(10)) for _ in range(9))
                password = secrets.token_urlsafe(18)
                try:
                    post_json(args.base_url, "/api/v1/auth/register", {"phone": phone, "password": password})
                    break
                except RuntimeError as error:
                    if "ALREADY" not in str(error) and "EXISTS" not in str(error):
                        raise
            else:
                raise RuntimeError("Could not generate a unique phone number")
            login = post_json(args.base_url, "/api/v1/auth/login", {"phone": phone, "password": password})
            writer.writerow([phone, login["accessToken"], f"load-{run_id}-{index + 1:03d}", args.tier_id, 1])
            file.flush()
            print(f"Created {index + 1}/{args.count}", end="\r", flush=True)
    print(f"\nWrote {args.count} rows to {args.output}")


if __name__ == "__main__":
    main()
