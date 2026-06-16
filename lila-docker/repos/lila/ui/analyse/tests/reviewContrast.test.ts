import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const analyseFreeScss = readFileSync(fileURLToPath(new URL('../css/_analyse.free.scss', import.meta.url)), 'utf8');
const chesstoryScss = readFileSync(fileURLToPath(new URL('../css/_chesstory.scss', import.meta.url)), 'utf8');
const narrativeScss = readFileSync(fileURLToPath(new URL('../css/_narrative.scss', import.meta.url)), 'utf8');
const sideScss = readFileSync(fileURLToPath(new URL('../css/_side.scss', import.meta.url)), 'utf8');

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

  test('keeps the player board context rail compact and sticky with the board', () => {
    assert.match(sideScss, /\.move-review-player__board-anchor\s*\{/);
    assert.match(sideScss, /\.move-review-player__board-anchor-label\s*\{[\s\S]*?color:\s*\$c-primary;/);
    assert.match(sideScss, /\.move-review-player__board-anchor-move\s*\{[\s\S]*?font-family:\s*'Roboto Mono', monospace;/);
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
      'rgba(237, 223, 201, 0.82)',
      'rgba(253, 186, 116, 0.86)',
      '#0f1a24',
      '#ecf3fa',
    ].forEach(color => assert.doesNotMatch(chesstoryScss, new RegExp(escapeRegExp(color), 'i'), `off-palette color: ${color}`));

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
  });
});

describe('standalone narrative contrast', () => {
  test('uses readable badge fills for classification badges', () => {
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
        contrastRatio(parseHex('#ffffff'), parseHex(bg)) >= 4.5,
        `${label}: expected white text contrast >= 4.5, got ${contrastRatio(parseHex('#ffffff'), parseHex(bg)).toFixed(2)}`,
      );
    }
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
