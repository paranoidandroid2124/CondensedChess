package lila.commentary.analysis

import _root_.chess.{ Bishop, Bitboard, Board, Color, King, Knight, Move, Pawn, Piece, Position, Queen, Role, Rook, Square }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard

import lila.commentary.analysis.tactical.TacticalPatternDetectors
import lila.commentary.analysis.structure.WeaknessTargetProfile
import lila.commentary.StrategyRelationSupport
import lila.commentary.model.ProbeResult
import lila.commentary.model.strategic.VariationLine

private[commentary] object MoveReviewExchangeAnalyzer:

  final case class BoundedReplayStep(
      uci: String,
      before: Position,
      move: Move,
      after: Position,
      capturedRole: Option[Role]
  )

  final case class DefenderTradeBranch(
      defenderSquare: String,
      exchangeSquare: String,
      targetSquare: String,
      lineMoves: List[String]
  )

  final case class BadPieceLiquidationBranch(
      badPieceSquare: String,
      exchangeSquare: String,
      lineMoves: List[String]
  )

  final case class RelationWitness(
      kind: String,
      focusSquares: List[String],
      facts: List[String],
      lineMoves: List[String],
      targetSquare: Option[String] = None,
      details: RelationDetails = RelationDetails.Empty
  )

  final case class RelationProjection(
      kind: String,
      focusSquares: List[String],
      targetSquare: Option[String],
      factTerms: List[String],
      lineMoves: List[String],
      support: Option[StrategyRelationSupport] = None
  )

  final case class RelationWitnessInput(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil,
      continuationLines: List[List[String]] = Nil
  )

  final case class RelationWitnessTemplate(
      kind: String,
      witnessHook: String,
      extract: RelationWitnessInput => Option[RelationWitness]
  )

  sealed trait RelationDetails
  object RelationDetails:
    case object Empty extends RelationDetails
    final case class DefenderTrade(
        defenderSquare: String,
        exchangeSquare: String,
        targetSquare: String
    ) extends RelationDetails
    final case class BadPieceLiquidation(
        badPieceSquare: String,
        exchangeSquare: String
    ) extends RelationDetails
    final case class Overload(
        defenderSquare: String,
        targetSquares: List[String],
        attackerSquare: String
    ) extends RelationDetails
    final case class Deflection(
        defenderSquare: String,
        targetSquare: String,
        attackerSquare: String
    ) extends RelationDetails
    final case class DiscoveredAttack(
        attackerSquare: String,
        clearedSquare: String,
        targetSquare: String,
        attackerRole: String
    ) extends RelationDetails
    final case class DoubleCheck(
        kingSquare: String,
        checkerSquares: List[String],
        moverSquare: String,
        moverRole: String
    ) extends RelationDetails
    final case class MatePattern(
        relationKind: String,
        kingSquare: String,
        checkerSquares: List[String],
        matingMove: String,
        patternId: Option[String]
    ) extends RelationDetails
    final case class GreekGift(
        bishopSquare: String,
        targetSquare: String,
        entryMove: String,
        patternId: String
    ) extends RelationDetails
    final case class TargetPiece(square: String, role: String)
    final case class Fork(
        attackerSquare: String,
        attackerRole: String,
        targets: List[TargetPiece]
    ) extends RelationDetails
    final case class HangingPiece(
        attackerSquare: String,
        targetSquare: String,
        attackerRole: String,
        targetRole: String
    ) extends RelationDetails
    final case class TrappedPiece(
        targetSquare: String,
        targetRole: String,
        attackerSquares: List[String],
        legalEscapeCount: Int
    ) extends RelationDetails
    final case class Domination(
        controllerSquare: String,
        targetSquare: String,
        controllerRole: String,
        targetRole: String,
        legalMoveCount: Int
    ) extends RelationDetails
    final case class StalemateTrap(
        kingSquare: String,
        trappingMove: String
    ) extends RelationDetails
    final case class Zwischenzug(
        intermediateMove: String,
        threatType: String,
        responseMove: String,
        payoffMove: String,
        targetSquare: String
    ) extends RelationDetails
    final case class PerpetualCheck(
        kingSquare: String,
        checkingMoves: List[String],
        cycleMoves: List[String],
        repeatedPositionPly: Int
    ) extends RelationDetails
    final case class Decoy(
        baitFromSquare: String,
        baitSquare: String,
        luredFromSquare: String,
        executionFromSquare: String,
        executionToSquare: String,
        baitRole: String,
        luredRole: String
    ) extends RelationDetails
    final case class XRay(
        attackerSquare: String,
        blockerSquare: String,
        targetSquare: String,
        attackerRole: String,
        blockerRole: String,
        targetRole: String
    ) extends RelationDetails
    final case class Clearance(
        beneficiarySquare: String,
        clearedSquare: String,
        targetSquare: String,
        beneficiaryRole: String,
        clearingTo: String
    ) extends RelationDetails
    final case class Battery(
        frontSquare: String,
        backSquare: String,
        targetSquare: String,
        frontRole: String,
        backRole: String,
        axis: String
    ) extends RelationDetails
    final case class Interference(
        blockerSquare: String,
        defenderSquare: String,
        targetSquare: String,
        blockerRole: String,
        defenderRole: String,
        targetRole: String
    ) extends RelationDetails
    final case class Pin(
        attackerSquare: String,
        pinnedSquare: String,
        behindSquare: String,
        targetSquare: String,
        attackerRole: String,
        pinnedRole: String,
        behindRole: String,
        absolute: Boolean
    ) extends RelationDetails
    final case class Skewer(
        attackerSquare: String,
        frontSquare: String,
        backSquare: String,
        targetSquare: String,
        attackerRole: String,
        frontRole: String,
        backRole: String
    ) extends RelationDetails

  object RelationKind:
    val DefenderTrade = "defender_trade"
    val BadPieceLiquidation = "bad_piece_liquidation"
    val Overload = "overload"
    val Deflection = "deflection"
    val DiscoveredAttack = "discovered_attack"
    val DoubleCheck = "double_check"
    val BackRankMate = "back_rank_mate"
    val MateNet = "mate_net"
    val Fork = "fork"
    val HangingPiece = "hanging_piece"
    val Decoy = "decoy"
    val Interference = "interference"
    val Clearance = "clearance"
    val XRay = "xray"
    val Battery = "battery"
    val Pin = "pin"
    val Skewer = "skewer"
    val Zwischenzug = "zwischenzug"
    val Domination = "domination"
    val TrappedPiece = "trapped_piece"
    val GreekGift = "greek_gift"
    val StalemateTrap = "stalemate_trap"
    val PerpetualCheck = "perpetual_check"

    val Implemented: List[String] =
      List(
        DefenderTrade,
        BadPieceLiquidation,
        Overload,
        Deflection,
        DiscoveredAttack,
        DoubleCheck,
        BackRankMate,
        MateNet,
        GreekGift,
        Zwischenzug,
        Fork,
        HangingPiece,
        TrappedPiece,
        Domination,
        StalemateTrap,
        PerpetualCheck,
        XRay,
        Clearance,
        Battery,
        Pin,
        Skewer,
        Interference,
        Decoy
      )

    val Deferred: List[String] =
      Nil

    val All: List[String] =
      Implemented ++ Deferred

  def normalizedTopUciMoves(variations: List[VariationLine]): List[String] =
    variations.headOption.toList.flatMap(normalizedLineMoves)

  def normalizedLineMoves(line: VariationLine): List[String] =
    val rawUciMoves =
      line.moves.map(NarrativeUtils.normalizeUciMove).filter(isUciMove)
    if rawUciMoves.nonEmpty then rawUciMoves
    else
      line.parsedMoves
        .flatMap(move => clean(move.uci))
        .map(NarrativeUtils.normalizeUciMove)
        .filter(isUciMove)

  def boundedTopReplay(
      fen: String,
      variations: List[VariationLine],
      maxPlies: Int
  ): Option[List[BoundedReplayStep]] =
    boundedReplay(fen, normalizedTopUciMoves(variations), maxPlies)

  def boundedTopReplayPrefix(
      fen: String,
      variations: List[VariationLine],
      minPlies: Int,
      maxPlies: Int
  ): Option[List[BoundedReplayStep]] =
    boundedReplayPrefix(fen, normalizedTopUciMoves(variations), minPlies, maxPlies)

  def boundedReplay(
      fen: String,
      moves: List[String],
      maxPlies: Int
  ): Option[List[BoundedReplayStep]] =
    val normalizedMoves = normalizedBoundedMoves(moves, maxPlies)
    replayLegalPrefix(fen, normalizedMoves).filter(_.size == normalizedMoves.size)

  def boundedReplayPrefix(
      fen: String,
      moves: List[String],
      minPlies: Int,
      maxPlies: Int
  ): Option[List[BoundedReplayStep]] =
    val normalizedMoves = normalizedBoundedMoves(moves, maxPlies)
    Option
      .when(minPlies > 0 && maxPlies >= minPlies && normalizedMoves.size >= minPlies)(normalizedMoves)
      .flatMap(replayLegalPrefix(fen, _))
      .filter(_.size >= minPlies)

  private def replayLegalPrefix(
      fen: String,
      normalizedMoves: List[String]
  ): Option[List[BoundedReplayStep]] =
    Option.when(normalizedMoves.nonEmpty)(()).flatMap { _ =>
      val startOpt = Fen.read(Standard, Fen.Full(fen))

      startOpt.flatMap { start =>
        val accepted = scala.collection.mutable.ListBuffer.empty[BoundedReplayStep]
        var current = start
        var ok = true
        val it = normalizedMoves.iterator
        while it.hasNext && ok do
          val uci = it.next()
          legalMove(current, uci) match
            case Some(move) =>
              val capturedRole =
                move.capture
                  .flatMap(current.board.roleAt)
                  .orElse(current.board.roleAt(move.dest))
              accepted += BoundedReplayStep(
                uci = uci,
                before = current,
                move = move,
                after = move.after,
                capturedRole = capturedRole
              )
              current = move.after
            case None =>
              ok = false
        Option.when(ok || accepted.nonEmpty)(accepted.toList)
      }
    }

  private def normalizedBoundedMoves(moves: List[String], maxPlies: Int): List[String] =
    moves.map(NarrativeUtils.normalizeUciMove).filter(isUciMove).take(maxPlies)

  def defenderTradeBranch(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[DefenderTradeBranch] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      defenderStep <- replay.lift(1)
      recaptureStep <- replay.lift(2)
      exchangeSquare = first.move.dest
      defenderSquare = defenderStep.move.orig
      if defenderStep.move.dest == exchangeSquare
      if recaptureStep.move.dest == exchangeSquare
      if defenderStep.move.captures && recaptureStep.move.captures
      defender <- defenderStep.before.board.pieceAt(defenderSquare)
      movingSide = first.move.piece.color
      if defender.color != movingSide
      target <- defenderTradeTargetSquare(
        board = first.before.board,
        movingSide = movingSide,
        defenderSquare = defenderSquare,
        exchangeSquare = exchangeSquare,
        explicitTargets = explicitTargets
      )
      if defenseRelationRemoved(
        before = first.before.board,
        after = recaptureStep.after.board,
        defenderColor = defender.color,
        defenderSquare = defenderSquare,
        targetSquare = target
      )
    yield DefenderTradeBranch(
      defenderSquare = defenderSquare.key,
      exchangeSquare = exchangeSquare.key,
      targetSquare = target,
      lineMoves = replay.take(3).map(_.uci)
    )

  def relationWitnesses(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil,
      continuationLines: List[List[String]] = Nil
  ): List[RelationWitness] =
    val input = RelationWitnessInput(
      replay = replay,
      playedMove = playedMove,
      explicitTargets = explicitTargets,
      continuationLines = continuationLines
    )
    ImplementedRelationWitnessTemplates.flatMap(_.extract(input))

  val ImplementedRelationWitnessTemplates: List[RelationWitnessTemplate] =
    List(
      RelationWitnessTemplate(
        kind = RelationKind.DefenderTrade,
        witnessHook = "defenderTradeBranch",
        extract = input => defenderTradeBranch(input.replay, input.playedMove, input.explicitTargets).map(defenderTradeWitness)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.BadPieceLiquidation,
        witnessHook = "badPieceLiquidationBranch",
        extract = input => badPieceLiquidationBranch(input.replay, input.playedMove).map(badPieceLiquidationWitness)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Overload,
        witnessHook = "overloadWitness",
        extract = input => overloadWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Deflection,
        witnessHook = "deflectionWitness",
        extract = input => deflectionWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.DiscoveredAttack,
        witnessHook = "discoveredAttackWitness",
        extract = input => discoveredAttackWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.DoubleCheck,
        witnessHook = "doubleCheckWitness",
        extract = input => doubleCheckWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.BackRankMate,
        witnessHook = "backRankMateWitness",
        extract = input => backRankMateWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.MateNet,
        witnessHook = "mateNetWitness",
        extract = input => mateNetWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.GreekGift,
        witnessHook = "greekGiftWitness",
        extract = input => greekGiftWitness(input.replay, input.playedMove, input.explicitTargets, input.continuationLines)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Zwischenzug,
        witnessHook = "zwischenzugWitness",
        extract = input => zwischenzugWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Fork,
        witnessHook = "forkWitness",
        extract = input => forkWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.HangingPiece,
        witnessHook = "hangingPieceWitness",
        extract = input => hangingPieceWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.TrappedPiece,
        witnessHook = "trappedPieceWitness",
        extract = input => trappedPieceWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Domination,
        witnessHook = "dominationWitness",
        extract = input => dominationWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.StalemateTrap,
        witnessHook = "stalemateTrapWitness",
        extract = input => stalemateTrapWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.PerpetualCheck,
        witnessHook = "perpetualCheckWitness",
        extract = input => perpetualCheckWitness(input.replay, input.playedMove, input.explicitTargets, input.continuationLines)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.XRay,
        witnessHook = "xrayWitness",
        extract = input => xrayWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Clearance,
        witnessHook = "clearanceWitness",
        extract = input => clearanceWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Battery,
        witnessHook = "batteryWitness",
        extract = input => batteryWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Pin,
        witnessHook = "pinWitness",
        extract = input => pinWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Skewer,
        witnessHook = "skewerWitness",
        extract = input => skewerWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Interference,
        witnessHook = "interferenceWitness",
        extract = input => interferenceWitness(input.replay, input.playedMove, input.explicitTargets)
      ),
      RelationWitnessTemplate(
        kind = RelationKind.Decoy,
        witnessHook = "decoyWitness",
        extract = input => decoyWitness(input.replay, input.playedMove, input.explicitTargets)
      )
    )

  def implementedRelationWitnessKinds: Set[String] =
    ImplementedRelationWitnessTemplates.map(_.kind).toSet

  def relationWitnessTemplateForKind(kind: String): Option[RelationWitnessTemplate] =
    ImplementedRelationWitnessTemplates.find(_.kind == kind)

  def defenderTradeRelationWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    defenderTradeBranch(replay, playedMove, explicitTargets).map(defenderTradeWitness)

  def badPieceLiquidationRelationWitness(
      replay: List[BoundedReplayStep],
      playedMove: String
  ): Option[RelationWitness] =
    badPieceLiquidationBranch(replay, playedMove).map(badPieceLiquidationWitness)

  def typedDetailsFromWitness(witness: RelationWitness): Option[RelationDetails] =
    witness.details match
      case RelationDetails.Empty => None
      case details: RelationDetails.DefenderTrade if witness.kind == RelationKind.DefenderTrade => Some(details)
      case details: RelationDetails.BadPieceLiquidation if witness.kind == RelationKind.BadPieceLiquidation => Some(details)
      case details: RelationDetails.Overload if witness.kind == RelationKind.Overload => Some(details)
      case details: RelationDetails.Deflection if witness.kind == RelationKind.Deflection => Some(details)
      case details: RelationDetails.DiscoveredAttack if witness.kind == RelationKind.DiscoveredAttack => Some(details)
      case details: RelationDetails.DoubleCheck if witness.kind == RelationKind.DoubleCheck => Some(details)
      case details: RelationDetails.MatePattern
          if (witness.kind == RelationKind.BackRankMate || witness.kind == RelationKind.MateNet) &&
            details.relationKind == witness.kind =>
        Some(details)
      case details: RelationDetails.GreekGift if witness.kind == RelationKind.GreekGift => Some(details)
      case details: RelationDetails.Fork if witness.kind == RelationKind.Fork => Some(details)
      case details: RelationDetails.HangingPiece if witness.kind == RelationKind.HangingPiece => Some(details)
      case details: RelationDetails.TrappedPiece if witness.kind == RelationKind.TrappedPiece => Some(details)
      case details: RelationDetails.Domination if witness.kind == RelationKind.Domination => Some(details)
      case details: RelationDetails.StalemateTrap if witness.kind == RelationKind.StalemateTrap => Some(details)
      case details: RelationDetails.Zwischenzug if witness.kind == RelationKind.Zwischenzug => Some(details)
      case details: RelationDetails.PerpetualCheck if witness.kind == RelationKind.PerpetualCheck => Some(details)
      case details: RelationDetails.Decoy if witness.kind == RelationKind.Decoy => Some(details)
      case details: RelationDetails.XRay if witness.kind == RelationKind.XRay => Some(details)
      case details: RelationDetails.Clearance if witness.kind == RelationKind.Clearance => Some(details)
      case details: RelationDetails.Battery if witness.kind == RelationKind.Battery => Some(details)
      case details: RelationDetails.Interference if witness.kind == RelationKind.Interference => Some(details)
      case details: RelationDetails.Pin if witness.kind == RelationKind.Pin => Some(details)
      case details: RelationDetails.Skewer if witness.kind == RelationKind.Skewer => Some(details)
      case _ => None

  def relationDetailsValidForKind(witness: RelationWitness): Boolean =
    witness.details == RelationDetails.Empty || typedDetailsFromWitness(witness).nonEmpty

  def relationProjectionFromWitness(witness: RelationWitness): Option[RelationProjection] =
    Option.when(RelationKind.Implemented.contains(witness.kind) && relationDetailsValidForKind(witness)) {
      RelationProjection(
        kind = witness.kind,
        focusSquares = relationFocusSquaresFromWitness(witness),
        targetSquare = relationTargetSquareFromWitness(witness),
        factTerms = relationFactTermsFromWitness(witness),
        lineMoves = witness.lineMoves,
        support = relationSupportFromWitness(witness)
      )
    }

  def relationSupportFromWitness(witness: RelationWitness): Option[StrategyRelationSupport] =
    val baseFocus = relationFocusSquaresFromWitness(witness)
    val baseTarget = relationTargetSquareFromWitness(witness)
    def support(
        focusSquares: List[String] = baseFocus,
        targetSquare: Option[String] = baseTarget,
        targetSquares: List[String] = Nil,
        targetRoles: List[String] = Nil,
        targetRole: Option[String] = None,
        attackerSquare: Option[String] = None,
        attackerSquares: List[String] = Nil,
        attackerRole: Option[String] = None,
        defenderSquare: Option[String] = None,
        defenderRole: Option[String] = None,
        controllerSquare: Option[String] = None,
        controllerRole: Option[String] = None,
        moverSquare: Option[String] = None,
        moverRole: Option[String] = None,
        exchangeSquare: Option[String] = None,
        badPieceSquare: Option[String] = None,
        clearedSquare: Option[String] = None,
        beneficiarySquare: Option[String] = None,
        beneficiaryRole: Option[String] = None,
        clearingTo: Option[String] = None,
        blockerSquare: Option[String] = None,
        blockerRole: Option[String] = None,
        pinnedSquare: Option[String] = None,
        pinnedRole: Option[String] = None,
        behindSquare: Option[String] = None,
        behindRole: Option[String] = None,
        frontSquare: Option[String] = None,
        frontRole: Option[String] = None,
        backSquare: Option[String] = None,
        backRole: Option[String] = None,
        kingSquare: Option[String] = None,
        checkerSquares: List[String] = Nil,
        matingMove: Option[String] = None,
        patternId: Option[String] = None,
        bishopSquare: Option[String] = None,
        entryMove: Option[String] = None,
        intermediateMove: Option[String] = None,
        threatType: Option[String] = None,
        responseMove: Option[String] = None,
        payoffMove: Option[String] = None,
        checkingMoves: List[String] = Nil,
        cycleMoves: List[String] = Nil,
        repeatedPositionPly: Option[Int] = None,
        trappingMove: Option[String] = None,
        baitFromSquare: Option[String] = None,
        baitSquare: Option[String] = None,
        baitRole: Option[String] = None,
        luredFromSquare: Option[String] = None,
        luredRole: Option[String] = None,
        executionFromSquare: Option[String] = None,
        executionToSquare: Option[String] = None,
        legalEscapeCount: Option[Int] = None,
        legalMoveCount: Option[Int] = None,
        absolutePin: Option[Boolean] = None,
        axis: Option[String] = None
    ): StrategyRelationSupport =
      StrategyRelationSupport(
        relationKind = witness.kind,
        focusSquares = focusSquares,
        targetSquare = targetSquare,
        targetSquares = targetSquares.flatMap(square => squareFromKey(square).map(_.key)).distinct,
        targetRoles = targetRoles.flatMap(pieceRoleName),
        targetRole = targetRole,
        attackerSquare = attackerSquare,
        attackerSquares = attackerSquares.flatMap(square => squareFromKey(square).map(_.key)).distinct,
        attackerRole = attackerRole,
        defenderSquare = defenderSquare,
        defenderRole = defenderRole,
        controllerSquare = controllerSquare,
        controllerRole = controllerRole,
        moverSquare = moverSquare,
        moverRole = moverRole,
        exchangeSquare = exchangeSquare,
        badPieceSquare = badPieceSquare,
        clearedSquare = clearedSquare,
        beneficiarySquare = beneficiarySquare,
        beneficiaryRole = beneficiaryRole,
        clearingTo = clearingTo,
        blockerSquare = blockerSquare,
        blockerRole = blockerRole,
        pinnedSquare = pinnedSquare,
        pinnedRole = pinnedRole,
        behindSquare = behindSquare,
        behindRole = behindRole,
        frontSquare = frontSquare,
        frontRole = frontRole,
        backSquare = backSquare,
        backRole = backRole,
        kingSquare = kingSquare,
        checkerSquares = checkerSquares.flatMap(square => squareFromKey(square).map(_.key)).distinct,
        matingMove = matingMove,
        patternId = patternId,
        bishopSquare = bishopSquare,
        entryMove = entryMove,
        intermediateMove = intermediateMove,
        threatType = threatType,
        responseMove = responseMove,
        payoffMove = payoffMove,
        checkingMoves = checkingMoves,
        cycleMoves = cycleMoves,
        repeatedPositionPly = repeatedPositionPly,
        trappingMove = trappingMove,
        baitFromSquare = baitFromSquare,
        baitSquare = baitSquare,
        baitRole = baitRole,
        luredFromSquare = luredFromSquare,
        luredRole = luredRole,
        executionFromSquare = executionFromSquare,
        executionToSquare = executionToSquare,
        legalEscapeCount = legalEscapeCount,
        legalMoveCount = legalMoveCount,
        absolutePin = absolutePin,
        axis = axis,
        lineMoves = witness.lineMoves
      )

    typedDetailsFromWitness(witness).map {
      case RelationDetails.Empty =>
        support()
      case details: RelationDetails.DefenderTrade =>
        support(
          defenderSquare = Some(details.defenderSquare),
          exchangeSquare = Some(details.exchangeSquare),
          targetSquare = Some(details.targetSquare)
        )
      case details: RelationDetails.BadPieceLiquidation =>
        support(
          badPieceSquare = Some(details.badPieceSquare),
          exchangeSquare = Some(details.exchangeSquare),
          targetSquare = Some(details.exchangeSquare)
        )
      case details: RelationDetails.Overload =>
        support(
          defenderSquare = Some(details.defenderSquare),
          targetSquares = details.targetSquares,
          attackerSquare = Some(details.attackerSquare)
        )
      case details: RelationDetails.Deflection =>
        support(
          defenderSquare = Some(details.defenderSquare),
          targetSquare = Some(details.targetSquare),
          attackerSquare = Some(details.attackerSquare)
        )
      case details: RelationDetails.DiscoveredAttack =>
        support(
          attackerSquare = Some(details.attackerSquare),
          clearedSquare = Some(details.clearedSquare),
          targetSquare = Some(details.targetSquare),
          attackerRole = Some(details.attackerRole)
        )
      case details: RelationDetails.DoubleCheck =>
        support(
          kingSquare = Some(details.kingSquare),
          checkerSquares = details.checkerSquares,
          moverSquare = Some(details.moverSquare),
          moverRole = Some(details.moverRole),
          attackerSquare = Some(details.moverSquare),
          attackerRole = Some(details.moverRole),
          targetSquare = Some(details.kingSquare)
        )
      case details: RelationDetails.MatePattern =>
        support(
          kingSquare = Some(details.kingSquare),
          checkerSquares = details.checkerSquares,
          matingMove = Some(details.matingMove),
          patternId = details.patternId,
          targetSquare = Some(details.kingSquare)
        )
      case details: RelationDetails.GreekGift =>
        support(
          bishopSquare = Some(details.bishopSquare),
          targetSquare = Some(details.targetSquare),
          entryMove = Some(details.entryMove),
          patternId = Some(details.patternId)
        )
      case details: RelationDetails.Fork =>
        support(
          attackerSquare = Some(details.attackerSquare),
          attackerRole = Some(details.attackerRole),
          targetSquares = details.targets.map(_.square),
          targetRoles = details.targets.map(_.role)
        )
      case details: RelationDetails.HangingPiece =>
        support(
          attackerSquare = Some(details.attackerSquare),
          targetSquare = Some(details.targetSquare),
          attackerRole = Some(details.attackerRole),
          targetRole = Some(details.targetRole)
        )
      case details: RelationDetails.TrappedPiece =>
        support(
          targetSquare = Some(details.targetSquare),
          targetRole = Some(details.targetRole),
          attackerSquares = details.attackerSquares,
          legalEscapeCount = Some(details.legalEscapeCount)
        )
      case details: RelationDetails.Domination =>
        support(
          controllerSquare = Some(details.controllerSquare),
          targetSquare = Some(details.targetSquare),
          controllerRole = Some(details.controllerRole),
          targetRole = Some(details.targetRole),
          legalMoveCount = Some(details.legalMoveCount)
        )
      case details: RelationDetails.StalemateTrap =>
        support(
          kingSquare = Some(details.kingSquare),
          trappingMove = Some(details.trappingMove),
          targetSquare = Some(details.kingSquare)
        )
      case details: RelationDetails.Zwischenzug =>
        support(
          intermediateMove = Some(details.intermediateMove),
          threatType = Some(details.threatType),
          responseMove = Some(details.responseMove),
          payoffMove = Some(details.payoffMove),
          targetSquare = Some(details.targetSquare)
        )
      case details: RelationDetails.PerpetualCheck =>
        support(
          kingSquare = Some(details.kingSquare),
          checkingMoves = details.checkingMoves,
          cycleMoves = details.cycleMoves,
          repeatedPositionPly = Some(details.repeatedPositionPly),
          targetSquare = Some(details.kingSquare)
        )
      case details: RelationDetails.Decoy =>
        support(
          baitFromSquare = Some(details.baitFromSquare),
          baitSquare = Some(details.baitSquare),
          baitRole = Some(details.baitRole),
          luredFromSquare = Some(details.luredFromSquare),
          luredRole = Some(details.luredRole),
          executionFromSquare = Some(details.executionFromSquare),
          executionToSquare = Some(details.executionToSquare)
        )
      case details: RelationDetails.XRay =>
        support(
          attackerSquare = Some(details.attackerSquare),
          blockerSquare = Some(details.blockerSquare),
          targetSquare = Some(details.targetSquare),
          attackerRole = Some(details.attackerRole),
          blockerRole = Some(details.blockerRole),
          targetRole = Some(details.targetRole)
        )
      case details: RelationDetails.Clearance =>
        support(
          beneficiarySquare = Some(details.beneficiarySquare),
          clearedSquare = Some(details.clearedSquare),
          targetSquare = Some(details.targetSquare),
          beneficiaryRole = Some(details.beneficiaryRole),
          clearingTo = Some(details.clearingTo)
        )
      case details: RelationDetails.Battery =>
        support(
          frontSquare = Some(details.frontSquare),
          backSquare = Some(details.backSquare),
          targetSquare = Some(details.targetSquare),
          frontRole = Some(details.frontRole),
          backRole = Some(details.backRole),
          axis = Some(details.axis)
        )
      case details: RelationDetails.Interference =>
        support(
          blockerSquare = Some(details.blockerSquare),
          defenderSquare = Some(details.defenderSquare),
          targetSquare = Some(details.targetSquare),
          blockerRole = Some(details.blockerRole),
          defenderRole = Some(details.defenderRole),
          targetRole = Some(details.targetRole)
        )
      case details: RelationDetails.Pin =>
        support(
          attackerSquare = Some(details.attackerSquare),
          pinnedSquare = Some(details.pinnedSquare),
          behindSquare = Some(details.behindSquare),
          targetSquare = Some(details.targetSquare),
          attackerRole = Some(details.attackerRole),
          pinnedRole = Some(details.pinnedRole),
          behindRole = Some(details.behindRole),
          absolutePin = Some(details.absolute)
        )
      case details: RelationDetails.Skewer =>
        support(
          attackerSquare = Some(details.attackerSquare),
          frontSquare = Some(details.frontSquare),
          backSquare = Some(details.backSquare),
          targetSquare = Some(details.targetSquare),
          attackerRole = Some(details.attackerRole),
          frontRole = Some(details.frontRole),
          backRole = Some(details.backRole)
        )
    }

  def defenderTradeBranchFromWitness(witness: RelationWitness): Option[DefenderTradeBranch] =
    typedDetailsFromWitness(witness).collect {
      case details: RelationDetails.DefenderTrade =>
        DefenderTradeBranch(
          defenderSquare = details.defenderSquare,
          exchangeSquare = details.exchangeSquare,
          targetSquare = details.targetSquare,
          lineMoves = witness.lineMoves
        )
    }

  def badPieceLiquidationBranchFromWitness(witness: RelationWitness): Option[BadPieceLiquidationBranch] =
    typedDetailsFromWitness(witness).collect {
      case details: RelationDetails.BadPieceLiquidation =>
        BadPieceLiquidationBranch(
          badPieceSquare = details.badPieceSquare,
          exchangeSquare = details.exchangeSquare,
          lineMoves = witness.lineMoves
        )
    }

  def overloadDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Overload] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Overload => details }

  def deflectionDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Deflection] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Deflection => details }

  def discoveredAttackDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.DiscoveredAttack] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.DiscoveredAttack => details }

  def relationTransformationProofFromWitness(witness: RelationWitness): Option[PlayerFacingExactSliceProof] =
    typedDetailsFromWitness(witness).flatMap {
      case details: RelationDetails.Overload =>
        Some(
          PlayerFacingExactSliceProof.Overload(
            defenderSquare = details.defenderSquare,
            targetSquares = details.targetSquares,
            attackerSquare = details.attackerSquare
          )
        )
      case details: RelationDetails.Deflection =>
        Some(
          PlayerFacingExactSliceProof.Deflection(
            defenderSquare = details.defenderSquare,
            targetSquare = details.targetSquare,
            attackerSquare = details.attackerSquare
          )
        )
      case details: RelationDetails.DiscoveredAttack =>
        Some(
          PlayerFacingExactSliceProof.DiscoveredAttack(
            attackerSquare = details.attackerSquare,
            clearedSquare = details.clearedSquare,
            targetSquare = details.targetSquare,
            attackerRole = details.attackerRole
          )
        )
      case details: RelationDetails.DoubleCheck =>
        Some(
          PlayerFacingExactSliceProof.DoubleCheck(
            kingSquare = details.kingSquare,
            checkerSquares = details.checkerSquares,
            moverSquare = details.moverSquare,
            moverRole = details.moverRole
          )
        )
      case details: RelationDetails.MatePattern if details.relationKind == RelationKind.BackRankMate =>
        Some(
          PlayerFacingExactSliceProof.BackRankMate(
            kingSquare = details.kingSquare,
            checkerSquares = details.checkerSquares,
            matingMove = details.matingMove
          )
        )
      case details: RelationDetails.MatePattern if details.relationKind == RelationKind.MateNet =>
        Some(
          PlayerFacingExactSliceProof.MateNet(
            kingSquare = details.kingSquare,
            checkerSquares = details.checkerSquares,
            matingMove = details.matingMove,
            patternId = details.patternId
          )
        )
      case details: RelationDetails.GreekGift =>
        Some(
          PlayerFacingExactSliceProof.GreekGift(
            bishopSquare = details.bishopSquare,
            targetSquare = details.targetSquare,
            entryMove = details.entryMove,
            patternId = details.patternId
          )
        )
      case details: RelationDetails.Fork =>
        Some(
          PlayerFacingExactSliceProof.Fork(
            attackerSquare = details.attackerSquare,
            attackerRole = details.attackerRole,
            targets =
              details.targets.map(target =>
                PlayerFacingExactSliceProof.TargetPiece(square = target.square, role = target.role)
              )
          )
        )
      case details: RelationDetails.HangingPiece =>
        Some(
          PlayerFacingExactSliceProof.HangingPiece(
            attackerSquare = details.attackerSquare,
            targetSquare = details.targetSquare,
            attackerRole = details.attackerRole,
            targetRole = details.targetRole
          )
        )
      case details: RelationDetails.TrappedPiece =>
        Some(
          PlayerFacingExactSliceProof.TrappedPiece(
            targetSquare = details.targetSquare,
            targetRole = details.targetRole,
            attackerSquares = details.attackerSquares,
            legalEscapeCount = details.legalEscapeCount
          )
        )
      case details: RelationDetails.Domination =>
        Some(
          PlayerFacingExactSliceProof.Domination(
            controllerSquare = details.controllerSquare,
            targetSquare = details.targetSquare,
            controllerRole = details.controllerRole,
            targetRole = details.targetRole,
            legalMoveCount = details.legalMoveCount
          )
        )
      case details: RelationDetails.StalemateTrap =>
        Some(
          PlayerFacingExactSliceProof.StalemateTrap(
            kingSquare = details.kingSquare,
            trappingMove = details.trappingMove
          )
        )
      case details: RelationDetails.PerpetualCheck =>
        Some(
          PlayerFacingExactSliceProof.PerpetualCheck(
            kingSquare = details.kingSquare,
            checkingMoves = details.checkingMoves,
            cycleMoves = details.cycleMoves,
            repeatedPositionPly = details.repeatedPositionPly
          )
        )
      case details: RelationDetails.Zwischenzug =>
        Some(
          PlayerFacingExactSliceProof.Zwischenzug(
            intermediateMove = details.intermediateMove,
            threatType = details.threatType,
            responseMove = details.responseMove,
            payoffMove = details.payoffMove,
            targetSquare = details.targetSquare
          )
        )
      case details: RelationDetails.Decoy =>
        Some(
          PlayerFacingExactSliceProof.Decoy(
            baitFromSquare = details.baitFromSquare,
            baitSquare = details.baitSquare,
            luredFromSquare = details.luredFromSquare,
            executionFromSquare = details.executionFromSquare,
            executionToSquare = details.executionToSquare,
            baitRole = details.baitRole,
            luredRole = details.luredRole
          )
        )
      case details: RelationDetails.XRay =>
        Some(
          PlayerFacingExactSliceProof.XRay(
            attackerSquare = details.attackerSquare,
            blockerSquare = details.blockerSquare,
            targetSquare = details.targetSquare,
            attackerRole = details.attackerRole,
            blockerRole = details.blockerRole,
            targetRole = details.targetRole
          )
        )
      case details: RelationDetails.Clearance =>
        Some(
          PlayerFacingExactSliceProof.Clearance(
            beneficiarySquare = details.beneficiarySquare,
            clearedSquare = details.clearedSquare,
            targetSquare = details.targetSquare,
            beneficiaryRole = details.beneficiaryRole,
            clearingTo = details.clearingTo
          )
        )
      case details: RelationDetails.Battery =>
        Some(
          PlayerFacingExactSliceProof.Battery(
            frontSquare = details.frontSquare,
            backSquare = details.backSquare,
            targetSquare = details.targetSquare,
            frontRole = details.frontRole,
            backRole = details.backRole,
            axis = details.axis
          )
        )
      case details: RelationDetails.Pin =>
        Some(
          PlayerFacingExactSliceProof.Pin(
            attackerSquare = details.attackerSquare,
            pinnedSquare = details.pinnedSquare,
            behindSquare = details.behindSquare,
            targetSquare = details.targetSquare,
            attackerRole = details.attackerRole,
            pinnedRole = details.pinnedRole,
            behindRole = details.behindRole,
            absolute = details.absolute
          )
        )
      case details: RelationDetails.Skewer =>
        Some(
          PlayerFacingExactSliceProof.Skewer(
            attackerSquare = details.attackerSquare,
            frontSquare = details.frontSquare,
            backSquare = details.backSquare,
            targetSquare = details.targetSquare,
            attackerRole = details.attackerRole,
            frontRole = details.frontRole,
            backRole = details.backRole
          )
        )
      case details: RelationDetails.Interference =>
        Some(
          PlayerFacingExactSliceProof.Interference(
            blockerSquare = details.blockerSquare,
            defenderSquare = details.defenderSquare,
            targetSquare = details.targetSquare,
            blockerRole = details.blockerRole,
            defenderRole = details.defenderRole,
            targetRole = details.targetRole
          )
        )
      case _ => None
    }.filter(PlayerFacingExactSliceProofFacts.validShape)

  def relationTransformationProofFromSupport(
      support: StrategyRelationSupport
  ): Option[PlayerFacingExactSliceProof] =
    support.relationKind match
      case RelationKind.Overload =>
        for
          defender <- support.defenderSquare
          attacker <- support.attackerSquare
        yield PlayerFacingExactSliceProof.Overload(
          defenderSquare = defender,
          targetSquares = support.targetSquares,
          attackerSquare = attacker
        )
      case RelationKind.Deflection =>
        for
          defender <- support.defenderSquare
          target <- support.targetSquare
          attacker <- support.attackerSquare
        yield PlayerFacingExactSliceProof.Deflection(
          defenderSquare = defender,
          targetSquare = target,
          attackerSquare = attacker
        )
      case RelationKind.DiscoveredAttack =>
        for
          attacker <- support.attackerSquare
          cleared <- support.clearedSquare
          target <- support.targetSquare
          role <- support.attackerRole
        yield PlayerFacingExactSliceProof.DiscoveredAttack(
          attackerSquare = attacker,
          clearedSquare = cleared,
          targetSquare = target,
          attackerRole = role
        )
      case RelationKind.DoubleCheck =>
        for
          king <- support.kingSquare
          mover <- support.moverSquare
          role <- support.moverRole
        yield PlayerFacingExactSliceProof.DoubleCheck(
          kingSquare = king,
          checkerSquares = support.checkerSquares,
          moverSquare = mover,
          moverRole = role
        )
      case RelationKind.BackRankMate =>
        for
          king <- support.kingSquare
          matingMove <- support.matingMove
        yield PlayerFacingExactSliceProof.BackRankMate(
          kingSquare = king,
          checkerSquares = support.checkerSquares,
          matingMove = matingMove
        )
      case RelationKind.MateNet =>
        for
          king <- support.kingSquare
          matingMove <- support.matingMove
        yield PlayerFacingExactSliceProof.MateNet(
          kingSquare = king,
          checkerSquares = support.checkerSquares,
          matingMove = matingMove,
          patternId = support.patternId
        )
      case RelationKind.GreekGift =>
        for
          bishop <- support.bishopSquare
          target <- support.targetSquare
          entryMove <- support.entryMove
          patternId <- support.patternId
        yield PlayerFacingExactSliceProof.GreekGift(
          bishopSquare = bishop,
          targetSquare = target,
          entryMove = entryMove,
          patternId = patternId
        )
      case RelationKind.Fork =>
        for
          attacker <- support.attackerSquare
          attackerRole <- support.attackerRole
        yield PlayerFacingExactSliceProof.Fork(
          attackerSquare = attacker,
          attackerRole = attackerRole,
          targets =
            support.targetSquares
              .zip(support.targetRoles)
              .map((square, role) => PlayerFacingExactSliceProof.TargetPiece(square = square, role = role))
        )
      case RelationKind.HangingPiece =>
        for
          attacker <- support.attackerSquare
          target <- support.targetSquare
          attackerRole <- support.attackerRole
          targetRole <- support.targetRole
        yield PlayerFacingExactSliceProof.HangingPiece(
          attackerSquare = attacker,
          targetSquare = target,
          attackerRole = attackerRole,
          targetRole = targetRole
        )
      case RelationKind.TrappedPiece =>
        for
          target <- support.targetSquare
          targetRole <- support.targetRole
          legalEscapeCount <- support.legalEscapeCount
        yield PlayerFacingExactSliceProof.TrappedPiece(
          targetSquare = target,
          targetRole = targetRole,
          attackerSquares = support.attackerSquares,
          legalEscapeCount = legalEscapeCount
        )
      case RelationKind.Domination =>
        for
          controller <- support.controllerSquare
          target <- support.targetSquare
          controllerRole <- support.controllerRole
          targetRole <- support.targetRole
          legalMoveCount <- support.legalMoveCount
        yield PlayerFacingExactSliceProof.Domination(
          controllerSquare = controller,
          targetSquare = target,
          controllerRole = controllerRole,
          targetRole = targetRole,
          legalMoveCount = legalMoveCount
        )
      case RelationKind.StalemateTrap =>
        for
          king <- support.kingSquare
          trappingMove <- support.trappingMove
        yield PlayerFacingExactSliceProof.StalemateTrap(
          kingSquare = king,
          trappingMove = trappingMove
        )
      case RelationKind.PerpetualCheck =>
        for
          king <- support.kingSquare
          repeatedPositionPly <- support.repeatedPositionPly
        yield PlayerFacingExactSliceProof.PerpetualCheck(
          kingSquare = king,
          checkingMoves = support.checkingMoves,
          cycleMoves = support.cycleMoves,
          repeatedPositionPly = repeatedPositionPly
        )
      case RelationKind.Zwischenzug =>
        for
          intermediateMove <- support.intermediateMove
          threatType <- support.threatType
          responseMove <- support.responseMove
          payoffMove <- support.payoffMove
          target <- support.targetSquare
        yield PlayerFacingExactSliceProof.Zwischenzug(
          intermediateMove = intermediateMove,
          threatType = threatType,
          responseMove = responseMove,
          payoffMove = payoffMove,
          targetSquare = target
        )
      case RelationKind.Decoy =>
        for
          baitFrom <- support.baitFromSquare
          bait <- support.baitSquare
          luredFrom <- support.luredFromSquare
          executionFrom <- support.executionFromSquare
          executionTo <- support.executionToSquare
          baitRole <- support.baitRole
          luredRole <- support.luredRole
        yield PlayerFacingExactSliceProof.Decoy(
          baitFromSquare = baitFrom,
          baitSquare = bait,
          luredFromSquare = luredFrom,
          executionFromSquare = executionFrom,
          executionToSquare = executionTo,
          baitRole = baitRole,
          luredRole = luredRole
        )
      case RelationKind.XRay =>
        for
          attacker <- support.attackerSquare
          blocker <- support.blockerSquare
          target <- support.targetSquare
          attackerRole <- support.attackerRole
          blockerRole <- support.blockerRole
          targetRole <- support.targetRole
        yield PlayerFacingExactSliceProof.XRay(
          attackerSquare = attacker,
          blockerSquare = blocker,
          targetSquare = target,
          attackerRole = attackerRole,
          blockerRole = blockerRole,
          targetRole = targetRole
        )
      case RelationKind.Clearance =>
        for
          beneficiary <- support.beneficiarySquare
          cleared <- support.clearedSquare
          target <- support.targetSquare
          beneficiaryRole <- support.beneficiaryRole
          clearingTo <- support.clearingTo
        yield PlayerFacingExactSliceProof.Clearance(
          beneficiarySquare = beneficiary,
          clearedSquare = cleared,
          targetSquare = target,
          beneficiaryRole = beneficiaryRole,
          clearingTo = clearingTo
        )
      case RelationKind.Battery =>
        for
          front <- support.frontSquare
          back <- support.backSquare
          target <- support.targetSquare
          frontRole <- support.frontRole
          backRole <- support.backRole
          axis <- support.axis
        yield PlayerFacingExactSliceProof.Battery(
          frontSquare = front,
          backSquare = back,
          targetSquare = target,
          frontRole = frontRole,
          backRole = backRole,
          axis = axis
        )
      case RelationKind.Pin =>
        for
          attacker <- support.attackerSquare
          pinned <- support.pinnedSquare
          behind <- support.behindSquare
          target <- support.targetSquare
          attackerRole <- support.attackerRole
          pinnedRole <- support.pinnedRole
          behindRole <- support.behindRole
          absolute <- support.absolutePin
        yield PlayerFacingExactSliceProof.Pin(
          attackerSquare = attacker,
          pinnedSquare = pinned,
          behindSquare = behind,
          targetSquare = target,
          attackerRole = attackerRole,
          pinnedRole = pinnedRole,
          behindRole = behindRole,
          absolute = absolute
        )
      case RelationKind.Skewer =>
        for
          attacker <- support.attackerSquare
          front <- support.frontSquare
          back <- support.backSquare
          target <- support.targetSquare
          attackerRole <- support.attackerRole
          frontRole <- support.frontRole
          backRole <- support.backRole
        yield PlayerFacingExactSliceProof.Skewer(
          attackerSquare = attacker,
          frontSquare = front,
          backSquare = back,
          targetSquare = target,
          attackerRole = attackerRole,
          frontRole = frontRole,
          backRole = backRole
        )
      case RelationKind.Interference =>
        for
          blocker <- support.blockerSquare
          defender <- support.defenderSquare
          target <- support.targetSquare
          blockerRole <- support.blockerRole
          defenderRole <- support.defenderRole
          targetRole <- support.targetRole
        yield PlayerFacingExactSliceProof.Interference(
          blockerSquare = blocker,
          defenderSquare = defender,
          targetSquare = target,
          blockerRole = blockerRole,
          defenderRole = defenderRole,
          targetRole = targetRole
        )
      case _ => None

  def sameRelationTransformationProof(
      left: PlayerFacingExactSliceProof,
      right: PlayerFacingExactSliceProof
  ): Boolean =
    (left, right) match
      case (
            PlayerFacingExactSliceProof.Overload(leftDefender, leftTargets, leftAttacker),
            PlayerFacingExactSliceProof.Overload(rightDefender, rightTargets, rightAttacker)
          ) =>
        normalizedProofSquareKey(leftDefender) == normalizedProofSquareKey(rightDefender) &&
          normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofSquareKeys(leftTargets).sorted == normalizedProofSquareKeys(rightTargets).sorted
      case (
            PlayerFacingExactSliceProof.Deflection(leftDefender, leftTarget, leftAttacker),
            PlayerFacingExactSliceProof.Deflection(rightDefender, rightTarget, rightAttacker)
          ) =>
        normalizedProofSquareKey(leftDefender) == normalizedProofSquareKey(rightDefender) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker)
      case (
            PlayerFacingExactSliceProof.DiscoveredAttack(leftAttacker, leftCleared, leftTarget, leftRole),
            PlayerFacingExactSliceProof.DiscoveredAttack(rightAttacker, rightCleared, rightTarget, rightRole)
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
        normalizedProofSquareKey(leftCleared) == normalizedProofSquareKey(rightCleared) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftRole) == normalizedProofToken(rightRole)
      case (
            PlayerFacingExactSliceProof.DoubleCheck(leftKing, leftCheckers, leftMover, leftRole),
            PlayerFacingExactSliceProof.DoubleCheck(rightKing, rightCheckers, rightMover, rightRole)
          ) =>
        normalizedProofSquareKey(leftKing) == normalizedProofSquareKey(rightKing) &&
          normalizedProofSquareKeys(leftCheckers).sorted == normalizedProofSquareKeys(rightCheckers).sorted &&
          normalizedProofSquareKey(leftMover) == normalizedProofSquareKey(rightMover) &&
          normalizedProofToken(leftRole) == normalizedProofToken(rightRole)
      case (
            PlayerFacingExactSliceProof.BackRankMate(leftKing, leftCheckers, leftMatingMove),
            PlayerFacingExactSliceProof.BackRankMate(rightKing, rightCheckers, rightMatingMove)
          ) =>
        normalizedProofSquareKey(leftKing) == normalizedProofSquareKey(rightKing) &&
          normalizedProofSquareKeys(leftCheckers).sorted == normalizedProofSquareKeys(rightCheckers).sorted &&
          normalizedProofToken(leftMatingMove) == normalizedProofToken(rightMatingMove)
      case (
            PlayerFacingExactSliceProof.MateNet(leftKing, leftCheckers, leftMatingMove, leftPatternId),
            PlayerFacingExactSliceProof.MateNet(rightKing, rightCheckers, rightMatingMove, rightPatternId)
          ) =>
        normalizedProofSquareKey(leftKing) == normalizedProofSquareKey(rightKing) &&
          normalizedProofSquareKeys(leftCheckers).sorted == normalizedProofSquareKeys(rightCheckers).sorted &&
          normalizedProofToken(leftMatingMove) == normalizedProofToken(rightMatingMove) &&
          leftPatternId.map(normalizedProofToken) == rightPatternId.map(normalizedProofToken)
      case (
            PlayerFacingExactSliceProof.GreekGift(leftBishop, leftTarget, leftEntryMove, leftPatternId),
            PlayerFacingExactSliceProof.GreekGift(rightBishop, rightTarget, rightEntryMove, rightPatternId)
          ) =>
        normalizedProofSquareKey(leftBishop) == normalizedProofSquareKey(rightBishop) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftEntryMove) == normalizedProofToken(rightEntryMove) &&
          normalizedProofToken(leftPatternId) == normalizedProofToken(rightPatternId)
      case (
            PlayerFacingExactSliceProof.Fork(leftAttacker, leftRole, leftTargets),
            PlayerFacingExactSliceProof.Fork(rightAttacker, rightRole, rightTargets)
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofToken(leftRole) == normalizedProofToken(rightRole) &&
          normalizedProofTargetPieces(leftTargets) == normalizedProofTargetPieces(rightTargets)
      case (
            PlayerFacingExactSliceProof.HangingPiece(leftAttacker, leftTarget, leftAttackerRole, leftTargetRole),
            PlayerFacingExactSliceProof.HangingPiece(rightAttacker, rightTarget, rightAttackerRole, rightTargetRole)
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftAttackerRole) == normalizedProofToken(rightAttackerRole) &&
          normalizedProofToken(leftTargetRole) == normalizedProofToken(rightTargetRole)
      case (
            PlayerFacingExactSliceProof.TrappedPiece(leftTarget, leftRole, leftAttackers, leftEscapeCount),
            PlayerFacingExactSliceProof.TrappedPiece(rightTarget, rightRole, rightAttackers, rightEscapeCount)
          ) =>
        normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftRole) == normalizedProofToken(rightRole) &&
          normalizedProofSquareKeys(leftAttackers).sorted == normalizedProofSquareKeys(rightAttackers).sorted &&
          leftEscapeCount == rightEscapeCount
      case (
            PlayerFacingExactSliceProof.Domination(leftController, leftTarget, leftControllerRole, leftTargetRole, leftMoveCount),
            PlayerFacingExactSliceProof.Domination(rightController, rightTarget, rightControllerRole, rightTargetRole, rightMoveCount)
          ) =>
        normalizedProofSquareKey(leftController) == normalizedProofSquareKey(rightController) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftControllerRole) == normalizedProofToken(rightControllerRole) &&
          normalizedProofToken(leftTargetRole) == normalizedProofToken(rightTargetRole) &&
          leftMoveCount == rightMoveCount
      case (
            PlayerFacingExactSliceProof.StalemateTrap(leftKing, leftTrappingMove),
            PlayerFacingExactSliceProof.StalemateTrap(rightKing, rightTrappingMove)
          ) =>
        normalizedProofSquareKey(leftKing) == normalizedProofSquareKey(rightKing) &&
          normalizedProofToken(leftTrappingMove) == normalizedProofToken(rightTrappingMove)
      case (
            PlayerFacingExactSliceProof.PerpetualCheck(leftKing, leftChecks, leftCycle, leftRepeatPly),
            PlayerFacingExactSliceProof.PerpetualCheck(rightKing, rightChecks, rightCycle, rightRepeatPly)
          ) =>
        normalizedProofSquareKey(leftKing) == normalizedProofSquareKey(rightKing) &&
          leftChecks.map(normalizedProofToken) == rightChecks.map(normalizedProofToken) &&
          leftCycle.map(normalizedProofToken) == rightCycle.map(normalizedProofToken) &&
          leftRepeatPly == rightRepeatPly
      case (
            PlayerFacingExactSliceProof.Zwischenzug(leftIntermediate, leftThreat, leftResponse, leftPayoff, leftTarget),
            PlayerFacingExactSliceProof.Zwischenzug(rightIntermediate, rightThreat, rightResponse, rightPayoff, rightTarget)
          ) =>
        normalizedProofToken(leftIntermediate) == normalizedProofToken(rightIntermediate) &&
          normalizedProofToken(leftThreat) == normalizedProofToken(rightThreat) &&
          normalizedProofToken(leftResponse) == normalizedProofToken(rightResponse) &&
          normalizedProofToken(leftPayoff) == normalizedProofToken(rightPayoff) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget)
      case (
            PlayerFacingExactSliceProof.Decoy(
              leftBaitFrom,
              leftBait,
              leftLuredFrom,
              leftExecutionFrom,
              leftExecutionTo,
              leftBaitRole,
              leftLuredRole
            ),
            PlayerFacingExactSliceProof.Decoy(
              rightBaitFrom,
              rightBait,
              rightLuredFrom,
              rightExecutionFrom,
              rightExecutionTo,
              rightBaitRole,
              rightLuredRole
            )
          ) =>
        normalizedProofSquareKey(leftBaitFrom) == normalizedProofSquareKey(rightBaitFrom) &&
          normalizedProofSquareKey(leftBait) == normalizedProofSquareKey(rightBait) &&
          normalizedProofSquareKey(leftLuredFrom) == normalizedProofSquareKey(rightLuredFrom) &&
          normalizedProofSquareKey(leftExecutionFrom) == normalizedProofSquareKey(rightExecutionFrom) &&
          normalizedProofSquareKey(leftExecutionTo) == normalizedProofSquareKey(rightExecutionTo) &&
          normalizedProofToken(leftBaitRole) == normalizedProofToken(rightBaitRole) &&
          normalizedProofToken(leftLuredRole) == normalizedProofToken(rightLuredRole)
      case (
            PlayerFacingExactSliceProof.XRay(leftAttacker, leftBlocker, leftTarget, leftAttackerRole, leftBlockerRole, leftTargetRole),
            PlayerFacingExactSliceProof.XRay(rightAttacker, rightBlocker, rightTarget, rightAttackerRole, rightBlockerRole, rightTargetRole)
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofSquareKey(leftBlocker) == normalizedProofSquareKey(rightBlocker) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftAttackerRole) == normalizedProofToken(rightAttackerRole) &&
          normalizedProofToken(leftBlockerRole) == normalizedProofToken(rightBlockerRole) &&
          normalizedProofToken(leftTargetRole) == normalizedProofToken(rightTargetRole)
      case (
            PlayerFacingExactSliceProof.Clearance(leftBeneficiary, leftCleared, leftTarget, leftRole, leftClearingTo),
            PlayerFacingExactSliceProof.Clearance(rightBeneficiary, rightCleared, rightTarget, rightRole, rightClearingTo)
          ) =>
        normalizedProofSquareKey(leftBeneficiary) == normalizedProofSquareKey(rightBeneficiary) &&
          normalizedProofSquareKey(leftCleared) == normalizedProofSquareKey(rightCleared) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftRole) == normalizedProofToken(rightRole) &&
          normalizedProofSquareKey(leftClearingTo) == normalizedProofSquareKey(rightClearingTo)
      case (
            PlayerFacingExactSliceProof.Battery(leftFront, leftBack, leftTarget, leftFrontRole, leftBackRole, leftAxis),
            PlayerFacingExactSliceProof.Battery(rightFront, rightBack, rightTarget, rightFrontRole, rightBackRole, rightAxis)
          ) =>
        normalizedProofSquareKey(leftFront) == normalizedProofSquareKey(rightFront) &&
          normalizedProofSquareKey(leftBack) == normalizedProofSquareKey(rightBack) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftFrontRole) == normalizedProofToken(rightFrontRole) &&
          normalizedProofToken(leftBackRole) == normalizedProofToken(rightBackRole) &&
          normalizedProofToken(leftAxis) == normalizedProofToken(rightAxis)
      case (
            PlayerFacingExactSliceProof.Pin(
              leftAttacker,
              leftPinned,
              leftBehind,
              leftTarget,
              leftAttackerRole,
              leftPinnedRole,
              leftBehindRole,
              leftAbsolute
            ),
            PlayerFacingExactSliceProof.Pin(
              rightAttacker,
              rightPinned,
              rightBehind,
              rightTarget,
              rightAttackerRole,
              rightPinnedRole,
              rightBehindRole,
              rightAbsolute
            )
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofSquareKey(leftPinned) == normalizedProofSquareKey(rightPinned) &&
          normalizedProofSquareKey(leftBehind) == normalizedProofSquareKey(rightBehind) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftAttackerRole) == normalizedProofToken(rightAttackerRole) &&
          normalizedProofToken(leftPinnedRole) == normalizedProofToken(rightPinnedRole) &&
          normalizedProofToken(leftBehindRole) == normalizedProofToken(rightBehindRole) &&
          leftAbsolute == rightAbsolute
      case (
            PlayerFacingExactSliceProof.Skewer(leftAttacker, leftFront, leftBack, leftTarget, leftAttackerRole, leftFrontRole, leftBackRole),
            PlayerFacingExactSliceProof.Skewer(rightAttacker, rightFront, rightBack, rightTarget, rightAttackerRole, rightFrontRole, rightBackRole)
          ) =>
        normalizedProofSquareKey(leftAttacker) == normalizedProofSquareKey(rightAttacker) &&
          normalizedProofSquareKey(leftFront) == normalizedProofSquareKey(rightFront) &&
          normalizedProofSquareKey(leftBack) == normalizedProofSquareKey(rightBack) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftAttackerRole) == normalizedProofToken(rightAttackerRole) &&
          normalizedProofToken(leftFrontRole) == normalizedProofToken(rightFrontRole) &&
          normalizedProofToken(leftBackRole) == normalizedProofToken(rightBackRole)
      case (
            PlayerFacingExactSliceProof.Interference(leftBlocker, leftDefender, leftTarget, leftBlockerRole, leftDefenderRole, leftTargetRole),
            PlayerFacingExactSliceProof.Interference(rightBlocker, rightDefender, rightTarget, rightBlockerRole, rightDefenderRole, rightTargetRole)
          ) =>
        normalizedProofSquareKey(leftBlocker) == normalizedProofSquareKey(rightBlocker) &&
          normalizedProofSquareKey(leftDefender) == normalizedProofSquareKey(rightDefender) &&
          normalizedProofSquareKey(leftTarget) == normalizedProofSquareKey(rightTarget) &&
          normalizedProofToken(leftBlockerRole) == normalizedProofToken(rightBlockerRole) &&
          normalizedProofToken(leftDefenderRole) == normalizedProofToken(rightDefenderRole) &&
          normalizedProofToken(leftTargetRole) == normalizedProofToken(rightTargetRole)
      case _ => false

  def relationTransformationProofTerms(
      proof: PlayerFacingExactSliceProof
  ): List[String] =
    proof match
      case PlayerFacingExactSliceProof.Overload(defenderSquare, targetSquares, attackerSquare) =>
        List(
          "overload_relation_branch",
          s"defender:${normalizedProofToken(defenderSquare)}",
          s"attacker:${normalizedProofToken(attackerSquare)}"
        ) ++ normalizedProofSquareKeys(targetSquares).map(square => s"target:$square")
      case PlayerFacingExactSliceProof.Deflection(defenderSquare, targetSquare, attackerSquare) =>
        List(
          "deflection_relation_branch",
          s"defender:${normalizedProofToken(defenderSquare)}",
          s"defended_target:${normalizedProofToken(targetSquare)}",
          s"attacker:${normalizedProofToken(attackerSquare)}"
        )
      case PlayerFacingExactSliceProof.DiscoveredAttack(attackerSquare, clearedSquare, targetSquare, attackerRole) =>
        List(
          "discovered_attack_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"cleared_square:${normalizedProofToken(clearedSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}"
        )
      case PlayerFacingExactSliceProof.DoubleCheck(kingSquare, checkerSquares, moverSquare, moverRole) =>
        List(
          "double_check_relation_branch",
          s"king:${normalizedProofToken(kingSquare)}",
          s"checkers:${normalizedProofSquareKeys(checkerSquares).sorted.mkString("|")}",
          s"mover:${normalizedProofToken(moverSquare)}",
          s"mover_role:${normalizedProofToken(moverRole)}"
        )
      case PlayerFacingExactSliceProof.BackRankMate(kingSquare, checkerSquares, matingMove) =>
        List(
          "back_rank_mate_relation_branch",
          "mate",
          s"king:${normalizedProofToken(kingSquare)}",
          s"checkers:${normalizedProofSquareKeys(checkerSquares).sorted.mkString("|")}",
          s"mating_move:${normalizedProofToken(matingMove)}"
        )
      case PlayerFacingExactSliceProof.MateNet(kingSquare, checkerSquares, matingMove, patternId) =>
        List(
          "mate_net_relation_branch",
          "mate",
          s"king:${normalizedProofToken(kingSquare)}",
          s"checkers:${normalizedProofSquareKeys(checkerSquares).sorted.mkString("|")}",
          s"mating_move:${normalizedProofToken(matingMove)}"
        ) ++ patternId.map(id => s"pattern:${normalizedProofToken(id)}").toList
      case PlayerFacingExactSliceProof.GreekGift(bishopSquare, targetSquare, entryMove, patternId) =>
        List(
          "greek_gift_relation_branch",
          "sacrifice_entry",
          s"bishop:${normalizedProofToken(bishopSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"entry_move:${normalizedProofToken(entryMove)}",
          s"pattern:${normalizedProofToken(patternId)}"
        )
      case PlayerFacingExactSliceProof.Fork(attackerSquare, attackerRole, targets) =>
        List(
          "fork_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}"
        ) ++ normalizedProofTargetPieces(targets).map((square, role) => s"target:$square:$role")
      case PlayerFacingExactSliceProof.HangingPiece(attackerSquare, targetSquare, attackerRole, targetRole) =>
        List(
          "hanging_piece_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}",
          s"target_role:${normalizedProofToken(targetRole)}",
          "undefended_target"
        )
      case PlayerFacingExactSliceProof.TrappedPiece(targetSquare, targetRole, attackerSquares, legalEscapeCount) =>
        List(
          "trapped_piece_relation_branch",
          s"target:${normalizedProofToken(targetSquare)}",
          s"target_role:${normalizedProofToken(targetRole)}",
          s"attackers:${normalizedProofSquareKeys(attackerSquares).sorted.mkString("|")}",
          s"legal_escape_count:$legalEscapeCount",
          "no_safe_escape"
        )
      case PlayerFacingExactSliceProof.Domination(
            controllerSquare,
            targetSquare,
            controllerRole,
            targetRole,
            legalMoveCount
          ) =>
        List(
          "domination_relation_branch",
          s"controller:${normalizedProofToken(controllerSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"controller_role:${normalizedProofToken(controllerRole)}",
          s"target_role:${normalizedProofToken(targetRole)}",
          s"legal_move_count:$legalMoveCount",
          "restricted_target"
        )
      case PlayerFacingExactSliceProof.StalemateTrap(kingSquare, trappingMove) =>
        List(
          "stalemate_trap_relation_branch",
          "stalemate",
          s"king:${normalizedProofToken(kingSquare)}",
          s"trapping_move:${normalizedProofToken(trappingMove)}"
        )
      case PlayerFacingExactSliceProof.PerpetualCheck(kingSquare, checkingMoves, cycleMoves, repeatedPositionPly) =>
        List(
          "perpetual_check_relation_branch",
          "repeated_check_sequence",
          s"king:${normalizedProofToken(kingSquare)}",
          s"checks:${checkingMoves.map(normalizedProofToken).mkString("|")}",
          s"cycle:${cycleMoves.map(normalizedProofToken).mkString("|")}",
          s"repeated_position_ply:$repeatedPositionPly"
        )
      case PlayerFacingExactSliceProof.Zwischenzug(intermediateMove, threatType, responseMove, payoffMove, targetSquare) =>
        List(
          "zwischenzug_relation_branch",
          s"intermediate_move:${normalizedProofToken(intermediateMove)}",
          s"threat_type:${normalizedProofToken(threatType)}",
          s"response_move:${normalizedProofToken(responseMove)}",
          s"payoff_move:${normalizedProofToken(payoffMove)}",
          s"target:${normalizedProofToken(targetSquare)}",
          "forcing_intermediate_move"
        )
      case PlayerFacingExactSliceProof.Decoy(
            baitFromSquare,
            baitSquare,
            luredFromSquare,
            executionFromSquare,
            executionToSquare,
            baitRole,
            luredRole
          ) =>
        List(
          "decoy_relation_branch",
          s"bait_from:${normalizedProofToken(baitFromSquare)}",
          s"bait:${normalizedProofToken(baitSquare)}",
          s"lured_from:${normalizedProofToken(luredFromSquare)}",
          s"execution:${normalizedProofToken(executionFromSquare)}-${normalizedProofToken(executionToSquare)}",
          s"bait_role:${normalizedProofToken(baitRole)}",
          s"lured_role:${normalizedProofToken(luredRole)}"
        )
      case PlayerFacingExactSliceProof.XRay(attackerSquare, blockerSquare, targetSquare, attackerRole, blockerRole, targetRole) =>
        List(
          "xray_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"blocker:${normalizedProofToken(blockerSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}",
          s"blocker_role:${normalizedProofToken(blockerRole)}",
          s"target_role:${normalizedProofToken(targetRole)}"
        )
      case PlayerFacingExactSliceProof.Clearance(beneficiarySquare, clearedSquare, targetSquare, beneficiaryRole, clearingTo) =>
        List(
          "clearance_relation_branch",
          s"beneficiary:${normalizedProofToken(beneficiarySquare)}",
          s"cleared_square:${normalizedProofToken(clearedSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"beneficiary_role:${normalizedProofToken(beneficiaryRole)}",
          s"clearing_to:${normalizedProofToken(clearingTo)}"
        )
      case PlayerFacingExactSliceProof.Battery(frontSquare, backSquare, targetSquare, frontRole, backRole, axis) =>
        List(
          "battery_relation_branch",
          s"front:${normalizedProofToken(frontSquare)}",
          s"back:${normalizedProofToken(backSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"front_role:${normalizedProofToken(frontRole)}",
          s"back_role:${normalizedProofToken(backRole)}",
          s"axis:${normalizedProofToken(axis)}"
        )
      case PlayerFacingExactSliceProof.Pin(
            attackerSquare,
            pinnedSquare,
            behindSquare,
            targetSquare,
            attackerRole,
            pinnedRole,
            behindRole,
            absolute
          ) =>
        List(
          "pin_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"pinned:${normalizedProofToken(pinnedSquare)}",
          s"behind:${normalizedProofToken(behindSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}",
          s"pinned_role:${normalizedProofToken(pinnedRole)}",
          s"behind_role:${normalizedProofToken(behindRole)}",
          if absolute then "absolute_pin" else "relative_pin"
        )
      case PlayerFacingExactSliceProof.Skewer(attackerSquare, frontSquare, backSquare, targetSquare, attackerRole, frontRole, backRole) =>
        List(
          "skewer_relation_branch",
          s"attacker:${normalizedProofToken(attackerSquare)}",
          s"front:${normalizedProofToken(frontSquare)}",
          s"back:${normalizedProofToken(backSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"attacker_role:${normalizedProofToken(attackerRole)}",
          s"front_role:${normalizedProofToken(frontRole)}",
          s"back_role:${normalizedProofToken(backRole)}"
        )
      case PlayerFacingExactSliceProof.Interference(blockerSquare, defenderSquare, targetSquare, blockerRole, defenderRole, targetRole) =>
        List(
          "interference_relation_branch",
          s"blocker:${normalizedProofToken(blockerSquare)}",
          s"defender:${normalizedProofToken(defenderSquare)}",
          s"target:${normalizedProofToken(targetSquare)}",
          s"blocker_role:${normalizedProofToken(blockerRole)}",
          s"defender_role:${normalizedProofToken(defenderRole)}",
          s"target_role:${normalizedProofToken(targetRole)}"
        )
      case _ => Nil

  private def normalizedProofTargetPieces(
      targets: List[PlayerFacingExactSliceProof.TargetPiece]
  ): List[(String, String)] =
    targets
      .flatMap(target =>
        normalizedProofSquareKey(target.square).map(square => square -> normalizedProofToken(target.role))
      )
      .filter((_, role) => role.nonEmpty)
      .distinct
      .sortBy((square, role) => (square, role))

  private def normalizedProofSquareKeys(raw: List[String]): List[String] =
    raw.flatMap(normalizedProofSquareKey).distinct

  private def normalizedProofSquareKey(raw: String): Option[String] =
    squareFromKey(raw).map(_.key)

  private def normalizedProofToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  def doubleCheckDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.DoubleCheck] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.DoubleCheck => details }

  def matePatternDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.MatePattern] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.MatePattern => details }

  def greekGiftDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.GreekGift] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.GreekGift => details }

  def forkDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Fork] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Fork => details }

  def hangingPieceDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.HangingPiece] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.HangingPiece => details }

  def trappedPieceDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.TrappedPiece] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.TrappedPiece => details }

  def dominationDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Domination] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Domination => details }

  def stalemateTrapDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.StalemateTrap] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.StalemateTrap => details }

  def zwischenzugDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Zwischenzug] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Zwischenzug => details }

  def perpetualCheckDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.PerpetualCheck] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.PerpetualCheck => details }

  def decoyDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Decoy] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Decoy => details }

  def xrayDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.XRay] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.XRay => details }

  def clearanceDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Clearance] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Clearance => details }

  def batteryDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Battery] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Battery => details }

  def interferenceDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Interference] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Interference => details }

  def pinDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Pin] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Pin => details }

  def skewerDetailsFromWitness(witness: RelationWitness): Option[RelationDetails.Skewer] =
    typedDetailsFromWitness(witness).collect { case details: RelationDetails.Skewer => details }

  def relationFocusSquaresFromWitness(witness: RelationWitness): List[String] =
    val raw =
      typedDetailsFromWitness(witness).getOrElse(RelationDetails.Empty) match
        case details: RelationDetails.DefenderTrade if witness.kind == RelationKind.DefenderTrade =>
          List(details.targetSquare, details.exchangeSquare)
        case details: RelationDetails.BadPieceLiquidation if witness.kind == RelationKind.BadPieceLiquidation =>
          List(details.badPieceSquare, details.exchangeSquare)
        case details: RelationDetails.Overload if witness.kind == RelationKind.Overload =>
          (details.defenderSquare :: details.targetSquares).take(4)
        case details: RelationDetails.Deflection if witness.kind == RelationKind.Deflection =>
          List(details.targetSquare, details.defenderSquare, details.attackerSquare)
        case details: RelationDetails.DiscoveredAttack if witness.kind == RelationKind.DiscoveredAttack =>
          List(details.attackerSquare, details.clearedSquare, details.targetSquare)
        case details: RelationDetails.DoubleCheck if witness.kind == RelationKind.DoubleCheck =>
          (details.kingSquare :: details.checkerSquares).distinct
        case details: RelationDetails.MatePattern
            if (witness.kind == RelationKind.BackRankMate || witness.kind == RelationKind.MateNet) &&
              details.relationKind == witness.kind =>
          (details.kingSquare :: details.checkerSquares).distinct
        case details: RelationDetails.GreekGift if witness.kind == RelationKind.GreekGift =>
          List(details.bishopSquare, details.targetSquare)
        case details: RelationDetails.Fork if witness.kind == RelationKind.Fork =>
          (details.attackerSquare :: details.targets.map(_.square)).distinct
        case details: RelationDetails.HangingPiece if witness.kind == RelationKind.HangingPiece =>
          List(details.attackerSquare, details.targetSquare)
        case details: RelationDetails.TrappedPiece if witness.kind == RelationKind.TrappedPiece =>
          (details.targetSquare :: details.attackerSquares).distinct
        case details: RelationDetails.Domination if witness.kind == RelationKind.Domination =>
          List(details.targetSquare, details.controllerSquare)
        case details: RelationDetails.StalemateTrap if witness.kind == RelationKind.StalemateTrap =>
          List(details.kingSquare)
        case details: RelationDetails.Zwischenzug if witness.kind == RelationKind.Zwischenzug =>
          List(details.targetSquare, details.intermediateMove.slice(2, 4))
        case details: RelationDetails.PerpetualCheck if witness.kind == RelationKind.PerpetualCheck =>
          details.kingSquare :: details.checkingMoves.flatMap(uci => squareFromKey(uci.takeRight(2)).map(_.key))
        case details: RelationDetails.Decoy if witness.kind == RelationKind.Decoy =>
          List(details.baitFromSquare, details.baitSquare, details.luredFromSquare)
        case details: RelationDetails.XRay if witness.kind == RelationKind.XRay =>
          List(details.attackerSquare, details.blockerSquare, details.targetSquare)
        case details: RelationDetails.Clearance if witness.kind == RelationKind.Clearance =>
          List(details.beneficiarySquare, details.clearedSquare, details.targetSquare)
        case details: RelationDetails.Battery if witness.kind == RelationKind.Battery =>
          List(details.frontSquare, details.backSquare, details.targetSquare)
        case details: RelationDetails.Interference if witness.kind == RelationKind.Interference =>
          List(details.blockerSquare, details.defenderSquare, details.targetSquare)
        case details: RelationDetails.Pin if witness.kind == RelationKind.Pin =>
          List(details.attackerSquare, details.pinnedSquare, details.behindSquare)
        case details: RelationDetails.Skewer if witness.kind == RelationKind.Skewer =>
          List(details.attackerSquare, details.frontSquare, details.backSquare)
        case _ =>
          if witness.details == RelationDetails.Empty then witness.focusSquares else Nil
    raw.flatMap(square => squareFromKey(square).map(_.key)).distinct

  def relationTargetSquareFromWitness(witness: RelationWitness): Option[String] =
    val raw =
      typedDetailsFromWitness(witness).getOrElse(RelationDetails.Empty) match
        case details: RelationDetails.DefenderTrade if witness.kind == RelationKind.DefenderTrade =>
          Some(details.targetSquare)
        case details: RelationDetails.BadPieceLiquidation if witness.kind == RelationKind.BadPieceLiquidation =>
          Some(details.exchangeSquare)
        case details: RelationDetails.Overload if witness.kind == RelationKind.Overload =>
          details.targetSquares.headOption
        case details: RelationDetails.Deflection if witness.kind == RelationKind.Deflection =>
          Some(details.targetSquare)
        case details: RelationDetails.DiscoveredAttack if witness.kind == RelationKind.DiscoveredAttack =>
          Some(details.targetSquare)
        case details: RelationDetails.DoubleCheck if witness.kind == RelationKind.DoubleCheck =>
          Some(details.kingSquare)
        case details: RelationDetails.MatePattern
            if (witness.kind == RelationKind.BackRankMate || witness.kind == RelationKind.MateNet) &&
              details.relationKind == witness.kind =>
          Some(details.kingSquare)
        case details: RelationDetails.GreekGift if witness.kind == RelationKind.GreekGift =>
          Some(details.targetSquare)
        case details: RelationDetails.Fork if witness.kind == RelationKind.Fork =>
          details.targets.headOption.map(_.square)
        case details: RelationDetails.HangingPiece if witness.kind == RelationKind.HangingPiece =>
          Some(details.targetSquare)
        case details: RelationDetails.TrappedPiece if witness.kind == RelationKind.TrappedPiece =>
          Some(details.targetSquare)
        case details: RelationDetails.Domination if witness.kind == RelationKind.Domination =>
          Some(details.targetSquare)
        case details: RelationDetails.StalemateTrap if witness.kind == RelationKind.StalemateTrap =>
          Some(details.kingSquare)
        case details: RelationDetails.Zwischenzug if witness.kind == RelationKind.Zwischenzug =>
          Some(details.targetSquare)
        case details: RelationDetails.PerpetualCheck if witness.kind == RelationKind.PerpetualCheck =>
          Some(details.kingSquare)
        case details: RelationDetails.Decoy if witness.kind == RelationKind.Decoy =>
          Some(details.baitSquare)
        case details: RelationDetails.XRay if witness.kind == RelationKind.XRay =>
          Some(details.targetSquare)
        case details: RelationDetails.Clearance if witness.kind == RelationKind.Clearance =>
          Some(details.targetSquare)
        case details: RelationDetails.Battery if witness.kind == RelationKind.Battery =>
          Some(details.targetSquare)
        case details: RelationDetails.Interference if witness.kind == RelationKind.Interference =>
          Some(details.targetSquare)
        case details: RelationDetails.Pin if witness.kind == RelationKind.Pin =>
          Some(details.targetSquare)
        case details: RelationDetails.Skewer if witness.kind == RelationKind.Skewer =>
          Some(details.targetSquare)
        case _ =>
          if witness.details == RelationDetails.Empty then witness.targetSquare else None
    raw.flatMap(square => squareFromKey(square).map(_.key))

  def relationFactTermsFromWitness(witness: RelationWitness): List[String] =
    val details =
      typedDetailsFromWitness(witness).getOrElse(RelationDetails.Empty) match
        case details: RelationDetails.DefenderTrade if witness.kind == RelationKind.DefenderTrade =>
          List(
            "defender_trade_branch",
            s"defender:${details.defenderSquare}"
          ) ++ exchangeSquareFact(details.exchangeSquare) ++
            List(s"defended_target:${details.targetSquare}")
        case details: RelationDetails.BadPieceLiquidation if witness.kind == RelationKind.BadPieceLiquidation =>
          List(
            "bad_piece_liquidation_branch",
            s"bad_piece:${details.badPieceSquare}"
          ) ++ exchangeSquareFact(details.exchangeSquare)
        case details: RelationDetails.Overload if witness.kind == RelationKind.Overload =>
          List(
            "overload_relation_witness",
            s"defender:${details.defenderSquare}",
            s"duties:${details.targetSquares.mkString("|")}",
            s"attacker:${details.attackerSquare}"
          )
        case details: RelationDetails.Deflection if witness.kind == RelationKind.Deflection =>
          List(
            "deflection_relation_witness",
            s"defender:${details.defenderSquare}",
            s"defended_target:${details.targetSquare}",
            s"attacker:${details.attackerSquare}"
          )
        case details: RelationDetails.DiscoveredAttack if witness.kind == RelationKind.DiscoveredAttack =>
          List(
            "discovered_attack_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"cleared_square:${details.clearedSquare}",
            s"target:${details.targetSquare}",
            s"attacker_role:${details.attackerRole}"
          )
        case details: RelationDetails.DoubleCheck if witness.kind == RelationKind.DoubleCheck =>
          List(
            "double_check_relation_witness",
            s"king:${details.kingSquare}",
            s"checkers:${details.checkerSquares.sorted.mkString("|")}",
            s"mover:${details.moverSquare}",
            s"mover_role:${details.moverRole}"
          )
        case details: RelationDetails.MatePattern if details.relationKind == RelationKind.BackRankMate && witness.kind == RelationKind.BackRankMate =>
          List(
            "back_rank_mate_relation_witness",
            "mate",
            s"king:${details.kingSquare}",
            s"checkers:${details.checkerSquares.sorted.mkString("|")}",
            s"mating_move:${details.matingMove}"
          )
        case details: RelationDetails.MatePattern if details.relationKind == RelationKind.MateNet && witness.kind == RelationKind.MateNet =>
          List(
            "mate_net_relation_witness",
            "mate",
            s"pattern:${details.patternId.getOrElse(RelationKind.MateNet)}",
            s"king:${details.kingSquare}",
            s"checkers:${details.checkerSquares.sorted.mkString("|")}",
            s"mating_move:${details.matingMove}"
          )
        case details: RelationDetails.GreekGift if witness.kind == RelationKind.GreekGift =>
          List(
            "greek_gift_relation_witness",
            "sacrifice_entry",
            s"pattern:${details.patternId}",
            s"target:${details.targetSquare}",
            s"bishop:${details.bishopSquare}",
            s"entry_move:${details.entryMove}"
          )
        case details: RelationDetails.Fork if witness.kind == RelationKind.Fork =>
          List(
            "fork_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"attacker_role:${details.attackerRole}",
            s"targets:${details.targets.map(_.square).sorted.mkString("|")}"
          ) ++ details.targets.map(target => s"target:${target.square}:${target.role}")
        case details: RelationDetails.HangingPiece if witness.kind == RelationKind.HangingPiece =>
          List(
            "hanging_piece_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"target:${details.targetSquare}",
            s"attacker_role:${details.attackerRole}",
            s"target_role:${details.targetRole}",
            "undefended_target"
          )
        case details: RelationDetails.TrappedPiece if witness.kind == RelationKind.TrappedPiece =>
          List(
            "trapped_piece_relation_witness",
            s"target:${details.targetSquare}",
            s"target_role:${details.targetRole}",
            s"attackers:${details.attackerSquares.sorted.mkString("|")}",
            s"legal_escape_count:${details.legalEscapeCount}",
            "no_safe_escape"
          )
        case details: RelationDetails.Domination if witness.kind == RelationKind.Domination =>
          List(
            "domination_relation_witness",
            s"controller:${details.controllerSquare}",
            s"controller_role:${details.controllerRole}",
            s"target:${details.targetSquare}",
            s"target_role:${details.targetRole}",
            s"legal_move_count:${details.legalMoveCount}",
            "restricted_target"
          )
        case details: RelationDetails.StalemateTrap if witness.kind == RelationKind.StalemateTrap =>
          List(
            "stalemate_trap_relation_witness",
            "stalemate",
            s"king:${details.kingSquare}",
            s"trapping_move:${details.trappingMove}"
          )
        case details: RelationDetails.Zwischenzug if witness.kind == RelationKind.Zwischenzug =>
          List(
            "zwischenzug_relation_witness",
            s"intermediate_move:${details.intermediateMove}",
            s"threat_type:${details.threatType}",
            s"response_move:${details.responseMove}",
            s"payoff_move:${details.payoffMove}",
            s"target:${details.targetSquare}",
            "forcing_intermediate_move"
          )
        case details: RelationDetails.PerpetualCheck if witness.kind == RelationKind.PerpetualCheck =>
          List(
            "perpetual_check_relation_witness",
            "repeated_check_sequence",
            s"king:${details.kingSquare}",
            s"checks:${details.checkingMoves.mkString("|")}",
            s"cycle:${details.cycleMoves.mkString("|")}",
            s"repeated_position_ply:${details.repeatedPositionPly}"
          )
        case details: RelationDetails.Decoy if witness.kind == RelationKind.Decoy =>
          List(
            "decoy_relation_witness",
            s"bait:${details.baitSquare}",
            s"lured_from:${details.luredFromSquare}",
            s"lured_role:${details.luredRole}",
            s"bait_role:${details.baitRole}",
            s"execution:${details.executionFromSquare}-${details.executionToSquare}"
          )
        case details: RelationDetails.XRay if witness.kind == RelationKind.XRay =>
          List(
            "xray_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"blocker:${details.blockerSquare}",
            s"target:${details.targetSquare}",
            s"attacker_role:${details.attackerRole}",
            s"blocker_role:${details.blockerRole}",
            s"target_role:${details.targetRole}"
          )
        case details: RelationDetails.Clearance if witness.kind == RelationKind.Clearance =>
          List(
            "clearance_relation_witness",
            s"beneficiary:${details.beneficiarySquare}",
            s"cleared_square:${details.clearedSquare}",
            s"target:${details.targetSquare}",
            s"beneficiary_role:${details.beneficiaryRole}",
            s"clearing_to:${details.clearingTo}"
          )
        case details: RelationDetails.Battery if witness.kind == RelationKind.Battery =>
          List(
            "battery_relation_witness",
            s"front:${details.frontSquare}",
            s"back:${details.backSquare}",
            s"target:${details.targetSquare}",
            s"front_role:${details.frontRole}",
            s"back_role:${details.backRole}",
            s"axis:${details.axis}"
          )
        case details: RelationDetails.Interference if witness.kind == RelationKind.Interference =>
          List(
            "interference_relation_witness",
            s"blocker:${details.blockerSquare}",
            s"defender:${details.defenderSquare}",
            s"target:${details.targetSquare}",
            s"blocker_role:${details.blockerRole}",
            s"defender_role:${details.defenderRole}",
            s"target_role:${details.targetRole}"
          )
        case details: RelationDetails.Pin if witness.kind == RelationKind.Pin =>
          List(
            "pin_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"pinned:${details.pinnedSquare}",
            s"behind:${details.behindSquare}",
            s"attacker_role:${details.attackerRole}",
            s"pinned_role:${details.pinnedRole}",
            s"behind_role:${details.behindRole}",
            if details.absolute then "absolute_pin" else "relative_pin"
          )
        case details: RelationDetails.Skewer if witness.kind == RelationKind.Skewer =>
          List(
            "skewer_relation_witness",
            s"attacker:${details.attackerSquare}",
            s"front:${details.frontSquare}",
            s"back:${details.backSquare}",
            s"attacker_role:${details.attackerRole}",
            s"front_role:${details.frontRole}",
            s"back_role:${details.backRole}"
          )
        case _ =>
          if witness.details == RelationDetails.Empty then witness.facts else Nil
    val branchTerms =
      if relationDetailsValidForKind(witness) then branchFactFromMoves(witness.lineMoves)
      else Nil
    (details ++ branchTerms).distinct

  def relationOwnerPacketTermsFromSupport(support: StrategyRelationSupport): List[String] =
    val detailTerms =
      support.relationKind match
        case RelationKind.DefenderTrade =>
          List("defender_trade_branch") ++
            relationSquareTerm("defender", support.defenderSquare) ++
            support.exchangeSquare.toList.flatMap(exchangeSquareFact) ++
            relationSquareTerm("defended_target", support.targetSquare)
        case RelationKind.BadPieceLiquidation =>
          List("bad_piece_liquidation_branch") ++
            relationSquareTerm("bad_piece", support.badPieceSquare) ++
            support.exchangeSquare.toList.flatMap(exchangeSquareFact)
        case RelationKind.Overload =>
          List("overload_relation_branch") ++
            relationSquareTerm("defender", support.defenderSquare) ++
            support.targetSquares.flatMap(square => relationSquareTerm("target", Some(square))) ++
            relationSquareTerm("attacker", support.attackerSquare)
        case RelationKind.Deflection =>
          List("deflection_relation_branch") ++
            relationSquareTerm("defender", support.defenderSquare) ++
            relationSquareTerm("defended_target", support.targetSquare) ++
            relationSquareTerm("attacker", support.attackerSquare)
        case RelationKind.DiscoveredAttack =>
          List("discovered_attack_relation_branch") ++
            relationSquareTerm("attacker", support.attackerSquare) ++
            relationSquareTerm("cleared_square", support.clearedSquare) ++
            relationSquareTerm("target", support.targetSquare) ++
            support.attackerRole.toList.map(role => s"attacker_role:${role.trim.toLowerCase}")
        case _ =>
          Nil
    (detailTerms ++ branchFactFromMoves(support.lineMoves)).distinct

  private def relationSquareTerm(prefix: String, square: Option[String]): List[String] =
    square.toList.flatMap(value => squareFromKey(value).map(square => s"$prefix:${square.key}"))

  def ownerSeedTermsFromWitness(
      witness: RelationWitness,
      proofFamily: String,
      aliases: List[String]
  ): List[String] =
    relationProjectionFromWitness(witness)
      .flatMap(projection =>
        projection.support.map { support =>
          (aliases ++ projection.focusSquares ++ relationOwnerPacketTermsFromSupport(support) ++ List(proofFamily)).distinct
        }
      )
      .getOrElse(Nil)

  def transitionTermsFromWitness(
      witness: RelationWitness,
      extras: List[String]
  ): List[String] =
    relationProjectionFromWitness(witness)
      .flatMap(projection =>
        projection.support.map { support =>
          (relationOwnerPacketTermsFromSupport(support) ++ extras ++ MoveReviewPvLine.pvMoveTerms(projection.lineMoves)).distinct
        }
      )
      .getOrElse(Nil)

  def defenderTradeWitness(branch: DefenderTradeBranch): RelationWitness =
    RelationWitness(
      kind = RelationKind.DefenderTrade,
      focusSquares = List(branch.targetSquare, branch.exchangeSquare),
      facts = List(
        "defender_trade_branch",
        s"defender:${branch.defenderSquare}"
      ) ++ exchangeSquareFact(branch.exchangeSquare) ++
        List(s"defended_target:${branch.targetSquare}") ++
        branchFactFromMoves(branch.lineMoves),
      lineMoves = branch.lineMoves,
      targetSquare = Some(branch.targetSquare),
      details = RelationDetails.DefenderTrade(
        defenderSquare = branch.defenderSquare,
        exchangeSquare = branch.exchangeSquare,
        targetSquare = branch.targetSquare
      )
    )

  def badPieceLiquidationWitness(branch: BadPieceLiquidationBranch): RelationWitness =
    RelationWitness(
      kind = RelationKind.BadPieceLiquidation,
      focusSquares = List(branch.badPieceSquare, branch.exchangeSquare),
      facts = List(
        "bad_piece_liquidation_branch",
        s"bad_piece:${branch.badPieceSquare}"
      ) ++ exchangeSquareFact(branch.exchangeSquare) ++
        branchFactFromMoves(branch.lineMoves),
      lineMoves = branch.lineMoves,
      targetSquare = Some(branch.exchangeSquare),
      details = RelationDetails.BadPieceLiquidation(
        badPieceSquare = branch.badPieceSquare,
        exchangeSquare = branch.exchangeSquare
      )
    )

  def overloadWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSquares = relationTargetSquares(first.after.board, movingSide, explicitTargets)
      witness <- overloadedDefender(first.before.board, first.after.board, movingSide, first.move.dest, targetSquares)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def deflectionWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      reply <- replay.lift(1)
      movingSide = first.move.piece.color
      defenderSquare = reply.move.orig
      defender <- first.before.board.pieceAt(defenderSquare)
      if defender.color != movingSide
      if first.after.board.attackers(defenderSquare, movingSide).exists(_ == first.move.dest)
      if reply.move.piece.color == defender.color
      target <- relationTargetSquares(first.before.board, movingSide, explicitTargets)
        .find(target =>
          first.before.board.attackers(target, defender.color).exists(_ == defenderSquare) &&
            !reply.after.board.attackers(target, defender.color).exists(_ == defenderSquare) &&
            reply.after.board.attackers(target, defender.color).count < first.before.board.attackers(target, defender.color).count
        )
    yield
      RelationWitness(
        kind = RelationKind.Deflection,
        focusSquares = List(target.key, defenderSquare.key, first.move.dest.key),
        facts = List(
          "deflection_relation_witness",
          s"defender:${defenderSquare.key}",
          s"defended_target:${target.key}",
          s"attacker:${first.move.dest.key}"
        ) ++ branchFactFromMoves(replay.map(_.uci)),
        lineMoves = replay.take(2).map(_.uci),
        targetSquare = Some(target.key),
        details = RelationDetails.Deflection(
          defenderSquare = defenderSquare.key,
          targetSquare = target.key,
          attackerSquare = first.move.dest.key
        )
      )

  def discoveredAttackWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- discoveredAttackAfterMove(first.before.board, first.after.board, movingSide, first.move.orig, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def xrayWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- xrayAfterMove(first.after.board, movingSide, first.move.dest, first.move.piece.role, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def doubleCheckWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).isEmpty
      movingSide = first.move.piece.color
      king <- first.after.board.kingPosOf(!movingSide)
      checkers = first.after.checkers.squares.toList.map(_.key).sorted
      if first.after.check.yes && checkers.size >= 2
    yield
      RelationWitness(
        kind = RelationKind.DoubleCheck,
        focusSquares = (king.key :: checkers).distinct,
        facts = List(
          "double_check_relation_witness",
          s"king:${king.key}",
          s"checkers:${checkers.mkString("|")}",
          s"mover:${first.move.dest.key}",
          s"mover_role:${first.move.piece.role.name}"
        ),
        lineMoves = replay.take(1).map(_.uci),
        targetSquare = Some(king.key),
        details = RelationDetails.DoubleCheck(
          kingSquare = king.key,
          checkerSquares = checkers,
          moverSquare = first.move.dest.key,
          moverRole = first.move.piece.role.name
        )
      )

  def backRankMateWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).isEmpty
      pattern <- tacticalPattern(RelationKind.BackRankMate)
      if first.after.checkMate
      if pattern.matches(Some(first.before), first.after, first.uci)
      king <- first.after.board.kingPosOf(first.after.color)
      checkers = first.after.checkers.squares.toList.map(_.key).sorted
    yield
      RelationWitness(
        kind = RelationKind.BackRankMate,
        focusSquares = (king.key :: checkers).distinct,
        facts = List(
          "back_rank_mate_relation_witness",
          "mate",
          s"king:${king.key}",
          s"checkers:${checkers.mkString("|")}",
          s"mating_move:${first.uci}"
        ),
        lineMoves = replay.take(1).map(_.uci),
        targetSquare = Some(king.key),
        details = RelationDetails.MatePattern(
          relationKind = RelationKind.BackRankMate,
          kingSquare = king.key,
          checkerSquares = checkers,
          matingMove = first.uci,
          patternId = Some(pattern.id)
        )
      )

  def mateNetWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).isEmpty
      if first.after.checkMate
      pattern <- matePatternExcept(RelationKind.BackRankMate, first)
      king <- first.after.board.kingPosOf(first.after.color)
      checkers = first.after.checkers.squares.toList.map(_.key).sorted
    yield
      RelationWitness(
        kind = RelationKind.MateNet,
        focusSquares = (king.key :: checkers).distinct,
        facts = List(
          "mate_net_relation_witness",
          "mate",
          s"pattern:${pattern.id}",
          s"king:${king.key}",
          s"checkers:${checkers.mkString("|")}",
          s"mating_move:${first.uci}"
        ),
        lineMoves = replay.take(1).map(_.uci),
        targetSquare = Some(king.key),
        details = RelationDetails.MatePattern(
          relationKind = RelationKind.MateNet,
          kingSquare = king.key,
          checkerSquares = checkers,
          matingMove = first.uci,
          patternId = Some(pattern.id)
        )
      )

  def greekGiftWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil,
      continuationLines: List[List[String]] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).isEmpty
      pattern <- tacticalPattern(RelationKind.GreekGift)
      movingSide = first.move.piece.color
      target <- squareFromKey(if movingSide.white then "h7" else "h2")
      if pattern.matchesWithContinuations(Some(first.before), first.after, first.uci, continuationLines)
    yield
      RelationWitness(
        kind = RelationKind.GreekGift,
        focusSquares = List(first.move.dest.key, target.key).distinct,
        facts = List(
          "greek_gift_relation_witness",
          "sacrifice_entry",
          s"pattern:${pattern.id}",
          s"target:${target.key}",
          s"bishop:${first.move.dest.key}",
          s"entry_move:${first.uci}"
        ),
        lineMoves = replay.take(1).map(_.uci),
        targetSquare = Some(target.key),
        details = RelationDetails.GreekGift(
          bishopSquare = first.move.dest.key,
          targetSquare = target.key,
          entryMove = first.uci,
          patternId = pattern.id
        )
      )

  def zwischenzugWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).forall(squareFromKey(_).nonEmpty)
      reply <- replay.lift(1)
      payoff <- replay.lift(2)
      movingSide = first.move.piece.color
      if first.after.check.yes
      if reply.move.piece.color != movingSide
      if payoff.move.piece.color == movingSide
      if payoff.move.captures
      captured <- payoff.capturedRole
      if pieceValue(captured) >= pieceValue(Knight)
      target = payoff.move.dest
      explicit = normalizedExplicitTargetKeys(explicitTargets)
      if explicit.isEmpty || explicit.contains(target.key)
      king <- first.after.board.kingPosOf(!movingSide)
      threatType = if first.after.checkMate then "mate_threat" else "check"
    yield
      RelationWitness(
        kind = RelationKind.Zwischenzug,
        focusSquares = List(king.key, first.move.dest.key, target.key).distinct,
        facts = List(
          "zwischenzug_relation_witness",
          s"intermediate_move:${first.uci}",
          s"threat_type:$threatType",
          s"response_move:${reply.uci}",
          s"payoff_move:${payoff.uci}",
          s"target:${target.key}",
          "forcing_intermediate_move"
        ) ++ branchFactFromMoves(replay.take(3).map(_.uci), maxPlies = 3),
        lineMoves = replay.take(3).map(_.uci),
        targetSquare = Some(target.key),
        details = RelationDetails.Zwischenzug(
          intermediateMove = first.uci,
          threatType = threatType,
          responseMove = reply.uci,
          payoffMove = payoff.uci,
          targetSquare = target.key
        )
      )

  def forkWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- forkAfterMove(first.after.board, movingSide, first.move.dest, first.move.piece.role, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def hangingPieceWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      explicitTargetSet = normalizedExplicitTargetKeys(explicitTargets).flatMap(squareFromKey).toSet
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- hangingPieceAfterMove(
        board = first.after.board,
        movingSide = movingSide,
        attacker = first.move.dest,
        attackerRole = first.move.piece.role,
        targetSet = targetSet,
        explicitTargetSet = explicitTargetSet
      )
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def trappedPieceWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- trappedPieceAfterMove(first, movingSide, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def dominationWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- dominationAfterMove(first.after, movingSide, first.move.dest, first.move.piece.role, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def stalemateTrapWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if first.after.staleMate
      pattern <- tacticalPattern(RelationKind.StalemateTrap)
      if pattern.matches(Some(first.before), first.after, first.uci)
      king <- first.after.board.kingPosOf(first.after.color)
      explicit = normalizedExplicitTargetKeys(explicitTargets)
      if explicit.isEmpty || (explicit.forall(squareFromKey(_).nonEmpty) && explicit.contains(king.key))
    yield
      RelationWitness(
        kind = RelationKind.StalemateTrap,
        focusSquares = List(king.key),
        facts = List(
          "stalemate_trap_relation_witness",
          "stalemate",
          s"king:${king.key}",
          s"trapping_move:${first.uci}"
        ),
        lineMoves = replay.take(1).map(_.uci),
        targetSquare = Some(king.key),
        details = RelationDetails.StalemateTrap(
          kingSquare = king.key,
          trappingMove = first.uci
        )
      )

  def perpetualCheckWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil,
      continuationLines: List[List[String]] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if normalizedExplicitTargetKeys(explicitTargets).isEmpty
      movingSide = first.move.piece.color
      candidate <- perpetualCheckCandidateLines(continuationLines, replay.map(_.uci), normalizedPlayed).iterator
        .flatMap(line => perpetualCheckFromLine(first.before, movingSide, line))
        .toList
        .headOption
    yield candidate

  def clearanceWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- clearanceAfterMove(first.before.board, first.after.board, movingSide, first.move.orig, first.move.dest, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def batteryWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      explicitTargetSet = explicitTargets.flatMap(squareFromKey).toSet
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- batteryAfterMove(
        board = first.after.board,
        movingSide = movingSide,
        movedTo = first.move.dest,
        movedRole = first.move.piece.role,
        targetSet = targetSet,
        explicitTargetSet = explicitTargetSet
      )
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def pinWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- pinAfterMove(first.after.board, movingSide, first.move.dest, first.move.piece.role, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def skewerWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- skewerAfterMove(first.after.board, movingSide, first.move.dest, first.move.piece.role, targetSet)
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def interferenceWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- interferenceAfterMove(
        before = first.before.board,
        after = first.after.board,
        movingSide = movingSide,
        blocker = first.move.dest,
        blockerRole = first.move.piece.role,
        targetSet = targetSet
      )
    yield witness.copy(lineMoves = replay.take(1).map(_.uci))

  def decoyWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      reply <- replay.lift(1)
      response <- replay.lift(2)
      movingSide = first.move.piece.color
      baitSquare = first.move.dest
      if reply.move.piece.color != movingSide
      if response.move.piece.color == movingSide
      if reply.move.dest == baitSquare && reply.move.captures
      if reply.capturedRole.contains(first.move.piece.role)
      if response.move.dest == baitSquare && response.move.captures
      if response.capturedRole.contains(reply.move.piece.role)
      if pieceValue(reply.move.piece.role) > pieceValue(first.move.piece.role)
      targetSet = relationTargetSquares(reply.after.board, movingSide, explicitTargets).toSet
      if targetSet.contains(baitSquare) || targetSet.contains(reply.move.orig)
    yield
      RelationWitness(
        kind = RelationKind.Decoy,
        focusSquares = List(first.move.orig.key, baitSquare.key, reply.move.orig.key),
        facts = List(
          "decoy_relation_witness",
          s"bait:${baitSquare.key}",
          s"lured_from:${reply.move.orig.key}",
          s"lured_role:${reply.move.piece.role.name}",
          s"bait_role:${first.move.piece.role.name}",
          s"execution:${response.move.orig.key}-${response.move.dest.key}"
        ) ++ branchFactFromMoves(replay.map(_.uci), maxPlies = 3),
        lineMoves = replay.take(3).map(_.uci),
        targetSquare = Some(baitSquare.key),
        details = RelationDetails.Decoy(
          baitFromSquare = first.move.orig.key,
          baitSquare = baitSquare.key,
          luredFromSquare = reply.move.orig.key,
          executionFromSquare = response.move.orig.key,
          executionToSquare = response.move.dest.key,
          baitRole = first.move.piece.role.name,
          luredRole = reply.move.piece.role.name
        )
      )

  def badPieceLiquidationBranch(
      replay: List[BoundedReplayStep],
      playedMove: String
  ): Option[BadPieceLiquidationBranch] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      badPieceSquare = first.move.orig
      firstDest = first.move.dest
      piece <- first.before.board.pieceAt(badPieceSquare)
      if piece.color == movingSide && piece.role == Bishop
      if isBadBishopOnCurrentBoard(first.before.board, movingSide, badPieceSquare)
      branch <- immediateBadPieceLiquidation(replay, movingSide, badPieceSquare, firstDest)
        .orElse(sameBranchBadPieceLiquidation(replay, movingSide, badPieceSquare, firstDest))
    yield branch

  def queenTradeShieldLine(replay: List[BoundedReplayStep]): Option[List[String]] =
    replay.zipWithIndex.collectFirst {
      case (step, index)
          if step.move.piece.color == replay.head.before.color &&
            step.move.piece.role == _root_.chess.Queen &&
            step.move.captures &&
            step.capturedRole.contains(_root_.chess.Queen) &&
            replay.lift(index + 1).exists(reply =>
              reply.move.piece.color != step.move.piece.color &&
                reply.move.piece.role == _root_.chess.King &&
                reply.move.dest == step.move.dest &&
                reply.move.captures &&
                reply.capturedRole.contains(_root_.chess.Queen)
            ) =>
        replay.take(index + 2).map(_.uci)
    }

  def immediateExchangeSquare(replay: List[BoundedReplayStep]): Option[String] =
    (replay.headOption, replay.lift(1)) match
      case (Some(first), Some(second))
          if first.move.captures &&
            second.move.captures &&
            first.move.dest == second.move.dest &&
            first.move.piece.color != second.move.piece.color =>
        Some(first.move.dest.key)
      case _ => None

  def branchKey(replay: List[BoundedReplayStep], plies: Int = 2): Option[String] =
    branchKeyFromMoves(replay.map(_.uci), plies)

  def branchKeyFromMoves(moves: List[String], plies: Int = 2): Option[String] =
    val normalized = normalizedBoundedMoves(moves, plies)
    Option.when(plies > 0 && normalized.size == plies)(normalized.mkString("|"))

  def probeStableBranchKey(result: ProbeResult, plies: Int = 2): Option[String] =
    result.variationHash.flatMap(clean).map(_.toLowerCase)
      .orElse(result.seedId.flatMap(clean).map(_.toLowerCase))
      .orElse(probeReplyPrefixKeyFromMoves(result.bestReplyPv, plies))
      .orElse(
        result.replyPvs.toList
          .flatten
          .flatMap(probeReplyPrefixKeyFromMoves(_, plies))
          .headOption
      )

  def probeFullReplyLineKey(result: ProbeResult): Option[String] =
    probeFullReplyLineKeyFromMoves(result.bestReplyPv)
      .orElse {
        result.replyPvs.toList
          .flatten
          .flatMap(probeFullReplyLineKeyFromMoves)
          .headOption
      }

  def probeFullReplyLineMatches(result: ProbeResult, expectedBranchKey: String): Boolean =
    probeFullReplyLineKey(result).contains(expectedBranchKey) ||
      result.replyPvs.toList.flatten.exists(line =>
        probeFullReplyLineKeyFromMoves(line).contains(expectedBranchKey)
      )

  def probeFirstReplyOrMoveKey(result: ProbeResult): Option[String] =
    result.variationHash.flatMap(clean)
      .orElse(result.seedId.flatMap(clean))
      .orElse(probeFirstReplyKeyFromMoves(result.bestReplyPv))
      .orElse(
        result.replyPvs
          .flatMap(_.headOption)
          .flatMap(probeFirstReplyKeyFromMoves)
      )
      .orElse(result.probedMove.flatMap(clean))
      .orElse(result.candidateMove.flatMap(clean))

  def probeHasReplyCoverage(result: ProbeResult): Boolean =
    result.bestReplyPv.nonEmpty ||
      result.replyPvs.exists(_.exists(_.nonEmpty))

  def probeBestReplyHead(result: ProbeResult): Option[String] =
    result.bestReplyPv.headOption.flatMap(clean)

  def probeDistinctReplyHeads(result: ProbeResult): List[String] =
    val replyHeads =
      result.replyPvs.toList
        .flatten
        .flatMap(_.headOption.flatMap(clean))
    val bestReply =
      probeBestReplyHead(result).toList
    (replyHeads ++ bestReply).distinct

  def probeDistinctReplyHeads(results: List[ProbeResult]): List[String] =
    results.flatMap(probeDistinctReplyHeads).distinct

  def probeBestReplyLineDisplay(result: ProbeResult): Option[String] =
    Option.when(result.bestReplyPv.flatMap(clean).nonEmpty)(
      result.bestReplyPv.flatMap(clean).mkString(" ")
    )

  def probeDisplayReplyLines(result: ProbeResult): List[List[String]] =
    result.replyPvs
      .getOrElse(if result.bestReplyPv.nonEmpty then List(result.bestReplyPv) else Nil)
      .filter(_.nonEmpty)

  def probeAllReplyLines(result: ProbeResult): List[List[String]] =
    (result.bestReplyPv :: result.replyPvs.toList.flatten).filter(_.nonEmpty).distinct

  def probeBestReplyLines(result: ProbeResult): List[List[String]] =
    List(result.bestReplyPv).filter(_.nonEmpty)

  def probeBestReplyPrefix(result: ProbeResult, maxPlies: Int): List[String] =
    if maxPlies <= 0 then Nil else result.bestReplyPv.take(maxPlies)

  def probeBestReplyLength(result: ProbeResult): Int =
    result.bestReplyPv.size

  def probeReplyPrefixKeyFromMoves(moves: List[String], plies: Int = 2): Option[String] =
    val normalized = normalizedBoundedMoves(moves, plies)
    Option.when(plies > 0 && normalized.size == plies)(normalized.mkString(" "))

  def probeFullReplyLineKeyFromMoves(moves: List[String]): Option[String] =
    Option.when(moves.nonEmpty)(moves.flatMap(clean).mkString(" "))

  def probeFirstReplyKeyFromMoves(moves: List[String]): Option[String] =
    moves.headOption.flatMap(clean)

  def linePrefixKeyFromMoves(moves: List[String], maxPlies: Int = 2): Option[String] =
    val normalized = normalizedBoundedMoves(moves, maxPlies)
    Option.when(maxPlies > 0 && normalized.nonEmpty)(normalized.mkString("|"))

  def branchFactFromMoves(moves: List[String], maxPlies: Int = 2): List[String] =
    linePrefixKeyFromMoves(moves, maxPlies).map(key => s"branch:$key").toList

  def exchangeSquareFact(square: String): List[String] =
    exchangeSquareTerm(square).toList

  def exchangeSquareTerm(square: String): Option[String] =
    squareFromKey(square)
      .map(_.key)
      .orElse(clean(square).map(_.toLowerCase))
      .map(squareKey => s"exchange_square:$squareKey")

  def bestBranchFactFromKey(key: String): Option[String] =
    clean(key).map(cleanKey => s"best_branch:$cleanKey")

  def bestBranchFactFromMoves(moves: List[String], maxPlies: Int = 2): List[String] =
    linePrefixKeyFromMoves(moves, maxPlies).flatMap(bestBranchFactFromKey).toList

  def replayUcis(replay: List[BoundedReplayStep], fromPly: Int, maxPlies: Int): List[String] =
    replay.drop(fromPly).take(maxPlies).map(_.uci)

  def legalMove(position: Position, uci: String): Option[Move] =
    Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def perpetualCheckCandidateLines(
      continuationLines: List[List[String]],
      fallbackLine: List[String],
      normalizedPlayed: String
  ): List[List[String]] =
    (continuationLines :+ fallbackLine)
      .map(line => normalizedBoundedMoves(line, maxPlies = 8))
      .filter(line => line.headOption.contains(normalizedPlayed) && line.size >= 5)
      .distinct

  private def perpetualCheckFromLine(
      start: Position,
      checkingSide: Color,
      rawLine: List[String]
  ): Option[RelationWitness] =
    val line = normalizedBoundedMoves(rawLine, maxPlies = 8)
    val steps = scala.collection.mutable.ListBuffer.empty[BoundedReplayStep]
    val checkingPositions = scala.collection.mutable.ListBuffer.empty[(Int, String, BoundedReplayStep)]
    var current = start
    var ok = line.size >= 5
    var ply = 0
    val iterator = line.iterator
    while iterator.hasNext && ok do
      val uci = iterator.next()
      ply += 1
      legalMove(current, uci) match
        case Some(move) =>
          val capturedRole =
            move.capture
              .flatMap(current.board.roleAt)
              .orElse(current.board.roleAt(move.dest))
          val step = BoundedReplayStep(
            uci = uci,
            before = current,
            move = move,
            after = move.after,
            capturedRole = capturedRole
          )
          steps += step
          if move.piece.color == checkingSide then
            if step.after.check.yes then
              checkingPositions += ((ply, repeatedPositionKey(step.after), step))
            else ok = false
          current = step.after
        case None =>
          ok = false

    if !ok || checkingPositions.size < 3 then None
    else
      val seen = scala.collection.mutable.Map.empty[String, (Int, BoundedReplayStep)]
      var repeated: Option[(Int, Int, BoundedReplayStep)] = None
      val checkIterator = checkingPositions.iterator
      while checkIterator.hasNext && repeated.isEmpty do
        val (repeatPly, key, step) = checkIterator.next()
        seen.get(key) match
          case Some((firstPly, _)) if repeatPly - firstPly >= 4 =>
            repeated = Some((firstPly, repeatPly, step))
          case Some(_) =>
            ()
          case None =>
            seen += key -> (repeatPly, step)

      repeated.flatMap { case (firstPly, repeatPly, repeatedStep) =>
        val checkingMoves =
          checkingPositions.toList.collect { case (checkPly, _, step) if checkPly <= repeatPly => step.uci }
        val cycleMoves = steps.toList.slice(firstPly - 1, repeatPly).map(_.uci)
        for
          king <- repeatedStep.after.board.kingPosOf(!checkingSide)
          checkers = repeatedStep.after.checkers.squares.toList.map(_.key).sorted
          if checkingMoves.size >= 3 && cycleMoves.size >= 4
        yield
          RelationWitness(
            kind = RelationKind.PerpetualCheck,
            focusSquares = (king.key :: checkers).distinct,
            facts = List(
              "perpetual_check_relation_witness",
              "repeated_check_sequence",
              s"king:${king.key}",
              s"checks:${checkingMoves.mkString("|")}",
              s"cycle:${cycleMoves.mkString("|")}",
              s"repeated_position_ply:$repeatPly"
            ),
            lineMoves = steps.take(repeatPly).map(_.uci).toList,
            targetSquare = Some(king.key),
            details = RelationDetails.PerpetualCheck(
              kingSquare = king.key,
              checkingMoves = checkingMoves,
              cycleMoves = cycleMoves,
              repeatedPositionPly = repeatPly
            )
          )
      }

  private def repeatedPositionKey(position: Position): String =
    Fen.write(position).value.split(" ").take(4).mkString(" ")

  def isUciMove(move: String): Boolean =
    move.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  def squareFromKey(key: String): Option[Square] =
    Square.fromKey(Option(key).map(_.trim.toLowerCase).getOrElse(""))

  private def pieceRoleName(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).flatMap {
      case "p" | "pawn"   => Some("pawn")
      case "n" | "knight" => Some("knight")
      case "b" | "bishop" => Some("bishop")
      case "r" | "rook"   => Some("rook")
      case "q" | "queen"  => Some("queen")
      case "k" | "king"   => Some("king")
      case _               => None
    }

  def isConstrainedBadBishop(board: Board, color: Color, square: Square): Boolean =
    isBadBishopOnCurrentBoard(board, color, square)

  private def defenderTradeTargetSquare(
      board: Board,
      movingSide: Color,
      defenderSquare: Square,
      exchangeSquare: Square,
      explicitTargets: List[String]
  ): Option[String] =
    val explicit = normalizedExplicitTargetKeys(explicitTargets)
    val candidateTargets =
      if explicit.nonEmpty then
        Option.when(explicit.forall(squareFromKey(_).nonEmpty))(explicit).getOrElse(Nil)
      else WeaknessTargetProfile.targetsForPressure(board, movingSide).map(_.targetSquare)
    candidateTargets
      .distinct
      .filterNot(_ == exchangeSquare.key)
      .flatMap(squareFromKey)
      .find(target => board.attackers(target, !movingSide).exists(_ == defenderSquare))
      .map(_.key)

  private def defenseRelationRemoved(
      before: Board,
      after: Board,
      defenderColor: Color,
      defenderSquare: Square,
      targetSquare: String
  ): Boolean =
    squareFromKey(targetSquare).exists { target =>
      val beforeDefenders = before.attackers(target, defenderColor)
      val afterDefenders = after.attackers(target, defenderColor)
      beforeDefenders.exists(_ == defenderSquare) &&
        !afterDefenders.exists(_ == defenderSquare) &&
        afterDefenders.count < beforeDefenders.count
    }

  private def relationTargetSquares(
      board: Board,
      movingSide: Color,
      explicitTargets: List[String]
  ): List[Square] =
    val explicit = normalizedExplicitTargetKeys(explicitTargets)
    if explicit.nonEmpty then
      val parsed = explicit.flatMap(squareFromKey)
      if parsed.size == explicit.size then parsed.distinct else Nil
    else
      val structural =
        WeaknessTargetProfile.targetsForPressure(board, movingSide).flatMap(target => squareFromKey(target.targetSquare))
      val material =
        board.byColor(!movingSide).squares.filterNot(square => board.roleAt(square).contains(King)).toList
      (structural ++ material).distinct

  private def normalizedExplicitTargetKeys(explicitTargets: List[String]): List[String] =
    explicitTargets.map(_.trim.toLowerCase).filter(_.nonEmpty).distinct

  private def overloadedDefender(
      before: Board,
      after: Board,
      movingSide: Color,
      movedTo: Square,
      targets: List[Square]
  ): Option[RelationWitness] =
    val defenderColor = !movingSide
    val dutiesByDefender =
      targets
        .filter(target => after.attackers(target, movingSide).nonEmpty)
        .flatMap { target =>
          val pressureWasCreated =
            !before.attackers(target, movingSide).exists(_ == movedTo) &&
              after.attackers(target, movingSide).exists(_ == movedTo)
          val targetIsPressed =
            pressureWasCreated || after.attackers(target, movingSide).count > before.attackers(target, movingSide).count
          Option.when(targetIsPressed)(after.attackers(target, defenderColor).squares.map(_ -> target)).toList.flatten
        }
        .groupMap(_._1)(_._2)

    dutiesByDefender.collectFirst {
      case (defenderSquare, duties) if duties.distinct.size >= 2 =>
        val dutySquares = duties.distinct.map(_.key).sorted
        RelationWitness(
          kind = RelationKind.Overload,
          focusSquares = (defenderSquare.key :: dutySquares).take(4),
          facts = List(
            "overload_relation_witness",
            s"defender:${defenderSquare.key}",
            s"duties:${dutySquares.mkString("|")}",
            s"attacker:${movedTo.key}"
          ),
          lineMoves = Nil,
          targetSquare = dutySquares.headOption,
          details = RelationDetails.Overload(
            defenderSquare = defenderSquare.key,
            targetSquares = dutySquares,
            attackerSquare = movedTo.key
          )
        )
    }

  private def discoveredAttackAfterMove(
      before: Board,
      after: Board,
      movingSide: Color,
      clearedSquare: Square,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    after.byColor(!movingSide).squares.toList
      .filter(targetSet.contains)
      .filterNot(target => after.roleAt(target).contains(King))
      .flatMap { target =>
        val beforeAttackers = before.attackers(target, movingSide)
        val afterAttackers = after.attackers(target, movingSide)
        (afterAttackers & ~beforeAttackers).squares.toList.flatMap { attacker =>
          after.roleAt(attacker).filter(isLongRangeRole).flatMap { role =>
            Option.when(Bitboard.between(attacker, target).contains(clearedSquare)) {
              RelationWitness(
                kind = RelationKind.DiscoveredAttack,
                focusSquares = List(attacker.key, clearedSquare.key, target.key),
                facts = List(
                  "discovered_attack_relation_witness",
                  s"attacker:${attacker.key}",
                  s"cleared_square:${clearedSquare.key}",
                  s"target:${target.key}",
                  s"attacker_role:${role.name}"
                ),
                lineMoves = Nil,
                targetSquare = Some(target.key),
                details = RelationDetails.DiscoveredAttack(
                  attackerSquare = attacker.key,
                  clearedSquare = clearedSquare.key,
                  targetSquare = target.key,
                  attackerRole = role.name
                )
              )
            }
          }
        }
      }
      .headOption

  private def xrayAfterMove(
      board: Board,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    Option.when(isLongRangeRole(attackerRole))(()).flatMap { _ =>
      rayDirections(attackerRole).flatMap { case (fileStep, rankStep) =>
        for
          blocker <- firstOccupiedOnRay(board, attacker, fileStep, rankStep)
          target <- firstOccupiedOnRay(board, blocker, fileStep, rankStep)
          if targetSet.contains(target)
          blockerPiece <- board.pieceAt(blocker)
          targetPiece <- board.pieceAt(target)
          if blockerPiece.color != movingSide && targetPiece.color != movingSide
          if targetPiece.role != Pawn
        yield
          RelationWitness(
            kind = RelationKind.XRay,
            focusSquares = List(attacker.key, blocker.key, target.key),
            facts = List(
              "xray_relation_witness",
              s"attacker:${attacker.key}",
              s"blocker:${blocker.key}",
              s"target:${target.key}",
              s"attacker_role:${attackerRole.name}",
              s"target_role:${targetPiece.role.name}"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.XRay(
              attackerSquare = attacker.key,
              blockerSquare = blocker.key,
              targetSquare = target.key,
              attackerRole = attackerRole.name,
              blockerRole = blockerPiece.role.name,
              targetRole = targetPiece.role.name
            )
          )
      }.headOption
    }

  private def forkAfterMove(
      board: Board,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    val targets =
      roleAttacks(attackerRole, attacker, movingSide, board.occupied).squares.toList
        .filter(targetSet.contains)
        .flatMap(target =>
          board.pieceAt(target).filter(_.color != movingSide).map(piece => target -> piece.role)
        )
        .filter { case (target, role) =>
          role == King ||
            pieceValue(role) > pieceValue(attackerRole) ||
            board.attackers(target, !movingSide).isEmpty
        }
        .sortBy { case (target, role) => (-pieceValue(role), target.key) }
        .take(4)
    Option.when(targets.size >= 2) {
      val targetSquares = targets.map(_._1.key)
      val targetPieces =
        targets.map { case (target, role) => RelationDetails.TargetPiece(target.key, role.name) }
      RelationWitness(
        kind = RelationKind.Fork,
        focusSquares = (attacker.key :: targetSquares).distinct,
        facts =
          List(
            "fork_relation_witness",
            s"attacker:${attacker.key}",
            s"attacker_role:${attackerRole.name}",
            s"targets:${targetSquares.sorted.mkString("|")}"
          ) ++ targets.map { case (target, role) => s"target:${target.key}:${role.name}" },
        lineMoves = Nil,
        targetSquare = targetSquares.headOption,
        details = RelationDetails.Fork(
          attackerSquare = attacker.key,
          attackerRole = attackerRole.name,
          targets = targetPieces
        )
      )
    }

  private def hangingPieceAfterMove(
      board: Board,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square],
      explicitTargetSet: Set[Square]
  ): Option[RelationWitness] =
    val attackedTargets =
      roleAttacks(attackerRole, attacker, movingSide, board.occupied).squares.toList
        .filter(targetSet.contains)
        .flatMap(target =>
          board.pieceAt(target).filter(_.color != movingSide).map(piece => target -> piece.role)
        )
        .filter { case (target, role) =>
          role != King &&
            board.attackers(target, !movingSide).isEmpty &&
            !sameColorRayPieceBehind(board, attacker, target) &&
            (role != Pawn || explicitTargetSet.contains(target)) &&
            (pieceValue(role) > pieceValue(attackerRole) || explicitTargetSet.contains(target))
        }
        .sortBy { case (target, role) => (-pieceValue(role), target.key) }
    attackedTargets.headOption.map { case (target, role) =>
      RelationWitness(
        kind = RelationKind.HangingPiece,
        focusSquares = List(attacker.key, target.key),
        facts = List(
          "hanging_piece_relation_witness",
          s"attacker:${attacker.key}",
          s"target:${target.key}",
          s"attacker_role:${attackerRole.name}",
          s"target_role:${role.name}",
          "undefended_target"
        ),
        lineMoves = Nil,
        targetSquare = Some(target.key),
        details = RelationDetails.HangingPiece(
          attackerSquare = attacker.key,
          targetSquare = target.key,
          attackerRole = attackerRole.name,
          targetRole = role.name
        )
      )
    }

  private def trappedPieceAfterMove(
      first: BoundedReplayStep,
      movingSide: Color,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    val after = first.after
    val board = after.board
    val movedAttacks =
      roleAttacks(first.move.piece.role, first.move.dest, movingSide, board.occupied).squares.toSet
    targetSet.toList
      .flatMap(target =>
        board
          .pieceAt(target)
          .filter(piece => piece.color != movingSide && trappedPieceTargetRole(piece.role))
          .map(piece => target -> piece)
      )
      .flatMap { case (target, piece) =>
        val attackers = board.attackers(target, movingSide).squares.toList.map(_.key).sorted
        val legalMoves = trappedPieceLegalMoves(after, target, piece)
        val safeRoutes = legalMoves.filter(trappedPieceMovePreservesSafety(_, piece.color, movingSide))
        Option.when(
          attackers.nonEmpty &&
            safeRoutes.isEmpty &&
            trappedPieceMoveLinks(first, target, piece, movedAttacks)
        ) {
          RelationWitness(
            kind = RelationKind.TrappedPiece,
            focusSquares = (target.key :: attackers).distinct,
            facts = List(
              "trapped_piece_relation_witness",
              s"target:${target.key}",
              s"target_role:${piece.role.name}",
              s"attackers:${attackers.mkString("|")}",
              s"legal_escape_count:${legalMoves.size}",
              "no_safe_escape"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.TrappedPiece(
              targetSquare = target.key,
              targetRole = piece.role.name,
              attackerSquares = attackers,
              legalEscapeCount = legalMoves.size
            )
          )
        }
      }
      .sortBy(witness =>
        val value = witness.targetSquare.flatMap(squareFromKey).flatMap(board.roleAt).map(pieceValue).getOrElse(0)
        (-value, witness.targetSquare.getOrElse(""))
      )
      .headOption

  private def trappedPieceTargetRole(role: Role): Boolean =
    role == Knight || role == Bishop || role == Rook || role == Queen

  private def trappedPieceMoveLinks(
      first: BoundedReplayStep,
      target: Square,
      targetPiece: Piece,
      movedAttacks: Set[Square]
  ): Boolean =
    val before = first.before.board
    val after = first.after.board
    val movingSide = first.move.piece.color
    val movedTo = first.move.dest
    val directAttack = after.attackers(target, movingSide).exists(_ == movedTo)
    val pressureIncreased = after.attackers(target, movingSide).count > before.attackers(target, movingSide).count
    val movedIntoTargetRoute =
      trappedPieceCandidateDestinations(before, target, targetPiece).contains(movedTo) ||
        trappedPieceCandidateDestinations(after, target, targetPiece).contains(movedTo)
    val movedControlsRoute =
      trappedPieceCandidateDestinations(after, target, targetPiece).exists(movedAttacks.contains)
    directAttack || pressureIncreased || movedIntoTargetRoute || movedControlsRoute

  private def trappedPieceLegalMoves(
      position: Position,
      target: Square,
      piece: Piece
  ): List[Move] =
    trappedPieceCandidateDestinations(position.board, target, piece).flatMap { destination =>
      legalMove(position, s"${target.key}${destination.key}").filter(move => move.orig == target && move.dest == destination)
    }

  private def trappedPieceCandidateDestinations(
      board: Board,
      target: Square,
      piece: Piece
  ): List[Square] =
    roleAttacks(piece.role, target, piece.color, board.occupied).squares.toList
      .filterNot(destination => board.pieceAt(destination).exists(_.color == piece.color))
      .filterNot(destination => board.pieceAt(destination).exists(_.role == King))

  private def trappedPieceMovePreservesSafety(
      move: Move,
      targetColor: Color,
      attackerColor: Color
  ): Boolean =
    val destination = move.dest
    val board = move.after.board
    val attackers = board.attackers(destination, attackerColor).count
    val defenders = board.attackers(destination, targetColor).count
    attackers == 0 || defenders >= attackers

  private def dominationAfterMove(
      position: Position,
      movingSide: Color,
      controller: Square,
      controllerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    val board = position.board
    roleAttacks(controllerRole, controller, movingSide, board.occupied).squares.toList
      .filter(targetSet.contains)
      .flatMap(target =>
        board
          .pieceAt(target)
          .filter(piece => piece.color != movingSide && dominatedTargetRole(piece.role))
          .map(piece => target -> piece)
      )
      .flatMap { case (target, piece) =>
        val legalMoves = pieceLegalMoves(position, target, piece)
        Option.when(legalMoves.nonEmpty && legalMoves.size <= 1) {
          RelationWitness(
            kind = RelationKind.Domination,
            focusSquares = List(target.key, controller.key),
            facts = List(
              "domination_relation_witness",
              s"controller:${controller.key}",
              s"controller_role:${controllerRole.name}",
              s"target:${target.key}",
              s"target_role:${piece.role.name}",
              s"legal_move_count:${legalMoves.size}",
              "restricted_target"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.Domination(
              controllerSquare = controller.key,
              targetSquare = target.key,
              controllerRole = controllerRole.name,
              targetRole = piece.role.name,
              legalMoveCount = legalMoves.size
            )
          )
        }
      }
      .sortBy(witness =>
        val value = witness.targetSquare.flatMap(squareFromKey).flatMap(board.roleAt).map(pieceValue).getOrElse(0)
        (-value, witness.targetSquare.getOrElse(""))
      )
      .headOption

  private def dominatedTargetRole(role: Role): Boolean =
    role == Knight || role == Bishop || role == Rook || role == Queen

  private def pieceLegalMoves(
      position: Position,
      square: Square,
      piece: Piece
  ): List[Move] =
    pieceCandidateDestinations(position.board, square, piece).flatMap { destination =>
      legalMove(position, s"${square.key}${destination.key}").filter(move => move.orig == square && move.dest == destination)
    }

  private def pieceCandidateDestinations(
      board: Board,
      square: Square,
      piece: Piece
  ): List[Square] =
    roleAttacks(piece.role, square, piece.color, board.occupied).squares.toList
      .filterNot(destination => board.pieceAt(destination).exists(_.color == piece.color))
      .filterNot(destination => board.pieceAt(destination).exists(_.role == King))

  private def sameColorRayPieceBehind(
      board: Board,
      attacker: Square,
      target: Square
  ): Boolean =
    (for
      step <- rayStep(attacker, target)
      targetPiece <- board.pieceAt(target)
      behind <- firstOccupiedOnRay(board, target, step._1, step._2)
      behindPiece <- board.pieceAt(behind)
    yield behindPiece.color == targetPiece.color).getOrElse(false)

  private def rayStep(from: Square, to: Square): Option[(Int, Int)] =
    val fileDiff = to.file.value - from.file.value
    val rankDiff = to.rank.value - from.rank.value
    Option.when(
      fileDiff == 0 ||
        rankDiff == 0 ||
        fileDiff.abs == rankDiff.abs
    ) {
      (Integer.signum(fileDiff), Integer.signum(rankDiff))
    }.filter(_ != (0, 0))

  private def clearanceAfterMove(
      before: Board,
      after: Board,
      movingSide: Color,
      clearedSquare: Square,
      movedTo: Square,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    val friendlyLongRange =
      (after.byColor(movingSide) & (after.bishops | after.rooks | after.queens)).squares.toList.filterNot(_ == movedTo)
    friendlyLongRange.flatMap { beneficiary =>
      after.roleAt(beneficiary).filter(isLongRangeRole).toList.flatMap { role =>
        targetSet.toList.flatMap { target =>
          val beforeAttack = roleAttacks(role, beneficiary, movingSide, before.occupied).contains(target)
          val afterAttack = roleAttacks(role, beneficiary, movingSide, after.occupied).contains(target)
          val clearedRay =
            Bitboard.between(beneficiary, target).contains(clearedSquare) &&
              !Bitboard.between(beneficiary, target).contains(movedTo)
          for
            targetPiece <- after.pieceAt(target).toList
            if targetPiece.color != movingSide && targetPiece.role != Pawn
            if !beforeAttack && afterAttack && clearedRay
          yield
            RelationWitness(
              kind = RelationKind.Clearance,
              focusSquares = List(beneficiary.key, clearedSquare.key, target.key),
              facts = List(
                "clearance_relation_witness",
                s"beneficiary:${beneficiary.key}",
                s"cleared_square:${clearedSquare.key}",
                s"target:${target.key}",
                s"beneficiary_role:${role.name}",
                s"clearing_to:${movedTo.key}"
              ),
              lineMoves = Nil,
              targetSquare = Some(target.key),
              details = RelationDetails.Clearance(
                beneficiarySquare = beneficiary.key,
                clearedSquare = clearedSquare.key,
                targetSquare = target.key,
                beneficiaryRole = role.name,
                clearingTo = movedTo.key
              )
            )
        }
      }
    }.headOption

  private def batteryAfterMove(
      board: Board,
      movingSide: Color,
      movedTo: Square,
      movedRole: Role,
      targetSet: Set[Square],
      explicitTargetSet: Set[Square]
  ): Option[RelationWitness] =
    batteryPartnerRoles(movedRole).flatMap { partnerRole =>
      board.byPiece(movingSide, partnerRole).squares.toList.filterNot(_ == movedTo).flatMap { partner =>
        batteryLine(movedTo, movedRole, partner, partnerRole).toList.flatMap { case (axis, fileStep, rankStep) =>
          val clearBatteryLine =
            !(Bitboard.between(movedTo, partner) & board.occupied).nonEmpty
          Option.when(clearBatteryLine)(()).toList.flatMap { _ =>
            List(
              movedTo -> (-fileStep, -rankStep, partner),
              partner -> (fileStep, rankStep, movedTo)
            ).flatMap { case (front, (targetFileStep, targetRankStep, back)) =>
              for
                target <- firstOccupiedOnRay(board, front, targetFileStep, targetRankStep)
                if targetSet.contains(target)
                targetPiece <- board.pieceAt(target)
                if targetPiece.color != movingSide
                if targetPiece.role != Pawn || explicitTargetSet.contains(target)
                frontRole = board.roleAt(front).map(_.name).getOrElse(movedRole.name)
                backRole = board.roleAt(back).map(_.name).getOrElse(partnerRole.name)
              yield
                RelationWitness(
                  kind = RelationKind.Battery,
                  focusSquares = List(front.key, back.key, target.key),
                  facts = List(
                    "battery_relation_witness",
                    s"front:${front.key}",
                    s"back:${back.key}",
                    s"target:${target.key}",
                    s"front_role:$frontRole",
                    s"back_role:$backRole",
                    s"axis:$axis"
                  ),
                  lineMoves = Nil,
                  targetSquare = Some(target.key),
                  details = RelationDetails.Battery(
                    frontSquare = front.key,
                    backSquare = back.key,
                    targetSquare = target.key,
                    frontRole = frontRole,
                    backRole = backRole,
                    axis = axis
                  )
                )
            }
          }
        }
      }
    }.headOption

  private def interferenceAfterMove(
      before: Board,
      after: Board,
      movingSide: Color,
      blocker: Square,
      blockerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    val defenderColor = !movingSide
    val defenders =
      (before.byColor(defenderColor) & (before.bishops | before.rooks | before.queens)).squares.toList
    defenders.flatMap { defender =>
      before.roleAt(defender).filter(isLongRangeRole).toList.flatMap { defenderRole =>
        targetSet.toList.flatMap { target =>
          val targetPiece = after.pieceAt(target)
          val defendedBefore =
            roleAttacks(defenderRole, defender, defenderColor, before.occupied).contains(target)
          val defendedAfter =
            roleAttacks(defenderRole, defender, defenderColor, after.occupied).contains(target)
          val lineBlocked =
            Bitboard.between(defender, target).contains(blocker) &&
              after.pieceAt(blocker).exists(_.color == movingSide)
          val targetIsPressured =
            after.attackers(target, movingSide).nonEmpty
          for
            piece <- targetPiece.toList
            if piece.color == defenderColor && piece.role != King
            if defender != target && blocker != target
            if defendedBefore && !defendedAfter && lineBlocked && targetIsPressured
          yield
            RelationWitness(
              kind = RelationKind.Interference,
              focusSquares = List(blocker.key, defender.key, target.key),
              facts = List(
                "interference_relation_witness",
                s"blocker:${blocker.key}",
                s"defender:${defender.key}",
                s"target:${target.key}",
                s"blocker_role:${blockerRole.name}",
                s"defender_role:${defenderRole.name}",
                s"target_role:${piece.role.name}"
              ),
              lineMoves = Nil,
              targetSquare = Some(target.key),
              details = RelationDetails.Interference(
                blockerSquare = blocker.key,
                defenderSquare = defender.key,
                targetSquare = target.key,
                blockerRole = blockerRole.name,
                defenderRole = defenderRole.name,
                targetRole = piece.role.name
              )
            )
        }
      }
    }.headOption

  private def pinAfterMove(
      board: Board,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    Option.when(isLongRangeRole(attackerRole))(()).flatMap { _ =>
      rayDirections(attackerRole).flatMap { case (fileStep, rankStep) =>
        for
          pinned <- firstOccupiedOnRay(board, attacker, fileStep, rankStep)
          pinnedPiece <- board.pieceAt(pinned)
          if pinnedPiece.color != movingSide && pinnedPiece.role != King
          behind <- firstOccupiedOnRay(board, pinned, fileStep, rankStep)
          behindPiece <- board.pieceAt(behind)
          if behindPiece.color == pinnedPiece.color
          if behindPiece.role == King || pieceValue(behindPiece.role) > pieceValue(pinnedPiece.role)
          if targetSet.contains(pinned) || targetSet.contains(behind)
          target = if targetSet.contains(pinned) then pinned else behind
        yield
          RelationWitness(
            kind = RelationKind.Pin,
            focusSquares = List(attacker.key, pinned.key, behind.key),
            facts = List(
              "pin_relation_witness",
              s"attacker:${attacker.key}",
              s"pinned:${pinned.key}",
              s"behind:${behind.key}",
              s"attacker_role:${attackerRole.name}",
              s"pinned_role:${pinnedPiece.role.name}",
              s"behind_role:${behindPiece.role.name}",
              Option.when(behindPiece.role == King)("absolute_pin").getOrElse("relative_pin")
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.Pin(
              attackerSquare = attacker.key,
              pinnedSquare = pinned.key,
              behindSquare = behind.key,
              targetSquare = target.key,
              attackerRole = attackerRole.name,
              pinnedRole = pinnedPiece.role.name,
              behindRole = behindPiece.role.name,
              absolute = behindPiece.role == King
            )
          )
      }.headOption
    }

  private def skewerAfterMove(
      board: Board,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    Option.when(isLongRangeRole(attackerRole))(()).flatMap { _ =>
      rayDirections(attackerRole).flatMap { case (fileStep, rankStep) =>
        for
          front <- firstOccupiedOnRay(board, attacker, fileStep, rankStep)
          frontPiece <- board.pieceAt(front)
          if frontPiece.color != movingSide
          back <- firstOccupiedOnRay(board, front, fileStep, rankStep)
          backPiece <- board.pieceAt(back)
          if backPiece.color == frontPiece.color && backPiece.role != Pawn
          if frontPiece.role == King || pieceValue(frontPiece.role) > pieceValue(backPiece.role)
          if targetSet.contains(front) || targetSet.contains(back)
          target = if targetSet.contains(front) then front else back
        yield
          RelationWitness(
            kind = RelationKind.Skewer,
            focusSquares = List(attacker.key, front.key, back.key),
            facts = List(
              "skewer_relation_witness",
              s"attacker:${attacker.key}",
              s"front:${front.key}",
              s"back:${back.key}",
              s"attacker_role:${attackerRole.name}",
              s"front_role:${frontPiece.role.name}",
              s"back_role:${backPiece.role.name}"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.Skewer(
              attackerSquare = attacker.key,
              frontSquare = front.key,
              backSquare = back.key,
              targetSquare = target.key,
              attackerRole = attackerRole.name,
              frontRole = frontPiece.role.name,
              backRole = backPiece.role.name
            )
          )
      }.headOption
    }

  private def isLongRangeRole(role: Role): Boolean =
    role == Bishop || role == Rook || role == Queen

  private def roleAttacks(role: Role, square: Square, color: Color, occupied: Bitboard): Bitboard =
    role match
      case Pawn   => square.pawnAttacks(color)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook   => square.rookAttacks(occupied)
      case Queen  => square.queenAttacks(occupied)
      case King   => square.kingAttacks

  private def tacticalPattern(id: String) =
    TacticalPatternDetectors.ordered.find(_.id == id)

  private def matePatternExcept(
      excludedId: String,
      step: BoundedReplayStep
  ) =
    TacticalPatternDetectors.ordered.find(detector =>
      detector.requiresMate &&
        detector.id != excludedId &&
        detector.matches(Some(step.before), step.after, step.uci)
    )

  private def rayDirections(role: Role): List[(Int, Int)] =
    val bishopDirections = List(1 -> 1, 1 -> -1, -1 -> 1, -1 -> -1)
    val rookDirections = List(1 -> 0, -1 -> 0, 0 -> 1, 0 -> -1)
    role match
      case Bishop => bishopDirections
      case Rook   => rookDirections
      case Queen  => bishopDirections ++ rookDirections
      case _      => Nil

  private def batteryPartnerRoles(role: Role): List[Role] =
    role match
      case Queen  => List(Rook, Bishop)
      case Rook   => List(Queen, Rook)
      case Bishop => List(Queen)
      case _      => Nil

  private def batteryLine(
      movedTo: Square,
      movedRole: Role,
      partner: Square,
      partnerRole: Role
  ): Option[(String, Int, Int)] =
    val fileDiff = partner.file.value - movedTo.file.value
    val rankDiff = partner.rank.value - movedTo.rank.value
    val axis =
      if fileDiff == 0 && rankDiff != 0 then Some("file")
      else if rankDiff == 0 && fileDiff != 0 then Some("rank")
      else if fileDiff.abs == rankDiff.abs && fileDiff != 0 then Some("diagonal")
      else None
    axis.filter(batteryPairCompatible(movedRole, partnerRole, _)).map { name =>
      (name, Integer.signum(fileDiff), Integer.signum(rankDiff))
    }

  private def batteryPairCompatible(left: Role, right: Role, axis: String): Boolean =
    axis match
      case "diagonal" =>
        (left == Queen && right == Bishop) || (left == Bishop && right == Queen)
      case "file" | "rank" =>
        (left == Queen && right == Rook) ||
          (left == Rook && right == Queen) ||
          (left == Rook && right == Rook)
      case _ => false

  private def pieceValue(role: Role): Int =
    role match
      case Pawn   => 100
      case Knight => 320
      case Bishop => 330
      case Rook   => 500
      case Queen  => 900
      case King   => 10000

  private def sameBranchBadPieceLiquidation(
      replay: List[BoundedReplayStep],
      movingSide: Color,
      originalSquare: Square,
      firstDest: Square
  ): Option[BadPieceLiquidationBranch] =
    def loop(remaining: List[(BoundedReplayStep, Int)], bishopSquare: Square): Option[BadPieceLiquidationBranch] =
      remaining match
        case Nil => None
        case (step, index) :: rest
            if step.move.piece.color == movingSide &&
              step.move.piece.role == Bishop &&
              step.move.orig == bishopSquare =>
          val exchange = step.move.dest
          val recapture = replay.lift(index + 1)
          val liquidation =
            step.move.captures &&
              step.capturedRole.exists(_ != Pawn) &&
              recapture.exists(reply =>
                reply.move.piece.color != movingSide &&
                  reply.move.dest == exchange &&
                  reply.move.captures &&
                  reply.capturedRole.contains(Bishop)
              )
          if liquidation then
            Some(
              BadPieceLiquidationBranch(
                badPieceSquare = originalSquare.key,
                exchangeSquare = exchange.key,
                lineMoves = replay.take(index + 2).map(_.uci)
              )
            )
          else loop(rest, step.move.dest)
        case _ :: rest =>
          loop(rest, bishopSquare)

    loop(replay.zipWithIndex.drop(2), firstDest)

  private def immediateBadPieceLiquidation(
      replay: List[BoundedReplayStep],
      movingSide: Color,
      originalSquare: Square,
      firstDest: Square
  ): Option[BadPieceLiquidationBranch] =
    (replay.headOption, replay.lift(1)) match
      case (Some(first), Some(reply))
          if first.move.piece.color == movingSide &&
            first.move.piece.role == Bishop &&
            first.move.dest == firstDest &&
            first.move.captures &&
            first.capturedRole.exists(_ != Pawn) &&
            reply.move.piece.color != movingSide &&
            reply.move.dest == firstDest &&
            reply.move.captures &&
            reply.capturedRole.contains(Bishop) =>
        Some(
          BadPieceLiquidationBranch(
            badPieceSquare = originalSquare.key,
            exchangeSquare = firstDest.key,
            lineMoves = replay.take(2).map(_.uci)
          )
        )
      case _ => None

  private def isBadBishopOnCurrentBoard(board: Board, color: Color, square: Square): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == Bishop) &&
      sameColorCentralPawnCount(board, color, square) >= 2 &&
      (bishopMobility(board, color, square) <= 5 || bishopOwnPawnBlockers(board, color, square) > 0)

  private def sameColorCentralPawnCount(board: Board, color: Color, square: Square): Int =
    board
      .byPiece(color, Pawn)
      .squares
      .count(pawn => Set("c", "d", "e", "f").contains(pawn.key.take(1)) && pawn.isLight == square.isLight)

  private def bishopMobility(board: Board, color: Color, square: Square): Int =
    (square.bishopAttacks(board.occupied) & ~board.byColor(color)).count

  private def bishopOwnPawnBlockers(board: Board, color: Color, square: Square): Int =
    List((1, 1), (1, -1), (-1, 1), (-1, -1)).count { case (fileStep, rankStep) =>
      firstOccupiedOnRay(board, square, fileStep, rankStep).exists(blocker =>
        board.pieceAt(blocker).exists(piece =>
          piece.color == color &&
            piece.role == Pawn &&
            Set("c", "d", "e", "f").contains(blocker.key.take(1))
        )
      )
    }

  private def firstOccupiedOnRay(
      board: Board,
      square: Square,
      fileStep: Int,
      rankStep: Int
  ): Option[Square] =
    def loop(file: Int, rank: Int): Option[Square] =
      Square.at(file, rank) match
        case Some(next) if board.pieceAt(next).nonEmpty => Some(next)
        case Some(next)                                => loop(next.file.value + fileStep, next.rank.value + rankStep)
        case None                                      => None
    loop(square.file.value + fileStep, square.rank.value + rankStep)

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)
