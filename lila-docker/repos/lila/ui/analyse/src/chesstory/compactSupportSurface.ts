import type { NarrativeSignalDigest } from './signalTypes';
import {
  buildPlayerFacingSupportOptions,
  filterPlayerFacingValues,
  rewritePlayerFacingSupportText,
} from './signalFormatting';

export type CompactSupportRow = [string, string];

export type CompactSupportSurface = {
  mainPlanTexts: string[];
  rows: CompactSupportRow[];
};

type Args = {
  signalDigest: NarrativeSignalDigest | null | undefined;
  mainPlanTexts?: string[];
  deploymentSummary?: string | null;
};

export function buildCompactSupportSurface({
  signalDigest,
  mainPlanTexts = [],
  deploymentSummary,
}: Args): CompactSupportSurface {
  const digest = signalDigest || null;
  const supportOpts = buildPlayerFacingSupportOptions(digest);
  const practicalFactors = filterPlayerFacingValues((digest?.practicalFactors || []).filter(Boolean).slice(0, 2), supportOpts);
  const structureBits = filterPlayerFacingValues([
    digest?.structuralCue || '',
    digest?.structureProfile || '',
    digest?.centerState ? `${digest.centerState.toLowerCase()} center` : '',
  ], supportOpts);
  const structureValue = structureBits.length ? dedupeParts(structureBits).join(' · ') : '';
  const practicalValue = filterPlayerFacingValues([
    digest?.practicalVerdict || '',
    practicalFactors.join('; '),
  ], supportOpts).join(' · ');
  const rows: CompactSupportRow[] = [
    rewritePlayerFacingSupportText(digest?.opening, supportOpts)
      ? ['Opening', rewritePlayerFacingSupportText(digest?.opening, supportOpts)!]
      : null,
    rewritePlayerFacingSupportText(digest?.opponentPlan, supportOpts)
      ? ['Opponent', rewritePlayerFacingSupportText(digest?.opponentPlan, supportOpts)!]
      : null,
    structureValue ? ['Structure', structureValue] : null,
    rewritePlayerFacingSupportText(deploymentSummary, supportOpts)
      ? ['Piece deployment', rewritePlayerFacingSupportText(deploymentSummary, supportOpts)!]
      : null,
    practicalValue ? ['Practical', practicalValue] : null,
  ].filter(Boolean) as CompactSupportRow[];

  return {
    mainPlanTexts: filterPlayerFacingValues(mainPlanTexts, supportOpts),
    rows,
  };
}

function dedupeParts(values: string[]): string[] {
  const seen = new Set<string>();
  return values.filter(value => {
    const key = value.trim().toLowerCase();
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
