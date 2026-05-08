import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { makeMoveExplanation, type MoveExplanationHost } from '../src/chesstory/moveExplanation';

const beforeFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const currentFen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
const nextFen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';

describe('move explanation frontend no-op', () => {
  test('stays empty across refreshes and node changes', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const ctrl = makeMoveExplanation(host);

    assert.deepEqual(ctrl.state(), { kind: 'empty' });
    await ctrl.refresh();
    assert.deepEqual(ctrl.state(), { kind: 'empty' });

    host.path = 'branch-a';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [host.node];
    await ctrl.refresh();

    assert.deepEqual(ctrl.state(), { kind: 'empty' });
  });

  test('does not carry public or local commentary transport code', () => {
    const source = readFileSync(fileURLToPath(new URL('../src/chesstory/moveExplanation.ts', import.meta.url)), 'utf8');

    assert.doesNotMatch(source, /\/api\/commentary\/render/);
    assert.doesNotMatch(source, /\/internal\/commentary\/render-local-probe/);
    assert.doesNotMatch(source, /fetch\(/);
  });
});

function hostAt(current: { path: string; fen: string; ply: number; uci?: string }, previous?: { fen: string; ply: number }): MoveExplanationHost {
  const node = { fen: current.fen, ply: current.ply, uci: current.uci };
  return {
    path: current.path,
    node,
    nodeList: previous ? [{ fen: previous.fen, ply: previous.ply }, node] : [node],
    redraw: () => undefined,
  };
}
