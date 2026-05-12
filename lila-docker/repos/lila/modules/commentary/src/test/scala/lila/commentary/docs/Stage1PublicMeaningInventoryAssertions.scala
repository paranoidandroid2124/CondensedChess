package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage1PublicMeaningInventoryAssertions:

  def assertInventory(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
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
      "## Stage-1 Current Public Meaning Inventory",
      "Stage-1 opens read-only inventory of every currently opened public Story meaning.",
      "Stage-1 inventory extraction rules:",
      "Stage-1 current public meaning inventory:",
      "Stage-1 inventory keeps closed:",
      "- no implementation changes",
      "- no table-driven runtime path",
      "- no new public meaning",
      "- no docs duplication outside `StoryInteractionLaw.md`",
      "Completion standard: Stage-1 Current Public Meaning Inventory closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-1 current inventory law must pin: $marker")

    val expectedPublicMeanings =
      "Tactic.Hanging|Tactic.Fork|Scene.Material|Scene.Defense|Tactic.DiscoveredAttack|Tactic.Pin|" +
        "Tactic.RemoveGuard|Tactic.Overload|Tactic.Skewer|Tactic.QueenHit|Tactic.Loose|" +
        "Scene.PawnAdvance|Scene.PawnStop|Scene.PawnBreak|Scene.PawnBlock|Scene.PromotionThreat|" +
        "Scene.Promotion|Scene.PawnCapture|Scene.PassedPawnCreated|Scene.FileOpened|" +
        "Scene.CheckGiven|Scene.CheckEscaped|Scene.Checkmate|Scene.Stalemate"

    val inventoryRows =
      interactionLaw.linesIterator
        .filter(_.startsWith("| `PMI-"))
        .map: line =>
          line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim)
        .toVector

    assertEquals(inventoryRows.map(row => row(1).replace("`", "")), expectedPublicMeanings.split("\\|").toVector)
    inventoryRows.foreach: row =>
      assertEquals(row.size, 10, s"Stage-1 inventory row must carry all requested fields: ${row.mkString("|")}")
      assert(!row.contains(""), s"Stage-1 inventory row must not omit fields: ${row.mkString("|")}")
      assert(row.last.contains("complete-audit"), s"opened rows must have complete audit state: ${row.mkString("|")}")

    "Tactic.AbsPin|Tactic.RelPin|Tactic.Xray|Tactic.PawnFork|Tactic.Decoy|Tactic.Deflect|Scene.Plan|Scene.Source|Scene.Quiet"
      .split("\\|")
      .foreach: tombstone =>
        assert(
          !inventoryRows.exists(row => row(1).contains(tombstone) || row(3).contains(tombstone)),
          s"Stage-1 inventory must not classify closed enum tombstone as opened: $tombstone"
        )

    inventoryRows.foreach: row =>
      val claimOwnerCells = Vector(row(1), row(3), row(4), row(5))
      "BoardFacts|EngineCheck|proofFailures|Diagnostic|DeterministicRenderer|LLM"
        .split("\\|")
        .foreach: forbidden =>
          assert(
            !claimOwnerCells.exists(_.contains(forbidden)),
            s"Stage-1 inventory must not classify $forbidden as a claim owner: ${row.mkString("|")}"
          )

    val detailedMarkers =
      Vector(
        "## Stage-1 Current Public Meaning Inventory",
        "Stage-1 inventory extraction rules:",
        "Stage-1 current public meaning inventory:",
        "Stage-1 inventory keeps closed:",
        "Completion standard: Stage-1 Current Public Meaning Inventory closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-1 inventory marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-1 inventory law: $marker")

    assert(!agents.contains("## Stage-1 Current Public Meaning Inventory"))
    assert(!agents.contains("Stage-1 inventory extraction rules:"))
