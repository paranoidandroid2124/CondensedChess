package lila.accountintel

import chess.Color

import lila.accountintel.opening.CanonicalOpeningBook

class CanonicalOpeningBookTest extends munit.FunSuite:

  test("classifies white KID positions as facing KID"):
    val classified =
      CanonicalOpeningBook.classify(
        pgn = "1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6",
        subjectColor = Color.White,
        providerOpeningName = None,
        providerEcoCode = None,
        providerEcoUrl = None
      )

    assertEquals(classified.family, "King's Indian Defense")
    assertEquals(classified.relation, "faced")
    assertEquals(classified.bucket, "faced King's Indian Defense")
    assertEquals(classified.ecoCode, Some("E70"))

  test("classifies white Catalan positions as choosing Catalan"):
    val classified =
      CanonicalOpeningBook.classify(
        pgn = "1. d4 Nf6 2. c4 e6 3. g3 d5",
        subjectColor = Color.White,
        providerOpeningName = None,
        providerEcoCode = None,
        providerEcoUrl = None
      )

    assertEquals(classified.family, "Catalan Opening")
    assertEquals(classified.relation, "chose")
    assertEquals(classified.bucket, "chose Catalan Opening")
    assertEquals(classified.ecoCode, Some("E00"))

  test("classifies black Najdorf positions as choosing Najdorf"):
    val classified =
      CanonicalOpeningBook.classify(
        pgn = "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6",
        subjectColor = Color.Black,
        providerOpeningName = None,
        providerEcoCode = None,
        providerEcoUrl = None
      )

    assertEquals(classified.family, "Sicilian Defense: Najdorf Variation")
    assertEquals(classified.relation, "chose")
    assertEquals(classified.bucket, "chose Sicilian Defense: Najdorf Variation")
    assertEquals(classified.ecoCode, Some("B90"))

  test("covers every ECO code from A00 to E99"):
    assertEquals(CanonicalOpeningBook.missingEcoPerspectiveCodes, Set.empty[String])
