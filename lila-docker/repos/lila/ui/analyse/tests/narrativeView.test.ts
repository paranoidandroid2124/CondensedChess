import { after, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import type { VNode } from 'snabbdom';

type NarrativeViewModule = typeof import('../src/narrative/narrativeView');

let narrativeViewModulePromise: Promise<NarrativeViewModule> | null = null;

async function loadNarrativeViewModule(): Promise<NarrativeViewModule> {
  if (!narrativeViewModulePromise) {
    const dom = new JSDOM('', { url: 'https://example.test/analyse' });
    const requestAnimationFrame =
      dom.window.requestAnimationFrame ||
      ((callback: FrameRequestCallback) => setTimeout(() => callback(Date.now()), 0));
    const cancelAnimationFrame =
      dom.window.cancelAnimationFrame ||
      ((handle: number) => clearTimeout(handle));
    Object.defineProperty(dom.window, 'requestAnimationFrame', {
      value: requestAnimationFrame.bind(dom.window),
      configurable: true,
      writable: true,
    });
    Object.defineProperty(dom.window, 'cancelAnimationFrame', {
      value: cancelAnimationFrame.bind(dom.window),
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'window', {
      value: dom.window,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'document', {
      value: dom.window.document,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'navigator', {
      value: dom.window.navigator,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'HTMLElement', {
      value: dom.window.HTMLElement,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'Node', {
      value: dom.window.Node,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'localStorage', {
      value: dom.window.localStorage,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'requestAnimationFrame', {
      value: dom.window.requestAnimationFrame.bind(dom.window),
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, 'cancelAnimationFrame', {
      value: dom.window.cancelAnimationFrame.bind(dom.window),
      configurable: true,
      writable: true,
    });
    narrativeViewModulePromise = import('../src/narrative/narrativeView');
  }
  return narrativeViewModulePromise;
}

after(() => {
  delete (globalThis as typeof globalThis & { window?: Window }).window;
  delete (globalThis as typeof globalThis & { document?: Document }).document;
  delete (globalThis as typeof globalThis & { navigator?: Navigator }).navigator;
  delete (globalThis as typeof globalThis & { HTMLElement?: typeof HTMLElement }).HTMLElement;
  delete (globalThis as typeof globalThis & { Node?: typeof Node }).Node;
  delete (globalThis as typeof globalThis & { localStorage?: Storage }).localStorage;
  delete (globalThis as typeof globalThis & { requestAnimationFrame?: typeof requestAnimationFrame }).requestAnimationFrame;
  delete (globalThis as typeof globalThis & { cancelAnimationFrame?: typeof cancelAnimationFrame }).cancelAnimationFrame;
});

function collectText(node: VNode | string | undefined): string {
  if (!node) return '';
  if (typeof node === 'string') return node;
  const own = typeof node.text === 'string' ? node.text : '';
  const children = Array.isArray(node.children) ? node.children.map(child => collectText(child as VNode | string)).join(' ') : '';
  return `${own} ${children}`.trim();
}

function collectSelectors(node: VNode | string | undefined, acc: string[] = []): string[] {
  if (!node || typeof node === 'string') return acc;
  if (typeof node.sel === 'string') acc.push(node.sel);
  if (Array.isArray(node.children)) node.children.forEach(child => collectSelectors(child as VNode | string, acc));
  return acc;
}

function collectRoutePayloads(node: VNode | string | undefined, acc: string[] = []): string[] {
  if (!node || typeof node === 'string') return acc;
  const attrs = (node.data as any)?.attrs;
  if (attrs?.['data-route']) acc.push(attrs['data-route']);
  if (Array.isArray(node.children)) node.children.forEach(child => collectRoutePayloads(child as VNode | string, acc));
  return acc;
}

describe('narrative review card click guard', () => {
  test('ignores interactive descendants inside a review card', async () => {
    const { shouldIgnoreReviewCardClick } = await loadNarrativeViewModule();
    const dom = new JSDOM(`
      <section class="narrative-moment">
        <button class="patch-replay-open-btn">Replay</button>
        <span data-board="fen|e2e4">e4</span>
        <span data-route="c1-g5" data-route-fen="fen">Bcg5</span>
      </section>
    `);
    const doc = dom.window.document;

    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('button')), true);
    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('[data-board]')), true);
    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('[data-route]')), true);
  });

  test('allows plain card content to select the moment', async () => {
    const { shouldIgnoreReviewCardClick } = await loadNarrativeViewModule();
    const dom = new JSDOM(`
      <section class="narrative-moment">
        <div class="narrative-body">
          <span class="plain-copy">Critical turning point</span>
        </div>
      </section>
    `);
    const doc = dom.window.document;

    assert.equal(shouldIgnoreReviewCardClick(doc.querySelector('.plain-copy')), false);
  });

  test('renders campaign thread summary and theme/stage badges for active notes', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 21,
      moveNumber: 11,
      side: 'white',
      momentType: 'SustainedPressure',
      fen: 'r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11',
      narrative: 'White keeps the same plan running.',
      selectionKind: 'thread_bridge',
      selectionLabel: 'Campaign Bridge',
      selectionReason: 'fills switch stage of Whole-Board Play',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White now treats this move as the switch into whole-board pressure.',
      activeBranchDossier: {
        dominantLens: 'structure',
        chosenBranchLabel: 'Whole-Board Play -> switch pressure',
        threadLabel: 'Whole-Board Play',
        threadStage: 'Switch',
        threadSummary: 'White fixes one sector before switching pressure across the board.',
      },
      strategicThread: {
        threadId: 'thread_1',
        themeKey: 'whole_board_play',
        themeLabel: 'Whole-Board Play',
        stageKey: 'switch',
        stageLabel: 'Switch',
      },
      activeStrategicMoves: [],
      activeStrategicRoutes: [],
    };

    const vnode = narrativeMomentView(ctrl, moment, {
      threadSummary: {
        threadId: 'thread_1',
        side: 'white',
        themeKey: 'whole_board_play',
        themeLabel: 'Whole-Board Play',
        summary: 'White fixes one sector before switching pressure across the board.',
        seedPly: 11,
        lastPly: 21,
        representativePlies: [11, 21],
        continuityScore: 0.81,
      },
    });

    const selectors = collectSelectors(vnode);
    const text = collectText(vnode);

    assert.ok(selectors.includes('div.narrative-thread-summary'));
    assert.ok(selectors.includes('span.narrative-badge.selection.campaign-bridge'));
    assert.ok(selectors.includes('span.narrative-badge.theme.whole-board-play'));
    assert.ok(selectors.includes('span.narrative-badge.stage.switch'));
    assert.match(text, /Campaign Thread/);
    assert.match(text, /Campaign Bridge/);
    assert.match(text, /Whole-Board Play/);
    assert.match(text, /Switch/);
    assert.match(text, /fills switch stage of Whole-Board Play/);
  });

  test('fallback route chips hide opponent routes and suppress raw paths for toward-only surfaces', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 21,
      moveNumber: 11,
      side: 'white',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3N4/4K3 w - - 0 1',
      narrative: 'White keeps the same plan running.',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White keeps the knight headed toward e3.',
      activeStrategicMoves: [],
      activeStrategicRoutes: [
        {
          routeId: 'route_1',
          ownerSide: 'white',
          piece: 'N',
          route: ['d2', 'f1', 'e3'],
          purpose: 'kingside clamp',
          strategicFit: 0.84,
          tacticalSafety: 0.70,
          surfaceConfidence: 0.70,
          surfaceMode: 'toward',
        },
        {
          routeId: 'route_2',
          ownerSide: 'black',
          piece: 'Q',
          route: ['d8', 'd4'],
          purpose: 'central pressure',
          strategicFit: 0.86,
          tacticalSafety: 0.86,
          surfaceConfidence: 0.86,
          surfaceMode: 'exact',
        },
      ],
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const text = collectText(vnode);
    const routePayloads = collectRoutePayloads(vnode);

    assert.match(text, /N toward e3/);
    assert.doesNotMatch(text, /Black Q/);
    assert.deepEqual(routePayloads, []);
  });

  test('active note shows Idea, Execution, and Objective as distinct surfaces', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 21,
      moveNumber: 11,
      side: 'white',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3N4/4K3 w - - 0 1',
      narrative: 'White keeps the same plan running.',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White keeps building the same idea.',
      activeStrategicIdeas: [
        {
          ideaId: 'idea_1',
          ownerSide: 'white',
          kind: 'space_gain_or_restriction',
          group: 'structural_change',
          readiness: 'build',
          focusSummary: 'e3, g4',
          confidence: 0.88,
        },
      ],
      activeStrategicRoutes: [
        {
          routeId: 'route_1',
          ownerSide: 'white',
          piece: 'N',
          route: ['d2', 'f1', 'e3'],
          purpose: 'kingside clamp',
          strategicFit: 0.84,
          tacticalSafety: 0.70,
          surfaceConfidence: 0.70,
          surfaceMode: 'toward',
        },
      ],
      activeDirectionalTargets: [
        {
          targetId: 'target_1',
          ownerSide: 'white',
          piece: 'N',
          from: 'd2',
          targetSquare: 'g4',
          readiness: 'build',
          strategicReasons: ['supports kingside expansion'],
          prerequisites: ['prepare the supporting squares first'],
        },
      ],
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const selectors = collectSelectors(vnode);
    const text = collectText(vnode);
    const routePayloads = collectRoutePayloads(vnode);

    assert.ok(selectors.includes('div.narrative-strategic-surface.narrative-strategic-surface--idea'));
    assert.ok(selectors.includes('div.narrative-strategic-surface.narrative-strategic-surface--execution'));
    assert.ok(selectors.includes('div.narrative-strategic-surface.narrative-strategic-surface--objective'));
    assert.match(text, /Idea/);
    assert.match(text, /Execution/);
    assert.match(text, /Objective/);
    assert.match(text, /Dominant: Space Gain Or Restriction · e3, g4 · Build/);
    assert.match(text, /N toward e3/);
    assert.match(text, /work toward making g4 available · Build/);
    assert.deepEqual(routePayloads, []);
  });

  test('exact execution route keeps raw path preview while objective stays out of route chips', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'white',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3R4/4K3 w - - 0 1',
      narrative: 'White keeps the same plan running.',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White switches onto the file.',
      activeStrategicIdeas: [
        {
          ideaId: 'idea_1',
          ownerSide: 'white',
          kind: 'line_occupation',
          group: 'piece_and_line_management',
          readiness: 'ready',
          focusSummary: 'b-file',
          confidence: 0.84,
        },
      ],
      activeStrategicRoutes: [
        {
          routeId: 'route_1',
          ownerSide: 'white',
          piece: 'R',
          route: ['a1', 'b1', 'b3'],
          purpose: 'open-file occupation',
          strategicFit: 0.86,
          tacticalSafety: 0.86,
          surfaceConfidence: 0.86,
          surfaceMode: 'exact',
        },
      ],
      activeDirectionalTargets: [
        {
          targetId: 'target_2',
          ownerSide: 'white',
          piece: 'R',
          from: 'a1',
          targetSquare: 'b5',
          readiness: 'contested',
        },
      ],
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const routePayloads = collectRoutePayloads(vnode);
    const text = collectText(vnode);

    assert.deepEqual(routePayloads, ['a1-b1-b3']);
    assert.match(text, /work toward making b5 available · Contested/);
  });

  test('branch dossier keeps owner label and hides raw path for toward-only cues', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'white',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3N4/4K3 w - - 0 1',
      narrative: 'White keeps the same plan running.',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White has to watch the black queen heading toward c5.',
      activeStrategicMoves: [],
      activeStrategicRoutes: [],
      activeBranchDossier: {
        dominantLens: 'structure',
        chosenBranchLabel: 'Semi-open center -> central pressure',
        routeCue: {
          routeId: 'route_2',
          ownerSide: 'black',
          piece: 'Q',
          route: ['d8', 'c5'],
          purpose: 'central pressure',
          strategicFit: 0.81,
          tacticalSafety: 0.64,
          surfaceConfidence: 0.64,
          surfaceMode: 'toward',
        },
      },
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const text = collectText(vnode);
    const routePayloads = collectRoutePayloads(vnode);

    assert.match(text, /Black Q toward c5/);
    assert.deepEqual(routePayloads, []);
  });

  test('signal box surfaces idea, campaign, execution, objective, and focus from strategyPack', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'black',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3Q4/4K3 b - - 0 1',
      narrative: 'White keeps the compensation rolling.',
      concepts: [],
      variations: [],
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
            confidence: 0.92,
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
            tacticalSafety: 0.74,
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
        compensation: 'initiative against the king',
        investedMaterial: 180,
        dominantIdeaKind: 'king_attack_build_up',
        dominantIdeaFocus: 'g7, h7',
      },
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const text = collectText(vnode);

    assert.match(text, /Dominant King Attack Build Up · g7, h7 · Build/);
    assert.match(text, /White campaign/);
    assert.match(text, /Q toward h5/);
    assert.match(text, /work toward making h7 available/);
    assert.match(text, /keep the initiative rather than recovering material/);
  });

  test('support box does not rebuild decision-comparison support from topEngineMove fallback alone', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'white',
      momentType: 'StrategicBridge',
      fen: '4k3/8/8/8/8/8/3Q4/4K3 w - - 0 1',
      narrative: 'Qe2 keeps the center stable.',
      concepts: [],
      variations: [],
      topEngineMove: {
        uci: 'd1e2',
        san: 'Qe2',
        cpLossVsPlayed: 32,
        pv: ['d1e2', 'e8d7', 'e2e4'],
      },
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const selectors = collectSelectors(vnode);
    const text = collectText(vnode);

    assert.doesNotMatch(text, /Support/);
    assert.doesNotMatch(text, /Engine/);
    assert.ok(!selectors.includes('div.narrative-signal-box'));
  });

  test('decision-comparison support renders exact comparative consequence from the canonical digest', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'white',
      momentType: 'StrategicBridge',
      fen: 'rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1',
      narrative: 'Nd2 fixes d6 as the next target.',
      concepts: [],
      variations: [],
      signalDigest: {
        decisionComparison: {
          chosenMove: 'Nd2',
          engineBestMove: 'Nd2',
          engineBestPv: ['Nd2', '...Na6', 'f3', 'Nc7'],
          comparedMove: 'Qc2',
          comparativeConsequence:
            'Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.',
          comparativeSource: 'exact_target_fixation_delta',
          chosenMatchesBest: true,
        },
      },
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const text = collectText(vnode);

    assert.match(text, /Decision compare/);
    assert.match(text, /Chosen/);
    assert.match(text, /Compared/);
    assert.match(text, /Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch/);
  });

  test('strategic note no longer rebuilds active strategy surfaces from strategyPack fallback', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'black',
      momentType: 'SustainedPressure',
      fen: '4k3/8/8/8/8/8/3Q4/4K3 b - - 0 1',
      narrative: 'White keeps the compensation rolling.',
      concepts: [],
      variations: [],
      activeStrategicNote: 'White should keep the initiative rather than rushing to recover the pawn.',
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
            confidence: 0.92,
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
            tacticalSafety: 0.74,
            surfaceConfidence: 0.84,
            surfaceMode: 'toward',
          },
        ],
        pieceMoveRefs: [],
        directionalTargets: [],
        longTermFocus: ['keep the initiative rather than recovering material'],
      },
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const text = collectText(vnode);

    assert.match(text, /Strategic Note/);
    assert.match(text, /White should keep the initiative rather than rushing to recover the pawn/);
    assert.doesNotMatch(text, /White Campaign/);
    assert.doesNotMatch(text, /Dominant: King Attack Build Up/);
    assert.doesNotMatch(text, /Q toward h5/);
    assert.doesNotMatch(text, /keep the initiative rather than recovering material/);
  });

  test('default Chronicle support stays compact and pushes analyst detail into collapsed advanced details', async () => {
    const { narrativeMomentView } = await loadNarrativeViewModule();
    const ctrl: any = {
      root: {
        data: {
          game: {
            variant: { key: 'standard' },
          },
        },
      },
    };

    const moment: any = {
      ply: 24,
      moveNumber: 12,
      side: 'white',
      momentType: 'StrategicBridge',
      fen: '4k3/8/8/8/8/8/3Q4/4K3 w - - 0 1',
      narrative: 'Qe2 keeps the e4 push available while covering the c4 pawn.',
      concepts: [],
      variations: [],
      mainStrategicPlans: [
        {
          planId: 'kingside_attack',
          planName: 'Kingside attack',
          rank: 1,
          score: 0.82,
        },
      ],
      strategicPlanExperiments: [
        {
          planId: 'kingside_attack',
          supportStatus: 'evidence_backed',
          evidenceTier: 'evidence_backed',
          moveOrderSensitive: false,
          supportProbeCount: 2,
          opposingProbeCount: 0,
          contradictoryProbeCount: 0,
        },
      ],
      signalDigest: {
        opening: 'Queen’s Pawn Game',
        opponentPlan: 'break with ...c5',
        structuralCue: 'central tension still matters',
        structureProfile: 'semi-open center',
        practicalVerdict: 'The move keeps the position flexible.',
        practicalFactors: ['holds e4 in reserve'],
      },
      activeStrategicNote: 'White keeps a kingside build in reserve if the center stays stable.',
      authorQuestions: [
        {
          id: 'q1',
          kind: 'latent_plan',
          priority: 1,
          question: 'Which attacking setup remains in reserve?',
          confidence: 'heuristic',
        },
      ],
    };

    const vnode = narrativeMomentView(ctrl, moment);
    const selectors = collectSelectors(vnode);
    const text = collectText(vnode);

    assert.ok(selectors.includes('div.narrative-signal-box'));
    assert.ok(selectors.includes('details.narrative-advanced-details'));
    assert.match(text, /Support/);
    assert.match(text, /Main plans/);
    assert.match(text, /Advanced details/);
    assert.match(text, /Strategic Note/);
    assert.match(text, /Authoring Evidence/);
  });
});
