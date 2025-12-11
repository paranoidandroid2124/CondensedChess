import React from 'react';
import { BookSection, SectionType } from '../../../types/StudyModel';

interface Props {
    sections: BookSection[];
    activeSectionIndex: number;
    onSelect: (index: number) => void;
}

// Icons mapping for SectionType
const getSectionIcon = (type: SectionType) => {
    switch (type) {
        case 'OpeningReview': return '📖';
        case 'TurningPoints': return '🔥';
        case 'MiddlegamePlans': return '🏛️';
        case 'TacticalStorm':
        case 'TacticalMoments': return '⚡';
        case 'EndgameLessons': return '🎓';
        case 'TitleSummary': return '📋';
        case 'KeyDiagrams': return '🖼️';
        case 'FinalChecklist': return '✅';
        default: return '📄';
    }
};

const getSectionLabel = (type: SectionType) => {
    switch (type) {
        case 'OpeningReview': return 'Opening';
        case 'TurningPoints': return 'Critical Moment';
        case 'MiddlegamePlans': return 'Strategy';
        case 'TacticalStorm':
        case 'TacticalMoments': return 'Tactics';
        case 'EndgameLessons': return 'Endgame';
        case 'TitleSummary': return 'Summary';
        case 'KeyDiagrams': return 'Diagrams';
        case 'FinalChecklist': return 'Checklist';
        default: return 'Chapter';
    }
};

export const ChapterNavigation: React.FC<Props> = ({ sections, activeSectionIndex, onSelect }) => {
    return (
        <div className="flex flex-col h-full bg-slate-900 border-r border-slate-700 overflow-y-auto w-64">
            <div className="p-4 border-b border-slate-700 font-serif text-lg font-bold text-slate-100">
                Table of Contents
            </div>
            <div className="flex-1 p-2 space-y-1">
                {sections.map((section, idx) => {
                    const isActive = idx === activeSectionIndex;
                    return (
                        <button
                            key={idx}
                            onClick={() => onSelect(idx)}
                            className={`w-full text-left p-3 rounded-lg transition-all duration-200 border border-transparent
                ${isActive
                                    ? 'bg-blue-900/40 border-blue-500/50 text-blue-100 shadow-md'
                                    : 'hover:bg-slate-800 text-slate-400 hover:text-slate-200'}
              `}
                        >
                            <div className="flex items-center gap-3">
                                <span className="text-xl" role="img" aria-label={section.sectionType}>
                                    {getSectionIcon(section.sectionType)}
                                </span>
                                <div className="flex flex-col overflow-hidden">
                                    <span className="text-xs uppercase tracking-wider font-semibold opacity-70">
                                        {getSectionLabel(section.sectionType)}
                                    </span>
                                    <span className="truncate font-medium text-sm leading-tight" title={section.title}>
                                        {section.title}
                                    </span>
                                </div>
                            </div>
                        </button>
                    );
                })}
            </div>
        </div>
    );
};
