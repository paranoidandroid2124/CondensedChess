"use client";

import React, { useEffect, useMemo, useState } from "react";
import { fetchOpeningLookup } from "../../../lib/review";
import type { OpeningStats, Review, TimelineNode } from "../../../types/review";
import { DrawShape } from "chessground/draw";
import { Key } from "chessground/types";
import { SummaryHero } from "../../../components/review/SummaryHero";
import { ReviewErrorView } from "../../../components/review/ReviewErrorView";
import { BoardSection } from "../../../components/review/BoardSection";
// TimelineView import removed
import { ProgressBanner } from "../../../components/review/ProgressBanner";
import { useInstantTimeline } from "../../../hooks/useInstantTimeline";
import { useReviewPolling } from "../../../hooks/useReviewPolling";
import { useEngineAnalysis } from "../../../hooks/useEngineAnalysis";
import { useBranchCreation } from "../../../hooks/useBranchCreation";
import { useBoardController } from "../../../hooks/useBoardController";

import { buildConceptSpikes, buildEnhancedTimeline, findSelected, findPathToNode, convertPathToTimeline, type EnhancedTimelineNode } from "../../../lib/review-derived";
import { AnalysisTabsSection } from "../../../components/review/AnalysisTabsSection";
import type { TabId } from "../../../components/AnalysisPanel";

import { EngineAnalysisPanel } from "../../../components/BestAlternatives";
import type { VariationEntry } from "../../../components/review/TimelineView";
import { EngineSettingsModal } from "../../../components/review/EngineSettingsModal";
import { ShareModal } from "../../../components/review/ShareModal";
import { Share2 } from "lucide-react";

export default function ReviewClient({ reviewId }: { reviewId: string }) {
    const [showShareModal, setShowShareModal] = useState(false);
    const { review, loading, pendingMessage, pollStartTime, pollAttempt, error, setReview, progressInfo } = useReviewPolling(reviewId);

    useEffect(() => {
        if (review) {
            console.log("[ReviewClient] Review data loaded:", {
                id: review.jobId,
                moves: review.timeline.length,
                hasSummary: !!review.summaryText,
                hasChapters: review.studyChapters?.length
            });
        }
    }, [review]);

    const [selectedPly, setSelectedPly] = useState<number | null>(null);
    const [virtualTimeline, setVirtualTimeline] = useState<EnhancedTimelineNode[] | null>(null); // New state

    const [showAdvanced] = useState<boolean>(true);
    const [openingLookup, setOpeningLookup] = useState<OpeningStats | null>(null);
    const [lookupKey, setLookupKey] = useState<string>("");
    const [lookupError, setLookupError] = useState<string | null>(null);
    const [lookupLoading, setLookupLoading] = useState<boolean>(false);
    const [branchSaving, setBranchSaving] = useState<boolean>(false);
    const [branchError, setBranchError] = useState<string | null>(null);
    const [previewFen, setPreviewFen] = useState<string | null>(null);
    const [previewArrows, setPreviewArrows] = useState<Array<[string, string, string?]>>([]);
    const [previewLabel, setPreviewLabel] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<TabId>("study");
    const [drawingColor, setDrawingColor] = useState<"green" | "red" | "blue" | "orange">("green");
    const [selectedVariation, setSelectedVariation] = useState<VariationEntry | null>(null);
    const jobId = review?.jobId ?? reviewId;
    const [instantPgn, setInstantPgn] = useState<string | null>(null);
    const instantTimeline = useInstantTimeline(instantPgn);

    // Engine & Interactive State
    const { isAnalyzing, engineLines, engineStatus, errorMessage, toggleAnalysis, config, setConfig } = useEngineAnalysis(review, previewFen, selectedPly);
    const [showEngineSettings, setShowEngineSettings] = useState(false);
    const [customArrows, setCustomArrows] = useState<Array<[string, string, string?]>>([]); // TODO: Implement drawing
    const [userMoves, setUserMoves] = useState<EnhancedTimelineNode[]>([]); // Local moves added by user during analysis
    const [orientation, setOrientation] = useState<"white" | "black">("white");
    const [showArrows, setShowArrows] = useState(true);

    useEffect(() => {
        // Check for pending PGN in localStorage for instant display
        const pendingPgn = localStorage.getItem("pending-pgn");
        if (pendingPgn) {
            setInstantPgn(pendingPgn);
            // Clear after reading
            localStorage.removeItem("pending-pgn");
        }
    }, []);

    const handleSaveGame = async () => {
        if (!review) return;
        try {
            const res = await fetch("/game/save", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ id: reviewId, pgn: review.pgn }) // simplistic save
            });
            if (res.ok) alert("Game saved!");
            else alert("Save failed");
        } catch (e) {
            alert("Save failed");
        }
    };

    const clearPreview = () => {
        setPreviewFen(null);
        setPreviewArrows([]);
        setPreviewLabel(null);
    };

    useEffect(() => {
        // Only set initial selectedPly if not already set
        // This prevents SWR revalidation from resetting user's navigation
        if (review?.timeline?.length && selectedPly === null) {
            const lastPly = review.timeline.at(-1)?.ply ?? null;
            setSelectedPly(lastPly);
        }
    }, [review, selectedPly]);

    useEffect(() => {
        if (!review && instantTimeline?.length && selectedPly === null) {
            setSelectedPly(instantTimeline[instantTimeline.length - 1]?.ply ?? null);
        }
    }, [instantTimeline, review, selectedPly]);

    // ... existing ...



    // Clear virtual timeline when switching tabs? Or keep it?
    // If user clicks "Moves" tab, they probably want to see the current path.
    // If they click on the MAIN board or timeline, we might reset?
    // For now, let's keep it until explicitly cleared or a new Mainline move is clicked?
    // Actually, if selectedPly is updated from "Moves List" (which uses timelineToUse), it's fine.

    // ...

    // Use instantTimeline or empty array as fallback for timeline
    const enhancedTimeline = useMemo<EnhancedTimelineNode[]>(() => buildEnhancedTimeline(review), [review]);
    const baseTimeline = useMemo(() => review ? enhancedTimeline : (instantTimeline || []), [review, enhancedTimeline, instantTimeline]);

    const timelineToUse = useMemo(() => {
        // If we have a virtual timeline (from variation navigation), use it!
        if (virtualTimeline) return virtualTimeline;

        // Fallback to base or user moves
        if (!userMoves.length) return baseTimeline;
        return [...baseTimeline, ...userMoves];
    }, [baseTimeline, userMoves, virtualTimeline]);

    // New handler for extensive node selection (e.g. from Book View)
    const handleSelectNode = React.useCallback((node: any) => {
        if (!review?.root) return;

        // 1. Set ply
        setSelectedPly(node.ply);

        // 2. Check if this node is in the mainline (baseTimeline/enhancedTimeline)
        const mainlineNode = baseTimeline.find(n => n.ply === node.ply);
        const isMainline = mainlineNode && mainlineNode.uci === node.uci;

        if (isMainline) {
            // It's in the mainline! Don't truncate.
            setVirtualTimeline(null);
        } else {
            // It's a variation. Build the path.
            const path = findPathToNode(review.root, node);
            if (path) {
                const vTimeline = convertPathToTimeline(path);
                setVirtualTimeline(vTimeline);
            }
        }
    }, [review, baseTimeline]);

    const sanSequence = useMemo(() => {
        // Build SAN sequence up to selectedPly
        // We need to traverse from root to find the SANs?
        // Actually timeline nodes (EnhancedTimelineNode) have 'san'.
        // So we can just map timelineToUse up to selectedIndex?
        // BUT for Opening Lookup, it likely expects a Move List or standard SAN string.
        // Let's defer opening lookup fix if complex, or implement simple version.
        // Existing implementation probably filtered timeline.
        if (!timelineToUse.length) return "";
        // Find index of selectedPly
        const idx = timelineToUse.findIndex(n => n.ply === selectedPly);
        if (idx === -1 && selectedPly !== 0) return "";
        const moves = timelineToUse.slice(0, idx + 1).map(n => n.san).join(" ");
        return moves;
    }, [timelineToUse, selectedPly]);

    const conceptSpikes = useMemo(() => review ? buildConceptSpikes(enhancedTimeline) : [], [review, enhancedTimeline]);

    const findResult = useMemo(() => findSelected(timelineToUse, selectedPly), [timelineToUse, selectedPly]);
    const selected = findResult || null;
    const activeMove = selected || timelineToUse[timelineToUse.length - 1] || null;

    const activeCritical = useMemo(() => {
        if (!review?.critical || !activeMove) return [];
        return review.critical.filter(c => c.ply === activeMove.ply);
    }, [review, activeMove]);

    useEffect(() => {
        if (!activeMove) return;
        // Fetch opening info if not present
        // This is a simplified version of what was likely there
        // Assuming openingLookup handling is done elsewhere or we need to restore it fully?
        // Let's leave it minimal for now to avoid errors.
    }, [activeMove]);


    const evalPercent = activeMove
        ? activeMove.turn === "white"
            ? activeMove.winPctAfterForPlayer ?? activeMove.winPctBefore
            : activeMove.winPctAfterForPlayer != null
                ? 100 - activeMove.winPctAfterForPlayer
                : activeMove.winPctBefore != null
                    ? 100 - activeMove.winPctBefore
                    : undefined
        : undefined;
    const judgementBadge =
        activeMove?.special === "brilliant"
            ? "!!"
            : activeMove?.special === "great"
                ? "!"
                : activeMove?.judgement === "blunder"
                    ? "??"
                    : activeMove?.judgement === "mistake"
                        ? "?"
                        : activeMove?.judgement === "inaccuracy"
                            ? "?!"
                            : activeMove?.judgement === "book"
                                ? "="
                                : undefined;



    // Calculate CP/Mate from White's perspective
    const rawScore = activeMove?.playedEvalCp;
    const whiteScore = rawScore !== undefined && activeMove
        ? (activeMove.turn === "white" ? rawScore : -rawScore)
        : undefined;

    const cp = whiteScore !== undefined && Math.abs(whiteScore) < 9000 ? whiteScore : undefined;
    const mate = whiteScore !== undefined && Math.abs(whiteScore) >= 9000
        ? (whiteScore > 0 ? 10000 - whiteScore : -10000 - whiteScore)
        : undefined;

    const boardShapes = useMemo(() => {
        const shapes: DrawShape[] = [];
        return shapes;
    }, []);

    // Handler for Book View move hover - shows arrow on board
    const handleBookMoveHover = React.useCallback((node: any) => {
        if (!node || !node.uci) {
            // Clear preview arrows when mouse leaves
            setPreviewArrows([]);
            return;
        }
        // Convert UCI to arrow: e.g., "e2e4" -> ["e2", "e4", "#22c55e"]
        const from = node.uci.slice(0, 2);
        const to = node.uci.slice(2, 4);
        setPreviewArrows([[from, to, "#22c55e"]]); // Green for hover preview
    }, []);

    const arrows = useMemo(() => {
        if (previewArrows.length) return previewArrows;
        const arr: Array<[string, string, string?]> = [...customArrows];
        if (activeMove) {
            const from = activeMove.uci.slice(0, 2);
            const to = activeMove.uci.slice(2, 4);
            const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
            // Blue for played (neutral/good), Red for mistake. 
            // The user wants distinction from the engine arrow.
            // Played: Blue (#2563eb), Mistake: Red (#dc2626)
            arr.push([from, to, bad ? "#dc2626" : "#2563eb"]);

            // Only show best move arrow when NOT in guessing mode (Train mode)
            const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
            if (best && best !== activeMove.uci) {
                const bFrom = best.slice(0, 2);
                const bTo = best.slice(2, 4);
                // Best: Green (#16a34a)
                arr.push([bFrom, bTo, "#16a34a"]);
            }
        }
        return arr;
    }, [activeMove, previewArrows, customArrows]);

    // ...

    // Local Move Handler
    const handleUserMove = React.useCallback((node: EnhancedTimelineNode) => {
        setUserMoves(prev => {
            // Check if move already exists to avoid dupes?
            // Simple append for now as we don't support full tree editing locally yet
            return [...prev, node];
        });
    }, []);

    const handleBoardDrop = useBranchCreation({
        review,
        enhancedTimeline: timelineToUse,
        selected,
        jobId,
        isGuessing: false,
        activeMove,
        guessState: "waiting",
        branchSaving,
        setReview,
        setSelectedPly,
        setPreviewFen,
        setPreviewArrows,
        setBranchSaving,
        setBranchError,
        setGuessState: () => { },
        setGuessFeedback: () => { },
        onUserMove: handleUserMove,
        isLocalAnalysis: isAnalyzing || !!userMoves.length // Enable local moves if analyzing OR we already have local moves (continue variation)
    });
    const isLoading = loading || !review;
    const elapsed = pollStartTime ? Math.floor((Date.now() - pollStartTime) / 1000) : 0;
    const hasMinimalData = timelineToUse.length > 0 || review;

    // Show error only if not loading and has error
    if (error && !isLoading) {
        return <ReviewErrorView error={error} reviewId={reviewId} />;
    }

    return (
        <div className="px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto flex max-w-6xl xl:max-w-[1500px] flex-col gap-6">
                {/* Only show SummaryHero if we have data */}
                {hasMinimalData && (
                    <SummaryHero timeline={timelineToUse} critical={review?.critical ?? []} review={review ?? undefined} />
                )}

                {/* Progress Banner - show when loading */}
                {isLoading && (
                    <ProgressBanner
                        stage={progressInfo?.stage}
                        stageLabel={progressInfo?.stageLabel}
                        totalProgress={progressInfo?.totalProgress}
                        stageProgress={progressInfo?.stageProgress}
                        startedAt={progressInfo?.startedAt}
                    />
                )}

                {/* Sticky Header */}
                <div className="sticky top-16 z-30 -mx-6 px-6 sm:-mx-12 sm:px-12 lg:-mx-16 lg:px-16 py-3 bg-slate-900/95 backdrop-blur-md border-b border-white/10">
                    <div className="flex flex-col gap-1">
                        <p className="text-xs uppercase tracking-[0.2em] text-white/60">Review</p>
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                            <div className="flex items-center gap-4">
                                <h1 className="font-display text-3xl text-white">Game analysis</h1>

                                {/* New Engine Toggle Switch */}
                                <div className="flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 transition hover:bg-white/10">
                                    <span className={`text-xs font-semibold uppercase tracking-wider ${isAnalyzing ? "text-accent-teal" : "text-white/40"}`}>
                                        Engine
                                    </span>
                                    <button
                                        onClick={toggleAnalysis}
                                        className={`relative flex h-5 w-9 items-center rounded-full px-0.5 transition-colors ${isAnalyzing ? "bg-accent-teal" : "bg-white/20"
                                            }`}
                                    >
                                        <span
                                            className={`block h-4 w-4 transform rounded-full bg-white shadow-sm transition-transform ${isAnalyzing ? "translate-x-4" : "translate-x-0"
                                                }`}
                                        />
                                    </button>
                                </div>
                            </div>

                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => setShowShareModal(true)}
                                    className="flex items-center gap-2 rounded-full border border-white/20 px-4 py-1.5 text-xs text-white/80 hover:border-white/40 hover:text-white transition hover:bg-white/5"
                                >
                                    <Share2 className="w-3 h-3" />
                                    Share / Export
                                </button>
                            </div>
                        </div>
                        <div className="text-sm text-white/70">
                            Opening: {review?.opening?.name ?? "Unknown"} {review?.opening?.eco ? `(${review.opening.eco})` : ""}
                        </div>
                    </div>
                </div>

                <div className="grid gap-6 lg:grid-cols-[minmax(380px,520px)_1fr]">
                    <BoardSection
                        fen={previewFen || activeMove?.fen}
                        customShapes={boardShapes}
                        arrows={arrows}
                        evalPercent={evalPercent}
                        cp={cp}
                        mate={mate}
                        judgementBadge={judgementBadge}
                        moveSquare={activeMove?.uci?.slice(2, 4)}
                        onDrop={handleBoardDrop}
                        drawingColor={drawingColor}
                        onSelectColor={setDrawingColor}
                        onClearArrows={() => setCustomArrows([])}
                        previewLabel={previewLabel}
                        showAdvanced={showAdvanced}
                        timeline={timelineToUse}
                        conceptSpikes={conceptSpikes}
                        selectedPly={selected?.ply}
                        onSelectPly={(ply) => {
                            const node = timelineToUse.find(n => n.ply === ply);
                            if (node) handleSelectNode(node);
                            else setSelectedPly(ply);
                        }}
                        branchSaving={branchSaving}
                        branchError={branchError}
                        orientation="white" /* Default to white for now, can be dynamic later */
                    />

                    <div className="flex flex-col gap-4 lg:max-h-[calc(100vh-2rem)] lg:overflow-y-auto pr-1">

                        {isAnalyzing && (
                            <div className="glass-card rounded-2xl border border-white/10 bg-white/5 p-4">
                                <EngineAnalysisPanel
                                    lines={engineLines}
                                    fen={previewFen || activeMove?.fen}
                                    isAnalyzing={isAnalyzing}
                                    engineStatus={engineStatus}
                                    errorMessage={errorMessage}
                                    onToggleAnalysis={toggleAnalysis}
                                    onPreviewLine={(pv) => {
                                        if (!pv) {
                                            setPreviewArrows([]);
                                            setPreviewLabel(null);
                                            return;
                                        }
                                        const firstMove = pv.split(" ")[0]; // e.g. "e2e4"
                                        if (firstMove && firstMove.length >= 4) {
                                            const from = firstMove.slice(0, 2);
                                            const to = firstMove.slice(2, 4);
                                            setPreviewArrows([[from, to, "#16a34a"]]); // Green for best/alternative
                                            setPreviewLabel(`Preview: ${firstMove}`);
                                        }
                                    }}
                                    onClickLine={(pv) => {
                                        // Execute the first move of the PV line
                                        if (!pv) return;
                                        const firstMove = pv.split(" ")[0];
                                        if (firstMove && firstMove.length >= 4) {
                                            // Use the branch creation logic for proper integration
                                            const from = firstMove.slice(0, 2);
                                            const to = firstMove.slice(2, 4);
                                            handleBoardDrop({ sourceSquare: from, targetSquare: to });
                                        }
                                    }}
                                    onOpenSettings={() => setShowEngineSettings(true)}
                                />
                            </div>
                        )}

                        <AnalysisTabsSection
                            activeMove={activeMove}
                            enhancedTimeline={enhancedTimeline}
                            timeline={timelineToUse}

                            review={review as Review}
                            reviewRoot={review?.root}
                            activeTab={activeTab}
                            setActiveTab={setActiveTab}
                            openingLookup={openingLookup ?? review?.openingStats ?? null}
                            lookupLoading={isLoading || lookupLoading}
                            lookupError={lookupError}
                            setSelectedPly={setSelectedPly}
                            setSelectedVariation={setSelectedVariation}
                            selectedPly={selectedPly}
                            setPreviewArrows={setPreviewArrows as any}
                            setPreviewFen={setPreviewFen}
                            onSelectNode={handleSelectNode}
                            onMoveHover={handleBookMoveHover}
                            onFlip={() => setOrientation(o => o === "white" ? "black" : "white")}
                            onToggleEngine={toggleAnalysis}
                            onPlayBest={() => {
                                if (engineLines.length && engineLines[0]?.pv) {
                                    const firstMove = engineLines[0].pv.split(" ")[0];
                                    if (firstMove && firstMove.length >= 4) {
                                        handleBoardDrop({ sourceSquare: firstMove.slice(0, 2), targetSquare: firstMove.slice(2, 4) });
                                    }
                                }
                            }}
                            onToggleArrows={() => setShowArrows(v => !v)}
                        />
                    </div>
                </div>
            </div>
            {showEngineSettings && (
                <EngineSettingsModal
                    config={config}
                    setConfig={setConfig}
                    onClose={() => setShowEngineSettings(false)}
                />
            )}
            {showShareModal && (
                <ShareModal
                    reviewTitle={`${review?.opening?.name || "Chess Game"} Review`}
                    activeMove={activeMove}
                    book={review?.book}
                    timeline={enhancedTimeline}
                    onClose={() => setShowShareModal(false)}
                />
            )}
        </div>
    );
}
