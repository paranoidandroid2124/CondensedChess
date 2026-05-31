import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  decodeMoveReviewResponse,
  moveReviewNeedsRetry,
} from '../src/moveReview/responsePayload';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

const quotedStrings = (source: string): string[] => [...source.matchAll(/'([^']+)'/g)].map(match => match[1] ?? '');

const backendCatalogRelationTokens = (): string[] => {
  const analyzerSource = readFileSync(
    resolve(repoRoot, 'modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala'),
    'utf8',
  );
  const catalogSource = readFileSync(
    resolve(repoRoot, 'modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservation.scala'),
    'utf8',
  );
  const relationKindBlock = analyzerSource.match(/object RelationKind:([\s\S]*?)val All:/)?.[1] ?? '';
  const valuesByName = new Map(
    [...relationKindBlock.matchAll(/\bval\s+([A-Za-z0-9]+)\s*=\s*"([^"]+)"/g)].map(match => [
      match[1] ?? '',
      match[2] ?? '',
    ]),
  );
  const implementedBlock =
    catalogSource.match(/val Implemented: List\[RelationObservationDescriptor\]\s*=\s*List\(([\s\S]*?)\)\s*val DeferredRelationKinds/)?.[1] ?? '';
  const implementedNames = [
    ...implementedBlock.matchAll(/relationKind\s*=\s*MoveReviewExchangeAnalyzer\.RelationKind\.([A-Za-z0-9]+)/g),
  ].map(match => match[1] ?? '');

  return implementedNames.map(name => {
    const token = valuesByName.get(name);
    assert.ok(token, `Missing backend relation token for ${name}`);
    return token;
  });
};

const frontendRelationTokens = (): string[] => {
  const source = readFileSync(resolve(repoRoot, 'ui/analyse/src/moveReview/responsePayload.ts'), 'utf8');
  const tokenBlock = source.match(/const strategicRelationAuthorityTokens = new Set\(\[([\s\S]*?)\]\);/)?.[1] ?? '';

  return quotedStrings(tokenBlock);
};

const backendDeferredRelationTokens = (): string[] => {
  const analyzerSource = readFileSync(
    resolve(repoRoot, 'modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala'),
    'utf8',
  );
  const catalogSource = readFileSync(
    resolve(repoRoot, 'modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservation.scala'),
    'utf8',
  );
  const relationKindBlock = analyzerSource.match(/object RelationKind:([\s\S]*?)val All:/)?.[1] ?? '';
  const valuesByName = new Map(
    [...relationKindBlock.matchAll(/\bval\s+([A-Za-z0-9]+)\s*=\s*"([^"]+)"/g)].map(match => [
      match[1] ?? '',
      match[2] ?? '',
    ]),
  );
  const deferredBlock =
    catalogSource.match(/val Deferred: List\[DeferredRelationDescriptor\]\s*=\s*List\(([\s\S]*?)\)\s*val Implemented/)?.[1] ?? '';
  const deferredNames = [
    ...deferredBlock.matchAll(/relationKind\s*=\s*MoveReviewExchangeAnalyzer\.RelationKind\.([A-Za-z0-9]+)/g),
  ].map(match => match[1] ?? '');

  return deferredNames.map(name => {
    const token = valuesByName.get(name);
    assert.ok(token, `Missing backend deferred relation token for ${name}`);
    return token;
  });
};

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

  test('decodeMoveReviewResponse reuses fallback prose without rehydrating supporting arrays', () => {
    const decoded = decodeMoveReviewResponse(
      {
        sourceMode: 'ai_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
      },
      {
        html: '<p>cached html</p>',
        commentary: 'cached commentary',
      },
    );

    assert.equal(decoded.html, '<p>cached html</p>');
    assert.equal(decoded.commentary, 'cached commentary');
    assert.equal(decoded.probeRequests.length, 0);
    assert.equal(decoded.authorQuestions.length, 0);
    assert.equal(decoded.authorEvidence.length, 0);
  });

  test('decodeMoveReviewResponse ignores legacy raw probe and authoring carriers', () => {
    const decoded = decodeMoveReviewResponse({
      probeRequests: [{ id: 'probe-1', fen: 'fen-1', moves: ['g2g4'], depth: 20 }],
      authorQuestions: [{ id: 'question-1', kind: 'plan_gap', priority: 1, question: 'Why?', confidence: 'medium' }],
      authorEvidence: [{ questionId: 'question-1', questionKind: 'plan_gap', question: 'Why?', status: 'pending', branchCount: 0, pendingProbeCount: 1 }],
    } as any);

    assert.equal(decoded.probeRequests.length, 0);
    assert.equal(decoded.authorQuestions.length, 0);
    assert.equal(decoded.authorEvidence.length, 0);
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

  test('decodeMoveReviewResponse ignores top-level explanation fact fragments', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewExplanation: {
        title: 'Move review title',
        prose: 'Short explanation.',
        reasonTags: [],
        source: 'basic_move_explanation',
        factFragments: [
          {
            type: 'strategic_support',
            proofFamily: 'raw_proof_family',
            proofSource: 'raw_proof_source',
          },
        ],
      },
    });

    assert.equal((decoded.moveReviewExplanation as any)?.factFragments, undefined);
  });

  test('decodeMoveReviewResponse drops malformed top-level ledger lines only', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewLedger: {
        schema: 'chesstory.move_review.ledger.v1',
        motifKey: 'piece_route',
        motifLabel: 'Piece route',
        stageKey: 'build',
        stageLabel: 'Build',
        carryOver: false,
        prerequisites: [],
        primaryLine: {
          title: 'Raw request line',
          sanMoves: ['Nf3'],
          source: 'probe_request',
          note: 'raw request purpose',
        },
        resourceLine: {
          title: 'Probe line',
          sanMoves: ['Nf3', 'Nc6'],
          source: 'probe',
          note: '12cp vs baseline',
        },
      },
    });

    assert.equal(decoded.moveReviewLedger?.primaryLine, null);
    assert.equal(decoded.moveReviewLedger?.resourceLine?.source, 'probe');
    assert.deepEqual(decoded.moveReviewLedger?.resourceLine?.sanMoves, ['Nf3', 'Nc6']);
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

  test('decodeMoveReviewResponse preserves decision target comparison metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
        decisionComparison: {
          kicker: 'Decision point',
          secondaryText: 'The branches leave different targets.',
          chosenMatchesBest: false,
          targetComparison: {
            chosenTarget: 'e5',
            chosenTargetKind: 'isolated_pawn',
            bestTarget: 'd5',
            bestTargetKind: 'backward_pawn',
          },
        },
      },
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.decisionComparison?.targetComparison, {
      chosenTarget: 'e5',
      chosenTargetKind: 'isolated_pawn',
      bestTarget: 'd5',
      bestTargetKind: 'backward_pawn',
    });
  });

  test('decodeMoveReviewResponse drops malformed decision target comparison metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
        decisionComparison: {
          kicker: 'Decision point',
          chosenMatchesBest: false,
          targetComparison: {
            chosenTarget: 'h9',
            chosenTargetKind: 'isolated_pawn',
            bestTarget: 'd5',
            bestTargetKind: 'backward_pawn',
          },
        },
      },
    });

    assert.equal(decoded.moveReviewPlayerSurface?.decisionComparison?.targetComparison, null);
  });

  test('decodeMoveReviewResponse preserves valid surface authority target metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [
          {
            label: 'Opening',
            text: 'The opening structure points at d5.',
            authority: {
              kind: 'opening_family',
              openingFamily: 'queens_gambit',
              target: 'd5',
            },
          },
        ],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
      },
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, {
      kind: 'opening_family',
      token: null,
      openingFamily: 'queens_gambit',
      target: 'd5',
      openingBook: null,
    });
  });

  test('decodeMoveReviewResponse preserves bounded opening book metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [
          {
            label: 'Opening',
            text: 'The opening structure is backed by public aggregate book data.',
            authority: {
              kind: 'opening_family',
              openingFamily: 'queens_gambit',
              openingBook: {
                eco: 'D06',
                totalGames: 12345.9,
                topMoves: ['e6', 'raw note', 'Nf6', 'O-O'],
              },
            },
          },
          {
            label: 'Counterplay break',
            text: 'Opening metadata is not accepted on other authority shapes.',
            authority: {
              kind: 'counterplay_break',
              token: 'd5',
              openingBook: {
                eco: 'D06',
                totalGames: 12345,
                topMoves: ['e6'],
              },
            },
          },
        ],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
      },
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority?.openingBook, {
      eco: 'D06',
      totalGames: 12345,
      topMoves: ['e6', 'Nf6', 'O-O'],
    });
    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[1]?.authority?.openingBook, null);
  });

  test('decodeMoveReviewResponse keeps surface row while downgrading malformed authority', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [
          {
            label: 'Opening',
            text: 'The text should remain visible.',
            authority: {
              kind: 'opening_family',
              openingFamily: 'Queen/Gambit',
              target: 'h9',
            },
          },
        ],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
      },
    });

    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.text, 'The text should remain visible.');
    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, null);
  });

  test('decodeMoveReviewResponse downgrades unsupported or malformed surface authority shapes', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [
          {
            label: 'Central break',
            text: 'This stale cached row has a malformed central-break token.',
            authority: {
              kind: 'central_break',
              token: 'd5',
            },
          },
          {
            label: 'Counterplay break',
            text: 'This square counterplay token is still a supported public shape.',
            authority: {
              kind: 'counterplay_break',
              token: 'd5',
            },
          },
          {
            label: 'Unsupported',
            text: 'This stale cached row uses an unknown authority kind.',
            authority: {
              kind: 'raw_proof_family',
              token: 'd4-d5',
            },
          },
        ],
        advancedRows: [],
        probeRows: [],
        authorRows: [],
      },
    });

    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, null);
    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[1]?.authority, {
      kind: 'counterplay_break',
      token: 'd5',
      openingFamily: null,
      target: null,
      openingBook: null,
    });
    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[2]?.authority, null);
  });

  test('decodeMoveReviewResponse preserves bounded strategic relation authority', () => {
    const relationTokens = backendCatalogRelationTokens();
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [
          {
            label: 'Line relation',
            text: 'A stale summary relation row should not carry relation authority.',
            authority: {
              kind: 'strategic_relation',
              token: relationTokens[0],
              target: 'g6',
            },
          },
        ],
        advancedRows: [
          ...relationTokens.map(token => ({
            label: 'Line relation',
            text: `The checked line gives ${token} evidence around e4, f5, g6.`,
            authority: {
              kind: 'strategic_relation',
              token,
              target: 'g6',
            },
          })),
          {
            label: 'Line relation',
            text: 'Malformed relation source ids are not public authority.',
            authority: {
              kind: 'strategic_relation',
              token: 'source:xray_relation',
              target: 'g6',
            },
          },
          {
            label: 'Line relation',
            text: 'Untargeted relation tokens are not public authority.',
            authority: {
              kind: 'strategic_relation',
              token: relationTokens[0],
            },
          },
          {
            label: 'Line relation',
            text: 'Uncataloged relation ids are not public authority.',
            authority: {
              kind: 'strategic_relation',
              token: 'unsupported_relation',
              target: 'g6',
            },
          },
        ],
        probeRows: [],
        authorRows: [],
      },
    });

    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, null);
    assert.deepEqual(
      decoded.moveReviewPlayerSurface?.advancedRows.slice(0, relationTokens.length).map(row => row.authority?.token),
      relationTokens,
    );
    assert.deepEqual(decoded.moveReviewPlayerSurface?.advancedRows[0]?.authority, {
      kind: 'strategic_relation',
      token: 'defender_trade',
      openingFamily: null,
      target: 'g6',
      openingBook: null,
    });
    assert.equal(decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length]?.authority, null);
    assert.equal(decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length + 1]?.authority, null);
    assert.equal(decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length + 2]?.authority, null);
  });

  test('frontend strategic relation authority tokens stay aligned with backend relation catalog', () => {
    assert.deepEqual(frontendRelationTokens(), backendCatalogRelationTokens());
  });

  test('frontend strategic relation authority tokens exclude backend deferred relation inventory', () => {
    const deferredTokens = backendDeferredRelationTokens();
    const frontendTokens = new Set(frontendRelationTokens());
    assert.ok(deferredTokens.length > 0);
    assert.deepEqual(
      deferredTokens.filter(token => frontendTokens.has(token)),
      [],
    );

    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: {
        schema: 'chesstory.move_review.player_surface.v2',
        summaryRows: [],
        advancedRows: deferredTokens.map(token => ({
          label: 'Line relation',
          text: `Deferred relation ${token} is not public authority.`,
          authority: {
            kind: 'strategic_relation',
            token,
            target: 'g6',
          },
        })),
        probeRows: [],
        authorRows: [],
      },
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.advancedRows.map(row => row.authority), deferredTokens.map(() => null));
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
