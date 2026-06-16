import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { cleanNarrativeSurfaceLabel } from '../src/chesstory/signalFormatting';

type LabelCase = {
  raw: string;
  expected: string;
};

type ContractFile = {
  labelCases: LabelCase[];
};

const contract = JSON.parse(
  readFileSync(
    fileURLToPath(
      new URL('../../../modules/commentaryTools/src/test/resources/playerFacingSupportContract.json', import.meta.url),
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
});
