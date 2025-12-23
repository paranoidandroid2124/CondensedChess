import React from "react";
import { ReviewTreeNode, Branch, TimelineNode } from "../../../types/review";

import { MiniBoard } from "../../common/MiniBoard";
import { BookDiagram } from "../../../types/review";
import { HypothesisDiagram } from "./HypothesisDiagram";

interface NarrativeRendererProps {
    node: ReviewTreeNode;
    depth?: number;
    variationLabel?: string;
    onInteract?: (node: ReviewTreeNode) => void;
    onMoveHover?: (node: ReviewTreeNode | null) => void;
    diagrams?: BookDiagram[]; // Pass diagrams for inline rendering
    endPly?: number; // Optional limit to stop rendering
    timeline?: TimelineNode[]; // For hypotheses lookup
    onHypothesisHover?: (branch: Branch | null) => void;
}


// Helper to convert markdown and Q&A format to JSX
function renderMarkdown(text: string) {
    if (!text) return null;

    // Check for Q&A pattern: "Question: ... Answer: ..."
    // Using [\s\S] instead of 's' flag for ES2015 compatibility
    const qaMatch = text.match(/^(Question:\s*)([\s\S]+?)(\s*Answer:\s*)([\s\S]+)$/);
    if (qaMatch) {
        return (
            <span className="block my-4 p-4 rounded-lg bg-amber-900/20 border-l-4 border-amber-500">
                <span className="block">
                    <span className="font-bold text-amber-400 uppercase text-sm tracking-wide">Q: </span>
                    <span className="italic text-white/90">{qaMatch[2].trim()}</span>
                </span>
                <span className="block mt-2">
                    <span className="font-bold text-teal-400 uppercase text-sm tracking-wide">A: </span>
                    <span className="text-white/90">{renderBoldText(qaMatch[4].trim())}</span>
                </span>
            </span>
        );
    }

    return renderBoldText(text);
}

// Helper for bold markdown only
function renderBoldText(text: string) {
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, idx) => {
        if (part.startsWith("**") && part.endsWith("**")) {
            return <strong key={idx} className="font-bold text-white">{part.slice(2, -2)}</strong>;
        }
        return part;
    });
}

// Move Span Component
const MoveSpan = ({ node, isFirst, variationLabel, onInteract, onMoveHover }: {
    node: ReviewTreeNode,
    isFirst?: boolean,
    variationLabel?: string,
    onInteract?: (node: ReviewTreeNode) => void,
    onMoveHover?: (node: ReviewTreeNode | null) => void
}) => {
    const isHypothesis = node.nodeType === "hypothesis";
    // Check if this move starts a sentence or is Black's move to determine numbering
    const moveNumber = Math.ceil((node.ply + 1) / 2);
    const isWhite = node.ply % 2 === 0;
    const showNumber = isWhite || isFirst;
    const numberText = isWhite ? `${moveNumber}.` : `${moveNumber}...`;

    return (
        <span className="inline mr-0.5 leading-relaxed">
            {variationLabel && (
                <span className="inline-block mr-1 font-bold text-neutral-400 text-xs">
                    {variationLabel}
                </span>
            )}

            {showNumber && (
                <span className="text-neutral-500 mr-0.5 select-none font-mono text-[10px]">{numberText}</span>
            )}

            <span
                className={`cursor-pointer hover:bg-yellow-500/20 hover:text-yellow-200 rounded px-0.5 transition-colors mx-0
                    ${isHypothesis ? "text-red-400 decoration-red-900 underline decoration-dashed" : "font-semibold text-sky-300"}
                `}
                onClick={() => onInteract?.(node)}
                onMouseEnter={() => onMoveHover?.(node)}
                onMouseLeave={() => onMoveHover?.(null)}
            >
                {node.san}
            </span>

            {isHypothesis && (
                <span className="inline-flex items-center px-1 py-0 rounded text-[9px] font-medium bg-red-900/30 text-red-200 border border-red-800/50 mx-0.5 align-middle">
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


export function NarrativeRenderer({ node, depth = 0, variationLabel, onInteract, onMoveHover, diagrams, endPly, timeline, onHypothesisHover }: NarrativeRendererProps) {
    // Flatten logic: 
    // We want to transform the tree into a list of "Paragraphs" and "Variation Blocks".
    // But since this component is called recursively, we handle the CURRENT sequence.

    // 1. Collect linear mainline nodes starting from 'node'
    const linearSegment: ReviewTreeNode[] = [];
    let current: ReviewTreeNode | undefined = node;

    // Safety check for loops or massive depth
    let count = 0;
    while (current && count < 200) {
        // Stop if we exceed the section's end ply (if specified)
        if (endPly && current.ply > endPly) {
            break;
        }

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

    // Guard: if segment is empty (e.g. startNode was already beyond endPly), return null
    if (linearSegment.length === 0) return null;

    const lastNode = linearSegment[linearSegment.length - 1];

    // Only render branches if we haven't hit the endPly limit for the *next* move
    const hasBranches = lastNode.children && lastNode.children.length > 1;

    return (
        <>
            {/* Render the gathered linear segment as ONE paragraph block (if it's not empty) */}
            <div className={`inline leading-relaxed ${depth > 0 ? "text-neutral-300 text-sm" : "text-neutral-200"}`}>
                {linearSegment.map((n, idx) => {
                    const diag = diagrams?.find(d => d.ply === n.ply);
                    return (
                        <React.Fragment key={n.uci}>
                            <MoveSpan
                                node={n}
                                isFirst={idx === 0 && !!variationLabel}
                                variationLabel={idx === 0 ? variationLabel : undefined}
                                onInteract={onInteract}
                                onMoveHover={onMoveHover}
                            />
                            {/* Inline Diagram Injection: Only for Mainline (depth 0) to avoid clutter in variations */}
                            {diag && depth === 0 && (
                                <div className="clear-both my-6 flex justify-center">
                                    <div className="bg-neutral-800/80 p-3 rounded-lg border border-neutral-700 shadow-xl max-w-[200px]">
                                        <MiniBoard fen={diag.fen} orientation="white" />
                                        <div className="mt-2 text-[10px] text-neutral-400 text-center font-medium uppercase tracking-wider">
                                            {diag.tags.transition.length > 0 ? diag.tags.transition[0] :
                                                diag.tags.tactic.length > 0 ? "Tactical Moment" :
                                                    diag.tags.structure.length > 0 ? "Key Structure" : "Position"}
                                        </div>
                                    </div>
                                </div>
                            )}
                            {/* Hypothesis Diagram: Show for mainline nodes with hypotheses */}
                            {depth === 0 && (() => {
                                const timelineNode = timeline?.find(t => t.ply === n.ply);
                                const hypotheses = timelineNode?.hypotheses;
                                if (hypotheses && hypotheses.length > 0) {
                                    return (
                                        <div className="clear-both my-4 flex justify-center">
                                            <HypothesisDiagram
                                                fen={n.fen}
                                                hypotheses={hypotheses}
                                                onHoverBranch={onHypothesisHover}
                                            />
                                        </div>
                                    );
                                }
                                return null;
                            })()}
                        </React.Fragment>
                    );
                })}
            </div>

            {/* If the last node has branches, render them */}
            {hasBranches && lastNode.children && (
                <div className="block my-4 pl-5 border-l-2 border-sky-500/30 bg-sky-900/10 rounded-r-lg py-3 space-y-3">
                    <div className="text-[10px] uppercase tracking-widest text-sky-400/70 font-semibold mb-2">Alternative Lines</div>
                    {/* Render Alternatives (Children 1..N) */}
                    {lastNode.children.slice(1).map((child, i) => (
                        <div key={child.uci} className="mb-2 pl-2 border-l border-neutral-600/30">
                            <NarrativeRenderer
                                node={child}
                                depth={depth + 1}
                                variationLabel={String.fromCharCode(65 + i) + ")"}
                                onInteract={onInteract}
                                onMoveHover={onMoveHover}
                                diagrams={diagrams}
                                timeline={timeline}
                                onHypothesisHover={onHypothesisHover}
                            />
                        </div>
                    ))}

                    {/* Mainline Continuation (Child 0) */}
                    {/* Only recurse if the next main move is within bounds */}
                    {lastNode.children[0].ply <= (endPly ?? 9999) && (
                        <div className="mt-2 pt-1">
                            <NarrativeRenderer
                                node={lastNode.children[0]}
                                depth={depth}
                                onInteract={onInteract}
                                onMoveHover={onMoveHover}
                                diagrams={diagrams}
                                endPly={endPly}
                                timeline={timeline}
                                onHypothesisHover={onHypothesisHover}
                            />
                        </div>
                    )}
                </div>
            )}
        </>
    );
}
