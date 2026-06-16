import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const reviewViewSource = readFileSync(fileURLToPath(new URL('../src/review/view.ts', import.meta.url)), 'utf8');
const controlsSource = readFileSync(fileURLToPath(new URL('../src/view/controls.ts', import.meta.url)), 'utf8');
const actionMenuSource = readFileSync(fileURLToPath(new URL('../src/view/actionMenu.ts', import.meta.url)), 'utf8');
const mainViewSource = readFileSync(fileURLToPath(new URL('../src/view/main.ts', import.meta.url)), 'utf8');
const componentsSource = readFileSync(fileURLToPath(new URL('../src/view/components.ts', import.meta.url)), 'utf8');
const roundTrainingSource = readFileSync(fileURLToPath(new URL('../src/view/roundTraining.ts', import.meta.url)), 'utf8');
const treeContextMenuSource = readFileSync(fileURLToPath(new URL('../src/treeView/contextMenu.ts', import.meta.url)), 'utf8');
const pgnImportSource = readFileSync(fileURLToPath(new URL('../src/pgnImport.ts', import.meta.url)), 'utf8');
const pgnPipelineSource = readFileSync(fileURLToPath(new URL('../src/pgnPipeline.ts', import.meta.url)), 'utf8');
const moveReviewSource = readFileSync(fileURLToPath(new URL('../src/moveReview.ts', import.meta.url)), 'utf8');
const moveReviewRenderingSource = readFileSync(fileURLToPath(new URL('../src/moveReview/rendering.ts', import.meta.url)), 'utf8');
const analyseViewSource = readFileSync(fileURLToPath(new URL('../../../app/views/analyse.scala', import.meta.url)), 'utf8');
const homeSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/home.scala', import.meta.url)), 'utf8');
const importerSource = readFileSync(fileURLToPath(new URL('../../../app/views/importer.scala', import.meta.url)), 'utf8');
const accountIntelSource = readFileSync(fileURLToPath(new URL('../../../app/views/accountIntel.scala', import.meta.url)), 'utf8');
const journalSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/journal.scala', import.meta.url)), 'utf8');
const landingSource = readFileSync(fileURLToPath(new URL('../../../app/views/pages/landing.scala', import.meta.url)), 'utf8');
const moveReviewRendererSource = readFileSync(
  fileURLToPath(new URL('../../../modules/analyse/src/main/ui/MoveReviewRenderer.scala', import.meta.url)),
  'utf8',
);

describe('review player copy', () => {
  test('keeps review shell tabs and panels player-facing', () => {
    [
      'Eval and candidate lines',
      'Opening context',
      'Score sheet',
      'Load a game',
      'Board setup',
      'Coach lesson',
      'Eval and lines',
      'Book context',
      'Bring in a game',
      'Review board',
      'Nothing to show for this position yet.',
    ].forEach(copy => assert.match(reviewViewSource, new RegExp(escapeRegExp(copy)), `missing copy: ${copy}`));

    [
      'Suggested lines',
      'Board position',
      'Paste PGN',
      'Load PGN',
      'Game moves',
      'No review content yet.',
      'Review scene',
      'Compare lines',
    ].forEach(copy => assert.doesNotMatch(reviewViewSource, new RegExp(escapeRegExp(copy)), `stale tool copy: ${copy}`));
  });

  test('keeps review controls aligned with the review player labels', () => {
    ['Opening context', 'Board setup', 'Toggle candidate lines'].forEach(copy =>
      assert.match(controlsSource, new RegExp(escapeRegExp(copy)), `missing control copy: ${copy}`),
    );
    ['Candidate lines', 'candidate lines are on', 'Eval gauge', 'eval gauge visible', 'move list', 'Variation handles'].forEach(copy =>
      assert.match(actionMenuSource, new RegExp(escapeRegExp(copy)), `missing board setup copy: ${copy}`),
    );
    ['Reference lines', 'reference lines are on', 'Position gauge', 'evaluation gauge', 'move tree', 'Disclosure buttons'].forEach(copy =>
      assert.doesNotMatch(actionMenuSource, new RegExp(escapeRegExp(copy)), `stale board setup copy: ${copy}`),
    );
  });

  test('keeps saved move review fallback framed as replay', () => {
    assert.match(moveReviewSource, /Saved line to replay/);
    assert.doesNotMatch(moveReviewSource, /Saved Lines/);
  });

  test('keeps move review eval visible as core evidence', () => {
    assert.doesNotMatch(moveReviewRendererSource, /move-review-content move-review-hide-eval/);
    assert.match(moveReviewRendererSource, /attr\("aria-pressed"\) := "true"/);
    assert.match(moveReviewRendererSource, /Eval shown/);
    assert.match(moveReviewRendererSource, /s"Eval \$\{normalizedScore\(v\.scoreCp\)\}"/);
    assert.doesNotMatch(moveReviewRendererSource, /s" \(\$\{normalizedScore\(v\.scoreCp\)\}\)"/);
    assert.doesNotMatch(moveReviewRendererSource, /Eval: On/);
    assert.match(moveReviewSource, /storedBooleanPropWithEffect\('analyse\.move_review\.showEval', true/);
    assert.match(moveReviewRenderingSource, /Eval shown/);
    assert.match(moveReviewRenderingSource, /Eval hidden/);
    [moveReviewRenderingSource, moveReviewRendererSource].forEach(source => {
      assert.doesNotMatch(source, /Scores shown/);
      assert.doesNotMatch(source, /Scores hidden/);
      assert.doesNotMatch(source, /Numbers shown/);
      assert.doesNotMatch(source, /Numbers hidden/);
    });
  });

  test('keeps move review alternatives framed as playable lines', () => {
    assert.match(moveReviewRendererSource, /Other lines to try/);
    assert.doesNotMatch(moveReviewRendererSource, /Alternative Options/);
  });

  test('keeps move chips tied to the board in player-facing labels', () => {
    assert.match(moveReviewRendererSource, /Show \$\{item\.san\} on the board/);
    assert.match(moveReviewRendererSource, /Show \$\{escapeHtml\(shown\)\} on the board/);
    assert.doesNotMatch(moveReviewRendererSource, /Preview move/);
    assert.doesNotMatch(moveReviewRendererSource, /Play move/);
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
    assert.match(analyseViewSource, /review board/);
    assert.doesNotMatch(analyseViewSource, /Analysis board/);
    assert.doesNotMatch(analyseViewSource, /engine analysis/);
    assert.doesNotMatch(analyseViewSource, /analysis surface/);
  });

  test('keeps home and landing entry points framed as games, not PGN tooling', () => {
    [
      'Start from a pasted game',
      'Start from game text',
      'Saved game ready for review',
      'Pasted game',
    ].forEach(copy => assert.match(homeSource, new RegExp(escapeRegExp(copy)), `missing home copy: ${copy}`));
    [
      'Start from game text',
      'public account, a pasted game, or a board',
      'Pasted game',
      'Game text',
    ].forEach(copy => assert.match(landingSource, new RegExp(escapeRegExp(copy)), `missing landing copy: ${copy}`));

    ['Start from PGN', 'Start from a PGN', 'Saved PGN ready for review', 'Manual PGN', 'PGN import'].forEach(copy => {
      assert.doesNotMatch(homeSource, new RegExp(escapeRegExp(copy)), `stale home copy: ${copy}`);
      assert.doesNotMatch(landingSource, new RegExp(escapeRegExp(copy)), `stale landing copy: ${copy}`);
    });
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
    assert.match(importerSource, /saved games/);
    [
      "Find a player's games",
      'Find another player',
      'Game site',
      'Original game',
      'Cross-device games',
      'Sign in to keep recent games',
      'Open a pasted game or player game once',
      'Saved game ready for review',
      'Pasted game',
    ].forEach(copy => assert.match(importerSource, new RegExp(escapeRegExp(copy)), `missing importer copy: ${copy}`));
    assert.doesNotMatch(importerSource, /No saved analyses yet/);
    assert.doesNotMatch(importerSource, /saved game reads/);
    ['Manual PGN', 'Saved PGN ready for review', 'Source provider', 'Load a player', 'Load another username', 'import history'].forEach(
      copy => assert.doesNotMatch(importerSource, new RegExp(escapeRegExp(copy)), `stale importer copy: ${copy}`),
    );
  });

  test('keeps account study entry points framed as games, not PGN jobs', () => {
    ['Review one game', 'Open a pasted game', 'Study reference', 'Chesstory is still reading the games.', 'saved games'].forEach(
      copy => assert.match(accountIntelSource, new RegExp(escapeRegExp(copy)), `missing account study copy: ${copy}`),
    );
    ['Import a PGN', 'Study ID', 'The games are still being reviewed.', 'reviewed ${account.analysisCount}'].forEach(copy =>
      assert.doesNotMatch(accountIntelSource, new RegExp(escapeRegExp(copy)), `stale account study copy: ${copy}`),
    );
  });

  test('keeps recent board history framed as study, not analysis work', () => {
    assert.match(componentsSource, /Studied/);
    assert.match(componentsSource, /saved games/);
    assert.doesNotMatch(componentsSource, /['"`]Analysed['"`]/);
    assert.doesNotMatch(componentsSource, /saved game reads/);
  });

  test('keeps load-game copy framed as a board replay, not file statistics', () => {
    [
      'Load game',
      'Reset draft',
      'Recent game drafts',
      'On the board',
      'Ready to load',
      'played move',
      'score sheet line',
      'Pasted game',
      'paste a game below',
      'Position setup',
      'Game text',
      'Game text + lines',
      'Creating the new study from the current game.',
      'Game text needs fixes',
      'Saved game ready to reopen.',
    ].forEach(copy => assert.match(componentsSource, new RegExp(escapeRegExp(copy)), `missing load-game copy: ${copy}`));

    [
      'Recent PGNs',
      'half-moves',
      'PGN lines',
      'characters',
      'Pasted PGN',
      'FEN needs fixes',
      'Invalid FEN',
      'Paste a PGN',
      'The draft matches the PGN',
      'Loading this PGN',
      'PGN needs fixes',
      'current PGN',
      'Saved PGN ready to reopen.',
      'Load a PGN',
    ].forEach(copy =>
      assert.doesNotMatch(componentsSource, new RegExp(escapeRegExp(copy)), `stale file-stat copy: ${copy}`),
    );
    assert.match(pgnImportSource, /Game text needs fixes:/);
    assert.match(pgnPipelineSource, /Paste a game before opening the board\./);
    assert.doesNotMatch(pgnImportSource, /PGN error:/);
    assert.doesNotMatch(pgnPipelineSource, /Paste a PGN before importing\./);
  });

  test('keeps round summary metrics from showing engine jargon', () => {
    assert.match(roundTrainingSource, /Average move loss/);
    assert.doesNotMatch(roundTrainingSource, /Average centipawn loss/);
  });

  test('keeps move-tree context actions framed as notation, not PGN files', () => {
    ['Copy main line notation', 'Copy variation notation'].forEach(copy =>
      assert.match(treeContextMenuSource, new RegExp(escapeRegExp(copy)), `missing context menu copy: ${copy}`),
    );
    ['Copy main line PGN', 'Copy variation PGN'].forEach(copy =>
      assert.doesNotMatch(treeContextMenuSource, new RegExp(escapeRegExp(copy)), `stale context menu copy: ${copy}`),
    );
  });
});

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
