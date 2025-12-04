import type { StudyChapter } from "../../types/review";
import { displayTag, humanizeTag } from "../../lib/review-tags";
import { formatDelta, formatSanHuman } from "../../lib/review-format";

export function StudyPanel({ chapters, onSelect }: { chapters?: StudyChapter[]; onSelect: (ply: number) => void }) {
  if (!chapters || !chapters.length) return null;
  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Chapters</p>
          <h3 className="text-sm font-semibold text-white/90">Training chapters</h3>
        </div>
        <span className="rounded-full bg-white/10 px-2 py-1 text-[11px] text-white/70">#{chapters.length}</span>
      </div>
      <div className="mt-3 space-y-3">
        {chapters.map((ch) => (
          <button
            key={ch.id}
            onClick={() => onSelect(ch.anchorPly)}
            className="w-full rounded-xl border border-white/10 bg-white/5 p-3 text-left hover:border-accent-teal/40"
          >
            <div className="flex items-center justify-between text-xs text-white/70">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-white">Ply {ch.anchorPly}</span>
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/70">
                  Study score / Quality {ch.studyScore?.toFixed?.(1) ?? "–"}
                </span>
              </div>
              <span className={`rounded-full px-2 py-0.5 text-[11px] ${ch.deltaWinPct < 0 ? "bg-rose-500/15 text-rose-100" : "bg-emerald-500/15 text-emerald-100"}`}>
                Δ {formatDelta(ch.deltaWinPct)}
              </span>
            </div>
            <div className="mt-1 text-sm text-white">
              {formatSanHuman(ch.played)}
              {ch.best ? (
                <span className="text-xs text-white/60">
                  {" "}
                  vs {formatSanHuman(ch.best)}
                </span>
              ) : null}
            </div>
            <div className="mt-1 flex flex-wrap gap-2 text-[11px] text-white/70">
              {ch.tags.slice(0, 5).map((t) => (
                <span key={t} className="rounded-full bg-white/10 px-2 py-0.5">
                  {displayTag(t)}
                </span>
              ))}
            </div>
            <div className="mt-2 space-y-1 text-[11px] text-white/70">
              {ch.lines.map((l) => (
                <div key={l.label} className="flex items-center gap-2">
                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-white/80">
                    {l.label === "played"
                      ? "You should've played"
                      : l.label === "engine"
                        ? "Best move"
                        : l.label === "alt"
                          ? "Practical alternative"
                          : humanizeTag(l.label)}
                  </span>
                  <span className="text-white/80">{l.pv.map(formatSanHuman).join(" ")}</span>
                  <span className="text-white/50">{l.winPct.toFixed(1)}%</span>
                </div>
              ))}
            </div>
            {ch.summary ? (
              <p className="mt-2 text-sm text-white/80">{ch.summary}</p>
            ) : (
              <p className="mt-2 text-xs text-white/50">No summary (server fallback).</p>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}

