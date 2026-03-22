import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { initialReviewState, reduceReviewState, shouldFetchReviewPatterns } from '../src/review/state';

describe('review state', () => {
  test('starts in moves with utility panel cleared', () => {
    assert.deepEqual(initialReviewState(), {
      primaryTab: 'moves',
      utilityPanel: null,
      momentFilter: 'all',
      selectedMomentPly: null,
      selectedCollapseId: null,
    });
  });

  test('switches primary tabs directly', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'primary-tab', tab: 'moves' });
    assert.equal(next.primaryTab, 'moves');
    assert.equal(next.utilityPanel, null);
  });

  test('utility panels open without replacing the current primary tab', () => {
    const next = reduceReviewState(initialReviewState(), { type: 'utility-panel', panel: 'explorer' });
    assert.equal(next.primaryTab, 'moves');
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

  test('patterns fetches only when the tab is active and dna is missing', () => {
    assert.equal(
      shouldFetchReviewPatterns(
        { primaryTab: 'patterns' },
        { narrativeAvailable: true, hasDnaData: false, dnaLoading: false },
      ),
      true,
    );
    assert.equal(
      shouldFetchReviewPatterns(
        { primaryTab: 'patterns' },
        { narrativeAvailable: true, hasDnaData: true, dnaLoading: false },
      ),
      false,
    );
    assert.equal(
      shouldFetchReviewPatterns(
        { primaryTab: 'overview' },
        { narrativeAvailable: true, hasDnaData: false, dnaLoading: false },
      ),
      false,
    );
  });
});
