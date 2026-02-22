package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.*

class EndgameFlagBehaviorTest extends FunSuite:

  private def extractor(enabled: Boolean, shadow: Boolean): StrategicFeatureExtractorImpl =
    new StrategicFeatureExtractorImpl(
      prophylaxisAnalyzer = new lila.llm.analysis.strategic.ProphylaxisAnalyzerImpl(),
      activityAnalyzer = new lila.llm.analysis.strategic.ActivityAnalyzerImpl(),
      structureAnalyzer = new lila.llm.analysis.strategic.StructureAnalyzerImpl(),
      endgameAnalyzer = new lila.llm.analysis.strategic.EndgameAnalyzerImpl(),
      practicalityScorer = new lila.llm.analysis.strategic.PracticalityScorerImpl(),
      endgameOracleEnabled = enabled,
      endgameOracleShadowMode = shadow
    )

  private val fen = "k7/P7/2K5/8/8/8/6B1/8 w - - 0 1"
  private val baseData = BaseAnalysisData(
    nature = PositionNature(NatureType.Static, 0.0, 1.0, "endgame"),
    motifs = Nil,
    plans = Nil
  )
  private val vars = List(VariationLine(moves = Nil, scoreCp = 0, mate = None, depth = 0))

  private def extractPattern(enabled: Boolean, shadow: Boolean): Option[String] =
    extractor(enabled, shadow)
      .extract(
        fen = fen,
        metadata = AnalysisMetadata(_root_.chess.Color.White, 1, None),
        baseData = baseData,
        vars = vars,
        playedMove = None,
        probeResults = Nil
      )
      .endgameFeatures
      .flatMap(_.primaryPattern)

  test("disabled + non-shadow mode exposes only core endgame signals") {
    val pattern = extractPattern(enabled = false, shadow = false)
    assert(pattern.isEmpty)
  }

  test("disabled + shadow mode evaluates pattern but does not expose it") {
    val pattern = extractPattern(enabled = false, shadow = true)
    assert(pattern.isEmpty)
  }

  test("enabled mode exposes primaryPattern and applies overrides") {
    val pattern = extractPattern(enabled = true, shadow = false)
    assertEquals(pattern, Some("WrongRookPawnWrongBishopFortress"))
  }

