package lila.llm.analysis

import lila.llm.model._

/**
 * Phase 6.8: LogicReconstructor
 * Detects moves that exhibit Greedy, Lazy, or Phantom behaviors.
 */
object LogicReconstructor:

  case class Reconstruction(
    kind: ReconstructionKind,
    description: String
  )

  enum ReconstructionKind:
    case Greedy, Lazy, Phantom, Principled

  def analyze(ctx: NarrativeContext): Option[Reconstruction] =
    detectGreedy(ctx) match
      case Some(r) => Some(r)
      case None => detectLazy(ctx) match
        case Some(r) => Some(r)
        case None => detectPhantom(ctx)

  private def detectGreedy(ctx: NarrativeContext): Option[Reconstruction] =
    for
      delta <- ctx.delta
      best <- ctx.candidates.headOption
      playedUci <- ctx.playedMove
      playedSan <- ctx.playedSan
      if playedSan.contains("x") // Capture
      if delta.evalChange <= -100 // Significant drop
      if best.uci.exists(_ != playedUci) // Not the best move
    yield Reconstruction(
      ReconstructionKind.Greedy,
      "The capture is blinded by greed, overlooking the tactical refutation."
    )

  private def detectLazy(ctx: NarrativeContext): Option[Reconstruction] =
    for
      delta <- ctx.delta
      best <- ctx.candidates.headOption
      playedUci <- ctx.playedMove
      playedSan <- ctx.playedSan
      if playedSan.length <= 2 // Simple pawn move or piece shuffle
      if delta.evalChange <= -50 // Noticeable drop
      if best.uci.exists(_ != playedUci)
      if ctx.header.criticality == "CRITICAL" // Lazy in a critical moment
    yield Reconstruction(
      ReconstructionKind.Lazy,
      "A lazy decision that fails to address the urgency of the position."
    )

  private def detectPhantom(ctx: NarrativeContext): Option[Reconstruction] =
    for
      playedUci <- ctx.playedMove
      playedSan <- ctx.playedSan
      if !playedSan.contains("x") && !playedSan.contains("+")
      if ctx.threats.toUs.isEmpty // No threats to us
      best <- ctx.candidates.headOption
      if best.uci.exists(_ != playedUci)
      // If it looks like a defensive move but there's nothing to defend
      if isDefensiveStyle(playedSan) 
    yield Reconstruction(
      ReconstructionKind.Phantom,
      "Defending against a phantom threat, wasting a valuable tempo."
    )

  private def isDefensiveStyle(san: String): Boolean =
    san.startsWith("K") || san.startsWith("R") || san.contains("1") || san.contains("8")
