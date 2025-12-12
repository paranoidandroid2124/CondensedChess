import React from 'react';
import { PracticalityScore } from '@/types/review';

interface Props {
    practicality?: PracticalityScore;
    studyScore?: number;
    className?: string;
}

export const ScoreWidget: React.FC<Props> = ({ practicality, studyScore, className = "" }) => {
    if (!practicality && (studyScore === undefined || studyScore === null)) return null;

    // Helper for stars
    const renderStars = (score: number) => {
        // robustness is 0.0-1.0. Map to 0-5 stars.
        const stars = Math.round(score * 5);
        return "★".repeat(stars) + "☆".repeat(5 - stars);
    };

    return (
        <div className={`glass-card relative overflow-hidden rounded-2xl border border-white/10 bg-slate-900/50 p-5 ${className}`}>
            <div className="absolute inset-0 bg-gradient-to-br from-accent-teal/5 to-accent-blue/5 pointer-events-none" />

            <div className="relative z-10 space-y-4">
                <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-widest text-white/50">Analysis Scores</span>
                    <div className="h-px flex-1 bg-white/10 mx-3" />
                </div>

                {/* Practicality Section */}
                {practicality && (
                    <div className="space-y-1.5">
                        <div className="flex justify-between items-center text-sm">
                            <span className="text-white/80 font-medium">Practicality</span>
                            <span className="text-amber-400 text-base tracking-widest">
                                {renderStars(practicality.overall)}
                            </span>
                        </div>
                        <div className="flex justify-between items-center text-xs">
                            <span className="text-white/40">Mistake Tolerance</span>
                            <span className={`font-semibold ${practicality.categoryGlobal === "Human-Friendly" ? "text-green-400" :
                                    practicality.categoryGlobal === "Challenging" ? "text-amber-400" :
                                        "text-red-400"
                                }`}>
                                {practicality.categoryGlobal}
                            </span>
                        </div>
                    </div>
                )}

                {/* Divider if both exist */}
                {practicality && studyScore !== undefined && (
                    <div className="border-t border-white/10" />
                )}

                {/* Study Score */}
                {studyScore !== undefined && (
                    <div className="space-y-1.5">
                        <div className="flex justify-between items-center text-sm">
                            <span className="text-white/80 font-medium">Study Value</span>
                            <span className="font-display font-bold text-lg text-accent-blue">
                                {studyScore.toFixed(1)}
                            </span>
                        </div>
                        <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                            <div
                                className="h-full bg-gradient-to-r from-accent-blue to-accent-teal"
                                style={{ width: `${Math.min(100, Math.max(0, studyScore * 20))}%` }}
                            />
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
