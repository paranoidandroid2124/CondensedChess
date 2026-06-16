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
    renderCoachMoveChip('Chosen', chosen, resolveTrustedDecisionSanRef(chosen, trustedSans, refIndex), 'chosen'),
    !comparison.chosenMatchesBest
      ? renderCoachMoveChip('Suggested', best, resolveTrustedDecisionSanRef(best, trustedSans, refIndex), 'engine')
      : null,
    compared
      ? renderCoachMoveChip('Compared', compared, resolveTrustedDecisionSanRef(compared, trustedSans, refIndex), 'deferred')
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
        <span class="move-review-coach__decision-kicker">Coach verdict</span>
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
      const branchMove = branchRefs || `<code class="move-review-coach__move-chip">${escapeHtml(branch.label)}</code>`;
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

function renderTryLine(lineSans: string[], refIndex: MoveReviewRefIndex): string {
  const cleanSans = lineSans.filter(san => normalizeSanToken(san));
  if (!cleanSans.length) return '';
  const refs = resolveSanSequenceRefs(cleanSans, refIndex);
  const chips = cleanSans
    .map((san, idx) =>
      renderInteractiveSanChip(san, refs[idx] || null, {
        interactiveClasses: 'move-review-coach__move-chip move-chip move-chip--interactive',
        fallbackTag: 'code',
        fallbackClasses: 'move-review-coach__move-chip',
      }),
    )
    .join(' ');

  return `
    <section class="move-review-coach__section move-review-coach__section--line">
      <h4>Try the line</h4>
      <div class="move-review-coach__line">${chips}</div>
    </section>
  `;
}

function primaryTryLine(playerSurface: MoveReviewPlayerSurfaceV1): string[] {
  const decisionLine = playerSurface.decisionComparison?.refSans || [];
  if (decisionLine.length) return decisionLine;
  return playerSurface.summaryRows.find(row => row.refSans.length)?.refSans || [];
}

export function decorateMoveReviewHtml(
  html: string,
  refs: MoveReviewRefsV1 | null,
  playerSurface: MoveReviewPlayerSurfaceV1 | null,
): string {
  if (!playerSurface) return html;

  const refIndex = buildMoveReviewRefIndex(refs);
  const titleText = playerSurface.title?.trim() || 'Move review';
  const decision = renderCoachVerdict(playerSurface.decisionComparison, refIndex);
  const summaryRows = playerSurface.summaryRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const advancedRows = playerSurface.advancedRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const probeRows = playerSurface.probeRows.map(row => renderCoachSurfaceRow(row, refIndex)).join('');
  const authorRows = playerSurface.authorRows.map(row => renderAuthorRow(row, refIndex)).join('');
  const tryLine = renderTryLine(primaryTryLine(playerSurface), refIndex);
  const hasDeepReview = !!(advancedRows || probeRows || authorRows);
  const hasCoachSurface = !!(decision || summaryRows || tryLine || hasDeepReview);

  return `
    <div class="move-review-coach">
      <header class="move-review-coach__header">
        <span class="move-review-coach__eyebrow">Key moment</span>
        <h3>${escapeHtml(titleText)}</h3>
      </header>
      ${decision || ''}
      ${summaryRows ? `<section class="move-review-coach__section"><h4>Why it mattered</h4><div class="move-review-coach__reasons">${summaryRows}</div></section>` : ''}
      ${tryLine}
      <section class="move-review-coach__section move-review-coach__section--body">
        ${hasCoachSurface ? '<h4>Coach notes</h4>' : ''}
        <div class="move-review-coach__body">${html}</div>
      </section>
      ${
        summaryRows || decision
          ? '<section class="move-review-coach__practice"><strong>Remember this</strong><span>Replay the line, then try to name the idea before checking the note.</span></section>'
          : ''
      }
      ${
        hasDeepReview
          ? `<details class="move-review-coach__details"><summary>More to check</summary>${advancedRows}${
              probeRows ? `<div class="move-review-coach__subsection"><h5>Extra lines</h5>${probeRows}</div>` : ''
            }${
              authorRows ? `<div class="move-review-coach__subsection"><h5>Review questions</h5>${authorRows}</div>` : ''
            }</details>`
          : ''
      }
    </div>
  `;
}
