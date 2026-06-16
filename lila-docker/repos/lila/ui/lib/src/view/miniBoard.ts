/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';

const initMiniBoard = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initMiniBoardWith(node, { fen, orientation: orientation as Color, lastMove: uciToMove(lm) });
};

const initMiniBoardWith = (node: HTMLElement, config: CgConfig): void => {
  const cgConfig = {
    coordinates: false,
    viewOnly: !node.getAttribute('data-playable'),
    drawable: { enabled: false, visible: false },
    ...config,
  };
  (node as unknown as Record<string, unknown>)['chesstory-chessground'] = makeChessground(node, cgConfig);
};

export const initMiniBoards = (parent?: HTMLElement): void =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach((el: HTMLElement) => {
    el.classList.remove('mini-board--init');
    initMiniBoard(el);
  });
