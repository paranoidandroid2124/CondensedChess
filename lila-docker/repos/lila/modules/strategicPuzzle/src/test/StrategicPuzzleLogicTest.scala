package lila.strategicPuzzle

import lila.core.userId.UserId
import lila.strategicPuzzle.StrategicPuzzle.*
import munit.FunSuite

class StrategicPuzzleLogicTest extends FunSuite:

  test("normalizeStatus prefers giveup and rejects unknown statuses") {
    assertEquals(normalizeStatus(StatusFull, giveUp = false), StatusFull)
    assertEquals(normalizeStatus(StatusPartial, giveUp = false), StatusPartial)
    assertEquals(normalizeStatus("mystery", giveUp = false), StatusWrong)
    assertEquals(normalizeStatus(StatusFull, giveUp = true), StatusGiveUp)
  }

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

  test("newAttempt stores root move from line head") {
    val doc =
      newAttempt(
        userId = UserId("tester"),
        puzzleId = "puzzle-1",
        status = StatusFull,
        lineUcis = List("e2e4", "e7e5", "g1f3"),
        terminalId = Some("t1"),
        terminalFamilyKey = Some("pawn_break|e4")
      )

    assertEquals(doc.rootUci, Some("e2e4"))
    assertEquals(doc.terminalId, Some("t1"))
    assertEquals(doc.terminalFamilyKey, Some("pawn_break|e4"))
  }

  test("resolveCompletion derives terminal outcome from the runtime shell") {
    val req = CompleteRequest(
      lineUcis = List("e2e4", "e7e5", "g1f3"),
      status = StatusWrong,
      terminalId = Some("t-full"),
      giveUp = false
    )

    val resolved = resolveCompletion(sampleShell, req)

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusFull,
          lineUcis = List("e2e4", "e7e5", "g1f3"),
          terminalId = Some("t-full"),
          terminalFamilyKey = Some("pawn_break|e4")
        )
      )
    )
  }

  test("resolveCompletion rejects invalid non-terminal lines") {
    val req = CompleteRequest(
      lineUcis = List("e2e4", "g1f3"),
      status = StatusFull,
      terminalId = Some("t-full"),
      giveUp = false
    )

    assertEquals(resolveCompletion(sampleShell, req), None)
  }

  test("resolveCompletion derives canonical giveup line from the runtime shell") {
    val req = CompleteRequest(
      lineUcis = Nil,
      status = StatusFull,
      terminalId = None,
      giveUp = true
    )

    val resolved = resolveCompletion(sampleShell, req)

    assertEquals(
      resolved,
      Some(
        ResolvedCompletion(
          status = StatusGiveUp,
          lineUcis = List("e2e4", "e7e5", "g1f3"),
          terminalId = Some("t-full"),
          terminalFamilyKey = Some("pawn_break|e4")
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

  private def attempt(puzzleId: String, status: String): AttemptDoc =
    AttemptDoc(
      _id = s"attempt_$puzzleId",
      schema = AttemptSchema,
      userId = "tester",
      puzzleId = puzzleId,
      status = status,
      lineUcis = Nil,
      rootUci = None,
      terminalId = None,
      terminalFamilyKey = None,
      completedAt = "2026-03-18T00:00:00Z"
    )
