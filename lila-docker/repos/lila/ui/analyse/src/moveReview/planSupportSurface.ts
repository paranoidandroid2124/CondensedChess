import type { StrategicPlanExperiment } from './types';
import { cleanNarrativeSurfaceLabel } from '../chesstory/signalFormatting';

export type StrategicPlanLike = {
  planId?: string | null;
  subplanId?: string | null;
  planName?: string | null;
  score?: number | null;
  rank?: number | null;
};

export function strategicPlanExperimentKey(planId: string | null | undefined, subplanId?: string | null): string {
  const primary = typeof planId === 'string' ? planId.trim().toLowerCase() : '';
  const secondary = typeof subplanId === 'string' ? subplanId.trim().toLowerCase() : '';
  return `${primary}::${secondary}`;
}

export function strategicPlanExperimentIndex(
  experiments: StrategicPlanExperiment[],
): Map<string, StrategicPlanExperiment> {
  return new Map(
    experiments.map(experiment => [strategicPlanExperimentKey(experiment.planId, experiment.subplanId), experiment]),
  );
}

export function strategicPlanSupportBits(experiment: StrategicPlanExperiment | null | undefined): string[] {
  if (!experiment) return [];
  const bits: string[] = [];
  if (experiment.evidenceTier === 'evidence_backed') bits.push('Probe-backed');
  else if (experiment.evidenceTier === 'pv_coupled') bits.push('Engine-line only');
  if (experiment.moveOrderSensitive) bits.push('Move-order sensitive');
  return bits;
}

export function formatStrategicPlanText(
  plan: StrategicPlanLike,
  experimentIndex: Map<string, StrategicPlanExperiment>,
  opts: { includeRank?: boolean } = {},
): string {
  const name = typeof plan.planName === 'string' ? plan.planName.trim() : '';
  if (!name) return '';
  const cleanedName = cleanNarrativeSurfaceLabel(name);
  const experiment = experimentIndex.get(strategicPlanExperimentKey(plan.planId, plan.subplanId));
  const annotations = [
    typeof plan.score === 'number' ? plan.score.toFixed(2) : '',
    ...strategicPlanSupportBits(experiment),
  ].filter(Boolean);
  const prefix = opts.includeRank && typeof plan.rank === 'number' ? `${plan.rank}. ` : '';
  return annotations.length ? `${prefix}${cleanedName} (${annotations.join(' · ')})` : `${prefix}${cleanedName}`;
}
