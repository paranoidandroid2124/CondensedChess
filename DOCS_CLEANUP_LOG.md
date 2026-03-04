# DOCS_CLEANUP_LOG

## 2026-03-04 - Chesstory Analysis Legacy Removal (analysis screen only)

### Scope
- Targeted only `ui/analyse`.
- Preserved global routes/features (`/import`, `/editor`).
- Reduced analysis to:
  - core analysis flow (board/moves/FEN/PGN/local engine)
  - read-only explorer (`masters` / `online`)
  - minimal workbench controls

### Removed
- Workbench/action-menu legacy entries:
  - Practice with computer
  - Learn from your mistakes
  - Show fishnet analysis
  - Best move arrow toggle
  - Evaluation gauge toggle entry
  - Import latest from Lichess/Chess.com
  - Board editor entry
  - Continue from here
  - Board behavior / Appearance sections
- Practice/retro code paths and files:
  - `ui/analyse/src/practice/*`
  - `ui/analyse/src/retrospect/*`
- Explorer player/config paths:
  - removed `ExplorerDb.player`
  - removed `#explorer/<player>` handling
  - removed explorer gear/config panel
  - removed player-specific params (`playerName`, speed/rating/date/mode filters)

### Kept
- Flip board
- Inline notation
- Disclosure buttons
- Variation opacity
- Local engine analysis
- Read-only explorer DB switch (`Masters` / `Online`)

### Code/CSS cleanup highlights
- Simplified:
  - `ui/analyse/src/ctrl.ts`
  - `ui/analyse/src/view/actionMenu.ts`
  - `ui/analyse/src/view/controls.ts`
  - `ui/analyse/src/view/components.ts`
  - `ui/analyse/src/explorer/*`
- Removed dead CSS and imports:
  - `ui/analyse/css/_practice.scss`
  - `ui/analyse/css/_retro.scss`
  - `ui/analyse/css/explorer/_config.scss`
  - explorer `toconf` style blocks

### Verification
- UI package build:
  - `node ui/build.mjs --no-install analyse` ✅
- Scala compile (affected server-side modules):
  - `sbt "project analyse" "compile" "project api" "compile" "project web" "compile"` ✅
- Static checks:
  - no remaining analysis UI strings for removed menu items ✅
  - no `/api/explorer/player`, `opening/player`, `#explorer/` usage in `ui/analyse/src` ✅
