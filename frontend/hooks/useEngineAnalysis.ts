import { useCallback, useEffect, useState } from "react";
import { StockfishEngine, type EngineMessage } from "../lib/engine";
import type { Review } from "../types/review";

export function useEngineAnalysis(review: Review | null, previewFen: string | null, selectedPly: number | null) {
  const [engine, setEngine] = useState<StockfishEngine | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [engineLines, setEngineLines] = useState<EngineMessage[]>([]);

  useEffect(() => {
    const eng = new StockfishEngine((msg) => {
      if (msg.pv) {
        setEngineLines((prev) => {
          const existing = prev.find((l) => l.pv === msg.pv);
          if (existing) {
            return prev.map((l) => (l.pv === msg.pv ? msg : l));
          } else {
            const updated = [...prev, msg].sort((a, b) => (b.depth || 0) - (a.depth || 0));
            return updated.slice(0, 3);
          }
        });
      }
    });
    setEngine(eng);
    return () => eng.stop();
  }, []);

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
      setEngineLines([]); // Clear previous lines for new position
      engine.analyze(currentFen, 18, 3);
    } else {
      engine?.stop();
    }
  }, [isAnalyzing, selectedPly, review, engine, previewFen]);

  const toggleAnalysis = useCallback(() => {
    if (!isAnalyzing) {
      engine?.start();
      setIsAnalyzing(true);
    } else {
      engine?.stop();
      setIsAnalyzing(false);
      setEngineLines([]);
    }
  }, [engine, isAnalyzing]);

  return { engine, isAnalyzing, engineLines, toggleAnalysis, setEngineLines, setIsAnalyzing };
}

