import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const componentsTs = readFileSync(fileURLToPath(new URL('../src/view/components.ts', import.meta.url)), 'utf8');
const ctrlTs = readFileSync(fileURLToPath(new URL('../src/ctrl.ts', import.meta.url)), 'utf8');
const keyboardTs = readFileSync(fileURLToPath(new URL('../src/keyboard.ts', import.meta.url)), 'utf8');
const analyseFreeScss = readFileSync(fileURLToPath(new URL('../css/_analyse.free.scss', import.meta.url)), 'utf8');
const analyseLayoutScss = readFileSync(fileURLToPath(new URL('../css/_layout.scss', import.meta.url)), 'utf8');
const chesstoryScss = readFileSync(fileURLToPath(new URL('../css/_chesstory.scss', import.meta.url)), 'utf8');
const studyIndexScss = readFileSync(fileURLToPath(new URL('../css/study/_index.scss', import.meta.url)), 'utf8');
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
const baseScss = readFileSync(fileURLToPath(new URL('../../../ui/lib/css/layout/_base.scss', import.meta.url)), 'utf8');
const chesstoryOverridesScss = readFileSync(
  fileURLToPath(new URL('../../../ui/lib/css/page/_chesstory-overrides.scss', import.meta.url)),
  'utf8',
);
const headerScss = readFileSync(fileURLToPath(new URL('../../../ui/lib/css/header/_header.scss', import.meta.url)), 'utf8');
const titleScss = readFileSync(
  fileURLToPath(new URL('../../../ui/lib/css/header/_title.scss', import.meta.url)),
  'utf8',
);
const headerButtonsScss = readFileSync(
  fileURLToPath(new URL('../../../ui/lib/css/header/_buttons.scss', import.meta.url)),
  'utf8',
);
const landingScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_landing.scss', import.meta.url)), 'utf8');
const landingScala = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/landing.scala', import.meta.url)),
  'utf8',
);
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
const strategicPuzzlePageView = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/strategicPuzzlePage.scala', import.meta.url)),
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
const bookmakerTs = readFileSync(fileURLToPath(new URL('../src/bookmaker.ts', import.meta.url)), 'utf8');
const narrativeViewTs = readFileSync(
  fileURLToPath(new URL('../src/narrative/narrativeView.ts', import.meta.url)),
  'utf8',
);
const reviewViewTs = readFileSync(fileURLToPath(new URL('../src/review/view.ts', import.meta.url)), 'utf8');
const controlsTs = readFileSync(fileURLToPath(new URL('../src/view/controls.ts', import.meta.url)), 'utf8');
const signalFormattingTs = readFileSync(
  fileURLToPath(new URL('../src/chesstory/signalFormatting.ts', import.meta.url)),
  'utf8',
);

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

  test('top navigation stays focused on core workspaces', () => {
    assert.match(topNavScala, /item\("\/", "Home", ctx\.req\.path == "\/", mobileOnly = true\)/);
    assert.match(topNavScala, /item\("\/import", "Import Games", isOn\("\/import"\), Some\("Import recent public games"\)\)/);
    assert.match(topNavScala, /item\("\/account-intel", "Account Reports", isOn\("\/account-intel"\), Some\("Open account reports"\)\)/);
    assert.match(topNavScala, /item\("\/analysis", "Analysis", isOn\("\/analysis"\)\)/);
    assert.match(topNavScala, /item\("\/strategic-puzzle", "Strategic Puzzles", isOn\("\/strategic-puzzle"\)\)/);
    assert.match(topNavScala, /item\("\/notebook", "Notebook", isOn\("\/notebook"\)\)/);
    assert.doesNotMatch(topNavScala, /item\("\/journal", "Journal", isOn\("\/journal"\)\)/);
    assert.doesNotMatch(topNavScala, /item\("\/support", "Support", isOn\("\/support"\)/);
    assert.match(importerView, /div\(cls := "importer-hero__eyebrow"\)\("Recent games"\)/);
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
    assert.match(strategicPuzzleTs, /puzzleId: this\.payload\.puzzle\.id/);
    assert.doesNotMatch(strategicPuzzleTs, /payload: this\.payload/);
    assert.match(strategicPuzzleTs, /this\.restoreSnapshot\(snapshot\)/);
  });

  test('strategic puzzle solved-next flow reloads by puzzle id instead of reusing next bootstrap payloads', () => {
    assert.match(routes, /^GET\s+\/api\/strategic-puzzle\/:id\s+controllers\.StrategicPuzzle\.showJson\(id\)$/m);
    assert.match(strategicPuzzleTs, /await fetch\(`\/api\/strategic-puzzle\/\$\{encodeURIComponent\(puzzleId\)\}`\)/);
    assert.doesNotMatch(strategicPuzzleTs, /fetch\(`\/api\/strategic-puzzle\/next\?after=/);
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

  test('shared container tiers anchor audited surfaces to shell tokens instead of local max-widths', () => {
    assert.match(baseScss, /---cs-content-max-width:/);
    assert.match(baseScss, /---cs-workspace-max-width:/);
    assert.match(baseScss, /---site-header-sticky-padding: max\(/);
    assert.match(chesstoryOverridesScss, /---cs-content-max-width: 1320px;/);
    assert.match(chesstoryOverridesScss, /---cs-workspace-max-width: 1520px;/);
    assert.match(chesstoryOverridesScss, /---cs-shell-gutter-x: clamp\(1rem, 2\.2vw, 2rem\);/);
    assert.match(chesstoryOverridesScss, /---cs-shell-block-gap: clamp\(0\.75rem, 1\.4vw, 1\.5rem\);/);
    assert.match(chesstoryOverridesScss, /#main-wrap \{\s*---main-max-width: var\(---cs-workspace-max-width\);/);
    assert.match(headerScss, /var\(---cs-workspace-max-width, 1600px\)/);
    assert.match(landingScss, /--landing-shell-max-width: var\(---cs-content-max-width, 1320px\);/);
    assert.match(landingScss, /width: min\(calc\(100% - \(var\(--landing-shell-gutter\) \* 2\)\), var\(--landing-shell-max-width\)\);/);
    assert.doesNotMatch(landingScss, /--landing-max-width: 1520px/);
    assert.match(authScss, /var\(---cs-content-max-width, 1320px\)/);
    assert.match(authScss, /\.auth-container--wide \{[\s\S]*?width: 100%;[\s\S]*?padding: var\(---cs-shell-block-gap/);
    assert.doesNotMatch(authScss, /\.auth-container--wide \{[\s\S]*?max-width: 1120px;/);
    assert.match(analyseLayoutScss, /---main-max-width: var\(---cs-workspace-max-width, 1520px\);/);
    assert.match(analyseLayoutScss, /padding: \$analyse-block-gap \$analyse-shell-gutter-x;/);
    assert.match(studyIndexScss, /var\(---cs-content-max-width, 1320px\)/);
    assert.match(chesstoryScss, /\.notebook-shell \{[\s\S]*?var\(---cs-workspace-max-width, 100%\)/);
    assert.match(chesstoryScss, /\.notebook-shell > \.analyse \{[\s\S]*?padding: 0;/);
    assert.match(strategicPuzzleScss, /var\(---cs-workspace-max-width/);
    assert.doesNotMatch(strategicPuzzleScss, /width: min\(100%, 1460px\)/);
  });

  test('Game Chronicle can analyze a staged PGN draft and no longer nukes direct URLs for engine selection', () => {
    assert.match(componentsTs, /void ctrl\.openNarrative\(pgnInspection\.status === 'ready' \? draftPgn : undefined\);/);
    assert.match(narrativeCtrlTs, /fetchNarrative = async \(pgnOverride\?: string \| null\) => \{/);
    assert.match(narrativeCtrlTs, /const pgn = stagedPgn && stagedPgn !== currentPgn \? stagedPgn : currentPgn;/);
    assert.match(ctrlTs, /url\.searchParams\.delete\('engine'\);/);
    assert.match(ctrlTs, /window\.history\.replaceState\(window\.history\.state, '', `\$\{url\.pathname\}\$\{url\.search\}\$\{url\.hash\}`\);/);
    assert.doesNotMatch(ctrlTs, /site\.redirect\('\/analysis'\)/);
  });

  test('Game Chronicle persists across refresh for the same analysis context', () => {
    assert.match(narrativeCtrlTs, /const NARRATIVE_SESSION_STORAGE_KEY = 'analyse\.game-chronicle\.session\.v2';/);
    assert.match(narrativeCtrlTs, /syncPersistedNarrative = \(\): void => \{/);
    assert.match(narrativeCtrlTs, /this\.persistNarrativeResponse\(response, pgnOverride\);/);
    assert.match(narrativeCtrlTs, /tempStorage\.set\(NARRATIVE_SESSION_STORAGE_KEY, JSON\.stringify\(entries\)\);/);
    assert.match(ctrlTs, /this\.narrative\.syncPersistedNarrative\(\);/);
    assert.match(ctrlTs, /this\.narrative\?\.syncPersistedNarrative\(\);/);
  });

  test('review shell keeps import primary and moves utilities out of the main tab row', () => {
    assert.match(reviewViewTs, /\{ tab: 'import', label: 'Import PGN' \}/);
    assert.doesNotMatch(reviewViewTs, /\{ tab: 'reference', label: 'Reference' \}/);
    assert.match(reviewViewTs, /renderUtilityPanel\(ctrl, nodes\)/);
    assert.match(reviewViewTs, /'Opening Explorer'/);
    assert.match(reviewViewTs, /'Close panel'/);
    assert.doesNotMatch(reviewViewTs, /Reference sections/);
    assert.match(reviewViewTs, /Paste a PGN or jump by FEN without leaving this analysis shell\./);
    assert.match(reviewViewTs, /'Turn On Engine'/);
    assert.match(reviewViewTs, /Use the engine switch in the header above to start local Stockfish/);
    assert.match(reviewViewTs, /cleanNarrativeSurfaceLabel/);
    assert.match(reviewViewTs, /bindPreviewHover/);
    assert.match(reviewViewTs, /analyse-review__tabs-head/);
    assert.match(controlsTs, /fbt--engine-toggle/);
    assert.match(controlsTs, /title: reviewShell \? 'Opening explorer' : 'Opening explorer and Tablebase'/);
    assert.match(controlsTs, /title: reviewShell \? 'Board view and settings' : 'Menu'/);
    assert.match(controlsTs, /showLabel = displayColumns\(\) > 1/);
    assert.match(componentsTs, /analyse-review__engine-stack/);
    assert.match(componentsTs, /analyse-review__hover-preview-wrap/);
    assert.match(componentsTs, /'Open recent games'/);
    assert.match(analyseFreeScss, /&__next-actions[\s\S]*?> :last-child:nth-child\(odd\)[\s\S]*?grid-column: 1 \/ -1/);
    assert.match(analyseFreeScss, /&__hover-preview-wrap/);
    assert.match(analyseFreeScss, /&__utility/);
    assert.match(componentsTs, /const notebookUrl =/);
    assert.match(componentsTs, /notebookTarget\.pathname \+ notebookTarget\.search === window\.location\.pathname \+ window\.location\.search/);
  });

  test('review shell state migrates from v1 reference tabs to v2 utility panels', () => {
    assert.match(ctrlTs, /const legacyReviewStateStorageKey = 'analyse\.review-shell\.v1';/);
    assert.match(ctrlTs, /const reviewStateStorageKey = 'analyse\.review-shell\.v2';/);
    assert.match(ctrlTs, /storedPrimaryTab === 'reference'/);
    assert.match(ctrlTs, /storedReferenceTab === 'import'/);
    assert.match(ctrlTs, /storedReferenceTab === 'explorer' \|\| storedReferenceTab === 'board'/);
    assert.match(ctrlTs, /utilityPanel: null/);
    assert.match(ctrlTs, /this\.setReviewUtilityPanel\(this\.reviewUtilityPanel\(\) === 'explorer' \? null : 'explorer'\)/);
    assert.match(ctrlTs, /this\.setReviewUtilityPanel\(this\.reviewUtilityPanel\(\) === 'board' \? null : 'board'\)/);
  });

  test('recent games intake stays utility-first on the index page', () => {
    assert.match(importerView, /div\(cls := "importer-grid importer-grid--intake"\)/);
    assert.doesNotMatch(importerView, /What happens next/);
    assert.doesNotMatch(importerView, /Why this surface works/);
    assert.doesNotMatch(importerView, /importer-timeline/);
    assert.doesNotMatch(importerView, /importer-note-grid/);
    assert.match(authScss, /\.importer-grid--intake/);
    assert.doesNotMatch(authScss, /\.importer-note-grid/);
    assert.doesNotMatch(authScss, /\.importer-timeline/);
  });

  test('account intel history preserves run-specific permalinks through server and client state', () => {
    assert.match(accountIntelController, /"selectedJobId" -> state\.selectedJobId/);
    assert.match(accountIntelController, /accountResultUrl\(job\.provider, job\.username, job\.kind, jobId = Some\(job\.id\)\)/);
    assert.match(accountIntelView, /Viewing run/);
    assert.match(domHandlersTs, /let currentSelectedJobId = state\.selectedJobId \|\| null;/);
    assert.match(domHandlersTs, /url\.searchParams\.set\('jobId', jobId\);/);
  });

  test('account intel landing captures mode before redirecting to a result page', () => {
    assert.match(accountIntelController, /val requestedKindKey = get\("kind"\)\.filter\(_\.nonEmpty\)\.getOrElse\(defaultKind\.key\)/);
    assert.match(accountIntelController, /Redirect\(accountResultUrl\(validProvider, validUsername, requestedKind\)\)/);
    assert.match(accountIntelView, /name := "kind"/);
    assert.match(accountIntelView, /"My Account"/);
    assert.match(accountIntelView, /"Opponent Prep"/);
  });

  test('account intel status page gates the result CTA until success', () => {
    assert.match(accountIntelView, /statusPrimaryAction\(job, resultHref\)/);
    assert.match(accountIntelView, /Analysis still running/);
    assert.match(accountIntelView, /submitButton\(cls := "status-links__primary"\)\("Run again"\)/);
  });

  test('strategic puzzle exposes live entry points and a non-404 exhausted state', () => {
    const demoRouteIndex = routes.indexOf('GET   /strategic-puzzle/demo           controllers.Main.strategicPuzzleDemo');
    const showRouteIndex = routes.indexOf('GET   /strategic-puzzle/:id            controllers.StrategicPuzzle.show(id)');
    assert.ok(demoRouteIndex >= 0 && showRouteIndex >= 0 && demoRouteIndex < showRouteIndex);
    assert.match(strategicPuzzleController, /_\.fold\(renderEmptyPage\):/);
    assert.match(strategicPuzzleEmptyView, /No live strategic puzzle is ready right now\./);
    assert.match(strategicPuzzleEmptyView, /finished the puzzles that are currently available/);
    assert.doesNotMatch(strategicPuzzleEmptyView, /live shells/);
    assert.match(strategicPuzzleController, /No live strategic puzzle is ready right now\./);
    assert.match(strategicPuzzleController, /We could not record that puzzle result\./);
    assert.match(strategicPuzzlePageView, /enable JavaScript to solve the live puzzle and open the review\./);
    assert.doesNotMatch(strategicPuzzlePageView, /\.flag\(_\.noHeader\)/);
    assert.doesNotMatch(strategicPuzzlePageView, /\.flag\(_\.fullScreen\)/);
    assert.doesNotMatch(strategicPuzzlePageView, /sp-demo-header/);
    assert.doesNotMatch(strategicPuzzleEmptyView, /\.flag\(_\.noHeader\)/);
    assert.doesNotMatch(strategicPuzzleEmptyView, /\.flag\(_\.fullScreen\)/);
    assert.doesNotMatch(strategicPuzzleEmptyView, /sp-demo-header/);
    assert.doesNotMatch(strategicPuzzleDemoView, /sp-demo-header/);
    assert.match(strategicPuzzleDemoView, /a\(href := routes\.StrategicPuzzle\.home\.url, cls := "sp-demo-link"\)\("Try a live puzzle"\)/);
    assert.doesNotMatch(strategicPuzzleDemoView, /button\(tpe := "button", cls := "sp-choice/);
    assert.match(strategicPuzzleDemoView, /Move list stays hidden/);
    assert.doesNotMatch(strategicPuzzleDemoView, /div\(cls := "sp-choice is-primary", role := "listitem"\)/);
    assert.doesNotMatch(strategicPuzzleDemoView, /Plan-first puzzle shell/);
    assert.doesNotMatch(strategicPuzzleDemoView, /Dominant family key/);
    assert.match(strategicPuzzleTs, /Give up and open review/);
    assert.match(strategicPuzzleTs, /Started position/);
    assert.match(strategicPuzzleTs, /How The Task Begins/);
    assert.match(strategicPuzzleTs, /Open exact proof on the board/);
    assert.match(strategicPuzzleTs, /The featured review is open from the stored start\./);
    assert.match(strategicPuzzleTs, /The board above is paused immediately after/);
    assert.match(strategicPuzzleTs, /The board is now showing the confirmed continuation\./);
    assert.match(strategicPuzzleTs, /data-action="show-proof-board"/);
    assert.match(strategicPuzzleTs, /data-action="show-start-board"/);
    assert.doesNotMatch(strategicPuzzleTs, /Reveal best line/);
    assert.doesNotMatch(strategicPuzzleTs, /Three-step shell/);
    assert.doesNotMatch(strategicPuzzleTs, /quality score/);
    assert.doesNotMatch(strategicPuzzleTs, /active choices/);
    assert.doesNotMatch(strategicPuzzleTs, /Give up and reveal the line/);
    assert.doesNotMatch(strategicPuzzleTs, /data-action="hint"/);
    assert.match(strategicPuzzleScss, /\.sp-live-page \.sp-demo-lines:not\(\.is-open\) \{/);
    assert.match(strategicPuzzleScss, /\.sp-demo-panel--solve \{\s*position: static;/);
    assert.match(strategicPuzzleScss, /\.sp-runtime-board-stage\.is-success/);
    assert.match(strategicPuzzleScss, /@keyframes sp-board-shake/);
    assert.match(strategicPuzzleScss, /\.sp-runtime-actions__note \{/);
    assert.match(strategicPuzzleScss, /\.sp-runtime-actions \.sp-demo-link \{\s*border: none;\s*cursor: pointer;\s*width: auto;/);
    assert.match(strategicPuzzleScss, /\.sp-runtime-actions \{\s*flex-direction: column;\s*align-items: stretch;/);
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

  test('shared shell header uses the Chesstory logo asset and landing no longer renders a custom header clone', () => {
    assert.match(layoutScala, /staticAssetUrl\("logo\/chesstory\.svg"\)/);
    assert.match(titleScss, /\.cs-mark[\s\S]*?&__img/);
    assert.doesNotMatch(landingScala, /header\(cls := "landing-header"/);
    assert.doesNotMatch(landingScala, /landing-theme-switch/);
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

  test('narrative surface labels are cleaned before rendering review chips and badges', () => {
    assert.match(signalFormattingTs, /export function cleanNarrativeSurfaceLabel/);
    assert.match(signalFormattingTs, /export function cleanNarrativeProseText/);
    assert.match(signalFormattingTs, /export function summarizeReviewMomentProse/);
    assert.match(signalFormattingTs, /replace\(\/\\\*\\\*\/g, ''\)/);
    assert.match(signalFormattingTs, /Played Line/);
    assert.match(reviewViewTs, /cleanNarrativeProseText\(data\.intro\)/);
    assert.match(reviewViewTs, /compact: true/);
    assert.match(reviewViewTs, /Turn on local engine from the header above/);
  });

  test('bookmaker fallback guard still blocks internal commentary leak tokens', () => {
    assert.match(bookmakerTs, /PlayableByPV\|PlayedPV\|return vector\|cash out/i);
    assert.match(bookmakerTs, /engine-coupled continuation/);
  });

  test('bookmaker and chronicle default support surfaces use the compact player-facing panel with collapsed details', () => {
    assert.match(bookmakerTs, /buildCompactSupportSurface/);
    assert.match(bookmakerTs, /bookmaker-strategic-summary__title">Support/);
    assert.match(bookmakerTs, /<details class="bookmaker-strategic-summary__details">/);
    assert.match(bookmakerTs, /Advanced details/);

    assert.match(narrativeViewTs, /buildCompactSupportSurface/);
    assert.match(narrativeViewTs, /const decisionComparison = digest\?\.decisionComparison;/);
    assert.doesNotMatch(narrativeViewTs, /narrativeFallbackDecisionComparison/);
    assert.match(narrativeViewTs, /h3\.narrative-signal-title', 'Support'/);
    assert.match(narrativeViewTs, /details\.narrative-advanced-details/);
    assert.match(narrativeViewTs, /summary\.narrative-advanced-details__summary', 'Advanced details'/);
  });

  test('decision comparison surfaces only read canonical comparative digest fields for the new exact lane', () => {
    const bookmakerDecisionBlock = extractBetween(
      bookmakerTs,
      'function renderDecisionCompareStrip(',
      'type BookmakerStrategySurface = {',
    );
    const narrativeDecisionBlock = extractBetween(
      narrativeViewTs,
      'function narrativeDecisionMoveStrip(',
      'function narrativeDecisionMoveChip(',
    );

    assert.match(bookmakerDecisionBlock, /comparison\?\.comparativeConsequence\?\.trim\(\) \? comparison\?\.comparedMove/);
    assert.match(bookmakerDecisionBlock, /renderBookmakerMoveChip\('Compared'/);
    assert.match(narrativeDecisionBlock, /comparison\.comparativeConsequence[\s\S]*narrativeDecisionMoveChip\('Compared'/);
    assert.doesNotMatch(bookmakerDecisionBlock, /topEngineMove/);
    assert.doesNotMatch(narrativeDecisionBlock, /topEngineMove/);
  });
});

function extractBetween(source: string, start: string, end: string): string {
  const startIndex = source.indexOf(start);
  assert.notEqual(startIndex, -1, `missing block start: ${start}`);
  const endIndex = source.indexOf(end, startIndex);
  assert.notEqual(endIndex, -1, `missing block end: ${end}`);
  return source.slice(startIndex, endIndex);
}
