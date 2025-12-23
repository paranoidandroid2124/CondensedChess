package chess.analysis

import munit.FunSuite
import ujson.*
import chess.{Ply, Color}
import chess.analysis.AnalysisModel.*
import chess.analysis.AnalysisTypes.*
import chess.analysis.ConceptLabeler.*

class PayloadIntegritySuite extends FunSuite {

  val dummyConcepts = Concepts(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  
  def mockPly(p: Int, s: String, u: String, c: Color) = PlyOutput(
    ply = Ply(p),
    turn = c,
    san = s,
    uci = u,
    fen = "",
    fenBefore = "",
    legalMoves = 20,
    features = null.asInstanceOf[FeatureExtractor.SideFeatures],
    evalBeforeShallow = EngineEval(0, Nil),
    evalBeforeDeep = EngineEval(0, Nil),
    winPctBefore = 50.0,
    winPctAfterForPlayer = 50.0,
    deltaWinPct = 0.0,
    epBefore = 0.0,
    epAfter = 0.0,
    epLoss = 0.0,
    judgement = "None",
    special = None,
    conceptsBefore = dummyConcepts,
    concepts = dummyConcepts,
    conceptDelta = dummyConcepts,
    bestVsSecondGap = None,
    bestVsPlayedGap = None,
    semanticTags = Nil,
    mistakeCategory = None,
    phaseLabel = None,
    phase = "middlegame",
    shortComment = None,
    studyTags = Nil,
    studyScore = 0.0,
    practicality = None,
    materialDiff = 0.0,
    bestMaterialDiff = None,
    tacticalMotif = None,
    roles = Nil,
    conceptLabels = None,
    fullFeatures = None,
    playedEvalCp = None,
    hypotheses = Nil
  )
  
  test("EvidencePack serialization in LlmAnnotator preview") {
    val ply = mockPly(10, "e4", "e2e4", Color.White).copy(
      judgement = "Inaccuracy",
      deltaWinPct = -5.0,
      semanticTags = List("IqpWhite"),
      conceptLabels = Some(ConceptLabels(
        structureTags = List(StructureTag.IqpWhite),
        planTags = Nil,
        tacticTags = Nil,
        mistakeTags = Nil,
        endgameTags = Nil,
        richTags = List(RichTag("struct:iqp_white", 1.0, TagCategory.Structure, List(EvidenceRef("structure", "struct:iqp_white")))),
        evidence = EvidencePack(
          structure = Map("struct:iqp_white" -> StructureEvidence("White IQP", List("d4")))
        )
      ))
    )

    val output = Output(
      timeline = Vector(ply),
      opening = None,
      openingStats = None,
      accuracyWhite = None,
      accuracyBlack = None,
      critical = Vector.empty,
      root = None,
      studyChapters = Vector.empty,
      oppositeColorBishops = false
    )

    val timelineByPly = Map(10 -> ply)
    val jsonStr = LlmAnnotator.renderPreview(output, timelineByPly)
    val json = ujson.read(jsonStr)

    // Check if evidence exists in keySwings
    val keySwings = json("keySwings").arr
    val target = keySwings.find(_.obj("label").str.contains("e4"))
    assert(target.isDefined)
    
    val evidence = target.get.obj("evidence").arr
    assert(evidence.nonEmpty)
    
    val ev = evidence.head.obj
    assertEquals(ev("id").str, "struct:iqp_white")
    assertEquals(ev("detail").str, "White IQP")
    assertEquals(ev("squares").arr.head.str, "d4")
  }

  test("Tactics Evidence serialization") {
    val ply = mockPly(18, "Bxh7+", "d3h7", Color.White).copy(
      judgement = "Brilliant",
      deltaWinPct = 30.0,
      winPctAfterForPlayer = 80.0,
      semanticTags = List("GreekGiftSound"),
      conceptLabels = Some(ConceptLabels(
        structureTags = Nil,
        planTags = Nil,
        tacticTags = List(TacticTag.GreekGiftSound),
        mistakeTags = Nil,
        endgameTags = Nil,
        richTags = List(RichTag("tactic:greek_gift", 1.0, TagCategory.Tactic, List(EvidenceRef("tactics", "tactic:greek_gift")))),
        evidence = EvidencePack(
          tactics = Map("tactic:greek_gift" -> TacticsEvidence("Greek Gift Sacrifice", List("Bxh7+", "Kxh7", "Ng5+"), Some("P")))
        )
      ))
    )

    val output = Output(
      timeline = Vector(ply),
      opening = None,
      openingStats = None,
      accuracyWhite = None,
      accuracyBlack = None,
      critical = Vector.empty,
      root = None,
      studyChapters = Vector.empty,
      oppositeColorBishops = false
    )

    val timelineByPly = Map(18 -> ply)
    val jsonStr = LlmAnnotator.renderPreview(output, timelineByPly)
    val json = ujson.read(jsonStr)

    val keySwings = json("keySwings").arr
    val target = keySwings.find(_.obj("label").str.contains("Bxh7"))
    assert(target.isDefined)
    
    val evidence = target.get.obj("evidence").arr
    val ev = evidence.head.obj
    assertEquals(ev("id").str, "tactic:greek_gift")
    assertEquals(ev("motif").str, "Greek Gift Sacrifice")
    assertEquals(ev("sequence").arr.head.str, "Bxh7+")
  }
}
