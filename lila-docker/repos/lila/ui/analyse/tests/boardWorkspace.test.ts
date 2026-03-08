import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import * as Prefs from 'lib/prefs';
import { boardLabelModeFromCoords, boardLabelModeToCoords } from '../src/boardWorkspace';

describe('board workspace label modes', () => {
  test('maps stored coordinates into workspace modes', () => {
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Hidden), 'off');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Outside), 'rim');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Inside), 'rim');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.All), 'full');
  });

  test('maps workspace modes back into chessground coordinates', () => {
    assert.equal(boardLabelModeToCoords('off'), Prefs.Coords.Hidden);
    assert.equal(boardLabelModeToCoords('rim'), Prefs.Coords.Outside);
    assert.equal(boardLabelModeToCoords('full'), Prefs.Coords.All);
  });
});
