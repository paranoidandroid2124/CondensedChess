package lila.commentary.projection

import chess.{ Color, Square }

import lila.commentary.witness.{ WitnessAnchor, WitnessValue }
import lila.commentary.witness.seed.{
  StrategySupportSeed,
  StrategySupportSeedExtraction,
  StrategySupportSeedId,
  StrategySupportSeedScopeContract
}

object StrategyProjectionAdmission:

  def admits(
      bandId: StrategyProjectionBandId,
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Either[String, Boolean] =
    if !StrategyProjectionScopeContract.isStartReadyBandId(bandId) then
      Left(s"Unsupported projection admission band: ${bandId.value}")
    else if !evidence.matches(extraction.rootState) then
      Left("Strategy projection admission rejected stale evidence bundle")
    else
      Right(
        bandId.value match
          case "S17" => admitsS17(extraction, evidence, owner)
          case "S23" => admitsS23(extraction, evidence, owner)
          case "S24" => admitsS24(extraction, evidence, owner)
          case "S25" => admitsS25(extraction, evidence, owner)
          case other => throw MatchError(other)
      )

  private def admitsS17(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val anchors =
      seeds(extraction, StrategySupportSeedScopeContract.S17SamePieceLiabilityAnchor, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }

    anchors.exists: anchor =>
      val liveReliefKinds =
        Vector(
          Option.when(
            hasSeed(extraction, StrategySupportSeedScopeContract.S17SamePieceRepairRoute, owner, anchor)
          )("repair_route"),
          Option.when(
            hasSeed(extraction, StrategySupportSeedScopeContract.S17SamePieceExchangeRelief, owner, anchor)
          )("exchange_relief")
        ).flatten

      liveReliefKinds.exists: reliefKind =>
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S17,
            StrategyProjectionScopeContract.LiabilityReliefCertified,
            owner,
            anchor
          )
          .exists(claim => token(claim.payload, "relief_kind").contains(reliefKind))

  private def admitsS23(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    admitsKingEntry(extraction, evidence, owner) ||
      admitsKingOpposition(extraction, evidence, owner)

  private def admitsKingEntry(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val entrySquares =
      seeds(extraction, StrategySupportSeedScopeContract.S23KingEntrySquare, owner)
        .map(_.anchor)
        .collect { case WitnessAnchor.SquareAnchor(square) => square }

    entrySquares.exists: entrySquare =>
      seeds(extraction, StrategySupportSeedScopeContract.S23KingAccessRoute, owner)
        .exists(seed => square(seed.payload, "entry_square").contains(entrySquare)) &&
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S23,
            StrategyProjectionScopeContract.KingEntryConversionCertified,
            owner,
            WitnessAnchor.SquareAnchor(entrySquare)
          )
          .nonEmpty

  private def admitsKingOpposition(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    seeds(extraction, StrategySupportSeedScopeContract.S23KingOppositionContact, owner)
      .exists(seed =>
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S23,
            StrategyProjectionScopeContract.KingOppositionCertified,
            owner,
            seed.anchor
          )
          .nonEmpty
      )

  private def admitsS24(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val dependencyAnchors =
      seeds(extraction, StrategySupportSeedScopeContract.S24TargetResourceDependency, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }
        .toSet
    val convergenceAnchors =
      seeds(extraction, StrategySupportSeedScopeContract.S24TargetAttackConvergence, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }
        .toSet

    dependencyAnchors.intersect(convergenceAnchors).exists: anchor =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S24,
          StrategyProjectionScopeContract.SameTargetForcingRealization,
          owner,
          anchor
        )
        .nonEmpty &&
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S24,
            StrategyProjectionScopeContract.SameTargetConversionCertified,
            owner,
            anchor
          )
          .nonEmpty

  private def admitsS25(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    seeds(extraction, StrategySupportSeedScopeContract.S25RankCorridorState, owner)
      .exists: seed =>
        val entrySquare = square(seed.payload, "entry_square")
        val corridorKind = token(seed.payload, "corridor_kind")
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S25,
            StrategyProjectionScopeContract.RankAccessConsequenceCertified,
            owner,
            seed.anchor
          )
          .exists: claim =>
            entrySquare.nonEmpty &&
              entrySquare == square(claim.payload, "entry_square") &&
              corridorKind.contains("cross_wing_rank_switch") &&
              corridorKind == token(claim.payload, "corridor_kind")

  private def hasSeed(
      extraction: StrategySupportSeedExtraction,
      seedId: StrategySupportSeedId,
      owner: Color,
      anchor: WitnessAnchor
  ): Boolean =
    seeds(extraction, seedId, owner).exists(_.anchor == anchor)

  private def seeds(
      extraction: StrategySupportSeedExtraction,
      seedId: StrategySupportSeedId,
      owner: Color
  ): Vector[StrategySupportSeed] =
    extraction.seeds.forSeedId(seedId).filter(_.color.contains(owner))

  private def token(payload: lila.commentary.witness.WitnessPayload, field: String): Option[String] =
    payload.get(field).collect { case WitnessValue.Token(value) => value }

  private def square(payload: lila.commentary.witness.WitnessPayload, field: String): Option[Square] =
    payload.get(field).collect { case WitnessValue.SquareValue(value) => value }
