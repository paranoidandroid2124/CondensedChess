import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { shouldIgnoreReviewCardClick } from '../src/narrative/narrativeView';

describe('narrative review card click guard', () => {
  test('ignores interactive descendants inside a review card', () => {
    const dom = new JSDOM(`
      <section class="narrative-moment">
        <button class="patch-replay-open-btn">Replay</button>
        <span data-board="fen|e2e4">e4</span>
        <span data-route="c1-g5" data-route-fen="fen">Bcg5</span>
      </section>
    `);
    const doc = dom.window.document;

    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('button')), true);
    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('[data-board]')), true);
    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('[data-route]')), true);
  });

  test('allows plain card content to select the moment', () => {
    const dom = new JSDOM(`
      <section class="narrative-moment">
        <div class="narrative-body">
          <span class="plain-copy">Critical turning point</span>
        </div>
      </section>
    `);
    const doc = dom.window.document;

    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('.plain-copy')), false);
  });
});
