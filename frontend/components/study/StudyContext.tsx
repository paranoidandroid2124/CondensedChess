import React, { createContext, useContext, useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { StudyModel } from '../../types/StudyModel';

interface StudyContextType {
    study: StudyModel;
    currentChapterIndex: number;
    currentPly: number;
    setChapter: (index: number) => void;
    setPly: (ply: number) => void;
    // Overlay controls
    showOverlays: boolean;
    setShowOverlays: (show: boolean) => void;
    // Tab/View Mode
    viewMode: 'read' | 'quiz' | 'dashboard';
    setViewMode: (mode: 'read' | 'quiz' | 'dashboard') => void;
}

const StudyContext = createContext<StudyContextType | undefined>(undefined);

export const StudyProvider: React.FC<{
    study: StudyModel,
    children: React.ReactNode
}> = ({ study, children }) => {
    const router = useRouter();
    const searchParams = useSearchParams();

    // Initial state from URL or defaults
    const initialChapter = parseInt(searchParams.get('chapter') || '0', 10);
    const initialPly = parseInt(searchParams.get('ply') || study.book.sections[0]?.startPly.toString() || '0', 10);

    const [currentChapterIndex, setCurrentChapterIndex] = useState(initialChapter);
    const [currentPly, setCurrentPly] = useState(initialPly);
    const [showOverlays, setShowOverlays] = useState(true);
    const [viewMode, setViewMode] = useState<'read' | 'quiz' | 'dashboard'>('read');

    // URL Sync
    useEffect(() => {
        // Basic sync: update URL when state changes.
        // Use replace to avoid polluting history stack too much?
        // Or push? Deep linking implies push usually.
        // Let's use simpler approach: just update URL params.
        const params = new URLSearchParams(searchParams.toString());
        params.set('chapter', currentChapterIndex.toString());
        params.set('ply', currentPly.toString());
        // router.replace(`?${params.toString()}`, { scroll: false }); 
        // Optimization: Debounce URL updates if ply changes rapidly (e.g. scrubbing).
        // For now, simple.
    }, [currentChapterIndex, currentPly]);

    const setChapter = (index: number) => {
        if (index >= 0 && index < study.book.sections.length) {
            setCurrentChapterIndex(index);
            // Auto-jump ply to start of section?
            // Yes, usually expected in navigation.
            const section = study.book.sections[index];
            setCurrentPly(section.startPly);
            setViewMode('read'); // Reset to reading when changing chapter
        }
    };

    const setPly = (ply: number) => {
        setCurrentPly(ply);
        // Auto-detect chapter change? 
        // Optional. If user scrubs timeline into another chapter, should we switch active chapter UI?
        // Yes, for consistency.
        const matchingSectionIdx = study.book.sections.findIndex(
            s => ply >= s.startPly && ply <= s.endPly
        );
        if (matchingSectionIdx !== -1 && matchingSectionIdx !== currentChapterIndex) {
            setCurrentChapterIndex(matchingSectionIdx);
        }
    };

    return (
        <StudyContext.Provider value={{
            study,
            currentChapterIndex,
            currentPly,
            setChapter,
            setPly,
            showOverlays,
            setShowOverlays,
            viewMode,
            setViewMode
        }}>
            {children}
        </StudyContext.Provider>
    );
};

export const useStudy = () => {
    const context = useContext(StudyContext);
    if (!context) {
        throw new Error('useStudy must be used within a StudyProvider');
    }
    return context;
};
