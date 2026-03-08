import * as licon from 'lib/licon';
import { displayColumns } from 'lib/device';
import type { VNode, ToggleSettings } from 'lib/view';
import { bind, hl, toggle } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import { clamp } from 'lib/algo';

const ctrlToggle = (t: ToggleSettings, ctrl: AnalyseCtrl) => toggle(t, ctrl.redraw);

type BoardSettingsOpts = {
  closeOnChange?: boolean;
};

export function view(ctrl: AnalyseCtrl): VNode {
  return hl('div.action-menu', boardSettingsView(ctrl));
}

export function boardSettingsView(ctrl: AnalyseCtrl, opts: BoardSettingsOpts = {}): VNode[] {
  const closeOnChange = opts.closeOnChange ?? true;
  const closeMenu = () => {
    if (closeOnChange) ctrl.actionMenu.toggle();
  };

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

function renderVariationOpacitySlider(ctrl: AnalyseCtrl): VNode {
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
