export type ProbeRequest = {
  id: string;
  fen: string;
  moves: string[];
  depth: number;
  purpose?: string;
  questionId?: string;
  questionKind?: string;
  multiPv?: number;
  baselineEvalCp?: number;
  baselineMove?: string;
  baselineMate?: number | null;
  baselineDepth?: number;
};

export type ProbeResult = {
  id: string;
  fen?: string;
  evalCp: number;
  bestReplyPv: string[];
  replyPvs?: string[][];
  deltaVsBaseline: number;
  keyMotifs: string[];
  purpose?: string;
  questionId?: string;
  questionKind?: string;
  probedMove?: string;
  mate?: number | null;
  depth?: number;
};

export type EvalVariation = {
  moves: string[];
  scoreCp: number;
  mate: number | null;
  depth: number;
};

export type PlanContinuityToken = {
  planName: string;
  planId?: string | null;
  consecutivePlies: number;
  startingPly: number;
};

export type PlanTransitionToken = {
  transitionType: string;
  momentum: number;
  primaryPlanId?: string | null;
  secondaryPlanId?: string | null;
};

export type PlanColorStateToken = {
  primary?: PlanContinuityToken | null;
  secondary?: PlanContinuityToken | null;
  lastTransition?: PlanTransitionToken | null;
  lastPly?: number | null;
};

export type PlanStateToken = {
  version?: number;
  history?: {
    white?: PlanColorStateToken | PlanContinuityToken | null;
    black?: PlanColorStateToken | PlanContinuityToken | null;
  };
};

type OpeningMove = {
  uci: string;
  san: string;
  total: number;
  white: number;
  draws: number;
  black: number;
  performance: number;
};

type OpeningPlayer = {
  name: string;
  rating: number;
};

type OpeningGame = {
  id: string;
  winner?: string | null;
  white: OpeningPlayer;
  black: OpeningPlayer;
  year: number;
  month: number;
  event?: string | null;
  pgn?: string | null;
};

export type OpeningReferencePayload = {
  eco?: string | null;
  name?: string | null;
  totalGames: number;
  topMoves: OpeningMove[];
  sampleGames: OpeningGame[];
};
