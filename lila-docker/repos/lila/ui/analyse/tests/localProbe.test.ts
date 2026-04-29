import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildLocalCommentaryProbePayload,
  LocalCommentaryProbeCache,
  type LocalCommentaryProbeEngine,
} from '../src/chesstory/localProbe';

const currentFen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
const afterE5Fen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';

describe('local commentary probe payload', () => {
  test('builds root MultiPV 3 and child MultiPV 2 from local engine results', async () => {
    const calls: Array<{ fen: string; multiPv: number; targetDepth: number }> = [];
    const engine: LocalCommentaryProbeEngine = {
      async run(request) {
        calls.push({ fen: request.fen, multiPv: request.multiPv, targetDepth: request.targetDepth });
        if (request.multiPv === 3)
          return {
            fen: request.fen,
            depth: 18,
            pvs: [
              { moves: ['e7e5', 'g1f3'] },
              { moves: ['c7c5', 'g1f3'] },
              { moves: ['e7e6', 'd2d4'] },
            ],
          };
        return {
          fen: request.fen,
          depth: 18,
          pvs: [
            { moves: ['g1f3', 'b8c6'] },
            { moves: ['d2d4', 'e5d4'] },
          ],
        };
      },
    };

    const payload = await buildLocalCommentaryProbePayload({
      current: { currentFen, nodeId: 'root', ply: 1 },
      beforeFen: 'before',
      playedMove: 'e2e4',
      variant: 'standard',
      engineFingerprint: 'local-stockfish-test',
      now: () => 1000,
      engine,
    });

    assert.ok(payload);
    assert.equal(payload.request.currentFen, currentFen);
    assert.equal(payload.completedProbe.rootProbe.multiPv, 3);
    assert.equal(payload.completedProbe.rootProbe.realizedDepth, 18);
    assert.deepEqual(payload.completedProbe.rootProbe.lines.map(line => line.uci[0]), ['e7e5', 'c7c5', 'e7e6']);
    assert.equal(payload.completedProbe.childProbes.length, 2);
    assert.deepEqual(payload.completedProbe.childProbes.map(child => child.multiPv), [2, 2]);
    assert.deepEqual(payload.completedProbe.childProbes.map(child => child.parentBranchId), ['root-candidate-1', 'root-candidate-2']);
    assert.equal(payload.completedProbe.childProbes[0].currentFen, afterE5Fen);
    assert.deepEqual(calls.map(call => [call.multiPv, call.targetDepth]), [
      [3, 18],
      [2, 18],
      [2, 18],
    ]);
  });

  test('fails closed without a root depth floor or complete child replies', async () => {
    const shallow = await buildLocalCommentaryProbePayload({
      current: { currentFen, nodeId: 'root', ply: 1 },
      variant: 'standard',
      engineFingerprint: 'local-stockfish-test',
      now: () => 1000,
      engine: {
        async run() {
          return { fen: currentFen, depth: 15, pvs: [{ moves: ['e7e5'] }, { moves: ['c7c5'] }, { moves: ['e7e6'] }] };
        },
      },
    });

    assert.equal(shallow, null);

    const missingChild = await buildLocalCommentaryProbePayload({
      current: { currentFen, nodeId: 'root', ply: 1 },
      variant: 'standard',
      engineFingerprint: 'local-stockfish-test',
      now: () => 1000,
      engine: {
        async run(request) {
          if (request.multiPv === 3)
            return {
              fen: request.fen,
              depth: 18,
              pvs: [
                { moves: ['e7e5', 'g1f3'] },
                { moves: ['c7c5', 'g1f3'] },
                { moves: ['e7e6', 'd2d4'] },
              ],
            };
          return { fen: request.fen, depth: 18, pvs: [{ moves: ['g1f3'] }] };
        },
      },
    });

    assert.equal(missingChild, null);
  });

  test('reuses completed local probe results from the in-memory cache', async () => {
    const cache = new LocalCommentaryProbeCache();
    let calls = 0;
    const engine: LocalCommentaryProbeEngine = {
      async run(request) {
        calls += 1;
        if (request.multiPv === 3)
          return {
            fen: request.fen,
            depth: 18,
            pvs: [
              { moves: ['e7e5', 'g1f3'] },
              { moves: ['c7c5', 'g1f3'] },
              { moves: ['e7e6', 'd2d4'] },
            ],
          };
        return {
          fen: request.fen,
          depth: 18,
          pvs: [
            { moves: ['g1f3', 'b8c6'] },
            { moves: ['d2d4', 'e5d4'] },
          ],
        };
      },
    };
    const input = {
      current: { currentFen, nodeId: 'root', ply: 1 },
      variant: 'standard' as VariantKey,
      engineFingerprint: 'local-stockfish-test',
      now: () => 1000,
      engine,
      cache,
    };

    assert.ok(await buildLocalCommentaryProbePayload(input));
    assert.ok(await buildLocalCommentaryProbePayload(input));

    assert.equal(calls, 3);
  });

  test('does not reuse stale local probe cache entries', async () => {
    const cache = new LocalCommentaryProbeCache();
    let calls = 0;
    const engine: LocalCommentaryProbeEngine = {
      async run(request) {
        calls += 1;
        if (request.multiPv === 3)
          return {
            fen: request.fen,
            depth: 18,
            pvs: [
              { moves: ['e7e5', 'g1f3'] },
              { moves: ['c7c5', 'g1f3'] },
              { moves: ['e7e6', 'd2d4'] },
            ],
          };
        return {
          fen: request.fen,
          depth: 18,
          pvs: [
            { moves: ['g1f3', 'b8c6'] },
            { moves: ['d2d4', 'e5d4'] },
          ],
        };
      },
    };

    assert.ok(
      await buildLocalCommentaryProbePayload({
        current: { currentFen, nodeId: 'root', ply: 1 },
        variant: 'standard',
        engineFingerprint: 'local-stockfish-test',
        now: () => 1000,
        engine,
        cache,
      }),
    );
    assert.ok(
      await buildLocalCommentaryProbePayload({
        current: { currentFen, nodeId: 'root', ply: 1 },
        variant: 'standard',
        engineFingerprint: 'local-stockfish-test',
        now: () => 62_001,
        engine,
        cache,
      }),
    );

    assert.equal(calls, 6);
  });
});
