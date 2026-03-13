import type { NarrativeSignalDigest } from './signalTypes';

function splitWords(text: string): string[] {
  return text
    .split(/\s+/)
    .map(t => t.trim())
    .filter(Boolean);
}

export function humanizeToken(raw: string): string {
  return splitWords(raw.replace(/[_-]+/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2'))
    .map(word => (word.length <= 2 ? word.toUpperCase() : word.charAt(0).toUpperCase() + word.slice(1)))
    .join(' ');
}

export function formatEvidenceStatus(status: string): string {
  switch (status.trim().toLowerCase()) {
    case 'resolved':
      return 'Resolved';
    case 'pending':
      return 'Pending';
    case 'question_only':
      return 'Heuristic';
    default:
      return humanizeToken(status);
  }
}

export function formatEvidenceScore(evalCp?: number | null, mate?: number | null): string {
  if (typeof mate === 'number') return `mate ${mate}`;
  if (typeof evalCp === 'number') return `${evalCp >= 0 ? '+' : ''}${evalCp}cp`;
  return '';
}

export function formatDeploymentSummary(signalDigest: NarrativeSignalDigest): string | null {
  if (!signalDigest.deploymentPiece || !signalDigest.deploymentPurpose) return null;
  const route = (signalDigest.deploymentRoute || []).filter(Boolean);
  const surfaceMode = signalDigest.deploymentSurfaceMode || 'hidden';
  if (surfaceMode === 'hidden') return null;
  const destination = route[route.length - 1] || '';
  const lead =
    surfaceMode === 'exact'
      ? `${signalDigest.deploymentPiece} via ${route.join('-')}`
      : `${signalDigest.deploymentPiece} toward ${destination || 'the target square'}`;
  const contribution = signalDigest.deploymentContribution?.trim();
  return [lead, `for ${signalDigest.deploymentPurpose}`, contribution].filter(Boolean).join(' · ');
}
