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
    val relation = OpeningPrecedentBranching.relationSentence(ctx, ctx.openingData, requireFocus = true).getOrElse(fail("missing relation"))
    assert(relation.toLowerCase.contains("keeps the game inside"))
    assert(relation.toLowerCase.contains("queenside pressure branch"))
  }

  test("out-of-book opening move is described as bending away from the representative branch") {
    val outOfBookCtx =
      ctx.copy(
        playedMove = Some("g2g4"),
        playedSan = Some("g4"),
        openingEvent = Some(OpeningEvent.OutOfBook("g4", List("b3", "dxc5", "Qc2"), 16))
      )
    val relation = OpeningPrecedentBranching.relationSentence(outOfBookCtx, outOfBookCtx.openingData, requireFocus = true).getOrElse(fail("missing relation"))
    assert(relation.toLowerCase.contains("bends away"))
    assert(relation.toLowerCase.contains("queenside pressure"))
  }

  test("generic opening hints upgrade to flank fianchetto support when precedent route shows the pattern") {
    val fianchettoCtx =
      ctx.copy(
        summary = NarrativeSummary("Development", None, "NarrowChoice", "Maintain", "+0.12"),
        plans = PlanTable(
          top5 = List(PlanRow(rank = 1, name = "Development", score = 0.62, evidence = List("quiet setup"))),
          suppressed = Nil
        ),
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "opening_development",
            planName = "Opening Development",
            rank = 1,
            score = 0.71,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = lila.llm.model.authoring.PlanViability(0.73, "high", "coordinated"),
            themeL1 = "opening_principles"
          )
        ),
        openingEvent = Some(OpeningEvent.BranchPoint(List("g3", "Bg2", "O-O"), "Main line shifts", Some("lichess.org/game3"))),
        openingData = ctx.openingData.map(_.copy(sampleGames =
          List(
            ExplorerGame(
              id = "game3",
              winner = Some(White),
              white = ExplorerPlayer("Gelfand, Boris", 2750),
              black = ExplorerPlayer("Topalov, Veselin", 2785),
              year = 2010,
              month = 5,
              event = Some("Grand Prix"),
              pgn = Some("9. g3 Bg7 10. Bg2 O-O 11. O-O")
            )
          )
        ))
      )

    val precedent = OpeningPrecedentBranching.representative(fianchettoCtx, fianchettoCtx.openingData, requireFocus = false).getOrElse(fail("missing precedent"))
    assertEquals(precedent.branchLabel, "flank fianchetto support")
    assertEquals(precedent.mechanism, OpeningBranchMechanism.StructuralTransformation)
    assert(precedent.summarySentence.toLowerCase.contains("flank fianchetto support"))
  }

  test("generic opening hints upgrade to early queen exposure when precedent route is queen-led") {
    val queenCtx =
      ctx.copy(
        summary = NarrativeSummary("Development", None, "NarrowChoice", "Maintain", "+0.08"),
        plans = PlanTable(
          top5 = List(PlanRow(rank = 1, name = "Development", score = 0.58, evidence = List("piece activity"))),
          suppressed = Nil
        ),
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "opening_development",
            planName = "Opening Development",
            rank = 1,
            score = 0.66,
            preconditions = Nil,
            executionSteps = Nil,
            failureModes = Nil,
            viability = lila.llm.model.authoring.PlanViability(0.68, "medium", "queen active"),
            themeL1 = "opening_principles"
          )
        ),
        openingEvent = Some(OpeningEvent.Intro("B01", "Scandinavian Defense", "early queen exposure", List("Qxd5", "Nc3", "Qa4"))),
        openingData = ctx.openingData.map(_.copy(sampleGames =
          List(
            ExplorerGame(
              id = "game4",
              winner = Some(Black),
              white = ExplorerPlayer("Ivanchuk, Vassily", 2740),
              black = ExplorerPlayer("Karjakin, Sergey", 2760),
              year = 2012,
              month = 9,
              event = Some("Tal Memorial"),
              pgn = Some("3. Qxd4 Nc6 4. Qa4 Nf6 5. Nc3")
            )
          )
        ))
      )

    val precedent = OpeningPrecedentBranching.representative(queenCtx, queenCtx.openingData, requireFocus = false).getOrElse(fail("missing precedent"))
    assertEquals(precedent.branchLabel, "early queen exposure")
    assertEquals(precedent.mechanism, OpeningBranchMechanism.InitiativeSwing)
    assert(precedent.representativeSentence.toLowerCase.contains("early queen exposure branch"))
  }

  test("outline keeps the released precedent summary family singular after comparison assembly") {
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val released = outline.beats.map(_.text).mkString(" ")
    val summaryMarkers = List(
      "Across these branches,",
      "Common pattern:",
      "All cited branches revolve around",
      "The recurring practical theme across these games is",
      "These precedent lines point to one key driver:"
    )

    assertEquals(summaryMarkers.count(released.contains), 1, clue(released))
    assert(!released.contains("Shared lesson:"), clue(released))
  }
