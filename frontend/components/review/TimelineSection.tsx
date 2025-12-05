import { TimelineView } from "./TimelineView";

import { OpeningStatsPanel } from "./OpeningStatsPanel";
import type { MoveRow } from "../../lib/review-derived";
import type { VariationEntry } from "./TimelineView";
import type { Review } from "../../types/review";

export function TimelineSection({
  moveRows,
  selected,
  onSelect,
  variations,
  showAdvanced,
  onSelectVariation,
  onPreviewLine,
  reviewRoot,
  openingLookup,
  lookupLoading,
  lookupError
}: {
  moveRows: MoveRow[];
  selected?: number;
  onSelect: (ply: number) => void;
  variations: Record<number, VariationEntry[]>;
  showAdvanced: boolean;
  onSelectVariation: (v: VariationEntry | null) => void;
  onPreviewLine: (fenBefore?: string, pv?: string[], label?: string) => void;
  reviewRoot: Review["root"];
  openingLookup: Review["openingStats"] | null;
  lookupLoading: boolean;
  lookupError: string | null;
}) {
  return (
    <div className="grid gap-4 lg:grid-cols-[1.65fr_1.35fr] xl:grid-cols-[1.6fr_1.4fr]">
      <div>
        <TimelineView
          rows={moveRows}
          selected={selected}
          onSelect={onSelect}
          variations={variations}
          showAdvanced={showAdvanced}
          onSelectVariation={onSelectVariation}
          onPreviewLine={onPreviewLine}
        />
      </div>
      <div className="space-y-4">
        <OpeningStatsPanel stats={openingLookup} loading={lookupLoading} error={lookupError} />
      </div>
    </div>
  );
}
