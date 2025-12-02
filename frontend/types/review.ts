export type Judgement = "best" | "excellent" | "good" | "inaccuracy" | "mistake" | "blunder";

export interface Opening {
  name?: string;
  eco?: string;
  ply?: number;
}

export interface OpeningTopMove {
  san: string;
  uci: string;
  freq?: number; // percent if present
  winPct?: number; // percent if present
}

export interface OpeningStats {
  bookPly: number;
  noveltyPly: number;
  freq?: number; // percent if present
  winWhite?: number; // percent if present
  winBlack?: number; // percent if present
  draw?: number; // percent if present
  topMoves?: OpeningTopMove[];
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
  special?: "brilliant" | "great" | "miss";
  mistakeCategory?: string;
  semanticTags?: string[];
  concepts?: Concepts;
  shortComment?: string; // LLM short comment
}

export interface Review {
  opening?: Opening;
  openingStats?: OpeningStats;
  oppositeColorBishops?: boolean;
  critical: CriticalNode[];
  timeline: TimelineNode[];
  summaryText?: string; // LLM game summary
  root?: ReviewTreeNode;
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
}
