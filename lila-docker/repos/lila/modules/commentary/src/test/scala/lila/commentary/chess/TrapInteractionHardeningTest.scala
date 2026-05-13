package lila.commentary.chess

class TrapInteractionHardeningTest extends munit.FunSuite:

  test("Stage-0 Trap collision fixtures keep Trap and neighbor owners separate"):
    neighborFixtures.foreach: fixture =>
      val trap = trapStory
      val neighbor = fixture.story.copy(proof = orderingProof)
      val verdicts = StoryTable.choose(Vector(neighbor, trap))
      val trapVerdict = verdicts.find(_.story.writer.contains(StoryWriter.TacticTrap)).get
      val neighborVerdict = verdicts.find(_.story.writer == fixture.writer).get

      assertEquals(trapVerdict.story.tactic, Some(Tactic.Trap), fixture.label)
      assertEquals(trapVerdict.story.trapProof.exists(_.complete), true, fixture.label)
      assertEquals(trapVerdict.story.writer, Some(StoryWriter.TacticTrap), fixture.label)
      assertEquals(neighborVerdict.story.scene, fixture.scene, fixture.label)
      assertEquals(neighborVerdict.story.tactic, fixture.tactic, fixture.label)
      assertEquals(neighborVerdict.story.writer, fixture.writer, fixture.label)
      assertEquals(neighborVerdict.story.trapProof, None, fixture.label)
      assertEquals(TacticTrap.write(fixture.facts, Some(fixture.route)), None, fixture.label)
      assertEquals(TrapProof.fromBoardFacts(fixture.facts, fixture.route).complete, false, fixture.label)
      assertNoTrapText(neighborVerdict, fixture.label)

  test("Stage-0 Trap collision fixtures reject proof sidecar cross-contamination"):
    val trap = trapStory

    neighborFixtures.foreach: fixture =>
      val neighborWithTrapProof = fixture.story.copy(trapProof = trap.trapProof)
      val trapWithNeighborProof = fixture.contaminateTrap(trap)

      val neighborVerdict = StoryTable.choose(Vector(neighborWithTrapProof)).head
      val trapVerdict = StoryTable.choose(Vector(trapWithNeighborProof)).head

      assertEquals(neighborVerdict.story.writer, fixture.writer, fixture.label)
      assertNoTrapText(neighborVerdict, s"${fixture.label} with TrapProof")
      assertEquals(trapVerdict.story.writer, Some(StoryWriter.TacticTrap), fixture.label)
      assertEquals(trapVerdict.role, Role.Blocked, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(trapVerdict), None, fixture.label)

  test("Stage-0 Trap collision fixtures keep forbidden wording out of Trap output"):
    val verdict = StoryTable.choose(Vector(trapStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains(phrase), phrase)
      assertEquals(
        LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} ${phrase}.").accepted,
        false,
        phrase
      )

  private final case class NeighborFixture(
      label: String,
      facts: BoardFacts,
      route: Line,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: Option[StoryWriter],
      contaminateTrap: Story => Story
  )

  private def neighborFixtures: Vector[NeighborFixture] =
    Vector(
      NeighborFixture(
        "Trap near Loose",
        looseFacts,
        looseMove,
        TacticLoose.write(looseFacts, Some(looseMove)).get,
        Scene.Tactic,
        Some(Tactic.Loose),
        Some(StoryWriter.TacticLoose),
        trap => trap.copy(loosePieceProof = TacticLoose.write(looseFacts, Some(looseMove)).get.loosePieceProof)
      ),
      NeighborFixture(
        "Trap near Hanging",
        materialFacts,
        materialMove,
        TacticHanging.write(materialFacts, materialMove).get,
        Scene.Tactic,
        Some(Tactic.Hanging),
        Some(StoryWriter.TacticHanging),
        trap => trap.copy(captureResult = TacticHanging.write(materialFacts, materialMove).get.captureResult)
      ),
      NeighborFixture(
        "Trap near Material",
        materialFacts,
        materialMove,
        SceneMaterial.write(materialFacts, materialMove).get,
        Scene.Material,
        None,
        Some(StoryWriter.SceneMaterial),
        trap => trap.copy(captureResult = SceneMaterial.write(materialFacts, materialMove).get.captureResult)
      ),
      NeighborFixture(
        "Trap near QueenHit",
        queenHitFacts,
        queenHitMove,
        TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get,
        Scene.Tactic,
        Some(Tactic.QueenHit),
        Some(StoryWriter.TacticQueenHit),
        trap => trap.copy(queenHitProof = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.queenHitProof)
      ),
      NeighborFixture(
        "Trap near Pin",
        pinFacts,
        pinMove,
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get,
        Scene.Tactic,
        Some(Tactic.Pin),
        Some(StoryWriter.TacticPin),
        trap =>
          trap.copy(
            pinProof =
              TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get.pinProof
          )
      ),
      NeighborFixture(
        "Trap near RemoveGuard",
        removeGuardFacts,
        removeGuardMove,
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get,
        Scene.Tactic,
        Some(Tactic.RemoveGuard),
        Some(StoryWriter.TacticRemoveGuard),
        trap =>
          trap.copy(
            removeGuardProof =
              TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get.removeGuardProof
          )
      ),
      NeighborFixture(
        "Trap near Overload",
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
        Scene.Tactic,
        Some(Tactic.Overload),
        Some(StoryWriter.TacticOverload),
        trap =>
          trap.copy(
            overloadProof = TacticOverload
              .write(
                overloadFacts,
                Some(overloadMove),
                Some(Square('e', 6)),
                Some(Square('e', 7)),
                Some(Square('a', 6)),
                Some(Square('e', 7))
              )
              .get
              .overloadProof
          )
      ),
      NeighborFixture(
        "Trap near Fork",
        forkFacts,
        forkMove,
        TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get,
        Scene.Tactic,
        Some(Tactic.Fork),
        Some(StoryWriter.TacticFork),
        trap => trap.copy(multiTargetProof = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get.multiTargetProof)
      ),
      NeighborFixture(
        "Trap near Skewer",
        skewerFacts,
        skewerMove,
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get,
        Scene.Tactic,
        Some(Tactic.Skewer),
        Some(StoryWriter.TacticSkewer),
        trap =>
          trap.copy(
            skewerProof =
              TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get.skewerProof
          )
      )
    )

  private def assertNoTrapText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.flatMap(_.allowedClaim).contains(ExplanationClaim.TrapsPiece), false, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan).exists(_.claimKey == "traps_piece"), false, label)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap interaction FEN: $fen -> $error"), identity)

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private val forbiddenPhrases =
    Vector(
      "wins piece",
      "wins material",
      "forced",
      "only move",
      "best move",
      "no escape",
      "no counterplay",
      "queen trap",
      "rook trap"
    )

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

  private val orderingProof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 99,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )
