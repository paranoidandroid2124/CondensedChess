package lila.commentary.analysis

import lila.commentary.model.NarrativeContext

private[commentary] object EndgameIdeaCatalog:

  final case class EndgameIdea(
      pattern: String,
      title: String,
      prose: String,
      reasonTags: List[String],
      confirms: List[String]
  )

  def matchIdea(
      ctx: NarrativeContext,
      facts: MoveReviewBoardFacts.MoveFacts,
      primitiveTags: Set[String],
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[EndgameIdea] =
    if !isEndgame(ctx) || lineFacts.flatMap(_.continuation).isEmpty || !primitiveTags.contains("endgame_technique") then None
    else
      List(
        passedPawnSupport(facts),
        rookBehindPassedPawn(facts),
        kingActivity(facts)
      ).flatten.headOption

  private def kingActivity(facts: MoveReviewBoardFacts.MoveFacts): Option[EndgameIdea] =
    Option.when(facts.isKing && centralOrNearCentral(facts.toKey)) {
      EndgameIdea(
        pattern = "king_activity_center",
        title = s"${facts.san} improves king activity",
        prose =
          s"${facts.san} brings the king toward the working area of the endgame. This stays local: the move improves activity before the PV shows whether the pawn play can advance.",
        reasonTags = List("endgame_technique", "king_activity"),
        confirms = List("endgame_activity", "king_activity_center")
      )
    }

  private def passedPawnSupport(facts: MoveReviewBoardFacts.MoveFacts): Option[EndgameIdea] =
    val supported = ownPassedPawns(facts).exists { pawn =>
      advancedEnough(facts.isWhite, pawn) &&
        adjacent(facts.to, pawn) &&
        facts.isKing
    }
    Option.when(supported) {
      EndgameIdea(
        pattern = "passed_pawn_support",
        title = s"${facts.san} supports the passed pawn",
        prose =
          s"${facts.san} moves the king next to an advanced passer. The PV-backed point is support: the pawn can move with the king close enough to help it advance.",
        reasonTags = List("endgame_technique", "passed_pawn_support"),
        confirms = List("endgame_activity", "passed_pawn_support")
      )
    }

  private def rookBehindPassedPawn(facts: MoveReviewBoardFacts.MoveFacts): Option[EndgameIdea] =
    val behind = facts.role == 'r' && ownPassedPawns(facts).exists { pawn =>
      facts.to.file == pawn.file &&
        (if facts.isWhite then facts.to.rank < pawn.rank else facts.to.rank > pawn.rank)
    }
    Option.when(behind) {
      EndgameIdea(
        pattern = "rook_behind_passer",
        title = s"${facts.san} puts the rook behind the passer",
        prose =
          s"${facts.san} places the rook behind a passed pawn. The line support is exact: the rook is on the pawn's file and the PV keeps the passer moving.",
        reasonTags = List("endgame_technique", "rook_behind_passer"),
        confirms = List("endgame_activity", "rook_behind_passer")
      )
    }

  private def ownPassedPawns(facts: MoveReviewBoardFacts.MoveFacts): List[MoveReviewBoardFacts.Coord] =
    facts.after.toList.flatMap { case (square, piece) =>
      MoveReviewBoardFacts.coord(square).filter(_ =>
        piece.toLower == 'p' &&
          piece.isUpper == facts.piece.isUpper &&
          isPassedPawn(facts.isWhite, square, facts.after)
      )
    }

  private def isPassedPawn(isWhite: Boolean, square: String, board: Map[String, Char]): Boolean =
    MoveReviewBoardFacts.coord(square).exists { pawn =>
      val enemyIsUpper = !isWhite
      board.forall { case (otherSquare, piece) =>
        if piece.toLower != 'p' || piece.isUpper != enemyIsUpper then true
        else
          MoveReviewBoardFacts.coord(otherSquare).forall { other =>
            val sameOrAdjacentFile = math.abs(other.file - pawn.file) <= 1
            val ahead = if isWhite then other.rank > pawn.rank else other.rank < pawn.rank
            !(sameOrAdjacentFile && ahead)
          }
      }
    }

  private def adjacent(a: MoveReviewBoardFacts.Coord, b: MoveReviewBoardFacts.Coord): Boolean =
    math.abs(a.file - b.file) <= 1 && math.abs(a.rank - b.rank) <= 1

  private def advancedEnough(isWhite: Boolean, pawn: MoveReviewBoardFacts.Coord): Boolean =
    if isWhite then pawn.rank >= 4 else pawn.rank <= 5

  private def centralOrNearCentral(square: String): Boolean =
    Set("c3", "d3", "e3", "f3", "c4", "d4", "e4", "f4", "c5", "d5", "e5", "f5", "c6", "d6", "e6", "f6", "d2", "e2", "d7", "e7")
      .contains(square)

  private def isEndgame(ctx: NarrativeContext): Boolean =
    ctx.phase.current.equalsIgnoreCase("Endgame") || ctx.header.phase.equalsIgnoreCase("Endgame")
