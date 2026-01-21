import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import { pubsub } from 'lib/pubsub';
import type { CevalEngine, Work } from 'lib/ceval';
import type AnalyseCtrl from './ctrl';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let handlersBound = false;
let activeProbeSession = 0;

type ProbeRequest = {
    id: string;
    fen: string;
    moves: string[];
    depth: number;
    purpose?: string;
    questionId?: string;
    questionKind?: string;
    multiPv?: number;
    baselineEvalCp?: number;
    baselineMove?: string;
    baselineMate?: number | null;
    baselineDepth?: number;
};

type ProbeResult = {
    id: string;
    fen?: string;
    evalCp: number;
    bestReplyPv: string[];
    replyPvs?: string[][];
    deltaVsBaseline: number;
    keyMotifs: string[];
    purpose?: string;
    questionId?: string;
    questionKind?: string;
    probedMove?: string;
    mate?: number | null;
    depth?: number;
};

function initBookmakerHandlers(): void {
    if (handlersBound) return;
    handlersBound = true;

    // Use delegated handlers so Bookmaker remains functional across Snabbdom redraws.
    $(document)
        .on('mouseenter.bookmaker', '.analyse__bookmaker-text .pv-line', function (this: HTMLElement) {
            const fen = $(this).data('fen');
            const color = $(this).data('color') || 'white';
            const lastmove = $(this).data('lastmove');
            if (fen) pubsub.emit('analysis.bookmaker.hover', { fen, color, lastmove });
        })
        .on('mouseleave.bookmaker', '.analyse__bookmaker-text .pv-line', () => {
            pubsub.emit('analysis.bookmaker.hover', null);
        })
        .on('click.bookmaker', '.analyse__bookmaker-text .move-chip', function (this: HTMLElement) {
            const uci = $(this).data('uci');
            const san = $(this).data('san');
            if (uci) pubsub.emit('analysis.bookmaker.move', { uci, san });
        });
}

export function bookmakerToggleBox() {
    initBookmakerHandlers();

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

    bookmakerRestore();
}

export default function bookmakerNarrative(ctrl?: AnalyseCtrl): BookmakerNarrative {
    const cache = new Map<string, string>();
    let probeEngine: CevalEngine | undefined;

    const ensureProbeEngine = (): CevalEngine | undefined => {
        if (!ctrl) return;
        try {
            probeEngine ??= ctrl.ceval.engines.make({ variant: ctrl.data.game.variant.key });
            return probeEngine;
        } catch {
            return;
        }
    };

    const stopProbeEngine = () => {
        try {
            probeEngine?.stop();
        } catch {
            /* noop */
        }
    };

    const probeWorkPlyAfterMove = (fen: string): number => (fen.includes(' w ') ? 1 : 0);
    const probeWorkPlyAtFen = (fen: string): number => (fen.includes(' w ') ? 0 : 1);

    const runPositionEval = async (
        fen: string,
        depth: number,
        timeoutMs: number,
        multiPv: number,
        session: number,
    ): Promise<Tree.LocalEval | null> => {
        const engine = ensureProbeEngine();
        if (!engine) return null;

        engine.stop();

        return await new Promise<Tree.LocalEval | null>(resolve => {
            let best: Tree.LocalEval | null = null;
            let done = false;

            const finish = () => {
                if (done) return;
                done = true;
                clearTimeout(timer);
                try {
                    engine.stop();
                } catch {
                    /* noop */
                }
                resolve(best);
            };

            const timer = setTimeout(finish, timeoutMs);

            const work: Work = {
                variant: ctrl?.data.game.variant.key ?? 'standard',
                threads: 1,
                hashSize: 16,
                gameId: undefined,
                stopRequested: false,
                path: `bookmaker-eval:${session}`,
                search: { depth },
                multiPv,
                ply: probeWorkPlyAtFen(fen),
                threatMode: false,
                initialFen: fen,
                currentFen: fen,
                moves: [],
                emit: (ev: Tree.LocalEval) => {
                    if (session !== activeProbeSession) return finish();
                    best = ev;
                    const pvCount = Array.isArray(ev.pvs) ? ev.pvs.filter(pv => Array.isArray(pv?.moves) && pv.moves.length).length : 0;
                    if (ev.depth >= depth && pvCount >= multiPv) finish();
                },
            };

            engine.start(work);
        });
    };

    const evalToEvalData = (ceval: any): any | null => {
        const pv0 = ceval?.pvs?.[0];
        if (!ceval || (!Array.isArray(ceval?.pvs) && typeof ceval?.cp !== 'number' && typeof ceval?.mate !== 'number'))
            return null;
        return {
            cp: typeof ceval.cp === 'number' ? ceval.cp : 0,
            mate: typeof ceval.mate === 'number' ? ceval.mate : null,
            pv: Array.isArray(pv0?.moves) ? pv0.moves.slice(0, 24) : null,
        };
    };

    const evalToVariations = (ceval: any, maxPvs: number): any[] | null => {
        if (!ceval || !Array.isArray(ceval.pvs)) return null;
        return ceval.pvs
            .filter(pv => Array.isArray(pv?.moves) && pv.moves.length)
            .slice(0, maxPvs)
            .map(pv => ({
                moves: pv.moves.slice(0, 24),
                scoreCp: typeof pv.cp === 'number' ? pv.cp : 0,
                mate: typeof pv.mate === 'number' ? pv.mate : null,
                depth: typeof pv.depth === 'number' ? pv.depth : typeof ceval.depth === 'number' ? ceval.depth : 0,
            }));
    };

    const runProbeEval = async (
        fen: string,
        move: string,
        depth: number,
        timeoutMs: number,
        multiPv: number,
        session: number,
    ): Promise<Tree.LocalEval | null> => {
        const engine = ensureProbeEngine();
        if (!engine) return null;

        engine.stop();

        return await new Promise<Tree.LocalEval | null>(resolve => {
            let best: Tree.LocalEval | null = null;
            let done = false;

            const finish = () => {
                if (done) return;
                done = true;
                clearTimeout(timer);
                try {
                    engine.stop();
                } catch {
                    /* noop */
                }
                resolve(best);
            };

            const timer = setTimeout(finish, timeoutMs);

            const work: Work = {
                variant: ctrl?.data.game.variant.key ?? 'standard',
                threads: 1,
                hashSize: 16,
                gameId: undefined,
                stopRequested: false,
                path: `bookmaker-probe:${session}:${move}`,
                search: { depth },
                multiPv,
                ply: probeWorkPlyAfterMove(fen),
                threatMode: false,
                initialFen: fen,
                currentFen: fen,
                moves: [move],
                emit: (ev: Tree.LocalEval) => {
                    if (session !== activeProbeSession) return finish();
                    best = ev;
                    if (ev.depth >= depth && Array.isArray(ev.pvs) && ev.pvs[0]?.moves?.length) finish();
                },
            };

            engine.start(work);
        });
    };

    const runProbes = async (
        probeRequests: ProbeRequest[],
        baselineEvalCp: number,
        session: number,
    ): Promise<ProbeResult[]> => {
        const highEffort =
            probeRequests.some(pr => typeof pr.purpose === 'string' && pr.purpose.length) ||
            probeRequests.some(pr => typeof pr.multiPv === 'number' && pr.multiPv >= 3) ||
            probeRequests.some(pr => typeof pr.depth === 'number' && pr.depth >= 20);

        const maxEffort =
            probeRequests.some(pr => pr.purpose === 'free_tempo_branches') ||
            probeRequests.some(pr => pr.purpose === 'latent_plan_refutation') ||
            probeRequests.some(pr => pr.purpose === 'recapture_branches') ||
            probeRequests.some(pr => pr.purpose === 'keep_tension_branches') ||
            probeRequests.some(pr => pr.purpose === 'convert_reply_multipv') ||
            probeRequests.some(pr => pr.purpose === 'defense_reply_multipv');

        const maxProbeMoves = maxEffort ? 16 : highEffort ? 10 : 6;
        const totalBudgetMs = maxEffort ? 35000 : highEffort ? 20000 : 8000;

        const flattened = probeRequests
            .flatMap(pr => (Array.isArray(pr.moves) ? pr.moves.map(m => ({ pr, move: m })) : []))
            .slice(0, maxProbeMoves); // Hard cap: keep probes bounded

        if (!flattened.length) return [];

        const perMoveBudget = Math.max(
            highEffort ? 1000 : 700,
            Math.min(highEffort ? 5000 : 2000, Math.floor(totalBudgetMs / flattened.length)),
        );

        const results: ProbeResult[] = [];

        for (const { pr, move } of flattened) {
            if (session !== activeProbeSession) break;
            if (!move) continue;

            const baseCp = typeof pr.baselineEvalCp === 'number' ? pr.baselineEvalCp : baselineEvalCp;
            const depth = typeof pr.depth === 'number' && pr.depth > 0 ? pr.depth : 20;
            const multiPv = typeof pr.multiPv === 'number' && pr.multiPv > 0 ? pr.multiPv : 2;

            const ev = await runProbeEval(pr.fen, move, depth, perMoveBudget, multiPv, session);
            if (!ev || session !== activeProbeSession) continue;

            const replyPvs = Array.isArray(ev.pvs)
                ? ev.pvs
                    .filter(pv => Array.isArray(pv?.moves) && pv.moves.length)
                    .slice(0, Math.max(1, Math.min(4, multiPv)))
                    .map(pv => pv.moves.slice(0, 12))
                : [];

            const bestReplyPv = replyPvs[0] ?? [];
            const evalCp = typeof ev.cp === 'number' ? ev.cp : 0;

            results.push({
                id: pr.id,
                fen: pr.fen,
                evalCp,
                bestReplyPv,
                replyPvs: replyPvs.length ? replyPvs : undefined,
                deltaVsBaseline: evalCp - (typeof baseCp === 'number' ? baseCp : 0),
                keyMotifs: [],
                purpose: typeof pr.purpose === 'string' ? pr.purpose : undefined,
                questionId: typeof pr.questionId === 'string' ? pr.questionId : undefined,
                questionKind: typeof pr.questionKind === 'string' ? pr.questionKind : undefined,
                probedMove: move,
                mate: typeof ev.mate === 'number' ? ev.mate : undefined,
                depth: typeof ev.depth === 'number' ? ev.depth : undefined,
            });
        }

        return results;
    };

    const show = (html: string) => {
        lastShownHtml = html;
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
            const prevNode = nodes.length >= 2 ? nodes[nodes.length - 2] : undefined;
            const playedMove = typeof node.uci === 'string' && prevNode?.fen ? node.uci : null;
            const analysisFen = playedMove ? prevNode!.fen : fen;
            const analysisCeval = playedMove ? prevNode?.ceval : node.ceval;

            if (requestsBlocked) {
                if (blockedHtml) show(blockedHtml);
                return;
            }
            if (cache.has(fen)) return show(cache.get(fen)!);

            const briefingEvalData = evalToEvalData(analysisCeval);

            // Any new request cancels in-flight probe batches.
            activeProbeSession++;
            stopProbeEngine();
            const probeSession = activeProbeSession;

            lastRequestedFen = fen;
            try {
                // Stage 1: Fast Briefing
                const briefingRes = await fetch('/api/llm/bookmaker-briefing', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        fen: analysisFen,
                        lastMove: playedMove || null,
                        eval: briefingEvalData,
                        context: {
                            opening: null,
                            phase: phaseOf(node.ply),
                            ply: node.ply,
                        },
                    }),
                });

                if (lastRequestedFen !== fen) return;

                if (briefingRes.ok) {
                    const data = await briefingRes.json();
                    const briefingHtml = typeof data?.html === 'string' ? data.html : '';
                    show(`<div class="bookmaker-briefing">${briefingHtml}</div><div class="bookmaker-thinking">AI is deep thinking...</div>`);
                }

                // Build analysis evidence for the move (or current position if no played move).
                // Prefer existing ceval if it already has enough MultiPV; otherwise run a dedicated search.
                const targetDepth = 18;
                const targetMultiPv = 3;
                const analysisTimeoutMs = 10000;

                let analysisEval: any = analysisCeval;
                let variations = evalToVariations(analysisEval, targetMultiPv);
                if ((!variations || variations.length < targetMultiPv) && ctrl) {
                    analysisEval = await runPositionEval(analysisFen, targetDepth, analysisTimeoutMs, targetMultiPv, probeSession);
                    if (probeSession !== activeProbeSession || lastRequestedFen !== fen) return;
                    variations = evalToVariations(analysisEval, targetMultiPv);
                }

                // Ensure the played move is analyzable even if it isn't in top MultiPV (book-style annotation).
                if (playedMove && variations && !variations.some(v => Array.isArray(v.moves) && v.moves[0] === playedMove) && ctrl) {
                    const playedEv = await runProbeEval(analysisFen, playedMove, targetDepth, 5000, 1, probeSession);
                    if (probeSession !== activeProbeSession || lastRequestedFen !== fen) return;
                    if (playedEv) {
                        const replyPv = Array.isArray(playedEv.pvs) ? playedEv.pvs[0]?.moves : null;
                        const playedVar = {
                            moves: [playedMove, ...(Array.isArray(replyPv) ? replyPv.slice(0, 16) : [])],
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

                // Stage 2: Full Deep Analysis
                const res = await fetch('/api/llm/bookmaker-position', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        fen: analysisFen,
                        lastMove: playedMove || null,
                        eval: evalData,
                        variations,
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

                    const probeRequests = Array.isArray(data?.probeRequests) ? (data.probeRequests as ProbeRequest[]) : [];
                    const baselineCp =
                        typeof variations?.[0]?.scoreCp === 'number'
                            ? variations[0].scoreCp
                            : typeof evalData?.cp === 'number'
                                ? evalData.cp
                                : 0;

                    if (probeRequests.length && ctrl) {
                        void (async () => {
                            const probeResults = await runProbes(probeRequests, baselineCp, probeSession);
                            if (probeSession !== activeProbeSession || lastRequestedFen !== fen) return;
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
                                        context: {
                                            opening: null,
                                            phase: phaseOf(node.ply),
                                            ply: node.ply,
                                        },
                                    }),
                                });

                                if (probeSession !== activeProbeSession || lastRequestedFen !== fen) return;

                                if (refinedRes.ok) {
                                    const refined = await refinedRes.json();
                                    const refinedHtml = typeof refined?.html === 'string' ? refined.html : html;
                                    cache.set(fen, refinedHtml);
                                    show(refinedHtml);
                                }
                            } catch {
                                /* noop */
                            }
                        })();
                    }
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
                    // If full analysis fails, at least we keep the briefing if we had it
                    $('.bookmaker-thinking').remove();
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
    $('.analyse__bookmaker').toggleClass('empty', true);
}

export function bookmakerRestore(): void {
    const $text = $('.analyse__bookmaker-text');
    if (!$text.length) return;
    if (!$text.html() && lastShownHtml) {
        $('.analyse__bookmaker').toggleClass('empty', !lastShownHtml);
        $text.html(lastShownHtml);
    }
}
