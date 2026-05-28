import { pubsub } from 'lib/pubsub';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';
import type { MoveReviewRefsV1 } from './responsePayload';
import type AnalyseCtrl from '../ctrl';

type MoveReviewPreviewState = {
  cg?: CgApi;
  container?: HTMLElement;
};

type MoveReviewHoverCtrl = AnalyseCtrl & {
  moveReviewHoverSquare?: Key | null;
};

let handlersBound = false;
let currentCtrl: MoveReviewHoverCtrl | undefined;
let moveReviewPreview: MoveReviewPreviewState = {};
let moveReviewPreviewOrientation: Color = 'white';
let touchHoldTimer: ReturnType<typeof setTimeout> | undefined;
let touchHoldTriggered = false;
let suppressNextTap = false;
let swipeStartX = 0;
let swipeStartY = 0;
let swipeActiveList: HTMLElement | null = null;
let refsById = new Map<string, { fenAfter: string; uci: string }>();

function boardPayloadFromElement(el: HTMLElement): string | null {
  const refId = el.dataset.refId;
  if (refId) {
    const ref = refsById.get(refId);
    if (ref) return `${ref.fenAfter}|${ref.uci}`;
  }
  return el.dataset.board ?? null;
}

export function setMoveReviewRefs(refs: MoveReviewRefsV1 | null): void {
  refsById = new Map<string, { fenAfter: string; uci: string }>();
  if (!refs) return;
  for (const variation of refs.variations) {
    for (const move of variation.moves) {
      refsById.set(move.refId, { fenAfter: move.fenAfter, uci: move.uci });
    }
  }
}

export function setMoveReviewPreviewOrientation(orientation: Color): void {
  moveReviewPreviewOrientation = orientation;
}

function cancelTouchHold(): void {
  if (!touchHoldTimer) return;
  clearTimeout(touchHoldTimer);
  touchHoldTimer = undefined;
}

function setActiveVariationItem(item: HTMLElement, syncPreview = true): void {
  const list = item.closest('.variation-list') as HTMLElement | null;
  if (!list) return;
  list.querySelectorAll<HTMLElement>('.variation-item.is-active').forEach(el => {
    if (el !== item) el.classList.remove('is-active');
  });
  item.classList.add('is-active');
  if (!syncPreview) return;
  const firstMove = item.querySelector<HTMLElement>('[data-ref-id], [data-board]');
  const board = firstMove ? boardPayloadFromElement(firstMove) : null;
  if (board) updateMoveReviewPreview(board);
}

function stepVariation(list: HTMLElement, direction: 1 | -1): void {
  const items = Array.from(list.querySelectorAll<HTMLElement>('.variation-item'));
  if (!items.length) return;
  const activeIndex = items.findIndex(item => item.classList.contains('is-active'));
  const current = activeIndex >= 0 ? activeIndex : 0;
  const next = Math.max(0, Math.min(items.length - 1, current + direction));
  if (next === current && activeIndex >= 0) return;
  const nextItem = items[next];
  setActiveVariationItem(nextItem, true);
  nextItem.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
}

function isBoardSquare(value: unknown): value is Key {
  return typeof value === 'string' && /^[a-h][1-8]$/.test(value);
}

export function initMoveReviewHandlers(ctrl: AnalyseCtrl | undefined, onEvalToggle: () => void): void {
  currentCtrl = ctrl as MoveReviewHoverCtrl | undefined;
  if (handlersBound) return;
  handlersBound = true;

  $(document)
    .on('mouseover.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      const board = boardPayloadFromElement(this);
      if (board) updateMoveReviewPreview(board);
    })
    .on('focusin.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      const board = boardPayloadFromElement(this);
      if (board) updateMoveReviewPreview(board);
    })
    .on('focusout.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      const root = this.closest('.analyse__move-review-text') as HTMLElement | null;
      setTimeout(() => {
        if (root?.contains(document.activeElement)) return;
        hideMoveReviewPreview();
      }, 0);
    })
    .on('mouseleave.moveReview', '.analyse__move-review-text', () => {
      hideMoveReviewPreview();
    })
    .on('mouseenter.moveReview', '.analyse__move-review-text .pv-line', function (this: HTMLElement) {
      const fen = $(this).data('fen');
      const color = $(this).data('color') || 'white';
      const lastmove = $(this).data('lastmove');
      if (fen) pubsub.emit('analysis.move-review.hover' as any, { fen, color, lastmove });
    })
    .on('mouseleave.moveReview', '.analyse__move-review-text .pv-line', () => {
      pubsub.emit('analysis.move-review.hover' as any, null);
    })
    .on('click.moveReview', '.analyse__move-review-text .move-chip', function (this: HTMLElement) {
      if (suppressNextTap) return;
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.move-review.move' as any, { uci, san });
    })
    .on('keydown.moveReview', '.analyse__move-review-text .move-chip--interactive', function (this: HTMLElement, e) {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      e.preventDefault();
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.move-review.move' as any, { uci, san });
    })
    .on('keydown.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement, e) {
      if (e.key !== 'Escape') return;
      e.preventDefault();
      hideMoveReviewPreview();
      this.blur();
    })
    .on('mouseenter.moveReview', '.analyse__move-review-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, false);
    })
    .on('click.moveReview', '.analyse__move-review-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, true);
    })
    .on('touchstart.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      if (!('ontouchstart' in window)) return;
      const board = boardPayloadFromElement(this);
      if (!board) return;
      cancelTouchHold();
      touchHoldTriggered = false;
      touchHoldTimer = setTimeout(() => {
        updateMoveReviewPreview(board);
        touchHoldTriggered = true;
        suppressNextTap = true;
        setTimeout(() => {
          suppressNextTap = false;
        }, 400);
      }, 280);
    })
    .on('touchend.moveReview touchcancel.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', () => {
      cancelTouchHold();
      if (!touchHoldTriggered) return;
      touchHoldTriggered = false;
      setTimeout(() => hideMoveReviewPreview(), 900);
    })
    .on('touchstart.moveReview', '.analyse__move-review-text .variation-list[data-variation-swipe="true"]', function (this: HTMLElement, e) {
      const t = (e.originalEvent as TouchEvent).touches?.[0];
      if (!t) return;
      swipeStartX = t.clientX;
      swipeStartY = t.clientY;
      swipeActiveList = this;
    })
    .on('touchend.moveReview', '.analyse__move-review-text .variation-list[data-variation-swipe="true"]', function (this: HTMLElement, e) {
      if (!swipeActiveList) return;
      const t = (e.originalEvent as TouchEvent).changedTouches?.[0];
      if (!t) {
        swipeActiveList = null;
        return;
      }
      const dx = t.clientX - swipeStartX;
      const dy = t.clientY - swipeStartY;
      if (Math.abs(dx) >= 42 && Math.abs(dx) > Math.abs(dy)) {
        stepVariation(swipeActiveList, dx < 0 ? 1 : -1);
      }
      swipeActiveList = null;
    })
    .on('click.moveReview', '.analyse__move-review-text .move-review-score-toggle', e => {
      e.preventDefault();
      onEvalToggle();
    })
    .on('mouseenter.moveReview focusin.moveReview', '.analyse__move-review-text [data-move-review-square]', function (this: HTMLElement) {
      const square = $(this).data('moveReviewSquare');
      if (isBoardSquare(square) && currentCtrl) {
        currentCtrl.moveReviewHoverSquare = square;
        currentCtrl.setAutoShapes();
      }
    })
    .on('mouseleave.moveReview focusout.moveReview', '.analyse__move-review-text [data-move-review-square]', () => {
      if (currentCtrl) {
        currentCtrl.moveReviewHoverSquare = null;
        currentCtrl.setAutoShapes();
      }
    });
}

export function mountMoveReviewPreview(root: HTMLElement): void {
  try {
    moveReviewPreview.cg?.destroy?.();
  } catch {}
  moveReviewPreview = {};

  const container = root.querySelector('.move-review-pv-preview') as HTMLElement | null;
  if (!container) return;

  container.classList.remove('is-active');
  container.innerHTML = '<div class="pv-board"><div class="pv-board-square"><div class="cg-wrap is2d"></div></div></div>';

  const wrap = container.querySelector('.cg-wrap') as HTMLElement | null;
  if (!wrap) return;

  moveReviewPreview.container = container;
  moveReviewPreview.cg = makeChessground(wrap, {
    fen: 'start',
    orientation: moveReviewPreviewOrientation,
    coordinates: false,
    viewOnly: true,
    drawable: { enabled: false, visible: false },
  });

  const firstVariation = root.querySelector('.variation-list .variation-item') as HTMLElement | null;
  if (firstVariation) setActiveVariationItem(firstVariation, false);
}

function updateMoveReviewPreview(board: string): void {
  const container = moveReviewPreview.container;
  const cg = moveReviewPreview.cg;
  if (!container || !cg) return;

  const parts = board.split('|');
  if (parts.length < 2) return;
  const fen = parts[0];
  const uci = parts[1] as Uci;
  if (!fen || !uci) return;

  container.classList.add('is-active');
  cg.set({
    fen,
    lastMove: uciToMove(uci),
    orientation: moveReviewPreviewOrientation,
    coordinates: false,
    viewOnly: true,
    drawable: { enabled: false, visible: false },
  });
}

export function hideMoveReviewPreview(): void {
  moveReviewPreview.container?.classList.remove('is-active');
}
