import React from "react";
import { StudyChapter } from "../types/review";
import { getTagLabel, getTagColor } from "./ConceptsTab";

interface ChapterCardProps {
    chapter: StudyChapter;
    isActive: boolean;
    onClick: () => void;
}

export function ChapterCard({ chapter, isActive, onClick }: ChapterCardProps) {
    const phase = (chapter.tags.find((t: string) => ["opening", "middlegame", "endgame"].includes(t)) || "middlegame") as "opening" | "middlegame" | "endgame";

    const phaseConfigMap = {
        opening: { color: "border-blue-400/50 text-blue-300 bg-blue-400/10", icon: "📖", label: "Opening" },
        middlegame: { color: "border-amber-400/50 text-amber-300 bg-amber-400/10", icon: "⚔️", label: "Middlegame" },
        endgame: { color: "border-purple-400/50 text-purple-300 bg-purple-400/10", icon: "👑", label: "Endgame" },
    } as const;

    const phaseConfig = phaseConfigMap[phase] || { color: "border-gray-500 text-gray-400", icon: "Unknown", label: "Unknown" };

    return (
        <div
            onClick={onClick}
            className={`
        relative cursor-pointer rounded-2xl border p-6 transition-all duration-300
        ${isActive
                    ? "border-accent-teal bg-accent-teal/10 shadow-[0_0_30px_-5px_rgba(45,212,191,0.15)] scale-[1.02]"
                    : "border-white/5 bg-white/5 hover:bg-white/10 hover:border-white/10"
                }
      `}
        >
            {/* Phase Badge */}
            <div className="mb-4 flex items-center justify-between">
                <span className={`flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium ${phaseConfig.color}`}>
                    <span>{phaseConfig.icon}</span>
                    <span>{phaseConfig.label}</span>
                </span>
                <span className="text-xs font-mono text-white/40">Ply {chapter.anchorPly}</span>
            </div>

            {/* Narrative Text */}
            <div className="prose prose-invert prose-sm max-w-none mb-4">
                <p className="text-white/90 leading-relaxed text-base">
                    {chapter.summary || "No summary available for this chapter."}
                </p>
            </div>

            {/* Tags */}
            <div className="flex flex-wrap gap-2">
                {chapter.tags
                    .filter((t: string) => !["opening", "middlegame", "endgame"].includes(t))
                    .map((tag: string) => (
                        <span
                            key={tag}
                            className={`rounded px-2 py-0.5 text-xs font-medium border ${getTagColor(tag)}`}
                        >
                            {getTagLabel(tag)}
                        </span>
                    ))}
            </div>

            {/* Active Indicator */}
            {isActive && (
                <div className="absolute -left-3 top-1/2 h-12 w-1 -translate-y-1/2 rounded-full bg-accent-teal shadow-[0_0_10px_#2dd4bf]" />
            )}
        </div>
    );
}
