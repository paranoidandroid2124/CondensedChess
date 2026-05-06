package lila.commentary.chess

import java.util.{ Collections, WeakHashMap }

import chess.format.Fen
import chess.{
  Bishop,
  Bitboard,
  Board,
  Color,
  King,
  Knight,
  Pawn,
  Position,
  Queen,
  Role,
  Rook,
  Square as ChessSquare
}
import lila.commentary.root.{ RootAtomRegistry, RootExtractor, RootStateVector }

final case class Square private (index: Int):
  def bit: Long = 1L << index
  def file: Int = index % 8
  def rank: Int = index / 8

object Square:
  def fromIndex(index: Int): Square =
    require(index >= 0 && index < 64, "Square index must be 0..63")
    new Square(index)

  def apply(file: Char, rank: Int): Square =
    val f = file.toLower - 'a'
    require(f >= 0 && f < 8, "Square file must be a..h")
    require(rank >= 1 && rank <= 8, "Square rank must be 1..8")
    fromIndex((rank - 1) * 8 + f)

enum Man:
  case Pawn
  case Knight
  case Bishop
  case Rook
  case Queen
  case King

final case class Piece(side: Side, man: Man, square: Square):
  require(side == Side.White || side == Side.Black, "Piece side must be White or Black")

final case class Line(from: Square, to: Square)

final case class BoardHeader(
    known: Boolean = false,
    plyFromStart: Int = 0,
    phaseTotal: Int = 0,
    phaseNonPawn: Int = 0,
    halfmoveClock: Int = 0,
    fullmoveNumber: Int = 0,
    castlingMask: Int = 0,
    epSquare: Option[Square] = None,
    inCheckMask: Int = 0,
    snapshotPly: Int = 0,
    hashLo: Int = 0,
    hashHi: Int = 0
):
  def sane: Boolean =
    known &&
      plyFromStart >= 0 &&
      phaseTotal >= 0 &&
      phaseNonPawn >= 0 &&
      halfmoveClock >= 0 &&
      fullmoveNumber >= 1 &&
      castlingMask >= 0 &&
      castlingMask <= 15 &&
      inCheckMask >= 0 &&
      inCheckMask <= 3 &&
      snapshotPly >= 0 &&
      hashLo == 0 &&
      hashHi == 0

final case class Moves(
    known: Boolean = false,
    lines: Vector[Line] = Vector.empty,
    destinationUnion: Long = 0L,
    moveCount: Int = 0,
    captureCount: Int = 0,
    checkCount: Int = 0
):
  def legalDestinationUnion: Long =
    lines.foldLeft(destinationUnion): (mask, line) =>
      mask | line.to.bit

  def sane: Boolean =
    val destinationsMatchMoveCount =
      if moveCount == 0 then destinationUnion == 0L && lines.isEmpty
      else destinationUnion != 0L || lines.nonEmpty
    known &&
    moveCount >= 0 &&
    captureCount >= 0 &&
    checkCount >= 0 &&
    captureCount <= moveCount &&
    checkCount <= moveCount &&
    destinationsMatchMoveCount

final case class ControlSide(
    space: Int = 0,
    controlledSquares: Int = 0,
    attackedTwice: Int = 0,
    attackedSquares: Long = 0L,
    controlledMask: Long = 0L
):
  def sane: Boolean =
    space >= 0 &&
      controlledSquares >= 0 &&
      controlledSquares <= 64 &&
      attackedTwice >= 0 &&
      attackedTwice <= 64

final case class Control(
    known: Boolean = false,
    white: ControlSide = ControlSide(),
    black: ControlSide = ControlSide(),
    contestedSquares: Int = 0,
    spaceDiff: Int = 0
):
  def sane: Boolean =
    known &&
      white.sane &&
      black.sane &&
      contestedSquares >= 0 &&
      contestedSquares <= 64 &&
      spaceDiff == white.space - black.space

final case class Pieces(
    pawns: Int = 0,
    knights: Int = 0,
    bishops: Int = 0,
    rooks: Int = 0,
    queens: Int = 0,
    kings: Int = 0,
    value: Int = 0
):
  def sane: Boolean =
    Vector(pawns, knights, bishops, rooks, queens, kings, value).forall(_ >= 0)

final case class Material(
    known: Boolean = false,
    white: Pieces = Pieces(),
    black: Pieces = Pieces(),
    diff: Int = 0,
    imbalance: Int = 0
):
  def sane: Boolean =
    known &&
      white.sane &&
      black.sane &&
      white.kings == 1 &&
      black.kings == 1 &&
      diff == white.value - black.value &&
      imbalance == 0

final case class PawnSide(
    fileCounts: Int = 0,
    isolated: Int = 0,
    backward: Int = 0,
    doubledFiles: Int = 0,
    passed: Int = 0,
    candidatePassers: Int = 0,
    protectedPassers: Int = 0,
    fixed: Int = 0,
    chainBases: Int = 0,
    levers: Int = 0,
    breakChances: Int = 0,
    blockaded: Int = 0,
    bestPromotionDistance: Int = 0,
    support: Int = 0,
    risk: Int = 0,
    structure: Int = 0
):
  def scalars: Vector[Int] = Vector(
    fileCounts,
    isolated,
    backward,
    doubledFiles,
    passed,
    candidatePassers,
    protectedPassers,
    fixed,
    chainBases,
    levers,
    breakChances,
    blockaded,
    bestPromotionDistance,
    support,
    risk,
    structure
  )

  def sane: Boolean =
    scalars.forall(_ >= 0)

final case class Pawns(
    known: Boolean = false,
    white: PawnSide = PawnSide(),
    black: PawnSide = PawnSide()
):
  def sane: Boolean =
    known && white.sane && black.sane

final class BoardFacts private (
    val root: RootStateVector,
    val sideToMove: Side,
    val header: BoardHeader,
    val sideLegal: Moves,
    val rivalLegal: Moves,
    val control: Control,
    val material: Material,
    val pawns: Pawns,
    val pieces: Vector[Piece]
):
  private[chess] lazy val seen: BoardFacts.Seen = BoardFacts.seen(this)

  require(
    sideToMove == Side.White || sideToMove == Side.Black,
    "BoardFacts sideToMove must be White or Black"
  )
  private[commentary] def sameBoardReady: Boolean =
    BoardFacts.sameBoardReady(this)

object BoardFacts:

  private val sameBoardFacts =
    Collections.synchronizedMap(new WeakHashMap[BoardFacts, java.lang.Boolean])

  private def markSameBoard(facts: BoardFacts): BoardFacts =
    sameBoardFacts.put(facts, java.lang.Boolean.TRUE)
    facts

  private[commentary] def sameBoardReady(facts: BoardFacts): Boolean =
    sameBoardFacts.containsKey(facts)

  private[chess] final case class LegalMove(side: Side, piece: Piece, line: Line)
  private[chess] final case class Attack(attacker: Piece, target: Piece)
  private[chess] final case class Guard(guard: Piece, target: Piece)
  private[chess] final case class PieceUnderAttack(piece: Piece, attackers: Vector[Piece])
  private[chess] final case class GuardedPiece(piece: Piece, guards: Vector[Piece])
  private[chess] final case class AttackedUnguardedPiece(piece: Piece, attackers: Vector[Piece])
  private[chess] final case class LoosePieceObservation(piece: Piece)
  private[chess] enum LineKind:
    case File, Rank, Diagonal
  private[chess] final case class LineObservation(kind: LineKind, from: Piece, to: Piece, line: Line)
  private[chess] final case class Ray(side: Side, piece: Piece, kind: LineKind, line: Line, blockers: Vector[Piece])
  private[chess] final case class LineBlocker(side: Side, piece: Piece, blocker: Piece, kind: LineKind, line: Line)
  private[chess] final case class XRayShape(side: Side, piece: Piece, screen: Piece, target: Piece, kind: LineKind, line: Line)
  private[chess] final case class Pin(side: Side, king: Piece, pinned: Piece, attacker: Piece, line: Line)
  private[chess] final case class PawnLever(side: Side, pawn: Piece, target: Piece, line: Line)
  private[chess] final case class PawnChallenge(side: Side, pawn: Piece, square: Square, line: Line)
  private[chess] final case class PawnCannotChallengeSquare(side: Side, square: Square, by: Side)
  private[chess] final case class PawnSafeSquareObservation(side: Side, square: Square, by: Side)
  private[chess] final case class NoCurrentPawnChase(side: Side, square: Square, by: Side)
  private[chess] final case class FrontBlocker(side: Side, pawn: Piece, blocker: Piece, square: Square, line: Line)
  private[chess] final case class PassedPawnObservation(side: Side, pawn: Piece)
  private[chess] final case class IsolatedPawnObservation(side: Side, pawn: Piece)
  private[chess] final case class BackwardPawnFrontSquare(side: Side, pawn: Piece, square: Square, line: Line)
  private[chess] final case class PieceReachableSquare(side: Side, piece: Piece, square: Square, line: Line)
  private[chess] final case class SquareGuardMap(side: Side, square: Square, guards: Vector[Piece])
  private[chess] final case class RookEntry(side: Side, rook: Piece, line: Line)
  private[chess] final case class OpenFile(file: Int, rookEntries: Vector[RookEntry])
  private[chess] final case class OpenFileObservation(file: Int)
  private[chess] final case class SemiOpenFileObservation(side: Side, file: Int)
  private[chess] final case class RookOnFile(side: Side, rook: Piece, file: Int)
  private[chess] final case class LegalFileEntryMove(side: Side, piece: Piece, file: Int, line: Line)
  private[chess] final case class RookOpenFileEntry(side: Side, rook: Piece, file: Int, line: Line)
  private[chess] final case class FileBlocker(side: Side, file: Int, blocker: Piece)
  private[chess] final case class FileTargetSquare(side: Side, file: Int, square: Square, line: Line)
  private[chess] final case class KingSquare(side: Side, king: Piece)
  private[chess] final case class KingRingSquare(side: Side, king: Piece, square: Square)
  private[chess] final case class KingRingAttack(side: Side, king: Piece, square: Square, attacker: Piece)
  private[chess] final case class KingRingDefender(side: Side, king: Piece, square: Square, defender: Piece)
  private[chess] final case class LegalEscapeSquare(side: Side, king: Piece, square: Square, line: Line)
  private[chess] final case class ContactCheckObservation(side: Side, king: Piece, attacker: Piece, line: Line)
  private[chess] final case class LineToKing(side: Side, king: Piece, piece: Piece, kind: LineKind, line: Line, blockers: Vector[Piece])
  private[chess] final case class BlockerNearKing(side: Side, king: Piece, blocker: Piece, piece: Piece, kind: LineKind, line: Line)
  private[chess] final case class MissingEvidence(fact: String, missing: Vector[String])

  private[chess] final case class Seen(
      legalMoves: Vector[LegalMove],
      attacks: Vector[Attack],
      guards: Vector[Guard],
      piecesUnderAttack: Vector[PieceUnderAttack],
      guardedPieces: Vector[GuardedPiece],
      attackedUnguardedPieces: Vector[AttackedUnguardedPiece],
      loosePieceObservations: Vector[LoosePieceObservation],
      lineObservations: Vector[LineObservation],
      rays: Vector[Ray],
      lineBlockers: Vector[LineBlocker],
      xrayShapes: Vector[XRayShape],
      pins: Vector[Pin],
      pawnLevers: Vector[PawnLever],
      pawnChallenges: Vector[PawnChallenge],
      pawnCannotChallengeSquares: Vector[PawnCannotChallengeSquare],
      pawnSafeSquareObservations: Vector[PawnSafeSquareObservation],
      noCurrentPawnChases: Vector[NoCurrentPawnChase],
      frontBlockers: Vector[FrontBlocker],
      passedPawnObservations: Vector[PassedPawnObservation],
      isolatedPawnObservations: Vector[IsolatedPawnObservation],
      backwardPawnFrontSquares: Vector[BackwardPawnFrontSquare],
      pieceReachableSquares: Vector[PieceReachableSquare],
      squareGuardMaps: Vector[SquareGuardMap],
      openFiles: Vector[OpenFile],
      openFileObservations: Vector[OpenFileObservation],
      semiOpenFileObservations: Vector[SemiOpenFileObservation],
      rookOnFiles: Vector[RookOnFile],
      legalFileEntryMoves: Vector[LegalFileEntryMove],
      rookOpenFileEntries: Vector[RookOpenFileEntry],
      fileBlockers: Vector[FileBlocker],
      fileTargetSquares: Vector[FileTargetSquare],
      kingSquares: Vector[KingSquare],
      kingRingSquares: Vector[KingRingSquare],
      kingRingAttacks: Vector[KingRingAttack],
      kingRingDefenders: Vector[KingRingDefender],
      legalEscapeSquares: Vector[LegalEscapeSquare],
      contactCheckObservations: Vector[ContactCheckObservation],
      linesToKing: Vector[LineToKing],
      blockersNearKing: Vector[BlockerNearKing],
      failures: Vector[MissingEvidence]
  )

  private[chess] object Seen:
    def empty(failures: Vector[MissingEvidence] = Vector.empty): Seen =
      Seen(
        legalMoves = Vector.empty,
        attacks = Vector.empty,
        guards = Vector.empty,
        piecesUnderAttack = Vector.empty,
        guardedPieces = Vector.empty,
        attackedUnguardedPieces = Vector.empty,
        loosePieceObservations = Vector.empty,
        lineObservations = Vector.empty,
        rays = Vector.empty,
        lineBlockers = Vector.empty,
        xrayShapes = Vector.empty,
        pins = Vector.empty,
        pawnLevers = Vector.empty,
        pawnChallenges = Vector.empty,
        pawnCannotChallengeSquares = Vector.empty,
        pawnSafeSquareObservations = Vector.empty,
        noCurrentPawnChases = Vector.empty,
        frontBlockers = Vector.empty,
        passedPawnObservations = Vector.empty,
        isolatedPawnObservations = Vector.empty,
        backwardPawnFrontSquares = Vector.empty,
        pieceReachableSquares = Vector.empty,
        squareGuardMaps = Vector.empty,
        openFiles = Vector.empty,
        openFileObservations = Vector.empty,
        semiOpenFileObservations = Vector.empty,
        rookOnFiles = Vector.empty,
        legalFileEntryMoves = Vector.empty,
        rookOpenFileEntries = Vector.empty,
        fileBlockers = Vector.empty,
        fileTargetSquares = Vector.empty,
        kingSquares = Vector.empty,
        kingRingSquares = Vector.empty,
        kingRingAttacks = Vector.empty,
        kingRingDefenders = Vector.empty,
        legalEscapeSquares = Vector.empty,
        contactCheckObservations = Vector.empty,
        linesToKing = Vector.empty,
        blockersNearKing = Vector.empty,
        failures = failures
      )

  private def seen(facts: BoardFacts): Seen =
    val missing = missingSeenEvidence(facts)
    if missing.nonEmpty then Seen.empty(Vector(MissingEvidence("Board Facts", missing)))
    else
      val pieces = facts.pieces.sortBy(pieceKey)
      val bySquare = pieces.map(piece => piece.square -> piece).toMap
      val occupied = pieces.foldLeft(0L): (mask, piece) =>
        mask | piece.square.bit
      val legal = legalMoves(facts, bySquare)
      val attacksSeen = attacks(pieces, occupied)
      val guardsSeen = guards(pieces, occupied)
      val underAttack = pieceUnderAttackRows(attacksSeen)
      val guarded = guardedPieceRows(guardsSeen)
      val fileRows = fileFacts(pieces, legal)
      val rayRows = rays(pieces, bySquare)
      val pawnSquareRows = pawnSquareFacts(pieces, legal, bySquare, occupied)
      val kingRows = kingFacts(pieces, legal, bySquare, occupied)
      Seen(
        legalMoves = legal,
        attacks = attacksSeen,
        guards = guardsSeen,
        piecesUnderAttack = underAttack,
        guardedPieces = guarded,
        attackedUnguardedPieces = attackedUnguardedPieceRows(underAttack, guarded),
        loosePieceObservations = loosePieceObservationRows(pieces, guarded),
        lineObservations = lineObservations(pieces),
        rays = rayRows,
        lineBlockers = lineBlockers(rayRows),
        xrayShapes = xrayShapes(rayRows),
        pins = pins(pieces, bySquare),
        pawnLevers = pawnSquareRows.pawnLevers,
        pawnChallenges = pawnSquareRows.pawnChallenges,
        pawnCannotChallengeSquares = pawnSquareRows.pawnCannotChallengeSquares,
        pawnSafeSquareObservations = pawnSquareRows.pawnSafeSquareObservations,
        noCurrentPawnChases = pawnSquareRows.noCurrentPawnChases,
        frontBlockers = pawnSquareRows.frontBlockers,
        passedPawnObservations = pawnSquareRows.passedPawnObservations,
        isolatedPawnObservations = pawnSquareRows.isolatedPawnObservations,
        backwardPawnFrontSquares = pawnSquareRows.backwardPawnFrontSquares,
        pieceReachableSquares = pawnSquareRows.pieceReachableSquares,
        squareGuardMaps = pawnSquareRows.squareGuardMaps,
        openFiles = fileRows.openFiles,
        openFileObservations = fileRows.openFileObservations,
        semiOpenFileObservations = fileRows.semiOpenFileObservations,
        rookOnFiles = fileRows.rookOnFiles,
        legalFileEntryMoves = fileRows.legalFileEntryMoves,
        rookOpenFileEntries = fileRows.rookOpenFileEntries,
        fileBlockers = fileRows.fileBlockers,
        fileTargetSquares = fileRows.fileTargetSquares,
        kingSquares = kingRows.kingSquares,
        kingRingSquares = kingRows.kingRingSquares,
        kingRingAttacks = kingRows.kingRingAttacks,
        kingRingDefenders = kingRows.kingRingDefenders,
        legalEscapeSquares = kingRows.legalEscapeSquares,
        contactCheckObservations = kingRows.contactCheckObservations,
        linesToKing = kingRows.linesToKing,
        blockersNearKing = kingRows.blockersNearKing,
        failures = Vector.empty
      )

  private def missingSeenEvidence(facts: BoardFacts): Vector[String] =
    Vector(
      Option.when(!facts.sameBoardReady)("same-board producer proof"),
      Option.when(facts.root.activeIndices.isEmpty)("same board root"),
      Option.when(!facts.header.sane)("board header"),
      Option.when(!facts.sideLegal.sane || !facts.rivalLegal.sane)("legal moves"),
      Option.when(!facts.control.sane)("attacks"),
      Option.when(!facts.material.sane)("pieces"),
      Option.when(!facts.pawns.sane)("pawns"),
      Option.when(facts.pieces.isEmpty)("piece list")
    ).flatten

  private def legalMoves(facts: BoardFacts, bySquare: Map[Square, Piece]): Vector[LegalMove] =
    Vector(facts.sideToMove -> facts.sideLegal, opposite(facts.sideToMove) -> facts.rivalLegal)
      .flatMap: (side, moves) =>
        moves.lines.flatMap: line =>
          bySquare.get(line.from).filter(_.side == side).map: piece =>
            LegalMove(side, piece, line)
      .sortBy(move => (move.side.ordinal, move.line.from.index, move.line.to.index))

  private def attacks(pieces: Vector[Piece], occupied: Long): Vector[Attack] =
    (for
      target <- pieces
      attacker <- pieces
      if attacker.side != target.side && attacksSquare(attacker, target.square, occupied)
    yield Attack(attacker, target)).sortBy(attack => (pieceKey(attack.attacker), pieceKey(attack.target)))

  private def guards(pieces: Vector[Piece], occupied: Long): Vector[Guard] =
    (for
      target <- pieces
      guard <- pieces
      if guard != target && guard.side == target.side && attacksSquare(guard, target.square, occupied)
    yield Guard(guard, target)).sortBy(guard => (pieceKey(guard.guard), pieceKey(guard.target)))

  private def pieceUnderAttackRows(attacks: Vector[Attack]): Vector[PieceUnderAttack] =
    attacks
      .groupBy(_.target)
      .toVector
      .map: (piece, rows) =>
        PieceUnderAttack(piece, rows.map(_.attacker).sortBy(pieceKey))
      .sortBy(row => pieceKey(row.piece))

  private def guardedPieceRows(guards: Vector[Guard]): Vector[GuardedPiece] =
    guards
      .groupBy(_.target)
      .toVector
      .map: (piece, rows) =>
        GuardedPiece(piece, rows.map(_.guard).sortBy(pieceKey))
      .sortBy(row => pieceKey(row.piece))

  private def attackedUnguardedPieceRows(
      attacked: Vector[PieceUnderAttack],
      guarded: Vector[GuardedPiece]
  ): Vector[AttackedUnguardedPiece] =
    val guardedPieces = guarded.map(_.piece).toSet
    attacked
      .filterNot(row => guardedPieces.contains(row.piece))
      .map(row => AttackedUnguardedPiece(row.piece, row.attackers))

  private def loosePieceObservationRows(
      pieces: Vector[Piece],
      guarded: Vector[GuardedPiece]
  ): Vector[LoosePieceObservation] =
    val guardedPieces = guarded.map(_.piece).toSet
    pieces
      .filter(piece => piece.man != Man.Pawn && piece.man != Man.King)
      .filterNot(guardedPieces.contains)
      .map(LoosePieceObservation.apply)
      .sortBy(row => pieceKey(row.piece))

  private def lineObservations(pieces: Vector[Piece]): Vector[LineObservation] =
    val bySquare = pieces.sortBy(_.square.index)
    (for
      (from, index) <- bySquare.zipWithIndex
      to <- bySquare.drop(index + 1)
      step <- lineStep(from.square, to.square)
    yield LineObservation(lineKind(step), from, to, Line(from.square, to.square)))
      .sortBy(row => (row.kind.ordinal, row.line.from.index, row.line.to.index))

  private final case class RayDirection(step: Int, fileDelta: Int, rankDelta: Int)

  private val rayDirections = Vector(
    RayDirection(1, 1, 0),
    RayDirection(-1, -1, 0),
    RayDirection(8, 0, 1),
    RayDirection(-8, 0, -1),
    RayDirection(9, 1, 1),
    RayDirection(-9, -1, -1),
    RayDirection(7, -1, 1),
    RayDirection(-7, 1, -1)
  )

  private def rays(pieces: Vector[Piece], bySquare: Map[Square, Piece]): Vector[Ray] =
    pieces
      .filter(piece => sliderUses(piece.man, 1) || sliderUses(piece.man, 7) || sliderUses(piece.man, 8))
      .flatMap: piece =>
        rayDirections
          .filter(direction => sliderUses(piece.man, direction.step))
          .flatMap: direction =>
            rayEnd(piece.square, direction).map: end =>
              val raySquares = squaresBetween(piece.square, end, direction.step) :+ end
              Ray(
                side = piece.side,
                piece = piece,
                kind = lineKind(direction.step),
                line = Line(piece.square, end),
                blockers = raySquares.flatMap(bySquare.get)
              )
      .sortBy(ray => (pieceKey(ray.piece), ray.kind.ordinal, ray.line.to.index))

  private def lineBlockers(rays: Vector[Ray]): Vector[LineBlocker] =
    rays
      .flatMap: ray =>
        ray.blockers.headOption.map: blocker =>
          LineBlocker(
            side = ray.side,
            piece = ray.piece,
            blocker = blocker,
            kind = ray.kind,
            line = Line(ray.piece.square, blocker.square)
          )
      .sortBy(row => (pieceKey(row.piece), row.kind.ordinal, row.line.to.index, pieceKey(row.blocker)))

  private def xrayShapes(rays: Vector[Ray]): Vector[XRayShape] =
    rays
      .collect:
        case ray if ray.blockers.size >= 2 =>
          XRayShape(
            side = ray.side,
            piece = ray.piece,
            screen = ray.blockers(0),
            target = ray.blockers(1),
            kind = ray.kind,
            line = Line(ray.piece.square, ray.blockers(1).square)
          )
      .sortBy(row => (pieceKey(row.piece), row.kind.ordinal, row.line.to.index, pieceKey(row.screen), pieceKey(row.target)))

  private def pins(pieces: Vector[Piece], bySquare: Map[Square, Piece]): Vector[Pin] =
    val kings = pieces.filter(_.man == Man.King)
    (for
      king <- kings
      attacker <- pieces
      step <- lineStep(attacker.square, king.square)
      if attacker.side == opposite(king.side) && sliderUses(attacker.man, step)
      between = squaresBetween(attacker.square, king.square, step).flatMap(bySquare.get)
      if between.size == 1 && between.head.side == king.side
    yield Pin(
      side = king.side,
      king = king,
      pinned = between.head,
      attacker = attacker,
      line = Line(attacker.square, king.square)
    )).sortBy(pin => (pin.side.ordinal, pin.line.from.index, pin.line.to.index, pin.pinned.square.index))

  private def pawnLevers(pieces: Vector[Piece], occupied: Long): Vector[PawnLever] =
    (for
      pawn <- pieces
      target <- pieces
      if pawn.man == Man.Pawn && target.man == Man.Pawn && pawn.side != target.side
      if attacksSquare(pawn, target.square, occupied)
    yield PawnLever(pawn.side, pawn, target, Line(pawn.square, target.square)))
      .sortBy(lever => (lever.side.ordinal, lever.pawn.square.index, lever.target.square.index))

  private final case class PawnSquareFacts(
      pawnLevers: Vector[PawnLever],
      pawnChallenges: Vector[PawnChallenge],
      pawnCannotChallengeSquares: Vector[PawnCannotChallengeSquare],
      pawnSafeSquareObservations: Vector[PawnSafeSquareObservation],
      noCurrentPawnChases: Vector[NoCurrentPawnChase],
      frontBlockers: Vector[FrontBlocker],
      passedPawnObservations: Vector[PassedPawnObservation],
      isolatedPawnObservations: Vector[IsolatedPawnObservation],
      backwardPawnFrontSquares: Vector[BackwardPawnFrontSquare],
      pieceReachableSquares: Vector[PieceReachableSquare],
      squareGuardMaps: Vector[SquareGuardMap]
  )

  private def pawnSquareFacts(
      pieces: Vector[Piece],
      legalMoves: Vector[LegalMove],
      bySquare: Map[Square, Piece],
      occupied: Long
  ): PawnSquareFacts =
    val challenges = pawnChallenges(pieces)
    val noChase = noCurrentPawnChases(pieces, legalMoves, challenges)
    PawnSquareFacts(
      pawnLevers = pawnLevers(pieces, occupied),
      pawnChallenges = challenges,
      pawnCannotChallengeSquares = noChase.map(row => PawnCannotChallengeSquare(row.side, row.square, row.by)),
      pawnSafeSquareObservations = noChase.map(row => PawnSafeSquareObservation(row.side, row.square, row.by)),
      noCurrentPawnChases = noChase,
      frontBlockers = frontBlockers(pieces, bySquare),
      passedPawnObservations = passedPawnObservations(pieces),
      isolatedPawnObservations = isolatedPawnObservations(pieces),
      backwardPawnFrontSquares = backwardPawnFrontSquares(pieces),
      pieceReachableSquares = pieceReachableSquares(legalMoves),
      squareGuardMaps = squareGuardMaps(pieces, occupied)
    )

  private def pawnChallenges(pieces: Vector[Piece]): Vector[PawnChallenge] =
    pieces
      .filter(_.man == Man.Pawn)
      .flatMap: pawn =>
        pawnAttacks(pawn).map: square =>
          PawnChallenge(pawn.side, pawn, square, Line(pawn.square, square))
      .sortBy(row => (row.side.ordinal, row.pawn.square.index, row.square.index))

  private def noCurrentPawnChases(
      pieces: Vector[Piece],
      legalMoves: Vector[LegalMove],
      challenges: Vector[PawnChallenge]
  ): Vector[NoCurrentPawnChase] =
    val challengedBySide = challenges.groupBy(_.side).view.mapValues(_.map(_.square).toSet).toMap
    pawnSafeCandidateSquares(pieces, legalMoves)
      .filterNot: (side, square) =>
        challengedBySide.getOrElse(opposite(side), Set.empty).contains(square)
      .map: (side, square) =>
        NoCurrentPawnChase(side, square, opposite(side))
      .sortBy(row => (row.side.ordinal, row.square.index, row.by.ordinal))

  private def pawnSafeCandidateSquares(pieces: Vector[Piece], legalMoves: Vector[LegalMove]): Vector[(Side, Square)] =
    val occupiedCandidates =
      pieces
        .filter(piece => piece.man != Man.Pawn && piece.man != Man.King)
        .map(piece => piece.side -> piece.square)
    val reachableCandidates =
      legalMoves
        .filter(move => move.piece.man != Man.Pawn && move.piece.man != Man.King)
        .map(move => move.side -> move.line.to)
    (occupiedCandidates ++ reachableCandidates).distinct

  private def frontBlockers(pieces: Vector[Piece], bySquare: Map[Square, Piece]): Vector[FrontBlocker] =
    pieces
      .filter(_.man == Man.Pawn)
      .flatMap: pawn =>
        frontSquare(pawn).flatMap: square =>
          bySquare.get(square).map: blocker =>
            FrontBlocker(pawn.side, pawn, blocker, square, Line(pawn.square, square))
      .sortBy(row => (row.side.ordinal, row.pawn.square.index, row.square.index, pieceKey(row.blocker)))

  private def passedPawnObservations(pieces: Vector[Piece]): Vector[PassedPawnObservation] =
    pieces
      .filter(_.man == Man.Pawn)
      .filter(pawn => isPassedPawnObservation(pawn, pieces))
      .map(pawn => PassedPawnObservation(pawn.side, pawn))
      .sortBy(row => pieceKey(row.pawn))

  private def isolatedPawnObservations(pieces: Vector[Piece]): Vector[IsolatedPawnObservation] =
    pieces
      .filter(_.man == Man.Pawn)
      .filter: pawn =>
        !pieces.exists(piece =>
          piece.side == pawn.side && piece.man == Man.Pawn && piece != pawn && math.abs(piece.square.file - pawn.square.file) == 1
        )
      .map(pawn => IsolatedPawnObservation(pawn.side, pawn))
      .sortBy(row => pieceKey(row.pawn))

  private def backwardPawnFrontSquares(pieces: Vector[Piece]): Vector[BackwardPawnFrontSquare] =
    val pawns = pieces.filter(_.man == Man.Pawn)
    pawns
      .flatMap: pawn =>
        frontSquare(pawn).filter: front =>
          hasAdjacentFriendlyPawn(pawn, pawns) &&
            !pawns.exists(friendly => friendly.side == pawn.side && pawnAttacks(friendly).contains(front)) &&
            (
              pawns.exists(enemy => enemy.side == opposite(pawn.side) && pawnAttacks(enemy).contains(front)) ||
                pieces.exists(piece => piece.side == opposite(pawn.side) && piece.square == front)
            )
        .map(front => BackwardPawnFrontSquare(pawn.side, pawn, front, Line(pawn.square, front)))
      .sortBy(row => (row.side.ordinal, row.pawn.square.index, row.square.index))

  private def pieceReachableSquares(legalMoves: Vector[LegalMove]): Vector[PieceReachableSquare] =
    legalMoves
      .map(move => PieceReachableSquare(move.side, move.piece, move.line.to, move.line))
      .sortBy(row => (row.side.ordinal, pieceKey(row.piece), row.square.index))

  private def squareGuardMaps(pieces: Vector[Piece], occupied: Long): Vector[SquareGuardMap] =
    (for
      side <- Vector(Side.White, Side.Black)
      square <- (0 until 64).toVector.map(Square.fromIndex)
      guards = pieces.filter(piece => piece.side == side && attacksSquare(piece, square, occupied)).sortBy(pieceKey)
      if guards.nonEmpty
    yield SquareGuardMap(side, square, guards))
      .sortBy(row => (row.side.ordinal, row.square.index))

  private def frontSquare(pawn: Piece): Option[Square] =
    val rankStep = if pawn.side == Side.White then 1 else -1
    squareAt(pawn.square.file, pawn.square.rank + rankStep)

  private def isPassedPawnObservation(pawn: Piece, pieces: Vector[Piece]): Boolean =
    pieces
      .filter(piece => piece.side == opposite(pawn.side) && piece.man == Man.Pawn)
      .forall: enemy =>
        math.abs(enemy.square.file - pawn.square.file) > 1 || !isAheadOf(pawn.side, pawn.square, enemy.square)

  private def isAheadOf(side: Side, from: Square, to: Square): Boolean =
    if side == Side.White then to.rank > from.rank else to.rank < from.rank

  private def hasAdjacentFriendlyPawn(pawn: Piece, pawns: Vector[Piece]): Boolean =
    pawns.exists(piece => piece.side == pawn.side && piece != pawn && math.abs(piece.square.file - pawn.square.file) == 1)

  private final case class FileFacts(
      openFiles: Vector[OpenFile],
      openFileObservations: Vector[OpenFileObservation],
      semiOpenFileObservations: Vector[SemiOpenFileObservation],
      rookOnFiles: Vector[RookOnFile],
      legalFileEntryMoves: Vector[LegalFileEntryMove],
      rookOpenFileEntries: Vector[RookOpenFileEntry],
      fileBlockers: Vector[FileBlocker],
      fileTargetSquares: Vector[FileTargetSquare]
  )

  private def fileFacts(pieces: Vector[Piece], legalMoves: Vector[LegalMove]): FileFacts =
    val whitePawnFiles = pawnFiles(pieces, Side.White)
    val blackPawnFiles = pawnFiles(pieces, Side.Black)
    val allPawnFiles = whitePawnFiles ++ blackPawnFiles
    val openFileObservations =
      (0 until 8).filterNot(allPawnFiles.contains).map(OpenFileObservation.apply).toVector
    val openFileIndexes = openFileObservations.map(_.file).toSet
    val semiOpenFileObservations =
      (0 until 8).toVector.flatMap: file =>
        Vector(
          Option.when(!whitePawnFiles.contains(file) && blackPawnFiles.contains(file))(
            SemiOpenFileObservation(Side.White, file)
          ),
          Option.when(!blackPawnFiles.contains(file) && whitePawnFiles.contains(file))(
            SemiOpenFileObservation(Side.Black, file)
          )
        ).flatten
    val semiOpenFilesBySide = semiOpenFileObservations.map(row => row.side -> row.file).toSet
    val observedFiles = openFileIndexes ++ semiOpenFileObservations.map(_.file).toSet
    val rookOnFiles =
      pieces
        .filter(_.man == Man.Rook)
        .map(rook => RookOnFile(rook.side, rook, rook.square.file))
        .sortBy(row => (row.side.ordinal, row.file, pieceKey(row.rook)))
    val legalFileEntryMoves =
      legalMoves
        .filter(_.piece.man == Man.Rook)
        .filter: move =>
          openFileIndexes.contains(move.line.to.file) || semiOpenFilesBySide.contains(move.side -> move.line.to.file)
        .map(move => LegalFileEntryMove(move.side, move.piece, move.line.to.file, move.line))
        .sortBy(row => (row.side.ordinal, row.file, row.line.from.index, row.line.to.index))
    val rookOpenFileEntries =
      legalFileEntryMoves
        .filter(move => openFileIndexes.contains(move.file))
        .map(move => RookOpenFileEntry(move.side, move.piece, move.file, move.line))
    val openFiles =
      openFileObservations.map: row =>
        OpenFile(
          row.file,
          rookOpenFileEntries
            .filter(_.file == row.file)
            .map(entry => RookEntry(entry.side, entry.rook, entry.line))
        )
    val fileBlockers =
      pieces
        .filter(piece => observedFiles.contains(piece.square.file))
        .map(piece => FileBlocker(piece.side, piece.square.file, piece))
        .sortBy(row => (row.file, pieceKey(row.blocker)))
    val fileTargetSquares =
      legalFileEntryMoves
        .map(move => FileTargetSquare(move.side, move.file, move.line.to, move.line))
        .sortBy(row => (row.side.ordinal, row.file, row.square.index, row.line.from.index))

    FileFacts(
      openFiles = openFiles,
      openFileObservations = openFileObservations,
      semiOpenFileObservations = semiOpenFileObservations,
      rookOnFiles = rookOnFiles,
      legalFileEntryMoves = legalFileEntryMoves,
      rookOpenFileEntries = rookOpenFileEntries,
      fileBlockers = fileBlockers,
      fileTargetSquares = fileTargetSquares
    )

  private def pawnFiles(pieces: Vector[Piece], side: Side): Set[Int] =
    pieces.filter(piece => piece.side == side && piece.man == Man.Pawn).map(_.square.file).toSet

  private final case class KingFacts(
      kingSquares: Vector[KingSquare],
      kingRingSquares: Vector[KingRingSquare],
      kingRingAttacks: Vector[KingRingAttack],
      kingRingDefenders: Vector[KingRingDefender],
      legalEscapeSquares: Vector[LegalEscapeSquare],
      contactCheckObservations: Vector[ContactCheckObservation],
      linesToKing: Vector[LineToKing],
      blockersNearKing: Vector[BlockerNearKing]
  )

  private def kingFacts(
      pieces: Vector[Piece],
      legalMoves: Vector[LegalMove],
      bySquare: Map[Square, Piece],
      occupied: Long
  ): KingFacts =
    val kings = pieces.filter(_.man == Man.King).sortBy(pieceKey)
    val lines = linesToKing(kings, pieces, bySquare)
    KingFacts(
      kingSquares = kings.map(king => KingSquare(king.side, king)),
      kingRingSquares = kingRingSquares(kings),
      kingRingAttacks = kingRingAttacks(kings, pieces, occupied),
      kingRingDefenders = kingRingDefenders(kings, pieces, occupied),
      legalEscapeSquares = legalEscapeSquares(legalMoves),
      contactCheckObservations = contactCheckObservations(kings, legalMoves, occupied),
      linesToKing = lines,
      blockersNearKing = blockersNearKing(lines)
    )

  private def kingRingSquares(kings: Vector[Piece]): Vector[KingRingSquare] =
    kings
      .flatMap: king =>
        kingRing(king.square).map(square => KingRingSquare(king.side, king, square))
      .sortBy(row => (row.side.ordinal, row.king.square.index, row.square.index))

  private def kingRingAttacks(kings: Vector[Piece], pieces: Vector[Piece], occupied: Long): Vector[KingRingAttack] =
    (for
      king <- kings
      square <- kingRing(king.square)
      attacker <- pieces
      if attacker.side == opposite(king.side) && attacksSquare(attacker, square, occupied)
    yield KingRingAttack(king.side, king, square, attacker))
      .sortBy(attack => (attack.side.ordinal, attack.king.square.index, attack.square.index, pieceKey(attack.attacker)))

  private def kingRingDefenders(kings: Vector[Piece], pieces: Vector[Piece], occupied: Long): Vector[KingRingDefender] =
    (for
      king <- kings
      square <- kingRing(king.square)
      defender <- pieces
      if defender.side == king.side && defender != king && attacksSquare(defender, square, occupied)
    yield KingRingDefender(king.side, king, square, defender))
      .sortBy(row => (row.side.ordinal, row.king.square.index, row.square.index, pieceKey(row.defender)))

  private def legalEscapeSquares(legalMoves: Vector[LegalMove]): Vector[LegalEscapeSquare] =
    legalMoves
      .filter(_.piece.man == Man.King)
      .map(move => LegalEscapeSquare(move.side, move.piece, move.line.to, move.line))
      .sortBy(row => (row.side.ordinal, row.king.square.index, row.square.index))

  private def contactCheckObservations(
      kings: Vector[Piece],
      legalMoves: Vector[LegalMove],
      occupied: Long
  ): Vector[ContactCheckObservation] =
    (for
      king <- kings
      move <- legalMoves
      if move.side == opposite(king.side)
      if kingRing(king.square).contains(move.line.to)
      moved = Piece(move.side, move.piece.man, move.line.to)
      occupiedAfter = (occupied & ~move.line.from.bit) | move.line.to.bit
      if attacksSquare(moved, king.square, occupiedAfter)
    yield ContactCheckObservation(king.side, king, moved, move.line))
      .sortBy(row => (row.side.ordinal, row.king.square.index, pieceKey(row.attacker), row.line.from.index))

  private def linesToKing(kings: Vector[Piece], pieces: Vector[Piece], bySquare: Map[Square, Piece]): Vector[LineToKing] =
    (for
      king <- kings
      piece <- pieces
      step <- lineStep(piece.square, king.square)
      if piece != king && sliderUses(piece.man, step)
      blockers = squaresBetween(piece.square, king.square, step).flatMap(bySquare.get)
    yield LineToKing(
      side = king.side,
      king = king,
      piece = piece,
      kind = lineKind(step),
      line = Line(piece.square, king.square),
      blockers = blockers
    )).sortBy(row => (row.side.ordinal, row.king.square.index, pieceKey(row.piece), row.line.from.index))

  private def blockersNearKing(lines: Vector[LineToKing]): Vector[BlockerNearKing] =
    lines
      .flatMap: line =>
        line.blockers
          .filter(blocker => kingRing(line.king.square).contains(blocker.square))
          .map: blocker =>
            BlockerNearKing(line.side, line.king, blocker, line.piece, line.kind, line.line)
      .sortBy(row => (row.side.ordinal, row.king.square.index, pieceKey(row.blocker), pieceKey(row.piece)))

  private def attacksSquare(piece: Piece, target: Square, occupied: Long): Boolean =
    piece.man match
      case Man.Pawn   => pawnAttacks(piece).contains(target)
      case Man.Knight => knightAttacks(piece.square).contains(target)
      case Man.King   => kingRing(piece.square).contains(target)
      case Man.Bishop | Man.Rook | Man.Queen =>
        lineStep(piece.square, target).exists: step =>
          sliderUses(piece.man, step) && squaresBetween(piece.square, target, step).forall: square =>
            (occupied & square.bit) == 0L

  private def pawnAttacks(piece: Piece): Vector[Square] =
    val rankStep = if piece.side == Side.White then 1 else -1
    Vector(-1, 1).flatMap(fileStep => squareAt(piece.square.file + fileStep, piece.square.rank + rankStep))

  private def knightAttacks(square: Square): Vector[Square] =
    Vector((1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2))
      .flatMap: (fileStep, rankStep) =>
        squareAt(square.file + fileStep, square.rank + rankStep)

  private def kingRing(square: Square): Vector[Square] =
    (for
      fileStep <- -1 to 1
      rankStep <- -1 to 1
      if fileStep != 0 || rankStep != 0
      ringSquare <- squareAt(square.file + fileStep, square.rank + rankStep)
    yield ringSquare).toVector

  private def squareAt(file: Int, rank: Int): Option[Square] =
    Option.when(file >= 0 && file < 8 && rank >= 0 && rank < 8)(Square.fromIndex(rank * 8 + file))

  private def lineStep(from: Square, to: Square): Option[Int] =
    val fileDelta = to.file - from.file
    val rankDelta = to.rank - from.rank
    if fileDelta == 0 && rankDelta != 0 then Some(Integer.signum(rankDelta) * 8)
    else if rankDelta == 0 && fileDelta != 0 then Some(Integer.signum(fileDelta))
    else if math.abs(fileDelta) == math.abs(rankDelta) && fileDelta != 0 then
      Some(Integer.signum(rankDelta) * 8 + Integer.signum(fileDelta))
    else None

  private def lineKind(step: Int): LineKind =
    if math.abs(step) == 8 then LineKind.File
    else if math.abs(step) == 1 then LineKind.Rank
    else LineKind.Diagonal

  private def rayEnd(from: Square, direction: RayDirection): Option[Square] =
    var file = from.file
    var rank = from.rank
    var end: Option[Square] = None
    var onBoard = true
    while onBoard do
      file += direction.fileDelta
      rank += direction.rankDelta
      if file >= 0 && file < 8 && rank >= 0 && rank < 8 then end = Some(Square.fromIndex(rank * 8 + file))
      else onBoard = false
    end

  private def squaresBetween(from: Square, to: Square, step: Int): Vector[Square] =
    val builder = Vector.newBuilder[Square]
    var index = from.index + step
    while index != to.index do
      builder += Square.fromIndex(index)
      index += step
    builder.result()

  private def sliderUses(man: Man, step: Int): Boolean =
    man match
      case Man.Rook  => math.abs(step) == 1 || math.abs(step) == 8
      case Man.Bishop => math.abs(step) == 7 || math.abs(step) == 9
      case Man.Queen => math.abs(step) == 1 || math.abs(step) == 7 || math.abs(step) == 8 || math.abs(step) == 9
      case _         => false

  private def opposite(side: Side): Side =
    side match
      case Side.White => Side.Black
      case Side.Black => Side.White
      case _          => Side.None

  private def pieceKey(piece: Piece): (Int, Int, Int) =
    (piece.side.ordinal, piece.man.ordinal, piece.square.index)

  def fromFen(fenInput: Fen.Full | String): Either[String, BoardFacts] =
    val fen = Fen.Full(fenInput.toString)
    RootExtractor
      .fromFenWithPositionFailClosed(fen)
      .flatMap: rooted =>
        fenHeader(fen).map(headerParts => fromRooted(rooted, headerParts))

  private[commentary] def untrusted(
      root: RootStateVector,
      sideToMove: Side,
      header: BoardHeader,
      sideLegal: Moves,
      rivalLegal: Moves,
      control: Control,
      material: Material,
      pawns: Pawns,
      pieces: Vector[Piece] = Vector.empty
  ): BoardFacts =
    BoardFacts(
      root = root,
      sideToMove = sideToMove,
      header = header,
      sideLegal = sideLegal,
      rivalLegal = rivalLegal,
      control = control,
      material = material,
      pawns = pawns,
      pieces = pieces
    )

  private[commentary] def fromPosition(position: Position, fullmoveNumber: Int): Either[String, BoardFacts] =
    Either
      .cond(fullmoveNumber >= 1, fullmoveNumber, s"Invalid fullmove number: $fullmoveNumber")
      .flatMap: validFullmoveNumber =>
        RootExtractor
          .fromPositionFailClosed(position)
          .map: rooted =>
            fromRooted(
              rooted,
              HeaderParts(
                halfmoveClock = position.history.halfMoveClock.value,
                fullmoveNumber = validFullmoveNumber,
                castlingMask = castlingMask(position),
                epSquare = position.enPassantSquare.map(square => Square.fromIndex(square.value))
              )
            )

  private final case class HeaderParts(
      halfmoveClock: Int,
      fullmoveNumber: Int,
      castlingMask: Int,
      epSquare: Option[Square]
  )

  private def fromRooted(rooted: RootExtractor.RootWithPosition, headerParts: HeaderParts): BoardFacts =
    val position = rooted.position
    val ply = plyFromStart(headerParts.fullmoveNumber, position.color)
    markSameBoard(BoardFacts(
      root = rooted.root,
      sideToMove = sideFromColor(position.color),
      header = BoardHeader(
        known = true,
        plyFromStart = ply,
        phaseTotal = phaseTotal(position.board),
        phaseNonPawn = phaseNonPawn(position.board),
        halfmoveClock = headerParts.halfmoveClock,
        fullmoveNumber = headerParts.fullmoveNumber,
        castlingMask = headerParts.castlingMask,
        epSquare = headerParts.epSquare,
        inCheckMask = inCheckMask(position),
        snapshotPly = ply,
        hashLo = 0,
        hashHi = 0
      ),
      sideLegal = movesFor(position),
      rivalLegal = movesFor(position.withColor(!position.color)),
      control = control(position),
      material = material(position.board),
      pawns = pawns(position.board, rooted.root),
      pieces = pieces(position.board)
    ))

  private def fenHeader(fen: Fen.Full): Either[String, HeaderParts] =
    val parts = fen.value.split(' ')
    for
      halfmove <- parseNonNegative(parts.lift(4), "halfmove clock", fen)
      fullmove <- parsePositive(parts.lift(5), "fullmove number", fen)
      castling <- RootAtomRegistry
        .castlingRightsMask(parts.lift(2).getOrElse("-"))
        .toRight(s"Invalid castling-rights field: $fen")
      epSquare <- parseEpSquare(parts.lift(3).getOrElse("-"), fen)
    yield HeaderParts(
      halfmoveClock = halfmove,
      fullmoveNumber = fullmove,
      castlingMask = castling,
      epSquare = epSquare
    )

  private def parseNonNegative(raw: Option[String], field: String, fen: Fen.Full): Either[String, Int] =
    raw
      .flatMap(value => value.toIntOption)
      .filter(_ >= 0)
      .toRight(s"Invalid $field: $fen")

  private def parsePositive(raw: Option[String], field: String, fen: Fen.Full): Either[String, Int] =
    parseNonNegative(raw, field, fen).flatMap: value =>
      Either.cond(value >= 1, value, s"Invalid $field: $fen")

  private def parseEpSquare(raw: String, fen: Fen.Full): Either[String, Option[Square]] =
    if raw == "-" then Right(None)
    else
      ChessSquare
        .fromKey(raw)
        .map(square => Some(Square.fromIndex(square.value)))
        .toRight(s"Invalid en-passant square: $fen")

  private def movesFor(position: Position): Moves =
    val legal = position.legalMoves.toVector
    Moves(
      known = true,
      lines = legal.map(move => Line(Square.fromIndex(move.orig.value), Square.fromIndex(move.dest.value))),
      moveCount = legal.size,
      captureCount = legal.count(move => move.capture.isDefined || move.enpassant),
      checkCount = legal.count(move => move.after.check.yes)
    )

  private def control(position: Position): Control =
    val whiteAttacks = attackMasks(position.board, Color.White)
    val blackAttacks = attackMasks(position.board, Color.Black)
    val whiteUnion = whiteAttacks.foldLeft(Bitboard.empty)(_ | _)
    val blackUnion = blackAttacks.foldLeft(Bitboard.empty)(_ | _)
    val occupied = position.board.occupied.value
    val whiteSpace = space(whiteUnion.value, blackUnion.value, occupied, Color.White)
    val blackSpace = space(blackUnion.value, whiteUnion.value, occupied, Color.Black)
    Control(
      known = true,
      white = ControlSide(
        space = whiteSpace,
        controlledSquares = Bitboard.count(whiteUnion),
        attackedTwice = attackedTwice(whiteAttacks),
        attackedSquares = whiteUnion.value,
        controlledMask = whiteUnion.value
      ),
      black = ControlSide(
        space = blackSpace,
        controlledSquares = Bitboard.count(blackUnion),
        attackedTwice = attackedTwice(blackAttacks),
        attackedSquares = blackUnion.value,
        controlledMask = blackUnion.value
      ),
      contestedSquares = Bitboard.count(whiteUnion & blackUnion),
      spaceDiff = whiteSpace - blackSpace
    )

  private def space(controlled: Long, opposed: Long, occupied: Long, color: Color): Int =
    java.lang.Long.bitCount(spaceZoneMask(color) & controlled & ~opposed & ~occupied)

  private def spaceZoneMask(color: Color): Long =
    val ranks = if color.white then 3 to 5 else 2 to 4
    (for
      rank <- ranks
      file <- 2 to 5
    yield 1L << (rank * 8 + file)).foldLeft(0L)(_ | _)

  private def attackMasks(board: Board, color: Color): Vector[Bitboard] =
    board.pieceMap.iterator
      .collect:
        case (square, piece) if piece.color == color => attackMaskFrom(board, square, piece)
      .toVector

  private def attackMaskFrom(board: Board, square: ChessSquare, piece: chess.Piece): Bitboard =
    piece.role match
      case Pawn => square.pawnAttacks(piece.color)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(board.occupied)
      case Rook => square.rookAttacks(board.occupied)
      case Queen => square.queenAttacks(board.occupied)
      case King => square.kingAttacks

  private def attackedTwice(masks: Vector[Bitboard]): Int =
    val counts = Array.fill(64)(0)
    masks.foreach: mask =>
      Bitboard.foreach(mask): square =>
        counts(square.value) += 1
    counts.count(_ >= 2)

  private def material(board: Board): Material =
    val white = piecesFor(board, Color.White)
    val black = piecesFor(board, Color.Black)
    Material(
      known = true,
      white = white,
      black = black,
      diff = white.value - black.value,
      imbalance = 0
    )

  private def piecesFor(board: Board, color: Color): Pieces =
    val pawns = board.count(color, Pawn)
    val knights = board.count(color, Knight)
    val bishops = board.count(color, Bishop)
    val rooks = board.count(color, Rook)
    val queens = board.count(color, Queen)
    val kings = board.count(color, King)
    Pieces(
      pawns = pawns,
      knights = knights,
      bishops = bishops,
      rooks = rooks,
      queens = queens,
      kings = kings,
      value = pawns * 100 + knights * 320 + bishops * 330 + rooks * 500 + queens * 900
    )

  private def pawns(board: Board, root: RootStateVector): Pawns =
    Pawns(
      known = true,
      white = pawnSide(board, root, Color.White),
      black = pawnSide(board, root, Color.Black)
    )

  private def pawnSide(board: Board, root: RootStateVector, color: Color): PawnSide =
    val pawnSquares = Bitboard.squares(board.byPiece(color, Pawn)).toVector
    PawnSide(
      fileCounts = packedPawnFileCounts(pawnSquares),
      isolated = rootPawnCount(root, RootAtomRegistry.SchemaId.IsolatedPawn, color),
      backward = rootPawnCount(root, RootAtomRegistry.SchemaId.BackwardPawn, color),
      doubledFiles =
        root.fileMask8(RootAtomRegistry.SchemaId.DoubledFile, Some(color)).fold(0)(Integer.bitCount),
      passed = rootPawnCount(root, RootAtomRegistry.SchemaId.PassedPawn, color),
      candidatePassers = rootPawnCount(root, RootAtomRegistry.SchemaId.CandidatePasser, color),
      protectedPassers = 0,
      fixed = rootPawnCount(root, RootAtomRegistry.SchemaId.FixedPawn, color),
      chainBases = 0,
      levers = rootPawnCount(root, RootAtomRegistry.SchemaId.LeverAvailable, color),
      breakChances = 0,
      blockaded = 0,
      bestPromotionDistance = bestPromotionDistance(pawnSquares, color),
      support = 0,
      risk = 0,
      structure = 0
    )

  private def rootPawnCount(root: RootStateVector, schemaId: String, color: Color): Int =
    java.lang.Long.bitCount(root.squareMask64(schemaId, Some(color)).getOrElse(0L))

  private def packedPawnFileCounts(pawnSquares: Vector[ChessSquare]): Int =
    pawnSquares.foldLeft(0): (packed, square) =>
      val shift = square.file.value * 4
      val count = ((packed >>> shift) & 0xf) + 1
      (packed & ~(0xf << shift)) | (count << shift)

  private def bestPromotionDistance(pawnSquares: Vector[ChessSquare], color: Color): Int =
    if pawnSquares.isEmpty then 0
    else
      pawnSquares
        .map: square =>
          if color.white then 7 - square.rank.value else square.rank.value
        .min

  private def pieces(board: Board): Vector[Piece] =
    board.pieceMap.iterator
      .map:
        case (square, piece) =>
          Piece(sideFromColor(piece.color), manFromRole(piece.role), Square.fromIndex(square.value))
      .toVector

  private def plyFromStart(fullmoveNumber: Int, color: Color): Int =
    (fullmoveNumber - 1) * 2 + (if color.black then 1 else 0)

  private def inCheckMask(position: Position): Int =
    (if position.isCheck(Color.White).yes then 1 else 0) |
      (if position.isCheck(Color.Black).yes then 2 else 0)

  private def castlingMask(position: Position): Int =
    val castles = position.history.castles
    (if castles.whiteKingSide then 1 else 0) |
      (if castles.whiteQueenSide then 2 else 0) |
      (if castles.blackKingSide then 4 else 0) |
      (if castles.blackQueenSide then 8 else 0)

  private def phaseTotal(board: Board): Int =
    RootAtomRegistry.canonicalColors
      .map: color =>
        board.count(color, Knight) + board.count(color, Bishop) +
          board.count(color, Rook) * 2 + board.count(color, Queen) * 4
      .sum

  private def phaseNonPawn(board: Board): Int =
    RootAtomRegistry.canonicalColors
      .map: color =>
        board.count(color, Knight) + board.count(color, Bishop) + board
          .count(color, Rook) + board.count(color, Queen)
      .sum

  private def sideFromColor(color: Color): Side =
    if color.white then Side.White else Side.Black

  private def manFromRole(role: Role): Man =
    role match
      case Pawn => Man.Pawn
      case Knight => Man.Knight
      case Bishop => Man.Bishop
      case Rook => Man.Rook
      case Queen => Man.Queen
      case King => Man.King
