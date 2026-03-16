import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildProbeResultsByPlyEntries,
  collectGameArcProbeMomentBundles,
  validateProbeResultAgainstRequest,
} from '../src/narrative/probePlanning';

describe('Game Arc probe planning', () => {
  test('collects internal refinement requests, dedupes by probe contract key, and caps at three moments', () => {
    const bundles = collectGameArcProbeMomentBundles({
      moments: [
        {
          ply: 10,
          cpAfter: 42,
          probeRefinementRequests: [
            {
              id: 'probe-1',
              fen: 'fen-1',
              moves: ['e2e4'],
              depth: 18,
              purpose: 'opening_branch_validation',
            },
          ],
        },
        {
          ply: 12,
          cpAfter: 18,
          probeRefinementRequests: [
            {
              id: 'probe-1b',
              fen: 'fen-1',
              moves: ['e2e4'],
              depth: 18,
              purpose: 'opening_branch_validation',
            },
          ],
        },
        {
          ply: 14,
          cpAfter: 7,
          probeRefinementRequests: [
            {
              id: 'probe-2',
              fen: 'fen-2',
              moves: ['g2g4'],
              depth: 20,
              purpose: 'latent_plan_refutation',
              questionId: 'q2',
            },
          ],
        },
        {
          ply: 16,
          cpBefore: -12,
          probeRefinementRequests: [
            {
              id: 'probe-3',
              fen: 'fen-3',
              moves: ['b2b4'],
              depth: 16,
              purpose: 'free_tempo_branches',
            },
          ],
        },
        {
          ply: 18,
          cpAfter: -35,
          probeRefinementRequests: [
            {
              id: 'probe-4',
              fen: 'fen-4',
              moves: ['h2h4'],
              depth: 16,
              purpose: 'keep_tension_branches',
            },
          ],
        },
      ],
    });

    assert.equal(bundles.length, 3);
    assert.deepEqual(
      bundles.map(bundle => [bundle.ply, bundle.baselineCp, bundle.requests[0]?.id]),
      [
        [10, 42, 'probe-1'],
        [14, 7, 'probe-2'],
        [16, -12, 'probe-3'],
      ],
    );
  });

  test('prioritizes compensation and strategic-branch moments ahead of earlier low-signal plies', () => {
    const bundles = collectGameArcProbeMomentBundles({
      moments: [
        {
          ply: 8,
          cpAfter: 12,
          probeRefinementRequests: [
            {
              id: 'probe-low',
              fen: 'fen-low',
              moves: ['a2a4'],
              depth: 16,
              purpose: 'quiet_followup',
            },
          ],
        },
        {
          ply: 16,
          cpAfter: 26,
          selectionKind: 'key',
          strategicBranch: true,
          strategicSalience: 'High',
          signalDigest: {
            compensation: 'return vector through line pressure and delayed recovery',
            compensationVectors: ['Line Pressure (0.7)', 'Delayed Recovery (0.6)'],
            investedMaterial: 100,
            dominantIdeaKind: 'target_fixing',
          },
          strategyPack: {
            longTermFocus: ['fix the queenside targets before recovering the pawn'],
          },
          probeRefinementRequests: [
            {
              id: 'probe-quiet-comp',
              fen: 'fen-quiet-comp',
              moves: ['b7b5'],
              depth: 18,
              purpose: 'latent_plan_refutation',
            },
          ],
        },
        {
          ply: 18,
          cpAfter: 30,
          selectionKind: 'key',
          strategicBranch: true,
          strategicSalience: 'High',
          signalDigest: {
            compensation: 'return vector through initiative and line pressure',
            compensationVectors: ['Initiative (0.6)', 'Line Pressure (0.5)'],
            investedMaterial: 120,
            dominantIdeaKind: 'king_attack_build_up',
          },
          probeRefinementRequests: [
            {
              id: 'probe-comp',
              fen: 'fen-comp',
              moves: ['g2g4'],
              depth: 18,
              purpose: 'latent_plan_refutation',
            },
          ],
        },
        {
          ply: 14,
          cpAfter: 8,
          selectionKind: 'key',
          strategicSalience: 'High',
          signalDigest: {
            dominantIdeaKind: 'line_occupation',
          },
          probeRefinementRequests: [
            {
              id: 'probe-key',
              fen: 'fen-key',
              moves: ['d2d4'],
              depth: 18,
              purpose: 'opening_branch_validation',
            },
          ],
        },
      ],
    });

    assert.deepEqual(
      bundles.map(bundle => bundle.ply),
      [16, 18, 14],
    );
  });

  test('validateProbeResultAgainstRequest drops results that miss required signals', () => {
    const valid = validateProbeResultAgainstRequest(
      {
        id: 'probe-1',
        fen: 'fen',
        moves: ['e2e4'],
        depth: 18,
        purpose: 'latent_plan_refutation',
        requiredSignals: ['replyPvs', 'l1Delta', 'futureSnapshot'],
      },
      {
        id: 'probe-1',
        evalCp: 23,
        bestReplyPv: ['e7e5'],
        deltaVsBaseline: -12,
        keyMotifs: ['forcing_exchange_pattern'],
        purpose: 'latent_plan_refutation',
        l1Delta: {
          materialDelta: 0,
          kingSafetyDelta: -1,
          centerControlDelta: 0,
          openFilesDelta: 1,
          mobilityDelta: -1,
        },
        futureSnapshot: {
          resolvedThreatKinds: ['Counterplay'],
          newThreatKinds: [],
          targetsDelta: {
            tacticalAdded: [],
            tacticalRemoved: [],
            strategicAdded: ['open h-file'],
            strategicRemoved: [],
          },
          planBlockersRemoved: [],
          planPrereqsMet: ['line opened'],
        },
      },
    );
    const invalid = validateProbeResultAgainstRequest(
      {
        id: 'probe-1',
        fen: 'fen',
        moves: ['e2e4'],
        depth: 18,
        purpose: 'latent_plan_refutation',
        requiredSignals: ['replyPvs', 'l1Delta', 'futureSnapshot'],
      },
      {
        id: 'probe-1',
        evalCp: 23,
        bestReplyPv: ['e7e5'],
        deltaVsBaseline: -12,
        keyMotifs: ['forcing_exchange_pattern'],
        purpose: 'latent_plan_refutation',
        l1Delta: {
          materialDelta: 0,
          kingSafetyDelta: -1,
          centerControlDelta: 0,
          openFilesDelta: 1,
          mobilityDelta: -1,
        },
      },
    );

    assert.equal(valid, true);
    assert.equal(invalid, false);
  });

  test('buildProbeResultsByPlyEntries filters empty bundles and preserves non-empty results', () => {
    const entries = buildProbeResultsByPlyEntries([
      { ply: 12, results: [] },
      {
        ply: 14,
        results: [
          {
            id: 'probe-2',
            evalCp: 17,
            bestReplyPv: ['g7g6'],
            deltaVsBaseline: 11,
            keyMotifs: ['plan_validation_signal'],
          },
        ],
      },
    ]);

    assert.deepEqual(entries, [
      {
        ply: 14,
        results: [
          {
            id: 'probe-2',
            evalCp: 17,
            bestReplyPv: ['g7g6'],
            deltaVsBaseline: 11,
            keyMotifs: ['plan_validation_signal'],
          },
        ],
      },
    ]);
  });
});
