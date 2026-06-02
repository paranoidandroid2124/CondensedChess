import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { renderMoveReviewPlayerSurfaceHtml } from '../src/moveReview/playerSurfaceRendering';

describe('move review player surface rendering', () => {
  test('renders strategic relation summary cue as the lead player-facing support row', () => {
    const html = renderMoveReviewPlayerSurfaceHtml({
      html: '<div class="commentary">Original explanation</div>',
      moveReviewExplanation: null,
      refs: null,
      playerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        title: 'Move review: Be4',
        summaryRows: [
          {
            label: 'Line relation',
            text: 'Why it works: the line runs from e4 through f5 toward g6. Next check: the line geometry through e4, f5, and g6.',
            tone: 'relation',
            refSans: [],
            authority: {
              kind: 'strategic_relation',
              token: 'xray',
              openingFamily: null,
              target: 'g6',
              openingBook: null,
            },
          },
          {
            label: 'Main plans',
            text: 'Improve the worst piece',
            refSans: [],
            authority: null,
          },
        ],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
        decisionComparison: null,
      },
    });

    const relationIndex = html.indexOf('Line relation');
    const mainPlanIndex = html.indexOf('Main plans');

    assert(relationIndex >= 0, html);
    assert(mainPlanIndex > relationIndex, html);
    assert.match(html, /move-review-strategic-summary__row--tone-relation/);
    assert.match(html, /move-review-strategic-summary__row--authority-strategic_relation/);
    assert.match(html, /move-review-strategic-summary__relation-chip">X-ray<\/span>/);
    assert.match(html, /data-move-review-square="g6"/);
  });
});
