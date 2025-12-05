import { useMemo } from "react";
import { Chess } from "chess.js";
import type { Features } from "../types/review";
import type { EnhancedTimelineNode } from "../lib/review-derived";

export function useInstantTimeline(instantPgn: string | null): EnhancedTimelineNode[] | null {
  return useMemo(() => {
    if (!instantPgn) return null;
    try {
      const baseFeatures: Features = {
        pawnIslands: 0,
        isolatedPawns: 0,
        doubledPawns: 0,
        passedPawns: 0,
        rookOpenFiles: 0,
        rookSemiOpenFiles: 0,
        bishopPair: false,
        kingRingPressure: 0,
        spaceControl: 0
      };
      const replay = new Chess();
      const verbose = new Chess();
      verbose.loadPgn(instantPgn);
      const history = verbose.history({ verbose: true });

      const timeline: EnhancedTimelineNode[] = [
        {
          ply: 0,
          turn: "white",
          san: "Start",
          uci: "",
          fen: replay.fen(),
          fenBefore: replay.fen(),
          features: { ...baseFeatures },
          label: "Start",
          // EnhancedTimelineNode additional fields with defaults
          winPctBefore: 50,
          judgement: undefined,
          special: undefined
        }
      ];

      history.forEach((mv, idx) => {
        const fenBefore = replay.fen();
        const move = replay.move(mv);
        if (!move) return;
        const ply = idx + 1;
        const moveNumber = Math.ceil(ply / 2);
        const label = `${moveNumber}${move.color === "w" ? "." : "..."} ${move.san}`;
        timeline.push({
          ply,
          turn: move.color === "w" ? "white" : "black",
          san: move.san,
          uci: `${move.from}${move.to}${move.promotion ?? ""}`,
          fen: replay.fen(),
          fenBefore,
          features: { ...baseFeatures },
          label,
          // EnhancedTimelineNode additional fields with defaults
          winPctBefore: 50,
          judgement: undefined,
          special: undefined
        });
      });
      return timeline;
    } catch {
      return null;
    }
  }, [instantPgn]);
}
