import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildDecisionComparisonRows,
  buildDecisionComparisonSurface,
  formatDecisionComparisonHeadline,
  formatDecisionTargetComparison,
} from '../src/decisionComparison';

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
      deferredReason: 'it leaves the target less clear',
      chosenMatchesBest: true,
    });

    assert.equal(surface.headline, 'Played Nd2 · Compared Qc2');
    assert.equal(
      surface.secondary,
      'Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.',
    );
    assert.equal(surface.engineLine, 'Nd2 ...Na6 f3 Nc7');
  });

  test('formats target comparison as subordinate endpoint metadata', () => {
    assert.equal(
      formatDecisionTargetComparison({
        chosenTarget: 'e5',
        chosenTargetKind: 'isolated_pawn',
        bestTarget: 'd5',
        bestTargetKind: 'iqp',
      }),
      'Line target: played e5 (isolated pawn); coach line points at d5 (isolated queen pawn).',
    );
  });

  test('frames move comparison as a coach line instead of a bare suggestion', () => {
    assert.equal(
      formatDecisionComparisonHeadline({
        engineBestMove: 'Nd2',
        cpLossVsChosen: 11,
      }),
      'Coach move is Nd2 (tiny score gap).',
    );

    const surface = buildDecisionComparisonSurface({
      chosenMove: 'Qc2',
      engineBestMove: 'Nd2',
      cpLossVsChosen: 11,
      chosenMatchesBest: false,
    });

    assert.equal(surface.headline, 'Played Qc2 · coach move Nd2');
    assert.equal(surface.gap, 'tiny score gap');
  });

  test('labels engine pv as a candidate line for players', () => {
    const rows = buildDecisionComparisonRows({
      chosenMove: 'Nd2',
      engineBestMove: 'Nd2',
      engineBestPv: ['Nd2', '...Na6'],
      chosenMatchesBest: true,
    });

    assert.equal(rows[0]?.label, 'Candidate line');
  });
});
