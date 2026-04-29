import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import {
  buildCompletedProbeBridgePayload,
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
    const completedProbePayload = completedProbeInput();
    const request = buildCommentaryRequest({
      current: currentNode,
      beforeFen: 'r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3',
      playedMove: 'b1c3',
      enginePacket: { nodeId: 'mainline:0', ply: 5, rawEval: 'must not display' },
      completedProbePayload,
      debug: true,
      ignoredSourceContext: { truthClaim: 'best move' },
    } as any);

    assert.deepEqual(Object.keys(request).sort(), [
      'beforeFen',
      'currentFen',
      'enginePacket',
      'nodeId',
      'playedMove',
      'ply',
    ]);
    assert.equal(request.currentFen, currentNode.currentFen);
    assert.equal(request.nodeId, currentNode.nodeId);
    assert.equal(request.ply, currentNode.ply);
    assert.equal((request as any).completedProbePayload, undefined);
    assert.equal((request as any).debug, undefined);
    assert.equal((request as any).ignoredSourceContext, undefined);
  });

  test('omits enginePacket when it contains completed-probe bridge fields', () => {
    const request = buildCommentaryRequest({
      current: currentNode,
      enginePacket: {
        nodeId: currentNode.nodeId,
        ply: currentNode.ply,
        rawEval: 'allowed certification intake',
        completedProbePayload: completedProbeInput(),
      },
    });

    assert.equal(request.enginePacket, undefined);
    assert.doesNotMatch(JSON.stringify(request), /completedProbePayload|rootProbe|childProbes|probeRequests/);
  });

  test('omits enginePacket when candidate-line probe fields are nested inside it', () => {
    const request = buildCommentaryRequest({
      current: currentNode,
      enginePacket: {
        nodeId: currentNode.nodeId,
        pv: ['e2e4'],
        cache: {
          CandidateLineEvidence: {
            parentBranchId: 'root-rank-1',
            probeRequests: completedProbeInput().probeRequests,
          },
        },
      },
    });

    assert.equal(request.enginePacket, undefined);
    assert.doesNotMatch(JSON.stringify(request), /CandidateLineEvidence|parentBranchId|probeRequests/);
  });

  test('omits enginePacket for separator variants of candidate-line completed-probe probe cache and internal wrappers', () => {
    const allowed = buildCommentaryRequest({
      current: currentNode,
      enginePacket: {
        nodeId: currentNode.nodeId,
        ply: currentNode.ply,
        rawEval: 'allowed certification intake',
        depth: 18,
      },
    });
    assert.deepEqual(allowed.enginePacket, {
      nodeId: currentNode.nodeId,
      ply: currentNode.ply,
      rawEval: 'allowed certification intake',
      depth: 18,
    });

    for (const enginePacket of [
      { 'candidate-line': { rawEval: 'wrapper must still be rejected' } },
      { 'completed-probe': { depth: 18 } },
      { completed_probe: { depth: 18 } },
      { wrapper: { probe_payload: { rootProbe: completedProbeInput().rootProbe } } },
      { nested: { 'cache-key': 'candidate-line-cache' } },
      { nested: { internal_payload: { suppressions: [] } } },
    ]) {
      const request = buildCommentaryRequest({ current: currentNode, enginePacket });
      assert.equal(request.enginePacket, undefined);
    }
  });

  test('builds a sanitized copied completed-probe bridge payload for root and child probes', () => {
    const input = completedProbeInput();
    const payload = buildCompletedProbeBridgePayload(input);

    assert.deepEqual(payload, {
      current: { ...currentNode, variant: 'standard' },
      engineFingerprint: 'stockfish-local:16:nnue',
      budget: {
        rootMultiPv: 3,
        childMultiPv: 2,
        depthFloor: 16,
        rootTargetDepth: 18,
        childTargetDepth: 18,
        maxAgeMillis: 60000,
      },
      probeRequests: [
        {
          role: 'root_candidate',
          currentFen: currentNode.currentFen,
          nodeId: currentNode.nodeId,
          ply: currentNode.ply,
          variant: 'standard',
          multiPv: 3,
          requestedDepth: 18,
          depthFloor: 16,
        },
        {
          role: 'defender_resource',
          currentFen: childStartFen,
          nodeId: 'mainline:0/e2e4',
          ply: 6,
          variant: 'standard',
          multiPv: 2,
          requestedDepth: 18,
          depthFloor: 16,
          parentBranchId: 'root-rank-1',
          parentUciPrefix: ['e2e4'],
          parentRootRank: 1,
        },
      ],
      rootProbe: {
        currentFen: currentNode.currentFen,
        nodeId: currentNode.nodeId,
        ply: currentNode.ply,
        variant: 'standard',
        engineFingerprint: 'stockfish-local:16:nnue',
        requestedDepth: 18,
        realizedDepth: 18,
        multiPv: 3,
        generatedAt: '2026-04-29T00:00:00.000Z',
        maxAgeMillis: 60000,
        completed: true,
        lines: [
          { rank: 1, multiPvIndex: 1, multiPv: 3, uci: ['e2e4', 'e7e5'] },
          { rank: 2, multiPvIndex: 2, multiPv: 3, uci: ['d2d4', 'd7d5'] },
          { rank: 3, multiPvIndex: 3, multiPv: 3, uci: ['g1f3', 'g8f6'] },
        ],
      },
      childProbes: [
        {
          currentFen: childStartFen,
          nodeId: 'mainline:0/e2e4',
          ply: 6,
          variant: 'standard',
          engineFingerprint: 'stockfish-local:16:nnue',
          parentBranchId: 'root-rank-1',
          parentUciPrefix: ['e2e4'],
          parentRootRank: 1,
          requestedDepth: 18,
          realizedDepth: 18,
          multiPv: 2,
          generatedAt: '2026-04-29T00:00:01.000Z',
          maxAgeMillis: 60000,
          completed: true,
          lines: [
            { rank: 1, multiPvIndex: 1, multiPv: 2, uci: ['e7e5', 'g1f3'] },
            { rank: 2, multiPvIndex: 2, multiPv: 2, uci: ['c7c5', 'g1f3'] },
          ],
        },
      ],
    });
  });

  test('completed-probe payload helper strips display raw source debug and verdict fields', () => {
    const payload = buildCompletedProbeBridgePayload(completedProbeInput({
      rootProbe: {
        san: ['e4'],
        evalCp: 42,
        centipawn: 42,
        mate: null,
        rawPv: 'e2e4 e7e5',
        rawText: 'info depth 18 score cp 42',
        bestMove: 'e2e4',
        engineLabel: 'Stockfish display label',
        sourceRows: [{ id: 'opening-row' }],
        retrievalSnippets: ['source text'],
        debug: { raw: true },
        internal: { cache: 'secret' },
        prose: 'Play e4.',
        recommendation: 'best',
        verdict: 'winning',
        result: '1-0',
        theory: 'main line',
      },
      childProbes: [
        {
          sanHints: ['...e5'],
          eval: 'equal',
          sourceRow: { id: 'child-source' },
          debugInfo: 'secret',
          prose: 'Black answers.',
          recommendation: 'best defense',
          verdict: 'draw',
          result: '1/2-1/2',
        },
      ],
    }));

    assert.notEqual(payload, null);
    const serialized = JSON.stringify(payload);
    for (const forbiddenKey of [
      '"san"',
      '"sanHints"',
      '"eval"',
      '"evalCp"',
      '"centipawn"',
      '"mate"',
      '"rawPv"',
      '"rawText"',
      '"bestMove"',
      '"engineLabel"',
      '"sourceRows"',
      '"sourceRow"',
      '"retrievalSnippets"',
      '"debug"',
      '"debugInfo"',
      '"internal"',
      '"prose"',
      '"recommendation"',
      '"verdict"',
      '"result"',
      '"theory"',
    ])
      assert.doesNotMatch(serialized, new RegExp(forbiddenKey, 'i'));
  });

  test('completed-probe payload helper fails closed on wrong identity policy depth and UCI shape', () => {
    const cases = [
      completedProbeInput({ current: { nodeId: 'other-node' } }),
      completedProbeInput({ rootProbe: { currentFen: '8/8/8/8/8/8/8/8 w - - 0 1' } }),
      completedProbeInput({ rootProbe: { engineFingerprint: '' } }),
      completedProbeInput({ rootProbe: { completed: false } }),
      completedProbeInput({ rootProbe: { multiPv: 2, lines: rootLines(2) } }),
      completedProbeInput({ rootProbe: { multiPv: 4, lines: rootLines(4) } }),
      completedProbeInput({ childProbes: [{ multiPv: 1, lines: childLines(1) }] }),
      completedProbeInput({ childProbes: [{ multiPv: 3, lines: childLines(3) }] }),
      completedProbeInput({ rootProbe: { realizedDepth: 15 } }),
      completedProbeInput({ childProbes: [{ realizedDepth: 15 }] }),
      completedProbeInput({ rootProbe: { lines: [{ rank: 1, multiPvIndex: 1, multiPv: 3, uci: [] }] } }),
      completedProbeInput({ rootProbe: { lines: [{ rank: 1, multiPvIndex: 1, multiPv: 3, uci: ['e2-e4'] }] } }),
      completedProbeInput({ childProbes: [{ parentBranchId: '' }] }),
      completedProbeInput({ childProbes: [{ parentUciPrefix: [] }] }),
    ];

    for (const input of cases) assert.equal(buildCompletedProbeBridgePayload(input), null);
  });

  test('completed-probe payload helper fails closed on permuted root rank MultiPV pairs', () => {
    assert.equal(
      buildCompletedProbeBridgePayload(completedProbeInput({
        rootProbe: {
          lines: [
            { rank: 1, multiPvIndex: 2, multiPv: 3, uci: ['e2e4', 'e7e5'] },
            { rank: 2, multiPvIndex: 1, multiPv: 3, uci: ['d2d4', 'd7d5'] },
            { rank: 3, multiPvIndex: 3, multiPv: 3, uci: ['g1f3', 'g8f6'] },
          ],
        },
      })),
      null,
    );
  });

  test('completed-probe payload helper fails closed on permuted child rank MultiPV pairs', () => {
    assert.equal(
      buildCompletedProbeBridgePayload(completedProbeInput({
        childProbes: [
          {
            lines: [
              { rank: 1, multiPvIndex: 2, multiPv: 2, uci: ['e7e5', 'g1f3'] },
              { rank: 2, multiPvIndex: 1, multiPv: 2, uci: ['c7c5', 'g1f3'] },
            ],
          },
        ],
      })),
      null,
    );
  });

  test('completed-probe payload helper fails closed when root probe request does not match current exactly', () => {
    const rootRequest = completedProbeInput().probeRequests[0];
    const cases = [
      { ...rootRequest, currentFen: '8/8/8/8/8/8/8/8 w - - 0 1' },
      { ...rootRequest, nodeId: 'other-node' },
      { ...rootRequest, ply: currentNode.ply + 1 },
      { ...rootRequest, variant: 'chess960' },
      { ...rootRequest, multiPv: 2 },
      { ...rootRequest, requestedDepth: 17 },
      { ...rootRequest, depthFloor: 17 },
    ];

    for (const probeRequest of cases)
      assert.equal(
        buildCompletedProbeBridgePayload(completedProbeInput({ probeRequests: [probeRequest, completedProbeInput().probeRequests[1]] })),
        null,
      );
  });

  test('completed-probe payload helper fails closed when child probe requests do not match sanitized child probes', () => {
    const childRequest = completedProbeInput().probeRequests[1];
    const cases = [
      { ...childRequest, currentFen: currentNode.currentFen },
      { ...childRequest, nodeId: currentNode.nodeId },
      { ...childRequest, ply: currentNode.ply },
      { ...childRequest, variant: 'chess960' },
      { ...childRequest, multiPv: 3 },
      { ...childRequest, requestedDepth: 17 },
      { ...childRequest, depthFloor: 17 },
      { ...childRequest, parentBranchId: 'other-branch' },
      { ...childRequest, parentUciPrefix: ['d2d4'] },
      { ...childRequest, parentRootRank: 2 },
    ];

    for (const probeRequest of cases)
      assert.equal(
        buildCompletedProbeBridgePayload(completedProbeInput({ probeRequests: [completedProbeInput().probeRequests[0], probeRequest] })),
        null,
      );
  });

  test('completed-probe payload helper fails closed when a sanitized child probe has no request', () => {
    assert.equal(buildCompletedProbeBridgePayload(completedProbeInput({ probeRequests: [completedProbeInput().probeRequests[0]] })), null);
  });

  test('completed-probe payload helper treats only undefined or null childProbes as absent', () => {
    for (const childProbes of [false, 0, ''] as const) {
      const input = completedProbeInput();
      (input as any).childProbes = childProbes;
      (input as any).probeRequests = [completedProbeInput().probeRequests[0]];

      assert.equal(buildCompletedProbeBridgePayload(input), null);
    }

    assert.notEqual(
      buildCompletedProbeBridgePayload({ ...completedProbeInput(), childProbes: undefined, probeRequests: [completedProbeInput().probeRequests[0]] }),
      null,
    );
    assert.notEqual(
      buildCompletedProbeBridgePayload({ ...completedProbeInput(), childProbes: null, probeRequests: [completedProbeInput().probeRequests[0]] }),
      null,
    );
  });

  test('completed-probe payload helper deep-copies mutable source arrays', () => {
    const input = completedProbeInput();
    const payload = buildCompletedProbeBridgePayload(input);
    assert.notEqual(payload, null);

    input.rootProbe.lines[0].uci[0] = 'a2a3';
    input.childProbes![0].parentUciPrefix[0] = 'a2a3';
    input.childProbes![0].lines[0].uci.push('a7a6');
    input.probeRequests![1].parentUciPrefix![0] = 'a2a3';

    assert.deepEqual(payload.rootProbe.lines[0].uci, ['e2e4', 'e7e5']);
    assert.deepEqual(payload.childProbes[0].parentUciPrefix, ['e2e4']);
    assert.deepEqual(payload.childProbes[0].lines[0].uci, ['e7e5', 'g1f3']);
    assert.deepEqual(payload.probeRequests[1].parentUciPrefix, ['e2e4']);
  });

  test('keeps response display public-only and never exposes internal suppressions', () => {
    const decoded = decodePublicCommentaryRender(response({
      internal: {
        suppressions: [{ claimId: 'blocked-source-truth', reasons: ['source_context_only'], public: false }],
        engineIntake: { status: 'rejected', reason: 'engine_intake_rejected' },
        invalidReason: 'not a fen',
      },
      render: {
        ...baseRender('rendered'),
        suppressions: [{ claimId: 'blocked-source-truth', reasons: ['source_context_only'], public: false }],
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.equal(decoded.schemaVersion, 1);
    assert.deepEqual(decoded.blocks.map(block => block.claimId), ['claim-1']);
    assert.equal((decoded as any).internal, undefined);
    assert.equal((decoded as any).suppressions, undefined);
  });

  test('copies only public render block fields when decoding backend blocks', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        blocks: [
          {
            ...baseRender('rendered').blocks[0],
            backendOnly: 'branchId:root-candidate-1',
            cacheKey: 'cacheKey:secret',
          } as any,
        ],
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.equal(decoded.blocks[0].claimId, 'claim-1');
    assert.equal(decoded.blocks[0].text.publicText, 'Primary');
    assert.equal((decoded.blocks[0] as any).backendOnly, undefined);
    assert.equal((decoded.blocks[0] as any).cacheKey, undefined);
    assert.doesNotMatch(JSON.stringify(decoded), /branchId:root-candidate-1|cacheKey:secret/);
  });

  test('copies top-level public render metadata when decoding backend render', () => {
    const render = {
      ...baseRender('rendered'),
      evidenceRefs: [
        {
          kind: 'Certification',
          id: 'certification:claim-1',
          owner: 'white',
          anchor: 'e5',
          route: 'counterplay_resource',
          scope: 'position',
        },
      ],
      boundaries: [{ claimId: 'claim-1', reason: 'depth_floor' }],
      wording: wording('qualified_support'),
    };
    render.wording.forbiddenTerms.push('best');

    const decoded = decodePublicCommentaryRender(response({ render }));
    assert.equal(decoded.kind, 'render');
    assert.notEqual(decoded.evidenceRefs, render.evidenceRefs);
    assert.notEqual(decoded.evidenceRefs[0], render.evidenceRefs[0]);
    assert.notEqual(decoded.boundaries, render.boundaries);
    assert.notEqual(decoded.boundaries[0], render.boundaries[0]);
    assert.notEqual(decoded.wording, render.wording);
    assert.notEqual(decoded.wording.forbiddenTerms, render.wording.forbiddenTerms);

    render.evidenceRefs[0].id = 'mutated-certification';
    render.boundaries[0].reason = 'mutated-boundary';
    render.wording.maxStrength = 'hidden';
    render.wording.forbiddenTerms[0] = 'mutated-term';

    assert.deepEqual(decoded.evidenceRefs, [
      {
        kind: 'Certification',
        id: 'certification:claim-1',
        owner: 'white',
        anchor: 'e5',
        route: 'counterplay_resource',
        scope: 'position',
      },
    ]);
    assert.deepEqual(decoded.boundaries, [{ claimId: 'claim-1', reason: 'depth_floor' }]);
    assert.deepEqual(decoded.wording, {
      maxStrength: 'qualified_support',
      allowedPublicText: true,
      forbiddenTerms: ['best'],
    });
  });

  test('preserves public-safe backend variation evidence without adding UI wording', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        blocks: [
          {
            ...baseRender('rendered').blocks[0],
            variationEvidenceIds: ['proof-claim-1-resource'],
          },
        ],
        variationEvidence: [publicVariationEvidence()],
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.deepEqual(decoded.blocks[0].variationEvidenceIds, ['proof-claim-1-resource']);
    assert.deepEqual(decoded.variationEvidence, [publicVariationEvidence()]);
    assert.equal((decoded as any).variationEvidence[0].bookProse, undefined);
  });

  test('variation evidence bridge shape omits internal proof tokens and developer role wording', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        blocks: [
          {
            ...baseRender('rendered').blocks[0],
            variationEvidenceIds: ['proof-claim-1-resource'],
          },
        ],
        variationEvidence: [
          {
            ...publicVariationEvidence(),
            role: 'caution',
            proves: 'tempting_move_fails',
          } as any,
        ],
      },
    }));

    assert.equal(decoded.kind, 'render');
    const evidence = (decoded as any).variationEvidence[0];
    const serialized = JSON.stringify(decoded);
    assert.equal(Object.hasOwn(evidence, 'proves'), false);
    assert.doesNotMatch(serialized, /failed_tempting_move|tempting|proves/i);
  });

  test('variation evidence bridge drops non-public line roles from stale responses', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        blocks: [
          {
            ...baseRender('rendered').blocks[0],
            variationEvidenceIds: ['proof-claim-1-resource'],
          },
        ],
        variationEvidence: [
          {
            ...publicVariationEvidence(),
            role: 'failed_tempting_move',
          } as any,
        ],
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.equal(decoded.variationEvidence, undefined);
    assert.doesNotMatch(JSON.stringify(decoded), /failed_tempting_move|tempting|proves/i);
  });

  test('preserves structured backend line blocks even before prose text exists', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        blocks: [
          {
            ...baseRender('rendered').blocks[0],
            text: { publicText: null, forbiddenTerms: [] },
            evidenceIds: ['certification:claim-1'],
            variationEvidenceIds: ['proof-claim-1-resource'],
          },
        ],
        variationEvidence: [publicVariationEvidence()],
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.deepEqual(decoded.blocks.map(block => block.claimId), ['claim-1']);
    assert.deepEqual(decoded.blocks[0].variationEvidenceIds, ['proof-claim-1-resource']);
    assert.deepEqual(decoded.variationEvidence, [publicVariationEvidence()]);
  });

  test('omits variation evidence when the backend response does not include it', () => {
    const decoded = decodePublicCommentaryRender(response());

    assert.equal(decoded.kind, 'render');
    assert.equal(decoded.variationEvidence, undefined);
  });

  test('drops raw candidate-line and probe/cache fields from decoded variation evidence', () => {
    const decoded = decodePublicCommentaryRender(response({
      render: {
        ...baseRender('rendered'),
        variationEvidence: [
          {
            ...publicVariationEvidence(),
            CandidateLineEvidence: { branchId: 'branch-1' },
            branchId: 'branch-1',
            parentBranchId: 'branch-parent',
            engineConfigFingerprint: 'stockfish-dev',
            cacheKey: 'cache:key',
            rawLines: ['e2e4 e7e5'],
            pvLines: ['e2e4', 'e7e5'],
          } as any,
        ],
      },
    }));

    assert.equal(decoded.kind, 'render');
    const serialized = JSON.stringify(decoded);
    for (const forbidden of [
      'CandidateLineEvidence',
      'branchId',
      'parentBranchId',
      'engineConfigFingerprint',
      'cacheKey',
      'rawLines',
      'pvLines',
    ])
      assert.doesNotMatch(serialized, new RegExp(forbidden));
  });

  test('treats noCommentary hidden and negative_only as silent public output', () => {
    const noCommentary = decodePublicCommentaryRender(response({ status: 'noCommentary', noCommentary: true, render: baseRender('noCommentary') }));
    const hidden = decodePublicCommentaryRender(response({ render: { ...baseRender('rendered'), wording: wording('hidden') } }));
    const negativeOnly = decodePublicCommentaryRender(response({ render: { ...baseRender('rendered'), wording: wording('negative_only') } }));

    assert.equal(noCommentary.kind, 'empty');
    assert.equal(hidden.kind, 'empty');
    assert.equal(negativeOnly.kind, 'empty');
  });

  test('shows context only as non-authoritative backend blocks without adding chess wording', () => {
    const decoded = decodePublicCommentaryRender(response({
      status: 'contextOnly',
      render: {
        ...baseRender('contextOnly'),
        blocks: [
          {
            role: 'context',
            claimId: 'opening-context',
            text: { publicText: 'Context', forbiddenTerms: ['best', 'theory', 'forced', 'result'] },
            wordingStrength: 'context_only',
            evidenceIds: [],
            boundaries: [],
            nonAuthoritative: true,
          },
        ],
        wording: wording('context_only'),
      },
    }));

    assert.equal(decoded.kind, 'render');
    assert.deepEqual(decoded.blocks.map(block => block.text.publicText), ['Context']);
    assert.equal(decoded.blocks[0].nonAuthoritative, true);
    assert.doesNotMatch(decoded.blocks[0].text.publicText || '', /best|theory|forced|result/i);
  });

  test('discards stale node or wrong ply response before exposing render blocks', async () => {
    const applied = await fetchCommentaryRender({
      endpoint: '/api/commentary/render',
      current: currentNode,
      getCurrent: () => ({ ...currentNode, ply: currentNode.ply + 1 }),
      fetchJson: async () => response(),
    });

    assert.equal(applied.kind, 'empty');
    assert.equal(applied.reason, 'stale_node');
  });

  test('fetch sends no completed-probe or candidate-line fields inside enginePacket', async () => {
    let sentRequest: unknown;
    await fetchCommentaryRender({
      endpoint: '/api/commentary/render',
      current: currentNode,
      enginePacket: {
        rawEval: 'allowed certification intake',
        rootProbe: completedProbeInput().rootProbe,
        childProbes: completedProbeInput().childProbes,
        cacheKey: 'candidate-line-cache',
      },
      getCurrent: () => currentNode,
      fetchJson: async (_endpoint, request) => {
        sentRequest = request;
        return response();
      },
    });

    assert.equal((sentRequest as any).enginePacket, undefined);
    assert.doesNotMatch(JSON.stringify(sentRequest), /rootProbe|childProbes|cacheKey|completedProbePayload|CandidateLineEvidence/);
  });

  test('frontend bridge contract docs freeze adapter-only scope and forbidden responsibilities', () => {
    const contract = readFileSync(
      fileURLToPath(new URL('../../../modules/commentary/docs/CommentaryFrontendBridgeContract.md', import.meta.url)),
      'utf8',
    );
    const core = readFileSync(
      fileURLToPath(new URL('../../../modules/commentary/docs/CommentaryCoreSSOT.md', import.meta.url)),
      'utf8',
    );

    for (const token of [
      'CommentaryFrontendBridgeContract',
      'buildCommentaryRequest',
      'decodePublicCommentaryRender',
      'fetchCommentaryRender',
      'buildCompletedProbeBridgePayload',
      'completed-probe payload helper only',
      'not a public controller or API route field',
      'not product UI',
      'not Stockfish execution',
      'not SAN authority',
      'schemaVersion',
      'evidenceIds',
      'forbiddenTerms',
      'must not rank',
      'must not admit',
      'must not revive',
      'must not upgrade wording',
      'no product UI',
    ])
      assert.match(contract, new RegExp(token));
    assert.match(core, /CommentaryFrontendBridgeContract\.md/);
  });
});

function response(overrides: Partial<CommentaryResponse> = {}): CommentaryResponse {
  return {
    status: 'rendered',
    noCommentary: false,
    render: baseRender('rendered'),
    ...overrides,
  };
}

function baseRender(status: 'rendered' | 'contextOnly' | 'noCommentary') {
  return {
    schemaVersion: 1,
    status,
    blocks:
      status === 'noCommentary'
        ? []
        : [
            {
              role: 'primary',
              claimId: 'claim-1',
              text: { publicText: 'Primary', forbiddenTerms: [] },
              wordingStrength: status === 'contextOnly' ? 'context_only' : 'qualified_support',
              evidenceIds: [],
              boundaries: [],
              nonAuthoritative: false,
            },
          ],
    evidenceRefs: [],
    variationEvidence: undefined,
    boundaries: [],
    suppressions: [],
    wording: wording(status === 'contextOnly' ? 'context_only' : 'qualified_support', status !== 'noCommentary'),
  };
}

function wording(maxStrength: 'hidden' | 'negative_only' | 'context_only' | 'qualified_support', allowedPublicText = true) {
  return {
    maxStrength,
    allowedPublicText,
    forbiddenTerms: [],
  };
}

function publicVariationEvidence() {
  return {
    proofId: 'proof-claim-1-resource',
    boundClaimId: 'claim-1',
    startFen: currentNode.currentFen,
    owner: 'white',
    defender: 'black',
    anchor: 'e5',
    route: 'counterplay_resource',
    scope: 'position',
    role: 'resource',
    moveRole: 'defender_resource',
    lineSan: ['Nxe5', 'Nxe5'],
    lineUci: ['f3e5', 'c6e5'],
    playedMove: { san: 'Nxe5', uci: 'f3e5' },
    candidateMove: { san: 'Nxe5', uci: 'f3e5' },
    defenderResource: { san: 'Nxe5', uci: 'c6e5' },
    continuation: [{ san: 'Nxe5', uci: 'c6e5' }],
    testedMove: { san: 'Nxe5', uci: 'f3e5' },
    testedLine: [{ san: 'Nxe5', uci: 'f3e5' }],
    replyLine: [{ san: 'Nxe5', uci: 'c6e5' }],
    resourceLine: [{ san: 'Nxe5', uci: 'c6e5' }],
    testResult: 'resource_fails',
    proofPurpose: 'fails',
    provenanceRefs: [
      {
        kind: 'Certification',
        id: 'certification:claim-1',
        owner: 'white',
        anchor: 'e5',
        route: 'counterplay_resource',
        scope: 'position',
      },
    ],
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

const childStartFen = 'r1bqkbnr/pppp1ppp/2n5/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R b KQkq - 0 3';

function completedProbeInput(overrides: any = {}) {
  const current = { ...currentNode, variant: 'standard', ...(overrides.current || {}) };
  const rootProbe = {
    currentFen: currentNode.currentFen,
    nodeId: currentNode.nodeId,
    ply: currentNode.ply,
    variant: 'standard',
    engineFingerprint: 'stockfish-local:16:nnue',
    requestedDepth: 18,
    realizedDepth: 18,
    multiPv: 3,
    generatedAt: '2026-04-29T00:00:00.000Z',
    maxAgeMillis: 60000,
    completed: true,
    lines: rootLines(3),
    ...(overrides.rootProbe || {}),
  };
  const childProbes =
    overrides.childProbes === undefined
      ? [
          {
            currentFen: childStartFen,
            nodeId: 'mainline:0/e2e4',
            ply: 6,
            variant: 'standard',
            engineFingerprint: 'stockfish-local:16:nnue',
            parentBranchId: 'root-rank-1',
            parentUciPrefix: ['e2e4'],
            parentRootRank: 1,
            requestedDepth: 18,
            realizedDepth: 18,
            multiPv: 2,
            generatedAt: '2026-04-29T00:00:01.000Z',
            maxAgeMillis: 60000,
            completed: true,
            lines: childLines(2),
          },
        ]
      : [
          {
            currentFen: childStartFen,
            nodeId: 'mainline:0/e2e4',
            ply: 6,
            variant: 'standard',
            engineFingerprint: 'stockfish-local:16:nnue',
            parentBranchId: 'root-rank-1',
            parentUciPrefix: ['e2e4'],
            parentRootRank: 1,
            requestedDepth: 18,
            realizedDepth: 18,
            multiPv: 2,
            generatedAt: '2026-04-29T00:00:01.000Z',
            maxAgeMillis: 60000,
            completed: true,
            lines: childLines(2),
            ...(overrides.childProbes[0] || {}),
          },
        ];
  return {
    current,
    engineFingerprint: 'stockfish-local:16:nnue',
    budget: {
      rootMultiPv: 3,
      childMultiPv: 2,
      depthFloor: 16,
      rootTargetDepth: 18,
      childTargetDepth: 18,
      maxAgeMillis: 60000,
      ...(overrides.budget || {}),
    },
    probeRequests: overrides.probeRequests || [
      {
        role: 'root_candidate',
        currentFen: currentNode.currentFen,
        nodeId: currentNode.nodeId,
        ply: currentNode.ply,
        variant: 'standard',
        multiPv: 3,
        requestedDepth: 18,
        depthFloor: 16,
      },
      {
        role: 'defender_resource',
        currentFen: childStartFen,
        nodeId: 'mainline:0/e2e4',
        ply: 6,
        variant: 'standard',
        multiPv: 2,
        requestedDepth: 18,
        depthFloor: 16,
        parentBranchId: 'root-rank-1',
        parentUciPrefix: ['e2e4'],
        parentRootRank: 1,
      },
    ],
    rootProbe,
    childProbes,
  };
}

function rootLines(multiPv: number) {
  return [
    { rank: 1, multiPvIndex: 1, multiPv, uci: ['e2e4', 'e7e5'] },
    { rank: 2, multiPvIndex: 2, multiPv, uci: ['d2d4', 'd7d5'] },
    { rank: 3, multiPvIndex: 3, multiPv, uci: ['g1f3', 'g8f6'] },
    { rank: 4, multiPvIndex: 4, multiPv, uci: ['c2c4', 'g8f6'] },
  ].slice(0, multiPv);
}

function childLines(multiPv: number) {
  return [
    { rank: 1, multiPvIndex: 1, multiPv, uci: ['e7e5', 'g1f3'] },
    { rank: 2, multiPvIndex: 2, multiPv, uci: ['c7c5', 'g1f3'] },
    { rank: 3, multiPvIndex: 3, multiPv, uci: ['g8f6', 'g1f3'] },
  ].slice(0, multiPv);
}
