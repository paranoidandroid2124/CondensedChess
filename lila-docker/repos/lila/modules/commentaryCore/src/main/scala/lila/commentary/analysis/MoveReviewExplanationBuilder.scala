package lila.commentary.analysis

import chess.{ Color, Piece, Role, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.{ MoveReviewExplanation, MoveReviewRefs }
import lila.commentary.model.*

private[commentary] object MoveReviewExplanationBuilder:

  final case class PlayedMove(
      uci: String,
      san: String,
      from: Square,
      to: Square,
      piece: Piece,
      afterFen: String,
      capturedRole: Option[Role]
  ):
    def role: Role = piece.role
    def color: Color = piece.color
    def isWhite: Boolean = color.white
    def isCapture: Boolean = capturedRole.nonEmpty || san.contains("x")
    def isCastle: Boolean =
      san.startsWith("O-O") || Set("e1g1", "e1c1", "e8g8", "e8c8").contains(uci)
    def toKey: String = to.key

  final case class CanonicalEvidence(
      facts: List[Fact],
      motifs: List[Motif],
      openingGoal: Option[OpeningGoals.Evaluation],
      openingName: Option[String]
  ):
    def tacticalFacts: List[Fact] =
      facts.collect {
        case fact: Fact.Fork         => fact
        case fact: Fact.Pin          => fact
        case fact: Fact.Skewer       => fact
        case fact: Fact.HangingPiece => fact
        case fact: Fact.TargetPiece  => fact
        case fact: Fact.DoubleCheck  => fact
      }

    def endgameFacts: List[Fact] =
      facts.collect {
        case fact: Fact.KingActivity             => fact
        case fact: Fact.Opposition               => fact
        case fact: Fact.RuleOfSquare             => fact
        case fact: Fact.TriangulationOpportunity => fact
        case fact: Fact.RookEndgamePattern       => fact
        case fact: Fact.PawnPromotion            => fact
        case fact: Fact.Zugzwang                 => fact
      }

    def hasCaptureMotif: Boolean =
      motifs.exists(_.isInstanceOf[Motif.Capture])

  def build(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[MoveReviewExplanation] =
    for
      played <- current(ctx)
      lineFacts = MoveReviewPvFacts.firstCoupled(ctx.fen, played.uci, refs)
      evidence = canonicalEvidence(ctx, played)
      descriptor <- MoveReviewIdeaSurface.describe(played, evidence, lineFacts)
      pvInterpretation = descriptor.pvInterpretation(lineFacts)
      shortLine = MoveReviewPvFacts.shortLine(refs, pvInterpretation.flatMap(_.supportedByLineId))
    yield
      MoveReviewExplanation(
        title = descriptor.title,
        prose = descriptor.prose,
        qualityLabel = qualityLabel(truthContract),
        reasonTags = descriptor.reasonTags,
        shortLine = shortLine,
        pvInterpretation = pvInterpretation,
        source = descriptor.source
      )

  def current(ctx: NarrativeContext): Option[PlayedMove] =
    for
      uci <- ctx.playedMove.map(MoveReviewPvFacts.normalizeUci).filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
      san <- ctx.playedSan.map(_.trim).filter(_.nonEmpty)
      from <- Square.fromKey(uci.take(2))
      to <- Square.fromKey(uci.slice(2, 4))
      before <- Fen.read(Standard, Fen.Full(ctx.fen))
      piece <- before.board.pieceAt(from)
      afterFen <- MoveReviewPvChainValidator.legalFenAfter(ctx.fen, uci)
      after <- Fen.read(Standard, Fen.Full(afterFen))
      movedPiece <- after.board.pieceAt(to)
      if movedPiece.color == piece.color
    yield
      PlayedMove(
        uci = uci,
        san = san,
        from = from,
        to = to,
        piece = piece,
        afterFen = afterFen,
        capturedRole = before.board.pieceAt(to).filter(_.color != piece.color).map(_.role)
      )

  private def canonicalEvidence(ctx: NarrativeContext, played: PlayedMove): CanonicalEvidence =
    val playedCandidate =
      ctx.candidates.find(candidate =>
        candidate.uci.exists(uci => MoveReviewPvFacts.normalizeUci(uci) == played.uci) ||
          sanitizeSan(candidate.move) == sanitizeSan(played.san)
      )
    val facts =
      (playedCandidate.toList.flatMap(_.facts) ++ ctx.facts ++ ctx.mainPvFacts ++ ctx.threatLineFacts ++ ctx.counterfactualFacts)
        .distinct
    val motifs =
      playedCandidate.toList.flatMap(_.lineMotifs).distinct
    val openingGoal =
      ctx.openingGoalEvaluation
        .filter(goal => goal.status == OpeningGoals.Status.Achieved || goal.status == OpeningGoals.Status.Partial)
    CanonicalEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = ctx.openingData.flatMap(_.name).orElse(openingNameFromEvent(ctx))
    )

  private def qualityLabel(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.map(_.truthClass.toString)

  private def openingNameFromEvent(ctx: NarrativeContext): Option[String] =
    ctx.openingEvent.collect { case event: OpeningEvent.Intro => event.name }

  private def sanitizeSan(san: String): String =
    Option(san).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
