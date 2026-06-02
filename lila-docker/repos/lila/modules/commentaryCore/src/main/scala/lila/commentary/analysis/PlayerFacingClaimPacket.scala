package lila.commentary.analysis


import chess.format.Uci
import lila.commentary.analysis.claim.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
private[commentary] enum PlayerFacingPacketScope:
  case MoveLocal
  case PositionLocal
  case LineScoped
  case BackendOnly

private[commentary] enum PlayerFacingSameBranchState:
  case Proven
  case Missing
  case Ambiguous

private[commentary] enum PlayerFacingClaimPersistence:
  case Stable
  case BestDefenseOnly
  case FutureOnly
  case Broken

private[commentary] enum PlayerFacingClaimFallbackMode:
  case Suppress
  case LineOnly
  case WeakMain
  case ExactFactual

private[commentary] object PlayerFacingClaimSuppressionReason:
  val AlternativeDominance = "alternative_dominance"
  val RivalStoryAlive = "rival_story_alive"
  val SameBranchMissing = "same_branch_missing"
  val SameBranchAmbiguous = "same_branch_ambiguous"
  val ScopeInflation = "scope_inflation"
  val SupportOnlyReinflation = "support_only_reinflation"
  val SameJobConversion = "same_job_conversion"
  val TradeKeyDefenderRelabel = "trade_key_defender_relabel"
  val RouteBindRelabel = "route_bind_relabel"
  val BetterEndgameInflation = "better_endgame_inflation"
  val B7Drift = "B7_drift"

private[commentary] object PlayerFacingClaimReleaseRisk:
  val MoveOrderFragility = "move_order_fragility"
  val HeavyPieceLeakage = "heavy_piece_leakage"
  val SurfaceReinflation = "surface_reinflation"
  val RouteMirage = "route_mirage"
  val RivalRelease = "rival_release"

private[commentary] sealed trait PlayerFacingExactSliceProof

private[commentary] object PlayerFacingExactSliceProof:
  final case class ExactTargetFixation(targetSquare: String) extends PlayerFacingExactSliceProof
  final case class CarlsbadFixedTarget(targetSquare: String, minoritySupport: Boolean) extends PlayerFacingExactSliceProof
  final case class TargetFocusedCoordination(
      targetSquare: String,
      supportFromSquares: List[String],
      targetPieces: List[String]
  ) extends PlayerFacingExactSliceProof
  final case class ColorComplexSqueeze(
      targetSquare: String,
      squareColor: String,
      minorPieceRole: String,
      minorPieceSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class LocalFileEntryBind(file: String, entrySquare: String) extends PlayerFacingExactSliceProof
  final case class CounterplayAxisSuppression(breakToken: String) extends PlayerFacingExactSliceProof
  final case class ProphylacticRestraint(resourceToken: String) extends PlayerFacingExactSliceProof
  final case class QueenTradeShield(lineMoves: List[String]) extends PlayerFacingExactSliceProof
  final case class CentralBreakTiming(
      breakMove: String,
      breakSquare: String,
      breakToken: String
  ) extends PlayerFacingExactSliceProof
  final case class DefenderTrade(
      defenderSquare: String,
      exchangeSquare: String,
      targetSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class BadPieceLiquidation(
      badPieceSquare: String,
      exchangeSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class Overload(
      defenderSquare: String,
      targetSquares: List[String],
      attackerSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class Deflection(
      defenderSquare: String,
      targetSquare: String,
      attackerSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class DiscoveredAttack(
      attackerSquare: String,
      clearedSquare: String,
      targetSquare: String,
      attackerRole: String
  ) extends PlayerFacingExactSliceProof
  final case class DoubleCheck(
      kingSquare: String,
      checkerSquares: List[String],
      moverSquare: String,
      moverRole: String
  ) extends PlayerFacingExactSliceProof
  final case class BackRankMate(
      kingSquare: String,
      checkerSquares: List[String],
      matingMove: String
  ) extends PlayerFacingExactSliceProof
  final case class MateNet(
      kingSquare: String,
      checkerSquares: List[String],
      matingMove: String,
      patternId: Option[String]
  ) extends PlayerFacingExactSliceProof
  final case class GreekGift(
      bishopSquare: String,
      targetSquare: String,
      entryMove: String,
      patternId: String
  ) extends PlayerFacingExactSliceProof
  final case class TargetPiece(square: String, role: String)
  final case class Fork(
      attackerSquare: String,
      attackerRole: String,
      targets: List[TargetPiece]
  ) extends PlayerFacingExactSliceProof
  final case class HangingPiece(
      attackerSquare: String,
      targetSquare: String,
      attackerRole: String,
      targetRole: String
  ) extends PlayerFacingExactSliceProof
  final case class TrappedPiece(
      targetSquare: String,
      targetRole: String,
      attackerSquares: List[String],
      legalEscapeCount: Int
  ) extends PlayerFacingExactSliceProof
  final case class Domination(
      controllerSquare: String,
      targetSquare: String,
      controllerRole: String,
      targetRole: String,
      legalMoveCount: Int
  ) extends PlayerFacingExactSliceProof
  final case class StalemateTrap(
      kingSquare: String,
      trappingMove: String
  ) extends PlayerFacingExactSliceProof
  final case class PerpetualCheck(
      kingSquare: String,
      checkingMoves: List[String],
      cycleMoves: List[String],
      repeatedPositionPly: Int
  ) extends PlayerFacingExactSliceProof
  final case class Zwischenzug(
      intermediateMove: String,
      threatType: String,
      responseMove: String,
      payoffMove: String,
      targetSquare: String
  ) extends PlayerFacingExactSliceProof
  final case class Decoy(
      baitFromSquare: String,
      baitSquare: String,
      luredFromSquare: String,
      executionFromSquare: String,
      executionToSquare: String,
      baitRole: String,
      luredRole: String
  ) extends PlayerFacingExactSliceProof
  final case class XRay(
      attackerSquare: String,
      blockerSquare: String,
      targetSquare: String,
      attackerRole: String,
      blockerRole: String,
      targetRole: String
  ) extends PlayerFacingExactSliceProof
  final case class Clearance(
      beneficiarySquare: String,
      clearedSquare: String,
      targetSquare: String,
      beneficiaryRole: String,
      clearingTo: String
  ) extends PlayerFacingExactSliceProof
  final case class Battery(
      frontSquare: String,
      backSquare: String,
      targetSquare: String,
      frontRole: String,
      backRole: String,
      axis: String
  ) extends PlayerFacingExactSliceProof
  final case class Pin(
      attackerSquare: String,
      pinnedSquare: String,
      behindSquare: String,
      targetSquare: String,
      attackerRole: String,
      pinnedRole: String,
      behindRole: String,
      absolute: Boolean
  ) extends PlayerFacingExactSliceProof
  final case class Skewer(
      attackerSquare: String,
      frontSquare: String,
      backSquare: String,
      targetSquare: String,
      attackerRole: String,
      frontRole: String,
      backRole: String
  ) extends PlayerFacingExactSliceProof
  final case class Interference(
      blockerSquare: String,
      defenderSquare: String,
      targetSquare: String,
      blockerRole: String,
      defenderRole: String,
      targetRole: String
  ) extends PlayerFacingExactSliceProof

private[commentary] object PlayerFacingExactSliceProofFacts:
  final case class Path(proofSource: String, proofFamily: String):
    def matches(packet: PlayerFacingClaimPacket): Boolean =
      matches(packet.proofSource, packet.proofFamily)

    def matches(source: String, family: String): Boolean =
      normalize(source) == normalize(proofSource) &&
        normalize(family) == normalize(proofFamily)

  def expectedPath(proof: PlayerFacingExactSliceProof): Path =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(_) =>
        Path(ProofSourceId.ExactTargetFixation.wireKey, proofFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation))
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(_, _) =>
        Path(ProofSourceId.CarlsbadFixedTargetProbe.wireKey, ProofFamilyId.BackwardPawnTargeting.wireKey)
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(_, _, _) =>
        Path(ProofSourceId.TargetFocusedCoordinationProbe.wireKey, ProofFamilyId.TargetFocusedCoordination.wireKey)
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(_, _, _, _) =>
        Path(ProofSourceId.ColorComplexSqueezeProbe.wireKey, ProofFamilyId.ColorComplexSqueeze.wireKey)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(_, _) =>
        Path(ProofSourceId.LocalFileEntryBind.wireKey, ProofFamilyId.HalfOpenFilePressure.wireKey)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(_) =>
        Path(ProofSourceId.CounterplayAxisSuppression.wireKey, ProofFamilyId.NeutralizeKeyBreak.wireKey)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(_) =>
        Path(ProofSourceId.ProphylacticMove.wireKey, ProofFamilyId.CounterplayRestraint.wireKey)
      case PlayerFacingExactSliceProof.QueenTradeShield(_) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.QueenTradeShield)
        Path(family, family)
      case PlayerFacingExactSliceProof.CentralBreakTiming(_, _, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.CentralBreakTiming)
        Path(family, family)
      case PlayerFacingExactSliceProof.DefenderTrade(_, _, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.DefenderTrade)
        Path(family, family)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(_, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.BadPieceLiquidation)
        Path(family, family)
      case _: PlayerFacingExactSliceProof.Overload |
          _: PlayerFacingExactSliceProof.Deflection |
          _: PlayerFacingExactSliceProof.DiscoveredAttack |
          _: PlayerFacingExactSliceProof.DoubleCheck |
          _: PlayerFacingExactSliceProof.BackRankMate |
          _: PlayerFacingExactSliceProof.MateNet |
          _: PlayerFacingExactSliceProof.GreekGift |
          _: PlayerFacingExactSliceProof.Fork |
          _: PlayerFacingExactSliceProof.HangingPiece |
          _: PlayerFacingExactSliceProof.TrappedPiece |
          _: PlayerFacingExactSliceProof.Domination |
          _: PlayerFacingExactSliceProof.StalemateTrap |
          _: PlayerFacingExactSliceProof.PerpetualCheck |
          _: PlayerFacingExactSliceProof.Zwischenzug |
          _: PlayerFacingExactSliceProof.Decoy |
          _: PlayerFacingExactSliceProof.XRay |
          _: PlayerFacingExactSliceProof.Clearance |
          _: PlayerFacingExactSliceProof.Battery |
          _: PlayerFacingExactSliceProof.Pin |
          _: PlayerFacingExactSliceProof.Skewer |
          _: PlayerFacingExactSliceProof.Interference =>
        Path(ProofSourceId.RelationTransformation.wireKey, ProofFamilyId.RelationTransformation.wireKey)

  def matchesPacket(
      packet: PlayerFacingClaimPacket,
      proof: PlayerFacingExactSliceProof
  ): Boolean =
    validShape(proof) && expectedPath(proof).matches(packet)

  def matchesPath(
      proof: PlayerFacingExactSliceProof,
      proofSource: String,
      proofFamily: String
  ): Boolean =
    validShape(proof) && expectedPath(proof).matches(proofSource, proofFamily)

  def validShape(proof: PlayerFacingExactSliceProof): Boolean =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        squareKey(targetSquare)
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, minoritySupport) =>
        Set("c6", "c3").contains(normalize(targetSquare)) &&
          minoritySupport
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, supportFromSquares, targetPieces) =>
        squareKey(targetSquare) &&
          supportFromSquares.map(normalize).filter(squareKey).distinct.size >= 2 &&
          targetPieces.exists(token => normalize(token).startsWith("target_"))
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, squareColor, minorPieceRole, minorPieceSquare) =>
        squareKey(targetSquare) &&
          Set("light", "dark").contains(normalize(squareColor)) &&
          Set("bishop", "knight").contains(normalize(minorPieceRole)) &&
          squareKey(minorPieceSquare)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        fileToken(file) &&
          squareKey(entrySquare)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) =>
        breakTokenShape(breakToken)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceTokenValue) =>
        resourceToken(resourceTokenValue)
      case PlayerFacingExactSliceProof.QueenTradeShield(lineMoves) =>
        lineMoves.size >= 2 &&
          lineMoves.forall(uciMove)
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        uciMove(breakMove) &&
          squareKey(breakSquare) &&
          uciDestination(breakMove).contains(normalize(breakSquare)) &&
          routeToken(breakToken)
      case PlayerFacingExactSliceProof.DefenderTrade(defenderSquare, exchangeSquare, targetSquare) =>
        squareKey(defenderSquare) &&
          squareKey(exchangeSquare) &&
          squareKey(targetSquare)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(badPieceSquare, exchangeSquare) =>
        squareKey(badPieceSquare) &&
          squareKey(exchangeSquare)
      case PlayerFacingExactSliceProof.Overload(defenderSquare, targetSquares, attackerSquare) =>
        squareKey(defenderSquare) &&
          targetSquares.map(normalize).filter(squareKey).distinct.size >= 2 &&
          squareKey(attackerSquare)
      case PlayerFacingExactSliceProof.Deflection(defenderSquare, targetSquare, attackerSquare) =>
        squareKey(defenderSquare) &&
          squareKey(targetSquare) &&
          squareKey(attackerSquare)
      case PlayerFacingExactSliceProof.DiscoveredAttack(attackerSquare, clearedSquare, targetSquare, attackerRole) =>
        squareKey(attackerSquare) &&
          squareKey(clearedSquare) &&
          squareKey(targetSquare) &&
          normalize(attackerRole).nonEmpty
      case PlayerFacingExactSliceProof.DoubleCheck(kingSquare, checkerSquares, moverSquare, moverRole) =>
        val normalizedCheckers = checkerSquares.map(normalize).filter(squareKey).distinct
        squareKey(kingSquare) &&
          normalizedCheckers.size >= 2 &&
          squareKey(moverSquare) &&
          normalizedCheckers.contains(normalize(moverSquare)) &&
          normalize(moverRole).nonEmpty
      case PlayerFacingExactSliceProof.BackRankMate(kingSquare, checkerSquares, matingMove) =>
        squareKey(kingSquare) &&
          checkerSquares.map(normalize).filter(squareKey).distinct.nonEmpty &&
          uciMove(matingMove)
      case PlayerFacingExactSliceProof.MateNet(kingSquare, checkerSquares, matingMove, patternId) =>
        squareKey(kingSquare) &&
          checkerSquares.map(normalize).filter(squareKey).distinct.nonEmpty &&
          uciMove(matingMove) &&
          patternId.forall(id => normalize(id).nonEmpty)
      case PlayerFacingExactSliceProof.GreekGift(bishopSquare, targetSquare, entryMove, patternId) =>
        squareKey(bishopSquare) &&
          squareKey(targetSquare) &&
          normalize(bishopSquare) == normalize(targetSquare) &&
          uciMove(entryMove) &&
          uciDestination(entryMove).contains(normalize(bishopSquare)) &&
          normalize(patternId).nonEmpty
      case PlayerFacingExactSliceProof.Fork(attackerSquare, attackerRole, targets) =>
        squareKey(attackerSquare) &&
          normalize(attackerRole).nonEmpty &&
          targets
            .filter(target => squareKey(target.square) && normalize(target.role).nonEmpty)
            .map(target => normalize(target.square))
            .distinct
            .size >= 2
      case PlayerFacingExactSliceProof.HangingPiece(attackerSquare, targetSquare, attackerRole, targetRole) =>
        squareKey(attackerSquare) &&
          squareKey(targetSquare) &&
          normalize(attackerRole).nonEmpty &&
          normalize(targetRole).nonEmpty
      case PlayerFacingExactSliceProof.TrappedPiece(targetSquare, targetRole, attackerSquares, legalEscapeCount) =>
        squareKey(targetSquare) &&
          normalize(targetRole).nonEmpty &&
          attackerSquares.map(normalize).filter(squareKey).distinct.nonEmpty &&
          legalEscapeCount == 0
      case PlayerFacingExactSliceProof.Domination(
            controllerSquare,
            targetSquare,
            controllerRole,
            targetRole,
            legalMoveCount
          ) =>
        squareKey(controllerSquare) &&
          squareKey(targetSquare) &&
          normalize(controllerRole).nonEmpty &&
          normalize(targetRole).nonEmpty &&
          legalMoveCount >= 0 &&
          legalMoveCount <= 1
      case PlayerFacingExactSliceProof.StalemateTrap(kingSquare, trappingMove) =>
        squareKey(kingSquare) &&
          uciMove(trappingMove)
      case PlayerFacingExactSliceProof.PerpetualCheck(kingSquare, checkingMoves, cycleMoves, repeatedPositionPly) =>
        squareKey(kingSquare) &&
          checkingMoves.size >= 3 &&
          checkingMoves.forall(uciMove) &&
          cycleMoves.size >= 4 &&
          cycleMoves.forall(uciMove) &&
          repeatedPositionPly > 0
      case PlayerFacingExactSliceProof.Zwischenzug(
            intermediateMove,
            threatType,
            responseMove,
            payoffMove,
            targetSquare
          ) =>
        uciMove(intermediateMove) &&
          normalize(threatType).nonEmpty &&
          uciMove(responseMove) &&
          uciMove(payoffMove) &&
          squareKey(targetSquare) &&
          uciDestination(payoffMove).contains(normalize(targetSquare))
      case PlayerFacingExactSliceProof.Decoy(
            baitFromSquare,
            baitSquare,
            luredFromSquare,
            executionFromSquare,
            executionToSquare,
            baitRole,
            luredRole
          ) =>
        squareKey(baitFromSquare) &&
          squareKey(baitSquare) &&
          squareKey(luredFromSquare) &&
          squareKey(executionFromSquare) &&
          squareKey(executionToSquare) &&
          normalize(executionToSquare) == normalize(baitSquare) &&
          normalize(baitRole).nonEmpty &&
          normalize(luredRole).nonEmpty
      case PlayerFacingExactSliceProof.XRay(
            attackerSquare,
            blockerSquare,
            targetSquare,
            attackerRole,
            blockerRole,
            targetRole
          ) =>
        squareKey(attackerSquare) &&
          squareKey(blockerSquare) &&
          squareKey(targetSquare) &&
          normalize(attackerRole).nonEmpty &&
          normalize(blockerRole).nonEmpty &&
          normalize(targetRole).nonEmpty
      case PlayerFacingExactSliceProof.Clearance(
            beneficiarySquare,
            clearedSquare,
            targetSquare,
            beneficiaryRole,
            clearingTo
          ) =>
        squareKey(beneficiarySquare) &&
          squareKey(clearedSquare) &&
          squareKey(targetSquare) &&
          squareKey(clearingTo) &&
          normalize(beneficiaryRole).nonEmpty
      case PlayerFacingExactSliceProof.Battery(frontSquare, backSquare, targetSquare, frontRole, backRole, axis) =>
        squareKey(frontSquare) &&
          squareKey(backSquare) &&
          squareKey(targetSquare) &&
          normalize(frontRole).nonEmpty &&
          normalize(backRole).nonEmpty &&
          Set("file", "rank", "diagonal").contains(normalize(axis))
      case PlayerFacingExactSliceProof.Pin(
            attackerSquare,
            pinnedSquare,
            behindSquare,
            targetSquare,
            attackerRole,
            pinnedRole,
            behindRole,
            _
          ) =>
        squareKey(attackerSquare) &&
          squareKey(pinnedSquare) &&
          squareKey(behindSquare) &&
          squareKey(targetSquare) &&
          normalize(targetSquare) == normalize(pinnedSquare) &&
          normalize(attackerRole).nonEmpty &&
          normalize(pinnedRole).nonEmpty &&
          normalize(behindRole).nonEmpty
      case PlayerFacingExactSliceProof.Skewer(
            attackerSquare,
            frontSquare,
            backSquare,
            targetSquare,
            attackerRole,
            frontRole,
            backRole
          ) =>
        squareKey(attackerSquare) &&
          squareKey(frontSquare) &&
          squareKey(backSquare) &&
          squareKey(targetSquare) &&
          normalize(targetSquare) == normalize(frontSquare) &&
          normalize(attackerRole).nonEmpty &&
          normalize(frontRole).nonEmpty &&
          normalize(backRole).nonEmpty
      case PlayerFacingExactSliceProof.Interference(
            blockerSquare,
            defenderSquare,
            targetSquare,
            blockerRole,
            defenderRole,
            targetRole
          ) =>
        squareKey(blockerSquare) &&
          squareKey(defenderSquare) &&
          squareKey(targetSquare) &&
          normalize(blockerRole).nonEmpty &&
          normalize(defenderRole).nonEmpty &&
          normalize(targetRole).nonEmpty

  def targetSquare(proof: PlayerFacingExactSliceProof): Option[String] =
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.DefenderTrade(_, _, targetSquare) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(_, exchangeSquare) =>
        Some(normalize(exchangeSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Overload(defenderSquare, _, _) =>
        Some(normalize(defenderSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Deflection(_, targetSquare, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.DiscoveredAttack(_, _, targetSquare, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.DoubleCheck(kingSquare, _, _, _) =>
        Some(normalize(kingSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.BackRankMate(kingSquare, _, _) =>
        Some(normalize(kingSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.MateNet(kingSquare, _, _, _) =>
        Some(normalize(kingSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.GreekGift(_, targetSquare, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Fork(_, _, targets) =>
        targets.map(target => normalize(target.square)).find(squareKey)
      case PlayerFacingExactSliceProof.HangingPiece(_, targetSquare, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.TrappedPiece(targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Domination(_, targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.StalemateTrap(kingSquare, _) =>
        Some(normalize(kingSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.PerpetualCheck(kingSquare, _, _, _) =>
        Some(normalize(kingSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Zwischenzug(_, _, _, _, targetSquare) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Decoy(_, baitSquare, _, _, _, _, _) =>
        Some(normalize(baitSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.XRay(_, _, targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Clearance(_, _, targetSquare, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Battery(_, _, targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Pin(_, _, _, targetSquare, _, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Skewer(_, _, _, targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.Interference(_, _, targetSquare, _, _, _) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case _ => None

  def fixedTargetTerm(square: String): String =
    exactTargetTerm("fixed_target", square)

  def weakSquareTerm(square: String): String =
    exactTargetTerm("weak_square", square)

  def coordinatedTargetTerm(square: String): String =
    exactTargetTerm("coordinated_target", square)

  def targetWitnessTerm(proof: PlayerFacingExactSliceProof): Option[String] =
    targetSquare(proof).map { square =>
      proof match
        case _: PlayerFacingExactSliceProof.ColorComplexSqueeze =>
          weakSquareTerm(square)
        case _: PlayerFacingExactSliceProof.TargetFocusedCoordination =>
          coordinatedTargetTerm(square)
        case _ =>
          fixedTargetTerm(square)
    }

  def targetWitnessTermForPath(proofSource: String, square: String): String =
    normalize(proofSource) match
      case source if source == ProofSourceId.ColorComplexSqueezeProbe.wireKey =>
        weakSquareTerm(square)
      case source if source == ProofSourceId.TargetFocusedCoordinationProbe.wireKey =>
        coordinatedTargetTerm(square)
      case _ =>
        fixedTargetTerm(square)

  def localFileEntryTerm(file: String, entrySquare: String): Option[String] =
    Option.when(fileToken(file) && squareKey(entrySquare))(
      s"file-entry:${normalize(file)}:${normalize(entrySquare)}"
    )

  def localFileEntryTerms(file: String, entrySquare: String): List[String] =
    localFileEntryTerm(file, entrySquare).toList

  def coordinationSupportTerms(fromSquare: String, targetPiece: String): List[String] =
    List(
      cleanTerm("support_from", fromSquare),
      cleanTerm("target_piece", targetPiece),
      Some("coordinated_piece_pressure")
    ).flatten

  def colorComplexTerm(squareColor: String): Option[String] =
    val color = normalize(squareColor)
    Option.when(Set("light", "dark").contains(color))(s"color_complex:$color")

  def minorPieceTerm(roleName: String, square: String): Option[String] =
    val role = normalize(roleName)
    Option.when(Set("bishop", "knight").contains(role) && squareKey(square))(
      s"minor_piece:${role}_${normalize(square)}"
    )

  def attacksTerm(square: String): Option[String] =
    squareTerm("attacks", square)

  def minorPieceAttackTerm(fromSquare: String, targetSquare: String): Option[String] =
    Option.when(squareKey(fromSquare) && squareKey(targetSquare))(
      s"minor_piece_attack:${normalize(fromSquare)}-${normalize(targetSquare)}"
    )

  private def proofFamily(kind: PlanTaxonomy.PlanKind): String =
    ProofFamilyId
      .fromPlanKind(kind)
      .map(_.wireKey)
      .getOrElse(kind.id)

  private def squareKey(raw: String): Boolean =
    normalize(raw).matches("[a-h][1-8]")

  private def uciMove(raw: String): Boolean =
    normalize(raw).matches("[a-h][1-8][a-h][1-8][nbrq]?")

  private def uciDestination(raw: String): Option[String] =
    Uci(normalize(raw)).collect { case move: Uci.Move => move.dest.key }

  private def fileToken(raw: String): Boolean =
    normalize(raw).matches("[a-h](?:-file)?")

  private def routeToken(raw: String): Boolean =
    normalize(raw).matches("""(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""")

  private def breakTokenShape(raw: String): Boolean =
    normalize(raw).matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""")

  private def resourceToken(raw: String): Boolean =
    val token = normalize(raw)
    token.nonEmpty &&
      !token.contains("|") &&
      (
        token.matches("""(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""") ||
          PlanEvidenceEvaluator.isProphylacticDeniedResourceTerm(token)
      )

  private def exactTargetTerm(prefix: String, square: String): String =
    s"$prefix:${normalize(square)}"

  private def squareTerm(prefix: String, square: String): Option[String] =
    Option.when(squareKey(square))(s"$prefix:${normalize(square)}")

  private def cleanTerm(prefix: String, value: String): Option[String] =
    val token = normalize(value)
    Option.when(token.nonEmpty)(s"$prefix:$token")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

private[commentary] final case class PlayerFacingProofPathWitness(
    ownerSeedTerms: List[String] = Nil,
    continuationTerms: List[String] = Nil,
    rivalTerms: List[String] = Nil,
    structureTransitionTerms: List[String] = Nil,
    exactSliceProof: Option[PlayerFacingExactSliceProof] = None
):
  def hasOwnerSeed: Boolean = ownerSeedTerms.nonEmpty
  def hasContinuation: Boolean = continuationTerms.nonEmpty
  def hasRivalContext: Boolean = rivalTerms.nonEmpty
  def hasStructureTransition: Boolean = structureTransitionTerms.nonEmpty
  def hasExactSlice: Boolean = exactSliceProof.nonEmpty

private[commentary] object PlayerFacingProofPathWitness:
  val empty: PlayerFacingProofPathWitness = PlayerFacingProofPathWitness()

private[commentary] final case class PlayerFacingClaimPacket(
    claimGate: PlanEvidenceEvaluator.ClaimCertification = PlanEvidenceEvaluator.ClaimCertification(),
    proofSource: String = "unowned",
    proofFamily: String = "unknown",
    scope: PlayerFacingPacketScope = PlayerFacingPacketScope.BackendOnly,
    triggerKind: String = "unknown",
    anchorTerms: List[String] = Nil,
    bestDefenseMove: Option[String] = None,
    bestDefenseBranchKey: Option[String] = None,
    sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Missing,
    persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Broken,
    rivalKind: Option[String] = None,
    proofPathWitness: PlayerFacingProofPathWitness = PlayerFacingProofPathWitness.empty,
    suppressionReasons: List[String] = Nil,
    releaseRisks: List[String] = Nil,
    fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.Suppress,
    proofTrace: ProofTrace = ProofTrace.empty
):
  def admitsStrategicTruthMode: Boolean =
    scope != PlayerFacingPacketScope.BackendOnly &&
      fallbackMode != PlayerFacingClaimFallbackMode.Suppress

  def allowsLineEvidence: Boolean =
    fallbackMode == PlayerFacingClaimFallbackMode.WeakMain ||
      fallbackMode == PlayerFacingClaimFallbackMode.LineOnly

  def allowsMoveLocalClaim: Boolean =
    scope == PlayerFacingPacketScope.MoveLocal &&
      fallbackMode == PlayerFacingClaimFallbackMode.WeakMain

private[commentary] object PlayerFacingClaimPacket:
  private val lineOnlyPilotOwners =
    Set.empty[String]
  private val lineOnlyPilotFamilies =
    Set.empty[String]

  def isLineOnlyPilot(proofSource: String, proofFamily: String): Boolean =
    lineOnlyPilotOwners.contains(proofSource) || lineOnlyPilotFamilies.contains(proofFamily)

  val empty: PlayerFacingClaimPacket = PlayerFacingClaimPacket()
