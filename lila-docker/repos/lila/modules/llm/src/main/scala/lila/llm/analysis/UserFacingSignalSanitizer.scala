package lila.llm.analysis

private[analysis] object UserFacingSignalSanitizer:

  private val placeholderRewrites: List[(String, String)] = List(
    "probe needed for validation" -> "more confirmation is still needed",
    "under strict evidence mode" -> "under the current evidence threshold",
    "supported by engine-coupled continuation" -> "supported by the current engine line",
    "supported by engine coupled continuation" -> "supported by the current engine line",
    "probe evidence pending" -> "confirmation is still pending",
    "probe contract passed but support signal is insufficient" -> "current supporting evidence is still thin",
    "{them}" -> "the opponent",
    "{us}" -> "the attacking side",
    "{seed}" -> "the intended pawn lever"
  )
  private val rawLabelRegex = """\b(?:subplan|theme|support|seed|proposal):([a-z0-9_]+)\b""".r
  private val bracketedSubplanRegex = """\s*\[subplan:[^\]]+\]""".r
  private val placeholderPatterns: List[String] = List(
    "probe needed for validation",
    "under strict evidence mode",
    "supported by engine coupled continuation",
    "supported by engine-coupled continuation",
    "probe evidence pending",
    "probe contract passed but support signal is insufficient",
    "[subplan:",
    "subplan:",
    "theme:",
    "support:",
    "seed:",
    "proposal:",
    "{them}",
    "{us}",
    "{seed}"
  )

  def sanitize(raw: String): String =
    cleanup(
      collapseWhitespace(
        rawLabelRegex
          .replaceAllIn(
            placeholderRewrites.foldLeft(bracketedSubplanRegex.replaceAllIn(Option(raw).getOrElse(""), "")) {
              case (acc, (needle, replacement)) => acc.replace(needle, replacement)
            },
            m => humanizeLabel(m.group(1))
          )
      )
    )

  def placeholderHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    placeholderPatterns.filter(low.contains)

  private def humanizeLabel(raw: String): String =
    Option(raw).getOrElse("").replace('_', ' ').trim

  private def collapseWhitespace(text: String): String =
    text
      .replaceAll("""[ \t]+""", " ")
      .replaceAll("""\s+\.(?!\.)""", ".")
      .replaceAll("""\s+,""", ",")
      .replaceAll("""\(\s+""", "(")
      .replaceAll("""\s+\)""", ")")
      .trim

  private def cleanup(text: String): String =
    normalizeChessMarkers(
      text
        .replaceAll("""\s{2,}""", " ")
        .replaceAll("""\.{4,}""", "...")
        .replaceAll("""\s+([;:]|[.](?![.]))""", "$1")
        .replaceAll("""(?<!\.)([.;:])(?!\.)([A-Za-z])""", "$1 $2")
        .trim
    )

  private def normalizeChessMarkers(text: String): String =
    Option(text)
      .getOrElse("")
      .replaceAll(
        """(\d+)\.\.\s+(?=(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?))""",
        "$1..."
      )
      .replaceAll(
        """([A-Za-z])\.\.\s+(?=(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?))""",
        "$1 ..."
      )
      .replaceAll(
        """\b(starts with|begins with|with|after)\.\s+(?=(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?))""",
        "$1 ..."
      )
