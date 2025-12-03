import React from "react";
import { StudyChapter } from "../types/review";

type StudyTabProps = {
    chapters?: StudyChapter[];
    onSelectChapter: (ply: number) => void;
    onStartGuess?: (chapter: StudyChapter) => void;
};

const tagLabelMap: Record<string, string> = {
    // Piece quality
    "bad_bishop": "Restricted Bishop",
    "good_knight_outpost": "Strong Knight",
    "active_rooks": "Active Rooks",

    // King safety
    "king_exposed": "King in Danger",
    "king_attack_ready": "King Attack",
    "king_stuck_center": "Uncastled King",
    "weak_back_rank": "Weak Back Rank",

    // Positional themes
    "fortress_building": "Fortress",
    "space_advantage": "Space Advantage",
    "color_complex_weakness": "Color Weakness",
    "material_imbalance": "Material Imbalance",

    // Pawn structure
    "pawn_storm": "Pawn Storm",
    "pawn_storm_against_castled_king": "Attacking Pawns",
    "isolated_d_pawn": "Isolated d-Pawn",
    "weak_f7": "Weak f7",
    "weak_f2": "Weak f2",

    // Tactical/Strategic
    "tactical_complexity": "Tactical Position",
    "high_blunder_risk": "Sharp Position",
    "dynamic_position": "Dynamic Play",
    "dry_position": "Quiet Position",
    "drawish_position": "Drawish",

    // Advanced concepts
    "conversion_difficulty": "Hard to Convert",
    "long_term_compensation": "Long-term Edge",
    "positional_sacrifice": "Positional Sacrifice",
    "engine_only_move": "Computer Move",
    "comfortable_position": "Comfortable",
    "unpleasant_position": "Unpleasant",

    // File/rank features
    "open_h_file": "Open h-File",
    "open_g_file": "Open g-File",
    "rook_on_seventh": "Rook on 7th",

    // Phase transitions
    "endgame_transition": "Entering Endgame",
    "shift_tactical_to_positional": "Simplifying",
    "opening_theory_branch": "Theory Branch",
    "plan_change": "Plan Shift",

    // Other
    "opposite_color_bishops": "Opposite Bishops"
};

function getTagLabel(tag: string): string {
    return tagLabelMap[tag] || tag.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
}

const phaseConfig: Record<string, { icon: string; color: string; label: string }> = {
    "opening": { icon: "📖", color: "bg-blue-500/20 text-blue-300 border-blue-500/30", label: "Opening" },
    "middlegame": { icon: "⚔️", color: "bg-orange-500/20 text-orange-300 border-orange-500/30", label: "Middlegame" },
    "endgame": { icon: "👑", color: "bg-purple-500/20 text-purple-300 border-purple-500/30", label: "Endgame" }
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

    // Group chapters by phase and sort by ply within each group
    const grouped: Record<string, StudyChapter[]> = {};
    chapters.forEach(ch => {
        const phase = ch.tags.find(t => ["opening", "middlegame", "endgame"].includes(t)) || "middlegame";
        if (!grouped[phase]) grouped[phase] = [];
        grouped[phase].push(ch);
    });

    // Sort each group by ply
    Object.keys(grouped).forEach(phase => {
        grouped[phase].sort((a, b) => a.anchorPly - b.anchorPly);
    });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white/80">Study Chapters</h3>
                <span className="rounded-full bg-accent-teal/10 px-2 py-0.5 text-xs text-accent-teal">
                    {chapters.length} chapters
                </span>
            </div>

            {/* Render by phase */}
            {(["opening", "middlegame", "endgame"] as const).map(phase => {
                const items = grouped[phase];
                if (!items || items.length === 0) return null;

                const config = phaseConfig[phase];
                return (
                    <div key={phase} className="space-y-3">
                        <div className="flex items-center gap-2">
                            <span className={`px-2 py-0.5 rounded text-xs font-semibold border ${config.color}`}>
                                {config.icon} {config.label}
                            </span>
                            <span className="text-xs text-white/40">{items.length} moments</span>
                        </div>

                        {items.map((chapter, idx) => (
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

                                <p className="text-xs text-white/70 line-clamp-3 mb-3">
                                    {chapter.summary || "No summary available."}
                                </p>

                                <div className="flex items-center justify-between mt-3">
                                    <div className="flex flex-wrap gap-1.5">
                                        {chapter.tags.filter(t => !["opening", "middlegame", "endgame"].includes(t)).slice(0, 3).map(tag => (
                                            <span key={tag} className="rounded bg-black/20 px-1.5 py-0.5 text-[10px] text-white/50">
                                                {getTagLabel(tag)}
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
                );
            })}
        </div>
    );
}
