import { initMiniBoards } from 'lib/view/miniBoard';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { mountBookmakerPreview, setBookmakerPreviewOrientation } from './interactionHandlers';

const bookmakerTextSelector = '.analyse__bookmaker-text';
const bookmakerPanelSelector = '.analyse__bookmaker';

function scheduleBoardResize(): void {
  requestAnimationFrame(() => requestAnimationFrame(dispatchChessgroundResize));
}

function hydrateBookmakerPanel($text: Cash, html: string, orientation: Color, showEval: boolean): void {
  setBookmakerPreviewOrientation(orientation);
  $text.html(html);
  syncBookmakerEvalDisplay(showEval);
  if (!html) return;

  initMiniBoards($text[0] as HTMLElement);
  mountBookmakerPreview($text[0] as HTMLElement);
}

export function syncBookmakerEvalDisplay(showEval: boolean): void {
  const $scope = $(bookmakerTextSelector);
  $scope.find('.bookmaker-content').toggleClass('bookmaker-hide-eval', !showEval);
  $scope
    .find('.bookmaker-score-toggle')
    .attr('aria-pressed', showEval ? 'true' : 'false')
    .text(showEval ? 'Eval: On' : 'Eval: Off');
}

export function renderBookmakerPanel(html: string, orientation: Color, showEval: boolean): void {
  $(bookmakerPanelSelector).toggleClass('empty', !html);
  const $text = $(bookmakerTextSelector);
  hydrateBookmakerPanel($text, html, orientation, showEval);
  scheduleBoardResize();
}

export function clearBookmakerPanel(): void {
  $(bookmakerPanelSelector).toggleClass('empty', true);
  scheduleBoardResize();
}

export function restoreBookmakerPanel(lastShownHtml: string, orientation: Color, showEval: boolean): void {
  const $text = $(bookmakerTextSelector);
  if (!$text.length) return;
  if ($text.html() || !lastShownHtml) return;
  $(bookmakerPanelSelector).toggleClass('empty', !lastShownHtml);
  hydrateBookmakerPanel($text, lastShownHtml, orientation, showEval);
  scheduleBoardResize();
}
