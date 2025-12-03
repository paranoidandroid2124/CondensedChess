import React from "react";
import { EngineMessage } from "../lib/engine";

type BestAlternativesProps = {
    lines: EngineMessage[];
    isAnalyzing: boolean;
    onToggleAnalysis: () => void;
    onPreviewLine?: (pv: string) => void;
};

export function BestAlternatives({
    lines,
    isAnalyzing,
    onToggleAnalysis,
    onPreviewLine
}: BestAlternativesProps) {
    // Sort lines by score (descending for White, ascending for Black? Usually engine returns sorted)
    // Stockfish usually sends lines in order if MultiPV is set, but we might receive them individually.
    // For now, assume `lines` is a list of the latest unique PVs or just the latest single message if not accumulating.
    // In ReviewClient, we are currently doing `setEngineLines((prev) => [msg])` which replaces it.
    // We need to support MultiPV in ReviewClient to get multiple lines.
    // For this component, let's assume we get a list of lines.

    return (
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white/80">Engine Analysis</h3>
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

            {!isAnalyzing ? (
                <div className="rounded-xl border border-white/10 bg-white/5 p-8 text-center text-sm text-white/40">
                    Click Start to see engine lines
                </div>
            ) : lines.length === 0 ? (
                <div className="rounded-xl border border-white/10 bg-white/5 p-8 text-center text-sm text-white/40">
                    Calculating...
                </div>
            ) : (
                <div className="space-y-2">
                    {lines.map((line, idx) => {
                        const score = line.mate
                            ? `Mate in ${Math.abs(line.mate)}`
                            : line.cp
                                ? `${line.cp > 0 ? "+" : ""}${(line.cp / 100).toFixed(2)}`
                                : "—";

                        // Simple visual bar for evaluation
                        const evalVal = line.cp ? Math.max(-500, Math.min(500, line.cp)) : 0;
                        const barWidth = Math.abs(evalVal) / 5; // 0 to 100% roughly
                        const barColor = evalVal > 0 ? "bg-white" : "bg-black";

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
                                            PV{idx + 1}
                                        </span>
                                        <span className="font-mono text-sm font-bold text-white">
                                            {score}
                                        </span>
                                        <span className="text-[10px] text-white/40">
                                            depth {line.depth}
                                        </span>
                                    </div>
                                </div>

                                <button
                                    onClick={() => onPreviewLine?.(line.pv || "")}
                                    className="w-full text-left text-sm text-white/80 font-medium leading-relaxed break-words hover:text-white transition"
                                >
                                    {line.pv || "..."}
                                </button>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
