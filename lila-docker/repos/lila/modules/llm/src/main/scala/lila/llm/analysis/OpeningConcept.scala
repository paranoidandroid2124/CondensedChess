package lila.llm.analysis

import lila.llm.model._
import _root_.chess.format.Fen
import _root_.chess.{ Color, Role, Square, Position }
import _root_.chess.variant.Standard

object OpeningGoals:

  enum Status:
    case Achieved
    case Partial
    case Premature
    case Failed
    case Mismatch

  case class Evaluation(
    goalName: String,
    status: Status,
    supportedEvidence: List[String],
    missingEvidence: List[String],
    confidence: Double,
    mismatchReason: Option[String] = None
  )

  trait GoalDefinition:
    def id: String
    def name: String
    def triggers(uci: String): Boolean
    def evaluate(ctx: NarrativeContext, situation: Option[Position]): Evaluation

  // --- Helpers ---
  private def isKingSafe(ctx: NarrativeContext): Boolean =
    ctx.snapshots.headOption.flatMap(_.kingSafetyUs) match
      case Some(s) => !s.toLowerCase.contains("exposed")
      case None => true

  private def checkCp(ctx: NarrativeContext, color: Color, threshold: Int): Boolean =
    val whiteScore = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
    val sideScore = if (color == Color.White) whiteScore else -whiteScore
    sideScore >= threshold
    
  private def hasPawn(sit: Option[Position], sq: Square, color: Color): Boolean =
    sit.exists(_.board.pieceAt(sq).contains(_root_.chess.Piece(color, _root_.chess.Pawn)))
  
  private def hasPiece(sit: Option[Position], sq: Square, color: Color, role: Role): Boolean =
    sit.exists(_.board.pieceAt(sq).contains(_root_.chess.Piece(color, role)))

  private def toSquare(uci: String): Option[Square] =
    Option(uci).filter(_.length >= 4).flatMap(raw => Square.fromKey(raw.slice(2, 4)))

  private def movedPiece(sit: Option[Position], uci: String): Option[_root_.chess.Piece] =
    toSquare(uci).flatMap(sq => sit.flatMap(_.board.pieceAt(sq)))

  private def movedColor(sit: Option[Position], uci: String): Option[Color] =
    movedPiece(sit, uci).map(_.color)

  private def developedMinorCount(sit: Option[Position], color: Color): Int =
    val homeSquares =
      if color == Color.White then
        List(
          (Square.B1, _root_.chess.Knight),
          (Square.G1, _root_.chess.Knight),
          (Square.C1, _root_.chess.Bishop),
          (Square.F1, _root_.chess.Bishop)
        )
      else
        List(
          (Square.B8, _root_.chess.Knight),
          (Square.G8, _root_.chess.Knight),
          (Square.C8, _root_.chess.Bishop),
          (Square.F8, _root_.chess.Bishop)
        )
    homeSquares.count { case (sq, role) => !hasPiece(sit, sq, color, role) }

  private def centerFootprint(sit: Option[Position], color: Color): Int =
    val centerSquares =
      if color == Color.White then List(Square.C4, Square.D4, Square.E4, Square.F4)
      else List(Square.C5, Square.D5, Square.E5, Square.F5)
    centerSquares.count(sq => hasPawn(sit, sq, color))

  private def queenAtHome(sit: Option[Position], color: Color): Boolean =
    hasPiece(sit, if color == Color.White then Square.D1 else Square.D8, color, _root_.chess.Queen)

  private def fianchettoReady(sit: Option[Position], color: Color): Boolean =
    if color == Color.White then
      hasPawn(sit, Square.G3, color) || hasPawn(sit, Square.B3, color)
    else
      hasPawn(sit, Square.G6, color) || hasPawn(sit, Square.B6, color)

  private def fianchettoEstablished(sit: Option[Position], color: Color): Boolean =
    if color == Color.White then
      hasPiece(sit, Square.G2, color, _root_.chess.Bishop) || hasPiece(sit, Square.B2, color, _root_.chess.Bishop)
    else
      hasPiece(sit, Square.G7, color, _root_.chess.Bishop) || hasPiece(sit, Square.B7, color, _root_.chess.Bishop)

  private def rookPawnAdvanced(sit: Option[Position], color: Color): Boolean =
    if color == Color.White then
      List(Square.A3, Square.A4, Square.B3, Square.B4, Square.G4, Square.H3, Square.H4).exists(hasPawn(sit, _, color))
    else
      List(Square.A6, Square.A5, Square.B6, Square.B5, Square.G5, Square.H6, Square.H5).exists(hasPawn(sit, _, color))

  private def castled(sit: Option[Position], color: Color): Boolean =
    if color == Color.White then
      hasPiece(sit, Square.G1, color, _root_.chess.King) || hasPiece(sit, Square.C1, color, _root_.chess.King)
    else
      hasPiece(sit, Square.G8, color, _root_.chess.King) || hasPiece(sit, Square.C8, color, _root_.chess.King)

  private def oppositeSideCastled(sit: Option[Position]): Boolean =
    val whiteShort = hasPiece(sit, Square.G1, Color.White, _root_.chess.King)
    val whiteLong = hasPiece(sit, Square.C1, Color.White, _root_.chess.King)
    val blackShort = hasPiece(sit, Square.G8, Color.Black, _root_.chess.King)
    val blackLong = hasPiece(sit, Square.C8, Color.Black, _root_.chess.King)
    (whiteShort && blackLong) || (whiteLong && blackShort)

  // --- Goals ---

  // 1. Sicilian Liberator (...d5)
  object SicilianLiberator extends GoalDefinition:
    val id = "sicilian_liberator"
    val name = "Sicilian Liberator"
    def triggers(uci: String) = uci == "d7d5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasC5 = hasPawn(sit, Square.C5, Color.Black)
      val isOpenSicilian = !hasPawn(sit, Square.C7, Color.Black) && 
                          !hasPawn(sit, Square.C5, Color.Black) &&
                          hasPawn(sit, Square.E4, Color.White) &&
                          (hasPawn(sit, Square.D6, Color.Black) || hasPawn(sit, Square.E6, Color.Black))
      
      if !(hasC5 || isOpenSicilian) then
         Evaluation(name, Status.Mismatch, Nil, List("Not a Sicilian structure"), 0.11, Some("this thematic break requires a Sicilian structure (c5 pawn or traded c-pawn)"))
      else
        val safe = isKingSafe(ctx)
        val sound = checkCp(ctx, Color.Black, -50)
        if safe && sound then Evaluation(name, Status.Achieved, List("King safe", "Sound"), Nil, 0.9)
        else if sound then Evaluation(name, Status.Partial, List("Sound"), List("King safety"), 0.8)
        else Evaluation(name, Status.Premature, List("Structure ready"), List("Soundness"), 0.6)

  // 2. French Base Chipper (...c5)
  object FrenchBaseChipper extends GoalDefinition:
    val id = "french_base_chipper" 
    val name = "French Base Chipper"
    def triggers(uci: String) = uci == "c7c5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      if !(hasPawn(sit, Square.E6, Color.Black) && hasPawn(sit, Square.D5, Color.Black)) then
         Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch"), 0.10, Some("this thematic strike requires a French structure (e6 and d5 pawns)"))
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -40 then Evaluation(name, Status.Achieved, List("Sound"), Nil, 0.9)
        else if sideScore >= -80 then Evaluation(name, Status.Partial, List("Structure met"), List("Tactical precision"), 0.75)
        else Evaluation(name, Status.Premature, List("Structure met"), List("Soundness"), 0.6)

  // 3. French Chain Breaker (...f6)
  object FrenchChainBreaker extends GoalDefinition:
    val id = "french_chain_breaker"
    val name = "French Chain Breaker"
    def triggers(uci: String) = uci == "f7f6"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      if !(hasPawn(sit, Square.E6, Color.Black) && hasPawn(sit, Square.D5, Color.Black) && hasPawn(sit, Square.E5, Color.White)) then 
        Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch (needs White e5)"), 0.0)
      else
         val safe = isKingSafe(ctx)
         val sound = checkCp(ctx, Color.Black, -60)
         if safe && sound then Evaluation(name, Status.Achieved, List("King safe"), Nil, 0.9)
         else if sound then Evaluation(name, Status.Partial, List("Structure met"), List("King safety"), 0.8)
         else Evaluation(name, Status.Premature, List("Idea correct"), List("King safety").filter(_ => !safe), 0.7)

  // 4. KID Kingside Storm (...f5)
  object KIDKingsideStorm extends GoalDefinition:
    val id = "kid_storm"
    val name = "Kingside Storm"
    def triggers(uci: String) = uci == "f7f5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasStructure = hasPawn(sit, Square.G6, Color.Black) && 
                        hasPawn(sit, Square.D6, Color.Black) && 
                        hasPiece(sit, Square.F6, Color.Black, _root_.chess.Knight)
      val whiteControl = hasPawn(sit, Square.D4, Color.White) || hasPawn(sit, Square.C4, Color.White)
      
      if !(hasStructure && whiteControl) then 
         Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch (needs g6, d6, Nf6)"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -60 then Evaluation(name, Status.Achieved, List("Sound"), Nil, 0.9)
        else if sideScore >= -100 then Evaluation(name, Status.Partial, List("Storm brewing"), List("Stability"), 0.7)
        else Evaluation(name, Status.Premature, List("Aggressive intent"), List("Soundness"), 0.5)

  // 5. Benoni Queenside Expansion (...b5)
  object BenoniExpansion extends GoalDefinition:
    val id = "benoni_expansion"
    val name = "Benoni Expansion"
    def triggers(uci: String) = uci == "b7b5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasStructure = hasPawn(sit, Square.C5, Color.Black) && hasPawn(sit, Square.D6, Color.Black)
      val whitePawnD5 = hasPawn(sit, Square.D5, Color.White)
      val whiteD4Moved = !hasPawn(sit, Square.D2, Color.White) && !hasPawn(sit, Square.D4, Color.White)
      
      if !(hasStructure && (whitePawnD5 || whiteD4Moved)) then 
         Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch (needs c5, d6, and White d5/moved d4)"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -40 then Evaluation(name, Status.Achieved, List("Sound"), Nil, 0.9)
        else if sideScore >= -80 then Evaluation(name, Status.Partial, List("Thematic idea"), List("Tactical precision"), 0.7)
        else Evaluation(name, Status.Premature, List("Structural ambition"), List("Soundness"), 0.6)

  // 6. Catalan Expansion (e4)
  object CatalanExpansion extends GoalDefinition:
    val id = "catalan_expansion"
    val name = "Catalan Expansion"
    def triggers(uci: String) = uci == "g2g3" || uci == "c2c4"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasD4 = hasPawn(sit, Square.D4, Color.White)
      val hasBg2 = hasPiece(sit, Square.G2, Color.White, _root_.chess.Bishop)
      val hasC4 = hasPawn(sit, Square.C4, Color.White)
      
      if !(hasD4 && (hasBg2 || hasC4)) then 
         Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch (needs d4 + c4/Bg2)"), 0.0)
      else
        val safe = isKingSafe(ctx)
        val sound = checkCp(ctx, Color.White, -30)
        if safe && sound then Evaluation(name, Status.Achieved, List("King safe"), Nil, 0.9)
        else if sound then Evaluation(name, Status.Partial, List("Sound"), List("King safety"), 0.8)
        else Evaluation(name, Status.Premature, List("Ambition high"), List("Preparation").filter(_ => !safe), 0.7)
  
  // 7. QG Challenge (...c5)
  object QGChallenge extends GoalDefinition:
    val id = "qg_challenge"
    val name = "Queen's Gambit Challenge"
    def triggers(uci: String) = uci == "c7c5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      if !hasPawn(sit, Square.D5, Color.Black) || !hasPawn(sit, Square.C4, Color.White) then 
        Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -40 then Evaluation(name, Status.Achieved, List("Sound"), Nil, 0.9)
        else if sideScore >= -80 then Evaluation(name, Status.Partial, List("Thematic break"), List("Coordination"), 0.7)
        else Evaluation(name, Status.Premature, List("Ambition"), List("Soundness"), 0.6)

  // 8. London Pyramid Peak (e4)
  object LondonPeak extends GoalDefinition:
    val id = "london_peak"
    val name = "London Pyramid Peak"
    def triggers(uci: String) = uci == "e2e4" || uci == "c2c4"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val core = hasPawn(sit, Square.D4, Color.White) && 
                 (hasPawn(sit, Square.E3, Color.White) || hasPawn(sit, Square.E4, Color.White)) &&
                 hasPiece(sit, Square.F4, Color.White, _root_.chess.Bishop) &&
                 hasPiece(sit, Square.F3, Color.White, _root_.chess.Knight)
                 
      if !core then
        Evaluation(name, Status.Mismatch, Nil, List("No London setup (needs d4, e3/e4, Bf4, Nf3)"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = cp // White goal
        if sideScore >= -20 then Evaluation(name, Status.Achieved, List("Pyramid peak reached"), Nil, 0.95)
        else if sideScore >= -50 then Evaluation(name, Status.Partial, List("Structural ambition"), List("Precise timing"), 0.8)
        else Evaluation(name, Status.Premature, List("Structural ambition"), List("Minor piece preparation"), 0.7)

  // 9. Caro-Kann Liquidator (...c5)
  object CaroLiquidator extends GoalDefinition:
    val id = "caro_liquidator"
    val name = "Caro-Kann Liquidator"
    def triggers(uci: String) = uci == "c6c5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      if !(hasPawn(sit, Square.C6, Color.Black) && hasPawn(sit, Square.D5, Color.Black)) then
        Evaluation(name, Status.Mismatch, Nil, List("No Caro-Kann structure"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -50 then Evaluation(name, Status.Achieved, List("Central tension liquidated"), Nil, 0.9)
        else if sideScore >= -90 then Evaluation(name, Status.Partial, List("Thematic liquidation"), List("Coordination"), 0.7)
        else Evaluation(name, Status.Premature, List("Ambition"), List("Soundness"), 0.6)

  // 10. Nimzo-Indian Challenge (...c5)
  object NimzoChallenge extends GoalDefinition:
    val id = "nimzo_challenge"
    val name = "Nimzo-Indian Challenge"
    def triggers(uci: String) = uci == "c7c5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasPin = hasPiece(sit, Square.B4, Color.Black, _root_.chess.Bishop) &&
                   hasPiece(sit, Square.C3, Color.White, _root_.chess.Knight)
      if !hasPin then
        Evaluation(name, Status.Mismatch, Nil, List("No Nimzo pin (needs Bb4 on Nc3)"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -30 then Evaluation(name, Status.Achieved, List("Pinned center challenged"), Nil, 0.95)
        else if sideScore >= -70 then Evaluation(name, Status.Partial, List("Positional strike"), List("Coordination"), 0.8)
        else Evaluation(name, Status.Premature, List("Positional strike"), List("Soundness"), 0.7)

  // 11. Scandinavian Expansion (d5)
  object ScandinavianExpansion extends GoalDefinition:
    val id = "scandi_expansion"
    val name = "Scandinavian Expansion"
    def triggers(uci: String) = uci == "d7d5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      // Scandinavian is specifically the central response to 1.e4
      val whiteE4 = hasPawn(sit, Square.E4, Color.White)
      if !whiteE4 then Evaluation(name, Status.Mismatch, Nil, List("Not a Scandinavian response to e4"), 0.0)
      else if ctx.ply > 2 then Evaluation(name, Status.Mismatch, Nil, List("Too late for Scandinavian"), 0.0)
      else
        val cp = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp).getOrElse(0)
        val sideScore = -cp // Black goal
        if sideScore >= -60 then Evaluation(name, Status.Achieved, List("Center opened"), Nil, 0.95)
        else if sideScore >= -100 then Evaluation(name, Status.Partial, List("Center opened"), List("Stability"), 0.8)
        else Evaluation(name, Status.Premature, List("Early strike"), List("Black stability"), 0.7)

  // 12. Open Center d4 Break (White)
  object OpenCenterBreak extends GoalDefinition:
    val id = "open_center_break"
    val name = "Open Center d4 Break"
    def triggers(uci: String) = uci == "d2d4"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val structure = hasPawn(sit, Square.E4, Color.White) && hasPawn(sit, Square.E5, Color.Black)
      val development = hasPiece(sit, Square.F3, Color.White, _root_.chess.Knight) ||
                        hasPiece(sit, Square.C4, Color.White, _root_.chess.Bishop) ||
                        hasPiece(sit, Square.B5, Color.White, _root_.chess.Bishop)
      
      if !(structure && development) then
        Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch (needs e4, e5 and development)"), 0.0)
      else
        val sound = checkCp(ctx, Color.White, -30)
        if sound then Evaluation(name, Status.Achieved, List("Central breakthrough"), Nil, 0.9)
        else Evaluation(name, Status.Premature, List("Dynamic intent"), List("Preparation"), 0.6)

  // 13. d5 Equalizer (...d5 in e5-structures)
  object E5Equalizer extends GoalDefinition:
    val id = "e5_equalizer"
    val name = "d5 Equalizer"
    def triggers(uci: String) = uci == "d7d5"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val structure = hasPawn(sit, Square.E4, Color.White) && hasPawn(sit, Square.E5, Color.Black)
      if !structure then
        Evaluation(name, Status.Mismatch, Nil, List("Not a central e4-e5 structure"), 0.12, Some("this stabilizing move is typical in Open Games (1.e4 e5), but the current pawn structure is different"))
      else
        val safe = isKingSafe(ctx)
        val sound = checkCp(ctx, Color.Black, -40)
        if safe && sound then Evaluation(name, Status.Achieved, List("Center neutralized", "King safe"), Nil, 0.9)
        else if sound then Evaluation(name, Status.Partial, List("Sound strike"), List("King safety"), 0.7)
        else Evaluation(name, Status.Premature, List("Structural ambition"), List("Soundness"), 0.6)

  // 14. English Squeeze (1.c4 White flank control)
  object EnglishSqueeze extends GoalDefinition:
    val id = "english_squeeze"
    val name = "English Squeeze"
    def triggers(uci: String) = uci == "c2c4" || uci == "g2g3"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val hasC4 = hasPawn(sit, Square.C4, Color.White)
      val hasG3 = hasPawn(sit, Square.G3, Color.White) || hasPiece(sit, Square.G2, Color.White, _root_.chess.Bishop)
      
      if !(hasC4 && hasG3) then 
        Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch"), 0.0)
      else
        val hasD5Suppression = hasPawn(sit, Square.E4, Color.White) || 
                               hasPiece(sit, Square.C3, Color.White, _root_.chess.Knight) || 
                               hasPiece(sit, Square.F3, Color.White, _root_.chess.Knight) || 
                               hasPawn(sit, Square.B3, Color.White) || 
                               hasPiece(sit, Square.B2, Color.White, _root_.chess.Bishop)
        val sound = checkCp(ctx, Color.White, -30)
        
        if hasD5Suppression && sound then 
          Evaluation(name, Status.Achieved, List("d5 restraint established; White breathes easier in space/development"), Nil, 0.9)
        else
          Evaluation(name, Status.Premature, List("Strategic intent"), List("Fianchetto achieved, but lack of d5 restraint/space allows opponent to equalize easily"), 0.6)

  // 15. Austrian Attack (f4-e4-d4 vs Pirc/Modern)
  object AustrianAttack extends GoalDefinition:
    val id = "austrian_attack"
    val name = "Austrian Attack"
    def triggers(uci: String) = uci == "f2f4"
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      val whitePawns = hasPawn(sit, Square.E4, Color.White) && 
                       hasPawn(sit, Square.D4, Color.White) &&
                       hasPawn(sit, Square.F4, Color.White)
      val blackHypermodern = hasPawn(sit, Square.G6, Color.Black) || hasPawn(sit, Square.D6, Color.Black)
      
      if !(whitePawns && blackHypermodern) then
        Evaluation(name, Status.Mismatch, Nil, List("Structure mismatch"), 0.0)
      else
        val minorDevelopment = hasPiece(sit, Square.C3, Color.White, _root_.chess.Knight) || 
                               hasPiece(sit, Square.F3, Color.White, _root_.chess.Knight)
        val sound = checkCp(ctx, Color.White, -50)
        val safe = isKingSafe(ctx)
        
        if minorDevelopment && sound && safe then 
          Evaluation(name, Status.Achieved, List("Center slam accomplished, development/coordination follows, making the attack sound"), Nil, 0.95)
        else
          Evaluation(name, Status.Premature, List("Aggressive intent"), List("Only ahead in pawns; deferred development/defense makes the center a target"), 0.7)

  // 16. Development Logic (classical minor-piece setup)
  object DevelopmentLogic extends GoalDefinition:
    val id = "development_logic"
    val name = "Development Logic"
    private val developmentMoves = Set(
      "g1f3", "b1c3", "b1d2", "g8f6", "b8c6", "b8d7",
      "f1c4", "f1b5", "c1g5", "c1f4", "f8c5", "f8b4", "c8g4", "c8f5"
    )
    def triggers(uci: String) = developmentMoves.contains(uci)
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      ctx.playedMove
        .flatMap(uci => movedColor(sit, uci).map(_ -> uci))
        .map { case (color, _) =>
          val center = centerFootprint(sit, color)
          val minors = developedMinorCount(sit, color)
          val queenHome = queenAtHome(sit, color)
          val sound = checkCp(ctx, color, -40)
          if center >= 1 && minors >= 2 && queenHome && sound then
            Evaluation(name, Status.Achieved, List("Center foothold", "Minor pieces developed"), Nil, 0.9)
          else if center >= 1 && minors >= 1 && sound then
            Evaluation(name, Status.Partial, List("Development underway"), List("More piece coordination"), 0.78)
          else
            Evaluation(name, Status.Premature, List("Classical setup started"), List("Center or coordination not settled"), 0.62)
        }
        .getOrElse(Evaluation(name, Status.Mismatch, Nil, List("Development pattern missing"), 0.0))

  // 17. Flank Fianchetto Support
  object FlankFianchettoSupport extends GoalDefinition:
    val id = "flank_fianchetto_support"
    val name = "Flank Fianchetto Support"
    private val triggerMoves = Set("g2g3", "b2b3", "g7g6", "b7b6", "f1g2", "c1b2", "f8g7", "c8b7")
    def triggers(uci: String) = triggerMoves.contains(uci)
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      ctx.playedMove
        .flatMap(uci => movedColor(sit, uci).map(_ -> uci))
        .map { case (color, _) =>
          val center = centerFootprint(sit, color)
          val ready = fianchettoReady(sit, color)
          val established = fianchettoEstablished(sit, color)
          val minors = developedMinorCount(sit, color)
          val sound = checkCp(ctx, color, -45)
          if established && center >= 1 && minors >= 1 && sound then
            Evaluation(name, Status.Achieved, List("Long diagonal active", "Center supported"), Nil, 0.92)
          else if (ready || established) && center >= 1 then
            Evaluation(name, Status.Partial, List("Fianchetto shell built"), List("Diagonal activation or coordination"), 0.8)
          else
            Evaluation(name, Status.Mismatch, Nil, List("Fianchetto support missing"), 0.0)
        }
        .getOrElse(Evaluation(name, Status.Mismatch, Nil, List("Fianchetto support missing"), 0.0))

  // 18. Early Queen Exposure
  object EarlyQueenExposure extends GoalDefinition:
    val id = "early_queen_exposure"
    val name = "Early Queen Exposure"
    def triggers(uci: String) =
      Option(uci).exists(raw => raw.startsWith("d1") || raw.startsWith("d8"))
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      ctx.playedMove
        .flatMap(uci => movedColor(sit, uci).map(_ -> uci))
        .map { case (color, uci) =>
          val queenAdvanced = !queenAtHome(sit, color)
          val centralSquare =
            toSquare(uci).exists(sq => Set(Square.D4, Square.D5, Square.E4, Square.E5).contains(sq))
          val minors = developedMinorCount(sit, color)
          val sound = checkCp(ctx, color, -35)
          if ctx.ply > 16 || !queenAdvanced then
            Evaluation(name, Status.Mismatch, Nil, List("Not an early queen deployment"), 0.0)
          else if (centralSquare || minors >= 2) && sound then
            Evaluation(name, Status.Achieved, List("Queen is active early", "Development cost contained"), Nil, 0.86)
          else if sound then
            Evaluation(name, Status.Partial, List("Queen developed"), List("Minor pieces still lag"), 0.72)
          else
            Evaluation(name, Status.Premature, List("Queen left home"), List("Development lag or shaky evaluation"), 0.58)
        }
        .getOrElse(Evaluation(name, Status.Mismatch, Nil, List("Not an early queen deployment"), 0.0))

  // 19. Castle Race
  object CastleRace extends GoalDefinition:
    val id = "castle_race"
    val name = "Castle Race"
    def triggers(uci: String) = Set("e1g1", "e1c1", "e8g8", "e8c8").contains(uci)
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      ctx.playedMove
        .flatMap(uci => movedColor(sit, uci).map(_ -> uci))
        .map { case (color, _) =>
          val raceGeometry =
            oppositeSideCastled(sit) || rookPawnAdvanced(sit, color) || rookPawnAdvanced(sit, !color)
          val sound = checkCp(ctx, color, -40)
          if castled(sit, color) && raceGeometry && sound then
            Evaluation(name, Status.Achieved, List("Kings committed", "Race geometry visible"), Nil, 0.9)
          else if castled(sit, color) && raceGeometry then
            Evaluation(name, Status.Partial, List("Race geometry visible"), List("Need cleaner coordination"), 0.78)
          else
            Evaluation(name, Status.Mismatch, Nil, List("No castling race yet"), 0.0)
        }
        .getOrElse(Evaluation(name, Status.Mismatch, Nil, List("No castling race yet"), 0.0))

  // 20. Thematic Break Preparation
  object ThematicBreakPreparation extends GoalDefinition:
    val id = "thematic_break_preparation"
    val name = "Thematic Break Preparation"
    private val prepMoves = Set(
      "c2c3", "d2d3", "f2f3", "a2a3", "h2h3", "d1c2", "d1e2", "f1e2", "c1e3", "a1e1", "h1e1",
      "c7c6", "d7d6", "f7f6", "a7a6", "h7h6", "d8c7", "d8e7", "f8e7", "c8e6", "a8e8", "h8e8"
    )
    def triggers(uci: String) = prepMoves.contains(uci)
    def evaluate(ctx: NarrativeContext, sit: Option[Position]): Evaluation =
      ctx.playedMove
        .flatMap(uci => movedColor(sit, uci).map(_ -> uci))
        .map { case (color, _) =>
          val center = centerFootprint(sit, color)
          val minors = developedMinorCount(sit, color)
          val sound = checkCp(ctx, color, -40)
          if center >= 1 && minors >= 2 && sound then
            Evaluation(name, Status.Achieved, List("Break support pieces in place", "Center contact preserved"), Nil, 0.88)
          else if center >= 1 && minors >= 1 then
            Evaluation(name, Status.Partial, List("Break scaffold appears"), List("Need one more coordinator"), 0.74)
          else
            Evaluation(name, Status.Mismatch, Nil, List("Break preparation not yet grounded"), 0.0)
        }
        .getOrElse(Evaluation(name, Status.Mismatch, Nil, List("Break preparation not yet grounded"), 0.0))

  val allGoals: List[GoalDefinition] = List(
    NimzoChallenge, ScandinavianExpansion, 
    SicilianLiberator, FrenchBaseChipper, FrenchChainBreaker, 
    KIDKingsideStorm, BenoniExpansion, CatalanExpansion, QGChallenge,
    LondonPeak, CaroLiquidator, OpenCenterBreak, E5Equalizer,
    EnglishSqueeze, AustrianAttack,
    DevelopmentLogic, FlankFianchettoSupport, EarlyQueenExposure,
    CastleRace, ThematicBreakPreparation
  )

  def analyze(ctx: NarrativeContext): Option[Evaluation] =
    val sit = Fen.read(Standard, Fen.Full(ctx.fen))
    ctx.playedMove.flatMap { uci =>
      allGoals
        .filter(_.triggers(uci))
        .map(_.evaluate(ctx, sit))
        .filter(e => e.status != Status.Mismatch || e.mismatchReason.isDefined)
        // Sort by: status (non-mismatch first), then confidence
        .sortBy { e => 
          val statusPriority = if (e.status == Status.Mismatch) 1 else 0
          (statusPriority, -e.confidence)
        }
        .headOption
    }
