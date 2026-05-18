package lila.commentary.analysis

import lila.commentary.{ MoveReviewMoveRef, MoveReviewPvInterpretation }
import lila.commentary.model.NarrativeContext

private[commentary] object PvSemanticInterpreter:

  def interpret(
      ctx: NarrativeContext,
      played: MoveReviewBoardFacts.MoveFacts,
      openingIdea: Option[OpeningIdeaCatalog.OpeningIdea],
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea],
      reasonTags: List[String],
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[MoveReviewPvInterpretation] =
    lineFacts.flatMap { lineFacts =>
      val family = openingIdea.map(_.family.toLowerCase).getOrElse("")
      val pattern = openingIdea.map(_.pattern).getOrElse("")
      val purpose = classifyPurpose(ctx, played, endgameIdea, family, pattern, lineFacts.reply, lineFacts.continuation, reasonTags)
      purpose.map { linePurpose =>
        val tension = classifyTension(linePurpose, played, lineFacts.reply, lineFacts.continuation)
        val opponentMeaning = lineFacts.reply.flatMap(move => classifyOpponentReply(played, family, move))
        val confirms = confirmedFacts(linePurpose, openingIdea, endgameIdea, reasonTags, lineFacts.continuation)
        MoveReviewPvInterpretation(
          linePurpose = linePurpose,
          confirms = confirms,
          tension = tension,
          opponentReplyMeaning = opponentMeaning,
          learningPoint = "",
          supportedByLineId = Some(lineFacts.line.lineId),
          confidence = "bounded_local"
        )
      }
    }

  private def classifyPurpose(
      ctx: NarrativeContext,
      played: MoveReviewBoardFacts.MoveFacts,
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea],
      family: String,
      pattern: String,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef],
      reasonTags: List[String]
  ): Option[String] =
    val openingContext =
      ctx.header.phase.equalsIgnoreCase("Opening") ||
        ctx.phase.current.equalsIgnoreCase("Opening") ||
        ctx.openingData.nonEmpty ||
        ctx.openingEvent.nonEmpty
    if played.isCastle then Some("king_safety_first")
    else if family.contains("sicilian") || played.uci == "c7c5" then Some("challenge_center")
    else if family.contains("queen's gambit") || pattern == "c4_d5_tension" || played.uci == "c2c4" then
      Some("challenge_center")
    else if played.isCapture && MoveReviewPvFacts.isImmediateRecapture(played, reply) then
      if played.capturedPiece.exists(_.toLower != 'p') || !played.isPawn then Some("clarify_exchange")
      else Some("resolve_capture_tension")
    else if endgameIdea.nonEmpty then
      Some("improve_endgame_activity")
    else if ctx.phase.current.equalsIgnoreCase("Endgame") && (played.isKing || played.isPawn) && endgameContinuationShowsActivity(continuation) then
      Some("improve_endgame_activity")
    else if continuation.exists(move => MoveReviewPvFacts.isCentralBreak(move.uci, played.isWhite)) then
      Some("center_break_setup")
    else if family.contains("italian") && continuation.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == "d2d3") then
      Some("quiet_development")
    else if played.attackedEnemyPieces.nonEmpty && reply.exists(move => replyAddressesDirectThreat(played, move)) then
      Some("answer_direct_threat")
    else if openingContext && reasonTags.contains("king_safety") then Some("king_safety_first")
    else if openingContext && reasonTags.contains("defends_center_pawn") then Some("defend_center_pawn")
    else if openingContext && reasonTags.contains("controls_center") then Some("challenge_center")
    else if openingContext && reasonTags.contains("develops_piece") then Some("quiet_development")
    else None

  private def classifyTension(
      purpose: String,
      played: MoveReviewBoardFacts.MoveFacts,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef]
  ): String =
    if purpose == "resolve_capture_tension" || purpose == "clarify_exchange" then "center_released"
    else if continuation.exists(move => MoveReviewPvFacts.isCentralBreak(move.uci, played.isWhite)) then "delayed_center_break"
    else if purpose == "challenge_center" && Set("c2c4", "c7c5").contains(played.uci) then "tension_maintained"
    else if MoveReviewPvFacts.isCentralBreak(played.uci, played.isWhite) then "immediate_center_break"
    else if (reply.toList ++ continuation.toList).exists(move => MoveReviewPvFacts.isCenterCapture(move.uci)) then "center_released"
    else "tension_maintained"

  private def classifyOpponentReply(
      played: MoveReviewBoardFacts.MoveFacts,
      family: String,
      reply: MoveReviewMoveRef
  ): Option[String] =
    val uci = MoveReviewPvFacts.normalizeUci(reply.uci)
    if family.contains("ruy lopez") && uci == "a7a6" then Some("asks_piece_commitment")
    else if played.isWhite && (family.contains("italian") || played.uci == "f1c4") && uci == "g8f6" then
      Some("attacks_center_pawn")
    else if played.isCapture && MoveReviewPvFacts.isImmediateRecapture(played, Some(reply)) then Some("recaptures")
    else if played.attackedEnemyPieces.nonEmpty && attacksMovedPiece(played, reply) then Some("attacks_moved_piece")
    else if !played.isWhite && uci == "g1f3" then Some("develops_and_contests")
    else if Set("e7e6", "d7d6", "c7c6", "e2e3", "d2d3", "c2c3").contains(uci) then Some("reinforces_center")
    else if Set("c7c5", "c2c4", "d7d5", "d2d4", "e7e5", "e2e4").contains(uci) then Some("challenges_center")
    else if uci.endsWith("c6") || uci.endsWith("f6") || uci.endsWith("c3") || uci.endsWith("f3") then
      Some("develops_and_contests")
    else None

  private def confirmedFacts(
      purpose: String,
      openingIdea: Option[OpeningIdeaCatalog.OpeningIdea],
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea],
      reasonTags: List[String],
      continuation: Option[MoveReviewMoveRef]
  ): List[String] =
    val values = scala.collection.mutable.LinkedHashSet.empty[String]
    if openingIdea.nonEmpty then values += "opening_idea"
    endgameIdea.foreach(_.confirms.foreach(values += _))
    reasonTags.foreach {
      case tag @ ("develops_piece" | "controls_center" | "targets_f7_or_f2" | "king_safety") => values += tag
      case _                                                                                  =>
    }
    purpose match
      case "center_break_setup"       => values += "center_break_setup"
      case "resolve_capture_tension"  => values += "capture_tension"
      case "answer_direct_threat"     => values += "direct_threat"
      case "clarify_exchange"         => values += "exchange_clarified"
      case "improve_endgame_activity" => values += "endgame_activity"
      case _                          =>
    if continuation.exists(move => Set("d2d3", "d7d6", "e2e3", "e7e6").contains(MoveReviewPvFacts.normalizeUci(move.uci))) then
      values += "defends_center_pawn"
    values.toList

  private def attacksMovedPiece(played: MoveReviewBoardFacts.MoveFacts, reply: MoveReviewMoveRef): Boolean =
    val afterReply = MoveReviewBoardFacts.boardFromFen(reply.fenAfter)
    val attacks =
      for
      to <- MoveReviewPvFacts.toSquare(reply.uci)
      coord <- MoveReviewBoardFacts.coord(to)
      piece <- afterReply.get(to)
      yield MoveReviewBoardFacts.attacks(piece.toLower, piece.isUpper, coord, played.toKey, afterReply)
    attacks.contains(true)

  private def replyAddressesDirectThreat(played: MoveReviewBoardFacts.MoveFacts, reply: MoveReviewMoveRef): Boolean =
    attacksMovedPiece(played, reply) ||
      (MoveReviewPvFacts.isCaptureLike(reply) && MoveReviewPvFacts.toSquare(reply.uci).exists(square =>
        played.attackedEnemyPieces.exists { case (targetSquare, _) => targetSquare == square }
      ))

  private def endgameContinuationShowsActivity(continuation: Option[MoveReviewMoveRef]): Boolean =
    continuation.exists { move =>
      val san = move.san.trim
      san.startsWith("K") || san.headOption.exists(ch => ch.isLower || ch.isDigit)
    }
