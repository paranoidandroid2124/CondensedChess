package chess
package analysis

import AnalysisModel.EngineEval
import FeatureExtractor.PositionFeatures

/**
 * Encapsulates all context required to determine the "Role" of a move.
 * This aggregates engine evaluations, feature changes, and game phase info.
 */
case class AnalysisContext(
    move: String,
    
    // Evaluations
    evalBefore: Option[EngineEval],
    evalAfter: Option[EngineEval],
    
    // Best move info (to determine if we found the best move or blundered)
    bestMoveBefore: Option[String],
    bestMoveEval: Option[EngineEval], // The evaluation if we had played the best move
    
    // Feature snapshots
    featuresBefore: PositionFeatures,
    featuresAfter: PositionFeatures,
    
    // Game Phase
    phaseBefore: String,
    phaseAfter: String,
    
    // Move metadata
    isCapture: Boolean,
    isCheck: Boolean,
    isCastle: Boolean
) {
  
  private def getCp(eval: EngineEval): Option[Int] = 
    eval.lines.headOption.flatMap(_.cp)

  // Helpers
  def evalDiff: Int = 
    (for {
      bef <- evalBefore.flatMap(getCp)
      aft <- evalAfter.flatMap(getCp)
    } yield aft - bef).getOrElse(0)

  // How much worse is this move compared to the engine's best move?
  def accuracyLoss: Int =
    (for {
      best <- bestMoveEval.flatMap(getCp)
      actual <- evalAfter.flatMap(getCp)
    } yield best - actual).getOrElse(0)
    
  // Absolute eval (White perspective assumed)
  def absoluteEval: Int = evalAfter.flatMap(getCp).getOrElse(0)

  // Derived properties
  def isWhiteToMove: Boolean = featuresBefore.sideToMove.toLowerCase == "white"
  
  // Hero Eval: Evaluation from the perspective of the side to move at the START of the turn (the one making the move).
  // If move was made by White, hero is White. If Black, hero is Black.
  // Note: Evaluation usually refers to the position AFTER the move.
  // If Standard Stockfish UCI: 'score cp' is side-to-move relative.
  // BUT most engines/wrappers normalize to White.
  // User instruction: "If Stockfish API as is, it's white perspective."
  // So we assume eval is White-relative.
  // Hero is `isWhiteToMove`.
  def heroEval(eval: Option[EngineEval]): Int = 
    val raw = getCp(eval.getOrElse(EngineEval(0, Nil))).getOrElse(0)
    if isWhiteToMove then raw else -raw
    
  def heroEvalBefore: Int = heroEval(evalBefore)
  def heroEvalAfter: Int = heroEval(evalAfter)
  def heroEvalDiff: Int = heroEvalAfter - heroEvalBefore
}
