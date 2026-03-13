package lila.llm.analysis

import lila.llm.NarrativeSignalDigest
import lila.llm.model.{ NarrativeContext, PlanAlignmentInfo, PracticalBiasInfo, StructureProfileInfo }
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeatKind }

object NarrativeSignalDigestBuilder:

  def build(
      ctx: NarrativeContext,
      preservedSignalsOverride: Option[List[String]] = None
  ): Option[NarrativeSignalDigest] =
    buildWithAuthoringEvidence(ctx, preservedSignalsOverride, AuthoringEvidenceSummaryBuilder.headline(ctx))

  def buildWithAuthoringEvidence(
      ctx: NarrativeContext,
      preservedSignalsOverride: Option[List[String]],
      authoringEvidence: Option[String]
  ): Option[NarrativeSignalDigest] =
    val opening =
      ctx.openingData.flatMap(_.name).flatMap(normalized)
        .orElse(ctx.openingEvent.map(_.toString.replace('_', ' ')).flatMap(normalized))
    val strategicStack =
      ctx.mainStrategicPlans.take(3).map(plan => f"${plan.rank}. ${plan.planName} (${plan.score}%.2f)")
    val latentPlan = ctx.latentPlans.headOption.flatMap(plan => normalized(plan.planName))
    val latentReason =
      (ctx.whyAbsentFromTopMultiPV ++ ctx.latentPlans.map(_.whyAbsentFromTopMultiPv))
        .flatMap(normalized)
        .headOption
    val decisionComparison = DecisionComparisonBuilder.digest(ctx)

    val practical = ctx.semantic.flatMap(_.practicalAssessment)
    val compensationInfo = ctx.semantic.flatMap(_.compensation)
    val prevented = ctx.semantic.flatMap(_.preventedPlans.headOption)
    val alignment = ctx.semantic.flatMap(_.planAlignment)
    val structure = ctx.semantic.flatMap(_.structureProfile)
    val structureArc = StructurePlanArcBuilder.build(ctx)

    val practicalVerdict = practical.flatMap(pa => normalized(pa.verdict))
    val practicalFactors =
      practical.toList
        .flatMap(_.biasFactors)
        .sortBy(bias => -math.abs(bias.weight))
        .take(2)
        .flatMap(formatPracticalBias)

    val compensation = compensationInfo.flatMap(comp => normalized(comp.conversionPlan))
    val compensationVectors =
      compensationInfo.toList
        .flatMap(_.returnVector.toList)
        .sortBy { case (_, weight) => -weight }
        .take(3)
        .flatMap(formatCompensationVector)
    val investedMaterial = compensationInfo.map(_.investedMaterial).filter(_ > 0)

    val structureProfile =
      structure.flatMap(sp => normalized(sp.primary).filterNot(_.equalsIgnoreCase("Unknown")))
    val centerState = structure.flatMap(sp => normalized(sp.centerState))
    val alignmentBand = alignment.flatMap(pa => normalized(pa.band))
    val alignmentReasons =
      alignment.toList
        .flatMap(_.reasonCodes)
        .flatMap(humanizeAlignmentReason)
        .distinct
        .take(3)
    val structuralCue = buildStructuralCue(structure, alignment)
    val deployment = structureArc.flatMap(StructurePlanArcBuilder.visibleDeployment)

    val prophylaxisPlan = prevented.flatMap(pp => normalized(pp.planId))
    val prophylaxisThreat =
      prevented.flatMap { pp =>
        pp.preventedThreatType.flatMap(normalized)
          .orElse(pp.breakNeutralized.flatMap(file => normalized(s"$file-break")))
      }
    val counterplayScoreDrop = prevented.map(_.counterplayScoreDrop).filter(_ > 0)

    val decision = ctx.decision.flatMap(d => normalized(d.logicSummary))
    val strategicFlow = ctx.strategicFlow.flatMap(normalized)
    val opponentPlan = ctx.opponentPlan.flatMap(p => normalized(p.name))

    val preservedSignals =
      preservedSignalsOverride.getOrElse(
        List(
          Option.when(opening.isDefined)("opening"),
          Option.when(strategicStack.nonEmpty)("strategic_stack"),
          Option.when(latentPlan.isDefined)("latent_plan"),
          Option.when(authoringEvidence.isDefined)("authoring_evidence"),
          Option.when(practicalVerdict.isDefined || compensation.isDefined)("practical"),
          Option.when(structuralCue.isDefined || prophylaxisPlan.isDefined)("structure"),
          Option.when(decision.isDefined)("decision"),
          Option.when(strategicFlow.isDefined)("strategic_flow"),
          Option.when(opponentPlan.isDefined)("opponent_plan")
        ).flatten
      )

    Option.when(
      preservedSignals.nonEmpty ||
        strategicStack.nonEmpty ||
        practicalFactors.nonEmpty ||
        alignmentReasons.nonEmpty ||
        compensationVectors.nonEmpty
    )(
      NarrativeSignalDigest(
        opening = opening,
        strategicStack = strategicStack,
        latentPlan = latentPlan,
        latentReason = latentReason,
        decisionComparison = decisionComparison,
        authoringEvidence = authoringEvidence,
        practicalVerdict = practicalVerdict,
        practicalFactors = practicalFactors,
        compensation = compensation,
        compensationVectors = compensationVectors,
        investedMaterial = investedMaterial,
        structuralCue = structuralCue,
        structureProfile = structureProfile,
        centerState = centerState,
        alignmentBand = alignmentBand,
        alignmentReasons = alignmentReasons,
        deploymentOwnerSide = deployment.map(_.ownerSide),
        deploymentPiece = deployment.map(_.piece),
        deploymentRoute = deployment.map(_.routeSquares).getOrElse(Nil),
        deploymentPurpose = deployment.map(_.purpose),
        deploymentContribution = structureArc.map(_.moveContribution).filter(_.nonEmpty),
        deploymentStrategicFit = deployment.map(_.strategicFit),
        deploymentTacticalSafety = deployment.map(_.tacticalSafety),
        deploymentSurfaceConfidence = deployment.map(_.surfaceConfidence),
        deploymentSurfaceMode = deployment.map(_.surfaceMode),
        prophylaxisPlan = prophylaxisPlan,
        prophylaxisThreat = prophylaxisThreat,
        counterplayScoreDrop = counterplayScoreDrop,
        decision = decision,
        strategicFlow = strategicFlow,
        opponentPlan = opponentPlan,
        preservedSignals = preservedSignals
      )
    )

  def preservedSignalsFromOutline(
      ctx: NarrativeContext,
      outline: NarrativeOutline
  ): List[String] =
    val hasPractical = ctx.semantic.flatMap(_.practicalAssessment).isDefined || ctx.semantic.flatMap(_.compensation).isDefined
    val hasStructure = ctx.semantic.flatMap(_.planAlignment).isDefined || ctx.semantic.flatMap(_.preventedPlans.headOption).isDefined
    outline.beats.flatMap { beat =>
      beat.kind match
        case OutlineBeatKind.Context =>
          List(
            Option.when(beat.conceptIds.contains("opening_context"))("opening"),
            Option.when(beat.conceptIds.contains("strategic_stack"))("strategic_stack"),
            Option.when(beat.conceptIds.contains("structural_context") && hasStructure)("structure"),
            Option.when(beat.questionIds.nonEmpty || beat.questionKinds.nonEmpty)("authoring_evidence"),
            Option.when(beat.conceptIds.contains("strategic_flow"))("strategic_flow"),
            Option.when(beat.conceptIds.contains("opponent_plan"))("opponent_plan")
          ).flatten
        case OutlineBeatKind.DecisionPoint => List("decision")
        case OutlineBeatKind.ConditionalPlan => List("latent_plan", "authoring_evidence")
        case OutlineBeatKind.Evidence => List("authoring_evidence")
        case OutlineBeatKind.OpeningTheory => List("opening_theory")
        case OutlineBeatKind.MainMove if hasPractical => List("practical")
        case OutlineBeatKind.WrapUp if beat.conceptIds.contains("practical_assessment") => List("wrap_up")
        case _ => Nil
    }.distinct

  private def buildStructuralCue(
      structure: Option[StructureProfileInfo],
      alignment: Option[PlanAlignmentInfo]
  ): Option[String] =
    val base =
      (structure.flatMap(sp => normalized(sp.primary).filterNot(_.equalsIgnoreCase("Unknown"))), structure.flatMap(sp => normalized(sp.centerState))) match
        case (Some(profile), Some(center)) => Some(s"$profile structure with a ${center.toLowerCase} center")
        case (Some(profile), None)         => Some(s"$profile structure")
        case (None, Some(center))          => Some(s"${center.toLowerCase} center")
        case _                             => None

    alignment.flatMap { pa =>
      val band = normalized(pa.band).map(_.toLowerCase)
      val intent = pa.narrativeIntent.flatMap(normalized)
      val risk = pa.narrativeRisk.flatMap(normalized)
      val summary =
        List(
          base,
          band.map(b => s"plan fit $b"),
          intent,
          risk
        ).flatten.distinct
      Option.when(summary.nonEmpty)(summary.mkString("; "))
    }.orElse(base)

  private def formatPracticalBias(bias: PracticalBiasInfo): Option[String] =
    normalized(bias.factor).map { factor =>
      val desc = normalized(bias.description)
      desc match
        case Some(d) => s"$factor: $d"
        case None    => factor
    }

  private def formatCompensationVector(entry: (String, Double)): Option[String] =
    val (label, weight) = entry
    normalized(label).map(l => s"$l (${f"$weight%.1f"})")

  private def humanizeAlignmentReason(raw: String): Option[String] =
    normalized(raw).map(_.toUpperCase).map {
      case "PA_MATCH"     => "expected plans are present"
      case "PRECOND_MISS" => "some structural preconditions are missing"
      case "ANTI_PLAN"    => "the move order fights the structure"
      case "LOW_CONF"     => "the structure read is uncertain"
      case code if code.startsWith("TOP_") => "the current top plan disagrees with the structure template"
      case code => code.toLowerCase.replace('_', ' ')
    }

  private def normalized(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)
