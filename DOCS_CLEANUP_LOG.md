# Docs Cleanup Log

Date: 2026-02-21

## Purpose
- Remove legacy Lichess-era narrative/links from non-legal documentation.
- Remove clearly unrelated root text artifacts.
- Keep legal attribution files and technical identifiers intact.

## Policy Applied
- Non-legal docs: removed Lichess-specific branding text and links.
- Legal docs: preserved as-is (`lila-docker/repos/lila/COPYING.md`, `lila-docker/repos/lila/public/flags/LICENSE.txt`).
- Technical identifiers: preserved where operationally relevant (e.g., Mongo DB name `lichess`, package/module identifiers).
- Data-integrity exception: PGN source tags were not rewritten.

## Deleted Files
- `tmp_doknjas_ai_revolution.txt`
  - Reason: unrelated, non-product text artifact at repo root.

## Updated Files
- `lila-docker/README.md`
  - Removed legacy fork narrative and external legacy links.
- `lila-docker/repos/lila/README.md`
  - Removed Lichess-focused intro/credits; retained license reference to `COPYING.md`.
- `lila-docker/repos/lila/ui/README.md`
  - Replaced `lila/lichess server` phrasing and removed legacy external wiki link.
- `lila-docker/repos/lila-ws/README.md`
  - Reworded service description for local Chesstory stack.
- `lila-docker/repos/scalachess/README.md`
  - Reframed as vendored local library usage; removed upstream clone/badge focus.
- `lila-docker/repos/lila-db-seed/README.md`
  - Removed legacy external org links; switched to local path guidance.
- `lila-docker/repos/lila/modules/llm/ARCHITECTURE.md`
  - Replaced Lichess-branded terms with neutral architecture terminology.
- `lila-docker/repos/lila/modules/llm/docs/CpContract.md`
  - Replaced `Lichess backend` with `Server Stockfish backend`.
- `lila-docker/repos/lila/translation/dest/README.md`
  - Removed legacy Crowdin project link; kept generated-file warning.
- `lila-docker/repos/lila-db-seed/spamdb/data/categs.txt`
  - `Lichess Feedback` -> `Chesstory Feedback`.
- `lila-docker/repos/lila-db-seed/spamdb/data/countries.txt`
  - `_lichess` -> `_chesstory`.
- `lila-docker/repos/lila-db-seed/spamdb/data/social_media_links.txt`
  - `https://lichess.org` -> `http://localhost:8080`.
- `lila-docker/repos/lila-db-seed/spamdb/data/teams.txt`
  - `We Shall Fight on the Lichess` -> `We Shall Fight on Chesstory`.

## Notes
- This pass intentionally avoided code/config/script mutations.
- Remaining Lichess strings in repo outside legal-doc exceptions are mostly code identifiers, tests, fixtures, or runtime config values and were out of scope for this docs-only cleanup.
