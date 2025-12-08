import React from "react";
import { ReviewTreeNode } from "../../../types/review";

interface NarrativeRendererProps {
    node: ReviewTreeNode;
    depth?: number;
    variationLabel?: string;
    onInteract?: (fen: string, uci: string) => void;
}

// Helper to convert bold markdown to JSX
function renderMarkdown(text: string) {
    if (!text) return null;
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, idx) => {
        if (part.startsWith("**") && part.endsWith("**")) {
            return <strong key={idx} className="font-bold text-white">{part.slice(2, -2)}</strong>;
        }
        return part;
    });
}

// Move Span Component
const MoveSpan = ({ node, isFirst, variationLabel, onInteract }: {
    node: ReviewTreeNode,
    isFirst?: boolean,
    variationLabel?: string,
    onInteract?: (fen: string, uci: string) => void
}) => {
    const isHypothesis = node.nodeType === "hypothesis";
    // Check if this move starts a sentence or is Black's move to determine numbering
    const moveNumber = Math.ceil((node.ply + 1) / 2);
    const isWhite = node.ply % 2 === 0;
    const showNumber = isWhite || isFirst;
    const numberText = isWhite ? `${moveNumber}.` : `${moveNumber}...`;

    return (
        <span className="inline mr-1 leading-relaxed">
            {variationLabel && (
                <span className="inline-block mr-1 font-bold text-neutral-400 text-sm">
                    {variationLabel}
                </span>
            )}

            {showNumber && (
                <span className="text-neutral-500 mr-1 select-none font-mono text-xs">{numberText}</span>
            )}

            <span
                className={`cursor-pointer hover:bg-yellow-500/20 hover:text-yellow-200 rounded px-1 transition-colors mx-0.5
                    ${isHypothesis ? "text-red-300 decoration-red-900 underline decoration-dashed" : "font-bold text-white"}
                `}
                onClick={() => onInteract?.(node.fen, node.uci)}
            >
                {node.san}
            </span>

            {isHypothesis && (
                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-red-900/30 text-red-200 border border-red-800/50 mx-1 align-middle">
                    Why Not?
                </span>
            )}

            {node.comment && (
                <span className="text-neutral-300 ml-1">
                    {renderMarkdown(node.comment)}
                </span>
            )}
        </span>
    );
};

export function NarrativeRenderer({ node, depth = 0, variationLabel, onInteract }: NarrativeRendererProps) {
    // Flatten logic: 
    // We want to transform the tree into a list of "Paragraphs" and "Variation Blocks".
    // But since this component is called recursively, we handle the CURRENT sequence.

    // 1. Collect linear mainline nodes starting from 'node'
    const linearSegment: ReviewTreeNode[] = [];
    let current: ReviewTreeNode | undefined = node;

    // Safety check for loops or massive depth
    let count = 0;
    while (current && count < 200) {
        linearSegment.push(current);

        // Stop if branching (>1 children) because we need to render sub-blocks
        if (current.children && current.children.length > 1) {
            break;
        }
        // Stop if end of line
        if (!current.children || current.children.length === 0) {
            break;
        }

        current = current.children[0];
        count++;
    }

    const lastNode = linearSegment[linearSegment.length - 1];
    const hasBranches = lastNode.children && lastNode.children.length > 1;

    return (
        <>
            {/* Render the gathered linear segment as ONE paragraph block (if it's not empty) */}
            <div className={`inline leading-relaxed ${depth > 0 ? "text-neutral-300" : "text-neutral-200"}`}>
                {linearSegment.map((n, idx) => (
                    <MoveSpan
                        key={n.uci}
                        node={n}
                        isFirst={idx === 0 && !!variationLabel}
                        variationLabel={idx === 0 ? variationLabel : undefined}
                        onInteract={onInteract}
                    />
                ))}
            </div>

            {/* If the last node has branches, render them */}
            {hasBranches && lastNode.children && (
                <div className="block my-2 pl-4 border-l-2 border-neutral-700/50 space-y-2">
                    {/* Render variations (skip index 0 as it is the mainline continuation, 
                        BUT wait, if we break for branches, the mainline continuation 
                        should usually be treated as a new paragraph AFTER the variations? 
                        
                        Standard Chess Book Style:
                        12. e4 e5 13. Nf3 (13. f4?! ... ) 13... Nc6
                        
                        The variation 13. f4 is distinct. 
                        Then 13... Nc6 continues the main flow.
                    */}

                    {/* Render Alternatives (Children 1..N) */}
                    {lastNode.children.slice(1).map((child, i) => (
                        <div key={child.uci} className="mb-2">
                            <NarrativeRenderer
                                node={child}
                                depth={depth + 1}
                                variationLabel={String.fromCharCode(65 + i) + ")"}
                                onInteract={onInteract}
                            />
                        </div>
                    ))}

                    {/* Mainline Continuation (Child 0) 
                        We recursively call Logic for Child 0, effectively starting a new paragraph/segment.
                    */}
                    <div className="mt-2 pt-1">
                        <NarrativeRenderer
                            node={lastNode.children[0]}
                            depth={depth} // Maintain depth for mainline
                            onInteract={onInteract}
                        />
                    </div>
                </div>
            )}
        </>
    );
}

