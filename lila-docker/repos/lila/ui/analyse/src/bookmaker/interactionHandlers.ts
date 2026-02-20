import { pubsub } from 'lib/pubsub';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';

type BookmakerPreviewState = {
  cg?: CgApi;
  container?: HTMLElement;
};

let handlersBound = false;
let bookmakerPreview: BookmakerPreviewState = {};
let bookmakerPreviewOrientation: Color = 'white';
let touchHoldTimer: ReturnType<typeof setTimeout> | undefined;
let touchHoldTriggered = false;
let suppressNextTap = false;
let swipeStartX = 0;
let swipeStartY = 0;
let swipeActiveList: HTMLElement | null = null;

export function setBookmakerPreviewOrientation(orientation: Color): void {
  bookmakerPreviewOrientation = orientation;
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
  const board = item.querySelector<HTMLElement>('[data-board]')?.dataset.board;
  if (board) updateBookmakerPreview(board);
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

export function initBookmakerHandlers(onEvalToggle: () => void): void {
  if (handlersBound) return;
  handlersBound = true;

  $(document)
    .on('mouseover.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
      const board = this.dataset.board;
      if (board) updateBookmakerPreview(board);
    })
    .on('focusin.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
      const board = this.dataset.board;
      if (board) updateBookmakerPreview(board);
    })
    .on('focusout.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
      const root = this.closest('.analyse__bookmaker-text') as HTMLElement | null;
      setTimeout(() => {
        if (root?.contains(document.activeElement)) return;
        hideBookmakerPreview();
      }, 0);
    })
    .on('mouseleave.bookmaker', '.analyse__bookmaker-text', () => {
      hideBookmakerPreview();
    })
    .on('mouseenter.bookmaker', '.analyse__bookmaker-text .pv-line', function (this: HTMLElement) {
      const fen = $(this).data('fen');
      const color = $(this).data('color') || 'white';
      const lastmove = $(this).data('lastmove');
      if (fen) pubsub.emit('analysis.bookmaker.hover', { fen, color, lastmove });
    })
    .on('mouseleave.bookmaker', '.analyse__bookmaker-text .pv-line', () => {
      pubsub.emit('analysis.bookmaker.hover', null);
    })
    .on('click.bookmaker', '.analyse__bookmaker-text .move-chip', function (this: HTMLElement) {
      if (suppressNextTap) return;
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.bookmaker.move', { uci, san });
    })
    .on('keydown.bookmaker', '.analyse__bookmaker-text .move-chip--interactive', function (this: HTMLElement, e) {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      e.preventDefault();
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.bookmaker.move', { uci, san });
    })
    .on('keydown.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement, e) {
      if (e.key !== 'Escape') return;
      e.preventDefault();
      hideBookmakerPreview();
      this.blur();
    })
    .on('mouseenter.bookmaker', '.analyse__bookmaker-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, false);
    })
    .on('click.bookmaker', '.analyse__bookmaker-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, true);
    })
    .on('touchstart.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
      if (!('ontouchstart' in window)) return;
      const board = this.dataset.board;
      if (!board) return;
      cancelTouchHold();
      touchHoldTriggered = false;
      touchHoldTimer = setTimeout(() => {
        updateBookmakerPreview(board);
        touchHoldTriggered = true;
        suppressNextTap = true;
        setTimeout(() => {
          suppressNextTap = false;
        }, 400);
      }, 280);
    })
    .on('touchend.bookmaker touchcancel.bookmaker', '.analyse__bookmaker-text [data-board]', () => {
      cancelTouchHold();
      if (!touchHoldTriggered) return;
      touchHoldTriggered = false;
      setTimeout(() => hideBookmakerPreview(), 900);
    })
    .on('touchstart.bookmaker', '.analyse__bookmaker-text .variation-list[data-variation-swipe="true"]', function (this: HTMLElement, e) {
      const t = (e.originalEvent as TouchEvent).touches?.[0];
      if (!t) return;
      swipeStartX = t.clientX;
      swipeStartY = t.clientY;
      swipeActiveList = this;
    })
    .on('touchend.bookmaker', '.analyse__bookmaker-text .variation-list[data-variation-swipe="true"]', function (this: HTMLElement, e) {
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
    .on('click.bookmaker', '.analyse__bookmaker-text .bookmaker-score-toggle', e => {
      e.preventDefault();
      onEvalToggle();
    });
}

export function mountBookmakerPreview(root: HTMLElement): void {
  try {
    bookmakerPreview.cg?.destroy?.();
  } catch {}
  bookmakerPreview = {};

  const container = root.querySelector('.bookmaker-pv-preview') as HTMLElement | null;
  if (!container) return;

  container.classList.remove('is-active');
  container.innerHTML = '<div class="pv-board"><div class="pv-board-square"><div class="cg-wrap is2d"></div></div></div>';

  const wrap = container.querySelector('.cg-wrap') as HTMLElement | null;
  if (!wrap) return;

  bookmakerPreview.container = container;
  bookmakerPreview.cg = makeChessground(wrap, {
    fen: 'start',
    orientation: bookmakerPreviewOrientation,
    coordinates: false,
    viewOnly: true,
    drawable: { enabled: false, visible: false },
  });

  const firstVariation = root.querySelector('.variation-list .variation-item') as HTMLElement | null;
  if (firstVariation) setActiveVariationItem(firstVariation, false);
}

function updateBookmakerPreview(board: string): void {
  const container = bookmakerPreview.container;
  const cg = bookmakerPreview.cg;
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
    orientation: bookmakerPreviewOrientation,
    coordinates: false,
    viewOnly: true,
    drawable: { enabled: false, visible: false },
  });
}

export function hideBookmakerPreview(): void {
  bookmakerPreview.container?.classList.remove('is-active');
}
