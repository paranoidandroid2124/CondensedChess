package lila.llm.analysis

import _root_.chess.{ Pawn, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.llm.{ DirectionalTargetReadiness, GameChronicleMoment, RouteSurfaceMode, StrategicIdeaKind, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef }
import lila.llm.model.*
import lila.llm.model.strategic.{ VariationLine, VariationTag }
import scala.annotation.unused

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
    connectorPermission: Boolean = false,
    packet: PlayerFacingClaimPacket = PlayerFacingClaimPacket.empty
):
  def allowsStrongMainClaim: Boolean =
    PlayerFacingClaimCertification.allowsStrongMainClaim(packet)

  def allowsWeakMainClaim: Boolean =
    PlayerFacingClaimCertification.allowsWeakMainClaim(packet)

  def allowsLineEvidenceHook: Boolean =
    PlayerFacingClaimCertification.allowsLineEvidenceHook(packet)

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
    else if sacrificeClass == PlayerFacingSacrificeClass.StrategicSacrifice ||
        deltaEvidence.exists(_.packet.admitsStrategicTruthMode)
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
    else if sacrificeClass == PlayerFacingSacrificeClass.StrategicSacrifice ||
        deltaEvidence.exists(_.packet.admitsStrategicTruthMode)
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
          (
            strategicTextMatchesMoveLinkedAnchor(text, surface, truthContract) ||
              delta.anchorTerms.exists(anchor =>
                anchor.length >= 2 && normalize(text).contains(normalize(anchor))
              )
          ) &&
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
              (
                strategicTextMatchesMoveLinkedAnchor(text, surface, truthContract) ||
                  delta.anchorTerms.exists(anchor =>
                    anchor.length >= 2 && normalize(text).contains(normalize(anchor))
                  )
              ) &&
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
    val anchors =
      (
        moveLinkedAnchorTerms(surface, truthContract) ++
          boundedSimplificationLineAnchors(ctx, surface) ++
          exactPressureIncreaseAnchorTerms(ctx, surface)
      ).distinct
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
      else if hasMainPathExchangeForcing(ctx, surface, bestLine, squareAnchors) then
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
    val exactBoundedSimplificationWitness =
      exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val exactPressureIncreaseProof =
      exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val moveLinkedAnchor =
      hasMoveLinkedStrategicAnchor(surface) ||
        boundedSimplificationLineAnchors(ctx, surface).nonEmpty ||
        exactPressureIncreaseAnchorTerms(ctx, surface).nonEmpty
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
      (
        semanticConcrete ||
          truthBacked ||
          exactBoundedSimplificationWitness ||
          exactPressureIncreaseProof
      ) &&
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
    val anchors =
      (
        moveLinkedAnchorTerms(surface, truthContract) ++
          boundedSimplificationLineAnchors(ctx, surface) ++
          exactPressureIncreaseAnchorTerms(ctx, surface)
      ).distinct
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
      val boundedSimplificationSignal =
        boundedSimplificationLineAnchors(ctx, surface).nonEmpty
      def result(kind: PlayerFacingMoveDeltaClass): Option[PlayerFacingMoveDeltaEvidence] =
        Some(
          activeStrategicDeltaEvidence(
            ctx = ctx,
            surface = surface,
            deltaClass = kind,
            anchors = anchors
          )
        )

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
          planAdvancements.exists(text => containsAny(text, List("exchange", "trade", "simplif"))) ||
          boundedSimplificationSignal
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
          target.exists(_.strategicReasons.exists(reason => containsAny(normalize(reason), List("pressure", "target", "attack")))) ||
          carlsbadFixedTargetProbeWitness(ctx, surface).nonEmpty
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
      surface: StrategyPackSurface.Snapshot,
      bestLine: Option[VariationLine],
      squareAnchors: List[String]
  ): Boolean =
    val provingLineShowsExchange =
      bestLine.exists(line => variationShowsAnchoredExchange(line, squareAnchors))
    val moveLinkedExchangeEvidence =
      whyNotSources(ctx).exists(text => textShowsAnchoredExchange(text, squareAnchors)) ||
        decisionDeltaExchangeEvidence(ctx).exists(text => textShowsAnchoredExchange(text, squareAnchors))
    val ideaLinkedExchangeEvidence =
      surface.topMoveRef.exists(ref =>
        clean(ref.target).nonEmpty &&
          containsAny(normalize(ref.idea), List("trade", "exchange", "simplif", "capture"))
      ) ||
        surface.topDirectionalTarget.exists(target =>
          clean(target.targetSquare).nonEmpty &&
            target.strategicReasons.exists(reason =>
              containsAny(normalize(reason), List("trade", "exchange", "simplif", "capture"))
            )
        ) ||
        surface.dominantIdea.exists(ideaIsBoundedFavorableSimplification) ||
        surface.secondaryIdea.exists(ideaIsBoundedFavorableSimplification)
    squareAnchors.nonEmpty && provingLineShowsExchange && (moveLinkedExchangeEvidence || ideaLinkedExchangeEvidence)

  private def hasMainPathCounterplayReduction(
      preventedNow: List[PreventedPlanInfo]
  ): Boolean =
    preventedNow.exists(plan =>
      (
        plan.counterplayScoreDrop > 0 ||
          plan.mobilityDelta < 0
      ) && (
        plan.breakNeutralized.exists(_.trim.nonEmpty) ||
          plan.deniedSquares.nonEmpty ||
          namedPreventedResourceLabel(plan).nonEmpty
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
    val exactPressureIncreaseSignal =
      exactPressureIncreaseWitness(ctx, surface).nonEmpty
    (deltaSignals && anchoredTarget) || compensationSignal || exactPressureIncreaseSignal

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
    val provenanceClass = claimProvenance(ctx, surface, deltaClass)
    val quantifier = quantifierForDelta(ctx, surface, deltaClass, provenanceClass)
    val stabilityGrade = stabilityForDelta(ctx, surface, deltaClass, provenanceClass)
    val attributionGrade =
      attributionForDelta(
        ctx = ctx,
        anchors = anchors,
        deltaClass = deltaClass,
        ontologyFamily = ontologyFamily
      )
    val taintFlags =
      claimTaintFlags(
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
    val claimGate =
      PlanEvidenceEvaluator.ClaimCertification(
        certificateStatus = certificateStatus,
        quantifier = quantifier,
        modalityTier = modalityForDelta(deltaClass, quantifier, stabilityGrade, ontologyFamily),
        attributionGrade = attributionGrade,
        stabilityGrade = stabilityGrade,
        provenanceClass = provenanceClass,
        taintFlags = taintFlags.toList,
        ontologyFamily = ontologyFamily,
        alternativeDominance = ctx.strategicPlanExperiments.exists(_.refuteProbeCount > 0)
      )
    val packet =
      mainPathClaimPacket(
        ctx = ctx,
        surface = surface,
        deltaClass = deltaClass,
        anchors = anchors,
        preventedNow = preventedNow,
        claimGate = claimGate
      )
    PlayerFacingMoveDeltaEvidence(
      deltaClass = deltaClass,
      anchorTerms = anchors,
      quantifier = quantifier,
      modalityTier = claimGate.modalityTier,
      attributionGrade = attributionGrade,
      stabilityGrade = stabilityGrade,
      provenanceClass = provenanceClass,
      certificateStatus = certificateStatus,
      taintFlags = taintFlags,
      ontologyFamily = ontologyFamily,
      connectorPermission =
        packet.fallbackMode == PlayerFacingClaimFallbackMode.WeakMain &&
          packet.releaseRisks.isEmpty,
      packet = packet
    )

  private final case class ClaimOwnerSeed(
      ownerSource: String,
      ownerFamily: String,
      triggerKind: String,
      ownerSeedTerms: List[String] = Nil,
      structureTransitionTerms: List[String] = Nil
  ):
    def allWitnessTerms: List[String] =
      (ownerSeedTerms ++ structureTransitionTerms).distinct

  private final case class RivalAssessment(
      rivalKind: Option[String],
      rivalWitnessTerms: List[String],
      rivalStoryAlive: Boolean,
      rivalReleaseRisk: Boolean
    )

  private val BoundedFavorableSimplificationFamily =
    ThemeTaxonomy.SubplanId.SimplificationWindow.id
  private[llm] val ExactTargetFixationOwnerSource =
    "exact_target_fixation"
  private[llm] val CarlsbadFixedTargetProbeOwnerSource =
    "carlsbad_fixed_target_probe"
  private[llm] val TargetFocusedCoordinationOwnerSource =
    "target_focused_coordination_probe"
  private[llm] val TargetFocusedCoordinationOwnerFamily =
    "target_focused_coordination"

  private enum ExactSliceKind:
    case TargetFixation
    case CarlsbadFixedTargetProbe
    case TargetFocusedCoordinationProbe

  private final case class ExactSliceDescriptor(
      kind: ExactSliceKind,
      ownerSource: String,
      ownerFamily: String,
      triggerKind: String,
      releasedScope: PlayerFacingPacketScope,
      releaseOwnerFamilies: Set[String],
      exemptFromRivalChecks: Boolean = false
  ):
    def packetMatches(packet: PlayerFacingClaimPacket): Boolean =
      packet.ownerSource == ownerSource &&
        packet.ownerFamily == ownerFamily

    def ownerSeedMatches(ownerSeed: ClaimOwnerSeed): Boolean =
      ownerSeed.ownerSource == ownerSource &&
        releaseOwnerFamilies.contains(ownerSeed.ownerFamily)

  private val ExactTargetFixationDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.TargetFixation,
      ownerSource = ExactTargetFixationOwnerSource,
      ownerFamily = ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id,
      triggerKind = "target_fixation",
      releasedScope = PlayerFacingPacketScope.MoveLocal,
      releaseOwnerFamilies = Set(ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id)
    )

  private val CarlsbadFixedTargetProbeDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.CarlsbadFixedTargetProbe,
      ownerSource = CarlsbadFixedTargetProbeOwnerSource,
      ownerFamily = ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
      triggerKind = "position_probe",
      releasedScope = PlayerFacingPacketScope.PositionLocal,
      releaseOwnerFamilies =
        Set(
          ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
          ThemeTaxonomy.SubplanId.MinorityAttackFixation.id
        ),
      exemptFromRivalChecks = true
    )

  private val TargetFocusedCoordinationDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.TargetFocusedCoordinationProbe,
      ownerSource = TargetFocusedCoordinationOwnerSource,
      ownerFamily = TargetFocusedCoordinationOwnerFamily,
      triggerKind = "position_probe",
      releasedScope = PlayerFacingPacketScope.PositionLocal,
      releaseOwnerFamilies = Set(TargetFocusedCoordinationOwnerFamily)
    )

  private val ExactSliceDescriptors =
    List(
      ExactTargetFixationDescriptor,
      CarlsbadFixedTargetProbeDescriptor,
      TargetFocusedCoordinationDescriptor
    )

  private final case class ExactSliceWitness(
      descriptor: ExactSliceDescriptor,
      targetSquare: String,
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  )

  private[llm] final case class PositionProbeQuestionSeed(
      questionFocusText: String,
      why: String,
      anchors: List[String]
  )

  private def activeStrategicDeltaEvidence(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      anchors: List[String]
  ): PlayerFacingMoveDeltaEvidence =
    val ontologyFamily = ontologyFamilyForActiveDelta(deltaClass)
    val claimGate =
      PlanEvidenceEvaluator.ClaimCertification(
        certificateStatus = PlayerFacingCertificateStatus.Invalid,
        quantifier = PlayerFacingClaimQuantifier.Existential,
        modalityTier = PlayerFacingClaimModalityTier.Available,
        attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
        stabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
        provenanceClass = PlayerFacingClaimProvenanceClass.StructuralOnly,
        ontologyFamily = ontologyFamily
      )
    val packet =
      PlayerFacingClaimPacket(
        claimGate = claimGate,
        ownerSource = "active_move_delta",
        ownerFamily = genericOwnerFamily(deltaClass),
        scope = PlayerFacingPacketScope.MoveLocal,
        triggerKind = genericTriggerKind(deltaClass),
        anchorTerms = packetAnchorTerms(anchors, surface),
        bestDefenseMove = bestDefenseMoveFromContext(ctx),
        bestDefenseBranchKey = bestDefenseBranchKeyFromContext(ctx),
        sameBranchState =
          if bestDefenseBranchKeyFromContext(ctx).nonEmpty then
            PlayerFacingSameBranchState.Ambiguous
          else PlayerFacingSameBranchState.Missing,
        persistence =
          if ctx.engineEvidence.toList.flatMap(_.variations).nonEmpty then
            PlayerFacingClaimPersistence.BestDefenseOnly
          else PlayerFacingClaimPersistence.Broken,
        rivalKind = surface.secondaryIdea.flatMap(idea => clean(idea.kind)),
        ownerPathWitness =
          PlayerFacingOwnerPathWitness(
            ownerSeedTerms = packetAnchorTerms(anchors, surface),
            continuationTerms =
              bestDefenseBranchKeyFromContext(ctx).toList ++
                bestDefenseMoveFromContext(ctx).toList,
            rivalTerms = surface.secondaryIdea.toList.flatMap(idea => clean(idea.kind))
          ),
        fallbackMode =
          if anchors.nonEmpty then PlayerFacingClaimFallbackMode.ExactFactual
          else PlayerFacingClaimFallbackMode.Suppress
      )
    PlayerFacingMoveDeltaEvidence(
      deltaClass = deltaClass,
      anchorTerms = anchors,
      quantifier = claimGate.quantifier,
      modalityTier = claimGate.modalityTier,
      attributionGrade = claimGate.attributionGrade,
      stabilityGrade = claimGate.stabilityGrade,
      provenanceClass = claimGate.provenanceClass,
      certificateStatus = claimGate.certificateStatus,
      taintFlags = claimGate.taintFlags.toSet,
      ontologyFamily = ontologyFamily,
      connectorPermission = false,
      packet = packet
    )

  private def mainPathClaimPacket(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      anchors: List[String],
      preventedNow: List[PreventedPlanInfo],
      claimGate: PlanEvidenceEvaluator.ClaimCertification
  ): PlayerFacingClaimPacket =
    val ownerSeed = ownerSeedForMainPath(ctx, surface, deltaClass, preventedNow)
    val anchorTerms = packetAnchorTerms(anchors, surface)
    val bestDefenseMove = bestDefenseMoveFromContext(ctx)
    val bestDefenseBranchKey = bestDefenseBranchKeyFromContext(ctx)
    val continuationTerms =
      continuationWitnessTermsForMainPath(
        ctx = ctx,
        ownerSeed = ownerSeed,
        bestDefenseBranchKey = bestDefenseBranchKey,
        bestDefenseMove = bestDefenseMove
      )
    val sameBranchState =
      sameBranchStateForMainPath(
        ctx = ctx,
        ownerSeed = ownerSeed,
        claimGate = claimGate,
        bestDefenseBranchKey = bestDefenseBranchKey,
        continuationTerms = continuationTerms
      )
    val persistence = persistenceForMainPath(ctx, ownerSeed, claimGate)
    val rivalAssessment =
      rivalAssessmentForMainPath(
        ctx = ctx,
        surface = surface,
        ownerSeed = ownerSeed,
        deltaClass = deltaClass,
        preventedNow = preventedNow,
        sameBranchState = sameBranchState,
        persistence = persistence
      )
    val suppressionReasons =
      suppressionReasonsForMainPath(
        ctx = ctx,
        surface = surface,
        ownerSeed = ownerSeed,
        claimGate = claimGate,
        sameBranchState = sameBranchState,
        rivalAssessment = rivalAssessment
      )
    val releaseRisks =
      releaseRisksForMainPath(
        ctx = ctx,
        surface = surface,
        ownerSeed = ownerSeed,
        sameBranchState = sameBranchState,
        persistence = persistence,
        rivalAssessment = rivalAssessment
      )
    val fallbackMode =
      fallbackModeForMainPath(
        claimGate = claimGate,
        ownerSeed = ownerSeed,
        anchors = anchorTerms,
        bestDefenseBranchKey = bestDefenseBranchKey,
        sameBranchState = sameBranchState,
        persistence = persistence,
        suppressionReasons = suppressionReasons,
        releaseRisks = releaseRisks
      )
    PlayerFacingClaimPacket(
      claimGate = claimGate,
      ownerSource = ownerSeed.ownerSource,
      ownerFamily = ownerSeed.ownerFamily,
      scope = scopeForFallback(fallbackMode, ownerSeed),
      triggerKind = ownerSeed.triggerKind,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = bestDefenseBranchKey,
      sameBranchState = sameBranchState,
      persistence = persistence,
      rivalKind = rivalAssessment.rivalKind,
      ownerPathWitness =
        PlayerFacingOwnerPathWitness(
          ownerSeedTerms = ownerSeed.ownerSeedTerms ++ anchorTerms,
          continuationTerms = continuationTerms,
          rivalTerms = rivalAssessment.rivalWitnessTerms,
          structureTransitionTerms = ownerSeed.structureTransitionTerms
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
        fallbackMode = fallbackMode
    )

  private def exactPressureIncreaseOwnerSeed(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ClaimOwnerSeed] =
    exactPressureIncreaseWitness(ctx, surface).map { witness =>
      ClaimOwnerSeed(
        ownerSource = witness.descriptor.ownerSource,
        ownerFamily = witness.descriptor.ownerFamily,
        triggerKind = witness.descriptor.triggerKind,
        ownerSeedTerms = witness.ownerSeedTerms,
        structureTransitionTerms = witness.structureTransitionTerms
      )
    }

  private def ownerSeedForMainPath(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      preventedNow: List[PreventedPlanInfo]
  ): ClaimOwnerSeed =
    val planOwner = leadingOwnerFamily(ctx)
    val trigger = leadingTriggerKind(ctx)
    val localFileEntryPair = LocalFileEntryBindCertification.certifiedSurfacePair(ctx)
    val targetComplexTerms = targetComplexWitnessTerms(ctx, surface)
    val tradeOwnerTerms = tradeOwnerSeedTerms(ctx, surface)
    val tradeTransitionTerms = tradeStructureTransitionTerms(ctx, surface)
    val namedPreventedTerms = namedPreventedResourceTerms(preventedNow)
    val breakTerms = breakResourceTerms(preventedNow)
    val exactPressureIncreaseSeed = exactPressureIncreaseOwnerSeed(ctx, surface)
    val prophylacticRestraintPlan =
      trigger.contains("counterplay_restraint") || planOwner.contains("prophylaxis_restraint")
    deltaClass match
      case PlayerFacingMoveDeltaClass.PressureIncrease
          if exactPressureIncreaseSeed.nonEmpty =>
        exactPressureIncreaseSeed.get
      case PlayerFacingMoveDeltaClass.CounterplayReduction
          if localFileEntryPair.nonEmpty =>
        val pair = localFileEntryPair.get
        ClaimOwnerSeed(
          ownerSource = "local_file_entry_bind",
          ownerFamily = "half_open_file_pressure",
          triggerKind = "file_entry_denial",
          ownerSeedTerms = List(pair.file, pair.entrySquare),
          structureTransitionTerms = List(s"file-entry:${pair.file}:${pair.entrySquare}")
        )
      case PlayerFacingMoveDeltaClass.ResourceRemoval
          if localFileEntryPair.nonEmpty =>
        val pair = localFileEntryPair.get
        ClaimOwnerSeed(
          ownerSource = "local_file_entry_bind",
          ownerFamily = "half_open_file_pressure",
          triggerKind = "file_entry_denial",
          ownerSeedTerms = List(pair.file, pair.entrySquare),
          structureTransitionTerms = List(s"file-entry:${pair.file}:${pair.entrySquare}")
        )
      case PlayerFacingMoveDeltaClass.CounterplayReduction | PlayerFacingMoveDeltaClass.ResourceRemoval
          if prophylacticRestraintPlan &&
            hasNamedPreventedResource(preventedNow) =>
        ClaimOwnerSeed(
          ownerSource = "prophylactic_move",
          ownerFamily = "counterplay_restraint",
          triggerKind = "prophylactic_move",
          ownerSeedTerms = namedPreventedTerms,
          structureTransitionTerms = namedPreventedTerms
        )
      case PlayerFacingMoveDeltaClass.CounterplayReduction
          if preventedNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty)) =>
        ClaimOwnerSeed(
          ownerSource = "counterplay_axis_suppression",
          ownerFamily = "neutralize_key_break",
          triggerKind = "break_neutralization",
          ownerSeedTerms = breakTerms,
          structureTransitionTerms = breakTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if boundedFavorableSimplificationSeed(
            ctx = ctx,
            surface = surface,
            planOwner = planOwner,
            tradeOwnerTerms = tradeOwnerTerms,
            targetComplexTerms = targetComplexTerms
          ) =>
        ClaimOwnerSeed(
          ownerSource = BoundedFavorableSimplificationFamily,
          ownerFamily = BoundedFavorableSimplificationFamily,
          triggerKind = BoundedFavorableSimplificationFamily,
          ownerSeedTerms =
            (tradeOwnerTerms ++ targetComplexTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct,
          structureTransitionTerms = targetComplexTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if likelyTradeKeyDefender(ctx, surface) || tradeOwnerTerms.nonEmpty || tradeTransitionTerms.nonEmpty =>
        ClaimOwnerSeed(
          ownerSource = "exchange_forcing_delta",
          ownerFamily = "trade_key_defender",
          triggerKind = trigger.getOrElse("trade_key_defender"),
          ownerSeedTerms = tradeOwnerTerms,
          structureTransitionTerms = tradeTransitionTerms
        )
      case _ =>
        val guardedPlanOwner =
          planOwner.filterNot { owner =>
            (owner == "half_open_file_pressure" && localFileEntryPair.isEmpty) ||
            (owner == "neutralize_key_break" && !preventedNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty))) ||
            (owner == "counterplay_restraint" && !hasNamedPreventedResource(preventedNow)) ||
            (owner == "prophylaxis_restraint" && !hasNamedPreventedResource(preventedNow))
          }
        ClaimOwnerSeed(
          ownerSource = genericOwnerSource(deltaClass),
          ownerFamily = guardedPlanOwner.getOrElse(genericOwnerFamily(deltaClass)),
          triggerKind = trigger.getOrElse(genericTriggerKind(deltaClass)),
          ownerSeedTerms =
            genericOwnerSeedTerms(
              deltaClass = deltaClass,
              ctx = ctx,
              surface = surface,
              targetComplexTerms = targetComplexTerms,
              tradeOwnerTerms = tradeOwnerTerms,
              namedPreventedTerms = namedPreventedTerms,
              breakTerms = breakTerms
            ),
          structureTransitionTerms =
            genericStructureTransitionTerms(
              deltaClass = deltaClass,
              targetComplexTerms = targetComplexTerms,
              tradeTransitionTerms = tradeTransitionTerms,
              namedPreventedTerms = namedPreventedTerms,
              breakTerms = breakTerms
            )
        )

  private def genericOwnerSeedTerms(
      deltaClass: PlayerFacingMoveDeltaClass,
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      targetComplexTerms: List[String],
      tradeOwnerTerms: List[String],
      namedPreventedTerms: List[String],
      breakTerms: List[String]
  ): List[String] =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        (breakTerms ++ namedPreventedTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        (namedPreventedTerms ++ breakTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        (tradeOwnerTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        (targetComplexTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct
      case PlayerFacingMoveDeltaClass.PlanAdvance =>
        (moveLocalAnchorSeedTerms(ctx, surface) ++ targetComplexTerms).distinct
      case PlayerFacingMoveDeltaClass.NewAccess =>
        moveLocalAnchorSeedTerms(ctx, surface)

  private def genericStructureTransitionTerms(
      deltaClass: PlayerFacingMoveDeltaClass,
      targetComplexTerms: List[String],
      tradeTransitionTerms: List[String],
      namedPreventedTerms: List[String],
      breakTerms: List[String]
  ): List[String] =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        (breakTerms ++ namedPreventedTerms).distinct
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        (namedPreventedTerms ++ breakTerms).distinct
      case PlayerFacingMoveDeltaClass.ExchangeForcing =>
        tradeTransitionTerms
      case PlayerFacingMoveDeltaClass.PressureIncrease =>
        targetComplexTerms
      case PlayerFacingMoveDeltaClass.PlanAdvance =>
        targetComplexTerms ++ tradeTransitionTerms
      case PlayerFacingMoveDeltaClass.NewAccess =>
        Nil

  private def moveLocalAnchorSeedTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    (
      surface.topRoute.toList.flatMap(route =>
        route.route.lastOption.flatMap(clean).toList ++ clean(route.purpose).toList
      ) ++
        surface.topDirectionalTarget.toList.flatMap(target =>
          clean(target.targetSquare).toList ++ target.strategicReasons.flatMap(clean)
        ) ++
        surface.topMoveRef.toList.flatMap(ref =>
          List(ref.target, ref.idea).flatMap(clean)
        ) ++
        ctx.decision.toList.flatMap(decision =>
          decision.focalPoint.toList.collect { case TargetSquare(key) => key } ++
            decision.delta.newOpportunities.flatMap(clean) ++
            decision.delta.planAdvancements.flatMap(clean)
        )
    ).distinct

  private def targetComplexWitnessTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    (
      ctx.semantic.toList.flatMap(_.structuralWeaknesses).flatMap { weakness =>
        weakness.squares ++
          clean(weakness.cause).toList ++
          Option.when(weakness.isOutpost)("outpost").toList
      } ++
        surface.topDirectionalTarget.toList.flatMap(target =>
          clean(target.targetSquare).toList ++
            target.strategicReasons.flatMap(clean)
        ) ++
        ctx.decision.toList.flatMap(decision =>
          decision.focalPoint.toList.collect { case TargetSquare(key) => key }
        )
    ).distinct

  private def tradeOwnerSeedTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    (
      exactBoundedSimplificationExchangeSquare(ctx, surface).toList ++
        surface.topMoveRef.toList.flatMap(ref => List(ref.target, ref.idea).flatMap(clean)) ++
        surface.topDirectionalTarget.toList.flatMap(target =>
          clean(target.targetSquare).toList ++
            target.strategicReasons.filter(reason =>
              containsAny(normalize(reason), List("trade", "exchange", "simplif", "defender"))
            ).flatMap(clean)
        ) ++
        ctx.decision.toList.flatMap(decision =>
          decision.focalPoint.toList.collect { case TargetSquare(key) => key } ++
            decision.delta.newOpportunities.filter(text =>
              containsAny(normalize(text), List("trade", "exchange", "simplif", "defender"))
            ).flatMap(clean) ++
            decision.delta.planAdvancements.filter(text =>
              containsAny(normalize(text), List("trade", "exchange", "simplif", "defender"))
            ).flatMap(clean)
        )
    ).distinct

  private def tradeStructureTransitionTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    (
      ctx.semantic.toList.flatMap { semantic =>
        semantic.structuralWeaknesses.flatMap(weakness =>
          weakness.squares ++ clean(weakness.cause).toList
        ) ++
          semantic.endgameFeatures.toList.flatMap(feature =>
            clean(feature.theoreticalOutcomeHint.toString).toList ++
              feature.keySquaresControlled
          ) ++
          semantic.compensation.toList.flatMap(comp => clean(comp.conversionPlan).toList)
      } ++
        surface.topDirectionalTarget.toList.flatMap(target =>
          target.strategicReasons.filter(reason =>
            containsAny(normalize(reason), List("structure", "isolated", "backward", "endgame", "trade", "simplif"))
          ).flatMap(clean)
        ) ++
        ctx.decision.toList.flatMap(decision =>
          decision.delta.planAdvancements.filter(text =>
            containsAny(normalize(text), List("structure", "isolated", "backward", "endgame", "trade", "simplif"))
          ).flatMap(clean)
        )
    ).distinct

  private def boundedFavorableSimplificationSeed(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      planOwner: Option[String],
      tradeOwnerTerms: List[String],
      targetComplexTerms: List[String]
  ): Boolean =
    exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty &&
      tradeOwnerTerms.nonEmpty &&
      (
        planOwner.contains(BoundedFavorableSimplificationFamily) ||
          surface.dominantIdea.exists(ideaIsBoundedFavorableSimplification) ||
          surface.secondaryIdea.exists(ideaIsBoundedFavorableSimplification)
      ) &&
      !boundedSimplificationTradeKeyDefenderRelabel(ctx, surface) &&
      !boundedSimplificationBetterEndgameInflation(ctx, surface) &&
      (targetComplexTerms.nonEmpty || tradeOwnerTerms.exists(_.matches(".*[a-h][1-8].*")))

  private def namedPreventedResourceTerms(
      preventedNow: List[PreventedPlanInfo]
  ): List[String] =
    preventedNow.flatMap(plan =>
      namedPreventedResourceLabel(plan).toList ++
        plan.deniedSquares.flatMap(clean) ++
        plan.preventedThreatType.flatMap(clean).toList
    ).distinct

  private def breakResourceTerms(
      preventedNow: List[PreventedPlanInfo]
  ): List[String] =
    preventedNow.flatMap(plan =>
      plan.breakNeutralized.flatMap(clean).toList ++
        plan.deniedSquares.flatMap(clean)
    ).distinct

  private def leadingOwnerFamily(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport
      .evidenceBackedMainPlans(ctx)
      .headOption
      .map(plan => PlanMatcher.ownerFamily(plan.themeL1, plan.subplanId))

  private def secondaryOwnerFamily(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport
      .evidenceBackedMainPlans(ctx)
      .lift(1)
      .map(plan => PlanMatcher.ownerFamily(plan.themeL1, plan.subplanId))

  private def leadingTriggerKind(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport
      .evidenceBackedMainPlans(ctx)
      .headOption
      .map(plan => PlanMatcher.triggerKind(plan.themeL1, plan.subplanId))

  private def ownerLinkedEvidenceBackedExperiments(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): List[StrategicPlanExperiment] =
    val evidenceBacked =
      ctx.strategicPlanExperiments.filter(exp => isEvidenceBackedTier(exp.evidenceTier))
    val matched = evidenceBacked.filter(exp => ownerPathMatchesExperiment(ownerSeed, exp))
    if matched.nonEmpty then matched else evidenceBacked

  private def ownerPathMatchesExperiment(
      ownerSeed: ClaimOwnerSeed,
      experiment: StrategicPlanExperiment
  ): Boolean =
    val subplan = experiment.subplanId.map(normalize)
    val theme = normalize(experiment.themeL1)
    ownerSeed.ownerFamily match
      case "neutralize_key_break" =>
        subplan.contains(normalize(ThemeTaxonomy.SubplanId.BreakPrevention.id)) ||
          experiment.counterBreakNeutralized
      case "half_open_file_pressure" =>
        Set(
          ThemeTaxonomy.SubplanId.KeySquareDenial.id,
          ThemeTaxonomy.SubplanId.OpenFilePressure.id,
          ThemeTaxonomy.SubplanId.RookFileTransfer.id
        ).map(normalize).exists(subplan.contains)
      case "counterplay_restraint" =>
        subplan.contains(normalize(ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id)) ||
          theme == normalize(ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id)
      case "trade_key_defender" =>
        Set(
          ThemeTaxonomy.SubplanId.DefenderTrade.id,
          ThemeTaxonomy.SubplanId.SimplificationConversion.id
        ).map(normalize).exists(subplan.contains) ||
          theme == normalize(ThemeTaxonomy.ThemeL1.FavorableExchange.id) ||
          theme == normalize(ThemeTaxonomy.ThemeL1.AdvantageTransformation.id)
      case family if family == BoundedFavorableSimplificationFamily =>
        subplan.contains(normalize(ThemeTaxonomy.SubplanId.SimplificationWindow.id)) ||
          (
            theme == normalize(ThemeTaxonomy.ThemeL1.FavorableExchange.id) &&
              subplan.contains(normalize(ThemeTaxonomy.SubplanId.SimplificationWindow.id))
          )
      case family
          if Set(
            ThemeTaxonomy.ThemeL1.WeaknessFixation.id,
            ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id,
            ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
            ThemeTaxonomy.SubplanId.MinorityAttackFixation.id,
            ThemeTaxonomy.SubplanId.IQPInducement.id
          ).contains(family) =>
        Set(
          ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id,
          ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
          ThemeTaxonomy.SubplanId.MinorityAttackFixation.id,
          ThemeTaxonomy.SubplanId.IQPInducement.id
        ).map(normalize).exists(subplan.contains) ||
          theme == normalize(ThemeTaxonomy.ThemeL1.WeaknessFixation.id)
      case other =>
        subplan.contains(normalize(other)) || theme == normalize(other)

  private def continuationWitnessTermsForMainPath(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed,
      bestDefenseBranchKey: Option[String],
      bestDefenseMove: Option[String]
  ): List[String] =
    val exactTradeContinuation =
      exactBoundedSimplificationContinuationSquare(ctx, ownerSeed)
    val exactSliceContinuation =
      exactSliceContinuationTerms(ctx, ownerSeed)
    (
      bestDefenseBranchKey.toList ++
        bestDefenseMove.toList ++
        exactTradeContinuation.toList.flatMap(square =>
          List("exact_trade_continuation", s"exchange_square:$square", square)
        ) ++
        exactSliceContinuation ++
        ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed).flatMap { experiment =>
          List(
            Option.when(experiment.bestReplyStable)("best_reply_stable"),
            Option.when(experiment.futureSnapshotAligned)("future_snapshot_aligned"),
            Option.when(experiment.counterBreakNeutralized)("counter_break_neutralized"),
            experiment.subplanId.flatMap(clean).map(id => s"subplan:$id"),
            clean(experiment.themeL1).map(theme => s"theme:$theme")
          ).flatten
        }
    ).distinct

  private def rivalAssessmentForMainPath(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed,
      deltaClass: PlayerFacingMoveDeltaClass,
      preventedNow: List[PreventedPlanInfo],
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence
  ): RivalAssessment =
    val localFileEntryPair = LocalFileEntryBindCertification.certifiedSurfacePair(ctx)
    val exactSignalFamilies =
      List(
        Option.when(localFileEntryPair.nonEmpty)("half_open_file_pressure" -> "exact:file_entry_pair"),
        Option.when(preventedNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty)))("neutralize_key_break" -> "exact:break_denial"),
        Option.when(hasNamedPreventedResource(preventedNow))("counterplay_restraint" -> "exact:named_resource"),
        Option.when(targetComplexWitnessTerms(ctx, surface).nonEmpty)(ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id -> "exact:target_complex"),
        Option.when(tradeStructureTransitionTerms(ctx, surface).nonEmpty)(ownerSeed.ownerFamily -> "exact:trade_transition")
      ).flatten
    val secondaryPlan = secondaryOwnerFamily(ctx)
    val secondaryIdeaFamily =
      surface.secondaryIdea.flatMap(ownerFamilyForIdea)
    val ownerStrength =
      ownerSeed.ownerSeedTerms.size +
        ownerSeed.structureTransitionTerms.size +
        (if sameBranchState == PlayerFacingSameBranchState.Proven then 3 else if sameBranchState == PlayerFacingSameBranchState.Ambiguous then 1 else 0) +
        (if persistence == PlayerFacingClaimPersistence.Stable then 2 else if persistence != PlayerFacingClaimPersistence.Broken then 1 else 0)
    val rivalTerms =
      (
        secondaryPlan.filter(_ != ownerSeed.ownerFamily).map(family => s"secondary_plan:$family").toList ++
          secondaryIdeaFamily.filter(_ != ownerSeed.ownerFamily).map(family => s"secondary_idea:$family").toList ++
          exactSignalFamilies.collect { case (family, tag) if family != ownerSeed.ownerFamily => s"$tag:$family" }
      ).distinct
    val rivalKind =
      secondaryPlan.filter(_ != ownerSeed.ownerFamily)
        .orElse(secondaryIdeaFamily.filter(_ != ownerSeed.ownerFamily))
        .orElse(exactSignalFamilies.collectFirst { case (family, _) if family != ownerSeed.ownerFamily => family })
    val rivalStrength =
      rivalKind.map { family =>
        rivalTerms.count(_.endsWith(s":$family")) +
          exactSignalFamilies.count(_._1 == family) +
          secondaryPlan.count(_ == family) * 2 +
          secondaryIdeaFamily.count(_ == family)
      }.getOrElse(0)
    val rivalStoryAlive =
      rivalKind.exists { family =>
        family != ownerSeed.ownerFamily &&
          (
            rivalStrength > ownerStrength ||
              (rivalStrength == ownerStrength && PlayerFacingClaimCertification.exactOwnerPathFamily(ownerSeed.ownerFamily)) ||
              (ownerSeed.ownerFamily == "trade_key_defender" && rivalStrength > 0)
          )
      }
    RivalAssessment(
      rivalKind = rivalKind,
      rivalWitnessTerms = rivalTerms,
      rivalStoryAlive = rivalStoryAlive,
      rivalReleaseRisk = rivalStoryAlive && ownerSeed.ownerFamily != deltaFamilyFallback(deltaClass)
    )

  private def ownerFamilyForIdea(idea: StrategyIdeaSignal): Option[String] =
    idea.kind match
      case StrategicIdeaKind.CounterplaySuppression => Some("neutralize_key_break")
      case StrategicIdeaKind.Prophylaxis            => Some("counterplay_restraint")
      case StrategicIdeaKind.LineOccupation         => Some("half_open_file_pressure")
      case StrategicIdeaKind.TargetFixing =>
        if ideaHasSource(idea, "minority_attack_fixation") || ideaHasSource(idea, "carlsbad_fixation_profile") then
          Some(ThemeTaxonomy.SubplanId.MinorityAttackFixation.id)
        else if ideaHasSource(idea, "weak_complex_fixation") || ideaHasSource(idea, "directional_target_fixation") then
          Some(ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id)
        else Some(ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id)
      case StrategicIdeaKind.FavorableTradeOrTransformation =>
        if ideaIsBoundedFavorableSimplification(idea) then Some(BoundedFavorableSimplificationFamily)
        else Some("trade_key_defender")
      case _ => None

  private def ideaHasSource(idea: StrategyIdeaSignal, source: String): Boolean =
    idea.evidenceRefs.contains(s"source:$source")

  private def ideaHasAnySource(
      idea: StrategyIdeaSignal,
      sources: Set[String]
  ): Boolean =
    sources.exists(source => ideaHasSource(idea, source))

  private def boundedFavorableSimplificationIdeaVisible(
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    surface.dominantIdea.exists(ideaIsBoundedFavorableSimplification) ||
      surface.secondaryIdea.exists(ideaIsBoundedFavorableSimplification)

  private def ideaIsBoundedFavorableSimplification(
      idea: StrategyIdeaSignal
  ): Boolean =
    idea.kind == StrategicIdeaKind.FavorableTradeOrTransformation &&
      ideaHasAnySource(
        idea,
        Set(
          "classification_transformation_window",
          "exchange_availability_bridge",
          "capture_exchange_transformation",
          "iqp_simplification_profile",
          "plan_match_transformation"
        )
      ) &&
      !ideaHasAnySource(
        idea,
        Set(
          "removing_the_defender",
          "winning_endgame_transition"
        )
      )

  private def boundedSimplificationLineAnchors(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    exactBoundedSimplificationExchangeSquare(ctx, surface).toList

  private def deltaFamilyFallback(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "neutralize_key_break"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "counterplay_restraint"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "trade_key_defender"
      case _                                              => genericOwnerFamily(deltaClass)

  private def exactSliceDescriptor(
      ownerSeed: ClaimOwnerSeed
  ): Option[ExactSliceDescriptor] =
    ExactSliceDescriptors.find(_.ownerSeedMatches(ownerSeed))

  private def exactSliceDescriptor(
      packet: PlayerFacingClaimPacket
  ): Option[ExactSliceDescriptor] =
    ExactSliceDescriptors.find(_.packetMatches(packet))

  private def exactSliceTargetWitnessTag(
      descriptor: ExactSliceDescriptor,
      square: String
  ): String =
    descriptor.kind match
      case ExactSliceKind.TargetFocusedCoordinationProbe => s"coordinated_target:$square"
      case _                                            => s"fixed_target:$square"

  private def exactSlicePositionProbeDescriptor(
      packet: PlayerFacingClaimPacket
  ): Option[ExactSliceDescriptor] =
    exactSliceDescriptor(packet).filter(descriptor =>
      descriptor.kind == ExactSliceKind.CarlsbadFixedTargetProbe ||
        descriptor.kind == ExactSliceKind.TargetFocusedCoordinationProbe
    )

  private def exactSliceContinuationTerms(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): List[String] =
    exactSliceDescriptor(ownerSeed).toList.flatMap { descriptor =>
      val targetSquares =
        ownerSeed.ownerSeedTerms
          .map(normalize)
          .filter(_.matches("[a-h][1-8]"))
          .distinct
      ctx.engineEvidence.toList.flatMap(_.variations).headOption.toList.flatMap { line =>
        val continuationMoves =
          line.moves.drop(2).take(2).flatMap(clean).map(normalize)
        Option.when(targetSquares.nonEmpty && continuationMoves.nonEmpty) {
          targetSquares.flatMap(square =>
            List(
              descriptor.ownerSource,
              exactSliceTargetWitnessTag(descriptor, square),
              s"best_branch:${continuationMoves.mkString("|")}",
              square
            ) ++ continuationMoves
          )
        }.getOrElse(Nil)
      }
    }.distinct

  private def exactSliceReleaseAllowed(
      ownerSeed: ClaimOwnerSeed,
      bestDefenseBranchKey: Option[String],
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence
  ): Boolean =
    exactSliceDescriptor(ownerSeed).nonEmpty &&
      bestDefenseBranchKey.nonEmpty &&
      sameBranchState == PlayerFacingSameBranchState.Proven &&
      persistence == PlayerFacingClaimPersistence.Stable

  private def sameBranchStateForMainPath(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed,
      claimGate: PlanEvidenceEvaluator.ClaimCertification,
      bestDefenseBranchKey: Option[String],
      continuationTerms: List[String]
  ): PlayerFacingSameBranchState =
    val branchVisible = bestDefenseBranchKey.nonEmpty
    if claimGate.provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingSameBranchState.Missing
    else if !branchVisible then
      PlayerFacingSameBranchState.Missing
    else
      val evidenceBacked = ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed)
      val exactTradeContinuation =
        exactBoundedSimplificationContinuationSquare(ctx, ownerSeed)
      val exactSliceContinuation =
        exactSliceContinuationTerms(ctx, ownerSeed)
      val stableBestDefense = evidenceBacked.exists(_.bestReplyStable)
      val futureAligned = evidenceBacked.exists(_.futureSnapshotAligned)
      val moveOrderClean = !evidenceBacked.exists(_.moveOrderSensitive)
      val concreteOwnerSeed = ownerSeed.ownerSeedTerms.nonEmpty
      val concreteTransition = ownerSeed.structureTransitionTerms.nonEmpty
      if exactSliceDescriptor(ownerSeed).nonEmpty &&
          concreteOwnerSeed &&
          exactSliceContinuation.nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.ownerFamily == BoundedFavorableSimplificationFamily &&
          concreteOwnerSeed &&
          exactTradeContinuation.nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if concreteOwnerSeed && stableBestDefense && futureAligned && moveOrderClean then
        PlayerFacingSameBranchState.Proven
      else if concreteOwnerSeed && stableBestDefense && concreteTransition && continuationTerms.nonEmpty && moveOrderClean then
        PlayerFacingSameBranchState.Proven
      else if continuationTerms.nonEmpty || evidenceBacked.nonEmpty then
        PlayerFacingSameBranchState.Ambiguous
      else PlayerFacingSameBranchState.Missing

  private def persistenceForMainPath(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed,
      claimGate: PlanEvidenceEvaluator.ClaimCertification
  ): PlayerFacingClaimPersistence =
    if claimGate.provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimPersistence.Broken
    else
      val evidenceBacked = ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed)
      if exactSliceContinuationTerms(ctx, ownerSeed).nonEmpty &&
          exactSliceDescriptor(ownerSeed).nonEmpty &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if exactBoundedSimplificationContinuationSquare(ctx, ownerSeed).nonEmpty &&
          ownerSeed.ownerFamily == BoundedFavorableSimplificationFamily &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if evidenceBacked.exists(exp => exp.bestReplyStable && exp.futureSnapshotAligned && !exp.moveOrderSensitive) then
        PlayerFacingClaimPersistence.Stable
      else if evidenceBacked.exists(_.bestReplyStable) then
        PlayerFacingClaimPersistence.BestDefenseOnly
      else if evidenceBacked.exists(_.futureSnapshotAligned) then
        PlayerFacingClaimPersistence.FutureOnly
      else PlayerFacingClaimPersistence.Broken

  private def suppressionReasonsForMainPath(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed,
      claimGate: PlanEvidenceEvaluator.ClaimCertification,
      sameBranchState: PlayerFacingSameBranchState,
      rivalAssessment: RivalAssessment
  ): List[String] =
    val reasons = scala.collection.mutable.ListBuffer.empty[String]
    if claimGate.alternativeDominance then
      reasons += PlayerFacingClaimSuppressionReason.AlternativeDominance
    if PlayerFacingClaimCertification.exactOwnerPathFamily(ownerSeed.ownerFamily)
    then
      sameBranchState match
        case PlayerFacingSameBranchState.Missing =>
          reasons += PlayerFacingClaimSuppressionReason.SameBranchMissing
        case PlayerFacingSameBranchState.Ambiguous =>
          reasons += PlayerFacingClaimSuppressionReason.SameBranchAmbiguous
        case PlayerFacingSameBranchState.Proven => ()
    if ownerSeed.ownerFamily == "trade_key_defender" then
      reasons += PlayerFacingClaimSuppressionReason.SupportOnlyReinflation
    if standaloneEntrySquareDenialReinflates(ownerSeed) then
      reasons += PlayerFacingClaimSuppressionReason.ScopeInflation
    if ownerSeed.ownerFamily == BoundedFavorableSimplificationFamily then
      if boundedSimplificationSameJobConversion(ctx) then
        reasons += PlayerFacingClaimSuppressionReason.SameJobConversion
      if boundedSimplificationTradeKeyDefenderRelabel(ctx, surface) then
        reasons += PlayerFacingClaimSuppressionReason.TradeKeyDefenderRelabel
      if boundedSimplificationRouteBindRelabel(rivalAssessment) then
        reasons += PlayerFacingClaimSuppressionReason.RouteBindRelabel
      if boundedSimplificationBetterEndgameInflation(ctx, surface) then
        reasons += PlayerFacingClaimSuppressionReason.BetterEndgameInflation
      if boundedSimplificationB7Drift(ctx, surface) then
        reasons += PlayerFacingClaimSuppressionReason.B7Drift
    if rivalAssessment.rivalStoryAlive &&
        !exactSliceDescriptor(ownerSeed).exists(_.exemptFromRivalChecks)
    then
      reasons += PlayerFacingClaimSuppressionReason.RivalStoryAlive
    reasons.toList.distinct

  private def releaseRisksForMainPath(
      ctx: NarrativeContext,
      @unused surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed,
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence,
      rivalAssessment: RivalAssessment
  ): List[String] =
    val risks = scala.collection.mutable.ListBuffer.empty[String]
    if ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed).exists(_.moveOrderSensitive) then
      risks += PlayerFacingClaimReleaseRisk.MoveOrderFragility
    if HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx) then
      risks += PlayerFacingClaimReleaseRisk.HeavyPieceLeakage
    if ownerSeed.ownerFamily == "neutralize_key_break" &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence != PlayerFacingClaimPersistence.Stable
    then risks += PlayerFacingClaimReleaseRisk.RouteMirage
    if ownerSeed.ownerFamily == "counterplay_restraint" &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence != PlayerFacingClaimPersistence.Stable
    then risks += PlayerFacingClaimReleaseRisk.RouteMirage
    if (ownerSeed.ownerSource == "local_file_entry_bind" || ownerSeed.ownerFamily == "half_open_file_pressure") &&
        sameBranchState != PlayerFacingSameBranchState.Proven
    then
      risks += PlayerFacingClaimReleaseRisk.SurfaceReinflation
    if (ownerSeed.ownerSource == "prophylactic_move" || ownerSeed.ownerFamily == "counterplay_restraint") &&
        sameBranchState != PlayerFacingSameBranchState.Proven
    then
      risks += PlayerFacingClaimReleaseRisk.SurfaceReinflation
    if ownerSeed.ownerFamily == "trade_key_defender" then
      risks += PlayerFacingClaimReleaseRisk.RivalRelease
    if rivalAssessment.rivalReleaseRisk &&
        !exactSliceDescriptor(ownerSeed).exists(_.exemptFromRivalChecks)
    then
      risks += PlayerFacingClaimReleaseRisk.RivalRelease
    if ownerSeed.ownerFamily == BoundedFavorableSimplificationFamily &&
        boundedSimplificationRouteBindRelabel(rivalAssessment)
    then
      risks += PlayerFacingClaimReleaseRisk.RivalRelease
    risks.toList.distinct

  private def fallbackModeForMainPath(
      claimGate: PlanEvidenceEvaluator.ClaimCertification,
      ownerSeed: ClaimOwnerSeed,
      anchors: List[String],
      bestDefenseBranchKey: Option[String],
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence,
      suppressionReasons: List[String],
      releaseRisks: List[String]
  ): PlayerFacingClaimFallbackMode =
    val allowsWeak =
      PlayerFacingClaimCertification.allowsWeakMainClaim(
        certificateStatus = claimGate.certificateStatus,
        quantifier = claimGate.quantifier,
        attribution = claimGate.attributionGrade,
        stability = claimGate.stabilityGrade,
        provenance = claimGate.provenanceClass,
        taintFlags = claimGate.taintFlags.toSet
      )
    val allowsLine =
      PlayerFacingClaimCertification.allowsLineEvidenceHook(
        certificateStatus = claimGate.certificateStatus,
        provenance = claimGate.provenanceClass,
        taintFlags = claimGate.taintFlags.toSet
      )
    val lineOnlyPilot =
      PlayerFacingClaimPacket.isLineOnlyPilot(ownerSeed.ownerSource, ownerSeed.ownerFamily)
    val promotedReleaseAllowed =
      allowsPromotedMoveLocalRelease(
        ownerSeed = ownerSeed,
        bestDefenseBranchKey = bestDefenseBranchKey,
        sameBranchState = sameBranchState,
        persistence = persistence
      )
    if lineOnlyPilot then
      PlayerFacingClaimFallbackMode.LineOnly
    else if standaloneEntrySquareDenialReinflates(ownerSeed) then
      PlayerFacingClaimFallbackMode.Suppress
    else if allowsWeak && promotedReleaseAllowed && suppressionReasons.isEmpty && releaseRisks.isEmpty then
      PlayerFacingClaimFallbackMode.WeakMain
    else if allowsLine &&
        ownerSeed.ownerFamily != "trade_key_defender" &&
        !suppressionReasons.contains(PlayerFacingClaimSuppressionReason.AlternativeDominance)
    then PlayerFacingClaimFallbackMode.LineOnly
    else if anchors.nonEmpty then PlayerFacingClaimFallbackMode.ExactFactual
    else PlayerFacingClaimFallbackMode.Suppress

  private def scopeForFallback(
      fallbackMode: PlayerFacingClaimFallbackMode,
      ownerSeed: ClaimOwnerSeed
  ): PlayerFacingPacketScope =
    fallbackMode match
      case PlayerFacingClaimFallbackMode.WeakMain
          if exactSliceDescriptor(ownerSeed).exists(_.releasedScope == PlayerFacingPacketScope.PositionLocal) =>
        PlayerFacingPacketScope.PositionLocal
      case PlayerFacingClaimFallbackMode.WeakMain   => PlayerFacingPacketScope.MoveLocal
      case PlayerFacingClaimFallbackMode.LineOnly   => PlayerFacingPacketScope.LineScoped
      case PlayerFacingClaimFallbackMode.ExactFactual
          if exactSliceDescriptor(ownerSeed).exists(_.releasedScope == PlayerFacingPacketScope.PositionLocal) =>
        PlayerFacingPacketScope.PositionLocal
      case PlayerFacingClaimFallbackMode.ExactFactual => PlayerFacingPacketScope.MoveLocal
      case PlayerFacingClaimFallbackMode.Suppress   => PlayerFacingPacketScope.BackendOnly

  private def bestDefenseMoveFromContext(ctx: NarrativeContext): Option[String] =
    bestDefenseMoveFromVariations(ctx.engineEvidence.toList.flatMap(_.variations))

  private def bestDefenseBranchKeyFromContext(ctx: NarrativeContext): Option[String] =
    bestDefenseBranchKeyFromVariations(ctx.engineEvidence.toList.flatMap(_.variations))

  private def bestDefenseMoveFromVariations(
      variations: List[VariationLine]
  ): Option[String] =
    variations.headOption.flatMap { line =>
      line.parsedMoves.lift(1).flatMap(move => clean(move.san))
        .orElse(line.moves.lift(1).flatMap(clean))
    }

  private def bestDefenseBranchKeyFromVariations(
      variations: List[VariationLine]
  ): Option[String] =
    variations.headOption.flatMap(line =>
      branchKeyFromMoves(line.moves)
        .orElse(branchKeyFromMoves(line.parsedMoves.flatMap(move => clean(move.uci))))
    )

  private def branchKeyFromMoves(
      moves: List[String]
  ): Option[String] =
    moves.take(2).flatMap(clean) match
      case first :: second :: Nil => Some(s"${normalize(first)}|${normalize(second)}")
      case _                      => None

  private def packetAnchorTerms(
      anchors: List[String],
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    (
      anchors ++
        surface.dominantIdea.toList.flatMap(StrategicIdeaSelector.packetAnchorTerms) ++
        surface.topDirectionalTarget.toList.flatMap(target => List(target.targetSquare)) ++
        surface.topMoveRef.toList.flatMap(ref => List(ref.target))
    ).flatMap(clean).distinct

  private def likelyTradeKeyDefender(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val evidenceText =
      ctx.decision.toList.flatMap(decision =>
        decision.delta.newOpportunities ++ decision.delta.planAdvancements ++ decision.delta.resolvedThreats
      ).map(normalize)
    StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).exists(plan =>
      plan.subplanId.contains(ThemeTaxonomy.SubplanId.DefenderTrade.id)
    ) ||
      evidenceText.exists(text => containsAny(text, List("defender", "guard", "cover", "protector"))) ||
      surface.topMoveRef.exists(ref =>
        containsAny(normalize(ref.idea), List("defender", "guard", "cover", "protector"))
      )

  private def genericOwnerSource(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => "new_access_delta"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "pressure_increase_delta"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "exchange_forcing_delta"
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "counterplay_reduction_delta"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "resource_removal_delta"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "plan_advance_delta"

  private def genericOwnerFamily(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => "new_access"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "pressure_increase"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "exchange_forcing"
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "counterplay_reduction"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "resource_removal"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "plan_advance"

  private def genericTriggerKind(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => "new_access"
      case PlayerFacingMoveDeltaClass.PressureIncrease     => "pressure_increase"
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => "exchange_forcing"
      case PlayerFacingMoveDeltaClass.CounterplayReduction => "counterplay_reduction"
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => "resource_removal"
      case PlayerFacingMoveDeltaClass.PlanAdvance          => "plan_advance"

  private def ontologyFamilyForActiveDelta(
      deltaClass: PlayerFacingMoveDeltaClass
  ): PlayerFacingClaimOntologyFamily =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => PlayerFacingClaimOntologyFamily.Access
      case PlayerFacingMoveDeltaClass.PressureIncrease     => PlayerFacingClaimOntologyFamily.Pressure
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => PlayerFacingClaimOntologyFamily.Exchange
      case PlayerFacingMoveDeltaClass.CounterplayReduction => PlayerFacingClaimOntologyFamily.CounterplayRestraint
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => PlayerFacingClaimOntologyFamily.ResourceRemoval
      case PlayerFacingMoveDeltaClass.PlanAdvance          => PlayerFacingClaimOntologyFamily.PlanAdvance

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def allowsPromotedMoveLocalRelease(
      ownerSeed: ClaimOwnerSeed,
      bestDefenseBranchKey: Option[String],
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence
  ): Boolean =
    if exactSliceDescriptor(ownerSeed).nonEmpty
    then
      exactSliceReleaseAllowed(ownerSeed, bestDefenseBranchKey, sameBranchState, persistence)
    else if isReviewedAbsorbedWeaknessOwnerFamily(ownerSeed.ownerFamily) then false
    else
      ownerSeed.ownerFamily match
        case "half_open_file_pressure" =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case "neutralize_key_break" =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case "counterplay_restraint" =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case family if family == BoundedFavorableSimplificationFamily =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case "trade_key_defender" => false
        case _                    => true

  private def standaloneEntrySquareDenialReinflates(
      ownerSeed: ClaimOwnerSeed
  ): Boolean =
    ownerSeed.triggerKind == "entry_square_denial" &&
      ownerSeed.ownerSource != "local_file_entry_bind" &&
      ownerSeed.ownerFamily != "half_open_file_pressure" &&
      ownerSeed.ownerFamily != "counterplay_restraint"

  private def boundedSimplificationSameJobConversion(
      ctx: NarrativeContext
  ): Boolean =
    leadingOwnerFamily(ctx).contains(ThemeTaxonomy.SubplanId.SimplificationConversion.id) ||
      secondaryOwnerFamily(ctx).contains(ThemeTaxonomy.SubplanId.SimplificationConversion.id) ||
      ctx.semantic.exists(_.compensation.exists(comp =>
        clean(comp.conversionPlan).exists(text =>
          containsAny(normalize(text), List("convert", "cash", "winning", "technical"))
        )
      )) ||
      ctx.decision.exists(decision =>
        decision.delta.planAdvancements.exists(text =>
          containsAny(normalize(text), List("convert", "cash", "winning", "technical"))
        )
      )

  private def boundedSimplificationTradeKeyDefenderRelabel(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    leadingOwnerFamily(ctx).contains(ThemeTaxonomy.SubplanId.DefenderTrade.id) ||
      secondaryOwnerFamily(ctx).contains(ThemeTaxonomy.SubplanId.DefenderTrade.id) ||
      surface.dominantIdea.exists(ideaHasSource(_, "removing_the_defender")) ||
      surface.secondaryIdea.exists(ideaHasSource(_, "removing_the_defender")) ||
      likelyTradeKeyDefender(ctx, surface)

  private def boundedSimplificationRouteBindRelabel(
      rivalAssessment: RivalAssessment
  ): Boolean =
    rivalAssessment.rivalStoryAlive &&
      rivalAssessment.rivalKind.contains("half_open_file_pressure")

  private def boundedSimplificationBetterEndgameInflation(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    ctx.semantic.exists(_.endgameFeatures.exists(feature =>
      normalize(feature.theoreticalOutcomeHint) == "win"
    )) ||
      surface.dominantIdea.exists(ideaHasSource(_, "winning_endgame_transition")) ||
      surface.secondaryIdea.exists(ideaHasSource(_, "winning_endgame_transition"))

  private def boundedSimplificationB7Drift(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val boundedIdeaVisible =
      surface.dominantIdea.exists(ideaIsBoundedFavorableSimplification) ||
        surface.secondaryIdea.exists(ideaIsBoundedFavorableSimplification)
    !boundedIdeaVisible &&
      leadingOwnerFamily(ctx).exists(family =>
        family == ThemeTaxonomy.SubplanId.SimplificationConversion.id ||
          family == ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id ||
          family == ThemeTaxonomy.SubplanId.IQPInducement.id
      )

  private def isReviewedAbsorbedWeaknessOwnerFamily(ownerFamily: String): Boolean =
    Set(
      ThemeTaxonomy.ThemeL1.WeaknessFixation.id,
      ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id,
      ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id,
      ThemeTaxonomy.SubplanId.MinorityAttackFixation.id,
      ThemeTaxonomy.SubplanId.IQPInducement.id
    ).contains(ownerFamily)

  private def exactBoundedSimplificationContinuationSquare(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): Option[String] =
    Option.when(ownerSeed.ownerFamily == BoundedFavorableSimplificationFamily) {
      exactBoundedSimplificationExchangeSquare(ctx).filter(square =>
        ownerSeed.allWitnessTerms.exists(term => normalize(term).contains(square))
      )
    }.flatten

  private def exactBoundedSimplificationExchangeSquare(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    Option.when(boundedFavorableSimplificationIdeaVisible(surface)) {
      exactBoundedSimplificationExchangeSquare(ctx)
    }.flatten

  private def exactBoundedSimplificationExchangeSquare(
      ctx: NarrativeContext
  ): Option[String] =
    ctx.engineEvidence.toList.flatMap(_.variations).headOption.flatMap(immediateExchangeContinuationSquare)

  private def exactPressureIncreaseWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    carlsbadFixedTargetProbeWitness(ctx, surface)
      .orElse(targetFocusedCoordinationWitness(ctx, surface))
      .orElse(exactTargetFixationWitness(ctx, surface))

  private def exactPressureIncreaseAnchorTerms(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    exactPressureIncreaseWitness(ctx, surface).toList.flatMap { witness =>
      witness.targetSquare :: witness.ownerSeedTerms
    }.distinct

  private def carlsbadFixedTargetProbeWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    val exactSignalVisible =
      List(surface.dominantIdea, surface.secondaryIdea).flatten.exists(idea =>
        idea.kind == StrategicIdeaKind.TargetFixing &&
          (
            ideaHasSource(idea, "carlsbad_fixation_profile") ||
              ideaHasSource(idea, "minority_attack_fixation")
          )
      ) ||
        carlsbadFixedTargetSupportVisible(ctx)
    Option.when(
      exactTargetFixationWitness(ctx, surface).isEmpty &&
        exactTargetFixationPosition(ctx).exists { case (board, sideToMove) =>
          carlsbadFixedTargetBoardTarget(board, sideToMove).contains("c6") &&
            exactSignalVisible &&
            bestDefenseBranchKeyFromContext(ctx).nonEmpty
        }
    ) {
      ExactSliceWitness(
        descriptor = CarlsbadFixedTargetProbeDescriptor,
        targetSquare = "c6",
        ownerSeedTerms =
          List(
            "c6",
            "fixed_target:c6",
            "queenside",
            ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id
          ).distinct,
        structureTransitionTerms =
          List(
            "queenside_fixed_chain",
            "c6_target",
            "d5_chain"
          ).distinct
      )
    }

  private def targetFocusedCoordinationWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    val targetSquare =
      exactTargetFixationPosition(ctx).flatMap { case (board, sideToMove) =>
        targetFocusedCoordinationBoardTarget(board, sideToMove)
      }
    val moveRefs =
      targetSquare.toList.flatMap(square => targetFocusedCoordinationMoveRefs(surface, square))
    val exactSignalVisible =
      targetSquare.nonEmpty &&
        moveRefs.map(_.from).distinct.size >= 2 &&
        moveRefs.exists(_.evidence.contains("target_knight")) &&
        surface.allRoutes.exists(route =>
          containsAny(normalize(route.purpose), List("coordination", "plan activation"))
        )
    Option.when(
      exactTargetFixationWitness(ctx, surface).isEmpty &&
        carlsbadFixedTargetProbeWitness(ctx, surface).isEmpty &&
        exactSignalVisible &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    ) {
      ExactSliceWitness(
        descriptor = TargetFocusedCoordinationDescriptor,
        targetSquare = targetSquare.get,
        ownerSeedTerms =
          List(
            targetSquare.get,
            s"coordinated_target:${targetSquare.get}",
            TargetFocusedCoordinationOwnerFamily,
            "rook_on_c1"
          ).distinct,
        structureTransitionTerms =
          moveRefs
            .flatMap(ref =>
              List(
                s"support_from:${normalize(ref.from)}",
                s"target_piece:${normalize(ref.target)}",
                "coordinated_piece_pressure"
              )
            )
            .distinct
      )
    }

  private def carlsbadFixedTargetSupportVisible(
      ctx: NarrativeContext
  ): Boolean =
    val weaknessPlanVisible =
      StrategicNarrativePlanSupport
        .evidenceBackedMainPlans(ctx)
        .exists(plan =>
          isReviewedAbsorbedWeaknessOwnerFamily(PlanMatcher.ownerFamily(plan.themeL1, plan.subplanId))
        )
    val weaknessSupportVisible =
      ctx.plans.top5.exists(plan =>
        plan.supports.exists(support =>
          containsAny(
            normalize(support),
            List(
              "weakness fixation",
              normalize(ThemeTaxonomy.SubplanId.StaticWeaknessFixation.id),
              normalize(ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id),
              normalize(ThemeTaxonomy.SubplanId.MinorityAttackFixation.id)
            )
          )
        )
      )
    val semanticWeaknessVisible =
      ctx.semantic.exists(value =>
        value.structuralWeaknesses.exists(weakness =>
          weakness.squares.exists(_.equalsIgnoreCase("c6")) ||
            clean(weakness.cause).exists(text =>
              containsAny(normalize(text), List("backward", "queenside", "fixed", "minority"))
            )
        )
      )
    weaknessPlanVisible || weaknessSupportVisible || semanticWeaknessVisible

  private def targetFocusedCoordinationMoveRefs(
      surface: StrategyPackSurface.Snapshot,
      targetSquare: String
  ): List[StrategyPieceMoveRef] =
    surface.allMoveRefs.filter(ref =>
      normalize(ref.target) == targetSquare &&
        normalize(ref.idea).contains("contest")
    )

  private def exactTargetFixationWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    val targetFixIdea =
      List(surface.dominantIdea, surface.secondaryIdea).flatten.find { idea =>
        idea.kind == StrategicIdeaKind.TargetFixing &&
          ideaHasSource(idea, "plan_match_target_fixing") &&
          ideaHasSource(idea, "weak_complex_fixation") &&
          ideaHasSource(idea, "minority_attack_fixation") &&
          !ideaHasSource(idea, "carlsbad_fixation_profile")
      }
    val backwardPawnTarget =
      targetFixIdea.flatMap { idea =>
        exactTargetFixationPosition(ctx).flatMap { case (board, sideToMove) =>
          val focusSquares =
            idea.focusSquares.map(normalize).filter(_.matches("[a-h][1-8]")).distinct
          val pawnTargets =
            focusSquares.filter { squareKey =>
              Square.all
                .find(_.key == squareKey)
                .flatMap(board.pieceAt)
                .exists(piece => piece.color != sideToMove && piece.role == Pawn)
            }
          Option.when(
            pawnTargets.size == 1 &&
              bestDefenseBranchKeyFromContext(ctx).nonEmpty
          )(pawnTargets.head)
        }
      }
    for
      idea <- targetFixIdea
      targetSquare <- backwardPawnTarget
      if idea.focusSquares.isEmpty || idea.focusSquares.map(normalize).contains(targetSquare)
    yield ExactSliceWitness(
      descriptor = ExactTargetFixationDescriptor,
      targetSquare = targetSquare,
      ownerSeedTerms =
        List(
          targetSquare,
          s"fixed_target:$targetSquare",
          "backward_pawn_target"
        ).distinct,
      structureTransitionTerms =
        List(
          s"weak_complex:$targetSquare",
          "backward_pawn_target"
        ).distinct
    )

  private def exactTargetFixationPosition(
      ctx: NarrativeContext
  ) =
    Fen.read(Standard, Fen.Full(ctx.fen)).map(position => (position.board, position.color))

  private def carlsbadFixedTargetBoardTarget(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color
  ): Option[String] =
    Option.when(
      sideToMove.white &&
        boardHasEnemyPawn(board, sideToMove, "c6") &&
        boardHasEnemyPawn(board, sideToMove, "d5") &&
        boardHasFriendlyPawn(board, sideToMove, "b2") &&
        boardHasFriendlyPawn(board, sideToMove, "d4")
    )("c6")

  private def targetFocusedCoordinationBoardTarget(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color
  ): Option[String] =
    Option.when(
      sideToMove.white &&
        carlsbadFixedTargetBoardTarget(board, sideToMove).isEmpty &&
        boardHasFriendlyRole(board, sideToMove, "c1", _root_.chess.Rook) &&
        boardHasEnemyRole(board, sideToMove, "c6", _root_.chess.Knight)
    )("c6")

  private def boardHasEnemyPawn(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color,
      squareKey: String
  ): Boolean =
    Square.all
      .find(_.key == squareKey)
      .flatMap(board.pieceAt)
      .exists(piece => piece.color != sideToMove && piece.role == Pawn)

  private def boardHasEnemyRole(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color,
      squareKey: String,
      role: _root_.chess.Role
  ): Boolean =
    Square.all
      .find(_.key == squareKey)
      .flatMap(board.pieceAt)
      .exists(piece => piece.color != sideToMove && piece.role == role)

  private def boardHasFriendlyPawn(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color,
      squareKey: String
  ): Boolean =
    boardHasFriendlyRole(board, sideToMove, squareKey, Pawn)

  private def boardHasFriendlyRole(
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color,
      squareKey: String,
      role: _root_.chess.Role
  ): Boolean =
    Square.all
      .find(_.key == squareKey)
      .flatMap(board.pieceAt)
      .exists(piece => piece.color == sideToMove && piece.role == role)

  private[llm] def exactSliceTargetSquare(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    exactSliceDescriptor(packet).flatMap { _ =>
      (packet.ownerPathWitness.ownerSeedTerms ++ packet.anchorTerms)
        .flatMap(clean)
        .map(_.toLowerCase)
        .find(_.matches("[a-h][1-8]"))
    }

  private def certifiedExactSlicePacket(
      packet: PlayerFacingClaimPacket,
      accepts: ExactSliceDescriptor => Boolean
  ): Boolean =
    exactSliceDescriptor(packet).exists(descriptor =>
      accepts(descriptor) &&
        packet.scope == descriptor.releasedScope &&
        packet.bestDefenseBranchKey.nonEmpty &&
        packet.sameBranchState == PlayerFacingSameBranchState.Proven &&
        packet.persistence == PlayerFacingClaimPersistence.Stable &&
        packet.releaseRisks.isEmpty &&
        PlayerFacingClaimCertification.allowsWeakMainClaim(packet)
    )

  private[llm] def certifiedExactTargetFixationPacket(
      packet: PlayerFacingClaimPacket
  ): Boolean =
    certifiedExactSlicePacket(packet, _ == ExactTargetFixationDescriptor)

  private[llm] def certifiedPositionProbePacket(
      packet: PlayerFacingClaimPacket
  ): Boolean =
    certifiedExactSlicePacket(packet, descriptor =>
      exactSlicePositionProbeDescriptor(packet).contains(descriptor)
    )

  private[llm] def pressureIncreaseMainClaim(
      packet: PlayerFacingClaimPacket,
      modalityTier: PlayerFacingClaimModalityTier,
      fallbackAnchor: Option[String]
  ): Option[String] =
    exactSliceTargetSquare(packet).orElse(fallbackAnchor).map { square =>
      if packet.ownerSource == CarlsbadFixedTargetProbeOwnerSource then
        s"The key strategic fact here is that $square is the fixed target."
      else if packet.ownerSource == TargetFocusedCoordinationOwnerSource then
        s"The key strategic fact here is that the pressure is coordinated on $square."
      else if packet.ownerSource == ExactTargetFixationOwnerSource then
        modalityTier match
          case PlayerFacingClaimModalityTier.Supports => s"This keeps $square fixed as the target."
          case _                                      => s"This keeps the pressure fixed on $square."
      else
        modalityTier match
          case PlayerFacingClaimModalityTier.Supports => s"This increases pressure on $square."
          case _                                      => s"This continues to allow pressure on $square."
    }

  private[llm] def exactTargetFixationWhatChangedClaim(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"This changes the position by fixing $square as the target."
      )
    }.flatten

  private[llm] def exactTargetFixationWhatChangedContrast(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"Before the move, $square was not yet fixed as the target on that defended branch."
      )
    }.flatten

  private[llm] def exactTargetFixationWhatChangedConsequence(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"That same defended branch keeps the pressure fixed on $square."
      )
    }.flatten

  private[llm] def positionProbeTaskConsequence(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedPositionProbePacket(packet)) {
      packet.ownerSource match
        case CarlsbadFixedTargetProbeOwnerSource =>
          exactSliceTargetSquare(packet)
            .map(square =>
              s"So the task is to keep the queenside pressure trained on $square instead of rushing a conversion."
            )
            .orElse(Some("So the task is to keep the pressure on the fixed target instead of rushing a conversion."))
        case TargetFocusedCoordinationOwnerSource =>
          exactSliceTargetSquare(packet)
            .map(square =>
              s"So the task is to keep the pressure coordinated on $square until the target has to give way."
            )
            .orElse(Some("So the task is to keep the pieces coordinated on the target until it has to give way."))
        case _ => None
    }.flatten

  private[llm] def positionProbeQuestionSeed(
      ctx: IntegratedContext,
      posOpt: Option[_root_.chess.Position]
  ): Option[PositionProbeQuestionSeed] =
    carlsbadFixedTargetQuestionSeed(ctx, posOpt)
      .orElse(targetFocusedCoordinationQuestionSeed(ctx, posOpt))

  private def carlsbadFixedTargetQuestionSeed(
      ctx: IntegratedContext,
      posOpt: Option[_root_.chess.Position]
  ): Option[PositionProbeQuestionSeed] =
    Option.when(
      ctx.isWhiteToMove &&
        ctx.maxThreatLossToUs < 120 &&
        !ctx.attackingOpportunityAtRisk &&
        posOpt.exists(pos => carlsbadFixedTargetBoardTarget(pos.board, pos.color).contains("c6"))
    ) {
      PositionProbeQuestionSeed(
        questionFocusText = "the fixed target on c6",
        why =
          "The position is defined less by an immediate race than by the fixed queenside target that has to stay under pressure.",
        anchors = List("c6", "fixed target", "queenside")
      )
    }

  private def targetFocusedCoordinationQuestionSeed(
      ctx: IntegratedContext,
      posOpt: Option[_root_.chess.Position]
  ): Option[PositionProbeQuestionSeed] =
    Option.when(
      ctx.isWhiteToMove &&
        ctx.maxThreatLossToUs < 120 &&
        !ctx.attackingOpportunityAtRisk &&
        posOpt.exists(pos => targetFocusedCoordinationBoardTarget(pos.board, pos.color).contains("c6"))
    ) {
      PositionProbeQuestionSeed(
        questionFocusText = "the coordinated pressure on c6",
        why =
          "The position turns on how the pieces keep c6 under coordinated pressure, not on a generic trade or file story.",
        anchors = List("c6", "coordinated pressure", "target coordination")
      )
    }

  private def immediateExchangeContinuationSquare(
      variation: VariationLine
  ): Option[String] =
    variation.parsedMoves.take(2) match
      case first :: second :: Nil
          if first.isCapture &&
            second.isCapture &&
            normalize(first.to) == normalize(second.to) =>
        Some(normalize(first.to))
      case _ =>
        variation.moves.take(2).flatMap(clean) match
          case first :: second :: Nil
              if first.length >= 4 &&
                second.length >= 4 &&
                normalize(first.slice(2, 4)) == normalize(second.slice(2, 4)) =>
            Some(normalize(first.slice(2, 4)))
          case _ => None

  private val GenericNamedResourceLabels =
    Set(
      "counterplay",
      "deny counterplay",
      "deny_counterplay",
      "counterplay resource",
      "resource",
      "threat",
      "plan",
      "their plan",
      "the plan"
    )

  private def hasNamedPreventedResource(
      preventedNow: List[PreventedPlanInfo]
  ): Boolean =
    preventedNow.exists(plan => namedPreventedResourceLabel(plan).nonEmpty)

  private def namedPreventedResourceLabel(
      plan: PreventedPlanInfo
  ): Option[String] =
    clean(plan.planId)
      .map(_.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim)
      .filter(_.nonEmpty)
      .filterNot(label => GenericNamedResourceLabels.contains(normalize(label)))
      .filter(_ =>
        plan.breakNeutralized.isEmpty &&
          plan.deniedSquares.isEmpty &&
          (
            plan.preventedThreatType.exists(_.trim.nonEmpty) ||
              plan.deniedResourceClass.exists(_.trim.nonEmpty) ||
              plan.counterplayScoreDrop > 0 ||
              plan.mobilityDelta < 0
          )
      )

  private def isEvidenceBackedTier(raw: String): Boolean =
    val normalized = normalize(raw)
    normalized == "evidence_backed" ||
      normalized == "evidence backed" ||
      normalized == "evidencebacked"

  private def claimProvenance(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass
  ): PlayerFacingClaimProvenanceClass =
    val exactBoundedSimplification =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    val exactPressureIncrease =
      deltaClass == PlayerFacingMoveDeltaClass.PressureIncrease &&
        exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val evidenceBacked =
      StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).nonEmpty ||
        ctx.strategicPlanExperiments.exists(_.evidenceTier == "evidence_backed")
    val pvCoupled =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "pv_coupled")
    val deferred =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "deferred") ||
        ctx.probeRequests.nonEmpty
    if evidenceBacked || exactBoundedSimplification || exactPressureIncrease then
      PlayerFacingClaimProvenanceClass.ProbeBacked
    else if pvCoupled then PlayerFacingClaimProvenanceClass.PvCoupled
    else if deferred then PlayerFacingClaimProvenanceClass.Deferred
    else PlayerFacingClaimProvenanceClass.StructuralOnly

  private def quantifierForDelta(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimQuantifier =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimQuantifier.LineConditioned
    else if
      deltaClass == PlayerFacingMoveDeltaClass.PressureIncrease &&
        exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
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
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      provenanceClass: PlayerFacingClaimProvenanceClass
  ): PlayerFacingClaimStabilityGrade =
    if provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimStabilityGrade.Unknown
    else if
      deltaClass == PlayerFacingMoveDeltaClass.PressureIncrease &&
        exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else
      val evidenceBackedExperiments =
        ctx.strategicPlanExperiments.filter(_.evidenceTier == "evidence_backed")
      if evidenceBackedExperiments.exists(exp =>
          (exp.bestReplyStable || exp.futureSnapshotAligned) && !exp.moveOrderSensitive
        ) then PlayerFacingClaimStabilityGrade.Stable
      else if evidenceBackedExperiments.nonEmpty then PlayerFacingClaimStabilityGrade.Unstable
      else PlayerFacingClaimStabilityGrade.Unknown

  private def claimTaintFlags(
      provenanceClass: PlayerFacingClaimProvenanceClass,
      quantifier: PlayerFacingClaimQuantifier
  ): Set[PlayerFacingClaimTaintFlag] =
    List(
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
