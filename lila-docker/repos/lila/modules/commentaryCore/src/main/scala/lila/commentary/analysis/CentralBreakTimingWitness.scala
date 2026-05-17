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

  object Failure:
    val NoPv = "central_break_timing:no_pv"
    val PvGapTooSmall = "central_break_timing:pv_gap_below_40"
    val NoCentralBreak = "central_break_timing:no_central_break"
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

  val ProofSource: String = PlanTaxonomy.PlanKind.CentralBreakTiming.id
  val ProofFamily: String = PlanTaxonomy.PlanKind.CentralBreakTiming.id
  val PvGapThresholdCp: Int = 40
  private val BreakHorizonPly = 3
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaPrepFen =
    "nrbqr1k1/1p1n1pbp/p2p2p1/P1pP4/4PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 1 16"

  def diagnose(ctx: NarrativeContext): Diagnosis =
    val variations = ctx.engineEvidence.toList.flatMap(_.variations)
    val parsed = Fen.read(Standard, Fen.Full(ctx.fen))
    (parsed, variations.headOption) match
      case (Some(position), Some(pv1)) =>
        val pvGap = pvGapForMover(position.color, variations)
        val breakCandidate = firstCentralBreak(position, pv1)
        val played = playedMove(ctx, pv1)
        val branch = branchKey(pv1.moves)
        val failureCodes =
          List(
            Option.when(pvGap.exists(_ < PvGapThresholdCp))(Failure.PvGapTooSmall),
            Option.when(pvGap.isEmpty)(Failure.PvGapTooSmall),
            Option.when(breakCandidate.isEmpty)(Failure.NoCentralBreak),
            Option.when(branch.isEmpty)(Failure.BranchMissing)
          ).flatten
        val witnesses =
          for
            gap <- pvGap.toList
            candidate <- breakCandidate.toList
            playedUci <- played.toList
            if gap >= PvGapThresholdCp
            playedMove <- legalMove(position, playedUci).toList
          yield buildWitness(
            ctx = ctx,
            position = position,
            playedMove = playedMove,
            candidate = candidate,
            gap = gap,
            pv1 = pv1
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

  def anchorTerms(ctx: NarrativeContext): List[String] =
    exact(ctx).toList.flatMap(_.ownerSeedTerms).distinct

  def failureCodes(ctx: NarrativeContext): List[String] =
    diagnose(ctx).failureCodes

  private final case class BreakCandidate(
      move: Move,
      uci: String,
      plyIndex: Int,
      before: Position
  )

  private def buildWitness(
      ctx: NarrativeContext,
      position: Position,
      playedMove: Move,
      candidate: BreakCandidate,
      gap: Int,
      pv1: VariationLine
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
    val breakToken = breakTokenFor(position.color, breakSquare)
    val sourceTags =
      (
        List(ProofFamily) ++
          Option.when(ctx.fen.trim == MadernaExactFen)("exact:maderna-palermo-1955-direct-break").toList ++
          Option.when(ctx.fen.trim == MadernaPrepFen)("review:maderna-palermo-1955-prep").toList ++
          Option.when(playedIsBreak)("board:played_break").toList ++
          Option.when(!playedIsBreak && hasBoardLink)("board:break_support").toList ++
          Option.when(!hasBoardLink)("plan_only").toList
      ).distinct
    val branch = branchKey(pv1.moves).toList
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
            s"break_move:${candidate.uci}"
          ) ++
            branch.map(key => s"best_branch:$key") ++
            pv1.moves.take(BreakHorizonPly + 1).map(move => s"pv:${normalizeUci(move)}")
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
              Option.when(current.color == initialSide && isCentralBreak(move, initialSide, current.board)) {
                BreakCandidate(move = move, uci = uci, plyIndex = index, before = current)
              }
            (move.after, found)
          case None => (current, None)
    }._2

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

  private def isCentralBreak(move: Move, side: Color, board: Board): Boolean =
    move.piece.role == Pawn &&
      move.piece.color == side &&
      isForward(move, side) &&
      centralFile(move.orig) &&
      centralFile(move.dest) &&
      (move.captures || adjacentEnemyPawn(board, side, move.dest))

  private def isForward(move: Move, side: Color): Boolean =
    if side.white then move.dest.rank.value > move.orig.rank.value
    else move.dest.rank.value < move.orig.rank.value

  private def centralFile(square: Square): Boolean =
    square.file.value == 3 || square.file.value == 4

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

  private def branchKey(moves: List[String]): Option[String] =
    moves.take(2).map(normalizeUci).filter(_.nonEmpty) match
      case first :: second :: Nil => Some(s"${first.toLowerCase}|${second.toLowerCase}")
      case _                      => None

  private def breakTokenFor(side: Color, square: String): String =
    val file = square.headOption.map(_.toString).getOrElse(square)
    if side.black then s"...$file-break" else s"$file-break"

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
