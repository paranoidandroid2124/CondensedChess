package lila.commentary.certification

import chess.{ Color, File, Queen, Rank }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object MateNetCertificationRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("MateNetCertification")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("forcing_mate_net")
  protected val helperTags: Vector[String] =
    Vector("mate_net_forcing_window", "best_defense_survival")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(
      CertificationEvidencePurpose.BestDefenseSurvival,
      CertificationEvidencePurpose.TacticalReleaseDetection
    )
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.Deferred

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val defender = !color
    val defenderKingSquare = context.current.board.kingSquare(defender)
    val edgeKing =
      defenderKingSquare.exists(square =>
        square.file == File.A || square.file == File.H || square.rank == Rank.First || square.rank == Rank.Eighth
      )
    val ownerQueenPresent = context.current.activePieceSquares(color, Queen).nonEmpty
    val attackedKingRing = attackedKingRingSquares(context, color, defender)
    val attackedShelter = attackedShelterSquares(context, color, defender)
    val attackStrength = attacksKingTheaterCount(context, color, defender)

    Option.when(
      ownerQueenPresent &&
        majorPieceCount(context, color) >= 2 &&
        edgeKing &&
        attackedKingRing.size >= 2 &&
        attackedShelter.nonEmpty &&
        attackStrength >= 5
    )(
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "major_piece_count" -> WitnessValue.Number(majorPieceCount(context, color)),
          "attacked_king_ring_squares" -> WitnessValue.SquareListValue(attackedKingRing),
          "attacked_shelter_squares" -> WitnessValue.SquareListValue(attackedShelter)
        ),
        support = support(
          targetSquares = attackedKingRing ++ attackedShelter
        )
      )
    )
