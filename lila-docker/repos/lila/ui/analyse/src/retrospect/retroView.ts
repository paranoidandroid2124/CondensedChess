import { renderIndexAndMove } from '../view/components';
import type { RetroCtrl } from './retroCtrl';
import type AnalyseCtrl from '../ctrl';
import * as licon from 'lib/licon';
import { bind, dataIcon, hl, type VNode, spinnerVdom as spinner } from 'lib/view';

function skipOrViewSolution(ctrl: RetroCtrl) {
  return hl('div.choices', [
    hl('a', { hook: bind('click', ctrl.viewSolution, ctrl.redraw) }, 'View the solution'),
    hl('a', { hook: bind('click', ctrl.skip) }, 'Skip this move'),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return hl('a.half.continue', { hook: bind('click', ctrl.jumpToNext) }, [
    hl('i', { attrs: dataIcon(licon.PlayTriangle) }),
    'Next',
  ]);
}

const minDepth = 8;
const maxDepth = 18;

function renderEvalProgress(node: Tree.Node): VNode {
  return hl(
    'div.progress',
    hl('div', {
      attrs: {
        style: `width: ${node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%' : 0
          }`,
      },
    }),
  );
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.no-square', hl('piece.king.' + ctrl.color)),
        hl('div.instruction', [
          hl('strong', [
            renderIndexAndMove(ctrl.current()!.fault.node, false, true),
            ' was played',
          ]),
          hl(
            'em',
            ctrl.color === 'white' ? 'Find a better move for White' : 'Find a better move for Black',
          ),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  // user has browsed away from the move to solve
  offTrack(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.icon.off', '!'),
        hl('div.instruction', [
          hl('strong', 'You browsed away'),
          hl('div.choices.off', [
            hl('a', { hook: bind('click', ctrl.jumpToNext) }, 'Resume learning'),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.icon', '✗'),
        hl('div.instruction', [
          hl('strong', 'You can do better'),
          hl('em', ctrl.color === 'white' ? 'Try another move for White' : 'Try another move for Black'),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  win(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player', [hl('div.icon', '✓'), hl('div.instruction', hl('strong', 'Good move'))]),
      ),
      jumpToNext(ctrl),
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player', [
          hl('div.icon', '✓'),
          hl('div.instruction', [
            hl('strong', 'Solution'),
            hl('em', ['Best was ', renderIndexAndMove(ctrl.current()!.solution.node, false, false)]),
          ]),
        ]),
      ),
      jumpToNext(ctrl),
    ];
  },
  eval(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player.center', [
          hl('div.instruction', [
            hl('strong', 'Evaluating your move'),
            renderEvalProgress(ctrl.node()),
          ]),
        ]),
      ),
    ];
  },
  end(ctrl: RetroCtrl, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis())
      return [
        hl(
          'div.half.top',
          hl('div.player', [hl('div.icon', spinner()), hl('div.instruction', 'Waiting for analysis')]),
        ),
      ];
    const nothing = !ctrl.completion()[1];
    return [
      hl('div.player', [
        hl('div.no-square', hl('piece.king.' + ctrl.color)),
        hl('div.instruction', [
          hl(
            'em',
            nothing
              ? ctrl.color === 'white'
                ? 'No mistakes found for White'
                : 'No mistakes found for Black'
              : ctrl.color === 'white'
                ? 'Done reviewing White mistakes'
                : 'Done reviewing Black mistakes',
          ),
          hl('div.choices.end', [
            !nothing &&
            hl(
              'a',
              {
                key: 'reset',
                hook: bind('click', ctrl.reset),
              },
              'Do it again',
            ),
            hl(
              'a',
              {
                key: 'flip',
                hook: bind('click', ctrl.flip),
              },
              ctrl.color === 'white' ? 'Review Black mistakes' : 'Review White mistakes',
            ),
          ]),
        ]),
      ]),
    ];
  },
};

function renderFeedback(root: AnalyseCtrl, fb: Exclude<keyof typeof feedback, 'end'>) {
  const ctrl: RetroCtrl = root.retro!;
  const current = ctrl.current();
  if (ctrl.isSolving() && current && root.path !== current.prev.path) return feedback.offTrack(ctrl);
  if (fb === 'find') return current ? feedback.find(ctrl) : feedback.end(ctrl, root.hasFullComputerAnalysis);
  return feedback[fb](ctrl);
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.retro;
  if (!ctrl) return;
  const fb = ctrl.feedback(),
    completion = ctrl.completion();
  return hl('div.retro-box.training-box.sub-box', [
    hl('div.title', [
      hl('span', 'Learn from your mistakes'),
      hl('span', `${Math.min(completion[0] + 1, completion[1])} / ${completion[1]}`),
      hl('button.fbt', {
        hook: bind('click', root.toggleRetro, root.redraw),
        attrs: { 'data-icon': licon.X, 'aria-label': 'Close learn window' },
      }),
    ]),
    hl('div.feedback.' + fb, renderFeedback(root, fb)),
  ]);
}
