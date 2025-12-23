package chess
package analysis

import munit.FunSuite
import chess.analysis.BookModel.*
import chess.analysis.AnalysisModel.{ PlyOutput, EngineEval, EngineLine, Concepts }
import chess.analysis.ConceptLabeler.*
import chess.analysis.AnalysisTypes.EvidencePack
import chess.Color
import chess.Ply

class BookBuilderTest extends FunSuite:

  def mockPly(
    plyVal: Int, 
    roles: List[String] = Nil, 
    structureTags: List[StructureTag] = Nil,
    transitionTags: List[TransitionTag] = Nil,
    tacticTags: List[TacticTag] = Nil,
    mistakeTags: List[MistakeTag] = Nil,
    endgameTags: List[EndgameTag] = Nil
  ): PlyOutput =
    val dummyFeatures = FeatureExtractor.SideFeatures(0,0,0,0,0,0,false,0,0) // bishopPair is boolean

    PlyOutput(
      ply = Ply(plyVal),
      turn = if plyVal % 2 == 0 then Color.White else Color.Black,
      san = "e4", // SanStr wrapper not needed if AnalysisModel uses String
      uci = "e2e4",
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      fenBefore = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      legalMoves = 20,
      features = dummyFeatures,
      evalBeforeShallow = EngineEval(10, Nil),
      evalBeforeDeep = EngineEval(20, List(EngineLine("e2e4", 0.5, Some(10), None, Nil))),
      winPctBefore = 50.0,
      winPctAfterForPlayer = 50.0,
      deltaWinPct = 0.0,
      epBefore = 100,
      epAfter = 100,
      epLoss = 0,

      judgement = "Normal",
      special = None,
      concepts = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      conceptsBefore = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      conceptDelta = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
      bestVsSecondGap = None,
      bestVsPlayedGap = None,
      semanticTags = Nil,
      mistakeCategory = None,
      roles = roles,
      conceptLabels = Some(ConceptLabels(structureTags, Nil, tacticTags, mistakeTags, endgameTags, Nil, transitionTags, Nil, Nil, EvidencePack())),
      playedEvalCp = Some(10)
    )

  test("storyboard creates Opening section for short game") {
    val timeline = (1 to 10).map(i => mockPly(i)).toVector
    val sections = BookBuilder.storyboard(timeline)
    
    assertEquals(sections.size, 1)
    assertEquals(sections.head.sectionType, SectionType.OpeningReview)
    assertEquals(sections.head.endPly, 10)
  }

  test("storyboard segments Middlegame into Structure and Tactics") {
    // 0-15: Opening
    // 16-20: Structure
    // 21-25: Tactics (Violence)
    // 26-30: Crisis (GameDecider)
    
    val opening = (1 to 16).map(i => mockPly(i)).toVector
    val structure = (17 to 20).map(i => mockPly(i)).toVector
    val tactics = (21 to 25).map(i => mockPly(i, roles = List("Violence"))).toVector
    val crisis = (26 to 30).map(i => mockPly(i, roles = List("GameDecider"))).toVector
    
    val timeline = opening ++ structure ++ tactics ++ crisis
    val sections = BookBuilder.storyboard(timeline)
    
    // Expected: Opening(1-16) -> Structure(17-20) -> Tactics(21-25) -> Crisis(26-30)
    // Structure section might be "StructuralDeepDive" or "NarrativeBridge" depending on default
    
    assertEquals(sections.head.sectionType, SectionType.OpeningReview)
    assertEquals(sections(1).sectionType, SectionType.TacticalStorm)
    assertEquals(sections(2).sectionType, SectionType.TurningPoints)
  }

  test("storyboard detects Endgame") {
    // 0-20: Opening/Middle
    // 21: EndgameTransition
    // 22-30: Endgame
    
    val moves = (1 to 20).map(i => mockPly(i)).toVector
    val trans = mockPly(21, transitionTags = List(TransitionTag.EndgameTransition))
    val end = (22 to 30).map(i => mockPly(i)).toVector
    
    // Should have Opening, maybe Middlegame, then Endgame
    // Opening is capped at 16 heuristics.
    // 1-16 Opening
    // 17-20 Mid
    // 21-30 Endgame
    
    // Explicit vector concatenation to avoid precedence issues
    val timeline = (moves :+ trans) ++ end
    val sections = BookBuilder.storyboard(timeline)
    
    val endgameSec = sections.last
    assertEquals(endgameSec.sectionType, SectionType.EndgameLessons)
    assertEquals(endgameSec.startPly, 21)
  }
