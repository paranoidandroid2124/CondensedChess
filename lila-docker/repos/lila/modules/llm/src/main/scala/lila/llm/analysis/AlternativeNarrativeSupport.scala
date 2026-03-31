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
    fromCloseAlternative(ctx)

  def sentence(ctx: NarrativeContext): Option[String] =
    build(ctx).map(_.sentence)

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

  private def renderSentence(move: Option[String], reason: String): String =
    val lead = move.map(m => s"The practical alternative $m").getOrElse("The practical alternative")
    s"$lead stays secondary because $reason."

  private def variationLeadSan(fen: String, line: VariationLine): Option[String] =
    line.ourMove.map(_.san).map(normalize).filter(_.nonEmpty)
      .orElse {
        line.moves.headOption.map(m => NarrativeUtils.uciToSanOrFormat(fen, m)).map(normalize).filter(_.nonEmpty)
      }

  private def normalizeSentenceFragment(raw: String): String =
    normalize(raw).stripSuffix(".")

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim

  private def normalizeMoveToken(raw: String): String =
    normalize(raw).replaceAll("""[+#?!]+$""", "").toLowerCase
