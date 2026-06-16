package lila.commentary.analysis.semantic.evidence

import lila.commentary.*
import lila.commentary.analysis.{ BreakFileToken, PlanTaxonomy, PositionFeatures, StrategicIdeaSemanticContext, StrategicStateFeatures }
import lila.commentary.analysis.semantic.{ StrategicIdeaEvidence, StrategicIdeaEvidenceTier }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceSourceId
import _root_.chess.{ Bishop, Board, Color, File, Knight, Move, Pawn, Position, Queen, Rank, Role, Rook, Square }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard
import lila.commentary.analysis.L3.{ PawnPlayAnalysis, ThreatAnalysis, ThreatKind }
import lila.commentary.model.{ Motif, PlanId, PlanMatch, StrategicPlanExperiment }
import lila.commentary.model.strategic.{ PositionalTag, PreventedPlan }
import lila.commentary.model.structure.{ StructureId }


private[evidence] object StrategicIdeaEvidenceSupport:

  private val SanDestinationSquarePattern = """(?i)([a-h][1-8])""".r

  private def isPlayableExperiment(experiment: StrategicPlanExperiment): Boolean =
    experiment.evidenceTier != "refuted"

  def evidence(
      ownerSide: String,
      kind: String,
      readiness: String,
      source: EvidenceSourceId,
      confidence: Double,
      focusSquares: List[String] = Nil,
      focusFiles: List[String] = Nil,
      focusDiagonals: List[String] = Nil,
      focusZone: Option[String] = None,
      beneficiaryPieces: List[String] = Nil,
      factIds: List[String] = Nil,
      tier: StrategicIdeaEvidenceTier = StrategicIdeaEvidenceTier.SelectorSupport
  ): StrategicIdeaEvidence =
    StrategicIdeaEvidence.from(
      ownerSide = ownerSide,
      kind = kind,
      readiness = readiness,
      source = source,
      confidence = confidence,
      focusSquares = focusSquares,
      focusFiles = focusFiles,
      focusDiagonals = focusDiagonals,
      focusZone = focusZone,
      beneficiaryPieces = beneficiaryPieces,
      factIds = factIds,
      tier = tier
    )

  def prophylaxisThreatBonus(prevented: PreventedPlan): Double =
    (if prevented.preventedThreatType.isDefined then 0.04 else 0.0) +
      (if prevented.mobilityDelta < 0 then 0.02 else 0.0) +
      (if prevented.defensiveSufficiency.exists(_ >= 80) then 0.02 else 0.0)

  def counterplaySuppressionBonus(prevented: PreventedPlan): Double =
    (if prevented.breakNeutralized.isDefined then 0.06 else 0.0) +
      (if prevented.counterplayScoreDrop >= 140 then 0.06 else if prevented.counterplayScoreDrop >= 100 then 0.03 else 0.0) +
      (if prevented.deniedSquares.size >= 2 then 0.03 else 0.0) +
      (if prevented.deniedResourceClass.contains("break") then 0.03 else 0.0) +
      (if prevented.breakNeutralizationStrength.exists(_ >= 80) then 0.03 else 0.0)

  def moveRefSupportBonus(
      side: String,
      ref: StrategyPieceMoveRef,
      semantic: StrategicIdeaSemanticContext
  ): Double =
    val materialBonus =
      semantic.positionFeatures.fold(0.0)(features =>
        if materialEdgeFor(side, features) >= 150 then 0.08
        else if materialEdgeFor(side, features) >= 80 then 0.04
        else 0.0
      )
    val badBishopBonus =
      if ownBadBishop(side, semantic) && ref.piece == "B" then 0.06 else 0.0
    materialBonus + badBishopBonus

  def favorableTradeContext(
      side: String,
      ref: StrategyPieceMoveRef,
      semantic: StrategicIdeaSemanticContext,
      hasStructuredTradeSignal: Boolean
  ): Boolean =
    hasStructuredTradeSignal ||
      hasFavorableClassificationWindow(side, semantic) ||
      semantic.positionFeatures.exists(features => materialEdgeFor(side, features) >= 80) ||
      (ownBadBishop(side, semantic) && ref.piece == "B")

  def isPreventiveWithoutCounterplaySuppression(prevented: PreventedPlan): Boolean =
    (
      prevented.deniedSquares.nonEmpty ||
        prevented.preventedThreatType.isDefined ||
        prevented.mobilityDelta < 0 ||
        prevented.counterplayScoreDrop > 0 ||
        prevented.deniedResourceClass.isDefined ||
        prevented.defensiveSufficiency.exists(_ > 0)
    ) &&
      !isCounterplaySuppression(prevented)

  def isCounterplaySuppression(prevented: PreventedPlan): Boolean =
    prevented.breakNeutralized.isDefined ||
      prevented.deniedResourceClass.contains("break") ||
      prevented.deniedEntryScope.exists(scope => scope == "file" || scope == "sector") ||
      prevented.breakNeutralizationStrength.exists(_ >= 60) ||
      prevented.counterplayScoreDrop >= 100 ||
      prevented.deniedSquares.size >= 2 ||
      prevented.mobilityDelta <= -2

  def topPlansFor(side: String, semantic: StrategicIdeaSemanticContext): List[PlanMatch] =
    semantic.plans
      .filter(plan => matchesSide(plan.plan.color, side))
      .sortBy(plan => -plan.score)

  def structureIs(semantic: StrategicIdeaSemanticContext, structureId: StructureId): Boolean =
    semantic.structureProfile.exists(_.primary == structureId)

  def clampForSide(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.strategicState.exists(colorComplexClampFor(side, _))

  def mobilityClampForSide(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.positionFeatures.exists(features =>
      lowMobilityPiecesFor(opponentSide(side), features) > lowMobilityPiecesFor(side, features) + 1
    )

  def hasBishopPinWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      if side == "white" then
        hasPiece(board, Color.White, Square.F3, Knight) &&
        hasPiece(board, Color.Black, Square.C8, Bishop) &&
        diagonalClear(board, Square.C8, Square.G4)
      else
        hasPiece(board, Color.Black, Square.F6, Knight) &&
        hasPiece(board, Color.White, Square.C1, Bishop) &&
        diagonalClear(board, Square.C1, Square.G5)
    }

  def hasQueensideClampWatch(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      if side == "white" then
        pawnAt(board, Color.White, Square.C4) &&
        pawnAt(board, Color.White, Square.D5) &&
        pawnAt(board, Color.White, Square.E4) &&
        pawnAt(board, Color.Black, Square.D6) &&
        pawnAt(board, Color.Black, Square.E5) &&
        pawnAt(board, Color.Black, Square.G6) &&
        pawnAt(board, Color.Black, Square.B7)
      else
        pawnAt(board, Color.Black, Square.C5) &&
        pawnAt(board, Color.Black, Square.D4) &&
        pawnAt(board, Color.Black, Square.E5) &&
        pawnAt(board, Color.White, Square.D3) &&
        pawnAt(board, Color.White, Square.E4) &&
        pawnAt(board, Color.White, Square.G3) &&
        pawnAt(board, Color.White, Square.B2)
    }

  def hasOppositeSideStormAttack(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.board.exists { board =>
      val ourKing = board.kingPosOf(sideColor(side))
      val theirKing = board.kingPosOf(sideColor(opponentSide(side)))
      side match
        case "white" =>
          ourKing.exists(king => king.file.value <= File.C.value) &&
          theirKing.exists(king => king.file.value >= File.G.value) &&
          (
            pawnAt(board, Color.White, Square.H4) ||
              pawnAt(board, Color.White, Square.H5) ||
              pawnAt(board, Color.White, Square.G4) ||
              pawnAt(board, Color.White, Square.G5)
          )
        case _ =>
          ourKing.exists(king => king.file.value >= File.F.value) &&
          theirKing.exists(king => king.file.value <= File.C.value) &&
          (
            pawnAt(board, Color.Black, Square.H5) ||
              pawnAt(board, Color.Black, Square.H4) ||
              pawnAt(board, Color.Black, Square.G5) ||
              pawnAt(board, Color.Black, Square.G4)
          )
    }

  def hasStablePlanEvidence(
      semantic: StrategicIdeaSemanticContext,
      kind: String
  ): Boolean =
    semantic.strategicPlanExperiments.exists { experiment =>
      planEvidenceAppliesToKind(experiment, kind) &&
        isPlayableExperiment(experiment) &&
        !experiment.moveOrderSensitive &&
        (
          experiment.bestReplyStable ||
            experiment.futureSnapshotAligned ||
            experiment.counterBreakNeutralized ||
            experiment.supportProbeCount > 0
        )
    }

  def planEvidenceAppliesToKind(
      experiment: StrategicPlanExperiment,
      kind: String
  ): Boolean =
    val matchedKinds =
      experiment.subplanId
        .flatMap(PlanTaxonomy.PlanKind.fromId)
        .map {
          case PlanTaxonomy.PlanKind.BreakPrevention | PlanTaxonomy.PlanKind.KeySquareDenial =>
            if experiment.counterBreakNeutralized then Set(StrategicIdeaKind.CounterplaySuppression, StrategicIdeaKind.Prophylaxis)
            else Set(StrategicIdeaKind.Prophylaxis)
          case PlanTaxonomy.PlanKind.ProphylaxisRestraint =>
            Set(StrategicIdeaKind.Prophylaxis)
          case PlanTaxonomy.PlanKind.OutpostEntrenchment =>
            Set(StrategicIdeaKind.OutpostCreationOrOccupation)
          case PlanTaxonomy.PlanKind.WorstPieceImprovement | PlanTaxonomy.PlanKind.BishopReanchor =>
            Set(StrategicIdeaKind.MinorPieceImbalanceExploitation, StrategicIdeaKind.LineOccupation)
          case PlanTaxonomy.PlanKind.RookFileTransfer | PlanTaxonomy.PlanKind.OpenFilePressure =>
            Set(StrategicIdeaKind.LineOccupation)
          case PlanTaxonomy.PlanKind.FlankClamp | PlanTaxonomy.PlanKind.CentralSpaceBind |
              PlanTaxonomy.PlanKind.MobilitySuppression =>
            Set(StrategicIdeaKind.SpaceGainOrRestriction)
          case PlanTaxonomy.PlanKind.StaticWeaknessFixation | PlanTaxonomy.PlanKind.MinorityAttackFixation |
              PlanTaxonomy.PlanKind.BackwardPawnTargeting | PlanTaxonomy.PlanKind.IQPInducement =>
            Set(StrategicIdeaKind.TargetFixing)
          case PlanTaxonomy.PlanKind.CentralBreakTiming | PlanTaxonomy.PlanKind.WingBreakTiming |
              PlanTaxonomy.PlanKind.TensionMaintenance =>
            Set(StrategicIdeaKind.PawnBreak)
          case PlanTaxonomy.PlanKind.SimplificationWindow | PlanTaxonomy.PlanKind.DefenderTrade |
              PlanTaxonomy.PlanKind.QueenTradeShield | PlanTaxonomy.PlanKind.SimplificationConversion |
              PlanTaxonomy.PlanKind.PasserConversion | PlanTaxonomy.PlanKind.PassedPawnManufacture |
              PlanTaxonomy.PlanKind.BadPieceLiquidation | PlanTaxonomy.PlanKind.InvasionTransition |
              PlanTaxonomy.PlanKind.OppositeBishopsConversion =>
            Set(StrategicIdeaKind.FavorableTradeOrTransformation)
          case PlanTaxonomy.PlanKind.RookPawnMarch | PlanTaxonomy.PlanKind.HookCreation |
              PlanTaxonomy.PlanKind.RookLiftScaffold =>
            Set(StrategicIdeaKind.KingAttackBuildUp)
          case PlanTaxonomy.PlanKind.OpeningDevelopment |
              PlanTaxonomy.PlanKind.ForcingTacticalShot | PlanTaxonomy.PlanKind.DefenderOverload |
              PlanTaxonomy.PlanKind.ClearanceBreak | PlanTaxonomy.PlanKind.BatteryPressure =>
            Set.empty[String]
        }
        .getOrElse(themeIdeaKinds(experiment.themeL1))
    matchedKinds.contains(kind)

  def themeIdeaKinds(themeL1: String): Set[String] =
    PlanTaxonomy.PlanTheme
      .fromId(themeL1)
      .map {
        case PlanTaxonomy.PlanTheme.RestrictionProphylaxis =>
          Set(StrategicIdeaKind.Prophylaxis)
        case PlanTaxonomy.PlanTheme.PieceRedeployment =>
          Set(StrategicIdeaKind.LineOccupation)
        case PlanTaxonomy.PlanTheme.SpaceClamp =>
          Set(StrategicIdeaKind.SpaceGainOrRestriction)
        case PlanTaxonomy.PlanTheme.WeaknessFixation =>
          Set(StrategicIdeaKind.TargetFixing)
        case PlanTaxonomy.PlanTheme.PawnBreakPreparation =>
          Set(StrategicIdeaKind.PawnBreak)
        case PlanTaxonomy.PlanTheme.FavorableExchange | PlanTaxonomy.PlanTheme.AdvantageTransformation =>
          Set(StrategicIdeaKind.FavorableTradeOrTransformation)
        case PlanTaxonomy.PlanTheme.FlankInfrastructure =>
          Set(StrategicIdeaKind.KingAttackBuildUp)
        case PlanTaxonomy.PlanTheme.ImmediateTacticalGain =>
          Set.empty[String]
        case _ =>
          Set.empty[String]
      }
      .getOrElse(Set.empty)

  def isNearEnemyKing(
      side: String,
      square: Square,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.board.flatMap(_.kingPosOf(sideColor(opponentSide(side)))).exists(enemyKing => chebyshev(square, enemyKing) <= 2)

  def routeAttackLaneEndpoint(
      side: String,
      route: StrategyPieceRoute,
      semantic: StrategicIdeaSemanticContext
  ): Option[Square] =
    Option
      .when(route.ownerSide == side && route.surfaceMode != RouteSurfaceMode.Hidden)(route)
      .flatMap(_.route.lastOption)
      .flatMap(squareFromKey)
      .filterNot(endpoint => endpoint.key == route.from.trim.toLowerCase)
      .filter(endpoint => isBoardProvedAttackLaneToEnemyKing(side, route.piece, endpoint, semantic))

  def directionalAttackLaneEndpoint(
      side: String,
      target: StrategyDirectionalTarget,
      semantic: StrategicIdeaSemanticContext
  ): Option[Square] =
    Option
      .when(target.ownerSide == side)(target)
      .flatMap(target => squareFromKey(target.targetSquare))
      .filter(endpoint => isBoardProvedAttackLaneToEnemyKing(side, target.piece, endpoint, semantic))

  def isBoardProvedAttackLaneToEnemyKing(
      side: String,
      piece: String,
      endpoint: Square,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.board.exists { board =>
      board
        .kingPosOf(sideColor(opponentSide(side)))
        .exists(enemyKing =>
          isNearEnemyKing(side, endpoint, semantic) &&
            endpoint != enemyKing &&
            pieceAttacksSquareFrom(piece, side, endpoint, enemyKing, board)
        )
    }

  private def pieceAttacksSquareFrom(
      piece: String,
      side: String,
      from: Square,
      target: Square,
      board: Board
  ): Boolean =
    piece.trim.toUpperCase match
      case "N" => from.knightAttacks.contains(target)
      case "B" => from.bishopAttacks(board.occupied).contains(target)
      case "R" => from.rookAttacks(board.occupied).contains(target)
      case "Q" => from.queenAttacks(board.occupied).contains(target)
      case "P" => from.pawnAttacks(sideColor(side)).contains(target)
      case "K" => from.kingAttacks.contains(target)
      case _   => false

  def hasAlignmentReason(semantic: StrategicIdeaSemanticContext, code: String): Boolean =
    semantic.planAlignmentReasonCodes.contains(code)

  def hasFavorableClassificationWindow(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.classification.exists(classification =>
      semantic.positionFeatures.exists(features =>
        materialEdgeFor(side, features) >= 80 &&
          (classification.simplifyBias.shouldSimplify || classification.taskMode.isConvertMode)
      )
    )

  def bishopPairFor(side: String, features: PositionFeatures): Boolean =
    if side == "white" then features.imbalance.whiteBishopPair else features.imbalance.blackBishopPair

  def hasPiece(board: Board, color: Color, square: Square, role: Role): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  def pawnAt(board: Board, color: Color, square: Square): Boolean =
    hasPiece(board, color, square, Pawn)

  def diagonalClear(board: Board, from: Square, to: Square): Boolean =
    val fileStep = math.signum(to.file.value - from.file.value)
    val rankStep = math.signum(to.rank.value - from.rank.value)
    val fileDiff = (to.file.value - from.file.value).abs
    val rankDiff = (to.rank.value - from.rank.value).abs
    if fileDiff != rankDiff || fileDiff == 0 then false
    else
      (1 until fileDiff).forall { offset =>
        Square
          .at(from.file.value + offset * fileStep, from.rank.value + offset * rankStep)
          .forall(board.pieceAt(_).isEmpty)
      }

  def zoneFromSquares(squares: List[Square]): Option[String] =
    mostCommon(squares.flatMap(zoneFromSquare))

  def threatSquares(threats: ThreatAnalysis): List[String] =
    threats.threats.flatMap(_.attackSquares).flatMap(squareFromKey).map(_.key).distinct.take(3)

  def threatFocusZone(threats: ThreatAnalysis): Option[String] =
    zoneFromSquareKeys(threats.threats.flatMap(_.attackSquares))

  def threatDefenseBonus(threats: ThreatAnalysis): Double =
    (if threats.resourceAvailable then 0.03 else 0.0) +
      (if threats.maxLossIfIgnored >= 120 then 0.03 else if threats.maxLossIfIgnored >= 60 then 0.01 else 0.0)

  def threatSuppressionBonus(threats: ThreatAnalysis): Double =
    (if threats.maxLossIfIgnored >= 250 then 0.06 else if threats.maxLossIfIgnored >= 180 then 0.03 else 0.0) +
      (if threats.threats.exists(_.kind == ThreatKind.Mate) then 0.04 else 0.0)

  def isThreatDrivenProphylaxis(threats: ThreatAnalysis): Boolean =
    threats.prophylaxisNeeded &&
      !threats.immediateThreat &&
      threats.resourceAvailable &&
      !threats.counterThreatBetter &&
      (threats.strategicThreat || threats.defense.prophylaxisNeeded) &&
      threats.maxLossIfIgnored > 0 &&
      threats.maxLossIfIgnored < 250

  def isThreatDrivenCounterplaySuppression(
      threats: ThreatAnalysis,
      opponentPawnAnalysis: Option[PawnPlayAnalysis],
      preventedPlans: List[PreventedPlan]
  ): Boolean =
    threats.maxLossIfIgnored >= 120 &&
      !threats.threatIgnorable &&
      (
        threats.strategicThreat ||
          opponentPawnAnalysis.exists(_.counterBreak) ||
          preventedPlans.exists(isCounterplaySuppression)
      )

  def preventsCounterBreak(
      prevented: PreventedPlan,
      opponentPawn: PawnPlayAnalysis
  ): Boolean =
    opponentPawn.counterBreak &&
      (
        prevented.breakNeutralized.flatMap(normalizeFileToken) == opponentPawn.breakFile.flatMap(normalizeFileToken) ||
          prevented.deniedResourceClass.contains("break") ||
          prevented.breakNeutralizationStrength.exists(_ >= 60) ||
          prevented.counterplayScoreDrop >= 100
      )

  def directCentralBreakOwnsPreventedBreak(
      prevented: PreventedPlan,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    prevented.breakNeutralized
      .flatMap(breakDestination)
      .exists(destination => playedDirectCentralBreakDestination(semantic).contains(destination))

  private def playedDirectCentralBreakDestination(semantic: StrategicIdeaSemanticContext): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(semantic.fen))
      uci <- semantic.playedMove.map(normalizeUci).filter(isUci)
      move <- legalMove(position, uci)
      if isDirectCentralBreak(move, position.color)
    yield move.dest.key

  private def breakDestination(token: String): Option[String] =
    val normalized = normalizeToken(token).stripPrefix("...")
    normalized
      .split("-")
      .lastOption
      .flatMap(squareFromKey)
      .map(_.key)

  private def isDirectCentralBreak(move: Move, side: Color): Boolean =
    move.piece.role == Pawn &&
      move.piece.color == side &&
      isForward(move, side) &&
      move.orig.file == move.dest.file &&
      isCoreCentralSquare(move.dest) &&
      !move.captures &&
      adjacentEnemyPawn(move.after.board, side, move.dest)

  private def isForward(move: Move, side: Color): Boolean =
    if side.white then move.dest.rank.value > move.orig.rank.value
    else move.dest.rank.value < move.orig.rank.value

  private def isCoreCentralSquare(square: Square): Boolean =
    Set("d4", "e4", "d5", "e5").contains(square.key)

  private def adjacentEnemyPawn(board: Board, side: Color, square: Square): Boolean =
    Square.all.exists(candidate =>
      candidate != square &&
        (candidate.file.value - square.file.value).abs <= 1 &&
        (candidate.rank.value - square.rank.value).abs <= 1 &&
        board.pieceAt(candidate).exists(piece => piece.color != side && piece.role == Pawn)
    )

  private def legalMove(position: Position, uci: String): Option[Move] =
    Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def normalizeUci(raw: String): String =
    normalizeToken(raw)

  private def normalizeToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def isUci(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  def isKingAttackThreatProfile(
      threats: ThreatAnalysis,
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    threats.threats.exists(_.kind == ThreatKind.Mate) ||
      threats.primaryDriver == "mate_threat" ||
      (
        threats.maxLossIfIgnored >= 180 &&
          threatSquares(threats).flatMap(squareFromKey).exists(isNearEnemyKing(side, _, semantic))
      )

  def checkTypeBonus(checkType: Motif.CheckType): Double =
    checkType match
      case Motif.CheckType.Mate       => 0.10
      case Motif.CheckType.Double     => 0.06
      case Motif.CheckType.Discovered => 0.04
      case Motif.CheckType.Smothered  => 0.08
      case _                          => 0.02

  def ideaReadinessFromDirectionalTarget(readiness: String): String =
    readiness match
      case DirectionalTargetReadiness.Build     => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Contested => StrategicIdeaReadiness.Build
      case DirectionalTargetReadiness.Premature => StrategicIdeaReadiness.Premature
      case DirectionalTargetReadiness.Blocked   => StrategicIdeaReadiness.Blocked
      case _                                    => StrategicIdeaReadiness.Build

  def ideaReadinessFromRoute(surfaceMode: String): String =
    if surfaceMode == RouteSurfaceMode.Exact then StrategicIdeaReadiness.Ready else StrategicIdeaReadiness.Build

  def readinessBonus(readiness: String): Double =
    readiness match
      case DirectionalTargetReadiness.Build     => 0.04
      case DirectionalTargetReadiness.Contested => 0.00
      case DirectionalTargetReadiness.Premature => -0.02
      case DirectionalTargetReadiness.Blocked   => -0.05
      case _                                    => 0.0

  def roleToken(role: Role): String =
    role match
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case Pawn   => "P"
      case _      => "K"

  def normalizeFileToken(value: String): Option[String] =
    BreakFileToken.extract(value)

  def zoneFromFileToken(file: String): Option[String] =
    file.trim.toLowerCase.headOption.flatMap {
      case ch if ch <= 'c'              => Some("queenside")
      case ch if ch >= 'f'              => Some("kingside")
      case ch if ch == 'd' || ch == 'e' => Some("center")
      case _                            => None
    }

  def fileToken(file: File): String = file.char.toString

  def squareFromKey(key: String): Option[Square] =
    Square.all.find(_.key == Option(key).map(_.trim.toLowerCase).getOrElse(""))

  def destinationSquareFromSan(raw: String): Option[Square] =
    SanDestinationSquarePattern
      .findAllMatchIn(Option(raw).getOrElse(""))
      .toList
      .lastOption
      .flatMap(m => squareFromKey(m.group(1)))

  def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  def opponentSide(side: String): String =
    if side == "white" then "black" else "white"

  def matchesSide(color: Color, side: String): Boolean =
    color.white == (side == "white")

  def materialEdgeFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.materialPhase.materialDiff else -features.materialPhase.materialDiff

  def hasCompensationMaterialDeficitFor(side: String, features: PositionFeatures): Boolean =
    val edge = materialEdgeFor(side, features)
    edge <= -1 && edge >= -3

  def lowMobilityPiecesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.whiteLowMobilityPieces else features.activity.blackLowMobilityPieces

  def developmentLagFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.whiteDevelopmentLag else features.activity.blackDevelopmentLag

  def enemyDevelopmentLagFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.activity.blackDevelopmentLag else features.activity.whiteDevelopmentLag

  def developmentLeadFor(side: String, features: PositionFeatures): Int =
    (enemyDevelopmentLagFor(side, features) - developmentLagFor(side, features)).max(0)

  def attackersCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.whiteAttackersCount else features.kingSafety.blackAttackersCount

  def enemyKingRingAttackedFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingRingAttacked else features.kingSafety.whiteKingRingAttacked

  def enemyKingExposedFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.kingSafety.blackKingExposedFiles else features.kingSafety.whiteKingExposedFiles

  def enemyKingCastledSideFor(side: String, features: PositionFeatures): String =
    if side == "white" then features.kingSafety.blackCastledSide else features.kingSafety.whiteCastledSide

  def colorComplexClampFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteColorComplexClamp else state.blackColorComplexClamp

  def hookCreationChanceFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteHookCreationChance else state.blackHookCreationChance

  def rookPawnMarchReadyFor(side: String, state: StrategicStateFeatures): Boolean =
    if side == "white" then state.whiteRookPawnMarchReady else state.blackRookPawnMarchReady

  def ownBadBishop(side: String, semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.positionalFeatures.exists {
      case PositionalTag.BadBishop(color) => matchesSide(color, side)
      case _                              => false
    } || semantic.board.exists { board =>
      semantic.pieceActivity.exists(activity =>
        activity.isBadBishop && board.colorAt(activity.square).exists(color => matchesSide(color, side))
      )
    }

  def isCompensationEligiblePhase(semantic: StrategicIdeaSemanticContext): Boolean =
    semantic.phase == "opening" || semantic.phase == "middlegame"

  def hasConversionPlanPressure(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    topPlansFor(side, semantic).take(2).exists(plan =>
      plan.plan.id == PlanId.Simplification ||
        plan.plan.id == PlanId.Exchange ||
        plan.plan.id == PlanId.QueenTrade
    )

  def hasCompensationAttackPlanSupport(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    !hasConversionPlanPressure(side, semantic) &&
      topPlansFor(side, semantic).take(4).exists(plan =>
        plan.plan.id == PlanId.OpeningDevelopment ||
          plan.plan.id == PlanId.PieceActivation ||
          plan.plan.id == PlanId.KingsideAttack ||
          plan.plan.id == PlanId.PawnStorm ||
          plan.plan.id == PlanId.FileControl
      )

  def hasAttackLaneTowardEnemyKing(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    pack.pieceRoutes.exists(route => routeAttackLaneEndpoint(side, route, semantic).nonEmpty) ||
      pack.directionalTargets.exists(target => directionalAttackLaneEndpoint(side, target, semantic).nonEmpty)

  def hasDiagonalBatteryCompensation(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    semantic.motifs.exists {
      case Motif.Battery(_, _, axis, color, _, _, _, _)
          if matchesSide(color, side) && axis == Motif.BatteryAxis.Diagonal =>
        true
      case _ => false
    }

  def chebyshev(a: Square, b: Square): Int =
    math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)

  def zoneFromSquare(square: Square): Option[String] =
    if square.file.value <= File.C.value then Some("queenside")
    else if square.file.value >= File.F.value then Some("kingside")
    else Some("center")

  def zoneFromSquareKeys(keys: List[String]): Option[String] =
    zoneFromSquares(keys.flatMap(squareFromKey))

  def mostCommon(values: List[String]): Option[String] =
    values.groupBy(identity).toList.sortBy { case (value, grouped) => (-grouped.size, value) }.headOption.map(_._1)

  def lineAccessFacts(
      side: String,
      endpoint: Square,
      semantic: StrategicIdeaSemanticContext
  ): Option[(List[String], Option[String], List[String])] =
    semantic.board.flatMap { board =>
      val color = sideColor(side)
      val open = isOpenFile(board, endpoint.file)
      val semiOpen = isSemiOpenFileFor(board, endpoint.file, color)
      val seventh = isSeventhRankFor(side, endpoint)
      Option.when(open || semiOpen || seventh) {
        val files = Option.when(open || semiOpen)(List(fileToken(endpoint.file))).getOrElse(Nil)
        val facts =
          List(
            Option.when(open)(s"open_file_${fileToken(endpoint.file)}"),
            Option.when(semiOpen)(s"semi_open_file_${fileToken(endpoint.file)}"),
            Option.when(seventh)("seventh_rank_entry")
          ).flatten
        val zone =
          if seventh then Some("back rank")
          else zoneFromFileToken(fileToken(endpoint.file))
        (files, zone, facts)
      }
    }

  def taggedOutpostSquaresFor(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Set[String] =
    semantic.positionalFeatures.collect {
      case PositionalTag.Outpost(square, color) if matchesSide(color, side) => square.key
    }.toSet

  def occupiedStrongKnightSquaresFor(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Set[String] =
    semantic.positionalFeatures.collect {
      case PositionalTag.StrongKnight(square, color)
          if matchesSide(color, side) &&
            semantic.board.exists(board =>
              hasPiece(board, sideColor(side), square, Knight) || hasPiece(board, sideColor(side), square, Bishop)
            ) =>
        square.key
    }.toSet

  def hasCompensationLinePlanSupport(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    !hasConversionPlanPressure(side, semantic) &&
      topPlansFor(side, semantic).take(4).exists(plan =>
        plan.plan.id == PlanId.OpeningDevelopment ||
          plan.plan.id == PlanId.PieceActivation ||
          plan.plan.id == PlanId.RookActivation ||
          plan.plan.id == PlanId.FileControl ||
          plan.plan.id == PlanId.WeakPawnAttack
      )

  def hasDelayedRecoveryCompensationPlan(
      side: String,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    hasCompensationLinePlanSupport(side, semantic) &&
      topPlansFor(side, semantic).take(3).exists(plan =>
        plan.plan.id == PlanId.OpeningDevelopment ||
          plan.plan.id == PlanId.PieceActivation ||
          plan.plan.id == PlanId.RookActivation ||
          plan.plan.id == PlanId.FileControl
      )

  def hasCompensationLineAccess(
      side: String,
      pack: StrategyPack,
      semantic: StrategicIdeaSemanticContext
  ): Boolean =
    pack.pieceRoutes.exists(route =>
      route.ownerSide == side &&
        route.surfaceMode != RouteSurfaceMode.Hidden &&
        isMajorPiece(route.piece) &&
        route.route.lastOption.flatMap(squareFromKey).flatMap(endpoint => lineAccessFacts(side, endpoint, semantic)).nonEmpty
    ) ||
      pack.directionalTargets.exists(target =>
        target.ownerSide == side &&
          isMajorPiece(target.piece) &&
          squareFromKey(target.targetSquare).flatMap(endpoint => lineAccessFacts(side, endpoint, semantic)).nonEmpty
      )


  def spaceDiffFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.centralSpace.spaceDiff else -features.centralSpace.spaceDiff

  def bishopCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.imbalance.whiteBishops else features.imbalance.blackBishops

  def knightCountFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.imbalance.whiteKnights else features.imbalance.blackKnights


  def openFilesCount(features: PositionFeatures): Int = features.lineControl.openFilesCount

  def semiOpenFilesFor(side: String, features: PositionFeatures): Int =
    if side == "white" then features.lineControl.whiteSemiOpenFiles else features.lineControl.blackSemiOpenFiles

  def entrenchedPiecesFor(side: String, state: StrategicStateFeatures): Int =
    if side == "white" then state.whiteEntrenchedPieces else state.blackEntrenchedPieces

  def isMajorPiece(piece: String): Boolean =
    piece == "R" || piece == "Q"

  def isMinorPiece(piece: String): Boolean =
    piece == "N" || piece == "B"

  def isSeventhRankFor(side: String, square: Square): Boolean =
    if side == "white" then square.rank == Rank.Seventh else square.rank == Rank.Second

  def isOpenFile(board: Board, file: File): Boolean =
    (board.pawns & _root_.chess.Bitboard.file(file)).isEmpty

  def isSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = _root_.chess.Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty
