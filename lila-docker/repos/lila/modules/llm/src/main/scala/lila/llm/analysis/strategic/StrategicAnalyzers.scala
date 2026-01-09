package lila.llm.analysis.strategic

import chess._
import chess.Bitboard
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.model.strategic.VariationLine

trait ProphylaxisAnalyzer {
  def analyze(
      board: Board,
      color: Color,
      mainLine: VariationLine,
      threatLine: Option[VariationLine],
      explicitPlanId: Option[String] = None
  ): List[PreventedPlan]
}

trait ActivityAnalyzer {
  def analyze(board: Board, color: Color): List[PieceActivity]
}

trait StructureAnalyzer {
  def analyze(board: Board): List[WeakComplex]
  def analyzeCompensation(board: Board, color: Color): Option[Compensation]
  def detectPositionalFeatures(board: Board, color: Color): List[PositionalTag]
}

trait EndgameAnalyzer {
  def analyze(board: Board, color: Color): Option[EndgameFeature]
}

trait PracticalityScorer {
  def score(
      board: Board,
      color: Color,
      engineScore: Int,
      variations: List[VariationLine],
      activity: List[PieceActivity],
      structure: List[WeakComplex],
      endgame: Option[EndgameFeature]
  ): PracticalAssessment
}

// Implementation
// Implementation

// Implementation
class ProphylaxisAnalyzerImpl extends ProphylaxisAnalyzer {
  def analyze(board: Board, color: Color, mainLine: VariationLine, threatLine: Option[VariationLine], explicitPlanId: Option[String] = None): List[PreventedPlan] = {
    threatLine.map { threat =>
       val scoreDiff = mainLine.effectiveScore - threat.effectiveScore
       
       if (scoreDiff > 50) {
         val threatMoveStr = threat.moves.mkString(" ")
         val internalPlanId = if (threatMoveStr.contains("#")) "Checkmate Threat"
                      else if (threatMoveStr.contains("x")) "Material Loss"
                      else if (threatMoveStr.contains("+")) "King Attack"
                      else if (scoreDiff > 300) "Decisive Advantage"
                      else "Positional Concession"
                      
         val finalPlanId = explicitPlanId.getOrElse(internalPlanId)
                      
         PreventedPlan(
           planId = finalPlanId, 
           deniedSquares = Nil,
           breakNeutralized = None,
           mobilityDelta = 0,
           counterplayScoreDrop = scoreDiff
         )
       } else null
    }.toList.filter(_ != null) 
  }
}

trait StrategicAnalyzer

class StrategicAnalyzers

class ActivityAnalyzerImpl extends ActivityAnalyzer {
  
  def analyze(board: Board, color: Color): List[PieceActivity] = {
    // Correct API: board.byColor(color) returns Bitboard, .squares returns List[Square]
    board.byColor(color).squares.flatMap { square =>
      board.pieceAt(square).map { piece =>
        val mobility = calculateMobility(board, piece, square)
        val isBadBishop = checkBadBishop(board, piece, square)
        
        // P1 FIX: Distinguish undeveloped from truly trapped
        // "Trapped" means: no safe moves AND under attack OR deeply embedded
        // Undeveloped piece on home rank with 0 mobility is NOT trapped, just undeveloped
        val isOnHomeRank = isHomeSquare(piece, square)
        val isUnderAttack = board.attackers(square, !color).nonEmpty
        
        // Only mark as trapped if:
        // 1. Not a pawn (pawns have natural limited mobility)
        // 2. No legal squares to move
        // 3. Either under attack OR not on home square (truly stuck, not just undeveloped)
        val isTrapped = piece.role != Pawn && mobility == 0 && (!isOnHomeRank || isUnderAttack)
        
        PieceActivity(
          piece = piece.role,
          square = square,
          mobilityScore = normalizeMobility(piece.role, mobility),
          isTrapped = isTrapped,
          isBadBishop = isBadBishop,
          keyRoutes = Nil, 
          coordinationLinks = Nil 
        )
      }
    }
  }
  
  // Check if piece is on its typical starting square
  private def isHomeSquare(piece: chess.Piece, square: chess.Square): Boolean = {
    val homeRank = if (piece.color.white) Rank.First else Rank.Eighth
    val pawnHomeRank = if (piece.color.white) Rank.Second else Rank.Seventh
    
    piece.role match {
      case Pawn => square.rank == pawnHomeRank
      case King => false  // King safety is handled separately
      case _ => square.rank == homeRank  // Rooks, Knights, Bishops, Queens
    }
  }

  private def calculateMobility(board: Board, piece: chess.Piece, square: chess.Square): Int = {
    val occupied = board.occupied
    val targets = piece.role match {
      case Pawn => 
        // Forward moves
        val direction = if (piece.color.white) 1 else -1
        val forward1 = Square.at(square.file.value, square.rank.value + direction)
        val f1 = forward1.filterNot(sq => board.pieceAt(sq).isDefined).map(sq => sq.file.bb & sq.rank.bb).getOrElse(Bitboard.empty)
        
        val f2 = if (f1.nonEmpty && isHomeSquare(piece, square)) {
          val forward2 = Square.at(square.file.value, square.rank.value + (2 * direction))
          forward2.filterNot(sq => board.pieceAt(sq).isDefined).map(sq => sq.file.bb & sq.rank.bb).getOrElse(Bitboard.empty)
        } else Bitboard.empty
        
        // Captures
        val captures = square.pawnAttacks(piece.color) & board.byColor(!piece.color)
        f1 | f2 | captures
        
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook => square.rookAttacks(occupied)
      case Queen => square.queenAttacks(occupied)
      case King => square.kingAttacks
    }
    // Filter out own pieces (us)
    val us = board.byColor(piece.color)
    (targets & ~us).count
  }

  private def normalizeMobility(role: Role, count: Int): Double = role match {
    case Pawn => count / 4.0 
    case Knight => count / 8.0
    case Bishop => count / 13.0
    case Rook => count / 14.0
    case Queen => count / 27.0
    case King => count / 8.0
  }

  private def checkBadBishop(board: Board, piece: chess.Piece, square: chess.Square): Boolean = {
    if (piece.role != Bishop) return false
    val isLightSquareBishop = square.isLight
    
    // Check central pawns (C, D, E, F files)
    // board.byPiece returns Bitboard.
    val myPawns = board.byPiece(piece.color, Pawn)
    
    val centerFiles = List(File.C, File.D, File.E, File.F)
    val pawnsOnCenter = centerFiles.foldLeft(Bitboard.empty) { (acc, file) =>
      acc | (myPawns & Bitboard.file(file))
    }
    
    // Count how many are on same color complex
    pawnsOnCenter.squares.count(s => s.isLight == isLightSquareBishop) >= 2
  }
}

class StructureAnalyzerImpl extends StructureAnalyzer {
  
  def analyze(board: Board): List[WeakComplex] = {
    val structures = for {
      color <- List(Color.White, Color.Black)
      cause <- List("Holes", "Doubled Pawns", "Backward Pawn", "Hanging Pawns")
    } yield (color, cause)

    structures.flatMap { case (color, cause) =>
      val squares = cause match {
        case "Holes" => detectHoles(board, color)
        case "Doubled Pawns" => detectDoubled(board, color)
        case "Backward Pawn" => detectBackward(board, color)
        case "Hanging Pawns" => detectHanging(board, color)
        case _ => Nil
      }
      if (squares.nonEmpty) Some(WeakComplex(color, squares, false, cause)) else None
    }
  }

  def analyzeCompensation(board: Board, color: Color): Option[Compensation] = {
    val us = board.byColor(color)
    val them = board.byColor(!color)
    
    val matUs = countMaterial(board, us)
    val matThem = countMaterial(board, them)
    
    val deficiency = matThem - matUs
    if (deficiency > 0) {
      val vectors = scala.collection.mutable.Map[String, Double]()
      
      // 1. Bishop Pair
      val bishops = board.byPiece(color, Bishop).count
      val enemyBishops = board.byPiece(!color, Bishop).count
      if (bishops >= 2 && enemyBishops < 2) vectors += ("Bishop Pair" -> 0.5)
      
      // 2. Space (Advanced Pawns)
      val rank5or4 = if (color.white) Rank.Fifth else Rank.Fourth
      val spaceCount = (board.pawns & Bitboard.rank(rank5or4)).count
      if (spaceCount > 0) vectors += ("Space Advantage" -> (0.3 * spaceCount))
      
      // 3. King Safety (Enemy King Exposed)
      board.kingPosOf(!color).foreach { kSq =>
        val zone = kSq.kingAttacks
        val enemyDefenders = (board.byColor(!color) & zone).count
        val ourAttackers = board.attackers(kSq, color).count
        if (ourAttackers > enemyDefenders) vectors += ("Attack on King" -> (ourAttackers - enemyDefenders) * 0.5)
        
        val enemyIsWhite = !color.white
        val frontRank = kSq.rank.value + (if (enemyIsWhite) 1 else -1)
        val frontSq = Square.at(kSq.file.value, frontRank)
        
        if (frontSq.exists(s => !board.pawns.contains(s))) vectors += ("Exposed King" -> 0.5)
      }
      
      val totalComp = vectors.values.sum
      if (totalComp > 0.5) Some(Compensation(
        investedMaterial = deficiency,
        returnVector = vectors.toMap,
        expiryPly = None,
        conversionPlan = if (vectors.contains("Attack on King")) "Mating Attack" else "Positional Compensation"
      )) else None
    } else None
  }
  
  private def countMaterial(board: Board, pieces: Bitboard): Int = {
    pieces.squares.foldLeft(0) { (acc, sq) =>
      acc + board.roleAt(sq).map(pieceValue).getOrElse(0)
    }
  }

  private def detectDoubled(board: Board, color: Color): List[chess.Square] = {
    val pawns = board.byPiece(color, Pawn)
    val files = pawns.squares.groupBy(_.file).filter(_._2.size > 1)
    files.values.flatten.toList.sortBy(_.value)
  }

  private def detectBackward(board: Board, color: Color): List[chess.Square] = {
    val pawns = board.byPiece(color, Pawn).squares
    val pawnsByFile = pawns.groupBy(_.file)
    val direction = if (color.white) 1 else -1

    pawns.filter { pawn =>
      val file = pawn.file
      val rank = pawn.rank.value
      val adjFiles = List(file.value - 1, file.value + 1).flatMap(File.all.lift)
      
      // No friendly pawn on adjacent files is further back or same rank (supporting candidate)
      // Actually strictly: No friendly pawn on adjacent files checks the stop square.
      // Simplified: "Backward" if no adjacent friends behind it or equal.
      val supports = adjFiles.exists { f =>
        pawnsByFile.get(f).exists(_.exists { neighbor =>
          // Neighbor must be further back or same rank (if unblocked)
          // Classic definition: Behind neighbors, and stop square controlled by enemy.
          if (color.white) neighbor.rank.value <= rank else neighbor.rank.value >= rank
        })
      }
      
      // Also stop square (immediate forward) must be controlled by enemy pawn
      // This requires calculation. For "Super Optimization", we do it.
      val stopSquare = Square.at(file.value, rank + direction)
      val stopControlled = stopSquare.exists { s => 
        board.attackers(s, !color).intersects(board.byPiece(!color, Pawn)) 
      }

      !supports && stopControlled
    }
  }

  private def detectHanging(board: Board, color: Color): List[chess.Square] = {
     val pawns = board.byPiece(color, Pawn).squares
     val pawnsByFile = pawns.groupBy(_.file)
     
     // Couple: 2 adjacent files have pawns.
     // Hanging: They are isolated from OTHER pawns.
     // Logic: Find couples implies looking for (File, File+1).
     val couples = pawns.filter { p =>
       val f = p.file.value
       val neighbors = List(f-1, f+1)
       // Has NO support from file-2 or file+2 (Concept of "Hanging Couple" implies isolation)
       // This is complex. We'll simplify: Hanging = Semi-Open adjacent files + No Support.
       // Actually simpler: "Hanging Pawns" usually refers to c+d pawns typically isolated from b and e.
       // Implementation: Pawn has neighbor on ONE side, but NO neighbor on the OTHER side.
       // And the neighbor also shares this property.
       
       val left = File.all.lift(f - 1)
       val right = File.all.lift(f + 1)
       val hasLeft = left.exists(pawnsByFile.contains)
       val hasRight = right.exists(pawnsByFile.contains)
       
       // Hanging couple member: Has exactly one neighbor pawn (left or right), and THAT neighbor has no other neighbor.
       // e.g. p at D. Neighbor at C. D has no E. C has no B. -> C+D hanging.
       
       val _isolatedFromSide = if (hasLeft) !right.exists(pawnsByFile.contains) else !left.exists(pawnsByFile.contains)
       val isCouple = hasLeft ^ hasRight // XOR: Only one neighbor.
       
       if (isCouple) {
         val partnerFile = if (hasLeft) left.get else right.get
         val partnerHasOther = if (hasLeft) {
           // Partner is Left. Check Left's Left.
           File.all.lift(partnerFile.value - 1).exists(pawnsByFile.contains)
         } else {
           // Partner is Right. Check Right's Right.
           File.all.lift(partnerFile.value + 1).exists(pawnsByFile.contains)
         }
         !partnerHasOther
       } else false
     }
     
     // Also implies strictly no friendly pawns on adjacent files? No, we just checked that.
     // "Hanging" also implies open lines. If files open?
     // We assume if they exist, they are weak.
     couples
  }

  private def detectHoles(board: Board, color: Color): List[chess.Square] = {
    val ranks = if (color.white) List(Rank.Third, Rank.Fourth) else List(Rank.Sixth, Rank.Fifth)
    val relevantSquares = Square.all.filter(s => ranks.contains(s.rank))
    
    val friendlyPawns = board.byPiece(color, Pawn)
    if (friendlyPawns.isEmpty) return Nil  // No pawn structure => no meaningful "holes"
    
    val homePawnRank = if (color.white) Rank.Second else Rank.Seventh
    val pawnsOnHomeRank = friendlyPawns.squares.count(_.rank == homePawnRank)
    val totalPawns = board.pawns.count
    
    // P1 FIX: No meaningful holes if too few pawns remain (Endgame noise) or in starting position
    if (totalPawns <= 2 || pawnsOnHomeRank >= 6) return Nil
    
    var attacks = Bitboard.empty
    friendlyPawns.squares.foreach { sq =>
       attacks = attacks | sq.pawnAttacks(color)
    }
    
    val rawHoles = relevantSquares.filter(s => !attacks.contains(s))
    
    // Prioritize central squares (c, d, e, f files) and limit to max 4
    val centerFiles = Set(2, 3, 4, 5) // c, d, e, f
    val sortedHoles = rawHoles.sortBy { sq =>
      val centralityScore = if (centerFiles.contains(sq.file.value)) 0 else 1
      val advancementScore = if (color.white) -sq.rank.value else sq.rank.value
      (centralityScore, advancementScore)
    }
    
    sortedHoles.take(4) // Return at most 4 holes
  }

  // NEW: Detect Outposts and Open Files
  def detectPositionalFeatures(board: Board, color: Color): List[PositionalTag] = {
    val features = scala.collection.mutable.ListBuffer[PositionalTag]()
    
    // 1. Outposts: Holes occupied by Knight or Bishop
    val holes = detectHoles(board, !color) // Enemy's holes = our outpost squares
    val ourPieces = board.byColor(color)
    holes.foreach { sq =>
      if (ourPieces.contains(sq)) {
        board.roleAt(sq).foreach {
          case Knight | Bishop => features += PositionalTag.Outpost(sq, color)
          case _ => // Only minor pieces count as outposts
        }
      }
    }
    
    // 2. Open Files: No pawns on file + we have Rook/Queen on it
    File.all.foreach { file =>
      val pawnsOnFile = board.pawns & Bitboard.file(file)
      if (pawnsOnFile.isEmpty) {
        // Check if we have a heavy piece on this file
        val ourHeavy = (board.byPiece(color, Rook) | board.byPiece(color, Queen)) & Bitboard.file(file)
        if (ourHeavy.nonEmpty) {
          features += PositionalTag.OpenFile(file, color)
        }
      }
    }
    
    // 3. Loose Pieces: Attacked but not defended (excluding King)
    board.byColor(color).squares.foreach { sq =>
      board.roleAt(sq).foreach { role =>
        if (role != King) {
          // Check if square is attacked by enemy
          val attacked = isAttackedBy(board, sq, !color)
          // Check if square is defended by us
          val defended = isDefendedBy(board, sq, color)
          if (attacked && !defended) {
            features += PositionalTag.LoosePiece(sq, role, color)
          }
        }
      }
    }
    
    // 4. Rook on 7th Rank (2nd for Black) - now as its own tag
    val seventhRank = if (color.white) Rank.Seventh else Rank.Second
    board.byPiece(color, Rook).squares.foreach { sq =>
      if (sq.rank == seventhRank) {
        features += PositionalTag.RookOnSeventh(color)
        features += PositionalTag.WeakBackRank(!color) // Opponent's back rank is weak
      }
    }
    
    // 5. Bishop Pair Advantage
    val ourBishops = board.bishops & board.byColor(color)
    val theirBishops = board.bishops & board.byColor(!color)
    if (ourBishops.count >= 2 && theirBishops.count < 2) {
      features += PositionalTag.BishopPairAdvantage(color)
    }
    
    // 6. Bad/Good Bishop detection
    ourBishops.squares.foreach { bSq =>
      val isLightSquare = bSq.isLight
      val ourPawns = board.pawns & board.byColor(color)
      val centerPawns = ourPawns.squares.filter(p => p.file.value >= 2 && p.file.value <= 5) // C-F files
      val sameColorPawns = centerPawns.count(_.isLight == isLightSquare)
      
      if (centerPawns.nonEmpty) {
        if (sameColorPawns >= (centerPawns.size / 2) + 1) {
          features += PositionalTag.BadBishop(color)
        } else if (sameColorPawns == 0) {
          features += PositionalTag.GoodBishop(color)
        }
      }
    }
    
    // 7. Strong Knight (knight on advanced protected square OR deep in enemy territory)
    val knightHoles = detectHoles(board, !color)
    board.byPiece(color, Knight).squares.foreach { sq =>
      val isOnHole = knightHoles.contains(sq)
      val pawnSupport = (sq.pawnAttacks(!color) & board.byPiece(color, Pawn)).nonEmpty
      
      // Phase 12: "Octopus Knight" - deep in enemy territory (rank 5+ for White, rank 4- for Black)
      val isDeepInEnemyTerritory = if (color.white) sq.rank.value >= 5 else sq.rank.value <= 2
      val isAdvancedAssault = if (color.white) sq.rank.value >= 4 else sq.rank.value <= 3
      
      // StrongKnight if: (on a hole && pawn-supported) OR (deep in enemy territory)
      if ((isOnHole && pawnSupport) || (isDeepInEnemyTerritory && isAdvancedAssault)) {
        features += PositionalTag.StrongKnight(sq, color)
      }
    }
    
    // 8. Weak Squares (holes not yet occupied)
    knightHoles.filterNot(sq => board.byColor(color).contains(sq)).take(2).foreach { sq =>
      features += PositionalTag.WeakSquare(sq, !color)
    }
    
    // 9. Space Advantage (Central pawn presence beyond 4th rank)
    val ourPawns = board.pawns & board.byColor(color)
    val centerFiles = Set(2, 3, 4, 5) // c, d, e, f
    val advancedCenterPawns = ourPawns.squares.count { p =>
      centerFiles.contains(p.file.value) && (
        if (color.white) p.rank.value >= 4 
        else p.rank.value <= 3
      )
    }
    if (advancedCenterPawns >= 2) {
      features += PositionalTag.SpaceAdvantage(color)
    }
    
    // 10. Opposite Color Bishops
    if (ourBishops.count == 1 && theirBishops.count == 1) {
      val ourLight = ourBishops.squares.head.isLight
      val theirLight = theirBishops.squares.head.isLight
      if (ourLight != theirLight) {
        features += PositionalTag.OppositeColorBishops
      }
    }
    
    // 11. King Stuck in Center (not castled, pieces still on board)
    if (board.occupied.count >= 16) { // Middlegame/Endgame transition
      board.kingPosOf(color).foreach { kSq =>
        val centerFiles = Set(3, 4) // D and E files
        if (centerFiles.contains(kSq.file.value)) {
          // Phase 15: Deep Logic Correction
          // 2. Must be EXPOSED (no pawn shield on the file)
          // If there is a friendly pawn on the file, it's not "stuck/exposed" in a dangerous way.
          val filePawns = (board.pawns & board.byColor(color)).squares.exists(_.file == kSq.file)
          
          if (!filePawns) {
            features += PositionalTag.KingStuckCenter(color)
          }
        }
      }
    }
    
    // 12. Connected Rooks (on same rank)
    val ourRooks = board.byPiece(color, Rook).squares
    if (ourRooks.size == 2) {
      val r1 = ourRooks.head
      val r2 = ourRooks.last
      if (r1.rank == r2.rank) {
        // Check if no pieces between them using simple distance check
        val minFile = math.min(r1.file.value, r2.file.value)
        val maxFile = math.max(r1.file.value, r2.file.value)
        // Simplified: check if rooks are adjacent or have clear path
        val blocked = (minFile + 1 until maxFile).exists { fileVal =>
          // Check each square between them
          File.all.find(_.value == fileVal).exists { f =>
            Rank.all.find(_.value == r1.rank.value).exists { r =>
              board.occupied.contains(Square(f, r))
            }
          }
        }
        if (!blocked) {
          features += PositionalTag.ConnectedRooks(color)
        }
      }
      // 13. Doubled Rooks on same file
      if (r1.file == r2.file) {
        features += PositionalTag.DoubledRooks(r1.file, color)
      }
    }
    
    // Phase 11: 14. Color Complex Weakness (cluster of weak squares on same color)
    val colorHoles = detectHoles(board, color)
    val lightHoles = colorHoles.filter(_.isLight)
    val darkHoles = colorHoles.filterNot(_.isLight)
    if (lightHoles.size >= 3) {
      features += PositionalTag.ColorComplexWeakness(color, "light", lightHoles.take(4))
    }
    if (darkHoles.size >= 3) {
      features += PositionalTag.ColorComplexWeakness(color, "dark", darkHoles.take(4))
    }
    
    // Phase 11: 15. Pawn Majority (queenside files a-d vs kingside files e-h)
    val ourPawnsList = (board.pawns & board.byColor(color)).squares
    val theirPawnsList = (board.pawns & board.byColor(!color)).squares
    val ourQueenside = ourPawnsList.count(_.file.value <= 3)  // a-d
    val theirQueenside = theirPawnsList.count(_.file.value <= 3)
    val ourKingside = ourPawnsList.count(_.file.value >= 4)   // e-h
    val theirKingside = theirPawnsList.count(_.file.value >= 4)
    
    if (ourQueenside > theirQueenside + 1) {
      features += PositionalTag.PawnMajority(color, "queenside", ourQueenside - theirQueenside)
    }
    if (ourKingside > theirKingside + 1) {
      features += PositionalTag.PawnMajority(color, "kingside", ourKingside - theirKingside)
    }

    // Phase 24: Minority Attack (The inverse of majority - fewer pawns attacking a chain)
    // Heuristic: We have fewer pawns (but > 0), they have majority, and we have open lines.
    // Carlson Structure / Carlsbad often has White minority attack on Q-side (2 vs 3).
    if (ourQueenside > 0 && ourQueenside < theirQueenside && theirQueenside >= 2) {
      // Check if we are advancing or have semi-open files
      val semiOpen = ourQueenside < 4 // implied by having fewer pawns than files?
      // Simple heuristic: 2 vs 3 or 1 vs 2 on queenside is a classic minority attack setup
      features += PositionalTag.MinorityAttack(color, "queenside")
    }
    if (ourKingside > 0 && ourKingside < theirKingside && theirKingside >= 2) {
      features += PositionalTag.MinorityAttack(color, "kingside")
    }
    
    features.toList.distinct
  }
  
  // Helper: Is square attacked by given color?
  private def isAttackedBy(board: Board, sq: Square, by: Color): Boolean = {
    // Check pawn attacks
    val pawnAttacks = board.byPiece(by, Pawn).squares.exists(p => p.pawnAttacks(by).contains(sq))
    if (pawnAttacks) return true
    
    // Check knight attacks
    val knightAttacks = board.byPiece(by, Knight).squares.exists(n => n.knightAttacks.contains(sq))
    if (knightAttacks) return true
    
    // Check sliding pieces (simplified - doesn't account for blockers perfectly)
    val occupied = board.occupied
    val bishopStyle = board.byPiece(by, Bishop).squares.exists(b => b.bishopAttacks(occupied).contains(sq))
    val rookStyle = board.byPiece(by, Rook).squares.exists(r => r.rookAttacks(occupied).contains(sq))
    val queenAttacks = board.byPiece(by, Queen).squares.exists(q => q.queenAttacks(occupied).contains(sq))
    val kingAttacks = board.byPiece(by, King).squares.exists(k => k.kingAttacks.contains(sq))
    
    bishopStyle || rookStyle || queenAttacks || kingAttacks
  }
  
  // Helper: Is square defended by given color?
  private def isDefendedBy(board: Board, sq: Square, by: Color): Boolean = {
    isAttackedBy(board, sq, by)
  }

  private def pieceValue(role: Role): Int = role match {
    case Pawn => 100
    case Knight => 300
    case Bishop => 300
    case Rook => 500
    case Queen => 900
    case King => 0
  }
}

class EndgameAnalyzerImpl extends EndgameAnalyzer {
  
  def analyze(board: Board, color: Color): Option[EndgameFeature] = {
    if (board.occupied.count > 10) return None
    
    val wKing = board.kingPosOf(Color.White)
    val bKing = board.kingPosOf(Color.Black)
    
    val hasOpposition = (wKing, bKing) match {
      case (Some(wk), Some(bk)) =>
        val fDiff = (wk.file.value - bk.file.value).abs
        val rDiff = (wk.rank.value - bk.rank.value).abs
        (fDiff == 0 && rDiff == 2) || (rDiff == 0 && fDiff == 2)
      case _ => false
    }

    val advancedPawns = board.byPiece(color, Pawn).squares.filter { s =>
      if (color.white) s.rank.value >= 5 else s.rank.value <= 2
    }
    val promotionSquares = advancedPawns.flatMap { s => 
       Square.at(s.file.value, if (color.white) 7 else 0)
    }
    
    val controlledKeys = promotionSquares.filter { key =>
      board.attackers(key, color).nonEmpty
    }

    // Heuristic: Zugzwang requires legal move generation which is currently inaccessible via Board
    val isZugzwang = false

    if (hasOpposition || controlledKeys.nonEmpty || isZugzwang) Some(EndgameFeature(
      hasOpposition = hasOpposition,
      isZugzwang = isZugzwang, 
      keySquaresControlled = controlledKeys 
    )) else None
  }
}

class PracticalityScorerImpl extends PracticalityScorer {
  def score(
      board: Board,
      color: Color,
      engineScore: Int,
      variations: List[VariationLine],
      activity: List[PieceActivity],
      structure: List[WeakComplex],
      endgame: Option[EndgameFeature]
  ): PracticalAssessment = {

    val (whiteActivity, blackActivity) = activity.partition(a => board.colorAt(a.square).contains(Color.White))
    val whiteMobilitySum = whiteActivity.map(_.mobilityScore).sum
    val blackMobilitySum = blackActivity.map(_.mobilityScore).sum
    val mobilityDiff = if (color == Color.White) whiteMobilitySum - blackMobilitySum else blackMobilitySum - whiteMobilitySum
    val mobilityScore = mobilityDiff * 20.0

    val bestScore = variations.headOption.map(_.effectiveScore).getOrElse(0)
    val safeMargin = 50
    val safeMoveCount = variations.count(v => (bestScore - v.effectiveScore).abs <= safeMargin)
    val forgivenessIndex = math.min(1.0, (safeMoveCount - 1) / 4.0)
    
    // P1 FIX: Early opening phase threshold (starts with 32)
    val isOpening = board.occupied.count >= 28
    val isStartingPosition = board.occupied.count >= 30
    
    val forgivenessPenalty = if (isStartingPosition) 0.0 else (1.0 - forgivenessIndex) * -50.0

    val myWeaknesses = structure.filter(_.color == color)
    val myWeaknessPenalty = myWeaknesses.map { w =>
       w.cause match {
         case "Holes" => -20.0
         case "Doubled Pawns" => -15.0
         case "Backward Pawn" => -25.0
         case "Hanging Pawns" => -10.0
         case _ => -10.0
       }
    }.sum
    val badBishopPenalty = activity.collect { 
      case a if a.isBadBishop && board.colorAt(a.square).contains(color) && !isOpening => -30.0 
    }.sum

    var practicalScoreTerm = engineScore.toDouble + mobilityScore + forgivenessPenalty + badBishopPenalty + myWeaknessPenalty

    // P1 FIX: Higher noise floor for "Under Pressure" in early opening
    val pressureThreshold = if (isOpening) -150.0 else -50.0
    val advantageThreshold = if (isOpening) 150.0 else 50.0

    endgame.foreach { eg =>
      if (eg.hasOpposition && engineScore.abs < 50) {
        practicalScoreTerm += 20.0
      }
    }

    // P1 FIX: Align verdict with absolute practical score, not just the delta
    // Thresholds: [-inf, -30) = Under Pressure, [-30, 30] = Balanced, (30, inf] = Comfortable
    val finalScore = practicalScoreTerm
    val verdict = if (finalScore > 30.0) "Comfortable"
                  else if (finalScore < -30.0) "Under Pressure"
                  else "Balanced"

    val biasFactors = List(
      BiasFactor("Mobility", f"Diff: $mobilityDiff%.1f", mobilityScore),
      BiasFactor("Forgiveness", f"$safeMoveCount safe moves", forgivenessPenalty),
      BiasFactor("Structure", "Bad Bishop/Weakness", badBishopPenalty + myWeaknessPenalty)
    ).filter(_.weight.abs > 5.0)

    PracticalAssessment(
      engineScore = engineScore,
      practicalScore = practicalScoreTerm,
      biasFactors = biasFactors,
      verdict = verdict
    )
  }
}
