import type { MoveReviewStrategicLedgerV1 } from './responsePayload';
import { escapeHtml } from './surfaceShared';

export function moveReviewLedgerRootAttrs(
  ledger: MoveReviewStrategicLedgerV1 | null,
): Record<string, string> {
  if (!ledger) return {};
  return {
    'data-commentary-motif': ledger.motifKey,
    'data-commentary-stage': ledger.stageKey,
    'data-commentary-carry-over': String(ledger.carryOver),
  };
}

export function renderMoveReviewLedgerSignalRows(
  ledger: MoveReviewStrategicLedgerV1 | null,
): string[] {
  if (!ledger) return [];
  const rows = [
    `<div class="move-review-strategic-summary__row"><strong>Theme:</strong> ${escapeHtml(ledger.motifLabel)}</div>`,
    `<div class="move-review-strategic-summary__row"><strong>Phase:</strong> ${escapeHtml([ledger.stageLabel, ledger.stageReason || ''].filter(Boolean).join(' · '))}</div>`,
    `<div class="move-review-strategic-summary__row"><strong>Continuation:</strong> ${escapeHtml(ledger.carryOver ? 'Continuing plan' : 'New plan')}</div>`,
  ];
  if (ledger.prerequisites.length)
    rows.push(
      `<div class="move-review-strategic-summary__row"><strong>Requirements:</strong> ${escapeHtml(
        ledger.prerequisites.slice(0, 2).join(' · '),
      )}</div>`,
    );
  if (ledger.conversionTrigger)
    rows.push(
      `<div class="move-review-strategic-summary__row"><strong>Realization:</strong> ${escapeHtml(
        ledger.conversionTrigger,
      )}</div>`,
    );
  return rows;
}
