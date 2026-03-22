package lila.llm.analysis

import lila.llm.*
import lila.llm.model.*

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

  def build(ctx: NarrativeContext, strategyPack: Option[StrategyPack] = None): Option[StrategicThesis] =
    val surface = StrategyPackSurface.from(strategyPack)
    buildCompensation(ctx, surface)
      .orElse(buildProphylaxis(ctx, surface))
      .orElse(buildStructure(ctx, surface))
      .orElse(buildSurfaceIdea(ctx, surface))
      .orElse(buildDecision(ctx, surface))
      .orElse(buildPractical(ctx))
      .orElse(buildOpening(ctx))

  private def buildCompensation(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    val compensationDecision = CompensationInterpretation.effectiveConsumerDecision(ctx, surface)
    val vectors = compensationDecision.map(_.signal.vectors).filter(_.nonEmpty).getOrElse(Nil)
    val plan =
      compensationDecision.flatMap(_.signal.summary).map(normalizeText).filter(_.nonEmpty)
    val hasCompensationSignal = compensationDecision.isDefined
    val allowCompensationNarration =
      !surface.compensationPosition || CompensationDisplayPhrasing.compensationNarrationEligible(surface)
    if !hasCompensationSignal then None
    else if !allowCompensationNarration then buildOrdinaryCompensationReframe(ctx, surface)
    else
      val vectorText = joinNatural(vectors)
      val subtypePayoff =
        Option.when(surface.normalizationActive) {
          surface.normalizedCompensationLead.orElse(CompensationDisplayPhrasing.compensationPayoffText(surface))
        }.flatten
      val payoff =
        subtypePayoff.getOrElse(
          if vectorText.nonEmpty then vectorText
          else
            surface.compensationSummary.map(_.toLowerCase).filter(_.nonEmpty)
              .orElse(plan.map(_.toLowerCase))
              .orElse(surface.dominantIdeaText.map(_.toLowerCase))
                .getOrElse("long-term compensation")
        )
      val claim =
        CompensationDisplayPhrasing.compensationWhyNowText(surface)
          .orElse {
            val ownerLead =
              surface.campaignOwnerText.filter(_ => surface.ownerMismatch)
                .map(side => s"$side gives up material")
                .getOrElse("The move gives up material")
            Some(s"$ownerLead for $payoff.")
          }
      val compensationSupportBase = List(
          CompensationDisplayPhrasing.compensationObjectiveText(surface),
          renderCompensationFollowUp(surface),
          Option.when(surface.normalizationActive)(surface.effectiveCompensationSubtype).flatten.flatMap { _ =>
            CompensationDisplayPhrasing.compensationPersistenceText(surface)
              .map(reason => LiveNarrativeCompressionCore.rewritePlayerLanguage(s"This works only while $reason."))
          }
        ).flatten
      val subtypeSupport = Option.when(surface.normalizationActive)(CompensationDisplayPhrasing.compensationSupportText(surface)).getOrElse(Nil)
      val orderedCompensationSupport = compensationSupportBase ++ subtypeSupport
      val support =
        CompensationDisplayPhrasing.dedupeCompensationSupport(
          claim.getOrElse("The move gives up material."),
          orderedCompensationSupport.distinct
        )
      Some(
          StrategicThesis(
            lens = StrategicLens.Compensation,
            claim = claim.getOrElse("The move keeps the pressure going rather than winning the material back at once."),
            support = support.take(2),
          tension = opponentOrAbsenceTension(ctx),
          evidenceHook = NarrativeEvidenceHooks.build(ctx)
        )
      )

  private def ordinaryClaimFromSurface(
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    surface.dominantIdeaText
      .filterNot(surfaceIdeaClaimTooBroad(_, surface))
      .map(renderStrategicIdeaClaim)
      .orElse(surface.objectiveText.map(StrategicSentenceRenderer.renderObjectiveClaim))
      .orElse(surface.executionText.map(renderExecutionClaim))
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)

  private def renderNeutralFocalSupport(target: TargetRef): String =
    s"Pressure on ${renderTargetRef(target)} is the concrete point of the move."

  private def buildOrdinaryCompensationReframe(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    val decisionReason =
      ctx.decision.flatMap(decision =>
        normalizedDecisionSummary(decision.logicSummary)
          .orElse(ctx.whyAbsentFromTopMultiPV.headOption.map(normalizeSentenceFragment))
          .map(reason => s"The move is mainly about $reason.")
      )
    val claim =
      ordinaryClaimFromSurface(surface)
        .orElse(decisionReason)
        .orElse(ctx.decision.flatMap(_.focalPoint.map(target => s"The move is really playing for ${renderTargetRef(target)}.")))
        .orElse(Option.when(normalizeText(ctx.summary.primaryPlan).nonEmpty)(s"The move keeps ${normalizeText(ctx.summary.primaryPlan)} on track."))
        .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
    val focalSupport =
      ctx.decision.flatMap(_.focalPoint.map(target => renderNeutralFocalSupport(target)))
    val support =
      List(
        surface.objectiveText
          .filter(objective => claim.forall(existing => !existing.toLowerCase.contains(normalizeText(objective).toLowerCase)))
          .map(StrategicSentenceRenderer.renderObjectiveSupport),
        surface.executionText
          .filter(execution => claim.forall(existing => !existing.toLowerCase.contains(normalizeText(execution).toLowerCase)))
          .map(StrategicSentenceRenderer.renderExecutionSupport),
        buildDecisionDeltaSupport(ctx.decision.map(_.delta).getOrElse(PVDelta(Nil, Nil, Nil, Nil))),
        focalSupport
      ).flatten.distinct.take(2)
    claim.map(claimText =>
      StrategicThesis(
        lens = StrategicLens.Decision,
        claim = claimText,
        support = support,
        tension = opponentOrAbsenceTension(ctx),
        evidenceHook = NarrativeEvidenceHooks.build(ctx)
      )
    )

  private def buildSurfaceIdea(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    val claim =
      ordinaryClaimFromSurface(surface)
        .filter(LiveNarrativeCompressionCore.hasConcreteAnchor)
    val surfaceSupport =
      decisionNarrativeSupport(
        ctx = ctx,
        surface = surface,
        claim = claim,
        preferFocalSupportFirst = true,
        compensationObjectiveSupport = None,
        compensationFollowUp = None,
        alternativeSupport =
          ctx.whyAbsentFromTopMultiPV.headOption
            .map(normalizeSentenceFragment)
            .filter(_.nonEmpty)
            .map(reason => s"The alternative stays secondary because $reason.")
      )
    claim.map(claimText =>
      StrategicThesis(
        lens = StrategicLens.Decision,
        claim = claimText,
        support = surfaceSupport.take(3),
        tension = opponentOrAbsenceTension(ctx),
        evidenceHook = NarrativeEvidenceHooks.build(ctx)
      )
    )

  private def buildProphylaxis(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.preventedPlans.headOption).flatMap { prevented =>
      val target =
        prevented.preventedThreatType.map(normalizeText).filter(_.nonEmpty)
          .orElse(prevented.breakNeutralized.map(file => s"$file-break"))
          .orElse(Option.when(normalizeText(prevented.planId).nonEmpty)(normalizeText(prevented.planId)))
      val hasSignal = target.nonEmpty || prevented.counterplayScoreDrop > 0
      if !hasSignal then None
      else
        prevented.sourceScope match
          case FactScope.Now =>
            val stoppedBreak =
              prevented.breakNeutralized.map(renderBreakRef)
            val denialLead =
              stoppedBreak
                .map(breakRef => s"The move is mainly about stopping $breakRef before it gets going.")
                .orElse(
                  prevented.deniedSquares.headOption.map(square => s"The move is mainly about keeping the opponent out of $square.")
                )
                .orElse(
                  target.filterNot(_.equalsIgnoreCase("counterplay")).map(text => s"The move is mainly about slowing down $text.")
                )
                .getOrElse("The move is mainly about slowing down the opponent's next active idea.")
            val claim =
              surface.dominantIdeaText.map(idea =>
                s"$denialLead It keeps $idea in view."
              ).getOrElse(denialLead)
            val support = strategySupport(surface) ++ List(
              stoppedBreak.map(breakRef => s"$breakRef is harder to justify right away."),
              prevented.deniedSquares.headOption.map(square => s"The opponent no longer gets a free entry on $square."),
              leadingPlanName(ctx).map(plan => s"It buys time for $plan.")
            ).flatten
            Some(
              StrategicThesis(
                lens = StrategicLens.Prophylaxis,
                claim = claim,
                support = support.take(2),
                tension = opponentPlanTension(ctx),
                evidenceHook = NarrativeEvidenceHooks.build(ctx)
              )
            )
          case _ =>
            None
    }

  private def buildStructure(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
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
      val anchoredPlan =
        surface.objectiveText
          .orElse(surface.executionText)
          .orElse(alignmentOpt.flatMap(_.narrativeIntent).map(normalizeText).filter(_.nonEmpty))
          .orElse(leadPlan)
          .orElse(surface.dominantIdeaText)
          .map(normalizeText)
          .filter(_.nonEmpty)
      val claim =
        arcOpt.filter(StructurePlanArcBuilder.proseEligible).map { arc =>
          val structureLead = structureName.orElse(centerState).getOrElse(arc.structureLabel)
          structureClaimFromArc(structureLead, arc, anchoredPlan)
        }
          .getOrElse {
            val structureLead = structureName.orElse(centerState).getOrElse("the structure")
            anchoredPlan
              .map { plan =>
                if structureName.exists(name => plan.toLowerCase.contains(name.toLowerCase)) then
                  s"The move follows $plan."
                else if structureLead.equalsIgnoreCase("the structure") then
                  s"The structure points to $plan, and the move follows that plan."
                else s"The $structureLead points to $plan, and the move follows that plan."
              }
              .getOrElse(s"The move follows the structure of $structureLead.")
          }
      val support =
        val structuralSupport =
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
                Option.when(reasons.nonEmpty)(s"The structure points that way because ${joinNatural(reasons)}.").getOrElse("")
              }.filter(_.nonEmpty)
            ).flatten
        strategySupport(surface) ++ structuralSupport
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
          evidenceHook = NarrativeEvidenceHooks.build(ctx)
        )
      )

  private def strategySupport(surface: StrategyPackSurface.Snapshot): List[String] =
    List(
      surface.executionText.map(StrategicSentenceRenderer.renderExecutionSupport),
      surface.objectiveText
        .filter(objective => surface.executionText.forall(execution => !objective.equalsIgnoreCase(execution)))
        .map(StrategicSentenceRenderer.renderObjectiveSupport)
    ).flatten.filter(_.nonEmpty)

  private def hasStrategicClaimSurface(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.dominantIdeaText.nonEmpty ||
      surface.executionText.nonEmpty ||
      surface.objectiveText.nonEmpty ||
      surface.compensationPosition ||
      surface.investedMaterial.exists(_ > 0)

  private def decisionClaimFromSurface(
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    Option.when(hasStrategicClaimSurface(surface)) {
      val lead =
        if surface.compensationPosition then
          surface.objectiveText
            .map(StrategicSentenceRenderer.renderCompensationClaimFromObjective)
            .orElse(surface.executionText.map(StrategicSentenceRenderer.renderCompensationClaimFromExecution))
            .orElse(surface.dominantIdeaText.map(idea => renderCompensationClaimFromIdea(idea)))
        else
          surface.dominantIdeaText
            .map(idea => renderStrategicIdeaClaim(idea))
            .orElse(surface.objectiveText.map(StrategicSentenceRenderer.renderObjectiveClaim))
            .orElse(surface.executionText.map(execution => renderExecutionClaim(execution)))
      lead.map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
    }.flatten

  private def renderDecisionFocalSupport(
      target: TargetRef,
      surface: StrategyPackSurface.Snapshot
  ): String =
    val rendered = renderTargetRef(target)
    if surface.compensationPosition then s"Pressure on $rendered keeps the compensation alive."
    else
      surface.dominantIdeaText.map(_ => s"Pressure on $rendered is the concrete point of the move.")
        .orElse(surface.objectiveText.map(_ => s"Pressure on $rendered is the concrete target here."))
        .orElse(Option.when(hasStrategicClaimSurface(surface))(s"The move is really about pressure on $rendered."))
        .getOrElse(s"The decision becomes concrete around $rendered.")

  private def buildDecision(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[StrategicThesis] =
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
        val surfaceClaim = decisionClaimFromSurface(surface)
        val claim =
          surfaceClaim.getOrElse(s"The move chooses $chosen first because $reason.")
        val focalSupport = ctx.decision.flatMap(_.focalPoint.map(target => renderDecisionFocalSupport(target, surface)))
        val support =
          (
            if surfaceClaim.isDefined || hasStrategicClaimSurface(surface) then
              decisionNarrativeSupport(
                ctx = ctx,
                surface = surface,
                claim = Some(claim),
                preferFocalSupportFirst = false,
                compensationObjectiveSupport =
                  surface.objectiveText
                    .filter(objective => surface.executionText.forall(execution => !objective.equalsIgnoreCase(execution)))
                    .map(objective =>
                      LiveNarrativeCompressionCore.rewritePlayerLanguage(
                        StrategicSentenceRenderer.renderCompensationSupportFromObjective(objective)
                      )
                    )
                    .filter(_ => surface.compensationPosition),
                compensationFollowUp =
                  Option.when(surface.compensationPosition)(renderCompensationFollowUp(surface)).flatten,
                alternativeSupport = Some(s"$deferred stays secondary because $reason.")
              ).map(Some(_))
            else
              List(
                buildDecisionDeltaSupport(decision.delta),
                focalSupport
              )
          ).flatten.distinct
        Some(
          StrategicThesis(
            lens = StrategicLens.Decision,
            claim = claim,
            support = support.take(3),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def decisionNarrativeSupport(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      claim: Option[String],
      preferFocalSupportFirst: Boolean,
      compensationObjectiveSupport: Option[String],
      compensationFollowUp: Option[String],
      alternativeSupport: Option[String]
  ): List[String] =
    val focalSupport =
      ctx.decision
        .flatMap(_.focalPoint.map(target => renderDecisionFocalSupport(target, surface)))
        .filter(text => claim.forall(existing => !sameStrategicIdea(existing, text)))
    val deltaSupport =
      ctx.decision
        .flatMap(decision => buildDecisionDeltaSupport(decision.delta))
        .filter(text => claim.forall(existing => !sameStrategicIdea(existing, text)))
    val objectiveSupport =
      compensationObjectiveSupport.orElse(
        surface.objectiveText
          .filter(objective => surface.executionText.forall(execution => !objective.equalsIgnoreCase(execution)))
          .map(StrategicSentenceRenderer.renderObjectiveSupport)
      ).filter(text => claim.forall(existing => !sameStrategicIdea(existing, text)))
    val executionSupport =
      compensationFollowUp.orElse(
        surface.executionText
          .map(StrategicSentenceRenderer.renderExecutionSupport)
      ).filter(text => claim.forall(existing => !sameStrategicIdea(existing, text)))
    val anchoredSupport =
      if preferFocalSupportFirst then
        List(focalSupport, objectiveSupport, executionSupport, deltaSupport).flatten.distinct
      else
        List(deltaSupport, objectiveSupport, executionSupport, focalSupport).flatten.distinct
    anchoredSupport.headOption.toList ++ alternativeSupport.toList ++ anchoredSupport.drop(1)

  private def buildPractical(ctx: NarrativeContext): Option[StrategicThesis] =
    ctx.semantic.flatMap(_.practicalAssessment).flatMap { practical =>
      val verdict = normalizeText(practical.verdict)
      val drivers = practical.biasFactors.sortBy(b => -Math.abs(b.weight)).take(2).map(renderBiasFactor)
      if verdict.isEmpty && drivers.isEmpty then None
      else
        val driverText = joinNatural(drivers)
        val claim =
          if driverText.nonEmpty then
            s"The move is easier to handle because $driverText."
          else
            "The move is easier to handle over the board."
        val support = List(
          Option.when(verdict.nonEmpty)(s"That leaves a ${verdict.toLowerCase.stripSuffix(".")} version to handle."),
          Option.when(driverText.nonEmpty)(s"That is easier because $driverText.")
        ).flatten
        Some(
          StrategicThesis(
            lens = StrategicLens.Practical,
            claim = claim,
            support = support.take(2),
            tension = opponentOrAbsenceTension(ctx),
            evidenceHook = NarrativeEvidenceHooks.build(ctx)
          )
        )
    }

  private def buildOpening(ctx: NarrativeContext): Option[StrategicThesis] =
    openingLabel(ctx).flatMap { openingName =>
      val planClue = leadingPlanName(ctx)
      val thematicClue =
        openingTheme(ctx)
          .orElse(ctx.semantic.flatMap(_.structureProfile).map(_.primary).map(normalizeText).filter(_.nonEmpty))
      val claim =
        planClue
          .map(clue => s"The move extends $openingName ideas toward ${clue.toLowerCase}.")
          .orElse(
            thematicClue.map(clue => s"The move keeps the game inside $openingName themes around ${clue.toLowerCase}.")
          )
          .getOrElse(s"The move keeps the game inside $openingName territory.")
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
          evidenceHook = NarrativeEvidenceHooks.build(ctx)
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

  private def structureClaimFromArc(
      structureLead: String,
      arc: StructurePlanArc,
      anchoredPlan: Option[String]
  ): String =
    if arc.planBacked then
      val deploymentLead =
        s"the ${structurePieceWord(arc.primaryDeployment.piece)} route toward ${structureDestination(arc.primaryDeployment.destination)}"
      val structurePrefix =
        if structureLead.equalsIgnoreCase("the structure") then "The structure"
        else s"The $structureLead"
      s"$structurePrefix points to ${structurePlanLabel(arc.planLabel)}, and the move starts $deploymentLead."
    else
      anchoredPlan
        .filter(plan => !plan.equalsIgnoreCase(arc.planLabel))
        .map { plan =>
          if structureLead.equalsIgnoreCase("the structure") then
            s"The structure points to $plan, and the move follows that plan."
          else s"The $structureLead points to $plan, and the move follows that plan."
        }
        .getOrElse(StructurePlanArcBuilder.claimText(arc))

  private def structurePlanLabel(planLabel: String): String =
    val normalized = normalizeText(planLabel)
    if normalized.toLowerCase.startsWith("the ") then normalized.toLowerCase else s"the ${normalized.toLowerCase}"

  private def structurePieceWord(piece: String): String =
    piece match
      case "N" => "knight"
      case "B" => "bishop"
      case "R" => "rook"
      case "Q" => "queen"
      case "K" => "king"
      case other => other.toLowerCase

  private def structureDestination(destination: String): String =
    if destination.endsWith("-file") then s"the $destination" else destination

  private def leadingPlanName(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport.evidenceBackedLeadingPlanName(ctx).map(normalizeText).filter(_.nonEmpty)

  private def openingTheme(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect {
      case OpeningEvent.Intro(_, _, theme, _) => normalizeText(theme)
    }.find(_.nonEmpty)

  private def opponentPlanTension(ctx: NarrativeContext): Option[String] =
    ctx.opponentPlan.map(_.name).map(normalizeText).filter(_.nonEmpty).map { plan =>
      s"The main counterplay still revolves around $plan."
    }

  private def buildDecisionDeltaSupport(delta: PVDelta): Option[String] =
    val resolvedThreat =
      delta.resolvedThreats.headOption.map(_.trim).filter(_.nonEmpty)
    val concreteTarget =
      delta.newOpportunities.headOption.map(normalizeSentenceFragment).filter(_.nonEmpty)
    val followUp =
      delta.planAdvancements.headOption
        .map(step => normalizeSentenceFragment(step.replace("Met:", "").replace("Removed:", "")))
        .filter(_.nonEmpty)
    (resolvedThreat, concreteTarget, followUp) match
      case (Some(threat), Some(target), _) =>
        Some(s"It does that by resolving $threat. A concrete target is $target.")
      case (Some(threat), None, Some(step)) =>
        Some(s"It does that by resolving $threat. A likely follow-up is $step.")
      case (None, Some(target), Some(step)) =>
        Some(s"A concrete target is $target. A likely follow-up is $step.")
      case (Some(threat), None, None) =>
        Some(s"It does that by resolving $threat.")
      case (None, Some(target), None) =>
        Some(s"A concrete target is $target.")
      case (None, None, Some(step)) =>
        Some(s"A likely follow-up is $step.")
      case _ =>
        None

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

  private def renderBiasFactor(bias: PracticalBiasInfo): String =
    LiveNarrativeCompressionCore
      .renderPracticalBiasPlayer(normalizeText(bias.factor), normalizeText(bias.description))
      .getOrElse(normalizeText(bias.description).toLowerCase)

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

  private def renderBreakRef(raw: String): String =
    val normalized = normalizeText(raw).toLowerCase
    if normalized.isEmpty then "...c5"
    else if normalized.startsWith("...") then normalized
    else if normalized.matches("[a-h]5") then s"...$normalized"
    else if normalized.matches("[a-h]") then s"...${normalized}5"
    else s"...$normalized"

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

  private def renderStrategicIdeaClaim(idea: String): String =
    val normalized = normalizeText(idea)
    val low = normalized.toLowerCase
    if low.contains(" break") || low.contains("pawn break") then s"The move prepares $normalized."
    else if low.startsWith("pressure ") then s"The move increases $normalized."
    else if low.startsWith("exchanges ") || low.contains("favorable exchanges") then s"The move leans toward $normalized."
    else if low.startsWith("fixed targets") then s"The move puts $normalized in view."
    else if low.startsWith("an outpost ") then s"The move points to $normalized."
    else if low.startsWith("space ") then s"The move fights for $normalized."
    else if low.startsWith("stopping ") || low.startsWith("keeping the opponent out") then s"The move is mainly about $normalized."
    else s"The move is aimed at $normalized."

  private def renderExecutionClaim(execution: String): String =
    val normalized = normalizeText(execution)
    if normalized.toLowerCase.startsWith("pressure ") then s"The move is mainly about $normalized."
    else s"The move is mainly about $normalized."

  private def renderCompensationClaimFromIdea(idea: String): String =
    val normalized = normalizeText(idea)
    if normalized.toLowerCase.startsWith("pressure ") then
      s"The point is to keep $normalized before winning the material back."
    else s"The point is to keep $normalized in play before winning the material back."

  private def renderCompensationFollowUp(surface: StrategyPackSurface.Snapshot): Option[String] =
    surface.objectiveText
      .flatMap(objective =>
        StrategicSentenceRenderer.pieceHeadFor(objective).map { case (piece, square) => s"The $piece can head for $square next." }
      )
      .orElse(surface.executionText.flatMap(StrategicSentenceRenderer.renderCompensationFollowUpFromExecution))
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)

  private def surfaceIdeaClaimTooBroad(
      idea: String,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val normalizedIdea = normalizeText(idea)
    val lower = normalizedIdea.toLowerCase
    val squareCount = concreteSquareCount(normalizedIdea)
    val overloadedPressure = lower.startsWith("pressure ") && squareCount >= 3
    overloadedPressure &&
      !surface.objectiveText.exists(sharedConcreteAnchor(normalizedIdea, _)) &&
      !surface.executionText.exists(sharedConcreteAnchor(normalizedIdea, _))

  private def sameStrategicIdea(left: String, right: String): Boolean =
    normalizeSentenceFragment(left).equalsIgnoreCase(normalizeSentenceFragment(right))

  private def sharedConcreteAnchor(left: String, right: String): Boolean =
    concreteAnchors(left).intersect(concreteAnchors(right)).nonEmpty

  private def concreteSquareCount(raw: String): Int =
    """\b[a-h][1-8]\b""".r.findAllMatchIn(Option(raw).getOrElse("")).length

  private def concreteAnchors(raw: String): Set[String] =
    val text = Option(raw).getOrElse("").toLowerCase
    val squares = """\b[a-h][1-8]\b""".r.findAllMatchIn(text).map(_.matched).toSet
    val files = """\b[a-h]-file\b""".r.findAllMatchIn(text).map(_.matched).toSet
    val breaks = """\b[a-h]-?break\b""".r.findAllMatchIn(text).map(_.matched.replace("-", "")).toSet
    squares ++ files ++ breaks
