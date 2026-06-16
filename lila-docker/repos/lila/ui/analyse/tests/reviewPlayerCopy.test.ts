import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const reviewViewSource = readFileSync(fileURLToPath(new URL('../src/review/view.ts', import.meta.url)), 'utf8');
const controlsSource = readFileSync(fileURLToPath(new URL('../src/view/controls.ts', import.meta.url)), 'utf8');
const actionMenuSource = readFileSync(fileURLToPath(new URL('../src/view/actionMenu.ts', import.meta.url)), 'utf8');
const mainViewSource = readFileSync(fileURLToPath(new URL('../src/view/main.ts', import.meta.url)), 'utf8');
const moveReviewSource = readFileSync(fileURLToPath(new URL('../src/moveReview.ts', import.meta.url)), 'utf8');
const analyseViewSource = readFileSync(fileURLToPath(new URL('../../../app/views/analyse.scala', import.meta.url)), 'utf8');
const homeSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/home.scala', import.meta.url)), 'utf8');
const importerSource = readFileSync(fileURLToPath(new URL('../../../app/views/importer.scala', import.meta.url)), 'utf8');
const journalSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/journal.scala', import.meta.url)), 'utf8');
const landingSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/landing.scala', import.meta.url)), 'utf8');

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
    ['Candidate lines', 'candidate lines are on'].forEach(copy =>
      assert.match(actionMenuSource, new RegExp(escapeRegExp(copy)), `missing board setup copy: ${copy}`),
    );
    ['Reference lines', 'reference lines are on'].forEach(copy =>
      assert.doesNotMatch(actionMenuSource, new RegExp(escapeRegExp(copy)), `stale board setup copy: ${copy}`),
    );
  });

  test('keeps saved move review fallback framed as replay', () => {
    assert.match(moveReviewSource, /Saved line to replay/);
    assert.doesNotMatch(moveReviewSource, /Saved Lines/);
  });

  test('keeps loading copy framed as a review, not a technical job', () => {
    ['Position', 'Candidate lines', 'Coach lesson', 'Board replay'].forEach(copy =>
      assert.match(moveReviewSource, new RegExp(escapeRegExp(copy)), `missing loading copy: ${copy}`),
    );
    assert.match(moveReviewSource, /Review \$\{step\}\/4/);
    assert.match(moveReviewSource, /Preparing the board replay/);
    assert.doesNotMatch(moveReviewSource, /Step \$\{step\}\/4/);
    assert.doesNotMatch(moveReviewSource, /Preparing the final review/);
  });

  test('keeps current-move help framed as a player question', () => {
    [mainViewSource, landingSource].forEach(source => {
      assert.match(source, /Ask About This Move/);
      assert.doesNotMatch(source, /Explain This Move/);
    });
  });

  test('keeps board entry copy focused on returning to the board', () => {
    [homeSource, importerSource, landingSource].forEach(source => {
      assert.doesNotMatch(source, /full board/i);
      assert.doesNotMatch(source, /Open full board/);
    });
    assert.match(homeSource, /Open board/);
    assert.match(landingSource, /Board View/);
    assert.match(analyseViewSource, /Review board/);
    assert.match(analyseViewSource, /Toggle candidate lines/);
    assert.doesNotMatch(analyseViewSource, /Analysis board/);
    assert.doesNotMatch(analyseViewSource, /engine analysis/);
  });

  test('keeps journal and import history copy board-facing', () => {
    assert.match(journalSource, /what strategy still needs from the board/);
    assert.match(journalSource, /strategy-first study/);
    assert.match(journalSource, /open the board/);
    assert.match(journalSource, /Open board/);
    ['Open analysis', 'jump straight into analysis', 'strategy-first analysis', 'chess analysis'].forEach(copy =>
      assert.doesNotMatch(journalSource, new RegExp(escapeRegExp(copy)), `stale journal copy: ${copy}`),
    );

    assert.match(importerSource, /No saved games yet/);
    assert.doesNotMatch(importerSource, /No saved analyses yet/);
  });
});

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
