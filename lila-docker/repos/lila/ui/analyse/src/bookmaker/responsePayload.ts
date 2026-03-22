import type {
  AuthorEvidenceSummary,
  AuthorQuestionSummary,
  EndgameStateToken,
  EvalVariation,
  LatentPlanNarrative,
  PlanHypothesis,
  PlanStateToken,
  ProbeRequest,
  StrategicPlanExperiment,
} from './types';

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
  strategyCoverage?: StrategyCoverageMetaV1 | null;
};

export type StrategyCoverageMetaV1 = {
  mode: string;
  enforced: boolean;
  threshold: number;
  availableCategories: number;
  coveredCategories: number;
  requiredCategories: number;
  coverageScore: number;
  passesThreshold: boolean;
  planSignals: number;
  planHits: number;
  routeSignals: number;
  routeHits: number;
  focusSignals: number;
  focusHits: number;
};

import type {
  NarrativeSignalDigest,
  StrategicIdeaGroup,
  StrategicIdeaKind,
} from '../chesstory/signalTypes';
export type {
  DecisionComparisonDigest,
  NarrativeSignalDigest,
  StrategicIdeaGroup,
  StrategicIdeaKind,
} from '../chesstory/signalTypes';

export type BookmakerLedgerLineV1 = {
  title: string;
  sanMoves: string[];
  scoreCp?: number | null;
  mate?: number | null;
  note?: string | null;
  source: 'probe' | 'decision_compare' | 'variation' | 'authoring';
};

export type BookmakerStrategicLedgerV1 = {
  schema: 'chesstory.bookmaker.ledger.v1';
  motifKey: string;
  motifLabel: string;
  stageKey: string;
  stageLabel: string;
  carryOver: boolean;
  stageReason?: string | null;
  prerequisites: string[];
  conversionTrigger?: string | null;
  primaryLine?: BookmakerLedgerLineV1 | null;
  resourceLine?: BookmakerLedgerLineV1 | null;
};

export type StrategyPieceRouteV1 = {
  ownerSide: string;
  piece: string;
  from: string;
  route: string[];
  purpose: string;
  strategicFit: number;
  tacticalSafety: number;
  surfaceConfidence: number;
  surfaceMode: string;
  evidence?: string[];
};

export type StrategyPieceMoveRefV1 = {
  ownerSide: string;
  piece: string;
  from: string;
  target: string;
  idea: string;
  tacticalTheme?: string | null;
  evidence?: string[];
};

export type StrategyDirectionalTargetV1 = {
  targetId: string;
  ownerSide: string;
  piece: string;
  from: string;
  targetSquare: string;
  readiness: string;
  strategicReasons?: string[];
  prerequisites?: string[];
  evidence?: string[];
};

export type StrategyIdeaSignalV1 = {
  ideaId: string;
  ownerSide: string;
  kind: StrategicIdeaKind | string;
  group: StrategicIdeaGroup | string;
  readiness: string;
  focusSquares?: string[];
  focusFiles?: string[];
  focusDiagonals?: string[];
  focusZone?: string | null;
  beneficiaryPieces?: string[];
  confidence: number;
  evidenceRefs?: string[];
};

export type StrategyPackV1 = {
  schema: string;
  sideToMove: string;
  strategicIdeas: StrategyIdeaSignalV1[];
  pieceRoutes: StrategyPieceRouteV1[];
  pieceMoveRefs: StrategyPieceMoveRefV1[];
  directionalTargets: StrategyDirectionalTargetV1[];
  longTermFocus: string[];
  signalDigest?: NarrativeSignalDigest | null;
};

export type DecodedBookmakerResponse = {
  html: string;
  commentary: string;
  sourceMode: string | null;
  model: string | null;
  cacheHit: boolean | null;
  refs: BookmakerRefsV1 | null;
  polishMeta: PolishMetaV1 | null;
  bookmakerLedger: BookmakerStrategicLedgerV1 | null;
  strategyPack: StrategyPackV1 | null;
  signalDigest: NarrativeSignalDigest | null;
  mainStrategicPlans: PlanHypothesis[];
  strategicPlanExperiments: StrategicPlanExperiment[];
  latentPlans: LatentPlanNarrative[];
  holdReasons: string[];
  probeRequests: ProbeRequest[];
  authorQuestions: AuthorQuestionSummary[];
  authorEvidence: AuthorEvidenceSummary[];
  planStateToken: PlanStateToken | null;
  endgameStateToken: EndgameStateToken | null;
};

type DecodeBookmakerResponseFallbacks = {
  html?: string;
  commentary?: string;
  probeRequests?: ProbeRequest[];
  authorQuestions?: AuthorQuestionSummary[];
  authorEvidence?: AuthorEvidenceSummary[];
};

export type MaybeResponse = {
  html?: unknown;
  commentary?: unknown;
  variations?: unknown;
  probeRequests?: unknown;
  authorQuestions?: unknown;
  authorEvidence?: unknown;
  mainStrategicPlans?: unknown;
  strategicPlanExperiments?: unknown;
  latentPlans?: unknown;
  whyAbsentFromTopMultiPV?: unknown;
  planStateToken?: unknown;
  endgameStateToken?: unknown;
  sourceMode?: unknown;
  model?: unknown;
  cacheHit?: unknown;
  signalDigest?: unknown;
  bookmakerLedger?: unknown;
  strategyPack?: unknown;
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

export function authorQuestionsFromResponse(data: MaybeResponse): AuthorQuestionSummary[] {
  return Array.isArray(data?.authorQuestions) ? (data.authorQuestions as AuthorQuestionSummary[]) : [];
}

export function authorEvidenceFromResponse(data: MaybeResponse): AuthorEvidenceSummary[] {
  return Array.isArray(data?.authorEvidence) ? (data.authorEvidence as AuthorEvidenceSummary[]) : [];
}

export function mainStrategicPlansFromResponse(data: MaybeResponse): PlanHypothesis[] {
  return Array.isArray(data?.mainStrategicPlans) ? (data.mainStrategicPlans as PlanHypothesis[]) : [];
}

function strategicPlanExperimentFromUnknown(raw: unknown): StrategicPlanExperiment | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.planId !== 'string' || typeof raw.themeL1 !== 'string' || typeof raw.evidenceTier !== 'string')
    return null;
  return {
    planId: raw.planId,
    themeL1: raw.themeL1,
    subplanId: typeof raw.subplanId === 'string' ? raw.subplanId : null,
    evidenceTier: raw.evidenceTier,
    supportProbeCount: typeof raw.supportProbeCount === 'number' ? raw.supportProbeCount : 0,
    refuteProbeCount: typeof raw.refuteProbeCount === 'number' ? raw.refuteProbeCount : 0,
    bestReplyStable: raw.bestReplyStable === true,
    futureSnapshotAligned: raw.futureSnapshotAligned === true,
    counterBreakNeutralized: raw.counterBreakNeutralized === true,
    moveOrderSensitive: raw.moveOrderSensitive === true,
    experimentConfidence: typeof raw.experimentConfidence === 'number' ? raw.experimentConfidence : 0,
  };
}

export function strategicPlanExperimentsFromResponse(data: MaybeResponse): StrategicPlanExperiment[] {
  return Array.isArray(data?.strategicPlanExperiments)
    ? (data.strategicPlanExperiments as unknown[])
        .map(strategicPlanExperimentFromUnknown)
        .filter((value): value is StrategicPlanExperiment => !!value)
    : [];
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

export function endgameStateTokenFromResponse(data: MaybeResponse): EndgameStateToken | null {
  return data?.endgameStateToken && typeof data.endgameStateToken === 'object'
    ? (data.endgameStateToken as EndgameStateToken)
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

export function signalDigestFromResponse(data: MaybeResponse): NarrativeSignalDigest | null {
  return isRecord(data?.signalDigest) ? (data.signalDigest as NarrativeSignalDigest) : null;
}

export function strategyPackFromResponse(data: MaybeResponse): StrategyPackV1 | null {
  return isRecord(data?.strategyPack) ? (data.strategyPack as StrategyPackV1) : null;
}

function fallbackList<T>(primary: T[], fallback?: T[]): T[] {
  return primary.length ? primary : fallback || [];
}

export function decodeBookmakerResponse(
  data: MaybeResponse,
  fallbacks: DecodeBookmakerResponseFallbacks = {},
): DecodedBookmakerResponse {
  return {
    html: htmlFromResponse(data, fallbacks.html || ''),
    commentary: commentaryFromResponse(data, fallbacks.commentary || ''),
    sourceMode: sourceModeFromResponse(data),
    model: modelFromResponse(data),
    cacheHit: cacheHitFromResponse(data),
    refs: refsFromResponse(data),
    polishMeta: polishMetaFromResponse(data),
    bookmakerLedger: bookmakerLedgerFromResponse(data),
    strategyPack: strategyPackFromResponse(data),
    signalDigest: signalDigestFromResponse(data),
    mainStrategicPlans: mainStrategicPlansFromResponse(data),
    strategicPlanExperiments: strategicPlanExperimentsFromResponse(data),
    latentPlans: latentPlansFromResponse(data),
    holdReasons: whyAbsentFromTopMultiPVFromResponse(data),
    probeRequests: fallbackList(probeRequestsFromResponse(data), fallbacks.probeRequests),
    authorQuestions: fallbackList(authorQuestionsFromResponse(data), fallbacks.authorQuestions),
    authorEvidence: fallbackList(authorEvidenceFromResponse(data), fallbacks.authorEvidence),
    planStateToken: planStateTokenFromResponse(data),
    endgameStateToken: endgameStateTokenFromResponse(data),
  };
}

function ledgerLineFromUnknown(raw: unknown): BookmakerLedgerLineV1 | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.title !== 'string' || typeof raw.source !== 'string' || !Array.isArray(raw.sanMoves)) return null;
  const sanMoves = raw.sanMoves.filter((value): value is string => typeof value === 'string');
  if (sanMoves.length !== raw.sanMoves.length) return null;
  if (!['probe', 'decision_compare', 'variation', 'authoring'].includes(raw.source)) return null;
  return {
    title: raw.title,
    sanMoves,
    scoreCp: typeof raw.scoreCp === 'number' ? raw.scoreCp : null,
    mate: typeof raw.mate === 'number' ? raw.mate : null,
    note: typeof raw.note === 'string' ? raw.note : null,
    source: raw.source as BookmakerLedgerLineV1['source'],
  };
}

export function bookmakerLedgerFromResponse(data: MaybeResponse): BookmakerStrategicLedgerV1 | null {
  const raw = data?.bookmakerLedger;
  if (!isRecord(raw)) return null;
  if (raw.schema !== 'chesstory.bookmaker.ledger.v1') return null;
  if (
    typeof raw.motifKey !== 'string' ||
    typeof raw.motifLabel !== 'string' ||
    typeof raw.stageKey !== 'string' ||
    typeof raw.stageLabel !== 'string' ||
    typeof raw.carryOver !== 'boolean' ||
    !Array.isArray(raw.prerequisites)
  )
    return null;
  const prerequisites = raw.prerequisites.filter((value): value is string => typeof value === 'string');
  if (prerequisites.length !== raw.prerequisites.length) return null;
  const primaryLine = ledgerLineFromUnknown(raw.primaryLine);
  const resourceLine = ledgerLineFromUnknown(raw.resourceLine);
  if (raw.primaryLine != null && !primaryLine) return null;
  if (raw.resourceLine != null && !resourceLine) return null;
  return {
    schema: 'chesstory.bookmaker.ledger.v1',
    motifKey: raw.motifKey,
    motifLabel: raw.motifLabel,
    stageKey: raw.stageKey,
    stageLabel: raw.stageLabel,
    carryOver: raw.carryOver,
    stageReason: typeof raw.stageReason === 'string' ? raw.stageReason : null,
    prerequisites,
    conversionTrigger: typeof raw.conversionTrigger === 'string' ? raw.conversionTrigger : null,
    primaryLine,
    resourceLine,
  };
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
  const strategyCoverage = parseStrategyCoverage(raw.strategyCoverage);

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
    strategyCoverage,
  };
}

function parseStrategyCoverage(raw: unknown): StrategyCoverageMetaV1 | null {
  if (!isRecord(raw)) return null;
  if (
    typeof raw.mode !== 'string' ||
    typeof raw.enforced !== 'boolean' ||
    typeof raw.threshold !== 'number' ||
    typeof raw.availableCategories !== 'number' ||
    typeof raw.coveredCategories !== 'number' ||
    typeof raw.requiredCategories !== 'number' ||
    typeof raw.coverageScore !== 'number' ||
    typeof raw.passesThreshold !== 'boolean' ||
    typeof raw.planSignals !== 'number' ||
    typeof raw.planHits !== 'number' ||
    typeof raw.routeSignals !== 'number' ||
    typeof raw.routeHits !== 'number' ||
    typeof raw.focusSignals !== 'number' ||
    typeof raw.focusHits !== 'number'
  )
    return null;

  return {
    mode: raw.mode,
    enforced: raw.enforced,
    threshold: raw.threshold,
    availableCategories: raw.availableCategories,
    coveredCategories: raw.coveredCategories,
    requiredCategories: raw.requiredCategories,
    coverageScore: raw.coverageScore,
    passesThreshold: raw.passesThreshold,
    planSignals: raw.planSignals,
    planHits: raw.planHits,
    routeSignals: raw.routeSignals,
    routeHits: raw.routeHits,
    focusSignals: raw.focusSignals,
    focusHits: raw.focusHits,
  };
}

export function ratelimitSecondsFromResponse(data: MaybeResponse): number | null {
  const seconds = data?.ratelimit?.seconds;
  return typeof seconds === 'number' ? seconds : null;
}

export function resetAtFromResponse(data: MaybeResponse): string {
  return typeof data?.resetAt === 'string' ? data.resetAt : 'Unknown';
}
