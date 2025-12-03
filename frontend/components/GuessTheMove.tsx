import React, { useState, useEffect } from "react";
import { CriticalNode, TimelineNode } from "../types/review";

type GuessState = "waiting" | "correct" | "incorrect" | "giveup";

type GuessTheMoveProps = {
    targetPly: number;
    fenBefore: string;
    bestMoveSan: string;
    playedMoveSan: string; // The mistake played in the game
    onSolve: () => void;
    onGiveUp: () => void;
    onClose: () => void;
    guessState: GuessState;
    feedbackMessage?: string;
};

export function GuessTheMove({
    targetPly,
    bestMoveSan,
    playedMoveSan,
    onSolve,
    onGiveUp,
    onClose,
    guessState,
    feedbackMessage
}: GuessTheMoveProps) {
    const isWhiteTurn = targetPly % 2 !== 0;

    return (
        <div className="flex flex-col gap-4 rounded-xl border border-accent-teal/30 bg-accent-teal/5 p-6">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold text-white">
                    Find the best move for {isWhiteTurn ? "White" : "Black"}
                </h3>
                <button
                    onClick={onClose}
                    className="rounded-full p-1 text-white/40 hover:bg-white/10 hover:text-white"
                >
                    ✕
                </button>
            </div>

            <div className="text-sm text-white/80">
                {guessState === "waiting" && (
                    <p>Make a move on the board to guess.</p>
                )}
                {guessState === "correct" && (
                    <div className="flex items-center gap-2 text-emerald-400 font-bold">
                        <span className="text-xl">✓</span>
                        <span>Correct! {bestMoveSan} is the best move.</span>
                    </div>
                )}
                {guessState === "incorrect" && (
                    <div className="flex items-center gap-2 text-rose-400 font-bold">
                        <span className="text-xl">✗</span>
                        <span>{feedbackMessage || "Not quite. Try again!"}</span>
                    </div>
                )}
                {guessState === "giveup" && (
                    <div className="flex items-center gap-2 text-amber-400 font-bold">
                        <span>The best move was {bestMoveSan}.</span>
                    </div>
                )}
            </div>

            {guessState === "waiting" || guessState === "incorrect" ? (
                <div className="flex justify-end">
                    <button
                        onClick={onGiveUp}
                        className="text-xs text-white/40 hover:text-white underline"
                    >
                        I give up, show me the move
                    </button>
                </div>
            ) : (
                <div className="flex justify-end">
                    <button
                        onClick={onSolve} // Or "Next Chapter" if we had a list passed
                        className="rounded-lg bg-accent-teal px-4 py-2 text-sm font-bold text-black hover:bg-accent-teal/90"
                    >
                        Continue
                    </button>
                </div>
            )}
        </div>
    );
}
