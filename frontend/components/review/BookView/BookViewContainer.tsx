import React from "react";
import type { Book, BookDiagram, BookTurningPoint, BookTacticalMoment, ChecklistBlock, BookSection, SectionType } from "../../../types/StudyModel";
import { MiniBoard } from "../../common/MiniBoard";

interface BookViewContainerProps {
    book: Book;
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


function SectionRenderer({ section, onSelectPly }: { section: BookSection; onSelectPly: (ply: number) => void }) {
    const typeIcons: Record<string, string> = {
        OpeningPortrait: "🌅",
        CriticalCrisis: "⚠️",
        StructuralDeepDive: "🏗️",
        TacticalStorm: "⚔️",
        EndgameMasterclass: "👑",
        NarrativeBridge: "🌉"
    };

    const typeColors: Record<string, string> = {
        OpeningPortrait: "border-blue-500/30 bg-blue-500/5",
        CriticalCrisis: "border-red-500/30 bg-red-500/5",
        StructuralDeepDive: "border-cyan-500/30 bg-cyan-500/5",
        TacticalStorm: "border-orange-500/30 bg-orange-500/5",
        EndgameMasterclass: "border-purple-500/30 bg-purple-500/5",
        NarrativeBridge: "border-slate-500/30 bg-slate-500/5"
    };

    return (
        <div className={`py-6 border-b border-white/5`}>
            <div className="flex items-center gap-2 mb-3">
                <span className="text-xl">{typeIcons[section.sectionType]}</span>
                <h2 className="text-lg font-bold text-white">{section.title}</h2>
                <span className="text-xs text-white/40 ml-auto">Ply {section.startPly}-{section.endPly}</span>
            </div>

            <div className={`p-4 rounded-lg border ${typeColors[section.sectionType] || "border-white/10 bg-white/5"}`}>
                <p className="text-sm text-white/80 mb-4 whitespace-pre-wrap leading-relaxed">
                    {section.narrativeHint}
                </p>

                {section.diagrams.length > 0 && (
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
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
            </div>
        </div>
    );
}

function ChecklistSection({ blocks }: { blocks: ChecklistBlock[] }) {
    if (blocks.length === 0) return null;

    return (
        <div className="py-6">
            <h2 className="text-lg font-bold text-white mb-4">✅ Study Checklist</h2>
            <div className="space-y-4">
                {blocks.map((block, i) => (
                    <div key={i} className="p-4 rounded-lg border border-white/10 bg-white/5">
                        <div className="font-medium text-white mb-2">{block.category}</div>
                        <ul className="space-y-1">
                            {block.hintTags.map((hint, j) => (
                                <li key={j} className="text-sm text-white/70 flex items-start gap-2">
                                    <span className="text-green-400 mt-0.5">•</span>
                                    {hint}
                                </li>
                            ))}
                        </ul>
                    </div>
                ))}
            </div>
        </div>
    );
}

// --- Main Container ---

export function BookViewContainer({ book, onSelectPly }: BookViewContainerProps) {
    return (
        <div className="space-y-2 pb-16">
            <IntroSection meta={book.gameMeta} />
            {/* Render Sections Dynamically */}
            {book.sections.map((section, idx) => (
                <SectionRenderer key={idx} section={section} onSelectPly={onSelectPly} />
            ))}
            <ChecklistSection blocks={book.checklist} />
        </div>
    );
}
