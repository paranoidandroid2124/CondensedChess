import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { cleanNarrativeSurfaceLabel, rewritePlayerFacingSupportText } from '../src/chesstory/signalFormatting';

type LabelCase = {
  raw: string;
  expected: string;
};

type CompensationCase = {
  raw: string;
  allowed: boolean;
};

type ContractFile = {
  labelCases: LabelCase[];
  compensationCases: CompensationCase[];
};

const contract = JSON.parse(
  readFileSync(
    fileURLToPath(
      new URL('../../../modules/llm/src/test/resources/playerFacingSupportContract.json', import.meta.url),
    ),
    'utf8',
  ),
) as ContractFile;

describe('player-facing support contract', () => {
  test('surface-label cleanup matches shared contract', () => {
    contract.labelCases.forEach(entry => {
      assert.equal(cleanNarrativeSurfaceLabel(entry.raw), entry.expected);
    });
  });

  test('compensation support filtering matches shared contract', () => {
    contract.compensationCases.forEach(entry => {
      const cleaned = rewritePlayerFacingSupportText(entry.raw, { compensationContext: true });
      assert.equal(Boolean(cleaned), entry.allowed);
    });
  });
});
