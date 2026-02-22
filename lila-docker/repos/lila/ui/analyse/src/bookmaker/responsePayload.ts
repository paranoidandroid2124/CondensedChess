import type { EvalVariation, PlanStateToken, ProbeRequest } from './types';

type MaybeResponse = {
  html?: unknown;
  commentary?: unknown;
  variations?: unknown;
  probeRequests?: unknown;
  planStateToken?: unknown;
  ratelimit?: {
    seconds?: unknown;
  };
  resetAt?: unknown;
};

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

export function planStateTokenFromResponse(data: MaybeResponse): PlanStateToken | null {
  return data?.planStateToken && typeof data.planStateToken === 'object'
    ? (data.planStateToken as PlanStateToken)
    : null;
}

export function ratelimitSecondsFromResponse(data: MaybeResponse): number | null {
  const seconds = data?.ratelimit?.seconds;
  return typeof seconds === 'number' ? seconds : null;
}

export function resetAtFromResponse(data: MaybeResponse): string {
  return typeof data?.resetAt === 'string' ? data.resetAt : 'Unknown';
}
