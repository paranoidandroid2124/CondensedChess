import React, { useState } from "react";
import { Share2, FileText, Smartphone, Download, Check } from "lucide-react";
import { BoardSection } from "./BoardSection";
import { EnhancedTimelineNode } from "../../lib/review-derived";

type ShareModalProps = {
    activeMove: EnhancedTimelineNode | null;
    reviewTitle?: string;
    onClose: () => void;
};

export function ShareModal({ activeMove, reviewTitle, onClose }: ShareModalProps) {
    const [subTab, setSubTab] = useState<"social" | "pdf">("social");
    const [cardFormat, setCardFormat] = useState<"square" | "story">("square"); // 1:1 vs 9:16
    const [isDownloading, setIsDownloading] = useState(false);
    const [downloadDone, setDownloadDone] = useState(false);

    // Mock Download
    const handleDownload = () => {
        setIsDownloading(true);
        setTimeout(() => {
            setIsDownloading(false);
            setDownloadDone(true);
            setTimeout(() => setDownloadDone(false), 2000);
        }, 1500);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm" onClick={onClose}>
            <div className="w-[90vw] max-w-[800px] max-h-[90vh] overflow-y-auto rounded-3xl border border-white/10 bg-[#0f1115] shadow-2xl flex flex-col md:flex-row" onClick={e => e.stopPropagation()}>

                {/* Left: Configuration Panel */}
                <div className="w-full md:w-[320px] border-r border-white/5 p-6 flex flex-col gap-6 bg-white/[0.02]">
                    <div className="flex items-center justify-between">
                        <h2 className="text-xl font-display font-bold text-white flex items-center gap-2">
                            <Share2 className="w-5 h-5 text-accent-teal" />
                            Share
                        </h2>
                        <button onClick={onClose} className="text-white/40 hover:text-white transition">✕</button>
                    </div>

                    {/* Format Tabs */}
                    <div className="flex p-1 rounded-xl bg-black/40 border border-white/5">
                        <button
                            onClick={() => setSubTab("social")}
                            className={`flex-1 py-2 text-sm font-semibold rounded-lg transition ${subTab === "social" ? "bg-white/10 text-white shadow-sm" : "text-white/40 hover:text-white/70"}`}
                        >
                            Social Card
                        </button>
                        <button
                            onClick={() => setSubTab("pdf")}
                            className={`flex-1 py-2 text-sm font-semibold rounded-lg transition ${subTab === "pdf" ? "bg-white/10 text-white shadow-sm" : "text-white/40 hover:text-white/70"}`}
                        >
                            PDF Report
                        </button>
                    </div>

                    {/* Context Specific Controls */}
                    {subTab === "social" && (
                        <div className="space-y-4">
                            <div className="text-xs font-semibold text-white/40 uppercase tracking-widest">Format</div>
                            <div className="grid grid-cols-2 gap-3">
                                <button
                                    onClick={() => setCardFormat("square")}
                                    className={`p-3 rounded-xl border flex flex-col items-center gap-2 transition ${cardFormat === "square" ? "border-accent-teal bg-accent-teal/10 text-white" : "border-white/10 hover:border-white/20 text-white/50"}`}
                                >
                                    <div className="w-6 h-6 border-2 border-current rounded-sm" />
                                    <span className="text-xs">Post (1:1)</span>
                                </button>
                                <button
                                    onClick={() => setCardFormat("story")}
                                    className={`p-3 rounded-xl border flex flex-col items-center gap-2 transition ${cardFormat === "story" ? "border-accent-teal bg-accent-teal/10 text-white" : "border-white/10 hover:border-white/20 text-white/50"}`}
                                >
                                    <div className="w-4 h-7 border-2 border-current rounded-sm" />
                                    <span className="text-xs">Story (9:16)</span>
                                </button>
                            </div>

                            <p className="text-xs text-white/40 leading-relaxed">
                                Share this critical moment with your followers. The card includes the position, your analysis snippet, and game info.
                            </p>
                        </div>
                    )}

                    {subTab === "pdf" && (
                        <div className="space-y-4">
                            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-200 text-sm">
                                <FileText className="w-5 h-5 mb-2" />
                                Generates a full PDF report of the entire game analysis, including all chapters and key moments.
                            </div>
                        </div>
                    )}

                    <div className="mt-auto pt-6 border-t border-white/5">
                        <button
                            onClick={handleDownload}
                            disabled={isDownloading || downloadDone}
                            className="w-full py-3 rounded-full bg-accent-teal text-black font-bold text-sm flex items-center justify-center gap-2 hover:bg-accent-teal/90 transition disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isDownloading ? (
                                <>Downloading...</>
                            ) : downloadDone ? (
                                <><Check className="w-4 h-4" /> Saved!</>
                            ) : (
                                <><Download className="w-4 h-4" /> Download {subTab === "social" ? "Image" : "PDF"}</>
                            )}
                        </button>
                    </div>
                </div>

                {/* Right: Preview Area */}
                <div className="flex-1 p-8 bg-gradient-to-br from-[#1a1a1a] to-[#0f1115] flex items-center justify-center relative overflow-hidden">
                    <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(45,212,191,0.05),transparent_50%)]" />

                    {/* SOCIAL CAROUSEL PREVIEW */}
                    {subTab === "social" && (
                        <div className="flex flex-col items-center gap-6 w-full h-full justify-center">

                            {/* Carousel Container */}
                            <div className="flex gap-6 overflow-x-auto w-full px-8 py-4 snap-x snap-mandatory no-scrollbar items-center h-[580px]">
                                {/* Slide 1: Cover */}
                                <div className={`shrink-0 snap-center relative bg-slate-900 border border-white/10 shadow-2xl overflow-hidden flex flex-col transition-all duration-300 ${cardFormat === "square" ? "w-[400px] h-[400px]" : "w-[320px] h-[568px]"}`}>
                                    <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-slate-800 to-black" />
                                    <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(45,212,191,0.1),transparent_50%)]" />

                                    <div className="relative z-10 flex flex-col items-center justify-center h-full text-center p-8 space-y-6">
                                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-accent-teal/10 border border-accent-teal/20 text-accent-teal text-xs font-bold uppercase tracking-widest">
                                            Game Analysis
                                        </div>
                                        <h1 className="text-3xl font-display font-bold text-white leading-tight">
                                            {reviewTitle?.split(" vs ")[0] ?? "White"} <span className="text-white/30 font-light">vs</span><br />
                                            {reviewTitle?.split(" vs ")[1] ?? "Black"}
                                        </h1>
                                        <div className="w-16 h-1 bg-white/20 rounded-full" />
                                        <p className="text-white/60 font-serif italic text-sm">
                                            &quot;A tactical masterpiece featuring a brilliant exchange sacrifice.&quot;
                                        </p>
                                    </div>
                                    <div className="absolute bottom-6 w-full text-center text-[10px] text-white/20 font-mono uppercase tracking-[0.2em]">
                                        Swipe to see moments →
                                    </div>
                                </div>

                                {/* Slide 2: Active Moment (Dynamic) */}
                                <div className={`shrink-0 snap-center relative bg-slate-900 border border-white/10 shadow-2xl overflow-hidden flex flex-col transition-all duration-300 ${cardFormat === "square" ? "w-[400px] h-[400px]" : "w-[320px] h-[568px]"}`}>
                                    {/* Watermark */}
                                    <div className="absolute top-4 left-4 z-10 flex items-center gap-1.5 opacity-80">
                                        <span className="w-3 h-3 bg-accent-teal rounded-full" />
                                        <span className="text-[10px] font-bold text-white tracking-widest uppercase">Chesstory</span>
                                    </div>

                                    {/* 1:1 / 9:16 Layout: Top Board, Bottom Text */}
                                    <div className="flex-1 flex flex-col">
                                        {/* Board Area (Top ~60%) */}
                                        <div className="relative flex-1 bg-black/20 p-6 flex items-center justify-center">
                                            <div className="relative w-full aspect-square rounded shadow-2xl pointer-events-none">
                                                {activeMove?.fen && (
                                                    <BoardSection
                                                        fen={activeMove.fen}
                                                        orientation="white"
                                                        showAdvanced={false}
                                                        onDrop={() => true} onSelectPly={() => { }} onClearArrows={() => { }}
                                                        arrows={[]} customShapes={[]}
                                                        drawingColor="green"
                                                        onSelectColor={() => { }}
                                                        timeline={[]}
                                                        conceptSpikes={[]}
                                                        previewLabel={null}
                                                        branchSaving={false}
                                                        branchError={null}
                                                    />
                                                )}
                                            </div>
                                        </div>

                                        {/* Text Area (Bottom) */}
                                        <div className="relative z-10 bg-slate-950/50 backdrop-blur-md border-t border-white/10 p-6 space-y-3">
                                            <div className="flex items-center justify-between">
                                                <div className="text-accent-teal text-xs font-bold uppercase tracking-wider">
                                                    {activeMove?.judgement || "Key Moment"}
                                                </div>
                                                <span className="text-white/30 text-xs font-mono">Move {Math.ceil((activeMove?.ply || 0) / 2)}</span>
                                            </div>
                                            <p className="text-white text-sm font-serif leading-relaxed line-clamp-3">
                                                {activeMove?.shortComment || "White seizes the initiative with a bold pawn sacrifice, opening lines against the black king."}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {/* Slide 3: Summary/Stats mockup */}
                                <div className={`shrink-0 snap-center relative bg-slate-900 border border-white/10 shadow-2xl overflow-hidden flex flex-col items-center justify-center transition-all duration-300 ${cardFormat === "square" ? "w-[400px] h-[400px]" : "w-[320px] h-[568px]"}`}>
                                    <div className="text-center space-y-4 p-8">
                                        <div className="text-6xl font-bold text-white">94<span className="text-2xl text-accent-teal">%</span></div>
                                        <div className="text-white/40 text-xs font-bold uppercase tracking-widest">Accuracy</div>
                                        <div className="w-full h-px bg-white/10 my-4" />
                                        <div className="grid grid-cols-2 gap-4 text-sm">
                                            <div className="text-emerald-400">2 Brilliant</div>
                                            <div className="text-blue-400">5 Best</div>
                                            <div className="text-amber-400">1 Mistake</div>
                                            <div className="text-rose-400">0 Blunder</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <p className="text-xs text-white/30 animate-pulse">
                                ← Scroll to preview generated slides →
                            </p>
                        </div>
                    )}

                    {/* PDF PREVIEW */}
                    {subTab === "pdf" && (
                        <div className="w-[400px] h-[560px] bg-white rounded-sm shadow-2xl border border-white/10 relative flex flex-col items-center p-8">
                            {/* Paper texture/look */}
                            <div className="w-full border-b-2 border-black/10 pb-4 mb-6 text-center">
                                <h1 className="text-2xl font-serif font-bold text-slate-900">Analysis Report</h1>
                                <p className="text-xs text-slate-500 mt-2 uppercase tracking-widest">{reviewTitle || "Chess Game Review"}</p>
                            </div>

                            <div className="w-full space-y-4 opacity-50 blur-[1px]">
                                <div className="h-4 bg-slate-200 rounded w-3/4" />
                                <div className="h-4 bg-slate-200 rounded w-full" />
                                <div className="h-4 bg-slate-200 rounded w-5/6" />
                                <div className="h-32 bg-slate-100 rounded border border-slate-200 mt-4" />
                                <div className="h-4 bg-slate-200 rounded w-full mt-4" />
                                <div className="h-4 bg-slate-200 rounded w-4/5" />
                            </div>

                            <div className="absolute inset-0 flex items-center justify-center bg-white/50 backdrop-blur-[1px]">
                                <div className="px-6 py-3 bg-slate-900 text-white rounded-full text-xs font-bold shadow-lg">
                                    Preview Generated on Export
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
