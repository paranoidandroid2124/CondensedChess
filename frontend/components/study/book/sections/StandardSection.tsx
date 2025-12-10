import React from 'react';
import { BookSection } from '@/types/StudyModel';
import InteractiveBoard from '@/components/board/InteractiveBoard';
import { motion } from 'framer-motion';

interface Props {
    section: BookSection;
    onDiagramClick?: (fen: string) => void;
}

export const StandardSection: React.FC<Props> = ({ section, onDiagramClick }) => {
    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex-1 overflow-y-auto p-8 bg-slate-900 text-slate-200 font-sans leading-relaxed"
        >
            {/* Header */}
            <h2 className="text-3xl font-serif font-bold text-slate-100 mb-6 border-b border-slate-700 pb-2 flex items-center justify-between">
                {section.title}
                <span className="text-sm font-mono font-normal text-slate-500 px-2 py-1 bg-slate-800 rounded">
                    {section.sectionType}
                </span>
            </h2>

            {/* Narrative Text */}
            <div className="prose prose-invert max-w-none text-lg text-slate-300 mb-8 space-y-4">
                {section.narrativeHint.split('\n').map((para, idx) => (
                    para.trim() && <p key={idx}>{para}</p>
                ))}
            </div>

            {/* Embedded Diagrams */}
            {section.diagrams.length > 0 && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 my-8">
                    {section.diagrams.map((diag) => (
                        <div key={diag.id} className="group flex flex-col items-center">
                            <div
                                className="relative w-full aspect-square max-w-sm border-2 border-slate-600 rounded-lg overflow-hidden shadow-lg cursor-pointer hover:border-blue-500 transition-colors"
                                onClick={() => onDiagramClick?.(diag.fen)}
                            >
                                <InteractiveBoard
                                    fen={diag.fen}
                                    viewOnly={true}
                                    orientation={diag.roles.includes('Black') ? 'black' : 'white'}
                                />
                                {/* Overlay hint */}
                                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                    <span className="text-white font-bold bg-blue-600 px-3 py-1 rounded-full text-sm">
                                        Load Position
                                    </span>
                                </div>
                            </div>
                            <div className="mt-2 text-sm text-slate-400 text-center italic">
                                {diag.roles.join(', ') || 'Key Position'}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </motion.div>
    );
};
