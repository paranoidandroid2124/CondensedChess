import React, { useState } from "react";

interface StarRatingProps {
    score?: number; // 0.0 - 1.0
    maxStars?: number;
    className?: string;
}

export function StarRating({ score, maxStars = 5, className = "" }: StarRatingProps) {
    const [showHelp, setShowHelp] = useState(false);

    if (score === undefined || score === null) return null;

    const filled = Math.round(score * maxStars);

    return (
        <div className="relative flex items-center gap-1.5">
            <div className={`flex items-center gap-0.5 ${className}`}>
                {Array.from({ length: maxStars }, (_, i) => (
                    <span
                        key={i}
                        className={i < filled ? "text-amber-400" : "text-white/20"}
                    >
                        {i < filled ? "★" : "☆"}
                    </span>
                ))}
            </div>
            <button
                onClick={() => setShowHelp(!showHelp)}
                className="text-white/40 hover:text-white/80 transition-colors"
                aria-label="Show explanation"
            >
                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                </svg>
            </button>
            {showHelp && (
                <div className="absolute left-0 top-full mt-1 z-50 w-56 rounded-lg bg-slate-800 border border-white/20 p-3 text-xs shadow-xl">
                    <div>
                        <span className="font-semibold text-white">Study Value: {score.toFixed(2)}/1.0</span>
                        <p className="mt-1 text-white/70">
                            Rated based on position complexity, mistake severity, and instructional value. Higher scores indicate better learning opportunities.
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
}
