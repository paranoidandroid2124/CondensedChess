package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage2OwnershipIndexShapeAssertions:

  private val RequiredColumns =
    Vector(
      "public meaning",
      "proof home",
      "Story label",
      "writer",
      "speech key",
      "allowed claim boundary",
      "duplicate-neighbor no-go"
    )

  private val ExpectedPublicMeanings =
    Vector(
      "Tactic.Hanging",
      "Tactic.Fork",
      "Scene.Material",
      "Scene.Defense",
      "Tactic.DiscoveredAttack",
      "Tactic.Pin",
      "Tactic.RemoveGuard",
      "Tactic.Overload",
      "Tactic.Skewer",
      "Tactic.QueenHit",
      "Tactic.Loose",
      "Scene.PawnAdvance",
      "Scene.PawnStop",
      "Scene.PawnBreak",
      "Scene.PawnBlock",
      "Scene.PromotionThreat",
      "Scene.Promotion",
      "Scene.PawnCapture",
      "Scene.PassedPawnCreated",
      "Scene.FileOpened",
      "Scene.CheckGiven",
      "Scene.CheckEscaped",
      "Scene.Checkmate",
      "Scene.Stalemate"
    )

  def assertShape(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
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
      "## Stage-2 Ownership Index Shape",
      "Stage-2 opens one generic ownership index table in `StoryInteractionLaw.md`.",
      "The ownership index table is an audit artifact, not runtime authority.",
      "Stage-2 generic ownership index table:",
      "Stage-2 non-public ingredients:",
      "Stage-2 ownership index keeps closed:",
      "- no runtime parser for the table",
      "- no generated code",
      "- no public API exposure",
      "- no production surface",
      "Completion standard: Stage-2 Ownership Index Shape closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-2 ownership index law must pin: $marker")

    val rows = parseOwnershipRows(interactionLaw)
    assertEquals(rows.map(_("public meaning").replace("`", "")), ExpectedPublicMeanings)
    rows.foreach: row =>
      assertEquals(row.keySet, RequiredColumns.toSet, s"Stage-2 row must use only required columns: $row")
      RequiredColumns.foreach: column =>
        assert(row(column).nonEmpty, s"Stage-2 row must not omit $column: $row")

    Vector("proof home", "Story label", "writer", "speech key").foreach: column =>
      val values = rows.map(_(column).replace("`", ""))
      assertEquals(values.distinct.size, values.size, s"Stage-2 $column values must be unique")
      values.foreach: value =>
        assert(!value.equalsIgnoreCase("none"), s"Stage-2 $column must be named")
        assert(!value.equalsIgnoreCase("incomplete-audit"), s"Stage-2 $column must be named")
        assert(value.exists(_.isLetterOrDigit), s"Stage-2 $column must be named")

    rows.foreach: row =>
      val ownerCells = Vector(row("proof home"), row("writer"), row("speech key"))
      "BoardFacts|EngineCheck|proofFailures|diagnostics|Diagnostic|renderer|Renderer|LLM"
        .split("\\|")
        .foreach: forbidden =>
          assert(
            !ownerCells.exists(_.contains(forbidden)),
            s"Stage-2 public row must not use $forbidden as proof home, writer, or speech owner: $row"
          )

    val nonPublicSection = sectionAfter(interactionLaw, "Stage-2 non-public ingredients:")
    Vector("BoardFacts", "EngineCheck", "proof-deficit diagnostics", "DeterministicRenderer", "LLM smoke")
      .foreach: ingredient =>
        assert(
          nonPublicSection.contains(ingredient),
          s"Stage-2 internal ingredient must be listed outside public rows: $ingredient"
        )

    val detailedMarkers =
      Vector(
        "## Stage-2 Ownership Index Shape",
        "Stage-2 generic ownership index table:",
        "Stage-2 non-public ingredients:",
        "Stage-2 ownership index keeps closed:",
        "Completion standard: Stage-2 Ownership Index Shape closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-2 marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-2 law: $marker")

    val forbiddenWording =
      Vector(
        "claim registry creates " + "Story",
        "proof score creates " + "row",
        "renderer owns " + "claim",
        "LLM owns " + "claim",
        "EngineCheck owns public " + "meaning"
      )
    (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        forbiddenWording.foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-2 wording: $phrase")

    assert(!agents.contains("## Stage-2 Ownership Index Shape"))
    assert(!agents.contains("Stage-2 generic ownership index table:"))

  private def parseOwnershipRows(doc: String): Vector[Map[String, String]] =
    val lines = doc.linesIterator.toVector
    val markerIndex = lines.indexWhere(_.trim == "Stage-2 generic ownership index table:")
    assert(markerIndex >= 0, "Stage-2 ownership index table marker must exist")
    val headerIndex = lines.indexWhere(line => line.trim.startsWith("| public meaning |"), markerIndex)
    assert(headerIndex >= 0, "Stage-2 ownership index table header must exist")
    val header = cells(lines(headerIndex))
    assertEquals(header, RequiredColumns, "Stage-2 ownership index table must use exactly required columns")
    assert(lines.lift(headerIndex + 1).exists(_.trim.matches("^\\|[- ]+(\\|[- ]+)+\\|$")))

    lines
      .drop(headerIndex + 2)
      .takeWhile(_.trim.startsWith("|"))
      .map: line =>
        val rowCells = cells(line)
        assertEquals(rowCells.size, RequiredColumns.size, s"Stage-2 row must have exactly required cells: $line")
        RequiredColumns.zip(rowCells).toMap

  private def cells(line: String): Vector[String] =
    line.trim.stripPrefix("|").stripSuffix("|").split("\\|", -1).toVector.map(_.trim)

  private def sectionAfter(doc: String, marker: String): String =
    val start = doc.indexOf(marker)
    assert(start >= 0, s"section marker must exist: $marker")
    val remaining = doc.substring(start + marker.length)
    val end = remaining.indexOf("\n## ")
    if end >= 0 then remaining.substring(0, end) else remaining
