import { initMiniBoards } from 'lib/view/miniBoard';
import { mountBookmakerPreview, setBookmakerPreviewOrientation } from './interactionHandlers';

const bookmakerTextSelector = '.analyse__bookmaker-text';
const bookmakerPanelSelector = '.analyse__bookmaker';

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
  setBookmakerPreviewOrientation(orientation);
  $text.html(html);
  syncBookmakerEvalDisplay(showEval);
  if (!html) return;

  initMiniBoards($text[0] as HTMLElement);
  mountBookmakerPreview($text[0] as HTMLElement);
}

export function clearBookmakerPanel(): void {
  $(bookmakerPanelSelector).toggleClass('empty', true);
}

export function restoreBookmakerPanel(lastShownHtml: string, showEval: boolean): void {
  const $text = $(bookmakerTextSelector);
  if (!$text.length) return;
  if ($text.html() || !lastShownHtml) return;
  $(bookmakerPanelSelector).toggleClass('empty', !lastShownHtml);
  $text.html(lastShownHtml);
  syncBookmakerEvalDisplay(showEval);
}
