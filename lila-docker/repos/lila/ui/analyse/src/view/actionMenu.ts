import { isEmpty } from 'lib';
import * as licon from 'lib/licon';
import { displayColumns } from 'lib/device';
import type { VNode, LooseVNodes, MaybeVNodes, ToggleSettings } from 'lib/view';
import { domDialog, bind, dataIcon, hl, toggle } from 'lib/view';
import type { AutoplayDelay } from '../autoplay';
import type AnalyseCtrl from '../ctrl';
import { cont as contRoute } from 'lib/game/router';
import { myUserId } from 'lib';
import * as xhr from 'lib/xhr';

import { clamp } from 'lib/algo';

interface AutoplaySpeed {
  name: string;
  delay: AutoplayDelay;
}

const baseSpeeds: AutoplaySpeed[] = [
  { name: 'Fast', delay: 1000 },
  { name: 'Slow', delay: 5000 },
];

const realtimeSpeed: AutoplaySpeed = {
  name: 'Realtime',
  delay: 'realtime',
};

const cplSpeed: AutoplaySpeed = {
  name: 'By CPL',
  delay: 'cpl',
};

const ctrlToggle = (t: ToggleSettings, ctrl: AnalyseCtrl) => toggle(t, ctrl.redraw);

type PrefOptions = {
  piece?: { d2?: { current: string; list: Array<{ name: string }> } };
  board?: { d2?: { current: string; list: Array<{ name: string }> } };
};

let prefOptions: PrefOptions | null = null;
let prefOptionsPromise: Promise<void> | null = null;

function ensurePrefOptions(ctrl: AnalyseCtrl): void {
  if (prefOptions || prefOptionsPromise) return;
  prefOptionsPromise = xhr
    .json('/api/pref/options')
    .then(data => {
      prefOptions = data as PrefOptions;
      prefOptionsPromise = null;
      ctrl.redraw();
    })
    .catch(() => {
      prefOptionsPromise = null;
    });
}

function prefSelect(
  label: string,
  current: string | undefined,
  options: string[],
  onChange: (v: string) => void,
): VNode {
  return hl('span.setting', [
    hl('label', label),
    hl(
      'select',
      {
        attrs: { disabled: !options.length },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLSelectElement;
            el.value = current || '';
            el.addEventListener('change', () => onChange(el.value));
          },
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLSelectElement;
            const next = current || '';
            if (el.value !== next) el.value = next;
          },
        },
      },
      options.map(o => hl('option', { attrs: { value: o } }, o)),
    ),
  ]);
}

type PrefSelectOption = { value: string; label: string };

function prefSelectKV(
  label: string,
  current: string | undefined,
  options: PrefSelectOption[],
  onChange: (v: string) => void,
): VNode {
  return hl('span.setting', [
    hl('label', label),
    hl(
      'select',
      {
        attrs: { disabled: !options.length },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLSelectElement;
            el.value = current || '';
            el.addEventListener('change', () => onChange(el.value));
          },
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLSelectElement;
            const next = current || '';
            if (el.value !== next) el.value = next;
          },
        },
      },
      options.map(o => hl('option', { attrs: { value: o.value } }, o.label)),
    ),
  ]);
}

function prefBoolToggle(ctrl: AnalyseCtrl, name: string, id: string, checked: boolean, prefKey: string): VNode {
  return ctrlToggle(
    {
      name,
      id,
      checked,
      change: v => xhr.text(`/pref/${prefKey}?v=${v ? '1' : '0'}`, { method: 'post' }).then(() => site.reload()),
    },
    ctrl,
  );
}

function autoplayButtons(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;
  const speeds = [
    ...baseSpeeds,
    ...(d.game.speed !== 'correspondence' && !isEmpty(d.game.moveCentis) ? [realtimeSpeed] : []),
    ...(d.analysis ? [cplSpeed] : []),
  ];
  return hl(
    'div.autoplay',
    speeds.map(speed => {
      const active = ctrl.autoplay.getDelay() === speed.delay;
      return hl(
        'a.button',
        {
          class: { active, 'button-empty': !active },
          hook: bind('click', () => ctrl.togglePlay(speed.delay), ctrl.redraw),
        },
        String(speed.name),
      );
    }),
  );
}





export function view(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data,
    canContinue = !ctrl.ongoing && d.game.variant.key === 'standard',
    canPractice = ctrl.isCevalAllowed() && !ctrl.isEmbed && !ctrl.practice,
    canRetro = ctrl.hasFullComputerAnalysis() && !ctrl.isEmbed && !ctrl.retro,
    linkAttrs = { rel: ctrl.isEmbed ? '' : 'nofollow', target: ctrl.isEmbed ? '_blank' : '' };

  const isLoggedIn = !!myUserId();
  if (isLoggedIn) ensurePrefOptions(ctrl);

  const animationDuration = d.pref.animationDuration ?? 250;
  const animationPrefValue =
    animationDuration === 0 ? '0' : animationDuration <= 150 ? '1' : animationDuration >= 450 ? '3' : '2';

  const tools: MaybeVNodes = [
    hl('div.action-menu__tools', [
      hl(
        'a',
        {
          hook: bind('click', () => {
            ctrl.flip();
            ctrl.actionMenu.toggle();
            ctrl.redraw();
          }),
          attrs: { 'data-icon': licon.ChasingArrows, title: 'Hotkey: f' },
        },
        'Flip board',
      ),
      !ctrl.ongoing &&
      hl(
        'a',
        {
          attrs: {
            href: d.userAnalysis
              ? '/editor?' +
              new URLSearchParams({
                fen: ctrl.node.fen,
                variant: d.game.variant.key,
                color: ctrl.chessground.state.orientation,
              })
              : `/${d.game.id}/edit?fen=${ctrl.node.fen}`,
            'data-icon': licon.Pencil,
            ...linkAttrs,
          },
        },
        'Board editor',
      ),
      displayColumns() === 1 &&
      canPractice &&
      hl(
        'a',
        { hook: bind('click', () => ctrl.togglePractice()), attrs: dataIcon(licon.Bullseye) },
        'Practice with computer',
      ),
      canRetro &&
      hl(
        'a',
        { hook: bind('click', ctrl.toggleRetro, ctrl.redraw), attrs: dataIcon(licon.GraduateCap) },
        'Learn from your mistakes',
      ),
      ,
      canContinue &&
      hl(
        'a',
        {
          hook: bind('click', () =>
            domDialog({ cash: $('.continue-with.g_' + d.game.id), modal: true, show: true }),
          ),
          attrs: dataIcon(licon.Swords),
        },
        'Continue from here',
      ),

      ctrl.idbTree.isDirty &&
      hl(
        'a',
        {
          attrs: {
            title: 'Clear saved moves',
            'data-icon': licon.Trash,
          },
          hook: bind('click', ctrl.idbTree.clear),
        },
        'Clear saved moves',
      ),
    ]),
  ];

  const cevalConfig: LooseVNodes = [
    displayColumns() > 1 && hl('h2', 'Computer analysis'),
    ctrlToggle(
      {
        name: 'Show fishnet analysis',
        title: 'Show fishnet analysis (Hotkey: z)',
        id: 'all',
        checked: ctrl.showFishnetAnalysis(),
        change: ctrl.toggleFishnetAnalysis,
      },
      ctrl,
    ),
    ctrlToggle(
      {
        name: 'Best move arrow',
        title: 'Hotkey: a',
        id: 'shapes',
        checked: ctrl.showBestMoveArrowsProp(),
        change: ctrl.showBestMoveArrowsProp,
      },
      ctrl,
    ),
    displayColumns() > 1 &&
    ctrlToggle(
      {
        name: 'Evaluation gauge',
        id: 'gauge',
        checked: ctrl.showGauge(),
        change: ctrl.showGauge,
      },
      ctrl,
    ),
  ];

  const displayConfig = [
    displayColumns() > 1 && hl('h2', 'Display'),
    ctrlToggle(
      {
        name: 'Inline notation',
        title: 'Shift+I',
        id: 'inline',
        checked: ctrl.treeView.modePreference() === 'inline',
        change(v) {
          ctrl.treeView.modePreference(v ? 'inline' : 'column');
          ctrl.actionMenu.toggle();
        },
      },
      ctrl,
    ),
    ctrlToggle(
      {
        name: 'Disclosure buttons',
        title: 'Show disclosure buttons to expand/collapse variations',
        id: 'disclosure',
        checked: ctrl.disclosureMode(),
        change: ctrl.disclosureMode,
      },
      ctrl,
    ),
    !ctrl.ongoing &&
    ctrlToggle(
      {
        name: 'Annotations on board',
        title: 'Display analysis symbols on the board',
        id: 'move-annotation',
        checked: ctrl.possiblyShowMoveAnnotationsOnBoard(),
        change: ctrl.togglePossiblyShowMoveAnnotationsOnBoard,
      },
      ctrl,
    ),
  ];

  const behaviorConfig: LooseVNodes = [
    displayColumns() > 1 && hl('h2', 'Board behavior'),
    !isLoggedIn && hl('div.setting', [hl('span', 'Sign in to change board behavior settings.')]),
    isLoggedIn && [
      prefBoolToggle(ctrl, 'Highlights', 'pref-highlight', !!d.pref.highlight, 'highlight'),
      prefBoolToggle(ctrl, 'Show destinations', 'pref-destination', !!d.pref.destination, 'destination'),
      prefBoolToggle(ctrl, 'Castle by moving onto rook', 'pref-rookCastle', !!d.pref.rookCastle, 'rookCastle'),
      prefSelectKV(
        'Move input',
        String(d.pref.moveEvent),
        [
          { value: '0', label: 'Click' },
          { value: '1', label: 'Drag' },
          { value: '2', label: 'Both' },
        ],
        v => xhr.text(`/pref/moveEvent?v=${encodeURIComponent(v)}`, { method: 'post' }).then(() => site.reload()),
      ),
      prefSelectKV(
        'Coordinates',
        String(d.pref.coords),
        [
          { value: '0', label: 'Hidden' },
          { value: '1', label: 'Inside' },
          { value: '2', label: 'Outside' },
          { value: '3', label: 'All squares' },
        ],
        v => xhr.text(`/pref/coords?v=${encodeURIComponent(v)}`, { method: 'post' }).then(() => site.reload()),
      ),
      prefSelectKV(
        'Animation',
        animationPrefValue,
        [
          { value: '0', label: 'None' },
          { value: '1', label: 'Fast' },
          { value: '2', label: 'Normal' },
          { value: '3', label: 'Slow' },
        ],
        v => xhr.text(`/pref/animation?v=${encodeURIComponent(v)}`, { method: 'post' }).then(() => site.reload()),
      ),
    ],
  ];

  const appearanceConfig: LooseVNodes = [
    displayColumns() > 1 && hl('h2', 'Appearance'),
    !isLoggedIn && hl('div.setting', [hl('span', 'Sign in to change appearance settings.')]),
    isLoggedIn &&
    (prefOptions
      ? [
        prefSelect(
          'Piece set',
          prefOptions.piece?.d2?.current,
          (prefOptions.piece?.d2?.list || []).map(p => p.name),
          v =>
            xhr.text(`/pref/pieceSet?v=${encodeURIComponent(v)}`, { method: 'post' }).then(() => site.reload()),
        ),
        prefSelect(
          'Board theme',
          prefOptions.board?.d2?.current,
          (prefOptions.board?.d2?.list || []).map(t => t.name),
          v => xhr.text(`/pref/theme?v=${encodeURIComponent(v)}`, { method: 'post' }).then(() => site.reload()),
        ),
      ]
      : [hl('div.setting', 'Loading…')]),
  ];

  return hl('div.action-menu', [
    tools,
    displayConfig,
    displayColumns() > 1 && renderVariationOpacitySlider(ctrl),
    behaviorConfig,
    cevalConfig,
    appearanceConfig,
    displayColumns() === 1 && renderVariationOpacitySlider(ctrl),
    ctrl.mainline.length > 4 && [hl('h2', 'Replay mode'), autoplayButtons(ctrl)],
    canContinue &&
    hl('div.continue-with.none.g_' + d.game.id, [
      hl(
        'a.button',
        {
          attrs: {
            href: d.userAnalysis
              ? '/?fen=' + ctrl.encodeNodeFen() + '#ai'
              : contRoute(d, 'ai') + '?fen=' + ctrl.node.fen,
            ...linkAttrs,
          },
        },
        'Play against computer',
      ),
      hl(
        'a.button',
        {
          attrs: {
            href: d.userAnalysis
              ? '/?fen=' + ctrl.encodeNodeFen() + '#friend'
              : contRoute(d, 'friend') + '?fen=' + ctrl.node.fen,
            ...linkAttrs,
          },
        },
        'Challenge a friend',
      ),
    ]),
  ]);
}

function renderVariationOpacitySlider(ctrl: AnalyseCtrl) {
  return hl('span.setting', [
    hl('label', 'Variation opacity'),
    hl('input.range', {
      key: 'variation-arrows',
      attrs: { min: 0, max: 1, step: 0.1, type: 'range', value: ctrl.variationArrowOpacity() || 0 },
      props: { value: ctrl.variationArrowOpacity() || 0 },
      hook: {
        insert: (vnode: VNode) => {
          const input = vnode.elm as HTMLInputElement;
          input.addEventListener('input', () => {
            ctrl.variationArrowOpacity(parseFloat(input.value));
          });
          input.addEventListener('wheel', e => {
            e.preventDefault();
            ctrl.variationArrowOpacity(
              clamp((ctrl.variationArrowOpacity() || 0) + (e.deltaY > 0 ? -0.1 : 0.1), {
                min: 0,
                max: 1,
              }),
            );
          });
        },
      },
    }),
  ]);
}
