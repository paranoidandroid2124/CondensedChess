package lila.commentary.analysis

import munit.FunSuite

class OpeningFamilyCatalogTest extends FunSuite:

  test("loads opening aliases and target squares from TSV rows") {
    val catalog =
      OpeningFamilyCatalog.fromTsvLines(
        List(
          "wire_key\tdisplay_name\tstructure_label\taliases\ttarget_squares",
          "pirc_modern\tPirc/Modern\tPirc/Modern\tpirc|pirc defense|modern defense\te4|d6"
        )
      )

    assert(catalog.openingMatchesFamily("Pirc Defense", "pirc_modern"), clue(catalog))
    assert(catalog.targetAllowed("pirc_modern", "e4"), clue(catalog))
    assert(catalog.targetAllowed("pirc_modern", "d6"), clue(catalog))
    assert(!catalog.targetAllowed("pirc_modern", "h4"), clue(catalog))
    assert(!catalog.openingMatchesFamily("Caro-Kann Defense", "pirc_modern"), clue(catalog))
  }

  test("matches catalog aliases with common variation suffixes") {
    val catalog =
      OpeningFamilyCatalog.fromTsvLines(
        List(
          "wire_key\tdisplay_name\tstructure_label\taliases\ttarget_squares",
          "gruenfeld\tGruenfeld Defense\tGruenfeld\tgruenfeld|gruenfeld defense\td4|c4|d5"
        )
      )

    assert(catalog.openingMatchesFamily("Gruenfeld Defense: Exchange Variation", "gruenfeld"), clue(catalog))
    assert(catalog.targetAllowed("gruenfeld", "d5"), clue(catalog))
    assert(!catalog.openingMatchesFamily("King's Indian Defense", "gruenfeld"), clue(catalog))
  }

  test("default catalog contains major opening families beyond the original starter set") {
    val catalog = OpeningFamilyCatalog.default

    List(
      "gruenfeld",
      "alekhine",
      "nimzowitsch",
      "reti",
      "bird",
      "dutch",
      "slav",
      "queens_indian",
      "bogo_indian",
      "kings_gambit"
    ).foreach { key =>
      assert(catalog.family(key).nonEmpty, clue(key))
    }
  }
