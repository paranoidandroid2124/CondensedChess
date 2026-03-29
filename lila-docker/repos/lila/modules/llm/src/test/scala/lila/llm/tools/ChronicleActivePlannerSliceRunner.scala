package lila.llm.tools

import java.nio.file.{ Files, Path, Paths }
import java.time.Instant

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import play.api.libs.json.{ Json, Writes }
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.*

object ChronicleActivePlannerSliceRunner:

  import CommentaryPlayerQcSupport.*

  given Writes[ActiveStrategicCoachingBriefBuilder.DeterministicSupportCandidate] =
    Json.writes[ActiveStrategicCoachingBriefBuilder.DeterministicSupportCandidate]
  given Writes[ActiveStrategicCoachingBriefBuilder.DeterministicComposeDebug] =
    Json.writes[ActiveStrategicCoachingBriefBuilder.DeterministicComposeDebug]

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      entriesPath: Path = DefaultReportDir.resolve("phase4_planner_surface_entries.jsonl"),
      jsonPath: Path = DefaultReportDir.resolve("phase4_planner_surface_summary.json"),
      markdownPath: Path = DefaultReportDir.resolve("phase4_planner_surface_summary.md"),
      anomalyPath: Path = DefaultReportDir.resolve("phase4_planner_surface_anomalies.jsonl"),
      depth: Int = 8,
      multiPv: Int = 2,
      perBucketGames: Int = 6,
      maxGames: Option[Int] = None,
      enginePath: Path
  )

  final case class SliceSurfaceEntry(
      sampleId: String,
      gameKey: String,
      mixBucket: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      momentPresent: Boolean,
      authorQuestionKinds: List[String],
      authorEvidenceKinds: List[String],
      chronicleMode: String,
      chroniclePrimaryKind: Option[String],
      chronicleSecondaryKind: Option[String],
      chronicleNarrative: Option[String],
      chronicleBlankLike: Boolean,
      activeMode: String,
      activePrimaryKind: Option[String],
      activeSecondaryKind: Option[String],
      activeFinalizationStage: Option[String],
      activeHardReasons: List[String],
      activeWarningReasons: List[String],
      activeNoteStatus: Option[String],
      activeNote: Option[String],
      activeBlankLike: Boolean,
      activeComposeDebug: Option[ActiveStrategicCoachingBriefBuilder.DeterministicComposeDebug] = None,
      plannerSceneType: Option[String] = None,
      plannerOwnerCandidates: List[String] = Nil,
      plannerAdmittedFamilies: List[String] = Nil,
      plannerDroppedFamilies: List[String] = Nil,
      plannerSupportMaterialSeparation: List[String] = Nil,
      plannerProposedFamilyMappings: List[String] = Nil,
      plannerDemotionReasons: List[String] = Nil,
      plannerSelectedQuestion: Option[String] = None,
      plannerSelectedOwnerFamily: Option[String] = None,
      plannerSelectedOwnerSource: Option[String] = None,
      chronicleSurfaceReplayOutcome: Option[String] = None,
      activeSurfaceReplayOutcome: Option[String] = None
  )
  object SliceSurfaceEntry:
    given Writes[SliceSurfaceEntry] = Json.writes[SliceSurfaceEntry]

  final case class MomentSurfaceState(
      gameKey: String,
      mixBucket: String,
      ply: Int,
      chronicleMode: String,
      chroniclePrimaryKind: Option[String],
      chronicleBlankLike: Boolean,
      activeMode: String,
      activePrimaryKind: Option[String],
      activeBlankLike: Boolean,
      activeFinalizationStage: Option[String],
      activeHardReasons: List[String],
      activeWarningReasons: List[String],
      authorQuestionKinds: List[String],
      authorEvidenceKinds: List[String],
      authorEvidenceStatuses: List[String],
      rejectedKinds: List[String],
      rejectedReasons: List[String],
      activeNoteStatus: Option[String],
      activeNote: Option[String],
      chronicleNarrative: Option[String],
      activeSecondaryKind: Option[String] = None,
      chronicleSecondaryKind: Option[String] = None,
      activeComposeDebug: Option[ActiveStrategicCoachingBriefBuilder.DeterministicComposeDebug] = None,
      plannerSceneType: Option[String] = None,
      plannerOwnerCandidates: List[String] = Nil,
      plannerAdmittedFamilies: List[String] = Nil,
      plannerDroppedFamilies: List[String] = Nil,
      plannerSupportMaterialSeparation: List[String] = Nil,
      plannerProposedFamilyMappings: List[String] = Nil,
      plannerDemotionReasons: List[String] = Nil,
      plannerSelectedQuestion: Option[String] = None,
      plannerSelectedOwnerFamily: Option[String] = None,
      plannerSelectedOwnerSource: Option[String] = None,
      chronicleSurfaceReplayOutcome: Option[String] = None,
      activeSurfaceReplayOutcome: Option[String] = None
  )

  final case class QuestionKindCoverage(
      targetSeededCount: Int,
      targetEvidenceCount: Int,
      targetCarriedCount: Int,
      targetChroniclePlannerOwnedCount: Int,
      targetChronicleFailClosedCount: Int,
      targetActiveApprovedCount: Int,
      targetActiveAttachedCount: Int,
      targetActiveFailClosedCount: Int,
      visibleSeededCount: Int,
      visibleEvidenceCount: Int,
      visibleCarriedCount: Int,
      visibleChroniclePlannerOwnedCount: Int,
      visibleChronicleFailClosedCount: Int,
      visibleActiveApprovedCount: Int,
      visibleActiveAttachedCount: Int,
      visibleActiveFailClosedCount: Int
  )
  object QuestionKindCoverage:
    given Writes[QuestionKindCoverage] = Json.writes[QuestionKindCoverage]

  final case class TargetSliceCoverage(
      targetCount: Int,
      carriedCount: Int,
      chroniclePlannerOwnedCount: Int,
      activeApprovedCount: Int,
      activeAttachedCount: Int
  )
  object TargetSliceCoverage:
    given Writes[TargetSliceCoverage] = Json.writes[TargetSliceCoverage]

  final case class ChronicleSummary(
      totalTargets: Int,
      momentPresentCount: Int,
      plannerOwnedCount: Int,
      factualFallbackCount: Int,
      omittedCount: Int,
      failClosedRatio: Double,
      blankLikeCount: Int,
      surfacedKinds: Map[String, Int],
      visibleMomentCount: Int,
      visiblePlannerOwnedCount: Int,
      visibleBlankLikeCount: Int,
      visibleKinds: Map[String, Int]
  )
  object ChronicleSummary:
    given Writes[ChronicleSummary] = Json.writes[ChronicleSummary]

  final case class ActiveSummary(
      totalTargets: Int,
      momentPresentCount: Int,
      plannerApprovedCount: Int,
      attachedCount: Int,
      omittedNoPrimaryCount: Int,
      omittedAfterPrimaryCount: Int,
      omittedNoMomentCount: Int,
      failClosedRatio: Double,
      blankLikeCount: Int,
      approvedKinds: Map[String, Int],
      attachedKinds: Map[String, Int],
      approvedButNotAttachedStages: Map[String, Int],
      approvedButNotAttachedHardReasons: Map[String, Int],
      approvedButNotAttachedWarningReasons: Map[String, Int],
      visibleMomentCount: Int,
      visiblePlannerApprovedCount: Int,
      visibleAttachedCount: Int,
      visibleBlankLikeCount: Int,
      visibleApprovedKinds: Map[String, Int],
      visibleAttachedKinds: Map[String, Int],
      visibleApprovedButNotAttachedStages: Map[String, Int],
      visibleApprovedButNotAttachedHardReasons: Map[String, Int],
      visibleApprovedButNotAttachedWarningReasons: Map[String, Int]
  )
  object ActiveSummary:
    given Writes[ActiveSummary] = Json.writes[ActiveSummary]

  final case class Summary(
      generatedAt: String,
      manifestPath: String,
      depth: Int,
      multiPv: Int,
      selectedGames: Int,
      totalTargets: Int,
      chronicle: ChronicleSummary,
      active: ActiveSummary,
      targetSliceCoverage: Map[String, TargetSliceCoverage],
      questionCoverage: Map[String, QuestionKindCoverage],
      visibleRejectedReasons: Map[String, Int]
  )
  object Summary:
    given Writes[Summary] = Json.writes[Summary]

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("chronicle-active-planner-slice-runner")

    val config = parseConfig(args.toList)
    val manifest =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value.filter(_.surface == ReviewSurface.Chronicle)
        case Left(err) =>
          System.err.println(s"[planner-slice] failed to read `${config.manifestPath}`: $err")
          sys.exit(1)

    if manifest.isEmpty then
      System.err.println(s"[planner-slice] no chronicle entries in `${config.manifestPath}`")
      sys.exit(1)

    val selectedEntries = selectEntries(manifest, config)
    if selectedEntries.isEmpty then
      System.err.println(s"[planner-slice] selection produced no entries from `${config.manifestPath}`")
      sys.exit(1)

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val api =
      LlmApi(
        openingExplorer = OpeningExplorerClient(ws),
        geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
        openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
        commentaryCache = CommentaryCache(),
        llmConfig = LlmConfig.fromEnv,
        providerConfig = LlmProviderConfig.fromEnv.copy(provider = "none")
      )
    val engine = new RealPgnNarrativeEvalRunner.LocalUciEngine(config.enginePath, timeoutMs = 30000L)

    try
      val (entries, visibleMoments) =
        selectedEntries
          .groupBy(_.gameKey)
          .toList
          .sortBy(_._1)
          .foldLeft((List.empty[SliceSurfaceEntry], List.empty[MomentSurfaceState])) {
            case ((entryAcc, visibleAcc), (gameKey, gameEntries)) =>
              val (gameEntriesOut, visibleOut) =
                analyzeGame(gameKey, gameEntries.sortBy(_.targetPly), api, engine, config)
              (entryAcc ::: gameEntriesOut, visibleAcc ::: visibleOut)
          }
      val summary = buildSummary(entries, visibleMoments, config)
      writeJsonLines(config.entriesPath, entries)
      writeJsonLines(config.anomalyPath, anomalyRows(entries, visibleMoments))
      writeJson(config.jsonPath, Json.toJson(summary))
      writeText(config.markdownPath, renderMarkdown(summary))
      println(
        s"[planner-slice] wrote `${config.entriesPath}`, `${config.anomalyPath}`, `${config.jsonPath}`, `${config.markdownPath}` " +
          s"(games=${summary.selectedGames}, targets=${summary.totalTargets})"
      )
    finally
      engine.close()
      ws.close()
      summon[ActorSystem].terminate()

  private def analyzeGame(
      gameKey: String,
      entries: List[SliceManifestEntry],
      api: LlmApi,
      engine: RealPgnNarrativeEvalRunner.LocalUciEngine,
      config: Config
  ): (List[SliceSurfaceEntry], List[MomentSurfaceState]) =
    val pgnPath = Paths.get(entries.head.pgnPath)
    val pgn = Files.readString(pgnPath)
    val artifacts =
      RealPgnNarrativeEvalRunner.analyzeChronicleGame(
        pgn = pgn,
        api = api,
        engine = engine,
        depth = config.depth,
        multiPv = config.multiPv,
        gameId = Some(gameKey)
      )
    val visibleResponse = artifacts.response
    val internalResponse = artifacts.internalResponse
    val visibleMomentsByPly = visibleResponse.moments.map(moment => moment.ply -> moment).toMap
    val internalMomentsByPly = internalResponse.moments.map(moment => moment.ply -> moment).toMap
    val threadsById = internalResponse.strategicThreads.map(thread => thread.threadId -> thread).toMap
    val visibleMoments =
      internalResponse.moments.map { internalMoment =>
        analyzeMoment(
          gameKey = gameKey,
          mixBucket = entries.head.mixBucket.getOrElse(MixBucket.Club),
          internalMoment = internalMoment,
          visibleMoment = visibleMomentsByPly.get(internalMoment.ply),
          threadsById = threadsById,
          api = api
        )
      }

    val targetEntries = entries.map { entry =>
      val internalMomentOpt = internalMomentsByPly.get(entry.targetPly)
      val visibleMomentOpt = visibleMomentsByPly.get(entry.targetPly)
      val analyzed =
        internalMomentOpt.map { internalMoment =>
          analyzeMoment(
            gameKey = gameKey,
            mixBucket = entry.mixBucket.getOrElse(MixBucket.Club),
            internalMoment = internalMoment,
            visibleMoment = visibleMomentOpt,
            threadsById = threadsById,
            api = api
          )
        }

      SliceSurfaceEntry(
        sampleId = entry.sampleId,
        gameKey = entry.gameKey,
        mixBucket = entry.mixBucket.getOrElse(MixBucket.Club),
        sliceKind = entry.sliceKind,
        targetPly = entry.targetPly,
        playedSan = entry.playedSan,
        momentPresent = visibleMomentOpt.nonEmpty,
        authorQuestionKinds = analyzed.map(_.authorQuestionKinds).getOrElse(Nil),
        authorEvidenceKinds = analyzed.map(_.authorEvidenceKinds).getOrElse(Nil),
        chronicleMode = analyzed.map(_.chronicleMode).getOrElse("omitted"),
        chroniclePrimaryKind = analyzed.flatMap(_.chroniclePrimaryKind),
        chronicleSecondaryKind = analyzed.flatMap(_.chronicleSecondaryKind),
        chronicleNarrative = visibleMomentOpt.flatMap(moment => Option(moment.narrative).map(_.trim).filter(_.nonEmpty)),
        chronicleBlankLike = analyzed.exists(_.chronicleBlankLike),
        activeMode = analyzed.map(_.activeMode).getOrElse("omitted_no_moment"),
        activePrimaryKind = analyzed.flatMap(_.activePrimaryKind),
        activeSecondaryKind = analyzed.flatMap(_.activeSecondaryKind),
        activeFinalizationStage = analyzed.flatMap(_.activeFinalizationStage),
        activeHardReasons = analyzed.map(_.activeHardReasons).getOrElse(Nil),
        activeWarningReasons = analyzed.map(_.activeWarningReasons).getOrElse(Nil),
        activeNoteStatus = visibleMomentOpt.flatMap(_.activeStrategicSourceMode),
        activeNote = visibleMomentOpt.flatMap(_.activeStrategicNote).map(_.trim).filter(_.nonEmpty),
        activeBlankLike = analyzed.exists(_.activeBlankLike),
        activeComposeDebug = analyzed.flatMap(_.activeComposeDebug),
        plannerSceneType = analyzed.flatMap(_.plannerSceneType),
        plannerOwnerCandidates = analyzed.map(_.plannerOwnerCandidates).getOrElse(Nil),
        plannerAdmittedFamilies = analyzed.map(_.plannerAdmittedFamilies).getOrElse(Nil),
        plannerDroppedFamilies = analyzed.map(_.plannerDroppedFamilies).getOrElse(Nil),
        plannerSupportMaterialSeparation = analyzed.map(_.plannerSupportMaterialSeparation).getOrElse(Nil),
        plannerProposedFamilyMappings = analyzed.map(_.plannerProposedFamilyMappings).getOrElse(Nil),
        plannerDemotionReasons = analyzed.map(_.plannerDemotionReasons).getOrElse(Nil),
        plannerSelectedQuestion = analyzed.flatMap(_.plannerSelectedQuestion),
        plannerSelectedOwnerFamily = analyzed.flatMap(_.plannerSelectedOwnerFamily),
        plannerSelectedOwnerSource = analyzed.flatMap(_.plannerSelectedOwnerSource),
        chronicleSurfaceReplayOutcome = analyzed.flatMap(_.chronicleSurfaceReplayOutcome),
        activeSurfaceReplayOutcome = analyzed.flatMap(_.activeSurfaceReplayOutcome)
      )
    }
    (targetEntries, visibleMoments)

  private[tools] def analyzeMoment(
      gameKey: String,
      mixBucket: String,
      internalMoment: GameChronicleMoment,
      visibleMoment: Option[GameChronicleMoment],
      threadsById: Map[String, ActiveStrategicThread],
      api: LlmApi
  ): MomentSurfaceState =
    val routeRefs = rebuildRouteRefs(internalMoment)
    val moveRefs = rebuildMoveRefs(internalMoment)
    val thread = internalMoment.strategicThread.flatMap(ref => threadsById.get(ref.threadId))
    val dossier =
      ActiveBranchDossierBuilder.build(internalMoment, routeRefs, moveRefs, internalMoment.strategicThread, thread)
    val deltaBundle = PlayerFacingMoveDeltaBuilder.build(internalMoment, routeRefs, moveRefs)
    val decisionFrame = CertifiedDecisionFrameBuilder.build(internalMoment, deltaBundle, dossier)
    val replay =
      ActiveStrategicCoachingBriefBuilder.replayPlanner(internalMoment, deltaBundle, dossier, decisionFrame)
    val chronicleSelection =
      replay.flatMap(replay => GameChronicleCompressionPolicy.selectPlannerSurface(replay.rankedPlans, replay.inputs))
    val activeSelection =
      replay.flatMap(ActiveStrategicCoachingBriefBuilder.selectPlannerSurface)
    val activeTrace =
      api.traceActiveFinalization(
        moment = internalMoment,
        deltaBundle = deltaBundle,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = moveRefs,
        plannerSelection = activeSelection,
        strategicBranchSelected = internalMoment.strategicBranch
      )
    val rejectedKinds =
      replay.toList.flatMap(_.rankedPlans.rejected.map(_.questionKind.toString)).distinct
    val rejectedReasons =
      replay.toList.flatMap(_.rankedPlans.rejected.flatMap(_.reasons)).groupBy(identity).toList.sortBy(_._1).map(_._1)
    val plannerTrace = replay.map(_.rankedPlans.ownerTrace)
    val chronicleNarrative =
      visibleMoment.flatMap(moment => Option(moment.narrative).map(_.trim).filter(_.nonEmpty))
    val activeNote =
      visibleMoment.flatMap(_.activeStrategicNote).map(_.trim).filter(_.nonEmpty)
    val chronicleReplayOutcome = chronicleMode(visibleMoment, chronicleSelection, chronicleNarrative)
    val activeReplayOutcome = activeMode(visibleMoment, activeSelection, activeNote)

    MomentSurfaceState(
      gameKey = gameKey,
      mixBucket = mixBucket,
      ply = internalMoment.ply,
      chronicleMode = chronicleReplayOutcome,
      chroniclePrimaryKind = chronicleSelection.map(_.primary.questionKind.toString),
      chronicleSecondaryKind = chronicleSelection.flatMap(_.secondary.map(_.questionKind.toString)),
      chronicleBlankLike = chronicleNarrative.exists(blankLike),
      activeMode = activeReplayOutcome,
      activePrimaryKind = activeSelection.map(_.primary.questionKind.toString),
      activeSecondaryKind = activeSelection.flatMap(_.secondary.map(_.questionKind.toString)),
      activeBlankLike = activeNote.exists(blankLike),
      activeFinalizationStage = Some(activeTrace.stage),
      activeHardReasons = activeTrace.hardReasons,
      activeWarningReasons = activeTrace.warningReasons,
      authorQuestionKinds = internalMoment.authorQuestions.map(_.kind),
      authorEvidenceKinds = internalMoment.authorEvidence.map(_.questionKind),
      authorEvidenceStatuses = internalMoment.authorEvidence.map(_.status),
      rejectedKinds = rejectedKinds,
      rejectedReasons = rejectedReasons,
      activeNoteStatus = visibleMoment.flatMap(_.activeStrategicSourceMode),
      activeNote = activeNote,
      chronicleNarrative = chronicleNarrative,
      activeComposeDebug = activeTrace.composeDebug,
      plannerSceneType = plannerTrace.map(_.sceneType.wireName),
      plannerOwnerCandidates = plannerTrace.map(_.ownerCandidateLabels).getOrElse(Nil),
      plannerAdmittedFamilies = plannerTrace.map(_.admittedFamilyLabels).getOrElse(Nil),
      plannerDroppedFamilies = plannerTrace.map(_.droppedFamilyLabels).getOrElse(Nil),
      plannerSupportMaterialSeparation = plannerTrace.map(_.supportMaterialSeparationLabels).getOrElse(Nil),
      plannerProposedFamilyMappings = plannerTrace.map(_.proposedFamilyMappingLabels).getOrElse(Nil),
      plannerDemotionReasons = plannerTrace.map(_.demotionReasons).getOrElse(Nil),
      plannerSelectedQuestion = plannerTrace.flatMap(_.selectedQuestion.map(_.toString)),
      plannerSelectedOwnerFamily = plannerTrace.flatMap(_.selectedOwnerFamily.map(_.wireName)),
      plannerSelectedOwnerSource = plannerTrace.flatMap(_.selectedOwnerSource),
      chronicleSurfaceReplayOutcome = Some(chronicleReplayOutcome),
      activeSurfaceReplayOutcome = Some(activeReplayOutcome)
    )

  private def chronicleMode(
      momentOpt: Option[GameChronicleMoment],
      selection: Option[GameChronicleCompressionPolicy.ChroniclePlanSurface],
      narrative: Option[String]
  ): String =
    if momentOpt.isEmpty || narrative.isEmpty then "omitted"
    else if selection.nonEmpty then "planner_owned"
    else "factual_fallback"

  private def activeMode(
      momentOpt: Option[GameChronicleMoment],
      selection: Option[ActiveStrategicCoachingBriefBuilder.PlannerSurfaceSelection],
      note: Option[String]
  ): String =
    if momentOpt.isEmpty then "omitted_no_moment"
    else if note.nonEmpty then "attached"
    else if selection.nonEmpty then "omitted_after_primary"
    else "omitted_no_primary"

  private def rebuildRouteRefs(
      moment: GameChronicleMoment
  ): List[ActiveStrategicRouteRef] =
    val routeRegex = "^[a-h][1-8]$".r
    moment.strategyPack.toList
      .flatMap(_.pieceRoutes)
      .zipWithIndex
      .flatMap { case (route, idx) =>
        val squares =
          route.route
            .map(_.trim.toLowerCase)
            .filter(s => routeRegex.matches(s))
            .distinct
        Option.when(squares.size >= 2 && route.surfaceMode != RouteSurfaceMode.Hidden)(
          ActiveStrategicRouteRef(
            routeId = s"route_${idx + 1}",
            ownerSide = route.ownerSide,
            piece = route.piece,
            route = squares,
            purpose = route.purpose,
            strategicFit = route.strategicFit,
            tacticalSafety = route.tacticalSafety,
            surfaceConfidence = route.surfaceConfidence,
            surfaceMode = route.surfaceMode
          )
        )
      }
      .take(3)

  private def rebuildMoveRefs(
      moment: GameChronicleMoment
  ): List[ActiveStrategicMoveRef] =
    val fromEngine =
      moment.topEngineMove.toList
        .map(_.uci.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
        .take(1)
        .map { uci =>
          ActiveStrategicMoveRef(
            label = "Engine preference",
            source = "top_engine_move",
            uci = uci,
            san = NarrativeUtils.uciToSan(moment.fen, uci),
            fenAfter = Some(NarrativeUtils.uciListToFen(moment.fen, List(uci)))
          )
        }
    val fromVariation =
      moment.variations.headOption
        .flatMap(_.moves.headOption)
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
        .toList
        .map { uci =>
          ActiveStrategicMoveRef(
            label = "Principal line",
            source = "top_variation",
            uci = uci,
            san = NarrativeUtils.uciToSan(moment.fen, uci),
            fenAfter = Some(NarrativeUtils.uciListToFen(moment.fen, List(uci)))
          )
        }
    (fromEngine ++ fromVariation).groupBy(_.uci).values.map(_.head).toList.take(2)

  private def blankLike(text: String): Boolean =
    val cleaned = Option(text).getOrElse("").replaceAll("""\s+""", " ").trim
    val words = cleaned.split("\\s+").count(_.nonEmpty)
    words <= 11 &&
      !LineScopedCitation.hasInlineCitation(cleaned) &&
      !LiveNarrativeCompressionCore.hasConcreteAnchor(cleaned)

  private def selectEntries(
      manifest: List[SliceManifestEntry],
      config: Config
  ): List[SliceManifestEntry] =
    val gamesByBucket =
      manifest
        .groupBy(_.gameKey)
        .values
        .map { entries =>
          val sorted = entries.sortBy(_.targetPly)
          (sorted.head.mixBucket.getOrElse(MixBucket.Club), sorted.head.gameKey, sorted)
        }
        .toList
        .groupBy(_._1)
        .view
        .mapValues(_.sortBy(_._2))
        .toMap

    val selectedGames =
      MixBucket.all.flatMap { bucket =>
        gamesByBucket.getOrElse(bucket, Nil).take(config.perBucketGames)
      }
    val limited =
      config.maxGames match
        case Some(limit) => selectedGames.take(limit.max(1))
        case None        => selectedGames
    limited.flatMap(_._3)

  def buildSummary(
      entries: List[SliceSurfaceEntry],
      visibleMoments: List[MomentSurfaceState],
      config: Config
  ): Summary =
    val QuestionKinds =
      List("WhyThis", "WhyNow", "WhatChanged", "WhatMustBeStopped", "WhosePlanIsFaster")

    def countKinds[A](rows: List[A], kindFn: A => Option[String]): Map[String, Int] =
      rows.flatMap(kindFn).groupBy(identity).view.mapValues(_.size).toMap

    def countValues(values: List[String]): Map[String, Int] =
      values.groupBy(identity).view.mapValues(_.size).toMap

    def targetHasCarriedQuestion(entry: SliceSurfaceEntry, kind: String): Boolean =
      entry.authorQuestionKinds.contains(kind) || entry.authorEvidenceKinds.contains(kind)

    def visibleHasCarriedQuestion(moment: MomentSurfaceState, kind: String): Boolean =
      moment.authorQuestionKinds.contains(kind) || moment.authorEvidenceKinds.contains(kind)

    val chroniclePlannerOwned = entries.filter(_.chronicleMode == "planner_owned")
    val chronicleFactual = entries.count(_.chronicleMode == "factual_fallback")
    val chronicleOmitted = entries.count(_.chronicleMode == "omitted")
    val visibleChroniclePlannerOwned = visibleMoments.filter(_.chronicleMode == "planner_owned")
    val activeApproved = entries.filter(_.activePrimaryKind.nonEmpty)
    val activeAttached = entries.filter(_.activeMode == "attached")
    val activeApprovedButNotAttached = activeApproved.filterNot(_.activeMode == "attached")
    val visibleActiveApproved = visibleMoments.filter(_.activePrimaryKind.nonEmpty)
    val visibleActiveAttached = visibleMoments.filter(_.activeMode == "attached")
    val visibleActiveApprovedButNotAttached = visibleActiveApproved.filterNot(_.activeMode == "attached")
    val targetSliceCoverage =
      List(SliceKind.QuestionWhyNow).map { sliceKind =>
        val sliceEntries = entries.filter(_.sliceKind == sliceKind)
        sliceKind ->
          TargetSliceCoverage(
            targetCount = sliceEntries.size,
            carriedCount =
              sliceEntries.count(entry => entry.authorQuestionKinds.contains("WhyNow") || entry.authorEvidenceKinds.contains("WhyNow")),
            chroniclePlannerOwnedCount = sliceEntries.count(_.chroniclePrimaryKind.contains("WhyNow")),
            activeApprovedCount = sliceEntries.count(_.activePrimaryKind.contains("WhyNow")),
            activeAttachedCount = sliceEntries.count(entry => entry.activeMode == "attached" && entry.activePrimaryKind.contains("WhyNow"))
          )
      }.toMap
    val questionCoverage =
      QuestionKinds.map { kind =>
        val targetSeeded = entries.count(_.authorQuestionKinds.contains(kind))
        val targetEvidence = entries.count(_.authorEvidenceKinds.contains(kind))
        val targetCarried = entries.count(entry => targetHasCarriedQuestion(entry, kind))
        val visibleSeeded = visibleMoments.count(_.authorQuestionKinds.contains(kind))
        val visibleEvidence = visibleMoments.count(_.authorEvidenceKinds.contains(kind))
        val visibleCarried = visibleMoments.count(moment => visibleHasCarriedQuestion(moment, kind))
        kind ->
          QuestionKindCoverage(
            targetSeededCount = targetSeeded,
            targetEvidenceCount = targetEvidence,
            targetCarriedCount = targetCarried,
            targetChroniclePlannerOwnedCount = entries.count(_.chroniclePrimaryKind.contains(kind)),
            targetChronicleFailClosedCount =
              entries.count(entry => targetHasCarriedQuestion(entry, kind) && !entry.chroniclePrimaryKind.contains(kind)),
            targetActiveApprovedCount = entries.count(_.activePrimaryKind.contains(kind)),
            targetActiveAttachedCount =
              entries.count(entry => entry.activeMode == "attached" && entry.activePrimaryKind.contains(kind)),
            targetActiveFailClosedCount =
              entries.count(entry => targetHasCarriedQuestion(entry, kind) && !entry.activePrimaryKind.contains(kind)),
            visibleSeededCount = visibleSeeded,
            visibleEvidenceCount = visibleEvidence,
            visibleCarriedCount = visibleCarried,
            visibleChroniclePlannerOwnedCount = visibleMoments.count(_.chroniclePrimaryKind.contains(kind)),
            visibleChronicleFailClosedCount =
              visibleMoments.count(moment => visibleHasCarriedQuestion(moment, kind) && !moment.chroniclePrimaryKind.contains(kind)),
            visibleActiveApprovedCount = visibleMoments.count(_.activePrimaryKind.contains(kind)),
            visibleActiveAttachedCount =
              visibleMoments.count(moment => moment.activeMode == "attached" && moment.activePrimaryKind.contains(kind)),
            visibleActiveFailClosedCount =
              visibleMoments.count(moment => visibleHasCarriedQuestion(moment, kind) && !moment.activePrimaryKind.contains(kind))
          )
      }.toMap
    val visibleRejectedReasons =
      visibleMoments.flatMap(_.rejectedReasons).groupBy(identity).view.mapValues(_.size).toMap

    Summary(
      generatedAt = Instant.now().toString,
      manifestPath = config.manifestPath.toAbsolutePath.normalize.toString,
      depth = config.depth,
      multiPv = config.multiPv,
      selectedGames = entries.map(_.gameKey).distinct.size,
      totalTargets = entries.size,
      chronicle =
        ChronicleSummary(
          totalTargets = entries.size,
          momentPresentCount = entries.count(_.momentPresent),
          plannerOwnedCount = chroniclePlannerOwned.size,
          factualFallbackCount = chronicleFactual,
          omittedCount = chronicleOmitted,
          failClosedRatio =
            if entries.isEmpty then 0.0 else (chronicleFactual + chronicleOmitted).toDouble / entries.size.toDouble,
          blankLikeCount = chroniclePlannerOwned.count(_.chronicleBlankLike),
          surfacedKinds = countKinds(chroniclePlannerOwned, _.chroniclePrimaryKind),
          visibleMomentCount = visibleMoments.size,
          visiblePlannerOwnedCount = visibleChroniclePlannerOwned.size,
          visibleBlankLikeCount = visibleChroniclePlannerOwned.count(_.chronicleBlankLike),
          visibleKinds = countKinds(visibleChroniclePlannerOwned, _.chroniclePrimaryKind)
        ),
      active =
        ActiveSummary(
          totalTargets = entries.size,
          momentPresentCount = entries.count(_.momentPresent),
          plannerApprovedCount = activeApproved.size,
          attachedCount = activeAttached.size,
          omittedNoPrimaryCount = entries.count(_.activeMode == "omitted_no_primary"),
          omittedAfterPrimaryCount = entries.count(_.activeMode == "omitted_after_primary"),
          omittedNoMomentCount = entries.count(_.activeMode == "omitted_no_moment"),
          failClosedRatio =
            if entries.isEmpty then 0.0 else entries.count(_.activeMode != "attached").toDouble / entries.size.toDouble,
          blankLikeCount = activeAttached.count(_.activeBlankLike),
          approvedKinds = countKinds(activeApproved, _.activePrimaryKind),
          attachedKinds = countKinds(activeAttached, _.activePrimaryKind),
          approvedButNotAttachedStages = countValues(activeApprovedButNotAttached.flatMap(_.activeFinalizationStage)),
          approvedButNotAttachedHardReasons = countValues(activeApprovedButNotAttached.flatMap(_.activeHardReasons)),
          approvedButNotAttachedWarningReasons = countValues(activeApprovedButNotAttached.flatMap(_.activeWarningReasons)),
          visibleMomentCount = visibleMoments.size,
          visiblePlannerApprovedCount = visibleActiveApproved.size,
          visibleAttachedCount = visibleActiveAttached.size,
          visibleBlankLikeCount = visibleActiveAttached.count(_.activeBlankLike),
          visibleApprovedKinds = countKinds(visibleActiveApproved, _.activePrimaryKind),
          visibleAttachedKinds = countKinds(visibleActiveAttached, _.activePrimaryKind),
          visibleApprovedButNotAttachedStages = countValues(visibleActiveApprovedButNotAttached.flatMap(_.activeFinalizationStage)),
          visibleApprovedButNotAttachedHardReasons = countValues(visibleActiveApprovedButNotAttached.flatMap(_.activeHardReasons)),
          visibleApprovedButNotAttachedWarningReasons =
            countValues(visibleActiveApprovedButNotAttached.flatMap(_.activeWarningReasons))
        ),
      targetSliceCoverage = targetSliceCoverage,
      questionCoverage = questionCoverage,
      visibleRejectedReasons = visibleRejectedReasons
    )

  private def renderMarkdown(summary: Summary): String =
    def renderKindMap(kindMap: Map[String, Int], total: Int): String =
      if kindMap.isEmpty then "- none\n"
      else
        kindMap.toList
          .sortBy { case (kind, count) => (-count, kind) }
          .map { case (kind, count) =>
            val ratio = if total == 0 then 0.0 else count.toDouble / total.toDouble
            f"- $kind: `$count` (${ratio * 100}%.1f%%)\n"
          }
          .mkString

    def renderCountMap(kindMap: Map[String, Int]): String =
      if kindMap.isEmpty then "- none\n"
      else
        kindMap.toList
          .sortBy { case (kind, count) => (-count, kind) }
          .map { case (kind, count) => s"- $kind: `$count`\n" }
          .mkString

    def renderCoverage(kind: String, coverage: QuestionKindCoverage): String =
      def ratio(num: Int, den: Int): String =
        if den == 0 then "n/a"
        else f"${num.toDouble / den.toDouble * 100}%.1f%%"

      s"""- $kind:
         |  target carried `${coverage.targetCarriedCount}` (seeded `${coverage.targetSeededCount}`, evidence `${coverage.targetEvidenceCount}`), Chronicle `${coverage.targetChroniclePlannerOwnedCount}` surfaced / `${coverage.targetChronicleFailClosedCount}` fail-closed, Active `${coverage.targetActiveAttachedCount}` attached / `${coverage.targetActiveFailClosedCount}` fail-closed
         |  visible carried `${coverage.visibleCarriedCount}` (seeded `${coverage.visibleSeededCount}`, evidence `${coverage.visibleEvidenceCount}`), Chronicle `${coverage.visibleChroniclePlannerOwnedCount}` surfaced (${ratio(coverage.visibleChroniclePlannerOwnedCount, coverage.visibleCarriedCount)}), Active `${coverage.visibleActiveAttachedCount}` attached (${ratio(coverage.visibleActiveAttachedCount, coverage.visibleCarriedCount)}) / `${coverage.visibleActiveFailClosedCount}` fail-closed
         |""".stripMargin

    val chroniclePlannerOwnedTotal = summary.chronicle.plannerOwnedCount
    val visibleChroniclePlannerOwnedTotal = summary.chronicle.visiblePlannerOwnedCount
    val activeApprovedTotal = summary.active.plannerApprovedCount
    val activeAttachedTotal = summary.active.attachedCount
    val visibleActiveApprovedTotal = summary.active.visiblePlannerApprovedCount
    val visibleActiveAttachedTotal = summary.active.visibleAttachedCount
    val coverageSection =
      summary.questionCoverage.toList
        .sortBy { case (kind, _) => kind }
        .map { case (kind, coverage) => renderCoverage(kind, coverage) }
        .mkString
    val targetSliceSection =
      summary.targetSliceCoverage.toList
        .sortBy(_._1)
        .map { case (sliceKind, coverage) =>
          s"""- $sliceKind:
             |  targets `${coverage.targetCount}`, carried `${coverage.carriedCount}`, Chronicle `${coverage.chroniclePlannerOwnedCount}` surfaced, Active `${coverage.activeApprovedCount}` approved / `${coverage.activeAttachedCount}` attached
             |""".stripMargin
        }
        .mkString
    val rejectedSection =
      renderKindMap(summary.visibleRejectedReasons, summary.visibleRejectedReasons.values.sum)
    s"""# Phase 4 Planner Surface Validation
       |
       |- Generated: `${summary.generatedAt}`
       |- Manifest: `${summary.manifestPath}`
       |- Games: `${summary.selectedGames}`
       |- Targets: `${summary.totalTargets}`
       |- Engine: depth `${summary.depth}`, multiPV `${summary.multiPv}`
       |
       |## Chronicle
       |
       |- Moment present: `${summary.chronicle.momentPresentCount}`
       |- Planner-owned: `${summary.chronicle.plannerOwnedCount}`
       |- Factual fallback: `${summary.chronicle.factualFallbackCount}`
       |- Omitted: `${summary.chronicle.omittedCount}`
       |- Fail-closed ratio: `${f"${summary.chronicle.failClosedRatio * 100}%.1f"}%`
       |- Blank-like planner prose: `${summary.chronicle.blankLikeCount}`
       |
       |### Chronicle target-slice surfaced kinds
       |
       |${renderKindMap(summary.chronicle.surfacedKinds, chroniclePlannerOwnedTotal)}
       |### Chronicle visible-moment density
       |
       |- Visible moments: `${summary.chronicle.visibleMomentCount}`
       |- Visible planner-owned: `${summary.chronicle.visiblePlannerOwnedCount}`
       |- Blank-like visible planner prose: `${summary.chronicle.visibleBlankLikeCount}`
       |
       |${renderKindMap(summary.chronicle.visibleKinds, visibleChroniclePlannerOwnedTotal)}
       |## Active
       |
       |- Moment present: `${summary.active.momentPresentCount}`
       |- Planner-approved primary: `${summary.active.plannerApprovedCount}`
       |- Attached notes: `${summary.active.attachedCount}`
       |- Omitted without primary: `${summary.active.omittedNoPrimaryCount}`
       |- Omitted after primary: `${summary.active.omittedAfterPrimaryCount}`
       |- Omitted without moment: `${summary.active.omittedNoMomentCount}`
       |- Fail-closed ratio: `${f"${summary.active.failClosedRatio * 100}%.1f"}%`
       |- Blank-like attached notes: `${summary.active.blankLikeCount}`
       |
       |### Active target-slice approved kinds
       |
       |${renderKindMap(summary.active.approvedKinds, activeApprovedTotal)}
       |### Active target-slice attached kinds
       |
       |${renderKindMap(summary.active.attachedKinds, activeAttachedTotal)}
       |### Active target-slice approved-but-not-attached stages
       |
       |${renderCountMap(summary.active.approvedButNotAttachedStages)}
       |#### Active target-slice approved-but-not-attached hard reasons
       |
       |${renderCountMap(summary.active.approvedButNotAttachedHardReasons)}
       |#### Active target-slice approved-but-not-attached warning reasons
       |
       |${renderCountMap(summary.active.approvedButNotAttachedWarningReasons)}
       |### Active visible-moment density
       |
       |- Visible moments: `${summary.active.visibleMomentCount}`
       |- Visible planner-approved: `${summary.active.visiblePlannerApprovedCount}`
       |- Visible attached: `${summary.active.visibleAttachedCount}`
       |- Blank-like visible notes: `${summary.active.visibleBlankLikeCount}`
       |
       |#### Active visible approved kinds
       |
       |${renderKindMap(summary.active.visibleApprovedKinds, visibleActiveApprovedTotal)}
       |#### Active visible attached kinds
       |
       |${renderKindMap(summary.active.visibleAttachedKinds, visibleActiveAttachedTotal)}
       |#### Active visible approved-but-not-attached stages
       |
       |${renderCountMap(summary.active.visibleApprovedButNotAttachedStages)}
       |##### Active visible approved-but-not-attached hard reasons
       |
       |${renderCountMap(summary.active.visibleApprovedButNotAttachedHardReasons)}
       |##### Active visible approved-but-not-attached warning reasons
       |
       |${renderCountMap(summary.active.visibleApprovedButNotAttachedWarningReasons)}
       |## Question-Target Coverage
       |
       |$targetSliceSection
       |## Question Coverage
       |
       |$coverageSection
       |## Visible Rejected Reasons
       |
       |$rejectedSection
       |""".stripMargin

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get("LLM_ACTIVE_CORPUS_ENGINE_PATH").map(_.trim).filter(_.nonEmpty))
        .orElse(sys.env.get("STOCKFISH_BIN").map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println("[planner-slice] missing engine path. Pass `--engine /path/to/uci-engine`.")
          sys.exit(1)
        }
    Config(
      manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
      entriesPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("phase4_planner_surface_entries.jsonl")),
      jsonPath = positional.lift(2).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("phase4_planner_surface_summary.json")),
      markdownPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("phase4_planner_surface_summary.md")),
      anomalyPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("phase4_planner_surface_anomalies.jsonl")),
      depth = optionInt(args, "--depth").getOrElse(8).max(6),
      multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(2).max(1),
      perBucketGames = optionInt(args, "--per-bucket").getOrElse(6).max(1),
      maxGames = optionInt(args, "--max-games").map(_.max(1)),
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue =
      Set("--engine", "--depth", "--multi-pv", "--multiPv", "--per-bucket", "--max-games")
    val out = scala.collection.mutable.ListBuffer.empty[String]
    var idx = 0
    while idx < args.length do
      val current = args(idx)
      if current.startsWith("--") then idx += (if optionsWithValue.contains(current) then 2 else 1)
      else
        out += current
        idx += 1
    out.toList

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst { case List(flag, value) if flag == name => value.trim }.filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)

  private def anomalyRows(
      entries: List[SliceSurfaceEntry],
      visibleMoments: List[MomentSurfaceState]
  ): List[play.api.libs.json.JsObject] =
    val targetKeys = entries.map(entry => s"${entry.gameKey}:${entry.targetPly}").toSet
    visibleMoments.flatMap { moment =>
      val hasChronicleAnomaly =
        moment.chronicleNarrative.nonEmpty && moment.chroniclePrimaryKind.isEmpty
      val hasActiveAnomaly =
        moment.activeNote.nonEmpty && moment.activePrimaryKind.isEmpty
      Option.when(hasChronicleAnomaly || hasActiveAnomaly) {
        Json.obj(
          "gameKey" -> moment.gameKey,
          "mixBucket" -> moment.mixBucket,
          "ply" -> moment.ply,
          "isTargetPly" -> targetKeys.contains(s"${moment.gameKey}:${moment.ply}"),
          "chronicleMode" -> moment.chronicleMode,
          "chronicleNarrative" -> moment.chronicleNarrative,
          "activeMode" -> moment.activeMode,
          "activeFinalizationStage" -> moment.activeFinalizationStage,
          "activeHardReasons" -> moment.activeHardReasons,
          "activeWarningReasons" -> moment.activeWarningReasons,
          "activeNoteStatus" -> moment.activeNoteStatus,
          "activeNote" -> moment.activeNote,
          "activeComposeDebug" -> moment.activeComposeDebug,
          "authorQuestionKinds" -> moment.authorQuestionKinds,
          "authorEvidenceKinds" -> moment.authorEvidenceKinds,
          "authorEvidenceStatuses" -> moment.authorEvidenceStatuses,
          "rejectedKinds" -> moment.rejectedKinds,
          "rejectedReasons" -> moment.rejectedReasons
        )
      }
    }
