import { getTagLabel } from "../../StudyTab";
import type { StudyChapter } from "../../../types/review";

interface ChapterHeaderTableProps {
    chapter: StudyChapter;
}

export function ChapterHeaderTable({ chapter }: ChapterHeaderTableProps) {
    const moodTag = chapter.tags.find((t) => t.startsWith("mood:"))?.split(":")[1];
    const adjTag = chapter.tags.find((t) => t.startsWith("adj:"))?.split(":")[1];
    const themes = chapter.tags.filter((t) => !t.startsWith("mood:") && !t.startsWith("adj:"));

    // Helper to render stars for studyScore
    const renderStars = (score: number) => {
        const stars = Math.min(5, Math.max(1, Math.round(score || 1)));
        return "★".repeat(stars) + "☆".repeat(5 - stars);
    };

    return (
        <div className="mb-8 border border-neutral-700 rounded-sm overflow-hidden text-sm font-serif">
            {/* Header */}
            <div className="bg-neutral-800/50 p-3 border-b border-neutral-700">
                <h3 className="font-bold text-neutral-300 uppercase tracking-widest text-xs">
                    Chapter Key Points
                </h3>
            </div>

            <div className="divide-y divide-neutral-700 bg-neutral-900/30">
                {/* Theme Row */}
                <div className="grid grid-cols-[120px_1fr] divide-x divide-neutral-700">
                    <div className="p-3 text-neutral-400 font-semibold bg-neutral-800/10">Theme</div>
                    <div className="p-3 text-neutral-200">
                        {themes.length > 0 ? themes.map(getTagLabel).join(", ") : "General Strategy"}
                    </div>
                </div>

                {/* Mood/Intensity Row */}
                {(moodTag || adjTag) && (
                    <div className="grid grid-cols-[120px_1fr] divide-x divide-neutral-700">
                        <div className="p-3 text-neutral-400 font-semibold bg-neutral-800/10">Atmosphere</div>
                        <div className="p-3 text-neutral-200">
                            {moodTag && <span className="text-white font-bold mr-2">{moodTag}</span>}
                            {adjTag && <span className="text-neutral-400 italic">({adjTag})</span>}
                        </div>
                    </div>
                )}

                {/* Phase & Complexity Row */}
                <div className="grid grid-cols-[120px_1fr] divide-x divide-neutral-700">
                    <div className="p-3 text-neutral-400 font-semibold bg-neutral-800/10">Context</div>
                    <div className="p-3 text-neutral-200 flex gap-6">
                        <div>
                            <span className="text-neutral-500 mr-2 text-xs uppercase">Phase</span>
                            <span className="capitalize">{chapter.phase || "Middlegame"}</span>
                        </div>
                        {chapter.studyScore !== undefined && (
                            <div>
                                <span className="text-neutral-500 mr-2 text-xs uppercase">Impact</span>
                                <span className="text-yellow-500 text-xs tracking-widest">{renderStars(chapter.studyScore)}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* What to watch for */}
                <div className="grid grid-cols-[120px_1fr] divide-x divide-neutral-700">
                    <div className="p-3 text-neutral-400 font-semibold bg-neutral-800/10">Focus</div>
                    <div className="p-3 text-neutral-300 italic">
                        {chapter.metadata?.description || "Analyze the consequences of the key move."}
                    </div>
                </div>
            </div>
        </div>
    );
}
