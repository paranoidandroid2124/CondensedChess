package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage3OwnershipGuardAssertions:

  private val Columns =
    Vector(
      "public meaning",
      "proof home",
      "Story label",
      "writer",
      "speech key",
      "allowed claim boundary",
      "duplicate-neighbor no-go"
    )

  def assertGuard(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
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
      "## Stage-3 One Meaning / One Owner Guard",
      "Stage-3 opens docs and runtime tests that enforce uniqueness across the current opened surface.",
      "Stage-3 guard rules:",
      "Stage-3 alias and consequence no-go:",
      "Stage-3 ownership guard keeps closed:",
      "Completion standard: Stage-3 One Meaning / One Owner Guard closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-3 guard law must pin: $marker")

    val rows = parseRows(interactionLaw)
    assertEquals(rows.size, 24, "Stage-3 guard audits the current opened surface only")

    rows.groupBy(_("public meaning")).foreach: (meaning, meaningRows) =>
      assertEquals(meaningRows.map(_("proof home")).distinct.size, 1, s"$meaning must have one proof home")
      assertEquals(meaningRows.map(_("Story label")).distinct.size, 1, s"$meaning must have one Story label")
      assertEquals(meaningRows.map(_("writer")).distinct.size, 1, s"$meaning must have one writer")
      assertEquals(meaningRows.map(_("speech key")).distinct.size, 1, s"$meaning must have one speech key")
      assertEquals(
        meaningRows.head("public meaning").replace("`", ""),
        meaningRows.head("Story label").replace("`", ""),
        s"$meaning must not use a second public alias"
      )

    val speechKeys = rows.map(_("speech key").replace("`", ""))
    assertEquals(speechKeys.distinct.size, speechKeys.size, "speech keys must not be shared")

    val writers = rows.map(_("writer").replace("`", ""))
    assertEquals(writers.distinct.size, writers.size, "writers must not produce unrelated public meanings")

    val proofHomes = rows.map(_("proof home").replace("`", "")).toSet
    val storyLabels = rows.map(_("Story label").replace("`", "")).toSet
    assertEquals(proofHomes.intersect(storyLabels), Set.empty[String], "proof homes must not be Story labels")

    val internalIngredients =
      Set(
        "BoardFacts",
        "EngineCheck",
        "proof-deficit diagnostics",
        "DeterministicRenderer",
        "LLM smoke",
        "DefenderDuty",
        "DualDefenderDuty",
        "OverloadTest",
        "CannotSatisfyBoth"
      )
    assertEquals(speechKeys.toSet.intersect(internalIngredients), Set.empty[String])

    val detailedMarkers =
      Vector(
        "## Stage-3 One Meaning / One Owner Guard",
        "Stage-3 guard rules:",
        "Stage-3 alias and consequence no-go:",
        "Stage-3 ownership guard keeps closed:",
        "Completion standard: Stage-3 One Meaning / One Owner Guard closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-3 marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-3 law: $marker")

    val forbiddenWording =
      Vector(
        "aliases that make two names public for the same " + "meaning",
        "same " + "enough",
        "consequence wording to be owned by a cause " + "Story"
      )
    (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        forbiddenWording.foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-3 wording: $phrase")

    assert(!agents.contains("## Stage-3 One Meaning / One Owner Guard"))
    assert(!agents.contains("Stage-3 guard rules:"))

  private def parseRows(doc: String): Vector[Map[String, String]] =
    val lines = doc.linesIterator.toVector
    val markerIndex = lines.indexWhere(_.trim == "Stage-2 generic ownership index table:")
    assert(markerIndex >= 0, "Stage-2 ownership index table marker must exist")
    val headerIndex = lines.indexWhere(line => line.trim.startsWith("| public meaning |"), markerIndex)
    assert(headerIndex >= 0, "Stage-2 ownership index table header must exist")
    assertEquals(cells(lines(headerIndex)), Columns)
    lines
      .drop(headerIndex + 2)
      .takeWhile(_.trim.startsWith("|"))
      .map: line =>
        val rowCells = cells(line)
        assertEquals(rowCells.size, Columns.size, s"ownership row must keep exact shape: $line")
        Columns.zip(rowCells).toMap

  private def cells(line: String): Vector[String] =
    line.trim.stripPrefix("|").stripSuffix("|").split("\\|", -1).toVector.map(_.trim)
