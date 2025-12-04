import React from "react";

interface StarRatingProps {
    score?: number; // 0.0 - 1.0
    maxStars?: number;
    className?: string;
}

export function StarRating({ score, maxStars = 5, className = "" }: StarRatingProps) {
    if (score === undefined || score === null) return null;

    const filled = Math.round(score * maxStars);

    return (
        <div
            className={`flex items-center gap-0.5 ${className}`}
            title={`Study value: ${score.toFixed(2)} (${filled}/${maxStars} stars)`}
        >
            {Array.from({ length: maxStars }, (_, i) => (
                <span
                    key={i}
                    className={i < filled ? "text-amber-400" : "text-white/20"}
                >
                    {i < filled ? "★" : "☆"}
                </span>
            ))}
        </div>
    );
}
