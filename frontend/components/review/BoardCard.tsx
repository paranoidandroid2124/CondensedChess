import React, { useEffect, useRef, useState } from "react";
import InteractiveBoard from "../board/InteractiveBoard"; // Renamed from LichessBoard
import { VerticalEvalBar } from "../VerticalEvalBar";
import { DrawShape } from "chessground/draw";
import { Key } from "chessground/types";

export type PieceDropArgs = {
  sourceSquare: string;
  targetSquare: string;
  piece?: string;
};

export function BoardCard({
  fen,
  customShapes = [],
  arrows,
  evalPercent,
  judgementBadge,
  moveSquare,
  onDrop
}: {
  fen?: string;
  customShapes?: DrawShape[]; // New prop from ReviewClient
  squareStyles?: Record<string, React.CSSProperties>; // Deprecated but kept for type compat if needed
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  judgementBadge?: string;
  moveSquare?: string;
  onDrop?: (args: PieceDropArgs) => boolean;
}) {
  const [width, setWidth] = useState(420);
  const rowRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const measure = (el?: HTMLElement) => {
      const target = el ?? rowRef.current;
      if (!target) return;
      const total = target.clientWidth;
      const available = Math.max(240, total - 48);
      setWidth(Math.min(640, available));
    };

    measure();
    if (typeof ResizeObserver !== "undefined") {
      const ro = new ResizeObserver((entries) => {
        entries.forEach((entry) => measure(entry.target as HTMLElement));
      });
      if (rowRef.current) ro.observe(rowRef.current);
      return () => ro.disconnect();
    }
    window.addEventListener("resize", () => measure());
    return () => window.removeEventListener("resize", () => measure());
  }, []);

  // Convert artifacts to Chessground Shapes
  const shapes: DrawShape[] = [...customShapes];

  // 1. Arrows
  if (arrows) {
    arrows.forEach(([orig, dest, color]) => {
      shapes.push({
        orig: orig as Key,
        dest: dest as Key,
        brush: color === 'red' ? 'red' : color === 'green' ? 'green' : 'blue', // Simple mapping
      });
    });
  }

  // 2. Move Square Highlight (Judgment)
  if (moveSquare && judgementBadge) {
    const brush =
      judgementBadge.includes("??") ? "red" :
        judgementBadge.includes("?") && !judgementBadge.includes("!") ? "orange" :
          judgementBadge.includes("?!") ? "yellow" :
            judgementBadge.includes("!") ? "green" :
              "blue"; // Default

    shapes.push({
      orig: moveSquare as Key,
      brush: brush,
    });
  }

  // 3. SquareStyles (Fallback map if needed, mainly checks/highlights)
  // react-chessboard style mapping is hard, simplified for Lichess style.

  const handleMove = (orig: Key, dest: Key, newFen: string) => {
    if (onDrop) {
      onDrop({
        sourceSquare: orig,
        targetSquare: dest,
        piece: undefined // Chessground doesn't check piece here easily, backend/logic handles it
      });
    }
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Board</h3>
        <span className="text-xs text-white/60">{fen ? "Selected ply" : "Starting position"}</span>
      </div>
      <div ref={rowRef} className="relative mt-3 flex gap-3">
        <VerticalEvalBar evalPercent={evalPercent} />
        <div
          className="relative overflow-hidden rounded-xl border border-white/10"
          style={{ width: width, height: width }}
        >
          <InteractiveBoard
            fen={fen || "start"}
            shapes={shapes}
            onMove={handleMove}
            viewOnly={!onDrop}
          />
        </div>
      </div>
    </div>
  );
}
