package lila.llm.model

import chess.{ Square, Role, Color }

/**
 * Fact Scope: Defines the temporal or logical context of a fact.
 */
sealed trait FactScope
object FactScope {
  case object Now extends FactScope
  case class PV(lineId: String, plyOffset: Int) extends FactScope
  case object Hypothesis extends FactScope
}

/**
 * Fact: A verified, structured piece of chess evidence.
 * Facts are the only source of truth for the narrative engine.
 */
sealed trait Fact {
  def scope: FactScope
  def participants: List[Square]
}

object Fact {

  // --- Tactical Facts ---

  /** A piece that is attacked more than it is defended, or undefended. */
  case class HangingPiece(
      square: Square,
      role: Role,
      attackers: List[Square],
      defenders: List[Square],
      scope: FactScope
  ) extends Fact {
    def participants = square :: (attackers ++ defenders).distinct
  }

  /** A piece on a square that is a tactical target (even if not hanging). */
  case class TargetPiece(
      square: Square,
      role: Role,
      attackers: List[Square],
      defenders: List[Square],
      scope: FactScope
  ) extends Fact {
    def participants = square :: (attackers ++ defenders).distinct
  }

  /** A piece (pinned) that cannot move without exposing a more valuable piece (behind). */
  case class Pin(
      attacker: Square,
      attackerRole: Role,
      pinned: Square,
      pinnedRole: Role,
      behind: Square,
      behindRole: Role,
      isAbsolute: Boolean,
      scope: FactScope
  ) extends Fact {
    def participants = List(attacker, pinned, behind)
  }

  /** A piece (front) that must move, exposing another piece (back) to capture. */
  case class Skewer(
      attacker: Square,
      attackerRole: Role,
      front: Square,
      frontRole: Role,
      back: Square,
      backRole: Role,
      scope: FactScope
  ) extends Fact {
    def participants = List(attacker, front, back)
  }

  /** A single piece (attacker) that attacks multiple targets simultaneously. */
  case class Fork(
      attacker: Square,
      attackerRole: Role,
      targets: List[(Square, Role)],
      scope: FactScope
  ) extends Fact {
    def participants = attacker :: targets.map(_._1)
  }

  // --- Structural / Strategic Facts ---

  /** A square that is difficult to defend and can be exploited. */
  case class WeakSquare(
      square: Square,
      color: Color,
      reason: String, // e.g., "no pawn defense", "structural hole"
      scope: FactScope
  ) extends Fact {
    def participants = List(square)
  }

  /** A strong outpost for a piece. */
  case class Outpost(
      square: Square,
      role: Role,
      scope: FactScope
  ) extends Fact {
    def participants = List(square)
  }

  // --- Intent Facts ---

  /** Verifiable intent: A piece was moved to a square with a specific effect. */
  case class ActivatesPiece(
      role: Role,
      from: Square,
      to: Square,
      openedRay: Boolean = false,
      scope: FactScope
  ) extends Fact {
    def participants = List(from, to)
  }

  // --- Endgame Facts ---

  /** Metrics for king activity in the endgame. */
  case class KingActivity(
      square: Square,
      mobility: Int,
      proximityToCenter: Int,
      scope: FactScope
  ) extends Fact {
    def participants = List(square)
  }

  /** Relationship between kings in the endgame. */
  case class Opposition(
      king: Square,
      enemyKing: Square,
      distance: Int, // 1 for direct, 3/5 for distant
      isDirect: Boolean,
      scope: FactScope
  ) extends Fact {
    def participants = List(king, enemyKing)
  }

  /** Zugzwang - any move worsens the position. */
  case class Zugzwang(
      color: Color,
      scope: FactScope
  ) extends Fact {
    def participants = Nil
  }

  /** Pawn promotion or push towards it. */
  case class PawnPromotion(
      square: Square,
      promotedTo: Option[Role], // None if it's a push of a passed pawn
      scope: FactScope
  ) extends Fact {
    def participants = List(square)
  }

  /** Stalemate threat in a desperate position. */
  case class StalemateThreat(
      king: Square,
      scope: FactScope
  ) extends Fact {
    def participants = List(king)
  }

  /** Double check - two pieces checking at once. */
  case class DoubleCheck(
      checkingSquares: List[Square],
      scope: FactScope
  ) extends Fact {
    def participants = checkingSquares
  }
}
