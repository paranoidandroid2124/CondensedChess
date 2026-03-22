import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import type { DefeatDnaReport, GameChronicleResponse } from '../src/narrative/narrativeCtrl';

const dom = new JSDOM('<!doctype html><html><body data-theme="dark"></body></html>');
const mediaQuery = {
  matches: false,
  media: '',
  onchange: null,
  addListener() {},
  removeListener() {},
  addEventListener() {},
  removeEventListener() {},
  dispatchEvent() {
    return false;
  },
};

Object.defineProperty(globalThis, 'window', { value: dom.window, configurable: true });
Object.defineProperty(globalThis, 'document', { value: dom.window.document, configurable: true });
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true });
Object.defineProperty(globalThis, 'HTMLElement', { value: dom.window.HTMLElement, configurable: true });
Object.defineProperty(globalThis, 'Node', { value: dom.window.Node, configurable: true });
Object.defineProperty(globalThis, 'Event', { value: dom.window.Event, configurable: true });
Object.defineProperty(globalThis, '$html', {
  value: (strings: TemplateStringsArray, ...values: unknown[]) =>
    strings.reduce((acc, part, index) => `${acc}${part}${index < values.length ? String(values[index]) : ''}`, ''),
  configurable: true,
});
Object.defineProperty(globalThis, 'requestAnimationFrame', {
  value: (cb: FrameRequestCallback) => setTimeout(() => cb(0), 0),
  configurable: true,
});
Object.defineProperty(globalThis, 'cancelAnimationFrame', {
  value: (id: number) => clearTimeout(id),
  configurable: true,
});
Object.defineProperty(dom.window, 'requestAnimationFrame', {
  value: (cb: FrameRequestCallback) => setTimeout(() => cb(0), 0),
  configurable: true,
});
Object.defineProperty(dom.window, 'cancelAnimationFrame', {
  value: (id: number) => clearTimeout(id),
  configurable: true,
});
Object.defineProperty(window, 'matchMedia', {
  value: () => mediaQuery,
  configurable: true,
});
Object.defineProperty(window, 'getComputedStyle', {
  value: () => ({ getPropertyValue: () => '2' }),
  configurable: true,
});

const { reviewView } = await import('../src/review/view');

type ReviewViewNodes = {
  moveListNode: any;
  explorerNode?: any;
  boardSettingsNodes: any[];
  importNode?: any;
};

const textNode = (text: string) => ({ sel: 'div', data: {}, children: [], text }) as any;

const reviewNodes: ReviewViewNodes = {
  moveListNode: textNode('move list'),
  explorerNode: textNode('explorer'),
  boardSettingsNodes: [textNode('board settings')],
  importNode: textNode('import'),
};

describe('review shell', () => {
  test('overview shows deep-analysis CTA before narrative data exists', () => {
    const vnode = reviewView(makeCtrl({ primaryTab: 'overview' }), reviewNodes);
    const text = collectText(vnode);

    assert.match(text, /Run Game Chronicle/);
    assert.match(text, /Post-game review/);
  });

  test('overview shows narrative summary and next actions after analysis', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'overview',
        narrativeData: sampleNarrative({
          intro: 'White stabilized the center and then took over the c-file.',
          themes: ['Center Control', 'File Pressure'],
          review: {
            totalPlies: 42,
            evalCoveredPlies: 36,
            evalCoveragePct: 85.7,
            selectedMoments: 3,
            selectedMomentPlies: [12, 24, 33],
            blundersCount: 1,
            missedWinsCount: 0,
          },
          moments: [sampleMoment({ ply: 24, narrative: 'Critical sequence', collapse: sampleCollapse('22-27') })],
        }),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Narrative-first game review/);
    assert.match(text, /White stabilized the center/);
    assert.match(text, /Go to Moments/);
    assert.match(text, /Open Repair/);
    assert.match(text, /See Patterns/);
  });

  test('overview disables repair action when no collapse is available', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'overview',
        narrativeData: sampleNarrative({
          themes: ['Space Advantage'],
          moments: [sampleMoment({ ply: 14, narrative: 'Quiet squeeze', momentType: 'Plan' })],
        }),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Repair unavailable/);
    assert.match(text, /This game has no causal collapse window yet/);
  });

  test('moments filters the list for collapse-only review', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'moments',
        momentFilter: 'collapses',
        narrativeData: sampleNarrative({
          moments: [
            sampleMoment({ ply: 14, narrative: 'Quiet improvement', momentType: 'Plan', moveClassification: 'Best' }),
            sampleMoment({
              ply: 28,
              narrative: 'Collapse marker',
              momentType: 'Critical',
              collapse: sampleCollapse('28-31'),
            }),
          ],
        }),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Collapse\s+marker/);
    assert.doesNotMatch(text, /Quiet improvement/);
  });

  test('moments renders badges and nested collapse cards', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'moments',
        narrativeData: sampleNarrative({
          moments: [
            sampleMoment({
              ply: 22,
              narrative: 'White misses the forcing continuation.',
              moveClassification: 'Brilliant',
              momentType: 'Critical',
              strategicSalience: 'High',
              collapse: sampleCollapse('22-27'),
            }),
          ],
        }),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Brilliant/);
    assert.match(text, /High/);
    assert.match(text, /Causal Collapse Analyzer/);
    assert.match(text, /Tactical Oversight/);
  });

  test('repair shows empty state when no collapse exists', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'repair',
        narrativeData: sampleNarrative({
          moments: [sampleMoment({ ply: 18, narrative: 'Quiet edge', momentType: 'Plan' })],
        }),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /No causal collapse detected/);
    assert.match(text, /Go to Moments/);
  });

  test('patterns shows placeholder when dna is unavailable', () => {
    const vnode = reviewView(makeCtrl({ primaryTab: 'patterns' }), reviewNodes);
    const text = collectText(vnode);

    assert.match(text, /Patterns unlock after more analysis/);
    assert.match(text, /Defeat DNA becomes active/);
  });

  test('patterns renders the dna dashboard when data exists', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'patterns',
        dnaData: sampleDnaReport(),
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Account-level profile built from 4 analyzed games/);
    assert.match(text, /Root Cause Distribution/);
    assert.match(text, /Recent Collapses/);
    assert.match(text, /Games Analyzed/);
  });

  test('utility panel renders explorer without replacing the current primary tab', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'moves',
        utilityPanel: 'explorer',
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Raw analysis/);
    assert.match(text, /Opening Explorer/);
    assert.match(text, /Close panel/);
    assert.match(text, /explorer/);
  });

  test('import is promoted to a primary review tab', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'import',
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Import PGN/);
    assert.match(text, /Paste a PGN or jump by FEN without leaving this analysis shell/);
    assert.match(text, /Stay in analysis/);
    assert.match(text, /Recent Games page/);
  });

  test('utility panel renders board settings as a secondary tool surface', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'overview',
        utilityPanel: 'board',
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Board View/);
    assert.match(text, /Close panel/);
    assert.match(text, /board settings/);
    assert.match(text, /Run Game Chronicle/);
  });

  test('moves empty state exposes an immediate engine CTA', () => {
    const vnode = reviewView(
      makeCtrl({
        primaryTab: 'moves',
      }),
      reviewNodes,
    );
    const text = collectText(vnode);

    assert.match(text, /Local engine is off/);
    assert.match(text, /Turn On Engine/);
    assert.doesNotMatch(text, /Enable the engine panel from Board View/);
  });
});

function makeCtrl({
  primaryTab = 'overview',
  utilityPanel = null,
  momentFilter = 'all',
  selectedMomentPly = null,
  selectedCollapseId = null,
  narrativeData = null,
  dnaData = null,
  engineVisible = false,
}: {
  primaryTab?: 'overview' | 'moments' | 'repair' | 'patterns' | 'moves' | 'import';
  utilityPanel?: 'explorer' | 'board' | null;
  momentFilter?: 'all' | 'critical' | 'collapses';
  selectedMomentPly?: number | null;
  selectedCollapseId?: string | null;
  narrativeData?: GameChronicleResponse | null;
  dnaData?: DefeatDnaReport | null;
  engineVisible?: boolean;
}) {
  const root = {
    jumpToMain() {},
    redraw() {},
    mainline: [],
    data: {
      game: {
        variant: { key: 'standard', name: 'Standard' },
        opening: { name: 'French Defense', eco: 'C00', ply: 4 },
      },
    },
  };

  const narrative = {
    root,
    data: () => narrativeData,
    loading: () => false,
    loadingDetail: () => null,
    error: () => null,
    needsLogin: () => false,
    loginHref: () => '/login',
    dnaLoading: () => false,
    dnaError: () => null,
    dnaData: () => dnaData,
    pvBoard: () => null,
    patchReplay: () => null,
    patchOpen() {},
    patchClose() {},
    patchStep() {},
    patchToggle() {},
    showAllCollapses: (() => {
      let expanded = false;
      return (next?: boolean) => {
        if (next === undefined) return expanded;
        expanded = next;
        return expanded;
      };
    })(),
  };

  return {
    explorer: {
      allowed: () => true,
      db: () => 'lichess',
      loading: () => false,
      current: () => ({
        isOpening: true,
        opening: { name: 'French Defense', eco: 'C00' },
        moves: [{ san: 'e4', uci: 'e2e4' }],
        fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2',
        white: 1,
        black: 1,
        draws: 0,
      }),
    },
    narrative,
    node: {
      ply: 7,
      fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2',
    },
    mainline: Array.from({ length: 12 }, (_, index) => ({ ply: index })),
    reviewPrimaryTab: () => primaryTab,
    reviewUtilityPanel: () => utilityPanel,
    reviewMomentFilter: () => momentFilter,
    selectedReviewMomentPly: () => selectedMomentPly,
    selectedReviewCollapseId: () => selectedCollapseId,
    cevalEnabled: () => engineVisible,
    showEnginePanel: () => engineVisible,
    setShowEnginePanel() {},
    isCevalAllowed: () => true,
    setReviewPrimaryTab() {},
    setReviewUtilityPanel() {},
    setReviewMomentFilter() {},
    selectReviewMoment() {},
    selectReviewCollapse() {},
    openNarrative: async () => {},
    jumpToMain() {},
  } as any;
}

function sampleNarrative({
  intro = 'Sample intro',
  moments = [sampleMoment({ ply: 12, narrative: 'Sample moment' })],
  conclusion = 'Sample conclusion',
  themes = ['Space'],
  review = {
    totalPlies: 24,
    evalCoveredPlies: 20,
    evalCoveragePct: 83.3,
    selectedMoments: 1,
    selectedMomentPlies: [12],
  },
}: Partial<GameChronicleResponse>): GameChronicleResponse {
  return {
    schema: 'v1',
    intro,
    moments,
    conclusion,
    themes,
    review,
  };
}

function sampleMoment(overrides: Record<string, unknown>) {
  return {
    ply: 12,
    momentType: 'Critical',
    fen: '8/8/8/8/8/8/8/8 w - - 0 1',
    narrative: 'Sample moment',
    concepts: [],
    variations: [],
    ...overrides,
  };
}

function sampleCollapse(interval: string) {
  return {
    interval,
    rootCause: 'Tactical Oversight',
    earliestPreventablePly: parseInt(interval.split('-')[0]!, 10),
    patchLineUci: [],
    recoverabilityPlies: 3,
  };
}

function sampleDnaReport(): DefeatDnaReport {
  return {
    userId: 'u1',
    totalGamesAnalyzed: 4,
    rootCauseDistribution: {
      'Tactical Miss': 2,
      'Plan Deviation': 1,
    },
    avgRecoverabilityPlies: 3.5,
    mostCommonPatchLines: ['e4 e5 Nf3'],
    recentCollapses: [
      {
        interval: '22-27',
        rootCause: 'Tactical Miss',
        earliestPreventablePly: 22,
        patchLineUci: [],
        recoverabilityPlies: 3,
      },
    ],
  };
}

function collectText(node: unknown): string {
  if (node == null || node === false) return '';
  if (Array.isArray(node)) return node.map(collectText).join(' ');
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (typeof node === 'object') {
    const vnode = node as { text?: string; children?: unknown[] };
    return [vnode.text || '', collectText(vnode.children || [])].filter(Boolean).join(' ');
  }
  return '';
}
