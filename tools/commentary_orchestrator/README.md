# Commentary Orchestrator

This directory contains a small development-only orchestration harness for
running Codex CLI tasks without stopping for manual handoff after each step.

The harness is intentionally outside `src/main` because it is repo tooling, not
commentary runtime behavior.

## Output Contract

Each worker must finish by returning JSON matching
[task_result.schema.json](/C:/Codes/CondensedChess/tools/commentary_orchestrator/task_result.schema.json):

```json
{
  "status": "completed",
  "summary": "Short machine-readable summary.",
  "needs_human": false,
  "next_tasks": ["next-task-id"],
  "artifacts": ["optional/path.md"]
}
```

Meaning:

- `status = completed`
  - mark the task done and enqueue `next_tasks`
- `status = retry`
  - requeue the same task until `max_attempts`
- `status = blocked` or `needs_human = true`
  - either stop for a person, or route into a dedicated block-handler task if
    the workflow declares one

## Mock Smoke Run

This verifies the orchestration loop without spending Codex usage:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 run `
  --workflow C:\Codes\CondensedChess\tools\commentary_orchestrator\samples\r_to_u_smoke.json `
  --state-dir C:\Codes\CondensedChess\tmp\commentary_orchestrator_smoke `
  --runner mock
```

The resulting state file lives under:

`tmp/commentary_orchestrator_smoke/state.json`

Per-attempt stdout, stderr, and final JSON results live under:

`tmp/commentary_orchestrator_smoke/runs/...`

## Real Codex Run

Swap the workflow runner to `codex` and provide real prompts:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 run `
  --workflow C:\path\to\workflow.json `
  --state-dir C:\Codes\CondensedChess\tmp\commentary_orchestrator_real `
  --runner codex
```

Codex tasks are launched as `codex exec --json -o <result.json> ...`.

The orchestrator does not block on each task by design. It starts workers with
`subprocess.Popen`, polls them, and enqueues follow-up tasks when a worker
finishes successfully.

## Managed Run Lifecycle

For a background run that you can inspect and stop later:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 launch `
  --workflow C:\Codes\CondensedChess\tools\commentary_orchestrator\workflows\u_primary_implementation.json `
  --state-dir C:\Codes\CondensedChess\tmp\u_primary_implementation_managed `
  --runner codex
```

The launch command enforces a single live run per runs-root directory. If
another managed run is still alive under the same parent `tmp/` directory, it
is stopped before the new run starts so worktree-level compile/test activity
does not overlap.

Check the current status:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 status `
  --state-dir C:\Codes\CondensedChess\tmp\u_primary_implementation_managed
```

Watch it refresh live in the terminal:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 watch `
  --state-dir C:\Codes\CondensedChess\tmp\u_primary_implementation_managed
```

Stop it by PID recorded in the state file:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\run.ps1 stop `
  --state-dir C:\Codes\CondensedChess\tmp\u_primary_implementation_managed
```

## Browser Dashboard

For a simple local GUI, start the dashboard server:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Codes\CondensedChess\tools\commentary_orchestrator\dashboard.ps1 `
  --host 127.0.0.1 `
  --port 8765 `
  --open-browser
```

The dashboard shows:

- discovered state directories under `tmp/`
- current run status, queue, failed tasks, and block-context count
- launch form for new `codex` or `mock` runs
- stop button for background runs
- per-task latest `stdout`, `stderr`, `result`, and block-context log tails

The GUI reads and writes the same `state.json` based lifecycle used by the CLI,
so `launch/status/watch/stop` and the browser view stay in sync.

The historical U-primary workflow is intentionally retained only as a retired
tombstone. Launching it must produce a blocked human handoff until
`PositionFixtureLaw` exists and a replacement workflow is written against the
live reset docs:

[u_primary_implementation.json](/C:/Codes/CondensedChess/tools/commentary_orchestrator/workflows/u_primary_implementation.json)

Workflows may also declare `expected_next_tasks`. When present, the
orchestrator rejects any worker result whose `next_tasks` does not match the
frozen sequence.

Workflows may declare `block_handler_task` in `defaults` or per-task. When a
task returns `blocked` or `needs_human = true`, the orchestrator writes a block
context JSON file under `state_dir/block_contexts/` and enqueues the handler
task instead of stopping immediately. Handler prompts can read:

- `{{active_block_context_path}}`
- `{{blocked_task_id}}`
- `{{blocked_summary}}`
- `{{blocked_result_path}}`
- `{{blocked_stdout_path}}`
- `{{blocked_stderr_path}}`

The handler task may be reused across multiple blocked steps in the same run.

Workflows may also declare `review_handler_task` in `defaults` or per-task. When
an implementation task completes successfully, the orchestrator can route that
result into a dedicated review Codex session before any follow-up task is
enqueued. Review tasks may also use their own retry budget. If a review still
fails or returns blocked after that budget is exhausted, the orchestrator routes
the original reviewed task into its block handler instead of stopping the whole
run immediately.

`run.ps1` prefers a working `python.exe` from the current environment, then
falls back to a packaged Anaconda interpreter when the default launcher is
broken on Windows.

On Windows, the orchestrator also resolves `codex` to a concrete `.cmd` or
`.exe` path automatically so Python worker processes do not get stuck on the
PowerShell shim.
