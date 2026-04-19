from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from collections import deque
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, TextIO


ALLOWED_RESULT_STATUSES = {"completed", "retry", "blocked"}


class WorkflowError(RuntimeError):
    pass


def utc_now() -> str:
    return datetime.now(UTC).isoformat()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_json_retry(path: Path, attempts: int = 8, delay_seconds: float = 0.05) -> Any:
    last_error: Exception | None = None
    for _ in range(attempts):
        try:
            return load_json(path)
        except (FileNotFoundError, json.JSONDecodeError) as exc:
            last_error = exc
            time.sleep(delay_seconds)
    if last_error is not None:
        raise last_error
    return load_json(path)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def parse_utc(raw_value: Any) -> datetime | None:
    if not isinstance(raw_value, str) or not raw_value:
        return None
    try:
        return datetime.fromisoformat(raw_value)
    except ValueError:
        return None


def is_pid_running(pid: int | None) -> bool:
    if not isinstance(pid, int) or pid <= 0:
        return False
    if os.name == "nt":
        completed = subprocess.run(
            ["tasklist", "/FI", f"PID eq {pid}", "/FO", "CSV", "/NH"],
            capture_output=True,
            text=True,
            check=False,
        )
        output = completed.stdout.strip()
        return bool(output) and not output.startswith("INFO:")
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    return True


def terminate_process_tree(pid: int) -> tuple[bool, str]:
    if os.name == "nt":
        completed = subprocess.run(
            ["taskkill", "/PID", str(pid), "/T", "/F"],
            capture_output=True,
            text=True,
            check=False,
        )
        detail = (completed.stdout + completed.stderr).strip()
        return completed.returncode == 0, detail
    completed = subprocess.run(
        ["pkill", "-TERM", "-P", str(pid)],
        capture_output=True,
        text=True,
        check=False,
    )
    try:
        os.kill(pid, 15)
    except OSError as exc:
        return False, str(exc)
    detail = (completed.stdout + completed.stderr).strip()
    return True, detail


def discover_orchestrator_pid_for_state_dir(state_dir: Path) -> int | None:
    needle = str(state_dir.resolve())
    if os.name == "nt":
        script = (
            f"$needle = {json.dumps(needle)}; "
            "$proc = Get-CimInstance Win32_Process | "
            "Where-Object { $_.CommandLine -and $_.CommandLine -like \"*$needle*\" -and "
            "($_.CommandLine -like \"*orchestrator.py*\" -or $_.CommandLine -like \"*run.ps1*\") } | "
            "Select-Object -First 1 -ExpandProperty ProcessId; "
            "if ($proc) { Write-Output $proc }"
        )
        completed = subprocess.run(
            ["powershell", "-NoProfile", "-Command", script],
            capture_output=True,
            text=True,
            check=False,
        )
        raw_value = completed.stdout.strip()
        return int(raw_value) if raw_value.isdigit() else None
    completed = subprocess.run(
        ["ps", "-ax", "-o", "pid=,command="],
        capture_output=True,
        text=True,
        check=False,
    )
    for raw_line in completed.stdout.splitlines():
        line = raw_line.strip()
        if not line or needle not in line:
            continue
        if "orchestrator.py" not in line and "run.ps1" not in line:
            continue
        pid_text, _, _command = line.partition(" ")
        if pid_text.isdigit():
            return int(pid_text)
    return None


def resolve_orchestrator_pid(state_dir: Path, recorded_pid: Any) -> int | None:
    if isinstance(recorded_pid, int) and is_pid_running(recorded_pid):
        return recorded_pid
    discovered_pid = discover_orchestrator_pid_for_state_dir(state_dir)
    if isinstance(discovered_pid, int) and is_pid_running(discovered_pid):
        return discovered_pid
    return None


def iter_state_dirs(runs_root: Path) -> list[Path]:
    if not runs_root.exists():
        return []
    state_dirs: list[Path] = []
    for child in runs_root.iterdir():
        if child.is_dir() and (child / "state.json").exists():
            state_dirs.append(child.resolve())
    return sorted(state_dirs)


def resolve_state_path(state_dir: Path) -> Path:
    return state_dir.resolve() / "state.json"


def collect_task_status_counts(tasks: dict[str, Any]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for task in tasks.values():
        status = task.get("status", "unknown")
        counts[status] = counts.get(status, 0) + 1
    return dict(sorted(counts.items()))


def find_last_activity(tasks: dict[str, Any]) -> dict[str, Any] | None:
    latest: tuple[datetime, str, str] | None = None
    for task_id, task_state in tasks.items():
        for field in ("last_finished_at", "last_started_at"):
            parsed = parse_utc(task_state.get(field))
            if parsed is None:
                continue
            candidate = (parsed, task_id, field)
            if latest is None or candidate > latest:
                latest = candidate
    if latest is None:
        return None
    return {
        "task_id": latest[1],
        "field": latest[2],
        "timestamp": latest[0].isoformat(),
    }


def derive_external_run_status(state: dict[str, Any], pid_running: bool) -> str:
    explicit = state.get("run_status")
    tasks = state.get("tasks", {})
    has_active_work = bool(state.get("queue")) or any(
        task.get("status") in {"queued", "running", "retry_queued", "awaiting_review"} for task in tasks.values()
    )
    if explicit == "stopped":
        return "stopped"
    if pid_running:
        return "running"
    if explicit in {"completed", "failed", "needs_human"}:
        return explicit
    if has_active_work:
        return "stale"
    if explicit == "running":
        return "stale"
    return explicit or "unknown"


def build_state_summary(state_path: Path) -> dict[str, Any]:
    state = load_json_retry(state_path)
    tasks = state.get("tasks", {})
    pid = resolve_orchestrator_pid(state_path.parent, state.get("orchestrator_pid"))
    pid_running = is_pid_running(pid)
    status_counts = collect_task_status_counts(tasks)
    running_tasks = sorted(
        task_id for task_id, task in tasks.items() if task.get("status") == "running"
    )
    queued_tasks = sorted(
        task_id for task_id, task in tasks.items() if task.get("status") == "queued"
    )
    failed_tasks = sorted(
        task_id
        for task_id, task in tasks.items()
        if task.get("status") in {"failed", "review_failed"}
    )
    needs_human_tasks = sorted(
        task_id
        for task_id, task in tasks.items()
        if task.get("status") in {"needs_human", "review_blocked"}
    )
    summary = {
        "state_path": str(state_path),
        "state_dir": str(state_path.parent),
        "workflow_path": state.get("workflow_path"),
        "runner": state.get("runner"),
        "run_status": derive_external_run_status(state, pid_running),
        "orchestrator_pid": pid,
        "pid_running": pid_running,
        "started_at": state.get("started_at"),
        "updated_at": state.get("updated_at"),
        "queue_length": len(state.get("queue", [])),
        "queue": state.get("queue", []),
        "running_tasks": running_tasks,
        "queued_tasks": queued_tasks,
        "failed_tasks": failed_tasks,
        "needs_human_tasks": needs_human_tasks,
        "status_counts": status_counts,
        "active_block_context_path": state.get("active_block_context_path"),
        "block_context_count": len(state.get("block_context_history", [])),
        "active_review_context_path": state.get("active_review_context_path"),
        "review_context_count": len(state.get("review_context_history", [])),
        "last_activity": find_last_activity(tasks),
    }
    return summary


def print_human_status(summary: dict[str, Any]) -> None:
    lines = [
        f"Run status: {summary['run_status']}",
        f"State dir: {summary['state_dir']}",
        f"Workflow: {summary.get('workflow_path') or '-'}",
        f"PID: {summary.get('orchestrator_pid') or '-'} ({'alive' if summary['pid_running'] else 'dead'})",
        f"Updated: {summary.get('updated_at') or '-'}",
        f"Queue: {summary['queue_length']} | Running: {', '.join(summary['running_tasks']) or '-'}",
        f"Failed: {', '.join(summary['failed_tasks']) or '-'}",
        f"Needs human: {', '.join(summary['needs_human_tasks']) or '-'}",
        f"Block contexts: {summary['block_context_count']}",
        f"Task counts: {json.dumps(summary['status_counts'], ensure_ascii=False)}",
    ]
    last_activity = summary.get("last_activity")
    if last_activity:
        lines.append(
            "Last activity: "
            f"{last_activity['task_id']} ({last_activity['field']}, {last_activity['timestamp']})"
        )
    print("\n".join(lines))


def launch_background_run(
    workflow_path: Path,
    state_dir: Path,
    runner: str | None,
    max_parallel: int | None,
    poll_interval_seconds: float,
    codex_binary: str,
) -> dict[str, Any]:
    state_dir = state_dir.resolve()
    state_dir.mkdir(parents=True, exist_ok=True)
    stopped_prior_runs: list[str] = []
    for sibling_state_dir in iter_state_dirs(state_dir.parent):
        if sibling_state_dir == state_dir:
            continue
        sibling_pid = resolve_orchestrator_pid(
            sibling_state_dir,
            load_json_retry(resolve_state_path(sibling_state_dir)).get("orchestrator_pid"),
        )
        if sibling_pid is None:
            continue
        summary, exit_code = stop_background_run(sibling_state_dir)
        if exit_code != 0:
            raise WorkflowError(
                f"Failed to stop existing run before launch: {sibling_state_dir}"
            )
        stopped_prior_runs.append(summary["state_dir"])
    stdout_path = state_dir / "launcher.stdout.log"
    stderr_path = state_dir / "launcher.stderr.log"
    manager_path = state_dir / "manager.json"
    command = [
        sys.executable,
        str(Path(__file__).resolve()),
        "run",
        "--workflow",
        str(workflow_path.resolve()),
        "--state-dir",
        str(state_dir),
        "--poll-interval-seconds",
        str(poll_interval_seconds),
        "--codex-binary",
        codex_binary,
    ]
    if runner:
        command.extend(["--runner", runner])
    if max_parallel is not None:
        command.extend(["--max-parallel", str(max_parallel)])
    creationflags = 0
    if os.name == "nt":
        creationflags |= getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0)
        creationflags |= getattr(subprocess, "DETACHED_PROCESS", 0)
        creationflags |= getattr(subprocess, "CREATE_NO_WINDOW", 0)
    stdout_handle = stdout_path.open("w", encoding="utf-8")
    stderr_handle = stderr_path.open("w", encoding="utf-8")
    process = subprocess.Popen(
        command,
        cwd=str(Path(__file__).resolve().parent),
        stdout=stdout_handle,
        stderr=stderr_handle,
        text=True,
        close_fds=True,
        creationflags=creationflags,
    )
    stdout_handle.close()
    stderr_handle.close()
    state_path = resolve_state_path(state_dir)
    for _ in range(50):
        if state_path.exists() or process.poll() is not None:
            break
        time.sleep(0.1)
    payload = {
        "launched_at": utc_now(),
        "state_dir": str(state_dir),
        "state_path": str(state_path),
        "launcher_stdout_path": str(stdout_path),
        "launcher_stderr_path": str(stderr_path),
        "orchestrator_pid": process.pid,
        "command": command,
        "pid_running": is_pid_running(process.pid),
        "stopped_prior_runs": stopped_prior_runs,
    }
    write_json(manager_path, payload)
    payload["manager_path"] = str(manager_path)
    return payload


def stop_background_run(state_dir: Path) -> tuple[dict[str, Any], int]:
    state_path = resolve_state_path(state_dir)
    if not state_path.exists():
        raise WorkflowError(f"State file does not exist: {state_path}")
    state = load_json_retry(state_path)
    pid = resolve_orchestrator_pid(state_dir, state.get("orchestrator_pid"))
    stop_result = {
        "stopped_at": utc_now(),
        "orchestrator_pid": pid,
        "pid_running_before_stop": is_pid_running(pid),
    }
    if stop_result["pid_running_before_stop"]:
        ok, detail = terminate_process_tree(pid)
        stop_result["terminate_ok"] = ok
        stop_result["detail"] = detail
        if ok and isinstance(pid, int):
            for _ in range(40):
                if not is_pid_running(pid):
                    break
                time.sleep(0.05)
    else:
        stop_result["terminate_ok"] = False
        stop_result["detail"] = "Process was not running."
    if isinstance(pid, int):
        state["orchestrator_pid"] = pid
    state["run_status"] = "stopped"
    state["manager_stop"] = stop_result
    write_json(state_path, state)
    summary = build_state_summary(state_path)
    exit_code = 0 if stop_result["terminate_ok"] or not stop_result["pid_running_before_stop"] else 1
    return summary, exit_code


def resolve_path(base_dir: Path, raw_path: str | None) -> Path | None:
    if raw_path is None:
        return None
    path = Path(raw_path)
    return path if path.is_absolute() else (base_dir / path).resolve()


def render_prompt_template(prompt: str, context: dict[str, Any]) -> str:
    rendered = prompt
    for key, value in context.items():
        rendered = rendered.replace(f"{{{{{key}}}}}", str(value))
    return rendered


def validate_worker_result(task_id: str, payload: Any) -> dict[str, Any]:
    if not isinstance(payload, dict):
        raise WorkflowError(f"Task {task_id} result must be a JSON object.")
    status = payload.get("status")
    if status not in ALLOWED_RESULT_STATUSES:
        raise WorkflowError(
            f"Task {task_id} returned invalid status {status!r}; "
            f"expected one of {sorted(ALLOWED_RESULT_STATUSES)}."
        )
    summary = payload.get("summary")
    if not isinstance(summary, str) or not summary.strip():
        raise WorkflowError(f"Task {task_id} result must include a non-empty summary.")
    needs_human = payload.get("needs_human")
    if not isinstance(needs_human, bool):
        raise WorkflowError(f"Task {task_id} result must include boolean needs_human.")
    next_tasks = payload.get("next_tasks")
    if not isinstance(next_tasks, list) or not all(
        isinstance(item, str) and item for item in next_tasks
    ):
        raise WorkflowError(
            f"Task {task_id} result must include next_tasks as a string array."
        )
    artifacts = payload.get("artifacts", [])
    if not isinstance(artifacts, list) or not all(
        isinstance(item, str) and item for item in artifacts
    ):
        raise WorkflowError(
            f"Task {task_id} result must include artifacts as a string array."
        )
    return {
        "status": status,
        "summary": summary.strip(),
        "needs_human": needs_human,
        "next_tasks": next_tasks,
        "artifacts": artifacts,
    }


def parse_event_stream(path: Path) -> dict[str, Any]:
    metadata: dict[str, Any] = {}
    if not path.exists():
        return metadata
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        try:
            payload = json.loads(line)
        except json.JSONDecodeError:
            continue
        event_type = payload.get("type")
        if event_type == "thread.started":
            metadata["thread_id"] = payload.get("thread_id")
        elif event_type == "turn.completed":
            metadata["usage"] = payload.get("usage")
    return metadata


@dataclass
class RunningProcess:
    task_id: str
    attempt: int
    process: subprocess.Popen[str]
    started_at: float
    cwd: Path
    command: list[str]
    stdout_path: Path
    stderr_path: Path
    result_path: Path
    stdout_handle: TextIO
    stderr_handle: TextIO
    timeout_seconds: float | None

    def close_handles(self) -> None:
        self.stdout_handle.close()
        self.stderr_handle.close()


class WorkflowOrchestrator:
    def __init__(
        self,
        workflow_path: Path,
        state_dir: Path,
        runner_type: str | None,
        max_parallel: int | None,
        poll_interval_seconds: float,
        codex_binary: str,
    ) -> None:
        self.workflow_path = workflow_path.resolve()
        self.workflow_dir = self.workflow_path.parent
        self.workflow = load_json(self.workflow_path)
        self.defaults = self.workflow.get("defaults", {})
        self.tasks = self.workflow.get("tasks", {})
        if not isinstance(self.tasks, dict) or not self.tasks:
            raise WorkflowError("Workflow must define a non-empty tasks object.")
        self.initial_tasks = self.workflow.get("initial_tasks", [])
        if not isinstance(self.initial_tasks, list) or not self.initial_tasks:
            raise WorkflowError("Workflow must define a non-empty initial_tasks array.")
        self.runner_type = runner_type or self.defaults.get("runner", "codex")
        if self.runner_type not in {"codex", "mock"}:
            raise WorkflowError("Runner must be either 'codex' or 'mock'.")
        self.max_parallel = max_parallel or int(self.defaults.get("max_parallel", 1))
        if self.max_parallel < 1:
            raise WorkflowError("max_parallel must be at least 1.")
        self.poll_interval_seconds = poll_interval_seconds
        self.codex_binary = self._resolve_codex_binary(codex_binary)
        self.mock_runner = (Path(__file__).parent / "mock_codex.py").resolve()
        self.state_dir = state_dir.resolve()
        self.state_dir.mkdir(parents=True, exist_ok=True)
        self.state_path = self.state_dir / "state.json"
        self.block_context_dir = self.state_dir / "block_contexts"
        self.block_context_dir.mkdir(parents=True, exist_ok=True)
        self.block_context_counter = 0
        self.review_context_dir = self.state_dir / "review_contexts"
        self.review_context_dir.mkdir(parents=True, exist_ok=True)
        self.review_context_counter = 0
        self.queue: deque[str] = deque()
        self.running: dict[str, RunningProcess] = {}
        self.state: dict[str, Any] = {
            "workflow_path": str(self.workflow_path),
            "runner": self.runner_type,
            "orchestrator_pid": os.getpid(),
            "run_status": "running",
            "started_at": utc_now(),
            "updated_at": utc_now(),
            "queue": [],
            "running_tasks": [],
            "active_block_context_path": None,
            "block_context_history": [],
            "active_review_context_path": None,
            "review_context_history": [],
            "tasks": {},
        }
        for task_id in self.initial_tasks:
            self.enqueue(task_id, reason="initial")

    @staticmethod
    def _resolve_codex_binary(raw_binary: str) -> str:
        if os.name != "nt":
            return raw_binary
        binary_path = Path(raw_binary)
        if binary_path.suffix.lower() in {".cmd", ".exe", ".bat"}:
            return raw_binary
        candidates = [
            f"{raw_binary}.cmd",
            f"{raw_binary}.exe",
            raw_binary,
        ]
        for candidate in candidates:
            resolved = shutil.which(candidate)
            if resolved:
                return resolved
        return raw_binary

    def run(self) -> dict[str, Any]:
        while self.queue or self.running:
            self._launch_ready_tasks()
            self._poll_running_tasks()
            self._write_state()
            if self.running and not self.queue:
                time.sleep(self.poll_interval_seconds)
            elif self.queue and len(self.running) >= self.max_parallel:
                time.sleep(self.poll_interval_seconds)
        self.state["updated_at"] = utc_now()
        self._write_state()
        return self.state

    def _derive_run_status(self) -> str:
        if self.running or self.queue:
            return "running"
        task_statuses = [task.get("status") for task in self.state["tasks"].values()]
        if any(status in {"failed", "review_failed"} for status in task_statuses):
            return "failed"
        if any(status == "needs_human" for status in task_statuses):
            return "needs_human"
        if any(status == "review_blocked" for status in task_statuses):
            return "needs_human"
        if self.state.get("run_status") == "stopped":
            return "stopped"
        return "completed"

    def enqueue(self, task_id: str, reason: str) -> None:
        task = self.tasks.get(task_id)
        if not isinstance(task, dict):
            raise WorkflowError(f"Workflow references unknown task {task_id!r}.")
        task_state = self.state["tasks"].setdefault(
            task_id,
            {"history": [], "status": "new", "attempts": 0},
        )
        if task_state["status"] in {"completed", "queued", "running"}:
            return
        if task_state["status"] == "needs_human" and reason != "retry":
            return
        task_state["status"] = "queued"
        task_state["queue_reason"] = reason
        if task_id not in self.queue:
            self.queue.append(task_id)
        self.state["queue"] = list(self.queue)

    def _launch_ready_tasks(self) -> None:
        while self.queue and len(self.running) < self.max_parallel:
            task_id = self.queue.popleft()
            self.state["queue"] = list(self.queue)
            task_state = self.state["tasks"][task_id]
            if task_state["status"] == "completed":
                continue
            running_process = self._spawn_task(task_id)
            task_state["status"] = "running"
            task_state["attempts"] = running_process.attempt
            task_state["last_started_at"] = utc_now()
            task_state["cwd"] = str(running_process.cwd)
            task_state["command"] = running_process.command
            task_state["current_attempt"] = running_process.attempt
            task_state["current_stdout_path"] = str(running_process.stdout_path)
            task_state["current_stderr_path"] = str(running_process.stderr_path)
            task_state["current_result_path"] = str(running_process.result_path)
            self.running[task_id] = running_process

    def _spawn_task(self, task_id: str) -> RunningProcess:
        task = self.tasks[task_id]
        task_state = self.state["tasks"][task_id]
        attempt = int(task_state.get("attempts", 0)) + 1
        run_dir = self.state_dir / "runs" / task_id / f"attempt-{attempt:02d}"
        run_dir.mkdir(parents=True, exist_ok=True)
        stdout_path = run_dir / "stdout.jsonl"
        stderr_path = run_dir / "stderr.log"
        result_path = run_dir / "result.json"
        cwd = resolve_path(
            self.workflow_dir,
            task.get("cwd") or self.defaults.get("cwd"),
        ) or self.workflow_dir
        if self.runner_type == "mock":
            spec_path = run_dir / "mock_spec.json"
            write_json(spec_path, task.get("mock", {}))
            command = [
                sys.executable,
                str(self.mock_runner),
                "--spec",
                str(spec_path),
                "--result-file",
                str(result_path),
            ]
        else:
            prompt = task.get("prompt")
            if not isinstance(prompt, str) or not prompt.strip():
                raise WorkflowError(f"Codex task {task_id} requires a non-empty prompt.")
            prompt_context = {
                "workflow_path": str(self.workflow_path),
                "state_dir": str(self.state_dir),
                "state_path": str(self.state_path),
                "task_id": task_id,
                "active_block_context_path": self.state.get("active_block_context_path") or "",
                "active_review_context_path": self.state.get("active_review_context_path") or "",
            }
            injected_context = task_state.get("injected_context")
            if isinstance(injected_context, dict):
                prompt_context.update(injected_context)
            rendered_prompt = render_prompt_template(prompt, prompt_context)
            command = [
                self.codex_binary,
                "exec",
                "--json",
                "-o",
                str(result_path),
                "-C",
                str(cwd),
            ]
            if bool(task.get("full_auto", self.defaults.get("full_auto", True))):
                command.append("--full-auto")
            schema_path = resolve_path(
                self.workflow_dir,
                task.get("schema_path") or self.defaults.get("schema_path"),
            )
            if schema_path is not None:
                command.extend(["--output-schema", str(schema_path)])
            model = task.get("model") or self.defaults.get("model")
            if model:
                command.extend(["-m", str(model)])
            command.append(rendered_prompt)
            task_state["rendered_prompt"] = rendered_prompt
        stdout_handle = stdout_path.open("w", encoding="utf-8")
        stderr_handle = stderr_path.open("w", encoding="utf-8")
        process = subprocess.Popen(
            command,
            cwd=str(cwd),
            stdout=stdout_handle,
            stderr=stderr_handle,
            text=True,
        )
        timeout_seconds = task.get("timeout_seconds", self.defaults.get("timeout_seconds"))
        timeout_value = float(timeout_seconds) if timeout_seconds is not None else None
        return RunningProcess(
            task_id=task_id,
            attempt=attempt,
            process=process,
            started_at=time.monotonic(),
            cwd=cwd,
            command=command,
            stdout_path=stdout_path,
            stderr_path=stderr_path,
            result_path=result_path,
            stdout_handle=stdout_handle,
            stderr_handle=stderr_handle,
            timeout_seconds=timeout_value,
        )

    def _poll_running_tasks(self) -> None:
        for task_id in list(self.running.keys()):
            running = self.running[task_id]
            if running.timeout_seconds is not None:
                elapsed = time.monotonic() - running.started_at
                if elapsed > running.timeout_seconds:
                    running.process.kill()
                    running.process.wait(timeout=10)
                    running.close_handles()
                    self._finish_failed_task(
                        task_id,
                        running,
                        f"Task timed out after {running.timeout_seconds:.1f}s.",
                    )
                    del self.running[task_id]
                    continue
            return_code = running.process.poll()
            if return_code is None:
                continue
            running.close_handles()
            del self.running[task_id]
            if return_code != 0:
                self._finish_failed_task(
                    task_id,
                    running,
                    f"Worker exited with code {return_code}.",
                )
                continue
            try:
                raw_result = load_json(running.result_path)
                result = validate_worker_result(task_id, raw_result)
            except Exception as exc:  # noqa: BLE001
                self._finish_failed_task(task_id, running, str(exc))
                continue
            metadata = parse_event_stream(running.stdout_path)
            self._finish_successful_task(task_id, running, result, metadata)

    def _finish_failed_task(
        self,
        task_id: str,
        running: RunningProcess,
        message: str,
    ) -> None:
        task = self.tasks[task_id]
        task_state = self.state["tasks"][task_id]
        review_target_task_id = self._resolve_review_target_task_id(task_state)
        task_state["status"] = "failed"
        task_state["last_finished_at"] = utc_now()
        task_state["last_error"] = message
        task_state["history"].append(
            {
                "attempt": running.attempt,
                "status": "failed",
                "summary": message,
                "result_path": str(running.result_path),
                "stdout_path": str(running.stdout_path),
                "stderr_path": str(running.stderr_path),
            }
        )
        max_attempts = int(task.get("max_attempts", self.defaults.get("max_attempts", 1)))
        if task_state["attempts"] < max_attempts:
            self.enqueue(task_id, reason="retry")
            if review_target_task_id is not None:
                self._set_review_target_status(
                    review_target_task_id,
                    "review_failed",
                    message,
                    task_id,
                )
            return
        if review_target_task_id is not None:
            metadata = parse_event_stream(running.stdout_path)
            if self._route_review_resolution(
                reviewed_task_id=review_target_task_id,
                reviewer_task_id=task_id,
                reviewer_running=running,
                summary=message,
                reviewer_status="review_failed",
                needs_human=False,
                artifacts=[],
                next_tasks=[],
                metadata=metadata,
            ):
                task_state["status"] = "review_resolution_routed"
                self._set_review_target_status(
                    review_target_task_id,
                    "review_failed_routed",
                    message,
                    task_id,
                )
                return
            self._set_review_target_status(
                review_target_task_id,
                "review_failed",
                message,
                task_id,
            )

    def _finish_successful_task(
        self,
        task_id: str,
        running: RunningProcess,
        result: dict[str, Any],
        metadata: dict[str, Any],
    ) -> None:
        task = self.tasks[task_id]
        task_state = self.state["tasks"][task_id]
        expected_next_tasks = task.get("expected_next_tasks")
        if expected_next_tasks is not None:
            if not isinstance(expected_next_tasks, list) or not all(
                isinstance(item, str) and item for item in expected_next_tasks
            ):
                raise WorkflowError(
                    f"Task {task_id} has invalid expected_next_tasks workflow metadata."
                )
            if (
                result["status"] == "completed"
                and not result["needs_human"]
                and result["next_tasks"] != expected_next_tasks
            ):
                self._finish_failed_task(
                    task_id,
                    running,
                    "Worker returned unexpected next_tasks. "
                    f"Expected {expected_next_tasks!r}, got {result['next_tasks']!r}.",
                )
                return
        history_entry = {
            "attempt": running.attempt,
            "status": result["status"],
            "summary": result["summary"],
            "needs_human": result["needs_human"],
            "next_tasks": result["next_tasks"],
            "artifacts": result["artifacts"],
            "result_path": str(running.result_path),
            "stdout_path": str(running.stdout_path),
            "stderr_path": str(running.stderr_path),
        }
        if metadata:
            history_entry["metadata"] = metadata
        task_state["history"].append(history_entry)
        task_state["last_finished_at"] = utc_now()
        task_state["last_result"] = result
        if metadata:
            task_state["session_metadata"] = metadata
        review_target_task_id = self._resolve_review_target_task_id(task_state)
        max_attempts = int(task.get("max_attempts", self.defaults.get("max_attempts", 1)))
        if result["needs_human"] or result["status"] == "blocked":
            if review_target_task_id is not None:
                if self._route_review_resolution(
                    reviewed_task_id=review_target_task_id,
                    reviewer_task_id=task_id,
                    reviewer_running=running,
                    summary=result["summary"],
                    reviewer_status="review_blocked",
                    needs_human=result["needs_human"],
                    artifacts=result["artifacts"],
                    next_tasks=result["next_tasks"],
                    metadata=metadata,
                ):
                    task_state["status"] = "review_resolution_routed"
                    self._set_review_target_status(
                        review_target_task_id,
                        "review_blocked_routed",
                        result["summary"],
                        task_id,
                    )
                else:
                    self._set_review_target_status(
                        review_target_task_id,
                        "review_blocked",
                        result["summary"],
                        task_id,
                    )
                    task_state["status"] = "needs_human"
                return
            if self._route_block(task_id, task, task_state, running, result, metadata):
                task_state["status"] = "blocked_routed"
            else:
                task_state["status"] = "needs_human"
            return
        if result["status"] == "retry":
            if task_state["attempts"] < max_attempts:
                task_state["status"] = "retry_queued"
                self.enqueue(task_id, reason="retry")
            else:
                retry_message = "Retry budget exhausted."
                if review_target_task_id is not None and self._route_review_resolution(
                    reviewed_task_id=review_target_task_id,
                    reviewer_task_id=task_id,
                    reviewer_running=running,
                    summary=retry_message,
                    reviewer_status="review_retry_exhausted",
                    needs_human=False,
                    artifacts=result["artifacts"],
                    next_tasks=result["next_tasks"],
                    metadata=metadata,
                ):
                    task_state["status"] = "review_resolution_routed"
                    self._set_review_target_status(
                        review_target_task_id,
                        "review_failed_routed",
                        retry_message,
                        task_id,
                    )
                else:
                    task_state["status"] = "failed"
                    task_state["last_error"] = retry_message
                    if review_target_task_id is not None:
                        self._set_review_target_status(
                            review_target_task_id,
                            "review_failed",
                            retry_message,
                            task_id,
                        )
            return
        if review_target_task_id is not None:
            self._approve_review_target(review_target_task_id, result["summary"], task_id)
        if self._route_review(task_id, task, task_state, running, result, metadata):
            task_state["status"] = "awaiting_review"
            return
        task_state["status"] = "completed"
        for next_task in result["next_tasks"]:
            self.enqueue(next_task, reason=f"completed:{task_id}")

    def _resolve_review_target_task_id(self, task_state: dict[str, Any]) -> str | None:
        injected_context = task_state.get("injected_context")
        if not isinstance(injected_context, dict):
            return None
        raw_value = injected_context.get("reviewed_task_id")
        return raw_value if isinstance(raw_value, str) and raw_value else None

    def _set_review_target_status(
        self,
        reviewed_task_id: str,
        status: str,
        summary: str,
        reviewer_task_id: str,
    ) -> None:
        reviewed_state = self.state["tasks"].get(reviewed_task_id)
        if not isinstance(reviewed_state, dict):
            return
        reviewed_state["status"] = status
        reviewed_state["review_status"] = status
        reviewed_state["review_last_summary"] = summary
        reviewed_state["reviewed_by_task"] = reviewer_task_id
        reviewed_state["review_last_updated_at"] = utc_now()

    def _approve_review_target(
        self,
        reviewed_task_id: str,
        summary: str,
        reviewer_task_id: str,
    ) -> None:
        reviewed_state = self.state["tasks"].get(reviewed_task_id)
        if not isinstance(reviewed_state, dict):
            return
        reviewed_state["status"] = "completed"
        reviewed_state["review_status"] = "approved"
        reviewed_state["review_last_summary"] = summary
        reviewed_state["reviewed_by_task"] = reviewer_task_id
        reviewed_state["review_approved_at"] = utc_now()
        reviewed_state.pop("review_context_path", None)
        reviewed_state.pop("pending_review_handler", None)

    def _resolve_block_handler_task(self, task: dict[str, Any]) -> str | None:
        if "block_handler_task" in task:
            raw_handler = task.get("block_handler_task")
        else:
            raw_handler = self.defaults.get("block_handler_task")
        if raw_handler in (None, "", False):
            return None
        if not isinstance(raw_handler, str):
            raise WorkflowError("block_handler_task must be a string or null.")
        if raw_handler not in self.tasks:
            raise WorkflowError(f"Unknown block handler task {raw_handler!r}.")
        return raw_handler

    def _resolve_review_handler_task(self, task: dict[str, Any]) -> str | None:
        if "review_handler_task" in task:
            raw_handler = task.get("review_handler_task")
        else:
            raw_handler = self.defaults.get("review_handler_task")
        if raw_handler in (None, "", False):
            return None
        if not isinstance(raw_handler, str):
            raise WorkflowError("review_handler_task must be a string or null.")
        if raw_handler not in self.tasks:
            raise WorkflowError(f"Unknown review handler task {raw_handler!r}.")
        return raw_handler

    def _route_review(
        self,
        task_id: str,
        task: dict[str, Any],
        task_state: dict[str, Any],
        running: RunningProcess,
        result: dict[str, Any],
        metadata: dict[str, Any],
    ) -> bool:
        handler_task_id = self._resolve_review_handler_task(task)
        if handler_task_id is None:
            return False
        self.review_context_counter += 1
        review_context_path = self.review_context_dir / (
            f"review-{self.review_context_counter:03d}-{task_id}.json"
        )
        review_context = {
            "reviewed_task_id": task_id,
            "reviewed_summary": result["summary"],
            "reviewed_status": result["status"],
            "reviewed_attempt": running.attempt,
            "reviewed_result_path": str(running.result_path),
            "reviewed_stdout_path": str(running.stdout_path),
            "reviewed_stderr_path": str(running.stderr_path),
            "reviewed_artifacts": result["artifacts"],
            "reviewed_next_tasks": result["next_tasks"],
            "reviewed_session_metadata": metadata,
            "captured_at": utc_now(),
        }
        write_json(review_context_path, review_context)
        self.state["active_review_context_path"] = str(review_context_path)
        self.state["review_context_history"].append(str(review_context_path))
        task_state["review_context_path"] = str(review_context_path)
        task_state["pending_review_handler"] = handler_task_id
        handler_state = self.state["tasks"].setdefault(
            handler_task_id,
            {"history": [], "status": "new", "attempts": 0},
        )
        if handler_state.get("status") not in {"queued", "running"}:
            handler_state["status"] = "new"
        handler_state["injected_context"] = {
            "active_review_context_path": str(review_context_path),
            "reviewed_task_id": task_id,
            "reviewed_summary": result["summary"],
            "reviewed_status": result["status"],
            "reviewed_result_path": str(running.result_path),
            "reviewed_stdout_path": str(running.stdout_path),
            "reviewed_stderr_path": str(running.stderr_path),
            "reviewed_next_tasks_json": json.dumps(result["next_tasks"]),
        }
        self.enqueue(handler_task_id, reason=f"review:{task_id}")
        return True

    def _route_review_resolution(
        self,
        reviewed_task_id: str,
        reviewer_task_id: str,
        reviewer_running: RunningProcess,
        summary: str,
        reviewer_status: str,
        needs_human: bool,
        artifacts: list[str],
        next_tasks: list[str],
        metadata: dict[str, Any],
    ) -> bool:
        reviewed_task = self.tasks.get(reviewed_task_id)
        reviewed_state = self.state["tasks"].get(reviewed_task_id)
        if not isinstance(reviewed_task, dict) or not isinstance(reviewed_state, dict):
            return False
        review_context_path = reviewed_state.get("review_context_path")
        review_context_payload: dict[str, Any] = {}
        if isinstance(review_context_path, str) and review_context_path:
            review_context_file = Path(review_context_path)
            if review_context_file.exists():
                try:
                    loaded = load_json_retry(review_context_file)
                except Exception:  # noqa: BLE001
                    loaded = {}
                if isinstance(loaded, dict):
                    review_context_payload = loaded
        extra_context = {
            "blocked_origin": "review",
            "review_context_path": review_context_path or "",
            "reviewed_task_id": reviewed_task_id,
            "reviewed_status_at_route": reviewed_state.get("status", ""),
            "reviewed_result_path": review_context_payload.get("reviewed_result_path", ""),
            "reviewed_stdout_path": review_context_payload.get("reviewed_stdout_path", ""),
            "reviewed_stderr_path": review_context_payload.get("reviewed_stderr_path", ""),
            "reviewed_next_tasks_json": json.dumps(review_context_payload.get("reviewed_next_tasks", [])),
            "reviewer_task_id": reviewer_task_id,
            "reviewer_status": reviewer_status,
            "reviewer_needs_human": needs_human,
            "reviewer_artifacts_json": json.dumps(artifacts),
            "reviewer_next_tasks_json": json.dumps(next_tasks),
        }
        return self._route_block_for_target(
            blocked_task_id=reviewed_task_id,
            blocked_task=reviewed_task,
            blocked_task_state=reviewed_state,
            source_task_id=reviewer_task_id,
            source_running=reviewer_running,
            summary=summary,
            status=reviewer_status,
            needs_human=needs_human,
            artifacts=artifacts,
            next_tasks=next_tasks,
            metadata=metadata,
            extra_context=extra_context,
        )

    def _route_block(
        self,
        task_id: str,
        task: dict[str, Any],
        task_state: dict[str, Any],
        running: RunningProcess,
        result: dict[str, Any],
        metadata: dict[str, Any],
    ) -> bool:
        return self._route_block_for_target(
            blocked_task_id=task_id,
            blocked_task=task,
            blocked_task_state=task_state,
            source_task_id=task_id,
            source_running=running,
            summary=result["summary"],
            status=result["status"],
            needs_human=result["needs_human"],
            artifacts=result["artifacts"],
            next_tasks=result["next_tasks"],
            metadata=metadata,
            extra_context=None,
        )

    def _route_block_for_target(
        self,
        blocked_task_id: str,
        blocked_task: dict[str, Any],
        blocked_task_state: dict[str, Any],
        source_task_id: str,
        source_running: RunningProcess,
        summary: str,
        status: str,
        needs_human: bool,
        artifacts: list[str],
        next_tasks: list[str],
        metadata: dict[str, Any],
        extra_context: dict[str, Any] | None,
    ) -> bool:
        handler_task_id = self._resolve_block_handler_task(blocked_task)
        if handler_task_id is None:
            return False
        self.block_context_counter += 1
        block_context_path = self.block_context_dir / (
            f"block-{self.block_context_counter:03d}-{blocked_task_id}.json"
        )
        block_context = {
            "blocked_task_id": blocked_task_id,
            "blocked_summary": summary,
            "blocked_status": status,
            "blocked_needs_human": needs_human,
            "blocked_attempt": source_running.attempt,
            "blocked_result_path": str(source_running.result_path),
            "blocked_stdout_path": str(source_running.stdout_path),
            "blocked_stderr_path": str(source_running.stderr_path),
            "blocked_artifacts": artifacts,
            "blocked_next_tasks": next_tasks,
            "blocked_session_metadata": metadata,
            "blocked_source_task_id": source_task_id,
            "captured_at": utc_now(),
        }
        if extra_context:
            block_context.update(extra_context)
        write_json(block_context_path, block_context)
        self.state["active_block_context_path"] = str(block_context_path)
        self.state["block_context_history"].append(str(block_context_path))
        blocked_task_state["block_context_path"] = str(block_context_path)
        handler_state = self.state["tasks"].setdefault(
            handler_task_id,
            {"history": [], "status": "new", "attempts": 0},
        )
        if handler_state.get("status") not in {"queued", "running"}:
            handler_state["status"] = "new"
        injected_context = {
            "active_block_context_path": str(block_context_path),
            "blocked_task_id": blocked_task_id,
            "blocked_summary": summary,
            "blocked_status": status,
            "blocked_result_path": str(source_running.result_path),
            "blocked_stdout_path": str(source_running.stdout_path),
            "blocked_stderr_path": str(source_running.stderr_path),
        }
        if extra_context:
            injected_context.update(
                {
                    key: value
                    for key, value in extra_context.items()
                    if key != "reviewed_task_id"
                }
            )
        handler_state["injected_context"] = injected_context
        self.enqueue(handler_task_id, reason=f"blocked:{blocked_task_id}")
        return True

    def _write_state(self) -> None:
        self.state["updated_at"] = utc_now()
        self.state["queue"] = list(self.queue)
        self.state["running_tasks"] = sorted(self.running.keys())
        self.state["run_status"] = self._derive_run_status()
        write_json(self.state_path, self.state)


def add_run_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--workflow", required=True, help="Path to workflow JSON.")
    parser.add_argument(
        "--state-dir",
        required=True,
        help="Directory where orchestration state and run logs will be stored.",
    )
    parser.add_argument(
        "--runner",
        choices=("codex", "mock"),
        help="Override the workflow runner type.",
    )
    parser.add_argument(
        "--max-parallel",
        type=int,
        help="Override max parallel workers for the run.",
    )
    parser.add_argument(
        "--poll-interval-seconds",
        type=float,
        default=0.25,
        help="Polling cadence while workers are active.",
    )
    parser.add_argument(
        "--codex-binary",
        default="codex",
        help="Codex CLI binary to use for codex runner mode.",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Run a small Codex CLI orchestration loop for commentary work.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    run_parser = subparsers.add_parser("run", help="Run the orchestration workflow.")
    add_run_arguments(run_parser)

    launch_parser = subparsers.add_parser(
        "launch",
        help="Launch the orchestration workflow in the background.",
    )
    add_run_arguments(launch_parser)

    status_parser = subparsers.add_parser(
        "status",
        help="Inspect the current orchestration state.",
    )
    status_parser.add_argument(
        "--state-dir",
        required=True,
        help="State directory created by a previous run or launch command.",
    )
    status_parser.add_argument(
        "--json",
        action="store_true",
        help="Print machine-readable JSON instead of the human summary.",
    )

    watch_parser = subparsers.add_parser(
        "watch",
        help="Refresh the orchestration status until the run finishes or is stopped.",
    )
    watch_parser.add_argument(
        "--state-dir",
        required=True,
        help="State directory created by a previous run or launch command.",
    )
    watch_parser.add_argument(
        "--interval-seconds",
        type=float,
        default=2.0,
        help="Refresh cadence while watching a run.",
    )
    watch_parser.add_argument(
        "--no-clear",
        action="store_true",
        help="Do not clear the terminal between refreshes.",
    )

    stop_parser = subparsers.add_parser(
        "stop",
        help="Stop a background orchestration run by PID from its state directory.",
    )
    stop_parser.add_argument(
        "--state-dir",
        required=True,
        help="State directory created by a previous run or launch command.",
    )
    return parser


def run_command(args: argparse.Namespace) -> int:
    orchestrator = WorkflowOrchestrator(
        workflow_path=Path(args.workflow),
        state_dir=Path(args.state_dir),
        runner_type=args.runner,
        max_parallel=args.max_parallel,
        poll_interval_seconds=args.poll_interval_seconds,
        codex_binary=args.codex_binary,
    )
    state = orchestrator.run()
    tasks = state["tasks"]
    completed = sorted(task_id for task_id, task in tasks.items() if task["status"] == "completed")
    needs_human = sorted(
        task_id for task_id, task in tasks.items() if task["status"] == "needs_human"
    )
    failed = sorted(task_id for task_id, task in tasks.items() if task["status"] == "failed")
    summary = {
        "state_path": str(orchestrator.state_path),
        "completed": completed,
        "needs_human": needs_human,
        "failed": failed,
    }
    print(json.dumps(summary, indent=2))
    return 1 if failed else 0


def launch_command(args: argparse.Namespace) -> int:
    payload = launch_background_run(
        workflow_path=Path(args.workflow),
        state_dir=Path(args.state_dir),
        runner=args.runner,
        max_parallel=args.max_parallel,
        poll_interval_seconds=args.poll_interval_seconds,
        codex_binary=args.codex_binary,
    )
    print(json.dumps(payload, indent=2))
    return 0


def status_command(args: argparse.Namespace) -> int:
    state_path = resolve_state_path(Path(args.state_dir))
    if not state_path.exists():
        raise WorkflowError(f"State file does not exist: {state_path}")
    summary = build_state_summary(state_path)
    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        print_human_status(summary)
    return 0


def watch_command(args: argparse.Namespace) -> int:
    state_path = resolve_state_path(Path(args.state_dir))
    if not state_path.exists():
        raise WorkflowError(f"State file does not exist: {state_path}")
    while True:
        summary = build_state_summary(state_path)
        if not args.no_clear:
            os.system("cls" if os.name == "nt" else "clear")
        print_human_status(summary)
        if summary["run_status"] in {"completed", "failed", "needs_human", "stopped", "stale"}:
            return 0
        time.sleep(args.interval_seconds)


def stop_command(args: argparse.Namespace) -> int:
    summary, exit_code = stop_background_run(Path(args.state_dir))
    print(json.dumps(summary, indent=2))
    return exit_code


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if args.command == "run":
        return run_command(args)
    if args.command == "launch":
        return launch_command(args)
    if args.command == "status":
        return status_command(args)
    if args.command == "watch":
        return watch_command(args)
    if args.command == "stop":
        return stop_command(args)
    raise AssertionError(f"Unhandled command {args.command!r}")


if __name__ == "__main__":
    raise SystemExit(main())
