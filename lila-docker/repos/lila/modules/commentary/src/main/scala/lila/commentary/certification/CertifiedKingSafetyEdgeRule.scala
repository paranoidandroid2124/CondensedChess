package lila.commentary.certification

import chess.Color

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object CertifiedKingSafetyEdgeRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("CertifiedKingSafetyEdge")
  val scope: CertificationScope = CertificationScope.Comparative
  val burdenTag: CertificationBurdenTag =
    CertificationBurdenTag("king_safety_edge_certification")
  protected val helperTags: Vector[String] =
    Vector(
      "attack_host_viability",
      "attacker_budget_present",
      "move_order_relevance_gate",
      "best_defense_survival",
      "major_piece_presence"
    )
  override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
    Vector(
      CertificationSupportFamily("AttackScaffold"),
      CertificationSupportFamily("ComparativeKingFragility")
    )
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(
      CertificationEvidencePurpose.ComparativeSuperiority,
      CertificationEvidencePurpose.BestDefenseSurvival
    )
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.Deferred

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val defender = !color
    val attackedSquares = attackedKingRingSquares(context, color, defender)
    val attackedShelter = attackedShelterSquares(context, color, defender)
    val attackStrength = attacksKingTheaterCount(context, color, defender)
    val majorPieces = majorPieceCount(context, color)
    val attackScaffoldPresent =
      context.current.board.kingSquare(defender).exists(square =>
        squareObject(context, "AttackScaffold", color, square).nonEmpty
      )
    val comparativeFragilityPresent =
      supportiveVerdict(extractedSoFar, "ComparativeKingFragility", color)

    Option.when(
      context.sideToMove == color &&
        attackScaffoldPresent &&
        comparativeFragilityPresent &&
        majorPieces >= 1 &&
        attackStrength >= 2 &&
        (attackedSquares.nonEmpty || attackedShelter.nonEmpty)
    )(
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "attacked_king_ring_squares" -> WitnessValue.SquareListValue(attackedSquares),
          "attacked_shelter_squares" -> WitnessValue.SquareListValue(attackedShelter),
          "major_piece_count" -> WitnessValue.Number(majorPieces),
          "attack_strength" -> WitnessValue.Number(attackStrength)
        ),
        support = support(
          targetSquares = attackedSquares ++ attackedShelter
        )
      )
    )
