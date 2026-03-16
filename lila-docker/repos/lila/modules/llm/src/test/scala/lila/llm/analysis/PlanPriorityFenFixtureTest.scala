package lila.llm.analysis

import chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.*
import lila.llm.model.{ ExtendedAnalysisData, PlanMatch }
import lila.llm.model.strategic.VariationLine
import munit.FunSuite

class PlanPriorityFenFixtureTest extends FunSuite:

  private enum Bank:
    case TacticalOverride
    case MaterialImbalance
    case MixedRegime
    case StrategicCompensation

  private final case class Fixture(
      id: String,
      bank: Bank,
      label: String,
      fen: String,
      stockfishScoreCp: Int,
      pvMoves: List[String],
      expectedTopThemes: Set[String],
      phase: String = "middlegame",
      requireMaterialParity: Boolean = false,
      requireMaterialImbalance: Boolean = false,
      requiredSecondaryThemes: Set[String] = Set.empty,
      expectedTaskModes: Set[String] = Set.empty,
      forbiddenTopThemes: Set[String] = Set.empty,
      forbiddenTaskModes: Set[String] = Set.empty
  )

  private final case class EvaluatedFixture(
      fixture: Fixture,
      board: Board,
      data: ExtendedAnalysisData
  )

  private val tacticalOverrideFixtures = List(
    Fixture(
      id = "TO1",
      bank = Bank.TacticalOverride,
      label = "won-pawn continuation stays tactical-first",
      fen = "r2q1rk1/1b1nbppp/pp1Bpn2/8/2PQ4/1PN2NP1/P3PPBP/R2R2K1 w - - 1 12",
      stockfishScoreCp = 241,
      pvMoves = List("f3e5", "e7d6", "e5d7", "f6d7", "g2b7", "d6e5", "d4e3", "d8f6", "d1d7", "e5c3"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      expectedTaskModes = Set("ExplainTactics")
    ),
    Fixture(
      id = "TO2",
      bank = Bank.TacticalOverride,
      label = "exchange-up continuation still resolves concretely",
      fen = "r2q1rk1/1b1n1ppp/pp1bpn2/8/2PQ4/1PN2NP1/P3PPBP/2RR2K1 w - - 0 13",
      stockfishScoreCp = 240,
      pvMoves = List("d4d6", "d8b8", "d6b8", "a8b8", "d1d6", "h7h6", "c1d1", "f8d8", "e2e4", "e6e5", "g2f1"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      expectedTaskModes = Set("ExplainTactics")
    ),
    Fixture(
      id = "TO3",
      bank = Bank.TacticalOverride,
      label = "queen incursion aftermath stays forcing-first",
      fen = "2r2rk1/pQ3pp1/2p2n1p/3p4/3P4/q1P1PNRP/P4PP1/2R3K1 w - - 1 23",
      stockfishScoreCp = 43,
      pvMoves = List("c1f1", "f6e4", "g3g4", "e4f6", "g4f4", "a3a2", "f3e5", "c8b8", "b7c6", "f8c8"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      expectedTaskModes = Set("ExplainTactics")
    )
  )

  private val materialImbalanceFixtures = List(
    Fixture(
      id = "MI1",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus rook converts by simplification",
      fen = "2bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 735,
      pvMoves = List("d4c6", "b7c6", "d1a4", "c8d7", "a4a7", "e7d6", "e3d4", "d8e7", "a1d1", "e7e6", "d4f6", "g7f6"),
      expectedTopThemes = Set(PlanMatcher.Theme.FavorableExchange),
      expectedTaskModes = Set("ExplainConvert"),
      requireMaterialImbalance = true
    ),
    Fixture(
      id = "MI2",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus queen simplifies the edge",
      fen = "r1b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 1366,
      pvMoves = List("d4b5", "e7b4", "b5c7", "b4c3", "b2c3", "c8g4", "c7a8", "e8a8", "d1b3", "g4e2", "b3b7", "a8e8"),
      expectedTopThemes = Set(PlanMatcher.Theme.FavorableExchange),
      expectedTaskModes = Set("ExplainConvert"),
      requireMaterialImbalance = true
    ),
    Fixture(
      id = "MI3",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus queen and rook stays conversion-led",
      fen = "2b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 1819,
      pvMoves = List("d4b5", "c8e6", "b5c7", "e8d8", "d1b3", "d5d4", "c7e6", "f7e6", "b3b7", "d4c3", "b7c6"),
      expectedTopThemes = Set(PlanMatcher.Theme.FavorableExchange),
      expectedTaskModes = Set("ExplainConvert"),
      requireMaterialImbalance = true
    ),
    Fixture(
      id = "MI4",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus bishop pawn prefers simplification",
      fen = "r1bqr1k1/pp3pp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 596,
      pvMoves = List("a1c1", "c8e6", "d4e6", "f7e6", "e3c5", "d8d7", "e2e4", "d5d4", "c3b5"),
      expectedTopThemes = Set(PlanMatcher.Theme.FavorableExchange),
      expectedTaskModes = Set("ExplainConvert"),
      requireMaterialImbalance = true
    ),
    Fixture(
      id = "MI5",
      bank = Bank.MaterialImbalance,
      label = "won-pawn aftermath for the defender still chooses relief",
      fen = "r2qk2r/1b1nbppp/pp1Qpn2/8/2P5/BPN2NP1/P3PPBP/R2R2K1 b kq - 0 11",
      stockfishScoreCp = -541,
      pvMoves = List("e7d6", "a3d6", "a8c8", "f3e5", "b7g2", "g1g2", "c8a8", "g2g1", "d8c8", "d1d4", "d7e5"),
      expectedTopThemes = Set(PlanMatcher.Theme.FavorableExchange),
      expectedTaskModes = Set("ExplainConvert"),
      requireMaterialImbalance = true
    )
  )

  private val mixedRegimeFixtures = List(
    Fixture(
      id = "MR1",
      bank = Bank.MixedRegime,
      label = "Hedgehog shell with direct tactical strike",
      fen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11",
      stockfishScoreCp = 250,
      pvMoves = List("a3d6", "e8g8", "a1c1", "e7d6", "d4d6", "d8c8", "e2e4", "f8d8", "d6f4", "h7h5", "d1d6", "c8c5", "c1d1"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.Restriction, PlanMatcher.Theme.SpaceClamp),
      expectedTaskModes = Set("ExplainTactics")
    ),
    Fixture(
      id = "MR2",
      bank = Bank.MixedRegime,
      label = "open-file fight still routes through tactic first",
      fen = "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22",
      stockfishScoreCp = 43,
      pvMoves = List("b3b7", "d6a3", "c1f1", "f6e4", "g3g4", "e4c3", "b7d7", "a3a2", "f3e5"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.FlankInfrastructure, PlanMatcher.Theme.Restriction),
      expectedTaskModes = Set("ExplainTactics")
    ),
    Fixture(
      id = "MR3",
      bank = Bank.MixedRegime,
      label = "Dragon shell keeps its attack background but forcing line leads",
      fen = "2rq1rk1/pp1bppb1/3p1np1/4n2p/3NP2P/1BN1BP2/PPPQ2P1/2KR3R w - - 0 13",
      stockfishScoreCp = 29,
      pvMoves = List("c1b1", "e5c4", "b3c4", "c8c4", "d4e2", "b7b5", "e2d4", "b5b4", "c3e2", "e7e6", "b2b3"),
      expectedTopThemes = Set(PlanMatcher.Theme.ImmediateTacticalGain),
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.Redeployment, PlanMatcher.Theme.FlankInfrastructure),
      expectedTaskModes = Set("ExplainTactics")
    )
  )

  private val strategicCompensationFixtures = List(
    Fixture(
      id = "SC1",
      bank = Bank.StrategicCompensation,
      label = "Open Catalan delayed c4 recovery keeps a long-term initiative frame",
      fen = "rn1q1rk1/1bp1bppp/p3pn2/1p6/P1pP4/5NP1/1PQNPPBP/R1B2RK1 w - - 2 10",
      stockfishScoreCp = -75,
      pvMoves = List("a4b5", "a6b5", "a1a8", "b7a8", "b2b3", "c4b3", "d2b3", "a8e4", "c2b2", "b8d7", "b3d2", "e4c6", "f1e1", "d7b6", "e2e4"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.Opening
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC2",
      bank = Bank.StrategicCompensation,
      label = "Open Catalan queenside bind remains strategy-led while the pawn stays down",
      fen = "r1bq1rk1/2p1bppp/p1n1pn2/8/PppP4/2N2NP1/1PQ1PPBP/R1BR2K1 w - - 0 11",
      stockfishScoreCp = 18,
      pvMoves = List("f3e5", "c6d4", "d1d4", "d8d4", "e5c6", "d4c5", "c6e7", "c5e7", "g2a8", "b4c3", "c2c3", "c8d7"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.Opening
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC3",
      bank = Bank.StrategicCompensation,
      label = "Benko accepted keeps black on long-term activity rather than instant tactics",
      fen = "rn1q1rk1/4ppbp/b2p1np1/2pP4/8/2N2NP1/PP2PPBP/R1BQ1RK1 b - - 5 10",
      stockfishScoreCp = 102,
      pvMoves = List("b8d7", "d1d2", "f6g4", "f1e1", "g7h6", "d2c2", "h6g7", "h2h3", "g4h6", "g3g4", "d8b6", "b2b3"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.PawnBreakPreparation
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC4",
      bank = Bank.StrategicCompensation,
      label = "Benko bishop pressure still reads as enduring compensation",
      fen = "rnbq1rk1/4ppbp/P2p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R b KQ - 4 9",
      stockfishScoreCp = 102,
      pvMoves = List("c8a6", "e1g1", "a6e2", "d1e2", "b8d7", "f3d2", "e7e6", "d5e6", "f7e6", "f1d1", "d6d5", "e4d5", "e6d5"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.PawnBreakPreparation
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC5",
      bank = Bank.StrategicCompensation,
      label = "Blumenfeld compensation stays about pressure, not a forcing shot",
      fen = "rnbq1rk1/3p1pbp/P3pnp1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R b KQ - 2 9",
      stockfishScoreCp = 118,
      pvMoves = List("b8a6", "d5e6", "d7e6", "e1g1", "c8b7", "d1d8", "f8d8", "e4e5", "b7f3", "g2f3", "f6d5", "c1g5", "d5c3"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.PawnBreakPreparation
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC6",
      bank = Bank.StrategicCompensation,
      label = "Blumenfeld queenside pawn investment keeps strategic initiative alive",
      fen = "rn1qkb1r/5p1p/b2ppnp1/2pP4/4P3/2N2N1P/PP3PP1/R1BQKB1R b KQkq - 0 9",
      stockfishScoreCp = 114,
      pvMoves = List("e6d5", "f1a6", "a8a6", "c3d5", "f8g7", "c1g5", "h7h6", "g5f6", "g7f6", "e1g1", "f6b2", "a1b1", "b2g7", "b1b7", "e8g8", "d1e2", "f8e8"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.WeaknessFixation,
        PlanMatcher.Theme.PawnBreakPreparation
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC7",
      bank = Bank.StrategicCompensation,
      label = "Smith-Morra development lead should stay plan-led before any tactic lands",
      fen = "r1bqkb1r/1p3ppp/p1nppn2/8/2B1P3/2N2N2/PP2QPPP/R1B2RK1 w kq - 0 9",
      stockfishScoreCp = 25,
      pvMoves = List("f1d1", "b7b5", "e4e5", "b5c4", "e5f6", "g7f6", "b2b4", "c8d7", "c3d5", "f8g7", "e2c4"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Opening,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.FlankInfrastructure
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC8",
      bank = Bank.StrategicCompensation,
      label = "Smith-Morra central lead stays in the strategic initiative bucket",
      fen = "r1bqk2r/pp2bppp/2nppn2/8/2B1P3/2N2N2/PP2QPPP/R1B2RK1 w kq - 4 9",
      stockfishScoreCp = 1,
      pvMoves = List("f1d1", "e6e5", "c1e3", "e8g8", "h2h3", "h7h6", "f3h4", "g8h8", "a1c1", "c8d7", "a2a3", "a8c8", "h4f5", "d7f5"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Opening,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.Restriction
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC9",
      bank = Bank.StrategicCompensation,
      label = "Queen's Indian pawn investment keeps white on long-term initiative",
      fen = "rn1q1rk1/pbn2ppp/1pp2b2/3p1N2/Q3PB2/2N3P1/PP3PBP/R4RK1 b - - 3 14",
      stockfishScoreCp = -12,
      pvMoves = List("b7c8", "a1d1", "c8f5", "e4f5", "g7g5", "f5g6", "h7g6", "c3e2", "f8e8", "a4c2", "e8e6"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Opening,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.FlankInfrastructure
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    ),
    Fixture(
      id = "SC10",
      bank = Bank.StrategicCompensation,
      label = "Queen's Indian follow-up still values coordination before cashing in material",
      fen = "rnbq1rk1/p1n2ppp/1pp2b2/3p1N2/Q3PB2/2N3P1/PP3PBP/3R1RK1 b - - 5 15",
      stockfishScoreCp = -11,
      pvMoves = List("a7a5", "c3e2", "f6b2", "a4c2", "b2f6", "e4e5", "f6g5", "f5d6", "c7e6"),
      expectedTopThemes = Set.empty,
      phase = "opening",
      requireMaterialImbalance = true,
      requiredSecondaryThemes = Set(
        PlanMatcher.Theme.Opening,
        PlanMatcher.Theme.Redeployment,
        PlanMatcher.Theme.Restriction,
        PlanMatcher.Theme.PawnBreakPreparation,
        PlanMatcher.Theme.FlankInfrastructure
      ),
      forbiddenTopThemes = Set(PlanMatcher.Theme.FavorableExchange)
    )
  )

  private val fixtures =
    tacticalOverrideFixtures ++ materialImbalanceFixtures ++ mixedRegimeFixtures ++ strategicCompensationFixtures

  private lazy val evaluatedFixtures: List[EvaluatedFixture] = fixtures.map(evaluate)

  private def evaluate(fixture: Fixture): EvaluatedFixture =
    val board =
      Fen.read(Standard, Fen.Full(fixture.fen)).map(_.board).getOrElse(fail(s"invalid FEN: ${fixture.id}"))
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fixture.fen,
          variations = List(VariationLine(fixture.pvMoves, fixture.stockfishScoreCp, depth = 10)),
          phase = Some(fixture.phase),
          ply = if fixture.phase == "endgame" then 90 else 24
        )
        .getOrElse(fail(s"analysis missing for ${fixture.id}"))
    EvaluatedFixture(fixture = fixture, board = board, data = data)

  private def materialValueDiff(board: Board): Int =
    def score(color: Color): Int =
      board.byPiece(color, Pawn).count +
        board.byPiece(color, Knight).count * 3 +
        board.byPiece(color, Bishop).count * 3 +
        board.byPiece(color, Rook).count * 5 +
        board.byPiece(color, Queen).count * 9

    score(Color.White) - score(Color.Black)

  private def topTheme(plan: PlanMatch): String =
    plan.supports
      .collectFirst { case support if support.startsWith("theme:") => support.stripPrefix("theme:") }
      .getOrElse("unknown")

  private def taskMode(data: ExtendedAnalysisData): String =
    data.toContext.classification.map(_.taskMode.taskMode.toString).getOrElse("Unknown")

  private def simplifyBias(data: ExtendedAnalysisData): Boolean =
    data.toContext.classification.exists(_.simplifyBias.shouldSimplify)

  test("plan-priority FEN bank stays complete and banked by regime") {
    assertEquals(tacticalOverrideFixtures.size, 3)
    assertEquals(materialImbalanceFixtures.size, 5)
    assertEquals(mixedRegimeFixtures.size, 3)
    assertEquals(strategicCompensationFixtures.size, 10)
    assertEquals(fixtures.size, 21)
    assertEquals(fixtures.map(_.id).distinct.size, fixtures.size)
    assert(fixtures.forall(_.pvMoves.nonEmpty))
  }

  tacticalOverrideFixtures.foreach { fixture =>
    test(s"${fixture.id} tactical override: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      assert(fixture.expectedTopThemes.contains(topTheme(top)))
      assert(fixture.expectedTaskModes.contains(taskMode(evaluated.data)))
      assert(top.score >= 0.5, clue(s"${fixture.id} top score too low: ${top.score}"))
    }
  }

  materialImbalanceFixtures.foreach { fixture =>
    test(s"${fixture.id} material imbalance: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      assert(fixture.requireMaterialImbalance, clue(s"${fixture.id} should declare material imbalance"))
      assert(materialValueDiff(evaluated.board) != 0, clue(s"${fixture.id} should be materially imbalanced"))
      assert(fixture.expectedTopThemes.contains(topTheme(top)))
      assert(simplifyBias(evaluated.data), clue(s"${fixture.id} should be in simplify/conversion mode"))
      assert(fixture.expectedTaskModes.contains(taskMode(evaluated.data)))
    }
  }

  mixedRegimeFixtures.foreach { fixture =>
    test(s"${fixture.id} mixed regime: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val themes = evaluated.data.plans.take(4).map(topTheme).toSet
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      if fixture.requireMaterialParity then
        assertEquals(materialValueDiff(evaluated.board), 0, clue(s"${fixture.id} should stay materially even"))

      assert(fixture.expectedTopThemes.contains(topTheme(top)))
      assert(fixture.expectedTaskModes.contains(taskMode(evaluated.data)))
      assert(
        themes.intersect(fixture.requiredSecondaryThemes).nonEmpty,
        clue(s"${fixture.id} lost strategic background: themes=${themes.toList.sorted.mkString(",")}")
      )
    }
  }

  strategicCompensationFixtures.foreach { fixture =>
    test(s"${fixture.id} strategic compensation: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val themes = evaluated.data.plans.take(4).map(topTheme).toSet
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))
      val mode = taskMode(evaluated.data)

      assert(fixture.requireMaterialImbalance, clue(s"${fixture.id} should declare material imbalance"))
      assert(materialValueDiff(evaluated.board) != 0, clue(s"${fixture.id} should stay materially imbalanced"))
      if fixture.expectedTopThemes.nonEmpty then
        assert(
          fixture.expectedTopThemes.contains(topTheme(top)),
          clue(s"${fixture.id} top theme=${topTheme(top)} themes=${themes.toList.sorted.mkString(",")}")
        )
      assert(
        !fixture.forbiddenTopThemes.contains(topTheme(top)),
        clue(s"${fixture.id} should avoid top theme ${topTheme(top)}")
      )
      assert(
        themes.intersect(fixture.requiredSecondaryThemes).nonEmpty,
        clue(s"${fixture.id} lost compensation background: themes=${themes.toList.sorted.mkString(",")}")
      )
      if fixture.forbiddenTaskModes.nonEmpty then
        assert(
          !fixture.forbiddenTaskModes.contains(mode),
          clue(s"${fixture.id} should avoid task mode $mode")
        )
    }
  }
