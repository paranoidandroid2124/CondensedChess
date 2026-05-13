package lila.commentary.chess

import java.nio.file.{ Files, Paths }
import java.util.Locale

class TrapCloseoutCleanupTest extends munit.FunSuite:

  test("Trap owns only TrapProof TacticTrap Tactic.Trap and traps_piece"):
    val story = trapStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.trapProof.exists(_.complete), true)
    assertEquals(story.tactic, Some(Tactic.Trap))
    assertEquals(story.writer, Some(StoryWriter.TacticTrap))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.TrapsPiece))
    assertEquals(rendered.claimKey, "traps_piece")
    assertEquals(ExplanationClaim.values.count(_ == ExplanationClaim.TrapsPiece), 1)
    assertEquals(Tactic.values.count(_ == Tactic.Trap), 1)
    assertEquals(StoryWriter.values.count(_ == StoryWriter.TacticTrap), 1)
    assertEquals(plan.allowedClaim.exists(materialOrForcedClaim), false)

    Vector(
      story.captureResult,
      story.multiTargetProof,
      story.lineProof,
      story.pinProof,
      story.removeGuardProof,
      story.overloadProof,
      story.skewerProof,
      story.queenHitProof,
      story.loosePieceProof,
      story.threatProof,
      story.defenseProof
    ).foreach(proofHome => assertEquals(proofHome, None))

  test("Trap closeout keeps neighbors and EngineCheck from duplicating Trap"):
    neighborRows.foreach: row =>
      assertEquals(row.story.writer.contains(StoryWriter.TacticTrap), false, row.label)
      assertEquals(row.story.tactic.contains(Tactic.Trap), false, row.label)
      assertEquals(row.story.trapProof, None, row.label)
      assertEquals(TacticTrap.write(row.facts, Some(row.route)), None, row.label)
      assertEquals(TrapProof.fromBoardFacts(row.facts, row.route).complete, false, row.label)
      assertEquals(StoryTable.choose(Vector(row.story)).exists(_.story.tactic.contains(Tactic.Trap)), false, row.label)

    val engineOnly =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(trapMove),
        engineLine = Some(EngineLine(Vector(trapMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticTrap.withEngineCheck(trapStory, engineOnly), None)

  test("Trap closeout keeps runtime names and public surfaces closed"):
    val runtimeNames =
      Tactic.values.map(_.toString).toVector ++
        StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.toString).toVector
    val forbiddenRuntimeNameParts = Vector("Stage", "Workflow", "Audit", "Gate", "Checklist", "BroadTrap")

    forbiddenRuntimeNameParts.foreach: forbidden =>
      assertEquals(runtimeNames.exists(_.contains(forbidden)), false, forbidden)

    val routes = Files.readString(Paths.get("conf/routes"))
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))
    assert(routes.contains("POST  /api/commentary/render"))
    assert(routes.contains("POST  /internal/commentary/render-local-probe"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(controller.contains("\"status\" -> \"unavailable\""))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "public route 200 remains closed")
    assert(!controller.contains("TacticTrap"), "public route must not expose Trap writer")
    assert(!controller.contains("TrapProof"), "public route must not expose TrapProof")
    assert(!controller.contains("LlmNarrationSmoke"), "public/user-facing LLM narration remains closed")

  test("Trap closeout keeps material forced and no-escape claims rejected"):
    val verdict = StoryTable.choose(Vector(trapStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    Vector(
      "wins piece" -> "Ra7 traps the piece on a8 and wins a piece.",
      "wins material" -> "Ra7 traps the piece on a8 and wins material.",
      "forced" -> "Ra7 traps the piece on a8 by force.",
      "only move" -> "Ra7 is the only move.",
      "best move" -> "Ra7 is the best move.",
      "no escape" -> "Ra7 traps the piece on a8 with no escape.",
      "no counterplay" -> "Ra7 traps the piece on a8 with no counterplay.",
      "engine" -> "The engine confirms Ra7 traps the piece.",
      "raw pv" -> "The raw PV proves Ra7 traps the piece."
    ).foreach: (label, output) =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, label)

    val renderedText = rendered.text.toLowerCase(Locale.ROOT)
    Vector("wins piece", "wins material", "forced", "only move", "best move", "no escape").foreach: phrase =>
      assert(!renderedText.contains(phrase), phrase)

  private case class NeighborRow(label: String, facts: BoardFacts, route: Line, story: Story)

  private def neighborRows: Vector[NeighborRow] =
    Vector(
      NeighborRow("Loose", looseFacts, looseMove, TacticLoose.write(looseFacts, Some(looseMove)).get),
      NeighborRow("Hanging", materialFacts, materialMove, TacticHanging.write(materialFacts, materialMove).get),
      NeighborRow("Material", materialFacts, materialMove, SceneMaterial.write(materialFacts, materialMove).get),
      NeighborRow("QueenHit", queenHitFacts, queenHitMove, TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get),
      NeighborRow(
        "Pin",
        pinFacts,
        pinMove,
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get
      ),
      NeighborRow(
        "RemoveGuard",
        removeGuardFacts,
        removeGuardMove,
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
      ),
      NeighborRow(
        "Overload",
        overloadFacts,
        overloadMove,
        TacticOverload
          .write(
            overloadFacts,
            Some(overloadMove),
            Some(Square('e', 6)),
            Some(Square('e', 7)),
            Some(Square('a', 6)),
            Some(Square('e', 7))
          )
          .get
      ),
      NeighborRow("Fork", forkFacts, forkMove, TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get),
      NeighborRow(
        "Skewer",
        skewerFacts,
        skewerMove,
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
      )
    )

  private def materialOrForcedClaim(claim: ExplanationClaim): Boolean =
    claim == ExplanationClaim.MaterialBalanceChanges ||
      claim == ExplanationClaim.LineLeavesMaterialGain ||
      claim == ExplanationClaim.ExchangeLeavesSideAhead ||
      claim == ExplanationClaim.CanWinPiece ||
      claim == ExplanationClaim.CaptureLeavesMaterialGain

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap closeout cleanup FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val pinFacts = board("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
  private val removeGuardFacts = board("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val forkFacts = board("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
