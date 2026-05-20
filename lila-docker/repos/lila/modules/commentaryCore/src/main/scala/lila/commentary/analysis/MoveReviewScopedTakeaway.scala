package lila.commentary.analysis

import lila.commentary.model.Fact

private[commentary] object MoveReviewScopedTakeaway:

  enum EvidenceTier:
    case PvCoupledLocal

    def key: String =
      this match
        case PvCoupledLocal => "pv_coupled_local"

  enum Source:
    case MoveReviewPvMeaning

    def key: String =
      this match
        case MoveReviewPvMeaning => "move_review_pv_meaning"

  final case class ScopedTakeaway(
      text: String,
      fen: String,
      playedUci: String,
      lineId: Option[String],
      evidenceTier: EvidenceTier,
      source: Source,
      guardrails: List[String]
  )

  private val Guardrails = List(
    "scope:move_review_local",
    "owner:pv_coupled_line",
    "schema:compatibility_projection",
    "excludes:support_only_strategy_pack",
    "excludes:fallback_provider"
  )

  private val ForbiddenGlobalizers =
    List(
      """(?i)\balways\b""".r,
      """(?i)\bgenerally\b""".r,
      """(?i)\bin every position\b""".r,
      """(?i)\bshared lesson\b""".r,
      """(?i)\bthe lesson is\b""".r,
      """(?i)\bas a rule\b""".r,
      """(?i)\bthe rule is\b""".r
    )

  def build(
      purpose: String,
      played: CommentaryIdeaSurface.PlayedMove,
      evidence: CommentaryIdeaSurface.MoveReviewEvidence,
      lineFacts: Option[MoveReviewPvLine.LineFacts]
  ): Option[ScopedTakeaway] =
    lineFacts
      .filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
      .flatMap { line =>
        renderText(purpose, played, evidence, line).filter(isAllowedText).map { text =>
          ScopedTakeaway(
            text = text,
            fen = line.first.fenAfter,
            playedUci = played.uci,
            lineId = Some(line.line.lineId),
            evidenceTier = EvidenceTier.PvCoupledLocal,
            source = Source.MoveReviewPvMeaning,
            guardrails = Guardrails
          )
        }
      }

  def isAllowedText(text: String): Boolean =
    val clean = Option(text).getOrElse("").trim
    clean.nonEmpty && !ForbiddenGlobalizers.exists(_.findFirstIn(clean).nonEmpty)

  private def renderText(
      purpose: String,
      played: CommentaryIdeaSurface.PlayedMove,
      evidence: CommentaryIdeaSurface.MoveReviewEvidence,
      line: MoveReviewPvLine.LineFacts
  ): Option[String] =
    val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
    val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")
    val openingGoalName = evidence.openingGoal.map(_.goalName.toLowerCase).getOrElse("")
    Some(
      purpose match
        case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
          s"$replySan asks about the e4 pawn, and $continuationSan shows the quiet setup: keep ${played.san} useful, support e4, and postpone the central break."
        case "quiet_development" =>
          s"The line keeps the purpose local: ${played.san} improves the setup, $replySan asks for a response, and $continuationSan shows how the development is maintained."
        case "center_break_setup" =>
          s"The line shows the setup idea: ${played.san} comes first, then $continuationSan tests whether the center can be opened on better terms."
        case "challenge_center" =>
          s"The line keeps the center question alive: ${played.san} starts the challenge while $continuationSan shows how the structure is supported."
        case "king_safety_first" =>
          s"The line shows king safety first: ${played.san} gets the king safe before deciding how or when to open the center."
        case "create_tactical_threat" if openingGoalName.nonEmpty =>
          s"The PV keeps the opening goal bounded: ${played.san} is the move under review, $replySan is the first answer, and the line does not authorize a broader evaluation claim."
        case "create_tactical_threat" =>
          s"The PV keeps the tactical point concrete: ${played.san} creates the verified target pattern, and $replySan is the first line response to that pressure."
        case "answer_direct_threat" =>
          s"${played.san} creates a direct target, and $replySan is the reply that asks the moved piece to prove the threat instead of letting it continue for free."
        case "resolve_capture_tension" =>
          s"The PV shows the capture tension clearly: ${played.san} can be answered by $replySan, so the point is what remains after the recapture rather than the capture alone."
        case "clarify_exchange" =>
          s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence resolves which trade remains on ${played.toKey}."
        case "improve_endgame_activity" if evidence.endgameFacts.exists(_.isInstanceOf[Fact.KingActivity]) =>
          s"The line shows king activity in the endgame: ${played.san} improves the king first, and $continuationSan shows how that activity starts to matter."
        case "improve_endgame_activity" =>
          s"The line shows endgame activity in concrete terms: ${played.san} changes the verified endgame feature, and $continuationSan shows the next local use."
        case _ =>
          s"The line keeps the purpose local: ${played.san} sets the idea, $replySan asks for a response, and $continuationSan shows how it is maintained."
    )
