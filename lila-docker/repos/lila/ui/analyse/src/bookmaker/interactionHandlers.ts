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

export function setBookmakerPreviewOrientation(orientation: Color): void {
  bookmakerPreviewOrientation = orientation;
}

export function initBookmakerHandlers(onEvalToggle: () => void): void {
  if (handlersBound) return;
  handlersBound = true;

  $(document)
    .on('mouseover.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
      const board = this.dataset.board;
      if (board) updateBookmakerPreview(board);
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
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.bookmaker.move', { uci, san });
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
