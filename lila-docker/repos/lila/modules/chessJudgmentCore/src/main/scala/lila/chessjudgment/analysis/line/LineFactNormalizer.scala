package lila.chessjudgment.analysis.line

import chess.{ Color, King, Position }
import chess.format.Fen
import chess.variant.Standard

import lila.chessjudgment.model.judgment.*

object LineFactNormalizer:

  def fromValidatedLine(
      id: String,
      lineRef: LineNodeRef,
      facts: PrincipalVariationEvidence.LineFacts,
      position: PositionNodeRef,
      scope: EvidenceScope,
      forcedTheme: Option[ForcedLineThemeEvidence] = None,
      materialSummary: Option[LineMaterialSummary] = None,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.LegalLineProducer,
        layer = EvidenceLayer.Line,
        position = position,
        line = Some(lineRef),
        scope = scope,
        confidence = EvidenceConfidence.LegalReplayVerified
      )
    EvidenceRecord(
      ref = ref,
      payload = LineFactEvidence(
        line = lineRef,
        firstMove = Some(facts.first.uci),
        replyMove = facts.reply.map(_.uci),
        continuationMoves = facts.continuation.toList.map(_.uci) ++ facts.continuationTail.map(_.uci),
        forcedTheme = forcedTheme,
        material = materialSummary
      )(
        replay = replaySteps(position.fen, facts),
        events = lineEvents(facts, forcedTheme, materialSummary),
        consequences = lineConsequences(facts, forcedTheme, materialSummary)
      ),
      parents = parents
    )

  def fromForcedTheme(theme: ForcedLineTruth.VerifiedTheme): ForcedLineThemeEvidence =
    ForcedLineThemeEvidence(
      id = theme.id,
      lineMoves = theme.lineMoves
    )

  private def replaySteps(startFen: String, facts: PrincipalVariationEvidence.LineFacts): List[LineReplayStep] =
    facts.line.moves.foldLeft((startFen, List.empty[LineReplayStep])) { case ((fenBefore, acc), move) =>
      move.fenAfter -> (acc :+ LineReplayStep(
        ply = move.ply,
        moveUci = move.uci,
        fenBefore = fenBefore,
        fenAfter = move.fenAfter
      ))
    }._2

  private def lineEvents(
      facts: PrincipalVariationEvidence.LineFacts,
      forcedTheme: Option[ForcedLineThemeEvidence],
      materialSummary: Option[LineMaterialSummary]
  ): List[LineMoveEvent] =
    val replayEvents =
      facts.line.moves.zipWithIndex.flatMap { case (move, index) =>
        val normalized = PrincipalVariationEvidence.normalizeUci(move.uci)
        val stateEvents =
          positionAfter(move.fenAfter).toList.flatMap { position =>
            List(
              Option.when(position.checkMate)(
                LineMoveEvent(
                  kind = LineEventKind.Mate,
                  moveUci = normalized,
                  plyOffset = index,
                  side = Some(!position.color),
                  pieceRole = Some(EvidencePieceRole(King.name)),
                  square = position.board.kingPosOf(position.color).map(square => EvidenceSquare(square.key))
                )
              ),
              Option.when(position.check.yes && !position.checkMate)(
                LineMoveEvent(
                  kind = LineEventKind.Check,
                  moveUci = normalized,
                  plyOffset = index,
                  side = Some(!position.color),
                  pieceRole = Some(EvidencePieceRole(King.name)),
                  square = position.board.kingPosOf(position.color).map(square => EvidenceSquare(square.key))
                )
              ),
              Option.when(position.staleMate)(
                LineMoveEvent(
                  kind = LineEventKind.Stalemate,
                  moveUci = normalized,
                  plyOffset = index,
                  side = Some(!position.color),
                  pieceRole = Some(EvidencePieceRole(King.name)),
                  square = position.board.kingPosOf(position.color).map(square => EvidenceSquare(square.key))
                )
              )
            ).flatten
          }
        castlingEvent(normalized, index).toList ++ stateEvents
      }
    val forcedEvents =
      forcedTheme.toList.flatMap(theme =>
        theme.lineMoves.headOption.toList.flatMap(move =>
          List(
            LineMoveEvent(
              kind = LineEventKind.ForcedTheme,
              moveUci = move,
              plyOffset = 0
            ),
            LineMoveEvent(
              kind = LineEventKind.Threat,
              moveUci = move,
              plyOffset = 0
            )
          )
        )
      )
    val materialEvents =
      materialSummary.toList.flatMap { summary =>
        val captureEvents =
          summary.captures.flatMap { capture =>
            val captureEvent =
              LineMoveEvent(
                kind = if capture.recapture then LineEventKind.Recapture else LineEventKind.Capture,
                moveUci = capture.moveUci,
                plyOffset = capture.plyOffset,
                side = Some(capture.side),
                pieceRole = Some(capture.attackerRole),
                targetRole = Some(capture.capturedRole),
                square = Some(capture.square)
              )
            captureEvent :: Option
              .when(capture.recapture)(
                LineMoveEvent(
                  kind = LineEventKind.DefenderMove,
                  moveUci = capture.moveUci,
                  plyOffset = capture.plyOffset,
                  side = Some(capture.side),
                  pieceRole = Some(capture.attackerRole),
                  targetRole = Some(capture.capturedRole),
                  square = Some(capture.square)
                )
              )
              .toList
          }
        val promotionEvents =
          Option.when(summary.hasPromotion)(
            LineMoveEvent(
              kind = LineEventKind.Promotion,
              moveUci = summary.captures.lastOption.map(_.moveUci).getOrElse(""),
              plyOffset = summary.captures.lastOption.map(_.plyOffset).getOrElse(0)
            )
          ).toList
        captureEvents ++ promotionEvents
      }
    (replayEvents ++ forcedEvents ++ materialEvents).distinct

  private def lineConsequences(
      facts: PrincipalVariationEvidence.LineFacts,
      forcedTheme: Option[ForcedLineThemeEvidence],
      materialSummary: Option[LineMaterialSummary]
  ): List[LineConsequence] =
    val outcome =
      facts.line.moves.zipWithIndex.flatMap { case (move, index) =>
        val normalized = PrincipalVariationEvidence.normalizeUci(move.uci)
        positionAfter(move.fenAfter).toList.flatMap { position =>
          val prefix = facts.line.moves.take(index + 1).map(_.uci)
          List(
            Option.when(position.checkMate)(
              LineConsequence(LineConsequenceKind.Mate, prefix, proofSignal = true, eventMove = Some(normalized))
            ),
            Option.when(position.staleMate)(
              LineConsequence(
                LineConsequenceKind.DrawResource,
                prefix,
                proofSignal = true,
                eventMove = Some(normalized)
              )
            )
          ).flatten
        }
      }
    val forced =
      forcedTheme.toList.map(theme =>
        LineConsequence(
          kind =
            if theme.id == ForcedLineTruth.ImmediateReplyCheckId then LineConsequenceKind.ImmediateReplyCheck
            else LineConsequenceKind.ForcedTheme,
          lineMoves = theme.lineMoves,
          proofSignal = ForcedLineTruth.isProofSignalThemeId(theme.id),
          eventMove = theme.lineMoves.headOption
        )
      )
    val material =
      materialSummary.toList.flatMap { summary =>
        List(
          Option.when(summary.hasProofSignalMaterialGain || summary.hasUnrecoveredPawnGainForMover)(
            LineConsequence(
              LineConsequenceKind.MaterialGain,
              summary.captures.map(_.moveUci),
              proofSignal = summary.hasProofSignalMaterialGain
            )
          ),
          Option.when(summary.hasProofSignalMaterialLoss || summary.hasUnrecoveredPawnLossForMover)(
            LineConsequence(
              LineConsequenceKind.MaterialLoss,
              summary.captures.map(_.moveUci),
              proofSignal = summary.hasProofSignalMaterialLoss
            )
          ),
          Option.when(summary.hasResolvedMaterialSequence)(
            LineConsequence(LineConsequenceKind.RecaptureSequence, summary.captures.map(_.moveUci), proofSignal = true)
          ),
          Option.when(summary.hasRecoveryWindow)(
            LineConsequence(LineConsequenceKind.RecoveryWindow, summary.captures.map(_.moveUci), proofSignal = true)
          ),
          Option.when(summary.hasSacrificeMaterialEvent)(
            LineConsequence(LineConsequenceKind.Sacrifice, summary.captures.map(_.moveUci), proofSignal = true)
          ),
          Option.when(summary.hasPromotion)(
            LineConsequence(LineConsequenceKind.PromotionRace, summary.captures.map(_.moveUci), proofSignal = true)
          )
        ).flatten
      }
    (outcome ++ forced ++ material).distinct

  private def castlingEvent(moveUci: String, plyOffset: Int): Option[LineMoveEvent] =
    castlingSide(moveUci).map(side =>
      LineMoveEvent(
        kind = LineEventKind.Castling,
        moveUci = moveUci,
        plyOffset = plyOffset,
        side = Some(side),
        pieceRole = Some(EvidencePieceRole(King.name)),
        square = destinationSquare(moveUci)
      )
    )

  private def castlingSide(moveUci: String): Option[Color] =
    moveUci match
      case "e1g1" | "e1c1" => Some(Color.White)
      case "e8g8" | "e8c8" => Some(Color.Black)
      case _               => None

  private def destinationSquare(moveUci: String): Option[EvidenceSquare] =
    Option.when(moveUci.length >= 4)(EvidenceSquare(moveUci.slice(2, 4)))

  private def positionAfter(fen: String): Option[Position] =
    Fen.read(Standard, Fen.Full(fen))
