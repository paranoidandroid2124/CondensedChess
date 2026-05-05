import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import {
  buildCommentaryRequest,
  decodePublicCommentaryRender,
  fetchCommentaryRender,
  type CommentaryBridgeNodeIdentity,
  type CommentaryResponse,
} from '../src/chesstory/commentaryBridge';

const currentNode: CommentaryBridgeNodeIdentity = {
  currentFen: 'r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3',
  nodeId: 'mainline:0',
  ply: 5,
};

describe('minimal commentary frontend bridge', () => {
  test('builds only the backend CommentaryRequest fields from exact node input', () => {
    const request = buildCommentaryRequest({
      current: currentNode,
      beforeFen: 'r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3',
      playedMove: 'b1c3',
      enginePacket: { nodeId: 'mainline:0', ply: 5, depth: 18 },
      completedProbe: { rootProbe: { lines: [['e2e4']] } },
      debug: true,
      ignoredContext: { truthClaim: 'best move' },
    } as any);

    assert.deepEqual(Object.keys(request).sort(), [
      'beforeFen',
      'currentFen',
      'enginePacket',
      'nodeId',
      'playedMove',
      'ply',
    ]);
    assert.deepEqual(request.enginePacket, { nodeId: currentNode.nodeId, ply: currentNode.ply, depth: 18 });
    assert.equal((request as any).completedProbe, undefined);
    assert.equal((request as any).debug, undefined);
    assert.equal((request as any).ignoredContext, undefined);
  });

  test('omits enginePacket when caller data contains internal line, cache, or raw engine fields', () => {
    for (const enginePacket of [
      { completedProbePayload: { rootProbe: { lines: [['e2e4']] } } },
      { wrapper: { probe_payload: { rootProbe: { lines: [['e2e4']] } } } },
      { nested: { cacheKey: 'line-cache-key' } },
      { lineToken: 'caller-line-token' },
      { proves: 'best_move' },
      { rawPv: 'e2e4 e7e5' },
      { ['raw' + 'Eval']: '+0.42' },
      { ['so' + 'urce' + 'Row']: { verdict: 'best' } },
    ]) {
      const request = buildCommentaryRequest({ current: currentNode, enginePacket });
      assert.equal(request.enginePacket, undefined);
    }
  });

  test('backend-shaped rendered payloads fail closed without exposing blocks or evidence', () => {
    const decoded = decodePublicCommentaryRender(renderedResponse());

    assert.deepEqual(decoded, { kind: 'empty', reason: 'no_commentary' });
    assert.doesNotMatch(JSON.stringify(decoded), /Backend text|claim-public|ev-public|Nxe5/);
  });

  test('disabled route payloads decode as no commentary instead of public render', () => {
    const decoded = decodePublicCommentaryRender({
      status: 'unavailable',
      noCommentary: true,
      render: null,
    } as any);

    assert.deepEqual(decoded, { kind: 'empty', reason: 'no_commentary' });
  });

  test('invalid noCommentary hidden and negative-only statuses stay silent', () => {
    const cases: Array<[CommentaryResponse, string]> = [
      [response({ status: 'invalidRequest', render: { ...emptyRender(), status: 'noCommentary' } }), 'invalid_request'],
      [response({ status: 'noCommentary', noCommentary: true, render: emptyRender() }), 'no_commentary'],
      [response({ render: { ...baseRender(), wording: wording('hidden') } }), 'hidden'],
      [response({ render: { ...baseRender(), wording: wording('negative_only') } }), 'negative_only'],
    ];

    for (const [backendResponse, reason] of cases) assert.deepEqual(decodePublicCommentaryRender(backendResponse), { kind: 'empty', reason });
  });

  test('fetch sends sanitized request but still refuses current public render payloads', async () => {
    let sentEndpoint = '';
    let sentRequest: unknown;
    const decoded = await fetchCommentaryRender({
      endpoint: '/api/commentary/render',
      current: currentNode,
      enginePacket: {
        depth: 18,
        rootProbe: { lines: [['e2e4']] },
      },
      getCurrent: () => currentNode,
      fetchJson: async (endpoint, request) => {
        sentEndpoint = endpoint;
        sentRequest = request;
        return renderedResponse();
      },
    });

    assert.equal(sentEndpoint, '/api/commentary/render');
    assert.equal((sentRequest as any).enginePacket, undefined);
    assert.deepEqual(decoded, { kind: 'empty', reason: 'no_commentary' });
  });

  test('stale node responses are discarded before tombstone decoding', async () => {
    const decoded = await fetchCommentaryRender({
      endpoint: '/api/commentary/render',
      current: currentNode,
      getCurrent: () => ({ ...currentNode, ply: currentNode.ply + 1 }),
      fetchJson: async () => renderedResponse(),
    });

    assert.deepEqual(decoded, { kind: 'empty', reason: 'stale_node' });
  });

  test('live docs keep the frontend bridge downstream of fail-closed tombstones', () => {
    const readme = readFileSync(
      fileURLToPath(new URL('../../../modules/commentary/docs/README.md', import.meta.url)),
      'utf8',
    );
    const ssot = readFileSync(
      fileURLToPath(new URL('../../../modules/commentary/docs/ChessCommentarySSOT.md', import.meta.url)),
      'utf8',
    );
    const liveDocs = `${readme}\n${ssot}`.replace(/\s+/g, ' ');

    for (const token of [
      'Live authority is exactly and exhaustively',
      'Public route no-go',
      '/api/commentary/render',
      '/internal/commentary/render-local-probe',
      'fail-closed tombstones',
      'No `200`',
      'frontend mock',
      '`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`',
      'No other path owns current public chess meaning.',
      'selected `Verdict` data',
      'renderer non-authority',
      'must not create chess meaning',
    ])
      assert.match(liveDocs, new RegExp(token));
  });
});

function response(overrides: Partial<CommentaryResponse> = {}): CommentaryResponse {
  return {
    status: 'rendered',
    noCommentary: false,
    render: baseRender(),
    ...overrides,
  };
}

function renderedResponse(): CommentaryResponse {
  return response({
    render: {
      ...baseRender(),
      blocks: [
        {
          ...baseRender().blocks[0],
          claimId: 'claim-public',
          text: { publicText: 'Backend text must not display.', forbiddenTerms: [] },
          evidenceIds: ['ev-public'],
          variationEvidenceIds: ['line-public'],
        },
      ],
      evidenceRefs: [{ kind: 'line', id: 'ev-public', owner: 'white', anchor: 'e5', route: 'a1-a8', scope: 'position' }],
      variationEvidence: [{ id: 'line-public', lineSan: ['Nxe5', 'Nxe5'], surfaceAllowance: 'public_line' } as any],
    },
  });
}

function baseRender() {
  return {
    schemaVersion: 1,
    status: 'rendered' as const,
    blocks: [
      {
        role: 'primary' as const,
        claimId: 'claim-1',
        text: { publicText: 'Backend text must not display.', forbiddenTerms: [] },
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
      },
    ],
    evidenceRefs: [],
    variationEvidence: undefined,
    boundaries: [],
    suppressions: [],
    wording: wording('qualified_support'),
  };
}

function emptyRender() {
  return {
    ...baseRender(),
    status: 'noCommentary' as const,
    blocks: [],
    wording: wording('qualified_support', false),
  };
}

function wording(maxStrength: 'hidden' | 'negative_only' | 'qualified_support', allowedPublicText = true) {
  return {
    maxStrength,
    allowedPublicText,
    forbiddenTerms: [],
  };
}
