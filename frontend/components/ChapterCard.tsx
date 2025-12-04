import React from "react";
import { StudyChapter } from "../types/review";
import { getTagLabel, getTagColor } from "./ConceptsTab";
import { PracticalityBadge } from "./PracticalityBadge";
import { StarRating } from "./StarRating";

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

    // Extract Theme/Takeaway hints from summary if present
    const parseSummary = (text?: string) => {
        if (!text) return { body: "", theme: "", takeaway: "" };
        let remaining = text.trim();
        let theme = "";
        let takeaway = "";
        const themeMatch = remaining.match(/Theme:\s*(.+?)(?:\s*Takeaway:|$)/i);
        if (themeMatch) {
            theme = themeMatch[1].trim();
            remaining = remaining.replace(themeMatch[0], "").trim();
        }
        const takeawayMatch = remaining.match(/Takeaway:\s*(.+)$/i);
        if (takeawayMatch) {
            takeaway = takeawayMatch[1].trim();
            remaining = remaining.replace(takeawayMatch[0], "").trim();
        } else {
            // Fallback: use the last sentence as takeaway if none provided explicitly
            const sentences = remaining.split(/(?<=\.)\s+/).filter(Boolean);
            if (sentences.length > 1) {
                takeaway = sentences.pop()?.trim() ?? "";
                remaining = sentences.join(" ").trim();
            }
        }
        return { body: remaining, theme, takeaway };
    };

    const { body, theme, takeaway } = parseSummary(chapter.summary);

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
                <div className="flex items-center gap-2">
                    <span className={`flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium ${phaseConfig.color}`}>
                        <span>{phaseConfig.icon}</span>
                        <span>{phaseConfig.label}</span>
                    </span>
                    {chapter.practicality && <PracticalityBadge score={chapter.practicality} />}
                    {chapter.studyScore !== undefined && <StarRating score={chapter.studyScore} />}
                </div>
                <span className="text-xs font-mono text-white/40">Ply {chapter.anchorPly}</span>
            </div>

            {/* Narrative Text */}
            <div className="prose prose-invert prose-sm max-w-none mb-4">
                <p className="text-white/90 leading-relaxed text-base">
                    {body || chapter.summary || "No summary available for this chapter."}
                </p>
                {(theme || takeaway) && (
                    <div className="mt-2 space-y-1">
                        {theme ? (
                            <div className="flex items-start gap-2 text-sm text-white/80">
                                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-accent-teal/80">Theme</span>
                                <span className="leading-snug">{theme}</span>
                            </div>
                        ) : null}
                        {takeaway ? (
                            <div className="flex items-start gap-2 text-sm text-white/80">
                                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-amber-200/90">Takeaway</span>
                                <span className="leading-snug">{takeaway}</span>
                            </div>
                        ) : null}
                    </div>
                )}
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
