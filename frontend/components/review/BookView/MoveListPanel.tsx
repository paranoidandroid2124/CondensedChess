import React from "react";
import { EnhancedTimelineNode } from "../../../lib/review-derived";

interface MoveListPanelProps {
    timeline: EnhancedTimelineNode[];
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
}

export const MoveListPanel = ({ timeline, currentPly, onSelectPly }: MoveListPanelProps) => {
    // Group timeline into pairs (White, Black)
    const pairs: { moveNum: number; white?: EnhancedTimelineNode; black?: EnhancedTimelineNode }[] = [];

    // Skip ply 0 (start position)
    for (let i = 1; i < timeline.length; i += 2) {
        const white = timeline[i];
        const black = timeline[i + 1];
        pairs.push({
            moveNum: Math.ceil(white.ply / 2),
            white,
            black
        });
    }

    return (
        <div className="flex flex-col h-full bg-black/20 rounded-xl overflow-hidden border border-white/5">
            <div className="px-4 py-3 bg-white/5 border-b border-white/5 font-medium text-white/80 flex justify-between items-center">
                <span>Game Moves</span>
            </div>

            <div className="flex-1 overflow-y-auto custom-scrollbar p-2">
                <table className="w-full text-sm border-collapse">
                    <tbody>
                        {pairs.map((pair) => (
                            <tr key={pair.moveNum} className="group hover:bg-white/5 transition-colors">
                                {/* Move Number */}
                                <td className="py-1 px-2 text-white/30 font-mono w-12 text-right select-none">
                                    {pair.moveNum}.
                                </td>

                                {/* White Move */}
                                <td className="py-1 px-1 w-1/2">
                                    {pair.white && (
                                        <MoveCell
                                            node={pair.white}
                                            timeline={timeline}
                                            isSelected={currentPly === pair.white.ply}
                                            onSelect={onSelectPly}
                                        />
                                    )}
                                </td>

                                {/* Black Move */}
                                <td className="py-1 px-1 w-1/2">
                                    {pair.black && (
                                        <MoveCell
                                            node={pair.black}
                                            timeline={timeline}
                                            isSelected={currentPly === pair.black.ply}
                                            onSelect={onSelectPly}
                                        />
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

// Sub-component for individual move cell with hover preview
const MoveCell = ({
    node,
    timeline,
    isSelected,
    onSelect
}: {
    node: EnhancedTimelineNode;
    timeline: EnhancedTimelineNode[];
    isSelected: boolean;
    onSelect: (ply: number) => void;
}) => {

    // Determine color based on judgement
    const getMoveColor = (judgement?: string) => {
        switch (judgement) {
            case "brilliant": return "text-teal-400";
            case "best": return "text-emerald-400";
            case "good": return "text-emerald-200/80";
            case "inaccuracy": return "text-yellow-200/80";
            case "mistake": return "text-orange-400";
            case "blunder": return "text-red-500";
            default: return "text-white/90";
        }
    };

    // Evaluation diff
    const getEvalColor = (delta?: number) => {
        if (!delta) return "bg-white/10";
        if (node.turn === "white") {
            return delta > 0 ? "bg-emerald-500" : "bg-red-500";
        } else {
            return delta < 0 ? "bg-emerald-500" : "bg-red-500";
        }
    };

    return (
        <div
            className={`relative group/cell flex items-center justify-between px-2 py-1 rounded cursor-pointer transition-all
                ${isSelected ? "bg-accent-teal/20 ring-1 ring-accent-teal/50" : "hover:bg-white/10"}
            `}
            onClick={() => onSelect(node.ply)}
        >
            {/* Move Text */}
            <span className={`font-medium ${getMoveColor(node.judgement)}`}>
                {node.san}
            </span>

            {/* Tiny Eval Bar */}
            {node.winPctAfterForPlayer !== undefined && (
                <div className="h-1 w-6 bg-white/10 rounded-full overflow-hidden ml-2 opacity-50">
                    <div
                        className="h-full bg-white/60"
                        style={{ width: `${Math.min(100, Math.max(0, node.winPctAfterForPlayer))}%` }}
                    />
                </div>
            )}
        </div>
    );
};
