package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage6SliceAdmissionPreflightAssertions:

  private val ChecklistItems =
    Vector(
      "the meaning is not already owned",
      "proof home is unique",
      "Story label is unique",
      "writer is unique",
      "speech key is unique",
      "duplicate-neighbor no-go list is explicit",
      "same-board legal replay exists",
      "complete StoryProof exists",
      "EngineCheck is not Refute",
      "Support/Context/Blocked/capped/refuted rows stay silent",
      "downstream speech is bounded by selected uncapped Lead Verdict only"
    )

  def assertPreflight(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val summaryDocs =
      Vector(
        "README.md",
        "ChessCommentarySSOT.md",
        "ChessModelArchitecture.md",
        "ChessModelContract.md",
        "LegacyPruneManifest.md"
      )

    Vector(
      "## Stage-6 Slice Admission Preflight Contract",
      "Stage-6 opens a generic preflight checklist for every subsequent positive slice.",
      "Stage-6 slice admission preflight checklist:",
      "Stage-6 preflight authority:",
      "Stage-6 slice admission preflight keeps closed:",
      "Completion standard: Stage-6 Slice Admission Preflight Contract closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-6 preflight contract must pin: $marker")

    ChecklistItems.foreach: item =>
      assert(interactionLaw.contains(item), s"Stage-6 preflight checklist must require: $item")

    Vector(
      "- no specific new slice",
      "- no Tempo positive scope",
      "- no PawnFork positive scope",
      "- no public route `200`",
      "- no production API",
      "- no public/user-facing LLM narration"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-6 preflight keeps closed marker missing: $marker")

    val forbiddenWording =
      Vector(
        "passing preflight opens a " + "Story",
        "preflight replaces proof " + "sidecar",
        "preflight replaces writer " + "admission"
      )
    (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        forbiddenWording.foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-6 wording: $phrase")

    val detailedMarkers =
      Vector(
        "## Stage-6 Slice Admission Preflight Contract",
        "Stage-6 slice admission preflight checklist:",
        "Stage-6 preflight authority:",
        "Stage-6 slice admission preflight keeps closed:",
        "Completion standard: Stage-6 Slice Admission Preflight Contract closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-6 marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-6 law: $marker")

    assert(!agents.contains("## Stage-6 Slice Admission Preflight Contract"))
    assert(!agents.contains("Stage-6 slice admission preflight checklist:"))
