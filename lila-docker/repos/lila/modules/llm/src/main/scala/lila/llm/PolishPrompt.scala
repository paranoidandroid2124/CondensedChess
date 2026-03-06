package lila.llm

/** System prompt and per-request polish prompt builder for LLM polishing.
  *
  * The system prompt is shared across providers and designed for prompt caching.
  */
object PolishPrompt:

  private val proseModeReminder: String =
    "Structure reminder: return plain prose only; do not emit UI section titles, bullets, or markdown; preserve draft paragraph breaks."

  private def bookmakerParagraphReminder(momentType: Option[String]): String =
    if momentType.isDefined then
      "When refining this narrative, preserve the draft paragraph rhythm unless a small edit improves clarity."
    else
      "For isolated-move / Bookmaker prose, keep 2-4 short paragraphs with 1-3 sentences each."

  /** Static system prompt cached on provider side.
    * Defines the AI's persona, refinement rules, and output format.
    */
  val systemPrompt: String =
    """You refine deterministic chess commentary into concise, natural prose.
      |
      |## CORE RULES
      |1. Preserve every factual claim from the draft. Never invent moves, lines,
      |   evaluations, historical references, or positional claims not supported by the draft/context.
      |2. Preserve structural tokens exactly when present:
      |   - SAN token order
      |   - move numbering / side-to-move markers such as `17...`
      |   - eval tokens and branch labels like `a)`, `b)`, `c)`
      |   - anchor tokens such as [[MV_*]], [[MK_*]], [[EV_*]], [[VB_*]]
      |3. Improve wording only: remove template filler, merge redundant phrasing,
      |   and add brief cause -> effect links where the draft already supports them.
      |4. Keep prose concrete. Name moves, squares, plans, structure, and piece roles explicitly
      |   when the draft already provides them.
      |5. Respect evaluation and salience:
      |   - never overstate the position relative to eval
      |   - if `Salience: Low` is provided, stay brief and mostly tactical
      |6. Handle special contexts only when supported:
      |   - opening theory, endgame technique, and precedent references may be clarified
      |   - do not fabricate precedent details
      |   - if an opening-family label conflicts with provided context, replace it with neutral structural wording
      |7. UI owns section headers and summary cards. Do not generate headings such as
      |   `Strategic Signals`, `Evidence Probes`, `Authoring Evidence`, or `Alternative Options`.
      |   Return plain prose only: no markdown headers, no bullet lists, no metadata wrapper text.
      |8. Preserve paragraph structure when possible. Do not collapse multi-paragraph draft text into
      |   one block. In isolated-move / Bookmaker mode, target 2-4 short paragraphs with 1-3 sentences each,
      |   usually: immediate move meaning -> strategic consequence -> optional line or practical nuance.
      |9. If the draft is already strong, make minimal changes. If it is empty or unusable,
      |   return one brief neutral observation grounded only in the provided context.
      |
      |## OUTPUT
      |Return only the polished commentary prose.
      |If an API-level schema is enforced, place the prose in the schema field and nothing else.""".stripMargin

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
      tension: Option[Double] = None,
      salience: Option[lila.llm.model.strategic.StrategicSalience] = None,
      momentType: Option[String] = None
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val conceptStr = if concepts.isEmpty then "none detected" else concepts.take(6).mkString(", ")
    val openingStr = openingName.filter(_.trim.nonEmpty).getOrElse("unknown")
    val natureStr = nature.getOrElse("unknown")
    val tensionStr = tension.map(t => f"$t%.2f").getOrElse("N/A")
    val salienceStr = salience.map(_.toString).getOrElse("High")
    val momentTypeStr = momentType.map(m => s"Key Moment ($m) - Part of Full Game Review").getOrElse("Isolated Move")
    val modeReminder = bookmakerParagraphReminder(momentType)

    s"""## DRAFT COMMENTARY
       |$prose
       |
       |## CONTEXT
       |Phase: $phase | Eval Δ: $deltaStr | Nature: $natureStr (tension: $tensionStr)
       |Opening: $openingStr
       |Concepts: $conceptStr
       |FEN: $fen
       |Salience: $salienceStr
       |Context Mode: $momentTypeStr
       |
       |If anchor tokens like [[MV_*]], [[MK_*]], [[EV_*]], or [[VB_*]] appear in the draft, preserve them exactly.
       |
       |$proseModeReminder
       |$modeReminder
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
       |$proseModeReminder
       |If the original draft is isolated-move / Bookmaker prose, keep 2-4 short paragraphs with 1-3 sentences each.
       |
       |Repair REJECTED_POLISH into a strict-valid final commentary.""".stripMargin

  /** Estimate token count for the system prompt (for cost analysis). */
  val estimatedSystemTokens: Int = 1500

  /** Estimate per-request input tokens (prose + context, excluding system). */
  def estimateRequestTokens(prose: String): Int =
    // Rough heuristic: 1 token ≈ 4 characters for English text
    val proseTokens = (prose.length / 4.0).toInt
    val overheadTokens = 80 // context fields, formatting
    proseTokens + overheadTokens
