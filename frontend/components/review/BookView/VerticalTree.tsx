import React from "react";
import { ReviewTreeNode } from "../../../types/review";
import { TreeNodeCard } from "./TreeNodeCard";

interface VerticalTreeProps {
    rootNode: ReviewTreeNode;
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
    isRoot?: boolean;
}

export const VerticalTree = ({ rootNode, currentPly, onSelectPly, isRoot = false }: VerticalTreeProps) => {
    // Identify Mainline child vs Variations
    const mainlineChild = rootNode.children?.find(c => c.nodeType === "mainline");
    const variations = rootNode.children?.filter(c => c.nodeType !== "mainline") || [];

    // Sort variations: Critical > Best > Practical > Sideline
    const sortedVariations = React.useMemo(() => {
        return [...variations].sort((a, b) => {
            const score = (type?: string) => {
                if (type === "critical") return 4;
                if (type === "best") return 3;
                if (type === "practical") return 2;
                return 1;
            };
            return score(b.nodeType) - score(a.nodeType);
        });
    }, [variations]);

    return (
        <div className="flex flex-col">
            {/* Current Node Row */}
            <div className="flex items-start group relative">

                {/* 1. Spine & Node Container */}
                <div className="flex flex-col items-center relative z-10">
                    {/* Node Card */}
                    <TreeNodeCard
                        node={rootNode}
                        isSelected={currentPly === rootNode.ply}
                        onSelect={() => onSelectPly(rootNode.ply)}
                    />

                    {/* Vertical Spine (If there is a next mainline move) */}
                    {mainlineChild && (
                        <div className="w-1 bg-accent-teal/30 h-8 grow min-h-[2rem]" />
                    )}
                </div>

                {/* 2. Variations (Branching off to the right) */}
                {sortedVariations.length > 0 && (
                    <div className="flex flex-col gap-3 ml-8 mt-2 relative">
                        {/* Connector to Variations */}
                        <div className="absolute -left-8 top-4 w-6 h-px bg-white/20" />

                        {/* Vertical bar connecting multiple variations */}
                        {sortedVariations.length > 1 && (
                            <div className="absolute -left-2 top-4 bottom-4 w-px bg-white/20" />
                        )}

                        {sortedVariations.map((v, i) => (
                            <div key={i} className="flex flex-col">
                                <div className="flex items-center relative">
                                    {/* Horizontal connector per variation */}
                                    {sortedVariations.length > 1 && (
                                        <div className="absolute -left-2 top-1/2 w-2 h-px bg-white/20" />
                                    )}
                                    <TreeNodeCard
                                        node={v}
                                        isSelected={currentPly === v.ply}
                                        onSelect={() => onSelectPly(v.ply)}
                                    />
                                </div>

                                {/* Render variation's mainline continuation */}
                                {v.children && v.children.length > 0 && (
                                    <div className="ml-4 mt-2">
                                        {(() => {
                                            const varMainline = v.children.find(c => c.nodeType === "mainline");
                                            if (varMainline) {
                                                return (
                                                    <VerticalTree
                                                        rootNode={varMainline}
                                                        currentPly={currentPly}
                                                        onSelectPly={onSelectPly}
                                                    />
                                                );
                                            }
                                            return null;
                                        })()}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Recursive Step: Mainline Continuation */}
            {mainlineChild && (
                <VerticalTree
                    rootNode={mainlineChild}
                    currentPly={currentPly}
                    onSelectPly={onSelectPly}
                />
            )}
        </div>
    );
};
