package lila.llm

import lila.llm.analysis.ActiveStrategicCoachingBriefBuilder

object ActiveStrategicPrompt:

  val systemPrompt: String =
    """You polish one deterministic strategic coaching draft for a critical branch
      |moment in a full PGN review.
      |
      |## ROLE
      |- This is optional polish over a deterministic active-note draft.
      |- Do not invent a fresh thesis, new plan family, new compensation story, or
      |  new strategic theater.
      |- Preserve the deterministic draft's dominant idea, concrete anchor, and
      |  forward-looking follow-up unless the supplied repair reasons explicitly
      |  require tighter wording.
      |
      |## HARD RULES
      |1. Return exactly one compact note in 2-3 sentences, usually about 50-90 words.
      |2. Keep the note forward-looking strategic coaching, not a recap of the position.
      |3. Improve wording, compression, and sentence flow only. Do not widen or replace
      |   the deterministic draft's strategic contract.
      |4. Preserve the same campaign owner, dominant idea, compensation family, and
      |   theater/mode when they are already present in the deterministic draft or
      |   coaching brief.
      |5. Keep one concrete anchor and one concrete continuation when the draft already
      |   has them. Do not rewrite the note into vague plan talk.
      |6. Stay grounded in the supplied deterministic draft and coaching brief. Do not
      |   invent facts, moves, evaluations, or lines that are not provided.
      |7. Never expose internal handles or raw metadata in the prose: route ids,
      |   literal move labels, source tags, or raw UCI strings.
      |8. If the brief shows an immediate tactical or material gain, keep that concrete
      |   fact near the start rather than burying it.
      |9. Do not describe an already occupied friendly square as if it still needs to
      |   be newly occupied. When the relevant piece is already there, describe holding,
      |   anchoring, or maintaining that post.
      |10. No markdown headers, no bullet lists, no metadata wrappers.
      |11. Write natural English prose only inside the JSON field.
      |12. If the deterministic draft is already valid, prefer a light rewrite over a
      |    new sentence plan.
      |
      |## OUTPUT FORMAT
      |Return JSON with one field only:
      |{ "commentary": "<strategic note>" }""".stripMargin

  private def optionalLine(label: String, value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty).map(v => s"$label: $v")

  private def section(title: String, body: String): Option[String] =
    Option(body)
      .map(_.trim)
      .filter(value => value.nonEmpty && value != "none" && value != "- none")
      .map(value => s"## $title\n$value")

  private def rewriteGuardrails(
      draftNote: String,
      brief: ActiveStrategicCoachingBriefBuilder.Brief
  ): String =
    List(
      Some(
        "- Preserve the deterministic draft's dominant idea as the thesis. Do not swap campaign owner, theater, or compensation mode."
      ),
      Some(
        "- Keep route/objective language as supporting evidence only; do not let route wording replace the thesis."
      ),
      Some(
        "- Keep the concrete anchor and the forward-looking continuation if the draft already has them."
      ),
      Option.when(brief.executionHint.isDefined && brief.longTermObjective.isDefined)(
        "- Keep at most one of the execution hint or the long-term objective explicit if compact wording is enough."
      ),
      brief.whyNow
        .filter(hasImmediateTacticalCue)
        .map(value => s"- Keep this immediate tactical/material fact near the start when possible: $value"),
      brief.whyNow
        .filterNot(hasImmediateTacticalCue)
        .map(value => s"- Preserve this why-now evidence if it fits naturally: $value"),
      brief.opponentReply.map(value => s"- Preserve the opponent resource or reply to watch when it is already grounded: $value"),
      brief.keyTrigger.map(value => s"- Preserve the key trigger or failure mode if it is already grounded: $value"),
      Option.when(draftNote.trim.nonEmpty)(
        "- This is a rewrite pass over the deterministic draft, not a fresh independent note."
      )
    ).flatten.mkString("\n")

  def buildPrompt(
      draftNote: String,
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val coachingBrief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, moveRefs, Some(fen))
    val coachingBriefBlock =
      coachingBrief.nonEmptySections
        .map { case (label, value) => s"- $label: $value" }
        .mkString("\n")
    val contextLines =
      List(
        Some("## MOMENT CONTEXT"),
        Some(s"Phase: $phase"),
        Some(s"Moment Type: $momentType"),
        optionalLine("FEN", Option(fen).map(_.trim).filter(_.nonEmpty)),
        Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}")
      ).flatten.mkString("\n")
    val sections =
      List(
        Some(contextLines),
        section("COACHING BRIEF", coachingBriefBlock),
        section("DETERMINISTIC DRAFT", draftNote),
        section("REWRITE GUARDRAILS", rewriteGuardrails(draftNote, coachingBrief)),
        Some(
          "Rewrite the deterministic draft into one polished active note. Keep the same strategic contract, keep it attach-worthy, and improve wording or flow only."
        )
      ).flatten

    sections.mkString("\n\n")

  def buildRepairPrompt(
      draftNote: String,
      rejectedPolish: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val reasons = failureReasons.map(_.trim).filter(_.nonEmpty).distinct
    val reasonLine = if reasons.isEmpty then "format_or_content_violation" else reasons.mkString(", ")
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val coachingBrief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, moveRefs, Some(fen))
    val coachingBriefBlock =
      coachingBrief.nonEmptySections
        .map { case (label, value) => s"- $label: $value" }
        .mkString("\n")
    val contextLines =
      List(
        Some("## MOMENT CONTEXT"),
        Some(s"Phase: $phase"),
        Some(s"Moment Type: $momentType"),
        optionalLine("FEN", Option(fen).map(_.trim).filter(_.nonEmpty)),
        Option.when(conceptStr.nonEmpty)(s"Concepts: ${conceptStr.mkString(", ")}")
      ).flatten.mkString("\n")
    val sections =
      List(
        Some(contextLines),
        section("COACHING BRIEF", coachingBriefBlock),
        section("DETERMINISTIC DRAFT", draftNote),
        section("REJECTED POLISH", rejectedPolish),
        Some(s"## REPAIR REASONS\n$reasonLine"),
        section("REWRITE GUARDRAILS", rewriteGuardrails(draftNote, coachingBrief)),
        Some(
          "Return to the deterministic draft and repair the rejected polish. Keep the same dominant idea, anchor, and follow-up plan; improve wording only and satisfy the listed repair reasons."
        )
      ).flatten

    sections.mkString("\n\n")

  private def hasImmediateTacticalCue(text: String): Boolean =
    val normalized = Option(text).getOrElse("").trim.toLowerCase
    normalized.contains("immediately wins") || normalized.contains("forces the issue immediately")
