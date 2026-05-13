package lila.commentary.chess

class TrapCloseoutStage10Test extends munit.FunSuite:

  test("Stage-10 Trap closeout keeps one proof home label writer and speech key"):
    val story = TacticTrap.write(trapFacts, Some(trapMove)).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.trapProof.exists(_.complete), true)
    assertEquals(story.tactic, Some(Tactic.Trap))
    assertEquals(story.writer, Some(StoryWriter.TacticTrap))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.TrapsPiece))
    assertEquals(rendered.claimKey, "traps_piece")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      story.captureResult,
      story.multiTargetProof,
      story.threatProof,
      story.defenseProof,
      story.lineProof,
      story.pinProof,
      story.removeGuardProof,
      story.overloadProof,
      story.skewerProof,
      story.queenHitProof,
      story.loosePieceProof
    ).foreach(proofHome => assertEquals(proofHome, None))

  test("Stage-10 Trap closeout keeps neighbor owners from duplicating Trap"):
    neighborRows.foreach: row =>
      assertEquals(row.story.writer.contains(StoryWriter.TacticTrap), false, row.label)
      assertEquals(row.story.tactic.contains(Tactic.Trap), false, row.label)
      assertEquals(TacticTrap.write(row.facts, Some(row.route)), None, row.label)
      assertEquals(TrapProof.fromBoardFacts(row.facts, row.route).complete, false, row.label)
      assertEquals(StoryTable.choose(Vector(row.story)).exists(_.story.tactic.contains(Tactic.Trap)), false, row.label)

    val unsupportedEngineCheck =
      EngineCheck.fromStory(
        facts = trapFacts,
        story = None,
        engineLine = Some(EngineLine(Vector(trapMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(unsupportedEngineCheck.storyBound, false)
    assertEquals(TacticTrap.write(trapFacts, None), None)

  test("Stage-10 Trap closeout keeps closed tombstone admission gate"):
    val forgedClosedTrap =
      Story(
        scene = Scene.Tactic,
        tactic = Some(Tactic.Trap),
        proof = orderingProof,
        side = Side.White,
        rival = Side.Black,
        target = Some(Square('a', 8)),
        anchor = Some(Square('a', 7)),
        route = Some(trapMove),
        routeSan = Some("Ra7"),
        storyProof = StoryProof.fromBoardFacts(trapFacts, trapMove),
        writer = None
      )

    assertEquals(StoryTable.choose(Vector(forgedClosedTrap)), Vector.empty)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(trapStory)).head).flatMap(DeterministicRenderer.fromPlan).nonEmpty, true)

  private case class NeighborRow(label: String, facts: BoardFacts, route: Line, story: Story)

  private def neighborRows: Vector[NeighborRow] =
    Vector(
      NeighborRow("QueenHit", queenHitFacts, queenHitMove, TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get),
      NeighborRow("Loose", looseFacts, looseMove, TacticLoose.write(looseFacts, Some(looseMove)).get),
      NeighborRow("Material", materialFacts, materialMove, SceneMaterial.write(materialFacts, materialMove).get),
      NeighborRow("Hanging", materialFacts, materialMove, TacticHanging.write(materialFacts, materialMove).get),
      NeighborRow(
        "Fork",
        forkFacts,
        forkMove,
        TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
      ),
      NeighborRow(
        "Skewer",
        skewerFacts,
        skewerMove,
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
      ),
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
      NeighborRow("Defense", defenseFacts, defenseMove, SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get),
      NeighborRow(
        "Overload",
        overloadFacts,
        overloadMove,
        TacticOverload.write(
          overloadFacts,
          Some(overloadMove),
          Some(Square('e', 6)),
          Some(Square('e', 7)),
          Some(Square('a', 6)),
          Some(Square('e', 7))
        ).get
      )
    )

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Trap Stage-10 FEN: $fen -> $error"), identity)

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
