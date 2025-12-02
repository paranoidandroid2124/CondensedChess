package chess
package analysis

/** Lightweight mistake taxonomy based on move delta, SAN, and concept signals.
  * The goal is to give the LLM and UI a concrete "why" (tactical miss vs greedy vs positional vs ignored threat).
  */
object MistakeClassifier:

  final case class Input(
      ply: Ply,
      judgement: String,
      deltaWinPct: Double,
      san: String,
      player: Color,
      inCheckBefore: Boolean,
      concepts: AnalyzePgn.Concepts,
      features: FeatureExtractor.SideFeatures,
      oppFeatures: FeatureExtractor.SideFeatures
  )

  def classify(in: Input): Option[String] =
    val severe = in.judgement == "blunder" || in.judgement == "mistake"
    val capture = in.san.contains("x")
    val bigDrop = in.deltaWinPct <= -8.0
    val midDrop = in.deltaWinPct <= -4.0
    val tacticalZone = in.concepts.tacticalDepth >= 0.35 || in.concepts.blunderRisk >= 0.3
    val weakKingZone = in.oppFeatures.kingRingPressure >= 3

    if severe && bigDrop && tacticalZone then Some("tactical_miss")
    else if severe && capture && (bigDrop || midDrop) then Some("greedy")
    else if severe && !capture && in.concepts.badBishop >= 0.35 && in.features.bishopPair == false then Some("positional_trade_error")
    else if (severe || midDrop) && (in.inCheckBefore || weakKingZone) then Some("ignored_threat")
    else None
