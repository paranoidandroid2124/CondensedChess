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
import { isMobile } from 'lib/device';
import * as materialView from 'lib/game/view/material';

import { view as actionMenu } from './actionMenu';
import retroView from '../retrospect/retroView';
import practiceView from '../practice/practiceView';
import explorerView from '../explorer/explorerView';
import { narrativeView } from '../narrative/narrativeView';
import { view as forkView } from '../fork';
import renderClocks from './clocks';
import * as control from '../control';
import * as chessground from '../ground';
import type AnalyseCtrl from '../ctrl';
import type { ConcealOf } from '../interfaces';
import * as pgnExport from '../pgnExport';
import { spinnerVdom as spinner, stepwiseScroll } from 'lib/view';
import * as Prefs from 'lib/prefs';
import statusView from 'lib/game/view/status';
import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import serverSideUnderboard from '../serverSideUnderboard';
import { renderPgnError } from '../pgnImport';
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
  const gaugeOn = false;
  return {
    ctrl,
    concealOf: makeConcealOf(ctrl),
    showCevalPvs: !ctrl.retro?.isSolving() && !ctrl.practice,
    gamebookPlayView: undefined,
    playerBars,
    playerStrips: renderPlayerStrips(ctrl),
    gaugeOn,
    needsInnerCoords: ctrl.data.pref.showCaptured || !!playerBars,
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
        'comp-off': !ctrl.showFishnetAnalysis(),
        'gauge-on': gaugeOn,
        'has-players': !!playerBars,
        'gamebook-play': !!gamebookPlayView,
        'analyse-hunter': ctrl.opts.hunter,
        'analyse--bookmaker': !!ctrl.opts.bookmaker,
      },
    },
    [renderSidebar(ctrl), ...kids],
  );
}

function renderSidebar(ctrl: AnalyseCtrl): VNode {
  return hl('div.analyse__sidebar', [
    hl('button.fbt', {
      attrs: {
        title: 'Opening explorer',
        'data-act': 'opening-explorer',
      },
      class: {
        active: ctrl.activeControlBarTool() === 'opening-explorer',
      },
    }, [icon(licon.Book as any)]),
    hl('button.fbt', {
      attrs: {
        title: 'Analysis Menu',
        'data-act': 'menu',
      },
      class: {
        active: ctrl.activeControlBarTool() === 'action-menu',
      },
    }, [icon(licon.Hamburger as any)]),
  ]);
}

export function renderTools({ ctrl, concealOf, allowVideo }: ViewContext, embeddedVideo?: LooseVNode) {
  const showCeval = ctrl.isCevalAllowed() && ctrl.showCeval();
  const narrativeEnabled = !!ctrl.narrative?.enabled();
  const narrativeNode = ctrl.narrative ? narrativeView(ctrl.narrative) : null;
  const activeTool = narrativeEnabled
    ? narrativeNode || retroView(ctrl) || explorerView(ctrl) || practiceView(ctrl)
    : retroView(ctrl) || explorerView(ctrl) || practiceView(ctrl) || narrativeNode;
  return hl('div.analyse__tools', [
    allowVideo && embeddedVideo,
    showCeval && cevalView.renderCeval(ctrl),
    showCeval && !ctrl.retro?.isSolving() && !ctrl.practice && cevalView.renderPvs(ctrl),
    renderMoveList(ctrl, concealOf),
    forkView(ctrl, concealOf),
    activeTool,
    ctrl.actionMenu() && actionMenu(ctrl),
  ]);
}

export function renderBoard({ ctrl, playerBars, playerStrips }: ViewContext, skipInfo = false) {
  return hl(
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
  );
}

export function renderUnderboard({ ctrl }: ViewContext) {
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
  return hl('div.copyables', [
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
    hl('div.pgn', [
      hl('div.pair', [
        hl('label.name', 'PGN'),
        hl('textarea.copyable', {
          attrs: { spellcheck: 'false' },
          class: { 'is-error': !!ctrl.pgnError },
          hook: {
            ...onInsert((el: HTMLTextAreaElement) => {
              el.value = defined(ctrl.pgnInput) ? ctrl.pgnInput : pgnExport.renderFullTxt(ctrl);
              const changePgnIfDifferent = () =>
                el.value !== pgnExport.renderFullTxt(ctrl) && ctrl.changePgn(el.value, true);

              el.addEventListener('input', () => (ctrl.pgnInput = el.value));

              el.addEventListener('keypress', (e: KeyboardEvent) => {
                if (e.key !== 'Enter' || e.shiftKey || e.ctrlKey || e.altKey || e.metaKey || isMobile())
                  return;
                else if (changePgnIfDifferent()) e.preventDefault();
              });
              if (isMobile()) el.addEventListener('focusout', changePgnIfDifferent);
            }),
            postpatch: (_, vnode) => {
              (vnode.elm as HTMLTextAreaElement).value = defined(ctrl.pgnInput)
                ? ctrl.pgnInput
                : pgnExport.renderFullTxt(ctrl);
            },
          },
        }),
        !isMobile() &&
        hl(
          'button.button.button-thin.bottom-item.bottom-action.text',
          {
            hook: bind('click', _ => {
              const pgn = $('.copyables .pgn textarea').val() as string;
              if (pgn !== pgnExport.renderFullTxt(ctrl)) ctrl.changePgn(pgn, true);
            }),
          },
          [icon(licon.PlayTriangle as any), ' Import PGN'],
        ),
        hl(
          'button.button.button-thin.bottom-item.bottom-action.text',
          {
            hook: bind('click', () => {
              ctrl.actionMenu(false);
              void ctrl.narrative?.openAndFetch();
            }),
          },
          [icon(licon.Book as any), ' Analyze Full Game'],
        ),
        hl(
          'div.bottom-item.bottom-error',
          { class: { 'is-error': !!ctrl.pgnError } },
          [icon(licon.CautionTriangle as any), renderPgnError(ctrl.pgnError)],
        ),
      ]),
    ]),
  ]);
}

export function renderResult(ctrl: AnalyseCtrl): VNode[] {
  const render = (result: string, status: string) => [
    hl('div.result', result),
    hl('div.status', status),
  ];
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
    !!ctrl.data.pref.showCaptured,
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
