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
  onUserMove?: (node: EnhancedTimelineNode) => void;
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
    setGuessFeedback,
    onUserMove
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
      // Allow interaction if we have a timeline, even if review is not yet ready (preview mode)
      if (!enhancedTimeline.length || branchSaving) return false;
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

        // If review is not ready (analyzing), just show preview and return
        if (!review) {
          // Create a temporary node for the UI
          if (onUserMove) {
            const newNode: EnhancedTimelineNode = {
              ply: (anchor.ply) + 1,
              turn: anchor.turn === "white" ? "black" : "white",
              san: move.san,
              uci: uci,
              fen: newFen,
              fenBefore: anchor.fen,
              judgement: undefined,
              // Fill other fields with defaults or nulls
              legalMoves: 0,
              evalBeforeShallow: { depth: 0, lines: [] },
              evalBeforeDeep: { depth: 0, lines: [] },
              winPctBefore: 0,
              deltaWinPct: 0,
              epBefore: 0,
              epAfter: 0,
              epLoss: 0,
              conceptsBefore: {} as any,
              concepts: {} as any,
              conceptDelta: {} as any,
              semanticTags: [],
              mistakeCategory: undefined,
              phaseLabel: undefined,
              practicality: undefined,
              bestVsSecondGap: undefined,
              bestVsPlayedGap: undefined,
              special: undefined,
              winPctAfterForPlayer: undefined,
              features: {} as any
            };
            onUserMove(newNode);
            setSelectedPly(newNode.ply);
          }
          return true;
        }

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

