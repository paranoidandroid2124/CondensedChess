import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { buildDecisionComparisonSurface } from '../src/decisionComparison';

describe('decisionComparison surface', () => {
  test('prefers exact comparative consequence over generic deferred copy', () => {
    const surface = buildDecisionComparisonSurface({
      chosenMove: 'Nd2',
      engineBestMove: 'Nd2',
      engineBestPv: ['Nd2', '...Na6', 'f3', 'Nc7'],
      comparedMove: 'Qc2',
      comparativeConsequence:
        'Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.',
      comparativeSource: 'exact_target_fixation_delta',
      deferredMove: 'Qc2',
      deferredReason: 'it trails the engine line by about 11cp',
      chosenMatchesBest: true,
    });

    assert.equal(surface.headline, 'Chosen Nd2 · Compared Qc2');
    assert.equal(
      surface.secondary,
      'Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.',
    );
    assert.equal(surface.engineLine, 'Nd2 ...Na6 f3 Nc7');
  });
});
