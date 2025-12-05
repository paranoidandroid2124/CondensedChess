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
    const nodeType = (node.nodeType as "mainline" | "critical" | "sideline") || "sideline";

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
        sideline: {
            border: "border-white/20",
            bg: "bg-white/5",
            text: "text-white/70",
            badge: "bg-white/20 text-white"
        }
    };

    const style = colors[nodeType];
    const evalText = node.eval ? (node.eval > 0 ? `+${node.eval.toFixed(2)}` : node.eval.toFixed(2)) : "";

    return (
        <div
            className={`
                group relative flex flex-col gap-1 p-2 rounded-lg border transition-all cursor-pointer min-w-[100px]
                ${style.border} ${style.bg}
                ${isSelected ? "ring-2 ring-white shadow-lg shadow-white/10" : "hover:bg-white/10"}
            `}
            onClick={(e) => {
                e.stopPropagation();
                onSelect();
            }}
        >
            {/* Header: Move & Eval */}
            <div className="flex justify-between items-baseline gap-2">
                <span className={`font-bold text-sm ${style.text}`}>
                    {formatSanHuman(node.san)}
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

            {/* Hover Tooltip with MiniBoard */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50">
                <div className="bg-gray-900 rounded-lg p-2 shadow-xl border border-white/20 w-32">
                    <MiniBoard fen={node.fen} orientation="white" lastMove={node.uci} />
                    {node.comment && (
                        <p className="mt-1 text-[10px] text-white/80 italic leading-tight">
                            {node.comment}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};
