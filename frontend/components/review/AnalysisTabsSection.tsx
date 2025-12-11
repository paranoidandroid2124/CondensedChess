import React from "react";
import { Chess } from "chess.js";
import { AnalysisPanel } from "../AnalysisPanel";
import { OpeningExplorerTab } from "../OpeningExplorerTab";
import { ConceptsTab } from "../ConceptsTab";
import { ConceptCards } from "../ConceptCards";
import { CompressedMoveList } from "../CompressedMoveList";
import { QuickJump } from "../common/QuickJump";

import { AnnotationView } from "./BookView/AnnotationView";
import type { Review } from "../../types/review";
import type { EnhancedTimelineNode } from "../../lib/review-derived";
import type { EngineMessage } from "../../lib/engine";
import type { VariationEntry } from "./TimelineView";
import type { TabId } from "../AnalysisPanel";

import { useKeyboardNavigation } from "../../hooks/useKeyboardNavigation";

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
  reviewRoot,
  setPreviewFen,
  onSelectNode,
  onMoveHover
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
  setPreviewFen?: (fen: string | null) => void;
  onSelectNode?: (node: any) => void;
  onMoveHover?: (node: any) => void;
}) {
  const tabs: TabId[] = tabOrder ?? ["concepts", "opening", "moves", "study"];
  const maxPly = timeline.length > 0 ? timeline[timeline.length - 1].ply : 0;

  useKeyboardNavigation({
    onPrev: () => {
      const current = selectedPly ?? 0;
      if (current > 0) setSelectedPly(current - 1);
    },
    onNext: () => {
      const current = selectedPly ?? 0;
      if (current < maxPly) setSelectedPly(current + 1);
    },
    onFirst: () => setSelectedPly(0),
    onLast: () => setSelectedPly(maxPly)
  });

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
            study: "Book",
            concepts: "Concepts"
          };
          const icons: Partial<Record<TabId, string>> = {
            opening: "📖",
            moves: "↔️",
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

        {/* Tree tab removed as per user request */}

        {activeTab === "study" && review && (
          <AnnotationView
            review={review}
            rootNode={reviewRoot}
            selectedPly={selectedPly}
            onSelectPly={setSelectedPly}
            onPreviewFen={setPreviewFen}
            onSelectNode={onSelectNode}
            onMoveHover={onMoveHover}
          />
        )}

        {activeTab === "concepts" && (
          <ConceptsTab
            review={review}
            currentConcepts={activeMove?.concepts}
            currentSemanticTags={activeMove?.semanticTags}
            conceptDelta={activeMove?.conceptDelta}
            timeline={enhancedTimeline}
            currentPly={selectedPly ?? undefined}
          />
        )}
      </AnalysisPanel>

      {/* TreeModal removed */}
    </div>
  );
}
