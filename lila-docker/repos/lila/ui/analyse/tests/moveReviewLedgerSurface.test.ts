import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  moveReviewLedgerRootAttrs,
  renderMoveReviewLedgerSignalRows,
} from '../src/moveReview/ledgerSurface';
import type { MoveReviewStrategicLedgerV1 } from '../src/moveReview/responsePayload';

const sampleLedger: MoveReviewStrategicLedgerV1 = {
  schema: 'chesstory.move_review.ledger.v1',
  motifKey: 'rook_pawn_march',
  motifLabel: 'Rook-pawn march',
  stageKey: 'build',
  stageLabel: 'Build',
  carryOver: true,
  stageReason: 'The rook lift is still being assembled.',
  prerequisites: ['Rook lift in place', 'Center remains locked'],
  conversionTrigger: 'Open the h-file',
  primaryLine: {
    title: 'Probe continuation',
    sanMoves: ['Rh3', 'Kh7', 'Rg3'],
    scoreCp: 28,
    mate: null,
    note: 'free_tempo_branches',
    source: 'probe',
  },
  resourceLine: {
    title: 'Deferred branch',
    sanMoves: ['h5', 'Rc1+', 'Kh2'],
    scoreCp: -14,
    mate: null,
    note: 'why-not branch',
    source: 'variation',
  },
};

describe('moveReview ledger surface', () => {
  test('renders compact signal rows inside the existing strategic summary', () => {
    const rows = renderMoveReviewLedgerSignalRows(sampleLedger).join('');
    assert.match(rows, /Motif:/);
    assert.match(rows, /Stage:/);
    assert.match(rows, /Carry-over:/);
    assert.match(rows, /Prereqs:/);
    assert.match(rows, /Conversion:/);
  });

  test('exports root observability attrs from the ledger', () => {
    assert.deepEqual(moveReviewLedgerRootAttrs(sampleLedger), {
      'data-commentary-motif': 'rook_pawn_march',
      'data-commentary-stage': 'build',
      'data-commentary-carry-over': 'true',
    });
  });
});
