import React from "react";
import { ReviewTreeNode } from "../../../types/review";
import { TreeNodeCard } from "./TreeNodeCard";

interface HorizontalTreeViewProps {
    rootNode: ReviewTreeNode;
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
    isRoot?: boolean;
}

export const HorizontalTreeView = ({ rootNode, currentPly, onSelectPly, isRoot = false }: HorizontalTreeViewProps) => {
    const hasChildren = rootNode.children && rootNode.children.length > 0;

    // Sort children: Mainline first, then Critical, then Sideline
    const sortedChildren = React.useMemo(() => {
        if (!hasChildren) return [];
        return [...rootNode.children].sort((a, b) => {
            const typeScore = (type?: string) => {
                if (type === "mainline") return 3;
                if (type === "critical") return 2;
                return 1;
            };
            return typeScore(b.nodeType) - typeScore(a.nodeType);
        });
    }, [rootNode.children, hasChildren]);

    return (
        <div className="flex items-start">
            {/* Node Card */}
            <div className="flex items-center">
                <TreeNodeCard
                    node={rootNode}
                    isSelected={currentPly === rootNode.ply}
                    onSelect={() => onSelectPly(rootNode.ply)}
                />

                {/* Connector to children container */}
                {hasChildren && (
                    <div className="w-8 h-px bg-white/20" />
                )}
            </div>

            {/* Children Container */}
            {hasChildren && (
                <div className="flex flex-col gap-4 relative">
                    {/* Vertical Line connecting branches */}
                    {sortedChildren.length > 1 && (
                        <div
                            className="absolute left-0 w-px bg-white/20"
                            style={{
                                top: "2rem", // Approximate center of first child
                                bottom: "2rem" // Approximate center of last child
                            }}
                        />
                    )}

                    {sortedChildren.map((child, idx) => (
                        <div key={child.ply || idx} className="flex items-center relative">
                            {/* Horizontal connector from vertical line to child */}
                            {/* Only needed if there are multiple children, to connect to the vertical bar */}
                            {sortedChildren.length > 1 && (
                                <div className="w-4 h-px bg-white/20" />
                            )}

                            <HorizontalTreeView
                                rootNode={child}
                                currentPly={currentPly}
                                onSelectPly={onSelectPly}
                            />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};
