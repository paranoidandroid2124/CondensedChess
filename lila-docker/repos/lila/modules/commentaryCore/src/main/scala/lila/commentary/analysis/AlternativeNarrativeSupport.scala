package lila.commentary.analysis

import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

private[analysis] final case class AlternativeNarrative(
    move: Option[String],
    reason: String,
    sentence: String,
    source: String
)

private[analysis] object AlternativeNarrativeSupport:

  private val CloseAlternativeThresholdCp = 120

  def build(ctx: NarrativeContext): Option[AlternativeNarrative] =
    fromCloseAlternative(ctx)

  def sentence(ctx: NarrativeContext): Option[String] =
    build(ctx).map(_.sentence)

  private def fromCloseAlternative(ctx: NarrativeContext): Option[AlternativeNarrative] =
    val bestLine = ctx.engineEvidence.flatMap(_.best)
    val bestMoveOpt = bestLine.flatMap(variationLeadSan(ctx.fen, _))
    val playedMoveOpt = ctx.playedSan.filter(_.trim.nonEmpty)

    val moveA = bestMoveOpt
    val uciA = bestLine.flatMap(_.moves.headOption)

    val (moveB, uciB) =
      playedMoveOpt.filter(pm => !bestMoveOpt.exists(equalMoveToken(ctx.fen)(pm, _))) match
        case Some(played) =>
          val playedLine = ctx.engineEvidence.flatMap(_.variations.find(v => 
            v.moves.headOption.exists(uci => 
              NarrativeUtils.uciEquivalent(uci, played) || 
              NarrativeUtils.uciToSanOrFormat(ctx.fen, uci) == played
            )
          ))
          val playedUci = playedLine.flatMap(_.moves.headOption).orElse(
            ctx.candidates.find(c => equalMoveToken(ctx.fen)(c.move, played)).flatMap(_.uci)
          )
          (Some(played), playedUci)
        case None =>
          val altLine = ctx.engineEvidence.flatMap(_.alternatives(CloseAlternativeThresholdCp).headOption)
          val altMove = altLine.flatMap(variationLeadSan(ctx.fen, _))
          (altMove, altLine.flatMap(_.moves.headOption))

    (moveA, moveB) match
      case (Some(ma), Some(mb)) =>
        val engineEvidences = LineConsequenceEvaluator.fromEngine(ctx)
        val evidenceA = engineEvidences.find(e =>
          e.sanMoves.headOption.exists(equalMoveToken(ctx.fen)(_, ma)) ||
          e.uciMoves.headOption.exists(equalMoveToken(ctx.fen)(_, ma))
        )
        val evidenceB = engineEvidences.find(e =>
          e.sanMoves.headOption.exists(equalMoveToken(ctx.fen)(_, mb)) ||
          e.uciMoves.headOption.exists(equalMoveToken(ctx.fen)(_, mb))
        )

        val startPly = NarrativeUtils.plyFromFen(ctx.fen).map(_ + 1).getOrElse(1)
        val intentA = findMoveIntent(ctx, ma, uciA)
        val intentB = findMoveIntent(ctx, mb, uciB)
        val playedIsBest = playedMoveOpt.exists(equalMoveToken(ctx.fen)(_, ma))

        (evidenceA, evidenceB) match
          case (Some(evA), Some(evB)) if evA.sanMoves.nonEmpty && evB.sanMoves.nonEmpty =>
            val lineAStr = NarrativeUtils.formatSanWithMoveNumbers(startPly, evA.sanMoves.take(3))
            val lineBStr = NarrativeUtils.formatSanWithMoveNumbers(startPly, evB.sanMoves.take(3))
            val sentenceText =
              (evA.kind, evB.kind) match
                case (LineConsequenceKind.CentralPawnAdvance, LineConsequenceKind.ExchangeSequence) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"While the engine prefers $lineAStr ($intentA), the alternative $mb ($intentB) instead triggers an exchange sequence with $lineBStr."
                case (LineConsequenceKind.ExchangeSequence, LineConsequenceKind.CentralPawnAdvance) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"While the engine prefers $lineAStr ($intentA) leading to an exchange sequence, the alternative $mb ($intentB) instead pushes a central pawn with $lineBStr."
                case (LineConsequenceKind.ForcingCheckSequence, _) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"While the engine prefers $lineAStr ($intentA) to initiate a forcing check sequence, the alternative $mb ($intentB) continues quieter with $lineBStr."
                case (_, LineConsequenceKind.ForcingCheckSequence) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"While the engine prefers $lineAStr ($intentA), the alternative $mb ($intentB) instead starts a forcing check sequence with $lineBStr."
                case (LineConsequenceKind.ExchangeSequence, LineConsequenceKind.ExchangeSequence) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"Both options lead to exchanges: the engine prefers $lineAStr ($intentA), whereas the alternative $mb ($intentB) trades via $lineBStr."
                case (LineConsequenceKind.CentralPawnAdvance, LineConsequenceKind.CentralPawnAdvance) =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"Both lines push pawns into the center: the engine prefers $lineAStr ($intentA), while the alternative $mb ($intentB) advances via $lineBStr."
                case _ =>
                  if (playedIsBest) playedBranchSentence(ma, intentA, lineAStr, mb, intentB, lineBStr, evA.kind, evB.kind)
                  else s"While the engine prefers $lineAStr ($intentA), the alternative $mb ($intentB) opts for $lineBStr."
            Some(
              AlternativeNarrative(
                move = Some(mb),
                reason = "different strategic branches",
                sentence = sentenceText,
                source = "close_candidate"
              )
            )
          case (Some(evA), _) if evA.sanMoves.nonEmpty =>
            val lineAStr = NarrativeUtils.formatSanWithMoveNumbers(startPly, evA.sanMoves.take(3))
            val actionA = evA.kind match
              case LineConsequenceKind.CentralPawnAdvance   => "to push a central pawn"
              case LineConsequenceKind.ExchangeSequence     => "to initiate an exchange sequence"
              case LineConsequenceKind.ForcingCheckSequence => "to initiate a forcing check sequence"
              case LineConsequenceKind.MaterialTransition   => "to change the material balance"
              case _                                        => "for positional reasons"
            val legacyAlt = fallbackLegacy(ctx, mb)
            val legacyReason = legacyAlt.map(_.reason).getOrElse("it leads to a less active setup")
            val sentenceText =
              if (playedIsBest) s"The played $ma ($intentA) follows $lineAStr, whereas $mb ($intentB) stays secondary because $legacyReason."
              else s"While the engine prefers $lineAStr ($intentA) $actionA, the alternative $mb ($intentB) stays secondary because $legacyReason."
            Some(
              AlternativeNarrative(
                move = Some(mb),
                reason = "engine pv contrast",
                sentence = sentenceText,
                source = "close_candidate"
              )
            )
          case _ =>
            val legacyAlt = fallbackLegacy(ctx, mb)
            legacyAlt.map { la =>
              val enhancedSentence = s"The practical alternative $mb ($intentB) stays secondary because ${la.reason}."
              la.copy(sentence = enhancedSentence)
            }
      case _ => None

  private def fallbackLegacy(ctx: NarrativeContext, altMove: String): Option[AlternativeNarrative] =
    val candidateReason =
      ctx.candidates.drop(1).find(c => equalMoveToken(ctx.fen)(c.move, altMove))
        .flatMap(_.whyNot)
        .map(normalizeSentenceFragment)
        .filter(_.nonEmpty)
    candidateReason.map { reason =>
      AlternativeNarrative(
        move = Some(altMove),
        reason = reason,
        sentence = renderSentence(Some(altMove), reason),
        source = "close_candidate"
      )
    }

  private def findMoveIntent(ctx: NarrativeContext, moveSan: String, moveUci: Option[String]): String =
    ctx.candidates.find(c =>
      equalMoveToken(ctx.fen)(c.move, moveSan) ||
      (moveUci.isDefined && c.uci.exists(_.equalsIgnoreCase(moveUci.get)))
    ).map(_.planAlignment)
     .map(_.toLowerCase)
     .getOrElse("positional maneuvering")

  private def playedBranchSentence(
      playedMove: String,
      playedIntent: String,
      playedLine: String,
      otherMove: String,
      otherIntent: String,
      otherLine: String,
      playedKind: LineConsequenceKind,
      otherKind: LineConsequenceKind
  ): String =
    val playedAction = branchAction(playedKind, playedLine)
    val otherAction = branchAction(otherKind, otherLine)
    s"Both candidate branches are viable: the played $playedMove ($playedIntent) $playedAction, whereas $otherMove ($otherIntent) $otherAction."

  private def branchAction(kind: LineConsequenceKind, line: String): String =
    kind match
      case LineConsequenceKind.CentralPawnAdvance   => s"advances via $line"
      case LineConsequenceKind.ExchangeSequence     => s"trades via $line"
      case LineConsequenceKind.ForcingCheckSequence => s"starts a forcing check sequence with $line"
      case LineConsequenceKind.MaterialTransition   => s"changes the material balance with $line"
      case _                                        => s"follows $line"

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

  private def equalMoveToken(fen: String)(a: String, b: String): Boolean =
    val uciA = NarrativeUtils.sanToUci(fen, a).getOrElse(NarrativeUtils.normalizeUciMove(a))
    val uciB = NarrativeUtils.sanToUci(fen, b).getOrElse(NarrativeUtils.normalizeUciMove(b))
    NarrativeUtils.uciEquivalent(uciA, uciB)
