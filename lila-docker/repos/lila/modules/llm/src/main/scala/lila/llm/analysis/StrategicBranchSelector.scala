package lila.llm.analysis

import lila.llm.{ ActiveStrategicThread, ActiveStrategicThreadRef, GameChronicleMoment }

object StrategicBranchSelector:

  private val OpeningEventMomentTypes = Set(
    "OpeningBranchPoint",
    "OpeningOutOfBook",
    "OpeningNovelty",
    "OpeningTheoryEnds"
  )
  private val MaxThreads = 3
  private val MaxVisibleMoments = 12
  private val MaxVisibleThreadMoments = MaxThreads * 3
  private val BaseActiveNotes = 8

  final case class StrategicBranchSelection(
      selectedMoments: List[GameChronicleMoment],
      activeNoteMoments: List[GameChronicleMoment],
      threads: List[ActiveStrategicThread],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef],
      threadRepresentativeSelectedPlies: Set[Int]
  )

  private case class TruthSelectionView(
      classificationKey: String,
      ownershipRole: TruthOwnershipRole,
      visibilityRole: TruthVisibilityRole,
      surfaceMode: TruthSurfaceMode,
      exemplarRole: TruthExemplarRole,
      chainKey: Option[String],
      maintenanceExemplarCandidate: Boolean,
      reasonFamily: DecisiveReasonFamily,
      failureMode: FailureInterpretationMode,
      benchmarkCriticalMove: Boolean
  )

  private case class ProtectedMomentCandidate(
      moment: GameChronicleMoment,
      priority: (Int, Int, Int, Int),
      truthView: TruthSelectionView,
      threadId: Option[String],
      threadRepresentativePromoted: Boolean
  )

  private case class ThreadSelectionArtifacts(
      rankedThreads: List[ActiveStrategicThreadBuilder.BuiltThread],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef],
      threadedPlies: Set[Int],
      threadLocalRepresentativeMoments: List[GameChronicleMoment],
      threadRepresentativeSelectedPlies: Set[Int]
  )

  private case class NonProtectedVisibleCandidate(
      moment: GameChronicleMoment,
      sourcePriority: Int,
      sortKey: (Int, Int, Int, Int)
  )

  def select(moments: List[GameChronicleMoment]): List[GameChronicleMoment] =
    buildSelection(moments).selectedMoments

  def rankThreads(moments: List[GameChronicleMoment]): List[ActiveStrategicThreadBuilder.BuiltThread] =
    sortThreads(ActiveStrategicThreadBuilder.build(moments).builtThreads)

  def buildSelection(moments: List[GameChronicleMoment]): StrategicBranchSelection =
    buildSelection(moments, Map.empty)

  def buildSelection(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): StrategicBranchSelection =
    buildSelectionArtifacts(moments, truthContractsByPly)

  private def buildSelectionArtifacts(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): StrategicBranchSelection =
    val threadSelection = buildThreadSelectionArtifacts(moments, truthContractsByPly)
    val canonicalProtected =
      buildCanonicalProtectedOrder(
        moments,
        truthContractsByPly,
        threadSelection.threadRefsByPly,
        threadSelection.threadLocalRepresentativeMoments
      )
    val visibleSelection =
      buildVisibleMoments(
        moments,
        truthContractsByPly,
        threadSelection,
        canonicalProtected
      )
    val activeSelection =
      buildActiveNoteMoments(
        moments,
        truthContractsByPly,
        threadSelection,
        canonicalProtected
      )
    if threadSelection.rankedThreads.isEmpty then
      StrategicBranchSelection(
        selectedMoments = visibleSelection,
        activeNoteMoments = activeSelection,
        threads = Nil,
        threadRefsByPly = Map.empty,
        threadRepresentativeSelectedPlies = threadSelection.threadRepresentativeSelectedPlies
      )
    else
      StrategicBranchSelection(
        selectedMoments = visibleSelection,
        activeNoteMoments = activeSelection,
        threads = threadSelection.rankedThreads.map(_.thread),
        threadRefsByPly = threadSelection.threadRefsByPly,
        threadRepresentativeSelectedPlies = threadSelection.threadRepresentativeSelectedPlies
      )

  private def buildThreadSelectionArtifacts(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): ThreadSelectionArtifacts =
    val threadResult = ActiveStrategicThreadBuilder.build(moments)
    val rankedThreads = sortThreads(threadResult.builtThreads).take(MaxThreads)
    val rankedThreadIds = rankedThreads.map(_.thread.threadId).toSet
    val threadRefsByPly =
      threadResult.threadRefsByPly.filter { case (_, ref) => rankedThreadIds.contains(ref.threadId) }
    val threadedPlies = threadRefsByPly.keySet
    val threadLocalRepresentativeMoments =
      rankedThreads
        .flatMap(thread => selectRepresentatives(thread, truthContractsByPly))
        .distinctBy(_.ply)
    val threadRepresentativeSelectedPlies = threadLocalRepresentativeMoments.map(_.ply).toSet
    ThreadSelectionArtifacts(
      rankedThreads = rankedThreads,
      threadRefsByPly = threadRefsByPly,
      threadedPlies = threadedPlies,
      threadLocalRepresentativeMoments = threadLocalRepresentativeMoments,
      threadRepresentativeSelectedPlies = threadRepresentativeSelectedPlies
    )

  private def buildVisibleMoments(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      threadSelection: ThreadSelectionArtifacts,
      canonicalProtectedOrder: List[GameChronicleMoment]
  ): List[GameChronicleMoment] =
    val protectedVisibleMoments = canonicalProtectedOrder.take(MaxVisibleMoments)
    val protectedVisiblePlies = protectedVisibleMoments.map(_.ply).toSet
    val protectedVisibleThreadIds =
      protectedVisibleMoments.flatMap(moment => surfacedThreadId(moment, threadSelection.threadRefsByPly)).toSet
    val remainingVisibleSlotsAfterProtected = (MaxVisibleMoments - protectedVisibleMoments.size).max(0)
    val visibleThreadOrder =
      selectGlobalThreadMoments(
        representativeMoments = threadSelection.threadLocalRepresentativeMoments,
        truthContractsByPly = truthContractsByPly,
        eligibility = _.globalVisibleEligible
      )
        .filterNot(moment => protectedVisiblePlies.contains(moment.ply))
        .filterNot(moment =>
          surfacedThreadId(moment, threadSelection.threadRefsByPly).exists(protectedVisibleThreadIds.contains)
        )
    val visibleThreadMoments =
      visibleThreadOrder.take(MaxVisibleThreadMoments.min(remainingVisibleSlotsAfterProtected))
    val visibleThreadPlies = visibleThreadMoments.map(_.ply).toSet
    val visibleNonProtectedMoments =
      buildNonProtectedVisibleStream(
        moments = moments,
        threadedPlies = threadSelection.threadedPlies,
        truthContractsByPly = truthContractsByPly,
        excludedPlies = protectedVisiblePlies ++ visibleThreadPlies
      ).take((remainingVisibleSlotsAfterProtected - visibleThreadMoments.size).max(0))
    val finalVisibleOrder =
      (
        protectedVisibleMoments ++
          visibleThreadMoments ++
          visibleNonProtectedMoments
      ).distinctBy(_.ply).take(MaxVisibleMoments)
    finalVisibleOrder.sortBy(_.ply)

  private def buildActiveNoteMoments(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      threadSelection: ThreadSelectionArtifacts,
      canonicalProtectedOrder: List[GameChronicleMoment]
  ): List[GameChronicleMoment] =
    val baseProtectedActiveNoteMoments = canonicalProtectedOrder.take(BaseActiveNotes)
    val protectedActiveNotePlies = baseProtectedActiveNoteMoments.map(_.ply).toSet
    val remainingActiveNoteSlotsAfterProtected =
      (BaseActiveNotes - baseProtectedActiveNoteMoments.size).max(0)
    val activeNoteThreadMoments =
      selectGlobalThreadMoments(
        representativeMoments = threadSelection.threadLocalRepresentativeMoments,
        truthContractsByPly = truthContractsByPly,
        eligibility = _.globalActiveNoteEligible
      )
        .filterNot(moment => protectedActiveNotePlies.contains(moment.ply))
        .take(remainingActiveNoteSlotsAfterProtected)
    val activeNoteThreadPlies = activeNoteThreadMoments.map(_.ply).toSet
    val activeNoteFallbackMoments =
      buildNonProtectedActiveNoteStream(
        moments = moments,
        excludedPlies = protectedActiveNotePlies ++ activeNoteThreadPlies,
        threadedRepresentativePlies = threadSelection.threadRepresentativeSelectedPlies,
        truthContractsByPly = truthContractsByPly
      )
        .take((remainingActiveNoteSlotsAfterProtected - activeNoteThreadMoments.size).max(0))
    val prioritizedActiveNoteMoments =
      if threadSelection.rankedThreads.isEmpty then
        (baseProtectedActiveNoteMoments ++ activeNoteFallbackMoments).distinctBy(_.ply).take(BaseActiveNotes)
      else
        (
          baseProtectedActiveNoteMoments ++
            (activeNoteThreadMoments ++ activeNoteFallbackMoments)
              .filterNot(moment => protectedActiveNotePlies.contains(moment.ply))
        ).distinctBy(_.ply).take(BaseActiveNotes)
    val overflowActiveNoteMoments =
      selectProtectedOverflowActiveNotes(
        protectedCandidates = canonicalProtectedOrder,
        baseProtectedSeedMoments = baseProtectedActiveNoteMoments,
        threadRefsByPly = threadSelection.threadRefsByPly,
        truthContractsByPly = truthContractsByPly
      )
    (prioritizedActiveNoteMoments ++ overflowActiveNoteMoments).distinctBy(_.ply)

  private def buildNonProtectedVisibleStream(
      moments: List[GameChronicleMoment],
      threadedPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      excludedPlies: Set[Int]
  ): List[GameChronicleMoment] =
    suppressInvestmentEchoes(
      moments
        .filterNot(moment => excludedPlies.contains(moment.ply))
        .filterNot(moment => isThreadedSelectionSuppressed(moment, threadedPlies))
        .flatMap(moment =>
          nonProtectedVisiblePriority(moment, truthContractsByPly).map(candidate => candidate -> moment)
        )
        .sortBy { case (candidate, moment) =>
          (
            candidate.sourcePriority,
            candidate.sortKey._1,
            candidate.sortKey._2,
            candidate.sortKey._3,
            candidate.sortKey._4,
            moment.ply
          )
        }
        .map(_._2),
      truthContractsByPly
    ).filterNot(moment => excludedPlies.contains(moment.ply))

  private def buildNonProtectedActiveNoteStream(
      moments: List[GameChronicleMoment],
      excludedPlies: Set[Int],
      threadedRepresentativePlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments
      .filterNot(moment => excludedPlies.contains(moment.ply))
      .filterNot(moment => isActiveNoteThreadSuppressed(moment, threadedRepresentativePlies))
      .flatMap(moment => activeNoteFallbackScore(moment, truthContractsByPly).map(score => score -> moment))
      .sortBy { case ((priority, secondary, salience, tiebreak), moment) =>
        (priority, secondary, salience, tiebreak, moment.ply)
      }
      .map(_._2)

  private def isThreadedSelectionSuppressed(
      moment: GameChronicleMoment,
      threadedPlies: Set[Int]
  ): Boolean =
    threadedPlies.contains(moment.ply) ||
      moment.selectionKind == "thread_bridge" ||
      moment.momentType == "OpeningIntro"

  private def isActiveNoteThreadSuppressed(
      moment: GameChronicleMoment,
      threadedRepresentativePlies: Set[Int]
  ): Boolean =
    threadedRepresentativePlies.contains(moment.ply) ||
      moment.selectionKind == "thread_bridge" ||
      moment.momentType == "OpeningIntro"

  private def nonProtectedVisiblePriority(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[NonProtectedVisibleCandidate] =
    compensationPriorityScore(moment, truthContractsByPly).map(score =>
      NonProtectedVisibleCandidate(moment, 0, score)
    ).orElse(
      coreEventPriority(moment, truthContractsByPly).map { case (priority, tiebreak) =>
        NonProtectedVisibleCandidate(moment, 1, (priority, tiebreak, 0, 0))
      }
    ).orElse(
      strategicFallbackScore(moment, truthContractsByPly).map { case (priority, secondary, tiebreak) =>
        NonProtectedVisibleCandidate(moment, 2, (priority, secondary, tiebreak, 0))
      }
    )

  private def coreEventPriority(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int)] =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = chronicleSemantics(moment, truthContractsByPly)
    val fallbackCoreEligible =
      !semantics.hasTruthContract &&
        (isSevereFailure(truthView) || isMatePivot(moment) || isOpeningBranchEvent(moment))
    Option.when(semantics.globalVisibleEligible || fallbackCoreEligible) {
      if isSevereFailure(truthView) then 1 -> moment.ply
      else if isPromotedBestHold(truthView) then 2 -> moment.ply
      else if isVerifiedExemplar(truthView) then 3 -> moment.ply
      else if isProvisionalExemplar(truthView) || isPrimaryConversionOwner(truthView, moment) then 4 -> moment.ply
      else if isSelectionExemplar(truthView) then 5 -> moment.ply
      else if isMaintenanceEcho(truthView) then 6 -> moment.ply
      else if isMatePivot(moment) then 7 -> moment.ply
      else 8 -> moment.ply
    }

  private def isBlunder(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "blunder"

  private def isMissedWin(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.BlunderOwner &&
      truthView.classificationKey == "missedwin"

  private def isSevereFailure(truthView: TruthSelectionView): Boolean =
    isBlunder(truthView) || isMissedWin(truthView)

  private def isPromotedBestHold(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey == "best" &&
      truthView.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense &&
      truthView.benchmarkCriticalMove

  private def isOverflowProtectedFamily(truthView: TruthSelectionView): Boolean =
    isBlunder(truthView) || isMissedWin(truthView) || isPromotedBestHold(truthView)

  private def isCriticalBestTacticalOrTechnical(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey == "best" &&
      (
        truthView.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
          (
            truthView.reasonFamily == DecisiveReasonFamily.QuietTechnicalMove &&
              truthView.benchmarkCriticalMove
          )
      )

  private def isFailureSignificantThreadLocal(truthView: TruthSelectionView): Boolean =
    truthView.classificationKey != "best" &&
      truthView.failureMode != FailureInterpretationMode.NoClearPlan &&
      (
        truthView.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
          truthView.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
          truthView.reasonFamily == DecisiveReasonFamily.QuietTechnicalMove
      )

  private def isMatePivot(moment: GameChronicleMoment): Boolean =
    moment.momentType == "MatePivot"

  private def isExemplarPivot(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar ||
      truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar ||
      truthView.maintenanceExemplarCandidate

  private def isOpeningBranchEvent(moment: GameChronicleMoment): Boolean =
    OpeningEventMomentTypes.contains(moment.momentType)

  private def isVerifiedExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.VerifiedExemplar &&
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible

  private def isProvisionalExemplar(truthView: TruthSelectionView): Boolean =
    truthView.exemplarRole == TruthExemplarRole.ProvisionalExemplar &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden

  private def isSelectionExemplar(truthView: TruthSelectionView): Boolean =
    isProvisionalExemplar(truthView) ||
      (
        truthView.maintenanceExemplarCandidate &&
          truthView.visibilityRole != TruthVisibilityRole.Hidden
      )

  private def isMaintenanceEcho(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.MaintenanceEcho &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden

  private def isCommitmentOwner(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.CommitmentOwner

  private def isConversionOwner(truthView: TruthSelectionView): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.ConversionOwner

  private def isPrimaryConversionOwner(
      truthView: TruthSelectionView,
      moment: GameChronicleMoment
  ): Boolean =
    truthView.ownershipRole == TruthOwnershipRole.ConversionOwner &&
      truthView.visibilityRole != TruthVisibilityRole.Hidden &&
      (
        truthView.surfaceMode == TruthSurfaceMode.ConversionExplain ||
          moment.transitionType.exists(_.toLowerCase.contains("convert")) ||
          moment.transitionType.exists(_.toLowerCase.contains("promotion")) ||
          moment.transitionType.exists(_.toLowerCase.contains("exchange")) ||
          moment.transitionType.exists(_.toLowerCase.contains("simplif"))
      )

  private def truthSelectionView(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): TruthSelectionView =
    val projection = DecisiveTruth.momentProjection(moment, truthContractsByPly.get(moment.ply))
    TruthSelectionView(
      classificationKey = projection.classificationKey,
      ownershipRole = projection.ownershipRole,
      visibilityRole = projection.visibilityRole,
      surfaceMode = projection.surfaceMode,
      exemplarRole = projection.exemplarRole,
      chainKey = projection.chainKey,
      maintenanceExemplarCandidate = projection.maintenanceExemplarCandidate,
      reasonFamily =
        truthContractsByPly.get(moment.ply).map(_.reasonFamily).getOrElse(DecisiveReasonFamily.QuietTechnicalMove),
      failureMode =
        truthContractsByPly.get(moment.ply).map(_.failureMode).getOrElse(FailureInterpretationMode.NoClearPlan),
      benchmarkCriticalMove = truthContractsByPly.get(moment.ply).exists(_.benchmarkCriticalMove)
    )

  private def chronicleSemantics(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): MomentTruthSemantics.ChronicleSemantics =
    MomentTruthSemantics.chronicle(moment, truthContractsByPly.get(moment.ply))

  private def strategicFallbackScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val semantics = chronicleSemantics(moment, truthContractsByPly)
    Option.when(semantics.globalVisibleEligible) {
      val priority =
        if surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if surface.strictCompensationPosition && surface.durableCompensationPosition then 1
        else if surface.strictCompensationPosition then 2
        else if surface.dominantIdeaText.nonEmpty || surface.executionText.nonEmpty then 3
        else if surface.focusText.nonEmpty then 4
        else 5
      val secondary =
        if moment.selectionKind == "key" then 0
        else if moment.selectionKind == "opening" then 1
        else 2
      val tiebreak =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      (priority, secondary, tiebreak)
    }

  private def compensationPriorityScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = chronicleSemantics(moment, truthContractsByPly)
    val visibleInvestment =
      truthView.visibilityRole == TruthVisibilityRole.PrimaryVisible ||
        truthView.visibilityRole == TruthVisibilityRole.SupportingVisible
    val exemplarTruth = isExemplarPivot(truthView)
    val compensationEligible =
      if semantics.hasTruthContract then semantics.compensationSelectionEligible && semantics.globalVisibleEligible
      else surface.strictCompensationPosition || visibleInvestment || exemplarTruth
    Option.when(compensationEligible) {
      val priority =
        if isVerifiedExemplar(truthView) then 0
        else if isPrimaryConversionOwner(truthView, moment) then 1
        else if isSelectionExemplar(truthView) then 2
        else if isMaintenanceEcho(truthView) then 3
        else if !semantics.hasTruthContract && surface.quietCompensationPosition then 4
        else if !semantics.hasTruthContract && surface.durableCompensationPosition then 5
        else 6
      val evidence =
        if isVerifiedExemplar(truthView) then 0
        else if isSelectionExemplar(truthView) then 1
        else if isMaintenanceEcho(truthView) then 2
        else if moment.probeRefinementRequests.nonEmpty then 0
        else if !semantics.hasTruthContract && moment.signalDigest.exists(_.decisionComparison.isDefined) then 3
        else if moment.selectionKind == "key" then 4
        else 5
      val salience =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      val tiebreak = if moment.selectionKind == "key" then 0 else 1
      (priority, evidence, salience, tiebreak)
    }

  private def activeNoteFallbackScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = chronicleSemantics(moment, truthContractsByPly)
    Option.when(semantics.globalActiveNoteEligible) {
      val priority =
        if isSevereFailure(truthView) then 0
        else if isPromotedBestHold(truthView) then 1
        else if isVerifiedExemplar(truthView) then 2
        else if isPrimaryConversionOwner(truthView, moment) then 3
        else if isSelectionExemplar(truthView) then 4
        else if isMaintenanceEcho(truthView) then 5
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.quietCompensationPosition then 6
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.durableCompensationPosition then 7
        else if !semantics.hasTruthContract && surface.strictCompensationPosition then 8
        else if moment.selectionKind == "key" && surface.dominantIdeaText.nonEmpty then 9
        else if moment.selectionKind == "key" then 10
        else 11
      val secondary =
        if isSevereFailure(truthView) then 0
        else if isPromotedBestHold(truthView) then 1
        else if isVerifiedExemplar(truthView) then 2
        else if isSelectionExemplar(truthView) then 3
        else if isMaintenanceEcho(truthView) then 4
        else if !semantics.hasTruthContract && surface.strictCompensationPosition && surface.quietCompensationPosition then 0
        else if !semantics.hasTruthContract && moment.signalDigest.exists(_.compensation.exists(_.trim.nonEmpty)) then 5
        else if surface.executionText.nonEmpty || surface.objectiveText.nonEmpty then 5
        else 6
      val salience =
        moment.strategicSalience match
          case Some(level) if level.equalsIgnoreCase("High")   => 0
          case Some(level) if level.equalsIgnoreCase("Medium") => 1
          case _                                               => 2
      val tiebreak =
        if moment.strategicThread.isDefined then 0
        else if moment.selectionKind == "opening" then 1
        else 2
      (priority, secondary, salience, tiebreak)
    }

  private def suppressInvestmentEchoes(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    moments.foldLeft(
      (
        List.empty[GameChronicleMoment],
        Set.empty[String],
        Map.empty[String, Int]
      )
    ) { case ((accepted, primarySeen, supportSeen), moment) =>
      val truthView = truthSelectionView(moment, truthContractsByPly)
      truthView.chainKey match
        case Some(chainKey) if chainKey.nonEmpty =>
          truthView.visibilityRole match
            case TruthVisibilityRole.PrimaryVisible =>
              if primarySeen.contains(chainKey) then (accepted, primarySeen, supportSeen)
              else (accepted :+ moment, primarySeen + chainKey, supportSeen)
            case TruthVisibilityRole.SupportingVisible =>
              val supportCount = supportSeen.getOrElse(chainKey, 0)
              val maxSupports =
                if qualifiesForSecondSupport(truthView) then 2
                else 1
              if supportCount >= maxSupports then (accepted, primarySeen, supportSeen)
              else (accepted :+ moment, primarySeen, supportSeen.updated(chainKey, supportCount + 1))
            case TruthVisibilityRole.Hidden =>
              (accepted :+ moment, primarySeen, supportSeen)
        case _ =>
          (accepted :+ moment, primarySeen, supportSeen)
    }._1

  private def threadScore(thread: ActiveStrategicThreadBuilder.BuiltThread): Double =
    val moments = thread.moments
    val routeRichness = moments.count(_.moment.strategyPack.exists(_.pieceRoutes.nonEmpty)) * 0.45
    val comparisonRichness = moments.count(_.moment.signalDigest.flatMap(_.decisionComparison).isDefined) * 0.40
    val salience = moments.count(_.moment.strategicSalience.exists(_.equalsIgnoreCase("High"))) * 0.35
    val opponentVisibility =
      moments.count(m =>
        m.moment.signalDigest.flatMap(_.opponentPlan).exists(_.trim.nonEmpty) ||
          m.moment.signalDigest.flatMap(_.prophylaxisThreat).exists(_.trim.nonEmpty)
      ) * 0.30
    val specificity = moments.headOption.map(_.theme.specificityScore * 2.4).getOrElse(0.0)
    thread.thread.continuityScore * 3.2 + specificity + routeRichness + comparisonRichness + salience + opponentVisibility

  private def sortThreads(
      threads: List[ActiveStrategicThreadBuilder.BuiltThread]
  ): List[ActiveStrategicThreadBuilder.BuiltThread] =
    threads.sortBy(thread => (-threadScore(thread), thread.thread.seedPly))

  private def selectRepresentatives(
      thread: ActiveStrategicThreadBuilder.BuiltThread,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    val seed = thread.moments.find(_.stage == ActiveStrategicThreadBuilder.ThreadStage.Seed).map(_.moment)
    val build =
      thread.moments.find(_.stage == ActiveStrategicThreadBuilder.ThreadStage.Build).map(_.moment)
        .orElse(thread.moments.drop(1).headOption.map(_.moment))
    val finisher =
      thread.moments.reverse.find(m =>
        m.stage == ActiveStrategicThreadBuilder.ThreadStage.Switch ||
          m.stage == ActiveStrategicThreadBuilder.ThreadStage.Convert
      ).map(_.moment)
        .orElse(thread.moments.lastOption.map(_.moment))
    val stagePriorityByPly =
      List(seed.map(_.ply -> 0), build.map(_.ply -> 1), finisher.map(_.ply -> 2)).flatten.toMap
    val threadMoments = thread.moments.map(_.moment).distinctBy(_.ply)
    val semanticsByPly =
      threadMoments.map(moment => moment.ply -> chronicleSemantics(moment, truthContractsByPly)).toMap
    val orderedCandidates =
      threadMoments
        .sortBy(moment =>
          representativeMomentScore(moment, truthContractsByPly, stagePriorityByPly, semanticsByPly)
        )
    val stageRepresentatives =
      List(seed, build, finisher).foldLeft(List.empty[GameChronicleMoment]) { case (selected, stageMomentOpt) =>
        val usedPlies = selected.map(_.ply).toSet
        val chosen =
          stageMomentOpt.flatMap(stageMoment =>
            orderedCandidates
              .filterNot(moment => usedPlies.contains(moment.ply))
              .sortBy(moment =>
                representativeStageScore(
                  moment = moment,
                  stageMoment = stageMoment,
                  truthContractsByPly = truthContractsByPly,
                  stagePriorityByPly = stagePriorityByPly,
                  semanticsByPly = semanticsByPly
                )
              )
              .headOption
          )
        chosen.fold(selected)(selected :+ _)
      }
    val supplementalRepresentatives =
      orderedCandidates
        .filterNot(moment => stageRepresentatives.exists(_.ply == moment.ply))
        .take((3 - stageRepresentatives.size).max(0))
    (stageRepresentatives ++ supplementalRepresentatives).distinctBy(_.ply).take(3).sortBy(_.ply)

  private def representativeMomentScore(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      stagePriorityByPly: Map[Int, Int],
      semanticsByPly: Map[Int, MomentTruthSemantics.ChronicleSemantics]
  ): (Int, Int, Int, Int) =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    val semantics = semanticsByPly.getOrElse(moment.ply, chronicleSemantics(moment, truthContractsByPly))
    val selectionBand =
      if isStrongRepresentativeQualified(semantics) || isFailureSignificantThreadLocal(truthView) then 0
      else if isSecondaryRepresentativeQualified(semantics) then 1
      else 2
    val priority =
      if isSevereFailure(truthView) then 0
      else if isPromotedBestHold(truthView) then 1
      else if isVerifiedExemplar(truthView) then 2
      else if isProvisionalExemplar(truthView) || isPrimaryConversionOwner(truthView, moment) then 3
      else if isFailureSignificantThreadLocal(truthView) then 4
      else if isCriticalBestTacticalOrTechnical(truthView) then 5
      else if isSelectionExemplar(truthView) then 6
      else if isMaintenanceEcho(truthView) then 7
      else if truthView.visibilityRole != TruthVisibilityRole.Hidden then 8
      else 9
    val stagePenalty = stagePriorityByPly.getOrElse(moment.ply, 3)
    (selectionBand, priority, stagePenalty, moment.ply)

  private def representativeStageScore(
      moment: GameChronicleMoment,
      stageMoment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      stagePriorityByPly: Map[Int, Int],
      semanticsByPly: Map[Int, MomentTruthSemantics.ChronicleSemantics]
  ): (Int, Int, Int, Int, Int) =
    val (selectionBand, priority, stagePenalty, tiebreak) =
      representativeMomentScore(moment, truthContractsByPly, stagePriorityByPly, semanticsByPly)
    val distancePenalty = math.abs(stageMoment.ply - moment.ply)
    (selectionBand, distancePenalty, priority, stagePenalty, tiebreak)

  private def selectGlobalThreadMoments(
      representativeMoments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      eligibility: MomentTruthSemantics.ChronicleSemantics => Boolean
  ): List[GameChronicleMoment] =
    representativeMoments.zipWithIndex
      .flatMap { case (moment, order) =>
        val truthView = truthSelectionView(moment, truthContractsByPly)
        val semantics = chronicleSemantics(moment, truthContractsByPly)
        Option.when(eligibility(semantics)) {
          globalThreadCandidatePriority(moment, truthView, order) -> moment
        }
      }
      .sortBy { case ((priority, secondary, order, ply), moment) =>
        (priority, secondary, order, ply, moment.ply)
      }
      .map(_._2)
      .distinctBy(_.ply)

  private def globalThreadCandidatePriority(
      moment: GameChronicleMoment,
      truthView: TruthSelectionView,
      order: Int
  ): (Int, Int, Int, Int) =
    val priority =
      if isSevereFailure(truthView) then 0
      else if isPromotedBestHold(truthView) then 1
      else if isVerifiedExemplar(truthView) then 2
      else if isProvisionalExemplar(truthView) then 3
      else if isCommitmentOwner(truthView) then 4
      else if isConversionOwner(truthView) then 5
      else if isMaintenanceEcho(truthView) then 6
      else 7
    val secondary =
      moment.strategicSalience match
        case Some(level) if level.equalsIgnoreCase("High")   => 0
        case Some(level) if level.equalsIgnoreCase("Medium") => 1
        case _                                               => 2
    (priority, secondary, order, moment.ply)

  private def qualifiesForSecondSupport(truthView: TruthSelectionView): Boolean =
    truthView.maintenanceExemplarCandidate ||
      truthView.classificationKey != "best" ||
      truthView.failureMode != FailureInterpretationMode.NoClearPlan

  private def isStrongRepresentativeQualified(
      semantics: MomentTruthSemantics.ChronicleSemantics
  ): Boolean =
    semantics.globalVisibleEligible

  private def isSecondaryRepresentativeQualified(
      semantics: MomentTruthSemantics.ChronicleSemantics
  ): Boolean =
    semantics.threadLocalReplacementEligible && !semantics.globalVisibleEligible

  private def buildCanonicalProtectedOrder(
      moments: List[GameChronicleMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef],
      threadLocalRepresentativeMoments: List[GameChronicleMoment]
  ): List[GameChronicleMoment] =
    val representativePromotedPlies =
      threadLocalRepresentativeMoments.flatMap { moment =>
        val truthView = truthSelectionView(moment, truthContractsByPly)
        Option.when(isPromotedBestHold(truthView))(moment.ply)
      }.toSet
    val candidates =
      moments
        .flatMap { moment =>
        val truthView = truthSelectionView(moment, truthContractsByPly)
        protectedMomentPriority(moment, truthContractsByPly).map(priority =>
          ProtectedMomentCandidate(
            moment = moment,
            priority = priority,
            truthView = truthView,
            threadId = surfacedThreadId(moment, threadRefsByPly),
            threadRepresentativePromoted = representativePromotedPlies.contains(moment.ply)
          )
        )
      }
    val severeCandidates =
      candidates
        .filter(candidate => isSevereFailure(candidate.truthView))
        .sortBy(protectedCandidateSortKey)
    val promotedCandidates =
      candidates.filter(candidate => isPromotedBestHold(candidate.truthView))
    val threadedPromoted = promotedCandidates.filter(_.threadId.nonEmpty)
    val canonicalPromotedByThread =
      threadedPromoted
        .groupBy(_.threadId.get)
        .values
        .flatMap(_.sortBy(canonicalPromotedSortKey).headOption)
        .toList
        .sortBy(canonicalPromotedSortKey)
    val canonicalPromotedPlies = canonicalPromotedByThread.map(_.moment.ply).toSet
    val nonThreadedPromoted =
      promotedCandidates
        .filter(_.threadId.isEmpty)
        .sortBy(protectedCandidateSortKey)
    val duplicatePromoted =
      threadedPromoted
        .filterNot(candidate => canonicalPromotedPlies.contains(candidate.moment.ply))
        .sortBy(canonicalPromotedSortKey)
    val remainingProtected =
      candidates
        .filterNot(candidate => isSevereFailure(candidate.truthView) || isPromotedBestHold(candidate.truthView))
        .sortBy(protectedCandidateSortKey)

    (severeCandidates ++ canonicalPromotedByThread ++ nonThreadedPromoted ++ duplicatePromoted ++ remainingProtected)
      .map(_.moment)
      .distinctBy(_.ply)

  private def selectProtectedOverflowActiveNotes(
      protectedCandidates: List[GameChronicleMoment],
      baseProtectedSeedMoments: List[GameChronicleMoment],
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef],
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): List[GameChronicleMoment] =
    val basePlies = baseProtectedSeedMoments.map(_.ply).toSet
    val seededThreadIds =
      baseProtectedSeedMoments
        .flatMap(moment => threadRefsByPly.get(moment.ply).map(_.threadId))
        .filter(_.nonEmpty)
        .toSet
    val seededChainKeys =
      baseProtectedSeedMoments
        .flatMap(moment => truthSelectionView(moment, truthContractsByPly).chainKey.filter(_.nonEmpty))
        .toSet
    val (selectedMoments, _, _) =
      protectedCandidates
        .filterNot(moment => basePlies.contains(moment.ply))
        .foldLeft((List.empty[GameChronicleMoment], seededThreadIds, seededChainKeys)) {
        case ((accepted, seenThreadIds, seenChainKeys), moment) =>
          val truthView = truthSelectionView(moment, truthContractsByPly)
          val threadIdOpt = surfacedThreadId(moment, threadRefsByPly)
          val chainKeyOpt = truthView.chainKey.filter(_.nonEmpty)
          val threadSeen = threadIdOpt.exists(seenThreadIds.contains)
          val chainSeen = chainKeyOpt.exists(seenChainKeys.contains)
          if !isOverflowProtectedFamily(truthView) then
            (accepted, seenThreadIds, seenChainKeys)
          else if threadSeen || chainSeen then
            (accepted, seenThreadIds, seenChainKeys)
          else
            (
              accepted :+ moment,
              threadIdOpt.fold(seenThreadIds)(seenThreadIds + _),
              chainKeyOpt.fold(seenChainKeys)(seenChainKeys + _)
            )
      }
    selectedMoments

  private def protectedMomentPriority(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[(Int, Int, Int, Int)] =
    val truthView = truthSelectionView(moment, truthContractsByPly)
    if isBlunder(truthView) then Some((0, protectedSeverityScore(moment), 0, moment.ply))
    else if isMissedWin(truthView) then Some((1, protectedSeverityScore(moment), 0, moment.ply))
    else if isPromotedBestHold(truthView) then Some((2, protectedSeverityScore(moment), 1, moment.ply))
    else if isVerifiedExemplar(truthView) then Some((3, protectedSeverityScore(moment), 0, moment.ply))
    else if isProvisionalExemplar(truthView) then Some((4, protectedSeverityScore(moment), 0, moment.ply))
    else if isCommitmentOwner(truthView) then Some((5, protectedSeverityScore(moment), 0, moment.ply))
    else if isConversionOwner(truthView) then Some((6, protectedSeverityScore(moment), 0, moment.ply))
    else None

  private def protectedSeverityScore(
      moment: GameChronicleMoment
  ): Int =
    -math.abs(moment.cpAfter - moment.cpBefore)

  private def surfacedThreadId(
      moment: GameChronicleMoment,
      threadRefsByPly: Map[Int, ActiveStrategicThreadRef]
  ): Option[String] =
    threadRefsByPly.get(moment.ply).map(_.threadId)
      .filter(_.nonEmpty)

  private def protectedCandidateSortKey(
      candidate: ProtectedMomentCandidate
  ): (Int, Int, Int, Int, Int) =
    val (priority, severity, secondary, tiebreak) = candidate.priority
    (priority, severity, secondary, tiebreak, candidate.moment.ply)

  private def canonicalPromotedSortKey(
      candidate: ProtectedMomentCandidate
  ): (Int, Int, Int, Int, Int, Int) =
    val (priority, severity, secondary, tiebreak) = candidate.priority
    (
      if candidate.threadRepresentativePromoted then 0 else 1,
      priority,
      severity,
      secondary,
      tiebreak,
      candidate.moment.ply
    )
