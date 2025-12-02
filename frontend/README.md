# scalachess frontend (Next.js + Tailwind)

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
- The review page calls `GET {API_BASE}/result/:id`. Use `/review/sample` for offline/demo.

## Structure
- `app/page.tsx`: Landing page (Hero + feature cards).
- `app/review/[id]/`: Client-side review page (board, timeline, critical list, summary).
- `types/review.ts`: Typed schema mirroring `REVIEW_SCHEMA.md`.
- `lib/review.ts`: Fetch helper with sample fallback.

## Styling
Tailwind with a glassy navy theme and accent colors (blue/teal/purple). Fonts pulled from Google (Space Grotesk + Inter) via `app/globals.css`.
