package lila.commentary.analysis

import scala.util.matching.Regex

private[commentary] object PlayerFacingSupportPolicy:

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
      """(?i)\bPreparing ([A-H])[- ](?:pawn\s+)?break Break\b""".r -> (m => s"Preparing the ${m.group(1).toLowerCase}-break"),
      """(?i)\bPreparing ([A-H])[- ](?:pawn\s+)?break\b""".r -> (m => s"Preparing the ${m.group(1).toLowerCase}-break"),
      """(?i)\b([A-H])[- ](?:pawn\s+)?break Break\b""".r -> (m => s"${m.group(1).toLowerCase}-break"),
      """(?i)\b([A-H])[- ](?:pawn\s+)?break\b""".r -> (m => s"${m.group(1).toLowerCase}-break"),
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
