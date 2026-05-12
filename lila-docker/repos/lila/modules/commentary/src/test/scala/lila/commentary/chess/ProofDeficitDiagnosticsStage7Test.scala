package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class ProofDeficitDiagnosticsStage7Test extends munit.FunSuite:

  test("Stage-7 closeout keeps diagnostics internal and non-authoritative"):
    val blockedTactic = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(loosePieceProof = None)
    val blockedFile = SceneFileOpened.write(board(fileOpenedFen), fileOpenedMove).get.copy(
      storyProof = StoryProof.untrustedLegalLine(fileOpenedMove)
    )
    val blockedCheck = SceneCheckGiven.write(board(checkGivenFen), checkGivenMove).get.copy(target = None)
    val refuted = SceneMaterial.withEngineCheck(materialStory, materialEngineCheck(EngineCheckStatus.Refutes)).get
    val support = supportVerdict

    val diagnostics =
      Vector(
        StoryTable.choose(Vector(blockedTactic)).head,
        StoryTable.choose(Vector(blockedFile)).head,
        StoryTable.choose(Vector(blockedCheck)).head,
        StoryTable.choose(Vector(refuted)).head,
        support
      ).map: verdict =>
        val diagnostic = verdict.proofDeficitDiagnostic.getOrElse(fail(s"${verdict.story.scene} missed diagnostic"))
        assertEquals(verdict.values.size, Verdict.Size)
        assertEquals(verdict.values.map(_.toString).mkString(" ").contains("ProofDeficitDiagnostic"), false)
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None)
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)
        diagnostic

    assertEquals(diagnostics.map(_.getClass).distinct.size, 1)
    assertEquals(diagnostics.map(_.proofCoordinates.getClass).distinct.size, 1)
    assert(diagnostics.exists(_.missingSidecar.contains("LoosePieceProof")))
    assert(diagnostics.exists(_.missingSidecar.contains("StoryProof same-board proof")))
    assert(diagnostics.exists(_.missingSidecar.contains("Story identity target")))
    assertEquals(diagnostics.exists(_.blockedBy.contains("engine_refutes")), true)
    assertEquals(support.role == Role.Support || support.role == Role.Context, true)

  test("Stage-7 closeout does not let diagnostics repair proof or change StoryTable order"):
    val validLead = materialStory.copy(proof = orderingProof)
    val missingTarget = SceneCheckGiven.write(board(checkGivenFen), checkGivenMove).get.copy(target = None)
    val rows = Vector(validLead, missingTarget)

    val before = StoryTable.choose(rows).map(v => (v.story.scene, v.role, v.leadAllowed))
    StoryTable.choose(rows).foreach(_.proofDeficitDiagnostic)
    val after = StoryTable.choose(rows).map(v => (v.story.scene, v.role, v.leadAllowed))
    val diagnostic = StoryTable.choose(Vector(missingTarget)).head.proofDeficitDiagnostic.get

    assertEquals(before, after)
    assertEquals(diagnostic.proofCoordinates.target, None)
    assertEquals(diagnostic.missingSidecar.contains("Story identity target"), true)
    assertEquals(diagnostic.blockedBy.contains("target"), true)

  test("Stage-7 closeout exposes diagnostics only through the internal Story accessor"):
    val chessSource = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val sources =
      Files.walk(chessSource).toArray.toVector.map(_.asInstanceOf[java.nio.file.Path]).filter(_.toString.endsWith(".scala"))

    val references =
      sources.flatMap: path =>
        Files.readAllLines(path).toArray.toVector.map(_.toString).zipWithIndex.collect:
          case (line, index) if line.contains("ProofDeficitDiagnostic") || line.contains("ProofDeficitDiagnostics") =>
            s"${path.getFileName}:$index:$line"

    assertEquals(
      references.map(_.takeWhile(_ != ':')).toSet,
      Set("ProofDeficitDiagnostics.scala", "Story.scala")
    )
    assert(references.exists(_.contains("private[commentary] final case class ProofDeficitDiagnostic")))
    assert(references.exists(_.contains("private[commentary] def proofDeficitDiagnostic")))
    assertEquals(references.exists(_.contains("ExplanationPlan")), false)
    assertEquals(references.exists(_.contains("DeterministicRenderer")), false)
    assertEquals(references.exists(_.contains("LlmNarrationSmoke")), false)
    assertEquals(references.exists(_.contains("Controller")), false)

  private val looseFen = "4k3/8/8/7b/8/8/3R4/4K3 w - - 0 1"
  private val looseMove = Line(Square('d', 2), Square('h', 2))
  private val fileOpenedFen = "4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1"
  private val fileOpenedMove = Line(Square('e', 5), Square('d', 6))
  private val checkGivenFen = "4k3/8/8/8/8/8/3R4/4K3 w - - 0 1"
  private val checkGivenMove = Line(Square('d', 2), Square('e', 2))
  private val materialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
  private val materialMove = Line(Square('d', 4), Square('e', 5))

  private def materialStory: Story =
    SceneMaterial.write(board(materialFen), materialMove).get

  private def supportVerdict: Verdict =
    val lead = materialStory.copy(proof = orderingProof)
    val support = TacticLoose.write(board(looseFen), Some(looseMove)).get.copy(proof = orderingProof)
    StoryTable.choose(Vector(lead, support)).find(_.story.tactic.contains(Tactic.Loose)).get

  private def materialEngineCheck(status: EngineCheckStatus): EngineCheck =
    EngineCheck.fromStory(
      facts = board(materialFen),
      story = Some(materialStory),
      engineLine = Some(EngineLine(Vector(materialMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(300)),
      evalAfter = Some(EngineEval(260)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = status
    )

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
