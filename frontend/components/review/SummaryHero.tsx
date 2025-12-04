import React, { useMemo } from "react";
import type { CriticalNode, Review, TimelineNode } from "../../types/review";
import { formatDelta } from "../../lib/review-format";

interface SummaryHeroProps {
  timeline: (TimelineNode & { label?: string })[];
  critical: CriticalNode[];
  review?: Review;
}

export function SummaryHero({ timeline, critical, review }: SummaryHeroProps) {
  type SummaryStats = {
    total: number;
    counts: Record<string, number>;
    worst: { label: string; delta: number } | null;
    topCritical?: { ply: number; reason: string } | null;
  };
  const stats = useMemo<SummaryStats>(() => {
    const counts = { blunder: 0, mistake: 0, inaccuracy: 0, good: 0, best: 0 };
    let worstDelta = 0;
    let worstMove: { label: string; delta: number } | null = null;
    timeline.forEach((t) => {
      const j = t.judgement?.toLowerCase() ?? "good";
      if (j === "blunder") counts.blunder++;
      else if (j === "mistake") counts.mistake++;
      else if (j === "inaccuracy") counts.inaccuracy++;
      else if (j === "best") counts.best++;
      else counts.good++;
      if (t.deltaWinPct && t.deltaWinPct < worstDelta) {
        worstDelta = t.deltaWinPct;
        const moveNumber = Math.ceil(t.ply / 2);
        const turnPrefix = t.ply % 2 === 1 ? "." : "...";
        worstMove = { label: `${moveNumber}${turnPrefix} ${t.san}`, delta: t.deltaWinPct };
      }
    });
    const topCritical = critical.length
      ? { ply: critical[0].ply, reason: critical[0].reason.split(":").pop()?.trim() ?? critical[0].reason }
      : null;
    return { total: timeline.length, counts, worst: worstMove, topCritical };
  }, [timeline, critical]);

  const avgPracticality = useMemo(() => {
    const withPracticality = timeline.filter(t => t.practicality);
    if (!withPracticality.length) return null;

    const avg = withPracticality.reduce((sum, t) =>
      sum + t.practicality!.overall, 0) / withPracticality.length;

    const category =
      avg >= 0.75 ? { label: "Human-Friendly", color: "text-emerald-400", icon: "🟢" } :
        avg >= 0.50 ? { label: "Challenging", color: "text-amber-400", icon: "🟡" } :
          avg >= 0.25 ? { label: "Engine-Like", color: "text-orange-400", icon: "🟠" } :
            { label: "Computer-Only", color: "text-rose-400", icon: "🔴" };

    return { score: avg, ...category };
  }, [timeline]);

  const pressurePointCount = useMemo(() =>
    critical.filter(c => c.isPressurePoint).length,
    [critical]);

  const pressurePointsByColor = useMemo(() => {
    const counts = { white: 0, black: 0 };
    critical
      .filter((c) => c.isPressurePoint)
      .forEach((c) => {
        const turn = timeline.find((t) => t.ply === c.ply)?.turn ?? (c.ply % 2 === 1 ? "white" : "black");
        if (turn === "white") counts.white += 1;
        else counts.black += 1;
      });
    return counts;
  }, [critical, timeline]);

  return (
    <div className="glass-card mb-4 rounded-2xl p-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Game summary</p>
          <h2 className="font-display text-2xl text-white">At a glance</h2>
        </div>
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-white/10 px-3 py-1">Moves: {stats.total}</span>
          <span className="rounded-full bg-rose-500/15 px-3 py-1 text-rose-100">Blunder {stats.counts.blunder}</span>
          <span className="rounded-full bg-orange-500/15 px-3 py-1 text-orange-100">Mistake {stats.counts.mistake}</span>
          <span className="rounded-full bg-amber-500/15 px-3 py-1 text-amber-100">Inacc {stats.counts.inaccuracy}</span>
        </div>
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Accuracy (White)</p>
          {review?.accuracyWhite != null ? (
            <>
              <div className="text-xl font-bold text-accent-teal">{review.accuracyWhite.toFixed(1)}%</div>
              <div className="text-xs text-white/60">Overall precision</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Accuracy (Black)</p>
          {review?.accuracyBlack != null ? (
            <>
              <div className="text-xl font-bold text-accent-teal">{review.accuracyBlack.toFixed(1)}%</div>
              <div className="text-xs text-white/60">Overall precision</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Biggest drop</p>
          {stats.worst ? (
            <>
              <div className="text-sm font-semibold text-white">{stats.worst.label}</div>
              <div className="text-xs text-rose-200">{formatDelta(stats.worst.delta)}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No blunders detected</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Critical</p>
          {stats.topCritical ? (
            <>
              <div className="text-sm font-semibold text-white">Ply {stats.topCritical.ply}</div>
              <div className="text-xs text-white/70">{stats.topCritical.reason}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No critical nodes</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Avg Practicality</p>
          {avgPracticality ? (
            <>
              <div className={`text-lg font-bold ${avgPracticality.color} flex items-center gap-1`}>
                <span>{avgPracticality.icon}</span>
                <span>{avgPracticality.label}</span>
              </div>
              <div className="text-xs text-white/60">Score: {avgPracticality.score.toFixed(2)}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Pressure Points</p>
          {pressurePointCount > 0 ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm text-white">
                <span>White</span>
                <span className="text-lg font-bold text-amber-300">⚡ {pressurePointsByColor.white}</span>
              </div>
              <div className="flex items-center justify-between text-sm text-white">
                <span>Black</span>
                <span className="text-lg font-bold text-amber-300">⚡ {pressurePointsByColor.black}</span>
              </div>
              <div className="text-xs text-white/60">Counted from critical nodes marked as pressure points.</div>
            </div>
          ) : (
            <p className="text-xs text-white/60">None detected</p>
          )}
        </div>
      </div>
    </div>
  );
}
