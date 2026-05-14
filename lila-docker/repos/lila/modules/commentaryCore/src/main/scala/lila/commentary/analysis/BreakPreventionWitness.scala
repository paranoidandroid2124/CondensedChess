package lila.commentary.analysis

import chess.{ Board, Color }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.StrategicIdeaKind
import lila.commentary.analysis.strategic.BreakClampMaterializer
import lila.commentary.model.{ FactScope, NarrativeContext, PreventedPlanInfo, StrategicPlanExperiment }
import lila.commentary.model.strategic.VariationLine

object BreakPreventionWitness:

  object Failure:
    val NoPreventedPlan = "no_prevented_plan"
    val NoNamedBreak = "no_named_break"
    val BranchMissing = "branch_missing"
    val PersistenceUnstable = "persistence_unstable"
    val RivalRelease = "rival_release"
    val TacticalFirst = "tactical_first"
    val FamilyMismatch = "family_mismatch"
    val RouteIdentityMissing = "route_identity_missing"
    val RouteStillLegal = "route_still_legal"
    val SameRouteRestored = "same_route_restored"
    val CaptureTransformRisk = "capture_transform_risk"
    val CaptureTransformUnanswered = "capture_transform_unanswered"
    val CaptureTransformRecaptureUnproven = "capture_transform_recapture_unproven"
    val CaptureTransformRecaptureStillReleases = "capture_transform_recapture_still_releases"
    val BranchUnstable = "branch_unstable"

  final case class Witness(
      planId: String,
      breakToken: String,
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  )

  final case class Diagnosis(
      witness: Option[Witness],
      failureCodes: List[String]
  ):
    def exactReady: Boolean = witness.nonEmpty && failureCodes.isEmpty

  def diagnose(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      preventedNow: List[PreventedPlanInfo]
  ): Diagnosis =
    val currentPlans = preventedNow.filter(_.sourceScope == FactScope.Now)
    val routeEvidence = routeEvidenceFor(ctx)
    if tacticalFirst(ctx) then Diagnosis(None, List(Failure.TacticalFirst))
    else if currentPlans.isEmpty then
      val routeFailures = routeEvidence.flatMap(routeFailure).distinct
      Diagnosis(None, if routeFailures.nonEmpty then routeFailures else List(Failure.NoPreventedPlan))
    else
      val breakPlans =
        currentPlans.flatMap(plan =>
          clean(plan.breakNeutralized)
            .filter(_ => plan.counterplayScoreDrop > 0 || plan.mobilityDelta < 0)
            .map(token => plan -> token)
        )
      if breakPlans.isEmpty then Diagnosis(None, List(Failure.NoNamedBreak))
      else if !familyAligned(ctx, surface) then Diagnosis(None, List(Failure.FamilyMismatch))
      else
        val (plan, token) = breakPlans.maxBy { case (plan, _) =>
          math.max(plan.counterplayScoreDrop, -plan.mobilityDelta)
        }
        val witness = Witness(
          planId = plan.planId,
          breakToken = token,
          ownerSeedTerms = (List(token) ++ plan.deniedSquares.flatMap(clean)).distinct,
          structureTransitionTerms = (List(token) ++ plan.deniedSquares.flatMap(clean)).distinct
        )
        val routeFailureOpt = routeFailureFor(token, routeEvidence, ctx)
        val routeStable = routePersistenceStable(token, routeEvidence)
        val failures =
          List(
            routeFailureOpt,
            Option.when(bestDefenseBranchKey(ctx).isEmpty)(Failure.BranchMissing),
            Option.when(routeFailureOpt.isEmpty && !stableFamilyEvidence(ctx) && !routeStable)(
              Failure.PersistenceUnstable
            ),
            Option.when(hasRivalRelease(ctx))(Failure.RivalRelease)
          ).flatten.distinct
        Diagnosis(Some(witness), failures)

  def candidate(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      preventedNow: List[PreventedPlanInfo]
  ): Option[Witness] =
    diagnose(ctx, surface, preventedNow).witness

  def exact(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      preventedNow: List[PreventedPlanInfo]
  ): Option[Witness] =
    val result = diagnose(ctx, surface, preventedNow)
    Option.when(result.exactReady)(result.witness).flatten

  def anchorTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      preventedNow: List[PreventedPlanInfo]
  ): List[String] =
    candidate(ctx, surface, preventedNow).toList.flatMap(_.ownerSeedTerms).distinct

  private def familyAligned(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    ctx.mainStrategicPlans.exists(plan => plan.subplanId.exists(normalize(_) == ThemeTaxonomy.SubplanId.BreakPrevention.id)) ||
      ctx.strategicPlanExperiments.exists(alignedExperiment) ||
      List(surface.dominantIdea, surface.secondaryIdea).flatten.exists(_.kind == StrategicIdeaKind.CounterplaySuppression)

  private def alignedExperiment(experiment: StrategicPlanExperiment): Boolean =
    experiment.subplanId.exists(normalize(_) == ThemeTaxonomy.SubplanId.BreakPrevention.id) ||
      experiment.counterBreakNeutralized

  private def stableFamilyEvidence(ctx: NarrativeContext): Boolean =
    ctx.strategicPlanExperiments
      .filter(alignedExperiment)
      .exists(exp => exp.bestReplyStable && exp.futureSnapshotAligned && !exp.moveOrderSensitive)

  private def hasRivalRelease(ctx: NarrativeContext): Boolean =
    ctx.strategicPlanExperiments.filter(alignedExperiment).exists(_.moveOrderSensitive)

  private def tacticalFirst(ctx: NarrativeContext): Boolean =
    normalize(ctx.header.criticality) == "forced"

  private def bestDefenseBranchKey(ctx: NarrativeContext): Option[String] =
    bestDefenseBranchKey(ctx.engineEvidence.toList.flatMap(_.variations))

  private def bestDefenseBranchKey(variations: List[VariationLine]): Option[String] =
    variations.headOption.flatMap(line =>
      branchKey(line.moves)
        .orElse(branchKey(line.parsedMoves.flatMap(move => clean(move.uci))))
    )

  private def routeEvidenceFor(ctx: NarrativeContext): List[BreakClampMaterializer.BreakRouteEvidence] =
    routeRuntimeInputs(ctx).flatMap { case (board, color, line) =>
      BreakClampMaterializer.routeEvidence(
        fen = ctx.fen,
        board = board,
        color = color,
        mainLine = line
      )
    }

  private def routeRuntimeInputs(ctx: NarrativeContext): List[(Board, Color, VariationLine)] =
    for
      pos <- Fen.read(Standard, Fen.Full(ctx.fen)).toList
      played <- ctx.playedMove.toList.flatMap(clean)
      line <- routeEvidenceLine(ctx, played).toList
    yield (pos.board, pos.color, line)

  private def routeEvidenceLine(ctx: NarrativeContext, played: String): Option[VariationLine] =
    val best = ctx.engineEvidence.flatMap(_.best)
    best match
      case Some(line) if line.moves.headOption.contains(played) => Some(line)
      case Some(line) if line.moves.nonEmpty                    => Some(line.copy(moves = played :: line.moves.drop(1)))
      case _                                                    => Some(VariationLine(List(played), scoreCp = 0, depth = 0))

  private def routeFailureFor(
      token: String,
      evidence: List[BreakClampMaterializer.BreakRouteEvidence],
      ctx: NarrativeContext
  ): Option[String] =
    evidence
      .find(route => normalize(route.token) == normalize(token) || normalize(route.destinationToken) == normalize(token))
      .flatMap(routeFailure)
      .orElse(routeRecheckFailure(token, ctx))

  private def routeFailure(evidence: BreakClampMaterializer.BreakRouteEvidence): Option[String] =
    evidence.transformRisk match
      case BreakClampMaterializer.BreakTransformRisk.CaptureTransform =>
        captureTransformFailure(evidence.transformAssessments)
      case BreakClampMaterializer.BreakTransformRisk.SameRouteRestored => Some(Failure.SameRouteRestored)
      case BreakClampMaterializer.BreakTransformRisk.BranchUnstable    => Some(Failure.BranchUnstable)
      case BreakClampMaterializer.BreakTransformRisk.None              => None

  private def routePersistenceStable(
      token: String,
      evidence: List[BreakClampMaterializer.BreakRouteEvidence]
  ): Boolean =
    evidence.exists(route =>
      (normalize(route.token) == normalize(token) || normalize(route.destinationToken) == normalize(token)) &&
        route.transformRisk == BreakClampMaterializer.BreakTransformRisk.None &&
        route.sourceLine.exists(_.moves.size >= 4)
    )

  private def captureTransformFailure(
      assessments: List[BreakClampMaterializer.BreakTransformAssessment]
  ): Option[String] =
    if assessments.exists(_.verdict == BreakClampMaterializer.BreakTransformVerdict.RecaptureStillReleases) then
      Some(Failure.CaptureTransformRecaptureStillReleases)
    else if assessments.exists(_.verdict == BreakClampMaterializer.BreakTransformVerdict.UnansweredCapture) then
      Some(Failure.CaptureTransformUnanswered)
    else if assessments.exists(_.verdict == BreakClampMaterializer.BreakTransformVerdict.RecaptureAvailableUnproven) then
      Some(Failure.CaptureTransformRecaptureUnproven)
    else Some(Failure.CaptureTransformRisk)

  private def routeRecheckFailure(token: String, ctx: NarrativeContext): Option[String] =
    val inputs = routeRuntimeInputs(ctx)
    if inputs.exists { case (board, color, line) =>
        BreakClampMaterializer.routeStillLegal(ctx.fen, board, color, line, token)
      }
    then Some(Failure.RouteStillLegal)
    else if routeTokenLike(token) && inputs.nonEmpty && !inputs.exists { case (board, color, line) =>
        BreakClampMaterializer.routeIdentityExists(ctx.fen, board, color, line, token)
      }
    then Some(Failure.RouteIdentityMissing)
    else None

  private def routeTokenLike(token: String): Boolean =
    normalize(token).contains("-")

  private def branchKey(moves: List[String]): Option[String] =
    moves.take(2).flatMap(clean) match
      case first :: second :: Nil => Some(s"${normalize(first)}|${normalize(second)}")
      case _                      => None

  private def clean(raw: Option[String]): Option[String] =
    raw.flatMap(clean)

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase).getOrElse("")
