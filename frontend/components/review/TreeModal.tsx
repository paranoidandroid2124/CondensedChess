import React, { useRef, useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { ReviewTreeNode } from "../../types/review";
import { VerticalTree } from "./BookView/VerticalTree";
import { MoveListPanel } from "./BookView/MoveListPanel";
import { EnhancedTimelineNode } from "../../lib/review-derived";

interface TreeModalProps {
    rootNode: ReviewTreeNode;
    currentPly: number | null;
    onSelectPly: (ply: number) => void;
    onClose: () => void;
    timeline?: EnhancedTimelineNode[];
}

export function TreeModal({ rootNode, currentPly, onSelectPly, onClose, timeline }: TreeModalProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [isDragging, setIsDragging] = useState(false);
    const [position, setPosition] = useState({ x: 0, y: 0 });
    const [startPos, setStartPos] = useState({ x: 0, y: 0 });
    const [scale, setScale] = useState(1);
    const [mounted, setMounted] = useState(false);

    // Center the tree initially
    useEffect(() => {
        setMounted(true);
        // Use requestAnimationFrame to ensure layout is computed
        requestAnimationFrame(() => {
            if (containerRef.current) {
                const { clientWidth } = containerRef.current;
                // Center horizontally (assuming node width ~120px), start from top (y=100)
                setPosition({ x: clientWidth / 2 - 60, y: 100 });
            }
        });

        // Force overflow hidden on body when modal is open
        document.body.style.overflow = "hidden";
        return () => {
            document.body.style.overflow = "";
        };
    }, []);

    const handleMouseDown = (e: React.MouseEvent) => {
        setIsDragging(true);
        setStartPos({ x: e.clientX - position.x, y: e.clientY - position.y });
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (!isDragging) return;
        setPosition({
            x: e.clientX - startPos.x,
            y: e.clientY - startPos.y
        });
    };

    const handleMouseUp = () => {
        setIsDragging(false);
    };

    const handleWheel = (e: React.WheelEvent) => {
        e.stopPropagation();
        // Zoom logic
        if (e.ctrlKey || e.metaKey) {
            e.preventDefault();
            const delta = e.deltaY > 0 ? 0.9 : 1.1;
            setScale(s => Math.min(Math.max(0.5, s * delta), 2));
        } else {
            // Pan logic for trackpads
            setPosition(p => ({
                x: p.x - e.deltaX,
                y: p.y - e.deltaY
            }));
        }
    };

    const modalContent = (
        <div className="fixed inset-0 z-[100] flex bg-black/90 backdrop-blur-md animate-in fade-in duration-200">
            {/* Main Canvas Area */}
            <div className="relative flex-1 h-full overflow-hidden">
                {/* Close Button (Top-Right of Canvas) */}
                <button
                    onClick={onClose}
                    className="absolute top-6 right-6 z-[110] rounded-full bg-white/10 p-2 text-white/60 hover:bg-white/20 hover:text-white transition-colors"
                >
                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>

                {/* Controls (Bottom Center of Canvas) */}
                <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-[110] flex gap-2 rounded-full bg-black/50 border border-white/10 p-1 backdrop-blur-md shadow-2xl">
                    <button
                        onClick={() => setScale(s => Math.max(0.5, s - 0.1))}
                        className="p-2 text-white/60 hover:text-white rounded-full hover:bg-white/10"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                        </svg>
                    </button>
                    <span className="px-2 py-2 text-sm font-mono text-white/80 min-w-[3rem] text-center">
                        {Math.round(scale * 100)}%
                    </span>
                    <button
                        onClick={() => setScale(s => Math.min(2, s + 0.1))}
                        className="p-2 text-white/60 hover:text-white rounded-full hover:bg-white/10"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                    </button>
                    <div className="w-px bg-white/10 mx-1" />
                    <button
                        onClick={() => {
                            if (containerRef.current) {
                                setPosition({ x: containerRef.current.clientWidth / 2 - 60, y: 100 });
                            }
                            setScale(1);
                        }}
                        className="px-3 py-2 text-xs text-white/60 hover:text-white rounded-full hover:bg-white/10"
                    >
                        Reset
                    </button>
                </div>

                {/* Canvas */}
                <div
                    ref={containerRef}
                    className={`w-full h-full cursor-grab ${isDragging ? "cursor-grabbing" : ""}`}
                    onMouseDown={handleMouseDown}
                    onMouseMove={handleMouseMove}
                    onMouseUp={handleMouseUp}
                    onMouseLeave={handleMouseUp}
                    onWheel={handleWheel}
                >
                    <div
                        style={{
                            transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
                            transformOrigin: "0 0",
                            transition: isDragging ? "none" : "transform 0.1s ease-out"
                        }}
                        className="absolute top-0 left-0"
                    >
                        <VerticalTree
                            rootNode={rootNode}
                            currentPly={currentPly}
                            onSelectPly={onSelectPly}
                            isRoot={true}
                        />
                    </div>
                </div>
            </div>

            {/* Right Panel: Move List */}
            {timeline && (
                <div className="w-80 h-full border-l border-white/10 bg-black/40 backdrop-blur-xl p-4 flex flex-col z-[120]">
                    <div className="flex-1 overflow-hidden rounded-xl border border-white/5 bg-white/5">
                        <MoveListPanel
                            timeline={timeline}
                            currentPly={currentPly}
                            onSelectPly={onSelectPly}
                        />
                    </div>
                </div>
            )}
        </div>
    );

    if (!mounted) return null;

    return createPortal(modalContent, document.body);
}
