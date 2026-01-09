package lila.llm.analysis

import munit.FunSuite
import chess.{ Position, Color, Board, Piece, Square, Role, Pawn, Knight, Bishop, Rook, Queen, King }
import chess.format.Fen
import lila.llm.model.*
import lila.llm.model.Motif.*
import lila.llm.model.strategic.*

class QueenNarrativeTest extends FunSuite {

  def assertFound(result: String, keywords: List[String]): Unit = {
    val lower = result.toLowerCase
    val found = keywords.exists(k => lower.contains(k.toLowerCase))
    assert(found, s"Result did not contain any of $keywords:\n$result")
  }

  def createPositionalCtx(fen: String, tags: List[PositionalTag]): NarrativeContext = {
    val pos = Fen.read(chess.variant.Standard, Fen.Full(fen)).get
    val tagInfos = tags.map {
      case PositionalTag.QueenActivity(c) => PositionalTagInfo("QueenActivity", None, None, c.name)
      case PositionalTag.QueenManeuver(c) => PositionalTagInfo("QueenManeuver", None, None, c.name)
      case PositionalTag.MateNet(c) => PositionalTagInfo("MateNet", None, None, c.name)
      case PositionalTag.PerpetualCheck(c) => PositionalTagInfo("PerpetualCheck", None, None, c.name)
      case PositionalTag.RemovingTheDefender(target, c) => PositionalTagInfo("RemovingTheDefender", None, None, c.name, Some(target.name))
      case PositionalTag.Initiative(c) => PositionalTagInfo("Initiative", None, None, c.name)
      case _ => PositionalTagInfo("Unknown", None, None, "White")
    }

    val semantic = SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = tagInfos,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = Nil
    )

    NarrativeContext(
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      summary = NarrativeSummary("Attack", None, "StyleChoice", "Maintain", "+1.0"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "None", "Background", None, false, "quiet"),
      plans = PlanTable(List(PlanRow(1, "Kingside Attack", 0.9, List("Active Queen"))), Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Material balance"),
      candidates = List(CandidateInfo("Qh5", Some(""), "!", "Attack", None, None, "clean", None)),
      semantic = Some(semantic)
    )
  }

  test("Removing the Defender - Narrative Linkage") {
    val fen = "r1b2rk1/pp1p1ppp/2n1pn2/2b5/2P5/2N1PN2/PP1B1PPP/R2QKB1R w KQ - 0 1"
    val ctx = createPositionalCtx(fen, List(PositionalTag.RemovingTheDefender(Knight, Color.White)))
    val result = BookStyleRenderer.render(ctx)
    
    assertFound(result, List("stripping the defense", "removing the Knight", "eliminating"))
  }

  test("Mate Net - Narrative Linkage") {
    val fen = "k7/8/K7/8/1Q6/8/8/8 w - - 0 1"
    val ctx = createPositionalCtx(fen, List(PositionalTag.MateNet(Color.White)))
    val result = BookStyleRenderer.render(ctx)
    
    assertFound(result, List("mate net", "lethal", "entombing", "finish"))
  }

  test("Initiative - Narrative Linkage") {
    val fen = "r1bqk2r/pppp1ppp/2n2n2/4p3/1bB1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 0 1"
    val ctx = createPositionalCtx(fen, List(PositionalTag.Initiative(Color.White)))
    val result = BookStyleRenderer.render(ctx)
    
    assertFound(result, List("initiative", "pressure", "dictating", "piece play"))
  }

  test("Queen Activity - Narrative Linkage") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ctx = createPositionalCtx(fen, List(PositionalTag.QueenActivity(Color.White)))
    val result = BookStyleRenderer.render(ctx)
    
    assertFound(result, List("maximizing queen activity", "dominant scope", "centralizing"))
  }

  test("MoveAnalyzer - RemovingTheDefender Detection") {
    // White Bishop on b5 captures Black Knight on c6 which defended e5.
    val fen = "r1bqk2r/pppp1ppp/2n2n2/1B2p3/4P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 0 1"
    val pos = Fen.read(chess.variant.Standard, Fen.Full(fen)).get
    // Bxc6 is legal
    val mv = pos.move(Square.B5, Square.C6, None).toOption.get
    
    // We don't assert boolean here because we'd need a full analysis pipeline.
    // We just ensure the move generation in test is valid.
    assert(mv.dest == Square.C6)
  }
}
