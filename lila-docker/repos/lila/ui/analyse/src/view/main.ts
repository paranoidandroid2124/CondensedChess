import { view as cevalView } from 'lib/ceval';
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
  chat: HTMLElement | null;
  board: HTMLElement | null;
  meta: HTMLElement | null;
};

export default function () {
  return function (ctrl: AnalyseCtrl): VNode {
    resizeCache ??= resizeHandler(ctrl);
    return analyseView(ctrl);
  };
}

function analyseView(ctrl: AnalyseCtrl): VNode {
  const ctx = viewContext(ctrl);
  return renderMain(
    ctx,
    ctrl.keyboardHelp && keyboardView(ctrl),
    renderSide(ctrl),
    renderBoard(ctx),
    ctx.gaugeOn && cevalView.renderGauge(ctrl),
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
    if (resizeCache.columns !== displayColumns()) ctrl.redraw();
    resizeCache.columns = displayColumns();
  });
  return { columns: displayColumns(), chat: null, board: null, meta: null };
}
