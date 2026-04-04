package lila.strategicPuzzle

import play.api.libs.json.{ JsObject, Json }

import lila.core.userId.UserId
import lila.strategicPuzzle.StrategicPuzzle.*
import munit.FunSuite

class StrategicPuzzleLogicTest extends FunSuite:

  test("computeStreak only counts leading unique full clears") {
    val attempts = List(
      attempt("p3", StatusFull),
      attempt("p3", StatusFull),
      attempt("p2", StatusFull),
      attempt("p1", StatusPartial),
      attempt("p0", StatusFull)
    )
    assertEquals(computeStreak(attempts), 2)
    assertEquals(computeStreak(List(attempt("p1", StatusWrong))), 0)
  }

  test("chooseNextId prefers uncleared puzzles and stops after exhaustion") {
    val publicIds = List("a", "b", "c")
    val clearedIds = List("a")

    val unseen = PuzzleSelector.chooseNextId(publicIds, clearedIds, None, _ => 1)
    assertEquals(unseen, Some("c"))

    val exhausted = PuzzleSelector.chooseNextId(publicIds, List("a", "b", "c"), None, _ => 0)
    assertEquals(exhausted, None)

    val excluded = PuzzleSelector.chooseNextId(publicIds, List("a", "b", "c"), Some("c"), _ => 0)
    assertEquals(excluded, None)
  }

  test("newAttempt stores the plan-first attempt fields") {
    val doc =
      newAttempt(
        userId = UserId("tester"),
        puzzleId = "puzzle-1",
        status = StatusFull,
        lineUcis = List("e2e4", "e7e5", "g1f3"),
        planId = Some("plan_main"),
        startUci = Some("e2e4"),
        terminalId = Some("t1"),
        terminalFamilyKey = Some("pawn_break|e4")
      )

    assertEquals(doc.startUci, Some("e2e4"))
    assertEquals(doc.planId, Some("plan_main"))
    assertEquals(doc.terminalId, Some("t1"))
    assertEquals(doc.terminalFamilyKey, Some("pawn_break|e4"))
  }

  test("progressFromAttempts rebuilds unique latest attempts and a monotonic cleared set") {
    val attempts = List(
      attempt("p3", StatusFull),
      attempt("p3", StatusWrong),
      attempt("p2", StatusFull),
      attempt("p1", StatusPartial),
      attempt("p0", StatusFull)
    )

    val progress = progressFromAttempts(UserId("tester"), attempts)

    assertEquals(progress._id, "tester")
    assertEquals(progress.currentStreak, 2)
    assertEquals(progress.latestAttemptsByPuzzle.map(_.puzzleId), List("p3", "p2", "p1", "p0"))
    assertEquals(progress.latestAttemptsByPuzzle.map(_.status), List(StatusFull, StatusFull, StatusPartial, StatusFull))
    assertEquals(progress.clearedPuzzleIds, List("p3", "p2", "p0"))
  }

  test("applyAttempt refreshes the latest unique puzzle attempt and keeps cleared puzzles monotonic") {
    val base =
      ProgressDoc(
        _id = "tester",
        currentStreak = 1,
        latestAttemptsByPuzzle = List(
          AttemptSummary("p2", StatusFull, "2026-03-18T00:00:00Z"),
          AttemptSummary("p1", StatusPartial, "2026-03-17T00:00:00Z")
        ),
        clearedPuzzleIds = List("p2"),
        updatedAt = "2026-03-18T00:00:00Z"
      )

    val updated = applyAttempt(base, attempt("p1", StatusFull))

    assertEquals(updated.currentStreak, 2)
    assertEquals(updated.latestAttemptsByPuzzle.map(_.puzzleId), List("p1", "p2"))
    assertEquals(updated.latestAttemptsByPuzzle.map(_.status), List(StatusFull, StatusFull))
    assertEquals(updated.clearedPuzzleIds, List("p1", "p2"))
  }

  test("strategic puzzle payload omits nested runtime shell when stripped for public bootstrap") {
    val doc =
      StrategicPuzzleDoc(
        id = "puzzle-1",
        schema = "chesstory.strategicPuzzle.v1",
        source = SourcePayload(seedId = "seed-1", opening = Some("Sample Opening"), eco = Some("C20")),
        position = PositionPayload(fen = "sample-fen", sideToMove = "white"),
        dominantFamily = Some(DominantFamilySummary("pawn_break|e4", "pawn_break", "e4")),
        qualityScore = QualityScore(12),
        generationMeta = GenerationMeta(PublicSelectionStatus),
        runtimeShell = None
      )

    val json = Json.toJson(doc).as[JsObject]

    assert(!json.keys.contains("runtimeShell"))
  }

  test("materialize promotes a public plan layer without dropping proof data") {
    assertEquals(publicShell.schema, RuntimeShellSchemaV2)
    assertEquals(publicShell.plans.map(_.allowedStarts.map(_.uci)), List(List("e2e4"), List("d2d4")))
    assertEquals(publicShell.proof.rootChoices.map(_.uci), List("e2e4", "d2d4"))
    assertEquals(publicShell.proof.nodes.map(_.id), List("n1"))
    assertEquals(publicShell.proof.forcedReplies.map(_.uci), List("e7e5"))

    val fullTerminal = publicShell.terminals.find(_.id == "t-full").getOrElse(fail("full terminal missing"))
    assertEquals(fullTerminal.planId, publicShell.plans.headOption.map(_.id))
    assertEquals(fullTerminal.acceptedStarts, List("e4"))
    assertEquals(fullTerminal.featuredStart, Some("e4"))
    assert(fullTerminal.planTask.exists(_.nonEmpty))
    assert(fullTerminal.whyPlan.exists(_.nonEmpty))
    assert(fullTerminal.whyMove.exists(_.nonEmpty))
  }

  test("runtime shell json round-trips plan-first and proof layers together") {
    val roundTrip = Json.toJson(publicShell).as[RuntimeShell]

    assertEquals(roundTrip.schema, RuntimeShellSchemaV2)
    assertEquals(roundTrip.plans.map(_.id), publicShell.plans.map(_.id))
    assertEquals(roundTrip.proof.rootChoices.map(_.uci), publicShell.proof.rootChoices.map(_.uci))
    assertEquals(roundTrip.proof.forcedReplies.map(_.uci), publicShell.proof.forcedReplies.map(_.uci))
    assertEquals(roundTrip.terminals.find(_.id == "t-full").flatMap(_.planId), publicShell.terminals.find(_.id == "t-full").flatMap(_.planId))
  }

  test("resolveCompletion accepts plan-first completion and derives the canonical proof line") {
    val fullPlan = publicShell.plans.find(_.allowedStarts.exists(_.uci == "e2e4")).getOrElse(fail("full plan missing"))
    val req = CompleteRequest(
      planId = Some(fullPlan.id),
      startUci = Some("e2e4")
    )

    val resolved = resolveCompletion(publicShell, req)

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusFull,
          lineUcis = List("e2e4", "e7e5", "g1f3"),
          planId = Some(fullPlan.id),
          startUci = Some("e2e4"),
          terminalId = Some("t-full"),
          terminalFamilyKey = Some("pawn_break|e4")
        )
      )
    )
  }

  test("resolveCompletion rejects solve requests without the plan-first contract") {
    assertEquals(resolveCompletion(publicShell, CompleteRequest()), None)
    assertEquals(resolveCompletion(publicShell, CompleteRequest(planId = Some("missing"), startUci = Some("e2e4"))), None)
    assertEquals(resolveCompletion(publicShell, CompleteRequest(planId = publicShell.plans.headOption.map(_.id))), None)
  }

  test("resolveCompletion derives canonical giveup line from the materialized runtime shell") {
    val req = CompleteRequest(
      giveUp = true
    )

    val resolved = resolveCompletion(publicShell, req)
    val fullPlan = publicShell.plans.find(_.allowedStarts.exists(_.uci == "e2e4")).getOrElse(fail("full plan missing"))

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusGiveUp,
          lineUcis = List("e2e4", "e7e5", "g1f3"),
          planId = Some(fullPlan.id),
          startUci = Some("e2e4"),
          terminalId = Some("t-full"),
          terminalFamilyKey = Some("pawn_break|e4")
        )
      )
    )
  }

  test("resolveCompletion uses the selected plan for giveup when a plan id is supplied") {
    val partialPlan = publicShell.plans.find(_.allowedStarts.exists(_.uci == "d2d4")).getOrElse(fail("partial plan missing"))
    val req = CompleteRequest(
      planId = Some(partialPlan.id),
      giveUp = true
    )

    val resolved = resolveCompletion(publicShell, req)

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusGiveUp,
          lineUcis = List("d2d4"),
          planId = Some(partialPlan.id),
          startUci = Some("d2d4"),
          terminalId = Some("t-part"),
          terminalFamilyKey = Some("quiet|d4")
        )
      )
    )
  }

  test("resolveCompletion scores a partial start as partial even when the proof terminal is full") {
    val req = CompleteRequest(
      planId = Some("plan_shared_task"),
      startUci = Some("c2c4")
    )

    val resolved = resolveCompletion(mixedCreditPublicShell, req)

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusPartial,
          lineUcis = List("c2c4"),
          planId = Some("plan_shared_task"),
          startUci = Some("c2c4"),
          terminalId = Some("t-shared"),
          terminalFamilyKey = Some("file_bind|e4")
        )
      )
    )
  }

  private val sampleShell =
    RuntimeShell(
      schema = RuntimeShellSchema,
      startFen = "sample-start",
      sideToMove = "white",
      prompt = "Find the route",
      proof = RuntimeProofLayer(
        rootChoices = List(
          ShellChoice(
            uci = "e2e4",
            san = "e4",
            credit = StatusFull,
            nextNodeId = Some("n1"),
            terminalId = None,
            afterFen = Some("after-e4"),
            familyKey = None,
            label = Some("Main line"),
            feedback = "Good start"
          ),
          ShellChoice(
            uci = "d2d4",
            san = "d4",
            credit = StatusPartial,
            nextNodeId = None,
            terminalId = Some("t-part"),
            afterFen = Some("after-d4"),
            familyKey = None,
            label = Some("Alternate"),
            feedback = "Playable, but softer"
          )
        ),
        nodes = List(
          PlayerNode(
            id = "n1",
            step = 2,
            fen = "after-e5",
            prompt = "Keep the pressure",
            badMoveFeedback = "Not the plan",
            choices = List(
              ShellChoice(
                uci = "g1f3",
                san = "Nf3",
                credit = StatusFull,
                nextNodeId = None,
                terminalId = Some("t-full"),
                afterFen = Some("after-nf3"),
                familyKey = Some("pawn_break|e4"),
                label = Some("Finish"),
                feedback = "Complete the route"
              )
            )
          )
        ),
        forcedReplies = List(
          ForcedReply(
            id = "reply-1",
            fromNodeId = "root:e2e4",
            uci = "e7e5",
            san = "e5",
            afterFen = "after-e5",
            nextNodeId = Some("n1")
          )
        )
      ),
      terminals = List(
        TerminalReveal(
          id = "t-full",
          outcome = StatusFull,
          title = "Main plan lands",
          summary = "The route holds.",
          commentary = "Full credit terminal.",
          familyKey = Some("pawn_break|e4"),
          dominantIdeaKind = Some("plan"),
          anchor = Some("e4"),
          lineSan = List("e4", "e5", "Nf3"),
          siblingMoves = List("Nc3"),
          opening = Some("Sample Opening"),
          eco = Some("C20"),
          dominantFamilyKey = Some("pawn_break|e4")
        ),
        TerminalReveal(
          id = "t-part",
          outcome = StatusPartial,
          title = "Playable but softer",
          summary = "The idea survives in reduced form.",
          commentary = "Partial terminal.",
          familyKey = Some("quiet|d4"),
          dominantIdeaKind = Some("plan"),
          anchor = Some("d4"),
          lineSan = List("d4"),
          siblingMoves = Nil,
          opening = Some("Sample Opening"),
          eco = Some("D00"),
          dominantFamilyKey = Some("quiet|d4")
        )
      )
    )

  private val publicShell =
    sampleShell.materialize(
      Some(
        DominantFamilySummary(
          key = "pawn_break|e4",
          dominantIdeaKind = "pawn_break",
          anchor = "e4"
        )
      )
    )

  private val mixedCreditPublicShell =
    RuntimeShell(
      schema = RuntimeShellSchema,
      startFen = "sample-start",
      sideToMove = "white",
      prompt = "Take over the e-file before the counterplay arrives.",
      plans = List(
        PuzzlePlan(
          id = "plan_shared_task",
          familyKey = Some("file_bind|e4"),
          dominantIdeaKind = Some("line_occupation"),
          anchor = Some("e-file"),
          task = "Take over the e-file before the counterplay arrives.",
          feedback = "The task is right, but not every start carries full credit.",
          allowedStarts = List(
            PlanStart(
              uci = "e2e4",
              san = "e4",
              credit = StatusFull,
              label = Some("Main start"),
              feedback = "This start carries the task at full strength.",
              afterFen = Some("after-e4"),
              terminalId = Some("t-shared")
            ),
            PlanStart(
              uci = "c2c4",
              san = "c4",
              credit = StatusPartial,
              label = Some("Softer start"),
              feedback = "This start reaches the same task more softly.",
              afterFen = Some("after-c4"),
              terminalId = Some("t-shared")
            )
          ),
          featuredTerminalId = "t-shared",
          featuredStartUci = Some("e2e4")
        )
      ),
      proof = RuntimeProofLayer(
        rootChoices = List(
          ShellChoice(
            uci = "e2e4",
            san = "e4",
            credit = StatusFull,
            nextNodeId = None,
            terminalId = Some("t-shared"),
            afterFen = Some("after-e4"),
            familyKey = Some("file_bind|e4"),
            label = Some("Main start"),
            feedback = "This start carries the task at full strength."
          ),
          ShellChoice(
            uci = "c2c4",
            san = "c4",
            credit = StatusPartial,
            nextNodeId = None,
            terminalId = Some("t-shared"),
            afterFen = Some("after-c4"),
            familyKey = Some("file_bind|e4"),
            label = Some("Softer start"),
            feedback = "This start reaches the same task more softly."
          )
        ),
        nodes = Nil,
        forcedReplies = Nil
      ),
      terminals = List(
        TerminalReveal(
          id = "t-shared",
          outcome = StatusFull,
          title = "The file bind lands",
          summary = "The shared task still works.",
          commentary = "The proof line is full, but the softer start remains partial credit.",
          familyKey = Some("file_bind|e4"),
          dominantIdeaKind = Some("line_occupation"),
          anchor = Some("e-file"),
          lineSan = List("e4"),
          siblingMoves = List("c4"),
          opening = Some("Sample Opening"),
          eco = Some("A00"),
          dominantFamilyKey = Some("file_bind|e4"),
          planId = Some("plan_shared_task")
        )
      )
    ).materialize(
      Some(
        DominantFamilySummary(
          key = "file_bind|e4",
          dominantIdeaKind = "line_occupation",
          anchor = "e-file"
        )
      )
    )

  private def attempt(puzzleId: String, status: String): AttemptDoc =
    AttemptDoc(
      _id = s"attempt_$puzzleId",
      schema = AttemptSchema,
      userId = "tester",
      puzzleId = puzzleId,
      status = status,
      lineUcis = Nil,
      terminalId = None,
      terminalFamilyKey = None,
      completedAt = "2026-03-18T00:00:00Z"
    )
