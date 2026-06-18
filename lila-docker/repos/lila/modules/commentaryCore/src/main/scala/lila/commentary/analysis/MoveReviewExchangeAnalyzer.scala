package lila.commentary.analysis

import _root_.chess.{ Bishop, Bitboard, Board, Color, King, Knight, Move, Pawn, Position, Queen, Role, Rook, Square }
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard

import lila.commentary.analysis.tactical.TacticalPatternDetectors
import lila.commentary.analysis.structure.WeaknessTargetProfile
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

  final case class RelationPracticalSurface(
      text: String,
      patternId: Option[String] = None
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
      lineMoves: List[String]
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
        attackerSquare: String,
        targetSquare: String,
        attackerRole: String,
        targetRole: String
    ) extends RelationDetails
    final case class Domination(
        attackerSquare: String,
        targetSquare: String,
        attackerRole: String,
        targetRole: String,
        controlledEscapeSquares: List[String]
    ) extends RelationDetails
    final case class Zwischenzug(
        intermediateMove: String,
        expectedRecaptureSquare: String,
        checkingPieceSquare: String,
        checkingPieceRole: String,
        checkedKingSquare: String,
        threatType: String
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
    final case class StalemateTrap(
        stalematedKingSquare: String,
        resourceSquare: String,
        entryMove: String,
        terminalMove: String,
        scoreCp: Int
    ) extends RelationDetails
    final case class PerpetualCheck(
        checkedKingSquare: String,
        checkerSquares: List[String],
        checkingSide: String,
        entryMove: String,
        cycleStartMove: String,
        cycleReturnMove: String,
        repeatedPositionKey: String,
        scoreCp: Int
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
        Fork,
        HangingPiece,
        TrappedPiece,
        Domination,
        Zwischenzug,
        XRay,
        Clearance,
        Battery,
        Pin,
        Skewer,
        Interference,
        Decoy,
        StalemateTrap,
        PerpetualCheck
      )

    val Deferred: List[String] =
      Nil

    val All: List[String] =
      Implemented ++ Deferred

  def normalizedTopUciMoves(variations: List[VariationLine]): List[String] =
    variations.headOption.toList.flatMap(normalizedLineMoves)

  private val DrawResourceMaxCp = 80
  private[commentary] val DrawResourceRelationReplayMaxPlies = 12

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
      lineMoves = replayUcis(replay, 0, 3)
    )

  def relationWitnesses(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil,
      continuationLines: List[List[String]] = Nil,
      engineScoreCp: Option[Int] = None,
      engineMate: Option[Int] = None,
      includeDrawResources: Boolean = true
  ): List[RelationWitness] =
    val zwischenzug = zwischenzugWitness(replay, playedMove, explicitTargets)
    val drawResourceWitnesses =
      if includeDrawResources then
        List(
          stalemateTrapWitness(replay, playedMove, engineScoreCp, engineMate),
          perpetualCheckWitness(replay, playedMove, engineScoreCp, engineMate)
        ).flatten
      else Nil
    val fork =
      if zwischenzug.isEmpty then forkWitness(replay, playedMove, explicitTargets)
      else None
    val loosePiecePressure =
      if zwischenzug.isEmpty then
        val trapped = trappedPieceWitness(replay, playedMove, explicitTargets)
        trapped
          .orElse(dominationWitness(replay, playedMove, explicitTargets))
          .orElse(hangingPieceWitness(replay, playedMove, explicitTargets))
      else None
    List(
      defenderTradeBranch(replay, playedMove, explicitTargets).map(defenderTradeWitness),
      badPieceLiquidationBranch(replay, playedMove).map(badPieceLiquidationWitness),
      overloadWitness(replay, playedMove, explicitTargets),
      deflectionWitness(replay, playedMove, explicitTargets),
      discoveredAttackWitness(replay, playedMove, explicitTargets),
      doubleCheckWitness(replay, playedMove, explicitTargets),
      backRankMateWitness(replay, playedMove, explicitTargets),
      mateNetWitness(replay, playedMove, explicitTargets),
      greekGiftWitness(replay, playedMove, explicitTargets, continuationLines)
    ).flatten ++ drawResourceWitnesses ++ List(
      zwischenzug,
      fork,
      loosePiecePressure,
      pinWitness(replay, playedMove, explicitTargets),
      xrayWitness(replay, playedMove, explicitTargets),
      clearanceWitness(replay, playedMove, explicitTargets),
      batteryWitness(replay, playedMove, explicitTargets),
      skewerWitness(replay, playedMove, explicitTargets),
      interferenceWitness(replay, playedMove, explicitTargets),
      decoyWitness(replay, playedMove, explicitTargets)
    ).flatten

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
      case details: RelationDetails.Zwischenzug if witness.kind == RelationKind.Zwischenzug => Some(details)
      case details: RelationDetails.Decoy if witness.kind == RelationKind.Decoy => Some(details)
      case details: RelationDetails.XRay if witness.kind == RelationKind.XRay => Some(details)
      case details: RelationDetails.Clearance if witness.kind == RelationKind.Clearance => Some(details)
      case details: RelationDetails.Battery if witness.kind == RelationKind.Battery => Some(details)
      case details: RelationDetails.Interference if witness.kind == RelationKind.Interference => Some(details)
      case details: RelationDetails.Pin if witness.kind == RelationKind.Pin => Some(details)
      case details: RelationDetails.Skewer if witness.kind == RelationKind.Skewer => Some(details)
      case details: RelationDetails.StalemateTrap if witness.kind == RelationKind.StalemateTrap => Some(details)
      case details: RelationDetails.PerpetualCheck if witness.kind == RelationKind.PerpetualCheck => Some(details)
      case _ => None

  def relationDetailsValidForKind(witness: RelationWitness): Boolean =
    if RelationKind.Implemented.contains(witness.kind) then typedDetailsFromWitness(witness).nonEmpty
    else witness.details == RelationDetails.Empty || typedDetailsFromWitness(witness).nonEmpty

  def relationProjectionFromWitness(witness: RelationWitness): Option[RelationProjection] =
    for
      typedDetails <- typedDetailsFromWitness(witness)
      if RelationKind.Implemented.contains(witness.kind)
    yield
      val (rawFocusSquares, rawTargetSquare, rawFactTerms) =
        typedDetails match
          case details: RelationDetails.DefenderTrade =>
            (
              List(details.targetSquare, details.exchangeSquare),
              Some(details.targetSquare),
              List(
                "defender_trade_branch",
                s"defender:${details.defenderSquare}",
                s"exchange_square:${details.exchangeSquare}",
                s"defended_target:${details.targetSquare}"
              )
            )
          case details: RelationDetails.BadPieceLiquidation =>
            (
              List(details.badPieceSquare, details.exchangeSquare),
              Some(details.exchangeSquare),
              List(
                "bad_piece_liquidation_branch",
                s"bad_piece:${details.badPieceSquare}",
                s"exchange_square:${details.exchangeSquare}"
              )
            )
          case details: RelationDetails.Overload =>
            (
              (details.defenderSquare :: details.targetSquares).take(4),
              details.targetSquares.headOption,
              List(
                "overload_relation_witness",
                s"defender:${details.defenderSquare}",
                s"duties:${details.targetSquares.mkString("|")}",
                s"attacker:${details.attackerSquare}"
              )
            )
          case details: RelationDetails.Deflection =>
            (
              List(details.targetSquare, details.defenderSquare, details.attackerSquare),
              Some(details.targetSquare),
              List(
                "deflection_relation_witness",
                s"defender:${details.defenderSquare}",
                s"defended_target:${details.targetSquare}",
                s"attacker:${details.attackerSquare}"
              )
            )
          case details: RelationDetails.DiscoveredAttack =>
            (
              List(details.attackerSquare, details.clearedSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "discovered_attack_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"cleared_square:${details.clearedSquare}",
                s"target:${details.targetSquare}",
                s"attacker_role:${details.attackerRole}"
              )
            )
          case details: RelationDetails.DoubleCheck =>
            (
              (details.kingSquare :: details.checkerSquares).distinct,
              Some(details.kingSquare),
              List(
                "double_check_relation_witness",
                s"king:${details.kingSquare}",
                s"checkers:${details.checkerSquares.sorted.mkString("|")}",
                s"mover:${details.moverSquare}",
                s"mover_role:${details.moverRole}"
              )
            )
          case details: RelationDetails.MatePattern
              if details.relationKind == RelationKind.BackRankMate =>
            (
              (details.kingSquare :: details.checkerSquares).distinct,
              Some(details.kingSquare),
              List(
                "back_rank_mate_relation_witness",
                "mate",
                s"king:${details.kingSquare}",
                s"checkers:${details.checkerSquares.sorted.mkString("|")}",
                s"mating_move:${details.matingMove}"
              )
            )
          case details: RelationDetails.MatePattern
              if details.relationKind == RelationKind.MateNet =>
            (
              (details.kingSquare :: details.checkerSquares).distinct,
              Some(details.kingSquare),
              List(
                "mate_net_relation_witness",
                "mate",
                s"pattern:${details.patternId.getOrElse(RelationKind.MateNet)}",
                s"king:${details.kingSquare}",
                s"checkers:${details.checkerSquares.sorted.mkString("|")}",
                s"mating_move:${details.matingMove}"
              )
            )
          case details: RelationDetails.GreekGift =>
            (
              List(details.bishopSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "greek_gift_relation_witness",
                "sacrifice_entry",
                s"pattern:${details.patternId}",
                s"target:${details.targetSquare}",
                s"bishop:${details.bishopSquare}",
                s"entry_move:${details.entryMove}"
              )
            )
          case details: RelationDetails.Fork =>
            (
              (details.attackerSquare :: details.targets.map(_.square)).distinct,
              details.targets.headOption.map(_.square),
              List(
                "fork_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"attacker_role:${details.attackerRole}",
                s"targets:${details.targets.map(_.square).sorted.mkString("|")}"
              ) ++ details.targets.map(target => s"target:${target.square}:${target.role}")
            )
          case details: RelationDetails.HangingPiece =>
            (
              List(details.attackerSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "hanging_piece_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"target:${details.targetSquare}",
                s"attacker_role:${details.attackerRole}",
                s"target_role:${details.targetRole}",
                "undefended_target"
              )
            )
          case details: RelationDetails.TrappedPiece =>
            (
              List(details.attackerSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "trapped_piece_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"target:${details.targetSquare}",
                s"attacker_role:${details.attackerRole}",
                s"target_role:${details.targetRole}",
                "no_safe_escape"
              )
            )
          case details: RelationDetails.Domination =>
            (
              (details.attackerSquare :: details.targetSquare :: details.controlledEscapeSquares).distinct,
              Some(details.targetSquare),
              List(
                "domination_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"target:${details.targetSquare}",
                s"attacker_role:${details.attackerRole}",
                s"target_role:${details.targetRole}",
                s"controlled_escapes:${details.controlledEscapeSquares.sorted.mkString("|")}",
                "no_safe_escape",
                "escape_square_control"
              )
            )
          case details: RelationDetails.Zwischenzug =>
            (
              List(details.checkingPieceSquare, details.expectedRecaptureSquare, details.checkedKingSquare),
              Some(details.expectedRecaptureSquare),
              List(
                "zwischenzug_relation_witness",
                s"intermediate_move:${details.intermediateMove}",
                s"expected_recapture:${details.expectedRecaptureSquare}",
                s"checker:${details.checkingPieceSquare}",
                s"checker_role:${details.checkingPieceRole}",
                s"king:${details.checkedKingSquare}",
                s"threat:${details.threatType}",
                "recapture_available",
                "in_between_check"
              )
            )
          case details: RelationDetails.Decoy =>
            (
              List(details.baitFromSquare, details.baitSquare, details.luredFromSquare),
              Some(details.baitSquare),
              List(
                "decoy_relation_witness",
                s"bait:${details.baitSquare}",
                s"lured_from:${details.luredFromSquare}",
                s"lured_role:${details.luredRole}",
                s"bait_role:${details.baitRole}",
                s"execution:${details.executionFromSquare}-${details.executionToSquare}"
              )
            )
          case details: RelationDetails.XRay =>
            (
              List(details.attackerSquare, details.blockerSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "xray_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"blocker:${details.blockerSquare}",
                s"target:${details.targetSquare}",
                s"attacker_role:${details.attackerRole}",
                s"blocker_role:${details.blockerRole}",
                s"target_role:${details.targetRole}"
              )
            )
          case details: RelationDetails.Clearance =>
            (
              List(details.beneficiarySquare, details.clearedSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "clearance_relation_witness",
                s"beneficiary:${details.beneficiarySquare}",
                s"cleared_square:${details.clearedSquare}",
                s"target:${details.targetSquare}",
                s"beneficiary_role:${details.beneficiaryRole}",
                s"clearing_to:${details.clearingTo}"
              )
            )
          case details: RelationDetails.Battery =>
            (
              List(details.frontSquare, details.backSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "battery_relation_witness",
                s"front:${details.frontSquare}",
                s"back:${details.backSquare}",
                s"target:${details.targetSquare}",
                s"front_role:${details.frontRole}",
                s"back_role:${details.backRole}",
                s"axis:${details.axis}"
              )
            )
          case details: RelationDetails.Interference =>
            (
              List(details.blockerSquare, details.defenderSquare, details.targetSquare),
              Some(details.targetSquare),
              List(
                "interference_relation_witness",
                s"blocker:${details.blockerSquare}",
                s"defender:${details.defenderSquare}",
                s"target:${details.targetSquare}",
                s"blocker_role:${details.blockerRole}",
                s"defender_role:${details.defenderRole}",
                s"target_role:${details.targetRole}"
              )
            )
          case details: RelationDetails.Pin =>
            (
              List(details.attackerSquare, details.pinnedSquare, details.behindSquare),
              Some(details.targetSquare),
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
            )
          case details: RelationDetails.Skewer =>
            (
              List(details.attackerSquare, details.frontSquare, details.backSquare),
              Some(details.targetSquare),
              List(
                "skewer_relation_witness",
                s"attacker:${details.attackerSquare}",
                s"front:${details.frontSquare}",
                s"back:${details.backSquare}",
                s"attacker_role:${details.attackerRole}",
                s"front_role:${details.frontRole}",
                s"back_role:${details.backRole}"
              )
            )
          case details: RelationDetails.StalemateTrap =>
            (
              List(details.stalematedKingSquare, details.resourceSquare),
              Some(details.stalematedKingSquare),
              List(
                "stalemate_trap_relation_witness",
                "draw_resource",
                "terminal:stalemate",
                s"king:${details.stalematedKingSquare}",
                s"resource:${details.resourceSquare}",
                s"entry_move:${details.entryMove}",
                s"terminal_move:${details.terminalMove}",
                s"draw_score_cp:${details.scoreCp}"
              )
            )
          case details: RelationDetails.PerpetualCheck =>
            (
              (details.checkedKingSquare :: details.checkerSquares).distinct,
              Some(details.checkedKingSquare),
              List(
                "perpetual_check_relation_witness",
                "draw_resource",
                "checking_cycle",
                s"king:${details.checkedKingSquare}",
                s"checkers:${details.checkerSquares.sorted.mkString("|")}",
                s"checking_side:${details.checkingSide}",
                s"entry_move:${details.entryMove}",
                s"cycle_start:${details.cycleStartMove}",
                s"cycle_return:${details.cycleReturnMove}",
                s"repetition_key:${compactPositionKey(details.repeatedPositionKey)}",
                s"draw_score_cp:${details.scoreCp}"
              )
            )
          case _ => (Nil, None, Nil)
      RelationProjection(
        kind = witness.kind,
        focusSquares = rawFocusSquares.flatMap(square => squareFromKey(square).map(_.key)).distinct,
        targetSquare = rawTargetSquare.flatMap(square => squareFromKey(square).map(_.key)),
        factTerms = (rawFactTerms ++ branchFactFromMoves(witness.lineMoves)).distinct,
        lineMoves = witness.lineMoves
      )

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

  def relationPracticalSurfaceFromWitness(
      witness: RelationWitness,
      matePatternPhraseById: String => Option[String] = _ => None
  ): Option[RelationPracticalSurface] =
    typedDetailsFromWitness(witness).flatMap {
      case details: RelationDetails.DefenderTrade =>
        Some(
          RelationPracticalSurface(
            s"The checked line trades on ${details.exchangeSquare} to remove the defender from ${details.defenderSquare}, loosening ${details.targetSquare}."
          )
        )
      case details: RelationDetails.BadPieceLiquidation =>
        Some(
          RelationPracticalSurface(
            s"The checked line trades on ${details.exchangeSquare} to clear the bad piece from ${details.badPieceSquare}."
          )
        )
      case details: RelationDetails.XRay =>
        Some(
          RelationPracticalSurface(
            s"The checked line sets x-ray pressure from the ${details.attackerRole} on ${details.attackerSquare} through ${details.blockerSquare} toward ${details.targetSquare}."
          )
        )
      case details: RelationDetails.Deflection =>
        Some(
          RelationPracticalSurface(
            s"The checked line attacks the defender on ${details.defenderSquare} from ${details.attackerSquare}; after it moves, ${details.targetSquare} loses that defense."
          )
        )
      case details: RelationDetails.DiscoveredAttack =>
        Some(
          RelationPracticalSurface(
            s"The checked line clears ${details.clearedSquare}, revealing a ${details.attackerRole} attack from ${details.attackerSquare} on ${details.targetSquare}."
          )
        )
      case details: RelationDetails.Clearance =>
        Some(
          RelationPracticalSurface(
            s"The checked line clears ${details.clearedSquare}, opening the ${details.beneficiaryRole} on ${details.beneficiarySquare} toward ${details.targetSquare}."
          )
        )
      case details: RelationDetails.MatePattern
          if details.relationKind == RelationKind.BackRankMate =>
        Some(RelationPracticalSurface(matePatternPracticalText(details, "back-rank mate"), details.patternId))
      case details: RelationDetails.MatePattern
          if details.relationKind == RelationKind.MateNet =>
        val pattern = details.patternId.flatMap(matePatternPhraseById).getOrElse("mate net")
        Some(RelationPracticalSurface(matePatternPracticalText(details, pattern), details.patternId))
      case details: RelationDetails.GreekGift =>
        Some(
          RelationPracticalSurface(
            s"The checked line starts a Greek gift sacrifice with the bishop on ${details.targetSquare}."
          )
        )
      case details: RelationDetails.DoubleCheck =>
        Some(
          RelationPracticalSurface(
            s"The checked line gives double check on ${details.kingSquare} from ${details.checkerSquares.mkString(" and ")}."
          )
        )
      case details: RelationDetails.Fork
          if details.attackerRole == "knight" || details.attackerRole == "pawn" || details.targets.exists(_.role == "king") =>
        val targetText =
          details.targets.take(3).map(target => s"the ${target.role} on ${target.square}").mkString(" and ")
        Some(
          RelationPracticalSurface(
            s"The checked line puts the ${details.attackerRole} on ${details.attackerSquare} attacking $targetText."
          )
        )
      case details: RelationDetails.Overload if details.targetSquares.size >= 2 =>
        Some(
          RelationPracticalSurface(
            s"The checked line overloads the defender on ${details.defenderSquare} across ${details.targetSquares.take(3).mkString(" and ")}."
          )
        )
      case details: RelationDetails.Interference =>
        Some(
          RelationPracticalSurface(
            s"The checked line puts the ${details.blockerRole} on ${details.blockerSquare} between the ${details.defenderRole} on ${details.defenderSquare} and the ${details.targetRole} on ${details.targetSquare}."
          )
        )
      case details: RelationDetails.HangingPiece =>
        Some(
          RelationPracticalSurface(
            s"The checked line attacks the undefended ${details.targetRole} on ${details.targetSquare} with the ${details.attackerRole} on ${details.attackerSquare}."
          )
        )
      case details: RelationDetails.TrappedPiece =>
        Some(
          RelationPracticalSurface(
            s"The checked line traps the ${details.targetRole} on ${details.targetSquare} with the ${details.attackerRole} on ${details.attackerSquare}."
          )
        )
      case details: RelationDetails.Domination if details.controlledEscapeSquares.nonEmpty =>
        val escapes = details.controlledEscapeSquares.take(3).mkString(" and ")
        Some(
          RelationPracticalSurface(
            s"The checked line dominates the ${details.targetRole} on ${details.targetSquare}; the ${details.attackerRole} on ${details.attackerSquare} controls $escapes."
          )
        )
      case details: RelationDetails.Zwischenzug =>
        Some(
          RelationPracticalSurface(
            s"The checked line inserts ${details.intermediateMove} as a check on ${details.checkedKingSquare} before the recapture on ${details.expectedRecaptureSquare}."
          )
        )
      case details: RelationDetails.Skewer =>
        Some(
          RelationPracticalSurface(
            s"The checked line lines up the ${details.attackerRole} on ${details.attackerSquare} against the ${details.frontRole} on ${details.frontSquare}, with the ${details.backRole} on ${details.backSquare} behind it."
          )
        )
      case details: RelationDetails.Battery =>
        Some(
          RelationPracticalSurface(
            s"The checked line forms a ${details.frontRole}-${details.backRole} battery on the ${details.axis} toward ${details.targetSquare}."
          )
        )
      case details: RelationDetails.Pin if details.absolute =>
        Some(
          RelationPracticalSurface(
            s"The checked line pins the ${details.pinnedRole} on ${details.pinnedSquare} to the ${details.behindRole} on ${details.behindSquare}."
          )
        )
      case details: RelationDetails.Decoy =>
        Some(
          RelationPracticalSurface(
            s"The checked line offers the ${details.baitRole} on ${details.baitSquare} to lure the ${details.luredRole} from ${details.luredFromSquare}, then recaptures on ${details.executionToSquare}."
          )
        )
      case details: RelationDetails.StalemateTrap =>
        Some(
          RelationPracticalSurface(
            s"The checked line keeps a stalemate resource around the king on ${details.stalematedKingSquare} via ${details.resourceSquare}."
          )
        )
      case details: RelationDetails.PerpetualCheck =>
        Some(
          RelationPracticalSurface(
            s"The checked line keeps a perpetual-check resource against the king on ${details.checkedKingSquare} from ${details.checkerSquares.take(3).mkString(" and ")}."
          )
        )
      case _ => None
    }

  private def matePatternPracticalText(details: RelationDetails.MatePattern, pattern: String): String =
    s"The checked line ends in $pattern on ${details.kingSquare} after ${details.matingMove}."

  def ownerSeedTermsFromWitness(
      witness: RelationWitness,
      planKindId: String,
      aliases: List[String]
  ): List[String] =
    relationProjectionFromWitness(witness)
      .map(projection => (aliases ++ projection.focusSquares ++ projection.factTerms ++ List(planKindId)).distinct)
      .getOrElse(Nil)

  def transitionTermsFromWitness(
      witness: RelationWitness,
      extras: List[String]
  ): List[String] =
    relationProjectionFromWitness(witness)
      .map(projection => (projection.factTerms ++ extras ++ projection.lineMoves.map(move => s"pv:$move")).distinct)
      .getOrElse(Nil)

  def defenderTradeWitness(branch: DefenderTradeBranch): RelationWitness =
    RelationWitness(
      kind = RelationKind.DefenderTrade,
      focusSquares = List(branch.targetSquare, branch.exchangeSquare),
      facts = List(
        "defender_trade_branch",
        s"defender:${branch.defenderSquare}",
        s"exchange_square:${branch.exchangeSquare}",
        s"defended_target:${branch.targetSquare}"
      ) ++ branchFactFromMoves(branch.lineMoves),
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
        s"bad_piece:${branch.badPieceSquare}",
        s"exchange_square:${branch.exchangeSquare}"
      ) ++ branchFactFromMoves(branch.lineMoves),
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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
        ) ++ branchFactFromMoves(replayUcis(replay, 0, 2)),
        lineMoves = replayUcis(replay, 0, 2),
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
      witness <- discoveredAttackAfterMove(first.before.board, first.after.board, movingSide, first.move.orig, first.move.dest, targetSet)
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
        lineMoves = replayUcis(replay, 0, 1),
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
        lineMoves = replayUcis(replay, 0, 1),
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
        lineMoves = replayUcis(replay, 0, 1),
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
        lineMoves = replayUcis(replay, 0, 1),
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
      movingSide = first.move.piece.color
      explicit = normalizedExplicitTargetKeys(explicitTargets)
      explicitTargetSquares = explicit.flatMap(squareFromKey).distinct
      if explicit.nonEmpty && explicitTargetSquares.size == explicit.size
      checkers = first.after.checkers.squares.toList
      if first.after.check.yes && checkers.exists(_ == first.move.dest)
      recapture <- explicitTargetSquares
        .flatMap { target =>
          first.before.legalMoves.toList.flatMap { candidate =>
            val capturedRole =
              candidate.capture
                .flatMap(first.before.board.roleAt)
                .orElse(first.before.board.roleAt(candidate.dest))
            capturedRole
              .filter(role => candidate.piece.color == movingSide && candidate.dest == target && candidate.captures && role != Pawn)
              .map(role => target -> role)
          }
        }
        .sortBy { case (target, role) => (-pieceValue(role), target.key) }
        .headOption
      (expectedRecaptureSquare, _) = recapture
      if first.move.dest != expectedRecaptureSquare
      king <- first.after.board.kingPosOf(first.after.color)
      threatType = if first.after.checkMate then "mate_check" else "check"
      lineMoves = replayUcis(replay, 0, 1)
    yield
      RelationWitness(
        kind = RelationKind.Zwischenzug,
        focusSquares = List(first.move.dest.key, expectedRecaptureSquare.key, king.key),
        facts = List(
          "zwischenzug_relation_witness",
          s"intermediate_move:${first.uci}",
          s"expected_recapture:${expectedRecaptureSquare.key}",
          s"checker:${first.move.dest.key}",
          s"checker_role:${first.move.piece.role.name}",
          s"king:${king.key}",
          s"threat:$threatType",
          "recapture_available",
          "in_between_check"
        ) ++ branchFactFromMoves(lineMoves, maxPlies = 1),
        lineMoves = lineMoves,
        targetSquare = Some(expectedRecaptureSquare.key),
        details = RelationDetails.Zwischenzug(
          intermediateMove = first.uci,
          expectedRecaptureSquare = expectedRecaptureSquare.key,
          checkingPieceSquare = first.move.dest.key,
          checkingPieceRole = first.move.piece.role.name,
          checkedKingSquare = king.key,
          threatType = threatType
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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

  def trappedPieceWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      explicitTargetSet = normalizedExplicitTargetKeys(explicitTargets).flatMap(squareFromKey).toSet
      if explicitTargetSet.nonEmpty
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- trappedPieceAfterMove(
        position = first.after,
        movingSide = movingSide,
        attacker = first.move.dest,
        attackerRole = first.move.piece.role,
        targetSet = targetSet,
        explicitTargetSet = explicitTargetSet
      )
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

  def dominationWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      explicitTargets: List[String] = Nil
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      movingSide = first.move.piece.color
      explicitTargetSet = normalizedExplicitTargetKeys(explicitTargets).flatMap(squareFromKey).toSet
      if explicitTargetSet.nonEmpty
      targetSet = relationTargetSquares(first.after.board, movingSide, explicitTargets).toSet
      witness <- dominationAfterMove(
        position = first.after,
        movingSide = movingSide,
        attacker = first.move.dest,
        attackerRole = first.move.piece.role,
        targetSet = targetSet,
        explicitTargetSet = explicitTargetSet
      )
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
    yield witness.copy(lineMoves = replayUcis(replay, 0, 1))

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
        ) ++ branchFactFromMoves(replayUcis(replay, 0, 3), maxPlies = 3),
        lineMoves = replayUcis(replay, 0, 3),
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

  def stalemateTrapWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      engineScoreCp: Option[Int],
      engineMate: Option[Int]
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      scoreCp <- engineScoreCp
      first <- replay.headOption.filter(_.uci == normalizedPlayed)
      if drawResourceScoreStable(Some(scoreCp), engineMate)
      terminal <- replay.lastOption
      if terminal.after.staleMate
      king <- terminal.after.board.kingPosOf(terminal.after.color)
    yield
      val lineMoves = replayUcis(replay, 0, replay.length)
      RelationWitness(
        kind = RelationKind.StalemateTrap,
        focusSquares = List(king.key, terminal.move.dest.key),
        facts = List(
          "stalemate_trap_relation_witness",
          "draw_resource",
          "terminal:stalemate",
          s"king:${king.key}",
          s"resource:${terminal.move.dest.key}",
          s"entry_move:${first.uci}",
          s"terminal_move:${terminal.uci}",
          s"draw_score_cp:$scoreCp"
        ) ++ branchFactFromMoves(lineMoves),
        lineMoves = lineMoves,
        targetSquare = Some(king.key),
        details = RelationDetails.StalemateTrap(
          stalematedKingSquare = king.key,
          resourceSquare = terminal.move.dest.key,
          entryMove = first.uci,
          terminalMove = terminal.uci,
          scoreCp = scoreCp
        )
      )

  def perpetualCheckWitness(
      replay: List[BoundedReplayStep],
      playedMove: String,
      engineScoreCp: Option[Int],
      engineMate: Option[Int]
  ): Option[RelationWitness] =
    val normalizedPlayed = NarrativeUtils.normalizeUciMove(playedMove)
    for
      scoreCp <- engineScoreCp
      first <- replay.headOption.filter(step => step.uci == normalizedPlayed && step.after.check.yes)
      if drawResourceScoreStable(Some(scoreCp), engineMate)
      cycle <- repeatedCheckingCycle(replay, first.move.piece.color)
    yield
      val lineMoves = replayUcis(replay, 0, replay.length)
      RelationWitness(
        kind = RelationKind.PerpetualCheck,
        focusSquares = (cycle.kingSquare :: cycle.checkerSquares).distinct,
        facts = List(
          "perpetual_check_relation_witness",
          "draw_resource",
          "checking_cycle",
          s"king:${cycle.kingSquare}",
          s"checkers:${cycle.checkerSquares.sorted.mkString("|")}",
          s"checking_side:${cycle.checkingSide}",
          s"entry_move:${first.uci}",
          s"cycle_start:${cycle.startMove}",
          s"cycle_return:${cycle.returnMove}",
          s"repetition_key:${compactPositionKey(cycle.positionKey)}",
          s"draw_score_cp:$scoreCp"
        ) ++ branchFactFromMoves(lineMoves, maxPlies = 5),
        lineMoves = lineMoves,
        targetSquare = Some(cycle.kingSquare),
        details = RelationDetails.PerpetualCheck(
          checkedKingSquare = cycle.kingSquare,
          checkerSquares = cycle.checkerSquares,
          checkingSide = cycle.checkingSide,
          entryMove = first.uci,
          cycleStartMove = cycle.startMove,
          cycleReturnMove = cycle.returnMove,
          repeatedPositionKey = cycle.positionKey,
          scoreCp = scoreCp
        )
      )

  private def drawResourceScoreStable(scoreCp: Option[Int], mate: Option[Int]): Boolean =
    mate.isEmpty && scoreCp.exists(cp => math.abs(cp) <= DrawResourceMaxCp)

  private final case class RepeatedCheckingCycle(
      kingSquare: String,
      checkerSquares: List[String],
      checkingSide: String,
      startMove: String,
      returnMove: String,
      positionKey: String
  )

  private def repeatedCheckingCycle(
      replay: List[BoundedReplayStep],
      checkingSide: Color
  ): Option[RepeatedCheckingCycle] =
    val checkingPositions =
      replay.zipWithIndex.flatMap { case (step, index) =>
        Option.when(step.move.piece.color == checkingSide && step.after.check.yes)(
          for
            king <- step.after.board.kingPosOf(step.after.color)
            checkers = step.after.checkers.squares.toList.map(_.key).sorted
            if checkers.nonEmpty
          yield (index, step, repetitionPositionKey(step.after), king.key, checkers)
        ).flatten
      }
    Option.when(checkingPositions.size >= 3)(()).flatMap { _ =>
      checkingPositions
        .groupBy(_._3)
        .values
        .toList
        .flatMap { positions =>
          val sorted = positions.sortBy(_._1)
          for
            start <- sorted.headOption
            end <- sorted.drop(1).headOption
            if end._1 - start._1 >= 4
          yield RepeatedCheckingCycle(
            kingSquare = end._4,
            checkerSquares = end._5,
            checkingSide = if checkingSide.white then "white" else "black",
            startMove = start._2.uci,
            returnMove = end._2.uci,
            positionKey = end._3
          )
        }
        .sortBy(cycle => replay.indexWhere(_.uci == cycle.startMove))
        .headOption
    }

  private def repetitionPositionKey(position: Position): String =
    Fen.write(position).value.split("\\s+").take(4).mkString(" ")

  private def compactPositionKey(positionKey: String): String =
    positionKey.trim.replaceAll("""\s+""", "|")

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
        replayUcis(replay, 0, index + 2)
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
    branchKeyFromMoves(replayUcis(replay, 0, plies), plies)

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

  private def replayUcis(replay: List[BoundedReplayStep], fromPly: Int, maxPlies: Int): List[String] =
    replay.drop(fromPly).take(maxPlies).map(_.uci)

  def legalMove(position: Position, uci: String): Option[Move] =
    Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  def isUciMove(move: String): Boolean =
    move.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  def squareFromKey(key: String): Option[Square] =
    Square.fromKey(Option(key).map(_.trim.toLowerCase).getOrElse(""))

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
          if targetIsPressed then
            after
              .attackers(target, defenderColor)
              .squares
              .filterNot(square => after.roleAt(square).contains(King))
              .map(_ -> target)
          else Nil
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
      movedTo: Square,
      targetSet: Set[Square]
  ): Option[RelationWitness] =
    after.byColor(!movingSide).squares.toList
      .filter(targetSet.contains)
      .filterNot(target => after.roleAt(target).contains(King))
      .flatMap { target =>
        val beforeAttackers = before.attackers(target, movingSide)
        val afterAttackers = after.attackers(target, movingSide)
        (afterAttackers & ~beforeAttackers).squares.toList.flatMap { attacker =>
          if attacker == movedTo then Nil
          else
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
      position: Position,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square],
      explicitTargetSet: Set[Square]
  ): Option[RelationWitness] =
    val board = position.board
    Option.when(position.color == !movingSide)(()).flatMap { _ =>
      roleAttacks(attackerRole, attacker, movingSide, board.occupied).squares.toList
        .filter(target => targetSet.contains(target) && explicitTargetSet.contains(target))
        .flatMap(target =>
          board.pieceAt(target).filter(_.color != movingSide).map(piece => target -> piece.role)
        )
        .filter { case (target, role) =>
          role != King &&
            role != Pawn &&
            pieceValue(role) > pieceValue(attackerRole) &&
            safeEscapeSquares(position, target, role, !movingSide, movingSide).isEmpty
        }
        .sortBy { case (target, role) => (-pieceValue(role), target.key) }
        .headOption
        .map { case (target, role) =>
          RelationWitness(
            kind = RelationKind.TrappedPiece,
            focusSquares = List(attacker.key, target.key),
            facts = List(
              "trapped_piece_relation_witness",
              s"attacker:${attacker.key}",
              s"target:${target.key}",
              s"attacker_role:${attackerRole.name}",
              s"target_role:${role.name}",
              "no_safe_escape"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.TrappedPiece(
              attackerSquare = attacker.key,
              targetSquare = target.key,
              attackerRole = attackerRole.name,
              targetRole = role.name
            )
          )
        }
    }

  private def safeEscapeSquares(
      position: Position,
      target: Square,
      targetRole: Role,
      targetColor: Color,
      pressureSide: Color
  ): List[String] =
    position.legalMoves.toList
      .filter(move => move.orig == target && move.piece.color == targetColor && move.piece.role == targetRole)
      .flatMap { move =>
        Option.when(
          move.after.board.pieceAt(move.dest).exists(piece => piece.color == targetColor && piece.role == targetRole) &&
            move.after.board.attackers(move.dest, pressureSide).isEmpty
        )(move.dest.key)
      }
      .distinct

  private def dominationAfterMove(
      position: Position,
      movingSide: Color,
      attacker: Square,
      attackerRole: Role,
      targetSet: Set[Square],
      explicitTargetSet: Set[Square]
  ): Option[RelationWitness] =
    val board = position.board
    Option.when(position.color == !movingSide)(()).flatMap { _ =>
      roleAttacks(attackerRole, attacker, movingSide, board.occupied).squares.toList
        .filter(target => targetSet.contains(target) && explicitTargetSet.contains(target))
        .flatMap(target =>
          board.pieceAt(target).filter(_.color != movingSide).map(piece => target -> piece.role)
        )
        .flatMap { case (target, role) =>
          val targetColor = !movingSide
          val pseudoEscapes = pseudoEscapeSquares(board, target, role, targetColor)
          val controlledEscapes =
            pseudoEscapes.filter(square => board.attackers(square, movingSide).nonEmpty).map(_.key).sorted
          Option.when(
            role != King &&
              role != Pawn &&
              pieceValue(role) <= pieceValue(attackerRole) &&
              pseudoEscapes.nonEmpty &&
              controlledEscapes.size == pseudoEscapes.size &&
              safeEscapeSquares(position, target, role, targetColor, movingSide).isEmpty &&
              !(isLongRangeRole(attackerRole) && sameColorRayPieceBehind(board, attacker, target))
          )(
            target -> role -> controlledEscapes
          )
        }
        .sortBy { case ((target, role), _) => (-pieceValue(role), target.key) }
        .headOption
        .map { case ((target, role), controlledEscapes) =>
          RelationWitness(
            kind = RelationKind.Domination,
            focusSquares = (attacker.key :: target.key :: controlledEscapes).distinct,
            facts = List(
              "domination_relation_witness",
              s"attacker:${attacker.key}",
              s"target:${target.key}",
              s"attacker_role:${attackerRole.name}",
              s"target_role:${role.name}",
              s"controlled_escapes:${controlledEscapes.mkString("|")}",
              "no_safe_escape",
              "escape_square_control"
            ),
            lineMoves = Nil,
            targetSquare = Some(target.key),
            details = RelationDetails.Domination(
              attackerSquare = attacker.key,
              targetSquare = target.key,
              attackerRole = attackerRole.name,
              targetRole = role.name,
              controlledEscapeSquares = controlledEscapes
            )
          )
        }
    }

  private def pseudoEscapeSquares(
      board: Board,
      target: Square,
      targetRole: Role,
      targetColor: Color
  ): List[Square] =
    roleAttacks(targetRole, target, targetColor, board.occupied).squares.toList
      .filter(square => !board.pieceAt(square).exists(_.color == targetColor))
      .distinct

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

  private[commentary] def roleAttacks(role: Role, square: Square, color: Color, occupied: Bitboard): Bitboard =
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
                lineMoves = replayUcis(replay, 0, index + 2)
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
            lineMoves = replayUcis(replay, 0, 2)
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
