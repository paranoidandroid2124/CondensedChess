package lila.commentary.analysis.claim

class OpeningFamilyClaimResolverTest extends munit.FunSuite:

  private val OpenGamesFen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
  private val CaroKannFen =
    "rnbqkbnr/ppp1pppp/2p5/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 3"
  private val SicilianFen =
    "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

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

  test("suppresses opening-family claims when label and board proof both mismatch") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This plan is typical in Open Games (1.e4 e5).",
        proof(opening = "Catalan Opening", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assertEquals(
      decision.toList.flatMap(_.failureCodes),
      List("opening_family_label_mismatch", "opening_family_structure_mismatch", "opening_family_mismatch:open_games"),
      clue(decision)
    )
  }

  test("admits opening-family claims when the opening label matches") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This plan is typical in Open Games (1.e4 e5).",
        proof(opening = "Italian Game", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("admits opening-family claims when exact board structure matches despite label drift") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This thematic break requires a Sicilian structure.",
        proof(opening = "French Defense", fen = SicilianFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("admits opening-family claims from FEN proof even without an opening label") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This plan is typical in Open Games (1.e4 e5).",
        proofWithOpening(opening = None, fen = OpenGamesFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("suppresses mixed-family sentences when any claimed family lacks label and board proof") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This is typical in Open Games (1.e4 e5), but also requires a French structure.",
        proof(opening = "Italian Game", fen = OpenGamesFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assert(
      decision.toList.flatMap(_.failureCodes).contains("opening_family_mismatch:french"),
      clue(decision)
    )
  }

  test("does not turn Caro-Kann into a Sicilian Kan claim") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This setup requires a Caro-Kann structure.",
        proof(opening = "Queen's Gambit Declined", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("does not admit a Sicilian claim from Caro-Kann label substring") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This thematic break requires a Sicilian structure.",
        proof(opening = "Caro-Kann Defense", fen = CaroKannFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assert(
      decision.toList.flatMap(_.failureCodes).contains("opening_family_mismatch:sicilian"),
      clue(decision)
    )
  }

  test("does not decide opening-family claims outside the opening proof window") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        "This plan is typical in Open Games (1.e4 e5).",
        proof(opening = "Catalan Opening", fen = OpenGamesFen, phase = "middlegame", ply = 35)
      )

    assertEquals(decision, None)
  }
