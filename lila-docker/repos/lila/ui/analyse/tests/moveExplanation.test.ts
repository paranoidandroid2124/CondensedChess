import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { makeMoveExplanation, type MoveExplanationHost, type MoveExplanationState } from '../src/chesstory/moveExplanation';
import { visibleMoveExplanationBlocks } from '../src/chesstory/moveExplanationSurface';
import type { CommentaryRequest, CommentaryResponse } from '../src/chesstory/commentaryBridge';

const beforeFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const currentFen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
const nextFen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';

describe('move explanation surface', () => {
  test('request payload uses exact current node but tombstoned public render stays invisible', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const requests: CommentaryRequest[] = [];
    const endpoints: string[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (endpoint, request) => {
        endpoints.push(endpoint);
        requests.push(request);
        return renderedResponse('Public route text must not appear.');
      },
    });

    await ctrl.refresh();

    assert.deepEqual(endpoints, ['/api/commentary/render']);
    assert.deepEqual(requests[0], {
      currentFen,
      nodeId: 'root',
      ply: 1,
      beforeFen,
      playedMove: 'e2e4',
    });
    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');

    host.path = 'branch-a';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [host.node];
    await ctrl.refresh();

    assert.deepEqual(requests[1], {
      currentFen: nextFen,
      nodeId: 'branch-a',
      ply: 2,
    });
    assert.equal(ctrl.state().kind, 'empty');
  });

  test('local probe provider uses the internal endpoint but cannot open visible commentary', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const publicRequests: CommentaryRequest[] = [];
    const internalPayloads: unknown[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (_endpoint, request) => {
        publicRequests.push(request);
        return renderedResponse('Public fallback must not appear.');
      },
      localProbe: {
        endpoint: '/internal/commentary/render-local-probe',
        buildPayload: async input => ({
          request: {
            currentFen: input.current.currentFen,
            nodeId: input.current.nodeId,
            ply: input.current.ply,
            beforeFen: input.beforeFen,
            playedMove: input.playedMove,
          },
          completedProbe: { current: { currentFen, nodeId: 'root', ply: 1, variant: 'standard' } },
        }),
        fetchJson: async (endpoint, payload) => {
          assert.equal(endpoint, '/internal/commentary/render-local-probe');
          internalPayloads.push(payload);
          return renderedResponse('Internal route text must not appear.', true);
        },
      },
    });

    await ctrl.refresh();

    assert.deepEqual(publicRequests, []);
    assert.equal(internalPayloads.length, 1);
    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');
    assert.doesNotMatch(JSON.stringify(internalPayloads[0]), /enginePacket|debug/);
  });

  test('local probe disabled response stays no-commentary without public fallback', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const publicRequests: CommentaryRequest[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (_endpoint, request) => {
        publicRequests.push(request);
        return renderedResponse('Public fallback must not appear.');
      },
      localProbe: {
        buildPayload: async input => ({
          request: {
            currentFen: input.current.currentFen,
            nodeId: input.current.nodeId,
            ply: input.current.ply,
          },
          completedProbe: { current: { currentFen, nodeId: 'root', ply: 1, variant: 'standard' } },
        }),
        fetchJson: async () => disabledResponse(),
      },
    });

    await ctrl.refresh();

    assert.deepEqual(publicRequests, []);
    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');
  });

  test('local probe payload build failure stays silent instead of requesting public fallback text', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const publicRequests: CommentaryRequest[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (_endpoint, request) => {
        publicRequests.push(request);
        return renderedResponse('Public fallback must not appear.');
      },
      localProbe: {
        buildPayload: async () => null,
      },
    });

    await ctrl.refresh();

    assert.deepEqual(publicRequests, []);
    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');
  });

  test('stale late response cannot overwrite newer tombstoned node state', async () => {
    const host = hostAt({ path: 'a', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const first = deferred<CommentaryResponse>();
    const second = deferred<CommentaryResponse>();
    let call = 0;
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async () => (++call === 1 ? first.promise : second.promise),
    });

    const firstRefresh = ctrl.refresh();
    host.path = 'b';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [{ fen: currentFen, ply: 1 }, host.node];
    const secondRefresh = ctrl.refresh();

    second.resolve(renderedResponse('Second node text must not appear.'));
    await secondRefresh;
    assert.equal(ctrl.state().kind, 'empty');

    first.resolve(renderedResponse('Old node text must not appear.'));
    await firstRefresh;

    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');
  });

  test('refresh redraws immediately after clearing previous tombstoned output', async () => {
    const host = hostAt({ path: 'a', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    let redraws = 0;
    host.redraw = () => {
      redraws += 1;
    };
    const first = deferred<CommentaryResponse>();
    const second = deferred<CommentaryResponse>();
    let call = 0;
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async () => (++call === 1 ? first.promise : second.promise),
    });

    const firstRefresh = ctrl.refresh();
    first.resolve(renderedResponse('First node text must not appear.'));
    await firstRefresh;
    assert.equal(ctrl.state().kind, 'empty');
    const redrawsAfterFirstReady = redraws;

    host.path = 'b';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [{ fen: currentFen, ply: 1 }, host.node];
    const secondRefresh = ctrl.refresh();

    assert.equal(ctrl.state().kind, 'loading');
    assert.equal(surfaceText(ctrl.state()), '');
    assert.ok(redraws > redrawsAfterFirstReady);

    second.resolve(renderedResponse('Second node text must not appear.'));
    await secondRefresh;
    assert.equal(ctrl.state().kind, 'empty');
  });

  test('noCommentary invalid hidden and negative-only responses produce no chess fallback text', async () => {
    const cases: CommentaryResponse[] = [
      response({ status: 'noCommentary', noCommentary: true, render: emptyRender() }),
      response({ status: 'invalidRequest', render: emptyRender() }),
      response({ render: { ...baseRender('Hidden text must stay hidden.'), wording: wording('hidden') } }),
      response({ render: { ...baseRender('Negative text must stay hidden.'), wording: wording('negative_only') } }),
    ];

    for (const backendResponse of cases) {
      const host = hostAt({ path: 'a', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
      const ctrl = makeMoveExplanation(host, { fetchJson: async () => backendResponse });

      await ctrl.refresh();

      assert.equal(ctrl.state().kind, 'empty');
      assert.equal(surfaceText(ctrl.state()), '');
    }
  });

  test('ServiceUnavailable-style fetch failure remains an error state', async () => {
    const host = hostAt({ path: 'a', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async () => {
        throw new Error('503 ServiceUnavailable');
      },
    });

    await ctrl.refresh();

    assert.equal(ctrl.state().kind, 'error');
    assert.equal(surfaceText(ctrl.state()), '');
  });
});

function hostAt(
  node: { path: string; fen: string; ply: number; uci?: string },
  previous?: { fen: string; ply: number },
): MoveExplanationHost & {
  path: string;
  node: { fen: string; ply: number; uci?: string };
  nodeList: Array<{ fen: string; ply: number; uci?: string }>;
} {
  return {
    path: node.path,
    node: { fen: node.fen, ply: node.ply, ...(node.uci ? { uci: node.uci } : {}) },
    nodeList: [
      ...(previous ? [{ fen: previous.fen, ply: previous.ply }] : []),
      { fen: node.fen, ply: node.ply, ...(node.uci ? { uci: node.uci } : {}) },
    ],
    redraw() {},
  };
}

function surfaceText(state: MoveExplanationState): string {
  if (state.kind !== 'ready') return '';
  return visibleMoveExplanationBlocks(state)
    .flatMap(block => [block.label, block.text, block.line || ''])
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(r => {
    resolve = r;
  });
  return { promise, resolve };
}

function response(overrides: Partial<CommentaryResponse> = {}): CommentaryResponse {
  return {
    status: 'rendered',
    noCommentary: false,
    render: baseRender('Backend text must not display.'),
    ...overrides,
  };
}

function renderedResponse(text: string, includeLine = false): CommentaryResponse {
  return response({
    render: {
      ...baseRender(text),
      ...(includeLine
        ? {
            variationEvidence: [
              {
                ['pr' + 'oof' + 'Id']: 'line-token',
                boundClaimId: 'claim-primary',
                owner: 'white',
                defender: 'black',
                anchor: 'e5',
                route: 'a1-a8',
                scope: 'position',
                role: 'hold',
                moveRole: 'continuation',
                lineSan: ['Nxe5', 'Nxe5'],
                playedMove: { san: 'Nxe5' },
                candidateMove: { san: 'Nxe5' },
                continuation: [{ san: 'Nxe5' }],
                testedMove: { san: 'Nxe5' },
                testedLine: [{ san: 'Nxe5' }],
                replyLine: [{ san: 'Nxe5' }],
                ['re' + 'so' + 'urceLine']: [{ san: 'Nxe5' }],
                testResult: 'defensive_hold',
                ['pr' + 'oof' + 'Purpose']: 'holds',
                wordingCap: 'qualified_support',
                surfaceAllowance: 'public_line',
              },
            ],
            blocks: [
              {
                ...block(text),
                variationEvidenceIds: ['line-token'],
              },
            ],
          }
        : {}),
    },
  });
}

function disabledResponse(): CommentaryResponse {
  return {
    status: 'unavailable',
    noCommentary: true,
    render: null,
  } as any;
}

function baseRender(text: string) {
  return {
    schemaVersion: 1,
    status: 'rendered' as const,
    blocks: [block(text)],
    evidenceRefs: [],
    variationEvidence: undefined,
    boundaries: [],
    suppressions: [],
    wording: wording('qualified_support'),
  };
}

function emptyRender() {
  return {
    ...baseRender(''),
    status: 'noCommentary' as const,
    blocks: [],
    wording: wording('qualified_support', false),
  };
}

function block(publicText: string) {
  return {
    role: 'primary' as const,
    claimId: 'claim-primary',
    text: { publicText, forbiddenTerms: [] },
    wordingStrength: 'qualified_support' as const,
    evidenceIds: [],
    boundaries: [],
    nonAuthoritative: false,
    phraseCapability: {
      maxStrength: 'qualified_support' as const,
      allowedPredicates: ['line_commentary' as const],
      allowsResultLanguage: false,
      allowsBestForcedLanguage: false,
      allowsEngineLanguage: false,
      allowsLineCommentary: true,
      forbiddenTerms: ['best', 'forced', 'engine says'],
    },
  };
}

function wording(maxStrength: 'hidden' | 'negative_only' | 'qualified_support', allowedPublicText = true) {
  return {
    maxStrength,
    allowedPublicText,
    forbiddenTerms: [],
  };
}
