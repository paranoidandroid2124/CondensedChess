import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { moveArrowAttributes, winnerOf } from './explorerUtil';
import type { TablebaseMoveStats } from './interfaces';

export function showTablebase(
  ctrl: AnalyseCtrl,
  fen: FEN,
  title: string,
  tooltip: string | undefined,
  moves: TablebaseMoveStats[],
): VNode[] {
  if (!moves.length) return [];
  return [
    h('div.title', tooltip ? { attrs: { title: tooltip } } : {}, title),
    h('table.tablebase', [
      h(
        'tbody',
        moveArrowAttributes(ctrl, { fen, onClick: (_, uci) => uci && ctrl.explorerMove(uci) }),
        moves.map(move =>
          h('tr', { key: move.uci, attrs: { 'data-uci': move.uci } }, [
            h('td', move.san),
            h('td', [showDtz(fen, move), showDtc(fen, move), showDtm(fen, move), showDtw(fen, move)]),
          ]),
        ),
      ),
    ]),
  ];
}

function showDtm(fen: FEN, move: TablebaseMoveStats) {
  return move.dtm
    ? h(
      'result.' + winnerOf(fen, move),
      {
        attrs: { title: `Mate in ${Math.abs(move.dtm)} half-moves (Depth To Mate)` },
      },
      'DTM ' + Math.abs(move.dtm),
    )
    : undefined;
}

function showDtw(fen: FEN, move: TablebaseMoveStats) {
  return move.dtw
    ? h(
      'result.' + winnerOf(fen, move),
      { attrs: { title: 'Half-moves to win (Depth To Win)' } },
      'DTW ' + Math.abs(move.dtw),
    )
    : undefined;
}

function showDtc(fen: FEN, move: TablebaseMoveStats) {
  return move.dtc
    ? h(
      'result.' + winnerOf(fen, move),
      { attrs: { title: 'Moves to capture, promotion, or checkmate (Depth To Conversion, experimental)' } },
      'DTC ' + Math.abs(move.dtc),
    )
    : undefined;
}

function showDtz(fen: FEN, move: TablebaseMoveStats): VNode | undefined {
  if (move.checkmate) return h('result.' + winnerOf(fen, move), 'Checkmate');
  if (move.variant_win) return h('result.' + winnerOf(fen, move), 'Variant loss');
  if (move.variant_loss) return h('result.' + winnerOf(fen, move), 'Variant win');
  if (move.stalemate) return h('result.draws', 'Stalemate');
  if (move.insufficient_material) return h('result.draws', 'Insufficient material');
  if (move.dtz === 0 || move.dtc === 0) return h('result.draws', 'Draw');
  if ((move.dtz || move.dtc) && move.zeroing)
    return move.san.includes('x')
      ? h('result.' + winnerOf(fen, move), 'Capture')
      : h('result.' + winnerOf(fen, move), 'Pawn move');
  return move.dtz
    ? h(
      'result.' + winnerOf(fen, move),
      {
        attrs: {
          title: 'Distance to zeroing, possibly rounded (Distance To Zeroing)',
        },
      },
      'DTZ ' + Math.abs(move.dtz),
    )
    : undefined;
}
