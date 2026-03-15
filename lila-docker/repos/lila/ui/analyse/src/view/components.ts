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
        'analyse--bookmaker': !!ctrl.opts.bookmaker,
        'analyse--review-shell': ctrl.isReviewShell(),
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
        [icon(tool.icon as any), hl('span.label', tool.label)],
      ),
    ),
  );
}

type WorkspaceToolId = 'opening-explorer' | 'narrative' | 'action-menu';

type WorkspaceTool = {
  id: WorkspaceToolId;
  label: string;
  summary: string;
  icon: string;
  active: boolean;
  busy?: boolean;
  open: () => void;
};

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
      label: 'Insights',
      summary: ctrl.narrative.loading()
        ? 'Game Chronicle is running'
        : ctrl.narrative.data()
          ? 'Resume Game Chronicle insights'
          : 'Run Game Chronicle',
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
          ? 'Jump between openings, Game Chronicle, and board controls without leaving the move list.'
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
            hl('span.analyse__workspace-card-icon', [icon(tool.icon as any)]),
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
  const showCeval = ctrl.isCevalAllowed() && (ctrl.isReviewShell() ? ctrl.showEnginePanel() : ctrl.showCeval());
  if (ctrl.isReviewShell()) {
    return hl('div.analyse__tools.analyse__tools--review', [
      allowVideo && embeddedVideo,
      reviewView(ctrl, {
        cevalNode: showCeval ? cevalView.renderCeval(ctrl) : undefined,
        pvsNode: showCeval ? cevalView.renderPvs(ctrl) : undefined,
        moveListNode: renderMoveList(ctrl, concealOf),
        forkNode: forkView(ctrl, concealOf),
        explorerNode: renderExplorerPanel(ctrl, { force: true, closable: false }),
        boardSettingsNodes: boardSettingsView(ctrl, { closeOnChange: false, mode: 'workspace' }),
        importNode: renderInputs(ctrl),
      }),
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
  return hl('div.copyables.copyables--workspace', [
    hl('div.analyse-review__summary-grid.copyables__summary', [
      compactSummaryCard(pgnInspection.headline, 'import status'),
      compactSummaryCard(
        pgnInspection.preview ? `${pgnInspection.preview.plies} plies` : String(Math.max(1, pgnInspection.lines)),
        pgnInspection.preview ? 'incoming line' : 'draft lines',
      ),
      compactSummaryCard(fenInspection.headline, 'fen jump'),
    ]),
    hl('div.copyables__panel', [
      hl('div.pair', [
        hl('label.name', 'FEN'),
        hl('input.copyable', {
          attrs: { spellcheck: 'false', enterkeyhint: 'done' },
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              el.value = defined(ctrl.fenInput) ? ctrl.fenInput : ctrl.node.fen;
              el.addEventListener('change', () => {
                if (el.value !== ctrl.node.fen && el.reportValidity()) ctrl.changeFen(el.value.trim());
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
        !isMobile() &&
          hl(
            'button.button.button-thin.bottom-action.text',
            {
              attrs: pgnInspection.status !== 'ready' ? { disabled: true } : {},
              hook: bind('click', () => {
                if (pgnInspection.status !== 'ready') return;
                const pgn = $('.copyables textarea').val() as string;
                  if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.importPgn(pgn);
                }),
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
              title: 'Runs deeper on-device WASM scan; may take longer for full PGNs.',
            },
            hook: bind('click', () => {
              void ctrl.openNarrative();
            }),
          },
                [icon(licon.Book as any), ' Run Game Chronicle'],
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
      hl('a.copyables__recent-link', { attrs: { href: '/import' } }, 'Open import'),
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
