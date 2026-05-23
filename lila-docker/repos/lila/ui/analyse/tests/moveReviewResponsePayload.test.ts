import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  decodeMoveReviewResponse,
  moveReviewNeedsRetry,
} from '../src/moveReview/responsePayload';

describe('moveReview response payload', () => {
  test('decodeMoveReviewResponse accepts minimized payload without raw strategic carriers', () => {
    const decoded = decodeMoveReviewResponse({
      html: '<p>cached html</p>',
      commentary: 'cached commentary',
      sourceMode: 'ai_polished',
      model: 'gpt-5-mini',
      cacheHit: false,
      mainStrategicPlanCount: 2,
      diagnostics: {
        status: 'ready',
        sourceModeReason: 'ready',
      },
    });

    assert.equal(decoded.html, '<p>cached html</p>');
    assert.equal(decoded.commentary, 'cached commentary');
    assert.equal(decoded.mainStrategicPlanCount, 2);
    assert.equal(decoded.diagnostics?.status, 'ready');
    assert.equal(Object.prototype.hasOwnProperty.call(decoded, 'strategyPack'), false);
    assert.equal(Object.prototype.hasOwnProperty.call(decoded, 'signalDigest'), false);
  });

  test('decodeMoveReviewResponse reuses fallback prose and supporting arrays when refined payload omits them', () => {
    const decoded = decodeMoveReviewResponse(
      {
        sourceMode: 'ai_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
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
  });

  test('raw strategic carriers alone do not synthesize a product surface or plan count', () => {
    const decoded = decodeMoveReviewResponse({
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
      signalDigest: {
        authoringEvidence: 'Question remains open.',
      },
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
    } as any);

    assert.equal(decoded.moveReviewPlayerSurface, null);
    assert.equal(decoded.mainStrategicPlanCount, 0);
  });

  test('decodeMoveReviewResponse ignores legacy mainStrategicPlans when count is absent', () => {
    const decoded = decodeMoveReviewResponse({
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
    });

    assert.equal(decoded.mainStrategicPlanCount, 0);
  });

  test('decodeMoveReviewResponse drops internal polish diagnostics from optional metadata', () => {
    const decoded = decodeMoveReviewResponse({
      polishMeta: {
        provider: 'openai',
        model: 'gpt-test',
        sourceMode: 'ai_polished',
        validationPhase: 'middlegame',
        validationReasons: ['contract_violation'],
        cacheHit: false,
        promptTokens: 123,
        cachedTokens: 45,
        completionTokens: 67,
        estimatedCostUsd: 0.0123,
        strategyCoverage: {
          mode: 'strict',
          enforced: true,
          threshold: 0.7,
          availableCategories: 4,
          coveredCategories: 3,
          requiredCategories: 2,
          coverageScore: 0.75,
          passesThreshold: true,
          planSignals: 2,
          planHits: 1,
          routeSignals: 1,
          routeHits: 1,
          focusSignals: 1,
          focusHits: 1,
        },
      },
    });

    assert.deepEqual(decoded.polishMeta?.validationReasons, []);
    assert.equal(decoded.polishMeta?.promptTokens, null);
    assert.equal(decoded.polishMeta?.cachedTokens, null);
    assert.equal(decoded.polishMeta?.completionTokens, null);
    assert.equal(decoded.polishMeta?.estimatedCostUsd, null);
    assert.equal(decoded.polishMeta?.strategyCoverage, null);
  });

  test('retry gating follows backend diagnostics instead of fallback prose tokens', () => {
    const displayableFallback = decodeMoveReviewResponse({
      sourceMode: 'fallback_rule_invalid',
      commentary: 'theme: rook lift keeps {seed} alive',
      diagnostics: {
        status: 'fallback_available',
        sourceModeReason: 'invalid_polish',
      },
    });

    assert.equal(displayableFallback.diagnostics?.status, 'fallback_available');
    assert.equal(moveReviewNeedsRetry(displayableFallback), false);

    const backendBlockedFallback = decodeMoveReviewResponse({
      sourceMode: 'fallback_rule_invalid',
      commentary: 'This move keeps the position playable.',
      diagnostics: {
        status: 'retryable_fallback',
        sourceModeReason: 'internal_marker_leak',
      },
    });

    assert.equal(backendBlockedFallback.diagnostics?.sourceModeReason, 'internal_marker_leak');
    assert.equal(moveReviewNeedsRetry(backendBlockedFallback), true);
  });

  test('retry gating ignores malformed or absent diagnostics', () => {
    const malformed = decodeMoveReviewResponse({
      sourceMode: 'fallback_rule_invalid',
      commentary: 'This move mentions theme: in saved prose.',
      diagnostics: {
        status: 'retryable_fallback',
      },
    });

    assert.equal(malformed.diagnostics, null);
    assert.equal(moveReviewNeedsRetry(malformed), false);

    const absent = decodeMoveReviewResponse({
      sourceMode: 'fallback_rule_invalid',
      commentary: 'under strict evidence mode',
    });

    assert.equal(absent.diagnostics, null);
    assert.equal(moveReviewNeedsRetry(absent), false);
  });
});
