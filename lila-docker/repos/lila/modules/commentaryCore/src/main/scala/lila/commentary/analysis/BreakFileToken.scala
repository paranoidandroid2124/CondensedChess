package lila.commentary.analysis

private[analysis] object BreakFileToken:

  private val FileMarkerToken =
    """(?i)(?:^|[^a-z0-9])([a-h])[-_](?:file|break|pawn)(?=$|[^a-z0-9])""".r
  private val SquareToken =
    """(?i)(?:^|[^a-z0-9])([a-h])[1-8](?=$|[^a-z0-9])""".r
  private val SingleFileToken =
    """(?i)^\s*([a-h])\s*$""".r

  def extract(raw: String): Option[String] =
    val text = Option(raw).getOrElse("").trim
    if text.isEmpty then None
    else
      FileMarkerToken
        .findFirstMatchIn(text)
        .orElse(SquareToken.findFirstMatchIn(text))
        .orElse(SingleFileToken.findFirstMatchIn(text))
        .map(_.group(1).toLowerCase)

  def extractOrEmpty(raw: String): String =
    extract(raw).getOrElse("")

  def extractOrUnknown(raw: String): String =
    extract(raw).getOrElse("unknown")
