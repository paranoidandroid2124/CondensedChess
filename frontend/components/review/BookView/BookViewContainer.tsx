import React from "react";
import type { Book, BookDiagram, BookTurningPoint, BookTacticalMoment, BookSection, SectionType } from "../../../types/StudyModel";
import { MiniBoard } from "../../common/MiniBoard";

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

function SectionRenderer({ section, root, onSelectPly, onPreviewFen, onSelectNode, onMoveHover }: {
    section: BookSection;
    root?: ReviewTreeNode;
    onSelectPly: (ply: number) => void;
    onPreviewFen?: (fen: string | null) => void;
    onSelectNode?: (node: ReviewTreeNode) => void;
    onMoveHover?: (node: ReviewTreeNode | null) => void;
}) {
    // ... Type defs omitted for brevity, they are unchanged ...

    // (Re-copy icons/colors maps from original if not already in closure, assuming component function continues)
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

    return (
        <div className={`py-8 border-b border-white/5`}>
            {/* Header */}
            <div className="flex items-center gap-3 mb-6">
                <span className="text-2xl filter drop-shadow-md">{typeIcons[section.sectionType]}</span>
                <h2 className="text-2xl font-bold text-white font-serif tracking-tight">{section.title}</h2>
                <div className="ml-auto flex flex-col items-end">
                    <span className="text-xs font-mono text-white/30 uppercase tracking-widest">Ply {section.startPly}-{section.endPly}</span>
                </div>
            </div>

            {/* Structured Metadata Table */}
            {section.metadata && (
                <KeyPointsTable metadata={section.metadata} />
            )}

            <div className={`p-6 rounded-xl border ${typeColors[section.sectionType] || "border-white/10 bg-white/5"} relative overflow-hidden group`}>

                {/* Introduction Text */}
                <div className="mb-6 font-serif text-lg leading-loose text-white/80 space-y-4 max-w-none">
                    {section.narrativeHint.split('\n').map((para, i) => (
                        para.trim() && <p key={i}>{para}</p>
                    ))}
                </div>

                {/* Diagrams */}
                {section.diagrams.length > 0 && (
                    <div className="my-8 grid grid-cols-2 sm:grid-cols-3 gap-4">
                        {section.diagrams.map(d => (
                            <DiagramCard
                                key={d.id}
                                diagram={d}
                                displayRole={section.sectionType}
                                onClick={() => onSelectPly(d.ply)}
                            />
                        ))}
                    </div>
                )}

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
    );
}

// --- Table of Contents ---

function TableOfContents({ sections }: { sections: BookSection[] }) {
    const scrollToSection = (idx: number) => {
        const el = document.getElementById(`book-section-${idx}`);
        if (el) {
            const offset = 80; // Header offset
            const bodyRect = document.body.getBoundingClientRect().top;
            const elementRect = el.getBoundingClientRect().top;
            const elementPosition = elementRect - bodyRect;
            const offsetPosition = elementPosition - offset;

            window.scrollTo({
                top: offsetPosition,
                behavior: "smooth"
            });
        }
    };

    return (
        <nav className="space-y-1">
            <h3 className="text-xs font-bold uppercase tracking-widest text-white/40 mb-3 px-2">Contents</h3>
            {sections.map((section, idx) => (
                <button
                    key={idx}
                    onClick={() => scrollToSection(idx)}
                    className="block w-full text-left px-3 py-2 text-sm rounded transition-colors text-white/60 hover:bg-white/5 hover:text-white truncate"
                >
                    <span className="mr-2 opacity-50 text-[10px]">{idx + 1}.</span>
                    {section.title}
                </button>
            ))}
        </nav>
    );
}

export function BookViewContainer({ book, root, onSelectPly, onPreviewFen, onSelectNode, onMoveHover }: BookViewContainerProps) {
    if (!book || !book.sections) return (
        <div className="py-12 text-center text-white/40 italic">
            No book content generated yet. Run analysis to create the book.
        </div>
    );

    return (
        <div className="flex flex-col lg:flex-row gap-8 lg:gap-12 relative items-start">
            {/* Sidebar TOC - Desktop only */}
            <aside className="hidden lg:block w-56 xl:w-64 sticky top-24 self-start max-h-[calc(100vh-8rem)] overflow-y-auto pr-2 custom-scrollbar">
                <TableOfContents sections={book.sections} />
            </aside>

            {/* Main Content */}
            <main className="flex-1 min-w-0 space-y-4 pb-16 w-full">
                <IntroSection meta={book.gameMeta} />

                {/* Render Sections Dynamically */}
                {book.sections.map((section, idx) => (
                    <div key={idx} id={`book-section-${idx}`}>
                        <SectionRenderer
                            section={section}
                            root={root}
                            onSelectPly={onSelectPly}
                            onPreviewFen={onPreviewFen}
                            onSelectNode={onSelectNode}
                            onMoveHover={onMoveHover}
                        />
                    </div>
                ))}


            </main>
        </div>
    );
}
