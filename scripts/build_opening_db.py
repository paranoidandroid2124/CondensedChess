#!/usr/bin/env python3
"""
Build a SQLite opening database from PGN games (e.g., twic_raw 2400+).

Schema:
- positions(id, san_seq UNIQUE, ply, games, win_w, win_b, draw)
- moves(position_id, san, uci, games, win_w, win_b, draw)  # per-next-move outcomes
- games(position_id, white, black, white_elo, black_elo, result, date, event)

Key behavior:
- win/draw per move 집계 포함 (moves.win_w/win_b/draw 비율 저장).
- 기본은 포지션별 상위 Elo 게임을 `--top-games-per-pos` 만큼만 저장; `--store-all-games`로 전체 저장 가능.
- 전체를 한 트랜잭션으로 삽입해 속도 개선, executemany 배치 사용.

Usage 예:
  python scripts/build_opening_db.py \
    --pgn-dir twic_raw \
    --out opening/masters_stats.db \
    --max-plies 200 \
    --min-elo 0 \
    --top-games-per-pos 20
"""

import argparse
import os
import sqlite3
import time
from collections import Counter, defaultdict
from typing import Dict, List, Tuple, Optional

import chess.pgn


def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pgn-dir", required=True, help="Directory containing PGN files")
    ap.add_argument("--out", required=True, help="Output SQLite path")
    ap.add_argument("--min-elo", type=int, default=0, help="Minimum Elo for both players (0 to disable)")
    ap.add_argument("--max-plies", type=int, default=200, help="Max plies per game to index")
    ap.add_argument("--top-games-per-pos", type=int, default=20, help="Max games to keep per position when not storing all")
    ap.add_argument("--store-all-games", action="store_true", help="Store all games per position (DB can become large)")
    ap.add_argument("--sample", type=int, default=None, help="Optional limit of games to process")
    return ap.parse_args()


class MoveStat:
    __slots__ = ("games", "w", "b", "d")

    def __init__(self):
        self.games = 0
        self.w = 0
        self.b = 0
        self.d = 0

    def add(self, result: str):
        self.games += 1
        if result == "1-0":
            self.w += 1
        elif result == "0-1":
            self.b += 1
        else:
            self.d += 1

    def ratios(self):
        g = max(1, self.games)
        return self.games, self.w / g, self.b / g, self.d / g


class NodeStats:
    __slots__ = ("games", "white", "black", "draw", "move_stats", "top_games", "year_counts", "san_seq")

    def __init__(self):
        self.games = 0
        self.white = 0
        self.black = 0
        self.draw = 0
        self.move_stats: Dict[str, MoveStat] = defaultdict(MoveStat)
        self.top_games: List[Tuple[int, dict]] = []  # (elo_sum, payload)
        self.year_counts: Dict[int, int] = defaultdict(int)
        self.san_seq: Optional[str] = None

    def add_game(self, result: str, next_move_san: Optional[str], game_info: dict, top_keep: int, store_all: bool, san_seq_str: str):
        self.games += 1
        if result == "1-0":
            self.white += 1
        elif result == "0-1":
            self.black += 1
        else:
            self.draw += 1
        if next_move_san:
            self.move_stats[next_move_san].add(result)
        year = game_info.get("year")
        if year:
            self.year_counts[year] += 1
        if not self.san_seq:
            self.san_seq = san_seq_str
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

    def parse_year(date_str: str):
        if not date_str:
            return None
        parts = date_str.split(".")
        if parts and len(parts[0]) == 4 and parts[0].isdigit():
            try:
                return int(parts[0])
            except Exception:
                return None
        return None

    return {
        "white": game.headers.get("White") or "",
        "black": game.headers.get("Black") or "",
        "whiteElo": to_int(game.headers.get("WhiteElo")),
        "blackElo": to_int(game.headers.get("BlackElo")),
        "result": game.headers.get("Result") or "*",
        "date": game.headers.get("Date") or "",
        "year": parse_year(game.headers.get("Date") or ""),
        "event": game.headers.get("Event") or "",
    }


def process_file(path: str, stats: Dict[str, NodeStats], args, log_prefix: str = "", progress_every: int = 5000, total_offset: int = 0):
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
                # Use shredder FEN (board + turn + castling + ep) to merge transpositions regardless of move clocks.
                key = board.shredder_fen()
                san_str = " ".join(san_seq)
                # 다음 수 추출
                next_move_label = None
                if node.variations[0].variations:
                  try:
                    next_move_label = board.san(node.variations[0].variations[0].move)
                  except Exception:
                    next_move_label = None
                stats.setdefault(key, NodeStats()).add_game(result, next_move_label, info, args.top_games_per_pos, args.store_all_games, san_str)
                node = node.variations[0]
            absolute = total_offset + game_count
            if progress_every and absolute > 0 and absolute % progress_every == 0:
                print(f"{log_prefix} progress: {absolute} games total, positions={len(stats)}")
    return game_count


def create_schema(conn: sqlite3.Connection):
    cur = conn.cursor()
    cur.executescript(
        """
        CREATE TABLE IF NOT EXISTS positions (
          id INTEGER PRIMARY KEY,
          fen TEXT UNIQUE,
          san_seq TEXT,
          ply INTEGER,
          games INTEGER,
          win_w REAL,
          win_b REAL,
          draw REAL,
          min_year INTEGER,
          max_year INTEGER,
          bucket_pre2012 INTEGER,
          bucket_2012_2017 INTEGER,
          bucket_2018_2019 INTEGER,
          bucket_2020_plus INTEGER
        );

        CREATE TABLE IF NOT EXISTS moves (
          position_id INTEGER,
          san TEXT,
          uci TEXT,
          games INTEGER,
          win_w REAL,
          win_b REAL,
          draw REAL
        );

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

        CREATE INDEX IF NOT EXISTS idx_positions_fen ON positions(fen);
        CREATE INDEX IF NOT EXISTS idx_positions_san ON positions(san_seq);
        CREATE INDEX IF NOT EXISTS idx_moves_pos ON moves(position_id);
        CREATE INDEX IF NOT EXISTS idx_games_pos ON games(position_id);
        """
    )
    conn.commit()


def write_db(path: str, stats: Dict[str, NodeStats], args):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        os.remove(path)
    conn = sqlite3.connect(path)
    create_schema(conn)
    cur = conn.cursor()
    cur.execute("BEGIN")

    def bucket_for_year(year: int) -> str:
        if year < 2012:
            return "pre2012"
        if year <= 2017:
            return "2012_2017"
        if year <= 2019:
            return "2018_2019"
        return "2020_plus"

    for fen, st in stats.items():
        san_seq = st.san_seq or ""
        ply = len(san_seq.split())
        games = st.games
        win_w = st.white / games if games else 0
        win_b = st.black / games if games else 0
        drw = st.draw / games if games else 0
        min_year = min(st.year_counts.keys()) if st.year_counts else None
        max_year = max(st.year_counts.keys()) if st.year_counts else None
        buckets = {"pre2012": 0, "2012_2017": 0, "2018_2019": 0, "2020_plus": 0}
        for y, cnt in st.year_counts.items():
            buckets[bucket_for_year(y)] += cnt
        cur.execute(
            "INSERT INTO positions(fen, san_seq, ply, games, win_w, win_b, draw, min_year, max_year, bucket_pre2012, bucket_2012_2017, bucket_2018_2019, bucket_2020_plus) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (
                fen,
                san_seq,
                ply,
                games,
                win_w,
                win_b,
                drw,
                min_year,
                max_year,
                buckets["pre2012"],
                buckets["2012_2017"],
                buckets["2018_2019"],
                buckets["2020_plus"],
            ),
        )
        pos_id = cur.lastrowid

        move_rows = []
        for mv, mstat in st.move_stats.items():
            g, w, b, d = mstat.ratios()
            move_rows.append((pos_id, mv, "", g, w, b, d))
        if move_rows:
            cur.executemany(
                "INSERT INTO moves(position_id, san, uci, games, win_w, win_b, draw) VALUES (?,?,?,?,?,?,?)",
                move_rows,
            )

        game_rows = []
        for _, g in st.top_games:
            game_rows.append(
                (
                    pos_id,
                    g.get("white"),
                    g.get("black"),
                    g.get("whiteElo"),
                    g.get("blackElo"),
                    g.get("result"),
                    g.get("date"),
                    g.get("event"),
                )
            )
        if game_rows:
            cur.executemany(
                "INSERT INTO games(position_id, white, black, white_elo, black_elo, result, date, event) VALUES (?,?,?,?,?,?,?,?)",
                game_rows,
            )

    conn.commit()
    conn.close()


def main():
    start_time = time.time()
    args = parse_args()
    stats: Dict[str, NodeStats] = {}
    pgn_files = [os.path.join(args.pgn_dir, f) for f in os.listdir(args.pgn_dir) if f.lower().endswith(".pgn")]
    total_games = 0
    for idx, p in enumerate(sorted(pgn_files)):
        prefix = f"[build {idx+1}/{len(pgn_files)}]"
        print(f"{prefix} processing {p}")
        total_games += process_file(p, stats, args, log_prefix=prefix, total_offset=total_games)
        print(f"{prefix} done: games so far {total_games}, positions so far {len(stats)}")
    print(f"[build] positions collected: {len(stats)}, games processed: {total_games}")
    write_db(args.out, stats, args)
    elapsed = time.time() - start_time
    print(f"[build] wrote DB to {args.out} (elapsed {elapsed/60:.1f} min)")


if __name__ == "__main__":
    main()
