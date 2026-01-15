import { h, thunk, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';

import { getPlayer } from 'lib/game';
import * as licon from 'lib/licon';
import { bind, dataIcon } from 'lib/view';
import { ratingDiff } from 'lib/view/userLink';

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  plural: (nb: number) => (string | VNode)[];
  symbol: string;
}

const renderPlayer = (ctrl: AnalyseCtrl, color: Color): VNode => {
  const p = getPlayer(ctrl.data, color);
  if (p.user)
    return h('a.user-link.ulpt', { attrs: { href: '/@/' + p.user.username } }, [
      p.user.username,
      ' ',
      ratingDiff(p),
    ]);
  return h(
    'span',
    p.name ||
    (p.ai && 'Stockfish level ' + p.ai) ||
    'Anonymous',
  );
};

const advices: Advice[] = [
  {
    kind: 'inaccuracy',
    plural: nb => [h('strong', nb), nb === 1 ? ' Inaccuracy' : ' Inaccuracies'],
    symbol: '?!',
  },
  {
    kind: 'mistake',
    plural: nb => [h('strong', nb), nb === 1 ? ' Mistake' : ' Mistakes'],
    symbol: '?',
  },
  {
    kind: 'blunder',
    plural: nb => [h('strong', nb), nb === 1 ? ' Blunder' : ' Blunders'],
    symbol: '??',
  },
];

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data,
    sideData = d.analysis![color];

  return h('div.advice-summary__side', [
    h('div.advice-summary__player', [h(`i.is.color-icon.${color}`), renderPlayer(ctrl, color)]),
    ...advices.map(a => error(d.analysis![color][a.kind], color, a)),
    h('div.advice-summary__acpl', [
      h('strong', sideData.acpl),
      h('span', ' Average centipawn loss'),
    ]),
    h('div.advice-summary__accuracy', [
      h('strong', [sideData.accuracy, '%']),
      h('span', [
        'Accuracy',
        ' ',
        h('a', {
          attrs: { 'data-icon': licon.InfoCircle, href: '/page/accuracy', target: '_blank' },
        }),
      ]),
    ]),
  ]);
}

const error = (nb: number, color: Color, advice: Advice) =>
  h(
    'div.advice-summary__error' + (nb ? `.symbol.${advice.kind}` : ''),
    { attrs: nb ? { 'data-color': color, 'data-symbol': advice.symbol } : {} },
    advice.plural(nb),
  );

const doRender = (ctrl: AnalyseCtrl): VNode => {
  return h(
    'div.advice-summary',
    {
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on('click', 'div.symbol', function (this: HTMLElement) {
            ctrl.jumpToGlyphSymbol(this.dataset.color as Color, this.dataset.symbol!);
          });
        },
      },
    },
    [
      playerTable(ctrl, 'white'),
      h(
        'a.button.text',
        {
          class: { active: !!ctrl.retro },
          attrs: dataIcon(licon.PlayTriangle),
          hook: bind('click', ctrl.toggleRetro, ctrl.redraw),
        },
        'Learn from your mistakes',
      ),
      playerTable(ctrl, 'black'),
    ],
  );
};

export function puzzleLink(ctrl: AnalyseCtrl): VNode | undefined {
  const puzzle = ctrl.data.puzzle;
  return (
    puzzle &&
    h(
      'div.analyse__puzzle',
      h(
        'a.button-link.text',
        {
          attrs: {
            'data-icon': licon.ArcheryTarget,
            href: `/training/${puzzle.key}/${ctrl.bottomColor()}`,
          },
        },
        ['Recommended puzzle training', h('br'), puzzle.name],
      ),
    )
  );
}

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  if (
    !ctrl.data.analysis ||
    !ctrl.showFishnetAnalysis()
  )
    return h('div.analyse__round-training', puzzleLink(ctrl));

  // don't cache until the analysis is complete!
  const buster = ctrl.data.analysis.partial ? Math.random() : '';
  const cacheKey = '' + buster + !!ctrl.retro;

  return h('div.analyse__round-training', [
    h('div.analyse__acpl', thunk('div.advice-summary', doRender, [ctrl, cacheKey])),
    puzzleLink(ctrl),
  ]);
}
