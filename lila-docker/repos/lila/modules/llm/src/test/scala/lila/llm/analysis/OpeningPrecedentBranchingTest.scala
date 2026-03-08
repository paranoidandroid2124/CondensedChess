package lila.llm.analysis

import munit.FunSuite
import chess.{ Black, White }
import lila.llm.model.*
import lila.llm.model.authoring.PlanHypothesis

class OpeningPrecedentBranchingTest extends FunSuite:

  private def ctx: NarrativeContext =
    NarrativeContext(
      fen = "r1bq1rk1/pp3ppp/2n1pn2/2bp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w - - 0 8",
      header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 16,
      playedMove = Some("b2b3"),
      playedSan = Some("b3"),
      summary = NarrativeSummary("Queenside Pressure", None, "NarrowChoice", "Maintain", "+0.18"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(PlanRow(rank = 1, name = "Queenside Pressure", score = 0.81, evidence = List("c-file pressure"))),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Opening", "Branching opening position"),
      candidates = List(
        CandidateInfo(
          move = "b3",
          annotation = "",
          planAlignment = "Queenside Pressure",
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "queenside_pressure",
          planName = "Queenside Pressure",
          rank = 1,
          score = 0.84,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = lila.llm.model.authoring.PlanViability(0.8, "high", "slow"),
          themeL1 = "open_file"
        )
      ),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Qc2", "b3", "dxc5"), "Main line shifts", Some("lichess.org/game1"))),
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 120,
          topMoves = List(
            ExplorerMove("b2b3", "b3", 38, 16, 10, 12, 2520),
            ExplorerMove("d4c5", "dxc5", 31, 13, 8, 10, 2510)
          ),
          sampleGames = List(
            ExplorerGame(
              id = "game1",
              winner = Some(White),
              white = ExplorerPlayer("Kramnik, Vladimir", 2810),
              black = ExplorerPlayer("Anand, Viswanathan", 2791),
              year = 2008,
              month = 10,
              event = Some("WCh"),
              pgn = Some("9. b3 Qe7 10. Bb2 Rd8 11. Rc1")
            ),
            ExplorerGame(
              id = "game2",
              winner = Some(Black),
              white = ExplorerPlayer("Aronian, Levon", 2780),
              black = ExplorerPlayer("Carlsen, Magnus", 2862),
              year = 2013,
              month = 4,
              event = Some("Candidates"),
              pgn = Some("9. Ne5 c6 10. Qa4 Nbd7 11. Rd1")
            )
          )
        )
      ),
      renderMode = NarrativeRenderMode.Bookmaker
    )

  test("representative precedent selects a player game and maps it to a strategic branch") {
    val precedent = OpeningPrecedentBranching.representative(ctx, ctx.openingData, requireFocus = true).getOrElse(fail("missing precedent"))
    assert(precedent.representativeSentence.contains("Vladimir Kramnik-Viswanathan Anand"))
    assert(precedent.representativeSentence.toLowerCase.contains("queenside pressure branch"))
    assert(precedent.summarySentence.toLowerCase.contains("queenside pressure branch"))
  }
