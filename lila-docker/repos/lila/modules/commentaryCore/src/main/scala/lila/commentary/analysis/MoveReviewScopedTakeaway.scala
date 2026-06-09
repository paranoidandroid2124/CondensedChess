package lila.commentary.analysis

import lila.commentary.model.Fact

private[commentary] object MoveReviewScopedTakeaway:
  import MoveReviewLocalFact.{
    Admission as LocalFactAdmission,
    Candidate as LocalFactCandidate,
    Family as LocalFactFamily,
    LineBinding as LocalFactLineBinding,
    Source as LocalFactSource,
    Subject as LocalFactSubject
  }

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
      lineFacts: Option[MoveReviewPvLine.LineFacts],
      localFact: LocalFactAdmission =
        MoveReviewLocalFact.admitted(LocalFactCandidate(
          family = LocalFactFamily.LineConsequence,
          source = LocalFactSource.PvCoupledLine,
          subject = LocalFactSubject.PlayedMove,
          strictFallbackCandidate = false,
          lineBinding = LocalFactLineBinding.PvCoupled,
          guardrails = List("compatibility_default")
        ))
  ): Option[ScopedTakeaway] =
    lineFacts
      .filter(line => MoveReviewPvLine.normalizeUci(line.first.uci) == played.uci)
      .flatMap { line =>
        renderText(purpose, played, evidence, line, localFact).filter(isAllowedText).map { text =>
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
      line: MoveReviewPvLine.LineFacts,
      localFact: LocalFactAdmission
  ): Option[String] =
    val replySan = line.reply.map(_.san).filter(_.nonEmpty).getOrElse("the reply")
    val continuationSan = line.continuation.map(_.san).filter(_.nonEmpty).getOrElse("the follow-up")
    val openingGoalName = evidence.openingGoal.map(_.goalName.toLowerCase).getOrElse("")
    Some(
      localFact.family match
        case LocalFactFamily.Threat | LocalFactFamily.Pressure =>
          s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $replySan is the next checked move, and the line does not authorize a broader evaluation claim."
        case LocalFactFamily.Defense =>
          s"The PV keeps the defensive detail bounded: ${played.san} is the move under review, $replySan is the next checked move, and the line does not authorize a broader threat claim."
        case LocalFactFamily.Endgame if evidence.endgameFacts.exists(_.isInstanceOf[Fact.KingActivity]) =>
          s"The line keeps the endgame detail local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
        case LocalFactFamily.Endgame =>
          s"The line keeps the concrete endgame detail local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
        case LocalFactFamily.Capture =>
          s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence remains bounded to ${played.toKey}."
        case LocalFactFamily.KingSafety =>
          s"The line keeps the king-safety detail bounded: ${played.san} is the move under review, $replySan is the next checked move, and $continuationSan continues the line."
        case LocalFactFamily.OpeningGoal =>
          purpose match
            case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
              s"The checked line stays local to ${played.san}: after $replySan, $continuationSan remains the checked continuation."
            case "quiet_development" =>
              s"The checked line stays local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
            case "center_break_setup" =>
              s"The checked line keeps the setup bounded: ${played.san} comes first, then $continuationSan continues the line."
            case "challenge_center" =>
              s"The checked line keeps the center sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case _ =>
              s"The checked line stays local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
        case _ =>
          purpose match
            case "quiet_development" if line.continuation.exists(move => MoveReviewPvLine.normalizeUci(move.uci) == "d2d3") =>
              s"The checked line stays local to ${played.san}: after $replySan, $continuationSan remains the checked continuation."
            case "quiet_development" =>
              s"The checked line stays local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
            case "center_break_setup" =>
              s"The checked line keeps the setup bounded: ${played.san} comes first, then $continuationSan continues the line."
            case "challenge_center" =>
              s"The checked line keeps the center sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case "king_safety_first" =>
              s"The line keeps the king-safety sequence bounded: ${played.san} is the move under review, and $continuationSan continues the line."
            case "create_tactical_threat" if openingGoalName.nonEmpty =>
              s"The PV keeps the opening goal bounded: ${played.san} is the move under review, $replySan is the next checked move, and the line does not authorize a broader evaluation claim."
            case "create_tactical_threat" =>
              s"The PV keeps the local tactical detail bounded: ${played.san} is the move under review, $replySan is the next checked move, and the line does not authorize a broader evaluation claim."
            case "answer_direct_threat" =>
              s"The PV keeps the defensive detail bounded: ${played.san} is the move under review, $replySan is the next checked move, and the line does not authorize a broader threat claim."
            case "resolve_capture_tension" =>
              s"The PV shows the capture tension clearly: ${played.san} can be answered by $replySan, so the point is what remains after the recapture rather than the capture alone."
            case "clarify_exchange" =>
              s"The line clarifies the exchange: ${played.san} is met by $replySan, so the sequence resolves which trade remains on ${played.toKey}."
            case "improve_endgame_activity" if evidence.endgameFacts.exists(_.isInstanceOf[Fact.KingActivity]) =>
              s"The line keeps the endgame detail local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
            case "improve_endgame_activity" =>
              s"The line keeps the concrete endgame detail local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
            case _ =>
              s"The checked line stays local to ${played.san}: $replySan is the next checked move, and $continuationSan continues the line."
    )
