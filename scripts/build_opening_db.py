#!/usr/bin/env python3
"""
Build a SQLite opening database from PGN games (e.g., twic_raw 2400+).

Schema (tables):
- positions(id INTEGER PRIMARY KEY, san_seq TEXT UNIQUE, ply INT, games INT, win_w REAL, win_b REAL, draw REAL)
- moves(position_id INT, san TEXT, uci TEXT, games INT, win_w REAL, win_b REAL, draw REAL)
- games(position_id INT, white TEXT, black TEXT, white_elo INT, black_elo INT, result TEXT, date TEXT, event TEXT)

Usage example:
  python scripts/build_opening_db.py \
    --pgn-dir twic_raw \
    --out opening/masters_stats.db \
    --min-elo 0 \
    --top-games-per-pos 20 \
    --store-all-games false

Notes:
- By default 모든 게임에 대해 집계 통계만 저장하고, 각 포지션마다 Elo 상위 게임 20개만 보관합니다.
- `--store-all-games`를 true로 주면 각 포지션마다 해당 게임 전체를 저장하지만 DB 크기가 매우 커질 수 있습니다.
"""

import argparse
import os
import sqlite3
from collections import defaultdict, Counter
from typing import Dict, List, Tuple, Optional

import chess.pgn


def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pgn-dir", required=True, help="Directory containing PGN files")
    ap.add_argument("--out", required=True, help="Output SQLite path")
    ap.add_argument("--min-elo", type=int, default=0, help="Minimum Elo for both players (0 to disable)")
    ap.add_argument("--max-plies", type=int, default=200, help="Max plies per game to index (set high to include full games)")
    ap.add_argument("--top-games-per-pos", type=int, default=20, help="Max games to keep per position when not storing all")
    ap.add_argument("--store-all-games", action="store_true", help="Store all games per position (DB may be huge)")
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

    def add_game(self, result: str, next_move: Optional[str], game_info: dict, top_keep: int, store_all: bool):
        self.games += 1
        if result == "1-0":
            self.white += 1
        elif result == "0-1":
            self.black += 1
        else:
            self.draw += 1
        if next_move:
            self.next_moves[next_move] += 1
        if store_all:
            self.top_games.append((0, game_info))
        else:
            elo_sum = (game_info.get("whiteElo") or 0) + (game_info.get("blackElo") or 0)
            self.top_games.append((elo_sum, game_info))
            self.top_games = sorted(self.top_games, key=lambda x: -x[0])[:top_keep]


def should_keep_game(game, min_elo: int) -> bool:
    try:
        w_elo = int(game.headers.get("WhiteElo") or 0)
        b_elo = int(game.headers.get("BlackElo") or 0)
    except ValueError:
        return False
    if min_elo > 0 and (w_elo < min_elo or b_elo < min_elo):
        return False
    return True


def game_info_from_headers(game) -> dict:
    def to_int(val):
        try:
            return int(val)
        except Exception:
            return None

    return {
        "white": game.headers.get("White") or "",
        "black": game.headers.get("Black") or "",
        "whiteElo": to_int(game.headers.get("WhiteElo")),
        "blackElo": to_int(game.headers.get("BlackElo")),
        "result": game.headers.get("Result") or "*",
        "date": game.headers.get("Date") or "",
        "event": game.headers.get("Event") or "",
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
            if not should_keep_game(game, args.min_elo):
                continue
            info = game_info_from_headers(game)
            result = info["result"]
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
                next_move_san = node.variations[0].variations[0].move if node.variations[0].variations else None
                next_move_label = None
                if next_move_san:
                    try:
                        next_move_label = board.san(next_move_san)
                    except Exception:
                        next_move_label = None
                stats.setdefault(key, NodeStats()).add_game(result, next_move_label, info, args.top_games_per_pos, args.store_all_games)
                node = node.variations[0]


def create_schema(conn: sqlite3.Connection):
    cur = conn.cursor()
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS positions (
          id INTEGER PRIMARY KEY,
          san_seq TEXT UNIQUE,
          ply INTEGER,
          games INTEGER,
          win_w REAL,
          win_b REAL,
          draw REAL
        );
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS moves (
          position_id INTEGER,
          san TEXT,
          uci TEXT,
          games INTEGER,
          win_w REAL,
          win_b REAL,
          draw REAL
        );
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS games (
          position_id INTEGER,
          white TEXT,
          black TEXT,
          white_elo INTEGER,
          black_elo INTEGER,
          result TEXT,
          date TEXT,
          event TEXT
        );
        """
    )
    cur.execute("CREATE INDEX IF NOT EXISTS idx_positions_san ON positions(san_seq);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_moves_pos ON moves(position_id);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_games_pos ON games(position_id);")
    conn.commit()


def write_db(path: str, stats: Dict[str, NodeStats], args):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        os.remove(path)
    conn = sqlite3.connect(path)
    create_schema(conn)
    cur = conn.cursor()
    for san_seq, st in stats.items():
        ply = len(san_seq.split())
        cur.execute(
            "INSERT INTO positions(san_seq, ply, games, win_w, win_b, draw) VALUES (?,?,?,?,?,?)",
            (san_seq, ply, st.games, st.white / st.games if st.games else 0, st.black / st.games if st.games else 0, st.draw / st.games if st.games else 0),
        )
        pos_id = cur.lastrowid
        for mv, cnt in st.next_moves.most_common():
            cur.execute(
                "INSERT INTO moves(position_id, san, uci, games, win_w, win_b, draw) VALUES (?,?,?,?,?,?,?)",
                (pos_id, mv, "", cnt, None, None, None),
            )
        for _, g in st.top_games:
            cur.execute(
                "INSERT INTO games(position_id, white, black, white_elo, black_elo, result, date, event) VALUES (?,?,?,?,?,?,?,?)",
                (
                    pos_id,
                    g.get("white"),
                    g.get("black"),
                    g.get("whiteElo"),
                    g.get("blackElo"),
                    g.get("result"),
                    g.get("date"),
                    g.get("event"),
                    ),
            )
    conn.commit()
    conn.close()


def main():
    args = parse_args()
    stats: Dict[str, NodeStats] = {}
    pgn_files = [os.path.join(args.pgn_dir, f) for f in os.listdir(args.pgn_dir) if f.lower().endswith(".pgn")]
    for idx, p in enumerate(sorted(pgn_files)):
        print(f"[build] processing {idx+1}/{len(pgn_files)}: {p}")
        process_file(p, stats, args)
    print(f"[build] positions collected: {len(stats)}")
    write_db(args.out, stats, args)
    print(f"[build] wrote DB to {args.out}")


if __name__ == "__main__":
    main()
