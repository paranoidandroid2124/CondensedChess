from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import webbrowser
from datetime import datetime
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlparse

from orchestrator import (
    build_state_summary,
    launch_background_run,
    load_json,
    resolve_state_path,
    stop_background_run,
)


REPO_ROOT = Path(__file__).resolve().parents[2]
TOOL_DIR = Path(__file__).resolve().parent
DEFAULT_RUNS_ROOT = REPO_ROOT / "tmp"
DEFAULT_WORKFLOW = TOOL_DIR / "workflows" / "u_primary_implementation.json"
DEFAULT_HTML = TOOL_DIR / "dashboard.html"


class DashboardError(RuntimeError):
    pass


def ensure_relative_to(path: Path, root: Path, label: str) -> Path:
    resolved = path.resolve()
    root_resolved = root.resolve()
    try:
        resolved.relative_to(root_resolved)
    except ValueError as exc:
        raise DashboardError(f"{label} must stay under {root_resolved}") from exc
    return resolved


def resolve_state_dir(raw_value: str, runs_root: Path) -> Path:
    candidate = Path(raw_value)
    if not candidate.is_absolute():
        candidate = runs_root / candidate
    return ensure_relative_to(candidate, runs_root, "state_dir")


def resolve_workflow_path(raw_value: str, repo_root: Path) -> Path:
    candidate = Path(raw_value)
    if not candidate.is_absolute():
        candidate = repo_root / candidate
    resolved = ensure_relative_to(candidate, repo_root, "workflow_path")
    if not resolved.exists():
        raise DashboardError(f"Workflow path does not exist: {resolved}")
    return resolved


def discover_state_dirs(runs_root: Path) -> list[Path]:
    if not runs_root.exists():
        return []
    state_dirs: list[Path] = []
    for child in runs_root.iterdir():
        if child.is_dir() and (child / "state.json").exists():
            state_dirs.append(child.resolve())
    state_dirs.sort(key=lambda item: item.stat().st_mtime, reverse=True)
    return state_dirs


def generate_state_dir(runs_root: Path, workflow_path: Path) -> Path:
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    return runs_root / f"{workflow_path.stem}_run_{timestamp}"


def build_task_details(task_id: str, task_state: dict[str, Any]) -> dict[str, Any]:
    history = task_state.get("history", [])
    latest_history = history[-1] if history else {}
    last_result = task_state.get("last_result", {})
    summary = (
        last_result.get("summary")
        or latest_history.get("summary")
        or task_state.get("last_error")
        or ""
    )
    return {
        "task_id": task_id,
        "status": task_state.get("status", "not_started"),
        "attempts": task_state.get("attempts", 0),
        "queue_reason": task_state.get("queue_reason"),
        "last_started_at": task_state.get("last_started_at"),
        "last_finished_at": task_state.get("last_finished_at"),
        "summary": summary,
        "last_error": task_state.get("last_error"),
        "current_attempt": task_state.get("current_attempt"),
        "stdout_path": task_state.get("current_stdout_path")
        or latest_history.get("stdout_path"),
        "stderr_path": task_state.get("current_stderr_path")
        or latest_history.get("stderr_path"),
        "result_path": task_state.get("current_result_path")
        or latest_history.get("result_path"),
        "block_context_path": task_state.get("block_context_path"),
        "history_count": len(history),
    }


def build_run_details(state_dir: Path) -> dict[str, Any]:
    state_path = resolve_state_path(state_dir)
    if not state_path.exists():
        raise DashboardError(f"State file does not exist: {state_path}")
    summary = build_state_summary(state_path)
    state = load_json(state_path)
    workflow_path_raw = state.get("workflow_path")
    ordered_task_ids: list[str] = []
    if isinstance(workflow_path_raw, str):
        workflow_path = Path(workflow_path_raw)
        if workflow_path.exists():
            workflow = load_json(workflow_path)
            metadata = workflow.get("metadata", {})
            sequence = metadata.get("sequence", [])
            if isinstance(sequence, list):
                ordered_task_ids.extend(
                    task_id for task_id in sequence if isinstance(task_id, str) and task_id
                )
            tasks = workflow.get("tasks", {})
            if isinstance(tasks, dict):
                for task_id in tasks:
                    if task_id not in ordered_task_ids:
                        ordered_task_ids.append(task_id)
    for task_id in state.get("tasks", {}):
        if task_id not in ordered_task_ids:
            ordered_task_ids.append(task_id)
    details = []
    for task_id in ordered_task_ids:
        task_state = state.get("tasks", {}).get(task_id, {})
        details.append(build_task_details(task_id, task_state))
    return {
        "summary": summary,
        "queue": state.get("queue", []),
        "running_tasks": state.get("running_tasks", []),
        "active_block_context_path": state.get("active_block_context_path"),
        "block_context_history": state.get("block_context_history", []),
        "tasks": details,
    }


def tail_text(path: Path, max_lines: int) -> str:
    if not path.exists():
        raise DashboardError(f"Log file does not exist: {path}")
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    if max_lines > 0 and len(lines) > max_lines:
        lines = lines[-max_lines:]
    return "\n".join(lines)


def get_task_log(state_dir: Path, task_id: str, kind: str, max_lines: int) -> dict[str, Any]:
    details = build_run_details(state_dir)
    task = next((item for item in details["tasks"] if item["task_id"] == task_id), None)
    if task is None:
        raise DashboardError(f"Unknown task {task_id!r}")
    key = {
        "stdout": "stdout_path",
        "stderr": "stderr_path",
        "result": "result_path",
        "block": "block_context_path",
    }.get(kind)
    if key is None:
        raise DashboardError(f"Unsupported log kind {kind!r}")
    raw_path = task.get(key)
    if not raw_path:
        raise DashboardError(f"No {kind} path recorded for task {task_id}")
    path = Path(raw_path)
    return {
        "task_id": task_id,
        "kind": kind,
        "path": str(path),
        "content": tail_text(path, max_lines=max_lines),
    }


def open_local_path(path: Path) -> None:
    if os.name == "nt":
        subprocess.Popen(["explorer.exe", str(path)])
        return
    if sys.platform == "darwin":
        subprocess.Popen(["open", str(path)])
        return
    subprocess.Popen(["xdg-open", str(path)])


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Serve a small browser dashboard for commentary orchestrator runs.",
    )
    parser.add_argument("--host", default="127.0.0.1", help="Bind host.")
    parser.add_argument("--port", type=int, default=8765, help="Bind port.")
    parser.add_argument(
        "--runs-root",
        default=str(DEFAULT_RUNS_ROOT),
        help="Directory containing orchestration state directories.",
    )
    parser.add_argument(
        "--default-workflow",
        default=str(DEFAULT_WORKFLOW),
        help="Workflow path prefilled in the launch form.",
    )
    parser.add_argument(
        "--codex-binary",
        default="codex",
        help="Codex CLI binary used when launching new runs.",
    )
    parser.add_argument(
        "--open-browser",
        action="store_true",
        help="Open the dashboard URL in the default browser after startup.",
    )
    return parser


def make_handler(
    runs_root: Path,
    repo_root: Path,
    default_workflow: Path,
    codex_binary: str,
    html_path: Path,
):
    class DashboardHandler(BaseHTTPRequestHandler):
        server_version = "CommentaryOrchestratorDashboard/0.1"

        def log_message(self, format: str, *args: Any) -> None:  # noqa: A003
            return

        def _send_json(self, payload: Any, status: int = HTTPStatus.OK) -> None:
            body = json.dumps(payload, indent=2).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _send_html(self, path: Path) -> None:
            body = path.read_bytes()
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _read_json_body(self) -> dict[str, Any]:
            raw_length = self.headers.get("Content-Length")
            if raw_length is None:
                return {}
            payload = self.rfile.read(int(raw_length))
            if not payload:
                return {}
            return json.loads(payload.decode("utf-8"))

        def _handle_error(self, exc: Exception) -> None:
            status = HTTPStatus.BAD_REQUEST if isinstance(exc, DashboardError) else HTTPStatus.INTERNAL_SERVER_ERROR
            self._send_json({"error": str(exc)}, status=status)

        def do_GET(self) -> None:  # noqa: N802
            parsed = urlparse(self.path)
            query = parse_qs(parsed.query)
            try:
                if parsed.path == "/":
                    self._send_html(html_path)
                    return
                if parsed.path == "/api/config":
                    self._send_json(
                        {
                            "runs_root": str(runs_root),
                            "repo_root": str(repo_root),
                            "default_workflow": str(default_workflow),
                            "default_runner": "codex",
                        }
                    )
                    return
                if parsed.path == "/api/runs":
                    runs = [
                        build_state_summary(resolve_state_path(state_dir))
                        for state_dir in discover_state_dirs(runs_root)
                    ]
                    self._send_json({"runs": runs})
                    return
                if parsed.path == "/api/run-details":
                    raw_state_dir = query.get("state_dir", [""])[0]
                    state_dir = resolve_state_dir(raw_state_dir, runs_root)
                    self._send_json(build_run_details(state_dir))
                    return
                if parsed.path == "/api/task-log":
                    raw_state_dir = query.get("state_dir", [""])[0]
                    task_id = query.get("task_id", [""])[0]
                    kind = query.get("kind", ["stdout"])[0]
                    max_lines = int(query.get("lines", ["200"])[0])
                    state_dir = resolve_state_dir(raw_state_dir, runs_root)
                    self._send_json(get_task_log(state_dir, task_id, kind, max_lines))
                    return
                self._send_json({"error": "Not found"}, status=HTTPStatus.NOT_FOUND)
            except Exception as exc:  # noqa: BLE001
                self._handle_error(exc)

        def do_POST(self) -> None:  # noqa: N802
            parsed = urlparse(self.path)
            try:
                body = self._read_json_body()
                if parsed.path == "/api/launch":
                    workflow_path = resolve_workflow_path(
                        body.get("workflow_path") or str(default_workflow),
                        repo_root,
                    )
                    raw_state_dir = body.get("state_dir")
                    state_dir = (
                        resolve_state_dir(raw_state_dir, runs_root)
                        if raw_state_dir
                        else generate_state_dir(runs_root, workflow_path)
                    )
                    runner = body.get("runner") or "codex"
                    payload = launch_background_run(
                        workflow_path=workflow_path,
                        state_dir=state_dir,
                        runner=runner,
                        max_parallel=None,
                        poll_interval_seconds=0.25,
                        codex_binary=codex_binary,
                    )
                    self._send_json(payload)
                    return
                if parsed.path == "/api/stop":
                    state_dir = resolve_state_dir(body.get("state_dir", ""), runs_root)
                    summary, _ = stop_background_run(state_dir)
                    self._send_json(summary)
                    return
                if parsed.path == "/api/open":
                    raw_path = body.get("path") or body.get("state_dir") or ""
                    candidate = Path(raw_path)
                    if not candidate.is_absolute():
                        candidate = runs_root / candidate
                    resolved = candidate.resolve()
                    if resolved.exists():
                        try:
                            ensure_relative_to(resolved, runs_root, "open path")
                        except DashboardError:
                            ensure_relative_to(resolved, repo_root, "open path")
                        open_local_path(resolved)
                        self._send_json({"opened": str(resolved)})
                        return
                    raise DashboardError(f"Path does not exist: {resolved}")
                self._send_json({"error": "Not found"}, status=HTTPStatus.NOT_FOUND)
            except Exception as exc:  # noqa: BLE001
                self._handle_error(exc)

    return DashboardHandler


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    runs_root = Path(args.runs_root).resolve()
    repo_root = REPO_ROOT.resolve()
    default_workflow = resolve_workflow_path(args.default_workflow, repo_root)
    html_path = DEFAULT_HTML.resolve()
    handler = make_handler(
        runs_root=runs_root,
        repo_root=repo_root,
        default_workflow=default_workflow,
        codex_binary=args.codex_binary,
        html_path=html_path,
    )
    server = ThreadingHTTPServer((args.host, args.port), handler)
    url = f"http://{args.host}:{server.server_address[1]}/"
    print(url)
    if args.open_browser:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        return 0
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
