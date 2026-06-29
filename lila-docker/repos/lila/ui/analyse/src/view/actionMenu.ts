import * as licon from 'lib/licon';
import { displayColumns } from 'lib/device';
import type { VNode, ToggleSettings } from 'lib/view';
import { bind, hl, toggle } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import { clamp } from 'lib/algo';

const ctrlToggle = (t: ToggleSettings, ctrl: AnalyseCtrl) => toggle(t, ctrl.redraw);

type BoardSettingsOpts = {
  closeOnChange?: boolean;
  mode?: 'menu' | 'workspace';
};

const flipBoard = (ctrl: AnalyseCtrl, closeMenu: () => void) => {
  ctrl.flip();
  closeMenu();
  ctrl.redraw();
};

const setInlineMoveList = (ctrl: AnalyseCtrl, closeMenu: () => void, inline: boolean) => {
  ctrl.treeView.modePreference(inline ? 'inline' : 'column');
  closeMenu();
  ctrl.redraw();
};

const setVariationControls = (ctrl: AnalyseCtrl, closeMenu: () => void, enabled: boolean) => {
  ctrl.disclosureMode(enabled);
  closeMenu();
  ctrl.redraw();
};

export function view(ctrl: AnalyseCtrl): VNode {
  return hl('div.action-menu', boardSettingsView(ctrl));
}

export function boardSettingsView(ctrl: AnalyseCtrl, opts: BoardSettingsOpts = {}): VNode[] {
  const mode = opts.mode ?? 'menu';
  const closeOnChange = opts.closeOnChange ?? true;
  const closeMenu = () => {
    if (closeOnChange) ctrl.actionMenu.toggle();
  };

  if (mode === 'workspace') return boardWorkspaceView(ctrl, closeMenu);

  return [
    hl('div.action-menu__tools', [
      hl(
        'button',
        {
          hook: bind('click', () => flipBoard(ctrl, closeMenu)),
          attrs: { type: 'button', 'data-icon': licon.ChasingArrows, title: 'Hotkey: f' },
        },
        'Flip board',
      ),
    ]),
    displayColumns() > 1 && hl('h2', 'Display'),
    ctrlToggle(
      {
        name: 'Inline notation',
        title: 'Shift+I',
        id: 'inline',
        checked: ctrl.treeView.modePreference() === 'inline',
        change: v => setInlineMoveList(ctrl, closeMenu, v),
      },
      ctrl,
    ),
    ctrlToggle(
      {
        name: 'Line branches',
        title: 'Show controls to expand or hide side lines',
        id: 'disclosure',
        checked: ctrl.disclosureMode(),
        change: v => setVariationControls(ctrl, closeMenu, v),
      },
      ctrl,
    ),
    renderVariationOpacitySlider(ctrl),
  ].filter(Boolean) as VNode[];
}

function boardWorkspaceView(ctrl: AnalyseCtrl, closeMenu: () => void): VNode[] {
  const engineUnavailable = !ctrl.isCevalAllowed();
  return [
    workspaceSection('View', 'Set how the board and moves read at a glance.', [
      workspaceChoiceCard(
        'Board labels',
        'Choose how coordinates appear around the board.',
        [
          { key: 'off', label: 'Off' },
          { key: 'inside', label: 'Inside' },
          { key: 'rim', label: 'Rim' },
          { key: 'full', label: 'Full' },
        ],
        ctrl.boardLabelMode(),
        mode => {
          ctrl.setBoardLabelMode(mode as 'off' | 'inside' | 'rim' | 'full');
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'Material sidebar',
        'Show captured material beside the board.',
        ctrl.showCapturedMaterial(),
        next => {
          ctrl.setShowCapturedMaterial(next);
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'Inline moves',
        'Read the move history as one flowing line.',
        ctrl.treeView.modePreference() === 'inline',
        next => setInlineMoveList(ctrl, closeMenu, next),
      ),
    ]),
    workspaceSection(
      'Analysis',
      'Keep eval, candidate lines, and board cues visible as the position changes.',
      [
        workspaceSwitchCard(
          'Candidate lines',
          engineUnavailable
            ? 'Candidate lines are unavailable in this position.'
            : 'Compare candidate lines beside the moves.',
          ctrl.showCeval(),
          next => {
            ctrl.showCeval(next);
            closeMenu();
          },
          engineUnavailable,
        ),
        workspaceSwitchCard(
          'Eval bar',
          'Keep the eval bar anchored while you compare the position and candidate lines.',
          ctrl.showGauge(),
          next => {
            ctrl.setShowEvalGauge(next);
            closeMenu();
          },
        ),
        workspaceSwitchCard(
          'On-board cues',
          'Draw arrows, highlights, and review cues directly over the board.',
          ctrl.possiblyShowMoveAnnotationsOnBoard(),
          next => {
            ctrl.togglePossiblyShowMoveAnnotationsOnBoard(next);
            closeMenu();
          },
        ),
        workspaceSwitchCard(
          'Line branches',
          'Show controls to expand or hide side lines.',
          ctrl.disclosureMode(),
          next => setVariationControls(ctrl, closeMenu, next),
        ),
        workspaceSliderCard(
          'Line emphasis',
          'Fade or strengthen variation arrows and branch traces.',
          renderVariationOpacityRange(ctrl, 'Line emphasis'),
        ),
      ],
    ),
    workspaceSection('Orientation', 'Reset perspective without leaving the board.', [
      workspaceActionRow([
        workspaceAction('Flip board', 'Swap sides instantly.', licon.ChasingArrows, () =>
          flipBoard(ctrl, closeMenu),
        ),
        workspaceAction(
          'Return to player side',
          'Snap back to the original board orientation.',
          licon.Checkmark,
          () => {
            ctrl.resetOrientation();
            closeMenu();
          },
          !ctrl.flipped,
        ),
      ]),
    ]),
  ];
}

function renderVariationOpacitySlider(ctrl: AnalyseCtrl): VNode {
  return renderVariationOpacityRange(ctrl, 'Line emphasis');
}

function renderVariationOpacityRange(ctrl: AnalyseCtrl, label: string): VNode {
  return hl('span.setting.action-menu__workspace-range', [
    hl('label', label),
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

function workspaceSection(title: string, description: string, children: VNode[]): VNode {
  return hl('section.action-menu__workspace-section', [
    hl('div.action-menu__workspace-head', [hl('h4', title), hl('p', description)]),
    hl('div.action-menu__workspace-grid', children),
  ]);
}

function workspaceSwitchCard(
  title: string,
  description: string,
  checked: boolean,
  change: (value: boolean) => void,
  disabled = false,
): VNode {
  return hl(
    `button.action-menu__workspace-card${checked ? '.is-active' : ''}${disabled ? '.disabled' : ''}`,
    {
      attrs: disabled ? { type: 'button', disabled: true } : { type: 'button' },
      hook: bind('click', () => {
        if (!disabled) change(!checked);
      }),
    },
    [
      hl('span.action-menu__workspace-copy', [hl('strong', title), hl('span', description)]),
      hl('span.action-menu__workspace-pill', checked ? 'On' : 'Off'),
    ],
  );
}

function workspaceChoiceCard(
  title: string,
  description: string,
  options: Array<{ key: string; label: string }>,
  active: string,
  change: (value: string) => void,
): VNode {
  return hl('div.action-menu__workspace-card.action-menu__workspace-card--stacked', [
    hl('span.action-menu__workspace-copy', [hl('strong', title), hl('span', description)]),
    hl(
      'div.action-menu__workspace-choices',
      options.map(option =>
        hl(
          `button.action-menu__workspace-choice${active === option.key ? '.active' : ''}`,
          {
            key: option.key,
            attrs: { type: 'button' },
            hook: bind('click', () => change(option.key)),
          },
          option.label,
        ),
      ),
    ),
  ]);
}

function workspaceSliderCard(title: string, description: string, control: VNode): VNode {
  return hl('div.action-menu__workspace-card.action-menu__workspace-card--stacked', [
    hl('span.action-menu__workspace-copy', [hl('strong', title), hl('span', description)]),
    control,
  ]);
}

function workspaceActionRow(children: VNode[]): VNode {
  return hl('div.action-menu__workspace-actions', children);
}

function workspaceAction(
  title: string,
  description: string,
  iconId: string,
  run: () => void,
  disabled = false,
): VNode {
  return hl(
    `button.action-menu__workspace-action${disabled ? '.disabled' : ''}`,
    {
      attrs: disabled
        ? { type: 'button', disabled: true, 'data-icon': iconId }
        : { type: 'button', 'data-icon': iconId },
      hook: bind('click', () => {
        if (!disabled) run();
      }),
    },
    [hl('strong', title), hl('span', description)],
  );
}
