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
   * Converts PascalCase or camelCase to lowercase spaced words.
   * e.g., "BadBishop" -> "bad bishop", "NarrowChoice" -> "narrow choice"
   */
  def humanize(s: String): String =
    s.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase

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
      }
    }

  /**
   * Parses a SAN snippet (may include move numbers like "7." / "7..." and punctuation)
   * into a list of UCI moves, validating legality step-by-step from the given FEN.
   *
   * Returns None if any SAN token cannot be applied legally.
   */
  def sanLineToUciList(fen: String, sanLine: String): Option[List[String]] =
    val raw = sanLine.trim
    if raw.isEmpty then Some(Nil)
    else
      val tokens =
        raw
          .split("\\s+")
          .toList
          .map(_.trim)
          .filter(_.nonEmpty)
          .filterNot(t => t.matches("^[0-9]+\\.{1,3}$")) // "7." or "7..."
          .filterNot(t => t == "..." || t == "…")
          .filterNot(t => t == "1-0" || t == "0-1" || t == "1/2-1/2" || t == "*")

      val ucis = scala.collection.mutable.ListBuffer.empty[String]
      var currentFen = fen
      var ok = true
      val it = tokens.iterator
      while (it.hasNext && ok) {
        val san = it.next()
        sanToUci(currentFen, san) match
          case None => ok = false
          case Some(uci) =>
            ucis += uci
            currentFen = uciListToFen(currentFen, List(uci))
      }
      Option.when(ok)(ucis.toList)

  /**
   * Converts a UCI string to SAN string based on the given FEN.
   * Returns None if the move is illegal or cannot be parsed.
   * 
   * This is the single source of truth for UCI→SAN conversion.
   */
  def uciToSan(fen: String, uciMove: String): Option[String] =
    for
      pos <- chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen))
      uci <- Uci(uciMove)
      move <- uci match
        case m: Uci.Move => pos.move(m).toOption
        case _: Uci.Drop => None
    yield move.toSanStr.toString

  /**
   * Best-effort UCI to SAN-like format without board context.
   * e2e4 → e4, e7e8q → e8=Q
   */
  def formatUciAsSan(uci: String): String =
    if (uci.length < 4) uci
    else
      val dest = uci.substring(2, 4)
      val promotion = if (uci.length > 4) s"=${uci(4).toUpper}" else ""
      s"$dest$promotion"

  /**
   * Normalize a UCI move token.
   * Handles optional "=" in promotions and trailing punctuation.
   *
   * Examples:
   * - "e2e4" -> "e2e4"
   * - "e7e8=Q" -> "e7e8q"
   * - "e7e8q+" -> "e7e8q"
   */
  def normalizeUciMove(move: String): String =
    val cleaned = Option(move).getOrElse("").trim.toLowerCase
      .replace("=", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("""\s+""", "")
    val pattern = """^([a-h][1-8])([a-h][1-8])([qrbn])?$""".r
    cleaned match
      case pattern(from, to, promo) => s"$from$to${Option(promo).getOrElse("")}"
      case _                        => cleaned

  /**
   * Compare two UCI moves leniently:
   * - exact normalized match, or
   * - same from/to squares when one side omits promotion piece.
   */
  def uciEquivalent(a: String, b: String): Boolean =
    val na = normalizeUciMove(a)
    val nb = normalizeUciMove(b)
    na.nonEmpty && nb.nonEmpty &&
      (na == nb || (na.take(4) == nb.take(4) && (na.length == 4 || nb.length == 4)))

  /**
   * Converts UCI → SAN when possible, otherwise falls back to a lightweight SAN-like format.
   */
  def uciToSanOrFormat(fen: String, uciMove: String): String =
    uciToSan(fen, uciMove).getOrElse(formatUciAsSan(uciMove))

  /**
   * Formats a SAN line with correct move numbers starting at `startPly`.
   *
   * Example:
   * - startPly=13, sans=["c3","O-O","d4"] → "7. c3 O-O 8. d4"
   * - startPly=14, sans=["...dxc4","Bg2"] → "7... dxc4 8. Bg2"
   */
  def formatSanWithMoveNumbers(startPly: Int, sans: List[String]): String =
    if sans.isEmpty then ""
    else
      val sb = new StringBuilder()
      var ply = startPly
      var lastMoveNo: Option[Int] = None
      var lastWasWhite = false

      sans.foreach { san =>
        val moveNo = (ply + 1) / 2
        val prefix =
          if (ply % 2 == 1) s"$moveNo."
          else if (lastWasWhite && lastMoveNo.contains(moveNo)) "" else s"$moveNo..."

        if (sb.nonEmpty) sb.append(" ")
        if (prefix.nonEmpty) sb.append(prefix).append(" ")
        sb.append(san)

        lastMoveNo = Some(moveNo)
        lastWasWhite = (ply % 2 == 1)
        ply += 1
      }

      sb.toString()

  /**
   * Infer absolute ply from a full FEN string.
   *
   * Formula:
   * - White to move at fullmove N -> ply = (N - 1) * 2
   * - Black to move at fullmove N -> ply = (N - 1) * 2 + 1
   */
  def plyFromFen(fen: String): Option[Int] =
    val parts = fen.trim.split("\\s+")
    if parts.length < 6 then None
    else
      val sideOffset =
        parts(1) match
          case "w" => Some(0)
          case "b" => Some(1)
          case _   => None
      val fullMove = parts(5).toIntOption.filter(_ >= 1)
      for
        fm <- fullMove
        so <- sideOffset
      yield (fm - 1) * 2 + so

  /**
   * Resolve a stable ply for book-style move annotation.
   *
   * In move-annotation mode (`playedMove` defined), `fen` is the position before
   * the played move, so the annotated move ply is `fenPly + 1`.
   */
  def resolveAnnotationPly(fen: String, playedMove: Option[String], fallbackPly: Int): Int =
    plyFromFen(fen)
      .map(_ + (if playedMove.exists(_.nonEmpty) then 1 else 0))
      .getOrElse(fallbackPly)

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


