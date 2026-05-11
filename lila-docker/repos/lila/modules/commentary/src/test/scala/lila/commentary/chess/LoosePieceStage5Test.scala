package lila.commentary.chess

class LoosePieceStage5Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-5 StoryTable orders standalone proof-backed Loose"):
    val loose = looseStory.copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(loose)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.story.tactic, Some(Tactic.Loose))
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticLoose))

  test("Stage-5 StoryTable keeps collision targets in their existing proof homes"):
    collisionRows.foreach: row =>
      val loose = row.loose.copy(proof = orderingProof)
      val claim = row.claim.copy(proof = orderingProof)
      val verdicts = StoryTable.choose(Vector(loose, claim))
      val lead = verdicts.find(_.role == Role.Lead).get
      val looseVerdict = verdicts.find(_.story.tactic.contains(Tactic.Loose)).get

      assertEquals(lead.story.writer, row.claim.writer, row.label)
      assertEquals(lead.story.scene, row.claim.scene, row.label)
      assertEquals(lead.story.tactic, row.claim.tactic, row.label)
      assertEquals(looseVerdict.story.tactic, Some(Tactic.Loose), row.label)
      assertEquals(looseVerdict.role == Role.Lead, false, row.label)
      assertEquals(ExplanationPlan.fromSelected(looseVerdict), None, row.label)

  test("Stage-5 Support Context Blocked capped refuted and non-Lead Loose rows do not speak"):
    val facts = board(looseFen)
    val loose = looseStory.copy(proof = orderingProof)
    val materialRow = SceneMaterial
      .write(board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"), Line(Square('d', 4), Square('e', 5)))
      .get
      .copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(materialRow, loose)).find(_.story.tactic.contains(Tactic.Loose)).get
    val context = StoryTable.choose(Vector(loose.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(loose.copy(writer = None))).head
    val capped = StoryTable
      .choose(Vector(TacticLoose.withEngineCheck(loose, check(facts, loose, EngineCheckStatus.Caps)).get))
      .head
    val refuted = StoryTable
      .choose(
        Vector(TacticLoose.withEngineCheck(loose, check(facts, loose, EngineCheckStatus.Supports, before = 220, after = 20)).get)
      )
      .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, verdict.toString)

  private final case class CollisionRow(label: String, claim: Story, loose: Story)

  private def collisionRows: Vector[CollisionRow] =
    val genericLoose = looseStory

    val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get

    val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    val queenHitMove = Line(Square('d', 2), Square('h', 2))
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val queenLoose = TacticLoose.write(queenHitFacts, Some(queenHitMove)).get

    val forkFacts = board("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    val discoveredFacts = board("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredPinMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredPinMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get

    val pinFacts = board("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val pin =
      TacticPin
        .write(
          pinFacts,
          Some(discoveredPinMove),
          Some(Square('b', 1)),
          Some(Square('g', 6)),
          Some(Square('h', 7))
        )
        .get

    val removeFacts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeMove = Line(Square('d', 3), Square('e', 5))
    val remove = TacticRemoveGuard.write(removeFacts, Some(removeMove), Some(Square('g', 6)), Some(Square('e', 5))).get

    val defenseFacts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get

    Vector(
      CollisionRow("Scene.Material", material, genericLoose),
      CollisionRow("Tactic.Hanging", hanging, genericLoose),
      CollisionRow("Tactic.QueenHit", queenHit, queenLoose),
      CollisionRow("Tactic.Fork", fork, genericLoose),
      CollisionRow("Tactic.Skewer", skewer, genericLoose),
      CollisionRow("Tactic.Pin", pin, genericLoose),
      CollisionRow("Tactic.RemoveGuard", remove, genericLoose),
      CollisionRow("Tactic.DiscoveredAttack", discovered, genericLoose),
      CollisionRow("Scene.Defense", defense, genericLoose)
    )

  private def looseStory: Story =
    TacticLoose.write(board(looseFen), Some(looseMove)).get

  private def check(
      facts: BoardFacts,
      story: Story,
      status: EngineCheckStatus,
      before: Int = 20,
      after: Int = 20
  ): EngineCheck =
    EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-5 FEN: $fen -> $error"), identity)

  private def orderingProof: Proof =
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

  private def lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
