import type { Outcome } from 'chessops/types';
import { hl, type VNode, bind, type MaybeVNodes } from 'lib/view';
import type { PracticeCtrl, Comment } from './practiceCtrl';
import type AnalyseCtrl from '../ctrl';

import type { Prop } from 'lib';

function commentBest(c: Comment, ctrl: PracticeCtrl): MaybeVNodes {
  return c.best
    ? [
      c.verdict === 'goodMove' ? 'Another was ' : 'Best was ',
      hl(
        'move',
        {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLElement;
              el.addEventListener('click', ctrl.playCommentBest);
              el.addEventListener('mouseover', () => ctrl.commentShape(true));
              el.addEventListener('mouseout', () => ctrl.commentShape(false));
            },
            destroy: () => ctrl.commentShape(false),
          },
        },
        c.best.san,
      ),
    ]
    : [];
}

function renderOffTrack(ctrl: PracticeCtrl): VNode {
  return hl('div.player.off', [
    hl('div.icon.off', '!'),
    hl('div.instruction', [
      hl('strong', 'You browsed away'),
      hl('div.choices', [
        hl('a', { hook: bind('click', ctrl.resume, ctrl.redraw) }, 'Resume practice'),
      ]),
    ]),
  ]);
}

function renderEnd(root: AnalyseCtrl, end: Outcome): VNode {
  const color = end.winner || root.turnColor();
  const isFiftyMoves = root.practice?.currentNode().fen.split(' ')[4] === '100';
  return hl('div.player', [
    color ? hl('div.no-square', hl('piece.king.' + color)) : hl('div.icon.off', '!'),
    hl('div.instruction', [
      hl('strong', end.winner ? 'Checkmate' : 'Draw'),
      end.winner
        ? hl('em', hl('color', end.winner === 'white' ? 'White wins game' : 'Black wins game'))
        : isFiftyMoves
          ? 'Draw by fifty moves'
          : hl('em', 'The game is a draw'),
    ]),
  ]);
}

function renderRunning(root: AnalyseCtrl, ctrl: PracticeCtrl): VNode {
  const hint = ctrl.hinting();
  return hl('div.player.running', [
    hl('div.no-square', hl('piece.king.' + root.turnColor())),
    hl(
      'div.instruction',
      (ctrl.isMyTurn()
        ? [hl('strong', 'Your turn')]
        : [hl('strong', 'Computer thinking')]
      ).concat(
        hl('div.choices', [
          ctrl.isMyTurn()
            ? hl(
              'a',
              { hook: bind('click', () => root.practice!.hint(), ctrl.redraw) },
              hint
                ? hint.mode === 'piece'
                  ? 'See best move'
                  : 'Hide best move'
                : 'Get a hint',
            )
            : '',
        ]),
      ),
    ),
  ]);
}

export function renderCustomPearl({ ceval }: AnalyseCtrl, hardMode: boolean): VNode {
  if (hardMode) {
    const time = `${!isFinite(ceval.storedMovetime()) ? 60 : Math.round(ceval.storedMovetime() / 1000)} seconds`;
    return hl('div.practice-mode', [hl('p', 'Mastery'), hl('p.secondary', time)]);
  }
  return hl('div.practice-mode', [hl('p', 'Casual'), hl('p.secondary', 'depth 18')]);
}

export function renderCustomStatus({ ceval }: AnalyseCtrl, hardMode: Prop<boolean>): VNode | undefined {
  return ceval.isComputing
    ? undefined
    : hl(
      'button.status.button-link',
      { hook: bind('click', () => hardMode(!hardMode())) },
      'Toggle difficulty',
    );
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.practice;
  if (!ctrl) return;
  const comment: Comment | null = ctrl.comment();
  const isFiftyMoves = ctrl.currentNode().fen.split(' ')[4] === '100';
  const running: boolean = ctrl.running();
  const end = ctrl.currentNode().threefold || isFiftyMoves ? { winner: undefined } : root.outcome();
  return hl('div.practice-box.training-box.sub-box.' + (comment ? comment.verdict : 'no-verdict'), [
    hl('div.title', 'Practice with computer'),
    hl(
      'div.feedback',
      end ? renderEnd(root, end) : running ? renderRunning(root, ctrl) : renderOffTrack(ctrl),
    ),
    running
      ? hl(
        'div.comment',
        (comment
          ? (
            [
              hl(
                'span.verdict',
                comment.verdict === 'goodMove'
                  ? 'Good move'
                  : comment.verdict.charAt(0).toUpperCase() + comment.verdict.slice(1),
              ),
              ' ',
            ] as MaybeVNodes
          ).concat(commentBest(comment, ctrl))
          : [ctrl.isMyTurn() || end ? '' : hl('span.wait', 'Evaluating your move')]),
      )
      : null,
  ]);
}
