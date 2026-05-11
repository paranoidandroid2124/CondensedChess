package lila.commentary.chess

class ForkPawnAttackerStage5Test extends munit.FunSuite:

  private val pawnForkFacts = facts("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val pawnForkRoute = Line(Square('e', 4), Square('e', 5))
  private val pawnForkTargetA = Square('d', 6)
  private val pawnForkTargetB = Square('f', 6)

  test("Stage-5 StoryTable admits pawn-attacker Fork only as existing Tactic.Fork"):
    val fork = pawnFork.copy(proof = orderingProof)
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.story.scene, Scene.Tactic)
    assertEquals(verdict.story.tactic, Some(Tactic.Fork))
    assertEquals(verdict.story.tactic.contains(Tactic.PawnFork), false)
    assertEquals(verdict.story.writer, Some(StoryWriter.TacticFork))
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))

  test("Stage-5 StoryTable never emits PawnFork rows"):
    val forgedPawnFork = pawnFork.copy(tactic = Some(Tactic.PawnFork), writer = None, proof = orderingProof)
    val verdicts = StoryTable.choose(Vector(forgedPawnFork, pawnFork.copy(proof = orderingProof)))

    assertEquals(StoryTable.choose(Vector(forgedPawnFork)), Vector.empty)
    assertEquals(verdicts.exists(_.story.tactic.contains(Tactic.PawnFork)), false)
    assertEquals(verdicts.exists(_.story.writer.exists(_.toString.contains("TacticPawnFork"))), false)

  test("Stage-5 collision targets keep their own Story identity"):
    collisionRows.foreach: row =>
      val fork = pawnFork.copy(proof = orderingProof)
      val claim = row.claim.copy(proof = orderingProof)
      val verdicts = StoryTable.choose(Vector(fork, claim))
      val forkVerdict = verdicts.find(_.story.writer.contains(StoryWriter.TacticFork)).get
      val claimVerdict = verdicts.find(_.story.writer == row.claim.writer).get

      assertEquals(forkVerdict.story.tactic, Some(Tactic.Fork), row.label)
      assertEquals(forkVerdict.story.tactic.contains(Tactic.PawnFork), false, row.label)
      assertEquals(claimVerdict.story.scene, row.scene, row.label)
      assertEquals(claimVerdict.story.tactic, row.tactic, row.label)
      assertEquals(claimVerdict.story.writer, row.claim.writer, row.label)
      if claimVerdict.role != Role.Lead then assertNoSpeech(claimVerdict, row.label)

  test("Stage-5 queen-hit-only and loose-only rows stay out of Fork"):
    val queenHitFacts = facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    val queenHitRoute = Line(Square('d', 2), Square('h', 2))
    val looseFacts = facts("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitRoute)).get
    val loose = TacticLoose.write(looseFacts, Some(queenHitRoute)).get

    assertEquals(TacticFork.write(queenHitFacts, Some(queenHitRoute), Some(Square('h', 5)), Some(Square('a', 5))), None)
    assertEquals(TacticFork.write(looseFacts, Some(queenHitRoute), Some(Square('h', 5)), Some(Square('a', 5))), None)
    assertEquals(StoryTable.choose(Vector(queenHit)).head.story.tactic, Some(Tactic.QueenHit))
    assertEquals(StoryTable.choose(Vector(loose)).head.story.tactic, Some(Tactic.Loose))

  test("Stage-5 Support Context Blocked capped refuted and non-Lead Fork rows do not speak"):
    val strongFork = nonPawnFork.copy(proof = orderingProof)
    val pawnSupportCandidate = pawnFork.copy(proof = orderingProof)
    val support = StoryTable
      .choose(Vector(strongFork, pawnSupportCandidate))
      .find(verdict => verdict.story.route == pawnSupportCandidate.route && verdict.story.target == pawnSupportCandidate.target)
      .get
    val context = StoryTable.choose(Vector(pawnFork.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(pawnFork.copy(writer = None, proof = orderingProof))).head
    val capped =
      StoryTable.choose(Vector(TacticFork.withEngineCheck(pawnFork, check(pawnFork, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable
      .choose(Vector(TacticFork.withEngineCheck(pawnFork, check(pawnFork, EngineCheckStatus.Supports, before = 220, after = 20)).get))
      .head

    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    Vector(support, context, blocked, capped, refuted).foreach: verdict =>
      assertNoSpeech(verdict, verdict.toString)

  private final case class CollisionRow(label: String, claim: Story, scene: Scene, tactic: Option[Tactic])

  private def collisionRows: Vector[CollisionRow] =
    val materialFacts = facts("4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1")
    val materialRoute = Line(Square('e', 4), Square('d', 5))
    val promotionFacts = facts("3n1n1k/4P3/8/8/8/8/8/7K w - - 0 1")
    val promotionRoute = Line(Square('e', 7), Square('e', 8))
    val promotionThreatFacts = facts("7k/8/4P3/8/8/8/8/7K w - - 0 1")
    val promotionThreatRoute = Line(Square('e', 6), Square('e', 7))
    val pawnAdvanceFacts = facts("7k/8/8/8/4P3/8/8/7K w - - 0 1")
    val pawnCaptureFacts = facts("7k/8/8/3p4/4P3/8/8/7K w - - 0 1")
    val pawnCaptureRoute = Line(Square('e', 4), Square('d', 5))
    val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    val skewerRoute = Line(Square('a', 1), Square('e', 1))

    Vector(
      CollisionRow("Tactic.Fork", nonPawnFork, Scene.Tactic, Some(Tactic.Fork)),
      CollisionRow("Tactic.QueenHit", TacticQueenHit.write(facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"), Some(Line(Square('d', 2), Square('h', 2)))).get, Scene.Tactic, Some(Tactic.QueenHit)),
      CollisionRow("Tactic.Loose", TacticLoose.write(facts("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"), Some(Line(Square('d', 2), Square('h', 2)))).get, Scene.Tactic, Some(Tactic.Loose)),
      CollisionRow("Tactic.Hanging", TacticHanging.write(materialFacts, materialRoute).get, Scene.Tactic, Some(Tactic.Hanging)),
      CollisionRow("Tactic.Skewer", TacticSkewer.write(skewerFacts, Some(skewerRoute), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get, Scene.Tactic, Some(Tactic.Skewer)),
      CollisionRow("Scene.Material", SceneMaterial.write(materialFacts, materialRoute).get, Scene.Material, None),
      CollisionRow("Scene.PawnAdvance", ScenePawnAdvance.write(pawnAdvanceFacts, pawnForkRoute).get, Scene.PawnAdvance, None),
      CollisionRow("Scene.PawnCapture", ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureRoute).get, Scene.PawnCapture, None),
      CollisionRow("Scene.PromotionThreat", ScenePromotionThreat.write(promotionThreatFacts, promotionThreatRoute).get, Scene.PromotionThreat, None),
      CollisionRow("Scene.Promotion", ScenePromotion.write(promotionFacts, promotionRoute).get, Scene.Promotion, None)
    )

  private def assertNoSpeech(verdict: Verdict, label: String): Unit =
    assertEquals(verdict.leadAllowed && !verdict.engineStrengthLimited && verdict.role == Role.Lead, false, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private def check(story: Story, status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
    EngineCheck.fromStory(
      facts = pawnForkFacts,
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(before)),
      evalAfter = Some(EngineEval(after)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def pawnFork: Story =
    TacticFork.write(pawnForkFacts, Some(pawnForkRoute), Some(pawnForkTargetA), Some(pawnForkTargetB)).get

  private def nonPawnFork: Story =
    val forkFacts = facts("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    val forkMove = Line(Square('f', 3), Square('d', 4))
    TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-5 FEN: $fen -> $error"), identity)

  private val orderingProof: Proof =
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

  private val lowProof: Proof =
    orderingProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
