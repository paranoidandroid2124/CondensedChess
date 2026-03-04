import type AnalyseCtrl from './ctrl';
import { baseUrl } from './view/util';
import * as licon from 'lib/licon';
import { url as xhrUrl, textRaw as xhrTextRaw } from 'lib/xhr';
import type { AnalyseData } from './interfaces';
import type { ChartGame, AcplChart } from 'chart';
import { spinnerHtml, domDialog, alert, confirm } from 'lib/view';
import { escapeHtml } from 'lib';
import { storage } from 'lib/storage';
import { pubsub } from 'lib/pubsub';

export const stockfishName = 'Stockfish 17.1';
const siteI18n = ((window as any).i18n?.site ?? {}) as Record<string, string | undefined>;
const t = (key: string, fallback: string): string => siteI18n[key] || fallback;

const chartFlagFromQuery = new URLSearchParams(location.search).get('evalChart');
const chartFlagFromStorage = (() => {
  try {
    return window.localStorage.getItem('chesstory.evalChart');
  } catch {
    return null;
  }
})();
// Client-side rollout guard until a server-driven user flag is wired through AnalyseData.
const chartsEnabled = chartFlagFromQuery === '1' || (chartFlagFromQuery !== '0' && chartFlagFromStorage !== '0');
const logChartError = (stage: string, err: unknown) => {
  console.warn('[analyse/chart]', stage, err);
};

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  $(element).replaceWith(ctrl.opts.$underboard);

  const data = ctrl.data;
  const $panels = $('.analyse__underboard__panels > div');
  const $menu = $('.analyse__underboard__menu');
  const inputFen = document.querySelector<HTMLInputElement>('.analyse__underboard__fen input');
  const positionGifLink =
    document.querySelector<HTMLAnchorElement>('.position-gif a') ||
    document.querySelector<HTMLAnchorElement>('a.position-gif');

  let lastInputHash: string | undefined;
  let advChart: AcplChart | undefined;
  let timeChartLoaded = false;

  const updateGifLinks = (fen: FEN) => {
    const ds = document.body.dataset;
    if (!positionGifLink) return;
    positionGifLink.href = xhrUrl(ds.assetUrl + '/export/fen.gif', {
      fen,
      color: ctrl.bottomColor(),
      lastMove: ctrl.node.uci,
      variant: ctrl.data.game.variant.key,
      theme: ds.board,
      piece: ds.pieceSet,
    });
  };

  const onFenChange = (fen: FEN) => {
    const nextInputHash = `${fen}${ctrl.bottomColor()}`;
    if (fen && nextInputHash !== lastInputHash) {
      if (inputFen) inputFen.value = fen;
      if (!site.blindMode) updateGifLinks(fen);
      lastInputHash = nextInputHash;
    }
  };

  onFenChange(ctrl.node.fen);
  pubsub.on('analysis.change', onFenChange);

  if (!site.blindMode) {
    pubsub.on('board.change', () => inputFen && updateGifLinks(inputFen.value));
    pubsub.on('analysis.server.progress', (d: AnalyseData) => {
      if (!advChart) startAdvantageChart();
      else advChart.updateData(d, ctrl.serverMainline());
      if (d.analysis && !d.analysis.partial) $('#acpl-chart-container-loader').remove();
    });
  }

  const chartLoader = () =>
    `<div id="acpl-chart-container-loader"><span>${stockfishName}<br>server analysis</span>${spinnerHtml}</div>`;

  function startAdvantageChart() {
    if (advChart || site.blindMode || !chartsEnabled) return;
    const loading = !ctrl.tree.root.eval || !Object.keys(ctrl.tree.root.eval).length;
    const $panel = $panels.filter('.computer-analysis');

    if (!$('#acpl-chart-container').length)
      $panel.html(
        '<div id="acpl-chart-container"><canvas id="acpl-chart"></canvas></div>' +
          (loading ? chartLoader() : ''),
      );
    else if (loading && !$('#acpl-chart-container-loader').length) $panel.append(chartLoader());

    site.asset
      .loadEsm<ChartGame>('chart.game')
      .then(m =>
        m.acpl($('#acpl-chart')[0] as HTMLCanvasElement, data, ctrl.serverMainline()).then(chart => {
          advChart = chart;
        }),
      )
      .catch(err => logChartError('acpl-init', err));
  }

  const store = storage.make('analysis.panel');
  const setPanel = function (panel: string) {
    $menu.children('.active').removeClass('active');
    $menu.find(`[data-panel="${panel}"]`).addClass('active');

    $panels
      .removeClass('active')
      .filter('.' + panel)
      .addClass('active');

    if ((panel === 'move-times' || ctrl.opts.hunter) && !timeChartLoaded && chartsEnabled)
      site.asset
        .loadEsm<ChartGame>('chart.game')
        .then(m => {
          $('#movetimes-chart').each(function (this: HTMLCanvasElement) {
            timeChartLoaded = true;
            m.movetime(this, data, ctrl.opts.hunter).catch(err => logChartError('movetime-init', err));
          });
        })
        .catch(err => logChartError('movetime-module', err));

    if ((panel === 'computer-analysis' || ctrl.opts.hunter) && $('#acpl-chart-container').length && chartsEnabled)
      setTimeout(startAdvantageChart, 200);
  };

  $menu.on('click', '[data-panel]', function (this: HTMLElement) {
    const panel = this.dataset.panel;
    if (!panel) return;
    store.set(panel);
    setPanel(panel);
  });

  const stored = store.get();
  const foundStored =
    stored &&
    $menu
      .children(`[data-panel="${stored}"]`)
      .filter(function (this: HTMLElement) {
        const display = window.getComputedStyle(this).display;
        return !!display && display !== 'none';
      }).length;

  if (foundStored) setPanel(stored);
  else {
    const $menuCt = $menu.children('[data-panel="ctable"]');
    ($menuCt.length ? $menuCt : $menu.children(':first-child')).trigger('click');
  }

  if (!data.analysis) {
    $panels.find('form.future-game-analysis').on('submit', function (this: HTMLFormElement) {
      if ($(this).hasClass('must-login')) {
        confirm(
          t('youNeedAnAccountToDoThat', 'You need an account to do that.'),
          t('signIn', 'Sign in'),
          t('cancel', 'Cancel'),
        ).then(yes => {
          if (yes) location.href = '/login?referrer=' + window.location.pathname;
        });
        return false;
      }

      xhrTextRaw(this.action, { method: this.method }).then(res => {
        if (res.ok) startAdvantageChart();
        else
          res.text().then(async t => {
            if (t && !t.startsWith('<!DOCTYPE html>')) await alert(t);
            site.reload();
          });
      });
      return false;
    });
  }

  $panels.on('click', '.pgn', function (this: HTMLElement) {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(this);
    const currentlyUnselected = selection!.isCollapsed;
    selection!.removeAllRanges();
    if (currentlyUnselected) selection!.addRange(range);
  });

  $panels.on('click', '.embed-howto', function (this: HTMLElement) {
    const url = `${baseUrl()}/embed/game/${data.game.id}?theme=auto&bg=auto${location.hash}`;
    const iframe = `<iframe src="${url}"\nwidth=600 height=397 frameborder=0></iframe>`;
    domDialog({
      modal: true,
      show: true,
      htmlText:
        '<div><strong style="font-size:1.5em">' +
        $(this).html() +
        '</strong><br /><br />' +
        '<pre>' +
        escapeHtml(iframe) +
        '</pre><br />' +
        iframe +
        '<br /><br />' +
        `<a class="text" data-icon="${licon.InfoCircle}" href="/developers#embed-game">Read more about embedding games</a></div>`,
    });
  });

  document.querySelector<HTMLAnchorElement>('a.game-gif')?.addEventListener('click', e => {
    e.preventDefault();
    site.asset.loadEsm('analyse.gifDialog', { init: ctrl });
  });
}
