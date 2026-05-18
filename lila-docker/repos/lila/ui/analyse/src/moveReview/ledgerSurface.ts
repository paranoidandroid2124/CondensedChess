import type { MoveReviewLedgerLineV1, MoveReviewStrategicLedgerV1 } from './responsePayload';
import { escapeHtml, normalizeSanToken, renderInteractiveSanChip } from './surfaceShared';

export type MoveReviewInteractiveRef = {
  refId: string;
  san: string;
  uci: string;
};

function formatScore(line: MoveReviewLedgerLineV1): string | null {
  if (typeof line.mate === 'number') return `mate ${line.mate}`;
  if (typeof line.scoreCp === 'number') return `${line.scoreCp >= 0 ? '+' : ''}${line.scoreCp}cp`;
  return null;
}

function renderLineMoves(
  line: MoveReviewLedgerLineV1,
  resolveRef: (san: string) => MoveReviewInteractiveRef | null,
): string {
  return line.sanMoves
    .map(san => {
      const normalized = normalizeSanToken(san);
      const ref = resolveRef(normalized);
      return renderInteractiveSanChip(san, ref, {
        interactiveClasses: 'move-chip move-chip--interactive',
        fallbackTag: 'code',
      });
    })
    .join(' ');
}

function renderLedgerLineRow(
  label: string,
  line: MoveReviewLedgerLineV1,
  resolveRef: (san: string) => MoveReviewInteractiveRef | null,
): string {
  const details = [
    line.title,
    formatScore(line),
    line.note || '',
  ].filter(Boolean);
  return `
    <div class="move-review-probe-summary__row move-review-probe-summary__row--ledger">
      <strong>${escapeHtml(label)}:</strong>
      <span>${escapeHtml(details.join(' · '))}</span>
      <span class="move-review-probe-summary__line">${renderLineMoves(line, resolveRef)}</span>
    </div>
  `;
}

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
    `<div class="move-review-strategic-summary__row"><strong>Motif:</strong> ${escapeHtml(ledger.motifLabel)}</div>`,
    `<div class="move-review-strategic-summary__row"><strong>Stage:</strong> ${escapeHtml([ledger.stageLabel, ledger.stageReason || ''].filter(Boolean).join(' · '))}</div>`,
    `<div class="move-review-strategic-summary__row"><strong>Carry-over:</strong> ${escapeHtml(ledger.carryOver ? 'Continuing plan state' : 'Fresh state')}</div>`,
  ];
  if (ledger.prerequisites.length)
    rows.push(
      `<div class="move-review-strategic-summary__row"><strong>Prereqs:</strong> ${escapeHtml(
        ledger.prerequisites.slice(0, 2).join(' · '),
      )}</div>`,
    );
  if (ledger.conversionTrigger)
    rows.push(
      `<div class="move-review-strategic-summary__row"><strong>Conversion:</strong> ${escapeHtml(
        ledger.conversionTrigger,
      )}</div>`,
    );
  return rows;
}

export function renderMoveReviewLedgerProbeRows(
  ledger: MoveReviewStrategicLedgerV1 | null,
  resolveRef: (san: string) => MoveReviewInteractiveRef | null,
): string[] {
  if (!ledger) return [];
  const rows: string[] = [];
  if (ledger.primaryLine) rows.push(renderLedgerLineRow('Plan line', ledger.primaryLine, resolveRef));
  if (ledger.resourceLine) rows.push(renderLedgerLineRow('Counter-resource', ledger.resourceLine, resolveRef));
  return rows;
}
