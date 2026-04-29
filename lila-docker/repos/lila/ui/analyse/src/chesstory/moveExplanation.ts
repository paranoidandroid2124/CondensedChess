import { defaultInit, ensureOk, jsonHeader, xhrHeader } from 'lib/xhr';
import {
  decodePublicCommentaryRender,
  fetchCommentaryRender,
  type CommentaryBridgeNodeIdentity,
  type CommentaryRequest,
  type CommentaryResponse,
  type FetchCommentaryRenderInput,
  type PublicCommentaryRender,
} from './commentaryBridge';
import {
  localCommentaryProbeEndpoint,
  postLocalCommentaryProbeJson,
  type LocalCommentaryProbePayload,
  type LocalCommentaryProbeProvider,
} from './localProbe';

const commentaryRenderEndpoint = '/api/commentary/render';

type HostNode = {
  fen: string;
  ply: number;
  uci?: string;
};

export type MoveExplanationHost = {
  path: string;
  node: HostNode;
  nodeList: HostNode[];
  redraw: Redraw;
};

export type MoveExplanationReadyState = {
  kind: 'ready';
  identity: CommentaryBridgeNodeIdentity;
  render: Extract<PublicCommentaryRender, { kind: 'render' }>;
};

export type MoveExplanationState =
  | { kind: 'empty' }
  | { kind: 'loading'; identity: CommentaryBridgeNodeIdentity }
  | MoveExplanationReadyState
  | { kind: 'error'; identity: CommentaryBridgeNodeIdentity };

export type MoveExplanationCtrl = {
  state: () => MoveExplanationState;
  refresh: () => Promise<void>;
};

export type MoveExplanationOptions = {
  endpoint?: string;
  fetchJson?: FetchCommentaryRenderInput['fetchJson'];
  localProbe?: LocalCommentaryProbeProvider;
};

export function makeMoveExplanation(host: MoveExplanationHost, opts: MoveExplanationOptions = {}): MoveExplanationCtrl {
  const endpoint = opts.endpoint || commentaryRenderEndpoint;
  const fetchJson = opts.fetchJson || postCommentaryJson;
  let state: MoveExplanationState = { kind: 'empty' };
  let requestSerial = 0;

  const ctrl: MoveExplanationCtrl = {
    state: () => state,
    refresh: async () => {
      const serial = ++requestSerial;
      const identity = currentIdentity(host);
      const transition = currentTransition(host);
      state = { kind: 'loading', identity };
      host.redraw();

      try {
        const render =
          opts.localProbe
            ? await fetchLocalProbeCommentary(opts.localProbe, identity, transition, () => currentIdentity(host))
            : await fetchCommentaryRender({
                endpoint,
                current: identity,
                beforeFen: transition?.beforeFen,
                playedMove: transition?.playedMove,
                getCurrent: () => currentIdentity(host),
                fetchJson,
              });
        if (serial !== requestSerial) return;
        state = render.kind === 'render' ? { kind: 'ready', identity, render } : { kind: 'empty' };
      } catch (_) {
        if (serial !== requestSerial) return;
        state = { kind: 'error', identity };
      } finally {
        if (serial === requestSerial) host.redraw();
      }
    },
  };

  return ctrl;
}

async function fetchLocalProbeCommentary(
  provider: LocalCommentaryProbeProvider,
  identity: CommentaryBridgeNodeIdentity,
  transition: { beforeFen: string; playedMove: string } | undefined,
  getCurrent: () => CommentaryBridgeNodeIdentity,
): Promise<PublicCommentaryRender> {
  const payload = await provider.buildPayload({
    current: identity,
    beforeFen: transition?.beforeFen,
    playedMove: transition?.playedMove,
  });
  if (!payload) return { kind: 'empty', reason: 'no_commentary' };
  const response = await (provider.fetchJson || postLocalCommentaryProbeJson)(
    provider.endpoint || localCommentaryProbeEndpoint,
    stripInternalOnlyRequestFields(payload),
  );
  const current = getCurrent();
  if (current.nodeId !== identity.nodeId || current.ply !== identity.ply || current.currentFen !== identity.currentFen)
    return { kind: 'empty', reason: 'stale_node' };
  const render = decodePublicCommentaryRender(response);
  return localProbeBacked(render) ? render : { kind: 'empty', reason: 'no_commentary' };
}

function stripInternalOnlyRequestFields(payload: LocalCommentaryProbePayload): LocalCommentaryProbePayload {
  const request: CommentaryRequest = {
    currentFen: payload.request.currentFen,
    nodeId: payload.request.nodeId,
    ply: payload.request.ply,
  };
  if (payload.request.beforeFen && payload.request.playedMove) {
    request.beforeFen = payload.request.beforeFen;
    request.playedMove = payload.request.playedMove;
  }
  return {
    request,
    completedProbe: payload.completedProbe,
  };
}

function localProbeBacked(render: PublicCommentaryRender): boolean {
  if (render.kind !== 'render' || !render.variationEvidence?.length) return false;
  const publicLineIds = new Set(
    render.variationEvidence
      .filter(evidence => evidence.surfaceAllowance === 'public_line')
      .map(evidence => evidence.proofId),
  );
  return render.blocks.some(block => block.variationEvidenceIds?.some(id => publicLineIds.has(id)));
}

export async function postCommentaryJson(endpoint: string, request: CommentaryRequest): Promise<CommentaryResponse> {
  const res = await fetch(endpoint, {
    ...defaultInit,
    method: 'post',
    headers: {
      ...jsonHeader,
      ...xhrHeader,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return ensureOk(res).json() as Promise<CommentaryResponse>;
}

function currentIdentity(host: MoveExplanationHost): CommentaryBridgeNodeIdentity {
  return {
    currentFen: host.node.fen,
    nodeId: host.path || 'root',
    ply: host.node.ply,
  };
}

function currentTransition(host: MoveExplanationHost): { beforeFen: string; playedMove: string } | undefined {
  const previous = host.nodeList.length > 1 ? host.nodeList[host.nodeList.length - 2] : undefined;
  if (!previous || !host.node.uci) return undefined;
  return {
    beforeFen: previous.fen,
    playedMove: host.node.uci,
  };
}
