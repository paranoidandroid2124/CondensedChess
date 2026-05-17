package lila.commentary.tools.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.claim.*
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import lila.commentary.PgnAnalysisHelper
import lila.commentary.analysis.render.QuietStrategicSupportComposer
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, NarrativeOutline }
import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.realpgn.RealPgnNarrativeEvalRunner

private[commentary] object SourceReview:

  object Verdict:
    val AdmitAuthorityRow = "admit_authority_row"
    val ScreenOnly = "screen_only"
    val RejectTacticalFirst = "reject_tactical_first"
    val RejectOwnerMissing = "reject_owner_missing"
    val RejectReplayFailed = "reject_replay_failed"

  object Diagnosis:
    val AdmitReady = "admit_ready"
    val EngineMissingBeforeAdmission = "engine_missing_before_admission"
    val EngineTopMoveDisagrees = "engine_top_move_disagrees"
    val EngineNearTopMultiPvSourceMove = "engine_near_top_multipv_source_move"
    val EngineMultipvOnlySourceMove = "engine_multipv_only_source_move"
    val TacticalFirstSource = "tactical_first_source"
    val ReplayFailed = "replay_failed"
    val PlannerOwnerSceneBlocked = "planner_owner_scene_blocked"
    val PlannerOwnerSuppressed = "planner_owner_suppressed"
    val RootVocabularyOrExtractionGap = "root_vocabulary_or_extraction_gap"
    val MoveOwnerMissing = "move_owner_missing"
    val SurfaceContractBlocked = "surface_contract_blocked"
    val OwnerMissingUnclassified = "owner_missing_unclassified"

  object EngineGate:
    val EngineMissing = "engine_missing"
    val SourceMoveTopPv = "source_move_top_pv"
    val SourceMoveNearTopMultiPv = "source_move_near_top_multipv"
    val SourceMoveMultiPvOnly = "source_move_multipv_only"
    val SourceMoveAbsentFromMultiPv = "source_move_absent_from_multipv"
    val PvAvailableNoFirstMove = "pv_available_no_first_move"

  private val NearTopMultiPvMaxGapCp = 50

  private[commentary] trait SourceReviewEngine:
    def newGame(): Unit
    def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine]

  private final class LocalSourceReviewEngine(engine: RealPgnNarrativeEvalRunner.LocalUciEngine) extends SourceReviewEngine:
    override def newGame(): Unit = engine.newGame()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      engine.analyze(fen, depth, multiPv)

  final case class Observation(
      source: SourceWitnessCatalog.SourceCandidate,
      verdict: String,
      diagnosis: String,
      engineAgreement: String,
      plannerOwnership: String,
      surfaceGate: String,
      release: String,
      taxonomy: String,
      ply: Option[Int],
      fen: Option[String],
      playedUci: Option[String],
      enginePv: List[String],
      primary: String,
      bookmaker: String,
      chronicle: String,
      reason: String,
      mainProofSource: String = "-",
      mainClaimScope: String = "-",
      packetSummary: String = "-",
      contractId: String = "-",
      contractStatus: String = "-",
      contractFailures: String = "-",
      sceneType: String = "-",
      ownerTraceSummary: String = "-",
      ownerFailureCodes: String = "-",
      admissionBlockers: String = "none"
  ):
    def tsv: String =
      List(
        source.id,
        verdict,
        diagnosis,
        clean(admissionBlockers),
        clean(ownerFailureCodes),
        engineAgreement,
        plannerOwnership,
        surfaceGate,
        mainProofSource,
        mainClaimScope,
        clean(packetSummary),
        contractId,
        contractStatus,
        clean(contractFailures),
        sceneType,
        clean(ownerTraceSummary),
        release,
        taxonomy,
        ply.fold("-")(_.toString),
        clean(fen.getOrElse("-")),
        playedUci.getOrElse("-"),
        clean(enginePv.mkString(" ")),
        clean(primary),
        clean(bookmaker),
        clean(chronicle),
        clean(reason)
      ).mkString("\t")

  private final case class EvaluationSurface(
      release: String,
      primary: String,
      bookmaker: String,
      chronicle: String,
      rejected: String,
      ownerTrace: PlannerOwnerTrace,
      mainClaimScope: Option[String],
      mainProofSource: Option[String],
      packetSummary: Option[String],
      contractId: Option[String],
      contractStatus: Option[String],
      contractFailures: List[String],
      ownerFailureCodes: List[String],
      breakPreventionFailureCodes: List[String],
      releaseDecision: Option[String]
  )

  private val header =
    List(
      "id",
      "verdict",
      "diagnosis",
      "admissionBlockers",
      "ownerFailureCodes",
      "engineAgreement",
      "plannerOwnership",
      "surfaceGate",
      "mainProofSource",
      "mainClaimScope",
      "packetSummary",
      "contractId",
      "contractStatus",
      "contractFailures",
      "sceneType",
      "ownerTrace",
      "release",
      "taxonomy",
      "ply",
      "fen",
      "playedUci",
      "enginePv",
      "primary",
      "bookmaker",
      "chronicle",
      "reason"
    ).mkString("\t")

  private[commentary] def observations(
      engine: Option[RealPgnNarrativeEvalRunner.LocalUciEngine],
      depth: Int = 16,
      multiPv: Int = 3
  ): List[Observation] =
    observationsWithEngine(engine.map(LocalSourceReviewEngine(_)), depth, multiPv)

  private[commentary] def observationsWithEngine(
      engine: Option[SourceReviewEngine],
      depth: Int = 16,
      multiPv: Int = 3,
      sourceIds: Set[String] = Set.empty
  ): List[Observation] =
    SourceWitnessCatalog.all
      .filter(source => sourceIds.isEmpty || sourceIds.contains(source.id))
      .map(observe(_, engine, depth, multiPv))

  private[commentary] def observationsForSources(
      sources: List[SourceWitnessCatalog.SourceCandidate],
      engine: Option[SourceReviewEngine],
      depth: Int = 16,
      multiPv: Int = 3
  ): List[Observation] =
    sources.map(observe(_, engine, depth, multiPv))

  private[commentary] def windowObservations(
      engine: Option[RealPgnNarrativeEvalRunner.LocalUciEngine],
      depth: Int = 16,
      multiPv: Int = 3,
      sourceIds: Set[String] = Set.empty
  ): List[Observation] =
    windowObservationsWithEngine(engine.map(LocalSourceReviewEngine(_)), depth, multiPv, sourceIds)

  private[commentary] def windowObservationsWithEngine(
      engine: Option[SourceReviewEngine],
      depth: Int = 16,
      multiPv: Int = 3,
      sourceIds: Set[String] = Set.empty
  ): List[Observation] =
    SourceWitnessCatalog.all
      .filter(source => sourceIds.isEmpty || sourceIds.contains(source.id))
      .flatMap(source =>
        PgnAnalysisHelper.extractPlyDataStrict(source.pgn) match
          case Left(err) =>
            List(replayFailedObservation(source, err))
          case Right(plyData) =>
            val window = plyData.filter(ply => source.candidatePlyRange.contains(ply.ply))
            if window.isEmpty then
              List(replayFailedObservation(source, s"candidate window ${source.candidatePlyRange} resolved to no ply"))
            else
              window.map(ply => observePly(source, ply, plyData.size, engine, depth, multiPv))
      )

  private def observe(
      source: SourceWitnessCatalog.SourceCandidate,
      engine: Option[SourceReviewEngine],
      depth: Int,
      multiPv: Int
  ): Observation =
    PgnAnalysisHelper.extractPlyDataStrict(source.pgn) match
      case Left(err) =>
        replayFailedObservation(source, err)
      case Right(plyData) =>
        val window = plyData.filter(ply => source.candidatePlyRange.contains(ply.ply))
        window.headOption match
          case None =>
            replayFailedObservation(source, s"candidate window ${source.candidatePlyRange} resolved to no ply")
          case Some(ply) =>
            observePly(source, ply, plyData.size, engine, depth, multiPv)

  private def replayFailedObservation(
      source: SourceWitnessCatalog.SourceCandidate,
      err: String
  ): Observation =
    Observation(
      source,
      Verdict.RejectReplayFailed,
      Diagnosis.ReplayFailed,
      "replay_failed",
      "not_evaluated",
      "not_reached",
      "-",
      taxonomy(source),
      None,
      None,
      None,
      Nil,
      "-",
      "-",
      "-",
      err,
      admissionBlockers = "replay:failed"
    )

  private def observePly(
      source: SourceWitnessCatalog.SourceCandidate,
      ply: PgnAnalysisHelper.PlyData,
      totalPlies: Int,
      engine: Option[SourceReviewEngine],
      depth: Int,
      multiPv: Int
  ): Observation =
    val tacticalFirst = source.intendedVerdict == Verdict.RejectTacticalFirst || source.reviewGroup.toLowerCase.contains("tactical")
    val engineLines =
      engine.toList.flatMap { e =>
        e.newGame()
        e.analyze(ply.fen, depth = depth, multiPv = multiPv)
      }
    if tacticalFirst then
      Observation(
        source,
        Verdict.RejectTacticalFirst,
        Diagnosis.TacticalFirstSource,
        engineAgreement(ply.playedUci, engineLines),
        "not_evaluated_tactical_first",
        "not_reached_tactical_first",
        "-",
        taxonomy(source),
        Some(ply.ply),
        Some(ply.fen),
        Some(ply.playedUci),
        engineLines.headOption.toList.flatMap(_.moves),
        "-",
        "-",
        "-",
        "tactical-first source candidate stays out of B/C strategic admission",
        admissionBlockers = "tactical:first"
      )
    else if engineLines.isEmpty then
      Observation(
        source,
        Verdict.RejectOwnerMissing,
        Diagnosis.EngineMissingBeforeAdmission,
        "engine_missing",
        "not_evaluated_engine_missing",
        "not_reached_engine_missing",
        "-",
        taxonomy(source),
        Some(ply.ply),
        Some(ply.fen),
        Some(ply.playedUci),
        Nil,
        "-",
        "-",
        "-",
        "engine_missing_before_admission",
        admissionBlockers = "engine:missing"
      )
    else
      val engineGate = engineAuthorityGate(ply.playedUci, engineLines)
      val surfaceEngineLines =
        engineLinesForSurface(ply.fen, ply.playedUci, engineLines, engineGate)
      val surface =
        evaluateSurface(
          ply.fen,
          surfaceEngineLines,
          phaseFromPly(ply.ply, totalPlies),
          ply.ply,
          Some(ply.playedUci)
        )
      val surfaceOk = supportedLocalSurfaceSafe(surface)
      val familyAligned = sourceReviewGroupAligned(source, surface)
      val rawOwnerDiagnosis =
        classifyAdmission(
          admitted = surface.release == "CertifiedOwner" ||
            surface.release == "SupportedLocal" && surfaceOk,
          release = surface.release,
          rejected = surface.rejected,
          mainClaimScope = surface.mainClaimScope,
          ownerTrace = surface.ownerTrace,
          supportedLocalSurfaceOk = surfaceOk
        )
      val ownerDiagnosis =
        if familyAligned then rawOwnerDiagnosis
        else Diagnosis.RootVocabularyOrExtractionGap
      val certifiedOwnerAdmitted =
        familyAligned &&
          engineGate == EngineGate.SourceMoveTopPv &&
          surface.release == "CertifiedOwner"
      val supportedLocalAdmitted =
        familyAligned &&
          (engineGate == EngineGate.SourceMoveTopPv ||
            engineGate == EngineGate.SourceMoveNearTopMultiPv) &&
          surface.release == "SupportedLocal" &&
          surfaceOk
      val admitted =
        certifiedOwnerAdmitted || supportedLocalAdmitted
      val diagnosis =
        if admitted then Diagnosis.AdmitReady
        else
          engineGate match
            case EngineGate.SourceMoveAbsentFromMultiPv => Diagnosis.EngineTopMoveDisagrees
            case EngineGate.SourceMoveNearTopMultiPv    => ownerDiagnosis
            case EngineGate.SourceMoveMultiPvOnly       => Diagnosis.EngineMultipvOnlySourceMove
            case EngineGate.PvAvailableNoFirstMove      => Diagnosis.EngineTopMoveDisagrees
            case _                                      => ownerDiagnosis
      val verdict =
        if admitted then Verdict.AdmitAuthorityRow
        else Verdict.RejectOwnerMissing
      val gate = surfaceGate(surface, surfaceOk)
      val ownerFailures = ownerFailureCodes(source, surface)
      val blockers =
        admissionBlockers(
          source = source,
          admitted = admitted,
          engineGate = engineGate,
          ownerDiagnosis = ownerDiagnosis,
          surfaceGate = gate,
          ownerFailureCodes = ownerFailures
        )
      Observation(
        source,
        verdict,
        diagnosis,
        engineAgreement(ply.playedUci, engineLines),
        plannerOwnership(surface),
        gate,
        if !admitted && source.reviewGroup.toLowerCase.contains("central_break_timing") then "-"
        else surface.release,
        taxonomy(source),
        Some(ply.ply),
        Some(ply.fen),
        Some(ply.playedUci),
        engineLines.headOption.toList.flatMap(_.moves),
        surface.primary,
        surface.bookmaker,
        surface.chronicle,
        if admitted then "exact replay and surfaces agree" else s"admission_blocked: $blockers; planner_owner_missing_or_surface_unsafe: ${surface.rejected}",
        mainProofSource = surface.mainProofSource.getOrElse("-"),
        mainClaimScope = surface.mainClaimScope.getOrElse("-"),
        packetSummary = surface.packetSummary.getOrElse("-"),
        contractId = surface.contractId.getOrElse("-"),
        contractStatus = surface.contractStatus.getOrElse("-"),
        contractFailures =
          if surface.contractFailures.isEmpty then "-" else surface.contractFailures.distinct.mkString("+"),
        sceneType = surface.ownerTrace.sceneType.wireName,
        ownerTraceSummary = ownerTraceSummary(surface.ownerTrace),
        ownerFailureCodes =
          if ownerFailures.isEmpty then "-" else ownerFailures.distinct.mkString("+"),
        admissionBlockers = blockers
      )

  private def evaluateSurface(
      fen: String,
      engineLines: List[VariationLine],
      phase: String,
      ply: Int,
      playedMove: Option[String]
  ): EvaluationSurface =
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = engineLines,
          playedMove = playedMove,
          phase = Some(phase),
          ply = ply,
          prevMove = playedMove
        )
        .getOrElse(sys.error(s"analysis missing for corpus FEN: $fen"))
    val ctx =
      NarrativeContextBuilder
        .build(data, data.toContext, None)
        .copy(authorQuestions = defaultQuestions)
    val pack =
      StrategyPackBuilder
        .build(data, ctx)
        .getOrElse(sys.error(s"strategy pack missing for corpus FEN: $fen"))
    val surfaceSnapshot = StrategyPackSurface.from(Some(pack))
    val inputs = QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val primary = ranked.primary.map(_.claim).getOrElse("-")
    val release = ranked.primary.flatMap(positiveRelease).getOrElse("-")
    val mainClaim =
      inputs.mainBundle.flatMap(_.mainClaim)
    val positionProbe =
      mainClaim.filter(_.scope == PlayerFacingClaimScope.PositionLocal)
    val releaseDecision =
      positionProbe.flatMap(_.packet).map(packet =>
        releaseDecisionSummary(ClaimAuthorityPolicy.decidePositionProbe(Some(ctx), inputs, None, packet))
      )
    val proofTrace =
      mainClaim.flatMap(_.packet).map(_.proofTrace)
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(pack), truthContract = None)
    val bookmaker =
      Option(
        clean(
          LiveNarrativeCompressionCore.deterministicProse(
            BookmakerLiveCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
              ctx = ctx,
              inputs = inputs,
              rankedPlans = ranked,
              strategyPack = Some(pack),
              truthContract = None
            )
          )
        )
      ).filter(_.nonEmpty).getOrElse("-")
    val chronicle =
      chronicleNarrative(ctx, pack, inputs, ranked, outline)
    EvaluationSurface(
      release = release,
      primary = primary,
      bookmaker = bookmaker,
      chronicle = chronicle,
      rejected = ranked.rejected.map(r => s"${r.questionKind}:${r.reasons.mkString("+")}").mkString(" | "),
      ownerTrace = ranked.ownerTrace,
      mainClaimScope = mainClaim.map(_.scope.toString),
      mainProofSource = mainClaim.flatMap(_.packet).map(_.proofSource).orElse(mainClaim.map(_.sourceKind)),
      packetSummary = mainClaim.flatMap(_.packet).map(packetSummary),
      contractId = proofTrace.flatMap(_.contractId),
      contractStatus = proofTrace.flatMap(_.contractStatus),
      contractFailures = proofTrace.toList.flatMap(_.failureCodes),
      ownerFailureCodes = PlayerFacingTruthModePolicy.iqpInducementFailureCodes(ctx),
      breakPreventionFailureCodes =
        BreakPreventionWitness
          .diagnose(ctx, surfaceSnapshot, inputs.preventedPlansNow)
          .failureCodes
          .map(code => s"break_prevention_$code"),
      releaseDecision = releaseDecision
    )

  private[commentary] def classifyAdmission(
      admitted: Boolean,
      release: String,
      rejected: String,
      mainClaimScope: Option[String],
      ownerTrace: PlannerOwnerTrace,
      supportedLocalSurfaceOk: Boolean
  ): String =
    if admitted then Diagnosis.AdmitReady
    else if release == "SupportedLocal" && !supportedLocalSurfaceOk then Diagnosis.SurfaceContractBlocked
    else if rejected.contains("position_probe_support_only_outside_quiet_scene") ||
        ownerTrace.droppedPlannerOwners.exists(dropped =>
          dropped.plannerOwnerKind == PlannerOwnerKind.PositionProbe &&
            dropped.reasons.exists(_.contains("position_probe_support_only_outside_quiet_scene"))
        )
    then Diagnosis.PlannerOwnerSceneBlocked
    else if mainClaimScope.contains("PositionLocal") ||
        ownerTrace.ownerCandidates.exists(_.plannerOwnerKind == PlannerOwnerKind.PositionProbe)
    then Diagnosis.PlannerOwnerSuppressed
    else if rejected.contains("position_probe_missing") then Diagnosis.RootVocabularyOrExtractionGap
    else if rejected.contains("missing_move_owner") then Diagnosis.MoveOwnerMissing
    else Diagnosis.OwnerMissingUnclassified

  private def positiveRelease(plan: QuestionPlan): Option[String] =
    if plan.admissibilityReasons.contains("strategic_claim_supported_local") then Some("SupportedLocal")
    else if plan.admissibilityReasons.contains("certified_position_probe") ||
        plan.admissibilityReasons.contains("exact_target_state_delta") ||
        plan.sourceKinds.exists(_ == PlanTaxonomy.PlanKind.SimplificationWindow.id) ||
        plan.claim.toLowerCase.contains("same local edge")
    then Some("CertifiedOwner")
    else None

  private def supportedLocalSurfaceSafe(surface: EvaluationSurface): Boolean =
    val claimOnly =
      surface.primary == surface.bookmaker &&
        surface.primary == surface.chronicle
    claimOnly &&
      List(surface.primary, surface.bookmaker, surface.chronicle).forall { text =>
        val low = text.toLowerCase
        low.contains("a local reading is that") &&
          !low.contains("the key strategic fact") &&
          !low.contains("so the task is")
      }

  private def sourceReviewGroupAligned(
      source: SourceWitnessCatalog.SourceCandidate,
      surface: EvaluationSurface
  ): Boolean =
    val reviewGroup = source.reviewGroup.toLowerCase
    if reviewGroup.contains("break_prevention") then
      surface.mainProofSource.contains("counterplay_axis_suppression") ||
        surface.packetSummary.exists(_.contains("proof_family=neutralize_key_break"))
    else if reviewGroup.contains("central_break_timing") then
      surface.mainProofSource.contains(PlanTaxonomy.PlanKind.CentralBreakTiming.id) ||
        surface.packetSummary.exists(_.contains(s"proof_family=${PlanTaxonomy.PlanKind.CentralBreakTiming.id}"))
    else if reviewGroup.contains("prophylaxis_restraint") then
      surface.release == "SupportedLocal" &&
        surface.mainProofSource.contains("prophylactic_move") &&
        surface.packetSummary.exists(_.contains("proof_family=counterplay_restraint")) &&
        surface.contractId.exists(_.contains("counterplay_restraint"))
    else true

  private def plannerOwnership(surface: EvaluationSurface): String =
    if surface.release == "CertifiedOwner" || surface.release == "SupportedLocal" then
      s"primary_${surface.release}"
    else if surface.rejected.contains("position_probe_support_only_outside_quiet_scene") then
      "position_probe_scene_blocked"
    else surface.mainClaimScope match
      case Some("PositionLocal") => surface.releaseDecision.getOrElse("position_probe_present_unreleased")
      case Some(scope)           => s"main_claim_scope=$scope"
      case None if surface.rejected.contains("position_probe_missing") =>
        "position_probe_missing"
      case None => "main_claim_missing"

  private def surfaceGate(surface: EvaluationSurface, surfaceOk: Boolean): String =
    surface.release match
      case "SupportedLocal" if surfaceOk => "supported_local_surface_passed"
      case "SupportedLocal"              => "supported_local_surface_failed"
      case "CertifiedOwner"              => "certified_owner_surface_not_blocking"
      case _                             => "not_reached_no_release"

  private def engineAgreement(playedUci: String, engineLines: List[VariationLine]): String =
    if engineLines.isEmpty then "engine_missing"
    else
      val firstMoves = engineLines.flatMap(_.moves.headOption)
      firstMoves.headOption match
        case Some(top) if top == playedUci => "top_pv_matches_played"
        case Some(top) if firstMoves.contains(playedUci) && nearTopGapCp(playedUci, engineLines).nonEmpty =>
          s"near_top_multipv_contains_played_top=${top}_gap=${nearTopGapCp(playedUci, engineLines).get}cp"
        case Some(top) if firstMoves.contains(playedUci) => s"multipv_contains_played_top=$top"
        case Some(top) => s"pv_available_top_differs:$top"
        case None      => "pv_available_no_first_move"

  private[commentary] def engineAuthorityGate(playedUci: String, engineLines: List[VariationLine]): String =
    if engineLines.isEmpty then EngineGate.EngineMissing
    else
      val firstMoves = engineLines.flatMap(_.moves.headOption)
      firstMoves.headOption match
        case Some(top) if top == playedUci => EngineGate.SourceMoveTopPv
        case Some(_) if firstMoves.contains(playedUci) && nearTopGapCp(playedUci, engineLines).nonEmpty =>
          EngineGate.SourceMoveNearTopMultiPv
        case Some(_) if firstMoves.contains(playedUci) =>
          EngineGate.SourceMoveMultiPvOnly
        case Some(_) => EngineGate.SourceMoveAbsentFromMultiPv
        case None    => EngineGate.PvAvailableNoFirstMove

  private def nearTopGapCp(playedUci: String, engineLines: List[VariationLine]): Option[Int] =
    for
      top <- engineLines.headOption
      playedLine <- engineLines.find(_.moves.headOption.contains(playedUci))
      gap = math.abs(top.effectiveScore - playedLine.effectiveScore)
      if gap <= NearTopMultiPvMaxGapCp
    yield gap

  private def engineLinesForSurface(
      fen: String,
      playedUci: String,
      engineLines: List[VariationLine],
      engineGate: String
  ): List[VariationLine] =
    if engineGate != EngineGate.SourceMoveNearTopMultiPv then engineLines
    else
      val top = engineLines.headOption
      val (playedBranch, otherBranches) =
        engineLines.partition(_.moves.headOption.contains(playedUci))
      val focusedBranch =
        for
          branch <- playedBranch.headOption
          best <- top
        yield
          val focusedScore =
            fen.split("\\s+").lift(1).map(_.trim) match
              case Some("b") => best.scoreCp - 100
              case _         => best.scoreCp + 100
          branch.copy(scoreCp = focusedScore, mate = best.mate)
      focusedBranch.toList ++ otherBranches

  private[commentary] def admissionBlockers(
      source: SourceWitnessCatalog.SourceCandidate,
      admitted: Boolean,
      engineGate: String,
      ownerDiagnosis: String,
      surfaceGate: String,
      ownerFailureCodes: List[String] = Nil
  ): String =
    if admitted then "none"
    else
      val surfaceBlockers =
        List(
          Option.when(ownerDiagnosis == Diagnosis.SurfaceContractBlocked || surfaceGate == "supported_local_surface_failed")(
            "surface:supported_local_contract_failed"
          )
        ).flatten
      val blockers =
        (
          List(engineBlocker(engineGate)).flatten ++
          ownerBlockers(source, ownerDiagnosis, ownerFailureCodes) ++
          surfaceBlockers
        ).distinct
      if blockers.isEmpty then "none" else blockers.mkString(";")

  private def engineBlocker(engineGate: String): Option[String] =
    engineGate match
      case EngineGate.SourceMoveTopPv             => None
      case EngineGate.SourceMoveNearTopMultiPv    => None
      case EngineGate.SourceMoveMultiPvOnly       => Some("engine:source_move_multipv_only")
      case EngineGate.SourceMoveAbsentFromMultiPv => Some("engine:source_move_absent_from_multipv")
      case EngineGate.PvAvailableNoFirstMove      => Some("engine:pv_available_no_first_move")
      case EngineGate.EngineMissing               => Some("engine:missing")
      case other                                  => Some(s"engine:$other")

  private def ownerFailureCodes(
      source: SourceWitnessCatalog.SourceCandidate,
      surface: EvaluationSurface
  ): List[String] =
    val reviewGroup = source.reviewGroup.toLowerCase
    if reviewGroup.contains("break_prevention") then
      val packetContractMismatch =
        surface.mainProofSource.exists(source => source != "counterplay_axis_suppression") ||
          surface.packetSummary.exists(summary => summary.contains("proof_family=") && !summary.contains("proof_family=neutralize_key_break"))
      if packetContractMismatch then List("proof:break_prevention_contract_mismatch")
      else surface.breakPreventionFailureCodes.map(normalizeBreakPreventionFailureCode)
    else if reviewGroup.contains("central_break_timing") then
      val packetContractMismatch =
        surface.mainProofSource.exists(source => source != PlanTaxonomy.PlanKind.CentralBreakTiming.id) ||
          surface.packetSummary.exists(summary =>
            summary.contains("proof_family=") &&
              !summary.contains(s"proof_family=${PlanTaxonomy.PlanKind.CentralBreakTiming.id}")
          )
      if packetContractMismatch || surface.mainProofSource.isEmpty then List("central_break_timing_witness_missing")
      else Nil
    else if reviewGroup.contains("prophylaxis_restraint") then
      val packetContractMismatch =
        surface.mainProofSource.exists(source => source != "prophylactic_move") ||
          surface.packetSummary.exists(summary => summary.contains("proof_family=") && !summary.contains("proof_family=counterplay_restraint")) ||
          surface.contractId.exists(contract => contract != "-" && !contract.contains("counterplay_restraint"))
      if packetContractMismatch then List("proof:prophylaxis_restraint_contract_mismatch")
      else
        surface.contractFailures
          .filterNot(failure => failure == "-" || failure == "none")
          .map(failure => s"prophylaxis_restraint_${blockerCode(failure)}")
    else if reviewGroup.contains("iqp") && surface.contractId.isEmpty then
      surface.ownerFailureCodes
    else Nil

  private def ownerBlockers(
      source: SourceWitnessCatalog.SourceCandidate,
      ownerDiagnosis: String,
      ownerFailureCodes: List[String]
  ): List[String] =
    ownerDiagnosis match
      case Diagnosis.RootVocabularyOrExtractionGap =>
        val reviewGroup = source.reviewGroup.toLowerCase
        if reviewGroup.contains("iqp") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("iqp") then List("owner:iqp_not_induced_or_side_mismatch")
        else if reviewGroup.contains("break_prevention") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("break_prevention") then List("owner:break_prevention_witness_missing")
        else if reviewGroup.contains("central_break_timing") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("central_break_timing") then List("owner:central_break_timing_witness_missing")
        else if reviewGroup.contains("prophylaxis_restraint") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("prophylaxis_restraint") then List("owner:prophylaxis_restraint_witness_missing")
        else if reviewGroup.contains("defender_trade") then List("owner:defender_trade_owner_missing")
        else if reviewGroup.contains("carlsbad") then List("owner:carlsbad_probe_missing")
        else List("owner:root_vocabulary_or_extraction_gap")
      case Diagnosis.MoveOwnerMissing =>
        val reviewGroup = source.reviewGroup.toLowerCase
        if reviewGroup.contains("break_prevention") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("break_prevention") then List("owner:break_prevention_witness_missing")
        else if reviewGroup.contains("central_break_timing") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("central_break_timing") then List("owner:central_break_timing_witness_missing")
        else if reviewGroup.contains("prophylaxis_restraint") && ownerFailureCodes.nonEmpty then
          ownerFailureCodes.map(blockerFromFailureCode)
        else if reviewGroup.contains("prophylaxis_restraint") then List("owner:prophylaxis_restraint_witness_missing")
        else if reviewGroup.contains("defender_trade") then List("owner:defender_trade_owner_missing")
        else List("owner:move_owner_missing")
      case Diagnosis.PlannerOwnerSceneBlocked  => List("owner:planner_scene_blocked")
      case Diagnosis.PlannerOwnerSuppressed    => List("owner:planner_suppressed")
      case Diagnosis.OwnerMissingUnclassified  => List("owner:unclassified_missing")
      case Diagnosis.SurfaceContractBlocked    => Nil
      case Diagnosis.AdmitReady                => Nil
      case Diagnosis.EngineMissingBeforeAdmission => Nil
      case Diagnosis.EngineTopMoveDisagrees       => Nil
      case Diagnosis.EngineMultipvOnlySourceMove  => Nil
      case Diagnosis.TacticalFirstSource          => Nil
      case Diagnosis.ReplayFailed                 => Nil
      case _                                      => Nil

  private def blockerFromFailureCode(code: String): String =
    if code.startsWith("proof:") then code
    else if code.endsWith("_contract_mismatch") then s"proof:$code"
    else s"owner:${code.replace(":", "_")}"

  private def normalizeBreakPreventionFailureCode(code: String): String =
    code match
      case "contract_mismatch" | "break_prevention_contract_mismatch" =>
        "proof:break_prevention_contract_mismatch"
      case other => other

  private def releaseDecisionSummary(decision: ClaimAuthorityDecision): String =
    val base = decision.tier.toString
    if decision.vetoReasons.isEmpty then base
    else s"$base:${decision.vetoReasons.mkString("+")}"

  private def packetSummary(packet: PlayerFacingClaimPacket): String =
    List(
      s"proof_source=${packet.proofSource}",
      s"proof_family=${packet.proofFamily}",
      s"scope=${packet.scope}",
      s"fallback=${packet.fallbackMode}",
      s"same_branch=${packet.sameBranchState}",
      s"persistence=${packet.persistence}",
      s"seed=${packet.proofPathWitness.ownerSeedTerms.mkString("+")}",
      s"continuation=${packet.proofPathWitness.continuationTerms.mkString("+")}",
      s"suppression=${packet.suppressionReasons.mkString("+")}",
      s"risks=${packet.releaseRisks.mkString("+")}"
    ).mkString(";")

  private def chronicleNarrative(
      ctx: NarrativeContext,
      pack: lila.commentary.StrategyPack,
      inputs: QuestionPlannerInputs,
      ranked: RankedQuestionPlans,
      outline: NarrativeOutline
  ): String =
    ranked.primary
      .flatMap { primary =>
        GameChronicleCompressionPolicy.renderPlanSurface(
          ctx,
          GameChronicleCompressionPolicy.ChronicleRenderSurface(
            primary = primary,
            secondary = ranked.secondary,
            contrastTrace = lila.commentary.analysis.practical.ContrastiveSupportAdmissibility.decide(primary, inputs, None),
            quietSupportTrace = QuietStrategicSupportComposer.diagnose(ctx, inputs, ranked, Some(pack))
          ),
          beatEvidence = Nil
        ).map(_.narrative)
      }
      .orElse(
        GameChronicleCompressionPolicy
          .renderWithTrace(
            ctx = ctx,
            parts = emptyParts.copy(focusedOutline = outline),
            strategyPack = Some(pack),
            truthContract = None
          )
          .map(_.narrative)
      )
      .map(clean)
      .filter(_.nonEmpty)
      .getOrElse("-")

  private def taxonomy(source: SourceWitnessCatalog.SourceCandidate): String =
    val low = source.reviewGroup.toLowerCase
    if low.contains("tactical") then "source_tactical_first"
    else if low.contains("break_prevention") then "source_break_prevention"
    else if low.contains("central_break_timing") then "source_central_break_timing"
    else if low.contains("prophylaxis_restraint") then "source_prophylaxis_restraint"
    else if low.contains("simplification_window") then "source_simplification_window"
    else if low.contains("queen_trade") then "source_queen_trade_boundary"
    else if low.contains("static_weakness") then "source_static_weakness_fixation"
    else if low.contains("defender_trade") then "source_defender_trade"
    else if low.contains("carlsbad") then "source_carlsbad_fixed_target"
    else if low.contains("iqp_inducement") then "source_iqp_inducement"
    else if low.contains("iqp") then "source_iqp_simplification"
    else "source_boundary"

  private def blockerCode(raw: String): String =
    raw.toLowerCase
      .replace(":", "_")
      .replaceAll("[^a-z0-9_]+", "_")
      .replaceAll("(^_+|_+$)", "")

  private def phaseFromPly(ply: Int, totalPlies: Int): String =
    if ply <= 20 then "opening"
    else if totalPlies > 0 && totalPlies - ply <= 12 then "endgame"
    else "middlegame"

  private def defaultQuestions =
    List(
      AuthorQuestion("why_this", AuthorQuestionKind.WhyThis, 100, "Why this move?"),
      AuthorQuestion("what_matters_here", AuthorQuestionKind.WhatMattersHere, 90, "What matters here?"),
      AuthorQuestion("what_changed", AuthorQuestionKind.WhatChanged, 80, "What changed?"),
      AuthorQuestion("why_now", AuthorQuestionKind.WhyNow, 60, "Why now?")
    )

  private val emptyParts =
    CommentaryEngine.HybridNarrativeParts(
      lead = "Lead",
      defaultBridge = "Bridge",
      criticalBranch = None,
      body = "Body",
      primaryPlan = None,
      focusedOutline = NarrativeOutline(beats = Nil),
      phase = "Middlegame",
      tacticalPressure = false,
      cpWhite = Some(20),
      bead = 1
    )

  private[commentary] def markdown(observations: List[Observation]): String =
    val counts = observations.groupBy(_.verdict).view.mapValues(_.size).toList.sortBy(_._1)
    val diagnosisCounts = observations.groupBy(_.diagnosis).view.mapValues(_.size).toList.sortBy(_._1)
    val blockerCounts =
      observations
        .flatMap(_.admissionBlockers.split(";").toList.map(_.trim).filter(_.nonEmpty).filterNot(_ == "none"))
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toList
        .sortBy(_._1)
    val naturalSupported =
      observations.filter(obs => obs.verdict == Verdict.AdmitAuthorityRow && obs.release == "SupportedLocal")
    val surfaceBlocked =
      observations.filter(_.diagnosis == Diagnosis.SurfaceContractBlocked)
    val lines =
      List(
        "# Strategic Claim Authority Source Review",
        "",
        s"summary=${counts.map { case (k, v) => s"$k=$v" }.mkString(", ")}",
        s"Admission diagnostics: ${diagnosisCounts.map { case (k, v) => s"$k=$v" }.mkString(", ")}",
        s"Admission blockers: ${if blockerCounts.isEmpty then "none" else blockerCounts.map { case (k, v) => s"$k=$v" }.mkString(", ")}",
        s"Contract proof: ${contractCountText(observations)}",
        s"Admitted SupportedLocal source rows: ${if naturalSupported.isEmpty then "none found" else naturalSupported.map(_.source.id).mkString(", ")}",
        s"Surface contract blocked: ${if surfaceBlocked.isEmpty then "none found" else surfaceBlocked.map(_.source.id).mkString(", ")}",
        ""
      ) ++ observations.map { obs =>
        s"- ${obs.source.id}: verdict=${obs.verdict} diagnosis=${obs.diagnosis} blockers=${obs.admissionBlockers} ownerFailures=${obs.ownerFailureCodes} engine=${obs.engineAgreement} planner=${obs.plannerOwnership} surface=${obs.surfaceGate} source=${obs.mainProofSource} scope=${obs.mainClaimScope} contract=${obs.contractId} contractStatus=${obs.contractStatus} contractFailures=${obs.contractFailures} scene=${obs.sceneType} packet=${clean(obs.packetSummary)} ownerTrace=${clean(obs.ownerTraceSummary)} release=${obs.release} taxonomy=${obs.taxonomy} ply=${obs.ply.getOrElse("-")} reason=${clean(obs.reason)}"
      }
    lines.mkString("\n") + "\n"

  private[commentary] def windowMarkdown(observations: List[Observation]): String =
    val grouped =
      observations.groupBy(_.source.id).toList.sortBy(_._1)
    val lines =
      List(
        "# Strategic Claim Authority Source Window Review",
        "",
        s"rows=${observations.size}",
        "Acceptance rule: only verdict=admit_authority_row is source acceptance; release on rejected rows is diagnostic materialization.",
        ""
      ) ++ grouped.flatMap { case (sourceId, rows) =>
        val byPly = rows.sortBy(_.ply.getOrElse(Int.MaxValue))
        val representative =
          byPly
            .find(_.verdict == Verdict.AdmitAuthorityRow)
            .orElse(byPly.find(_.mainProofSource != "-"))
            .orElse(byPly.headOption)
        val summary =
          s"- $sourceId: scanned=${byPly.size} diagnostics=${countText(byPly.map(_.diagnosis))} blockers=${countText(byPly.flatMap(blockerTerms))}"
        val best =
          representative.toList.map(obs =>
            s"  best=ply=${obs.ply.getOrElse("-")} verdict=${obs.verdict} release=${obs.release} source=${obs.mainProofSource} scope=${obs.mainClaimScope} contract=${obs.contractId} contractStatus=${obs.contractStatus} failures=${obs.contractFailures} ownerFailures=${obs.ownerFailureCodes} engine=${obs.engineAgreement} primary=${clean(obs.primary)}"
          )
        summary :: best
      }
    lines.mkString("\n") + "\n"

  private[commentary] def writeArtifacts(observations: List[Observation]): (Path, Path) =
    val matrix = Paths.get("tmp", "strategic_claim_source_review.tsv")
    val review = Paths.get("tmp", "strategic_claim_source_review.md")
    Files.createDirectories(matrix.getParent)
    Files.write(matrix, (header :: observations.map(_.tsv)).mkString("\n").getBytes(StandardCharsets.UTF_8))
    Files.write(review, markdown(observations).getBytes(StandardCharsets.UTF_8))
    (matrix, review)

  private[commentary] def writeWindowArtifacts(observations: List[Observation]): (Path, Path) =
    val matrix = Paths.get("tmp", "strategic_claim_source_window_review.tsv")
    val review = Paths.get("tmp", "strategic_claim_source_window_review.md")
    Files.createDirectories(matrix.getParent)
    Files.write(matrix, (header :: observations.map(_.tsv)).mkString("\n").getBytes(StandardCharsets.UTF_8))
    Files.write(review, windowMarkdown(observations).getBytes(StandardCharsets.UTF_8))
    (matrix, review)

  private def enginePath(args: Seq[String]): Option[Path] =
    optionString(args.toList, "--engine")
      .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
      .orElse(sys.env.get("AI_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
      .map(Paths.get(_))

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value }.map(_.trim).filter(_.nonEmpty)

  private def optionSet(args: List[String], name: String): Set[String] =
    optionString(args, name)
      .map(_.split(",").toList.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)

  private def clean(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim

  private def countText(values: List[String]): String =
    val counts =
      values
        .map(_.trim)
        .filter(_.nonEmpty)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toList
        .sortBy(_._1)
    if counts.isEmpty then "none"
    else counts.map { case (key, count) => s"$key=$count" }.mkString(", ")

  private def blockerTerms(observation: Observation): List[String] =
    observation.admissionBlockers
      .split(";")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot(_ == "none")

  private def contractCountText(observations: List[Observation]): String =
    val values =
      observations.map(obs => s"${obs.contractStatus}:${obs.contractId}")
    countText(values)

  private def ownerTraceSummary(trace: PlannerOwnerTrace): String =
    List(
      s"scene=${trace.sceneType.wireName}",
      s"selected=${trace.selectedQuestion.map(_.toString).getOrElse("-")}:${trace.selectedPlannerOwnerKind.map(_.wireName).getOrElse("-")}:${trace.selectedPlannerSource.getOrElse("-")}",
      s"candidates=${trace.ownerCandidateLabels.mkString("|")}",
      s"dropped=${trace.droppedPlannerOwners.map(dropped => s"${dropped.plannerOwnerKind.wireName}:${dropped.source}:${dropped.reasons.mkString("+")}").mkString("|")}"
    ).mkString(";")

  @main def runSourceReview(args: String*): Unit =
    val depth = optionInt(args.toList, "--depth").getOrElse(16)
    val multiPv = optionInt(args.toList, "--multi-pv").orElse(optionInt(args.toList, "--multiPv")).getOrElse(3)
    val maybeEnginePath = enginePath(args)
    if maybeEnginePath.isEmpty then
      System.err.println("[strategic-source-review] missing engine path; rows will be replayed but not admitted")
    val observations =
      maybeEnginePath match
        case None => SourceReview.observations(None, depth = depth, multiPv = multiPv)
        case Some(path) =>
          val engine = RealPgnNarrativeEvalRunner.LocalUciEngine(path, timeoutMs = 30000L)
          try SourceReview.observations(Some(engine), depth = depth, multiPv = multiPv)
          finally engine.close()
    val (matrix, review) = writeArtifacts(observations)
    println((header :: observations.map(_.tsv)).mkString("\n"))
    println()
    println(s"wrote=${matrix.toAbsolutePath}")
    println(s"review=${review.toAbsolutePath}")
    println(markdown(observations).linesIterator.take(3).mkString("\n"))

  @main def runSourceWindowReview(args: String*): Unit =
    val depth = optionInt(args.toList, "--depth").getOrElse(16)
    val multiPv = optionInt(args.toList, "--multi-pv").orElse(optionInt(args.toList, "--multiPv")).getOrElse(3)
    val selectedIds = optionSet(args.toList, "--ids")
    val maybeEnginePath = enginePath(args)
    if maybeEnginePath.isEmpty then
      System.err.println("[strategic-source-window-review] missing engine path; rows will be replayed but not engine-classified")
    val observations =
      maybeEnginePath match
        case None =>
          SourceReview.windowObservations(None, depth = depth, multiPv = multiPv, sourceIds = selectedIds)
        case Some(path) =>
          val engine = RealPgnNarrativeEvalRunner.LocalUciEngine(path, timeoutMs = 30000L)
          try SourceReview.windowObservations(Some(engine), depth = depth, multiPv = multiPv, sourceIds = selectedIds)
          finally engine.close()
    val (matrix, review) = writeWindowArtifacts(observations)
    println((header :: observations.map(_.tsv)).mkString("\n"))
    println()
    println(s"wrote=${matrix.toAbsolutePath}")
    println(s"review=${review.toAbsolutePath}")
    println(windowMarkdown(observations))
