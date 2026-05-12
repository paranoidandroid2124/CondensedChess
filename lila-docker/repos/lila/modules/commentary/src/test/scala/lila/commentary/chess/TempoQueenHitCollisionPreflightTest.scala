package lila.commentary.chess

class TempoQueenHitCollisionPreflightTest extends munit.FunSuite:

  test("Stage-6 closed Tempo tombstone rows do not enter StoryTable public rows"):
    val closedRow = forgedClosedTempoRow()

    assertEquals(StoryTable.choose(Vector(closedRow)), Vector.empty)

  test("Stage-6 neighboring owners do not become Tempo or emit Tempo text"):
    neighboringRows.foreach: row =>
      assertEquals(row.story.tactic.contains(Tactic.Tempo), false, row.label)
      assertEquals(row.story.writer.exists(_.toString.contains("Tempo")), false, row.label)

      val verdict = StoryTable.choose(Vector(row.story.copy(proof = orderingProof))).head
      assertEquals(verdict.story.tactic.contains(Tactic.Tempo), false, row.label)
      assertEquals(verdict.role, Role.Lead, row.label)

      val rendered = ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan)
      assert(rendered.nonEmpty, row.label)
      assertNoTempoText(rendered.get.text, row.label)
      assertNoTempoText(LlmNarrationSmoke.mockNarrate(ExplanationPlan.fromSelected(verdict).get, rendered.get).get, row.label)

  test("Stage-6 EngineCheck statuses do not create standalone Tempo text"):
    val story = queenHitStory.copy(proof = orderingProof)

    Vector(
      EngineCheckStatus.Supports,
      EngineCheckStatus.Caps,
      EngineCheckStatus.Refutes,
      EngineCheckStatus.Unknown
    ).foreach: status =>
      val checked = TacticQueenHit.withEngineCheck(story, engineCheck(story, status)).get
      assertEquals(checked.tactic, Some(Tactic.QueenHit), status.toString)
      assertEquals(checked.tactic.contains(Tactic.Tempo), false, status.toString)

      val verdict = StoryTable.choose(Vector(checked)).head
      assertEquals(verdict.story.tactic.contains(Tactic.Tempo), false, status.toString)
      val rendered = ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan)
      rendered.foreach(line => assertNoTempoText(line.text, status.toString))

  test("Stage-6 non-Lead rows produce no standalone Tempo text"):
    val queenHit = queenHitStory.copy(proof = orderingProof)
    val material = materialStory.copy(proof = orderingProof)
    val verdicts = StoryTable.choose(Vector(queenHit, material))

    assert(verdicts.exists(_.role == Role.Lead))
    verdicts.filter(_.role != Role.Lead).foreach: verdict =>
      assertEquals(verdict.story.tactic.contains(Tactic.Tempo), false)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

    val capped = StoryTable.choose(Vector(TacticQueenHit.withEngineCheck(queenHit, engineCheck(queenHit, EngineCheckStatus.Caps)).get)).head
    val refuted = StoryTable.choose(Vector(TacticQueenHit.withEngineCheck(queenHit, engineCheck(queenHit, EngineCheckStatus.Refutes)).get)).head

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, verdict) =>
      assertEquals(verdict.story.tactic.contains(Tactic.Tempo), false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  private final case class NeighborRow(label: String, story: Story)

  private def neighboringRows: Vector[NeighborRow] =
    Vector(
      "QueenHit" -> queenHitStory,
      "Loose" -> looseStory,
      "Hanging" -> hangingStory,
      "Material" -> materialStory,
      "Fork" -> forkStory,
      "Skewer" -> skewerStory,
      "Defense" -> defenseStory
    ).map(NeighborRow.apply)

  private def forgedClosedTempoRow(): Story =
    Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Tempo),
      proof = orderingProof,
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('h', 5)),
      anchor = Some(Square('h', 2)),
      route = Some(Line(Square('d', 2), Square('h', 2))),
      routeSan = Some("Rh2"),
      storyProof = StoryProof.fromBoardFacts(board(queenHitFen), Line(Square('d', 2), Square('h', 2)))
    )

  private def queenHitStory: Story =
    TacticQueenHit.write(board(queenHitFen), Some(Line(Square('d', 2), Square('h', 2)))).get

  private def looseStory: Story =
    TacticLoose.write(board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"), Some(Line(Square('d', 2), Square('h', 2)))).get

  private def hangingStory: Story =
    TacticHanging.write(board(materialFen), Line(Square('d', 4), Square('e', 5))).get

  private def materialStory: Story =
    SceneMaterial.write(board(materialFen), Line(Square('d', 4), Square('e', 5))).get

  private def forkStory: Story =
    TacticFork
      .write(
        board("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1"),
        Some(Line(Square('f', 3), Square('d', 4))),
        Some(Square('b', 5)),
        Some(Square('f', 5))
      )
      .get

  private def skewerStory: Story =
    TacticSkewer
      .write(
        board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1"),
        Some(Line(Square('a', 1), Square('e', 1))),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      )
      .get

  private def defenseStory: Story =
    SceneDefense
      .write(
        board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1"),
        Line(Square('f', 5), Square('d', 4)),
        Line(Square('d', 4), Square('e', 4))
      )
      .get

  private def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = board(queenHitFen),
      story = Some(story),
      engineLine = Some(EngineLine(Vector(story.route.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('f', 8))))),
      evalBefore = Some(EngineEval(if status == EngineCheckStatus.Refutes then 250 else 20)),
      evalAfter = Some(EngineEval(if status == EngineCheckStatus.Refutes then 20 else 20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = status
    )

  private def assertNoTempoText(text: String, label: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    Vector("tempo", "with tempo", "wins time").foreach: phrase =>
      assert(!normalized.contains(phrase), s"$label emitted forbidden wording: $phrase in '$text'")

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-6 FEN: $fen -> $error"), identity)

  private val queenHitFen = "4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1"
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"

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
