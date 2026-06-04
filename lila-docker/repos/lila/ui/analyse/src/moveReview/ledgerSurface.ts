import type { MoveReviewStrategicLedgerV1 } from './responsePayload';

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
