package lila.commentary.analysis

import lila.commentary.{ DecisionComparisonDigest, MoveReviewRefs }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

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

  def build(ctx: NarrativeContext, refs: Option[MoveReviewRefs] = None): Option[DecisionComparison] =
    val chosen = chosenMove(ctx)
    val bestLine = ctx.engineEvidence.flatMap(_.best)
    val bestMove = bestLine.flatMap(leadSan(ctx.fen, _))
    val bestScore = bestLine.map(_.effectiveScore)
    val bestPv = bestPvPreview(ctx, refs, bestLine)
    val cpLoss = ctx.counterfactual.map(_.cpLoss).filter(_ > 0)
    val alternative = AlternativeNarrativeSupport.build(ctx)
    val fallbackDeferred = bestMove.filterNot(best => chosen.exists(equalMoveToken(best, _)))
    val deferredMove = alternative.flatMap(_.move).orElse(fallbackDeferred)
    val deferredReason = deferredMoveReason(alternative, fallbackDeferred, cpLoss)
    val deferredSource =
      alternative.map(_.source).orElse(Option.when(fallbackDeferred.nonEmpty)("engine_gap"))
    val evidence = NarrativeEvidenceHooks.build(ctx, refs)
    val chosenMatchesBest = movesMatchOrBothMissing(chosen, bestMove)

    Option.when(hasComparisonMaterial(chosen, bestMove, deferredMove, deferredReason, evidence))(
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

  def digest(ctx: NarrativeContext, refs: Option[MoveReviewRefs] = None): Option[DecisionComparisonDigest] =
    build(ctx, refs).map(_.toDigest)

  private def bestPvPreview(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      bestLine: Option[VariationLine]
  ): List[String] =
    LineConsequenceEvaluator
      .narrativeCandidate(ctx, refs)
      .map(_.sanMoves.take(4))
      .filter(_.nonEmpty)
      .orElse(bestLine.flatMap(linePreview(ctx.fen, _)))
      .getOrElse(Nil)

  private def deferredMoveReason(
      alternative: Option[AlternativeNarrative],
      fallbackDeferred: Option[String],
      cpLoss: Option[Int]
  ): Option[String] =
    alternative.map(_.reason).map(UserFacingSignalSanitizer.sanitize)
      .orElse {
        fallbackDeferred.flatMap(_ =>
          cpLoss.map(loss => s"it trails the engine line by about ${loss}cp")
        )
      }

  private def movesMatchOrBothMissing(chosen: Option[String], bestMove: Option[String]): Boolean =
    (chosen, bestMove) match
      case (Some(c), Some(b)) => equalMoveToken(c, b)
      case (None, None)       => true
      case _                  => false

  private def hasComparisonMaterial(
      chosen: Option[String],
      bestMove: Option[String],
      deferredMove: Option[String],
      deferredReason: Option[String],
      evidence: Option[String]
  ): Boolean =
    chosen.nonEmpty || bestMove.nonEmpty || deferredMove.nonEmpty || deferredReason.nonEmpty || evidence.nonEmpty

  private[analysis] def cleanMoveText(raw: Option[String]): Option[String] =
    raw.flatMap(cleanMoveText)

  private[analysis] def cleanMoveText(raw: String): Option[String] =
    Option(raw).map(normalize).filter(_.nonEmpty)

  private[analysis] def equalMoveToken(a: String, b: String): Boolean =
    normalizeMoveToken(a) == normalizeMoveToken(b)

  private[analysis] def normalizeMoveToken(raw: String): String =
    normalize(raw).replaceAll("""[+#?!]+$""", "").toLowerCase

  private def chosenMove(ctx: NarrativeContext): Option[String] =
    ctx.playedSan.flatMap(cleanMoveText)
      .orElse {
        ctx.playedMove
          .map(NarrativeUtils.formatUciAsSan)
          .flatMap(cleanMoveText)
      }

  private def leadSan(fen: String, line: VariationLine): Option[String] =
    LineScopedCitation.sanMoves(fen, line).headOption.flatMap(cleanMoveText)

  private def linePreview(fen: String, line: VariationLine): Option[List[String]] =
    val tokens = LineScopedCitation.sanMoves(fen, line).take(4).map(_.trim).filter(_.nonEmpty)
    Option.when(tokens.nonEmpty)(tokens)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim
