package lila.accountintel

import chess.Color
import chess.format.pgn.PgnStr
import lila.llm.MoveEval
import lila.llm.model.CollapseAnalysis
import lila.llm.model.ExtendedAnalysisData
import play.api.libs.json.*

import java.time.Instant
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import scala.concurrent.duration.FiniteDuration

object AccountIntel:

  enum DispatchOutcome:
    case Accepted
    case Failed(message: String)

  enum RunJobOutcome:
    case Started
    case Missing
    case NotQueued

  enum ProductKind(val key: String):
    case MyAccountIntelligenceLite extends ProductKind("my_account_intelligence_lite")
    case OpponentPrep extends ProductKind("opponent_prep")

    def role = if this == MyAccountIntelligenceLite then "self" else "opponent"
    def lens = if this == MyAccountIntelligenceLite then "self_repair" else "opponent_pressure"

  object ProductKind:
    def fromKey(key: String): Option[ProductKind] =
      key.trim.toLowerCase match
        case ProductKind.MyAccountIntelligenceLite.key => Some(ProductKind.MyAccountIntelligenceLite)
        case ProductKind.OpponentPrep.key => Some(ProductKind.OpponentPrep)
        case _ => None

  enum JobStatus(val key: String):
    case Queued extends JobStatus("queued")
    case Running extends JobStatus("running")
    case Succeeded extends JobStatus("succeeded")
    case Failed extends JobStatus("failed")

    def terminal = this == Succeeded || this == Failed

  object JobStatus:
    def fromKey(key: String): Option[JobStatus] =
      values.find(_.key == key.trim.toLowerCase)

  case class JobError(code: String, message: String)
  object JobError:
    given OWrites[JobError] = Json.writes[JobError]

  case class SubmitResponse(jobId: String, status: String, url: String)
  object SubmitResponse:
    given OWrites[SubmitResponse] = Json.writes[SubmitResponse]

  case class StatusResponse(
      jobId: String,
      status: String,
      requested: String,
      startedAt: Option[String],
      finishedAt: Option[String],
      progressStage: String,
      queueState: String,
      snapshotState: String,
      processedGames: Int,
      totalGames: Option[Int],
      etaSec: Option[Int],
      cacheHit: Boolean,
      refreshLockedUntil: Option[String],
      studyId: Option[String],
      chapterId: Option[String],
      url: Option[String],
      notebookUrl: Option[String],
      warnings: List[String],
      error: Option[JobError]
  )
  object StatusResponse:
    given OWrites[StatusResponse] = Json.writes[StatusResponse]

  case class AccountIntelJob(
      id: String,
      ownerId: String,
      provider: String,
      username: String,
      kind: ProductKind,
      status: JobStatus,
      dedupeKey: String,
      ownerScopeKey: String,
      buildOwner: Boolean = true,
      buildSourceId: Option[String] = None,
      surfaceId: Option[String] = None,
      sourceFingerprint: Option[String] = None,
      progressStage: String,
      queueState: String = "queued",
      snapshotState: String = "queued",
      processedGames: Int = 0,
      totalGames: Option[Int] = None,
      etaSec: Option[Int] = Some(180),
      cacheHit: Boolean = false,
      refreshLockedUntil: Option[Instant] = None,
      requestedAt: Instant,
      startedAt: Option[Instant] = None,
      finishedAt: Option[Instant] = None,
      attemptCount: Int = 0,
      warnings: List[String] = Nil,
      studyId: Option[String] = None,
      chapterId: Option[String] = None,
      notebookUrl: Option[String] = None,
      surfaceJson: Option[String] = None,
      errorCode: Option[String] = None,
      errorMessage: Option[String] = None,
      createdAt: Instant = nowInstant,
      updatedAt: Instant = nowInstant
  ):
    def toStatusResponse(resultUrl: Option[String] = None): StatusResponse =
      StatusResponse(
        jobId = id,
        status = status.key,
        requested = requestedAt.toString,
        startedAt = startedAt.map(_.toString),
        finishedAt = finishedAt.map(_.toString),
        progressStage = progressStage,
        queueState = queueState,
        snapshotState = snapshotState,
        processedGames = processedGames,
        totalGames = totalGames,
        etaSec = etaSec,
        cacheHit = cacheHit,
        refreshLockedUntil = refreshLockedUntil.map(_.toString),
        studyId = studyId,
        chapterId = chapterId,
        url = resultUrl.orElse(Some(s"/account-intel/$provider/$username?kind=${kind.key}&jobId=$id")),
        notebookUrl = notebookUrl,
        warnings = warnings,
        error = errorCode.zip(errorMessage).headOption.map { case (code, message) =>
          JobError(code, message)
        }
      )

  object AccountIntelJob:
    def dedupeKey(provider: String, username: String, kind: ProductKind): String =
      s"${provider.trim.toLowerCase}|${username.trim.toLowerCase}|${kind.key}"

    def ownerScopeKey(ownerId: String, provider: String, username: String, kind: ProductKind): String =
      s"${ownerId.trim.toLowerCase}|${dedupeKey(provider, username, kind)}"

    def sameRouteScope(
        job: AccountIntelJob,
        ownerId: String,
        provider: String,
        username: String,
        kind: ProductKind
    ): Boolean =
      job.ownerScopeKey == ownerScopeKey(ownerId, provider, username, kind)

    def newQueued(ownerId: String, provider: String, username: String, kind: ProductKind): AccountIntelJob =
      val normalizedProvider = provider.trim.toLowerCase
      val normalizedUsername = username.trim
      val now = nowInstant
      AccountIntelJob(
        id = java.util.UUID.randomUUID().toString.replace("-", ""),
        ownerId = ownerId,
        provider = normalizedProvider,
        username = normalizedUsername,
        kind = kind,
        status = JobStatus.Queued,
        dedupeKey = dedupeKey(normalizedProvider, normalizedUsername, kind),
        ownerScopeKey = ownerScopeKey(ownerId, normalizedProvider, normalizedUsername, kind),
        progressStage = "queued",
        queueState = "queued",
        snapshotState = "queued",
        requestedAt = now,
        createdAt = now,
        updatedAt = now
      )

    def canReuse(
        job: AccountIntelJob,
        now: Instant,
        successTtl: FiniteDuration,
        failedCooldown: FiniteDuration
    ): Boolean =
      job.status match
        case JobStatus.Queued | JobStatus.Running => true
        case JobStatus.Succeeded => job.updatedAt.isAfter(now.minusMillis(successTtl.toMillis))
        case JobStatus.Failed => job.updatedAt.isAfter(now.minusMillis(failedCooldown.toMillis))

    def retryableFailed(
        job: AccountIntelJob,
        now: Instant,
        failedCooldown: FiniteDuration,
        retryLimit: Int
    ): Boolean =
      job.status == JobStatus.Failed &&
        job.attemptCount < retryLimit &&
        job.updatedAt.isBefore(now.minusMillis(failedCooldown.toMillis))

    def timedOutRunning(
        job: AccountIntelJob,
        now: Instant,
        runningTimeout: FiniteDuration
    ): Boolean =
      job.status == JobStatus.Running &&
        job.updatedAt.isBefore(now.minusMillis(runningTimeout.toMillis))

    def newAttached(
        ownerId: String,
        provider: String,
        username: String,
        kind: ProductKind,
        sourceJob: AccountIntelJob,
        refreshLockedUntil: Option[Instant]
    ): AccountIntelJob =
      val now = nowInstant
      AccountIntelJob(
        id = java.util.UUID.randomUUID().toString.replace("-", ""),
        ownerId = ownerId,
        provider = provider.trim.toLowerCase,
        username = username.trim,
        kind = kind,
        status = sourceJob.status,
        dedupeKey = sourceJob.dedupeKey,
        ownerScopeKey = ownerScopeKey(ownerId, provider, username, kind),
        buildOwner = false,
        buildSourceId = Some(sourceJob.id),
        surfaceId = sourceJob.surfaceId,
        sourceFingerprint = sourceJob.sourceFingerprint,
        progressStage = sourceJob.progressStage,
        queueState = sourceJob.queueState,
        snapshotState = sourceJob.snapshotState,
        processedGames = sourceJob.processedGames,
        totalGames = sourceJob.totalGames,
        etaSec = sourceJob.etaSec,
        cacheHit = sourceJob.cacheHit,
        refreshLockedUntil = refreshLockedUntil.orElse(sourceJob.refreshLockedUntil),
        requestedAt = now,
        startedAt = sourceJob.startedAt,
        finishedAt = sourceJob.finishedAt,
        warnings = sourceJob.warnings,
        studyId = sourceJob.studyId,
        chapterId = sourceJob.chapterId,
        notebookUrl = sourceJob.notebookUrl,
        surfaceJson = sourceJob.surfaceJson,
        errorCode = sourceJob.errorCode,
        errorMessage = sourceJob.errorMessage,
        createdAt = now,
        updatedAt = now
      )

    def newCached(
        ownerId: String,
        provider: String,
        username: String,
        kind: ProductKind,
        snapshot: AccountIntelSurfaceSnapshot,
        refreshLockedUntil: Option[Instant]
    ): AccountIntelJob =
      val now = nowInstant
      AccountIntelJob(
        id = java.util.UUID.randomUUID().toString.replace("-", ""),
        ownerId = ownerId,
        provider = provider.trim.toLowerCase,
        username = username.trim,
        kind = kind,
        status = JobStatus.Succeeded,
        dedupeKey = snapshot.dedupeKey,
        ownerScopeKey = ownerScopeKey(ownerId, provider, username, kind),
        buildOwner = false,
        surfaceId = Some(snapshot.id),
        sourceFingerprint = Some(snapshot.sourceFingerprint),
        progressStage = "completed",
        queueState = "ready",
        snapshotState = "ready",
        processedGames = snapshot.sampledGameCount,
        totalGames = Some(snapshot.sampledGameCount),
        etaSec = Some(0),
        cacheHit = true,
        refreshLockedUntil = refreshLockedUntil,
        requestedAt = now,
        startedAt = Some(now),
        finishedAt = Some(now),
        warnings = snapshot.warnings,
        surfaceJson = Some(snapshot.surfaceJson),
        createdAt = now,
        updatedAt = now
      )

  case class AccountIntelSurfaceSnapshot(
      id: String,
      dedupeKey: String,
      provider: String,
      username: String,
      kind: ProductKind,
      sourceFingerprint: String,
      surfaceJson: String,
      dossierJson: String,
      representativePgn: String,
      sampledGameCount: Int,
      eligibleGameCount: Int,
      warnings: List[String] = Nil,
      createdAt: Instant = nowInstant,
      updatedAt: Instant = nowInstant
  ):
    def toArtifact: NotebookBuildArtifact =
      NotebookBuildArtifact(
        dossier = Json.parse(dossierJson).as[JsObject],
        surface = Json.parse(surfaceJson).as[JsObject],
        representativePgn = PgnStr(representativePgn),
        sampledGameCount = sampledGameCount,
        eligibleGameCount = eligibleGameCount,
        warnings = warnings,
        diagnostics = NotebookBuildDiagnostics(Nil, Nil)
      )

  object AccountIntelSurfaceSnapshot:
    def fromArtifact(
        provider: String,
        username: String,
        kind: ProductKind,
        sourceFingerprint: String,
        artifact: NotebookBuildArtifact,
        now: Instant = nowInstant
    ): AccountIntelSurfaceSnapshot =
      AccountIntelSurfaceSnapshot(
        id = java.util.UUID.randomUUID().toString.replace("-", ""),
        dedupeKey = AccountIntelJob.dedupeKey(provider, username, kind),
        provider = provider.trim.toLowerCase,
        username = username.trim,
        kind = kind,
        sourceFingerprint = sourceFingerprint,
        surfaceJson = Json.stringify(artifact.surface),
        dossierJson = Json.stringify(artifact.dossier),
        representativePgn = artifact.representativePgn.value,
        sampledGameCount = artifact.sampledGameCount,
        eligibleGameCount = artifact.eligibleGameCount,
        warnings = artifact.warnings,
        createdAt = now,
        updatedAt = now
      )

    def fingerprint(games: List[ExternalGame]): String =
      val digest = MessageDigest.getInstance("SHA-256")
      games
        .sortBy(game => s"${game.playedAt}|${game.gameId}")
        .foreach: game =>
          digest.update(game.provider.getBytes(StandardCharsets.UTF_8))
          digest.update(0.toByte)
          digest.update(game.gameId.getBytes(StandardCharsets.UTF_8))
          digest.update(0.toByte)
          digest.update(game.playedAt.getBytes(StandardCharsets.UTF_8))
          digest.update(0.toByte)
          digest.update(game.result.getBytes(StandardCharsets.UTF_8))
          digest.update(0.toByte)
          digest.update(game.pgn.take(256).getBytes(StandardCharsets.UTF_8))
          digest.update(1.toByte)
      Base64.getUrlEncoder.withoutPadding.encodeToString(digest.digest())

  case class ExternalGame(
      provider: String,
      gameId: String,
      playedAt: String,
      white: String,
      black: String,
      result: String,
      sourceUrl: Option[String],
      pgn: String,
      moveEvals: List[MoveEval] = Nil
  )

  private[accountintel] enum SubjectResult:
    case Win, Draw, Loss

  case class PlySnap(ply: Int, fen: String, san: String, uci: String, color: Color)
  case class RepPos(snap: PlySnap, lastSan: Option[String])

  case class ParsedGame(
      external: ExternalGame,
      subjectName: String,
      subjectColor: Color,
      subjectResult: SubjectResult,
      openingName: String,
      openingFamily: String,
      openingBucket: String,
      openingRelation: String,
      canonicalEcoCode: Option[String],
      providerOpeningName: Option[String],
      providerEcoCode: Option[String],
      providerEcoUrl: Option[String],
      labels: List[String],
      plyCount: Int,
      rep: Option[RepPos]
  )

  case class DecisionEvent(
      gameId: String,
      subjectColor: Color,
      openingFamily: String,
      labels: List[String],
      ply: Int,
      fen: String,
      sideToMove: Color,
      quiet: Boolean,
      triggerHints: List[String],
      playedSan: String,
      playedUci: String,
      lastSan: Option[String],
      moveEval: Option[MoveEval],
      game: ParsedGame
  )

  case class SnapshotFeatureRow(
      gameId: String,
      subjectColor: Color,
      openingFamily: String,
      structureFamily: String,
      labels: List[String],
      ply: Int,
      fen: String,
      sideToMove: Color,
      quiet: Boolean,
      triggerHints: List[String],
      playedUci: String,
      playedSan: String,
      explainabilityScore: Double,
      preventabilityScore: Double,
      branchingScore: Double,
      transitionType: Option[String],
      strategicSalienceHigh: Boolean,
      planAlignmentBand: Option[String],
      planIntent: Option[String],
      planRisk: Option[String],
      hypothesisThemes: List[String],
      integratedTension: Double,
      earliestPreventablePly: Option[Int],
      collapseMomentPly: Option[Int],
      collapseAnalysis: Option[CollapseAnalysis],
      analysis: ExtendedAnalysisData,
      game: ParsedGame,
      lastSan: Option[String]
  )

  case class DecisionSnapshotCandidate(
      gameId: String,
      triggerType: String,
      side: Color,
      openingFamily: String,
      structureFamily: String,
      labels: List[String],
      ply: Int,
      fen: String,
      quiet: Boolean,
      playedUci: String,
      explainabilityScore: Double,
      preventabilityScore: Double,
      branchingScore: Double,
      snapshotConfidence: Double,
      commitmentScore: Double,
      collapseBacked: Boolean,
      transitionType: Option[String],
      planAlignmentBand: Option[String],
      earliestPreventablePly: Option[Int],
      windowStartPly: Int,
      windowEndPly: Int,
      repeatabilityKey: String,
      game: ParsedGame,
      lastSan: Option[String]
  )

  case class ClusterExemplar(
      game: ParsedGame,
      candidate: Option[DecisionSnapshotCandidate]
  )

  case class PriorityBreakdown(
      supportScore: Double,
      repeatabilityScore: Double,
      snapshotScore: Double,
      preventabilityScore: Double,
      branchingScore: Double,
      repairImpactScore: Double,
      readinessBonus: Double,
      triggerPenalty: Double,
      redundancyPenalty: Double
  ):
    def total: Double =
      supportScore +
        repeatabilityScore +
        snapshotScore +
        preventabilityScore +
        branchingScore +
        repairImpactScore +
        readinessBonus -
        triggerPenalty -
        redundancyPenalty

  case class SnapshotCluster(
      id: String,
      side: Color,
      openingFamily: String,
      structureFamily: String,
      triggerType: String,
      labels: List[String],
      priorityScore: Double,
      priorityBreakdown: PriorityBreakdown,
      distinctGames: Int,
      distinctOpenings: Int,
      collapseBackedRate: Double,
      offPlanRate: Double,
      quietRate: Double,
      triggerDiversity: Double,
      snapshotConfidenceMean: Double,
      exemplarCentrality: Double,
      nonContinuationRate: Double,
      averageExplainability: Double,
      averagePreventability: Double,
      averageBranching: Double,
      resultImpactScore: Double,
      earliestPreventableRate: Double,
      candidates: List[DecisionSnapshotCandidate],
      exemplar: ClusterExemplar
  ):
    def support = distinctGames
    def ready =
      exemplar.candidate.exists(_.fen.trim.nonEmpty) &&
        (distinctGames >= 3 || (distinctGames == 2 && collapseBackedRate > 0))

  case class PrimitiveBundle(
      parsedGames: List[ParsedGame],
      featureRows: List[SnapshotFeatureRow],
      sampledGameCount: Int,
      eligibleGameCount: Int,
      warnings: List[String]
  )

  case class NotebookBuildDiagnostics(
      allClusters: List[SnapshotCluster],
      selectedClusters: List[SnapshotCluster]
  )

  case class NotebookBuildArtifact(
      dossier: JsObject,
      surface: JsObject,
      representativePgn: PgnStr,
      sampledGameCount: Int,
      eligibleGameCount: Int,
      warnings: List[String],
      diagnostics: NotebookBuildDiagnostics
  )

  trait AccountIntelJobStore:
    def byId(id: String): Fu[Option[AccountIntelJob]]
    def insert(job: AccountIntelJob): Funit
    def latestByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]]
    def latestSucceededByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]]
    def latestActiveByOwnerScopeKey(ownerScopeKey: String): Fu[Option[AccountIntelJob]]
    def latestActiveBuildByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelJob]]
    def recentByOwnerScopeKey(ownerScopeKey: String, limit: Int): Fu[List[AccountIntelJob]]
    def recentSucceededByOwner(ownerId: String, limit: Int): Fu[List[AccountIntelJob]]
    def claimNextQueued(now: Instant): Fu[Option[AccountIntelJob]]
    def claimQueuedById(id: String, now: Instant): Fu[Option[AccountIntelJob]]
    def setProgress(
        id: String,
        progressStage: String,
        snapshotState: String,
        processedGames: Option[Int] = None,
        totalGames: Option[Int] = None,
        etaSec: Option[Int] = None
    ): Funit
    def markSucceeded(
        id: String,
        surfaceId: String,
        sourceFingerprint: String,
        warnings: List[String],
        surfaceJson: String,
        sampledGameCount: Int,
        totalGames: Int,
        cacheHit: Boolean,
        refreshLockedUntil: Option[Instant]
    ): Funit
    def setPublication(id: String, studyId: String, chapterId: String, notebookUrl: String): Funit
    def markFailed(id: String, code: String, message: String): Funit
    def requeueTimedOutRunning(timeout: FiniteDuration, retryLimit: Int): Funit
    def cleanupExpired(successTtl: FiniteDuration, failedTtl: FiniteDuration): Funit

  trait AccountIntelSurfaceStore:
    def byId(id: String): Fu[Option[AccountIntelSurfaceSnapshot]]
    def latestByDedupeKey(dedupeKey: String): Fu[Option[AccountIntelSurfaceSnapshot]]
    def latestFreshByDedupeKey(dedupeKey: String, freshSince: Instant): Fu[Option[AccountIntelSurfaceSnapshot]]
    def byDedupeKeyAndFingerprint(
        dedupeKey: String,
        sourceFingerprint: String
    ): Fu[Option[AccountIntelSurfaceSnapshot]]
    def insert(snapshot: AccountIntelSurfaceSnapshot): Funit

  trait AccountIntelJobDispatcher:
    def dispatch(job: AccountIntelJob): Fu[DispatchOutcome]

  trait AccountGameFetcher:
    def fetchRecentGames(provider: String, username: String): Fu[List[ExternalGame]]

  def normalizeProvider(provider: String): Option[String] =
    Option(provider).map(_.trim.toLowerCase).filter(Set("lichess", "chesscom"))

  def normalizeUsername(username: String): Option[String] =
    Option(username).map(_.trim).filter(_.matches("^[A-Za-z0-9][A-Za-z0-9_-]{1,29}$"))

  def notebookName(username: String, kind: ProductKind): lila.core.study.data.StudyName =
    lila.study.Study.toName(
      kind match
        case ProductKind.MyAccountIntelligenceLite => s"$username notebook"
        case ProductKind.OpponentPrep => s"$username prep notebook"
    )

  private[accountintel] def colorLabel(color: Color): String = if color.white then "White" else "Black"
  private[accountintel] def colorKey(color: Color): String = if color.white then "white" else "black"
  private[accountintel] def slug(value: String): String =
    value.toLowerCase.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") match
      case "" => "item"
      case v => v
