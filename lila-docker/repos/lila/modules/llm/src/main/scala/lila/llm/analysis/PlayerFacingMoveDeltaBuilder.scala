package lila.llm.analysis

import lila.llm.*
import lila.llm.model.strategic.VariationLine

private[llm] final case class PlayerFacingMoveDeltaClaim(
    deltaClass: PlayerFacingMoveDeltaClass,
    anchorText: String,
    reasonText: Option[String],
    routeCue: Option[ActiveBranchRouteCue] = None,
    moveCue: Option[ActiveBranchMoveCue] = None,
    directionalTargets: List[StrategyDirectionalTarget] = Nil,
    evidenceLines: List[String] = Nil,
    sourceKind: String
)

private[llm] final case class PlayerFacingMoveDeltaBundle(
    claims: List[PlayerFacingMoveDeltaClaim],
    visibleRouteRefs: List[ActiveStrategicRouteRef],
    visibleMoveRefs: List[ActiveStrategicMoveRef],
    visibleDirectionalTargets: List[StrategyDirectionalTarget],
    tacticalLead: Option[String],
    tacticalEvidence: Option[String]
):
  def hasVisibleSupport: Boolean =
    claims.nonEmpty ||
      visibleRouteRefs.nonEmpty ||
      visibleMoveRefs.nonEmpty ||
      visibleDirectionalTargets.nonEmpty ||
      tacticalLead.nonEmpty ||
      tacticalEvidence.nonEmpty

private[llm] object PlayerFacingMoveDeltaBuilder:

  def build(
      moment: GameChronicleMoment,
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef]
  ): PlayerFacingMoveDeltaBundle =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    PlayerFacingTruthModePolicy.classify(moment) match
      case PlayerFacingTruthMode.Minimal =>
        PlayerFacingMoveDeltaBundle(Nil, Nil, Nil, Nil, None, None)
      case PlayerFacingTruthMode.Tactical =>
        PlayerFacingMoveDeltaBundle(
          claims = Nil,
          visibleRouteRefs = Nil,
          visibleMoveRefs = selectVisibleMoveRefs(moment, moveRefs),
          visibleDirectionalTargets = Nil,
          tacticalLead = PlayerFacingTruthModePolicy.tacticalLeadSentence(moment),
          tacticalEvidence = tacticalEvidenceLine(moment)
        )
      case PlayerFacingTruthMode.Strategic =>
        strategicBundle(moment, surface, routeRefs, moveRefs)

  private def strategicBundle(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot,
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef]
  ): PlayerFacingMoveDeltaBundle =
    PlayerFacingTruthModePolicy.activeMoveDeltaEvidence(moment) match
      case None =>
        PlayerFacingMoveDeltaBundle(Nil, Nil, Nil, Nil, None, None)
      case Some(deltaEvidence) =>
        val routeCue = selectRouteCue(surface, routeRefs)
        val moveCue = selectMoveCue(surface, moveRefs, moment.signalDigest.flatMap(_.decisionComparison))
        val targets = selectDirectionalTargets(surface)
        val anchor = anchorTextFor(deltaEvidence, routeCue, moveCue, targets, surface)
        val reason = reasonTextFor(deltaEvidence, routeCue, moveCue, targets, moment, surface)
        val evidence = evidenceLinesFor(deltaEvidence, moment)
        if !passesActiveStrategicGate(moment, deltaEvidence, routeCue, moveCue, targets, evidence) then
          PlayerFacingMoveDeltaBundle(Nil, Nil, Nil, Nil, None, None)
        else
          val claim =
            PlayerFacingMoveDeltaClaim(
              deltaClass = deltaEvidence.deltaClass,
              anchorText = anchor,
              reasonText = reason,
              routeCue = routeCue,
              moveCue = moveCue,
              directionalTargets = targets,
              evidenceLines = evidence,
              sourceKind = sourceKind(routeCue, moveCue, targets)
            )

          PlayerFacingMoveDeltaBundle(
            claims = List(claim),
            visibleRouteRefs = routeCue.toList.flatMap(cue => visibleRouteRef(routeRefs, cue)),
            visibleMoveRefs = moveCue.toList.flatMap(cue => visibleMoveRef(moveRefs, cue)),
            visibleDirectionalTargets = targets,
            tacticalLead = None,
            tacticalEvidence = None
          )

  private def selectRouteCue(
      surface: StrategyPackSurface.Snapshot,
      routeRefs: List[ActiveStrategicRouteRef]
  ): Option[ActiveBranchRouteCue] =
    surface.topRoute.flatMap { topRoute =>
      routeRefs.find(ref =>
        normalize(ref.ownerSide) == normalize(topRoute.ownerSide) &&
          normalize(ref.piece) == normalize(topRoute.piece) &&
          ref.route.map(normalize) == topRoute.route.map(normalize) &&
          normalize(ref.purpose) == normalize(topRoute.purpose)
      ).map { ref =>
        ActiveBranchRouteCue(
          routeId = ref.routeId,
          ownerSide = ref.ownerSide,
          piece = ref.piece,
          route = ref.route,
          purpose = ref.purpose,
          strategicFit = ref.strategicFit,
          tacticalSafety = ref.tacticalSafety,
          surfaceConfidence = ref.surfaceConfidence,
          surfaceMode = ref.surfaceMode
        )
      }
    }

  private def selectMoveCue(
      surface: StrategyPackSurface.Snapshot,
      moveRefs: List[ActiveStrategicMoveRef],
      comparison: Option[DecisionComparisonDigest]
  ): Option[ActiveBranchMoveCue] =
    val topMoveRef = surface.topMoveRef
    val preferredMove =
      topMoveRef.flatMap { top =>
        moveRefs.find(ref =>
          normalize(ref.label).contains(normalize(top.idea)) ||
            normalize(ref.uci).endsWith(normalize(top.target)) ||
            ref.san.exists(san => normalize(san).contains(normalize(top.target)))
        )
      }.orElse {
        val comparisonMoves =
          comparison.toList.flatMap(cmp => List(cmp.chosenMove, cmp.engineBestMove).flatten).map(normalizeSan)
        moveRefs.find(ref =>
          ref.san.exists(san => comparisonMoves.contains(normalizeSan(san))) ||
            comparisonMoves.contains(normalize(ref.uci))
        )
      }.orElse(moveRefs.headOption)

    preferredMove.map(ref =>
      ActiveBranchMoveCue(
        label = ref.label,
        uci = ref.uci,
        san = ref.san,
        source = ref.source
      )
    )

  private def selectDirectionalTargets(surface: StrategyPackSurface.Snapshot): List[StrategyDirectionalTarget] =
    surface.topDirectionalTarget.toList.filter(target =>
      target.targetSquare.trim.nonEmpty &&
        target.evidence.nonEmpty &&
        target.readiness != DirectionalTargetReadiness.Premature &&
        target.strategicReasons.nonEmpty
    )

  private def anchorTextFor(
      deltaEvidence: PlayerFacingMoveDeltaEvidence,
      routeCue: Option[ActiveBranchRouteCue],
      moveCue: Option[ActiveBranchMoveCue],
      targets: List[StrategyDirectionalTarget],
      surface: StrategyPackSurface.Snapshot
  ): String =
    val targetAnchor =
      targets.headOption.map(_.targetSquare)
        .orElse(routeCue.flatMap(_.route.lastOption))
        .orElse(moveCue.flatMap(_.san))
        .orElse(surface.topMoveRef.map(_.target))
        .orElse(surface.topDirectionalTarget.map(_.targetSquare))
        .orElse(deltaEvidence.anchorTerms.find(term => term.matches(".*[a-h][1-8].*")))
        .orElse(deltaEvidence.anchorTerms.headOption)
        .getOrElse("the current setup")
    UserFacingSignalSanitizer.sanitize(targetAnchor)

  private def reasonTextFor(
      deltaEvidence: PlayerFacingMoveDeltaEvidence,
      routeCue: Option[ActiveBranchRouteCue],
      moveCue: Option[ActiveBranchMoveCue],
      targets: List[StrategyDirectionalTarget],
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    val deltaLead =
      deltaEvidence.deltaClass match
        case PlayerFacingMoveDeltaClass.NewAccess =>
          routeCue.flatMap(_.route.lastOption).map(square => s"This move opens access to $square.")
            .orElse(moveCue.flatMap(_.san).map(san => s"This move opens a new route through $san."))
        case PlayerFacingMoveDeltaClass.PressureIncrease =>
          targets.headOption.map(target => s"This move increases pressure on ${target.targetSquare}.")
            .orElse(routeCue.flatMap(_.route.lastOption).map(square => s"This move increases pressure around $square."))
        case PlayerFacingMoveDeltaClass.ExchangeForcing =>
          moveCue.flatMap(_.san).map(san => s"This move makes the exchange around $san more forcing.")
            .orElse(Some("This move makes the simplifying sequence more forcing."))
        case PlayerFacingMoveDeltaClass.CounterplayReduction =>
          targets.headOption.map(target => s"This move cuts down the counterplay around ${target.targetSquare}.")
            .orElse(Some("This move cuts down the opponent's counterplay."))
        case PlayerFacingMoveDeltaClass.ResourceRemoval =>
          moveCue.flatMap(_.san).map(san => s"This move strips away a defensive resource after $san.")
            .orElse(Some("This move removes a key defensive resource."))
        case PlayerFacingMoveDeltaClass.PlanAdvance =>
          routeCue.flatMap(_.route.lastOption).map(square => s"This move advances the plan toward $square.")
            .orElse(targets.headOption.map(target => s"This move advances the plan toward ${target.targetSquare}."))
            .orElse(Some("This move advances the plan by one concrete step."))

    val evidenceBackedReason =
      moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.evidence).flatMap(clean)
        .orElse(targets.headOption.flatMap(_.strategicReasons.headOption).flatMap(clean))
        .orElse(routeCue.map(_.purpose).flatMap(clean))
        .orElse(surface.topMoveRef.map(_.idea).flatMap(clean))

    deltaLead.orElse(evidenceBackedReason).map(UserFacingSignalSanitizer.sanitize)

  private def evidenceLinesFor(
      deltaEvidence: PlayerFacingMoveDeltaEvidence,
      moment: GameChronicleMoment
  ): List[String] =
    val comparisonEvidence = moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.evidence).flatMap(clean).toList
    val authorEvidence =
      moment.authorEvidence.headOption.toList.flatMap { summary =>
        summary.branches.headOption.flatMap { branch =>
          val key = clean(branch.keyMove)
          val line = clean(branch.line)
          (key, line) match
            case (Some(move), Some(text)) => Some(s"$move: $text")
            case (Some(move), None)       => Some(move)
            case (None, Some(text))       => Some(text)
            case _                        => None
        }
      }
    val variationEvidence =
      Option.when(deltaEvidence.deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing)(variationPreview(moment.variations))
        .flatten
        .toList
    (comparisonEvidence ++ authorEvidence ++ variationEvidence).distinct.take(1)

  private def selectVisibleMoveRefs(
      moment: GameChronicleMoment,
      moveRefs: List[ActiveStrategicMoveRef]
  ): List[ActiveStrategicMoveRef] =
    val tokens =
      moment.topEngineMove.toList.flatMap(top => List(top.san, Some(top.uci)).flatten.map(normalizeSan)) ++
        moment.variations.headOption.toList.flatMap(_.moves.take(1).map(normalizeSan))
    moveRefs.filter(ref =>
      ref.san.exists(san => tokens.contains(normalizeSan(san))) || tokens.contains(normalize(ref.uci))
    ).take(1)

  private def tacticalEvidenceLine(moment: GameChronicleMoment): Option[String] =
    variationPreview(moment.variations)
      .map(text => s"The line begins $text.")
      .flatMap(clean)

  private def passesActiveStrategicGate(
      moment: GameChronicleMoment,
      deltaEvidence: PlayerFacingMoveDeltaEvidence,
      routeCue: Option[ActiveBranchRouteCue],
      moveCue: Option[ActiveBranchMoveCue],
      targets: List[StrategyDirectionalTarget],
      evidence: List[String]
  ): Boolean =
    val deploymentContribution = moment.signalDigest.flatMap(_.deploymentContribution).flatMap(clean)
    val comparisonEvidence = moment.signalDigest.flatMap(_.decisionComparison).flatMap(_.evidence).flatMap(clean)
    val exactRoute =
      routeCue.exists(_.surfaceMode == RouteSurfaceMode.Exact) ||
        moment.signalDigest.flatMap(_.deploymentSurfaceMode).exists(mode => normalize(mode) == "exact")
    val anchorBacked =
      mentionsAnchor(deploymentContribution.toList ++ comparisonEvidence.toList ++ evidence, routeCue, moveCue, targets)
    val counterplayDrop = moment.signalDigest.flatMap(_.counterplayScoreDrop).exists(_ > 0)
    val captureMove = moveCue.flatMap(_.san).exists(_.contains("x"))

    deltaEvidence.deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess | PlayerFacingMoveDeltaClass.PlanAdvance =>
        exactRoute && deploymentContribution.nonEmpty && anchorBacked
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        exactRoute &&
          deploymentContribution.exists(text =>
            containsAny(normalize(text), List("pressure", "target", "attack", "initiative", "clamp"))
          ) &&
          anchorBacked
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        counterplayDrop && anchorBacked
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        captureMove &&
          evidenceMentionsFamily(deploymentContribution, comparisonEvidence, List("resource", "defend", "guard", "escape", "counterplay", "remove")) &&
          anchorBacked
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        captureMove &&
          evidenceMentionsFamily(deploymentContribution, comparisonEvidence, List("exchange", "trade", "simplif", "capture")) &&
          anchorBacked

  private def evidenceMentionsFamily(
      deploymentContribution: Option[String],
      comparisonEvidence: Option[String],
      words: List[String]
  ): Boolean =
    (deploymentContribution.toList ++ comparisonEvidence.toList).exists(text => containsAny(normalize(text), words))

  private def mentionsAnchor(
      texts: List[String],
      routeCue: Option[ActiveBranchRouteCue],
      moveCue: Option[ActiveBranchMoveCue],
      targets: List[StrategyDirectionalTarget]
  ): Boolean =
    val anchors =
      (routeCue.toList.flatMap(_.route.lastOption) ++
        moveCue.toList.flatMap(_.san) ++
        targets.map(_.targetSquare)).map(normalize).filter(_.nonEmpty)
    anchors.nonEmpty && texts.exists { text =>
      val low = normalize(text)
      anchors.exists(anchor => low.contains(anchor))
    }

  private def sourceKind(
      routeCue: Option[ActiveBranchRouteCue],
      moveCue: Option[ActiveBranchMoveCue],
      targets: List[StrategyDirectionalTarget]
  ): String =
    if routeCue.nonEmpty then "route"
    else if targets.nonEmpty then "target"
    else if moveCue.nonEmpty then "move_ref"
    else "evidence"

  private def visibleRouteRef(
      routeRefs: List[ActiveStrategicRouteRef],
      cue: ActiveBranchRouteCue
  ): Option[ActiveStrategicRouteRef] =
    routeRefs.find(ref =>
      normalize(ref.routeId) == normalize(cue.routeId) ||
        (
          normalize(ref.ownerSide) == normalize(cue.ownerSide) &&
            normalize(ref.piece) == normalize(cue.piece) &&
            ref.route.map(normalize) == cue.route.map(normalize)
        )
    )

  private def visibleMoveRef(
      moveRefs: List[ActiveStrategicMoveRef],
      cue: ActiveBranchMoveCue
  ): Option[ActiveStrategicMoveRef] =
    moveRefs.find(ref =>
      normalize(ref.uci) == normalize(cue.uci) ||
        ref.san.exists(san => cue.san.exists(cueSan => normalizeSan(san) == normalizeSan(cueSan)))
    )

  private def variationPreview(variations: List[VariationLine]): Option[String] =
    variations.headOption.flatMap { line =>
      val moves =
        if line.parsedMoves.nonEmpty then line.parsedMoves.take(3).map(_.san.trim).filter(_.nonEmpty)
        else line.moves.take(3).map(_.trim).filter(_.nonEmpty)
      Option.when(moves.nonEmpty)(moves.mkString(" "))
    }

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).map(UserFacingSignalSanitizer.sanitize)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def normalizeSan(raw: String): String =
    normalize(raw).replaceAll("""[!?+#]+$""", "")

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)
