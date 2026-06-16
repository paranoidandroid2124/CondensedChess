import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const reviewViewSource = readFileSync(fileURLToPath(new URL('../src/review/view.ts', import.meta.url)), 'utf8');
const controlsSource = readFileSync(fileURLToPath(new URL('../src/view/controls.ts', import.meta.url)), 'utf8');

describe('review player copy', () => {
  test('keeps review shell tabs and panels player-facing', () => {
    [
      'Candidate lines',
      'Opening context',
      'Score sheet',
      'Load PGN',
      'Board setup',
      'Review board',
      'Nothing to show for this position yet.',
    ].forEach(copy => assert.match(reviewViewSource, new RegExp(escapeRegExp(copy)), `missing copy: ${copy}`));

    ['Suggested lines', 'Board position', 'Paste PGN', 'Game moves', 'No review content yet.'].forEach(copy =>
      assert.doesNotMatch(reviewViewSource, new RegExp(escapeRegExp(copy)), `stale tool copy: ${copy}`),
    );
  });

  test('keeps review controls aligned with the review player labels', () => {
    ['Opening context', 'Board setup', 'Toggle candidate lines'].forEach(copy =>
      assert.match(controlsSource, new RegExp(escapeRegExp(copy)), `missing control copy: ${copy}`),
    );
  });
});

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
