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

  private final case class Fixture(
      id: String,
      bank: Bank,
      label: String,
      fen: String,
      stockfishScoreCp: Int,
      pvMoves: List[String],
      expectedTopTheme: String,
      phase: String = "middlegame",
      requireMaterialParity: Boolean = false,
      requiredSecondaryThemes: Set[String] = Set.empty
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
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain
    ),
    Fixture(
      id = "TO2",
      bank = Bank.TacticalOverride,
      label = "exchange-up continuation still resolves concretely",
      fen = "r2q1rk1/1b1n1ppp/pp1bpn2/8/2PQ4/1PN2NP1/P3PPBP/2RR2K1 w - - 0 13",
      stockfishScoreCp = 240,
      pvMoves = List("d4d6", "d8b8", "d6b8", "a8b8", "d1d6", "h7h6", "c1d1", "f8d8", "e2e4", "e6e5", "g2f1"),
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain
    ),
    Fixture(
      id = "TO3",
      bank = Bank.TacticalOverride,
      label = "queen incursion aftermath stays forcing-first",
      fen = "2r2rk1/pQ3pp1/2p2n1p/3p4/3P4/q1P1PNRP/P4PP1/2R3K1 w - - 1 23",
      stockfishScoreCp = 43,
      pvMoves = List("c1f1", "f6e4", "g3g4", "e4f6", "g4f4", "a3a2", "f3e5", "c8b8", "b7c6", "f8c8"),
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain
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
      expectedTopTheme = PlanMatcher.Theme.FavorableExchange
    ),
    Fixture(
      id = "MI2",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus queen simplifies the edge",
      fen = "r1b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 1366,
      pvMoves = List("d4b5", "e7b4", "b5c7", "b4c3", "b2c3", "c8g4", "c7a8", "e8a8", "d1b3", "g4e2", "b3b7", "a8e8"),
      expectedTopTheme = PlanMatcher.Theme.FavorableExchange
    ),
    Fixture(
      id = "MI3",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus queen and rook stays conversion-led",
      fen = "2b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 1819,
      pvMoves = List("d4b5", "c8e6", "b5c7", "e8d8", "d1b3", "d5d4", "c7e6", "f7e6", "b3b7", "d4c3", "b7c6"),
      expectedTopTheme = PlanMatcher.Theme.FavorableExchange
    ),
    Fixture(
      id = "MI4",
      bank = Bank.MaterialImbalance,
      label = "K09 shell minus bishop pawn prefers simplification",
      fen = "r1bqr1k1/pp3pp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
      stockfishScoreCp = 596,
      pvMoves = List("a1c1", "c8e6", "d4e6", "f7e6", "e3c5", "d8d7", "e2e4", "d5d4", "c3b5"),
      expectedTopTheme = PlanMatcher.Theme.FavorableExchange
    ),
    Fixture(
      id = "MI5",
      bank = Bank.MaterialImbalance,
      label = "won-pawn aftermath for the defender still chooses relief",
      fen = "r2qk2r/1b1nbppp/pp1Qpn2/8/2P5/BPN2NP1/P3PPBP/R2R2K1 b kq - 0 11",
      stockfishScoreCp = -541,
      pvMoves = List("e7d6", "a3d6", "a8c8", "f3e5", "b7g2", "g1g2", "c8a8", "g2g1", "d8c8", "d1d4", "d7e5"),
      expectedTopTheme = PlanMatcher.Theme.FavorableExchange
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
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain,
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.Restriction, PlanMatcher.Theme.SpaceClamp)
    ),
    Fixture(
      id = "MR2",
      bank = Bank.MixedRegime,
      label = "open-file fight still routes through tactic first",
      fen = "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22",
      stockfishScoreCp = 43,
      pvMoves = List("b3b7", "d6a3", "c1f1", "f6e4", "g3g4", "e4c3", "b7d7", "a3a2", "f3e5"),
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain,
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.FlankInfrastructure, PlanMatcher.Theme.Restriction)
    ),
    Fixture(
      id = "MR3",
      bank = Bank.MixedRegime,
      label = "Dragon shell keeps its attack background but forcing line leads",
      fen = "2rq1rk1/pp1bppb1/3p1np1/4n2p/3NP2P/1BN1BP2/PPPQ2P1/2KR3R w - - 0 13",
      stockfishScoreCp = 29,
      pvMoves = List("c1b1", "e5c4", "b3c4", "c8c4", "d4e2", "b7b5", "e2d4", "b5b4", "c3e2", "e7e6", "b2b3"),
      expectedTopTheme = PlanMatcher.Theme.ImmediateTacticalGain,
      requireMaterialParity = true,
      requiredSecondaryThemes = Set(PlanMatcher.Theme.Redeployment, PlanMatcher.Theme.FlankInfrastructure)
    )
  )

  private val fixtures = tacticalOverrideFixtures ++ materialImbalanceFixtures ++ mixedRegimeFixtures

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
    assertEquals(fixtures.size, 11)
    assertEquals(fixtures.map(_.id).distinct.size, fixtures.size)
    assert(fixtures.forall(_.pvMoves.nonEmpty))
  }

  tacticalOverrideFixtures.foreach { fixture =>
    test(s"${fixture.id} tactical override: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      assertEquals(topTheme(top), fixture.expectedTopTheme)
      assertEquals(taskMode(evaluated.data), "ExplainTactics")
      assert(top.score >= 0.5, clue(s"${fixture.id} top score too low: ${top.score}"))
    }
  }

  materialImbalanceFixtures.foreach { fixture =>
    test(s"${fixture.id} material imbalance: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      assert(materialValueDiff(evaluated.board) != 0, clue(s"${fixture.id} should be materially imbalanced"))
      assertEquals(topTheme(top), fixture.expectedTopTheme)
      assert(simplifyBias(evaluated.data), clue(s"${fixture.id} should be in simplify/conversion mode"))
      assertEquals(taskMode(evaluated.data), "ExplainConvert")
    }
  }

  mixedRegimeFixtures.foreach { fixture =>
    test(s"${fixture.id} mixed regime: ${fixture.label}") {
      val evaluated = evaluatedFixtures.find(_.fixture.id == fixture.id).getOrElse(fail(s"missing ${fixture.id}"))
      val themes = evaluated.data.plans.take(4).map(topTheme).toSet
      val top = evaluated.data.plans.headOption.getOrElse(fail(s"missing top plan for ${fixture.id}"))

      if fixture.requireMaterialParity then
        assertEquals(materialValueDiff(evaluated.board), 0, clue(s"${fixture.id} should stay materially even"))

      assertEquals(topTheme(top), fixture.expectedTopTheme)
      assertEquals(taskMode(evaluated.data), "ExplainTactics")
      assert(
        themes.intersect(fixture.requiredSecondaryThemes).nonEmpty,
        clue(s"${fixture.id} lost strategic background: themes=${themes.toList.sorted.mkString(",")}")
      )
    }
  }
