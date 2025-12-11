import React, { useMemo } from "react";
import { Review, Concepts, TimelineNode } from "../types/review";
import { ConceptTrendChart } from "./review/ConceptTrendChart";

type ConceptsTabProps = {
    review: Review | null;
    currentConcepts?: Concepts;
    currentSemanticTags?: string[];
    conceptDelta?: Concepts;
    timeline?: TimelineNode[];
    currentPly?: number;
};

const tagLabelMap: Record<string, string> = {
    // Piece quality
    "restricted_bishop": "Restricted Bishop",
    "strong_knight": "Strong Knight",
    "active_rooks": "Active Rooks",

    // King safety
    "king_exposed": "King in Danger",
    "king_attack_ready": "King Attack",
    "king_stuck_center": "Uncastled King",
    "weak_back_rank": "Weak Back Rank",
    "king_safety_crisis": "King Safety Crisis",

    // Positional themes
    "locked_position": "Locked Position",
    "fortress_defense": "Fortress Defense",
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
    "opposite_color_bishops": "Opposite Bishops",

    // Rich Concepts (New)
    "conversion_difficulty_endgame": "Tricky Endgame",
    "conversion_difficulty_opposite_bishops": "Opposite Bishop Draw Risk",
    "bishop_pair_advantage": "Bishop Pair Advantage",
    "knight_outpost_central": "Central Outpost"
};

const tagColorMap: Record<string, string> = {
    "king": "bg-rose-500/20 text-rose-300 border-rose-500/30",
    "tactical": "bg-amber-500/20 text-amber-300 border-amber-500/30",
    "fortress": "bg-blue-500/20 text-blue-300 border-blue-500/30",
    "bishop": "bg-purple-500/20 text-purple-300 border-purple-500/30",
    "knight": "bg-green-500/20 text-green-300 border-green-500/30",
    "rook": "bg-cyan-500/20 text-cyan-300 border-cyan-500/30",
    "pawn": "bg-lime-500/20 text-lime-300 border-lime-500/30",
    "dynamic": "bg-orange-500/20 text-orange-300 border-orange-500/30",
    "drawish": "bg-gray-500/20 text-gray-300 border-gray-500/30",
    "space": "bg-indigo-500/20 text-indigo-300 border-indigo-500/30",
    "default": "bg-white/10 text-white/70 border-white/20"
};

export function parseTag(tag: string): { label: string; color: "White" | "Black" | null; colorClass: string } {
    const parts = tag.split('_');
    let color: "White" | "Black" | null = null;
    let baseTag = tag;

    if (parts[0] === "white") {
        color = "White";
        baseTag = parts.slice(1).join('_');
    } else if (parts[0] === "black") {
        color = "Black";
        baseTag = parts.slice(1).join('_');
    }

    const label = tagLabelMap[baseTag] || baseTag.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');

    // Find color class based on baseTag (e.g. "strong_knight" -> "knight")
    const colorKey = Object.keys(tagColorMap).find(k => baseTag.toLowerCase().includes(k));
    const colorClass = colorKey ? tagColorMap[colorKey] : tagColorMap.default;

    return { label, color, colorClass };
}

// Keep generic helpers for backward compatibility if needed, but they forward to parseTag
export function getTagLabel(tag: string): string {
    const { label, color } = parseTag(tag);
    return color ? `${label} (${color})` : label;
}

export function getTagColor(tag: string): string {
    return parseTag(tag).colorClass;
}

export function ConceptsTab({ review, currentConcepts, currentSemanticTags, conceptDelta, timeline, currentPly }: ConceptsTabProps) {
    if (!review) return null;

    return (
        <div className="space-y-6">
            {/* Game Accuracy */}
            <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-surface-highlight bg-surface-elevated p-3 text-center">
                    <div className="text-xs uppercase tracking-wider text-content-tertiary mb-1">White</div>
                    <div className="text-2xl font-bold text-content-primary">
                        {review.accuracyWhite?.toFixed(1) ?? "—"}
                        <span className="text-sm text-content-tertiary">%</span>
                    </div>
                </div>
                <div className="rounded-xl border border-surface-highlight bg-surface-elevated p-3 text-center">
                    <div className="text-xs uppercase tracking-wider text-content-tertiary mb-1">Black</div>
                    <div className="text-2xl font-bold text-content-primary">
                        {review.accuracyBlack?.toFixed(1) ?? "—"}
                        <span className="text-sm text-content-tertiary">%</span>
                    </div>
                </div>
            </div>

            {/* Concept Trend Chart */}
            {timeline && timeline.length > 0 && (
                <div className="rounded-xl border border-surface-highlight bg-surface-elevated p-4 animate-scale-in">
                    <p className="mb-2 text-xs font-semibold text-content-secondary">Concept Trends</p>
                    <ConceptTrendChart
                        data={timeline.map(node => ({
                            ply: node.ply,
                            dynamic: node.concepts?.dynamic,
                            kingSafety: node.concepts?.kingSafety,
                            tacticalDepth: node.concepts?.tacticalDepth,
                            comfortable: node.concepts?.comfortable
                        }))}
                        currentPly={currentPly}
                        height={160}
                    />
                </div>
            )}

            {/* Current Position Concepts */}
            {currentConcepts && (
                <div className="space-y-3 animate-slide-up">
                    <h3 className="text-sm font-semibold text-content-secondary">Current Position Concepts</h3>

                    {/* Semantic Tags */}
                    {currentSemanticTags && currentSemanticTags.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-4">
                            {currentSemanticTags.map(tag => (
                                <span key={tag} className={`rounded px-2 py-1 text-xs font-medium border ${getTagColor(tag)}`}>
                                    {getTagLabel(tag)}
                                </span>
                            ))}
                        </div>
                    )}

                    {/* Concept Deltas */}
                    {conceptDelta && (
                        <div className="mb-4 space-y-2">
                            <h4 className="text-xs font-medium text-content-tertiary uppercase tracking-wider">Significant Changes</h4>
                            {Object.entries(conceptDelta)
                                .filter(([, val]) => typeof val === 'number' && Math.abs(val) >= 0.1)
                                .sort(([, a], [, b]) => Math.abs(b as number) - Math.abs(a as number))
                                .map(([key, val]) => (
                                    <div key={key} className="flex items-center justify-between text-xs">
                                        <span className="text-content-secondary capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                                        <span className={`font-mono ${(val as number) > 0 ? 'text-green-400' : 'text-rose-400'}`}>
                                            {(val as number) > 0 ? '+' : ''}{(val as number).toFixed(2)}
                                        </span>
                                    </div>
                                ))}
                        </div>
                    )}

                    <div className="grid gap-2">
                        {Object.entries(currentConcepts)
                            .filter(([, val]) => typeof val === 'number')
                            .sort(([, a], [, b]) => (b as number) - (a as number))
                            .slice(0, 6)
                            .map(([key, val]) => (
                                <div key={key} className="flex items-center justify-between rounded-lg bg-surface-base px-3 py-2 border border-surface-highlight">
                                    <span className="text-xs text-content-secondary capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                                    <div className="flex items-center gap-2">
                                        <div className="h-1.5 w-16 rounded-full bg-black/20 overflow-hidden">
                                            <div
                                                className="h-full bg-accent-teal"
                                                style={{ width: `${Math.min(100, Math.max(0, (val as number) * 100))}%` }}
                                            />
                                        </div>
                                        <span className="text-xs font-mono text-accent-teal">{(val as number).toFixed(2)}</span>
                                    </div>
                                </div>
                            ))}
                    </div>
                </div>
            )}
        </div>
    );
}
