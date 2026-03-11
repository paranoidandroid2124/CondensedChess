import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  bookmakerLedgerRootAttrs,
  renderBookmakerLedgerProbeRows,
  renderBookmakerLedgerSignalRows,
} from '../src/bookmaker/ledgerSurface';
import type { BookmakerStrategicLedgerV1 } from '../src/bookmaker/responsePayload';

const sampleLedger: BookmakerStrategicLedgerV1 = {
  schema: 'chesstory.bookmaker.ledger.v1',
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

describe('bookmaker ledger surface', () => {
  test('renders compact signal rows inside the existing strategic summary', () => {
    const rows = renderBookmakerLedgerSignalRows(sampleLedger).join('');
    assert.match(rows, /Motif:/);
    assert.match(rows, /Stage:/);
    assert.match(rows, /Carry-over:/);
    assert.match(rows, /Prereqs:/);
    assert.match(rows, /Conversion:/);
  });

  test('renders compact probe rows with interactive move chips', () => {
    const rows = renderBookmakerLedgerProbeRows(sampleLedger, san => {
      if (san === 'Rh3') return { refId: 'r1', san: 'Rh3', uci: 'h1h3' };
      if (san === 'Rg3') return { refId: 'r2', san: 'Rg3', uci: 'h3g3' };
      return null;
    }).join('');

    assert.match(rows, /Plan line:/);
    assert.match(rows, /Counter-resource:/);
    assert.match(rows, /data-ref-id="r1"/);
    assert.match(rows, /move-chip--interactive/);
  });

  test('exports root observability attrs from the ledger', () => {
    assert.deepEqual(bookmakerLedgerRootAttrs(sampleLedger), {
      'data-llm-motif': 'rook_pawn_march',
      'data-llm-stage': 'build',
      'data-llm-carry-over': 'true',
    });
  });
});
