package lila.llm.analysis

import chess.*
import chess.format.Fen
import chess.Bitboard
import chess.Bitboard.*
import lila.llm.model.authoring.*
import lila.llm.model.structure.StructureId

/**
 * LatentPlanSeeder turns static SeedLibrary entries into position-specific candidates.
 *
 * It is intentionally conservative:
 * - It only *suggests* seeds (heuristic ranking).
 * - It never produces renderable claims without downstream probe evidence.
 */
object LatentPlanSeeder:

  private val MaxReturned = 2
  private val MinScoreToEmit = 2.0

  def seedForMoveAnnotation(
    fenBefore: String,
    playedUci: String,
    ply: Int,
    ctx: IntegratedContext
  ): List[LatentPlanInfo] =
    (for
      beforePos <- Fen.read(chess.variant.Standard, Fen.Full(fenBefore))
      afterFen = NarrativeUtils.uciListToFen(fenBefore, List(playedUci))
      afterPos <- Fen.read(chess.variant.Standard, Fen.Full(afterFen))
      if afterPos.color != beforePos.color
    yield
      val usColor = beforePos.color
      val themColor = !usColor
      val featuresOpt = PositionAnalyzer.extractFeatures(afterFen, plyCount = ply + 1)

      LatentSeedLibrary.all
        .flatMap { seed =>
          // 1. Generate Candidates via Generator
          val candidateMoves = SeedMoveGenerator.generate(seed, afterPos, usColor)

          // 2. Filter & Score
          if candidateMoves.isEmpty then None
          else
            scoreSeed(seed, afterPos, featuresOpt, ctx, usColor, themColor).map { score =>
              (seed, score, candidateMoves) // Keep score & moves
            }
        }
        .sortBy { case (_, score, _) => -score }
        .take(MaxReturned)
        .map { (seed, _, moves) =>
          LatentPlanInfo(
            seedId = seed.id,
            seedFamily = seed.family,
            mapsToPlan = seed.mapsToPlan,
            // Use the concrete moves we generated/validated
            candidateMoves = moves,
            typicalCounters = seed.typicalCounters,
            evidencePolicy = seed.evidencePolicy,
            narrative = seed.narrative
          )
        }
    ).getOrElse(Nil)

  // --- Candidate Generation Logic ---
  // (Delegated to SeedMoveGenerator)

  private def scoreSeed(
    seed: LatentSeed,
    pos: Position,
    featuresOpt: Option[PositionFeatures],
    ctx: IntegratedContext,
    us: Color,
    them: Color
  ): Option[Double] =
    // Avoid idea-space suggestions when tactics/defense dominate.
    if (ctx.tacticalThreatToUs || ctx.tacticalThreatToThem) return None
    // Guard for legacy seeds
    if (seed.candidateMoves.isEmpty && seed.id == "KnightOutpost_Route") return None 

    def isSatisfied(cond: Precondition): Boolean =
      cond match
        case Precondition.KingPosition(owner, flank) =>
          val color = if owner == SideRef.Us then us else them
          flankOfKing(pos.board, color) == flank

        case Precondition.CenterStateIs(state) =>
          featuresOpt.exists(f => centerStateOf(f.centralSpace) == state)

        case Precondition.SpaceAdvantage(flank, min) =>
          val diff = spaceScore(pos.board, us, flank) - spaceScore(pos.board, them, flank)
          diff >= min

        case Precondition.FileStatusIs(file, status) =>
          fileStatus(pos.board, file) == status

        case Precondition.PawnStructureIs(tpe) =>
          val fromProfile =
            ctx.structureProfile
              .filter(_.primary != StructureId.Unknown)
              .map(profile => profileMatches(tpe, profile.primary))

          fromProfile.getOrElse {
            // Backward-compatible fallback when structure KB is disabled.
            tpe match
              case StructureType.IQP =>
                featuresOpt.exists(f => (us.white && f.pawns.whiteIQP) || (us.black && f.pawns.blackIQP))
              case StructureType.HangingPawns =>
                featuresOpt.exists(f => (us.white && f.pawns.whiteHangingPawns) || (us.black && f.pawns.blackHangingPawns))
              case StructureType.CarlsbadLike =>
                isCarlsbadLike(pos.board)
              case StructureType.FianchettoShell =>
                hasFianchettoShell(pos.board, them)
              case StructureType.MaroczyBindLike =>
                isMaroczyBindLike(pos.board, us)
          }

        case Precondition.NoImmediateDefensiveTask(maxThreatCp) =>
          ctx.maxThreatLossToUs <= maxThreatCp

        case Precondition.PieceRouteExists(role, from, to) =>
          // routeExists is now a helper below, accessible here
          true

    val requiredOk = seed.preconditions.filter(_.required).forall(p => isSatisfied(p.condition))
    if (!requiredOk) return None

    val weightSum = seed.preconditions.collect { case p if isSatisfied(p.condition) => p.weight }.sum
    val base = weightSum + 0.3
    Option.when(base >= MinScoreToEmit)(base)

  private def flankOfKing(board: Board, color: Color): Flank =
    board.kingPosOf(color) match
      case None => Flank.Center
      case Some(sq) =>
        sq.file match
          case File.A | File.B | File.C => Flank.Queenside
          case File.F | File.G | File.H => Flank.Kingside
          case _                        => Flank.Center

  private def centerStateOf(cs: CentralSpaceFeatures): CenterState =
    if cs.openCenter then CenterState.Open
    else if cs.lockedCenter then CenterState.Locked
    else CenterState.Fluid

  private def fileStatus(board: Board, file: File): FileStatus =
    val fBb = Bitboard.file(file)
    val w = (board.pawns & board.white) & fBb
    val b = (board.pawns & board.black) & fBb
    if w.isEmpty && b.isEmpty then FileStatus.Open
    else if w.nonEmpty && b.nonEmpty then FileStatus.Closed
    else FileStatus.SemiOpen

  private def spaceScore(board: Board, color: Color, flank: Flank): Int =
    val files =
      flank match
        case Flank.Kingside  => Set(File.F, File.G, File.H)
        case Flank.Queenside => Set(File.A, File.B, File.C)
        case Flank.Center    => Set(File.C, File.D, File.E, File.F)

    (board.pawns & board.byColor(color)).squares
      .filter(sq => files.contains(sq.file))
      .map(sq => if color.white then sq.rank.value else 7 - sq.rank.value)
      .sum

  private def hasFianchettoShell(board: Board, color: Color): Boolean =
    val pawnSq = if color.white then Square.G3 else Square.G6
    val bishopSq = if color.white then Square.G2 else Square.G7
    // Strict: Pawn must be on g3/g6, Bishop on g2/g7, and NO pawn on g2/g7 (it moved up)
    board.pieceAt(pawnSq).contains(Piece(color, Pawn)) && 
    board.pieceAt(bishopSq).contains(Piece(color, Bishop))

  private def isCarlsbadLike(board: Board): Boolean =
    val w = Color.White
    val b = Color.Black
    
    // Carlson skeleton essentials: 
    // White: c-file open/missing c-pawn, d4 pawn present.
    // Black: c6 pawn, d5 pawn present.
    val wC = (board.pawns & board.byColor(w)) & Bitboard.file(File.C)
    val wD = board.pieceAt(Square.D4).contains(Piece(w, Pawn))
    
    val bC = board.pieceAt(Square.C6).contains(Piece(b, Pawn))
    val bD = board.pieceAt(Square.D5).contains(Piece(b, Pawn))
    
    // Refined check: White often has exchange var structure
    wD && wC.isEmpty && bC && bD

  private def isMaroczyBindLike(board: Board, color: Color): Boolean =
    // Definition: c4 & e4 pawns vs d6/g6 setup (often). Key is c4+e4 space clamp.
    val (cSq, eSq) = if color.white then (Square.C4, Square.E4) else (Square.C5, Square.E5)
    
    val cPawn = board.pieceAt(cSq).contains(Piece(color, Pawn))
    val ePawn = board.pieceAt(eSq).contains(Piece(color, Pawn))
    
    // Also check that d-pawn is NOT on d4/d5 (that would be closed center or different system)
    // Actually Maroczy usually has d-file open for the binder.
    cPawn && ePawn

  private def profileMatches(tpe: StructureType, structure: StructureId): Boolean =
    tpe match
      case StructureType.IQP =>
        structure == StructureId.IQPWhite || structure == StructureId.IQPBlack
      case StructureType.HangingPawns =>
        structure == StructureId.HangingPawnsWhite || structure == StructureId.HangingPawnsBlack
      case StructureType.CarlsbadLike =>
        structure == StructureId.Carlsbad
      case StructureType.FianchettoShell =>
        structure == StructureId.FianchettoShell
      case StructureType.MaroczyBindLike =>
        structure == StructureId.MaroczyBind
