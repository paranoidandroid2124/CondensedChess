import { afterEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import {
  persistSessionBookmakerSnapshot,
  readSessionBookmakerSnapshot,
  type StoredBookmakerEntry,
} from '../src/bookmaker/studyPersistence';
import { restoreStoredBookmakerTokens } from '../src/bookmaker/stateContinuity';

describe('bookmaker session persistence', () => {
  afterEach(() => {
    delete (globalThis as { window?: Window }).window;
  });

  test('round-trips ledger and continuity tokens through session restore', () => {
    const dom = new JSDOM('', { url: 'https://example.test/analyse' });
    (globalThis as { window?: Window }).window = dom.window as unknown as Window;

    const entry: StoredBookmakerEntry = {
      html: '<div>cached</div>',
      refs: null,
      polishMeta: null,
      sourceMode: 'rule',
      model: null,
      cacheHit: true,
      mainPlansCount: 1,
      latentPlansCount: 0,
      holdReasonsCount: 1,
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

    persistSessionBookmakerSnapshot('scope', 'path', 'commentary', entry);
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
      latentPlansCount: 0,
      holdReasonsCount: 0,
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
});
