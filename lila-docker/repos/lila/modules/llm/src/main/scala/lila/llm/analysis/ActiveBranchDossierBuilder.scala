package lila.llm.analysis

import lila.llm.*

object ActiveBranchDossierBuilder:

  def build(
      moment: GameChronicleMoment,
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      threadRef: Option[ActiveStrategicThreadRef] = None,
      thread: Option[ActiveStrategicThread] = None,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[ActiveBranchDossier] =
    val comparison = moment.signalDigest.flatMap(_.decisionComparison)
    val deltaBundle = PlayerFacingMoveDeltaBuilder.build(moment, routeRefs, moveRefs)
    if !deltaBundle.hasVisibleSupport then None
    else
      val claim = deltaBundle.claims.headOption
      val dominantLens =
        if deltaBundle.tacticalLead.nonEmpty then "tactical"
        else claim.map(_.deltaClass.toString.toLowerCase).getOrElse(lensFor(moment, truthContract))
      val chosenBranch = chosenBranchLabel(moment, claim, deltaBundle.tacticalLead)
      val routeCue = claim.flatMap(_.routeCue)
      val moveCue = claim.flatMap(_.moveCue).orElse(deltaBundle.visibleMoveRefs.headOption.map { ref =>
        ActiveBranchMoveCue(label = ref.label, uci = ref.uci, san = ref.san, source = ref.source)
      })
      val whyChosen =
        deltaBundle.tacticalLead
          .orElse(claim.flatMap(_.reasonText))
      val evidenceCue =
        deltaBundle.tacticalEvidence
          .orElse(claim.flatMap(_.evidenceLines.headOption))
      val practicalRisk =
        Option.when(deltaBundle.tacticalLead.nonEmpty)(evidenceCue).flatten
      val gapCp = comparison.flatMap(_.cpLossVsChosen).orElse(moment.topEngineMove.flatMap(_.cpLossVsPlayed))

      Some(
        ActiveBranchDossier(
          dominantLens = dominantLens,
          chosenBranchLabel = chosenBranch,
          engineBranchLabel = None,
          deferredBranchLabel = None,
          whyChosen = whyChosen,
          whyDeferred = None,
          opponentResource =
            claim
              .filter(claim =>
                claim.deltaClass == PlayerFacingMoveDeltaClass.CounterplayReduction ||
                  claim.deltaClass == PlayerFacingMoveDeltaClass.ResourceRemoval
              )
              .flatMap(_.reasonText),
          routeCue = routeCue,
          moveCue = moveCue,
          evidenceCue = evidenceCue,
          continuationFocus = None,
          practicalRisk = practicalRisk,
          comparisonGapCp = gapCp,
          threadLabel = threadRef.map(_.themeLabel),
          threadStage = threadRef.map(_.stageLabel),
          threadSummary = None,
          threadOpponentCounterplan = None
        )
      )

  private def lensFor(
      moment: GameChronicleMoment,
      truthContract: Option[DecisiveTruthContract]
  ): String =
    MomentTruthSemantics.chronicle(moment, truthContract).canonicalLens

  private def chosenBranchLabel(
      moment: GameChronicleMoment,
      claim: Option[PlayerFacingMoveDeltaClaim],
      tacticalLead: Option[String]
  ): String =
    tacticalLead.map(_ =>
      moment.moveClassification.flatMap(normalized)
        .orElse(normalized(moment.momentType))
        .getOrElse("tactical turn")
    ).orElse {
      claim.map { deltaClaim =>
        s"${deltaLabel(deltaClaim.deltaClass)} -> ${UserFacingSignalSanitizer.sanitize(deltaClaim.anchorText)}"
      }
    }.getOrElse("active delta")

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def deltaLabel(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => "new access"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "pressure increase"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "exchange forcing"
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "counterplay reduction"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "resource removal"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "plan advance"
