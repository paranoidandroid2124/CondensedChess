import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { copyMeInput, dataIcon } from 'lib/view';
import type { MouchEvent, NumberPair } from '@lichess-org/chessground/types';
import { dragNewPiece } from '@lichess-org/chessground/drag';
import { eventPosition, opposite } from '@lichess-org/chessground/util';
import type { Rules } from 'chessops/types';
import { parseFen } from 'chessops/fen';
import { parseSquare, makeSquare } from 'chessops/util';
import type EditorCtrl from './ctrl';
import chessground from './chessground';
import type { Selected, CastlingToggle, EditorState, EndgamePosition, OpeningPosition } from './interfaces';
import { fenToEpd } from 'lib/game/chess';

function castleCheckBox(ctrl: EditorCtrl, id: CastlingToggle, label: string, reversed: boolean): VNode {
  const input = h('input', {
    attrs: { type: 'checkbox' },
    props: { checked: ctrl.castlingToggles[id] },
    on: {
      change(e) {
        ctrl.setCastlingToggle(id, (e.target as HTMLInputElement).checked);
      },
    },
  });
  return h('label', reversed ? [input, label] : [label, input]);
}

function optgroup(name: string, opts: VNode[]): VNode {
  return h('optgroup', { attrs: { label: name } }, opts);
}

function variant2option(key: Rules, name: string, ctrl: EditorCtrl): VNode {
  return h(
    'option',
    { attrs: { value: key, selected: key === ctrl.rules } },
    `Variant | ${name}`,
  );
}

const allVariants: Array<[Rules, string]> = [
  ['chess', 'Standard'],
  ['antichess', 'Antichess'],
  ['atomic', 'Atomic'],
  ['crazyhouse', 'Crazyhouse'],
  ['horde', 'Horde'],
  ['kingofthehill', 'King of the Hill'],
  ['racingkings', 'Racing Kings'],
  ['3check', 'Three-check'],
];

function controls(ctrl: EditorCtrl, state: EditorState): VNode {
  const endgamePosition2option = function (pos: EndgamePosition): VNode {
    return h('option', { attrs: { value: pos.epd || pos.fen, 'data-fen': pos.fen } }, pos.name);
  };

  const buttonStart = (icon?: string) =>
    h(
      `a.button.button-empty${icon ? '.text' : ''}`,
      { on: { click: ctrl.startPosition }, attrs: icon ? dataIcon(icon) : {} },
      'Start position',
    );
  const buttonClear = (icon?: string) =>
    h(
      `a.button.button-empty${icon ? '.text' : ''}`,
      { on: { click: ctrl.clearBoard }, attrs: icon ? dataIcon(icon) : {} },
      'Clear board',
    );

  return h('div.board-editor__tools', [
    h('div.metadata', [
      h(
        'div.color',
        h(
          'select',
          {
            on: {
              change(e) {
                ctrl.setTurn((e.target as HTMLSelectElement).value as Color);
              },
            },
            props: { value: ctrl.turn },
          },
          (['whitePlays', 'blackPlays'] as const).map(function (key) {
            return h(
              'option',
              {
                attrs: { value: key[0] === 'w' ? 'white' : 'black', selected: key[0] === ctrl.turn[0] },
              },
              key === 'whitePlays' ? 'White to play' : 'Black to play',
            );
          }),
        ),
      ),
      h('div.castling', [
        h('strong', 'Castling'),
        h('div', [
          castleCheckBox(ctrl, 'K', 'O-O', !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'Q', 'O-O-O', true),
        ]),
        h('div', [
          castleCheckBox(ctrl, 'k', 'O-O', !!ctrl.options.inlineCastling),
          castleCheckBox(ctrl, 'q', 'O-O-O', true),
        ]),
      ]),
      h('div.enpassant', [
        h('label', { attrs: { for: 'enpassant-select' } }, 'En passant'),
        h(
          'select#enpassant-select',
          {
            on: {
              change(e) {
                ctrl.setEnPassant(parseSquare((e.target as HTMLSelectElement).value));
              },
            },
            props: { value: ctrl.epSquare ? makeSquare(ctrl.epSquare) : '' },
          },
          ['', ...[ctrl.turn === 'black' ? 3 : 6].flatMap(r => 'abcdefgh'.split('').map(f => f + r))].map(
            key =>
              h(
                'option',
                {
                  attrs: {
                    value: key,
                    selected: (key ? parseSquare(key) : undefined) === ctrl.epSquare,
                    hidden: Boolean(key && !state.enPassantOptions.includes(key)),
                    disabled: Boolean(key && !state.enPassantOptions.includes(key)) /*Safari*/,
                  },
                },
                key,
              ),
          ),
        ),
      ]),
    ]),
    ...(ctrl.cfg.embed || !ctrl.cfg.positions || !ctrl.cfg.endgamePositions
      ? []
      : [
        (() => {
          const positionOption = (pos: OpeningPosition): VNode =>
            h(
              'option',
              { attrs: { value: pos.epd || pos.fen, 'data-fen': pos.fen } },
              pos.eco ? `${pos.eco} ${pos.name}` : pos.name,
            );
          const epd = fenToEpd(state.fen);
          const value =
            (
              ctrl.cfg.positions.find(p => p.fen.startsWith(epd)) ||
              ctrl.cfg.endgamePositions.find(p => p.epd === epd)
            )?.epd || '';
          return h(
            'select.positions',
            {
              props: { value },
              on: {
                insert(vnode) {
                  (vnode.elm as HTMLSelectElement).value = fenToEpd(state.fen);
                },
                change(e) {
                  const el = e.target as HTMLSelectElement;
                  const value = el.selectedOptions[0].getAttribute('data-fen');
                  if (!value || !ctrl.setFen(value)) el.value = '';
                },
              },
            },
            [
              h('option', { attrs: { value: '' } }, 'Set the board'),
              optgroup('Popular openings', ctrl.cfg.positions.map(positionOption)),
              optgroup('Endgame positions', ctrl.cfg.endgamePositions.map(endgamePosition2option)),
            ],
          );
        })(),
      ]),
    ...(ctrl.cfg.embed
      ? [h('div.actions', [buttonStart(), buttonClear()])]
      : [
        h('div', [
          h(
            'select',
            {
              attrs: { id: 'variants' },
              on: {
                change(e) {
                  ctrl.setRules((e.target as HTMLSelectElement).value as Rules);
                },
              },
            },
            allVariants.map(x => variant2option(x[0], x[1], ctrl)),
          ),
        ]),
        h('div.actions', [
          buttonStart(licon.Reload),
          buttonClear(licon.Trash),
          h(
            'button.button.button-empty.text',
            {
              attrs: { 'data-icon': licon.ChasingArrows },
              on: {
                click() {
                  ctrl.chessground!.toggleOrientation();
                  ctrl.onChange();
                },
              },
            },
            'Flip board',
          ),
          h(
            'a',
            {
              attrs: {
                'data-icon': licon.Microscope,
                rel: 'nofollow',
                ...(state.legalFen
                  ? { href: ctrl.makeAnalysisUrl(state.legalFen, ctrl.bottomColor()) }
                  : {}),
              },
              class: {
                button: true,
                'button-empty': true,
                text: true,
                disabled: !state.legalFen,
              },
            },
            'Analysis',
          ),
        ]),
      ]),
  ]);
}

function inputs(ctrl: EditorCtrl, fen: string): VNode | undefined {
  if (ctrl.cfg.embed) return;
  return h('div.copyables', [
    h('p', [
      h('strong', 'FEN'),
      h('input', {
        attrs: { spellcheck: 'false', enterkeyhint: 'done' },
        props: { value: fen },
        on: {
          change(e) {
            const el = e.target as HTMLInputElement;
            ctrl.setFen(el.value.trim());
            el.reportValidity();
          },
          input(e) {
            const el = e.target as HTMLInputElement;
            const valid = parseFen(el.value.trim()).isOk;
            el.setCustomValidity(valid ? '' : 'Invalid FEN');
          },
          blur(e) {
            const el = e.target as HTMLInputElement;
            el.value = ctrl.getFen();
            el.setCustomValidity('');
          },
          keypress(e) {
            const el = e.target as HTMLInputElement;
            if (e.key === 'Enter') {
              el.blur();
            }
          },
        },
      }),
    ]),
    h('p', [h('strong.name', 'URL'), copyMeInput(ctrl.makeEditorUrl(fen, ctrl.bottomColor()))]),
  ]);
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return s === 'pointer' || s === 'trash' ? s : s.join(' ');
}

let lastTouchMovePos: NumberPair | undefined;

function sparePieces(ctrl: EditorCtrl, color: Color, _orientation: Color, position: 'top' | 'bottom'): VNode {
  const selectedClass = selectedToClass(ctrl.selected());

  const pieces = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].map(function (role) {
    return [color, role];
  });

  return h(
    'div',
    { attrs: { class: ['spare', 'spare-' + position, 'spare-' + color].join(' ') } },
    ['pointer', ...pieces, 'trash'].map((s: Selected) => {
      const className = selectedToClass(s);
      const attrs = {
        class: className,
        ...(s !== 'pointer' && s !== 'trash'
          ? {
            'data-color': s[0],
            'data-role': s[1],
          }
          : {}),
      };
      const selectedSquare =
        selectedClass === className &&
        (!ctrl.chessground ||
          !ctrl.chessground.state.draggable.current ||
          !ctrl.chessground.state.draggable.current.newPiece);
      return h(
        'div',
        {
          class: {
            'no-square': true,
            pointer: s === 'pointer',
            trash: s === 'trash',
            'selected-square': selectedSquare,
          },
          on: {
            mousedown: onSelectSparePiece(ctrl, s, 'mouseup'),
            touchstart: onSelectSparePiece(ctrl, s, 'touchend'),
            touchmove: e => {
              lastTouchMovePos = eventPosition(e);
            },
          },
        },
        [h('div', [h('piece', { attrs })])],
      );
    }),
  );
}

function onSelectSparePiece(ctrl: EditorCtrl, s: Selected, upEvent: string): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    e.preventDefault();
    if (s === 'pointer' || s === 'trash') {
      ctrl.selected(s);
      ctrl.redraw();
    } else {
      ctrl.selected('pointer');

      dragNewPiece(ctrl.chessground!.state, { color: s[0], role: s[1] }, e, true);

      document.addEventListener(
        upEvent,
        (e: MouchEvent) => {
          const eventPos = eventPosition(e) || lastTouchMovePos;
          if (eventPos && ctrl.chessground!.getKeyAtDomPos(eventPos)) ctrl.selected('pointer');
          else ctrl.selected(s);
          ctrl.redraw();
        },
        { once: true },
      );
    }
  };
}

function makeCursor(selected: Selected): string {
  if (selected === 'pointer') return 'pointer';

  const name = selected === 'trash' ? 'trash' : selected.join('-');
  const url = site.asset.url('cursors/' + name + '.cur');

  return `url('${url}'), default !important`;
}

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState();
  const color = ctrl.bottomColor();

  return h('div.board-editor', { attrs: { style: `cursor: ${makeCursor(ctrl.selected())}` } }, [
    sparePieces(ctrl, opposite(color), color, 'top'),
    h('div.main-board', [chessground(ctrl)]),
    sparePieces(ctrl, color, color, 'bottom'),
    controls(ctrl, state),
    inputs(ctrl, state.legalFen || state.fen),
  ]);
}
