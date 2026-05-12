package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage9CloseoutReadinessAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  def assertCloseout(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "## Stage-9 Closeout / Next-Slice Readiness Decision",
      "Stage-9 opens the final ownership-index closeout section in `StoryInteractionLaw.md`.",
      "Stage-9 closeout decision options:",
      "Stage-9 selected closeout decision: A.",
      "Stage-9 closed preflight recommendation:",
      "Stage-9 closeout keeps closed:",
      "Completion standard: Stage-9 Closeout / Next-Slice Readiness Decision closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-9 closeout must pin: $marker")

    Vector(
      "A. ownership index completed with no runtime leaks",
      "B. duplicate leak found and fixed without opening meaning",
      "C. ownership ambiguity remains and blocks the next positive slice"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-9 decision option missing: $marker")

    Vector(
      "- next work item: closed preflight audit for the next proposed positive slice only",
      "- no implementation work is opened by this recommendation",
      "- no positive Tempo slice",
      "- no positive PawnFork slice",
      "- no renderer/LLM/public API expansion",
      "- no runtime registry"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-9 closed recommendation or no-go marker missing: $marker")

    Vector(
      "next slice is " + "approved",
      "index opens " + "Tempo",
      "index opens " + "PawnFork",
      "index opens " + "Plan",
      "index opens " + "Source",
      "index opens " + "Strategy",
      "index opens " + "public narration",
      "duplicate risk is solved " + "globally"
    ).foreach: phrase =>
      (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
        (name, doc) =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-9 wording: $phrase")

    val detailedMarkers =
      Vector(
        "## Stage-9 Closeout / Next-Slice Readiness Decision",
        "Stage-9 closeout decision options:",
        "Stage-9 selected closeout decision: A.",
        "Stage-9 closed preflight recommendation:",
        "Completion standard: Stage-9 Closeout / Next-Slice Readiness Decision closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-9 marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-9 law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Stage-9 law: $marker")
