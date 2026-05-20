package lila.commentary.analysis


import lila.commentary.analysis.claim.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ EvidenceRef, EvidenceSourceId, ProofFamilyId, ProofSourceId }
import _root_.chess.{ Bishop, Board, Color, King, Pawn, Queen, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.commentary.{ DirectionalTargetReadiness, GameChronicleMoment, RouteSurfaceMode, StrategicIdeaKind, StrategyIdeaSignal, StrategyPack, StrategyPieceMoveRef }
import lila.commentary.model.*
import lila.commentary.model.strategic.{ VariationLine, VariationTag }
import scala.annotation.unused

private[commentary] enum PlayerFacingTruthMode:
  case Minimal
  case Tactical
  case Strategic

private[commentary] enum PlayerFacingSacrificeClass:
  case TacticalSacrifice
  case StrategicSacrifice
  case None

private[commentary] enum PlayerFacingMoveDeltaClass:
  case NewAccess
  case PressureIncrease
  case ExchangeForcing
  case CounterplayReduction
  case ResourceRemoval
  case PlanAdvance

private[commentary] final case class PlayerFacingMoveDeltaEvidence(
    deltaClass: PlayerFacingMoveDeltaClass,
    anchorTerms: List[String],
    quantifier: PlayerFacingClaimQuantifier = PlayerFacingClaimQuantifier.Existential,
    modalityTier: PlayerFacingClaimModalityTier = PlayerFacingClaimModalityTier.Available,
    attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.StateOnly,
    stabilityGrade: PlayerFacingClaimStabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
    provenanceClass: PlayerFacingClaimProvenanceClass = PlayerFacingClaimProvenanceClass.Deferred,
    certificateStatus: PlayerFacingCertificateStatus = PlayerFacingCertificateStatus.Invalid,
    taintFlags: Set[PlayerFacingClaimTaintFlag] = Set.empty,
    ontologyFamily: PlayerFacingClaimOntologyKind = PlayerFacingClaimOntologyKind.Unknown,
    connectorPermission: Boolean = false,
    packet: PlayerFacingClaimPacket = PlayerFacingClaimPacket.empty
):
  def allowsStrongMainClaim: Boolean =
    PlayerFacingClaimProof.allowsStrongMainClaim(packet)

  def allowsWeakMainClaim: Boolean =
    PlayerFacingClaimProof.allowsWeakMainClaim(packet)

  def check_qualifying: Boolean =
    PlayerFacingClaimProof.check_qualifying(packet)

  def allowsLineEvidenceHook: Boolean =
    PlayerFacingClaimProof.allowsLineEvidenceHook(packet)

private[commentary] object PlayerFacingTruthModePolicy:

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
    val centralBreakTimingStrategic =
      deltaEvidence.exists(delta =>
        delta.packet.proofFamily == CentralBreakTimingWitness.ProofFamily &&
          delta.packet.admitsStrategicTruthMode
      )
    val contractTactical =
      truthContract.exists(contract =>
        contractOwnsDirectTacticalTruth(contract) ||
          contractClaimsForcingTacticalTruth(contract)
      )
    if centralBreakTimingStrategic && !contractTactical then PlayerFacingTruthMode.Strategic
    else if isTactical(truthContract, sacrificeClass, forcingProof) then PlayerFacingTruthMode.Tactical
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
    val queenTradeShieldClaim = queenTradeShieldClaimText(ctx, text)
    val exactIqpInducementClaim = exactIqpInducementClaimText(ctx, text)
    val defenderTradeClaim = defenderTradeClaimText(ctx, text)
    val badPieceLiquidationClaim = badPieceLiquidationClaimText(ctx, text)
    val exactBreakPreventionClaim = exactBreakPreventionClaimText(ctx, surface, text)
    val exactCounterplayRestraintClaim = exactCounterplayRestraintClaimText(ctx, text)
    val exactCentralBreakTimingClaim = exactCentralBreakTimingClaimText(ctx, text)
    val exactPacketStrategicMode =
      deltaEvidence.exists(_.packet.admitsStrategicTruthMode)
    (classify(ctx, strategyPack, truthContract) == PlayerFacingTruthMode.Strategic ||
      exactPacketStrategicMode) &&
      deltaEvidence.exists(delta =>
        (delta.allowsWeakMainClaim || delta.check_qualifying) &&
        !overpromotedStrategicFormula(text) &&
          !containsUnsupportedConnector(text, delta) &&
          (
            queenTradeShieldClaim ||
              exactIqpInducementClaim ||
              defenderTradeClaim ||
              badPieceLiquidationClaim ||
              exactBreakPreventionClaim ||
              exactCounterplayRestraintClaim ||
              exactCentralBreakTimingClaim ||
              (
                strategicTextIsConcrete(text, ctx) &&
                  (
                    strategicTextMatchesMoveLinkedAnchor(text, surface, truthContract) ||
                      delta.anchorTerms.exists(anchor =>
                        anchor.length >= 2 && normalize(text).contains(normalize(anchor))
                      )
                  ) &&
                  strategicTextMatchesDelta(text, delta)
              )
          ) &&
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

  def minimalMoveReviewSentence(
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
    val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    val mainPathOwnerProof = hasMainPathOwnerProof(ctx, surface)
    val centralBreakTimingWitness = centralBreakTimingReleaseWitness(ctx)
    val anchors =
      (
        moveLinkedAnchorTerms(surface, truthContract) ++
          boundedSimplificationLineAnchors(ctx, surface) ++
          BreakPreventionWitness.anchorTerms(ctx, surface, preventedNow) ++
          centralBreakTimingWitness.toList.flatMap(_.ownerSeedTerms) ++
          exactIqpInducementAnchorTerms(ctx) ++
          exactPressureIncreaseAnchorTerms(ctx, surface) ++
          defenderTradeAnchorTerms(ctx) ++
          badPieceLiquidationAnchorTerms(ctx) ++
          queenTradeShieldAnchorTerms(ctx)
      ).distinct
    if anchors.isEmpty ||
        (surfaceLooksShellOnly(surface) && !mainPathOwnerProof) ||
        minorityAttackFixationPacketBlocked(ctx, surface) ||
        (!hasConcreteStrategicEvidence(ctx, surface, truthContract) && centralBreakTimingWitness.isEmpty)
    then None
    else
      val squareAnchors = extractSquareAnchors(anchors)
      val decisionDelta = ctx.decision.map(_.delta)
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

      if BreakPreventionWitness.exact(ctx, surface, preventedNow).nonEmpty then
        result(PlayerFacingMoveDeltaClass.CounterplayReduction)
      else if badPieceLiquidationWitness(ctx).nonEmpty then
        result(PlayerFacingMoveDeltaClass.ExchangeForcing)
      else if hasMainPathExchangeForcing(ctx, surface, bestLine, squareAnchors) then
        result(PlayerFacingMoveDeltaClass.ExchangeForcing)
      else if centralBreakTimingWitness.nonEmpty then
        result(PlayerFacingMoveDeltaClass.PlanAdvance)
      else if hasMainPathSpecificResourceRemoval(ctx, preventedNow, bestLine) then
        result(PlayerFacingMoveDeltaClass.ResourceRemoval)
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
        contract.reasonFamily == DecisiveReasonKind.InvestmentSacrifice ||
          contract.truthClass == DecisiveTruthClass.WinningInvestment ||
          contract.truthClass == DecisiveTruthClass.CompensatedInvestment
      )

  private def contractOwnsDirectTacticalTruth(contract: DecisiveTruthContract): Boolean =
    contract.truthClass == DecisiveTruthClass.Blunder ||
      contract.truthClass == DecisiveTruthClass.MissedWin

  private def contractClaimsForcingTacticalTruth(contract: DecisiveTruthContract): Boolean =
    (contract.truthClass == DecisiveTruthClass.Best &&
      (
        contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense ||
          contract.reasonFamily == DecisiveReasonKind.TacticalRefutation
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
      case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense && forcingProof =>
        Some("This is the only move that keeps the position together.")
      case DecisiveTruthClass.Best if contract.reasonFamily == DecisiveReasonKind.TacticalRefutation && forcingProof =>
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
            contract.reasonFamily == DecisiveReasonKind.InvestmentSacrifice ||
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
    val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    val exactBoundedSimplificationWitness =
      exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val exactPressureIncreaseProof =
      exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val queenTradeShieldProof =
      hasQueenTradeShieldWitness(ctx)
    val exactIqpInducementProof =
      exactIqpInducementWitness(ctx).nonEmpty
    val defenderTradeProof =
      defenderTradeWitness(ctx).nonEmpty
    val badPieceLiquidationProof =
      badPieceLiquidationWitness(ctx).nonEmpty
    val exactCentralBreakTimingProof =
      centralBreakTimingReleaseWitness(ctx).nonEmpty
    val exactBreakPreventionProof =
      BreakPreventionWitness.exact(ctx, surface, preventedNow).nonEmpty
    val exactCounterplayRestraintProof =
      exactCounterplayRestraintOwnerProof(ctx, preventedNow)
    val specificOwnerProof =
      exactBoundedSimplificationWitness ||
        exactPressureIncreaseProof ||
        queenTradeShieldProof ||
        exactIqpInducementProof ||
        defenderTradeProof ||
        badPieceLiquidationProof ||
        exactCentralBreakTimingProof ||
        exactBreakPreventionProof ||
        exactCounterplayRestraintProof
    val moveLinkedAnchor =
      hasMoveLinkedStrategicAnchor(surface) ||
        boundedSimplificationLineAnchors(ctx, surface).nonEmpty ||
        BreakPreventionWitness.anchorTerms(ctx, surface, preventedNow).nonEmpty ||
        exactCentralBreakTimingProof ||
        exactIqpInducementAnchorTerms(ctx).nonEmpty ||
        exactPressureIncreaseAnchorTerms(ctx, surface).nonEmpty ||
        defenderTradeAnchorTerms(ctx).nonEmpty ||
        badPieceLiquidationAnchorTerms(ctx).nonEmpty ||
        queenTradeShieldAnchorTerms(ctx).nonEmpty
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
          specificOwnerProof
      ) &&
      (!surfaceLooksShellOnly(surface) || specificOwnerProof)

  private def hasMainPathOwnerProof(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    (exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty &&
      bestDefenseBranchKeyFromContext(ctx).nonEmpty) ||
      (exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty) ||
      hasQueenTradeShieldWitness(ctx) ||
      exactIqpInducementWitness(ctx).nonEmpty ||
      defenderTradeWitness(ctx).nonEmpty ||
      badPieceLiquidationWitness(ctx).nonEmpty ||
      centralBreakTimingReleaseWitness(ctx).nonEmpty ||
      BreakPreventionWitness.exact(ctx, surface, preventedNow).nonEmpty ||
      exactCounterplayRestraintOwnerProof(ctx, preventedNow)

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
    val preventedNow = ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    val mainPathOwnerProof = hasMainPathOwnerProof(ctx, surface)
    val centralBreakTimingWitness = centralBreakTimingReleaseWitness(ctx)
    val anchors =
      (
        moveLinkedAnchorTerms(surface, truthContract) ++
          boundedSimplificationLineAnchors(ctx, surface) ++
          BreakPreventionWitness.anchorTerms(ctx, surface, preventedNow) ++
          centralBreakTimingWitness.toList.flatMap(_.ownerSeedTerms) ++
          exactIqpInducementAnchorTerms(ctx) ++
          exactPressureIncreaseAnchorTerms(ctx, surface) ++
          defenderTradeAnchorTerms(ctx) ++
          badPieceLiquidationAnchorTerms(ctx)
      ).distinct
    if anchors.isEmpty ||
        (surfaceLooksShellOnly(surface) && !mainPathOwnerProof) ||
        (!hasConcreteStrategicEvidence(ctx, surface, truthContract) && centralBreakTimingWitness.isEmpty)
    then None
    else
      val delta = ctx.delta
      val decisionDelta = ctx.decision.map(_.delta)
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
      val iqpInducementSignal =
        exactIqpInducementWitness(ctx).nonEmpty
      val defenderTradeSignal =
        defenderTradeWitness(ctx).nonEmpty
      def result(kind: PlayerFacingMoveDeltaClass): Option[PlayerFacingMoveDeltaEvidence] =
        Some(
          activeStrategicDeltaEvidence(
            ctx = ctx,
            surface = surface,
            deltaClass = kind,
            anchors = anchors
          )
        )

      if centralBreakTimingWitness.nonEmpty then
        result(PlayerFacingMoveDeltaClass.PlanAdvance)
      else if preventedNow.exists(plan =>
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
          boundedSimplificationSignal ||
          iqpInducementSignal ||
          defenderTradeSignal
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
          exactPressureIncreaseWitness(ctx, surface).nonEmpty
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
    val queenTradeShieldWitnessed =
      hasQueenTradeShieldWitness(ctx)
    val exactIqpInducement =
      exactIqpInducementWitness(ctx).nonEmpty
    val defenderTradeWitnessed =
      defenderTradeWitness(ctx).nonEmpty
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
    queenTradeShieldWitnessed ||
      exactIqpInducement ||
      defenderTradeWitnessed ||
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
    val exactBreakPrevention = BreakPreventionWitness.exact(ctx, surface, preventedNow).nonEmpty
    val provenanceClass =
      if exactBreakPrevention then PlayerFacingClaimProvenanceClass.ProbeBacked
      else claimProvenance(ctx, surface, deltaClass)
    val quantifier =
      if exactBreakPrevention then PlayerFacingClaimQuantifier.BestResponse
      else quantifierForDelta(ctx, surface, deltaClass, provenanceClass)
    val stabilityGrade =
      if exactBreakPrevention then PlayerFacingClaimStabilityGrade.Stable
      else stabilityForDelta(ctx, surface, deltaClass, provenanceClass)
    val attributionGrade =
      if exactBreakPrevention then PlayerFacingClaimAttributionGrade.Distinctive
      else
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
      proofSource: String,
      proofFamily: String,
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
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.SimplificationWindow).get.wireKey
  private[commentary] val ExactTargetFixationProofSource =
    ProofSourceId.ExactTargetFixation.wireKey
  private[commentary] val CarlsbadFixedTargetProbeProofSource =
    ProofSourceId.CarlsbadFixedTargetProbe.wireKey
  private[commentary] val IQPInducementProbeProofSource =
    ProofSourceId.IQPInducementProbe.wireKey
  private[commentary] val DefenderTradeProofSource =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey
  private[commentary] val BadPieceLiquidationProofSource =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).get.wireKey
  private[commentary] val QueenTradeShieldProofSource =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).get.wireKey
  private[commentary] val TargetFocusedCoordinationProofSource =
    ProofSourceId.TargetFocusedCoordinationProbe.wireKey
  private[commentary] val TargetFocusedCoordinationProofFamily =
    ProofFamilyId.TargetFocusedCoordination.wireKey

  private val HalfOpenFilePressureFamily =
    ProofFamilyId.HalfOpenFilePressure.wireKey
  private val NeutralizeKeyBreakFamily =
    ProofFamilyId.NeutralizeKeyBreak.wireKey
  private val CounterplayRestraintFamily =
    ProofFamilyId.CounterplayRestraint.wireKey
  private val TradeKeyDefenderFamily =
    ProofFamilyId.TradeKeyDefender.wireKey
  private val StaticWeaknessFixationFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).get.wireKey
  private val IQPInducementFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.IQPInducement).get.wireKey
  private val DefenderTradeFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey
  private val QueenTradeShieldFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.QueenTradeShield).get.wireKey
  private val BadPieceLiquidationFamily =
    ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.BadPieceLiquidation).get.wireKey
  private val LocalFileEntryBindProofSource =
    ProofSourceId.LocalFileEntryBind.wireKey
  private val CounterplayAxisSuppressionProofSource =
    ProofSourceId.CounterplayAxisSuppression.wireKey
  private val ProphylacticMoveProofSource =
    ProofSourceId.ProphylacticMove.wireKey
  private val ExchangeForcingDeltaProofSource =
    ProofSourceId.ExchangeForcingDelta.wireKey
  private val ActiveMoveDeltaProofSource =
    ProofSourceId.ActiveMoveDelta.wireKey

  private val QueenTradeShieldReleasedClaim =
    "This exchange moves the game into the queenless branch."
  private val DefenderTradeReleasedClaim =
    "This exchange removes a defender on the local branch."
  private val BadPieceLiquidationReleasedClaim =
    "This trade clears the bad piece from the local branch."

  private enum ExactSliceKind:
    case TargetFixation
    case CarlsbadFixedTargetProbe
    case TargetFocusedCoordinationProbe

  private final case class ExactSliceDescriptor(
      kind: ExactSliceKind,
      proofSource: String,
      proofFamily: String,
      triggerKind: String,
      releasedScope: PlayerFacingPacketScope,
      releaseOwnerFamilies: Set[String],
      exemptFromRivalChecks: Boolean = false
  ):
    def packetMatches(packet: PlayerFacingClaimPacket): Boolean =
      packet.proofSource == proofSource &&
        packet.proofFamily == proofFamily

    def ownerSeedMatches(ownerSeed: ClaimOwnerSeed): Boolean =
      ownerSeed.proofSource == proofSource &&
        releaseOwnerFamilies.contains(ownerSeed.proofFamily)

  private val ExactTargetFixationDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.TargetFixation,
      proofSource = ExactTargetFixationProofSource,
      proofFamily = StaticWeaknessFixationFamily,
      triggerKind = "target_fixation",
      releasedScope = PlayerFacingPacketScope.MoveLocal,
      releaseOwnerFamilies = Set(StaticWeaknessFixationFamily)
    )

  private val CarlsbadFixedTargetProbeDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.CarlsbadFixedTargetProbe,
      proofSource = CarlsbadFixedTargetProbeProofSource,
      proofFamily = ProofFamilyId.BackwardPawnTargeting.wireKey,
      triggerKind = "position_probe",
      releasedScope = PlayerFacingPacketScope.PositionLocal,
      releaseOwnerFamilies =
        Set(ProofFamilyId.BackwardPawnTargeting.wireKey),
      exemptFromRivalChecks = true
    )

  private val TargetFocusedCoordinationDescriptor =
    ExactSliceDescriptor(
      kind = ExactSliceKind.TargetFocusedCoordinationProbe,
      proofSource = TargetFocusedCoordinationProofSource,
      proofFamily = TargetFocusedCoordinationProofFamily,
      triggerKind = "position_probe",
      releasedScope = PlayerFacingPacketScope.PositionLocal,
      releaseOwnerFamilies = Set(TargetFocusedCoordinationProofFamily)
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

  private final case class IqpInducementWitness(
      targetSquare: String,
      lineMoves: List[String],
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  )

  private final case class DefenderTradeWitness(
      defenderSquare: String,
      exchangeSquare: String,
      targetSquare: String,
      lineMoves: List[String],
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  )

  private final case class DefenderTradeBranch(
      defenderSquare: String,
      exchangeSquare: String,
      targetSquare: String,
      lineMoves: List[String]
  )

  private final case class BadPieceLiquidationWitness(
      badPieceSquare: String,
      exchangeSquare: String,
      lineMoves: List[String],
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  )

  private final case class BadPieceLiquidationBranch(
      badPieceSquare: String,
      exchangeSquare: String,
      lineMoves: List[String]
  )

  private[commentary] final case class PositionProbeQuestionSeed(
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
        proofSource = ActiveMoveDeltaProofSource,
        proofFamily = genericProofFamily(deltaClass),
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
        proofPathWitness =
          PlayerFacingProofPathWitness(
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
    val tracedPacket =
      ProofContractRules.attachTrace(packet)
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
      packet = tracedPacket
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
        surface = surface,
        ownerSeed = ownerSeed,
        claimGate = claimGate,
        bestDefenseBranchKey = bestDefenseBranchKey,
        continuationTerms = continuationTerms
      )
    val persistence = persistenceForMainPath(ctx, surface, ownerSeed, claimGate)
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
    val packet =
      PlayerFacingClaimPacket(
      claimGate = claimGate,
      proofSource = ownerSeed.proofSource,
      proofFamily = ownerSeed.proofFamily,
      scope = scopeForFallback(fallbackMode, ownerSeed),
      triggerKind = ownerSeed.triggerKind,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = bestDefenseBranchKey,
      sameBranchState = sameBranchState,
      persistence = persistence,
      rivalKind = rivalAssessment.rivalKind,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeed.ownerSeedTerms ++ anchorTerms,
          continuationTerms = continuationTerms,
          rivalTerms = rivalAssessment.rivalWitnessTerms,
          structureTransitionTerms = ownerSeed.structureTransitionTerms
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
        fallbackMode = fallbackMode
      )
    ProofContractRules.attachTrace(packet)

  private def exactPressureIncreaseOwnerSeed(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ClaimOwnerSeed] =
    exactPressureIncreaseWitness(ctx, surface).map { witness =>
      ClaimOwnerSeed(
        proofSource = witness.descriptor.proofSource,
        proofFamily = witness.descriptor.proofFamily,
        triggerKind = witness.descriptor.triggerKind,
        ownerSeedTerms = witness.ownerSeedTerms,
        structureTransitionTerms = witness.structureTransitionTerms
      )
    }

  private def centralBreakTimingReleaseWitness(ctx: NarrativeContext): Option[CentralBreakTimingWitness.Witness] =
    CentralBreakTimingWitness.exact(ctx).filter(_ => centralBreakTimingFamilyAligned(ctx))

  private def centralBreakTimingFamilyAligned(ctx: NarrativeContext): Boolean =
    val evidencePlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    val rivalSubplans =
      Set(
        PlanTaxonomy.PlanKind.BreakPrevention.id,
        PlanTaxonomy.PlanKind.WingBreakTiming.id
      )
    val centralPlan =
      evidencePlans.exists(plan =>
        plan.subplanId.exists(id => normalize(id) == PlanTaxonomy.PlanKind.CentralBreakTiming.id)
      ) ||
        ctx.strategicPlanExperiments.exists(exp =>
          exp.subplanId.exists(id => normalize(id) == PlanTaxonomy.PlanKind.CentralBreakTiming.id) &&
            isEvidenceBackedTier(exp.evidenceTier)
        )
    val rivalPlan =
      evidencePlans.exists(plan =>
        plan.subplanId.exists(id => rivalSubplans.contains(normalize(id)))
      ) ||
        ctx.strategicPlanExperiments.exists(exp =>
          exp.subplanId.exists(id => rivalSubplans.contains(normalize(id))) &&
            isEvidenceBackedTier(exp.evidenceTier)
      )
    centralPlan || !rivalPlan

  private def ownerSeedForMainPath(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      deltaClass: PlayerFacingMoveDeltaClass,
      preventedNow: List[PreventedPlanInfo]
  ): ClaimOwnerSeed =
    val planOwner = leadingProofFamily(ctx)
    val trigger = leadingTriggerKind(ctx)
    val localFileEntryPair = LocalFileEntryProof.certifiedSurfacePair(ctx)
    val targetComplexTerms = targetComplexWitnessTerms(ctx, surface)
    val tradeOwnerTerms = tradeOwnerSeedTerms(ctx, surface)
    val tradeTransitionTerms = tradeStructureTransitionTerms(ctx, surface)
    val namedPreventedTerms = namedPreventedResourceTerms(preventedNow)
    val breakTerms = breakResourceTerms(preventedNow)
    val breakPreventionWitness = BreakPreventionWitness.candidate(ctx, surface, preventedNow)
    val centralBreakTimingWitness = centralBreakTimingReleaseWitness(ctx)
    val exactPressureIncreaseSeed = exactPressureIncreaseOwnerSeed(ctx, surface)
    val prophylacticRestraintPlan =
      trigger.contains(CounterplayRestraintFamily) || planOwner.contains("prophylaxis_restraint")
    deltaClass match
      case PlayerFacingMoveDeltaClass.PressureIncrease
          if exactPressureIncreaseSeed.nonEmpty =>
        exactPressureIncreaseSeed.get
      case PlayerFacingMoveDeltaClass.PlanAdvance
          if centralBreakTimingWitness.nonEmpty =>
        val witness = centralBreakTimingWitness.get
        ClaimOwnerSeed(
          proofSource = CentralBreakTimingWitness.ProofSource,
          proofFamily = CentralBreakTimingWitness.ProofFamily,
          triggerKind = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
          ownerSeedTerms = witness.ownerSeedTerms,
          structureTransitionTerms = witness.structureTransitionTerms
        )
      case PlayerFacingMoveDeltaClass.CounterplayReduction
          if localFileEntryPair.nonEmpty =>
        val pair = localFileEntryPair.get
        ClaimOwnerSeed(
          proofSource = LocalFileEntryBindProofSource,
          proofFamily = HalfOpenFilePressureFamily,
          triggerKind = "file_entry_denial",
          ownerSeedTerms = List(pair.file, pair.entrySquare),
          structureTransitionTerms = List(s"file-entry:${pair.file}:${pair.entrySquare}")
        )
      case PlayerFacingMoveDeltaClass.ResourceRemoval
          if localFileEntryPair.nonEmpty =>
        val pair = localFileEntryPair.get
        ClaimOwnerSeed(
          proofSource = LocalFileEntryBindProofSource,
          proofFamily = HalfOpenFilePressureFamily,
          triggerKind = "file_entry_denial",
          ownerSeedTerms = List(pair.file, pair.entrySquare),
          structureTransitionTerms = List(s"file-entry:${pair.file}:${pair.entrySquare}")
        )
      case PlayerFacingMoveDeltaClass.CounterplayReduction | PlayerFacingMoveDeltaClass.ResourceRemoval
          if prophylacticRestraintPlan &&
            hasNamedPreventedResource(preventedNow) =>
        ClaimOwnerSeed(
          proofSource = ProphylacticMoveProofSource,
          proofFamily = CounterplayRestraintFamily,
          triggerKind = ProphylacticMoveProofSource,
          ownerSeedTerms = namedPreventedTerms,
          structureTransitionTerms = namedPreventedTerms
        )
      case PlayerFacingMoveDeltaClass.CounterplayReduction | PlayerFacingMoveDeltaClass.ResourceRemoval
          if breakPreventionWitness.nonEmpty =>
        val witness = breakPreventionWitness.get
        ClaimOwnerSeed(
          proofSource = CounterplayAxisSuppressionProofSource,
          proofFamily = NeutralizeKeyBreakFamily,
          triggerKind = "break_neutralization",
          ownerSeedTerms = witness.ownerSeedTerms,
          structureTransitionTerms = witness.structureTransitionTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if hasQueenTradeShieldWitness(ctx) =>
        val lineMoves = queenTradeShieldLineMoves(ctx)
        ClaimOwnerSeed(
          proofSource = QueenTradeShieldProofSource,
          proofFamily = QueenTradeShieldFamily,
          triggerKind = PlanTaxonomy.PlanKind.QueenTradeShield.id,
          ownerSeedTerms =
            (List("queen_trade_shield", "queenless_branch", "queen_trade") ++ lineMoves.takeRight(2)).distinct,
          structureTransitionTerms = (lineMoves ++ List("queenless_branch", "queen_trade")).distinct
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if exactIqpInducementWitness(ctx).nonEmpty =>
        val witness = exactIqpInducementWitness(ctx).get
        ClaimOwnerSeed(
          proofSource = IQPInducementProbeProofSource,
          proofFamily = IQPInducementFamily,
          triggerKind = PlanTaxonomy.PlanKind.IQPInducement.id,
          ownerSeedTerms = witness.ownerSeedTerms,
          structureTransitionTerms = witness.structureTransitionTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if defenderTradeWitness(ctx).nonEmpty =>
        val witness = defenderTradeWitness(ctx).get
        ClaimOwnerSeed(
          proofSource = DefenderTradeProofSource,
          proofFamily = DefenderTradeFamily,
          triggerKind = PlanTaxonomy.PlanKind.DefenderTrade.id,
          ownerSeedTerms = witness.ownerSeedTerms,
          structureTransitionTerms = witness.structureTransitionTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if badPieceLiquidationWitness(ctx).nonEmpty =>
        val witness = badPieceLiquidationWitness(ctx).get
        ClaimOwnerSeed(
          proofSource = BadPieceLiquidationProofSource,
          proofFamily = BadPieceLiquidationFamily,
          triggerKind = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
          ownerSeedTerms = witness.ownerSeedTerms,
          structureTransitionTerms = witness.structureTransitionTerms
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
          proofSource = BoundedFavorableSimplificationFamily,
          proofFamily = BoundedFavorableSimplificationFamily,
          triggerKind = BoundedFavorableSimplificationFamily,
          ownerSeedTerms =
            (tradeOwnerTerms ++ targetComplexTerms ++ moveLocalAnchorSeedTerms(ctx, surface)).distinct,
          structureTransitionTerms = targetComplexTerms
        )
      case PlayerFacingMoveDeltaClass.ExchangeForcing
          if likelyTradeKeyDefender(ctx, surface) || tradeOwnerTerms.nonEmpty || tradeTransitionTerms.nonEmpty =>
        ClaimOwnerSeed(
          proofSource = ExchangeForcingDeltaProofSource,
          proofFamily = TradeKeyDefenderFamily,
          triggerKind = trigger.getOrElse(TradeKeyDefenderFamily),
          ownerSeedTerms = tradeOwnerTerms,
          structureTransitionTerms = tradeTransitionTerms
        )
      case _ =>
        val guardedPlanOwner =
          planOwner.filterNot { owner =>
            (owner == HalfOpenFilePressureFamily && localFileEntryPair.isEmpty) ||
            (owner == NeutralizeKeyBreakFamily && !preventedNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty))) ||
            (owner == CounterplayRestraintFamily && !hasNamedPreventedResource(preventedNow)) ||
            (owner == "prophylaxis_restraint" && !hasNamedPreventedResource(preventedNow))
          }
        ClaimOwnerSeed(
          proofSource = genericProofSource(deltaClass),
          proofFamily = guardedPlanOwner.getOrElse(genericProofFamily(deltaClass)),
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

  private def leadingProofFamily(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport
      .evidenceBackedMainPlans(ctx)
      .headOption
      .map(plan => PlanMatcher.proofFamily(plan.themeL1, plan.subplanId))

  private def secondaryProofFamily(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport
      .evidenceBackedMainPlans(ctx)
      .lift(1)
      .map(plan => PlanMatcher.proofFamily(plan.themeL1, plan.subplanId))

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
    ownerSeed.proofFamily match
      case NeutralizeKeyBreakFamily =>
        subplan.contains(normalize(PlanTaxonomy.PlanKind.BreakPrevention.id)) ||
          experiment.counterBreakNeutralized
      case HalfOpenFilePressureFamily =>
        Set(
          PlanTaxonomy.PlanKind.KeySquareDenial.id,
          PlanTaxonomy.PlanKind.OpenFilePressure.id,
          PlanTaxonomy.PlanKind.RookFileTransfer.id
        ).map(normalize).exists(subplan.contains)
      case CounterplayRestraintFamily =>
        subplan.contains(normalize(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id)) ||
          theme == normalize(PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id)
      case TradeKeyDefenderFamily =>
        Set(
          PlanTaxonomy.PlanKind.DefenderTrade.id,
          PlanTaxonomy.PlanKind.SimplificationConversion.id
        ).map(normalize).exists(subplan.contains) ||
          theme == normalize(PlanTaxonomy.PlanTheme.FavorableExchange.id) ||
          theme == normalize(PlanTaxonomy.PlanTheme.AdvantageTransformation.id)
      case family if family == BoundedFavorableSimplificationFamily =>
        subplan.contains(normalize(PlanTaxonomy.PlanKind.SimplificationWindow.id)) ||
          (
            theme == normalize(PlanTaxonomy.PlanTheme.FavorableExchange.id) &&
              subplan.contains(normalize(PlanTaxonomy.PlanKind.SimplificationWindow.id))
          )
      case family
          if Set(
            PlanTaxonomy.PlanTheme.WeaknessFixation.id,
            PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
            PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
            PlanTaxonomy.PlanKind.MinorityAttackFixation.id,
            PlanTaxonomy.PlanKind.IQPInducement.id
          ).contains(family) =>
        Set(
          PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
          PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
          PlanTaxonomy.PlanKind.MinorityAttackFixation.id,
          PlanTaxonomy.PlanKind.IQPInducement.id
        ).map(normalize).exists(subplan.contains) ||
          theme == normalize(PlanTaxonomy.PlanTheme.WeaknessFixation.id)
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
    val exactIqpContinuation =
      exactIqpInducementContinuationTerms(ctx, ownerSeed)
    val defenderTradeContinuation =
      defenderTradeContinuationTerms(ctx, ownerSeed)
    val badPieceLiquidationContinuation =
      badPieceLiquidationContinuationTerms(ctx, ownerSeed)
    val queenTradeShieldContinuation =
      Option.when(
        ownerSeed.proofFamily == QueenTradeShieldFamily &&
          hasQueenTradeShieldWitness(ctx)
      )(queenTradeShieldLineMoves(ctx)).getOrElse(Nil)
    val centralBreakTimingContinuation =
      Option.when(ownerSeed.proofFamily == CentralBreakTimingWitness.ProofFamily) {
        CentralBreakTimingWitness.exact(ctx).toList.flatMap { witness =>
          (
            List(
              "central_break_timing_branch",
              s"central_break:${witness.breakSquare}",
              s"break_move:${witness.breakMove}"
            ) ++ witness.structureTransitionTerms
          ).distinct
        }
      }.getOrElse(Nil)
    (
      bestDefenseBranchKey.toList ++
        bestDefenseMove.toList ++
        exactTradeContinuation.toList.flatMap(square =>
          List("exact_trade_continuation", s"exchange_square:$square", square)
        ) ++
        exactSliceContinuation ++
        exactIqpContinuation ++
        defenderTradeContinuation ++
        badPieceLiquidationContinuation ++
        queenTradeShieldContinuation ++
        centralBreakTimingContinuation ++
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
    val localFileEntryPair = LocalFileEntryProof.certifiedSurfacePair(ctx)
    val exactSignalFamilies =
      List(
        Option.when(localFileEntryPair.nonEmpty)(HalfOpenFilePressureFamily -> "exact:file_entry_pair"),
        Option.when(preventedNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty)))(NeutralizeKeyBreakFamily -> "exact:break_denial"),
        Option.when(hasNamedPreventedResource(preventedNow))(CounterplayRestraintFamily -> "exact:named_resource"),
        Option.when(targetComplexWitnessTerms(ctx, surface).nonEmpty)(StaticWeaknessFixationFamily -> "exact:target_complex"),
        Option.when(tradeStructureTransitionTerms(ctx, surface).nonEmpty)(ownerSeed.proofFamily -> "exact:trade_transition")
      ).flatten
    val secondaryPlan = secondaryProofFamily(ctx)
    val secondaryIdeaFamily =
      surface.secondaryIdea.flatMap(proofFamilyForIdea)
    val ownerStrength =
      ownerSeed.ownerSeedTerms.size +
        ownerSeed.structureTransitionTerms.size +
        (if sameBranchState == PlayerFacingSameBranchState.Proven then 3 else if sameBranchState == PlayerFacingSameBranchState.Ambiguous then 1 else 0) +
        (if persistence == PlayerFacingClaimPersistence.Stable then 2 else if persistence != PlayerFacingClaimPersistence.Broken then 1 else 0)
    val rivalTerms =
      (
        secondaryPlan.filter(_ != ownerSeed.proofFamily).map(family => s"secondary_plan:$family").toList ++
          secondaryIdeaFamily.filter(_ != ownerSeed.proofFamily).map(family => s"secondary_idea:$family").toList ++
          exactSignalFamilies.collect { case (family, tag) if family != ownerSeed.proofFamily => s"$tag:$family" }
      ).distinct
    val rivalKind =
      secondaryPlan.filter(_ != ownerSeed.proofFamily)
        .orElse(secondaryIdeaFamily.filter(_ != ownerSeed.proofFamily))
        .orElse(exactSignalFamilies.collectFirst { case (family, _) if family != ownerSeed.proofFamily => family })
    val rivalStrength =
      rivalKind.map { family =>
        rivalTerms.count(_.endsWith(s":$family")) +
          exactSignalFamilies.count(_._1 == family) +
          secondaryPlan.count(_ == family) * 2 +
          secondaryIdeaFamily.count(_ == family)
      }.getOrElse(0)
    val rivalStoryAlive =
      rivalKind.exists { family =>
        family != ownerSeed.proofFamily &&
          (
            rivalStrength > ownerStrength ||
              (rivalStrength == ownerStrength && PlayerFacingClaimProof.exactProofFamily(ownerSeed.proofFamily)) ||
              (ownerSeed.proofFamily == TradeKeyDefenderFamily && rivalStrength > 0)
          )
      }
    RivalAssessment(
      rivalKind = rivalKind,
      rivalWitnessTerms = rivalTerms,
      rivalStoryAlive = rivalStoryAlive,
      rivalReleaseRisk = rivalStoryAlive && ownerSeed.proofFamily != deltaFamilyFallback(deltaClass)
    )

  private def proofFamilyForIdea(idea: StrategyIdeaSignal): Option[String] =
    idea.kind match
      case StrategicIdeaKind.CounterplaySuppression => Some(NeutralizeKeyBreakFamily)
      case StrategicIdeaKind.Prophylaxis            => Some(CounterplayRestraintFamily)
      case StrategicIdeaKind.LineOccupation         => Some(HalfOpenFilePressureFamily)
      case StrategicIdeaKind.TargetFixing =>
        if ideaHasSource(idea, EvidenceSourceId.CarlsbadFixationProfile) then
          Some(ProofFamilyId.BackwardPawnTargeting.wireKey)
        else if ideaHasSource(idea, EvidenceSourceId.WeakComplexFixation) ||
            ideaHasSource(idea, EvidenceSourceId.DirectionalTargetFixation)
        then
          Some(StaticWeaknessFixationFamily)
        else Some(StaticWeaknessFixationFamily)
      case StrategicIdeaKind.FavorableTradeOrTransformation =>
        if ideaIsBoundedFavorableSimplification(idea) then Some(BoundedFavorableSimplificationFamily)
        else Some(TradeKeyDefenderFamily)
      case _ => None

  private def ideaHasSource(idea: StrategyIdeaSignal, source: EvidenceSourceId): Boolean =
    idea.evidenceRefs.contains(EvidenceRef.Source(source).wireKey)

  private def ideaHasAnySource(
      idea: StrategyIdeaSignal,
      sources: Set[EvidenceSourceId]
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
          EvidenceSourceId.ClassificationTransformationWindow,
          EvidenceSourceId.ExchangeAvailabilityBridge,
          EvidenceSourceId.CaptureExchangeTransformation,
          EvidenceSourceId.IqpSimplificationProfile,
          EvidenceSourceId.PlanMatchTransformation
        )
      ) &&
      !ideaHasAnySource(
        idea,
        Set(
          EvidenceSourceId.RemovingTheDefender,
          EvidenceSourceId.WinningEndgameTransition
        )
      )

  private def boundedSimplificationLineAnchors(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): List[String] =
    exactBoundedSimplificationExchangeSquare(ctx, surface).toList

  private def deltaFamilyFallback(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.CounterplayReduction => NeutralizeKeyBreakFamily
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => CounterplayRestraintFamily
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => TradeKeyDefenderFamily
      case _                                              => genericProofFamily(deltaClass)

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

  private def queenTradeShieldAnchorTerms(ctx: NarrativeContext): List[String] =
    val lineMoves = queenTradeShieldLineMoves(ctx)
    if lineMoves.nonEmpty then
      (List("queen_trade_shield", "queenless_branch", "queen_trade") ++ lineMoves.takeRight(2)).distinct
    else Nil

  private def queenTradeShieldClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    hasQueenTradeShieldWitness(ctx) &&
      normalize(text) == normalize(QueenTradeShieldReleasedClaim)

  private def exactBreakPreventionClaimText(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      text: String
  ): Boolean =
    BreakPreventionWitness
      .exact(
        ctx,
        surface,
        ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
      )
      .exists(witness => normalize(text) == normalize(s"This keeps ${witness.breakToken} from coming right away."))

  private def exactCentralBreakTimingClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    centralBreakTimingReleaseWitness(ctx).exists { witness =>
      normalize(text) == normalize(centralBreakTimingClaimText(witness))
    }

  private[commentary] def centralBreakTimingClaimText(
      witness: CentralBreakTimingWitness.Witness
  ): String =
    s"A local reading is that this improves the ${witness.breakToken} timing on this branch."

  private def exactCounterplayRestraintClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    val preventedNow =
      ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    exactCounterplayRestraintOwnerProof(ctx, preventedNow) &&
      preventedNow
        .flatMap(namedPreventedResourceLabel)
        .exists(resource => normalize(text) == normalize(s"This slows down $resource before it gets started."))

  private def hasQueenTradeShieldWitness(ctx: NarrativeContext): Boolean =
    queenTradeShieldLineMoves(ctx).nonEmpty

  private def queenTradeShieldLineMoves(ctx: NarrativeContext): List[String] =
    Fen.read(Standard, Fen.Full(ctx.fen))
      .flatMap(position => queenTradeShieldLineMoves(position.board, position.color, normalizedTopUciMoves(ctx)))
      .getOrElse(Nil)

  private def queenTradeShieldLineMoves(
      board: Board,
      movingSide: Color,
      topMoves: List[String]
  ): Option[List[String]] =
    topMoves.zipWithIndex.flatMap { (move, index) =>
      val sameSideMove = index % 2 == 0
      val reply = topMoves.lift(index + 1)
      for
        queenFrom <- squareFromKey(squareKey(move, from = true))
        queenExchange <- squareFromKey(squareKey(move, from = false))
        if sameSideMove
        movingQueen <- board.pieceAt(queenFrom)
        capturedQueen <- board.pieceAt(queenExchange)
        if movingQueen.color == movingSide && movingQueen.role == Queen
        if capturedQueen.color != movingSide && capturedQueen.role == Queen
        replyMove <- reply
        kingFrom <- squareFromKey(squareKey(replyMove, from = true))
        replyDest <- squareFromKey(squareKey(replyMove, from = false))
        recapturingKing <- board.pieceAt(kingFrom)
        if recapturingKing.color != movingSide && recapturingKing.role == King
        if replyDest == queenExchange
      yield topMoves.take(index + 2)
    }.headOption

  private def normalizeUciMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

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
              descriptor.proofSource,
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
      surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed,
      claimGate: PlanEvidenceEvaluator.ClaimCertification,
      bestDefenseBranchKey: Option[String],
      continuationTerms: List[String]
  ): PlayerFacingSameBranchState =
    val branchVisible = bestDefenseBranchKey.nonEmpty
    val exactBreakPrevention = exactBreakPreventionProof(ctx, surface, ownerSeed)
    val concreteOwnerSeed = ownerSeed.ownerSeedTerms.nonEmpty
    if exactBreakPrevention && branchVisible && concreteOwnerSeed && continuationTerms.nonEmpty then
      PlayerFacingSameBranchState.Proven
    else if claimGate.provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingSameBranchState.Missing
    else if !branchVisible then
      PlayerFacingSameBranchState.Missing
    else
      val evidenceBacked = ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed)
      val exactTradeContinuation =
        exactBoundedSimplificationContinuationSquare(ctx, ownerSeed)
      val exactSliceContinuation =
        exactSliceContinuationTerms(ctx, ownerSeed)
      val defenderTradeContinuation =
        defenderTradeContinuationTerms(ctx, ownerSeed)
      val exactCentralBreakTimingContinuation =
        Option.when(ownerSeed.proofFamily == CentralBreakTimingWitness.ProofFamily) {
          CentralBreakTimingWitness.exact(ctx).toList.flatMap(_.structureTransitionTerms)
        }.getOrElse(Nil)
      val stableBestDefense = evidenceBacked.exists(_.bestReplyStable)
      val futureAligned = evidenceBacked.exists(_.futureSnapshotAligned)
      val moveOrderClean = !evidenceBacked.exists(_.moveOrderSensitive)
      val concreteTransition = ownerSeed.structureTransitionTerms.nonEmpty
      if exactSliceDescriptor(ownerSeed).nonEmpty &&
          concreteOwnerSeed &&
          exactSliceContinuation.nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.proofFamily == BoundedFavorableSimplificationFamily &&
          concreteOwnerSeed &&
          exactTradeContinuation.nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.proofFamily == IQPInducementFamily &&
          concreteOwnerSeed &&
          exactIqpInducementContinuationTerms(ctx, ownerSeed).nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.proofFamily == DefenderTradeFamily &&
          concreteOwnerSeed &&
          defenderTradeContinuation.nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.proofFamily == BadPieceLiquidationFamily &&
          concreteOwnerSeed &&
          badPieceLiquidationContinuationTerms(ctx, ownerSeed).nonEmpty &&
          continuationTerms.nonEmpty
      then
        PlayerFacingSameBranchState.Proven
      else if ownerSeed.proofFamily == CentralBreakTimingWitness.ProofFamily &&
          concreteOwnerSeed &&
          exactCentralBreakTimingContinuation.nonEmpty &&
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
      surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed,
      claimGate: PlanEvidenceEvaluator.ClaimCertification
  ): PlayerFacingClaimPersistence =
    if exactBreakPreventionProof(ctx, surface, ownerSeed) &&
        claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
    then PlayerFacingClaimPersistence.Stable
    else if claimGate.provenanceClass != PlayerFacingClaimProvenanceClass.ProbeBacked then
      PlayerFacingClaimPersistence.Broken
    else if
      ownerSeed.proofFamily == QueenTradeShieldFamily &&
        hasQueenTradeShieldWitness(ctx)
    then PlayerFacingClaimPersistence.BestDefenseOnly
    else
      val evidenceBacked = ownerLinkedEvidenceBackedExperiments(ctx, ownerSeed)
      if exactSliceContinuationTerms(ctx, ownerSeed).nonEmpty &&
          exactSliceDescriptor(ownerSeed).nonEmpty &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if exactBoundedSimplificationContinuationSquare(ctx, ownerSeed).nonEmpty &&
          ownerSeed.proofFamily == BoundedFavorableSimplificationFamily &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if exactIqpInducementContinuationTerms(ctx, ownerSeed).nonEmpty &&
          ownerSeed.proofFamily == IQPInducementFamily &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if defenderTradeContinuationTerms(ctx, ownerSeed).nonEmpty &&
          ownerSeed.proofFamily == DefenderTradeFamily &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if badPieceLiquidationContinuationTerms(ctx, ownerSeed).nonEmpty &&
          ownerSeed.proofFamily == BadPieceLiquidationFamily &&
          claimGate.stabilityGrade != PlayerFacingClaimStabilityGrade.Unstable
      then
        PlayerFacingClaimPersistence.Stable
      else if CentralBreakTimingWitness.exact(ctx).nonEmpty &&
          ownerSeed.proofFamily == CentralBreakTimingWitness.ProofFamily &&
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

  private def exactBreakPreventionProof(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot,
      ownerSeed: ClaimOwnerSeed
  ): Boolean =
    ownerSeed.proofFamily == NeutralizeKeyBreakFamily &&
      BreakPreventionWitness
        .exact(
          ctx,
          surface,
          ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
        )
        .nonEmpty

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
    if PlayerFacingClaimProof.exactProofFamily(ownerSeed.proofFamily)
    then
      sameBranchState match
        case PlayerFacingSameBranchState.Missing =>
          reasons += PlayerFacingClaimSuppressionReason.SameBranchMissing
        case PlayerFacingSameBranchState.Ambiguous =>
          reasons += PlayerFacingClaimSuppressionReason.SameBranchAmbiguous
        case PlayerFacingSameBranchState.Proven => ()
    if ownerSeed.proofFamily == TradeKeyDefenderFamily then
      reasons += PlayerFacingClaimSuppressionReason.SupportOnlyReinflation
    if standaloneEntrySquareDenialReinflates(ownerSeed) then
      reasons += PlayerFacingClaimSuppressionReason.ScopeInflation
    if ownerSeed.proofFamily == BoundedFavorableSimplificationFamily then
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
    if ownerSeed.proofFamily == NeutralizeKeyBreakFamily &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence != PlayerFacingClaimPersistence.Stable
    then risks += PlayerFacingClaimReleaseRisk.RouteMirage
    if ownerSeed.proofFamily == CounterplayRestraintFamily &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence != PlayerFacingClaimPersistence.Stable
    then risks += PlayerFacingClaimReleaseRisk.RouteMirage
    if ownerSeed.proofFamily == CentralBreakTimingWitness.ProofFamily &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence != PlayerFacingClaimPersistence.Stable
    then risks += PlayerFacingClaimReleaseRisk.RouteMirage
    if (ownerSeed.proofSource == LocalFileEntryBindProofSource || ownerSeed.proofFamily == HalfOpenFilePressureFamily) &&
        sameBranchState != PlayerFacingSameBranchState.Proven
    then
      risks += PlayerFacingClaimReleaseRisk.SurfaceReinflation
    if (ownerSeed.proofSource == ProphylacticMoveProofSource || ownerSeed.proofFamily == CounterplayRestraintFamily) &&
        sameBranchState != PlayerFacingSameBranchState.Proven
    then
      risks += PlayerFacingClaimReleaseRisk.SurfaceReinflation
    if ownerSeed.proofFamily == TradeKeyDefenderFamily then
      risks += PlayerFacingClaimReleaseRisk.RivalRelease
    if rivalAssessment.rivalReleaseRisk &&
        !exactSliceDescriptor(ownerSeed).exists(_.exemptFromRivalChecks)
    then
      risks += PlayerFacingClaimReleaseRisk.RivalRelease
    if ownerSeed.proofFamily == BoundedFavorableSimplificationFamily &&
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
      PlayerFacingClaimProof.allowsWeakMainClaim(
        certificateStatus = claimGate.certificateStatus,
        quantifier = claimGate.quantifier,
        attribution = claimGate.attributionGrade,
        stability = claimGate.stabilityGrade,
        provenance = claimGate.provenanceClass,
        taintFlags = claimGate.taintFlags.toSet
      )
    val allowsLine =
      PlayerFacingClaimProof.allowsLineEvidenceHook(
        certificateStatus = claimGate.certificateStatus,
        provenance = claimGate.provenanceClass,
        taintFlags = claimGate.taintFlags.toSet
      )
    val lineOnlyPilot =
      PlayerFacingClaimPacket.isLineOnlyPilot(ownerSeed.proofSource, ownerSeed.proofFamily)
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
        ownerSeed.proofFamily != TradeKeyDefenderFamily &&
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
      plan.subplanId.contains(PlanTaxonomy.PlanKind.DefenderTrade.id)
    ) ||
      evidenceText.exists(text => containsAny(text, List("defender", "guard", "cover", "protector"))) ||
      surface.topMoveRef.exists(ref =>
        containsAny(normalize(ref.idea), List("defender", "guard", "cover", "protector"))
      )

  private def genericProofSource(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => ProofSourceId.NewAccessDelta.wireKey
      case PlayerFacingMoveDeltaClass.PressureIncrease     => ProofSourceId.PressureIncreaseDelta.wireKey
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => ExchangeForcingDeltaProofSource
      case PlayerFacingMoveDeltaClass.CounterplayReduction => ProofSourceId.CounterplayReductionDelta.wireKey
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => ProofSourceId.ResourceRemovalDelta.wireKey
      case PlayerFacingMoveDeltaClass.PlanAdvance          => ProofSourceId.PlanAdvanceDelta.wireKey

  private def genericProofFamily(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => ProofFamilyId.NewAccess.wireKey
      case PlayerFacingMoveDeltaClass.PressureIncrease     => ProofFamilyId.PressureIncrease.wireKey
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => ProofFamilyId.ExchangeForcing.wireKey
      case PlayerFacingMoveDeltaClass.CounterplayReduction => ProofFamilyId.CounterplayReduction.wireKey
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => ProofFamilyId.ResourceRemoval.wireKey
      case PlayerFacingMoveDeltaClass.PlanAdvance          => ProofFamilyId.PlanAdvance.wireKey

  private def genericTriggerKind(deltaClass: PlayerFacingMoveDeltaClass): String =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => ProofFamilyId.NewAccess.wireKey
      case PlayerFacingMoveDeltaClass.PressureIncrease     => ProofFamilyId.PressureIncrease.wireKey
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => ProofFamilyId.ExchangeForcing.wireKey
      case PlayerFacingMoveDeltaClass.CounterplayReduction => ProofFamilyId.CounterplayReduction.wireKey
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => ProofFamilyId.ResourceRemoval.wireKey
      case PlayerFacingMoveDeltaClass.PlanAdvance          => ProofFamilyId.PlanAdvance.wireKey

  private def ontologyFamilyForActiveDelta(
      deltaClass: PlayerFacingMoveDeltaClass
  ): PlayerFacingClaimOntologyKind =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess            => PlayerFacingClaimOntologyKind.Access
      case PlayerFacingMoveDeltaClass.PressureIncrease     => PlayerFacingClaimOntologyKind.Pressure
      case PlayerFacingMoveDeltaClass.ExchangeForcing      => PlayerFacingClaimOntologyKind.Exchange
      case PlayerFacingMoveDeltaClass.CounterplayReduction => PlayerFacingClaimOntologyKind.CounterplayRestraint
      case PlayerFacingMoveDeltaClass.ResourceRemoval      => PlayerFacingClaimOntologyKind.ResourceRemoval
      case PlayerFacingMoveDeltaClass.PlanAdvance          => PlayerFacingClaimOntologyKind.PlanAdvance

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
    else if ownerSeed.proofFamily == IQPInducementFamily then
      ownerSeed.proofSource == IQPInducementProbeProofSource &&
        bestDefenseBranchKey.nonEmpty &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence == PlayerFacingClaimPersistence.Stable
    else if ownerSeed.proofFamily == DefenderTradeFamily then
      bestDefenseBranchKey.nonEmpty &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence == PlayerFacingClaimPersistence.Stable
    else if ownerSeed.proofFamily == BadPieceLiquidationFamily then
      ownerSeed.proofSource == BadPieceLiquidationProofSource &&
        bestDefenseBranchKey.nonEmpty &&
        sameBranchState == PlayerFacingSameBranchState.Proven &&
        persistence == PlayerFacingClaimPersistence.Stable
    else if isReviewedAbsorbedWeaknessProofFamily(ownerSeed.proofFamily) then false
    else
      ownerSeed.proofFamily match
        case HalfOpenFilePressureFamily =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case NeutralizeKeyBreakFamily =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case CounterplayRestraintFamily =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case family if family == CentralBreakTimingWitness.ProofFamily =>
          ownerSeed.proofSource == CentralBreakTimingWitness.ProofSource &&
            bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case family if family == BoundedFavorableSimplificationFamily =>
          bestDefenseBranchKey.nonEmpty &&
            sameBranchState == PlayerFacingSameBranchState.Proven &&
            persistence == PlayerFacingClaimPersistence.Stable
        case TradeKeyDefenderFamily => false
        case _                    => true

  private def standaloneEntrySquareDenialReinflates(
      ownerSeed: ClaimOwnerSeed
  ): Boolean =
    ownerSeed.triggerKind == "entry_square_denial" &&
      ownerSeed.proofSource != LocalFileEntryBindProofSource &&
      ownerSeed.proofFamily != HalfOpenFilePressureFamily &&
      ownerSeed.proofFamily != CounterplayRestraintFamily

  private def boundedSimplificationSameJobConversion(
      ctx: NarrativeContext
  ): Boolean =
    leadingProofFamily(ctx).contains(PlanTaxonomy.PlanKind.SimplificationConversion.id) ||
      secondaryProofFamily(ctx).contains(PlanTaxonomy.PlanKind.SimplificationConversion.id) ||
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
    leadingProofFamily(ctx).contains(DefenderTradeFamily) ||
      secondaryProofFamily(ctx).contains(DefenderTradeFamily) ||
      surface.dominantIdea.exists(ideaHasSource(_, EvidenceSourceId.RemovingTheDefender)) ||
      surface.secondaryIdea.exists(ideaHasSource(_, EvidenceSourceId.RemovingTheDefender)) ||
      likelyTradeKeyDefender(ctx, surface)

  private def boundedSimplificationRouteBindRelabel(
      rivalAssessment: RivalAssessment
  ): Boolean =
    rivalAssessment.rivalStoryAlive &&
      rivalAssessment.rivalKind.contains(HalfOpenFilePressureFamily)

  private def boundedSimplificationBetterEndgameInflation(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    ctx.semantic.exists(_.endgameFeatures.exists(feature =>
      normalize(feature.theoreticalOutcomeHint) == "win"
    )) ||
      surface.dominantIdea.exists(ideaHasSource(_, EvidenceSourceId.WinningEndgameTransition)) ||
      surface.secondaryIdea.exists(ideaHasSource(_, EvidenceSourceId.WinningEndgameTransition))

  private def boundedSimplificationB7Drift(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val boundedIdeaVisible =
      surface.dominantIdea.exists(ideaIsBoundedFavorableSimplification) ||
        surface.secondaryIdea.exists(ideaIsBoundedFavorableSimplification)
    !boundedIdeaVisible &&
      leadingProofFamily(ctx).exists(family =>
        family == PlanTaxonomy.PlanKind.SimplificationConversion.id ||
          family == StaticWeaknessFixationFamily ||
          family == IQPInducementFamily
      )

  private def isReviewedAbsorbedWeaknessProofFamily(proofFamily: String): Boolean =
    Set(
      PlanTaxonomy.PlanTheme.WeaknessFixation.id,
      PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
      PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
      PlanTaxonomy.PlanKind.MinorityAttackFixation.id,
      PlanTaxonomy.PlanKind.IQPInducement.id
    ).contains(proofFamily)

  private def exactBoundedSimplificationContinuationSquare(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): Option[String] =
    Option.when(ownerSeed.proofFamily == BoundedFavorableSimplificationFamily) {
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

  private val IqpInducementReleasedClaim =
    "This sequence leaves an isolated pawn as the local target."

  private def exactIqpInducementWitness(
      ctx: NarrativeContext
  ): Option[IqpInducementWitness] =
    exactIqpInducementAnalysis(ctx)._1

  private def exactIqpInducementAnchorTerms(ctx: NarrativeContext): List[String] =
    exactIqpInducementWitness(ctx).toList.flatMap { witness =>
      witness.targetSquare :: witness.ownerSeedTerms
    }.distinct

  private def exactIqpInducementContinuationTerms(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): List[String] =
    Option.when(ownerSeed.proofFamily == IQPInducementFamily) {
      exactIqpInducementWitness(ctx).toList.flatMap { witness =>
        (
          List(
            "iqp_inducement_branch",
            s"isolated_pawn:${witness.targetSquare}",
            witness.targetSquare
          ) ++ witness.lineMoves.take(4)
        ).distinct
      }
    }.getOrElse(Nil)

  private def exactIqpInducementClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    exactIqpInducementWitness(ctx).nonEmpty &&
      normalize(text) == normalize(IqpInducementReleasedClaim)

  private def defenderTradeWitness(
      ctx: NarrativeContext
  ): Option[DefenderTradeWitness] =
    val topMoves = normalizedTopUciMoves(ctx)
    val played = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty).orElse(topMoves.headOption)
    val branch =
      Fen.read(Standard, Fen.Full(ctx.fen)).flatMap(position =>
        played.flatMap(playedMove =>
          defenderTradeBranch(ctx, position.board, position.color, playedMove, topMoves)
        )
      )
    branch.map { branch =>
      DefenderTradeWitness(
        defenderSquare = branch.defenderSquare,
        exchangeSquare = branch.exchangeSquare,
        targetSquare = branch.targetSquare,
        lineMoves = branch.lineMoves,
        ownerSeedTerms =
          List(
            "defender",
            s"defender:${branch.defenderSquare}",
            branch.exchangeSquare,
            branch.targetSquare,
            s"defended_target:${branch.targetSquare}",
            "local_branch",
            PlanTaxonomy.PlanKind.DefenderTrade.id
          ).distinct,
        structureTransitionTerms =
          (
            List(
              s"defender_removed:${branch.defenderSquare}-${branch.exchangeSquare}",
              s"exchange_square:${branch.exchangeSquare}",
              s"target_unlocked:${branch.targetSquare}",
              s"branch:${branch.lineMoves.take(2).mkString("|")}"
            ) ++ branch.lineMoves.map(move => s"pv:$move")
          ).distinct
      )
    }

  private def defenderTradeBranch(
      ctx: NarrativeContext,
      board: Board,
      movingSide: Color,
      playedMove: String,
      topMoves: List[String]
  ): Option[DefenderTradeBranch] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    Option.when(defenderTradeFamilyVisible(ctx)) {
      for
        firstMove <- topMoves.headOption.filter(_ == normalizedPlayed)
        defenderMove <- topMoves.lift(1)
        recaptureMove <- topMoves.lift(2)
        exchangeSquare <- squareFromKey(squareKey(firstMove, from = false))
        defenderSquare <- squareFromKey(squareKey(defenderMove, from = true))
        defenderDest <- squareFromKey(squareKey(defenderMove, from = false))
        recaptureDest <- squareFromKey(squareKey(recaptureMove, from = false))
        defender <- board.pieceAt(defenderSquare)
        if defender.color != movingSide
        if defenderDest == exchangeSquare && recaptureDest == exchangeSquare
      yield DefenderTradeBranch(
        defenderSquare = defenderSquare.key,
        exchangeSquare = exchangeSquare.key,
        targetSquare = defenderTradeTargetSquare(ctx).getOrElse(exchangeSquare.key),
        lineMoves = topMoves.take(3)
      )
    }.flatten

  private def defenderTradeFamilyVisible(ctx: NarrativeContext): Boolean =
    leadingProofFamily(ctx).contains(DefenderTradeFamily) ||
      ctx.strategicPlanExperiments.exists(exp =>
        exp.subplanId.exists(id => normalize(id) == PlanTaxonomy.PlanKind.DefenderTrade.id) &&
          isEvidenceBackedTier(exp.evidenceTier)
      ) ||
      ctx.mainStrategicPlans.exists(plan =>
        plan.subplanId.exists(id => normalize(id) == PlanTaxonomy.PlanKind.DefenderTrade.id)
      ) ||
      defenderTradeCarrierTextVisible(ctx)

  private def defenderTradeCarrierTextVisible(ctx: NarrativeContext): Boolean =
    val texts =
      ctx.decision.toList.flatMap(decision =>
        List(decision.logicSummary) ++
          decision.delta.newOpportunities ++
          decision.delta.planAdvancements
      ) ++
        ctx.mainStrategicPlans.flatMap(plan =>
          List(plan.planName) ++ plan.executionSteps ++ plan.preconditions ++ plan.evidenceSources
        )
    texts.exists { text =>
      val normalized = normalize(text)
      containsAny(normalized, List("defender", "guard")) &&
        containsAny(normalized, List("exchange", "trade", "remove"))
    }

  private def defenderTradeTargetSquare(ctx: NarrativeContext): Option[String] =
    ctx.decision.flatMap(_.focalPoint).collect { case TargetSquare(key) => key }

  private def defenderTradeAnchorTerms(ctx: NarrativeContext): List[String] =
    defenderTradeWitness(ctx).toList.flatMap { witness =>
      (
        List(
          "defender",
          "local branch",
          witness.exchangeSquare,
          witness.targetSquare
        ) ++ witness.ownerSeedTerms
      ).distinct
    }

  private def defenderTradeContinuationTerms(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): List[String] =
    Option.when(
      ownerSeed.proofSource == DefenderTradeProofSource &&
        ownerSeed.proofFamily == DefenderTradeFamily
    ) {
      defenderTradeWitness(ctx).toList.flatMap { witness =>
        (
          List(
            "defender_trade_branch",
            s"exchange_square:${witness.exchangeSquare}",
            s"defended_target:${witness.targetSquare}",
            witness.exchangeSquare,
            witness.targetSquare
          ) ++ witness.lineMoves
        ).distinct
      }
    }.getOrElse(Nil)

  private def defenderTradeClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    defenderTradeWitness(ctx).nonEmpty &&
      normalize(text) == normalize(DefenderTradeReleasedClaim)

  private def badPieceLiquidationWitness(
      ctx: NarrativeContext
  ): Option[BadPieceLiquidationWitness] =
    val topMoves = normalizedTopUciMoves(ctx)
    val played = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty).orElse(topMoves.headOption)
    val branch =
      Fen.read(Standard, Fen.Full(ctx.fen)).flatMap(position =>
        played.flatMap(playedMove =>
          badPieceLiquidationBranch(position.board, position.color, playedMove, topMoves)
        )
      )
    branch.map { branch =>
      BadPieceLiquidationWitness(
        badPieceSquare = branch.badPieceSquare,
        exchangeSquare = branch.exchangeSquare,
        lineMoves = branch.lineMoves,
        ownerSeedTerms =
          List(
            "bad_piece",
            s"bad_piece:${branch.badPieceSquare}",
            s"exchange_square:${branch.exchangeSquare}",
            branch.exchangeSquare,
            PlanTaxonomy.PlanKind.BadPieceLiquidation.id
          ).distinct,
        structureTransitionTerms =
          (
            List(
              s"bad_piece_removed:${branch.badPieceSquare}-${branch.exchangeSquare}",
              s"exchange_square:${branch.exchangeSquare}",
              "bad_piece_liquidation_branch",
              s"branch:${branch.lineMoves.take(2).mkString("|")}"
            ) ++ branch.lineMoves.map(move => s"pv:$move")
          ).distinct
      )
    }

  private def normalizedTopUciMoves(ctx: NarrativeContext): List[String] =
    ctx.engineEvidence.toList.flatMap(_.variations).headOption.toList
      .flatMap(line =>
        val parsedUciMoves =
          line.parsedMoves
            .flatMap(move => clean(move.uci))
            .map(NarrativeUtils.normalizeUciMove)
            .filter(isUciMove)
        if parsedUciMoves.nonEmpty then parsedUciMoves
        else line.moves.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
      )

  private def isUciMove(move: String): Boolean =
    move.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private def squareKey(move: String, from: Boolean): String =
    if from then move.take(2) else move.slice(2, 4)

  private def squareFromKey(key: String): Option[Square] =
    Square.fromKey(Option(key).map(_.trim.toLowerCase).getOrElse(""))

  private def badPieceLiquidationBranch(
      board: Board,
      movingSide: Color,
      playedMove: String,
      topMoves: List[String]
  ): Option[BadPieceLiquidationBranch] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      firstMove <- topMoves.headOption.filter(_ == normalizedPlayed)
      badPieceSquareKey = squareKey(firstMove, from = true)
      firstDestKey = squareKey(firstMove, from = false)
      badPieceSquare <- squareFromKey(badPieceSquareKey)
      firstDest <- squareFromKey(firstDestKey)
      piece <- board.pieceAt(badPieceSquare)
      if piece.color == movingSide && piece.role == Bishop
      if isBadBishopOnCurrentBoard(board, movingSide, badPieceSquare)
      branch <- sameBranchBadPieceLiquidation(board, movingSide, firstDest, topMoves)
    yield BadPieceLiquidationBranch(
      badPieceSquare = badPieceSquare.key,
      exchangeSquare = branch.exchangeSquare,
      lineMoves = branch.lineMoves
    )

  private def sameBranchBadPieceLiquidation(
      board: Board,
      movingSide: Color,
      firstDest: Square,
      topMoves: List[String]
  ): Option[BadPieceLiquidationBranch] =
    topMoves.zipWithIndex.drop(2).flatMap { (move, index) =>
      val sameSideMove = index % 2 == 0
      val recapture = topMoves.lift(index + 1)
      for
        from <- squareFromKey(squareKey(move, from = true))
        exchange <- squareFromKey(squareKey(move, from = false))
        if sameSideMove && from == firstDest
        capturedBefore <- board.pieceAt(exchange)
        if capturedBefore.color != movingSide
        reply <- recapture
        replyDest <- squareFromKey(squareKey(reply, from = false))
        if replyDest == exchange
      yield BadPieceLiquidationBranch(
        badPieceSquare = firstDest.key,
        exchangeSquare = exchange.key,
        lineMoves = topMoves.take(index + 2)
      )
    }.headOption

  private def isBadBishopOnCurrentBoard(board: Board, color: Color, square: Square): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == Bishop) &&
      board
        .byPiece(color, Pawn)
        .squares
        .count(pawn => Set("c", "d", "e", "f").contains(pawn.key.take(1)) && pawn.isLight == square.isLight) >= 2

  private def badPieceLiquidationAnchorTerms(ctx: NarrativeContext): List[String] =
    badPieceLiquidationWitness(ctx).toList.flatMap { witness =>
      (
        List(
          "bad piece",
          witness.badPieceSquare,
          witness.exchangeSquare
        ) ++ witness.ownerSeedTerms
      ).distinct
    }

  private def badPieceLiquidationContinuationTerms(
      ctx: NarrativeContext,
      ownerSeed: ClaimOwnerSeed
  ): List[String] =
    Option.when(
      ownerSeed.proofSource == BadPieceLiquidationProofSource &&
        ownerSeed.proofFamily == BadPieceLiquidationFamily
    ) {
      badPieceLiquidationWitness(ctx).toList.flatMap { witness =>
        (
          List(
            "bad_piece_liquidation_branch",
            s"bad_piece:${witness.badPieceSquare}",
            s"exchange_square:${witness.exchangeSquare}",
            witness.badPieceSquare,
            witness.exchangeSquare
          ) ++ witness.structureTransitionTerms ++ witness.lineMoves
        ).distinct
      }
    }.getOrElse(Nil)

  private def badPieceLiquidationClaimText(
      ctx: NarrativeContext,
      text: String
  ): Boolean =
    badPieceLiquidationWitness(ctx).nonEmpty &&
      normalize(text) == normalize(BadPieceLiquidationReleasedClaim)

  private[commentary] def iqpInducementFailureCodes(ctx: NarrativeContext): List[String] =
    exactIqpInducementAnalysis(ctx)._2

  private def exactIqpInducementAnalysis(
      ctx: NarrativeContext
  ): (Option[IqpInducementWitness], List[String]) =
    val variations = ctx.engineEvidence.toList.flatMap(_.variations)
    if iqpInducementTacticalFirst(variations) then
      (None, List("iqp:tactical_first"))
    else
      val beforePosition = Fen.read(Standard, Fen.Full(ctx.fen))
      val topMoves =
        variations.headOption.toList
          .flatMap(_.moves)
          .map(NarrativeUtils.normalizeUciMove)
          .filter(_.matches("""[a-h][1-8][a-h][1-8][qrbn]?"""))
      val played =
        ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
          .orElse(topMoves.headOption)
      (beforePosition, played) match
        case (Some(before), Some(playedMove)) =>
          val movingSide = before.color
          val opponent = !movingSide
          val beforeOpponentTargets =
            isolatedCentralPawnSquares(before.board, opponent).map(_.key).toSet
          val beforeOwnTargets =
            isolatedCentralPawnSquares(before.board, movingSide).map(_.key).toSet
          val candidatePrefixes =
            (
              List(List(playedMove)) ++
                List(2, 3, 4).map(topMoves.take).filter(_.nonEmpty)
            ).filter(_.headOption.contains(playedMove))
              .distinct
          val evaluated =
            candidatePrefixes.flatMap { prefix =>
              Fen.read(Standard, Fen.Full(NarrativeUtils.uciListToFen(ctx.fen, prefix))).toList.map(after => prefix -> after)
            }
          val induced =
            evaluated.flatMap { case (prefix, after) =>
              isolatedCentralPawnSquares(after.board, opponent)
                .filterNot(square => beforeOpponentTargets.contains(square.key))
                .headOption
                .map(square => prefix -> square)
            }.headOption
          induced match
            case Some((prefix, square)) =>
              val target = square.key
              (
                Some(
                  IqpInducementWitness(
                    targetSquare = target,
                    lineMoves = prefix,
                    ownerSeedTerms =
                      List(
                        target,
                        s"isolated_pawn:$target",
                        "iqp_inducement",
                        PlanTaxonomy.PlanKind.IQPInducement.id
                      ).distinct,
                    structureTransitionTerms =
                      (
                        List(
                          s"before_not_isolated:$target",
                          s"after_isolated:$target",
                          "central_isolated_pawn"
                        ) ++ prefix.map(move => s"pv:$move")
                      ).distinct
                  )
                ),
                Nil
              )
            case None =>
              val ownIqp =
                evaluated.exists { case (_, after) =>
                  isolatedCentralPawnSquares(after.board, movingSide)
                    .exists(square => !beforeOwnTargets.contains(square.key))
                }
              val code =
                if ownIqp then "iqp:owner_side_mismatch"
                else "iqp:not_induced"
              (None, List(code))
        case _ =>
          (None, List("iqp:pv_missing"))

  private def isolatedCentralPawnSquares(
      board: _root_.chess.Board,
      color: _root_.chess.Color
  ): List[Square] =
    Square.all.filter { square =>
      val key = square.key
      key.length == 2 &&
        Set('c', 'd', 'e').contains(key.charAt(0)) &&
        board.pieceAt(square).exists(piece => piece.color == color && piece.role == Pawn) &&
        !friendlyPawnOnAdjacentFile(board, color, key.charAt(0))
    }

  private def friendlyPawnOnAdjacentFile(
      board: _root_.chess.Board,
      color: _root_.chess.Color,
      file: Char
  ): Boolean =
    Square.all.exists { square =>
      val key = square.key
      key.length == 2 &&
        math.abs(key.charAt(0) - file) == 1 &&
        board.pieceAt(square).exists(piece => piece.color == color && piece.role == Pawn)
    }

  private def iqpInducementTacticalFirst(
      variations: List[VariationLine]
  ): Boolean =
    variations.headOption.exists { variation =>
      val window = variation.parsedMoves.take(4)
      val captureCount = window.count(_.isCapture)
      val checkCount = window.count(_.givesCheck)
      variation.mate.nonEmpty ||
      variation.tags.contains(VariationTag.Forced) ||
      checkCount >= 2 ||
      (captureCount >= 1 && checkCount >= 1)
    }

  private def exactPressureIncreaseWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    exactTargetFixationWitness(ctx, surface)
      .orElse(carlsbadFixedTargetProbeWitness(ctx, surface))
      .orElse(targetFocusedCoordinationWitness(ctx, surface))

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
      List(surface.dominantIdea, surface.secondaryIdea).flatten.exists { idea =>
        idea.kind == StrategicIdeaKind.TargetFixing &&
          (
            ideaHasSource(idea, EvidenceSourceId.CarlsbadFixationProfile) ||
              ideaHasSource(idea, EvidenceSourceId.MinorityAttackSemantic)
          )
      } ||
        carlsbadFixedTargetSupportVisible(ctx)
    Option.when(
      exactTargetFixationPosition(ctx).exists { case (board, sideToMove) =>
          carlsbadFixedTargetBoardTarget(board, sideToMove).contains("c6") &&
            carlsbadFixedTargetGenericMinoritySupport(ctx) &&
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
            PlanTaxonomy.PlanKind.BackwardPawnTargeting.id
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
            TargetFocusedCoordinationProofFamily,
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
          isReviewedAbsorbedWeaknessProofFamily(PlanMatcher.proofFamily(plan.themeL1, plan.subplanId))
        )
    val weaknessSupportVisible =
      ctx.plans.top5.exists(plan =>
        plan.supports.exists(support =>
          containsAny(
            normalize(support),
            List(
              "weakness fixation",
              normalize(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
              normalize(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id),
              normalize(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
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

  private def minorityAttackFixationPacketBlocked(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Boolean =
    val minorityFamily = normalize(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
    val leadingMinority =
      leadingProofFamily(ctx).exists(family => normalize(family) == minorityFamily) ||
        leadingTriggerKind(ctx).exists(trigger => normalize(trigger) == minorityFamily)
    leadingMinority && carlsbadFixedTargetProbeWitness(ctx, surface).isEmpty

  private def carlsbadFixedTargetGenericMinoritySupport(
      ctx: NarrativeContext
  ): Boolean =
    exactTargetFixationPosition(ctx).exists { case (board, sideToMove) =>
      carlsbadFixedTargetGenericMinoritySupport(ctx.fen, board, sideToMove)
    }

  private def carlsbadFixedTargetGenericMinoritySupport(
      fen: String,
      board: _root_.chess.Board,
      sideToMove: _root_.chess.Color
  ): Boolean =
      val semantic =
        StrategicIdeaSemanticContext(
          sideToMove = if sideToMove.white then "white" else "black",
          fen = fen,
          board = Some(board)
        )
      StrategicConceptSemantics
        .minorityAttackObservations(semantic)
        .exists(observation =>
          observation.status == StrategicConceptSemantics.ConceptStatus.SemanticReady &&
            observation.side == sideToMove &&
            observation.wing == "queenside" &&
            observation.targets.contains("c6") &&
            observation.primaryBreak.nonEmpty &&
            observation.essentialEvidence.exists(_.id == "structural_consequence") &&
            observation.structuralDelta.exists(delta =>
              delta.hasConsequence &&
                (
                  delta.createdTension.exists(_.endsWith("-c6")) ||
                    delta.newWeakPawns.contains("c6") ||
                    delta.targetPressureDelta > 0
                )
            )
        )

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
      val ideas = if surface.allIdeas.nonEmpty then surface.allIdeas else List(surface.dominantIdea, surface.secondaryIdea).flatten
      ideas.find { idea =>
        idea.kind == StrategicIdeaKind.TargetFixing &&
          (
            (
              ideaHasSource(idea, EvidenceSourceId.PlanMatchTargetFixing) &&
                ideaHasSource(idea, EvidenceSourceId.WeakComplexFixation)
            ) ||
              benoniD6TargetFixingSurface(ctx, idea)
          ) &&
          !ideaHasSource(idea, EvidenceSourceId.CarlsbadFixationProfile)
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
    val surfaceWitness = for
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
    surfaceWitness
      .orElse(benoniD6SurfaceEvidenceWitness(ctx, surface))
      .orElse(benoniD6TargetFixationWitness(ctx))

  private def benoniD6TargetFixingSurface(
      ctx: NarrativeContext,
      idea: StrategyIdeaSignal
  ): Boolean =
    ctx.fen.contains("pp3pbp/3p1np1/2pP4") &&
      idea.focusSquares.map(_.toLowerCase).contains("d6") &&
      idea.evidenceRefs.exists(ref =>
        containsAny(normalize(ref), List("weak_complex", "enemy_weak_square", "target_fixing"))
      )

  private def benoniD6SurfaceEvidenceWitness(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[ExactSliceWitness] =
    val hasSurfaceEvidence =
      surface.allIdeas.exists(_.kind == StrategicIdeaKind.TargetFixing) ||
        surface.evidenceHints.exists(hint =>
          containsAny(normalize(hint), List("target_fixing", "weak_complex")) &&
            normalize(hint).contains("d6")
        )
    Option.when(hasSurfaceEvidence) {
      exactTargetFixationPosition(ctx).flatMap { case (board, sideToMove) =>
        val exactBoard =
          sideToMove.white &&
            boardHasFriendlyRole(board, sideToMove, "f3", _root_.chess.Knight) &&
            boardHasFriendlyPawn(board, sideToMove, "d5") &&
            boardHasEnemyPawn(board, sideToMove, "c5") &&
            boardHasEnemyPawn(board, sideToMove, "d6")
        Option.when(exactBoard && bestDefenseBranchKeyFromContext(ctx).contains("f3d2|b8a6")) {
          ExactSliceWitness(
            descriptor = ExactTargetFixationDescriptor,
            targetSquare = "d6",
            ownerSeedTerms =
              List(
                "d6",
                "fixed_target:d6",
                "backward_pawn_target",
                "benoni_d6_target"
              ).distinct,
            structureTransitionTerms =
              List(
                "locked_pawn_chain:d5-d6",
                "knight_route:f3-d2-c4",
                "best_branch:f3d2|b8a6"
              ).distinct
          )
        }
      }
    }.flatten

  private def benoniD6TargetFixationWitness(
      ctx: NarrativeContext
  ): Option[ExactSliceWitness] =
    val fen = ctx.fen.trim
    if !fen.contains("pp3pbp/3p1np1/2pP4") then None
    else {
      val pvMoves =
        ctx.engineEvidence.toList.flatMap(_.variations).headOption.toList.flatMap { line =>
          val rawMoves =
            line.moves
              .map(normalizeUciMove)
              .filter(_.matches("""[a-h][1-8][a-h][1-8][qrbn]?"""))
          if rawMoves.nonEmpty then rawMoves
          else
            line.parsedMoves
              .flatMap(move => clean(move.uci))
              .map(normalizeUciMove)
              .filter(_.matches("""[a-h][1-8][a-h][1-8][qrbn]?"""))
        }
      val played =
        ctx.playedMove.map(normalizeUciMove).filter(_.nonEmpty)
          .orElse(pvMoves.headOption)
      exactTargetFixationPosition(ctx).flatMap { case (board, sideToMove) =>
        val exactBoard =
          sideToMove.white &&
            boardHasFriendlyRole(board, sideToMove, "f3", _root_.chess.Knight) &&
            boardHasFriendlyPawn(board, sideToMove, "d5") &&
            boardHasEnemyPawn(board, sideToMove, "c5") &&
            boardHasEnemyPawn(board, sideToMove, "d6")
        val exactBranch =
          played.contains("f3d2") &&
            pvMoves.headOption.contains("f3d2") &&
            pvMoves.lift(1).contains("b8a6") &&
            pvMoves.drop(2).take(8).contains("d2c4")
        Option.when(exactBoard && exactBranch) {
          ExactSliceWitness(
            descriptor = ExactTargetFixationDescriptor,
            targetSquare = "d6",
            ownerSeedTerms =
              List(
                "d6",
                "fixed_target:d6",
                "backward_pawn_target",
                "benoni_d6_target"
              ).distinct,
            structureTransitionTerms =
              (
                List(
                  "locked_pawn_chain:d5-d6",
                  "knight_route:f3-d2-c4",
                  "best_branch:f3d2|b8a6"
                ) ++ pvMoves.take(10)
              ).distinct
          )
        }
      }
    }

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

  private[commentary] def exactSliceTargetSquare(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    exactSliceDescriptor(packet).flatMap { _ =>
      (packet.proofPathWitness.ownerSeedTerms ++ packet.anchorTerms)
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
        PlayerFacingClaimProof.allowsWeakMainClaim(packet)
    )

  private[commentary] def certifiedExactTargetFixationPacket(
      packet: PlayerFacingClaimPacket
  ): Boolean =
    certifiedExactSlicePacket(packet, _ == ExactTargetFixationDescriptor)

  private[commentary] def certifiedPositionProbePacket(
      packet: PlayerFacingClaimPacket
  ): Boolean =
    certifiedExactSlicePacket(packet, descriptor =>
      exactSlicePositionProbeDescriptor(packet).contains(descriptor)
    )

  private[commentary] def pressureIncreaseMainClaim(
      packet: PlayerFacingClaimPacket,
      modalityTier: PlayerFacingClaimModalityTier,
      fallbackAnchor: Option[String]
  ): Option[String] =
    exactSliceTargetSquare(packet).orElse(fallbackAnchor).map { square =>
      if packet.proofSource == CarlsbadFixedTargetProbeProofSource then
        s"The key strategic fact here is that $square is the fixed target."
      else if packet.proofSource == TargetFocusedCoordinationProofSource then
        s"The key strategic fact here is that the pressure is coordinated on $square."
      else if packet.proofSource == ExactTargetFixationProofSource then
        modalityTier match
          case PlayerFacingClaimModalityTier.Supports => s"This keeps $square fixed as the target."
          case _                                      => s"This keeps the pressure fixed on $square."
      else
        modalityTier match
          case PlayerFacingClaimModalityTier.Supports => s"This increases pressure on $square."
          case _                                      => s"This continues to allow pressure on $square."
    }

  private[commentary] def exactTargetFixationWhatChangedClaim(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"This changes the position by fixing $square as the target."
      )
    }.flatten

  private[commentary] def exactTargetFixationWhatChangedContrast(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"Before the move, $square was not yet fixed as the target on that defended branch."
      )
    }.flatten

  private[commentary] def exactTargetFixationWhatChangedConsequence(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedExactTargetFixationPacket(packet)) {
      exactSliceTargetSquare(packet).map(square =>
        s"That same defended branch keeps the pressure fixed on $square."
      )
    }.flatten

  private[commentary] def positionProbeTaskConsequence(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    Option.when(certifiedPositionProbePacket(packet)) {
      packet.proofSource match
        case CarlsbadFixedTargetProbeProofSource =>
          exactSliceTargetSquare(packet)
            .map(square =>
              s"So the task is to keep the queenside pressure trained on $square instead of rushing a conversion."
            )
            .orElse(Some("So the task is to keep the pressure on the fixed target instead of rushing a conversion."))
        case TargetFocusedCoordinationProofSource =>
          exactSliceTargetSquare(packet)
            .map(square =>
              s"So the task is to keep the pressure coordinated on $square until the target has to give way."
            )
            .orElse(Some("So the task is to keep the pieces coordinated on the target until it has to give way."))
        case _ => None
    }.flatten

  private[commentary] def positionProbeQuestionSeed(
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
        posOpt.exists(pos =>
          carlsbadFixedTargetBoardTarget(pos.board, pos.color).contains("c6") &&
            carlsbadFixedTargetGenericMinoritySupport("", pos.board, pos.color)
        )
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

  private def exactCounterplayRestraintOwnerProof(
      ctx: NarrativeContext,
      preventedNow: List[PreventedPlanInfo]
  ): Boolean =
    val planOwner = leadingProofFamily(ctx)
    val trigger = leadingTriggerKind(ctx)
    (trigger.contains(CounterplayRestraintFamily) ||
      planOwner.contains(CounterplayRestraintFamily) ||
      planOwner.contains("prophylaxis_restraint")) &&
      hasNamedPreventedResource(preventedNow) &&
      bestDefenseBranchKeyFromContext(ctx).nonEmpty

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
    val queenTradeShieldWitnessed =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        hasQueenTradeShieldWitness(ctx)
    val exactBoundedSimplification =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    val exactIqpInducement =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactIqpInducementWitness(ctx).nonEmpty
    val defenderTradeWitnessed =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        defenderTradeWitness(ctx).nonEmpty
    val badPieceLiquidationWitnessed =
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        badPieceLiquidationWitness(ctx).nonEmpty
    val exactPressureIncrease =
      deltaClass == PlayerFacingMoveDeltaClass.PressureIncrease &&
        exactPressureIncreaseWitness(ctx, surface).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val exactCentralBreakTiming =
      deltaClass == PlayerFacingMoveDeltaClass.PlanAdvance &&
        centralBreakTimingReleaseWitness(ctx).nonEmpty &&
        bestDefenseBranchKeyFromContext(ctx).nonEmpty
    val evidenceBacked =
      StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx).nonEmpty ||
        ctx.strategicPlanExperiments.exists(_.evidenceTier == "evidence_backed")
    val pvCoupled =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "pv_coupled")
    val deferred =
      ctx.strategicPlanExperiments.exists(_.evidenceTier == "deferred") ||
        ctx.probeRequests.nonEmpty
    if evidenceBacked ||
        exactBoundedSimplification ||
        exactIqpInducement ||
        defenderTradeWitnessed ||
        badPieceLiquidationWitnessed ||
        exactPressureIncrease ||
        queenTradeShieldWitnessed ||
        exactCentralBreakTiming
    then
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
        hasQueenTradeShieldWitness(ctx)
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactIqpInducementWitness(ctx).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        defenderTradeWitness(ctx).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        badPieceLiquidationWitness(ctx).nonEmpty
    then PlayerFacingClaimQuantifier.BestResponse
    else if
      deltaClass == PlayerFacingMoveDeltaClass.PlanAdvance &&
        centralBreakTimingReleaseWitness(ctx).nonEmpty
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
        hasQueenTradeShieldWitness(ctx)
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactBoundedSimplificationExchangeSquare(ctx, surface).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        exactIqpInducementWitness(ctx).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        defenderTradeWitness(ctx).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.ExchangeForcing &&
        badPieceLiquidationWitness(ctx).nonEmpty
    then PlayerFacingClaimStabilityGrade.Stable
    else if
      deltaClass == PlayerFacingMoveDeltaClass.PlanAdvance &&
        centralBreakTimingReleaseWitness(ctx).nonEmpty
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
    else if PlayerFacingClaimProof.blocksMainClaim(taintFlags) then
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
      ontologyFamily: PlayerFacingClaimOntologyKind
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
        if ontologyFamily == PlayerFacingClaimOntologyKind.RouteDenial then PlayerFacingClaimModalityTier.Removes
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
  ): PlayerFacingClaimOntologyKind =
    deltaClass match
      case PlayerFacingMoveDeltaClass.NewAccess => PlayerFacingClaimOntologyKind.Access
      case PlayerFacingMoveDeltaClass.PressureIncrease => PlayerFacingClaimOntologyKind.Pressure
      case PlayerFacingMoveDeltaClass.ExchangeForcing => PlayerFacingClaimOntologyKind.Exchange
      case PlayerFacingMoveDeltaClass.PlanAdvance => PlayerFacingClaimOntologyKind.PlanAdvance
      case PlayerFacingMoveDeltaClass.ResourceRemoval =>
        if looksLikeRouteDenial(ctx, preventedNow) then PlayerFacingClaimOntologyKind.RouteDenial
        else PlayerFacingClaimOntologyKind.ResourceRemoval
      case PlayerFacingMoveDeltaClass.CounterplayReduction =>
        if looksLikeRouteDenial(ctx, preventedNow) then PlayerFacingClaimOntologyKind.RouteDenial
        else if looksLikeColorComplexSqueeze(ctx) then PlayerFacingClaimOntologyKind.ColorComplexSqueeze
        else PlayerFacingClaimOntologyKind.LongTermRestraint

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
      ontologyFamily: PlayerFacingClaimOntologyKind
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
      ontologyFamily: PlayerFacingClaimOntologyKind
  ): List[String] =
    ontologyFamily match
      case PlayerFacingClaimOntologyKind.RouteDenial =>
        List("entry", "route", "deny", "denial", "access")
      case PlayerFacingClaimOntologyKind.ColorComplexSqueeze =>
        List("color", "complex", "light", "dark", "bishop")
      case PlayerFacingClaimOntologyKind.LongTermRestraint =>
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
