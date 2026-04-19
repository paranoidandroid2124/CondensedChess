from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path
from typing import Any


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def emit(payload: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload) + "\n")
    sys.stdout.flush()


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Mock Codex runner for local orchestration tests.")
    parser.add_argument("--spec", required=True, help="Mock task spec JSON path.")
    parser.add_argument("--result-file", required=True, help="Where to write the final JSON result.")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    spec = load_json(Path(args.spec))
    result_path = Path(args.result_file)
    sleep_seconds = float(spec.get("sleep_seconds", 0.0))
    emit({"type": "thread.started", "thread_id": spec.get("thread_id", "mock-thread")})
    emit({"type": "turn.started"})
    if sleep_seconds > 0:
        emit(
            {
                "type": "item.started",
                "item": {
                    "id": "mock-sleep",
                    "type": "command_execution",
                    "status": "in_progress",
                },
            }
        )
        time.sleep(sleep_seconds)
        emit(
            {
                "type": "item.completed",
                "item": {
                    "id": "mock-sleep",
                    "type": "command_execution",
                    "status": "completed",
                },
            }
        )
    result = spec.get(
        "result",
        {
            "status": "completed",
            "summary": "Mock task completed.",
            "needs_human": False,
            "next_tasks": [],
            "artifacts": [],
        },
    )
    write_json(result_path, result)
    emit(
        {
            "type": "turn.completed",
            "usage": spec.get(
                "usage",
                {
                    "input_tokens": 1200,
                    "cached_input_tokens": 0,
                    "output_tokens": 180,
                },
            ),
        }
    )
    return int(spec.get("exit_code", 0))


if __name__ == "__main__":
    raise SystemExit(main())
