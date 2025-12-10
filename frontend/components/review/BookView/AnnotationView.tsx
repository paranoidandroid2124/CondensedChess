import React, { useEffect, useMemo, useState } from "react";
import type { Review, ReviewTreeNode, StudyChapter, Book } from "../../../types/review";
import { ChapterList } from "./ChapterList";
import { ChapterDetail } from "./ChapterDetail";
import { BookViewContainer } from "./BookViewContainer";

interface AnnotationViewProps {
    review: Review;
    rootNode?: ReviewTreeNode;
    selectedPly: number | null;
    onSelectPly: (ply: number) => void;
    onPreviewFen?: (fen: string | null) => void;
    onSelectNode?: (node: ReviewTreeNode) => void;
}

// Helper to find a node in the tree by ply
function findNodeByPly(root: ReviewTreeNode, ply: number): ReviewTreeNode | null {
    if (root.ply === ply) return root;
    if (root.children) {
        for (const child of root.children) {
            const found = findNodeByPly(child, ply);
            if (found) return found;
        }
    }
    return null;
}

// Legacy component for StudyChapter-based rendering
function LegacyAnnotationView({
    review,
    rootNode,
    selectedPly,
    onSelectPly,
    onPreviewFen
}: AnnotationViewProps) {
    // Sort chapters by Ply (chronological order)
    const chapters = useMemo(() => {
        const chaps = review?.studyChapters || [];
        return [...chaps].sort((a, b) => a.anchorPly - b.anchorPly);
    }, [review?.studyChapters]);

    const [activeChapterPly, setActiveChapterPly] = useState<number>(chapters[0]?.anchorPly || 0);

    // Sync active chapter when chapters change
    useEffect(() => {
        if (chapters.length > 0 && !chapters.find(c => c.anchorPly === activeChapterPly)) {
            setActiveChapterPly(chapters[0].anchorPly);
        }
    }, [chapters, activeChapterPly]);

    const activeChapter = chapters.find(c => c.anchorPly === activeChapterPly) || chapters[0];

    // Find the actual tree node for this chapter's anchor
    const chapterRootNode = rootNode ? findNodeByPly(rootNode, activeChapter?.anchorPly || 0) : null;

    if (!activeChapter) {
        return (
            <div className="flex items-center justify-center h-full text-white/40">
                No study chapters available.
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-3 h-full">
            {/* Color Legend */}
            <div className="flex items-center gap-3 px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-xs">
                <span className="text-white/60 font-semibold">Node Colors:</span>
                <div className="flex items-center gap-1">
                    <span className="w-3 h-3 rounded bg-accent-teal/50"></span>
                    <span className="text-white/70">Mainline</span>
                </div>
                <div className="flex items-center gap-1">
                    <span className="w-3 h-3 rounded bg-amber-500/50"></span>
                    <span className="text-white/70">Critical</span>
                </div>
                <div className="flex items-center gap-1">
                    <span className="w-3 h-3 rounded bg-purple-400/50"></span>
                    <span className="text-white/70">Practical</span>
                </div>
                <div className="flex items-center gap-1">
                    <span className="w-3 h-3 rounded bg-white/20"></span>
                    <span className="text-white/70">Sideline</span>
                </div>
            </div>

            <div className="flex flex-col lg:flex-row gap-6 flex-1">
                {/* Sidebar: Chapter List */}
                <div className="w-full lg:w-64 shrink-0">
                    <ChapterList
                        chapters={chapters}
                        activeChapterPly={activeChapterPly}
                        onSelectChapter={(ply) => {
                            setActiveChapterPly(ply);
                            onSelectPly(ply);
                        }}
                    />
                </div>

                {/* Main Content: Chapter Detail */}
                <div className="flex-1 overflow-y-auto pr-2 lg:h-[calc(100vh-16rem)]">
                    {chapterRootNode ? (
                        <ChapterDetail
                            chapter={activeChapter}
                            rootNode={chapterRootNode}
                            onSelectPly={onSelectPly}
                            currentPly={selectedPly}
                            onPreviewFen={onPreviewFen}
                        />
                    ) : (
                        <div className="text-white/40 text-center py-10">
                            Select a chapter to view analysis.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

// Main export - routes to BookView or Legacy based on data availability
export function AnnotationView({ review, rootNode, selectedPly, onSelectPly, onPreviewFen, onSelectNode }: AnnotationViewProps) {
    // If Phase 4.6 Book is available, use the new BookView
    if (review.book) {
        return (
            <div className="h-full overflow-y-auto">
                <BookViewContainer
                    book={review.book}
                    root={rootNode}
                    onSelectPly={onSelectPly}
                    onPreviewFen={onPreviewFen}
                    onSelectNode={onSelectNode}
                />
            </div>
        );
    }

    // Fallback to legacy StudyChapter-based view
    return (
        <LegacyAnnotationView
            review={review}
            rootNode={rootNode}
            selectedPly={selectedPly}
            onSelectPly={onSelectPly}
            onPreviewFen={onPreviewFen}
        />
    );
}
