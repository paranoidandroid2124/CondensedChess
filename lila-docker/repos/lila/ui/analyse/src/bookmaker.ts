import { debounce } from 'lib/async';
import { storedBooleanPropWithEffect } from 'lib/storage';
import { pubsub } from 'lib/pubsub';
import type { CevalEngine, Work } from 'lib/ceval';
import type AnalyseCtrl from './ctrl';
import { initMiniBoards } from 'lib/view/miniBoard';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { uciToMove } from '@lichess-org/chessground/util';
import { treePath } from 'lib/tree';
import * as studyApi from './studyApi';
import { renderCreditWidget, renderInsufficientCredits } from './CreditWidget';
import type { CreditStatus } from './CreditWidget';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let handlersBound = false;
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

type BookmakerPreviewState = {
    cg?: CgApi;
    container?: HTMLElement;
};

let bookmakerPreview: BookmakerPreviewState = {};
let bookmakerPreviewOrientation: Color = 'white';

const bookmakerEvalDisplay = storedBooleanPropWithEffect('analyse.bookmaker.showEval', true, value => {
    const $scope = $('.analyse__bookmaker-text');
    $scope.find('.bookmaker-content').toggleClass('bookmaker-hide-eval', !value);
    $scope
        .find('.bookmaker-score-toggle')
        .attr('aria-pressed', value ? 'true' : 'false')
        .text(value ? 'Eval: On' : 'Eval: Off');
});

function applyBookmakerEvalDisplay(): void {
    const value = bookmakerEvalDisplay();
    const $scope = $('.analyse__bookmaker-text');
    $scope.find('.bookmaker-content').toggleClass('bookmaker-hide-eval', !value);
    $scope
        .find('.bookmaker-score-toggle')
        .attr('aria-pressed', value ? 'true' : 'false')
        .text(value ? 'Eval: On' : 'Eval: Off');
}

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
        .on('mouseover.bookmaker', '.analyse__bookmaker-text [data-board]', function (this: HTMLElement) {
            const board = this.dataset.board;
            if (board) updateBookmakerPreview(board);
        })
        .on('mouseleave.bookmaker', '.analyse__bookmaker-text', () => {
            hideBookmakerPreview();
        })
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
        })
        .on('click.bookmaker', '.analyse__bookmaker-text .bookmaker-score-toggle', e => {
            e.preventDefault();
            bookmakerEvalDisplay(!bookmakerEvalDisplay());
        });
}

function mountBookmakerPreview(root: HTMLElement): void {
    // Destroy previous chessground if the markup was replaced.
    try {
        bookmakerPreview.cg?.destroy?.();
    } catch {
        /* noop */
    }
    bookmakerPreview = {};

    const container = root.querySelector('.bookmaker-pv-preview') as HTMLElement | null;
    if (!container) return;

    container.classList.remove('is-active');
    container.innerHTML =
        '<div class="pv-board"><div class="pv-board-square"><div class="cg-wrap is2d"></div></div></div>';

    const wrap = container.querySelector('.cg-wrap') as HTMLElement | null;
    if (!wrap) return;

    bookmakerPreview.container = container;
    bookmakerPreview.cg = makeChessground(wrap, {
        fen: 'start',
        orientation: bookmakerPreviewOrientation,
        coordinates: false,
        viewOnly: true,
        drawable: { enabled: false, visible: false },
    });
}

function updateBookmakerPreview(board: string): void {
    const container = bookmakerPreview.container;
    const cg = bookmakerPreview.cg;
    if (!container || !cg) return;

    const parts = board.split('|');
    if (parts.length < 2) return;
    const fen = parts[0];
    const uci = parts[1] as Uci;
    if (!fen || !uci) return;

    container.classList.add('is-active');
    cg.set({
        fen,
        lastMove: uciToMove(uci),
        orientation: bookmakerPreviewOrientation,
        coordinates: false,
        viewOnly: true,
        drawable: { enabled: false, visible: false },
    });
}

function hideBookmakerPreview(): void {
    bookmakerPreview.container?.classList.remove('is-active');
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

    applyBookmakerEvalDisplay();
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

    const evalToVariations = (ceval: any, maxPvs: number): any[] | null => {
        if (!ceval || !Array.isArray(ceval.pvs)) return null;
        return ceval.pvs
            .filter((pv: any) => Array.isArray(pv?.moves) && pv.moves.length)
            .slice(0, maxPvs)
            .map((pv: any) => ({
                moves: pv.moves.slice(0, 40),
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
                    .filter((pv: any) => Array.isArray(pv?.moves) && pv.moves.length)
                    .slice(0, Math.max(1, Math.min(4, multiPv)))
                    .map((pv: any) => pv.moves.slice(0, 12))
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

    const handleCreditStatus = async () => {
        try {
            const res = await fetch('/api/llm/credits');
            if (res.ok) {
                const status = await res.json() as CreditStatus;
                $('.analyse__bookmaker .llm-credit-widget-container').html(renderCreditWidget(status));
            }
        } catch { /* noop */ }
    };

    const show = (html: string) => {
        lastShownHtml = html;
        $('.analyse__bookmaker').toggleClass('empty', !html);
        const $text = $('.analyse__bookmaker-text');
        bookmakerPreviewOrientation = ctrl?.getOrientation() ?? 'white';

        // Ensure credit container exists
        if (!$('.analyse__bookmaker .llm-credit-widget-container').length) {
            $('.analyse__bookmaker').prepend('<div class="llm-credit-widget-container"></div>');
        }

        $text.html(html);
        applyBookmakerEvalDisplay();
        if (html) {
            initMiniBoards($text[0] as HTMLElement);
            mountBookmakerPreview($text[0] as HTMLElement);
            handleCreditStatus();
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

            // Any new request cancels in-flight probe batches.
            activeProbeSession++;
            stopProbeEngine();
            const probeSession = activeProbeSession;

            lastRequestedFen = fen;
            try {
                // Stage 1 (briefing) intentionally disabled: it was low-signal and caused flicker.
                // Keep the panel responsive with a simple placeholder until the full commentary is ready.
                show('<div class="bookmaker-thinking-hud glass"><div class="hud-aura"></div><div class="hud-content"><i data-icon="L" class="hud-icon pulse"></i><span class="hud-text">Analyzing strategic depth…</span></div><div class="hud-shimmer"></div></div>');

                // Build analysis evidence for the move (or current position if no played move).
                // Prefer existing ceval if it already has enough MultiPV; otherwise run a dedicated search.
                const targetDepth = 20;
                const targetMultiPv = 5;
                const analysisTimeoutMs = 15000;

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

                // Delta support: also provide the post-move position so the server can compute before/after changes.
                const afterFen = playedMove ? fen : null;
                let afterEval: any = playedMove ? node.ceval : null;
                let afterVariations = afterFen ? evalToVariations(afterEval, 1) : null;
                // If the post-move ceval isn't ready yet, derive a PV from the analysed played-move line.
                // This avoids triggering an extra engine search just to compute a before/after delta.
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

                // Stage 2: Client-side Opening Explorer fetch (from user IP)
                let openingData = null;
                if (node.ply >= 1 && node.ply <= 30 && phaseOf(node.ply) === 'opening') {
                    try {
                        const explorerRes = await fetch(`https://explorer.lichess.ovh/masters?fen=${encodeURIComponent(analysisFen)}`);
                        if (explorerRes.ok) {
                            const raw = await explorerRes.json();
                            const topMoves = (raw.moves || []).map((m: any) => ({
                                uci: m.uci,
                                san: m.san,
                                total: (m.white || 0) + (m.draws || 0) + (m.black || 0),
                                white: m.white || 0,
                                draws: m.draws || 0,
                                black: m.black || 0,
                                performance: m.averageRating || 0
                            }));

                            // Fetch PGN snippets for the top 3 games to enable precedent commentary
                            const sampleGames = await Promise.all((raw.topGames || []).slice(0, 3).map(async (g: any) => {
                                let pgn = null;
                                try {
                                    const pgnRes = await fetch(`https://explorer.lichess.ovh/master/pgn/${g.id}`);
                                    if (pgnRes.ok) pgn = await pgnRes.text();
                                } catch (e) {
                                    // non-critical, some games might not have PGN
                                }
                                return {
                                    id: g.id,
                                    winner: g.winner,
                                    white: { name: g.white?.name || '?', rating: g.white?.rating || 0 },
                                    black: { name: g.black?.name || '?', rating: g.black?.rating || 0 },
                                    year: g.year || 0,
                                    month: g.month || 1,
                                    event: g.event,
                                    pgn: pgn
                                };
                            }));

                            openingData = {
                                eco: raw.opening?.eco,
                                name: raw.opening?.name,
                                totalGames: (raw.white || 0) + (raw.draws || 0) + (raw.black || 0),
                                topMoves,
                                sampleGames
                            };
                        }
                    } catch (e) {
                        console.warn('Bookmaker: failed to fetch client-side opening data', e);
                    }
                }

                // Stage 2: Full Deep Analysis
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

                if (lastRequestedFen !== fen) return;

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

                                if (probeSession !== activeProbeSession || lastRequestedFen !== fen) return;

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
                            } catch {
                                /* noop */
                            }
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
                    // Full analysis failed; remove the placeholder if still present.
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
        applyBookmakerEvalDisplay();
    }
}
