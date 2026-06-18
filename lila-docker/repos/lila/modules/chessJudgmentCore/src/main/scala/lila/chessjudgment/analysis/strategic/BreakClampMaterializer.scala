package lila.chessjudgment.analysis.strategic

import chess.*
import chess.format.{ Fen, Uci }
import chess.variant.Standard
import lila.chessjudgment.model.FactScope
import lila.chessjudgment.model.strategic.{ PreventedPlan, VariationLine }

object BreakClampMaterializer:

  enum BreakRouteKind:
    case QuietPush, CaptureBreak

  enum BreakTransformRisk:
    case None, CaptureTransform, SameRouteRestored, BranchUnstable

  enum BreakTransformShape:
    case SameDestinationCapture, SameDestinationQuiet, UnknownTransform

  enum BreakTransformVerdict:
    case UnansweredCapture, RecaptureAvailableUnproven, RecaptureStillReleases, RecaptureProvenHarmless

  final case class BreakTransformAssessment(
      routeId: String,
      shape: BreakTransformShape,
      verdict: BreakTransformVerdict,
      captureDestinationIsPlayedDestination: Boolean,
      immediateRecapture: Option[String],
      releaseRoutesAfterRecapture: List[String]
  )

  final case class BreakRouteEvidence(
      routeId: String,
      token: String,
      destinationToken: String,
      origin: Square,
      destination: Square,
      kind: BreakRouteKind,
      destinationIsPlayedDestination: Boolean,
      transformRisk: BreakTransformRisk,
      transformRoutes: List[String],
      transformAssessments: List[BreakTransformAssessment] = Nil,
      mobilityDelta: Int,
      sourceLine: Option[VariationLine],
      mechanismEvidenceCodes: List[String] = Nil
  )

  private final case class BreakCandidate(
      move: Move,
      routeId: String,
      token: String,
      destinationToken: String,
      origin: Square,
      destination: Square,
      kind: BreakRouteKind
  )

  private final case class RouteSnapshot(
      beforeBreaks: List[BreakCandidate],
      afterBreaks: List[BreakCandidate],
      afterPosition: Position,
      branchMoves: List[String],
      playedMove: Move
  )

  def materialize(
      fen: String,
      board: Board,
      color: Color,
      mainLine: VariationLine
  ): List[PreventedPlan] =
    routeEvidence(fen, board, color, mainLine)
      .filterNot(evidence =>
        evidence.transformRisk == BreakTransformRisk.SameRouteRestored ||
          evidence.transformRisk == BreakTransformRisk.BranchUnstable
      )
      .map { evidence =>
        val token =
          if evidence.transformRisk == BreakTransformRisk.CaptureTransform || evidence.destinationIsPlayedDestination
          then evidence.token
          else evidence.destinationToken
        PreventedPlan(
          planId = s"neutralize_${token.filter(_.isLetterOrDigit)}_break",
          deniedSquares = List(evidence.destination),
          breakNeutralized = Some(token),
          mobilityDelta = evidence.mobilityDelta,
          counterplayScoreDrop = 0,
          preventedThreatType = Some("Break"),
          deniedResourceClass = Some("break"),
          deniedEntryScope = Some("file"),
          sourceScope = FactScope.Now,
          sourceLine = evidence.sourceLine
        )
      }

  private[analysis] def routeEvidence(
      fen: String,
      board: Board,
      color: Color,
      mainLine: VariationLine
  ): List[BreakRouteEvidence] =
    routeSnapshots(fen, board, color, mainLine).flatMap { snapshot =>
      val clampedRoutes =
        snapshot.beforeBreaks
          .filterNot(candidate => snapshot.afterBreaks.exists(sameRoute(candidate, _)))
          .distinctBy(_.routeId)

      clampedRoutes.map { candidate =>
        val transformCandidates =
          snapshot.afterBreaks
            .filter(route => route.destination == candidate.destination && !sameRoute(candidate, route))
            .sortBy(_.routeId)
        val transformRoutes =
          transformCandidates
            .map(_.routeId)
            .distinct
        val transformAssessments =
          transformCandidates.map(transformAssessment(candidate, _, snapshot, color))
        val sameRouteInBranch = snapshot.branchMoves.flatMap(parseUci).exists(sameRouteMove(candidate, _))
        val sameRouteLegalLater =
          !sameRouteInBranch &&
            sameRouteReappears(snapshot.afterPosition, snapshot.branchMoves, candidate, color, snapshot.playedMove)
        val transformRisk =
          if sameRouteInBranch then BreakTransformRisk.SameRouteRestored
          else if sameRouteLegalLater then BreakTransformRisk.BranchUnstable
          else if transformRoutes.nonEmpty then BreakTransformRisk.CaptureTransform
          else BreakTransformRisk.None

        BreakRouteEvidence(
          routeId = candidate.routeId,
          token = candidate.token,
          destinationToken = candidate.destinationToken,
          origin = candidate.origin,
          destination = candidate.destination,
          kind = candidate.kind,
          destinationIsPlayedDestination = candidate.destination == snapshot.playedMove.dest,
          transformRisk = transformRisk,
          transformRoutes = transformRoutes,
          transformAssessments = transformAssessments,
          mobilityDelta = -1,
          sourceLine = Some(mainLine.copy(moves = mainLine.moves.take(4))),
          mechanismEvidenceCodes = mechanismEvidenceCodes(candidate, snapshot)
        )
      }
    }

  private[analysis] def routeStillLegal(
      fen: String,
      board: Board,
      color: Color,
      mainLine: VariationLine,
      token: String
  ): Boolean =
    routeSnapshots(fen, board, color, mainLine).exists { snapshot =>
      snapshot.beforeBreaks
        .filter(matchesToken(_, token))
        .exists(candidate => snapshot.afterBreaks.exists(sameRoute(candidate, _)))
    }

  private[analysis] def routeIdentityExists(
      fen: String,
      board: Board,
      color: Color,
      mainLine: VariationLine,
      token: String
  ): Boolean =
    routeSnapshots(fen, board, color, mainLine).exists(_.beforeBreaks.exists(matchesToken(_, token)))

  private def routeSnapshots(
      fen: String,
      board: Board,
      color: Color,
      mainLine: VariationLine
  ): List[RouteSnapshot] =
    mainLine.moves.headOption.flatMap(parseUci).toList.flatMap { playedUci =>
      Fen.read(Standard, Fen.Full(fen)).toList.filter(_.board == board).flatMap { rawPosition =>
        val position = if rawPosition.color == color then rawPosition else rawPosition.withColor(color)
        position.move(playedUci).toOption.toList.flatMap { playedMove =>
          if tacticalFirstMove(playedMove) then Nil
          else
            val opponent = !color
            val nullTurnPosition = position.withColor(opponent)
            val afterPosition =
              if playedMove.after.color == opponent then playedMove.after else playedMove.after.withColor(opponent)
            val beforeBreaks = legalContactBreaks(nullTurnPosition, color, playedMove)
            Option.when(beforeBreaks.nonEmpty)(
              RouteSnapshot(
                beforeBreaks = beforeBreaks,
                afterBreaks = legalContactBreaks(afterPosition, color, playedMove),
                afterPosition = afterPosition,
                branchMoves = mainLine.moves.drop(1).take(3),
                playedMove = playedMove
              )
            )
        }
      }
    }

  private def parseUci(uci: String): Option[Uci.Move] =
    Uci(uci).collect { case move: Uci.Move => move }

  private def tacticalFirstMove(move: Move): Boolean =
    move.captures || move.after.check.yes || move.promotion.isDefined

  private def legalContactBreaks(position: Position, activeColor: Color, playedMove: Move): List[BreakCandidate] =
    val activePawnSquares =
      position.board.byColor(activeColor).squares.filter { square =>
        position.board.pieceAt(square).exists(_.role == Pawn)
      }
    val playedSquares = Set(playedMove.orig, playedMove.dest)

    position.legalMoves.toList.flatMap { move =>
      Option.when(
        move.piece.role == Pawn &&
          !move.promotion.isDefined &&
          move.dest != playedMove.orig &&
          isContactBreak(move.dest, activePawnSquares, playedSquares)
      ) {
        val kind = if move.captures then BreakRouteKind.CaptureBreak else BreakRouteKind.QuietPush
        BreakCandidate(
          move = move,
          routeId = routeId(position.color, move.orig, move.dest, kind),
          token = routeToken(position.color, move.orig, move.dest),
          destinationToken = destinationToken(position.color, move.dest),
          origin = move.orig,
          destination = move.dest,
          kind = kind
        )
      }
    }

  private def isContactBreak(
      breakDestination: Square,
      activePawnSquares: List[Square],
      playedSquares: Set[Square]
  ): Boolean =
    activePawnSquares.exists(square => adjacent(square, breakDestination)) ||
      playedSquares.exists(square => square == breakDestination || adjacent(square, breakDestination))

  private def adjacent(a: Square, b: Square): Boolean =
    (a.file.value - b.file.value).abs <= 1 &&
      (a.rank.value - b.rank.value).abs <= 1 &&
      a != b

  private def destinationToken(breakColor: Color, destination: Square): String =
    if breakColor.black then s"...${destination.key}" else destination.key

  private def routeToken(breakColor: Color, origin: Square, destination: Square): String =
    val token = s"${origin.key}-${destination.key}"
    if breakColor.black then s"...$token" else token

  private def routeId(color: Color, origin: Square, destination: Square, kind: BreakRouteKind): String =
    s"${colorName(color)}:${origin.key}-${destination.key}:${kindId(kind)}"

  private def colorName(color: Color): String =
    if color.white then "white" else "black"

  private def kindId(kind: BreakRouteKind): String =
    kind match
      case BreakRouteKind.QuietPush    => "quiet_push"
      case BreakRouteKind.CaptureBreak => "capture_break"

  private def sameRoute(a: BreakCandidate, b: BreakCandidate): Boolean =
    a.origin == b.origin && a.destination == b.destination && a.kind == b.kind

  private def transformAssessment(
      candidate: BreakCandidate,
      transform: BreakCandidate,
      snapshot: RouteSnapshot,
      activeColor: Color
  ): BreakTransformAssessment =
    val recaptures =
      immediateRecaptures(transform.move.after, activeColor, transform.destination)
        .sortBy(moveId)
    val releaseRoutesAfterRecapture =
      recaptures
        .flatMap { recapture =>
          val opponent = transform.move.piece.color
          val afterRecapture =
            if recapture.after.color == opponent then recapture.after else recapture.after.withColor(opponent)
          legalContactBreaks(afterRecapture, activeColor, snapshot.playedMove)
            .filter(route => route.destination == candidate.destination)
            .map(_.routeId)
        }
        .distinct
        .sorted
    val sameDestinationCapture =
      transformShape(transform) == BreakTransformShape.SameDestinationCapture &&
        transform.destination == snapshot.playedMove.dest
    val branchProvesTransformRecapture =
      branchMoveId(snapshot, 0).contains(moveId(transform.move)) &&
        branchMoveId(snapshot, 1).exists(recaptures.map(moveId).toSet.contains)
    val verdict =
      if recaptures.isEmpty then BreakTransformVerdict.UnansweredCapture
      else if releaseRoutesAfterRecapture.nonEmpty then BreakTransformVerdict.RecaptureStillReleases
      else if sameDestinationCapture && branchProvesTransformRecapture then BreakTransformVerdict.RecaptureProvenHarmless
      else BreakTransformVerdict.RecaptureAvailableUnproven

    BreakTransformAssessment(
      routeId = transform.routeId,
      shape = transformShape(transform),
      verdict = verdict,
      captureDestinationIsPlayedDestination = transform.destination == snapshot.playedMove.dest,
      immediateRecapture = recaptures.headOption.map(moveId),
      releaseRoutesAfterRecapture = releaseRoutesAfterRecapture
    )

  private def mechanismEvidenceCodes(candidate: BreakCandidate, snapshot: RouteSnapshot): List[String] =
    val destinationOccupancy =
      Option.when(candidate.destination == snapshot.playedMove.dest)(
        List(
          "break_clamp_mechanism:occupied_destination",
          s"occupied_break_square:${candidate.destination.key}"
        )
      ).getOrElse(Nil)
    (destinationOccupancy ++ pinnedPawnEvidenceCodes(candidate, snapshot)).distinct

  private def pinnedPawnEvidenceCodes(candidate: BreakCandidate, snapshot: RouteSnapshot): List[String] =
    val board = snapshot.afterPosition.board
    val attackerSquare = snapshot.playedMove.dest
    val pinnedSquare = candidate.origin
    val breakColor = candidate.move.piece.color
    val pieceAtPinnedSquare =
      board.pieceAt(pinnedSquare).filter(piece => piece.color == breakColor && piece.role == Pawn)
    val pin =
      for
        attacker <- board.pieceAt(attackerSquare)
        if attacker.color == snapshot.playedMove.piece.color
        if slidingRole(attacker.role)
        _ <- pieceAtPinnedSquare
        king <- board.kingPosOf(breakColor)
        ray <- rayBetween(attackerSquare, king)
        if ray.squares.contains(pinnedSquare)
        if !ray.squares.contains(candidate.destination)
        occupied = ray.squares.filter(square => board.pieceAt(square).nonEmpty)
        if occupied == List(pinnedSquare)
        if roleCanPinAlong(attacker.role, ray)
      yield List(
        "break_clamp_mechanism:pinned_pawn",
        s"pinned_break_pawn:${pinnedSquare.key}",
        s"pin_attacker:${attackerSquare.key}",
        s"pin_king:${king.key}",
        s"break_route:${candidate.token}",
        s"break_destination:${candidate.destination.key}"
      )
    pin.getOrElse(Nil)

  private final case class Ray(squares: List[Square], diagonal: Boolean, orthogonal: Boolean)

  private def rayBetween(from: Square, to: Square): Option[Ray] =
    val fileDelta = to.file.value - from.file.value
    val rankDelta = to.rank.value - from.rank.value
    val diagonal = fileDelta.abs == rankDelta.abs && fileDelta != 0
    val orthogonal = (fileDelta == 0 && rankDelta != 0) || (rankDelta == 0 && fileDelta != 0)
    Option.when(diagonal || orthogonal) {
      val fileStep = if fileDelta == 0 then 0 else fileDelta / fileDelta.abs
      val rankStep = if rankDelta == 0 then 0 else rankDelta / rankDelta.abs
      val distance = math.max(fileDelta.abs, rankDelta.abs)
      val squares =
        (1 until distance).toList.flatMap { step =>
          squareAt(from.file.value + fileStep * step, from.rank.value + rankStep * step)
        }
      Option.when(squares.size == distance - 1)(Ray(squares, diagonal = diagonal, orthogonal = orthogonal))
    }.flatten

  private def squareAt(file: Int, rank: Int): Option[Square] =
    Square.all.find(square => square.file.value == file && square.rank.value == rank)

  private def slidingRole(role: Role): Boolean =
    role == Bishop || role == Rook || role == Queen

  private def roleCanPinAlong(role: Role, ray: Ray): Boolean =
    role == Queen ||
      (role == Bishop && ray.diagonal) ||
      (role == Rook && ray.orthogonal)

  private def transformShape(transform: BreakCandidate): BreakTransformShape =
    if transform.move.captures then BreakTransformShape.SameDestinationCapture
    else if transform.kind == BreakRouteKind.QuietPush then BreakTransformShape.SameDestinationQuiet
    else BreakTransformShape.UnknownTransform

  private def immediateRecaptures(position: Position, activeColor: Color, destination: Square): List[Move] =
    val activePosition = if position.color == activeColor then position else position.withColor(activeColor)
    activePosition.legalMoves.toList.filter { move =>
      move.piece.color == activeColor &&
        move.captures &&
        move.dest == destination
    }

  private def branchMoveId(snapshot: RouteSnapshot, index: Int): Option[String] =
    snapshot.branchMoves.lift(index).flatMap(parseUci).map(move => s"${move.orig.key}${move.dest.key}")

  private def moveId(move: Move): String =
    s"${move.orig.key}${move.dest.key}"

  private def matchesToken(candidate: BreakCandidate, token: String): Boolean =
    normalize(candidate.token) == normalize(token) ||
      normalize(candidate.destinationToken) == normalize(token)

  private def normalize(raw: String): String =
    Option(raw).map(_.trim.toLowerCase).getOrElse("")

  private def sameRouteMove(candidate: BreakCandidate, move: Uci.Move): Boolean =
    move.orig == candidate.origin && move.dest == candidate.destination

  private def sameRouteReappears(
      start: Position,
      branchMoves: List[String],
      candidate: BreakCandidate,
      activeColor: Color,
      playedMove: Move
  ): Boolean =
    branchMoves
      .scanLeft(Option(start)) { (posOpt, uci) =>
        for
          pos <- posOpt
          move <- parseUci(uci)
          next <- pos.move(move).toOption.map(_.after)
        yield next
      }
      .drop(1)
      .flatten
      .exists { pos =>
        pos.color == candidate.move.piece.color &&
          legalContactBreaks(pos, activeColor, playedMove).exists(sameRoute(candidate, _))
      }
