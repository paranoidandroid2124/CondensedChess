package lila.chessjudgment.analysis.assembly

import java.util.Locale

import chess.Color
import lila.chessjudgment.model.judgment.*

final case class JudgmentProvenanceAllocator(prefix: String):

  def positionRef(
      role: PositionNodeRole,
      fen: String,
      ply: Int,
      sideToMove: Option[Color]
  ): PositionNodeRef =
    PositionNodeRef(
      fen = fen,
      ply = ply,
      sideToMove = sideToMove,
      id = Some(s"$prefix:position:${key(role)}:$ply")
    )

  def lineRef(line: NormalizedCandidateLine): LineNodeRef =
    LineNodeRef(
      id = s"$prefix:line:${key(line.role)}:${line.rank}:${line.rootMove.getOrElse("none")}",
      rootMove = line.rootMove.getOrElse("none"),
      rank = line.rank,
      role = line.role
    )

  def transitionId(role: TransitionEdgeRole, moveUci: String): String =
    s"$prefix:transition:${key(role)}:${MoveReviewInputNormalizer.normalizeUci(moveUci)}"

  def evidenceId(suffix: String): String =
    s"$prefix:evidence:$suffix"

  def evidenceRef(
      suffix: String,
      producer: EvidenceProducer,
      layer: EvidenceLayer,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      confidence: EvidenceConfidence
  ): EvidenceRef =
    EvidenceRef(
      id = evidenceId(suffix),
      producer = producer,
      layer = layer,
      position = position,
      line = line,
      scope = scope,
      confidence = confidence
    )

  def key(value: Any): String =
    Option(value)
      .map(_.toString.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT))
      .getOrElse("none")

object JudgmentProvenanceAllocator:

  def forInput(input: NormalizedMoveReviewInput): JudgmentProvenanceAllocator =
    JudgmentProvenanceAllocator(
      s"move-review:${input.beforePly}:${MoveReviewInputNormalizer.normalizeUci(input.playedMoveUci)}"
    )
