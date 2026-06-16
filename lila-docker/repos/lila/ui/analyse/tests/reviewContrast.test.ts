import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const analyseFreeScss = readFileSync(fileURLToPath(new URL('../css/_analyse.free.scss', import.meta.url)), 'utf8');
const chesstoryScss = readFileSync(fileURLToPath(new URL('../css/_chesstory.scss', import.meta.url)), 'utf8');
const narrativeScss = readFileSync(fileURLToPath(new URL('../css/_narrative.scss', import.meta.url)), 'utf8');
const sideScss = readFileSync(fileURLToPath(new URL('../css/_side.scss', import.meta.url)), 'utf8');
const commentaryWidgetScss = readFileSync(fileURLToPath(new URL('../css/_commentary.widget.scss', import.meta.url)), 'utf8');
const explorerScss = readFileSync(fileURLToPath(new URL('../css/explorer/_explorer.scss', import.meta.url)), 'utf8');
const forkScss = readFileSync(fileURLToPath(new URL('../css/_fork.scss', import.meta.url)), 'utf8');
const homeScss = readFileSync(fileURLToPath(new URL('../../site/css/_home.scss', import.meta.url)), 'utf8');
const landingScss = readFileSync(fileURLToPath(new URL('../../site/css/_landing.scss', import.meta.url)), 'utf8');
const authScss = readFileSync(fileURLToPath(new URL('../../site/css/_auth.scss', import.meta.url)), 'utf8');
const accountScss = readFileSync(fileURLToPath(new URL('../../site/css/_account.scss', import.meta.url)), 'utf8');
const journalScss = readFileSync(fileURLToPath(new URL('../../site/css/_journal.scss', import.meta.url)), 'utf8');
const strategicPuzzleScss = readFileSync(fileURLToPath(new URL('../../site/css/_strategicPuzzle.scss', import.meta.url)), 'utf8');
const studyIndexScss = readFileSync(fileURLToPath(new URL('../css/study/_index.scss', import.meta.url)), 'utf8');
const studyListWidgetScss = readFileSync(fileURLToPath(new URL('../css/study/_list-widget.scss', import.meta.url)), 'utf8');
const chesstoryOverridesScss = readFileSync(
  fileURLToPath(new URL('../../lib/css/page/_chesstory-overrides.scss', import.meta.url)),
  'utf8',
);
const headerButtonsScss = readFileSync(fileURLToPath(new URL('../../lib/css/header/_buttons.scss', import.meta.url)), 'utf8');
const topnavVisibleScss = readFileSync(
  fileURLToPath(new URL('../../lib/css/header/_topnav-visible.scss', import.meta.url)),
  'utf8',
);
const topnavHiddenScss = readFileSync(
  fileURLToPath(new URL('../../lib/css/header/_topnav-hidden.scss', import.meta.url)),
  'utf8',
);
const cookieConsentScss = readFileSync(fileURLToPath(new URL('../../lib/css/page/_cookie-consent.scss', import.meta.url)), 'utf8');

describe('review shell contrast palette', () => {
  test('keeps the review-shell palette above AA thresholds', () => {
    const reviewVars = extractCustomProperties(analyseFreeScss, '.analyse-review');
    const summaryCardBg = reviewVars.get('--review-summary-surface');

    const cases = [
      ['idle review tabs', reviewVars.get('--review-text-muted'), reviewVars.get('--review-surface-raised'), 4.5],
      ['eyebrow and secondary copy', reviewVars.get('--review-text-muted'), reviewVars.get('--review-surface'), 4.5],
      ['moment and variation prose', reviewVars.get('--review-text'), reviewVars.get('--review-surface-soft'), 4.5],
      ['summary labels', reviewVars.get('--review-text-muted'), summaryCardBg, 4.5],
      ['board workspace headings', reviewVars.get('--review-text-subtle'), reviewVars.get('--review-surface'), 4.5],
      ['board workspace card copy', reviewVars.get('--review-text'), reviewVars.get('--review-surface-soft'), 4.5],
      ['board workspace secondary copy', reviewVars.get('--review-text-muted'), reviewVars.get('--review-surface-soft'), 4.5],
      ['board workspace inactive choices', reviewVars.get('--review-text-subtle'), reviewVars.get('--review-surface-raised'), 4.5],
      ['neutral metrics', reviewVars.get('--review-neutral-chip-text'), reviewVars.get('--review-neutral-chip-bg'), 4.5],
      ['info chips', reviewVars.get('--review-info-text'), reviewVars.get('--review-info-bg'), 4.5],
      ['warning chips', reviewVars.get('--review-warning-text'), reviewVars.get('--review-warning-bg'), 4.5],
      ['success chips', reviewVars.get('--review-success-text'), reviewVars.get('--review-success-bg'), 4.5],
      ['danger chips', reviewVars.get('--review-danger-text'), reviewVars.get('--review-danger-bg'), 4.5],
      ['accent actions', reviewVars.get('--review-accent'), reviewVars.get('--review-accent-soft'), 4.5],
    ] as const;

    for (const [label, fg, bg, min] of cases) {
      assert.ok(fg, `${label}: missing foreground color`);
      assert.ok(bg, `${label}: missing background color`);
      assert.ok(
        contrastRatio(parseHex(fg), parseHex(bg)) >= min,
        `${label}: expected contrast >= ${min}, got ${contrastRatio(parseHex(fg), parseHex(bg)).toFixed(2)}`,
      );
    }
  });

  test('keeps review-shell overrides wired to the reused narrative surfaces', () => {
    const selectors = [
      '.narrative-review',
      '.narrative-moment',
      '.narrative-variation',
      '.narrative-collapse-card',
      '.narrative-signal-box',
      '.narrative-evidence-box',
      '.narrative-strategic-note-box',
      '.dna-stat-card',
      '.dna-collapse-table',
      '.dna-show-more',
      '.action-menu__workspace-choice',
      '.action-menu__workspace-pill',
    ];

    for (const selector of selectors) {
      assert.match(analyseFreeScss, new RegExp(escapeRegExp(selector)), `missing override for ${selector}`);
    }

    assert.match(analyseFreeScss, /--review-text:/);
    assert.match(analyseFreeScss, /--review-info-bg:/);
    assert.match(analyseFreeScss, /--review-warning-bg:/);
    assert.match(analyseFreeScss, /--review-success-bg:/);
    assert.match(analyseFreeScss, /--review-danger-bg:/);
    assert.match(analyseFreeScss, /&__reference-action\s*\{/);
    assert.match(
      analyseFreeScss,
      /&__panel--board[\s\S]*?&\.action-menu\s*\{[\s\S]*?color:\s*var\(--review-text\);[\s\S]*?span\s*\{[\s\S]*?color:\s*inherit;/,
    );
  });

  test('keeps review player typography calm across the shell and side panel', () => {
    [...analyseFreeScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `review shell letter-spacing must be 0, got ${match[1]}`),
    );
    [...sideScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `review side letter-spacing must be 0, got ${match[1]}`),
    );
  });

  test('keeps review shell tool tabs as compact player work choices', () => {
    assert.match(analyseFreeScss, /&__surface-toggle\s*\{[\s\S]*?flex-direction:\s*column;/);
    assert.match(analyseFreeScss, /&__surface-toggle-label,\s*[\s\S]*?&__surface-toggle-detail\s*\{[\s\S]*?overflow-wrap:\s*anywhere;/);
    assert.match(analyseFreeScss, /&__surface-toggle-detail\s*\{[\s\S]*?color:\s*var\(--review-text-subtle\);/);
    assert.match(
      analyseFreeScss,
      /@include mq-is-col1[\s\S]*&__surface-switch\s*\{[\s\S]*?overflow-x:\s*auto;/,
    );
    assert.match(
      analyseFreeScss,
      /@include mq-is-col1[\s\S]*&__surface-toggle-detail\s*\{[\s\S]*?display:\s*none;/,
    );
  });

  test('keeps load-game workspace labels from crowding the input fields', () => {
    assert.match(
      analyseFreeScss,
      /&--workspace\s*\{[\s\S]*?\.name\s*\{[\s\S]*?flex:\s*0 0 clamp\(5\.75rem,\s*18%,\s*7\.5rem\);[\s\S]*?overflow-wrap:\s*anywhere;[\s\S]*?white-space:\s*normal;/,
    );
    assert.match(
      analyseFreeScss,
      /&--workspace\s*\{[\s\S]*?\.copyable\s*\{[\s\S]*?min-width:\s*0;/,
    );
  });

  test('keeps import-history provider badges above AA in both themes', () => {
    const lightVars = extractCustomProperties(analyseFreeScss, '.analyse-review');
    const darkVars = extractCustomProperties(analyseFreeScss, 'html.dark .analyse-review');

    const cases = [
      [
        'light lichess provider badge',
        lightVars.get('--review-provider-lichess-text'),
        lightVars.get('--review-provider-lichess-bg'),
      ],
      [
        'light chess.com provider badge',
        lightVars.get('--review-provider-chesscom-text'),
        lightVars.get('--review-provider-chesscom-bg'),
      ],
      [
        'dark lichess provider badge',
        darkVars.get('--review-provider-lichess-text'),
        darkVars.get('--review-provider-lichess-bg'),
      ],
      [
        'dark chess.com provider badge',
        darkVars.get('--review-provider-chesscom-text'),
        darkVars.get('--review-provider-chesscom-bg'),
      ],
    ] as const;

    for (const [label, fg, bg] of cases) {
      assert.ok(fg, `${label}: missing foreground color`);
      assert.ok(bg, `${label}: missing background color`);
      assert.ok(
        contrastRatio(parseHex(fg), parseHex(bg)) >= 4.5,
        `${label}: expected contrast >= 4.5, got ${contrastRatio(parseHex(fg), parseHex(bg)).toFixed(2)}`,
      );
    }
  });

  test('keeps the mobile review board below the sticky scene timeline', () => {
    assert.match(sideScss, /--move-review-sticky-board-top:\s*3\.35rem;/);
    assert.match(sideScss, /\.move-review-player__board-note\s*\{/);
    assert.match(
      sideScss,
      /\.move-review-player__board-title,\s*[\s\S]*?\.move-review-player__board-subtitle,\s*[\s\S]*?\.move-review-player__board-note\s*\{[\s\S]*?overflow-wrap:\s*anywhere;/,
    );
    assert.match(sideScss, /@include mq-is-col1[\s\S]*--move-review-sticky-board-top:\s*2\.7rem;/);
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__board-shell\s*\{[\s\S]*top:\s*var\(--move-review-sticky-board-top\);/,
    );
    assert.doesNotMatch(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__board-shell\s*\{[\s\S]*top:\s*0\.2rem;/,
    );
  });

  test('keeps review scene flow labels visible without crowding mobile', () => {
    assert.match(sideScss, /\.move-review-player__timeline-action\s*\{/);
    assert.match(
      sideScss,
      /\.move-review-player__timeline-step\.is-active \.move-review-player__timeline-action\s*\{[\s\S]*?color:\s*\$c-primary;/,
    );
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__timeline-action\s*\{[\s\S]*?display:\s*none;/,
    );
  });

  test('keeps the active coach lesson step visually anchored in the flow', () => {
    assert.match(
      sideScss,
      /&\.is-active\s*\{[\s\S]*?background:\s*linear-gradient\([\s\S]*?color-mix\(in srgb,\s*#\{\$c-primary\}\s*16%,\s*#\{\$c-bg-low\}\)[\s\S]*?box-shadow:[\s\S]*?rgba\(0,\s*0,\s*0,\s*0\.14\);/,
    );
    assert.match(
      sideScss,
      /&\.is-active \.move-review-player__timeline-kicker\s*\{[\s\S]*?color:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*74%,\s*#\{\$c-font\}\);/,
    );
  });

  test('keeps enabled eval visibly anchored in muted green', () => {
    assert.match(sideScss, /\.move-review-score-toggle\s*\{[\s\S]*?color:\s*\$c-font-dim;/);
    assert.match(
      sideScss,
      /\.move-review-score-toggle\s*\{[\s\S]*?&\[aria-pressed='true'\]\s*\{[\s\S]*?border-color:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*34%,\s*#\{\$c-border\}\);[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*10%,\s*#\{\$c-bg-low\}\);[\s\S]*?color:\s*\$c-primary;/,
    );
    assert.match(
      sideScss,
      /\.eval-badge,\s*[\s\S]*?\.eval-badge--inline\s*\{[\s\S]*?border:\s*1px solid color-mix\(in srgb,\s*#\{\$c-primary\}\s*24%,\s*transparent\);[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*11%,\s*#\{\$c-bg-low\}\);[\s\S]*?color:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*82%,\s*#\{\$c-font\}\);[\s\S]*?font-weight:\s*700;/,
    );
    assert.match(
      sideScss,
      /\.move-review-coach__gap\s*\{[\s\S]*?border:\s*1px solid color-mix\(in srgb,\s*#\{\$c-primary\}\s*24%,\s*transparent\);[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*11%,\s*#\{\$c-bg-low\}\);[\s\S]*?font-family:\s*'Roboto Mono', monospace;/,
    );
    assert.match(
      sideScss,
      /\.move-review-player__scene-line-eval\s*\{[\s\S]*?border:\s*1px solid color-mix\(in srgb,\s*#\{\$c-primary\}\s*24%,\s*transparent\);[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*11%,\s*#\{\$c-bg-low\}\);[\s\S]*?font-family:\s*'Roboto Mono', monospace;/,
    );
  });

  test('keeps the player board context rail compact and sticky with the board', () => {
    assert.match(sideScss, /\.move-review-player__board-anchor\s*\{/);
    assert.match(
      sideScss,
      /\.move-review-player__board-anchor-label\s*\{[\s\S]*?font-family:\s*'Roboto Mono', monospace;[\s\S]*?font-weight:\s*800;[\s\S]*?color:\s*\$c-primary;/,
    );
    assert.match(sideScss, /\.move-review-player__board-anchor-move\s*\{[\s\S]*?font-family:\s*'Roboto Mono', monospace;/);
    assert.match(
      sideScss,
      /\.move-review-player__board-anchor-eval\s*\{[\s\S]*?border:\s*1px solid color-mix\(in srgb,\s*#\{\$c-primary\}\s*24%,\s*transparent\);[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*14%,\s*#\{\$c-bg-low\}\);[\s\S]*?color:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*86%,\s*#\{\$c-font\}\);[\s\S]*?font-weight:\s*800;/,
    );
    assert.match(
      sideScss,
      /\.move-review-player__board-shell\s*\{[\s\S]*?border-left:\s*3px solid color-mix\(in srgb,\s*#\{\$c-primary\}\s*48%,\s*#\{\$c-border\}\);[\s\S]*?box-shadow:[\s\S]*?inset 0 0 0 1px color-mix\(in srgb,\s*#\{\$c-primary\}\s*8%,\s*transparent\),/,
    );
    assert.match(
      sideScss,
      /\.move-review-player__board-preview\s*\{[\s\S]*?\.pv-board\s*\{[\s\S]*?box-shadow:[\s\S]*?0 0 0 1px color-mix\(in srgb,\s*#\{\$c-primary\}\s*20%,\s*transparent\),/,
    );
    assert.match(sideScss, /\.move-review-player__board-cue-item\s*\{[\s\S]*?span\s*\{[\s\S]*?text-transform:\s*uppercase;/);
    assert.match(sideScss, /\.move-review-player__board-cue-item\s*\{[\s\S]*?strong\s*\{[\s\S]*?font-family:\s*'Roboto Mono', monospace;/);
    assert.match(sideScss, /\.move-review-player__board-cue-item--line\s*\{[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*10%,\s*transparent\);/);
    assert.match(
      sideScss,
      /\.move-review-player__board-cue-item--eval\s*\{[\s\S]*?background:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*11%,\s*#\{\$c-bg-low\}\);/,
    );
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__board-anchor\s*\{[\s\S]*?grid-column:\s*1 \/ -1;[\s\S]*?grid-row:\s*1;/,
    );
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__board-preview\s*\{[\s\S]*?grid-row:\s*2;/,
    );
  });

  test('keeps scene board focus readable inside one coaching scene', () => {
    assert.match(sideScss, /\.move-review-player__scene-focus\s*\{/);
    assert.match(sideScss, /\.move-review-player__scene-focus-label\s*\{[\s\S]*?color:\s*\$c-primary;/);
    assert.match(sideScss, /\.move-review-player__scene-focus p\s*\{[\s\S]*?overflow-wrap:\s*anywhere;/);
    assert.match(sideScss, /\.move-review-player__scene-focus-square\s*\{[\s\S]*?font-family:\s*'Roboto Mono', monospace;/);
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__scene-focus\s*\{[\s\S]*?grid-template-columns:\s*minmax\(0,\s*1fr\);/,
    );
  });

  test('keeps line replay controls wrapped with a scene-board return', () => {
    const lineControlsBlock = extractBlock(sideScss, '.move-review-player__line-controls');
    assert.match(lineControlsBlock, /flex-wrap:\s*wrap;/);
    assert.doesNotMatch(lineControlsBlock, /white-space:\s*nowrap;/);
    assert.match(sideScss, /\.move-review-player__line-step--scene\s*\{[\s\S]*?color:\s*\$c-primary;/);
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-player__line-controls\s*\{[\s\S]*?flex-wrap:\s*wrap;/,
    );
  });

  test('keeps opened review detail layers bounded inside the current scene', () => {
    assert.match(
      sideScss,
      /\.move-review-coach__details\s*\{[\s\S]*?summary\s*\{[\s\S]*?position:\s*sticky;[\s\S]*?top:\s*0;/,
    );
    assert.match(
      sideScss,
      /&\[open\]\s*\{[\s\S]*?max-height:\s*min\(42vh,\s*21rem\);[\s\S]*?overflow-y:\s*auto;[\s\S]*?overscroll-behavior:\s*contain;/,
    );
    assert.match(
      sideScss,
      /@include mq-is-col1[\s\S]*\.move-review-coach__details\[open\]\s*\{[\s\S]*?max-height:\s*min\(36vh,\s*17rem\);/,
    );
  });

  test('keeps Chesstory study surfaces in the dark study-room palette', () => {
    [
      '#fff9f1',
      '#fff',
      'rgba(237, 223, 201, 0.82)',
      'rgba(253, 186, 116, 0.86)',
      '#0f1a24',
      '#ecf3fa',
      'rgba(255, 255, 255',
    ].forEach(color => assert.doesNotMatch(chesstoryScss, new RegExp(escapeRegExp(color), 'i'), `off-palette color: ${color}`));

    [...chesstoryScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `chesstory letter-spacing must be 0, got ${match[1]}`),
    );

    assert.match(
      chesstoryScss,
      /select\s*\{[\s\S]*?background:\s*var\(--atlas-surface-strong\);[\s\S]*?color:\s*var\(--atlas-text\);[\s\S]*?border:\s*1px solid rgba\(139,\s*192,\s*113,\s*0\.24\);/,
    );
    assert.match(
      chesstoryScss,
      /&__study-cover-title\s*\{[\s\S]*?color:\s*var\(--atlas-text\);/,
    );
    assert.match(
      chesstoryScss,
      /&__study-cover-detail\s*\{[\s\S]*?color:\s*var\(--atlas-accent-strong\);/,
    );
    assert.match(chesstoryScss, /\.ceval\s*\{[\s\S]*?box-shadow:\s*inset 0 -1px 0 rgba\(139,\s*192,\s*113,\s*0\.28\);/);
  });

  test('keeps the commentary credit widget in the dark study-room palette', () => {
    [
      '#fff',
      '#4caf50',
      '#fbc02d',
      '#ffd700',
      '#f44336',
      '#ff5252',
      '#000',
      'rgba(255, 255, 255',
    ].forEach(color =>
      assert.doesNotMatch(commentaryWidgetScss, new RegExp(escapeRegExp(color), 'i'), `off-palette commentary widget color: ${color}`),
    );

    assert.match(commentaryWidgetScss, /\$cw-bg-page:\s*#11130f;/);
    assert.match(commentaryWidgetScss, /\$cw-surface:\s*#22241f;/);
    assert.match(commentaryWidgetScss, /\$cw-primary:\s*#8bc071;/);
    assert.match(commentaryWidgetScss, /\$cw-accent:\s*#e3b15b;/);
    assert.match(commentaryWidgetScss, /\.commentary-credit-widget \.fill\s*\{[\s\S]*?background:\s*linear-gradient\(90deg,\s*\$cw-primary,\s*\$cw-primary-strong\);/);
    assert.match(commentaryWidgetScss, /\.move-review-error\.credit-exhausted\s*\{[\s\S]*?background:\s*rgba\(\$cw-danger,\s*0\.1\);/);
  });

  test('keeps narrative plan and evidence boxes in the study-room palette', () => {
    const narrativeReviewBlocks = [
      '.narrative-active-plan-box',
      '.narrative-strategic-note-box',
      '.narrative-strategic-surface',
      '.narrative-thread-summary',
      '.narrative-selection-reason',
      '.narrative-branch-dossier',
      '.narrative-signal-box',
      '.narrative-evidence-box',
      '.narrative-decision-compare',
    ]
      .map(selector => extractBlock(narrativeScss, selector))
      .join('\n');

    [
      '#3c7ebb',
      '#245382',
      '#173858',
      '#ffffff',
      '#2f5f9f',
      '#7a5c1f',
      '#c99a2a',
      '#4c7330',
      '#315022',
      '#6a9c3c',
      '#f0c66b',
      '#91631f',
    ].forEach(color =>
      assert.doesNotMatch(narrativeReviewBlocks, new RegExp(escapeRegExp(color), 'i'), `off-palette narrative review color: ${color}`),
    );

    assert.match(narrativeReviewBlocks, /border:\s*1px solid rgba\(\$c-primary,\s*0\.3\);/);
    assert.match(narrativeReviewBlocks, /background:\s*rgba\(\$c-primary,\s*0\.08\);/);
    assert.match(narrativeReviewBlocks, /background:\s*rgba\(\$c-bg-low,\s*0\.72\);/);
    assert.match(narrativeReviewBlocks, /color:\s*color-mix\(in srgb,\s*#\{\$c-secondary\}\s*78%,\s*#\{\$c-font\}\);/);
  });

  test('keeps narrative review chips in the study-room palette', () => {
    const narrativeChipBlocks = [
      '.narrative-decision-compare__move',
      '.narrative-decision-compare__move--engine',
      '.narrative-decision-compare__move--deferred',
      '.narrative-decision-compare__gap',
      '.narrative-decision-compare__details',
      '.narrative-signal-chip',
      '.narrative-evidence-card',
      '.narrative-evidence-status',
      '.narrative-strategic-chip',
      '.narrative-plan-theme-text',
    ]
      .map(selector => extractBlock(narrativeScss, selector))
      .join('\n');

    [
      '#3c7ebb',
      '#245382',
      '#2f5f9f',
      '#7a5c1f',
      '#c99a2a',
      '#4c7330',
      '#315022',
      '#6a9c3c',
      '#fff7e1',
      '#f0c66b',
      '#91631f',
      '#714a12',
      '#365028',
    ].forEach(color =>
      assert.doesNotMatch(narrativeChipBlocks, new RegExp(escapeRegExp(color), 'i'), `off-palette narrative chip color: ${color}`),
    );

    assert.match(narrativeChipBlocks, /background:\s*rgba\(\$c-primary,\s*0\.12\);/);
    assert.match(narrativeChipBlocks, /background:\s*rgba\(\$c-secondary,\s*0\.14\);/);
    assert.match(narrativeChipBlocks, /background:\s*rgba\(\$c-bg-low,\s*0\.72\);/);
    assert.match(narrativeChipBlocks, /color:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*78%,\s*#\{\$c-font\}\);/);
    assert.match(narrativeChipBlocks, /color:\s*color-mix\(in srgb,\s*#\{\$c-secondary\}\s*78%,\s*#\{\$c-font\}\);/);
  });

  test('keeps study index surfaces in the dark study-room palette', () => {
    const studyScss = [studyIndexScss, studyListWidgetScss].join('\n');
    [
      '#f7f0e3',
      '#c9b38e',
      '#d97706',
      '#c8b9a1',
      '#f6ead7',
      '#cbbda5',
      '#c2410c',
      '#9a3412',
      '#fff7ed',
      '#fff9f1',
      'rgba(237, 223, 201',
      'rgba(253, 186, 116',
      'rgba(255, 255, 255',
    ].forEach(color => assert.doesNotMatch(studyScss, new RegExp(escapeRegExp(color), 'i'), `off-palette study color: ${color}`));

    assert.match(studyIndexScss, /\$study-bg:\s*#171816;/);
    assert.match(studyIndexScss, /\$study-surface:\s*#22241f;/);
    assert.match(studyIndexScss, /\$study-accent:\s*#8bc071;/);
    assert.match(studyIndexScss, /\$study-warm:\s*#e3b15b;/);
    assert.match(studyIndexScss, /@import 'list-widget';/);
    [...studyScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `study letter-spacing must be 0, got ${match[1]}`),
    );
    assert.match(studyListWidgetScss, /\.study\s*\{[\s\S]*?background:\s*rgba\(\$study-surface,\s*0\.72\);/);
    assert.match(studyListWidgetScss, /&__title\s*\{[\s\S]*?color:\s*\$study-text;/);
    assert.match(studyListWidgetScss, /&__detail\s*\{[\s\S]*?color:\s*\$study-warm;/);
  });

  test('keeps opening explorer in the dark study-room palette', () => {
    [
      '#fff',
      'rgba(15, 17, 23',
      'rgba(255, 255, 255',
      '$m-secondary_bg--mix-40',
      '$m-secondary_bg--mix-50',
      '$m-primary_bg--mix-25',
    ].forEach(color => assert.doesNotMatch(explorerScss, new RegExp(escapeRegExp(color), 'i'), `off-palette explorer color: ${color}`));

    assert.match(explorerScss, /\$explorer-bg:\s*#171816;/);
    assert.match(explorerScss, /\$explorer-surface:\s*#22241f;/);
    assert.match(explorerScss, /\$explorer-accent:\s*#8bc071;/);
    assert.match(explorerScss, /\.explorer-title\s*\{[\s\S]*?letter-spacing:\s*0;/);
    assert.match(explorerScss, /\.long\s*\{[\s\S]*?letter-spacing:\s*0;/);
    assert.match(
      explorerScss,
      /&:first-child\s*\{[\s\S]*?background:\s*linear-gradient\(to right,\s*\$explorer-accent-dark,\s*\$explorer-accent\);[\s\S]*?color:\s*\$explorer-bg;/,
    );
    assert.match(explorerScss, /\.game_menu\s*\{[\s\S]*?background:\s*\$explorer-surface-raised;/);
  });

  test('keeps candidate fork choices in the study-room palette', () => {
    [
      '#fff',
      '$m-primary_bg--mix-25',
      '$m-secondary_bg--mix-25',
      '$m-bad_bg--mix-25',
      'background: $c-fork',
      'background: $c-secondary',
      'background: $c-bad',
    ].forEach(color => assert.doesNotMatch(forkScss, new RegExp(escapeRegExp(color), 'i'), `off-palette fork color: ${color}`));

    assert.match(forkScss, /\$fork-bg:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*8%,\s*#\{\$c-bg-low\}\);/);
    assert.match(forkScss, /\$fork-active-bg:\s*color-mix\(in srgb,\s*#\{\$c-primary\}\s*18%,\s*#\{\$c-bg-low\}\);/);
    assert.match(forkScss, /move\s*\{[\s\S]*?background:\s*\$fork-bg;[\s\S]*?color:\s*\$c-font;/);
    assert.match(forkScss, /&\.wrong\s*\{[\s\S]*?background:\s*\$fork-wrong-bg;/);
    assert.match(
      forkScss,
      /move:hover,[\s\S]*?move\.selected\s*\{[\s\S]*?background:\s*\$fork-active-bg;[\s\S]*?color:\s*color-mix\(in srgb,\s*#\{\$c-fork\}\s*78%,\s*#\{\$c-font\}\);/,
    );
  });

  test('keeps site entry surfaces in the dark study-room palette', () => {
    const siteScss = [homeScss, landingScss, authScss, accountScss, journalScss, strategicPuzzleScss].join('\n');
    [
      '#fff9f1',
      '#fff',
      'rgba(237, 223, 201, 0.82)',
      'rgba(253, 186, 116, 0.86)',
      '#0f1a24',
      '#ecf3fa',
      'rgba(25, 36, 52',
      '#dc2626',
      '#bfeadd',
      '#d7c59c',
      '#76945f',
      '#f3ede3',
    ].forEach(color => assert.doesNotMatch(siteScss, new RegExp(escapeRegExp(color), 'i'), `off-palette site color: ${color}`));

    assert.match(homeScss, /\$home-bg-light:\s*#171816;/);
    assert.match(homeScss, /\$home-primary:\s*#8bc071;/);
    assert.match(homeScss, /\$home-accent:\s*#e3b15b;/);
    [...homeScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `home letter-spacing must be 0, got ${match[1]}`),
    );
    [...landingScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `landing letter-spacing must be 0, got ${match[1]}`),
    );
    [...authScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `auth letter-spacing must be 0, got ${match[1]}`),
    );
    assert.match(journalScss, /\$journal-bg-light:\s*#171816;/);
    assert.match(journalScss, /\$journal-primary-light:\s*#8bc071;/);
    [...journalScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `journal letter-spacing must be 0, got ${match[1]}`),
    );
    assert.match(accountScss, /\$c-bad-strong:\s*#c75f57;/);
    assert.match(strategicPuzzleScss, /\$sp-primary-strong:\s*#b4d99a;/);
    assert.match(strategicPuzzleScss, /\$sp-board-light:\s*#b7bf8f;/);
    assert.match(strategicPuzzleScss, /\$sp-board-dark:\s*#6b7f57;/);
    [...strategicPuzzleScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `strategic puzzle letter-spacing must be 0, got ${match[1]}`),
    );
    assert.match(accountScss, /\.btn-danger\s*\{[\s\S]*?background:\s*\$c-bad;[\s\S]*?color:\s*\$c-bg-page;/);
    assert.match(homeScss, /box-shadow:\s*[\s\S]*?rgba\(0,\s*0,\s*0,\s*0\.18\)[\s\S]*?rgba\(\$home-primary,\s*0\.08\);/);
  });

  test('keeps Chesstory page chrome aligned with the study-room palette', () => {
    const chromeScss = [chesstoryOverridesScss, headerButtonsScss, topnavVisibleScss, topnavHiddenScss, cookieConsentScss].join(
      '\n',
    );

    [...chromeScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `Chesstory chrome letter-spacing must be 0, got ${match[1]}`),
    );

    assert.doesNotMatch(headerButtonsScss, /rgba\(255,\s*255,\s*255/i, 'header theme switch should not use bright glass');
    assert.match(chesstoryOverridesScss, /\.cs-wordmark\s*\{[\s\S]*?letter-spacing:\s*0;/);
    assert.match(headerButtonsScss, /\.site-theme-switch\s*\{[\s\S]*?background:\s*rgba\(\$c-primary,\s*0\.08\);/);
    assert.match(
      headerButtonsScss,
      /&\[aria-pressed='true'\]\s*\{[\s\S]*?background:\s*rgba\(\$c-primary,\s*0\.16\);[\s\S]*?box-shadow:\s*inset 0 0 0 1px rgba\(\$c-primary,\s*0\.18\);/,
    );
    assert.match(cookieConsentScss, /background:\s*color-mix\(in srgb,\s*var\(--c-bg-box\)\s*92%,\s*#11130f\);/);
    assert.match(cookieConsentScss, /color-mix\(in srgb,\s*var\(--c-bg-box\)\s*96%,\s*#11130f\);/);
  });
});

describe('standalone narrative contrast', () => {
  test('uses readable badge fills for classification badges', () => {
    const badgeText = '#efeee8';
    const cases = [
      ['blunder', extractClassificationBackground('blunder')],
      ['mistake', extractClassificationBackground('mistake')],
      ['inaccuracy', extractClassificationBackground('inaccuracy')],
      ['good/best', extractLiteral(narrativeScss, /&\.good,\s*&\.best\s*\{[\s\S]*?background:\s*(#[0-9a-fA-F]{6});/)],
      ['great', extractClassificationBackground('great')],
      ['brilliant', extractClassificationBackground('brilliant')],
      ['book', extractClassificationBackground('book')],
    ] as const;

    for (const [label, bg] of cases) {
      assert.ok(
        contrastRatio(parseHex(badgeText), parseHex(bg)) >= 4.5,
        `${label}: expected study-room text contrast >= 4.5, got ${contrastRatio(parseHex(badgeText), parseHex(bg)).toFixed(2)}`,
      );
    }
  });

  test('keeps narrative badges in the study-room palette', () => {
    const badgeBlock = extractBlock(narrativeScss, '.narrative-badge');
    [
      '#fff',
      '#a92f44',
      '#8a4d00',
      '#5d6773',
      '#245382',
      '#006a67',
      '#735217',
      '#3c7ebb',
      '#7a5c1f',
      '#c99a2a',
      '#4c7330',
      '#6a9c3c',
    ].forEach(color =>
      assert.doesNotMatch(badgeBlock, new RegExp(escapeRegExp(color), 'i'), `off-palette narrative badge color: ${color}`),
    );

    assert.match(badgeBlock, /&\.blunder\s*\{[\s\S]*?color:\s*#efeee8;[\s\S]*?background:\s*#7c3f3a;/);
    assert.match(badgeBlock, /&\.great\s*\{[\s\S]*?background:\s*#4f6f3e;/);
    assert.match(badgeBlock, /&\.brilliant\s*\{[\s\S]*?background:\s*#3f6f63;/);
    assert.match(badgeBlock, /&\.branch\s*\{[\s\S]*?border-color:\s*rgba\(\$c-primary,\s*0\.36\);/);
    assert.match(badgeBlock, /&\.stage\s*\{[\s\S]*?background:\s*rgba\(\$c-secondary,\s*0\.12\);/);
    assert.match(badgeBlock, /&\.selection\s*\{[\s\S]*?background:\s*rgba\(\$c-secondary,\s*0\.16\);/);
  });

  test('keeps narrative typography and bright literals in the study-room palette', () => {
    [...narrativeScss.matchAll(/letter-spacing:\s*([^;]+);/g)].forEach(match =>
      assert.equal(match[1]!.trim(), '0', `narrative letter-spacing must be 0, got ${match[1]}`),
    );

    ['#fff', '#ffffff', 'rgba(255, 255, 255'].forEach(color =>
      assert.doesNotMatch(narrativeScss, new RegExp(escapeRegExp(color), 'i'), `off-palette narrative literal: ${color}`),
    );

    assert.match(narrativeScss, /\.timeline-marker\s*\{[\s\S]*?background:\s*\$c-font;/);
    assert.match(narrativeScss, /\.patch-toggle-btn\s*\{[\s\S]*?&\.active\s*\{[\s\S]*?color:\s*\$c-bg-box;/);
  });

  test('keeps standalone narrative tabs on font-colored active text', () => {
    assert.match(
      narrativeScss,
      /\.narrative-tab[\s\S]*?&\.active\s*\{[\s\S]*?color:\s*\$c-font;[\s\S]*?background:\s*rgba\(\$c-primary,\s*0\.16\);/,
    );
  });
});

function extractCustomProperties(source: string, selector: string): Map<string, string> {
  const block = extractBlock(source, selector);
  const props = new Map<string, string>();
  const pattern = /^\s*(--[\w-]+):\s*(#[0-9a-fA-F]{6});/gm;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(block))) props.set(match[1]!, match[2]!.toLowerCase());
  return props;
}

function extractBlock(source: string, selector: string): string {
  const start = source.indexOf(selector);
  assert.notEqual(start, -1, `missing selector block: ${selector}`);
  const open = source.indexOf('{', start);
  assert.notEqual(open, -1, `missing opening brace for ${selector}`);
  let depth = 0;
  for (let i = open; i < source.length; i++) {
    if (source[i] === '{') depth += 1;
    if (source[i] === '}') depth -= 1;
    if (depth === 0) return source.slice(open + 1, i);
  }
  throw new Error(`unterminated block for ${selector}`);
}

function extractLiteral(source: string, pattern: RegExp): string {
  const match = source.match(pattern);
  assert.ok(match?.[1], `missing literal for pattern ${pattern}`);
  return match[1]!.toLowerCase();
}

function extractClassificationBackground(name: string): string {
  return extractLiteral(
    narrativeScss,
    new RegExp(`&\\.${escapeRegExp(name)}\\s*\\{[\\s\\S]*?background:\\s*(#[0-9a-fA-F]{6});`),
  );
}

function parseHex(hex: string): [number, number, number] {
  const normalized = hex.trim().replace('#', '');
  assert.equal(normalized.length, 6, `expected 6-digit hex color, got ${hex}`);
  return [
    parseInt(normalized.slice(0, 2), 16),
    parseInt(normalized.slice(2, 4), 16),
    parseInt(normalized.slice(4, 6), 16),
  ];
}

function contrastRatio(fg: [number, number, number], bg: [number, number, number]): number {
  const fgLum = relativeLuminance(fg);
  const bgLum = relativeLuminance(bg);
  const lighter = Math.max(fgLum, bgLum);
  const darker = Math.min(fgLum, bgLum);
  return (lighter + 0.05) / (darker + 0.05);
}

function relativeLuminance([r, g, b]: [number, number, number]): number {
  return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b);
}

function srgbToLinear(channel: number): number {
  const normalized = channel / 255;
  return normalized <= 0.03928 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
