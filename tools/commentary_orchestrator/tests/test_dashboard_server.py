import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(r"C:\Codes\CondensedChess")
TOOL_DIR = REPO_ROOT / "tools" / "commentary_orchestrator"
ORCHESTRATOR = TOOL_DIR / "orchestrator.py"
SMOKE_WORKFLOW = TOOL_DIR / "samples" / "r_to_u_smoke.json"
sys.path.insert(0, str(TOOL_DIR))

from dashboard_server import build_run_details, discover_state_dirs, get_task_log


class DashboardServerTest(unittest.TestCase):
    def test_dashboard_helpers_surface_runs_and_logs(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            state_dir = temp_path / "smoke_state"
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

            state_dirs = discover_state_dirs(temp_path)
            self.assertEqual(state_dirs, [state_dir.resolve()])

            details = build_run_details(state_dir)
            self.assertEqual(details["summary"]["run_status"], "completed")
            task_ids = [task["task_id"] for task in details["tasks"]]
            self.assertIn("r_space_frontier", task_ids)
            self.assertIn("u_file_lane_contract", task_ids)

            result_log = get_task_log(state_dir, "r_space_frontier", "result", 40)
            self.assertIn('"status": "completed"', result_log["content"])


if __name__ == "__main__":
    unittest.main()
