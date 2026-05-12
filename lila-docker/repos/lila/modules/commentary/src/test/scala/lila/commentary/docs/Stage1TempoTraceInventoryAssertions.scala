package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage1TempoTraceInventoryAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val Classifications =
    Vector(
      "closed enum tombstone",
      "forbidden wording",
      "internal proof field",
      "duplicate-owned neighbor meaning",
      "rejected broad strategy wording",
      "possibly unique candidate"
    )

  def assertInventory(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))

    Vector(
      "## Stage-1 Tempo Trace Inventory",
      "Stage-1 Tempo Trace Inventory opens read-only inventory only.",
      "Stage-1 Tempo trace classification set:",
      "Stage-1 Tempo runtime trace inventory:",
      "Stage-1 Tempo docs trace inventory:",
      "Stage-1 Tempo neighbor collision inventory:",
      "Stage-1 Tempo Trace Inventory keeps closed:",
      "Completion standard: Stage-1 Tempo Trace Inventory closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-1 Tempo inventory must pin: $marker")

    Classifications.foreach: classification =>
      assert(interactionLaw.contains(s"- $classification"), s"Stage-1 Tempo classification missing: $classification")

    Vector(
      "`Tactic.Tempo`",
      "`ForbiddenWording.GainsTempo`",
      "`materialOrTempoResult`",
      "QueenHit",
      "Loose",
      "Hanging",
      "Material",
      "Fork",
      "Skewer",
      "Pin",
      "RemoveGuard",
      "Defense",
      "EngineCheck"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-1 Tempo inventory missing trace or neighbor marker: $marker")

    Vector(
      "- no implementation",
      "- no new proof home",
      "- no new writer",
      "- no new public Story",
      "- no new downstream wording"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-1 Tempo closed marker missing: $marker")

    Vector(
      "code presence is not public authority",
      "forbidden wording labels are not opened claims",
      "Stage-1 inventory lives only in `StoryInteractionLaw.md`."
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-1 Tempo authority marker missing: $marker")

    Vector(
      "Tempo is open",
      "Tempo is planned public meaning",
      "Tempo claim exists"
    ).foreach: phrase =>
      (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
        (name, doc) =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Tempo inventory wording: $phrase")

    val detailedMarkers =
      Vector(
        "## Stage-1 Tempo Trace Inventory",
        "Stage-1 Tempo trace classification set:",
        "Stage-1 Tempo runtime trace inventory:",
        "Stage-1 Tempo docs trace inventory:",
        "Stage-1 Tempo neighbor collision inventory:",
        "Completion standard: Stage-1 Tempo Trace Inventory closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-1 Tempo inventory marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-1 Tempo inventory law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Stage-1 Tempo inventory law: $marker")
