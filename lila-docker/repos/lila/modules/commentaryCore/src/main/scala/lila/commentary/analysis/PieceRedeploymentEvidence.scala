package lila.commentary.analysis

import chess.*
import chess.Bitboard
import chess.format.Fen
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.analysis.strategic.ActivityAnalyzerImpl
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object PieceRedeploymentEvidence:

  final case class RedeploymentCandidate(
      kind: PlanKind,
      role: Role,
      from: Square,
      route: List[Square],
      destination: Square,
      mobilityBefore: Int,
      mobilityAfter: Int,
      score: Double,
      reasons: List[String]
  ):
    def mobilityGain: Int = mobilityAfter - mobilityBefore

  private val Analyzer = ActivityAnalyzerImpl()

  def candidatesFromFen(fen: String, color: Color): List[RedeploymentCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap(pos => candidates(pos, color))

  def candidates(pos: Position, color: Color): List[RedeploymentCandidate] =
    val oriented = orientedPosition(pos, color)
    combinedCandidates(
      routeCandidates = candidates(oriented.board, color),
      filePressureCandidates = openFilePressureCandidates(oriented, color)
    )

  def candidates(board: Board, color: Color): List[RedeploymentCandidate] =
    Analyzer
      .analyze(board, color)
      .flatMap { activity =>
        board.pieceAt(activity.square).filter(piece => piece.color == color && roleCanRedeploy(piece.role)).flatMap { piece =>
          val route = activity.keyRoutes.distinct
          val destination = route.lastOption
          destination.flatMap { dest =>
            val beforeMobility = pieceMobility(board, piece, activity.square)
            val projected = pieceMobility(board, piece, dest)
            val kind = candidateKind(board, color, activity.piece, activity.isBadBishop, activity.square, dest)
            val reasons =
              candidateReasons(
                board = board,
                color = color,
                role = activity.piece,
                isBadBishop = activity.isBadBishop,
                isTrapped = activity.isTrapped,
                fromMobility = beforeMobility,
                toMobility = projected,
                destination = dest
              )
            val score = candidateScore(activity.mobilityScore, route.size, reasons.size)
            Option.when(planCandidateAdmissible(kind, activity.piece, activity.isTrapped, beforeMobility, projected, reasons))(
              RedeploymentCandidate(
              kind = kind,
              role = activity.piece,
              from = activity.square,
              route = route,
              destination = dest,
              mobilityBefore = beforeMobility,
              mobilityAfter = projected,
              score = score,
              reasons = reasons
              )
            )
          }
        }
      }
      .filter(c => c.score >= 0.54 && c.route.nonEmpty)

  private def combinedCandidates(
      routeCandidates: List[RedeploymentCandidate],
      filePressureCandidates: List[RedeploymentCandidate]
  ): List[RedeploymentCandidate] =
    (routeCandidates ++ filePressureCandidates)
      .sortBy(c => (-c.score, c.kind.id, c.from.key, c.destination.key))
      .distinctBy(c => s"${c.kind.id}|${c.role}|${c.from.key}|${c.destination.key}")

  def planHypotheses(fen: String, color: Color): List[PlanHypothesis] =
    candidatesFromFen(fen, color)
      .groupBy(_.kind)
      .values
      .toList
      .flatMap(_.sortBy(c => -c.score).headOption)
      .sortBy(c => -c.score)
      .take(3)
      .map(toHypothesis)

  def movesForSubplan(pos: Position, subplan: PlanKind, legalMoves: List[Move]): List[Move] =
    legalMoves
      .flatMap(moveCandidate(pos.board, subplan, _))
      .sortBy { case (_, score) => -score }
      .map(_._1)
      .distinct

  private def toHypothesis(candidate: RedeploymentCandidate): PlanHypothesis =
    val planName = candidate.kind match
      case PlanKind.BishopReanchor =>
        s"Re-anchor the bishop from ${candidate.from.key} toward ${candidate.destination.key}"
      case PlanKind.RookFileTransfer | PlanKind.OpenFilePressure =>
        s"Transfer the ${roleName(candidate.role)} from ${candidate.from.key} toward ${candidate.destination.file.char}-file pressure"
      case PlanKind.OutpostEntrenchment =>
        s"Improve the ${roleName(candidate.role)} toward the outpost on ${candidate.destination.key}"
      case _ =>
        s"Improve the ${roleName(candidate.role)} from ${candidate.from.key} toward ${candidate.destination.key}"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = redeploymentWhyText(candidate).distinct.take(5),
      executionSteps = redeploymentExecutionText(candidate),
      failureModes = List(
        "the route is too slow if an immediate tactic appears",
        "the target square can lose value if the opponent opens the center first"
      ),
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.70 then "high" else "medium",
        risk = "piece route must hold against the defender's best reply"
      ),
      refutation = Some("switch plans if the route gives up concrete play"),
      evidenceSources = List(
        ThemeResolver.themeTag(PlanTheme.PieceRedeployment),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag("piece_redeployment"),
        "piece_activity"
      ) ++ candidate.reasons,
      themeL1 = PlanTheme.PieceRedeployment.id,
      subplanId = Some(candidate.kind.id)
    )

  private def redeploymentWhyText(candidate: RedeploymentCandidate): List[String] =
    val roleText = roleName(candidate.role)
    val pressureFile = reasonPayload(candidate.reasons, "pressure_file:").getOrElse(candidate.destination.file.char.toString)
    val targets = fileTargets(candidate)
    val why =
      List(
        Option.when(candidate.mobilityBefore <= worstPieceMobilityFloor(candidate.role))(
          s"the $roleText on ${candidate.from.key} is short of active squares"
        ),
        Option.when(candidate.mobilityGain > 0)(
          s"the route raises $roleText mobility from ${candidate.mobilityBefore} to ${candidate.mobilityAfter}"
        ),
        Option.when(candidate.reasons.contains("bad bishop route"))(
          "the bishop can leave a same-color pawn bind"
        ),
        Option.when(candidate.reasons.contains("piece is currently trapped"))(
          s"the $roleText currently has too few practical exits"
        ),
        Option.when(candidate.reasons.contains("open_or_semi_open_file"))(
          if targets.nonEmpty then s"the $pressureFile-file gives the $roleText pressure against ${targetsText(targets)}"
          else s"the $pressureFile-file is open enough for $roleText pressure"
        ),
        Option.when(candidate.reasons.contains("outpost_target"))(
          s"${candidate.destination.key} can serve as a protected outpost for the $roleText"
        ),
        Option.when(candidate.reasons.contains("central_route"))(
          s"the route brings the $roleText closer to the center"
        )
      ).flatten
    if why.nonEmpty then why
    else List(s"the $roleText can improve from ${candidate.from.key} toward ${candidate.destination.key}")

  private def redeploymentExecutionText(candidate: RedeploymentCandidate): List[String] =
    val roleText = roleName(candidate.role)
    val route = routeText(candidate)
    val pressureFile = reasonPayload(candidate.reasons, "pressure_file:").getOrElse(candidate.destination.file.char.toString)
    val targets = fileTargets(candidate)
    val first =
      candidate.kind match
        case PlanKind.BishopReanchor =>
          s"reroute the bishop via $route so it works outside the pawn bind"
        case PlanKind.RookFileTransfer =>
          s"shift the rook via $route before committing to $pressureFile-file pressure"
        case PlanKind.OpenFilePressure =>
          s"place the $roleText on ${candidate.destination.key} to pressure the $pressureFile-file"
        case PlanKind.OutpostEntrenchment =>
          s"move the $roleText via $route and keep ${candidate.destination.key} protected"
        case _ =>
          s"improve the $roleText via $route"
    val second =
      if candidate.reasons.contains("open_or_semi_open_file") && targets.nonEmpty then
        Some(s"coordinate the pressure around ${targetsText(targets)} before the defender can cover it")
      else if candidate.reasons.contains("outpost_target") then
        Some(s"support ${candidate.destination.key} before using it as a lasting square")
      else if candidate.reasons.contains("central_route") then
        Some("keep the route ready for central play instead of forcing it immediately")
      else
        Some("avoid forcing play until the improved piece has a stable square")
    (first :: second.toList).distinct

  private def routeText(candidate: RedeploymentCandidate): String =
    candidate.route.map(_.key).mkString(" -> ") match
      case ""   => candidate.destination.key
      case text => text

  private def fileTargets(candidate: RedeploymentCandidate): List[String] =
    reasonPayload(candidate.reasons, "file_targets:")
      .toList
      .flatMap(_.split(",").toList)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .sorted

  private def targetsText(targets: List[String]): String =
    targets.take(3).mkString(", ")

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def candidateKind(
      board: Board,
      color: Color,
      role: Role,
      isBadBishop: Boolean,
      from: Square,
      destination: Square
  ): PlanKind =
    if role == Bishop && isBadBishop then PlanKind.BishopReanchor
    else if role == Rook && from.file != destination.file && isOpenOrSemiOpenFileFor(board, destination.file, color) then PlanKind.RookFileTransfer
    else if (role == Knight || role == Bishop) && isOutpostSquare(board, destination, color) then PlanKind.OutpostEntrenchment
    else PlanKind.WorstPieceImprovement

  private def candidateReasons(
      board: Board,
      color: Color,
      role: Role,
      isBadBishop: Boolean,
      isTrapped: Boolean,
      fromMobility: Int,
      toMobility: Int,
      destination: Square
  ): List[String] =
    List(
      Option.when(isBadBishop)("bad bishop route"),
      Option.when(isTrapped)("piece is currently trapped"),
      Option.when(toMobility >= fromMobility + 2)(s"mobility_gain:${toMobility - fromMobility}"),
      Option.when(role == Rook && isOpenOrSemiOpenFileFor(board, destination.file, color))("open_or_semi_open_file"),
      Option.when((role == Knight || role == Bishop) && isOutpostSquare(board, destination, color))("outpost_target"),
      Option.when(isCentralSquare(destination))("central_route")
    ).flatten

  private def candidateScore(mobilityScore: Double, routeLength: Int, reasonCount: Int): Double =
    (0.50 +
      ((0.55 - mobilityScore).max(0.0) * 0.28) +
      routeLength.min(3) * 0.045 +
      reasonCount.min(4) * 0.035)
      .max(0.30)
      .min(0.86)

  private def moveCandidate(board: Board, subplan: PlanKind, mv: Move): Option[(Move, Double)] =
    val role = mv.piece.role
    val color = mv.piece.color
    val beforeMobility = pieceMobility(board, mv.piece, mv.orig)
    val afterMobility = projectedMobility(board, mv.piece, mv.orig, mv.dest)
    val mobilityGain = afterMobility - beforeMobility
    val badBefore = isBadBishop(board, mv.piece, mv.orig, beforeMobility)
    val badAfter = isBadBishop(board, mv.piece, mv.dest, afterMobility)
    val outpost = isOutpostSquare(board, mv.dest, color)
    val lineFile = isOpenOrSemiOpenFileFor(board, mv.dest.file, color)
    val central = isCentralSquare(mv.dest)
    val quiet = !mv.captures && mv.promotion.isEmpty
    val base =
      subplan match
        case PlanKind.BishopReanchor if role == Bishop && quiet =>
          Option.when((badBefore && !badAfter) || mobilityGain >= 2 || central)(0.50)
        case PlanKind.WorstPieceImprovement if quiet && roleCanRedeploy(role) =>
          val lowActivity = beforeMobility <= worstPieceMobilityFloor(role)
          val improvesActivity = mobilityGain >= 2
          val minorCentralRoute = (role == Knight || role == Bishop) && (central || outpost)
          val queenRecovery = role == Queen && lowActivity && improvesActivity
          Option.when((role != Queen && (lowActivity || improvesActivity || minorCentralRoute)) || queenRecovery)(0.46)
        case PlanKind.RookFileTransfer if role == Rook && quiet =>
          Option.when(mv.orig.file != mv.dest.file && (lineFile || mobilityGain >= 2))(0.48)
        case PlanKind.OpenFilePressure if (role == Rook || role == Queen) =>
          val targets = visibleFilePressureTargets(board, color, mv.dest, Some(mv.orig))
          Option.when((lineFile && targets.nonEmpty) || (mv.captures && mv.dest.file == mv.orig.file))(0.48)
        case PlanKind.OutpostEntrenchment if quiet && (role == Knight || role == Bishop) =>
          Option.when(outpost)(0.52)
        case _ => None
    base.map { start =>
      val score =
        start +
          mobilityGain.max(0).min(5) * 0.045 +
          (if badBefore && !badAfter then 0.12 else 0.0) +
          (if outpost then 0.10 else 0.0) +
          (if lineFile then 0.08 else 0.0) +
          (if central then 0.04 else 0.0)
      mv -> score
    }.filter(_._2 >= 0.50)

  private def openFilePressureCandidates(pos: Position, color: Color): List[RedeploymentCandidate] =
    val board = pos.board
    pos.legalMoves.toList.flatMap { mv =>
      val role = mv.piece.role
      val heavy = role == Rook || role == Queen
      val quietOrSameFileCapture = !mv.captures || mv.dest.file == mv.orig.file
      if mv.piece.color != color || !heavy || mv.promotion.nonEmpty || !quietOrSameFileCapture then None
      else
        val lineFile = isOpenOrSemiOpenFileFor(board, mv.dest.file, color)
        val targets = visibleFilePressureTargets(board, color, mv.dest, Some(mv.orig))
        val beforeMobility = pieceMobility(board, mv.piece, mv.orig)
        val afterMobility = projectedMobility(board, mv.piece, mv.orig, mv.dest)
        val mobilityGain = afterMobility - beforeMobility
        val changedFile = mv.orig.file != mv.dest.file
        val qualifies =
          lineFile &&
            targets.nonEmpty &&
            (changedFile || mobilityGain >= 1 || mv.captures)
        Option.when(qualifies) {
          val score =
            (0.64 +
              targets.size.min(2) * 0.04 +
              mobilityGain.max(0).min(4) * 0.025 +
              (if changedFile then 0.03 else 0.0) +
              (if mv.captures then 0.04 else 0.0))
              .max(0.30)
              .min(0.84)
          RedeploymentCandidate(
            kind = PlanKind.OpenFilePressure,
            role = role,
            from = mv.orig,
            route = List(mv.dest),
            destination = mv.dest,
            mobilityBefore = beforeMobility,
            mobilityAfter = afterMobility,
            score = score,
            reasons =
              List(
                "open_or_semi_open_file",
                s"pressure_file:${mv.dest.file.char}",
                s"file_targets:${targets.map(_.key).sorted.mkString(",")}",
                Option.when(changedFile)("file_transfer").getOrElse("same_file_pressure"),
                Option.when(mobilityGain > 0)(s"mobility_gain:$mobilityGain").getOrElse("")
              ).filter(_.nonEmpty)
          )
        }
    }

  private def mobilityFloor(role: Role): Int =
    role match
      case Knight => 4
      case Bishop => 5
      case Rook   => 6
      case Queen  => 9
      case _      => 4

  private def worstPieceMobilityFloor(role: Role): Int =
    role match
      case Queen => 5
      case other => mobilityFloor(other)

  private def roleCanRedeploy(role: Role): Boolean =
    role == Knight || role == Bishop || role == Rook || role == Queen

  private def planCandidateAdmissible(
      kind: PlanKind,
      role: Role,
      isTrapped: Boolean,
      beforeMobility: Int,
      afterMobility: Int,
      reasons: List[String]
  ): Boolean =
    kind match
      case PlanKind.WorstPieceImprovement =>
        val mobilityGain = afterMobility - beforeMobility
        val lowActivity = beforeMobility <= worstPieceMobilityFloor(role)
        val improvesActivity = mobilityGain >= 2
        val minorRoute = (role == Knight || role == Bishop) && reasons.exists(reason => reason == "central_route" || reason == "outpost_target")
        val queenRecovery = role == Queen && (isTrapped || (lowActivity && improvesActivity))
        (role != Queen && (isTrapped || lowActivity || improvesActivity || minorRoute)) || queenRecovery
      case _ =>
        true

  private def isBadBishop(board: Board, piece: Piece, square: Square, mobilityCount: Int): Boolean =
    piece.role == Bishop && {
      val ownPawns = board.byPiece(piece.color, Pawn).squares
      val bishopLight = isLightSquare(square)
      val sameColorPawnCount = ownPawns.count(p => isLightSquare(p) == bishopLight)
      sameColorPawnCount >= 3 && mobilityCount <= 5
    }

  private def filePressureTargets(board: Board, color: Color, file: File): List[Square] =
    (board.byColor(!color) & Bitboard.file(file)).squares.toList.filter { square =>
      board.pieceAt(square).exists(piece => piece.color == !color && piece.role != King)
    }

  private def visibleFilePressureTargets(board: Board, color: Color, from: Square, emptySquare: Option[Square]): List[Square] =
    filePressureTargets(board, color, from.file).filter(target => clearFileRay(board, from, target, emptySquare))

  private def clearFileRay(board: Board, from: Square, target: Square, emptySquare: Option[Square]): Boolean =
    from.file == target.file && {
      val low = math.min(from.rank.value, target.rank.value) + 1
      val high = math.max(from.rank.value, target.rank.value) - 1
      (low to high).forall { rank =>
        Square.at(from.file.value, rank).forall { sq =>
          emptySquare.contains(sq) || board.pieceAt(sq).isEmpty
        }
      }
    }

  private def roleName(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"
