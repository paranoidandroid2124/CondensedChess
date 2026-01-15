import type { VNode } from 'snabbdom';
import type { NarrativeCtrl } from './narrativeCtrl';
import { hl, bind, dataIcon } from 'lib/view';
import * as licon from 'lib/licon';

export function narrativeView(ctrl: NarrativeCtrl): VNode | null {
    if (!ctrl.enabled()) return null;

    return hl('div.narrative-box', {
        hook: {
            insert: vnode => ((vnode.elm as HTMLElement).scrollTop = 0)
        }
    }, [
        hl('div.narrative-header', [
            hl('h2', [
                hl('span.icon', { attrs: { ...dataIcon(licon.Book) } }),
                'Narrative Analysis'
            ]),
            hl('button.button.button-empty.text', {
                attrs: { 'aria-label': 'Close' },
                hook: bind('click', ctrl.toggle, ctrl.root.redraw)
            }, hl('span', { attrs: { ...dataIcon(licon.X) } }))
        ]),
        hl('div.narrative-content', [
            ctrl.loading() ? hl('div.loader', 'Consulting the Oracle...') :
                ctrl.error() ? hl('div.error', [
                    hl('div', ctrl.error()),
                    ctrl.needsLogin() ? hl('a.button', { attrs: { href: ctrl.loginHref() } }, 'Sign in') : null
                ]) :
                    hl('pre.narrative-text', ctrl.content() || 'No narrative generated yet.')
        ]),
        !ctrl.loading() && !ctrl.content() && hl('div.actions', [
            hl('button.button.action', {
                hook: bind('click', ctrl.fetchNarrative, ctrl.root.redraw)
            }, 'Generate Commentary')
        ])
    ]);
}
