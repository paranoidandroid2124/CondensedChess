package lila.commentary.chess

class DefenderCloseoutTest extends munit.FunSuite:

  private final case class DefenderOwner(
      label: String,
      proofHome: String,
      tactic: Tactic,
      writer: StoryWriter,
      speechClaim: ExplanationClaim,
      story: Story
  ):
    def storyLabel: String = s"Tactic.$label"
    def speechKey: String = speechClaim.key

  test("RemoveGuard Overload Deflect and Trap each keep one owner tuple"):
    owners.foreach: owner =>
      val verdict = StoryTable.choose(Vector(owner.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).getOrElse(fail(s"missing plan for ${owner.label}"))
      val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail(s"missing rendered line for ${owner.label}"))

      assertEquals(owner.story.scene, Scene.Tactic, owner.label)
      assertEquals(owner.story.tactic, Some(owner.tactic), owner.label)
      assertEquals(owner.story.writer, Some(owner.writer), owner.label)
      assertEquals(plan.tactic, Some(owner.tactic), owner.label)
      assertEquals(plan.allowedClaim, Some(owner.speechClaim), owner.label)
      assertEquals(rendered.claimKey, owner.speechKey, owner.label)
      assertEquals(owner.storyLabel, s"Tactic.${owner.tactic}", owner.label)

    assertEquals(owners.map(_.label).distinct.size, owners.size)
    assertEquals(owners.map(_.proofHome).distinct.size, owners.size)
    assertEquals(owners.map(_.storyLabel).distinct.size, owners.size)
    assertEquals(owners.map(_.writer).distinct.size, owners.size)
    assertEquals(owners.map(_.speechKey).distinct.size, owners.size)

  test("opened defender meanings expose no generic DefenderManipulation runtime owner"):
    val runtimeNames =
      Tactic.values.map(_.toString).toVector ++
        StoryWriter.values.map(_.toString) ++
        ExplanationClaim.values.map(_.toString) ++
        ExplanationClaim.values.map(_.key)

    assertEquals(runtimeNames.exists(_.contains("DefenderManipulation")), false)
    assertEquals(runtimeNames.exists(_.contains("Defender_Manipulation")), false)
    assertEquals(runtimeNames.exists(_.contains("defender_manipulation")), false)

  private def owners: Vector[DefenderOwner] =
    Vector(
      DefenderOwner(
        "RemoveGuard",
        "RemoveGuardProof",
        Tactic.RemoveGuard,
        StoryWriter.TacticRemoveGuard,
        ExplanationClaim.RemovesDefender,
        removeGuardStory
      ),
      DefenderOwner(
        "Overload",
        "OverloadProof",
        Tactic.Overload,
        StoryWriter.TacticOverload,
        ExplanationClaim.OverloadsDefender,
        overloadStory
      ),
      DefenderOwner(
        "Deflect",
        "DeflectProof",
        Tactic.Deflect,
        StoryWriter.TacticDeflect,
        ExplanationClaim.DeflectsDefender,
        deflectStory
      ),
      DefenderOwner(
        "Trap",
        "TrapProof",
        Tactic.Trap,
        StoryWriter.TacticTrap,
        ExplanationClaim.TrapsPiece,
        trapStory
      )
    )

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid defender closeout FEN: $fen -> $error"), identity)

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
