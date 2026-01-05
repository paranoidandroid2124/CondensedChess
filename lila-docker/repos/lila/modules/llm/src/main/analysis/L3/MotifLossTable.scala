package lila.llm.analysis.L3

/**
 * Motif to Loss Mapping Table
 * 
 * Maps L2 tactical motifs to estimated centipawn loss if the threat is ignored.
 * These are baseline values that get corrected by MultiPV analysis.
 */
object MotifLossTable:

  /**
   * Get baseline loss for a motif type.
   * Returns (baseLoss, turnsToImpact)
   */
  def getBaseLoss(motifType: String): (Int, Int) = motifType.toLowerCase match
    // Mate threats - highest priority
    case s if s.contains("mate") || s.contains("checkmate") => (10000, 1)
    case "backrankweakness" | "backrank" => (800, 2)
    
    // Major piece threats
    case "fork" => (300, 1)  // Assumes major piece involved
    case "skewer" => (400, 1)
    case "discoveredattack" | "discovered" => (300, 1)
    
    // Pin-based threats
    case "pin" => (200, 1)  // Depends on pinned piece
    case "absolutepin" => (300, 1)
    
    // Loose/hanging pieces
    case s if s.contains("loose") && s.contains("queen") => (900, 1)
    case s if s.contains("loose") && s.contains("rook") => (500, 1)
    case s if s.contains("loose") => (300, 1)
    case s if s.contains("hanging") => (300, 1)
    
    // Deflection/interference
    case "deflection" => (250, 1)
    case "overloading" | "overloaded" => (200, 2)
    
    // Pawn threats
    case "pawnfork" => (150, 1)
    case "passedpawn" => (100, 3)  // Strategic, not immediate
    
    // Default for unknown motifs
    case _ => (100, 2)

  /**
   * Piece value for calculating material threats.
   */
  def pieceValue(piece: String): Int = piece.toLowerCase match
    case "q" | "queen" => 900
    case "r" | "rook" => 500
    case "b" | "bishop" => 330
    case "n" | "knight" => 320
    case "p" | "pawn" => 100
    case _ => 100

  /**
   * Check if a motif indicates a mate threat.
   */
  def isMateMotif(motifType: String): Boolean =
    val lower = motifType.toLowerCase
    lower.contains("mate") || lower.contains("checkmate") || lower == "backrankweakness"

  /**
   * Check if a motif is tactical (immediate) vs positional (strategic).
   */
  def isTacticalMotif(motifType: String): Boolean =
    val lower = motifType.toLowerCase
    List("fork", "pin", "skewer", "discovered", "deflection", "loose", "hanging", "mate", "check")
      .exists(lower.contains)
