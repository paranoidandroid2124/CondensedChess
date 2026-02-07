/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { h, type VNode } from 'snabbdom';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';

export type BoardPreview = { fen: FEN; uci: Uci };

export function renderBoardPreview(preview: BoardPreview, orientation: Color, sel = 'div.pv-board'): VNode {
  const cgConfig = {
    fen: preview.fen,
    lastMove: uciToMove(preview.uci),
    orientation,
    coordinates: false,
    viewOnly: true,
    drawable: {
      enabled: false,
      visible: false,
    },
  };

  const cgVNode = h('div.cg-wrap.is2d', {
    hook: {
      insert: (vnode: any) => (vnode.elm._cg = makeChessground(vnode.elm, cgConfig)),
      update: (vnode: any) => vnode.elm._cg?.set(cgConfig),
      destroy: (vnode: any) => vnode.elm._cg?.destroy(),
    },
  });

  return h(sel, [h('div.pv-board-square', [cgVNode])]);
}

