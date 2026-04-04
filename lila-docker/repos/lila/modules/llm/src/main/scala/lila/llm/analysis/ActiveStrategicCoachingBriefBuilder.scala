package lila.llm.analysis

import _root_.chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.llm.*
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.model.*
import lila.llm.model.authoring.*
import scala.annotation.unused

private[llm] object ActiveStrategicCoachingBriefBuilder:

  final case class PlannerReplay(
      authorQuestions: List[AuthorQuestion],
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract] = None
  )

  final case class PlannerSurfaceSelection(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract] = None
  )

  final case class DeterministicSupportCandidate(
      source: String,
      rawText: Option[String],
      cleanedText: Option[String],
      droppedReasons: List[String] = Nil
  )

  final case class DeterministicComposeDebug(
      primaryKind: String,
      secondaryKind: Option[String],
      primaryClaimRaw: String,
      primaryClaim: Option[String],
      primaryEvidenceRaw: Option[String],
      supportCandidates: List[DeterministicSupportCandidate],
      selectedSupportSource: Option[String],
      selectedSupport: Option[String],
      sentences: List[String],
      minimumSentences: Int,
      validatorInput: Option[String],
      failureReasons: List[String]
  ):
    def result: Option[String] = validatorInput

  final case class VisibleSideSurfaces(
      ideaRefs: List[ActiveStrategicIdeaRef],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      directionalTargets: List[StrategyDirectionalTarget],
      dossier: Option[ActiveBranchDossier]
  )

  final case class Brief(
      campaignRole: Option[String],
      primaryIdea: Option[String],
      compensationLead: Option[String],
      whyNow: Option[String],
      opponentReply: Option[String],
      executionHint: Option[String],
      longTermObjective: Option[String],
      keyTrigger: Option[String],
      compensationAnchor: Option[String],
      continuationFocus: Option[String],
      decisionSupports: List[String] = Nil
  ):
    def nonEmptySections: List[(String, String)] =
      List(
        "Campaign role" -> campaignRole,
        "Primary idea" -> primaryIdea,
        "Compensation lead" -> compensationLead,
        "Why now" -> whyNow,
        "Opponent reply to watch" -> opponentReply,
        "Execution hint" -> executionHint,
        "Long-term objective" -> longTermObjective,
        "Key trigger or failure mode" -> keyTrigger,
        "Compensation anchor" -> compensationAnchor,
        "Continuation focus" -> continuationFocus
      ).collect { case (label, Some(value)) if value.trim.nonEmpty => label -> value }

  final case class Coverage(
      hasDominantIdea: Boolean,
      hasForwardPlan: Boolean,
      hasConcreteAnchor: Boolean,
      hasGroundedSignal: Boolean,
      hasOpponentOrTrigger: Boolean,
      hasCampaignOwner: Boolean
  )

  private val PieceNames = Map(
    "P" -> "pawn",
    "N" -> "knight",
    "B" -> "bishop",
    "R" -> "rook",
    "Q" -> "queen",
    "K" -> "king"
  )

  private val ForwardCuePatterns = List(
    """\b(should|must|needs? to|want(?:s)? to|aim(?:s)? to|plan(?:s)? to|prepare(?:s)? to|look(?:s)? to|tries? to)\b""",
    """\b(can then|so that|before [^.!?]{0,48}\bcan\b|if [^.!?]{0,64}\bthen\b|once\b|next\b|follow(?:s)? with\b)\b""",
    """\b(reroute|reroutes|rerouting|expand|expands|expanding|clamp|clamps|clamping|targeting|pressing|challenge|challenges|challenging|consolidate|consolidates|consolidating|switch|switches|switching|convert|converts|converting|prevent|prevents|preventing|build(?:s|ing)? toward|head(?:s|ing)? toward)\b"""
  ).map(_.r)

  def selectPlannerSurface(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier],
      decisionFrame: CertifiedDecisionFrame,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[PlannerSurfaceSelection] =
    replayPlanner(moment, deltaBundle, dossier, decisionFrame, truthContract)
      .flatMap(selectPlannerSurface)

  def selectPlannerSurface(
      replay: PlannerReplay
  ): Option[PlannerSurfaceSelection] =
    selectActiveSurface(replay.rankedPlans, replay.inputs)
      .map(_.copy(truthContract = replay.truthContract))

  def replayPlanner(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier],
      decisionFrame: CertifiedDecisionFrame,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[PlannerReplay] =
    val authorEvidence = replayAuthorEvidence(moment.authorEvidence)
    val authorQuestions = replayAuthorQuestions(moment.authorQuestions, authorEvidence)
    Option.when(authorQuestions.nonEmpty) {
      val inputs = buildPlannerInputs(moment, deltaBundle, dossier, decisionFrame, authorEvidence)
      val rankedPlans =
        QuestionFirstCommentaryPlanner.plan(
          ply = moment.ply,
          authorQuestions = authorQuestions,
          inputs = inputs,
          truthContract = truthContract
        )
      PlannerReplay(
        authorQuestions = authorQuestions,
        inputs = inputs,
        rankedPlans = rankedPlans,
        truthContract = truthContract
      )
    }

  def visibleSideSurfaces(
      selection: PlannerSurfaceSelection,
      decisionFrame: CertifiedDecisionFrame,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier]
  ): VisibleSideSurfaces =
    val alignedIdeas =
      selection.primary.questionKind match
        case AuthorQuestionKind.WhatMattersHere =>
          decisionFrame.ideaRefs(max = 1)
        case AuthorQuestionKind.WhyThis =>
          decisionFrame.ideaRefs(max = 2)
        case AuthorQuestionKind.WhatChanged =>
          decisionFrame.ideaRefs(max = 1)
        case AuthorQuestionKind.WhyNow =>
          decisionFrame.ideaRefs(max = 1)
        case AuthorQuestionKind.WhatMustBeStopped =>
          Nil
        case AuthorQuestionKind.WhosePlanIsFaster =>
          Nil
    val alignedDossier = decisionFrame.alignedDossier(dossier)
    selection.primary.questionKind match
      case AuthorQuestionKind.WhatMattersHere =>
        VisibleSideSurfaces(
          ideaRefs = alignedIdeas,
          routeRefs = decisionFrame.alignedRouteRefs(deltaBundle.visibleRouteRefs),
          moveRefs = decisionFrame.alignedMoveRefs(deltaBundle.visibleMoveRefs),
          directionalTargets = decisionFrame.alignedTargets(deltaBundle.visibleDirectionalTargets),
          dossier = alignedDossier
        )
      case AuthorQuestionKind.WhyThis =>
        VisibleSideSurfaces(
          ideaRefs = alignedIdeas,
          routeRefs = decisionFrame.alignedRouteRefs(deltaBundle.visibleRouteRefs),
          moveRefs = decisionFrame.alignedMoveRefs(deltaBundle.visibleMoveRefs),
          directionalTargets = decisionFrame.alignedTargets(deltaBundle.visibleDirectionalTargets),
          dossier = alignedDossier
        )
      case AuthorQuestionKind.WhatChanged =>
        VisibleSideSurfaces(
          ideaRefs = alignedIdeas,
          routeRefs = decisionFrame.alignedRouteRefs(deltaBundle.visibleRouteRefs),
          moveRefs = decisionFrame.alignedMoveRefs(deltaBundle.visibleMoveRefs),
          directionalTargets = decisionFrame.alignedTargets(deltaBundle.visibleDirectionalTargets),
          dossier = alignedDossier
        )
      case AuthorQuestionKind.WhyNow =>
        VisibleSideSurfaces(
          ideaRefs = alignedIdeas,
          routeRefs = Nil,
          moveRefs = Nil,
          directionalTargets = Nil,
          dossier = alignedDossier.filter(d => d.practicalRisk.nonEmpty || d.whyChosen.nonEmpty || d.opponentResource.nonEmpty)
        )
      case AuthorQuestionKind.WhatMustBeStopped =>
        VisibleSideSurfaces(
          ideaRefs = Nil,
          routeRefs = Nil,
          moveRefs = Nil,
          directionalTargets = Nil,
          dossier = alignedDossier.filter(d => d.opponentResource.nonEmpty || d.practicalRisk.nonEmpty || d.whyChosen.nonEmpty)
        )
      case AuthorQuestionKind.WhosePlanIsFaster =>
        VisibleSideSurfaces(Nil, Nil, Nil, Nil, None)

  private def selectActiveSurface(
      rankedPlans: RankedQuestionPlans,
      inputs: QuestionPlannerInputs
  ): Option[PlannerSurfaceSelection] =
    rankedPlans.primary.flatMap { primary =>
      val secondary = rankedPlans.secondary
      val swapped =
        secondary.filter(candidate =>
          !replayClosedForActive(candidate, inputs) &&
            activePriority(candidate.questionKind) > activePriority(primary.questionKind) &&
            activeKindAllowed(candidate.questionKind) &&
            notWeaker(candidate, primary, inputs)
        )
      val selected =
        if replayClosedForActive(primary, inputs) then
          swapped.map(candidate => PlannerSurfaceSelection(candidate, None, inputs))
        else if !activeKindAllowed(primary.questionKind) then
          swapped.map(candidate => PlannerSurfaceSelection(candidate, Some(primary), inputs))
        else
          swapped
            .map(candidate => PlannerSurfaceSelection(candidate, Some(primary), inputs))
            .orElse(Some(PlannerSurfaceSelection(primary, secondary.filterNot(replayClosedForActive(_, inputs)), inputs)))
      selected.filter { selection =>
        activeKindAllowed(selection.primary.questionKind) &&
          !replayClosedForActive(selection.primary, inputs) &&
          (
            selection.primary.questionKind != AuthorQuestionKind.WhatChanged ||
              compactWhatChanged(selection.primary)
          )
      }
    }

  private def replayClosedForActive(
      plan: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Boolean =
    replayClosedNamedRouteNetwork(plan) ||
      replayClosedCounterplayRestraint(plan, inputs) ||
      replayClosedHalfOpenFilePressure(plan, inputs)

  private def replayClosedNamedRouteNetwork(
      plan: QuestionPlan
  ): Boolean =
    plan.ownerSource == NamedRouteNetworkBindCertification.OwnerSource ||
      plan.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource)

  private def replayClosedHalfOpenFilePressure(
      plan: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Boolean =
    inputs.mainBundle.flatMap(_.primaryClaim).exists { claim =>
      val sourceKinds =
        Set(claim.sourceKind, s"${claim.sourceKind}_line").filter(_.trim.nonEmpty)
      claim.packet.exists { packet =>
        packet.ownerFamily == "half_open_file_pressure" &&
          (
            NarrativeDedupCore.sameSemanticSentence(claim.claimText, plan.claim) ||
              plan.sourceKinds.exists(sourceKinds.contains)
          )
      }
    }

  private def replayClosedCounterplayRestraint(
      plan: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Boolean =
    inputs.mainBundle.flatMap(_.primaryClaim).exists { claim =>
      val sourceKinds =
        Set(claim.sourceKind, s"${claim.sourceKind}_line").filter(_.trim.nonEmpty)
      claim.packet.exists { packet =>
        packet.ownerFamily == "counterplay_restraint" &&
          (
            NarrativeDedupCore.sameSemanticSentence(claim.claimText, plan.claim) ||
              plan.sourceKinds.exists(sourceKinds.contains)
          )
      }
    }

  private def activePriority(kind: AuthorQuestionKind): Int =
    kind match
      case AuthorQuestionKind.WhatMattersHere    => 4
      case AuthorQuestionKind.WhyThis            => 4
      case AuthorQuestionKind.WhatMustBeStopped  => 3
      case AuthorQuestionKind.WhyNow             => 2
      case AuthorQuestionKind.WhatChanged        => 1
      case AuthorQuestionKind.WhosePlanIsFaster  => 0

  private def activeKindAllowed(kind: AuthorQuestionKind): Boolean =
    kind != AuthorQuestionKind.WhosePlanIsFaster

  private def compactWhatChanged(plan: QuestionPlan): Boolean =
    plan.contrast.nonEmpty || plan.consequence.nonEmpty || plan.evidence.nonEmpty

  private def notWeaker(
      candidate: QuestionPlan,
      current: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Boolean =
    strengthScore(candidate.strengthTier) >= strengthScore(current.strengthTier) &&
      fallbackScore(candidate.fallbackMode) >= fallbackScore(current.fallbackMode) &&
      claimOwnershipScore(candidate, inputs) >= claimOwnershipScore(current, inputs) &&
      evidenceScore(candidate.evidence) >= evidenceScore(current.evidence)

  private def strengthScore(tier: QuestionPlanStrengthTier): Int =
    tier match
      case QuestionPlanStrengthTier.Strong   => 3
      case QuestionPlanStrengthTier.Moderate => 2
      case QuestionPlanStrengthTier.Exact    => 1

  private def fallbackScore(mode: QuestionPlanFallbackMode): Int =
    mode match
      case QuestionPlanFallbackMode.PlannerOwned               => 4
      case QuestionPlanFallbackMode.DemotedToWhyThis          => 3
      case QuestionPlanFallbackMode.DemotedToWhatMustBeStopped => 3
      case QuestionPlanFallbackMode.FactualFallback            => 2
      case QuestionPlanFallbackMode.Suppressed                 => 1

  private def evidenceScore(evidence: Option[QuestionPlanEvidence]): Int =
    evidence match
      case Some(value) if value.text.linesIterator.count(_.trim.matches("""^[a-z]\)\s+.*""")) >= 2 => 3
      case Some(value) if value.branchScoped => 2
      case Some(_)                           => 1
      case None                              => 0

  private def claimOwnershipScore(
      plan: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Int =
    if plan.ownerFamily == OwnerFamily.OpeningRelation &&
      plan.ownerSource == "opening_relation_translator"
    then
      if plan.questionKind == AuthorQuestionKind.WhyThis then 5 else 4
    else if plan.ownerFamily == OwnerFamily.EndgameTransition &&
      plan.ownerSource == "endgame_transition_translator"
    then
      if plan.questionKind == AuthorQuestionKind.WhatChanged then 5 else 4
    else if inputs.mainBundle.flatMap(_.mainClaim).exists(claim => NarrativeDedupCore.sameSemanticSentence(claim.claimText, plan.claim)) then 4
    else if plan.questionKind == AuthorQuestionKind.WhatChanged &&
      plan.sourceKinds.exists(kind =>
        kind == "pv_delta" || kind == "move_delta" || kind == "prevented_plan" || kind == "decision_comparison"
      )
    then 4
    else if inputs.quietIntent.exists(intent => NarrativeDedupCore.sameSemanticSentence(intent.claimText, plan.claim)) then 3
    else if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
      plan.sourceKinds.exists(kind => kind == "threat" || kind == "prevented_plan")
    then 3
    else if plan.questionKind == AuthorQuestionKind.WhyNow &&
      plan.sourceKinds.exists(kind =>
        kind == "threat" || kind == "prevented_plan" || kind == "truth_contract" || kind == "decision_comparison"
      )
    then 4
    else 1

  private def buildPlannerInputs(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier],
      decisionFrame: CertifiedDecisionFrame,
      authorEvidence: List[QuestionEvidence]
  ): QuestionPlannerInputs =
    val decisionComparison = replayDecisionComparison(moment)
    val candidateEvidenceLines =
      (
        authorEvidence.flatMap(_.branches).flatMap(branchDisplayLine) ++
          deltaBundle.claims.flatMap(_.evidenceLines) ++
          deltaBundle.tacticalEvidence.toList ++
          dossier.flatMap(_.evidenceCue).toList ++
          decisionComparison.flatMap(_.evidence).toList
      ).flatMap(cleanStringSignal).distinct
    val mainBundle = replayMainBundle(moment, deltaBundle, candidateEvidenceLines)

    QuestionPlannerInputs(
      mainBundle = mainBundle,
      quietIntent = None,
      decisionFrame = decisionFrame,
      decisionComparison = decisionComparison,
      alternativeNarrative = replayAlternativeNarrative(decisionComparison),
      truthMode = PlayerFacingTruthModePolicy.classify(moment),
      preventedPlansNow = replayPreventedPlans(moment, dossier),
      pvDelta = replayPvDelta(deltaBundle, dossier),
      counterfactual = None,
      practicalAssessment = replayPracticalInfo(moment),
      opponentThreats = replayThreats(moment, dossier),
      forcingThreats = Nil,
      evidenceByQuestionId = authorEvidence.groupBy(_.questionId),
      candidateEvidenceLines = candidateEvidenceLines,
      evidenceBackedPlans = replayEvidenceBackedPlans(moment),
      opponentPlan = replayOpponentPlan(moment),
      factualFallback = None,
      openingRelationClaim =
        moment.signalDigest.flatMap(_.openingRelationClaim).flatMap(cleanStringSignal),
      endgameTransitionClaim =
        moment.signalDigest.flatMap(_.endgameTransitionClaim).flatMap(cleanStringSignal)
    )

  private def replayMainBundle(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      candidateEvidenceLines: List[String]
  ): Option[MainPathClaimBundle] =
    PlayerFacingTruthModePolicy.classify(moment) match
      case PlayerFacingTruthMode.Tactical =>
        val evidenceLines =
          (deltaBundle.tacticalEvidence.toList ++ candidateEvidenceLines).flatMap(cleanStringSignal).distinct.take(1)
        val mainClaim =
          deltaBundle.tacticalLead
            .orElse(PlayerFacingTruthModePolicy.tacticalLeadSentence(moment))
            .flatMap(cleanStringSignal)
            .map { lead =>
              MainPathScopedClaim(
                scope = PlayerFacingClaimScope.MoveLocal,
                mode = PlayerFacingTruthMode.Tactical,
                deltaClass = None,
                claimText = lead,
                anchorTerms = Nil,
                evidenceLines = evidenceLines,
                sourceKind = "active_tactical",
                tacticalOwnership = moment.moveClassification.flatMap(cleanStringSignal)
              )
            }
        val lineClaim =
          evidenceLines.headOption.map { line =>
            MainPathScopedClaim(
              scope = PlayerFacingClaimScope.LineScoped,
              mode = PlayerFacingTruthMode.Tactical,
              deltaClass = None,
              claimText = line,
              anchorTerms = Nil,
              evidenceLines = List(line),
              sourceKind = "active_tactical_line",
              tacticalOwnership = moment.moveClassification.flatMap(cleanStringSignal)
            )
          }
        mainClaim
          .map(main => MainPathClaimBundle(Some(main), lineClaim))
          .orElse(lineClaim.map(line => MainPathClaimBundle(None, Some(line))))
      case PlayerFacingTruthMode.Strategic =>
        deltaBundle.claims.headOption.flatMap { claim =>
          activeMainClaim(claim)
            .flatMap(cleanStringSignal)
            .map { claimText =>
              val mainClaim =
                MainPathScopedClaim(
                  scope = PlayerFacingClaimScope.MoveLocal,
                  mode = PlayerFacingTruthMode.Strategic,
                  deltaClass = Some(claim.deltaClass),
                  claimText = claimText,
                  anchorTerms = anchorTermsForClaim(claim),
                  evidenceLines = claim.evidenceLines.flatMap(cleanStringSignal).distinct.take(1),
                  sourceKind = claim.sourceKind,
                  tacticalOwnership = None
                )
              val lineClaim =
                mainClaim.evidenceLines.headOption.map { line =>
                  MainPathScopedClaim(
                    scope = PlayerFacingClaimScope.LineScoped,
                    mode = PlayerFacingTruthMode.Strategic,
                    deltaClass = Some(claim.deltaClass),
                    claimText = line,
                    anchorTerms = mainClaim.anchorTerms,
                    evidenceLines = List(line),
                    sourceKind = s"${claim.sourceKind}_line",
                    tacticalOwnership = None
                  )
                }
              MainPathClaimBundle(Some(mainClaim), lineClaim)
            }
        }
      case PlayerFacingTruthMode.Minimal =>
        None

  private def activeMainClaim(claim: PlayerFacingMoveDeltaClaim): Option[String] =
    claim.reasonText.orElse {
      cleanStringSignal(claim.anchorText).map { anchor =>
        claim.deltaClass match
          case PlayerFacingMoveDeltaClass.NewAccess =>
            s"This opens access to $anchor."
          case PlayerFacingMoveDeltaClass.PressureIncrease =>
            s"This increases pressure on $anchor."
          case PlayerFacingMoveDeltaClass.ExchangeForcing =>
            s"This makes the exchange around $anchor more forcing."
          case PlayerFacingMoveDeltaClass.CounterplayReduction =>
            s"This cuts down the counterplay around $anchor."
          case PlayerFacingMoveDeltaClass.ResourceRemoval =>
            s"This removes a defensive resource around $anchor."
          case PlayerFacingMoveDeltaClass.PlanAdvance =>
            s"This advances the plan toward $anchor."
      }
    }

  private def anchorTermsForClaim(claim: PlayerFacingMoveDeltaClaim): List[String] =
    (
      cleanStringSignal(claim.anchorText).toList ++
        claim.routeCue.toList.flatMap(_.route.lastOption).flatMap(cleanStringSignal) ++
        claim.moveCue.flatMap(_.san).flatMap(cleanStringSignal).toList ++
        claim.directionalTargets.flatMap(target => cleanStringSignal(target.targetSquare))
    ).distinct

  private def replayDecisionComparison(moment: GameChronicleMoment): Option[DecisionComparison] =
    val digest = moment.signalDigest.flatMap(_.decisionComparison)
    val topEngine = moment.topEngineMove
    val chosenMove = digest.flatMap(_.chosenMove).flatMap(cleanStringSignal)
    val engineBestMove =
      digest.flatMap(_.engineBestMove).flatMap(cleanStringSignal)
        .orElse(topEngine.flatMap(replayTopEngineMoveLabel(moment, _)))
    val cpLossVsChosen =
      replayDecisionComparisonLoss(
        digest.flatMap(_.cpLossVsChosen).filter(_ > 0),
        topEngine.flatMap(_.cpLossVsPlayed).filter(_ > 0)
      )
    val chosenMatchesBest =
      digest.flatMap(value => Option.when(value.chosenMatchesBest)(true))
        .orElse(
          for
            chosen <- chosenMove
            best <- engineBestMove
          yield sameReplayMoveToken(chosen, best)
        )
        .getOrElse(false)
    val deferredMove =
      digest.flatMap(_.deferredMove).flatMap(cleanStringSignal)
        .orElse(engineBestMove.filterNot(best => chosenMove.exists(chosen => sameReplayMoveToken(chosen, best))))
    val engineBestPv =
      digest.map(_.engineBestPv).filter(_.nonEmpty).getOrElse(topEngine.map(_.pv).getOrElse(Nil))
    val evidence =
      digest.flatMap(_.evidence).flatMap(cleanStringSignal)
        .orElse(replayTopEngineEvidence(moment, topEngine))

    Option.when(
      chosenMove.nonEmpty ||
        engineBestMove.nonEmpty ||
        cpLossVsChosen.nonEmpty ||
        deferredMove.nonEmpty ||
        digest.flatMap(_.deferredReason).flatMap(cleanStringSignal).nonEmpty ||
        evidence.nonEmpty
    ) {
      DecisionComparison(
        chosenMove = chosenMove,
        engineBestMove = engineBestMove,
        engineBestScoreCp = digest.flatMap(_.engineBestScoreCp).orElse(topEngine.flatMap(_.cpAfterAlt)),
        engineBestPv = engineBestPv,
        cpLossVsChosen = cpLossVsChosen,
        deferredMove = deferredMove,
        deferredReason = digest.flatMap(_.deferredReason).flatMap(cleanStringSignal),
        deferredSource =
          digest.flatMap(_.deferredSource).flatMap(cleanStringSignal)
            .orElse(Option.when(deferredMove.nonEmpty && topEngine.nonEmpty)("top_engine_move")),
        evidence = evidence,
        practicalAlternative = digest.exists(_.practicalAlternative),
        chosenMatchesBest = chosenMatchesBest
      )
    }

  private def replayDecisionComparisonLoss(
      digestLoss: Option[Int],
      topEngineLoss: Option[Int]
  ): Option[Int] =
    digestLoss.filter(_ >= 60)
      .orElse(topEngineLoss)
      .orElse(digestLoss)

  private def replayTopEngineMoveLabel(
      moment: GameChronicleMoment,
      engineMove: EngineAlternative
  ): Option[String] =
    engineMove.san.flatMap(cleanStringSignal)
      .orElse(cleanStringSignal(NarrativeUtils.uciToSanOrFormat(moment.fen, engineMove.uci)))

  private def replayTopEngineEvidence(
      moment: GameChronicleMoment,
      engineMove: Option[EngineAlternative]
  ): Option[String] =
    engineMove
      .flatMap { value =>
        val sanLine =
          NarrativeUtils
            .uciListToSan(moment.fen, value.pv.take(4))
            .map(_.trim)
            .filter(_.nonEmpty)
        Option.when(sanLine.nonEmpty)(sanLine.mkString(" "))
      }
      .flatMap(cleanStringSignal)

  private def sameReplayMoveToken(a: String, b: String): Boolean =
    normalizeReplayMoveToken(a) == normalizeReplayMoveToken(b)

  private def normalizeReplayMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.replaceAll("""[+#?!]+$""", "").toLowerCase

  private def replayAlternativeNarrative(
      comparison: Option[DecisionComparison]
  ): Option[AlternativeNarrative] =
    comparison
      .flatMap(cmp => cmp.deferredReason.map(reason => cmp -> reason))
      .map { case (cmp, reason) =>
        AlternativeNarrative(
          move = cmp.deferredMove,
          reason = reason,
          sentence =
            cmp.deferredMove match
              case Some(move) => s"The practical alternative $move stays secondary because $reason."
              case None       => s"The practical alternative stays secondary because $reason."
          ,
          source = cmp.deferredSource.getOrElse("active_digest")
        )
      }

  private def replayPreventedPlans(
      moment: GameChronicleMoment,
      dossier: Option[ActiveBranchDossier]
  ): List[PreventedPlanInfo] =
    val digest = moment.signalDigest
    val threatType =
      digest.flatMap(_.prophylaxisThreat).flatMap(cleanStringSignal)
        .orElse(dossier.flatMap(_.opponentResource).flatMap(cleanStringSignal))
    val breakNeutralized =
      digest.flatMap(_.prophylaxisPlan).flatMap(cleanStringSignal).filter(_.toLowerCase.contains("break"))
    val counterplayDrop = digest.flatMap(_.counterplayScoreDrop).getOrElse(if threatType.nonEmpty then 120 else 0)
    Option.when(threatType.nonEmpty || breakNeutralized.nonEmpty || counterplayDrop > 0) {
      PreventedPlanInfo(
        planId = "active_prevention",
        deniedSquares = Nil,
        breakNeutralized = breakNeutralized,
        mobilityDelta = 0,
        counterplayScoreDrop = counterplayDrop,
        preventedThreatType = threatType,
        deniedResourceClass = None,
        deniedEntryScope = None,
        sourceScope = FactScope.Now,
        citationLine = dossier.flatMap(_.evidenceCue).flatMap(cleanStringSignal)
      )
    }.toList

  private def replayPvDelta(
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier]
  ): Option[PVDelta] =
    deltaBundle.claims.headOption.map { claim =>
      val anchor = cleanStringSignal(claim.anchorText).toList
      val resolved =
        if claim.deltaClass == PlayerFacingMoveDeltaClass.CounterplayReduction || claim.deltaClass == PlayerFacingMoveDeltaClass.ResourceRemoval
        then dossier.flatMap(_.opponentResource).flatMap(cleanStringSignal).toList
        else Nil
      val opportunities =
        claim.deltaClass match
          case PlayerFacingMoveDeltaClass.NewAccess | PlayerFacingMoveDeltaClass.PressureIncrease =>
            anchor
          case PlayerFacingMoveDeltaClass.ExchangeForcing =>
            claim.moveCue.flatMap(_.san).flatMap(cleanStringSignal).toList
          case _ =>
            Nil
      val advancements =
        Option.when(claim.deltaClass == PlayerFacingMoveDeltaClass.PlanAdvance)(anchor).getOrElse(Nil)
      PVDelta(
        resolvedThreats = resolved,
        newOpportunities = opportunities,
        planAdvancements = advancements,
        concessions = Nil
      )
    }

  private def replayPracticalInfo(moment: GameChronicleMoment): Option[PracticalInfo] =
    moment.signalDigest.flatMap(_.practicalVerdict).flatMap(cleanStringSignal).map { verdict =>
      PracticalInfo(
        engineScore = moment.cpAfter,
        practicalScore = 1.0,
        verdict = verdict,
        biasFactors =
          moment.signalDigest.toList
            .flatMap(_.practicalFactors)
            .flatMap(cleanStringSignal)
            .map(factor => PracticalBiasInfo("active_practical", factor, 0.6))
            .take(2)
      )
    }

  private def replayThreats(
      moment: GameChronicleMoment,
      dossier: Option[ActiveBranchDossier]
  ): List[ThreatRow] =
    val threatText =
      moment.signalDigest.flatMap(_.prophylaxisThreat).flatMap(cleanStringSignal)
        .orElse(dossier.flatMap(_.opponentResource).flatMap(cleanStringSignal))
    threatText.toList.map { text =>
      ThreatRow(
        kind = text,
        side = "US",
        square = None,
        lossIfIgnoredCp = moment.signalDigest.flatMap(_.counterplayScoreDrop).getOrElse(120),
        turnsToImpact = 1,
        bestDefense = dossier.flatMap(_.practicalRisk).flatMap(cleanStringSignal),
        defenseCount = 1,
        insufficientData = false,
        confidence = ConfidenceLevel.Heuristic
      )
    }

  private def replayEvidenceBackedPlans(moment: GameChronicleMoment): List[PlanHypothesis] =
    StrategicNarrativePlanSupport.filterEvidenceBacked(
      moment.mainStrategicPlans,
      moment.strategicPlanExperiments
    )

  private def replayOpponentPlan(moment: GameChronicleMoment): Option[PlanRow] =
    moment.signalDigest.flatMap(_.opponentPlan).flatMap(cleanStringSignal).map { name =>
      PlanRow(
        rank = 1,
        name = name,
        score = 0.6,
        evidence = Nil,
        confidence = ConfidenceLevel.Heuristic
      )
    }

  private def replayAuthorQuestions(
      summaries: List[AuthorQuestionSummary],
      evidence: List[QuestionEvidence]
  ): List[AuthorQuestion] =
    val purposesByQuestion = evidence.groupBy(_.questionId).view.mapValues(_.map(_.purpose).distinct).toMap
    summaries.flatMap { summary =>
      replayQuestionKind(summary.kind).map { kind =>
        AuthorQuestion(
          id = summary.id,
          kind = kind,
          priority = summary.priority,
          question = summary.question,
          why = summary.why,
          anchors = summary.anchors,
          confidence = replayConfidence(summary.confidence),
          evidencePurposes = purposesByQuestion.getOrElse(summary.id, Nil),
          latentPlan = None
        )
      }
    }

  private def replayAuthorEvidence(
      summaries: List[AuthorEvidenceSummary]
  ): List[QuestionEvidence] =
    summaries.map { summary =>
      QuestionEvidence(
        questionId = summary.questionId,
        purpose = summary.purposes.headOption.getOrElse("active_summary"),
        branches =
          summary.branches.map(branch =>
            EvidenceBranch(
              keyMove = branch.keyMove,
              line = branch.line,
              evalCp = branch.evalCp,
              mate = branch.mate,
              depth = branch.depth,
              sourceId = branch.sourceId
            )
          )
      )
    }

  private def replayQuestionKind(raw: String): Option[AuthorQuestionKind] =
    Option(raw).map(_.trim) collect {
      case "WhatMattersHere"    => AuthorQuestionKind.WhatMattersHere
      case "WhyThis"            => AuthorQuestionKind.WhyThis
      case "WhyNow"             => AuthorQuestionKind.WhyNow
      case "WhatChanged"        => AuthorQuestionKind.WhatChanged
      case "WhatMustBeStopped"  => AuthorQuestionKind.WhatMustBeStopped
      case "WhosePlanIsFaster"  => AuthorQuestionKind.WhosePlanIsFaster
    }

  private def replayConfidence(raw: String): ConfidenceLevel =
    Option(raw).map(_.trim) match
      case Some("Engine") => ConfidenceLevel.Engine
      case Some("Probe")  => ConfidenceLevel.Probe
      case _              => ConfidenceLevel.Heuristic

  private def branchDisplayLine(branch: EvidenceBranch): Option[String] =
    LineScopedCitation
      .evidenceBranchDisplayLine(branch)
      .orElse(cleanStringSignal(branch.line))

  private def renderPlannerFirstNoteDebug(
      selection: PlannerSurfaceSelection,
      moment: GameChronicleMoment
  ): DeterministicComposeDebug =
    val contrastSupport =
      selection.primary.questionKind match
        case AuthorQuestionKind.WhyThis | AuthorQuestionKind.WhyNow =>
          ContrastiveSupportAdmissibility
            .decide(selection.primary, selection.inputs, truthContract = selection.truthContract)
            .effectiveSupport(selection.primary.contrast)
        case _ =>
          selection.primary.contrast
    val claim =
      selection.primary.questionKind match
        case AuthorQuestionKind.WhyNow =>
          activeWhyNowClaim(selection, cleanActiveSentence(selection.primary.claim))
        case _ =>
          cleanActiveSentence(selection.primary.claim)
    val supportCandidates =
      selection.primary.questionKind match
        case AuthorQuestionKind.WhatMattersHere =>
          List(
            supportCandidate(
              source = "consequence",
              raw = selection.primary.consequence.map(_.text),
              cleaned = cleanActiveSupportSentence(selection.primary.consequence.map(_.text))
            ),
            supportCandidate(
              source = "evidence",
              raw = selection.primary.evidence.map(_.text),
              cleaned = shortEvidenceSentence(selection.primary.evidence)
            )
          )
        case AuthorQuestionKind.WhyThis =>
          List(
            supportCandidate(
              source = "evidence",
              raw = selection.primary.evidence.map(_.text),
              cleaned = shortEvidenceSentence(selection.primary.evidence)
            ),
            supportCandidate(
              source = "contrast",
              raw = contrastSupport,
              cleaned = cleanActiveSupportSentence(contrastSupport)
            )
          )
        case AuthorQuestionKind.WhatMustBeStopped =>
          List(
            supportCandidate(
              source = "evidence",
              raw = selection.primary.evidence.map(_.text),
              cleaned = shortEvidenceSentence(selection.primary.evidence)
            ),
            supportCandidate(
              source = "contrast",
              raw = selection.primary.contrast,
              cleaned = cleanActiveSupportSentence(selection.primary.contrast)
            )
          )
        case AuthorQuestionKind.WhyNow =>
          List(
            supportCandidate(
              source = "contrast",
              raw = contrastSupport,
              cleaned = cleanActiveSupportSentence(contrastSupport)
            ),
            supportCandidate(
              source = "consequence",
              raw = selection.primary.consequence.map(_.text),
              cleaned = cleanActiveSupportSentence(selection.primary.consequence.map(_.text))
            ),
            supportCandidate(
              source = "evidence",
              raw = selection.primary.evidence.map(_.text),
              cleaned = shortEvidenceSentence(selection.primary.evidence)
            )
          ) ++ fallbackWhyNowSupportCandidates(selection, moment)
        case AuthorQuestionKind.WhatChanged =>
          List(
            supportCandidate(
              source = "consequence",
              raw = selection.primary.consequence.map(_.text),
              cleaned = cleanActiveSupportSentence(selection.primary.consequence.map(_.text))
            ),
            supportCandidate(
              source = "contrast",
              raw = selection.primary.contrast,
              cleaned = cleanActiveSupportSentence(selection.primary.contrast)
            ),
            supportCandidate(
              source = "evidence",
              raw = selection.primary.evidence.map(_.text),
              cleaned = shortEvidenceSentence(selection.primary.evidence)
            )
          )
        case AuthorQuestionKind.WhosePlanIsFaster =>
          Nil

    val vettedSupportCandidates = markDuplicateClaimSupport(claim, supportCandidates)
    val selectedSupport = vettedSupportCandidates.collectFirst { case candidate if candidate.cleanedText.nonEmpty => candidate }

    val sentences =
      distinctSentences(List(claim, selectedSupport.flatMap(_.cleanedText)).flatten.map(asSentence))

    val minimumSentences =
      selection.primary.questionKind match
        case AuthorQuestionKind.WhatMattersHere | AuthorQuestionKind.WhyNow | AuthorQuestionKind.WhatChanged => 2
        case _                                                                                                 => 1

    val result =
      Option.when(sentences.nonEmpty && sentences.size >= minimumSentences) {
        sentences.take(2).mkString(" ")
      }.flatMap(cleanStringSignal)

    val failureReasons =
      List(
        Option.when(claim.isEmpty)("claim_empty"),
        Option.when(selectedSupport.isEmpty) {
          if vettedSupportCandidates.exists(_.droppedReasons.contains("duplicate_claim")) then "support_duplicate_claim"
          else if vettedSupportCandidates.exists(_.rawText.nonEmpty) then "support_filtered"
          else "support_missing"
        },
        Option.when(sentences.size < minimumSentences)(s"sentence_count_${sentences.size}_of_$minimumSentences"),
        Option.when(result.isEmpty && claim.nonEmpty && selectedSupport.nonEmpty && sentences.size >= minimumSentences)(
          "result_filtered"
        )
      ).flatten

    DeterministicComposeDebug(
      primaryKind = selection.primary.questionKind.toString,
      secondaryKind = selection.secondary.map(_.questionKind.toString),
      primaryClaimRaw = selection.primary.claim,
      primaryClaim = claim,
      primaryEvidenceRaw = selection.primary.evidence.map(_.text),
      supportCandidates = vettedSupportCandidates,
      selectedSupportSource = selectedSupport.map(_.source),
      selectedSupport = selectedSupport.flatMap(_.cleanedText),
      sentences = sentences,
      minimumSentences = minimumSentences,
      validatorInput = result,
      failureReasons = failureReasons
    )

  private def renderPlannerFirstNote(
      selection: PlannerSurfaceSelection,
      moment: GameChronicleMoment
  ): Option[String] =
    renderPlannerFirstNoteDebug(selection, moment).result

  private def fallbackWhyNowSupportCandidates(
      selection: PlannerSurfaceSelection,
      moment: GameChronicleMoment
  ): List[DeterministicSupportCandidate] =
    val comparisonSupport =
      selection.inputs.decisionComparison.flatMap { comparison =>
        val loss = comparison.cpLossVsChosen.map(math.abs).filter(_ >= 60)
        val alternative =
          comparison.deferredMove
            .filter(_.trim.nonEmpty)
            .orElse(
              comparison.engineBestMove
                .filter(_.trim.nonEmpty)
                .filter(move => comparison.chosenMove.forall(chosen => !sameMoveToken(chosen, move)))
            )
        alternative.flatMap { move =>
          loss.map(cp => s"If delayed, $move is the cleaner continuation and about ${cp}cp slips away.")
        }.orElse {
          comparison.deferredReason
            .filter(_.trim.nonEmpty)
            .flatMap(reason => loss.map(cp => s"If delayed, about ${cp}cp slips away: $reason."))
        }.orElse {
          loss.map(cp => s"If delayed, about ${cp}cp of practical value slips away.")
        }
      }
    val threatSupport =
      selection.inputs.opponentThreats.headOption.map { threat =>
        s"If delayed, the ${threat.kind.toLowerCase} threat becomes harder to meet."
      }
    val preventedBreakSupport =
      selection.inputs.preventedPlansNow.collectFirst {
        case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) =>
          s"If delayed, the ${plan.breakNeutralized.get}-break becomes a live counterplay window."
      }
    val mainLineSupport =
      selection.inputs.mainBundle
        .flatMap(_.lineScopedClaim)
        .flatMap(claim => anchoredWhyNowSupportSentence(Some(claim.claimText)))
    val candidateLineSupport =
      selection.inputs.candidateEvidenceLines.view
        .flatMap(line => anchoredWhyNowSupportSentence(Some(line)))
        .headOption
    val narrativeTailSupport =
      Option(moment.narrative)
        .toList
        .flatMap(_.split("""(?<=[.!?])\s+""").toList.drop(1))
        .flatMap(text => anchoredWhyNowSupportSentence(Some(text)))
        .headOption
    val playedMoveSupport =
      selection.inputs.decisionComparison
        .flatMap(_.chosenMove)
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(move => s"If delayed, $move no longer works with the same immediate tempo here.")
    List(
      supportCandidate(
        source = "fallback_comparison",
        raw = comparisonSupport,
        cleaned = comparisonSupport.flatMap(text => cleanActiveSupportSentence(Some(text)))
      ),
      supportCandidate(
        source = "fallback_threat",
        raw = threatSupport,
        cleaned = threatSupport.flatMap(text => cleanActiveSupportSentence(Some(text)))
      ),
      supportCandidate(
        source = "fallback_prevented_break",
        raw = preventedBreakSupport,
        cleaned = preventedBreakSupport.flatMap(text => cleanActiveSupportSentence(Some(text)))
      ),
      supportCandidate(
        source = "fallback_main_line",
        raw = selection.inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText),
        cleaned = mainLineSupport
      ),
      supportCandidate(
        source = "fallback_candidate_line",
        raw = selection.inputs.candidateEvidenceLines.headOption,
        cleaned = candidateLineSupport
      ),
      supportCandidate(
        source = "fallback_narrative_tail",
        raw =
          Option(moment.narrative)
            .toList
            .flatMap(_.split("""(?<=[.!?])\s+""").toList.drop(1))
            .find(_.trim.nonEmpty),
        cleaned = narrativeTailSupport
      ),
      supportCandidate(
        source = "fallback_played_move",
        raw = playedMoveSupport,
        cleaned = playedMoveSupport.flatMap(text => cleanActiveSupportSentence(Some(text)))
      )
    )

  private def activeWhyNowClaim(
      selection: PlannerSurfaceSelection,
      cleanedClaim: Option[String]
  ): Option[String] =
    cleanedClaim.flatMap { claim =>
      val chosenMove =
        selection.inputs.decisionComparison
          .flatMap(_.chosenMove)
          .map(_.trim)
          .filter(_.nonEmpty)
      chosenMove
        .flatMap { move =>
          val rewritten =
            claim
              .replaceFirst(
                """(?i)^The timing matters now because\s+""",
                s"$move has to be played now because "
              )
              .replaceFirst(
                """(?i)^The move has to happen now because\s+""",
                s"$move has to be played now because "
              )
          Option.when(rewritten != claim)(rewritten)
        }
        .orElse(Some(claim))
        .flatMap(cleanActiveSentence)
    }

  private def supportCandidate(
      source: String,
      raw: Option[String],
      cleaned: Option[String]
  ): DeterministicSupportCandidate =
    val normalizedRaw = raw.map(_.trim).filter(_.nonEmpty)
    val baseReasons =
      List(
        Option.when(normalizedRaw.isEmpty)("missing_raw"),
        Option.when(normalizedRaw.nonEmpty && cleaned.isEmpty)("filtered")
      ).flatten
    DeterministicSupportCandidate(
      source = source,
      rawText = normalizedRaw,
      cleanedText = cleaned,
      droppedReasons = baseReasons
    )

  private def markDuplicateClaimSupport(
      claim: Option[String],
      candidates: List[DeterministicSupportCandidate]
  ): List[DeterministicSupportCandidate] =
    candidates.map { candidate =>
      val duplicateClaim =
        claim.exists(current =>
          candidate.cleanedText.exists(text =>
            NarrativeDedupCore.sameSemanticSentence(current, text) ||
              StrategicSignalMatcher.containsComparablePhrase(current, text)
          )
        )
      if duplicateClaim then
        candidate.copy(
          cleanedText = None,
          droppedReasons = (candidate.droppedReasons :+ "duplicate_claim").distinct
        )
      else candidate
    }

  private def anchoredWhyNowSupportSentence(raw: Option[String]): Option[String] =
    raw.flatMap { text =>
      val stripped = stripEvidenceLead(text)
      cleanActiveSupportSentence(Some(stripped)).orElse {
        cleanStringSignal(LiveNarrativeCompressionCore.rewritePlayerLanguage(stripped))
          .filter(LiveNarrativeCompressionCore.hasConcreteAnchor)
      }
    }

  private def sameMoveToken(a: String, b: String): Boolean =
    normalizeMoveToken(a) == normalizeMoveToken(b)

  private def normalizeMoveToken(move: String): String =
    Option(move).getOrElse("").replaceAll("""[^A-Za-z0-9]""", "").trim.toLowerCase

  private def shortEvidenceSentence(
      evidence: Option[QuestionPlanEvidence]
  ): Option[String] =
    evidence
      .flatMap(_.text.linesIterator.map(_.trim).find(_.nonEmpty))
      .map(stripEvidenceLead)
      .flatMap(cleanActiveSupportSentence)

  private def cleanActiveSentence(raw: String): Option[String] =
    playerFacingSentence(raw)
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)

  private def cleanActiveSupportSentence(raw: String): Option[String] =
    cleanActiveSupportSentence(Some(raw))

  private def cleanActiveSupportSentence(raw: Option[String]): Option[String] =
    raw.flatMap(text =>
      playerFacingSentence(stripEvidenceLead(text))
        .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)
    )

  private def stripEvidenceLead(text: String): String =
    Option(text)
      .getOrElse("")
      .replaceFirst("""^[a-z]\)\s*""", "")
      .replaceFirst("""^Line:\s*""", "")
      .replaceFirst("""(?i)^Further probe work still targets\s+""", "")
      .trim

  private def distinctSentences(sentences: List[String]): List[String] =
    sentences.foldLeft(List.empty[String]) { (acc, sentence) =>
      if acc.exists(existing => StrategicSignalMatcher.containsComparablePhrase(existing, sentence)) then acc
      else acc :+ sentence
    }

  def build(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      @unused routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None,
      decisionFrame: Option[CertifiedDecisionFrame] = None
  ): Brief =
    val surface = StrategyPackSurface.from(strategyPack)
    val dominantIdea = strategyPack.toList.flatMap(_.strategicIdeas).headOption
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove)).getOrElse("white")
    val currentBoard = currentFen.flatMap(parseBoard)
    val decisionSupports = decisionFrame.toList.flatMap(_.orderedSupports(max = 2))
    val tacticalReality = immediateTacticalReality(currentBoard, preferredSide, moveRefs)
    val compensationNarrationEligible = CompensationDisplayPhrasing.compensationNarrationEligible(surface)
    val compensationLead =
      Option.when(compensationNarrationEligible)(
        StrategyPackSurface
          .resolvedNormalizedCompensationLead(surface)
          .orElse(canonicalCompensationLead(surface))
      ).flatten
    val whyNow =
      contextualizeSignal(
        pickFirst(
          tacticalReality,
          dossier.flatMap(_.whyChosen),
          Option.when(compensationNarrationEligible)(compensationLead.map(lead => s"This keeps $lead in play.")).flatten
        ),
        currentBoard,
        preferredSide
      )
    val opponentReply =
      contextualizeSignal(
        pickFirst(
          dossier.flatMap(_.opponentResource),
          dossier.flatMap(_.threadOpponentCounterplan)
        ),
        currentBoard,
        preferredSide
      )
    val executionHint =
      contextualizeSignal(
        pickFirst(
          Option.when(compensationNarrationEligible)(CompensationDisplayPhrasing.compensationExecutionTail(surface)).flatten
        ),
        currentBoard,
        preferredSide
      )
    val keyTrigger =
      contextualizeSignal(
        dossier.flatMap(_.practicalRisk),
        currentBoard,
        preferredSide
      )
    Brief(
      campaignRole =
        surface.campaignOwnerText.filter(_ => surface.ownerMismatch).map(side =>
          (List(side + "'s campaign") ++ dossier.flatMap(_.threadStage).flatMap(stageRoleDescription).toList).mkString(": ")
        ).orElse(dossier.flatMap(_.threadStage).flatMap(stageRoleDescription)),
      primaryIdea = decisionFrame.flatMap(_.intent).map(_.sentence),
      compensationLead = compensationLead,
      whyNow = whyNow,
      opponentReply = opponentReply,
      executionHint = executionHint,
      longTermObjective = decisionFrame.flatMap(_.battlefront).map(_.sentence),
      keyTrigger = keyTrigger,
      compensationAnchor = dossier.flatMap(_.evidenceCue).flatMap(cleanStringSignal).filter(LiveNarrativeCompressionCore.hasConcreteAnchor),
      continuationFocus = decisionFrame.flatMap(_.urgency).map(_.sentence),
      decisionSupports = decisionSupports
    )

  def evaluateCoverage(text: String, brief: Brief): Coverage =
    val normalizedText = normalize(text)
    val textTokens = StrategicSignalMatcher.signalTokens(normalizedText)

    def mentioned(signal: Option[String]): Boolean =
      signal.exists(signalMentioned(normalizedText, textTokens, _))

    def explicitlyMentioned(signal: Option[String]): Boolean =
      signal.exists { value =>
        val normalizedSignal = normalize(value)
        normalizedSignal.nonEmpty && (
          StrategicSignalMatcher.phraseMentioned(normalizedText, normalizedSignal) ||
            StrategicSignalMatcher.containsComparablePhrase(normalizedText, value)
        )
      }

    val dominantIdeaMentioned =
      brief.primaryIdea.exists(primary =>
        StrategicSignalMatcher.phraseMentioned(normalizedText, normalize(primary)) ||
          StrategicSignalMatcher.signalTokens(normalize(primary)).intersect(textTokens).size >= 2
      )
    val campaignOwnerMentioned =
      brief.campaignRole.exists(role =>
        (normalize(role).contains("white") && normalizedText.contains("white")) ||
          (normalize(role).contains("black") && normalizedText.contains("black"))
      ) || brief.primaryIdea.exists(primary =>
        (normalize(primary).contains("white") && normalizedText.contains("white")) ||
          (normalize(primary).contains("black") && normalizedText.contains("black"))
      )

    val forwardCue = ForwardCuePatterns.exists(_.findFirstIn(normalizedText).nonEmpty)
    val structuralSequenceCue =
      List("before", "then", "next", "once", "after", "if").exists(word => normalizedText.contains(s" $word ")) ||
        normalizedText.startsWith("if ")
    val objectiveCue =
      explicitlyMentioned(brief.longTermObjective) ||
        explicitlyMentioned(brief.continuationFocus) ||
        brief.longTermObjective.exists(_ => normalizedText.contains("work toward") || normalizedText.contains("making")) ||
        brief.continuationFocus.exists(_ => normalizedText.contains("next step") || normalizedText.contains("work toward"))
    val executionCueMentioned = explicitlyMentioned(brief.executionHint)
    val compensationAnchorMentioned =
      explicitlyMentioned(brief.compensationAnchor) || hasConcreteCompensationAnchor(normalizedText)

    Coverage(
      hasDominantIdea = dominantIdeaMentioned,
      hasForwardPlan =
        forwardCue ||
          (
            (dominantIdeaMentioned || executionCueMentioned || objectiveCue || compensationAnchorMentioned) &&
              (structuralSequenceCue || executionCueMentioned || objectiveCue)
          ),
      hasConcreteAnchor = compensationAnchorMentioned,
      hasGroundedSignal = dominantIdeaMentioned || compensationAnchorMentioned,
      hasOpponentOrTrigger = mentioned(brief.opponentReply) || mentioned(brief.keyTrigger),
      hasCampaignOwner = campaignOwnerMentioned
    )

  def buildDeterministicNote(
      selection: PlannerSurfaceSelection,
      moment: GameChronicleMoment
  ): Option[String] =
    renderPlannerFirstNote(selection, moment)

  def debugDeterministicNote(
      selection: PlannerSurfaceSelection,
      moment: GameChronicleMoment
  ): DeterministicComposeDebug =
    renderPlannerFirstNoteDebug(selection, moment)

  def buildDeterministicNote(
      moment: GameChronicleMoment,
      deltaBundle: PlayerFacingMoveDeltaBundle,
      dossier: Option[ActiveBranchDossier],
      decisionFrame: Option[CertifiedDecisionFrame] = None
  ): Option[String] =
    decisionFrame
      .flatMap(frame => selectPlannerSurface(moment, deltaBundle, dossier, frame))
      .flatMap(buildDeterministicNote(_, moment))

  def buildStrictCompensationFallbackNote(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None,
      decisionFrame: Option[CertifiedDecisionFrame] = None
  ): Option[String] =
    val surface = StrategyPackSurface.from(strategyPack)
    Option.when(LlmApi.activeCompensationNoteExpected(surface)) {
      renderStrictCompensationFallback(
        brief = build(strategyPack, dossier, routeRefs, moveRefs, currentFen, decisionFrame),
        surface = surface
      )
    }.flatten

  def renderDeterministicNote(brief: Brief): Option[String] =
    val leadSentence =
      pickFirst(
        brief.whyNow.flatMap(playerFacingSentence),
        brief.compensationLead.flatMap(playerFacingSentence).map(lead =>
          s"The compensation comes from ${stripTrailingPunctuation(lead)}."
        )
      ).map(asSentence)

    val executionSentence =
      brief.executionHint
        .flatMap(playerFacingSentence)
        .filterNot(signal => leadSentence.exists(sentence => normalize(sentence).contains(normalize(signal))))
        .map(asSentence)
    val decisionSupportSentences =
      brief.decisionSupports
        .flatMap(playerFacingSentence)
        .filterNot(signal =>
          leadSentence.exists(sentence => normalize(sentence).contains(normalize(signal))) ||
            executionSentence.exists(sentence => normalize(sentence).contains(normalize(signal)))
        )
        .map(asSentence)
        .distinct

    val cautionSentence =
      (brief.opponentReply.flatMap(cleanStringSignal), brief.keyTrigger.flatMap(cleanStringSignal)) match
        case (Some(reply), Some(trigger)) =>
          Some(
            s"${asSentence(capitalizeSentenceStart(stripTrailingPunctuation(reply)))} ${asSentence(capitalizeSentenceStart(stripTrailingPunctuation(trigger)))}"
          )
        case (Some(reply), None) =>
          Some(asSentence(capitalizeSentenceStart(stripTrailingPunctuation(reply))))
        case (None, Some(trigger)) =>
          Some(asSentence(capitalizeSentenceStart(stripTrailingPunctuation(trigger))))
        case _ =>
          None

    val sentences =
      (
        leadSentence.toList ++
          decisionSupportSentences.take(1) ++
          executionSentence.toList.filterNot(sentence => decisionSupportSentences.exists(existing => normalize(existing) == normalize(sentence))) ++
          cautionSentence.toList ++
          decisionSupportSentences.drop(1).take(1)
      )
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(3)

    Option.when(sentences.nonEmpty)(sentences.mkString(" "))

  private def renderStrictCompensationFallback(
      brief: Brief,
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    val leadSentence =
      brief.compensationLead
        .flatMap(cleanStringSignal)
        .orElse(StrategyPackSurface.resolvedNormalizedCompensationLead(surface).flatMap(cleanStringSignal))
        .orElse(canonicalCompensationLead(surface))
        .map(lead => asSentence(s"The compensation comes from ${stripTrailingPunctuation(lead)}."))

    val anchorSignal =
      selectStrictCompensationAnchorSignal(brief, surface, CompensationContractMatcher.canonicalSubtype(surface))
    val anchorSentence =
      anchorSignal
        .flatMap(cleanStringSignal)
        .filter(hasConcreteCompensationAnchor)
        .filterNot(anchor => leadSentence.exists(sentence => normalize(sentence).contains(normalize(anchor))))
        .map(StrategicSentenceRenderer.renderCompensationAnchor)

    val whyNowSentence =
      brief.whyNow
        .flatMap(cleanStringSignal)
        .filter(text => normalize(text).nonEmpty)
        .filterNot(text => leadSentence.exists(sentence => normalize(sentence).contains(normalize(text))))
        .filterNot(text => anchorSignal.exists(anchor => normalize(anchor) == normalize(text)))
        .map(asSentence)
    val decisionSupportSentence =
      brief.decisionSupports
        .flatMap(cleanStringSignal)
        .find(text =>
          !leadSentence.exists(sentence => normalize(sentence).contains(normalize(text))) &&
            !anchorSignal.exists(anchor => normalize(anchor) == normalize(text))
        )
        .map(asSentence)

    val sentences =
      List(leadSentence, decisionSupportSentence, whyNowSentence, anchorSentence)
        .flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(3)

    Option.when(leadSentence.nonEmpty && (anchorSentence.nonEmpty || whyNowSentence.nonEmpty)) {
      sentences.mkString(" ")
    }

  private def canonicalCompensationLead(surface: StrategyPackSurface.Snapshot): Option[String] =
    CompensationContractMatcher
      .canonicalSubtype(surface)
      .map { subtype =>
        val theater = canonicalCompensationTheaterLabel(subtype)
        subtype.pressureMode match
          case "line_occupation" =>
            if theater == "central" then "central file pressure" else s"$theater file pressure"
          case "target_fixing" =>
            if theater == "central" then "pressure against fixed central targets"
            else s"$theater pressure against fixed targets"
          case "counterplay_denial" =>
            s"denying $theater counterplay"
          case "defender_tied_down" =>
            s"keeping the defender tied down on the $theater"
          case "conversion_window" =>
            s"$theater conversion pressure"
          case "break_preparation" =>
            s"keeping the $theater break ready"
          case _ =>
            s"$theater compensation"
      }
      .flatMap(cleanStringSignal)

  private def canonicalCompensationTheaterLabel(
      subtype: StrategyPackSurface.CompensationSubtype
  ): String =
    subtype.pressureTheater match
      case "queenside" => "queenside"
      case "center"    => "central"
      case "kingside"  => "kingside"
      case other       => other

  private def selectStrictCompensationAnchorSignal(
      brief: Brief,
      surface: StrategyPackSurface.Snapshot,
      canonicalSubtype: Option[StrategyPackSurface.CompensationSubtype]
  ): Option[String] =
    pickFirst(
      canonicalSubtype.flatMap(subtype =>
        StrategyPackSurface.alignedDirectionalTarget(surface, subtype).flatMap(compensationTargetAnchorSummary)
      ),
      canonicalSubtype.flatMap(subtype =>
        StrategyPackSurface.alignedMoveRef(surface, subtype).map(compensationMoveRefAnchorSummary)
      ),
      canonicalSubtype.flatMap(subtype =>
        StrategyPackSurface.alignedRoute(surface, subtype).map(compensationRouteAnchorSummary)
      ),
      StrategyPackSurface.resolvedNormalizedExecutionText(surface).filter(hasConcreteCompensationAnchor),
      StrategyPackSurface.resolvedNormalizedObjectiveText(surface).filter(hasConcreteCompensationAnchor),
      StrategyPackSurface.resolvedNormalizedLongTermFocusText(surface).filter(hasConcreteCompensationAnchor),
      brief.compensationAnchor.filter(text => compensationSignalAllowed(text, canonicalSubtype)),
      brief.executionHint.filter(text => compensationSignalAllowed(text, canonicalSubtype)),
      brief.longTermObjective.filter(text => compensationSignalAllowed(text, canonicalSubtype)),
      brief.continuationFocus.filter(text => compensationSignalAllowed(text, canonicalSubtype)),
      surface.executionText.filter(text => compensationSignalAllowed(text, canonicalSubtype) && hasConcreteCompensationAnchor(text)),
      surface.objectiveText.filter(text => compensationSignalAllowed(text, canonicalSubtype) && hasConcreteCompensationAnchor(text)),
      surface.focusText.filter(text => compensationSignalAllowed(text, canonicalSubtype) && hasConcreteCompensationAnchor(text))
    )

  private def pickFirst(values: Option[String]*): Option[String] =
    values.iterator.flatMap(cleanSignal).toSeq.headOption

  private def cleanSignal(raw: Option[String]): Option[String] =
    raw.flatMap { value =>
      val sanitized =
        UserFacingSignalSanitizer.sanitize(normalizeExecutionLikeSignal(naturalizeLabel(value)))
      Option(sanitized).map(_.trim).filter(_.nonEmpty)
    }

  private def cleanStringSignal(raw: String): Option[String] =
    cleanSignal(Some(raw))

  private def playerFacingSentence(raw: String): Option[String] =
    cleanStringSignal(raw)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(cleanStringSignal)
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)

  private def contextualizeSignal(raw: Option[String], board: Option[Board], side: String): Option[String] =
    raw.flatMap { value =>
      cleanSignal(Some(rewriteOccupiedSquareLanguage(value, board, side)))
    }

  private def signalMentioned(normalizedText: String, textTokens: Set[String], signal: String): Boolean =
    val normalizedSignal = normalize(signal)
    if normalizedSignal.isEmpty then false
    else
      StrategicSignalMatcher.phraseMentioned(normalizedText, normalizedSignal) ||
        StrategicSignalMatcher.signalTokens(normalizedSignal).intersect(textTokens).nonEmpty

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def stripTrailingPunctuation(text: String): String =
    Option(text).getOrElse("").trim.stripSuffix(".").stripSuffix("!").stripSuffix("?").trim

  private def capitalizeSentenceStart(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else s"${trimmed.head.toUpper}${trimmed.tail}"

  private def asSentence(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else if ".!?".contains(trimmed.last) then trimmed else s"$trimmed."

  private def naturalizeLabel(raw: String): String =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(
        _.replace("->", " leading to ")
          .replaceAll("(?i)\\b([a-h])-break\\s+Break\\b", "$1-break")
          .replaceAll("\\s+", " ")
      )
      .getOrElse("")

  private def pieceName(code: String): String =
    val normalized = Option(code).map(_.trim).getOrElse("")
    val upper = normalized.toUpperCase
    PieceNames.get(upper)
      .orElse(
        upper.split("\\s+").toList.reverse.collectFirst(Function.unlift(PieceNames.get))
      )
      .orElse {
        val lowered = normalized.toLowerCase
        List("pawn", "knight", "bishop", "rook", "queen", "king").find(lowered.contains)
      }
      .getOrElse("piece")

  private def compensationMoveRefAnchorSummary(moveRef: StrategyPieceMoveRef): String =
    cleanSignal(Some(s"${pieceName(moveRef.piece)} toward ${moveRef.target}"))
      .getOrElse(s"${pieceName(moveRef.piece)} toward ${moveRef.target}")

  private def compensationTargetAnchorSummary(target: StrategyDirectionalTarget): Option[String] =
    cleanSignal(Some(target.targetSquare.toLowerCase))

  private def compensationRouteAnchorSummary(route: StrategyPieceRoute): String =
    routeLabel(
      ownerSide = None,
      piece = route.piece,
      route = route.route,
      purpose = None,
      surfaceMode = route.surfaceMode
    )

  private def routeLabel(
      ownerSide: Option[String],
      piece: String,
      route: List[String],
      purpose: Option[String],
      surfaceMode: String
  ): String =
    val destination =
      route
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8]$"))
        .lastOption
    val deploymentText =
      if surfaceMode == RouteSurfaceMode.Exact && route.nonEmpty then s"${pieceName(piece)} via ${route.mkString("-")}"
      else
        destination match
          case Some(square) => s"${pieceName(piece)} toward $square"
          case None         => s"${pieceName(piece)} redeployment"
    val sidePrefix = cleanSignal(ownerSide).map(_ + " ").getOrElse("")
    val prefixedDeployment = s"$sidePrefix$deploymentText".trim
    cleanSignal(purpose)
      .map(text => s"$prefixedDeployment ${purposeClause(text, destination.orNull)}")
      .getOrElse(prefixedDeployment)

  private def purposeClause(raw: String, targetSquare: String | Null): String =
    val cleaned = stripTrailingPunctuation(cleanSignal(Some(raw)).getOrElse(raw))
    if cleaned.isEmpty then ""
    else
      val normalizedIdea = normalize(cleaned)
      val target = Option(targetSquare).map(_.trim.toLowerCase).filter(_.matches("^[a-h][1-8]$"))
      if normalizedIdea.startsWith("keep the ") then
        target.filter(square => normalizedIdea.endsWith(square))
          .map(_ => "to keep the pressure fixed there")
          .getOrElse(s"to $cleaned")
      else if normalizedIdea.startsWith("contest ") || normalizedIdea.startsWith("attack ") || normalizedIdea.startsWith("build ") ||
        normalizedIdea.startsWith("prepare ") || normalizedIdea.startsWith("improve ") || normalizedIdea.startsWith("activate ") ||
        normalizedIdea.startsWith("reroute ") || normalizedIdea.startsWith("stabilize ") || normalizedIdea.startsWith("consolidate ") ||
        normalizedIdea.startsWith("pressure ") || normalizedIdea.startsWith("occupy ") then
        s"to $cleaned"
      else if normalizedIdea.contains("coordination improvement") then
        "to improve coordination"
      else if normalizedIdea.contains("plan activation lane") then
        "to activate the plan"
      else if normalizedIdea.contains("open file occupation") then
        "to occupy the open file"
      else if normalizedIdea.contains("semi-open file occupation") then
        "to occupy the semi-open file"
      else if normalizedIdea.contains("kingside pressure") then
        "to increase kingside pressure"
      else if normalizedIdea.contains("queenside pressure") then
        "to increase queenside pressure"
      else if normalizedIdea.contains("clamp") then
        s"to reinforce ${withArticle(strippedLeadingArticle(cleaned))}"
      else if normalizedIdea.contains("circuit") then
        s"to improve ${withArticle(strippedLeadingArticle(cleaned))}"
      else if normalizedIdea.contains("contest the ") then
        target.filter(square => normalizedIdea.endsWith(square))
          .map(_ => "to contest the target there")
          .getOrElse(s"to $cleaned")
      else s"to support ${strippedLeadingArticle(cleaned)}"

  private def compensationSignalAllowed(
      text: String,
      canonicalSubtype: Option[StrategyPackSurface.CompensationSubtype],
      requireSubtypeSupport: Boolean = false
  ): Boolean =
    cleanStringSignal(text).exists { cleaned =>
      canonicalSubtype.forall { subtype =>
        !conflictsWithCanonicalSubtype(cleaned, subtype) &&
          (!requireSubtypeSupport || CompensationContractMatcher.supportsSubtype(cleaned, subtype))
      }
    }

  private def conflictsWithCanonicalSubtype(
      text: String,
      subtype: StrategyPackSurface.CompensationSubtype
  ): Boolean =
    val normalized = normalize(text)
    normalized.nonEmpty && (mentionsConflictingTheater(normalized, subtype) || mentionsConflictingMode(normalized, subtype))

  private def mentionsConflictingTheater(
      normalized: String,
      subtype: StrategyPackSurface.CompensationSubtype
  ): Boolean =
    subtype.pressureTheater match
      case "queenside" => normalized.contains("kingside") || normalized.contains("center") || normalized.contains("central")
      case "center"    => normalized.contains("kingside") || normalized.contains("queenside")
      case "kingside"  => normalized.contains("queenside") || normalized.contains("center") || normalized.contains("central")
      case _           => false

  private val LineOccupationMarkers =
    List("line pressure", "file pressure", "open file", "open files", "queenside files", "central files")
  private val TargetFixingMarkers =
    List("fixed target", "fixed targets", "targets tied down", "fixed pawn", "weak pawn", "tied down")

  private def mentionsConflictingMode(
      normalized: String,
      subtype: StrategyPackSurface.CompensationSubtype
  ): Boolean =
    subtype.pressureMode match
      case "target_fixing"   => LineOccupationMarkers.exists(normalized.contains)
      case "line_occupation" => TargetFixingMarkers.exists(normalized.contains)
      case _                 => false

  private def normalizeExecutionLikeSignal(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else
      val executionLike =
        """(?i)^(?:(white|black)\s+)?([pnbrqk]|pawn|knight|bishop|rook|queen|king)\s+toward\s+([a-h][1-8])\s+for\s+(.+)$""".r
      trimmed match
        case executionLike(_, pieceToken, square, purpose) =>
          UserFacingSignalSanitizer
            .sanitize(s"${pieceName(pieceToken)} toward ${square.toLowerCase} ${purposeClause(purpose, square.toLowerCase)}")
            .trim
        case _ => trimmed

  private def strippedLeadingArticle(text: String): String =
    text.replaceFirst("(?i)^(a|an|the)\\s+", "").trim

  private def withArticle(text: String): String =
    val trimmed = text.trim
    if trimmed.isEmpty then trimmed
    else if trimmed.matches("(?i)^(a|an|the)\\s+.*") then trimmed
    else s"the $trimmed"

  def stageRoleDescription(rawStage: String): Option[String] =
    Option(rawStage)
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .map {
        case "seed"    => "the plan is only starting to take shape"
        case "build"   => "the plan is being consolidated move by move"
        case "switch"  => "the game is pivoting toward a new sector or target"
        case "convert" => "the accumulated pressure should now turn into something concrete"
        case other     => naturalizeLabel(other)
      }
      .flatMap(text => cleanSignal(Some(text)))

  private def hasConcreteCompensationAnchor(text: String): Boolean =
    val normalized = normalize(text)
    val hasSquare = """\b[a-h][1-8]\b""".r.findFirstIn(normalized).nonEmpty
    val hasFile = """\b[a-h]-file\b""".r.findFirstIn(normalized).nonEmpty
    val hasPieceRoute =
      """\b(pawn|knight|bishop|rook|queen|king)\s+(?:toward|via|can use|head(?:s)? for)\b""".r.findFirstIn(normalized).nonEmpty
    hasSquare ||
    hasFile ||
    hasPieceRoute ||
    normalized.contains("central files") ||
    normalized.contains("queenside files") ||
    normalized.contains("open file") ||
    normalized.contains("open files")

  private def parseBoard(fen: String): Option[Board] =
    Fen.read(Standard, Fen.Full(fen)).map(_.board)

  private def materialScore(board: Board, color: Color): Int =
    board.byPiece(color, Pawn).count +
      board.byPiece(color, Knight).count * 3 +
      board.byPiece(color, Bishop).count * 3 +
      board.byPiece(color, Rook).count * 5 +
      board.byPiece(color, Queen).count * 9

  private def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def immediateTacticalReality(
      currentBoard: Option[Board],
      side: String,
      moveRefs: List[ActiveStrategicMoveRef]
  ): Option[String] =
    currentBoard.flatMap { before =>
      val activeColor = sideColor(side)
      val currentEdge = materialScore(before, activeColor) - materialScore(before, !activeColor)

      moveRefs
        .flatMap { ref =>
          val san = ref.san.map(_.trim).filter(_.nonEmpty)
          val afterEdge =
            ref.fenAfter.flatMap(parseBoard).map(after =>
              materialScore(after, activeColor) - materialScore(after, !activeColor)
            )
          val gain = afterEdge.map(_ - currentEdge).getOrElse(0)
          san.flatMap { move =>
            if gain > 0 then
              Some(
                (
                  20 + gain,
                  s"$move immediately wins ${materialGainLabel(gain)}${if isForcingSan(move) then " while forcing the issue" else ""}."
                )
              )
            else if isForcingSan(move) then Some(10 -> s"$move forces the issue immediately.")
            else None
          }
        }
        .sortBy { case (priority, _) => -priority }
        .headOption
        .map(_._2)
    }

  private def materialGainLabel(gain: Int): String =
    if gain >= 9 then "a queen"
    else if gain >= 5 then "a rook"
    else if gain >= 3 then "a piece"
    else if gain >= 1 then "a pawn"
    else "material"

  private def isForcingSan(san: String): Boolean =
    val trimmed = Option(san).map(_.trim).getOrElse("")
    trimmed.contains("+") || trimmed.contains("#")

  private def rewriteOccupiedSquareLanguage(
      text: String,
      board: Option[Board],
      side: String
  ): String =
    board.fold(text) { currentBoard =>
      val color = sideColor(side)

      def occupantLabel(squareKey: String): Option[String] =
        Square.all
          .find(_.key == squareKey)
          .flatMap(currentBoard.pieceAt)
          .filter(_.color == color)
          .map(piece => pieceName(roleToken(piece.role)))

      def preserveCapitalization(original: String, replacement: String): String =
        if original.headOption.exists(_.isUpper) then replacement.take(1).toUpperCase + replacement.drop(1)
        else replacement

      val focus = """(?i)\bfocus on ([a-h][1-8])\b""".r
      val pointing = """(?i)\bpointing toward ([a-h][1-8])\b""".r

      val step1 =
        focus.replaceAllIn(text, m =>
          occupantLabel(m.group(1).toLowerCase)
            .map(piece => preserveCapitalization(m.matched, s"keep the $piece anchored on ${m.group(1).toLowerCase}"))
            .getOrElse(m.matched)
        )

      pointing.replaceAllIn(step1, m =>
        occupantLabel(m.group(1).toLowerCase)
          .map(piece => preserveCapitalization(m.matched, s"the $piece already anchored on ${m.group(1).toLowerCase}"))
          .getOrElse(m.matched)
      )
    }

  private def roleToken(role: _root_.chess.Role): String =
    role match
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case Pawn   => "P"
      case _      => "K"
