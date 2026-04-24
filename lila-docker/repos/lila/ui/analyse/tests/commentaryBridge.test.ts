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
    assert.equal((request as any).debug, undefined);
    assert.equal((request as any).ignoredSourceContext, undefined);
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
