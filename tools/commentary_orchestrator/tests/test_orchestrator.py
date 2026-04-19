from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
import time
import unittest
from contextlib import contextmanager
from pathlib import Path


REPO_ROOT = Path(r"C:\Codes\CondensedChess")
TOOL_DIR = REPO_ROOT / "tools" / "commentary_orchestrator"
ORCHESTRATOR = TOOL_DIR / "orchestrator.py"
SMOKE_WORKFLOW = TOOL_DIR / "samples" / "r_to_u_smoke.json"
U_PRIMARY_WORKFLOW = TOOL_DIR / "workflows" / "u_primary_implementation.json"
COMMENTARY_DOCS = (
    REPO_ROOT / "lila-docker" / "repos" / "lila" / "modules" / "commentary" / "docs"
)
TEST_TEMP_ROOT = REPO_ROOT / "tmp" / "orchestrator-test-temp"
DECISION_LEDGER = COMMENTARY_DOCS / "DecisionFreezeLedger.md"
DESCRIPTOR_MATRIX = COMMENTARY_DOCS / "DescriptorOwnershipMatrix.md"
WITNESSES_61 = COMMENTARY_DOCS / "Witnesses61.md"
VALIDATION_METHODOLOGY = COMMENTARY_DOCS / "ValidationMethodology.md"
COMMENTARY_CORE_SSOT = COMMENTARY_DOCS / "CommentaryCoreSSOT.md"


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def load_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


@contextmanager
def workspace_temp_dir():
    TEST_TEMP_ROOT.mkdir(parents=True, exist_ok=True)
    temp_path = TEST_TEMP_ROOT / f"case-{time.time_ns()}"
    temp_path.mkdir(parents=True, exist_ok=False)
    try:
        yield str(temp_path)
    finally:
        shutil.rmtree(temp_path, ignore_errors=True)


def parse_descriptor_owner_rows(path: Path) -> dict[str, dict[str, str]]:
    rows: dict[str, dict[str, str]] = {}
    in_table = False
    for line in load_text(path).splitlines():
        stripped = line.strip()
        if stripped.startswith("| Descriptor | Family | Primary owner layer |"):
            in_table = True
            continue
        if not in_table:
            continue
        if not stripped.startswith("|"):
            if rows:
                break
            continue
        if stripped.startswith("| ---"):
            continue
        columns = [column.strip() for column in stripped.strip("|").split("|")]
        if len(columns) != 6:
            continue
        descriptor = columns[0].strip("`")
        rows[descriptor] = {
            "family": columns[1].strip("`"),
            "owner_layer": columns[2].strip("`"),
            "owner_home": columns[3].strip("`"),
            "linked_homes": columns[4].strip("`"),
            "notes": columns[5].strip(),
        }
    return rows


class CommentaryOrchestratorTest(unittest.TestCase):
    def test_r_to_u_smoke_enqueues_followups(self) -> None:
        with workspace_temp_dir() as temp_dir:
            state_dir = Path(temp_dir) / "state"
            command = [
                sys.executable,
                str(ORCHESTRATOR),
                "run",
                "--workflow",
                str(SMOKE_WORKFLOW),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
            ]
            completed = subprocess.run(
                command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            tasks = state["tasks"]
            self.assertEqual(tasks["r_space_frontier"]["status"], "completed")
            self.assertEqual(tasks["u_space_gain_contract"]["status"], "completed")
            self.assertEqual(tasks["u_file_lane_contract"]["status"], "completed")

    def test_parallel_mock_tasks_do_not_block_the_loop(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "max_parallel": 2,
                "max_attempts": 1,
            },
            "initial_tasks": ["slow_root_gate", "fast_side_probe"],
            "tasks": {
                "slow_root_gate": {
                    "mock": {
                        "sleep_seconds": 2.0,
                        "result": {
                            "status": "completed",
                            "summary": "Slow root gate finished.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        },
                    }
                },
                "fast_side_probe": {
                    "mock": {
                        "sleep_seconds": 0.6,
                        "result": {
                            "status": "completed",
                            "summary": "Fast side probe finished.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        },
                    }
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "parallel.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            command = [
                sys.executable,
                str(ORCHESTRATOR),
                "run",
                "--workflow",
                str(workflow_path),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
                "--poll-interval-seconds",
                "0.1",
            ]
            started_at = time.monotonic()
            completed = subprocess.run(
                command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            elapsed = time.monotonic() - started_at
            self.assertEqual(completed.returncode, 0, completed.stderr)
            self.assertLess(
                elapsed,
                4.2,
                f"Expected parallel run to finish before 4.2s on Windows, got {elapsed:.3f}s.",
            )
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["slow_root_gate"]["status"], "completed")
            self.assertEqual(state["tasks"]["fast_side_probe"]["status"], "completed")

    def test_u_primary_workflow_matches_active_scope(self) -> None:
        workflow = load_json(U_PRIMARY_WORKFLOW)
        metadata = workflow["metadata"]
        active_rows = set(metadata["active_u_primary_rows"])
        self.assertEqual(len(active_rows), 18)
        self.assertEqual(
            active_rows,
            {
                "file_lane_state",
                "diagonal_lane_only",
                "weak_pawn_target_state",
                "passed_pawn_entity_state",
                "weak_outpost_square_state",
                "loose_piece_target_state",
                "pawn_push_break_contact_source",
                "sector_asymmetry_state",
                "available_lever_trigger",
                "rook_on_open_file_state",
                "bishop_pair_state",
                "knight_on_outpost_square",
                "duty_bound_defender",
                "short_run_slider_gate_restriction",
                "pin",
                "fork",
                "skewer",
                "overload",
            },
        )
        self.assertEqual(
            set(metadata["banned_rehome_rows"]),
            {
                "opening-tempo",
                "middlegame-positional",
                "transition-liquidation",
                "endgame-race",
                "central tension",
            },
        )
        self.assertEqual(
            set(metadata["host_shell_only_rows"]),
            {"closed center", "fixed chain"},
        )
        self.assertEqual(
            metadata["sequence"],
            [
                "u_scope_freeze_gate",
                "u_runtime_foundation",
                "u_registry_wiring",
                "u_validation_harness",
                "u_file_lane_state",
                "u_diagonal_lane_only",
                "u_available_lever_trigger",
                "u_weak_pawn_target_state",
                "u_passed_pawn_entity_state",
                "u_weak_outpost_square_state",
                "u_loose_piece_target_state",
                "u_sector_asymmetry_state",
                "u_pawn_push_break_contact_source",
                "u_rook_on_open_file_state",
                "u_bishop_pair_state",
                "u_knight_on_outpost_square",
                "u_duty_bound_defender",
                "u_short_run_slider_gate_restriction",
                "u_pin",
                "u_fork",
                "u_skewer",
                "u_overload",
                "u_integration_compile",
                "u_runtime_validation_smoke",
            ],
        )
        self.assertEqual(workflow["initial_tasks"], ["u_scope_freeze_gate"])
        self.assertEqual(workflow["defaults"]["max_parallel"], 1)
        self.assertEqual(workflow["defaults"]["block_handler_task"], "u_block_resolution")
        self.assertEqual(workflow["defaults"]["review_handler_task"], "u_completion_review")
        self.assertIn("u_runtime_validation_smoke", workflow["tasks"])
        self.assertIn("u_integration_compile", workflow["tasks"])
        self.assertIn("u_registry_wiring", workflow["tasks"])
        self.assertIn("u_validation_harness", workflow["tasks"])
        self.assertIn("u_block_resolution", workflow["tasks"])
        self.assertIn("u_completion_review", workflow["tasks"])
        self.assertIsNone(workflow["tasks"]["u_block_resolution"]["block_handler_task"])
        self.assertIsNone(workflow["tasks"]["u_block_resolution"]["review_handler_task"])
        self.assertEqual(workflow["tasks"]["u_completion_review"]["max_attempts"], 2)
        self.assertIsNone(workflow["tasks"]["u_completion_review"]["block_handler_task"])
        self.assertIsNone(workflow["tasks"]["u_completion_review"]["review_handler_task"])
        self.assertIn("{{blocked_task_id}}", workflow["tasks"]["u_block_resolution"]["prompt"])
        self.assertIn("blocked_origin=review", workflow["tasks"]["u_block_resolution"]["prompt"])
        self.assertIn("{{reviewed_task_id}}", workflow["tasks"]["u_completion_review"]["prompt"])
        for index, task_id in enumerate(metadata["sequence"]):
            expected = [] if index == len(metadata["sequence"]) - 1 else [metadata["sequence"][index + 1]]
            self.assertEqual(workflow["tasks"][task_id]["expected_next_tasks"], expected)

    def test_blocked_task_can_route_to_block_handler(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "block_handler_task": "block_triage",
            },
            "initial_tasks": ["main_task"],
            "tasks": {
                "main_task": {
                    "mock": {
                        "result": {
                            "status": "blocked",
                            "summary": "Main task hit a semantic block.",
                            "needs_human": True,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    }
                },
                "block_triage": {
                    "block_handler_task": None,
                    "expected_next_tasks": ["recovered_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Block handler routed the run to a recovery task.",
                            "needs_human": False,
                            "next_tasks": ["recovered_task"],
                            "artifacts": [],
                        }
                    }
                },
                "recovered_task": {
                    "expected_next_tasks": [],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Recovery task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    }
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "blocked.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            command = [
                sys.executable,
                str(ORCHESTRATOR),
                "run",
                "--workflow",
                str(workflow_path),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
            ]
            completed = subprocess.run(
                command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["main_task"]["status"], "blocked_routed")
            self.assertEqual(state["tasks"]["block_triage"]["status"], "completed")
            self.assertEqual(state["tasks"]["recovered_task"]["status"], "completed")
            self.assertTrue(state["active_block_context_path"])
            block_context_path = Path(state["active_block_context_path"])
            self.assertTrue(block_context_path.exists())
            block_context = load_json(block_context_path)
            self.assertEqual(block_context["blocked_task_id"], "main_task")
            self.assertEqual(
                state["tasks"]["block_triage"]["injected_context"]["blocked_task_id"],
                "main_task",
            )

    def test_completed_task_can_route_to_review_handler(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "review_handler_task": "review_gate",
            },
            "initial_tasks": ["main_task"],
            "tasks": {
                "main_task": {
                    "expected_next_tasks": ["next_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Main task completed and awaits review.",
                            "needs_human": False,
                            "next_tasks": ["next_task"],
                            "artifacts": [],
                        }
                    }
                },
                "review_gate": {
                    "review_handler_task": None,
                    "block_handler_task": None,
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Review passed after verification.",
                            "needs_human": False,
                            "next_tasks": ["next_task"],
                            "artifacts": [],
                        }
                    }
                },
                "next_task": {
                    "review_handler_task": None,
                    "expected_next_tasks": [],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Next task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    }
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "review.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            command = [
                sys.executable,
                str(ORCHESTRATOR),
                "run",
                "--workflow",
                str(workflow_path),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
            ]
            completed = subprocess.run(
                command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["main_task"]["status"], "completed")
            self.assertEqual(state["tasks"]["main_task"]["review_status"], "approved")
            self.assertEqual(state["tasks"]["review_gate"]["status"], "completed")
            self.assertEqual(state["tasks"]["next_task"]["status"], "completed")
            self.assertTrue(state["active_review_context_path"])
            review_context_path = Path(state["active_review_context_path"])
            self.assertTrue(review_context_path.exists())
            review_context = load_json(review_context_path)
            self.assertEqual(review_context["reviewed_task_id"], "main_task")
            self.assertEqual(
                state["tasks"]["review_gate"]["injected_context"]["reviewed_task_id"],
                "main_task",
            )

    def test_review_blocked_can_route_to_reviewed_task_block_handler(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "review_handler_task": "review_gate",
            },
            "initial_tasks": ["main_task"],
            "tasks": {
                "main_task": {
                    "block_handler_task": "block_triage",
                    "expected_next_tasks": ["next_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Main task completed and awaits review.",
                            "needs_human": False,
                            "next_tasks": ["next_task"],
                            "artifacts": [],
                        }
                    },
                },
                "review_gate": {
                    "review_handler_task": None,
                    "block_handler_task": None,
                    "mock": {
                        "result": {
                            "status": "blocked",
                            "summary": "Review found an unresolved runtime issue.",
                            "needs_human": True,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    },
                },
                "block_triage": {
                    "block_handler_task": None,
                    "review_handler_task": None,
                    "expected_next_tasks": ["recovery_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Block triage redirected the run to a recovery task.",
                            "needs_human": False,
                            "next_tasks": ["recovery_task"],
                            "artifacts": [],
                        }
                    },
                },
                "recovery_task": {
                    "review_handler_task": None,
                    "expected_next_tasks": [],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Recovery task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    },
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "review-blocked.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            completed = subprocess.run(
                [
                    sys.executable,
                    str(ORCHESTRATOR),
                    "run",
                    "--workflow",
                    str(workflow_path),
                    "--state-dir",
                    str(state_dir),
                    "--runner",
                    "mock",
                ],
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["main_task"]["status"], "review_blocked_routed")
            self.assertEqual(state["tasks"]["main_task"]["review_status"], "review_blocked_routed")
            self.assertEqual(state["tasks"]["review_gate"]["status"], "review_resolution_routed")
            self.assertEqual(state["tasks"]["block_triage"]["status"], "completed")
            self.assertEqual(state["tasks"]["recovery_task"]["status"], "completed")
            block_context_path = Path(state["active_block_context_path"])
            block_context = load_json(block_context_path)
            self.assertEqual(block_context["blocked_task_id"], "main_task")
            self.assertEqual(block_context["blocked_origin"], "review")
            self.assertEqual(block_context["reviewer_task_id"], "review_gate")
            self.assertEqual(
                state["tasks"]["block_triage"]["injected_context"]["blocked_task_id"],
                "main_task",
            )

    def test_review_failure_retries_then_routes_to_reviewed_task_block_handler(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "review_handler_task": "review_gate",
            },
            "initial_tasks": ["main_task"],
            "tasks": {
                "main_task": {
                    "block_handler_task": "block_triage",
                    "expected_next_tasks": ["next_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Main task completed and awaits review.",
                            "needs_human": False,
                            "next_tasks": ["next_task"],
                            "artifacts": [],
                        }
                    },
                },
                "review_gate": {
                    "max_attempts": 2,
                    "review_handler_task": None,
                    "block_handler_task": None,
                    "mock": {
                        "exit_code": 1,
                    },
                },
                "block_triage": {
                    "block_handler_task": None,
                    "review_handler_task": None,
                    "expected_next_tasks": ["recovery_task"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Block triage redirected the run to a recovery task.",
                            "needs_human": False,
                            "next_tasks": ["recovery_task"],
                            "artifacts": [],
                        }
                    },
                },
                "recovery_task": {
                    "review_handler_task": None,
                    "expected_next_tasks": [],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Recovery task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    },
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "review-failed.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            completed = subprocess.run(
                [
                    sys.executable,
                    str(ORCHESTRATOR),
                    "run",
                    "--workflow",
                    str(workflow_path),
                    "--state-dir",
                    str(state_dir),
                    "--runner",
                    "mock",
                ],
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["main_task"]["status"], "review_failed_routed")
            self.assertEqual(state["tasks"]["main_task"]["review_status"], "review_failed_routed")
            self.assertEqual(state["tasks"]["review_gate"]["status"], "review_resolution_routed")
            self.assertEqual(len(state["tasks"]["review_gate"]["history"]), 2)
            self.assertEqual(state["tasks"]["block_triage"]["status"], "completed")
            self.assertEqual(state["tasks"]["recovery_task"]["status"], "completed")
            block_context_path = Path(state["active_block_context_path"])
            block_context = load_json(block_context_path)
            self.assertEqual(block_context["blocked_task_id"], "main_task")
            self.assertEqual(block_context["blocked_status"], "review_failed")
            self.assertEqual(block_context["reviewer_task_id"], "review_gate")

    def test_block_handler_task_can_be_reused_after_completion(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "block_handler_task": "block_triage",
            },
            "initial_tasks": ["first_blocked"],
            "tasks": {
                "first_blocked": {
                    "mock": {
                        "result": {
                            "status": "blocked",
                            "summary": "First task hit a semantic block.",
                            "needs_human": True,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    }
                },
                "block_triage": {
                    "block_handler_task": None,
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Block handler routed the run back into the queue.",
                            "needs_human": False,
                            "next_tasks": ["recovered_task"],
                            "artifacts": [],
                        }
                    }
                },
                "recovered_task": {
                    "expected_next_tasks": ["second_blocked"],
                    "mock": {
                        "result": {
                            "status": "completed",
                            "summary": "Recovery task completed.",
                            "needs_human": False,
                            "next_tasks": ["second_blocked"],
                            "artifacts": [],
                        }
                    }
                },
                "second_blocked": {
                    "mock": {
                        "result": {
                            "status": "blocked",
                            "summary": "Second task hit a semantic block.",
                            "needs_human": True,
                            "next_tasks": [],
                            "artifacts": [],
                        }
                    }
                },
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "reused-block-handler.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            command = [
                sys.executable,
                str(ORCHESTRATOR),
                "run",
                "--workflow",
                str(workflow_path),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
            ]
            completed = subprocess.run(
                command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            state = load_json(state_dir / "state.json")
            self.assertEqual(state["tasks"]["first_blocked"]["status"], "blocked_routed")
            self.assertEqual(state["tasks"]["second_blocked"]["status"], "blocked_routed")
            self.assertEqual(state["tasks"]["block_triage"]["status"], "completed")
            self.assertEqual(len(state["tasks"]["block_triage"]["history"]), 2)
            self.assertEqual(len(state["block_context_history"]), 2)

    def test_launch_status_stop_manage_background_run(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "max_attempts": 1,
            },
            "initial_tasks": ["slow_task"],
            "tasks": {
                "slow_task": {
                    "mock": {
                        "sleep_seconds": 8.0,
                        "result": {
                            "status": "completed",
                            "summary": "Slow task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        },
                    }
                }
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "background.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            state_dir = temp_path / "state"
            launch_command = [
                sys.executable,
                str(ORCHESTRATOR),
                "launch",
                "--workflow",
                str(workflow_path),
                "--state-dir",
                str(state_dir),
                "--runner",
                "mock",
                "--poll-interval-seconds",
                "0.1",
            ]
            launched = subprocess.run(
                launch_command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(launched.returncode, 0, launched.stderr)
            launch_payload = json.loads(launched.stdout)
            self.assertIn("orchestrator_pid", launch_payload)

            status_payload = None
            for _ in range(30):
                time.sleep(0.2)
                status_command = [
                    sys.executable,
                    str(ORCHESTRATOR),
                    "status",
                    "--state-dir",
                    str(state_dir),
                    "--json",
                ]
                status = subprocess.run(
                    status_command,
                    cwd=str(REPO_ROOT),
                    capture_output=True,
                    text=True,
                    check=False,
                )
                self.assertEqual(status.returncode, 0, status.stderr)
                status_payload = json.loads(status.stdout)
                if status_payload["run_status"] == "running":
                    break
            self.assertIsNotNone(status_payload)
            self.assertEqual(status_payload["run_status"], "running")
            self.assertIn("slow_task", status_payload["running_tasks"])

            stop_command = [
                sys.executable,
                str(ORCHESTRATOR),
                "stop",
                "--state-dir",
                str(state_dir),
            ]
            stopped = subprocess.run(
                stop_command,
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(stopped.returncode, 0, stopped.stderr)

            final_status = subprocess.run(
                [
                    sys.executable,
                    str(ORCHESTRATOR),
                    "status",
                    "--state-dir",
                    str(state_dir),
                    "--json",
                ],
                cwd=str(REPO_ROOT),
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(final_status.returncode, 0, final_status.stderr)
            final_payload = json.loads(final_status.stdout)
            self.assertEqual(final_payload["run_status"], "stopped")

    def test_launch_stops_previous_background_run(self) -> None:
        workflow = {
            "defaults": {
                "runner": "mock",
                "max_attempts": 1,
            },
            "initial_tasks": ["slow_task"],
            "tasks": {
                "slow_task": {
                    "mock": {
                        "sleep_seconds": 10.0,
                        "result": {
                            "status": "completed",
                            "summary": "Slow task completed.",
                            "needs_human": False,
                            "next_tasks": [],
                            "artifacts": [],
                        },
                    }
                }
            },
        }
        with workspace_temp_dir() as temp_dir:
            temp_path = Path(temp_dir)
            workflow_path = temp_path / "single-run.json"
            workflow_path.write_text(json.dumps(workflow, indent=2), encoding="utf-8")
            first_state_dir = temp_path / "run_a"
            second_state_dir = temp_path / "run_b"
            try:
                first_launch = subprocess.run(
                    [
                        sys.executable,
                        str(ORCHESTRATOR),
                        "launch",
                        "--workflow",
                        str(workflow_path),
                        "--state-dir",
                        str(first_state_dir),
                        "--runner",
                        "mock",
                        "--poll-interval-seconds",
                        "0.1",
                    ],
                    cwd=str(REPO_ROOT),
                    capture_output=True,
                    text=True,
                    check=False,
                )
                self.assertEqual(first_launch.returncode, 0, first_launch.stderr)
                time.sleep(0.8)

                second_launch = subprocess.run(
                    [
                        sys.executable,
                        str(ORCHESTRATOR),
                        "launch",
                        "--workflow",
                        str(workflow_path),
                        "--state-dir",
                        str(second_state_dir),
                        "--runner",
                        "mock",
                        "--poll-interval-seconds",
                        "0.1",
                    ],
                    cwd=str(REPO_ROOT),
                    capture_output=True,
                    text=True,
                    check=False,
                )
                self.assertEqual(second_launch.returncode, 0, second_launch.stderr)
                second_payload = json.loads(second_launch.stdout)
                stopped_paths = {Path(path).resolve() for path in second_payload["stopped_prior_runs"]}
                self.assertIn(first_state_dir.resolve(), stopped_paths)

                first_status = subprocess.run(
                    [
                        sys.executable,
                        str(ORCHESTRATOR),
                        "status",
                        "--state-dir",
                        str(first_state_dir),
                        "--json",
                    ],
                    cwd=str(REPO_ROOT),
                    capture_output=True,
                    text=True,
                    check=False,
                )
                self.assertEqual(first_status.returncode, 0, first_status.stderr)
                first_payload = json.loads(first_status.stdout)
                self.assertEqual(first_payload["run_status"], "stopped")
            finally:
                if second_state_dir.exists():
                    subprocess.run(
                        [
                            sys.executable,
                            str(ORCHESTRATOR),
                            "stop",
                            "--state-dir",
                            str(second_state_dir),
                        ],
                        cwd=str(REPO_ROOT),
                        capture_output=True,
                        text=True,
                        check=False,
                    )

    def test_u_primary_workflow_stays_in_sync_with_scope_authority_docs(self) -> None:
        workflow = load_json(U_PRIMARY_WORKFLOW)
        metadata = workflow["metadata"]
        scope_prompt = workflow["tasks"]["u_scope_freeze_gate"]["prompt"]
        foundation_prompt = workflow["tasks"]["u_runtime_foundation"]["prompt"]
        doc_root = "lila-docker/repos/lila/modules/commentary/docs/"
        matrix_rows = parse_descriptor_owner_rows(DESCRIPTOR_MATRIX)
        ledger_text = load_text(DECISION_LEDGER)
        witnesses_text = load_text(WITNESSES_61)
        validation_text = load_text(VALIDATION_METHODOLOGY)
        core_ssot_text = load_text(COMMENTARY_CORE_SSOT)

        active_rows = {
            row["owner_home"]
            for row in matrix_rows.values()
            if row["owner_layer"] == "Witness / U-primary"
        }
        self.assertEqual(set(metadata["active_u_primary_rows"]), active_rows)
        self.assertEqual(len(active_rows), 18)

        for descriptor in metadata["banned_rehome_rows"]:
            self.assertIn(descriptor, matrix_rows)
            self.assertNotEqual(matrix_rows[descriptor]["owner_layer"], "Witness / U-primary")
            self.assertIn(f"`{descriptor}`", ledger_text)
        self.assertIn("must not re-enter `U`", ledger_text)
        self.assertIn("must not be revived as a raw `U-primary` witness", ledger_text)

        for descriptor in metadata["host_shell_only_rows"]:
            self.assertEqual(matrix_rows[descriptor]["owner_layer"], "Witness / U-attached")
            self.assertIn("host shell", matrix_rows[descriptor]["owner_home"])
        self.assertIn("### Closed Center Host-Shell Reclassification", witnesses_text)
        self.assertIn("### Fixed Chain Host-Shell Reclassification", witnesses_text)
        self.assertIn("must remain host-shell vocabulary only", ledger_text)

        for seed_family in metadata["blocked_strategy_seed_families"]:
            self.assertIn(f"`{seed_family}`", ledger_text)
        self.assertIn(
            "frozen as required future contracts, not as active runtime contracts",
            ledger_text,
        )
        self.assertIn("blocked S23/S24 support seeds remain out of U", scope_prompt)
        self.assertIn("upper-layer-owned inventory entries do not leak back in", validation_text)
        self.assertIn("witness verdicts", validation_text)
        self.assertIn("most static witness admission", validation_text)
        self.assertIn(f"{doc_root}CommentaryCoreSSOT.md", scope_prompt)
        self.assertIn(f"{doc_root}CommentaryCoreSSOT.md", foundation_prompt)
        self.assertIn("object -> delta -> certification", core_ssot_text)
        self.assertIn("inventory label `closed center`", core_ssot_text)
        self.assertIn("inventory label `fixed chain`", core_ssot_text)
        self.assertIn("inventory label `central tension`", core_ssot_text)

        self.assertIn("active U-primary 18", scope_prompt)
        self.assertIn("rehome rows do not re-enter U", scope_prompt)
        self.assertIn("closed center and fixed chain stay host-shell only", scope_prompt)

        bad_doc_root = re.compile(r"(?<!lila-docker/repos/lila/)modules/commentary/docs/")
        for task in workflow["tasks"].values():
            prompt = task.get("prompt", "")
            if "commentary/docs/" in prompt:
                self.assertIn(doc_root, prompt)
            self.assertIsNone(bad_doc_root.search(prompt))


if __name__ == "__main__":
    unittest.main()
