import React from 'react';
import { SectionMetadata } from '../../../../types/StudyModel';
import { Layout, Cloud, ListChecks } from 'lucide-react';

interface KeyPointsTableProps {
    metadata: SectionMetadata;
}

export function KeyPointsTable({ metadata }: KeyPointsTableProps) {
    if (!metadata) return null;

    return (
        <div className="mb-8 overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-sm">
            <div className="grid divide-y divide-white/10 sm:grid-cols-3 sm:divide-x sm:divide-y-0">

                {/* Theme */}
                <div className="p-4">
                    <div className="flex items-center gap-2 mb-2 text-blue-400">
                        <Layout size={18} />
                        <span className="text-xs font-bold uppercase tracking-wider">Theme</span>
                    </div>
                    <div className="text-sm font-medium text-white/90 leading-relaxed">
                        {metadata.theme}
                    </div>
                </div>

                {/* Atmosphere */}
                <div className="p-4">
                    <div className="flex items-center gap-2 mb-2 text-purple-400">
                        <Cloud size={18} />
                        <span className="text-xs font-bold uppercase tracking-wider">Atmosphere</span>
                    </div>
                    <div className="text-sm font-medium text-white/90 leading-relaxed">
                        {metadata.atmosphere}
                    </div>
                </div>

                {/* Context (First 2 items) */}
                <div className="p-4">
                    <div className="flex items-center gap-2 mb-2 text-emerald-400">
                        <ListChecks size={18} />
                        <span className="text-xs font-bold uppercase tracking-wider">Context</span>
                    </div>
                    <div className="space-y-1.5">
                        {Object.entries(metadata.context).slice(0, 3).map(([key, value]) => (
                            <div key={key} className="flex items-start justify-between text-sm">
                                <span className="text-white/50">{key}</span>
                                <span className="font-medium text-white/90 text-right">{value}</span>
                            </div>
                        ))}
                    </div>
                </div>

            </div>
        </div>
    );
}
