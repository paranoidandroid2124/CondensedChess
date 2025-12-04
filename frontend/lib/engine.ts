export type EngineMessage = {
  uci?: string;
  depth?: number;
  cp?: number;
  mate?: number;
  pv?: string;
  bestMove?: string;
};

export class StockfishEngine {
  private worker: Worker | null = null;
  private onMessage: (msg: EngineMessage) => void;
  private isReady = false;
  private pending: { fen: string; depth: number; multiPv: number } | null = null;

  constructor(onMessage: (msg: EngineMessage) => void) {
    this.onMessage = onMessage;
  }

  start() {
    if (typeof window === "undefined") return;
    this.worker = new Worker("/stockfish.js");
    this.worker.onmessage = (e) => {
      const line = e.data;
      // console.log("Engine:", line);
      if (line === "uciok") {
        this.isReady = true;
        if (this.pending) {
          const { fen, depth, multiPv } = this.pending;
          this.pending = null;
          this.analyze(fen, depth, multiPv);
        }
      }
      if (line.startsWith("info depth")) {
        this.parseInfo(line);
      }
      if (line.startsWith("bestmove")) {
        const parts = line.split(" ");
        this.onMessage({ bestMove: parts[1] });
      }
    };
    this.worker.onerror = (err) => {
      console.error("[stockfish] worker error", err);
    };
    this.worker.postMessage("uci");
  }

  stop() {
    this.worker?.terminate();
    this.worker = null;
    this.isReady = false;
    this.pending = null;
  }

  analyze(fen: string, depth: number = 18, multiPv: number = 1) {
    if (!this.worker) return;
    if (!this.isReady) {
      this.pending = { fen, depth, multiPv };
      return;
    }
    this.worker.postMessage("stop");
    this.worker.postMessage(`setoption name MultiPV value ${multiPv}`);
    this.worker.postMessage(`position fen ${fen}`);
    this.worker.postMessage(`go depth ${depth}`);
  }

  private parseInfo(line: string) {
    const parts = line.split(" ");
    const depthIdx = parts.indexOf("depth");
    const cpIdx = parts.indexOf("cp");
    const mateIdx = parts.indexOf("mate");
    const pvIdx = parts.indexOf("pv");

    const msg: EngineMessage = {};
    if (depthIdx !== -1) msg.depth = parseInt(parts[depthIdx + 1]);
    if (cpIdx !== -1) msg.cp = parseInt(parts[cpIdx + 1]);
    if (mateIdx !== -1) msg.mate = parseInt(parts[mateIdx + 1]);
    if (pvIdx !== -1) msg.pv = parts.slice(pvIdx + 1).join(" ");

    this.onMessage(msg);
  }
}
