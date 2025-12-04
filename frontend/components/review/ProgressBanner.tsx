interface ProgressBannerProps {
    stage?: string;
    stageLabel?: string;
    totalProgress?: number;
    stageProgress?: number;
    startedAt?: number;
}

export function ProgressBanner({ stage, stageLabel, totalProgress, stageProgress, startedAt }: ProgressBannerProps) {
    const getStageIcon = (s?: string) => {
        switch (s) {
            case 'parsing': return '📄';
            case 'engine': return '⚙️';
            case 'llm': return '🤖';
            case 'concepts': return '💡';
            default: return '🔍';
        }
    };

    const progress = totalProgress ? totalProgress * 100 : 0;
    const currentStagePct = stageProgress ? Math.round(stageProgress * 100) : 0;
    const elapsed = startedAt ? Math.floor((Date.now() - startedAt) / 1000) : 0;

    return (
        <div className="rounded-xl border border-accent-teal/30 bg-gradient-to-r from-accent-teal/10 to-blue-500/10 p-3 mb-4">
            <div className="flex items-center gap-3">
                <span className="text-xl">{getStageIcon(stage)}</span>
                <div className="flex-1">
                    <div className="text-sm font-semibold text-white">
                        {stageLabel || "Analyzing game..."}
                        {currentStagePct > 0 && stage !== 'finalization' && (
                            <span className="ml-2 text-accent-teal/80 text-xs font-normal">
                                ({currentStagePct}%)
                            </span>
                        )}
                    </div>
                    <div className="text-xs text-white/60">
                        {elapsed ? `${elapsed}s elapsed` : "Processing..."}
                    </div>
                </div>
                <div className="text-sm font-medium text-accent-teal">
                    {progress > 0 ? `${Math.round(progress)}%` : "—"}
                </div>
            </div>
            <div className="mt-2 h-1 rounded-full bg-black/20 overflow-hidden">
                <div
                    className="h-full bg-gradient-to-r from-accent-teal to-blue-400 transition-all duration-500"
                    style={{ width: `${progress}%` }}
                />
            </div>
        </div>
    );
}
