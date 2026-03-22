package controllers

object Chess960Analysis:

  private val MinPosition = 0
  private val MaxPosition = 959

  def parsePosition(queryValue: Option[String]): Option[Int] =
    queryValue
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(_.toIntOption)
      .filter: position =>
        position >= MinPosition && position <= MaxPosition

  def positionFen(queryValue: Option[String]): Option[chess.format.Fen.Full] =
    parsePosition(queryValue).flatMap(chess.variant.Chess960.positionToFen)

  def fenPathArg(fen: chess.format.Fen.Full): String =
    s"${chess.variant.Chess960.key.value}/${fen.value.replace(" ", "_")}"

  def canonicalArgForPosition(queryValue: Option[String]): Option[String] =
    positionFen(queryValue).map(fenPathArg)

  def positionNumber(
      variant: chess.variant.Variant,
      fen: Option[chess.format.Fen.Full]
  ): Option[Int] =
    if variant.chess960 then fen.flatMap(chess.variant.Chess960.positionNumber)
    else None
