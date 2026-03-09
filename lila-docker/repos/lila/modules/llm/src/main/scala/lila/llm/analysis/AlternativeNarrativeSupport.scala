package lila.llm.analysis

import lila.llm.model.NarrativeContext
import lila.llm.model.strategic.VariationLine

private[analysis] final case class AlternativeNarrative(
    move: Option[String],
    reason: String,
    sentence: String,
    source: String
)

private[analysis] object AlternativeNarrativeSupport:

  private val CloseAlternativeThresholdCp = 35

  def build(ctx: NarrativeContext): Option[AlternativeNarrative] =
    fromWhyAbsent(ctx).orElse(fromCloseAlternative(ctx))

  def sentence(ctx: NarrativeContext): Option[String] =
    build(ctx).map(_.sentence)

  def moveLabel(ctx: NarrativeContext): Option[String] =
    build(ctx).flatMap(_.move)

  def observedIn(text: String, alt: AlternativeNarrative): Boolean =
    val low = normalize(text).toLowerCase
    val moveHit = alt.move.exists(m => low.contains(normalize(m).toLowerCase))
    val reasonTokens =
      normalize(alt.reason)
        .toLowerCase
        .split("""[^a-z0-9]+""")
        .toList
        .filter(token => token.nonEmpty && token.length > 4)
    moveHit || reasonTokens.count(low.contains) >= math.min(2, reasonTokens.size)

  private def fromWhyAbsent(ctx: NarrativeContext): Option[AlternativeNarrative] =
    ctx.whyAbsentFromTopMultiPV.headOption.flatMap { raw =>
      val reason = normalizeSentenceFragment(raw)
      Option.when(reason.nonEmpty) {
        val move = extractQuotedMove(raw).orElse(bestAlternativeMove(ctx))
        AlternativeNarrative(
          move = move,
          reason = reason,
          sentence = renderSentence(move, reason),
          source = "why_absent"
        )
      }
    }

  private def fromCloseAlternative(ctx: NarrativeContext): Option[AlternativeNarrative] =
    val altLine = ctx.engineEvidence.flatMap(_.alternatives(CloseAlternativeThresholdCp).headOption)
    val altMove = altLine.flatMap(variationLeadSan(ctx.fen, _))
    val candidateReason =
      altMove.flatMap { san =>
        ctx.candidates.drop(1).find(c => normalizeMoveToken(c.move) == normalizeMoveToken(san))
      }.flatMap(_.whyNot)
        .map(normalizeSentenceFragment)
        .filter(_.nonEmpty)
    candidateReason.map { reason =>
      AlternativeNarrative(
        move = altMove,
        reason = reason,
        sentence = renderSentence(altMove, reason),
        source = "close_candidate"
      )
    }

  private def bestAlternativeMove(ctx: NarrativeContext): Option[String] =
    ctx.engineEvidence.flatMap(_.alternatives(CloseAlternativeThresholdCp).headOption).flatMap(variationLeadSan(ctx.fen, _))
      .orElse(ctx.candidates.drop(1).headOption.map(_.move).map(normalize).filter(_.nonEmpty))

  private def renderSentence(move: Option[String], reason: String): String =
    val lead = move.map(m => s"The practical alternative $m").getOrElse("The practical alternative")
    s"$lead stays secondary because $reason."

  private def variationLeadSan(fen: String, line: VariationLine): Option[String] =
    line.ourMove.map(_.san).map(normalize).filter(_.nonEmpty)
      .orElse {
        line.moves.headOption.map(m => NarrativeUtils.uciToSanOrFormat(fen, m)).map(normalize).filter(_.nonEmpty)
      }

  private def extractQuotedMove(raw: String): Option[String] =
    "\"([^\"]+)\"".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty)
      .orElse("'([^']+)'".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).trim).filter(_.nonEmpty))

  private def normalizeSentenceFragment(raw: String): String =
    normalize(raw).stripSuffix(".")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim

  private def normalizeMoveToken(raw: String): String =
    normalize(raw).replaceAll("""[+#?!]+$""", "").toLowerCase
