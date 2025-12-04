import { DrawingTools } from "../DrawingTools";
import { EvalSparkline } from "./EvalSparkline";
import { MoveControls } from "./MoveControls";
import { BoardCard, type PieceDropArgs } from "./BoardCard";
import type { EnhancedTimelineNode } from "../../lib/review-derived";

export function BoardSection({
  fen,
  squareStyles,
  arrows,
  evalPercent,
  judgementBadge,
  moveSquare,
  onDrop,
  drawingColor,
  onSelectColor,
  onClearArrows,
  previewLabel,
  showAdvanced,
  timeline,
  conceptSpikes,
  selectedPly,
  onSelectPly,
  branchSaving,
  branchError
}: {
  fen?: string;
  squareStyles?: Record<string, React.CSSProperties>;
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  judgementBadge?: string;
  moveSquare?: string;
  onDrop?: (args: PieceDropArgs) => boolean;
  drawingColor: "green" | "red" | "blue" | "orange";
  onSelectColor: (c: "green" | "red" | "blue" | "orange") => void;
  onClearArrows: () => void;
  previewLabel?: string | null;
  showAdvanced: boolean;
  timeline: EnhancedTimelineNode[];
  conceptSpikes: Array<{ ply: number; concept: string; delta: number; label: string }>;
  selectedPly?: number | null;
  onSelectPly: (ply: number) => void;
  branchSaving: boolean;
  branchError: string | null;
}) {
  return (
    <div className="space-y-4 lg:sticky lg:top-4 lg:self-start">
      <div className="group relative">
        <BoardCard
          fen={fen}
          squareStyles={squareStyles}
          arrows={arrows}
          evalPercent={evalPercent}
          judgementBadge={judgementBadge}
          moveSquare={moveSquare}
          onDrop={onDrop}
        />
        <DrawingTools
          selectedColor={drawingColor}
          onSelectColor={onSelectColor}
          onClear={onClearArrows}
        />
      </div>
      {previewLabel ? (
        <div className="text-xs text-amber-100">Previewing line: {previewLabel}</div>
      ) : null}
      {showAdvanced ? (
        <EvalSparkline
          timeline={timeline}
          spikePlys={conceptSpikes.map((s) => ({ ply: s.ply, concept: s.concept }))}
        />
      ) : null}
      <MoveControls timeline={timeline} selected={selectedPly ?? undefined} onSelect={onSelectPly} />
      <div className="text-xs text-white/60">
        Drag on board to add a variation at the selected ply (server merges/dedupes).
        {branchSaving ? <span className="ml-2 text-accent-teal">Saving…</span> : null}
        {branchError ? <span className="ml-2 text-rose-200">{branchError}</span> : null}
      </div>
    </div>
  );
}

