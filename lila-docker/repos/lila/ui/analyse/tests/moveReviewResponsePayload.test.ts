import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  decodeMoveReviewResponse,
  type MoveReviewPlayerSurfaceV1,
  moveReviewNeedsRetry,
} from '../src/moveReview/responsePayload';

const playerSurface = (overrides: Partial<MoveReviewPlayerSurfaceV1> = {}): MoveReviewPlayerSurfaceV1 => ({
  schema: 'chesstory.move_review.player_surface.v2',
  summaryRows: [],
  advancedRows: [],
  probeRows: [],
  authorRows: [],
  ...overrides,
});

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
      moveReviewPlayerSurface: playerSurface({
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
      }),
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
      moveReviewPlayerSurface: playerSurface({
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
      }),
    });

    assert.equal(decoded.moveReviewPlayerSurface?.decisionComparison?.targetComparison, null);
  });

  test('decodeMoveReviewResponse preserves valid surface authority metadata without opening targets', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
        schema: 'chesstory . move_review . player_surface . v2' as MoveReviewPlayerSurfaceV1['schema'],
        summaryRows: [
          {
            label: 'Opening',
            text: 'The opening structure supplies family context.',
            authority: {
              kind: 'opening_family',
              openingFamily: 'queens_gambit',
              target: 'd5',
            },
          },
        ],
      }),
    });

    assert.equal(decoded.moveReviewPlayerSurface?.schema, 'chesstory.move_review.player_surface.v2');
    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, {
      kind: 'opening_family',
      token: null,
      openingFamily: 'queens_gambit',
      target: null,
      openingBook: null,
    });
  });

  test('decodeMoveReviewResponse treats opening-family targets as context-only metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
        summaryRows: [
          {
            label: 'Opening',
            text: 'The opening family remains visible without a public target chip.',
            authority: {
              kind: 'opening_family',
              openingFamily: 'queens_gambit',
              target: 'd5',
            },
          },
        ],
      }),
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, {
      kind: 'opening_family',
      token: null,
      openingFamily: 'queens_gambit',
      target: null,
      openingBook: null,
    });
  });

  test('decodeMoveReviewResponse preserves typed practical-plan targets without frontend prose parsing', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
        summaryRows: [
          {
            label: 'Fixed target',
            text: 'The checked line keeps d6 fixed as the target.',
            authority: {
              kind: 'practical_plan',
              target: 'd6',
            },
          },
          {
            label: 'Minority attack',
            text: 'The checked line keeps c6 as the minority-attack fixed target.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'IQP target',
            text: 'The checked line leaves d5 as an isolated pawn target.',
            authority: {
              kind: 'practical_plan',
              target: 'd5',
            },
          },
          {
            label: 'Simplification',
            text: 'The checked line keeps the same local edge after the exchange on e6.',
            authority: {
              kind: 'practical_plan',
              target: 'e6',
            },
          },
          {
            label: 'Knight outpost',
            text: 'The checked line puts the knight on the e5 outpost.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'File entry',
            text: 'The checked line keeps pressure on c6 through the c-file.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Target coordination',
            text: 'The checked line coordinates pressure on c6 from c1 and e3.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Color complex',
            text: 'The checked line keeps the knight on c4 attacking e5 in the dark-square complex.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'Color complex',
            text: 'The checked line keeps the knight on c4 attacking e5 in the red-square complex.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'Color complex',
            text: 'The checked line keeps the queen on c4 attacking e5 in the dark-square complex.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'Color complex',
            text: 'The checked line keeps the bishop on c4 attacking e5 in the dark-square complex.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'Simplification window',
            text: 'Approximate labels are backend responsibility, not a frontend parser gate.',
            authority: {
              kind: 'practical_plan',
              target: 'e6',
            },
          },
          {
            label: 'Knight outpost plan',
            text: 'Approximate outpost labels are backend responsibility, not a frontend parser gate.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'File entry plan',
            text: 'Approximate file-entry labels are backend responsibility, not a frontend parser gate.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Target coordination plan',
            text: 'Approximate coordination labels are backend responsibility, not a frontend parser gate.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Color complex plan',
            text: 'Approximate color-complex labels are backend responsibility, not a frontend parser gate.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
          {
            label: 'Practical plan',
            text: 'Generic practical-plan rows carry only typed payload metadata.',
            authority: {
              kind: 'practical_plan',
              target: 'd5',
            },
          },
        ],
      }),
    });

    assert.deepEqual(
      decoded.moveReviewPlayerSurface?.summaryRows.map(row => row.authority?.target),
      [
        'd6',
        'c6',
        'd5',
        'e6',
        'e5',
        'c6',
        'c6',
        'e5',
        'e5',
        'e5',
        'e5',
        'e6',
        'e5',
        'c6',
        'c6',
        'e5',
        'd5',
      ],
    );
  });

  test('decodeMoveReviewResponse does not use stale practical-plan prose as a frontend target gate', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
        summaryRows: [
          {
            label: 'File entry',
            text: 'The rook already has a practical c-file post.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Minority attack',
            text: "The Carlsbad-type pawn shape makes c6 a natural queenside target for White's minority-attack ideas.",
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'File entry',
            text: 'The checked line keeps pressure on c6 through the c-file.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'File entry',
            text: 'The checked line keeps pressure on e6 through the c-file.',
            authority: {
              kind: 'practical_plan',
              target: 'e6',
            },
          },
          {
            label: 'Target coordination',
            text: 'The checked line coordinates pressure on c6 from c1 and c1.',
            authority: {
              kind: 'practical_plan',
              target: 'c6',
            },
          },
          {
            label: 'Knight outpost',
            text: 'The checked line puts the queen on the e5 outpost.',
            authority: {
              kind: 'practical_plan',
              target: 'e5',
            },
          },
        ],
      }),
    });

    assert.deepEqual(
      decoded.moveReviewPlayerSurface?.summaryRows.map(row => row.authority?.target),
      ['c6', 'c6', 'c6', 'e6', 'c6', 'e5'],
    );
  });

  test('decodeMoveReviewResponse preserves bounded opening book metadata', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
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
      }),
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
      moveReviewPlayerSurface: playerSurface({
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
      }),
    });

    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.text, 'The text should remain visible.');
    assert.equal(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, null);
  });

  test('decodeMoveReviewResponse downgrades unsupported or malformed surface authority shapes', () => {
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
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
      }),
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

  test('decodeMoveReviewResponse preserves strategic relation authority by typed shape', () => {
    const relationTokens = ['defender_trade', 'unsupported_relation'];
    const decoded = decodeMoveReviewResponse({
      moveReviewPlayerSurface: playerSurface({
        summaryRows: [
          {
            label: 'Line relation',
            text: 'Summary placement is producer-owned, not a frontend label gate.',
            authority: {
              kind: 'strategic_relation',
              token: relationTokens[0],
              target: 'g6',
            },
          },
          {
            label: 'Defender trade',
            text: 'The checked line trades on d4 to remove the defender from c5, loosening e5.',
            authority: {
              kind: 'strategic_relation',
              token: 'defender_trade',
              target: 'e5',
            },
          },
        ],
        advancedRows: [
          ...relationTokens.map(token => ({
            label: 'Line relation',
            text: `The checked line carries typed relation metadata around e4, f5, g6.`,
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
            text: 'Catalog membership is enforced by the backend sanitizer, not by a frontend mirror.',
            authority: {
              kind: 'strategic_relation',
              token: 'unsupported_relation',
              target: 'g6',
            },
          },
        ],
      }),
    });

    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[0]?.authority, {
      kind: 'strategic_relation',
      token: 'defender_trade',
      openingFamily: null,
      target: 'g6',
      openingBook: null,
    });
    assert.deepEqual(decoded.moveReviewPlayerSurface?.summaryRows[1]?.authority, {
      kind: 'strategic_relation',
      token: 'defender_trade',
      openingFamily: null,
      target: 'e5',
      openingBook: null,
    });
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
    assert.deepEqual(decoded.moveReviewPlayerSurface?.advancedRows[1]?.authority, {
      kind: 'strategic_relation',
      token: 'unsupported_relation',
      openingFamily: null,
      target: 'g6',
      openingBook: null,
    });
    assert.equal(decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length]?.authority, null);
    assert.equal(decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length + 1]?.authority, null);
    assert.equal(
      decoded.moveReviewPlayerSurface?.advancedRows[relationTokens.length + 2]?.authority?.token,
      'unsupported_relation',
    );
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
