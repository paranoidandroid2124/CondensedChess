import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import {
  decodeBookmakerResponse,
  signalDigestFromResponse,
} from '../src/bookmaker/responsePayload';

describe('bookmaker response payload', () => {
  test('signal digest preserves authoring evidence alongside strategic fields', () => {
    const digest = signalDigestFromResponse({
      signalDigest: {
        authoringEvidence: 'Probe the h-file before committing.',
        dominantIdeaKind: 'prophylaxis',
        dominantIdeaFocus: 'h5',
      },
    });

    assert.equal(digest?.authoringEvidence, 'Probe the h-file before committing.');
    assert.equal(digest?.dominantIdeaKind, 'prophylaxis');
    assert.equal(digest?.dominantIdeaFocus, 'h5');
  });

  test('decodeBookmakerResponse reuses fallback prose and supporting arrays when refined payload omits them', () => {
    const decoded = decodeBookmakerResponse(
      {
        sourceMode: 'llm_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
        signalDigest: {
          authoringEvidence: 'Question remains open.',
        },
      },
      {
        html: '<p>cached html</p>',
        commentary: 'cached commentary',
        probeRequests: [
          {
            id: 'probe-1',
            fen: 'fen-1',
            moves: ['g2g4'],
            depth: 20,
          },
        ],
        authorQuestions: [
          {
            id: 'question-1',
            kind: 'plan_gap',
            priority: 1,
            question: 'Why is g4 delayed?',
            confidence: 'medium',
          },
        ],
        authorEvidence: [
          {
            questionId: 'question-1',
            questionKind: 'plan_gap',
            question: 'Why is g4 delayed?',
            status: 'pending',
            branchCount: 0,
            pendingProbeCount: 1,
          },
        ],
      },
    );

    assert.equal(decoded.html, '<p>cached html</p>');
    assert.equal(decoded.commentary, 'cached commentary');
    assert.equal(decoded.probeRequests.length, 1);
    assert.equal(decoded.authorQuestions.length, 1);
    assert.equal(decoded.authorEvidence.length, 1);
    assert.equal(decoded.signalDigest?.authoringEvidence, 'Question remains open.');
  });
});
