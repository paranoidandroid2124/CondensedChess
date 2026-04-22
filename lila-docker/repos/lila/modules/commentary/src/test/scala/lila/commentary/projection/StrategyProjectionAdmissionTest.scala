package lila.commentary.projection

import chess.{ Color, Square }
import chess.format.Fen

import lila.commentary.CommentaryCore
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.witness.seed.StrategySupportSeedExtractor

class StrategyProjectionAdmissionTest extends munit.FunSuite:

  test("S17/S23/S24/S25 projection rows expose start-ready admission contracts"):
    assertEquals(
      StrategyProjectionScopeContract.startReadyBandIds.map(_.value),
      Vector("S17", "S23", "S24", "S25")
    )
    assertEquals(
      CommentaryCore.strategyProjectionStartReadyBandIds,
      Vector("S17", "S23", "S24", "S25")
    )
    assertEquals(
      StrategyProjectionScopeContract.requiredEvidenceKindsByBand.view.mapValues(_.map(_.value)).toMap,
      Map(
        "S17" -> Vector("liability_relief_certified"),
        "S23" -> Vector("king_entry_conversion_certified", "king_opposition_certified"),
        "S24" -> Vector("same_target_forcing_realization", "same_target_conversion_certified"),
        "S25" -> Vector("rank_access_consequence_certified")
      )
    )

  test("S17 admits only same-piece liability plus same-piece relief with certified relief evidence"):
    val fen = "4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("d5"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S17"),
        extraction,
        evidence,
        Color.Black
      ),
      Right(true)
    )

  test("S17 rejects liability-only boards even when a relief evidence claim is supplied"):
    val fen = "6kb/5Npp/8/8/8/8/8/4K3 b - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("h8"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S17"),
        extraction,
        evidence,
        Color.Black
      ),
      Right(false)
    )

  test("S23 admits king entry only when route and entry-conversion evidence share the entry square"):
    val fen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"
    val extraction = seedExtraction(fen)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_entry_conversion_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5")
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        evidence,
        Color.White
      ),
      Right(true)
    )

  test("S23 admits direct opposition only with an opposition-specific evidence claim"):
    val fen = "8/8/4k3/8/4K3/8/8/8 b - - 0 1"
    val extraction = seedExtraction(fen)
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val evidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S23"),
        kind = StrategyProjectionEvidenceKind("king_opposition_certified"),
        owner = Color.White,
        anchor = squareAnchor("e5")
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        noEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S23"),
        extraction,
        evidence,
        Color.White
      ),
      Right(true)
    )

  test("S24 admits only when dependency, convergence, forcing, and conversion all share one target"):
    val fen = "6k1/8/4b3/3q4/8/8/6B1/3R2K1 w - - 0 1"
    val extraction = seedExtraction(fen)
    val sameTargetEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_forcing_realization"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      ),
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_conversion_certified"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      )
    )
    val wrongTargetEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_forcing_realization"),
        owner = Color.White,
        anchor = pieceAnchor("d5")
      ),
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S24"),
        kind = StrategyProjectionEvidenceKind("same_target_conversion_certified"),
        owner = Color.White,
        anchor = pieceAnchor("e6")
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S24"),
        extraction,
        sameTargetEvidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S24"),
        extraction,
        wrongTargetEvidence,
        Color.White
      ),
      Right(false)
    )

  test("S25 admits only when rank corridor evidence names the same source, entry, and corridor kind"):
    val fen = "6k1/8/8/8/R7/8/8/6K1 w - - 0 1"
    val extraction = seedExtraction(fen)
    val sameCorridorEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )
    val noEvidence = StrategyProjectionEvidence.forSeedExtraction(extraction, Vector.empty)
    val wrongEntryEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("g4"))
        )
      )
    )
    val wrongKindEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("a4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("generic_rook_lift"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )
    val wrongAnchorEvidence = evidenceFor(
      extraction,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S25"),
        kind = StrategyProjectionEvidenceKind("rank_access_consequence_certified"),
        owner = Color.White,
        anchor = pieceAnchor("b4"),
        payload = WitnessPayload(
          "corridor_kind" -> WitnessValue.Token("cross_wing_rank_switch"),
          "entry_square" -> WitnessValue.SquareValue(squareFromKey("f4"))
        )
      )
    )

    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        sameCorridorEvidence,
        Color.White
      ),
      Right(true)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        noEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongEntryEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongKindEvidence,
        Color.White
      ),
      Right(false)
    )
    assertEquals(
      StrategyProjectionAdmission.admits(
        StrategyProjectionBandId("S25"),
        extraction,
        wrongAnchorEvidence,
        Color.White
      ),
      Right(false)
    )

  test("projection admission rejects stale non-empty evidence bundles"):
    val current = seedExtraction("4k3/8/8/3b4/5N2/8/8/4K3 b - - 0 1")
    val staleSource = seedExtraction("4k3/8/8/3b4/5N2/8/6B1/7K b - - 0 1")
    val staleEvidence = evidenceFor(
      staleSource,
      StrategyProjectionEvidenceClaim(
        bandId = StrategyProjectionBandId("S17"),
        kind = StrategyProjectionEvidenceKind("liability_relief_certified"),
        owner = Color.Black,
        anchor = pieceAnchor("d5"),
        payload = WitnessPayload("relief_kind" -> WitnessValue.Token("repair_route"))
      )
    )

    assert(
      StrategyProjectionAdmission
        .admits(StrategyProjectionBandId("S17"), current, staleEvidence, Color.Black)
        .left
        .exists(_.contains("stale"))
    )

  private def seedExtraction(fen: String) =
    StrategySupportSeedExtractor
      .fromFen(Fen.Full.clean(fen))
      .fold(message => fail(message), identity)

  private def evidenceFor(
      extraction: lila.commentary.witness.seed.StrategySupportSeedExtraction,
      claims: StrategyProjectionEvidenceClaim*
  ): StrategyProjectionEvidence =
    StrategyProjectionEvidence.forSeedExtraction(extraction, claims)

  private def squareAnchor(square: String): WitnessAnchor =
    WitnessAnchor.SquareAnchor(squareFromKey(square))

  private def pieceAnchor(square: String): WitnessAnchor =
    WitnessAnchor.PieceSquareAnchor(squareFromKey(square))

  private def squareFromKey(square: String): Square =
    Square.fromKey(square).getOrElse(fail(s"bad square $square"))
