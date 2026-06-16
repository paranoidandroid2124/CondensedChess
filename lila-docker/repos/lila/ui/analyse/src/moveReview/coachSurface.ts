import { formatDecisionTargetComparison } from '../decisionComparison';
import type {
  MoveReviewPlayerAuthorRowV1,
  MoveReviewPlayerDecisionComparisonV1,
  MoveReviewPlayerSurfaceRowV1,
  MoveReviewPlayerSurfaceV1,
  MoveReviewRefsV1,
} from './responsePayload';
import { escapeHtml, normalizeSanToken, renderInteractiveSanChip } from './surfaceShared';

type MoveReviewMoveRef = {
  refId: string;
  san: string;
  uci: string;
  fenAfter: string;
};

type MoveReviewRefIndex = {
  refsBySan: Map<string, MoveReviewMoveRef[]>;
  lines: MoveReviewMoveRef[][];
};

type MoveReviewSceneKey = 'verdict' | 'why' | 'plan' | 'try' | 'remember';

type MoveReviewScene = {
  key: MoveReviewSceneKey;
  label: string;
  shortLabel: string;
  title: string;
  kicker: string;
  body: string;
  board?: string | null;
  boardTitle: string;
  boardSubtitle?: string | null;
  square?: string | null;
  lineSans?: string[];
  lineLabel?: string;
};

function buildMoveReviewRefIndex(refs: MoveReviewRefsV1 | null): MoveReviewRefIndex {
  const refsBySan = new Map<string, MoveReviewMoveRef[]>();
  const lines: MoveReviewMoveRef[][] = [];
  if (!refs) return { refsBySan, lines };

  refs.variations.forEach(variation => {
    const line: MoveReviewMoveRef[] = [];
    variation.moves.forEach(move => {
      const normalized = normalizeSanToken(move.san);
      if (!normalized) return;
      const ref: MoveReviewMoveRef = {
        refId: move.refId,
        san: move.san,
        uci: move.uci,
        fenAfter: move.fenAfter,
      };
      line.push(ref);
      const refsForSan = refsBySan.get(normalized);
      if (refsForSan) refsForSan.push(ref);
      else refsBySan.set(normalized, [ref]);
    });
    if (line.length) lines.push(line);
  });

  return { refsBySan, lines };
}

function sameMoveRef(a: MoveReviewMoveRef, b: MoveReviewMoveRef): boolean {
  return a.uci === b.uci && a.fenAfter === b.fenAfter;
}

function sameMoveRefSequence(a: MoveReviewMoveRef[], b: MoveReviewMoveRef[]): boolean {
  return a.length === b.length && a.every((ref, idx) => sameMoveRef(ref, b[idx]));
}

function boardPayloadForRef(ref: MoveReviewMoveRef | null | undefined): string | null {
  return ref ? `${ref.fenAfter}|${ref.uci}` : null;
}

function equivalentSingleSanRef(normalizedSan: string, refIndex: MoveReviewRefIndex): MoveReviewMoveRef | null {
  const refs = refIndex.refsBySan.get(normalizedSan) || [];
  return equivalentMoveRef(refs);
}

function equivalentMoveRef(refs: MoveReviewMoveRef[]): MoveReviewMoveRef | null {
  if (!refs.length) return null;
  const first = refs[0];
  return refs.every(ref => sameMoveRef(ref, first)) ? first : null;
}

function resolveSanSequenceRefs(rawSans: string[], refIndex: MoveReviewRefIndex): (MoveReviewMoveRef | null)[] {
  const normalizedSans = rawSans.map(normalizeSanToken);
  if (!normalizedSans.length) return [];
  if (normalizedSans.some(san => !san)) return normalizedSans.map(() => null);

  const matches: MoveReviewMoveRef[][] = [];
  for (const line of refIndex.lines) {
    for (let start = 0; start <= line.length - normalizedSans.length; start++) {
      const candidate = line.slice(start, start + normalizedSans.length);
      if (candidate.every((ref, idx) => normalizeSanToken(ref.san) === normalizedSans[idx])) matches.push(candidate);
    }
  }

  if (matches.length) {
    const first = matches[0];
    if (matches.every(match => sameMoveRefSequence(match, first))) return first;
    return normalizedSans.map(() => null);
  }

  if (normalizedSans.length === 1) return [equivalentSingleSanRef(normalizedSans[0], refIndex)];
  return normalizedSans.map(() => null);
}

function firstResolvedSanRef(rawSans: string[], refIndex: MoveReviewRefIndex): MoveReviewMoveRef | null {
  return resolveSanSequenceRefs(rawSans, refIndex).find((ref): ref is MoveReviewMoveRef => !!ref) || null;
}

function lastResolvedSanRef(rawSans: string[], refIndex: MoveReviewRefIndex): MoveReviewMoveRef | null {
  const refs = resolveSanSequenceRefs(rawSans, refIndex).filter((ref): ref is MoveReviewMoveRef => !!ref);
  return refs[refs.length - 1] || null;
}

function firstSurfaceRowRef(rows: MoveReviewPlayerSurfaceRowV1[], refIndex: MoveReviewRefIndex): MoveReviewMoveRef | null {
  for (const row of rows) {
    const ref = firstResolvedSanRef(row.refSans || [], refIndex);
    if (ref) return ref;
  }
  return null;
}

function firstSurfaceSquare(rows: MoveReviewPlayerSurfaceRowV1[]): string | null {
  for (const row of rows) {
    const target = row.authority?.kind === 'opening_family' ? null : row.authority?.target;
    if (target) return target;
  }
  return null;
}

function resolveTrustedDecisionSanRef(
  rawSan: string | null | undefined,
  trustedSans: string[],
  refIndex: MoveReviewRefIndex,
): MoveReviewMoveRef | null {
  const normalizedSan = normalizeSanToken(rawSan);
  if (!normalizedSan || !trustedSans.length) return null;
  const normalizedTrustedSans = trustedSans.map(normalizeSanToken);
  const candidates: MoveReviewMoveRef[] = [];

  normalizedTrustedSans.forEach((san, idx) => {
    if (san !== normalizedSan) return;
    const maxWindow = Math.min(5, trustedSans.length - idx);
    for (let length = maxWindow; length >= 1; length--) {
      const refs = resolveSanSequenceRefs(trustedSans.slice(idx, idx + length), refIndex);
      const ref = refs[0] || null;
      if (ref && normalizeSanToken(ref.san) === normalizedSan) {
        candidates.push(ref);
        break;
      }
    }
  });

  return equivalentMoveRef(candidates);
}

function primaryDecisionRef(
  comparison: MoveReviewPlayerDecisionComparisonV1 | null | undefined,
  refIndex: MoveReviewRefIndex,
): MoveReviewMoveRef | null {
  if (!comparison) return null;
  return (
    resolveTrustedDecisionSanRef(comparison.chosenSan, comparison.refSans || [], refIndex) ||
    resolveTrustedDecisionSanRef(comparison.engineSan, comparison.refSans || [], refIndex) ||
    firstResolvedSanRef(comparison.refSans || [], refIndex)
  );
}

function renderCoachMoveChip(
  label: string,
  move: string | null | undefined,
  ref: MoveReviewMoveRef | null,
  tone: 'chosen' | 'engine' | 'deferred',
): string | null {
  const normalized = normalizeSanToken(move);
  const raw = move?.trim() || normalized;
  if (!normalized || !raw) return null;
  const chip = renderInteractiveSanChip(raw, ref || null, {
    interactiveClasses: 'move-review-coach__move-chip move-chip move-chip--interactive',
    fallbackTag: 'span',
    fallbackClasses: 'move-review-coach__move-chip',
  });

  return `
    <span class="move-review-coach__decision-move move-review-coach__decision-move--${tone}">
      <span class="move-review-coach__decision-label">${escapeHtml(label)}</span>
      ${chip}
    </span>
  `;
}

function renderCoachVerdict(
  comparison: MoveReviewPlayerDecisionComparisonV1 | null | undefined,
  refIndex: MoveReviewRefIndex,
): string | null {
  if (!comparison) return null;
  const chosen = comparison.chosenSan?.trim() || '';
  const best = comparison.engineSan?.trim() || '';
  const compared = comparison.comparedSan?.trim() || '';
  const secondary = comparison.secondaryText?.trim() || '';
  const targetComparison = formatDecisionTargetComparison(comparison.targetComparison);
  const trustedSans = comparison.refSans || [];

  const moveBits = [
    renderCoachMoveChip('You played', chosen, resolveTrustedDecisionSanRef(chosen, trustedSans, refIndex), 'chosen'),
    !comparison.chosenMatchesBest
      ? renderCoachMoveChip('Coach move', best, resolveTrustedDecisionSanRef(best, trustedSans, refIndex), 'engine')
      : null,
    compared
      ? renderCoachMoveChip('Other idea', compared, resolveTrustedDecisionSanRef(compared, trustedSans, refIndex), 'deferred')
      : null,
  ].filter(Boolean);

  if (!moveBits.length && !secondary && !comparison.gapLabel && !targetComparison) return null;

  const classes = [
    'move-review-coach__decision',
    comparison.chosenMatchesBest ? 'move-review-coach__decision--match' : '',
    !chosen && !best ? 'move-review-coach__decision--fallback' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return `
    <section class="${classes}">
      <div class="move-review-coach__decision-topline">
        <span class="move-review-coach__decision-kicker">Your choice</span>
        ${comparison.gapLabel ? `<span class="move-review-coach__gap">${escapeHtml(comparison.gapLabel)}</span>` : ''}
      </div>
      ${moveBits.length ? `<div class="move-review-coach__decision-moves">${moveBits.join('')}</div>` : ''}
      ${secondary ? `<p class="move-review-coach__decision-note">${escapeHtml(secondary)}</p>` : ''}
      ${targetComparison ? `<p class="move-review-coach__target-note">${escapeHtml(targetComparison)}</p>` : ''}
    </section>
  `;
}

function surfaceRowClasses(row: MoveReviewPlayerSurfaceRowV1): string {
  const classes = ['move-review-coach__reason'];
  const tone = (row.tone || '').trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '_');
  if (tone) classes.push(`move-review-coach__reason--tone-${tone}`);
  return classes.join(' ');
}

function renderCoachRefs(row: MoveReviewPlayerSurfaceRowV1, refIndex: MoveReviewRefIndex): string {
  const rowRefs = resolveSanSequenceRefs(row.refSans || [], refIndex);
  const chips = (row.refSans || [])
    .map((san, idx) => {
      if (!normalizeSanToken(san)) return '';
      return renderInteractiveSanChip(san, rowRefs[idx] || null, {
        interactiveClasses: 'move-review-coach__move-chip move-chip move-chip--interactive',
        fallbackTag: 'code',
        fallbackClasses: 'move-review-coach__move-chip',
      });
    })
    .filter(Boolean)
    .join(' ');

  return chips ? `<span class="move-review-coach__refs">${chips}</span>` : '';
}

function renderCoachSurfaceRow(row: MoveReviewPlayerSurfaceRowV1, refIndex: MoveReviewRefIndex): string {
  const target = row.authority?.kind === 'opening_family' ? null : row.authority?.target;
  const targetChip = target
    ? `<span class="move-review-coach__target-chip" data-move-review-square="${escapeHtml(target)}" tabindex="0">${escapeHtml(target)}</span>`
    : '';
  const openingBook = row.authority?.kind === 'opening_family' ? row.authority.openingBook : null;
  const openingBookMarkup = openingBook ? renderOpeningBookMetadata(openingBook) : '';
  const refsMarkup = renderCoachRefs(row, refIndex);

  return `
    <article class="${surfaceRowClasses(row)}">
      <span class="move-review-coach__reason-label">${escapeHtml(row.label)}</span>
      <span class="move-review-coach__reason-text">${escapeHtml(row.text)}</span>
      ${targetChip}
      ${openingBookMarkup}
      ${refsMarkup}
    </article>
  `;
}

function renderOpeningBookMetadata(openingBook: NonNullable<MoveReviewPlayerSurfaceRowV1['authority']>['openingBook']): string {
  if (!openingBook) return '';
  const bits: string[] = [];
  if (openingBook.eco) bits.push(`ECO ${openingBook.eco}`);
  if (openingBook.totalGames) bits.push(`${formatOpeningGameCount(openingBook.totalGames)} games`);
  if (openingBook.topMoves.length) bits.push(`Book: ${openingBook.topMoves.slice(0, 3).join(' / ')}`);
  if (!bits.length) return '';
  return `<span class="move-review-coach__book">${bits
    .map(bit => `<span class="move-review-coach__book-chip">${escapeHtml(bit)}</span>`)
    .join('')}</span>`;
}

function formatOpeningGameCount(value: number): string {
  const count = Math.max(0, Math.trunc(value));
  if (count >= 1000000) return `${(count / 1000000).toFixed(count >= 10000000 ? 0 : 1)}M`;
  if (count >= 1000) return `${(count / 1000).toFixed(count >= 10000 ? 0 : 1)}k`;
  return `${count}`;
}

function surfaceStatusLabel(status: string): string {
  const key = status.trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '_');
  if (key === 'resolved') return 'Backed by line';
  if (key === 'pending') return 'Still to check';
  if (key === 'question_only') return 'Question to keep';
  return status
    .replace(/[_-]+/g, ' ')
    .split(/\s+/)
    .filter(Boolean)
    .map(part => `${part.charAt(0).toUpperCase()}${part.slice(1).toLowerCase()}`)
    .join(' ');
}

function renderAuthorRow(row: MoveReviewPlayerAuthorRowV1, refIndex: MoveReviewRefIndex): string {
  const statusKey = (row.status || 'question_only').trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '_');
  const branchMarkup = (row.branches || [])
    .map(branch => {
      const branchRefs = renderCoachRefs(branch, refIndex);
      const branchMove =
        branchRefs || `<span class="move-review-coach__branch-label">${escapeHtml(branch.label)}</span>`;
      return `
        <div class="move-review-coach__question-branch">
          ${branchMove}
          <span>${escapeHtml(branch.text)}</span>
        </div>
      `;
    })
    .join('');

  return `
    <article class="move-review-coach__question">
      <div class="move-review-coach__question-head">
        <strong>${escapeHtml(row.title)}</strong>
        <span class="move-review-coach__status move-review-coach__status--${escapeHtml(statusKey)}">${escapeHtml(surfaceStatusLabel(row.status))}</span>
      </div>
      <p>${escapeHtml(row.question)}</p>
      ${row.why ? `<p class="move-review-coach__question-why">${escapeHtml(row.why)}</p>` : ''}
      ${branchMarkup ? `<div class="move-review-coach__question-branches">${branchMarkup}</div>` : ''}
    </article>
  `;
}

type ResolvedTryLine = {
  cleanSans: string[];
  refs: (MoveReviewMoveRef | null)[];
};

function resolveTryLine(lineSans: string[], refIndex: MoveReviewRefIndex): ResolvedTryLine {
  const cleanSans = lineSans.filter(san => normalizeSanToken(san));
  return {
    cleanSans,
    refs: cleanSans.length ? resolveSanSequenceRefs(cleanSans, refIndex) : [],
  };
}

function renderResolvedTryLineChips(line: ResolvedTryLine): string {
  if (!line.cleanSans.length) return '';
  return line.cleanSans
    .map((san, idx) =>
      renderInteractiveSanChip(san, line.refs[idx] || null, {
        interactiveClasses: 'move-review-coach__move-chip move-chip move-chip--interactive',
        fallbackTag: 'code',
        fallbackClasses: 'move-review-coach__move-chip',
      }),
    )
    .join(' ');
}

function renderTryLineChips(lineSans: string[], refIndex: MoveReviewRefIndex): string {
  return renderResolvedTryLineChips(resolveTryLine(lineSans, refIndex));
}

function renderSceneLine(scene: MoveReviewScene, refIndex: MoveReviewRefIndex): string {
  const lineSans = scene.lineSans || [];
  const resolvedLine = resolveTryLine(lineSans, refIndex);
  const boardMoveCount = resolvedLine.refs.filter(Boolean).length;
  const chips = renderResolvedTryLineChips(resolvedLine);
  if (!chips) return '';
  const controls =
    boardMoveCount > 1
      ? `<span class="move-review-player__line-controls" aria-label="Replay line controls">
          <button type="button" class="move-review-player__line-step" data-move-review-line-step="-1" disabled>Back</button>
          <span class="move-review-player__line-count" aria-live="polite">Board 1/${boardMoveCount}</span>
          <button type="button" class="move-review-player__line-step" data-move-review-line-step="1">Next</button>
        </span>`
      : '';
  return `
    <div class="move-review-player__scene-line" data-scene-line="${escapeHtml(lineSans.join(' '))}">
      <span class="move-review-player__scene-line-head">
        <span class="move-review-player__scene-line-label">${escapeHtml(scene.lineLabel || 'Line to play through')}</span>
        ${controls}
      </span>
      <span class="move-review-player__scene-line-chips">${chips}</span>
    </div>
  `;
}

function renderBoardCue(scene: MoveReviewScene | undefined): string {
  const focusSquare = scene?.square || '';
  const line = (scene?.lineSans || []).filter(san => normalizeSanToken(san)).slice(0, 4).join(' ');
  const hidden = !focusSquare && !line ? ' hidden' : '';
  return `
    <div class="move-review-player__board-cue"${hidden}>
      <span class="move-review-player__board-cue-item move-review-player__board-cue-item--square"${focusSquare ? '' : ' hidden'}>
        <span>Watch</span>
        <strong>${escapeHtml(focusSquare)}</strong>
      </span>
      <span class="move-review-player__board-cue-item move-review-player__board-cue-item--line"${line ? '' : ' hidden'}>
        <span>Line</span>
        <strong>${escapeHtml(line)}</strong>
      </span>
      <span class="move-review-player__board-cue-item move-review-player__board-cue-item--move" hidden>
        <span>Now</span>
        <strong></strong>
      </span>
    </div>
  `;
}

function primaryTryLine(playerSurface: MoveReviewPlayerSurfaceV1): string[] {
  const decisionLine = playerSurface.decisionComparison?.refSans || [];
  if (decisionLine.length) return decisionLine;
  return playerSurface.summaryRows.find(row => row.refSans.length)?.refSans || [];
}

function renderSceneNav(scenes: MoveReviewScene[]): string {
  return `
    <nav class="move-review-player__timeline" aria-label="Review scenes">
      ${scenes
        .map(
          (scene, idx) => `
        <button
          id="move-review-scene-tab-${scene.key}"
          type="button"
          class="move-review-player__timeline-step${idx === 0 ? ' is-active' : ''}"
          data-move-review-scene="${idx}"
          role="tab"
          aria-selected="${idx === 0 ? 'true' : 'false'}"
          aria-controls="move-review-scene-${scene.key}"
          tabindex="${idx === 0 ? '0' : '-1'}"
        >
          <span class="move-review-player__timeline-index">${idx + 1}</span>
          <span class="move-review-player__timeline-copy">
            <span class="move-review-player__timeline-label">${escapeHtml(scene.label)}</span>
            <span class="move-review-player__timeline-kicker">${escapeHtml(scene.kicker)}</span>
          </span>
        </button>
      `,
        )
        .join('')}
    </nav>
  `;
}

function renderScenePanel(scene: MoveReviewScene, idx: number, refIndex: MoveReviewRefIndex, sceneCount: number): string {
  const board = scene.board ? ` data-scene-board="${escapeHtml(scene.board)}"` : '';
  const boardKicker = ` data-scene-board-kicker="${escapeHtml(scene.kicker)}"`;
  const boardTitle = ` data-scene-board-title="${escapeHtml(scene.boardTitle)}"`;
  const boardSubtitle = scene.boardSubtitle ? ` data-scene-board-subtitle="${escapeHtml(scene.boardSubtitle)}"` : '';
  const square = scene.square ? ` data-scene-square="${escapeHtml(scene.square)}"` : '';
  const label = ` data-scene-label="${escapeHtml(scene.label)}" data-scene-short-label="${escapeHtml(scene.shortLabel)}"`;
  const hidden = idx === 0 ? '' : ' hidden aria-hidden="true"';
  return `
    <section
      id="move-review-scene-${scene.key}"
      class="move-review-player__scene move-review-player__scene--${scene.key}${idx === 0 ? ' is-active' : ''}"
      data-move-review-scene-panel
      data-scene-index="${idx}"
      data-scene-key="${scene.key}"
      role="tabpanel"
      aria-labelledby="move-review-scene-tab-${scene.key}"
      ${board}${boardKicker}${boardTitle}${boardSubtitle}${square}${label}${hidden}
    >
      <header class="move-review-player__scene-head">
        <span class="move-review-player__scene-kicker">${escapeHtml(scene.kicker)} · ${idx + 1}/${sceneCount}</span>
        <h4>${escapeHtml(scene.title)}</h4>
      </header>
      ${renderSceneLine(scene, refIndex)}
      <div class="move-review-player__scene-body">${scene.body}</div>
    </section>
  `;
}

function renderSceneControls(scenes: MoveReviewScene[]): string {
  const sceneCount = scenes.length;
  const nextLabel = sceneCount > 1 ? scenes[1]?.shortLabel || 'scene' : null;
  return `
    <footer class="move-review-player__controls">
      <button type="button" class="move-review-player__control" data-move-review-scene-step="-1" disabled>Back</button>
      <span class="move-review-player__scene-count" aria-live="polite">${escapeHtml(scenes[0]?.label || 'Scene')} · 1/${sceneCount}</span>
      <button type="button" class="move-review-player__control move-review-player__control--primary" data-move-review-scene-step="1"${
        sceneCount <= 1 ? ' disabled' : ''
      }>${nextLabel ? `Next: ${escapeHtml(nextLabel)}` : 'Next'}</button>
    </footer>
  `;
}

function renderMoreToCheck(
  probeRows: MoveReviewPlayerSurfaceRowV1[],
  authorRows: MoveReviewPlayerAuthorRowV1[],
  refIndex: MoveReviewRefIndex,
): string {
  const probeMarkup = probeRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const authorMarkup = authorRows.map(row => renderAuthorRow(row, refIndex)).join('');
  if (!probeMarkup && !authorMarkup) return '';
  return `<details class="move-review-coach__details move-review-player__detail-layer"><summary>Study the follow-up</summary>${
    probeMarkup ? `<div class="move-review-coach__subsection"><h5>If this line appears</h5>${probeMarkup}</div>` : ''
  }${
    authorMarkup ? `<div class="move-review-coach__subsection"><h5>What to ask at the board</h5>${authorMarkup}</div>` : ''
  }</details>`;
}

function renderRememberSceneBody(
  html: string,
  hasCoachSurface: boolean,
  probeRows: MoveReviewPlayerSurfaceRowV1[],
  authorRows: MoveReviewPlayerAuthorRowV1[],
  refIndex: MoveReviewRefIndex,
): string {
  const practice = hasCoachSurface
    ? '<section class="move-review-coach__practice"><strong>Remember this</strong><span>When this structure appears again, find the board cue before naming the move.</span></section>'
    : '';
  const coachNotes = html
    ? `<details class="move-review-coach__details move-review-player__detail-layer"><summary>Coach notes</summary><div class="move-review-coach__body">${html}</div></details>`
    : '';
  return `${practice}${coachNotes}${renderMoreToCheck(probeRows, authorRows, refIndex)}`;
}

function buildMoveReviewScenes(
  html: string,
  playerSurface: MoveReviewPlayerSurfaceV1,
  refIndex: MoveReviewRefIndex,
): MoveReviewScene[] {
  const titleText = playerSurface.title?.trim() || 'Coach review';
  const decision = renderCoachVerdict(playerSurface.decisionComparison, refIndex);
  const summaryRows = playerSurface.summaryRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const primaryLine = primaryTryLine(playerSurface);
  const hasTryLine = !!renderTryLineChips(primaryLine, refIndex);
  const advancedRows = playerSurface.advancedRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const planRows = advancedRows || summaryRows;

  const decisionRef = primaryDecisionRef(playerSurface.decisionComparison, refIndex) || firstResolvedSanRef(primaryLine, refIndex);
  const summaryRef = firstSurfaceRowRef(playerSurface.summaryRows, refIndex) || decisionRef;
  const planSourceRows = playerSurface.advancedRows.length ? playerSurface.advancedRows : playerSurface.summaryRows;
  const planRef = firstSurfaceRowRef(planSourceRows, refIndex) || summaryRef;
  const tryStartRef = firstResolvedSanRef(primaryLine, refIndex) || planRef;
  const tryEndRef = lastResolvedSanRef(primaryLine, refIndex) || planRef;
  const summarySquare = firstSurfaceSquare(playerSurface.summaryRows);
  const planSquare = firstSurfaceSquare(planSourceRows) || summarySquare;
  const hasCoachSurface = !!(decision || summaryRows || hasTryLine || planRows || playerSurface.probeRows.length || playerSurface.authorRows.length);

  const scenes: MoveReviewScene[] = [
    {
      key: 'verdict',
      label: 'Verdict',
      shortLabel: 'Verdict',
      title: titleText,
      kicker: 'Decision',
      body:
        decision ||
        '<p class="move-review-player__empty">Start from the current position, then move through the coach scenes.</p>',
      board: boardPayloadForRef(decisionRef),
      boardTitle: 'Position tied to the choice',
      boardSubtitle: playerSurface.decisionComparison?.chosenSan || playerSurface.decisionComparison?.engineSan || null,
      square: summarySquare,
      lineSans: primaryLine,
      lineLabel: 'Line behind the choice',
    },
  ];

  if (summaryRows) {
    scenes.push({
      key: 'why',
      label: 'Why',
      shortLabel: 'Why',
      title: 'Why it mattered',
      kicker: 'Reason',
      body: `<div class="move-review-coach__reasons">${summaryRows}</div>`,
      board: boardPayloadForRef(summaryRef),
      boardTitle: 'Reason position',
      boardSubtitle: playerSurface.summaryRows[0]?.label || null,
      square: summarySquare,
      lineSans: playerSurface.summaryRows.find(row => row.refSans.length)?.refSans || primaryLine,
      lineLabel: 'Moves behind the reason',
    });
  }

  if (planRows) {
    scenes.push({
      key: 'plan',
      label: 'Plan',
      shortLabel: 'Plan',
      title: 'What to watch next',
      kicker: 'Plan',
      body: `<div class="move-review-coach__reasons">${planRows}</div>`,
      board: boardPayloadForRef(planRef),
      boardTitle: 'Plan position',
      boardSubtitle: planSourceRows[0]?.label || null,
      square: planSquare,
      lineSans: planSourceRows.find(row => row.refSans.length)?.refSans || primaryLine,
      lineLabel: 'Plan in moves',
    });
  }

  if (hasTryLine) {
    scenes.push({
      key: 'try',
      label: 'Try line',
      shortLabel: 'Line',
      title: 'Try the line',
      kicker: 'Replay',
      body: '',
      board: boardPayloadForRef(tryStartRef),
      boardTitle: 'Line position',
      boardSubtitle: primaryLine[0] || null,
      square: planSquare || summarySquare,
      lineSans: primaryLine,
      lineLabel: 'Replay on the board',
    });
  }

  const rememberBody = renderRememberSceneBody(html, hasCoachSurface, playerSurface.probeRows, playerSurface.authorRows, refIndex);
  if (rememberBody) {
    scenes.push({
      key: 'remember',
      label: 'Remember',
      shortLabel: 'Recall',
      title: 'Remember this',
      kicker: 'Memory',
      body: rememberBody,
      board: boardPayloadForRef(tryEndRef || summaryRef || decisionRef),
      boardTitle: 'Pattern position',
      boardSubtitle: primaryLine[primaryLine.length - 1] || planSourceRows[0]?.label || null,
      square: planSquare || summarySquare,
      lineSans: primaryLine,
      lineLabel: 'Pattern in moves',
    });
  }

  return scenes;
}

function fallbackPlayerSurfaceFromRefs(refs: MoveReviewRefsV1 | null): MoveReviewPlayerSurfaceV1 | null {
  const primaryLine =
    refs?.variations
      .find(variation => variation.moves.length)
      ?.moves.map(move => move.san)
      .filter(san => normalizeSanToken(san)) || [];
  if (!primaryLine.length) return null;

  return {
    schema: 'chesstory.move_review.player_surface.v1',
    title: 'Coach review',
    summaryRows: [],
    advancedRows: [],
    decisionComparison: {
      kicker: 'Replay',
      chosenSan: null,
      engineSan: null,
      comparedSan: null,
      secondaryText: null,
      gapLabel: null,
      chosenMatchesBest: true,
      targetComparison: null,
      refSans: primaryLine,
    },
    probeRows: [],
    authorRows: [],
  };
}

export function decorateMoveReviewHtml(
  html: string,
  refs: MoveReviewRefsV1 | null,
  playerSurface: MoveReviewPlayerSurfaceV1 | null,
): string {
  const surface = playerSurface || fallbackPlayerSurfaceFromRefs(refs);
  if (!surface) return html;

  const refIndex = buildMoveReviewRefIndex(refs);
  const titleText = surface.title?.trim() || 'Coach review';
  const scenes = buildMoveReviewScenes(html, surface, refIndex);
  const sceneCount = scenes.length;

  return `
    <div class="move-review-coach move-review-player" data-move-review-player data-scene-index="0">
      <header class="move-review-coach__header">
        <span class="move-review-coach__eyebrow">Move review</span>
        <h3>${escapeHtml(titleText)}</h3>
      </header>
      ${renderSceneNav(scenes)}
      <div class="move-review-player__stage">
        <aside class="move-review-player__board-shell" aria-label="Current coaching board">
          <div class="move-review-player__board-meta">
            <span class="move-review-player__board-kicker">Board follows · ${escapeHtml(scenes[0]?.kicker || 'Scene')}</span>
            <strong class="move-review-player__board-title">${escapeHtml(scenes[0]?.boardTitle || 'Coaching board')}</strong>
            <span class="move-review-player__board-subtitle">${escapeHtml(scenes[0]?.boardSubtitle || scenes[0]?.label || '')}</span>
          </div>
          ${renderBoardCue(scenes[0])}
          <div class="move-review-pv-preview move-review-player__board-preview"></div>
        </aside>
        <div class="move-review-player__scene-stack">${scenes.map((scene, idx) => renderScenePanel(scene, idx, refIndex, sceneCount)).join('')}</div>
      </div>
      ${renderSceneControls(scenes)}
    </div>
  `;
}
