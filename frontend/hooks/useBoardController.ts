import { useCallback, useMemo, useRef } from "react";
import { Chess } from "chess.js";
import type { Review } from "../types/review";
import type { EnhancedTimelineNode } from "../lib/review-derived";
import { addBranch } from "../lib/review";

export interface BoardControllerOptions {
    // State
    timeline: EnhancedTimelineNode[];
    selectedPly: number | null;
    review: Review | null;

    // Callbacks
    setSelectedPly: (ply: number | null) => void;
    setReview?: (r: Review) => void;
    setPreviewFen?: (fen: string | null) => void;
    setPreviewArrows?: (arrows: Array<[string, string, string?]>) => void;

    // Optional: orientation control
    orientation?: "white" | "black";
    setOrientation?: (o: "white" | "black") => void;

    // Optional: engine control
    isAnalyzing?: boolean;
    toggleAnalysis?: () => void;
    engineLines?: Array<{ pv?: string; cp?: number; mate?: number }>;
}

export interface BoardController {
    // Move execution
    playUci: (uci: string) => boolean;
    playUciList: (uciList: string[], delayMs?: number) => Promise<void>;
    playBestMove: () => boolean;

    // Navigation
    goToMove: (ply: number) => void;
    goToFirst: () => void;
    goToLast: () => void;
    goToPrev: () => void;
    goToNext: () => void;

    // UI control
    flipBoard: () => void;

    // Current state
    currentFen: string;
    orientation: "white" | "black";
}

/**
 * Central board controller hook inspired by Lichess's ctrl.ts.
 * Provides programmatic move execution, navigation, and board control.
 */
export function useBoardController(options: BoardControllerOptions): BoardController {
    const {
        timeline,
        selectedPly,
        review,
        setSelectedPly,
        setReview,
        setPreviewFen,
        setPreviewArrows,
        orientation: orientationProp = "white",
        setOrientation,
        engineLines = [],
    } = options;

    // Track orientation locally if no external setter
    const orientationRef = useRef(orientationProp);
    if (setOrientation) {
        orientationRef.current = orientationProp;
    }

    // Find current node
    const currentNode = useMemo(() => {
        if (selectedPly === null || selectedPly === 0) {
            return timeline[0] ?? null;
        }
        return timeline.find(n => n.ply === selectedPly) ?? null;
    }, [timeline, selectedPly]);

    const currentFen = useMemo(() => {
        return currentNode?.fen ?? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }, [currentNode]);

    /**
     * Execute a single UCI move from the current position.
     * Returns true if the move was valid and executed.
     */
    const playUci = useCallback((uci: string): boolean => {
        if (!uci || uci.length < 4) return false;

        const from = uci.slice(0, 2);
        const to = uci.slice(2, 4);
        const promotion = uci.length > 4 ? uci.slice(4) : undefined;

        try {
            const chess = new Chess(currentFen);
            const move = chess.move({ from, to, promotion });

            if (!move) return false;

            const newFen = chess.fen();
            const newUci = `${move.from}${move.to}${move.promotion ?? ""}`;

            // Check if this move already exists in timeline
            const existingNode = timeline.find(n =>
                n.fenBefore === currentFen && n.uci === newUci
            );

            if (existingNode) {
                // Move already exists - just navigate to it
                setSelectedPly(existingNode.ply);
                setPreviewFen?.(null);
                return true;
            }

            // If we have a review and jobId, try to add as branch
            if (review?.jobId && setReview && currentNode) {
                addBranch(review.jobId, currentNode.ply, newUci)
                    .then((updated) => {
                        setReview(updated);
                        // Find the new node in updated timeline
                        const newNode = updated.timeline.find(t => t.fen === newFen);
                        if (newNode) {
                            setSelectedPly(newNode.ply);
                        }
                    })
                    .catch(console.error);
            } else {
                // No review - just show preview
                setPreviewFen?.(newFen);
                setPreviewArrows?.([[from, to, "#16a34a"]]);
            }

            return true;
        } catch (e) {
            console.error("playUci error:", e);
            return false;
        }
    }, [currentFen, currentNode, timeline, review, setReview, setSelectedPly, setPreviewFen, setPreviewArrows]);

    /**
     * Execute a sequence of UCI moves with optional animation delay.
     */
    const playUciList = useCallback(async (uciList: string[], delayMs = 300): Promise<void> => {
        if (!uciList.length) return;

        // Play first move immediately
        const firstMove = uciList[0];
        playUci(firstMove);

        // Queue remaining moves with delay
        for (let i = 1; i < uciList.length; i++) {
            await new Promise(resolve => setTimeout(resolve, delayMs));
            playUci(uciList[i]);
        }
    }, [playUci]);

    /**
     * Play the engine's best move (first line in engineLines).
     */
    const playBestMove = useCallback((): boolean => {
        if (!engineLines.length || !engineLines[0]?.pv) return false;

        const pv = engineLines[0].pv;
        const firstMove = pv.split(" ")[0];

        if (firstMove) {
            return playUci(firstMove);
        }
        return false;
    }, [engineLines, playUci]);

    // Navigation
    const goToMove = useCallback((ply: number) => {
        const target = timeline.find(n => n.ply === ply);
        if (target) {
            setSelectedPly(ply);
            setPreviewFen?.(null);
        }
    }, [timeline, setSelectedPly, setPreviewFen]);

    const goToFirst = useCallback(() => {
        if (timeline.length > 0) {
            setSelectedPly(timeline[0].ply);
            setPreviewFen?.(null);
        }
    }, [timeline, setSelectedPly, setPreviewFen]);

    const goToLast = useCallback(() => {
        if (timeline.length > 0) {
            setSelectedPly(timeline[timeline.length - 1].ply);
            setPreviewFen?.(null);
        }
    }, [timeline, setSelectedPly, setPreviewFen]);

    const goToPrev = useCallback(() => {
        if (selectedPly === null) return;
        const idx = timeline.findIndex(n => n.ply === selectedPly);
        if (idx > 0) {
            setSelectedPly(timeline[idx - 1].ply);
            setPreviewFen?.(null);
        }
    }, [timeline, selectedPly, setSelectedPly, setPreviewFen]);

    const goToNext = useCallback(() => {
        if (selectedPly === null) {
            if (timeline.length > 0) {
                setSelectedPly(timeline[0].ply);
            }
            return;
        }
        const idx = timeline.findIndex(n => n.ply === selectedPly);
        if (idx >= 0 && idx < timeline.length - 1) {
            setSelectedPly(timeline[idx + 1].ply);
            setPreviewFen?.(null);
        }
    }, [timeline, selectedPly, setSelectedPly, setPreviewFen]);

    // UI control
    const flipBoard = useCallback(() => {
        const newOrientation = orientationRef.current === "white" ? "black" : "white";
        orientationRef.current = newOrientation;
        setOrientation?.(newOrientation);
    }, [setOrientation]);

    return {
        playUci,
        playUciList,
        playBestMove,
        goToMove,
        goToFirst,
        goToLast,
        goToPrev,
        goToNext,
        flipBoard,
        currentFen,
        orientation: orientationRef.current,
    };
}
