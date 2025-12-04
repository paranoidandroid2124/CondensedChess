import { CriticalNode } from "../types/review";

const tagDisplayMap: Record<string, string> = {
  opening_theory_branch: "Opening branch",
  plan_change: "Plan change",
  endgame_transition: "Transition to endgame",
  shift_tactical_to_positional: "Tactical → positional",
  fortress_building: "Fortress building",
  king_exposed: "King exposed",
  conversion_difficulty: "Conversion difficulty",
  positional_sacrifice: "Positional sacrifice",
  weak_back_rank: "Weak back rank",
  weak_f2: "Weak f2",
  weak_f7: "Weak f7",
  tactical_miss: "Missed tactic",
  greedy: "Greedy capture",
  positional_trade_error: "Bad piece trade",
  ignored_threat: "Ignored threat"
};

export function humanizeTag(text?: string): string {
  if (!text) return "";
  const spaced = text
    .replace(/_/g, " ")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/\s+/g, " ")
    .trim();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

export function displayTag(text?: string): string {
  if (!text) return "";
  const key = text.trim();
  if (tagDisplayMap[key]) return tagDisplayMap[key];
  return humanizeTag(text);
}

export function phaseOf(c: CriticalNode) {
  if (c.phaseLabel) return humanizeTag(c.phaseLabel);
  if (c.reason?.toLowerCase().startsWith("phase shift:")) return humanizeTag(c.reason.split(":").slice(1).join(":"));
  const tagMatch = c.tags?.find((t) => t.includes("transition") || t.includes("_to_") || t.includes("phase"));
  return displayTag(tagMatch);
}
