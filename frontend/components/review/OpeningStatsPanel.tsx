import React from "react";
import type { OpeningStats } from "../../types/review";

interface OpeningStatsPanelProps {
  stats?: OpeningStats | null;
  loading: boolean;
  error?: string | null;
}

export function OpeningStatsPanel({ stats, loading, error }: OpeningStatsPanelProps) {
  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-4">
        <div className="animate-pulse space-y-2 text-sm text-white/60">Opening lookup…</div>
      </div>
    );
  }
  if (error) {
    return (
      <div className="glass-card rounded-2xl border border-rose-500/40 bg-rose-500/10 p-4 text-sm text-rose-100">
        Opening lookup 실패: {error}
      </div>
    );
  }
  if (!stats) {
    return (
      <div className="glass-card rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
        Opening DB 데이터를 찾지 못했습니다.
      </div>
    );
  }
  const hasMoves = stats.topMoves && stats.topMoves.length > 0;
  const hasGames = stats.topGames && stats.topGames.length > 0;
  const totalGames =
    stats.games ??
    Math.max(
      1,
      ...(stats.topMoves ?? []).map((m) => m.games ?? 0)
    );
  const totalWin = stats.winWhite != null ? stats.winWhite : null;
  const totalDraw = stats.draw != null ? stats.draw : null;
  const totalLose = stats.winBlack != null ? stats.winBlack : totalWin != null && totalDraw != null ? 1 - totalWin - totalDraw : null;

  const renderBars = (w?: number, d?: number, b?: number) => {
    const ww = w != null ? Math.max(0, Math.min(1, w)) : 0;
    const dd = d != null ? Math.max(0, Math.min(1, d)) : 0;
    const bb = b != null ? Math.max(0, Math.min(1, b)) : Math.max(0, 1 - ww - dd);
    return (
      <div className="h-2 overflow-hidden rounded-full bg-white/10">
        <div className="flex h-full w-full">
          <div className="h-full bg-emerald-400/80" style={{ width: `${ww * 100}%` }} />
          <div className="h-full bg-white/60" style={{ width: `${dd * 100}%` }} />
          <div className="h-full bg-rose-400/80" style={{ width: `${bb * 100}%` }} />
        </div>
      </div>
    );
  };

  const renderRow = (label: string, games: number, w?: number, d?: number, b?: number) => {
    return (
      <div className="rounded-xl bg-white/5 px-3 py-2">
        <div className="flex items-center justify-between text-xs text-white/70">
          <span className="font-semibold text-white">{label}</span>
          <span>
            {games} games
            {w != null ? ` · W ${(w * 100).toFixed(1)}%` : ""} {d != null ? ` · D ${(d * 100).toFixed(1)}%` : ""}
          </span>
        </div>
        <div className="mt-1 flex items-center justify-between text-[11px] text-white/60">
          <span>White</span>
          <span>Draw</span>
          <span>Black</span>
        </div>
        {renderBars(w, d, b)}
      </div>
    );
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Opening DB</p>
          <h3 className="font-display text-lg text-white">현재 국면 통계</h3>
        </div>
        <div className="flex flex-col items-end text-[11px] text-white/60">
          <span>Book ply {stats.bookPly}</span>
          <span>Novelty {stats.noveltyPly}</span>
          {stats.games ? <span>Games {stats.games}</span> : null}
        </div>
      </div>
      {hasMoves ? (
        <div className="mt-3 space-y-2">
          <div className="flex items-center justify-between text-xs uppercase tracking-[0.14em] text-white/60">
            <span>다음 수 분포</span>
            <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/70">Σ {totalGames} games</span>
          </div>
          {renderRow("Σ (All)", totalGames, totalWin ?? undefined, totalDraw ?? undefined, totalLose ?? undefined)}
          {stats.topMoves?.map((m, idx) => {
            const w = m.winPct != null ? m.winPct : undefined;
            const d = m.drawPct != null ? m.drawPct : undefined;
            const b = w != null && d != null ? Math.max(0, 1 - w - d) : undefined;
            return (
              <div key={`${m.san}-${idx}`}>
                {renderRow(m.san, m.games ?? 0, w, d, b)}
              </div>
            );
          })}
        </div>
      ) : null}
      {hasGames ? (
        <div className="mt-4 space-y-1">
          <div className="text-[11px] uppercase tracking-[0.14em] text-white/60">상위 대국</div>
          <div className="space-y-1 text-xs text-white/80">
            {stats.topGames?.slice(0, 10).map((g, idx) => (
              <div key={`${g.white}-${g.black}-${idx}`} className="rounded-lg bg-white/5 px-2 py-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white">
                    {g.white} {g.whiteElo ? `(${g.whiteElo})` : ""}
                  </span>
                  <span className="text-[11px] text-white/60">{g.date ?? ""}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white">
                    {g.black} {g.blackElo ? `(${g.blackElo})` : ""}
                  </span>
                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/80">{g.result}</span>
                </div>
                {g.event ? <div className="text-[11px] text-white/50">{g.event}</div> : null}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
