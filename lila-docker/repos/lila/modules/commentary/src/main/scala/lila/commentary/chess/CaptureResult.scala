package lila.commentary.chess

private[commentary] final case class CaptureResult(
    side: Side,
    capturingPiece: Option[Piece],
    targetPiece: Option[Piece],
    captureLine: Line,
    capturedValue: Option[Int],
    recaptureCandidates: Vector[CaptureResult.Recapture],
    materialResult: Option[Int],
    sameBoardProof: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def positiveMaterial: Boolean = missingEvidence.isEmpty && materialResult.exists(_ > 0)

private[commentary] object CaptureResult:
  final case class Recapture(piece: Piece, line: Line)

  def fromBoardFacts(facts: BoardFacts, captureLine: Line): CaptureResult =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val piecesBySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val targetPiece = piecesBySquare.get(captureLine.to)
    val legalSide = legalSideFor(facts, captureLine)
    val capturingPiece =
      piecesBySquare
        .get(captureLine.from)
        .filter(piece => legalSide.contains(piece.side))

    val targetEnemy = targetPiece.exists(target => capturingPiece.exists(_.side != target.side))
    val targetNonKing = targetPiece.forall(_.man != Man.King)
    val legalCapture =
      legalSide.nonEmpty &&
        capturingPiece.nonEmpty &&
        targetEnemy &&
        targetNonKing
    val materialKnown = facts.material.sane
    val capturedValue = targetPiece.filter(target => targetEnemy && target.man != Man.King).flatMap(pieceValue)
    val recaptures =
      if legalCapture && targetNonKing then recaptureCandidates(facts.pieces, captureLine, targetPiece.get)
      else Vector.empty
    val materialResult =
      Option.when(legalCapture && targetNonKing && materialKnown)(capturedValue.getOrElse(0) - recaptureCost(capturingPiece, recaptures))

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(!legalCapture)("legal capture"),
      Option.when(capturingPiece.isEmpty)("capturing piece"),
      Option.when(targetPiece.isEmpty)("target piece"),
      Option.when(targetPiece.nonEmpty && !targetEnemy)("target enemy piece"),
      Option.when(!targetNonKing)("target non-king"),
      Option.when(materialResult.isEmpty)("material result")
    ).flatten

    CaptureResult(
      side = legalSide.getOrElse(capturingPiece.map(_.side).getOrElse(Side.None)),
      capturingPiece = capturingPiece,
      targetPiece = targetPiece,
      captureLine = captureLine,
      capturedValue = capturedValue,
      recaptureCandidates = recaptures,
      materialResult = materialResult,
      sameBoardProof = sameBoardProof,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("CaptureResult", missing))
    )

  private def legalSideFor(facts: BoardFacts, line: Line): Option[Side] =
    if facts.sideLegal.lines.contains(line) then Some(facts.sideToMove)
    else if facts.rivalLegal.lines.contains(line) then Some(BoardFacts.opposite(facts.sideToMove))
    else None

  private def recaptureCost(capturingPiece: Option[Piece], recaptures: Vector[Recapture]): Int =
    if recaptures.isEmpty then 0
    else capturingPiece.flatMap(pieceValue).getOrElse(0)

  private def recaptureCandidates(
      pieces: Vector[Piece],
      captureLine: Line,
      targetPiece: Piece
  ): Vector[Recapture] =
    val occupiedAfter =
      pieces
        .filterNot(piece => piece.square == captureLine.from || piece.square == captureLine.to)
        .foldLeft(captureLine.to.bit): (mask, piece) =>
          mask | piece.square.bit
    pieces
      .filter(piece => piece.side == targetPiece.side && piece != targetPiece)
      .filter(piece => BoardFacts.attacksSquare(piece, captureLine.to, occupiedAfter))
      .map(piece => Recapture(piece, Line(piece.square, captureLine.to)))
      .sortBy(recapture => (recapture.piece.man.ordinal, recapture.piece.square.index))

  private def pieceValue(piece: Piece): Option[Int] =
    piece.man match
      case Man.Pawn => Some(100)
      case Man.Knight => Some(320)
      case Man.Bishop => Some(330)
      case Man.Rook => Some(500)
      case Man.Queen => Some(900)
      case Man.King => None
