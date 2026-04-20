package lila.commentary.certification

import chess.{ Bishop, Color, Queen, Rook }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.strategic.StrategicObjectHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object ComparativeKingFragilityRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("ComparativeKingFragility")
  val scope: CertificationScope = CertificationScope.Comparative
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("king_fragility_asymmetry")
  protected val helperTags: Vector[String] =
    Vector("king_theater_fragility_bundle", "king_fragility_asymmetry")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.ComparativeSuperiority)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val defender = !color
    val defenderHomeWingKing = homeWingKingSquare(context.current, defender)
    val ownerHomeWingKing = homeWingKingSquare(context.current, color)
    val defenderHoles = homeShelterHoles(context.current, defender)
    val ownerHoles = homeShelterHoles(context.current, color)
    val attackedShelterSquaresForDefender = attackedShelterSquares(context, color, defender)
    val attackedKingRingSquaresForDefender = attackedKingRingSquares(context, color, defender)
    val attackedShelterSquaresForOwner = attackedShelterSquares(context, defender, color)
    val attackedKingRingSquaresForOwner = attackedKingRingSquares(context, defender, color)
    val theaterAttackSources =
      kingRingAndShelterMask(context.current, defender).toVector
        .flatMap(square => context.current.board.attackersOf(square, color))
        .distinct
        .sortBy(_.value)
    val sourceRoles =
      theaterAttackSources.flatMap(square => context.current.pieceAt(square).map(_.role)).toSet
    val filePressurePresent =
      sourceRoles.exists(role => role == Rook || role == Queen)
    val diagonalPressurePresent =
      sourceRoles.exists(role => role == Bishop || role == Queen)
    val defenderScore =
      defenderHoles.size + attackedShelterSquaresForDefender.size + attackedKingRingSquaresForDefender.size
    val ownerScore =
      ownerHoles.size + attackedShelterSquaresForOwner.size + attackedKingRingSquaresForOwner.size
    val fragilityGap = defenderScore - ownerScore

    Option.when(
      defenderHomeWingKing.nonEmpty &&
        ownerHomeWingKing.nonEmpty &&
        (attackedShelterSquaresForDefender.nonEmpty || attackedKingRingSquaresForDefender.nonEmpty) &&
        filePressurePresent &&
        diagonalPressurePresent &&
        fragilityGap >= 2
    )(
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "defender_holes" -> WitnessValue.SquareListValue(defenderHoles),
          "owner_holes" -> WitnessValue.SquareListValue(ownerHoles),
          "attacked_shelter_squares" -> WitnessValue.SquareListValue(attackedShelterSquaresForDefender),
          "attacked_king_ring_squares" -> WitnessValue.SquareListValue(attackedKingRingSquaresForDefender),
          "defender_fragility_score" -> WitnessValue.Number(defenderScore),
          "owner_fragility_score" -> WitnessValue.Number(ownerScore),
          "theater_source_squares" -> WitnessValue.SquareListValue(theaterAttackSources)
        ),
        support = CertificationHelpers.support(
          indices = rootIndicesForSquares(context.current, SchemaId.KingShelterHole, color, defenderHoles),
          targetSquares =
            defenderHoles ++ attackedShelterSquaresForDefender ++ attackedKingRingSquaresForDefender
        )
      )
    )
