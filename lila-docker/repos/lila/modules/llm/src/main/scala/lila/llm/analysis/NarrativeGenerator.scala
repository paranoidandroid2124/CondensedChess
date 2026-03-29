package lila.llm

import lila.llm.model.strategic.*

/**
 * Narrative Generator
 *
 * Converts raw analysis data into structured text blocks for the LLM prompt.
 * Focuses on "what to say" so the LLM can focus on "how to say it".
 */
object NarrativeGenerator:
  // 1. POSITION STRUCTURE NARRATIVE
  // 2. VARIATION NARRATIVE (Multi-PV)

  private def normalizedScoreDisplay(line: VariationLine): String = line.mate match
    case Some(m) if m > 0 => s"Mate in $m"
    case Some(m) if m < 0 => s"Mated in ${-m}"
    case _                =>
      val pawns = line.scoreCp / 100.0
      f"Eval: $pawns%.1f"

  def describeVariations(lines: List[VariationLine]): String =
    val parts = List.newBuilder[String]

    lines.headOption.foreach { main =>
      val scoreText = s"(${normalizedScoreDisplay(main)})"

      val mainMotifs =
        if main.tags.contains(VariationTag.Prophylaxis) then "is a strong prophylactic decision"
        else if main.tags.contains(VariationTag.Simplification) then "simplifies into a winning ending"
        else if main.tags.contains(VariationTag.Sharp) then "leads to complications"
        else "is the most principled choice"

      parts += s"**MAIN LINE** $scoreText: ${main.moves.take(6).mkString(" ")} — $mainMotifs."
    }

    val alts = lines.drop(1).take(3)
    if alts.nonEmpty then
      parts += "\n**ALTERNATIVE LINES:**"
      alts.foreach { alt =>
        val scoreText = normalizedScoreDisplay(alt)
        val tagText = if alt.tags.nonEmpty then alt.tags.map(t => s"[${t.toString}]").mkString(" ") else ""
        val assessment =
          if alt.tags.contains(VariationTag.Blunder) then "— blunder"
          else if alt.tags.contains(VariationTag.Mistake) then "— problematic"
          else if alt.tags.contains(VariationTag.Good) then "— playable"
          else if alt.tags.contains(VariationTag.Excellent) then "— strong alternative"
          else ""

        parts += s"  • ${alt.moves.take(6).mkString(" ")} ($scoreText) $tagText $assessment"
      }

    parts.result().mkString("\n")
