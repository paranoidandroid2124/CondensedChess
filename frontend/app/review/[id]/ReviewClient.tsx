"use client";

import React, { useEffect, useMemo, useState } from "react";
import { fetchOpeningLookup } from "../../../lib/review";
import type { OpeningStats, Review, TimelineNode } from "../../../types/review";
import { DrawShape } from "chessground/draw";
import { Key } from "chessground/types";
import { SummaryHero } from "../../../components/review/SummaryHero";
import { ReviewErrorView } from "../../../components/review/ReviewErrorView";
import { BoardSection } from "../../../components/review/BoardSection";
import { TimelineView } from "../../../components/review/TimelineView";
import { ProgressBanner } from "../../../components/review/ProgressBanner";
import { useInstantTimeline } from "../../../hooks/useInstantTimeline";
import { useReviewPolling } from "../../../hooks/useReviewPolling";
import { useEngineAnalysis } from "../../../hooks/useEngineAnalysis";
import { useBranchCreation } from "../../../hooks/useBranchCreation";

import { buildConceptSpikes, buildEnhancedTimeline, findSelected, type EnhancedTimelineNode } from "../../../lib/review-derived";
import { CommentCard } from "../../../components/review/CommentCard";
import { AnalysisTabsSection } from "../../../components/review/AnalysisTabsSection";
import type { TabId } from "../../../components/AnalysisPanel";
import type { VariationEntry } from "../../../components/review/TimelineView";
import { BestAlternatives } from "../../../components/BestAlternatives";



export default function ReviewClient({ reviewId }: { reviewId: string }) {
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
    const [activeTab, setActiveTab] = useState<TabId>("concepts");
    const [drawingColor, setDrawingColor] = useState<"green" | "red" | "blue" | "orange">("green");
    const [selectedVariation, setSelectedVariation] = useState<VariationEntry | null>(null);
    const jobId = review?.jobId ?? reviewId;
    const [instantPgn, setInstantPgn] = useState<string | null>(null);
    const instantTimeline = useInstantTimeline(instantPgn);

    // Engine & Interactive State
    const { isAnalyzing, engineLines, toggleAnalysis } = useEngineAnalysis(review, previewFen, selectedPly);
    const [customArrows, setCustomArrows] = useState<Array<[string, string, string?]>>([]); // TODO: Implement drawing
    const [userMoves, setUserMoves] = useState<EnhancedTimelineNode[]>([]); // Local moves added by user during analysis

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
        if (review?.timeline?.length) {
            const lastPly = review.timeline.at(-1)?.ply ?? null;
            setSelectedPly(lastPly);
        }
    }, [review]);

    useEffect(() => {
        if (!review && instantTimeline?.length && selectedPly === null) {
            setSelectedPly(instantTimeline[instantTimeline.length - 1]?.ply ?? null);
        }
    }, [instantTimeline, review, selectedPly]);

    const enhancedTimeline = useMemo<EnhancedTimelineNode[]>(() => buildEnhancedTimeline(review), [review]);

    // Use instantTimeline or empty array as fallback for timeline
    const baseTimeline = review ? enhancedTimeline : (instantTimeline || []);
    const timelineToUse = useMemo(() => {
        if (!userMoves.length) return baseTimeline;
        return [...baseTimeline, ...userMoves];
    }, [baseTimeline, userMoves]);



    // Build SAN sequence up to selected ply for opening lookup
    const sanSequence = useMemo(() => {
        if (!enhancedTimeline.length) return [] as string[];
        const sorted = [...enhancedTimeline].sort((a, b) => a.ply - b.ply);
        const cutoff = selectedPly ?? sorted[sorted.length - 1]?.ply ?? 0;
        return sorted
            .filter((t) => t.ply <= cutoff)
            .map((t) => t.san)
            .filter(Boolean);
    }, [enhancedTimeline, selectedPly]);

    const conceptSpikes = useMemo(() => buildConceptSpikes(enhancedTimeline), [enhancedTimeline]);
    const selected = useMemo(() => findSelected(timelineToUse, selectedPly), [selectedPly, timelineToUse]);

    const activeMove = useMemo<EnhancedTimelineNode | null>(() => selected, [selected]);
    const activeCritical = review?.critical.find(c => c.ply === activeMove?.ply);

    useEffect(() => {
        const fetch = async () => {
            if (!selected || !enhancedTimeline.length) return;
            const movesToPly = enhancedTimeline.filter((t) => t.ply <= selected.ply).map((t) => t.san);
            const newKey = movesToPly.join(" ");
            if (newKey === lookupKey) return; // No change
            setLookupKey(newKey);
            setLookupError(null);
            setLookupLoading(true);
            try {
                const stats = await fetchOpeningLookup(movesToPly);
                setOpeningLookup(stats);
            } catch (err) {
                setLookupError(err instanceof Error ? err.message : "Opening lookup failed");
            } finally {
                setLookupLoading(false);
            }
        };
        fetch();
    }, [selected, enhancedTimeline, lookupKey]);

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



    const boardShapes = useMemo(() => {
        const shapes: DrawShape[] = [];
        // CLEANUP: We use Arrows for move indication now. 
        // Removing the square highlights to reduce visual clutter as per user feedback.
        /*
        const highlight = (uci: string, brush: "red" | "green" | "paleBlue") => { 
            if (!uci || uci.length < 4) return;
            const from = uci.slice(0, 2) as Key;
            const to = uci.slice(2, 4) as Key;
            shapes.push({ orig: from, brush });
            shapes.push({ orig: to, brush });
        };

        if (previewFen) return shapes;
        if (activeMove) {
            const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
            highlight(activeMove.uci, bad ? "red" : "paleBlue");
            const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
            if (best && best !== activeMove.uci) {
                highlight(best, "green");
            }
        }
        */
        return shapes;
    }, [activeMove, previewFen]);

    const arrows = useMemo(() => {
        if (previewArrows.length) return previewArrows;
        const arr: Array<[string, string, string?]> = [...customArrows];
        if (activeMove) {
            const from = activeMove.uci.slice(0, 2);
            const to = activeMove.uci.slice(2, 4);
            const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
            arr.push([from, to, bad ? "#f87171" : "#818cf8"]);
            // Only show best move arrow when NOT in guessing mode (Train mode)
            const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
            if (best && best !== activeMove.uci) {
                const bFrom = best.slice(0, 2);
                const bTo = best.slice(2, 4);
                arr.push([bFrom, bTo, "#4ade80"]);
            }
        }
        return arr;
    }, [activeMove, previewArrows, customArrows]);



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
        onUserMove: (node) => setUserMoves(prev => [...prev, node])
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

                <div className="flex flex-col gap-2">
                    <p className="text-xs uppercase tracking-[0.2em] text-white/60">Review</p>
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <h1 className="font-display text-3xl text-white">Game analysis</h1>
                        <div className="flex flex-wrap gap-2 text-xs">
                            <span className="rounded-full bg-white/10 px-3 py-1">PGN timeline</span>
                            <span className="rounded-full bg-white/10 px-3 py-1">Stockfish shallow/deep</span>
                            <span className="rounded-full bg-white/10 px-3 py-1">Concept scores</span>
                            {review?.studyChapters && review.studyChapters.length ? (
                                <span className="rounded-full bg-accent-teal/15 px-3 py-1 text-accent-teal/80">Study chapters</span>
                            ) : null}
                        </div>
                    </div>
                    <div className="flex flex-wrap gap-2 text-xs">
                        <button
                            onClick={toggleAnalysis}
                            className={`rounded-full border px-3 py-1 transition ${isAnalyzing
                                ? "border-accent-teal bg-accent-teal/10 text-accent-teal"
                                : "border-white/20 text-white/60 hover:border-white/40 hover:text-white"
                                }`}
                        >
                            {isAnalyzing ? "Stop Analysis" : "Analyze"}
                        </button>
                        <button
                            onClick={handleSaveGame}
                            className="rounded-full border border-white/20 px-3 py-1 text-white/60 hover:border-white/40 hover:text-white"
                        >
                            Save
                        </button>
                    </div>
                    <div className="text-sm text-white/70">
                        Opening: {review?.opening?.name ?? "Unknown"} {review?.opening?.eco ? `(${review.opening.eco})` : ""}
                    </div>
                </div>

                <div className="grid gap-6 lg:grid-cols-[minmax(380px,520px)_1fr]">
                    <BoardSection
                        fen={previewFen || activeMove?.fen}
                        customShapes={boardShapes}
                        arrows={arrows}
                        evalPercent={activeMove?.winPctAfterForPlayer}
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
                        onSelectPly={setSelectedPly}
                        branchSaving={branchSaving}
                        branchError={branchError}
                    />

                    <div className="flex flex-col gap-4 lg:max-h-[calc(100vh-2rem)] lg:overflow-y-auto pr-1">
                        <CommentCard move={activeMove} critical={activeCritical} />
                        <div className="glass-card rounded-2xl border border-white/10 bg-white/5 p-4">
                            <BestAlternatives
                                lines={engineLines}
                                isAnalyzing={isAnalyzing}
                                onToggleAnalysis={toggleAnalysis}
                                onPreviewLine={() => {
                                    setPreviewFen(null);
                                    setPreviewArrows([]);
                                    setPreviewLabel("Engine line");
                                }}
                            />
                        </div>

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
                            tabOrder={["concepts", "opening", "moves", "tree", "study"]}
                            setPreviewArrows={setPreviewArrows as any}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
}
