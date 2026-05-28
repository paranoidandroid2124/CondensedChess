package lila.commentary.analysis.tactical

import chess.Position

private[commentary] trait TacticalPatternDetector:
  def id: String
  def displayName: String
  def requiresMate: Boolean
  def matches(before: Option[Position], after: Position, lastUci: String): Boolean
  def matchesWithContinuations(
      before: Option[Position],
      after: Position,
      lastUci: String,
      _continuations: List[List[String]]
  ): Boolean =
    val _ = _continuations
    matches(before, after, lastUci)
