import React from "react";
import { PracticalityScore } from "../types/review";

interface PracticalityBadgeProps {
    score: PracticalityScore;
    className?: string;
}

export function PracticalityBadge({ score, className = "" }: PracticalityBadgeProps) {
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
            <span className="text-xs text-white/40">
                (Robust: {score.robustness.toFixed(2)}, Horizon: {score.horizon.toFixed(2)})
            </span>
        </div>
    );
}
