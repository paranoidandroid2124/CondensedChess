import React, { useEffect, useRef } from "react";
import { TimelineNode } from "../../../types/review";

type Props = {
    timeline: TimelineNode[];
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
};

// Simple classification for demo - typically shared with backend definitions or fetched
const TAG_CATEGORIES: Record<string, string> = {
    "SpaceAdvantage": "bg-blue-500/20 text-blue-300 border-blue-500/30",
    "KingSafety": "bg-red-500/20 text-red-300 border-red-500/30",
    "BadBishop": "bg-purple-500/20 text-purple-300 border-purple-500/30",
    "PawnStorm": "bg-orange-500/20 text-orange-300 border-orange-500/30",
    "TacticalMiss": "bg-red-600/20 text-red-200 border-red-600/40",
    "Blunder": "bg-red-600/20 text-red-200 border-red-600/40",
    "GoodPlan": "bg-green-500/20 text-green-300 border-green-500/30",
};

const getTagStyle = (tag: string) => {
    // Prefix match or exact match
    const found = Object.keys(TAG_CATEGORIES).find(k => tag.includes(k));
    return found ? TAG_CATEGORIES[found] : "bg-slate-700 text-slate-400 border-slate-600";
};

export const TaggedMoveList: React.FC<Props> = ({ timeline, currentPly, onSelectPly }) => {
    const scrollRef = useRef<HTMLDivElement>(null);
    const activeRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (activeRef.current && scrollRef.current) {
            activeRef.current.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }, [currentPly]);

    // Pairs
    const movePairs: Array<{ number: number; white?: TimelineNode; black?: TimelineNode }> = [];
    timeline.forEach((node) => {
        const moveNumber = Math.ceil(node.ply / 2);
        if (!movePairs[moveNumber]) movePairs[moveNumber] = { number: moveNumber };
        if (node.ply % 2 !== 0) movePairs[moveNumber].white = node;
        else movePairs[moveNumber].black = node;
    });

    return (
        <div className="flex flex-col h-full bg-slate-900 border-l border-slate-700">
            <div className="p-3 border-b border-slate-700 text-sm font-semibold text-slate-400 uppercase tracking-wider">
                Annotated Moves
            </div>
            <div ref={scrollRef} className="flex-1 overflow-y-auto p-2 space-y-1">
                {movePairs.map((pair) => pair && (
                    <div key={pair.number} className="flex gap-2 p-1 text-sm bg-slate-800/40 rounded">
                        <div className="w-8 text-slate-500 font-mono text-right py-1">{pair.number}.</div>
                        <div className="flex-1 flex gap-2">
                            <MoveCell
                                node={pair.white}
                                isActive={currentPly === pair.white?.ply}
                                onClick={() => pair.white && onSelectPly(pair.white.ply)}
                                ref={currentPly === pair.white?.ply ? activeRef : undefined}
                            />
                            <MoveCell
                                node={pair.black}
                                isActive={currentPly === pair.black?.ply}
                                onClick={() => pair.black && onSelectPly(pair.black.ply)}
                                ref={currentPly === pair.black?.ply ? activeRef : undefined}
                            />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

interface MoveCellProps {
    node?: TimelineNode;
    isActive: boolean;
    onClick: () => void;
}

// Wrapping ForwardRef
const MoveCell = React.forwardRef<HTMLDivElement, MoveCellProps>(({ node, isActive, onClick }, ref) => {
    if (!node) return <div className="flex-1" />;

    const tags = node.semanticTags || [];
    const importantTags = tags.filter(t => !["User", "White", "Black"].includes(t)).slice(0, 2); // Show max 2 badges

    return (
        <div
            ref={ref}
            onClick={onClick}
            className={`flex-1 flex flex-col p-2 rounded cursor-pointer transition-colors border
             ${isActive
                    ? 'bg-blue-600/20 border-blue-500 text-white shadow-[0_0_10px_rgba(59,130,246,0.2)]'
                    : 'bg-slate-700/30 border-transparent hover:bg-slate-700 hover:border-slate-600 text-slate-300'}
           `}
        >
            <div className={`font-bold font-mono ${isActive ? 'text-blue-200' : 'text-slate-200'}`}>
                {node.san} <span className="opacity-50 text-xs font-normal">({node.evalBeforeDeep?.lines[0]?.winPct?.toFixed(0)}%)</span>
            </div>
            {importantTags.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-1">
                    {importantTags.map(tag => (
                        <span key={tag} className={`text-[10px] px-1.5 py-0.5 rounded border ${getTagStyle(tag)}`}>
                            {tag}
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
});
MoveCell.displayName = "MoveCell";
