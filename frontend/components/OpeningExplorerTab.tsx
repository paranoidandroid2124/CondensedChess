import React from "react";
import { OpeningStats } from "../types/review";

type OpeningExplorerTabProps = {
    stats?: OpeningStats | null;
    loading: boolean;
    error?: string | null;
};

export function OpeningExplorerTab({ stats, loading, error }: OpeningExplorerTabProps) {
    if (loading) {
        return (
            <div className="animate-pulse space-y-4">
                <div className="h-8 w-1/3 rounded bg-white/10" />
                <div className="h-24 rounded-xl bg-white/5" />
                <div className="h-24 rounded-xl bg-white/5" />
            </div>
        );
    }

    if (error) {
        return (
            <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">
                {error}
            </div>
        );
    }

    if (!stats) {
        return (
            <div className="flex flex-col items-center justify-center py-12 text-center">
                <div className="text-4xl mb-2">📚</div>
                <p className="text-white/60">No opening data found for this position.</p>
            </div>
        );
    }

    const totalGames = stats.games ?? 0;
    const win = stats.winWhite ?? 0;
    const draw = stats.draw ?? 0;
    const loss = stats.winBlack ?? 0;

    return (
        <div className="space-y-6">
            {/* Header Stats */}
            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-white/80">Masters Database</h3>
                    <span className="text-xs text-white/50">{totalGames.toLocaleString()} games</span>
                </div>

                {/* Win/Draw/Loss Bar */}
                <div className="flex h-2 w-full overflow-hidden rounded-full bg-white/10">
                    <div className="bg-emerald-400" style={{ width: `${win * 100}%` }} />
                    <div className="bg-white/40" style={{ width: `${draw * 100}%` }} />
                    <div className="bg-rose-400" style={{ width: `${loss * 100}%` }} />
                </div>
                <div className="flex justify-between text-[10px] text-white/60">
                    <span>{Math.round(win * 100)}% W</span>
                    <span>{Math.round(draw * 100)}% D</span>
                    <span>{Math.round(loss * 100)}% B</span>
                </div>
            </div>

            {/* Top Moves */}
            <div className="space-y-3">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-white/40">Top Moves</h4>
                <div className="space-y-1">
                    {stats.topMoves?.map((move) => {
                        const moveTotal = move.games ?? 0;
                        const moveWin = move.winPct ?? 0;
                        const moveDraw = move.drawPct ?? 0;
                        const moveLoss = 1 - moveWin - moveDraw;

                        return (
                            <div key={move.uci} className="group relative overflow-hidden rounded-lg bg-white/5 p-2 hover:bg-white/10">
                                <div className="flex items-center justify-between relative z-10">
                                    <span className="font-bold text-white w-12">{move.san}</span>
                                    <span className="text-xs text-white/40 w-16 text-right">{moveTotal.toLocaleString()}</span>

                                    {/* Mini WDL Bar */}
                                    <div className="flex h-1.5 w-24 overflow-hidden rounded-full bg-black/20">
                                        <div className="bg-emerald-400" style={{ width: `${moveWin * 100}%` }} />
                                        <div className="bg-white/40" style={{ width: `${moveDraw * 100}%` }} />
                                        <div className="bg-rose-400" style={{ width: `${moveLoss * 100}%` }} />
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Recent Games */}
            {stats.topGames && stats.topGames.length > 0 && (
                <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-white/40">Recent Games</h4>
                    <div className="space-y-2">
                        {stats.topGames.slice(0, 5).map((game, idx) => (
                            <div key={idx} className="rounded border border-white/5 bg-white/5 p-2 text-xs">
                                <div className="flex justify-between mb-1">
                                    <span className="text-white/90">{game.white} ({game.whiteElo})</span>
                                    <span className="font-bold text-white">{game.result}</span>
                                </div>
                                <div className="flex justify-between text-white/50">
                                    <span>{game.black} ({game.blackElo})</span>
                                    <span>{game.date?.split('-')[0]}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
