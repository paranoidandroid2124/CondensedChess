package lila.commentary.chess

class QueenHitStage5Test extends munit.FunSuite:

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-5 StoryTable orders standalone proof-backed QueenHit"):
    val board = facts(queenHitFen)
    val queenHit = TacticQueenHit.write(board, Some(queenHitMove)).get.copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(queenHit)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.story.tactic, Some(Tactic.QueenHit))

  test("Stage-5 StoryTable keeps collision targets in their existing proof homes"):
    collisionRows.foreach: row =>
      val verdicts = StoryTable.choose(Vector(row.claim.copy(proof = orderingProof), row.queenHit.copy(proof = orderingProof)))
      val lead = verdicts.find(_.role == Role.Lead).get
      val queenHitVerdict = verdicts.find(_.story.tactic.contains(Tactic.QueenHit)).get

      assertEquals(lead.story.writer, row.claim.writer, row.label)
      assertEquals(lead.story.scene, row.claim.scene, row.label)
      assertEquals(lead.story.tactic, row.claim.tactic, row.label)
      assertEquals(queenHitVerdict.story.tactic, Some(Tactic.QueenHit), row.label)
      assertEquals(queenHitVerdict.role == Role.Lead, false, row.label)
      assertEquals(ExplanationPlan.fromSelected(queenHitVerdict), None, row.label)

  test("Stage-5 non-Lead capped and refuted QueenHit rows do not speak"):
    val board = facts(queenHitFen)
    val queenHit = TacticQueenHit.write(board, Some(queenHitMove)).get.copy(proof = orderingProof)
    val materialRow = SceneMaterial
      .write(facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"), Line(Square('d', 4), Square('e', 5)))
      .get
      .copy(proof = orderingProof)
    val support = StoryTable.choose(Vector(materialRow, queenHit)).find(_.story.tactic.contains(Tactic.QueenHit)).get
    val context = StoryTable.choose(Vector(queenHit.copy(proof = lowProof))).head
    val capped = StoryTable.choose(Vector(TacticQueenHit.withEngineCheck(queenHit, check(board, queenHit, EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(TacticQueenHit.withEngineCheck(queenHit, check(board, queenHit, EngineCheckStatus.Supports, before = 220, after = 20)).get)).head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, capped, refuted).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)

  private final case class CollisionRow(label: String, claim: Story, queenHit: Story)

  private def collisionRows: Vector[CollisionRow] =
    val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val materialQueenHit = TacticQueenHit.write(facts(queenHitFen), Some(queenHitMove)).get
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get

    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val forkQueenHit = TacticQueenHit.write(forkFacts, Some(forkMove)).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewerQueenHit = TacticQueenHit.write(skewerFacts, Some(skewerMove)).get
    val skewer =
      TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get

    val discoveredFacts = facts("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredPinFacts = facts("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    val discoveredPinMove = Line(Square('d', 3), Square('f', 4))
    val discoveredPinQueenHit = materialQueenHit
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredPinMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pin =
      TacticPin
        .write(
          discoveredPinFacts,
          Some(discoveredPinMove),
          Some(Square('b', 1)),
          Some(Square('g', 6)),
          Some(Square('h', 7))
        )
        .get

    val removeFacts = facts("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    val removeMove = Line(Square('d', 3), Square('e', 5))
    val removeQueenHit = materialQueenHit
    val remove = TacticRemoveGuard.write(removeFacts, Some(removeMove), Some(Square('g', 6)), Some(Square('e', 5))).get

    val defenseFacts = facts("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defenseQueenHit = materialQueenHit
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get

    Vector(
      CollisionRow("Scene.Material", material, materialQueenHit),
      CollisionRow("Tactic.Hanging", hanging, materialQueenHit),
      CollisionRow("Tactic.Fork", fork, forkQueenHit),
      CollisionRow("Tactic.Skewer", skewer, skewerQueenHit),
      CollisionRow("Tactic.Pin", pin, discoveredPinQueenHit),
      CollisionRow("Tactic.RemoveGuard", remove, removeQueenHit),
      CollisionRow("Tactic.DiscoveredAttack", discovered, discoveredPinQueenHit),
      CollisionRow("Scene.Defense", defense, defenseQueenHit)
    )

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

  private def facts(fen: String): BoardFacts =
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
