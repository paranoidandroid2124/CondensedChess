from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "validate_openbeta_bindings.py"
DOC = ROOT.parents[2] / "OPENBETA_GCP.md"
MANIFEST = ROOT / "conf" / "openbeta-bindings.json"

spec = importlib.util.spec_from_file_location("validate_openbeta_bindings", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)


def service_json(*env_entries: dict[str, object]) -> dict[str, object]:
    return {
        "spec": {
            "template": {
                "spec": {
                    "containers": [
                        {
                            "env": list(env_entries)
                        }
                    ]
                }
            }
        }
    }


def plain(name: str, value: str) -> dict[str, object]:
    return {"name": name, "value": value}


def secret(name: str, secret_name: str) -> dict[str, object]:
    return {
        "name": name,
        "valueFrom": {
            "secretKeyRef": {
                "name": secret_name,
                "key": "latest",
            }
        },
    }


class ValidateOpenBetaBindingsTest(unittest.TestCase):
    maxDiff = None

    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))

    def test_all_required_always_bindings_pass(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always"
        }
        errors, warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertEqual(errors, [])
        self.assertEqual(warnings, [])

    def test_missing_required_binding_fails(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always"
        }
        envs["EXTERNAL_ENGINE_ENDPOINT"] = False
        errors, _warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertTrue(any("EXTERNAL_ENGINE_ENDPOINT" in err for err in errors))

    def test_dispatch_disabled_does_not_require_dispatch_bindings(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always"
        }
        errors, warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertEqual(errors, [])
        self.assertEqual(warnings, [])

    def test_hardcoded_runtime_bindings_do_not_require_cloud_run_env(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always" and spec.get("cloudRunManaged", True)
        }
        errors, warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertEqual(errors, [])
        self.assertEqual(warnings, [])

    def test_dispatch_enabled_without_token_fails(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always"
        }
        envs["ACCOUNT_INTEL_DISPATCH_BASE_URL"] = True
        envs["ACCOUNT_INTEL_DISPATCH_BEARER_TOKEN"] = False
        envs["ACCOUNT_INTEL_WORKER_TOKEN"] = False
        errors, _warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertTrue(any("ACCOUNT_INTEL_DISPATCH_BEARER_TOKEN" in err for err in errors))
        self.assertTrue(any("ACCOUNT_INTEL_WORKER_TOKEN" in err for err in errors))

    def test_removed_binding_only_warns(self) -> None:
        envs = {
            spec["env"]: True
            for spec in self.manifest["bindings"]
            if spec["requiredMode"] == "always"
        }
        envs["PUSH_WEB_URL"] = True
        errors, warnings = module.evaluate_manifest(self.manifest, envs)
        self.assertEqual(errors, [])
        self.assertTrue(any("PUSH_WEB_URL" in warning for warning in warnings))

    def test_service_json_env_extraction_supports_plain_and_secret_bindings(self) -> None:
        service = service_json(
            plain("LILA_DOMAIN", "beta.chesstory.com"),
            secret("PLAY_HTTP_SECRET_KEY", "play-http-secret"),
            plain("PUSH_WEB_URL", ""),
        )
        envs = module.extract_service_envs(service)
        self.assertTrue(envs["LILA_DOMAIN"])
        self.assertTrue(envs["PLAY_HTTP_SECRET_KEY"])
        self.assertFalse(envs["PUSH_WEB_URL"])

    def test_doc_matches_manifest(self) -> None:
        errors, warnings = module.compare_doc(self.manifest, DOC.read_text(encoding="utf-8"))
        self.assertEqual(errors, [])
        self.assertEqual(warnings, [])

    def test_main_cli_returns_failure_when_required_binding_missing(self) -> None:
        service = service_json(plain("LILA_DOMAIN", "beta.chesstory.com"))
        with tempfile.TemporaryDirectory() as tmpdir:
            service_path = Path(tmpdir) / "service.json"
            service_path.write_text(json.dumps(service), encoding="utf-8")
            exit_code = module.main(
                [
                    "--manifest",
                    str(MANIFEST),
                    "--service-json",
                    str(service_path),
                ]
            )
        self.assertEqual(exit_code, 1)


if __name__ == "__main__":
    unittest.main()
