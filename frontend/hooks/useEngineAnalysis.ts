import { useCallback, useEffect, useState, useRef } from "react";
import { StockfishEngine, type EngineMessage } from "../lib/engine";
import type { Review } from "../types/review";

export type EngineConfig = {
  threads: number;
  hash: number;
  multiPv: number;
  depth: number; // 99 for infinite
};

export type EngineStatus = "idle" | "loading" | "ready" | "analyzing" | "error" | "restarting";

export function useEngineAnalysis(review: Review | null, previewFen: string | null, selectedPly: number | null) {
  const [engine, setEngine] = useState<StockfishEngine | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [engineLines, setEngineLines] = useState<EngineMessage[]>([]);
  const [engineStatus, setEngineStatus] = useState<EngineStatus>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Use a map keyed by multipv index for stable accumulation
  const linesMapRef = useRef<Map<number, EngineMessage>>(new Map());

  // Settings State
  const [config, setConfig] = useState<EngineConfig>({
    threads: 1,
    hash: 32,
    multiPv: 3,
    depth: 99
  });

  useEffect(() => {
    setEngineStatus("loading");
    setErrorMessage(null);

    const eng = new StockfishEngine(
      // onMessage callback
      (msg) => {
        // Handle error message from crash
        if (msg.error) {
          setEngineStatus("restarting");
          setErrorMessage(msg.error);
          return;
        }

        if (msg.pv) {
          // Clear error on successful message
          setErrorMessage(null);

          // Use multipv index if available, otherwise fallback to line order
          const pvIndex = msg.multipv ?? linesMapRef.current.size + 1;
          linesMapRef.current.set(pvIndex, msg);

          // Convert map to array sorted by multipv index
          const sortedLines = Array.from(linesMapRef.current.entries())
            .sort((a, b) => a[0] - b[0])
            .map(([_, line]) => line)
            .slice(0, config.multiPv);

          setEngineLines(sortedLines);
        }
        // Detect if engine is ready (uciok received)
        if (msg.bestMove !== undefined || msg.pv) {
          setEngineStatus("analyzing");
        }
      },
      // onError callback
      (error) => {
        if (error.includes("repeatedly")) {
          setEngineStatus("error");
        } else {
          setEngineStatus("restarting");
        }
        setErrorMessage(error);
      }
    );
    eng.start();
    setEngine(eng);

    // Give engine time to initialize
    const readyTimer = setTimeout(() => {
      if (eng.isAlive()) {
        setEngineStatus("ready");
        setErrorMessage(null);
      }
    }, 1000);

    return () => {
      clearTimeout(readyTimer);
      eng.stop();
    };
  }, []);

  // Update engine options when config changes
  useEffect(() => {
    if (engine) {
      engine.setOption("Threads", config.threads);
      engine.setOption("Hash", config.hash);
    }
  }, [engine, config.threads, config.hash]);

  useEffect(() => {
    if (isAnalyzing && engine && review) {
      let currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
      if (previewFen) {
        currentFen = previewFen;
      } else if (selectedPly !== null) {
        const node = review.timeline.find((t) => t.ply === selectedPly);
        if (node) currentFen = node.fen;
      } else if (review.timeline?.length) {
        currentFen = review.timeline[review.timeline.length - 1].fen;
      }

      // Clear previous lines for new position
      linesMapRef.current.clear();
      setEngineLines([]);
      setEngineStatus("analyzing");

      engine.analyze(currentFen, {
        depth: config.depth,
        multipv: config.multiPv
      });

    } else if (!isAnalyzing && engine) {
      engine.stopAnalysis?.();
      setEngineStatus(engineStatus === "analyzing" ? "ready" : engineStatus);
    }
  }, [isAnalyzing, selectedPly, review, engine, previewFen, config.multiPv, config.depth]);

  const toggleAnalysis = useCallback(() => {
    if (!isAnalyzing) {
      setIsAnalyzing(true);
    } else {
      engine?.stopAnalysis?.();
      setIsAnalyzing(false);
      linesMapRef.current.clear();
      setEngineLines([]);
    }
  }, [engine, isAnalyzing]);

  return {
    engine,
    isAnalyzing,
    engineLines,
    engineStatus,
    errorMessage,
    toggleAnalysis,
    setEngineLines,
    setIsAnalyzing,
    config,
    setConfig
  };
}

