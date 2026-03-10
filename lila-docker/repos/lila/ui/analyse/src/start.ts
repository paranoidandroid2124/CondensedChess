import makeCtrl from './ctrl';
import menuHover from 'lib/menuHover';
import makeView from './view/main';
import type { AnalyseApi, AnalyseOpts } from './interfaces';
import type { VNode } from 'snabbdom';


export default function (
  patch: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode,
) {
  return function (opts: AnalyseOpts): AnalyseApi {
    opts.element = document.querySelector('main.analyse') as HTMLElement;

    const view = makeView();
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

    return {
      socketReceive: ctrl.socket.receive,
      path: () => ctrl.path,
      setChapter(_id: string) { },
    };
  };
}
