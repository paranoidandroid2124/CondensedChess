package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object AdvantageTransformationEvidence:

  final case class TransformationCandidate(
      kind: PlanKind,
      moveUci: String,
      role: Role,
      from: Square,
      to: Square,
      materialBalance: Int,
      promotionDistanceAfter: Option[Int],
      score: Double,
      reasons: List[String]
  )

  def candidatesFromFen(fen: String, color: Color): List[TransformationCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        candidates(oriented, color)
      }

  def candidates(pos: Position, color: Color): List[TransformationCandidate] =
    val oriented = orientedPosition(pos, color)
    val board = oriented.board
    val balance = materialBalance(board, color)
    val oppositeBishops = hasOppositeColoredBishops(board, color)
    oriented.legalMoves.toList
      .flatMap(moveCandidates(board, color, balance, oppositeBishops, _))
      .sortBy(c => (-c.score, c.kind.id, c.moveUci))
      .distinctBy(c => s"${c.kind.id}|${c.moveUci}")

  def planHypotheses(fen: String, color: Color): List[PlanHypothesis] =
    candidatesFromFen(fen, color)
      .groupBy(_.kind)
      .values
      .toList
      .flatMap(_.sortBy(c => -c.score).headOption)
      .sortBy(c => -c.score)
      .take(4)
      .map(toHypothesis)

  def movesForSubplan(pos: Position, subplan: PlanKind, legalMoves: List[Move]): List[Move] =
    val byUci =
      candidates(pos, pos.color)
        .filter(_.kind == subplan)
        .map(candidate => candidate.moveUci -> candidate.score)
        .toMap
    legalMoves
      .flatMap(mv => byUci.get(mv.toUci.uci).map(score => mv -> score))
      .sortBy { case (_, score) => -score }
      .map(_._1)
      .distinct

  private def moveCandidates(
      board: Board,
      color: Color,
      balance: Int,
      oppositeBishops: Boolean,
      mv: Move
  ): List[TransformationCandidate] =
    if mv.piece.color != color || mv.promotion.nonEmpty && mv.piece.role != Pawn then Nil
    else
      val pawnConversion = pawnConversionCandidate(board, color, mv)
      val simplification = simplificationConversionCandidate(board, balance, mv)
      val invasion = invasionCandidate(balance, mv)
      val oppositeBishop = oppositeBishopsConversionCandidate(board, color, balance, oppositeBishops, mv)
      List(pawnConversion, simplification, invasion, oppositeBishop).flatten.filter(_.score >= 0.58)

  private def pawnConversionCandidate(
      board: Board,
      color: Color,
      mv: Move
  ): Option[TransformationCandidate] =
    if mv.piece.role != Pawn || !advancesTowardPromotion(mv, color) then None
    else
      val after = mv.after.board
      val beforePassers = passedPawns(board, color)
      val afterPassers = passedPawns(after, color)
      val wasPassed = isPassedPawn(board, mv.orig, color)
      val promotes = mv.promotion.nonEmpty
      val isPassedAfter = promotes || isPassedPawn(after, mv.dest, color)
      val createsPassed = isPassedAfter && (!wasPassed || afterPassers.size > beforePassers.size)
      val kind =
        if wasPassed && isPassedAfter then Some(PlanKind.PasserConversion)
        else if createsPassed then Some(PlanKind.PassedPawnManufacture)
        else None
      kind.flatMap { k =>
        val distanceAfter = mv.promotion.fold(promotionDistance(mv.dest, color))(_ => 0)
        val protectedAfter = !promotes && isProtectedByPawn(after, mv.dest, color)
        val frontBlocked = !promotes && forwardSquare(mv.dest, color).exists(after.pieceAt(_).isDefined)
        val passerGain = (afterPassers.size - beforePassers.size).max(0)
        val score =
          (conversionBase(k) +
            (6 - distanceAfter).max(0).min(6) * 0.025 +
            passerGain.min(2) * 0.08 +
            (if protectedAfter then 0.05 else 0.0) +
            (if mv.captures then 0.035 else 0.0) +
            (if promotes then 0.14 else 0.0) -
            (if frontBlocked then 0.08 else 0.0))
            .max(0.30)
            .min(0.88)
        Option.when(score >= 0.58) {
          TransformationCandidate(
            kind = k,
            moveUci = mv.toUci.uci,
            role = Pawn,
            from = mv.orig,
            to = mv.dest,
            materialBalance = materialBalance(board, color),
            promotionDistanceAfter = Some(distanceAfter),
            score = score,
            reasons =
              List(
                Option.when(wasPassed)("existing_passed_pawn"),
                Option.when(createsPassed)("creates_passed_pawn"),
                Option.when(passerGain > 0)(s"passer_count:${beforePassers.size}->${afterPassers.size}"),
                Option.when(mv.captures)("capture_removes_blocker_or_guard"),
                Option.when(protectedAfter)("protected_passer"),
                Option.when(promotes)("promotion_move"),
                Option.when(frontBlocked)("front_blockade_risk")
              ).flatten
          )
        }
      }

  private def simplificationConversionCandidate(
      board: Board,
      balance: Int,
      mv: Move
  ): Option[TransformationCandidate] =
    if !mv.captures || balance < 3 || mv.piece.role == King then None
    else
      board.pieceAt(mv.dest).filter(_.color == !mv.piece.color).flatMap { captured =>
        val favorableTrade = pieceValue(captured.role) >= pieceValue(mv.piece.role)
        Option.when(favorableTrade) {
          val score =
            (0.66 +
              balance.max(0).min(8) * 0.018 +
              (pieceValue(captured.role) - pieceValue(mv.piece.role)).max(0).min(5) * 0.02)
              .max(0.30)
              .min(0.86)
          TransformationCandidate(
            kind = PlanKind.SimplificationConversion,
            moveUci = mv.toUci.uci,
            role = mv.piece.role,
            from = mv.orig,
            to = mv.dest,
            materialBalance = balance,
            promotionDistanceAfter = None,
            score = score,
            reasons = List("material_edge_trade_down", s"material_balance:$balance", s"captured_role:${captured.role.toString.toLowerCase}")
          )
        }
      }

  private def invasionCandidate(
      balance: Int,
      mv: Move
  ): Option[TransformationCandidate] =
    val heavy = mv.piece.role == Rook || mv.piece.role == Queen
    val invasionSquare = isEnemyInvasionSquare(mv.dest, mv.piece.color)
    val attacked =
      attackedEnemyPieces(mv.after.board, mv.piece, mv.dest)
        .filter(square => mv.after.board.pieceAt(square).exists(piece => piece.color == !mv.piece.color && piece.role != King))
    val givesCheck = mv.after.check.yes
    val qualifies =
      heavy &&
        invasionSquare &&
        (givesCheck || attacked.nonEmpty)
    Option.when(qualifies) {
      val score =
        (0.64 +
          (if givesCheck then 0.07 else 0.0) +
          attacked.size.min(3) * 0.035 +
          balance.max(0).min(6) * 0.015)
          .max(0.30)
          .min(0.84)
      TransformationCandidate(
        kind = PlanKind.InvasionTransition,
        moveUci = mv.toUci.uci,
        role = mv.piece.role,
        from = mv.orig,
        to = mv.dest,
        materialBalance = balance,
        promotionDistanceAfter = None,
        score = score,
        reasons =
          List(
            Some("heavy_piece_invasion_square"),
            Option.when(givesCheck)("invasion_check"),
            Option.when(attacked.nonEmpty)(s"attacks_resources:${attacked.map(_.key).sorted.mkString(",")}"),
            Option.when(balance > 0)(s"material_balance:$balance")
          ).flatten
      )
    }

  private def oppositeBishopsConversionCandidate(
      board: Board,
      color: Color,
      balance: Int,
      oppositeBishops: Boolean,
      mv: Move
  ): Option[TransformationCandidate] =
    if !oppositeBishops || mv.piece.role != Pawn || !advancesTowardPromotion(mv, color) then None
    else
      val beforePassers = passedPawns(board, color)
      val afterPassers = passedPawns(mv.after.board, color)
      val wasPassed = isPassedPawn(board, mv.orig, color)
      val isPassedAfter = mv.promotion.nonEmpty || isPassedPawn(mv.after.board, mv.dest, color)
      val createsPassed = isPassedAfter && (!wasPassed || afterPassers.size > beforePassers.size)
      val distanceAfter = mv.promotion.fold(promotionDistance(mv.dest, color))(_ => 0)
      val qualifies =
        isPassedAfter &&
          (wasPassed || createsPassed) &&
          distanceAfter <= 4
      Option.when(qualifies) {
        val score =
          (0.62 +
            (if wasPassed then 0.05 else 0.0) +
            (if createsPassed then 0.06 else 0.0) +
            (4 - distanceAfter).max(0).min(4) * 0.025 +
            balance.max(0).min(5) * 0.015)
            .max(0.30)
            .min(0.84)
        TransformationCandidate(
          kind = PlanKind.OppositeBishopsConversion,
          moveUci = mv.toUci.uci,
          role = Pawn,
          from = mv.orig,
          to = mv.dest,
          materialBalance = balance,
          promotionDistanceAfter = Some(distanceAfter),
          score = score,
          reasons =
            List(
              Some("opposite_colored_bishops"),
              Option.when(wasPassed)("existing_passed_pawn"),
              Option.when(createsPassed)("creates_passed_pawn"),
              Some(s"promotion_distance_after:$distanceAfter")
            ).flatten
        )
      }

  private def toHypothesis(candidate: TransformationCandidate): PlanHypothesis =
    val moveText = displayMove(candidate.moveUci)
    val roleText = roleName(candidate.role)
    val planName =
      candidate.kind match
        case PlanKind.PasserConversion =>
          s"Advance the passed pawn with $moveText"
        case PlanKind.PassedPawnManufacture =>
          s"Create a passed pawn with $moveText"
        case PlanKind.SimplificationConversion =>
          s"Simplify toward conversion with $moveText"
        case PlanKind.InvasionTransition =>
          s"Invade with $moveText"
        case PlanKind.OppositeBishopsConversion =>
          s"Convert opposite-colored bishops with $moveText"
        case _ =>
          s"Transform the advantage with $moveText"
    val structuralTag =
      candidate.kind match
        case PlanKind.PasserConversion             => "passed_pawn_conversion"
        case PlanKind.PassedPawnManufacture        => "passed_pawn_manufacture"
        case PlanKind.SimplificationConversion     => "simplification_conversion"
        case PlanKind.InvasionTransition          => "invasion_transition"
        case PlanKind.OppositeBishopsConversion  => "opposite_bishops_conversion"
        case _                                   => "advantage_transformation"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = transformationWhyText(candidate, moveText, roleText).distinct.take(5),
      executionSteps = transformationExecutionText(candidate, moveText, roleText),
      failureModes = List(
        "the invasion square can be chased away immediately",
        "the passer or color-complex edge may be blockaded without concession"
      ),
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk = "transformation can fail if the defender immediately neutralizes the new asset"
      ),
      refutation = Some("change plans if the defender neutralizes the transformed edge"),
      evidenceSources = List(
        ThemeResolver.themeTag(PlanTheme.AdvantageTransformation),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag(structuralTag),
        "advantage_transformation"
      ) ++ candidate.reasons,
      themeL1 = PlanTheme.AdvantageTransformation.id,
      subplanId = Some(candidate.kind.id)
    )

  private def transformationWhyText(candidate: TransformationCandidate, moveText: String, roleText: String): List[String] =
    val resources = resourceTargets(candidate)
    val why =
      List(
        Some(s"$moveText is a concrete ${transformationMoveLabel(candidate.kind)}"),
        Option.when(candidate.reasons.contains("existing_passed_pawn"))(
          s"the $roleText is already a passed pawn"
        ),
        Option.when(candidate.reasons.contains("creates_passed_pawn"))(
          s"$moveText creates a passed pawn"
        ),
        reasonPayload(candidate.reasons, "passer_count:").map(count =>
          s"the passer count improves from ${count.replace("->", " to ")}"
        ),
        Option.when(candidate.reasons.contains("protected_passer"))(
          "the passer remains protected after the move"
        ),
        Option.when(candidate.reasons.contains("promotion_move"))(
          "the move promotes immediately"
        ),
        candidate.promotionDistanceAfter.map(distance =>
          if distance == 0 then "the pawn reaches promotion"
          else s"the pawn is ${distanceText(distance)} from promotion after the move"
        ),
        Option.when(candidate.reasons.contains("material_edge_trade_down"))(
          s"the side has a material edge worth simplifying"
        ),
        reasonPayload(candidate.reasons, "captured_role:").map(role =>
          s"the move trades off a ${role.replace('_', ' ')}"
        ),
        Option.when(candidate.reasons.contains("heavy_piece_invasion_square"))(
          s"${candidate.to.key} is an invasion square for the $roleText"
        ),
        Option.when(candidate.reasons.contains("invasion_check"))(
          "the invasion move gives check"
        ),
        Option.when(resources.nonEmpty)(
          s"the invasion attacks ${targetsText(resources)}"
        ),
        Option.when(candidate.reasons.contains("opposite_colored_bishops"))(
          "opposite-colored bishops make pawn conversion more important"
        ),
        Option.when(candidate.materialBalance > 0)(
          s"the side keeps a material edge of ${candidate.materialBalance}"
        )
      ).flatten
    if why.nonEmpty then why else List(s"$moveText changes the position into a more convertible asset")

  private def transformationExecutionText(candidate: TransformationCandidate, moveText: String, roleText: String): List[String] =
    val first =
      candidate.kind match
        case PlanKind.PasserConversion =>
          s"advance the passer with $moveText and keep it protected"
        case PlanKind.PassedPawnManufacture =>
          s"use $moveText to create the passer before the defender can blockade"
        case PlanKind.SimplificationConversion =>
          s"trade with $moveText while the material edge still converts"
        case PlanKind.InvasionTransition =>
          s"place the $roleText on ${candidate.to.key} with $moveText and keep it from being chased"
        case PlanKind.OppositeBishopsConversion =>
          s"push with $moveText before the opposite-colored bishop setup freezes the passer"
        case _ =>
          s"use $moveText as the concrete transformation move"
    val second =
      candidate.kind match
        case PlanKind.SimplificationConversion =>
          "keep the recapture line from improving the defender's coordination"
        case PlanKind.InvasionTransition =>
          "convert the invasion before the defender can drive the piece back"
        case _ =>
          "keep the transformed edge protected against chase or blockade"
    List(first, second).distinct

  private def transformationMoveLabel(kind: PlanKind): String =
    kind match
      case PlanKind.PasserConversion            => "passer conversion move"
      case PlanKind.PassedPawnManufacture       => "passed-pawn creation move"
      case PlanKind.SimplificationConversion    => "simplification move"
      case PlanKind.InvasionTransition          => "invasion move"
      case PlanKind.OppositeBishopsConversion   => "opposite-bishops conversion move"
      case _                                    => "advantage transformation move"

  private def resourceTargets(candidate: TransformationCandidate): List[String] =
    reasonPayload(candidate.reasons, "attacks_resources:")
      .toList
      .flatMap(_.split(",").toList)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .sorted

  private def targetsText(targets: List[String]): String =
    targets.take(3).mkString(", ")

  private def distanceText(distance: Int): String =
    s"$distance ${if distance == 1 then "move" else "moves"}"

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def displayMove(uci: String): String =
    if uci.length >= 4 then s"${uci.take(2)}-${uci.slice(2, 4)}" else uci

  private def roleName(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"

  private def isEnemyInvasionSquare(square: Square, color: Color): Boolean =
    if color.white then square.rank == Rank.Seventh || square.rank == Rank.Eighth
    else square.rank == Rank.Second || square.rank == Rank.First

  private def conversionBase(kind: PlanKind): Double =
    kind match
      case PlanKind.PasserConversion      => 0.63
      case PlanKind.PassedPawnManufacture => 0.61
      case _                              => 0.55

  private def hasOppositeColoredBishops(board: Board, color: Color): Boolean =
    val ours = board.byPiece(color, Bishop).squares.toList
    val theirs = board.byPiece(!color, Bishop).squares.toList
    ours.size == 1 &&
      theirs.size == 1 &&
      isLightSquare(ours.head) != isLightSquare(theirs.head)
