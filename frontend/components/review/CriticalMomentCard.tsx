import React from "react";
import type { CriticalNode } from "../../types/review";
import { PracticalityBadge } from "../PracticalityBadge";
import { uciToSan } from "../../lib/chess-utils";
import { getPracticalityNarrative } from "../../lib/practicality-narrative";
import { displayTag, humanizeTag, phaseOf } from "../../lib/review-tags";
import { formatDelta } from "../../lib/review-format";
import { TagBadge } from "../common/TagBadge";

interface Props {
  critical: CriticalNode;
  fenBefore: string | undefined;
  onSelectPly?: (ply: number) => void;
}

const humanReason = (r: string) => {
  const low = r.toLowerCase();
  if (low.includes("blunder")) return "Blunder: big swing";
  if (low.includes("mistake")) return "Mistake: swing down";
  if (low.includes("swing")) return "Big eval swing";
  return "Concept change";
};

export function CriticalMomentCard({ critical: c, fenBefore, onSelectPly }: Props) {
  const narrative = getPracticalityNarrative(c.practicality, c.comment);

  return (
    <div className={`rounded-xl border p-3 ${c.isPressurePoint
      ? 'border-2 border-amber-500/60 bg-amber-500/5'
      : 'border-white/10 bg-white/5'
      }`}>
      {c.isPressurePoint && (
        <div className="flex items-center gap-2 mb-2 pb-2 border-b border-amber-500/30">
          <span className="text-xl">⚡</span>
          <span className="text-amber-300 font-semibold text-sm">Pressure Point</span>
          <span className="text-xs text-amber-200/60">You created a difficult defensive problem</span>
        </div>
      )}
      <div className="flex items-center justify-between">
        <div className="text-sm font-semibold text-white">
          {`Move ${Math.ceil(c.ply / 2)}${c.ply % 2 === 0 ? "..." : ""}`}
          <span className="ml-2 text-xs text-white/60">{humanReason(c.reason)}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="text-xs font-semibold text-rose-200">{formatDelta(c.deltaWinPct)}</div>
          {onSelectPly ? (
            <button
              type="button"
              onClick={() => onSelectPly(c.ply)}
              className="rounded-full border border-white/10 px-2 py-0.5 text-[11px] text-white/70 hover:border-accent-teal/60 hover:text-white"
            >
              Jump to board
            </button>
          ) : null}
        </div>
      </div>

      <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-white/70">
        {phaseOf(c) ? (
          <span className="rounded-full bg-accent-teal/15 px-2 py-1 text-accent-teal/80">{phaseOf(c)}</span>
        ) : null}
        {c.mistakeCategory ? (
          <span className="rounded-full bg-rose-500/15 px-2 py-1 text-rose-100">{displayTag(c.mistakeCategory)}</span>
        ) : null}
        {c.tags?.slice(0, 5).map((t) => (
          <span key={t} className="rounded-full bg-white/10 px-2 py-1">
            {displayTag(t)}
          </span>
        ))}
        {c.practicality ? <PracticalityBadge score={c.practicality} /> : null}
        {c.opponentRobustness !== undefined ? (
          <span className="rounded-full bg-indigo-500/20 px-2 py-0.5 text-[11px] text-indigo-300 border border-indigo-500/30" title={`Opponent Robustness: ${c.opponentRobustness.toFixed(2)}`}>
            Opponent Burden: {c.opponentRobustness < 0.3 ? "High" : c.opponentRobustness > 0.7 ? "Low" : "Medium"}
          </span>
        ) : null}
      </div>

      {c.semanticTags && c.semanticTags.length > 0 && (
        <div className="mt-2 flex flex-wrap items-center gap-1.5">
          <span className="text-[10px] uppercase tracking-wide text-white/40">Facts:</span>
          {c.semanticTags.slice(0, 6).map((tag) => (
            <TagBadge key={tag} tag={tag} />
          ))}
        </div>
      )}

      {c.branches?.length ? (
        <div className="mt-2 grid gap-2">
          {c.branches.slice(0, 3).map((b) => (
            <div key={b.move} className="rounded-lg bg-white/5 px-3 py-2">
              <div className="flex items-center justify-between">
                <div className="text-sm text-white">
                  <span className="mr-2 rounded-full bg-white/10 px-2 py-0.5 text-[11px] uppercase tracking-wide text-white/70">
                    {b.label}
                  </span>
                  {uciToSan(fenBefore, b.move)}
                </div>
                <div className="text-xs text-accent-teal">{b.winPct.toFixed(1)}%</div>
              </div>
              {b.comment && (
                <div className="mt-1 text-xs text-white/60 italic ml-2 border-l-2 border-white/10 pl-2">
                  {b.comment}
                </div>
              )}
            </div>
          ))}
        </div>
      ) : null}

      <p className="mt-2 rounded-lg bg-white/5 px-3 py-2 text-sm text-white/80">
        {narrative || "No commentary available for this moment."}
      </p>
    </div>
  );
}
