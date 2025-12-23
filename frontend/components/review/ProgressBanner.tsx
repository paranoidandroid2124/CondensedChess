import { useEffect, useState } from "react";

interface ProgressBannerProps {
    stage?: string;
    stageLabel?: string;
    totalProgress?: number;
    stageProgress?: number;
    startedAt?: number;
}

const FLAVOR_TEXTS = [
    "Consulting Stockfish...",
    "Analyzing critical moments...",
    "Finding missed wins...",
    "Reviewing blunders...",
    "Writing narrative...",
    "Adding drama...",
    "Polishing masterclass...",
    "Double-checking calculations..."
];

export function ProgressBanner({ stage, stageLabel, totalProgress, stageProgress, startedAt }: ProgressBannerProps) {
    const [flavorIndex, setFlavorIndex] = useState(0);

    useEffect(() => {
        const interval = setInterval(() => {
            setFlavorIndex(prev => (prev + 1) % FLAVOR_TEXTS.length);
        }, 3000);
        return () => clearInterval(interval);
    }, []);

    const getStageIcon = (s?: string) => {
        switch (s) {
            case 'parsing': return '📄';
            case 'engine': return '⚙️';
            case 'llm': return '📝';
            case 'concepts': return '💡';
            default: return '🔍';
        }
    };

    const progress = totalProgress ? totalProgress * 100 : 0;
    const currentStagePct = stageProgress ? Math.round(stageProgress * 100) : 0;
    const isIndeterminate = !totalProgress || totalProgress <= 0;

    // Fix: Ensure startedAt is a valid timestamp (e.g., > year 2000) to avoid showing epoch delta
    const isValidStart = startedAt && startedAt > 946684800000;
    const elapsed = isValidStart ? Math.floor((Date.now() - startedAt!) / 1000) : 0;

    // Map backend raw strings to friendly labels
    const getFriendlyLabel = (s?: string) => {
        if (!s) return FLAVOR_TEXTS[flavorIndex];

        // Normalize
        const lower = s.toLowerCase();

        if (lower.includes('engine') || lower.includes('stockfish')) return "Consulting Stockfish...";
        if (lower.includes('parsing') || lower.includes('fen')) return "Reading Game...";
        if (lower.includes('llm') || lower.includes('narrative')) return "Writing Commentary...";
        if (lower.includes('concept') || lower.includes('cluster')) return "Organizing Concepts...";
        if (lower.includes('classif') || lower.includes('blunder')) return "Checking Accuracy...";
        if (lower.includes('key')) return "Finding Key Moments...";
        if (lower === 'unknown') return FLAVOR_TEXTS[flavorIndex]; // Fallback to flavor text for 'unknown'

        return stageLabel || s; // Use provided label or raw string if no match
    };

    const effectiveLabel = getFriendlyLabel(stage);

    return (
        <div className="rounded-xl border border-accent-teal/30 bg-gradient-to-r from-accent-teal/10 to-blue-500/10 p-4 mb-4 shadow-lg shadow-accent-teal/5 relative overflow-hidden">
            {/* Indeterminate Shimmer Background */}
            {isIndeterminate && (
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
            )}

            <div className="flex items-center gap-4 relative z-10">
                <div className="p-2 rounded-full bg-accent-teal/20 text-xl animate-pulse">
                    {getStageIcon(stage)}
                </div>
                <div className="flex-1">
                    <div className="text-sm font-bold text-white tracking-wide flex items-center gap-2">
                        {effectiveLabel}
                        {currentStagePct > 0 && stage !== 'finalization' && (
                            <span className="px-1.5 py-0.5 rounded bg-accent-teal/20 text-accent-teal text-[10px] font-mono">
                                STEP {currentStagePct}%
                            </span>
                        )}
                    </div>
                    <div className="text-xs text-white/50 mt-1 font-mono">
                        {elapsed ? `${elapsed}s elapsed` : "Preparing..."}
                    </div>
                </div>
                <div className="text-lg font-bold text-accent-teal font-mono">
                    {progress > 0 ? `${Math.round(progress)}%` : <span className="animate-pulse">...</span>}
                </div>
            </div>

            <div className="mt-3 h-1.5 rounded-full bg-black/40 overflow-hidden relative">
                {isIndeterminate ? (
                    <div className="absolute inset-0 w-1/3 bg-gradient-to-r from-transparent via-accent-teal to-transparent animate-[indeterminate_1.5s_infinite_linear]" />
                ) : (
                    <div
                        className="h-full bg-gradient-to-r from-accent-teal to-blue-400 transition-all duration-500 shadow-[0_0_10px_rgba(45,212,191,0.5)]"
                        style={{ width: `${progress}%` }}
                    />
                )}
            </div>
        </div>
    );
}
