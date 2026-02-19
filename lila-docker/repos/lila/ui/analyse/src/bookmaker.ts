import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import type AnalyseCtrl from './ctrl';
import { treePath } from 'lib/tree';
import { fetchOpeningReferenceViaProxy } from './bookmaker/openingProxy';
import { initBookmakerHandlers } from './bookmaker/interactionHandlers';
import { createProbeOrchestrator } from './bookmaker/probeOrchestrator';
import { clearBookmakerPanel, renderBookmakerPanel, restoreBookmakerPanel, syncBookmakerEvalDisplay } from './bookmaker/rendering';
import { buildBookmakerRequest, deriveAfterVariations, toBaselineCp, toEvalData } from './bookmaker/requestPayload';
import { blockedHtmlFromErrorResponse } from './bookmaker/blockingState';
import { flushBookmakerStudySyncQueue, rememberBookmakerStudySync } from './bookmaker/studySyncQueue';
import {
    commentaryFromResponse,
    htmlFromResponse,
    probeRequestsFromResponse,
    variationLinesFromResponse,
} from './bookmaker/responsePayload';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let activeProbeSession = 0;

export function flushBookmakerStudySync(ctrl: AnalyseCtrl): void {
    flushBookmakerStudySyncQueue(ctrl);
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
    const bookmakerEndpoint = '/api/llm/bookmaker-position';

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
            const syncStudy = (commentary: string, lines: any[]) => {
                if (!commentary) return;
                const payload = { commentPath, originPath, commentary, variations: lines };
                rememberBookmakerStudySync(payload);
                if (ctrl?.canWriteStudy()) ctrl.syncBookmaker(payload);
            };

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

                const afterFen = playedMove ? fen : null;
                let afterVariations = afterFen ? probes.evalToVariations(playedMove ? node.ceval : null, 1) : null;
                afterVariations = deriveAfterVariations(afterFen, afterVariations, playedMove, variations);
                const evalData = toEvalData(variations);

                const useAnalysisSurfaceV3 = document.body.dataset.brandV3AnalysisSurface !== '0';
                const useExplorerProxy = useAnalysisSurfaceV3 && document.body.dataset.brandExplorerProxy !== '0';
                const openingData = await fetchOpeningReferenceViaProxy(analysisFen, node.ply, useExplorerProxy);
                const initialPayload = buildBookmakerRequest({
                    fen: analysisFen,
                    lastMove: playedMove || null,
                    variations,
                    probeResults: null,
                    openingData,
                    afterFen,
                    afterVariations,
                    phase: phaseOf(node.ply),
                    ply: node.ply,
                });

                const res = await fetch(bookmakerEndpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(initialPayload),
                });

                if (!isCurrentSession()) return;

                if (res.ok) {
                    const data = await res.json();
                    const html = htmlFromResponse(data);
                    cache.set(fen, html);
                    show(html);

                    const commentary = commentaryFromResponse(data);
                    const vLines = variationLinesFromResponse(data, variations);
                    syncStudy(commentary, vLines);

                    const probeRequests = probeRequestsFromResponse(data);
                    const baselineCp = toBaselineCp(variations, evalData);

                    if (probeRequests.length && ctrl) {
                        void (async () => {
                            const probeResults = await probes.runProbes(probeRequests, baselineCp, probeSession);
                            if (!isCurrentSession()) return;
                            if (!probeResults.length) return;

                            try {
                                const refinedPayload = buildBookmakerRequest({
                                    fen: analysisFen,
                                    lastMove: playedMove || null,
                                    variations,
                                    probeResults,
                                    openingData,
                                    afterFen,
                                    afterVariations,
                                    phase: phaseOf(node.ply),
                                    ply: node.ply,
                                });
                                const refinedRes = await fetch(bookmakerEndpoint, {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify(refinedPayload),
                                });

                                if (!isCurrentSession()) return;

                                if (refinedRes.ok) {
                                    const refined = await refinedRes.json();
                                    const refinedHtml = htmlFromResponse(refined, html);
                                    cache.set(fen, refinedHtml);
                                    show(refinedHtml);

                                    const commentary = commentaryFromResponse(refined, commentaryFromResponse(data));
                                    const vLines = variationLinesFromResponse(refined, variationLinesFromResponse(data, variations));
                                    syncStudy(commentary, vLines);
                                }
                            } catch {}
                        })();
                    }
                } else {
                    const blocked = await blockedHtmlFromErrorResponse(res, loginHref());
                    if (!blocked) return show('');
                    blockedHtml = blocked;
                    requestsBlocked = true;
                    show(blockedHtml);
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
