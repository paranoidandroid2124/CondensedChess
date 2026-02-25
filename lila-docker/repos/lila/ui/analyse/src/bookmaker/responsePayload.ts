import type { EvalVariation, LatentPlanNarrative, PlanHypothesis, PlanStateToken, ProbeRequest } from './types';

export type MoveRefV1 = {
  refId: string;
  san: string;
  uci: string;
  fenAfter: string;
  ply: number;
  moveNo: number;
  marker?: string | null;
};

export type VariationRefV1 = {
  lineId: string;
  scoreCp: number;
  mate?: number | null;
  depth: number;
  moves: MoveRefV1[];
};

export type BookmakerRefsV1 = {
  schema: 'chesstory.refs.v1';
  startFen: string;
  startPly: number;
  variations: VariationRefV1[];
};

export type PolishMetaV1 = {
  provider: string;
  model?: string | null;
  sourceMode: string;
  validationPhase: string;
  validationReasons: string[];
  cacheHit: boolean;
  promptTokens?: number | null;
  cachedTokens?: number | null;
  completionTokens?: number | null;
  estimatedCostUsd?: number | null;
};

type MaybeResponse = {
  html?: unknown;
  commentary?: unknown;
  variations?: unknown;
  probeRequests?: unknown;
  mainStrategicPlans?: unknown;
  latentPlans?: unknown;
  whyAbsentFromTopMultiPV?: unknown;
  planStateToken?: unknown;
  sourceMode?: unknown;
  model?: unknown;
  cacheHit?: unknown;
  refs?: unknown;
  polishMeta?: unknown;
  ratelimit?: {
    seconds?: unknown;
  };
  resetAt?: unknown;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

export function htmlFromResponse(data: MaybeResponse, fallback = ''): string {
  return typeof data?.html === 'string' ? data.html : fallback;
}

export function commentaryFromResponse(data: MaybeResponse, fallback = ''): string {
  return typeof data?.commentary === 'string' ? data.commentary : fallback;
}

export function variationLinesFromResponse(data: MaybeResponse, fallback: EvalVariation[] | null): any[] {
  return Array.isArray(data?.variations) ? (data.variations as any[]) : fallback || [];
}

export function probeRequestsFromResponse(data: MaybeResponse): ProbeRequest[] {
  return Array.isArray(data?.probeRequests) ? (data.probeRequests as ProbeRequest[]) : [];
}

export function mainStrategicPlansFromResponse(data: MaybeResponse): PlanHypothesis[] {
  return Array.isArray(data?.mainStrategicPlans) ? (data.mainStrategicPlans as PlanHypothesis[]) : [];
}

export function latentPlansFromResponse(data: MaybeResponse): LatentPlanNarrative[] {
  return Array.isArray(data?.latentPlans) ? (data.latentPlans as LatentPlanNarrative[]) : [];
}

export function whyAbsentFromTopMultiPVFromResponse(data: MaybeResponse): string[] {
  return Array.isArray(data?.whyAbsentFromTopMultiPV)
    ? (data.whyAbsentFromTopMultiPV as unknown[]).filter((v): v is string => typeof v === 'string')
    : [];
}

export function planStateTokenFromResponse(data: MaybeResponse): PlanStateToken | null {
  return data?.planStateToken && typeof data.planStateToken === 'object'
    ? (data.planStateToken as PlanStateToken)
    : null;
}

export function sourceModeFromResponse(data: MaybeResponse): string | null {
  return typeof data?.sourceMode === 'string' ? data.sourceMode : null;
}

export function modelFromResponse(data: MaybeResponse): string | null {
  return typeof data?.model === 'string' ? data.model : null;
}

export function cacheHitFromResponse(data: MaybeResponse): boolean | null {
  return typeof data?.cacheHit === 'boolean' ? data.cacheHit : null;
}

export function refsFromResponse(data: MaybeResponse): BookmakerRefsV1 | null {
  const raw = data?.refs;
  if (!isRecord(raw)) return null;
  if (raw.schema !== 'chesstory.refs.v1') return null;
  if (typeof raw.startFen !== 'string' || typeof raw.startPly !== 'number' || !Array.isArray(raw.variations)) return null;

  const variations: VariationRefV1[] = [];
  for (const line of raw.variations) {
    if (!isRecord(line)) return null;
    if (
      typeof line.lineId !== 'string' ||
      typeof line.scoreCp !== 'number' ||
      typeof line.depth !== 'number' ||
      !Array.isArray(line.moves)
    )
      return null;

    const moves: MoveRefV1[] = [];
    for (const move of line.moves) {
      if (!isRecord(move)) return null;
      if (
        typeof move.refId !== 'string' ||
        typeof move.san !== 'string' ||
        typeof move.uci !== 'string' ||
        typeof move.fenAfter !== 'string' ||
        typeof move.ply !== 'number' ||
        typeof move.moveNo !== 'number'
      )
        return null;
      moves.push({
        refId: move.refId,
        san: move.san,
        uci: move.uci,
        fenAfter: move.fenAfter,
        ply: move.ply,
        moveNo: move.moveNo,
        marker: typeof move.marker === 'string' ? move.marker : null,
      });
    }

    variations.push({
      lineId: line.lineId,
      scoreCp: line.scoreCp,
      mate: typeof line.mate === 'number' ? line.mate : null,
      depth: line.depth,
      moves,
    });
  }

  return {
    schema: 'chesstory.refs.v1',
    startFen: raw.startFen,
    startPly: raw.startPly,
    variations,
  };
}

export function polishMetaFromResponse(data: MaybeResponse): PolishMetaV1 | null {
  const raw = data?.polishMeta;
  if (!isRecord(raw)) return null;
  if (
    typeof raw.provider !== 'string' ||
    typeof raw.sourceMode !== 'string' ||
    typeof raw.validationPhase !== 'string' ||
    typeof raw.cacheHit !== 'boolean' ||
    !Array.isArray(raw.validationReasons)
  )
    return null;

  const validationReasons = raw.validationReasons.filter((v): v is string => typeof v === 'string');
  if (validationReasons.length !== raw.validationReasons.length) return null;

  return {
    provider: raw.provider,
    model: typeof raw.model === 'string' ? raw.model : null,
    sourceMode: raw.sourceMode,
    validationPhase: raw.validationPhase,
    validationReasons,
    cacheHit: raw.cacheHit,
    promptTokens: typeof raw.promptTokens === 'number' ? raw.promptTokens : null,
    cachedTokens: typeof raw.cachedTokens === 'number' ? raw.cachedTokens : null,
    completionTokens: typeof raw.completionTokens === 'number' ? raw.completionTokens : null,
    estimatedCostUsd: typeof raw.estimatedCostUsd === 'number' ? raw.estimatedCostUsd : null,
  };
}

export function ratelimitSecondsFromResponse(data: MaybeResponse): number | null {
  const seconds = data?.ratelimit?.seconds;
  return typeof seconds === 'number' ? seconds : null;
}

export function resetAtFromResponse(data: MaybeResponse): string {
  return typeof data?.resetAt === 'string' ? data.resetAt : 'Unknown';
}
