import argparse
import json
import math
from collections import Counter, defaultdict
from pathlib import Path
from typing import Iterable


FORBIDDEN_ROW_FIELDS = {
    "bestMove",
    "bestLine",
    "theoryTruth",
    "forcedLine",
    "forcedContinuation",
    "objectiveResult",
    "objectiveVerdict",
    "engineVerdict",
    "engineProof",
    "oracleVerdict",
}
EXPECTED = {
    "master": {"sourceUse": "master_reference", "aggregateUse": "master_reference_stat"},
    "online": {"sourceUse": "online_trend", "aggregateUse": "online_trend_stat"},
}
DEFAULT_CONFIDENCE_THRESHOLD = 5


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def read_jsonl(path: Path) -> list[dict]:
    rows = []
    with path.open(encoding="utf-8") as f:
        for line in f:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def write_jsonl(path: Path, rows: Iterable[dict]) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with path.open("w", encoding="utf-8", newline="\n") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n")
            count += 1
    return count


def load_contract_rows(path: Path | None) -> dict[str, dict]:
    if not path:
        return {}
    return {row["sourceId"]: row for row in read_jsonl(path) if row.get("sourceId")}


def effective_manifest(local: dict, contract_rows: dict[str, dict], role: str, warnings: list[dict]) -> dict:
    source_id = local.get("sourceId")
    contract = contract_rows.get(source_id, {})
    expected = EXPECTED[role]
    effective = dict(local)
    for field in ["sourceUse", "aggregateUse"]:
        local_value = local.get(field)
        contract_value = contract.get(field)
        if local_value == expected[field]:
            effective[field] = local_value
        elif contract_value == expected[field]:
            effective[field] = contract_value
            warnings.append(
                {
                    "sourceId": source_id,
                    "field": field,
                    "localValue": local_value,
                    "contractValue": contract_value,
                    "effectiveValue": contract_value,
                    "reason": "local_manifest_does_not_match_current_contract",
                }
            )
        else:
            raise ValueError(f"{role} source {source_id} cannot resolve {field}={expected[field]}")
    return effective


def validate_manifest(role: str, manifest: dict) -> None:
    expected = EXPECTED[role]
    for field, value in expected.items():
        if manifest.get(field) != value:
            raise ValueError(f"{role} manifest must declare {field}={value}")
    if not manifest.get("sourceChecksum"):
        raise ValueError(f"{role} manifest must declare sourceChecksum")
    if role == "master":
        for field in ["attributionText", "licenseNotice"]:
            if not manifest.get(field):
                raise ValueError(f"master manifest must declare {field}")


def validate_local_path(path: Path) -> str:
    resolved = path.resolve()
    allowed_root = (Path.cwd() / "tmp" / "commentary-opening" / "reports").resolve()
    try:
        resolved.relative_to(allowed_root)
    except ValueError as error:
        raise ValueError(f"comparison output path must stay under {allowed_root}: {path}") from error
    return str(resolved).replace("\\", "/")


def normalize_rows(rows: list[dict], manifest: dict, role: str) -> tuple[list[dict], Counter]:
    expected = EXPECTED[role]
    warnings = Counter()
    normalized = []
    for row in rows:
        forbidden = FORBIDDEN_ROW_FIELDS.intersection(row)
        if forbidden:
            raise ValueError(f"{role} row {row.get('id')} carries forbidden truth fields: {sorted(forbidden)}")
        if row.get("candidateKind") not in {"statistical_reference", "context_reference"}:
            raise ValueError(f"{role} row {row.get('id')} has unsupported candidateKind {row.get('candidateKind')}")
        if row.get("currentPositionTruth") not in (None, False):
            raise ValueError(f"{role} row {row.get('id')} claims currentPositionTruth")
        copy = dict(row)
        for field, value in expected.items():
            if copy.get(field) is None:
                warnings[f"missing_{field}"] += 1
                copy[field] = value
            elif copy[field] != value:
                raise ValueError(f"{role} row {row.get('id')} has {field}={copy[field]} expected {value}")
        if copy.get("sourceId") != manifest.get("sourceId"):
            raise ValueError(f"{role} row {row.get('id')} sourceId mismatch")
        normalized.append(copy)
    return normalized, warnings


def group_by_position(rows: list[dict]) -> dict[str, list[dict]]:
    grouped: dict[str, list[dict]] = defaultdict(list)
    for row in rows:
        key = row.get("positionKey")
        if not key:
            raise ValueError(f"row {row.get('id')} missing positionKey")
        grouped[key].append(row)
    return dict(grouped)


def position_sample_size(rows: list[dict]) -> int:
    declared = [row.get("positionSampleSize") for row in rows if isinstance(row.get("positionSampleSize"), int)]
    if declared:
        return max(declared)
    summed = sum(int(row.get("sampleSize", 0)) for row in rows)
    inferred = []
    for row in rows:
        frequency = float(row.get("frequency", 0.0) or 0.0)
        sample_size = int(row.get("sampleSize", 0) or 0)
        if frequency > 0.0 and sample_size > 0:
            inferred.append(max(1, int(round(sample_size / frequency))))
    return max([summed] + inferred)


def most_frequent_row(rows: list[dict]) -> dict:
    return max(rows, key=lambda row: (int(row.get("sampleSize", 0)), float(row.get("frequency", 0.0)), row.get("move", "")))


def confidence_for(rows: list[dict], threshold: int = DEFAULT_CONFIDENCE_THRESHOLD) -> str:
    declared = {row.get("sampleConfidence") for row in rows if row.get("sampleConfidence")}
    if "meets_threshold" in declared:
        return "meets_threshold"
    if "below_threshold" in declared:
        return "below_threshold"
    return "meets_threshold" if position_sample_size(rows) >= threshold else "below_threshold"


def confidence_summary(grouped: dict[str, list[dict]], threshold: int = DEFAULT_CONFIDENCE_THRESHOLD) -> dict:
    position_counts = Counter()
    row_counts = Counter()
    for rows in grouped.values():
        tier = confidence_for(rows, threshold)
        position_counts[tier] += 1
        row_counts[tier] += len(rows)
    return {
        "positions": dict(sorted(position_counts.items())),
        "rows": dict(sorted(row_counts.items())),
    }


def source_only_sample(grouped: dict[str, list[dict]], only_keys: set[str], limit: int = 50) -> list[dict]:
    rows = []
    for key in sorted(only_keys):
        items = grouped[key]
        most_frequent = most_frequent_row(items)
        rows.append(
            {
                "positionKey": key,
                "mostFrequentMove": most_frequent.get("move"),
                "mostFrequentMoveFrequency": most_frequent.get("frequency"),
                "positionSampleSize": position_sample_size(items),
                "confidence": confidence_for(items),
            }
        )
        if len(rows) >= limit:
            break
    return rows


def frequency_disagreement_examples(
    master_grouped: dict[str, list[dict]],
    online_grouped: dict[str, list[dict]],
    common: set[str],
    limit: int = 25,
) -> tuple[int, int, list[dict]]:
    all_count = 0
    high_confidence = []
    for key in common:
        master_frequent = most_frequent_row(master_grouped[key])
        online_frequent = most_frequent_row(online_grouped[key])
        if master_frequent["move"] == online_frequent["move"]:
            continue
        all_count += 1
        master_confidence = confidence_for(master_grouped[key])
        online_confidence = confidence_for(online_grouped[key])
        if master_confidence != "meets_threshold" or online_confidence != "meets_threshold":
            continue
        master_freq = float(master_frequent.get("frequency", 0.0))
        online_freq = float(online_frequent.get("frequency", 0.0))
        master_samples = position_sample_size(master_grouped[key])
        online_samples = position_sample_size(online_grouped[key])
        score = abs(master_freq - online_freq) + math.log1p(min(master_samples, online_samples)) / 10.0
        high_confidence.append(
            {
                "positionKey": key,
                "masterMostFrequentMove": master_frequent["move"],
                "masterMostFrequentMoveFrequency": master_freq,
                "masterMostFrequentMoveSampleSize": master_frequent.get("sampleSize"),
                "masterPositionSampleSize": master_samples,
                "masterConfidence": master_confidence,
                "onlineMostFrequentMove": online_frequent["move"],
                "onlineMostFrequentMoveFrequency": online_freq,
                "onlineMostFrequentMoveSampleSize": online_frequent.get("sampleSize"),
                "onlinePositionSampleSize": online_samples,
                "onlineConfidence": online_confidence,
                "frequencyGapAuditScore": round(score, 6),
                "boundary": "ranking_not_merged; frequency_gap_is_source_quality_context_not_best_move",
            }
        )
    high_confidence_sorted = sorted(high_confidence, key=lambda row: row["frequencyGapAuditScore"], reverse=True)
    return all_count, len(high_confidence_sorted), high_confidence_sorted[:limit]


def low_confidence_rows(grouped: dict[str, list[dict]], role: str, limit: int = 500) -> list[dict]:
    rows = []
    for key in sorted(grouped):
        items = grouped[key]
        if confidence_for(items) != "below_threshold":
            continue
        for row in sorted(items, key=lambda value: value.get("id", "")):
            rows.append(
                {
                    "sourceRole": role,
                    "rowId": row.get("id"),
                    "positionKey": key,
                    "move": row.get("move"),
                    "sampleSize": row.get("sampleSize"),
                    "positionSampleSize": position_sample_size(items),
                    "confidence": "below_threshold",
                }
            )
            if len(rows) >= limit:
                return rows
    return rows


def low_confidence_row_count(grouped: dict[str, list[dict]]) -> int:
    count = 0
    for items in grouped.values():
        if confidence_for(items) == "below_threshold":
            count += len(items)
    return count


def compare_sources(
    *,
    master_manifest_path: Path,
    master_rows_path: Path,
    online_manifest_path: Path,
    online_rows_path: Path,
    contract_manifest_path: Path | None,
    output_dir: Path,
) -> dict:
    validate_local_path(output_dir)
    manifest_warnings: list[dict] = []
    contract_rows = load_contract_rows(contract_manifest_path)
    master_manifest = effective_manifest(read_json(master_manifest_path), contract_rows, "master", manifest_warnings)
    online_manifest = effective_manifest(read_json(online_manifest_path), contract_rows, "online", manifest_warnings)
    validate_manifest("master", master_manifest)
    validate_manifest("online", online_manifest)

    master_rows, master_row_warnings = normalize_rows(read_jsonl(master_rows_path), master_manifest, "master")
    online_rows, online_row_warnings = normalize_rows(read_jsonl(online_rows_path), online_manifest, "online")

    master_grouped = group_by_position(master_rows)
    online_grouped = group_by_position(online_rows)
    master_positions = set(master_grouped)
    online_positions = set(online_grouped)
    common = master_positions & online_positions
    master_only = master_positions - online_positions
    online_only = online_positions - master_positions

    disagreement_total, high_confidence_disagreement_total, examples = frequency_disagreement_examples(master_grouped, online_grouped, common)
    low_sample_limit = 500
    low_rows = low_confidence_rows(master_grouped, "master_reference", low_sample_limit) + low_confidence_rows(
        online_grouped, "online_trend", low_sample_limit
    )
    low_summary_master = confidence_summary(master_grouped)
    low_summary_online = confidence_summary(online_grouped)
    total_low_rows = low_confidence_row_count(master_grouped) + low_confidence_row_count(online_grouped)

    report = {
        "summary": {
            "masterSourceId": master_manifest["sourceId"],
            "onlineSourceId": online_manifest["sourceId"],
            "commonPositionCount": len(common),
            "masterOnlyPositionCount": len(master_only),
            "onlineOnlyPositionCount": len(online_only),
            "masterMoveStatRows": len(master_rows),
            "onlineMoveStatRows": len(online_rows),
            "frequencyDisagreementTotalCount": disagreement_total,
            "highConfidenceFrequencyDisagreementCount": high_confidence_disagreement_total,
            "frequencyDisagreementExampleCount": len(examples),
        },
        "sourceBoundary": {
            "master": {
                "sourceUse": master_manifest["sourceUse"],
                "aggregateUse": master_manifest["aggregateUse"],
                "sourceChecksum": master_manifest.get("sourceChecksum"),
                "licenseNotice": master_manifest.get("licenseNotice"),
                "attributionText": master_manifest.get("attributionText"),
            },
            "online": {
                "sourceUse": online_manifest["sourceUse"],
                "aggregateUse": online_manifest["aggregateUse"],
                "sourceChecksum": online_manifest.get("sourceChecksum"),
            },
            "pipelineSmoke": "pipeline_smoke is excluded from this comparison and must not be reported as trend or master reference",
            "rankingPolicy": "do_not_merge_rankings",
            "truthBoundary": "move frequencies are source-backed statistics only; engine/certification is required for best or objective claims",
        },
        "manifestWarnings": manifest_warnings,
        "rowWarnings": {
            "master": dict(sorted(master_row_warnings.items())),
            "online": dict(sorted(online_row_warnings.items())),
        },
        "lowConfidence": {
            "masterRows": low_summary_master["rows"].get("below_threshold", 0),
            "onlineRows": low_summary_online["rows"].get("below_threshold", 0),
            "masterPositions": low_summary_master["positions"].get("below_threshold", 0),
            "onlinePositions": low_summary_online["positions"].get("below_threshold", 0),
            "threshold": DEFAULT_CONFIDENCE_THRESHOLD,
            "sampleLimitPerSource": low_sample_limit,
            "sampleRowsWritten": len(low_rows),
            "sampleCapped": len(low_rows) < total_low_rows,
        },
        "sourceOnlySamples": {
            "master": source_only_sample(master_grouped, master_only),
            "online": source_only_sample(online_grouped, online_only),
        },
        "frequencyDisagreementExamples": examples,
        "recommendedSourcePolicy": {
            "defaultEducationalSource": "master_reference",
            "secondaryContextSource": "online_trend",
            "pipelineSmokeUse": "pipeline_and_validator_smoke_only",
            "objectiveClaimRequirement": "engine_or_certification_evidence_required",
        },
        "artifacts": {
            "summary": str((output_dir / "comparison-summary.json")).replace("\\", "/"),
            "frequencyDisagreementExamples": str((output_dir / "frequency-disagreements.jsonl")).replace("\\", "/"),
            "lowConfidenceRowSample": str((output_dir / "low-confidence-row-sample.jsonl")).replace("\\", "/"),
        },
    }

    write_json(output_dir / "comparison-summary.json", report)
    write_jsonl(output_dir / "frequency-disagreements.jsonl", examples)
    write_jsonl(output_dir / "low-confidence-row-sample.jsonl", low_rows)
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare local-only opening master-reference and online-trend move statistics.")
    parser.add_argument("--master-manifest", required=True, type=Path)
    parser.add_argument("--master-rows", required=True, type=Path)
    parser.add_argument("--online-manifest", required=True, type=Path)
    parser.add_argument("--online-rows", required=True, type=Path)
    parser.add_argument("--contract-manifest", type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()
    report = compare_sources(
        master_manifest_path=args.master_manifest,
        master_rows_path=args.master_rows,
        online_manifest_path=args.online_manifest,
        online_rows_path=args.online_rows,
        contract_manifest_path=args.contract_manifest,
        output_dir=args.output_dir,
    )
    print(json.dumps(report["summary"], ensure_ascii=False, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
