import React, { useEffect, useRef } from "react";
import { TimelineNode } from "../types/review";

type CompressedMoveListProps = {
    timeline: TimelineNode[];
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
};

const judgementColors: Record<string, string> = {
    brilliant: "text-purple-400",
    great: "text-cyan-400",
    best: "text-emerald-400",
    good: "text-green-400",
    book: "text-blue-300",
    inaccuracy: "text-amber-400",
    mistake: "text-orange-400",
    blunder: "text-rose-400"
};

const judgementMarks: Record<string, string> = {
    brilliant: "!!",
    great: "!",
    best: "★",
    good: "",
    book: "",
    inaccuracy: "?!",
    mistake: "?",
    blunder: "??"
};

const practicalityBg: Record<string, string> = {
    "Human-Friendly": "bg-emerald-500/10 hover:bg-emerald-500/15",
    "Challenging": "bg-amber-500/10 hover:bg-amber-500/15",
    "Engine-Like": "bg-orange-500/10 hover:bg-orange-500/15",
    "Computer-Only": "bg-rose-500/10 hover:bg-rose-500/15"
};

export function CompressedMoveList({ timeline, currentPly, onSelectPly }: CompressedMoveListProps) {
    const scrollRef = useRef<HTMLDivElement>(null);
    const activeRef = useRef<HTMLButtonElement>(null);

    // Auto-scroll to active move
    useEffect(() => {
        if (activeRef.current && scrollRef.current) {
            const container = scrollRef.current;
            const element = activeRef.current;

            const containerRect = container.getBoundingClientRect();
            const elementRect = element.getBoundingClientRect();

            if (elementRect.bottom > containerRect.bottom || elementRect.top < containerRect.top) {
                element.scrollIntoView({ behavior: "smooth", block: "nearest" });
            }
        }
    }, [currentPly]);

    // Group moves into pairs (White, Black)
    const movePairs: Array<{ number: number; white?: TimelineNode; black?: TimelineNode }> = [];

    timeline.forEach((node) => {
        const moveNumber = Math.ceil(node.ply / 2);
        if (!movePairs[moveNumber]) {
            movePairs[moveNumber] = { number: moveNumber };
        }
        if (node.ply % 2 !== 0) {
            movePairs[moveNumber].white = node;
        } else {
            movePairs[moveNumber].black = node;
        }
    });

    return (
        <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-white/10 bg-white/5">
            <div className="flex items-center justify-between border-b border-white/10 bg-white/5 px-4 py-2">
                <h3 className="text-sm font-semibold text-white/80">Moves</h3>
            </div>

            <div ref={scrollRef} className="flex-1 overflow-y-auto p-2 text-sm font-medium">
                <div className="flex flex-wrap content-start gap-x-1 gap-y-0.5">
                    {movePairs.map((pair, idx) => {
                        if (!pair) return null; // Skip empty slots (0 index)
                        return (
                            <div key={pair.number} className="flex items-baseline gap-1 px-1 py-0.5 hover:bg-white/5 rounded">
                                <span className="mr-1 text-white/40 font-mono text-xs">{pair.number}.</span>

                                {pair.white && (
                                    <MoveButton
                                        node={pair.white}
                                        isActive={currentPly === pair.white.ply}
                                        onClick={() => onSelectPly(pair.white!.ply)}
                                        ref={currentPly === pair.white.ply ? activeRef : undefined}
                                    />
                                )}

                                {pair.black && (
                                    <MoveButton
                                        node={pair.black}
                                        isActive={currentPly === pair.black.ply}
                                        onClick={() => onSelectPly(pair.black!.ply)}
                                        ref={currentPly === pair.black.ply ? activeRef : undefined}
                                    />
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}

const MoveButton = React.forwardRef<HTMLButtonElement, {
    node: TimelineNode;
    isActive: boolean;
    onClick: () => void;
}>(({ node, isActive, onClick }, ref) => {
    const judgement = (node.special as keyof typeof judgementMarks) ?? node.judgement ?? "good";
    const mark = judgementMarks[judgement] ?? "";
    const colorClass = judgementColors[judgement] ?? "text-white";

    // Get practicality background color
    const practicalityCategory = node.practicality?.categoryPersonal || node.practicality?.categoryGlobal || node.practicality?.category;
    const practicalityBgClass = practicalityCategory && !isActive ? practicalityBg[practicalityCategory] : "";

    return (
        <button
            ref={ref}
            onClick={onClick}
            className={`
        group relative rounded px-1.5 py-0.5 transition-colors
        ${isActive ? "bg-accent-teal/20 text-white ring-1 ring-accent-teal/50" : `text-white/80 ${practicalityBgClass || "hover:bg-white/10"} hover:text-white`}
      `}
        >
            <span>{node.san}</span>
            {mark && (
                <span className={`ml-0.5 text-xs font-bold ${colorClass}`}>
                    {mark}
                </span>
            )}
        </button>
    );
});

MoveButton.displayName = "MoveButton";
