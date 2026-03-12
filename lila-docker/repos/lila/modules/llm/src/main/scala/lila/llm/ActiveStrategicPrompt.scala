package lila.llm

import lila.llm.analysis.ActiveStrategicCoachingBriefBuilder

object ActiveStrategicPrompt:

  private final case class OpeningLens(
      label: String,
      instruction: String
  )

  val systemPrompt: String =
    """You are a grandmaster-level strategic coach writing a short forward-looking
      |note for one critical branch moment in a full PGN review.
      |
      |## GOAL
      |- Produce a compact strategic coaching note that tells the reader what plan
      |  should be carried forward next, why that dominant idea matters now, and
      |  what reply or trigger should be watched.
      |
      |## HARD RULES
      |1. Return exactly one compact note in 2-3 sentences, usually about 50-90 words.
      |2. Frame the note as forward-looking strategic coaching, not as a recap of the current position.
      |   The note must include at least one next plan, follow-up action, or long-term deployment.
      |3. Do not recycle long phrases, sentence openings, or generic framing from any prior note.
      |4. Keep the note narrow: emphasize one dominant strategic idea, plus at most
      |   one supporting execution hint or long-term objective.
      |5. When available in context, explain why this is the right moment for that plan:
      |   structure, timing, practical trigger, or opponent counterplay are all valid.
      |6. If a campaign role is provided, explain this moment as one role in the longer plan
      |   without surfacing raw stage labels.
      |7. Do not invent facts, moves, evaluations, or lines that are not provided.
      |8. Do not contradict provided evaluation intent or side-to-move context.
      |9. No markdown headers, no bullet lists, no metadata.
      |10. Never expose internal handles or raw metadata in the prose:
      |   route ids (such as route_1), literal move labels, source tags, or raw UCI strings.
      |   Translate them into player-facing move names, square paths, or plan language.
      |11. Vary sentence openings. Prefer opening from the situation, timing,
      |    practical problem, or likely consequence, then let the plan follow.
      |    Avoid defaulting to stock leads such as
      |    "This position is", "This moment is", or "Treat this as" when a more
      |    concrete opener is available.
      |12. Prefer declarative, timing-first, counterplay-first, or consequence-first
      |    openings over bare imperative verbs. Do not default to leads such as
      |    "Start", "Push", "Kick off", "Launch", "Drive", "Prepare", or
      |    "Organize" as the very first word unless the moment is an immediate emergency.
      |13. Explain the reason before the prescription when possible: the note should
      |    usually read as why-now first, then what plan follows from it.
      |14. Fold the opponent's best reply or the failure trigger into a dependent
      |    clause when possible, rather than adding a separate warning sentence.
      |15. If piece deployment matters, describe the target square or purpose
      |    ("toward e3", "onto the d-file", "to reinforce dark squares") instead
      |    of spelling out a raw square-by-square route unless that exact path is
      |    absolutely necessary. Route wording must not become the thesis sentence.
      |16. Use a measured coaching tone. Mild hedge words such as "can", "often",
      |    "usually", "tends to", or "should" are welcome when they keep the note
      |    grounded and less mechanical.
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

  private def openingLens(
      brief: ActiveStrategicCoachingBriefBuilder.Brief,
      phase: String,
      momentType: String,
      fen: String
  ): Option[OpeningLens] =
    val candidates =
      List(
        brief.whyNow.map(_ =>
          OpeningLens(
            label = "timing-first",
            instruction =
              "Open with the timing or structural reason that makes the plan urgent, then let the recommendation follow from that reason."
          )
        ),
        Option.when(brief.opponentReply.isDefined)(
          OpeningLens(
            label = "problem-first",
            instruction =
              "Open with the practical problem or opponent resource that has to be managed, then pivot into the plan."
          )
        ),
        Option.when(brief.keyTrigger.isDefined)(
          OpeningLens(
            label = "consequence-first",
            instruction =
              "Open with the likely consequence or drift risk that matters most, then explain the plan that addresses it."
          )
        ),
        brief.executionHint.map(_ =>
          OpeningLens(
            label = "target-purpose-first",
            instruction =
              "If piece redeployment matters, open with the target square or strategic purpose in natural language rather than a raw route string."
          )
        ),
        brief.campaignRole.map(_ =>
          OpeningLens(
            label = "campaign-role-first",
            instruction =
              "Open with this moment's role in the longer campaign, in natural language rather than with an imperative push command."
          )
        ),
        brief.primaryIdea.map(_ =>
          OpeningLens(
            label = "idea-first",
            instruction =
              "Open with the dominant strategic idea itself, but phrase it as a concrete objective or pressure shift rather than a bare command."
          )
        )
      ).flatten

    Option.when(candidates.nonEmpty) {
      val seed =
        List(
          phase.trim.toLowerCase,
          momentType.trim.toLowerCase,
          Option(fen).map(_.trim).getOrElse(""),
          brief.campaignRole.getOrElse(""),
          brief.primaryIdea.getOrElse(""),
          brief.whyNow.getOrElse(""),
          brief.opponentReply.getOrElse(""),
          brief.executionHint.getOrElse(""),
          brief.longTermObjective.getOrElse(""),
          brief.keyTrigger.getOrElse("")
        ).mkString("|")
      val idx = Math.floorMod(seed.hashCode, candidates.size)
      candidates(idx)
    }

  private def openingLensBlock(
      brief: ActiveStrategicCoachingBriefBuilder.Brief,
      phase: String,
      momentType: String,
      fen: String
  ): Option[String] =
    openingLens(brief, phase, momentType, fen).map { lens =>
      s"""- Preferred opening lens: ${lens.label}
         |- First-sentence guidance: ${lens.instruction}
         |- Opening-shape guardrail: avoid bare imperative leads like "Start", "Push", "Kick off", "Launch", "Drive", "Prepare", or "Organize" unless the moment is a direct tactical emergency.""".stripMargin
    }.flatMap(body => section("OPENING LENS", body))

  def buildPrompt(
      baseNarrative: String,
      phase: String,
      momentType: String,
      fen: String,
      concepts: List[String],
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): String =
    val _ = baseNarrative
    val conceptStr = concepts.map(_.trim).filter(_.nonEmpty).distinct.take(8)
    val coachingBrief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, moveRefs)
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
        openingLensBlock(coachingBrief, phase, momentType, fen),
        Some(
          "Write one independent strategic coaching note now. Center the dominant idea as the thesis, keep route/objective language as supporting evidence only, mention at most one of the execution hint or the long-term objective, open from the situation, timing, practical problem, or likely consequence when possible, and let the plan follow from that reason."
        )
      ).flatten

    sections.mkString("\n\n")

  def buildRepairPrompt(
      baseNarrative: String,
      rejectedNote: String,
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
    val coachingBrief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, moveRefs)
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
        Some(s"""## PRIOR NOTE TO AVOID PARAPHRASING
                |$baseNarrative
                |
                |Use the prior note only as a contradiction guard. Do not mirror its wording or sentence structure.""".stripMargin),
        Some(s"## REJECTED NOTE\n$rejectedNote"),
        Some(s"## REPAIR REASONS\n$reasonLine"),
        Some(contextLines),
        section("COACHING BRIEF", coachingBriefBlock),
        openingLensBlock(coachingBrief, phase, momentType, fen),
        Some(
          "Rewrite into one valid strategic coaching note (2-3 sentences), grounded, compact, and concrete. Center the dominant idea as the thesis, keep route/objective language as supporting evidence only, mention at most one of the execution hint or the long-term objective, follow the preferred opening lens when it is provided, and explain why before the plan when possible."
        )
      ).flatten

    sections.mkString("\n\n")
