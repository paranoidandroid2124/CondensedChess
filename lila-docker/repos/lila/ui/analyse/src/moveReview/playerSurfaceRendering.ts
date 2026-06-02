import { formatDecisionTargetComparison } from '../decisionComparison';
import {
  escapeHtml,
  normalizeSanToken,
  renderInteractiveSanChip,
  type MoveReviewInteractiveRefLike,
} from './surfaceShared';
import type {
  MoveReviewExplanationV1,
  MoveReviewPlayerAuthorRowV1,
  MoveReviewPlayerDecisionComparisonV1,
  MoveReviewPlayerSurfaceRowV1,
  MoveReviewPlayerSurfaceV1,
  MoveReviewRefsV1,
} from './responsePayload';

type MoveReviewMoveRef = MoveReviewInteractiveRefLike & {
  fenAfter: string;
};

type MoveReviewRefIndex = {
  firstBySan: Map<string, MoveReviewMoveRef>;
  anyBySan: Map<string, MoveReviewMoveRef>;
};

function buildMoveReviewRefIndex(refs: MoveReviewRefsV1 | null): MoveReviewRefIndex {
  const firstBySan = new Map<string, MoveReviewMoveRef>();
  const anyBySan = new Map<string, MoveReviewMoveRef>();
  if (!refs) return { firstBySan, anyBySan };

  refs.variations.forEach(variation => {
    variation.moves.forEach((move, idx) => {
      const normalized = normalizeSanToken(move.san);
      if (!normalized) return;
      const ref: MoveReviewMoveRef = {
        refId: move.refId,
        san: move.san,
        uci: move.uci,
        fenAfter: move.fenAfter,
      };
      if (idx === 0 && !firstBySan.has(normalized)) firstBySan.set(normalized, ref);
      if (!anyBySan.has(normalized)) anyBySan.set(normalized, ref);
    });
  });

  return { firstBySan, anyBySan };
}

function renderMoveReviewMoveChip(
  label: string,
  move: string | null | undefined,
  refIndex: Map<string, MoveReviewMoveRef>,
  tone: 'chosen' | 'engine' | 'deferred',
): string | null {
  const normalized = normalizeSanToken(move);
  const raw = move?.trim() || normalized;
  if (!normalized || !raw) return null;
  const ref = refIndex.get(normalized);
  const chip = renderInteractiveSanChip(raw, ref || null, {
    interactiveClasses: 'move-review-decision-compare__move-chip move-chip move-chip--interactive',
    fallbackTag: 'span',
    fallbackClasses: 'move-review-decision-compare__move-chip',
  });

  return `
    <span class="move-review-decision-compare__move move-review-decision-compare__move--${tone}">
      <span class="move-review-decision-compare__move-label">${escapeHtml(label)}</span>
      ${chip}
    </span>
  `;
}

function renderDecisionCompareStrip(
  comparison: MoveReviewPlayerDecisionComparisonV1 | null | undefined,
  refIndex: MoveReviewRefIndex,
): string | null {
  if (!comparison) return null;
  const chosen = comparison.chosenSan?.trim() || '';
  const best = comparison.engineSan?.trim() || '';
  const compared = comparison.comparedSan?.trim() || '';
  const secondary = comparison.secondaryText?.trim() || '';
  const targetComparison = formatDecisionTargetComparison(comparison.targetComparison);

  const moveBits = [
    renderMoveReviewMoveChip('Chosen', chosen, refIndex.firstBySan, 'chosen'),
    !comparison.chosenMatchesBest ? renderMoveReviewMoveChip('Engine', best, refIndex.firstBySan, 'engine') : null,
    compared ? renderMoveReviewMoveChip('Compared', compared, refIndex.firstBySan, 'deferred') : null,
  ].filter(Boolean);

  if (!moveBits.length && !secondary) return null;

  const classes = [
    'move-review-decision-compare',
    comparison.chosenMatchesBest ? 'move-review-decision-compare--match' : '',
    !chosen && !best ? 'move-review-decision-compare--fallback' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return `
    <div class="${classes}">
      <div class="move-review-decision-compare__topline">
        <span class="move-review-decision-compare__kicker">${escapeHtml(comparison.kicker)}</span>
        ${comparison.gapLabel ? `<span class="move-review-decision-compare__gap">${escapeHtml(comparison.gapLabel)}</span>` : ''}
      </div>
      ${moveBits.length ? `<div class="move-review-decision-compare__moves">${moveBits.join('')}</div>` : ''}
      ${secondary ? `<div class="move-review-decision-compare__secondary">${escapeHtml(secondary)}</div>` : ''}
      ${targetComparison ? `<div class="move-review-decision-compare__target">${escapeHtml(targetComparison)}</div>` : ''}
    </div>
  `;
}

function surfaceRowClasses(row: MoveReviewPlayerSurfaceRowV1): string {
  const classes = ['move-review-strategic-summary__row'];
  const tone = (row.tone || '').trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '_');
  const authority = (row.authority?.kind || '').trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '_');
  if (tone) classes.push(`move-review-strategic-summary__row--tone-${tone}`);
  if (authority) classes.push(`move-review-strategic-summary__row--authority-${authority}`);
  return classes.join(' ');
}

function renderSurfaceRow(row: MoveReviewPlayerSurfaceRowV1, refIndex: MoveReviewRefIndex): string {
  const chips = (row.refSans || [])
    .map(san => {
      const normalized = normalizeSanToken(san);
      if (!normalized) return '';
      const ref = refIndex.anyBySan.get(normalized) || null;
      return renderInteractiveSanChip(san, ref, {
        interactiveClasses: 'move-review-strategic-summary__move-chip move-chip move-chip--interactive',
        fallbackTag: 'code',
        fallbackClasses: 'move-review-strategic-summary__move-chip',
      });
    })
    .filter(Boolean)
    .join(' ');
  const target = row.authority?.target;
  const targetChip = target
    ? `<span class="move-review-strategic-summary__target-chip" data-move-review-square="${escapeHtml(target)}" tabindex="0">${escapeHtml(target)}</span>`
    : '';
  const relationToken = row.authority?.kind === 'strategic_relation' ? row.authority.token : null;
  const relationChip = relationToken
    ? `<span class="move-review-strategic-summary__relation-chip">${escapeHtml(formatSurfaceAuthorityLabel(relationToken))}</span>`
    : '';
  const openingBook = row.authority?.kind === 'opening_family' ? row.authority.openingBook : null;
  const openingBookMarkup = openingBook ? renderOpeningBookMetadata(openingBook) : '';
  return `
    <div class="${surfaceRowClasses(row)}">
      <strong>${escapeHtml(row.label)}:</strong> ${escapeHtml(row.text)}
      ${relationChip}
      ${targetChip}
      ${openingBookMarkup}
      ${chips ? `<span class="move-review-strategic-summary__refs">${chips}</span>` : ''}
    </div>
  `;
}

function formatSurfaceAuthorityLabel(value: string): string {
  if (value === 'xray') return 'X-ray';
  return value
    .replace(/[_-]+/g, ' ')
    .split(/\s+/)
    .filter(Boolean)
    .map(part => `${part.charAt(0).toUpperCase()}${part.slice(1).toLowerCase()}`)
    .join(' ');
}

function renderOpeningBookMetadata(openingBook: NonNullable<MoveReviewPlayerSurfaceRowV1['authority']>['openingBook']): string {
  if (!openingBook) return '';
  const bits: string[] = [];
  if (openingBook.eco) bits.push(`ECO ${openingBook.eco}`);
  if (openingBook.totalGames) bits.push(`${formatOpeningGameCount(openingBook.totalGames)} games`);
  if (openingBook.topMoves.length) bits.push(`Book: ${openingBook.topMoves.slice(0, 3).join(' / ')}`);
  if (!bits.length) return '';
  return `<span class="move-review-strategic-summary__opening-book">${bits
    .map(bit => `<span class="move-review-strategic-summary__opening-book-chip">${escapeHtml(bit)}</span>`)
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
      const san = (branch.refSans && branch.refSans[0]) || branch.label;
      const normalized = normalizeSanToken(san);
      const moveRef = normalized ? refIndex.anyBySan.get(normalized) : null;
      const branchMove = moveRef
        ? `<span class="move-review-authoring-summary__branch-move move-chip move-chip--interactive" data-ref-id="${escapeHtml(moveRef.refId)}" data-uci="${escapeHtml(moveRef.uci)}" data-san="${escapeHtml(moveRef.san)}" tabindex="0">${escapeHtml(branch.label)}</span>`
        : `<code>${escapeHtml(branch.label)}</code>`;
      return `
        <div class="move-review-authoring-summary__branch">
          ${branchMove}
          <span>${escapeHtml(branch.text)}</span>
        </div>
      `;
    })
    .join('');
  return `
    <div class="move-review-authoring-summary__card">
      <div class="move-review-authoring-summary__head">
        <strong>${escapeHtml(row.title)}</strong>
        <span class="move-review-authoring-summary__status move-review-authoring-summary__status--${escapeHtml(statusKey)}">${escapeHtml(surfaceStatusLabel(row.status))}</span>
      </div>
      <div class="move-review-authoring-summary__question">${escapeHtml(row.question)}</div>
      ${row.why ? `<div class="move-review-authoring-summary__why">${escapeHtml(row.why)}</div>` : ''}
      ${branchMarkup ? `<div class="move-review-authoring-summary__branches">${branchMarkup}</div>` : ''}
    </div>
  `;
}

export function renderMoveReviewPlayerSurfaceHtml(input: {
  html: string;
  moveReviewExplanation: MoveReviewExplanationV1 | null;
  refs: MoveReviewRefsV1 | null;
  playerSurface: MoveReviewPlayerSurfaceV1 | null;
}): string {
  const rows: string[] = [];
  const advancedRows: string[] = [];
  const probeRows: string[] = [];
  const authorRows: string[] = [];
  const refIndex = buildMoveReviewRefIndex(input.refs);
  const titleText = input.playerSurface?.title?.trim() || input.moveReviewExplanation?.title?.trim() || '';
  const moveReviewTitle = titleText ? `<div class="move-review-move-review__title">${escapeHtml(titleText)}</div>` : '';

  if (!input.playerSurface) return moveReviewTitle ? `${moveReviewTitle}${input.html}` : input.html;

  input.playerSurface.summaryRows.forEach(row => rows.push(renderSurfaceRow(row, refIndex)));
  const decisionStrip = renderDecisionCompareStrip(input.playerSurface.decisionComparison, refIndex);
  if (decisionStrip) rows.push(decisionStrip);
  input.playerSurface.advancedRows.forEach(row => advancedRows.push(renderSurfaceRow(row, refIndex)));
  input.playerSurface.probeRows.forEach(row => probeRows.push(renderSurfaceRow(row, refIndex)));
  input.playerSurface.authorRows.forEach(row => authorRows.push(renderAuthorRow(row, refIndex)));

  if (!rows.length && !advancedRows.length && !probeRows.length && !authorRows.length)
    return moveReviewTitle ? `${moveReviewTitle}${input.html}` : input.html;

  return `
    ${moveReviewTitle}
    <div class="move-review-strategic-summary">
      <div class="move-review-strategic-summary__title">Support</div>
      ${rows.join('')}
      ${
        advancedRows.length || probeRows.length || authorRows.length
          ? `
        <details class="move-review-strategic-summary__details">
          <summary class="move-review-strategic-summary__details-summary">Advanced details</summary>
          ${advancedRows.join('')}
          ${
            probeRows.length
              ? `
            <div class="move-review-probe-summary">
              <div class="move-review-probe-summary__title">Analytical Lines</div>
              ${probeRows.join('')}
            </div>
          `
              : ''
          }
          ${
            authorRows.length
              ? `
            <div class="move-review-authoring-summary">
              <div class="move-review-probe-summary__title">Alternative Lines</div>
              ${authorRows.join('')}
            </div>
          `
              : ''
          }
        </details>
      `
          : ''
      }
    </div>
    ${input.html}
  `;
}
