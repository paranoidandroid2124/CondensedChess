import type { LooseVNode, LooseVNodes, VNode } from 'lib/view';
import { bind, hl } from 'lib/view';
import { cleanNarrativeProseText, cleanNarrativeSurfaceLabel } from '../chesstory/signalFormatting';
import type AnalyseCtrl from '../ctrl';
import { bookmakerToggleBox } from '../bookmaker';
import type { NarrativeMomentFilter, ReviewPrimaryTab } from './state';
import {
  MIN_GAME_CHRONICLE_PLY,
  moveNumberFromPly,
  totalMainlinePly,
  type GameChronicleMoment,
} from '../narrative/narrativeCtrl';
import {
  bindPreviewHover,
  collapseTimelineView,
  defeatDnaContentView,
  narrativeCollapseCardView,
  narrativeMomentView,
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

type ReviewOverviewStats = {
  totalMoments: number;
  collapseMoments: number;
  selectedMoments: number;
  evalCoveragePct: number | null;
};

type CoachPaneTab = 'review' | 'explain' | 'engine' | 'explorer' | 'board';
type RawPaneTab = 'moves' | 'explain' | 'engine' | 'explorer' | 'board' | 'patterns' | 'import';

export function reviewView(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  return hl(
    'section.analyse-review',
    {
      hook: {
        insert: vnode => {
          if (ctrl.narrative) bindPreviewHover(ctrl.narrative, vnode.elm as HTMLElement);
        },
        postpatch: (_, vnode) => {
          if (ctrl.narrative) bindPreviewHover(ctrl.narrative, vnode.elm as HTMLElement);
        },
      },
    },
    [
      renderSurfaceSwitch(ctrl),
      hl('div.analyse-review__body', [
        ctrl.reviewSurfaceMode() === 'review' ? renderCoachReview(ctrl, nodes) : renderRawWorkspace(ctrl, nodes),
      ]),
    ],
  );
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

function renderSurfaceSwitch(ctrl: AnalyseCtrl): VNode {
  return hl('div.analyse-review__surface-switch', [
    surfaceModeButton(ctrl, 'review', 'Guided Review'),
    surfaceModeButton(ctrl, 'raw', 'Full Analysis'),
  ]);
}

function surfaceModeButton(ctrl: AnalyseCtrl, mode: 'review' | 'raw', label: string): VNode {
  return hl(
    `button.analyse-review__surface-toggle${ctrl.reviewSurfaceMode() === mode ? '.active' : ''}`,
    {
      attrs: {
        type: 'button',
        'aria-pressed': ctrl.reviewSurfaceMode() === mode ? 'true' : 'false',
      },
      hook: bind('click', () => ctrl.setReviewSurfaceMode(mode)),
    },
    label,
  );
}

function renderCoachReview(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const narrative = ctrl.narrative;
  const data = narrative?.data();
  const totalPly = totalMainlinePly(ctrl);
  const chronicleEligible = totalPly >= MIN_GAME_CHRONICLE_PLY;

  if (!narrative) return hl('div.analyse-review__empty', 'Guided review is unavailable for this game.');

  if (!data) {
    return hl('div.analyse-review__coach', [
      hl('section.analyse-review__hero', [
        hl('span.analyse-review__eyebrow', 'Guided Review'),
        hl('h2', 'Review this game'),
        hl(
          'p',
          'Build a board-first review with critical moments, one main explanation card, and the best chance to fix the game before you open full analysis.',
        ),
        narrative.loading()
          ? hl('div.loader', narrative.loadingDetail() || 'Guided review in progress...')
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
                  'Review this game',
                )
              : hl('div.analyse-review__status', [
                  hl(
                    'button.button.button-fat',
                    {
                      attrs: { type: 'button', disabled: true },
                    },
                    `Available after move ${moveNumberFromPly(MIN_GAME_CHRONICLE_PLY)}`,
                  ),
                ]),
      ]),
    ]);
  }

  const stats = reviewOverviewStats(ctrl);
  const selectedMoment = selectedCoachMoment(ctrl);
  const keyMoments = keyCoachMoments(ctrl, selectedMoment);
  const selectedCollapse = selectedMoment?.collapse ? selectedMoment : null;
  const accountHref = ctrl.accountPatternsHref();
  const activeTab = resolveCoachTab(ctrl.reviewPrimaryTab());

  return hl('div.analyse-review__coach', [
    hl('section.analyse-review__hero.analyse-review__hero--ready', [
      hl('span.analyse-review__eyebrow', 'Guided Review'),
      hl('h2', reviewHeadline(data.themes || [])),
      hl('p', reviewLead(data.intro, data.conclusion)),
      data.themes?.length
        ? hl(
            'div.narrative-themes',
            data.themes.map(theme => hl('span.narrative-theme', cleanNarrativeSurfaceLabel(theme))),
          )
        : null,
      renderOverviewStats(stats),
      hl('div.analyse-review__hero-actions', [
        renderCoachExpertStrip(ctrl),
        hl(
          'button.button',
          {
            hook: bind('click', () => openFullAnalysis(ctrl, { tab: 'moves' })),
          },
          'Open full analysis',
        ),
      ]),
    ]),
    hl('div.analyse-review__moment-layout', [
      renderMomentListCard(
        ctrl,
        keyMoments,
        selectedMoment,
        activeTab === 'review'
          ? 'Selecting one keeps the board and the explanation in sync.'
          : 'Selecting one keeps the board in sync while you work in another tool.',
      ),
      hl('div.analyse-review__detail-stack', [
        renderCoachTabs(ctrl, activeTab),
        renderCoachPane(ctrl, nodes, narrative, selectedMoment, selectedCollapse, data, stats, accountHref, activeTab),
      ]),
    ]),
  ]);
}

function renderMomentListCard(
  ctrl: AnalyseCtrl,
  keyMoments: GameChronicleMoment[],
  selectedMoment: GameChronicleMoment | null,
  copy: string,
): VNode {
  return hl('section.analyse-review__moment-list-card', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Critical moments'),
      hl('p', `Start with ${keyMoments.length} curated moments. ${copy}`),
    ]),
    hl(
      'div.analyse-review__moment-list',
      { hook: ensureActiveChildVisible('.analyse-review__moment-pill.active') },
      keyMoments.map(moment =>
        hl(
          `button.analyse-review__moment-pill${selectedMoment?.ply === moment.ply ? '.active' : ''}`,
          {
            key: String(moment.ply),
            attrs: { type: 'button' },
            hook: bind('click', () => {
              ctrl.setReviewSelectedMoment(moment.ply);
              ctrl.jumpToMain(moment.ply);
            }),
          },
          [
            hl('span.analyse-review__moment-pill-kicker', reviewMomentLabel(moment)),
            hl('strong', reviewMomentTitle(moment)),
            hl('span.analyse-review__moment-pill-copy', reviewMomentCopy(moment)),
          ],
        ),
      ),
    ),
  ]);
}

function renderCoachTabs(ctrl: AnalyseCtrl, activeTab: CoachPaneTab): VNode {
  return hl('div.analyse-review__tool-tabs', [
    coachTabButton(ctrl, activeTab, 'review', 'Review', 'overview'),
    ctrl.opts.bookmaker ? coachTabButton(ctrl, activeTab, 'explain', 'Explain', 'explain') : null,
    coachTabButton(ctrl, activeTab, 'engine', 'Engine', 'engine'),
    ctrl.explorer.allowed() ? coachTabButton(ctrl, activeTab, 'explorer', 'Explorer', 'explorer') : null,
    coachTabButton(ctrl, activeTab, 'board', 'Board', 'board'),
  ]);
}

function coachTabButton(
  ctrl: AnalyseCtrl,
  activeTab: CoachPaneTab,
  tab: CoachPaneTab,
  label: string,
  primaryTab: ReviewPrimaryTab,
): VNode {
  return hl(
    `button.analyse-review__tab${activeTab === tab ? '.active' : ''}`,
    {
      attrs: { type: 'button' },
      hook: bind('click', () => ctrl.setReviewPrimaryTab(primaryTab)),
    },
    label,
  );
}

function renderCoachPane(
  ctrl: AnalyseCtrl,
  nodes: ReviewViewNodes,
  narrative: NonNullable<AnalyseCtrl['narrative']>,
  selectedMoment: GameChronicleMoment | null,
  selectedCollapse: GameChronicleMoment | null,
  data: {
    intro?: string | null;
    conclusion?: string | null;
    themes?: string[];
    sourceMode?: string;
    model?: string | null;
    planTier?: string;
    llmLevel?: string;
    review?: { totalPlies?: number; evalCoveredPlies?: number };
  },
  stats: ReviewOverviewStats,
  accountHref: string | null,
  activeTab: CoachPaneTab,
): VNode {
  if (activeTab === 'explain')
    return hl('div.analyse-review__tool-pane', [
      renderSelectedMomentContext(selectedMoment, 'Selected moment'),
      renderExplainMovePanel(
        ctrl,
        'Selected moment only',
        'Ask for a move-level explanation of this selected moment. Choose another moment on the left to update the request without leaving the current viewport.',
      ),
    ]);

  if (activeTab === 'engine')
    return hl('div.analyse-review__tool-pane', [
      renderSelectedMomentContext(selectedMoment, 'Board-linked moment'),
      renderCoachEnginePreview(ctrl, nodes),
    ]);

  if (activeTab === 'explorer')
    return hl('div.analyse-review__tool-pane', [
      renderSelectedMomentContext(selectedMoment, 'Board-linked moment'),
      renderExplorerTab(ctrl, nodes),
    ]);

  if (activeTab === 'board')
    return hl('div.analyse-review__tool-pane', [
      renderSelectedMomentContext(selectedMoment, 'Board-linked moment'),
      renderBoardTab(nodes),
    ]);

  return hl('div.analyse-review__tool-pane', [
    renderCoachReviewCard(ctrl, narrative, selectedMoment),
    selectedCollapse
      ? renderSectionDrawer(
          'Best chance to fix it',
          'Open the repair window for the current collapse interval.',
          [
            collapseTimelineView(narrative, [selectedCollapse]),
            narrativeCollapseCardView(narrative, selectedCollapse, {
              selected: true,
              onSelect: () => {
                if (!selectedCollapse.collapse) return;
                ctrl.selectReviewCollapse(selectedCollapse.collapse.interval);
                ctrl.jumpToMain(selectedCollapse.collapse.earliestPreventablePly);
              },
            }),
          ],
          ctrl.reviewPrimaryTab() === 'repair',
        )
      : null,
    renderSectionDrawer(
      'What to remember',
      cleanNarrativeProseText(
        data.conclusion || 'Review the selected moment, then open full analysis only when you need the full move tree.',
      ),
      [
        hl('div.analyse-review__context-actions', [
          hl(
            'button.button',
            {
              hook: bind('click', () => ctrl.setReviewPrimaryTab('explain')),
            },
            'Explain this move',
          ),
          hl(
            'button.button.button-empty',
            {
              hook: bind('click', () => openFullAnalysis(ctrl, { tab: 'moves' })),
            },
            'Open full analysis',
          ),
          accountHref ? hl('a.button.button-empty', { attrs: { href: accountHref } }, 'See if this shows up often') : null,
        ]),
      ],
    ),
    renderAnalysisDetails(ctrl, data, stats),
  ]);
}

function renderCoachReviewCard(
  ctrl: AnalyseCtrl,
  narrative: NonNullable<AnalyseCtrl['narrative']>,
  selectedMoment: GameChronicleMoment | null,
): VNode {
  return selectedMoment
    ? hl('section.analyse-review__detail-card', [
        hl('div.analyse-review__section-head', [
          hl('h3', 'Why this move mattered'),
          hl(
            'p',
            'The selected moment stays board-linked. Use the tabs above when you want explanation, engine, explorer, or board controls without leaving the current tools pane.',
          ),
        ]),
        narrativeMomentView(narrative, selectedMoment, {
          selected: true,
          onSelect: () => {
            ctrl.setReviewSelectedMoment(selectedMoment.ply);
            ctrl.jumpToMain(selectedMoment.ply);
          },
        }),
        hl('div.analyse-review__context-actions', [
          ctrl.opts.bookmaker
            ? hl(
                'button.button.button-empty',
                {
                  hook: bind('click', () => ctrl.setReviewPrimaryTab('explain')),
                },
                'Explain this move',
              )
            : null,
          hl(
            'button.button.button-empty',
            {
              hook: bind('click', () => ctrl.setReviewPrimaryTab('engine')),
            },
            'Engine preview',
          ),
          ctrl.explorer.allowed()
            ? hl(
                'button.button.button-empty',
                {
                  hook: bind('click', () => ctrl.setReviewPrimaryTab('explorer')),
                },
                'Explorer',
              )
            : null,
          hl(
            'button.button.button-empty',
            {
              hook: bind('click', () => ctrl.setReviewPrimaryTab('board')),
            },
            'Board settings',
          ),
        ]),
      ])
    : hl('div.analyse-review__empty', 'No highlighted moment is available yet.');
}

function renderSelectedMomentContext(selectedMoment: GameChronicleMoment | null, label: string): VNode {
  return selectedMoment
    ? hl('section.analyse-review__context-card', [
        hl('span.analyse-review__eyebrow', label),
        hl('strong', `${reviewMomentLabel(selectedMoment)} ${reviewMomentTitle(selectedMoment)}`),
        hl('p', reviewMomentCopy(selectedMoment)),
      ])
    : hl('div.analyse-review__empty', 'Pick a moment from the left rail to anchor this tool.');
}

function renderSectionDrawer(title: string, summaryCopy: string, body: Array<VNode | LooseVNode | null>, open = false): VNode {
  return hl(
    'details.analyse-review__section-drawer',
    {
      attrs: open ? { open: true } : {},
    },
    [
      hl('summary', [hl('strong', title), hl('span', summaryCopy)]),
      hl('div.analyse-review__section-drawer-body', body),
    ],
  );
}

function renderRawWorkspace(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const activeTab = resolveRawTab(ctrl.reviewPrimaryTab());
  return hl('div.analyse-review__raw', [
    hl('section.analyse-review__section-head', [
      hl('h3', 'Full Analysis'),
      hl(
        'p',
        'Keep the current board position while you open the move list, Explain This Move, engine, explorer, board settings, recurring mistakes, or PGN import.',
      ),
    ]),
    hl('div.analyse-review__raw-toolbar', [
      hl(
        'button.analyse-review__reference-action.analyse-review__reference-action--primary',
        {
          attrs: { type: 'button' },
          hook: bind('click', () => ctrl.setReviewSurfaceMode('review')),
        },
        'Back to guided review',
      ),
      rawTabButton(ctrl, activeTab, 'moves', 'Moves'),
      ctrl.opts.bookmaker ? rawTabButton(ctrl, activeTab, 'explain', 'Explain This Move') : null,
      rawTabButton(ctrl, activeTab, 'engine', 'Engine'),
      ctrl.explorer.allowed() ? rawTabButton(ctrl, activeTab, 'explorer', 'Explorer') : null,
      rawTabButton(ctrl, activeTab, 'board', 'Board'),
      rawTabButton(ctrl, activeTab, 'patterns', 'Recurring mistakes'),
      rawTabButton(ctrl, activeTab, 'import', 'Import PGN'),
    ]),
    activeTab === 'patterns'
      ? renderPatterns(ctrl)
      : activeTab === 'import'
        ? renderImport(ctrl, nodes)
        : activeTab === 'explain'
          ? renderExplainMovePanel(
              ctrl,
              'Current move or variation',
              'Ask for a move-level explanation of the current node. Switch moves or variations first if you want a different branch.',
            )
          : activeTab === 'engine'
            ? renderCoachEnginePreview(ctrl, nodes)
            : activeTab === 'explorer'
              ? renderExplorerTab(ctrl, nodes)
              : activeTab === 'board'
                ? renderBoardTab(nodes)
                : renderMoves(ctrl, nodes),
  ]);
}

function rawTabButton(ctrl: AnalyseCtrl, activeTab: RawPaneTab, tab: RawPaneTab, label: string): VNode {
  return hl(
    `button.analyse-review__tab${activeTab === tab ? '.active' : ''}`,
    {
      key: tab,
      attrs: { type: 'button' },
      hook: bind('click', () => ctrl.setReviewPrimaryTab(tab)),
    },
    label,
  );
}

function selectedCoachMoment(ctrl: AnalyseCtrl): GameChronicleMoment | null {
  const moments = ctrl.narrative?.data()?.moments || [];
  const selected = ctrl.selectedReviewMomentPly();
  return moments.find(moment => moment.ply === selected) || keyCoachMoments(ctrl)[0] || moments[0] || null;
}

function keyCoachMoments(ctrl: AnalyseCtrl, selectedMoment?: GameChronicleMoment | null): GameChronicleMoment[] {
  const moments = ctrl.narrative?.data()?.moments || [];
  const critical = filterNarrativeMoments(moments, 'critical');
  const base = (critical.length ? critical : moments).slice(0, 5);
  const selected = selectedMoment || moments.find(moment => moment.ply === ctrl.selectedReviewMomentPly()) || null;
  if (!selected || base.some(moment => moment.ply === selected.ply)) return base;
  return [selected, ...base].slice(0, 5);
}

function reviewHeadline(themes: string[]): string {
  return themes.length ? cleanNarrativeSurfaceLabel(themes[0]) : 'Guided review';
}

function reviewLead(intro?: string | null, conclusion?: string | null): string {
  const source = cleanNarrativeProseText(intro || conclusion || '').replace(/\s+/g, ' ').trim();
  return source || 'Review the most important moments first, then open full analysis only when you need the full move tree.';
}

function reviewMomentLabel(moment: GameChronicleMoment): string {
  const moveNumber = moment.moveNumber || Math.floor((moment.ply + 1) / 2);
  const side = moment.side || (moment.ply % 2 === 0 ? 'black' : 'white');
  return `${moveNumber}${side === 'black' ? '...' : '.'}`;
}

function reviewMomentTitle(moment: GameChronicleMoment): string {
  return cleanNarrativeSurfaceLabel(moment.moveClassification || moment.selectionLabel || moment.momentType || 'Key moment');
}

function reviewMomentCopy(moment: GameChronicleMoment): string {
  const copy = cleanNarrativeProseText(moment.narrative || '').replace(/\s+/g, ' ').trim();
  return copy.length > 120 ? `${copy.slice(0, 117).trimEnd()}...` : copy || 'Open this moment for the full support card.';
}

function renderAnalysisDetails(
  ctrl: AnalyseCtrl,
  data: {
    sourceMode?: string;
    model?: string | null;
    planTier?: string;
    llmLevel?: string;
    review?: { totalPlies?: number; evalCoveredPlies?: number };
  },
  stats: ReviewOverviewStats,
): VNode {
  const metaRows: Array<[string, string | null | undefined]> = [
    ['Source', data.sourceMode],
    ['Model', data.model],
    ['Plan', data.planTier],
    ['Level', data.llmLevel],
    ['Total moments', stats.totalMoments ? String(stats.totalMoments) : null],
    ['Repair windows', stats.collapseMoments ? String(stats.collapseMoments) : null],
    ['Selected moments', stats.selectedMoments ? String(stats.selectedMoments) : null],
    ['Eval coverage', stats.evalCoveragePct !== null ? `${Math.round(stats.evalCoveragePct)}%` : null],
  ].filter(([, value]) => !!value) as [string, string][];

  if (!metaRows.length) return hl('div');

  return hl(
    'details.analyse-review__details-drawer',
    {
      attrs: ctrl.reviewAnalysisDetailsOpen() ? { open: true } : {},
      hook: bind('toggle', e => ctrl.setReviewAnalysisDetailsOpen((e.target as HTMLDetailsElement).open)),
    },
    [
      hl('summary', 'Review info'),
      hl(
        'div.analyse-review__details-grid',
        metaRows.map(([label, value]) => hl('div.analyse-review__details-row', [hl('strong', label), hl('span', value)])),
      ),
    ],
  );
}

function resolveCoachTab(tab: ReviewPrimaryTab): CoachPaneTab {
  if (tab === 'explain') return 'explain';
  if (tab === 'engine') return 'engine';
  if (tab === 'explorer') return 'explorer';
  if (tab === 'board') return 'board';
  return 'review';
}

function resolveRawTab(tab: ReviewPrimaryTab): RawPaneTab {
  if (
    tab === 'moves' ||
    tab === 'explain' ||
    tab === 'engine' ||
    tab === 'explorer' ||
    tab === 'board' ||
    tab === 'patterns' ||
    tab === 'import'
  )
    return tab;
  return 'moves';
}

function renderPatterns(ctrl: AnalyseCtrl): VNode {
  const narrative = ctrl.narrative;
  if (!narrative) return hl('div.analyse-review__empty', 'Recurring mistakes are unavailable for this game.');

  if (!narrative.dnaLoading() && !narrative.dnaError() && !narrative.dnaData()) {
    return hl('div.analyse-review__empty', [
      hl('strong', 'Patterns appear after more reviewed games'),
      hl('p', 'Review a few more games to unlock recurring mistakes.'),
      hl(
        'button.button',
        {
          hook: bind('click', () => ctrl.setReviewSurfaceMode('review')),
        },
        'Back to guided review',
      ),
    ]);
  }

  return hl('div.analyse-review__patterns', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Recurring mistakes'),
      hl(
        'p',
        narrative.dnaData()
          ? `Built from ${narrative.dnaData()!.totalGamesAnalyzed} reviewed games.`
          : narrative.dnaLoading()
            ? 'Building recurring mistakes from your reviewed games.'
            : 'Review recurring mistakes across analyzed games.',
      ),
    ]),
    defeatDnaContentView(narrative),
  ]);
}

function renderMoves(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const engineEnabled = !!ctrl.cevalEnabled();
  return hl('div.analyse-review__moves', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Moves'),
      hl(
        'p',
        engineEnabled
          ? 'The move list stays here while the Engine tab shows the live Stockfish preview.'
          : 'Turn on local engine, then open the Engine tab when you want live Stockfish lines.',
      ),
    ]),
    !engineEnabled
      ? hl('div.analyse-review__empty', [
          hl('strong', 'Local engine is off'),
          hl('p', 'Use the engine tab to start local Stockfish without leaving full analysis.'),
          hl(
            'button.button',
            {
              hook: bind('click', () => {
                ctrl.cevalEnabled(true);
                ctrl.setReviewPrimaryTab('engine');
              }),
            },
            'Open Engine tab',
          ),
        ])
      : null,
    nodes.moveListNode,
    nodes.forkNode,
  ]);
}

function renderExplorerTab(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__panel.analyse-review__panel--explorer', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Opening Explorer'),
      hl('p', 'Theory, database, and tablebase stay available without leaving the current tools pane.'),
    ]),
    ctrl.explorer.allowed()
      ? nodes.explorerNode
      : hl('div.analyse-review__empty', 'Opening explorer is unavailable for this position.'),
  ]);
}

function renderBoardTab(nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__panel.analyse-review__panel--board', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Board settings'),
      hl('p', 'Adjust guides, perspective, labels, and board display without leaving the current tools pane.'),
    ]),
    hl('div.action-menu.analyse-review__panel.analyse-review__panel--board', nodes.boardSettingsNodes),
  ]);
}

function renderExplainMovePanel(ctrl: AnalyseCtrl, title: string, copy: string): VNode {
  return ctrl.opts.bookmaker
    ? hl(
        'fieldset.analyse-review__detail-card.analyse-review__bookmaker-card.analyse__bookmaker.toggle-box.toggle-box--toggle.empty',
        {
          attrs: { id: 'bookmaker-field' },
          hook: {
            insert: () => bookmakerToggleBox(ctrl),
            update: () => bookmakerToggleBox(ctrl),
          },
        },
        [
          hl('legend', { attrs: { tabindex: '0' } }, 'Explain This Move'),
          hl('div.analyse-review__bookmaker-copy', [
            hl('strong', title),
            hl('p.analyse-review__bookmaker-note', copy),
          ]),
          hl('div.analyse__bookmaker-text'),
        ],
      )
    : hl('div.analyse-review__empty', 'Explain This Move is unavailable for this account.');
}

function renderImport(_: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  return hl('div.analyse-review__import', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Import PGN'),
      hl('p', 'Paste a PGN or jump by FEN without leaving full analysis.'),
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
    stats.selectedMoments ? compactStat(String(stats.selectedMoments), 'selected moments') : null,
    stats.evalCoveragePct !== null ? compactStat(`${Math.round(stats.evalCoveragePct)}%`, 'eval coverage') : null,
  ].filter(Boolean) as VNode[];
  return items.length ? hl('div.analyse-review__summary-grid', items) : null;
}

function compactStat(value: string, label: string): VNode {
  return hl('div.analyse-review__summary-card', [hl('strong', value), hl('span', label)]);
}

function ensureActiveChildVisible(selector: string) {
  const sync = (elm: Element) => {
    const active = elm.querySelector(selector);
    if (!active || typeof (active as HTMLElement).scrollIntoView !== 'function') return;
    (active as HTMLElement).scrollIntoView({ block: 'nearest', inline: 'nearest' });
  };

  return {
    insert(vnode: VNode) {
      sync(vnode.elm as Element);
    },
    postpatch(_: VNode, vnode: VNode) {
      sync(vnode.elm as Element);
    },
  };
}

function openFullAnalysis(ctrl: AnalyseCtrl, options: { tab?: RawPaneTab } = {}): void {
  if (options.tab) ctrl.setReviewPrimaryTab(options.tab);
  ctrl.setReviewSurfaceMode('raw');
}

function renderCoachExpertStrip(ctrl: AnalyseCtrl): VNode {
  const engineReady = !!ctrl.cevalEnabled();
  return hl('div.analyse-review__expert-strip', [
    ctrl.opts.bookmaker
      ? hl(
          `button.analyse-review__reference-action${ctrl.reviewPrimaryTab() === 'explain' ? '.analyse-review__reference-action--primary' : ''}`,
          {
            attrs: { type: 'button' },
            hook: bind('click', () => ctrl.setReviewPrimaryTab('explain')),
          },
          'Explain',
        )
      : null,
    hl(
      `button.analyse-review__reference-action${ctrl.reviewPrimaryTab() === 'engine' || engineReady ? '.analyse-review__reference-action--primary' : ''}`,
      {
        attrs: {
          type: 'button',
          'aria-pressed': ctrl.reviewPrimaryTab() === 'engine' ? 'true' : 'false',
        },
        hook: bind('click', () => {
          if (!engineReady) ctrl.cevalEnabled(true);
          ctrl.setReviewPrimaryTab('engine');
        }),
      },
      'Engine',
    ),
    ctrl.explorer.allowed()
      ? hl(
          `button.analyse-review__reference-action${ctrl.reviewPrimaryTab() === 'explorer' ? '.analyse-review__reference-action--primary' : ''}`,
          {
            attrs: { type: 'button' },
            hook: bind('click', () => ctrl.setReviewPrimaryTab('explorer')),
          },
          'Explorer',
        )
      : null,
    hl(
      `button.analyse-review__reference-action${ctrl.reviewPrimaryTab() === 'board' ? '.analyse-review__reference-action--primary' : ''}`,
      {
        attrs: { type: 'button' },
        hook: bind('click', () => ctrl.setReviewPrimaryTab('board')),
      },
      'Board settings',
    ),
  ]);
}

function renderCoachEnginePreview(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const engineEnabled = !!ctrl.cevalEnabled();
  const hasPreview = !!nodes.cevalNode || !!nodes.pvsNode;

  return hl('section.analyse-review__detail-card.analyse-review__engine-preview', [
    hl('div.analyse-review__section-head', [
      hl('h3', 'Engine preview'),
      hl(
        'p',
        engineEnabled
          ? 'Keep a lightweight Stockfish preview here. Open the Moves tab in full analysis only when you need the move tree and branch workbench.'
          : 'Turn on the local engine to pin a lightweight Stockfish preview in the current tools pane.',
      ),
    ]),
    !engineEnabled
      ? hl('div.analyse-review__empty', [
          hl('strong', 'Engine preview is off'),
          hl('p', 'Turn on the local engine here, or open full analysis if you want the full move list right away.'),
          hl('div.analyse-review__context-actions', [
            hl(
              'button.button',
              {
                hook: bind('click', () => ctrl.cevalEnabled(true)),
              },
              'Turn on engine preview',
            ),
            hl(
              'button.button.button-empty',
              {
                hook: bind('click', () => openFullAnalysis(ctrl, { tab: 'moves' })),
              },
              'Open full analysis',
            ),
          ]),
        ])
      : hasPreview
        ? hl('div.analyse-review__engine-stack', [
            nodes.cevalNode ? hl('div', nodes.cevalNode) : null,
            nodes.pvsNode ? hl('div', nodes.pvsNode) : null,
          ])
        : hl('div.analyse-review__empty', [
            hl('strong', 'Engine preview is loading'),
            hl('p', 'Local evaluation is on. The preview appears here as soon as lines are ready.'),
          ]),
  ]);
}
