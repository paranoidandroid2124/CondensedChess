import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { buildCompactSupportSurface } from '../src/chesstory/compactSupportSurface';
import { cleanNarrativeSurfaceLabel, rewritePlayerFacingSupportText } from '../src/chesstory/signalFormatting';

describe('compact support surface', () => {
  test('keeps only player-facing rows by default', () => {
    const surface = buildCompactSupportSurface({
      signalDigest: {
        opening: 'French Defense',
        opponentPlan: 'challenge the center with ...c5',
        structuralCue: 'fixed center with kingside space',
        structureProfile: 'closed chain',
        centerState: 'Locked',
        practicalVerdict: 'The move keeps the structure under control.',
        practicalFactors: ['limits counterplay', 'keeps the king safe'],
        latentPlan: 'minority attack',
        latentReason: 'needs one free tempo',
        strategicFlow: 'stack first',
      },
      mainPlanTexts: ['Kingside expansion [Probe-backed]'],
      holdReasons: ['it still needs one free tempo first'],
      deploymentSummary: 'N toward e3 · supports kingside clamp',
    });

    assert.deepEqual(surface.mainPlanTexts, ['Kingside expansion [Probe-backed]']);
    assert.deepEqual(surface.holdReasons, ['it still needs one free tempo first']);
    assert.deepEqual(surface.rows, [
      ['Opening', 'French Defense'],
      ['Opponent', 'challenge the center with ...c5'],
      ['Structure', 'fixed center with kingside space · closed chain · locked center'],
      ['Piece deployment', 'N toward e3 · supports kingside clamp'],
      ['Practical', 'The move keeps the structure under control. · keeps the king safe'],
    ]);
  });

  test('drops support placeholders and humanizes raw strategic labels', () => {
    assert.equal(rewritePlayerFacingSupportText('Preparing e-break Break still needs more concrete support.'), null);
    assert.equal(cleanNarrativeSurfaceLabel('Preparing e-break Break'), 'Preparing the e-break');
    assert.equal(cleanNarrativeSurfaceLabel('Piece Activation'), 'Improving piece placement');
  });

  test('drops abstract compensation rows without concrete anchors', () => {
    const surface = buildCompactSupportSurface({
      signalDigest: {
        compensation: 'The material can wait while the open lines stay active.',
        investedMaterial: 100,
      },
      mainPlanTexts: ['Improving piece placement', 'Preparing the e-break'],
      holdReasons: ['Winning the material back can wait because pressure on d4 is still there.'],
      deploymentSummary: 'Improving piece placement',
    });

    assert.deepEqual(surface.mainPlanTexts, ['Preparing the e-break']);
    assert.deepEqual(surface.holdReasons, ['Winning the material back can wait because pressure on d4 is still there.']);
    assert.deepEqual(surface.rows, []);
  });

  test('keeps only concrete compensation support rows in compensation context', () => {
    assert.equal(
      rewritePlayerFacingSupportText('Winning the material back can wait because the open lines stay active.', {
        compensationContext: true,
      }),
      null,
    );
    assert.equal(
      rewritePlayerFacingSupportText('Winning the material back can wait because pressure on d4 is still there.', {
        compensationContext: true,
      }),
      'Winning the material back can wait because pressure on d4 is still there.',
    );
    assert.equal(
      rewritePlayerFacingSupportText('The rooks can take over the queenside files next.', {
        compensationContext: true,
      }),
      'The rooks can take over the queenside files next.',
    );
  });
});
