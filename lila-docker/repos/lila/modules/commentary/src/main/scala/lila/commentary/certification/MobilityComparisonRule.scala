package lila.commentary.certification

import chess.Color
import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object MobilityComparisonRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("MobilityComparison")
  val scope: CertificationScope = CertificationScope.Comparative
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("mobility_superiority")
  protected val helperTags: Vector[String] =
    Vector("mobility_balance_count", "mobility_gap_floor", "restriction_support_gate")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.ComparativeSuperiority)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val ownerLegalMoves = context.legalMoveCount(color)
    val rivalLegalMoves = context.legalMoveCount(!color)
    val gap = ownerLegalMoves - rivalLegalMoves
    val restrictionWitnesses =
      context.current.primaryWitnessesFor("short_run_slider_gate_restriction").filter(_.color.contains(color)) ++
        context.current.primaryWitnessesFor("duty_bound_defender").filter(_.color.contains(color))
    val restrictionSupportPresent =
      restrictionWitnesses.nonEmpty

    Option.when(ownerLegalMoves >= 18 && gap >= 1 && restrictionSupportPresent)(
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "owner_legal_moves" -> WitnessValue.Number(ownerLegalMoves),
          "rival_legal_moves" -> WitnessValue.Number(rivalLegalMoves),
          "mobility_gap" -> WitnessValue.Number(gap)
        ),
        support = support(
          indices = restrictionWitnesses.flatMap(_.support.rootIndices),
          targetSquares = restrictionWitnesses.flatMap(_.support.targetSquares)
        )
      )
    )
