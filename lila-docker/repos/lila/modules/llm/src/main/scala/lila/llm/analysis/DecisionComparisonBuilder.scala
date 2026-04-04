package lila.llm.analysis

import lila.llm.DecisionComparisonDigest
import lila.llm.model.NarrativeContext
import lila.llm.model.strategic.VariationLine

private[analysis] final case class DecisionComparison(
    chosenMove: Option[String],
    engineBestMove: Option[String],
    engineBestScoreCp: Option[Int],
    engineBestPv: List[String],
    cpLossVsChosen: Option[Int],
    deferredMove: Option[String],
    deferredReason: Option[String],
    deferredSource: Option[String],
    evidence: Option[String],
    practicalAlternative: Boolean,
    chosenMatchesBest: Boolean,
    comparedMove: Option[String] = None,
    comparativeConsequence: Option[String] = None,
    comparativeSource: Option[String] = None
):
  def toDigest: DecisionComparisonDigest =
    DecisionComparisonDigest(
      chosenMove = chosenMove,
      engineBestMove = engineBestMove,
      engineBestScoreCp = engineBestScoreCp,
      engineBestPv = engineBestPv,
      comparedMove = comparedMove,
      comparativeConsequence = comparativeConsequence,
      comparativeSource = comparativeSource,
      cpLossVsChosen = cpLossVsChosen,
      deferredMove = deferredMove,
      deferredReason = deferredReason,
      deferredSource = deferredSource,
      evidence = evidence,
      practicalAlternative = practicalAlternative,
      chosenMatchesBest = chosenMatchesBest
    )

private[analysis] object DecisionComparisonBuilder:

  def build(ctx: NarrativeContext): Option[DecisionComparison] =
    val chosen = chosenMove(ctx)
    val bestLine = ctx.engineEvidence.flatMap(_.best)
    val bestMove = bestLine.flatMap(leadSan(ctx.fen, _))
    val bestScore = bestLine.map(_.effectiveScore)
    val bestPv = bestLine.flatMap(linePreview(ctx.fen, _)).getOrElse(Nil)
    val cpLoss = ctx.counterfactual.map(_.cpLoss).filter(_ > 0)
    val alternative = AlternativeNarrativeSupport.build(ctx)
    val fallbackDeferred = bestMove.filterNot(best => chosen.exists(equalMoveToken(best, _)))
    val deferredMove = alternative.flatMap(_.move).orElse(fallbackDeferred)
    val deferredReason =
      alternative.map(_.reason).map(UserFacingSignalSanitizer.sanitize)
        .orElse {
          fallbackDeferred.flatMap(_ =>
            cpLoss.map(loss => s"it trails the engine line by about ${loss}cp")
          )
        }
    val deferredSource =
      alternative.map(_.source).orElse(Option.when(fallbackDeferred.nonEmpty)("engine_gap"))
    val evidence = NarrativeEvidenceHooks.build(ctx)
    val chosenMatchesBest =
      (chosen, bestMove) match
        case (Some(c), Some(b)) => equalMoveToken(c, b)
        case (None, None)       => true
        case _                  => false

    Option.when(
      chosen.nonEmpty || bestMove.nonEmpty || deferredMove.nonEmpty || deferredReason.nonEmpty || evidence.nonEmpty
    )(
      DecisionComparison(
        chosenMove = chosen,
        engineBestMove = bestMove,
        engineBestScoreCp = bestScore,
        engineBestPv = bestPv,
        cpLossVsChosen = cpLoss,
        deferredMove = deferredMove,
        deferredReason = deferredReason,
        deferredSource = deferredSource,
        evidence = evidence,
        practicalAlternative = alternative.exists(_.source == "close_candidate"),
        chosenMatchesBest = chosenMatchesBest
      )
    )

  def digest(ctx: NarrativeContext): Option[DecisionComparisonDigest] =
    build(ctx).map(_.toDigest)

  private def chosenMove(ctx: NarrativeContext): Option[String] =
    ctx.playedSan.map(normalize).filter(_.nonEmpty)
      .orElse {
        ctx.playedMove
          .map(NarrativeUtils.formatUciAsSan)
          .map(normalize)
          .filter(_.nonEmpty)
      }

  private def leadSan(fen: String, line: VariationLine): Option[String] =
    line.ourMove.map(_.san).map(normalize).filter(_.nonEmpty)
      .orElse {
        line.moves.headOption.map(m => NarrativeUtils.uciToSanOrFormat(fen, m)).map(normalize).filter(_.nonEmpty)
      }

  private def linePreview(fen: String, line: VariationLine): Option[List[String]] =
    val tokens =
      if line.parsedMoves.nonEmpty then line.parsedMoves.take(4).map(_.san.trim).filter(_.nonEmpty)
      else NarrativeUtils.uciListToSan(fen, line.moves.take(4)).map(_.trim).filter(_.nonEmpty)
    Option.when(tokens.nonEmpty)(tokens)

  private def equalMoveToken(a: String, b: String): Boolean =
    normalizeMoveToken(a) == normalizeMoveToken(b)

  private def normalizeMoveToken(raw: String): String =
    normalize(raw).replaceAll("""[+#?!]+$""", "").toLowerCase

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim
