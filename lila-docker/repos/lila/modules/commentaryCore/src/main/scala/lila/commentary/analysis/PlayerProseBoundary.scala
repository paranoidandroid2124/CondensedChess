package lila.commentary.analysis

import lila.commentary.analysis.semantic.RelationObservationCatalog
import scala.util.matching.Regex

private[commentary] object PlayerProseBoundary:

  final case class Evaluation(
      text: String,
      reasons: List[String]
  ):
    def isAccepted: Boolean = reasons.isEmpty

  private def relationHelperPattern(kind: String): Regex =
    val parts = kind.split("_").toList.filter(_.nonEmpty)
    val separated = parts.map(java.util.regex.Pattern.quote).mkString("""[_\-\s]*""")
    val compact = java.util.regex.Pattern.quote(parts.mkString(""))
    raw"""(?i)\b(?:$separated|$compact)\s*\(""".r

  private val RelationHelperLeakPatterns: List[(String, Regex)] =
    RelationObservationCatalog.InventoryKinds.distinct.map(kind => kind -> relationHelperPattern(kind))

  private val HelperLeakPatterns: List[(String, Regex)] = RelationHelperLeakPatterns ++ List(
    "pin" -> """(?i)\bpin\s*\(""".r,
    "maneuver" -> """(?i)\bmaneuver\s*\(""".r,
    "xray" -> """(?i)\bx\s*-?\s*ray\s*\(""".r,
    "winning_capture" -> """(?i)\bwinningcapture\s*\(""".r,
    "open_file_control" -> """(?i)\bopenfilecontrol\s*\(""".r,
    "centralization" -> """(?i)\bcentralization\s*\(""".r,
    "discovered_attack" -> """(?i)\bdiscoveredattack\s*\(""".r,
    "skewer" -> """(?i)\bskewer\s*\(""".r,
    "rook_lift" -> """(?i)\brooklift\s*\(""".r,
    "domination" -> """(?i)\bdomination\s*\(""".r,
    "trapped_piece" -> """(?i)\btrappedpiece\s*\(""".r,
    "knight_vs_bishop" -> """(?i)\bknightvsbishop\s*\(""".r,
    "blockade" -> """(?i)\bblockade\s*\(""".r,
    "smothered_mate" -> """(?i)\bsmotheredmate\s*\(""".r,
    "check" -> """(?i)\bcheck\s*\(""".r,
    "fork" -> """(?i)\bfork\s*\(""".r
  )

  private val BrokenFragmentPatterns = List(
    """(?i)^(?:after|before)\s+\d+(?:\.\.\.)?\.?$""".r,
    """(?i)^(?:after|before)\s+\d+\.\.\.[a-z0-9+#=]+$""".r,
    """(?i)^(?:and|but|because|with|from|then)\s+\d+(?:\.\.\.)?\.?$""".r
  )

  def sanitize(raw: String): String =
    UserFacingSignalSanitizer.sanitize(raw).trim

  def validate(raw: String): Evaluation =
    validateSanitized(sanitize(raw))

  def validateSanitized(raw: String): Evaluation =
    val trimmed = Option(raw).getOrElse("").trim
    val reasons = List.newBuilder[String]
    if trimmed.nonEmpty && looksJsonWrapper(trimmed) then reasons += "json_wrapper_unparsed"
    if trimmed.nonEmpty && looksTruncated(trimmed) then reasons += "truncated_output"
    if trimmed.nonEmpty && placeholderHits(trimmed).nonEmpty then reasons += "placeholder_leak_detected"
    if trimmed.nonEmpty && metaLeakHits(trimmed).nonEmpty then reasons += "meta_label_leak_detected"
    if trimmed.nonEmpty && helperLeakHits(trimmed).nonEmpty then reasons += "helper_symbol_leak_detected"
    if trimmed.nonEmpty && brokenFragmentHits(trimmed).nonEmpty then reasons += "broken_fragment_detected"
    if trimmed.nonEmpty && duplicateSentenceHits(trimmed).nonEmpty then reasons += "duplicate_sentence_detected"

    Evaluation(text = trimmed, reasons = reasons.result().distinct)

  def placeholderHits(raw: String): List[String] =
    UserFacingSignalSanitizer.placeholderHits(raw)

  def metaLeakHits(raw: String): List[String] =
    FullGameDraftNormalizer.metaLeakHits(raw)

  def helperLeakHits(raw: String): List[String] =
    val text = Option(raw).getOrElse("")
    HelperLeakPatterns.collect { case (label, pattern) if pattern.findFirstIn(text).nonEmpty => label }.distinct

  def brokenFragmentHits(raw: String): List[String] =
    splitSentences(raw).flatMap { sentence =>
      val trimmed = sentence.trim
      if trimmed.nonEmpty && BrokenFragmentPatterns.exists(_.matches(trimmed)) then Some("fragment")
      else None
    }.distinct

  def duplicateSentenceHits(raw: String): List[String] =
    val sentences = splitSentences(raw).map(normalizeSentenceFingerprint).filter(_.nonEmpty)
    sentences
      .groupBy(identity)
      .collect { case (fingerprint, rows) if rows.size > 1 && fingerprint.length >= 12 => "duplicate" }
      .toList
      .distinct

  def looksJsonWrapper(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    trimmed.startsWith("{") && trimmed.contains("\"commentary\"")

  def looksTruncated(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    if trimmed.isEmpty then false
    else
      def balanced(open: Char, close: Char): Boolean =
        trimmed.count(_ == open) == trimmed.count(_ == close)
      val quoteCount = trimmed.count(_ == '"')
      !balanced('{', '}') || !balanced('[', ']') || (quoteCount % 2 != 0) || trimmed.endsWith("\\") ||
        lila.commentary.MoveAnchorCodec.hasBrokenAnchorPrefix(trimmed)

  private def splitSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def normalizeSentenceFingerprint(text: String): String =
    Option(text)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
