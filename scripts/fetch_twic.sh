#!/usr/bin/env bash
# Download TWIC PGN ZIPs, extract, and filter by Elo using pgn-extract.
# Usage: ./scripts/fetch_twic.sh <from> <to>
# Example: ./scripts/fetch_twic.sh 920 1380

set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <from> <to> (e.g., 920 1380)" >&2
  exit 1
fi

FROM=$1
TO=$2

WORKDIR="twic_raw"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

echo "[twic] downloading TWIC $FROM..$TO"
for n in $(seq "$FROM" "$TO"); do
  url="https://theweekinchess.com/zips/twic${n}g.zip"
  echo "→ $url"
  curl -fLO "$url" || echo "skip $n (not found)"
done

echo "[twic] extracting zips"
for z in twic*g.zip; do
  [ -f "$z" ] || continue
  unzip -o "$z"
done

echo "[twic] done. PGNs are in $WORKDIR. Run pgn-extract with filters in scripts/filters/."
echo "Example:"
echo "  pgn-extract -t ../scripts/filters/tag_elo2400_both.txt -o twic_elo2400_both.pgn twic*.pgn"
echo "  pgn-extract -t ../scripts/filters/tag_elo2400_any.txt -o twic_elo2400_any.pgn twic*.pgn"
