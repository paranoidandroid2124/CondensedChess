package lila.llm.analysis

import lila.llm.*

object ActiveBranchDossierBuilder:

  def build(
      moment: GameNarrativeMoment,
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef]
  ): Option[ActiveBranchDossier] =
    val digest = moment.signalDigest
    val comparison = digest.flatMap(_.decisionComparison)
    val dominantLens = lensFor(moment, digest)
    val chosenBranch = chosenBranchLabel(moment, digest, comparison, dominantLens)
    val engineBranch = engineBranchLabel(digest, comparison)
    val deferredBranch = deferredBranchLabel(digest, comparison)
    val routeCue = selectRouteCue(digest, routeRefs)
    val moveCue = selectMoveCue(comparison, moveRefs)
    val whyChosen = whyChosenText(digest, routeCue)
    val whyDeferred = whyDeferredText(digest, comparison)
    val opponentResource = opponentResourceText(digest, whyDeferred)
    val evidenceCue = evidenceText(moment, comparison)
    val continuationFocus = continuationFocusText(moment.strategyPack)
    val practicalRisk = practicalRiskText(digest)
    val gapCp = comparison.flatMap(_.cpLossVsChosen).orElse(moment.topEngineMove.flatMap(_.cpLossVsPlayed))

    Option.when(
      chosenBranch.nonEmpty ||
        engineBranch.nonEmpty ||
        deferredBranch.nonEmpty ||
        routeCue.nonEmpty ||
        moveCue.nonEmpty ||
        evidenceCue.nonEmpty ||
        continuationFocus.nonEmpty ||
        practicalRisk.nonEmpty
    )(
      ActiveBranchDossier(
        dominantLens = dominantLens,
        chosenBranchLabel = chosenBranch,
        engineBranchLabel = engineBranch,
        deferredBranchLabel = deferredBranch,
        whyChosen = whyChosen,
        whyDeferred = whyDeferred,
        opponentResource = opponentResource,
        routeCue = routeCue,
        moveCue = moveCue,
        evidenceCue = evidenceCue,
        continuationFocus = continuationFocus,
        practicalRisk = practicalRisk,
        comparisonGapCp = gapCp
      )
    )

  private def lensFor(moment: GameNarrativeMoment, digest: Option[NarrativeSignalDigest]): String =
    val momentType = normalize(moment.momentType)
    val classification = normalize(moment.moveClassification.getOrElse(""))
    if isTactical(momentType) || isTactical(classification) then "tactical"
    else if digest.exists(d => d.structureProfile.exists(_.trim.nonEmpty) && d.deploymentPiece.exists(_.trim.nonEmpty)) then
      "structure"
    else if digest.exists(_.opening.exists(_.trim.nonEmpty)) then "opening"
    else if digest.exists(d => d.compensation.exists(_.trim.nonEmpty) || d.investedMaterial.exists(_ > 0)) then "compensation"
    else if digest.exists(d => d.prophylaxisPlan.exists(_.trim.nonEmpty) || d.prophylaxisThreat.exists(_.trim.nonEmpty)) then
      "prophylaxis"
    else if digest.exists(d => d.decisionComparison.isDefined || d.decision.exists(_.trim.nonEmpty)) then "decision"
    else if digest.exists(_.practicalVerdict.exists(_.trim.nonEmpty)) then "practical"
    else "strategic"

  private def chosenBranchLabel(
      moment: GameNarrativeMoment,
      digest: Option[NarrativeSignalDigest],
      comparison: Option[DecisionComparisonDigest],
      dominantLens: String
  ): String =
    val chosenMove = comparison.flatMap(_.chosenMove).flatMap(normalized)
    dominantLens match
      case "tactical" =>
        val head =
          moment.moveClassification.flatMap(normalized)
            .orElse(normalized(moment.momentType))
            .getOrElse("critical tactical turn")
        val movePart = chosenMove.map(move => s" via $move").getOrElse("")
        s"$head$movePart"
      case "structure" =>
        val structure = digest.flatMap(_.structureProfile).flatMap(normalized).getOrElse("structural branch")
        val plan =
          digest.flatMap(_.deploymentPurpose).flatMap(normalized)
            .orElse(digest.flatMap(_.structuralCue).flatMap(normalized))
            .getOrElse("long-term deployment")
        s"$structure -> $plan"
      case "opening" =>
        val opening = digest.flatMap(_.opening).flatMap(normalized).getOrElse("opening branch")
        val branch =
          digest.flatMap(_.decisionComparison).flatMap(_.deferredReason).flatMap(normalized)
            .orElse(digest.flatMap(_.latentPlan).flatMap(normalized))
            .orElse(digest.flatMap(_.structuralCue).flatMap(normalized))
            .getOrElse("main branch")
        s"$opening -> $branch"
      case "compensation" =>
        val plan = digest.flatMap(_.compensation).flatMap(normalized).getOrElse("compensation plan")
        s"compensation -> $plan"
      case "prophylaxis" =>
        val target =
          digest.flatMap(_.prophylaxisPlan).flatMap(normalized)
            .orElse(digest.flatMap(_.prophylaxisThreat).flatMap(normalized))
            .getOrElse("counterplay control")
        s"prophylaxis -> $target"
      case "decision" =>
        val movePart = chosenMove.getOrElse("the chosen move")
        val reason =
          digest.flatMap(_.decision).flatMap(normalized)
            .orElse(digest.flatMap(_.decisionComparison).flatMap(_.deferredReason).flatMap(normalized))
            .getOrElse("the preferred branch")
        s"$movePart -> $reason"
      case "practical" =>
        val verdict = digest.flatMap(_.practicalVerdict).flatMap(normalized).getOrElse("practical branch")
        s"practical branch -> $verdict"
      case _ =>
        chosenMove
          .map(move => s"chosen branch -> $move")
          .orElse(digest.flatMap(_.decision).flatMap(normalized).map(reason => s"strategic branch -> $reason"))
          .orElse(digest.flatMap(_.structuralCue).flatMap(normalized).map(reason => s"strategic branch -> $reason"))
          .getOrElse("strategic branch")

  private def engineBranchLabel(
      digest: Option[NarrativeSignalDigest],
      comparison: Option[DecisionComparisonDigest]
  ): Option[String] =
    comparison.flatMap(_.engineBestMove).flatMap(normalized).filterNot(move =>
      comparison.flatMap(_.chosenMove).exists(equalToken(_, move))
    ).map { move =>
      val suffix =
        digest.flatMap(_.deploymentPurpose).flatMap(normalized)
          .orElse(comparison.flatMap(_.deferredReason).flatMap(normalized))
          .map(text => s" -> $text")
          .getOrElse("")
      s"engine $move$suffix"
    }

  private def deferredBranchLabel(
      digest: Option[NarrativeSignalDigest],
      comparison: Option[DecisionComparisonDigest]
  ): Option[String] =
    comparison.flatMap(_.deferredMove).flatMap(normalized).map { move =>
      val prefix = if comparison.exists(_.practicalAlternative) then "practical" else "deferred"
      val suffix =
        comparison.flatMap(_.deferredReason).flatMap(normalized)
          .orElse(digest.flatMap(_.latentReason).flatMap(normalized))
          .map(text => s" -> $text")
          .getOrElse("")
      s"$prefix $move$suffix"
    }

  private def whyChosenText(
      digest: Option[NarrativeSignalDigest],
      routeCue: Option[ActiveBranchRouteCue]
  ): Option[String] =
    val fromDigest =
      digest.flatMap(_.deploymentContribution).flatMap(normalized)
        .orElse(digest.flatMap(_.decision).flatMap(normalized))
        .orElse(routeCue.flatMap(cue => normalized(s"${cue.piece} ${cue.route.mkString("-")} for ${cue.purpose}")))
    fromDigest.map(UserFacingSignalSanitizer.sanitize)

  private def whyDeferredText(
      digest: Option[NarrativeSignalDigest],
      comparison: Option[DecisionComparisonDigest]
  ): Option[String] =
    comparison.flatMap(_.deferredReason).flatMap(normalized)
      .orElse(digest.flatMap(_.latentReason).flatMap(normalized))
      .map(UserFacingSignalSanitizer.sanitize)

  private def opponentResourceText(
      digest: Option[NarrativeSignalDigest],
      whyDeferred: Option[String]
  ): Option[String] =
    digest.flatMap(_.opponentPlan).flatMap(normalized)
      .orElse(digest.flatMap(_.prophylaxisThreat).flatMap(normalized))
      .orElse {
        whyDeferred.filter(text => normalize(text).contains("counterplay") || normalize(text).contains("initiative"))
      }
      .map(UserFacingSignalSanitizer.sanitize)

  private def evidenceText(
      moment: GameNarrativeMoment,
      comparison: Option[DecisionComparisonDigest]
  ): Option[String] =
    comparison.flatMap(_.evidence).flatMap(normalized)
      .orElse {
        moment.authorEvidence.headOption.flatMap { summary =>
          summary.branches.headOption.flatMap { line =>
            val key = normalized(line.keyMove)
            val preview = normalized(line.line)
            val cp = line.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
            (key, preview) match
              case (Some(move), Some(text)) => Some(s"$move: $text$cp")
              case (Some(move), None)       => Some(move)
              case (None, Some(text))       => Some(text)
              case _                        => None
          }
        }
      }
      .orElse {
        moment.probeRequests.headOption.flatMap(req =>
          req.objective.flatMap(normalized)
            .orElse(req.planName.flatMap(normalized))
        )
      }
      .map(UserFacingSignalSanitizer.sanitize)

  private def continuationFocusText(strategyPack: Option[StrategyPack]): Option[String] =
    strategyPack.toList
      .flatMap(_.longTermFocus)
      .map(UserFacingSignalSanitizer.sanitize)
      .find { line =>
        val low = normalize(line)
        low.nonEmpty &&
        !low.startsWith("dominant thesis:") &&
        !low.startsWith("decision compare:") &&
        !low.startsWith("engine best:")
      }

  private def practicalRiskText(digest: Option[NarrativeSignalDigest]): Option[String] =
    digest.flatMap(_.practicalVerdict).flatMap(normalized)
      .orElse(digest.flatMap(_.compensation).flatMap(normalized))
      .map(UserFacingSignalSanitizer.sanitize)

  private def selectRouteCue(
      digest: Option[NarrativeSignalDigest],
      routeRefs: List[ActiveStrategicRouteRef]
  ): Option[ActiveBranchRouteCue] =
    val digestRoute = digest.map(_.deploymentRoute.map(normalize).filter(_.nonEmpty))
    val digestPurpose = digest.flatMap(_.deploymentPurpose).flatMap(normalized)
    routeRefs
      .sortBy { route =>
        val routeSquares = route.route.map(normalize).filter(_.nonEmpty)
        val routeMatch =
          Option.when(
            digestRoute.exists { digestSquares =>
              digestSquares.nonEmpty && digestSquares.lastOption == routeSquares.lastOption
            }
          )(0).getOrElse(1)
        val purposeMatch =
          Option.when(digestPurpose.exists(purpose => normalize(route.purpose).contains(purpose) || purpose.contains(normalize(route.purpose))))(0)
            .getOrElse(1)
        (routeMatch, purposeMatch, -(route.confidence * 1000).round.toInt)
      }
      .headOption
      .map { route =>
        ActiveBranchRouteCue(
          routeId = route.routeId,
          piece = route.piece,
          route = route.route,
          purpose = UserFacingSignalSanitizer.sanitize(route.purpose),
          confidence = route.confidence
        )
      }

  private def selectMoveCue(
      comparison: Option[DecisionComparisonDigest],
      moveRefs: List[ActiveStrategicMoveRef]
  ): Option[ActiveBranchMoveCue] =
    val engineBest = comparison.flatMap(_.engineBestMove).flatMap(normalized)
    val preferred =
      moveRefs.find(ref => engineBest.exists(best => ref.san.exists(equalToken(_, best)) || equalToken(ref.uci, best)))
        .orElse(moveRefs.headOption)
    preferred.map { ref =>
      ActiveBranchMoveCue(
        label = ref.label,
        uci = ref.uci,
        san = ref.san.flatMap(normalized),
        source = ref.source
      )
    }

  private def isTactical(text: String): Boolean =
    List("blunder", "mistake", "missedwin", "missed win", "inaccuracy", "mate", "tactic").exists(text.contains)

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def equalToken(left: String, right: String): Boolean =
    normalize(left).replaceAll("""[+#?!]+$""", "") == normalize(right).replaceAll("""[+#?!]+$""", "")

  private def formatCp(cp: Int): String =
    if cp > 0 then f"+${cp.toDouble / 100}%.2f" else f"${cp.toDouble / 100}%.2f"
