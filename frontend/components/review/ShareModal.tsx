import { Dialog, Transition } from "@headlessui/react";
import { Fragment, useState, useRef, useCallback } from "react";
import { Share2, Download, MousePointerClick, Smartphone, Layout, FileText, Loader2 } from "lucide-react";
import { BoardSection } from "./BoardSection";
import type { EnhancedTimelineNode } from "../../lib/review-derived";
import type { Book } from "../../types/review";
import { toPng } from 'html-to-image';
import { jsPDF } from "jspdf";
import React from "react";

interface ShareModalProps {
    activeMove: EnhancedTimelineNode | null;
    reviewTitle: string;
    book?: Book;
    timeline: EnhancedTimelineNode[];
    onClose: () => void;
}

// Internal Slide Component for reuse in Preview and Export
const SlideCard = React.forwardRef<HTMLDivElement, {
    slide: any;
    activeMove: EnhancedTimelineNode | null;
    timeline: EnhancedTimelineNode[];
    reviewTitle: string;
    aspectRatio: "square" | "portrait";
    className?: string;
}>(({ slide, activeMove, timeline, reviewTitle, aspectRatio, className }, ref) => {

    // Resolve content
    const slideData = slide.data as any;
    const displayFen = slideData?.fen || activeMove?.fen;
    const moveNumber = slideData?.ply ? Math.ceil(slideData.ply / 2) : (activeMove ? Math.ceil(activeMove.ply / 2) : "-");

    // Improved Comment Logic: Look up the node in the timeline for this ply
    const nodeForSlide = slideData?.ply
        ? timeline.find(n => n.ply === slideData.ply)
        : activeMove;

    const displayComment = nodeForSlide?.shortComment
        || (nodeForSlide as any)?.comment
        || "A critical moment in the game.";

    const isBrilliant = slideData?.tags?.tactic?.includes("Brilliant")
        || nodeForSlide?.special === "brilliant"
        || (nodeForSlide?.judgement as any) === "brilliant";

    return (
        <div ref={ref} className={`relative flex flex-col bg-slate-900 overflow-hidden border border-white/10 ${aspectRatio === "square" ? "w-[600px] h-[600px]" : "w-[480px] h-[600px]"
            } ${className || ""}`}>

            {/* Header Brand */}
            <div className="h-1 bg-gradient-to-r from-accent-teal via-emerald-400 to-teal-600" />

            <div className="flex-1 relative">
                {slide.type === "cover" && (
                    <div className="absolute inset-0 flex flex-col items-center justify-center p-12 text-center">
                        <div className="w-24 h-24 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center rotate-6 mb-8 backdrop-blur-md shadow-2xl">
                            <span className="text-5xl">♟️</span>
                        </div>
                        <h1 className="text-4xl font-display font-bold text-white mb-4 leading-tight">
                            {reviewTitle}
                        </h1>
                        <div className="px-4 py-1.5 rounded-full bg-accent-teal/10 border border-accent-teal/20 text-accent-teal text-sm font-bold uppercase tracking-widest mb-8">
                            Game Analysis
                        </div>
                        <div className="grid grid-cols-3 gap-8 w-full max-w-sm border-t border-white/10 pt-8">
                            <div>
                                <div className="text-2xl font-bold text-white">24</div>
                                <div className="text-[10px] text-white/40 uppercase tracking-wider">Moves</div>
                            </div>
                            <div>
                                <div className="text-2xl font-bold text-emerald-400">85%</div>
                                <div className="text-[10px] text-white/40 uppercase tracking-wider">Accuracy</div>
                            </div>
                            <div>
                                <div className="text-2xl font-bold text-amber-400">!!</div>
                                <div className="text-[10px] text-white/40 uppercase tracking-wider">Brilliant</div>
                            </div>
                        </div>
                    </div>
                )}

                {slide.type === "moment" && (
                    <div className="absolute inset-0 flex flex-col">
                        <div className="h-14 border-b border-white/10 flex items-center justify-between px-6 bg-white/5">
                            <div className="flex items-center gap-3">
                                <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                                <span className="text-white font-bold font-mono">MOVE {moveNumber}</span>
                            </div>
                            {isBrilliant && (
                                <span className="text-emerald-400 font-display font-bold text-lg">BRILLIANT !!</span>
                            )}
                        </div>
                        <div className="flex-1 p-6 flex items-center justify-center bg-black/20">
                            <div className="relative aspect-square h-full max-h-full rounded shadow-2xl pointer-events-none border border-white/10">
                                {displayFen && (
                                    <BoardSection
                                        fen={displayFen}
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
                        <div className="p-6 bg-gradient-to-t from-black/60 to-transparent pt-0">
                            <div className="mb-2 flex gap-2">
                                {isBrilliant && (
                                    <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 text-[10px] font-bold uppercase">Brilliant</span>
                                )}
                            </div>
                            <p className="text-white text-sm font-serif leading-relaxed line-clamp-3">
                                {displayComment}
                            </p>
                        </div>
                    </div>
                )}

                {slide.type === "analysis" && (
                    <div className="absolute inset-0 flex flex-col p-8">
                        <div className="mb-6">
                            <div className="text-accent-teal font-mono text-xs uppercase tracking-widest mb-2">Coach&apos;s Insight</div>
                            <h3 className="text-2xl font-bold text-white leading-tight">
                                {displayComment.substring(0, 60)}...
                            </h3>
                        </div>
                        <div className="flex-1 bg-white/5 rounded-xl border border-white/10 p-6 relative overflow-hidden">
                            <div className="absolute top-0 right-0 p-4 opacity-10">
                                <Share2 className="w-24 h-24" />
                            </div>
                            <p className="text-white/80 text-lg leading-relaxed font-serif relative z-10 italic">
                                &quot;{displayComment}&quot;
                            </p>
                            <div className="mt-8 pt-6 border-t border-white/10">
                                <div className="text-xs font-bold text-white/40 uppercase mb-2">Coach Suggestion</div>
                                <div className="font-mono text-sm text-emerald-400">
                                    {/* Placeholder for engine best move logic, effectively accessing 'bestMove' from node */}
                                    This move was critical. Better was...
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {slide.type === "stats" && (
                    <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-b from-slate-900 to-black">
                        <div className="text-center space-y-8">
                            <div className="inline-block relative">
                                <div className="w-40 h-40 rounded-full border-8 border-white/5 flex items-center justify-center">
                                    <span className="text-6xl font-bold text-white">85</span>
                                </div>
                                <div className="absolute inset-0 rounded-full border-8 border-accent-teal border-t-transparent rotate-45" />
                            </div>
                            <div>
                                <h3 className="text-2xl font-bold text-white">Excellent Game!</h3>
                                <p className="text-white/40">You played with high precision.</p>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            <div className="h-12 border-t border-white/10 flex items-center justify-between px-6 bg-black/40">
                <div className="flex items-center gap-2 text-white/30 text-xs font-bold tracking-widest uppercase">
                    <div className="w-4 h-4 rounded bg-white/10" />
                    Chesstory.ai
                </div>
                <div className="text-white/30 text-xs font-mono">
                    @User • 2024
                </div>
            </div>
        </div>
    );
});
SlideCard.displayName = "SlideCard";

export function ShareModal({ activeMove, reviewTitle, book, timeline, onClose }: ShareModalProps) {
    const [aspectRatio, setAspectRatio] = useState<"square" | "portrait">("portrait");
    const [isGenerating, setIsGenerating] = useState(false);
    const exportRefs = useRef<(HTMLDivElement | null)[]>([]);

    // Carousel State
    type SlideType = "cover" | "moment" | "analysis" | "stats";
    interface Slide {
        id: string;
        type: SlideType;
        title: string;
        data?: any;
    }

    const [slides] = useState<Slide[]>(() => {
        const defaults: Slide[] = [{ id: "cover", type: "cover", title: "Cover" }];

        if (!book?.sections) return [
            ...defaults,
            { id: "s2", type: "moment", title: "Key Move" },
            { id: "s3", type: "analysis", title: "Coach's Insight" },
            { id: "last", type: "stats", title: "Stats" }
        ];

        const generatedSlides: Slide[] = [];
        book.sections.forEach(section => {
            if (!section.diagrams) return;
            section.diagrams.forEach(diag => {
                if (generatedSlides.length >= 3) return;
                generatedSlides.push({
                    id: `diag-${diag.ply}`,
                    type: "moment",
                    title: `Move ${Math.ceil(diag.ply / 2)}`,
                    data: diag
                });
                if (diag.tags?.mistake?.length || diag.tags?.tactic?.length) {
                    generatedSlides.push({
                        id: `analysis-${diag.ply}`,
                        type: "analysis",
                        title: "Insight",
                        data: diag
                    });
                }
            });
        });

        if (generatedSlides.length === 0) {
            generatedSlides.push({ id: "s2", type: "moment", title: "Key Move" });
        }

        return [...defaults, ...generatedSlides, { id: "stats", type: "stats", title: "Summary" }];
    });

    const [activeSlideId, setActiveSlideId] = useState<string>(slides[1]?.id || slides[0].id);
    const activeSlide = slides.find(s => s.id === activeSlideId) || slides[0];

    const handleDownloadAll = useCallback(async () => {
        setIsGenerating(true);
        try {
            await Promise.all(slides.map(async (slide, idx) => {
                const el = exportRefs.current[idx];
                if (el) {
                    const dataUrl = await toPng(el, { cacheBust: true, pixelRatio: 2 });
                    const link = document.createElement('a');
                    link.download = `chess-review-${slide.title.replace(/\s+/g, '-').toLowerCase()}-${idx + 1}.png`;
                    link.href = dataUrl;
                    link.click();
                }
            }));
        } catch (err) {
            console.error("Export failed:", err);
            alert("Failed to generate images. Please try again.");
        } finally {
            setIsGenerating(false);
        }
    }, [slides]);

    const handleDownloadPDF = useCallback(async () => {
        setIsGenerating(true);
        try {
            const pdf = new jsPDF({
                orientation: "portrait",
                unit: "px",
                format: "a4"
            });

            // A4 size in px (approx)
            const gw = 446; // pdf.internal.pageSize.getWidth();
            const gh = 630; // pdf.internal.pageSize.getHeight();

            for (let i = 0; i < slides.length; i++) {
                const el = exportRefs.current[i];
                if (!el) continue;

                if (i > 0) pdf.addPage();

                // Capture slide
                const dataUrl = await toPng(el, { cacheBust: true, pixelRatio: 2 });

                // Add to PDF centered
                const imgProps = pdf.getImageProperties(dataUrl);
                const pdfWidth = pdf.internal.pageSize.getWidth();
                const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;

                // Scale down if too tall
                // Simple logic: fit width with margins
                const margin = 40;
                const finalWidth = pdfWidth - (margin * 2);
                const finalHeight = (imgProps.height * finalWidth) / imgProps.width;

                pdf.addImage(dataUrl, 'PNG', margin, 60, finalWidth, finalHeight);

                // Add footer text or page number
                pdf.text(`Page ${i + 1}`, pdfWidth / 2, pdf.internal.pageSize.getHeight() - 20, { align: 'center' });
            }

            pdf.save(`chess-review-report.pdf`);

        } catch (err) {
            console.error("PDF Export failed:", err);
            alert("Failed to generate PDF. Please try again.");
        } finally {
            setIsGenerating(false);
        }
    }, [slides]);


    return (
        <Transition show={true} as={Fragment}>
            <Dialog onClose={onClose} className="relative z-50">
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm" />
                </Transition.Child>

                <div className="fixed inset-0 flex items-center justify-center p-4">
                    <Dialog.Panel className="w-full max-w-6xl h-[90vh] bg-slate-900 rounded-2xl border border-white/10 shadow-2xl flex overflow-hidden">
                        {/* Hidden Export Container */}
                        <div className="fixed left-[-9999px] top-0 pointer-events-none">
                            {slides.map((slide, idx) => (
                                <div key={slide.id} className="mb-4">
                                    <SlideCard
                                        ref={el => { exportRefs.current[idx] = el }}
                                        slide={slide}
                                        activeMove={activeMove}
                                        timeline={timeline}
                                        reviewTitle={reviewTitle}
                                        aspectRatio={aspectRatio}
                                    />
                                </div>
                            ))}
                        </div>

                        {/* Left Panel: Carousel Builder */}
                        <div className="w-80 border-r border-white/10 bg-black/20 p-6 flex flex-col gap-6">
                            <div>
                                <h2 className="text-xl font-display font-bold text-white mb-1">Share to Instagram</h2>
                                <p className="text-white/40 text-sm">Build your carousel.</p>
                            </div>

                            {/* Slide List */}
                            <div className="flex-1 overflow-y-auto space-y-2 pr-2">
                                {slides.map((slide, idx) => (
                                    <div
                                        key={slide.id}
                                        onClick={() => setActiveSlideId(slide.id)}
                                        className={`p-3 rounded-lg border cursor-pointer transition flex items-center gap-3 ${activeSlideId === slide.id
                                            ? "bg-white/10 border-accent-teal text-white"
                                            : "bg-white/5 border-white/5 text-white/50 hover:bg-white/10 hover:text-white"
                                            }`}
                                    >
                                        <div className="w-6 h-6 rounded-md bg-black/40 flex items-center justify-center text-xs font-bold">
                                            {idx + 1}
                                        </div>
                                        <div className="flex-1">
                                            <div className="text-xs font-bold uppercase tracking-wider opacity-70">{slide.type}</div>
                                            <div className="text-sm font-semibold">{slide.title}</div>
                                        </div>
                                    </div>
                                ))}

                                <button className="w-full py-3 rounded-lg border border-dashed border-white/20 text-white/40 hover:text-white hover:border-white/40 hover:bg-white/5 transition text-sm font-bold flex items-center justify-center gap-2">
                                    + Add Slide
                                </button>
                            </div>

                            {/* Action Buttons */}
                            <div className="space-y-3 pt-4 border-t border-white/10">
                                <div className="flex gap-2">
                                    <button
                                        onClick={() => setAspectRatio("square")}
                                        className={`flex-1 py-2 rounded text-xs font-bold border transition ${aspectRatio === "square" ? "bg-accent-teal/10 border-accent-teal text-accent-teal" : "border-transparent text-white/40 hover:text-white"}`}
                                    >
                                        1:1 Square
                                    </button>
                                    <button
                                        onClick={() => setAspectRatio("portrait")}
                                        className={`flex-1 py-2 rounded text-xs font-bold border transition ${aspectRatio === "portrait" ? "bg-accent-teal/10 border-accent-teal text-accent-teal" : "border-transparent text-white/40 hover:text-white"}`}
                                    >
                                        4:5 Portrait
                                    </button>
                                </div>
                                <button
                                    onClick={handleDownloadAll}
                                    disabled={isGenerating}
                                    className="w-full py-3 rounded-xl bg-white text-black font-bold hover:scale-[1.02] active:scale-[0.98] transition flex items-center justify-center gap-2 shadow-xl shadow-white/5 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {isGenerating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                                    Images
                                </button>
                                <button
                                    onClick={handleDownloadPDF}
                                    disabled={isGenerating}
                                    className="w-full py-3 rounded-xl bg-accent-teal text-white font-bold hover:scale-[1.02] active:scale-[0.98] transition flex items-center justify-center gap-2 shadow-xl shadow-accent-teal/20 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {isGenerating ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileText className="w-4 h-4" />}
                                    PDF Report
                                </button>
                            </div>
                        </div>

                        {/* Right Panel: Preview Canvas */}
                        <div className="flex-1 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] bg-slate-950 flex flex-col items-center justify-center relative p-12 overflow-hidden">
                            <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-slate-950 to-black opacity-90" />

                            {/* The Card/Canvas */}
                            <SlideCard
                                slide={activeSlide}
                                activeMove={activeMove}
                                timeline={timeline}
                                reviewTitle={reviewTitle}
                                aspectRatio={aspectRatio}
                                className="transition-all duration-500 ease-out shadow-2xl shadow-black/80"
                            />
                        </div>
                    </Dialog.Panel>
                </div>
            </Dialog>
        </Transition>
    );
}
