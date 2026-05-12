package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class ProofDeficitDiagnosticsStage0Test extends munit.FunSuite:

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))

  test("Stage-0 diagnostics explain blocked proof coordinates without changing Verdict.values"):
    val story = TacticLoose.write(board(looseFen), Some(looseMove)).get
    val blocked = StoryTable.choose(Vector(story.copy(writer = None))).head
    val valuesBefore = blocked.values
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked).get

    assertEquals(blocked.role, Role.Blocked)
    assertEquals(blocked.leadAllowed, false)
    assertEquals(blocked.values, valuesBefore)
    assertEquals(diagnostic.story, "Tactic.Loose")
    assertEquals(diagnostic.leadAllowed, false)
    assertEquals(diagnostic.blockedBy.contains("writer"), true)
    assertEquals(diagnostic.boardFactsPresent.contains("story_identity"), true)
    assertEquals(diagnostic.proofCoordinates.root, Some("same-board story proof"))
    assertEquals(diagnostic.proofCoordinates.side, Some("White"))
    assertEquals(diagnostic.proofCoordinates.target, Some("h5"))
    assertEquals(diagnostic.proofCoordinates.anchor, Some("h2"))
    assertEquals(diagnostic.proofCoordinates.route, Some("d2-h2"))
    assertEquals(diagnostic.proofCoordinates.rival, Some("Black"))
    assertEquals(diagnostic.proofCoordinates.requiredLegalLine, Some("d2-h2"))
    assertEquals(diagnostic.proofCoordinates.sameRootProofSidecar, Some("StoryProof"))
    assertEquals(diagnostic.missingSidecar.contains("Story writer"), true)
    assertEquals(
      diagnostic.reason.toLowerCase(java.util.Locale.ROOT).contains("missing proof coordinates"),
      true
    )

  test("Stage-0 diagnostics also cover non-lead StoryTable rows without upgrading them"):
    val loose = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    val material = SceneMaterial
      .write(board("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"), Line(Square('d', 4), Square('e', 5)))
      .get
      .copy(proof = orderingProof)
    val looseVerdict = StoryTable.choose(Vector(material, loose)).find(_.story.tactic.contains(Tactic.Loose)).get
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(looseVerdict).get

    assertEquals(looseVerdict.role, Role.Support)
    assertEquals(looseVerdict.leadAllowed, false)
    assertEquals(diagnostic.leadAllowed, false)
    assertEquals(diagnostic.blockedBy.contains("non_lead_role"), true)
    assertEquals(diagnostic.missingSidecar, Vector.empty)
    assertEquals(ExplanationPlan.fromSelected(looseVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(looseVerdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Stage-0 diagnostics stay out of ExplanationPlan renderer LLM smoke and commentary routes"):
    val story = TacticLoose.write(board(looseFen), Some(looseMove)).get
    val blocked = StoryTable.choose(Vector(story.copy(writer = None))).head
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked)
    val controller = Files.readString(Paths.get("app/controllers/Commentary.scala"))

    assertEquals(diagnostic.nonEmpty, true)
    assertEquals(ExplanationPlan.fromSelected(blocked), None)
    assertEquals(ExplanationPlan.fromSelected(blocked).flatMap(DeterministicRenderer.fromPlan), None)
    assert(!controller.contains("ProofDeficitDiagnostics"))
    assert(!controller.contains("ProofDeficitDiagnostic"))
    assert(controller.contains("ServiceUnavailable(unavailable).toFuccess"))
    assert(!controller.contains("Ok("), "commentary route tombstones must not return 200")

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
