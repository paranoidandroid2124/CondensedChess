package lila.llm.analysis

private[llm] object UserFacingProseHardGate:

  final case class Evaluation(
      text: String,
      reasons: List[String]
  ):
    def isAccepted: Boolean = reasons.isEmpty

  private val HelperLeakPatterns = List(
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
    val reasons =
      List(
        Option.when(trimmed.nonEmpty && looksJsonWrapper(trimmed))("json_wrapper_unparsed"),
        Option.when(trimmed.nonEmpty && looksTruncated(trimmed))("truncated_output"),
        Option.when(trimmed.nonEmpty && placeholderHits(trimmed).nonEmpty)("placeholder_leak_detected"),
        Option.when(trimmed.nonEmpty && metaLeakHits(trimmed).nonEmpty)("meta_label_leak_detected"),
        Option.when(trimmed.nonEmpty && helperLeakHits(trimmed).nonEmpty)("helper_symbol_leak_detected"),
        Option.when(trimmed.nonEmpty && brokenFragmentHits(trimmed).nonEmpty)("broken_fragment_detected"),
        Option.when(trimmed.nonEmpty && duplicateSentenceHits(trimmed).nonEmpty)("duplicate_sentence_detected")
      ).flatten.distinct

    Evaluation(text = trimmed, reasons = reasons)

  def placeholderHits(raw: String): List[String] =
    UserFacingSignalSanitizer.placeholderHits(raw)

  def metaLeakHits(raw: String): List[String] =
    FullGameDraftNormalizer.metaLeakHits(raw)

  def helperLeakHits(raw: String): List[String] =
    val text = Option(raw).getOrElse("")
    HelperLeakPatterns.collect { case (label, pattern) if pattern.findFirstIn(text).nonEmpty => label }

  def brokenFragmentHits(raw: String): List[String] =
    splitSentences(raw).flatMap { sentence =>
      val trimmed = sentence.trim
      Option.when(trimmed.nonEmpty && BrokenFragmentPatterns.exists(_.matches(trimmed)))("fragment")
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
        lila.llm.MoveAnchorCodec.hasBrokenAnchorPrefix(trimmed)

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
