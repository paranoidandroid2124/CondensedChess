package lila.llm.analysis

import lila.llm.{ DirectionalTargetReadiness, GameChronicleMoment, RouteSurfaceMode, StrategyPack }
import lila.llm.model.*
import lila.llm.model.strategic.{ VariationLine, VariationTag }

private[llm] enum PlayerFacingTruthMode:
  case Minimal
  case Tactical
  case Strategic

private[llm] enum PlayerFacingSacrificeClass:
  case TacticalSacrifice
  case StrategicSacrifice
  case None

private[llm] enum PlayerFacingMoveDeltaClass:
  case NewAccess
  case PressureIncrease
  case ExchangeForcing
  case CounterplayReduction
  case ResourceRemoval
  case PlanAdvance

private[llm] final case class PlayerFacingMoveDeltaEvidence(
    deltaClass: PlayerFacingMoveDeltaClass,
    anchorTerms: List[String],
    quantifier: PlayerFacingClaimQuantifier = PlayerFacingClaimQuantifier.Existential,
    modalityTier: PlayerFacingClaimModalityTier = PlayerFacingClaimModalityTier.Available,
    attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.StateOnly,
    stabilityGrade: PlayerFacingClaimStabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
    provenanceClass: PlayerFacingClaimProvenanceClass = PlayerFacingClaimProvenanceClass.Deferred,
    certificateStatus: PlayerFacingCertificateStatus = PlayerFacingCertificateStatus.Invalid,
    taintFlags: Set[PlayerFacingClaimTaintFlag] = Set.empty,
    ontologyFamily: PlayerFacingClaimOntologyFamily = PlayerFacingClaimOntologyFamily.Unknown,
    connectorPermission: Boolean = false
):
  def allowsStrongMainClaim: Boolean =
    PlayerFacingClaimCertification.allowsStrongMainClaim(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      attribution = attributionGrade,
      stability = stabilityGrade,
      provenance = provenanceClass,
      taintFlags = taintFlags
    )

  def allowsWeakMainClaim: Boolean =
    PlayerFacingClaimCertification.allowsWeakMainClaim(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      attribution = attributionGrade,
      stability = stabilityGrade,
      provenance = provenanceClass,
      taintFlags = taintFlags
    )

  def allowsLineEvidenceHook: Boolean =
    PlayerFacingClaimCertification.allowsLineEvidenceHook(
      certificateStatus = certificateStatus,
      provenance = provenanceClass,
      taintFlags = taintFlags
    )

private[llm] object PlayerFacingTruthModePolicy:

  private val abstractShellTokens = List(
    "opencenter",
    "fluidcenter",
    "carlsbad",
    "iqpblack",
    "iqpwhite",
    "minority attack",
    "isolated queen pawn",
    "hanging pawns"
  )

  def classify(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): PlayerFacingTruthMode =
    val surface = StrategyPackSurface.from(strategyPack)
    val sacrificeClass = classifySacrifice(ctx, surface, truthContract)
    val forcingProof = forcingProofAvailable(ctx.engineEvidence.toList.flatMap(_.variations))
    val deltaEvidence = strategicDeltaEvidence(ctx, surface, truthContract)
    if isTactical(truthContract, sacrificeClass, forcingProof) then PlayerFacingTruthMode.Tactical
    else if sacrificeClass == PlayerFacingSacrificeClass.StrategicSacrifice || deltaEvidence.nonEmpty
    then
      PlayerFacingTruthMode.Strategic
    else if StandardCommentaryClaimPolicy.quietStandardPosition(ctx, truthContract) then
      PlayerFacingTruthMode.Minimal
    else PlayerFacingTruthMode.Minimal

  def classify(moment: GameChronicleMoment): PlayerFacingTruthMode =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val sacrificeClass = classifySacrifice(moment, surface)
    val deltaEvidence = strategicDeltaEvidence(moment, surface)
    if isTactical(moment, sacrificeClass) then PlayerFacingTruthMode.Tactical
    else if sacrificeClass == PlayerFacingSacrificeClass.StrategicSacrifice || deltaEvidence.nonEmpty
    then PlayerFacingTruthMode.Strategic
    else PlayerFacingTruthMode.Minimal

  def allowsActiveNote(moment: GameChronicleMoment): Boolean =
    classify(moment) != PlayerFacingTruthMode.Minimal

  def allowsStrategicClaimText(
      text: String,
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    val surface = StrategyPackSurface.from(strategyPack)
    val deltaEvidence = mainPathMoveDeltaEvidence(ctx, surface, truthContract)
    classify(ctx, strategyPack, truthContract) == PlayerFacingTruthMode.Strategic &&
      deltaEvidence.exists(delta =>
        delta.allowsWeakMainClaim &&
        !overpromotedStrategicFormula(text) &&
          !containsUnsupportedConnector(text, delta) &&
          strategicTextIsConcrete(text, ctx) &&
          strategicTextMatchesMoveLinkedAnchor(text, surface, truthContract) &&
          strategicTextMatchesDelta(text, delta) &&
          hasConcreteStrategicEvidence(ctx, surface, truthContract)
      )

  def allowsStrategicSupportText(
      text: String,
      claim: String,
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    val surface = StrategyPackSurface.from(strategyPack)
    mainPathMoveDeltaEvidence(ctx, surface, truthContract).exists { delta =>
      delta.allowsLineEvidenceHook &&
        !NarrativeDedupCore.sameSemanticSentence(text, claim) &&
        !overpromotedStrategicFormula(text) &&
        (
          if LineScopedCitation.hasConcreteSanLine(text) then
            lineShowsStrategicDelta(text, delta)
          else
            strategicTextIsConcrete(text, ctx) &&
              strategicTextMatchesMoveLinkedAnchor(text, surface, truthContract) &&
              strategicTextMatchesDelta(text, delta)
        )
    }

  def isDrawResult(result: String): Boolean =
    Option(result).map(_.trim).exists(r => r == "1/2-1/2")

  def allowWholeGameDecisiveNarrative(result: String): Boolean =
    !isDrawResult(result)

  def minimalFocusSentence(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    minimalMoveSentence(ctx)
      .orElse(StandardCommentaryClaimPolicy.noEventNote(ctx, truthContract))

  def minimalBookmakerSentence(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    minimalFocusSentence(ctx, truthContract)

  def tacticalLeadSentence(
      ctx: NarrativeContext,
      truthContract: Option[DecisiveTruthContract],
      strategyPack: Option[StrategyPack] = None
  ): Option[String] =
    val sacrificeClass = classifySacrifice(ctx, StrategyPackSurface.from(strategyPack), truthContract)
    val forcingProof = forcingProofAvailable(ctx.engineEvidence.toList.flatMap(_.variations))
    truthContract.flatMap(contract => tacticalLeadFromContract(contract, forcingProof))
      .orElse(
        Option.when(sacrificeClass == PlayerFacingSacrificeClass.TacticalSacrifice) {
          "This is a tactical sacrifice, and the immediate point has to come first."
        }
      )

  def tacticalLeadSentence(moment: GameChronicleMoment): Option[String] =
    val surface = StrategyPackSurface.from(moment.strategyPack)
    val sacrificeClass = classifySacrifice(moment, surface)
    if sacrificeClass == PlayerFacingSacrificeClass.TacticalSacrifice then
      Some("This is a tactical sacrifice, and the immediate point has to come first.")
    else
      moment.moveClassification.map(normalize).collect {
        case "blunder"   => "This is a blunder, and the tactical point has to come first."
        case "missedwin" => "This misses a win, and the immediate tactical chance matters most."
      }

  def activeMoveDeltaEvidence(
      moment: GameChronicleMoment
  ): Option[PlayerFacingMoveDeltaEvidence] =
    strategicDeltaEvidence(moment, StrategyPackSurface.from(moment.strategyPack))

  def mainPathMoveDeltaEvidence(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Option[PlayerFacingMoveDeltaEvidence] =
    val anchors = moveLinkedAnchorTerms(surface, truthContract)
    if anchors.isEmpty || surfaceLooksShellOnly(surface) || !hasConcreteStrategicEvidence(ctx, surface, truthContract)
    then None
    else
      val squareAnchors = extractSquareAnchors(anchors)
      val decisionDelta = ctx.decision.map(_.delta)
      val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
      val bestLine = ctx.engineEvidence.toList.flatMap(_.variations).headOption
      def result(kind: PlayerFacingMoveDeltaClass): Option[PlayerFacingMoveDeltaEvidence] =
        Some(
          certifyMainPathDelta(
            ctx = ctx,
            surface = surface,
            deltaClass = kind,
            anchors = anchors,
            preventedNow = preventedNow
          )
        )

      if hasMainPathSpecificResourceRemoval(ctx, preventedNow, bestLine) then
        result(PlayerFacingMoveDeltaClass.ResourceRemoval)
      else if hasMainPathExchangeForcing(ctx, bestLine, squareAnchors) then
        result(PlayerFacingMoveDeltaClass.ExchangeForcing)
      else if hasMainPathCounterplayReduction(preventedNow) then
        result(PlayerFacingMoveDeltaClass.CounterplayReduction)
      else if hasMainPathNewAccess(ctx, surface, decisionDelta) then
        result(PlayerFacingMoveDeltaClass.NewAccess)
      else if hasMainPathPressureIncrease(ctx, surface, decisionDelta) then
        result(PlayerFacingMoveDeltaClass.PressureIncrease)
      else if hasMainPathPlanAdvance(surface, decisionDelta) then
        result(PlayerFacingMoveDeltaClass.PlanAdvance)
      else None

  def mainPathAnchorTerms(
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    moveLinkedAnchorTerms(surface, truthContract)

  def lineShowsMainPathDelta(
      text: String,
      delta: PlayerFacingMoveDeltaEvidence
  ): Boolean =
    val low = normalize(text)
    val squareAnchors = extractSquareAnchors(delta.anchorTerms)
    delta.deltaClass match
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        val exchangeHit = containsAny(low, List(" x", "trade", "exchange", "simplif", "capture"))
        exchangeHit && lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        val removalHit = containsAny(low, List("prevent", "deny", "stop", "no longer", "can t", "cannot", "loses", "limit"))
        removalHit && lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        containsAny(low, List("prevent", "deny", "keep", "stop", "counterplay", "break", "restrain")) &&
          lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        containsAny(low, List("pressure", "target", "attack", "initiative", "clamp")) &&
          lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)
      case PlayerFacingMoveDeltaClass.NewAccess =>
        containsAny(low, List("open", "access", "entry", "route", "line", "file", "diagonal", "square")) &&
          lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)
      case PlayerFacingMoveDeltaClass.PlanAdvance =>
        containsAny(low, List("advance", "prepare", "improve", "activate", "support", "coordinate")) &&
          lineTouchesAnchor(low, squareAnchors, delta.anchorTerms)

  private def isTactical(
      truthContract: Option[DecisiveTruthContract],
      sacrificeClass: PlayerFacingSacrificeClass,
      forcingProof: Boolean
  ): Boolean =
    sacrificeClass == PlayerFacingSacrificeClass.TacticalSacrifice ||
      truthContract.exists(contract =>
        contractOwnsDirectTacticalTruth(contract) ||
          (forcingProof && contractClaimsForcingTacticalTruth(contract))
      )

  private def isTactical(
      moment: GameChronicleMoment,
      sacrificeClass: PlayerFacingSacrificeClass
  ): Boolean =
    sacrificeClass == PlayerFacingSacrificeClass.TacticalSacrifice ||
      moment.moveClassification.exists(label =>
        Set("blunder", "missedwin").contains(label.trim.toLowerCase)
      )

  private def classifySacrifice(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): PlayerFacingSacrificeClass =
    val investedMaterial =
      surface.investedMaterial
        .orElse(ctx.semantic.flatMap(_.compensation.map(_.investedMaterial)))
        .filter(_ > 0)
    if !looksLikeSacrifice(investedMaterial, truthContract) then PlayerFacingSacrificeClass.None
    else if variationShowsImmediateTacticalSettlement(ctx.engineEvidence.toList.flatMap(_.variations)) then
      PlayerFacingSacrificeClass.TacticalSacrifice
    else if hasStrategicSacrificeEvidence(ctx, surface, truthContract) then
      PlayerFacingSacrificeClass.StrategicSacrifice
    else PlayerFacingSacrificeClass.None

  private def classifySacrifice(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): PlayerFacingSacrificeClass =
    val investedMaterial =
      moment.signalDigest.flatMap(_.investedMaterial)
        .orElse(surface.investedMaterial)
        .filter(_ > 0)
    if !looksLikeSacrifice(investedMaterial, None) then PlayerFacingSacrificeClass.None
    else if variationShowsImmediateTacticalSettlement(moment.variations) then
      PlayerFacingSacrificeClass.TacticalSacrifice
    else if hasStrategicSacrificeEvidence(moment, surface) then
      PlayerFacingSacrificeClass.StrategicSacrifice
    else PlayerFacingSacrificeClass.None

  private def looksLikeSacrifice(
      investedMaterial: Option[Int],
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    investedMaterial.exists(_ >= 100) ||
      truthContract.exists(contract =>
        contract.reasonFamily == DecisiveReasonFamily.InvestmentSacrifice ||
          contract.truthClass == DecisiveTruthClass.WinningInvestment ||
          contract.truthClass == DecisiveTruthClass.CompensatedInvestment
      )

  private def contractOwnsDirectTacticalTruth(contract: DecisiveTruthContract): Boolean =
    contract.truthClass == DecisiveTruthClass.Blunder ||
      contract.truthClass == DecisiveTruthClass.MissedWin

  private def contractClaimsForcingTacticalTruth(contract: DecisiveTruthContract): Boolean =
    (contract.truthClass == DecisiveTruthClass.Best &&
      (
        contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
          contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation
      )) ||
      contract.failureMode == FailureInterpretationMode.TacticalRefutation

  private def tacticalLeadFromContract(
      contract: DecisiveTruthContract,
      forcingProof: Boolean
  ): Option[String] =
    contract.truthClass match
      case DecisiveTruthClass.Blunder =>
        Some("This is a blunder, and the tactical point has to come first.")
      case DecisiveTruthClass.MissedWin =>
        Some("This misses a win, and the immediate tactical chance matters most.")
      case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense && forcingProof =>
        Some("This is the only move that keeps the position together.")
      case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation && forcingProof =>
        Some("This is the clean tactical refutation.")
      case _ =>
        None

  private def hasStrategicSacrificeEvidence(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    val moveLinkedCompensation = hasMoveLinkedCompensationEvidence(surface)
    val semanticCompensation =
      ctx.semantic.exists(semantic =>
        semantic.compensation.exists(comp =>
          comp.returnVector.nonEmpty || Option(comp.conversionPlan).exists(_.trim.nonEmpty)
        )
      )
    val verifiedCompensation =
      truthContract.exists(contract =>
        verifiedAnchorMatchesSurface(surface, contract.verifiedPayoffAnchor) &&
          (
            contract.reasonFamily == DecisiveReasonFamily.InvestmentSacrifice ||
              contract.truthClass == DecisiveTruthClass.WinningInvestment ||
              contract.truthClass == DecisiveTruthClass.CompensatedInvestment ||
              contract.surfaceMode == TruthSurfaceMode.InvestmentExplain ||
              contract.surfaceMode == TruthSurfaceMode.ConversionExplain
          )
      )
    moveLinkedCompensation && (semanticCompensation || verifiedCompensation)

  private def hasStrategicSacrificeEvidence(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    hasMoveLinkedCompensationEvidence(surface) &&
      moment.signalDigest.exists(digest =>
        digest.compensation.nonEmpty || digest.decisionComparison.nonEmpty
      )

  private def variationShowsImmediateTacticalSettlement(
      variations: List[VariationLine]
  ): Boolean =
    variations.headOption.exists { variation =>
      val window = variation.parsedMoves.take(4)
      val captureCount = window.count(_.isCapture)
      val checkCount = window.count(_.givesCheck)
      variation.mate.nonEmpty ||
      variation.tags.contains(VariationTag.Forced) ||
      (captureCount >= 2) ||
      (checkCount >= 2) ||
      (captureCount >= 1 && checkCount >= 1)
    }

  private def forcingProofAvailable(
      variations: List[VariationLine]
  ): Boolean =
    variations.headOption.exists { variation =>
      val window = variation.parsedMoves.take(4)
      val captureCount = window.count(_.isCapture)
      val checkCount = window.count(_.givesCheck)
      variation.mate.nonEmpty ||
      variation.tags.contains(VariationTag.Forced) ||
      (checkCount >= 2) ||
      (captureCount >= 1 && checkCount >= 1)
    }

  private def hasConcreteStrategicEvidence(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    val moveLinkedAnchor = hasMoveLinkedStrategicAnchor(surface)
    val semanticConcrete =
      ctx.decision.exists(decision =>
        decision.delta.resolvedThreats.nonEmpty ||
          decision.delta.newOpportunities.nonEmpty ||
          decision.delta.planAdvancements.nonEmpty
      ) ||
        ctx.semantic.exists { semantic =>
          semantic.preventedPlans.exists(_.sourceScope == FactScope.Now) ||
          semantic.structuralWeaknesses.nonEmpty ||
          semantic.positionalFeatures.nonEmpty ||
          semantic.compensation.exists(comp =>
            comp.returnVector.nonEmpty || Option(comp.conversionPlan).exists(_.trim.nonEmpty)
          )
        }
    val truthBacked =
      truthContract.exists(contract => verifiedAnchorMatchesSurface(surface, contract.verifiedPayoffAnchor))
    moveLinkedAnchor &&
      (semanticConcrete || truthBacked) &&
      !surfaceLooksShellOnly(surface)

  private def hasConcreteStrategicEvidence(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val digestConcrete =
      moment.signalDigest.exists { digest =>
        digest.decisionComparison.nonEmpty ||
          digest.compensation.nonEmpty ||
          digest.deploymentRoute.nonEmpty ||
          digest.deploymentPurpose.nonEmpty ||
          digest.prophylaxisThreat.nonEmpty
      }
    hasMoveLinkedStrategicAnchor(surface) &&
      digestConcrete &&
      !surfaceLooksShellOnly(surface)

  private def strategicDeltaEvidence(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Option[PlayerFacingMoveDeltaEvidence] =
    val anchors = moveLinkedAnchorTerms(surface, truthContract)
    if anchors.isEmpty || surfaceLooksShellOnly(surface) || !hasConcreteStrategicEvidence(ctx, surface, truthContract)
    then None
    else
      val delta = ctx.delta
      val decisionDelta = ctx.decision.map(_.delta)
      val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
      val route = surface.topRoute
      val target = surface.topDirectionalTarget
      val moveRef = surface.topMoveRef
      val routeEvidence = route.toList.flatMap(_.evidence).map(normalize)
      val targetEvidence = target.toList.flatMap(_.evidence).map(normalize)
      val moveRefEvidence = moveRef.toList.flatMap(_.evidence).map(normalize)
      val allEvidence =
        (surface.evidenceHints.map(normalize) ++ routeEvidence ++ targetEvidence ++ moveRefEvidence).filter(_.nonEmpty)
      val newMotifs = delta.toList.flatMap(_.newMotifs.map(normalize))
      val lostMotifs = delta.toList.flatMap(_.lostMotifs.map(normalize))
      val resolvedThreats = decisionDelta.toList.flatMap(_.resolvedThreats.map(normalize))
      val opportunities = decisionDelta.toList.flatMap(_.newOpportunities.map(normalize))
      val planAdvancements = decisionDelta.toList.flatMap(_.planAdvancements.map(normalize))
      val exchangeMoveRef =
        moveRef.exists(ref =>
          ref.tacticalTheme.exists(theme => normalize(theme).contains("exchange")) ||
            normalize(ref.idea).contains("exchange") ||
            normalize(ref.idea).contains("trade") ||
            normalize(ref.idea).contains("simplif")
        )
      def result(kind: PlayerFacingMoveDeltaClass): Option[PlayerFacingMoveDeltaEvidence] =
        Some(PlayerFacingMoveDeltaEvidence(kind, anchors))

      if preventedNow.exists(plan =>
          plan.counterplayScoreDrop > 0 ||
            plan.mobilityDelta < 0 ||
            plan.breakNeutralized.exists(_.trim.nonEmpty) ||
            plan.deniedSquares.nonEmpty
        )
      then result(PlayerFacingMoveDeltaClass.CounterplayReduction)
      else if resolvedThreats.exists(text =>
            containsAny(text, List("defend", "resource", "counterplay", "guard", "escape", "hold", "cover"))
          ) ||
          lostMotifs.exists(text =>
            containsAny(text, List("defend", "resource", "counterplay", "guard", "escape", "hold"))
          )
      then result(PlayerFacingMoveDeltaClass.ResourceRemoval)
      else if exchangeMoveRef ||
          opportunities.exists(text => containsAny(text, List("exchange", "trade", "simplif", "capture"))) ||
          planAdvancements.exists(text => containsAny(text, List("exchange", "trade", "simplif")))
      then result(PlayerFacingMoveDeltaClass.ExchangeForcing)
      else if delta.flatMap(_.openFileCreated).exists(_.trim.nonEmpty) ||
          opportunities.exists(text =>
            containsAny(text, List("open file", "open line", "diagonal", "access", "outpost", "entry square"))
          ) ||
          (route.exists(_.surfaceMode == RouteSurfaceMode.Exact) &&
            planAdvancements.nonEmpty &&
            allEvidence.exists(text => text.contains("deployment contribution")))
      then result(PlayerFacingMoveDeltaClass.NewAccess)
      else if opportunities.exists(text => containsAny(text, List("pressure", "target", "attack", "initiative", "clamp"))) ||
          newMotifs.exists(text => containsAny(text, List("pressure", "attack", "pin", "x ray", "battery", "initiative"))) ||
          target.exists(_.strategicReasons.exists(reason => containsAny(normalize(reason), List("pressure", "target", "attack"))))
      then result(PlayerFacingMoveDeltaClass.PressureIncrease)
      else if planAdvancements.nonEmpty ||
          allEvidence.exists(text => text.contains("deployment contribution"))
      then result(PlayerFacingMoveDeltaClass.PlanAdvance)
      else None

  private def hasMainPathSpecificResourceRemoval(
      ctx: NarrativeContext,
      preventedNow: List[PreventedPlanInfo],
      bestLine: Option[VariationLine]
  ): Boolean =
    preventedNow.exists { plan =>
      val resourceIdentity =
        plan.preventedThreatType.exists(_.trim.nonEmpty) ||
          plan.breakNeutralized.exists(_.trim.nonEmpty) ||
          plan.deniedSquares.nonEmpty
      val alternativeEvidence =
        ctx.meta.flatMap(_.whyNot).exists(text => whyNotMentionsPreventedResource(text, plan)) ||
          ctx.candidates.drop(1).exists(candidate =>
            candidate.whyNot.exists(text => whyNotMentionsPreventedResource(text, plan))
          )
      val lineEvidence =
        plan.citationLine.exists(line => citationShowsSpecificResourceLoss(line, plan)) ||
          bestLine.exists(line => variationShowsSpecificResourceLoss(line, plan))
      resourceIdentity && (alternativeEvidence || lineEvidence)
    }

  private def hasMainPathExchangeForcing(
      ctx: NarrativeContext,
      bestLine: Option[VariationLine],
      squareAnchors: List[String]
  ): Boolean =
    val provingLineShowsExchange =
      bestLine.exists(line => variationShowsAnchoredExchange(line, squareAnchors))
    val moveLinkedExchangeEvidence =
      whyNotSources(ctx).exists(text => textShowsAnchoredExchange(text, squareAnchors)) ||
        decisionDeltaExchangeEvidence(ctx).exists(text => textShowsAnchoredExchange(text, squareAnchors))
    squareAnchors.nonEmpty && provingLineShowsExchange && moveLinkedExchangeEvidence

  private def hasMainPathCounterplayReduction(
      preventedNow: List[PreventedPlanInfo]
  ): Boolean =
    preventedNow.exists(plan =>
      (
        plan.counterplayScoreDrop > 0 ||
          plan.mobilityDelta < 0
      ) && (
        plan.breakNeutralized.exists(_.trim.nonEmpty) ||
          plan.deniedSquares.nonEmpty
      )
    )

  private def hasMainPathNewAccess(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      decisionDelta: Option[PVDelta]
  ): Boolean =
    ctx.delta.flatMap(_.openFileCreated).exists(_.trim.nonEmpty) ||
      decisionDelta.exists(delta =>
        delta.newOpportunities.exists(text =>
          containsAny(normalize(text), List("open file", "open line", "diagonal", "access", "outpost", "entry square"))
        )
      ) ||
      (
        surface.topRoute.exists(_.surfaceMode == RouteSurfaceMode.Exact) &&
          decisionDelta.exists(_.planAdvancements.nonEmpty) &&
          surface.topRoute.exists(route =>
            containsAny(
              normalize(route.purpose),
              List("open", "access", "entry", "route", "line", "file", "diagonal", "outpost")
            )
          )
      )

  private def hasMainPathPressureIncrease(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      decisionDelta: Option[PVDelta]
  ): Boolean =
    val deltaSignals =
      decisionDelta.exists(delta =>
        delta.newOpportunities.exists(text => containsAny(normalize(text), List("pressure", "target", "attack", "initiative", "clamp"))) ||
          ctx.delta.exists(_.newMotifs.exists(text => containsAny(normalize(text), List("pressure", "attack", "pin", "x ray", "battery", "initiative"))))
      )
    val anchoredTarget =
      surface.topDirectionalTarget.exists(target =>
        target.targetSquare.trim.nonEmpty &&
          target.evidence.nonEmpty &&
          target.strategicReasons.exists(reason => containsAny(normalize(reason), List("pressure", "target", "attack")))
      )
    val compensationSignal =
      ctx.semantic.exists(_.compensation.exists(comp =>
        comp.returnVector.keys.exists(key => containsAny(normalize(key), List("attack", "initiative", "king")))
      )) &&
        anchoredTarget
    (deltaSignals && anchoredTarget) || compensationSignal

  private def hasMainPathPlanAdvance(
      surface: StrategyPackSurface.Snapshot,
      decisionDelta: Option[PVDelta]
  ): Boolean =
    val deltaSignals =
      decisionDelta.exists(_.planAdvancements.nonEmpty)
    val moveLinkedRoute =
      surface.topRoute.exists(route =>
        route.surfaceMode == RouteSurfaceMode.Exact &&
          route.evidence.nonEmpty &&
          route.route.size >= 2
      ) ||
        surface.topMoveRef.exists(ref =>
          ref.target.trim.nonEmpty &&
            ref.evidence.nonEmpty &&
            !isAbstractStrategicText(ref.idea)
        )
    deltaSignals && moveLinkedRoute

  private def certifyMainPathDelta(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      anchors: List[String],
      preventedNow: List[PreventedPlanInfo]
  ): PlayerFacingMoveDeltaEvidence =
    val ontologyFamily = ontologyFamilyForDelta(deltaClass, ctx, preventedNow)
    val provenanceClass = claimProvenance(ctx)
    val quantifier = quantifierForDelta(ctx, provenanceClass)
    val stabilityGrade = stabilityForDelta(ctx, provenanceClass)
    val attributionGrade =
      attributionForDelta(
        ctx = ctx,
        anchors = anchors,
        deltaClass = deltaClass,
        ontologyFamily = ontologyFamily
      )
    val taintFlags =
      claimTaintFlags(
        ctx = ctx,
        provenanceClass = provenanceClass,
        quantifier = quantifier
      )
    val certificateStatus =
      certificateStatusForDelta(
        provenanceClass = provenanceClass,
        quantifier = quantifier,
        attributionGrade = attributionGrade,
        stabilityGrade = stabilityGrade,
        taintFlags = taintFlags
      )
    PlayerFacingMoveDeltaEvidence(
      deltaClass = deltaClass,
      anchorTerms = anchors,
      quantifier = quantifier,
      modalityTier = modalityForDelta(deltaClass, quantifier, stabilityGrade, ontologyFamily),
      attributionGrade = attributionGrade,
      stabilityGrade = stabilityGrade,
      provenanceClass = provenanceClass,
      certificateStatus = certificateStatus,
      taintFlags = taintFlags,
      ontologyFamily = ontologyFamily,
      connectorPermission = false
    )

  private def claimProvenance(
      ctx: NarrativeContext
  ): PlayerFacingClaimProvenanceClass =
    val evidenceBacked =
      StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).nonEmpty ||
        ctx.strategicPlanExperiments.exists(_.evidenceTier == "evidence_backed")
    val pvCoupled =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "pv_coupled")
    val deferred =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "deferred") ||
        ctx.probeRequests.nonEmpty
    if evidenceBacked then PlayerFacingClaimProvenanceClass.ProbeBacked
    else if pvCoupled then PlayerFacingClaimProvenanceClass.PvCoupled
    else if deferred then PlayerFacingClaimProvenanceClass.Deferred
    else PlayerFacingClaimProvenanceClass.StructuralOnly

  private def quantifierForDelta(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimQuantifier =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimQuantifier.LineConditioned
    else
      val evidenceBackedExperiments =
        ctx.strategicPlanExperiments.filter(_.evidenceTier == "evidence_backed")
      if evidenceBackedExperiments.exists(exp =>
          exp.bestReplyStable && exp.futureSnapshotAligned && !exp.moveOrderSensitive
        ) then
        PlayerFacingClaimQuantifier.Universal
      else if evidenceBackedExperiments.exists(exp => exp.bestReplyStable || exp.futureSnapshotAligned) then
        PlayerFacingClaimQuantifier.BestResponse
      else PlayerFacingClaimQuantifier.Existential

  private def stabilityForDelta(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimStabilityGrade =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimStabilityGrade.Unknown
    else
      val evidenceBackedExperiments =
        ctx.strategicPlanExperiments.filter(_.evidenceTier == "evidence_backed")
      if evidenceBackedExperiments.exists(exp =>
          (exp.bestReplyStable || exp.futureSnapshotAligned) && !exp.moveOrderSensitive
        ) then PlayerFacingClaimStabilityGrade.Stable
      else if evidenceBackedExperiments.nonEmpty then PlayerFacingClaimStabilityGrade.Unstable
      else PlayerFacingClaimStabilityGrade.Unknown

  private def claimTaintFlags(
      ctx: NarrativeContext,
      provenanceClass: PlayerFacingClaimProvenanceClass,
      quantifier: PlayerFacingClaimQuantifier
  ): Set[PlayerFacingClaimTaintFlag] =
    List(
      Option.when(ctx.latentPlans.nonEmpty)(PlayerFacingClaimTaintFlag.Latent),
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.PvCoupled)(PlayerFacingClaimTaintFlag.PvCoupled),
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.Deferred)(PlayerFacingClaimTaintFlag.Deferred),
      Option.when(provenanceClass == PlayerFacingClaimProvenanceClass.StructuralOnly)(PlayerFacingClaimTaintFlag.StructuralOnly),
      Option.when(quantifier == PlayerFacingClaimQuantifier.LineConditioned)(PlayerFacingClaimTaintFlag.BranchConditioned)
    ).flatten.toSet

  private def certificateStatusForDelta(
      provenanceClass: PlayerFacingClaimProvenanceClass,
      quantifier: PlayerFacingClaimQuantifier,
      attributionGrade: PlayerFacingClaimAttributionGrade,
      stabilityGrade: PlayerFacingClaimStabilityGrade,
      taintFlags: Set[PlayerFacingClaimTaintFlag]
  ): PlayerFacingCertificateStatus =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingCertificateStatus.Invalid
    else if attributionGrade == PlayerFacingClaimAttributionGrade.StateOnly then
      PlayerFacingCertificateStatus.Invalid
    else if PlayerFacingClaimCertification.blocksMainClaim(taintFlags) then
      PlayerFacingCertificateStatus.Invalid
    else if
      quantifier == PlayerFacingClaimQuantifier.Universal &&
        stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable &&
        attributionGrade == PlayerFacingClaimAttributionGrade.Distinctive
    then PlayerFacingCertificateStatus.Valid
    else if
      quantifier == PlayerFacingClaimQuantifier.BestResponse &&
        attributionGrade != PlayerFacingClaimAttributionGrade.StateOnly
    then PlayerFacingCertificateStatus.WeaklyValid
    else PlayerFacingCertificateStatus.WeaklyValid

  private def modalityForDelta(
      deltaClass: PlayerFacingMoveDeltaClass,
      quantifier: PlayerFacingClaimQuantifier,
      stabilityGrade: PlayerFacingClaimStabilityGrade,
      ontologyFamily: PlayerFacingClaimOntologyFamily
  ): PlayerFacingClaimModalityTier =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess =>
        if quantifier == PlayerFacingClaimQuantifier.Universal then PlayerFacingClaimModalityTier.Advances
        else PlayerFacingClaimModalityTier.Available
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        if quantifier == PlayerFacingClaimQuantifier.Universal then PlayerFacingClaimModalityTier.Supports
        else PlayerFacingClaimModalityTier.Available
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        if quantifier == PlayerFacingClaimQuantifier.Universal && stabilityGrade == PlayerFacingClaimStabilityGrade.Stable
        then PlayerFacingClaimModalityTier.Forces
        else PlayerFacingClaimModalityTier.Available
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        if ontologyFamily == PlayerFacingClaimOntologyFamily.RouteDenial then PlayerFacingClaimModalityTier.Removes
        else if quantifier == PlayerFacingClaimQuantifier.Universal then PlayerFacingClaimModalityTier.Supports
        else PlayerFacingClaimModalityTier.Available
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        if quantifier != PlayerFacingClaimQuantifier.Existential then PlayerFacingClaimModalityTier.Removes
        else PlayerFacingClaimModalityTier.Supports
      case PlayerFacingMoveDeltaClass.PlanAdvance =>
        if quantifier != PlayerFacingClaimQuantifier.Existential then PlayerFacingClaimModalityTier.Advances
        else PlayerFacingClaimModalityTier.Supports

  private def ontologyFamilyForDelta(
      deltaClass: PlayerFacingMoveDeltaClass,
      ctx: NarrativeContext,
      preventedNow: List[PreventedPlanInfo]
  ): PlayerFacingClaimOntologyFamily =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess => PlayerFacingClaimOntologyFamily.Access
      case PlayerFacingMoveDeltaClass.PressureIncrease => PlayerFacingClaimOntologyFamily.Pressure
      case PlayerFacingMoveDeltaClass.ExchangeForcing => PlayerFacingClaimOntologyFamily.Exchange
      case PlayerFacingMoveDeltaClass.PlanAdvance => PlayerFacingClaimOntologyFamily.PlanAdvance
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        if looksLikeRouteDenial(ctx, preventedNow) then PlayerFacingClaimOntologyFamily.RouteDenial
        else PlayerFacingClaimOntologyFamily.ResourceRemoval
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        if looksLikeRouteDenial(ctx, preventedNow) then PlayerFacingClaimOntologyFamily.RouteDenial
        else if looksLikeColorComplexSqueeze(ctx) then PlayerFacingClaimOntologyFamily.ColorComplexSqueeze
        else PlayerFacingClaimOntologyFamily.LongTermRestraint

  private def looksLikeRouteDenial(
      ctx: NarrativeContext,
      preventedNow: List[PreventedPlanInfo]
  ): Boolean =
    val texts =
      (ctx.meta.flatMap(_.whyNot).toList ++
        preventedNow.flatMap(_.citationLine) ++
        ctx.candidates.flatMap(_.whyNot)).map(normalize)
    preventedNow.exists(_.deniedSquares.nonEmpty) &&
      texts.exists(text => containsAny(text, List("entry", "route", "route denial", "access", "out of")))

  private def looksLikeColorComplexSqueeze(ctx: NarrativeContext): Boolean =
    ctx.semantic.exists(semantic =>
      semantic.structuralWeaknesses.exists(weakness =>
        weakness.squares.nonEmpty &&
          (containsAny(normalize(weakness.cause), List("color", "bishop", "complex")) ||
            containsAny(normalize(weakness.squareColor), List("light", "dark")))
      )
    )

  private def attributionForDelta(
      ctx: NarrativeContext,
      anchors: List[String],
      deltaClass: PlayerFacingMoveDeltaClass,
      ontologyFamily: PlayerFacingClaimOntologyFamily
  ): PlayerFacingClaimAttributionGrade =
    val anchorTokens = anchors.map(normalize).filter(_.length >= 2).distinct
    if anchorTokens.isEmpty then PlayerFacingClaimAttributionGrade.StateOnly
    else
      val siblings = ctx.candidates.drop(1).take(3)
      if siblings.isEmpty then PlayerFacingClaimAttributionGrade.Distinctive
      else
        val keywords = deltaKeywords(deltaClass, ontologyFamily)
        val currentPiece = pieceLabelFromSan(ctx.playedSan)
        val shared = siblings.exists { candidate =>
          val candidateText = candidateSemanticText(candidate)
          val anchorHit = anchorTokens.exists(candidateText.contains)
          val keywordHits = keywords.count(candidateText.contains)
          val pieceShared =
            currentPiece.nonEmpty &&
              pieceLabelFromSan(Some(candidate.move)).contains(currentPiece.get)
          (anchorHit && keywordHits >= 1) || (pieceShared && keywordHits >= 2)
        }
        if shared then PlayerFacingClaimAttributionGrade.AnchoredButShared
        else PlayerFacingClaimAttributionGrade.Distinctive

  private def candidateSemanticText(candidate: CandidateInfo): String =
    normalize(
      List(
        candidate.move,
        candidate.planAlignment,
        candidate.structureGuidance.getOrElse(""),
        candidate.whyNot.getOrElse("")
      ).mkString(" ") +
        " " +
        candidate.probeLines.mkString(" ") +
        " " +
        candidate.lineSanMoves.mkString(" ")
    )

  private def deltaKeywords(
      deltaClass: PlayerFacingMoveDeltaClass,
      ontologyFamily: PlayerFacingClaimOntologyFamily
  ): List[String] =
    ontologyFamily match
      case PlayerFacingClaimOntologyFamily.RouteDenial =>
        List("entry", "route", "deny", "denial", "access")
      case PlayerFacingClaimOntologyFamily.ColorComplexSqueeze =>
        List("color", "complex", "light", "dark", "bishop")
      case PlayerFacingClaimOntologyFamily.LongTermRestraint =>
        List("counterplay", "restrain", "clamp", "prevent", "deny")
      case _ =>
        deltaClass match
          case PlayerFacingMoveDeltaClass.NewAccess =>
            List("open", "access", "entry", "route", "line", "file", "diagonal", "square")
          case PlayerFacingMoveDeltaClass.PressureIncrease =>
            List("pressure", "target", "attack", "clamp", "initiative")
          case PlayerFacingMoveDeltaClass.ExchangeForcing =>
            List("exchange", "trade", "simplif", "capture")
          case PlayerFacingMoveDeltaClass.CounterplayReduction =>
            List("prevent", "deny", "counterplay", "break", "restrain")
          case PlayerFacingMoveDeltaClass.ResourceRemoval =>
            List("remove", "resource", "deny", "no longer", "defensive")
          case PlayerFacingMoveDeltaClass.PlanAdvance =>
            List("advance", "prepare", "step", "activate", "improve", "coordinate", "support")

  private def strategicDeltaEvidence(
      moment: GameChronicleMoment,
      surface: StrategyPackSurface.Snapshot
  ): Option[PlayerFacingMoveDeltaEvidence] =
    val anchors = moveLinkedAnchorTerms(surface, None)
    if anchors.isEmpty || surfaceLooksShellOnly(surface) || !hasConcreteStrategicEvidence(moment, surface) then None
    else
      moment.signalDigest.flatMap { digest =>
        if digest.prophylaxisThreat.nonEmpty then
          Some(PlayerFacingMoveDeltaEvidence(PlayerFacingMoveDeltaClass.CounterplayReduction, anchors))
        else if digest.decisionComparison.nonEmpty then
          Some(PlayerFacingMoveDeltaEvidence(PlayerFacingMoveDeltaClass.PlanAdvance, anchors))
        else if digest.deploymentRoute.nonEmpty then
          Some(PlayerFacingMoveDeltaEvidence(PlayerFacingMoveDeltaClass.NewAccess, anchors))
        else if digest.compensation.nonEmpty then
          Some(PlayerFacingMoveDeltaEvidence(PlayerFacingMoveDeltaClass.PressureIncrease, anchors))
        else None
      }

  private def hasMoveLinkedStrategicAnchor(surface: StrategyPackSurface.Snapshot): Boolean =
    val routeAnchor =
      surface.topRoute.exists(route =>
        route.surfaceMode == RouteSurfaceMode.Exact &&
          route.route.size >= 2 &&
          route.surfaceConfidence >= 0.8 &&
          route.evidence.nonEmpty &&
          route.purpose.trim.nonEmpty &&
          !isAbstractStrategicText(route.purpose)
      )
    val targetAnchor =
      surface.topDirectionalTarget.exists(target =>
        target.targetSquare.trim.nonEmpty &&
          target.readiness != DirectionalTargetReadiness.Premature &&
          target.evidence.nonEmpty &&
          target.strategicReasons.nonEmpty
      )
    val moveRefAnchor =
      surface.topMoveRef.exists(ref =>
        ref.target.trim.nonEmpty &&
          ref.evidence.nonEmpty &&
          normalize(ref.idea).nonEmpty &&
          !isAbstractStrategicText(ref.idea)
      )
    routeAnchor || targetAnchor || moveRefAnchor

  private def hasMoveLinkedCompensationEvidence(surface: StrategyPackSurface.Snapshot): Boolean =
    surface.compensationPosition &&
      hasMoveLinkedStrategicAnchor(surface)

  private def surfaceLooksShellOnly(surface: StrategyPackSurface.Snapshot): Boolean =
    val texts =
      List(
        surface.dominantIdeaText,
        surface.secondaryIdeaText,
        surface.focusText,
        surface.longTermFocus
      ).flatten.map(normalize).filter(_.nonEmpty)
    texts.nonEmpty &&
      texts.forall(text => abstractShellTokens.exists(text.contains)) &&
      !hasMoveLinkedStrategicAnchor(surface)

  private def strategicTextIsConcrete(text: String, ctx: NarrativeContext): Boolean =
    val low = normalize(text)
    val backedPlans =
      StrategicNarrativePlanSupport
        .evidenceBackedPlanNames(ctx)
        .map(normalize)
        .filter(_.nonEmpty)
    val concrete =
      LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
        low.contains("pressure on ") ||
        low.contains("against ") ||
        low.contains("only while")
    concrete &&
      !abstractShellTokens.exists(low.contains) &&
      (backedPlans.isEmpty || backedPlans.exists(low.contains) || concrete)

  private def strategicTextMatchesMoveLinkedAnchor(
      text: String,
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    val low = normalize(text)
    moveLinkedAnchorTerms(surface, truthContract).exists(anchor =>
      anchor.length >= 2 &&
        (low.contains(anchor) || anchor.contains(low))
    )

  private def moveLinkedAnchorTerms(
      surface: StrategyPackSurface.Snapshot,
      truthContract: Option[DecisiveTruthContract]
  ): List[String] =
    (
      surface.topRoute.toList.flatMap(route =>
        List(normalize(route.purpose)).filter(_.nonEmpty) ++
          route.route.lastOption.map(normalize).filter(_.nonEmpty).toList
      ) ++
        surface.topDirectionalTarget.toList.flatMap(target =>
          List(normalize(target.targetSquare)).filter(_.nonEmpty) ++ target.strategicReasons.map(normalize)
        ).filter(_.nonEmpty) ++
        surface.topMoveRef.toList.flatMap(ref =>
          List(normalize(ref.target), normalize(ref.idea)).filter(_.nonEmpty)
        ) ++
        truthContract.toList.flatMap(_.verifiedPayoffAnchor.map(normalize).filter(_.nonEmpty))
    ).distinct

  private def verifiedAnchorMatchesSurface(
      surface: StrategyPackSurface.Snapshot,
      verifiedAnchor: Option[String]
  ): Boolean =
    verifiedAnchor.exists { rawAnchor =>
      val anchor = normalize(rawAnchor)
      anchor.nonEmpty &&
        moveLinkedAnchorTerms(surface, None).exists(term =>
          term.nonEmpty && (term.contains(anchor) || anchor.contains(term))
        )
    }

  private def overpromotedStrategicFormula(text: String): Boolean =
    val low = normalize(text)
    low.contains("better is ") ||
      low.contains("a concrete target is ") ||
      low.contains("the concrete square is ") ||
      low.contains("a likely follow up is ") ||
      low.contains("a likely follow-up is ") ||
      low.contains("route toward ") ||
      low.contains("headed for ") ||
      low.contains("leans toward ") ||
      low.contains("follows the structure s logic") ||
      low.contains("the current structure points to") ||
      low.contains("better bishop route") ||
      low.contains("better rook route") ||
      low.contains("better knight route") ||
      low.contains("better queen route")

  private def containsUnsupportedConnector(
      text: String,
      delta: PlayerFacingMoveDeltaEvidence
  ): Boolean =
    val low = normalize(text)
    !delta.connectorPermission &&
      List("therefore", "thereby", "which means", "so ", "thus", "by doing so").exists(low.contains)

  private def strategicTextMatchesDelta(
      text: String,
      delta: PlayerFacingMoveDeltaEvidence
  ): Boolean =
    val low = normalize(text)
    val familyMatch =
      delta.deltaClass match
        case PlayerFacingMoveDeltaClass.NewAccess =>
          containsAny(low, List("open", "access", "entry", "route", "line", "file", "diagonal", "square"))
        case PlayerFacingMoveDeltaClass.PressureIncrease =>
          containsAny(low, List("pressure", "target", "attack", "clamp", "initiative"))
        case PlayerFacingMoveDeltaClass.ExchangeForcing =>
          containsAny(low, List("exchange", "trade", "simplif", "capture"))
        case PlayerFacingMoveDeltaClass.CounterplayReduction =>
          containsAny(low, List("prevent", "deny", "keep", "stop", "counterplay", "break", "restrain"))
        case PlayerFacingMoveDeltaClass.ResourceRemoval =>
          containsAny(low, List("remove", "lose", "without", "trade", "exchange", "capture", "limit"))
        case PlayerFacingMoveDeltaClass.PlanAdvance =>
          containsAny(low, List("advance", "prepare", "step", "activate", "improve", "coordinate", "support", "available", "cover"))
    familyMatch &&
      delta.anchorTerms.exists(anchor => anchor.length >= 2 && (low.contains(anchor) || anchor.contains(low)))

  private def lineShowsStrategicDelta(
      text: String,
      delta: PlayerFacingMoveDeltaEvidence
  ): Boolean =
    val low = normalize(text)
    delta.deltaClass match
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        text.contains("x") || containsAny(low, List("trade", "exchange", "simplif"))
      case PlayerFacingMoveDeltaClass.CounterplayReduction | PlayerFacingMoveDeltaClass.ResourceRemoval =>
        delta.anchorTerms.exists(anchor => anchor.length >= 2 && low.contains(anchor))
      case _ =>
        false

  private def variationShowsAnchoredExchange(
      variation: VariationLine,
      squareAnchors: List[String]
  ): Boolean =
    val window = variation.parsedMoves.take(4)
    val captures = window.filter(_.isCapture)
    captures.nonEmpty && (
      squareAnchors.isEmpty ||
        captures.exists(move =>
          squareAnchors.contains(normalize(move.to)) || squareAnchors.contains(normalize(move.from))
        )
    )

  private def variationShowsSpecificResourceLoss(
      variation: VariationLine,
      plan: PreventedPlanInfo
  ): Boolean =
    val lowMoves = variation.moves.take(4).map(normalize)
    val breakHit =
      plan.breakNeutralized.exists(file =>
        lowMoves.exists(move => move.contains(normalize(file)))
      )
    val squareHit =
      plan.deniedSquares.map(normalize).exists(square =>
        variation.parsedMoves.take(4).exists(move =>
          normalize(move.to) == square || normalize(move.from) == square
        )
      )
    val threatHit =
      plan.preventedThreatType.exists(threat =>
        lowMoves.exists(move => move.contains(normalize(threat)))
      )
    breakHit || squareHit || threatHit

  private def citationShowsSpecificResourceLoss(
      text: String,
      plan: PreventedPlanInfo
  ): Boolean =
    val low = normalize(text)
    val squareHit = plan.deniedSquares.map(normalize).exists(low.contains)
    val breakHit = plan.breakNeutralized.map(normalize).exists(low.contains)
    val threatHit = plan.preventedThreatType.map(normalize).exists(low.contains)
    val lossHit = containsAny(low, List("prevent", "deny", "stop", "no longer", "can t", "cannot", "lose", "loses"))
    lossHit && (squareHit || breakHit || threatHit)

  private def whyNotMentionsPreventedResource(
      text: String,
      plan: PreventedPlanInfo
  ): Boolean =
    val low = normalize(text)
    val removalHit = containsAny(low, List("prevent", "deny", "stop", "no longer", "can t", "cannot", "lose", "loses"))
    val squareHit = plan.deniedSquares.map(normalize).exists(low.contains)
    val breakHit = plan.breakNeutralized.map(normalize).exists(low.contains)
    val planHit = normalize(plan.planId).nonEmpty && low.contains(normalize(plan.planId))
    val threatHit = plan.preventedThreatType.map(normalize).exists(low.contains)
    removalHit && (squareHit || breakHit || planHit || threatHit)

  private def textShowsAnchoredExchange(
      text: String,
      squareAnchors: List[String]
  ): Boolean =
    val low = normalize(text)
    containsAny(low, List("exchange", "trade", "trades", "simplif", "capture", "captures", "recapture")) &&
      squareAnchors.exists(low.contains)

  private def whyNotSources(ctx: NarrativeContext): List[String] =
    ctx.meta.flatMap(_.whyNot).toList ++
      ctx.candidates.flatMap(_.whyNot)

  private def decisionDeltaExchangeEvidence(ctx: NarrativeContext): List[String] =
    ctx.decision.toList.flatMap { decision =>
      decision.delta.resolvedThreats ++
      decision.delta.newOpportunities ++
      decision.delta.planAdvancements ++
      decision.delta.concessions
    }

  private def lineTouchesAnchor(
      normalizedText: String,
      squareAnchors: List[String],
      anchorTerms: List[String]
  ): Boolean =
    squareAnchors.exists(normalizedText.contains) ||
      anchorTerms.map(normalize).exists(anchor =>
        anchor.length >= 2 && normalizedText.contains(anchor)
      )

  private def extractSquareAnchors(anchors: List[String]): List[String] =
    val squarePattern = """\b([a-h][1-8])\b""".r
    anchors.flatMap { raw =>
      squarePattern.findAllMatchIn(Option(raw).getOrElse("").toLowerCase).map(_.group(1)).toList
    }.distinct

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)

  private def minimalMoveSentence(ctx: NarrativeContext): Option[String] =
    ctx.playedSan.flatMap(normalizedSan).map { san =>
      if san == "O-O" then "This castles."
      else if san == "O-O-O" then "This castles long."
      else
        val targetSquare = """([a-h][1-8])""".r.findAllMatchIn(san).map(_.group(1)).toList.lastOption
        val piece =
          san.headOption.collect {
            case 'K' => "king"
            case 'Q' => "queen"
            case 'R' => "rook"
            case 'B' => "bishop"
            case 'N' => "knight"
          }.getOrElse("pawn")
        if san.contains("x") then
          targetSquare.map(square => s"This captures on $square.")
            .getOrElse("This is a simplifying capture.")
        else if piece == "pawn" then
          targetSquare.map(square => s"This is a pawn move to $square.")
            .getOrElse("This is a quiet pawn move.")
        else
          targetSquare.map(square => s"This puts the $piece on $square.")
            .getOrElse(s"This improves the $piece.")
    }

  private def normalizedSan(raw: String): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""[!?+#]+$""", ""))

  private def pieceLabelFromSan(raw: Option[String]): Option[String] =
    raw.flatMap(normalizedSan).map { san =>
      san.headOption.collect {
        case 'K' => "king"
        case 'Q' => "queen"
        case 'R' => "rook"
        case 'B' => "bishop"
        case 'N' => "knight"
      }.getOrElse("pawn")
    }

  private def normalize(raw: String): String =
    Option(raw)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  private def isAbstractStrategicText(text: String): Boolean =
    val low = normalize(text)
    abstractShellTokens.exists(low.contains) ||
      low.startsWith("the long-term plan") ||
      low.startsWith("the strategic fight") ||
      low.startsWith("the plan is") ||
      low.startsWith("the route is") ||
      low.startsWith("the target is")
