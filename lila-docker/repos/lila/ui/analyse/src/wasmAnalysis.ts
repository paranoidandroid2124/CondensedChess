import type AnalyseCtrl from './ctrl';
import { treeOps } from 'lib/tree';
// import { opposite } from 'chessops/util';

export interface MoveEval {
    ply: number;
    fen: string;
    eval: {
        cp?: number;
        mate?: number;
        best?: string;
        variation?: string;
    };
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

function announce(msg: string): void {
    const site = (window as any).site;
    if (site?.announce) site.announce({ msg });
    else alert(msg);
}

export class WasmAnalysis {

    private isRunning = false;
    private currentStep = 0;
    private totalSteps = 0;
    private results: MoveEval[] = [];

    constructor(readonly ctrl: AnalyseCtrl) { }

    start = async () => {
        if (this.isRunning) return;

        // 1. Check if server has cached analysis
        const hasCache = await this.checkServerCache();
        if (hasCache) {
            console.log('Using cached analysis from server.');
            this.fetchAndDisplayAnalysis();
            return;
        }

        if (!confirm("Deep Analysis uses your device CPU. It may take roughly 30s-1m. Proceed?")) return;

        this.isRunning = true;
        this.results = [];
        this.ctrl.ceval.stop();

        // 2. Prepare the traverse path (Mainline)
        const mainline = treeOps.mainlineNodeList(this.ctrl.tree.root);
        this.totalSteps = mainline.length;
        this.currentStep = 0;

        // 3. Start Loop
        try {
            for (const node of mainline) {
                if (!this.isRunning) break;
                this.currentStep++;

                // Update UI Progress (Simple implementation for now)
                this.ctrl.redraw();

                // Navigate to node
                // this.ctrl.userJump(treeOps.nodePath(node)); 
                this.ctrl.jumpToMain(node.ply);

                // Analyze Node
                const evalData = await this.analyzeCurrentNode();
                if (evalData) this.results.push(evalData);
            }

            // 4. Send to Server
            if (this.isRunning) {
                await this.submitResults();
            }

        } catch (e) {
            console.error("WASM Analysis failed", e);
            announce("Analysis failed.");
        } finally {
            this.isRunning = false;
            this.ctrl.redraw();
        }
    };

    stop = () => {
        this.isRunning = false;
        this.ctrl.ceval.stop();
    };

    private analyzeCurrentNode = async (): Promise<MoveEval | null> => {
        // 1. Try cloud eval first (disabled)
        // Optimization (when enabled): Mostly useful for openings (Ply < 30) or common positions
        if (this.ctrl.node.ply < 40) {
            const cloud = await this.fetchCloudEval(this.ctrl.node.fen);
            if (cloud) return cloud;
        }

        // 2. Fallback to WASM
        return new Promise((resolve) => {
            const targetDepth = 18;
            this.ctrl.startCeval();

            const checkInterval = setInterval(() => {
                const currentEval = this.ctrl.node.ceval;
                if (!this.isRunning) {
                    clearInterval(checkInterval);
                    resolve(null);
                    return;
                }

                if (currentEval && (currentEval.depth >= targetDepth || currentEval.mate)) {
                    clearInterval(checkInterval);

                    const bestMove = currentEval.pvs && currentEval.pvs[0] ? currentEval.pvs[0].moves[0] : undefined;
                    const bestLine = currentEval.pvs && currentEval.pvs[0] ? currentEval.pvs[0].moves.join(' ') : undefined;

                    resolve({
                        ply: this.ctrl.node.ply,
                        fen: this.ctrl.node.fen,
                        eval: {
                            cp: currentEval.cp,
                            mate: currentEval.mate,
                            best: bestMove,
                            variation: bestLine
                        }
                    });
                }
            }, 200);
        });
    };

    private fetchCloudEval = async (fen: string): Promise<MoveEval | null> => {
        // Chesstory: Disable third-party cloud eval. Use local/WASM evaluation only.
        void fen;
        return null;
    };

    private checkServerCache = async (): Promise<boolean> => {
        try {
            // Placeholder
            return false;
        } catch (e) { return false; }
    };

    private submitResults = async () => {

        // We pass the PGN string if possible, or assume GameId logic handled by backend
        // Since backend expects "pgn", we might need to fetch the PGN export from ctrl
        const pgnExport = await this.getPgn();

        const body = {
            pgn: pgnExport,
            evals: this.results,
            options: { style: "comprehensive", focusOn: ["mistakes", "turning_points"] }
        };

        const res = await fetch('/api/llm/game-analysis', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (res.ok) {
            const json = await res.json();
            if (this.ctrl.opts.bookmaker) {
                announce("Analysis integration: " + (json.summary || "Done"));
            }
        } else if (res.status === 401) {
            announce('Login required to use AI analysis.');
        } else if (res.status === 429) {
            try {
                const data = await res.json();
                const seconds = data?.ratelimit?.seconds;
                if (typeof seconds === 'number') announce(`LLM quota exceeded. Try again in ${formatSeconds(seconds)}.`);
                else announce('LLM quota exceeded.');
            } catch {
                announce('LLM quota exceeded.');
            }
        } else {
            announce("Server failed to process analysis.");
        }
    };

    private fetchAndDisplayAnalysis = async () => {
        // Stub
    };

    private getPgn = async (): Promise<string> => {
        // ctrl.pgnExport might need import or usage
        // Attempt to find PGN wrapper
        // If we can't find it easily, return GameId and trust Backend can load PGN from DB
        // But verify LlmApi.analyzeFullGame signature
        return this.ctrl.data.game.id;
    }

    get progress() {
        return this.isRunning ? Math.round((this.currentStep / this.totalSteps) * 100) : 0;
    }
}
