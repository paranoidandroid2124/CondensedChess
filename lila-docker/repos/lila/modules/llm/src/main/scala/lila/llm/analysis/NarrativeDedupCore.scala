package lila.llm.analysis

import scala.util.matching.Regex

private[llm] object NarrativeDedupCore:

  enum NarrativeClaimFamily:
    case PlanLead
    case RouteAnchor
    case PressureAnchor
    case ImprovementAnchor
    case SupportCondition
    case PracticalVerdict
    case WholeGameShift
    case WholeGamePayoff
    case Other

  final case class NarrativeFingerprint(
      full: String,
      anchor: String
  )

  final case class NarrativeSentenceCandidate(
      surface: String,
      role: String,
      family: NarrativeClaimFamily,
      text: String,
      fingerprint: NarrativeFingerprint,
      concretenessScore: Int,
      priority: Int,
      order: Int
  )

  private val SentenceSplit = """(?<=[.!?])\s+""".r
  private val SquareRe = """\b[a-h][1-8]\b""".r
  private val FileRe = """\b[a-h]-file\b""".r
  private val TheaterRe = """\b(?:kingside|queenside|center|central)\b""".r
  private val PieceRe = """\b(?:king|queen|rook|bishop|knight|pawn)s?\b""".r
  private val MoveRe =
    """(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r
  private val ConcretePatterns: List[Regex] = List(SquareRe, FileRe, TheaterRe, PieceRe, MoveRe)

  private val WrapperLeadPatterns: List[Regex] = List(
    """(?i)^key theme:\s*""".r,
    """(?i)^strategic focus:\s*""".r,
    """(?i)^strategic priority:\s*""".r,
    """(?i)^strategic focus centers on\s+""".r,
    """(?i)^strategic focus remains on\s+""".r,
    """(?i)^strategic focus is sharpening along\s+""".r,
    """(?i)^current play is organized around\s+""".r,
    """(?i)^the practical roadmap centers on\s+""".r,
    """(?i)^the most reliable roadmap here is built around\s+""".r,
    """(?i)^the main plan remains\s+""".r,
    """(?i)^the strategic stack still points first to\s+""".r,
    """(?i)^the strategic stack still favors\s+""".r,
    """(?i)^the backup strategic stack is\s+""".r,
    """(?i)^the turning point came through\s+""".r,
    """(?i)^the decisive shift came through\s+""".r,
    """(?i)^the long strategic pressure finally became concrete through\s+""".r,
    """(?i)^the punishment story ran through\s+""".r,
    """(?i)^the winning route was\s+""".r,
    """(?i)^the conversion route ran through\s+""".r,
    """(?i)^white was mainly playing for\s+""".r,
    """(?i)^black was mainly playing for\s+""".r,
    """(?i)^white's clearest long-term plan was\s+""".r,
    """(?i)^black's clearest long-term plan was\s+""".r,
    """(?i)^the long strategic fight revolved around\s+""".r,
    """(?i)^the key idea is\s+""".r,
    """(?i)^the move is really about\s+""".r,
    """(?i)^a likely follow-up is\s+""".r,
    """(?i)^the next useful route is\s+""".r,
    """(?i)^the next target is\s+""".r,
    """(?i)^a concrete target is\s+""".r,
    """(?i)^the next step is to\s+""".r,
    """(?i)^this works best when\s+""".r
  )

  private val StopWords = Set(
    "the",
    "a",
    "an",
    "and",
    "or",
    "but",
    "with",
    "without",
    "through",
    "came",
    "ran",
    "was",
    "were",
    "is",
    "are",
    "to",
    "of",
    "for",
    "in",
    "on",
    "at",
    "this",
    "that",
    "these",
    "those",
    "white",
    "black",
    "move",
    "game",
    "idea",
    "plan",
    "next",
    "step",
    "mainly",
    "playing",
    "clearest",
    "long",
    "term",
    "route",
    "story"
  )

  def splitSentences(text: String): List[String] =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(SentenceSplit.split(_).toList)
      .getOrElse(Nil)
      .map(_.trim)
      .filter(_.nonEmpty)

  def normalizeFingerprint(text: String): String =
    Option(text)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  def buildCandidate(
      surface: String,
      role: String,
      text: String,
      priority: Int,
      order: Int,
      familyOverride: Option[NarrativeClaimFamily] = None
  ): NarrativeSentenceCandidate =
    val cleaned = Option(text).map(_.trim).getOrElse("")
    val family = familyOverride.getOrElse(inferFamily(role, cleaned))
    val fp = fingerprint(cleaned)
    NarrativeSentenceCandidate(
      surface = surface,
      role = role,
      family = family,
      text = cleaned,
      fingerprint = fp,
      concretenessScore = scoreConcreteness(cleaned),
      priority = priority,
      order = order
    )

  def dedupe(candidates: List[NarrativeSentenceCandidate]): List[NarrativeSentenceCandidate] =
    if candidates.isEmpty then Nil
    else
      val preferredByGroup =
        candidates
          .flatMap(candidate => groupKeys(candidate).map(_ -> candidate))
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2).minBy(candidateRank))
          .toMap

      candidates
        .filter(candidate =>
          groupKeys(candidate).forall(group =>
            preferredByGroup.get(group).exists(_.order == candidate.order)
          )
        )
        .sortBy(_.order)

  def dedupeStandaloneProse(
      prose: String,
      surface: String,
      role: String,
      priorityBase: Int = 60,
      familyOverride: Option[NarrativeClaimFamily] = None
  ): String =
    val candidates =
      splitSentences(prose).zipWithIndex.map { case (sentence, idx) =>
        buildCandidate(
          surface = surface,
          role = role,
          text = sentence,
          priority = priorityBase,
          order = idx,
          familyOverride = familyOverride
        )
      }
    val kept = dedupe(candidates).map(_.text).mkString(" ").trim
    if kept.nonEmpty then kept else Option(prose).getOrElse("").trim

  def proseAddsDistinctClaim(candidateProse: String, baseProse: String): Boolean =
    val baseSentences = splitSentences(baseProse)
    splitSentences(candidateProse).exists { candidateSentence =>
      !baseSentences.exists(baseSentence => sameSemanticSentence(candidateSentence, baseSentence))
    }

  def sameSemanticSentence(a: String, b: String): Boolean =
    val fa = fingerprint(a)
    val fb = fingerprint(b)
    (fa.full.nonEmpty && fa.full == fb.full) ||
    (fa.anchor.nonEmpty && fa.anchor == fb.anchor) ||
    semanticOverlap(fa.anchor, fb.anchor) >= 0.8 ||
    semanticOverlap(fa.full, fb.full) >= 0.88

  private def inferFamily(role: String, text: String): NarrativeClaimFamily =
    val low = normalizeFingerprint(text)
    role match
      case "shift"  => NarrativeClaimFamily.WholeGameShift
      case "payoff" => NarrativeClaimFamily.WholeGamePayoff
      case "wrap_up" | "practical" =>
        NarrativeClaimFamily.PracticalVerdict
      case _ if low.startsWith("this works best when") || low.contains("depends on") || low.contains("only while") =>
        NarrativeClaimFamily.SupportCondition
      case _ if low.contains("pressure on") || low.contains("pressure against") || low.contains("file control") || low.contains("targets") =>
        NarrativeClaimFamily.PressureAnchor
      case _ if low.contains("route") || low.contains("follow up") || low.contains("follow-up") || low.contains("head for") || low.contains("bring the") =>
        NarrativeClaimFamily.RouteAnchor
      case _ if low.contains("piece placement") || low.contains("improve") || low.contains("development") || low.contains("coordination") =>
        NarrativeClaimFamily.ImprovementAnchor
      case "main_move" | "plan_lead" =>
        NarrativeClaimFamily.PlanLead
      case _ if low.startsWith("the key idea is") ||
          low.startsWith("white was mainly playing for") ||
          low.startsWith("black was mainly playing for") ||
          low.startsWith("the long strategic fight revolved around") =>
        NarrativeClaimFamily.PlanLead
      case _ =>
        NarrativeClaimFamily.Other

  private def fingerprint(text: String): NarrativeFingerprint =
    val normalized = normalizeFingerprint(text)
    NarrativeFingerprint(
      full = normalized,
      anchor = normalizeFingerprint(stripWrapperLead(text))
    )

  private def stripWrapperLead(text: String): String =
    WrapperLeadPatterns.foldLeft(Option(text).getOrElse("").trim) { (acc, pattern) =>
      pattern.replaceFirstIn(acc, "")
    }

  private def scoreConcreteness(text: String): Int =
    val source = Option(text).getOrElse("")
    val concreteHits = ConcretePatterns.map(_.findAllIn(source).length).sum
    val tokenCount = normalizeFingerprint(source).split(" ").count(_.nonEmpty)
    concreteHits * 10 + math.min(tokenCount, 12)

  private def candidateRank(candidate: NarrativeSentenceCandidate): (Int, Int, Int) =
    (-candidate.concretenessScore, -candidate.priority, candidate.order)

  private def groupKeys(candidate: NarrativeSentenceCandidate): List[String] =
    val semanticFamily =
      candidate.family match
        case NarrativeClaimFamily.WholeGameShift | NarrativeClaimFamily.WholeGamePayoff
            if candidate.surface == "whole_game" =>
          "whole_game_resolution"
        case other                                                                      => other.toString

    List(
      Option.when(candidate.fingerprint.full.nonEmpty)(s"full:${candidate.fingerprint.full}"),
      Option.when(candidate.fingerprint.anchor.nonEmpty)(
        s"semantic:${semanticFamily}:${candidate.fingerprint.anchor}"
      )
    ).flatten.distinct

  private def semanticOverlap(a: String, b: String): Double =
    val left = semanticTokens(a)
    val right = semanticTokens(b)
    if left.isEmpty || right.isEmpty then 0.0
    else
      val intersection = left.intersect(right).size.toDouble
      val union = left.union(right).size.toDouble
      if union == 0 then 0.0 else intersection / union

  private def semanticTokens(text: String): Set[String] =
    normalizeFingerprint(text)
      .split(" ")
      .iterator
      .map(_.trim)
      .filter(token => token.length >= 3 && !StopWords.contains(token))
      .toSet
