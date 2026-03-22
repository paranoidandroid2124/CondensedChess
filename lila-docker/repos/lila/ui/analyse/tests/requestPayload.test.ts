import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { buildBookmakerRequest } from '../src/bookmaker/requestPayload';
import { buildFullAnalysisRequestPayload } from '../src/narrative/requestPayload';

describe('request payload builders', () => {
  test('bookmaker request includes the current variant in context', () => {
    const payload = buildBookmakerRequest({
      fen: 'fen',
      lastMove: 'e2e4',
      variations: null,
      probeResults: null,
      openingData: null,
      afterFen: null,
      afterVariations: null,
      phase: 'opening',
      ply: 5,
      variant: 'chess960',
      planStateToken: null,
      endgameStateToken: null,
    });

    assert.equal(payload.context.variant, 'chess960');
  });

  test('game chronicle request includes the current variant at the top level', () => {
    const payload = buildFullAnalysisRequestPayload({
      pgn: '1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O *',
      evals: [],
      variant: 'standard',
    });

    assert.equal(payload.variant, 'standard');
    assert.deepEqual(payload.options.focusOn, ['mistakes', 'turning_points']);
  });
});
