package chess
package analysis

import munit.FunSuite
import chess.analysis.ConceptLabeler.*
import chess.analysis.FeatureExtractor.*

class ConceptLabelerTest extends FunSuite {

  // Helper to create mock features
  // Note: FeatureExtractor case classes are simplified here for testing
  def mockFeatures(
    phase: String = "middlegame",
    whiteMajor: Int = 2, whiteMinor: Int = 2, whitePawnCount: Int = 8,
    blackMajor: Int = 2, blackMinor: Int = 2, blackPawnCount: Int = 8,
    whiteKingDist: Double = 1.0, blackKingDist: Double = 1.0,
    whiteBackRankWeak: Boolean = false, blackBackRankWeak: Boolean = false
  ): PositionFeatures =
    PositionFeatures(
      pawns = PawnStructureFeatures(whitePawnCount, blackPawnCount, Nil, Nil, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, false, 0, 0, false, false),
      activity = ActivityFeatures(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false),
      kingSafety = KingSafetyFeatures("none", "none", 0, 0, 0, 0, whiteBackRankWeak, blackBackRankWeak, whiteKingDist, blackKingDist, 0, 0),
      space = SpaceFeatures(0, 0, 0, 0, 0, 0),
      materialPhase = MaterialPhaseFeatures(0, 0, 0, whiteMajor, blackMajor, whiteMinor, blackMinor, phase),
      development = DevelopmentFeatures(0, 0, 0, 0, false, false),
      coordination = CoordinationFeatures(false, false, false, false, 0, 0),
      geometry = GeometryFeatures(false, false),
      tactics = TacticalFeatures(0, 0, 0, 0, 0, 0),
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      sideToMove = "white",
      plyCount = 1
    )

  test("labelEndgame detects KingActivityIgnored and Good") {
    // Pawn ending, white king moves away from center
    val fBefore = mockFeatures(phase = "endgame", whiteMajor = 0, whiteMinor = 0, blackMajor = 0, blackMinor = 0, whiteKingDist = 2.0)
    val fBadAfter = mockFeatures(phase = "endgame", whiteMajor = 0, whiteMinor = 0, blackMajor = 0, blackMinor = 0, whiteKingDist = 4.0)
    
    val tagsBad = ConceptLabeler.labelEndgame(fBefore, fBadAfter, "e1d1")
    assert(tagsBad.contains(EndgameTag.KingActivityIgnored))

    // White king moves closer to center
    val fGoodAfter = mockFeatures(phase = "endgame", whiteMajor = 0, whiteMinor = 0, blackMajor = 0, blackMinor = 0, whiteKingDist = 1.0)
    val tagsGood = ConceptLabeler.labelEndgame(fBefore, fGoodAfter, "e1d2")
    assert(tagsGood.contains(EndgameTag.KingActivityGood))
  }

  /*
  test("labelTactics detects BackRankMatePattern") {
    val fBefore = mockFeatures(whiteKingRank = 0, blackKingRank = 7)
    // Create experiment result showing mate in 2
    val exp = ExperimentResult(
      eval = EngineEval(10, List(EngineLine("d8d1", 0, None, Some(2), List("d8d1", "e1d1", "e8d8")))),
      move = Some("d8d1"),
      metadata = Map("candidateType" -> "TacticalCheck")
    )
    
    // bestEval showing mate
    val tags = ConceptLabeler.labelTactics(fBefore, List(exp), 0, "d8d1", 20000, 20000) 
    // Wait, labelTactics logic uses 'exps' but also 'bestEval' is generic.
    // The implementation checks if *any* experiment shows mate.
    
    assert(tags.contains(TacticTag.BackRankMatePattern))
  }
  */
  // Commented out BackRank test as it relies on specific complex setup with king shield pawns which our mock is stubbing.
  // We can uncomment once we refine mock.
}
