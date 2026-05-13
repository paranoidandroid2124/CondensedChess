package lila.commentary.chess

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class PawnsTest extends ChessTestSupport:

  test("PawnAdvance-0 opens only bounded passed pawn advance"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))

    val story = ScenePawnAdvance.write(facts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.route, Some(advance))
    assertEquals(story.routeSan, Some("e6"))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assert(story.pawnAdvanceProof.exists(_.complete))
    assertEquals(story.pawnAdvanceProof.exists(_.publicClaimAllowed), false)
    assertEquals(story.pawnAdvanceProof.exists(_.alreadyPassedBefore), true)
    assertEquals(story.pawnAdvanceProof.exists(_.afterBoardPassedPawn), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "e6 threatens promotion.",
      "e6 is a winning passed-pawn strategy.",
      "e6 is the best move.",
      "e6 is forced.",
      "e6 starts conversion.",
      "e6 creates a pawn race.",
      "e6 is unstoppable.",
      "e6 wins.",
      "e6 queens.",
      "e6 promotes next.",
      "e6 has a clear path.",
      "e6 cannot be stopped."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnAdvance-1 PawnAdvanceProof binds exact legal passed-pawn advance without public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val proof = PawnAdvanceProof.fromBoardFacts(facts, advance)

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 6))))
    assertEquals(proof.fromSquare, Some(Square('e', 5)))
    assertEquals(proof.toSquare, Some(Square('e', 6)))
    assertEquals(proof.advanceMove, Some(advance))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnAdvance, true)
    assertEquals(proof.nonCapture, true)
    assertEquals(proof.nonPromotion, true)
    assertEquals(proof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assert(
      facts.seen.passedPawnObservations.exists(row =>
        row.side == Side.White && row.pawn == proof.pawnBefore.get
      )
    )

  test("PawnAdvance-1 PawnAdvanceProof stays diagnostic for forged or converting moves"):
    val fakeAdvance = Line(Square('e', 5), Square('e', 6))
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(fakeAdvance),
          san = Vector("e6"),
          moveCount = 1
        )
      )
    val fakeProof = PawnAdvanceProof.fromBoardFacts(sameBoardProofMissing, fakeAdvance)

    assertEquals(fakeProof.sameBoardProof, false)
    assertEquals(fakeProof.exactAfterBoardReplay, false)
    assertEquals(fakeProof.afterBoardPassedPawn, false)
    assertEquals(fakeProof.publicClaimAllowed, false)
    assert(fakeProof.missingEvidence.exists(_.missing.contains("same-board proof")))
    assertEquals(ScenePawnAdvance.write(sameBoardProofMissing, fakeAdvance), None)

    val captureFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureProof = PawnAdvanceProof.fromBoardFacts(captureFacts, Line(Square('e', 5), Square('d', 6)))
    assertEquals(captureProof.nonCapture, false)
    assertEquals(captureProof.legalPawnAdvance, false)
    assertEquals(captureProof.exactAfterBoardReplay, false)
    assertEquals(captureProof.publicClaimAllowed, false)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionProof = PawnAdvanceProof.fromBoardFacts(promotionFacts, Line(Square('e', 7), Square('e', 8)))
    assertEquals(promotionProof.nonPromotion, false)
    assertEquals(promotionProof.exactAfterBoardReplay, false)
    assertEquals(promotionProof.publicClaimAllowed, false)

  test("PawnAdvance-2 ScenePawnAdvance writer pins bounded Story identity"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val proof = story.pawnAdvanceProof.get

    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.toSquare)
    assertEquals(story.anchor, proof.fromSquare)
    assertEquals(story.route, proof.advanceMove)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.pieceSupport, 0)

    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))

  test("PawnAdvance-2 ScenePawnAdvance writer rejects refuted or stronger forged rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val refutingCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(50)),
        evalAfter = Some(EngineEval(-200)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Refutes
      )
    val refuted = story.copy(engineCheck = Some(refutingCheck))
    val forgedPromotionThreat = story.copy(tactic = Some(Tactic.PawnFork))
    val forgedWinningConversion =
      story.copy(scene = Scene.Convert, proof = story.proof.copy(conversionPrize = 100))
    val forgedPlan = story.copy(plan = Some(Plan.Convert))

    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(refuted)).head), None)
    Vector(forgedPromotionThreat, forgedWinningConversion, forgedPlan).foreach: forged =>
      val maybeVerdict = StoryTable.choose(Vector(forged)).headOption
      forged.tactic match
        case Some(Tactic.PawnFork) =>
          assertEquals(maybeVerdict, None)
        case _ =>
          val verdict = maybeVerdict.get
          assertEquals(verdict.role, Role.Blocked)
          assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnAdvance-0 rejects near passed pawn advance false positives"):
    val notPassedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val doubleAdvanceFacts = BoardFacts.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1").toOption.get
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(Line(Square('e', 5), Square('e', 6))),
          san = Vector("e6"),
          moveCount = 1
        )
      )

    assertEquals(
      ScenePawnAdvance.write(notPassedFacts, Line(Square('e', 5), Square('e', 6))),
      None,
      "not already passed"
    )
    assertEquals(ScenePawnAdvance.write(captureFacts, Line(Square('e', 5), Square('d', 6))), None, "capture")
    assertEquals(
      ScenePawnAdvance.write(promotionFacts, Line(Square('e', 7), Square('e', 8))),
      None,
      "promotion"
    )
    assertEquals(
      ScenePawnAdvance.write(doubleAdvanceFacts, Line(Square('e', 2), Square('e', 4))),
      None,
      "double advance"
    )
    assertEquals(
      ScenePawnAdvance.write(sameBoardProofMissing, Line(Square('e', 5), Square('e', 6))),
      None,
      "same-board proof"
    )

  test("PawnAdvance-3 negative corpus requires complete PawnAdvanceProof or silence"):
    val legalAdvanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val legalAdvance = Line(Square('e', 5), Square('e', 6))
    val illegalLine = Line(Square('e', 5), Square('e', 7))
    val rookMoveFacts = BoardFacts.fromFen("k7/8/8/4R3/8/8/8/7K w - - 0 1").toOption.get
    val rookMove = Line(Square('e', 5), Square('e', 6))
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val enPassantCapture = Line(Square('e', 5), Square('d', 6))

    val illegalProof = PawnAdvanceProof.fromBoardFacts(legalAdvanceFacts, illegalLine)
    assertEquals(illegalProof.legalPawnAdvance, false)
    assertEquals(illegalProof.complete, false)
    assertEquals(ScenePawnAdvance.write(legalAdvanceFacts, illegalLine), None, "not a legal move")

    val rookProof = PawnAdvanceProof.fromBoardFacts(rookMoveFacts, rookMove)
    assertEquals(rookProof.pawnBefore, None)
    assertEquals(rookProof.complete, false)
    assertEquals(ScenePawnAdvance.write(rookMoveFacts, rookMove), None, "not a pawn")

    val enPassantProof = PawnAdvanceProof.fromBoardFacts(enPassantFacts, enPassantCapture)
    assertEquals(enPassantProof.nonCapture, false)
    assertEquals(enPassantProof.legalOneStepNonCaptureNonPromotion, false)
    assertEquals(
      ScenePawnAdvance.write(enPassantFacts, enPassantCapture),
      None,
      "en passant is capture complexity"
    )

    val story = ScenePawnAdvance.write(legalAdvanceFacts, legalAdvance).get
    val afterNotPassed =
      story.copy(pawnAdvanceProof =
        story.pawnAdvanceProof.map(_.copy(afterBoardPassedPawn = false, missingEvidence = Vector.empty))
      )
    val promotionThreatForgery = story.copy(tactic = Some(Tactic.PawnPush))
    Vector(afterNotPassed, promotionThreatForgery).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnAdvance-3 closes immediate promotion and stronger wording expansion"):
    val nearPromotionFacts = BoardFacts.fromFen("4k3/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 6), Square('e', 7))
    val story = ScenePawnAdvance.write(nearPromotionFacts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    Vector(
      "e7 promotes next.",
      "e7 is unstoppable.",
      "e7 is winning.",
      "e7 starts conversion.",
      "e7 creates a promotion threat."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnAdvance-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val reply = Line(Square('e', 8), Square('e', 7))

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noStandaloneEngineText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePawnAdvance.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 5), Square('e', 7))))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict)
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict)

    assertEquals(
      ScenePawnAdvance.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePawnAdvance.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PawnAdvance")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan, Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan, Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noStandaloneEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says e6 is best",
      "+3.2 and winning endgame",
      "only move for a tablebase-like win",
      "best move",
      "only move",
      "winning endgame"
    ).foreach: phrase =>
      assertEquals(
        LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)),
        None,
        phrase
      )

  test("PawnAdvance-5 StoryTable keeps existing tactical material and defense homes ahead of PawnAdvance"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
          verdict.engineStrengthLimited
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val pawnFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = ScenePawnAdvance.write(pawnFacts, pawnMove).get.copy(proof = strongProof(100))

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialLine).get.copy(proof = strongProof(90))
    val material = SceneMaterial.write(materialFacts, materialLine).get.copy(proof = strongProof(90))

    val forkFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork
        .write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5)))
        .get
        .copy(proof = strongProof(90))

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get.copy(proof = strongProof(90))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(90))

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin
        .write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get
        .copy(proof = strongProof(90))

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = strongProof(90))

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(
          skewerFacts,
          Some(skewerLine),
          Some(Square('e', 1)),
          Some(Square('e', 5)),
          Some(Square('e', 8))
        )
        .get
        .copy(proof = strongProof(90))

    val existingRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered,
        "Tactic.Pin" -> pin,
        "Tactic.RemoveGuard" -> removeGuard,
        "Tactic.Skewer" -> skewer
      )
    val mixedRows = existingRows :+ ("Scene.PawnAdvance" -> pawnAdvance)
    val forward = StoryTable.choose(mixedRows.map(_._2))
    val reverse = StoryTable.choose(mixedRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          mixedRows(8),
          mixedRows(2),
          mixedRows(5),
          mixedRows(0),
          mixedRows(7),
          mixedRows(3),
          mixedRows(1),
          mixedRows(6),
          mixedRows(4)
        ).map(_._2)
      )

    assertEquals(shape(mixedRows, forward), shape(mixedRows, reverse))
    assertEquals(shape(mixedRows, forward), shape(mixedRows, shuffled))
    assert(rowId(mixedRows, forward.head.story) != "Scene.PawnAdvance")
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assert(!forward.exists(verdict => verdict.story == pawnAdvance && verdict.role == Role.Lead))

    existingRows.foreach: (label, row) =>
      val verdicts = StoryTable.choose(Vector(pawnAdvance, row))
      val existingVerdict = verdicts.find(_.story == row).get
      val pawnVerdict = verdicts.find(_.story == pawnAdvance).get

      assertEquals(existingVerdict.role, Role.Lead, label)
      assertEquals(existingVerdict.leadAllowed, true, label)
      assertEquals(pawnVerdict.role, Role.Support, label)
      assertEquals(pawnVerdict.leadAllowed, false, label)
      val existingPlan = ExplanationPlan.fromSelected(existingVerdict).get
      val existingRendered = DeterministicRenderer.fromPlan(existingPlan).get
      assertEquals(ExplanationPlan.fromSelected(pawnVerdict), None, label)
      assertEquals(existingPlan.allowedClaim.exists(_ == ExplanationClaim.AdvancesPassedPawn), false, label)
      assert(!existingRendered.text.toLowerCase.contains("passed pawn"), label)

    val materialCollision = StoryTable.choose(Vector(pawnAdvance, material))
    val materialLead = materialCollision.find(_.story == material).get
    val pawnWithMaterial = materialCollision.find(_.story == pawnAdvance).get
    assertEquals(materialLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(materialLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(pawnWithMaterial.role, Role.Support)

    val immediateTacticRows = Vector(hanging, fork, discovered, pin, removeGuard, skewer)
    immediateTacticRows.foreach: tacticRow =>
      val verdicts = StoryTable.choose(Vector(pawnAdvance, tacticRow))
      val tacticVerdict = verdicts.find(_.story == tacticRow).get
      val pawnVerdict = verdicts.find(_.story == pawnAdvance).get
      assertEquals(tacticVerdict.role, Role.Lead, tacticRow.toString)
      assertEquals(tacticVerdict.story.scene, Scene.Tactic, tacticRow.toString)
      assertEquals(pawnVerdict.role, Role.Support, tacticRow.toString)
      assertNoStandaloneText("PawnAdvance support under immediate tactic", pawnVerdict)

    def pawnCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = pawnFacts,
        story = Some(pawnAdvance),
        engineLine = Some(EngineLine(Vector(pawnMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val cappedPawn = ScenePawnAdvance.withEngineCheck(pawnAdvance, pawnCheck(EngineCheckStatus.Caps)).get
    val refutedPawn = ScenePawnAdvance.withEngineCheck(pawnAdvance, pawnCheck(EngineCheckStatus.Refutes)).get
    Vector("capped" -> cappedPawn, "refuted" -> refutedPawn).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnAdvance-6 ExplanationPlan accepts only selected uncapped PawnAdvance Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val leadVerdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(leadVerdict).get

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadVerdict.leadAllowed, true)
    assertEquals(leadVerdict.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.PawnAdvance)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("advances_passed_pawn"))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))

    val forbiddenClaimKeys =
      Vector(
        "promotion_threat",
        "unstoppable_pawn",
        "wins_endgame",
        "converts_advantage",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative"
      )
    forbiddenClaimKeys.foreach: key =>
      assert(
        ExplanationClaim.PawnAdvanceForbiddenKeys.contains(key),
        s"PawnAdvance missing forbidden claim key: $key"
      )
      assert(
        plan.forbiddenWording.map(_.key).contains(key),
        s"PawnAdvance plan missing forbidden wording key: $key"
      )
    assertEquals(
      ExplanationClaim.PawnAdvanceAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.PawnAdvanceForbiddenKeys.toSet),
      Set.empty[String]
    )

    def pawnCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(advance))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict =
      leadVerdict.copy(
        role = Role.Blocked,
        leadAllowed = false,
        engineCheckStatus = Some(EngineCheckStatus.Refutes)
      )
    val unselectedVerdict = leadVerdict.copy(selected = false)
    val cappedVerdict =
      StoryTable
        .choose(Vector(ScenePawnAdvance.withEngineCheck(story, pawnCheck(EngineCheckStatus.Caps)).get))
        .head
    val refutedVerdict =
      StoryTable
        .choose(Vector(ScenePawnAdvance.withEngineCheck(story, pawnCheck(EngineCheckStatus.Refutes)).get))
        .head

    Vector(
      "Support" -> supportVerdict,
      "Context" -> contextVerdict,
      "Blocked" -> blockedVerdict,
      "unselected" -> unselectedVerdict,
      "capped" -> cappedVerdict,
      "refuted" -> refutedVerdict
    ).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PawnAdvance-7 DeterministicRenderer phrases only bounded passed-pawn advance"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    assertEquals(
      fromPlanMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "cannot be stopped" -> "e6 cannot be stopped",
      "will promote" -> "e6 will promote",
      "wins" -> "e6 wins",
      "winning endgame" -> "e6 is a winning endgame",
      "converts" -> "e6 converts",
      "best move" -> "e6 is the best move",
      "only move" -> "e6 is the only move",
      "forces" -> "e6 forces",
      "decisive" -> "e6 is decisive",
      "creates pressure" -> "e6 creates pressure"
    ).foreach: (label, forgedRouteSan) =>
      assertEquals(DeterministicRenderer.fromPlan(plan.copy(routeSan = Some(forgedRouteSan))), None, label)

  test("PawnAdvance-8 LLM smoke reuses 8B prompt boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val story = ScenePawnAdvance.write(facts, advance).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: e6 advances the passed pawn.",
      "claimKey: advances_passed_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PawnAdvance prompt must include allowed field: $required")
    Vector(
      "Story",
      "PawnAdvanceProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "FEN",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"PawnAdvance prompt must not include raw input label: $forbidden")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("e6 advances the passed pawn."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "e6 advances the passed pawn.").accepted, true)

    Vector(
      "raw Story" -> "The raw Story says e6 advances the passed pawn.",
      "raw PawnAdvanceProof" -> "PawnAdvanceProof says e6 advances the passed pawn.",
      "BoardFacts" -> "BoardFacts show e6 advances the passed pawn.",
      "EngineCheck" -> "EngineCheck supports e6.",
      "raw PV" -> "raw PV e6 Ke7 proves it.",
      "proofFailures" -> "proofFailures are empty, so e6 is right."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input"), s"$label must be rejected as raw input leak: $result")

    Vector(
      "new move" -> "e6 and then e7 advances again.",
      "new line" -> "After e6 Ke7, the pawn keeps going.",
      "promotion" -> "e6 will promote.",
      "unstoppable" -> "e6 cannot be stopped.",
      "winning" -> "e6 wins the endgame.",
      "conversion" -> "e6 converts the advantage."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "new_move_or_line" || v == "forbidden_wording" || v == "new_tactic_or_plan" || v == "stronger_claim"
        ),
        s"$label must be rejected as new chess fact or stronger claim: $result"
      )

  test("PawnAdvance Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advance = Line(Square('e', 5), Square('e', 6))
    val observation =
      facts.seen.passedPawnObservations
        .find(row => row.side == Side.White && row.pawn.square == Square('e', 5))
        .get
    val proof = PawnAdvanceProof.fromBoardFacts(facts, advance)
    val story = ScenePawnAdvance.write(facts, advance).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(observation.pawn.man, Man.Pawn)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.alreadyPassedBefore, true)
    assertEquals(proof.afterBoardPassedPawn, true)
    assertEquals(story.pawnAdvanceProof.contains(proof), true)
    assertEquals(story.scene, Scene.PawnAdvance)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.anchor, proof.fromSquare)
    assertEquals(story.target, proof.toSquare)
    assertEquals(story.route, proof.advanceMove)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))
    assertEquals(rendered.text, "e6 advances the passed pawn.")
    assertEquals(rendered.claimKey, "advances_passed_pawn")
    assert(!rendered.text.toLowerCase.contains("promot"))
    assert(!rendered.text.toLowerCase.contains("unstoppable"))
    assert(!rendered.text.toLowerCase.contains("conversion"))
    assert(!rendered.text.toLowerCase.contains("clear path"))
    assert(!rendered.text.toLowerCase.contains("strategy"))
    assert(!rendered.text.toLowerCase.contains("wins"))

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "promotion_threat",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "pawn_race",
      "passed_pawn_strategy",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"PawnAdvance closeout must forbid $key")

    Vector(
      "e6 will promote.",
      "e6 cannot be stopped.",
      "e6 converts the advantage.",
      "e6 has a clear path.",
      "e6 is the passed pawn strategy.",
      "e6 creates pressure.",
      "e6 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-0 opens only bounded immediate passed pawn next-square stop"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))

    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('g', 7)))
    assertEquals(story.route, Some(stop))
    assertEquals(story.routeSan, Some("Ne6"))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assert(story.pawnStopProof.exists(_.complete))
    assertEquals(story.pawnStopProof.exists(_.publicClaimAllowed), false)
    assertEquals(story.pawnStopProof.exists(_.targetPawnAlreadyPassed), true)
    assertEquals(story.pawnStopProof.exists(_.nextAdvanceSquareStoppedAfter), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("stops_pawn_advance"))
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "Ne6 stops promotion.",
      "Ne6 permanently stops the pawn.",
      "Ne6 draws the tablebase.",
      "Ne6 is the best defense.",
      "Ne6 is the only move.",
      "Ne6 wins the endgame.",
      "Ne6 stops conversion.",
      "Ne6 wins the pawn race.",
      "Ne6 uses the king route.",
      "Ne6 has the opposition.",
      "Ne6 is passed pawn strategy."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-1 PawnStopProof binds exact legal move to already-passed pawn next square"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val proof = PawnStopProof.fromBoardFacts(facts, stop)

    assertEquals(proof.side, Side.Black)
    assertEquals(proof.rivalSide, Side.White)
    assertEquals(proof.targetPawn, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(proof.stoppingPieceBefore, Some(Piece(Side.Black, Man.Knight, Square('g', 7))))
    assertEquals(proof.stoppingPieceAfter, Some(Piece(Side.Black, Man.Knight, Square('e', 6))))
    assertEquals(proof.stopMove, Some(stop))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawnAlreadyPassed, true)
    assertEquals(proof.nextAdvanceSquareNonPromotion, true)
    assertEquals(proof.nextAdvanceSquareEmptyBefore, true)
    assertEquals(proof.stopKind, Some(PawnStopKind.NextSquareOccupied))
    assertEquals(proof.nextAdvanceSquareOccupiedAfter, true)
    assertEquals(proof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(proof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.targetPawnStillPresentAfter, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assert(
      facts.seen.passedPawnObservations.exists(row =>
        row.side == Side.White && row.pawn == proof.targetPawn.get
      )
    )
    assertEquals(
      PawnStopKind.values.toVector,
      Vector(
        PawnStopKind.NextSquareOccupied,
        PawnStopKind.NextSquareAttacked,
        PawnStopKind.NextSquareControlledByPawn
      )
    )

  test("PawnStop-1 PawnStopProof admits direct next-square attack but not pre-existing blockade"):
    val attackedFacts = BoardFacts.fromFen("k3r3/8/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val attackStop = Line(Square('e', 8), Square('e', 7))
    val attackedProof = PawnStopProof.fromBoardFacts(attackedFacts, attackStop)

    assertEquals(attackedProof.targetPawn, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(attackedProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(attackedProof.stoppingPieceAfter, Some(Piece(Side.Black, Man.Rook, Square('e', 7))))
    assertEquals(attackedProof.stopKind, Some(PawnStopKind.NextSquareAttacked))
    assertEquals(attackedProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(attackedProof.nextAdvanceSquareAttackedAfter, true)
    assertEquals(attackedProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(attackedProof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(attackedProof.complete, true)
    assert(ScenePawnStop.write(attackedFacts, attackStop).nonEmpty)

    val preBlockedFacts = BoardFacts.fromFen("4k3/8/4n3/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val unrelatedMove = Line(Square('e', 8), Square('d', 8))
    val preBlockedProof = PawnStopProof.fromBoardFacts(preBlockedFacts, unrelatedMove)
    assertEquals(preBlockedProof.nextAdvanceSquareEmptyBefore, false)
    assertEquals(preBlockedProof.stopKind, None)
    assertEquals(preBlockedProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(ScenePawnStop.write(preBlockedFacts, unrelatedMove), None)

  test("PawnStop-2 ScenePawnStop writer binds only bounded pawn-stop Story identity"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val proof = story.pawnStopProof.get

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('g', 7)))
    assertEquals(story.route, Some(stop))
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawn.nonEmpty, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(
      proof.nextAdvanceSquareOccupiedAfter ||
        proof.nextAdvanceSquareAttackedAfter ||
        proof.nextAdvanceSquareControlledByPawnAfter,
      true
    )

    val supportedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(stop))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
      evalBefore = Some(EngineEval(0)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    assertEquals(ScenePawnStop.withEngineCheck(story, supportedCheck).nonEmpty, true)

    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(stop))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
      evalBefore = Some(EngineEval(200)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    val refutedStory = ScenePawnStop.withEngineCheck(story, refutingCheck).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val forgedAnchor = story.copy(anchor = proof.targetPawn.map(_.square))
    val forgedDefense = story.copy(scene = Scene.Defense)
    val forgedPromotion = story.copy(tactic = Some(Tactic.Promote))
    val forgedEndgame = story.copy(scene = Scene.Endgame)

    Vector(forgedAnchor, forgedDefense, forgedPromotion, forgedEndgame).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None)

  test("PawnStop-3 negative corpus requires complete PawnStopProof or silence"):
    val positiveFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val notPassedFacts = BoardFacts.fromFen("4k3/6n1/3p4/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val promotionStopFacts = BoardFacts.fromFen("3k4/4P3/8/8/8/8/8/4K3 b - - 0 1").toOption.get
    val illegalMove = Line(Square('g', 7), Square('g', 8))
    val safeNextAdvanceMove = Line(Square('g', 7), Square('f', 5))
    val sameBoardProofMissing =
      minimalBoardFacts(
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Knight, Square('g', 7))
        ),
        sideLegal = Moves(
          known = true,
          lines = Vector(Line(Square('g', 7), Square('e', 6))),
          san = Vector("Ne6"),
          moveCount = 1
        )
      )

    val illegalProof = PawnStopProof.fromBoardFacts(positiveFacts, illegalMove)
    assertEquals(illegalProof.legalStopMove, false)
    assertEquals(illegalProof.complete, false)
    assertEquals(ScenePawnStop.write(positiveFacts, illegalMove), None, "legal move absent")

    assertEquals(
      ScenePawnStop.write(notPassedFacts, Line(Square('g', 7), Square('e', 6))),
      None,
      "not already passed"
    )
    assertEquals(
      ScenePawnStop.write(promotionStopFacts, Line(Square('d', 8), Square('e', 8))),
      None,
      "promotion stop"
    )
    assertEquals(
      ScenePawnStop.write(sameBoardProofMissing, Line(Square('g', 7), Square('e', 6))),
      None,
      "same-board proof"
    )

    val safeNextAdvanceProof = PawnStopProof.fromBoardFacts(positiveFacts, safeNextAdvanceMove)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(
      ScenePawnStop.write(positiveFacts, safeNextAdvanceMove),
      None,
      "next advance square remains empty and safe"
    )

    val story = ScenePawnStop.write(positiveFacts, Line(Square('g', 7), Square('e', 6))).get
    val forgedEndgameDefense = story.copy(scene = Scene.Endgame)
    val forgedPlan = story.copy(plan = Some(Plan.PasserBlock))
    val missingNextSquare =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(nextAdvanceSquare = None, missingEvidence = Vector.empty))
      )
    val incompleteStop =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty))
      )
    val missingStopKind =
      story.copy(pawnStopProof =
        story.pawnStopProof.map(_.copy(stopKind = None, missingEvidence = Vector.empty))
      )
    Vector(forgedEndgameDefense, forgedPlan, missingNextSquare, incompleteStop, missingStopKind).foreach:
      forged =>
        val verdict = StoryTable.choose(Vector(forged)).head
        assertEquals(verdict.role, Role.Blocked)
        assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PawnStop-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val reply = Line(Square('e', 1), Square('e', 2))

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(stop))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noStandaloneEngineText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      val rendered = plan.flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best defense")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("tablebase")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("losing endgame")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePawnStop.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('g', 7), Square('f', 5))))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict)
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict)

    assertEquals(
      ScenePawnStop.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePawnStop.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PawnStop")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan, Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan, Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noStandaloneEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says Ne6 is best defense",
      "+3.2 and winning endgame",
      "only move for a tablebase draw",
      "best defense",
      "only move",
      "tablebase draw",
      "winning endgame",
      "losing endgame"
    ).foreach: phrase =>
      assertEquals(
        LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)),
        None,
        phrase
      )

  test("PawnStop-5 StoryTable keeps existing claim homes and same-pawn PawnAdvance ahead of PawnStop"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
          verdict.engineStrengthLimited
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val pawnStopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val pawnStopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = ScenePawnStop.write(pawnStopFacts, pawnStopMove).get.copy(proof = strongProof(100))

    val pawnAdvanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnAdvanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      ScenePawnAdvance.write(pawnAdvanceFacts, pawnAdvanceMove).get.copy(proof = strongProof(90))

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialLine).get.copy(proof = strongProof(90))
    val material = SceneMaterial.write(materialFacts, materialLine).get.copy(proof = strongProof(90))

    val forkFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork
        .write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5)))
        .get
        .copy(proof = strongProof(90))

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get.copy(proof = strongProof(90))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(90))

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin
        .write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get
        .copy(proof = strongProof(90))

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get
        .copy(proof = strongProof(90))

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(
          skewerFacts,
          Some(skewerLine),
          Some(Square('e', 1)),
          Some(Square('e', 5)),
          Some(Square('e', 8))
        )
        .get
        .copy(proof = strongProof(90))

    val existingRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered,
        "Tactic.Pin" -> pin,
        "Tactic.RemoveGuard" -> removeGuard,
        "Tactic.Skewer" -> skewer
      )
    val mixedRows = existingRows :+ ("Scene.PawnAdvance" -> pawnAdvance) :+ ("Scene.PawnStop" -> pawnStop)
    val forward = StoryTable.choose(mixedRows.map(_._2))
    val reverse = StoryTable.choose(mixedRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          mixedRows(9),
          mixedRows(2),
          mixedRows(8),
          mixedRows(5),
          mixedRows(0),
          mixedRows(7),
          mixedRows(3),
          mixedRows(1),
          mixedRows(6),
          mixedRows(4)
        ).map(_._2)
      )

    assertEquals(shape(mixedRows, forward), shape(mixedRows, reverse))
    assertEquals(shape(mixedRows, forward), shape(mixedRows, shuffled))
    assert(rowId(mixedRows, forward.head.story) != "Scene.PawnStop")
    assertEquals(forward.count(_.role == Role.Lead), 1)

    existingRows.foreach: (label, row) =>
      val verdicts = StoryTable.choose(Vector(pawnStop, row))
      val existingVerdict = verdicts.find(_.story == row).get
      val pawnStopVerdict = verdicts.find(_.story == pawnStop).get

      assertEquals(existingVerdict.role, Role.Lead, label)
      assertEquals(existingVerdict.leadAllowed, true, label)
      assertEquals(pawnStopVerdict.role, Role.Support, label)
      assertEquals(pawnStopVerdict.leadAllowed, false, label)
      val existingPlan = ExplanationPlan.fromSelected(existingVerdict).get
      val existingRendered = DeterministicRenderer.fromPlan(existingPlan).get
      assertEquals(ExplanationPlan.fromSelected(pawnStopVerdict), None, label)
      assertEquals(
        existingPlan.allowedClaim.exists(_ == ExplanationClaim.StopsPassedPawnNextAdvance),
        false,
        label
      )
      assertEquals(existingPlan.allowedClaim.map(_.key).contains("threatens_promotion_next"), false, label)
      assert(!existingRendered.text.toLowerCase.contains("passed pawn"), label)

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseLead = defenseCollision.find(_.story == defense).get
    val pawnStopWithDefense = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(defenseLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.DefendsPiece)
    )
    assertEquals(pawnStopWithDefense.role, Role.Support)

    val materialCollision = StoryTable.choose(Vector(pawnStop, material))
    val materialLead = materialCollision.find(_.story == material).get
    val pawnStopWithMaterial = materialCollision.find(_.story == pawnStop).get
    assertEquals(materialLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(materialLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(pawnStopWithMaterial.role, Role.Support)

    val samePawnRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Scene.PawnStop" -> pawnStop)
    val samePawnForward = StoryTable.choose(samePawnRows.map(_._2))
    val samePawnReverse = StoryTable.choose(samePawnRows.reverse.map(_._2))
    val advanceVerdict = samePawnForward.find(_.story == pawnAdvance).get
    val stopVerdict = samePawnForward.find(_.story == pawnStop).get
    assertEquals(shape(samePawnRows, samePawnForward), shape(samePawnRows, samePawnReverse))
    assertEquals(advanceVerdict.role, Role.Lead)
    assertEquals(advanceVerdict.leadAllowed, true)
    assertEquals(stopVerdict.role, Role.Support)
    assertEquals(stopVerdict.leadAllowed, false)
    assertNoStandaloneText("PawnStop support under same-pawn PawnAdvance", stopVerdict)

    def pawnStopCheck(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = pawnStopFacts,
        story = Some(pawnStop),
        engineLine = Some(EngineLine(Vector(pawnStopMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('e', 2))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val cappedPawnStop = ScenePawnStop.withEngineCheck(pawnStop, pawnStopCheck(EngineCheckStatus.Caps)).get
    val refutedPawnStop =
      ScenePawnStop.withEngineCheck(pawnStop, pawnStopCheck(EngineCheckStatus.Refutes)).get
    Vector("capped" -> cappedPawnStop, "refuted" -> refutedPawnStop).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnStop-6 ExplanationPlan accepts only selected uncapped PawnStop Lead"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(plan.scene, Scene.PawnStop)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(plan.allowedClaim.map(_.key), Some("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnStopForbiddenKeys.toSet.contains("stops_pawn_advance"), false)
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assert(!rendered.text.toLowerCase.contains("promotion"))
    assert(!rendered.text.toLowerCase.contains("permanent"))
    assert(!rendered.text.toLowerCase.contains("endgame"))
    assert(!rendered.text.toLowerCase.contains("best"))
    assert(!rendered.text.toLowerCase.contains("only"))
    assert(!rendered.text.toLowerCase.contains("draw"))
    assert(!rendered.text.toLowerCase.contains("strategy"))

    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(ExplanationClaim.PawnStopForbiddenKeys.contains(key), s"PawnStop must forbid $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"PawnStop plan must forbid $key")

    Vector(
      "stops_passed_pawn_next_advance",
      "promotion_stop",
      "permanent_stop",
      "conversion_stopped"
    ).foreach: key =>
      assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key).contains(key), false, key)

    Vector(
      "support" -> verdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> verdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false),
      "unselected" -> verdict.copy(selected = false),
      "capped" -> verdict.copy(
        engineStrengthLimited = true,
        engineCheckStatus = Some(EngineCheckStatus.Caps)
      ),
      "refuted" -> verdict.copy(
        role = Role.Blocked,
        leadAllowed = false,
        engineCheckStatus = Some(EngineCheckStatus.Refutes)
      )
    ).foreach: (label, row) =>
      assertEquals(ExplanationPlan.fromSelected(row), None, label)

    Vector(
      "promotion_stop",
      "permanent_stop",
      "conversion_stopped",
      "pawn_race",
      "king_route",
      "opposition",
      "passed_pawn_strategy"
    ).foreach: key =>
      assertEquals(ExplanationClaim.PawnStopForbiddenKeys.contains(key), false, key)

    Vector(
      "Ne6 stops promotion.",
      "Ne6 stops the promotion threat.",
      "Ne6 creates a tablebase draw.",
      "Ne6 draws the position.",
      "Ne6 reaches king opposition.",
      "Ne6 permanently stops the pawn.",
      "Ne6 means the pawn cannot advance.",
      "Ne6 is the only move.",
      "Engine says Ne6 stops the passed pawn's next advance."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnStop-7 DeterministicRenderer phrases only bounded next-advance stop"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "stops promotion",
      "stops the pawn for good",
      "draws",
      "holds the endgame",
      "best defense",
      "only move",
      "forces",
      "wins",
      "tablebase"
    ).foreach: forbidden =>
      val forged = plan.copy(routeSan = Some(forbidden))
      assertEquals(DeterministicRenderer.fromPlan(forged), None, forbidden)

    val methodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(methodNames.contains("fromPlan"))
    Vector("fromVerdict", "fromStory", "fromBoardFacts", "fromPawnStopProof", "fromEngineCheck").foreach:
      method => assert(!methodNames.contains(method), s"renderer must not expose $method")
    Vector("Verdict", "Story", "BoardFacts", "PawnStopProof", "EngineCheck").foreach: parameter =>
      assert(!parameterNames.contains(parameter), s"renderer must not accept $parameter")

  test("PawnStop-8 LLM smoke reuses 8B prompt boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: Ne6 stops the passed pawn from advancing next.",
      "claimKey: stops_pawn_advance",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PawnStop prompt must include allowed field: $required")
    Vector(
      "Story",
      "PawnStopProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "FEN",
      "PGN",
      "BoardMood",
      "Verdict",
      "raw Story",
      "source row",
      "role:",
      "scene:",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"PawnStop prompt must not include raw input label: $forbidden")

    assertEquals(
      LlmNarrationSmoke.mockNarrate(plan, rendered),
      Some("Ne6 stops the passed pawn from advancing next.")
    )
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, "Ne6 stops the passed pawn from advancing next.").accepted,
      true
    )

    Vector(
      "raw Story" -> "Raw Story says Ne6 stops the pawn.",
      "raw PawnStopProof" -> "PawnStopProof says Ne6 stops the passed pawn.",
      "BoardFacts" -> "BoardFacts show e6 is stopped.",
      "EngineCheck" -> "EngineCheck supports Ne6.",
      "raw PV" -> "Raw PV: Ne6 e2.",
      "proofFailures" -> "proofFailures are empty, so Ne6 works."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input"), s"$label must be rejected as raw input leak: $result")

    Vector(
      "new move" -> "Nf5 also stops the passed pawn.",
      "new line" -> "After Ne6 Kd7 the pawn is stopped.",
      "promotion" -> "Ne6 stops promotion.",
      "permanent stop" -> "Ne6 stops the pawn for good.",
      "draw" -> "Ne6 draws the endgame.",
      "tablebase" -> "Ne6 is a tablebase draw.",
      "winning" -> "Ne6 wins the endgame."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(
        result.violations.exists(v =>
          v == "new_move_or_line" || v == "forbidden_wording" || v == "new_tactic_or_plan" || v == "stronger_claim"
        ),
        s"$label must be rejected as new chess fact or stronger claim: $result"
      )

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "stops_promotion")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromPawnStopProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"LLM smoke must not expose $method")
    Vector("Story", "PawnStopProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach:
      parameter => assert(!parameterNames.contains(parameter), s"LLM smoke must not accept $parameter")

  test("PawnBreak-0 opens only one direct pawn lever after a legal pawn move"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val proof = PawnBreakProof.fromBoardFacts(facts, move)
    val story = ScenePawnBreak.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 4))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.targetPawn, Some(Piece(Side.Black, Man.Pawn, Square('d', 6))))
    assertEquals(proof.breakMove, Some(move))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonCapturingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.directPawnLeverAfterMove, true)
    assertEquals(proof.singleRivalPawnTarget, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(story.scene, Scene.PawnBreak)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBreak))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('e', 4)))
    assertEquals(story.target, Some(Square('d', 6)))
    assertEquals(story.route, Some(move))
    assertEquals(story.routeSan, Some("e5"))
    assertEquals(story.pawnBreakProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.scene, Scene.PawnBreak)
    assertEquals(plan.allowedClaim.map(_.key), Some("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(rendered.text, "e5 challenges the pawn on d6.")
    assertEquals(rendered.claimKey, "challenges_pawn")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "e5 opens the position.",
      "e5 breaks through.",
      "e5 creates a passed pawn.",
      "e5 weakens the structure.",
      "e5 wins space.",
      "e5 creates pressure.",
      "e5 takes the initiative.",
      "e5 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnBreak-1 PawnBreakProof owns only diagnostic direct pawn contact evidence"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val proof = PawnBreakProof.fromBoardFacts(facts, move)
    val story = ScenePawnBreak.write(facts, move).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 4))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.originSquare, Some(Square('e', 4)))
    assertEquals(proof.destinationSquare, Some(Square('e', 5)))
    assertEquals(proof.fromSquare, proof.originSquare)
    assertEquals(proof.toSquare, proof.destinationSquare)
    assertEquals(proof.breakMove, Some(move))
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionMove, true)
    assertEquals(proof.targetPawn, Some(Piece(Side.Black, Man.Pawn, Square('d', 6))))
    assertEquals(proof.directPawnLeverAfterMove, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(
      proof.contactKinds,
      Vector(PawnBreakContactKind.PawnChallengesPawn, PawnBreakContactKind.PawnLeverCreated)
    )
    assertEquals(
      PawnBreakContactKind.values.toVector.map(_.toString),
      Vector("PawnChallengesPawn", "PawnLeverCreated")
    )
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(classOf[Story].isAssignableFrom(classOf[PawnBreakProof]), false)
    assertEquals(classOf[Story].isAssignableFrom(classOf[BoardFacts.PawnLever]), false)
    assertEquals(story.pawnBreakProof, Some(proof))
    assertEquals(story.scene, Scene.PawnBreak)
    assertEquals(
      StoryTable
        .choose(Vector(story.copy(pawnBreakProof = Some(proof.copy(contactKinds = Vector.empty)))))
        .head
        .role,
      Role.Blocked
    )
    assertEquals(
      StoryTable
        .choose(Vector(story.copy(pawnBreakProof = Some(proof.copy(nonPromotionMove = false)))))
        .head
        .role,
      Role.Blocked
    )

    val proofSurface =
      classOf[PawnBreakProof].getDeclaredFields.map(_.getName).toSet ++
        classOf[PawnBreakProof].getDeclaredMethods.map(_.getName).toSet
    Vector(
      "longTermStructureWeakness",
      "openFileClaim",
      "passedPawnCreation",
      "pawnMajorityPlan",
      "breakthroughSequence",
      "sacrificeLine",
      "opensThePosition"
    ).foreach: forbidden =>
      assert(
        !proofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)),
        s"PawnBreakProof must not expose $forbidden"
      )

  test("PawnBreak-2 ScenePawnBreak writer admits only bounded proof-backed PawnBreak identity"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val proof = PawnBreakProof.fromBoardFacts(facts, move)
    val story = ScenePawnBreak.write(facts, move).get

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionMove, true)
    assertEquals(proof.targetPawn.exists(_.side == Side.Black), true)
    assertEquals(proof.directPawnLeverAfterMove && proof.leverCreatedByMove, true)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBreak))
    assertEquals(story.scene, Scene.PawnBreak)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.targetPawn.map(_.square))
    assertEquals(story.anchor, proof.originSquare)
    assertEquals(story.route, Some(move))
    assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead)

    assertEquals(ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Supports)).nonEmpty, true)
    assertEquals(ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Caps)).nonEmpty, true)
    assertEquals(
      StoryTable
        .choose(Vector(ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get))
        .head
        .role,
      Role.Blocked
    )

    Vector(
      "passed pawn created" -> story.copy(scene = Scene.PawnAdvance, pawnAdvanceProof = None),
      "material claim" -> story.copy(scene = Scene.Material),
      "strategy meaning" -> story.copy(scene = Scene.Plan, plan = Some(Plan.CenterBreak)),
      "conversion meaning" -> story.copy(
        scene = Scene.Plan,
        plan = Some(Plan.Convert),
        proof = story.proof.copy(conversionPrize = 80)
      ),
      "initiative meaning" -> story.copy(scene = Scene.Plan, plan = Some(Plan.Initiative))
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnBreak-3 negative corpus requires complete direct contact proof or silence"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnFacts = BoardFacts.fromFen("4k3/8/8/8/4N3/8/8/4K3 w - - 0 1").toOption.get
    val noContactFacts = BoardFacts.fromFen("4k3/p7/8/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val spaceGainFacts = BoardFacts.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val spaceMove = Line(Square('e', 2), Square('e', 4))
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val illegalMove = Line(Square('e', 4), Square('e', 6))
    val nonPawnMove = Line(Square('e', 4), Square('f', 6))
    val untrustedFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = move),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('d', 8))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 4)),
          Piece(Side.Black, Man.Pawn, Square('d', 6))
        )
      )
    val legalMoveNotEnough = PawnBreakProof.fromBoardFacts(spaceGainFacts, spaceMove)

    Vector(
      "legal move missing" -> (positiveFacts, illegalMove, "legal pawn move"),
      "same-board proof missing" -> (untrustedFacts, move, "same-board proof"),
      "not a pawn move" -> (nonPawnFacts, nonPawnMove, "pawn identity"),
      "promotion move" -> (promotionFacts, promotionMove, "move is non-promotion"),
      "rival pawn target missing" -> (noContactFacts, move, "exactly one rival pawn target"),
      "direct after-board pawn contact missing" -> (
        noContactFacts,
        move,
        "direct rival pawn lever after move"
      ),
      "simple space gain only" -> (spaceGainFacts, spaceMove, "direct rival pawn lever after move")
    ).foreach: tuple =>
      val (label, corpus) = tuple
      val (facts, line, missing) = corpus
      val proof = PawnBreakProof.fromBoardFacts(facts, line)
      assertEquals(proof.complete, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(ScenePawnBreak.write(facts, line), None, label)

    assertEquals(legalMoveNotEnough.legalPawnMove, true)
    assertEquals(legalMoveNotEnough.directPawnLeverAfterMove, false)
    assertEquals(legalMoveNotEnough.complete, false)

    val story = ScenePawnBreak.write(positiveFacts, move).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet

    Vector(
      "creates_passed_pawn",
      "file_control",
      "weakens_structure",
      "takes_initiative",
      "wins_space",
      "material_gain"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"PawnBreak-3 must forbid $key")

    Vector(
      "e5 creates a passed pawn.",
      "e5 opens the e-file.",
      "e5 weakens the pawn on d6.",
      "e5 takes the initiative.",
      "e5 just gains space.",
      "e5 gains material."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnBreak-0 stays silent unless exactly one direct rival pawn target is created"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val twoTargetsFacts = BoardFacts.fromFen("4k3/8/3p1p2/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val noTargetFacts = BoardFacts.fromFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val captureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val captureMove = Line(Square('e', 5), Square('d', 6))
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val nonPawnMove = Line(Square('e', 1), Square('d', 1))

    val twoTargets = PawnBreakProof.fromBoardFacts(twoTargetsFacts, move)
    assertEquals(twoTargets.directPawnLeverAfterMove, true)
    assertEquals(twoTargets.singleRivalPawnTarget, false)
    assertEquals(twoTargets.complete, false)
    assertEquals(ScenePawnBreak.write(twoTargetsFacts, move), None)

    val noTarget = PawnBreakProof.fromBoardFacts(noTargetFacts, move)
    assertEquals(noTarget.directPawnLeverAfterMove, false)
    assertEquals(noTarget.complete, false)
    assertEquals(ScenePawnBreak.write(noTargetFacts, move), None)

    val capture = PawnBreakProof.fromBoardFacts(captureFacts, captureMove)
    assertEquals(capture.legalPawnMove, true)
    assertEquals(capture.nonCapturingMove, false)
    assertEquals(capture.complete, false)
    assertEquals(ScenePawnBreak.write(captureFacts, captureMove), None)

    val promotion = PawnBreakProof.fromBoardFacts(promotionFacts, promotionMove)
    assertEquals(promotion.legalPawnMove, true)
    assertEquals(promotion.nonPromotionMove, false)
    assertEquals(promotion.complete, false)
    assertEquals(ScenePawnBreak.write(promotionFacts, promotionMove), None)

    val nonPawn = PawnBreakProof.fromBoardFacts(positiveFacts, nonPawnMove)
    assertEquals(nonPawn.pawnBefore, None)
    assertEquals(nonPawn.complete, false)
    assertEquals(ScenePawnBreak.write(positiveFacts, nonPawnMove), None)

    val story = ScenePawnBreak.write(positiveFacts, move).get
    val forgedStrategy = story.copy(plan = Some(Plan.CenterBreak))
    val forgedTactic = story.copy(tactic = Some(Tactic.PawnPush))
    val contaminatedPromotion = story.copy(promotionThreatProof =
      Some(
        PromotionThreatProof.fromBoardFacts(
          BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get,
          Line(Square('e', 6), Square('e', 7))
        )
      )
    )

    Vector(
      "broad strategy" -> forgedStrategy,
      "broad pawn tactic" -> forgedTactic,
      "promotion contamination" -> contaminatedPromotion
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PawnBreak-0 EngineCheck and downstream boundaries stay bounded to pawn contact"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val story = ScenePawnBreak.write(facts, move).get

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 4), Square('e', 6))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    assertEquals(
      ScenePawnBreak.withEngineCheck(story.copy(pawnBreakProof = None), check(EngineCheckStatus.Supports)),
      None
    )
    assertEquals(ScenePawnBreak.withEngineCheck(story, wrongRoute), None)
    assertEquals(
      StoryTable
        .choose(Vector(ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get))
        .head
        .role,
      Role.Blocked
    )
    assertEquals(StoryTable.choose(Vector(supports)).head.role, Role.Lead)
    assertEquals(
      ExplanationPlan
        .fromSelected(StoryTable.choose(Vector(supports)).head)
        .flatMap(_.allowedClaim)
        .map(_.key),
      Some("challenges_pawn")
    )
    assertEquals(StoryTable.choose(Vector(caps)).head.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(caps)).head), None)

    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(supports)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    Vector(
      "PawnBreakProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "proofFailures",
      "FEN",
      "raw PV"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"PawnBreak prompt leaked $forbiddenInput")

  test("PawnBreak-4 reuses EngineCheck without engine-owned PawnBreak claims"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val story = ScenePawnBreak.write(facts, move).get

    def check(
        status: EngineCheckStatus,
        storyInput: Option[Story] = Some(story),
        engineLine: Option[EngineLine] = Some(EngineLine(Vector(move))),
        before: Int = 20,
        after: Int = 20,
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = storyInput,
        engineLine = engineLine,
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val engineOnly = check(EngineCheckStatus.Supports, storyInput = None)
    val supports = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes =
      ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get
    val wrongRoute = check(
      EngineCheckStatus.Supports,
      engineLine = Some(EngineLine(Vector(Line(Square('e', 4), Square('e', 6)))))
    )

    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(ScenePawnBreak.withEngineCheck(story, wrongRoute), None)
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("challenges_pawn")
    )
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("challenges_pawn")
    )
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

    val plan = ExplanationPlan.fromSelected(supportsVerdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    Vector(
      "Engine says e5 is a pawn break.",
      "The eval is +2.20 after e5.",
      "e5 is the best move.",
      "e5 is the only move.",
      "e5 makes the breakthrough work.",
      "e5 creates a winning structure."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnBreak-5 StoryTable keeps direct pawn contact below existing claim homes"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def materialProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = value)

    def strong(story: Story, value: Int): Story =
      story.copy(proof = strongProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertNoPublicText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(label: String, verdict: Verdict, expected: ExplanationClaim): Unit =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(plan.allowedClaim, Some(expected), label)
      assertEquals(rendered.claimKey, expected.key, label)

    def assertExistingHomeKeepsLead(
        rows: Vector[(String, Story)],
        expectedLead: String,
        expectedClaim: ExplanationClaim
    ): Unit =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape, expectedLead)
      assertEquals(shape(rows, shuffled), forwardShape, expectedLead)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.count(_.role == Role.Lead), 1, expectedLead)
      val existingVerdict = forward.find(verdict => rowId(rows, verdict.story) == expectedLead).get
      val pawnBreakVerdict = forward.find(verdict => rowId(rows, verdict.story) == "Scene.PawnBreak").get
      assertEquals(existingVerdict.role, Role.Lead, expectedLead)
      assertLeadClaim(expectedLead, existingVerdict, expectedClaim)
      assertEquals(pawnBreakVerdict.role, Role.Support, expectedLead)
      assertNoStandaloneText(s"$expectedLead keeps PawnBreak support silent", pawnBreakVerdict)
      val existingClaimKey = ExplanationPlan.fromSelected(existingVerdict).flatMap(_.allowedClaim).map(_.key)
      assertEquals(
        existingClaimKey,
        Some(expectedClaim.key),
        expectedLead
      )
      assertEquals(existingClaimKey.contains("challenges_pawn"), false, expectedLead)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get, 99)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get, 90)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get, 90)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 90)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = ScenePromotion.write(promotionFacts, promotionMove).get

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = strongMaterial(SceneMaterial.write(materialFacts, materialMove).get, 90)
    val hanging = strongMaterial(TacticHanging.write(materialFacts, materialMove).get, 90)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 90)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        90
      )
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(
        TacticPin
          .write(
            pinFacts,
            Some(discoveredMove),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get,
        90
      )
    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get,
        90
      )
    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(
        TacticSkewer
          .write(
            skewerFacts,
            Some(skewerMove),
            Some(Square('e', 1)),
            Some(Square('e', 5)),
            Some(Square('e', 8))
          )
          .get,
        90
      )

    Vector(
      ("Scene.PawnAdvance", pawnAdvance, ExplanationClaim.AdvancesPassedPawn),
      ("Scene.PawnStop", pawnStop, ExplanationClaim.StopsPassedPawnNextAdvance),
      ("Scene.PromotionThreat", promotionThreat, ExplanationClaim.CreatesPromotionThreat),
      ("Scene.Promotion", promotion, ExplanationClaim.PromotesPawn),
      ("Scene.Material", material, ExplanationClaim.MaterialBalanceChanges),
      ("Scene.Defense", defense, ExplanationClaim.DefendsPiece),
      ("Tactic.Hanging", hanging, ExplanationClaim.CanWinPiece),
      ("Tactic.DiscoveredAttack", discovered, ExplanationClaim.RevealsAttackOnPiece),
      ("Tactic.Pin", pin, ExplanationClaim.PinsPiece),
      ("Tactic.RemoveGuard", removeGuard, ExplanationClaim.RemovesDefender),
      ("Tactic.Skewer", skewer, ExplanationClaim.SkewersPieceToPiece)
    ).foreach: (label, existing, expectedClaim) =>
      assertExistingHomeKeepsLead(
        Vector(label -> existing, "Scene.PawnBreak" -> pawnBreak),
        label,
        expectedClaim
      )

    val forgedMaterialBreak =
      pawnBreak.copy(
        scene = Scene.Material,
        captureResult = material.captureResult,
        proof = materialProof(99)
      )
    val forgedPassedPawnBreak =
      pawnBreak.copy(scene = Scene.PawnAdvance, pawnAdvanceProof = pawnAdvance.pawnAdvanceProof)
    Vector(
      "PawnBreak must not own Material claim" -> forgedMaterialBreak,
      "PawnBreak must not create PassedPawnCreated or PawnAdvance claim" -> forgedPassedPawnBreak
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertNoPublicText(label, verdict)

    assertEquals(ExplanationClaim.values.exists(_.key == "passed_pawn_created"), false)
    assertEquals(ExplanationClaim.values.exists(_.key == "challenges_pawn"), true)

    val capped =
      ScenePawnBreak
        .withEngineCheck(
          pawnBreak,
          engineCheck(pawnBreakFacts, pawnBreak, pawnBreakMove, EngineCheckStatus.Caps)
        )
        .get
    val refuted =
      ScenePawnBreak
        .withEngineCheck(
          pawnBreak,
          engineCheck(pawnBreakFacts, pawnBreak, pawnBreakMove, EngineCheckStatus.Refutes)
        )
        .get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText("capped PawnBreak has no standalone text", cappedVerdict)
    assertNoStandaloneText("refuted PawnBreak has no standalone text", refutedVerdict)

  test("PawnBreak-6 ExplanationPlan admits only selected uncapped Lead PawnBreak claim"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val story = ScenePawnBreak.write(facts, move).get

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 8))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertNoStandaloneClaim(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val leadVerdict = StoryTable.choose(Vector(story)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    val capped = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refuted = ScenePawnBreak.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head
    val unselectedVerdict = leadVerdict.copy(selected = false)
    val noLeadPermissionVerdict = leadVerdict.copy(leadAllowed = false)

    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadVerdict.leadAllowed, true)
    assertEquals(leadVerdict.engineStrengthLimited, false)
    assertEquals(leadPlan.allowedClaim.map(_.key), Some("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(rendered.claimKey, "challenges_pawn")
    assertEquals(rendered.text, "e5 challenges the pawn on d6.")

    Vector(
      "opens_position",
      "breaks_through",
      "creates_passed_pawn",
      "weakens_structure",
      "wins_space",
      "creates_pressure",
      "takes_initiative",
      "converts_advantage",
      "best_move",
      "only_move",
      "forced"
    ).foreach: forbiddenKey =>
      assertEquals(ExplanationClaim.PawnBreakAllowed.exists(_.key == forbiddenKey), false, forbiddenKey)
      assert(
        leadPlan.forbiddenWording.exists(_.key == forbiddenKey),
        s"PawnBreak-6 must forbid $forbiddenKey"
      )

    Vector(
      "Support" -> supportVerdict,
      "Context" -> contextVerdict,
      "Blocked" -> blockedVerdict,
      "capped" -> cappedVerdict,
      "refuted" -> refutedVerdict,
      "unselected" -> unselectedVerdict,
      "not leadAllowed" -> noLeadPermissionVerdict
    ).foreach: (label, verdict) =>
      assertNoStandaloneClaim(label, verdict)

  test("PawnBreak-7 DeterministicRenderer phrases only bounded pawn contact"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val story = ScenePawnBreak.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fromPlanParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assertEquals(fromPlanParameterNames, Vector("ExplanationPlan"))
    assertEquals(rendered.text, "e5 challenges the pawn on d6.")
    assertEquals(rendered.claimKey, "challenges_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "opens the position",
      "breaks through",
      "creates a passer",
      "weakens the structure",
      "wins space",
      "takes the initiative",
      "creates pressure",
      "best move",
      "only move",
      "forces"
    ).foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"PawnBreak renderer must not mention: $phrase")

    Vector(
      plan.copy(role = Role.Support, allowedClaim = None),
      plan.copy(role = Role.Context, allowedClaim = None),
      plan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(target = None),
      plan.copy(route = None),
      plan.copy(evidenceLine = None)
    ).foreach: blockedPlan =>
      assertEquals(DeterministicRenderer.fromPlan(blockedPlan), None)

  test("PawnBreak-8 LLM smoke reuses 8B boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val story = ScenePawnBreak.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val smokeParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .toSet

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    Vector(
      "renderedText: e5 challenges the pawn on d6.",
      "claimKey: challenges_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PawnBreak-8 prompt must include allowed input: $required")
    Vector(
      "Story(",
      "raw Story",
      "PawnBreakProof",
      "BoardFacts",
      "PawnLever",
      "EngineCheck",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "origin square",
      "destination square",
      "same-board proof",
      "exact after-board replay"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"PawnBreak-8 prompt must not include raw input: $forbidden")
    Vector(
      "Story",
      "PawnBreakProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval"
    ).foreach: forbiddenType =>
      assert(
        !smokeParameterNames.contains(forbiddenType),
        s"PawnBreak-8 smoke must not accept $forbiddenType"
      )

    Vector(
      "raw Story says e5 challenges d6." -> "raw_input",
      "PawnBreakProof proves e5 challenges d6." -> "raw_input",
      "PawnLever raw data shows e5 to d6." -> "raw_input",
      "BoardFacts show the pawn lever on d6." -> "raw_input",
      "EngineCheck and raw PV approve e5." -> "raw_input",
      "proofFailures are empty for e5." -> "raw_input",
      "e5 challenges the pawn on d6 and c5 is next." -> "new_move_or_line",
      "e5 challenges the pawn on d6 and opens the e-file." -> "new_tactic_or_plan",
      "e5 challenges the pawn on d6 and creates a passed pawn." -> "forbidden_wording",
      "e5 challenges the pawn on d6 and starts a strategy." -> "new_tactic_or_plan",
      "e5 challenges the pawn on d6 and takes the initiative." -> "forbidden_wording",
      "e5 challenges the pawn on d6 and converts the advantage." -> "forbidden_wording"
    ).foreach: (output, violation) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, output)
      assert(
        check.violations.contains(violation),
        s"$output should include $violation, got ${check.violations}"
      )

  test("PawnBreak Closeout hard cleanup keeps authority homes separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 4), Square('e', 5))
    val proof = PawnBreakProof.fromBoardFacts(facts, move)
    val story = ScenePawnBreak.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(
      proof.contactKinds,
      Vector(PawnBreakContactKind.PawnChallengesPawn, PawnBreakContactKind.PawnLeverCreated)
    )
    assertEquals(story.scene, Scene.PawnBreak)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBreak))
    assertEquals(story.pawnBreakProof.exists(_.complete), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.multiTargetProof, None)

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.ChallengesPawnDirectly))
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.values.count(_.key == "challenges_pawn"), 1)
    Vector(
      "advances_passed_pawn",
      "stops_pawn_advance",
      "threatens_promotion_next",
      "promotes_pawn",
      "passed_pawn_created",
      "wins_material",
      "defends_piece",
      "can_win_piece",
      "reveals_attack",
      "pins_piece",
      "removes_defender",
      "skewers_piece",
      "opens_position",
      "breaks_through",
      "creates_passed_pawn",
      "weakens_structure",
      "wins_space",
      "creates_pressure",
      "takes_initiative"
    ).foreach: forbiddenKey =>
      assert(
        !ExplanationClaim.PawnBreakAllowed.exists(_.key == forbiddenKey),
        s"PawnBreak must not allow $forbiddenKey"
      )

    assertEquals(rendered.text, "e5 challenges the pawn on d6.")
    assertEquals(rendered.claimKey, "challenges_pawn")
    assertEquals(rendered.strength, "bounded")
    assert(!prompt.contains("PawnBreakProof"))
    assert(!prompt.contains("PawnLever"))
    assert(!prompt.contains("EngineCheck"))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    Vector(
      "e5 opens the position.",
      "e5 breaks through.",
      "e5 weakens the structure.",
      "e5 wins space.",
      "e5 creates pressure.",
      "e5 takes the initiative.",
      "e5 creates a passed pawn.",
      "e5 wins material."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnCapture-0 opens only a legal pawn captures pawn event"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = PawnCaptureProof.fromBoardFacts(facts, move)
    val story = ScenePawnCapture.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('d', 6))))
    assertEquals(proof.capturedPawn, Some(Piece(Side.Black, Man.Pawn, Square('d', 6))))
    assertEquals(proof.captureMove, Some(move))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionMove, true)
    assertEquals(proof.pawnCapturesPawn, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.singleRivalPawnCaptured, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(story.scene, Scene.PawnCapture)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnCapture))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.target, Some(Square('d', 6)))
    assertEquals(story.route, Some(move))
    assertEquals(story.routeSan, Some("exd6"))
    assertEquals(story.pawnCaptureProof, Some(proof))
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.scene, Scene.PawnCapture)
    assertEquals(plan.allowedClaim.map(_.key), Some("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(rendered.text, "exd6 captures the pawn on d6.")
    assertEquals(rendered.claimKey, "captures_rival_pawn")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "exd6 wins a pawn.",
      "exd6 gains material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure.",
      "exd6 breaks through.",
      "exd6 takes the initiative.",
      "exd6 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnCapture-0 negative corpus requires a pawn taking exactly one rival pawn"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnTargetFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val quietPawnMoveFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnMoveFacts = BoardFacts.fromFen("4k3/8/3p4/4N3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionCaptureFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val quietMove = Line(Square('e', 5), Square('e', 6))
    val nonPawnMove = Line(Square('e', 5), Square('d', 7))
    val promotionCapture = Line(Square('e', 7), Square('d', 8))
    val untrustedFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = move, captureCount = 1),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('d', 8))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Pawn, Square('d', 6))
        )
      )

    Vector(
      "legal move missing" -> (positiveFacts, Line(Square('e', 5), Square('c', 6)), "legal pawn move"),
      "same-board proof missing" -> (untrustedFacts, move, "same-board proof"),
      "not a pawn move" -> (nonPawnMoveFacts, nonPawnMove, "pawn identity"),
      "quiet pawn move" -> (quietPawnMoveFacts, quietMove, "captured rival pawn"),
      "captured piece is not pawn" -> (nonPawnTargetFacts, move, "captured rival pawn"),
      "promotion capture closed" -> (promotionCaptureFacts, promotionCapture, "move is non-promotion")
    ).foreach: tuple =>
      val (label, corpus) = tuple
      val (facts, line, missing) = corpus
      val proof = PawnCaptureProof.fromBoardFacts(facts, line)
      assertEquals(proof.complete, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(ScenePawnCapture.write(facts, line), None, label)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    assertEquals(
      ScenePawnCapture.write(pawnBreakFacts, Line(Square('e', 4), Square('e', 5))),
      None,
      "pawn break is separate"
    )
    assertEquals(ScenePawnBreak.write(positiveFacts, move), None, "pawn capture is not PawnBreak")

    val story = ScenePawnCapture.write(positiveFacts, move).get
    val forgedMaterial = story.copy(scene = Scene.Material, proof = story.proof.copy(conversionPrize = 80))
    val forgedStrategy = story.copy(scene = Scene.Plan, plan = Some(Plan.CenterBreak))
    Vector("material claim" -> forgedMaterial, "strategy claim" -> forgedStrategy).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnCapture-1 PawnCaptureProof proves ordinary diagonal pawn capture only"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = PawnCaptureProof.fromBoardFacts(facts, move)
    val story = ScenePawnCapture.write(facts, move).get
    val material = SceneMaterial.write(facts, move).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.originSquare, Some(Square('e', 5)))
    assertEquals(proof.captureSquare, Some(Square('d', 6)))
    assertEquals(proof.capturedPawn, Some(Piece(Side.Black, Man.Pawn, Square('d', 6))))
    assertEquals(proof.captureMove, Some(move))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.legalPawnCapture, true)
    assertEquals(proof.nonPromotionMove, true)
    assertEquals(proof.ordinaryDiagonalPawnCapture, true)
    assertEquals(proof.pawnCapturesPawn, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.singleRivalPawnCaptured, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(classOf[Story].isAssignableFrom(classOf[PawnCaptureProof]), false)

    assertEquals(story.scene, Scene.PawnCapture)
    assertEquals(story.pawnCaptureProof, Some(proof))
    assertEquals(story.captureResult, None)
    assertEquals(material.scene, Scene.Material)
    assertEquals(material.captureResult.map(_.targetPiece), Some(proof.capturedPawn))
    assertEquals(material.pawnCaptureProof, None)

    val forgedMaterialClaim =
      story.copy(
        scene = Scene.Material,
        writer = Some(StoryWriter.SceneMaterial),
        proof = story.proof.copy(conversionPrize = 80)
      )
    val forgedCaptureResultClaim = story.copy(captureResult = material.captureResult)
    Vector(
      "Scene.Material replacement" -> forgedMaterialClaim,
      "CaptureResult replacement" -> forgedCaptureResultClaim
    )
      .foreach: (label, row) =>
        val verdict = StoryTable.choose(Vector(row)).head
        assertEquals(verdict.role, Role.Blocked, label)
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          label
        )

    val proofSurface = classOf[PawnCaptureProof].getDeclaredMethods.map(_.getName.toLowerCase).toSet
    Vector("material", "passed", "file", "weak", "win").foreach: forbidden =>
      assert(
        !proofSurface.exists(_.contains(forbidden)),
        s"PawnCaptureProof must not expose $forbidden meaning"
      )

  test("PawnCapture-1 keeps en passant promotion and structure claims closed"):
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val promotionCaptureFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val enPassant = Line(Square('e', 5), Square('d', 6))
    val promotionCapture = Line(Square('e', 7), Square('d', 8))
    val ordinaryCapture = Line(Square('e', 5), Square('d', 6))

    val enPassantProof = PawnCaptureProof.fromBoardFacts(enPassantFacts, enPassant)
    assertEquals(enPassantProof.legalPawnMove, true)
    assertEquals(enPassantProof.legalPawnCapture, false)
    assertEquals(enPassantProof.ordinaryDiagonalPawnCapture, false)
    assertEquals(enPassantProof.exactAfterBoardReplay, false)
    assertEquals(enPassantProof.complete, false)
    assert(enPassantProof.missingEvidence.exists(_.missing.contains("captured rival pawn")))
    assert(enPassantProof.missingEvidence.exists(_.missing.contains("ordinary diagonal pawn capture")))
    assertEquals(ScenePawnCapture.write(enPassantFacts, enPassant), None)

    val promotionProof = PawnCaptureProof.fromBoardFacts(promotionCaptureFacts, promotionCapture)
    assertEquals(promotionProof.legalPawnMove, true)
    assertEquals(promotionProof.legalPawnCapture, false)
    assertEquals(promotionProof.nonPromotionMove, false)
    assertEquals(promotionProof.ordinaryDiagonalPawnCapture, false)
    assertEquals(promotionProof.complete, false)
    assert(promotionProof.missingEvidence.exists(_.missing.contains("move is non-promotion")))
    assertEquals(ScenePawnCapture.write(promotionCaptureFacts, promotionCapture), None)

    val story = ScenePawnCapture.write(positiveFacts, ordinaryCapture).get
    Vector(
      "passed pawn creation" -> story.copy(plan = Some(Plan.PasserMake)),
      "file opening" -> story.copy(plan = Some(Plan.OpenFile)),
      "structural weakness" -> story.copy(plan = Some(Plan.WeakSquare)),
      "material gain" -> story.copy(scene = Scene.Material, proof = story.proof.copy(conversionPrize = 80))
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnCapture-2 ScenePawnCapture writer pins identity and EngineCheck boundary"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePawnCapture.write(facts, move).get
    val proof = story.pawnCaptureProof.get

    assertEquals(story.scene, Scene.PawnCapture)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnCapture))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.captureSquare)
    assertEquals(story.anchor, proof.originSquare)
    assertEquals(story.route, proof.captureMove)
    assertEquals(story.captureResult, None)
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnCapture, true)
    assertEquals(proof.ordinaryDiagonalPawnCapture, true)
    assertEquals(proof.capturedPawn.exists(_.side == proof.rivalSide), true)
    assertEquals(proof.capturedPawn.exists(_.man == Man.Pawn), true)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = check(EngineCheckStatus.Unknown)
    val supports = check(EngineCheckStatus.Supports)
    val caps = check(EngineCheckStatus.Caps)
    val refutes = check(EngineCheckStatus.Supports, before = 200, after = 0)

    assertEquals(unknown.storyBound, true)
    assertEquals(supports.storyBound, true)
    assertEquals(caps.storyBound, true)
    assertEquals(refutes.storyBound, true)
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    assertEquals(
      ScenePawnCapture.withEngineCheck(story, unknown).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Unknown)
    )
    assertEquals(
      ScenePawnCapture.withEngineCheck(story, supports).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Supports)
    )
    assertEquals(
      ScenePawnCapture.withEngineCheck(story, caps).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Caps)
    )
    val refutedStory = ScenePawnCapture.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))

    val material = SceneMaterial.write(facts, move).get
    Vector(
      "writerless row" -> story.copy(writer = None),
      "wrong writer" -> story.copy(writer = Some(StoryWriter.SceneMaterial)),
      "incomplete StoryProof" -> story.copy(storyProof = StoryProof.empty),
      "Scene.Material claim" -> story.copy(scene = Scene.Material, captureResult = material.captureResult),
      "PassedPawnCreated claim" -> story.copy(plan = Some(Plan.PasserMake)),
      "open-file claim" -> story.copy(plan = Some(Plan.OpenFile)),
      "weakness claim" -> story.copy(plan = Some(Plan.WeakSquare)),
      "strategy claim" -> story.copy(scene = Scene.Plan, plan = Some(Plan.CenterBreak))
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PawnCapture-3 negative corpus requires complete PawnCaptureProof or silence"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnTargetFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val quietPawnMoveFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnMoveFacts = BoardFacts.fromFen("4k3/8/3p4/4N3/8/8/8/4K3 w - - 0 1").toOption.get
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val promotionCaptureFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnCapture = Line(Square('e', 5), Square('d', 6))
    val illegalMove = Line(Square('e', 5), Square('c', 6))
    val quietMove = Line(Square('e', 5), Square('e', 6))
    val nonPawnMove = Line(Square('e', 5), Square('d', 7))
    val promotionCapture = Line(Square('e', 7), Square('d', 8))
    val untrustedFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = pawnCapture, captureCount = 1),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('e', 7))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Pawn, Square('d', 6))
        )
      )
    val sameSideTargetFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = pawnCapture, captureCount = 1),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('e', 7))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.White, Man.Pawn, Square('d', 6))
        )
      )

    Vector(
      "legal move 아님" -> (positiveFacts, illegalMove, "legal pawn move"),
      "same-board proof 없음" -> (untrustedFacts, pawnCapture, "same-board proof"),
      "moving piece가 pawn이 아님" -> (nonPawnMoveFacts, nonPawnMove, "pawn identity"),
      "capture가 아님" -> (quietPawnMoveFacts, quietMove, "captured rival pawn"),
      "captured piece가 pawn이 아님" -> (nonPawnTargetFacts, pawnCapture, "captured rival pawn"),
      "captured piece가 rival side가 아님" -> (sameSideTargetFacts, pawnCapture, "captured rival pawn"),
      "en passant" -> (enPassantFacts, pawnCapture, "captured rival pawn"),
      "promotion capture" -> (promotionCaptureFacts, promotionCapture, "move is non-promotion")
    ).foreach: (label, corpus) =>
      val (facts, line, missing) = corpus
      val proof = PawnCaptureProof.fromBoardFacts(facts, line)
      assertEquals(proof.complete, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(ScenePawnCapture.write(facts, line), None, label)

    val story = ScenePawnCapture.write(positiveFacts, pawnCapture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val material = SceneMaterial.write(positiveFacts, pawnCapture).get
    Vector(
      "material gain을 PawnCapture claim으로 말하려 함" -> story.copy(captureResult = material.captureResult),
      "passed pawn created wording 유입" -> story.copy(plan = Some(Plan.PasserMake)),
      "open file wording 유입" -> story.copy(plan = Some(Plan.OpenFile)),
      "weakness wording 유입" -> story.copy(plan = Some(Plan.WeakSquare))
    ).foreach: (label, row) =>
      val contaminated = StoryTable.choose(Vector(row)).head
      assertEquals(contaminated.role, Role.Blocked, label)
      assertEquals(
        ExplanationPlan.fromSelected(contaminated).flatMap(DeterministicRenderer.fromPlan),
        None,
        label
      )

    Vector(
      "exd6 wins a pawn.",
      "exd6 gains material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnCapture-4 reuses EngineCheck without engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePawnCapture.write(facts, move).get

    def check(
        status: EngineCheckStatus,
        line: Line = move,
        storyInput: Option[Story] = Some(story),
        before: Int = 20,
        after: Int = 20,
        depth: Option[Int] = Some(12),
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = storyInput,
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = depth,
        freshnessPly = freshness,
        requestedStatus = status
      )

    val noStory = check(EngineCheckStatus.Supports, storyInput = None)
    val wrongRoute = check(EngineCheckStatus.Supports, line = Line(Square('e', 1), Square('e', 2)))
    assertEquals(noStory.storyBound, false)
    assertEquals(noStory.status, EngineCheckStatus.Unknown)
    assertEquals(ScenePawnCapture.withEngineCheck(story, noStory), None)
    assertEquals(wrongRoute.storyBound, true)
    assertEquals(wrongRoute.status, EngineCheckStatus.Unknown)
    assertEquals(ScenePawnCapture.withEngineCheck(story, wrongRoute), None)

    val supports = ScenePawnCapture.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val unknown = ScenePawnCapture.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val caps = ScenePawnCapture.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      ScenePawnCapture.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 0)).get

    val baseVerdict = StoryTable.choose(Vector(story)).head
    val supportVerdict = StoryTable.choose(Vector(supports)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutedVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supportVerdict.role, Role.Lead)
    assertEquals(supportVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(
      ExplanationPlan.fromSelected(supportVerdict).flatMap(_.allowedClaim.map(_.key)),
      Some("captures_rival_pawn")
    )
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim.map(_.key)),
      Some("captures_rival_pawn")
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict).flatMap(DeterministicRenderer.fromPlan), None)

    val baseRendered = DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(baseVerdict).get).get
    Vector(supportVerdict, unknownVerdict).foreach: verdict =>
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(rendered.text, baseRendered.text)
      Vector(
        s"${rendered.text} The engine says this is best.",
        s"${rendered.text} The eval is +1.20.",
        s"${rendered.text} This is the only move.",
        s"${rendered.text} It wins a pawn.",
        s"${rendered.text} It creates a structural advantage."
      ).foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PawnCapture-5 StoryTable keeps existing claim homes ahead of PawnCapture"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def eventProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = 0)

    def materialProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = value)

    def strong(story: Story, value: Int): Story =
      story.copy(proof = eventProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      plan.foreach: relationOnly =>
        assertEquals(relationOnly.allowedClaim, None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(label: String, verdict: Verdict, expected: ExplanationClaim): RenderedLine =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(verdict.leadAllowed, true, label)
      assertEquals(plan.allowedClaim, Some(expected), label)
      assertEquals(rendered.claimKey, expected.key, label)
      rendered

    def assertExistingHomeKeepsLead(
        rows: Vector[(String, Story)],
        expectedLead: String,
        expectedClaim: ExplanationClaim
    ): Unit =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape, expectedLead)
      assertEquals(shape(rows, shuffled), forwardShape, expectedLead)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.count(_.role == Role.Lead), 1, expectedLead)
      val existingVerdict = forward.find(verdict => rowId(rows, verdict.story) == expectedLead).get
      val pawnCaptureVerdict = forward.find(verdict => rowId(rows, verdict.story) == "Scene.PawnCapture").get
      val rendered = assertLeadClaim(expectedLead, existingVerdict, expectedClaim)
      assertEquals(pawnCaptureVerdict.role, Role.Support, expectedLead)
      assertNoStandaloneText(s"$expectedLead keeps PawnCapture support silent", pawnCaptureVerdict)
      assertEquals(rendered.claimKey == ExplanationClaim.CapturesPawn.key, false, expectedLead)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(40)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val captureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = strong(ScenePawnCapture.write(captureFacts, captureMove).get, 99)
    val material = strongMaterial(SceneMaterial.write(captureFacts, captureMove).get, 90)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get, 90)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get, 90)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get, 90)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 90)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = ScenePromotion.write(promotionFacts, promotionMove).get

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = strongMaterial(TacticHanging.write(hangingFacts, hangingMove).get, 90)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 90)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        90
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(
        TacticPin
          .write(
            pinFacts,
            Some(discoveredMove),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get,
        90
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get,
        90
      )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(
        TacticSkewer
          .write(
            skewerFacts,
            Some(skewerMove),
            Some(Square('e', 1)),
            Some(Square('e', 5)),
            Some(Square('e', 8))
          )
          .get,
        90
      )

    Vector(
      ("Scene.PawnBreak", pawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      ("Scene.PawnAdvance", pawnAdvance, ExplanationClaim.AdvancesPassedPawn),
      ("Scene.PawnStop", pawnStop, ExplanationClaim.StopsPassedPawnNextAdvance),
      ("Scene.PromotionThreat", promotionThreat, ExplanationClaim.CreatesPromotionThreat),
      ("Scene.Promotion", promotion, ExplanationClaim.PromotesPawn),
      ("Scene.Material", material, ExplanationClaim.MaterialBalanceChanges),
      ("Tactic.Hanging", hanging, ExplanationClaim.CanWinPiece),
      ("Scene.Defense", defense, ExplanationClaim.DefendsPiece),
      ("Tactic.DiscoveredAttack", discovered, ExplanationClaim.RevealsAttackOnPiece),
      ("Tactic.Pin", pin, ExplanationClaim.PinsPiece),
      ("Tactic.RemoveGuard", removeGuard, ExplanationClaim.RemovesDefender),
      ("Tactic.Skewer", skewer, ExplanationClaim.SkewersPieceToPiece)
    ).foreach: (label, existing, expectedClaim) =>
      assertExistingHomeKeepsLead(
        Vector(label -> existing, "Scene.PawnCapture" -> pawnCapture),
        label,
        expectedClaim
      )

    val materialCollision = StoryTable.choose(Vector(material, pawnCapture))
    val materialLead = materialCollision.find(_.story == material).get
    val pawnUnderMaterial = materialCollision.find(_.story == pawnCapture).get
    val materialRendered =
      assertLeadClaim("actual material change now", materialLead, ExplanationClaim.MaterialBalanceChanges)
    assertEquals(materialRendered.claimKey, "material_balance_changes")
    assertEquals(ExplanationPlan.fromSelected(pawnUnderMaterial), None)

    val breakCollision = StoryTable.choose(Vector(pawnBreak, pawnCapture))
    val breakLead = breakCollision.find(_.story == pawnBreak).get
    val pawnUnderBreak = breakCollision.find(_.story == pawnCapture).get
    val breakRendered =
      assertLeadClaim("pawn contact/challenge", breakLead, ExplanationClaim.ChallengesPawnDirectly)
    assertEquals(breakRendered.claimKey, "challenges_pawn")
    assertEquals(ExplanationPlan.fromSelected(pawnUnderBreak), None)

    val blackCaptureFacts = BoardFacts.fromFen("4k3/8/8/8/3p4/4P3/8/4K3 b - - 0 1").toOption.get
    val blackCaptureMove = Line(Square('d', 4), Square('e', 3))
    val blackPawnCapture = strong(ScenePawnCapture.write(blackCaptureFacts, blackCaptureMove).get, 99)
    val captureRows = Vector("White PawnCapture" -> pawnCapture, "Black PawnCapture" -> blackPawnCapture)
    val captureForward = StoryTable.choose(captureRows.map(_._2))
    val captureReverse = StoryTable.choose(captureRows.reverse.map(_._2))
    assertEquals(shape(captureRows, captureForward), shape(captureRows, captureReverse))
    assertEquals(captureForward.count(_.role == Role.Lead), 1)
    assert(
      captureForward.forall(verdict =>
        verdict.story.scene != Scene.PawnCapture ||
          ExplanationPlan.fromSelected(verdict).forall(_.allowedClaim.contains(ExplanationClaim.CapturesPawn))
      )
    )

    val pawnVerdict = StoryTable.choose(Vector(pawnCapture)).head
    val pawnPlan = ExplanationPlan.fromSelected(pawnVerdict).get
    val pawnRendered = DeterministicRenderer.fromPlan(pawnPlan).get
    assertEquals(pawnPlan.allowedClaim, Some(ExplanationClaim.CapturesPawn))
    Vector(
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(pawnPlan, pawnRendered, output).accepted, false, output)

    val capped = ScenePawnCapture
      .withEngineCheck(
        pawnCapture,
        engineCheck(captureFacts, pawnCapture, captureMove, EngineCheckStatus.Caps)
      )
      .get
    val refutingCheck =
      EngineCheck.fromStory(
        facts = captureFacts,
        story = Some(pawnCapture),
        engineLine = Some(EngineLine(Vector(captureMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(240)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val refuted = ScenePawnCapture.withEngineCheck(pawnCapture, refutingCheck).get
    Vector("capped PawnCapture" -> capped, "refuted PawnCapture" -> refuted).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertNoStandaloneText(label, verdict)

  test("PawnCapture-6 ExplanationPlan accepts only selected uncapped Lead with bounded rival-pawn claim"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePawnCapture.write(facts, move).get
    val leadVerdict = StoryTable.choose(Vector(story)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadVerdict.leadAllowed, true)
    assertEquals(leadVerdict.engineStrengthLimited, false)
    assertEquals(leadPlan.allowedClaim.map(_.key), Some("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(rendered.claimKey, "captures_rival_pawn")
    assertEquals(rendered.strength, "bounded")

    Vector(
      "wins_pawn",
      "wins_material",
      "creates_passed_pawn",
      "weakens_structure",
      "breaks_through",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced"
    ).foreach: forbiddenKey =>
      assert(
        leadPlan.forbiddenWording.map(_.key).contains(forbiddenKey),
        s"PawnCapture must forbid $forbiddenKey"
      )

    Vector(
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure.",
      "exd6 breaks through.",
      "exd6 creates pressure.",
      "exd6 takes the initiative.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 is forced."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, output)

    val nonStandalone = Vector(
      "unselected" -> leadVerdict.copy(selected = false),
      "support" -> leadVerdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
      "blocked" -> leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    )
    nonStandalone.foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

    def engineCheck(status: EngineCheckStatus, before: Int = 40, after: Int = 40): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped = StoryTable
      .choose(Vector(ScenePawnCapture.withEngineCheck(story, engineCheck(EngineCheckStatus.Caps)).get))
      .head
    val refuted =
      StoryTable
        .choose(
          Vector(
            ScenePawnCapture
              .withEngineCheck(story, engineCheck(EngineCheckStatus.Supports, before = 220, after = 0))
              .get
          )
        )
        .head
    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(capped.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(refuted.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, verdict) =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PawnCapture-7 DeterministicRenderer phrases only bounded rival pawn capture"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePawnCapture.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "exd6 captures the pawn on d6.")
    assertEquals(rendered.claimKey, "captures_rival_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      plan.copy(role = Role.Support, allowedClaim = None),
      plan.copy(role = Role.Context, allowedClaim = None),
      plan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges)),
      plan.copy(scene = Scene.Material),
      plan.copy(tactic = Some(Tactic.Hanging)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(secondaryTarget = Some(Square('e', 5)))
    ).foreach: malformed =>
      assertEquals(DeterministicRenderer.fromPlan(malformed), None, malformed.toString)

    Vector(
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure.",
      "exd6 breaks through.",
      "exd6 takes the initiative.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 forces the issue."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    assertEquals(
      rendererMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )

  test("PawnCapture-8 LLM smoke reuses 8B prompt boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePawnCapture.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    assert(prompt.contains("renderedText: exd6 captures the pawn on d6."))
    assert(prompt.contains("claimKey: captures_rival_pawn"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("forbiddenWording:"))
    Vector(
      "raw Story",
      "PawnCaptureProof",
      "CaptureResult",
      "BoardFacts",
      "EngineCheck",
      "raw PV",
      "proofFailures"
    ).foreach: rawInput =>
      assert(!prompt.contains(rawInput), s"PawnCapture LLM prompt must not expose $rawInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )

    Vector(
      "raw Story says exd6 captures the pawn.",
      "PawnCaptureProof proves exd6.",
      "CaptureResult shows the capture.",
      "BoardFacts show exd6.",
      "EngineCheck supports exd6.",
      "raw PV continues Ke7.",
      "proofFailures are empty.",
      "exd6 captures the pawn on d6, then Ke7 follows.",
      "exd6 captures the pawn on d6 in the line 1...Ke7.",
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure.",
      "exd6 starts a strategy."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan.copy(allowedClaim = None), rendered), None)
    assertEquals(
      LlmNarrationSmoke.codexCliPrompt(plan, rendered.copy(claimKey = "material_balance_changes")),
      None
    )
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered.copy(text = "exd6 wins material.")), None)

    val promptMethods =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector.filter(_.getName == "codexCliPrompt")
    assertEquals(
      promptMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan", "RenderedLine"))
    )
    val checkMethods = LlmNarrationSmoke.getClass.getDeclaredMethods.toVector.filter(_.getName == "check")
    assertEquals(
      checkMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan", "RenderedLine", "String"))
    )

  test("PawnCapture Closeout hard cleanup keeps authority homes separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = PawnCaptureProof.fromBoardFacts(facts, move)
    val story = ScenePawnCapture.write(facts, move).get
    val material = SceneMaterial.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val materialFirst = StoryTable.choose(Vector(story, material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.pawnCapturesPawn, true)
    assertEquals(proof.singleRivalPawnCaptured, true)
    assertEquals(story.scene, Scene.PawnCapture)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnCapture))
    assertEquals(story.pawnCaptureProof.exists(_.complete), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.multiTargetProof, None)

    assertEquals(ScenePawnBreak.write(facts, move), None)
    assertEquals(ScenePawnAdvance.write(facts, move), None)
    assertEquals(ScenePawnStop.write(facts, move), None)
    assertEquals(ScenePromotionThreat.write(facts, move), None)
    assertEquals(ScenePromotion.write(facts, move), None)
    assertEquals(materialFirst.story.scene, Scene.Material)
    assertEquals(
      ExplanationPlan.fromSelected(materialFirst).flatMap(_.allowedClaim).map(_.key),
      Some("material_balance_changes")
    )

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CapturesPawn))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.values.count(_.key == "captures_rival_pawn"), 1)
    Vector(
      "challenges_pawn",
      "advances_passed_pawn",
      "stops_pawn_advance",
      "threatens_promotion_next",
      "promotes_pawn",
      "passed_pawn_created",
      "wins_pawn",
      "wins_material",
      "material_balance_changes",
      "can_win_piece",
      "defends_piece",
      "reveals_attack",
      "pins_piece",
      "removes_defender",
      "skewers_piece",
      "creates_passed_pawn",
      "opens_file",
      "weakens_structure",
      "creates_pressure",
      "takes_initiative"
    ).foreach: forbiddenKey =>
      assert(
        !ExplanationClaim.PawnCaptureAllowed.exists(_.key == forbiddenKey),
        s"PawnCapture must not allow $forbiddenKey"
      )

    assertEquals(rendered.text, "exd6 captures the pawn on d6.")
    assertEquals(rendered.claimKey, "captures_rival_pawn")
    assertEquals(rendered.strength, "bounded")
    assert(!prompt.contains("PawnCaptureProof"))
    assert(!prompt.contains("CaptureResult"))
    assert(!prompt.contains("BoardFacts"))
    assert(!prompt.contains("EngineCheck"))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    Vector(
      "exd6 wins pawn.",
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 opens the file.",
      "exd6 weakens the structure.",
      "exd6 takes the initiative.",
      "exd6 creates pressure."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PassedPawnCreated-0 opens only a newly created passed pawn"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = PassedPawnCreatedProof.fromBoardFacts(facts, move)
    val story = ScenePassedPawnCreated.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('d', 6))))
    assertEquals(proof.createdPassedPawn, Some(Piece(Side.White, Man.Pawn, Square('d', 6))))
    assertEquals(proof.originSquare, Some(Square('e', 5)))
    assertEquals(proof.afterSquare, Some(Square('d', 6)))
    assertEquals(proof.creatingMove, Some(move))
    assertEquals(proof.exactBeforeBoard, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.ordinaryPawnMoveOrCapture, true)
    assertEquals(proof.nonPromotionMove, true)
    assertEquals(proof.passedBefore, false)
    assertEquals(proof.passedAfter, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.exactlyOneNewPassedPawn, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(story.scene, Scene.PassedPawnCreated)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePassedPawnCreated))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.target, Some(Square('d', 6)))
    assertEquals(story.route, Some(move))
    assertEquals(story.routeSan, Some("exd6"))
    assertEquals(story.passedPawnCreatedProof, Some(proof))
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.scene, Scene.PassedPawnCreated)
    assertEquals(plan.allowedClaim.map(_.key), Some("creates_passed_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    assertEquals(rendered.text, "exd6 creates a passed pawn on d6.")
    assertEquals(rendered.claimKey, "creates_passed_pawn")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      "exd6 creates an unstoppable passer.",
      "exd6 threatens promotion.",
      "exd6 wins the endgame.",
      "exd6 converts the advantage.",
      "exd6 starts a pawn race.",
      "exd6 is the best move.",
      "exd6 is forced."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PassedPawnCreated-0 negative corpus requires before-after passed-pawn change"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val alreadyPassedFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val stillNotPassedFacts = BoardFacts.fromFen("4k3/3p4/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnFacts = BoardFacts.fromFen("4k3/8/3p4/4N3/8/8/8/4K3 w - - 0 1").toOption.get
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val advance = Line(Square('e', 5), Square('e', 6))
    val promotion = Line(Square('e', 7), Square('d', 8))
    val nonPawnMove = Line(Square('e', 5), Square('d', 7))
    val enPassant = Line(Square('e', 5), Square('d', 6))
    val untrustedFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = move, captureCount = 1),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('e', 7))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Pawn, Square('d', 6))
        )
      )

    Vector(
      "legal move missing" -> (positiveFacts, Line(Square('e', 5), Square('c', 6)), "legal pawn move"),
      "same-board proof missing" -> (untrustedFacts, move, "same-board proof"),
      "moving piece is not pawn" -> (nonPawnFacts, nonPawnMove, "pawn identity"),
      "already passed before" -> (alreadyPassedFacts, advance, "not passed before move"),
      "after board still not passed" -> (stillNotPassedFacts, advance, "passed pawn on exact after-board"),
      "en passant remains closed" -> (enPassantFacts, enPassant, "ordinary pawn move or capture"),
      "promotion remains closed" -> (promotionFacts, promotion, "move is non-promotion")
    ).foreach: (label, corpus) =>
      val (facts, line, missing) = corpus
      val proof = PassedPawnCreatedProof.fromBoardFacts(facts, line)
      assertEquals(proof.complete, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(ScenePassedPawnCreated.write(facts, line), None, label)

    val story = ScenePassedPawnCreated.write(positiveFacts, move).get
    val forgedPromotion = story.copy(scene = Scene.Promotion)
    val forgedThreat = story.copy(scene = Scene.PromotionThreat)
    val forgedConversion = story.copy(scene = Scene.Convert, plan = Some(Plan.Convert))
    val forgedMaterial = story.copy(scene = Scene.Material, proof = story.proof.copy(conversionPrize = 80))
    Vector(forgedPromotion, forgedThreat, forgedConversion, forgedMaterial).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("PassedPawnCreated-2 ScenePassedPawnCreated writer pins identity and sibling boundaries"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePassedPawnCreated.write(facts, move).get
    val proof = story.passedPawnCreatedProof.get
    def strongProof(value: Int): Proof =
      Proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = 0,
        conversionPrize = 0,
        counterplayRisk = 0,
        kingHeat = 0,
        pieceSupport = 0,
        pawnSupport = value,
        sourceFit = 0,
        novelty = 0,
        clarity = value
      )

    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactBeforeBoard, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.ordinaryPawnMoveOrCapture, true)
    assertEquals(proof.passedBefore, false)
    assertEquals(proof.passedAfter, true)
    assertEquals(story.writer, Some(StoryWriter.ScenePassedPawnCreated))
    assertEquals(story.scene, Scene.PassedPawnCreated)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.afterSquare)
    assertEquals(story.anchor, proof.originSquare)
    assertEquals(story.route, proof.creatingMove)
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = check(EngineCheckStatus.Supports)
    val caps = check(EngineCheckStatus.Caps)
    val unknown = check(EngineCheckStatus.Unknown)
    val refutes = check(EngineCheckStatus.Supports, before = 220, after = 0)

    assertEquals(supports.storyBound, true)
    assertEquals(supports.evidenceReady, true)
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    assertEquals(
      ScenePassedPawnCreated.withEngineCheck(story, supports).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Supports)
    )
    assertEquals(
      ScenePassedPawnCreated.withEngineCheck(story, caps).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Caps)
    )
    assertEquals(
      ScenePassedPawnCreated.withEngineCheck(story, unknown).flatMap(_.engineCheck.map(_.status)),
      Some(EngineCheckStatus.Unknown)
    )
    val refutedStory = ScenePassedPawnCreated.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val pawnCapture = ScenePawnCapture.write(facts, move).get
    val strongerCreated = story.copy(proof = strongProof(100))
    val captureCollision = StoryTable.choose(Vector(strongerCreated, pawnCapture))
    assertEquals(captureCollision.head.story.writer, Some(StoryWriter.ScenePawnCapture))
    assertEquals(captureCollision.head.role, Role.Lead)
    assertEquals(captureCollision.find(_.story.writer.contains(StoryWriter.ScenePassedPawnCreated)).map(_.role), Some(Role.Support))

    Vector(
      "writerless row" -> story.copy(writer = None),
      "wrong writer" -> story.copy(writer = Some(StoryWriter.ScenePawnAdvance)),
      "incomplete StoryProof" -> story.copy(storyProof = StoryProof.empty),
      "Scene.PawnAdvance meaning" -> story.copy(scene = Scene.PawnAdvance, pawnAdvanceProof = None),
      "Scene.PawnCapture meaning" -> story.copy(scene = Scene.PawnCapture, pawnCaptureProof = None),
      "PromotionThreat meaning" -> story.copy(scene = Scene.PromotionThreat),
      "Promotion meaning" -> story.copy(scene = Scene.Promotion),
      "Conversion meaning" -> story.copy(scene = Scene.Convert, plan = Some(Plan.Convert))
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PassedPawnCreated-3 negative corpus requires exact before-after passer proof or silence"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val alreadyPassedFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val stillNotPassedFacts = BoardFacts.fromFen("4k3/3p4/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val enPassantFacts = BoardFacts.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val twoMoveCreationFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val captureOnlyFacts = BoardFacts.fromFen("4k3/4p3/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val createMove = Line(Square('e', 5), Square('d', 6))
    val illegalMove = Line(Square('e', 5), Square('c', 6))
    val advance = Line(Square('e', 5), Square('e', 6))
    val promotion = Line(Square('e', 7), Square('d', 8))
    val twoMoveFirstStep = Line(Square('e', 4), Square('e', 5))
    val untrustedFacts =
      minimalBoardFacts(
        sideLegal = readyMoves(line = createMove, captureCount = 1),
        rivalLegal = readyMoves(line = Line(Square('e', 8), Square('e', 7))),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('e', 5)),
          Piece(Side.Black, Man.Pawn, Square('d', 6))
        )
      )

    Vector(
      "legal move missing" -> (positiveFacts, illegalMove, "legal pawn move"),
      "same-board proof missing" -> (untrustedFacts, createMove, "same-board proof"),
      "after board still not passed" -> (stillNotPassedFacts, advance, "passed pawn on exact after-board"),
      "before-board already passed" -> (alreadyPassedFacts, advance, "not passed before move"),
      "promotion move" -> (promotionFacts, promotion, "move is non-promotion"),
      "en passant" -> (enPassantFacts, createMove, "ordinary pawn move or capture"),
      "two-move passer creation" -> (twoMoveCreationFacts, twoMoveFirstStep, "passed pawn on exact after-board"),
      "pawn capture without passed pawn creation" -> (captureOnlyFacts, createMove, "passed pawn on exact after-board")
    ).foreach: (label, corpus) =>
      val (facts, line, missing) = corpus
      val proof = PassedPawnCreatedProof.fromBoardFacts(facts, line)
      assertEquals(proof.complete, false, label)
      assert(proof.missingEvidence.exists(_.missing.contains(missing)), label)
      assertEquals(ScenePassedPawnCreated.write(facts, line), None, label)

    val captureOnlyStory = ScenePawnCapture.write(captureOnlyFacts, createMove).get
    val captureOnlyVerdict = StoryTable.choose(Vector(captureOnlyStory)).head
    val captureOnlyPlan = ExplanationPlan.fromSelected(captureOnlyVerdict).get
    assertEquals(captureOnlyPlan.allowedClaim.map(_.key), Some("captures_rival_pawn"))
    assertEquals(DeterministicRenderer.fromPlan(captureOnlyPlan).map(_.text), Some("exd6 captures the pawn on d6."))

    val positiveStory = ScenePassedPawnCreated.write(positiveFacts, createMove).get
    val positiveVerdict = StoryTable.choose(Vector(positiveStory)).head
    val positivePlan = ExplanationPlan.fromSelected(positiveVerdict).get
    val positiveRendered = DeterministicRenderer.fromPlan(positivePlan).get
    Vector(
      "exd6 creates a passed pawn and threatens promotion.",
      "exd6 creates an unstoppable passed pawn.",
      "exd6 creates a winning passed pawn.",
      "exd6 wins because the passed pawn cannot be stopped."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(positivePlan, positiveRendered, output).accepted, false, output)

  test("PassedPawnCreated-4 reuses EngineCheck without engine-owned claims"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val reply = Line(Square('e', 8), Square('e', 7))
    val story = ScenePassedPawnCreated.write(facts, move).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val evidenceOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(evidenceOnly.storyBound, false)
    assertEquals(evidenceOnly.status, EngineCheckStatus.Unknown)
    assertEquals(evidenceOnly.publicClaimAllowed, false)

    val supportsStory = ScenePassedPawnCreated.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val supportsVerdict = StoryTable.choose(Vector(supportsStory)).head
    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsPlan.allowedClaim.map(_.key), Some("creates_passed_pawn"))
    assertEquals(supportsRendered.text, "exd6 creates a passed pawn on d6.")

    val capsStory = ScenePassedPawnCreated.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val capsVerdict = StoryTable.choose(Vector(capsStory)).head
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    val refutedStory = ScenePassedPawnCreated.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 0)).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val unknownStory = ScenePassedPawnCreated.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val unknownVerdict = StoryTable.choose(Vector(unknownStory)).head
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
    val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownPlan.allowedClaim.map(_.key), Some("creates_passed_pawn"))
    Vector("engine", "eval", "20", "best move", "only move", "winning", "tablebase", "pawn race").foreach:
      forbidden =>
        assertEquals(unknownRendered.text.toLowerCase.contains(forbidden), false, forbidden)

    Vector(
      "The engine says exd6 creates a passed pawn.",
      "exd6 is +2.20 and creates a passed pawn.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 creates a winning endgame.",
      "exd6 wins by tablebase.",
      "exd6 wins the pawn race."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(supportsPlan, supportsRendered, output).accepted, false, output)

  test("PassedPawnCreated-5 StoryTable keeps existing claim homes ahead of created passer"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def eventProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = 0)

    def materialProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = value)

    def strong(story: Story, value: Int): Story =
      story.copy(proof = eventProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(label: String, verdict: Verdict, expected: ExplanationClaim): RenderedLine =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(verdict.leadAllowed, true, label)
      assertEquals(plan.allowedClaim, Some(expected), label)
      assertEquals(rendered.claimKey, expected.key, label)
      rendered

    def assertExistingHomeKeepsLead(
        rows: Vector[(String, Story)],
        expectedLead: String,
        expectedClaim: ExplanationClaim
    ): Unit =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape, expectedLead)
      assertEquals(shape(rows, shuffled), forwardShape, expectedLead)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.count(_.role == Role.Lead), 1, expectedLead)
      val existingVerdict = forward.find(verdict => rowId(rows, verdict.story) == expectedLead).get
      val createdVerdict = forward.find(verdict => rowId(rows, verdict.story) == "Scene.PassedPawnCreated").get
      val rendered = assertLeadClaim(expectedLead, existingVerdict, expectedClaim)
      assertEquals(createdVerdict.role, Role.Support, expectedLead)
      assertNoStandaloneText(s"$expectedLead keeps PassedPawnCreated support silent", createdVerdict)
      assertEquals(rendered.claimKey == ExplanationClaim.CreatesPassedPawn.key, false, expectedLead)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(40)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val createdFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val createdMove = Line(Square('e', 5), Square('d', 6))
    val created = strong(ScenePassedPawnCreated.write(createdFacts, createdMove).get, 99)
    val pawnCapture = strong(ScenePawnCapture.write(createdFacts, createdMove).get, 90)
    val material = strongMaterial(SceneMaterial.write(createdFacts, createdMove).get, 90)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get, 90)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get, 90)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get, 90)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 90)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = ScenePromotion.write(promotionFacts, promotionMove).get

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = strongMaterial(TacticHanging.write(hangingFacts, hangingMove).get, 90)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 90)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        90
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(
        TacticPin
          .write(
            pinFacts,
            Some(discoveredMove),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get,
        90
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get,
        90
      )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(
        TacticSkewer
          .write(
            skewerFacts,
            Some(skewerMove),
            Some(Square('e', 1)),
            Some(Square('e', 5)),
            Some(Square('e', 8))
          )
          .get,
        90
      )

    Vector(
      ("Scene.PawnBreak", pawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      ("Scene.PawnCapture", pawnCapture, ExplanationClaim.CapturesPawn),
      ("Scene.PawnAdvance", pawnAdvance, ExplanationClaim.AdvancesPassedPawn),
      ("Scene.PawnStop", pawnStop, ExplanationClaim.StopsPassedPawnNextAdvance),
      ("Scene.PromotionThreat", promotionThreat, ExplanationClaim.CreatesPromotionThreat),
      ("Scene.Promotion", promotion, ExplanationClaim.PromotesPawn),
      ("Scene.Material", material, ExplanationClaim.MaterialBalanceChanges),
      ("Tactic.Hanging", hanging, ExplanationClaim.CanWinPiece),
      ("Scene.Defense", defense, ExplanationClaim.DefendsPiece),
      ("Tactic.DiscoveredAttack", discovered, ExplanationClaim.RevealsAttackOnPiece),
      ("Tactic.Pin", pin, ExplanationClaim.PinsPiece),
      ("Tactic.RemoveGuard", removeGuard, ExplanationClaim.RemovesDefender),
      ("Tactic.Skewer", skewer, ExplanationClaim.SkewersPieceToPiece)
    ).foreach: (label, existing, expectedClaim) =>
      assertExistingHomeKeepsLead(
        Vector(label -> existing, "Scene.PassedPawnCreated" -> created),
        label,
        expectedClaim
      )

    val materialCollision = StoryTable.choose(Vector(material, created))
    val materialLead = materialCollision.find(_.story == material).get
    val createdUnderMaterial = materialCollision.find(_.story == created).get
    val materialRendered =
      assertLeadClaim("actual material change now", materialLead, ExplanationClaim.MaterialBalanceChanges)
    assertEquals(materialRendered.claimKey, "material_balance_changes")
    assertEquals(ExplanationPlan.fromSelected(createdUnderMaterial), None)

    val blackCreatedFacts = BoardFacts.fromFen("4k3/8/8/8/3p4/4P3/8/4K3 b - - 0 1").toOption.get
    val blackCreatedMove = Line(Square('d', 4), Square('e', 3))
    val blackCreated = strong(ScenePassedPawnCreated.write(blackCreatedFacts, blackCreatedMove).get, 99)
    val createdRows = Vector("White PassedPawnCreated" -> created, "Black PassedPawnCreated" -> blackCreated)
    val createdForward = StoryTable.choose(createdRows.map(_._2))
    val createdReverse = StoryTable.choose(createdRows.reverse.map(_._2))
    assertEquals(shape(createdRows, createdForward), shape(createdRows, createdReverse))
    assertEquals(createdForward.count(_.role == Role.Lead), 1)
    assert(
      createdForward.forall(verdict =>
        verdict.story.scene != Scene.PassedPawnCreated ||
          ExplanationPlan.fromSelected(verdict).forall(_.allowedClaim.contains(ExplanationClaim.CreatesPassedPawn))
      )
    )

    val leadVerdict = StoryTable.choose(Vector(created)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val leadRendered = DeterministicRenderer.fromPlan(leadPlan).get
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.CreatesPassedPawn))
    Vector(
      "exd6 captures the pawn on d6.",
      "exd6 advances the passed pawn.",
      "exd6 threatens promotion.",
      "exd6 promotes.",
      "exd6 wins material."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(leadPlan, leadRendered, output).accepted, false, output)

    val capped = ScenePassedPawnCreated
      .withEngineCheck(
        created,
        engineCheck(createdFacts, created, createdMove, EngineCheckStatus.Caps)
      )
      .get
    val refutingCheck =
      EngineCheck.fromStory(
        facts = createdFacts,
        story = Some(created),
        engineLine = Some(EngineLine(Vector(createdMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(240)),
        evalAfter = Some(EngineEval(0)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val refuted = ScenePassedPawnCreated.withEngineCheck(created, refutingCheck).get
    Vector("capped PassedPawnCreated" -> capped, "refuted PassedPawnCreated" -> refuted).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertNoStandaloneText(label, verdict)

  test("PassedPawnCreated-6 ExplanationPlan admits only selected uncapped Lead created-passer claim"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePassedPawnCreated.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenClaimKeys =
      Vector(
        "unstoppable_pawn",
        "promotion_threat",
        "will_promote",
        "wins_endgame",
        "converts_advantage",
        "breaks_through",
        "creates_pressure",
        "takes_initiative",
        "best_move",
        "only_move",
        "forced"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.scene, Scene.PassedPawnCreated)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("creates_passed_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assert(
        ExplanationClaim.PassedPawnCreatedForbiddenKeys.contains(forbiddenKey),
        s"PassedPawnCreated missing forbidden claim key: $forbiddenKey"
      )
      assertEquals(
        ExplanationClaim.PassedPawnCreatedAllowed.map(_.key).contains(forbiddenKey),
        false,
        forbiddenKey
      )

    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assert(
        !livePositiveClaimKeys.contains(forbiddenKey),
        s"closed PassedPawnCreated meaning became a live claim key: $forbiddenKey"
      )

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(40)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(ScenePassedPawnCreated.withEngineCheck(story, check).get)).head

    Vector(
      "Support" -> verdict.copy(role = Role.Support, leadAllowed = false),
      "Context" -> verdict.copy(role = Role.Context, leadAllowed = false),
      "Blocked" -> verdict.copy(
        role = Role.Blocked,
        leadAllowed = false,
        engineCheckStatus = Some(EngineCheckStatus.Refutes)
      ),
      "unselected" -> verdict.copy(selected = false),
      "capped" -> checked(EngineCheckStatus.Caps),
      "refuted" -> checked(EngineCheckStatus.Refutes)
    ).foreach: (label, nonStandalone) =>
      assertEquals(ExplanationPlan.fromSelected(nonStandalone), None, label)

  test("PassedPawnCreated-7 DeterministicRenderer phrases only bounded created-passer text"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePassedPawnCreated.write(facts, move).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val forbiddenOutputs =
      Vector(
        "exd6 creates an unstoppable passer on d6.",
        "exd6 will promote.",
        "exd6 wins.",
        "exd6 creates a winning endgame.",
        "exd6 converts the advantage.",
        "exd6 breaks through.",
        "exd6 takes the initiative.",
        "exd6 creates pressure.",
        "exd6 is the best move.",
        "exd6 is the only move.",
        "exd6 forces a win."
      )

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    assertEquals(rendered.text, "exd6 creates a passed pawn on d6.")
    assertEquals(rendered.claimKey, "creates_passed_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenOutputs.foreach: output =>
      val candidate = rendered.copy(text = output)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, candidate), None, output)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector(
      plan.copy(role = Role.Support),
      plan.copy(role = Role.Context),
      plan.copy(role = Role.Blocked, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.AdvancesPassedPawn)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(evidenceLine = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(forbiddenWording = Vector.empty)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

  test("PassedPawnCreated-8 LLM smoke reuses 8B boundary without new chess facts"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = ScenePassedPawnCreated.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("exd6 creates a passed pawn on d6."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "exd6 creates a passed pawn on d6.").accepted, true)

    Vector(
      "renderedText: exd6 creates a passed pawn on d6.",
      "claimKey: creates_passed_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"LLM smoke prompt must include $required")

    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "raw Story",
      "Story:",
      "PassedPawnCreatedProof",
      "PassedPawnObservation",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "role:",
      "scene:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"LLM smoke prompt must not expose $forbiddenInput")

    Vector(
      "raw Story" -> "Raw Story says exd6 creates a passed pawn.",
      "raw PassedPawnCreatedProof" -> "PassedPawnCreatedProof proves exd6.",
      "PassedPawnObservation" -> "PassedPawnObservation marks d6 as passed.",
      "BoardFacts" -> "BoardFacts show the passer.",
      "EngineCheck" -> "EngineCheck supports exd6.",
      "raw PV" -> "Raw PV: exd6 Kd7.",
      "proofFailures" -> "proofFailures are empty."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)
      assert(check.violations.contains("raw_input"), label)

    Vector(
      "new move" -> "exd6 and Nf3 creates a passed pawn on d6.",
      "new line" -> "After exd6 Kd7, White creates a passed pawn on d6.",
      "promotion" -> "exd6 will promote.",
      "unstoppable" -> "exd6 creates an unstoppable passer.",
      "winning" -> "exd6 wins the endgame.",
      "conversion" -> "exd6 converts the advantage.",
      "pressure" -> "exd6 creates pressure."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "will_promote")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromPassedPawnCreatedProof",
      "fromPassedPawnObservation",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    Vector("Story", "PassedPawnCreatedProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach:
      forbiddenInput =>
        assert(!llmParameterNames.contains(forbiddenInput), s"LLM smoke must not accept $forbiddenInput")

  test("PassedPawnCreated Closeout hard cleanup keeps authority homes separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = PassedPawnCreatedProof.fromBoardFacts(facts, move)
    val story = ScenePassedPawnCreated.write(facts, move).get
    val capture = ScenePawnCapture.write(facts, move).get
    val material = SceneMaterial.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val captureCollision = StoryTable.choose(Vector(story, capture))
    val materialCollision = StoryTable.choose(Vector(story, material))
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(facts.seen.passedPawnObservations.exists(_.pawn.square == Square('e', 5)), false)
    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.passedBefore, false)
    assertEquals(proof.passedAfter, true)
    assertEquals(proof.exactlyOneNewPassedPawn, true)

    assertEquals(story.scene, Scene.PassedPawnCreated)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePassedPawnCreated))
    assertEquals(story.passedPawnCreatedProof.exists(_.complete), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.multiTargetProof, None)

    assertEquals(ScenePawnBreak.write(facts, move), None)
    assertEquals(ScenePawnAdvance.write(facts, move), None)
    assertEquals(ScenePawnStop.write(facts, move), None)
    assertEquals(ScenePromotionThreat.write(facts, move), None)
    assertEquals(ScenePromotion.write(facts, move), None)
    assertEquals(captureCollision.head.story.scene, Scene.PawnCapture)
    assertEquals(materialCollision.head.story.scene, Scene.Material)
    assertEquals(
      captureCollision.find(_.story.scene == Scene.PassedPawnCreated).map(_.role),
      Some(Role.Support)
    )
    assertEquals(
      materialCollision.find(_.story.scene == Scene.PassedPawnCreated).map(_.role),
      Some(Role.Support)
    )

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CreatesPassedPawn))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    assertEquals(ExplanationClaim.values.count(_.key == "creates_passed_pawn"), 1)
    Vector(
      "challenges_pawn",
      "captures_rival_pawn",
      "advances_passed_pawn",
      "stops_pawn_advance",
      "threatens_promotion_next",
      "promotes_pawn",
      "wins_material",
      "material_balance_changes",
      "can_win_piece",
      "defends_piece",
      "reveals_attack",
      "pins_piece",
      "removes_defender",
      "skewers_piece",
      "unstoppable_pawn",
      "promotion_threat",
      "will_promote",
      "converts_advantage",
      "wins_endgame",
      "breaks_through",
      "takes_initiative",
      "creates_pressure"
    ).foreach: forbiddenKey =>
      assert(
        !ExplanationClaim.PassedPawnCreatedAllowed.exists(_.key == forbiddenKey),
        s"PassedPawnCreated must not allow $forbiddenKey"
      )

    assertEquals(rendered.text, "exd6 creates a passed pawn on d6.")
    assertEquals(rendered.claimKey, "creates_passed_pawn")
    assertEquals(rendered.strength, "bounded")
    assert(!prompt.contains("PassedPawnCreatedProof"))
    assert(!prompt.contains("PassedPawnObservation"))
    assert(!prompt.contains("BoardFacts"))
    assert(!prompt.contains("EngineCheck"))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    Vector(
      "exd6 is unstoppable.",
      "exd6 creates a promotion threat.",
      "exd6 will promote.",
      "exd6 converts the advantage.",
      "exd6 wins.",
      "exd6 breaks through.",
      "exd6 takes the initiative.",
      "exd6 creates pressure.",
      "exd6 wins material.",
      "exd6 captures the pawn on d6."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    captureCollision
      .find(_.story.scene == Scene.PassedPawnCreated)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None, "PawnCapture keeps PassedPawnCreated support silent")
    materialCollision
      .find(_.story.scene == Scene.PassedPawnCreated)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None, "Material keeps PassedPawnCreated support silent")

  test("PSBNC-0 closes pawn structure break neighborhood without cross-claim ownership"):
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get
    val pawnBreakPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(pawnBreak)).head).get

    assertEquals(pawnBreak.scene, Scene.PawnBreak)
    assertEquals(pawnBreak.writer, Some(StoryWriter.ScenePawnBreak))
    assertEquals(pawnBreak.pawnBreakProof.exists(_.complete), true)
    assertEquals(pawnBreak.pawnCaptureProof, None)
    assertEquals(pawnBreak.passedPawnCreatedProof, None)
    assertEquals(pawnBreakPlan.allowedClaim.map(_.key), Some("challenges_pawn"))
    assertEquals(ScenePawnCapture.write(pawnBreakFacts, pawnBreakMove), None)
    assertEquals(ScenePassedPawnCreated.write(pawnBreakFacts, pawnBreakMove), None)

    val captureOnlyFacts = BoardFacts.fromFen("4k3/4p3/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureMove = Line(Square('e', 5), Square('d', 6))
    val captureOnly = ScenePawnCapture.write(captureOnlyFacts, captureMove).get
    val captureOnlyPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(captureOnly)).head).get

    assertEquals(captureOnly.scene, Scene.PawnCapture)
    assertEquals(captureOnly.writer, Some(StoryWriter.ScenePawnCapture))
    assertEquals(captureOnly.pawnCaptureProof.exists(_.complete), true)
    assertEquals(captureOnly.pawnBreakProof, None)
    assertEquals(captureOnly.passedPawnCreatedProof, None)
    assertEquals(captureOnlyPlan.allowedClaim.map(_.key), Some("captures_rival_pawn"))
    assertEquals(ScenePassedPawnCreated.write(captureOnlyFacts, captureMove), None)

    val creatingCaptureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureEvent = ScenePawnCapture.write(creatingCaptureFacts, captureMove).get
    val createdPasser = ScenePassedPawnCreated.write(creatingCaptureFacts, captureMove).get
    val captureCreatesPasser = StoryTable.choose(Vector(captureEvent, createdPasser))
    val captureVerdict = captureCreatesPasser.find(_.story.scene == Scene.PawnCapture).get
    val createdVerdict = captureCreatesPasser.find(_.story.scene == Scene.PassedPawnCreated).get
    val capturePlan = ExplanationPlan.fromSelected(captureVerdict).get
    val captureRendered = DeterministicRenderer.fromPlan(capturePlan).get

    assertEquals(captureVerdict.role, Role.Lead)
    assertEquals(capturePlan.allowedClaim, Some(ExplanationClaim.CapturesPawn))
    assertEquals(captureEvent.pawnCaptureProof.exists(_.complete), true)
    assertEquals(captureEvent.passedPawnCreatedProof, None)
    assertEquals(createdPasser.passedPawnCreatedProof.exists(_.complete), true)
    assertEquals(createdPasser.pawnCaptureProof, None)
    assertEquals(captureRendered.claimKey, "captures_rival_pawn")
    assertEquals(createdVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(createdVerdict), None)

    val createdAlonePlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(createdPasser)).head).get
    val createdRendered = DeterministicRenderer.fromPlan(createdAlonePlan).get
    assertEquals(createdAlonePlan.allowedClaim.map(_.key), Some("creates_passed_pawn"))
    assertEquals(createdRendered.claimKey, "creates_passed_pawn")
    assertEquals(
      LlmNarrationSmoke.check(createdAlonePlan, createdRendered, "exd6 captures the pawn on d6.").accepted,
      false
    )

    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    assertEquals(
      Vector("challenges_pawn", "captures_rival_pawn", "creates_passed_pawn").distinct.size,
      3
    )

  test("PSBNC-2 duplication audit keeps pawn structure proof homes labels and speech keys unique"):
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreakStory = ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get
    val pawnBreakProof = PawnBreakProof.fromBoardFacts(pawnBreakFacts, pawnBreakMove)
    val pawnBreakPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(pawnBreakStory)).head).get

    val pawnCaptureFacts = BoardFacts.fromFen("4k3/4p3/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnCaptureMove = Line(Square('e', 5), Square('d', 6))
    val pawnCaptureStory = ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureMove).get
    val pawnCaptureProof = PawnCaptureProof.fromBoardFacts(pawnCaptureFacts, pawnCaptureMove)
    val pawnCapturePlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(pawnCaptureStory)).head).get

    val passedPawnCreatedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val passedPawnCreatedStory = ScenePassedPawnCreated.write(passedPawnCreatedFacts, pawnCaptureMove).get
    val passedPawnCreatedProof = PassedPawnCreatedProof.fromBoardFacts(passedPawnCreatedFacts, pawnCaptureMove)
    val passedPawnCreatedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(passedPawnCreatedStory)).head).get

    val mappings =
      Vector(
        (
          "direct rival-pawn lever/contact",
          pawnBreakProof.complete,
          pawnBreakStory.scene,
          pawnBreakPlan.allowedClaim.map(_.key),
          classOf[PawnBreakProof].getSimpleName
        ),
        (
          "pawn captures rival pawn",
          pawnCaptureProof.complete,
          pawnCaptureStory.scene,
          pawnCapturePlan.allowedClaim.map(_.key),
          classOf[PawnCaptureProof].getSimpleName
        ),
        (
          "newly-created passed pawn",
          passedPawnCreatedProof.complete,
          passedPawnCreatedStory.scene,
          passedPawnCreatedPlan.allowedClaim.map(_.key),
          classOf[PassedPawnCreatedProof].getSimpleName
        )
      )

    assertEquals(
      mappings.map((_, _, scene, _, _) => scene),
      Vector(Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated)
    )
    assertEquals(
      mappings.map((_, _, _, claim, _) => claim),
      Vector(Some("challenges_pawn"), Some("captures_rival_pawn"), Some("creates_passed_pawn"))
    )
    assertEquals(mappings.forall((_, complete, _, _, _) => complete), true)
    assertEquals(mappings.map((_, _, scene, _, _) => scene).distinct.size, 3)
    assertEquals(mappings.flatMap((_, _, _, claim, _) => claim).distinct.size, 3)
    assertEquals(
      mappings.map((_, _, _, _, proofHome) => proofHome),
      Vector("PawnBreakProof", "PawnCaptureProof", "PassedPawnCreatedProof")
    )

    assertEquals(pawnBreakStory.pawnBreakProof.exists(_.complete), true)
    assertEquals(pawnBreakStory.pawnCaptureProof, None)
    assertEquals(pawnBreakStory.passedPawnCreatedProof, None)
    assertEquals(pawnCaptureStory.pawnCaptureProof.exists(_.complete), true)
    assertEquals(pawnCaptureStory.pawnBreakProof, None)
    assertEquals(pawnCaptureStory.passedPawnCreatedProof, None)
    assertEquals(passedPawnCreatedStory.passedPawnCreatedProof.exists(_.complete), true)
    assertEquals(passedPawnCreatedStory.pawnBreakProof, None)
    assertEquals(passedPawnCreatedStory.pawnCaptureProof, None)

    val breakSurface = classOf[PawnBreakProof].getDeclaredMethods.map(_.getName).toSet
    Vector("capturedPawn", "captureSquare", "legalPawnCapture", "pawnCapturesPawn").foreach: forbidden =>
      assert(!breakSurface.contains(forbidden), s"PawnBreakProof must not expose capture proof field $forbidden")

    val captureSurface = classOf[PawnCaptureProof].getDeclaredMethods.map(_.getName).toSet
    Vector("createdPassedPawn", "passedBefore", "passedAfter", "exactlyOneNewPassedPawn").foreach:
      forbidden =>
        assert(!captureSurface.contains(forbidden), s"PawnCaptureProof must not expose passed-pawn proof field $forbidden")
    Vector("material", "conversion", "weak", "file").foreach: forbidden =>
      assert(
        !captureSurface.exists(_.toLowerCase.contains(forbidden)),
        s"PawnCaptureProof must not expose $forbidden meaning"
      )

    val createdSurface = classOf[PassedPawnCreatedProof].getDeclaredMethods.map(_.getName).toSet
    Vector("capturedPawn", "captureSquare", "legalPawnCapture", "pawnCapturesPawn").foreach: forbidden =>
      assert(!createdSurface.contains(forbidden), s"PassedPawnCreatedProof must not expose capture proof field $forbidden")
    Vector("targetPawn", "directPawnLeverAfterMove", "leverCreatedByMove", "singleRivalPawnTarget").foreach:
      forbidden =>
        assert(!createdSurface.contains(forbidden), s"PassedPawnCreatedProof must not expose pawn-break field $forbidden")

  test("PSBNC-3 StoryTable interaction audit keeps pawn structure rows stable against existing homes"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def materialProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = value, pieceSupport = value, pawnSupport = 0)

    def strong(story: Story, value: Int): Story =
      story.copy(proof = strongProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key),
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey)
        )

    def assertStable(rows: Vector[(String, Story)]): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape)
      assertEquals(shape(rows, shuffled), forwardShape)
      assertEquals(forward.count(_.role == Role.Lead), 1)
      forward

    def assertLeadHome(label: String, verdict: Verdict, expectedScene: Scene, expectedClaim: ExplanationClaim): Unit =
      val plan = ExplanationPlan.fromSelected(verdict).getOrElse(fail(s"$label must lower to ExplanationPlan"))
      val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail(s"$label must render standalone text"))
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(verdict.story.scene, expectedScene, label)
      assertEquals(plan.allowedClaim, Some(expectedClaim), label)
      assertEquals(rendered.claimKey, expectedClaim.key, label)

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get, 90)

    val creatingCaptureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingCaptureMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = strong(ScenePawnCapture.write(creatingCaptureFacts, creatingCaptureMove).get, 90)
    val passedPawnCreated = strong(ScenePassedPawnCreated.write(creatingCaptureFacts, creatingCaptureMove).get, 99)
    val material = strongMaterial(SceneMaterial.write(creatingCaptureFacts, creatingCaptureMove).get, 90)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get, 90)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get, 90)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 90)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = ScenePromotion.write(promotionFacts, promotionMove).get

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = strongMaterial(TacticHanging.write(hangingFacts, hangingMove).get, 90)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 90)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        90
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(
        TacticPin
          .write(
            pinFacts,
            Some(discoveredMove),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get,
        90
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get,
        90
      )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(
        TacticSkewer
          .write(
            skewerFacts,
            Some(skewerMove),
            Some(Square('e', 1)),
            Some(Square('e', 5)),
            Some(Square('e', 8))
          )
          .get,
        90
      )

    val allRows =
      Vector(
        "Scene.PawnBreak" -> pawnBreak,
        "Scene.PawnCapture" -> pawnCapture,
        "Scene.PassedPawnCreated" -> passedPawnCreated,
        "Scene.PawnAdvance" -> pawnAdvance,
        "Scene.PawnStop" -> pawnStop,
        "Scene.PromotionThreat" -> promotionThreat,
        "Scene.Promotion" -> promotion,
        "Scene.Material" -> material,
        "Tactic.Hanging" -> hanging,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered,
        "Tactic.Pin" -> pin,
        "Tactic.RemoveGuard" -> removeGuard,
        "Tactic.Skewer" -> skewer
      )
    val allVerdicts = assertStable(allRows)
    allVerdicts.filter(_.role != Role.Lead).foreach: verdict =>
      assertNoStandaloneText(s"${rowId(allRows, verdict.story)} non-Lead must not speak", verdict)

    allRows.foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      val expectedClaim =
        label match
          case "Scene.PawnBreak" => ExplanationClaim.ChallengesPawnDirectly
          case "Scene.PawnCapture" => ExplanationClaim.CapturesPawn
          case "Scene.PassedPawnCreated" => ExplanationClaim.CreatesPassedPawn
          case "Scene.PawnAdvance" => ExplanationClaim.AdvancesPassedPawn
          case "Scene.PawnStop" => ExplanationClaim.StopsPassedPawnNextAdvance
          case "Scene.PromotionThreat" => ExplanationClaim.CreatesPromotionThreat
          case "Scene.Promotion" => ExplanationClaim.PromotesPawn
          case "Scene.Material" => ExplanationClaim.MaterialBalanceChanges
          case "Tactic.Hanging" => ExplanationClaim.CanWinPiece
          case "Scene.Defense" => ExplanationClaim.DefendsPiece
          case "Tactic.DiscoveredAttack" => ExplanationClaim.RevealsAttackOnPiece
          case "Tactic.Pin" => ExplanationClaim.PinsPiece
          case "Tactic.RemoveGuard" => ExplanationClaim.RemovesDefender
          case "Tactic.Skewer" => ExplanationClaim.SkewersPieceToPiece
      assertLeadHome(label, verdict, story.scene, expectedClaim)

    val captureCreatedRows = Vector("Scene.PawnCapture" -> pawnCapture, "Scene.PassedPawnCreated" -> passedPawnCreated)
    val captureCreated = assertStable(captureCreatedRows)
    val captureLead = captureCreated.find(_.story == pawnCapture).get
    val createdSupport = captureCreated.find(_.story == passedPawnCreated).get
    assertLeadHome("Scene.PawnCapture", captureLead, Scene.PawnCapture, ExplanationClaim.CapturesPawn)
    assertNoStandaloneText("PassedPawnCreated must not own PawnCapture event", createdSupport)
    assertEquals(pawnCapture.passedPawnCreatedProof, None)
    assertEquals(passedPawnCreated.pawnCaptureProof, None)

    val materialCreatedRows = Vector("Scene.Material" -> material, "Scene.PassedPawnCreated" -> passedPawnCreated)
    val materialCreated = assertStable(materialCreatedRows)
    val materialLead = materialCreated.find(_.story == material).get
    val createdUnderMaterial = materialCreated.find(_.story == passedPawnCreated).get
    assertLeadHome("Scene.Material", materialLead, Scene.Material, ExplanationClaim.MaterialBalanceChanges)
    assertNoStandaloneText("actual material change keeps PassedPawnCreated silent", createdUnderMaterial)

    val breakRows = Vector("Scene.PawnBreak" -> pawnBreak, "Scene.PawnCapture" -> pawnCapture, "Scene.PassedPawnCreated" -> passedPawnCreated)
    val breakVerdicts = assertStable(breakRows)
    val breakVerdict = breakVerdicts.find(_.story == pawnBreak).get
    if breakVerdict.role == Role.Lead then
      assertLeadHome("Scene.PawnBreak", breakVerdict, Scene.PawnBreak, ExplanationClaim.ChallengesPawnDirectly)
    else assertNoStandaloneText("PawnBreak non-Lead must not speak capture or passer creation", breakVerdict)
    assertEquals(pawnBreak.pawnCaptureProof, None)
    assertEquals(pawnBreak.passedPawnCreatedProof, None)

    assertLeadHome(
      "Scene.PromotionThreat",
      StoryTable.choose(Vector(promotionThreat)).head,
      Scene.PromotionThreat,
      ExplanationClaim.CreatesPromotionThreat
    )
    assertLeadHome(
      "Scene.Promotion",
      StoryTable.choose(Vector(promotion)).head,
      Scene.Promotion,
      ExplanationClaim.PromotesPawn
    )

    val cappedCapture =
      ScenePawnCapture
        .withEngineCheck(
          pawnCapture,
          engineCheck(creatingCaptureFacts, pawnCapture, creatingCaptureMove, EngineCheckStatus.Caps)
        )
        .get
    val refutedCapture =
      ScenePawnCapture
        .withEngineCheck(
          pawnCapture,
          engineCheck(creatingCaptureFacts, pawnCapture, creatingCaptureMove, EngineCheckStatus.Refutes)
        )
        .get
    Vector("capped PawnCapture" -> cappedCapture, "refuted PawnCapture" -> refutedCapture).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertNoStandaloneText(label, verdict)

  test("PSBNC-4 downstream boundary audit keeps pawn structure speech no stronger than selected claim"):
    val allowedClaimKeys =
      Vector("challenges_pawn", "captures_rival_pawn", "creates_passed_pawn")
    val forbiddenClaimKeys =
      Vector(
        "opens_file",
        "weakens_structure",
        "breaks_through",
        "wins_space",
        "wins_pawn",
        "wins_material",
        "promotion_threat",
        "unstoppable_pawn",
        "converts_advantage",
        "creates_pressure",
        "takes_initiative",
        "best_move",
        "only_move",
        "forced"
      )

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def promptClaimLines(prompt: String): Vector[String] =
      prompt.linesIterator.map(_.trim).filter(_.startsWith("claimKey:")).toVector

    def assertExisting8BBoundary(label: String, prompt: String, expectedClaimKey: String): Unit =
      Vector(
        "instruction: Rephrase only. Do not add chess facts.",
        "renderedText:",
        "claimKey:",
        "strength:",
        "forbiddenWording:"
      ).foreach: required =>
        assert(prompt.contains(required), s"$label prompt missing $required")
      assertEquals(promptClaimLines(prompt), Vector(s"claimKey: $expectedClaimKey"), label)
      Vector(
        "BoardFacts",
        "Story(",
        "Verdict(",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "Proof(",
        "proofFailures",
        "sourceRow",
        "raw PV",
        "routeSan"
      ).foreach: forbiddenInput =>
        assertEquals(prompt.contains(forbiddenInput), false, s"$label prompt leaked $forbiddenInput")

    def assertRendererSurfaceOnlyPlan(): Unit =
      val fromPlanShapes =
        DeterministicRenderer.getClass.getMethods
          .filter(_.getName == "fromPlan")
          .map(_.getParameterTypes.toVector)
          .toVector
      assert(
        fromPlanShapes.exists(_ == Vector(classOf[ExplanationPlan])),
        s"DeterministicRenderer.fromPlan must accept ExplanationPlan only, found $fromPlanShapes"
      )
      assertEquals(
        fromPlanShapes.exists(_.exists(param => param != classOf[ExplanationPlan])),
        false,
        s"DeterministicRenderer.fromPlan must not expose raw downstream inputs: $fromPlanShapes"
      )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertSliceBoundary(
        label: String,
        facts: BoardFacts,
        line: Line,
        leadStory: Story,
        supportStory: Story,
        cappedStory: Story,
        refutedStory: Story,
        expectedClaim: ExplanationClaim,
        badOutputs: Vector[String]
    ): Unit =
      val leadVerdict = StoryTable.choose(Vector(leadStory)).head
      val plan = ExplanationPlan.fromSelected(leadVerdict).getOrElse(fail(s"$label must lower Lead to plan"))
      val rendered = DeterministicRenderer.fromPlan(plan).getOrElse(fail(s"$label must render"))
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).getOrElse(fail(s"$label must build 8B prompt"))

      assertEquals(leadVerdict.role, Role.Lead, label)
      assertEquals(leadVerdict.selected, true, label)
      assertEquals(leadVerdict.engineStrengthLimited, false, label)
      assertEquals(plan.allowedClaim, Some(expectedClaim), label)
      assertEquals(allowedClaimKeys.contains(expectedClaim.key), true, label)
      assertEquals(rendered.claimKey, expectedClaim.key, label)
      assertEquals(rendered.forbiddenCheckPassed, true, label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text), label)
      assertExisting8BBoundary(label, prompt, expectedClaim.key)

      forbiddenClaimKeys.foreach: forbiddenKey =>
        assertEquals(plan.allowedClaim.exists(_.key == forbiddenKey), false, s"$label plan claim $forbiddenKey")
        assertEquals(rendered.claimKey == forbiddenKey, false, s"$label rendered claim $forbiddenKey")
        assertEquals(
          promptClaimLines(prompt).exists(_.stripPrefix("claimKey: ").trim == forbiddenKey),
          false,
          s"$label prompt claim $forbiddenKey"
        )

      badOutputs.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label allowed: $output")

      val nonLeadVerdict = StoryTable.choose(Vector(leadStory, supportStory)).find(_.story == supportStory).get
      assert(nonLeadVerdict.role != Role.Lead, s"$label support row must not become Lead")
      assertNoStandaloneText(s"$label support/context row", nonLeadVerdict)

      Vector("capped" -> cappedStory, "refuted" -> refutedStory).foreach: (caseLabel, row) =>
        val verdict = StoryTable.choose(Vector(row)).head
        assertEquals(verdict.selected, true, s"$label $caseLabel remains selected diagnostic row")
        assertNoStandaloneText(s"$label $caseLabel row", verdict)

      assertEquals(
        EngineCheck.fromStory(
          facts = facts,
          story = Some(leadStory),
          engineLine = Some(EngineLine(Vector(line))),
          replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
          evalBefore = Some(EngineEval(40)),
          evalAfter = Some(EngineEval(45)),
          depth = Some(18),
          freshnessPly = Some(0),
          requestedStatus = EngineCheckStatus.Supports
        ).status,
        EngineCheckStatus.Supports,
        label
      )

    assertRendererSurfaceOnlyPlan()
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get
    val pawnBreakSupport = pawnBreak.copy(proof = pawnBreak.proof.copy(clarity = 99))
    val pawnBreakCapped =
      ScenePawnBreak
        .withEngineCheck(pawnBreak, engineCheck(pawnBreakFacts, pawnBreak, pawnBreakMove, EngineCheckStatus.Caps))
        .get
    val pawnBreakRefuted =
      ScenePawnBreak
        .withEngineCheck(pawnBreak, engineCheck(pawnBreakFacts, pawnBreak, pawnBreakMove, EngineCheckStatus.Refutes))
        .get
    assertSliceBoundary(
      "Scene.PawnBreak",
      pawnBreakFacts,
      pawnBreakMove,
      pawnBreak,
      pawnBreakSupport,
      pawnBreakCapped,
      pawnBreakRefuted,
      ExplanationClaim.ChallengesPawnDirectly,
      Vector(
        "e5 opens the file.",
        "e5 weakens the structure.",
        "e5 breaks through.",
        "e5 wins space.",
        "e5 creates pressure.",
        "e5 is the best move.",
        "e5 is forced."
      )
    )

    val pawnCaptureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnCaptureMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureMove).get
    val pawnCaptureSupport = pawnCapture.copy(proof = pawnCapture.proof.copy(clarity = 99))
    val pawnCaptureCapped =
      ScenePawnCapture
        .withEngineCheck(
          pawnCapture,
          engineCheck(pawnCaptureFacts, pawnCapture, pawnCaptureMove, EngineCheckStatus.Caps)
        )
        .get
    val pawnCaptureRefuted =
      ScenePawnCapture
        .withEngineCheck(
          pawnCapture,
          engineCheck(pawnCaptureFacts, pawnCapture, pawnCaptureMove, EngineCheckStatus.Refutes)
        )
        .get
    assertSliceBoundary(
      "Scene.PawnCapture",
      pawnCaptureFacts,
      pawnCaptureMove,
      pawnCapture,
      pawnCaptureSupport,
      pawnCaptureCapped,
      pawnCaptureRefuted,
      ExplanationClaim.CapturesPawn,
      Vector(
        "exd6 wins a pawn.",
        "exd6 wins material.",
        "exd6 opens the file.",
        "exd6 creates a passed pawn.",
        "exd6 takes the initiative.",
        "exd6 is the only move.",
        "exd6 is forced."
      )
    )

    val passedPawnCreated = ScenePassedPawnCreated.write(pawnCaptureFacts, pawnCaptureMove).get
    val passedPawnCreatedSupport = passedPawnCreated.copy(proof = passedPawnCreated.proof.copy(clarity = 99))
    val passedPawnCreatedCapped =
      ScenePassedPawnCreated
        .withEngineCheck(
          passedPawnCreated,
          engineCheck(pawnCaptureFacts, passedPawnCreated, pawnCaptureMove, EngineCheckStatus.Caps)
        )
        .get
    val passedPawnCreatedRefuted =
      ScenePassedPawnCreated
        .withEngineCheck(
          passedPawnCreated,
          engineCheck(pawnCaptureFacts, passedPawnCreated, pawnCaptureMove, EngineCheckStatus.Refutes)
        )
        .get
    assertSliceBoundary(
      "Scene.PassedPawnCreated",
      pawnCaptureFacts,
      pawnCaptureMove,
      passedPawnCreated,
      passedPawnCreatedSupport,
      passedPawnCreatedCapped,
      passedPawnCreatedRefuted,
      ExplanationClaim.CreatesPassedPawn,
      Vector(
        "exd6 creates a promotion threat.",
        "exd6 creates an unstoppable pawn.",
        "exd6 converts the advantage.",
        "exd6 wins material.",
        "exd6 breaks through.",
        "exd6 creates pressure.",
        "exd6 is the best move."
      )
    )

  test("PSBNC-5 forbidden wording audit keeps closed pawn structure wording non-authoritative"):
    val forbiddenPhrases =
      Vector(
        "opens file",
        "opens position",
        "weakens structure",
        "breakthrough",
        "breaks through",
        "wins space",
        "wins pawn",
        "wins a pawn",
        "wins material",
        "creates passed pawn",
        "creates a passed pawn",
        "promotion threat",
        "unstoppable",
        "conversion",
        "initiative",
        "pressure",
        "best move",
        "only move",
        "forced"
      )
    val forbiddenClaimKeys =
      Vector(
        "opens_file",
        "weakens_structure",
        "breaks_through",
        "wins_space",
        "wins_pawn",
        "wins_material",
        "promotion_threat",
        "unstoppable_pawn",
        "converts_advantage",
        "creates_pressure",
        "takes_initiative",
        "best_move",
        "only_move",
        "forced"
      )
    val forbiddenWordingKeys =
      Vector(
        "opens_file",
        "opens_position",
        "weakens_structure",
        "breaks_through",
        "wins_space",
        "wins_pawn",
        "wins_material",
        "promotion_threat",
        "unstoppable_pawn",
        "converts_advantage",
        "creates_pressure",
        "takes_initiative",
        "best_move",
        "only_move",
        "forced"
      )
    val allowedByScene =
      Map(
        Scene.PawnBreak -> Set.empty[String],
        Scene.PawnCapture -> Set.empty[String],
        Scene.PassedPawnCreated -> Set("creates passed pawn", "creates a passed pawn")
      )

    def normalize(text: String): String =
      text
        .toLowerCase(java.util.Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .replaceAll("\\s+", " ")
        .trim

    def containsPhrase(text: String, phrase: String): Boolean =
      s" ${normalize(text)} ".contains(s" ${normalize(phrase)} ")

    def assertNoForbiddenSurface(label: String, text: String, allowed: Set[String] = Set.empty): Unit =
      forbiddenPhrases.filterNot(allowed.contains).foreach: phrase =>
        assertEquals(containsPhrase(text, phrase), false, s"$label leaked forbidden phrase: $phrase in `$text`")

    def assertLlmRejectsAddedFacts(label: String, plan: ExplanationPlan, rendered: RenderedLine): Unit =
      val allowed = allowedByScene.getOrElse(plan.scene, Set.empty)
      val sentenceByPhrase =
        Vector(
          "opens file" -> "The move opens file.",
          "opens position" -> "The move opens position.",
          "weakens structure" -> "The move weakens structure.",
          "breakthrough" -> "This is a breakthrough.",
          "breaks through" -> "The move breaks through.",
          "wins space" -> "The move wins space.",
          "wins pawn" -> "The move wins pawn.",
          "wins a pawn" -> "The move wins a pawn.",
          "wins material" -> "The move wins material.",
          "creates passed pawn" -> "The move creates passed pawn.",
          "creates a passed pawn" -> "The move creates a passed pawn.",
          "promotion threat" -> "The move creates a promotion threat.",
          "unstoppable" -> "The pawn is unstoppable.",
          "conversion" -> "This starts conversion.",
          "initiative" -> "The move takes initiative.",
          "pressure" -> "The move creates pressure.",
          "best move" -> "This is the best move.",
          "only move" -> "This is the only move.",
          "forced" -> "The move is forced."
        )
      sentenceByPhrase.filterNot((phrase, _) => allowed.contains(phrase)).foreach: (phrase, sentence) =>
        assertEquals(
          LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $sentence").accepted,
          false,
          s"$label LLM accepted forbidden added fact: $phrase"
        )

    def selectedSurface(story: Story): (Verdict, ExplanationPlan, RenderedLine, Option[String]) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val smoke = LlmNarrationSmoke.mockNarrate(plan, rendered)
      (verdict, plan, rendered, smoke)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get

    val pawnCaptureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnCaptureMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureMove).get
    val passedPawnCreated = ScenePassedPawnCreated.write(pawnCaptureFacts, pawnCaptureMove).get

    val rows =
      Vector(
        "Scene.PawnBreak" -> pawnBreak,
        "Scene.PawnCapture" -> pawnCapture,
        "Scene.PassedPawnCreated" -> passedPawnCreated
      )
    val proofHomes =
      Vector(
        "PawnBreakProof" -> pawnBreak.pawnBreakProof,
        "PawnCaptureProof" -> pawnCapture.pawnCaptureProof,
        "PassedPawnCreatedProof" -> passedPawnCreated.passedPawnCreatedProof
      )

    proofHomes.foreach: (label, proofHome) =>
      assert(proofHome.nonEmpty, s"$label must remain the proof home")
      assertNoForbiddenSurface(s"$label proof home", label)

    rows.foreach: (label, story) =>
      val (verdict, plan, rendered, smoke) = selectedSurface(story)
      val allowed = allowedByScene.getOrElse(story.scene, Set.empty)
      val publicValueText = verdict.values.mkString(" ")
      val storyPublicNames = Vector(label, story.scene.toString, story.writer.map(_.toString).getOrElse(""))
      val claimKey = plan.allowedClaim.map(_.key).getOrElse("")
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(verdict.engineStrengthLimited, false, label)
      assertEquals(rendered.forbiddenCheckPassed, true, label)
      assertEquals(smoke, Some(rendered.text), label)
      assertEquals(forbiddenClaimKeys.contains(claimKey), false, s"$label claim key $claimKey")
      assertEquals(prompt.contains(s"claimKey: $claimKey"), true, label)
      forbiddenClaimKeys.foreach: key =>
        assertEquals(prompt.contains(s"claimKey: $key"), false, s"$label prompt claim key $key")
      forbiddenWordingKeys.foreach: key =>
        assert(
          plan.forbiddenWording.map(_.key).contains(key),
          s"$label must keep $key only in forbidden wording"
        )

      storyPublicNames.foreach(name => assertNoForbiddenSurface(s"$label Story label", name, allowed))
      assertNoForbiddenSurface(s"$label renderer output", rendered.text, allowed)
      smoke.foreach(text => assertNoForbiddenSurface(s"$label LLM smoke echo", text, allowed))
      assertNoForbiddenSurface(s"$label public values", publicValueText, allowed)
      assertLlmRejectsAddedFacts(label, plan, rendered)

    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    assertEquals(ExplanationClaim.values.map(_.key).contains("weakens_structure"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("breaks_through"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("wins_space"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("wins_pawn"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("wins_material"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("promotion_threat"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("unstoppable_pawn"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("converts_advantage"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("creates_pressure"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("takes_initiative"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("best_move"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("only_move"), false)
    assertEquals(ExplanationClaim.values.map(_.key).contains("forced"), false)

  test("PSBNC-7 Runtime Boundary Audit keeps helpers out of runtime authority"):
    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()

    Vector(
      "PSBNC-",
      "closeout",
      "Runtime Boundary Audit",
      "Pawn Structure / Break Neighborhood Closeout",
      "fixture map",
      "negative corpus"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"runtime source must not contain closeout-only term: $closeoutOnlyTerm"
      )

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        Scene.values.map(_.toString).toVector ++
        Tactic.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector

    Vector("PSBNC", "Closeout", "RuntimeBoundary", "Fixture", "Corpus", "Audit", "Neighborhood")
      .foreach: helperOnlyName =>
        assert(
          !runtimeAuthorityNames.exists(_.contains(helperOnlyName)),
          s"test helper name became runtime authority: $helperOnlyName"
        )

    Vector(
      "PawnTactic",
      "PawnStructure",
      "PawnStrategy",
      "Breakthrough",
      "OpenFile",
      "WeakSquare",
      "UnstoppablePawn",
      "BestMove",
      "OnlyMove",
      "ForcedMove"
    ).foreach: forbiddenAuthorityName =>
      assert(
        !runtimeAuthorityNames.exists(_.contains(forbiddenAuthorityName)),
        s"closed pawn-structure authority name opened at runtime: $forbiddenAuthorityName"
      )

    Vector(
      "weakens_structure",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "promotion_threat",
      "unstoppable_pawn",
      "converts_advantage",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced"
    ).foreach: forbiddenClaimKey =>
      assertEquals(
        ExplanationClaim.values.map(_.key).contains(forbiddenClaimKey),
        false,
        s"closed claim key opened at runtime: $forbiddenClaimKey"
      )

    assertEquals(
      StoryWriter.values.map(_.toString).toVector.filter(Set("ScenePawnBreak", "ScenePawnCapture", "ScenePassedPawnCreated")),
      Vector("ScenePawnBreak", "ScenePawnCapture", "ScenePassedPawnCreated")
    )
    assertEquals(
      Scene.values.toVector.filter(Set(Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated)),
      Vector(Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated)
    )
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))

    Vector(
      "PawnTacticProof",
      "PawnStructureProof",
      "PawnStrategyProof",
      "BreakthroughProof",
      "OpenFileProof",
      "WeakSquareProof"
    ).foreach: forbiddenProofHome =>
      assert(
        !runtimeText.contains(forbiddenProofHome),
        s"closed proof home opened at runtime: $forbiddenProofHome"
      )

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnCaptureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnCaptureMove = Line(Square('e', 5), Square('d', 6))
    val selectedRows =
      Vector(
        ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get -> "e5 challenges the pawn on d6.",
        ScenePawnCapture.write(pawnCaptureFacts, pawnCaptureMove).get -> "exd6 captures the pawn on d6.",
        ScenePassedPawnCreated.write(pawnCaptureFacts, pawnCaptureMove).get -> "exd6 creates a passed pawn on d6."
      )

    selectedRows.foreach: (story, expectedText) =>
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(rendered.text, expectedText)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(expectedText))

    Vector("public route", "production API", "user-facing LLM narration").foreach: publicSurfaceTerm =>
      assert(
        !runtimeText.contains(publicSurfaceTerm),
        s"closed public surface term reached runtime source: $publicSurfaceTerm"
      )

  test("PromotionThreat-0 opens only immediate next-move promotion threat after a legal pawn move"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val promotionRoute = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 6))))
    assertEquals(proof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('e', 7))))
    assertEquals(proof.creatingMove, Some(creatingMove))
    assertEquals(proof.nextPromotionMove, Some(promotionRoute))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionRoute, Some(promotionRoute))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.pawnOnPenultimateRankAfter, true)
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('e', 6)))
    assertEquals(story.target, Some(Square('e', 8)))
    assertEquals(story.route, Some(creatingMove))
    assertEquals(story.promotionThreatProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.scene, Scene.PromotionThreat)
    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")

    Vector(
      ForbiddenWording.ActualPromotion,
      ForbiddenWording.UnstoppablePawn,
      ForbiddenWording.WinningEndgame,
      ForbiddenWording.ConvertsAdvantage,
      ForbiddenWording.PawnRace,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Forced
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.contains(forbidden), s"PromotionThreat must forbid ${forbidden.key}")

    Vector(
      "e7 will promote.",
      "e7 will queen.",
      "e7 cannot be stopped.",
      "e7 wins the endgame.",
      "e7 is the best move.",
      "e7 starts conversion.",
      "e7 wins the pawn race."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PromotionThreat-0 stays silent for actual promotion and non-immediate promotion-looking moves"):
    val notImmediateMove = Line(Square('e', 5), Square('e', 6))
    val notImmediateFacts = BoardFacts.fromFen("k7/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val actualPromotionMove = Line(Square('e', 7), Square('e', 8))
    val actualPromotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val blockedPromotionMove = Line(Square('e', 6), Square('e', 7))
    val blockedPromotionFacts = BoardFacts.fromFen("4r1k1/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get

    val notImmediateProof = PromotionThreatProof.fromBoardFacts(notImmediateFacts, notImmediateMove)
    val actualPromotionProof = PromotionThreatProof.fromBoardFacts(actualPromotionFacts, actualPromotionMove)
    val blockedPromotionProof =
      PromotionThreatProof.fromBoardFacts(blockedPromotionFacts, blockedPromotionMove)

    assertEquals(notImmediateProof.pawnOnPenultimateRankAfter, false)
    assertEquals(notImmediateProof.nextMovePromotionLegal, false)
    assertEquals(notImmediateProof.complete, false)
    assertEquals(ScenePromotionThreat.write(notImmediateFacts, notImmediateMove), None)

    assertEquals(actualPromotionProof.nonPromotionCreatingMove, false)
    assertEquals(actualPromotionProof.exactAfterBoardReplay, false)
    assertEquals(actualPromotionProof.complete, false)
    assertEquals(ScenePromotionThreat.write(actualPromotionFacts, actualPromotionMove), None)

    assertEquals(blockedPromotionProof.pawnOnPenultimateRankAfter, true)
    assertEquals(blockedPromotionProof.nextMovePromotionLegal, false)
    assertEquals(blockedPromotionProof.complete, false)
    assertEquals(ScenePromotionThreat.write(blockedPromotionFacts, blockedPromotionMove), None)

    val advanceStory = ScenePawnAdvance
      .write(BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get, blockedPromotionMove)
      .get
    val forgedPromotionThreat = advanceStory.copy(scene = Scene.PromotionThreat, promotionThreatProof = None)
    val verdict = StoryTable.choose(Vector(forgedPromotionThreat)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("PromotionThreat-1 PromotionThreatProof owns only diagnostic next-move promotion evidence"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val advanceProof = PawnAdvanceProof.fromBoardFacts(facts, creatingMove)
    val stopProof = PawnStopProof.fromBoardFacts(facts, creatingMove)
    val promotionStory = ScenePromotionThreat.write(facts, creatingMove).get
    val advanceStory = ScenePawnAdvance.write(facts, creatingMove).get
    val verdicts = StoryTable.choose(Vector(advanceStory, promotionStory))

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawnBefore.exists(piece => piece.man == Man.Pawn && piece.side == Side.White), true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.nextPromotionMove, Some(nextPromotionMove))
    assertEquals(proof.promotionRoute, Some(nextPromotionMove))
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(advanceProof.publicClaimAllowed, false)
    assertEquals(stopProof.complete, false)
    assertEquals(ScenePawnStop.write(facts, creatingMove), None)
    assertEquals(advanceStory.promotionThreatProof, None)
    assertEquals(advanceStory.scene, Scene.PawnAdvance)
    assertEquals(promotionStory.promotionThreatProof, Some(proof))
    assertEquals(verdicts.find(_.story.scene == Scene.PromotionThreat).map(_.role), Some(Role.Lead))
    assertEquals(verdicts.find(_.story.scene == Scene.PawnAdvance).exists(_.role == Role.Lead), false)

  test("Promotion-1 PromotionProof proves only legal non-capturing promotion event"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionProof.fromBoardFacts(facts, promotionMove)
    val threatProof = PromotionThreatProof.fromBoardFacts(facts, promotionMove)

    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.pawn.exists(piece => piece.side == Side.White && piece.man == Man.Pawn), true)
    assertEquals(proof.originSquare, Some(Square('e', 7)))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionMove, Some(promotionMove))
    assertEquals(proof.nonCapturing, true)
    assertEquals(proof.promotedPiece, Some(Piece(Side.White, Man.Queen, Square('e', 8))))
    assertEquals(proof.exactBoardReplay, true)
    assertEquals(proof.pawnReachesFinalRank, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)

    assertEquals(threatProof.publicClaimAllowed, false)
    assertEquals(threatProof.complete, false)
    assertEquals(threatProof.nonPromotionCreatingMove, false)

    val proofSurface =
      classOf[PromotionProof].getDeclaredFields.map(_.getName).toSet ++
        classOf[PromotionProof].getDeclaredMethods.map(_.getName).toSet
    Vector("wins", "decisive", "conversion", "bestMove", "tablebase", "materialValue", "materialResult")
      .foreach: forbidden =>
        assert(
          !proofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)),
          s"PromotionProof must not expose $forbidden"
        )

  test(
    "Promotion-1 PromotionProof stays diagnostic for captures, non-promotions, and missing same-board proof"
  ):
    val legalFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val captureFacts = BoardFacts.fromFen("k4n2/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPromotionFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val manualFacts = minimalBoardFacts(
      sideLegal = readyMoves(line = Line(Square('e', 7), Square('e', 8))),
      pieces = Vector(
        Piece(Side.White, Man.Pawn, Square('e', 7)),
        Piece(Side.White, Man.King, Square('e', 1)),
        Piece(Side.Black, Man.King, Square('a', 8))
      )
    )

    val negativeCases: Vector[(String, BoardFacts, Line, String)] = Vector(
      ("capture promotion", captureFacts, Line(Square('e', 7), Square('f', 8)), "move is non-capturing"),
      (
        "ordinary pawn move",
        nonPromotionFacts,
        Line(Square('e', 6), Square('e', 7)),
        "pawn reaches final rank"
      ),
      ("non-pawn move", legalFacts, Line(Square('e', 1), Square('e', 2)), "pawn identity"),
      ("missing same-board proof", manualFacts, Line(Square('e', 7), Square('e', 8)), "same-board proof")
    )

    negativeCases.foreach:
      case (label, facts, move, expectedMissing) =>
        val proof = PromotionProof.fromBoardFacts(facts, move)
        assertEquals(proof.complete, false, label)
        assertEquals(proof.publicClaimAllowed, false, label)
        assert(
          proof.missingEvidence.exists(_.missing.contains(expectedMissing)),
          s"$label must report $expectedMissing"
        )

  test("Promotion-2 ScenePromotion writer opens only proof-backed promotion identity"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotion.write(facts, promotionMove).get
    val proof = story.promotionProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotion))
    assertEquals(story.scene, Scene.Promotion)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.originSquare)
    assertEquals(story.route, proof.promotionMove)
    assertEquals(story.routeSan, Some("e8=Q+"))
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.promotionProof, Some(proof))
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)

  test("Promotion-2 ScenePromotion writer rejects threat material conversion and refuted rows"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotion.write(facts, promotionMove).get
    val refutedStory = story.copy(engineCheck =
      Some(
        EngineCheck(
          sameBoardProof = true,
          checkedMove = Some(promotionMove),
          engineLine = Some(EngineLine(Vector(promotionMove))),
          replyLine = Some(EngineLine(Vector(Line(Square('a', 8), Square('a', 7))))),
          evalBefore = Some(EngineEval(100)),
          evalAfter = Some(EngineEval(-100)),
          depth = Some(18),
          freshnessPly = Some(0),
          status = EngineCheckStatus.Refutes,
          missingEvidence = Vector.empty,
          storyBound = true
        )
      )
    )
    val forgedThreat = story.copy(scene = Scene.PromotionThreat)
    val forgedMaterial = story.copy(scene = Scene.Material, proof = story.proof.copy(conversionPrize = 100))
    val forgedConversion = story.copy(scene = Scene.Convert, plan = Some(Plan.Convert))
    val contaminatedThreat = story.copy(promotionThreatProof =
      Some(
        PromotionThreatProof.fromBoardFacts(
          BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get,
          Line(Square('e', 6), Square('e', 7))
        )
      )
    )

    assertEquals(StoryTable.choose(Vector(refutedStory)).head.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(StoryTable.choose(Vector(refutedStory)).head), None)
    Vector(
      "forged threat" -> forgedThreat,
      "forged material" -> forgedMaterial,
      "forged conversion" -> forgedConversion,
      "contaminated PromotionThreat proof" -> contaminatedThreat
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Promotion-3 negative corpus closes near-promotion and contaminated promotion rows"):
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val captureFacts = BoardFacts.fromFen("k4n2/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPromotionFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val missingIdentityFacts = minimalBoardFacts(
      sideLegal = readyMoves(line = Line(Square('e', 7), Square('e', 8))),
      pieces = Vector(
        Piece(Side.White, Man.Pawn, Square('e', 7)),
        Piece(Side.White, Man.King, Square('e', 1)),
        Piece(Side.Black, Man.King, Square('a', 8))
      )
    )
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotionStory = ScenePromotion.write(promotionFacts, promotionMove).get

    val proofNegatives: Vector[(String, BoardFacts, Line, String)] = Vector(
      ("legal move not", promotionFacts, Line(Square('e', 7), Square('e', 6)), "legal promotion move"),
      ("same-board proof 없음", missingIdentityFacts, promotionMove, "same-board proof"),
      ("pawn move가 아님", promotionFacts, Line(Square('e', 1), Square('e', 2)), "pawn identity"),
      (
        "final rank에 도달하지 않음",
        nonPromotionFacts,
        Line(Square('e', 6), Square('e', 7)),
        "pawn reaches final rank"
      ),
      ("promotion piece identity 없음", missingIdentityFacts, promotionMove, "promoted piece identity"),
      ("capture promotion", captureFacts, Line(Square('e', 7), Square('f', 8)), "move is non-capturing")
    )

    proofNegatives.foreach:
      case (label, facts, move, expectedMissing) =>
        val proof = PromotionProof.fromBoardFacts(facts, move)
        assertEquals(proof.complete, false, label)
        assertEquals(ScenePromotion.write(facts, move), None, label)
        assert(
          proof.missingEvidence.exists(_.missing.contains(expectedMissing)),
          s"$label must report $expectedMissing"
        )

    val nearPromotion = ScenePromotion.write(threatFacts, Line(Square('e', 6), Square('e', 7)))
    val threatOnly = ScenePromotionThreat.write(threatFacts, Line(Square('e', 6), Square('e', 7)))
    assertEquals(nearPromotion, None)
    assertEquals(threatOnly.exists(_.scene == Scene.PromotionThreat), true)

    val materialResultAsPromotion =
      promotionStory.copy(proof = promotionStory.proof.copy(conversionPrize = 100))
    val winningConversionTablebaseContamination = Vector(
      "material result를 promotion claim으로 말하려 함" -> materialResultAsPromotion,
      "winning wording 유입" -> promotionStory
        .copy(scene = Scene.Endgame, writer = Some(StoryWriter.ScenePromotion)),
      "conversion wording 유입" -> promotionStory.copy(scene = Scene.Convert, plan = Some(Plan.Convert)),
      "tablebase wording 유입" -> promotionStory.copy(
        scene = Scene.Endgame,
        writer = Some(StoryWriter.ScenePromotion),
        proof = promotionStory.proof.copy(sourceFit = 100)
      )
    )

    winningConversionTablebaseContamination.foreach:
      case (label, row) =>
        val verdict = StoryTable.choose(Vector(row)).head
        assertEquals(verdict.role, Role.Blocked, label)
        assertEquals(verdict.leadAllowed, false, label)
        val plan = ExplanationPlan.fromSelected(verdict)
        assertEquals(plan.flatMap(_.allowedClaim), None, label)
        assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

  test("Promotion-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val replyMove = Line(Square('a', 8), Square('a', 7))
    val story = ScenePromotion.write(facts, promotionMove).get

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(promotionMove))),
        replyLine = Some(EngineLine(Vector(replyMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noEngineText(label: String, verdict: Verdict): Unit =
      val rendered = ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan)
      Vector(
        "engine says",
        "best move",
        "only move",
        "tablebase",
        "winning endgame",
        "forced win",
        "+3.2",
        "40",
        "45"
      ).foreach: forbidden =>
        assertEquals(
          rendered.exists(_.text.toLowerCase.contains(forbidden)),
          false,
          s"$label must not render $forbidden"
        )

    val supports = ScenePromotion.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePromotion.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePromotion.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePromotion.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 7), Square('e', 6))))),
        replyLine = Some(EngineLine(Vector(replyMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val forgedWithoutProof = story.copy(promotionProof = None)

    assertEquals(
      ScenePromotion.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePromotion.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(
      ScenePromotion.withEngineCheck(forgedWithoutProof, supports.engineCheck.get),
      Option.empty[Story],
      "EngineCheck cannot repair or create Promotion without proof"
    )
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create Promotion")

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("promotes_pawn")
    )
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("promotes_pawn")
    )
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("promotes_pawn")
    )
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("promotes_pawn")
    )
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noEngineText(label, verdict)

  test("Promotion-5 StoryTable keeps actual Promotion stable and claim homes separate"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def materialProof(value: Int): Proof =
      strongProof(value).copy(conversionPrize = value)

    def strong(story: Story, value: Int): Story =
      story.copy(proof = strongProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertStable(rows: Vector[(String, Story)], expectedLead: String): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape)
      assertEquals(shape(rows, shuffled), forwardShape)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.count(_.role == Role.Lead), 1)
      forward

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertNoPublicText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(verdict: Verdict, expected: ExplanationClaim): Unit =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(plan.allowedClaim, Some(expected))
      assertEquals(rendered.claimKey, expected.key)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('a', 8), Square('a', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = strong(ScenePromotion.write(promotionFacts, promotionMove).get, 99)
    val promotionVerdict = StoryTable.choose(Vector(promotion)).head
    assertEquals(promotionVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(promotionVerdict).flatMap(DeterministicRenderer.fromPlan),
      Some(RenderedLine("e8=Q+ promotes the pawn.", "promotes_pawn", "bounded", forbiddenCheckPassed = true)),
      "actual Promotion has only bounded event text"
    )

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get, 90)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get, 90)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 90)

    val pawnRows =
      Vector(
        "Scene.PawnAdvance" -> pawnAdvance,
        "Scene.PawnStop" -> pawnStop,
        "Scene.PromotionThreat" -> promotionThreat,
        "Scene.Promotion" -> promotion
      )
    val pawnVerdicts = assertStable(pawnRows, "Scene.Promotion")
    assertNoStandaloneText(
      "PromotionThreat support must not speak actual Promotion",
      pawnVerdicts.find(_.story == promotionThreat).get
    )

    val forgedPromotionAsThreat = promotion.copy(scene = Scene.PromotionThreat)
    val forgedThreatAsPromotion = promotionThreat.copy(scene = Scene.Promotion)
    val threatWithActualPromotionProof = promotionThreat.copy(promotionProof = promotion.promotionProof)
    val promotionAsMaterial = promotion.copy(scene = Scene.Material, proof = materialProof(99))
    Vector(
      "Promotion must not own PromotionThreat meaning" -> forgedPromotionAsThreat,
      "PromotionThreat must not own actual Promotion meaning" -> forgedThreatAsPromotion,
      "PromotionThreat must not absorb PromotionProof" -> threatWithActualPromotionProof,
      "Promotion must not own Material claim" -> promotionAsMaterial
    ).foreach: (label, forged) =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertNoPublicText(label, verdict)

    val materialFacts = BoardFacts.fromFen("k7/8/4P3/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = strongMaterial(SceneMaterial.write(materialFacts, materialMove).get, 90)
    val hanging = strongMaterial(TacticHanging.write(materialFacts, materialMove).get, 90)

    val defenseFacts = BoardFacts.fromFen("k7/8/4P3/5n1P/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 90)

    val lineFacts = BoardFacts.fromFen("7k/8/4P1r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val lineMove = Line(Square('d', 3), Square('f', 4))
    val line =
      strong(
        TacticDiscoveredAttack
          .write(lineFacts, Some(lineMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        90
      )

    Vector(
      ("Scene.Material", material, ExplanationClaim.MaterialBalanceChanges),
      ("Tactic.Hanging", hanging, ExplanationClaim.CanWinPiece),
      ("Scene.Defense", defense, ExplanationClaim.DefendsPiece),
      ("Tactic.DiscoveredAttack", line, ExplanationClaim.RevealsAttackOnPiece)
    ).foreach: (label, existing, expectedClaim) =>
      val rows = Vector(label -> existing, "Scene.Promotion" -> promotion)
      val verdicts = assertStable(rows, label)
      val existingLead = verdicts.find(_.story == existing).get
      val promotionSupport = verdicts.find(_.story == promotion).get
      assertLeadClaim(existingLead, expectedClaim)
      assertNoStandaloneText(s"$label keeps actual Promotion support silent", promotionSupport)

    val cappedPromotion =
      ScenePromotion
        .withEngineCheck(
          promotion,
          engineCheck(promotionFacts, promotion, promotionMove, EngineCheckStatus.Caps)
        )
        .get
    val refutedPromotion =
      ScenePromotion
        .withEngineCheck(
          promotion,
          engineCheck(promotionFacts, promotion, promotionMove, EngineCheckStatus.Refutes)
        )
        .get
    val cappedVerdict = StoryTable.choose(Vector(cappedPromotion)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedPromotion)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText("capped Promotion has no standalone text", cappedVerdict)
    assertNoStandaloneText("refuted Promotion has no standalone text", refutedVerdict)

  test("Promotion-6 ExplanationPlan admits only selected uncapped Lead Promotion claim"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotion.write(facts, promotionMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenClaimKeys =
      Vector(
        "wins_endgame",
        "converts_advantage",
        "decisive",
        "best_move",
        "only_move",
        "forced_win",
        "tablebase_win",
        "unstoppable_pawn",
        "material_gain"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim.map(_.key), Some("promotes_pawn"))
    assertEquals(ExplanationClaim.PromotionAllowed.map(_.key), Vector("promotes_pawn"))
    assertEquals(ExplanationClaim.PromotionForbiddenKeys, forbiddenClaimKeys)
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assertEquals(ExplanationClaim.PromotionAllowed.map(_.key).contains(forbiddenKey), false, forbiddenKey)
      assertEquals(plan.forbiddenWording.map(_.key).contains(forbiddenKey), true, forbiddenKey)

    Vector(
      verdict.copy(selected = false),
      verdict.copy(role = Role.Support, leadAllowed = false, rank = 2),
      verdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
      verdict.copy(
        role = Role.Blocked,
        leadAllowed = false,
        rank = 4,
        engineCheckStatus = Some(EngineCheckStatus.Refutes)
      )
    ).foreach: nonStandalone =>
      assertEquals(ExplanationPlan.fromSelected(nonStandalone), None)

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(promotionMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('a', 8), Square('a', 7))))),
        evalBefore = Some(EngineEval(30)),
        evalAfter = Some(EngineEval(35)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(ScenePromotion.withEngineCheck(story, check).get)).head

    val capped = checked(EngineCheckStatus.Caps)
    val refuted = checked(EngineCheckStatus.Refutes)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None)
    assertEquals(ExplanationPlan.fromSelected(refuted), None)

  test("Promotion-7 DeterministicRenderer phrases only bounded actual promotion event"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotion.write(facts, promotionMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenRouteText =
      Vector(
        "wins",
        "winning endgame",
        "decisive",
        "converts",
        "best move",
        "only move",
        "forces",
        "tablebase win",
        "wins material",
        "cannot be stopped"
      )

    assertEquals(rendered.text, "e8=Q+ promotes the pawn.")
    assertEquals(rendered.claimKey, "promotes_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenRouteText.foreach: forbidden =>
      val forged = plan.copy(routeSan = Some(forbidden))
      assertEquals(DeterministicRenderer.fromPlan(forged), None, forbidden)

    Vector(
      plan.copy(role = Role.Support),
      plan.copy(role = Role.Context),
      plan.copy(role = Role.Blocked, debugOnly = true),
      plan.copy(scene = Scene.PromotionThreat),
      plan.copy(tactic = Some(Tactic.Hanging)),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.CreatesPromotionThreat)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(evidenceLine = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(secondaryTarget = Some(Square('e', 8))),
      plan.copy(forbiddenWording = Vector.empty)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(rendererMethodNames.contains("fromPlan"))
    Vector("fromVerdict", "fromStory", "fromPromotionProof", "fromBoardFacts", "fromEngineCheck").foreach:
      method => assert(!rendererMethodNames.contains(method), s"renderer must not expose $method")
    Vector("Verdict", "Story", "PromotionProof", "BoardFacts", "EngineCheck").foreach: forbiddenInput =>
      assert(!rendererParameterNames.contains(forbiddenInput), s"renderer must not accept $forbiddenInput")

  test("Promotion-8 LLM smoke reuses 8B boundary without new chess facts"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotion.write(facts, promotionMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("e8=Q+ promotes the pawn."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "e8=Q+ promotes the pawn.").accepted, true)

    Vector(
      "renderedText: e8=Q+ promotes the pawn.",
      "claimKey: promotes_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"LLM smoke prompt must include $required")

    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "raw Story",
      "Story:",
      "PromotionProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "role:",
      "scene:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"LLM smoke prompt must not expose $forbiddenInput")

    Vector(
      "raw Story" -> "Raw Story says e8=Q+ promotes the pawn.",
      "raw PromotionProof" -> "PromotionProof proves e8=Q+.",
      "BoardFacts" -> "BoardFacts show the promotion.",
      "EngineCheck" -> "EngineCheck supports e8=Q+.",
      "raw PV" -> "Raw PV: e8=Q+ Kd7.",
      "proofFailures" -> "proofFailures are empty."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)
      assert(check.violations.contains("raw_input"), label)

    Vector(
      "new move" -> "e8=Q+ and Kd2 promotes the pawn.",
      "new line" -> "After e8=Q+ Kd7, White promotes the pawn.",
      "promotion threat" -> "e8=Q+ threatens promotion next.",
      "winning" -> "e8=Q+ wins the endgame.",
      "conversion" -> "e8=Q+ converts the advantage.",
      "tablebase" -> "e8=Q+ is a tablebase win.",
      "material gain" -> "e8=Q+ gains material.",
      "best move" -> "e8=Q+ is the best move.",
      "only move" -> "e8=Q+ is the only move."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "wins_endgame")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromPromotionProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    Vector("Story", "PromotionProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach:
      forbiddenInput =>
        assert(!llmParameterNames.contains(forbiddenInput), s"LLM smoke must not accept $forbiddenInput")

  test("Promotion Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 7), Square('e', 8))
    val proof = PromotionProof.fromBoardFacts(facts, move)
    val story = ScenePromotion.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPromotionMove, true)
    assertEquals(proof.nonCapturing, true)
    assertEquals(proof.exactBoardReplay, true)
    assertEquals(proof.pawnReachesFinalRank, true)
    assertEquals(proof.pawn.map(_.man), Some(Man.Pawn))
    assertEquals(proof.originSquare, Some(Square('e', 7)))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionMove, Some(move))
    assertEquals(
      proof.promotedPiece.map(piece => (piece.side, piece.man, piece.square)),
      Some((Side.White, Man.Queen, Square('e', 8)))
    )

    assertEquals(story.scene, Scene.Promotion)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotion))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.originSquare)
    assertEquals(story.route, proof.promotionMove)
    assertEquals(story.promotionProof, Some(proof))
    assertEquals(story.promotionThreatProof, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.PromotesPawn))
    assertEquals(ExplanationClaim.PromotionAllowed.map(_.key), Vector("promotes_pawn"))
    assertEquals(rendered.text, "e8=Q+ promotes the pawn.")
    assertEquals(rendered.claimKey, "promotes_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      ExplanationClaim.PawnAdvanceAllowed,
      ExplanationClaim.PawnStopAllowed,
      ExplanationClaim.PromotionThreatAllowed,
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: openedHomeClaims =>
      assertEquals(openedHomeClaims.contains(ExplanationClaim.PromotesPawn), false)

    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "wins_endgame",
      "converts_advantage",
      "decisive",
      "tablebase_win",
      "material_gain",
      "capture_promotion",
      "threatens_promotion_next_as_actual",
      "pawn_break",
      "unstoppable_pawn"
    ).foreach: closedClaim =>
      assert(
        !livePositiveClaimKeys.contains(closedClaim),
        s"closed Promotion meaning became a live claim key: $closedClaim"
      )

    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    Vector(
      "capturePromotionProof",
      "tablebaseProof",
      "conversionProof",
      "winningEndgameProof",
      "decisiveProof",
      "materialGainProof",
      "unstoppablePawnProof"
    ).foreach: closedProofHome =>
      assert(!storyFieldNames.contains(closedProofHome), s"closed proof home reached Story: $closedProofHome")

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    ExplanationClaim.PromotionForbiddenKeys.foreach: key =>
      assert(forbiddenKeys.contains(key), s"Promotion closeout must keep $key forbidden")

    Vector(
      "e8=Q+ wins the endgame.",
      "e8=Q+ converts the advantage.",
      "e8=Q+ is decisive.",
      "e8=Q+ is a tablebase win.",
      "e8=Q+ gains material.",
      "e8=Q+ cannot be stopped.",
      "e8=Q+ threatens promotion next.",
      "e8=Q+ is a pawn break.",
      "e8=Q+ is the best move.",
      "e8=Q+ is the only move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val renderedLower = rendered.text.toLowerCase
    Vector(
      "wins",
      "winning",
      "conversion",
      "converts",
      "decisive",
      "tablebase",
      "material",
      "cannot be stopped",
      "threatens",
      "pawn break",
      "best move",
      "only move",
      "forced"
    ).foreach: forbidden =>
      assert(!renderedLower.contains(forbidden), s"closed Promotion wording leaked to renderer: $forbidden")

    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()
    Vector(
      "Promotion Closeout",
      "Hard Cleanup",
      "one live authority document",
      "public route 200",
      "production API",
      "user-facing LLM"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"closeout-only term became runtime authority: $closeoutOnlyTerm"
      )

  test("PromotionThreat-2 ScenePromotionThreat writer binds identity and blocks EngineCheck Refutes"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val proof = story.promotionThreatProof.get

    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.pawnBefore.map(_.square))
    assertEquals(story.route, Some(creatingMove))
    assertEquals(story.secondaryTarget, None)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(proof.nextPromotionMove, Some(nextPromotionMove))

    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(creatingMove))),
      replyLine = Some(EngineLine(Vector(nextPromotionMove))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutingCheck.storyBound, true)
    assertEquals(refutingCheck.evidenceReady, true)
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)

    val refutedStory = ScenePromotionThreat.withEngineCheck(story, refutingCheck).get
    val verdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

    val actualPromotionStory = story.copy(
      route = Some(nextPromotionMove),
      anchor = Some(Square('e', 7)),
      target = Some(Square('e', 8))
    )
    assertEquals(StoryTable.choose(Vector(actualPromotionStory)).head.role, Role.Blocked)

  test("PromotionThreat-3 negative corpus requires legal next-move promotion proof or silence"):
    val legalFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val legalCreatingMove = Line(Square('e', 6), Square('e', 7))
    val legalStory = ScenePromotionThreat.write(legalFacts, legalCreatingMove).get
    val legalVerdict = StoryTable.choose(Vector(legalStory)).head
    val legalPlan = ExplanationPlan.fromSelected(legalVerdict).get
    val legalRendered = DeterministicRenderer.fromPlan(legalPlan).get

    val illegalCreatingMove = Line(Square('e', 6), Square('e', 8))
    val nonPawnMove = Line(Square('e', 1), Square('e', 2))
    val actualPromotionMove = Line(Square('e', 7), Square('e', 8))
    val twoMovesNeeded = Line(Square('e', 5), Square('e', 6))
    val blockedPromotionMove = Line(Square('e', 6), Square('e', 7))
    val manualFacts = minimalBoardFacts(
      sideLegal = readyMoves(line = legalCreatingMove),
      pieces = Vector(
        Piece(Side.White, Man.Pawn, Square('e', 6)),
        Piece(Side.White, Man.King, Square('e', 1)),
        Piece(Side.Black, Man.King, Square('a', 8))
      )
    )
    val nonPawnFacts = BoardFacts.fromFen("k7/8/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val actualPromotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val twoMovesNeededFacts = BoardFacts.fromFen("k7/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val blockedPromotionFacts = BoardFacts.fromFen("4r1k1/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get

    val negativeCases: Vector[(String, BoardFacts, Line, String)] = Vector(
      "illegal creating move" -> (legalFacts, illegalCreatingMove, "legal pawn move"),
      "no same-board proof" -> (manualFacts, legalCreatingMove, "same-board proof"),
      "not a pawn move" -> (nonPawnFacts, nonPawnMove, "pawn identity"),
      "creating move itself promotes" -> (
        actualPromotionFacts,
        actualPromotionMove,
        "creating move is non-promotion"
      ),
      "next promotion move illegal on after-board" -> (
        blockedPromotionFacts,
        blockedPromotionMove,
        "legal next-move promotion"
      ),
      "promotion square cannot be computed" -> (nonPawnFacts, nonPawnMove, "promotion square"),
      "two or more moves still needed" -> (
        twoMovesNeededFacts,
        twoMovesNeeded,
        "pawn on penultimate rank after move"
      )
    ).map((label, data) => (label, data._1, data._2, data._3))

    negativeCases.foreach:
      case (label, facts, move, expectedMissing) =>
        val proof = PromotionThreatProof.fromBoardFacts(facts, move)
        assertEquals(proof.complete, false, label)
        assertEquals(ScenePromotionThreat.write(facts, move), None, label)
        assert(
          proof.missingEvidence.exists(_.missing.contains(expectedMissing)),
          s"$label must report $expectedMissing"
        )

    Vector(
      "e7 is unstoppable.",
      "e7 cannot be stopped.",
      "e7 wins.",
      "e7 wins by tablebase.",
      "e7 converts the endgame.",
      "e7 will queen."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(legalPlan, legalRendered, output).accepted, false, output)

  test("PromotionThreat-4 reuses EngineCheck without creating engine-owned claims"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val nextPromotionMove = Line(Square('e', 7), Square('e', 8))
    val story = ScenePromotionThreat.write(facts, creatingMove).get

    def check(status: EngineCheckStatus, boundStory: Option[Story] = Some(story)): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = boundStory,
        engineLine = Some(EngineLine(Vector(creatingMove))),
        replyLine = Some(EngineLine(Vector(nextPromotionMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(attached: Story): Verdict =
      StoryTable.choose(Vector(attached)).head

    def noEngineText(label: String, verdict: Verdict): Unit =
      val rendered = ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("engine says")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("best move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("only move")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("tablebase")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("winning endgame")), false, label)
      assertEquals(rendered.exists(_.text.toLowerCase.contains("forced win")), false, label)
      assertEquals(rendered.exists(_.text.contains("+3.2")), false, label)
      assertEquals(rendered.exists(_.text.contains("40")), false, label)
      assertEquals(rendered.exists(_.text.contains("45")), false, label)

    val supports = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = ScenePromotionThreat.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val unbound = check(EngineCheckStatus.Supports, boundStory = None)
    val wrongRoute =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 6), Square('e', 8))))),
        replyLine = Some(EngineLine(Vector(nextPromotionMove))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    val forgedWithoutProof = story.copy(promotionThreatProof = None)

    assertEquals(
      ScenePromotionThreat.withEngineCheck(story, unbound),
      Option.empty[Story],
      "EngineCheck cannot attach without Story binding"
    )
    assertEquals(
      ScenePromotionThreat.withEngineCheck(story, wrongRoute),
      Option.empty[Story],
      "EngineCheck cannot attach a different route"
    )
    assertEquals(
      ScenePromotionThreat.withEngineCheck(forgedWithoutProof, supports.engineCheck.get),
      Option.empty[Story],
      "EngineCheck cannot repair or create PromotionThreat without proof"
    )
    assertEquals(
      StoryTable.choose(Vector.empty),
      Vector.empty,
      "EngineCheck alone cannot create PromotionThreat"
    )

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))

    val supportsVerdict = selected(supports)
    val capsVerdict = selected(caps)
    val unknownVerdict = selected(unknown)
    val refutesVerdict = selected(refutes)

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), Option.empty[ExplanationPlan])
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), Option.empty[ExplanationPlan])

    Vector(
      "supports" -> supportsVerdict,
      "caps" -> capsVerdict,
      "unknown" -> unknownVerdict,
      "refutes" -> refutesVerdict
    ).foreach: (label, verdict) =>
      noEngineText(label, verdict)

    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    Vector(
      "engine says e7 is best move",
      "+3.2 and winning endgame",
      "only move by tablebase",
      "forced win",
      "best move",
      "only move",
      "tablebase result",
      "winning endgame"
    ).foreach: phrase =>
      assertEquals(
        LlmNarrationSmoke.mockNarrate(supportsPlan, supportsRendered.copy(text = phrase)),
        None,
        phrase
      )

  test("PromotionThreat-5 StoryTable keeps existing rows stable and claim homes separate"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story, value: Int): Story =
      story.copy(proof = strongProof(value))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertStable(rows: Vector[(String, Story)], expectedLead: String): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_._2))
      val reverse = StoryTable.choose(rows.reverse.map(_._2))
      val shuffled = StoryTable.choose(rows.sortBy(_._1).map(_._2))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape)
      assertEquals(shape(rows, shuffled), forwardShape)
      assertEquals(rowId(rows, forward.head.story), expectedLead)
      assertEquals(forward.head.role, Role.Lead)
      forward

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertLeadClaim(verdict: Verdict, expected: ExplanationClaim): (ExplanationPlan, RenderedLine) =
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(plan.allowedClaim, Some(expected))
      (plan, rendered)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 7), Square('e', 8))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val advanceThreatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceThreatMove = Line(Square('e', 6), Square('e', 7))
    val advance = strong(ScenePawnAdvance.write(advanceThreatFacts, advanceThreatMove).get, 90)
    val promotionThreat = strong(ScenePromotionThreat.write(advanceThreatFacts, advanceThreatMove).get, 99)
    val advanceRows = Vector("Scene.PawnAdvance" -> advance, "Scene.PromotionThreat" -> promotionThreat)
    val advanceVerdicts = assertStable(advanceRows, "Scene.PromotionThreat")
    val advanceSupport = advanceVerdicts.find(_.story == advance).get
    val promotionLead = advanceVerdicts.find(_.story == promotionThreat).get
    val (promotionPlan, promotionRendered) =
      assertLeadClaim(promotionLead, ExplanationClaim.CreatesPromotionThreat)
    assertNoStandaloneText("PawnAdvance support must not speak under PromotionThreat", advanceSupport)
    assertEquals(
      LlmNarrationSmoke.check(promotionPlan, promotionRendered, "e7 advances the passed pawn.").accepted,
      false
    )

    val stopThreatFacts = BoardFacts.fromFen("r5k1/5n2/8/7P/8/4p3/8/3K4 b - - 0 1").toOption.get
    val stopMove = Line(Square('f', 7), Square('h', 6))
    val blackThreatMove = Line(Square('e', 3), Square('e', 2))
    val stop = strong(ScenePawnStop.write(stopThreatFacts, stopMove).get, 99)
    val blackThreat = strong(ScenePromotionThreat.write(stopThreatFacts, blackThreatMove).get, 90)
    val stopRows = Vector("Scene.PawnStop" -> stop, "Scene.PromotionThreat" -> blackThreat)
    val stopVerdicts = assertStable(stopRows, "Scene.PawnStop")
    val (stopPlan, stopRendered) =
      assertLeadClaim(stopVerdicts.head, ExplanationClaim.StopsPassedPawnNextAdvance)
    val stopThreatSupport = stopVerdicts.find(_.story == blackThreat).get
    assertNoStandaloneText("PromotionThreat support must not own PawnStop meaning", stopThreatSupport)
    Vector("Nh6 stops promotion.", "Nh6 prevents the pawn from queening.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(stopPlan, stopRendered, output).accepted, false, output)

    val materialThreatFacts = BoardFacts.fromFen("k7/8/4P3/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val materialThreatMove = Line(Square('e', 6), Square('e', 7))
    val material = strong(SceneMaterial.write(materialThreatFacts, materialMove).get, 90)
    val hanging = strong(TacticHanging.write(materialThreatFacts, materialMove).get, 90)
    val materialThreat = strong(ScenePromotionThreat.write(materialThreatFacts, materialThreatMove).get, 99)

    val defenseThreatFacts = BoardFacts.fromFen("k7/8/4P3/5n1P/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defensePromotionMove = Line(Square('e', 6), Square('e', 7))
    val defense = strong(SceneDefense.write(defenseThreatFacts, defenseThreat, defenseMove).get, 90)
    val defensePromotionThreat =
      strong(ScenePromotionThreat.write(defenseThreatFacts, defensePromotionMove).get, 99)

    val lineThreatFacts = BoardFacts.fromFen("7k/8/4P1r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val lineMove = Line(Square('d', 3), Square('f', 4))
    val linePromotionMove = Line(Square('e', 6), Square('e', 7))
    val line = strong(
      TacticDiscoveredAttack
        .write(lineThreatFacts, Some(lineMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get,
      90
    )
    val linePromotionThreat = strong(ScenePromotionThreat.write(lineThreatFacts, linePromotionMove).get, 99)

    Vector(
      ("Scene.Material", material, materialThreat, ExplanationClaim.MaterialBalanceChanges),
      ("Tactic.Hanging", hanging, materialThreat, ExplanationClaim.CanWinPiece),
      ("Scene.Defense", defense, defensePromotionThreat, ExplanationClaim.DefendsPiece),
      ("Tactic.DiscoveredAttack", line, linePromotionThreat, ExplanationClaim.RevealsAttackOnPiece)
    ).foreach: (label, existing, threat, expectedClaim) =>
      val rows = Vector(label -> existing, "Scene.PromotionThreat" -> threat)
      val verdicts = assertStable(rows, label)
      val existingLead = verdicts.find(_.story == existing).get
      val threatSupport = verdicts.find(_.story == threat).get
      val (plan, rendered) = assertLeadClaim(existingLead, expectedClaim)
      assertNoStandaloneText(s"$label keeps PromotionThreat support silent", threatSupport)
      assertEquals(
        LlmNarrationSmoke
          .check(plan, rendered, s"${plan.routeSan.getOrElse("Move")} creates a next-move promotion threat.")
          .accepted,
        false
      )

    val cappedThreat =
      ScenePromotionThreat
        .withEngineCheck(
          promotionThreat,
          engineCheck(advanceThreatFacts, promotionThreat, advanceThreatMove, EngineCheckStatus.Caps)
        )
        .get
    val refutedThreat =
      ScenePromotionThreat
        .withEngineCheck(
          promotionThreat,
          engineCheck(advanceThreatFacts, promotionThreat, advanceThreatMove, EngineCheckStatus.Refutes)
        )
        .get
    val cappedVerdict = StoryTable.choose(Vector(cappedThreat)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedThreat)).head
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertNoStandaloneText("capped PromotionThreat has no standalone text", cappedVerdict)
    assertNoStandaloneText("refuted PromotionThreat has no standalone text", refutedVerdict)

  test("PromotionThreat-6 ExplanationPlan admits only selected uncapped Lead threat claim"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenClaimKeys =
      Vector(
        "unstoppable_pawn",
        "will_promote",
        "cannot_be_stopped",
        "wins_endgame",
        "converts_advantage",
        "best_move",
        "only_move",
        "forced",
        "tablebase_win",
        "no_counterplay"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes), false)
    assertEquals(plan.allowedClaim.map(_.key), Some("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(ExplanationClaim.PromotionThreatForbiddenKeys, forbiddenClaimKeys)
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assertEquals(
        ExplanationClaim.PromotionThreatAllowed.map(_.key).contains(forbiddenKey),
        false,
        forbiddenKey
      )

    Vector(
      verdict.copy(selected = false),
      verdict.copy(role = Role.Support, leadAllowed = false, rank = 2),
      verdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
      verdict.copy(
        role = Role.Blocked,
        leadAllowed = false,
        rank = 4,
        engineCheckStatus = Some(EngineCheckStatus.Refutes)
      )
    ).foreach: nonStandalone =>
      assertEquals(ExplanationPlan.fromSelected(nonStandalone), None)

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(creatingMove))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 7), Square('e', 8))))),
        evalBefore = Some(EngineEval(30)),
        evalAfter = Some(EngineEval(35)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(ScenePromotionThreat.withEngineCheck(story, check).get)).head

    val capped = checked(EngineCheckStatus.Caps)
    val refuted = checked(EngineCheckStatus.Refutes)

    assertEquals(capped.engineStrengthLimited, true)
    assertEquals(refuted.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(capped), None)
    assertEquals(ExplanationPlan.fromSelected(refuted), None)

  test("PromotionThreat-7 DeterministicRenderer phrases only bounded next promotion threat"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenOutputs =
      Vector(
        "e7 will promote.",
        "e7 cannot be stopped.",
        "e7 is unstoppable.",
        "e7 wins.",
        "e7 wins the winning endgame.",
        "e7 converts the advantage.",
        "e7 is the best move.",
        "e7 is the only move.",
        "e7 forces promotion.",
        "e7 is a tablebase win.",
        "e7 gives no counterplay."
      )

    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    forbiddenOutputs.foreach: output =>
      val candidate = rendered.copy(text = output)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, candidate), None, output)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector(
      plan.copy(role = Role.Support),
      plan.copy(role = Role.Context),
      plan.copy(role = Role.Blocked, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.AdvancesPassedPawn)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(evidenceLine = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(forbiddenWording = Vector.empty)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(!rendererMethodNames.contains("fromVerdict"))
    assert(!rendererMethodNames.contains("fromStory"))
    assert(!rendererMethodNames.contains("fromPromotionThreatProof"))
    Vector("Verdict", "Story", "PromotionThreatProof", "BoardFacts", "EngineCheck").foreach: forbiddenInput =>
      assert(!rendererParameterNames.contains(forbiddenInput), s"renderer must not accept $forbiddenInput")

  test("PromotionThreat-8 LLM smoke reuses 8B boundary without new chess facts"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("e7 threatens to promote next."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "e7 threatens to promote next.").accepted, true)

    Vector(
      "renderedText: e7 threatens to promote next.",
      "claimKey: threatens_promotion_next",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"LLM smoke prompt must include $required")

    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "raw Story",
      "Story:",
      "PromotionThreatProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "proofFailures",
      "role:",
      "scene:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"LLM smoke prompt must not expose $forbiddenInput")

    Vector(
      "raw Story" -> "Raw Story says e7 threatens to promote next.",
      "raw PromotionThreatProof" -> "PromotionThreatProof proves e7.",
      "BoardFacts" -> "BoardFacts show the route.",
      "EngineCheck" -> "EngineCheck supports e7.",
      "raw PV" -> "Raw PV: e7 e8=Q.",
      "proofFailures" -> "proofFailures are empty."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)
      assert(check.violations.contains("raw_input"), label)

    Vector(
      "new move" -> "e7 and Kd2 threaten promotion.",
      "new line" -> "After e7 Kd7, White threatens to promote next.",
      "actual promotion" -> "e7 is an actual promotion.",
      "will promote" -> "e7 will promote.",
      "unstoppable" -> "e7 cannot be stopped.",
      "winning" -> "e7 wins the endgame.",
      "conversion" -> "e7 converts the advantage.",
      "tablebase" -> "e7 is a tablebase win."
    ).foreach: (label, output) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, label)

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val mismatchedRendered = rendered.copy(claimKey = "will_promote")
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None)

    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    Vector(
      "fromStory",
      "fromPromotionThreatProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    Vector("Story", "PromotionThreatProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach:
      forbiddenInput =>
        assert(!llmParameterNames.contains(forbiddenInput), s"LLM smoke must not accept $forbiddenInput")

  test("PromotionThreat Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val creatingMove = Line(Square('e', 6), Square('e', 7))
    val proof = PromotionThreatProof.fromBoardFacts(facts, creatingMove)
    val story = ScenePromotionThreat.write(facts, creatingMove).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.nonPromotionCreatingMove, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.pawnOnPenultimateRankAfter, true)
    assertEquals(proof.nextMovePromotionLegal, true)
    assertEquals(proof.creatingMove, Some(creatingMove))
    assertEquals(proof.nextPromotionMove, Some(Line(Square('e', 7), Square('e', 8))))
    assertEquals(proof.promotionSquare, Some(Square('e', 8)))
    assertEquals(proof.promotionRoute, proof.nextPromotionMove)

    assertEquals(story.scene, Scene.PromotionThreat)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePromotionThreat))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.promotionSquare)
    assertEquals(story.anchor, proof.pawnBefore.map(_.square))
    assertEquals(story.route, proof.creatingMove)
    assertEquals(story.promotionThreatProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CreatesPromotionThreat))
    assertEquals(ExplanationClaim.PromotionThreatAllowed.map(_.key), Vector("threatens_promotion_next"))
    assertEquals(rendered.text, "e7 threatens to promote next.")
    assertEquals(rendered.claimKey, "threatens_promotion_next")
    assertEquals(rendered.strength, "bounded")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    Vector(
      ExplanationClaim.PawnAdvanceAllowed,
      ExplanationClaim.PawnStopAllowed,
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: openedHomeClaims =>
      assertEquals(openedHomeClaims.contains(ExplanationClaim.CreatesPromotionThreat), false)

    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "will_promote",
      "cannot_be_stopped",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "tablebase_win",
      "actual_promotion",
      "pawn_break",
      "promotion_story",
      "pawn_race"
    ).foreach: closedClaim =>
      assert(
        !livePositiveClaimKeys.contains(closedClaim),
        s"closed PromotionThreat meaning became a live claim key: $closedClaim"
      )

    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    Vector(
      "actualPromotionProof",
      "tablebaseProof",
      "conversionProof",
      "winningEndgameProof",
      "unstoppablePawnProof",
      "pawnRaceProof"
    ).foreach: closedProofHome =>
      assert(!storyFieldNames.contains(closedProofHome), s"closed proof home reached Story: $closedProofHome")

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "actual_promotion",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "tablebase_draw",
      "stops_promotion",
      "permanently_stops_pawn",
      "pawn_race",
      "king_route",
      "opposition",
      "passed_pawn_strategy"
    ).foreach: forbiddenKey =>
      assert(
        forbiddenKeys.contains(forbiddenKey),
        s"closed PromotionThreat wording must remain forbidden only: $forbiddenKey"
      )

    Vector(
      "e7 will promote.",
      "e7 cannot be stopped.",
      "e7 is unstoppable.",
      "e7 converts the advantage.",
      "e7 is a tablebase win.",
      "e7 wins material.",
      "e7 is a pawn break.",
      "e7 wins the endgame.",
      "e7 forces promotion.",
      "e7 creates no counterplay."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val renderedLower = rendered.text.toLowerCase
    Vector(
      "will promote",
      "cannot be stopped",
      "unstoppable",
      "conversion",
      "tablebase",
      "wins",
      "winning",
      "pawn break",
      "best move",
      "only move",
      "forced",
      "no counterplay"
    ).foreach: forbidden =>
      assert(
        !renderedLower.contains(forbidden),
        s"closed PromotionThreat wording leaked to renderer: $forbidden"
      )

    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()
    Vector(
      "PromotionThreat Closeout",
      "Hard Cleanup",
      "one live authority document",
      "public route 200",
      "production API",
      "user-facing LLM"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"closeout-only term became runtime authority: $closeoutOnlyTerm"
      )

  test("PawnStop Closeout hard cleanup keeps ownership layers separated and surfaces closed"):
    val facts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stop = Line(Square('g', 7), Square('e', 6))
    val observation =
      facts.seen.passedPawnObservations
        .find(row => row.side == Side.White && row.pawn.square == Square('e', 5))
        .get
    val proof = PawnStopProof.fromBoardFacts(facts, stop)
    val story = ScenePawnStop.write(facts, stop).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(observation.pawn.man, Man.Pawn)
    assertEquals(observation.side, Side.White)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.complete, true)
    assertEquals(proof.targetPawn, Some(observation.pawn))
    assertEquals(proof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(proof.stopMove, Some(stop))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalStopMove, true)
    assertEquals(proof.targetPawnAlreadyPassed, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.nextAdvanceSquareStoppedAfter, true)
    assertEquals(
      proof.nextAdvanceSquareOccupiedAfter || proof.nextAdvanceSquareAttackedAfter || proof.nextAdvanceSquareControlledByPawnAfter,
      true
    )

    assertEquals(story.scene, Scene.PawnStop)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(story.side, proof.side)
    assertEquals(story.rival, proof.rivalSide)
    assertEquals(story.target, proof.nextAdvanceSquare)
    assertEquals(story.anchor, Some(stop.from))
    assertEquals(story.route, proof.stopMove)
    assertEquals(story.pawnStopProof, Some(proof))
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.kingHeat, 0)

    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.DefenseAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.MaterialAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.HangingAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key).contains("stops_pawn_advance"), false)
    assertEquals(ExplanationClaim.SkewerAllowed.map(_.key).contains("stops_pawn_advance"), false)
    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(
        !ExplanationClaim.PawnStopAllowed.map(_.key).contains(key),
        s"$key must not be a PawnStop live claim"
      )

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(rendered.text, "Ne6 stops the passed pawn from advancing next.")
    assertEquals(rendered.claimKey, "stops_pawn_advance")
    assertEquals(rendered.strength, "bounded")
    val renderedLower = rendered.text.toLowerCase
    Vector(
      "promotion",
      "promotes",
      "permanent",
      "for good",
      "draw",
      "tablebase",
      "best defense",
      "only move",
      "wins"
    ).foreach: forbidden =>
      assert(!renderedLower.contains(forbidden), s"PawnStop rendered text must not contain $forbidden")

    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet
    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    ).foreach: key =>
      assert(forbiddenKeys.contains(key), s"PawnStop closeout must keep $key forbidden")
    Vector(
      "Ne6 stops promotion.",
      "Ne6 stops the pawn for good.",
      "Ne6 draws the endgame.",
      "Ne6 is a tablebase draw.",
      "Ne6 is the best defense.",
      "Ne6 is the only move.",
      "Ne6 wins the endgame."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val methodNames =
      (DeterministicRenderer.getClass.getDeclaredMethods ++ LlmNarrationSmoke.getClass.getDeclaredMethods)
        .map(_.getName)
        .toSet
    Vector("callApi", "productionApi", "fromPawnStopProof", "fromBoardFacts", "fromEngineCheck").foreach:
      method =>
        assert(!methodNames.contains(method), s"PawnStop closeout must not expose downstream method $method")

  test("PIH-0 Pawn Interaction Hardening keeps pawn rows below same-board tactic and separated proof homes"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    val sameBoardFacts = BoardFacts.fromFen("7k/8/6r1/4P3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pawnAdvanceMove = Line(Square('e', 5), Square('e', 6))
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val pawnAdvance =
      ScenePawnAdvance.write(sameBoardFacts, pawnAdvanceMove).get.copy(proof = strongProof(99))
    val discovered =
      TacticDiscoveredAttack
        .write(sameBoardFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(99))

    val sameBoardRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Tactic.DiscoveredAttack" -> discovered)
    val forward = StoryTable.choose(sameBoardRows.map(_._2))
    val reverse = StoryTable.choose(sameBoardRows.reverse.map(_._2))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict =>
        (
          rowId(sameBoardRows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim)
        )
      )

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.find(_.story == discovered).map(_.role), Some(Role.Lead))
    assertEquals(forward.find(_.story == pawnAdvance).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(forward.find(_.story == pawnAdvance).get), None)
    val discoveredPlan = ExplanationPlan.fromSelected(forward.find(_.story == discovered).get).get
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    assertEquals(discoveredPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    Vector(
      "Nf4 will promote the pawn.",
      "Nf4 converts the endgame.",
      "Nf4 is the best move.",
      "Nf4 advances the passed pawn."
    ).foreach: output =>
      assertEquals(
        LlmNarrationSmoke.check(discoveredPlan, discoveredRendered, output).accepted,
        false,
        output
      )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val contaminatedAdvance =
      pawnAdvance.copy(pawnStopProof = Some(PawnStopProof.fromBoardFacts(stopFacts, stopMove)))
    val contaminatedStop =
      ScenePawnStop.write(stopFacts, stopMove).get.copy(pawnAdvanceProof = pawnAdvance.pawnAdvanceProof)

    Vector(
      "PawnAdvance with PawnStopProof" -> contaminatedAdvance,
      "PawnStop with PawnAdvanceProof" -> contaminatedStop
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)

  test("PIH-1 fixture map covers pawn interaction hardening categories"):
    final case class PihFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        candidateLegalLines: Vector[Line],
        rows: Vector[(String, Story)],
        expectedOpenRows: Set[String],
        expectedBlockedRows: Set[String],
        expectedRoles: Map[String, Role],
        expectedSelectedVerdict: String,
        expectedSelectedRole: Role,
        forbiddenClaims: Vector[String]
    )

    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "promotion_threat" => s"$san will promote next."
        case "threatens_promotion_next" => s"$san will promote next."
        case "stops_promotion" => s"$san stops promotion."
        case "converts_advantage" => s"$san converts the advantage."
        case "wins_endgame" => s"$san wins the endgame."
        case "draws_endgame" => s"$san draws the endgame."
        case "tablebase_draw" => s"$san is a tablebase draw."
        case "best_move" => s"$san is the best move."
        case "only_move" => s"$san is the only move."
        case "forced" => s"$san is forced."
        case "defends_piece" => s"$san defends the piece."
        case "reveals_attack_on_piece" => s"$san reveals an attack."
        case "pins_piece" => s"$san pins the piece."
        case "removes_defender" => s"$san removes the defender."
        case "skewers_piece_to_piece" => s"$san skewers the piece."
        case "material_balance_changes" => s"$san changes the material balance."
        case "advances_passed_pawn" => s"$san advances the passed pawn."
        case "stops_pawn_advance" => s"$san stops the passed pawn from advancing next."
        case other => s"$san adds $other."

    def assertFixture(fixture: PihFixture): Unit =
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.candidateLegalLines.foreach: line =>
        assert(facts.sideLegal.lines.contains(line), s"${fixture.category} legal line missing: $line")
      assertEquals(
        fixture.rows.map(_._1).toSet,
        fixture.expectedOpenRows ++ fixture.expectedBlockedRows,
        fixture.category
      )

      val forward = StoryTable.choose(fixture.rows.map(_._2))
      val reverse = StoryTable.choose(fixture.rows.reverse.map(_._2))

      def shape(verdicts: Vector[Verdict]) =
        verdicts.map(verdict =>
          (
            rowId(fixture.rows, verdict.story),
            verdict.role,
            verdict.leadAllowed,
            verdict.engineCheckStatus,
            verdict.engineStrengthLimited
          )
        )

      val roleMap = forward.map(verdict => rowId(fixture.rows, verdict.story) -> verdict.role).toMap
      val selected = forward.head
      val selectedId = rowId(fixture.rows, selected.story)

      assertEquals(shape(forward), shape(reverse), fixture.category)
      assertEquals(roleMap, fixture.expectedRoles, fixture.category)
      fixture.expectedOpenRows.foreach: id =>
        assert(roleMap.get(id).exists(_ != Role.Blocked), s"${fixture.category} open row blocked: $id")
      fixture.expectedBlockedRows.foreach: id =>
        assertEquals(roleMap.get(id), Some(Role.Blocked), s"${fixture.category} blocked row role: $id")
      assertEquals(selectedId, fixture.expectedSelectedVerdict, fixture.category)
      assertEquals(selected.role, fixture.expectedSelectedRole, fixture.category)
      assertEquals(
        forward.count(_.role == Role.Lead),
        fixture.expectedRoles.values.count(_ == Role.Lead),
        fixture.category
      )

      forward
        .filter(_.role != Role.Lead)
        .foreach: verdict =>
          assertEquals(
            ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
            None,
            verdict.toString
          )

      if selected.role == Role.Lead && !selected.engineStrengthLimited then
        val plan = ExplanationPlan.fromSelected(selected).get
        val rendered = DeterministicRenderer.fromPlan(plan).get
        fixture.forbiddenClaims.foreach: claim =>
          val output = forbiddenOutput(claim, plan)
          assertEquals(
            LlmNarrationSmoke.check(plan, rendered, output).accepted,
            false,
            s"${fixture.category}: $claim"
          )
      else assertEquals(ExplanationPlan.fromSelected(selected), None, fixture.category)

    val pawnAdvanceStopFen = "4k3/8/8/4P3/8/8/8/4K3 w - - 0 1"
    val pawnAdvanceStopFacts = BoardFacts.fromFen(pawnAdvanceStopFen).toOption.get
    val pawnAdvanceStopMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvanceForStopFixture =
      strong(ScenePawnAdvance.write(pawnAdvanceStopFacts, pawnAdvanceStopMove).get)
    val blockedPawnStopSameBoard =
      pawnAdvanceForStopFixture.copy(scene = Scene.PawnStop, writer = None, pawnAdvanceProof = None)

    val advanceMaterialFen = "4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1"
    val advanceMaterialFacts = BoardFacts.fromFen(advanceMaterialFen).toOption.get
    val advanceMaterialPawnMove = Line(Square('d', 4), Square('d', 5))
    val advanceMaterialCapture = Line(Square('d', 4), Square('e', 5))
    val advanceMaterialPawn =
      strong(ScenePawnAdvance.write(advanceMaterialFacts, advanceMaterialPawnMove).get)
    val advanceMaterial = strong(SceneMaterial.write(advanceMaterialFacts, advanceMaterialCapture).get)

    val advanceDefenseFen = "4k3/8/8/5n1P/3Q4/8/8/4K3 w - - 0 1"
    val advanceDefenseFacts = BoardFacts.fromFen(advanceDefenseFen).toOption.get
    val advanceDefensePawnMove = Line(Square('h', 5), Square('h', 6))
    val advanceDefenseThreat = Line(Square('f', 5), Square('d', 4))
    val advanceDefenseMove = Line(Square('d', 4), Square('e', 4))
    val advanceDefensePawn = strong(ScenePawnAdvance.write(advanceDefenseFacts, advanceDefensePawnMove).get)
    val advanceDefense =
      strong(SceneDefense.write(advanceDefenseFacts, advanceDefenseThreat, advanceDefenseMove).get)

    val stopDefenseFen = "4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1"
    val stopDefenseFacts = BoardFacts.fromFen(stopDefenseFen).toOption.get
    val stopDefenseStopMove = Line(Square('g', 7), Square('e', 6))
    val stopDefenseThreat = Line(Square('f', 4), Square('d', 5))
    val stopDefenseMove = Line(Square('d', 5), Square('c', 6))
    val stopDefensePawnStop = strong(ScenePawnStop.write(stopDefenseFacts, stopDefenseStopMove).get)
    val stopDefense = strong(SceneDefense.write(stopDefenseFacts, stopDefenseThreat, stopDefenseMove).get)

    val stopLineFen = "r5k1/5n2/8/7P/8/8/4N3/4K3 b - - 0 1"
    val stopLineFacts = BoardFacts.fromFen(stopLineFen).toOption.get
    val stopLineStopMove = Line(Square('f', 7), Square('h', 6))
    val stopLinePinMove = Line(Square('a', 8), Square('e', 8))
    val stopLinePawnStop = strong(ScenePawnStop.write(stopLineFacts, stopLineStopMove).get)
    val stopLinePin =
      strong(
        TacticPin
          .write(
            stopLineFacts,
            Some(stopLinePinMove),
            Some(Square('e', 8)),
            Some(Square('e', 2)),
            Some(Square('e', 1))
          )
          .get
      )

    val engineFen = pawnAdvanceStopFen
    val engineFacts = pawnAdvanceStopFacts
    val engineMove = pawnAdvanceStopMove
    val enginePawnBase = strong(ScenePawnAdvance.write(engineFacts, engineMove).get)
    val engineSupports =
      strong(
        ScenePawnAdvance
          .withEngineCheck(
            enginePawnBase,
            engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Supports)
          )
          .get
      )
    val engineCaps =
      strong(
        ScenePawnAdvance
          .withEngineCheck(
            enginePawnBase,
            engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Caps)
          )
          .get
      )
    val engineRefutes =
      strong(
        ScenePawnAdvance
          .withEngineCheck(
            enginePawnBase,
            engineCheck(engineFacts, enginePawnBase, engineMove, EngineCheckStatus.Refutes)
          )
          .get
      )

    val promotionLookingFen = "4k3/8/4P3/8/8/8/8/4K3 w - - 0 1"
    val promotionLookingFacts = BoardFacts.fromFen(promotionLookingFen).toOption.get
    val promotionLookingMove = Line(Square('e', 6), Square('e', 7))
    val promotionLookingAdvance =
      strong(ScenePawnAdvance.write(promotionLookingFacts, promotionLookingMove).get)
    val blockedPromotionLooking =
      promotionLookingAdvance.copy(scene = Scene.Pawns, writer = None, pawnAdvanceProof = None)

    val tablebaseLookingFen = "4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1"
    val tablebaseLookingFacts = BoardFacts.fromFen(tablebaseLookingFen).toOption.get
    val tablebaseLookingStopMove = Line(Square('g', 7), Square('e', 6))
    val tablebaseLookingStop =
      strong(ScenePawnStop.write(tablebaseLookingFacts, tablebaseLookingStopMove).get)
    val blockedTablebase =
      tablebaseLookingStop.copy(scene = Scene.Endgame, writer = None, pawnStopProof = None)

    Vector(
      PihFixture(
        category = "PawnAdvance vs PawnStop",
        fen = pawnAdvanceStopFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(pawnAdvanceStopMove),
        rows = Vector(
          "Scene.PawnAdvance" -> pawnAdvanceForStopFixture,
          "Scene.PawnStop/blocked" -> blockedPawnStopSameBoard
        ),
        expectedOpenRows = Set("Scene.PawnAdvance"),
        expectedBlockedRows = Set("Scene.PawnStop/blocked"),
        expectedRoles = Map("Scene.PawnAdvance" -> Role.Lead, "Scene.PawnStop/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnAdvance",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("stops_pawn_advance", "promotion_threat", "converts_advantage", "best_move", "only_move")
      ),
      PihFixture(
        category = "PawnAdvance vs Material",
        fen = advanceMaterialFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(advanceMaterialPawnMove, advanceMaterialCapture),
        rows = Vector(
          "Scene.PawnAdvance" -> advanceMaterialPawn,
          "Scene.Material" -> advanceMaterial
        ),
        expectedOpenRows = Set("Scene.PawnAdvance", "Scene.Material"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Material" -> Role.Lead, "Scene.PawnAdvance" -> Role.Support),
        expectedSelectedVerdict = "Scene.Material",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("advances_passed_pawn", "promotion_threat", "converts_advantage", "best_move")
      ),
      PihFixture(
        category = "PawnAdvance vs Defense",
        fen = advanceDefenseFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(advanceDefensePawnMove, advanceDefenseMove),
        rows = Vector(
          "Scene.PawnAdvance" -> advanceDefensePawn,
          "Scene.Defense" -> advanceDefense
        ),
        expectedOpenRows = Set("Scene.PawnAdvance", "Scene.Defense"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Scene.PawnAdvance" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("advances_passed_pawn", "promotion_threat", "converts_advantage", "best_move", "only_move")
      ),
      PihFixture(
        category = "PawnStop vs Defense",
        fen = stopDefenseFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(stopDefenseStopMove, stopDefenseMove),
        rows = Vector(
          "Scene.PawnStop" -> stopDefensePawnStop,
          "Scene.Defense" -> stopDefense
        ),
        expectedOpenRows = Set("Scene.PawnStop", "Scene.Defense"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Scene.PawnStop" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "stops_pawn_advance",
          "stops_promotion",
          "draws_endgame",
          "tablebase_draw",
          "best_move",
          "only_move"
        )
      ),
      PihFixture(
        category = "PawnStop vs Line/Defender tactic",
        fen = stopLineFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(stopLineStopMove, stopLinePinMove),
        rows = Vector(
          "Scene.PawnStop" -> stopLinePawnStop,
          "Tactic.Pin" -> stopLinePin
        ),
        expectedOpenRows = Set("Scene.PawnStop", "Tactic.Pin"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Scene.PawnStop" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "stops_pawn_advance",
          "stops_promotion",
          "draws_endgame",
          "tablebase_draw",
          "best_move",
          "only_move"
        )
      ),
      PihFixture(
        category = "Pawn row vs EngineCheck Supports/Caps/Refutes",
        fen = engineFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(engineMove),
        rows = Vector(
          "Scene.PawnAdvance#Supports" -> engineSupports,
          "Scene.PawnAdvance#Caps" -> engineCaps,
          "Scene.PawnAdvance#Refutes" -> engineRefutes
        ),
        expectedOpenRows = Set("Scene.PawnAdvance#Supports", "Scene.PawnAdvance#Caps"),
        expectedBlockedRows = Set("Scene.PawnAdvance#Refutes"),
        expectedRoles = Map(
          "Scene.PawnAdvance#Supports" -> Role.Lead,
          "Scene.PawnAdvance#Caps" -> Role.Support,
          "Scene.PawnAdvance#Refutes" -> Role.Blocked
        ),
        expectedSelectedVerdict = "Scene.PawnAdvance#Supports",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("promotion_threat", "converts_advantage", "wins_endgame", "best_move", "only_move")
      ),
      PihFixture(
        category = "Promotion-looking but no PromotionThreat yet",
        fen = promotionLookingFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(promotionLookingMove),
        rows = Vector(
          "Scene.PawnAdvance" -> promotionLookingAdvance,
          "promotion-looking/blocked" -> blockedPromotionLooking
        ),
        expectedOpenRows = Set("Scene.PawnAdvance"),
        expectedBlockedRows = Set("promotion-looking/blocked"),
        expectedRoles = Map("Scene.PawnAdvance" -> Role.Lead, "promotion-looking/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnAdvance",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("promotion_threat", "converts_advantage", "wins_endgame", "best_move", "only_move")
      ),
      PihFixture(
        category = "tablebase-looking but no tablebase authority",
        fen = tablebaseLookingFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(tablebaseLookingStopMove),
        rows = Vector(
          "Scene.PawnStop" -> tablebaseLookingStop,
          "endgame-result/blocked" -> blockedTablebase
        ),
        expectedOpenRows = Set("Scene.PawnStop"),
        expectedBlockedRows = Set("endgame-result/blocked"),
        expectedRoles = Map("Scene.PawnStop" -> Role.Lead, "endgame-result/blocked" -> Role.Blocked),
        expectedSelectedVerdict = "Scene.PawnStop",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims =
          Vector("draws_endgame", "tablebase_draw", "wins_endgame", "best_move", "only_move", "forced")
      )
    ).foreach(assertFixture)

  test("PIH-2 Role Stability keeps pawn rows deterministic without duplicate pawn claims"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneClaim(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get)

    val defenseFacts = BoardFacts.fromFen("4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 4), Square('d', 5))
    val defenseMove = Line(Square('d', 5), Square('c', 6))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)

    val incompleteAdvance = pawnAdvance.copy(pawnAdvanceProof = None)
    val incompleteStop = pawnStop.copy(pawnStopProof = None)
    val refutedAdvance =
      ScenePawnAdvance
        .withEngineCheck(
          pawnAdvance,
          engineCheck(advanceFacts, pawnAdvance, advanceMove, EngineCheckStatus.Refutes)
        )
        .get
    val refutedStop =
      ScenePawnStop
        .withEngineCheck(pawnStop, engineCheck(stopFacts, pawnStop, stopMove, EngineCheckStatus.Refutes))
        .get
    val cappedAdvance =
      ScenePawnAdvance
        .withEngineCheck(
          pawnAdvance,
          engineCheck(advanceFacts, pawnAdvance, advanceMove, EngineCheckStatus.Caps)
        )
        .get

    val rows = Vector(
      "Scene.Defense" -> defense,
      "Scene.PawnAdvance" -> pawnAdvance,
      "Scene.PawnStop" -> pawnStop,
      "Scene.PawnAdvance/incomplete" -> incompleteAdvance,
      "Scene.PawnStop/incomplete" -> incompleteStop,
      "Scene.PawnAdvance/refuted" -> refutedAdvance,
      "Scene.PawnStop/refuted" -> refutedStop,
      "Scene.PawnAdvance/capped" -> cappedAdvance
    )
    val permutations =
      Vector(
        rows,
        rows.reverse,
        Vector(rows(5), rows(2), rows(7), rows(0), rows(4), rows(1), rows(6), rows(3))
      )
    val baseline = StoryTable.choose(rows.map(_._2))
    val baselineShape = shape(rows, baseline)

    permutations.foreach: ordered =>
      val verdicts = StoryTable.choose(ordered.map(_._2))
      assertEquals(rowId(rows, verdicts.head.story), "Scene.Defense")
      assertEquals(shape(rows, verdicts), baselineShape)

    assertEquals(baseline.count(_.role == Role.Lead), 1)
    assertEquals(baseline.find(_.story == incompleteAdvance).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == incompleteStop).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == refutedAdvance).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == refutedStop).map(_.role), Some(Role.Blocked))
    assertEquals(baseline.find(_.story == cappedAdvance).map(_.role), Some(Role.Support))
    Vector(incompleteAdvance, incompleteStop, refutedAdvance, refutedStop, cappedAdvance).foreach: row =>
      assertNoStandaloneClaim(row.toString, StoryTable.choose(Vector(row)).head)

    val samePawnRows = Vector("Scene.PawnAdvance" -> pawnAdvance, "Scene.PawnStop" -> pawnStop)
    val samePawnForward = StoryTable.choose(samePawnRows.map(_._2))
    val samePawnReverse = StoryTable.choose(samePawnRows.reverse.map(_._2))
    val samePawnAdvanceVerdict = samePawnForward.find(_.story == pawnAdvance).get
    val samePawnStopVerdict = samePawnForward.find(_.story == pawnStop).get

    assertEquals(shape(samePawnRows, samePawnForward), shape(samePawnRows, samePawnReverse))
    assertEquals(samePawnForward.count(_.role == Role.Lead), 1)
    assertEquals(samePawnAdvanceVerdict.role, Role.Lead)
    assertEquals(samePawnStopVerdict.role, Role.Support)
    assertNoStandaloneClaim("same-pawn PawnStop support", samePawnStopVerdict)

    assertEquals(
      StoryTable
        .choose(Vector(pawnAdvance, pawnAdvance.copy(anchor = pawnAdvance.anchor)))
        .count(_.role == Role.Lead),
      1
    )
    assertEquals(
      StoryTable.choose(Vector(pawnStop, pawnStop.copy(anchor = pawnStop.anchor))).count(_.role == Role.Lead),
      1
    )

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseLead = defenseCollision.find(_.story == defense).get
    val pawnStopSupport = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(defenseLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.DefendsPiece)
    )
    assertEquals(pawnStopSupport.role, Role.Support)
    assertNoStandaloneClaim("PawnStop must not own Defense claim", pawnStopSupport)

    val (_, advancePlan, advanceRendered) = leadPlan(pawnAdvance)
    assertEquals(advancePlan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    Vector("e6 will promote next.", "e6 converts the advantage.", "e6 wins the endgame.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(advancePlan, advanceRendered, output).accepted, false, output)

    val (_, stopPlan, stopRendered) = leadPlan(pawnStop)
    assertEquals(stopPlan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(stopPlan.allowedClaim.exists(_ == ExplanationClaim.DefendsPiece), false)
    assertEquals(LlmNarrationSmoke.check(stopPlan, stopRendered, "Ne6 defends the piece.").accepted, false)

  test("PIH-3 Meaning Ownership Boundary keeps pawn collision rows on their own claims"):
    def strongProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = value,
        conversionPrize = value,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(99))

    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    def assertOwnsOnly(
        label: String,
        story: Story,
        expectedClaim: ExplanationClaim,
        rejectedOutputs: Vector[String]
    ): ExplanationPlan =
      val (verdict, plan, rendered) = leadPlan(story)
      assertEquals(verdict.role, Role.Lead, label)
      assertEquals(plan.allowedClaim, Some(expectedClaim), label)
      rejectedOutputs.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label: $output")
      plan

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)
    assert(
      pawnAdvance.pawnAdvanceProof.exists(proof =>
        proof.alreadyPassedBefore &&
          proof.legalOneStepNonCaptureNonPromotion &&
          proof.afterBoardPassedPawn
      )
    )
    assertOwnsOnly(
      "Scene.PawnAdvance",
      pawnAdvance,
      ExplanationClaim.AdvancesPassedPawn,
      Vector(
        "e6 will promote.",
        "e6 is unstoppable.",
        "e6 converts the advantage.",
        "e6 wins the endgame.",
        "e6 is the only move."
      )
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get)
    assert(
      pawnStop.pawnStopProof.exists(proof =>
        proof.legalStopMove &&
          proof.targetPawnAlreadyPassed &&
          proof.nextAdvanceSquareNonPromotion &&
          proof.nextAdvanceSquareStoppedAfter
      )
    )
    assertOwnsOnly(
      "Scene.PawnStop",
      pawnStop,
      ExplanationClaim.StopsPassedPawnNextAdvance,
      Vector(
        "Ne6 stops promotion.",
        "Ne6 draws the endgame.",
        "Ne6 holds the endgame.",
        "Ne6 defends the piece.",
        "Ne6 is the only move."
      )
    )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialCapture = Line(Square('d', 4), Square('e', 5))
    val material = strong(SceneMaterial.write(materialFacts, materialCapture).get)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertOwnsOnly(
      "Scene.Material",
      material,
      ExplanationClaim.MaterialBalanceChanges,
      Vector(
        "dxe5 advances the passed pawn.",
        "dxe5 stops the passed pawn from advancing next.",
        "dxe5 will promote.",
        "dxe5 converts the advantage."
      )
    )

    val hanging = strong(TacticHanging.write(materialFacts, materialCapture).get)
    assert(
      hanging.captureResult.exists(result =>
        result.positiveMaterial && result.targetPiece.exists(_.man != Man.Pawn)
      )
    )
    assertOwnsOnly(
      "Tactic.Hanging",
      hanging,
      ExplanationClaim.CanWinPiece,
      Vector(
        "dxe5 advances the passed pawn.",
        "dxe5 stops the passed pawn from advancing next.",
        "dxe5 will promote.",
        "dxe5 converts the advantage."
      )
    )

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertOwnsOnly(
      "Scene.Defense",
      defense,
      ExplanationClaim.DefendsPiece,
      Vector(
        "Qe4+ advances the passed pawn.",
        "Qe4+ stops promotion.",
        "Qe4+ converts the advantage.",
        "Qe4+ is the only move."
      )
    )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)))
          .get
      )
    assertOwnsOnly(
      "Tactic.DiscoveredAttack",
      discovered,
      ExplanationClaim.RevealsAttackOnPiece,
      Vector(
        "Nf4 pins the piece.",
        "Nf4 removes the defender.",
        "Nf4 advances the passed pawn.",
        "Nf4 wins material."
      )
    )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      strong(
        TacticPin
          .write(
            pinFacts,
            Some(discoveredLine),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get
      )
    assertOwnsOnly(
      "Tactic.Pin",
      pin,
      ExplanationClaim.PinsPiece,
      Vector(
        "Nf4 reveals an attack.",
        "Nf4 removes the defender.",
        "Nf4 advances the passed pawn.",
        "Nf4 wins material."
      )
    )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
          .get
      )
    assertOwnsOnly(
      "Tactic.RemoveGuard",
      removeGuard,
      ExplanationClaim.RemovesDefender,
      Vector(
        "Nxe5 pins the piece.",
        "Nxe5 reveals an attack.",
        "Nxe5 advances the passed pawn.",
        "Nxe5 wins material."
      )
    )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerLine = Line(Square('a', 1), Square('e', 1))
    val skewer =
      strong(
        TacticSkewer
          .write(
            skewerFacts,
            Some(skewerLine),
            Some(Square('e', 1)),
            Some(Square('e', 5)),
            Some(Square('e', 8))
          )
          .get
      )
    assertOwnsOnly(
      "Tactic.Skewer",
      skewer,
      ExplanationClaim.SkewersPieceToPiece,
      Vector(
        "Re1 pins the piece.",
        "Re1 removes the defender.",
        "Re1 advances the passed pawn.",
        "Re1 wins material."
      )
    )

    val defenseCollision = StoryTable.choose(Vector(pawnStop, defense))
    val defenseVerdict = defenseCollision.find(_.story == defense).get
    val pawnStopVerdict = defenseCollision.find(_.story == pawnStop).get
    assertEquals(defenseVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(defenseVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.DefendsPiece)
    )
    assertEquals(pawnStopVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(pawnStopVerdict), None)

  test("PIH-4 EngineCheck Interaction reuses existing pawn statuses without engine-owned claims"):
    final case class PawnEngineFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        reply: Line,
        expectedClaim: ExplanationClaim,
        attach: (Story, EngineCheck) => Option[Story]
    )

    def engineCheck(fixture: PawnEngineFixture, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = fixture.facts,
        story = Some(fixture.story),
        engineLine = Some(EngineLine(Vector(fixture.line))),
        replyLine = Some(EngineLine(Vector(fixture.reply))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(45)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def selected(story: Story): Verdict =
      StoryTable.choose(Vector(story)).head

    def assertNoEnginePublicValues(label: String, verdict: Verdict): Unit =
      assertEquals(verdict.values.contains(1234.0), false, label)
      assertEquals(verdict.values.contains(1240.0), false, label)

    def assertRejectsEngineSpeech(label: String, plan: ExplanationPlan, rendered: RenderedLine): Unit =
      Vector(
        "The engine says this is best.",
        "Raw PV: e6 Ke7 proves it.",
        "+12.34 is the public eval.",
        "1234 centipawns proves the move.",
        "This is the best move.",
        "This is the only move.",
        "This is a tablebase-like claim.",
        "This is a tablebase draw."
      ).foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"$label: $output")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get

    Vector(
      PawnEngineFixture(
        label = "Scene.PawnAdvance",
        facts = advanceFacts,
        story = advanceStory,
        line = advanceLine,
        reply = Line(Square('e', 8), Square('e', 7)),
        expectedClaim = ExplanationClaim.AdvancesPassedPawn,
        attach = ScenePawnAdvance.withEngineCheck
      ),
      PawnEngineFixture(
        label = "Scene.PawnStop",
        facts = stopFacts,
        story = stopStory,
        line = stopLine,
        reply = Line(Square('e', 1), Square('e', 2)),
        expectedClaim = ExplanationClaim.StopsPassedPawnNextAdvance,
        attach = ScenePawnStop.withEngineCheck
      )
    ).foreach: fixture =>
      val baseVerdict = selected(fixture.story)
      val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
      val baseRendered = DeterministicRenderer.fromPlan(basePlan).get
      val supports = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Supports)).get
      val caps = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Caps)).get
      val refutes = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Refutes)).get
      val unknown = fixture.attach(fixture.story, engineCheck(fixture, EngineCheckStatus.Unknown)).get

      val supportsVerdict = selected(supports)
      val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
      val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
      val capsVerdict = selected(caps)
      val refutesVerdict = selected(refutes)
      val unknownVerdict = selected(unknown)
      val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
      val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get

      assertEquals(basePlan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(supportsVerdict.role, Role.Lead, fixture.label)
      assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(supportsRendered.text, baseRendered.text, fixture.label)
      assertRejectsEngineSpeech(s"${fixture.label} Supports", supportsPlan, supportsRendered)

      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.label)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.label)
      assertEquals(DeterministicRenderer.fromPlan(basePlan.copy(allowedClaim = None)), None, fixture.label)

      assertEquals(refutesVerdict.role, Role.Blocked, fixture.label)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.label)
      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.label)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.label)

      assertEquals(unknownVerdict.role, Role.Lead, fixture.label)
      assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown), fixture.label)
      assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(unknownRendered.text, baseRendered.text, fixture.label)
      assertRejectsEngineSpeech(s"${fixture.label} Unknown", unknownPlan, unknownRendered)

      Vector(baseVerdict, supportsVerdict, capsVerdict, refutesVerdict, unknownVerdict).foreach: verdict =>
        assertNoEnginePublicValues(fixture.label, verdict)

  test("PIH-5 Negative Corpus keeps close pawn false positives silent"):
    def selected(story: Story): Verdict =
      StoryTable.choose(Vector(story)).head

    def assertSilent(label: String, story: Story): Unit =
      val verdict = selected(story)
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20,
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val notPassedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val notPassedAdvance = Line(Square('e', 5), Square('e', 6))
    val notPassedProof = PawnAdvanceProof.fromBoardFacts(notPassedFacts, notPassedAdvance)
    assertEquals(notPassedProof.legalOneStepNonCaptureNonPromotion, true)
    assertEquals(notPassedProof.alreadyPassedBefore, false)
    assertEquals(notPassedProof.complete, false)
    assertEquals(
      ScenePawnAdvance.write(notPassedFacts, notPassedAdvance),
      None,
      "pawn advances but was not passed"
    )

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val noAfterReplay =
      advanceStory.copy(pawnAdvanceProof =
        advanceStory.pawnAdvanceProof.map(
          _.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty)
        )
      )
    assertSilent("passed pawn advances but no exact after-board proof", noAfterReplay)

    val routeMismatch =
      advanceStory.copy(route = Some(Line(Square('e', 5), Square('e', 7))))
    assertSilent("route mismatch", routeMismatch)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val safeNextAdvanceMove = Line(Square('g', 7), Square('f', 5))
    val safeNextAdvanceProof = PawnStopProof.fromBoardFacts(stopFacts, safeNextAdvanceMove)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareAttackedAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareControlledByPawnAfter, false)
    assertEquals(safeNextAdvanceProof.nextAdvanceSquareStoppedAfter, false)
    assertEquals(ScenePawnStop.write(stopFacts, safeNextAdvanceMove), None, "next square still available")

    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    assertSilent("long-term blockade claim", stopStory.copy(plan = Some(Plan.PasserBlock)))

    val promotionLookingFacts = BoardFacts.fromFen("4k3/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionLookingLine = Line(Square('e', 6), Square('e', 7))
    val promotionLookingAdvance = ScenePawnAdvance.write(promotionLookingFacts, promotionLookingLine).get
    val promotionPlan = ExplanationPlan.fromSelected(selected(promotionLookingAdvance)).get
    val promotionRendered = DeterministicRenderer.fromPlan(promotionPlan).get
    Vector("e7 will promote next.", "e7 is unstoppable.", "e7 converts the advantage.").foreach: output =>
      assertEquals(LlmNarrationSmoke.check(promotionPlan, promotionRendered, output).accepted, false, output)

    assertSilent("tablebase-looking position", stopStory.copy(scene = Scene.Endgame, pawnStopProof = None))
    assertSilent(
      "king opposition-looking position",
      stopStory.copy(scene = Scene.Endgame, plan = Some(Plan.KingConvert), pawnStopProof = None)
    )
    assertSilent(
      "pawn race-looking position",
      promotionLookingAdvance.copy(scene = Scene.Pawns, plan = Some(Plan.Race), pawnAdvanceProof = None)
    )

    val wrongBoard = BoardFacts.fromFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val wrongBoardCheck = engineCheck(wrongBoard, advanceStory, advanceLine, EngineCheckStatus.Supports)
    val staleCheck =
      engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Supports, freshness = Some(2))
    val routeMismatchCheck =
      EngineCheck.fromStory(
        facts = advanceFacts,
        story = Some(advanceStory),
        engineLine = Some(EngineLine(Vector(Line(Square('e', 5), Square('e', 7))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(20)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
    Vector(
      "wrong board" -> wrongBoardCheck,
      "stale board" -> staleCheck,
      "route mismatch check" -> routeMismatchCheck
    ).foreach: (label, check) =>
      assertEquals(check.status, EngineCheckStatus.Unknown, label)
      assertEquals(ScenePawnAdvance.withEngineCheck(advanceStory, check), None, label)

    val refutingCheck =
      engineCheck(
        advanceFacts,
        advanceStory,
        advanceLine,
        EngineCheckStatus.Supports,
        before = 220,
        after = 20
      )
    val refutedStory = ScenePawnAdvance.withEngineCheck(advanceStory, refutingCheck).get
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertSilent("engine refutes plausible pawn row", refutedStory)

  test("PIH-6 Downstream Boundary Smoke sends only selected Lead pawn Verdicts to text stages"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertDownstreamBoundary(
        label: String,
        leadStory: Story,
        supportStory: Story,
        contextStory: Story,
        blockedStory: Story,
        cappedStory: Story,
        refutedStory: Story,
        expectedClaim: ExplanationClaim
    ): Unit =
      val rows =
        Vector(
          "Lead" -> leadStory,
          "Support" -> supportStory,
          "Context" -> contextStory,
          "Blocked" -> blockedStory,
          "Capped" -> cappedStory,
          "Refuted" -> refutedStory
        )
      val verdicts = StoryTable.choose(rows.map(_._2))
      val byId = verdicts
        .map(verdict => rows.collectFirst { case (id, story) if story == verdict.story => id }.get -> verdict)
        .toMap
      assertEquals(byId("Lead").role, Role.Lead, label)
      assertEquals(byId("Support").role, Role.Support, label)
      assertEquals(byId("Context").role, Role.Context, label)
      assertEquals(byId("Blocked").role, Role.Blocked, label)
      assertEquals(byId("Capped").engineStrengthLimited, true, label)
      assertEquals(byId("Refuted").engineCheckStatus, Some(EngineCheckStatus.Refutes), label)

      val leadPlan = ExplanationPlan.fromSelected(byId("Lead")).get
      val rendered = DeterministicRenderer.fromPlan(leadPlan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
      assertEquals(leadPlan.allowedClaim, Some(expectedClaim), label)
      assertEquals(rendered.claimKey, expectedClaim.key, label)
      assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."), label)
      Vector("renderedText:", "claimKey:", "strength:", "forbiddenWording:").foreach: field =>
        assert(prompt.contains(field), s"$label prompt missing $field")
      Vector(
        "BoardFacts",
        "Story(",
        "Verdict(",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "proofFailures",
        "sourceRow",
        "FEN",
        "raw PV",
        "routeSan"
      ).foreach: forbiddenInput =>
        assertEquals(prompt.contains(forbiddenInput), false, s"$label prompt leaked $forbiddenInput")

      Vector(
        ForbiddenWording.PromotionThreat,
        ForbiddenWording.UnstoppablePawn,
        ForbiddenWording.WinningEndgame,
        ForbiddenWording.ConvertsAdvantage,
        ForbiddenWording.DrawsEndgame,
        ForbiddenWording.TablebaseDraw,
        ForbiddenWording.BestMove,
        ForbiddenWording.OnlyMove,
        ForbiddenWording.Forced,
        ForbiddenWording.CreatesPressure,
        ForbiddenWording.TakesInitiative
      ).foreach: forbidden =>
        assert(
          leadPlan.forbiddenWording.contains(forbidden),
          s"$label missing forbidden wording ${forbidden.key}"
        )

      Vector("Support", "Context", "Blocked", "Capped", "Refuted").foreach: id =>
        assertEquals(ExplanationPlan.fromSelected(byId(id)), None, s"$label $id plan")
        assertEquals(
          ExplanationPlan.fromSelected(byId(id)).flatMap(DeterministicRenderer.fromPlan),
          None,
          s"$label $id render"
        )

      Vector(
        "will promote",
        "is unstoppable",
        "wins the endgame",
        "is winning",
        "conversion",
        "draws the endgame",
        "holds the endgame",
        "tablebase",
        "is the best move",
        "is the only move",
        "is forced",
        "creates pressure",
        "takes the initiative"
      ).foreach: phrase =>
        val output = s"${rendered.text} It $phrase."
        assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, s"$label: $phrase")

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceLead = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advanceSupport = advanceLead.copy(proof = advanceLead.proof.copy(clarity = 99))
    val advanceContext = advanceLead.copy(proof = advanceLead.proof.copy(boardProof = 20))
    val advanceBlocked =
      advanceLead.copy(pawnAdvanceProof =
        advanceLead.pawnAdvanceProof.map(
          _.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty)
        )
      )
    val advanceCapped =
      ScenePawnAdvance
        .withEngineCheck(
          advanceLead,
          engineCheck(advanceFacts, advanceLead, advanceLine, EngineCheckStatus.Caps)
        )
        .get
    val advanceRefuted =
      ScenePawnAdvance
        .withEngineCheck(
          advanceLead,
          engineCheck(advanceFacts, advanceLead, advanceLine, EngineCheckStatus.Refutes)
        )
        .get
    assertDownstreamBoundary(
      "Scene.PawnAdvance",
      advanceLead,
      advanceSupport,
      advanceContext,
      advanceBlocked,
      advanceCapped,
      advanceRefuted,
      ExplanationClaim.AdvancesPassedPawn
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopLead = ScenePawnStop.write(stopFacts, stopLine).get
    val stopSupport = stopLead.copy(proof = stopLead.proof.copy(clarity = 99))
    val stopContext = stopLead.copy(proof = stopLead.proof.copy(boardProof = 20))
    val stopBlocked =
      stopLead.copy(pawnStopProof =
        stopLead.pawnStopProof.map(
          _.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty)
        )
      )
    val stopCapped =
      ScenePawnStop
        .withEngineCheck(stopLead, engineCheck(stopFacts, stopLead, stopLine, EngineCheckStatus.Caps))
        .get
    val stopRefuted =
      ScenePawnStop
        .withEngineCheck(stopLead, engineCheck(stopFacts, stopLead, stopLine, EngineCheckStatus.Refutes))
        .get
    assertDownstreamBoundary(
      "Scene.PawnStop",
      stopLead,
      stopSupport,
      stopContext,
      stopBlocked,
      stopCapped,
      stopRefuted,
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  test("PIH-7 Diagnostics Boundary keeps pawn diagnostics out of public meaning"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1240)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertDiagnosticBoundary(
        label: String,
        story: Story,
        blockedStory: Story,
        cappedStory: Story,
        refutedStory: Story,
        expectedClaim: ExplanationClaim
    ): Unit =
      val blockedVerdict = StoryTable.choose(Vector(blockedStory)).head
      assertEquals(blockedVerdict.role, Role.Blocked, label)
      assertEquals(blockedVerdict.proofFailures.nonEmpty, true, label)
      assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None, label)
      assertEquals(
        DeterministicRenderer.fromPlan(
          ExplanationPlan(
            role = Role.Blocked,
            scene = story.scene,
            tactic = None,
            side = story.side,
            rival = story.rival,
            target = story.target,
            anchor = story.anchor,
            route = story.route,
            routeSan = story.routeSan,
            secondaryTarget = None,
            allowedClaim = Some(expectedClaim),
            evidenceLine = story.route,
            strength = ExplanationStrength.Bounded,
            forbiddenWording = Vector(ForbiddenWording.EngineSays),
            relations = Vector(ExplanationRelation.BlockedByEngineRefute),
            debugOnly = true,
            supportContextLinks = Vector.empty
          )
        ),
        None,
        label
      )

      Vector(
        "PawnAdvanceProof",
        "PawnStopProof",
        "exact after-board replay",
        "missing evidence",
        "same-board proof",
        "EngineCheck",
        "EngineLine",
        "EngineEval",
        "1234",
        "1240"
      ).foreach: diagnostic =>
        assertEquals(
          blockedVerdict.values.mkString(" ").contains(diagnostic),
          false,
          s"$label Verdict.values leaked $diagnostic"
        )

      val cappedVerdict = StoryTable.choose(Vector(cappedStory)).head
      assertEquals(cappedVerdict.engineStrengthLimited, true, label)
      assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None, label)

      val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
      assertEquals(refutedVerdict.role, Role.Blocked, label)
      assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), label)
      assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None, label)

      val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
      val rendered = DeterministicRenderer.fromPlan(leadPlan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
      Vector(
        "proofFailures",
        "missing evidence",
        "exact after-board replay",
        "same-board proof",
        "EngineCheck",
        "EngineLine",
        "EngineEval",
        "StoryTable",
        "debug relation",
        "blocked_by_engine_refute",
        "capped_same_story"
      ).foreach: diagnostic =>
        assertEquals(prompt.contains(diagnostic), false, s"$label prompt leaked $diagnostic")

      val relationInjectedPlan =
        leadPlan.copy(relations =
          Vector(
            ExplanationRelation.BlockedByEngineRefute,
            ExplanationRelation.CappedSameStory,
            ExplanationRelation.SameFamilyLowerRank
          )
        )
      val relationRendered = DeterministicRenderer.fromPlan(relationInjectedPlan).get
      Vector(
        "blocked_by_engine_refute",
        "capped_same_story",
        "same_family_lower_rank",
        "debug relation",
        "StoryTable"
      ).foreach: diagnostic =>
        assertEquals(relationRendered.text.contains(diagnostic), false, s"$label renderer leaked $diagnostic")

      Vector(
        s"${rendered.text} Missing evidence: exact after-board replay.",
        s"${rendered.text} StoryTable debug relation: blocked_by_engine_refute.",
        s"${rendered.text} EngineCheck text says +12.34.",
        s"${rendered.text} capped_same_story proves the row.",
        s"${rendered.text} proofFailures show same-board proof failed."
      ).foreach: output =>
        assertEquals(
          LlmNarrationSmoke.check(leadPlan, rendered, output).accepted,
          false,
          s"$label accepted diagnostic output: $output"
        )

      val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
      val runtimeSourceStream = Files.walk(runtimeRoot)
      val runtimeText =
        try
          runtimeSourceStream
            .iterator()
            .asScala
            .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
            .map(Files.readString)
            .mkString("\n")
        finally runtimeSourceStream.close()
      Vector(
        "PIH-",
        "fixture map",
        "negative corpus",
        "Pawn Interaction Hardening"
      ).foreach: helperOnlyTerm =>
        assert(
          !runtimeText.contains(helperOnlyTerm),
          s"$label test helper term became runtime authority: $helperOnlyTerm"
        )

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advanceBlocked =
      advanceStory.copy(
        pawnAdvanceProof = advanceStory.pawnAdvanceProof.map(
          _.copy(exactAfterBoardReplay = false, missingEvidence = Vector.empty)
        ),
        storyProof = StoryProof.empty
      )
    assertDiagnosticBoundary(
      "Scene.PawnAdvance",
      advanceStory,
      advanceBlocked,
      ScenePawnAdvance
        .withEngineCheck(
          advanceStory,
          engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Caps)
        )
        .get,
      ScenePawnAdvance
        .withEngineCheck(
          advanceStory,
          engineCheck(advanceFacts, advanceStory, advanceLine, EngineCheckStatus.Refutes)
        )
        .get,
      ExplanationClaim.AdvancesPassedPawn
    )

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    val stopBlocked =
      stopStory.copy(
        pawnStopProof = stopStory.pawnStopProof.map(
          _.copy(nextAdvanceSquareStoppedAfter = false, missingEvidence = Vector.empty)
        ),
        storyProof = StoryProof.empty
      )
    assertDiagnosticBoundary(
      "Scene.PawnStop",
      stopStory,
      stopBlocked,
      ScenePawnStop
        .withEngineCheck(stopStory, engineCheck(stopFacts, stopStory, stopLine, EngineCheckStatus.Caps))
        .get,
      ScenePawnStop
        .withEngineCheck(stopStory, engineCheck(stopFacts, stopStory, stopLine, EngineCheckStatus.Refutes))
        .get,
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  test("PIH Closeout Hard Cleanup keeps pawn interaction authority separated and surfaces closed"):
    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceLine = Line(Square('e', 5), Square('e', 6))
    val advanceObservation =
      advanceFacts.seen.passedPawnObservations
        .find(row => row.side == Side.White && row.pawn.square == Square('e', 5))
        .get
    val advanceProof = PawnAdvanceProof.fromBoardFacts(advanceFacts, advanceLine)
    val advanceStory = ScenePawnAdvance.write(advanceFacts, advanceLine).get
    val advancePlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(advanceStory)).head).get
    val advanceRendered = DeterministicRenderer.fromPlan(advancePlan).get

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopLine = Line(Square('g', 7), Square('e', 6))
    val stopObservation =
      stopFacts.seen.passedPawnObservations
        .find(row => row.side == Side.White && row.pawn.square == Square('e', 5))
        .get
    val stopProof = PawnStopProof.fromBoardFacts(stopFacts, stopLine)
    val stopStory = ScenePawnStop.write(stopFacts, stopLine).get
    val stopPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(stopStory)).head).get
    val stopRendered = DeterministicRenderer.fromPlan(stopPlan).get

    assertEquals(advanceObservation.pawn, advanceProof.pawnBefore.get)
    assertEquals(advanceObservation.pawn, stopObservation.pawn)
    assertEquals(stopProof.targetPawn, Some(stopObservation.pawn))
    assertEquals(advanceProof.publicClaimAllowed, false)
    assertEquals(stopProof.publicClaimAllowed, false)

    assertEquals(advanceStory.scene, Scene.PawnAdvance)
    assertEquals(advanceStory.writer, Some(StoryWriter.ScenePawnAdvance))
    assertEquals(advanceStory.pawnAdvanceProof, Some(advanceProof))
    assertEquals(advanceStory.pawnStopProof, None)
    assertEquals(advanceStory.threatProof, None)
    assertEquals(advanceStory.defenseProof, None)
    assertEquals(advanceStory.captureResult, None)
    assertEquals(advanceStory.lineProof, None)
    assertEquals(advanceStory.pinProof, None)
    assertEquals(advanceStory.removeGuardProof, None)
    assertEquals(advanceStory.skewerProof, None)
    assertEquals(advancePlan.allowedClaim, Some(ExplanationClaim.AdvancesPassedPawn))
    assertEquals(advanceRendered.claimKey, "advances_passed_pawn")

    assertEquals(stopStory.scene, Scene.PawnStop)
    assertEquals(stopStory.writer, Some(StoryWriter.ScenePawnStop))
    assertEquals(stopStory.pawnStopProof, Some(stopProof))
    assertEquals(stopStory.pawnAdvanceProof, None)
    assertEquals(stopStory.threatProof, None)
    assertEquals(stopStory.defenseProof, None)
    assertEquals(stopStory.captureResult, None)
    assertEquals(stopStory.lineProof, None)
    assertEquals(stopStory.pinProof, None)
    assertEquals(stopStory.removeGuardProof, None)
    assertEquals(stopStory.skewerProof, None)
    assertEquals(stopPlan.allowedClaim, Some(ExplanationClaim.StopsPassedPawnNextAdvance))
    assertEquals(stopRendered.claimKey, "stops_pawn_advance")

    assertEquals(ExplanationClaim.PawnAdvanceAllowed.map(_.key), Vector("advances_passed_pawn"))
    assertEquals(ExplanationClaim.PawnStopAllowed.map(_.key), Vector("stops_pawn_advance"))
    assertEquals(
      ExplanationClaim.PawnAdvanceAllowed.intersect(ExplanationClaim.PawnStopAllowed),
      Vector.empty
    )
    Vector(
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: openedHomeClaims =>
      assertEquals(openedHomeClaims.contains(ExplanationClaim.AdvancesPassedPawn), false)
      assertEquals(openedHomeClaims.contains(ExplanationClaim.StopsPassedPawnNextAdvance), false)

    assertEquals(
      StoryWriter.values.toVector,
      Vector(
        StoryWriter.TacticHanging,
        StoryWriter.TacticFork,
        StoryWriter.SceneMaterial,
        StoryWriter.SceneDefense,
        StoryWriter.TacticDiscoveredAttack,
        StoryWriter.TacticPin,
        StoryWriter.TacticRemoveGuard,
        StoryWriter.TacticOverload,
        StoryWriter.TacticDeflect,
        StoryWriter.TacticSkewer,
        StoryWriter.TacticQueenHit,
        StoryWriter.TacticLoose,
        StoryWriter.TacticTrap,
        StoryWriter.ScenePawnAdvance,
        StoryWriter.ScenePawnStop,
        StoryWriter.ScenePawnBreak,
        StoryWriter.ScenePawnBlock,
        StoryWriter.ScenePromotionThreat,
        StoryWriter.ScenePromotion,
        StoryWriter.ScenePawnCapture,
        StoryWriter.ScenePassedPawnCreated,
        StoryWriter.SceneFileOpened,
        StoryWriter.SceneCheckGiven,
        StoryWriter.SceneCheckEscaped,
        StoryWriter.SceneCheckmate,
        StoryWriter.SceneStalemate
      )
    )
    val livePositiveClaimKeys = ExplanationClaim.values.map(_.key).toVector
    Vector(
      "promotion",
      "pawn_break",
      "tablebase",
      "tablebase_draw",
      "pawn_race",
      "king_route",
      "opposition",
      "unstoppable",
      "conversion",
      "winning_endgame",
      "draws_endgame"
    ).foreach: closedClaim =>
      assert(
        !livePositiveClaimKeys.contains(closedClaim),
        s"closed pawn meaning became a live claim key: $closedClaim"
      )

    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    Vector(
      "tablebaseProof",
      "pawnRaceProof",
      "kingRouteProof",
      "oppositionProof",
      "conversionProof",
      "winningEndgameProof"
    ).foreach: closedProofHome =>
      assert(!storyFieldNames.contains(closedProofHome), s"closed proof home reached Story: $closedProofHome")

    val pawnForbiddenKeys = (advancePlan.forbiddenWording ++ stopPlan.forbiddenWording).map(_.key).toSet
    Vector("promotion_threat", "tablebase_draw", "pawn_race", "king_route", "opposition").foreach:
      forbiddenKey =>
        assert(
          pawnForbiddenKeys.contains(forbiddenKey),
          s"closed meaning must remain forbidden wording only: $forbiddenKey"
        )

    Vector(advanceRendered.text, stopRendered.text).foreach: text =>
      val lowered = text.toLowerCase
      Vector(
        "will promote",
        "promotion",
        "pawn break",
        "tablebase",
        "pawn race",
        "king route",
        "opposition",
        "unstoppable",
        "conversion",
        "winning",
        "draw"
      ).foreach: forbidden =>
        assert(!lowered.contains(forbidden), s"closed pawn wording leaked to renderer: $forbidden")

    val runtimeRoot = Paths.get("modules/commentary/src/main/scala/lila/commentary/chess")
    val runtimeSourceStream = Files.walk(runtimeRoot)
    val runtimeText =
      try
        runtimeSourceStream
          .iterator()
          .asScala
          .filter(path => Files.isRegularFile(path) && path.toString.endsWith(".scala"))
          .map(Files.readString)
          .mkString("\n")
      finally runtimeSourceStream.close()
    Vector(
      "PIH Closeout",
      "Pawn Interaction Hardening",
      "fixture map",
      "negative corpus",
      "tablebase authority",
      "public route 200",
      "production API",
      "user-facing LLM"
    ).foreach: closeoutOnlyTerm =>
      assert(
        !runtimeText.contains(closeoutOnlyTerm),
        s"closeout-only term became runtime authority: $closeoutOnlyTerm"
      )

