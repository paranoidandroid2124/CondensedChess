import { CompressedMoveList } from "../CompressedMoveList";
import { QuickJump } from "../common/QuickJump";
import type { EnhancedTimelineNode } from "../../lib/review-derived";

export function MovesSection({
  timeline,
  selectedPly,
  onSelectPly
}: {
  timeline: EnhancedTimelineNode[];
  selectedPly: number | null;
  onSelectPly: (ply: number) => void;
}) {
  return (
    <div className="flex flex-col gap-4 lg:h-[calc(100vh-2rem)] lg:sticky lg:top-4">
      <CompressedMoveList
        timeline={timeline}
        currentPly={selectedPly}
        onSelectPly={onSelectPly}
      />
      <QuickJump timeline={timeline} onSelect={(ply) => onSelectPly(ply)} />
    </div>
  );
}

