import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import type AnalyseCtrl from './ctrl';
import { treePath } from 'lib/tree';
import { fetchOpeningReferenceViaProxy } from './bookmaker/openingProxy';
import { initBookmakerHandlers, setBookmakerRefs } from './bookmaker/interactionHandlers';
import { createProbeOrchestrator } from './bookmaker/probeOrchestrator';
import { clearBookmakerPanel, renderBookmakerPanel, restoreBookmakerPanel, syncBookmakerEvalDisplay } from './bookmaker/rendering';
import { buildBookmakerRequest, deriveAfterVariations, toBaselineCp, toEvalData } from './bookmaker/requestPayload';
import { blockedHtmlFromErrorResponse } from './bookmaker/blockingState';
import { flushBookmakerStudySyncQueue, rememberBookmakerStudySync } from './bookmaker/studySyncQueue';
import {
    type BookmakerRefsV1,
    type PolishMetaV1,
    cacheHitFromResponse,
    commentaryFromResponse,
    htmlFromResponse,
    latentPlansFromResponse,
    mainStrategicPlansFromResponse,
    modelFromResponse,
    planStateTokenFromResponse,
    polishMetaFromResponse,
    probeRequestsFromResponse,
    refsFromResponse,
    sourceModeFromResponse,
    variationLinesFromResponse,
    whyAbsentFromTopMultiPVFromResponse,
} from './bookmaker/responsePayload';
import type { PlanStateToken } from './bookmaker/types';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

type BookmakerCacheEntry = {
    html: string;
    refs: BookmakerRefsV1 | null;
    polishMeta: PolishMetaV1 | null;
    sourceMode: string | null;
    model: string | null;
    cacheHit: boolean | null;
    mainPlansCount: number;
    latentPlansCount: number;
    holdReasonsCount: number;
};

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let activeProbeSession = 0;

type LoadingStage = 'position' | 'lines' | 'compose' | 'polish';

const loadingStageOrder: Record<LoadingStage, number> = {
    position: 1,
    lines: 2,
    compose: 3,
    polish: 4,
};

const loadingStageTitle: Record<LoadingStage, string> = {
    position: 'Position analysis',
    lines: 'Line analysis',
    compose: 'Draft generation',
    polish: 'Language polish',
};

const loadingStageMessages: Record<LoadingStage, string[]> = {
    position: [
        'Reading the position...',
        'Checking king safety and piece activity...',
        'Dusting off the board for a clean read...',
    ],
    lines: [
        'Calculating principal variations...',
        'Comparing tactical and positional routes...',
        'Following forcing lines one by one...',
    ],
    compose: [
        'Building a concise explanation...',
        'Aligning plans with the current structure...',
        'Turning engine signals into human guidance...',
    ],
    polish: [
        'Polishing the final commentary...',
        'Ensuring move order and notation stay exact...',
        'Preparing a clean final render...',
    ],
};

function escapeHtml(raw: string): string {
    return raw
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function renderLoadingHud(stage: LoadingStage, message: string, streamPreview?: string): string {
    const step = loadingStageOrder[stage];
    const title = loadingStageTitle[stage];
    const isReveal = typeof streamPreview === 'string';
    const safeMessage = escapeHtml(message);
    const safeStream = isReveal ? escapeHtml(streamPreview || '') : '';

    return `
      <div class="bookmaker-thinking-hud glass${isReveal ? ' bookmaker-thinking-hud--reveal' : ''}">
        <div class="hud-aura"></div>
        <div class="hud-content">
          <span class="hud-stage">Step ${step}/4 · ${escapeHtml(title)}</span>
          <i data-icon="L" class="hud-icon pulse"></i>
          <span class="hud-text">${safeMessage}</span>
          ${isReveal ? `<span class="hud-stream">${safeStream}<span class="hud-caret">|</span></span>` : ''}
        </div>
        <div class="hud-shimmer"></div>
      </div>
    `;
}

function splitWords(text: string): string[] {
    return text
        .split(/\s+/)
        .map(t => t.trim())
        .filter(Boolean);
}

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
    const cache = new Map<string, BookmakerCacheEntry>();
    const planStateByPath = new Map<string, PlanStateToken | null>();
    const probes = createProbeOrchestrator(ctrl, session => session === activeProbeSession);
    const bookmakerEndpoint = '/api/llm/bookmaker-position';
    let loadingTicker: number | null = null;

    const canonicalize = (value: unknown): string => {
        if (Array.isArray(value)) return `[${value.map(canonicalize).join(',')}]`;
        if (value && typeof value === 'object') {
            const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) => a.localeCompare(b));
            return `{${entries.map(([k, v]) => `${JSON.stringify(k)}:${canonicalize(v)}`).join(',')}}`;
        }
        return JSON.stringify(value);
    };

    const tokenHash = (token: PlanStateToken | null): string => {
        if (!token) return '-';
        const str = canonicalize(token);
        let h = 2166136261;
        for (let i = 0; i < str.length; i++) {
            h ^= str.charCodeAt(i);
            h = Math.imul(h, 16777619);
        }
        return (h >>> 0).toString(16);
    };

    const cacheKeyOf = (fen: string, originPath: string, token: PlanStateToken | null): string =>
        `${fen}|${originPath}|${tokenHash(token)}`;
    const stateKeyOf = (originPath: string, analysisFen: string): string =>
        `${originPath}|${analysisFen}`;

    const show = (html: string, remember = true) => {
        if (remember) lastShownHtml = html;
        renderBookmakerPanel(html, ctrl?.getOrientation() ?? 'white', bookmakerEvalDisplay());
    };

    const applyMetaToRoot = (
        sourceMode: string | null,
        model: string | null,
        cacheHit: boolean | null,
        polishMeta: PolishMetaV1 | null,
    ) => {
        const root = document.querySelector('.analyse__bookmaker-text');
        if (!root) return;
        if (sourceMode) root.setAttribute('data-llm-source-mode', sourceMode);
        else root.removeAttribute('data-llm-source-mode');
        if (model) root.setAttribute('data-llm-model', model);
        else root.removeAttribute('data-llm-model');
        if (cacheHit !== null) root.setAttribute('data-llm-cache-hit', String(cacheHit));
        else root.removeAttribute('data-llm-cache-hit');
        if (polishMeta) {
            root.setAttribute('data-llm-polish-provider', polishMeta.provider);
            root.setAttribute('data-llm-polish-phase', polishMeta.validationPhase);
            if (polishMeta.model) root.setAttribute('data-llm-polish-model', polishMeta.model);
            else root.removeAttribute('data-llm-polish-model');
            root.setAttribute('data-llm-polish-source', polishMeta.sourceMode);
            root.setAttribute('data-llm-polish-cache-hit', String(polishMeta.cacheHit));
            if (polishMeta.validationReasons.length)
                root.setAttribute('data-llm-polish-reasons', polishMeta.validationReasons.join(','));
            else root.removeAttribute('data-llm-polish-reasons');
            if (polishMeta.strategyCoverage) {
                const s = polishMeta.strategyCoverage;
                root.setAttribute('data-llm-strategy-mode', s.mode);
                root.setAttribute('data-llm-strategy-score', s.coverageScore.toFixed(2));
                root.setAttribute('data-llm-strategy-covered', String(s.coveredCategories));
                root.setAttribute('data-llm-strategy-required', String(s.requiredCategories));
                root.setAttribute('data-llm-strategy-pass', String(s.passesThreshold));
                root.setAttribute('data-llm-strategy-plan', `${s.planHits}/${s.planSignals}`);
                root.setAttribute('data-llm-strategy-route', `${s.routeHits}/${s.routeSignals}`);
                root.setAttribute('data-llm-strategy-focus', `${s.focusHits}/${s.focusSignals}`);
            } else {
                root.removeAttribute('data-llm-strategy-mode');
                root.removeAttribute('data-llm-strategy-score');
                root.removeAttribute('data-llm-strategy-covered');
                root.removeAttribute('data-llm-strategy-required');
                root.removeAttribute('data-llm-strategy-pass');
                root.removeAttribute('data-llm-strategy-plan');
                root.removeAttribute('data-llm-strategy-route');
                root.removeAttribute('data-llm-strategy-focus');
            }
        } else {
            root.removeAttribute('data-llm-polish-provider');
            root.removeAttribute('data-llm-polish-phase');
            root.removeAttribute('data-llm-polish-model');
            root.removeAttribute('data-llm-polish-source');
            root.removeAttribute('data-llm-polish-cache-hit');
            root.removeAttribute('data-llm-polish-reasons');
            root.removeAttribute('data-llm-strategy-mode');
            root.removeAttribute('data-llm-strategy-score');
            root.removeAttribute('data-llm-strategy-covered');
            root.removeAttribute('data-llm-strategy-required');
            root.removeAttribute('data-llm-strategy-pass');
            root.removeAttribute('data-llm-strategy-plan');
            root.removeAttribute('data-llm-strategy-route');
            root.removeAttribute('data-llm-strategy-focus');
        }
    };

    const applyStrategicMetaToRoot = (mainPlansCount: number, latentPlansCount: number, holdReasonsCount: number) => {
        const root = document.querySelector('.analyse__bookmaker-text');
        if (!root) return;
        root.setAttribute('data-llm-main-plans-count', String(mainPlansCount));
        root.setAttribute('data-llm-latent-plans-count', String(latentPlansCount));
        root.setAttribute('data-llm-hold-reasons-count', String(holdReasonsCount));
    };

    const stopLoadingTicker = () => {
        if (loadingTicker !== null) {
            window.clearInterval(loadingTicker);
            loadingTicker = null;
        }
    };

    const setLoadingStage = (stage: LoadingStage, isCurrentSession: () => boolean) => {
        stopLoadingTicker();
        const messages = loadingStageMessages[stage];
        let index = 0;
        const draw = () => {
            if (!isCurrentSession()) {
                stopLoadingTicker();
                return;
            }
            const message = messages[index % messages.length];
            show(renderLoadingHud(stage, message), false);
            index++;
        };
        draw();
        if (messages.length > 1) loadingTicker = window.setInterval(draw, 1150);
    };

    const streamReveal = async (commentary: string, isCurrentSession: () => boolean) => {
        const words = splitWords(commentary);
        if (words.length < 16 || !isCurrentSession()) return;
        const steps = Math.max(6, Math.min(18, Math.floor(words.length / 8)));
        const totalMs = Math.max(420, Math.min(1200, words.length * 10));
        const stepMs = Math.max(30, Math.floor(totalMs / steps));

        for (let step = 1; step <= steps; step++) {
            if (!isCurrentSession()) return;
            const take = Math.max(1, Math.floor((words.length * step) / steps));
            const preview = words.slice(0, take).join(' ');
            show(renderLoadingHud('polish', 'Streaming final commentary...', preview), false);
            await new Promise<void>(resolve => window.setTimeout(resolve, stepMs));
        }
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
            if (!node?.fen) {
                setBookmakerRefs(null);
                return show('');
            }

            const fen = node.fen;
            const prevNode = nodes.length >= 2 ? nodes[nodes.length - 2] : undefined;
            const playedMove = typeof node.uci === 'string' && prevNode?.fen ? node.uci : null;
            const analysisFen = playedMove ? prevNode!.fen : fen;
            const analysisCeval = playedMove ? prevNode?.ceval : node.ceval;
            const commentPath = ctrl?.path ?? '';
            const originPath = playedMove ? treePath.init(commentPath) : commentPath;
            const stateKey = stateKeyOf(originPath, analysisFen);
            const syncStudy = (commentary: string, lines: any[]) => {
                if (!commentary) return;
                const payload = { commentPath, originPath, commentary, variations: lines };
                rememberBookmakerStudySync(payload);
                if (ctrl?.canWriteStudy()) ctrl.syncBookmaker(payload);
            };

            if (requestsBlocked) {
                if (blockedHtml) show(blockedHtml);
                setBookmakerRefs(null);
                return;
            }
            const requestToken = planStateByPath.get(stateKey) ?? null;
            const cacheKey = cacheKeyOf(fen, originPath, requestToken);
            const cached = cache.get(cacheKey);
            if (cached) {
                setBookmakerRefs(cached.refs);
                show(cached.html);
                applyMetaToRoot(cached.sourceMode, cached.model, cached.cacheHit, cached.polishMeta);
                applyStrategicMetaToRoot(cached.mainPlansCount, cached.latentPlansCount, cached.holdReasonsCount);
                return;
            }

            activeProbeSession++;
            probes.stop();
            const probeSession = activeProbeSession;
            const isCurrentSession = () => probeSession === activeProbeSession && lastRequestedFen === fen;

            lastRequestedFen = fen;
            try {
                setLoadingStage('position', isCurrentSession);

                const targetDepth = 20;
                const targetMultiPv = 5;
                const analysisTimeoutMs = 15000;

                let analysisEval: any = analysisCeval;
                let variations = probes.evalToVariations(analysisEval, targetMultiPv);
                if ((!variations || variations.length < targetMultiPv) && ctrl) {
                    setLoadingStage('lines', isCurrentSession);
                    analysisEval = await probes.runPositionEval(analysisFen, targetDepth, analysisTimeoutMs, targetMultiPv, probeSession);
                    if (!isCurrentSession()) {
                        stopLoadingTicker();
                        return;
                    }
                    variations = probes.evalToVariations(analysisEval, targetMultiPv);
                }

                if (playedMove && variations && !variations.some(v => Array.isArray(v.moves) && v.moves[0] === playedMove) && ctrl) {
                    setLoadingStage('lines', isCurrentSession);
                    const playedEv = await probes.runProbeEval(analysisFen, playedMove, targetDepth, 5000, 1, probeSession);
                    if (!isCurrentSession()) {
                        stopLoadingTicker();
                        return;
                    }
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
                setLoadingStage('compose', isCurrentSession);
                const openingData = await fetchOpeningReferenceViaProxy(analysisFen, node.ply, useExplorerProxy);
                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
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
                    planStateToken: requestToken,
                });

                setLoadingStage('polish', isCurrentSession);
                const res = await fetch(bookmakerEndpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(initialPayload),
                });

                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
                stopLoadingTicker();

                if (res.ok) {
                    const data = await res.json();
                    const emittedToken = planStateTokenFromResponse(data);
                    if (emittedToken) planStateByPath.set(stateKey, emittedToken);
                    else planStateByPath.delete(stateKey);
                    const html = htmlFromResponse(data);
                    const sourceMode = sourceModeFromResponse(data);
                    const model = modelFromResponse(data);
                    const cacheHit = cacheHitFromResponse(data);
                    const refs = refsFromResponse(data);
                    const polishMeta = polishMetaFromResponse(data);
                    const commentary = commentaryFromResponse(data);
                    const mainStrategicPlans = mainStrategicPlansFromResponse(data);
                    const latentPlans = latentPlansFromResponse(data);
                    const holdReasons = whyAbsentFromTopMultiPVFromResponse(data);
                    const shouldStream = sourceMode === 'llm_polished' && commentary.length > 0;

                    if (shouldStream) {
                        await streamReveal(commentary, isCurrentSession);
                        if (!isCurrentSession()) {
                            stopLoadingTicker();
                            return;
                        }
                    }

                    const initialEntry: BookmakerCacheEntry = {
                        html,
                        refs,
                        polishMeta,
                        sourceMode,
                        model,
                        cacheHit,
                        mainPlansCount: mainStrategicPlans.length,
                        latentPlansCount: latentPlans.length,
                        holdReasonsCount: holdReasons.length,
                    };
                    cache.set(cacheKey, initialEntry);
                    setBookmakerRefs(refs);
                    show(html);
                    applyMetaToRoot(sourceMode, model, cacheHit, polishMeta);
                    applyStrategicMetaToRoot(mainStrategicPlans.length, latentPlans.length, holdReasons.length);

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
                                const refinedToken = planStateByPath.get(stateKey) ?? requestToken;
                                const refinedCacheKey = cacheKeyOf(fen, originPath, refinedToken);
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
                                    planStateToken: refinedToken,
                                });
                                const refinedRes = await fetch(bookmakerEndpoint, {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify(refinedPayload),
                                });

                                if (!isCurrentSession()) return;

                                if (refinedRes.ok) {
                                    const refined = await refinedRes.json();
                                    const emittedRefinedToken = planStateTokenFromResponse(refined);
                                    if (emittedRefinedToken) planStateByPath.set(stateKey, emittedRefinedToken);
                                    else planStateByPath.delete(stateKey);
                                    const refinedHtml = htmlFromResponse(refined, html);
                                    const refinedSourceMode = sourceModeFromResponse(refined);
                                    const refinedModel = modelFromResponse(refined);
                                    const refinedCacheHit = cacheHitFromResponse(refined);
                                    const refinedRefs = refsFromResponse(refined);
                                    const refinedPolishMeta = polishMetaFromResponse(refined);
                                    const refinedMainPlans = mainStrategicPlansFromResponse(refined);
                                    const refinedLatentPlans = latentPlansFromResponse(refined);
                                    const refinedHoldReasons = whyAbsentFromTopMultiPVFromResponse(refined);
                                    const refinedEntry: BookmakerCacheEntry = {
                                        html: refinedHtml,
                                        refs: refinedRefs,
                                        polishMeta: refinedPolishMeta,
                                        sourceMode: refinedSourceMode,
                                        model: refinedModel,
                                        cacheHit: refinedCacheHit,
                                        mainPlansCount: refinedMainPlans.length,
                                        latentPlansCount: refinedLatentPlans.length,
                                        holdReasonsCount: refinedHoldReasons.length,
                                    };
                                    cache.set(refinedCacheKey, refinedEntry);
                                    setBookmakerRefs(refinedRefs);
                                    show(refinedHtml);
                                    applyMetaToRoot(refinedSourceMode, refinedModel, refinedCacheHit, refinedPolishMeta);
                                    applyStrategicMetaToRoot(
                                        refinedMainPlans.length,
                                        refinedLatentPlans.length,
                                        refinedHoldReasons.length,
                                    );

                                    const commentary = commentaryFromResponse(refined, commentaryFromResponse(data));
                                    const vLines = variationLinesFromResponse(refined, variationLinesFromResponse(data, variations));
                                    syncStudy(commentary, vLines);
                                }
                            } catch {}
                        })();
                    }
                } else {
                    const blocked = await blockedHtmlFromErrorResponse(res, loginHref());
                    if (!blocked) {
                        setBookmakerRefs(null);
                        return show('');
                    }
                    blockedHtml = blocked;
                    requestsBlocked = true;
                    setBookmakerRefs(null);
                    show(blockedHtml);
                }
            } catch {
                stopLoadingTicker();
                setBookmakerRefs(null);
                show('');
            }
        },
        500,
        true,
    );
}

export function bookmakerClear() {
    lastShownHtml = '';
    setBookmakerRefs(null);
    clearBookmakerPanel();
}

export function bookmakerRestore(): void {
    setBookmakerRefs(null);
    restoreBookmakerPanel(lastShownHtml, bookmakerEvalDisplay());
}
