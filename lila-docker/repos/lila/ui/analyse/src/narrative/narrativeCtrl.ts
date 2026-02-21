import { prop, type Prop } from 'lib';
import type AnalyseCtrl from '../ctrl';
import { storedBooleanProp } from 'lib/storage';
import * as pgnExport from '../pgnExport';
import type { CevalEngine, Work } from 'lib/ceval';

interface VariationLine {
    moves: string[];
    scoreCp: number;
    mate?: number | null;
    depth?: number;
    tags?: string[];
}

interface GameNarrativeMoment {
    ply: number;
    momentType: string;
    fen: string;
    narrative: string;
    concepts: string[];
    variations: VariationLine[];
}

interface GameNarrativeReview {
    totalPlies: number;
    evalCoveredPlies: number;
    evalCoveragePct: number;
    selectedMoments: number;
    selectedMomentPlies: number[];
}

export interface GameNarrativeResponse {
    schema: string;
    intro: string;
    moments: GameNarrativeMoment[];
    conclusion: string;
    themes: string[];
    review?: GameNarrativeReview;
}

const AUTO_EVAL_DEPTH = 12;
const AUTO_EVAL_MULTI_PV = 2;
const AUTO_EVAL_PER_PLY_TIMEOUT_MS = 450;
const AUTO_EVAL_MAX_BUDGET_MS = 25000;
const AUTO_EVAL_MAX_PLY_SCAN = 120;

function magicLinkHref(): string {
    return `/auth/magic-link?referrer=${encodeURIComponent(location.pathname + location.search)}`;
}

function formatSeconds(totalSeconds: number): string {
    const seconds = Math.max(0, Math.floor(totalSeconds));
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) return `${hours}h ${mins}m`;
    if (minutes > 0) return `${minutes}m`;
    return `${seconds}s`;
}

export class NarrativeCtrl {
    enabled: Prop<boolean>;
    loading: Prop<boolean> = prop(false);
    data: Prop<GameNarrativeResponse | null> = prop(null);
    error: Prop<string | null> = prop(null);
    needsLogin: Prop<boolean> = prop(false);
    loadingDetail: Prop<string | null> = prop(null);

    pvBoard: Prop<{ fen: string; uci: string } | null> = prop(null);

    constructor(readonly root: AnalyseCtrl) {
        this.enabled = storedBooleanProp('analyse.narrative.enabled', false);
    }

    loginHref = () => magicLinkHref();

    toggle = () => {
        this.enabled(!this.enabled());
        if (this.enabled() && !this.data() && !this.loading()) {
            this.fetchNarrative();
        }
        this.root.redraw();
    };

    openAndFetch = async () => {
        if (!this.enabled()) this.enabled(true);
        if (this.loading()) {
            this.root.redraw();
            return;
        }
        try {
            await this.fetchNarrative();
        } finally {
            this.root.redraw();
        }
    };

    fetchNarrative = async () => {
        this.loading(true);
        this.error(null);
        this.needsLogin(false);
        this.pvBoard(null);
        this.loadingDetail('Deep analysis prep: collecting PGN and existing eval...');
        this.root.redraw();
        try {
            const pgn = pgnExport.renderFullTxt(this.root);

            const evals = await extractMoveEvals(this.root, detail => {
                this.loadingDetail(detail);
                this.root.redraw();
            });

            const res = await fetch('/api/llm/game-analysis-local', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    pgn: pgn,
                    evals,
                    options: { style: 'book', focusOn: ['mistakes', 'turning_points'] }
                })
            });

            if (res.ok) {
                const data = await res.json();
                this.data(data as GameNarrativeResponse);
            } else if (res.status === 401) {
                this.needsLogin(true);
                this.error('Login required to use AI commentary.');
            } else if (res.status === 429) {
                try {
                    const data = await res.json();
                    const seconds = data?.ratelimit?.seconds;
                    if (typeof seconds === 'number') this.error(`LLM quota exceeded. Try again in ${formatSeconds(seconds)}.`);
                    else this.error('LLM quota exceeded.');
                } catch {
                    this.error('LLM quota exceeded.');
                }
            } else {
                const txt = await res.text();
                this.error("Error fetching narrative: " + res.status + " " + txt);
            }
        } catch (e) {
            console.error(e);
            this.error("Error: " + e);
        } finally {
            this.loading(false);
            this.loadingDetail(null);
            this.root.redraw();
        }
    }
}

export function make(root: AnalyseCtrl): NarrativeCtrl {
    return new NarrativeCtrl(root);
}

async function extractMoveEvals(
    ctrl: AnalyseCtrl,
    onProgress?: (detail: string) => void,
): Promise<any[]> {
    const evals: any[] = [];
    const byPly = new Map<number, any>();
    const nodes = ctrl.mainline.filter(node => node.ply >= 1);

    for (const node of nodes) {
        const raw = node.ceval || node.eval;
        if (!raw) continue;
        const normalized = normalizeEval(node.ply, raw);
        byPly.set(node.ply, normalized);
    }

    const missing = nodes.filter(node => !byPly.has(node.ply));
    if (missing.length) {
        const budgetMs = Math.min(AUTO_EVAL_MAX_BUDGET_MS, Math.max(7000, missing.length * 260));
        const missingSlice = missing.slice(0, AUTO_EVAL_MAX_PLY_SCAN);
        onProgress?.(`Deep scan: evaluating ${missingSlice.length} missing plies (may take up to ${(budgetMs / 1000).toFixed(0)}s)...`);
        const enriched = await enrichMissingEvalsWithWasm(ctrl, missingSlice, budgetMs, onProgress);
        for (const item of enriched) byPly.set(item.ply, item.eval);
    }

    for (const node of nodes) {
        const ev = byPly.get(node.ply);
        if (ev) evals.push(ev);
    }

    onProgress?.(`Deep scan complete: eval coverage ${byPly.size}/${nodes.length} plies.`);
    return evals;
}

function normalizeEval(ply: number, raw: any): any {
    const cp = typeof raw?.cp === 'number' ? raw.cp : 0;
    const mate = typeof raw?.mate === 'number' ? raw.mate : null;
    const depth = typeof raw?.depth === 'number' ? raw.depth : 0;

    const pvs: any[] = Array.isArray(raw?.pvs) ? raw.pvs : [];
    const variations = pvs
        .map(pv => {
            const moves = Array.isArray(pv?.moves)
                ? pv.moves
                : typeof pv?.moves === 'string'
                    ? pv.moves.trim().split(/\s+/).filter(Boolean)
                    : [];
            if (!moves.length) return null;
            return {
                moves,
                scoreCp: typeof pv?.cp === 'number' ? pv.cp : cp,
                mate: typeof pv?.mate === 'number' ? pv.mate : mate,
                depth: typeof pv?.depth === 'number' ? pv.depth : depth,
            };
        })
        .filter(Boolean);

    return {
        ply,
        cp,
        mate,
        pv: variations?.[0]?.moves ?? [],
        variations,
    };
}

async function enrichMissingEvalsWithWasm(
    ctrl: AnalyseCtrl,
    missingNodes: Tree.Node[],
    totalBudgetMs: number,
    onProgress?: (detail: string) => void,
): Promise<Array<{ ply: number; eval: any }>> {
    const enriched: Array<{ ply: number; eval: any }> = [];
    if (!missingNodes.length) return enriched;

    let engine: CevalEngine | undefined;
    try {
        engine = ctrl.ceval.engines.make({ variant: ctrl.data.game.variant.key });
    } catch {
        return enriched;
    }
    if (!engine) return enriched;

    const startedAt = Date.now();
    try {
        for (const [idx, node] of missingNodes.entries()) {
            if (Date.now() - startedAt >= totalBudgetMs) break;
            if (!node?.fen || typeof node.fen !== 'string') continue;
            const ev = await runNodeEval(engine, ctrl, node, AUTO_EVAL_DEPTH, AUTO_EVAL_MULTI_PV);
            if (!ev) continue;
            enriched.push({ ply: node.ply, eval: normalizeEval(node.ply, ev) });
            if ((idx + 1) % 6 === 0 || idx + 1 === missingNodes.length) {
                onProgress?.(`Deep scan progress: ${idx + 1}/${missingNodes.length} plies checked.`);
            }
        }
    } finally {
        try {
            engine.stop();
        } catch {}
        try {
            engine.destroy();
        } catch {}
    }

    return enriched;
}

async function runNodeEval(
    engine: CevalEngine,
    ctrl: AnalyseCtrl,
    node: Tree.Node,
    depth: number,
    multiPv: number,
): Promise<Tree.LocalEval | null> {
    try {
        engine.stop();
    } catch {}

    return await new Promise<Tree.LocalEval | null>(resolve => {
        let best: Tree.LocalEval | null = null;
        let done = false;
        const finish = () => {
            if (done) return;
            done = true;
            clearTimeout(timer);
            try {
                engine.stop();
            } catch {}
            resolve(best);
        };
        const timer = setTimeout(finish, AUTO_EVAL_PER_PLY_TIMEOUT_MS);

        const work: Work = {
            variant: ctrl.data.game.variant.key,
            threads: 1,
            hashSize: 16,
            gameId: undefined,
            stopRequested: false,
            path: `narrative-auto:${node.ply}`,
            search: { depth },
            multiPv,
            ply: node.ply,
            threatMode: false,
            initialFen: node.fen,
            currentFen: node.fen,
            moves: [],
            emit: (ev: Tree.LocalEval) => {
                best = ev;
                const pvCount = Array.isArray(ev.pvs)
                    ? ev.pvs.filter(pv => Array.isArray(pv?.moves) && pv.moves.length).length
                    : 0;
                if (ev.depth >= depth && pvCount >= 1) finish();
            },
        };

        try {
            engine.start(work);
        } catch {
            finish();
        }
    });
}
