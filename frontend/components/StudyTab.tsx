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

import { ScrollytellingLayout } from "./ScrollytellingLayout";

export function StudyTab({ chapters, onSelectChapter }: StudyTabProps) {
    if (!chapters || chapters.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-12 text-center">
                <div className="text-4xl mb-2">🎓</div>
                <p className="text-white/60">No study chapters generated for this game.</p>
                <p className="text-xs text-white/40 mt-2">Try analyzing a game with more mistakes!</p>
            </div>
        );
    }

    // Sort chapters by studyScore (highest value first)
    const sortedChapters = [...chapters].sort((a, b) => {
        const scoreA = a.studyScore ?? 0;
        const scoreB = b.studyScore ?? 0;
        return scoreB - scoreA; // Descending order
    });

    // We pass null for currentPly for now, as we need to wire it up from parent if we want 
    // bi-directional sync (board -> scroll). For now, scroll -> board is the priority.
    return (
        <div className="h-[calc(100vh-200px)]">
            <ScrollytellingLayout
                chapters={sortedChapters}
                currentPly={null}
                onPlySelect={onSelectPly => onSelectChapter(onSelectPly)}
            />
        </div>
    );
}
