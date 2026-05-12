package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage5ClosedInternalNameGuardAssertions:

  private val HighRiskNames =
    Vector(
      "DefenderDuty",
      "DualDefenderDuty",
      "OverloadTest",
      "CannotSatisfyBoth",
      "proofFailures",
      "ProofDeficitDiagnostics",
      "raw EngineCheck diagnostics",
      "closed PawnFork public path",
      "broad Tempo public path",
      "broad Plan/Source/Opening/Quiet public speech"
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
      "## Stage-5 Closed Names / Internal Ingredients Guard",
      "Stage-5 opens a closed-name and internal-ingredient guard section in `StoryInteractionLaw.md`.",
      "Stage-5 closed/internal guard rules:",
      "Stage-5 high-risk closed and internal names:",
      "Stage-5 closed/internal guard keeps closed:",
      "Completion standard: Stage-5 Closed Names / Internal Ingredients Guard closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Stage-5 closed/internal guard must pin: $marker")

    HighRiskNames.foreach: name =>
      assert(interactionLaw.contains(name), s"Stage-5 guard must list high-risk closed/internal name: $name")

    val publicOwnerCells =
      stage0OwnerCells(interactionLaw) ++
        stage1OwnerCells(interactionLaw) ++
        stage2OwnerCells(interactionLaw)

    val forbiddenOwnerTokens =
      HighRiskNames ++ Vector("PawnFork", "Tempo", "Plan", "Source", "Opening", "Quiet")

    publicOwnerCells.foreach: cell =>
      forbiddenOwnerTokens.foreach: token =>
        assert(!cell.contains(token), s"closed/internal name must not appear as public owner: $token in $cell")

    val forbiddenWording =
      Vector(
        "closed names " + "backlog",
        "closed enum values \"planned public rows\"",
        "internal ingredients are " + "claims"
      )
    (liveDocs.map(name => name -> Files.readString(docsRoot.resolve(name))) :+ ("AGENTS.md" -> agents)).foreach:
      (name, doc) =>
        forbiddenWording.foreach: phrase =>
          assert(!doc.contains(phrase), s"$name must not use forbidden Stage-5 wording: $phrase")

    val detailedMarkers =
      Vector(
        "## Stage-5 Closed Names / Internal Ingredients Guard",
        "Stage-5 closed/internal guard rules:",
        "Stage-5 high-risk closed and internal names:",
        "Stage-5 closed/internal guard keeps closed:",
        "Completion standard: Stage-5 Closed Names / Internal Ingredients Guard closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-5 marker must have one owner: $marker")

    summaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-5 law: $marker")

    assert(!agents.contains("## Stage-5 Closed Names / Internal Ingredients Guard"))
    assert(!agents.contains("Stage-5 high-risk closed and internal names:"))

  private def stage0OwnerCells(doc: String): Vector[String] =
    rowsStarting(doc, "| `PMO-").flatMap: cells =>
      Vector(cells(1), cells(2), cells(3), cells(4))

  private def stage1OwnerCells(doc: String): Vector[String] =
    rowsStarting(doc, "| `PMI-").flatMap: cells =>
      Vector(cells(1), cells(2), cells(3), cells(4), cells(5))

  private def stage2OwnerCells(doc: String): Vector[String] =
    val rows = parseTableAfter(doc, "Stage-2 generic ownership index table:")
    rows.flatMap: cells =>
      Vector(cells(0), cells(1), cells(2), cells(3), cells(4))

  private def rowsStarting(doc: String, prefix: String): Vector[Vector[String]] =
    doc.linesIterator
      .filter(_.startsWith(prefix))
      .map(cells)
      .toVector

  private def parseTableAfter(doc: String, marker: String): Vector[Vector[String]] =
    val lines = doc.linesIterator.toVector
    val markerIndex = lines.indexWhere(_.trim == marker)
    assert(markerIndex >= 0, s"marker must exist: $marker")
    val headerIndex = lines.indexWhere(_.trim.startsWith("| public meaning |"), markerIndex)
    assert(headerIndex >= 0, s"table header must exist after $marker")
    lines.drop(headerIndex + 2).takeWhile(_.trim.startsWith("|")).map(cells)

  private def cells(line: String): Vector[String] =
    line.trim.stripPrefix("|").stripSuffix("|").split("\\|", -1).toVector.map(_.trim)
