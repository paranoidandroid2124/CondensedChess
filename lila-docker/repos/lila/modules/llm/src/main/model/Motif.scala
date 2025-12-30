package lila.llm.model

import chess.*

/**
 * Atomic high-level observation from a move or position state.
 * Normalized to be color-agnostic where applicable using relative coordinates.
 * 
 * Categories:
 * - Pawn Motifs: PawnAdvance, PawnBreak, PawnPromotion, PassedPawnPush
 * - Piece Motifs: RookLift, Fianchetto, Outpost, Centralization, PieceLift
 * - King Motifs: KingStep, Castling
 * - Tactical Motifs: Check, Capture, Pin, Fork, Skewer, DiscoveredAttack, DoubleCheck
 * - Structural Motifs: DoubledPieces, Battery, IsolatedPawn, BackwardPawn, PassedPawn
 * - Endgame Motifs: Opposition, Zugzwang, Fortress
 */
sealed trait Motif:
  def plyIndex: Int
  def move: Option[String] // SAN representation if applicable
  def category: MotifCategory

enum MotifCategory:
  case Pawn, Piece, King, Tactical, Structural, Endgame

object Motif:

  /**
   * Normalizes rank based on side: 
   * White Rank 1-8 stays 1-8. 
   * Black Rank 1-8 becomes 8-1.
   */
  def relativeRank(rank: Int, color: Color): Int =
    if (color.white) rank else 9 - rank

  // ============================================================
  // PAWN MOTIFS
  // ============================================================

  /** Simple pawn advance (non-capture) */
  case class PawnAdvance(
      file: File,
      fromRank: Int,
      toRank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Pawn
    def relativeFrom: Int = relativeRank(fromRank, color)
    def relativeTo: Int = relativeRank(toRank, color)
    def isDoubleStep: Boolean = (relativeTo - relativeFrom).abs == 2

  /** Pawn break - pawn capture that opens a file or challenges pawn structure */
  case class PawnBreak(
      file: File,
      targetFile: File,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Pawn

  /** Pawn promotion */
  case class PawnPromotion(
      file: File,
      promotedTo: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Pawn
    def isUnderpromotion: Boolean = promotedTo != Queen

  /** Pushing a passed pawn toward promotion */
  case class PassedPawnPush(
      file: File,
      toRank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Pawn
    def relativeTo: Int = relativeRank(toRank, color)

  // ============================================================
  // PIECE MOTIFS
  // ============================================================

  /** Rook lift from back rank to 3rd/4th rank for attack */
  case class RookLift(
      file: File,
      fromRank: Int,
      toRank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Piece
    def relativeFrom: Int = relativeRank(fromRank, color)
    def relativeTo: Int = relativeRank(toRank, color)

  /** Bishop fianchetto (Bg2/Bb2 for White, Bg7/Bb7 for Black) */
  case class Fianchetto(
      side: FianchettoSide,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Piece

  enum FianchettoSide:
    case Kingside, Queenside

  /** Piece placed on a strong outpost (typically a knight on e5/d5) */
  case class Outpost(
      piece: Role,
      square: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Piece

  /** Piece moving to central squares (d4, d5, e4, e5) */
  case class Centralization(
      piece: Role,
      square: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Piece

  /** Generic piece lift (knight or bishop moving up the board for attack) */
  case class PieceLift(
      piece: Role,
      fromRank: Int,
      toRank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Piece

  // ============================================================
  // KING MOTIFS
  // ============================================================

  /** King movement with strategic purpose */
  case class KingStep(
      stepType: KingStepType,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.King

  enum KingStepType:
    case OffBackRank    // King leaves safety (risky or endgame activation)
    case ToCorner       // King retreats to corner for safety
    case Activation     // King marches toward center in endgame
    case Evasion        // King escapes from check or threat
    case Other

  /** Castling (short or long) */
  case class Castling(
      side: CastlingSide,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.King

  enum CastlingSide:
    case Kingside, Queenside

  // ============================================================
  // TACTICAL MOTIFS
  // ============================================================

  /** Check */
  case class Check(
      piece: Role,
      targetSquare: Square,
      checkType: CheckType,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  enum CheckType:
    case Normal, Discovered, Double, Mate, Smothered

  /** Capture (with sacrifice detection) */
  case class Capture(
      piece: Role,
      captured: Role,
      square: Square,
      captureType: CaptureType,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  enum CaptureType:
    case Normal, Sacrifice, Exchange, Recapture, Winning

  /** Pin - piece attacks through an enemy piece to a more valuable piece behind */
  case class Pin(
      pinningPiece: Role,
      pinnedPiece: Role,
      targetBehind: Role, // Usually King or Queen
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical
    def isAbsolutePin: Boolean = targetBehind == King

  /** Fork - piece attacks two or more pieces simultaneously */
  case class Fork(
      attackingPiece: Role,
      targets: List[Role],
      square: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical
    def isRoyalFork: Boolean = targets.contains(King) && targets.contains(Queen)

  /** Skewer - attack through a valuable piece to a less valuable piece behind */
  case class Skewer(
      attackingPiece: Role,
      frontPiece: Role,
      backPiece: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  /** Discovered attack - moving one piece reveals an attack from another */
  case class DiscoveredAttack(
      movingPiece: Role,
      attackingPiece: Role,
      target: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical
    def isDiscoveredCheck: Boolean = target == King

  /** Deflection - forcing a piece away from a critical defensive duty */
  case class Deflection(
      piece: Role,
      fromSquare: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  /** Decoy - luring a piece to a vulnerable square */
  case class Decoy(
      piece: Role,
      toSquare: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  /** Remove the Defender - capturing or deflecting a piece that protects another */
  case class RemoveDefender(
      defenderRole: Role,
      defenderSquare: Square,
      protectedRole: Role,
      protectedSquare: Square,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  /** Overloading - a piece is defending too many things at once */
  case class Overloading(
      overloadedPiece: Role,
      overloadedSquare: Square,
      duties: List[Square], // Squares the piece is trying to defend
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical
    def dutyCount: Int = duties.size

  /** Double Check - both the moving piece and revealed piece give check */
  case class DoubleCheck(
      movingPiece: Role,
      revealedPiece: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  /** Back Rank Mate threat or execution */
  case class BackRankMate(
      mateType: BackRankMateType,
      attackingPiece: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  enum BackRankMateType:
    case Threat, Execution

  /** Trapped Piece - a piece has no safe squares to escape */
  case class TrappedPiece(
      trappedRole: Role,
      trappedSquare: Square,
      color: Color, // Color of the trapped piece (opponent's color)
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical
    def isValuableTrap: Boolean = trappedRole == Queen || trappedRole == Rook

  /** Interference - blocking the coordination between enemy pieces */
  case class Interference(
      interferingPiece: Role,
      interferingSquare: Square,
      blockedPiece1: Role,
      blockedPiece2: Role,
      color: Color,
      plyIndex: Int,
      move: Option[String]
  ) extends Motif:
    val category = MotifCategory.Tactical

  // ============================================================
  // STRUCTURAL MOTIFS (Position State)
  // ============================================================

  /** Doubled pieces on a file or rank */
  case class DoubledPieces(
      role: Role,
      file: File,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  /** Battery - two pieces aligned on same line (e.g., Q+B on diagonal) */
  case class Battery(
      front: Role,
      back: Role,
      axis: BatteryAxis,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  enum BatteryAxis:
    case File, Rank, Diagonal

  /** Isolated pawn (no friendly pawns on adjacent files) */
  case class IsolatedPawn(
      file: File,
      rank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  /** Backward pawn (cannot be supported by other pawns) */
  case class BackwardPawn(
      file: File,
      rank: Int,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  /** Passed pawn (no enemy pawns can block or capture it) */
  case class PassedPawn(
      file: File,
      rank: Int,
      color: Color,
      isProtected: Boolean,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  /** Doubled pawns (two pawns on same file) */
  case class DoubledPawns(
      file: File,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  /** Pawn chain */
  case class PawnChain(
      baseFile: File,
      tipFile: File,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Structural

  // ============================================================
  // ENDGAME MOTIFS
  // ============================================================

  /** Opposition - kings face each other with odd number of squares between */
  case class Opposition(
      opponentKingSquare: Square,
      ownKingSquare: Square,
      oppType: OppositionType,
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Endgame

  enum OppositionType:
    case Direct, Distant, Diagonal

  /** Zugzwang - any move worsens the position */
  case class Zugzwang(
      color: Color, // Side in zugzwang
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Endgame

  /** Fortress - defensive setup that holds despite material deficit */
  case class Fortress(
      color: Color, // Defending side
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Endgame

  /** Stalemate threat */
  case class StalemateThreat(
      color: Color,
      plyIndex: Int,
      move: Option[String] = None
  ) extends Motif:
    val category = MotifCategory.Endgame
