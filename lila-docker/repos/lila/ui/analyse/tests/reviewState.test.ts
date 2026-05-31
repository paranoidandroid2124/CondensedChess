import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { initialReviewState, reduceReviewState } from '../src/review/state';

describe('review state', () => {
  test('starts in overview with utility panel cleared', () => {
    assert.deepEqual(initialReviewState(), {
      surfaceMode: 'review',
      primaryTab: 'overview',
      utilityPanel: null,
      momentFilter: 'all',
      selectedMomentPly: null,
      selectedCollapseId: null,
      analysisDetailsOpen: false,
    });
  });

  test('switches primary tabs directly', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'primary-tab', tab: 'moves' });
    assert.equal(next.primaryTab, 'moves');
    assert.equal(next.utilityPanel, null);
  });

  test('utility panels open and update the current primary tab', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'utility-panel', panel: 'explorer' });
    assert.equal(next.primaryTab, 'explorer');
    assert.equal(next.utilityPanel, 'explorer');
  });

  test('moment filters route into the moments tab', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'moment-filter', filter: 'collapses' });
    assert.equal(next.primaryTab, 'moments');
    assert.equal(next.momentFilter, 'collapses');
  });

  test('selecting a moment focuses moments', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'select-moment', ply: 27 });
    assert.equal(next.primaryTab, 'moments');
    assert.equal(next.selectedMomentPly, 27);
  });

  test('selecting a collapse focuses repair', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'select-collapse', collapseId: '22-27' });
    assert.equal(next.primaryTab, 'repair');
    assert.equal(next.selectedCollapseId, '22-27');
  });
});
