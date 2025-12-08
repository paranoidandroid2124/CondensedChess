# CondensedChess Frontend (Next.js + Tailwind)

Private-from-scratch UI (no Maia/GPL code) to consume the Review JSON schema.

## Quick start
```bash
cd scalachess/frontend
npm install          # or pnpm/yarn, requires Node 18+
npm run dev
```

Open http://localhost:3000/review/sample to see the mock Review JSON rendered. The mock lives in `public/mock/sample-review.json`.

## API wiring
- Set `NEXT_PUBLIC_REVIEW_API_BASE` to your backend host (e.g. `http://localhost:8080`).
- The review page calls `GET {API_BASE}/result/:id` (returns `jobId` too). Use `/review/sample` for offline/demo.
- Variations: the board drag/drop hits `POST {API_BASE}/analysis/branch` with `{jobId, ply, uci}` and re-renders the returned tree.
- Opening DB: backend can load multiple SQLite DBs via `OPENING_STATS_DB_LIST` (masters/classical). The API returns `openingStats.source`, which is displayed as a badge in Summary.

## Structure
- `app/page.tsx`: Landing page (Hero + feature cards).
- `app/review/[id]/`: Client-side review page (board, timeline, critical list, summary).
- `types/review.ts`: Typed schema mirroring `REVIEW_SCHEMA.md`.
- `lib/review.ts`: Fetch helper with sample fallback.

## Styling
Tailwind with a glassy navy theme and accent colors (blue/teal/purple). Fonts pulled from Google (Space Grotesk + Inter) via `app/globals.css`.
