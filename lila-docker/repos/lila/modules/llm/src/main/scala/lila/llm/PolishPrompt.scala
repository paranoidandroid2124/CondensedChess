package lila.llm

import lila.llm.analysis.BookmakerPolishSlots

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

  private def claimOpeningClause(claim: String): String =
    val trimmed = Option(claim).map(_.trim).getOrElse("")
    val stripped = trimmed.replaceFirst("""^\d+\.(?:\.\.)?\s+[^:]+:\s*""", "").trim
    val punctuated =
      List(",", ";", ":")
        .flatMap(mark => Option.when(stripped.contains(mark))(stripped.indexOf(mark)))
        .sorted
        .headOption
        .map(idx => stripped.take(idx).trim)
        .filter(_.nonEmpty)
    punctuated.getOrElse {
      stripped.split("\\s+").take(9).mkString(" ").trim
    }

  private def optionalLine(label: String, value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map(v => s"$label: $v")

  private def contextHeader(
      phase: String,
      deltaStr: String,
      nature: Option[String],
      tension: Option[Double]
  ): String =
    val natureValue = nature.map(_.trim).filter(v => v.nonEmpty && !v.equalsIgnoreCase("unknown"))
    val tensionValue = tension.map(t => f"$t%.2f")
    val suffix =
      (natureValue, tensionValue) match
        case (Some(n), Some(t)) => s" | Nature: $n (tension: $t)"
        case (Some(n), None)    => s" | Nature: $n"
        case (None, Some(t))    => s" | Tension: $t"
        case _                  => ""
    s"Phase: $phase | Eval Δ: $deltaStr$suffix"

  private def contextLines(
      phase: String,
      deltaStr: String,
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      nature: Option[String],
      tension: Option[Double],
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      momentTypeStr: String,
      paragraphPlan: Option[String] = None
  ): String =
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(6)
    val openingStr = openingName.map(_.trim).filter(v => v.nonEmpty && !v.equalsIgnoreCase("unknown"))
    val fenStr = Option(fen).map(_.trim).filter(_.nonEmpty)
    val salienceStr = salience.map(_.toString).filter(v => v.nonEmpty && !v.equalsIgnoreCase("High"))
    (
      List(
        Some(contextHeader(phase, deltaStr, nature, tension)),
        optionalLine("Opening", openingStr),
        Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}"),
        optionalLine("FEN", fenStr),
        optionalLine("Salience", salienceStr),
        Some(s"Context Mode: $momentTypeStr"),
        paragraphPlan.map(plan => s"Paragraph Plan: $plan")
      ).flatten
    ).mkString("\n")

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
      |3. Improve wording only: remove template filler and merge redundant phrasing,
      |   but preserve the draft's dominant strategic claim and cause -> effect chain instead of flattening them into generic advantage language.
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
      momentType: Option[String] = None,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val momentTypeStr = momentType.map(m => s"Key Moment ($m) - Part of Full Game Review").getOrElse("Isolated Move")
    val modeReminder = bookmakerParagraphReminder(momentType)
    bookmakerSlots match
      case Some(slots) =>
        val supportText = slots.support.mkString("\n")
        val claimLead = claimOpeningClause(slots.claim)
        val tensionSection = slots.tension.map(t => s"\n## SLOT TENSION\n$t").getOrElse("")
        val evidenceSection = slots.evidenceHook.map(e => s"\n## SLOT EVIDENCE\n$e").getOrElse("")
        val codaSection = slots.coda.map(c => s"\n## SLOT CODA\n$c").getOrElse("")
        val guardrailText =
          if slots.factGuardrails.nonEmpty then slots.factGuardrails.mkString("\n")
          else "Keep the move header/SAN and any exact branch/eval tokens already implied by the slots."
        val paragraphPlan =
          if slots.paragraphPlan.nonEmpty then slots.paragraphPlan.mkString(", ") else "p1=claim, p2=support_chain"
        val contextBlock =
          contextLines(
            phase = phase,
            deltaStr = deltaStr,
            concepts = concepts,
            fen = fen,
            openingName = openingName,
            nature = nature,
            tension = tension,
            salience = salience,
            momentTypeStr = momentTypeStr,
            paragraphPlan = Some(paragraphPlan)
          )
        s"""## REQUEST
           |Turn the slots into polished commentary following the system instructions.
           |
           |## CONTEXT
           |$contextBlock
           |
           |If anchor tokens like [[MV_*]], [[MK_*]], [[EV_*]], or [[VB_*]] appear in the guardrails, preserve them exactly.
           |
           |$proseModeReminder
           |$modeReminder
           |Keep paragraph order aligned to the slots: claim first, then the support chain, then optional tension/evidence, then optional coda.
           |Paragraph 1 must begin with this exact opening clause: "$claimLead".
           |Keep the slot claim's opening clause intact rather than paraphrasing it into a generic restatement.
           |
           |## SLOT CLAIM
           |${slots.claim}
           |
           |## SLOT SUPPORT
           |$supportText$tensionSection$evidenceSection$codaSection
           |
           |## FACT GUARDRAILS
           |$guardrailText""".stripMargin
      case None =>
        val contextBlock =
          contextLines(
            phase = phase,
            deltaStr = deltaStr,
            concepts = concepts,
            fen = fen,
            openingName = openingName,
            nature = nature,
            tension = tension,
            salience = salience,
            momentTypeStr = momentTypeStr
          )
        s"""## REQUEST
           |Refine the draft commentary below following the system instructions.
           |
           |## CONTEXT
           |$contextBlock
           |
           |If anchor tokens like [[MV_*]], [[MK_*]], [[EV_*]], or [[VB_*]] appear in the draft, preserve them exactly.
           |
           |$proseModeReminder
           |$modeReminder
           |
           |## DRAFT COMMENTARY
           |$prose""".stripMargin

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
      allowedSans: List[String] = Nil,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
  ): String =
    val deltaStr = evalDelta.map(d => s"$d cp").getOrElse("N/A")
    val allowedSansStr =
      allowedSans
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(28)
        .mkString(", ")
    bookmakerSlots match
      case Some(slots) =>
        val supportText = slots.support.mkString("\n")
        val claimLead = claimOpeningClause(slots.claim)
        val tensionSection = slots.tension.map(t => s"\n## SLOT TENSION\n$t").getOrElse("")
        val evidenceSection = slots.evidenceHook.map(e => s"\n## SLOT EVIDENCE\n$e").getOrElse("")
        val codaSection = slots.coda.map(c => s"\n## SLOT CODA\n$c").getOrElse("")
        val guardrailText =
          if slots.factGuardrails.nonEmpty then slots.factGuardrails.mkString("\n")
          else originalProse
        val contextBlock =
          contextLines(
            phase = phase,
            deltaStr = deltaStr,
            concepts = concepts,
            fen = fen,
            openingName = openingName,
            nature = None,
            tension = None,
            salience = None,
            momentTypeStr = "Repair"
          )
        s"""## REQUEST
           |Repair REJECTED_POLISH into a strict-valid final commentary.
           |
           |## CONTEXT
           |$contextBlock
           |
           |Anchor tokens ([[MV_*]], [[MK_*]], [[EV_*]], [[VB_*]]) must be preserved exactly and in-order.
           |
           |$proseModeReminder
           |If the original draft is isolated-move / Bookmaker prose, keep 2-4 short paragraphs with 1-3 sentences each.
           |Paragraph 1 must begin with this exact opening clause: "$claimLead".
           |Keep the slot claim's opening clause intact rather than paraphrasing it into a generic restatement.
           |
           |## STRICT REPAIR REQUIREMENTS
           |- Keep factual claims from the slots.
           |- Keep concrete line SAN token order exactly valid for the guardrails.
           |- Do not add SAN tokens beyond this allowed extension set: ${if allowedSansStr.nonEmpty then allowedSansStr else "none"}.
           |- Preserve move numbering, branch labels, eval tokens, and marker style for concrete lines.
           |- Restore the dominant strategic claim in paragraph 1 and keep the support chain in paragraph 2.
           |- Keep the prose concise and natural; remove repetitive template artifacts.
           |
           |## SLOT CLAIM
           |${slots.claim}
           |
           |## SLOT SUPPORT
           |$supportText$tensionSection$evidenceSection$codaSection
           |
           |## FACT GUARDRAILS
           |$guardrailText
           |
           |## REJECTED_POLISH
           |$rejectedPolish""".stripMargin
      case None =>
        val contextBlock =
          contextLines(
            phase = phase,
            deltaStr = deltaStr,
            concepts = concepts,
            fen = fen,
            openingName = openingName,
            nature = None,
            tension = None,
            salience = None,
            momentTypeStr = "Repair"
          )
        s"""## REQUEST
           |Repair REJECTED_POLISH into a strict-valid final commentary.
           |
           |## CONTEXT
           |$contextBlock
           |
           |Anchor tokens ([[MV_*]], [[MK_*]], [[EV_*]], [[VB_*]]) must be preserved exactly and in-order.
           |
           |$proseModeReminder
           |If the original draft is isolated-move / Bookmaker prose, keep 2-4 short paragraphs with 1-3 sentences each.
           |
           |## STRICT REPAIR REQUIREMENTS
           |- Keep factual claims from ORIGINAL_DRAFT.
           |- Keep concrete line SAN token order exactly valid for ORIGINAL_DRAFT context.
           |- Do not add SAN tokens beyond this allowed extension set: ${if allowedSansStr.nonEmpty then allowedSansStr else "none"}.
           |- Preserve move numbering and marker style for concrete lines (e.g., keep `17...`, do not mutate to `17.`).
           |- Keep the prose concise and natural; remove repetitive template artifacts.
           |
           |## ORIGINAL_DRAFT
           |$originalProse
           |
           |## REJECTED_POLISH
           |$rejectedPolish""".stripMargin

  /** Estimate token count for the system prompt (for cost analysis). */
  val estimatedSystemTokens: Int = 1500

  /** Estimate per-request input tokens (prose + context, excluding system). */
  def estimateRequestTokens(input: String): Int =
    // Rough heuristic: 1 token ≈ 4 characters for English text
    val proseTokens = (input.length / 4.0).toInt
    val overheadTokens = 80 // context fields, formatting
    proseTokens + overheadTokens
