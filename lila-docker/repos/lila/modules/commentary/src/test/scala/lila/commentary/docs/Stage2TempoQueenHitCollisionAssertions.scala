package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage2TempoQueenHitCollisionAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val ForbiddenQueenTargetWording =
    Vector(
      "gains tempo",
      "wins tempo",
      "with tempo",
      "queen must move",
      "forces the queen",
      "drives the queen",
      "gains time by attacking the queen",
      "seizes initiative"
    )

  def assertCollisionRule(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "## Stage-2 Tempo QueenHit Collision Rule",
      "Stage-2 opens a Tempo vs QueenHit collision rule in `StoryInteractionLaw.md`.",
      "Stage-2 Tempo QueenHit ownership rule:",
      "Stage-2 Tempo QueenHit forbidden wording:",
      "Stage-2 Tempo QueenHit keeps closed:",
      "Completion standard: Stage-2 Tempo QueenHit Collision Rule closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-2 Tempo QueenHit rule must pin: $marker")

    Vector(
      "If a Tempo trace's target or anchor is the rival queen and the proof reason is `queen is attacked`, ownership stays with QueenHit.",
      "QueenHit may say only `attacks_queen` through its existing selected uncapped Lead path.",
      "Tempo must not duplicate QueenHit by renaming queen attack as tempo."
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-2 Tempo QueenHit ownership marker missing: $marker")

    ForbiddenQueenTargetWording.foreach: phrase =>
      assert(interactionLaw.contains(s"- $phrase"), s"Stage-2 Tempo QueenHit forbidden wording missing: $phrase")

    Vector(
      "- no QueenHit wording expansion",
      "- no Tempo Story",
      "- no tempo speech key",
      "- no renderer or LLM public text for Tempo"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-2 Tempo QueenHit closed marker missing: $marker")

    val detailedMarkers =
      Vector(
        "## Stage-2 Tempo QueenHit Collision Rule",
        "Stage-2 Tempo QueenHit ownership rule:",
        "Stage-2 Tempo QueenHit forbidden wording:",
        "Completion standard: Stage-2 Tempo QueenHit Collision Rule closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-2 Tempo QueenHit marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-2 Tempo QueenHit law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Stage-2 Tempo QueenHit law: $marker")
