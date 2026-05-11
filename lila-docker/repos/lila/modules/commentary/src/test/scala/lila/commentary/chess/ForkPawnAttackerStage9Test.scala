package lila.commentary.chess

import java.nio.file.Files
import java.nio.file.Paths

class ForkPawnAttackerStage9Test extends munit.FunSuite:

  private val forkFen = "7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1"
  private val forkFacts = facts(forkFen)
  private val forkRoute = Line(Square('e', 4), Square('e', 5))
  private val targetA = Square('d', 6)
  private val targetB = Square('f', 6)

  test("Stage-9 closeout keeps pawn-attacker admission in existing Fork authority chain"):
    val story = forkStory
    val proof = story.multiTargetProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.writer, Some(StoryWriter.TacticFork))
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    assertEquals(rendered.claimKey, "forks_two_targets")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

  test("Stage-9 closeout keeps pawn-attacker Fork out of sibling public meanings"):
    val story = forkStory
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val publicText = Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get).mkString("\n").toLowerCase

    assertEquals(story.scene == Scene.PawnAdvance, false)
    assertEquals(story.scene == Scene.PawnCapture, false)
    assertEquals(story.scene == Scene.PromotionThreat, false)
    assertEquals(story.scene == Scene.Promotion, false)
    assertEquals(story.scene == Scene.Material, false)
    assertEquals(story.tactic.contains(Tactic.QueenHit), false)
    assertEquals(story.tactic.contains(Tactic.Loose), false)
    assertEquals(story.tactic.contains(Tactic.PawnFork), false)

    Vector(
      "pawn fork",
      "pawn advance",
      "pawn capture",
      "promotion threat",
      "promotes",
      "promotion",
      "material gain",
      "wins material",
      "wins piece",
      "wins a piece",
      "tempo",
      "best move",
      "only move",
      "forced",
      "decisive",
      "winning",
      "engine line"
    ).foreach: forbidden =>
      assert(!publicText.contains(forbidden), s"Stage-9 closeout leaked forbidden wording: $forbidden")

  test("Stage-9 closeout keeps PawnFork names files and speech key closed"):
    val storySource = read("modules/commentary/src/main/scala/lila/commentary/chess/Story.scala")
    val mainSources = Files
      .walk(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess"))
      .filter(path => path.toString.endsWith(".scala"))
      .toList
    val mainText = mainSources.toArray.toVector.map(path => read(path.toString)).mkString("\n")

    assert(storySource.contains("case PawnFork"), "Tactic.PawnFork may remain an enum tombstone")
    assert(!storySource.contains("case TacticPawnFork"), "TacticPawnFork writer must not exist")
    assert(!Files.exists(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/PawnForkProof.scala")))
    assert(!Files.exists(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess/TacticPawnFork.scala")))
    assert(!mainText.contains("pawn_forks_two_targets"))
    assert(!mainText.contains("TacticPawnFork"))
    assert(!mainText.contains("PawnForkProof"))

  test("Stage-9 closeout authority lives in StoryInteractionLaw"):
    val law = read("modules/commentary/docs/StoryInteractionLaw.md")

    assert(law.contains("## Stage-9 Fork-PawnAttacker Admission Closeout"))
    assert(law.contains("no new chess meaning beyond existing `Tactic.Fork`"))
    assert(law.contains("close only pawn attacker admission into existing Fork"))
    assert(law.contains("Authority audit:"))
    assert(law.contains("- `MultiTargetProof` remains proof home."))
    assert(law.contains("- `Tactic.Fork` remains Story label."))
    assert(law.contains("- `TacticFork` remains writer."))
    assert(law.contains("- existing Fork claim key remains speech key."))
    assert(law.contains("- `Tactic.PawnFork` remains closed."))
    assert(law.contains("- `pawn_forks_two_targets` does not exist."))
    assert(law.contains("Duplication audit:"))
    assert(law.contains("- pawn-attacker Fork is not PawnAdvance."))
    assert(law.contains("- pawn-attacker Fork is not Tempo."))
    assert(law.contains("Still closed:"))
    assert(law.contains("- public `Tactic.PawnFork`"))
    assert(law.contains("- public route `200`"))
    assert(law.contains("- public/user-facing LLM narration"))
    assert(law.contains("Stage-9 verification:"))

  private def forkStory: Story =
    TacticFork.write(forkFacts, Some(forkRoute), Some(targetA), Some(targetB)).get

  private def facts(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).fold(error => fail(s"invalid Stage-9 FEN: $fen -> $error"), identity)

  private def read(path: String): String =
    Files.readString(Paths.get(path))
