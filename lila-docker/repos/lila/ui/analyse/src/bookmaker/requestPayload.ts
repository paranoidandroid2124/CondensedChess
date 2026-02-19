import type { EvalVariation, OpeningReferencePayload, ProbeResult } from './types';

export type EvalData = {
  cp: number;
  mate: number | null;
  pv: string[] | null;
};

type BuildBookmakerRequestInput = {
  fen: string;
  lastMove: string | null;
  variations: EvalVariation[] | null;
  probeResults: ProbeResult[] | null;
  openingData: OpeningReferencePayload | null;
  afterFen: string | null;
  afterVariations: EvalVariation[] | null;
  phase: string;
  ply: number;
};

export function toEvalData(variations: EvalVariation[] | null): EvalData | null {
  if (!variations?.length) return null;
  const lead = variations[0];
  return {
    cp: typeof lead.scoreCp === 'number' ? lead.scoreCp : 0,
    mate: lead.mate ?? null,
    pv: Array.isArray(lead.moves) ? lead.moves : null,
  };
}

export function toBaselineCp(variations: EvalVariation[] | null, evalData: EvalData | null): number {
  if (typeof variations?.[0]?.scoreCp === 'number') return variations[0].scoreCp;
  if (typeof evalData?.cp === 'number') return evalData.cp;
  return 0;
}

export function deriveAfterVariations(
  afterFen: string | null,
  afterVariations: EvalVariation[] | null,
  playedMove: string | null,
  variations: EvalVariation[] | null,
): EvalVariation[] | null {
  if (!afterFen || afterVariations?.length || !playedMove || !variations?.length) return afterVariations;
  const playedLine = variations.find(v => Array.isArray(v?.moves) && v.moves[0] === playedMove);
  if (!playedLine) return afterVariations;

  const tail = Array.isArray(playedLine.moves) ? playedLine.moves.slice(1) : [];
  return [
    {
      moves: tail.slice(0, 40),
      scoreCp: typeof playedLine.scoreCp === 'number' ? playedLine.scoreCp : 0,
      mate: typeof playedLine.mate === 'number' ? playedLine.mate : null,
      depth: typeof playedLine.depth === 'number' ? playedLine.depth : 0,
    },
  ];
}

export function buildBookmakerRequest(input: BuildBookmakerRequestInput) {
  const { fen, lastMove, variations, probeResults, openingData, afterFen, afterVariations, phase, ply } = input;
  return {
    fen,
    lastMove: lastMove || null,
    eval: toEvalData(variations),
    variations,
    probeResults,
    openingData,
    afterFen,
    afterEval: toEvalData(afterVariations),
    afterVariations,
    context: {
      opening: null,
      phase,
      ply,
    },
  };
}
