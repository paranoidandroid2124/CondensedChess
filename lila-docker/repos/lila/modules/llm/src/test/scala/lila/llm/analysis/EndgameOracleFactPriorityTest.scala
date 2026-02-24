package lila.llm.analysis

import chess.Color
import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.*

class EndgameOracleFactPriorityTest extends FunSuite:

  private val fen = "8/8/4k3/8/4K3/8/8/8 w - - 0 1"
  private val motifs = List[Motif](
    Motif.Opposition(
      opponentKingSquare = chess.Square.E6,
      ownKingSquare = chess.Square.E4,
      oppType = Motif.OppositionType.Direct,
      color = Color.Black,
      plyIndex = 0
    ),
    Motif.Zugzwang(color = Color.Black, plyIndex = 0)
  )

  private val oracleSignal = EndgameFeature(
    hasOpposition = false,
    isZugzwang = false,
    keySquaresControlled = Nil,
    oppositionType = EndgameOppositionType.None,
    zugzwangLikelihood = 0.20,
    ruleOfSquare = RuleOfSquareStatus.NA,
    triangulationAvailable = false,
    kingActivityDelta = 0,
    rookEndgamePattern = RookEndgamePattern.None,
    theoreticalOutcomeHint = TheoreticalOutcomeHint.Unclear,
    confidence = 0.80
  )

  private def data(endgame: Option[EndgameFeature]) =
    ExtendedAnalysisData(
      fen = fen,
      nature = PositionNature(NatureType.Static, tension = 0.0, stability = 1.0, description = "endgame"),
      motifs = motifs,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = endgame,
      practicalAssessment = None,
      prevMove = None,
      ply = 40,
      evalCp = 0,
      isWhiteToMove = true,
      phase = "endgame",
      integratedContext = Some(IntegratedContext(evalCp = 0, isWhiteToMove = true))
    )

  test("oracle signal suppresses motif-derived opposition and zugzwang facts") {
    val ctx = NarrativeContextBuilder.build(
      data = data(Some(oracleSignal)),
      ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true),
      prevAnalysis = None,
      probeResults = Nil,
      openingRef = None,
      prevOpeningRef = None,
      openingBudget = OpeningEventBudget(),
      afterAnalysis = None
    )
    assert(!ctx.facts.exists(_.isInstanceOf[Fact.Opposition]), "oracle endgame signal should suppress motif opposition fact")
    assert(!ctx.facts.exists(_.isInstanceOf[Fact.Zugzwang]), "oracle endgame signal should suppress motif zugzwang fact")
  }

  test("without oracle signal motif-derived opposition and zugzwang facts remain") {
    val ctx = NarrativeContextBuilder.build(
      data = data(None),
      ctx = IntegratedContext(evalCp = 0, isWhiteToMove = true),
      prevAnalysis = None,
      probeResults = Nil,
      openingRef = None,
      prevOpeningRef = None,
      openingBudget = OpeningEventBudget(),
      afterAnalysis = None
    )
    assert(ctx.facts.exists(_.isInstanceOf[Fact.Opposition]), "without oracle signal motif opposition fact should remain")
    assert(ctx.facts.exists(_.isInstanceOf[Fact.Zugzwang]), "without oracle signal motif zugzwang fact should remain")
  }
