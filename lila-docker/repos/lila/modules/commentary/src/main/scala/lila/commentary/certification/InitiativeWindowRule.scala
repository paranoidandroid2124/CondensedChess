package lila.commentary.certification

import chess.Color

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object InitiativeWindowRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("InitiativeWindow")
  val scope: CertificationScope = CertificationScope.Comparative
  val burdenTag: CertificationBurdenTag =
    CertificationBurdenTag("counterplay_denial_window")
  protected val helperTags: Vector[String] =
    Vector("initiative_window_contract", "rival_counterplay_source", "move_order_relevance_gate")
  override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
    Vector(CertificationSupportFamily("DevelopmentComparison"))
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(
      CertificationEvidencePurpose.CounterplayDenial,
      CertificationEvidencePurpose.BestDefenseSurvival
    )
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.Deferred

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val ownerAdvancedPawns = advancedPawnSquares(context, color)
    val rivalCounterplaySources =
      context.current.primaryWitnessesFor("pawn_push_break_contact_source").count(_.color.contains(!color))
    val legalGap = context.legalMoveCount(color) - context.legalMoveCount(!color)
    val developmentGap = developmentCount(context, color) - developmentCount(context, !color)

    Option.when(
      context.sideToMove == color &&
        ownerAdvancedPawns.size >= 2 &&
        legalGap >= 2 &&
        rivalCounterplaySources <= 1
    )(
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "owner_advanced_pawns" -> WitnessValue.SquareListValue(ownerAdvancedPawns),
          "owner_legal_moves" -> WitnessValue.Number(context.legalMoveCount(color)),
          "rival_legal_moves" -> WitnessValue.Number(context.legalMoveCount(!color)),
          "rival_counterplay_source_count" -> WitnessValue.Number(rivalCounterplaySources),
          "development_gap" -> WitnessValue.Number(developmentGap)
        ),
        support = support(targetSquares = ownerAdvancedPawns)
      )
    )
