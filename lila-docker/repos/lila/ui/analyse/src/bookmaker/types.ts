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
