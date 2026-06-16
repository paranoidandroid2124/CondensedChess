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
let swipeActivePlayer: HTMLElement | null = null;
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

function moveReviewScenePanels(player: HTMLElement): HTMLElement[] {
  return Array.from(player.querySelectorAll<HTMLElement>('[data-move-review-scene-panel]'));
}

function moveReviewSceneIndex(player: HTMLElement): number {
  const raw = Number(player.dataset.sceneIndex);
  return Number.isFinite(raw) ? raw : 0;
}

function setSceneSquare(square: string | null): void {
  if (!currentCtrl) return;
  currentCtrl.moveReviewHoverSquare = isBoardSquare(square) ? square : null;
  currentCtrl.setAutoShapes();
}

function sceneSquareFromPanel(panel: HTMLElement | null | undefined): string | null {
  return panel?.dataset.sceneSquare || panel?.querySelector<HTMLElement>('[data-move-review-square]')?.dataset.moveReviewSquare || null;
}

function currentScenePanel(player: HTMLElement): HTMLElement | null {
  return moveReviewScenePanels(player)[moveReviewSceneIndex(player)] || null;
}

function sceneBoardKicker(panel: HTMLElement | null | undefined): string | null {
  const kicker = panel?.dataset.sceneBoardKicker;
  return kicker ? `Board shows · ${kicker}` : null;
}

function setPlayerBoardMeta(
  player: HTMLElement,
  title: string | null | undefined,
  subtitle: string | null | undefined,
  kicker: string | null | undefined,
  note: string | null | undefined,
): void {
  const kickerEl = player.querySelector<HTMLElement>('.move-review-player__board-kicker');
  const titleEl = player.querySelector<HTMLElement>('.move-review-player__board-title');
  const subtitleEl = player.querySelector<HTMLElement>('.move-review-player__board-subtitle');
  const noteEl = player.querySelector<HTMLElement>('.move-review-player__board-note');
  if (kickerEl) kickerEl.textContent = kicker || 'Board shows this scene';
  if (titleEl && title) titleEl.textContent = title;
  if (subtitleEl) subtitleEl.textContent = subtitle || '';
  if (noteEl) noteEl.textContent = note || 'Keep the board tied to this coaching scene.';
}

function setCueItem(player: HTMLElement, selector: string, value: string | null | undefined): void {
  const item = player.querySelector<HTMLElement>(selector);
  if (!item) return;
  const text = value?.trim() || '';
  item.hidden = !text;
  const valueEl = item.querySelector<HTMLElement>('strong');
  if (valueEl) valueEl.textContent = text;
}

function updateBoardCueVisibility(player: HTMLElement): void {
  const cue = player.querySelector<HTMLElement>('.move-review-player__board-cue');
  if (!cue) return;
  const hasVisibleCue = Array.from(cue.querySelectorAll<HTMLElement>('.move-review-player__board-cue-item')).some(
    item => !item.hidden,
  );
  cue.hidden = !hasVisibleCue;
}

function setPlayerBoardCue(player: HTMLElement, panel: HTMLElement): void {
  const cue = player.querySelector<HTMLElement>('.move-review-player__board-cue');
  if (!cue) return;
  const square = sceneSquareFromPanel(panel);
  const lineEl = panel.querySelector<HTMLElement>('[data-scene-line]');
  const line = lineEl?.dataset.sceneLineCue || lineEl?.dataset.sceneLine || '';
  setCueItem(player, '.move-review-player__board-cue-item--square', square);
  setCueItem(player, '.move-review-player__board-cue-item--line', line);
  updateBoardCueVisibility(player);
}

function setPlayerBoardMoveCue(player: HTMLElement, move: string | null | undefined): void {
  setCueItem(player, '.move-review-player__board-cue-item--move', move);
  updateBoardCueVisibility(player);
}

function revealMoveReviewScene(player: HTMLElement, panel: HTMLElement): void {
  const scroller = player.closest('.analyse__move-review-text') as HTMLElement | null;
  if (!scroller) return;
  const timeline = player.querySelector<HTMLElement>('.move-review-player__timeline');
  const scrollerRect = scroller.getBoundingClientRect();
  const panelRect = panel.getBoundingClientRect();
  const stickyOffset = (timeline?.offsetHeight || 0) + 8;
  const top = scroller.scrollTop + panelRect.top - scrollerRect.top - stickyOffset;
  scroller.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
}

function moveLabelFromElement(el: HTMLElement): string | null {
  return el.dataset.san || el.textContent?.trim() || null;
}

function setActiveCoachMove(player: HTMLElement, activeMove: HTMLElement | null): void {
  player.querySelectorAll<HTMLElement>('.move-review-coach__move-chip.is-active').forEach(el => {
    if (el !== activeMove) el.classList.remove('is-active');
  });
  activeMove?.classList.add('is-active');
}

function sceneLineChips(panel: HTMLElement): HTMLElement[] {
  return Array.from(
    panel.querySelectorAll<HTMLElement>('.move-review-player__scene-line [data-ref-id], .move-review-player__scene-line [data-board]'),
  );
}

function setSceneLineState(panel: HTMLElement, activeMove: HTMLElement | null): void {
  const line = panel.querySelector<HTMLElement>('.move-review-player__scene-line');
  if (!line) return;
  const chips = sceneLineChips(panel);
  const activeIndex = activeMove ? chips.indexOf(activeMove) : chips.findIndex(chip => chip.classList.contains('is-active'));
  const selectedIndex = activeIndex >= 0 ? activeIndex : 0;
  line.dataset.lineIndex = String(selectedIndex);

  const count = line.querySelector<HTMLElement>('.move-review-player__line-count');
  if (count) count.textContent = chips.length ? `Move ${selectedIndex + 1}/${chips.length}` : 'Line moves';
  const progress = chips.length ? ((selectedIndex + 1) / chips.length) * 100 : 0;
  line.style.setProperty('--move-review-line-progress', `${progress}%`);
  line.classList.toggle('has-active-board', !!chips.length);

  line.querySelectorAll<HTMLButtonElement>('[data-move-review-line-step]').forEach(button => {
    const step = Number(button.dataset.moveReviewLineStep);
    button.disabled = !chips.length || (step < 0 && selectedIndex <= 0) || (step > 0 && selectedIndex >= chips.length - 1);
  });
}

function stepSceneLine(button: HTMLButtonElement): void {
  const panel = button.closest('[data-move-review-scene-panel]') as HTMLElement | null;
  if (!panel) return;
  const chips = sceneLineChips(panel);
  if (!chips.length) return;
  const rawIndex = Number(panel.querySelector<HTMLElement>('.move-review-player__scene-line')?.dataset.lineIndex || 0);
  const current = Math.max(0, Math.min(chips.length - 1, Number.isFinite(rawIndex) ? rawIndex : 0));
  const next = Math.max(0, Math.min(chips.length - 1, current + Number(button.dataset.moveReviewLineStep)));
  const nextMove = chips[next];
  syncMoveReviewElementBoard(nextMove, true);
  nextMove.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
}

function restoreMoveReviewPlayerBoard(anchor?: HTMLElement | null): boolean {
  const player =
    (anchor?.closest('[data-move-review-player]') as HTMLElement | null) ||
    (anchor?.querySelector?.('[data-move-review-player]') as HTMLElement | null) ||
    (moveReviewPreview.container?.closest('[data-move-review-player]') as HTMLElement | null);
  const panel = player ? currentScenePanel(player) : null;
  if (!player || !panel) return false;
  syncMoveReviewSceneBoard(player, panel);
  return true;
}

function syncMoveReviewElementBoard(el: HTMLElement, markActive = false): void {
  const board = boardPayloadFromElement(el);
  if (board) updateMoveReviewPreview(board);

  const player = el.closest('[data-move-review-player]') as HTMLElement | null;
  if (!player) return;

  const panel = currentScenePanel(player);
  if (markActive && el.classList.contains('move-review-coach__move-chip')) setActiveCoachMove(player, el);
  if (panel && markActive && el.closest('.move-review-player__scene-line')) setSceneLineState(panel, el);
  setPlayerBoardMeta(
    player,
    panel?.dataset.sceneBoardTitle || null,
    moveLabelFromElement(el) || panel?.dataset.sceneBoardSubtitle || null,
    sceneBoardKicker(panel),
    panel?.dataset.sceneBoardNote || null,
  );
  setPlayerBoardMoveCue(player, moveLabelFromElement(el));
}

function syncMoveReviewSceneBoard(player: HTMLElement, panel: HTMLElement): void {
  setPlayerBoardCue(player, panel);
  const firstMove = panel.querySelector<HTMLElement>('[data-ref-id], [data-board]');
  const board = panel.dataset.sceneBoard || (firstMove ? boardPayloadFromElement(firstMove) : null);
  if (board) {
    updateMoveReviewPreview(board);
    const activeMove = Array.from(panel.querySelectorAll<HTMLElement>('[data-ref-id], [data-board]')).find(
      el => boardPayloadFromElement(el) === board,
    );
    setActiveCoachMove(player, activeMove || null);
    setSceneLineState(panel, activeMove || null);
    setPlayerBoardMeta(
      player,
      panel.dataset.sceneBoardTitle || null,
      (activeMove ? moveLabelFromElement(activeMove) : null) || panel.dataset.sceneBoardSubtitle || null,
      sceneBoardKicker(panel),
      panel.dataset.sceneBoardNote || null,
    );
    setPlayerBoardMoveCue(player, activeMove ? moveLabelFromElement(activeMove) : null);
  } else {
    setActiveCoachMove(player, null);
    setSceneLineState(panel, null);
    setPlayerBoardMeta(
      player,
      panel.dataset.sceneBoardTitle || null,
      panel.dataset.sceneBoardSubtitle || null,
      sceneBoardKicker(panel),
      panel.dataset.sceneBoardNote || null,
    );
    setPlayerBoardMoveCue(player, null);
    hideMoveReviewPreview({ force: true });
  }

  setSceneSquare(sceneSquareFromPanel(panel));
}

function sceneControlLabel(panel: HTMLElement | null | undefined): string {
  return panel?.dataset.sceneControlLabel || panel?.dataset.sceneShortLabel || panel?.dataset.sceneLabel || 'scene';
}

function updateMoveReviewSceneControls(player: HTMLElement, panels: HTMLElement[], index: number): void {
  player.querySelectorAll<HTMLButtonElement>('[data-move-review-scene-step]').forEach(button => {
    const step = Number(button.dataset.moveReviewSceneStep);
    const target = Math.max(0, Math.min(panels.length - 1, index + step));
    const disabled = (step < 0 && index === 0) || (step > 0 && index === panels.length - 1);
    button.disabled = disabled;
    button.textContent =
      step < 0
        ? disabled
          ? 'Back'
          : `Back: ${sceneControlLabel(panels[target])}`
        : disabled
          ? 'Next'
          : `Next: ${sceneControlLabel(panels[target])}`;
  });

  const counter = player.querySelector<HTMLElement>('.move-review-player__scene-count');
  if (counter) counter.textContent = `${panels[index]?.dataset.sceneLabel || 'Scene'} · ${index + 1}/${panels.length}`;
}

function activateMoveReviewScene(player: HTMLElement, targetIndex: number, syncBoard = true, reveal = false): void {
  const panels = moveReviewScenePanels(player);
  if (!panels.length) return;
  const previousIndex = moveReviewSceneIndex(player);
  const nextIndex = Math.max(0, Math.min(panels.length - 1, targetIndex));
  player.dataset.sceneIndex = String(nextIndex);
  player.dataset.sceneDirection = nextIndex === previousIndex ? 'still' : nextIndex > previousIndex ? 'forward' : 'back';
  player.style.setProperty('--move-review-scene-progress', `${((nextIndex + 1) / panels.length) * 100}%`);

  panels.forEach((panel, idx) => {
    const active = idx === nextIndex;
    panel.hidden = !active;
    panel.classList.toggle('is-active', active);
    panel.setAttribute('aria-hidden', active ? 'false' : 'true');
  });

  player.querySelectorAll<HTMLButtonElement>('[data-move-review-scene]').forEach(button => {
    const active = Number(button.dataset.moveReviewScene) === nextIndex;
    button.classList.toggle('is-active', active);
    button.setAttribute('aria-selected', active ? 'true' : 'false');
    button.tabIndex = active ? 0 : -1;
  });

  updateMoveReviewSceneControls(player, panels, nextIndex);
  if (syncBoard) syncMoveReviewSceneBoard(player, panels[nextIndex]);
  if (reveal) revealMoveReviewScene(player, panels[nextIndex]);
}

function hydrateMoveReviewPlayers(root: HTMLElement): void {
  root.querySelectorAll<HTMLElement>('[data-move-review-player]').forEach(player => {
    activateMoveReviewScene(player, moveReviewSceneIndex(player), true);
  });
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
      syncMoveReviewElementBoard(this, true);
    })
    .on('focusin.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      syncMoveReviewElementBoard(this, true);
    })
    .on('focusout.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement) {
      const root = this.closest('.analyse__move-review-text') as HTMLElement | null;
      setTimeout(() => {
        if (root?.contains(document.activeElement)) return;
        if (!restoreMoveReviewPlayerBoard(this)) hideMoveReviewPreview();
      }, 0);
    })
    .on('mouseleave.moveReview', '.analyse__move-review-text', function (this: HTMLElement) {
      if (!restoreMoveReviewPlayerBoard(this)) hideMoveReviewPreview();
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
      syncMoveReviewElementBoard(this, true);
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.move-review.move' as any, { uci, san });
    })
    .on('keydown.moveReview', '.analyse__move-review-text .move-chip--interactive', function (this: HTMLElement, e) {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      e.preventDefault();
      syncMoveReviewElementBoard(this, true);
      const uci = $(this).data('uci');
      const san = $(this).data('san');
      if (uci) pubsub.emit('analysis.move-review.move' as any, { uci, san });
    })
    .on('keydown.moveReview', '.analyse__move-review-text [data-ref-id], .analyse__move-review-text [data-board]', function (this: HTMLElement, e) {
      if (e.key !== 'Escape') return;
      e.preventDefault();
      if (!restoreMoveReviewPlayerBoard(this)) hideMoveReviewPreview();
      this.blur();
    })
    .on('mouseenter.moveReview', '.analyse__move-review-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, false);
    })
    .on('click.moveReview', '.analyse__move-review-text .variation-item', function (this: HTMLElement) {
      setActiveVariationItem(this, true);
    })
    .on('click.moveReview', '.analyse__move-review-text [data-move-review-scene]', function (this: HTMLButtonElement, e) {
      e.preventDefault();
      const player = this.closest('[data-move-review-player]') as HTMLElement | null;
      if (!player) return;
      activateMoveReviewScene(player, Number(this.dataset.moveReviewScene), true, true);
    })
    .on('keydown.moveReview', '.analyse__move-review-text [data-move-review-scene]', function (this: HTMLButtonElement, e) {
      const key = e.key;
      if (key !== 'ArrowRight' && key !== 'ArrowLeft' && key !== 'Home' && key !== 'End') return;
      e.preventDefault();
      const player = this.closest('[data-move-review-player]') as HTMLElement | null;
      if (!player) return;
      const panels = moveReviewScenePanels(player);
      if (!panels.length) return;
      const current = moveReviewSceneIndex(player);
      const next =
        key === 'Home' ? 0 : key === 'End' ? panels.length - 1 : current + (key === 'ArrowRight' ? 1 : -1);
      const bounded = Math.max(0, Math.min(panels.length - 1, next));
      activateMoveReviewScene(player, bounded, true, true);
      player.querySelector<HTMLButtonElement>(`[data-move-review-scene="${bounded}"]`)?.focus();
    })
    .on('click.moveReview', '.analyse__move-review-text [data-move-review-scene-step]', function (this: HTMLButtonElement, e) {
      e.preventDefault();
      const player = this.closest('[data-move-review-player]') as HTMLElement | null;
      if (!player) return;
      activateMoveReviewScene(player, moveReviewSceneIndex(player) + Number(this.dataset.moveReviewSceneStep), true, true);
    })
    .on('click.moveReview', '.analyse__move-review-text [data-move-review-line-step]', function (this: HTMLButtonElement, e) {
      e.preventDefault();
      stepSceneLine(this);
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
      setTimeout(() => {
        if (!restoreMoveReviewPlayerBoard()) hideMoveReviewPreview();
      }, 900);
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
    .on('touchstart.moveReview', '.analyse__move-review-text .move-review-player__scene-stack', function (this: HTMLElement, e) {
      const t = (e.originalEvent as TouchEvent).touches?.[0];
      if (!t) return;
      swipeStartX = t.clientX;
      swipeStartY = t.clientY;
      swipeActivePlayer = this.closest('[data-move-review-player]') as HTMLElement | null;
    })
    .on('touchend.moveReview', '.analyse__move-review-text .move-review-player__scene-stack', function (this: HTMLElement, e) {
      if (!swipeActivePlayer) return;
      const t = (e.originalEvent as TouchEvent).changedTouches?.[0];
      const player = swipeActivePlayer;
      swipeActivePlayer = null;
      if (!t) return;
      const dx = t.clientX - swipeStartX;
      const dy = t.clientY - swipeStartY;
      if (Math.abs(dx) >= 42 && Math.abs(dx) > Math.abs(dy)) {
        activateMoveReviewScene(player, moveReviewSceneIndex(player) + (dx < 0 ? 1 : -1), true, true);
      }
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
    .on('mouseleave.moveReview focusout.moveReview', '.analyse__move-review-text [data-move-review-square]', function (this: HTMLElement) {
      const player = this.closest('[data-move-review-player]') as HTMLElement | null;
      setSceneSquare(player ? sceneSquareFromPanel(currentScenePanel(player)) : null);
    });
}

export function mountMoveReviewPreview(root: HTMLElement): void {
  try {
    moveReviewPreview.cg?.destroy?.();
  } catch {}
  moveReviewPreview = {};

  const container = root.querySelector('.move-review-pv-preview') as HTMLElement | null;
  if (!container) {
    hydrateMoveReviewPlayers(root);
    return;
  }

  container.classList.toggle('is-active', !!container.closest('[data-move-review-player]'));
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
  hydrateMoveReviewPlayers(root);
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

export function hideMoveReviewPreview(options: { force?: boolean } = {}): void {
  if (!options.force && moveReviewPreview.container?.closest('[data-move-review-player]')) return;
  moveReviewPreview.container?.classList.remove('is-active');
}
