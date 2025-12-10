import React, { useState } from "react";
import type { TimelineNode } from "../../types/review";

interface QuickJumpProps {
  timeline: TimelineNode[];
  onSelect: (ply: number) => void;
}

export function QuickJump({ timeline, onSelect }: QuickJumpProps) {
  const [value, setValue] = useState<string>("");
  const maxPly = timeline[timeline.length - 1]?.ply ?? 0;
  const maxMove = Math.ceil(maxPly / 2);

  const submit = () => {
    const num = parseInt(value, 10);
    if (Number.isNaN(num) || num <= 0) return;

    // Jump to the White move of the requested turn (Ply = num*2 - 1)
    // If it's before start, clamp to 1. If after end, clamp to max.
    const targetPly = (num * 2) - 1;

    // Find nearest valid ply in timeline
    // (Timeline might not have specifically Ply 1 if started from FEN, but usually fine)
    const clampedPly = Math.min(Math.max(targetPly, timeline[0]?.ply ?? 0), maxPly);

    // Helper to find exact node or closest
    const best = timeline.reduce((prev, curr) =>
      Math.abs(curr.ply - clampedPly) < Math.abs(prev.ply - clampedPly) ? curr : prev
      , timeline[0]);

    if (best) onSelect(best.ply);
  };

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/70">
      <span className="uppercase tracking-[0.16em] text-white/60">Quick nav</span>
      <input
        type="number"
        min={1}
        max={maxMove || undefined}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit();
        }}
        className="w-20 rounded-md border border-white/10 bg-white/5 px-2 py-1 text-white outline-none focus:border-accent-teal/60"
        placeholder="Move #"
      />
      <button
        type="button"
        onClick={submit}
        className="rounded-md bg-accent-teal/20 px-3 py-1 text-white hover:bg-accent-teal/30"
      >
        Go
      </button>
      {maxMove ? <span className="text-[11px] text-white/50">1 – {maxMove}</span> : null}
    </div>
  );
}
