import { prop, type Prop } from 'lib';
import type AnalyseCtrl from '../ctrl';
import { storedBooleanProp } from 'lib/storage';
import * as pgnExport from '../pgnExport';

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

export interface GameNarrativeResponse {
    schema: string;
    intro: string;
    moments: GameNarrativeMoment[];
    conclusion: string;
    themes: string[];
}

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

    fetchNarrative = async () => {
        this.loading(true);
        this.error(null);
        this.needsLogin(false);
        this.pvBoard(null);
        this.root.redraw();
        try {
            const pgn = pgnExport.renderFullTxt(this.root);

            const evals = extractMoveEvals(this.root);

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
            this.root.redraw();
        }
    }
}

export function make(root: AnalyseCtrl): NarrativeCtrl {
    return new NarrativeCtrl(root);
}

function extractMoveEvals(ctrl: AnalyseCtrl): any[] {
    const evals: any[] = [];

    for (const node of ctrl.mainline) {
        // Skip the initial position (ply 0) to match PGN ply numbering
        if (node.ply < 1) continue;
        const ev: any = node.ceval || node.eval;
        if (!ev) continue;

        const cp = typeof ev.cp === 'number' ? ev.cp : 0;
        const mate = typeof ev.mate === 'number' ? ev.mate : null;
        const depth = typeof ev.depth === 'number' ? ev.depth : 0;

        const pvs: any[] = Array.isArray(ev.pvs) ? ev.pvs : [];
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
                    depth,
                };
            })
            .filter(Boolean);

        evals.push({
            ply: node.ply,
            cp,
            mate,
            pv: variations?.[0]?.moves ?? [],
            variations,
        });
    }

    return evals;
}
