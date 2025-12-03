import React from "react";
import { Review, Concepts } from "../types/review";

type ConceptsTabProps = {
    review: Review | null;
    currentConcepts?: Concepts;
};

export function ConceptsTab({ review, currentConcepts }: ConceptsTabProps) {
    if (!review) return null;

    return (
        <div className="space-y-6">
            {/* Game Accuracy */}
            <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-white/10 bg-white/5 p-3 text-center">
                    <div className="text-xs uppercase tracking-wider text-white/40 mb-1">White</div>
                    <div className="text-2xl font-bold text-white">
                        {review.accuracyWhite?.toFixed(1) ?? "—"}
                        <span className="text-sm text-white/40">%</span>
                    </div>
                </div>
                <div className="rounded-xl border border-white/10 bg-white/5 p-3 text-center">
                    <div className="text-xs uppercase tracking-wider text-white/40 mb-1">Black</div>
                    <div className="text-2xl font-bold text-white">
                        {review.accuracyBlack?.toFixed(1) ?? "—"}
                        <span className="text-sm text-white/40">%</span>
                    </div>
                </div>
            </div>

            {/* Game Summary */}
            {review.summaryText && (
                <div className="rounded-xl border border-white/10 bg-white/5 p-4">
                    <h3 className="text-sm font-semibold text-white/80 mb-2">Game Summary</h3>
                    <p className="text-sm text-white/70 leading-relaxed">
                        {review.summaryText}
                    </p>
                </div>
            )}

            {/* Current Position Concepts */}
            {currentConcepts && (
                <div className="space-y-3">
                    <h3 className="text-sm font-semibold text-white/80">Current Position Concepts</h3>
                    <div className="grid gap-2">
                        {Object.entries(currentConcepts)
                            .filter(([, val]) => typeof val === 'number')
                            .sort(([, a], [, b]) => (b as number) - (a as number))
                            .slice(0, 6)
                            .map(([key, val]) => (
                                <div key={key} className="flex items-center justify-between rounded-lg bg-white/5 px-3 py-2">
                                    <span className="text-xs text-white/80 capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                                    <div className="flex items-center gap-2">
                                        <div className="h-1.5 w-16 rounded-full bg-black/20 overflow-hidden">
                                            <div
                                                className="h-full bg-accent-teal"
                                                style={{ width: `${Math.min(100, Math.max(0, (val as number) * 100))}%` }}
                                            />
                                        </div>
                                        <span className="text-xs font-mono text-accent-teal">{(val as number).toFixed(2)}</span>
                                    </div>
                                </div>
                            ))}
                    </div>
                </div>
            )}
        </div>
    );
}
