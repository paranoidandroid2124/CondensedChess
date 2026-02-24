package lila.llm

class RuleTemplateSanitizerTest extends munit.FunSuite:

  private val OpenGamesFen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
  private val CatalanLikeFen =
    "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/5NP1/PP2PP1P/RNBQKB1R w KQkq - 0 4"
  private val FrenchLikeFen =
    "rnbqkbnr/ppp2ppp/4p3/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 3"
  private val SicilianFen =
    "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
  private val CaroKannFen =
    "rnbqkbnr/ppp1pppp/2p5/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 3"
  private val ScandinavianFen =
    "rnbqkbnr/pppp1ppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
  private val EnglishFen =
    "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1"

  test("neutralizes open-games claim when opening context is non-open-game") {
    val input =
      "This looks like an attempt at d5 Equalizer, but this stabilizing move is typical in Open Games (1.e4 e5), but the current pawn structure is different at this moment."
    val out = RuleTemplateSanitizer.sanitize(
      text = input,
      opening = Some("Catalan Opening"),
      phase = "opening",
      ply = 7,
      fen = Some(CatalanLikeFen)
    )

    assert(!out.toLowerCase.contains("open games"))
    assert(out.contains("opening-family claim"))
  }

  test("keeps open-games claim when opening context is compatible") {
    val input =
      "This looks like an attempt at d5 Equalizer, but this stabilizing move is typical in Open Games (1.e4 e5), but the current pawn structure is different at this moment."
    val out = RuleTemplateSanitizer.sanitize(
      text = input,
      opening = Some("Italian Game"),
      phase = "opening",
      ply = 7,
      fen = Some(OpenGamesFen)
    )

    assert(out.contains("Open Games (1.e4 e5)"))
    assert(!out.contains("opening-family claim"))
  }

  test("neutralizes Sicilian structure claim when opening is different") {
    val input =
      "While Sicilian Liberator is a common theme, this thematic break requires a Sicilian structure (c5 pawn or traded c-pawn) here."
    val out = RuleTemplateSanitizer.sanitize(
      text = input,
      opening = Some("French Defense"),
      phase = "opening",
      ply = 9,
      fen = Some(SicilianFen)
    )

    assert(out.contains("Sicilian"))
    assert(!out.contains("opening-family claim"))
  }

  test("neutralizes Sicilian structure claim when both label and structure mismatch") {
    val input =
      "While Sicilian Liberator is a common theme, this thematic break requires a Sicilian structure (c5 pawn or traded c-pawn) here."
    val out = RuleTemplateSanitizer.sanitize(
      text = input,
      opening = Some("French Defense"),
      phase = "opening",
      ply = 9,
      fen = Some(CaroKannFen)
    )

    assert(!out.toLowerCase.contains("sicilian"))
    assert(out.contains("opening-family claim"))
  }

  test("does not sanitize family mismatch text in late middlegame") {
    val input =
      "While Sicilian Liberator is a common theme, this thematic break requires a Sicilian structure (c5 pawn or traded c-pawn) here."
    val out = RuleTemplateSanitizer.sanitize(
      text = input,
      opening = Some("French Defense"),
      phase = "middlegame",
      ply = 35,
      fen = Some(CaroKannFen)
    )

    assert(out.contains("Sicilian structure"))
    assert(!out.contains("opening-family claim"))
  }

  test("handles multiple opening-family markers during opening stage") {
    case class Scenario(input: String, opening: String, fen: String, shouldNeutralize: Boolean)

    val scenarios = List(
      Scenario("This thematic break requires a French structure (e6 and d5 pawns).", "French Defense", FrenchLikeFen, false),
      Scenario("This thematic break requires a French structure (e6 and d5 pawns).", "Sicilian Defense", CaroKannFen, true),
      Scenario("This plan is typical in Open Games (1.e4 e5).", "Ruy Lopez", OpenGamesFen, false),
      Scenario("This plan is typical in Open Games (1.e4 e5).", "Catalan Opening", CatalanLikeFen, true),
      Scenario("This setup requires a Caro-Kann structure.", "Caro-Kann Defense", CaroKannFen, false),
      Scenario("This setup requires a Caro-Kann structure.", "Queen's Gambit Declined", CaroKannFen, false),
      Scenario("This looks like an attempt at Scandinavian Expansion, but timing is off.", "Scandinavian Defense", ScandinavianFen, false),
      Scenario("This looks like an attempt at Scandinavian Expansion, but timing is off.", "English Opening", EnglishFen, true)
    )

    scenarios.foreach { scenario =>
      val out = RuleTemplateSanitizer.sanitize(
        text = scenario.input,
        opening = Some(scenario.opening),
        phase = "opening",
        ply = 10,
        fen = Some(scenario.fen)
      )
      if scenario.shouldNeutralize then
        assert(out.contains("opening-family claim"), clues(scenario.input, scenario.opening, scenario.fen, out))
      else
        assert(!out.contains("opening-family claim"), clues(scenario.input, scenario.opening, scenario.fen, out))
    }
  }
