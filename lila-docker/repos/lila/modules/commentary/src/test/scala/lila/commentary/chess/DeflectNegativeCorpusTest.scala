package lila.commentary.chess

class DeflectNegativeCorpusTest extends munit.FunSuite:

  test("Deflect neighbor fixtures stay in their owner or stay silent"):
    neighborRows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head

      assertEquals(verdict.story.writer, row.writer, row.label)
      assertEquals(verdict.story.scene, row.scene, row.label)
      assertEquals(verdict.story.tactic, row.tactic, row.label)
      assert(verdict.story.tactic.forall(_ != Tactic.Deflect), row.label)
      assertEquals(
        TacticDeflect.write(row.facts, Some(row.route), None, None, None),
        None,
        s"${row.label} must not create Deflect"
      )
      assertNoNeighborDeflectText(verdict, row.label)

  test("Deflect rows do not borrow neighbor proof homes or speech"):
    val clean = deflectStory

    contaminationRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head

      assertEquals(verdict.story.writer, Some(StoryWriter.TacticDeflect), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.story.deflectProof, clean.deflectProof, label)
      assertNoDeflectText(verdict, label)

  test("Deflect non-speaking row states and forbidden wording stay closed"):
    val lead = StoryTable.choose(Vector(deflectStory)).head
    val support =
      StoryTable
        .choose(Vector(deflectStory, deflectStory.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected a non-lead Deflect support row"))
    val context = StoryTable.choose(Vector(deflectStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(deflectStory.copy(writer = None))).head
    val capped = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(deflectStory, check(EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticDeflect.withEngineCheck(deflectStory, check(EngineCheckStatus.Refutes)).get)).head
    val engineOnly =
      StoryTable
        .choose(
          Vector(
            deflectStory.copy(
              writer = None,
              deflectProof = None,
              engineCheck = Some(check(EngineCheckStatus.Supports))
            )
          )
        )
        .head

    assertEquals(lead.role, Role.Lead)
    assertEquals(support.role, Role.Support)
    assertEquals(context.role, Role.Context)
    assertEquals(blocked.role, Role.Blocked)
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(engineOnly.role, Role.Blocked)

    Vector(support, context, blocked, capped, refuted, engineOnly).foreach: verdict =>
      assertNoDeflectText(verdict, verdict.toString)

  private final case class NeighborRow(
      label: String,
      facts: BoardFacts,
      route: Line,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: Option[StoryWriter]
  )

  private def neighborRows: Vector[NeighborRow] =
    Vector(
      NeighborRow(
        "RemoveGuard owns removed defender",
        removeGuardFacts,
        removeGuardMove,
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get,
        Scene.Tactic,
        Some(Tactic.RemoveGuard),
        Some(StoryWriter.TacticRemoveGuard)
      ),
      NeighborRow(
        "Overload owns dual duty failure",
        overloadFacts,
        overloadMove,
        overloadStory,
        Scene.Tactic,
        Some(Tactic.Overload),
        Some(StoryWriter.TacticOverload)
      ),
      NeighborRow(
        "Trap owns no safe escape target",
        trapFacts,
        trapMove,
        TacticTrap.write(trapFacts, Some(trapMove)).get,
        Scene.Tactic,
        Some(Tactic.Trap),
        Some(StoryWriter.TacticTrap)
      ),
      NeighborRow(
        "QueenHit owns queen attack",
        queenHitFacts,
        queenHitMove,
        TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get,
        Scene.Tactic,
        Some(Tactic.QueenHit),
        Some(StoryWriter.TacticQueenHit)
      ),
      NeighborRow(
        "Loose owns undefended attacked piece",
        looseFacts,
        looseMove,
        TacticLoose.write(looseFacts, Some(looseMove)).get,
        Scene.Tactic,
        Some(Tactic.Loose),
        Some(StoryWriter.TacticLoose)
      ),
      NeighborRow(
        "Hanging owns material result tactic",
        materialFacts,
        materialMove,
        TacticHanging.write(materialFacts, materialMove).get,
        Scene.Tactic,
        Some(Tactic.Hanging),
        Some(StoryWriter.TacticHanging)
      ),
      NeighborRow(
        "Material owns material result scene",
        materialFacts,
        materialMove,
        SceneMaterial.write(materialFacts, materialMove).get,
        Scene.Material,
        None,
        Some(StoryWriter.SceneMaterial)
      ),
      NeighborRow(
        "Pin owns line geometry",
        pinFacts,
        pinMove,
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get,
        Scene.Tactic,
        Some(Tactic.Pin),
        Some(StoryWriter.TacticPin)
      ),
      NeighborRow(
        "Skewer owns line geometry",
        skewerFacts,
        skewerMove,
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get,
        Scene.Tactic,
        Some(Tactic.Skewer),
        Some(StoryWriter.TacticSkewer)
      ),
      NeighborRow(
        "DiscoveredAttack owns line geometry",
        discoveredFacts,
        discoveredMove,
        TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get,
        Scene.Tactic,
        Some(Tactic.DiscoveredAttack),
        Some(StoryWriter.TacticDiscoveredAttack)
      ),
      NeighborRow(
        "Defense owns defended own target",
        defenseFacts,
        defenseMove,
        SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get,
        Scene.Defense,
        None,
        Some(StoryWriter.SceneDefense)
      )
    )

  private def contaminationRows: Vector[(String, Story)] =
    val story = deflectStory
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val removeGuard =
      TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val trap = TacticTrap.write(trapFacts, Some(trapMove)).get
    val queenHit = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get
    val loose = TacticLoose.write(looseFacts, Some(looseMove)).get
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get
    val skewer = TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get
    val pin = TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get

    Vector(
      "RemoveGuard proof" -> story.copy(removeGuardProof = removeGuard.removeGuardProof),
      "Overload proof" -> story.copy(overloadProof = overloadStory.overloadProof),
      "Trap proof" -> story.copy(trapProof = trap.trapProof),
      "QueenHit proof" -> story.copy(queenHitProof = queenHit.queenHitProof),
      "Loose proof" -> story.copy(loosePieceProof = loose.loosePieceProof),
      "Material proof" -> story.copy(captureResult = material.captureResult),
      "Hanging proof" -> story.copy(captureResult = hanging.captureResult),
      "Fork proof" -> story.copy(multiTargetProof = fork.multiTargetProof),
      "Skewer proof" -> story.copy(skewerProof = skewer.skewerProof),
      "Pin proof" -> story.copy(pinProof = pin.pinProof)
    )

  private def assertNoDeflectText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan, None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertNoNeighborDeflectText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertNotEquals(plan.flatMap(_.allowedClaim), Some(ExplanationClaim.DeflectsDefender), label)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    assertNotEquals(rendered.map(_.claimKey), Some("deflects_defender"), label)
    assertEquals(rendered.exists(_.text.toLowerCase.contains("deflect")), false, label)

  private def deflectStory: Story =
    TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(deflectDefender), Some(deflectTarget)).get

  private def overloadStory: Story =
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

  private def check(status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(deflectMove),
      engineLine = Some(EngineLine(Vector(deflectMove))),
      replyLine = Some(EngineLine(Vector(deflectReply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(if status == EngineCheckStatus.Refutes then EngineEval(-200) else EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    ).copy(storyBound = true)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Deflect negative corpus FEN: $fen -> $error"), identity)

  private val deflectFacts = board("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val deflectDefender = Square('e', 6)
  private val deflectTarget = Square('d', 5)

  private val removeGuardFacts = board("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val queenHitFacts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val looseFacts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFacts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val pinFacts = board("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
  private val skewerFacts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
  private val discoveredFacts = board("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1")
  private val discoveredMove = Line(Square('d', 3), Square('f', 4))
  private val defenseFacts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
  private val defenseThreat = Line(Square('f', 5), Square('d', 4))
  private val defenseMove = Line(Square('d', 4), Square('e', 4))
  private val forkFacts = board("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))

  private val lowProof =
    Proof(
      boardProof = 70,
      lineProof = 70,
      ownerProof = 70,
      anchorProof = 70,
      routeProof = 70,
      persistence = 0,
      immediacy = 0,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 70,
      kingHeat = 0,
      pieceSupport = 0,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 0
    )

  private val supportProof =
    Proof(
      boardProof = 90,
      lineProof = 90,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      persistence = 95,
      immediacy = 85,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 95,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 95
    )
