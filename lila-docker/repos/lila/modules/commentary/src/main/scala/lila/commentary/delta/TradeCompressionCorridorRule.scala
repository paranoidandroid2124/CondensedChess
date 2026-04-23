package lila.commentary.delta

import chess.{ Bishop, Color, Knight, Rook, Square }

import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSupport, WitnessValue }

private[delta] object TradeCompressionCorridorRule extends StrategicDeltaRule:

  val familyId: StrategicDeltaId = StrategicDeltaId("TradeCompressionCorridor")

  private val transitionTag = StrategicDeltaTag("transition_compression")
  private val boardAnchor = WitnessAnchor.BoardAnchor
  private val helperTags = Vector(
    "compressed_trade_window",
    "reciprocal_exchange_corridor",
    "trade_compression_transition"
  )

  def extract(
      context: StrategicDeltaContext,
      _extractedSoFar: StrategicDeltaSet
  ): Vector[StrategicDelta] =
    tradeCompressionTransition(context)
      .filterNot(_ => forbiddenTradeInvariantRival(context))
      .flatMap: transition =>
        context.moverColor.map: moverColor =>
          owned(
            color = moverColor,
            scope = StrategicDeltaScope.MoveLocal,
            deltaTag = transitionTag,
            anchor = boardAnchor,
            payload = transitionPayload(context, transition),
            support = transitionSupport(context, transition)
          )
      .toVector

  private def tradeCompressionTransition(
      context: StrategicDeltaContext
  ): Option[TradeCompressionTransition] =
    reciprocalExchangeCorridor(context.after)
      .filter: _ =>
        context.capturesNonKingPiece &&
        context.nonKingNonPawnReduction == 1 &&
        compressedTradeWindow(context.after) &&
        beforeBoardFailedTransitionShape(context.before)
      .map(TradeCompressionTransition.apply)

  private def beforeBoardFailedTransitionShape(
      before: lila.commentary.strategic.StrategicObjectContext
  ): Boolean =
    !reciprocalExchangeCorridorShape(before)

  private def forbiddenTradeInvariantRival(context: StrategicDeltaContext): Boolean =
    context.moverColor.exists(color => TradeInvariantAdmission.admissionCarrierEvidence(context, color).nonEmpty)

  private def compressedTradeWindow(
      boardContext: lila.commentary.strategic.StrategicObjectContext
  ): Boolean =
    boardContext.activePieceSquares(Color.White, chess.Queen).isEmpty &&
      boardContext.activePieceSquares(Color.Black, chess.Queen).isEmpty &&
      Vector(Knight, Bishop, Rook)
        .map(role =>
          boardContext.activePieceSquares(Color.White, role).size +
            boardContext.activePieceSquares(Color.Black, role).size
        )
        .sum <= 4

  private def reciprocalExchangeCorridor(
      context: lila.commentary.strategic.StrategicObjectContext
  ): Option[CorridorPair] =
    val pairs = reciprocalExchangeCorridorPairs(context)
    Option.when(pairs.size == 1)(pairs.head)

  private def reciprocalExchangeCorridorShape(
      context: lila.commentary.strategic.StrategicObjectContext
  ): Boolean =
    compressedTradeWindow(context) && reciprocalExchangeCorridorPairs(context).size == 1

  private def reciprocalExchangeCorridorPairs(
      context: lila.commentary.strategic.StrategicObjectContext
  ): Vector[CorridorPair] =
    val whiteAttackers = attackableNonKingSquares(context, Color.White)
    val blackAttackers = attackableNonKingSquares(context, Color.Black)

    whiteAttackers
      .flatMap: whiteSquare =>
        blackAttackers.collect:
          case blackSquare
              if sharesCorridor(whiteSquare, blackSquare) &&
                context.board.attacksSquare(whiteSquare, blackSquare) &&
                context.board.attacksSquare(blackSquare, whiteSquare) =>
            CorridorPair(
              whiteSquare = whiteSquare,
              blackSquare = blackSquare,
              kind = corridorKind(whiteSquare, blackSquare)
            )
      .distinct
      .sortBy(_.sortKey)

  private def attackableNonKingSquares(
      context: lila.commentary.strategic.StrategicObjectContext,
      color: Color
  ): Vector[Square] =
    Vector(Knight, Bishop, Rook).flatMap(role => context.activePieceSquares(color, role))

  private def sharesCorridor(left: Square, right: Square): Boolean =
    left.file == right.file || left.onSameDiagonal(right)

  private def corridorKind(left: Square, right: Square): CorridorKind =
    if left.file == right.file then CorridorKind.File else CorridorKind.Diagonal

  private def transitionPayload(
      context: StrategicDeltaContext,
      transition: TradeCompressionTransition
  ): WitnessPayload =
    val capturedRole =
      context.destinationPieceBefore
        .map(_.role)
        .getOrElse(throw IllegalStateException("TradeCompressionCorridor requires a captured piece"))

    WitnessPayload(
      "corridorPairSquares" -> WitnessValue.SquareListValue(transition.canonicalPair.squares),
      "corridorKind" -> WitnessValue.Token(transition.canonicalPair.kind.key),
      "captureSquare" -> WitnessValue.SquareValue(context.playedMove.dest),
      "capturedRole" -> WitnessValue.RoleValue(capturedRole)
    )

  private def transitionSupport(
      context: StrategicDeltaContext,
      transition: TradeCompressionTransition
  ): WitnessSupport =
    val afterPairIndices =
      transition.canonicalPair.squares.flatMap: square =>
        context.after.pieceAt(square).flatMap(piece => context.after.pieceOnRootIndex(piece.color, piece.role, square))

    val capturedIndex =
      context.destinationPieceBefore.flatMap(piece =>
        context.before.pieceOnRootIndex(piece.color, piece.role, context.playedMove.dest)
      )

    val withRoots =
      (afterPairIndices ++ capturedIndex.toVector).foldLeft(WitnessSupport.empty): (acc, index) =>
        acc.addRootIndex(index)
    val withTargets =
      transition.canonicalPair.squares.foldLeft(withRoots): (acc, square) =>
        acc.addTargetSquare(square)

    helperTags.foldLeft(withTargets): (acc, tag) =>
      acc.addTag(tag)

  private enum CorridorKind(val key: String):
    case File extends CorridorKind("file")
    case Diagonal extends CorridorKind("diagonal")

  private final case class CorridorPair(
      whiteSquare: Square,
      blackSquare: Square,
      kind: CorridorKind
  ):
    def squares: Vector[Square] = Vector(whiteSquare, blackSquare)
    def sortKey: String = s"${whiteSquare.key}|${blackSquare.key}|${kind.key}"

  private final case class TradeCompressionTransition(canonicalPair: CorridorPair)
