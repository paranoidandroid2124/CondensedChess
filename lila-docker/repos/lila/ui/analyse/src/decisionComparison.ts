export type DecisionComparisonDigestLike = {
  chosenMove?: string;
  engineBestMove?: string;
  engineBestScoreCp?: number;
  engineBestPv?: string[];
  cpLossVsChosen?: number;
  deferredMove?: string;
  deferredReason?: string;
  deferredSource?: string;
  evidence?: string;
  practicalAlternative?: boolean;
  chosenMatchesBest?: boolean;
};

export type DecisionComparisonRow = {
  label: string;
  value: string;
};

export type DecisionComparisonSurface = {
  headline: string | null;
  secondary: string | null;
  engineLine: string | null;
  evidence: string | null;
  gap: string | null;
  chosenMatchesBest: boolean;
};

type RowOptions = {
  includeEngineLine?: boolean;
  includeEvidence?: boolean;
  mergeReasonIntoDeferred?: boolean;
};

function formatCp(cp?: number): string {
  if (typeof cp !== 'number') return '';
  return `${cp >= 0 ? '+' : ''}${cp}cp`;
}

function normalizeList(items?: string[]): string[] {
  return (items || []).map(item => item.trim()).filter(Boolean);
}

function buildDeferredLabel(comparison: DecisionComparisonDigestLike): string {
  return comparison.practicalAlternative ? 'Practical alternative' : 'Deferred branch';
}

export function formatDecisionComparisonHeadline(comparison?: DecisionComparisonDigestLike | null): string | null {
  if (!comparison) return null;

  const chosen = comparison.chosenMove?.trim();
  const best = comparison.engineBestMove?.trim();
  const gap = formatCp(comparison.cpLossVsChosen);

  if (comparison.chosenMatchesBest && chosen) return `Chosen ${chosen} matches the engine best.`;
  if (chosen && best) return `Chosen ${chosen} vs engine best ${best}${gap ? ` (${gap} gap)` : ''}.`;
  if (best) return `Engine best is ${best}${gap ? ` (${gap})` : ''}.`;
  if (chosen) return `Chosen move ${chosen}.`;
  return null;
}

export function buildDecisionComparisonRows(
  comparison?: DecisionComparisonDigestLike | null,
  opts: RowOptions = {},
): DecisionComparisonRow[] {
  if (!comparison) return [];

  const includeEngineLine = opts.includeEngineLine ?? true;
  const includeEvidence = opts.includeEvidence ?? true;
  const mergeReasonIntoDeferred = opts.mergeReasonIntoDeferred ?? false;
  const rows: DecisionComparisonRow[] = [];

  const engineLine = normalizeList(comparison.engineBestPv).slice(0, 4).join(' ');
  if (includeEngineLine && engineLine) rows.push({ label: 'Engine line', value: engineLine });

  const deferredMove = comparison.deferredMove?.trim();
  const deferredReason = comparison.deferredReason?.trim();
  if (deferredMove) {
    const label = comparison.practicalAlternative ? 'Practical alternative' : 'Deferred branch';
    const value =
      mergeReasonIntoDeferred && deferredReason ? `${deferredMove} · ${deferredReason}` : deferredMove;
    rows.push({ label, value });
  }

  if (deferredReason && (!mergeReasonIntoDeferred || !deferredMove))
    rows.push({ label: 'Why deferred', value: deferredReason });

  if (includeEvidence && comparison.evidence?.trim())
    rows.push({ label: 'Evidence', value: comparison.evidence.trim() });

  return rows;
}

export function buildDecisionComparisonSurface(
  comparison?: DecisionComparisonDigestLike | null,
  opts: Pick<RowOptions, 'includeEngineLine' | 'includeEvidence'> = {},
): DecisionComparisonSurface {
  const empty: DecisionComparisonSurface = {
    headline: null,
    secondary: null,
    engineLine: null,
    evidence: null,
    gap: null,
    chosenMatchesBest: false,
  };
  if (!comparison) return empty;

  const chosen = comparison.chosenMove?.trim();
  const best = comparison.engineBestMove?.trim();
  const deferredMove = comparison.deferredMove?.trim();
  const deferredReason = comparison.deferredReason?.trim();
  const deferredLabel = buildDeferredLabel(comparison);
  const includeEngineLine = opts.includeEngineLine ?? true;
  const includeEvidence = opts.includeEvidence ?? true;
  const engineLine = includeEngineLine ? normalizeList(comparison.engineBestPv).slice(0, 4).join(' ') : '';
  const evidence = includeEvidence ? comparison.evidence?.trim() || '' : '';
  const gap = formatCp(comparison.cpLossVsChosen);

  let headline: string | null = null;
  if (comparison.chosenMatchesBest && chosen) headline = `Chosen ${chosen} · engine agrees`;
  else if (chosen && best) headline = `Chosen ${chosen} · Engine ${best}`;
  else if (best) headline = `Engine ${best}`;
  else if (chosen) headline = `Chosen ${chosen}`;

  let secondary: string | null = null;
  if (deferredMove && deferredReason) secondary = `${deferredLabel}: ${deferredMove} · ${deferredReason}`;
  else if (deferredMove) secondary = `${deferredLabel}: ${deferredMove}`;
  else if (deferredReason) secondary = `${deferredLabel}: ${deferredReason}`;
  else if (evidence && !engineLine) secondary = `Line: ${evidence}`;

  return {
    headline,
    secondary,
    engineLine: engineLine || null,
    evidence: evidence || null,
    gap: gap || null,
    chosenMatchesBest: !!comparison.chosenMatchesBest,
  };
}
