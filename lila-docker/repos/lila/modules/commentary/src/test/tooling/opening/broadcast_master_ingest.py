import argparse
import hashlib
import io
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, TextIO


LOCAL_PYTHON = Path("tmp/commentary-opening/cache/python").resolve()
if LOCAL_PYTHON.exists():
    sys.path.insert(0, str(LOCAL_PYTHON))

import chess
import chess.pgn


PARSER_VERSION = "opening-broadcast-master-ingest-2026-04-24"
LICENSE_NOTICE = "Derived opening statistics must preserve CC BY-SA 4.0 attribution and share-alike notice."
ATTRIBUTION_TEXT = "Source: Lichess Broadcast PGN dumps, licensed under CC BY-SA 4.0."
FORBIDDEN_ROW_FIELDS = {"bestMove", "theoryTruth", "forcedLine", "objectiveResult", "engineVerdict"}
MASTER_TITLES = {"GM", "WGM", "IM", "WIM", "FM", "WFM", "CM", "WCM", "NM", "WNM"}
RESULTS = {"1-0", "0-1", "1/2-1/2"}
REPO_ROOT = Path(__file__).resolve().parents[6]
ALLOWED_OUTPUT_ROOTS = (
    REPO_ROOT / "tmp" / "commentary-opening",
    REPO_ROOT / "modules" / "commentary" / ".local" / "opening",
    REPO_ROOT / "modules" / "commentary" / "src" / "test" / "resources" / "commentary-corpus" / "generated-opening",
)


@dataclass(frozen=True)
class IngestResult:
    manifest_path: Path
    move_stats_path: Path
    summary_path: Path
    rejects_path: Path
    dedupe_path: Path
    annotation_path: Path
    position_sample_distribution_path: Path
    family_coverage_path: Path
    low_sample_rows_path: Path
    summary: dict


def json_dump(path: Path, row: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(row, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def jsonl_dump(path: Path, rows: Iterable[dict]) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with path.open("w", encoding="utf-8", newline="\n") as out:
        for row in rows:
            forbidden = FORBIDDEN_ROW_FIELDS.intersection(row.keys())
            if forbidden:
                raise ValueError(f"move-stat row carries forbidden truth fields: {sorted(forbidden)}")
            out.write(json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n")
            count += 1
    return count


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def position_key(board: chess.Board) -> str:
    return "std:" + " ".join(board.fen(en_passant="legal").split(" ")[:4])


def parse_elo(value: str | None) -> int | None:
    if not value:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def time_control_seconds(value: str | None) -> int | None:
    if not value or value == "-":
        return None
    normalized = value.strip()
    if normalized.isdigit():
        return int(normalized)
    period = re.match(r"\d+/(\d+\+\d+)(?::\d+\+\d+)?", normalized)
    if period:
        base, increment = period.group(1).split("+", 1)
        return int(base) + 40 * int(increment)
    match = re.fullmatch(r"(\d+)\+(\d+)", normalized)
    if not match:
        lowered = normalized.lower()
        natural = re.search(
            r"(\d+)\s*minutes?\s*(?:\+|plus)?\s*(?:(\d+)\s*seconds?)?",
            lowered,
        )
        if not natural:
            return None
        base = int(natural.group(1)) * 60
        increment = int(natural.group(2) or 0)
        return base + 40 * increment
    base, increment = int(match.group(1)), int(match.group(2))
    return base + 40 * increment


def time_control_bucket(seconds: int) -> str:
    if seconds >= 3600:
        return "classical"
    if seconds >= 600:
        return "rapid"
    if seconds >= 180:
        return "blitz"
    return "bullet"


def has_player_level_metadata(headers: chess.pgn.Headers, min_elo: int) -> tuple[bool, dict]:
    white_elo = parse_elo(headers.get("WhiteElo"))
    black_elo = parse_elo(headers.get("BlackElo"))
    white_title = headers.get("WhiteTitle")
    black_title = headers.get("BlackTitle")
    titles = {title for title in [white_title, black_title] if title}
    has_title = any(title in MASTER_TITLES for title in titles)
    elos = [elo for elo in [white_elo, black_elo] if elo is not None]
    has_rating_level = bool(elos) and max(elos) >= min_elo
    return has_title or has_rating_level, {
        "hasTitle": has_title,
        "hasAnyRating": bool(elos),
        "hasBothRatings": white_elo is not None and black_elo is not None,
        "maxElo": max(elos) if elos else None,
    }


def result_increment(result: str) -> tuple[int, int, int]:
    if result == "1-0":
        return 1, 0, 0
    if result == "0-1":
        return 0, 0, 1
    return 0, 1, 0


def normalized_game_key(game: chess.pgn.Game) -> str:
    headers = game.headers
    game_url = headers.get("GameURL")
    if game_url:
        return "gameUrl:" + game_url.strip()
    moves = " ".join(move.uci() for move in game.mainline_moves())
    basis = "|".join(
        [
            headers.get("Event", ""),
            headers.get("Date", ""),
            headers.get("Round", ""),
            headers.get("White", ""),
            headers.get("Black", ""),
            headers.get("Result", ""),
            moves,
        ]
    )
    return "pgnHash:" + sha256_text(basis)


def annotation_counts(game: chess.pgn.Game) -> dict:
    has_comment = False
    has_engine_eval = False
    has_textual_advice = False
    has_nag = False
    for node in game.mainline():
        comment = node.comment or ""
        if comment.strip():
            has_comment = True
            lowered = comment.lower()
            if "[%eval" in lowered or "engine" in lowered:
                has_engine_eval = True
            if " best" in f" {lowered}" or "inaccuracy" in lowered or "mistake" in lowered or "blunder" in lowered:
                has_textual_advice = True
        if node.nags:
            has_nag = True
    return {
        "hasComment": has_comment,
        "hasEngineEval": has_engine_eval,
        "hasTextualAdvice": has_textual_advice,
        "hasNag": has_nag,
    }


def reject(rejects: list[dict], game_index: int, reason: str, game: chess.pgn.Game, extra: dict | None = None) -> None:
    row = {
        "gameIndex": game_index,
        "reason": reason,
        "event": game.headers.get("Event"),
        "date": game.headers.get("Date") or game.headers.get("UTCDate"),
        "gameUrl": game.headers.get("GameURL"),
    }
    if extra:
        row.update(extra)
    rejects.append(row)


def open_pgn(path: Path):
    if path.suffix == ".zst":
        import zstandard as zstd

        compressed = path.open("rb")
        dctx = zstd.ZstdDecompressor()
        stream = dctx.stream_reader(compressed)
        text = io.TextIOWrapper(stream, encoding="utf-8", errors="replace")
        return compressed, stream, text
    text = path.open("r", encoding="utf-8", errors="replace")
    return text,


def opening_family_name(headers: chess.pgn.Headers) -> str:
    opening = (headers.get("Opening") or "unknown").strip()
    if not opening:
        return "unknown"
    return opening.split(":", 1)[0].strip() or "unknown"


def sample_distribution(position_totals: Counter, threshold: int) -> dict:
    histogram = Counter()
    for sample_size in position_totals.values():
        if sample_size <= 1:
            histogram["sample_1"] += 1
        elif sample_size < threshold:
            histogram[f"sample_2_to_{threshold - 1}"] += 1
        elif sample_size < 10:
            histogram["sample_5_to_9"] += 1
        elif sample_size < 25:
            histogram["sample_10_to_24"] += 1
        else:
            histogram["sample_25_plus"] += 1
    return {
        "perPositionSampleSizeThreshold": threshold,
        "positionsBelowThreshold": sum(1 for value in position_totals.values() if value < threshold),
        "positionsMeetingThreshold": sum(1 for value in position_totals.values() if value >= threshold),
        "maxPositionSampleSize": max(position_totals.values(), default=0),
        "histogram": dict(sorted(histogram.items())),
    }


def source_scope_for(stop_reason: str) -> str:
    if stop_reason == "end_of_input":
        return "full_month"
    if stop_reason == "max_accepted_games":
        return "partial_month"
    return "streamed_sample"


def validate_output_root(root: Path) -> Path:
    resolved = root.resolve()
    for allowed_root in ALLOWED_OUTPUT_ROOTS:
        allowed = allowed_root.resolve()
        try:
            resolved.relative_to(allowed)
            return resolved
        except ValueError:
            continue
    allowed_labels = ", ".join(str(path.relative_to(REPO_ROOT)).replace("\\", "/") for path in ALLOWED_OUTPUT_ROOTS)
    raise ValueError(f"output root {root} is outside ignored opening local output roots: {allowed_labels}")


def run_ingest_from_pgn_text(
    pgn_text: str,
    *,
    root: Path,
    source_id: str,
    source_url: str,
    source_checksum: str,
    year_month: str,
    max_read_games: int,
    max_accepted_games: int,
    per_position_sample_size_threshold: int,
    min_elo: int = 2200,
    max_ply: int = 20,
) -> IngestResult:
    root = validate_output_root(root)
    raw_dir = root / "raw" / source_id
    raw_dir.mkdir(parents=True, exist_ok=True)
    raw_path = raw_dir / "sample.pgn"
    raw_path.write_text(pgn_text, encoding="utf-8", newline="\n")
    return run_ingest(
        input_path=raw_path,
        root=root,
        source_id=source_id,
        source_url=source_url,
        source_checksum=source_checksum,
        year_month=year_month,
        max_read_games=max_read_games,
        max_accepted_games=max_accepted_games,
        per_position_sample_size_threshold=per_position_sample_size_threshold,
        min_elo=min_elo,
        max_ply=max_ply,
    )


def run_ingest(
    *,
    input_path: Path,
    root: Path,
    source_id: str,
    source_url: str,
    source_checksum: str,
    year_month: str,
    max_read_games: int,
    max_accepted_games: int,
    per_position_sample_size_threshold: int,
    min_elo: int = 2200,
    max_ply: int = 20,
) -> IngestResult:
    root = validate_output_root(root)
    generated_dir = root / "generated" / source_id
    report_dir = root / "reports" / source_id
    manifest_path = generated_dir / "source-manifest.json"
    move_stats_path = generated_dir / "opening-move-stats.jsonl"
    summary_path = report_dir / "summary.json"
    rejects_path = report_dir / "rejects.jsonl"
    dedupe_path = report_dir / "dedupe.jsonl"
    annotation_path = report_dir / "annotation-report.json"
    position_sample_distribution_path = report_dir / "position-sample-distribution.json"
    family_coverage_path = report_dir / "opening-family-coverage.json"
    low_sample_rows_path = report_dir / "low-sample-rows.jsonl"

    rejects: list[dict] = []
    dedupe_rows: list[dict] = []
    seen_keys: set[str] = set()
    stat_counts: dict[tuple[str, str], Counter] = defaultdict(Counter)
    position_totals: Counter = Counter()
    read_games = 0
    accepted_games = 0
    deduped_games = 0
    coverage = Counter()
    time_control_coverage = Counter()
    opening_family_coverage = Counter()
    annotation_report = Counter()
    stop_reason = "end_of_input"

    handles = open_pgn(input_path)
    try:
        handle = handles[-1]
        while True:
            if max_read_games > 0 and read_games >= max_read_games:
                stop_reason = "max_read_games"
                break
            game = chess.pgn.read_game(handle)
            if game is None:
                stop_reason = "end_of_input"
                break
            read_games += 1
            game_index = read_games
            if game.errors:
                reject(rejects, game_index, "pgn_parse_error", game, {"errors": [str(error) for error in game.errors]})
                continue
            headers = game.headers
            result = headers.get("Result", "")
            if result not in RESULTS:
                reject(rejects, game_index, "unsupported_result", game, {"result": result})
                continue
            variant = headers.get("Variant", "Standard")
            if variant != "Standard":
                reject(rejects, game_index, "non_standard_variant", game, {"variant": variant})
                continue
            if headers.get("FEN") or headers.get("SetUp") == "1":
                reject(rejects, game_index, "setup_fen_not_supported", game)
                continue
            if not (headers.get("BroadcastName") or headers.get("BroadcastURL")):
                reject(rejects, game_index, "missing_broadcast_provenance", game)
                continue

            seconds = time_control_seconds(headers.get("TimeControl"))
            if seconds is None:
                reject(rejects, game_index, "missing_time_control", game, {"timeControl": headers.get("TimeControl")})
                continue
            bucket = time_control_bucket(seconds)
            if bucket not in {"classical", "rapid"}:
                reject(rejects, game_index, "fast_time_control", game, {"timeControl": headers.get("TimeControl"), "bucket": bucket})
                continue

            admitted_player, player_meta = has_player_level_metadata(headers, min_elo)
            if not admitted_player:
                reject(rejects, game_index, "missing_player_level_metadata", game, player_meta)
                continue

            game_key = normalized_game_key(game)
            if game_key in seen_keys:
                deduped_games += 1
                dedupe_rows.append({"gameIndex": game_index, "reason": "duplicate_game", "dedupeKey": game_key, "gameUrl": headers.get("GameURL")})
                continue
            seen_keys.add(game_key)

            ann = annotation_counts(game)
            if ann["hasComment"]:
                annotation_report["gamesWithComments"] += 1
            if ann["hasEngineEval"]:
                annotation_report["gamesWithEngineEvalComments"] += 1
            if ann["hasTextualAdvice"]:
                annotation_report["gamesWithTextualAdviceComments"] += 1
            if ann["hasNag"]:
                annotation_report["gamesWithNags"] += 1

            board = game.board()
            moves_seen = 0
            ww, dd, bw = result_increment(result)
            game_stat_counts: dict[tuple[str, str], Counter] = defaultdict(Counter)
            game_position_totals: Counter = Counter()
            try:
                for move in game.mainline_moves():
                    if moves_seen >= max_ply:
                        break
                    if move not in board.legal_moves:
                        raise ValueError(f"illegal mainline move {move.uci()}")
                    key = position_key(board)
                    move_uci = move.uci()
                    game_stat_counts[(key, move_uci)]["sampleSize"] += 1
                    game_stat_counts[(key, move_uci)]["whiteWins"] += ww
                    game_stat_counts[(key, move_uci)]["draws"] += dd
                    game_stat_counts[(key, move_uci)]["blackWins"] += bw
                    game_position_totals[key] += 1
                    board.push(move)
                    moves_seen += 1
            except Exception as error:
                reject(rejects, game_index, "move_replay_failed", game, {"message": str(error)})
                seen_keys.remove(game_key)
                continue
            if moves_seen == 0:
                reject(rejects, game_index, "empty_mainline", game)
                seen_keys.remove(game_key)
                continue

            for key, counts in game_stat_counts.items():
                stat_counts[key].update(counts)
            position_totals.update(game_position_totals)
            accepted_games += 1
            if player_meta["hasTitle"]:
                coverage["gamesWithTitle"] += 1
            if player_meta["hasAnyRating"]:
                coverage["gamesWithAnyRating"] += 1
            if player_meta["hasBothRatings"]:
                coverage["gamesWithBothRatings"] += 1
            time_control_coverage[bucket] += 1
            opening_family_coverage[opening_family_name(headers)] += 1
            if max_accepted_games > 0 and accepted_games >= max_accepted_games:
                stop_reason = "max_accepted_games"
                break
    finally:
        for handle in reversed(handles):
            handle.close()

    rows = []
    for index, ((key, move), counts) in enumerate(sorted(stat_counts.items()), start=1):
        position_sample_size = position_totals[key]
        sample_size = counts["sampleSize"]
        frequency = sample_size / position_sample_size if position_sample_size else 0.0
        row_id = "master-stat-" + sha256_text(f"{source_id}|{key}|{move}")[:16]
        rows.append(
            {
                "id": row_id,
                "sourceId": source_id,
                "sourceUse": "master_reference",
                "aggregateUse": "master_reference_stat",
                "positionKey": key,
                "move": move,
                "sampleSize": sample_size,
                "positionSampleSize": position_sample_size,
                "whiteWins": counts["whiteWins"],
                "draws": counts["draws"],
                "blackWins": counts["blackWins"],
                "frequency": round(frequency, 6),
                "candidateKind": "statistical_reference",
                "authority": "opening_statistic",
                "currentPositionTruth": False,
                "sampleConfidence": "meets_threshold"
                if position_sample_size >= per_position_sample_size_threshold
                else "below_threshold",
                "perPositionSampleSizeThreshold": per_position_sample_size_threshold,
                "negativeBoundaries": [
                    "master_reference_stat_is_not_best_move",
                    "master_reference_stat_is_not_theory_truth",
                    "master_reference_stat_is_not_engine_or_result_verdict",
                    "comments_and_engine_eval_annotations_are_ignored",
                ],
            }
        )

    move_stat_rows = jsonl_dump(move_stats_path, rows)
    low_sample_rows = [row for row in rows if row["sampleConfidence"] == "below_threshold"]
    jsonl_dump(rejects_path, rejects)
    jsonl_dump(dedupe_path, dedupe_rows)
    jsonl_dump(low_sample_rows_path, low_sample_rows)
    distribution = sample_distribution(position_totals, per_position_sample_size_threshold)
    family_coverage_rows = [
        {"openingFamily": family, "acceptedGames": count}
        for family, count in opening_family_coverage.most_common()
    ]
    json_dump(position_sample_distribution_path, distribution)
    json_dump(
        family_coverage_path,
        {
            "sourceId": source_id,
            "acceptedGames": accepted_games,
            "families": family_coverage_rows,
        },
    )
    source_scope = source_scope_for(stop_reason)
    processed_full_input = stop_reason == "end_of_input"

    raw_artifact = {
        "path": str(input_path).replace("\\", "/"),
        "storagePolicy": "localOnly",
        "checksum": sha256_file(input_path),
    }
    generated_artifact = {
        "path": str(move_stats_path).replace("\\", "/"),
        "storagePolicy": "localOnly",
        "checksum": sha256_file(move_stats_path),
        "rowCount": move_stat_rows,
    }
    manifest = {
        "sourceId": source_id,
        "sourceFamily": "opening",
        "sourceType": "masterGameDb",
        "sourceUse": "master_reference",
        "sourceName": "Lichess Broadcast master reference aggregate",
        "sourceVersion": year_month,
        "parserVersion": PARSER_VERSION,
        "license": "CC-BY-SA-4.0",
        "licenseName": "CC-BY-SA-4.0",
        "licenseUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
        "licenseVerifiedAt": "2026-04-24",
        "redistribution": "allowed",
        "derivedData": "allowed",
        "attributionRequired": True,
        "shareAlikeRequired": True,
        "attributionText": ATTRIBUTION_TEXT,
        "licenseNotice": LICENSE_NOTICE,
        "sourceUrl": source_url,
        "rawStoragePolicy": "localOnly",
        "generatedStoragePolicy": "localOnly",
        "sourceChecksum": source_checksum,
        "aggregateUse": "master_reference_stat",
        "sourceScope": source_scope,
        "playEnvironment": "otb",
        "playerLevel": "master",
        "ratingSystem": "fide",
        "minElo": min_elo,
        "titlePolicy": "title_or_min_elo",
        "timeScope": "single_month",
        "periodStart": year_month,
        "periodEnd": year_month,
        "timeControlScope": "classical_rapid",
        "perPositionSampleSizeThreshold": per_position_sample_size_threshold,
        "dedupePolicy": "stable_game_id_or_normalized_pgn_hash",
        "annotationPolicy": "strip_comments_report_engine_eval",
        "rawArtifacts": [raw_artifact],
        "generatedArtifacts": [generated_artifact],
        "notes": "Local-only manifest for recent Lichess Broadcast master-reference statistics; not live runtime input.",
    }
    json_dump(manifest_path, manifest)

    summary = {
        "sourceId": source_id,
        "sourceUse": "master_reference",
        "aggregateUse": "master_reference_stat",
        "sourceUrl": source_url,
        "yearMonth": year_month,
        "parserVersion": PARSER_VERSION,
        "sourceScope": source_scope,
        "processedFullInput": processed_full_input,
        "stopReason": stop_reason,
        "readGames": read_games,
        "acceptedGames": accepted_games,
        "dedupedGames": deduped_games,
        "rejectedGames": len(rejects),
        "rejectedByReason": dict(sorted(Counter(row["reason"] for row in rejects).items())),
        "positionCount": len(position_totals),
        "moveStatRows": move_stat_rows,
        "lowSampleRows": len(low_sample_rows),
        "positionSampleDistribution": distribution,
        "topOpeningFamilies": family_coverage_rows[:20],
        "playerCoverage": dict(sorted(coverage.items())),
        "timeControlCoverage": dict(sorted(time_control_coverage.items())),
        "annotationReport": {
            "gamesWithComments": annotation_report["gamesWithComments"],
            "gamesWithEngineEvalComments": annotation_report["gamesWithEngineEvalComments"],
            "gamesWithTextualAdviceComments": annotation_report["gamesWithTextualAdviceComments"],
            "gamesWithNags": annotation_report["gamesWithNags"],
            "policy": "comments, NAGs, and engine eval annotations are ignored; only legal mainline moves are counted",
        },
        "rawPath": str(input_path).replace("\\", "/"),
        "generatedMoveStatsPath": str(move_stats_path).replace("\\", "/"),
        "manifestPath": str(manifest_path).replace("\\", "/"),
        "positionSampleDistributionPath": str(position_sample_distribution_path).replace("\\", "/"),
        "familyCoveragePath": str(family_coverage_path).replace("\\", "/"),
        "lowSampleRowsPath": str(low_sample_rows_path).replace("\\", "/"),
    }
    json_dump(summary_path, summary)
    json_dump(annotation_path, summary["annotationReport"])

    return IngestResult(
        manifest_path=manifest_path,
        move_stats_path=move_stats_path,
        summary_path=summary_path,
        rejects_path=rejects_path,
        dedupe_path=dedupe_path,
        annotation_path=annotation_path,
        position_sample_distribution_path=position_sample_distribution_path,
        family_coverage_path=family_coverage_path,
        low_sample_rows_path=low_sample_rows_path,
        summary=summary,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a local-only Lichess Broadcast master-reference opening aggregate.")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--root", default=Path("tmp/commentary-opening"), type=Path)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--source-checksum", required=True)
    parser.add_argument("--year-month", required=True)
    parser.add_argument("--max-read-games", type=int, default=0, help="0 means no read cap")
    parser.add_argument("--max-accepted-games", type=int, default=0, help="0 means no accepted-game cap")
    parser.add_argument("--per-position-sample-size-threshold", type=int, default=5)
    parser.add_argument("--min-elo", type=int, default=2200)
    parser.add_argument("--max-ply", type=int, default=20)
    args = parser.parse_args()

    result = run_ingest(
        input_path=args.input,
        root=args.root,
        source_id=args.source_id,
        source_url=args.source_url,
        source_checksum=args.source_checksum,
        year_month=args.year_month,
        max_read_games=args.max_read_games,
        max_accepted_games=args.max_accepted_games,
        per_position_sample_size_threshold=args.per_position_sample_size_threshold,
        min_elo=args.min_elo,
        max_ply=args.max_ply,
    )
    print(json.dumps(result.summary, ensure_ascii=False, sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
