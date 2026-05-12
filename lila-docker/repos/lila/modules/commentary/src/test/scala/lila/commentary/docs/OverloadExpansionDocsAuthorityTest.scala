package lila.commentary.docs

import java.nio.file.{ Files, Paths }

class OverloadExpansionDocsAuthorityTest extends munit.FunSuite:

  private val docsRoot = Paths.get("modules/commentary/docs")
  private val agentInstructions = Paths.get("../../..", "AGENTS.md")
  private def agents: String = Files.readString(agentInstructions)
  private val LiveDocs =
    Vector(
      "BoardFacts.md",
      "BoardMoodCutLaw.md",
      "BoardMoodSplitLaw.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "ChessResetRationale.md",
      "LegacyPruneManifest.md",
      "README.md",
      "StoryInteractionLaw.md",
      "StoryResurrectionLaw.md"
    )
  private val SummaryDocs =
    Vector(
      "README.md",
      "ChessCommentarySSOT.md",
      "ChessModelArchitecture.md",
      "ChessModelContract.md",
      "LegacyPruneManifest.md"
    )

  test("Overload narrow expansion audit detailed authority lives only in StoryInteractionLaw"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val markers =
      Vector(
        "## Stage-0: Overload Narrow Expansion Audit Charter",
        "Stage-0 opens audit-only scope for possible next `Tactic.Overload` narrow expansion.",
        "## Stage-1: Overload Read-Only Expansion Inventory",
        "Stage-1 neighbor owner inventory:",
        "## Stage-2: Overload Candidate Meaning Classification",
        "Stage-2 candidate classification table:",
        "## Stage-3: Overload Duplicate Collision Negative Corpus",
        "Stage-3 collision fixture duties:",
        "## Stage-4: Overload Possibly-Unique Candidate Proof Test",
        "Stage-4 result:",
        "- no next Overload public meaning admitted",
        "## Stage-5: Overload Downstream Boundary Audit",
        "Stage-5 required downstream contract:",
        "## Stage-6: Overload Audit Decision Closeout",
        "- C. Boundary leakage found and fixed without opening new meaning.",
        "`OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`"
      )

    markers.foreach: marker =>
      assert(interactionLaw.contains(marker), s"Overload expansion audit law must pin: $marker")
      val owners = LiveDocs.filter(name => Files.readString(docsRoot.resolve(name)).contains(marker)).toSet
      assertEquals(owners, Set("StoryInteractionLaw.md"), s"Overload expansion marker must have one owner: $marker")
      SummaryDocs.foreach: name =>
        assert(!Files.readString(docsRoot.resolve(name)).contains(marker), s"$name must not duplicate: $marker")

    assert(!agents.contains("Overload Narrow Expansion Audit"))
    assert(!agents.contains("Overload Duplicate Collision Negative Corpus"))
    assert(!agents.contains("Overload Downstream Boundary Audit"))

  test("Overload candidate classification keeps duplicate consequences outside Overload"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val stage2 =
      """(?s)## Stage-2: Overload Candidate Meaning Classification.*?(?=\n## |\z)""".r
        .findFirstIn(interactionLaw)
        .getOrElse(fail("Stage-2 Overload classification section missing"))
    val rows =
      stage2.linesIterator
        .filter(line => line.startsWith("| ") && !line.startsWith("| ---"))
        .drop(1)
        .map(line => line.stripPrefix("|").stripSuffix("|").split("\\|").toVector.map(_.trim))
        .toVector
    val allowedStatuses = Set("duplicate-owned", "internal-ingredient-only", "possibly-unique", "rejected-overclaim")

    assert(rows.nonEmpty, "Stage-2 candidate classification table must contain rows")
    rows.foreach: row =>
      assertEquals(row.length, 4, s"Stage-2 classification row must have four columns: ${row.mkString("|")}")
      assert(allowedStatuses.contains(row(1)), s"Stage-2 row has unknown status: ${row.mkString("|")}")
    assertEquals(rows.map(_(0)).distinct.length, rows.length, "Stage-2 candidate rows must not duplicate names")

    val duplicateOwnedConsequenceRows =
      rows.filter(row =>
        Vector("material", "loose", "undefended", "hanging", "queen", "defender removed", "defense", "EngineCheck")
          .exists(fragment => row(0).contains(fragment))
      )
    assert(duplicateOwnedConsequenceRows.nonEmpty, "Stage-2 must classify duplicate-owned consequence meanings")
    duplicateOwnedConsequenceRows.foreach: row =>
      assertEquals(row(1), "duplicate-owned", s"Consequence meaning must stay duplicate-owned: ${row.mkString("|")}")
      assert(!row(2).contains("OverloadProof"), s"Consequence owner must remain outside Overload proof: ${row.mkString("|")}")
      assert(!row(2).contains("TacticOverload"), s"Consequence owner must remain outside Overload writer: ${row.mkString("|")}")
      assert(!row(2).contains("overloads_defender"), s"Consequence owner must remain outside Overload speech key: ${row.mkString("|")}")

  test("Overload audit closeout keeps public surfaces and result ownership closed"):
    val interactionLaw = Files.readString(docsRoot.resolve("StoryInteractionLaw.md"))
    val stage6 =
      """(?s)## Stage-6: Overload Audit Decision Closeout.*?(?=\n## |\z)""".r
        .findFirstIn(interactionLaw)
        .getOrElse(fail("Stage-6 Overload audit decision section missing"))

    Vector(
      "- C. Boundary leakage found and fixed without opening new meaning.",
      "- Stage-3 found that Overload LLM smoke accepted loose-target wording stronger than `overloads_defender`.",
      "- The fix added Overload-only rejection for loose-target wording in the internal LLM smoke verifier.",
      "- No renderer text, public route, production API, public/user-facing LLM narration, Story label, writer, speech key, result Story, broad Overload family, or duplicate consequence owner was added.",
      "Stage-6 current Overload authority remains exactly:",
      "`OverloadProof -> Tactic.Overload -> TacticOverload -> overloads_defender`",
      "Stage-6 keeps closed:",
      "- public route `200`",
      "- production API",
      "- public/user-facing LLM narration",
      "- new Overload result Story",
      "- broad Overload family",
      "- Material consequence ownership",
      "- Loose consequence ownership",
      "- Hanging consequence ownership",
      "- QueenHit consequence ownership",
      "- RemoveGuard consequence ownership",
      "- Defense consequence ownership",
      "- CannotSatisfyBoth public wording",
      "- EngineCheck public claim ownership"
    ).foreach: marker =>
      assert(stage6.contains(marker), s"Stage-6 Overload closeout must pin: $marker")
