import React from "react";
import { AnalysisPanel } from "../AnalysisPanel";
import { ConceptCards } from "../ConceptCards";
import { CompressedMoveList } from "../CompressedMoveList";
import { ErrorBoundary } from "../common/ErrorBoundary";

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
  timeline,
  selectedPly,
  reviewRoot,
  setPreviewFen,
  onSelectNode,
  onMoveHover,
  onFlip,
  onToggleEngine,
  onPlayBest,
  onToggleArrows
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
  setPreviewArrows?: (arrows: [string, string, string][]) => void;
  setPreviewFen?: (fen: string | null) => void;
  onSelectNode?: (node: any) => void;
  onMoveHover?: (node: any) => void;
  // Keyboard shortcut handlers
  onFlip?: () => void;
  onToggleEngine?: () => void;
  onPlayBest?: () => void;
  onToggleArrows?: () => void;
}) {
  const maxPly = timeline.length > 0 ? timeline[timeline.length - 1].ply : 0;

  const handleSelectPly = (ply: number) => {
    setSelectedPly(ply);
    setPreviewFen?.(null);
  };

  useKeyboardNavigation({
    onPrev: () => {
      const current = selectedPly ?? 0;
      if (current > 0) handleSelectPly(current - 1);
    },
    onNext: () => {
      const current = selectedPly ?? 0;
      if (current < maxPly) handleSelectPly(current + 1);
    },
    onFirst: () => handleSelectPly(0),
    onLast: () => handleSelectPly(maxPly),
    onFlip,
    onToggleEngine,
    onPlayBest,
    onToggleArrows,
  });

  const [showTreeModal, setShowTreeModal] = React.useState(false);

  return (
    <div className="flex flex-col gap-4 h-full lg:h-[calc(100vh-2rem)]">
      <ConceptCards
        concepts={activeMove?.concepts}
        prevConcepts={enhancedTimeline.find(t => t.ply === (activeMove?.ply ?? 0) - 1)?.concepts}
      />
      <AnalysisPanel
        activeTab={activeTab}
        onTabChange={setActiveTab}
        tabs={[
          { id: "study", label: "Overview", icon: "📖" },
          { id: "moves", label: "Moves", icon: "↔️" }
        ]}
      >
        <div className="h-full overflow-y-auto custom-scrollbar px-1">
          {/* Tab Contents */}
          {activeTab === "moves" && (
            <CompressedMoveList
              timeline={timeline}
              currentPly={selectedPly}
              onSelectPly={handleSelectPly}
            />
          )}
          {activeTab === "study" && review && (
            <ErrorBoundary>
              <AnnotationView
                review={review}
                rootNode={reviewRoot ?? { ply: 0, fen: "", san: "", uci: "", eval: 0, evalType: "cp", judgement: "book", glyph: "", tags: [], pv: [], children: [] } /* fallback */}
                selectedPly={selectedPly}
                onSelectPly={handleSelectPly}
                onPreviewFen={setPreviewFen}
                onSelectNode={onSelectNode}
                onMoveHover={onMoveHover}
              />
            </ErrorBoundary>
          )}
        </div>
      </AnalysisPanel>
    </div>
  );
}
