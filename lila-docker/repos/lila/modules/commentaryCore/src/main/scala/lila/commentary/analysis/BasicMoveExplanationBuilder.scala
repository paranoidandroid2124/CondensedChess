package lila.commentary.analysis

import lila.commentary.{ MoveReviewExplanation, MoveReviewPvInterpretation, MoveReviewRefs }
import lila.commentary.model.NarrativeContext

private[commentary] object BasicMoveExplanationBuilder:

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[MoveReviewExplanation] =
    for
      facts <- MoveReviewBoardFacts.current(ctx)
      reasons = MoveReviewBoardFacts.primitiveTags(ctx, facts)
      if reasons.nonEmpty
      lineFacts = MoveReviewPvFacts.firstCoupled(ctx.fen, facts.uci, refs)
      openingIdea = OpeningIdeaCatalog.matchIdea(ctx, facts, reasons, lineFacts)
      endgameIdea = EndgameIdeaCatalog.matchIdea(ctx, facts, reasons, lineFacts)
      reasonTags = (openingIdea.map(_.reasonTags).getOrElse(Nil) ++ endgameIdea.map(_.reasonTags).getOrElse(Nil) ++ reasons.toList).distinct
      pvInterpretation = enrichInterpretation(
        PvSemanticInterpreter.interpret(
          ctx = ctx,
          played = facts,
          openingIdea = openingIdea,
          endgameIdea = endgameIdea,
          reasonTags = reasonTags,
          lineFacts = lineFacts
        ),
        openingIdea,
        endgameIdea,
        facts,
        lineFacts
      )
      shortLine = MoveReviewPvFacts.shortLine(refs, pvInterpretation.flatMap(_.supportedByLineId))
      if basicLaneAllowed(ctx, facts, pvInterpretation)
    yield
      val title =
        openingIdea.map(_.title).orElse(endgameIdea.map(_.title)).getOrElse(defaultTitle(facts.san, reasons, pvInterpretation))
      val prose =
        enrichProseWithPv(openingIdea.map(_.prose).orElse(endgameIdea.map(_.prose)).getOrElse(defaultProse(facts.san, reasons)), pvInterpretation)
      MoveReviewExplanation(
        title = title,
        prose = prose,
        qualityLabel = qualityLabel(truthContract),
        reasonTags = reasonTags,
        shortLine = shortLine,
        pvInterpretation = pvInterpretation,
        source = explanationSource(openingIdea, endgameIdea)
      )

  private def basicLaneAllowed(
      ctx: NarrativeContext,
      facts: MoveReviewBoardFacts.MoveFacts,
      pvInterpretation: Option[MoveReviewPvInterpretation]
  ): Boolean =
    facts.isCastle || isOpeningContext(ctx) || pvInterpretation.nonEmpty

  private def isOpeningContext(ctx: NarrativeContext): Boolean =
    ctx.header.phase.equalsIgnoreCase("Opening") ||
      ctx.phase.current.equalsIgnoreCase("Opening") ||
      ctx.openingData.nonEmpty ||
      ctx.openingEvent.nonEmpty

  private def defaultTitle(
      san: String,
      reasons: Set[String],
      pvInterpretation: Option[MoveReviewPvInterpretation]
  ): String =
    pvInterpretation.map(_.linePurpose) match
      case Some("resolve_capture_tension") => s"$san resolves the capture tension"
      case Some("answer_direct_threat")    => s"$san asks a concrete question"
      case Some("clarify_exchange")        => s"$san clarifies the exchange"
      case Some("improve_endgame_activity") => s"$san improves endgame activity"
      case _ =>
        if reasons.contains("king_safety") then s"$san improves king safety"
        else if reasons.contains("targets_f7_or_f2") then s"$san creates pressure near the king"
        else if reasons.contains("controls_center") && reasons.contains("develops_piece") then s"$san develops with central purpose"
        else if reasons.contains("controls_center") then s"$san fights for the center"
        else if reasons.contains("tempo") then s"$san gains time on a target"
        else if reasons.contains("endgame_technique") then s"$san improves the endgame setup"
        else s"$san gives the move a concrete job"

  private def defaultProse(san: String, reasons: Set[String]): String =
    val clauses = scala.collection.mutable.ListBuffer.empty[String]
    if reasons.contains("develops_piece") then clauses += "brings a minor piece into play"
    if reasons.contains("controls_center") then clauses += "adds control over the central squares"
    if reasons.contains("king_safety") then clauses += "improves king safety"
    if reasons.contains("tempo") then clauses += "uses a gain of time on an enemy target"
    if reasons.contains("targets_f7_or_f2") then clauses += "points pressure at the f-pawn near the king"
    if reasons.contains("opens_line") then clauses += "opens a line for later piece activity"
    if reasons.contains("defends_center_pawn") then clauses += "reinforces a central pawn"
    if reasons.contains("creates_basic_threat") then clauses += "creates a direct target for the opponent to answer"
    if reasons.contains("endgame_technique") then clauses += "improves king or pawn activity for the endgame"
    val purpose =
      clauses.toList match
        case Nil        => "serves a concrete local purpose"
        case one :: Nil => one
        case many       => many.dropRight(1).mkString(", ") + ", and " + many.last
    s"$san $purpose."

  private def enrichProseWithPv(base: String, pvInterpretation: Option[MoveReviewPvInterpretation]): String =
    pvInterpretation match
      case Some(interpretation) if interpretation.learningPoint.trim.nonEmpty =>
        val trimmed = base.trim
        val suffix = interpretation.learningPoint.trim
        if trimmed.endsWith(suffix) then trimmed
        else s"$trimmed $suffix"
      case _ => base

  private def enrichInterpretation(
      interpretation: Option[MoveReviewPvInterpretation],
      openingIdea: Option[OpeningIdeaCatalog.OpeningIdea],
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea],
      facts: MoveReviewBoardFacts.MoveFacts,
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[MoveReviewPvInterpretation] =
    interpretation.map { value =>
      value.copy(
        learningPoint = MoveReviewLearningPointRenderer.render(
          openingIdea = openingIdea,
          endgameIdea = endgameIdea,
          purpose = value.linePurpose,
          played = facts,
          reply = lineFacts.flatMap(_.reply),
          continuation = lineFacts.flatMap(_.continuation)
        )
      )
    }

  private def explanationSource(
      openingIdea: Option[OpeningIdeaCatalog.OpeningIdea],
      endgameIdea: Option[EndgameIdeaCatalog.EndgameIdea]
  ): String =
    if openingIdea.nonEmpty then "opening_idea"
    else if endgameIdea.nonEmpty then "endgame_idea"
    else "basic_move_explanation"

  private def qualityLabel(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.map(_.truthClass.toString)
