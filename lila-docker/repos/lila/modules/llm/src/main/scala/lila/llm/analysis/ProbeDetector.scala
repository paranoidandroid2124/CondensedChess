package lila.llm.analysis

import lila.llm.model._
import lila.llm.analysis.L3.PvLine
import chess.*
import chess.format.Fen

/**
 * Detects "Ghost Plans" and generates ProbeRequests for the client.
 */
object ProbeDetector:

  private val ScoreThreshold = 0.70
  private val MaxMovesPerPlan = 3
  private val DefaultDepth = 20

  /**
   * Detects ghost plans and generates requests.
   */
  def detect(
    ctx: IntegratedContext,
    planScoring: PlanScoringResult,
    multiPv: List[PvLine],
    fen: String
  ): List[ProbeRequest] = {
    if (multiPv.isEmpty) return Nil

    // We must only emit LEGAL UCI moves. If FEN is invalid, fail closed (no probes).
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map { pos =>
      val sideToMove = pos.color
      val ctxSideToMove = if ctx.isWhiteToMove then White else Black
      if (ctxSideToMove != sideToMove) Nil
      else
        val legalMoves = pos.legalMoves.toList
        val topPvMoves = multiPv.flatMap(_.moves.headOption).toSet
        val baseline = multiPv.headOption

        // 1. Plan-based Ghost Probes (Existing)
        val planProbes = planScoring.topPlans.flatMap { pm =>
          if (pm.score < ScoreThreshold) None
          else if (pm.plan.color != sideToMove) None
          else
            val repMoves = representativeMoves(pos, pm, legalMoves).take(MaxMovesPerPlan)
            val isRepresented = repMoves.exists(topPvMoves.contains)

            if (repMoves.isEmpty || isRepresented) None
            else
              Some(ProbeRequest(
                id = stableRequestId(pm.plan, fen),
                fen = fen,
                moves = repMoves,
                depth = DefaultDepth,
                planId = Some(pm.plan.id.toString),
                planName = Some(pm.plan.name),
                planScore = Some(pm.score),
                baselineMove = baseline.flatMap(_.moves.headOption),
                baselineEvalCp = baseline.map(_.evalCp),
                baselineMate = baseline.flatMap(_.mate),
                baselineDepth = baseline.map(_.depth).filter(_ > 0)
              ))
        }

        // 2. Competitive Probes: Similar engine scores but different moves
        val competitiveProbes = multiPv.drop(1).take(2).flatMap { pv =>
          val bestPv = multiPv.head
          val scoreDiff = (pv.evalCp - bestPv.evalCp).abs
          // If second/third best move is close (e.g. < 30cp difference), probe it for "choice" explanation
          if (scoreDiff < 30 && pv.moves.nonEmpty) {
            val move = pv.moves.head
            Some(ProbeRequest(
              id = s"competitive_${move}_${Integer.toHexString(fen.hashCode)}",
              fen = fen,
              moves = List(move),
              depth = DefaultDepth,
              planName = Some(s"Competitive Alternative: $move")
            ))
          } else None
        }

        // 3. Defensive Probes: If top move is passive but an aggressive one is slightly worse
        val defensiveProbes = if (multiPv.size > 1 && baseline.exists(_.evalCp > -50)) {
          val aggressiveMoves = legalMoves.filter(m => m.captures || m.after.check.yes).map(_.toUci.uci)
          multiPv.filter(pv => pv.moves.headOption.exists(aggressiveMoves.contains)).headOption.flatMap { aggPv =>
            val bestPv = multiPv.head
            val cost = (bestPv.evalCp - aggPv.evalCp)
            // If an aggressive move exists that costs 50-150cp, probe it to explain "Why not X?"
            if (cost >= 50 && cost <= 150) {
              val move = aggPv.moves.head
              Some(ProbeRequest(
                id = s"aggressive_why_not_${move}_${Integer.toHexString(fen.hashCode)}",
                fen = fen,
                moves = List(move),
                depth = DefaultDepth,
                planName = Some(s"Aggressive Alternative: $move")
              ))
            } else None
          }
        } else None

        (planProbes ++ competitiveProbes ++ defensiveProbes).take(5)
    }.getOrElse(Nil)
  }

  private def stableRequestId(plan: Plan, fen: String): String =
    val slug = plan.name
      .toLowerCase
      .map(c => if (c.isLetterOrDigit) c else '_')
      .mkString
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
    s"${plan.id}_${slug}_${Integer.toHexString(fen.hashCode)}"

  /**
   * Produces representative LEGAL UCI moves for a plan in the current position.
   *
   * Strategy:
   * 1) Try evidence-driven mapping (motif -> legal move constraints)
   * 2) Fill remaining slots with a small plan-specific heuristic fallback
   */
  private def representativeMoves(
      pos: Position,
      pm: PlanMatch,
      legalMoves: List[Move]
  ): List[String] = {
    val evidenceUci =
      pm.evidence
        .flatMap(e => movesFromMotif(e.motif, pos, legalMoves))
        .map(_.toUci.uci)
        .distinct

    val fallbackUci =
      movesFromPlan(pm.plan, pos, legalMoves)
        .map(_.toUci.uci)
        .distinct
        .sorted
        .filterNot(evidenceUci.contains)

    (evidenceUci ++ fallbackUci).take(MaxMovesPerPlan)
  }

  private def movesFromMotif(motif: Motif, pos: Position, legalMoves: List[Move]): List[Move] = {
    val sideToMove = pos.color
    motif match {
      case Motif.PawnAdvance(file, fromRank, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.orig.file == file &&
          mv.orig.rank.value + 1 == fromRank &&
          mv.dest.rank.value + 1 == toRank
        }

      case Motif.PawnBreak(file, targetFile, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.orig.file == file &&
          mv.dest.file == targetFile &&
          mv.captures
        }

      case Motif.PawnPromotion(file, promotedTo, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.dest.file == file &&
          mv.promotion.contains(promotedTo)
        }

      case Motif.PassedPawnPush(file, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Pawn &&
          mv.dest.file == file &&
          mv.dest.rank.value + 1 == toRank &&
          !mv.captures
        }

      case Motif.RookLift(file, fromRank, toRank, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Rook &&
          mv.dest.file == file &&
          mv.orig.rank.value + 1 == fromRank &&
          mv.dest.rank.value + 1 == toRank
        }

      case Motif.Fianchetto(_, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          mv.piece.role == Bishop &&
          (mv.dest == Square.G2 || mv.dest == Square.B2 || mv.dest == Square.G7 || mv.dest == Square.B7)
        }

      case Motif.Centralization(piece, sq, c, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq)

      case Motif.Outpost(piece, sq, c, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq)

      case Motif.OpenFileControl(file, c, _, _) if c == sideToMove =>
        legalMoves.filter { mv =>
          (mv.piece.role == Rook || mv.piece.role == Queen) && mv.dest.file == file
        }

      case Motif.Capture(piece, _, sq, _, c, _, _, _) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == piece && mv.dest == sq && mv.captures)

      case Motif.Check(_, _, _, c, _, _) if c == sideToMove =>
        legalMoves.filter(_.after.check.yes)

      case Motif.Castling(_, c, _, _) if c == sideToMove =>
        legalMoves.filter(_.castle.isDefined)

      case _ => Nil
    }
  }

  private def movesFromPlan(plan: Plan, pos: Position, legalMoves: List[Move]): List[Move] = {
    val sideToMove = pos.color
    plan match {
      case Plan.KingsideAttack(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.dest.file.isKingside && !mv.captures)

      case Plan.QueensideAttack(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.dest.file.isQueenside && !mv.captures)

      case Plan.PawnStorm(c, side) if c == sideToMove =>
        val filePred = if (side == "queenside") (_: File).isQueenside else (_: File).isKingside
        legalMoves.filter(mv => mv.piece.role == Pawn && filePred(mv.dest.file) && !mv.captures)

      case Plan.CentralControl(c) if c == sideToMove =>
        val centralSquares = Set(Square.D4, Square.E4, Square.D5, Square.E5)
        legalMoves.filter { mv =>
          (mv.piece.role == Pawn && mv.dest.file.isCentral && !mv.captures) ||
          (centralSquares.contains(mv.dest) && (mv.piece.role == Knight || mv.piece.role == Bishop))
        }

      case Plan.PieceActivation(c) if c == sideToMove =>
        val backRank = if c.white then Rank.First else Rank.Eighth
        legalMoves.filter(mv =>
          (mv.piece.role == Knight || mv.piece.role == Bishop) && mv.orig.rank == backRank
        )

      case Plan.RookActivation(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Rook && !mv.captures)

      case Plan.PassedPawnPush(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && !mv.captures)

      case Plan.Promotion(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == Pawn && mv.promotion.isDefined)

      case Plan.Blockade(c, squareStr) if c == sideToMove =>
        Square.fromKey(squareStr).map { sq =>
          legalMoves.filter(mv => mv.dest == sq)
        }.getOrElse(Nil)

      case Plan.KingActivation(c) if c == sideToMove =>
        legalMoves.filter(mv => mv.piece.role == King && !mv.captures && mv.castle.isEmpty)

      case _ => Nil
    }
  }

  extension (f: File)
    def isKingside: Boolean = f == File.F || f == File.G || f == File.H
    def isQueenside: Boolean = f == File.A || f == File.B || f == File.C
    def isCentral: Boolean = f == File.D || f == File.E
