import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import { pubsub } from 'lib/pubsub';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;

// Initialize Bookmaker (AI commentary) interactive handlers
function initBookmaker(): void {
    const $bookmaker = $('.analyse__bookmaker-text');

    // Mini-board hover preview on .pv-line elements
    $bookmaker.on('mouseenter', '.pv-line', function (this: HTMLElement) {
        const fen = $(this).data('fen');
        const color = $(this).data('color') || 'white';
        const lastmove = $(this).data('lastmove');
        if (fen) {
            // Emit hover event for potential mini-board display
            pubsub.emit('analysis.bookmaker.hover', { fen, color, lastmove });
        }
    });

    $bookmaker.on('mouseleave', '.pv-line', function () {
        pubsub.emit('analysis.bookmaker.hover', null);
    });

    // Move chip click for board sync
    $bookmaker.on('click', '.move-chip', function (this: HTMLElement) {
        const uci = $(this).data('uci');
        const san = $(this).data('san');
        if (uci) {
            // Emit move event for board synchronization
            pubsub.emit('analysis.bookmaker.move', { uci, san });
        }
    });

    console.log('Bookmaker interactive handlers initialized');
}

export function bookmakerToggleBox() {
    $('#bookmaker-field').each(function (this: HTMLElement) {
        const box = this;
        if (box.dataset.toggleBoxInit) return;
        box.dataset.toggleBoxInit = '1';

        const state = storedBooleanPropWithEffect('analyse.bookmaker.display', true, value =>
            box.classList.toggle('toggle-box--toggle-off', !value),
        );

        const toggle = () => state(!state());

        if (!state()) box.classList.add('toggle-box--toggle-off');

        $(box)
            .children('legend')
            .on('click', toggle)
            .on('keypress', e => e.key === 'Enter' && toggle());
    });
}

export default function bookmakerNarrative(): BookmakerNarrative {
    initBookmaker();

    const cache = new Map<string, string>();
    const show = (html: string) => {
        $('.analyse__bookmaker').toggleClass('empty', !html);
        $('.analyse__bookmaker-text').html(html);
    };

    const phaseOf = (ply: number): string => {
        if (ply <= 16) return 'opening';
        if (ply <= 60) return 'middlegame';
        return 'endgame';
    };

    const loginHref = () =>
        `/auth/magic-link?referrer=${encodeURIComponent(location.pathname + location.search)}`;

    return debounce(
        async (nodes: Tree.Node[]) => {
            const node = nodes[nodes.length - 1];
            if (!node?.fen) return show('');

            const fen = node.fen;
            if (requestsBlocked) {
                if (blockedHtml) show(blockedHtml);
                return;
            }
            if (cache.has(fen)) return show(cache.get(fen)!);

            const ceval = node.ceval;
            const pv0 = ceval?.pvs?.[0];
            const evalData =
                ceval && (typeof ceval.cp === 'number' || typeof ceval.mate === 'number')
                    ? {
                        cp: typeof ceval.cp === 'number' ? ceval.cp : 0,
                        mate: typeof ceval.mate === 'number' ? ceval.mate : null,
                        pv: Array.isArray(pv0?.moves) ? pv0.moves : null,
                    }
                    : null;

            lastRequestedFen = fen;
            try {
                const res = await fetch('/api/llm/bookmaker-position', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        fen,
                        lastMove: node.uci || null,
                        eval: evalData,
                        context: {
                            opening: null,
                            phase: phaseOf(node.ply),
                            ply: node.ply,
                        },
                    }),
                });

                if (lastRequestedFen !== fen) return;

                if (res.ok) {
                    const data = await res.json();
                    const html = typeof data?.html === 'string' ? data.html : '';
                    cache.set(fen, html);
                    show(html);
                } else if (res.status === 401) {
                    blockedHtml =
                        `<p>Sign in to use Bookmaker.</p><p><a class="button" href="${loginHref()}">Sign in</a></p>`,
                    requestsBlocked = true;
                    show(blockedHtml);
                } else if (res.status === 429) {
                    try {
                        const data = await res.json();
                        const seconds = data?.ratelimit?.seconds;
                        if (typeof seconds === 'number') blockedHtml = `<p>LLM quota exceeded. Try again in ${seconds}s.</p>`;
                        else blockedHtml = '<p>LLM quota exceeded.</p>';
                    } catch {
                        blockedHtml = '<p>LLM quota exceeded.</p>';
                    }
                    requestsBlocked = true;
                    show(blockedHtml);
                } else {
                    show('');
                }
            } catch {
                show('');
            }
        },
        500,
        true,
    );
}

export function bookmakerClear() {
    $('.analyse__bookmaker').toggleClass('empty', true);
}
