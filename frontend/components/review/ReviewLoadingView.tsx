import { MoveControls } from "./MoveControls";
import { BoardCard } from "./BoardCard";
import type { EnhancedTimelineNode } from "../../lib/review-derived";

export function ReviewLoadingView({
  loading,
  pendingMessage,
  elapsed,
  progress,
  progressInfo,
  instantTimeline,
  selectedPly,
  setSelectedPly
}: {
  loading: boolean;
  pendingMessage: string | null;
  elapsed: number;
  progress: number;
  progressInfo: { stage?: string; stageLabel?: string; totalProgress?: number } | null;
  instantTimeline: EnhancedTimelineNode[] | null;
  selectedPly: number | null;
  setSelectedPly: (ply: number | null) => void;
}) {
  if (instantTimeline?.length) {
    const movesOnly = instantTimeline.filter((t) => t.ply > 0);
    const grouped = [];
    for (let i = 0; i < movesOnly.length; i += 2) {
      grouped.push({ moveNumber: Math.floor(i / 2) + 1, white: movesOnly[i], black: movesOnly[i + 1] });
    }
    const selectedInstant =
      instantTimeline.find((t) => t.ply === selectedPly) ?? instantTimeline[instantTimeline.length - 1];
    const boardFen = selectedInstant?.fen ?? instantTimeline[instantTimeline.length - 1]?.fen;

    return (
      <div className="px-6 py-10 sm:px-12 lg:px-16">
        <div className="mx-auto max-w-6xl space-y-6">
          <div className="rounded-2xl border border-accent-teal/30 bg-accent-teal/10 p-4">
            <div className="flex items-center gap-3">
              <div className="flex gap-1">
                {loading ? (
                  <>
                    <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse" />
                    <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.2s]" />
                    <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.4s]" />
                  </>
                ) : (
                  <div className="w-2 h-2 rounded-full bg-amber-400" />
                )}
              </div>
              <div className="flex-1">
                <div className="text-sm font-semibold text-white">
                  {loading ? "Analysis in progress..." : "Analysis delayed"}
                </div>
                <div className="text-xs text-white/70">
                  {loading
                    ? `Board is ready. Advanced features will appear when complete. (${elapsed}s)`
                    : pendingMessage || "Still processing. You can refresh later."}
                </div>
              </div>
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <BoardCard
              fen={boardFen || undefined}
              evalPercent={undefined}
              judgementBadge={undefined}
            />
            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <h3 className="text-sm font-semibold text-white/80 mb-3">Game Moves</h3>
                <div className="max-h-96 overflow-y-auto text-sm text-white/70">
                  {grouped.map((row) => (
                    <div key={row.moveNumber} className="flex items-center gap-2 py-1">
                      <span className="w-6 text-right text-white/50">{row.moveNumber}.</span>
                      <button
                        className={`rounded px-2 py-1 transition ${selectedPly === row.white?.ply ? "bg-white/10 text-white" : "hover:bg-white/5"
                          }`}
                        onClick={() => row.white && setSelectedPly(row.white.ply)}
                      >
                        {row.white?.san}
                      </button>
                      {row.black ? (
                        <button
                          className={`rounded px-2 py-1 transition ${selectedPly === row.black.ply ? "bg-white/10 text-white" : "hover:bg-white/5"
                            }`}
                          onClick={() => setSelectedPly(row.black!.ply)}
                        >
                          {row.black.san}
                        </button>
                      ) : (
                        <span className="px-2 py-1 text-white/40">—</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-center text-sm text-white/60">
                <p>💡 You can use the local engine to analyze positions while waiting</p>
              </div>
              <MoveControls timeline={instantTimeline} selected={selectedPly ?? undefined} onSelect={setSelectedPly} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  const getStageIcon = (stage?: string) => {
    switch (stage) {
      case 'parsing': return '📄';
      case 'engine': return '⚙️';
      case 'llm': return '🤖';
      case 'concepts': return '💡';
      default: return '🔍';
    }
  };

  return (
    <div className="px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto max-w-3xl space-y-6">
        <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-semibold text-white">Analyzing Game...</h2>
            <span className="text-sm text-white/60">{elapsed}s elapsed</span>
          </div>

          <div className="relative h-2 rounded-full bg-black/20 overflow-hidden mb-4">
            <div
              className="absolute top-0 left-0 h-full bg-gradient-to-r from-accent-teal to-blue-400 transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>

          {progressInfo?.stageLabel || pendingMessage ? (
            <div className="flex items-center gap-3 text-white/80">
              <span className="text-2xl">{getStageIcon(progressInfo?.stage)}</span>
              <div className="flex-1">
                <div className="text-sm font-semibold">
                  {progressInfo?.stageLabel || pendingMessage}
                </div>
                {progressInfo?.stage && (
                  <div className="text-xs text-white/50">Stage: {progressInfo.stage}</div>
                )}
              </div>
              <div className="flex gap-1">
                <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse" />
                <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.2s]" />
                <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.4s]" />
              </div>
            </div>
          ) : null}

          {elapsed < 30 && (
            <p className="mt-4 text-xs text-white/50">
              Typically completes in 30 seconds
            </p>
          )}
        </div>

        <div className="animate-pulse space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="h-96 rounded-2xl bg-white/5" />
            <div className="h-96 rounded-2xl bg-white/5" />
          </div>
        </div>
      </div>
    </div>
  );
}

