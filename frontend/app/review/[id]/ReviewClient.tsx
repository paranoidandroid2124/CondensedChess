"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Chess } from "chess.js";
import { fetchOpeningLookup } from "../../../lib/review";
import type { OpeningStats, Review, TimelineNode } from "../../../types/review";
import { SummaryHero } from "../../../components/review/SummaryHero";
import { ReviewLoadingView } from "../../../components/review/ReviewLoadingView";
import { ReviewPendingView } from "../../../components/review/ReviewPendingView";
import { ReviewErrorView } from "../../../components/review/ReviewErrorView";
import { BoardSection } from "../../../components/review/BoardSection";
import { MovesSection } from "../../../components/review/MovesSection";
import { AnalysisTabsSection } from "../../../components/review/AnalysisTabsSection";
import { TimelineSection } from "../../../components/review/TimelineSection";
import { BlunderTimeline } from "../../../components/review/BlunderTimeline";
import { uciToSan } from "../../../lib/chess-utils";
import { formatDelta, formatSanHuman } from "../../../lib/review-format";
import type { VariationEntry } from "../../../components/review/TimelineView";
import { useInstantTimeline } from "../../../hooks/useInstantTimeline";
import { useReviewPolling } from "../../../hooks/useReviewPolling";
import { useEngineAnalysis } from "../../../hooks/useEngineAnalysis";
import { useBranchCreation } from "../../../hooks/useBranchCreation";
import { useGuessMode } from "../../../hooks/useGuessMode";
import {
    buildConceptSpikes,
    buildEnhancedTimeline,
    buildFenBeforeMap,
    buildMoveRows,
    buildVariationMap,
    findSelected,
    type EnhancedTimelineNode
} from "../../../lib/review-derived";



export default function ReviewClient({ reviewId }: { reviewId: string }) {
    const { review, loading, pendingMessage, pollStartTime, pollAttempt, error, setReview, progressInfo } = useReviewPolling(reviewId);
    const [selectedPly, setSelectedPly] = useState<number | null>(null);
    const [selectedVariation, setSelectedVariation] = useState<VariationEntry | null>(null);
    const [showAdvanced] = useState<boolean>(true);
    const [openingLookup, setOpeningLookup] = useState<OpeningStats | null>(null);
    const [lookupKey, setLookupKey] = useState<string>("");
    const [lookupError, setLookupError] = useState<string | null>(null);
    const [lookupLoading, setLookupLoading] = useState<boolean>(false);
    const [branchSaving, setBranchSaving] = useState<boolean>(false);
    const [branchError, setBranchError] = useState<string | null>(null);
    const [showStudy] = useState<boolean>(true);
    const [previewFen, setPreviewFen] = useState<string | null>(null);
    const [previewArrows, setPreviewArrows] = useState<Array<[string, string, string?]>>([]);
    const [previewLabel, setPreviewLabel] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<"engine" | "opening" | "study" | "concepts">("concepts");
    const [drawingColor, setDrawingColor] = useState<"green" | "red" | "blue" | "orange">("green");
    const jobId = review?.jobId ?? reviewId;
    const [instantPgn, setInstantPgn] = useState<string | null>(null);
    const instantTimeline = useInstantTimeline(instantPgn);

    // Engine & Interactive State
    const { isAnalyzing, engineLines, toggleAnalysis } = useEngineAnalysis(review, previewFen, selectedPly);
    const [customArrows, setCustomArrows] = useState<Array<[string, string, string?]>>([]); // TODO: Implement drawing

    // Study Mode State
    const { isGuessing, guessState, guessFeedback, setIsGuessing, setGuessState, setGuessFeedback } = useGuessMode();

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

    const clearPreview = useCallback(() => {
        setPreviewFen(null);
        setPreviewArrows([]);
        setPreviewLabel(null);
    }, []);

    const handlePreviewLine = useCallback(
        (fenBefore?: string, pv?: string[], label?: string) => {
            if (!fenBefore || !pv?.length) return;
            try {
                const chess = new Chess(fenBefore);
                const arrows: Array<[string, string, string?]> = [];
                pv.slice(0, 8).forEach((mv) => {
                    try {
                        const move = (chess as any).move(mv, { sloppy: true });
                        if (move?.from && move?.to) arrows.push([move.from, move.to, "#10b981"]);
                    } catch {
                        // ignore bad moves in PV
                    }
                });
                setPreviewFen(chess.fen());
                setPreviewArrows(arrows);
                setPreviewLabel(label ?? "Preview line");
            } catch {
                // ignore
            }
        },
        []
    );

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

    const moveRows = useMemo(() => buildMoveRows(enhancedTimeline), [enhancedTimeline]);

    const conceptSpikes = useMemo(() => buildConceptSpikes(enhancedTimeline), [enhancedTimeline]);

    const fenBeforeByPly = useMemo(() => buildFenBeforeMap(enhancedTimeline), [enhancedTimeline]);

    const selected = useMemo(() => findSelected(enhancedTimeline, selectedPly), [selectedPly, enhancedTimeline]);

    const activeMove = useMemo<EnhancedTimelineNode | null>(() => {
        if (selectedVariation) {
            const parent = enhancedTimeline.find((t) => t.ply === selectedVariation.parentPly);
            if (parent) {
                return {
                    ...parent,
                    uci: selectedVariation.node.uci,
                    san: selectedVariation.node.san,
                    fen: selectedVariation.node.fen,
                    fenBefore: selectedVariation.parentFenBefore ?? parent.fenBefore,
                    judgement: selectedVariation.node.judgement as any,
                    winPctAfterForPlayer: selectedVariation.node.eval,
                    label: selectedVariation.parentLabel ?? parent.label,
                    isCustom: true
                };
            }
        }
        return selected;
    }, [selectedVariation, enhancedTimeline, selected]);

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

    const variationMap = useMemo(() => buildVariationMap(review, enhancedTimeline), [review, enhancedTimeline]);

    const boardSquareStyles = useMemo(() => {
        const styles: Record<string, React.CSSProperties> = {};
        const highlight = (uci: string, color: "red" | "green" | "purple") => {
            if (!uci || uci.length < 4) return;
            const from = uci.slice(0, 2);
            const to = uci.slice(2, 4);
            styles[from] = { ...styles[from], animation: color === "red" ? "pulse-red 1.2s ease-in-out infinite" : color === "green" ? "pulse-green 1.2s ease-in-out infinite" : "pulse-purple 1.2s ease-in-out infinite" };
            styles[to] = { ...styles[to], animation: color === "red" ? "pulse-red 1.2s ease-in-out infinite" : color === "green" ? "pulse-green 1.2s ease-in-out infinite" : "pulse-purple 1.2s ease-in-out infinite" };
        };
        if (previewFen) return styles;
        if (activeMove) {
            const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
            highlight(activeMove.uci, bad ? "red" : "purple");
            const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
            if (best && best !== activeMove.uci) {
                highlight(best, "green");
            }
        }
        return styles;
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
            if (best && best !== activeMove.uci && !isGuessing) {
                const bFrom = best.slice(0, 2);
                const bTo = best.slice(2, 4);
                arr.push([bFrom, bTo, "#4ade80"]);
            }
        }
        return arr;
    }, [activeMove, previewArrows, customArrows, isGuessing]);

    const handleBoardDrop = useBranchCreation({
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
    });

    useEffect(() => {
        // selecting another ply exits preview
        clearPreview();
    }, [selectedPly, clearPreview]);

    if (loading) {
        const elapsed = pollStartTime ? Math.floor((Date.now() - pollStartTime) / 1000) : 0;
        const progress = progressInfo?.totalProgress !== undefined ? progressInfo.totalProgress * 100 : Math.min(95, (pollAttempt / 20) * 100);
        return (
            <ReviewLoadingView
                loading={loading}
                pendingMessage={pendingMessage}
                elapsed={elapsed}
                progress={progress}
                progressInfo={progressInfo}
                instantTimeline={instantTimeline}
                selectedPly={selectedPly}
                setSelectedPly={setSelectedPly}
            />
        );
    }

    // Handle Timeout/Pending state (when loading is false but review is not ready)
    if (!review && pendingMessage) {
        return <ReviewPendingView reviewId={reviewId} pendingMessage={pendingMessage} />;
    }

    if (error || !review) {
        return <ReviewErrorView error={error} reviewId={reviewId} />;
    }

    return (
        <div className="px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto flex max-w-6xl flex-col gap-6">
                <SummaryHero timeline={enhancedTimeline} critical={review.critical ?? []} review={review} />
                <div className="flex flex-col gap-2">
                    <p className="text-xs uppercase tracking-[0.2em] text-white/60">Review</p>
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <h1 className="font-display text-3xl text-white">Game analysis</h1>
                        <div className="flex flex-wrap gap-2 text-xs">
                            <span className="rounded-full bg-white/10 px-3 py-1">PGN timeline</span>
                            <span className="rounded-full bg-white/10 px-3 py-1">Stockfish shallow/deep</span>
                            <span className="rounded-full bg-white/10 px-3 py-1">Concept scores</span>
                            {review.studyChapters && review.studyChapters.length ? (
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
                        {previewFen ? (
                            <button
                                onClick={clearPreview}
                                className="rounded-full border border-amber-400/60 px-3 py-1 text-amber-100 hover:border-amber-300/80"
                            >
                                Exit preview
                            </button>
                        ) : null}
                    </div>
                    <div className="text-sm text-white/70">
                        Opening: {review.opening?.name ?? "Unknown"} {review.opening?.eco ? `(${review.opening.eco})` : ""}
                    </div>
                </div>

                <div className="grid gap-5 lg:grid-cols-[auto_300px_1fr] xl:grid-cols-[auto_360px_1fr]">
                    <BoardSection
                        fen={previewFen || activeMove?.fen}
                        squareStyles={boardSquareStyles}
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
                        timeline={enhancedTimeline}
                        conceptSpikes={conceptSpikes}
                        selectedPly={selected?.ply}
                        onSelectPly={setSelectedPly}
                        branchSaving={branchSaving}
                        branchError={branchError}
                    />

                    <MovesSection
                        timeline={enhancedTimeline}
                        selectedPly={selectedPly}
                        onSelectPly={setSelectedPly}
                    />

                    <AnalysisTabsSection
                        activeMove={activeMove}
                        enhancedTimeline={enhancedTimeline}
                        review={review}
                        activeTab={activeTab}
                        setActiveTab={setActiveTab}
                        engineLines={engineLines}
                        isAnalyzing={isAnalyzing}
                        toggleAnalysis={toggleAnalysis}
                        openingLookup={openingLookup}
                        lookupLoading={lookupLoading}
                        lookupError={lookupError}
                        setSelectedPly={setSelectedPly}
                        setSelectedVariation={setSelectedVariation}
                        isGuessing={isGuessing}
                        setIsGuessing={setIsGuessing}
                        guessState={guessState}
                        setGuessState={setGuessState}
                        guessFeedback={guessFeedback}
                        setGuessFeedback={setGuessFeedback}
                        setPreviewFen={setPreviewFen}
                        setPreviewArrows={setPreviewArrows}
                        setPreviewLabel={setPreviewLabel}
                    />
                </div>

                <TimelineSection
                    moveRows={moveRows}
                    selected={selected?.ply}
                    onSelect={setSelectedPly}
                    variations={variationMap}
                    showAdvanced={showAdvanced}
                    onSelectVariation={setSelectedVariation}
                    onPreviewLine={handlePreviewLine}
                    reviewRoot={review.root}
                    openingLookup={openingLookup}
                    lookupLoading={lookupLoading}
                    lookupError={lookupError}
                />

                <BlunderTimeline timeline={enhancedTimeline} selected={selected?.ply} onSelect={setSelectedPly} />
            </div>
        </div>
    );
}
