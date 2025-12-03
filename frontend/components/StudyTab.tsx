import React from "react";
import { StudyChapter } from "../types/review";

type StudyTabProps = {
    chapters?: StudyChapter[];
    onSelectChapter: (ply: number) => void;
    onStartGuess?: (chapter: StudyChapter) => void;
};

export function StudyTab({ chapters, onSelectChapter, onStartGuess }: StudyTabProps) {
    if (!chapters || chapters.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-12 text-center">
                <div className="text-4xl mb-2">🎓</div>
                <p className="text-white/60">No study chapters generated for this game.</p>
                <p className="text-xs text-white/40 mt-2">Try analyzing a game with more mistakes!</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white/80">Key Moments</h3>
                <span className="rounded-full bg-accent-teal/10 px-2 py-0.5 text-xs text-accent-teal">
                    {chapters.length} chapters
                </span>
            </div>

            <div className="space-y-3">
                {[...chapters].sort((a, b) => a.anchorPly - b.anchorPly).map((chapter, idx) => (
                    <div
                        key={chapter.id}
                        className="group relative overflow-hidden rounded-xl border border-white/10 bg-white/5 p-4 transition hover:border-accent-teal/50 hover:bg-white/10"
                    >
                        <div className="flex items-start justify-between mb-2">
                            <div className="flex items-center gap-2">
                                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-white/10 text-[10px] font-bold text-white/60">
                                    {idx + 1}
                                </span>
                                <span className="text-sm font-bold text-white">Ply {chapter.anchorPly}</span>
                            </div>
                            <span className={`text-xs font-bold ${chapter.deltaWinPct < 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                                {chapter.deltaWinPct > 0 ? '+' : ''}{chapter.deltaWinPct.toFixed(1)}%
                            </span>
                        </div>

                        <p className="text-xs text-white/70 line-clamp-2 mb-3">
                            {chapter.summary || "No summary available."}
                        </p>

                        <div className="flex items-center justify-between mt-3">
                            <div className="flex flex-wrap gap-1.5">
                                {chapter.tags.slice(0, 3).map(tag => (
                                    <span key={tag} className="rounded bg-black/20 px-1.5 py-0.5 text-[10px] text-white/50">
                                        {tag.replace(/_/g, ' ')}
                                    </span>
                                ))}
                            </div>

                            <div className="flex gap-2">
                                <button
                                    onClick={() => onSelectChapter(chapter.anchorPly)}
                                    className="rounded px-2 py-1 text-xs text-white/60 hover:bg-white/10 hover:text-white"
                                >
                                    View
                                </button>
                                {onStartGuess && (
                                    <button
                                        onClick={() => onStartGuess(chapter)}
                                        className="rounded bg-accent-teal/20 px-2 py-1 text-xs font-bold text-accent-teal hover:bg-accent-teal/30"
                                    >
                                        Train
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
