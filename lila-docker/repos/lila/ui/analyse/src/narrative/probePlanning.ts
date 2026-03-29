import type { ProbeRequest, ProbeResult } from '../bookmaker/types';

export type ProbePlanningMoment = {
  ply: number;
  cpBefore?: number;
  cpAfter?: number;
  selectionKind?: string;
  strategicSalience?: string;
  strategicBranch?: boolean;
  signalDigest?: {
    compensation?: string;
    compensationVectors?: string[];
    investedMaterial?: number;
    dominantIdeaKind?: string;
    opponentPlan?: string;
  };
  strategyPack?: {
    strategicIdeas?: unknown[];
    longTermFocus?: string[];
    pieceRoutes?: unknown[];
    signalDigest?: {
      compensation?: string;
      compensationVectors?: string[];
      investedMaterial?: number;
    } | null;
  };
  probeRequests?: ProbeRequest[];
  probeRefinementRequests?: ProbeRequest[];
  authorQuestions?: unknown[];
  authorEvidence?: unknown[];
};

export type ProbePlanningResponse = {
  moments: ProbePlanningMoment[];
};

export type ProbeMomentBundle = {
  ply: number;
  baselineCp: number;
  requests: ProbeRequest[];
};

export type ProbeResultsByPlyEntry = {
  ply: number;
  results: ProbeResult[];
};

type ProbeCompensationSubtype = {
  pressureMode: 'line_occupation' | 'target_fixing' | 'break_preparation' | 'defender_tied_down' | 'counterplay_denial' | 'conversion_window';
  recoveryPolicy: 'immediate' | 'delayed' | 'intentionally_deferred';
  stabilityClass: 'tactical_window' | 'durable_pressure' | 'transition_only';
};

export function probeRequestDedupKey(request: ProbeRequest): string {
  const moves = Array.isArray(request.moves) ? request.moves.join(',') : '';
  return [
    request.fen || '',
    moves,
    typeof request.depth === 'number' ? request.depth : '',
    request.purpose || '',
    request.questionId || '',
    request.planId || '',
    request.variationHash || '',
  ].join('|');
}

function requiredSignalPresent(signal: string, result: ProbeResult): boolean {
  switch (signal) {
    case 'replyPvs':
      return (
        (Array.isArray(result.replyPvs) && result.replyPvs.some(line => Array.isArray(line) && line.length > 0)) ||
        (Array.isArray(result.bestReplyPv) && result.bestReplyPv.length > 0)
      );
    case 'keyMotifs':
      return Array.isArray(result.keyMotifs) && result.keyMotifs.length > 0;
    case 'l1Delta':
      return !!result.l1Delta;
    case 'futureSnapshot':
      return !!result.futureSnapshot;
    case 'purpose':
      return typeof result.purpose === 'string' && result.purpose.trim().length > 0;
    case 'depth':
      return typeof result.depth === 'number' && result.depth > 0;
    default:
      return true;
  }
}

export function validateProbeResultAgainstRequest(request: ProbeRequest, result: ProbeResult): boolean {
  if (!request || !result) return false;
  if (request.id !== result.id) return false;
  if (typeof result.fen === 'string' && request.fen && result.fen !== request.fen) return false;
  if (typeof request.purpose === 'string' && request.purpose.length) {
    if (typeof result.purpose === 'string' && result.purpose.length && result.purpose !== request.purpose) return false;
  }
  if (typeof request.objective === 'string' && request.objective.length) {
    if (typeof result.objective === 'string' && result.objective.length && result.objective !== request.objective) return false;
  }
  if (typeof request.seedId === 'string' && request.seedId.length) {
    if (typeof result.seedId === 'string' && result.seedId.length && result.seedId !== request.seedId) return false;
  }
  const expectedMove =
    typeof request.candidateMove === 'string' && request.candidateMove.trim().length
      ? request.candidateMove.trim()
      : Array.isArray(request.moves) && request.moves.length === 1 && typeof request.moves[0] === 'string'
        ? request.moves[0].trim()
        : undefined;
  const resultMove =
    typeof result.probedMove === 'string' && result.probedMove.trim().length
      ? result.probedMove.trim()
      : typeof result.candidateMove === 'string' && result.candidateMove.trim().length
        ? result.candidateMove.trim()
        : undefined;
  if (expectedMove && resultMove && expectedMove !== resultMove) return false;
  if (resultMove && Array.isArray(request.moves) && request.moves.length && !request.moves.includes(resultMove)) return false;
  if (typeof request.variationHash === 'string' && request.variationHash.trim().length) {
    if (typeof result.variationHash === 'string' && result.variationHash.trim().length) {
      if (result.variationHash.trim() !== request.variationHash.trim()) return false;
    } else return false;
  }
  if (typeof request.engineConfigFingerprint === 'string' && request.engineConfigFingerprint.trim().length) {
    if (typeof result.engineConfigFingerprint === 'string' && result.engineConfigFingerprint.trim().length) {
      if (result.engineConfigFingerprint.trim() !== request.engineConfigFingerprint.trim()) return false;
    } else return false;
  }
  const depthFloor =
    typeof request.depthFloor === 'number' && request.depthFloor > 0
      ? request.depthFloor
      : typeof request.depth === 'number' && request.depth > 0
        ? request.depth
        : undefined;
  if (typeof depthFloor === 'number' && depthFloor > 0) {
    if (typeof result.depth !== 'number' || result.depth < depthFloor) return false;
  }
  const requiredSignals = Array.isArray(request.requiredSignals) ? request.requiredSignals.filter(Boolean) : [];
  return requiredSignals.every(signal => requiredSignalPresent(signal, result));
}

function requestSource(moment: ProbePlanningMoment): ProbeRequest[] {
  if (Array.isArray(moment.probeRefinementRequests) && moment.probeRefinementRequests.length) {
    return moment.probeRefinementRequests;
  }
  return Array.isArray(moment.probeRequests) ? moment.probeRequests : [];
}

function momentBaselineCp(moment: ProbePlanningMoment): number {
  if (typeof moment.cpAfter === 'number') return moment.cpAfter;
  if (typeof moment.cpBefore === 'number') return moment.cpBefore;
  return 0;
}

function hasCompensationCarrier(moment: ProbePlanningMoment): boolean {
  const directDigest = moment.signalDigest;
  const packDigest = moment.strategyPack?.signalDigest;
  const vectors = [
    ...(Array.isArray(directDigest?.compensationVectors) ? directDigest.compensationVectors : []),
    ...(Array.isArray(packDigest?.compensationVectors) ? packDigest.compensationVectors : []),
  ].filter(Boolean);
  return !!(
    (typeof directDigest?.compensation === 'string' && directDigest.compensation.trim().length) ||
    (typeof packDigest?.compensation === 'string' && packDigest.compensation.trim().length) ||
    vectors.length ||
    (typeof directDigest?.investedMaterial === 'number' && directDigest.investedMaterial > 0) ||
    (typeof packDigest?.investedMaterial === 'number' && packDigest.investedMaterial > 0)
  );
}

function hasStrategicCarrier(moment: ProbePlanningMoment): boolean {
  return !!(
    hasCompensationCarrier(moment) ||
    (typeof moment.signalDigest?.dominantIdeaKind === 'string' && moment.signalDigest.dominantIdeaKind.trim().length) ||
    (typeof moment.signalDigest?.opponentPlan === 'string' && moment.signalDigest.opponentPlan.trim().length) ||
    (Array.isArray(moment.strategyPack?.strategicIdeas) && moment.strategyPack!.strategicIdeas!.length > 0) ||
    (Array.isArray(moment.strategyPack?.longTermFocus) && moment.strategyPack!.longTermFocus!.length > 0) ||
    (Array.isArray(moment.strategyPack?.pieceRoutes) && moment.strategyPack!.pieceRoutes!.length > 0)
  );
}

function compensationTexts(moment: ProbePlanningMoment): string[] {
  const directDigest = moment.signalDigest;
  const packDigest = moment.strategyPack?.signalDigest;
  return [
    typeof directDigest?.compensation === 'string' ? directDigest.compensation : '',
    ...(Array.isArray(directDigest?.compensationVectors) ? directDigest.compensationVectors : []),
    typeof packDigest?.compensation === 'string' ? packDigest.compensation : '',
    ...(Array.isArray(packDigest?.compensationVectors) ? packDigest.compensationVectors : []),
    ...(Array.isArray(moment.strategyPack?.longTermFocus) ? moment.strategyPack!.longTermFocus! : []),
    typeof moment.signalDigest?.dominantIdeaKind === 'string' ? moment.signalDigest.dominantIdeaKind : '',
  ]
    .map(value => value?.toString().trim().toLowerCase())
    .filter(Boolean);
}

function deriveCompensationSubtype(moment: ProbePlanningMoment): ProbeCompensationSubtype | undefined {
  if (!hasCompensationCarrier(moment)) return;
  const texts = compensationTexts(moment);
  const dominantIdeaKind = typeof moment.signalDigest?.dominantIdeaKind === 'string' ? moment.signalDigest.dominantIdeaKind.trim().toLowerCase() : '';
  const hasAny = (needles: string[]) => texts.some(text => needles.some(needle => text.includes(needle)));
  const pressureMode: ProbeCompensationSubtype['pressureMode'] =
    dominantIdeaKind === 'target_fixing' || hasAny(['target fixing', 'fixed target', 'fixed targets', 'minority attack', 'weak target'])
      ? 'target_fixing'
      : dominantIdeaKind === 'line_occupation' || hasAny(['line pressure', 'file pressure', 'open file', 'semi open', 'file control', 'open line'])
        ? 'line_occupation'
        : dominantIdeaKind === 'counterplay_suppression' || hasAny(['counterplay', 'deny counterplay', 'denying counterplay', 'cannot breathe', 'clamp'])
          ? 'counterplay_denial'
          : dominantIdeaKind === 'pawn_break' || hasAny(['break', 'hook', 'pawn storm'])
            ? 'break_preparation'
            : dominantIdeaKind === 'favorable_trade_or_transformation' || hasAny(['cash out', 'transition', 'transform', 'trade down', 'conversion'])
              ? 'conversion_window'
              : 'defender_tied_down';
  const delayed = hasAny(['delayed recovery', 'delay recovery']);
  const recoveryPolicy: ProbeCompensationSubtype['recoveryPolicy'] =
    delayed && ['line_occupation', 'target_fixing', 'counterplay_denial', 'defender_tied_down'].includes(pressureMode)
      ? 'intentionally_deferred'
      : delayed || ['line_occupation', 'target_fixing', 'counterplay_denial'].includes(pressureMode)
        ? 'delayed'
        : 'immediate';
  const stabilityClass: ProbeCompensationSubtype['stabilityClass'] =
    pressureMode === 'conversion_window' || hasAny(['cash out', 'transition window'])
      ? 'transition_only'
      : ['line_occupation', 'target_fixing', 'counterplay_denial'].includes(pressureMode) || recoveryPolicy === 'intentionally_deferred'
        ? 'durable_pressure'
        : 'tactical_window';
  return { pressureMode, recoveryPolicy, stabilityClass };
}

function hasDurableCompensationSubtype(moment: ProbePlanningMoment): boolean {
  return deriveCompensationSubtype(moment)?.stabilityClass === 'durable_pressure';
}

function hasQuietCompensationSubtype(moment: ProbePlanningMoment): boolean {
  const subtype = deriveCompensationSubtype(moment);
  return !!(
    subtype?.stabilityClass === 'durable_pressure' &&
    ['line_occupation', 'target_fixing', 'counterplay_denial'].includes(subtype.pressureMode)
  );
}

function probeMomentPriority(moment: ProbePlanningMoment): [number, number, number, number] {
  const compensation = hasCompensationCarrier(moment);
  const quietCompensation = hasQuietCompensationSubtype(moment);
  const durableCompensation = hasDurableCompensationSubtype(moment);
  const strategic = hasStrategicCarrier(moment);
  const priority =
    quietCompensation && moment.strategicBranch
      ? 0
      : durableCompensation && moment.strategicBranch
        ? 1
        : compensation && moment.strategicBranch
          ? 2
          : moment.strategicBranch && strategic
            ? 3
            : quietCompensation
              ? 4
              : moment.selectionKind === 'key' && strategic
                ? 5
                : compensation
                  ? 6
                  : strategic
                    ? 7
                    : 8;
  const salience =
    moment.strategicSalience === 'High'
      ? 0
      : moment.strategicSalience === 'Medium'
        ? 1
        : 2;
  const selectionKind =
    moment.selectionKind === 'key'
      ? 0
      : moment.selectionKind === 'thread_bridge'
        ? 1
        : 2;
  const refinementBias = Array.isArray(moment.probeRefinementRequests) && moment.probeRefinementRequests.length ? 0 : 1;
  return [priority, salience, selectionKind, refinementBias];
}

export function collectGameArcProbeMomentBundles(
  response: ProbePlanningResponse,
  maxMoments = 3,
): ProbeMomentBundle[] {
  const seenRequests = new Set<string>();
  const moments = Array.isArray(response?.moments)
    ? response.moments.slice().sort((a, b) => {
        const aPriority = probeMomentPriority(a);
        const bPriority = probeMomentPriority(b);
        for (let i = 0; i < aPriority.length; i++) {
          if (aPriority[i] !== bPriority[i]) return aPriority[i]! - bPriority[i]!;
        }
        return a.ply - b.ply;
      })
    : [];
  const bundles: ProbeMomentBundle[] = [];

  for (const moment of moments) {
    const dedupedRequests: ProbeRequest[] = [];
    for (const request of requestSource(moment)) {
      const key = probeRequestDedupKey(request);
      if (seenRequests.has(key)) continue;
      seenRequests.add(key);
      dedupedRequests.push(request);
      break;
    }
    if (!dedupedRequests.length) continue;
    bundles.push({
      ply: moment.ply,
      baselineCp: momentBaselineCp(moment),
      requests: dedupedRequests,
    });
    if (bundles.length >= maxMoments) break;
  }

  return bundles;
}

export function buildProbeResultsByPlyEntries(
  entries: ProbeResultsByPlyEntry[],
): ProbeResultsByPlyEntry[] {
  return entries
    .filter(entry => typeof entry?.ply === 'number' && entry.ply > 0)
    .map(entry => ({
      ply: entry.ply,
      results: (Array.isArray(entry.results) ? entry.results : []).filter(Boolean),
    }))
    .filter(entry => entry.results.length > 0);
}

export function countSurfacedEvidenceMoments(response: ProbePlanningResponse): number {
  const moments = Array.isArray(response?.moments) ? response.moments : [];
  return moments.filter(
    moment =>
      (Array.isArray(moment.probeRequests) && moment.probeRequests.length > 0) ||
      (Array.isArray(moment.authorQuestions) && moment.authorQuestions.length > 0) ||
      (Array.isArray(moment.authorEvidence) && moment.authorEvidence.length > 0),
  ).length;
}
