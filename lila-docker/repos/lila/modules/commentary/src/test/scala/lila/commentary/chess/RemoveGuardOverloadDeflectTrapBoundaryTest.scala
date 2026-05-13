package lila.commentary.chess

class RemoveGuardOverloadDeflectTrapBoundaryTest extends munit.FunSuite:

  test("RemoveGuard Overload Deflect and Trap keep separate proof ownership"):
    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict)
      val rendered = plan.flatMap(DeterministicRenderer.fromPlan)

      assertEquals(verdict.role, Role.Lead, row.label)
      assertEquals(row.story.tactic, Some(row.tactic), row.label)
      assertEquals(row.story.writer, Some(row.writer), row.label)
      assertEquals(plan.flatMap(_.allowedClaim).map(_.key), Some(row.claimKey), row.label)
      assertEquals(rendered.map(_.claimKey), Some(row.claimKey), row.label)

  test("sibling proof sidecars block defender tactic speech"):
    rows.foreach: row =>
      siblingProofContaminations(row).foreach: (label, contaminated) =>
        val verdict = StoryTable.choose(Vector(contaminated)).head

        assertEquals(verdict.role, Role.Blocked, s"${row.label} with $label")
        assertNoText(verdict, s"${row.label} with $label")

  test("non Lead capped and refuted defender tactic rows produce no standalone text"):
    rows.foreach: row =>
      val support =
        StoryTable
          .choose(Vector(row.story, row.story))
          .find(verdict => verdict.story.writer.contains(row.writer) && verdict.role == Role.Support)
          .getOrElse(fail(s"expected Support row for ${row.label}"))
      val context = StoryTable.choose(Vector(row.story.copy(proof = lowProof))).head
      val blockedRows = StoryTable.choose(Vector(row.story.copy(writer = None)))
      val capped = StoryTable.choose(Vector(withEngine(row.story, EngineCheckStatus.Caps))).head
      val refuted = StoryTable.choose(Vector(withEngine(row.story, EngineCheckStatus.Refutes))).head

      blockedRows.foreach: verdict =>
        assertNoText(verdict, s"${row.label} writerless")
      Vector(support, context, capped, refuted).foreach: verdict =>
        assertNoText(verdict, s"${row.label} ${verdict.role}")

  private final case class Row(
      label: String,
      story: Story,
      tactic: Tactic,
      writer: StoryWriter,
      claimKey: String
  )

  private def rows: Vector[Row] =
    Vector(
      Row(
        "RemoveGuard",
        removeGuardStory,
        Tactic.RemoveGuard,
        StoryWriter.TacticRemoveGuard,
        "removes_defender"
      ),
      Row(
        "Overload",
        overloadStory,
        Tactic.Overload,
        StoryWriter.TacticOverload,
        "overloads_defender"
      ),
      Row(
        "Deflect",
        deflectStory,
        Tactic.Deflect,
        StoryWriter.TacticDeflect,
        "deflects_defender"
      ),
      Row(
        "Trap",
        trapStory,
        Tactic.Trap,
        StoryWriter.TacticTrap,
        "traps_piece"
      )
    )

  private def siblingProofContaminations(row: Row): Vector[(String, Story)] =
    Vector(
      Option.when(row.writer != StoryWriter.TacticRemoveGuard)(
        "RemoveGuardProof" -> row.story.copy(removeGuardProof = removeGuardStory.removeGuardProof)
      ),
      Option.when(row.writer != StoryWriter.TacticOverload)(
        "OverloadProof" -> row.story.copy(overloadProof = overloadStory.overloadProof)
      ),
      Option.when(row.writer != StoryWriter.TacticDeflect)(
        "DeflectProof" -> row.story.copy(deflectProof = deflectStory.deflectProof)
      ),
      Option.when(row.writer != StoryWriter.TacticTrap)(
        "TrapProof" -> row.story.copy(trapProof = trapStory.trapProof)
      )
    ).flatten

  private def withEngine(story: Story, status: EngineCheckStatus): Story =
    val check = EngineCheck.fromStory(
      facts = factsFor(story),
      story = Some(story),
      engineLine = story.route.map(route => EngineLine(Vector(route))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Refutes then -200 else 20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )
    story.writer match
      case Some(StoryWriter.TacticRemoveGuard) => TacticRemoveGuard.withEngineCheck(story, check).get
      case Some(StoryWriter.TacticOverload) => TacticOverload.withEngineCheck(story, check).get
      case Some(StoryWriter.TacticDeflect) => TacticDeflect.withEngineCheck(story, check).get
      case Some(StoryWriter.TacticTrap) => TacticTrap.withEngineCheck(story, check).get
      case _ => fail(s"unexpected writer for engine attachment: ${story.writer}")

  private def factsFor(story: Story): BoardFacts =
    story.writer match
      case Some(StoryWriter.TacticRemoveGuard) => removeGuardFacts
      case Some(StoryWriter.TacticOverload) => overloadFacts
      case Some(StoryWriter.TacticDeflect) => deflectFacts
      case Some(StoryWriter.TacticTrap) => trapFacts
      case _ => fail(s"unexpected writer for facts lookup: ${story.writer}")

  private def assertNoText(verdict: Verdict, label: String): Unit =
    val plan = ExplanationPlan.fromSelected(verdict)
    assertEquals(plan, None, label)
    assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid defender tactic boundary FEN: $fen -> $error"), identity)

  private def removeGuardStory: Story =
    TacticRemoveGuard.write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get

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

  private def deflectStory: Story =
    TacticDeflect.write(deflectFacts, Some(deflectMove), Some(deflectReply), Some(Square('e', 6)), Some(Square('d', 5))).get

  private def trapStory: Story =
    TacticTrap.write(trapFacts, Some(trapMove)).get

  private val removeGuardFacts = board("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1")
  private val removeGuardMove = Line(Square('g', 8), Square('c', 4))
  private val overloadFacts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
  private val overloadMove = Line(Square('g', 3), Square('d', 6))
  private val deflectFacts = board("7k/8/4b3/3n4/8/8/7P/7K w - - 0 1")
  private val deflectMove = Line(Square('h', 2), Square('h', 3))
  private val deflectReply = Line(Square('e', 6), Square('g', 4))
  private val trapFacts = board("n3k3/8/2b5/8/8/8/5B2/R5K1 w - - 0 1")
  private val trapMove = Line(Square('a', 1), Square('a', 7))

  private val supportProof =
    Proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      persistence = 99,
      immediacy = 99,
      forcing = 0,
      conversionPrize = 0,
      counterplayRisk = 0,
      kingHeat = 0,
      pieceSupport = 99,
      pawnSupport = 0,
      sourceFit = 0,
      novelty = 0,
      clarity = 99
    )

  private val lowProof =
    supportProof.copy(boardProof = 60, lineProof = 60, ownerProof = 60, anchorProof = 60, routeProof = 60)
