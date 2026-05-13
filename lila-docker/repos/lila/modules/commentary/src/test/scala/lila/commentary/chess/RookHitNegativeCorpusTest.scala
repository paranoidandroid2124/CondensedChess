package lila.commentary.chess

class RookHitNegativeCorpusTest extends munit.FunSuite:

  test("RookHit neighbor fixtures stay in their owner or stay silent"):
    neighborRows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head

      assertEquals(verdict.story.writer, row.writer, row.label)
      assertEquals(verdict.story.scene, row.scene, row.label)
      assertEquals(verdict.story.tactic, row.tactic, row.label)
      assert(verdict.story.tactic.forall(_ != Tactic.RookHit), row.label)
      assertEquals(verdict.story.rookHitProof, None, row.label)
      assertEquals(
        TacticRookHit.write(row.facts, Some(row.route), Some(row.target)),
        None,
        s"${row.label} must not create RookHit"
      )
      assertNoRookHitClaim(verdict, row.label)

  test("RookHit Story does not imply loose hanging material or neighboring tactic homes"):
    val story = rookHitStory

    assertEquals(story.tactic, Some(Tactic.RookHit))
    assertEquals(story.writer, Some(StoryWriter.TacticRookHit))
    assertEquals(story.rookHitProof.exists(_.complete), true)
    assertEquals(story.queenHitProof, None)
    assertEquals(story.loosePieceProof, None)
    assertEquals(story.trapProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.overloadProof, None)
    assertEquals(story.deflectProof, None)
    assertEquals(story.decoyProof, None)
    assertEquals(story.interferenceProof, None)
    assertEquals(SceneMaterial.write(rookHitFacts, rookHitMove), None)
    assertEquals(TacticHanging.write(rookHitFacts, rookHitMove), None)
    assertRookHitLeadText(StoryTable.choose(Vector(story)).head, "RookHit lead")

  test("RookHit writer rejects borrowed neighbor proof homes"):
    contaminationRows.foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head

      assertEquals(contaminated.writer, Some(StoryWriter.TacticRookHit), label)
      assertEquals(TacticRookHit.validStory(contaminated), false, label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertNoRookHitText(verdict, label)

  test("RookHitProof does not lend meaning to neighbor rows"):
    val proof = rookHitStory.rookHitProof

    rookHitProofContaminationRows(proof).foreach: (label, contaminated) =>
      val verdict = StoryTable.choose(Vector(contaminated)).head

      assertNotEquals(verdict.story.writer, Some(StoryWriter.TacticRookHit), label)
      assertEquals(verdict.role, Role.Blocked, label)
      assertNoRookHitText(verdict, label)

  test("RookHit Lead renders only attacks_rook while support context blocked capped refuted and EngineCheck-only rows stay silent"):
    val lead = StoryTable.choose(Vector(rookHitStory)).head
    val support =
      StoryTable
        .choose(Vector(rookHitStory, rookHitStory.copy(proof = supportProof)))
        .find(_.role == Role.Support)
        .getOrElse(fail("expected a non-lead RookHit support row"))
    val context = StoryTable.choose(Vector(rookHitStory.copy(proof = lowProof))).head
    val blocked = StoryTable.choose(Vector(rookHitStory.copy(target = Some(Square('h', 4))))).head
    val capped =
      StoryTable.choose(Vector(TacticRookHit.withEngineCheck(rookHitStory, rookHitCheck(EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(rookHitStory.copy(engineCheck = Some(rookHitCheck(EngineCheckStatus.Refutes))))).head
    val engineOnly =
      StoryTable
        .choose(
          Vector(
            rookHitStory.copy(
              writer = None,
              rookHitProof = None,
              engineCheck = Some(rookHitCheck(EngineCheckStatus.Supports))
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

    assertRookHitLeadText(lead, "Lead")
    Vector(support, context, blocked, capped, refuted, engineOnly).foreach: verdict =>
      assertNoRookHitText(verdict, verdict.toString)

  private final case class NeighborRow(
      label: String,
      facts: BoardFacts,
      route: Line,
      target: Square,
      story: Story,
      scene: Scene,
      tactic: Option[Tactic],
      writer: Option[StoryWriter]
  )

  private def neighborRows: Vector[NeighborRow] =
    Vector(
      NeighborRow(
        "QueenHit owns queen attack",
        queenHitFacts,
        queenHitMove,
        Square('h', 5),
        TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get,
        Scene.Tactic,
        Some(Tactic.QueenHit),
        Some(StoryWriter.TacticQueenHit)
      ),
      NeighborRow(
        "Loose owns undefended attacked piece",
        looseFacts,
        looseMove,
        Square('h', 5),
        TacticLoose.write(looseFacts, Some(looseMove)).get,
        Scene.Tactic,
        Some(Tactic.Loose),
        Some(StoryWriter.TacticLoose)
      ),
      NeighborRow(
        "Hanging owns material result tactic",
        materialFacts,
        materialMove,
        Square('e', 5),
        TacticHanging.write(materialFacts, materialMove).get,
        Scene.Tactic,
        Some(Tactic.Hanging),
        Some(StoryWriter.TacticHanging)
      ),
      NeighborRow(
        "Material owns material result scene",
        materialFacts,
        materialMove,
        Square('e', 5),
        SceneMaterial.write(materialFacts, materialMove).get,
        Scene.Material,
        None,
        Some(StoryWriter.SceneMaterial)
      ),
      NeighborRow(
        "Fork owns two attacked targets",
        forkFacts,
        forkMove,
        Square('d', 6),
        TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get,
        Scene.Tactic,
        Some(Tactic.Fork),
        Some(StoryWriter.TacticFork)
      ),
      NeighborRow(
        "Skewer owns front and rear targets",
        skewerFacts,
        skewerMove,
        Square('e', 5),
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get,
        Scene.Tactic,
        Some(Tactic.Skewer),
        Some(StoryWriter.TacticSkewer)
      ),
      NeighborRow(
        "Pin owns pinned line geometry",
        pinFacts,
        pinMove,
        Square('e', 2),
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get,
        Scene.Tactic,
        Some(Tactic.Pin),
        Some(StoryWriter.TacticPin)
      ),
      NeighborRow(
        "DiscoveredAttack owns revealed non-rook target",
        discoveredFacts,
        discoveredMove,
        Square('g', 6),
        TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get,
        Scene.Tactic,
        Some(Tactic.DiscoveredAttack),
        Some(StoryWriter.TacticDiscoveredAttack)
      ),
      NeighborRow(
        "Trap owns no safe escape target",
        trapFacts,
        trapMove,
        Square('a', 8),
        TacticTrap.write(trapFacts, Some(trapMove)).get,
        Scene.Tactic,
        Some(Tactic.Trap),
        Some(StoryWriter.TacticTrap)
      ),
      NeighborRow(
        "Interference owns line block",
        interferenceFacts,
        interferenceMove,
        interferenceTarget,
        interferenceStory,
        Scene.Tactic,
        Some(Tactic.Interference),
        Some(StoryWriter.TacticInterference)
      ),
      NeighborRow(
        "Deflect owns defender-left-duty evidence",
        deflectFacts,
        deflectMove,
        Square('d', 5),
        TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get,
        Scene.Tactic,
        Some(Tactic.Deflect),
        Some(StoryWriter.TacticDeflect)
      ),
      NeighborRow(
        "Decoy owns named piece arrival square",
        decoyFacts,
        decoyMove,
        Square('a', 8),
        TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), Some(Square('b', 6)), Some(Square('a', 8)), Some(decoyTrapProof)).get,
        Scene.Tactic,
        Some(Tactic.Decoy),
        Some(StoryWriter.TacticDecoy)
      ),
      NeighborRow(
        "RemoveGuard owns removed defender",
        removeGuardFacts,
        removeGuardMove,
        Square('e', 5),
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get,
        Scene.Tactic,
        Some(Tactic.RemoveGuard),
        Some(StoryWriter.TacticRemoveGuard)
      ),
      NeighborRow(
        "Overload owns dual duty failure",
        overloadFacts,
        overloadMove,
        Square('e', 7),
        overloadStory,
        Scene.Tactic,
        Some(Tactic.Overload),
        Some(StoryWriter.TacticOverload)
      )
    )

  private def contaminationRows: Vector[(String, Story)] =
    val story = rookHitStory
    Vector(
      "QueenHit proof" -> story.copy(queenHitProof = TacticQueenHit.write(queenHitFacts, Some(queenHitMove)).get.queenHitProof),
      "Loose proof" -> story.copy(loosePieceProof = TacticLoose.write(looseFacts, Some(looseMove)).get.loosePieceProof),
      "Hanging proof" -> story.copy(captureResult = TacticHanging.write(materialFacts, materialMove).get.captureResult),
      "Material proof" -> story.copy(captureResult = SceneMaterial.write(materialFacts, materialMove).get.captureResult),
      "Fork proof" -> story.copy(multiTargetProof =
        TacticFork.write(forkFacts, Some(forkMove), Some(Square('d', 6)), Some(Square('f', 6))).get.multiTargetProof
      ),
      "Skewer proof" -> story.copy(skewerProof =
        TacticSkewer.write(skewerFacts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8))).get.skewerProof
      ),
      "Pin proof" -> story.copy(pinProof =
        TacticPin.write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1))).get.pinProof
      ),
      "DiscoveredAttack proof" -> story.copy(lineProof =
        TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).get.lineProof
      ),
      "Trap proof" -> story.copy(trapProof = TacticTrap.write(trapFacts, Some(trapMove)).get.trapProof),
      "Interference proof" -> story.copy(interferenceProof = interferenceStory.interferenceProof),
      "Deflect proof" -> story.copy(deflectProof =
        TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get.deflectProof
      ),
      "Decoy proof" -> story.copy(decoyProof =
        TacticDecoy.write(decoyFacts, Some(decoyMove), Some(decoyReply), Some(Square('b', 6)), Some(Square('a', 8)), Some(decoyTrapProof)).get.decoyProof
      ),
      "RemoveGuard proof" -> story.copy(removeGuardProof =
        TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get.removeGuardProof
      ),
      "Overload proof" -> story.copy(overloadProof = overloadStory.overloadProof)
    )

  private def rookHitProofContaminationRows(proof: Option[RookHitProof]): Vector[(String, Story)] =
    neighborRows.map(row => row.label -> row.story.copy(rookHitProof = proof))

  private def assertNoRookHitText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan.exists(_.tactic.contains(Tactic.RookHit)), false, label)
    assertEquals(plan.flatMap(_.allowedClaim).exists(_.key == "attacks_rook"), false, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def assertRookHitLeadText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    val forbiddenPhrases = Vector(
      "wins the rook",
      "wins material",
      "wins exchange",
      "hanging",
      "loose",
      "trapped",
      "forced",
      "only",
      "best",
      "high-value piece",
      "major piece",
      "queen"
    )

    assertEquals(plan.flatMap(_.allowedClaim).map(_.key), Some("attacks_rook"), label)
    assertEquals(rendered.map(_.claimKey), Some("attacks_rook"), label)
    assertEquals(rendered.map(_.text), Some("Rh2 attacks the rook on h5."), label)
    forbiddenPhrases.foreach: phrase =>
      assertEquals(rendered.exists(_.text.toLowerCase.contains(phrase)), false, s"$label used forbidden wording: $phrase")

  private def assertNoRookHitClaim(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
    assertEquals(plan.exists(_.tactic.contains(Tactic.RookHit)), false, label)
    assertEquals(plan.flatMap(_.allowedClaim).exists(_.key == "attacks_rook"), false, label)
    assertNotEquals(rendered.map(_.claimKey), Some("attacks_rook"), label)
    assertEquals(rendered.exists(_.text.toLowerCase.contains("attacks the rook")), false, label)

  private def rookHitStory: Story =
    TacticRookHit.write(rookHitFacts, Some(rookHitMove), Some(rookHitTarget)).get

  private def interferenceStory: Story =
    TacticInterference
      .write(
        interferenceFacts,
        Some(interferenceMove),
        Some(interferenceDefender),
        Some(interferenceBlockingSquare),
        Some(interferenceTarget)
      )
      .get

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

  private def rookHitCheck(status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = rookHitFacts,
      story = Some(rookHitStory),
      engineLine = Some(EngineLine(Vector(rookHitMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 200 else 0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid RookHit negative FEN: $fen -> $error"), identity)

  private val rookHitFacts = facts("4k3/8/8/7r/8/8/3R4/4K3 w - - 0 1")
  private val rookHitMove = Line(Square('d', 2), Square('h', 2))
  private val rookHitTarget = Square('h', 5)
  private val queenHitFacts = facts("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
  private val queenHitMove = Line(Square('d', 2), Square('h', 2))
  private val looseFacts = facts("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFacts = facts("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
  private val materialMove = Line(Square('d', 4), Square('e', 5))
  private val forkFacts = facts("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1")
  private val forkMove = Line(Square('e', 4), Square('e', 5))
  private val skewerFacts = facts("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
  private val skewerMove = Line(Square('a', 1), Square('e', 1))
  private val pinFacts = facts("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1")
  private val pinMove = Line(Square('a', 8), Square('e', 8))
  private val discoveredFacts = facts("7k/8/6b1/8/8/3N4/8/1B5K w - - 0 1")
  private val discoveredMove = Line(Square('d', 3), Square('f', 4))
  private val trapFacts = facts("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))
  private val interferenceFacts = facts("r6k/8/8/8/8/1B6/8/n6K w - - 0 1")
  private val interferenceMove = Line(Square('b', 3), Square('a', 4))
  private val interferenceDefender = Square('a', 8)
  private val interferenceBlockingSquare = Square('a', 4)
  private val interferenceTarget = Square('a', 1)
  private val deflectFacts = facts("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val decoyFacts = facts("4k3/8/1nb5/8/8/8/8/R3B1K1 w - - 0 1")
  private val decoyMove = Line(Square('e', 1), Square('f', 2))
  private val decoyReply = Line(Square('b', 6), Square('a', 8))
  private def decoyTrapProof: TrapProof = TrapProof.fromBoardFacts(trapFacts, trapMove)
  private val removeGuardFacts = facts("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = facts("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))

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
