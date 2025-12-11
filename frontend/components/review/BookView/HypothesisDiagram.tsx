import React from "react";
import { MiniBoard } from "../../common/MiniBoard";
import type { Branch } from "../../../types/review";

interface HypothesisDiagramProps {
    fen: string;
    hypotheses: Branch[];
    onSelectBranch?: (branch: Branch) => void;
    onHoverBranch?: (branch: Branch | null) => void;
}

/**
 * Inline diagram component that displays a mini chess board with
 * hypothesis branches (alternative moves) for critical moments.
 */
export function HypothesisDiagram({
    fen,
    hypotheses,
    onSelectBranch,
    onHoverBranch
}: HypothesisDiagramProps) {
    // Show top 3 hypotheses max
    const topHypotheses = hypotheses.slice(0, 3);

    if (topHypotheses.length === 0) return null;

    // Format winPct as percentage string
    const formatWinPct = (pct: number) => {
        if (pct >= 50) return `+${Math.round(pct - 50)}%`;
        return `-${Math.round(50 - pct)}%`;
    };

    // Get color for winPct bar
    const getBarColor = (pct: number, label: string) => {
        if (label === "best" || label === "good") return "bg-green-500";
        if (label === "mistake" || label === "blunder") return "bg-red-500";
        if (pct >= 55) return "bg-green-500";
        if (pct >= 45) return "bg-yellow-500";
        return "bg-red-500";
    };

    return (
        <div className="my-4 p-3 rounded-lg bg-neutral-800/60 border border-neutral-700/50 max-w-xs">
            {/* Mini Board */}
            <div className="w-28 h-28 mx-auto mb-3">
                <MiniBoard fen={fen} orientation="white" />
            </div>

            {/* Hypothesis Lines */}
            <div className="space-y-1.5">
                <div className="text-[10px] uppercase tracking-wider text-neutral-400 font-semibold mb-2">
                    Alternative Lines
                </div>
                {topHypotheses.map((hyp, idx) => (
                    <div
                        key={idx}
                        className="flex items-center gap-2 p-1.5 rounded hover:bg-neutral-700/50 cursor-pointer transition-colors group"
                        onClick={() => onSelectBranch?.(hyp)}
                        onMouseEnter={() => onHoverBranch?.(hyp)}
                        onMouseLeave={() => onHoverBranch?.(null)}
                    >
                        {/* Move */}
                        <span className="font-mono text-sm font-semibold text-sky-300 min-w-[40px]">
                            {hyp.move}
                        </span>

                        {/* WinPct Bar */}
                        <div className="flex-1 h-1.5 bg-neutral-700 rounded-full overflow-hidden">
                            <div
                                className={`h-full ${getBarColor(hyp.winPct, hyp.label)} transition-all`}
                                style={{ width: `${Math.min(100, Math.max(0, hyp.winPct))}%` }}
                            />
                        </div>

                        {/* WinPct Text */}
                        <span className="text-[10px] font-mono text-neutral-400 min-w-[35px] text-right">
                            {formatWinPct(hyp.winPct)}
                        </span>

                        {/* Label Badge */}
                        {hyp.label && hyp.label !== "normal" && (
                            <span className={`text-[9px] px-1 py-0.5 rounded font-medium
                                ${hyp.label === "best" ? "bg-green-900/50 text-green-300" :
                                    hyp.label === "good" ? "bg-green-900/30 text-green-400" :
                                        hyp.label === "mistake" ? "bg-red-900/50 text-red-300" :
                                            "bg-neutral-700 text-neutral-400"}
                            `}>
                                {hyp.label}
                            </span>
                        )}
                    </div>
                ))}
            </div>

            {/* PV Preview on hover could be added here */}
        </div>
    );
}
