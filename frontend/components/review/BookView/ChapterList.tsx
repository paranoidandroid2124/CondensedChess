import React from "react";
import { MiniBoard } from "../../common/MiniBoard";
import type { StudyChapter } from "../../../types/review";
import { getTagLabel } from "../../StudyTab"; // Reusing tag label logic

interface ChapterListProps {
    chapters: StudyChapter[];
    activeChapterPly: number | null;
    onSelectChapter: (ply: number) => void;
}

export function ChapterList({ chapters, activeChapterPly, onSelectChapter }: ChapterListProps) {
    return (
        <div className="flex flex-col gap-2 overflow-y-auto pr-2 lg:h-[calc(100vh-16rem)] lg:w-64 shrink-0">
            <h3 className="mb-2 text-xs font-bold uppercase tracking-wider text-white/40">Chapters</h3>
            {chapters.map((chapter, idx) => {
                const isActive = activeChapterPly === chapter.anchorPly;
                return (
                    <button
                        key={chapter.anchorPly}
                        onClick={() => onSelectChapter(chapter.anchorPly)}
                        className={`group relative flex flex-col gap-2 rounded-xl border p-3 text-left transition-all ${isActive
                            ? "border-accent-teal/50 bg-accent-teal/10 shadow-[0_0_15px_-3px_rgba(45,212,191,0.2)]"
                            : "border-white/5 bg-white/5 hover:border-white/10 hover:bg-white/10"
                            }`}
                    >
                        <div className="flex items-start gap-3">
                            {/* Thumbnail */}
                            <div className="w-16 shrink-0 overflow-hidden rounded border border-white/10 shadow-lg">
                                <MiniBoard fen={chapter.fen} orientation="white" />
                            </div>

                            {/* Info */}
                            <div className="flex flex-1 flex-col gap-1 min-w-0">
                                <div className="flex items-center justify-between">
                                    <span className={`font-display text-sm font-bold ${isActive ? "text-accent-teal" : "text-white"}`}>
                                        {idx + 1}. {chapter.metadata?.name || "Critical Moment"}
                                    </span>
                                </div>

                                {/* Score Stars */}
                                {chapter.studyScore && (
                                    <div className="flex text-[10px] text-amber-400">
                                        {"★".repeat(Math.min(5, Math.round(chapter.studyScore)))}
                                        <span className="text-white/20">{"★".repeat(5 - Math.min(5, Math.round(chapter.studyScore)))}</span>
                                    </div>
                                )}

                                {/* Tags */}
                                <div className="flex flex-wrap gap-1">
                                    {chapter.tags
                                        .filter(t => !t.startsWith("adj:") && !t.startsWith("mood:"))
                                        .slice(0, 2).map((tag) => (
                                            <span key={tag} className="truncate rounded bg-white/10 px-1.5 py-0.5 text-[9px] text-white/60">
                                                {getTagLabel(tag)}
                                            </span>
                                        ))}
                                    {chapter.tags.filter(t => !t.startsWith("adj:") && !t.startsWith("mood:")).length > 2 && (
                                        <span className="rounded bg-white/10 px-1.5 py-0.5 text-[9px] text-white/60">
                                            +{chapter.tags.filter(t => !t.startsWith("adj:") && !t.startsWith("mood:")).length - 2}
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </button>
                );
            })}
        </div>
    );
}
