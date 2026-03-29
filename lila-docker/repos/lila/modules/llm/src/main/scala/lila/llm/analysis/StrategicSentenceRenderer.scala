package lila.llm.analysis

private[analysis] object StrategicSentenceRenderer:

  private val PieceHeadForPattern = """(?i)^the ([a-z]+) can head for ([a-h][1-8])$""".r
  private val PieceCanUsePattern = """(?i)^([a-z]+) can use ([a-h][1-8])$""".r
  private val PieceTowardPattern = """(?i)^([a-z]+) toward ([a-h][1-8])(?:\s+(?:for|to)\s+.+)?$""".r
  private val PieceViaPathPattern = """(?i)^([a-z]+) via ([a-h][1-8](?:-[a-h][1-8])+)(?:\s+(?:for|to)\s+.+)?$""".r
  private val BareSquarePattern = """(?i)^([a-h][1-8])$""".r
  private val AnySquarePattern = """(?i)\b([a-h][1-8])\b""".r

  def pieceHeadFor(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceHeadForPattern(piece, square) => Some(piece -> square)
      case _                                  => None

  def pieceCanUse(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceCanUsePattern(piece, square) => Some(piece -> square)
      case _                                 => None

  def pieceToward(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceTowardPattern(piece, square) => Some(piece -> square)
      case _                                 => None

  def pieceViaPath(raw: String): Option[(String, String)] =
    normalizeText(raw) match
      case PieceViaPathPattern(piece, path) => Some(piece -> path)
      case _                                => None

  def renderCompensationAnchor(anchor: String): String =
    val normalized = normalizeText(anchor)

    pieceHeadFor(normalized)
      .map { case (piece, square) => s"That pressure is anchored on the $piece headed for $square." }
      .orElse {
        pieceToward(normalized).map { case (piece, square) =>
          s"That pressure is anchored on the $piece headed for $square."
        }
      }
      .orElse {
        pieceViaPath(normalized).flatMap { case (piece, path) =>
          path.split("-").lastOption.filter(_.nonEmpty).map(square =>
            s"That pressure is anchored on the $piece headed for $square."
          )
        }
      }
      .orElse(pieceCanUse(normalized).map { case (_, square) => s"That pressure is anchored on $square." })
      .orElse(extractAnchorSquare(normalized).map(square => s"That pressure is anchored on $square."))
      .getOrElse(s"That pressure is anchored on ${compensationAnchorNounPhrase(normalized)}.")

  def renderCompensationFollowUpFromExecution(execution: String): Option[String] =
    pieceToward(execution)
      .map { case (piece, square) => s"From there, the $piece can head for $square." }
      .orElse {
        pieceViaPath(execution).flatMap { case (piece, path) =>
          path.split("-").lastOption.filter(_.nonEmpty).map(square => s"From there, the $piece can head for $square.")
        }
      }
      .orElse {
        val normalized = normalizeText(execution)
        Option.when(LiveNarrativeCompressionCore.hasConcreteAnchor(normalized)) {
          s"From there, the clearest continuation is $normalized."
        }
      }

  private def extractAnchorSquare(raw: String): Option[String] =
    normalizeText(raw) match
      case BareSquarePattern(square) => Some(square.toLowerCase)
      case other                     => AnySquarePattern.findAllMatchIn(other).toList.lastOption.map(_.group(1).toLowerCase)

  private def compensationAnchorNounPhrase(raw: String): String =
    val normalized = normalizeText(raw)
    if normalized.startsWith("the ") then normalized
    else if normalized.contains("file") then s"the $normalized"
    else normalized

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim
