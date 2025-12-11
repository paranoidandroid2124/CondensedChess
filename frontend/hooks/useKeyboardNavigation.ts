import { useEffect } from "react";

export function useKeyboardNavigation({
    onPrev,
    onNext,
    onFirst,
    onLast,
    onUp,
    onDown
}: {
    onPrev: () => void;
    onNext: () => void;
    onFirst?: () => void;
    onLast?: () => void;
    onUp?: () => void;
    onDown?: () => void;
}) {
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            // Avoid interfering with inputs
            if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
                return;
            }
            // Avoid scrolling with arrow keys
            if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Home", "End"].includes(e.key)) {
                e.preventDefault();
            }

            if (e.key === "ArrowLeft") {
                onPrev();
            } else if (e.key === "ArrowRight") {
                onNext();
            } else if (e.key === "ArrowUp") {
                onUp ? onUp() : onPrev();
            } else if (e.key === "ArrowDown") {
                onDown ? onDown() : onNext();
            } else if (e.key === "Home") {
                onFirst?.();
            } else if (e.key === "End") {
                onLast?.();
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onPrev, onNext, onFirst, onLast, onUp, onDown]);
}
