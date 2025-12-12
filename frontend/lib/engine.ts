export type EngineMessage = {
  bestMove?: string;
  cp?: number;
  depth?: number;
  pv?: string;
  mate?: number;
  multipv?: number;
  error?: string; // Error message for crash/failure
};

type EngineState = "IDLE" | "ANALYZING" | "STOPPING" | "CRASHED";

const MAX_RESTART_ATTEMPTS = 3;
const RESTART_DELAY_MS = 500;

export class StockfishEngine {
  private worker: Worker | null = null;
  private state: EngineState = "IDLE";
  private pendingTask: { fen: string; options: { depth?: number; multipv?: number } } | null = null;
  private restartAttempts = 0;
  private lastFen: string | null = null;
  private lastOptions: { depth?: number; multipv?: number } = {};

  constructor(
    private onMessage: (line: EngineMessage) => void,
    private onError?: (error: string) => void
  ) { }

  /** Check if worker is alive */
  isAlive(): boolean {
    return this.worker !== null && this.state !== "CRASHED";
  }

  start() {
    if (typeof window === "undefined") return;
    if (this.worker) return; // Already started

    try {
      this.worker = new Worker("/stockfish-runner.js");

      this.worker.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.worker.onerror = (error) => {
        console.error("Stockfish Worker Error:", error);
        this.handleCrash(error.message || "Worker crashed");
      };

      this.worker.postMessage("uci");
      this.state = "IDLE";
      this.restartAttempts = 0; // Reset on successful start
    } catch (e) {
      console.error("Failed to start Stockfish worker:", e);
      this.handleCrash(e instanceof Error ? e.message : "Failed to start");
    }
  }

  /** Handle worker crash with auto-restart */
  private handleCrash(errorMessage: string) {
    this.state = "CRASHED";
    this.worker = null;

    // Notify listener
    this.onMessage({ error: errorMessage });
    this.onError?.(errorMessage);

    // Attempt auto-restart
    if (this.restartAttempts < MAX_RESTART_ATTEMPTS) {
      this.restartAttempts++;
      console.warn(`Attempting restart ${this.restartAttempts}/${MAX_RESTART_ATTEMPTS}...`);

      setTimeout(() => {
        this.start();

        // Resume analysis if we had a pending task
        if (this.lastFen && this.state !== "CRASHED") {
          this.analyze(this.lastFen, this.lastOptions);
        }
      }, RESTART_DELAY_MS);
    } else {
      console.error("Max restart attempts reached. Engine unavailable.");
      this.onError?.("Engine crashed repeatedly. Please refresh the page.");
    }
  }

  /** Manual restart */
  restart() {
    console.warn("Manual engine restart requested");
    this.worker?.terminate();
    this.worker = null;
    this.state = "IDLE";
    this.restartAttempts = 0;
    this.start();
  }

  stop() {
    if (this.worker) {
      this.worker.postMessage("quit");
      this.worker.terminate();
      this.worker = null;
    }
    this.state = "IDLE";
    this.pendingTask = null;
  }

  /** Stop current analysis without terminating the engine. */
  stopAnalysis() {
    if (this.worker && this.state === "ANALYZING") {
      this.worker.postMessage("stop");
      this.state = "STOPPING";
    }
    this.pendingTask = null;
  }

  setOption(name: string, value: string | number) {
    if (!this.worker) return;
    this.worker.postMessage(`setoption name ${name} value ${value}`);
  }

  analyze(fen: string, options: { depth?: number; multipv?: number } = {}) {
    // Save for crash recovery
    this.lastFen = fen;
    this.lastOptions = options;

    if (!this.worker || this.state === "CRASHED") this.start();

    if (this.state === "IDLE") {
      this.startAnalysis(fen, options);
    } else {
      // If we are already analyzing or stopping, queue the new task (replacing any existing pending task)
      this.pendingTask = { fen, options };

      if (this.state === "ANALYZING") {
        this.worker?.postMessage("stop");
        this.state = "STOPPING";
      }
      // If already STOPPING, we just update the pending task and wait for bestmove
    }
  }

  private startAnalysis(fen: string, options: { depth?: number; multipv?: number }) {
    if (!this.worker) return;

    this.worker.postMessage(`position fen ${fen}`);

    if (options.multipv) {
      this.worker.postMessage(`setoption name MultiPV value ${options.multipv}`);
    }

    const depth = options.depth || 99;
    this.worker.postMessage(`go depth ${depth}`);
    this.state = "ANALYZING";
  }

  private handleMessage(line: string) {
    if (typeof line !== "string") return;

    if (line.startsWith("bestmove")) {
      const bestMove = line.split(" ")[1];
      this.onMessage({ bestMove }); // Notify that a move was found (or search stopped)

      // Transition State
      if (this.state === "STOPPING" || this.state === "ANALYZING") {
        this.state = "IDLE";

        // Check for pending tasks
        if (this.pendingTask) {
          const task = this.pendingTask;
          this.pendingTask = null;
          this.startAnalysis(task.fen, task.options);
        }
      }

    } else if (line.startsWith("info depth") && line.indexOf(" score ") !== -1) {
      // Parse info
      const parts = line.split(" ");
      let depth = 0;
      let cp = 0;
      let mate = 0;
      let pv = "";
      let multipv = 1;

      for (let i = 0; i < parts.length; i++) {
        if (parts[i] === "depth") depth = parseInt(parts[i + 1]);
        if (parts[i] === "multipv") multipv = parseInt(parts[i + 1]);
        if (parts[i] === "cp") cp = parseInt(parts[i + 1]);
        if (parts[i] === "mate") {
          mate = parseInt(parts[i + 1]);
          // If mate is found, score is infinity effectively, but we pass raw mate value too
          cp = mate > 0 ? 30000 - mate : -30000 - mate;
        }
        if (parts[i] === "pv") {
          pv = parts.slice(i + 1).join(" ");
          break;
        }
      }

      this.onMessage({ cp, depth, pv, mate, multipv });
    }
  }
}

