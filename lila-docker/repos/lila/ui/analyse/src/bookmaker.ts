import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import type AnalyseCtrl from './ctrl';
import { treePath } from 'lib/tree';
import * as studyApi from './studyApi';
import { renderInsufficientCredits } from './CreditWidget';
import { fetchOpeningReferenceViaProxy } from './bookmaker/openingProxy';
import { initBookmakerHandlers } from './bookmaker/interactionHandlers';
import type { ProbeRequest } from './bookmaker/types';
import { createProbeOrchestrator } from './bookmaker/probeOrchestrator';
import { clearBookmakerPanel, renderBookmakerPanel, restoreBookmakerPanel, syncBookmakerEvalDisplay } from './bookmaker/rendering';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let activeProbeSession = 0;

type PendingBookmakerStudySync = {
    payload: studyApi.BookmakerSyncPayload;
    savedAt: number;
};

const pendingBookmakerStudySync = new Map<string, PendingBookmakerStudySync>();
const maxPendingBookmakerSync = 200;

function rememberBookmakerStudySync(payload: studyApi.BookmakerSyncPayload): void {
    const key = payload.commentPath;
    pendingBookmakerStudySync.set(key, { payload, savedAt: Date.now() });

    if (pendingBookmakerStudySync.size <= maxPendingBookmakerSync) return;

    const oldest = [...pendingBookmakerStudySync.entries()].sort((a, b) => a[1].savedAt - b[1].savedAt);
    for (const [k] of oldest.slice(0, pendingBookmakerStudySync.size - maxPendingBookmakerSync)) pendingBookmakerStudySync.delete(k);
}

export function flushBookmakerStudySync(ctrl: AnalyseCtrl): void {
    if (!ctrl?.canWriteStudy()) return;

    const entries = [...pendingBookmakerStudySync.values()].sort((a, b) => a.savedAt - b.savedAt);
    if (!entries.length) return;

    for (const entry of entries) ctrl.syncBookmaker(entry.payload);

    pendingBookmakerStudySync.clear();
}

const bookmakerEvalDisplay = storedBooleanPropWithEffect('analyse.bookmaker.showEval', true, value => {
    syncBookmakerEvalDisplay(value);
});

export function bookmakerToggleBox() {
    initBookmakerHandlers(() => bookmakerEvalDisplay(!bookmakerEvalDisplay()));

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

    syncBookmakerEvalDisplay(bookmakerEvalDisplay());
    bookmakerRestore();
}

export default function bookmakerNarrative(ctrl?: AnalyseCtrl): BookmakerNarrative {
    const cache = new Map<string, string>();
    const probes = createProbeOrchestrator(ctrl, session => session === activeProbeSession);

    const show = (html: string) => {
        lastShownHtml = html;
        renderBookmakerPanel(html, ctrl?.getOrientation() ?? 'white', bookmakerEvalDisplay());
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
            const prevNode = nodes.length >= 2 ? nodes[nodes.length - 2] : undefined;
            const playedMove = typeof node.uci === 'string' && prevNode?.fen ? node.uci : null;
            const analysisFen = playedMove ? prevNode!.fen : fen;
            const analysisCeval = playedMove ? prevNode?.ceval : node.ceval;
            const commentPath = ctrl?.path ?? '';
            const originPath = playedMove ? treePath.init(commentPath) : commentPath;

            if (requestsBlocked) {
                if (blockedHtml) show(blockedHtml);
                return;
            }
            if (cache.has(fen)) return show(cache.get(fen)!);

            activeProbeSession++;
            probes.stop();
            const probeSession = activeProbeSession;
            const isCurrentSession = () => probeSession === activeProbeSession && lastRequestedFen === fen;

            lastRequestedFen = fen;
            try {
                show('<div class="bookmaker-thinking-hud glass"><div class="hud-aura"></div><div class="hud-content"><i data-icon="L" class="hud-icon pulse"></i><span class="hud-text">Analyzing strategic depth…</span></div><div class="hud-shimmer"></div></div>');

                const targetDepth = 20;
                const targetMultiPv = 5;
                const analysisTimeoutMs = 15000;

                let analysisEval: any = analysisCeval;
                let variations = probes.evalToVariations(analysisEval, targetMultiPv);
                if ((!variations || variations.length < targetMultiPv) && ctrl) {
                    analysisEval = await probes.runPositionEval(analysisFen, targetDepth, analysisTimeoutMs, targetMultiPv, probeSession);
                    if (!isCurrentSession()) return;
                    variations = probes.evalToVariations(analysisEval, targetMultiPv);
                }

                if (playedMove && variations && !variations.some(v => Array.isArray(v.moves) && v.moves[0] === playedMove) && ctrl) {
                    const playedEv = await probes.runProbeEval(analysisFen, playedMove, targetDepth, 5000, 1, probeSession);
                    if (!isCurrentSession()) return;
                    if (playedEv) {
                        const replyPv = Array.isArray(playedEv.pvs) ? playedEv.pvs[0]?.moves : null;
                        const playedVar = {
                            moves: [playedMove, ...(Array.isArray(replyPv) ? replyPv.slice(0, 28) : [])],
                            scoreCp: typeof playedEv.cp === 'number' ? playedEv.cp : 0,
                            mate: typeof playedEv.mate === 'number' ? playedEv.mate : null,
                            depth: typeof playedEv.depth === 'number' ? playedEv.depth : targetDepth,
                        };
                        variations = [...variations, playedVar].slice(0, targetMultiPv + 1);
                    }
                }

                const evalData =
                    variations && variations.length
                        ? {
                            cp: typeof variations[0].scoreCp === 'number' ? variations[0].scoreCp : 0,
                            mate: variations[0].mate ?? null,
                            pv: Array.isArray(variations[0].moves) ? variations[0].moves : null,
                        }
                        : null;

                const afterFen = playedMove ? fen : null;
                let afterEval: any = playedMove ? node.ceval : null;
                let afterVariations = afterFen ? probes.evalToVariations(afterEval, 1) : null;
                if (afterFen && (!afterVariations || !afterVariations.length) && playedMove && Array.isArray(variations)) {
                    const playedLine = variations.find(v => Array.isArray(v?.moves) && v.moves[0] === playedMove);
                    if (playedLine) {
                        const tail = Array.isArray(playedLine.moves) ? playedLine.moves.slice(1) : [];
                        afterVariations = [
                            {
                                moves: tail.slice(0, 40),
                                scoreCp: typeof playedLine.scoreCp === 'number' ? playedLine.scoreCp : 0,
                                mate: typeof playedLine.mate === 'number' ? playedLine.mate : null,
                                depth: typeof playedLine.depth === 'number' ? playedLine.depth : 0,
                            },
                        ];
                    }
                }
                const afterEvalData =
                    afterVariations && afterVariations.length
                        ? {
                            cp: typeof afterVariations[0].scoreCp === 'number' ? afterVariations[0].scoreCp : 0,
                            mate: afterVariations[0].mate ?? null,
                            pv: Array.isArray(afterVariations[0].moves) ? afterVariations[0].moves : null,
                        }
                        : null;

                const useAnalysisSurfaceV3 = document.body.dataset.brandV3AnalysisSurface !== '0';
                const useExplorerProxy = useAnalysisSurfaceV3 && document.body.dataset.brandExplorerProxy !== '0';
                const openingData = await fetchOpeningReferenceViaProxy(analysisFen, node.ply, useExplorerProxy);

                const res = await fetch('/api/llm/bookmaker-position', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        fen: analysisFen,
                        lastMove: playedMove || null,
                        eval: evalData,
                        variations,
                        probeResults: null,
                        openingData,
                        afterFen,
                        afterEval: afterEvalData,
                        afterVariations,
                        context: {
                            opening: null,
                            phase: phaseOf(node.ply),
                            ply: node.ply,
                        },
                    }),
                });

                if (!isCurrentSession()) return;

                if (res.ok) {
                    const data = await res.json();
                    const html = typeof data?.html === 'string' ? data.html : '';
                    cache.set(fen, html);
                    show(html);

                    const commentary = typeof data?.commentary === 'string' ? (data.commentary as string) : '';
                    const vLines = Array.isArray(data?.variations) ? (data.variations as any[]) : variations || [];
                    const payload = { commentPath, originPath, commentary, variations: vLines };
                    if (commentary) rememberBookmakerStudySync(payload);
                    if (ctrl?.canWriteStudy() && commentary) ctrl.syncBookmaker(payload);

                    const probeRequests = Array.isArray(data?.probeRequests) ? (data.probeRequests as ProbeRequest[]) : [];
                    const baselineCp =
                        typeof variations?.[0]?.scoreCp === 'number'
                            ? variations[0].scoreCp
                            : typeof evalData?.cp === 'number'
                                ? evalData.cp
                                : 0;

                    if (probeRequests.length && ctrl) {
                        void (async () => {
                            const probeResults = await probes.runProbes(probeRequests, baselineCp, probeSession);
                            if (!isCurrentSession()) return;
                            if (!probeResults.length) return;

                            try {
                                const refinedRes = await fetch('/api/llm/bookmaker-position', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        fen: analysisFen,
                                        lastMove: playedMove || null,
                                        eval: evalData,
                                        variations,
                                        probeResults,
                                        openingData,
                                        afterFen,
                                        afterEval: afterEvalData,
                                        afterVariations,
                                        context: {
                                            opening: null,
                                            phase: phaseOf(node.ply),
                                            ply: node.ply,
                                        },
                                    }),
                                });

                                if (!isCurrentSession()) return;

                                if (refinedRes.ok) {
                                    const refined = await refinedRes.json();
                                    const refinedHtml = typeof refined?.html === 'string' ? refined.html : html;
                                    cache.set(fen, refinedHtml);
                                    show(refinedHtml);

                                    const commentary =
                                        typeof refined?.commentary === 'string'
                                            ? (refined.commentary as string)
                                            : typeof data?.commentary === 'string'
                                                ? (data.commentary as string)
                                                : '';
                                    const vLines = Array.isArray(refined?.variations)
                                        ? (refined.variations as any[])
                                        : Array.isArray(data?.variations)
                                            ? (data.variations as any[])
                                            : variations || [];
                                    const payload = { commentPath, originPath, commentary, variations: vLines };
                                    if (commentary) rememberBookmakerStudySync(payload);
                                    if (ctrl?.canWriteStudy() && commentary) ctrl.syncBookmaker(payload);
                                }
                            } catch {}
                        })();
                    }
                } else if (res.status === 403) {
                    try {
                        const data = await res.json();
                        blockedHtml = renderInsufficientCredits(data.resetAt);
                    } catch {
                        blockedHtml = renderInsufficientCredits('Unknown');
                    }
                    requestsBlocked = true;
                    show(blockedHtml);
                } else if (res.status === 401) {
                    blockedHtml = `<p>Sign in to use Bookmaker.</p><p><a class="button" href="${loginHref()}">Sign in</a></p>`;
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
    lastShownHtml = '';
    clearBookmakerPanel();
}

export function bookmakerRestore(): void {
    restoreBookmakerPanel(lastShownHtml, bookmakerEvalDisplay());
}
