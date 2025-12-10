import React from 'react';
import { BookSection } from '@/types/StudyModel';
import InteractiveBoard from '@/components/board/InteractiveBoard';
import { motion } from 'framer-motion';

interface Props {
    section: BookSection;
    onDiagramClick?: (fen: string) => void;
}

export const CrisisView: React.FC<Props> = ({ section, onDiagramClick }) => {
    return (
        <div className="flex-1 overflow-y-auto bg-slate-950 text-slate-200 font-sans border-l-4 border-amber-600">
            <div className="p-8 md:p-12 max-w-5xl mx-auto">
                <div className="flex flex-col md:flex-row gap-8 items-start">

                    <div className="flex-1 space-y-6">
                        <motion.div
                            initial={{ scale: 0.9, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            transition={{ type: "spring" }}
                            className="flex items-center gap-3 mb-2"
                        >
                            <motion.div
                                animate={{ scale: [1, 1.2, 1] }}
                                transition={{ repeat: Infinity, duration: 2 }}
                                className="w-8 h-8 rounded bg-amber-500/20 text-amber-500 flex items-center justify-center font-bold"
                            >
                                !
                            </motion.div>
                            <span className="text-amber-500 font-mono text-sm uppercase tracking-widest">Critical Moment</span>
                        </motion.div>

                        <h2 className="text-3xl md:text-4xl font-bold text-white relative">
                            {section.title}
                            <span className="absolute -left-6 top-0 bottom-0 w-1 bg-amber-600/50 rounded-full hidden md:block"></span>
                        </h2>

                        <div className="prose prose-invert prose-lg text-slate-300">
                            {section.narrativeHint.split('\n').map((para, idx) => (
                                para.trim() && <p key={idx} className="first:text-xl first:text-slate-100 first:font-serif">{para}</p>
                            ))}
                        </div>
                    </div>

                    <div className="w-full md:w-96 flex-shrink-0">
                        {section.diagrams.map((diag, index) => (
                            <motion.div
                                key={diag.id}
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: index * 0.2 }}
                                className="mb-6 last:mb-0 bg-slate-900 p-4 rounded-xl border border-slate-800 shadow-xl"
                            >
                                <div
                                    className="aspect-square w-full rounded overflow-hidden border border-slate-700 cursor-pointer hover:border-amber-500 transition-colors"
                                    onClick={() => onDiagramClick?.(diag.fen)}
                                >
                                    <InteractiveBoard
                                        fen={diag.fen}
                                        viewOnly={true}
                                        orientation={diag.roles.includes('Black') ? 'black' : 'white'}
                                    />
                                </div>
                                <div className="mt-3 flex flex-wrap gap-2 justify-center">
                                    {diag.roles.map(role => (
                                        <span key={role} className="text-xs px-2 py-1 bg-slate-800 text-slate-400 rounded border border-slate-700">
                                            {role}
                                        </span>
                                    ))}
                                </div>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};
