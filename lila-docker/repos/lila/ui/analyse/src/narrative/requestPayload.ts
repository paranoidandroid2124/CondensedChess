import type { ProbeResultsByPlyEntry } from './probePlanning';

export type FullAnalysisRequestPayload = {
  pgn: string;
  evals: any[];
  options: {
    style: string;
    focusOn: string[];
  };
  probeResultsByPly?: ProbeResultsByPlyEntry[] | null;
  variant?: string;
};

type BuildFullAnalysisRequestPayloadInput = {
  pgn: string;
  evals: any[];
  variant: string;
  probeResultsByPly?: ProbeResultsByPlyEntry[] | null;
};

export function buildFullAnalysisRequestPayload(input: BuildFullAnalysisRequestPayloadInput): FullAnalysisRequestPayload {
  const { pgn, evals, variant, probeResultsByPly = null } = input;
  return {
    pgn,
    evals,
    options: { style: 'book', focusOn: ['mistakes', 'turning_points'] },
    probeResultsByPly,
    variant,
  };
}
