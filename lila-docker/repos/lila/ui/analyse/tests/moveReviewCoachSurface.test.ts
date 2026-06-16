import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { decorateMoveReviewHtml } from '../src/moveReview/coachSurface';
import type { MoveReviewPlayerSurfaceV1, MoveReviewRefsV1 } from '../src/moveReview/responsePayload';

const interactionHandlersSource = readFileSync(
  fileURLToPath(new URL('../src/moveReview/interactionHandlers.ts', import.meta.url)),
  'utf8',
);

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
        token: null,
        openingFamily: null,
        target: 'd5',
        openingBook: null,
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
    comparedSan: null,
    chosenMatchesBest: true,
    secondaryText: 'The move keeps the main plan alive.',
    targetComparison: null,
    refSans: ['Nf3', 'd5'],
  },
  ...overrides,
});

describe('moveReview coach surface', () => {
  test('renders player-facing coach structure without developer labels', () => {
    const html = decorateMoveReviewHtml('<div class="commentary"><p>Main note.</p></div>', refs, playerSurface());

    assert.match(html, /move-review-coach/);
    assert.match(html, /move-review-player/);
    assert.match(html, /data-move-review-player/);
    assert.match(html, /Move review/);
    assert.match(html, /Your choice/);
    assert.match(html, /Close choice/);
    assert.doesNotMatch(html, /\+0\.4/);
    assert.match(html, /Why it mattered/);
    assert.match(html, /What to watch next/);
    assert.match(html, /Try the line/);
    assert.match(html, /Remember this/);
    assert.match(html, /data-move-review-scene="0"/);
    assert.match(html, /data-move-review-scene-panel/);
    assert.match(html, /aria-label="Review flow" role="tablist"/);
    assert.match(html, /data-scene-flow-key="verdict"/);
    assert.match(html, /move-review-player__timeline-label--long">Verdict/);
    assert.match(html, /move-review-player__timeline-label--short">Recall/);
    assert.match(html, /move-review-player__timeline-action">Judge the move/);
    assert.match(html, /move-review-player__timeline-action">Find the reason/);
    assert.match(html, /move-review-player__timeline-action">Replay the line/);
    assert.match(html, /move-review-player__board-shell/);
    assert.match(html, /move-review-player__board-anchor/);
    assert.match(html, /move-review-player__board-anchor-label">Board first/);
    assert.match(html, /move-review-player__board-anchor-scene">Verdict/);
    assert.match(html, /move-review-player__board-anchor-move">Nf3/);
    assert.match(html, /move-review-player__board-preview/);
    assert.match(html, /move-review-player__board-title/);
    assert.match(html, /move-review-player__board-note/);
    assert.match(html, /move-review-player__scene-focus/);
    assert.match(html, /move-review-player__scene-focus-label">Board focus/);
    assert.match(html, /move-review-player__scene-focus-square" data-move-review-square="d5"/);
    assert.match(html, /move-review-player__scene-line/);
    assert.match(html, /data-scene-line="Nf3 d5"/);
    assert.match(html, /data-scene-line-cue="Nf3 d5"/);
    assert.match(html, /Decision · 1\/5/);
    assert.match(html, /move-review-player__timeline-copy/);
    assert.match(html, /move-review-player__timeline-kicker">Decision/);
    assert.match(html, /move-review-player__detail-layer/);
    assert.match(html, /Coach notes/);
    assert.match(html, /Board shows · Decision/);
    assert.match(html, /Move 1\/2/);
    assert.match(html, /Prev move/);
    assert.match(html, /Next move/);
    assert.match(html, /data-move-review-board-reset/);
    assert.match(html, /Scene board/);
    assert.match(html, /data-scene-square="d5"/);
    assert.match(html, /move-review-player__board-cue-item--move/);
    assert.match(html, /data-scene-board-title="Position tied to the choice"/);
    assert.match(html, /data-scene-board-note="Compare the move on the board before reading the verdict\."/);
    assert.match(html, /<p>Compare the move on the board before reading the verdict\.<\/p>/);
    assert.match(html, /data-scene-board-note="Keep this square in view; the reason lives on the board\."/);
    assert.match(html, /data-scene-board-note="Use this position to decide what the next move should improve\."/);
    assert.match(html, /data-scene-board-note="Play the line one move at a time; the board follows each click\."/);
    assert.match(html, /data-scene-board-note="Save this pattern as the cue to recognize in your next game\."/);
    assert.match(html, /data-scene-label="Verdict"/);
    assert.match(html, /data-scene-control-label="See why"/);
    assert.match(html, /data-scene-board="8\/8\/8\/8\/8\/5N2\/8\/8 b - - 1 1\|g1f3"/);
    assert.match(html, /Next: See why/);
    assert.doesNotMatch(html, /authority/i);
    assert.doesNotMatch(html, /diagnostics/i);
    assert.doesNotMatch(html, /Evidence Probes/);
  });

  test('keeps board cue compact while preserving the full replay line', () => {
    const longRefs: MoveReviewRefsV1 = {
      ...refs,
      variations: [
        {
          lineId: 'main',
          scoreCp: 42,
          mate: null,
          depth: 18,
          moves: [
            ...refs.variations[0].moves,
            {
              refId: 'main-3',
              san: 'e4',
              uci: 'e2e4',
              fenAfter: '8/8/8/3p4/4P3/5N2/8/8 b - - 0 2',
              ply: 3,
              moveNo: 2,
              marker: '2.',
            },
            {
              refId: 'main-4',
              san: 'e5',
              uci: 'e7e5',
              fenAfter: '8/8/8/3pp3/4P3/5N2/8/8 w - - 0 3',
              ply: 4,
              moveNo: 2,
              marker: '2...',
            },
            {
              refId: 'main-5',
              san: 'Bb5',
              uci: 'f1b5',
              fenAfter: '8/8/8/1B1pp3/4P3/5N2/8/8 b - - 1 3',
              ply: 5,
              moveNo: 3,
              marker: '3.',
            },
          ],
        },
      ],
    };
    const longLine = ['Nf3', 'd5', 'e4', 'e5', 'Bb5'];
    const html = decorateMoveReviewHtml(
      '<p>Main note.</p>',
      longRefs,
      playerSurface({
        decisionComparison: {
          kicker: 'Decision point',
          gapLabel: '+0.4',
          chosenSan: 'Nf3',
          engineSan: 'Nf3',
          comparedSan: null,
          chosenMatchesBest: true,
          secondaryText: 'The move keeps the main plan alive.',
          targetComparison: null,
          refSans: longLine,
        },
      }),
    );

    assert.match(html, /data-scene-line="Nf3 d5 e4 e5 Bb5"/);
    assert.match(html, /data-scene-line-cue="Nf3 d5 e4 e5"/);
    assert.doesNotMatch(html, /data-scene-line-cue="[^"]*Bb5/);
  });

  test('keeps board context rail synced with scene and move state', () => {
    assert.match(interactionHandlersSource, /move-review-player__board-anchor-scene/);
    assert.match(interactionHandlersSource, /move-review-player__board-anchor-move/);
    assert.match(interactionHandlersSource, /panel\.dataset\.sceneLabel/);
    assert.match(interactionHandlersSource, /moveLabelFromElement\(activeMove\)/);
    assert.match(interactionHandlersSource, /data-move-review-board-reset/);
    assert.match(interactionHandlersSource, /restoreMoveReviewPlayerBoard\(this\)/);
  });

  test('moves extra reasons into scene detail layers', () => {
    const html = decorateMoveReviewHtml(
      '<p>Main note.</p>',
      refs,
      playerSurface({
        summaryRows: [
          { label: 'First reason', text: 'Keep the file visible.', refSans: ['Nf3'] },
          { label: 'Second reason', text: 'Hold pressure on d5.', refSans: ['d5'] },
          { label: 'Third reason', text: 'Do not turn the scene into a scroll note.', refSans: ['Nf3', 'd5'] },
        ],
        advancedRows: [
          { label: 'First plan', text: 'Improve the rook.', refSans: ['Nf3'] },
          { label: 'Second plan', text: 'Watch the pin.', refSans: ['d5'] },
          { label: 'Third plan', text: 'Keep the deeper note in the drawer.', refSans: ['Nf3', 'd5'] },
        ],
      }),
    );

    assert.match(html, /<summary>More reasons<\/summary>[\s\S]*Third reason/);
    assert.match(html, /<summary>More plan notes<\/summary>[\s\S]*Third plan/);
    assert.match(html, /move-review-player__detail-layer/);
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
    assert.match(html, /data-board="8\/8\/8\/8\/8\/5N2\/8\/8 b - - 1 1\|g1f3"/);
    assert.match(html, /<code class="move-review-coach__move-chip">Qh5<\/code>/);
  });

  test('preserves repeated SAN order from checked refs', () => {
    const repeatedRefs: MoveReviewRefsV1 = {
      ...refs,
      variations: [
        {
          lineId: 'repeat',
          scoreCp: 18,
          mate: null,
          depth: 16,
          moves: [
            {
              refId: 'repeat-1',
              san: 'Nf3',
              uci: 'g1f3',
              fenAfter: '8/8/8/8/8/5N2/8/8 b - - 1 1',
              ply: 1,
              moveNo: 1,
              marker: '1.',
            },
            {
              refId: 'repeat-2',
              san: 'Nf3',
              uci: 'g8f6',
              fenAfter: '8/8/5n2/8/8/5N2/8/8 w - - 2 2',
              ply: 2,
              moveNo: 1,
              marker: '1...',
            },
          ],
        },
      ],
    };

    const html = decorateMoveReviewHtml(
      '<div class="commentary"><p>Main note.</p></div>',
      repeatedRefs,
      playerSurface({
        summaryRows: [
          {
            label: 'Line clue',
            text: 'The checked line repeats the same SAN token in order.',
            refSans: ['Nf3', 'Nf3'],
          },
        ],
        decisionComparison: null,
      }),
    );

    assert.match(html, /data-ref-id="repeat-1"[\s\S]*data-ref-id="repeat-2"/);
  });

  test('builds a board-synced player fallback from refs', () => {
    const html = decorateMoveReviewHtml('<p>Only commentary.</p>', refs, null);

    assert.match(html, /data-move-review-player/);
    assert.match(html, /Coach review/);
    assert.match(html, /data-scene-line="Nf3 d5"/);
    assert.match(html, /Try the line/);
    assert.match(html, /Remember this/);
    assert.match(html, /Coach notes/);
    assert.match(html, /data-scene-board="8\/8\/8\/8\/8\/5N2\/8\/8 b - - 1 1\|g1f3"/);
  });

  test('falls back to raw commentary when no player surface or refs exist', () => {
    const html = decorateMoveReviewHtml('<p>Only commentary.</p>', null, null);

    assert.equal(html, '<p>Only commentary.</p>');
  });
});
