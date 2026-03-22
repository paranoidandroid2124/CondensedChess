import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  decodeBookmakerResponse,
  signalDigestFromResponse,
} from '../src/bookmaker/responsePayload';

describe('bookmaker response payload', () => {
  test('signal digest preserves authoring evidence alongside strategic fields', () => {
    const digest = signalDigestFromResponse({
      signalDigest: {
        authoringEvidence: 'Probe the h-file before committing.',
        dominantIdeaKind: 'prophylaxis',
        dominantIdeaFocus: 'h5',
      },
    });

    assert.equal(digest?.authoringEvidence, 'Probe the h-file before committing.');
    assert.equal(digest?.dominantIdeaKind, 'prophylaxis');
    assert.equal(digest?.dominantIdeaFocus, 'h5');
  });

  test('decodeBookmakerResponse reuses fallback prose and supporting arrays when refined payload omits them', () => {
    const decoded = decodeBookmakerResponse(
      {
        sourceMode: 'llm_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
        signalDigest: {
          authoringEvidence: 'Question remains open.',
        },
      },
      {
        html: '<p>cached html</p>',
        commentary: 'cached commentary',
        probeRequests: [
          {
            id: 'probe-1',
            fen: 'fen-1',
            moves: ['g2g4'],
            depth: 20,
          },
        ],
        authorQuestions: [
          {
            id: 'question-1',
            kind: 'plan_gap',
            priority: 1,
            question: 'Why is g4 delayed?',
            confidence: 'medium',
          },
        ],
        authorEvidence: [
          {
            questionId: 'question-1',
            questionKind: 'plan_gap',
            question: 'Why is g4 delayed?',
            status: 'pending',
            branchCount: 0,
            pendingProbeCount: 1,
          },
        ],
      },
    );

    assert.equal(decoded.html, '<p>cached html</p>');
    assert.equal(decoded.commentary, 'cached commentary');
    assert.equal(decoded.probeRequests.length, 1);
    assert.equal(decoded.authorQuestions.length, 1);
    assert.equal(decoded.authorEvidence.length, 1);
    assert.equal(decoded.signalDigest?.authoringEvidence, 'Question remains open.');
  });

  test('decodeBookmakerResponse preserves optional strategyPack payload', () => {
    const decoded = decodeBookmakerResponse({
      strategyPack: {
        schema: 'chesstory.strategyPack.v2',
        sideToMove: 'black',
        strategicIdeas: [
          {
            ideaId: 'idea_1',
            ownerSide: 'white',
            kind: 'king_attack_build_up',
            group: 'tactical_forcing',
            readiness: 'build',
            focusSquares: ['g7', 'h7'],
            confidence: 0.91,
          },
        ],
        pieceRoutes: [
          {
            ownerSide: 'white',
            piece: 'Q',
            from: 'd1',
            route: ['d1', 'g4', 'h5'],
            purpose: 'mate threats',
            strategicFit: 0.9,
            tacticalSafety: 0.72,
            surfaceConfidence: 0.84,
            surfaceMode: 'toward',
          },
        ],
        pieceMoveRefs: [],
        directionalTargets: [
          {
            targetId: 'target_1',
            ownerSide: 'white',
            piece: 'Q',
            from: 'd1',
            targetSquare: 'h7',
            readiness: 'build',
          },
        ],
        longTermFocus: ['keep the initiative rather than recovering material'],
      },
    });

    assert.equal(decoded.strategyPack?.sideToMove, 'black');
    assert.equal(decoded.strategyPack?.strategicIdeas[0]?.ownerSide, 'white');
    assert.equal(decoded.strategyPack?.pieceRoutes[0]?.route[2], 'h5');
    assert.equal(decoded.strategyPack?.directionalTargets[0]?.targetSquare, 'h7');
  });

  test('decodeBookmakerResponse preserves strategicPlanExperiments for UI support labels', () => {
    const decoded = decodeBookmakerResponse({
      mainStrategicPlans: [
        {
          planId: 'king_attack',
          subplanId: 'rook_lift_scaffold',
          planName: 'Kingside Attack',
          rank: 1,
          score: 0.81,
          preconditions: [],
          executionSteps: [],
          failureModes: [],
          viability: { score: 0.75, label: 'high', risk: 'thin support' },
        },
      ],
      strategicPlanExperiments: [
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
      ],
    });

    assert.equal(decoded.mainStrategicPlans[0]?.subplanId, 'rook_lift_scaffold');
    assert.equal(decoded.strategicPlanExperiments.length, 1);
    assert.equal(decoded.strategicPlanExperiments[0]?.evidenceTier, 'pv_coupled');
    assert.equal(decoded.strategicPlanExperiments[0]?.moveOrderSensitive, true);
  });
});
