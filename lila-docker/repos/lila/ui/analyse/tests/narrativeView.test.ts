import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { narrativeMomentView, shouldIgnoreReviewCardClick } from '../src/narrative/narrativeView';
import type { VNode } from 'snabbdom';

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
  test('ignores interactive descendants inside a review card', () => {
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

  test('allows plain card content to select the moment', () => {
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

  test('renders campaign thread summary and theme/stage badges for active notes', () => {
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

  test('fallback route chips hide opponent routes and suppress raw paths for toward-only surfaces', () => {
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

  test('active note shows Idea, Execution, and Objective as distinct surfaces', () => {
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
    assert.match(text, /Dominant: Space Gain OR Restriction · e3, g4 · Build/);
    assert.match(text, /N toward e3/);
    assert.match(text, /work toward making g4 available · Build/);
    assert.deepEqual(routePayloads, []);
  });

  test('exact execution route keeps raw path preview while objective stays out of route chips', () => {
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

  test('branch dossier keeps owner label and hides raw path for toward-only cues', () => {
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
});
