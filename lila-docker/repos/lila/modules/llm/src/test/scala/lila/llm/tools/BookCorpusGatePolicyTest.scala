package lila.llm.tools

import munit.FunSuite
import lila.llm.model.strategic.VariationLine

class BookCorpusGatePolicyTest extends FunSuite:

  private def corpusCase(id: String, phase: String = "Opening"): BookCommentaryCorpusRunner.CorpusCase =
    BookCommentaryCorpusRunner.CorpusCase(
      id = id,
      title = id,
      analysisFen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      startFen = None,
      preMovesUci = None,
      ply = 1,
      playedMove = "e2e4",
      opening = None,
      phase = phase,
      openingRef = None,
      variations = List(VariationLine(moves = Nil, scoreCp = 0)),
      referenceExcerpt = None,
      expect = None,
      probeResults = None,
      virtualMotifs = None
    )

  private def result(
      id: String,
      phase: String = "Opening",
      prose: Option[String] = None,
      endgame: Option[BookCommentaryCorpusRunner.EndgameSnapshot] = None,
      eligible: Boolean = false,
      observed: Boolean = false
  ): BookCommentaryCorpusRunner.CaseResult =
    BookCommentaryCorpusRunner.CaseResult(
      c = corpusCase(id, phase),
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      prose = prose,
      metrics = None,
      quality = None,
      qualityFindings = Nil,
      advisoryFindings = Nil,
      outline = None,
      endgame = endgame,
      openingPrecedentEligible = eligible,
      openingPrecedentObserved = observed,
      semanticConcepts = Nil,
      strategic = None,
      analysisLatencyMs = Some(1L),
      failures = Nil
    )

  private def snapshot(
      oppositionType: String = "None",
      zug: Double = 0.20,
      isZug: Boolean = false,
      primaryPattern: Option[String] = None
  ): BookCommentaryCorpusRunner.EndgameSnapshot =
    BookCommentaryCorpusRunner.EndgameSnapshot(
      oppositionType = oppositionType,
      ruleOfSquare = "NA",
      triangulationAvailable = false,
      rookEndgamePattern = "None",
      zugzwangLikelihood = zug,
      isZugzwang = isZug,
      outcomeHint = "Unclear",
      confidence = 0.80,
      primaryPattern = primaryPattern
    )

  private def proseWithTokenCount(count: Int): String =
    (0 until count).map(i => f"tok$i%03d").mkString(" ")

  test("balanced gate uses eligible opening denominator and 0.8 coverage rule") {
    val rows = List(
      result("a", eligible = true, observed = true, prose = Some("in A-B (2020), after e4 e5, White won (1-0).")),
      result("b", eligible = true, observed = true, prose = Some("in C-D (2019), after d4 d5, Black won (0-1).")),
      result("c", eligible = true, observed = false, prose = Some("no precedent line")),
      result("d", eligible = false, observed = false, prose = Some("middlegame case")),
      result("e", eligible = false, observed = false, prose = Some("endgame case"))
    )

    val gate = BookCommentaryCorpusRunner.evaluateBalancedGateForTest(rows)
    assertEquals(gate.eligiblePrecedentCases, 3)
    assertEquals(gate.precedentCoveredCases, 2)
    assertEquals(gate.requiredPrecedentCases, 3) // ceil(3 * 0.8) = 3
    assert(!gate.passed)
  }

  test("balanced gate allows up to 20 repeated five-gram violations") {
    val passRows =
      List.fill(4)(result("pass", prose = Some(proseWithTokenCount(24)))) // 20 repeated 5-grams
    val failRows =
      List.fill(4)(result("fail", prose = Some(proseWithTokenCount(25)))) // 21 repeated 5-grams

    val passGate = BookCommentaryCorpusRunner.evaluateBalancedGateForTest(passRows)
    val failGate = BookCommentaryCorpusRunner.evaluateBalancedGateForTest(failRows)

    assertEquals(passGate.repeatedFivegramViolations, 20)
    assert(passGate.passed)
    assertEquals(failGate.repeatedFivegramViolations, 21)
    assert(!failGate.passed)
  }

  test("contradiction policy uses king-context opposition and zugzwang pattern buffer") {
    val rows = List(
      result(
        id = "opp_mismatch",
        prose = Some("The kings are in direct opposition."),
        endgame = Some(snapshot(oppositionType = "None"))
      ),
      result(
        id = "zug_buffered",
        prose = Some("This has a zugzwang flavor where every move concedes something."),
        endgame = Some(snapshot(primaryPattern = Some("TriangulationZugzwang")))
      ),
      result(
        id = "zug_mismatch",
        prose = Some("This has a zugzwang flavor where every move concedes something."),
        endgame = Some(snapshot(primaryPattern = None))
      )
    )

    val contradictions = BookCommentaryCorpusRunner.countEndgameContradictionsForTest(rows)
    assertEquals(contradictions, 2)
  }
