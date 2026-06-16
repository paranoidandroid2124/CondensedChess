import makeCtrl from './ctrl';
import menuHover from 'lib/menuHover';
import makeView from './view/main';
import type { AnalyseOpts } from './interfaces';
import type { VNode } from 'snabbdom';


export default function (
  patch: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode,
) {
  return function (opts: AnalyseOpts): void {
    opts.element = document.querySelector('main.analyse') as HTMLElement;

    const view = makeView();
    // redraw closes over ctrl before construction completes; keep this as a guarded late assignment.
    // eslint-disable-next-line prefer-const
    let ctrl: InstanceType<typeof makeCtrl> | undefined;
    let vnode: VNode | Element | DocumentFragment = opts.element;

    function redraw() {
      if (!ctrl) return;
      vnode = patch(vnode, view(ctrl));
    }

    ctrl = (site.analysis = new makeCtrl(opts, redraw));

    const blueprint = view(ctrl);
    opts.element.innerHTML = '';
    vnode = patch(opts.element, blueprint);

    menuHover();

  };
}
