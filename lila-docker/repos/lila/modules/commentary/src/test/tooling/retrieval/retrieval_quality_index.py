import argparse
import json
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[6]
ALLOWED_OUTPUT_ROOT = REPO_ROOT / "tmp" / "commentary-opening" / "retrieval"
SIMILARITY_FLOOR = 0.65
EXACT_SIMILARITY_FLOOR = 0.95
FORBIDDEN_WORDS = (
    "best",
    "theory",
    "forced",
    "result",
    "outcome",
    "engine",
    "oracle",
    "tablebase",
    "winning",
    "win",
    "draw",
    "loss",
    "won",
    "lost",
    "proof",
    "current position",
    "current-position",
    "current_position",
    "current position proof",
    "current-position proof",
    "current_position_truth",
    "truth",
    "famous player recommendation",
)
FORBIDDEN_CODED_RESULT_RE = re.compile(r"(^|[^a-z0-9])(1-0|0-1|1/2-1/2|wdl|dtz|dtm)([^a-z0-9]|$)")
ALLOWED_TOP_LEVEL_FIELDS = {
    "retrievalId",
    "sourceId",
    "sourceRef",
    "exampleKind",
    "fen",
    "positionKey",
    "sideToMove",
    "similarityKey",
    "similarityScore",
    "similarityKind",
    "matchedFeatures",
    "sourceQuality",
    "gameMetadata",
    "licenseNotice",
    "attributionText",
    "snippetRole",
    "currentPositionClaim",
    "authority",
    "tags",
    "negativeBoundaries",
    "expectation",
    "rejectReason",
    "snippetText",
}
ALLOWED_CITATION_FIELDS = {"players", "event", "date", "round", "result", "url"}
ALLOWED_EXAMPLE_KINDS = {"curated_reference", "broadcast_game_reference", "puzzle_reference", "study_reference", "educational_reference"}
ALLOWED_SIMILARITY_KINDS = {"exact_position", "opening_context", "motif_context", "endgame_study_context", "mixed_feature_context"}
ALLOWED_SOURCE_QUALITIES = {"local_curated", "manifest_backed", "attribution_required", "public_fact"}
SIMILARITY_FIELDS_BY_KIND = {
    "exact_position": {"positionKey", "sideToMove", "materialClass", "materialSignature", "openingFamily", "openingAlias", "pawnStructure"},
    "opening_context": {
        "positionKey",
        "sideToMove",
        "materialClass",
        "materialSignature",
        "openingFamily",
        "openingVariation",
        "openingAlias",
        "pawnStructure",
        "sourceRefs",
        "aliasId",
    },
    "motif_context": {"positionKey", "sideToMove", "materialClass", "materialSignature", "motifTags", "motifCarriers"},
    "endgame_study_context": {"positionKey", "sideToMove", "materialClass", "materialSignature", "endgameStudy", "applicabilityRefs"},
    "mixed_feature_context": {
        "positionKey",
        "sideToMove",
        "materialClass",
        "materialSignature",
        "openingFamily",
        "openingAlias",
        "pawnStructure",
        "phaseContext",
        "motifTags",
        "motifCarriers",
        "endgameStudy",
        "applicabilityRefs",
        "sourceRefs",
    },
}
STRING_SIMILARITY_FIELDS = {"positionKey", "sideToMove", "materialClass", "materialSignature", "openingFamily", "openingVariation", "openingAlias", "aliasId"}
ARRAY_SIMILARITY_FIELDS = {"pawnStructure", "sourceRefs", "phaseContext", "motifTags", "motifCarriers", "endgameStudy", "applicabilityRefs"}
DEFAULT_ALLOWED_SOURCE_IDS = {"example-index"}
DEFAULT_MOTIF_BINDINGS = {
    "motif-detector-carrier:motif-loose-piece-example": ("loose_piece", "std:4k3/6b1/8/8/3N4/8/8/4K3 w - -"),
    "motif-detector-carrier:motif-fork-example": ("fork", "std:4k3/8/3r1r2/8/4N3/8/8/4K3 w - -"),
    "motif-detector-carrier:motif-pin-example": ("pin", "std:4r2k/8/8/8/8/8/4B3/4K3 w - -"),
}
DEFAULT_ENDGAME_BINDINGS = {
    "endgame-study-applicability:study-lucena-context": ("lucena_rook_pawn", "std:6k1/3PK3/8/8/8/8/4R3/4r3 w - -"),
    "endgame-study-applicability:study-philidor-context": ("philidor_rook_pawn", "std:8/3k4/8/3P4/3K4/4r3/8/4R3 b - -"),
    "endgame-study-applicability:study-vancura-context": ("vancura_rook_pawn", "std:7k/8/r5KP/8/8/8/8/R7 b - -"),
}


@dataclass(frozen=True)
class RetrievalQualityIndexResult:
    index_path: Path
    rejects_path: Path
    summary_path: Path
    summary: dict


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


def validate_output_root(root: Path) -> Path:
    resolved = root.resolve()
    allowed = ALLOWED_OUTPUT_ROOT.resolve()
    try:
        resolved.relative_to(allowed)
    except ValueError as error:
        raise ValueError(f"output root {root} is outside ignored retrieval local output root: {allowed}") from error
    return resolved


def position_key_from_fen(fen: str) -> str:
    parts = fen.strip().split()
    if len(parts) < 4:
        raise ValueError("FEN must contain at least board, side, castling, and en-passant fields")
    return "std:" + " ".join(parts[:4])


def side_to_move_from_fen(fen: str) -> str:
    parts = fen.strip().split()
    if len(parts) < 2 or parts[1] not in {"w", "b"}:
        raise ValueError("FEN must contain side to move")
    return "white" if parts[1] == "w" else "black"


def material_signature_from_fen(fen: str) -> str:
    board = fen.strip().split()[0]
    pieces = [ch for ch in board if ch.isalpha()]
    if len(pieces) >= 24:
        return "opening_full_material"
    non_kings = [ch.lower() for ch in pieces if ch.lower() != "k"]
    rook_count = non_kings.count("r")
    pawn_count = non_kings.count("p")
    bishop_count = non_kings.count("b")
    knight_count = non_kings.count("n")
    other_count = sum(1 for ch in non_kings if ch not in {"r", "p", "b", "n"})
    if rook_count == 2 and pawn_count == 1 and bishop_count == 0 and knight_count == 0 and other_count == 0:
        return "rook_pawn_vs_rook"
    if rook_count == 0 and pawn_count == 1 and bishop_count == 0 and knight_count == 0 and other_count == 0:
        return "king_pawn_vs_king"
    if knight_count or bishop_count:
        return "minor_piece"
    return "other_material"


def pawn_structure_evidence_from_fen(fen: str) -> set[str]:
    board = fen.strip().split()[0]
    ranks = board.split("/")
    pieces: dict[str, str] = {}
    for rank_index, rank in enumerate(ranks):
        file_index = 0
        board_rank = 8 - rank_index
        for ch in rank:
            if ch.isdigit():
                file_index += int(ch)
            else:
                square = f"{chr(ord('a') + file_index)}{board_rank}"
                pieces[square] = ch
                file_index += 1
    evidence = set()
    if pieces.get("d4") == "P" and pieces.get("c4") == "p":
        evidence.add("queenside_tension")
    return evidence


def opening_family_evidence_from_fen(fen: str) -> set[str]:
    evidence = set()
    if "queenside_tension" in pawn_structure_evidence_from_fen(fen):
        evidence.add("Catalan Opening")
    return evidence


def contains_forbidden_wording(value: object) -> bool:
    if isinstance(value, str):
        normalized = value.lower()
        return (
            any(word in normalized for word in FORBIDDEN_WORDS)
            or re.search(r"(^|[^a-z0-9])s[0-9]{2}([^a-z0-9]|$)", normalized) is not None
            or FORBIDDEN_CODED_RESULT_RE.search(normalized) is not None
        )
    if isinstance(value, list):
        return any(contains_forbidden_wording(item) for item in value)
    if isinstance(value, dict):
        return any(contains_forbidden_wording(key) or contains_forbidden_wording(item) for key, item in value.items())
    return False


def has_complete_citation(row: dict) -> bool:
    return bool(row.get("licenseNotice")) and bool(row.get("attributionText"))


def has_complete_provenance(row: dict) -> bool:
    return bool(row.get("licenseNotice")) and bool(row.get("attributionText"))


def reject_row(row: dict, reason: str) -> dict:
    fixture_reason = row.get("rejectReason")
    rejected = {
        "retrievalId": row.get("retrievalId"),
        "sourceRef": row.get("sourceRef"),
        "reason": fixture_reason or reason,
    }
    if fixture_reason:
        rejected["fixtureRejectReason"] = fixture_reason
        rejected["validatorReason"] = reason
    return rejected


def validate_similarity_key(
    row: dict,
    position_key: str,
    side_to_move: str,
    material_signature: str,
    motif_bindings: dict[str, tuple[str, str]],
    endgame_bindings: dict[str, tuple[str, str]],
) -> tuple[dict | None, str | None]:
    key = dict(row.get("similarityKey") or {})
    kind = row.get("similarityKind") or "mixed_feature_context"
    if not key:
        return None, "missing_similarity_key"
    allowed = SIMILARITY_FIELDS_BY_KIND.get(kind)
    if allowed is None:
        return None, "unsupported_similarity_kind"
    unknown = sorted(set(key) - allowed)
    if unknown:
        return None, "unsupported_similarity_key_field"
    for field, value in key.items():
        if field in STRING_SIMILARITY_FIELDS and not (isinstance(value, str) and value.strip()):
            return None, "invalid_similarity_key_value"
        if field in ARRAY_SIMILARITY_FIELDS and not (isinstance(value, list) and value and all(isinstance(item, str) and item.strip() for item in value)):
            return None, "invalid_similarity_key_value"
    if contains_forbidden_wording(key):
        return None, "truth_wording"

    if key.get("positionKey") and key["positionKey"] != position_key:
        return None, "similarity_position_key_mismatch"
    if key.get("sideToMove") and key["sideToMove"] != side_to_move:
        return None, "similarity_side_to_move_mismatch"
    if key.get("materialSignature") and key["materialSignature"] != material_signature:
        return None, "similarity_material_mismatch"
    if key.get("materialClass") and key["materialClass"] != material_signature:
        return None, "similarity_material_mismatch"

    def validate_opening() -> str | None:
        if any(field in key for field in ["openingFamily", "openingVariation", "openingAlias", "aliasId", "pawnStructure", "sourceRefs"]):
            family = key.get("openingFamily")
            family_evidence = opening_family_evidence_from_fen(row["fen"])
            if family and family not in family_evidence:
                return "opening_family_mismatch"
            structures = key.get("pawnStructure")
            if not isinstance(structures, list) or not structures:
                return "opening_structure_missing"
            evidence = pawn_structure_evidence_from_fen(row["fen"])
            if any(structure not in evidence for structure in structures):
                return "opening_structure_mismatch"
        return None

    def validate_motif() -> str | None:
        if any(field in key for field in ["motifTags", "motifCarriers"]):
            tags = key.get("motifTags")
            carriers = key.get("motifCarriers")
            if not isinstance(tags, list) or not tags or not isinstance(carriers, list) or not carriers:
                return "motif_carrier_missing"
            for ref in carriers:
                if not ref.startswith("motif-detector-carrier:"):
                    return "motif_carrier_missing"
                binding = motif_bindings.get(ref)
                if not binding:
                    return "motif_carrier_unknown"
                motif_id, carrier_position_key = binding
                if motif_id not in tags or carrier_position_key != position_key:
                    return "motif_carrier_mismatch"
        return None

    def validate_endgame() -> str | None:
        if any(field in key for field in ["endgameStudy", "applicabilityRefs"]):
            studies = key.get("endgameStudy")
            refs = key.get("applicabilityRefs")
            if not isinstance(studies, list) or not studies or not isinstance(refs, list) or not refs:
                return "endgame_applicability_missing"
            for ref in refs:
                if not ref.startswith("endgame-study-applicability:"):
                    return "endgame_applicability_missing"
                binding = endgame_bindings.get(ref)
                if not binding:
                    return "endgame_applicability_unknown"
                study_id, fixture_position_key = binding
                if study_id not in studies or fixture_position_key != position_key:
                    return "endgame_applicability_mismatch"
        return None

    if kind == "exact_position":
        declared_key = row.get("similarityKey", {}).get("positionKey")
        if not declared_key:
            return None, "exact_position_key_missing"
        if declared_key != position_key:
            return None, "exact_position_key_mismatch"
        opening_error = validate_opening()
        if opening_error:
            return None, opening_error
    elif kind == "opening_context":
        if not key.get("openingFamily"):
            return None, "opening_family_missing"
        opening_error = validate_opening()
        if opening_error:
            return None, opening_error
    elif kind == "motif_context":
        motif_error = validate_motif()
        if motif_error:
            return None, motif_error
    elif kind == "endgame_study_context":
        endgame_error = validate_endgame()
        if endgame_error:
            return None, endgame_error
    elif kind == "mixed_feature_context":
        families = {
            family
            for family, present in [
                ("material", "materialClass" in key or "materialSignature" in key),
                ("phase", "phaseContext" in key),
                ("opening", any(field in key for field in ["openingFamily", "openingVariation", "openingAlias", "aliasId", "pawnStructure", "sourceRefs"])),
                ("motif", any(field in key for field in ["motifTags", "motifCarriers"])),
                ("endgame", any(field in key for field in ["endgameStudy", "applicabilityRefs"])),
            ]
            if present
        }
        if len(families) < 2:
            return None, "mixed_feature_family_missing"
        for validator in [validate_opening, validate_motif, validate_endgame]:
            error = validator()
            if error:
                return None, error
    key.setdefault("positionKey", position_key)
    key.setdefault("sideToMove", side_to_move)
    key.setdefault("materialSignature", material_signature)
    return key, None


def generated_matched_features(row: dict, similarity_key: dict) -> list[str]:
    kind = row["similarityKind"]
    if kind == "exact_position":
        return ["position:exact"]
    if kind == "opening_context":
        features = []
        if similarity_key.get("openingFamily"):
            features.append("opening:" + similarity_key["openingFamily"].replace(" ", "_"))
        if similarity_key.get("openingAlias"):
            features.append("alias:" + similarity_key["openingAlias"].replace(" ", "_"))
        return features or ["opening:context"]
    if kind == "motif_context":
        features = [f"motif:{tag}" for tag in similarity_key.get("motifTags", [])]
        features.extend(f"carrier:{ref}" for ref in similarity_key.get("motifCarriers", []))
        return features or ["motif:context"]
    if kind == "endgame_study_context":
        features = [f"study:{tag}" for tag in similarity_key.get("endgameStudy", [])]
        return features or ["study:context"]
    features = [f"material:{similarity_key['materialSignature']}", f"side:{similarity_key['sideToMove']}"]
    if similarity_key.get("openingFamily"):
        features.append("opening:" + similarity_key["openingFamily"].replace(" ", "_"))
    return features


def canonical_similarity_value(value):
    if isinstance(value, dict):
        return {key: canonical_similarity_value(value[key]) for key in sorted(value)}
    if isinstance(value, list):
        return sorted(canonical_similarity_value(item) for item in value)
    return value


def semantic_duplicate_key(row: dict) -> str:
    similarity = row["similarityKey"]
    kind = row["similarityKind"]
    if kind == "exact_position":
        evidence = {"positionKey": row["positionKey"]}
    elif kind == "opening_context":
        evidence = {
            field: similarity[field]
            for field in ["openingFamily", "openingVariation", "openingAlias", "aliasId", "pawnStructure", "sourceRefs"]
            if field in similarity
        }
    elif kind == "motif_context":
        evidence = {
            field: similarity[field]
            for field in ["motifTags", "motifCarriers"]
            if field in similarity
        }
    elif kind == "endgame_study_context":
        evidence = {
            field: similarity[field]
            for field in ["endgameStudy", "applicabilityRefs"]
            if field in similarity
        }
    else:
        evidence = similarity
    key = {
        "positionKey": row["positionKey"],
        "sideToMove": row["sideToMove"],
        "similarityKind": kind,
        "similarityKey": canonical_similarity_value(evidence),
    }
    return json.dumps(key, sort_keys=True, separators=(",", ":"))


def quality_report(accepted: list[dict], rejects: list[dict]) -> dict:
    validation_rejected_by_reason = Counter(row.get("validatorReason") or row["reason"] for row in rejects)

    def reason_count(*reasons: str) -> int:
        return sum(validation_rejected_by_reason.get(reason, 0) for reason in reasons)

    high_quality = [
        row
        for row in accepted
        if row["similarityScore"] >= 0.80 and (row["similarityKind"] != "exact_position" or row["similarityScore"] >= EXACT_SIMILARITY_FLOOR)
    ]
    high_quality_by_kind = Counter(row["similarityKind"] for row in high_quality)
    accepted_by_source_quality = Counter(row["sourceQuality"] for row in accepted)
    suppressed = len(rejects)
    return {
        "qualityDecision": "context_quality_pass" if accepted and suppressed >= 0 else "context_quality_blocked",
        "highQualityExampleCount": len(high_quality),
        "highQualityByKind": dict(sorted(high_quality_by_kind.items())),
        "lowConfidenceSuppressedCount": suppressed,
        "displayCandidateCount": 0,
        "acceptedBySourceQuality": dict(sorted(accepted_by_source_quality.items())),
        "validationRejectedByReason": dict(sorted(validation_rejected_by_reason.items())),
        "nastyNegativeResults": {
            "sameOpeningWrongStructureRejected": reason_count(
                "same_opening_wrong_structure",
                "opening_wrong_structure_token_consistent",
                "exact_wrong_structure",
                "mixed_wrong_structure",
                "mixed_opening_without_structure",
                "opening_structure_mismatch",
                "opening_structure_missing",
                "opening_family_mismatch",
                "opening_side_or_material_mismatch",
            ),
            "motifWithoutExactCarrierRejected": reason_count(
                "motif_tag_without_carrier",
                "mixed_motif_without_carrier",
                "fake_motif_carrier",
                "borrowed_motif_carrier",
                "mixed_borrowed_motif_carrier",
                "motif_carrier_missing",
                "motif_carrier_unknown",
                "motif_carrier_mismatch",
            ),
            "oppositeSideToMoveRejected": reason_count(
                "side_to_move_mismatch",
                "similarity_side_to_move_mismatch",
                "opening_side_or_material_mismatch",
            ),
            "duplicateExamplesRejected": reason_count("duplicate_source_ref", "duplicate_similarity_key"),
            "missingAttributionOrLicenseRejected": reason_count("missing_attribution", "missing_attribution_or_license", "missing_provenance"),
            "truthLeakageRejected": reason_count(
                "truth_wording",
                "result_current_claim",
                "result_feature_wording",
                "similarity_key_truth_field",
                "sxx_similarity_key",
                "current_position_claim",
            ),
            "lowSimilarityRejected": reason_count("low_similarity"),
        },
        "consumptionPolicy": {
            "retrievalLayer": "optional_citation_example",
            "defaultProseMentionsSpecificGames": False,
            "requiresRendererCitationContractForGameDisplay": True,
            "retrievalOutranksMasterReference": False,
            "retrievalOutranksStatistic": False,
            "retrievalOutranksCertification": False,
            "retrievalCreatesCurrentPositionTruth": False,
        },
    }


def normalize_candidate(
    row: dict,
    *,
    allowed_source_ids: set[str] = DEFAULT_ALLOWED_SOURCE_IDS,
    motif_bindings: dict[str, tuple[str, str]] = DEFAULT_MOTIF_BINDINGS,
    endgame_bindings: dict[str, tuple[str, str]] = DEFAULT_ENDGAME_BINDINGS,
) -> tuple[dict | None, dict | None]:
    unknown_fields = sorted(set(row) - ALLOWED_TOP_LEVEL_FIELDS)
    if unknown_fields:
        return None, reject_row(row, "unsupported_field")
    if not row.get("sourceId"):
        return None, reject_row(row, "missing_source_id")
    if row["sourceId"] not in allowed_source_ids:
        return None, reject_row(row, "unknown_source_id")
    if not row.get("sourceRef"):
        return None, reject_row(row, "missing_source_ref")
    if contains_forbidden_wording(row.get("retrievalId", "")) or contains_forbidden_wording(row.get("sourceRef", "")):
        return None, reject_row(row, "truth_wording")
    if not re.fullmatch(r"retrieval-example:[A-Za-z][A-Za-z0-9_:-]*", str(row["sourceRef"])):
        return None, reject_row(row, "invalid_source_ref")
    if not row.get("fen"):
        return None, reject_row(row, "missing_fen")
    if not row.get("positionKey"):
        return None, reject_row(row, "missing_position_key")
    if not row.get("sideToMove"):
        return None, reject_row(row, "missing_side_to_move")
    if row.get("currentPositionClaim") is True:
        return None, reject_row(row, "current_position_claim")
    if row.get("authority") and row["authority"] != "retrieval_example":
        return None, reject_row(row, "unsupported_authority")
    if row.get("snippetRole") and row["snippetRole"] != "non_authoritative_context":
        return None, reject_row(row, "unsupported_snippet_role")
    if row.get("negativeBoundaries") is not None and "retrieval_non_authoritative" not in row.get("negativeBoundaries", []):
        return None, reject_row(row, "missing_non_authoritative_boundary")
    if row.get("exampleKind") and row["exampleKind"] not in ALLOWED_EXAMPLE_KINDS:
        return None, reject_row(row, "unsupported_example_kind")
    if row.get("similarityKind") not in ALLOWED_SIMILARITY_KINDS:
        return None, reject_row(row, "unsupported_similarity_kind")
    if row.get("sourceQuality") and row["sourceQuality"] not in ALLOWED_SOURCE_QUALITIES:
        return None, reject_row(row, "unsupported_source_quality")
    if contains_forbidden_wording(row.get("snippetText", "")) or contains_forbidden_wording(row.get("matchedFeatures", [])) or contains_forbidden_wording(row.get("tags", [])):
        return None, reject_row(row, "truth_wording")
    if row.get("gameMetadata"):
        unknown_citation = sorted(set(row["gameMetadata"]) - ALLOWED_CITATION_FIELDS)
        if unknown_citation:
            return None, reject_row(row, "unsupported_citation_field")
        for field, value in row["gameMetadata"].items():
            if field == "players":
                if not isinstance(value, list) or not value or not all(isinstance(player, str) and player.strip() for player in value):
                    return None, reject_row(row, "invalid_citation_metadata")
                if contains_forbidden_wording(value):
                    return None, reject_row(row, "truth_wording")
                continue
            if field == "result":
                if value not in {"1-0", "0-1", "1/2-1/2", "*"}:
                    return None, reject_row(row, "unsupported_result_metadata")
                continue
            if field == "date":
                if not isinstance(value, str) or re.fullmatch(r"[0-9]{4}(-[0-9]{2}(-[0-9]{2})?)?", value) is None:
                    return None, reject_row(row, "invalid_citation_metadata")
                continue
            if field == "url":
                if not isinstance(value, str) or not (value.startswith("https://") or value.startswith("local-curation:")):
                    return None, reject_row(row, "invalid_citation_metadata")
            elif contains_forbidden_wording(value):
                return None, reject_row(row, "truth_wording")
    score = float(row.get("similarityScore", 0.0))
    kind = row.get("similarityKind") or "mixed_feature_context"
    if score < SIMILARITY_FLOOR or (kind == "exact_position" and score < EXACT_SIMILARITY_FLOOR):
        return None, reject_row(row, "low_similarity")
    if row.get("gameMetadata") and not has_complete_citation(row):
        return None, reject_row(row, "missing_attribution_or_license")

    position_key = position_key_from_fen(row["fen"])
    side_to_move = side_to_move_from_fen(row["fen"])
    material_signature = material_signature_from_fen(row["fen"])
    if row["positionKey"] != position_key:
        return None, reject_row(row, "position_key_mismatch")
    if row["sideToMove"] != side_to_move:
        return None, reject_row(row, "side_to_move_mismatch")
    source_similarity_key = dict(row.get("similarityKey") or {})
    expected_side = source_similarity_key.get("sideToMove")
    expected_material = source_similarity_key.get("materialSignature")
    if source_similarity_key.get("openingFamily") and (
        (expected_side and expected_side != side_to_move) or (expected_material and expected_material != material_signature)
    ):
        return None, reject_row(row, "opening_side_or_material_mismatch")

    similarity_key, similarity_error = validate_similarity_key(row, position_key, side_to_move, material_signature, motif_bindings, endgame_bindings)
    if similarity_error:
        return None, reject_row(row, similarity_error)
    assert similarity_key is not None
    if not has_complete_provenance(row):
        return None, reject_row(row, "missing_provenance")
    if contains_forbidden_wording(row.get("licenseNotice", "")) or contains_forbidden_wording(row.get("attributionText", "")):
        return None, reject_row(row, "truth_wording")
    accepted = {
        "retrievalId": row.get("retrievalId") or "retrieval-quality-" + re.sub(r"[^A-Za-z0-9]+", "-", row["sourceRef"]).strip("-"),
        "sourceId": row.get("sourceId") or "example-index",
        "sourceRef": row["sourceRef"],
        "exampleKind": row.get("exampleKind") or "curated_reference",
        "fen": row["fen"],
        "positionKey": position_key,
        "sideToMove": side_to_move,
        "similarityKey": similarity_key,
        "similarityScore": score,
        "similarityKind": kind,
        "matchedFeatures": row.get("matchedFeatures") or generated_matched_features({"similarityKind": kind}, similarity_key),
        "sourceQuality": row.get("sourceQuality") or "local_curated",
        "snippetRole": "non_authoritative_context",
        "currentPositionClaim": False,
        "authority": "retrieval_example",
        "tags": sorted(set(row.get("tags") or ["example_only", kind])),
        "negativeBoundaries": sorted(set(row.get("negativeBoundaries") or ["retrieval_non_authoritative"])),
    }
    for optional in ["gameMetadata", "licenseNotice", "attributionText"]:
        if row.get(optional) is not None:
            accepted[optional] = row[optional]
    return accepted, None


def build_retrieval_quality_index(candidates: Iterable[dict], output_root: Path) -> RetrievalQualityIndexResult:
    root = validate_output_root(output_root)
    index_path = root / "retrieval-index.jsonl"
    rejects_path = root / "retrieval-rejects.jsonl"
    summary_path = root / "retrieval-summary.json"

    accepted: list[dict] = []
    rejects: list[dict] = []
    seen_refs: set[str] = set()
    seen_similarity: set[str] = set()
    citation_complete = Counter()
    total_examples = 0

    for candidate in candidates:
        total_examples += 1
        if candidate.get("gameMetadata"):
            citation_complete["complete" if has_complete_citation(candidate) else "missing"] += 1
        normalized, rejected = normalize_candidate(candidate)
        if rejected:
            rejects.append(rejected)
            continue
        assert normalized is not None
        source_ref = normalized["sourceRef"]
        if source_ref in seen_refs:
            rejects.append(reject_row(candidate, "duplicate_source_ref"))
            continue
        similarity_ref = semantic_duplicate_key(normalized)
        if similarity_ref in seen_similarity:
            rejects.append(reject_row(candidate, "duplicate_similarity_key"))
            continue
        seen_refs.add(source_ref)
        seen_similarity.add(similarity_ref)
        accepted.append(normalized)

    rejected_by_reason = Counter(row["reason"] for row in rejects)
    similarity_split = Counter()
    for row in accepted:
        if row["similarityKind"] == "exact_position":
            similarity_split["exact_position"] += 1
        else:
            similarity_split["feature_context"] += 1

    summary = {
        "totalExamples": total_examples,
        "acceptedExamples": len(accepted),
        "rejectedExamples": len(rejects),
        "rejectedByReason": dict(sorted(rejected_by_reason.items())),
        "duplicateSourceRefCount": rejected_by_reason.get("duplicate_source_ref", 0),
        "duplicateSimilarityKeyCount": rejected_by_reason.get("duplicate_similarity_key", 0),
        "lowSimilarityCount": rejected_by_reason.get("low_similarity", 0),
        "similarityKindSplit": dict(sorted(similarity_split.items())),
        "citationMetadataCompleteness": {
            "complete": citation_complete.get("complete", 0),
            "missing": citation_complete.get("missing", 0),
        },
        "qualityReport": quality_report(accepted, rejects),
        "localOnlyOutputRoot": str(root).replace("\\", "/"),
    }

    write_jsonl(index_path, accepted)
    write_jsonl(rejects_path, rejects)
    write_json(summary_path, summary)
    return RetrievalQualityIndexResult(index_path=index_path, rejects_path=rejects_path, summary_path=summary_path, summary=summary)


def retrieval_rows_from_corpus(corpus_dir: Path) -> list[dict]:
    rows: list[dict] = []
    retrieval_examples = corpus_dir / "retrieval-examples.jsonl"
    if retrieval_examples.exists():
        rows.extend(read_jsonl(retrieval_examples))
    motif_examples = corpus_dir / "motif-examples.jsonl"
    if motif_examples.exists():
        for row in read_jsonl(motif_examples)[:3]:
            carrier = row.get("detectorCarrier") or {}
            rows.append(
                {
                    "retrievalId": "quality-from-" + row["id"],
                    "sourceId": "example-index",
                    "sourceRef": "retrieval-example:quality-from-" + row["id"],
                    "exampleKind": "puzzle_reference",
                    "fen": row["fen"],
                    "positionKey": position_key_from_fen(row["fen"]),
                    "sideToMove": side_to_move_from_fen(row["fen"]),
                    "similarityKind": "motif_context",
                    "similarityScore": 0.82,
                    "similarityKey": {"motifTags": [row["motifId"]], "motifCarriers": [carrier.get("ref")] if carrier.get("ref") else []},
                    "sourceQuality": "manifest_backed",
                    "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
                    "attributionText": "Local curated retrieval quality fixture.",
                }
            )
    endgame_fixtures = corpus_dir / "endgame-study-fixtures.jsonl"
    if endgame_fixtures.exists():
        for row in read_jsonl(endgame_fixtures)[:3]:
            applicability_refs = [ref for ref in row.get("sourceRefs", []) if ref.startswith("endgame-study-applicability:")]
            rows.append(
                {
                    "retrievalId": "quality-from-" + row["id"],
                    "sourceId": "example-index",
                    "sourceRef": "retrieval-example:quality-from-" + row["id"],
                    "exampleKind": "study_reference",
                    "fen": row["fen"],
                    "positionKey": position_key_from_fen(row["fen"]),
                    "sideToMove": side_to_move_from_fen(row["fen"]),
                    "similarityKind": "endgame_study_context",
                    "similarityScore": 0.88,
                    "similarityKey": {"endgameStudy": [row["studyId"]], "applicabilityRefs": applicability_refs},
                    "sourceQuality": "public_fact",
                    "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
                    "attributionText": "Local curated retrieval quality fixture.",
                }
            )
    rows.extend(quality_fail_closed_candidates())
    retrieval_rejects = corpus_dir / "retrieval-reject-fixtures.jsonl"
    if retrieval_rejects.exists():
        rows.extend(read_jsonl(retrieval_rejects))
    return rows


def quality_fail_closed_candidates() -> list[dict]:
    base_fen = "4k3/8/8/8/8/8/4N3/4K3 w - - 0 1"
    catalan_fen = "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/6P1/PP2PPBP/RNBQK1NR w KQkq - 0 5"
    base_key = position_key_from_fen(base_fen)
    catalan_key = position_key_from_fen(catalan_fen)
    base_similarity = {"materialClass": "minor_piece", "phaseContext": ["endgame"]}
    return [
        {
            "retrievalId": "quality-probe-missing-source-ref",
            "sourceId": "example-index",
            "exampleKind": "curated_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.72,
            "similarityKey": base_similarity,
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
        {
            "retrievalId": "quality-probe-citation-missing-attribution",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:quality-probe-citation-missing-attribution",
            "exampleKind": "broadcast_game_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.72,
            "similarityKey": base_similarity,
            "gameMetadata": {"players": ["White Player", "Black Player"], "result": "1-0"},
        },
        {
            "retrievalId": "quality-probe-duplicate-source-ref",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:exact-catalan-reference",
            "exampleKind": "curated_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.72,
            "similarityKey": base_similarity,
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
        {
            "retrievalId": "quality-probe-low-similarity",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:quality-probe-low-similarity",
            "exampleKind": "curated_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.4,
            "similarityKey": base_similarity,
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
        {
            "retrievalId": "quality-probe-truth-snippet",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:quality-probe-truth-snippet",
            "exampleKind": "curated_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.72,
            "similarityKey": base_similarity,
            "snippetText": "This proves the best forced result.",
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
        {
            "retrievalId": "quality-probe-current-position-claim",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:quality-probe-current-position-claim",
            "exampleKind": "curated_reference",
            "fen": base_fen,
            "positionKey": base_key,
            "sideToMove": "white",
            "similarityKind": "mixed_feature_context",
            "similarityScore": 0.72,
            "similarityKey": base_similarity,
            "currentPositionClaim": True,
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
        {
            "retrievalId": "quality-probe-opening-wrong-side",
            "sourceId": "example-index",
            "sourceRef": "retrieval-example:quality-probe-opening-wrong-side",
            "exampleKind": "curated_reference",
            "fen": catalan_fen,
            "positionKey": catalan_key,
            "sideToMove": "white",
            "similarityKind": "opening_context",
            "similarityScore": 0.72,
            "similarityKey": {
                "openingFamily": "Catalan Opening",
                "sideToMove": "black",
                "materialSignature": "king_pawn_vs_king",
                "pawnStructure": ["queenside_tension"],
            },
            "licenseNotice": "Local fixture provenance only; citation display remains deferred.",
            "attributionText": "Local curated retrieval quality fixture.",
        },
    ]


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a tiny local-only retrieval quality index from committed fixtures.")
    parser.add_argument("--corpus-dir", type=Path, default=REPO_ROOT / "modules" / "commentary" / "src" / "test" / "resources" / "commentary-corpus")
    parser.add_argument("--output-root", type=Path, default=ALLOWED_OUTPUT_ROOT / "quality")
    args = parser.parse_args()
    result = build_retrieval_quality_index(retrieval_rows_from_corpus(args.corpus_dir), args.output_root)
    print(json.dumps(result.summary, ensure_ascii=False, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
