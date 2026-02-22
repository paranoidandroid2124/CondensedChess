#!/usr/bin/env python3
"""
Generate a structure goldset with diverse, legal chess positions.

Design goals:
- Keep v1 contract shape: 300 rows, 18 structures x 14 + Unknown x 48.
- Keep each row legal and structurally stable.
- Increase board diversity beyond move-counter-only duplication.
- Preserve existing expectedTopPlanIds/alternatives schema.

Usage:
  python modules/llm/src/test/resources/generate_goldset.py
  python modules/llm/src/test/resources/generate_goldset.py \
      --input modules/llm/src/test/resources/structure_goldset_v1.jsonl \
      --output modules/llm/src/test/resources/structure_goldset_v1_llm_curated.jsonl
"""

from __future__ import annotations

import argparse
import json
from collections import OrderedDict, defaultdict, deque
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

import chess


TARGET_COUNTS = OrderedDict(
    [
        ("Carlsbad", 14),
        ("IQPWhite", 14),
        ("IQPBlack", 14),
        ("HangingPawnsWhite", 14),
        ("HangingPawnsBlack", 14),
        ("FrenchAdvanceChain", 14),
        ("NajdorfScheveningenCenter", 14),
        ("BenoniCenter", 14),
        ("KIDLockedCenter", 14),
        ("SlavCaroTriangle", 14),
        ("MaroczyBind", 14),
        ("Hedgehog", 14),
        ("FianchettoShell", 14),
        ("Stonewall", 14),
        ("OpenCenter", 14),
        ("LockedCenter", 14),
        ("FluidCenter", 14),
        ("SymmetricCenter", 14),
        ("Unknown", 48),
    ]
)


DEFAULT_BASE_FENS = {
    "Carlsbad": "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 w - - 0 11",
    "IQPWhite": "r1bq1rk1/pp3ppp/2n2n2/3p4/3P4/2N2N2/PP2BPPP/R2Q1RK1 w - - 0 11",
    "IQPBlack": "r1bq1rk1/pp3ppp/2n2n2/3p4/3P4/2N2N2/PP2BPPP/R2Q1RK1 b - - 0 11",
    "HangingPawnsWhite": "r1bq1rk1/p4ppp/1pn2n2/2pp4/2PP4/2N2N2/PP2BPPP/R2Q1RK1 w - - 0 11",
    "HangingPawnsBlack": "r1bq1rk1/p4ppp/1pn2n2/2pp4/2PP4/2N2N2/PP2BPPP/R2Q1RK1 b - - 0 11",
    "FrenchAdvanceChain": "r1bqk2r/pp2bppp/2n1p3/3pP3/3P4/3B1N2/PP3PPP/R1BQK2R w KQkq - 0 11",
    "NajdorfScheveningenCenter": "r1bqk2r/1p2bppp/p1nppn2/8/3NP3/2N5/PPP2PPP/R1BQR1K1 w kq - 0 11",
    "BenoniCenter": "r1bq1rk1/pp2bppp/2np1n2/2pPp3/2P5/2N2NP1/PP2PPBP/R1BQ1RK1 w - - 0 11",
    "KIDLockedCenter": "r1bq1rk1/pp2npbp/2np2p1/2p1p3/2PPP3/2N1BN2/PP2BPPP/R2Q1RK1 w - - 0 11",
    "SlavCaroTriangle": "r1bqkb1r/pp3ppp/2n1pn2/2pp4/3P4/2N1PN2/PPP2PPP/R1BQKB1R w KQkq - 0 11",
    "MaroczyBind": "r1bq1rk1/pp2ppbp/2np1np1/8/2PNP3/2N1B3/PP2BPPP/R2Q1RK1 w - - 0 11",
    "Hedgehog": "r2q1rk1/pp1n1pbp/2pp1np1/8/2PP4/1PN1PN2/PB2BPPP/R2Q1RK1 w - - 0 11",
    "FianchettoShell": "r1bq1rk1/ppp2pbp/2np1np1/4p3/2P1P3/2N2NP1/PP1PPPBP/R1BQ1RK1 w - - 0 11",
    "Stonewall": "r2q1rk1/ppp2ppp/2np1n2/3bp3/3P1P2/2N1PN2/PPP3PP/R1BQ1RK1 w - - 0 11",
    "OpenCenter": "r1bq1rk1/pp3ppp/2n2n2/3p4/3P4/2N1PN2/PP3PPP/R1BQ1RK1 w - - 0 11",
    "LockedCenter": "r1bq1rk1/pp1n1ppp/2p1pn2/3p4/3P1P2/2N1PN2/PP4PP/R1BQ1RK1 w - - 0 11",
    "FluidCenter": "r1bq1rk1/pp2ppbp/2np1np1/2p5/3PP3/2N2N2/PP1BBPPP/R2Q1RK1 w - - 0 11",
    "SymmetricCenter": "r1bq1rk1/pp3ppp/2n2n2/3pp3/3PP3/2N2N2/PP3PPP/R1BQ1RK1 w - - 0 11",
    "Unknown": "r1bqk2r/pp2bppp/2n1pn2/3p4/3P4/2N2NP1/PP2PP1P/R1BQ1R1K w kq - 0 11",
}


@dataclass(frozen=True)
class SeedRow:
    fen: str
    alternatives: List[str]
    expected_top_plans: List[str]
    seed_pv: List[str]


def load_seed_rows(path: Path) -> Dict[str, SeedRow]:
    if not path.exists():
        return {}
    out: Dict[str, SeedRow] = {}
    with path.open("r", encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            row = json.loads(line)
            label = row["primary"]
            if label in out:
                continue
            out[label] = SeedRow(
                fen=row["fen"],
                alternatives=list(row.get("alternatives", [])),
                expected_top_plans=list(row.get("expectedTopPlanIds", [])),
                seed_pv=list(row.get("seedPv", [])),
            )
    return out


def pawn_signature(board: chess.Board) -> Tuple[Tuple[int, ...], Tuple[int, ...]]:
    white = tuple(sorted(board.pieces(chess.PAWN, chess.WHITE)))
    black = tuple(sorted(board.pieces(chess.PAWN, chess.BLACK)))
    return white, black


def board_key(board: chess.Board) -> str:
    # Keep board-relevant uniqueness (not move counters).
    parts = board.fen(en_passant="fen").split(" ")
    return " ".join(parts[:4])


def is_allowed_move(label: str, board: chess.Board, move: chess.Move) -> bool:
    piece = board.piece_at(move.from_square)
    if piece is None:
        return False
    if piece.piece_type in (chess.PAWN, chess.KING):
        return False
    target_piece = board.piece_at(move.to_square)
    # Avoid pawn-structure drift from pawn captures by pieces.
    if target_piece is not None and target_piece.piece_type == chess.PAWN:
        return False
    # Preserve Fianchetto shell anchor bishop.
    if label == "FianchettoShell" and move.from_square in (chess.G2, chess.G7):
        return False
    return True


def generate_variants(
    label: str,
    base_fen: str,
    count: int,
    max_depth: int = 6,
    max_branch: int = 12,
) -> List[Tuple[chess.Board, List[str]]]:
    start = chess.Board(base_fen)
    start_sig = pawn_signature(start)

    queue: deque[Tuple[chess.Board, List[str]]] = deque([(start, [])])
    seen = {board_key(start)}
    collected: List[Tuple[chess.Board, List[str]]] = []

    while queue and len(collected) < count:
        board, line = queue.popleft()
        if line:
            collected.append((board, line))
            if len(collected) >= count:
                break
        if len(line) >= max_depth:
            continue

        legal = [m for m in board.legal_moves if is_allowed_move(label, board, m)]
        legal = sorted(legal, key=lambda m: m.uci())[:max_branch]
        for mv in legal:
            nxt = board.copy(stack=False)
            nxt.push(mv)
            if pawn_signature(nxt) != start_sig:
                continue
            k = board_key(nxt)
            if k in seen:
                continue
            seen.add(k)
            queue.append((nxt, line + [mv.uci()]))

    # Fallback: duplicate start with only counters adjusted if search was insufficient.
    while len(collected) < count:
        b = start.copy(stack=False)
        b.halfmove_clock = len(collected) % 50
        b.fullmove_number = max(1, start.fullmove_number + len(collected))
        collected.append((b, []))
    return collected[:count]


def build_rows(
    seed_rows: Dict[str, SeedRow],
    annotators: Sequence[str],
    adjudicator: str,
) -> List[dict]:
    rows: List[dict] = []
    serial = defaultdict(int)

    for label, count in TARGET_COUNTS.items():
        seed = seed_rows.get(label)
        base_fen = DEFAULT_BASE_FENS.get(label, seed.fen if seed else None)
        if base_fen is None:
            raise ValueError(f"missing base FEN for label: {label}")
        alternatives = seed.alternatives if seed else []
        expected = seed.expected_top_plans if seed else []
        default_seed_pv = seed.seed_pv if seed else []

        variants = generate_variants(label, base_fen, count=count)
        for idx, (board, line) in enumerate(variants, start=1):
            serial[label] += 1
            fen = board.fen(en_passant="fen")
            seed_pv = line[:2] if line else default_seed_pv[:2]
            row = {
                "id": f"{label}-{idx}",
                "fen": fen,
                "primary": label,
                "alternatives": alternatives[:2],
                "expectedTopPlanIds": expected,
                "seedPv": seed_pv,
                "sourceGameId": f"llm_curated_{label.lower()}_{1000 + idx}",
                "sourcePly": board.fullmove_number * 2 - (0 if board.turn == chess.WHITE else 1),
                "annotators": list(annotators),
                "adjudicatedBy": adjudicator,
                "notes": "llm-curated-v3"
            }
            rows.append(row)

    return rows


def validate_rows(rows: Iterable[dict]) -> None:
    rows = list(rows)
    if len(rows) != 300:
        raise ValueError(f"expected 300 rows, got {len(rows)}")
    by_label = defaultdict(int)
    for r in rows:
        by_label[r["primary"]] += 1
    for label, expected in TARGET_COUNTS.items():
        got = by_label.get(label, 0)
        if got != expected:
            raise ValueError(f"label {label}: expected {expected}, got {got}")

    fen_keys = [r["fen"] for r in rows]
    if len(set(fen_keys)) != len(fen_keys):
        raise ValueError("FEN values must be unique")


def write_jsonl(path: Path, rows: Sequence[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        for row in rows:
            fh.write(json.dumps(row, ensure_ascii=True) + "\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate LLM-curated structure goldset.")
    parser.add_argument(
        "--input",
        default="modules/llm/src/test/resources/structure_goldset_v1.jsonl",
        help="Existing goldset input for seed metadata."
    )
    parser.add_argument(
        "--output",
        default="modules/llm/src/test/resources/structure_goldset_v1_llm_curated.jsonl",
        help="Output jsonl path."
    )
    parser.add_argument(
        "--annotators",
        default="codex-gpt5-primary,codex-gpt5-crosscheck",
        help="Comma-separated annotator labels."
    )
    parser.add_argument(
        "--adjudicator",
        default="codex-gpt5-adjudication",
        help="Adjudicator label."
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    input_path = Path(args.input)
    output_path = Path(args.output)

    seeds = load_seed_rows(input_path)
    annotators = [a.strip() for a in args.annotators.split(",") if a.strip()]
    if len(annotators) < 2:
        raise ValueError("at least two annotators are required")
    rows = build_rows(seeds, annotators=annotators[:2], adjudicator=args.adjudicator.strip())
    validate_rows(rows)
    write_jsonl(output_path, rows)
    print(f"generated {len(rows)} rows -> {output_path}")


if __name__ == "__main__":
    main()
