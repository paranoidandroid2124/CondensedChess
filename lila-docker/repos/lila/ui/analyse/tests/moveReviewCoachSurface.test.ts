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
    assert.match(html, /Key moment/);
    assert.match(html, /Coach verdict/);
    assert.match(html, /Why it mattered/);
    assert.match(html, /What to watch next/);
    assert.match(html, /Try the line/);
    assert.match(html, /Remember this/);
    assert.match(html, /data-move-review-scene="0"/);
    assert.match(html, /data-move-review-scene-panel/);
    assert.match(html, /move-review-player__timeline-label">Verdict/);
    assert.match(html, /move-review-player__board-shell/);
    assert.match(html, /move-review-player__board-preview/);
    assert.match(html, /move-review-player__board-title/);
    assert.match(html, /move-review-player__scene-line/);
    assert.match(html, /data-scene-line="Nf3 d5"/);
    assert.match(html, /move-review-player__detail-layer/);
    assert.match(html, /Coach notes/);
    assert.match(html, /data-scene-square="d5"/);
    assert.match(html, /data-scene-board-title="Verdict board"/);
    assert.match(html, /data-scene-board="8\/8\/8\/8\/8\/5N2\/8\/8 b - - 1 1\|g1f3"/);
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

  test('falls back to raw commentary when no player surface exists', () => {
    const html = decorateMoveReviewHtml('<p>Only commentary.</p>', refs, null);

    assert.equal(html, '<p>Only commentary.</p>');
  });
});
