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
      enginePacket: { nodeId: 'mainline:0', ply: 5, rawEval: 'must not display' },
      completedProbe: { rootProbe: { lines: [['e2e4']] } },
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
    assert.equal((request as any).completedProbe, undefined);
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
        completedProbePayload: { rootProbe: { lines: [['e2e4']] } },
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
            probeRequests: [{ role: 'root_candidate' }],
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
      { wrapper: { probe_payload: { rootProbe: { lines: [['e2e4']] } } } },
      { nested: { 'cache-key': 'candidate-line-cache' } },
      { nested: { internal_payload: { suppressions: [] } } },
    ]) {
      const request = buildCommentaryRequest({ current: currentNode, enginePacket });
      assert.equal(request.enginePacket, undefined);
    }
  });

  test('omits enginePacket for proof raw PV and source-row field variants', () => {
    for (const enginePacket of [
      { proofId: 'caller-proof' },
      { proves: 'best_move' },
      { rawPv: 'e2e4 e7e5' },
      { raw_lines: ['e2e4'] },
      { rawProbe: { lines: ['e2e4'] } },
      { sourceRow: { verdict: 'best' } },
    ]) {
      const request = buildCommentaryRequest({ current: currentNode, enginePacket });
      assert.equal(request.enginePacket, undefined);
    }
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
        rootProbe: { lines: [['e2e4']] },
        childProbes: [{ lines: [['e7e5']] }],
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

  test('frontend bridge contract docs freeze display-only scope and forbidden responsibilities', () => {
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
      'first display-only analyse product surface',
      'moveExplanation.ts',
      'moveExplanationView.ts',
      'POST /api/commentary/render',
      'backend-prepared block `RenderText.publicText`',
      'public SAN notation',
      'Late or overlapping responses must not overwrite',
      'completed-probe payloads',
      'not public controller/API route fields',
      'localProbe.ts',
      'server-provided analyse',
      'frontend authority over commentary truth',
      'SAN generation',
      'schemaVersion',
      'evidenceIds',
      'forbiddenTerms',
      'must not rank',
      'must not admit',
      'must not revive',
      'must not upgrade wording',
      'must not render proof ids',
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
