package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class ProofDeficitDiagnosticsStage1Test extends munit.FunSuite:

  test("Stage-1 uses the same generic diagnostic shape for unrelated blocked families"):
    val loose = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(writer = None)
    val material = SceneMaterial.write(board(materialFen), materialMove).get.copy(writer = None)
    val diagnostics = Vector(loose, material).map(row => ProofDeficitDiagnostics.fromVerdict(blocked(row)).get)

    assertEquals(diagnostics.map(_.storyIdentityLabel), Vector("Tactic.Loose", "Material"))
    assertEquals(diagnostics.map(_.leadAllowed), Vector(false, false))
    diagnostics.foreach: diagnostic =>
      assertEquals(diagnostic.roleReason, Some("blocked"))
      assertEquals(diagnostic.blockedBy.contains("writer"), true)
      assertEquals(diagnostic.boardFactsPresent.contains("story_identity"), true)
      assertEquals(diagnostic.proofCoordinates.root.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.side.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.rival.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.target.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.anchor.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.route.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.requiredLegalLine.nonEmpty, true)
      assertEquals(diagnostic.proofCoordinates.sameRootProofSidecar, Some("StoryProof"))
      assertEquals(diagnostic.missingSidecar.contains("Story writer"), true)
      assertEquals(diagnostic.reason.nonEmpty, true)

    assertEquals(diagnostics.map(_.getClass), Vector.fill(2)(diagnostics.head.getClass))
    assertEquals(diagnostics.map(_.proofCoordinates.getClass), Vector.fill(2)(diagnostics.head.proofCoordinates.getClass))

  test("Stage-1 reports non-lead role reason without creating claim output"):
    val loose = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    val material = SceneMaterial.write(board(materialFen), materialMove).get.copy(proof = orderingProof)
    val looseSupport = StoryTable.choose(Vector(material, loose)).find(_.story.tactic.contains(Tactic.Loose)).get
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(looseSupport).get

    assertEquals(looseSupport.role, Role.Support)
    assertEquals(diagnostic.leadAllowed, false)
    assertEquals(diagnostic.roleReason, Some("non_lead_role:Support"))
    assertEquals(diagnostic.blockedBy.contains("non_lead_role"), true)
    assertEquals(diagnostic.missingSidecar, Vector.empty)
    assertEquals(ExplanationPlan.fromSelected(looseSupport), None)
    assertEquals(ExplanationPlan.fromSelected(looseSupport).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-1 diagnostics stay out of Verdict values and public route payloads"):
    val story = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(writer = None)
    val verdict = blocked(story)
    val values = verdict.values
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assertEquals(ProofDeficitDiagnostics.fromVerdict(verdict).nonEmpty, true)
    assertEquals(values, verdict.values)
    assertEquals(values.size, Verdict.Size)
    assert(!values.map(_.toString).mkString(" ").contains("storyIdentityLabel"))
    assert(!controller.contains("ProofDeficitDiagnostics"))
    assert(!controller.contains("ProofDeficitDiagnostic"))
    assert(!controller.contains("storyIdentityLabel"))
    assert(!controller.contains("proofCoordinates"))
    assert(controller.contains("\"status\" -> \"unavailable\""))
    assert(controller.contains("\"noCommentary\" -> true"))
    assert(controller.contains("\"render\" -> JsNull"))
    assert(!controller.contains("Ok("), "commentary route tombstones must not return 200")

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  private def blocked(story: Story): Verdict =
    StoryTable.choose(Vector(story)).head

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def orderingProof: Proof =
    Proof(
      boardProof = 80,
      lineProof = 80,
      ownerProof = 80,
      anchorProof = 80,
      routeProof = 80,
      persistence = 80,
      immediacy = 80,
      forcing = 80,
      conversionPrize = 80,
      counterplayRisk = 20,
      kingHeat = 80,
      pieceSupport = 80,
      pawnSupport = 80,
      sourceFit = 80,
      novelty = 80,
      clarity = 80
    )
