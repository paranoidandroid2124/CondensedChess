import { prop, type Prop } from 'lib';
import type AnalyseCtrl from '../ctrl';
import { storedBooleanProp } from 'lib/storage';
import * as pgnExport from '../pgnExport';

export interface NarrativeResponse {
    analysis: any; // Define structure based on API response
    // e.g. "game_overview": string, "key_moments": ...
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
    content: Prop<string | null> = prop(null);
    error: Prop<string | null> = prop(null);
    needsLogin: Prop<boolean> = prop(false);

    constructor(readonly root: AnalyseCtrl) {
        this.enabled = storedBooleanProp('analyse.narrative.enabled', false);
    }

    loginHref = () => magicLinkHref();

    toggle = () => {
        this.enabled(!this.enabled());
        if (this.enabled() && !this.content() && !this.loading()) {
            this.fetchNarrative();
        }
        this.root.redraw();
    };

    fetchNarrative = async () => {
        this.loading(true);
        this.error(null);
        this.needsLogin(false);
        this.root.redraw();
        try {
            const pgn = pgnExport.renderFullTxt(this.root);

            // Basic eval extraction (optimistic)
            // We'd ideally want to traverse the tree and get evals for each move
            // But for now, let's just send the PGN and let the server handle re-eval 
            // or we rely on whatever evals are in the PGN comments if pgnExport includes them.
            // Actually pgnExport usually exports what's in the tree.

            const res = await fetch('/api/llm/game-analysis', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    pgn: pgn,
                    evals: [], // We can iterate tree to populate this if needed
                    options: { style: 'book', focusOn: ['mistakes', 'turning_points'] }
                })
            });

            if (res.ok) {
                const data = await res.json();
                // TODO: Parse into a nicer format
                this.content(JSON.stringify(data, null, 2));
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
