import React from 'react';
import { ChecklistBlock } from '../../../types/StudyModel';

interface Props {
    checklist: ChecklistBlock[];
}

const getCategoryColor = (cat: string) => {
    if (cat.includes("Structure")) return "bg-blue-600";
    if (cat.includes("Plan")) return "bg-emerald-600";
    if (cat.includes("Tactic")) return "bg-red-600";
    if (cat.includes("Endgame")) return "bg-purple-600";
    return "bg-slate-600";
};

export const ChecklistDashboard: React.FC<Props> = ({ checklist }) => {
    return (
        <div className="p-8">
            <h2 className="text-3xl font-serif font-bold text-slate-100 mb-8 border-b border-slate-700 pb-4">
                Study Verification Checklist
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {checklist.map((block, idx) => {
                    const colorClass = getCategoryColor(block.category);
                    return (
                        <div key={idx} className="bg-slate-800 rounded-xl overflow-hidden border border-slate-600 shadow-lg hover:shadow-xl transition-shadow group">
                            <div className={`${colorClass} p-4`}>
                                <h3 className="text-lg font-bold text-white uppercase tracking-wider flex items-center gap-2">
                                    <span className="opacity-80">📌</span> {block.category}
                                </h3>
                            </div>
                            <div className="p-6">
                                <ul className="space-y-3">
                                    {block.hintTags.map((tag, tIdx) => (
                                        <li key={tIdx} className="flex items-start gap-3">
                                            <div className={`w-1.5 h-1.5 rounded-full mt-2 shrink-0 ${colorClass.replace('bg-', 'bg-') /* Hack: simplify logic */} bg-slate-400`} />
                                            <span className="text-slate-300 group-hover:text-slate-100 transition-colors">
                                                {tag}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};
