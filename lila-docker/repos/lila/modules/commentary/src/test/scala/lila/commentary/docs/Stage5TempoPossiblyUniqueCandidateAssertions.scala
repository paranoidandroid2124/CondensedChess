package lila.commentary.docs

import java.nio.file.{ Files, Path }

import munit.Assertions.*

object Stage5TempoPossiblyUniqueCandidateAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val RequiredBindFields =
    Vector(
      "scene/tactic/plan identity",
      "side",
      "rival",
      "target",
      "anchor",
      "route",
      "same-board legal replay",
      "complete StoryProof",
      "explicit rival reply restriction",
      "explicit non-duplication against QueenHit, Loose, Hanging, Material, Fork, Skewer, Pin, RemoveGuard, Defense, EngineCheck",
      "EngineCheck not Refute"
    )

  def assertPreflight(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val stage5 =
      """(?s)## Stage-5 Tempo Possibly-Unique Candidate Preflight.*?(?=\n## |\z)""".r
        .findFirstIn(interactionLaw)
        .getOrElse(fail("Stage-5 Tempo possibly-unique candidate preflight section missing"))

    Vector(
      "## Stage-5 Tempo Possibly-Unique Candidate Preflight",
      "Stage-5 opens audit evidence for at most one possibly-unique Tempo candidate.",
      "Stage-5 Tempo possible uniqueness bind requirements:",
      "Stage-5 Tempo duplicate rejection rules:",
      "Stage-5 Tempo closed outcome:",
      "Completion standard: Stage-5 Tempo Possibly-Unique Candidate Preflight closes when"
    ).foreach: marker =>
      assert(stage5.contains(marker), s"Stage-5 Tempo preflight law must pin: $marker")

    RequiredBindFields.foreach: field =>
      assert(stage5.contains(s"- $field"), s"Stage-5 Tempo bind requirement missing: $field")

    Vector(
      "If the candidate needs queen-attack proof, reject it as QueenHit.",
      "If the candidate needs material result proof, reject it as Material, Hanging, or Loose.",
      "If the candidate needs initiative or pressure wording, reject it as closed broad strategy.",
      "If the candidate needs best, only, or forced wording, reject it as EngineCheck diagnostics or overclaim."
    ).foreach: marker =>
      assert(stage5.contains(marker), s"Stage-5 Tempo rejection rule missing: $marker")

    Vector(
      "- no `Tactic.Tempo` Story",
      "- no TempoProof",
      "- no TacticTempo writer",
      "- no tempo speech key",
      "- no ExplanationPlan claim",
      "- no renderer text",
      "- no LLM smoke acceptance path"
    ).foreach: marker =>
      assert(stage5.contains(marker), s"Stage-5 Tempo closed marker missing: $marker")

    val closedNoPublicMeaning = stage5.contains("no unique Tempo public meaning admitted")
    val closedNextSliceCandidate = stage5.contains("closed next-slice candidate")
    assert(
      closedNoPublicMeaning ^ closedNextSliceCandidate,
      "Stage-5 Tempo preflight must record exactly one closed outcome"
    )
    if closedNextSliceCandidate then
      assertEquals(
        stage5.linesIterator.count(_.contains("closed next-slice candidate")),
        1,
        "Stage-5 Tempo preflight may record at most one closed next-slice candidate"
      )

    Vector(
      "TacticTempo",
      "TempoProof",
      "tempo speech key",
      "renderer text",
      "LLM smoke acceptance path"
    ).foreach: closedName =>
      val lines = stage5.linesIterator.filter(_.contains(closedName)).toVector
      assert(lines.nonEmpty, s"Stage-5 Tempo closed name must be present: $closedName")
      lines.foreach: line =>
        assert(
          line.contains("no ") || line.contains("closed"),
          s"Stage-5 Tempo closed name must appear only as closed preflight evidence: $line"
        )

    val detailedMarkers =
      Vector(
        "## Stage-5 Tempo Possibly-Unique Candidate Preflight",
        "Stage-5 Tempo possible uniqueness bind requirements:",
        "Stage-5 Tempo duplicate rejection rules:",
        "Stage-5 Tempo closed outcome:",
        "Completion standard: Stage-5 Tempo Possibly-Unique Candidate Preflight closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Stage-5 Tempo preflight marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Stage-5 Tempo law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Stage-5 Tempo law: $marker")
