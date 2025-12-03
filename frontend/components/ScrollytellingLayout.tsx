import React, { useEffect, useRef, useState } from "react";
import { StudyChapter } from "../types/review";
import { ChapterCard } from "./ChapterCard";

interface ScrollytellingLayoutProps {
    chapters: StudyChapter[];
    currentPly: number | null;
    onPlySelect: (ply: number) => void;
}

export function ScrollytellingLayout({ chapters, currentPly, onPlySelect }: ScrollytellingLayoutProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [activeChapterIndex, setActiveChapterIndex] = useState<number>(0);

    // Sort chapters by ply to ensure linear progression
    const sortedChapters = [...chapters].sort((a, b) => a.anchorPly - b.anchorPly);

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        const index = Number(entry.target.getAttribute("data-index"));
                        setActiveChapterIndex(index);

                        // Only auto-select if the user isn't manually interacting (heuristic)
                        // For now, we just select. We might need a "manual override" flag later.
                        const chapter = sortedChapters[index];
                        if (chapter) {
                            onPlySelect(chapter.anchorPly);
                        }
                    }
                });
            },
            {
                root: containerRef.current,
                threshold: 0.6, // Trigger when 60% of the card is visible
                rootMargin: "-20% 0px -20% 0px" // Focus on the center area
            }
        );

        const cards = document.querySelectorAll(".chapter-card");
        cards.forEach((card) => observer.observe(card));

        return () => observer.disconnect();
    }, [sortedChapters, onPlySelect]);

    // Sync active state from external ply changes (e.g. manual board navigation)
    useEffect(() => {
        if (currentPly === null) return;

        // Find the chapter that covers this ply
        // We assume chapters are sorted. The active chapter is the last one with anchorPly <= currentPly
        let foundIndex = 0;
        for (let i = 0; i < sortedChapters.length; i++) {
            if (sortedChapters[i].anchorPly <= currentPly) {
                foundIndex = i;
            } else {
                break;
            }
        }
        setActiveChapterIndex(foundIndex);

        // Optional: Scroll to that card if it's far off? 
        // Might be annoying if user is just clicking through moves.
    }, [currentPly, sortedChapters]);

    return (
        <div className="relative h-full flex flex-col">
            <div
                ref={containerRef}
                className="flex-1 overflow-y-auto pr-2 space-y-32 py-10 scroll-smooth"
            >
                {/* Intro Spacer */}
                <div className="h-[20vh] flex items-center justify-center text-center opacity-50">
                    <p className="text-sm">Scroll to explore the game story</p>
                </div>

                {sortedChapters.map((chapter, idx) => (
                    <div
                        key={chapter.id}
                        data-index={idx}
                        className="chapter-card transition-opacity duration-500"
                        style={{ opacity: activeChapterIndex === idx ? 1 : 0.4 }}
                    >
                        <ChapterCard
                            chapter={chapter}
                            isActive={activeChapterIndex === idx}
                            onClick={() => {
                                setActiveChapterIndex(idx);
                                onPlySelect(chapter.anchorPly);
                                // Scroll into view
                                document.querySelector(`[data-index="${idx}"]`)?.scrollIntoView({ behavior: "smooth", block: "center" });
                            }}
                        />
                    </div>
                ))}

                {/* Outro Spacer */}
                <div className="h-[40vh]" />
            </div>
        </div>
    );
}
