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
    normalized.startsWith('proof') ||
    normalized.includes('proves') ||
    normalized.includes('rawpv') ||
    normalized.includes('rawpvs') ||
    normalized.includes('rawline') ||
    normalized.includes('rawlines') ||
    normalized.includes('rawprobe') ||
    normalized.includes('rawprobes') ||
    normalized.includes('sourcerow') ||
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
