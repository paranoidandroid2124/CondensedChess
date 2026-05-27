package lila.commentary.analysis.claim

import lila.commentary.analysis.NarrativeUtils

class OpeningFamilyClaimResolverTest extends munit.FunSuite:

  private val FamilyClaim = OpeningFamilyClaimResolver.OpeningFamilyClaim
  private val FamilyId = OpeningFamilyClaimResolver.OpeningFamilyId

  private val InitialFen =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val OpenGamesFen =
    NarrativeUtils.uciListToFen(InitialFen, List("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"))
  private val CaroKannFen =
    NarrativeUtils.uciListToFen(InitialFen, List("e2e4", "c7c6", "d2d4", "d7d5"))
  private val SicilianFen =
    NarrativeUtils.uciListToFen(InitialFen, List("e2e4", "c7c5", "g1f3"))
  private val CoincidentalSicilianShapeFen =
    "4k3/8/8/2p5/4P3/8/8/4K3 w - - 0 1"

  private def proofWithOpening(
      opening: Option[String],
      fen: String,
      phase: String = "opening",
      ply: Int = 9
  ): OpeningFamilyClaimResolver.OpeningFamilyMatchProof =
    OpeningFamilyClaimResolver.OpeningFamilyMatchProof(
      opening = opening,
      phase = phase,
      ply = ply,
      fen = Some(fen)
    )

  private def proof(
      opening: String,
      fen: String,
      phase: String = "opening",
      ply: Int = 9
  ): OpeningFamilyClaimResolver.OpeningFamilyMatchProof =
    proofWithOpening(Some(opening), fen, phase, ply)

  test("suppresses structured opening-family claims when label and board proof both mismatch") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.OpenGames),
        proof(opening = "Catalan Opening", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assertEquals(
      decision.toList.flatMap(_.failureCodes),
      List("opening_family_label_mismatch", "opening_family_structure_mismatch", "opening_family_mismatch:open_games"),
      clue(decision)
    )
  }

  test("suppresses structured opening-family claims when only the opening label matches") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.OpenGames),
        proof(opening = "Italian Game", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
  }

  test("suppresses structured opening-family claims when only shallow board structure matches") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.Sicilian),
        proof(opening = "French Defense", fen = SicilianFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
  }

  test("suppresses opening-family claims when only coincidental piece placement matches") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.Sicilian),
        proof(opening = "Sicilian Defense", fen = CoincidentalSicilianShapeFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
  }

  test("suppresses structured opening-family claims from FEN proof without an opening label") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.OpenGames),
        proofWithOpening(opening = None, fen = OpenGamesFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
  }

  test("admits structured opening-family claims when label and board structure both match") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.OpenGames),
        proof(opening = "Italian Game", fen = OpenGamesFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("suppresses structured Caro-Kann claims when only FEN proof matches") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.CaroKann),
        proof(opening = "Queen's Gambit Declined", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
  }

  test("does not decide opening-family claims outside the opening proof window") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim(FamilyId.OpenGames),
        proof(opening = "Catalan Opening", fen = OpenGamesFen, phase = "middlegame", ply = 35)
      )

    assertEquals(decision, None)
  }
