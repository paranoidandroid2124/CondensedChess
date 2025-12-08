import React from "react";
import { ReviewTreeNode } from "../../../types/review";
import { MiniBoard } from "../../common/MiniBoard";
import { formatSanHuman } from "../../../lib/review-format";

interface TreeNodeCardProps {
    node: ReviewTreeNode;
    isSelected: boolean;
    onSelect: () => void;
}

export const TreeNodeCard = ({ node, isSelected, onSelect }: TreeNodeCardProps) => {
    const nodeType = (node.nodeType as "mainline" | "critical" | "sideline" | "practical") || "sideline";

    // Color schemes based on node type
    const colors = {
        mainline: {
            border: "border-accent-teal/50",
            bg: "bg-accent-teal/10",
            text: "text-white",
            badge: "bg-accent-teal text-black"
        },
        critical: {
            border: "border-amber-500/50",
            bg: "bg-amber-500/10",
            text: "text-amber-100",
            badge: "bg-amber-500 text-black"
        },
        practical: {
            border: "border-purple-400/50",
            bg: "bg-purple-400/10",
            text: "text-purple-200",
            badge: "bg-purple-400 text-black"
        },
        sideline: {
            border: "border-white/20",
            bg: "bg-white/5",
            text: "text-white/70",
            badge: "bg-white/20 text-white"
        },
        hypothesis: {
            border: "border-red-500/50 border-dashed",
            bg: "bg-red-500/10",
            text: "text-red-200",
            badge: "bg-red-500/20 text-red-300 border border-red-500/30"
        },
        root: {
            border: "border-accent-teal/50",
            bg: "bg-accent-teal/20",
            text: "text-accent-teal",
            badge: "bg-accent-teal text-black"
        }
    };

    const style = colors[nodeType] || colors.sideline;
    const evalText = node.eval ? (node.eval > 0 ? `+${node.eval.toFixed(2)}` : node.eval.toFixed(2)) : "";

    // State for expanded line view
    const [isExpanded, setIsExpanded] = React.useState(false);

    // Flatten children chain for "Line Preview"
    // We only do this if it's a variation node (critical/practical) to show what happens next
    const { previewLine, hasMore } = React.useMemo(() => {
        if (nodeType === "mainline") return { previewLine: [], hasMore: false };

        const line: { san: string; fen: string }[] = [];
        let current = node.children?.[0];
        // Collect all following moves
        while (current) {
            line.push({ san: current.san, fen: current.fen });
            current = current.children?.[0];
        }
        return {
            previewLine: isExpanded ? line : line.slice(0, 5),
            hasMore: line.length > 5
        };
    }, [node.children, nodeType, isExpanded]);

    const [hoverFen, setHoverFen] = React.useState<string | null>(null);

    return (
        <div
            className={`
                group relative flex flex-col gap-1 p-2 rounded-lg border transition-all cursor-pointer min-w-[120px]
                ${style.border} ${style.bg}
                ${isSelected ? "ring-2 ring-white shadow-lg shadow-white/10" : "hover:bg-white/10"}
                hover:z-50
            `}
            onClick={(e) => {
                e.stopPropagation();
                onSelect();
            }}
            onMouseLeave={() => setHoverFen(null)}
        >
            {/* Header: Move & Eval */}
            <div className="flex justify-between items-baseline gap-2">
                <span className={`font-bold text-sm ${style.text}`}>
                    {node.ply === 0 && !node.san ? "Start" : formatSanHuman(node.san)}
                </span>
                {evalText && (
                    <span className="text-[10px] font-mono opacity-70">
                        {evalText}
                    </span>
                )}
            </div>

            {/* Badge / Judgement */}
            <div className="flex items-center gap-1">
                {node.judgement && node.judgement !== "book" && node.judgement !== "normal" && (
                    <span className={`text-[9px] px-1 rounded uppercase font-bold tracking-wider ${style.badge}`}>
                        {node.judgement}
                    </span>
                )}
                {nodeType === "critical" && (
                    <span className="text-[9px] px-1 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                        Critical
                    </span>
                )}
            </div>

            {/* Interactive Line Preview */}
            {/* Disable for Start node (ply 0, no san) and Mainline */}
            {previewLine.length > 0 && (node.ply !== 0 || !!node.san) && (
                <div className="mt-1 flex flex-wrap gap-1 text-[10px] text-white/50">
                    {previewLine.map((move, i) => (
                        <span
                            key={i}
                            className="px-0.5"
                        >
                            {formatSanHuman(move.san)}
                        </span>
                    ))}
                    {hasMore && (
                        <button
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsExpanded(!isExpanded);
                            }}
                            className="text-accent-teal hover:text-white hover:bg-accent-teal/30 px-1 rounded transition-colors"
                        >
                            {isExpanded ? "▲ less" : `▼ +${previewLine.length > 0 ? "more" : ""}`}
                        </button>
                    )}
                    {!hasMore && previewLine.length > 0 && <span className="opacity-30">•</span>}
                </div>
            )}

            {/* Hover Tooltip with MiniBoard */}
            {/* Show tooltip if hovering card (default node.fen) OR hovering a preview move (hoverFen) */}
            <div className={`absolute bottom-full left-1/2 -translate-x-1/2 mb-2 transition-opacity pointer-events-none z-50 ${hoverFen ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
                <div className="bg-gray-900 rounded-lg p-2 shadow-xl border border-white/20 w-32">
                    <MiniBoard fen={hoverFen || node.fen} orientation="white" lastMove={node.uci} />
                    {node.comment && !hoverFen && (
                        <p className="mt-1 text-[10px] text-white/80 italic leading-tight">
                            {node.comment}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};
