package lila.commentary.docs

import java.nio.file.{ Files, Path }

import scala.jdk.CollectionConverters.*

import munit.Assertions.*

object Stage0TempoQueenHitPreflightAssertions:

  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  private val ForbiddenWording =
    Vector(
      "tempo",
      "gains tempo",
      "wins tempo",
      "with tempo",
      "forces the queen",
      "queen must move",
      "free move",
      "gains time",
      "seizes initiative",
      "keeps initiative",
      "pressure",
      "best",
      "only",
      "forced",
      "winning",
      "decisive",
      "no counterplay"
    )

  def assertPreflight(docsRoot: Path, liveDocs: Vector[String], agents: String): Unit =
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val sourceRoot = Paths.commentarySourceRoot(docsRoot)
    val storySource = Files.readString(sourceRoot.resolve("Story.scala"))
    val explanationPlanSource = Files.readString(sourceRoot.resolve("ExplanationPlan.scala"))
    val mainSource = readScalaSource(sourceRoot)

    Vector(
      "## Stage-0 Tempo / QueenHit Collision Preflight Charter",
      "Stage-0 Tempo / QueenHit opens audit-only preflight for Tempo viability against QueenHit and neighbor owners.",
      "Stage-0 Tempo / QueenHit proof authority:",
      "Stage-0 Tempo / QueenHit read-only inventory:",
      "Stage-0 Tempo / QueenHit forbidden wording:",
      "Stage-0 Tempo / QueenHit keeps closed:",
      "Completion standard: Stage-0 Tempo / QueenHit Collision Preflight closes when"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Tempo preflight law must pin: $marker")

    Vector(
      "`QueenHitProof` -> `Tactic.QueenHit` -> `TacticQueenHit` -> `attacks_queen`",
      "QueenHit currently owns only bounded queen-attack wording.",
      "Tempo preflight cannot create claims, rank Stories, repair proof, or widen QueenHit.",
      "EngineCheck supports, caps, or refutes only existing Stories.",
      "StoryTable orders only.",
      "Verdict decides.",
      "ExplanationPlan bounds speech.",
      "Renderer phrases.",
      "LLM only polishes.",
      "Verifier rejects overclaim."
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Tempo preflight proof authority must pin: $marker")

    Vector(
      "- no `Tactic.Tempo` public Story",
      "- no `TempoProof`",
      "- no `TacticTempo` writer",
      "- no tempo speech key",
      "- no ExplanationPlan claim for Tempo",
      "- no renderer text for Tempo",
      "- no LLM smoke expansion except forbidden-boundary checks if a leak is found",
      "- no QueenHit consequence expansion",
      "- no public route `200`",
      "- no production API",
      "- no public/user-facing LLM narration"
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Tempo preflight closed marker missing: $marker")

    ForbiddenWording.foreach: phrase =>
      assert(interactionLaw.contains(s"- $phrase"), s"Tempo forbidden wording missing: $phrase")

    Vector(
      "`Tactic.Tempo` exists only as a closed enum tombstone.",
      "`TempoProof` has no runtime proof home.",
      "`TacticTempo` has no runtime writer.",
      "Tempo has no `ExplanationClaim` case and no allowed claim key.",
      "`ForbiddenWording.GainsTempo` is a rejection guard only.",
      "QueenHit runtime tests already reject tempo additions around `attacks_queen`."
    ).foreach: marker =>
      assert(interactionLaw.contains(marker), s"Tempo inventory marker missing: $marker")

    assert(storySource.contains("case Tempo"), "Tactic.Tempo enum tombstone must remain visible for closed-name guard")
    assert(!mainSource.contains("TempoProof"), "TempoProof must not exist before unique proof ownership survives")
    assert(!mainSource.contains("TacticTempo"), "TacticTempo writer must not exist in Stage-0 preflight")

    val explanationClaimEnum =
      explanationPlanSource
        .split("private\\[commentary\\] object ExplanationClaim:", 2)
        .headOption
        .getOrElse(explanationPlanSource)
    assert(!explanationClaimEnum.contains("Tempo"), "ExplanationClaim must not contain a Tempo claim")
    assert(explanationPlanSource.contains("ForbiddenWording.GainsTempo"), "Tempo may appear as forbidden wording guard only")

    val detailedMarkers =
      Vector(
        "## Stage-0 Tempo / QueenHit Collision Preflight Charter",
        "Stage-0 Tempo / QueenHit proof authority:",
        "Stage-0 Tempo / QueenHit read-only inventory:",
        "Stage-0 Tempo / QueenHit forbidden wording:",
        "Completion standard: Stage-0 Tempo / QueenHit Collision Preflight closes when"
      )
    detailedMarkers.foreach: marker =>
      val owners = liveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Tempo preflight marker must have one owner: $marker")

    SummaryDocs.foreach: name =>
      val doc = Files.readString(docsRoot.resolve(name))
      detailedMarkers.foreach: marker =>
        assert(!doc.contains(marker), s"$name must not duplicate detailed Tempo preflight law: $marker")

    detailedMarkers.foreach: marker =>
      assert(!agents.contains(marker), s"AGENTS.md must not duplicate detailed Tempo preflight law: $marker")

  private def readScalaSource(root: Path): String =
    val stream = Files.walk(root)
    try
      stream
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
        .map(Files.readString)
        .mkString("\n")
    finally stream.close()

  private object Paths:
    def commentarySourceRoot(docsRoot: Path): Path =
      docsRoot.getParent.resolve("src/main/scala/lila/commentary/chess")
