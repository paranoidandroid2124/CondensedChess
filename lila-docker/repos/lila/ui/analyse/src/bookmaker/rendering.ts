import { initMiniBoards } from 'lib/view/miniBoard';
import { renderCreditWidget } from '../CreditWidget';
import type { CreditStatus } from '../CreditWidget';
import { mountBookmakerPreview, setBookmakerPreviewOrientation } from './interactionHandlers';

const bookmakerTextSelector = '.analyse__bookmaker-text';
const bookmakerPanelSelector = '.analyse__bookmaker';
const creditContainerSelector = `${bookmakerPanelSelector} .llm-credit-widget-container`;

function ensureCreditContainer(): void {
  if (!$(creditContainerSelector).length) {
    $(bookmakerPanelSelector).prepend('<div class="llm-credit-widget-container"></div>');
  }
}

async function refreshCreditStatus(): Promise<void> {
  try {
    const res = await fetch('/api/llm/credits');
    if (!res.ok) return;
    const status = (await res.json()) as CreditStatus;
    $(creditContainerSelector).html(renderCreditWidget(status));
  } catch {}
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
  setBookmakerPreviewOrientation(orientation);
  ensureCreditContainer();
  $text.html(html);
  syncBookmakerEvalDisplay(showEval);
  if (!html) return;

  initMiniBoards($text[0] as HTMLElement);
  mountBookmakerPreview($text[0] as HTMLElement);
  void refreshCreditStatus();
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
