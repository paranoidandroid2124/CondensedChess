# CondensedChess

Self-hosted PGN review stack built on scalachess + Stockfish + (optional) Gemini. It ingests a PGN, computes engine/feature timelines, marks critical moves, and emits a Review JSON that the Next.js frontend renders.

## Requirements
- JDK 21+, sbt
- Stockfish available on `PATH` or `STOCKFISH_BIN`
- Node 18+ for `frontend/`
- Optional: `GEMINI_API_KEY` (and `GEMINI_MODEL`, default `gemini-3-pro-preview`) to enable LLM summaries/comments

## Backend (Scala)
- Run API server (loads `.env` if present): `./scripts/run_api.sh`  
  - Endpoints: `POST /analyze` (body = PGN string or `{"pgn": "...", "llmPlys":[...]}`) → `{jobId,status}`; `GET /result/:id` → pending/ready/failed or Review JSON body (includes `jobId`); `POST /analysis/branch` with `{jobId, ply, uci}` merges a new variation into the server tree and returns updated Review JSON. CORS enabled.
  - Env: `PORT` (8080), `BIND` (0.0.0.0), `ANALYZE_*` depth/time knobs (see `AnalyzePgn.EngineConfig.fromEnv`), `ANALYZE_FORCE_CRITICAL_PLYS` to always request LLM at given plys, `OPENING_STATS_DB_LIST` (comma/semicolon) to load multiple opening DBs (e.g., masters + classical). `openingStats.source` shows which DB served the stats.
- CLI (one-off JSON): `sbt "core/runMain chess.analysis.AnalyzePgn path/to/game.pgn"` prints Review JSON to stdout.
- Schema: see `REVIEW_SCHEMA.md` for the Review JSON fields.

## Frontend (Next.js)
- `cd frontend && npm install && npm run dev`
- Set `NEXT_PUBLIC_REVIEW_API_BASE` (e.g., `http://localhost:8080`) to hit the Scala API. `/review/sample` uses the bundled mock JSON for offline demo.

## Notes
- Sample PGNs live at `sample*.pgn` for quick checks.
- `twic_raw/` is a local data dump and ignored via `.gitignore`.
- Upstream codebase started from lichess scalachess; many bench/tests were removed in favor of the review pipeline here.
