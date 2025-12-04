import { PracticalityScore } from "../types/review";

const categoryTone: Record<string, string> = {
  "Human-Friendly": "Easy to play and forgiving for humans",
  Challenging: "Playable but demands steady accuracy",
  "Engine-Like": "Strong line but feels computer-driven",
  "Computer-Only": "Unforgiving line that expects engine-level precision"
};

function describeRobustness(robustness: number): string {
  if (robustness >= 0.75) return "Multiple moves keep the eval stable";
  if (robustness >= 0.4) return "Stays okay if you stay in the main idea";
  return "Brittle line: only the top move survives";
}

function describeHorizon(horizon: number): string {
  if (horizon >= 0.75) return "Tactics are near the surface";
  if (horizon >= 0.4) return "Requires some calculation to stay afloat";
  return "Threats sit deep in the horizon";
}

function describeNaturalness(naturalness: number): string {
  if (naturalness >= 0.75) return "Moves look natural and human";
  if (naturalness >= 0.4) return "Playable but can feel non-obvious";
  return "Likely feels anti-intuitive or sac-heavy";
}

export function getPracticalityNarrative(
  practicality: PracticalityScore | undefined,
  comment: string | undefined
): string {
  const note = comment?.trim() ?? "";
  if (!practicality && !note) return "";
  if (!practicality) return note;

  const { category, robustness, horizon, naturalness, overall } = practicality;
  const parts: string[] = [];

  parts.push(categoryTone[category] ?? `Practicality: ${category}`);

  const insights = [describeRobustness(robustness), describeHorizon(horizon), describeNaturalness(naturalness)].filter(
    Boolean
  ) as string[];
  if (insights.length) parts.push(insights.join("; "));

  parts.push(`Overall ${overall.toFixed(2)}`);

  if (note) parts.push(note);

  return parts.join(" | ");
}
