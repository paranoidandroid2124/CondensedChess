package lila.llm.analysis

import lila.llm.model.*
import lila.llm.model.authoring.QuestionEvidence
import lila.llm.model.strategic.VariationLine

private[analysis] enum StrategicLens:
  case Compensation
  case Prophylaxis
  case Structure
  case Decision
  case Practical
  case Opening

private[analysis] case class StrategicThesis(
    lens: StrategicLens,
    claim: String,
    support: List[String],
    tension: Option[String],
    evidenceHook: Option[String]
)

private[analysis] object StrategicThesisBuilder:

  def build(ctx: NarrativeContext): Option[StrategicThesis] =
    buildCompensation(ctx)
      .orElse(buildProphylaxis(ctx))
      .orElse(buildStructure(ctx))
      .orElse(buildDecision(ctx))
      .orElse(buildPractical(ctx))
      .orElse(buildOpening(ctx))

  private def buildCompensation(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.compensation).flatMap { comp =>
      val vectors = topVectors(comp.returnVector)
      val plan = normalizeText(comp.conversionPlan)
      if comp.investedMaterial <= 0 || (vectors.isEmpty && plan.isEmpty) then None
      else
        val vectorText = joinNatural(vectors)
        val claim =
          val payoff =
            if vectorText.nonEmpty then vectorText
            else if plan.nonEmpty then plan.toLowerCase
            else "long-term compensation"
          s"The point of the move is not an immediate score, but to accept a ${comp.investedMaterial}cp investment for $payoff."
        val support = List(
          Option.when(plan.nonEmpty)(s"The compensation has to cash out through $plan."),
          Option.when(vectorText.nonEmpty)(s"That only works if the initiative keeps generating $vectorText.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Compensation,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = chooseEvidenceHook(ctx)
          )
        )
    }

  private def buildProphylaxis(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { prevented =>
      val target =
        prevented.preventedThreatType.map(normalizeText).filter(_.nonEmpty)
          .orElse(prevented.breakNeutralized.map(file => s"$file-break"))
          .orElse(Option.when(normalizeText(prevented.planId).nonEmpty)(normalizeText(prevented.planId)))
      val hasSignal = target.nonEmpty || prevented.counterplayScoreDrop > 0
      if !hasSignal then None
      else
        val targetText = target.getOrElse("the opponent's easiest counterplay")
        val claim = s"The move matters less for a direct gain than for cutting out $targetText."
        val support = List(
          Option.when(prevented.counterplayScoreDrop > 0)(
            s"That strips away roughly ${prevented.counterplayScoreDrop}cp of counterplay."
          ),
          leadingPlanName(ctx).map(plan => s"It also gives $plan more time to take hold.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Prophylaxis,
            claim = claim,
            support = support.take(2),
            tension = opponentPlanTension(ctx),
            evidenceHook = chooseEvidenceHook(ctx)
          )
        )
    }

  private def buildStructure(ctx: NarrativeContext): Option[StrategicThesis] =
    val semantic = ctx.semantic
    val profileOpt = semantic.flatMap(_.structureProfile)
    val alignmentOpt = semantic.flatMap(_.planAlignment)
    val hasStructure = profileOpt.isDefined || alignmentOpt.exists(pa => pa.reasonCodes.nonEmpty || normalizeText(pa.band).nonEmpty)
    if !hasStructure then None
    else
      val arcOpt = StructurePlanArcBuilder.build(ctx)
      val structureName = profileOpt.map(_.primary).map(normalizeText).filter(_.nonEmpty)
      val centerState = profileOpt.map(_.centerState).map(normalizeText).filter(_.nonEmpty)
      val leadPlan =
        leadingPlanName(ctx)
          .orElse(alignmentOpt.flatMap(_.narrativeIntent).map(normalizeText).filter(_.nonEmpty))
      val claim =
        arcOpt.filter(StructurePlanArcBuilder.proseEligible).map(StructurePlanArcBuilder.claimText)
          .getOrElse {
            val structureLead = structureName.orElse(centerState).getOrElse("the structure")
            val planLead = leadPlan.getOrElse("the natural plan")
            val centerText =
              centerState.filterNot(s => structureName.contains(s)).map(s => s" and its ${s.toLowerCase} center").getOrElse("")
            s"The move reorganizes the pieces around $structureLead$centerText, aiming at $planLead."
          }
      val support =
        arcOpt match
          case Some(arc) if StructurePlanArcBuilder.proseEligible(arc) =>
            List(
              Some(StructurePlanArcBuilder.supportPrimaryText(arc)),
              Some(StructurePlanArcBuilder.supportSecondaryText(arc))
            ).flatten
          case Some(arc) =>
            List(
              alignmentOpt.flatMap(_.narrativeIntent).map(intent => s"It follows the structure's logic of ${normalizeSentenceFragment(intent)}."),
              Some(StructurePlanArcBuilder.cautionSupportText(arc)),
              alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That makes move order matter because ${normalizeSentenceFragment(risk)}.")
            ).flatten
          case None =>
            List(
              alignmentOpt.flatMap(_.narrativeIntent).map(intent => s"It follows the structure's logic of ${normalizeSentenceFragment(intent)}."),
              alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That makes move order matter because ${normalizeSentenceFragment(risk)}."),
              Option.when(alignmentOpt.exists(_.reasonCodes.nonEmpty)) {
                val reasons = alignmentOpt.toList.flatMap(_.reasonCodes).map(humanizeCode).filter(_.nonEmpty).take(2)
                Option.when(reasons.nonEmpty)(s"The plan fit is shaped by ${joinNatural(reasons)}.").getOrElse("")
              }.filter(_.nonEmpty)
            ).flatten
      val tension =
        arcOpt.flatMap(_.prophylaxisSupport)
          .orElse(alignmentOpt.flatMap(_.narrativeRisk).map(risk => s"That still leaves ${normalizeSentenceFragment(risk)}."))
          .orElse(opponentOrAbsenceTension(ctx))
      Some(
        StrategicThesis(
          lens = StrategicLens.Structure,
          claim = claim,
          support = support.take(2),
          tension = tension,
          evidenceHook = chooseEvidenceHook(ctx)
        )
      )

  private def buildDecision(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.decision.flatMap { decision =>
      val hasSignal =
        ctx.whyAbsentFromTopMultiPV.nonEmpty || ctx.authorEvidence.nonEmpty || ctx.opponentPlan.isDefined
      if !hasSignal then None
      else
        val chosen =
          leadingPlanName(ctx)
            .orElse(normalizedDecisionSummary(decision.logicSummary))
            .getOrElse("the main continuation")
        val deferred =
          AlternativeNarrativeSupport.moveLabel(ctx)
            .orElse(ctx.meta.flatMap(_.whyNot).flatMap(extractQuotedMove))
            .getOrElse("the most direct alternative")
        val reason =
          normalizedDecisionSummary(decision.logicSummary)
            .orElse(ctx.whyAbsentFromTopMultiPV.headOption.map(normalizeSentenceFragment))
            .getOrElse("it keeps the move order coherent")
        val claim = s"The key decision is to choose $chosen and postpone $deferred, because $reason."
        val support = List(
          buildDecisionDeltaSupport(decision.delta),
          ctx.decision.flatMap(_.focalPoint.map(renderTargetRef)).map(target => s"The whole decision turns on $target.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Decision,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = chooseEvidenceHook(ctx)
          )
        )
    }

  private def buildPractical(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.practicalAssessment).flatMap { practical =>
      val verdict = normalizeText(practical.verdict)
      val drivers = practical.biasFactors.sortBy(b => -Math.abs(b.weight)).take(2).map(renderBiasFactor)
      if verdict.isEmpty && drivers.isEmpty then None
      else
        val driverText = joinNatural(drivers)
        val claim =
          if driverText.nonEmpty then
            s"More important than the nominal evaluation is that the move creates an easier practical task through $driverText."
          else
            s"More important than the nominal evaluation is that the move creates a ${verdict.toLowerCase} practical task."
        val support = List(
          Option.when(verdict.nonEmpty)(s"The resulting task is ${verdict.toLowerCase.stripSuffix(".")}."),
          Option.when(driverText.nonEmpty)(s"That matters because $driverText shape the workload.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Practical,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = chooseEvidenceHook(ctx)
          )
        )
    }

  private def buildOpening(ctx: NarrativeContext): Option[StrategicThesis] =
    openingLabel(ctx).flatMap { openingName =>
      val planClue =
        leadingPlanName(ctx)
          .orElse(ctx.semantic.flatMap(_.structureProfile).map(_.primary).map(normalizeText).filter(_.nonEmpty))
      if planClue.isEmpty then None
      else
        val claim = s"The move extends $openingName ideas toward ${planClue.get.toLowerCase}."
        val precedentBranch = OpeningPrecedentBranching.representative(ctx, ctx.openingData, requireFocus = true)
        val support = List(
          precedentBranch.map(_.representativeSentence),
          OpeningPrecedentBranching.relationSentence(ctx, ctx.openingData, requireFocus = true),
          precedentBranch.map(_.summarySentence),
          ctx.openingEvent.map(renderOpeningEventSupport),
          ctx.semantic.flatMap(_.structureProfile).map { profile =>
            val center = normalizeText(profile.centerState)
            if center.nonEmpty then s"The position already points to a ${center.toLowerCase} center and long-term maneuvering."
            else s"The structure already matches the long-term themes of $openingName."
          }
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Opening,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = chooseEvidenceHook(ctx)
          )
        )
    }

  private def openingLabel(ctx: NarrativeContext): Option[String] =
    ctx.openingData.flatMap(_.name).map(normalizeText).filter(_.nonEmpty)
      .orElse {
        ctx.openingEvent.collect {
          case OpeningEvent.Intro(_, name, _, _) => normalizeText(name)
        }.find(_.nonEmpty)
      }

  private def opponentOrAbsenceTension(ctx: NarrativeContext): Option[String] =
    AlternativeNarrativeSupport.sentence(ctx).orElse(opponentPlanTension(ctx))

  private def leadingPlanName(ctx: NarrativeContext): Option[String] =
    ctx.mainStrategicPlans.headOption.map(_.planName).map(normalizeText).filter(_.nonEmpty)
      .orElse(ctx.plans.top5.headOption.map(_.name).map(normalizeText).filter(_.nonEmpty))

  private def opponentPlanTension(ctx: NarrativeContext): Option[String] =
    ctx.opponentPlan.map(_.name).map(normalizeText).filter(_.nonEmpty).map { plan =>
      s"The main counterplay still revolves around $plan."
    }

  private def chooseEvidenceHook(ctx: NarrativeContext): Option[String] =
    authorEvidenceHook(ctx.authorEvidence)
      .orElse(probeRequestHook(ctx.probeRequests))
      .orElse(topVariationHook(ctx))
      .map(UserFacingSignalSanitizer.sanitize)

  private def authorEvidenceHook(evidence: List[QuestionEvidence]): Option[String] =
    evidence.flatMap(_.branches).headOption.map { branch =>
      val key = normalizeText(branch.keyMove)
      val line = normalizeText(branch.line)
      val cp = branch.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
      if key.nonEmpty && line.nonEmpty then s"Probe evidence starts with $key: $line$cp."
      else if line.nonEmpty then s"Probe evidence follows $line$cp."
      else s"Probe evidence starts with $key$cp."
    }

  private def probeRequestHook(requests: List[ProbeRequest]): Option[String] =
    requests.headOption.flatMap { req =>
      val plan = req.planName.map(normalizeText).filter(_.nonEmpty)
      val objective = req.objective.map(normalizeText).filter(_.nonEmpty)
      val moves = req.moves.take(2).map(m => NarrativeUtils.uciToSanOrFormat(req.fen, m)).filter(_.trim.nonEmpty)
      val moveText = Option.when(moves.nonEmpty)(s" through ${joinNatural(moves)}").getOrElse("")
      (plan orElse objective).map(label => s"Further probe work still targets $label$moveText.")
    }

  private def topVariationHook(ctx: NarrativeContext): Option[String] =
    ctx.engineEvidence.flatMap(_.best).flatMap { line =>
      val preview = variationPreview(ctx.fen, line, limit = 3)
      preview.map(text => s"The engine line begins $text.")
    }

  private def buildDecisionDeltaSupport(delta: PVDelta): Option[String] =
    val actions = List(
      delta.resolvedThreats.headOption.map(threat => s"resolving $threat"),
      delta.newOpportunities.headOption.map(target => s"creating pressure on ${normalizeSentenceFragment(target)}"),
      delta.planAdvancements.headOption.map(step => s"advancing ${normalizeSentenceFragment(step.replace("Met:", "").replace("Removed:", ""))}")
    ).flatten
    actions match
      case a :: b :: _ => Some(s"It does that by $a and then $b.")
      case a :: Nil    => Some(s"It does that by $a.")
      case Nil         => None

  private def renderOpeningEventSupport(event: OpeningEvent): String =
    event match
      case OpeningEvent.Intro(_, name, theme, _) if normalizeText(theme).nonEmpty =>
        s"The game is still in ${normalizeText(name)} territory, with $theme as the long-term guide."
      case OpeningEvent.BranchPoint(divergingMoves, _, _) if divergingMoves.nonEmpty =>
        s"The opening now branches around ${joinNatural(divergingMoves.take(3))}."
      case OpeningEvent.OutOfBook(playedMove, _, _) =>
        s"This already functions as an out-of-book decision after $playedMove."
      case OpeningEvent.Novelty(playedMove, cpLoss, _, _) =>
        val cost = if cpLoss > 0 then s" for only about ${cpLoss}cp" else ""
        s"This is effectively a novelty with $playedMove$cost."
      case OpeningEvent.TheoryEnds(_, sampleCount) =>
        s"Theory is already thinning out here, with only about $sampleCount games left in sample."
      case _ =>
        "The opening reference is already giving way to independent strategic play."

  private def variationLeadSan(fen: String, line: VariationLine): Option[String] =
    line.ourMove.map(_.san).map(normalizeText).filter(_.nonEmpty)
      .orElse {
        line.moves.headOption.map(m => NarrativeUtils.uciToSanOrFormat(fen, m)).map(normalizeText).filter(_.nonEmpty)
      }

  private def variationPreview(fen: String, line: VariationLine, limit: Int): Option[String] =
    val tokens =
      if line.parsedMoves.nonEmpty then line.parsedMoves.take(limit).map(_.san.trim).filter(_.nonEmpty)
      else NarrativeUtils.uciListToSan(fen, line.moves.take(limit)).map(_.trim).filter(_.nonEmpty)
    Option.when(tokens.nonEmpty)(tokens.mkString(" "))

  private def topVectors(returnVector: Map[String, Double]): List[String] =
    returnVector.toList
      .sortBy { case (_, value) => -value }
      .map { case (label, _) => normalizeText(label).toLowerCase }
      .filter(_.nonEmpty)
      .take(2)

  private def renderBiasFactor(bias: PracticalBiasInfo): String =
    val factor = normalizeText(bias.factor).toLowerCase
    val description = normalizeText(bias.description)
    if factor.nonEmpty && description.nonEmpty && !description.equalsIgnoreCase(factor) then s"$factor ($description)"
    else if factor.nonEmpty then factor
    else description.toLowerCase

  private def extractQuotedMove(raw: String): Option[String] =
    "\"([^\"]+)\"".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty)
      .orElse("'([^']+)'".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty))

  private def humanizeCode(code: String): String =
    normalizeText(code).replace("pa ", "").replace("req ", "").replace("sup ", "")

  private def renderTargetRef(target: TargetRef): String =
    target match
      case TargetSquare(key)        => key
      case TargetFile(file)         => s"$file-file"
      case TargetPiece(role, square) => s"${NarrativeUtils.humanize(role)} on $square"

  private def joinNatural(items: List[String]): String =
    items.map(normalizeText).filter(_.nonEmpty).distinct match
      case Nil => ""
      case one :: Nil => one
      case a :: b :: Nil => s"$a and $b"
      case many => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def normalizeSentenceFragment(raw: String): String =
    normalizeText(raw).stripSuffix(".")

  private def normalizedDecisionSummary(raw: String): Option[String] =
    Option(raw)
      .map(normalizeSentenceFragment)
      .map(_.replace(" -> ", "; then ").replace("->", "; then "))
      .map(_.stripPrefix("The idea is ").stripPrefix("the idea is ").trim)
      .filter(_.nonEmpty)

  private def formatCp(cp: Int): String =
    if cp > 0 then f"+${cp.toDouble / 100}%.2f"
    else f"${cp.toDouble / 100}%.2f"
