#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


SECTION_TITLES = {
    "Required always",
    "Required only in selected modes",
    "Soft optional integrations",
    "Removed / dormant bindings",
}


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def iter_env_entries(node: Any) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "env" and isinstance(value, list):
                entries.extend(item for item in value if isinstance(item, dict))
            else:
                entries.extend(iter_env_entries(value))
    elif isinstance(node, list):
        for item in node:
            entries.extend(iter_env_entries(item))
    return entries


def has_secret_ref(node: Any) -> bool:
    if isinstance(node, dict):
        if "secretKeyRef" in node:
            return True
        return any(has_secret_ref(value) for value in node.values())
    if isinstance(node, list):
        return any(has_secret_ref(item) for item in node)
    return False


def env_is_configured(entry: dict[str, Any]) -> bool:
    value = entry.get("value")
    if isinstance(value, str) and value.strip():
        return True
    return has_secret_ref(entry.get("valueFrom")) or has_secret_ref(entry.get("valueSource"))


def extract_service_envs(service: dict[str, Any]) -> dict[str, bool]:
    envs: dict[str, bool] = {}
    for entry in iter_env_entries(service):
        name = entry.get("name")
        if isinstance(name, str) and name:
            envs[name] = envs.get(name, False) or env_is_configured(entry)
    return envs


def is_required(spec: dict[str, Any], envs: dict[str, bool]) -> bool:
    mode = spec["requiredMode"]
    if mode == "always":
        return True
    if mode == "dispatch_only":
        return envs.get("ACCOUNT_INTEL_DISPATCH_BASE_URL", False)
    if mode == "selective_eval_only":
        return envs.get("ACCOUNT_INTEL_SELECTIVE_EVAL_ENDPOINT", False)
    return False


def cloud_run_managed(spec: dict[str, Any]) -> bool:
    return bool(spec.get("cloudRunManaged", True))


def evaluate_manifest(manifest: dict[str, Any], envs: dict[str, bool]) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    specs = manifest["bindings"]
    known_envs = {spec["env"] for spec in specs}
    removed_envs = set(manifest.get("removedBindings", []))

    for spec in specs:
        env_name = spec["env"]
        if cloud_run_managed(spec) and is_required(spec, envs) and not envs.get(env_name, False):
            errors.append(
                f"Missing required binding {env_name} ({spec['configPath'] or 'env-only'})"
            )

    for env_name in sorted(removed_envs):
        if envs.get(env_name, False):
            warnings.append(f"Removed binding still configured: {env_name}")

    unknown = sorted(name for name, configured in envs.items() if configured and name not in known_envs and name not in removed_envs)
    for env_name in unknown:
        warnings.append(f"Configured env is not tracked by openbeta manifest: {env_name}")

    return errors, warnings


def extract_documented_envs(doc_text: str) -> set[str]:
    documented: set[str] = set()
    current_section: str | None = None
    for raw_line in doc_text.splitlines():
        line = raw_line.strip()
        if line.startswith("## "):
            title = line[3:].strip()
            current_section = title if title in SECTION_TITLES else None
            continue
        if current_section is None:
            continue
        documented.update(re.findall(r"`([A-Z][A-Z0-9_]+)`", line))
    return documented


def compare_doc(manifest: dict[str, Any], doc_text: str) -> tuple[list[str], list[str]]:
    expected = {
        spec["env"] for spec in manifest["bindings"] if cloud_run_managed(spec)
    } | set(manifest.get("removedBindings", []))
    documented = extract_documented_envs(doc_text)
    missing = sorted(expected - documented)
    extra = sorted(documented - expected)
    errors = [f"Runtime binding missing from OPENBETA_GCP.md: {env_name}" for env_name in missing]
    warnings = [f"OPENBETA_GCP.md documents env not present in manifest: {env_name}" for env_name in extra]
    return errors, warnings


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate open-beta runtime bindings against Cloud Run service config.")
    parser.add_argument("--manifest", required=True, type=Path, help="Path to openbeta-bindings.json")
    parser.add_argument("--service-json", required=True, type=Path, help="Path to gcloud run services describe JSON")
    parser.add_argument("--doc", type=Path, help="Optional OPENBETA_GCP.md path for doc drift checks")
    args = parser.parse_args(argv)

    manifest = load_json(args.manifest)
    service = load_json(args.service_json)
    envs = extract_service_envs(service)

    errors, warnings = evaluate_manifest(manifest, envs)
    if args.doc:
        doc_errors, doc_warnings = compare_doc(manifest, args.doc.read_text(encoding="utf-8-sig"))
        errors.extend(doc_errors)
        warnings.extend(doc_warnings)

    for warning in warnings:
        print(f"WARNING: {warning}", file=sys.stderr)
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)

    if errors:
        return 1

    print("Open-beta binding validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
