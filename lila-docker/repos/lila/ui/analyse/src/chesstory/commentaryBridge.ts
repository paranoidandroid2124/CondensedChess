type JsonRecord = Record<string, unknown>;

export type WordingStrength = 'hidden' | 'negative_only' | 'context_only' | 'qualified_support' | 'assertive_certified';
export type RenderStatus = 'rendered' | 'contextOnly' | 'noCommentary';
export type CommentaryResponseStatus = RenderStatus | 'invalidRequest';

export type CommentaryBridgeNodeIdentity = {
  currentFen: string;
  nodeId: string;
  ply: number;
};

export type CommentaryRequest = {
  currentFen: string;
  beforeFen?: string;
  playedMove?: string;
  nodeId: string;
  ply: number;
  enginePacket?: JsonRecord;
};

export type CompletedProbeBridgeCurrent = CommentaryBridgeNodeIdentity & {
  variant: string;
};

export type CompletedProbeBridgeBudget = {
  rootMultiPv: 3;
  childMultiPv: 2;
  depthFloor: number;
  rootTargetDepth?: number;
  childTargetDepth?: number;
  maxAgeMillis?: number;
};

export type CompletedProbeBridgeProbeRole = 'root_candidate' | 'defender_resource';

export type CompletedProbeBridgeProbeRequest = {
  role: CompletedProbeBridgeProbeRole;
  currentFen: string;
  nodeId: string;
  ply: number;
  variant: string;
  multiPv: 2 | 3;
  requestedDepth: number;
  depthFloor: number;
  parentBranchId?: string;
  parentUciPrefix?: string[];
  parentRootRank?: number;
};

export type CompletedProbeBridgeLine = {
  rank: number;
  multiPvIndex: number;
  multiPv: 2 | 3;
  uci: string[];
};

export type CompletedProbeBridgeRootProbe = {
  currentFen: string;
  nodeId: string;
  ply: number;
  variant: string;
  engineFingerprint: string;
  requestedDepth: number;
  realizedDepth: number;
  multiPv: 3;
  generatedAt: string;
  maxAgeMillis: number;
  completed: boolean;
  lines: CompletedProbeBridgeLine[];
};

export type CompletedProbeBridgeChildProbe = {
  currentFen: string;
  nodeId: string;
  ply: number;
  variant: string;
  engineFingerprint: string;
  parentBranchId: string;
  parentUciPrefix: string[];
  parentRootRank: number;
  requestedDepth: number;
  realizedDepth: number;
  multiPv: 2;
  generatedAt: string;
  maxAgeMillis: number;
  completed: boolean;
  lines: CompletedProbeBridgeLine[];
};

export type CompletedProbeBridgeInput = {
  current: CompletedProbeBridgeCurrent;
  engineFingerprint: string;
  budget?: CompletedProbeBridgeBudget | null;
  probeRequests: CompletedProbeBridgeProbeRequest[];
  rootProbe: CompletedProbeBridgeRootProbe;
  childProbes?: CompletedProbeBridgeChildProbe[] | null;
};

export type CompletedProbeBridgePayload = {
  current: CompletedProbeBridgeCurrent;
  engineFingerprint: string;
  budget?: CompletedProbeBridgeBudget;
  probeRequests: CompletedProbeBridgeProbeRequest[];
  rootProbe: CompletedProbeBridgeRootProbe;
  childProbes: CompletedProbeBridgeChildProbe[];
};

export type RenderText = {
  publicText?: string | null;
  forbiddenTerms: string[];
};

export type RenderEvidenceRef = {
  kind: string;
  id: string;
  owner?: string | null;
  anchor?: string | null;
  route?: string | null;
  scope?: string | null;
};

export type RenderBoundary = {
  claimId: string;
  reason: string;
};

export type RenderLineRole = 'resource' | 'caution' | 'hold' | 'conversion' | 'pressure' | 'simplification';

export type VariationMoveRole = 'game_move' | 'candidate_move' | 'defender_resource' | 'continuation';

export type VariationTestResult =
  | 'resource_works'
  | 'resource_fails'
  | 'releases_counterplay'
  | 'does_not_restore_counterplay'
  | 'defensive_hold'
  | 'move_premature'
  | 'simplifies'
  | 'converts'
  | 'pressure_persists';

export type VariationProofPurpose =
  | 'holds'
  | 'fails'
  | 'releases_counterplay'
  | 'simplifies'
  | 'preserves_pressure'
  | 'denies_resource';

export type VariationSurfaceAllowance = 'public_line' | 'boundary_only';

export type RenderVariationMove = {
  san: string;
  uci: string;
};

export type RenderVariationBoundary = {
  depthFloor: number;
  realizedDepth: number;
  multiPv: number;
  freshnessChecked: boolean;
  legalReplayChecked: boolean;
  baselineChecked: boolean;
};

export type RenderVariationEvidence = {
  proofId: string;
  boundClaimId: string;
  startFen: string;
  owner: string;
  defender?: string | null;
  anchor: string;
  route: string;
  scope: string;
  role: RenderLineRole;
  moveRole: VariationMoveRole;
  lineSan: string[];
  lineUci: string[];
  playedMove?: RenderVariationMove | null;
  candidateMove?: RenderVariationMove | null;
  defenderResource?: RenderVariationMove | null;
  continuation: RenderVariationMove[];
  testedMove?: RenderVariationMove | null;
  testedLine: RenderVariationMove[];
  replyLine: RenderVariationMove[];
  resourceLine: RenderVariationMove[];
  testResult: VariationTestResult;
  proofPurpose: VariationProofPurpose;
  provenanceRefs: RenderEvidenceRef[];
  boundary: RenderVariationBoundary;
  wordingCap: WordingStrength;
  surfaceAllowance: VariationSurfaceAllowance;
};

export type RenderBlock = {
  role: 'primary' | 'supporting' | 'context' | 'contrast';
  claimId: string;
  text: RenderText;
  wordingStrength: WordingStrength;
  evidenceIds: string[];
  variationEvidenceIds?: string[];
  boundaries: RenderBoundary[];
  nonAuthoritative: boolean;
};

export type RenderSuppression = {
  claimId: string;
  reasons: string[];
  public: boolean;
};

export type RenderWording = {
  maxStrength: WordingStrength;
  allowedPublicText: boolean;
  forbiddenTerms: string[];
};

export type CommentaryRender = {
  schemaVersion: number;
  status: RenderStatus;
  blocks: RenderBlock[];
  evidenceRefs: RenderEvidenceRef[];
  variationEvidence?: RenderVariationEvidence[];
  boundaries: RenderBoundary[];
  suppressions: RenderSuppression[];
  wording: RenderWording;
};

export type CommentaryInternalMetadata = {
  suppressions?: RenderSuppression[];
  engineIntake?: { status: string; reason?: string | null } | null;
  invalidReason?: string | null;
};

export type CommentaryResponse = {
  status: CommentaryResponseStatus;
  render: CommentaryRender;
  noCommentary: boolean;
  internal?: CommentaryInternalMetadata | null;
};

export type PublicCommentaryRender =
  | {
      kind: 'render';
      schemaVersion: number;
      status: CommentaryResponseStatus;
      renderStatus: RenderStatus;
      blocks: RenderBlock[];
      evidenceRefs: RenderEvidenceRef[];
      variationEvidence?: RenderVariationEvidence[];
      boundaries: RenderBoundary[];
      wording: RenderWording;
    }
  | {
      kind: 'empty';
      reason: 'no_commentary' | 'invalid_request' | 'hidden' | 'negative_only' | 'stale_node';
    };

export type BuildCommentaryRequestInput = {
  current: CommentaryBridgeNodeIdentity;
  beforeFen?: string | null;
  playedMove?: string | null;
  enginePacket?: JsonRecord | null;
};

export type FetchCommentaryRenderInput = BuildCommentaryRequestInput & {
  endpoint: string;
  getCurrent: () => CommentaryBridgeNodeIdentity;
  fetchJson: (endpoint: string, request: CommentaryRequest) => Promise<CommentaryResponse>;
};

export function buildCompletedProbeBridgePayload(input: CompletedProbeBridgeInput): CompletedProbeBridgePayload | null {
  if (!input || typeof input !== 'object') return null;
  if (!isValidCurrent(input.current) || !isNonEmptyString(input.engineFingerprint)) return null;

  const budget = copyCompletedProbeBridgeBudget(input.budget);
  if (input.budget !== undefined && input.budget !== null && !budget) return null;

  const rootProbe = copyCompletedProbeBridgeRootProbe(input.rootProbe, input.current, input.engineFingerprint);
  if (!rootProbe) return null;

  if (!Array.isArray(input.probeRequests) || input.probeRequests.length === 0) return null;
  const probeRequests = input.probeRequests.map(request => copyCompletedProbeBridgeProbeRequest(request, input.current.variant));
  if (probeRequests.some(request => !request)) return null;

  const childInput = input.childProbes === undefined || input.childProbes === null ? [] : input.childProbes;
  if (!Array.isArray(childInput)) return null;
  const childProbes = childInput.map(probe => copyCompletedProbeBridgeChildProbe(probe, input.current.variant, input.engineFingerprint));
  if (childProbes.some(probe => !probe)) return null;
  if (
    !probeRequestsMatchCompletedProbes(
      probeRequests as CompletedProbeBridgeProbeRequest[],
      input.current,
      budget,
      rootProbe,
      childProbes as CompletedProbeBridgeChildProbe[],
    )
  )
    return null;

  return {
    current: copyCompletedProbeBridgeCurrent(input.current),
    engineFingerprint: input.engineFingerprint,
    ...(budget ? { budget } : {}),
    probeRequests: probeRequests as CompletedProbeBridgeProbeRequest[],
    rootProbe,
    childProbes: childProbes as CompletedProbeBridgeChildProbe[],
  };
}

export function buildCommentaryRequest(input: BuildCommentaryRequestInput): CommentaryRequest {
  const request: CommentaryRequest = {
    currentFen: input.current.currentFen,
    nodeId: input.current.nodeId,
    ply: input.current.ply,
  };

  if (input.beforeFen && input.playedMove) {
    request.beforeFen = input.beforeFen;
    request.playedMove = input.playedMove;
  }
  if (input.enginePacket && !containsForbiddenEnginePacketField(input.enginePacket)) request.enginePacket = input.enginePacket;

  return request;
}

function copyCompletedProbeBridgeCurrent(current: CompletedProbeBridgeCurrent): CompletedProbeBridgeCurrent {
  return {
    currentFen: current.currentFen,
    nodeId: current.nodeId,
    ply: current.ply,
    variant: current.variant,
  };
}

function probeRequestsMatchCompletedProbes(
  probeRequests: CompletedProbeBridgeProbeRequest[],
  current: CompletedProbeBridgeCurrent,
  budget: CompletedProbeBridgeBudget | null,
  rootProbe: CompletedProbeBridgeRootProbe,
  childProbes: CompletedProbeBridgeChildProbe[],
): boolean {
  const rootRequests = probeRequests.filter(request => request.role === 'root_candidate');
  const childRequests = probeRequests.filter(request => request.role === 'defender_resource');
  if (rootRequests.length !== 1 || childRequests.length !== childProbes.length) return false;

  const rootRequest = rootRequests[0];
  if (
    rootRequest.currentFen !== current.currentFen ||
    rootRequest.nodeId !== current.nodeId ||
    rootRequest.ply !== current.ply ||
    rootRequest.variant !== current.variant ||
    rootRequest.multiPv !== 3 ||
    rootRequest.requestedDepth !== rootProbe.requestedDepth ||
    rootRequest.depthFloor !== (budget?.depthFloor ?? 16)
  )
    return false;

  const unmatchedChildRequests = [...childRequests];
  for (const childProbe of childProbes) {
    const matchingIndex = unmatchedChildRequests.findIndex(request => childProbeMatchesRequest(childProbe, request, budget));
    if (matchingIndex < 0) return false;
    unmatchedChildRequests.splice(matchingIndex, 1);
  }

  return unmatchedChildRequests.length === 0;
}

function childProbeMatchesRequest(
  childProbe: CompletedProbeBridgeChildProbe,
  request: CompletedProbeBridgeProbeRequest,
  budget: CompletedProbeBridgeBudget | null,
): boolean {
  return (
    request.role === 'defender_resource' &&
    request.currentFen === childProbe.currentFen &&
    request.nodeId === childProbe.nodeId &&
    request.ply === childProbe.ply &&
    request.variant === childProbe.variant &&
    request.multiPv === childProbe.multiPv &&
    request.requestedDepth === childProbe.requestedDepth &&
    request.depthFloor === (budget?.depthFloor ?? 16) &&
    request.parentBranchId === childProbe.parentBranchId &&
    request.parentRootRank === childProbe.parentRootRank &&
    arraysEqual(request.parentUciPrefix, childProbe.parentUciPrefix)
  );
}

function containsForbiddenEnginePacketField(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;

  const keys = Object.keys(value as JsonRecord);
  if (keys.some(isForbiddenEnginePacketKey)) return true;
  if (hasCompletedProbeFieldCluster(keys)) return true;

  return Object.values(value as JsonRecord).some(containsForbiddenEnginePacketField);
}

function isForbiddenEnginePacketKey(key: string): boolean {
  const normalized = normalizeEnginePacketKey(key);
  return (
    normalized.includes('completedprobe') ||
    normalized.includes('candidateline') ||
    normalized.includes('probe') ||
    normalized.includes('cache') ||
    normalized.includes('internal') ||
    normalized === 'branchid' ||
    normalized === 'parentbranchid' ||
    normalized === 'parentuciprefix' ||
    normalized === 'parentrootrank' ||
    normalized === 'enginefingerprint'
  );
}

function hasCompletedProbeFieldCluster(keys: string[]): boolean {
  const normalizedKeys = new Set(keys.map(normalizeEnginePacketKey));
  return (
    normalizedKeys.has('completed') &&
    (normalizedKeys.has('multipv') ||
      normalizedKeys.has('requesteddepth') ||
      normalizedKeys.has('realizeddepth') ||
      normalizedKeys.has('maxagemillis') ||
      normalizedKeys.has('generatedat'))
  );
}

function normalizeEnginePacketKey(key: string): string {
  return key.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function arraysEqual(left: string[] | undefined, right: string[]): boolean {
  return Array.isArray(left) && left.length === right.length && left.every((value, index) => value === right[index]);
}

function copyCompletedProbeBridgeBudget(
  budget: CompletedProbeBridgeBudget | null | undefined,
): CompletedProbeBridgeBudget | null {
  if (budget === undefined || budget === null) return null;
  if (budget.rootMultiPv !== 3 || budget.childMultiPv !== 2 || !isDepthAtFloor(budget.depthFloor, 16)) return null;
  if (budget.rootTargetDepth !== undefined && !isDepthAtFloor(budget.rootTargetDepth, budget.depthFloor)) return null;
  if (budget.childTargetDepth !== undefined && !isDepthAtFloor(budget.childTargetDepth, budget.depthFloor)) return null;
  if (budget.maxAgeMillis !== undefined && !isNonNegativeNumber(budget.maxAgeMillis)) return null;

  return {
    rootMultiPv: 3,
    childMultiPv: 2,
    depthFloor: budget.depthFloor,
    ...(budget.rootTargetDepth !== undefined ? { rootTargetDepth: budget.rootTargetDepth } : {}),
    ...(budget.childTargetDepth !== undefined ? { childTargetDepth: budget.childTargetDepth } : {}),
    ...(budget.maxAgeMillis !== undefined ? { maxAgeMillis: budget.maxAgeMillis } : {}),
  };
}

function copyCompletedProbeBridgeProbeRequest(
  request: CompletedProbeBridgeProbeRequest,
  variant: string,
): CompletedProbeBridgeProbeRequest | null {
  if (!request || typeof request !== 'object') return null;
  if (
    !isNonEmptyString(request.currentFen) ||
    !isNonEmptyString(request.nodeId) ||
    !isNonNegativeInteger(request.ply) ||
    request.variant !== variant ||
    !isDepthAtFloor(request.depthFloor, 16) ||
    !isDepthAtFloor(request.requestedDepth, request.depthFloor)
  )
    return null;

  if (request.role === 'root_candidate') {
    if (request.multiPv !== 3) return null;
    return {
      role: 'root_candidate',
      currentFen: request.currentFen,
      nodeId: request.nodeId,
      ply: request.ply,
      variant: request.variant,
      multiPv: 3,
      requestedDepth: request.requestedDepth,
      depthFloor: request.depthFloor,
    };
  }

  if (request.role !== 'defender_resource' || request.multiPv !== 2) return null;
  if (!hasValidChildParentBinding(request)) return null;

  return {
    role: 'defender_resource',
    currentFen: request.currentFen,
    nodeId: request.nodeId,
    ply: request.ply,
    variant: request.variant,
    multiPv: 2,
    requestedDepth: request.requestedDepth,
    depthFloor: request.depthFloor,
    parentBranchId: request.parentBranchId,
    parentUciPrefix: [...request.parentUciPrefix],
    parentRootRank: request.parentRootRank,
  };
}

function copyCompletedProbeBridgeRootProbe(
  probe: CompletedProbeBridgeRootProbe,
  current: CompletedProbeBridgeCurrent,
  engineFingerprint: string,
): CompletedProbeBridgeRootProbe | null {
  if (!probe || typeof probe !== 'object') return null;
  if (
    !matchesCompletedProbeCurrent(probe, current) ||
    probe.engineFingerprint !== engineFingerprint ||
    probe.completed !== true ||
    probe.multiPv !== 3 ||
    !isDepthAtFloor(probe.requestedDepth, 16) ||
    !isDepthAtFloor(probe.realizedDepth, 16) ||
    !isNonEmptyString(probe.generatedAt) ||
    !isNonNegativeNumber(probe.maxAgeMillis)
  )
    return null;

  const lines = copyCompletedProbeBridgeLines(probe.lines, 3);
  if (!lines) return null;

  return {
    currentFen: probe.currentFen,
    nodeId: probe.nodeId,
    ply: probe.ply,
    variant: probe.variant,
    engineFingerprint: probe.engineFingerprint,
    requestedDepth: probe.requestedDepth,
    realizedDepth: probe.realizedDepth,
    multiPv: 3,
    generatedAt: probe.generatedAt,
    maxAgeMillis: probe.maxAgeMillis,
    completed: true,
    lines,
  };
}

function copyCompletedProbeBridgeChildProbe(
  probe: CompletedProbeBridgeChildProbe,
  variant: string,
  engineFingerprint: string,
): CompletedProbeBridgeChildProbe | null {
  if (!probe || typeof probe !== 'object') return null;
  if (
    !isNonEmptyString(probe.currentFen) ||
    !isNonEmptyString(probe.nodeId) ||
    !isNonNegativeInteger(probe.ply) ||
    probe.variant !== variant ||
    probe.engineFingerprint !== engineFingerprint ||
    probe.completed !== true ||
    probe.multiPv !== 2 ||
    !isDepthAtFloor(probe.requestedDepth, 16) ||
    !isDepthAtFloor(probe.realizedDepth, 16) ||
    !isNonEmptyString(probe.generatedAt) ||
    !isNonNegativeNumber(probe.maxAgeMillis) ||
    !hasValidChildParentBinding(probe)
  )
    return null;

  const lines = copyCompletedProbeBridgeLines(probe.lines, 2);
  if (!lines) return null;

  return {
    currentFen: probe.currentFen,
    nodeId: probe.nodeId,
    ply: probe.ply,
    variant: probe.variant,
    engineFingerprint: probe.engineFingerprint,
    parentBranchId: probe.parentBranchId,
    parentUciPrefix: [...probe.parentUciPrefix],
    parentRootRank: probe.parentRootRank,
    requestedDepth: probe.requestedDepth,
    realizedDepth: probe.realizedDepth,
    multiPv: 2,
    generatedAt: probe.generatedAt,
    maxAgeMillis: probe.maxAgeMillis,
    completed: true,
    lines,
  };
}

function copyCompletedProbeBridgeLines(lines: CompletedProbeBridgeLine[], multiPv: 2 | 3): CompletedProbeBridgeLine[] | null {
  if (!Array.isArray(lines) || lines.length !== multiPv) return null;

  const ranks = new Set<number>();
  const indexes = new Set<number>();
  const copied: CompletedProbeBridgeLine[] = [];

  for (const line of lines) {
    if (!line || typeof line !== 'object') return null;
    if (
      line.multiPv !== multiPv ||
      !isNonNegativeInteger(line.rank) ||
      !isNonNegativeInteger(line.multiPvIndex) ||
      line.rank < 1 ||
      line.rank > multiPv ||
      line.multiPvIndex < 1 ||
      line.multiPvIndex > multiPv ||
      line.rank !== line.multiPvIndex ||
      ranks.has(line.rank) ||
      indexes.has(line.multiPvIndex) ||
      !isValidUciLine(line.uci)
    )
      return null;
    ranks.add(line.rank);
    indexes.add(line.multiPvIndex);
    copied.push({
      rank: line.rank,
      multiPvIndex: line.multiPvIndex,
      multiPv,
      uci: [...line.uci],
    });
  }

  return copied;
}

function matchesCompletedProbeCurrent(probe: CompletedProbeBridgeRootProbe, current: CompletedProbeBridgeCurrent): boolean {
  return (
    probe.currentFen === current.currentFen &&
    probe.nodeId === current.nodeId &&
    probe.ply === current.ply &&
    probe.variant === current.variant
  );
}

function hasValidChildParentBinding(value: { parentBranchId?: string; parentUciPrefix?: string[]; parentRootRank?: number }): value is {
  parentBranchId: string;
  parentUciPrefix: string[];
  parentRootRank: number;
} {
  return (
    isNonEmptyString(value.parentBranchId) &&
    isValidUciLine(value.parentUciPrefix) &&
    (value.parentRootRank === 1 || value.parentRootRank === 2)
  );
}

function isValidCurrent(current: CompletedProbeBridgeCurrent): boolean {
  if (!current || typeof current !== 'object') return false;
  return (
    isNonEmptyString(current.currentFen) &&
    isNonEmptyString(current.nodeId) &&
    isNonNegativeInteger(current.ply) &&
    isNonEmptyString(current.variant)
  );
}

function isValidUciLine(uci: string[] | undefined): uci is string[] {
  return Array.isArray(uci) && uci.length > 0 && uci.every(move => /^[a-h][1-8][a-h][1-8][qrbn]?$/.test(move));
}

function isDepthAtFloor(value: number, floor: number): boolean {
  return Number.isInteger(value) && value >= floor;
}

function isNonNegativeInteger(value: number): boolean {
  return Number.isInteger(value) && value >= 0;
}

function isNonNegativeNumber(value: number): boolean {
  return Number.isFinite(value) && value >= 0;
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

export function decodePublicCommentaryRender(response: CommentaryResponse): PublicCommentaryRender {
  if (response.status === 'invalidRequest') return { kind: 'empty', reason: 'invalid_request' };
  if (response.noCommentary || response.render.status === 'noCommentary') return { kind: 'empty', reason: 'no_commentary' };
  if (response.render.wording.maxStrength === 'hidden') return { kind: 'empty', reason: 'hidden' };
  if (response.render.wording.maxStrength === 'negative_only') return { kind: 'empty', reason: 'negative_only' };

  const blocks = response.render.blocks.filter(hasPublicBlockPayload).map(copyRenderBlock);
  if (!blocks.length) return { kind: 'empty', reason: 'no_commentary' };
  const variationEvidence = response.render.variationEvidence?.flatMap(evidence => {
    const copied = copyRenderVariationEvidence(evidence);
    return copied ? [copied] : [];
  });

  return {
    kind: 'render',
    schemaVersion: response.render.schemaVersion,
    status: response.status,
    renderStatus: response.render.status,
    blocks,
    evidenceRefs: response.render.evidenceRefs.map(copyRenderEvidenceRef),
    ...(variationEvidence && variationEvidence.length ? { variationEvidence } : {}),
    boundaries: response.render.boundaries.map(copyRenderBoundary),
    wording: copyRenderWording(response.render.wording),
  };
}

function copyRenderBlock(block: RenderBlock): RenderBlock {
  return {
    role: block.role,
    claimId: block.claimId,
    text: {
      publicText: block.text.publicText,
      forbiddenTerms: [...block.text.forbiddenTerms],
    },
    wordingStrength: block.wordingStrength,
    evidenceIds: [...block.evidenceIds],
    ...(block.variationEvidenceIds !== undefined ? { variationEvidenceIds: [...block.variationEvidenceIds] } : {}),
    boundaries: block.boundaries.map(copyRenderBoundary),
    nonAuthoritative: block.nonAuthoritative,
  };
}

function copyRenderVariationEvidence(evidence: RenderVariationEvidence): RenderVariationEvidence | null {
  if (!isRenderLineRole(evidence.role)) return null;

  return {
    proofId: evidence.proofId,
    boundClaimId: evidence.boundClaimId,
    startFen: evidence.startFen,
    owner: evidence.owner,
    defender: evidence.defender,
    anchor: evidence.anchor,
    route: evidence.route,
    scope: evidence.scope,
    role: evidence.role,
    moveRole: evidence.moveRole,
    lineSan: [...evidence.lineSan],
    lineUci: [...evidence.lineUci],
    playedMove: copyOptionalRenderVariationMove(evidence.playedMove),
    candidateMove: copyOptionalRenderVariationMove(evidence.candidateMove),
    defenderResource: copyOptionalRenderVariationMove(evidence.defenderResource),
    continuation: evidence.continuation.map(copyRenderVariationMove),
    testedMove: copyOptionalRenderVariationMove(evidence.testedMove),
    testedLine: evidence.testedLine.map(copyRenderVariationMove),
    replyLine: evidence.replyLine.map(copyRenderVariationMove),
    resourceLine: evidence.resourceLine.map(copyRenderVariationMove),
    testResult: evidence.testResult,
    proofPurpose: evidence.proofPurpose,
    provenanceRefs: evidence.provenanceRefs.map(copyRenderEvidenceRef),
    boundary: {
      depthFloor: evidence.boundary.depthFloor,
      realizedDepth: evidence.boundary.realizedDepth,
      multiPv: evidence.boundary.multiPv,
      freshnessChecked: evidence.boundary.freshnessChecked,
      legalReplayChecked: evidence.boundary.legalReplayChecked,
      baselineChecked: evidence.boundary.baselineChecked,
    },
    wordingCap: evidence.wordingCap,
    surfaceAllowance: evidence.surfaceAllowance,
  };
}

function isRenderLineRole(value: unknown): value is RenderLineRole {
  return value === 'resource' || value === 'caution' || value === 'hold' || value === 'conversion' || value === 'pressure' || value === 'simplification';
}

function hasPublicBlockPayload(block: RenderBlock): boolean {
  return (
    (block.text.publicText || '').trim().length > 0 ||
    block.evidenceIds.length > 0 ||
    (block.variationEvidenceIds || []).length > 0
  );
}

function copyRenderVariationMove(move: RenderVariationMove): RenderVariationMove {
  return {
    san: move.san,
    uci: move.uci,
  };
}

function copyOptionalRenderVariationMove(move: RenderVariationMove | null | undefined): RenderVariationMove | null | undefined {
  return move ? copyRenderVariationMove(move) : move;
}

function copyRenderEvidenceRef(ref: RenderEvidenceRef): RenderEvidenceRef {
  return {
    kind: ref.kind,
    id: ref.id,
    owner: ref.owner,
    anchor: ref.anchor,
    route: ref.route,
    scope: ref.scope,
  };
}

function copyRenderBoundary(boundary: RenderBoundary): RenderBoundary {
  return {
    claimId: boundary.claimId,
    reason: boundary.reason,
  };
}

function copyRenderWording(wording: RenderWording): RenderWording {
  return {
    maxStrength: wording.maxStrength,
    allowedPublicText: wording.allowedPublicText,
    forbiddenTerms: [...wording.forbiddenTerms],
  };
}

export async function fetchCommentaryRender(input: FetchCommentaryRenderInput): Promise<PublicCommentaryRender> {
  const expected = input.current;
  const request = buildCommentaryRequest(input);
  const response = await input.fetchJson(input.endpoint, request);
  const current = input.getCurrent();

  if (current.nodeId !== expected.nodeId || current.ply !== expected.ply || current.currentFen !== expected.currentFen)
    return { kind: 'empty', reason: 'stale_node' };

  return decodePublicCommentaryRender(response);
}
