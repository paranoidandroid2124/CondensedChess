package lila.commentary.analysis

import chess.*
import chess.format.{ Fen, Uci }
import chess.variant.Standard
import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

private[commentary] object CentralBreakTimingWitness:

  enum Support:
    case BoardBacked
    case PlanOnly

  enum PracticalKind:
    case Liquidation
    case Challenge

  object Failure:
    val NoPv = "central_break_timing:no_pv"
    val PvGapTooSmall = "central_break_timing:pv_gap_below_40"
    val NoCentralBreak = "central_break_timing:no_central_break"
    val DiagonalCapture = "central_break_timing:diagonal_capture_liquidation"
    val PrepOrChallenge = "central_break_timing:prep_or_challenge"
    val NoBoardLink = "central_break_timing:no_board_link"
    val PlanOnly = "central_break_timing:plan_only"
    val BranchMissing = "central_break_timing:branch_missing"

  final case class Witness(
      support: Support,
      breakMove: String,
      breakSquare: String,
      breakToken: String,
      pvGapCp: Int,
      sourceTags: List[String],
      ownerSeedTerms: List[String],
      structureTransitionTerms: List[String]
  ):
    def releasable: Boolean = support == Support.BoardBacked

  final case class Diagnosis(
      releasable: Option[Witness],
      reviewOnly: List[Witness],
      failureCodes: List[String]
  )

  final case class PracticalMove(kind: PracticalKind, token: String)

  val ProofSource: String = PlanTaxonomy.PlanKind.CentralBreakTiming.id
  val ProofFamily: String = PlanTaxonomy.PlanKind.CentralBreakTiming.id
  val PvGapThresholdCp: Int = 40
  private val BreakHorizonPly = 3

  def diagnose(ctx: NarrativeContext): Diagnosis =
    val variations = ctx.engineEvidence.toList.flatMap(_.variations)
    val parsed = Fen.read(Standard, Fen.Full(ctx.fen))
    (parsed, variations.headOption) match
      case (Some(position), Some(pv1)) =>
        val pvGap = pvGapForMover(position.color, variations)
        val played = playedMove(ctx, pv1)
        val breakCandidate =
          playedDirectCentralBreak(position, played)
            .orElse(firstCentralBreak(position, pv1))
        val replay =
          MoveReviewExchangeAnalyzer
            .boundedReplay(ctx.fen, normalizedPvMoves(pv1), maxPlies = BreakHorizonPly + 1)
            .getOrElse(Nil)
        val branch = MoveReviewExchangeAnalyzer.branchKey(replay)
        val gap = pvGap.getOrElse(0)
        val shapeFailure =
          played
            .flatMap(uci => legalMove(position, uci))
            .flatMap(move => rejectedCentralBreakShape(move, position.color))
            .orElse(firstRejectedCentralBreakShape(position, pv1))
        val failureCodes =
          List(
            Option.when(breakCandidate.isEmpty)(shapeFailure.getOrElse(Failure.NoCentralBreak))
          ).flatten
        val witnesses =
          for
            candidate <- breakCandidate.toList
            playedUci <- played.toList
            playedMove <- legalMove(position, playedUci).toList
          yield buildWitness(
            position = position,
            playedMove = playedMove,
            candidate = candidate,
            gap = gap,
            branchMissing = branch.isEmpty,
            branchKey = branch,
            replayMoves = replay.map(_.uci)
          )
        val (releasable, reviewOnly) = witnesses.partition(_.releasable)
        val boardFailure =
          Option.when(witnesses.nonEmpty && releasable.isEmpty)(Failure.NoBoardLink)
        val planOnlyFailure =
          Option.when(reviewOnly.nonEmpty && releasable.isEmpty)(Failure.PlanOnly)
        Diagnosis(
          releasable = releasable.headOption,
          reviewOnly = reviewOnly,
          failureCodes = (failureCodes ++ boardFailure ++ planOnlyFailure).distinct
        )
      case _ =>
        Diagnosis(None, Nil, List(Failure.NoPv))

  def exact(ctx: NarrativeContext): Option[Witness] =
    diagnose(ctx).releasable

  def candidate(ctx: NarrativeContext): Option[Witness] =
    val diagnosis = diagnose(ctx)
    diagnosis.releasable.orElse(diagnosis.reviewOnly.headOption)

  def practical(ctx: NarrativeContext): Option[PracticalMove] =
    for
      position <- Fen.read(Standard, Fen.Full(ctx.fen))
      uci <- ctx.playedMove.map(normalizeUci).filter(isUci)
      move <- legalMove(position, uci)
      failure <- rejectedCentralBreakShape(move, position.color)
    yield
      val kind =
        if failure == Failure.DiagonalCapture then PracticalKind.Liquidation
        else PracticalKind.Challenge
      PracticalMove(kind, breakTokenFor(position.color, move))

  def anchorTerms(ctx: NarrativeContext): List[String] =
    exact(ctx).toList.flatMap(_.ownerSeedTerms).distinct

  def failureCodes(ctx: NarrativeContext): List[String] =
    diagnose(ctx).failureCodes

  private final case class BreakCandidate(
      move: Move,
      uci: String,
      plyIndex: Int,
      before: Position,
      fromPlayedMove: Boolean = false
  )

  private def buildWitness(
      position: Position,
      playedMove: Move,
      candidate: BreakCandidate,
      gap: Int,
      branchMissing: Boolean,
      branchKey: Option[String],
      replayMoves: List[String]
  ): Witness =
    val playedIsBreak =
      sameMove(playedMove, candidate.move)
    val hasBoardLink =
      playedIsBreak ||
        moveSupportsBreak(playedMove, candidate, position.color) ||
        clearsBreakSquare(playedMove, candidate) ||
        removesImmediateRefutation(playedMove, candidate, position.color)
    val support =
      if hasBoardLink then Support.BoardBacked else Support.PlanOnly
    val breakSquare = candidate.move.dest.key
    val breakToken = breakTokenFor(position.color, candidate.move)
    val sourceTags =
      (
          List(ProofFamily) ++
          Option.when(gap < PvGapThresholdCp)(s"diagnostic:${Failure.PvGapTooSmall}").toList ++
          Option.when(branchMissing)(s"diagnostic:${Failure.BranchMissing}").toList ++
          Option.when(candidate.fromPlayedMove)("board:played_move_direct").toList ++
          Option.when(playedIsBreak)("board:played_break").toList ++
          Option.when(!playedIsBreak && hasBoardLink)("board:break_support").toList ++
          Option.when(!hasBoardLink)("plan_only").toList
      ).distinct
    Witness(
      support = support,
      breakMove = candidate.uci,
      breakSquare = breakSquare,
      breakToken = breakToken,
      pvGapCp = gap,
      sourceTags = sourceTags,
      ownerSeedTerms =
        (
          List(
            ProofFamily,
            breakToken,
            breakSquare,
            s"break_square:$breakSquare",
            s"pv_gap:$gap"
          ) ++ sourceTags
        ).distinct,
      structureTransitionTerms =
        (
          List(
            "central_break_timing_branch",
            s"central_break:$breakSquare",
            s"break_token:$breakToken",
            s"break_move:${candidate.uci}"
          ) ++
            branchKey.flatMap(MoveReviewExchangeAnalyzer.bestBranchFactFromKey).toList ++
            MoveReviewPvLine.pvMoveTerms(replayMoves.take(BreakHorizonPly + 1))
        ).distinct
    )

  private def firstCentralBreak(position: Position, pv: VariationLine): Option[BreakCandidate] =
    val initialSide = position.color
    val moves = normalizedPvMoves(pv).take(BreakHorizonPly)
    moves.zipWithIndex.foldLeft((position, Option.empty[BreakCandidate])) {
      case ((current, found), _) if found.nonEmpty => (current, found)
      case ((current, _), (uci, index)) =>
        legalMove(current, uci) match
          case Some(move) =>
            val found =
              Option.when(current.color == initialSide && isCentralBreak(move, initialSide)) {
                BreakCandidate(move = move, uci = uci, plyIndex = index, before = current)
              }
            (move.after, found)
          case None => (current, None)
    }._2

  private def playedDirectCentralBreak(
      position: Position,
      played: Option[String]
  ): Option[BreakCandidate] =
    played
      .flatMap(uci => legalMove(position, uci).map(uci -> _))
      .collect {
        case (uci, move) if isCentralBreak(move, position.color) =>
          BreakCandidate(move = move, uci = uci, plyIndex = 0, before = position, fromPlayedMove = true)
      }

  private def normalizedPvMoves(pv: VariationLine): List[String] =
    val raw =
      if pv.moves.nonEmpty then pv.moves
      else pv.parsedMoves.flatMap(move => clean(move.uci))
    raw.map(normalizeUci).filter(isUci)

  private def playedMove(ctx: NarrativeContext, pv: VariationLine): Option[String] =
    ctx.playedMove
      .map(normalizeUci)
      .filter(isUci)
      .orElse(normalizedPvMoves(pv).headOption)

  private def legalMove(position: Position, uci: String): Option[Move] =
    Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)

  private def pvGapForMover(color: Color, variations: List[VariationLine]): Option[Int] =
    for
      top <- variations.headOption
      second <- variations.lift(1)
    yield PerspectiveMath.improvementForMover(color, top.scoreCp, second.scoreCp)

  private def firstRejectedCentralBreakShape(position: Position, pv: VariationLine): Option[String] =
    val initialSide = position.color
    val moves = normalizedPvMoves(pv).take(BreakHorizonPly)
    moves.zipWithIndex.foldLeft((position, Option.empty[String])) {
      case ((current, found), _) if found.nonEmpty => (current, found)
      case ((current, _), (uci, _)) =>
        legalMove(current, uci) match
          case Some(move) =>
            val found =
              Option.when(current.color == initialSide)(rejectedCentralBreakShape(move, initialSide)).flatten
            (move.after, found)
          case None => (current, None)
    }._2

  private def rejectedCentralBreakShape(move: Move, side: Color): Option[String] =
    if !centralPawnAdvanceCandidate(move, side) then None
    else if move.captures && centralFile(move.dest) then Some(Failure.DiagonalCapture)
    else if sameFile(move) && centralFile(move.dest) && !coreCentralDestination(move.dest) then Some(Failure.PrepOrChallenge)
    else None

  private def isCentralBreak(move: Move, side: Color): Boolean =
    centralPawnAdvanceCandidate(move, side) &&
      !move.captures &&
      sameFile(move) &&
      centralFile(move.dest) &&
      coreCentralDestination(move.dest) &&
      adjacentEnemyPawn(move.after.board, side, move.dest)

  private def centralPawnAdvanceCandidate(move: Move, side: Color): Boolean =
    move.piece.role == Pawn &&
      move.piece.color == side &&
      isForward(move, side) &&
      centralFile(move.orig)

  private def isForward(move: Move, side: Color): Boolean =
    if side.white then move.dest.rank.value > move.orig.rank.value
    else move.dest.rank.value < move.orig.rank.value

  private def centralFile(square: Square): Boolean =
    square.file.value == 3 || square.file.value == 4

  private def sameFile(move: Move): Boolean =
    move.orig.file == move.dest.file

  private def coreCentralDestination(square: Square): Boolean =
    Set("d4", "e4", "d5", "e5").contains(square.key)

  private def adjacentEnemyPawn(board: Board, side: Color, square: Square): Boolean =
    Square.all.exists(candidate =>
      adjacent(candidate, square) &&
        board.pieceAt(candidate).exists(piece => piece.color != side && piece.role == Pawn)
    )

  private def moveSupportsBreak(playedMove: Move, candidate: BreakCandidate, side: Color): Boolean =
    playedMove.piece.role == Pawn &&
      playedMove.piece.color == side &&
      candidate.move.orig != playedMove.orig &&
      playedMove.after.board.attackers(candidate.move.dest, side).exists(_ == playedMove.dest)

  private def clearsBreakSquare(playedMove: Move, candidate: BreakCandidate): Boolean =
    playedMove.orig == candidate.move.dest

  private def removesImmediateRefutation(
      playedMove: Move,
      candidate: BreakCandidate,
      side: Color
  ): Boolean =
    playedMove.captures &&
      candidate.before.board.attackers(candidate.move.dest, !side).exists(_ == playedMove.dest)

  private def sameMove(left: Move, right: Move): Boolean =
    left.orig == right.orig && left.dest == right.dest && left.promotion == right.promotion

  private def breakTokenFor(side: Color, move: Move): String =
    val route = s"${move.orig.key}-${move.dest.key}"
    if side.black then s"...$route" else route

  private def adjacent(a: Square, b: Square): Boolean =
    (a.file.value - b.file.value).abs <= 1 &&
      (a.rank.value - b.rank.value).abs <= 1 &&
      a != b

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalizeUci(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def isUci(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")
