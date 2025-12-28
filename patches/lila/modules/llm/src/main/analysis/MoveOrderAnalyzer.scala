package lila.llm

import lila.llm.model.*

import _root_.chess.*
import _root_.chess.format.{ Fen, Uci }

/**
 * Move Order Analyzer
 * 
 * Specifically designed to detect "Move Order Sensitivity".
 * It checks if flipping the order of moves (A, B) vs (B, A) leads to 
 * tactical catastrophes like losing a piece that was previously protected.
 */
object MoveOrderAnalyzer:

  case class MoveOrderEvidence(
      originalOrder: List[String],
      flippedOrder: List[String],
      issueType: String, // "broken_protection" | "missed_intermezzo" | "tactical_failure" | "transposition"
      description: String,
      affectedSquare: Option[Square] = None,
      affectedPiece: Option[Role] = None,
      isTransposition: Boolean = false
  )

  /**
   * Analyzes a sequence of two moves, taking existing Multi-PV lines into account.
   */
  def analyzeWithVariations(
      fen: String,
      moveA: String, // User move (mistake)
      moveB: String, // Engine best
      existingVariations: List[VariationLine]
  ): Option[MoveOrderEvidence] =
    // 1. Check if (moveB, moveA) already exists in Multi-PV
    val existingFlipped = existingVariations.find { v => 
      v.moves.take(2) == List(moveB, moveA)
    }

    // 2. Even if it exists or not, we run the tactical 'Broken Protection' check
    // to find the EVIDENCE for the LLM.
    analyzePairing(fen, moveA, moveB) match
      case Some(evidence) => Some(evidence)
      case None if existingFlipped.isDefined => 
        // If engine says it's different but we didn't find a simple broken protection,
        // we can flag a generic tactical failure.
        Some(MoveOrderEvidence(
          originalOrder = List(moveB, moveA),
          flippedOrder = List(moveA, moveB),
          issueType = "tactical_failure",
          description = "the engine confirms this order is significantly weaker, likely due to tactical nuances."
        ))
      case _ => None

  /**
   * Analyzes a sequence of two moves to see if their order is critical.
   */
  def analyzePairing(
      fen: String,
      moveA: String,
      moveB: String
  ): Option[MoveOrderEvidence] =
    val posA = Fen.read(chess.variant.Standard, Fen.Full(fen))
    
    posA.flatMap { pos =>
      for
        afterA <- uciMove(pos, moveA)
        afterAB <- uciMove(afterA, moveB)
        afterB <- uciMove(pos, moveB)
        afterBA <- uciMove(afterB, moveA)
      yield
        if (afterAB.board.occupied == afterBA.board.occupied && 
            afterAB.board.pieces == afterBA.board.pieces) then
          Some(MoveOrderEvidence(
            originalOrder = List(moveA, moveB),
            flippedOrder = List(moveB, moveA),
            issueType = "transposition",
            description = "both move orders lead to the exact same position (Transposition).",
            isTransposition = true
          ))
        else
          detectBrokenProtection(pos, moveA, moveB, afterA, afterB)
            .orElse(detectIntermezzo(pos, moveA, moveB, afterA, afterB))
    }.flatten

  private def detectBrokenProtection(
      initial: Position,
      moveA: String,
      moveB: String,
      afterA: Position,
      afterB: Position
  ): Option[MoveOrderEvidence] =
    val color = initial.color
    
    // Find a piece that move B interacts with (either moveB's destination or a capture)
    // which is defended in 'afterB' but NOT defended in 'afterBA' (where A happened first)
    
    // Check the destination of move B to see if its defense was broken
    Uci(moveB).collect { case m: Uci.Move => m }.flatMap { uciB =>
      val destB = uciB.dest
      // "Defenders" = pieces of our color that can attack (and thus defend) destB
      // Note: board.attackers(sq, color) returns all pieces of 'color' that attack 'sq'
      val defendersBefore = initial.board.attackers(destB, color)
      val defendersAfterA = afterA.board.attackers(destB, color)
      
      if defendersBefore.nonEmpty && defendersAfterA.isEmpty then
        Some(MoveOrderEvidence(
          originalOrder = List(moveB, moveA),
          flippedOrder = List(moveA, moveB),
          issueType = "broken_protection",
          description = s"move $moveA removes the defender of $destB",
          affectedSquare = Some(destB),
          affectedPiece = initial.board.roleAt(destB)
        ))
      else None
    }

  private def detectIntermezzo(
      initial: Position,
      moveA: String,
      moveB: String,
      afterA: Position,
      afterB: Position
  ): Option[MoveOrderEvidence] =
    // If move A is a forcing move (check) that changes the result of move B
    if initial.check.no && afterA.check.yes then
       Some(MoveOrderEvidence(
         originalOrder = List(moveA, moveB),
         flippedOrder = List(moveB, moveA),
         issueType = "missed_intermezzo",
         description = s"move $moveA is an intermezzo (check) that improves the sequence."
       ))
    else None

  private def uciMove(pos: Position, uciStr: String): Option[Position] =
    Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(pos.move(_).toOption).map(_.after)

  /**
   * Helper to describe why a move order is sensitive for the LLM.
   */
  def formatEvidence(ev: MoveOrderEvidence): String =
    s"MOVE ORDER SENSITIVITY: Found ${ev.issueType}. " +
    s"Playing ${ev.originalOrder.mkString(" then ")} is safe, but " +
    s"${ev.flippedOrder.mkString(" then ")} fails because ${ev.description}."
