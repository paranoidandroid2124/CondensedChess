package lila.llm.analysis

import chess.{ Color, Square, Knight }
import lila.llm.model.*
import lila.llm.model.authoring.{ PlanHypothesis, PlanViability }
import lila.llm.model.strategic.{ PieceActivity, PlanContinuity, PlanLifecyclePhase, PositionalTag }
import munit.FunSuite

class StrategyPackBuilderTest extends FunSuite:

  private val testFen = "r1bqkbnr/pppp1ppp/2n5/4p3/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 3"

  def hypothesis(name: String, score: Double, rank: Int): PlanHypothesis =
    PlanHypothesis(
      planId = name.toLowerCase.replace(' ', '_'),
      planName = name,
      rank = rank,
      score = score,
      preconditions = List("stable center"),
      executionSteps = List(s"execute $name"),
      failureModes = List(s"$name breaks if king is exposed"),
      viability = PlanViability(score = score, label = "high", risk = "counterplay")
    )

  def ctx(
      mainPlans: List[PlanHypothesis] = Nil,
      planRows: List[PlanRow] = Nil,
      opponent: Option[PlanRow] = None,
      continuity: Option[PlanContinuity] = None,
      conceptSummary: List[String] = Nil,
      whyAbsent: List[String] = Nil
  ): NarrativeContext =
    NarrativeContext(
      fen = testFen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 21,
      summary = NarrativeSummary("Test plan", None, "StyleChoice", "Maintain", "+0.3"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "none", "Background", None, false, "quiet"),
      plans = PlanTable(top5 = planRows, suppressed = Nil),
      planContinuity = continuity,
      delta = None,
      phase = PhaseContext("Middlegame", "test"),
      candidates = Nil,
      mainStrategicPlans = mainPlans,
      whyAbsentFromTopMultiPV = whyAbsent,
      semantic = Option.when(conceptSummary.nonEmpty)(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = conceptSummary
        )
      ),
      opponentPlan = opponent
    )

  def data(
      pieceActivity: List[PieceActivity] = Nil,
      positionalFeatures: List[PositionalTag] = Nil
  ): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = testFen,
      nature = PositionNature(NatureType.Dynamic, 0.6, 0.4, "dynamic test"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = pieceActivity,
      structuralWeaknesses = Nil,
      positionalFeatures = positionalFeatures,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = Nil,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = Nil,
      prevMove = None,
      ply = 21,
      evalCp = 30,
      isWhiteToMove = true
    )

  test("build expands mover plans beyond single-plan cap while keeping opponent plan") {
    val mainPlans = List(
      hypothesis("Kingside Expansion", 0.86, 1),
      hypothesis("Central Restriction", 0.79, 2)
    )
    val planRows = List(
      PlanRow(1, "Kingside Expansion", 0.86, Nil),
      PlanRow(2, "Central Restriction", 0.79, Nil),
      PlanRow(3, "File Control", 0.64, Nil)
    )
    val opponent = Some(PlanRow(1, "Queenside Counterplay", 0.61, Nil))

    val pack = StrategyPackBuilder.build(data(), ctx(mainPlans, planRows, opponent)).getOrElse(fail("pack missing"))
    val planNames = pack.plans.map(p => s"${p.side}:${p.planName}").toSet

    assertEquals(pack.plans.size, 4)
    assert(planNames.contains("white:Kingside Expansion"))
    assert(planNames.contains("white:Central Restriction"))
    assert(planNames.contains("white:File Control"))
    assert(planNames.contains("black:Queenside Counterplay"))
  }

  test("build route purpose uses outpost signal and emits coordination evidence") {
    val pa = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.42,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.E5, Square.D7),
      coordinationLinks = List(Square.E5, Square.G5)
    )
    val pack = StrategyPackBuilder
      .build(
        data(
          pieceActivity = List(pa),
          positionalFeatures = List(PositionalTag.Outpost(Square.D7, Color.White))
        ),
        ctx(mainPlans = List(hypothesis("Kingside Expansion", 0.8, 1)))
      )
      .getOrElse(fail("pack missing"))

    val route = pack.pieceRoutes.headOption.getOrElse(fail("route missing"))
    assertEquals(route.purpose, "outpost reinforcement")
    assert(route.evidence.exists(_.startsWith("coordination_links_")), clue(route.evidence))
  }

  test("build emits piece routes for both mover and opponent sides") {
    val whiteRoute = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.42,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.E5, Square.D7),
      coordinationLinks = List(Square.E5)
    )
    val blackRoute = PieceActivity(
      piece = Knight,
      square = Square.C6,
      mobilityScore = 0.40,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.B4, Square.D4),
      coordinationLinks = List(Square.D4)
    )

    val pack =
      StrategyPackBuilder
        .build(
          data(pieceActivity = List(whiteRoute, blackRoute)),
          ctx(mainPlans = List(hypothesis("Kingside Expansion", 0.8, 1)))
        )
        .getOrElse(fail("pack missing"))

    val sides = pack.pieceRoutes.map(_.side).toSet
    assert(sides.contains("white"), clue(pack.pieceRoutes))
    assert(sides.contains("black"), clue(pack.pieceRoutes))
  }

  test("build longTermFocus prioritizes continuity and route focus over raw concat") {
    val pa = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.35,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.E5, Square.G7),
      coordinationLinks = List(Square.E5)
    )
    val continuity = PlanContinuity(
      planName = "Kingside Expansion",
      planId = Some("kingside_expansion"),
      consecutivePlies = 3,
      startingPly = 17,
      phase = PlanLifecyclePhase.Execution,
      commitmentScore = 0.82
    )

    val pack = StrategyPackBuilder
      .build(
        data(pieceActivity = List(pa)),
        ctx(
          mainPlans = List(hypothesis("Kingside Expansion", 0.84, 1)),
          continuity = Some(continuity),
          conceptSummary = List("space advantage", "dark-square pressure"),
          whyAbsent = List("line rejected because center breaks too early")
        )
      )
      .getOrElse(fail("pack missing"))

    assert(pack.longTermFocus.exists(_.startsWith("continuity:")), clue(pack.longTermFocus))
    assert(pack.longTermFocus.exists(_.contains("route")), clue(pack.longTermFocus))
    assert(pack.evidence.exists(_.startsWith("route:")), clue(pack.evidence))
    assert(pack.evidence.exists(_.contains("line rejected")), clue(pack.evidence))
  }
