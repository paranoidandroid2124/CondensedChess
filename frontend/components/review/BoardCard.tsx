import React, { useEffect, useRef, useState } from "react";
import { Chessboard } from "react-chessboard";
import { VerticalEvalBar } from "../VerticalEvalBar";

export type PieceDropArgs = {
  sourceSquare: string;
  targetSquare: string;
  piece?: string;
};

export function BoardCard({
  fen,
  squareStyles,
  arrows,
  evalPercent,
  judgementBadge,
  moveSquare,
  onDrop
}: {
  fen?: string;
  squareStyles?: Record<string, React.CSSProperties>;
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  judgementBadge?: string;
  moveSquare?: string;
  onDrop?: (args: PieceDropArgs) => boolean;
}) {
  const [width, setWidth] = useState(420);
  const rowRef = useRef<HTMLDivElement | null>(null);
  const draggable = typeof onDrop === "function";

  useEffect(() => {
    const measure = (el?: HTMLElement) => {
      const target = el ?? rowRef.current;
      if (!target) return;
      const total = target.clientWidth;
      // Leave room for eval bar + gap (~48px) and clamp for responsiveness
      const available = Math.max(240, total - 48);
      setWidth(Math.min(640, available));
    };

    // Initial measure
    measure();

    if (typeof ResizeObserver !== "undefined") {
      const ro = new ResizeObserver((entries) => {
        entries.forEach((entry) => measure(entry.target as HTMLElement));
      });
      if (rowRef.current) ro.observe(rowRef.current);
      return () => ro.disconnect();
    }

    const onResize = () => measure();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const combinedSquareStyles = { ...squareStyles };

  if (moveSquare && judgementBadge) {
    const judgmentColor =
      judgementBadge.includes("??") ? "rgba(220, 38, 38, 0.8)" :
        judgementBadge.includes("?") && !judgementBadge.includes("!") ? "rgba(251, 146, 60, 0.7)" :
          judgementBadge.includes("?!") ? "rgba(251, 191, 36, 0.7)" :
            judgementBadge.includes("!") ? "rgba(34, 197, 94, 0.7)" :
              "rgba(148, 163, 184, 0.5)";

    combinedSquareStyles[moveSquare] = {
      ...combinedSquareStyles[moveSquare],
      position: "relative" as const,
      backgroundColor: judgmentColor,
      boxShadow: `inset 0 0 0 3px ${judgmentColor.replace("0.7", "1")}`
    };
  }

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Board</h3>
        <span className="text-xs text-white/60">{fen ? "Selected ply" : "Starting position"}</span>
      </div>
      <div ref={rowRef} className="relative mt-3 flex gap-3">
        <VerticalEvalBar evalPercent={evalPercent} />
        <div className="relative overflow-hidden rounded-xl border border-white/10">
          <Chessboard
            id="review-board"
            position={fen || "start"}
            boardWidth={width}
            arePiecesDraggable={draggable}
            onPieceDrop={
              draggable
                ? (sourceSquare, targetSquare, piece) => onDrop({ sourceSquare, targetSquare, piece })
                : undefined
            }
            customLightSquareStyle={{ backgroundColor: "#e7ecff" }}
            customDarkSquareStyle={{ backgroundColor: "#5b8def" }}
            customSquareStyles={combinedSquareStyles}
            customArrows={arrows as any}
          />
        </div>
      </div>
    </div>
  );
}
