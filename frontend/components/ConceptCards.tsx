import React from "react";
import { Concepts } from "../types/review";

type ConceptCardsProps = {
    concepts?: Concepts;
    prevConcepts?: Concepts;
};

export function ConceptCards({ concepts, prevConcepts }: ConceptCardsProps) {
    if (!concepts) return null;

    // Filter relevant concepts (e.g., score > 0.1) and sort by score
    const relevantConcepts = Object.entries(concepts)
        .filter(([, val]) => typeof val === 'number' && val > 0.1)
        .sort(([, a], [, b]) => (b as number) - (a as number))
        .slice(0, 3); // Top 3

    if (relevantConcepts.length === 0) return null;

    return (
        <div className="flex gap-2 mb-4 overflow-x-auto pb-1 scrollbar-hide">
            {relevantConcepts.map(([key, val]) => {
                const score = val as number;
                const prev = prevConcepts?.[key as keyof Concepts] as number | undefined;
                const delta = prev !== undefined ? score - prev : 0;

                // Determine color based on concept type (heuristic mapping)
                let colorClass = "bg-blue-500/20 text-blue-200 border-blue-500/30";
                if (key.includes("Tactical") || key.includes("Attack")) {
                    colorClass = "bg-violet-500/20 text-violet-200 border-violet-500/30";
                } else if (key.includes("King") || key.includes("Safety")) {
                    colorClass = "bg-rose-500/20 text-rose-200 border-rose-500/30";
                } else if (key.includes("Endgame") || key.includes("Pawn")) {
                    colorClass = "bg-cyan-500/20 text-cyan-200 border-cyan-500/30";
                }

                return (
                    <div
                        key={key}
                        className={`flex items-center gap-2 rounded-lg border px-3 py-1.5 text-xs font-medium whitespace-nowrap ${colorClass}`}
                    >
                        <span>{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                        <div className="flex items-center gap-0.5">
                            <span className="font-bold">{score.toFixed(2)}</span>
                            {delta !== 0 && (
                                <span className={`text-[10px] ${delta > 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                                    {delta > 0 ? '↑' : '↓'}
                                </span>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
