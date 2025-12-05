import React, { useEffect, useMemo, useState } from "react";
import type { Review, ReviewTreeNode, StudyChapter } from "../../../types/review";
import { ChapterList } from "./ChapterList";
import { ChapterDetail } from "./ChapterDetail";

interface AnnotationViewProps {
    review: Review;
    rootNode?: ReviewTreeNode;
    selectedPly: number | null;
    onSelectPly: (ply: number) => void;
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

export function AnnotationView({ review, rootNode, selectedPly, onSelectPly }: AnnotationViewProps) {
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
        <div className="flex flex-col lg:flex-row gap-6 h-full">
            {/* Sidebar: Chapter List */}
            <div className="w-full lg:w-64 shrink-0">
                <ChapterList
                    chapters={chapters}
                    activeChapterPly={activeChapterPly}
                    onSelectChapter={(ply) => {
                        setActiveChapterPly(ply);
                        onSelectPly(ply); // Also jump to that position on the board
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
                    />
                ) : (
                    <div className="text-white/40 text-center py-10">
                        Select a chapter to view analysis.
                    </div>
                )}
            </div>
        </div>
    );
}
