import { afterEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import {
  buildStoredBookmakerEntry,
  persistSessionBookmakerSnapshot,
  readSessionBookmakerSnapshot,
  type StoredBookmakerEntry,
} from '../src/bookmaker/studyPersistence';
import { restoreStoredBookmakerTokens } from '../src/bookmaker/stateContinuity';

describe('bookmaker session persistence', () => {
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

    const entry: StoredBookmakerEntry = {
      html: '<div>cached</div>',
      refs: null,
      polishMeta: null,
      sourceMode: 'rule',
      model: null,
      cacheHit: true,
      mainPlansCount: 1,
      bookmakerLedger: {
        schema: 'chesstory.bookmaker.ledger.v1',
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

    persistSessionBookmakerSnapshot('scope', 'path', 'path-a', 'commentary', entry);
    const restored = readSessionBookmakerSnapshot('scope', 'path');
    assert(restored);
    assert.deepEqual(restored.entry.bookmakerLedger, entry.bookmakerLedger);
    assert.deepEqual(restored.entry.planStateToken, entry.planStateToken);
    assert.deepEqual(restored.entry.endgameStateToken, entry.endgameStateToken);
    assert.deepEqual(restored.entry.tokenContext, entry.tokenContext);

    const planStateByPath = new Map();
    const endgameStateByPath = new Map();
    restoreStoredBookmakerTokens(
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

    const entry: StoredBookmakerEntry = {
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
    const restored = restoreStoredBookmakerTokens(
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

  test('buildStoredBookmakerEntry derives cache metadata from decoded response shape', () => {
    const entry = buildStoredBookmakerEntry(
      {
        refs: null,
        polishMeta: null,
        sourceMode: 'llm_polished',
        model: 'gpt-5-mini',
        cacheHit: false,
        mainStrategicPlans: [{
          planId: 'p1',
          planName: 'Clamp the kingside',
          rank: 1,
          score: 0.91,
          preconditions: ['Keep the center closed'],
          executionSteps: ['Bring the knight toward e3'],
          failureModes: ['...c5 opens the center'],
          viability: {
            score: 0.91,
            label: 'credible',
            risk: 'medium',
          },
        }],
        bookmakerLedger: null,
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
    assert.equal(entry.sourceMode, 'llm_polished');
    assert.equal(entry.model, 'gpt-5-mini');
    assert.deepEqual(entry.tokenContext, {
      stateKey: 'state-key',
      analysisFen: 'fen-1',
      originPath: 'path-a',
    });
  });
});
