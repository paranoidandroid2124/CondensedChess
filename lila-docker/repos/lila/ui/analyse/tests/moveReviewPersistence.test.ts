import { afterEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import {
  buildStoredMoveReviewEntry,
  persistSessionMoveReviewSnapshot,
  readSessionMoveReviewSnapshot,
  sanitizeStoredMoveReviewEntry,
  type StoredMoveReviewEntry,
} from '../src/moveReview/studyPersistence';
import { restoreStoredMoveReviewTokens } from '../src/moveReview/stateContinuity';

describe('moveReview session persistence', () => {
  afterEach(() => {
    delete (globalThis as { window?: Window }).window;
    delete (globalThis as { document?: Document }).document;
  });

  test('round-trips ledger and continuity tokens through session restore', () => {
    const dom = new JSDOM('', { url: 'https://example.test/analyse' });
    (globalThis as { window?: Window }).window = dom.window as unknown as Window;
    (globalThis as { document?: Document }).document = dom.window.document;
    dom.window.document.body.dataset.cookieConsentDecided = '1';
    dom.window.document.body.dataset.cookieConsentPrefs = '1';

    const entry: StoredMoveReviewEntry = {
      html: '<div>cached</div>',
      refs: null,
      polishMeta: null,
      sourceMode: 'rule',
      model: null,
      cacheHit: true,
      mainPlansCount: 1,
      moveReviewLedger: {
        schema: 'chesstory.move_review.ledger.v1',
        motifKey: 'counterplay_restraint',
        motifLabel: 'Counterplay restraint',
        stageKey: 'restrain',
        stageLabel: 'Restrain',
        carryOver: true,
        stageReason: 'Counterplay is being cut out.',
        prerequisites: ['Keep b4 under control'],
        conversionTrigger: null,
        primaryLine: null,
        resourceLine: null,
      },
      planStateToken: {
        version: 3,
        history: {
          white: {
            primary: {
              planName: 'Kingside Expansion',
              consecutivePlies: 2,
              startingPly: 18,
              phase: 'Execution',
            },
          },
        },
      },
      endgameStateToken: {
        activePattern: null,
        patternAge: 1,
        outcomeHint: 'Unclear',
        prevKingActivityDelta: 0,
        prevConfidence: 0.4,
        lastPly: 18,
      },
      tokenContext: {
        stateKey: 'state-key',
        analysisFen: 'fen-1',
        originPath: 'path-a',
      },
    };

    persistSessionMoveReviewSnapshot('scope', 'path', 'path-a', 'commentary', entry);
    const restored = readSessionMoveReviewSnapshot('scope', 'path');
    assert(restored);
    assert.deepEqual(restored.entry.moveReviewLedger, entry.moveReviewLedger);
    assert.deepEqual(restored.entry.planStateToken, entry.planStateToken);
    assert.deepEqual(restored.entry.endgameStateToken, entry.endgameStateToken);
    assert.deepEqual(restored.entry.tokenContext, entry.tokenContext);

    const planStateByPath = new Map();
    const endgameStateByPath = new Map();
    restoreStoredMoveReviewTokens(
      restored.entry,
      { stateKey: 'state-key', analysisFen: 'fen-1', originPath: 'path-a' },
      planStateByPath,
      endgameStateByPath,
    );

    assert.deepEqual(planStateByPath.get('state-key'), entry.planStateToken);
    assert.deepEqual(endgameStateByPath.get('state-key'), entry.endgameStateToken);
  });

  test('skips token restore when stored context does not match the current analysis state', () => {
    const dom = new JSDOM('', { url: 'https://example.test/analyse' });
    (globalThis as { window?: Window }).window = dom.window as unknown as Window;

    const entry: StoredMoveReviewEntry = {
      html: '<div>cached</div>',
      refs: null,
      polishMeta: null,
      sourceMode: 'rule',
      model: null,
      cacheHit: true,
      mainPlansCount: 0,
      planStateToken: {
        version: 3,
        history: {
          white: {
            primary: {
              planName: 'Kingside Expansion',
              consecutivePlies: 2,
              startingPly: 18,
              phase: 'Execution',
            },
          },
        },
      },
      endgameStateToken: {
        activePattern: null,
        patternAge: 1,
        outcomeHint: 'Unclear',
        prevKingActivityDelta: 0,
        prevConfidence: 0.4,
        lastPly: 18,
      },
      tokenContext: {
        stateKey: 'state-key',
        analysisFen: 'fen-1',
        originPath: 'path-a',
      },
    };

    const planStateByPath = new Map();
    const endgameStateByPath = new Map();
    const restored = restoreStoredMoveReviewTokens(
      entry,
      { stateKey: 'state-key', analysisFen: 'fen-2', originPath: 'path-a' },
      planStateByPath,
      endgameStateByPath,
    );

    assert.equal(restored.planStateToken, null);
    assert.equal(restored.endgameStateToken, null);
    assert.equal(planStateByPath.has('state-key'), false);
    assert.equal(endgameStateByPath.has('state-key'), false);
  });

  test('buildStoredMoveReviewEntry derives cache metadata from decoded response shape', () => {
    const entry = buildStoredMoveReviewEntry(
      {
        refs: null,
        polishMeta: null,
        sourceMode: 'ai_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
        mainStrategicPlanCount: 1,
        moveReviewLedger: null,
        planStateToken: null,
        endgameStateToken: null,
      },
      '<div>cached</div>',
      {
        stateKey: 'state-key',
        analysisFen: 'fen-1',
        originPath: 'path-a',
      },
    );

    assert.equal(entry.mainPlansCount, 1);
    assert.equal(entry.sourceMode, 'ai_polished');
    assert.equal(entry.model, 'gpt-5-mini');
    assert.deepEqual(entry.tokenContext, {
      stateKey: 'state-key',
      analysisFen: 'fen-1',
      originPath: 'path-a',
    });
  });

  test('sanitizeStoredMoveReviewEntry strips stale internal polish diagnostics from restored entries', () => {
    const entry: StoredMoveReviewEntry = {
      html: '<div>cached</div>',
      refs: null,
      polishMeta: {
        provider: 'openai',
        model: 'gpt-test',
        sourceMode: 'ai_polished',
        validationPhase: 'middlegame',
        validationReasons: ['contract_violation'],
        cacheHit: false,
        promptTokens: 123,
        cachedTokens: 45,
        completionTokens: 67,
        estimatedCostUsd: 0.0123,
        strategyCoverage: {
          mode: 'strict',
          enforced: true,
          threshold: 0.7,
          availableCategories: 4,
          coveredCategories: 3,
          requiredCategories: 2,
          coverageScore: 0.75,
          passesThreshold: true,
          planSignals: 2,
          planHits: 1,
          routeSignals: 1,
          routeHits: 1,
          focusSignals: 1,
          focusHits: 1,
        },
      },
      sourceMode: 'ai_polished',
      model: 'gpt-test',
      cacheHit: false,
      mainPlansCount: 0,
    };

    const sanitized = sanitizeStoredMoveReviewEntry(entry);

    assert.deepEqual(sanitized.polishMeta?.validationReasons, []);
    assert.equal(sanitized.polishMeta?.promptTokens, null);
    assert.equal(sanitized.polishMeta?.cachedTokens, null);
    assert.equal(sanitized.polishMeta?.completionTokens, null);
    assert.equal(sanitized.polishMeta?.estimatedCostUsd, null);
    assert.equal(sanitized.polishMeta?.strategyCoverage, null);
  });
});
