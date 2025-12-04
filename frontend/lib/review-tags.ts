import { CriticalNode } from "../types/review";

const tagDisplayMap: Record<string, string> = {
  // Study/Critical tags
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
  ignored_threat: "Ignored threat",

  // Semantic tags
  open_h_file: "Open H-File",
  open_g_file: "Open G-File",
  open_f_file: "Open F-File",
  open_e_file: "Open E-File",
  open_d_file: "Open D-File",
  open_c_file: "Open C-File",
  open_b_file: "Open B-File",
  open_a_file: "Open A-File",
  semi_open_h: "Semi-Open H",
  semi_open_g: "Semi-Open G",
  semi_open_f: "Semi-Open F",
  semi_open_e: "Semi-Open E",
  semi_open_d: "Semi-Open D",
  semi_open_c: "Semi-Open C",
  semi_open_b: "Semi-Open B",
  semi_open_a: "Semi-Open A",
  outpost_e5: "Outpost E5",
  outpost_d5: "Outpost D5",
  outpost_f5: "Outpost F5",
  outpost_c5: "Outpost C5",
  outpost_e4: "Outpost E4",
  outpost_d4: "Outpost D4",
  outpost_f4: "Outpost F4",
  outpost_c4: "Outpost C4",
  backward_pawn: "Backward Pawn",
  isolated_d_pawn: "Isolated D-Pawn",
  isolated_e_pawn: "Isolated E-Pawn",
  doubled_pawns: "Doubled Pawns",
  passed_pawn: "Passed Pawn",
  king_ring_pressure: "King Ring Pressure",
  opposite_color_bishops: "Opposite Bishops"
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
