import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const componentsTs = readFileSync(fileURLToPath(new URL('../src/view/components.ts', import.meta.url)), 'utf8');
const ctrlTs = readFileSync(fileURLToPath(new URL('../src/ctrl.ts', import.meta.url)), 'utf8');
const interfacesTs = readFileSync(fileURLToPath(new URL('../src/interfaces.ts', import.meta.url)), 'utf8');
const keyboardTs = readFileSync(fileURLToPath(new URL('../src/keyboard.ts', import.meta.url)), 'utf8');
const analyseFreeScss = readFileSync(fileURLToPath(new URL('../css/_analyse.free.scss', import.meta.url)), 'utf8');
const analyseLayoutScss = readFileSync(fileURLToPath(new URL('../css/_layout.scss', import.meta.url)), 'utf8');
const chesstoryScss = readFileSync(fileURLToPath(new URL('../css/_chesstory.scss', import.meta.url)), 'utf8');
const studyIndexScss = readFileSync(fileURLToPath(new URL('../css/study/_index.scss', import.meta.url)), 'utf8');
const routes = readFileSync(fileURLToPath(new URL('../../../conf/routes', import.meta.url)), 'utf8');
const analyseUiScala = readFileSync(
  fileURLToPath(new URL('../../../modules/analyse/src/main/ui/AnalyseUi.scala', import.meta.url)),
  'utf8',
);
const analyseViewScala = readFileSync(
  fileURLToPath(new URL('../../../app/views/analyse.scala', import.meta.url)),
  'utf8',
);
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
const supportScala = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/support.scala', import.meta.url)),
  'utf8',
);
const betaFeedbackScala = readFileSync(
  fileURLToPath(new URL('../../../app/views/pages/betaFeedback.scala', import.meta.url)),
  'utf8',
);
const sitePackageJson = readFileSync(fileURLToPath(new URL('../../../ui/site/package.json', import.meta.url)), 'utf8');
const monScala = readFileSync(fileURLToPath(new URL('../../../modules/common/src/main/mon.scala', import.meta.url)), 'utf8');
const opsModelScala = readFileSync(fileURLToPath(new URL('../../../app/ops/model.scala', import.meta.url)), 'utf8');
const authScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_auth.scss', import.meta.url)), 'utf8');
const accountScss = readFileSync(fileURLToPath(new URL('../../../ui/site/css/_account.scss', import.meta.url)), 'utf8');
const mainController = readFileSync(fileURLToPath(new URL('../../../app/controllers/Main.scala', import.meta.url)), 'utf8');
const journalView = readFileSync(fileURLToPath(new URL('../../../app/views/pages/journal.scala', import.meta.url)), 'utf8');
const topNavScala = readFileSync(
  fileURLToPath(new URL('../../../modules/web/src/main/ui/TopNav.scala', import.meta.url)),
  'utf8',
);
const importerView = readFileSync(fileURLToPath(new URL('../../../app/views/importer.scala', import.meta.url)), 'utf8');

describe('frontend audit regressions', () => {
  const retiredRouteSlug = ['strategic', 'puzzle'].join('-');
  const retiredController = ['Strategic', 'Puzzle'].join('');
  const retiredBundle = ['strategic', 'Puzzle.ts'].join('');
  const retiredMoveSurface = ['Move', 'Explanation'].join(' ');
  const retiredMoveAction = ['Explain', 'This', 'Move'].join(' ');
  const retiredChronicleKey = ['game', 'chronicle'].join('_');
  const retiredChronicleLabel = ['Game', 'Chronicle'].join(' ');
  const retiredLlmMetricPrefix = ['llm', 'commentary'].join('.');
  const retiredOpsTitle = ['Commentary'].join('');

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
    assert.match(topNavScala, /item\("\/notebook", "Notebook", isOn\("\/notebook"\)\)/);
    assert.doesNotMatch(topNavScala, /item\("\/journal", "Journal", isOn\("\/journal"\)\)/);
    assert.doesNotMatch(topNavScala, /item\("\/support", "Support", isOn\("\/support"\)/);
    assert.match(importerView, /div\(cls := "importer-hero__eyebrow"\)\("Recent games"\)/);
  });

  test('retired puzzle product surface stays removed from live entry points', () => {
    const retiredRoutePattern = new RegExp(retiredRouteSlug);
    const retiredControllerPattern = new RegExp(retiredController);
    const retiredBundlePattern = new RegExp(retiredBundle.replace('.', '\\.'));

    assert.doesNotMatch(routes, retiredRoutePattern);
    assert.doesNotMatch(routes, retiredControllerPattern);
    assert.doesNotMatch(topNavScala, retiredRoutePattern);
    assert.doesNotMatch(landingScala, retiredControllerPattern);
    assert.doesNotMatch(accountIntelView, retiredControllerPattern);
    assert.doesNotMatch(domHandlersTs, retiredRoutePattern);
    assert.doesNotMatch(sitePackageJson, retiredBundlePattern);
  });

  test('retired public commentary claims stay out of product copy and source hooks', () => {
    const moveSurfacePattern = new RegExp(retiredMoveSurface);
    const moveActionPattern = new RegExp(retiredMoveAction);
    const chronicleKeyPattern = new RegExp(retiredChronicleKey);
    const chronicleLabelPattern = new RegExp(retiredChronicleLabel);
    const llmMetricPattern = new RegExp(retiredLlmMetricPrefix.replace('.', '\\.'));
    const retiredOpsMappings = ['active', 'commentary', 'fullgame'].map(
      key => new RegExp(`"${key}"\\s*->\\s*"${retiredOpsTitle}"`),
    );

    for (const source of [landingScala, supportScala]) {
      assert.doesNotMatch(source, moveSurfacePattern);
      assert.doesNotMatch(source, moveActionPattern);
    }

    assert.doesNotMatch(betaFeedbackScala, chronicleKeyPattern);
    assert.doesNotMatch(betaFeedbackScala, chronicleLabelPattern);
    assert.doesNotMatch(monScala, llmMetricPattern);
    for (const mapping of retiredOpsMappings) assert.doesNotMatch(opsModelScala, mapping);
  });

  test('analysis import keeps an explicit CTA on mobile', () => {
    assert.match(componentsTs, /const submitPgnDraft = \(\) => \{/);
    assert.match(componentsTs, /\[icon\(licon\.PlayTriangle as any\), ' Import PGN'\]/);
    assert.doesNotMatch(componentsTs, /!isMobile\(\)\s*&&[\s\S]{0,240}Import PGN/);
  });

  test('notebook launch panel does not promise saved explanation transfer', () => {
    const launchPanel = extractBetween(componentsTs, 'function renderStudyLaunchPanel', 'type FenDraftInspection');

    assert.doesNotMatch(launchPanel, /studyTransferCountValue/);
    assert.doesNotMatch(launchPanel, /saved explanation/i);
    assert.doesNotMatch(launchPanel, /saved lines/i);
    assert.doesNotMatch(ctrlTs, /studyTransferCount/);
    assert.match(launchPanel, /PGN \+ move tree/);
    assert.match(launchPanel, /Branches stay explorable/);
  });

  test('analysis server shell no longer renders bookmaker bootstrap UI', () => {
    const analyseOptsBlock = extractBetween(interfacesTs, 'export interface AnalyseOpts', 'export interface JustCaptured');

    assert.doesNotMatch(analyseUiScala, /"bookmaker"\s*->/);
    assert.doesNotMatch(analyseUiScala, /analyse--bookmaker/);
    assert.doesNotMatch(analyseUiScala, /analyse__bookmaker/);
    assert.doesNotMatch(analyseUiScala, /data-bookmaker/);
    assert.doesNotMatch(analyseUiScala, /bookmaker-field/);
    assert.doesNotMatch(analyseUiScala, /Bookmaker/);
    assert.doesNotMatch(analyseViewScala, /"bookmaker"\s*->/);
    assert.doesNotMatch(analyseViewScala, /bookmaker:\s*Boolean/);
    assert.doesNotMatch(userAnalysisController, /bookmaker\s*=/);
    assert.doesNotMatch(analyseOptsBlock, /bookmaker\?:/);
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

  test('dark-theme contrast fixes cover account warnings', () => {
    assert.match(accountScss, /p\.desc[\s\S]*?html\.dark &/);
    assert.match(accountScss, /\.warning-box[\s\S]*?html\.dark &/);
    assert.match(accountScss, /\.danger-box[\s\S]*?html\.dark &/);
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
