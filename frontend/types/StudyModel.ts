import { TimelineNode } from './review';

export type SectionType =
    | "OpeningPortrait"
    | "CriticalCrisis"
    | "StructuralDeepDive"
    | "TacticalStorm"
    | "EndgameMasterclass"
    | "NarrativeBridge";

export type MistakeTag = "TacticalMiss" | "Blunder" | "PrematurePawnPush" | "PassiveMove" | "PositionalTradeError" | "MissedCentralBreak";
export type StructureTag = string; // e.g. "SpaceAdvantageWhite" - we can be loose or exhaustive here
export type PlanTag = string;
export type TacticTag = string;
export type EndgameTag = string;
export type TransitionTag = string;

export interface TagBundle {
    structure: StructureTag[];
    plan: PlanTag[];
    tactic: TacticTag[];
    mistake: MistakeTag[];
    endgame: EndgameTag[];
    transition: TransitionTag[];
}

export interface BookDiagram {
    id: string;
    fen: string;
    roles: string[];
    ply: number;
    tags: TagBundle;
}

export interface SectionMetadata {
    theme: string;
    atmosphere: string;
    context: Record<string, string>;
}

export interface BookSection {
    title: string;
    sectionType: SectionType;
    diagrams: BookDiagram[];
    narrativeHint: string;
    startPly: number;
    endPly: number;
    metadata?: SectionMetadata;
}

export interface BookTurningPoint {
    ply: number;
    side: "White" | "Black" | string;
    playedMove: string;
    bestMove: string;
    evalBefore: number;
    evalAfterPlayed: number;
    evalAfterBest: number;
    mistakeTags: MistakeTag[];
}

export interface BookTacticalMoment {
    ply: number;
    side: string;
    motifTags: TacticTag[];
    evalGainIfPlayed?: number;
    wasMissed: boolean;
}

export interface ChecklistBlock {
    category: string;
    hintTags: string[];
}

export interface GameMeta {
    white: string;
    black: string;
    result: string;
    openingName?: string;
}

export interface Book {
    gameMeta: GameMeta;
    sections: BookSection[];
    turningPoints: BookTurningPoint[];
    tacticalMoments: BookTacticalMoment[];
    checklist: ChecklistBlock[];
}

export interface EngineInfo {
    name: string;
    depth: string;
}

// Mirrors AnalyzePgn.Output + Meta fields
export interface StudyModel {
    schemaVersion: number;
    createdAt: string;
    jobId: string; // Added for frontend ref
    engineInfo?: EngineInfo;

    // From AnalyzePgn.Output
    pgn: string;
    root?: any; // Tree root, likely not heavily used in Book Mode vs timeline

    // Flattened/Processed Timeline? Or raw from API?
    // We'll trust the API structure for now.
    timeline: TimelineNode[];

    // The Star of the Show
    book: Book;

    // Other summary fields
    summaryText?: string;
}
