import React, { useState } from "react";
import type { TimelineNode } from "../../types/review";

interface QuickJumpProps {
  timeline: TimelineNode[];
  onSelect: (ply: number) => void;
}

export function QuickJump({ timeline, onSelect }: QuickJumpProps) {
  const [value, setValue] = useState<string>("");
  const maxPly = timeline[timeline.length - 1]?.ply ?? 0;

  const nearestPly = (target: number) => {
    if (!timeline.length) return null;
    let best = timeline[0].ply;
    let bestDiff = Math.abs(timeline[0].ply - target);
    for (const t of timeline) {
      const diff = Math.abs(t.ply - target);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = t.ply;
      }
    }
    return best;
  };

  const submit = () => {
    const num = parseInt(value, 10);
    if (Number.isNaN(num) || num <= 0) return;
    const clamped = Math.min(Math.max(num, 1), maxPly || num);
    const target = nearestPly(clamped);
    if (target != null) onSelect(target);
  };

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/70">
      <span className="uppercase tracking-[0.16em] text-white/60">Quick nav</span>
      <input
        type="number"
        min={1}
        max={maxPly || undefined}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit();
        }}
        className="w-20 rounded-md border border-white/10 bg-white/5 px-2 py-1 text-white outline-none focus:border-accent-teal/60"
        placeholder="Ply #"
      />
      <button
        type="button"
        onClick={submit}
        className="rounded-md bg-accent-teal/20 px-3 py-1 text-white hover:bg-accent-teal/30"
      >
        Go
      </button>
      {maxPly ? <span className="text-[11px] text-white/50">1 – {maxPly}</span> : null}
    </div>
  );
}
