import { Chess } from "chess.js";
import { AnalysisPanel } from "../AnalysisPanel";
import { BestAlternatives } from "../BestAlternatives";
import { OpeningExplorerTab } from "../OpeningExplorerTab";
import { StudyTab } from "../StudyTab";
import { ConceptsTab } from "../ConceptsTab";
import { ConceptCards } from "../ConceptCards";
import { GuessTheMove } from "../GuessTheMove";
import { uciToSan } from "../../lib/chess-utils";
import type { Review } from "../../types/review";
import type { EnhancedTimelineNode } from "../../lib/review-derived";
import type { EngineMessage } from "../../lib/engine";
import type { VariationEntry } from "./TimelineView";

export function AnalysisTabsSection({
  activeMove,
  enhancedTimeline,
  review,
  activeTab,
  setActiveTab,
  engineLines,
  isAnalyzing,
  toggleAnalysis,
  openingLookup,
  lookupLoading,
  lookupError,
  setSelectedPly,
  setSelectedVariation,
  isGuessing,
  setIsGuessing,
  guessState,
  setGuessState,
  guessFeedback,
  setGuessFeedback,
  setPreviewFen,
  setPreviewArrows,
  setPreviewLabel
}: {
  activeMove: EnhancedTimelineNode | null;
  enhancedTimeline: EnhancedTimelineNode[];
  review: Review;
  activeTab: "engine" | "opening" | "study" | "concepts";
  setActiveTab: (tab: "engine" | "opening" | "study" | "concepts") => void;
  engineLines: EngineMessage[];
  isAnalyzing: boolean;
  toggleAnalysis: () => void;
  openingLookup: Review["openingStats"] | null;
  lookupLoading: boolean;
  lookupError: string | null;
  setSelectedPly: (ply: number) => void;
  setSelectedVariation: (v: VariationEntry | null) => void;
  isGuessing: boolean;
  setIsGuessing: (v: boolean) => void;
  guessState: "waiting" | "correct" | "incorrect" | "giveup";
  setGuessState: (v: "waiting" | "correct" | "incorrect" | "giveup") => void;
  guessFeedback: string | undefined;
  setGuessFeedback: (v: string | undefined) => void;
  setPreviewFen: (fen: string | null) => void;
  setPreviewArrows: (arrows: Array<[string, string, string?]>) => void;
  setPreviewLabel: (label: string | null) => void;
}) {
  return (
    <div className="flex flex-col gap-4 lg:h-[calc(100vh-2rem)] lg:sticky lg:top-4">
      <ConceptCards
        concepts={activeMove?.concepts}
        prevConcepts={enhancedTimeline.find(t => t.ply === (activeMove?.ply ?? 0) - 1)?.concepts}
      />
      <AnalysisPanel activeTab={activeTab} onTabChange={setActiveTab}>
        {activeTab === "engine" && (
          <BestAlternatives
            lines={engineLines}
            isAnalyzing={isAnalyzing}
            onToggleAnalysis={toggleAnalysis}
            onPreviewLine={(pv) => {
              if (!activeMove?.fenBefore || !pv) return;
              try {
                const chess = new Chess(activeMove.fenBefore);
                const arrows: Array<[string, string, string?]> = [];
                pv.split(" ").slice(0, 8).forEach((mv) => {
                  try {
                    const move = (chess as any).move(mv, { sloppy: true });
                    if (move?.from && move?.to) arrows.push([move.from, move.to, "#10b981"]);
                  } catch {
                    // ignore
                  }
                });
                setPreviewFen(chess.fen());
                setPreviewArrows(arrows);
                setPreviewLabel("Engine Line");
              } catch {
                // ignore
              }
            }}
          />
        )}

        {activeTab === "opening" && (
          <OpeningExplorerTab
            stats={openingLookup}
            loading={lookupLoading}
            error={lookupError}
          />
        )}

        {activeTab === "study" && (
          <StudyTab
            chapters={review.studyChapters}
            onSelectChapter={(ply) => {
              setSelectedPly(ply);
              setSelectedVariation(null);
            }}
            onStartGuess={(chapter) => {
              setSelectedPly(chapter.anchorPly);
              setIsGuessing(true);
              setGuessState("waiting");
              setGuessFeedback(undefined);
            }}
          />
        )}

        {activeTab === "concepts" && (
          <ConceptsTab
            review={review}
            currentConcepts={activeMove?.concepts}
          />
        )}

        {isGuessing && activeMove && (
          <div className="absolute inset-0 z-20 bg-black/80 p-4 backdrop-blur-sm">
            <GuessTheMove
              targetPly={activeMove.ply}
              fenBefore={activeMove.fenBefore || ""}
              bestMoveSan={
                activeMove.evalBeforeDeep?.lines?.[0]?.move
                  ? uciToSan(activeMove.fenBefore || "", activeMove.evalBeforeDeep.lines[0].move)
                  : "Unknown"
              }
              playedMoveSan={activeMove.san}
              guessState={guessState}
              feedbackMessage={guessFeedback}
              onSolve={() => {
                setIsGuessing(false);
              }}
              onGiveUp={() => {
                setGuessState("giveup");
                const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
                if (best) {
                  const from = best.slice(0, 2);
                  const to = best.slice(2, 4);
                  setPreviewArrows([[from, to, "#22c55e"]]);
                }
              }}
              onClose={() => setIsGuessing(false)}
            />
          </div>
        )}
      </AnalysisPanel>
    </div>
  );
}

