package lila.chessjudgment.analysis.line

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
        replay = replaySteps(facts),
        events = lineEvents(forcedTheme, materialSummary),
        consequences = lineConsequences(forcedTheme, materialSummary)
      ),
      parents = parents
    )

  def fromForcedTheme(theme: ForcedLineTruth.VerifiedTheme): ForcedLineThemeEvidence =
    ForcedLineThemeEvidence(
      id = theme.id,
      lineMoves = theme.lineMoves
    )

  private def replaySteps(facts: PrincipalVariationEvidence.LineFacts): List[LineReplayStep] =
    facts.line.moves.map(move =>
      LineReplayStep(
        ply = move.ply,
        moveUci = move.uci,
        fenAfter = move.fenAfter
      )
    )

  private def lineEvents(
      forcedTheme: Option[ForcedLineThemeEvidence],
      materialSummary: Option[LineMaterialSummary]
  ): List[LineMoveEvent] =
    val forcedEvents =
      forcedTheme.toList.flatMap(theme =>
        theme.lineMoves.headOption.map(move =>
          LineMoveEvent(
            kind = LineEventKind.ForcedTheme,
            moveUci = move,
            plyOffset = 0
          )
        )
      )
    val materialEvents =
      materialSummary.toList.flatMap { summary =>
        val captureEvents =
          summary.captures.map(capture =>
            LineMoveEvent(
              kind = if capture.recapture then LineEventKind.Recapture else LineEventKind.Capture,
              moveUci = capture.moveUci,
              plyOffset = capture.plyOffset,
              side = Some(capture.side),
              pieceRole = Some(capture.attackerRole),
              targetRole = Some(capture.capturedRole),
              square = Some(capture.square)
            )
          )
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
    (forcedEvents ++ materialEvents).distinct

  private def lineConsequences(
      forcedTheme: Option[ForcedLineThemeEvidence],
      materialSummary: Option[LineMaterialSummary]
  ): List[LineConsequence] =
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
    (forced ++ material).distinct
