import React from 'react';
import { BookSection } from '@/types/StudyModel';
import InteractiveBoard from '@/components/board/InteractiveBoard';
import { motion } from 'framer-motion';

interface Props {
    section: BookSection;
    onDiagramClick?: (fen: string) => void;
}

export const OpeningHero: React.FC<Props> = ({ section, onDiagramClick }) => {
    // Usually the first diagram is the key position for Opening
    const mainDiagram = section.diagrams[0];

    const container = {
        hidden: { opacity: 0 },
        show: {
            opacity: 1,
            transition: {
                staggerChildren: 0.1
            }
        }
    };

    const item = {
        hidden: { opacity: 0, y: 20 },
        show: { opacity: 1, y: 0 }
    };

    return (
        <motion.div
            variants={container}
            initial="hidden"
            animate="show"
            className="flex-1 overflow-y-auto bg-slate-950 text-slate-200 font-serif"
        >
            <div className="relative w-full bg-gradient-to-b from-slate-900 to-slate-950 border-b border-slate-800 pb-12 pt-8 px-8 flex flex-col items-center text-center">
                <motion.div variants={item} className="mb-6">
                    <span className="inline-block px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 text-xs font-bold tracking-wider uppercase mb-3">
                        Opening Phase
                    </span>
                    <h2 className="text-4xl md:text-5xl font-serif font-bold text-white mb-4">
                        {section.title}
                    </h2>
                    <p className="text-xl text-slate-400 max-w-2xl mx-auto italic">
                        {section.narrativeHint.split('\n')[0]}
                    </p>
                </motion.div>

                {mainDiagram && (
                    <motion.div variants={item} className="relative group">
                        <div
                            className="relative w-80 h-80 md:w-96 md:h-96 rounded-lg shadow-2xl shadow-blue-900/20 overflow-hidden border-4 border-slate-700 group-hover:border-blue-500 transition-all cursor-pointer"
                            onClick={() => onDiagramClick?.(mainDiagram.fen)}
                        >
                            <InteractiveBoard
                                fen={mainDiagram.fen}
                                viewOnly={true}
                                orientation={mainDiagram.roles.includes('Black') ? 'black' : 'white'}
                            />
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                <span className="px-4 py-2 bg-blue-600 text-white font-bold rounded-full transform scale-90 group-hover:scale-100 transition-transform">
                                    Analyze Position
                                </span>
                            </div>
                        </div>
                    </motion.div>
                )}
            </div>

            <motion.div variants={item} className="max-w-3xl mx-auto p-8 prose prose-invert prose-lg">
                {section.narrativeHint.split('\n').slice(1).map((para, idx) => (
                    para.trim() && <p key={idx}>{para}</p>
                ))}
            </motion.div>

            {/* Secondary Diagrams */}
            {section.diagrams.length > 1 && (
                <motion.div variants={item} className="max-w-4xl mx-auto px-8 pb-12">
                    <h3 className="text-lg font-bold text-slate-400 mb-6 uppercase tracking-wider text-center">Key Variations</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                        {section.diagrams.slice(1).map((diag) => (
                            <div key={diag.id} className="flex flex-col items-center">
                                <div
                                    className="w-full aspect-square max-w-[240px] border-2 border-slate-700 rounded-lg overflow-hidden cursor-pointer hover:border-slate-500 transition-colors"
                                    onClick={() => onDiagramClick?.(diag.fen)}
                                >
                                    <InteractiveBoard fen={diag.fen} viewOnly={true} />
                                </div>
                                <span className="mt-2 text-sm text-slate-500">{diag.roles.join(', ')}</span>
                            </div>
                        ))}
                    </div>
                </motion.div>
            )}
        </motion.div>
    );
};
