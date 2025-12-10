import React, { useMemo } from 'react';
import { useStudy } from './StudyContext';
import { ChapterNavigation } from './book/ChapterNavigation';
import { ReadingMode } from './book/ReadingMode';
import { ChecklistDashboard } from './dashboard/ChecklistDashboard';
import { TurningPointView } from './interactive/TurningPointView';
import { TaggedMoveList } from './book/TaggedMoveList';
import InteractiveBoard from '../board/InteractiveBoard';
import { getConceptShapes } from '../board/ConceptOverlays';

export const StudyShell: React.FC = () => {
    const {
        study,
        currentChapterIndex,
        currentPly,
        setChapter,
        setPly,
        viewMode,
        setViewMode
    } = useStudy();

    const activeSection = study.book.sections[currentChapterIndex];

    // Board logic
    const currentNode = useMemo(() =>
        study.timeline.find(n => n.ply === currentPly),
        [study.timeline, currentPly]
    );

    const currentFen = currentNode?.fen || "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Overlays logic
    // Add null check for tags
    const orientation = 'white';
    const shapes = useMemo(() =>
        getConceptShapes(currentNode?.semanticTags || [], orientation),
        [currentNode, orientation]
    );

    return (
        <div className="flex h-screen bg-slate-950 text-slate-200 overflow-hidden">
            {/* Left Sidebar: Navigation */}
            <ChapterNavigation
                sections={study.book.sections}
                activeSectionIndex={currentChapterIndex}
                onSelect={setChapter}
            />

            {/* Center: Content Area */}
            <div className="flex-1 flex flex-col min-w-0">
                {/* Top Bar / Tabs */}
                <div className="flex items-center px-6 py-3 border-b border-slate-700 bg-slate-900 gap-4">
                    <button
                        onClick={() => setViewMode('read')}
                        className={`font-bold ${viewMode === 'read' ? 'text-blue-400' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                        📖 Read
                    </button>
                    <button
                        onClick={() => setViewMode('quiz')}
                        className={`font-bold ${viewMode === 'quiz' ? 'text-amber-400' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                        ⚡ Train
                    </button>
                    <button
                        onClick={() => setViewMode('dashboard')}
                        className={`font-bold ${viewMode === 'dashboard' ? 'text-emerald-400' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                        ✅ Review
                    </button>
                </div>

                {/* Main Viewport */}
                <div className="flex-1 overflow-hidden relative flex">
                    {/* Dynamic Content based on ViewMode */}
                    {viewMode === 'read' && activeSection && (
                        <div className="flex-1 flex overflow-hidden">
                            <ReadingMode
                                section={activeSection}
                                onDiagramClick={(fen) => {
                                    // Reverse lookup ply from diagrams if possible, else just use FEN
                                    const diag = activeSection.diagrams.find(d => d.fen === fen);
                                    if (diag) setPly(diag.ply);
                                }}
                            />
                        </div>
                    )}

                    {viewMode === 'quiz' && (
                        <div className="flex-1 overflow-y-auto p-8 flex items-center justify-center">
                            <div className="text-center text-slate-400">
                                Select a critical moment from the sidebar or...
                                <br />
                                (Detailed quiz integration pending)
                            </div>
                        </div>
                    )}

                    {viewMode === 'dashboard' && (
                        <div className="flex-1 overflow-y-auto">
                            <ChecklistDashboard checklist={study.book.checklist} />
                        </div>
                    )}

                    {/* Right Sidebar: Board & Moves (Desktop) */}
                    <div className="w-[400px] border-l border-slate-700 bg-slate-900 flex flex-col">
                        <div className="aspect-square w-full bg-slate-800">
                            <InteractiveBoard
                                fen={currentFen}
                                orientation={orientation}
                                viewOnly={true}
                                shapes={shapes}
                            />
                        </div>
                        <div className="flex-1 min-h-0">
                            <TaggedMoveList
                                timeline={study.timeline}
                                currentPly={currentPly}
                                onSelectPly={setPly}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
