package lila.llm.analysis

import scala.util.matching.Regex

private[llm] object PlayerFacingSupportPolicy:

  private val supportAbstractPattern =
    """(?i)\b(initiative|counterplay|compensation|conversion|pressure|attack|plan|campaign|objective|execution)\b""".r
  private val supportConcretePatterns: List[Regex] = List(
    """(?i)\b[a-h][1-8]\b""".r,
    """(?i)\b[a-h]-file\b""".r,
    """(?i)\b(queenside|kingside|central|open)\s+files?\b""".r,
    """(?i)\b(queenside|kingside|central|fixed)\s+targets?\b""".r,
    """(?i)\b(light|dark)-squared\b""".r,
    """(?i)\b(exchange|trade|recapture|pawn break|break|castling|castle|back-rank)\b""".r,
    """(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r,
    """\.\.\.(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r,
    """(?i)\b(king|queen|rook|bishop|knight|pawn)s?\b""".r
  )

  private val compensationAbstractSupportRows: List[Regex] = List(
    """(?i)^improving piece placement$""".r,
    """(?i)^development and central control$""".r,
    """(?i)^immediate counterplay$""".r,
    """(?i)^attacking a fixed pawn$""".r,
    """(?i)^using the space advantage$""".r,
    """(?i)^simplifying with favorable exchanges$""".r,
    """(?i)^simplifying toward an endgame$""".r,
    """(?i)^kingside pawn storm$""".r,
    """(?i)^gaining flank space with a rook pawn$""".r
  )
  private val compensationRelevantPattern =
    """(?i)\b(material can wait|winning the material back|compensation|initiative|pressure|attack|open lines?|open files?|queenside files?|central files?|queenside targets?|central targets?|fixed targets?|defenders?|extra pawn)\b""".r
  private val compensationStrongAnchorPatterns: List[Regex] = List(
    """(?i)\b[a-h][1-8]\b""".r,
    """(?i)\b[a-h]-file\b""".r,
    """(?i)\bpressure (?:on|against|along)\b""".r,
    """(?i)\b(?:queen|rook|bishop|knight|king|pawn)s?\b.*\b(?:queenside|central|open)\s+files?\b""".r,
    """(?i)\b(?:queen|rook|bishop|knight|king|pawn)s?\b.*\b(?:can head for|heads? for|to)\s+[a-h][1-8]\b""".r,
    """(?i)\bdefenders?\b.*\bking\b""".r,
    """(?i)\bextra pawn\b.*\bactive\b""".r,
    """(?i)\b[a-h]-break\b""".r,
    """(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r
  )

  def cleanNarrativeSurfaceLabel(raw: String): String =
    val cleaned =
      humanizeToken(
        Option(raw)
          .getOrElse("")
          .replace("**", "")
          .replaceAll("`+", " ")
          .replaceAll("""(?i)\bPlayed\s*PV\b""", "Played Line")
          .replaceAll("""(?i)\bPlayable\s*By\s*PV\b""", "Engine Backed Continuation")
      )
    val rewritten =
      rewriteSurfaceLabels(cleaned)
        .replaceAll("""(?i)\bPlayed Line\b""", "Played line")
        .replaceAll("""(?i)\bEngine Backed Continuation\b""", "Engine-backed continuation")
        .replaceAll("""(?i)\b(\w+)\s+\1\b""", "$1")
        .replaceAll("""\s{2,}""", " ")
        .trim
    if rewritten.nonEmpty then rewritten else Option(raw).getOrElse("")

  def rewriteSurfaceLabels(text: String): String =
    List[(Regex, Regex.Match => String)](
      """(?i)\bStrategic Focus\b""".r -> (_ => "Key theme"),
      """(?i)\bStrategic Priority\b""".r -> (_ => "Key theme"),
      """(?i)\bOpening Branch Point\b""".r -> (_ => "Opening Branch"),
      """(?i)\bPreparing ([A-H])[- ]break Break\b""".r -> (m => s"Preparing the ${m.group(1).toLowerCase}-break"),
      """(?i)\bPreparing ([A-H])[- ]break\b""".r -> (m => s"Preparing the ${m.group(1).toLowerCase}-break"),
      """(?i)\b([A-H])[- ]break Break\b""".r -> (m => s"${m.group(1).toLowerCase}-break"),
      """(?i)\b([A-H])[- ]break\b""".r -> (m => s"${m.group(1).toLowerCase}-break"),
      """(?i)\bOpening Development and Center Control\b""".r -> (_ => "Development and central control"),
      """(?i)\bPiece Activation\b""".r -> (_ => "Improving piece placement"),
      """(?i)\bExploiting Space Advantage\b""".r -> (_ => "Using the space advantage"),
      """(?i)\bExchanging for Favorable Simplification\b""".r -> (_ => "Simplifying with favorable exchanges"),
      """(?i)\bSimplification into Endgame\b""".r -> (_ => "Simplifying toward an endgame"),
      """(?i)\bImmediate Tactical Gain Counterplay\b""".r -> (_ => "Immediate counterplay"),
      """(?i)\bAttacking Fixed Pawn\b""".r -> (_ => "Attacking a fixed pawn"),
      """(?i)\bRook Pawn March To Gain Flank Space\b""".r -> (_ => "Gaining flank space with a rook pawn"),
      """(?i)\bKingside Pawn Storm\b""".r -> (_ => "kingside pawn storm")
    ).foldLeft(text) { case (acc, (pattern, rewrite)) =>
      pattern.replaceAllIn(acc, rewrite)
    }

  def allowPlayerFacingSupportText(text: String): Boolean =
    Option(text).exists { value =>
      value.nonEmpty && (
        supportAbstractPattern.findFirstIn(value).isEmpty ||
          supportConcretePatterns.exists(_.findFirstIn(value).nonEmpty)
      )
    }

  def allowCompensationSupportText(text: String): Boolean =
    text.nonEmpty &&
      !compensationAbstractSupportRows.exists(_.findFirstIn(text).nonEmpty) &&
      (
        compensationRelevantPattern.findFirstIn(text).isEmpty ||
          compensationStrongAnchorPatterns.exists(_.findFirstIn(text).nonEmpty)
      )

  private def humanizeToken(raw: String): String =
    Option(raw)
      .getOrElse("")
      .replaceAll("""[_-]+""", " ")
      .replaceAll("""([a-z])([A-Z])""", "$1 $2")
      .split("\\s+")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(word =>
        if word.length <= 2 then word.toUpperCase
        else word.take(1).toUpperCase + word.drop(1)
      )
      .mkString(" ")
