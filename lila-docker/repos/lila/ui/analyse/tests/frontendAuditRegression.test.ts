import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const componentsTs = readFileSync(fileURLToPath(new URL('../src/view/components.ts', import.meta.url)), 'utf8');
const ctrlTs = readFileSync(fileURLToPath(new URL('../src/ctrl.ts', import.meta.url)), 'utf8');
const keyboardTs = readFileSync(fileURLToPath(new URL('../src/keyboard.ts', import.meta.url)), 'utf8');
const analyseFreeScss = readFileSync(fileURLToPath(new URL('../css/_analyse.free.scss', import.meta.url)), 'utf8');
const chesstoryScss = readFileSync(fileURLToPath(new URL('../css/_chesstory.scss', import.meta.url)), 'utf8');
const routes = readFileSync(fileURLToPath(new URL('../../../conf/routes', import.meta.url)), 'utf8');
const userAnalysisController = readFileSync(
  fileURLToPath(new URL('../../../app/controllers/UserAnalysis.scala', import.meta.url)),
  'utf8',
);
const accountIntelController = readFileSync(
  fileURLToPath(new URL('../../../app/controllers/AccountIntel.scala', import.meta.url)),
  'utf8',
);
const accountIntelView = readFileSync(
  fileURLToPath(new URL('../../../app/views/accountIntel.scala', import.meta.url)),
  'utf8',
);
const domHandlersTs = readFileSync(fileURLToPath(new URL('../../../ui/site/src/domHandlers.ts', import.meta.url)), 'utf8');
const topBarTs = readFileSync(fileURLToPath(new URL('../../../ui/site/src/topBar.ts', import.meta.url)), 'utf8');
const cookieConsentTs = readFileSync(fileURLToPath(new URL('../../../ui/lib/src/cookieConsent.ts', import.meta.url)), 'utf8');
const layoutScala = readFileSync(
  fileURLToPath(new URL('../../../modules/web/src/main/ui/layout.scala', import.meta.url)),
  'utf8',
);
const headerButtonsScss = readFileSync(
  fileURLToPath(new URL('../../../ui/lib/css/header/_buttons.scss', import.meta.url)),
  'utf8',
);
const landingScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_landing.scss', import.meta.url)), 'utf8');
const authScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_auth.scss', import.meta.url)), 'utf8');
const accountScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_account.scss', import.meta.url)), 'utf8');
const strategicPuzzleTs = readFileSync(
  fileURLToPath(new URL('../../../ui/site/src/strategicPuzzle.ts', import.meta.url)),
  'utf8',
);
const strategicPuzzleController = readFileSync(
  fileURLToPath(new URL('../../../app/controllers/StrategicPuzzle.scala', import.meta.url)),
  'utf8',
);
const strategicPuzzleDemoView = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/strategicPuzzleDemo.scala', import.meta.url)),
  'utf8',
);
const strategicPuzzleEmptyView = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/strategicPuzzleEmpty.scala', import.meta.url)),
  'utf8',
);
const strategicPuzzleScss = readFileSync(
  fileURLToPath(new URL('../../../ui/site/css/_strategicPuzzle.scss', import.meta.url)),
  'utf8',
);
const mainController = readFileSync(fileURLToPath(new URL('../../../app/controllers/Main.scala', import.meta.url)), 'utf8');
const journalView = readFileSync(fileURLToPath(new URL('../../../app/views/pages/journal.scala', import.meta.url)), 'utf8');
const topNavScala = readFileSync(
  fileURLToPath(new URL('../../../modules/web/src/main/ui/TopNav.scala', import.meta.url)),
  'utf8',
);
const importerView = readFileSync(fileURLToPath(new URL('../../../app/views/importer.scala', import.meta.url)), 'utf8');
const narrativeCtrlTs = readFileSync(
  fileURLToPath(new URL('../src/narrative/narrativeCtrl.ts', import.meta.url)),
  'utf8',
);
const reviewViewTs = readFileSync(fileURLToPath(new URL('../src/review/view.ts', import.meta.url)), 'utf8');

describe('frontend audit regressions', () => {
  test('FEN workspace binds Enter to the relaunch path', () => {
    assert.match(componentsTs, /label\.name', 'FEN'[\s\S]*?addEventListener\('keydown', \(e: KeyboardEvent\) => \{/);
    assert.match(componentsTs, /e\.key !== 'Enter'/);
    assert.match(componentsTs, /submitFen\(\)/);
  });

  test('keyboard-help route stays wired on both frontend and server', () => {
    assert.match(keyboardTs, /htmlUrl:\s*xhr\.url\('\/analysis\/help'/);
    assert.match(routes, /^GET\s+\/analysis\/help\s+controllers\.UserAnalysis\.help$/m);
  });

  test('account intel product relies on client polling plus history navigation', () => {
    const productBlock = extractBetween(accountIntelView, 'def product', 'def status');
    assert.doesNotMatch(productBlock, /window\.location\.reload/);
    assert.match(domHandlersTs, /window\.history\.pushState/);
    assert.match(domHandlersTs, /window\.addEventListener\('popstate'/);
  });

  test('analysis direct-url parsing validates variant and fen before render', () => {
    assert.match(userAnalysisController, /Variant\(chess\.variant\.Variant\.LilaKey\(key\)\)\.fold/);
    assert.match(userAnalysisController, /Fen\.read\(variant,\s*fen\)\.isDefined/);
    assert.doesNotMatch(userAnalysisController, /Variant\.orDefault/);
  });

  test('dark theme overrides exist for review and notebook shells', () => {
    assert.match(analyseFreeScss, /html\.dark \.analyse-review \{/);
    assert.match(chesstoryScss, /html\.dark \{/);
    assert.match(chesstoryScss, /\.notebook-shell \{/);
    assert.match(chesstoryScss, /\.notebook-hero \{/);
    assert.match(chesstoryScss, /\.analyse \{/);
  });

  test('top navigation exposes import and account intel surfaces', () => {
    assert.match(topNavScala, /item\("\/import", "Import", isOn\("\/import"\)\)/);
    assert.match(topNavScala, /item\("\/account-intel", "Account Intel", isOn\("\/account-intel"\)\)/);
    assert.match(topNavScala, /item\("\/journal", "Journal", isOn\("\/journal"\)\)/);
    assert.match(topNavScala, /item\("\/strategic-puzzle", "Strategic Puzzles", isOn\("\/strategic-puzzle"\)\)/);
    assert.match(importerView, /div\(cls := "importer-hero__eyebrow"\)\("Import"\)/);
  });

  test('analysis import keeps an explicit CTA on mobile', () => {
    assert.match(componentsTs, /const submitPgnDraft = \(\) => \{/);
    assert.match(componentsTs, /\[icon\(licon\.PlayTriangle as any\), ' Import PGN'\]/);
    assert.doesNotMatch(componentsTs, /!isMobile\(\)\s*&&[\s\S]{0,240}Import PGN/);
  });

  test('strategic puzzle next navigation restores prior puzzle state through browser history', () => {
    assert.match(strategicPuzzleTs, /window\.addEventListener\('popstate', this\.onPopState\)/);
    assert.match(strategicPuzzleTs, /history\.replaceState\(\{ strategicPuzzle: this\.historySnapshot\(url\) \}, '', url\)/);
    assert.match(strategicPuzzleTs, /history\.pushState\(\{ strategicPuzzle: this\.historySnapshot\(url\) \}, '', url\)/);
    assert.match(strategicPuzzleTs, /this\.restoreSnapshot\(snapshot\)/);
  });

  test('analysis move navigation pushes user-visible ply history and restores it on popstate', () => {
    assert.match(ctrlTs, /window\.addEventListener\('popstate', this\.onHistoryPopState\)/);
    assert.match(ctrlTs, /window\.history\.pushState\(state, '', url\)/);
    assert.match(ctrlTs, /this\.jump\(path, 'push'\)/);
    assert.match(ctrlTs, /const targetPath = this\.mainlinePlyToPath/);
  });

  test('theme choice survives cookie-consent acceptance', () => {
    assert.match(domHandlersTs, /let pendingThemeChoice: string \| null = null;/);
    assert.match(domHandlersTs, /pendingThemeChoice = choice;\s*openCookieConsent\(\);/);
    assert.match(domHandlersTs, /setCookieConsent\('preferences'\);\s*applyPendingThemeChoice\(\);/);
    assert.match(domHandlersTs, /const choice = pendingThemeChoice;\s*pendingThemeChoice = null;\s*submitThemeChoice\(choice\);/);
  });

  test('shell accessibility keeps visible nav controls and modal cookie settings focus management', () => {
    assert.match(layoutScala, /class="\$burgerClass js-topnav-toggle"/);
    assert.match(layoutScala, /aria-controls="\$navId"/);
    assert.match(layoutScala, /attr\("aria-modal"\) := "true"/);
    assert.match(topBarTs, /const handleNavKeydown = \(event: KeyboardEvent\) => \{/);
    assert.match(topBarTs, /event\.key === 'Escape'/);
    assert.match(cookieConsentTs, /function onConsentKeydown\(event: KeyboardEvent\): void \{/);
    assert.match(cookieConsentTs, /export function syncCookieConsentDialogState\(\): void \{/);
  });

  test('Game Chronicle can analyze a staged PGN draft and no longer nukes direct URLs for engine selection', () => {
    assert.match(componentsTs, /void ctrl\.openNarrative\(pgnInspection\.status === 'ready' \? draftPgn : undefined\);/);
    assert.match(narrativeCtrlTs, /fetchNarrative = async \(pgnOverride\?: string \| null\) => \{/);
    assert.match(narrativeCtrlTs, /const pgn = stagedPgn && stagedPgn !== currentPgn \? stagedPgn : currentPgn;/);
    assert.match(ctrlTs, /url\.searchParams\.delete\('engine'\);/);
    assert.match(ctrlTs, /window\.history\.replaceState\(window\.history\.state, '', `\$\{url\.pathname\}\$\{url\.search\}\$\{url\.hash\}`\);/);
    assert.doesNotMatch(ctrlTs, /site\.redirect\('\/analysis'\)/);
  });

  test('review reference actions and notebook links stay honest about where they go', () => {
    assert.match(reviewViewTs, /'Back to Moves'/);
    assert.match(componentsTs, /const notebookUrl =/);
    assert.match(componentsTs, /notebookTarget\.pathname \+ notebookTarget\.search === window\.location\.pathname \+ window\.location\.search/);
  });

  test('account intel history preserves run-specific permalinks through server and client state', () => {
    assert.match(accountIntelController, /"selectedJobId" -> state\.selectedJobId/);
    assert.match(accountIntelController, /accountResultUrl\(job\.provider, job\.username, job\.kind, jobId = Some\(job\.id\)\)/);
    assert.match(accountIntelView, /Viewing run/);
    assert.match(domHandlersTs, /let currentSelectedJobId = state\.selectedJobId \|\| null;/);
    assert.match(domHandlersTs, /url\.searchParams\.set\('jobId', jobId\);/);
  });

  test('strategic puzzle exposes live entry points and a non-404 exhausted state', () => {
    assert.match(strategicPuzzleController, /_\.fold\(renderEmptyPage\):/);
    assert.match(strategicPuzzleEmptyView, /No unseen strategic puzzle is queued right now\./);
    assert.match(strategicPuzzleDemoView, /a\(href := routes\.StrategicPuzzle\.home\.url, cls := "sp-demo-link"\)\("Open live puzzle"\)/);
    assert.doesNotMatch(strategicPuzzleDemoView, /button\(tpe := "button", cls := "sp-choice/);
    assert.match(strategicPuzzleDemoView, /div\(cls := "sp-choice is-primary", role := "listitem"\)/);
  });

  test('support route canonicalizes /plan and theme styles expose focus and busy states', () => {
    assert.match(routes, /^GET\s+\/plan\s+controllers\.Main\.plan$/m);
    assert.match(mainController, /def plan = Open:\s*Redirect\(routes\.Main\.support, MOVED_PERMANENTLY\)\.toFuccess/);
    assert.match(headerButtonsScss, /&:focus-visible/);
    assert.match(headerButtonsScss, /&\[aria-busy='true'\]/);
    assert.match(landingScss, /&:focus-visible/);
    assert.match(landingScss, /&\[aria-busy='true'\]/);
    assert.match(authScss, /\.auth-theme-switch__button:focus-visible/);
    assert.match(authScss, /\.auth-theme-switch__button\[aria-busy='true'\]/);
  });

  test('dark-theme contrast fixes cover account warnings and strategic puzzle overlays', () => {
    assert.match(accountScss, /p\.desc[\s\S]*?html\.dark &/);
    assert.match(accountScss, /\.warning-box[\s\S]*?html\.dark &/);
    assert.match(accountScss, /\.danger-box[\s\S]*?html\.dark &/);
    assert.match(strategicPuzzleScss, /html\.dark \.sp-demo-page::after \{/);
    assert.match(strategicPuzzleScss, /html\.dark \.sp-empty-state \{/);
  });

  test('journal root stays an archive landing instead of duplicating the latest article body', () => {
    assert.match(mainController, /def journal = Open:\s*Ok\.page\(views\.pages\.journal\(journalContent\.all, None\)\)/);
    assert.match(journalView, /h1\("Browse the archive or start with the latest note\."\)/);
    assert.match(journalView, /"The journal root stays as an archive landing so each published note has a single article URL\."/);
  });
});

function extractBetween(source: string, start: string, end: string): string {
  const startIndex = source.indexOf(start);
  assert.notEqual(startIndex, -1, `missing block start: ${start}`);
  const endIndex = source.indexOf(end, startIndex);
  assert.notEqual(endIndex, -1, `missing block end: ${end}`);
  return source.slice(startIndex, endIndex);
}
