import { Chess } from "chess.js";
import { AnalysisPanel } from "../AnalysisPanel";
import { OpeningExplorerTab } from "../OpeningExplorerTab";
import { StudyTab } from "../StudyTab";
import { ConceptsTab } from "../ConceptsTab";
import { ConceptCards } from "../ConceptCards";
import { GuessTheMove } from "../GuessTheMove";
import { uciToSan } from "../../lib/chess-utils";
import { CompressedMoveList } from "../CompressedMoveList";
import { QuickJump } from "../common/QuickJump";
import { VariationTree } from "./VariationTree";
import type { Review } from "../../types/review";
import type { EnhancedTimelineNode } from "../../lib/review-derived";
import type { EngineMessage } from "../../lib/engine";
import type { VariationEntry } from "./TimelineView";
import type { TabId } from "../AnalysisPanel";

export function AnalysisTabsSection({
  activeMove,
  enhancedTimeline,
  review,
  activeTab,
  setActiveTab,
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
  tabOrder,
  timeline,
  selectedPly,
  reviewRoot
}: {
  activeMove: EnhancedTimelineNode | null;
  enhancedTimeline: EnhancedTimelineNode[];
  timeline: EnhancedTimelineNode[];
  review: Review | null;
  reviewRoot?: Review["root"];
  activeTab: TabId;
  setActiveTab: (tab: TabId) => void;
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
  selectedPly: number | null;
  tabOrder?: TabId[];
}) {
  const tabs: TabId[] = tabOrder ?? ["concepts", "opening", "moves", "tree", "study"];

  return (
    <div className="flex flex-col gap-4 lg:h-[calc(100vh-2rem)] lg:sticky lg:top-4">
      <ConceptCards
        concepts={activeMove?.concepts}
        prevConcepts={enhancedTimeline.find(t => t.ply === (activeMove?.ply ?? 0) - 1)?.concepts}
      />
      <AnalysisPanel
        activeTab={activeTab}
        onTabChange={setActiveTab}
        tabs={tabs.map((id) => {
          const labels: Record<TabId, string> = {
            opening: "Opening",
            moves: "Moves",
            tree: "Tree",
            study: "Study",
            concepts: "Concepts"
          };
          const icons: Partial<Record<TabId, string>> = {
            opening: "📖",
            moves: "↔️",
            tree: "🌿",
            study: "🎓",
            concepts: "💡"
          };
          return { id, label: labels[id], icon: icons[id] };
        })}
      >
        {activeTab === "opening" && (
          <OpeningExplorerTab
            stats={openingLookup}
            loading={lookupLoading}
            error={lookupError}
          />
        )}

        {activeTab === "moves" && (
          <div className="space-y-3">
            <CompressedMoveList timeline={timeline} currentPly={selectedPly} onSelectPly={setSelectedPly} />
            <QuickJump timeline={timeline} onSelect={setSelectedPly} />
          </div>
        )}

        {activeTab === "tree" && (
          <VariationTree root={reviewRoot} onSelect={setSelectedPly} selected={selectedPly ?? undefined} />
        )}

        {activeTab === "study" && (
          <StudyTab
            chapters={review?.studyChapters}
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
            currentSemanticTags={activeMove?.semanticTags}
            conceptDelta={activeMove?.conceptDelta}
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
