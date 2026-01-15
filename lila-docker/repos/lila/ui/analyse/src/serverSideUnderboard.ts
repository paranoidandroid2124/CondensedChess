import type AnalyseCtrl from './ctrl';

import { pubsub } from 'lib/pubsub';

export const stockfishName = 'Stockfish 17.1';

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  $(element).replaceWith(ctrl.opts.$underboard);
  const $panels = $('.analyse__underboard__panels > div'),
    $menu = $('.analyse__underboard__menu'),
    inputFen = document.querySelector<HTMLInputElement>('.analyse__underboard__fen input');
  let lastInputHash: string;


  pubsub.on('analysis.change', (fen: FEN, _) => {
    const nextInputHash = `${fen}${ctrl.bottomColor()}`;
    if (fen && nextInputHash !== lastInputHash) {
      if (inputFen) inputFen.value = fen;
      lastInputHash = nextInputHash;
    }
  });

  if (!site.blindMode) {
    pubsub.on('analysis.comp.toggle', (v: boolean) => {
      if (v) {
        setTimeout(() => $menu.find('.computer-analysis').first().trigger('mousedown'), 50);
      } else {
        $menu.find('span:not(.computer-analysis)').first().trigger('mousedown');
      }
    });
  }

  $panels.on('click', '.pgn', function (this: HTMLElement) {
    const selection = window.getSelection(),
      range = document.createRange();
    range.selectNodeContents(this);
    const currentlyUnselected = selection!.isCollapsed;
    selection!.removeAllRanges();
    if (currentlyUnselected) selection!.addRange(range);
  });
}
