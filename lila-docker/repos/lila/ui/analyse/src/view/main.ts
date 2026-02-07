import * as licon from 'lib/licon';
import { type VNode, hl } from 'lib/view';
import { playable } from 'lib/game';
import * as router from 'lib/game/router';
import { render as trainingView } from './roundTraining';
import crazyView from '../crazy/crazyView';
import type AnalyseCtrl from '../ctrl';
import forecastView from '../forecast/forecastView';
import { view as keyboardView } from '../keyboard';
import { bookmakerToggleBox } from '../bookmaker';

import { viewContext, renderBoard, renderMain, renderTools, renderUnderboard } from './components';
import { displayColumns } from 'lib/device';
import { renderControls } from './controls';

let resizeCache: {
  columns: number;
  main: HTMLElement | null;
  board: HTMLElement | null;
  observer: ResizeObserver | null;
  raf: number | null;
  boardRectSig: string;
};

export default function () {
  return function (ctrl: AnalyseCtrl): VNode {
    resizeCache ??= resizeHandler(ctrl);
    return analyseView(ctrl);
  };
}

function analyseView(ctrl: AnalyseCtrl): VNode {
  bindLayoutObserver(ctrl);
  const ctx = viewContext(ctrl);
  return renderMain(
    ctx,
    ctrl.keyboardHelp && keyboardView(ctrl),
    renderSide(ctrl),
    renderBoard(ctx),
    crazyView(ctrl, ctrl.topColor(), 'top'),
    renderTools(ctx),
    crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
    renderControls(ctrl),
    renderUnderboard(ctx),
    trainingView(ctrl),
    ctrl.forecast && forecastView(ctrl, ctrl.forecast),
    !ctrl.synthetic &&
    playable(ctrl.data) &&
    hl(
      'div.back-to-game',
      hl(
        'a.button.button-empty.text',
        {
          attrs: {
            href: router.game(ctrl.data, ctrl.data.player.color),
            'data-icon': licon.Back,
          },
        },
        'Back to game',
      ),
    ),
  );
}

function renderSide(ctrl: AnalyseCtrl): VNode | undefined {
  if (!ctrl.opts.bookmaker) return;

  return hl('aside.analyse__side', {
    hook: {
      insert: () => bookmakerToggleBox(),
      update: () => bookmakerToggleBox(),
    },
  }, [
    hl(
      'fieldset.analyse__bookmaker.toggle-box.toggle-box--toggle.empty',
      {
        attrs: { id: 'bookmaker-field' },
      },
      [hl('legend', { attrs: { tabindex: '0' } }, 'Bookmaker'), hl('div.analyse__bookmaker-text')],
    ),
  ]);
}

function resizeHandler(ctrl: AnalyseCtrl) {
  window.addEventListener('resize', () => {
    scheduleBoardSync(ctrl);
    if (resizeCache.columns !== displayColumns()) ctrl.redraw();
    resizeCache.columns = displayColumns();
  });
  return {
    columns: displayColumns(),
    main: null,
    board: null,
    observer: null,
    raf: null,
    boardRectSig: '',
  };
}

function bindLayoutObserver(ctrl: AnalyseCtrl): void {
  const main = document.querySelector('main.analyse') as HTMLElement | null;
  if (!main || resizeCache.main === main) return;

  resizeCache.observer?.disconnect();
  resizeCache.main = main;
  resizeCache.board = main.querySelector('.analyse__board') as HTMLElement | null;
  resizeCache.boardRectSig = '';

  resizeCache.observer = new ResizeObserver(() => scheduleBoardSync(ctrl));
  resizeCache.observer.observe(main);
  if (resizeCache.board) resizeCache.observer.observe(resizeCache.board);

  scheduleBoardSync(ctrl);
}

function scheduleBoardSync(ctrl: AnalyseCtrl): void {
  if (resizeCache.raf !== null) cancelAnimationFrame(resizeCache.raf);
  resizeCache.raf = requestAnimationFrame(() => {
    resizeCache.raf = null;
    syncBoard(ctrl);
  });
}

function syncBoard(ctrl: AnalyseCtrl): void {
  if (!resizeCache.main) return;

  if (!resizeCache.board || !resizeCache.main.contains(resizeCache.board))
    resizeCache.board = resizeCache.main.querySelector('.analyse__board') as HTMLElement | null;
  const board = resizeCache.board;
  if (!board) return;
  resizeCache.observer?.observe(board);

  const rect = board.getBoundingClientRect();
  const sig = `${Math.round(rect.left)}:${Math.round(rect.top)}:${Math.round(rect.width)}:${Math.round(rect.height)}`;
  if (sig === resizeCache.boardRectSig) return;

  resizeCache.boardRectSig = sig;
  ctrl.withCg(cg => cg.redrawAll());
}
