import type { VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { displayLocale, numberFormat } from 'lib/format';
import perfIcons from 'lib/game/perfIcons';
import { bind, dataIcon, type MaybeVNode, type LooseVNodes, hl } from 'lib/view';
import { view as renderConfig } from './explorerConfig';
import { moveArrowAttributes, ucfirst } from './explorerUtil';
import type AnalyseCtrl from '../ctrl';
import {
  isOpening,
  isTablebase,
  type TablebaseCategory,
  type OpeningData,
  type OpeningMoveStats,
  type OpeningGame,
  type ExplorerDb,
} from './interfaces';
import ExplorerCtrl, { MAX_DEPTH } from './explorerCtrl';
import { showTablebase } from './tablebaseView';

function resultBar(move: OpeningMoveStats): VNode {
  const sum = move.white + move.draws + move.black;
  const section = (key: 'white' | 'black' | 'draws') => {
    const percent = (move[key] * 100) / sum;
    return hl(
      'span.' + key,
      { attrs: { style: 'width: ' + Math.round((move[key] * 1000) / sum) / 10 + '%' } },
      percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : '',
    );
  };
  return hl('div.bar', ['white', 'draws', 'black'].map(section));
}

function showMoveTable(ctrl: AnalyseCtrl, data: OpeningData): VNode | null {
  if (!data.moves.length) return null;
  const sumTotal = data.white + data.black + data.draws;
  const movesWithCurrent =
    data.moves.length > 1
      ? [
        ...data.moves,
        {
          white: data.white,
          black: data.black,
          draws: data.draws,
          uci: '',
          san: 'Σ',
        } as OpeningMoveStats,
      ]
      : data.moves;

  return hl('table.moves', [
    hl('thead', [
      hl('tr', [
        hl('th', 'Move'),
        hl('th', { attrs: { colspan: 2 } }, 'Games'),
        hl('th', 'White / Draw / Black'),
      ]),
    ]),
    hl(
      'tbody',
      moveArrowAttributes(ctrl, { fen: data.fen, onClick: (_, uci) => uci && ctrl.explorerMove(uci) }),
      movesWithCurrent.map(move => {
        const total = move.white + move.draws + move.black;
        return hl(`tr${move.uci ? '' : '.sum'}`, { key: move.uci, attrs: { 'data-uci': move.uci } }, [
          hl(
            'td',
            { attrs: { title: move.opening ? `${move.opening.eco}: ${move.opening.name}` : '' } },
            move.san,
          ),
          hl('td', ((total / sumTotal) * 100).toFixed(0) + '%'),
          hl('td', bigNumberFormatter ? bigNumberFormatter.format(total) : numberFormat(total)),
          hl('td', { attrs: { title: moveStatsTooltip(ctrl, move) } }, resultBar(move)),
        ]);
      }),
    ),
  ]);
}

const bigNumberFormatter =
  window.Intl && Intl.NumberFormat ? new Intl.NumberFormat(displayLocale, { notation: 'compact' }) : null;

function moveStatsTooltip(ctrl: AnalyseCtrl, move: OpeningMoveStats): string {
  if (!move.uci) return 'Total';
  if (move.game) {
    const g = move.game;
    const result = g.winner === 'white' ? '1-0' : g.winner === 'black' ? '0-1' : '½-½';
    return ctrl.explorer.opts.showRatings
      ? `${g.white.name} (${g.white.rating}) ${result} ${g.black.name} (${g.black.rating})`
      : `${g.white.name} ${result} ${g.black.name}`;
  }
  if (ctrl.explorer.opts.showRatings) {
    if (move.averageRating) return `Average rating: ${move.averageRating}`;
    if (move.averageOpponentRating)
      return `Performance rating: ${move.performance}, average opponent: ${move.averageOpponentRating}`;
  }
  return '';
}

const showResult = (winner?: Color): VNode =>
  winner === 'white'
    ? hl('result.white', '1-0')
    : winner === 'black'
      ? hl('result.black', '0-1')
      : hl('result.draws', '½-½');

function showGameTable(ctrl: AnalyseCtrl, fen: FEN, title: string, games: OpeningGame[]): VNode | null {
  if (!ctrl.explorer.withGames || !games.length) return null;
  const openedId = ctrl.explorer.gameMenu(),
    isMasters = ctrl.explorer.db() === 'masters';
  return hl('table.games', [
    hl('thead', [hl('tr', [hl('th.title', { attrs: { colspan: isMasters ? 4 : 5 } }, title)])]),
    hl(
      'tbody',
      moveArrowAttributes(ctrl, {
        fen,
        onClick: (e, _) => {
          const $tr = $(e.target as HTMLElement).parents('tr');
          const id = $tr.data('id');
          openGame(ctrl, id);
        },
      }),
      games.map(game => {
        return openedId === game.id
          ? gameActions(ctrl, game)
          : hl('tr', { key: game.id, attrs: { 'data-id': game.id, 'data-uci': game.uci || '' } }, [
            ctrl.explorer.opts.showRatings &&
            hl(
              'td',
              [game.white, game.black].map(p => hl('span', '' + p.rating)),
            ),
            hl(
              'td',
              [game.white, game.black].map(p => hl('span', p.name)),
            ),
            hl('td', showResult(game.winner)),
            hl('td', game.month || game.year),
            !isMasters &&
            hl(
              'td',
              game.speed &&
              hl('i', { attrs: { title: ucfirst(game.speed), ...dataIcon(perfIcons[game.speed]!) } }),
            ),
          ]);
      }),
    ),
  ]);
}

function openGame(ctrl: AnalyseCtrl, gameId: string) {
  const orientation = ctrl.chessground.state.orientation,
    fenParam = ctrl.node.ply > 0 ? '?fen=' + ctrl.node.fen : '';
  let url = '/' + gameId + '/' + orientation + fenParam;
  if (ctrl.explorer.db() === 'masters') url = '/import/master' + url;
  window.open(url, '_blank');
}

function gameActions(ctrl: AnalyseCtrl, game: OpeningGame): VNode {

  return hl('tr', { key: game.id + '-m' }, [
    hl('td.game_menu', { attrs: { colspan: ctrl.explorer.db() === 'masters' ? 4 : 5 } }, [
      hl(
        'div.game_title',
        `${game.white.name} - ${game.black.name}, ${showResult(game.winner).text}, ${game.year}`,
      ),
      hl('div.menu', [
        hl(
          'a.text',
          { attrs: dataIcon(licon.Eye), hook: bind('click', _ => openGame(ctrl, game.id)) },
          'View',
        ),
        hl(
          'a.text',
          { attrs: dataIcon(licon.X), hook: bind('click', _ => ctrl.explorer.gameMenu(null), ctrl.redraw) },
          'Close',
        ),
      ]),
    ]),
  ]);
}

const closeButton = (ctrl: AnalyseCtrl): VNode =>
  hl(
    'button.button.button-empty.text',
    { attrs: dataIcon(licon.X), hook: bind('click', ctrl.toggleExplorer, ctrl.redraw) },
    'Close',
  );

const showEmpty = (ctrl: AnalyseCtrl, data?: OpeningData): VNode => {
  const isTooDeep = ctrl.explorer.root.node.ply >= MAX_DEPTH;
  return hl('div.data.empty', [
    explorerTitle(ctrl.explorer),
    openingTitle(ctrl, data),
    hl('div.message', [
      hl('strong', isTooDeep ? 'Max depth reached' : 'No game found'),
      !!data?.queuePosition
        ? hl('p.explanation', `Indexing ${data.queuePosition} other players first ...`)
        : !(ctrl.explorer.config.fullHouse() || isTooDeep) &&
        hl('p.explanation', 'Maybe include more games from the preferences menu'),
    ]),
  ]);
};

const showGameEnd = (ctrl: AnalyseCtrl, title: string): VNode =>
  hl('div.data.empty', [
    hl('div.title', 'Game over'),
    hl('div.message', [hl('i', { attrs: dataIcon(licon.InfoCircle) }), hl('h3', title), closeButton(ctrl)]),
  ]);

const openingTitle = (ctrl: AnalyseCtrl, data?: OpeningData) => {
  const opening = data?.opening;
  const title = opening ? `${opening.eco} ${opening.name}` : '';
  return hl(
    'div.title',
    { attrs: opening ? { title } : {} },
    opening
      ? [hl('a', { attrs: { href: `/opening/${opening.name}`, target: '_blank' } }, title)]
      : [showTitle(ctrl.data.game.variant)],
  );
};

let lastShow: MaybeVNode;
export const clearLastShow = () => {
  lastShow = undefined;
};

function show(ctrl: AnalyseCtrl): MaybeVNode {
  const data = ctrl.explorer.current();
  if (data && isOpening(data)) {
    const moveTable = showMoveTable(ctrl, data),
      recentTable = showGameTable(ctrl, data.fen, 'Recent games', data.recentGames || []),
      topTable = showGameTable(ctrl, data.fen, 'Top games', data.topGames || []);
    if (moveTable || recentTable || topTable)
      lastShow = hl('div.data', [
        explorerTitle(ctrl.explorer),
        data?.opening && openingTitle(ctrl, data),
        moveTable,
        topTable,
        recentTable,
      ]);
    else lastShow = showEmpty(ctrl, data);
  } else if (data && isTablebase(data)) {
    const row = (category: TablebaseCategory, title: string, tooltip?: string) =>
      showTablebase(
        ctrl,
        data.fen,
        title,
        tooltip,
        data.moves.filter(m => m.category === category),
      );
    if (data.moves.length)
      lastShow = hl('div.data', [
        row('loss', 'Winning'),
        row('unknown', 'Unknown'),
        row('syzygy-loss', 'Win or 50 moves by prior mistake', 'Unknown due to rounding'),
        row('maybe-loss', 'Win or 50 moves'),
        row('blessed-loss', 'Win prevented by 50 move rule'),
        row('draw', 'Drawn'),
        row('cursed-win', 'Loss saved by 50 move rule'),
        row('maybe-win', 'Loss or 50 moves'),
        row('syzygy-win', 'Loss or 50 moves by prior mistake', 'Unknown due to rounding'),
        row('win', 'Losing'),
      ]);
    else if (data.checkmate) lastShow = showGameEnd(ctrl, 'Checkmate');
    else if (data.stalemate) lastShow = showGameEnd(ctrl, 'Stalemate');
    else if (data.variant_win || data.variant_loss) lastShow = showGameEnd(ctrl, 'Variant ending');
    else lastShow = showEmpty(ctrl);
  }
  return lastShow;
}

const explorerTitle = (explorer: ExplorerCtrl) => {
  const db = explorer.db();
  const otherLink = (dbKey: ExplorerDb, label: string, title: string) =>
    hl(
      'button.button-link',
      {
        key: dbKey,
        attrs: { title },
        hook: bind('click', () => explorer.config.data.db(dbKey), explorer.reload),
      },
      label,
    );
  const playerLink = () =>
    hl(
      'button.button-link.player',
      {
        key: 'player',
        hook: bind(
          'click',
          () => {
            explorer.config.selectPlayer(playerName || 'me');
            if (explorer.db() !== 'player') {
              explorer.config.data.db('player');
              explorer.config.data.open(true);
            }
          },
          explorer.reload,
        ),
      },
      'Player',
    );
  const active = (nodes: LooseVNodes, title: string) =>
    hl(
      'span.active.text.' + db,
      {
        attrs: { title, ...dataIcon(licon.Book) },
        hook: db === 'player' ? bind('click', explorer.config.toggleColor, explorer.reload) : undefined,
      },
      nodes,
    );
  const playerName = explorer.config.data.playerName.value();
  const masterDbExplanation = '2 million games from top rated FIDE players from 1952 to 2024-08',
    onlineDbExplanation = 'Large community database';
  const data = explorer.current();
  const queuePosition = data && isOpening(data) && data.queuePosition;
  return hl('div.explorer-title', [
    db === 'masters'
      ? active([hl('strong', 'Masters'), ' database'], masterDbExplanation)
      : explorer.config.allDbs.includes('masters') && otherLink('masters', 'Masters', masterDbExplanation),
    db === 'lichess'
      ? active([hl('strong', 'Online'), ' database'], onlineDbExplanation)
      : otherLink('lichess', 'Online', onlineDbExplanation),
    db === 'player'
      ? playerName
        ? active(
          [
            hl(`strong${playerName.length > 14 ? '.long' : ''}`, playerName),
            ` ${explorer.config.data.color() === 'white' ? 'as White' : 'as Black'}`,
            explorer.isIndexing() &&
            !explorer.config.data.open() &&
            hl('i.ddloader', {
              attrs: {
                title: queuePosition
                  ? `Indexing ${queuePosition} other players first ...`
                  : 'Indexing ...',
              },
            }),
          ],
          'Switch sides',
        )
        : active([hl('strong', 'Player'), ' database'], '')
      : playerLink(),
  ]);
};

function showTitle(variant: Variant) {
  if (variant.key === 'standard' || variant.key === 'fromPosition') return 'Opening explorer';
  return `${variant.name} opening explorer`;
}

function showConfig(ctrl: AnalyseCtrl): VNode {
  return hl('div.config', [explorerTitle(ctrl.explorer), renderConfig(ctrl.explorer.config)]);
}

function showFailing(ctrl: AnalyseCtrl) {
  return hl('div.data.empty', [
    hl('div.title', showTitle(ctrl.data.game.variant)),
    hl('div.failing.message', [
      hl('h3', 'Oops, sorry!'),
      hl('p.explanation', ctrl.explorer.failing()?.toString()),
      closeButton(ctrl),
    ]),
  ]);
}

let lastFen: FEN = '';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const explorer = ctrl.explorer;
  if (!explorer.enabled()) return;
  const data = explorer.current(),
    config = explorer.config,
    configOpened = config.data.open(),
    loading = !configOpened && (explorer.loading() || (!data && !explorer.failing())),
    content = configOpened ? showConfig(ctrl) : explorer.failing() ? showFailing(ctrl) : show(ctrl);
  return hl(
    `section.explorer-box.sub-box${configOpened ? '.explorer__config' : ''}`,
    {
      class: { loading, reduced: !configOpened && (!!explorer.failing() || explorer.movesAway() > 2) },
      hook: {
        insert: vnode => ((vnode.elm as HTMLElement).scrollTop = 0),
        postpatch(_, vnode) {
          if (!data || lastFen === data.fen) return;
          (vnode.elm as HTMLElement).scrollTop = 0;
          lastFen = data.fen;
        },
      },
    },
    [
      hl('div.overlay'),
      content,
      hl('button.fbt.toconf', {
        attrs: {
          'aria-label': configOpened ? 'Close configuration' : 'Open configuration',
          ...dataIcon(configOpened ? licon.X : licon.Gear),
        },
        hook: bind('click', () => ctrl.explorer.config.toggleOpen(), ctrl.redraw),
      }),
    ],
  );
}
