import React from "react";
import type { Book, BookDiagram, BookTurningPoint, BookTacticalMoment, BookSection, SectionType } from "../../../types/StudyModel";
import { MiniBoard } from "../../common/MiniBoard";
import InteractiveBoard from "../../board/InteractiveBoard";

interface BookViewContainerProps {
    book: Book;
    root?: ReviewTreeNode;
    onSelectPly: (ply: number) => void;
}

// --- Section Components ---

function IntroSection({ meta }: { meta: Book["gameMeta"] }) {
    return (
        <div className="pb-6 border-b border-white/10">
            <h1 className="text-2xl font-bold text-white mb-2">
                {meta.white} vs {meta.black}
            </h1>
            <div className="flex gap-3 text-sm text-white/60">
                <span className="px-2 py-0.5 rounded bg-white/10">{meta.result}</span>
                {meta.openingName && <span>{meta.openingName}</span>}
            </div>
        </div>
    );
}

function DiagramCard({
    diagram,
    displayRole,
    onClick
}: {
    diagram: BookDiagram;
    displayRole: string;
    onClick: () => void;
}) {
    const roleColors: Record<string, string> = {
        OpeningPortrait: "border-blue-500/50 bg-blue-500/10",
        CriticalCrisis: "border-red-500/50 bg-red-500/10", // Mapped from TurningPoint? Or keep separate
        TurningPoint: "border-red-500/50 bg-red-500/10",
        StructuralDeepDive: "border-cyan-500/50 bg-cyan-500/10",
        TacticalStorm: "border-orange-500/50 bg-orange-500/10",
        EndgameMasterclass: "border-purple-500/50 bg-purple-500/10",
        Normal: "border-white/10 bg-white/5"
    };

    return (
        <div
            className={`p-3 rounded-lg border cursor-pointer hover:scale-[1.02] transition-transform ${roleColors[displayRole] || roleColors.Normal}`}
            onClick={onClick}
        >
            <div className="w-24 h-24 mb-2">
                <MiniBoard fen={diagram.fen} orientation="white" />
            </div>
            <div className="text-xs text-white/80 font-medium capitalize">
                {displayRole.replace(/([A-Z])/g, ' $1').trim()}
            </div>
            <div className="text-[10px] text-white/50">Ply {diagram.ply}</div>
            <div className="flex flex-wrap gap-1 mt-1">
                {diagram.tags.transition.slice(0, 2).map((t, i) => (
                    <span key={i} className="px-1.5 py-0.5 rounded bg-white/10 text-[9px] text-white/60">
                        {t}
                    </span>
                ))}
            </div>
        </div>
    );
}


import { NarrativeRenderer } from "./NarrativeRenderer";
import { KeyPointsTable } from "../../study/book/sections/KeyPointsTable";
import type { ReviewTreeNode } from "../../../types/review";

// Helper to find node
function findNodeByPly(root: ReviewTreeNode, ply: number): ReviewTreeNode | null {
    if (root.ply === ply) return root;
    if (root.children) {
        for (const child of root.children) {
            const found = findNodeByPly(child, ply);
            if (found) return found;
        }
    }
    return null;
}





// --- Main Container ---

interface BookViewContainerProps {
    book: Book;
    root?: ReviewTreeNode;
    onSelectPly: (ply: number) => void;
    onPreviewFen?: (fen: string | null) => void;
    onSelectNode?: (node: ReviewTreeNode) => void;
    onMoveHover?: (node: ReviewTreeNode | null) => void;
}

function SectionRenderer({ section, root, onSelectPly, onPreviewFen, onSelectNode, onMoveHover, sectionIndex = 0 }: {
    section: BookSection;
    root?: ReviewTreeNode;
    onSelectPly: (ply: number) => void;
    onPreviewFen?: (fen: string | null) => void;
    onSelectNode?: (node: ReviewTreeNode) => void;
    onMoveHover?: (node: ReviewTreeNode | null) => void;
    sectionIndex?: number;
}) {
    const typeIcons: Record<string, string> = {
        OpeningReview: "📖",
        TurningPoints: "⚡",
        MiddlegamePlans: "🎯",
        TacticalStorm: "⚔️",
        TacticalMoments: "✨",
        EndgameLessons: "🎓",
        TitleSummary: "📑",
        KeyDiagrams: "📸",
        FinalChecklist: "✅"
    };

    const typeColors: Record<string, string> = {
        OpeningReview: "border-indigo-500/30 bg-indigo-500/5",
        TurningPoints: "border-rose-500/30 bg-rose-500/5",
        MiddlegamePlans: "border-teal-500/30 bg-teal-500/5",
        TacticalStorm: "border-orange-500/30 bg-orange-500/5",
        TacticalMoments: "border-amber-500/30 bg-amber-500/5",
        EndgameLessons: "border-violet-500/30 bg-violet-500/5",
        TitleSummary: "border-white/10 bg-white/5",
        KeyDiagrams: "border-white/10 bg-white/5",
        FinalChecklist: "border-green-500/30 bg-green-500/5"
    };

    // Find the start node for this section to anchor the narrative
    const startNode = root ? findNodeByPly(root, section.startPly) : null;

    // Zigzag: even sections = board left, odd = board right
    const isEven = sectionIndex % 2 === 0;
    const primaryDiagram = section.diagrams[0];

    return (
        <div className={`py-10 border-b border-white/5`}>
            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <span className="text-3xl filter drop-shadow-md">{typeIcons[section.sectionType]}</span>
                <div className="flex-1">
                    <h2 className="text-2xl font-bold text-white font-serif tracking-tight">{section.title}</h2>
                    <span className="text-xs font-mono text-white/30 uppercase tracking-widest">
                        Moves {Math.ceil(section.startPly / 2)} – {Math.ceil(section.endPly / 2)}
                    </span>
                </div>
            </div>

            {/* Structured Metadata Table */}
            {section.metadata && (
                <KeyPointsTable metadata={section.metadata} />
            )}

            {/* Magazine-style Zigzag Layout */}
            <div className={`grid gap-8 ${primaryDiagram ? 'lg:grid-cols-[1fr_280px]' : ''} ${!isEven && primaryDiagram ? 'lg:grid-cols-[280px_1fr]' : ''}`}>

                {/* Board Column (appears first on even, second on odd) */}
                {primaryDiagram && !isEven && (
                    <div className="order-1 lg:order-1">
                        <div
                            className={`sticky top-28 rounded-xl border p-3 cursor-pointer hover:scale-[1.02] transition-all ${typeColors[section.sectionType] || 'border-white/10 bg-white/5'}`}
                            onClick={() => onSelectPly(primaryDiagram.ply)}
                        >
                            <div className="w-full aspect-square">
                                <InteractiveBoard fen={primaryDiagram.fen} orientation="white" viewOnly={true} />
                            </div>
                            <div className="mt-2 text-center text-xs text-white/50 font-mono">
                                Move {Math.ceil(primaryDiagram.ply / 2)}
                            </div>
                            {primaryDiagram.tags.transition.length > 0 && (
                                <div className="flex flex-wrap gap-1 justify-center mt-2">
                                    {primaryDiagram.tags.transition.slice(0, 2).map((t, i) => (
                                        <span key={i} className="px-2 py-0.5 rounded-full bg-white/10 text-[10px] text-white/60">
                                            {t}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Content Column */}
                <div className={`${!isEven && primaryDiagram ? 'order-2 lg:order-2' : 'order-1'}`}>
                    <div className={`p-6 rounded-xl border ${typeColors[section.sectionType] || "border-white/10 bg-white/5"} relative overflow-hidden`}>

                        {/* Narrative Text */}
                        <div className="font-serif text-lg leading-loose text-white/80 space-y-4 max-w-none">
                            {section.narrativeHint.split('\n').map((para, i) => (
                                para.trim() && <p key={i} className="first-letter:text-3xl first-letter:font-bold first-letter:mr-1 first-letter:float-left">{para}</p>
                            ))}
                        </div>

                        {/* Hierarchical Narrative Rendering */}
                        {startNode && (
                            <div className="mt-8 pt-6 border-t border-white/10">
                                <h3 className="text-sm font-bold text-white/50 uppercase tracking-widest mb-4">Detailed Analysis</h3>
                                <div className="font-serif text-base leading-relaxed space-y-2 break-words text-justify">
                                    <NarrativeRenderer
                                        node={startNode}
                                        onInteract={(node) => {
                                            onSelectPly(node.ply);
                                            if (onPreviewFen && node.fen) {
                                                onPreviewFen(node.fen);
                                            }
                                            onSelectNode?.(node);
                                        }}
                                        onMoveHover={onMoveHover}
                                        depth={0}
                                        endPly={section.endPly}
                                    />
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Board Column (appears second on even sections) */}
                {primaryDiagram && isEven && (
                    <div className="order-2 lg:order-2">
                        <div
                            className={`sticky top-28 rounded-xl border p-3 cursor-pointer hover:scale-[1.02] transition-all ${typeColors[section.sectionType] || 'border-white/10 bg-white/5'}`}
                            onClick={() => onSelectPly(primaryDiagram.ply)}
                        >
                            <div className="w-full aspect-square">
                                <InteractiveBoard fen={primaryDiagram.fen} orientation="white" viewOnly={true} />
                            </div>
                            <div className="mt-2 text-center text-xs text-white/50 font-mono">
                                Move {Math.ceil(primaryDiagram.ply / 2)}
                            </div>
                            {primaryDiagram.tags.transition.length > 0 && (
                                <div className="flex flex-wrap gap-1 justify-center mt-2">
                                    {primaryDiagram.tags.transition.slice(0, 2).map((t, i) => (
                                        <span key={i} className="px-2 py-0.5 rounded-full bg-white/10 text-[10px] text-white/60">
                                            {t}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>

            {/* Additional Diagrams (if more than 1) */}
            {section.diagrams.length > 1 && (
                <div className="mt-8 grid grid-cols-3 sm:grid-cols-4 gap-3">
                    {section.diagrams.slice(1).map(d => (
                        <DiagramCard
                            key={d.id}
                            diagram={d}
                            displayRole={section.sectionType}
                            onClick={() => onSelectPly(d.ply)}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}


// --- Table of Contents ---

// --- Table of Contents (Horizontal Tabs) ---

function TableOfContents({ sections }: { sections: BookSection[] }) {
    const scrollToSection = (idx: number) => {
        const el = document.getElementById(`book-section-${idx}`);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    };

    return (
        <div className="sticky top-0 z-20 bg-slate-900/95 backdrop-blur-sm border-b border-white/10 mb-6 -mx-4 px-4 py-2 flex items-center gap-2 overflow-x-auto no-scrollbar mask-gradient-right">
            {sections.map((section, idx) => (
                <button
                    key={idx}
                    onClick={() => scrollToSection(idx)}
                    className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-colors bg-white/5 text-white/60 hover:bg-white/10 hover:text-white border border-transparent hover:border-white/10 whitespace-nowrap"
                >
                    <span className="mr-1.5 opacity-50">{idx + 1}.</span>
                    {section.title}
                </button>
            ))}
        </div>
    );
}

export function BookViewContainer({ book, root, onSelectPly, onPreviewFen, onSelectNode, onMoveHover }: BookViewContainerProps) {
    if (!book || !book.sections) return (
        <div className="py-12 text-center text-white/40 italic">
            No book content generated yet. Run analysis to create the book.
        </div>
    );

    return (
        <div className="flex flex-col relative w-full max-w-4xl mx-auto px-4 lg:px-0 pb-20">
            <IntroSection meta={book.gameMeta} />

            {/* Sticky Horizontal TOC */}
            <TableOfContents sections={book.sections} />

            {/* Main Content (Full Width) */}
            <main className="w-full space-y-12">
                {/* Render Sections Dynamically */}
                {book.sections.map((section, idx) => (
                    <div key={idx} id={`book-section-${idx}`} className="scroll-mt-24">
                        <SectionRenderer
                            section={section}
                            root={root}
                            onSelectPly={onSelectPly}
                            onPreviewFen={onPreviewFen}
                            onSelectNode={onSelectNode}
                            onMoveHover={onMoveHover}
                            sectionIndex={idx}
                        />
                    </div>
                ))}
            </main>
        </div>
    );
}
