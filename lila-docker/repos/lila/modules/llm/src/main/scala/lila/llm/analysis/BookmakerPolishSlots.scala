package lila.llm.analysis

import lila.llm.BookmakerRefsV1
import lila.llm.StrategyPack
import lila.llm.model.*
import lila.llm.model.authoring.NarrativeOutline

final case class BookmakerPolishSlots(
    lens: StrategicLens,
    claim: String,
    supportPrimary: Option[String],
    supportSecondary: Option[String],
    tension: Option[String],
    evidenceHook: Option[String],
    coda: Option[String],
    factGuardrails: List[String],
    paragraphPlan: List[String]
):
  def support: List[String] = List(supportPrimary, supportSecondary).flatten.filter(_.nonEmpty)
  def validationSeedText: String =
    (
      List(
        Some(claim.trim),
        Option.when(support.nonEmpty)(support.mkString(" ").trim),
        tension.map(_.trim).filter(_.nonEmpty),
        evidenceHook.map(_.trim).filter(_.nonEmpty),
        coda.map(_.trim).filter(_.nonEmpty)
      ).flatten ++ factGuardrails.map(_.trim).filter(_.nonEmpty)
    ).distinct.mkString("\n\n")
  def withFactGuardrails(lines: List[String]): BookmakerPolishSlots =
    copy(factGuardrails = lines.map(_.trim).filter(_.nonEmpty))

object BookmakerSlotSanitizer:
  def sanitizeUserText(raw: String): String =
    UserFacingSignalSanitizer.sanitize(raw)

  def placeholderHits(raw: String): List[String] =
    UserFacingSignalSanitizer.placeholderHits(raw)

object BookmakerPolishSlotsBuilder:

  def build(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack] = None
  ): Option[BookmakerPolishSlots] =
    BookmakerLiveCompressionPolicy.buildSlots(ctx, outline, refs, strategyPack)

  def buildOrFallback(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack] = None
  ): BookmakerPolishSlots =
    BookmakerLiveCompressionPolicy.buildSlotsOrFallback(ctx, outline, refs, strategyPack)

object BookmakerProseContract:

  private val claimStopWords = Set(
    "this", "that", "with", "from", "into", "where", "because", "belongs", "calls",
    "move", "plan", "long", "term", "structure", "keeps", "stays", "there", "their",
    "have", "will", "after", "before", "than", "then", "here", "when", "line",
    "branch", "side", "piece", "pieces", "route", "routes", "through", "around"
  )

  final case class Evaluation(
      paragraphs: List[String],
      sentenceCount: Int,
      claimLikeFirstParagraph: Boolean,
      paragraphBudgetOk: Boolean,
      placeholderHits: List[String],
      genericHits: List[String]
  ):
    def needsRepair: Boolean =
      !claimLikeFirstParagraph || !paragraphBudgetOk || placeholderHits.nonEmpty || genericHits.nonEmpty

  private val genericPhrases = List(
    "keeps pressure",
    "holds the edge",
    "remains preferable",
    "good practical choice"
  )

  def evaluate(text: String, slots: BookmakerPolishSlots): Evaluation =
    val paragraphs = splitParagraphs(text)
    val first = paragraphs.headOption.getOrElse("")
    val claimLike = claimLikeFirstParagraph(first, slots.claim)
    val placeholderHits = BookmakerSlotSanitizer.placeholderHits(text)
    val genericHits =
      (genericPhrases.filter(text.toLowerCase.contains) ++
        BookmakerLiveCompressionPolicy.systemLanguageHits(text)).distinct
    val sentenceCount = countSentences(paragraphs)
    val paragraphBudgetOk =
      if expectsSingleParagraph(slots) then
        paragraphs.size == 1 && sentenceCount >= 1 && sentenceCount <= 2
      else
        paragraphs.size >= 2 && paragraphs.size <= 3 && sentenceCount >= 2 && sentenceCount <= 4
    Evaluation(
      paragraphs = paragraphs,
      sentenceCount = sentenceCount,
      claimLikeFirstParagraph = claimLike,
      paragraphBudgetOk = paragraphBudgetOk,
      placeholderHits = placeholderHits,
      genericHits = genericHits
    )

  def splitParagraphs(text: String): List[String] =
    Option(text).getOrElse("")
      .split("""\n\s*\n""")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList

  def claimLikeFirstParagraph(paragraph: String, claim: String): Boolean =
    val stripped = stripMoveHeader(paragraph)
    val claimCore = stripMoveHeader(claim)
    stripped.startsWith(claimCore.stripSuffix(".")) ||
    commonPrefixWords(stripped, claimCore) >= 4 ||
    significantClaimTokenOverlap(stripped, claimCore) >= 3

  def stripMoveHeader(paragraph: String): String =
    Option(paragraph).getOrElse("").replaceFirst("""^\d+\.(?:\.\.)?\s+[^:]+:\s*""", "").trim

  private def commonPrefixWords(a: String, b: String): Int =
    val as = normalizeWords(a)
    val bs = normalizeWords(b)
    as.zip(bs).takeWhile { case (x, y) => x == y }.size

  private def normalizeWords(text: String): List[String] =
    Option(text).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").split("\\s+").toList.filter(_.nonEmpty)

  def significantClaimTokenOverlap(a: String, b: String): Int =
    val as = normalizeWords(a).filter(token => token.length > 2 && !claimStopWords.contains(token)).toSet
    val bs = normalizeWords(b).filter(token => token.length > 2 && !claimStopWords.contains(token)).toSet
    (as intersect bs).size

  private def expectsSingleParagraph(slots: BookmakerPolishSlots): Boolean =
    slots.paragraphPlan == List("p1=claim")

  private def countSentences(paragraphs: List[String]): Int =
    paragraphs.flatMap(splitSentences).size

  private def splitSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

object BookmakerSoftRepair:

  private val CosmeticActions = Set("claim_restore")

  final case class RepairResult(
      text: String,
      applied: Boolean,
      actions: List[String],
      materialApplied: Boolean,
      materialActions: List[String],
      evaluation: BookmakerProseContract.Evaluation
  )

  def repair(text: String, slots: BookmakerPolishSlots): RepairResult =
    val deterministic = deterministicParagraphs(slots)
    val initial = normalizeParagraphs(text)
    val actions = scala.collection.mutable.ListBuffer.empty[String]
    val singleParagraphMode = slots.paragraphPlan == List("p1=claim")
    val paragraphTarget =
      if singleParagraphMode then 1
      else deterministic.size.max(2).min(3)
    val sentenceTarget = if singleParagraphMode then 2 else 4
    var paragraphs = if initial.nonEmpty then initial else deterministic
    if initial.isEmpty then actions += "empty_to_deterministic"

    val scrubbed = paragraphs.map(BookmakerSlotSanitizer.sanitizeUserText)
    if scrubbed != paragraphs then actions += "placeholder_scrub"
    paragraphs = scrubbed

    if paragraphs.size > paragraphTarget then
      paragraphs =
        if paragraphTarget == 1 then List(paragraphs.mkString(" ").trim)
        else
          val keep = paragraphs.take(paragraphTarget - 1)
          val mergedTail = paragraphs.drop(paragraphTarget - 1).mkString(" ").trim
          keep :+ mergedTail
      actions += "paragraph_merge"
    if paragraphs.size < paragraphTarget then
      paragraphs = (paragraphs ++ deterministic.drop(paragraphs.size)).take(paragraphTarget)
      actions += "paragraph_fill"

    paragraphs = trimToSentenceBudget(paragraphs, sentenceTarget, paragraphTarget)

    val currentEval = BookmakerProseContract.evaluate(paragraphs.mkString("\n\n"), slots)
    if !currentEval.claimLikeFirstParagraph || currentEval.genericHits.nonEmpty then
      paragraphs = deterministic.headOption.toList ++ paragraphs.drop(1)
      actions += "claim_restore"

    if !singleParagraphMode && paragraphs.size >= 2 && paragraphs(1).trim.isEmpty then
      paragraphs = paragraphs.updated(1, deterministic.lift(1).getOrElse(paragraphs(1)))
      actions += "support_restore"

    if !singleParagraphMode && (slots.tension.nonEmpty || slots.evidenceHook.nonEmpty) then
      val p3 = deterministic.lift(2)
      if p3.nonEmpty then
        val currentText = paragraphs.mkString(" ")
        val alreadyCovered = BookmakerProseContract.significantClaimTokenOverlap(currentText, p3.get) >= 3
        if paragraphs.size < 3 then
          if !alreadyCovered then
            paragraphs = paragraphs :+ p3.get
            actions += "evidence_append"
        else if BookmakerSlotSanitizer.placeholderHits(paragraphs(2)).nonEmpty then
          paragraphs = paragraphs.updated(2, p3.get)
          actions += "evidence_restore"
        else if !alreadyCovered && BookmakerProseContract.significantClaimTokenOverlap(paragraphs.lift(2).getOrElse(""), p3.get) < 2 then
          // If paragraph 3 exists but doesn't cover the evidence and the whole text doesn't either, we might need a restore,
          // but for now, we prioritize not duplicating if the LLM integrated it elsewhere.
          ()

    paragraphs = trimToSentenceBudget(paragraphs, sentenceTarget, paragraphTarget)

    if slots.coda.isEmpty && paragraphs.size > paragraphTarget then
      paragraphs = paragraphs.take(paragraphTarget)
      actions += "drop_extra_coda"

    val repairedText = paragraphs.map(_.trim).filter(_.nonEmpty).mkString("\n\n")
    val evaluation = BookmakerProseContract.evaluate(repairedText, slots)
    val distinctActions = actions.toList.distinct
    val materialActions = distinctActions.filterNot(CosmeticActions.contains)
    RepairResult(
      text = repairedText,
      applied = distinctActions.nonEmpty,
      actions = distinctActions,
      materialApplied = materialActions.nonEmpty,
      materialActions = materialActions,
      evaluation = evaluation
    )

  def deterministicParagraphs(slots: BookmakerPolishSlots): List[String] =
    if slots.paragraphPlan == List("p1=claim") then List(slots.claim.trim)
    else
      val supportParagraph = slots.support.mkString(" ").trim
      val thirdParagraph = composeThirdParagraph(slots).trim
      val thirdOrCoda =
        if thirdParagraph.nonEmpty && slots.coda.exists(_.trim.nonEmpty) then
          s"${ensureSentence(thirdParagraph)} ${ensureSentence(slots.coda.get)}".trim
        else if thirdParagraph.nonEmpty then thirdParagraph
        else slots.coda.map(_.trim).filter(_.nonEmpty).getOrElse("")
      List(
        Some(slots.claim.trim),
        Option.when(supportParagraph.nonEmpty)(supportParagraph),
        Option.when(thirdOrCoda.nonEmpty)(thirdOrCoda)
      ).flatten

  private def normalizeParagraphs(text: String): List[String] =
    BookmakerProseContract.splitParagraphs(text).map(_.trim).filter(_.nonEmpty)

  private def composeThirdParagraph(slots: BookmakerPolishSlots): String =
    (slots.tension.map(_.trim).filter(_.nonEmpty), slots.evidenceHook.map(_.trim).filter(_.nonEmpty)) match
      case (Some(tension), Some(evidence)) =>
        s"${ensureSentence(tension)} ${normalizeEvidenceHook(evidence)}".trim
      case (Some(tension), None) => tension
      case (None, Some(evidence)) => normalizeEvidenceHook(evidence)
      case (None, None) => ""

  private def normalizeEvidenceHook(text: String): String =
    val trimmed = text.trim
    if trimmed.toLowerCase.startsWith("one concrete line that keeps the idea in play is") then trimmed
    else if trimmed.toLowerCase.startsWith("a concrete line is") then
      trimmed.replaceFirst("(?i)^a concrete line is", "One concrete line that keeps the idea in play is")
    else if trimmed.matches("""^[a-z]\)\s+.*""") then s"One concrete line that keeps the idea in play is $trimmed"
    else trimmed

  private def ensureSentence(text: String): String =
    val trimmed = text.trim
    if trimmed.isEmpty || trimmed.matches(""".*[.!?]$""") then trimmed
    else s"$trimmed."

  private def splitSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def trimToSentenceBudget(
      paragraphs: List[String],
      sentenceTarget: Int,
      paragraphTarget: Int
  ): List[String] =
    val limited = scala.collection.mutable.ListBuffer.empty[String]
    var remaining = sentenceTarget
    paragraphs.take(paragraphTarget).foreach { paragraph =>
      if remaining > 0 then
        val keep = splitSentences(paragraph).take(remaining)
        if keep.nonEmpty then
          limited += keep.mkString(" ").trim
          remaining -= keep.size
    }
    limited.toList
