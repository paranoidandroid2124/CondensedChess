import { view as cevalView, renderEval as normalizeEval } from 'lib/ceval';
import { parseFen } from 'chessops/fen';
import { defined } from 'lib';
import * as licon from 'lib/licon';
import {
  type VNode,
  type LooseVNode,
  type LooseVNodes,
  bind,
  bindNonPassive,
  onInsert,
  icon,
  hl,
} from 'lib/view';
import { playable } from 'lib/game';
import { displayColumns, isMobile } from 'lib/device';
import * as materialView from 'lib/game/view/material';
import { renderBoardPreview } from 'lib/view/boardPreview';

import { boardSettingsView, view as actionMenu } from './actionMenu';
import explorerView, { renderExplorerPanel } from '../explorer/explorerView';
import { narrativeView } from '../narrative/narrativeView';
import { view as forkView } from '../fork';
import { reviewView } from '../review/view';
import renderClocks from './clocks';
import * as control from '../control';
import * as chessground from '../ground';
import type AnalyseCtrl from '../ctrl';
import type { ConcealOf, ImportHistoryAccount, ImportHistoryAnalysis, ImportHistoryView } from '../interfaces';
import * as pgnExport from '../pgnExport';
import { spinnerVdom as spinner, stepwiseScroll } from 'lib/view';
import * as Prefs from 'lib/prefs';
import statusView from 'lib/game/view/status';
import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import serverSideUnderboard from '../serverSideUnderboard';
import pgnImport, { renderPgnError } from '../pgnImport';
import { normalizeInlinePgn } from '../pgnPipeline';
import { storage } from 'lib/storage';
import {
  notebookDossierOverviewKindLabel,
  notebookDossierProductLabel,
  parseNotebookDossier,
  type NotebookSectionV1,
  type SectionCardV1,
} from '../notebookDossier';

export interface ViewContext {
  ctrl: AnalyseCtrl;
  allowVideo?: boolean;
  concealOf?: ConcealOf;
  showCevalPvs: boolean;
  gamebookPlayView?: VNode;
  playerBars: VNode[] | undefined;
  playerStrips: [VNode, VNode] | undefined;
  gaugeOn: boolean;
  needsInnerCoords: boolean;
}

export function viewContext(ctrl: AnalyseCtrl): ViewContext {
  const playerBars = undefined;
  const gaugeOn = ctrl.showEvalGauge();
  return {
    ctrl,
    concealOf: makeConcealOf(ctrl),
    showCevalPvs: true,
    gamebookPlayView: undefined,
    playerBars,
    playerStrips: renderPlayerStrips(ctrl),
    gaugeOn,
    needsInnerCoords: ctrl.showCapturedMaterial() || !!playerBars,
  };
}

export function renderMain(ctx: ViewContext, ...kids: LooseVNodes[]): VNode {
  const { ctrl, playerBars, gaugeOn, gamebookPlayView, needsInnerCoords } = ctx;
  return hl(
    'main.analyse.variant-' + ctrl.data.game.variant.key,
    {
      attrs: {
        'data-active-tool': ctrl.activeControlBarTool(),
        'data-active-mode': ctrl.activeControlMode(),
      },
      hook: {
        insert: () => {
          forceInnerCoords(ctrl, needsInnerCoords);
        },
        update(_, _2) {
          forceInnerCoords(ctrl, needsInnerCoords);
        },
        postpatch(old, vnode) {
          if (old.data!.gaugeOn !== gaugeOn) dispatchChessgroundResize();
          vnode.data!.gaugeOn = gaugeOn;
        },
      },
      class: {
        'gauge-on': gaugeOn,
        'has-players': !!playerBars,
        'gamebook-play': !!gamebookPlayView,
        'analyse-hunter': ctrl.opts.hunter,
        'analyse--bookmaker': !!ctrl.opts.bookmaker && !ctrl.isReviewShell(),
        'analyse--review-shell': ctrl.isReviewShell(),
        'analyse--notebook': ctrl.isStudy(),
      },
    },
    [renderSidebar(ctrl), ...kids],
  );
}

function renderSidebar(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.isReviewShell()) return;
  return hl(
    'div.analyse__sidebar',
    workspaceTools(ctrl).map(tool =>
      hl(
        'button.fbt',
        {
          key: tool.id,
          attrs: {
            title: tool.summary,
            'data-act': tool.id,
          },
          hook: bind('click', tool.open, ctrl.redraw),
          class: {
            active: tool.active,
            busy: !!tool.busy,
          },
        },
        [renderWorkspaceToolIcon(ctrl, tool), hl('span.label', tool.label)],
      ),
    ),
  );
}

type WorkspaceToolId = 'opening-explorer' | 'narrative' | 'action-menu';
type NotebookGlyphKind = 'notebook' | 'bookmark' | 'page' | 'section';

type WorkspaceTool = {
  id: WorkspaceToolId;
  label: string;
  summary: string;
  icon: string;
  active: boolean;
  busy?: boolean;
  open: () => void;
};

function notebookGlyphNodes(kind: NotebookGlyphKind): VNode[] {
  switch (kind) {
    case 'bookmark':
      return [
        hl('path', { attrs: { d: 'M10 6.5h12a2 2 0 0 1 2 2v17l-8-4.8-8 4.8v-17a2 2 0 0 1 2-2Z' } }),
        hl('path', { attrs: { d: 'M12 11h8' } }),
        hl('path', { attrs: { d: 'M12 14.5h8' } }),
      ];
    case 'page':
      return [
        hl('path', { attrs: { d: 'M11 5.5h8.5L25 11v14.5a2 2 0 0 1-2 2H11a2 2 0 0 1-2-2v-18a2 2 0 0 1 2-2Z' } }),
        hl('path', { attrs: { d: 'M19.5 5.5V11H25' } }),
        hl('path', { attrs: { d: 'M12.5 15h9' } }),
        hl('path', { attrs: { d: 'M12.5 18.5h9' } }),
        hl('path', { attrs: { d: 'M12.5 22h6.5' } }),
      ];
    case 'section':
      return [
        hl('path', { attrs: { d: 'M8 9.5a2 2 0 0 1 2-2h9l5 5v11a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2v-14Z' } }),
        hl('path', { attrs: { d: 'M10.5 7.5v-2a2 2 0 0 1 2-2H21l3 3' } }),
        hl('path', { attrs: { d: 'M13 15h7' } }),
        hl('path', { attrs: { d: 'M13 18.5h7' } }),
        hl('path', { attrs: { d: 'M13 22h4.5' } }),
      ];
    default:
      return [
        hl('path', { attrs: { d: 'M8.5 7.5a2 2 0 0 1 2-2h10.5a3 3 0 0 1 3 3v15.5a2 2 0 0 1-2 2H11a2.5 2.5 0 0 1-2.5-2.5V7.5Z' } }),
        hl('path', { attrs: { d: 'M12 5.5v21' } }),
        hl('path', { attrs: { d: 'M15 11.5h6' } }),
        hl('path', { attrs: { d: 'M15 15h6' } }),
        hl('path', { attrs: { d: 'M15 18.5h4.5' } }),
        hl('path', { attrs: { d: 'M12 23.5c.7-1 1.7-1.5 3-1.5h9' } }),
      ];
  }
}

function renderNotebookGlyph(kind: NotebookGlyphKind, extraClass?: string): VNode {
  return hl(
    'span.notebook-glyph',
    {
      class: {
        [`notebook-glyph--${kind}`]: true,
        [extraClass || '']: !!extraClass,
      },
    },
    [
      hl(
        'svg',
        {
          attrs: {
            viewBox: '0 0 32 32',
            fill: 'none',
            stroke: 'currentColor',
            'stroke-width': '1.7',
            'stroke-linecap': 'round',
            'stroke-linejoin': 'round',
            'aria-hidden': 'true',
          },
        },
        notebookGlyphNodes(kind),
      ),
    ],
  );
}

function notebookGlyphForTool(tool: WorkspaceTool): NotebookGlyphKind {
  switch (tool.id) {
    case 'opening-explorer':
      return 'bookmark';
    case 'narrative':
      return 'page';
    default:
      return 'section';
  }
}

function renderWorkspaceToolIcon(ctrl: AnalyseCtrl, tool: WorkspaceTool): VNode {
  return ctrl.isStudy() ? renderNotebookGlyph(notebookGlyphForTool(tool), 'analyse__workspace-glyph') : icon(tool.icon as any);
}

function workspaceTools(ctrl: AnalyseCtrl): WorkspaceTool[] {
  const tools: WorkspaceTool[] = [
    {
      id: 'opening-explorer',
      label: 'Explorer',
      summary: 'Opening explorer and tablebase',
      icon: licon.Book,
      active: ctrl.activeControlBarTool() === 'opening-explorer',
      open: ctrl.toggleExplorer,
    },
  ];

  if (ctrl.narrative) {
    tools.push({
      id: 'narrative',
      label: 'Guided Review',
      summary: ctrl.narrative.loading()
        ? 'Guided review is running'
        : ctrl.narrative.data()
          ? 'Resume guided review'
          : 'Review this game',
      icon: licon.BubbleSpeech,
      active: ctrl.activeControlBarTool() === 'narrative',
      busy: ctrl.narrative.loading(),
      open: ctrl.toggleNarrative,
    });
  }

  tools.push({
    id: 'action-menu',
    label: 'Workbench',
    summary: 'Board, notation, and display controls',
    icon: licon.Hamburger,
    active: ctrl.activeControlBarTool() === 'action-menu',
    open: ctrl.toggleActionMenu,
  });

  return tools;
}

function renderWorkspaceDock(ctrl: AnalyseCtrl): VNode {
  const activeTool = ctrl.activeControlBarTool();
  const tools = workspaceTools(ctrl);
  return hl(`section.analyse__workspace-dock${activeTool ? '' : '.is-idle'}`, [
    hl('div.analyse__workspace-dock-head', [
      hl('strong', activeTool ? 'Switch workspace' : 'Open a workspace'),
      hl(
        'span',
        activeTool
          ? 'Jump between openings, guided review, and board controls without leaving the move list.'
          : 'Keep the move list anchored and open the tool you need beside it.',
      ),
    ]),
    hl(
      'div.analyse__workspace-dock-grid',
      tools.map(tool =>
        hl(
          'button.analyse__workspace-card',
          {
            key: tool.id,
            attrs: {
              type: 'button',
              title: tool.summary,
              'data-tool-id': tool.id,
            },
            hook: bind('click', tool.open, ctrl.redraw),
            class: {
              active: tool.active,
              busy: !!tool.busy,
            },
          },
          [
            hl('span.analyse__workspace-card-icon', [renderWorkspaceToolIcon(ctrl, tool)]),
            hl('span.analyse__workspace-card-copy', [
              hl('strong', tool.label),
              hl('span', tool.summary),
            ]),
            tool.busy ? hl('span.analyse__workspace-card-state', 'Running') : null,
          ],
        ),
      ),
    ),
  ]);
}

export function renderTools({ ctrl, concealOf, allowVideo }: ViewContext, embeddedVideo?: LooseVNode) {
  const showCeval = ctrl.isCevalAllowed() && (ctrl.isReviewShell() ? ctrl.reviewPrimaryTab() === 'engine' : ctrl.showCeval());
  if (ctrl.isReviewShell()) {
    const boardPreview = renderBoardPreview(
      { fen: ctrl.node.fen, uci: ctrl.node.uci },
      ctrl.getOrientation(),
      'div.analyse-review__mobile-board',
    );
    return hl('div.analyse__tools.analyse__tools--review', [
      hl(
        'section.analyse-review__mobile-board-rail',
        {
          hook: {
            insert: vnode => syncReviewMobileRailHeight(vnode.elm as HTMLElement),
            update: (_, vnode) => syncReviewMobileRailHeight(vnode.elm as HTMLElement),
          },
        },
        [
        hl('div.analyse-review__mobile-board-copy', [
          hl('strong', ctrl.reviewSurfaceMode() === 'review' ? 'Guided Review board' : 'Full Analysis board'),
          hl(
            'span',
            ctrl.node.ply > 0
              ? `Current position around move ${plyToTurn(ctrl.node.ply)}. This compact rail keeps the board in view while the tools pane scrolls.`
              : 'Start position. This compact rail keeps the board in view while the tools pane scrolls.',
          ),
        ]),
        boardPreview,
        ],
      ),
      allowVideo && embeddedVideo,
      hl('div.analyse-review__workspace-shell', [
        reviewView(ctrl, {
          cevalNode: showCeval ? cevalView.renderCeval(ctrl) : undefined,
          pvsNode: showCeval ? cevalView.renderPvs(ctrl) : undefined,
          moveListNode: renderMoveList(ctrl, concealOf),
          forkNode: forkView(ctrl, concealOf),
          explorerNode: renderExplorerPanel(ctrl, { force: true, closable: false }),
          boardSettingsNodes: boardSettingsView(ctrl, { closeOnChange: false, mode: 'workspace' }),
          importNode: renderInputs(ctrl),
        }),
      ]),
    ]);
  }
  const narrativeEnabled = !!ctrl.narrative?.enabled();
  const narrativeNode = ctrl.narrative ? narrativeView(ctrl.narrative) : null;
  const activeTool = narrativeEnabled
    ? narrativeNode || explorerView(ctrl)
    : explorerView(ctrl) || narrativeNode;
  return hl('div.analyse__tools', [
    allowVideo && embeddedVideo,
    showCeval && cevalView.renderCeval(ctrl),
    showCeval && cevalView.renderPvs(ctrl),
    renderMoveList(ctrl, concealOf),
    forkView(ctrl, concealOf),
    displayColumns() > 1 && renderWorkspaceDock(ctrl),
    activeTool,
    ctrl.actionMenu() && actionMenu(ctrl),
  ]);
}

function syncReviewMobileRailHeight(elm: HTMLElement): void {
  const shell = elm.closest('.analyse--review-shell') as HTMLElement | null;
  if (!shell) return;
  shell.style.setProperty('--review-mobile-rail-height', `${Math.ceil(elm.getBoundingClientRect().height)}px`);
}

export function renderBoard({ ctrl, playerBars, playerStrips, gaugeOn }: ViewContext, skipInfo = false) {
  return hl('div.analyse__board-wrap', [
    gaugeOn && cevalView.renderHorizontalGauge(ctrl),
    hl(
      'div.analyse__board.main-board',
      {
        hook:
          'ontouchstart' in window || !storage.boolean('scrollMoves').getOrDefault(true)
            ? undefined
            : bindNonPassive(
                'wheel',
                stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                  const target = e.target as HTMLElement;
                  if (
                    target.tagName !== 'PIECE' &&
                    target.tagName !== 'SQUARE' &&
                    target.tagName !== 'CG-BOARD'
                  )
                    return;
                  if (scroll) {
                    e.preventDefault();
                    if (e.deltaY > 0) control.next(ctrl);
                    else if (e.deltaY < 0) control.prev(ctrl);
                    ctrl.redraw();
                  }
                }),
              ),
      },
      [
        !skipInfo && playerStrips,
        !skipInfo && playerBars?.[ctrl.bottomIsWhite() ? 1 : 0],
        chessground.render(ctrl),
        !skipInfo && playerBars?.[ctrl.bottomIsWhite() ? 0 : 1],
        ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess'),
      ],
    ),
  ]);
}

export function renderUnderboard({ ctrl }: ViewContext) {
  if (ctrl.isReviewShell()) return;
  return hl(
    'div.analyse__underboard',
    {
      hook:
        ctrl.synthetic || playable(ctrl.data) ? undefined : onInsert(elm => serverSideUnderboard(elm, ctrl)),
    },
    [renderInputs(ctrl)],
  );
}

export function renderInputs(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.data.userAnalysis) return;
  if (ctrl.redirecting) return spinner();
  const currentPgn = pgnExport.renderFullTxt(ctrl);
  const currentInspection = inspectPgnDraft(currentPgn, currentPgn);
  const draftPgn = defined(ctrl.pgnInput) ? ctrl.pgnInput : currentPgn;
  const pgnInspection = inspectPgnDraft(draftPgn, currentPgn);
  const fenInspection = inspectFenDraft(defined(ctrl.fenInput) ? ctrl.fenInput : ctrl.node.fen, ctrl.node.fen);
  const recentDrafts = ctrl
    .recentImportDrafts()
    .filter(draft => draft !== pgnInspection.normalized && draft !== currentInspection.normalized);
  const serverHistory = ctrl.opts.importHistory;
  const submitPgnDraft = () => {
    if (pgnInspection.status !== 'ready') return;
    const draft = defined(ctrl.pgnInput) ? ctrl.pgnInput : pgnExport.renderFullTxt(ctrl);
    if (draft !== pgnExport.renderFullTxt(ctrl)) ctrl.importPgn(draft);
  };
  return hl('div.copyables.copyables--workspace', [
    hl('div.analyse-review__summary-grid.copyables__summary', [
      compactSummaryCard(pgnInspection.headline, 'import status'),
      compactSummaryCard(
        pgnInspection.preview ? `${pgnInspection.preview.plies} plies` : String(Math.max(1, pgnInspection.lines)),
        pgnInspection.preview ? 'incoming line' : 'draft lines',
      ),
      compactSummaryCard(fenInspection.headline, 'fen jump'),
    ]),
    ctrl.isStudy() ? renderStudyWorkspacePanel(ctrl) : renderStudyLaunchPanel(ctrl),
    hl('div.copyables__panel', [
      hl('div.pair', [
        hl('label.name', 'FEN'),
        hl('input.copyable', {
          attrs: { spellcheck: 'false', enterkeyhint: 'done' },
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              el.value = defined(ctrl.fenInput) ? ctrl.fenInput : ctrl.node.fen;
              const submitFen = () => {
                const nextFen = el.value.trim();
                if (nextFen === ctrl.node.fen || !parseFen(nextFen).isOk) return false;
                ctrl.changeFen(nextFen);
                return true;
              };
              el.addEventListener('change', () => {
                if (el.reportValidity()) submitFen();
              });
              el.addEventListener('keydown', (e: KeyboardEvent) => {
                if (e.key !== 'Enter' || e.shiftKey || e.ctrlKey || e.altKey || e.metaKey) return;
                if (el.reportValidity() && submitFen()) e.preventDefault();
              });
              el.addEventListener('input', () => {
                ctrl.fenInput = el.value;
                el.setCustomValidity(parseFen(el.value.trim()).isOk ? '' : 'Invalid FEN');
                requestAnimationFrame(ctrl.redraw);
              });
            },
            postpatch: (_, vnode) => {
              const el = vnode.elm as HTMLInputElement;
              if (!defined(ctrl.fenInput)) {
                el.value = ctrl.node.fen;
                el.setCustomValidity('');
              } else if (el.value !== ctrl.fenInput) el.value = ctrl.fenInput;
            },
          },
        }),
      ]),
      renderInlineStatus(fenInspection.headline, fenInspection.message, fenInspection.status === 'invalid'),
    ]),
    hl('div.copyables__panel.copyables__panel--pgn', [
      hl('div.pair', [
        hl('label.name', 'PGN'),
        hl('textarea.copyable', {
          attrs: { spellcheck: 'false' },
          class: { 'is-error': !!ctrl.pgnError || pgnInspection.status === 'invalid' },
          hook: {
            ...onInsert((el: HTMLTextAreaElement) => {
              el.value = defined(ctrl.pgnInput) ? ctrl.pgnInput : pgnExport.renderFullTxt(ctrl);
              const importPgnIfDifferent = () =>
                el.value !== pgnExport.renderFullTxt(ctrl) && ctrl.importPgn(el.value);

              el.addEventListener('input', () => {
                ctrl.pgnInput = el.value;
                ctrl.pgnError = '';
                requestAnimationFrame(ctrl.redraw);
              });

              el.addEventListener('keypress', (e: KeyboardEvent) => {
                if (e.key !== 'Enter' || e.shiftKey || e.ctrlKey || e.altKey || e.metaKey || isMobile()) return;
                else if (importPgnIfDifferent()) e.preventDefault();
              });
              if (isMobile()) el.addEventListener('focusout', importPgnIfDifferent);
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.pgnInput)
                ? ctrl.pgnInput
                : pgnExport.renderFullTxt(ctrl);
            },
          },
        }),
      ]),
      hl('div.bottom-item.bottom-actions', [
        hl(
          'button.button.button-thin.bottom-action.text',
          {
            attrs: pgnInspection.status !== 'ready' ? { disabled: true } : {},
            hook: bind('click', submitPgnDraft),
          },
          [icon(licon.PlayTriangle as any), ' Import PGN'],
        ),
        pgnInspection.status !== 'current' &&
          hl(
            'button.button.button-thin.bottom-action.text',
            {
              hook: bind('click', () => ctrl.resetImportDraft()),
            },
            [icon(licon.Reload as any), ' Reset draft'],
          ),
        hl(
          'button.button.button-thin.bottom-action.text',
          {
            attrs: {
              title:
                pgnInspection.status === 'ready'
                  ? 'Analyze the staged PGN without importing it first.'
                  : 'Runs deeper on-device WASM scan; may take longer for full PGNs.',
              disabled: pgnInspection.status === 'invalid',
            },
            hook: bind('click', () => {
              void ctrl.openNarrative(pgnInspection.status === 'ready' ? draftPgn : undefined);
            }),
          },
          [icon(licon.Book as any), ' Review this game'],
        ),
      ]),
      renderInlineStatus(
        pgnInspection.headline,
        pgnInspection.preview
          ? `${pgnInspection.message} ${pgnInspection.preview.variant} • ${pgnInspection.preview.plies} plies${
              pgnInspection.preview.opening ? ` • ${pgnInspection.preview.opening}` : ''
            }`
          : pgnInspection.message,
        pgnInspection.status === 'invalid',
      ),
      hl('div.bottom-item.bottom-error', { class: { 'is-error': !!ctrl.pgnError || pgnInspection.status === 'invalid' } }, [
        icon(licon.CautionTriangle as any),
        renderPgnError(ctrl.pgnError || pgnInspection.error),
      ]),
      renderImportPreview(currentInspection, pgnInspection),
      recentDrafts.length ? renderRecentImportDrafts(ctrl, recentDrafts) : null,
      serverHistory ? renderServerImportHistory(serverHistory) : null,
    ]),
  ]);
}

function renderNotebookPanelCover(title: string, subtitle: string, detail: string): VNode {
  return hl('div.copyables__study-cover', [
    hl('div.copyables__study-cover-spine'),
    hl('div.copyables__study-cover-pages', [
      hl('span.copyables__study-cover-page'),
      hl('span.copyables__study-cover-page.copyables__study-cover-page--mid'),
      hl('span.copyables__study-cover-page.copyables__study-cover-page--inner'),
    ]),
    hl('div.copyables__study-cover-face', [
      hl('div.copyables__study-cover-seal', [renderNotebookGlyph('notebook', 'copyables__study-cover-glyph')]),
      hl('span.copyables__study-cover-eyebrow', 'Chess notebook'),
      hl('strong.copyables__study-cover-title', title),
      hl('span.copyables__study-cover-subtitle', subtitle),
      hl('span.copyables__study-cover-detail', detail),
    ]),
  ]);
}

function renderDossierEvidenceLine(
  evidence: { strength: string; supportingGames: number; totalSampledGames: number },
  extra?: string,
): VNode {
  const parts = [`${evidence.supportingGames}/${evidence.totalSampledGames} games`, evidence.strength];
  if (extra) parts.push(extra);
  return hl('span.copyables__study-dossier-meta', parts.join(' • '));
}

function renderDossierTag(label: string): VNode {
  return hl('span.copyables__study-dossier-tag', label);
}

function renderDossierCard(card: SectionCardV1): VNode {
  switch (card.cardKind) {
    case 'opening_map':
      return hl('article.copyables__study-card.copyables__study-card--opening', [
        hl('div.copyables__study-card-head', [hl('strong', card.title), renderDossierEvidenceLine(card.evidence)]),
        hl('span.copyables__study-card-kicker', `${card.openingFamily} • ${card.side}`),
        hl('p.copyables__study-card-copy', card.story),
        hl('div.copyables__study-dossier-tags', card.structureLabels.map(renderDossierTag)),
      ]);
    case 'anchor_position':
      return hl('article.copyables__study-card.copyables__study-card--anchor', [
        hl('div.copyables__study-card-head', [hl('strong', card.title), renderDossierEvidenceLine(card.evidence, `ply ${card.moveContext.ply}`)]),
        hl('span.copyables__study-card-kicker', card.claim),
        hl('p.copyables__study-card-copy', card.explanation),
        hl('div.copyables__study-card-block', [
          hl('span.copyables__study-card-label', 'Question'),
          hl('p', card.questionPrompt),
        ]),
        hl('div.copyables__study-card-block', [
          hl('span.copyables__study-card-label', 'Recommended plan'),
          hl('strong', card.recommendedPlan.label),
          hl('p', card.recommendedPlan.summary),
          card.recommendedPlan.candidateMoves?.length
            ? hl(
                'div.copyables__study-dossier-tags',
                card.recommendedPlan.candidateMoves.map(move => renderDossierTag(move.san || move.uci || move.note || 'candidate')),
              )
            : null,
        ]),
        card.antiPattern
          ? hl('div.copyables__study-card-block', [
              hl('span.copyables__study-card-label', 'Avoid'),
              hl('strong', card.antiPattern.label),
              hl('p', card.antiPattern.summary),
            ])
          : null,
        card.strategicTags.length ? hl('div.copyables__study-dossier-tags', card.strategicTags.map(renderDossierTag)) : null,
      ]);
    case 'exemplar_game':
      return hl('article.copyables__study-card.copyables__study-card--example', [
        hl('div.copyables__study-card-head', [
          hl('strong', card.title),
          hl('span.copyables__study-dossier-meta', `${card.game.white} vs ${card.game.black} • ${card.game.result}`),
        ]),
        hl('span.copyables__study-card-kicker', card.whyItMatters),
        hl('p.copyables__study-card-copy', card.narrative),
        hl('div.copyables__study-card-block', [
          hl('span.copyables__study-card-label', 'Takeaway'),
          hl('p', card.takeaway),
        ]),
      ]);
    case 'action_item':
      return hl('article.copyables__study-card.copyables__study-card--action', [
        hl('div.copyables__study-card-head', [hl('strong', card.title)]),
        hl('p.copyables__study-card-copy', card.instruction),
        hl('div.copyables__study-card-block', [
          hl('span.copyables__study-card-label', 'Success marker'),
          hl('p', card.successMarker),
        ]),
      ]);
    case 'checklist':
      return hl('article.copyables__study-card.copyables__study-card--checklist', [
        hl('div.copyables__study-card-head', [hl('strong', card.title)]),
        hl(
          'div.copyables__study-checklist',
          card.items.map(item =>
            hl('div.copyables__study-checklist-item', [
              hl('span.copyables__study-checklist-priority', item.priority),
              hl('div', [hl('strong', item.label), item.reason ? hl('span', item.reason) : null]),
            ]),
          ),
        ),
      ]);
  }
}

function renderDossierSection(section: NotebookSectionV1): VNode {
  return hl(`section.copyables__study-section.copyables__study-section--${section.kind}`, [
    hl('div.copyables__study-section-head', [
      hl('div', [hl('strong', section.title), hl('span', section.summary)]),
      hl('span.copyables__study-dossier-meta', `${section.kind.replace(/_/g, ' ')} • ${section.status}`),
    ]),
    section.evidence ? renderDossierEvidenceLine(section.evidence) : null,
    hl('div.copyables__study-section-cards', section.cards.map(renderDossierCard)),
  ]);
}

function renderNotebookDossierSurface(raw: unknown): VNode | null {
  const dossier = parseNotebookDossier(raw);
  if (!dossier) return null;

  return hl('div.copyables__study-dossier', [
    hl('div.copyables__study-dossier-head', [
      hl('div.copyables__study-dossier-copy', [
        hl('span.copyables__study-eyebrow', notebookDossierProductLabel(dossier.productKind)),
        hl('strong', dossier.headline),
        hl('span.copyables__study-subline', dossier.summary),
      ]),
      hl('div.analyse-review__summary-grid.copyables__study-dossier-summary', [
        compactSummaryCard(`${dossier.source.sampledGameCount}`, 'sampled games'),
        compactSummaryCard(`${dossier.sections.length}`, 'sections'),
        compactSummaryCard(dossier.status, 'status'),
      ]),
    ]),
    hl(
      'div.copyables__study-overview-grid',
      dossier.overview.cards.map(card =>
        hl('article.copyables__study-overview-card', [
          hl('span.copyables__study-card-label', notebookDossierOverviewKindLabel(card.kind)),
          hl('strong', card.headline),
          hl('p', card.summary),
          renderDossierEvidenceLine(card.evidence),
          card.tags?.length ? hl('div.copyables__study-dossier-tags', card.tags.map(renderDossierTag)) : null,
        ]),
      ),
    ),
    hl('div.copyables__study-sections', dossier.sections.map(renderDossierSection)),
  ]);
}

function renderStudyWorkspacePanel(ctrl: AnalyseCtrl): VNode | null {
  const study = ctrl.studyData();
  if (!study) return null;

  const visibility = study.visibility || 'public';
  const currentUrl = ctrl.studyUrl();
  const notebookTarget =
    currentUrl && typeof window !== 'undefined'
      ? new URL(currentUrl, window.location.origin)
      : null;
  const notebookUrl =
    notebookTarget &&
    notebookTarget.pathname + notebookTarget.search === window.location.pathname + window.location.search
      ? null
      : currentUrl;
  const actionMessage = ctrl.studyActionMessageText();
  const syncTone = ctrl.studyWriteError ? 'error' : ctrl.isStudyWriting() ? 'info' : 'success';
  const syncMessage = actionMessage || ctrl.studyStatusText();

  return hl('section.copyables__study.copyables__study--current', [
    hl('div.copyables__study-head', [
      renderNotebookPanelCover(
        study.name,
        study.chapterName,
        `${study.chapters.length} ${study.chapters.length === 1 ? 'section' : 'sections'}`,
      ),
      hl('div.copyables__study-copy', [
        hl('span.copyables__study-eyebrow', 'Notebook'),
        hl('strong', study.name),
        hl(
          'span.copyables__study-subline',
          `${study.chapterName}${study.chapters.length > 1 ? ` • ${study.chapters.length} sections` : ''}`,
        ),
      ]),
      hl('div.copyables__study-actions', [
        notebookUrl
          ? hl(
              'a.button.button-thin.copyables__study-button',
              { attrs: { href: notebookUrl } },
              [renderNotebookGlyph('page', 'copyables__study-button-glyph'), ' Open notebook'],
            )
          : null,
        hl(
          'button.button.button-thin.copyables__study-button',
          {
            attrs: { type: 'button' },
            hook: bind('click', () => {
              void ctrl.copyStudyShareLink();
            }),
          },
          [renderNotebookGlyph('bookmark', 'copyables__study-button-glyph'), ' Copy notebook link'],
        ),
      ]),
    ]),
    hl('div.analyse-review__summary-grid.copyables__study-summary', [
      compactSummaryCard(study.canWrite ? 'Editable' : 'Read only', 'access'),
      compactSummaryCard(visibility, 'visibility'),
      compactSummaryCard(`${study.chapters.length}`, 'sections'),
      compactSummaryCard(ctrl.isStudyWriting() ? 'Saving' : 'Ready', 'sync'),
    ]),
    renderStudyStatusCard(syncMessage, actionMessage ? ctrl.studyActionToneValue() : syncTone),
    hl('div.copyables__study-pills', [
      studyFeaturePill('page', `${study.chapterName} open`),
      studyFeaturePill('section', 'Use the section navigator above'),
      studyFeaturePill('bookmark', 'Share the exact section link'),
    ]),
    renderNotebookDossierSurface(study.notebookDossier),
  ]);
}

function renderStudyLaunchPanel(ctrl: AnalyseCtrl): VNode {
  const busy = ctrl.studyCreateBusy();
  const needsAuth = ctrl.studyNeedsAuth();
  const transferCount = ctrl.studyTransferCountValue();
  const narrativeReady = ctrl.hasNarrativeStudyBrief();
  const error = ctrl.studyCreateErrorText();

  return hl('section.copyables__study.copyables__study--launch', [
    hl('div.copyables__study-head', [
      renderNotebookPanelCover(
        'Untitled notebook',
        'First section from analysis',
        narrativeReady ? 'Guided review brief ready' : 'Add commentary as you go',
      ),
      hl('div.copyables__study-copy', [
        hl('span.copyables__study-eyebrow', 'Research notebook'),
        hl('strong', 'Turn this analysis into a notebook'),
        hl(
          'span.copyables__study-subline',
          'Create a section-based research notebook, keep annotating key moves, and share the result like a chess book in progress.',
        ),
      ]),
      hl('div.copyables__study-actions', [
        needsAuth
          ? hl(
              'a.button.copyables__study-button',
              { attrs: { href: ctrl.studyLoginHref() } },
              [renderNotebookGlyph('bookmark', 'copyables__study-button-glyph'), ' Sign in to create'],
            )
          : hl(
              'button.button.copyables__study-button',
              {
                attrs: busy ? { type: 'button', disabled: true } : { type: 'button' },
                hook: busy
                  ? undefined
                  : bind('click', () => {
                    void ctrl.createStudyFromCurrentAnalysis();
                  }),
              },
              [
                renderNotebookGlyph('notebook', 'copyables__study-button-glyph'),
                busy ? ' Creating notebook...' : ' Create notebook from analysis',
              ],
            ),
      ]),
    ]),
    hl('div.analyse-review__summary-grid.copyables__study-summary', [
      compactSummaryCard('PGN + move tree', 'base'),
      compactSummaryCard('Saved explanations', 'saved lines'),
      compactSummaryCard(narrativeReady ? 'Guided review brief included' : 'Guided review brief optional', 'section intro'),
    ]),
    renderStudyStatusCard(
      busy
        ? transferCount > 0
          ? `Creating the new notebook and moving ${transferCount} saved explanation${transferCount === 1 ? '' : 's'}.`
          : 'Creating the new notebook shell from the current PGN.'
        : 'Saved move explanations that already exist in this shell will be carried into the new notebook when possible.',
      busy ? 'info' : 'success',
    ),
    error ? renderStudyStatusCard(error, 'error') : null,
    hl('div.copyables__study-pills', [
      studyFeaturePill('page', 'Move-by-move notes'),
      studyFeaturePill('section', 'Branches stay explorable'),
      studyFeaturePill('bookmark', 'Shareable section URL'),
      narrativeReady ? studyFeaturePill('notebook', 'Guided review brief') : null,
    ]),
  ]);
}

type FenDraftInspection = {
  status: 'current' | 'ready' | 'invalid';
  headline: string;
  message: string;
};

type PgnDraftInspection = {
  status: 'empty' | 'current' | 'ready' | 'invalid';
  headline: string;
  message: string;
  chars: number;
  lines: number;
  normalized?: string;
  error?: string;
  preview?: {
    variant: string;
    plies: number;
    opening?: string;
  };
};

let lastPgnInspection: { draft: string; current: string; result: PgnDraftInspection } | undefined;

function inspectFenDraft(draft: string, currentFen: string): FenDraftInspection {
  const trimmed = draft.trim();
  if (!trimmed || trimmed === currentFen) {
    return {
      status: 'current',
      headline: 'Current position',
      message: 'Edit the FEN and press Enter to reopen analysis from a new position.',
    };
  }
  if (!parseFen(trimmed).isOk) {
    return {
      status: 'invalid',
      headline: 'FEN needs fixes',
      message: 'The jump is blocked until the FEN parses cleanly.',
    };
  }
  return {
    status: 'ready',
    headline: 'Ready to jump',
    message: 'Press Enter to relaunch analysis from this board state.',
  };
}

function inspectPgnDraft(draft: string, currentPgn: string): PgnDraftInspection {
  if (lastPgnInspection?.draft === draft && lastPgnInspection.current === currentPgn) return lastPgnInspection.result;
  const normalized = normalizeInlinePgn(draft);
  const normalizedCurrent = normalizeInlinePgn(currentPgn);
  const lines = draft ? draft.split(/\r?\n/).length : 0;
  const chars = draft.trim().length;
  let result: PgnDraftInspection;
  if (!normalized) {
    result = {
      status: 'empty',
      headline: 'Draft empty',
      message: 'Paste a PGN to stage a new game import.',
      chars,
      lines,
    };
  } else {
    try {
      const imported = pgnImport(normalized);
      const game = imported.game;
      const plies = Math.max(0, (game?.turns || imported.treeParts?.length || 1) - 1);
      const preview = {
        variant: game?.variant?.name || 'Chess',
        plies,
        opening: game?.opening?.name,
      };
      result =
        normalizedCurrent === normalized
          ? {
              status: 'current',
              headline: 'Current board snapshot',
              message: 'The draft matches the PGN already loaded in this shell.',
              chars,
              lines,
              normalized,
              preview,
            }
          : {
              status: 'ready',
              headline: 'Ready to import',
              message: 'Import will replace the current analysis tree with this PGN.',
              chars,
              lines,
              normalized,
              preview,
            };
    } catch (err) {
      result = {
        status: 'invalid',
        headline: 'PGN needs fixes',
        message: 'Import is blocked until the draft parses cleanly.',
        chars,
        lines,
        normalized,
        error: (err as Error).message,
      };
    }
  }
  lastPgnInspection = { draft, current: currentPgn, result };
  return result;
}

function renderInlineStatus(headline: string, message: string, error = false): VNode {
  return hl(`div.bottom-item.bottom-status.copyables__status${error ? '.is-error' : ''}`, [
    hl('strong', headline),
    hl('span', message),
  ]);
}

function compactSummaryCard(value: string, label: string): VNode {
  return hl('div.analyse-review__summary-card', [hl('strong', value), hl('span', label)]);
}

function renderStudyStatusCard(
  message: string,
  tone: 'info' | 'success' | 'error',
): VNode {
  return hl(`div.copyables__study-status.copyables__study-status--${tone}`, [
    hl('strong', tone === 'error' ? 'Notebook issue' : tone === 'info' ? 'Notebook in progress' : 'Notebook ready'),
    hl('span', message),
  ]);
}

function studyFeaturePill(kind: NotebookGlyphKind, label: string): VNode | null {
  return label ? hl('span.copyables__study-pill', [renderNotebookGlyph(kind, 'copyables__study-pill-glyph'), hl('span', label)]) : null;
}

function renderImportPreview(current: PgnDraftInspection, incoming: PgnDraftInspection): VNode {
  return hl('div.copyables__preview', [
    hl('div.copyables__preview-card', [
      hl('span.copyables__preview-label', 'Current'),
      hl('strong', current.preview?.opening || current.preview?.variant || 'Current analysis'),
      hl('span', `${current.preview?.plies || 0} plies • ${current.chars} chars`),
    ]),
    hl('div.copyables__preview-card', [
      hl('span.copyables__preview-label', 'Incoming'),
      hl(
        'strong',
        incoming.preview?.opening || incoming.preview?.variant || (incoming.status === 'invalid' ? 'PGN needs fixes' : 'Awaiting draft'),
      ),
      hl(
        'span',
        incoming.preview
          ? `${incoming.preview.plies} plies • ${incoming.chars} chars`
          : `${incoming.lines} lines • ${incoming.chars} chars`,
      ),
    ]),
  ]);
}

function renderRecentImportDrafts(ctrl: AnalyseCtrl, drafts: string[]): VNode {
  return hl('div.copyables__recent', [
    hl('div.copyables__recent-head', [hl('strong', 'Recent drafts'), hl('span', 'Session-only drafts you already imported.')]),
    hl(
      'div.copyables__recent-list',
      drafts.map((draft, index) => {
        const inspection = inspectPgnDraft(draft, '');
        return hl(
          'button.copyables__recent-item',
          {
            key: draft.slice(0, 40) + index,
            attrs: { type: 'button' },
            hook: bind('click', () => ctrl.useImportDraft(draft)),
          },
          [
            hl('strong', inspection.preview?.opening || inspection.preview?.variant || `Draft ${index + 1}`),
            hl(
              'span',
              inspection.preview
                ? `${inspection.preview.plies} plies • ${inspection.chars} chars`
                : `${inspection.lines} lines • ${inspection.chars} chars`,
            ),
          ],
        );
      }),
    ),
  ]);
}

function renderServerImportHistory(history: ImportHistoryView): VNode | undefined {
  const hasAnalyses = !!history.recentAnalyses?.length;
  const hasAccounts = !!history.recentAccounts?.length;
  if (!hasAnalyses && !hasAccounts) return renderEmptySavedHistory();
  const blocks: VNode[] = [];
  if (hasAnalyses) {
    blocks.push(
      hl('div.copyables__recent', [
        hl('div.copyables__recent-head', [
          hl('strong', 'Recent analyses'),
          hl('span', 'Server-saved imports you can reopen from any signed-in device.'),
        ]),
        hl(
          'div.copyables__recent-list',
          history.recentAnalyses.map((entry, index) =>
            renderSavedAnalysisEntry(entry, history.currentAnalysisId === entry.id, index === 0),
          ),
        ),
      ]),
    );
  }
  if (hasAccounts) {
    blocks.push(
      hl('div.copyables__recent', [
        hl('div.copyables__recent-head', [
          hl('strong', 'Recent accounts'),
          hl('span', 'Jump back into Lichess or Chess.com imports without retyping usernames.'),
        ]),
        hl(
          'div.copyables__recent-list',
          history.recentAccounts.map((entry, index) => renderSavedAccountEntry(entry, index === 0)),
        ),
      ]),
    );
  }
  return blocks.length ? hl('div.copyables__history-stack', blocks) : undefined;
}

function renderEmptySavedHistory(): VNode {
  return hl('div.copyables__recent.copyables__recent--empty', [
    hl('div.copyables__recent-head', [
      hl('strong', 'No saved imports yet'),
      hl('span', 'Analyze a PGN or imported game once, and it will stay here for quick reopen on any signed-in device.'),
    ]),
    hl('div.copyables__empty-actions', [
      hl('a.copyables__recent-link', { attrs: { href: '/import' } }, 'Open recent games'),
      hl('span.copyables__recent-link-note', 'or paste a PGN below to start a reusable history.'),
    ]),
  ]);
}

function renderSavedAnalysisEntry(entry: ImportHistoryAnalysis, current: boolean, priority: boolean): VNode {
  const supportLine = analysisSupportLine(entry);
  return hl(
    'a.copyables__recent-item',
    {
      key: entry.id,
      attrs: { href: entry.href },
      class: { 'is-current': current, 'is-priority': priority },
    },
    [
      hl('div.copyables__recent-kicker', [
        entry.providerLabel
          ? renderHistoryBadge(entry.providerLabel, 'copyables__badge--provider', `copyables__badge--${providerTone(entry.provider)}`)
          : null,
        renderHistoryBadge(
          sourceTypeLabel(entry.sourceType),
          `copyables__badge--${sourceTypeTone(entry.sourceType)}`,
        ),
        current
          ? renderHistoryBadge('Current', 'copyables__badge--current')
          : priority
            ? renderHistoryBadge('Latest', 'copyables__badge--priority')
            : null,
      ]),
      hl('div.copyables__recent-body', [
        hl('div.copyables__recent-title-row', [
          hl('strong', current ? `${entry.title} (Current)` : entry.title),
          hl('span.copyables__recent-cta', current ? 'On board' : priority ? 'Resume latest' : 'Resume'),
        ]),
        hl('span.copyables__recent-subline', analysisMetaLine(entry)),
        supportLine ? hl('span.copyables__recent-foot', supportLine) : null,
        hl('span.copyables__meta', `${current ? 'Opened and active' : 'Opened'} ${formatImportTimestamp(entry.openedAt)}`),
      ]),
    ],
  );
}

function renderSavedAccountEntry(entry: ImportHistoryAccount, priority: boolean): VNode {
  return hl(
    'a.copyables__recent-item',
    {
      key: `${entry.provider}:${entry.username}`,
      attrs: { href: entry.href },
      class: { 'is-priority': priority },
    },
    [
      hl('div.copyables__recent-kicker', [
        renderHistoryBadge(entry.providerLabel, 'copyables__badge--provider', `copyables__badge--${providerTone(entry.provider)}`),
        priority ? renderHistoryBadge('Latest', 'copyables__badge--priority') : null,
        entry.lastAnalysedAt ? renderHistoryBadge('Analysed', 'copyables__badge--activity') : null,
      ]),
      hl('div.copyables__recent-body', [
        hl('div.copyables__recent-title-row', [
          hl('strong', `@${entry.username}`),
          hl('span.copyables__recent-cta', priority ? 'Open latest' : 'Open'),
        ]),
        hl('span.copyables__recent-subline', `${entry.analysisCount} saved analyses`),
        hl(
          'span.copyables__recent-foot',
          priority
            ? 'Fastest way back to your most recently imported account list.'
            : 'Reopen this account list without retyping the username.',
        ),
        hl('span.copyables__meta', `Active ${formatImportTimestamp(entry.activityAt)}`),
      ]),
    ],
  );
}

function analysisMetaLine(entry: ImportHistoryAnalysis): string {
  const line = [
    entry.username ? `@${entry.username}` : undefined,
    entry.playedAtLabel && entry.playedAtLabel !== '-' ? entry.playedAtLabel : undefined,
    entry.result,
    entry.speed && entry.speed !== '-' ? entry.speed : undefined,
  ]
    .filter(Boolean)
    .join(' • ');
  return line || 'Saved PGN snapshot ready to reopen.';
}

function analysisSupportLine(entry: ImportHistoryAnalysis): string | undefined {
  const line = [
    entry.opening,
    entry.variant && entry.variant !== entry.opening ? entry.variant : undefined,
    entry.sourceType === 'manual' ? 'Manual PGN import' : 'Imported game snapshot',
  ]
    .filter(Boolean)
    .join(' • ');
  return line || undefined;
}

function formatImportTimestamp(value: string | undefined): string {
  if (!value) return 'recently';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function renderHistoryBadge(label: string, ...classes: string[]): VNode {
  const suffix = classes.filter(Boolean).map(cls => `.${cls}`).join('');
  return hl(`span.copyables__badge${suffix}`, label);
}

function providerTone(provider: string | undefined): string {
  return provider === 'chesscom' ? 'chesscom' : 'lichess';
}

function sourceTypeLabel(sourceType: string): string {
  return sourceType === 'manual' ? 'Manual PGN' : 'Imported game';
}

function sourceTypeTone(sourceType: string): string {
  return sourceType === 'manual' ? 'manual' : 'imported';
}

export function renderResult(ctrl: AnalyseCtrl): VNode[] {
  const render = (result: string, status: string) => [hl('div.result', result), hl('div.status', status)];
  if (ctrl.data.game.status.id >= 30) {
    const winner = ctrl.data.game.winner;
    const result = winner === 'white' ? '1-0' : winner === 'black' ? '0-1' : '½-½';
    return render(result, statusView(ctrl.data));
  }
  return [];
}

export const renderIndexAndMove = (node: Tree.Node, withEval: boolean, withGlyphs: boolean): LooseVNodes =>
  node.san ? [renderIndex(node.ply, true), renderMoveNodes(node, withEval, withGlyphs)] : undefined;

export const renderIndex = (ply: Ply, withDots: boolean): VNode =>
  hl(`index.sbhint${ply}`, plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : ''));

export function renderMoveNodes(
  node: Tree.Node,
  withEval: boolean,
  withGlyphs: boolean,
  ev?: Tree.ClientEval | Tree.ServerEval | false,
): LooseVNodes {
  ev ??= node.ceval ?? node.eval; // ev = false will override withEval
  const evalText = !ev
    ? ''
    : ev?.cp !== undefined
      ? normalizeEval(ev.cp)
      : ev?.mate !== undefined
        ? `#${ev.mate}`
        : '';
  return [
    hl('san', fixCrazySan(node.san!)),
    withGlyphs && node.glyphs?.map(g => hl('glyph', { attrs: { title: g.name } }, g.symbol)),
    withEval && !!node.shapes?.length && hl('shapes'),
    withEval && evalText && hl('eval', evalText.replace('-', '−')),
  ];
}

const renderMoveList = (ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode =>
  hl('div.analyse__moves.areplay', { hook: ctrl.treeView.hook() }, [
    hl('div', [ctrl.treeView.render(concealOf), renderResult(ctrl)]),
  ]);

export const renderMaterialDiffs = (ctrl: AnalyseCtrl): [VNode, VNode] =>
  materialView.renderMaterialDiffs(
    ctrl.showCapturedMaterial(),
    ctrl.bottomColor(),
    ctrl.node.fen,
    !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
    ctrl.nodeList,
    ctrl.node.ply,
  );

function makeConcealOf(_: AnalyseCtrl): ConcealOf | undefined {
  return undefined;
}

let prevForceInnerCoords: boolean;
function forceInnerCoords(ctrl: AnalyseCtrl, v: boolean) {
  if (ctrl.data.pref.coords === Prefs.Coords.Outside) {
    if (prevForceInnerCoords !== v) {
      prevForceInnerCoords = v;
      $('body').toggleClass('coords-in', v).toggleClass('coords-out', !v);
    }
  }
}

function renderPlayerStrips(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const renderPlayerStrip = (cls: string, materialDiff: VNode, clock?: VNode): VNode =>
    hl('div.analyse__player_strip.' + cls, [materialDiff, clock]);

  const clocks = renderClocks(ctrl, ctrl.path),
    whitePov = ctrl.bottomIsWhite(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return [
    renderPlayerStrip('top', materialDiffs[0], clocks?.[whitePov ? 1 : 0]),
    renderPlayerStrip('bottom', materialDiffs[1], clocks?.[whitePov ? 0 : 1]),
  ];
}
