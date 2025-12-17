import React, { useEffect, useRef, useState } from "react";
import InteractiveBoard from "../board/InteractiveBoard";
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
  cp,
  mate,
  judgementBadge,
  moveSquare,
  onDrop,
  check,
  lastMove,
  orientation = "white"
}: {
  fen?: string;
  customShapes?: DrawShape[];
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  cp?: number;
  mate?: number;
  judgementBadge?: string;
  moveSquare?: string;
  onDrop?: (args: PieceDropArgs) => boolean;
  check?: boolean;
  lastMove?: [string, string];
  orientation?: "white" | "black";
}) {
  const [width, setWidth] = useState(420);
  const rowRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // ResizeObserver handled inside InteractiveBoard now for Redraw.
    // This hook mainly handles container sizing logic if needed.
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
        brush: color === 'red' ? 'red' : color === 'green' ? 'green' : 'blue',
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
        <span className="text-xs text-white/60">{fen ? "Analysis Board" : "Starting position"}</span>
      </div>

      <div className="mt-4 flex justify-center" ref={rowRef}>
        <div style={{ width: width, height: width }}>
          <InteractiveBoard
            fen={fen || "start"}
            orientation={orientation}
            viewOnly={!onDrop}
            onMove={handleMove}
            shapes={shapes}
            lastMove={lastMove as [Key, Key]} /* Cast string to Key */
            check={check}
          />
        </div>

        {/* Eval Bar */}
        <div className="ml-4 h-full" style={{ height: width }}>
          <VerticalEvalBar evalPercent={evalPercent} cp={cp} mate={mate} />
        </div>
      </div>
    </div>
  );
}
