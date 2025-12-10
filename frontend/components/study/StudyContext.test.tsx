import React from 'react';
import { render, screen, act, renderHook } from '@testing-library/react';
import { StudyProvider, useStudy } from './StudyContext';
import { StudyModel, SectionType } from '../../types/StudyModel';

// Mock Next.js router
jest.mock('next/navigation', () => ({
    useRouter: () => ({
        push: jest.fn(),
        replace: jest.fn(),
    }),
    useSearchParams: () => new URLSearchParams(),
}));

// Mock Data
const mockStudy: StudyModel = {
    schemaVersion: 3,
    createdAt: "2024-01-01",
    jobId: "test-job",
    pgn: "1. e4 e5",
    timeline: [],
    book: {
        gameMeta: { white: "Hero", black: "Villain", result: "1-0" },
        sections: [
            {
                title: "Intro",
                sectionType: "OpeningPortrait" as SectionType,
                startPly: 0,
                endPly: 10,
                diagrams: [],
                narrativeHint: "Intro"
            },
            {
                title: "Middle",
                sectionType: "CriticalCrisis" as SectionType,
                startPly: 11,
                endPly: 20,
                diagrams: [],
                narrativeHint: "Middle"
            }
        ],
        turningPoints: [],
        tacticalMoments: [],
        checklist: []
    }
};

const TestComponent = () => {
    const { currentChapterIndex, currentPly, setChapter, setPly } = useStudy();
    return (
        <div>
            <span data-testid="chapter">{currentChapterIndex}</span>
            <span data-testid="ply">{currentPly}</span>
            <button onClick={() => setChapter(1)}>Go Ch1</button>
            <button onClick={() => setPly(15)}>Go Ply15</button>
        </div>
    );
};

describe('StudyContext Logic', () => {
    test('initializes correctly (default 0)', () => {
        render(
            <StudyProvider study={mockStudy}>
                <TestComponent />
            </StudyProvider>
        );
        expect(screen.getByTestId('chapter')).toHaveTextContent('0');
        expect(screen.getByTestId('ply')).toHaveTextContent('0');
    });

    test('setChapter updates chapter and ply to section start', () => {
        render(
            <StudyProvider study={mockStudy}>
                <TestComponent />
            </StudyProvider>
        );

        act(() => {
            screen.getByText('Go Ch1').click();
        });

        expect(screen.getByTestId('chapter')).toHaveTextContent('1');
        expect(screen.getByTestId('ply')).toHaveTextContent('11'); // Ch1 startPly
    });

    test('setPly triggers chapter auto-switch if enabled', () => {
        render(
            <StudyProvider study={mockStudy}>
                <TestComponent />
            </StudyProvider>
        );

        // Start at Ch0
        expect(screen.getByTestId('chapter')).toHaveTextContent('0');

        act(() => {
            screen.getByText('Go Ply15').click();
        });

        // Ply 15 is in Ch1 (11-20)
        expect(screen.getByTestId('ply')).toHaveTextContent('15');
        expect(screen.getByTestId('chapter')).toHaveTextContent('1');
    });
});
