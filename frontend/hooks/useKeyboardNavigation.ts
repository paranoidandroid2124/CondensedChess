import { useEffect, useCallback } from "react";

export interface KeyboardNavigationOptions {
    // Basic navigation
    onPrev: () => void;
    onNext: () => void;
    onFirst?: () => void;
    onLast?: () => void;
    onUp?: () => void;
    onDown?: () => void;

    // Lichess-style shortcuts
    onPlayBest?: () => void;      // Space
    onFlip?: () => void;          // 'f'
    onToggleEngine?: () => void;  // 'l'
    onToggleArrows?: () => void;  // 'a'
    onPrevBranch?: () => void;    // Shift+Left
    onNextBranch?: () => void;    // Shift+Right
}

export function useKeyboardNavigation({
    onPrev,
    onNext,
    onFirst,
    onLast,
    onUp,
    onDown,
    onPlayBest,
    onFlip,
    onToggleEngine,
    onToggleArrows,
    onPrevBranch,
    onNextBranch,
}: KeyboardNavigationOptions) {
    const handleKeyDown = useCallback((e: KeyboardEvent) => {
        // Avoid interfering with inputs
        if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
            return;
        }

        // Arrow-based navigation (prevent page scroll)
        if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Home", "End", " "].includes(e.key)) {
            e.preventDefault();
        }

        // Shift+Arrow for branch navigation
        if (e.shiftKey) {
            if (e.key === "ArrowLeft") {
                onPrevBranch?.();
                return;
            } else if (e.key === "ArrowRight") {
                onNextBranch?.();
                return;
            }
        }

        // Basic navigation
        if (e.key === "ArrowLeft" || e.key === "k") {
            onPrev();
        } else if (e.key === "ArrowRight" || e.key === "j") {
            onNext();
        } else if (e.key === "ArrowUp") {
            onUp ? onUp() : onPrev();
        } else if (e.key === "ArrowDown") {
            onDown ? onDown() : onNext();
        } else if (e.key === "Home" || e.key === "0") {
            onFirst?.();
        } else if (e.key === "End" || e.key === "$") {
            onLast?.();
        }

        // Lichess-style shortcuts
        else if (e.key === " ") {
            // Space = play best move
            onPlayBest?.();
        } else if (e.key === "f") {
            // Flip board
            onFlip?.();
        } else if (e.key === "l") {
            // Toggle engine analysis
            onToggleEngine?.();
        } else if (e.key === "a") {
            // Toggle arrows
            onToggleArrows?.();
        }
    }, [onPrev, onNext, onFirst, onLast, onUp, onDown, onPlayBest, onFlip, onToggleEngine, onToggleArrows, onPrevBranch, onNextBranch]);

    useEffect(() => {
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [handleKeyDown]);
}

