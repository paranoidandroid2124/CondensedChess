import { PracticalityScore } from "../types/review";

/**
 * Generates a practicality-based narrative for a chess move.
 * 
 * Design principle: LLM comment takes priority for real narrative content.
 * Template fallback is kept minimal (one-liner hint level) to avoid repetition fatigue.
 */
export function getPracticalityNarrative(
  practicality: PracticalityScore | undefined,
  comment: string | undefined
): string {
  // Priority 1: LLM-generated comment (rich, contextual narrative)
  if (comment?.trim()) return comment.trim();

  // Priority 2: No practicality → empty (badge will show category)
  if (!practicality) return "";

  // Priority 3: Minimal one-liner fallback (hint-level only)
  const category = practicality.categoryPersonal || practicality.categoryGlobal || practicality.category;

  const categoryOneLiner: Record<string, string> = {
    "Computer-Only": "Practically unplayable for humans",
    "Engine-Like": "Requires engine-level calculation",
    "Challenging": "Difficult but studiable",
    "Human-Friendly": "Playable with pattern recognition"
  };

  return categoryOneLiner[category] || "";
}
