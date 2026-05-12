package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage7TempoDocsSurfaceBoundaryAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val DetailedMarkers =
    Vector(
      "## Stage-6 Tempo Runtime Collision Fixtures",
      "Stage-6 Tempo runtime collision fixture duties:",
      "Stage-6 Tempo runtime collision keeps closed:",
      "Completion standard: Stage-6 Tempo Runtime Collision Fixtures closes when",
      "## Stage-7 Tempo Docs Surface Boundary",
      "Stage-7 Tempo detailed authority owners:",
      "Stage-7 Tempo docs surface keeps closed:",
      "Completion standard: Stage-7 Tempo Docs Surface Boundary closes when",
      "## Stage-8 Tempo Closeout Next Decision",
      "Stage-8 Tempo closeout decision:",
      "Stage-8 Tempo leak record:",
      "Stage-8 Tempo proof no new meaning opened:",
      "Completion standard: Stage-8 Tempo Closeout Next Decision closes when"
    )

  private val DetailedTempoLawMarkers =
    Vector(
      "Stage-1 Tempo trace classification set:",
      "Stage-2 Tempo QueenHit ownership rule:",
      "Stage-3 Tempo candidate ownership matrix:",
      "Stage-4 forbidden wording must be rejected where relevant",
      "Stage-5 Tempo possible uniqueness bind requirements:",
      "Stage-6 Tempo runtime collision fixture duties:",
      "Stage-8 Tempo closeout decision:"
    )

  def assertBoundary(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    DetailedMarkers.foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-7 Tempo docs surface boundary must pin: $marker")

    Vector(
      "- Tempo trace inventory: `StoryInteractionLaw.md`",
      "- Tempo QueenHit collision rule: `StoryInteractionLaw.md`",
      "- Tempo neighbor ownership matrix: `StoryInteractionLaw.md`",
      "- Tempo forbidden wording guard: `StoryInteractionLaw.md`",
      "- Tempo possibly-unique preflight result: `StoryInteractionLaw.md`",
      "- Tempo runtime collision fixture duties: `StoryInteractionLaw.md`",
      "- Tempo closeout decision: `StoryInteractionLaw.md`"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-7 Tempo authority owner marker missing: $marker")

    Vector(
      "- AGENTS.md unchanged unless durable operator rules change",
      "- README/SSOT/Architecture/Contract/Manifest stay summary-only if touched",
      "- no public route `200`",
      "- no production API",
      "- no public/user-facing LLM narration"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-7 Tempo closed marker missing: $marker")

    val decisions =
      Vector(
        "A. Tempo is duplicate-owned by existing meanings; no Tempo public meaning admitted.",
        "B. Tempo has one possibly-unique closed next-slice preflight row; no implementation opened.",
        "C. Boundary leakage found and fixed without admitting Tempo public meaning.",
        "D. Ownership ambiguity remains; Tempo remains blocked."
      )
    assertEquals(
      decisions.count(interactionLaw.contains),
      1,
      "Stage-8 Tempo closeout must record exactly one final decision"
    )
    assert(interactionLaw.contains(decisions(2)), "Stage-8 Tempo closeout must choose decision C")
    Vector(
      "forged `Tactic.Tempo` tombstone row could survive as a ranked blocked row",
      "`StoryTable` filters closed tactic tombstones before ranking",
      "no `Tactic.Tempo` Story",
      "no TempoProof",
      "no TacticTempo writer",
      "no tempo speech key",
      "no ExplanationPlan claim",
      "no renderer text",
      "no LLM smoke acceptance path"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-8 Tempo closeout evidence missing: $marker")

    DetailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-7 Tempo marker must have one owner: $marker")

    (SummaryDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        DetailedMarkers.foreach: marker =>
          assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-7 Tempo law: $marker")
        DetailedTempoLawMarkers.foreach: marker =>
          assert(!doc.contains(marker), s"$name must not duplicate detailed Tempo law: $marker")
        Vector(
          "Tempo is next opened slice",
          "QueenHit gains tempo wording",
          "Tempo is public",
          "QueenHit may say tempo",
          "the next slice is approved",
          "public narration may use tempo"
        ).foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Tempo summary wording: $phrase")
