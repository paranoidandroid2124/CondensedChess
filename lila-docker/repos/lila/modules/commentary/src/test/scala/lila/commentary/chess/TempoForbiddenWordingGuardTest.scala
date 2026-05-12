package lila.commentary.chess

class TempoForbiddenWordingGuardTest extends munit.FunSuite:

  test("Stage-4 Tempo forbidden wording guard rejects leakage across neighboring owners"):
    boundedRows.foreach: row =>
      assertEquals(LlmNarrationSmoke.mockNarrate(row.plan, row.rendered), Some(row.rendered.text), row.label)
      assertEquals(
        LlmNarrationSmoke.check(row.plan, row.rendered, row.rendered.text),
        NarrationSmokeCheck(true, Vector.empty),
        row.label
      )

      Stage4ForbiddenOutputs.foreach: output =>
        val result = LlmNarrationSmoke.check(row.plan, row.rendered, s"${row.rendered.text} $output.")
        assertEquals(result.accepted, false, s"${row.label} accepted forbidden output: $output")
        assert(
          result.violations.contains("forbidden_wording") ||
            result.violations.contains("new_tactic_or_plan") ||
            result.violations.contains("stronger_claim"),
          s"${row.label} must reject '$output' as forbidden boundary text: $result"
        )

  private final case class SmokeRow(label: String, plan: ExplanationPlan, rendered: RenderedLine)

  private val Stage4ForbiddenOutputs =
    Vector(
      "tempo",
      "gains tempo",
      "wins tempo",
      "with tempo",
      "gains time",
      "free move",
      "queen must move",
      "forces the queen",
      "forced response",
      "initiative",
      "pressure",
      "keeps the move",
      "keeps control"
    )

  private def boundedRows: Vector[SmokeRow] =
    Vector(
      "QueenHit" -> queenHitStory,
      "Loose" -> looseStory,
      "Hanging" -> hangingStory,
      "Material" -> materialStory,
      "Fork" -> forkStory,
      "Skewer" -> skewerStory,
      "Pin" -> pinStory,
      "RemoveGuard" -> removeGuardStory,
      "Defense" -> defenseStory,
      "Overload" -> overloadStory
    ).map: (label, story) =>
      val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = orderingProof))).head).get
      SmokeRow(label, plan, DeterministicRenderer.fromPlan(plan).get)

  private def queenHitStory: Story =
    val facts = board("4k3/8/8/7q/8/8/3R4/4K3 w - - 0 1")
    TacticQueenHit.write(facts, Some(Line(Square('d', 2), Square('h', 2)))).get

  private def looseStory: Story =
    val facts = board("4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1")
    TacticLoose.write(facts, Some(Line(Square('d', 2), Square('h', 2)))).get

  private def hangingStory: Story =
    val facts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get

  private def materialStory: Story =
    val facts = board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1")
    SceneMaterial.write(facts, Line(Square('d', 4), Square('e', 5))).get

  private def forkStory: Story =
    val facts = board("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1")
    TacticFork
      .write(facts, Some(Line(Square('f', 3), Square('d', 4))), Some(Square('b', 5)), Some(Square('f', 5)))
      .get

  private def skewerStory: Story =
    val facts = board("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1")
    TacticSkewer
      .write(
        facts,
        Some(Line(Square('a', 1), Square('e', 1))),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      )
      .get

  private def pinStory: Story =
    val facts = board("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1")
    TacticPin
      .write(
        facts,
        Some(Line(Square('d', 3), Square('f', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      )
      .get

  private def removeGuardStory: Story =
    val facts = board("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1")
    TacticRemoveGuard
      .write(facts, Some(Line(Square('d', 3), Square('e', 5))), Some(Square('g', 6)), Some(Square('e', 5)))
      .get

  private def defenseStory: Story =
    val facts = board("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1")
    SceneDefense
      .write(facts, Line(Square('f', 5), Square('d', 4)), Line(Square('d', 4), Square('e', 4)))
      .get

  private def overloadStory: Story =
    val facts = board("7k/4q3/b3r3/8/8/6B1/8/7K w - - 0 1")
    TacticOverload
      .write(
        facts,
        Some(Line(Square('g', 3), Square('d', 6))),
        Some(Square('e', 6)),
        Some(Square('e', 7)),
        Some(Square('a', 6)),
        Some(Square('e', 7))
      )
      .get

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-4 FEN: $fen -> $error"), identity)

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
