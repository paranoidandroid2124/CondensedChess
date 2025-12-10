import type { Book } from "./StudyModel";
export type { Book };

export type Judgement = "best" | "excellent" | "good" | "inaccuracy" | "mistake" | "blunder" | "book";

export interface Opening {
  name?: string;
  eco?: string;
  ply?: number;
}

export interface OpeningTopMove {
  san: string;
  uci: string;
  games?: number;
  winPct?: number; // percent if present
  drawPct?: number; // percent if present
}

export interface OpeningTopGame {
  white: string;
  black: string;
  whiteElo?: number;
  blackElo?: number;
  result: string;
  date?: string;
  event?: string;
}

export interface OpeningStats {
  bookPly: number;
  noveltyPly: number;
  games?: number;
  freq?: number; // percent if present
  winWhite?: number; // percent if present
  winBlack?: number; // percent if present
  draw?: number; // percent if present
  minYear?: number;
  maxYear?: number;
  yearBuckets?: Record<string, number>;
  topMoves?: OpeningTopMove[];
  topGames?: OpeningTopGame[];
  source?: string;
}

export interface Branch {
  move: string;
  winPct: number;
  label: string;
  pv: string[];
}

export interface CriticalNode {
  ply: number;
  reason: string;
  deltaWinPct: number;
  branches: Branch[];
  mistakeCategory?: string;
  tags?: string[];
  comment?: string; // LLM long form
  bestVsSecondGap?: number;
  bestVsPlayedGap?: number;
  legalMoves?: number;
  forced?: boolean;
  phaseLabel?: string;
  semanticTags?: string[];
  practicality?: PracticalityScore;
  opponentRobustness?: number;
  isPressurePoint?: boolean;
}

export interface EvalLine {
  move: string;
  winPct: number;
  cp?: number;
  pv: string[];
}

export interface EngineEval {
  depth: number;
  lines: EvalLine[];
}

export interface Features {
  pawnIslands: number;
  isolatedPawns: number;
  doubledPawns: number;
  passedPawns: number;
  rookOpenFiles: number;
  rookSemiOpenFiles: number;
  bishopPair: boolean;
  kingRingPressure: number;
  spaceControl: number;
}

export interface Concepts {
  dynamic?: number;
  drawish?: number;
  imbalanced?: number;
  tacticalDepth?: number;
  blunderRisk?: number;
  pawnStorm?: number;
  fortress?: number;
  colorComplex?: number;
  badBishop?: number;
  goodKnight?: number;
  rookActivity?: number;
  kingSafety?: number;
  dry?: number;
  comfortable?: number;
  unpleasant?: number;
  engineLike?: number;
  conversionDifficulty?: number;
  sacrificeQuality?: number;
  alphaZeroStyle?: number;
}

export interface PracticalityScore {
  overall: number;
  robustness: number;
  horizon: number;
  naturalness: number;
  categoryGlobal: string;
  categoryPersonal?: string;
  // Deprecated: kept for backward compatibility if needed, but prefer categoryGlobal
  category: string;
}

export interface TimelineNode {
  ply: number;
  turn: "white" | "black";
  san: string;
  uci: string;
  fen: string;
  fenBefore?: string;
  features: Features;
  evalBeforeShallow?: EngineEval;
  evalBeforeDeep?: EngineEval;
  winPctBefore?: number;
  winPctAfterForPlayer?: number;
  deltaWinPct?: number;
  epBefore?: number;
  epAfter?: number;
  epLoss?: number;
  judgement?: Judgement;
  special?: "brilliant" | "great";
  mistakeCategory?: string;
  conceptDelta?: Concepts;
  semanticTags?: string[];
  concepts?: Concepts;
  conceptsBefore?: Concepts;
  isCustom?: boolean;
  bestVsSecondGap?: number;
  bestVsPlayedGap?: number;
  legalMoves?: number;
  forced?: boolean;
  phaseLabel?: string;
  label?: string;
  shortComment?: string; // LLM short comment
  studyTags?: string[];
  studyScore?: number;
  practicality?: PracticalityScore;
  phase?: string; // "opening" | "middlegame" | "endgame"
}

export interface Review {
  opening?: Opening;
  openingStats?: OpeningStats;
  oppositeColorBishops?: boolean;
  openingSummary?: string;
  bookExitComment?: string;
  openingTrend?: string;
  critical: CriticalNode[];
  timeline: TimelineNode[];
  summaryText?: string; // LLM game summary
  root?: ReviewTreeNode;
  studyChapters?: StudyChapter[];
  jobId?: string;
  pgn?: string;
  accuracyWhite?: number;
  accuracyBlack?: number;
  book?: Book; // Phase 4.6 Book JSON
}



export interface ReviewTreeNode {
  ply: number;
  san: string;
  uci: string;
  fen: string;
  eval: number;
  evalType: string;
  judgement: string;
  glyph: string;
  tags: string[];
  bestMove?: string;
  bestEval?: number;
  pv: string[];
  comment?: string;
  children: ReviewTreeNode[];
  nodeType?: string; // "mainline" | "critical" | "sideline" | "hypothesis"
  concepts?: Concepts;
  features?: Features;
}

export interface StudyLine {
  label: string;
  pv: string[];
  winPct: number;
}

// Copied from BookModel.ts structure effectively
export interface TagBundle {
  structure: string[];
  plan: string[];
  tactic: string[];
  mistake: string[];
  endgame: string[];
  transition: string[];
}

export interface BookDiagram {
  id: string;
  fen: string;
  roles: string[];
  ply: number;
  tags: TagBundle;
}

export interface StudyChapter {
  id: string;
  anchorPly: number;
  fen: string;
  played: string;
  best?: string;
  deltaWinPct: number;
  tags: string[];
  lines: StudyLine[];
  summary?: string;
  studyScore?: number;
  practicality?: PracticalityScore;
  metadata?: {
    name: string;
    description: string;
  };
  phase?: string;
  winPctBefore?: number;
  winPctAfter?: number;
  rootNode?: ReviewTreeNode;
  variations?: any[];
  diagrams?: BookDiagram[]; // Added diagram support
}

// --- Phase 4.6 Book Types ---

// --- Phase 4.6 Book Types (Legacy types removed, using StudyModel.Book) ---
// See import at top.

