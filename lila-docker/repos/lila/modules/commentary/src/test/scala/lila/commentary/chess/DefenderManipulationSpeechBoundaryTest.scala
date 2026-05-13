package lila.commentary.chess

class DefenderManipulationSpeechBoundaryTest extends munit.FunSuite:

  test("selected Lead defender manipulation rows render only their own claim key"):
    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).getOrElse(fail(s"missing plan for ${row.label}"))
      val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail(s"missing rendered line for ${row.label}"))

      assertEquals(verdict.role, Role.Lead, row.label)
      assertEquals(plan.allowedClaim.map(_.key), Some(row.claimKey), row.label)
      assertEquals(rendered.claimKey, row.claimKey, row.label)
      rows.filterNot(_.claimKey == row.claimKey).foreach: sibling =>
        assertNotEquals(rendered.claimKey, sibling.claimKey, row.label)

  test("LLM smoke rejects sibling defender wording and closed wording"):
    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get

      forbiddenOutputs(row).foreach: (label, text) =>
        val result = LlmNarrationSmoke.check(plan, rendered, text)
        assertEquals(
          result.accepted,
          false,
          s"${row.label} must reject $label: $text"
        )

  private final case class Row(label: String, story: Story, claimKey: String)

  private def rows: Vector[Row] =
    Vector(
      Row("RemoveGuard", removeGuardStory, "removes_defender"),
      Row("Overload", overloadStory, "overloads_defender"),
      Row("Deflect", deflectStory, "deflects_defender"),
      Row("Trap", trapStory, "traps_piece")
    )

  private def forbiddenOutputs(row: Row): Vector[(String, String)] =
    val renderedText =
      DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(StoryTable.choose(Vector(row.story)).head).get).get.text
    val siblingPhrases =
      Vector(
        Option.when(row.claimKey != "removes_defender")("remove defender" -> s"$renderedText This removes the defender."),
        Option.when(row.claimKey != "removes_defender")("removed defender" -> s"$renderedText The defender is removed."),
        Option.when(row.claimKey != "overloads_defender")("overload defender" -> s"$renderedText This overloads the defender."),
        Option.when(row.claimKey != "overloads_defender")("overloaded defender" -> s"$renderedText The defender is overloaded."),
        Option.when(row.claimKey != "deflects_defender")("deflect defender" -> s"$renderedText This deflects the defender."),
        Option.when(row.claimKey != "deflects_defender")("deflected defender" -> s"$renderedText The defender is deflected."),
        Option.when(row.claimKey != "traps_piece")("trap piece" -> s"$renderedText This traps the piece."),
        Option.when(row.claimKey != "traps_piece")("trapped piece" -> s"$renderedText The piece is trapped.")
      ).flatten

    siblingPhrases ++ Vector(
      "decoy" -> s"$renderedText This is a decoy.",
      "lure" -> s"$renderedText This lures the defender.",
      "attract" -> s"$renderedText This attracts the defender.",
      "interference" -> s"$renderedText This is interference.",
      "interferes" -> s"$renderedText This interferes with the defense.",
      "blocks line" -> s"$renderedText This blocks the line.",
      "wins material" -> s"$renderedText This wins material.",
      "wins piece" -> s"$renderedText This wins a piece.",
      "forced" -> s"$renderedText This is forced.",
      "only" -> s"$renderedText This is the only move.",
      "best" -> s"$renderedText This is the best move.",
      "no defense" -> s"$renderedText There is no defense.",
      "no counterplay" -> s"$renderedText There is no counterplay."
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid defender speech boundary FEN: $fen -> $error"), identity)

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
