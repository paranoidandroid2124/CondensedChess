import * as licon from 'lib/licon';
import type { LooseVNode, LooseVNodes, VNode } from 'lib/view';
import { bind, hl, icon } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import type { NarrativeMomentFilter, ReviewPrimaryTab, ReviewReferenceTab } from './state';
import type { GameChronicleMoment } from '../narrative/narrativeCtrl';
import { isOpening } from '../explorer/interfaces';
import {
  collapseTimelineView,
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

type ReferenceTabMeta = {
  tab: ReviewReferenceTab;
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
  { tab: 'reference', label: 'Reference' },
];

const referenceTabs: ReferenceTabMeta[] = [
  { tab: 'explorer', label: 'Explorer' },
  { tab: 'board', label: 'Board View' },
  { tab: 'import', label: 'Import' },
];

const momentFilters: Array<{ filter: NarrativeMomentFilter; label: string }> = [
  { filter: 'all', label: 'All' },
  { filter: 'critical', label: 'Critical' },
  { filter: 'collapses', label: 'Collapses' },
];

export function reviewView(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const stats = reviewOverviewStats(ctrl);
  return hl('section.analyse-review', [
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
      return renderMoves(nodes);
    case 'reference':
      return renderReference(ctrl, nodes);
  }
}

function renderOverview(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  const data = narrative?.data();
  const stats = reviewOverviewStats(ctrl);

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
            : hl(
                'button.button.button-fat',
                {
                  hook: bind('click', () => {
                    void ctrl.openNarrative();
                  }),
                },
                'Run Game Chronicle',
              ),
      ]),
      hl('div.analyse-review__overview-cards', [
        overviewCard(licon.BubbleSpeech, 'Moments', 'Surface the turning points instead of scanning every ply.'),
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
        ? hl('div.narrative-themes', data.themes.map(theme => hl('span.narrative-theme', theme)))
        : null,
      renderOverviewStats(stats),
      narrativeReviewView(data),
      data.intro ? hl('pre.narrative-prose', data.intro) : null,
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
          ? `Reviewing ${selectedCollapse.collapse.rootCause} across ply ${selectedCollapse.collapse.interval}.`
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

function renderMoves(nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__moves', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Raw analysis'),
      hl(
        'p',
        nodes.cevalNode
          ? 'Engine lines, move tree, and branch workbench remain available as reference.'
          : 'Enable the engine panel from Board View, or turn on local analysis from the control bar to surface engine lines.',
      ),
    ]),
    nodes.cevalNode,
    nodes.pvsNode,
    nodes.moveListNode,
    nodes.forkNode,
  ]);
}

function renderReference(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__reference', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Reference'),
      hl('p', referenceHint(ctrl.reviewReferenceTab())),
    ]),
    renderReferenceSummary(ctrl),
    renderReferenceActions(ctrl),
    hl(
      'div.analyse-review__subtabs',
      {
        hook: ensureActiveChildVisible('[data-reference-tab].active'),
        attrs: { role: 'tablist', 'aria-label': 'Reference sections' },
      },
      referenceTabs.map(({ tab, label }) =>
        hl(
          `button.analyse-review__subtab${ctrl.reviewReferenceTab() === tab ? '.active' : ''}`,
          {
            key: tab,
            attrs: {
              type: 'button',
              role: 'tab',
              'aria-selected': ctrl.reviewReferenceTab() === tab ? 'true' : 'false',
              'data-reference-tab': tab,
            },
            hook: bind('click', () => ctrl.setReviewReferenceTab(tab)),
          },
          label,
        ),
      ),
    ),
    hl('div.analyse-review__reference-body', [renderReferenceBody(ctrl, nodes)]),
  ]);
}

function renderReferenceSummary(ctrl: AnalyseCtrl): VNode {
  const explorerAllowed = ctrl.explorer.allowed();
  const explorerCurrent = typeof ctrl.explorer.current === 'function' ? ctrl.explorer.current() : undefined;
  const explorerDb = typeof ctrl.explorer.db === 'function' ? ctrl.explorer.db() : 'lichess';
  const explorerLoading = typeof ctrl.explorer.loading === 'function' ? ctrl.explorer.loading() : false;
  const explorerOpening = explorerCurrent && isOpening(explorerCurrent) ? explorerCurrent.opening : undefined;
  const positionFen = ctrl.node?.fen;
  const sideToMove = positionFen?.split(' ')[1] === 'b' ? 'Black to move' : 'White to move';
  const openingName = explorerOpening?.name || ctrl.data.game.opening?.name || ctrl.data.game.variant.name;
  const openingEco = explorerOpening?.eco || ctrl.data.game.opening?.eco;
  const ply = typeof ctrl.node?.ply === 'number' ? `${ctrl.node.ply} ply` : 'Current node';

  return hl('div.analyse-review__summary-grid', [
    compactStat(
      !explorerAllowed
        ? 'Unavailable'
        : explorerLoading
          ? 'Loading'
          : explorerCurrent?.moves?.length
            ? `${explorerCurrent.moves.length} moves`
            : 'Ready',
      'explorer status',
    ),
    compactStat(
      openingEco ? `${openingEco} ${openingName}` : openingName || 'Custom position',
      'current position',
    ),
    compactStat(`${sideToMove} • ${ply}`, explorerDb === 'masters' ? 'masters db' : 'online db'),
  ]);
}

function renderReferenceActions(ctrl: AnalyseCtrl): VNode {
  return hl('div.analyse-review__reference-actions', [
    hl(
      'button.analyse-review__reference-action.analyse-review__reference-action--primary',
      {
        attrs: { type: 'button' },
        hook: bind('click', () => ctrl.setReviewPrimaryTab('overview')),
      },
      'Back to Overview',
    ),
    hl(
      'button.analyse-review__reference-action',
      {
        attrs: { type: 'button' },
        hook: bind('click', () => ctrl.setReviewPrimaryTab('moves')),
      },
      'Back to Moves',
    ),
  ]);
}

function renderReferenceBody(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  switch (ctrl.reviewReferenceTab()) {
    case 'explorer':
      return hl('div.analyse-review__panel.analyse-review__panel--explorer', [
        ctrl.explorer.allowed()
          ? nodes.explorerNode
          : hl('div.analyse-review__empty', 'Opening explorer is unavailable for this position.'),
      ]);
    case 'board':
      return hl('div.action-menu.analyse-review__panel.analyse-review__panel--board', nodes.boardSettingsNodes);
    case 'import':
      return hl('div.analyse-review__panel.analyse-review__panel--import', [
        nodes.importNode || hl('div.analyse-review__empty', 'Import is unavailable during live play.'),
      ]);
  }
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

function referenceHint(tab: ReviewReferenceTab): string {
  switch (tab) {
    case 'explorer':
      return 'Theory, game database, and tablebase access stay available as a secondary lookup surface.';
    case 'board':
      return 'Tune board view, guides, and perspective without dropping out of the review flow.';
    case 'import':
      return 'Stage a FEN jump or PGN import, preview the change, and relaunch from the same shell.';
  }
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
