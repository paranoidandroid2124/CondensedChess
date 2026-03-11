import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import * as Prefs from 'lib/prefs';
import {
  applyBoardLabelMode,
  boardCoordsToViewConfig,
  boardLabelModeFromCoords,
  boardLabelModeToCoords,
} from '../src/boardWorkspace';

describe('board workspace label modes', () => {
  test('maps stored coordinates into workspace modes', () => {
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Hidden), 'off');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Inside), 'inside');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.Outside), 'rim');
    assert.equal(boardLabelModeFromCoords(Prefs.Coords.All), 'full');
  });

  test('maps workspace modes back into chessground coordinates', () => {
    assert.equal(boardLabelModeToCoords('off'), Prefs.Coords.Hidden);
    assert.equal(boardLabelModeToCoords('inside'), Prefs.Coords.Inside);
    assert.equal(boardLabelModeToCoords('rim'), Prefs.Coords.Outside);
    assert.equal(boardLabelModeToCoords('full'), Prefs.Coords.All);
  });

  test('derives board-view flags from stored coordinate modes', () => {
    assert.deepEqual(boardCoordsToViewConfig(Prefs.Coords.Hidden), {
      coordinates: false,
      coordinatesOnSquares: false,
    });
    assert.deepEqual(boardCoordsToViewConfig(Prefs.Coords.Inside), {
      coordinates: true,
      coordinatesOnSquares: false,
    });
    assert.deepEqual(boardCoordsToViewConfig(Prefs.Coords.All), {
      coordinates: true,
      coordinatesOnSquares: true,
    });
  });

  test('forces a chessground redraw when board labels change', () => {
    const calls: Array<['set', { coordinates: boolean; coordinatesOnSquares: boolean }] | ['redrawAll']> = [];
    const target = {
      set(config: { coordinates: boolean; coordinatesOnSquares: boolean }) {
        calls.push(['set', config]);
      },
      redrawAll() {
        calls.push(['redrawAll']);
      },
    };

    applyBoardLabelMode(target, 'full');

    assert.deepEqual(calls, [
      ['set', { coordinates: true, coordinatesOnSquares: true }],
      ['redrawAll'],
    ]);
  });
});
