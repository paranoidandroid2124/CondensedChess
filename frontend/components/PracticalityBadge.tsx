import React, { useState } from "react";
import { PracticalityScore } from "../types/review";

interface PracticalityBadgeProps {
    score: PracticalityScore;
    className?: string;
}

export function PracticalityBadge({ score, className = "" }: PracticalityBadgeProps) {
    const [showHelp, setShowHelp] = useState(false);

    const getColors = (cat: string) => {
        switch (cat) {
            case "Human-Friendly": return "bg-emerald-500/20 text-emerald-300 border-emerald-500/30";
            case "Challenging": return "bg-amber-500/20 text-amber-300 border-amber-500/30";
            case "Engine-Like": return "bg-orange-500/20 text-orange-300 border-orange-500/30";
            case "Computer-Only": return "bg-rose-500/20 text-rose-300 border-rose-500/30";
            default: return "bg-gray-500/20 text-gray-300 border-gray-500/30";
        }
    };

    const displayCategory = score.categoryPersonal || score.categoryGlobal || score.category;
    const isPersonalized = !!score.categoryPersonal;

    return (
        <div className={`flex items-center gap-2 ${className}`} title={isPersonalized ? `Global category: ${score.categoryGlobal}` : undefined}>
            <span className={`px-2 py-0.5 rounded text-xs border ${getColors(displayCategory)} flex items-center gap-1`}>
                {displayCategory}
                {isPersonalized && <span className="text-[10px] opacity-70">👤</span>}
            </span>
            <div className="relative flex items-center gap-1">
                <span className="text-xs text-white/40">
                    (Robust: {score.robustness.toFixed(2)}, Horizon: {score.horizon.toFixed(2)})
                </span>
                <button
                    onClick={() => setShowHelp(!showHelp)}
                    className="text-white/40 hover:text-white/80 transition-colors"
                    aria-label="Show explanation"
                >
                    <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                    </svg>
                </button>
                {showHelp && (
                    <div className="absolute left-0 top-full mt-1 z-50 w-64 rounded-lg bg-slate-800 border border-white/20 p-3 text-xs shadow-xl">
                        <div className="space-y-2">
                            <div>
                                <span className="font-semibold text-white">Robustness:</span>
                                <span className="text-white/70"> Score stability when playing suboptimal moves. Higher = more forgiving.</span>
                            </div>
                            <div>
                                <span className="font-semibold text-white">Horizon:</span>
                                <span className="text-white/70"> Search depth required to find the best move. Lower = easier to find.</span>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
