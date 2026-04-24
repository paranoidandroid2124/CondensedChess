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

export type RenderBlock = {
  role: 'primary' | 'supporting' | 'context' | 'contrast';
  claimId: string;
  text: RenderText;
  wordingStrength: WordingStrength;
  evidenceIds: string[];
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
  if (input.enginePacket) request.enginePacket = input.enginePacket;

  return request;
}

export function decodePublicCommentaryRender(response: CommentaryResponse): PublicCommentaryRender {
  if (response.status === 'invalidRequest') return { kind: 'empty', reason: 'invalid_request' };
  if (response.noCommentary || response.render.status === 'noCommentary') return { kind: 'empty', reason: 'no_commentary' };
  if (response.render.wording.maxStrength === 'hidden') return { kind: 'empty', reason: 'hidden' };
  if (response.render.wording.maxStrength === 'negative_only') return { kind: 'empty', reason: 'negative_only' };

  const blocks = response.render.blocks.filter(block => (block.text.publicText || '').trim().length > 0);
  if (!blocks.length) return { kind: 'empty', reason: 'no_commentary' };

  return {
    kind: 'render',
    schemaVersion: response.render.schemaVersion,
    status: response.status,
    renderStatus: response.render.status,
    blocks,
    evidenceRefs: response.render.evidenceRefs,
    boundaries: response.render.boundaries,
    wording: response.render.wording,
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
