import { useState } from "react";
import type { CriticalNode } from "../../types/review";
import { CriticalMomentCard } from "./CriticalMomentCard";

export function CriticalList({
  critical,
  fenBeforeByPly,
  onSelectPly
}: {
  critical: CriticalNode[];
  fenBeforeByPly: Record<number, string | undefined>;
  onSelectPly?: (ply: number) => void;
}) {
  const [showOnlyPressure, setShowOnlyPressure] = useState(false);

  const pressurePoints = critical.filter(c => c.isPressurePoint);
  const displayedCritical = showOnlyPressure ? pressurePoints : critical;

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-white/80">Critical moments</h3>
        <span className="text-xs text-white/60">Key swings & ideas</span>
      </div>

      {pressurePoints.length > 0 && (
        <div className="flex items-center gap-2 mb-3 pb-3 border-b border-white/10">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={showOnlyPressure}
              onChange={(e) => setShowOnlyPressure(e.target.checked)}
              className="w-4 h-4 rounded border-white/20 bg-white/10 text-amber-500 focus:ring-amber-500"
            />
            <span className="text-sm text-white/80">⚡ Show only Pressure Points</span>
          </label>
          <span className="text-xs text-amber-300/70">
            ({pressurePoints.length} found)
          </span>
        </div>
      )}

      <div className="mt-3 space-y-3">
        {displayedCritical.map((c) => (
          <CriticalMomentCard key={c.ply} critical={c} fenBefore={fenBeforeByPly[c.ply]} onSelectPly={onSelectPly} />
        ))}
      </div>
    </div>
  );
}

