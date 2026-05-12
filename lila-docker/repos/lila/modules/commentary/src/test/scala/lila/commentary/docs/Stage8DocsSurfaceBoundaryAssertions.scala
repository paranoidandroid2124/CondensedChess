package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage8DocsSurfaceBoundaryAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val DetailedAuthorityMarkers =
    Vector(
      "Read-only inventory of current opened proof-backed Story meanings:",
      "Stage-2 generic ownership index table:",
      "Stage-4 duplicate-neighbor matrix:",
      "Stage-5 high-risk closed and internal names:",
      "Stage-6 slice admission preflight checklist:",
      "Completion standard: Stage-7 Runtime Guard Coverage Audit closes when"
    )

  def assertBoundary(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "## Stage-8 Docs Surface Boundary",
      "Stage-8 opens summary-only docs surface mentions only when docs tests require them.",
      "Stage-8 detailed authority owners:",
      "Stage-8 docs surface keeps closed:",
      "Completion standard: Stage-8 Docs Surface Boundary closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-8 docs surface boundary must pin: $marker")

    Vector(
      "- ownership index: `StoryInteractionLaw.md`",
      "- duplicate-neighbor matrix: `StoryInteractionLaw.md`",
      "- closed-name/internal-ingredient guard: `StoryInteractionLaw.md`",
      "- slice admission preflight: `StoryInteractionLaw.md`",
      "- closeout checklist: `StoryInteractionLaw.md`"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-8 owner marker missing: $marker")

    DetailedAuthorityMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-8 detailed marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      DetailedAuthorityMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed ownership law: $marker")
      assert(!doc.contains("| public meaning | proof home | Story label | writer | speech key | allowed claim boundary | duplicate-neighbor no-go |"))
      assert(!doc.contains("| neighborhood | collision surface | owner order | duplicate-neighbor no-go | closed stronger wording |"))

    DetailedAuthorityMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed ownership law: $marker")

    Vector(
      "## Stage-8 Docs Surface Boundary",
      "Stage-8 detailed authority owners:",
      "Stage-8 docs surface keeps closed:",
      "Completion standard: Stage-8 Docs Surface Boundary closes when"
    ).foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-8 marker must have one owner: $marker")

    Vector(
      "- AGENTS.md unchanged unless durable operator rules change",
      "- no public route `200`",
      "- no production API",
      "- no public/user-facing LLM narration"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-8 closed marker missing: $marker")
