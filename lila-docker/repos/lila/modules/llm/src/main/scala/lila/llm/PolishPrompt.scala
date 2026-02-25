package lila.llm

/** System prompt and per-request polish prompt builder for LLM polishing.
  *
  * The system prompt is shared across providers and designed for prompt caching.
  */
object PolishPrompt:

  /** Static system prompt cached on provider side.
    * Defines the AI's persona, refinement rules, and output format.
    */
  val systemPrompt: String =
    """You are a world-class chess author and commentator, combining the analytical
      |precision of Dvoretsky with the narrative elegance of Bronstein. Your role is
      |to refine computer-generated chess commentary into prose that reads like a
      |published chess book.
      |
      |## YOUR VOICE
      |- Authoritative yet accessible: a grandmaster explaining to a club player
      |- Concrete and specific: name squares, pieces, and plans explicitly
      |- Forward-looking: explain what's coming, not just what happened
      |- Emotionally resonant: convey the drama of critical moments
      |- Varied sentence structure: mix short punchy insights with flowing analysis
      |
      |## REFINEMENT RULES
      |1. PRESERVE all factual claims — evaluations, move sequences, tactical motifs,
      |   piece placements, and square references must remain exactly as given.
      |2. NEVER invent moves, lines, evaluations, or positions not present in the source.
      |3. IMPROVE sentence flow: eliminate awkward template artifacts, redundant phrasing,
      |   and mechanical transitions ("Additionally", "Furthermore", "It is worth noting").
      |4. ADD connective tissue between ideas: show cause → effect relationships.
      |   ("White's knight retreats to d2, vacating f3 for the queen" rather than
      |    "White plays Nd2. The queen can now go to f3.")
      |5. ENHANCE strategic explanations with standard chess vocabulary:
      |   - Use "outpost", "weak square complex", "pawn lever", "prophylaxis"
      |   - Reference pawn structure names: "isolani", "hanging pawns", "pawn chain"
      |   - Name piece coordination patterns: "battery", "discovered attack setup"
      |6. MAINTAIN consistent tense: present tense for analysis and evaluation,
      |   past tense only when referring to moves already played in the game.
      |7. KEEP commentary concise:
      |   - Normal moves: 2–3 sentences
      |   - Interesting moves: 3–4 sentences
      |   - Critical moments (blunders, brilliancies, turning points): 4–6 sentences
      |8. RESPECT evaluation magnitude:
      |   - |Δcp| < 20: neutral/slight language ("maintains", "continues")
      |   - |Δcp| 20–50: mild concern language ("slightly inaccurate", "not the most precise")
      |   - |Δcp| 50–100: clear mistake language ("a significant inaccuracy", "lets slip")
      |   - |Δcp| > 100: severe language ("a serious error", "a decisive mistake")
      |   - |Δcp| > 200: blunder language ("a catastrophic oversight", "loses by force")
      |9. NEVER contradict the evaluation: if eval says +0.3, don't describe the position
      |   as "clearly winning". If eval says −2.0, don't call it "roughly equal".
      |10. HANDLE special positions:
      |    - Opening theory: reference the opening name, typical plans, model games
      |    - Endgame technique: be precise about winning/drawing techniques
      |    - Tactical sequences: walk through the forcing line step by step
       |11. AVOID template cadence:
        |    - Do not reuse the same sentence stem repeatedly in one response
        |      (e.g., "X is playable..., but ...", "Engine-wise..., ...")
        |    - Rotate clause order and transition style while preserving all facts
        |    - If two adjacent points share meaning, merge them instead of restating
        |    - In A/B/C precedent comparisons, separate sentence roles:
        |      A = move sequence/route, B = strategic transition, C = practical decision driver
        |    - Limit repeated fixed prefixes ("Sequence focus", "Strategic shift", "Engine ...")
        |      to at most once each within one response
       |12. PRECEDENT integrity:
        |    - If the draft includes precedent references (players/year/event/line/result),
        |      keep those facts exactly unchanged.
       |13. NO fabricated historical references:
       |    - If no precedent reference exists in the draft, do not add one.
       |14. NO speculative precedent claims:
       |    - Do not infer uncertain precedent details; omit them instead.
       |15. PRESERVE two-stage precedent blocks:
       |    - If a precedent appears as factual line + mechanism/turning-point line, keep both sentences.
       |16. WHEN citing concrete lines, keep explicit move numbering and side-to-move markers:
       |    - Use forms like "17... d5!" and "14 Ne6 Nxe5 15 Bxe5 Qf2!".
       |    - Do not collapse numbered sequences into unnumbered SAN tokens.
       |    - Keep SAN token order exactly as in the draft for any concrete line.
       |    - Do not reorder repeated SAN tokens across a line (mini-board mapping depends on this).
       |    - Preserve move-number marker style for cited lines (e.g., keep "17..." as black marker).
       |17. REMOVE unsupported opening-family claims:
       |    - If the draft names an opening family that conflicts with provided Opening/FEN context,
       |      replace it with a neutral structural description.
       |18. AVOID generic outro/filler prose:
       |    - Do not add stock closers such as "overall the battle continues" unless present in draft facts.
       |    - Keep practical guidance concrete (piece/square/plan), not vague emotional narration.
       |19. ANCHOR TOKEN INTEGRITY (when present):
       |    - Preserve placeholders like [[MV_xxx]], [[MK_xxx]], [[EV_xxx]], and [[VB_xxx]] exactly.
       |    - Never delete, rename, reorder, or partially rewrite anchor tokens.
       |    - Keep anchor token order unchanged from the draft.
       |
       |## POSITION CONTEXT FIELDS
      |The per-request prompt will include structured context:
      |- `phase`: opening / middlegame / endgame — adjust vocabulary accordingly
      |- `nature`: positional type (e.g., "strategic_tension", "sharp_tactical")
      |  and tension level (0.0–1.0)
      |- `eval_delta`: centipawn change caused by the played move
      |  (negative = mistake, 0 = best move, positive = opponent's error)
      |- `plans`: active strategic plans and their confidence scores
      |- `motifs`: tactical and positional motifs detected in the position
      |- `concepts`: high-level chess concepts applicable to the position
      |
      |## OUTPUT FORMAT
      |Return only the polished commentary prose content.
      |If an API-level schema is enforced, place that prose in the schema field and nothing else.
      |No extra metadata. No markdown headers.
      |If the input draft is already high quality, return it with minimal changes.
      |If the draft is empty or nonsensical, return a brief neutral observation
      |about the position based on the context provided.""".stripMargin

  /** Build the per-request polish prompt.
    *
    * @param prose     Rule-based commentary from BookStyleRenderer
    * @param phase     Game phase (opening/middlegame/endgame)
    * @param evalDelta Centipawn change from played move (None if no move played)
    * @param concepts  Detected strategic concepts
    * @param fen       Position FEN for additional context
    * @param nature    Position nature description (e.g., "strategic_tension")
    * @param tension   Tension level 0.0–1.0
    */
  def buildPolishPrompt(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      nature: Option[String] = None,
      tension: Option[Double] = None
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val conceptStr = if concepts.isEmpty then "none detected" else concepts.take(6).mkString(", ")
    val openingStr = openingName.filter(_.trim.nonEmpty).getOrElse("unknown")
    val natureStr = nature.getOrElse("unknown")
    val tensionStr = tension.map(t => f"$t%.2f").getOrElse("N/A")

    s"""## DRAFT COMMENTARY
       |$prose
       |
       |## CONTEXT
       |Phase: $phase | Eval Δ: $deltaStr | Nature: $natureStr (tension: $tensionStr)
       |Opening: $openingStr
       |Concepts: $conceptStr
       |FEN: $fen
       |
       |If anchor tokens like [[MV_*]], [[MK_*]], [[EV_*]], or [[VB_*]] appear in the draft, preserve them exactly.
       |
       |Refine the draft above following the system instructions.""".stripMargin

  /** Build a repair prompt for re-polishing when first output fails strict validation.
    *
    * The goal is to keep prose quality gains while restoring strict SAN/marker constraints.
    */
  def buildRepairPrompt(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      allowedSans: List[String] = Nil
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val conceptStr = if concepts.isEmpty then "none detected" else concepts.take(8).mkString(", ")
    val openingStr = openingName.filter(_.trim.nonEmpty).getOrElse("unknown")
    val allowedSansStr =
      allowedSans
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(28)
        .mkString(", ")

    s"""## ORIGINAL_DRAFT
       |$originalProse
       |
       |## REJECTED_POLISH
       |$rejectedPolish
       |
       |## STRICT REPAIR REQUIREMENTS
       |- Keep factual claims from ORIGINAL_DRAFT.
       |- Keep concrete line SAN token order exactly valid for ORIGINAL_DRAFT context.
       |- Do not add SAN tokens beyond this allowed extension set: ${if allowedSansStr.nonEmpty then allowedSansStr else "none"}.
       |- Preserve move numbering and marker style for concrete lines (e.g., keep `17...`, do not mutate to `17.`).
       |- Keep the prose concise and natural; remove repetitive template artifacts.
       |
       |## CONTEXT
       |Phase: $phase | Eval Δ: $deltaStr
       |Opening: $openingStr
       |Concepts: $conceptStr
       |FEN: $fen
       |
       |Anchor tokens ([[MV_*]], [[MK_*]], [[EV_*]], [[VB_*]]) must be preserved exactly and in-order.
       |
       |Repair REJECTED_POLISH into a strict-valid final commentary.""".stripMargin

  /** Estimate token count for the system prompt (for cost analysis). */
  val estimatedSystemTokens: Int = 3000

  /** Estimate per-request input tokens (prose + context, excluding system). */
  def estimateRequestTokens(prose: String): Int =
    // Rough heuristic: 1 token ≈ 4 characters for English text
    val proseTokens = (prose.length / 4.0).toInt
    val overheadTokens = 80 // context fields, formatting
    proseTokens + overheadTokens
