#!/usr/bin/env python3
"""
Build a compact opening stats file from PGN games (e.g., twic_raw with 2400+ Elo games).

Outputs a JSON object keyed by SAN sequence (space-separated), with per-position stats:
- bookPly/noveltyPly (ply index of last book move and first novelty in that game)
- games: count of games reaching the position
- winWhite/winBlack/draw: win rates (0-1)
- topMoves: next-move choices with game counts and win/draw rates
- topGames: sample of top-Elo games hitting the position

Usage:
  python scripts/build_opening_stats.py \
    --pgn-dir twic_raw \
    --out opening/masters_stats.json \
    --max-plies 20 \
    --min-elo 2400 \
    --top-games 8

Requires: python-chess (`pip install python-chess`).
"""
import argparse
import json
import os
from collections import defaultdict, Counter
from typing import Dict, List, Tuple, Optional

import chess.pgn


def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pgn-dir", required=True, help="Directory containing PGN files")
    ap.add_argument("--out", required=True, help="Output JSON file")
    ap.add_argument("--max-plies", type=int, default=20, help="Max plies to index per game")
    ap.add_argument("--min-elo", type=int, default=2400, help="Minimum Elo for both players")
    ap.add_argument("--top-games", type=int, default=8, help="Top game samples to keep per node")
    ap.add_argument("--sample", type=int, default=None, help="Optional limit of games to process")
    return ap.parse_args()


class NodeStats:
    __slots__ = ("games", "white", "black", "draw", "next_moves", "top_games")

    def __init__(self):
        self.games = 0
        self.white = 0
        self.black = 0
        self.draw = 0
        self.next_moves: Counter = Counter()
        self.top_games: List[Tuple[int, dict]] = []  # (elo_sum, payload)

    def add_game(self, result: str, next_move: Optional[str], game_info: dict, top_games_keep: int):
        self.games += 1
        if result == "1-0":
            self.white += 1
        elif result == "0-1":
            self.black += 1
        else:
            self.draw += 1
        if next_move:
            self.next_moves[next_move] += 1
        if game_info:
            elo_sum = (game_info.get("whiteElo") or 0) + (game_info.get("blackElo") or 0)
            self.top_games.append((elo_sum, game_info))
            self.top_games = sorted(self.top_games, key=lambda x: -x[0])[:top_games_keep]

    def to_json(self, san_seq: List[str], max_plies: int, top_games_keep: int):
        # book ply = len(san_seq) (current position), novelty is bookPly+1
        book_ply = min(len(san_seq), max_plies)
        total = max(1, self.games)
        win_w = self.white / total
        win_b = self.black / total
        draw = self.draw / total
        top_moves = []
        for san, cnt in self.next_moves.most_common(8):
            top_moves.append(
                {
                    "san": san,
                    "uci": "",  # optional; not deriving here
                    "games": cnt,
                    "winPct": win_w,  # approximate; per-move win% requires deeper split
                }
            )
        top_games = [g for _, g in self.top_games[:top_games_keep]]
        return {
            "bookPly": book_ply,
            "noveltyPly": book_ply + 1,
            "games": self.games,
            "winWhite": win_w,
            "winBlack": win_b,
            "draw": draw,
            "topMoves": top_moves,
            "topGames": top_games,
            "source": "twic_raw",
        }


def process_file(path: str, stats: Dict[str, NodeStats], args):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        game_count = 0
        while True:
            game = chess.pgn.read_game(f)
            if game is None:
                break
            game_count += 1
            if args.sample and game_count > args.sample:
                break
            white_elo = int(game.headers.get("WhiteElo") or 0)
            black_elo = int(game.headers.get("BlackElo") or 0)
            if white_elo < args.min_elo or black_elo < args.min_elo:
                continue
            result = game.headers.get("Result") or "*"
            event = game.headers.get("Event") or ""
            date = game.headers.get("Date") or ""
            white = game.headers.get("White") or ""
            black = game.headers.get("Black") or ""
            game_info = {
                "white": white,
                "black": black,
                "whiteElo": white_elo or None,
                "blackElo": black_elo or None,
                "result": result,
                "date": date,
                "event": event,
            }
            board = game.board()
            san_seq: List[str] = []
            node = game
            ply = 0
            while node.variations and ply < args.max_plies:
                move = node.variations[0].move
                san = board.san(move)
                san_seq.append(san)
                board.push(move)
                ply += 1
                key = " ".join(san_seq)
                stats.setdefault(key, NodeStats()).add_game(result, None, game_info, args.top_games)
                node = node.variations[0]


def main():
    args = parse_args()
    stats: Dict[str, NodeStats] = {}
    pgn_files = [
        os.path.join(args.pgn_dir, f)
        for f in os.listdir(args.pgn_dir)
        if f.lower().endswith(".pgn")
    ]
    for p in sorted(pgn_files):
        print(f"[build] processing {p}")
        process_file(p, stats, args)
    out = {k: v.to_json(k.split(), args.max_plies, args.top_games) for k, v in stats.items()}
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(out, f)
    print(f"[build] wrote {len(out)} positions to {args.out}")


if __name__ == "__main__":
    main()
