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
  final case class DefenderTrade(defenderSquare: String, exchangeSquare: String, targetSquare: String)
      extends PlayerFacingExactSliceProof
  final case class BadPieceLiquidation(badPieceSquare: String, exchangeSquare: String) extends PlayerFacingExactSliceProof
  final case class CentralBreakTiming(
      breakMove: String,
      breakSquare: String,
      breakToken: String
  ) extends PlayerFacingExactSliceProof
  final case class OutpostOccupation(pieceRole: String, square: String) extends PlayerFacingExactSliceProof

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
      case PlayerFacingExactSliceProof.DefenderTrade(_, _, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.DefenderTrade)
        Path(family, family)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(_, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.BadPieceLiquidation)
        Path(family, family)
      case PlayerFacingExactSliceProof.CentralBreakTiming(_, _, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.CentralBreakTiming)
        Path(family, family)
      case PlayerFacingExactSliceProof.OutpostOccupation(_, _) =>
        val family = proofFamily(PlanTaxonomy.PlanKind.OutpostEntrenchment)
        Path(family, family)

  def matchesPacket(
      packet: PlayerFacingClaimPacket,
      proof: PlayerFacingExactSliceProof
  ): Boolean =
    validShape(proof) &&
      expectedPath(proof).matches(packet) &&
      proofTermsMatchPacket(packet, proof)

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
          squareKey(minorPieceSquare) &&
          colorComplexProofGeometry(targetSquare, squareColor, minorPieceRole, minorPieceSquare)
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        val fileBase = normalize(file).stripSuffix("-file")
        fileToken(file) &&
          squareKey(entrySquare) &&
          fileBase.length == 1 &&
          normalize(entrySquare).startsWith(fileBase)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) =>
        breakTokenShape(breakToken)
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceTokenValue) =>
        resourceToken(resourceTokenValue)
      case PlayerFacingExactSliceProof.QueenTradeShield(lineMoves) =>
        lineMoves.size >= 2 &&
          lineMoves.forall(uciMove)
      case PlayerFacingExactSliceProof.DefenderTrade(defenderSquare, exchangeSquare, targetSquare) =>
        squareKey(defenderSquare) &&
          squareKey(exchangeSquare) &&
          squareKey(targetSquare)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(badPieceSquare, exchangeSquare) =>
        squareKey(badPieceSquare) &&
          squareKey(exchangeSquare)
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        uciMove(breakMove) &&
          squareKey(breakSquare) &&
          uciDestination(breakMove).contains(normalize(breakSquare)) &&
          routeToken(breakToken)
      case PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square) =>
        normalize(pieceRole) == "knight" &&
          squareKey(square)

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
      case PlayerFacingExactSliceProof.OutpostOccupation(_, square) =>
        Some(normalize(square)).filter(squareKey)
      case PlayerFacingExactSliceProof.DefenderTrade(_, _, targetSquare) =>
        Some(normalize(targetSquare)).filter(squareKey)
      case PlayerFacingExactSliceProof.BadPieceLiquidation(_, exchangeSquare) =>
        Some(normalize(exchangeSquare)).filter(squareKey)
      case _ => None

  private def proofTermsMatchPacket(
      packet: PlayerFacingClaimPacket,
      proof: PlayerFacingExactSliceProof
  ): Boolean =
    val terms = packetTerms(packet)
    proof match
      case PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare) =>
        val target = normalize(targetSquare)
        terms.contains(s"fixed_target:$target")
      case PlayerFacingExactSliceProof.CarlsbadFixedTarget(targetSquare, _) =>
        val target = normalize(targetSquare)
        terms.contains(s"fixed_target:$target")
      case PlayerFacingExactSliceProof.TargetFocusedCoordination(targetSquare, _, _) =>
        val target = normalize(targetSquare)
        terms.contains(s"coordinated_target:$target")
      case PlayerFacingExactSliceProof.ColorComplexSqueeze(targetSquare, squareColor, minorPieceRole, minorPieceSquare) =>
        val target = normalize(targetSquare)
        val from = normalize(minorPieceSquare)
        val role = normalize(minorPieceRole)
        terms.contains(s"weak_square:$target") &&
          terms.contains(s"color_complex:${normalize(squareColor)}") &&
          terms.contains(s"minor_piece:${role}_${from}") &&
          terms.contains(s"minor_piece_attack:$from-$target")
      case PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare) =>
        val entry = normalize(entrySquare)
        val fileBase = normalize(file).stripSuffix("-file")
        val fileWithSuffix = s"$fileBase-file"
        val pairTerms = Set(s"file-entry:$fileWithSuffix:$entry", s"file_entry:$fileWithSuffix:$entry")
        pairTerms.exists(terms.contains)
      case PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken) =>
        terms.contains(normalize(breakToken))
      case PlayerFacingExactSliceProof.ProphylacticRestraint(resourceTokenValue) =>
        terms.contains(normalize(resourceTokenValue))
      case PlayerFacingExactSliceProof.QueenTradeShield(lineMoves) =>
        val moves = lineMoves.map(normalize).filter(uciMove)
        moves.size >= 2 && moves.forall(terms.contains)
      case PlayerFacingExactSliceProof.DefenderTrade(defenderSquare, exchangeSquare, targetSquare) =>
        val defender = normalize(defenderSquare)
        val exchange = normalize(exchangeSquare)
        val target = normalize(targetSquare)
        terms.contains("defender_trade_branch") &&
          terms.contains(s"defender:$defender") &&
          terms.contains(s"exchange_square:$exchange") &&
          terms.contains(s"defended_target:$target")
      case PlayerFacingExactSliceProof.BadPieceLiquidation(badPieceSquare, exchangeSquare) =>
        val badPiece = normalize(badPieceSquare)
        val exchange = normalize(exchangeSquare)
        terms.contains("bad_piece_liquidation_branch") &&
          terms.contains(s"bad_piece:$badPiece") &&
          terms.contains(s"exchange_square:$exchange")
      case PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken) =>
        val move = normalize(breakMove)
        val square = normalize(breakSquare)
        val token = normalize(breakToken)
        terms.contains(s"break_move:$move") &&
          (terms.contains(s"break_token:$token") || terms.contains(token)) &&
          (terms.contains(s"central_break:$square") || terms.contains(s"break_square:$square") || terms.contains(square))
      case PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square) =>
        val role = normalize(pieceRole)
        val target = normalize(square)
        terms.contains(s"outpost:$target") &&
          terms.contains(s"piece:$role") &&
          terms.contains(s"outpost_occupation:$role:$target")

  private def packetTerms(packet: PlayerFacingClaimPacket): Set[String] =
    (
      packet.anchorTerms ++
        packet.proofPathWitness.ownerSeedTerms ++
        packet.proofPathWitness.continuationTerms ++
        packet.proofPathWitness.structureTransitionTerms
    ).map(normalize).filter(_.nonEmpty).toSet

  private def proofFamily(kind: PlanTaxonomy.PlanKind): String =
    ProofFamilyId
      .fromPlanKind(kind)
      .map(_.wireKey)
      .getOrElse(kind.id)

  private def squareKey(raw: String): Boolean =
    normalize(raw).matches("[a-h][1-8]")

  private def colorComplexProofGeometry(
      targetSquare: String,
      squareColor: String,
      minorPieceRole: String,
      minorPieceSquare: String
  ): Boolean =
    squareColorOf(targetSquare).contains(normalize(squareColor)) &&
      roleCanAttackSquare(minorPieceRole, minorPieceSquare, targetSquare)

  private def squareColorOf(raw: String): Option[String] =
    squareCoords(raw).map { case (file, rank) =>
      if (file + rank) % 2 == 0 then "dark" else "light"
    }

  private def roleCanAttackSquare(role: String, from: String, target: String): Boolean =
    (squareCoords(from), squareCoords(target)) match
      case (Some((fromFile, fromRank)), Some((targetFile, targetRank))) =>
        val fileDelta = math.abs(fromFile - targetFile)
        val rankDelta = math.abs(fromRank - targetRank)
        normalize(role) match
          case "bishop" => fileDelta == rankDelta && fileDelta > 0
          case "knight" => (fileDelta == 1 && rankDelta == 2) || (fileDelta == 2 && rankDelta == 1)
          case _        => false
      case _ => false

  private def squareCoords(raw: String): Option[(Int, Int)] =
    val key = normalize(raw)
    Option.when(key.matches("[a-h][1-8]")) {
      (key.charAt(0) - 'a' + 1, key.charAt(1).asDigit)
    }

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
          token.matches("""denied_resource:(?:break|entry_square|forcing_threat|piece_activity|counterplay_route|route_node|reroute_square|pressure|color_complex_escape)""")
      )

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
