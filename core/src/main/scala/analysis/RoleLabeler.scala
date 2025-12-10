package chess
package analysis

import scala.collection.immutable.Set

object RoleLabeler {

  enum Role:
    // 1. Opening Portrait
    case DevelopmentAdvantage, SpaceGrab, KingSafetyCrisis
    // 2. Structural Landmark
    case CenterUnlock, WingAttack, PieceTrade
    // 3. Turning Point
    case BlunderPunishment, MissedOpportunity, Equalizer, GameDecider
    // 4. Tactical Moment
    case Violence, Complication
    // 5. Positional Mastery (Squeeze)
    case Squeeze, Prophylaxis, Consolidation
    // 6. Endgame Technique
    case PromotionRace, Conversion, FortressBuilding, TechnicalDefense

  import Role._



  def label(ctx: AnalysisContext): Set[Role] = {
    var roles = Set.empty[Role]

    // --- 1. Opening Portrait ---
    if (ctx.phaseBefore == "opening") {
      if (isDevelopmentAdvantage(ctx)) roles += DevelopmentAdvantage
      if (isSpaceGrab(ctx)) roles += SpaceGrab
      if (isKingSafetyCrisis(ctx)) roles += KingSafetyCrisis
    }

    // --- 2. Structural Landmark ---
    if (isCenterUnlock(ctx)) roles += CenterUnlock
    if (isWingAttack(ctx)) roles += WingAttack
    if (isPieceTrade(ctx)) roles += PieceTrade

    // --- 3. Turning Point ---
    val heroBef = ctx.heroEvalBefore
    val heroAft = ctx.heroEvalAfter
    
    // Equalizer: Losing (<-150) to Neutral (> -50)
    if (heroBef < -150 && heroAft > -50) roles += Equalizer
    
    // GameDecider: Neutral (abs < 100) to Winning (> 200)
    if (Math.abs(heroBef) < 100 && heroAft > 200) roles += GameDecider
    
    // BlunderPunishment: Opponent made error previous turn (implied by us being winning now?), 
    // And we maintained it? Or simply: We are winning, and we increased advantage?
    // User suggestion: "Maintained punishment". For single move, maybe skip or use high eval.
    // We'll skip for now as per user instruction "TODO later".

    // --- 4. Tactical Moment ---
    if (isViolence(ctx)) roles += Violence

    // --- 5. Positional Mastery ---
    if (isSqueeze(ctx)) roles += Squeeze
    if (isConsolidation(ctx)) roles += Consolidation

    // --- 6. Endgame Technique ---
    if (ctx.phaseAfter == "endgame") {
      if (isPromotionRace(ctx)) roles += PromotionRace
      if (isConversion(ctx)) roles += Conversion
      if (isFortressBuilding(ctx)) roles += FortressBuilding
    }

    roles
  }

  // --- Rule Implementations ---

  private def isDevelopmentAdvantage(ctx: AnalysisContext): Boolean =
    val (myBef, oppBef) = if ctx.isWhiteToMove 
      then (ctx.featuresBefore.development.whiteUndevelopedMinors, ctx.featuresBefore.development.blackUndevelopedMinors)
      else (ctx.featuresBefore.development.blackUndevelopedMinors, ctx.featuresBefore.development.whiteUndevelopedMinors)
    val (myAft, oppAft) = if ctx.isWhiteToMove
      then (ctx.featuresAfter.development.whiteUndevelopedMinors, ctx.featuresAfter.development.blackUndevelopedMinors)
      else (ctx.featuresAfter.development.blackUndevelopedMinors, ctx.featuresAfter.development.whiteUndevelopedMinors)
    
    val gapBefore = oppBef - myBef
    val gapAfter = oppAft - myAft
    gapAfter > gapBefore // Gap increased (we developed faster or opponent undeveloped?) relative to before

  private def isSpaceGrab(ctx: AnalysisContext): Boolean =
    def space(features: FeatureExtractor.PositionFeatures, white: Boolean): Int =
      // Use portfolio: Central + Restrictive
      if white then features.space.whiteCentralSpace + features.space.whiteRestrictivePawns
      else features.space.blackCentralSpace + features.space.blackRestrictivePawns
      
    val mySpaceBef = space(ctx.featuresBefore, ctx.isWhiteToMove)
    val mySpaceAft = space(ctx.featuresAfter, ctx.isWhiteToMove)
    
    mySpaceAft > mySpaceBef + 1 // Significant gain

  private def isKingSafetyCrisis(ctx: AnalysisContext): Boolean =
    // Crisis: Sudden drop in safety OR Check
    val shieldBef = if ctx.isWhiteToMove then ctx.featuresBefore.kingSafety.whiteKingShieldPawns else ctx.featuresBefore.kingSafety.blackKingShieldPawns
    val shieldAft = if ctx.isWhiteToMove then ctx.featuresAfter.kingSafety.whiteKingShieldPawns else ctx.featuresAfter.kingSafety.blackKingShieldPawns
    
    ctx.isCheck || (shieldAft < shieldBef)

  private def isCenterUnlock(ctx: AnalysisContext): Boolean =
    // Check central fixed pawns.
    // CenterUnlock: Locked -> Unlocked.
    val lockedBef = ctx.featuresBefore.pawns.whiteCentralFixed // Symmetric
    val lockedAft = ctx.featuresAfter.pawns.whiteCentralFixed
    lockedBef && !lockedAft

  private def isWingAttack(ctx: AnalysisContext): Boolean =
    // Wing Attack: Pawn move on a/b (Queenside) or g/h (Kingside)
    // 1. Parse move to get source square
    val moveStr = ctx.move
    val fromKey = moveStr.substring(0, 2)
    val toKey = moveStr.substring(2, 4)
    
    // 2. Identify Piece from FEN (Expensive but accurate)
    // We only need to check if it's a pawn and on wing.
    // Optimization: Check file first.
    val fileChar = fromKey.charAt(0)
    val isWing = fileChar == 'a' || fileChar == 'b' || fileChar == 'g' || fileChar == 'h'
    
    if (!isWing) false
    else
      // Check if it is a Pawn
      // Use scalachess Fen reader
      val fen = ctx.featuresBefore.fen
      val pieceOpt = for {
        parsed <- chess.format.Fen.read(chess.variant.Standard, fen.asInstanceOf[chess.format.Fen.Full])
        sq <- chess.Square.fromKey(fromKey)
        piece <- parsed.board.pieceAt(sq)
      } yield piece

      pieceOpt.exists(_.role == chess.Pawn)

  private def isPieceTrade(ctx: AnalysisContext): Boolean =
    // Total material decrease
    val matBef = ctx.featuresBefore.materialPhase.whiteMaterial + ctx.featuresBefore.materialPhase.blackMaterial
    val matAft = ctx.featuresAfter.materialPhase.whiteMaterial + ctx.featuresAfter.materialPhase.blackMaterial
    ctx.isCapture && (matAft < matBef)

  private def isViolence(ctx: AnalysisContext): Boolean =
    ctx.isCapture || ctx.isCheck

  private def isSqueeze(ctx: AnalysisContext): Boolean =
    // Opponent mobility drop + My space gain + Eval improving
    val hero = if ctx.isWhiteToMove then "white" else "black"
    def mobility(f: FeatureExtractor.PositionFeatures, isWhite: Boolean) = 
      if isWhite then f.activity.whiteLegalMoves else f.activity.blackLegalMoves // approximating mobility by legal moves
    
    // Opponent
    val oppMobBef = mobility(ctx.featuresBefore, !ctx.isWhiteToMove)
    val oppMobAft = mobility(ctx.featuresAfter, !ctx.isWhiteToMove)
    
    // Space (reuse isSpaceGrab logic?)
    def space(features: FeatureExtractor.PositionFeatures, white: Boolean): Int =
      if white then features.space.whiteCentralSpace else features.space.blackCentralSpace
      
    val mySpaceBef = space(ctx.featuresBefore, ctx.isWhiteToMove)
    val mySpaceAft = space(ctx.featuresAfter, ctx.isWhiteToMove)

    val mobilityDrop = oppMobBef - oppMobAft
    val spaceGain = mySpaceAft - mySpaceBef
    val dEval = ctx.heroEvalDiff
    
    mobilityDrop > 0 && spaceGain >= 0 && dEval > 0 && dEval < 60

  private def isConsolidation(ctx: AnalysisContext): Boolean =
    // Winning (> 150), stable, no violence
    val ev = ctx.heroEvalAfter
    val dEval = Math.abs(ctx.heroEvalDiff)
    
    ev > 150 && dEval < 30 && !isViolence(ctx)

  private def isPromotionRace(ctx: AnalysisContext): Boolean =
    // Advanced passed pawns for BOTH sides
    ctx.featuresAfter.pawns.whiteAdvancedPassedPawns > 0 && 
    ctx.featuresAfter.pawns.blackAdvancedPassedPawns > 0
     
  private def isConversion(ctx: AnalysisContext): Boolean =
     // Hero Winning + Simplification
     ctx.heroEvalAfter > 200 && isPieceTrade(ctx) && !isViolence(ctx) // Avoid tactical conversions
     
  private def isFortressBuilding(ctx: AnalysisContext): Boolean =
     // Losing but stable
     val ev = ctx.heroEvalAfter
     val dEval = Math.abs(ctx.heroEvalDiff)
     
     ev < -200 && dEval < 15 && !isViolence(ctx)
}
