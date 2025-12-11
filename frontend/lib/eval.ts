/**
 * Format evaluation score for display.
 * @param cp Centipawn score (optional) reference from white's perspective usually, but generic here.
 * @param mate Mate score (optional) positive = winning, negative = losing.
 * @param invert If true, negates the score (useful for black's perspective if input is absolute).
 */
export function formatEvaluation(cp?: number, mate?: number, invert: boolean = false): string {
    if (mate !== undefined && mate !== null) {
        const val = invert ? -mate : mate;
        const absVal = Math.abs(val);
        return `M${val > 0 ? '' : '-'}${absVal}`; // e.g. M3, M-5
    }
    if (cp !== undefined && cp !== null) {
        const val = invert ? -cp : cp;
        const sign = val > 0 ? '+' : '';
        return `${sign}${(val / 100).toFixed(2)}`; // e.g. +1.25, -0.40
    }
    return "-";
}

/**
 * Helper to determine evaluation color class
 */
export function getEvaluationColorClass(cp?: number, mate?: number, invert: boolean = false): string {
    if (mate !== undefined && mate !== null) {
        const val = invert ? -mate : mate;
        if (val > 0) return "text-emerald-400 font-bold";
        if (val < 0) return "text-rose-400 font-bold";
    }
    if (cp !== undefined && cp !== null) {
        const val = invert ? -cp : cp;
        if (val > 100) return "text-emerald-400";
        if (val < -100) return "text-rose-400";
        return "text-neutral-300";
    }
    return "text-neutral-500";
}
