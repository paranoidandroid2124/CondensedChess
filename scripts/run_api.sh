#!/usr/bin/env bash
set -euo pipefail

# Load .env if present (PORT, BIND, GEMINI_API_KEY, GEMINI_MODEL, ANALYZE_* etc.)
if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

PORT="${PORT:-8080}"
BIND="${BIND:-0.0.0.0}"

echo "[run_api] BIND=$BIND PORT=$PORT GEMINI_MODEL=${GEMINI_MODEL:-gemini-1.5-flash}"
sbt "scalachess/runMain chess.analysis.ApiServer"
