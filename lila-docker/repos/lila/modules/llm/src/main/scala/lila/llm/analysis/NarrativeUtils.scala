package lila.llm.analysis

import chess.*
import chess.format.pgn.*
import chess.format.Uci

/**
 * Narrative Utility Functions
 * 
 * Helper methods for converting internal representations to human-readable text.
 */
object NarrativeUtils:

  /**
   * Converts a list of UCI move strings to SAN (Standard Algebraic Notation).
   */
  def uciListToSan(fen: String, uciMoves: List[String]): List[String] =
    val pgn = s"[Variant \"Standard\"]\n[FEN \"$fen\"]"
    Parser.mainline(PgnStr(pgn)) match
      case Right(parsed) =>
        var game = parsed.toGame
        uciMoves.flatMap { moveStr =>
          Uci(moveStr).flatMap { uci =>
            game(uci).toOption.map { case (newGame, move) =>
              val san = move.toSanStr.toString
              game = newGame
              san
            }
          }
        }
      case _ => uciMoves
  
  /**
   * Converts a list of UCI move strings to the final FEN string.
   */
  def uciListToFen(fen: String, uciMoves: List[String]): String =
    val variant = chess.variant.Standard
    var current = chess.format.Fen.read(variant, chess.format.Fen.Full(fen))
    uciMoves.foreach { moveStr =>
      current = current.flatMap { sit =>
        Uci(moveStr).flatMap {
          case u: Uci.Move => sit.move(u).toOption.map(_.after)
          case _ => None
        }
      }
    }
    current.map(sit => chess.format.Fen.write(sit).value).getOrElse(fen)
  
  /**
   * Converts a SAN string to a UCI string based on the given FEN.
   * Returns None if the move is illegal or cannot be parsed.
   */
  def sanToUci(fen: String, san: String): Option[String] =
    val pgn = s"[Variant \"Standard\"]\n[FEN \"$fen\"]\n\n$san"
    Parser.full(PgnStr(pgn)).toOption.flatMap { parsed =>
      Replay.makeReplay(parsed.toGame, parsed.mainline).replay.chronoMoves.lastOption.flatMap {
        case m: chess.Move => Some(m.toUci.uci)
        case d: chess.Drop => Some(d.toUci.uci)
        case _ => None
      }
    }


  /**
   * Converts a list of Square objects to algebraic notation string.
   * Example: List(Square.D5, Square.E5, Square.F5) -> "d5, e5, f5"
   */
  def squaresToAlgebraic(squares: List[Square]): String =
    squares.map(_.key).mkString(", ")

  /**
   * Converts square color string to readable phrase.
   * "light" -> "light-square"
   * "dark" -> "dark-square"
   */
  def colorComplexPhrase(squareColor: String): String =
    if squareColor == "light" then "light-square" else "dark-square"

  /**
   * Converts flank string to readable phrase.
   * "queenside" -> "queenside"
   * "kingside" -> "kingside"
   */
  def flankPhrase(flank: String): String =
    flank.toLowerCase match
      case "queenside" => "queenside"
      case "kingside" => "kingside"
      case other => other

  /**
   * Generates intent phrase for Exchange Sacrifice based on ROI reason.
   */
  def exchangeSacrificeIntent(reason: String): String =
    val lowerReason = reason.toLowerCase
    if lowerReason.contains("outpost") then "to dominate a key outpost"
    else if lowerReason.contains("file") then "to control the open file"
    else if lowerReason.contains("pawn") then "to create a passed pawn"
    else if lowerReason.contains("king") then "to expose the enemy king"
    else if lowerReason.contains("weak") then "to exploit weak squares"
    else "for long-term positional compensation"

  /**
   * Formats a PositionalTag into a readable string with algebraic coordinates.
   * e.g. Outpost(36, White) -> Outpost(e5, White)
   */
  def formatPositionalTag(tag: lila.llm.model.strategic.PositionalTag): String =
    import lila.llm.model.strategic.PositionalTag._
    tag match
      case Outpost(sq, c) => s"Outpost(${sq.key}, $c)"
      case OpenFile(f, c) => s"OpenFile(${f.char}, $c)"
      case WeakSquare(sq, c) => s"WeakSquare(${sq.key}, $c)"
      case LoosePiece(sq, _, c) => s"LoosePiece(${sq.key}, $c)"
      case WeakBackRank(c) => s"WeakBackRank($c)"
      case BishopPairAdvantage(c) => s"BishopPair($c)"
      case BadBishop(c) => s"BadBishop($c)"
      case GoodBishop(c) => s"GoodBishop($c)"
      case RookOnSeventh(c) => s"RookOnSeventh($c)"
      case StrongKnight(sq, c) => s"StrongKnight(${sq.key}, $c)"
      case SpaceAdvantage(c) => s"SpaceAdvantage($c)"
      case OppositeColorBishops => "OppositeColorBishops"
      case KingStuckCenter(c) => s"KingStuckCenter($c)"
      case ConnectedRooks(c) => s"ConnectedRooks($c)"
      case DoubledRooks(f, c) => s"DoubledRooks(${f.char}, $c)"
      case ColorComplexWeakness(c, sc, sqs) => s"ColorComplexWeakness($c, $sc, List(${squaresToAlgebraic(sqs)}))"
      case PawnMajority(c, f, n) => s"PawnMajority($c, $f, $n)"
      case other => other.toString

  /**
   * Performs physical board verification for a claimed threat.
   * Ensures the square is occupied by a piece of the victim color and is attacked.
   */
  def isVerifiedThreat(fen: String, sq: chess.Square, victimColor: chess.Color): Boolean =
    chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).exists { situation =>
      val board = situation.board
      val isOccupiedByVictim = board.pieceAt(sq).exists(_.color == victimColor)
      val isAttackedByOpponent = board.attackers(sq, !victimColor).nonEmpty
      isOccupiedByVictim && isAttackedByOpponent
    }

  // ============================================================
  // POSITION CHARACTERIZATION (Migrated from PositionCharacterizer)
  // ============================================================
  
  import lila.llm.model.PositionNature
  import lila.llm.model.NatureType

  def characterizePosition(pos: chess.Position): PositionNature =
    val baseTension = calculateTension(pos)
    val fluidity = calculateFluidity(pos.board)
    
    // Phase 12: Boost tension if check
    val isCheck = pos.check.yes
    val checkBoost = if (isCheck) 0.3 else 0.0
    val tension = Math.min(1.0, baseTension + checkBoost)
    
    val natureType = 
      if (tension > 0.5 || isCheck) NatureType.Chaos
      else if (tension > 0.3 || fluidity > 0.5) NatureType.Dynamic
      else if (fluidity < 0.3) NatureType.Static
      else NatureType.Transition

    PositionNature(
      natureType = natureType,
      tension = tension,
      stability = 1.0 - (tension * 0.5),
      description = deriveDescription(natureType, tension)
    )

  private def calculateTension(pos: chess.Position): Double =
    val board = pos.board
    val occupied = board.occupied
    
    var attackCount = 0
    board.foreach { (color, role, sq) =>
      val targets: chess.Bitboard = role match
        case chess.Pawn   => sq.pawnAttacks(color)
        case chess.Knight => sq.knightAttacks
        case chess.Bishop => sq.bishopAttacks(occupied)
        case chess.Rook   => sq.rookAttacks(occupied)
        case chess.Queen  => sq.queenAttacks(occupied)
        case chess.King   => sq.kingAttacks
      
      attackCount += (targets & board.byColor(!color)).count
    }

    val pieceCount = board.nbPieces
    if (pieceCount == 0) 0.0
    else Math.min(1.0, attackCount.toDouble / (pieceCount.toDouble * 1.5))

  private def calculateFluidity(board: chess.Board): Double =
    val pawns = board.pawns
    val totalPawns = pawns.count
    if (totalPawns == 0) return 1.0
    
    val centerFiles = List(chess.File.C, chess.File.D, chess.File.E, chess.File.F)
    val openFilesCount = centerFiles.count(f => (pawns & chess.Bitboard.file(f)).isEmpty)
    
    openFilesCount.toDouble / centerFiles.size

  private def deriveDescription(nt: NatureType, _tension: Double): String = nt match
    case NatureType.Static => "A solid, maneuvering position with a fixed structure."
    case NatureType.Dynamic => "A dynamic position with active piece play and open lines."
    case NatureType.Chaos => "A high-tension tactical battlefield."
    case NatureType.Transition => "A fluid position transitioning between structures."
