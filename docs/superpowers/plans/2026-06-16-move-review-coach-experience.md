# MoveReview Coach Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first player-facing MoveReview coach experience on the existing payload contract.

**Architecture:** Move player-facing HTML assembly into a pure frontend surface module, then wire `moveReview.ts` to that module. Keep backend truth behavior unchanged and preserve trusted ref resolution for all interactive SAN chips.

**Tech Stack:** TypeScript, Node test runner with `tsx`, existing lila UI SCSS, existing MoveReview payload types.

---

## File Structure

- Create: `lila-docker/repos/lila/ui/analyse/src/moveReview/coachSurface.ts`
  - Owns player-facing MoveReview HTML composition.
  - Reuses `responsePayload` types and `surfaceShared` helpers.
  - Exports `decorateMoveReviewHtml`.
- Create: `lila-docker/repos/lila/ui/analyse/tests/moveReviewCoachSurface.test.ts`
  - Verifies player-facing labels, trusted chip behavior, fallback behavior, and no developer-facing labels.
- Modify: `lila-docker/repos/lila/ui/analyse/src/moveReview.ts`
  - Imports `decorateMoveReviewHtml`.
  - Removes the moved pure rendering helpers from the large orchestration file.
- Modify: `lila-docker/repos/lila/ui/analyse/css/_side.scss`
  - Restyles the MoveReview surface as a coach walkthrough: header, verdict, reasons, body, line prompt, deeper review drawer.

## Task 1: Pure Coach Surface Module

**Files:**
- Create: `lila-docker/repos/lila/ui/analyse/src/moveReview/coachSurface.ts`
- Create: `lila-docker/repos/lila/ui/analyse/tests/moveReviewCoachSurface.test.ts`

- [ ] **Step 1: Write the failing coach surface tests**

Create `lila-docker/repos/lila/ui/analyse/tests/moveReviewCoachSurface.test.ts`:

```ts
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { decorateMoveReviewHtml } from '../src/moveReview/coachSurface';
import type { MoveReviewPlayerSurfaceV1, MoveReviewRefsV1 } from '../src/moveReview/responsePayload';

const refs: MoveReviewRefsV1 = {
  schema: 'chesstory.refs.v1',
  startFen: '8/8/8/8/8/8/8/8 w - - 0 1',
  startPly: 1,
  variations: [
    {
      lineId: 'main',
      scoreCp: 42,
      mate: null,
      depth: 18,
      moves: [
        {
          refId: 'main-1',
          san: 'Nf3',
          uci: 'g1f3',
          fenAfter: '8/8/8/8/8/5N2/8/8 b - - 1 1',
          ply: 1,
          moveNo: 1,
          marker: '1.',
        },
        {
          refId: 'main-2',
          san: 'd5',
          uci: 'd7d5',
          fenAfter: '8/8/8/3p4/8/5N2/8/8 w - - 0 2',
          ply: 2,
          moveNo: 1,
          marker: '1...',
        },
      ],
    },
  ],
};

const playerSurface = (overrides: Partial<MoveReviewPlayerSurfaceV1> = {}): MoveReviewPlayerSurfaceV1 => ({
  schema: 'chesstory.move_review.player_surface.v2',
  title: 'Move review: Nf3',
  summaryRows: [
    {
      label: 'Plan clue',
      text: 'The checked line keeps pressure on d5.',
      tone: 'practical',
      refSans: ['Nf3', 'd5'],
      authority: {
        kind: 'practical_plan',
        target: 'd5',
      },
    },
  ],
  advancedRows: [],
  probeRows: [],
  authorRows: [],
  decisionComparison: {
    kicker: 'Decision point',
    gapLabel: '+0.4',
    chosenSan: 'Nf3',
    engineSan: 'Nf3',
    chosenMatchesBest: true,
    secondaryText: 'The move keeps the main plan alive.',
    refSans: ['Nf3', 'd5'],
  },
  ...overrides,
});

describe('moveReview coach surface', () => {
  test('renders player-facing coach structure without developer labels', () => {
    const html = decorateMoveReviewHtml('<div class="commentary"><p>Main note.</p></div>', refs, playerSurface());

    assert.match(html, /move-review-coach/);
    assert.match(html, /Key moment/);
    assert.match(html, /Coach verdict/);
    assert.match(html, /Why it mattered/);
    assert.match(html, /Try the line/);
    assert.doesNotMatch(html, /authority/i);
    assert.doesNotMatch(html, /diagnostics/i);
    assert.doesNotMatch(html, /Evidence Probes/);
  });

  test('keeps trusted SAN chips interactive and ambiguous SAN display-only', () => {
    const html = decorateMoveReviewHtml(
      '<div class="commentary"><p>Main note.</p></div>',
      refs,
      playerSurface({
        summaryRows: [
          {
            label: 'Line clue',
            text: 'The line starts with a trusted move.',
            refSans: ['Nf3'],
          },
          {
            label: 'Ambiguous clue',
            text: 'This SAN is not in refs.',
            refSans: ['Qh5'],
          },
        ],
      }),
    );

    assert.match(html, /data-ref-id="main-1"/);
    assert.match(html, /data-uci="g1f3"/);
    assert.match(html, /<code class="move-review-coach__move-chip">Qh5<\/code>/);
  });

  test('falls back to raw commentary when no player surface exists', () => {
    const html = decorateMoveReviewHtml('<p>Only commentary.</p>', refs, null);

    assert.equal(html, '<p>Only commentary.</p>');
  });
});
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd lila-docker/repos/lila
pnpm exec tsx --test ui/analyse/tests/moveReviewCoachSurface.test.ts
```

Expected:
- FAIL because `../src/moveReview/coachSurface` does not exist.

- [ ] **Step 3: Create the coach surface module**

Create `lila-docker/repos/lila/ui/analyse/src/moveReview/coachSurface.ts`.

Move the existing pure helper block from `moveReview.ts` into this module:
- `MoveReviewMoveRef`
- `MoveReviewRefIndex`
- `buildMoveReviewRefIndex`
- `sameMoveRef`
- `sameMoveRefSequence`
- `equivalentSingleSanRef`
- `equivalentMoveRef`
- `resolveSanSequenceRefs`
- `resolveTrustedDecisionSanRef`
- `renderOpeningBookMetadata`
- `formatOpeningGameCount`
- `surfaceStatusLabel`

Then implement these player-facing renderers in the new module:

```ts
import { formatDecisionTargetComparison } from '../decisionComparison';
import type {
  MoveReviewPlayerAuthorRowV1,
  MoveReviewPlayerDecisionComparisonV1,
  MoveReviewPlayerSurfaceRowV1,
  MoveReviewPlayerSurfaceV1,
  MoveReviewRefsV1,
} from './responsePayload';
import { escapeHtml, normalizeSanToken, renderInteractiveSanChip } from './surfaceShared';

function renderCoachMoveChip(
  label: string,
  move: string | null | undefined,
  ref: MoveReviewMoveRef | null,
  tone: 'chosen' | 'engine' | 'deferred',
): string | null {
  const normalized = normalizeSanToken(move);
  const raw = move?.trim() || normalized;
  if (!normalized || !raw) return null;
  const chip = renderInteractiveSanChip(raw, ref || null, {
    interactiveClasses: 'move-review-coach__move-chip move-chip move-chip--interactive',
    fallbackTag: 'span',
    fallbackClasses: 'move-review-coach__move-chip',
  });

  return `
    <span class="move-review-coach__decision-move move-review-coach__decision-move--${tone}">
      <span class="move-review-coach__decision-label">${escapeHtml(label)}</span>
      ${chip}
    </span>
  `;
}
```

Implement `renderCoachVerdict`, `renderCoachSurfaceRow`, `renderAuthorRow`, and exported `decorateMoveReviewHtml` using the existing helper logic but with these player-facing containers:

```ts
export function decorateMoveReviewHtml(
  html: string,
  refs: MoveReviewRefsV1 | null,
  playerSurface: MoveReviewPlayerSurfaceV1 | null,
): string {
  if (!playerSurface) return html;

  const refIndex = buildMoveReviewRefIndex(refs);
  const titleText = playerSurface.title?.trim() || 'Move review';
  const decision = renderCoachVerdict(playerSurface.decisionComparison, refIndex);
  const summaryRows = playerSurface.summaryRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const advancedRows = playerSurface.advancedRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const probeRows = playerSurface.probeRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const authorRows = playerSurface.authorRows.map(row => renderAuthorRow(row, refIndex)).join('');
  const hasDeepReview = !!(advancedRows || probeRows || authorRows);

  if (!decision && !summaryRows && !hasDeepReview) {
    return `<div class="move-review-coach"><header class="move-review-coach__header"><span class="move-review-coach__eyebrow">Key moment</span><h3>${escapeHtml(titleText)}</h3></header><div class="move-review-coach__body">${html}</div></div>`;
  }

  return `
    <div class="move-review-coach">
      <header class="move-review-coach__header">
        <span class="move-review-coach__eyebrow">Key moment</span>
        <h3>${escapeHtml(titleText)}</h3>
      </header>
      ${decision || ''}
      ${summaryRows ? `<section class="move-review-coach__section"><h4>Why it mattered</h4><div class="move-review-coach__reasons">${summaryRows}</div></section>` : ''}
      <section class="move-review-coach__section move-review-coach__section--body">
        <h4>Coach notes</h4>
        <div class="move-review-coach__body">${html}</div>
      </section>
      ${summaryRows || decision ? `<section class="move-review-coach__practice"><strong>Remember this</strong><span>Replay the line, then try to name the idea before checking the note.</span></section>` : ''}
      ${
        hasDeepReview
          ? `<details class="move-review-coach__details"><summary>More to check</summary>${advancedRows}${probeRows ? `<div class="move-review-coach__subsection"><h5>Extra lines</h5>${probeRows}</div>` : ''}${authorRows ? `<div class="move-review-coach__subsection"><h5>Review questions</h5>${authorRows}</div>` : ''}</details>`
          : ''
      }
    </div>
  `;
}
```

- [ ] **Step 4: Run the coach surface test**

Run:

```bash
cd lila-docker/repos/lila
pnpm exec tsx --test ui/analyse/tests/moveReviewCoachSurface.test.ts
```

Expected:
- PASS for all tests in `moveReviewCoachSurface.test.ts`.

- [ ] **Step 5: Commit task 1**

```bash
git add lila-docker/repos/lila/ui/analyse/src/moveReview/coachSurface.ts lila-docker/repos/lila/ui/analyse/tests/moveReviewCoachSurface.test.ts
git commit -m "Add MoveReview coach surface renderer"
```

## Task 2: Wire Existing MoveReview Orchestration To Coach Surface

**Files:**
- Modify: `lila-docker/repos/lila/ui/analyse/src/moveReview.ts`

- [ ] **Step 1: Replace local decoration import**

In `moveReview.ts`, add:

```ts
import { decorateMoveReviewHtml } from './moveReview/coachSurface';
```

Remove the moved pure helper block from `MoveReviewMoveRef` through local `decorateMoveReviewHtml`. Keep `decorateDecodedMoveReviewHtml` and change it to call the imported function:

```ts
function decorateDecodedMoveReviewHtml(decoded: DecodedMoveReviewResponse): string {
    return decorateMoveReviewHtml(decoded.html, decoded.refs, decoded.moveReviewPlayerSurface);
}
```

- [ ] **Step 2: Run TypeScript tests that cover payload and persistence**

Run:

```bash
cd lila-docker/repos/lila
pnpm exec tsx --test ui/analyse/tests/moveReviewCoachSurface.test.ts ui/analyse/tests/moveReviewResponsePayload.test.ts ui/analyse/tests/moveReviewPersistence.test.ts ui/analyse/tests/moveReviewLedgerSurface.test.ts ui/analyse/tests/decisionComparisonSurface.test.ts
```

Expected:
- PASS for all listed tests.

- [ ] **Step 3: Run TypeScript compile check**

Run:

```bash
cd lila-docker/repos/lila
pnpm exec tsc -b ui/analyse/tsconfig.json
```

Expected:
- PASS with no TypeScript errors.

- [ ] **Step 4: Commit task 2**

```bash
git add lila-docker/repos/lila/ui/analyse/src/moveReview.ts
git commit -m "Wire MoveReview to coach surface"
```

## Task 3: Player-Facing Coach Styling

**Files:**
- Modify: `lila-docker/repos/lila/ui/analyse/css/_side.scss`

- [ ] **Step 1: Replace old support-panel styles with coach surface styles**

Inside `fieldset.analyse__move-review`, keep existing state, toolbar, commentary, saved-study, and blocking styles. Replace the `move-review-strategic-summary`, `move-review-decision-compare`, probe, and authoring block styles with `.move-review-coach` styles:

```scss
.move-review-coach {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.move-review-coach__header {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  padding-bottom: 0.55rem;
  border-bottom: 1px solid $c-border;

  h3 {
    margin: 0;
    font-size: 1rem;
    line-height: 1.3;
  }
}

.move-review-coach__eyebrow,
.move-review-coach__decision-kicker,
.move-review-coach__decision-label {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: $c-font-dim;
}

.move-review-coach__decision {
  padding: 0.7rem 0.75rem;
  border: 1px solid rgba($c-primary, 0.16);
  border-radius: 8px;
  background: rgba($c-bg-zebra, 0.72);
}

.move-review-coach__decision--match {
  border-color: rgba($c-good, 0.24);
  background: rgba($c-good, 0.08);
}

.move-review-coach__decision-topline,
.move-review-coach__decision-moves {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.55rem;
  align-items: center;
}

.move-review-coach__decision-topline {
  justify-content: space-between;
}

.move-review-coach__gap {
  display: inline-flex;
  align-items: center;
  padding: 0.08rem 0.42rem;
  border-radius: 999px;
  background: rgba($c-primary, 0.08);
  color: $c-primary;
  font-size: 0.72rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.move-review-coach__decision-move {
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  min-width: 0;
  padding: 0.14rem 0.42rem;
  border-radius: 999px;
  background: rgba($c-primary, 0.06);
}

.move-review-coach__decision-move--engine {
  background: rgba($c-primary, 0.1);
  color: $c-primary;
}

.move-review-coach__move-chip {
  min-width: 0;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.2;
}

.move-review-coach__section {
  h4 {
    margin: 0 0 0.4rem;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    text-transform: uppercase;
    color: $c-primary;
  }
}

.move-review-coach__reasons {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.move-review-coach__reason {
  padding: 0.58rem 0.65rem;
  border: 1px solid rgba($c-primary, 0.12);
  border-radius: 8px;
  background: $c-bg-zebra;
  font-size: 0.86rem;
  line-height: 1.42;
}

.move-review-coach__reason-label {
  display: block;
  margin-bottom: 0.16rem;
  color: $c-primary;
  font-size: 0.74rem;
  font-weight: 700;
}

.move-review-coach__target-chip,
.move-review-coach__book-chip {
  display: inline-flex;
  align-items: center;
  margin-inline-start: 0.25rem;
  padding: 0.05rem 0.35rem;
  border-radius: 4px;
  background: rgba($c-primary, 0.08);
  color: $c-primary;
  font-size: 0.76rem;
  font-weight: 700;
}

.move-review-coach__target-chip {
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: $c-primary;
    color: $c-bg-box;
    outline: none;
  }
}

.move-review-coach__practice,
.move-review-coach__details {
  padding: 0.6rem 0.7rem;
  border: 1px solid $c-border;
  border-radius: 8px;
  background: rgba($c-bg-low, 0.7);
}

.move-review-coach__practice {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  font-size: 0.82rem;
  color: $c-font-dim;

  strong {
    color: $c-font;
  }
}
```

- [ ] **Step 2: Run UI build without Docker if local Node is available**

Run:

```bash
cd lila-docker/repos/lila
node ui/build.mjs --no-install analyse
```

Expected:
- PASS if local Node is version 24+ and dependencies are present.
- If local Node is unavailable or too old, record the blocker and use Docker UI build after Docker Desktop is running.

- [ ] **Step 3: Run Docker UI build if Docker Desktop is running**

Run:

```bash
cd lila-docker
docker compose run --rm -w /lila ui node ui/build.mjs --no-install analyse
```

Expected:
- PASS with compiled analyse assets.
- If Docker Desktop is not running, report that visual/app verification is blocked by Docker engine state.

- [ ] **Step 4: Commit task 3**

```bash
git add lila-docker/repos/lila/ui/analyse/css/_side.scss
git commit -m "Style MoveReview coach experience"
```

## Task 4: Final Verification And Review

**Files:**
- No new files expected.

- [ ] **Step 1: Run targeted TS tests**

```bash
cd lila-docker/repos/lila
pnpm exec tsx --test ui/analyse/tests/moveReviewCoachSurface.test.ts ui/analyse/tests/moveReviewResponsePayload.test.ts ui/analyse/tests/moveReviewPersistence.test.ts ui/analyse/tests/moveReviewLedgerSurface.test.ts ui/analyse/tests/decisionComparisonSurface.test.ts
```

Expected:
- PASS.

- [ ] **Step 2: Run TypeScript compile check**

```bash
cd lila-docker/repos/lila
pnpm exec tsc -b ui/analyse/tsconfig.json
```

Expected:
- PASS.

- [ ] **Step 3: Run Scala renderer regression if sbt is available**

```bash
cd lila-docker/repos/lila
sbt "analyse/testOnly lila.analyse.test.MoveReviewRendererTest"
```

Expected:
- PASS.
- If `sbt` is unavailable locally, report it and do not claim Scala test verification.

- [ ] **Step 4: Inspect final diff**

```bash
git status --short --untracked-files=all
git diff --stat
git diff --check
```

Expected:
- Only intended MoveReview UI files remain changed.
- No whitespace errors.

- [ ] **Step 5: Final commit if previous task commits were skipped**

```bash
git add lila-docker/repos/lila/ui/analyse/src/moveReview/coachSurface.ts lila-docker/repos/lila/ui/analyse/tests/moveReviewCoachSurface.test.ts lila-docker/repos/lila/ui/analyse/src/moveReview.ts lila-docker/repos/lila/ui/analyse/css/_side.scss
git commit -m "Build MoveReview coach experience"
```

Expected:
- Commit succeeds, unless the task commits were already made.

## Self-Review Notes

Spec coverage:
- Player-facing review structure: Task 1 and Task 3.
- Existing payload reuse: Task 1 and Task 2.
- Trust-preserving ref behavior: Task 1 tests and module implementation.
- Backend truth behavior unchanged: Task 2 scope excludes backend changes.
- Testing and verification: Task 4.

Known boundaries:
- Whole-game story remains limited to current title/commentary payload in this first pass.
- Full Socratic retry mode and saved mistake cards are future work.
- Docker-based visual verification depends on Docker Desktop being available.
