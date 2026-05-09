package lila.commentary.chess

private[commentary] final case class FileOpenedProof(
    side: Side,
    rivalSide: Side,
    pawnBefore: Option[Piece],
    pawnAfter: Option[Piece],
    originSquare: Option[Square],
    destinationSquare: Option[Square],
    originFile: Option[Int],
    openedFile: Option[Int],
    openingMove: Option[Line],
    sameBoardProof: Boolean,
    legalPawnMove: Boolean,
    nonPromotionMove: Boolean,
    ordinaryPawnMoveOrCapture: Boolean,
    enPassantMove: Boolean,
    leavesOriginFile: Boolean,
    originFileOccupiedBeforeByMovingPawn: Boolean,
    originFileOpenBefore: Boolean,
    exactAfterBoardReplay: Boolean,
    afterBoardHasNoWhitePawnOnOriginFile: Boolean,
    afterBoardHasNoBlackPawnOnOriginFile: Boolean,
    originFileOpenAfter: Boolean,
    openedFileIsOriginFile: Boolean,
    missingEvidence: Vector[BoardFacts.MissingEvidence]
):
  val publicClaimAllowed: Boolean = false
  def complete: Boolean =
    missingEvidence.isEmpty &&
      sameBoardProof &&
      legalPawnMove &&
      nonPromotionMove &&
      ordinaryPawnMoveOrCapture &&
      !enPassantMove &&
      leavesOriginFile &&
      originFileOccupiedBeforeByMovingPawn &&
      !originFileOpenBefore &&
      exactAfterBoardReplay &&
      afterBoardHasNoWhitePawnOnOriginFile &&
      afterBoardHasNoBlackPawnOnOriginFile &&
      originFileOpenAfter &&
      openedFileIsOriginFile

private[commentary] object FileOpenedProof:
  def fromBoardFacts(facts: BoardFacts, move: Line): FileOpenedProof =
    val sameBoardProof = BoardFacts.sameBoardReady(facts)
    val legalMove = facts.seen.legalMoves.find(_.line == move)
    val side = legalMove.map(_.side).getOrElse(facts.sideToMove)
    val rivalSide = BoardFacts.opposite(side)
    val bySquare = facts.pieces.map(piece => piece.square -> piece).toMap
    val pawnBefore =
      bySquare
        .get(move.from)
        .filter(piece => piece.man == Man.Pawn && piece.side == side)
        .filter(piece => legalMove.exists(_.piece == piece))
    val legalPawnMove =
      legalMove.nonEmpty &&
        pawnBefore.nonEmpty
    val leavesOriginFile =
      legalPawnMove &&
        move.from.file != move.to.file
    val afterPieces =
      if legalPawnMove then BoardFacts.piecesAfterLegalMove(facts, move).getOrElse(facts.pieces)
      else facts.pieces
    val pawnAfter =
      afterPieces.find(piece => piece.man == Man.Pawn && piece.side == side && piece.square == move.to)
    val nonPromotionMove =
      legalPawnMove &&
        pawnAfter.nonEmpty
    val destinationBefore = bySquare.get(move.to)
    val enPassantMove =
      legalPawnMove &&
        move.from.file != move.to.file &&
        destinationBefore.isEmpty
    val ordinaryPawnMoveOrCapture =
      legalPawnMove &&
        nonPromotionMove &&
        !enPassantMove &&
        (
          (move.from.file == move.to.file && destinationBefore.isEmpty) ||
            (move.from.file != move.to.file && destinationBefore.exists(_.side == rivalSide))
        )
    val originFile = pawnBefore.map(_.square.file)
    val originFileOccupiedBeforeByMovingPawn =
      pawnBefore.exists(pawn => facts.pieces.contains(pawn))
    val originFileOpenBefore =
      originFile.exists(file => pawnsOnFile(facts.pieces, file).isEmpty)
    val exactAfterBoardReplay =
      sameBoardProof &&
        legalPawnMove &&
        nonPromotionMove &&
        ordinaryPawnMoveOrCapture &&
        !enPassantMove &&
        leavesOriginFile &&
        BoardFacts.piecesAfterLegalMove(facts, move).nonEmpty &&
        !afterPieces.exists(_.square == move.from)
    val afterBoardHasNoWhitePawnOnOriginFile =
      exactAfterBoardReplay &&
        originFile.exists(file => pawnsOnFile(afterPieces, Side.White, file).isEmpty)
    val afterBoardHasNoBlackPawnOnOriginFile =
      exactAfterBoardReplay &&
        originFile.exists(file => pawnsOnFile(afterPieces, Side.Black, file).isEmpty)
    val originFileOpenAfter =
      exactAfterBoardReplay &&
        afterBoardHasNoWhitePawnOnOriginFile &&
        afterBoardHasNoBlackPawnOnOriginFile
    val openedFile =
      Option.when(originFileOpenAfter)(originFile).flatten
    val openedFileIsOriginFile =
      openedFile.nonEmpty &&
        openedFile == originFile

    val missing = Vector(
      Option.when(!sameBoardProof)("same-board proof"),
      Option.when(pawnBefore.isEmpty)("pawn identity"),
      Option.when(originFile.isEmpty)("origin file"),
      Option.when(!legalPawnMove)("legal pawn move"),
      Option.when(!nonPromotionMove)("move is non-promotion"),
      Option.when(enPassantMove)("move is not en passant"),
      Option.when(!ordinaryPawnMoveOrCapture)("ordinary pawn move or capture"),
      Option.when(!leavesOriginFile)("pawn leaves origin file"),
      Option.when(!originFileOccupiedBeforeByMovingPawn)("origin file occupied before by moving pawn"),
      Option.when(originFileOpenBefore)("origin file not open before"),
      Option.when(!exactAfterBoardReplay)("exact after-board replay"),
      Option.when(!afterBoardHasNoWhitePawnOnOriginFile)("no white pawn on origin file after"),
      Option.when(!afterBoardHasNoBlackPawnOnOriginFile)("no black pawn on origin file after"),
      Option.when(!originFileOpenAfter)("origin file open on exact after-board"),
      Option.when(!openedFileIsOriginFile)("opened file is origin file")
    ).flatten.distinct

    FileOpenedProof(
      side = side,
      rivalSide = rivalSide,
      pawnBefore = pawnBefore,
      pawnAfter = pawnAfter,
      originSquare = pawnBefore.map(_.square),
      destinationSquare = Some(move.to).filter(_ => legalPawnMove),
      originFile = originFile,
      openedFile = openedFile,
      openingMove = Some(move),
      sameBoardProof = sameBoardProof,
      legalPawnMove = legalPawnMove,
      nonPromotionMove = nonPromotionMove,
      ordinaryPawnMoveOrCapture = ordinaryPawnMoveOrCapture,
      enPassantMove = enPassantMove,
      leavesOriginFile = leavesOriginFile,
      originFileOccupiedBeforeByMovingPawn = originFileOccupiedBeforeByMovingPawn,
      originFileOpenBefore = originFileOpenBefore,
      exactAfterBoardReplay = exactAfterBoardReplay,
      afterBoardHasNoWhitePawnOnOriginFile = afterBoardHasNoWhitePawnOnOriginFile,
      afterBoardHasNoBlackPawnOnOriginFile = afterBoardHasNoBlackPawnOnOriginFile,
      originFileOpenAfter = originFileOpenAfter,
      openedFileIsOriginFile = openedFileIsOriginFile,
      missingEvidence =
        if missing.isEmpty then Vector.empty
        else Vector(BoardFacts.MissingEvidence("FileOpenedProof", missing))
    )

  private def pawnsOnFile(pieces: Vector[Piece], file: Int): Vector[Piece] =
    pieces.filter(piece => piece.man == Man.Pawn && piece.square.file == file)

  private def pawnsOnFile(pieces: Vector[Piece], side: Side, file: Int): Vector[Piece] =
    pieces.filter(piece => piece.side == side && piece.man == Man.Pawn && piece.square.file == file)
