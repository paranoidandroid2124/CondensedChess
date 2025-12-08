import React from "react";
import { Chess } from "chess.js";
import { AnalysisPanel } from "../AnalysisPanel";
import { OpeningExplorerTab } from "../OpeningExplorerTab";
import { ConceptsTab } from "../ConceptsTab";
import { ConceptCards } from "../ConceptCards";
import { CompressedMoveList } from "../CompressedMoveList";
import { QuickJump } from "../common/QuickJump";

import { AnnotationView } from "./BookView/AnnotationView";
import { VerticalTree } from "./BookView/VerticalTree";
import { MoveListPanel } from "./BookView/MoveListPanel";
import { TreeModal } from "./TreeModal";
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
  selectedPly: number | null;
  tabOrder?: TabId[];
  setPreviewArrows?: (arrows: [string, string, string][]) => void;
}) {
  const tabs: TabId[] = tabOrder ?? ["concepts", "opening", "moves", "tree", "study"];

  const [showTreeModal, setShowTreeModal] = React.useState(false);

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
          <div className="flex-1 flex gap-4 h-full relative">
            {/* Left Panel: Move List */}
            <div className="w-64 flex-shrink-0 h-full overflow-hidden hidden lg:block">
              <MoveListPanel
                timeline={timeline}
                currentPly={selectedPly}
                onSelectPly={setSelectedPly}
              />
            </div>

            {/* Right Panel: Vertical Tree */}
            <div className="flex-1 overflow-x-auto p-4 relative bg-black/20 rounded-xl border border-white/5">
              {review?.root ? (
                <>
                  <button
                    onClick={() => setShowTreeModal(true)}
                    className="absolute top-2 right-2 z-10 p-1.5 rounded-lg bg-white/10 text-white/60 hover:text-white hover:bg-white/20 transition-colors"
                    title="Expand Tree View"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                    </svg>
                  </button>
                  <VerticalTree
                    rootNode={review.root}
                    currentPly={selectedPly}
                    onSelectPly={setSelectedPly}
                    isRoot={true}
                  />
                </>
              ) : (
                <div className="text-white/60 text-center mt-10">
                  Tree view not available.
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === "study" && (
          <AnnotationView
            review={review as Review}
            rootNode={reviewRoot}
            selectedPly={selectedPly}
            onSelectPly={setSelectedPly}
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
      </AnalysisPanel>

      {showTreeModal && review?.root && (
        <TreeModal
          rootNode={review.root}
          currentPly={selectedPly}
          onSelectPly={setSelectedPly}
          onClose={() => setShowTreeModal(false)}
          timeline={timeline}
        />
      )}
    </div>
  );
}
