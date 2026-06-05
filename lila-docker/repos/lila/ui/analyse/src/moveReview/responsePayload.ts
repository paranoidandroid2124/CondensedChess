import type {
  AuthorEvidenceSummary,
  AuthorQuestionSummary,
  EndgameStateToken,
  EvalVariation,
  PlanStateToken,
  ProbeRequest,
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

export type MoveReviewRefsV1 = {
  schema: 'chesstory.refs.v1';
  startFen: string;
  startPly: number;
  variations: VariationRefV1[];
};

export type MoveReviewShortLineV1 = {
  san: string[];
  uci: string[];
  lineId?: string | null;
  scoreCp?: number | null;
  mate?: number | null;
  depth?: number | null;
  source: string;
};

export type MoveReviewPvInterpretationV1 = {
  linePurpose: string;
  confirms: string[];
  tension: string;
  opponentReplyMeaning?: string | null;
  learningPoint: string;
  supportedByLineId?: string | null;
  confidence: string;
};

export type MoveReviewExplanationV1 = {
  title: string;
  prose: string;
  qualityLabel?: string | null;
  reasonTags: string[];
  shortLine?: MoveReviewShortLineV1 | null;
  pvInterpretation?: MoveReviewPvInterpretationV1 | null;
  source: string;
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

export type MoveReviewDiagnosticsV1 = {
  status: string;
  sourceModeReason: string;
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

const moveReviewLedgerLineSources = ['probe', 'decision_compare', 'variation', 'authoring'] as const;
type MoveReviewLedgerLineSource = (typeof moveReviewLedgerLineSources)[number];

export type MoveReviewLedgerLineV1 = {
  title: string;
  sanMoves: string[];
  scoreCp?: number | null;
  mate?: number | null;
  note?: string | null;
  source: MoveReviewLedgerLineSource;
};

export type MoveReviewStrategicLedgerV1 = {
  schema: 'chesstory.move_review.ledger.v1';
  motifKey: string;
  motifLabel: string;
  stageKey: string;
  stageLabel: string;
  carryOver: boolean;
  stageReason?: string | null;
  prerequisites: string[];
  conversionTrigger?: string | null;
  primaryLine?: MoveReviewLedgerLineV1 | null;
  resourceLine?: MoveReviewLedgerLineV1 | null;
};

export type MoveReviewSurfaceAuthorityV2 = {
  kind: string;
  token?: string | null;
  openingFamily?: string | null;
  target?: string | null;
  openingBook?: MoveReviewOpeningBookMetadataV2 | null;
};

export type MoveReviewOpeningBookMetadataV2 = {
  eco?: string | null;
  totalGames?: number | null;
  topMoves: string[];
};

export type MoveReviewPlayerSurfaceRowV1 = {
  label: string;
  text: string;
  tone?: string | null;
  refSans: string[];
  authority?: MoveReviewSurfaceAuthorityV2 | null;
};

export type MoveReviewDecisionTargetComparisonV1 = {
  chosenTarget: string;
  chosenTargetKind: string;
  bestTarget: string;
  bestTargetKind: string;
};

export type MoveReviewPlayerDecisionComparisonV1 = {
  kicker: string;
  gapLabel?: string | null;
  chosenSan?: string | null;
  engineSan?: string | null;
  comparedSan?: string | null;
  secondaryText?: string | null;
  chosenMatchesBest: boolean;
  targetComparison?: MoveReviewDecisionTargetComparisonV1 | null;
};

export type MoveReviewPlayerAuthorRowV1 = {
  title: string;
  status: string;
  question: string;
  why?: string | null;
  branches: MoveReviewPlayerSurfaceRowV1[];
};

export type MoveReviewPlayerSurfaceV1 = {
  schema: 'chesstory.move_review.player_surface.v1' | 'chesstory.move_review.player_surface.v2';
  title?: string | null;
  summaryRows: MoveReviewPlayerSurfaceRowV1[];
  advancedRows: MoveReviewPlayerSurfaceRowV1[];
  decisionComparison?: MoveReviewPlayerDecisionComparisonV1 | null;
  probeRows: MoveReviewPlayerSurfaceRowV1[];
  authorRows: MoveReviewPlayerAuthorRowV1[];
};

const strategicRelationAuthorityTokens = new Set([
  'defender_trade',
  'bad_piece_liquidation',
  'overload',
  'deflection',
  'discovered_attack',
  'double_check',
  'back_rank_mate',
  'mate_net',
  'greek_gift',
  'stalemate_trap',
  'perpetual_check',
  'fork',
  'hanging_piece',
  'trapped_piece',
  'domination',
  'zwischenzug',
  'xray',
  'clearance',
  'battery',
  'pin',
  'skewer',
  'interference',
  'decoy',
]);

export type DecodedMoveReviewResponse = {
  html: string;
  commentary: string;
  sourceMode: string | null;
  model: string | null;
  cacheHit: boolean | null;
  refs: MoveReviewRefsV1 | null;
  polishMeta: PolishMetaV1 | null;
  diagnostics: MoveReviewDiagnosticsV1 | null;
  moveReviewExplanation: MoveReviewExplanationV1 | null;
  moveReviewLedger: MoveReviewStrategicLedgerV1 | null;
  moveReviewPlayerSurface: MoveReviewPlayerSurfaceV1 | null;
  mainStrategicPlanCount: number;
  probeRequests: ProbeRequest[];
  authorQuestions: AuthorQuestionSummary[];
  authorEvidence: AuthorEvidenceSummary[];
  planStateToken: PlanStateToken | null;
  endgameStateToken: EndgameStateToken | null;
};

type DecodeMoveReviewResponseFallbacks = {
  html?: string;
  commentary?: string;
};

export type MaybeResponse = {
  html?: unknown;
  commentary?: unknown;
  variations?: unknown;
  mainStrategicPlanCount?: unknown;
  planStateToken?: unknown;
  endgameStateToken?: unknown;
  sourceMode?: unknown;
  model?: unknown;
  cacheHit?: unknown;
  moveReviewLedger?: unknown;
  moveReviewExplanation?: unknown;
  moveReviewPlayerSurface?: unknown;
  refs?: unknown;
  polishMeta?: unknown;
  diagnostics?: unknown;
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

export function probeRequestsFromResponse(_data: MaybeResponse): ProbeRequest[] {
  return [];
}

export function authorQuestionsFromResponse(_data: MaybeResponse): AuthorQuestionSummary[] {
  return [];
}

export function authorEvidenceFromResponse(_data: MaybeResponse): AuthorEvidenceSummary[] {
  return [];
}

export function mainStrategicPlanCountFromResponse(data: MaybeResponse): number {
  if (typeof data?.mainStrategicPlanCount === 'number' && Number.isFinite(data.mainStrategicPlanCount))
    return Math.max(0, Math.trunc(data.mainStrategicPlanCount));
  return 0;
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

export function moveReviewDiagnosticsFromResponse(data: MaybeResponse): MoveReviewDiagnosticsV1 | null {
  const raw = data?.diagnostics;
  if (!isRecord(raw)) return null;
  if (typeof raw.status !== 'string' || typeof raw.sourceModeReason !== 'string') return null;
  return {
    status: raw.status,
    sourceModeReason: raw.sourceModeReason,
  };
}

export function moveReviewNeedsRetry(decoded: Pick<DecodedMoveReviewResponse, 'diagnostics'>): boolean {
  return decoded.diagnostics?.status === 'retryable_fallback';
}

function surfaceRowFromUnknown(
  raw: unknown,
  allowStrategicRelation = false,
  allowAuthority = true,
): MoveReviewPlayerSurfaceRowV1 | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.label !== 'string' || typeof raw.text !== 'string') return null;
  const refSans = raw.refSans == null ? [] : stringListFromUnknown(raw.refSans);
  if (!refSans) return null;
  const authority =
    allowAuthority && raw.authority != null
      ? surfaceAuthorityFromUnknown(raw.authority, allowStrategicRelation)
      : null;
  return {
    label: raw.label,
    text: raw.text,
    tone: typeof raw.tone === 'string' ? raw.tone : null,
    refSans,
    authority: authority,
  };
}

function surfaceAuthorityFromUnknown(raw: unknown, allowStrategicRelation: boolean): MoveReviewSurfaceAuthorityV2 | null {
  if (!isRecord(raw) || typeof raw.kind !== 'string') return null;
  if (!isAuthorityKey(raw.kind)) return null;

  const token = typeof raw.token === 'string' ? raw.token : null;
  if (token !== null && !isSurfaceAuthorityTokenForKind(raw.kind, token)) return null;

  const target = typeof raw.target === 'string' ? raw.target : null;
  if (target !== null && !isChessSquare(target)) return null;

  const openingFamily = typeof raw.openingFamily === 'string' ? raw.openingFamily : null;
  if (openingFamily !== null && !isAuthorityKey(openingFamily)) return null;

  const authority = {
    kind: raw.kind,
    token,
    openingFamily,
    target,
    openingBook: raw.kind === 'opening_family' ? openingBookFromUnknown(raw.openingBook) : null,
  };
  return isSurfaceAuthorityShape(authority, allowStrategicRelation) ? authority : null;
}

function isSurfaceAuthorityShape(authority: MoveReviewSurfaceAuthorityV2, allowStrategicRelation: boolean): boolean {
  switch (authority.kind) {
    case 'counterplay_break':
      return !!authority.token && !authority.openingFamily && !authority.target && !authority.openingBook;
    case 'central_break':
    case 'central_liquidation':
    case 'central_challenge':
      return (
        !!authority.token &&
        isSurfaceAuthorityRouteToken(authority.token) &&
        !authority.openingFamily &&
        !authority.target &&
        !authority.openingBook
      );
    case 'practical_plan':
      return !authority.token && !authority.openingFamily && !authority.target && !authority.openingBook;
    case 'opening_family':
      return !!authority.openingFamily && !authority.token;
    case 'strategic_relation':
      return (
        allowStrategicRelation &&
        !!authority.token &&
        isStrategicRelationAuthorityToken(authority.token) &&
        !!authority.target &&
        !authority.openingFamily &&
        !authority.openingBook
      );
    default:
      return false;
  }
}

function openingBookFromUnknown(raw: unknown): MoveReviewOpeningBookMetadataV2 | null {
  if (!isRecord(raw)) return null;
  const eco = typeof raw.eco === 'string' && isOpeningEco(raw.eco) ? raw.eco : null;
  const totalGames =
    typeof raw.totalGames === 'number' && Number.isFinite(raw.totalGames)
      ? Math.trunc(raw.totalGames)
      : null;
  const topMoves = raw.topMoves == null ? [] : stringListFromUnknown(raw.topMoves);
  if (!topMoves) return null;
  const cleanTopMoves = topMoves.filter(isOpeningBookMove).slice(0, 3);
  const cleanTotalGames = totalGames !== null && totalGames > 0 && totalGames <= 100000000 ? totalGames : null;
  if (!eco && cleanTotalGames === null && !cleanTopMoves.length) return null;
  return {
    eco,
    totalGames: cleanTotalGames,
    topMoves: cleanTopMoves,
  };
}

function isOpeningEco(value: string): boolean {
  return /^[A-E][0-9]{2}$/.test(value);
}

function isOpeningBookMove(value: string): boolean {
  return /^(?:[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|O-O(?:-O)?[+#]?)$/.test(value);
}

function isSurfaceAuthorityToken(value: string): boolean {
  return /^(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?$/.test(value);
}

function isSurfaceAuthorityTokenForKind(kind: string, value: string): boolean {
  return kind === 'strategic_relation' ? isStrategicRelationAuthorityToken(value) : isSurfaceAuthorityToken(value);
}

function isStrategicRelationAuthorityToken(value: string): boolean {
  return isAuthorityKey(value) && strategicRelationAuthorityTokens.has(value);
}

function isSurfaceAuthorityRouteToken(value: string): boolean {
  return /^(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]$/.test(value);
}

function surfaceRowsFromUnknown(
  raw: unknown,
  allowStrategicRelation = false,
  allowAuthority = true,
): MoveReviewPlayerSurfaceRowV1[] | null {
  if (raw == null) return [];
  if (!Array.isArray(raw)) return null;
  const rows = raw.map(row => surfaceRowFromUnknown(row, allowStrategicRelation, allowAuthority));
  if (rows.some(row => !row)) return null;
  return rows as MoveReviewPlayerSurfaceRowV1[];
}

function summarySurfaceRowsFromUnknown(raw: unknown, allowAuthority = true): MoveReviewPlayerSurfaceRowV1[] | null {
  if (raw == null) return [];
  if (!Array.isArray(raw)) return null;
  const rows = raw.map(row => surfaceRowFromUnknown(row, summaryRowAllowsStrategicRelation(row), allowAuthority));
  if (rows.some(row => !row)) return null;
  return rows as MoveReviewPlayerSurfaceRowV1[];
}

function summaryRowAllowsStrategicRelation(raw: unknown): boolean {
  if (!isRecord(raw) || typeof raw.label !== 'string' || !isRecord(raw.authority)) return false;
  const authority = raw.authority;
  if (authority.kind !== 'strategic_relation' || typeof authority.token !== 'string' || typeof authority.target !== 'string')
    return false;
  if (!isChessSquare(authority.target)) return false;
  return (
    (raw.label === 'Defender trade' && authority.token === 'defender_trade') ||
    (raw.label === 'Bad piece trade' && authority.token === 'bad_piece_liquidation')
  );
}

function playerDecisionComparisonFromUnknown(raw: unknown): MoveReviewPlayerDecisionComparisonV1 | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.kicker !== 'string' || typeof raw.chosenMatchesBest !== 'boolean') return null;
  return {
    kicker: raw.kicker,
    gapLabel: typeof raw.gapLabel === 'string' ? raw.gapLabel : null,
    chosenSan: typeof raw.chosenSan === 'string' ? raw.chosenSan : null,
    engineSan: typeof raw.engineSan === 'string' ? raw.engineSan : null,
    comparedSan: typeof raw.comparedSan === 'string' ? raw.comparedSan : null,
    secondaryText: typeof raw.secondaryText === 'string' ? raw.secondaryText : null,
    chosenMatchesBest: raw.chosenMatchesBest,
    targetComparison: decisionTargetComparisonFromUnknown(raw.targetComparison),
  };
}

function decisionTargetComparisonFromUnknown(raw: unknown): MoveReviewDecisionTargetComparisonV1 | null {
  if (!isRecord(raw)) return null;
  if (
    typeof raw.chosenTarget !== 'string' ||
    typeof raw.chosenTargetKind !== 'string' ||
    typeof raw.bestTarget !== 'string' ||
    typeof raw.bestTargetKind !== 'string'
  )
    return null;
  if (
    !isChessSquare(raw.chosenTarget) ||
    !isChessSquare(raw.bestTarget) ||
    !isAuthorityKey(raw.chosenTargetKind) ||
    !isAuthorityKey(raw.bestTargetKind)
  )
    return null;
  return {
    chosenTarget: raw.chosenTarget,
    chosenTargetKind: raw.chosenTargetKind,
    bestTarget: raw.bestTarget,
    bestTargetKind: raw.bestTargetKind,
  };
}

function isChessSquare(value: string): boolean {
  return /^[a-h][1-8]$/.test(value);
}

function isAuthorityKey(value: string): boolean {
  return /^[a-z][a-z0-9_]{1,40}$/.test(value);
}

function playerAuthorRowFromUnknown(raw: unknown, allowAuthority = true): MoveReviewPlayerAuthorRowV1 | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.title !== 'string' || typeof raw.status !== 'string' || typeof raw.question !== 'string') return null;
  const branches = surfaceRowsFromUnknown(raw.branches, false, allowAuthority);
  if (!branches) return null;
  return {
    title: raw.title,
    status: raw.status,
    question: raw.question,
    why: typeof raw.why === 'string' ? raw.why : null,
    branches,
  };
}

function playerAuthorRowsFromUnknown(raw: unknown, allowAuthority = true): MoveReviewPlayerAuthorRowV1[] | null {
  if (raw == null) return [];
  if (!Array.isArray(raw)) return null;
  const rows = raw.map(row => playerAuthorRowFromUnknown(row, allowAuthority));
  if (rows.some(row => !row)) return null;
  return rows as MoveReviewPlayerAuthorRowV1[];
}

export function moveReviewPlayerSurfaceFromResponse(data: MaybeResponse): MoveReviewPlayerSurfaceV1 | null {
  const raw = data?.moveReviewPlayerSurface;
  if (!isRecord(raw)) return null;
  const schema = typeof raw.schema === 'string' ? raw.schema.trim().replace(/\s*\.\s*/g, '.') : null;
  if (schema !== 'chesstory.move_review.player_surface.v1' && schema !== 'chesstory.move_review.player_surface.v2')
    return null;
  const allowAuthority = schema === 'chesstory.move_review.player_surface.v2';
  const summaryRows = summarySurfaceRowsFromUnknown(raw.summaryRows, allowAuthority);
  const advancedRows = surfaceRowsFromUnknown(raw.advancedRows, true, allowAuthority);
  const probeRows = surfaceRowsFromUnknown(raw.probeRows, false, allowAuthority);
  const authorRows = playerAuthorRowsFromUnknown(raw.authorRows, allowAuthority);
  const decisionComparison = raw.decisionComparison == null ? null : playerDecisionComparisonFromUnknown(raw.decisionComparison);
  if (!summaryRows || !advancedRows || !probeRows || !authorRows) return null;
  if (raw.decisionComparison != null && !decisionComparison) return null;
  return {
    schema,
    title: typeof raw.title === 'string' ? raw.title : null,
    summaryRows,
    advancedRows,
    decisionComparison,
    probeRows,
    authorRows,
  };
}

export function decodeMoveReviewResponse(
  data: MaybeResponse,
  fallbacks: DecodeMoveReviewResponseFallbacks = {},
): DecodedMoveReviewResponse {
  return {
    html: htmlFromResponse(data, fallbacks.html || ''),
    commentary: commentaryFromResponse(data, fallbacks.commentary || ''),
    sourceMode: sourceModeFromResponse(data),
    model: modelFromResponse(data),
    cacheHit: cacheHitFromResponse(data),
    refs: refsFromResponse(data),
    polishMeta: polishMetaFromResponse(data),
    diagnostics: moveReviewDiagnosticsFromResponse(data),
    moveReviewExplanation: moveReviewExplanationFromResponse(data),
    moveReviewLedger: moveReviewLedgerFromResponse(data),
    moveReviewPlayerSurface: moveReviewPlayerSurfaceFromResponse(data),
    mainStrategicPlanCount: mainStrategicPlanCountFromResponse(data),
    probeRequests: probeRequestsFromResponse(data),
    authorQuestions: authorQuestionsFromResponse(data),
    authorEvidence: authorEvidenceFromResponse(data),
    planStateToken: planStateTokenFromResponse(data),
    endgameStateToken: endgameStateTokenFromResponse(data),
  };
}

function stringListFromUnknown(raw: unknown): string[] | null {
  if (!Array.isArray(raw)) return null;
  const values = raw.filter((value): value is string => typeof value === 'string');
  return values.length === raw.length ? values : null;
}

function moveReviewShortLineFromUnknown(raw: unknown): MoveReviewShortLineV1 | null {
  if (!isRecord(raw)) return null;
  const san = stringListFromUnknown(raw.san);
  const uci = stringListFromUnknown(raw.uci);
  if (!san || !uci || typeof raw.source !== 'string') return null;
  return {
    san,
    uci,
    lineId: typeof raw.lineId === 'string' ? raw.lineId : null,
    scoreCp: typeof raw.scoreCp === 'number' ? raw.scoreCp : null,
    mate: typeof raw.mate === 'number' ? raw.mate : null,
    depth: typeof raw.depth === 'number' ? raw.depth : null,
    source: raw.source,
  };
}

function moveReviewPvInterpretationFromUnknown(raw: unknown): MoveReviewPvInterpretationV1 | null {
  if (!isRecord(raw)) return null;
  const confirms = stringListFromUnknown(raw.confirms);
  if (
    typeof raw.linePurpose !== 'string' ||
    !confirms ||
    typeof raw.tension !== 'string' ||
    typeof raw.learningPoint !== 'string' ||
    typeof raw.confidence !== 'string'
  )
    return null;
  return {
    linePurpose: raw.linePurpose,
    confirms,
    tension: raw.tension,
    opponentReplyMeaning: typeof raw.opponentReplyMeaning === 'string' ? raw.opponentReplyMeaning : null,
    learningPoint: raw.learningPoint,
    supportedByLineId: typeof raw.supportedByLineId === 'string' ? raw.supportedByLineId : null,
    confidence: raw.confidence,
  };
}

export function moveReviewExplanationFromResponse(data: MaybeResponse): MoveReviewExplanationV1 | null {
  const raw = data?.moveReviewExplanation;
  if (!isRecord(raw)) return null;
  if (typeof raw.title !== 'string' || typeof raw.prose !== 'string' || typeof raw.source !== 'string') return null;
  const reasonTags = stringListFromUnknown(raw.reasonTags);
  if (!reasonTags) return null;
  const shortLine = moveReviewShortLineFromUnknown(raw.shortLine);
  if (raw.shortLine != null && !shortLine) return null;
  const pvInterpretation = moveReviewPvInterpretationFromUnknown(raw.pvInterpretation);
  if (raw.pvInterpretation != null && !pvInterpretation) return null;
  return {
    title: raw.title,
    prose: raw.prose,
    qualityLabel: typeof raw.qualityLabel === 'string' ? raw.qualityLabel : null,
    reasonTags,
    shortLine,
    pvInterpretation,
    source: raw.source,
  };
}

function ledgerLineFromUnknown(raw: unknown): MoveReviewLedgerLineV1 | null {
  if (!isRecord(raw)) return null;
  if (typeof raw.title !== 'string' || typeof raw.source !== 'string' || !Array.isArray(raw.sanMoves)) return null;
  const sanMoves = raw.sanMoves.filter((value): value is string => typeof value === 'string');
  if (sanMoves.length !== raw.sanMoves.length) return null;
  if (!sanMoves.length) return null;
  if (!(moveReviewLedgerLineSources as readonly string[]).includes(raw.source)) return null;
  return {
    title: raw.title,
    sanMoves,
    scoreCp: typeof raw.scoreCp === 'number' ? raw.scoreCp : null,
    mate: typeof raw.mate === 'number' ? raw.mate : null,
    note: typeof raw.note === 'string' ? raw.note : null,
    source: raw.source as MoveReviewLedgerLineV1['source'],
  };
}

export function moveReviewLedgerFromResponse(data: MaybeResponse): MoveReviewStrategicLedgerV1 | null {
  const raw = data?.moveReviewLedger;
  if (!isRecord(raw)) return null;
  if (raw.schema !== 'chesstory.move_review.ledger.v1') return null;
  if (
    typeof raw.motifKey !== 'string' ||
    typeof raw.motifLabel !== 'string' ||
    typeof raw.stageKey !== 'string' ||
    typeof raw.stageLabel !== 'string' ||
    typeof raw.carryOver !== 'boolean' ||
    !Array.isArray(raw.prerequisites)
  )
    return null;
  if (!isAuthorityKey(raw.motifKey) || !isAuthorityKey(raw.stageKey)) return null;
  const prerequisites = raw.prerequisites.filter((value): value is string => typeof value === 'string');
  if (prerequisites.length !== raw.prerequisites.length) return null;
  const primaryLine = raw.primaryLine == null ? null : ledgerLineFromUnknown(raw.primaryLine);
  const resourceLine = raw.resourceLine == null ? null : ledgerLineFromUnknown(raw.resourceLine);
  return {
    schema: 'chesstory.move_review.ledger.v1',
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

export function refsFromResponse(data: MaybeResponse): MoveReviewRefsV1 | null {
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
    typeof raw.cacheHit !== 'boolean'
  )
    return null;

  const validationReasonList = raw.validationReasons == null ? [] : raw.validationReasons;
  if (!Array.isArray(validationReasonList)) return null;
  const validationReasons = validationReasonList.filter((v): v is string => typeof v === 'string');
  if (validationReasons.length !== validationReasonList.length) return null;

  return {
    provider: raw.provider,
    model: typeof raw.model === 'string' ? raw.model : null,
    sourceMode: raw.sourceMode,
    validationPhase: raw.validationPhase,
    validationReasons: [],
    cacheHit: raw.cacheHit,
    promptTokens: null,
    cachedTokens: null,
    completionTokens: null,
    estimatedCostUsd: null,
    strategyCoverage: null,
  };
}

export function ratelimitSecondsFromResponse(data: MaybeResponse): number | null {
  const seconds = data?.ratelimit?.seconds;
  return typeof seconds === 'number' ? seconds : null;
}

export function resetAtFromResponse(data: MaybeResponse): string {
  return typeof data?.resetAt === 'string' ? data.resetAt : 'Unknown';
}
