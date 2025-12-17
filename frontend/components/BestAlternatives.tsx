import React, { useMemo } from "react";
import { EngineMessage } from "../lib/engine";
import { Branch } from "../types/review";
import { pvToSan } from "../lib/chess-utils";
import type { EngineStatus } from "../hooks/useEngineAnalysis";
import { SanWithIcons } from "./review/SanWithIcons";

type BestAlternativesProps = {
    lines: EngineMessage[];
    hypotheses?: Branch[];
    fen?: string;
    isAnalyzing: boolean;
    engineStatus?: EngineStatus;
    errorMessage?: string | null;
    onToggleAnalysis: () => void;
    onPreviewLine?: (pv: string) => void;  // Hover: show arrow
    onClickLine?: (pv: string) => void;    // Click: execute moves
    onOpenSettings?: () => void;
};

export function EngineAnalysisPanel({
    lines,
    fen,
    isAnalyzing,
    engineStatus,
    errorMessage,
    onToggleAnalysis,
    onPreviewLine,
    onClickLine,
    onOpenSettings
}: {
    lines: EngineMessage[];
    fen?: string;
    isAnalyzing: boolean;
    engineStatus?: EngineStatus;
    errorMessage?: string | null;
    onToggleAnalysis: () => void;
    onPreviewLine?: (pv: string) => void;
    onClickLine?: (pv: string) => void;
    onOpenSettings?: () => void;
}) {
    // Adapter to unify display with SAN conversion
    const mergedLines = useMemo(() => {
        if (!isAnalyzing) return [];
        return lines.map(l => {
            const uciPv = (l.pv || "").split(" ").filter(Boolean);
            const sanPv = fen ? pvToSan(fen, uciPv) : uciPv;
            return {
                pv: l.pv || "",
                moves: sanPv,
                scoreDisplay: l.mate
                    ? `Mate in ${Math.abs(l.mate)}`
                    : l.cp
                        ? `${l.cp > 0 ? "+" : ""}${(l.cp / 100).toFixed(2)}`
                        : "—",
                depth: l.depth,
                label: null as string | null,
                isCached: false
            };
        });
    }, [isAnalyzing, lines, fen]);

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white/80">
                    Engine Analysis
                </h3>
                <div className="flex items-center gap-2">
                    <button
                        onClick={onOpenSettings}
                        className="rounded-full border border-white/20 p-1.5 text-white/60 hover:border-white/40 hover:text-white transition"
                        title="Engine Settings"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.47a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                        </svg>
                    </button>
                    <button
                        onClick={onToggleAnalysis}
                        className={`rounded-full border px-3 py-1 text-xs transition ${isAnalyzing
                            ? "border-accent-teal bg-accent-teal/10 text-accent-teal"
                            : "border-white/20 text-white/60 hover:border-white/40 hover:text-white"
                            }`}
                    >
                        {isAnalyzing ? "Stop" : "Start"}
                    </button>
                </div>
            </div>

            {/* Error/Restarting Status */}
            {(engineStatus === "error" || engineStatus === "restarting") && (
                <div className={`rounded-xl border p-4 text-center text-sm ${engineStatus === "error"
                    ? "border-red-500/30 bg-red-500/10 text-red-400"
                    : "border-amber-500/30 bg-amber-500/10 text-amber-400"
                    }`}>
                    <div className="font-medium mb-1">
                        {engineStatus === "error" ? "⚠️ Engine Error" : "🔄 Restarting Engine..."}
                    </div>
                    {errorMessage && (
                        <div className="text-xs opacity-80">{errorMessage}</div>
                    )}
                </div>
            )}

            {!isAnalyzing ? (
                <div className="rounded-xl border border-white/10 bg-white/5 p-8 text-center text-sm text-white/40">
                    Click Start to run local Stockfish analysis
                </div>
            ) : (lines.length === 0) ? (
                <div className="rounded-xl border border-white/10 bg-white/5 p-8 text-center text-sm text-white/40">
                    Calculating...
                </div>
            ) : (
                <div className="space-y-2">
                    {mergedLines.map((line, idx) => {
                        return (
                            <div key={idx} className={`rounded-xl border p-3 transition hover:border-white/30 ${idx === 0
                                ? "border-accent-teal/30 bg-accent-teal/5"
                                : "border-white/10 bg-white/5"
                                }`}>
                                <div className="flex items-center justify-between mb-2">
                                    <div className="flex items-center gap-2">
                                        <span className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${idx === 0
                                            ? "bg-accent-teal/20 text-accent-teal"
                                            : "bg-white/10 text-white/60"
                                            }`}>
                                            {`PV${idx + 1}`}
                                        </span>
                                        <span className="font-mono text-sm font-bold text-white">
                                            {line.scoreDisplay}
                                        </span>
                                        <span className="text-[10px] text-white/40">
                                            {`depth ${line.depth}`}
                                        </span>
                                    </div>
                                </div>

                                <button
                                    onClick={() => onClickLine?.(line.pv || "")}
                                    onMouseEnter={() => onPreviewLine?.(line.pv || "")}
                                    onMouseLeave={() => onPreviewLine?.("")}
                                    className="w-full text-left text-sm text-white/80 font-medium leading-relaxed break-words hover:text-white transition flex flex-wrap gap-x-2 gap-y-1"
                                >
                                    {line.moves.map((m, i) => (
                                        <SanWithIcons key={i} move={m} />
                                    ))}
                                    {line.moves.length === 0 && "..."}
                                </button>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
