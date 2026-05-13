package lila.commentary.chess

class TrapNegativeCorpusTest extends munit.FunSuite:

  test("neighbor fixtures remain in their owner and do not create Trap"):
    val rows = Vector(
      OwnerRow(
        "QueenHit",
        queenHitFacts,
        queenHitMove,
        TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get,
        Some(Tactic.QueenHit)
      ),
      OwnerRow(
        "Loose",
        looseFacts,
        looseMove,
        TacticLoose.write(looseFacts, Some(looseMove)).get,
        Some(Tactic.Loose)
      ),
      OwnerRow(
        "Material",
        materialFacts,
        materialMove,
        SceneMaterial.write(materialFacts, materialMove).get,
        None
      ),
      OwnerRow(
        "Hanging",
        materialFacts,
        materialMove,
        TacticHanging.write(materialFacts, materialMove).get,
        Some(Tactic.Hanging)
      ),
      OwnerRow(
        "Fork",
        forkFacts,
        forkMove,
        TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get,
        Some(Tactic.Fork)
      ),
      OwnerRow(
        "Skewer",
        skewerFacts,
        skewerMove,
        TacticSkewer
          .write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
          .get,
        Some(Tactic.Skewer)
      ),
      OwnerRow(
        "Pin",
        pinFacts,
        pinMove,
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get,
        Some(Tactic.Pin)
      ),
      OwnerRow(
        "RemoveGuard",
        removeGuardFacts,
        removeGuardMove,
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
          .get,
        Some(Tactic.RemoveGuard)
      ),
      OwnerRow(
        "Defense",
        defenseFacts,
        defenseMove,
        SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get,
        None
      ),
      OwnerRow(
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
          .get,
        Some(Tactic.Overload)
      )
    )

    rows.foreach: row =>
      assertEquals(row.story.tactic, row.expectedTactic, row.label)
      assertEquals(row.story.writer.exists(_ == StoryWriter.TacticTrap), false, row.label)
      assertEquals(TacticTrap.write(row.facts, Some(row.route)), None, row.label)
      assertEquals(TrapProof.fromBoardFacts(row.facts, row.route).complete, false, row.label)
      val verdict = StoryTable.choose(Vector(row.story)).head
      assertEquals(verdict.story.tactic.contains(Tactic.Trap), false, row.label)
      assertNoTrapPlanText(verdict, row.label)

  test("Trap Story does not borrow neighbor proof homes or renderer speech"):
    val story = TacticTrap.write(trapFacts, Some(trapMove)).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.tactic, Some(Tactic.Trap))
    assertEquals(story.writer, Some(StoryWriter.TacticTrap))
    assertEquals(story.trapProof.exists(_.complete), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.overloadProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.queenHitProof, None)
    assertEquals(story.loosePieceProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), Some("traps_piece"))
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan).get
    assertEquals(rendered.claimKey, "traps_piece")
    assertEquals(rendered.text, "Ra7 traps the piece on a8.")

  test("capped refuted and blocked Trap rows produce no public text"):
    val story = TacticTrap.write(trapFacts, Some(trapMove)).get
    val capped = TacticTrap.withEngineCheck(story, engineCheck(story, EngineCheckStatus.Caps)).get
    val refuted = story.copy(engineCheck = Some(engineCheck(story, EngineCheckStatus.Refutes)))
    val blocked = story.copy(target = Some(Square('b', 6)))

    Vector(capped, refuted, blocked).foreach: row =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(ExplanationPlan.fromSelected(verdict), None)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  private case class OwnerRow(label: String, facts: BoardFacts, route: Line, story: Story, expectedTactic: Option[Tactic])

  private def assertNoTrapPlanText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.exists(_.tactic.contains(Tactic.Trap)), false, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan).exists(_.claimKey == "traps_piece"), false, label)

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = trapFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(trapMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap negative corpus FEN: $fen -> $error"), identity)

  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))

  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val forkFacts = board("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
  private val pinFacts = board("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
  private val removeGuardFacts = board("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val defenseFacts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
  private val defenseThreat = Line(Square('f', 5), Square('d', 4))
  private val defenseMove = Line(Square('d', 4), Square('e', 4))
  private val overloadFacts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
