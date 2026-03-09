package lila.llm.analysis

import lila.llm.{ BookmakerRefsV1, VariationRefV1 }
import lila.llm.model.*
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeatKind }

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
      refs: Option[BookmakerRefsV1]
  ): Option[BookmakerPolishSlots] =
    val thesis = StrategicThesisBuilder.build(ctx)
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).map(_.text).filter(_.nonEmpty)
    val mainMoveBeat = outline.beats.find(_.kind == OutlineBeatKind.MainMove).map(_.text).filter(_.nonEmpty)
    val decisionBeat = outline.beats.find(_.kind == OutlineBeatKind.DecisionPoint).map(_.text).filter(_.nonEmpty)
    val conditionalBeat = outline.beats.find(_.kind == OutlineBeatKind.ConditionalPlan).map(_.text).filter(_.nonEmpty)
    val wrapBeat = outline.beats.find(_.kind == OutlineBeatKind.WrapUp).map(_.text).filter(_.nonEmpty)
    val mainMoveSentences =
      splitSentences(mainMoveBeat.getOrElse(""))
        .map(BookmakerSlotSanitizer.sanitizeUserText)
        .filter(_.nonEmpty)
    val annotationOverride = annotationClaimOverride(ctx, mainMoveSentences)
    val claimCore =
      annotationOverride.map(_._1)
        .orElse(thesis.map(_.claim))
        .orElse(contextBeat)
        .map(BookmakerSlotSanitizer.sanitizeUserText)
        .filter(_.nonEmpty)
    claimCore.map { baseClaim =>
      val claim = prefixMoveHeader(ctx, baseClaim)
      val supports =
        annotationOverride.map(_._2)
          .getOrElse(thesis.map(_.support).filter(_.nonEmpty).getOrElse(mainMoveSentences))
          .map(BookmakerSlotSanitizer.sanitizeUserText)
          .filter(_.nonEmpty)
          .distinct
          .take(2)
      val tension =
        thesis.flatMap(_.tension)
          .orElse(decisionBeat)
          .orElse(conditionalBeat)
          .map(BookmakerSlotSanitizer.sanitizeUserText)
          .filter(_.nonEmpty)
      val evidence =
        variationGuardrail(refs)
          .orElse(thesis.flatMap(_.evidenceHook).map(BookmakerSlotSanitizer.sanitizeUserText).filter(_.nonEmpty))
      val coda =
        wrapBeat
          .map(BookmakerSlotSanitizer.sanitizeUserText)
          .filter(_.nonEmpty)
      val guardrails = List(evidence).flatten
      val paragraphPlan = List(
        Some("p1=claim"),
        Option.when(supports.nonEmpty)("p2=support_chain"),
        Option.when(tension.nonEmpty || evidence.nonEmpty)("p3=tension_or_evidence"),
        Option.when(coda.nonEmpty)("p4=coda")
      ).flatten
      BookmakerPolishSlots(
        lens = thesis.map(_.lens).getOrElse(StrategicLens.Decision),
        claim = claim,
        supportPrimary = supports.headOption,
        supportSecondary = supports.lift(1),
        tension = tension,
        evidenceHook = evidence,
        coda = coda,
        factGuardrails = guardrails,
        paragraphPlan = paragraphPlan
      )
    }

  private def prefixMoveHeader(ctx: NarrativeContext, claim: String): String =
    if Option(claim).exists(_.matches("""^\d+\.(?:\.\.)?\s+[^:]+:\s*.*""")) then claim
    else
      val moveHeader =
        for
          san <- ctx.playedSan.filter(_.trim.nonEmpty)
        yield
          val moveNum = (ctx.ply + 1) / 2
          val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
          s"$prefix $san:"
      moveHeader.map(h => s"$h $claim").getOrElse(claim)

  private def annotationClaimOverride(
      ctx: NarrativeContext,
      mainMoveSentences: List[String]
  ): Option[(String, List[String])] =
    Option.when(CriticalAnnotationPolicy.shouldPrioritizeClaim(ctx) && mainMoveSentences.nonEmpty) {
      val claim = mainMoveSentences.head
      val support = mainMoveSentences.drop(1)
      (claim, support)
    }

  private def splitSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def variationGuardrail(refs: Option[BookmakerRefsV1]): Option[String] =
    refs.flatMap(_.variations.headOption).flatMap(renderVariationGuardrail)

  private def renderVariationGuardrail(variation: VariationRefV1): Option[String] =
    val preview =
      variation.moves
        .take(3)
        .map(_.san.trim)
        .filter(_.nonEmpty)
        .mkString(" ")
        .trim
    Option.when(preview.nonEmpty) {
      val eval = formatVariationScore(variation.scoreCp, variation.mate)
      s"a) $preview$eval"
    }

  private def formatVariationScore(scoreCp: Int, mate: Option[Int]): String =
    mate match
      case Some(m) if m > 0 => s" (mate in $m)"
      case Some(m) if m < 0 => s" (mated in ${Math.abs(m)})"
      case Some(_)          => ""
      case None =>
        val sign = if scoreCp >= 0 then "+" else ""
        f" ($sign${scoreCp.toDouble / 100}%.1f)"

object BookmakerProseContract:

  private val claimStopWords = Set(
    "this", "that", "with", "from", "into", "where", "because", "belongs", "calls",
    "move", "plan", "long", "term", "structure", "keeps", "stays", "there", "their",
    "have", "will", "after", "before", "than", "then", "here", "when", "line",
    "branch", "side", "piece", "pieces", "route", "routes", "through", "around"
  )

  final case class Evaluation(
      paragraphs: List[String],
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
    val genericHits = genericPhrases.filter(text.toLowerCase.contains)
    Evaluation(
      paragraphs = paragraphs,
      claimLikeFirstParagraph = claimLike,
      paragraphBudgetOk = paragraphs.size >= 2 && paragraphs.size <= 4,
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

  private def significantClaimTokenOverlap(a: String, b: String): Int =
    val as = normalizeWords(a).filter(token => token.length > 2 && !claimStopWords.contains(token)).toSet
    val bs = normalizeWords(b).filter(token => token.length > 2 && !claimStopWords.contains(token)).toSet
    (as intersect bs).size

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
    val paragraphTarget = deterministic.size.max(2).min(4)
    var paragraphs = if initial.nonEmpty then initial else deterministic
    if initial.isEmpty then actions += "empty_to_deterministic"

    val scrubbed = paragraphs.map(BookmakerSlotSanitizer.sanitizeUserText)
    if scrubbed != paragraphs then actions += "placeholder_scrub"
    paragraphs = scrubbed

    if paragraphs.size > paragraphTarget then
      val keep = paragraphs.take(paragraphTarget - 1)
      val mergedTail = paragraphs.drop(paragraphTarget - 1).mkString(" ").trim
      paragraphs = keep :+ mergedTail
      actions += "paragraph_merge"
    if paragraphs.size < paragraphTarget then
      paragraphs = (paragraphs ++ deterministic.drop(paragraphs.size)).take(paragraphTarget)
      actions += "paragraph_fill"

    val currentEval = BookmakerProseContract.evaluate(paragraphs.mkString("\n\n"), slots)
    if !currentEval.claimLikeFirstParagraph || currentEval.genericHits.nonEmpty then
      paragraphs = deterministic.headOption.toList ++ paragraphs.drop(1)
      actions += "claim_restore"

    if paragraphs.size >= 2 && paragraphs(1).trim.isEmpty then
      paragraphs = paragraphs.updated(1, deterministic.lift(1).getOrElse(paragraphs(1)))
      actions += "support_restore"

    if slots.tension.nonEmpty || slots.evidenceHook.nonEmpty then
      val p3 = deterministic.lift(2)
      if p3.nonEmpty then
        if paragraphs.size < 3 then
          paragraphs = paragraphs :+ p3.get
          actions += "evidence_append"
        else if BookmakerSlotSanitizer.placeholderHits(paragraphs(2)).nonEmpty then
          paragraphs = paragraphs.updated(2, p3.get)
          actions += "evidence_restore"

    if slots.coda.isEmpty && paragraphs.size == 4 && deterministic.size < 4 then
      paragraphs = paragraphs.take(3)
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
    val supportParagraph = slots.support.mkString(" ").trim
    val thirdParagraph = composeThirdParagraph(slots).trim
    List(
      Some(slots.claim.trim),
      Option.when(supportParagraph.nonEmpty)(supportParagraph),
      Option.when(thirdParagraph.nonEmpty)(thirdParagraph),
      slots.coda.map(_.trim).filter(_.nonEmpty)
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
    if trimmed.matches("""^[a-z]\)\s+.*""") then s"A concrete line is $trimmed"
    else trimmed

  private def ensureSentence(text: String): String =
    val trimmed = text.trim
    if trimmed.isEmpty || trimmed.matches(""".*[.!?]$""") then trimmed
    else s"$trimmed."
