package lila.commentary.analysis

import lila.commentary.MoveReviewMoveRef

private[commentary] object MoveReviewLearningPointRenderer:

  def render(
      openingIdea: Option[OpeningIdeaCatalog.OpeningIdea],
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea],
      purpose: String,
      played: MoveReviewBoardFacts.MoveFacts,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef]
  ): String =
    val family = openingIdea.map(_.family.toLowerCase).getOrElse("")
    val pattern = openingIdea.map(_.pattern).getOrElse("")
    val replySan = reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
    val continuationSan = continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")

    endgameIdea.map(_.pattern) match
      case Some("passed_pawn_support") =>
        s"The line shows passed-pawn support: ${played.san} brings the king close to the passer, and $continuationSan shows the pawn can keep moving with help nearby."
      case Some("rook_behind_passer") =>
        s"The line shows the rook-behind-passer rule in a concrete way: ${played.san} gets behind the pawn, and $continuationSan keeps the passer advancing on that file."
      case Some("king_activity_center") =>
        s"The line shows king activity in the endgame: ${played.san} improves the king first, and $continuationSan shows how that activity starts to matter."
      case _ if family.contains("italian") && continuation.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == "d2d3") =>
        s"$replySan asks about the e4 pawn, and $continuationSan shows the quiet Italian answer: keep Bc4 active, support e4, and postpone the central break."
      case _ if family.contains("italian") && continuation.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == "d2d4") =>
        s"$replySan asks about e4, but $continuationSan shows the sharper Italian plan: use the bishop placement first, then challenge the center with d4."
      case _ if family.contains("ruy lopez") && (pattern == "bishop_b5_c6_e5" || pattern == "a6_Ba4_maintains_pressure") =>
        s"$replySan asks the bishop to choose, and $continuationSan keeps the Ruy Lopez pressure on the c6 knight and e5 pawn instead of trading the idea away."
      case _ if family.contains("queen's gambit") || played.uci == "c2c4" =>
        s"The line keeps the question on d5: ${played.san} creates the tension, $replySan supports the center, and $continuationSan adds another piece to the fight."
      case _ if family.contains("sicilian") || played.uci == "c7c5" =>
        s"The line keeps the Sicilian question about d4 alive: ${played.san} contests the center from the flank while $continuationSan supports that structure."
      case _ =>
        purpose match
          case "king_safety_first" =>
            s"The line shows king safety first: ${played.san} gets the king safe before deciding how or when to open the center."
          case "resolve_capture_tension" =>
            s"The PV shows the capture tension clearly: ${played.san} can be answered by $replySan, so the lesson is what remains after the recapture rather than the capture alone."
          case "answer_direct_threat" =>
            s"${played.san} creates a direct target, and $replySan is the reply that asks the moved piece to prove the threat instead of letting it continue for free."
          case "clarify_exchange" =>
            s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence resolves which trade remains on ${played.toKey}."
          case "improve_endgame_activity" =>
            s"The line shows king activity in the endgame: ${played.san} improves the king or pawn setup first, and $continuationSan shows how that activity starts to matter."
          case "center_break_setup" =>
            s"The line shows the setup idea: ${played.san} comes first, then $continuationSan tests whether the center can be opened on better terms."
          case _ =>
            s"The line keeps the purpose local: ${played.san} sets the idea, $replySan asks for a response, and $continuationSan shows how the plan is maintained."
