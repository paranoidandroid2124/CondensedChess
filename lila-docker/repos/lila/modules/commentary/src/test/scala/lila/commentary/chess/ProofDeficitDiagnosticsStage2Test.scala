package lila.commentary.chess

class ProofDeficitDiagnosticsStage2Test extends munit.FunSuite:

  test("Stage-2 reports each missing Story identity coordinate without filling it"):
    val rows =
      Vector(
        MissingCoordinate("side", baseStory.copy(side = Side.None), _.side),
        MissingCoordinate("rival", baseStory.copy(rival = Side.None), _.rival),
        MissingCoordinate("target", baseStory.copy(target = None), _.target),
        MissingCoordinate("anchor", baseStory.copy(anchor = None), _.anchor),
        MissingCoordinate("route", baseStory.copy(route = None), _.route)
      )

    rows.foreach: row =>
      val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked(row.story)).get

      assertEquals(row.coordinate(diagnostic.proofCoordinates), None, row.label)
      assertEquals(diagnostic.blockedBy.contains(row.label), true, row.label)
      assertEquals(diagnostic.missingSidecar.contains(s"Story identity ${row.label}"), true, row.label)
      assertEquals(diagnostic.boardFactsPresent.contains("story_identity"), false, row.label)

  test("Stage-2 reports missing legal line without inventing legal replay"):
    val mismatchedRoute = Line(Square('a', 1), Square('a', 2))
    val row = baseStory.copy(route = Some(mismatchedRoute))
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked(row)).get

    assertEquals(diagnostic.proofCoordinates.route, Some("a1-a2"))
    assertEquals(diagnostic.proofCoordinates.requiredLegalLine, None)
    assertEquals(diagnostic.proofCoordinates.sameRootProofSidecar, None)
    assertEquals(diagnostic.blockedBy.contains("legal_line"), true)
    assertEquals(diagnostic.missingSidecar.contains("StoryProof legal line"), true)

  test("Stage-2 reports missing same-root proof sidecar without claiming probably same board"):
    val row = baseStory.copy(storyProof = StoryProof.untrustedLegalLine(looseMove))
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked(row)).get

    assertEquals(diagnostic.proofCoordinates.route, Some("d2-h2"))
    assertEquals(diagnostic.proofCoordinates.requiredLegalLine, Some("d2-h2"))
    assertEquals(diagnostic.proofCoordinates.sameRootProofSidecar, None)
    assertEquals(diagnostic.blockedBy.contains("same_board_proof"), true)
    assertEquals(diagnostic.missingSidecar.contains("StoryProof same-board proof"), true)
    assertNoForbiddenDiagnosticWording(diagnostic.reason)

  test("Stage-2 reports missing family sidecar only as sidecar deficit"):
    val row = baseStory.copy(loosePieceProof = None)
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked(row)).get

    assertEquals(diagnostic.proofCoordinates.side, Some("White"))
    assertEquals(diagnostic.proofCoordinates.rival, Some("Black"))
    assertEquals(diagnostic.proofCoordinates.target, Some("h5"))
    assertEquals(diagnostic.proofCoordinates.anchor, Some("h2"))
    assertEquals(diagnostic.proofCoordinates.route, Some("d2-h2"))
    assertEquals(diagnostic.missingSidecar.contains("LoosePieceProof"), true)
    assertEquals(diagnostic.boardFactsPresent.contains("loose_piece_proof"), false)

  test("Stage-2 does not fill a missing route from raw engine PV"):
    val engineOnlyRoute =
      EngineCheck.fromEvidence(
        sameBoardProof = true,
        checkedMove = Some(looseMove),
        engineLine = Some(EngineLine(Vector(looseMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val row = baseStory.copy(route = None, engineCheck = Some(engineOnlyRoute))
    val diagnostic = ProofDeficitDiagnostics.fromVerdict(blocked(row)).get

    assertEquals(diagnostic.proofCoordinates.route, None)
    assertEquals(diagnostic.proofCoordinates.requiredLegalLine, None)
    assertEquals(diagnostic.boardFactsPresent.contains("engine_check"), true)
    assertEquals(diagnostic.blockedBy.contains("route"), true)
    assertEquals(diagnostic.missingSidecar.contains("Story identity route"), true)
    assertNoForbiddenDiagnosticWording(diagnostic.reason)

  private final case class MissingCoordinate(
      label: String,
      story: Story,
      coordinate: ProofCoordinates => Option[String]
  )

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))

  private def baseStory: Story =
    TacticLoose.write(board(looseFen), Some(looseMove)).get

  private def blocked(story: Story): Verdict =
    StoryTable.choose(Vector(story)).head

  private def board(fen: String): BoardFacts =
    BoardFacts.fromFen(fen).toOption.get

  private def assertNoForbiddenDiagnosticWording(text: String): Unit =
    val normalized = text.toLowerCase(java.util.Locale.ROOT)
    Vector(
      "likely target",
      "implied route",
      "probably same board",
      "engine confirms identity",
      "boardfacts proves claim",
      "source proves claim"
    ).foreach: phrase =>
      assertEquals(normalized.contains(phrase), false, phrase)
