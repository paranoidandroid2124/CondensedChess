package lila.commentary.analysis.claim

import java.nio.file.{ Files, Paths }

import lila.commentary.analysis.NarrativeUtils
import lila.commentary.analysis.OpeningNameLookup

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
  private val GruenfeldFen =
    NarrativeUtils.uciListToFen(InitialFen, List("d2d4", "g8f6", "c2c4", "g7g6", "b1c3", "d7d5"))
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

  test("admits catalog-only opening-family claim keys without extending the enum facade") {
    assertEquals(FamilyId.fromWireKey("gruenfeld"), None)

    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim("gruenfeld"),
        proof(opening = "Gruenfeld Defense: Exchange Variation", fen = GruenfeldFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("admits transposed endpoint aliases from the same static opening-book FEN") {
    val lookup =
      OpeningNameLookup.fromTsvLines(
        List(
          "eco\tname\tpgn",
          "D06\tQueen's Gambit\t1. d4 d5 2. c4",
          "A13\tEnglish Opening: Anglo-Queen's Gambit\t1. c4 d5 2. d4"
        )
      )
    val transposedFen =
      NarrativeUtils.uciListToFen(InitialFen, List("c2c4", "d7d5", "d2d4"))

    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim("english"),
        proof(opening = "English Opening: Anglo-Queen's Gambit", fen = transposedFen),
        openingLookup = lookup
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("unknown catalog family keys fail closed") {
    val decision =
      OpeningFamilyClaimResolver.decideOpeningFamilyClaim(
        FamilyClaim("not_a_catalog_family"),
        proof(opening = "Gruenfeld Defense", fen = GruenfeldFen)
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assertEquals(
      decision.toList.flatMap(_.failureCodes).lastOption,
      Some("opening_family_mismatch:not_a_catalog_family"),
      clue(decision)
    )
  }

  test("resolver delegates opening metadata to the catalog instead of local alias rows") {
    val source =
      Files.readString(
        Paths.get(
          "modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/OpeningFamilyClaimResolver.scala"
        )
      )

    assert(source.contains("OpeningFamilyCatalog"), clue(source))
    assert(!source.contains("aliases = List("), clue(source))
  }
