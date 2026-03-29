package lila.llm.analysis

import scala.util.matching.Regex

private[llm] object UserFacingSignalSanitizer:

  private val placeholderRewrites: List[(String, String)] = List(
    "deferred as PlayableByPV under strict evidence mode" -> "deferred under the current evidence threshold",
    "accepted as PlayableByPV fallback" -> "accepted as an engine-backed fallback",
    "PlayableByPV" -> "an engine-backed continuation",
    "PlayedPV" -> "the played branch",
    "probe contract" -> "supporting evidence",
    "probe needed for validation" -> "",
    "under strict evidence mode" -> "under the current evidence threshold",
    "supported by engine-coupled continuation" -> "supported by the current engine line",
    "supported by engine coupled continuation" -> "supported by the current engine line",
    "engine-coupled continuation" -> "current engine line",
    "engine coupled continuation" -> "current engine line",
    "probe evidence pending" -> "confirmation is still pending",
    "probe contract passed but support signal is insufficient" -> "confirmation is still pending",
    "{them}" -> "the opponent",
    "{us}" -> "the attacking side",
    "{seed}" -> "the intended pawn lever",
    "coordination improvement" -> "better piece coordination",
    "plan activation lane" -> "the main plan",
    "a cleaner bishop circuit" -> "a better bishop route",
    "piece activation before the break" -> "a better square before the break",
    "in the foreground via" -> "with",
    "so the plan cannot drift" -> "so the idea stays clear",
    "more confirmation is still needed" -> ""
  )
  private val rawLabelRegex = """\b(?:subplan|theme|support|seed|proposal):([a-z0-9_]+)\b""".r
  private val bracketedSubplanRegex = """\s*\[subplan:[^\]]+\]""".r
  private val placeholderLiteralPatterns: List[String] = List(
    "playablebypv",
    "playedpv",
    "probe needed for validation",
    "under strict evidence mode",
    "supported by engine coupled continuation",
    "supported by engine-coupled continuation",
    "probe evidence pending",
    "probe contract passed but support signal is insufficient",
    "{them}",
    "{us}",
    "{seed}",
    "engine-coupled continuation",
    "return vector",
    "cash out",
    "????.??.??"
  )
  private val placeholderRegexes: List[(String, String)] = List(
    "subplan" -> """(?i)\[subplan:[^\]]+\]""",
    "raw_label" -> """(?i)\b(?:subplan|theme|support|seed|proposal):[a-z0-9_]+\b"""
  )
  private val clubPlayerRegexRewrites: List[(Regex, String)] = List(
    """(?i)\bthe compensation has to cash out through\b""".r -> "the compensation is still built around",
    """(?i)\bthe compensation still has to cash out toward\b""".r -> "the compensation still needs to point toward",
    """(?i)\bcash out the compensation into\b""".r -> "turn the compensation into",
    """(?i)\ba clean cash out\b""".r -> "it pays off cleanly",
    """(?i)\bcash out cleanly\b""".r -> "pay off cleanly",
    """(?i)\bcashing out the compensation\b""".r -> "the compensation paying off",
    """(?i)\bcash out\b""".r -> "pay off",
    """(?i)\b(?:the\s+)?return vector only holds if\b""".r -> "the compensation depends on",
    """(?i)\b(?:the\s+)?return vector through\b""".r -> "a path to compensation through",
    """(?i)\breturn vector\b""".r -> "path to compensation",
    """(?i)\bdelayed recovery only works if\b""".r -> "waiting before winning the material back makes the most sense while",
    """(?i)\bdelayed recovery\b""".r -> "waiting before winning the material back",
    """(?i)\bopen-line pressure\b""".r -> "pressure along the open lines",
    """(?i)\bline pressure\b""".r -> "continuing pressure",
    """(?i)\bUnknown and its (?:fluid|locked|symmetric|semi-open|open) center\b""".r -> "the current structure",
    """(?i)\bUnknown\b""".r -> "the current structure",
    """(?i)\s+\((?:contested|build)\)""".r -> "",
    """(?i)\bstill looks playable in the engine line, but it needs stronger support beyond that line\b""".r -> "",
    """(?i)\bis not promoted yet because the idea still looks playable, but the supporting evidence is still thin\b""".r -> "",
    """(?i)\beasier to organize\b""".r -> "easier to carry out"
  )
  def sanitize(raw: String): String =
    cleanup(
      collapseWhitespace(
        PlayerFacingSupportPolicy.rewriteSurfaceLabels(
          rewriteHelperNotation(
            clubPlayerRegexRewrites
              .foldLeft(
                rawLabelRegex
                  .replaceAllIn(
                    placeholderRewrites.foldLeft(bracketedSubplanRegex.replaceAllIn(Option(raw).getOrElse(""), "")) {
                      case (acc, (needle, replacement)) => acc.replace(needle, replacement)
                    },
                    m => humanizeLabel(m.group(1))
                  )
              ) {
                case (acc, (pattern, replacement)) => pattern.replaceAllIn(acc, replacement)
              }
          )
        )
      )
    )

  def placeholderHits(raw: String): List[String] =
    val source = Option(raw).getOrElse("")
    val low = source.toLowerCase
    val literalHits = placeholderLiteralPatterns.filter(low.contains)
    val regexHits =
      placeholderRegexes.flatMap { case (label, pattern) =>
        val regex = pattern.r
        regex.findFirstMatchIn(source).map(_ => label)
      }
    (literalHits ++ regexHits).distinct

  def allowCompensationSupportText(raw: String): Boolean =
    val text = sanitize(raw).trim
    PlayerFacingSupportPolicy.allowCompensationSupportText(text)

  private def humanizeLabel(raw: String): String =
    Option(raw).getOrElse("").replace('_', ' ').trim

  private def rewriteHelperNotation(text: String): String =
    Option(text)
      .getOrElse("")
      .replaceAll("""(?i)\bPin\([^)]*\)""", "pin pressure")
      .replaceAll("""(?i)\bWinningCapture\([^)]*\)""", "a winning capture")
      .replaceAll("""(?i)\bDiscoveredAttack\([^)]*\)""", "a discovered attack")
      .replaceAll("""(?i)\bOpenFileControl\([^)]*\)""", "pressure on the open file")
      .replaceAll("""(?i)\bCentralization\([^)]*\)""", "piece improvement")
      .replaceAll("""(?i)\bRookLift\([^)]*\)""", "a rook lift")
      .replaceAll("""(?i)\bDomination\([^)]*\)""", "piece domination")
      .replaceAll("""(?i)\bManeuver\([^)]*\)""", "piece improvement")
      .replaceAll("""(?i)\bTrappedPiece\([^)]*\)""", "a trapped piece")
      .replaceAll("""(?i)\bKnightVsBishop\([^)]*\)""", "the knight against the bishop")
      .replaceAll("""(?i)\bBlockade\([^)]*\)""", "a blockade")
      .replaceAll("""(?i)\bSmotheredMate\([^)]*\)""", "smothered-mate ideas")
      .replaceAll("""(?i)\bXRay\([^)]*\)""", "x-ray pressure")
      .replaceAll("""(?i)\bSkewer\([^)]*\)""", "a skewer")
      .replaceAll("""(?i)\bFork\([^)]*\)""", "fork pressure")
      .replaceAll("""(?i)\bCheck\([^)]*\)""", "checking pressure")

  private def collapseWhitespace(text: String): String =
    text
      .replaceAll("""[ \t]+""", " ")
      .replaceAll("""\s+\.(?!\.)""", ".")
      .replaceAll("""\s+,""", ",")
      .replaceAll("""\(\s+""", "(")
      .replaceAll("""\s+\)""", ")")
      .replaceAll("""\(\)""", "")
      .trim

  private def cleanup(text: String): String =
    normalizeChessMarkers(
      text
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replaceAll("""(?i)\bplayed\s*pv\b""", "the played branch")
        .replaceAll("""(?i)\bthe\s+the\b""", "the")
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
