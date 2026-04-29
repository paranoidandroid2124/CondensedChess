import { defaultInit, ensureOk, jsonHeader, xhrHeader } from 'lib/xhr';
import type { CevalCtrl, Work } from 'lib/ceval';
import { lichessRules } from 'chessops/compat';
import { makeFen, parseFen } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { parseUci } from 'chessops/util';
import { setupPosition } from 'chessops/variant';
import type { CommentaryBridgeNodeIdentity, CommentaryRequest, CommentaryResponse } from './commentaryBridge';

export const localCommentaryProbeEndpoint = '/internal/commentary/render-local-probe';

const defaultBudget = {
  rootMultiPv: 3,
  childMultiPv: 2,
  depthFloor: 16,
  rootTargetDepth: 18,
  childTargetDepth: 18,
  maxAgeMillis: 60_000,
} as const;

export type LocalCommentaryProbeRequest = {
  fen: string;
  nodeId: string;
  ply: number;
  variant: VariantKey;
  multiPv: number;
  targetDepth: number;
  depthFloor: number;
  role: 'root' | 'child';
  parentBranchId?: string;
  parentRootRank?: number;
  parentUciPrefix?: string[];
};

export type LocalCommentaryProbeLine = {
  moves: string[];
};

export type LocalCommentaryProbeResult = {
  fen: string;
  depth: number;
  pvs: LocalCommentaryProbeLine[];
};

export type LocalCommentaryProbeEngine = {
  run: (request: LocalCommentaryProbeRequest) => Promise<LocalCommentaryProbeResult | null>;
};

export type LocalCommentaryProbePayload = {
  request: CommentaryRequest;
  completedProbe: {
    current: {
      currentFen: string;
      nodeId: string;
      ply: number;
      variant: VariantKey;
    };
    engineFingerprint: string;
    budget: {
      rootMultiPv: number;
      childMultiPv: number;
      depthFloor: number;
      rootTargetDepth: number;
      childTargetDepth: number;
      maxAgeMillis: number;
    };
    probeRequests: LocalCommentaryProbeTransportRequest[];
    rootProbe: LocalCommentaryCompletedRootProbe;
    childProbes: LocalCommentaryCompletedChildProbe[];
  };
};

type LocalCommentaryProbeTransportRequest = {
  role: 'root_candidate' | 'defender_resource';
  currentFen: string;
  nodeId: string;
  ply: number;
  variant: VariantKey;
  multiPv: number;
  requestedDepth: number;
  depthFloor: number;
  parentBranchId?: string;
  parentUciPrefix?: string[];
  parentRootRank?: number;
};

type LocalCommentaryCompletedLine = {
  rank: number;
  multiPvIndex: number;
  multiPv: number;
  uci: string[];
};

type LocalCommentaryCompletedRootProbe = {
  currentFen: string;
  nodeId: string;
  ply: number;
  variant: VariantKey;
  engineFingerprint: string;
  requestedDepth: number;
  realizedDepth: number;
  multiPv: number;
  generatedAt: string;
  maxAgeMillis: number;
  completed: boolean;
  lines: LocalCommentaryCompletedLine[];
};

type LocalCommentaryCompletedChildProbe = LocalCommentaryCompletedRootProbe & {
  parentBranchId: string;
  parentUciPrefix: string[];
  parentRootRank: number;
};

export type BuildLocalCommentaryProbePayloadInput = {
  current: CommentaryBridgeNodeIdentity;
  beforeFen?: string | null;
  playedMove?: string | null;
  variant?: VariantKey;
  engineFingerprint: string;
  now?: () => number;
  engine: LocalCommentaryProbeEngine;
  cache?: LocalCommentaryProbeCache;
};

export class LocalCommentaryProbeCache {
  private readonly entries = new Map<string, LocalCommentaryCachedProbeResult>();

  read(
    request: LocalCommentaryProbeRequest,
    engineFingerprint: string,
    now: number,
  ): LocalCommentaryCachedProbeResult | undefined {
    const entry = this.entries.get(cacheKey(request, engineFingerprint));
    if (!entry || now - entry.generatedAt > defaultBudget.maxAgeMillis) return undefined;
    return entry;
  }

  write(
    request: LocalCommentaryProbeRequest,
    engineFingerprint: string,
    result: LocalCommentaryProbeResult,
    generatedAt: number,
  ): void {
    this.entries.set(cacheKey(request, engineFingerprint), {
      result: {
        fen: result.fen,
        depth: result.depth,
        pvs: result.pvs.map(pv => ({ moves: [...pv.moves] })),
      },
      generatedAt,
    });
  }
}

export type LocalCommentaryCachedProbeResult = {
  result: LocalCommentaryProbeResult;
  generatedAt: number;
};

export async function buildLocalCommentaryProbePayload(
  input: BuildLocalCommentaryProbePayloadInput,
): Promise<LocalCommentaryProbePayload | null> {
  const variant = input.variant || 'standard';
  if (variant !== 'standard') return null;
  const now = input.now?.() ?? Date.now();
  const rootRequest: LocalCommentaryProbeRequest = {
    fen: input.current.currentFen,
    nodeId: input.current.nodeId,
    ply: input.current.ply,
    variant,
    multiPv: defaultBudget.rootMultiPv,
    targetDepth: defaultBudget.rootTargetDepth,
    depthFloor: defaultBudget.depthFloor,
    role: 'root',
  };
  const rootRun = await runCached(input.engine, rootRequest, input.engineFingerprint, now, input.cache);
  if (!rootRun || !completeResult(rootRun.result, rootRequest)) return null;

  const rootLines = completedLines(rootRun.result, rootRequest.multiPv);
  if (!rootLines) return null;

  const childProbes: LocalCommentaryCompletedChildProbe[] = [];
  const probeRequests: LocalCommentaryProbeTransportRequest[] = [transportRequest(rootRequest)];
  for (const rootLine of rootLines.slice(0, 2)) {
    const rootMove = rootLine.uci[0];
    const childFen = rootMove ? fenAfterUci(input.current.currentFen, variant, rootMove) : null;
    if (!childFen) return null;
    const childRequest: LocalCommentaryProbeRequest = {
      fen: childFen,
      nodeId: input.current.nodeId,
      ply: input.current.ply + 1,
      variant,
      multiPv: defaultBudget.childMultiPv,
      targetDepth: defaultBudget.childTargetDepth,
      depthFloor: defaultBudget.depthFloor,
      role: 'child',
      parentBranchId: `root-candidate-${rootLine.rank}`,
      parentRootRank: rootLine.rank,
      parentUciPrefix: [rootMove],
    };
    probeRequests.push(transportRequest(childRequest));
    const childRun = await runCached(input.engine, childRequest, input.engineFingerprint, now, input.cache);
    if (!childRun || !completeResult(childRun.result, childRequest)) return null;
    const childLines = completedLines(childRun.result, childRequest.multiPv);
    if (!childLines) return null;
    childProbes.push({
      ...completedProbeBase(childRequest, input.engineFingerprint, childRun.generatedAt, childRun.result),
      parentBranchId: childRequest.parentBranchId!,
      parentUciPrefix: childRequest.parentUciPrefix!,
      parentRootRank: childRequest.parentRootRank!,
      lines: childLines,
    });
  }

  const request: CommentaryRequest = {
    currentFen: input.current.currentFen,
    nodeId: input.current.nodeId,
    ply: input.current.ply,
  };
  if (input.beforeFen && input.playedMove) {
    request.beforeFen = input.beforeFen;
    request.playedMove = input.playedMove;
  }

  return {
    request,
    completedProbe: {
      current: {
        currentFen: input.current.currentFen,
        nodeId: input.current.nodeId,
        ply: input.current.ply,
        variant,
      },
      engineFingerprint: input.engineFingerprint.trim(),
      budget: { ...defaultBudget },
      probeRequests,
      rootProbe: {
        ...completedProbeBase(rootRequest, input.engineFingerprint, rootRun.generatedAt, rootRun.result),
        lines: rootLines,
      },
      childProbes,
    },
  };
}

export type LocalCommentaryProbeProvider = {
  endpoint?: string;
  buildPayload: (input: {
    current: CommentaryBridgeNodeIdentity;
    beforeFen?: string | null;
    playedMove?: string | null;
  }) => Promise<LocalCommentaryProbePayload | null>;
  fetchJson?: (endpoint: string, payload: LocalCommentaryProbePayload) => Promise<CommentaryResponse>;
};

export function makeLocalCevalProbeProvider(input: {
  ceval: () => CevalCtrl | undefined;
  variant: () => VariantKey;
  canRun?: () => boolean;
  cache?: LocalCommentaryProbeCache;
}): LocalCommentaryProbeProvider {
  const cache = input.cache ?? new LocalCommentaryProbeCache();
  return {
    endpoint: localCommentaryProbeEndpoint,
    buildPayload: async probeInput => {
      const ceval = input.ceval();
      const engineFingerprint = ceval?.engines.active?.id;
      if (!ceval || !engineFingerprint || input.canRun?.() === false) return null;
      return buildLocalCommentaryProbePayload({
        current: probeInput.current,
        beforeFen: probeInput.beforeFen,
        playedMove: probeInput.playedMove,
        variant: input.variant(),
        engineFingerprint,
        engine: makeCevalProbeEngine(ceval, engineFingerprint),
        cache,
      });
    },
  };
}

export async function postLocalCommentaryProbeJson(
  endpoint: string,
  payload: LocalCommentaryProbePayload,
): Promise<CommentaryResponse> {
  const res = await fetch(endpoint, {
    ...defaultInit,
    method: 'post',
    headers: {
      ...jsonHeader,
      ...xhrHeader,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  return ensureOk(res).json() as Promise<CommentaryResponse>;
}

function makeCevalProbeEngine(ceval: CevalCtrl, engineFingerprint: string): LocalCommentaryProbeEngine {
  return {
    run: request => runCevalProbe(ceval, engineFingerprint, request),
  };
}

function runCevalProbe(
  ceval: CevalCtrl,
  engineFingerprint: string,
  request: LocalCommentaryProbeRequest,
): Promise<LocalCommentaryProbeResult | null> {
  return new Promise(resolve => {
    let settled = false;
    const timeout = window.setTimeout(() => finish(null), 90_000);
    const finish = (result: LocalCommentaryProbeResult | null) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timeout);
      ceval.stop();
      resolve(result);
    };
    const work: Work = {
      variant: request.variant,
      threads: ceval.threads,
      hashSize: ceval.hashSize,
      gameId: undefined,
      stopRequested: false,
      path: `commentary:${request.role}:${request.nodeId}:${request.ply}:${engineFingerprint}`,
      search: { depth: request.targetDepth },
      multiPv: request.multiPv,
      ply: request.ply,
      threatMode: false,
      initialFen: request.fen,
      currentFen: request.fen,
      moves: [],
      emit: ev => {
        if (ev.fen !== request.fen || ev.depth < request.targetDepth || ev.pvs.length < request.multiPv) return;
        finish({
          fen: ev.fen,
          depth: ev.depth,
          pvs: ev.pvs.slice(0, request.multiPv).map(pv => ({ moves: [...pv.moves] })),
        });
      },
    };
    ceval.resume(work);
  });
}

async function runCached(
  engine: LocalCommentaryProbeEngine,
  request: LocalCommentaryProbeRequest,
  engineFingerprint: string,
  now: number,
  cache?: LocalCommentaryProbeCache,
): Promise<LocalCommentaryCachedProbeResult | null> {
  const cached = cache?.read(request, engineFingerprint, now);
  if (cached && completeResult(cached.result, request)) return cached;
  const result = await engine.run(request);
  if (!result || !completeResult(result, request)) return null;
  cache?.write(request, engineFingerprint, result, now);
  return { result, generatedAt: now };
}

function completeResult(
  result: LocalCommentaryProbeResult | null | undefined,
  request: LocalCommentaryProbeRequest,
): result is LocalCommentaryProbeResult {
  return (
    !!result &&
    result.fen === request.fen &&
    result.depth >= request.depthFloor &&
    result.pvs.length >= request.multiPv &&
    result.pvs.slice(0, request.multiPv).every(pv => pv.moves.length > 0 && pv.moves.every(move => !!normalizeUci(move)))
  );
}

function completedLines(result: LocalCommentaryProbeResult, multiPv: number): LocalCommentaryCompletedLine[] | null {
  const lines = result.pvs.slice(0, multiPv).map((pv, index) => ({
    rank: index + 1,
    multiPvIndex: index + 1,
    multiPv,
    uci: pv.moves.map(normalizeUci).filter((move): move is string => !!move),
  }));
  return lines.length === multiPv && lines.every(line => line.uci.length > 0) ? lines : null;
}

function completedProbeBase(
  request: LocalCommentaryProbeRequest,
  engineFingerprint: string,
  now: number,
  result: LocalCommentaryProbeResult,
): Omit<LocalCommentaryCompletedRootProbe, 'lines'> {
  return {
    currentFen: request.fen,
    nodeId: request.nodeId,
    ply: request.ply,
    variant: request.variant,
    engineFingerprint: engineFingerprint.trim(),
    requestedDepth: request.targetDepth,
    realizedDepth: result.depth,
    multiPv: request.multiPv,
    generatedAt: String(now),
    maxAgeMillis: defaultBudget.maxAgeMillis,
    completed: true,
  };
}

function transportRequest(request: LocalCommentaryProbeRequest): LocalCommentaryProbeTransportRequest {
  return {
    role: request.role === 'root' ? 'root_candidate' : 'defender_resource',
    currentFen: request.fen,
    nodeId: request.nodeId,
    ply: request.ply,
    variant: request.variant,
    multiPv: request.multiPv,
    requestedDepth: request.targetDepth,
    depthFloor: request.depthFloor,
    ...(request.parentBranchId ? { parentBranchId: request.parentBranchId } : {}),
    ...(request.parentUciPrefix ? { parentUciPrefix: request.parentUciPrefix } : {}),
    ...(request.parentRootRank ? { parentRootRank: request.parentRootRank } : {}),
  };
}

function fenAfterUci(fen: string, variant: VariantKey, uci: string): string | null {
  try {
    const setup = parseFen(fen).unwrap();
    return setupPosition(lichessRules(variant), setup).unwrap(
      pos => {
        const move = parseUci(uci);
        if (!move || !pos.isLegal(move)) return null;
        makeSanAndPlay(pos, move);
        return makeFen(pos.toSetup());
      },
      () => null,
    );
  } catch (_) {
    return null;
  }
}

function normalizeUci(raw: string): string | null {
  const trimmed = raw.trim().toLowerCase();
  return trimmed && parseUci(trimmed) ? trimmed : null;
}

function cacheKey(request: LocalCommentaryProbeRequest, engineFingerprint: string): string {
  return JSON.stringify([
    request.variant,
    request.fen,
    request.nodeId,
    request.ply,
    engineFingerprint,
    request.role,
    request.multiPv,
    request.targetDepth,
    request.depthFloor,
    request.parentBranchId || '',
    request.parentRootRank || 0,
    ...(request.parentUciPrefix || []),
  ]);
}
