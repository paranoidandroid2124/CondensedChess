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
    topCritical?: { ply: number; reason: string; label: string } | null;
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
      ? {
        ply: critical[0].ply,
        label: timeline.find(t => t.ply === critical[0].ply)?.label ?? `Move ${Math.ceil(critical[0].ply / 2)}`,
        reason: critical[0].reason.split(":").pop()?.trim() ?? critical[0].reason
      }
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
      {review?.summaryText && (
        <div className="mt-3 text-sm leading-relaxed text-white/80 border-l-2 border-accent-teal/50 pl-3">
          {review.summaryText}
        </div>
      )}
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
              <div className="text-sm font-semibold text-white">{stats.topCritical.label}</div>
              <div className="text-xs text-white/70">{stats.topCritical.reason}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No critical nodes</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Game Character</p>
          {(() => {
            // Peak Intensity Logic: Top 20% average to avoid dilution
            const scores = {
              tactical: [] as number[],
              positional: [] as number[], // driven by 'strategic' or 'goodKnight' etc? Let's use 'imbalanced'
              endgame: [] as number[],
              creation: [] as number[], // 'dynamic'
              solid: [] as number[] // 'dry'
            };

            timeline.forEach(t => {
              if (!t.concepts) return;
              scores.tactical.push(t.concepts.tacticalDepth || 0);
              scores.positional.push(t.concepts.imbalanced || 0); // Proxy for now
              scores.creation.push(t.concepts.dynamic || 0);
              scores.solid.push(t.concepts.dry || 0);
            });

            const getPeakScore = (arr: number[]) => {
              if (arr.length === 0) return 0;
              const sorted = [...arr].sort((a, b) => b - a);
              const top20 = Math.ceil(sorted.length * 0.2);
              if (top20 === 0) return 0;
              return sorted.slice(0, top20).reduce((a, b) => a + b, 0) / top20;
            };

            const results = [
              { id: 'tactical', score: getPeakScore(scores.tactical), label: 'Sharp Tactical', icon: '🔥', desc: 'Complex calculations dominating' },
              { id: 'creation', score: getPeakScore(scores.creation), label: 'Dynamic Attack', icon: '⚔️', desc: 'Aggressive piece play' },
              { id: 'positional', score: getPeakScore(scores.positional), label: 'Imbalanced', icon: '⚖️', desc: 'Structural battles' },
              { id: 'solid', score: getPeakScore(scores.solid), label: 'Technical Grind', icon: '🔒', desc: 'Solid, maneuvering game' }
            ].sort((a, b) => b.score - a.score);

            const winner = results[0];

            if (winner.score < 0.3) return <div className="text-xs text-white/60">Balanced / Neutral</div>;

            return (
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xl">{winner.icon}</span>
                  <span className="text-sm font-bold text-white">{winner.label}</span>
                </div>
                <div className="text-[10px] text-white/50 leading-tight">{winner.desc} (Intensity: {Math.round(winner.score * 100)})</div>
              </div>
            );
          })()}
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
            </div>
          ) : (
            <p className="text-xs text-white/60">None detected</p>
          )}
        </div>
        <div className="md:col-span-3 rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60 mb-2">Practicality by Phase</p>
          {(() => {
            const phases = {
              Opening: { count: 0, sum: 0 },
              Middlegame: { count: 0, sum: 0 },
              Endgame: { count: 0, sum: 0 }
            };

            timeline.filter(t => t.practicality).forEach(t => {
              // Use backend-provided phase, or fallback to simple ply heuristic
              const phaseRaw = t.phase || (t.ply <= 20 ? "opening" : t.ply >= 60 ? "endgame" : "middlegame");
              // Capitalize: "opening" -> "Opening"
              const phase = (phaseRaw.charAt(0).toUpperCase() + phaseRaw.slice(1)) as keyof typeof phases;

              if (phases[phase]) {
                phases[phase].count++;
                phases[phase].sum += t.practicality!.overall;
              }
            });

            return (
              <div className="grid grid-cols-3 gap-4">
                {Object.entries(phases).map(([phase, data]) => {
                  if (data.count === 0) return (
                    <div key={phase} className="text-center opacity-50">
                      <div className="text-xs text-white/40 uppercase">{phase}</div>
                      <div className="text-sm text-white/40">-</div>
                    </div>
                  );

                  const avg = data.sum / data.count;
                  const color = avg >= 0.75 ? "text-emerald-400" :
                    avg >= 0.50 ? "text-amber-400" :
                      avg >= 0.25 ? "text-orange-400" : "text-rose-400";

                  return (
                    <div key={phase} className="text-center bg-white/5 rounded-xl py-2">
                      <div className="text-[10px] text-white/60 uppercase tracking-wide mb-1">{phase}</div>
                      <div className={`text-xl font-bold ${color}`}>{avg.toFixed(2)}</div>
                      <div className="text-[10px] text-white/40">{data.count} moves</div>
                    </div>
                  );
                })}
              </div>
            );
          })()}
        </div>

      </div>
    </div>
  );
}
