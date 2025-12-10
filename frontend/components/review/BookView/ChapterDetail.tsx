import React, { useState } from "react";
import { MiniBoard } from "../../common/MiniBoard";
import type { ReviewTreeNode, StudyChapter, StudyLine } from "../../../types/review";
import { formatSanHuman } from "../../../lib/review-format";
import { getTagLabel } from "../../StudyTab";
import { TreeModal } from "../TreeModal";
// Book Style Components
import { PaperLayout } from "./PaperLayout";
import { ChapterHeaderTable } from "./ChapterHeaderTable";
import { NarrativeRenderer } from "./NarrativeRenderer";

interface ChapterDetailProps {
    chapter: StudyChapter;
    rootNode: ReviewTreeNode; // The anchor node of the chapter
    onSelectPly: (ply: number) => void;
    currentPly: number | null;
    onPreviewFen?: (fen: string | null) => void;
}

// Basic markdown parser for bold text
function renderMarkdown(text: string): string {
    if (!text) return "";
    // Replace **text** with <strong>text</strong> (Global flag 'g' ensures all instances are replaced)
    let html = text.replace(/\*\*(.*?)\*\*/g, '<strong class="text-white font-bold">$1</strong>');

    // Clean up double spaces
    html = html.replace(/\s+/g, ' ');
    return html;
}

export function ChapterDetail({ chapter, rootNode, onSelectPly, currentPly, onPreviewFen }: ChapterDetailProps) {
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
                    <p className="text-sm text-white/70 leading-snug" dangerouslySetInnerHTML={{ __html: renderMarkdown(chapter.metadata?.description || chapter.summary || "No description available.") }} />

                    {/* Takeaway Section - More Compact */}
                    {chapter.summary && chapter.summary.includes("Takeaway:") && (
                        <div className="p-2.5 rounded-lg bg-yellow-500/10 border border-yellow-500/20">
                            <div className="flex items-start gap-2">
                                <span className="text-[10px] font-bold text-yellow-300 bg-yellow-500/20 px-1.5 py-0.5 rounded shrink-0">
                                    Takeaway
                                </span>
                                <p className="text-xs text-yellow-100/90 leading-snug flex-1" dangerouslySetInnerHTML={{ __html: renderMarkdown(chapter.summary.split("Takeaway:")[1]?.trim() || "") }} />
                            </div>
                        </div>
                    )}

                    {/* Tags */}
                    <div className="flex flex-wrap gap-1.5">
                        {chapter.tags
                            .filter(t => !t.startsWith("adj:") && !t.startsWith("mood:"))
                            .map(tag => (
                                <span key={tag} className="rounded-full border border-white/10 px-2 py-0.5 text-[10px] text-white/60">
                                    {getTagLabel(tag)}
                                </span>
                            ))}
                    </div>
                </div>
            </div>

            {/* Narrative Flow - Book Style */}
            <div className="flex-1 min-h-0 bg-neutral-900/50 overflow-y-auto">
                <PaperLayout>
                    <ChapterHeaderTable chapter={chapter} />

                    {/* Narrative Flow */}
                    <div className="space-y-6">
                        <h2 className="font-bold text-2xl text-neutral-100 mb-6 border-b border-neutral-700 pb-2">
                            {chapter.metadata?.name || "Analysis"}
                        </h2>

                        {/* Root Node starts the chain */}
                        {(chapter.rootNode || rootNode) && (
                            <NarrativeRenderer
                                node={chapter.rootNode || rootNode}
                                onInteract={(node) => {
                                    // Open modal when clicking notation
                                    console.log("Interact node:", node);
                                    if (onPreviewFen && node.fen) onPreviewFen(node.fen);
                                    // Also allow TreeModal if needed, but PreviewFen is priority for user request
                                    setShowTreeModal(true);
                                }}
                                diagrams={chapter.diagrams}
                            />
                        )}
                    </div>
                </PaperLayout>
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
