import { initMiniBoards } from 'lib/view/miniBoard';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { mountMoveReviewPreview, setMoveReviewPreviewOrientation } from './interactionHandlers';

const moveReviewTextSelector = '.analyse__move-review-text';
const moveReviewPanelSelector = '.analyse__move-review';

function scheduleBoardResize(): void {
  requestAnimationFrame(() => requestAnimationFrame(dispatchChessgroundResize));
}

function hydrateMoveReviewPanel($text: Cash, html: string, orientation: Color, showEval: boolean): void {
  setMoveReviewPreviewOrientation(orientation);
  $text.html(html);
  syncMoveReviewEvalDisplay(showEval);
  if (!html) return;

  initMiniBoards($text[0] as HTMLElement);
  mountMoveReviewPreview($text[0] as HTMLElement);
}

export function syncMoveReviewEvalDisplay(showEval: boolean): void {
  const $scope = $(moveReviewTextSelector);
  $scope.find('.move-review-content').toggleClass('move-review-hide-eval', !showEval);
  $scope
    .find('.move-review-score-toggle')
    .attr('aria-pressed', showEval ? 'true' : 'false')
    .text(showEval ? 'Eval: On' : 'Eval: Off');
}

export function renderMoveReviewPanel(html: string, orientation: Color, showEval: boolean): void {
  $(moveReviewPanelSelector).toggleClass('empty', !html);
  const $text = $(moveReviewTextSelector);
  hydrateMoveReviewPanel($text, html, orientation, showEval);
  scheduleBoardResize();
}

export function clearMoveReviewPanel(): void {
  $(moveReviewPanelSelector).toggleClass('empty', true);
  scheduleBoardResize();
}

export function restoreMoveReviewPanel(lastShownHtml: string, orientation: Color, showEval: boolean): void {
  const $text = $(moveReviewTextSelector);
  if (!$text.length) return;
  if ($text.html() || !lastShownHtml) return;
  $(moveReviewPanelSelector).toggleClass('empty', !lastShownHtml);
  hydrateMoveReviewPanel($text, lastShownHtml, orientation, showEval);
  scheduleBoardResize();
}
