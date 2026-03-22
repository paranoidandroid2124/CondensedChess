import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  formatStrategicPlanText,
  strategicPlanExperimentIndex,
} from '../src/bookmaker/planSupportSurface';

describe('plan support surface', () => {
  test('formats pv-coupled plans as engine-line only with move-order caution', () => {
    const index = strategicPlanExperimentIndex([
      {
        planId: 'king_attack',
        subplanId: 'rook_lift_scaffold',
        themeL1: 'king_attack',
        evidenceTier: 'pv_coupled',
        supportProbeCount: 0,
        refuteProbeCount: 0,
        bestReplyStable: false,
        futureSnapshotAligned: false,
        counterBreakNeutralized: false,
        moveOrderSensitive: true,
        experimentConfidence: 0.54,
      },
    ]);

    assert.equal(
      formatStrategicPlanText(
        {
          planId: 'king_attack',
          subplanId: 'rook_lift_scaffold',
          planName: 'Kingside Attack',
          rank: 1,
          score: 0.81,
        },
        index,
        { includeRank: true },
      ),
      '1. Kingside Attack (0.81 · Engine-line only · Move-order sensitive)',
    );
  });

  test('formats evidence-backed plans as probe-backed', () => {
    const index = strategicPlanExperimentIndex([
      {
        planId: 'minority_attack',
        subplanId: 'weakness_fixation',
        themeL1: 'minority_attack',
        evidenceTier: 'evidence_backed',
        supportProbeCount: 2,
        refuteProbeCount: 0,
        bestReplyStable: true,
        futureSnapshotAligned: true,
        counterBreakNeutralized: true,
        moveOrderSensitive: false,
        experimentConfidence: 0.82,
      },
    ]);

    assert.equal(
      formatStrategicPlanText(
        {
          planId: 'minority_attack',
          subplanId: 'weakness_fixation',
          planName: 'Minority Attack',
          score: 0.73,
        },
        index,
      ),
      'Minority Attack (0.73 · Probe-backed)',
    );
  });
});
