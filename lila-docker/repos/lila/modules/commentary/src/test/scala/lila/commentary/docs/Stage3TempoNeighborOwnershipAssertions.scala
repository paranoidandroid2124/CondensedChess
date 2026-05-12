package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage3TempoNeighborOwnershipAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val AllowedClassifications =
    Set("duplicate-owned", "rejected-overclaim", "closed-broad", "internal-only", "possibly-unique")

  private val ExpectedRows =
    Vector(
      "queen attack tempo" -> ("duplicate-owned", Vector("QueenHit")),
      "loose target tempo" -> ("duplicate-owned", Vector("Loose")),
      "hanging or wins-piece tempo" -> ("duplicate-owned", Vector("Hanging", "Material")),
      "material gain tempo" -> ("duplicate-owned", Vector("Material")),
      "two-target tempo" -> ("duplicate-owned", Vector("Fork", "Skewer")),
      "pin or cannot-move tempo" -> ("duplicate-owned", Vector("Pin")),
      "defender removed tempo" -> ("duplicate-owned", Vector("RemoveGuard")),
      "defensive tempo" -> ("duplicate-owned", Vector("Defense")),
      "engine-approved tempo" -> ("internal-only", Vector("EngineCheck")),
      "initiative or pressure tempo" -> ("closed-broad", Vector("closed broad strategy wording")),
      "result-wording tempo" -> ("rejected-overclaim", Vector("result wording")),
      "same-board replay trace not owned elsewhere" -> ("possibly-unique", Vector("complete StoryProof"))
    )

  def assertMatrix(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val stage3 =
      """(?s)## Stage-3 Tempo Neighbor Ownership Classification.*?(?=\n## |\z)""".r
        .findFirstIn(interactionLaw)
        .getOrElse(fail("Stage-3 Tempo neighbor ownership section missing"))

    Vector(
      "## Stage-3 Tempo Neighbor Ownership Classification",
      "Stage-3 opens a Tempo candidate ownership classification matrix in `StoryInteractionLaw.md`.",
      "Stage-3 Tempo ownership classifications:",
      "Stage-3 Tempo candidate ownership matrix:",
      "Stage-3 Tempo uniqueness survival rule:",
      "Stage-3 Tempo forbidden alias rule:",
      "Stage-3 Tempo keeps closed:",
      "Completion standard: Stage-3 Tempo Neighbor Ownership Classification closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-3 Tempo ownership law must pin: $marker")

    AllowedClassifications.foreach: classification =>
      assert(stage3.contains(s"- $classification"), s"Stage-3 Tempo classification missing: $classification")

    val rows =
      stage3.linesIterator
        .filter(line => line.startsWith("| ") && !line.startsWith("| ---"))
        .drop(1)
        .map(line => line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim))
        .toVector

    assert(rows.nonEmpty, "Stage-3 Tempo candidate ownership matrix must contain rows")
    rows.foreach: row =>
      assertEquals(row.length, 4, s"Stage-3 Tempo matrix row must have four columns: ${row.mkString("|")}")
      assert(AllowedClassifications.contains(row(1)), s"Stage-3 Tempo row has unknown classification: ${row.mkString("|")}")
    assertEquals(rows.map(_(0)).distinct.length, rows.length, "Stage-3 Tempo matrix rows must not duplicate candidates")

    ExpectedRows.foreach: (candidate, expected) =>
      val matches = rows.filter(_(0) == candidate)
      assertEquals(matches.length, 1, s"Stage-3 Tempo candidate must appear exactly once: $candidate")
      val row = matches.head
      assertEquals(row(1), expected._1, s"Stage-3 Tempo candidate has wrong classification: ${row.mkString("|")}")
      expected._2.foreach: ownerFragment =>
        assert(
          row(2).contains(ownerFragment) || row(3).contains(ownerFragment),
          s"Stage-3 Tempo candidate missing owner or boundary fragment '$ownerFragment': ${row.mkString("|")}"
        )

    Vector(
      "A candidate survives only if it is not owned by any existing proof home and can bind same-board legal replay plus complete StoryProof.",
      "Do not call duplicate-owned candidates `Tempo`.",
      "Do not create aliases such as QueenTempo, MaterialTempo, DefenseTempo, or EngineTempo.",
      "Do not use result or initiative wording to justify uniqueness."
    ).foreach: marker =>
      assert(stage3.contains(marker), s"Stage-3 Tempo boundary marker missing: $marker")

    Vector("QueenTempo", "MaterialTempo", "DefenseTempo", "EngineTempo").foreach: alias =>
      assert(stage3.contains(alias), s"Stage-3 Tempo forbidden alias must be named: $alias")
      val source = stage3.linesIterator.filter(_.contains(alias)).mkString("\n")
      assert(source.contains("Do not create aliases"), s"Stage-3 Tempo alias must appear only as a forbidden alias: $alias")

    Vector(
      "- no positive Tempo scope",
      "- no new proof home",
      "- no new speech key"
    ).foreach: marker =>
      assert(stage3.contains(marker), s"Stage-3 Tempo closed marker missing: $marker")

    val detailedMarkers =
      Vector(
        "## Stage-3 Tempo Neighbor Ownership Classification",
        "Stage-3 Tempo candidate ownership matrix:",
        "Stage-3 Tempo uniqueness survival rule:",
        "Completion standard: Stage-3 Tempo Neighbor Ownership Classification closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-3 Tempo ownership marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-3 Tempo law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Stage-3 Tempo law: $marker")
