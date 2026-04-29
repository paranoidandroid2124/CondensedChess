import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { makeMoveExplanation, type MoveExplanationHost, type MoveExplanationState } from '../src/chesstory/moveExplanation';
import { visibleMoveExplanationBlocks } from '../src/chesstory/moveExplanationSurface';
import type {
  CommentaryRequest,
  CommentaryResponse,
  PublicCommentaryRender,
  RenderVariationEvidence,
} from '../src/chesstory/commentaryBridge';

const beforeFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const currentFen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
const nextFen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';

describe('move explanation surface', () => {
  test('request payload uses exact current node and includes beforeFen and playedMove only as a pair', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const requests: CommentaryRequest[] = [];
    const endpoints: string[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (endpoint, request) => {
        endpoints.push(endpoint);
        requests.push(request);
        return response({ text: 'The move claims central space.' });
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

    host.path = 'branch-a';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [host.node];
    await ctrl.refresh();

    assert.deepEqual(requests[1], {
      currentFen: nextFen,
      nodeId: 'branch-a',
      ply: 2,
    });

    host.nodeList = [{ fen: currentFen, ply: 1 }, { fen: nextFen, ply: 2 }];
    host.node = { fen: nextFen, ply: 2 };
    await ctrl.refresh();

    assert.deepEqual(requests[2], {
      currentFen: nextFen,
      nodeId: 'branch-a',
      ply: 2,
    });
  });

  test('local probe provider uses the internal endpoint without adding proof fields to the public request', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const publicRequests: CommentaryRequest[] = [];
    const internalPayloads: unknown[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (_endpoint, request) => {
        publicRequests.push(request);
        return response({ text: 'Public fallback.' });
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
          return response({
            text: 'Line-backed internal note.',
            variationEvidence: [variationEvidence('proof-claim-primary-resource', ['Nxe5', 'Nxe5'])],
            variationEvidenceIds: ['proof-claim-primary-resource'],
          });
        },
      },
    });

    await ctrl.refresh();

    assert.deepEqual(publicRequests, []);
    assert.equal(internalPayloads.length, 1);
    assert.match(surfaceText(ctrl.state()), /Line-backed internal note/);
    assert.doesNotMatch(JSON.stringify(internalPayloads[0]), /enginePacket|debug/);
  });

  test('local probe failure stays silent instead of requesting public fallback text', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const publicRequests: CommentaryRequest[] = [];
    const ctrl = makeMoveExplanation(host, {
      fetchJson: async (_endpoint, request) => {
        publicRequests.push(request);
        return response({ text: 'Public fallback must not appear.' });
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

  test('local probe response without public line evidence stays silent', async () => {
    const host = hostAt({ path: '', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
    const ctrl = makeMoveExplanation(host, {
      localProbe: {
        buildPayload: async input => ({
          request: {
            currentFen: input.current.currentFen,
            nodeId: input.current.nodeId,
            ply: input.current.ply,
          },
          completedProbe: { current: { currentFen, nodeId: 'root', ply: 1, variant: 'standard' } },
        }),
        fetchJson: async () => response({ text: 'Fallback prose must stay hidden.' }),
      },
    });

    await ctrl.refresh();

    assert.equal(ctrl.state().kind, 'empty');
    assert.equal(surfaceText(ctrl.state()), '');
  });

  test('stale late response cannot overwrite newer node output', async () => {
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

    second.resolve(response({ text: 'Black answers in the center.' }));
    await secondRefresh;
    assert.match(surfaceText(ctrl.state()), /Black answers in the center/);

    first.resolve(response({ text: 'This old note must not return.' }));
    await firstRefresh;

    assert.match(surfaceText(ctrl.state()), /Black answers in the center/);
    assert.doesNotMatch(surfaceText(ctrl.state()), /old note/);
  });

  test('refresh redraws immediately after clearing the previous node output', async () => {
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
    first.resolve(response({ text: 'First node note.' }));
    await firstRefresh;
    assert.match(surfaceText(ctrl.state()), /First node note/);
    const redrawsAfterFirstReady = redraws;

    host.path = 'b';
    host.node = { fen: nextFen, ply: 2, uci: 'e7e5' };
    host.nodeList = [{ fen: currentFen, ply: 1 }, host.node];
    const secondRefresh = ctrl.refresh();

    assert.equal(ctrl.state().kind, 'loading');
    assert.equal(surfaceText(ctrl.state()), '');
    assert.ok(redraws > redrawsAfterFirstReady);

    second.resolve(response({ text: 'Second node note.' }));
    await secondRefresh;
  });

  test('noCommentary invalid hidden and negative_only produce no chess fallback text', async () => {
    const cases: CommentaryResponse[] = [
      response({ status: 'noCommentary', noCommentary: true, renderStatus: 'noCommentary', text: '' }),
      response({ status: 'invalidRequest', renderStatus: 'noCommentary', text: '' }),
      response({ wording: 'hidden', text: 'Hidden text must stay hidden.' }),
      response({ wording: 'negative_only', text: 'Negative text must stay hidden.' }),
    ];

    for (const backendResponse of cases) {
      const host = hostAt({ path: 'a', fen: currentFen, ply: 1, uci: 'e2e4' }, { fen: beforeFen, ply: 0 });
      const ctrl = makeMoveExplanation(host, { fetchJson: async () => backendResponse });

      await ctrl.refresh();

      assert.equal(ctrl.state().kind, 'empty');
      assert.equal(surfaceText(ctrl.state()), '');
    }
  });

  test('view does not expose developer or internal tokens to player text', () => {
    const state: MoveExplanationState = readyState(render({
      blocks: [
        block('claim-primary', 'Black keeps pressure on the center.', ['proof-claim-primary-resource']),
      ],
      variationEvidence: [
        {
          ...variationEvidence('proof-claim-primary-resource', ['Nxe5', 'Nxe5']),
          proofId: 'proof-internal-id',
          boundClaimId: 'claim-internal-id',
        },
      ],
      boundaries: [{ claimId: 'claim-primary', reason: 'boundary-depth' }],
      evidenceRefs: [{ kind: 'Certification', id: 'cache-proof-id', owner: 'white', anchor: 'e5', route: 'probe-route', scope: 'candidate' }],
    }));

    const text = surfaceText(state);

    assert.equal(hasForbiddenPlayerToken(text), false, text);
    assert.doesNotMatch(text, /proof-internal-id|claim-internal-id|cache-proof-id|boundary-depth|probe-route/i);
  });

  test('public block text is shown in block order', () => {
    const state: MoveExplanationState = readyState(render({
      blocks: [block('a', 'First block from the backend.'), block('b', 'Second block from the backend.')],
    }));

    const text = surfaceText(state);

    assert.ok(text.indexOf('First block from the backend.') >= 0);
    assert.ok(text.indexOf('Second block from the backend.') > text.indexOf('First block from the backend.'));
  });

  test('public variation SAN line is shown only from decoded public variation evidence without raw fields', () => {
    const withoutEvidence: MoveExplanationState = readyState(render({
      blocks: [block('claim-primary', 'The reply is the point.', ['proof-claim-primary-resource'])],
    }));

    assert.doesNotMatch(surfaceText(withoutEvidence), /Nxe5/);

    const withEvidence: MoveExplanationState = readyState(render({
      blocks: [block('claim-primary', 'The reply is the point.', ['proof-claim-primary-resource'])],
      variationEvidence: [
        {
          ...variationEvidence('proof-claim-primary-resource', ['Nxe5', 'Nxe5']),
          lineUci: ['f3e5', 'c6e5'],
          boundary: {
            depthFloor: 16,
            realizedDepth: 18,
            multiPv: 2,
            freshnessChecked: true,
            legalReplayChecked: true,
            baselineChecked: true,
          },
          engineEval: '+0.42',
          rawPv: 'f3e5 c6e5',
          cacheKey: 'candidate-line-cache',
        } as RenderVariationEvidence & Record<string, unknown>,
      ],
    }));

    const text = surfaceText(withEvidence);

    assert.match(text, /Line/);
    assert.match(text, /Nxe5 Nxe5/);
    assert.doesNotMatch(text, /f3e5|c6e5|depth|eval|engine|rawPv|cache|candidate-line-cache|proof-claim-primary-resource/i);
  });

  test('public variation SAN line must be bound to the same visible block claim', () => {
    const state: MoveExplanationState = readyState(render({
      blocks: [block('claim-primary', 'The reply is the point.', ['proof-shared-resource'])],
      variationEvidence: [
        {
          ...variationEvidence('proof-shared-resource', ['Nxe5', 'Nxe5']),
          boundClaimId: 'claim-other',
        },
      ],
    }));

    const text = surfaceText(state);

    assert.match(text, /The reply is the point/);
    assert.doesNotMatch(text, /Nxe5/);
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

function readyState(render: PublicCommentaryRender): MoveExplanationState {
  return render.kind === 'render'
    ? { kind: 'ready', identity: { currentFen, nodeId: 'a', ply: 1 }, render }
    : { kind: 'empty' };
}

function surfaceText(state: MoveExplanationState): string {
  if (state.kind !== 'ready') return '';
  return visibleMoveExplanationBlocks(state)
    .flatMap(block => [block.label, block.text, block.line ? `Line ${block.line}` : ''])
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function hasForbiddenPlayerToken(text: string): boolean {
  return /\b(claim|proof|evidence|boundary|candidate|probe|cache|engine|depth|eval|pv)\b/i.test(text);
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(r => {
    resolve = r;
  });
  return { promise, resolve };
}

function response(overrides: {
  status?: CommentaryResponse['status'];
  noCommentary?: boolean;
  renderStatus?: CommentaryResponse['render']['status'];
  wording?: CommentaryResponse['render']['wording']['maxStrength'];
  text?: string;
  variationEvidence?: RenderVariationEvidence[];
  variationEvidenceIds?: string[];
} = {}): CommentaryResponse {
  const status = overrides.renderStatus || (overrides.status === 'noCommentary' ? 'noCommentary' : 'rendered');
  return {
    status: overrides.status || status,
    noCommentary: overrides.noCommentary || false,
    render: {
      schemaVersion: 1,
      status,
      blocks:
        status === 'noCommentary'
          ? []
          : [
              block(
                'claim-primary',
                overrides.text === undefined ? 'Backend public text.' : overrides.text,
                overrides.variationEvidenceIds,
              ),
            ],
      evidenceRefs: [],
      variationEvidence: overrides.variationEvidence,
      boundaries: [],
      suppressions: [],
      wording: {
        maxStrength: overrides.wording || (status === 'contextOnly' ? 'context_only' : 'qualified_support'),
        allowedPublicText: true,
        forbiddenTerms: [],
      },
    },
  };
}

function render(overrides: Partial<Extract<PublicCommentaryRender, { kind: 'render' }>> = {}): PublicCommentaryRender {
  return {
    kind: 'render',
    schemaVersion: 1,
    status: 'rendered',
    renderStatus: 'rendered',
    blocks: [block('claim-primary', 'Backend public text.')],
    evidenceRefs: [],
    boundaries: [],
    wording: {
      maxStrength: 'qualified_support',
      allowedPublicText: true,
      forbiddenTerms: [],
    },
    ...overrides,
  };
}

function block(claimId: string, publicText: string, variationEvidenceIds?: string[]) {
  return {
    role: 'primary' as const,
    claimId,
    text: { publicText, forbiddenTerms: [] },
    wordingStrength: 'qualified_support' as const,
    evidenceIds: [],
    ...(variationEvidenceIds ? { variationEvidenceIds } : {}),
    boundaries: [],
    nonAuthoritative: false,
  };
}

function variationEvidence(proofId: string, lineSan: string[]): RenderVariationEvidence {
  return {
    proofId,
    boundClaimId: 'claim-primary',
    startFen: currentFen,
    owner: 'white',
    defender: 'black',
    anchor: 'e5',
    route: 'counterplay_resource',
    scope: 'position',
    role: 'resource',
    moveRole: 'defender_resource',
    lineSan,
    lineUci: ['f3e5', 'c6e5'],
    playedMove: { san: lineSan[0], uci: 'f3e5' },
    candidateMove: { san: lineSan[0], uci: 'f3e5' },
    defenderResource: { san: lineSan[1], uci: 'c6e5' },
    continuation: [{ san: lineSan[1], uci: 'c6e5' }],
    testedMove: { san: lineSan[0], uci: 'f3e5' },
    testedLine: [{ san: lineSan[0], uci: 'f3e5' }],
    replyLine: [{ san: lineSan[1], uci: 'c6e5' }],
    resourceLine: [{ san: lineSan[1], uci: 'c6e5' }],
    testResult: 'resource_fails',
    proofPurpose: 'fails',
    provenanceRefs: [],
    boundary: {
      depthFloor: 16,
      realizedDepth: 18,
      multiPv: 2,
      freshnessChecked: true,
      legalReplayChecked: true,
      baselineChecked: true,
    },
    wordingCap: 'qualified_support',
    surfaceAllowance: 'public_line',
  };
}
