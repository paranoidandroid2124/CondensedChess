import { useCallback } from "react";
import { Chess } from "chess.js";
import { addBranch } from "../lib/review";
import type { Review } from "../types/review";
import type { EnhancedTimelineNode } from "../lib/review-derived";
import type { GuessState } from "./useGuessMode";

export type PieceDropArgs = {
  sourceSquare: string;
  targetSquare: string;
  piece?: string;
};

export interface BranchCreationOptions {
  review: Review | null;
  enhancedTimeline: EnhancedTimelineNode[];
  selected: EnhancedTimelineNode | null;
  jobId: string;
  isGuessing: boolean;
  activeMove: EnhancedTimelineNode | null;
  guessState: GuessState;
  branchSaving: boolean;
  setReview: (r: Review) => void;
  setSelectedPly: (ply: number | null) => void;
  setPreviewFen: (fen: string | null) => void;
  setPreviewArrows: (arrows: Array<[string, string, string?]>) => void;
  setBranchSaving: (saving: boolean) => void;
  setBranchError: (msg: string | null) => void;
  setGuessState?: (state: GuessState) => void;
  setGuessFeedback?: (msg?: string) => void;
}

export function useBranchCreation(options: BranchCreationOptions) {
  const {
    review,
    enhancedTimeline,
    selected,
    jobId,
    isGuessing,
    activeMove,
    guessState,
    branchSaving,
    setReview,
    setSelectedPly,
    setPreviewFen,
    setPreviewArrows,
    setBranchSaving,
    setBranchError,
    setGuessState,
    setGuessFeedback
  } = options;

  return useCallback(
    ({ sourceSquare, targetSquare }: PieceDropArgs) => {
      // 1. Guess Mode handling
      if (isGuessing && activeMove && guessState !== "correct" && guessState !== "giveup") {
        try {
          const chess = new Chess(activeMove.fenBefore || activeMove.fen);
          const move = chess.move({ from: sourceSquare, to: targetSquare, promotion: "q" });
          if (!move) return false;

          const uci = `${move.from}${move.to}${move.promotion ?? ""}`;
          const bestMoveUci = activeMove.evalBeforeDeep?.lines?.[0]?.move;
          const playedMoveUci = activeMove.uci;

          if (bestMoveUci && (uci === bestMoveUci || (uci.length === 4 && bestMoveUci.startsWith(uci)))) {
            setGuessState?.("correct");
            setGuessFeedback?.(undefined);
            setPreviewFen(chess.fen());
            setPreviewArrows([[move.from, move.to, "#22c55e"]]);
            return true;
          } else if (uci === playedMoveUci) {
            setGuessState?.("incorrect");
            setGuessFeedback?.("That's the move played in the game (the mistake!). Try to find a better one.");
            return false;
          } else {
            setGuessState?.("incorrect");
            setGuessFeedback?.("Not quite the best move. Try again!");
            return false;
          }
        } catch {
          return false;
        }
      }

      // 2. Standard Branch Creation
      if (!review || !enhancedTimeline.length || branchSaving) return false;
      const anchorPly = selected?.ply ?? enhancedTimeline.at(-1)?.ply;
      const anchor = enhancedTimeline.find((t) => t.ply === anchorPly);
      if (!anchor) return false;
      try {
        const chess = new Chess(anchor.fenBefore || anchor.fen);
        const move = chess.move({ from: sourceSquare, to: targetSquare, promotion: "q" });
        if (!move) return false;
        const uci = `${move.from}${move.to}${move.promotion ?? ""}`;
        const newFen = chess.fen();

        setPreviewFen(newFen);
        setBranchSaving(true);
        setBranchError(null);

        addBranch(jobId, anchor.ply, uci)
          .then((updated) => {
            setReview(updated);

            let newPly: number | null = null;
            const nextNode = updated.timeline.find((t) => t.ply === anchor.ply + 1);
            if (nextNode && nextNode.uci === uci) {
              newPly = nextNode.ply;
            } else {
              const candidate = updated.timeline.find((t) => t.fen === newFen);
              if (candidate) newPly = candidate.ply;
            }

            if (newPly !== null) {
              setSelectedPly(newPly);
            }

            setPreviewFen(null);
            setBranchSaving(false);
          })
          .catch((err) => {
            setBranchError(err instanceof Error ? err.message : "Failed to add branch");
            setBranchSaving(false);
            setPreviewFen(null);
          });
        return true;
      } catch {
        return false;
      }
    },
    [
      isGuessing,
      activeMove,
      guessState,
      review,
      enhancedTimeline,
      branchSaving,
      selected,
      jobId,
      setGuessState,
      setGuessFeedback,
      setPreviewFen,
      setPreviewArrows,
      setBranchSaving,
      setBranchError,
      setReview,
      setSelectedPly
    ]
  );
}

