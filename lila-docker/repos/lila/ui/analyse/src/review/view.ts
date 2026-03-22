import * as licon from 'lib/licon';
import type { LooseVNode, LooseVNodes, VNode } from 'lib/view';
import { bind, hl, icon } from 'lib/view';
import { cleanNarrativeProseText, cleanNarrativeSurfaceLabel } from '../chesstory/signalFormatting';
import type AnalyseCtrl from '../ctrl';
import type { NarrativeMomentFilter, ReviewPrimaryTab } from './state';
import {
  MIN_GAME_CHRONICLE_PLY,
  moveNumberFromPly,
  totalMainlinePly,
  type GameChronicleMoment,
} from '../narrative/narrativeCtrl';
import {
  collapseTimelineView,
  bindPreviewHover,
  defeatDnaContentView,
  narrativeCollapseCardView,
  narrativeMomentView,
  narrativeReviewView,
} from '../narrative/narrativeView';

export type ReviewViewNodes = {
  cevalNode?: LooseVNodes;
  pvsNode?: LooseVNodes;
  moveListNode: VNode;
  forkNode?: LooseVNode;
  explorerNode?: LooseVNode;
  boardSettingsNodes: VNode[];
  importNode?: VNode;
};

type ReviewTabMeta = {
  tab: ReviewPrimaryTab;
  label: string;
};

type ReviewOverviewStats = {
  totalMoments: number;
  collapseMoments: number;
  selectedMoments: number;
  evalCoveragePct: number | null;
};

const primaryTabs: ReviewTabMeta[] = [
  { tab: 'overview', label: 'Overview' },
  { tab: 'moments', label: 'Moments' },
  { tab: 'repair', label: 'Repair' },
  { tab: 'patterns', label: 'Patterns' },
  { tab: 'moves', label: 'Moves' },
  { tab: 'import', label: 'Import PGN' },
];

const momentFilters: Array<{ filter: NarrativeMomentFilter; label: string }> = [
  { filter: 'all', label: 'All' },
  { filter: 'critical', label: 'Critical' },
  { filter: 'collapses', label: 'Collapses' },
];

export function reviewView(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const stats = reviewOverviewStats(ctrl);
  return hl('section.analyse-review', {
    hook: {
      insert: vnode => {
        if (ctrl.narrative) bindPreviewHover(ctrl.narrative, vnode.elm as HTMLElement);
      },
      postpatch: (_, vnode) => {
        if (ctrl.narrative) bindPreviewHover(ctrl.narrative, vnode.elm as HTMLElement);
      },
    },
  }, [
    hl('div.analyse-review__tabs-head', [
      hl(
        'div.analyse-review__tabs',
        {
          hook: ensureActiveChildVisible('[data-review-tab].active'),
          attrs: { role: 'tablist', 'aria-label': 'Review sections' },
        },
        primaryTabs.map(({ tab, label }) =>
          hl(
            `button.analyse-review__tab${ctrl.reviewPrimaryTab() === tab ? '.active' : ''}`,
            {
              key: tab,
              attrs: {
                type: 'button',
                role: 'tab',
                'aria-selected': ctrl.reviewPrimaryTab() === tab ? 'true' : 'false',
                'data-review-tab': tab,
              },
              hook: bind('click', () => ctrl.setReviewPrimaryTab(tab)),
            },
            [hl('span', label), renderPrimaryTabMeta(ctrl, tab, stats)],
          ),
        ),
      ),
    ]),
    renderUtilityPanel(ctrl, nodes),
    hl('div.analyse-review__body', [renderPrimaryTab(ctrl, nodes)]),
  ]);
}

export function filterNarrativeMoments(
  moments: readonly GameChronicleMoment[],
  filter: NarrativeMomentFilter,
): GameChronicleMoment[] {
  if (filter === 'all') return moments.slice();
  if (filter === 'collapses') return moments.filter(moment => !!moment.collapse);
  return moments.filter(isCriticalMoment);
}

function isCriticalMoment(moment: GameChronicleMoment): boolean {
  if (moment.collapse) return true;
  const haystack = [moment.moveClassification, moment.momentType, moment.strategicSalience]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return ['critical', 'turning', 'blunder', 'mistake', 'missed', 'swing'].some(token => haystack.includes(token));
}

function renderPrimaryTab(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  switch (ctrl.reviewPrimaryTab()) {
    case 'overview':
      return renderOverview(ctrl);
    case 'moments':
      return renderMoments(ctrl);
    case 'repair':
      return renderRepair(ctrl);
    case 'patterns':
      return renderPatterns(ctrl);
    case 'moves':
      return renderMoves(ctrl, nodes);
    case 'import':
      return renderImport(ctrl, nodes);
  }
}

function renderOverview(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  const data = narrative?.data();
  const stats = reviewOverviewStats(ctrl);
  const totalPly = totalMainlinePly(ctrl);
  const chronicleEligible = totalPly >= MIN_GAME_CHRONICLE_PLY;

  if (!narrative) return hl('div.analyse-review__empty', 'Game Chronicle review is unavailable on this surface.');

  if (!data) {
    return hl('div.analyse-review__overview', [
      hl('section.analyse-review__hero', [
        hl('span.analyse-review__eyebrow', 'Post-game review'),
        hl('h2', 'Run Game Chronicle'),
        hl(
          'p',
          'Generate a Game Chronicle with turning points, repair windows, and pattern tracking before you drop into raw moves.',
        ),
        narrative.loading()
          ? hl('div.loader', narrative.loadingDetail() || 'Game Chronicle in progress...')
          : narrative.error()
            ? hl('div.analyse-review__status.is-error', [
                hl('div', narrative.error()),
                narrative.needsLogin() ? hl('a.button', { attrs: { href: narrative.loginHref() } }, 'Sign in') : null,
              ])
            : chronicleEligible
              ? hl(
                'button.button.button-fat',
                {
                  hook: bind('click', () => {
                    void ctrl.openNarrative();
                  }),
                },
                'Run Game Chronicle',
              )
              : hl('div.analyse-review__status', [
                  hl(
                    'button.button.button-fat',
                    {
                      attrs: { type: 'button', disabled: true },
                    },
                    `Unlocks at move ${moveNumberFromPly(MIN_GAME_CHRONICLE_PLY)}`,
                  ),
                ]),
      ]),
      hl('div.analyse-review__overview-cards', [
        overviewCard(licon.BubbleSpeech, 'Moments', 'Surface the turning points instead of scanning every move.'),
        overviewCard(licon.Target, 'Repair', 'See where the game first broke and replay the patch line.'),
        overviewCard(licon.Book, 'Patterns', 'Track recurring collapse causes across analyzed games.'),
      ]),
    ]);
  }

  const collapseMoments = data.moments.filter(moment => !!moment.collapse);
  return hl('div.analyse-review__overview', [
    hl('section.analyse-review__hero.analyse-review__hero--ready', [
      hl('span.analyse-review__eyebrow', 'Review ready'),
      hl('h2', 'Narrative-first game review'),
      data.themes?.length
        ? hl('div.narrative-themes', data.themes.map(theme => hl('span.narrative-theme', cleanNarrativeSurfaceLabel(theme))))
        : null,
      renderOverviewStats(stats),
      narrativeReviewView(data),
      data.intro ? hl('pre.narrative-prose', cleanNarrativeProseText(data.intro)) : null,
    ]),
    hl('div.analyse-review__next-actions', [
      actionCard(ctrl, licon.BubbleSpeech, 'Go to Moments', 'Read the selected turning points in order.', 'moments'),
      actionCard(
        ctrl,
        licon.Target,
        collapseMoments.length ? 'Open Repair' : 'Repair unavailable',
        collapseMoments.length
          ? `Inspect ${collapseMoments.length} causal collapse${collapseMoments.length > 1 ? 's' : ''}.`
          : 'This game has no causal collapse window yet.',
        'repair',
        !collapseMoments.length,
      ),
      actionCard(ctrl, licon.Book, 'See Patterns', 'Open Defeat DNA and look for recurring failure modes.', 'patterns'),
    ]),
  ]);
}

function overviewCard(iconName: string, title: string, body: string): VNode {
  return hl('article.analyse-review__card', [
    hl('span.analyse-review__card-icon', icon(iconName as any)),
    hl('strong', title),
    hl('p', body),
  ]);
}

function actionCard(
  ctrl: AnalyseCtrl,
  iconName: string,
  title: string,
  body: string,
  tab: ReviewPrimaryTab,
  disabled = false,
): VNode {
  return hl(
    `button.analyse-review__action-card${disabled ? '.disabled' : ''}`,
    {
      attrs: { type: 'button', disabled },
      hook: disabled ? undefined : bind('click', () => ctrl.setReviewPrimaryTab(tab)),
    },
    [hl('span.analyse-review__card-icon', icon(iconName as any)), hl('strong', title), hl('p', body)],
  );
}

function renderMoments(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  const data = narrative?.data();

  if (!narrative) return hl('div.analyse-review__empty', 'Game Chronicle review is unavailable on this surface.');
  if (!data) return renderMissingNarrative(ctrl, 'Run deep analysis to unlock the moment-by-moment review.');

  const filtered = filterNarrativeMoments(data.moments || [], ctrl.reviewMomentFilter());
  return hl('div.analyse-review__moments', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Moments'),
      hl(
        'p',
        filtered.length === data.moments.length
          ? `Showing all ${filtered.length} highlighted moments.`
          : `Showing ${filtered.length} of ${data.moments.length} highlighted moments.`,
      ),
    ]),
    hl('div.analyse-review__filters', [
      hl('strong', 'Moment filter'),
      ...momentFilters.map(({ filter, label }) =>
        hl(
          `button.analyse-review__filter${ctrl.reviewMomentFilter() === filter ? '.active' : ''}`,
          {
            key: filter,
            attrs: { type: 'button' },
            hook: bind('click', () => ctrl.setReviewMomentFilter(filter)),
          },
          label,
        ),
      ),
    ]),
    filtered.length
      ? hl(
          'div.analyse-review__moment-list',
          { hook: ensureActiveChildVisible('.narrative-moment.active') },
          filtered.map(moment =>
            narrativeMomentView(narrative, moment, {
              selected: ctrl.selectedReviewMomentPly() === moment.ply,
              compact: true,
              onSelect: () => {
                ctrl.selectReviewMoment(moment.ply);
                ctrl.jumpToMain(moment.ply);
              },
            }),
          ),
        )
      : hl('div.analyse-review__empty', 'No moments match the current filter.'),
  ]);
}

function renderRepair(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  const data = narrative?.data();

  if (!narrative) return hl('div.analyse-review__empty', 'Game Chronicle review is unavailable on this surface.');
  if (!data) return renderMissingNarrative(ctrl, 'Run deep analysis to unlock collapse diagnosis and repair lines.');

  const collapseMoments = data.moments.filter(moment => !!moment.collapse);
  const selectedCollapse = collapseMoments.find(moment => moment.collapse?.interval === ctrl.selectedReviewCollapseId());
  if (!collapseMoments.length) {
    return hl('div.analyse-review__empty', [
      hl('strong', 'No causal collapse detected'),
      hl('p', 'This game does not have a repair window yet. Review the moments list for the main turning points.'),
      hl(
        'button.button',
        {
          hook: bind('click', () => ctrl.setReviewPrimaryTab('moments')),
        },
        'Go to Moments',
      ),
    ]);
  }

  return hl('div.analyse-review__repair', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Repair'),
      hl(
        'p',
        selectedCollapse?.collapse
          ? `Reviewing ${selectedCollapse.collapse.rootCause} across the collapse window.`
          : `Found ${collapseMoments.length} collapse window${collapseMoments.length > 1 ? 's' : ''}.`,
      ),
    ]),
    collapseTimelineView(narrative, collapseMoments),
    hl(
      'div.analyse-review__collapse-list',
      { hook: ensureActiveChildVisible('.narrative-collapse-card.active') },
      collapseMoments.map(moment =>
        narrativeCollapseCardView(narrative, moment, {
          selected: ctrl.selectedReviewCollapseId() === moment.collapse?.interval,
          onSelect: () => {
            if (!moment.collapse) return;
            ctrl.selectReviewCollapse(moment.collapse.interval);
            ctrl.jumpToMain(moment.collapse.earliestPreventablePly);
          },
        }),
      ),
    ),
  ]);
}

function renderPatterns(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  if (!narrative) return hl('div.analyse-review__empty', 'Game Chronicle review is unavailable on this surface.');

  if (!narrative.dnaLoading() && !narrative.dnaError() && !narrative.dnaData()) {
    return hl('div.analyse-review__empty', [
      hl('strong', 'Patterns unlock after more analysis'),
      hl('p', 'Defeat DNA becomes active after you accumulate enough deep reviews.'),
      hl(
        'button.button',
        {
          hook: bind('click', () => ctrl.setReviewPrimaryTab('overview')),
        },
        'Back to Overview',
      ),
    ]);
  }

  return hl('div.analyse-review__patterns', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Patterns'),
      hl(
        'p',
        narrative.dnaData()
          ? `Account-level profile built from ${narrative.dnaData()!.totalGamesAnalyzed} analyzed games.`
          : narrative.dnaLoading()
            ? 'Building your Defeat DNA profile.'
            : 'Review recurring failure modes across analyzed games.',
      ),
    ]),
    defeatDnaContentView(narrative),
  ]);
}

function renderMoves(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const engineEnabled = !!ctrl.cevalEnabled();
  return hl('div.analyse-review__moves', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Raw analysis'),
      hl(
        'p',
        engineEnabled
          ? 'Live Stockfish lines stay pinned above while the move tree and branch workbench remain available below.'
          : 'Turn on local engine from the header above to pin live Stockfish lines over the move tree and branch workbench.',
      ),
    ]),
    !engineEnabled
      ? hl('div.analyse-review__empty', [
          hl('strong', 'Local engine is off'),
          hl(
            'p',
            'Use the engine switch in the header above to start local Stockfish and surface live MultiPV lines over this review shell.',
          ),
          hl(
            'button.button',
            {
              hook: bind('click', () => ctrl.cevalEnabled(true)),
            },
            'Turn On Engine',
          ),
        ])
      : null,
    nodes.moveListNode,
    nodes.forkNode,
  ]);
}

function renderUtilityPanel(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode | null {
  const panel = ctrl.reviewUtilityPanel();
  if (!panel) return null;
  const heading = panel === 'explorer' ? 'Opening Explorer' : 'Board View';
  const description =
    panel === 'explorer'
      ? 'Theory, database, and tablebase stay available without leaving the review flow.'
      : 'Adjust board guides, perspective, and display settings without leaving the review flow.';
  const body =
    panel === 'explorer'
      ? hl('div.analyse-review__panel.analyse-review__panel--explorer', [
          ctrl.explorer.allowed()
            ? nodes.explorerNode
            : hl('div.analyse-review__empty', 'Opening explorer is unavailable for this position.'),
        ])
      : hl('div.action-menu.analyse-review__panel.analyse-review__panel--board', nodes.boardSettingsNodes);
  return hl('section.analyse-review__utility', [
    hl('div.analyse-review__utility-head', [
      hl('div.analyse-review__utility-copy', [hl('strong', heading), hl('span', description)]),
      hl(
        'button.analyse-review__utility-close',
        {
          attrs: { type: 'button' },
          hook: bind('click', () => ctrl.setReviewUtilityPanel(null)),
        },
        'Close panel',
      ),
    ]),
    hl('div.analyse-review__utility-body', [body]),
  ]);
}

function renderImport(_: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__import', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Import PGN'),
      hl('p', 'Paste a PGN or jump by FEN without leaving this analysis shell.'),
    ]),
    hl('div.analyse-review__section-copy', [
      hl('strong', 'Stay in analysis'),
      hl(
        'p',
        'PGN import belongs here. Use the top-bar Recent Games page when you want to pull games from a Lichess or Chess.com account.',
      ),
    ]),
    hl('div.analyse-review__panel.analyse-review__panel--import', [
      nodes.importNode || hl('div.analyse-review__empty', 'Import is unavailable during live play.'),
    ]),
  ]);
}

function renderMissingNarrative(ctrl: AnalyseCtrl, message: string): VNode {
  const narrative = ctrl.narrative;
  return hl('div.analyse-review__empty', [
    hl('strong', 'Deep analysis not started'),
    hl('p', message),
    narrative?.loading()
        ? hl('div.loader', narrative.loadingDetail() || 'Game Chronicle in progress...')
      : hl(
          'button.button',
          {
            hook: bind('click', () => {
              void ctrl.openNarrative();
            }),
          },
        'Run Game Chronicle',
        ),
  ]);
}

function reviewOverviewStats(ctrl: AnalyseCtrl): ReviewOverviewStats {
  const data = ctrl.narrative?.data();
  return {
    totalMoments: data?.moments.length || 0,
    collapseMoments: data?.moments.filter(moment => !!moment.collapse).length || 0,
    selectedMoments: data?.review?.selectedMoments || 0,
    evalCoveragePct: typeof data?.review?.evalCoveragePct === 'number' ? data.review.evalCoveragePct : null,
  };
}

function renderOverviewStats(stats: ReviewOverviewStats): VNode | null {
  const items = [
    stats.totalMoments ? compactStat(String(stats.totalMoments), 'moments') : null,
    stats.collapseMoments ? compactStat(String(stats.collapseMoments), 'repair windows') : null,
    stats.selectedMoments ? compactStat(String(stats.selectedMoments), 'selected beats') : null,
    stats.evalCoveragePct !== null ? compactStat(`${Math.round(stats.evalCoveragePct)}%`, 'eval coverage') : null,
  ].filter(Boolean) as VNode[];
  return items.length ? hl('div.analyse-review__summary-grid', items) : null;
}

function compactStat(value: string, label: string): VNode {
  return hl('div.analyse-review__summary-card', [hl('strong', value), hl('span', label)]);
}

function renderPrimaryTabMeta(ctrl: AnalyseCtrl, tab: ReviewPrimaryTab, stats: ReviewOverviewStats): VNode | null {
  if (tab === 'overview' && ctrl.narrative?.loading()) return hl('span.analyse-review__tab-meta.is-busy', 'Running');
  if (tab === 'overview' && ctrl.narrative?.data()) return hl('span.analyse-review__tab-meta', 'Ready');
  if (tab === 'moments' && stats.totalMoments) return hl('span.analyse-review__tab-meta', String(stats.totalMoments));
  if (tab === 'repair' && stats.collapseMoments) return hl('span.analyse-review__tab-meta', String(stats.collapseMoments));
  if (tab === 'patterns' && ctrl.narrative?.dnaLoading()) return hl('span.analyse-review__tab-meta.is-busy', '...');
  if (tab === 'patterns' && ctrl.narrative?.dnaData()) {
    return hl('span.analyse-review__tab-meta', String(ctrl.narrative.dnaData()!.totalGamesAnalyzed));
  }
  return null;
}

function ensureActiveChildVisible(selector: string) {
  const sync = (elm: Element) => {
    const container = elm as HTMLElement;
    const active = container.querySelector<HTMLElement>(selector);
    if (!active) return;
    const activeKey = active.dataset.reviewTab || active.dataset.referenceTab || active.dataset.ply || active.dataset.collapseId || active.textContent || '';
    if (container.dataset.activeKey === activeKey) return;
    container.dataset.activeKey = activeKey;
    requestAnimationFrame(() =>
      active.scrollIntoView({
        block: 'nearest',
        inline: 'nearest',
        behavior: 'smooth',
      }),
    );
  };
  return {
    insert: (vnode: VNode) => sync(vnode.elm as Element),
    postpatch: (_old: VNode, vnode: VNode) => sync(vnode.elm as Element),
  };
}
