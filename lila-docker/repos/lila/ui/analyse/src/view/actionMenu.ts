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
        'a',
        {
          hook: bind('click', () => {
            ctrl.flip();
            closeMenu();
            ctrl.redraw();
          }),
          attrs: { 'data-icon': licon.ChasingArrows, title: 'Hotkey: f' },
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
        change(v) {
          ctrl.treeView.modePreference(v ? 'inline' : 'column');
          closeMenu();
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
    renderVariationOpacitySlider(ctrl),
  ].filter(Boolean) as VNode[];
}

function boardWorkspaceView(ctrl: AnalyseCtrl, closeMenu: () => void): VNode[] {
  const engineUnavailable = !ctrl.isCevalAllowed();
  return [
    workspaceSection('View', 'Shape how the board and move stream read at a glance.', [
      workspaceChoiceCard(
        'Board labels',
        'Choose whether coordinates stay off, frame the rim, or label every square.',
        [
          { key: 'off', label: 'Off' },
          { key: 'rim', label: 'Rim' },
          { key: 'full', label: 'Full' },
        ],
        ctrl.boardLabelMode(),
        mode => {
          ctrl.setBoardLabelMode(mode as 'off' | 'rim' | 'full');
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'Material sidebar',
        'Keep captured material visible beside the player strips.',
        ctrl.showCapturedMaterial(),
        next => {
          ctrl.setShowCapturedMaterial(next);
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'Inline move list',
        'Show the move history as a flowing line instead of stacked columns.',
        ctrl.treeView.modePreference() === 'inline',
        next => {
          ctrl.treeView.modePreference(next ? 'inline' : 'column');
          ctrl.redraw();
          closeMenu();
        },
      ),
    ]),
    workspaceSection('Guides', 'Tune how much engine and annotation scaffolding stays on screen.', [
      workspaceSwitchCard(
        'Engine panel',
        engineUnavailable ? 'Engine guidance is unavailable in this position.' : 'Show engine lines beside the move tree.',
        ctrl.showCeval(),
        next => {
          ctrl.showCeval(next);
          ctrl.redraw();
          closeMenu();
        },
        engineUnavailable,
      ),
      workspaceSwitchCard(
        'Eval gauge',
        'Keep the side gauge visible whenever engine analysis is on and space allows.',
        ctrl.showGauge(),
        next => {
          ctrl.setShowEvalGauge(next);
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'On-board cues',
        'Draw move annotations and review cues directly over the board.',
        ctrl.possiblyShowMoveAnnotationsOnBoard(),
        next => {
          ctrl.togglePossiblyShowMoveAnnotationsOnBoard(next);
          closeMenu();
        },
      ),
      workspaceSwitchCard(
        'Variation controls',
        'Show expand and collapse handles inside the move tree.',
        ctrl.disclosureMode(),
        next => {
          ctrl.disclosureMode(next);
          ctrl.redraw();
          closeMenu();
        },
      ),
      workspaceSliderCard(
        'Line emphasis',
        'Fade or strengthen variation arrows and branch traces.',
        renderVariationOpacityRange(ctrl, 'Line emphasis'),
      ),
    ]),
    workspaceSection('Orientation', 'Reset perspective without leaving the review shell.', [
      workspaceActionRow([
        workspaceAction(
          'Flip board',
          'Swap sides instantly.',
          licon.ChasingArrows,
          () => {
            ctrl.flip();
            closeMenu();
          },
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
  return renderVariationOpacityRange(ctrl, 'Variation opacity');
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
      attrs: disabled ? { type: 'button', disabled: true, 'data-icon': iconId } : { type: 'button', 'data-icon': iconId },
      hook: bind('click', () => {
        if (!disabled) run();
      }),
    },
    [hl('strong', title), hl('span', description)],
  );
}
