package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.opening.{ OpeningRecognitionIndex, OpeningThemePriorIndex }
import lila.chessjudgment.analysis.line.PrincipalVariationEvidence
import lila.chessjudgment.model.strategic.VariationLine
import lila.chessjudgment.model.judgment.{
  LineNodeRole,
  OpeningContextSignal,
  OpeningFamily,
  OpeningIdentity,
  OpeningRecognition,
  OpeningThemePrior
}

final case class RawOpeningContext(
    eco: Option[String] = None,
    name: Option[String] = None,
    family: Option[String] = None
)

final case class RawMoveReviewInput(
    fen: String,
    playedMoveUci: String,
    variations: List[VariationLine],
    currentEvalCp: Option[Int] = None,
    ply: Option[Int] = None,
    openingContext: Option[RawOpeningContext] = None,
    movePrefixUci: List[String] = Nil
)

final case class NormalizedCandidateLine(
    role: LineNodeRole,
    rank: Int,
    line: VariationLine
):
  def rootMove: Option[String] =
    line.moves.headOption.map(MoveReviewInputNormalizer.normalizeUci)

final case class NormalizedMoveReviewInput(
    beforeFen: String,
    playedMoveUci: String,
    beforePly: Int,
    sideToMove: Option[Color],
    afterPlayedFen: String,
    afterReferenceFen: Option[String],
    lines: List[NormalizedCandidateLine],
    currentEvalCp: Int,
    opening: Option[OpeningIdentity],
    movePrefixUci: List[String] = Nil,
    openingRecognition: Option[OpeningRecognition] = None,
    openingThemePrior: Option[OpeningThemePrior] = None,
    openingSignals: List[OpeningContextSignal] = Nil
):
  def playedLine: Option[NormalizedCandidateLine] =
    lines.find(_.role == LineNodeRole.Played)

  def referenceLine: Option[NormalizedCandidateLine] =
    lines.find(_.role == LineNodeRole.BestReference)

object MoveReviewInputNormalizer:

  def normalize(
      raw: RawMoveReviewInput,
      recognitionIndex: OpeningRecognitionIndex = OpeningRecognitionIndex.default,
      themePriorIndex: OpeningThemePriorIndex = OpeningThemePriorIndex.default
  ): Option[NormalizedMoveReviewInput] =
    val beforeFen = normalizeFen(raw.fen)
    val playedMove = normalizeUci(raw.playedMoveUci)
    val movePrefix = raw.movePrefixUci.map(normalizeUci).filter(_.nonEmpty)
    for
      afterPlayed <- PrincipalVariationEvidence.legalFenAfter(beforeFen, playedMove)
      currentEval <- raw.currentEvalCp
        .orElse(raw.variations.headOption.map(_.scoreCp))
    yield
      val beforePly = raw.ply.getOrElse(plyFromFen(beforeFen))
      val side = sideToMove(beforeFen)
      val inputOpening = normalizeOpening(raw.openingContext)
      val recognition = recognitionIndex.recognize(movePrefix, beforeFen, beforePly)
      val opening = inputOpening.orElse(recognition.flatMap(_.bestIdentity))
      val themePrior = themePriorIndex.priorFor(recognition.flatMap(_.lineage), opening.flatMap(_.family))
      val openingSignals =
        List(
          Option.when(inputOpening.nonEmpty)(OpeningContextSignal.InputIdentity),
          Option.when(recognition.nonEmpty)(OpeningContextSignal.RecognizedIdentity),
          Option.when(themePrior.nonEmpty)(OpeningContextSignal.ThemePrior)
        ).flatten
      val ranked = raw.variations.zipWithIndex.filter(_._1.moves.nonEmpty)
      val reference = ranked.headOption.map { case (line, index) =>
        NormalizedCandidateLine(LineNodeRole.BestReference, index + 1, normalizedLine(line))
      }
      val played =
        ranked
          .find { case (line, _) => line.moves.headOption.exists(move => normalizeUci(move) == playedMove) }
          .map { case (line, index) => NormalizedCandidateLine(LineNodeRole.Played, index + 1, normalizedLine(line)) }
      val alternatives =
        ranked
          .filterNot { case (_, index) =>
            reference.exists(_.rank == index + 1) || played.exists(_.rank == index + 1)
          }
          .map { case (line, index) => NormalizedCandidateLine(LineNodeRole.Alternative, index + 1, normalizedLine(line)) }
      val lines = (reference.toList ++ played.toList ++ alternatives).distinctBy(line => line.role -> line.rank)
      val afterReference =
        reference.flatMap(_.rootMove).flatMap(PrincipalVariationEvidence.legalFenAfter(beforeFen, _))
      NormalizedMoveReviewInput(
        beforeFen = beforeFen,
        playedMoveUci = playedMove,
        beforePly = beforePly,
        sideToMove = side,
        afterPlayedFen = afterPlayed,
        afterReferenceFen = afterReference,
        lines = lines,
        currentEvalCp = currentEval,
        opening = opening,
        movePrefixUci = movePrefix,
        openingRecognition = recognition,
        openingThemePrior = themePrior,
        openingSignals = openingSignals
      )

  def normalizeUci(uci: String): String =
    PrincipalVariationEvidence.normalizeUci(uci)

  private def normalizedLine(line: VariationLine): VariationLine =
    line.copy(moves = line.moves.map(normalizeUci))

  private def normalizeFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).mkString(" ")

  private def normalizeOpening(raw: Option[RawOpeningContext]): Option[OpeningIdentity] =
    raw.flatMap { context =>
      val eco = cleanText(context.eco).map(_.toUpperCase)
      val name = cleanText(context.name)
      val family =
        cleanText(context.family).flatMap(OpeningFamily.fromRaw)
          .orElse(eco.flatMap(OpeningFamily.fromEco))
      Option.when(eco.nonEmpty || name.nonEmpty || family.nonEmpty)(
        OpeningIdentity(
          eco = eco,
          name = name,
          family = family
        )
      )
    }

  private def cleanText(raw: Option[String]): Option[String] =
    raw.map(_.trim).filter(_.nonEmpty)

  private def sideToMove(fen: String): Option[Color] =
    fen.split("\\s+").lift(1).flatMap:
      case "w" => Some(Color.White)
      case "b" => Some(Color.Black)
      case _   => None

  private def plyFromFen(fen: String): Int =
    val parts = fen.split("\\s+")
    val fullMove = parts.lift(5).flatMap(_.toIntOption).getOrElse(1).max(1)
    val blackToMove = parts.lift(1).contains("b")
    (fullMove - 1) * 2 + Option.when(blackToMove)(1).getOrElse(0)
