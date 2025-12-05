import React, { useState } from "react";
import { MiniBoard } from "../../common/MiniBoard";
import type { ReviewTreeNode, StudyChapter, StudyLine } from "../../../types/review";
import { formatSanHuman } from "../../../lib/review-format";
import { getTagLabel } from "../../StudyTab";
import { HorizontalTreeView } from "./HorizontalTreeView";
import { TreeModal } from "../TreeModal";

interface ChapterDetailProps {
    chapter: StudyChapter;
    rootNode: ReviewTreeNode; // The anchor node of the chapter
    onSelectPly: (ply: number) => void;
    currentPly: number | null;
}

export function ChapterDetail({ chapter, rootNode, onSelectPly, currentPly }: ChapterDetailProps) {
    const [showTreeModal, setShowTreeModal] = useState(false);

    return (
        <div className="flex flex-col gap-4 pb-20">
            {/* Compact Header */}
            <div className="flex gap-4 border-b border-white/10 pb-4">
                <div className="w-40 shrink-0">
                    <MiniBoard fen={chapter.fen} orientation="white" className="shadow-lg shadow-black/50" />
                </div>
                <div className="flex flex-col gap-2 flex-1 min-w-0">
                    {/* Title and Badge Row */}
                    <div className="flex items-center gap-2 flex-wrap">
                        <span className="rounded bg-accent-teal/20 px-2 py-0.5 text-xs font-bold text-accent-teal">
                            Chapter Start
                        </span>
                        {chapter.phase && (
                            <span className="rounded bg-white/10 px-2 py-0.5 text-xs font-medium text-white/80 capitalize">
                                {chapter.phase}
                            </span>
                        )}
                        <span className="text-xs text-yellow-400 font-semibold">
                            ⭐ {(chapter.studyScore ?? 0).toFixed(1)}/5
                        </span>
                        {chapter.winPctBefore !== undefined && chapter.winPctAfter !== undefined && (
                            <span className={`text-xs font-medium ${chapter.winPctAfter > chapter.winPctBefore ? 'text-green-400' : 'text-red-400'}`}>
                                {(chapter.winPctAfter - chapter.winPctBefore) > 0 ? '+' : ''}
                                {(chapter.winPctAfter - chapter.winPctBefore).toFixed(1)}% Win%
                            </span>
                        )}
                    </div>

                    <h2 className="font-display text-xl font-bold text-white leading-tight">{chapter.metadata?.name || "Chapter"}</h2>

                    {/* Summary */}
                    <p className="text-sm text-white/70 leading-snug">{chapter.metadata?.description || chapter.summary || "No description available."}</p>

                    {/* Takeaway Section - More Compact */}
                    {chapter.summary && chapter.summary.includes("Takeaway:") && (
                        <div className="p-2.5 rounded-lg bg-yellow-500/10 border border-yellow-500/20">
                            <div className="flex items-start gap-2">
                                <span className="text-[10px] font-bold text-yellow-300 bg-yellow-500/20 px-1.5 py-0.5 rounded shrink-0">
                                    Takeaway
                                </span>
                                <p className="text-xs text-yellow-100/90 leading-snug flex-1">
                                    {chapter.summary.split("Takeaway:")[1]?.trim() || ""}
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Tags */}
                    <div className="flex flex-wrap gap-1.5">
                        {chapter.tags.map(tag => (
                            <span key={tag} className="rounded-full border border-white/10 px-2 py-0.5 text-[10px] text-white/60">
                                {getTagLabel(tag)}
                            </span>
                        ))}
                    </div>
                </div>
            </div>

            {/* Narrative Flow - Now outside header, uses full width */}
            <div className="flex flex-col gap-3 overflow-x-auto pb-4 relative">
                {chapter.rootNode ? (
                    <div className="min-w-full p-4 relative">
                        <button
                            onClick={() => setShowTreeModal(true)}
                            className="absolute top-2 right-2 z-10 p-1.5 rounded-lg bg-white/10 text-white/60 hover:text-white hover:bg-white/20 transition-colors"
                            title="Expand Tree View"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                            </svg>
                        </button>
                        <HorizontalTreeView
                            rootNode={chapter.rootNode}
                            currentPly={currentPly}
                            onSelectPly={onSelectPly}
                            isRoot={true}
                        />
                    </div>
                ) : (
                    <div className="text-white/60 text-center py-8">
                        No tree data available for this chapter.
                    </div>
                )}
            </div>

            {showTreeModal && chapter.rootNode && (
                <TreeModal
                    rootNode={chapter.rootNode}
                    currentPly={currentPly}
                    onSelectPly={onSelectPly}
                    onClose={() => setShowTreeModal(false)}
                />
            )}
        </div>
    );
}
