package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage4DuplicateNeighborMatrixAssertions:

  private val Columns =
    Vector(
      "neighborhood",
      "collision surface",
      "owner order",
      "duplicate-neighbor no-go",
      "closed stronger wording"
    )

  def assertMatrix(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
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
      "## Stage-4 Duplicate Neighbor Matrix",
      "Stage-4 opens a duplicate-neighbor matrix in `StoryInteractionLaw.md`.",
      "Stage-4 duplicate-neighbor matrix:",
      "Stage-4 matrix rules:",
      "Stage-4 duplicate-neighbor matrix keeps closed:",
      "Completion standard: Stage-4 Duplicate Neighbor Matrix closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-4 duplicate-neighbor law must pin: $marker")

    val rows = parseMatrix(interactionLaw)
    assert(rows.size >= 7, "Stage-4 matrix must cover all requested high-risk neighborhoods")

    requireRow(rows, "Material / Hanging / Loose", Vector("Scene.Material", "Tactic.Hanging", "Tactic.Loose"))
    requireRow(rows, "QueenHit / Loose / Hanging / Material", Vector("Tactic.QueenHit", "Tactic.Loose", "Tactic.Hanging", "Scene.Material"))
    requireRow(rows, "RemoveGuard / Defense / Pin / DiscoveredAttack", Vector("Tactic.RemoveGuard", "Scene.Defense", "Tactic.Pin", "Tactic.DiscoveredAttack"))
    requireRow(rows, "Overload / RemoveGuard / CannotSatisfyBoth / Material / Loose / EngineCheck", Vector("Tactic.Overload", "Tactic.RemoveGuard", "CannotSatisfyBoth", "Scene.Material", "Tactic.Loose", "EngineCheck"))
    requireRow(rows, "PawnAdvance / PawnStop / PromotionThreat / Promotion / PawnBlock / FileOpened", Vector("Scene.PawnAdvance", "Scene.PawnStop", "Scene.PromotionThreat", "Scene.Promotion", "Scene.PawnBlock", "Scene.FileOpened"))
    requireRow(rows, "CheckGiven / Checkmate / CheckEscaped", Vector("Scene.CheckGiven", "Scene.Checkmate", "Scene.CheckEscaped"))
    requireRow(rows, "EngineCheck / all public Stories", Vector("EngineCheck", "supports", "caps", "refutes", "status evidence"))

    rows.foreach: row =>
      assert(
        row("owner order").contains("existing proof home") || row("owner order").contains("status evidence only"),
        s"Stage-4 matrix orders ownership only: $row"
      )
      assert(
        row("duplicate-neighbor no-go").nonEmpty && row("closed stronger wording").nonEmpty,
        s"Stage-4 matrix row must carry no-go and closed wording boundaries: $row"
      )

    val engineRow = rows.find(_("neighborhood").contains("EngineCheck / all public Stories")).get
    assert(engineRow("owner order").contains("status evidence only"))
    assert(engineRow("duplicate-neighbor no-go").contains("standalone public text"))

    val forbiddenWording =
      Vector(
        "material result from " + "Overload",
        "loose/hanging from " + "QueenHit",
        "defender removed from " + "Overload",
        "cannot satisfy both as public " + "Overload",
        "best/only/forced/winning/decisive/no-counterplay from " + "EngineCheck",
        "mate/no-escape from " + "CheckGiven",
        "promotion threat from actual " + "Promotion",
        "FileOpened as pressure/invasion/" + "strategy"
      )
    (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        forbiddenWording.foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-4 wording: $phrase")

    val detailedMarkers =
      Vector(
        "## Stage-4 Duplicate Neighbor Matrix",
        "Stage-4 duplicate-neighbor matrix:",
        "Stage-4 matrix rules:",
        "Stage-4 duplicate-neighbor matrix keeps closed:",
        "Completion standard: Stage-4 Duplicate Neighbor Matrix closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-4 marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-4 law: $marker")

    assert(!agents.contains("## Stage-4 Duplicate Neighbor Matrix"))
    assert(!agents.contains("Stage-4 duplicate-neighbor matrix:"))

  private def requireRow(rows: Vector[Map[String, String]], neighborhood: String, requiredTerms: Vector[String]): Unit =
    val row = rows.find(_("neighborhood").contains(neighborhood)).getOrElse(
      fail(s"Stage-4 matrix must cover neighborhood: $neighborhood")
    )
    requiredTerms.foreach: term =>
      assert(row.values.exists(_.contains(term)), s"Stage-4 row $neighborhood must mention $term")

  private def parseMatrix(doc: String): Vector[Map[String, String]] =
    val lines = doc.linesIterator.toVector
    val markerIndex = lines.indexWhere(_.trim == "Stage-4 duplicate-neighbor matrix:")
    assert(markerIndex >= 0, "Stage-4 duplicate-neighbor matrix marker must exist")
    val headerIndex = lines.indexWhere(line => line.trim.startsWith("| neighborhood |"), markerIndex)
    assert(headerIndex >= 0, "Stage-4 duplicate-neighbor matrix header must exist")
    assertEquals(cells(lines(headerIndex)), Columns)
    lines
      .drop(headerIndex + 2)
      .takeWhile(_.trim.startsWith("|"))
      .map: line =>
        val rowCells = cells(line)
        assertEquals(rowCells.size, Columns.size, s"Stage-4 matrix row must keep exact shape: $line")
        Columns.zip(rowCells).toMap

  private def cells(line: String): Vector[String] =
    line.trim.stripPrefix("|").stripSuffix("|").split("\\|", -1).toVector.map(_.trim)
