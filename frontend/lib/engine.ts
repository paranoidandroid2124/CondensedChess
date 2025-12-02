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
      }
      if (line.startsWith("info depth")) {
        this.parseInfo(line);
      }
      if (line.startsWith("bestmove")) {
        const parts = line.split(" ");
        this.onMessage({ bestMove: parts[1] });
      }
    };
    this.worker.postMessage("uci");
  }

  stop() {
    this.worker?.terminate();
    this.worker = null;
    this.isReady = false;
  }

  analyze(fen: string, depth: number = 18, multiPv: number = 1) {
    if (!this.worker) return;
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
