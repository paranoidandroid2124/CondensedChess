package lila.llm.analysis

import lila.llm.model.Motif
import lila.llm.model.authoring.EvidenceBranch
import lila.llm.model.strategic.VariationLine

/**
 * Central helper for concrete SAN citations attached to line-scoped claims.
 * If a safe SAN prefix cannot be produced, the claim should be suppressed.
 */
object LineScopedCitation:

  private val MoveNumberToken = """^(?:\d+\.\.\.|\d+\.)$""".r
  private val SourceLabelOnlyPatterns = List(
    """(?i)\bengine line\b""".r,
    """(?i)\bbest line\b""".r,
    """(?i)\brefutation line\b""".r,
    """(?i)\bprincipal variation\b""".r,
    """(?i)\bprimary engine line\b""".r,
    """(?i)\bprobe evidence says\b""".r
  )
  private val InlineCitationPattern =
    """(?i)\b(after|only after|only in|the refutation runs)\s+\d+\.(?:\.\.)?\s*\S+""".r
  private val ConcreteSanTokenPattern =
    """^(?:O-O(?:-O)?[+#]?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|[a-h]x[a-h][1-8](?:=[QRBN])?[+#]?|[a-h][1-8](?:=[QRBN])?[+#]?)$""".r

  private val TacticalFamilies: Set[Class[?]] = Set(
    classOf[Motif.Pin],
    classOf[Motif.Fork],
    classOf[Motif.Skewer],
    classOf[Motif.Check],
    classOf[Motif.MateNet],
    classOf[Motif.BackRankMate],
    classOf[Motif.SmotheredMate],
    classOf[Motif.RemovingTheDefender],
    classOf[Motif.Deflection],
    classOf[Motif.Interference],
    classOf[Motif.DiscoveredAttack],
    classOf[Motif.Decoy],
    classOf[Motif.Overloading],
    classOf[Motif.XRay]
  )

  def sanMoves(fen: String, line: VariationLine): List[String] =
    val parsed = line.parsedMoves.map(_.san.trim).filter(_.nonEmpty)
    if parsed.size == line.moves.size && parsed.nonEmpty then parsed
    else NarrativeUtils.uciListToSan(fen, line.moves).map(_.trim).filter(_.nonEmpty)

  def strategicCitation(
      fen: String,
      startPly: Int,
      line: VariationLine,
      minTokens: Int = 3,
      maxTokens: Int = 4
  ): Option[String] =
    strategicCitationFromSanMoves(startPly, sanMoves(fen, line), minTokens, maxTokens)

  def tacticalCitation(
      fen: String,
      startPly: Int,
      line: VariationLine,
      motifs: List[Motif],
      minTokens: Int = 3,
      maxTokens: Int = 6
  ): Option[String] =
    tacticalCitationFromSanMoves(startPly, sanMoves(fen, line), motifs, minTokens, maxTokens)

  def strategicCitationFromSanMoves(
      startPly: Int,
      sans: List[String],
      minTokens: Int = 3,
      maxTokens: Int = 4
  ): Option[String] =
    val bounded = sans.take(maxTokens).map(_.trim).filter(_.nonEmpty)
    Option.when(bounded.size >= minTokens)(NarrativeUtils.formatSanWithMoveNumbers(startPly, bounded))
      .map(_.trim)
      .filter(_.nonEmpty)

  def tacticalCitationFromSanMoves(
      startPly: Int,
      sans: List[String],
      motifs: List[Motif],
      minTokens: Int = 3,
      maxTokens: Int = 6
  ): Option[String] =
    tacticalTriggerPly(motifs).flatMap { plyIndex =>
      val normalized = sans.map(_.trim).filter(_.nonEmpty)
      val needed = math.max(minTokens, plyIndex + 1)
      val bounded = normalized.take(math.min(maxTokens, needed))
      Option.when(bounded.size >= minTokens && bounded.size >= needed)(NarrativeUtils.formatSanWithMoveNumbers(startPly, bounded))
        .map(_.trim)
        .filter(_.nonEmpty)
    }

  def afterClause(citation: String, body: String): Option[String] =
    val cited = Option(citation).map(_.trim).getOrElse("")
    val rest = Option(body).map(_.trim).getOrElse("").stripSuffix(".")
    Option.when(cited.nonEmpty && rest.nonEmpty)(s"After $cited, $rest.")

  def onlyInClause(citation: String, body: String): Option[String] =
    val cited = Option(citation).map(_.trim).getOrElse("")
    val rest = Option(body).map(_.trim).getOrElse("").stripSuffix(".")
    Option.when(cited.nonEmpty && rest.nonEmpty)(s"Only in $cited does $rest.")

  def evidenceBranchDisplayLine(
      branch: EvidenceBranch,
      minTokens: Int = 3,
      maxTokens: Int = 4
  ): Option[String] =
    truncateRawLineToSanBudget(mergedEvidenceBranchLine(branch), minTokens, maxTokens)

  def evidenceBranchSignature(
      branch: EvidenceBranch,
      minTokens: Int = 3,
      maxTokens: Int = 4
  ): Option[String] =
    val tokens = sanTokensFromRawLine(mergedEvidenceBranchLine(branch)).take(maxTokens)
    Option.when(tokens.size >= minTokens)(tokens.map(normalizeSanToken).mkString("|"))
      .filter(_.nonEmpty)

  def hasInlineCitation(text: String): Boolean =
    InlineCitationPattern.findFirstIn(Option(text).getOrElse("")).nonEmpty

  def hasConcreteSanLine(text: String): Boolean =
    val raw = Option(text).getOrElse("")
    hasInlineCitation(raw) || concreteSanTokensFromRawLine(raw).size >= 3

  def hasSourceLabelOnly(text: String): Boolean =
    val raw = Option(text).getOrElse("")
    !hasInlineCitation(raw) && SourceLabelOnlyPatterns.exists(_.findFirstIn(raw).nonEmpty)

  private def tacticalTriggerPly(motifs: List[Motif]): Option[Int] =
    motifs
      .filter(m => TacticalFamilies.contains(m.getClass))
      .map(_.plyIndex)
      .sorted
      .headOption

  private def mergedEvidenceBranchLine(branch: EvidenceBranch): String =
    val key = Option(branch.keyMove).map(_.trim).getOrElse("")
    val line = Option(branch.line).map(_.trim).getOrElse("")
    if line.isEmpty then key
    else if key.isEmpty then line
    else
      val keyHead = sanTokensFromRawLine(key).headOption
      val lineHead = sanTokensFromRawLine(line).headOption
      if line.startsWith(key) || (keyHead.nonEmpty && keyHead == lineHead) then line
      else s"$key $line".trim

  private def truncateRawLineToSanBudget(
      raw: String,
      minTokens: Int,
      maxTokens: Int
  ): Option[String] =
    val words = expandedWords(raw)
    val kept = scala.collection.mutable.ListBuffer.empty[String]
    var sanCount = 0
    var done = false
    words.foreach { word =>
      if !done then
        val clean = word.trim
        if clean.nonEmpty then
          if isSanToken(clean) then
            if sanCount < maxTokens then
              kept += clean
              sanCount += 1
            else done = true
          else kept += clean
    }
    while kept.lastOption.exists(isMoveNumberToken) do kept.remove(kept.size - 1)
    Option.when(sanCount >= minTokens)(kept.mkString(" ").trim).filter(_.nonEmpty)

  private def sanTokensFromRawLine(raw: String): List[String] =
    expandedWords(raw).filter(isSanToken)

  private def concreteSanTokensFromRawLine(raw: String): List[String] =
    sanTokensFromRawLine(raw).filter(isConcreteSanToken)

  private def expandedWords(raw: String): List[String] =
    Option(raw)
      .getOrElse("")
      .split("\\s+")
      .toList
      .flatMap(splitCombinedToken)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def splitCombinedToken(token: String): List[String] =
    val trimmed = token.trim
    val ellipsisIndex = trimmed.indexOf("...")
    if ellipsisIndex > 0 && ellipsisIndex + 3 < trimmed.length && trimmed.take(ellipsisIndex).forall(_.isDigit) then
      List(trimmed.take(ellipsisIndex + 3), trimmed.drop(ellipsisIndex + 3).trim).filter(_.nonEmpty)
    else
      val dotIndex = trimmed.indexOf('.')
      if dotIndex > 0 && dotIndex + 1 < trimmed.length && trimmed.take(dotIndex).forall(_.isDigit) then
        List(trimmed.take(dotIndex + 1), trimmed.drop(dotIndex + 1).trim).filter(_.nonEmpty)
      else List(trimmed).filter(_.nonEmpty)

  private def isMoveNumberToken(token: String): Boolean =
    MoveNumberToken.pattern.matcher(token.trim).matches()

  private def isSanToken(token: String): Boolean =
    val clean = token.trim
      .replaceAll("""^[\.\u2026]+""", "")
      .replaceAll("""[,:;]+$""", "")
    clean.nonEmpty &&
      !isMoveNumberToken(clean) &&
      !clean.matches("""^[a-z]\)$""")

  private def normalizeSanToken(token: String): String =
    token.trim
      .replaceAll("""^[\.\u2026]+""", "")
      .replaceAll("""[,:;]+$""", "")

  private def isConcreteSanToken(token: String): Boolean =
    ConcreteSanTokenPattern.pattern.matcher(normalizeSanToken(token)).matches()
