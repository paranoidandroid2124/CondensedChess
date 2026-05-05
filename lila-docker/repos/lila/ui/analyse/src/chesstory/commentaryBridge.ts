type JsonRecord = Record<string, unknown>;

export type WordingStrength = 'hidden' | 'negative_only' | 'context_only' | 'qualified_support' | 'assertive_certified';
export type RenderStatus = 'rendered' | 'contextOnly' | 'noCommentary';
export type CommentaryResponseStatus = RenderStatus | 'invalidRequest' | 'unavailable';

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

export type PublicClaimPredicate =
  | 'board_fact'
  | 'certification'
  | 'strategy_projection'
  | 'source_context'
  | 'line_commentary'
  | 'result_material';

export type PhraseCapability = {
  maxStrength: WordingStrength;
  allowedPredicates: PublicClaimPredicate[];
  allowsResultLanguage: boolean;
  allowsBestForcedLanguage: boolean;
  allowsEngineLanguage: boolean;
  allowsLineCommentary: boolean;
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
};

export type RenderVariationEvidence = {
  proofId: string;
  boundClaimId: string;
  owner: string;
  defender?: string | null;
  anchor: string;
  route: string;
  scope: string;
  role: RenderLineRole;
  moveRole: VariationMoveRole;
  lineSan: string[];
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
  phraseCapability?: PhraseCapability | null;
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
  render: CommentaryRender | null;
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
    normalized.includes('raweval') ||
    normalized === 'linetoken' ||
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
  const render = isCommentaryRender(response.render) ? response.render : null;
  if (response.noCommentary || !render || render.status === 'noCommentary') return { kind: 'empty', reason: 'no_commentary' };
  if (render.wording.maxStrength === 'hidden') return { kind: 'empty', reason: 'hidden' };
  if (render.wording.maxStrength === 'negative_only') return { kind: 'empty', reason: 'negative_only' };
  if (PublicRenderRoutesTombstoned) return { kind: 'empty', reason: 'no_commentary' };

  const renderWording = copyRenderWording(render.wording);
  const variationEvidence = render.variationEvidence?.flatMap(evidence => {
    const copied = copyRenderVariationEvidence(evidence);
    return copied ? [copied] : [];
  });
  const publicVariationProofIds = new Set((variationEvidence || []).map(evidence => evidence.proofId));
  const blocks = render.blocks
    .map(block => copyRenderBlock(block, renderWording, publicVariationProofIds))
    .filter(hasPublicBlockPayload);
  if (!blocks.length) return { kind: 'empty', reason: 'no_commentary' };

  return {
    kind: 'render',
    schemaVersion: render.schemaVersion,
    status: response.status,
    renderStatus: render.status,
    blocks,
    evidenceRefs: render.evidenceRefs.map(copyRenderEvidenceRef),
    ...(variationEvidence && variationEvidence.length ? { variationEvidence } : {}),
    boundaries: render.boundaries.map(copyRenderBoundary),
    wording: renderWording,
  };
}

const PublicRenderRoutesTombstoned = true;

function isCommentaryRender(value: unknown): value is CommentaryRender {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Partial<CommentaryRender>;
  return (
    typeof candidate.schemaVersion === 'number' &&
    (candidate.status === 'rendered' || candidate.status === 'contextOnly' || candidate.status === 'noCommentary') &&
    Array.isArray(candidate.blocks) &&
    Array.isArray(candidate.evidenceRefs) &&
    Array.isArray(candidate.boundaries) &&
    Array.isArray(candidate.suppressions) &&
    !!candidate.wording &&
    typeof candidate.wording === 'object' &&
    isWordingStrength((candidate.wording as Partial<RenderWording>).maxStrength) &&
    typeof (candidate.wording as Partial<RenderWording>).allowedPublicText === 'boolean' &&
    Array.isArray((candidate.wording as Partial<RenderWording>).forbiddenTerms)
  );
}

function copyRenderBlock(
  block: RenderBlock,
  renderWording: RenderWording,
  publicVariationProofIds: Set<string>,
): RenderBlock {
  const phraseCapability = copyPhraseCapability(block.phraseCapability);
  const allowsLine = blockAllowsLineCommentary(block, phraseCapability, renderWording);
  const variationEvidenceIds =
    allowsLine && block.variationEvidenceIds !== undefined
      ? block.variationEvidenceIds.filter(
          (proofId): proofId is string => typeof proofId === 'string' && publicVariationProofIds.has(proofId),
        )
      : undefined;
  return {
    role: block.role,
    claimId: block.claimId,
    text: copyRenderText(block.text, block, phraseCapability, renderWording),
    wordingStrength: block.wordingStrength,
    evidenceIds: [...block.evidenceIds],
    ...(variationEvidenceIds !== undefined && variationEvidenceIds.length ? { variationEvidenceIds } : {}),
    boundaries: block.boundaries.map(copyRenderBoundary),
    nonAuthoritative: block.nonAuthoritative,
    ...(phraseCapability ? { phraseCapability } : {}),
  };
}

function copyRenderText(
  text: RenderText,
  block: RenderBlock,
  phraseCapability: PhraseCapability | null,
  renderWording: RenderWording,
): RenderText {
  const forbiddenTerms = Array.isArray(text.forbiddenTerms) ? [...text.forbiddenTerms] : [];
  const publicText = typeof text.publicText === 'string' ? text.publicText : null;
  return {
    publicText: publicTextAllowedByPhraseCapability(publicText, block, phraseCapability, renderWording) ? publicText : null,
    forbiddenTerms,
  };
}

function copyPhraseCapability(value: unknown): PhraseCapability | null {
  if (!value || typeof value !== 'object') return null;
  const candidate = value as Partial<PhraseCapability>;
  if (
    !isWordingStrength(candidate.maxStrength) ||
    !Array.isArray(candidate.allowedPredicates) ||
    typeof candidate.allowsResultLanguage !== 'boolean' ||
    typeof candidate.allowsBestForcedLanguage !== 'boolean' ||
    typeof candidate.allowsEngineLanguage !== 'boolean' ||
    typeof candidate.allowsLineCommentary !== 'boolean' ||
    !Array.isArray(candidate.forbiddenTerms)
  )
    return null;

  return {
    maxStrength: candidate.maxStrength,
    allowedPredicates: candidate.allowedPredicates.filter(isPublicClaimPredicate),
    allowsResultLanguage: candidate.allowsResultLanguage,
    allowsBestForcedLanguage: candidate.allowsBestForcedLanguage,
    allowsEngineLanguage: candidate.allowsEngineLanguage,
    allowsLineCommentary: candidate.allowsLineCommentary,
    forbiddenTerms: candidate.forbiddenTerms.filter((term): term is string => typeof term === 'string'),
  };
}

function publicTextAllowedByPhraseCapability(
  publicText: string | null,
  block: RenderBlock,
  phraseCapability: PhraseCapability | null,
  renderWording: RenderWording,
): boolean {
  if (!publicText?.trim() || !phraseCapability) return false;
  return (
    blockAllowsLineCommentary(block, phraseCapability, renderWording) &&
    !containsForbiddenTerm(publicText, phraseCapability.forbiddenTerms)
  );
}

function blockAllowsLineCommentary(block: RenderBlock, phraseCapability: PhraseCapability | null, renderWording: RenderWording): boolean {
  if (!phraseCapability || !renderWording.allowedPublicText || block.role !== 'primary') return false;
  return (
    phraseCapability.allowsLineCommentary &&
    phraseCapability.allowedPredicates.includes('line_commentary') &&
    !phraseCapability.allowsResultLanguage &&
    !phraseCapability.allowsBestForcedLanguage &&
    !phraseCapability.allowsEngineLanguage &&
    wordingRank(block.wordingStrength) >= wordingRank('qualified_support') &&
    wordingRank(phraseCapability.maxStrength) >= wordingRank(block.wordingStrength) &&
    wordingRank(renderWording.maxStrength) >= wordingRank(block.wordingStrength)
  );
}

function containsForbiddenTerm(text: string, terms: string[]): boolean {
  const normalized = text.toLowerCase();
  return terms.some(term => normalized.includes(term.toLowerCase()));
}

function isWordingStrength(value: unknown): value is WordingStrength {
  return value === 'hidden' || value === 'negative_only' || value === 'context_only' || value === 'qualified_support' || value === 'assertive_certified';
}

function isPublicClaimPredicate(value: unknown): value is PublicClaimPredicate {
  return (
    value === 'board_fact' ||
    value === 'certification' ||
    value === 'strategy_projection' ||
    value === 'source_context' ||
    value === 'line_commentary' ||
    value === 'result_material'
  );
}

function wordingRank(strength: WordingStrength): number {
  switch (strength) {
    case 'hidden':
      return 0;
    case 'negative_only':
      return 1;
    case 'context_only':
      return 2;
    case 'qualified_support':
      return 3;
    case 'assertive_certified':
      return 4;
  }
}

function copyRenderVariationEvidence(evidence: RenderVariationEvidence): RenderVariationEvidence | null {
  if (!isRenderLineRole(evidence.role)) return null;
  if (evidence.surfaceAllowance !== 'public_line') return null;

  return {
    proofId: evidence.proofId,
    boundClaimId: evidence.boundClaimId,
    owner: evidence.owner,
    defender: evidence.defender,
    anchor: evidence.anchor,
    route: evidence.route,
    scope: evidence.scope,
    role: evidence.role,
    moveRole: evidence.moveRole,
    lineSan: [...evidence.lineSan],
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
