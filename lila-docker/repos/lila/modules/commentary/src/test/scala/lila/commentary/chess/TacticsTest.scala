package lila.commentary.chess

import java.nio.file.{ Files, Paths }

import scala.jdk.CollectionConverters.*

class TacticsTest extends ChessTestSupport:

  test("Line-1 LineProof proves one legal discovered slider attack without creating Story"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Some(Line(Square('d', 3), Square('f', 4)))
    val proof = LineProof.fromBoardFacts(
      facts = facts,
      revealingMove = revealMove,
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.slider, Some(Piece(Side.White, Man.Bishop, Square('b', 1))))
    assertEquals(proof.blocker, Some(Piece(Side.White, Man.Knight, Square('d', 3))))
    assertEquals(proof.movedPiece, Some(Piece(Side.White, Man.Knight, Square('d', 3))))
    assertEquals(proof.revealedTarget, Some(Piece(Side.Black, Man.Rook, Square('g', 6))))
    assertEquals(proof.revealingMove, revealMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.Diagonal))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.beforeLineBlockedOrInactive, true)
    assertEquals(proof.afterSliderAttacksTarget, true)
    assertEquals(proof.targetNonKingMaterial, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      LineProof.getClass.getDeclaredMethods.toVector ++ classOf[LineProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine")
          .exists(name => method.getReturnType.getSimpleName.contains(name))
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        LineProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "pin",
      "pressure",
      "winsMaterial",
      "winning",
      "decisive",
      "blunder",
      "bestMove",
      "forcedLine"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Line-1 LineProof keeps false positives and diagnostics out of public output"):
    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Some(Line(Square('d', 3), Square('f', 4)))
    val kingTargetProof = LineProof.fromBoardFacts(
      facts = kingTargetFacts,
      revealingMove = revealMove,
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )

    assertEquals(kingTargetProof.complete, false)
    assertEquals(kingTargetProof.publicClaimAllowed, false)
    assertEquals(
      kingTargetProof.missingEvidence.exists(_.missing.contains("target non-king material piece")),
      true
    )

    val legalMoveMissing = LineProof.fromBoardFacts(
      facts = kingTargetFacts,
      revealingMove = Some(Line(Square('d', 3), Square('d', 4))),
      sliderSquare = Some(Square('b', 1)),
      targetSquare = Some(Square('g', 6))
    )
    assertEquals(legalMoveMissing.complete, false)
    assertEquals(
      legalMoveMissing.missingEvidence.exists(_.missing.contains("legal revealing move")),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.DiscoveredAttack),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('g', 6)),
      anchor = Some(Square('b', 1)),
      route = revealMove,
      routeSan = BoardFacts.sanFor(kingTargetFacts, revealMove.get),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(kingTargetFacts, revealMove.get)
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Line-2 TacticDiscoveredAttack writer admits one proof-backed revealed slider Story"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(
        facts = facts,
        revealingMove = Some(revealMove),
        sliderSquare = Some(Square('b', 1)),
        targetSquare = Some(Square('g', 6))
      )
      .get
    val proof = story.lineProof.get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(story.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('g', 6)))
    assertEquals(story.anchor, Some(Square('d', 3)))
    assertEquals(story.route, Some(revealMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.revealingMove, Some(revealMove))
    assertEquals(proof.movedPiece.map(_.square), Some(Square('d', 3)))
    assertEquals(proof.slider.map(_.square), Some(Square('b', 1)))
    assertEquals(proof.revealedTarget.map(_.square), Some(Square('g', 6)))
    assertEquals(proof.afterSliderAttacksTarget, true)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(
      DeterministicRenderer.fromPlan(plan).map(_.text),
      Some("Nf4 reveals an attack on the piece on g6.")
    )

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-120)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedVerdict =
      StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, refutes).get)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)

  test("Line-2 TacticDiscoveredAttack writer keeps sibling line tactics and king targets silent"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positive = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get

    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    assertEquals(
      TacticDiscoveredAttack.write(
        kingTargetFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6))
      ),
      None
    )
    assertEquals(
      TacticDiscoveredAttack.write(
        facts,
        Some(Line(Square('d', 3), Square('d', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6))
      ),
      None
    )

    val incompleteStoryProof = positive.copy(storyProof = StoryProof.empty)
    assertEquals(StoryTable.choose(Vector(incompleteStoryProof)).head.role, Role.Blocked)

    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = positive.copy(tactic = Some(tactic))
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, tactic.toString)
      assertEquals(verdict.leadAllowed, false, tactic.toString)

    val writerSurfaceNames =
      TacticDiscoveredAttack.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticDiscoveredAttack.getClass.getDeclaredFields.map(_.getName).toSet
    Vector("pin", "skewer", "xray", "removeGuard", "mate", "kingSafety", "winning", "decisive", "blunder")
      .foreach: forbidden =>
        assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Line-3 negative corpus keeps revealed attack false positives silent"):
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positiveFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val sameBoardMissingFacts = minimalBoardFacts(
      sideToMove = Side.White,
      sideLegal = Moves(known = true, lines = Vector(revealMove), san = Vector("Nf4"), moveCount = 1),
      rivalLegal = Moves(
        known = true,
        lines = Vector(Line(Square('h', 8), Square('h', 7))),
        san = Vector("Kh7"),
        moveCount = 1
      ),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('h', 1)),
        Piece(Side.White, Man.Bishop, Square('b', 1)),
        Piece(Side.White, Man.Knight, Square('d', 3)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.Black, Man.Rook, Square('g', 6))
      )
    )
    val corpus = Vector(
      (
        "legal move missing",
        positiveFacts,
        Some(Line(Square('d', 3), Square('d', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "legal revealing move"
      ),
      (
        "same-board proof missing",
        sameBoardMissingFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "same-board proof"
      ),
      (
        "blocker moves but line remains closed",
        BoardFacts.fromFen("7k/8/6r1/8/8/3Q4/8/1B5K w - - 0 1").toOption.get,
        Some(Line(Square('d', 3), Square('e', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "before-move blocked or inactive line"
      ),
      (
        "target not attacked after move",
        BoardFacts.fromFen("7k/7r/8/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('h', 6)),
        "after-move slider attack"
      ),
      (
        "slider is not a slider",
        BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1N5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "slider piece"
      ),
      (
        "target is king",
        BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "target non-king material piece"
      ),
      (
        "another piece still blocks after the blocker moves",
        BoardFacts.fromFen("7k/8/6r1/8/4P3/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "after-move slider attack"
      ),
      (
        "discovered-looking move has no target",
        BoardFacts.fromFen("7k/8/8/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        "revealed target"
      )
    )

    corpus.foreach: (label, facts, move, slider, target, expectedMissing) =>
      val lineProof = LineProof.fromBoardFacts(facts, move, slider, target)
      assertEquals(lineProof.complete, false, label)
      assertEquals(
        lineProof.missingEvidence.exists(_.missing.contains(expectedMissing)),
        true,
        label
      )
      assertEquals(TacticDiscoveredAttack.write(facts, move, slider, target), None, label)

  test("Line-3 negative corpus blocks wording drift and sibling line classifications"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val positive = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val lineProofSurface =
      positive.lineProof.get.missingEvidence.flatMap(_.missing).mkString(" ").toLowerCase
    val writerSurfaceNames =
      (TacticDiscoveredAttack.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticDiscoveredAttack.getClass.getDeclaredFields.map(_.getName).toSet)
        .map(_.toLowerCase)
    val closedWording = Vector("pressure", "initiative", "mate")

    closedWording.foreach: forbidden =>
      assert(!lineProofSurface.contains(forbidden), forbidden)
      assert(!writerSurfaceNames.exists(_.contains(forbidden)), forbidden)

    val initiativeForgery = positive.copy(scene = Scene.Initiative, plan = Some(Plan.Initiative))
    val initiativeVerdict = StoryTable.choose(Vector(initiativeForgery)).head
    assertEquals(initiativeVerdict.role, Role.Blocked)
    assertEquals(initiativeVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(initiativeVerdict), None)

    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = positive.copy(tactic = Some(tactic))
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, tactic.toString)
      assertEquals(verdict.leadAllowed, false, tactic.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, tactic.toString)

  test("Line-4 DiscoveredAttack reuses EngineCheck statuses without creating claims"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val reply = Line(Square('h', 8), Square('h', 7))

    def check(
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20,
        storyInput: Option[Story] = Some(story),
        engineLine: Option[EngineLine] = Some(EngineLine(Vector(revealMove))),
        replyLine: Option[EngineLine] = Some(EngineLine(Vector(reply))),
        depth: Option[Int] = Some(18),
        freshnessPly: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = storyInput,
        engineLine = engineLine,
        replyLine = replyLine,
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = depth,
        freshnessPly = freshnessPly,
        requestedStatus = status
      )

    val unknown = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticDiscoveredAttack
        .withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20))
        .get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
    assertEquals(unknownPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(
      DeterministicRenderer.fromPlan(unknownPlan).map(_.text),
      Some("Nf4 reveals an attack on the piece on g6.")
    )

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    assertEquals(supportsPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(
      DeterministicRenderer.fromPlan(supportsPlan).map(_.text),
      Some("Nf4 reveals an attack on the piece on g6.")
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Line-4 EngineCheck cannot create or rank DiscoveredAttack from raw engine evidence"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val wrongRoute = Line(Square('h', 1), Square('g', 1))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val reply = Line(Square('h', 8), Square('h', 7))

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(revealMove),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(story, routeMismatch), None)

    val lowEvalCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(-850)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val highEvalCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(revealMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(850)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val lowEvalVerdict =
      StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, lowEvalCheck).get)).head
    val highEvalVerdict =
      StoryTable.choose(Vector(TacticDiscoveredAttack.withEngineCheck(story, highEvalCheck).get)).head

    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.engineCheckStatus, highEvalVerdict.engineCheckStatus)
    assertEquals(
      ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )

  test("Line-5 StoryTable keeps DiscoveredAttack stable against existing rows"):
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
        pawnSupport = 0,
        clarity = value
      )

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get.copy(proof = strongProof(96))
    val material = SceneMaterial.write(hangingFacts, hangingMove).get.copy(proof = strongProof(99))
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork =
      TacticFork
        .write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5)))
        .get
        .copy(proof = strongProof(94))
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get.copy(proof = strongProof(93))
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(95))

    val rows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Tactic.Fork" -> fork,
        "Scene.Material" -> material,
        "Scene.Defense" -> defense,
        "Tactic.DiscoveredAttack" -> discovered
      )

    def rowId(story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim.map(_.key))
        )

    val forward = StoryTable.choose(rows.map(_._2))
    val reverse = StoryTable.choose(rows.reverse.map(_._2))
    val shuffled = StoryTable.choose(Vector(rows(3), rows(1), rows(4), rows(2), rows(0)).map(_._2))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(verdict => rowId(verdict.story)).toSet, rows.map(_._1).toSet)
    assertEquals(forward.find(_.story == discovered).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(forward.find(_.story == discovered).get), None)

    val materialVerdict = forward.find(_.story == material).get
    assertEquals(material.target, hanging.target)
    assertEquals(material.route, hanging.route)
    assertEquals(materialVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim), None)
    assertEquals(DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(materialVerdict).get), None)

  test("Line-5 invalid Defense and incomplete Fork cannot block or absorb DiscoveredAttack"):
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
        pawnSupport = 0,
        clarity = value
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
        .copy(proof = strongProof(95))
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val validDefense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val defenseWithoutThreat = validDefense.copy(threatProof = None, proof = strongProof(100))
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val validFork =
      TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkWithoutTwoTargetProof = validFork.copy(multiTargetProof = None, proof = strongProof(100))

    val verdicts = StoryTable.choose(Vector(defenseWithoutThreat, forkWithoutTwoTargetProof, discovered))
    val discoveredVerdict = verdicts.find(_.story == discovered).get
    val defenseVerdict = verdicts.find(_.story == defenseWithoutThreat).get
    val forkVerdict = verdicts.find(_.story == forkWithoutTwoTargetProof).get
    val defensePlan = ExplanationPlan.fromSelected(defenseVerdict).get

    assertEquals(discoveredVerdict.role, Role.Lead)
    assertEquals(discoveredVerdict.leadAllowed, true)
    val discoveredPlan = ExplanationPlan.fromSelected(discoveredVerdict).get
    assertEquals(discoveredPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(
      DeterministicRenderer.fromPlan(discoveredPlan).map(_.text),
      Some("Nf4 reveals an attack on the piece on g6.")
    )
    assertEquals(defenseVerdict.role, Role.Blocked)
    assertEquals(defensePlan.allowedClaim, None)
    assertEquals(defensePlan.debugOnly, true)
    assertEquals(DeterministicRenderer.fromPlan(defensePlan), None)
    assertEquals(forkVerdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(forkVerdict), None)
    assertEquals(verdicts.count(_.role == Role.Lead), 1)

  test("Line-6 ExplanationPlan lowers selected DiscoveredAttack Lead only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('g', 6)))
    assertEquals(plan.anchor, Some(Square('d', 3)))
    assertEquals(plan.route, Some(revealMove))
    assertEquals(plan.routeSan, Some("Nf4"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(plan.evidenceLine, Some(revealMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key), Vector("reveals_attack_on_piece"))
    assertEquals(
      ExplanationClaim.DiscoveredAttackForbiddenKeys,
      Vector(
        "wins_material",
        "pins_piece",
        "skewers_piece",
        "creates_pressure",
        "takes_initiative",
        "mate_threat",
        "best_move",
        "forced",
        "decisive"
      )
    )
    val forbiddenWordingKeys = plan.forbiddenWording.map(_.key)
    ExplanationClaim.DiscoveredAttackForbiddenKeys.foreach: forbiddenKey =>
      assert(forbiddenWordingKeys.contains(forbiddenKey), forbiddenKey)
    assert(forbiddenWordingKeys.contains("winning"))
    assertEquals(
      ExplanationClaim.DiscoveredAttackAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.DiscoveredAttackForbiddenKeys.toSet)
        .isEmpty,
      true
    )
    assertEquals(
      LlmNarrationSmoke.mockNarrate(plan, DeterministicRenderer.fromPlan(plan)),
      Some("Nf4 reveals an attack on the piece on g6.")
    )

  test(
    "Line-6 DiscoveredAttack ExplanationPlan gives no standalone claim to non Lead capped or refuted rows"
  ):
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
        pawnSupport = 0,
        clarity = value
      )

    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(revealMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val leadVerdict = StoryTable.choose(Vector(story)).head
    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = leadVerdict.copy(selected = false)
    val capped = TacticDiscoveredAttack.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refuted =
      TacticDiscoveredAttack
        .withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20))
        .get
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val refutedVerdict = StoryTable.choose(Vector(refuted)).head

    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get.copy(proof = strongProof(99))
    val supportStory = story.copy(proof = strongProof(90))
    val mixedVerdicts = StoryTable.choose(Vector(supportStory, hanging))
    val realSupportVerdict = mixedVerdicts.find(_.story == supportStory).get

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)
    assertEquals(realSupportVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(realSupportVerdict), None)

  test("Line-7 DeterministicRenderer phrases selected DiscoveredAttack ExplanationPlan only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "winning",
        "decisive",
        "best move",
        "forces",
        "pins",
        "skewers",
        "puts pressure",
        "creates a mating threat"
      )

    assertEquals(rendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

  test("Line-7 DeterministicRenderer refuses DiscoveredAttack without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.CanWinPiece)),
      leadPlan.copy(tactic = Some(Tactic.Hanging)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Line-8 LLM smoke reuses 8B prompt contract for DiscoveredAttack RenderedLine only"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assert(prompt.contains("renderedText: Nf4 reveals an attack on the piece on g6."))
    assert(prompt.contains("claimKey: reveals_attack_on_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(
      prompt.contains(
        "forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe, pins piece, skewers piece"
      )
    )
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures")
      .foreach: forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Line-8 LLM smoke rejects DiscoveredAttack additions and stronger claims"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Nf4 wins material on g6." -> "forbidden_wording",
        "Nf4 creates pressure on the piece on g6." -> "forbidden_wording",
        "Nf4 takes initiative against g6." -> "forbidden_wording",
        "Nf4 creates a mating threat after attacking g6." -> "forbidden_wording",
        "Nf4 forces a new line after attacking g6." -> "forbidden_wording",
        "Nf4 pins the piece on g6." -> "forbidden_wording",
        "Nf4 reveals an attack on g6, then Bg2 adds another line." -> "new_move_or_line",
        "Nf4 reveals an attack on g6 because Black is winning." -> "forbidden_wording"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Line Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val story = TacticDiscoveredAttack
      .write(facts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
      .get
    val lineProof = story.lineProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val writerNames = StoryWriter.values.map(_.toString).toVector
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("CaptureResult", "MultiTargetProof", "ThreatProof", "DefenseProof", "EngineCheck").foreach:
      forbidden => assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    assertEquals(lineProof.publicClaimAllowed, false)
    assertEquals(lineProof.complete, true)
    assertEquals(story.lineProof.nonEmpty, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assert(!writerNames.exists(name => Vector("Line", "Ray", "XRay").exists(name.contains)))

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(ExplanationClaim.DiscoveredAttackAllowed.map(_.key), Vector("reveals_attack_on_piece"))
    assert(!ExplanationClaim.DiscoveredAttackAllowed.map(_.key).contains("wins_material"))
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(rendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Nf4 wins material on g6.",
      "Nf4 pins the piece on g6.",
      "Nf4 skewers the piece on g6.",
      "Nf4 creates pressure on g6.",
      "Nf4 creates a mating threat on g6.",
      "Nf4 is the best move and forces a line."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach:
      forbidden => assert(!prompt.contains(forbidden), forbidden)
    Vector(Tactic.AbsPin, Tactic.RelPin, Tactic.Skewer, Tactic.Xray, Tactic.RemoveGuard).foreach: tactic =>
      val forged = story.copy(tactic = Some(tactic))
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None, tactic.toString)

  test("Pin-1 PinProof proves one legal pinned-to-king relation without creating Story"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Some(Line(Square('a', 8), Square('e', 8)))
    val proof = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = pinningMove,
      sliderSquare = Some(Square('e', 8)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.Black)
    assertEquals(proof.pinnedTarget, Some(Piece(Side.White, Man.Knight, Square('e', 2))))
    assertEquals(proof.pinningSlider, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.kingBehindTarget, Some(Piece(Side.White, Man.King, Square('e', 1))))
    assertEquals(proof.pinningMove, pinningMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.File))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.beforePinRelation, false)
    assertEquals(proof.afterPinRelation, true)
    assertEquals(proof.targetNonKing, true)
    assertEquals(proof.targetAndKingSameSide, true)
    assertEquals(proof.sliderAttacksThroughTargetTowardKingAfterMove, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      PinProof.getClass.getDeclaredMethods.toVector ++ classOf[PinProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine")
          .exists(name => method.getReturnType.getSimpleName.contains(name))
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        PinProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "material",
      "kingUnsafe",
      "mate",
      "pressure",
      "initiative",
      "winning",
      "decisive",
      "bestMove",
      "forcedMove",
      "cannotMove"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Pin-1 PinProof keeps false positives and diagnostics out of public output"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Some(Line(Square('a', 8), Square('e', 8)))
    val illegalMove = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = Some(Line(Square('a', 8), Square('e', 7))),
      sliderSquare = Some(Square('e', 7)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )
    val targetIsKing = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = pinningMove,
      sliderSquare = Some(Square('e', 8)),
      targetSquare = Some(Square('e', 1)),
      kingSquare = Some(Square('e', 1))
    )
    val lineNotCreated = PinProof.fromBoardFacts(
      facts = facts,
      pinningMove = Some(Line(Square('a', 8), Square('a', 7))),
      sliderSquare = Some(Square('a', 7)),
      targetSquare = Some(Square('e', 2)),
      kingSquare = Some(Square('e', 1))
    )

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.publicClaimAllowed, false)
    assertEquals(
      illegalMove.missingEvidence.exists(_.missing.contains("legal pinning or revealing move")),
      true
    )
    assertEquals(targetIsKing.complete, false)
    assertEquals(targetIsKing.missingEvidence.exists(_.missing.contains("target non-king")), true)
    assertEquals(lineNotCreated.complete, false)
    assertEquals(
      lineNotCreated.missingEvidence.exists(_.missing.contains("before/after pin relation")),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99,
      forcing = 99,
      immediacy = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.AbsPin),
      side = Side.Black,
      rival = Side.White,
      target = Some(Square('e', 2)),
      anchor = Some(Square('e', 8)),
      route = pinningMove,
      routeSan = BoardFacts.sanFor(facts, pinningMove.get),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(facts, pinningMove.get)
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Pin-2 TacticPin writer admits one proof-backed Pin Story"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story = TacticPin
      .write(
        facts = facts,
        pinningMove = Some(pinningMove),
        sliderSquare = Some(Square('e', 8)),
        targetSquare = Some(Square('e', 2)),
        kingSquare = Some(Square('e', 1))
      )
      .get
    val proof = story.pinProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Pin))
    assertEquals(story.writer, Some(StoryWriter.TacticPin))
    assertEquals(story.side, Side.Black)
    assertEquals(story.rival, Side.White)
    assertEquals(story.target, Some(Square('e', 2)))
    assertEquals(story.anchor, Some(Square('e', 8)))
    assertEquals(story.route, Some(pinningMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, pinningMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.afterPinRelation, true)
    assertEquals(proof.pinnedTarget, Some(Piece(Side.White, Man.Knight, Square('e', 2))))
    assertEquals(proof.pinningSlider, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.kingBehindTarget, Some(Piece(Side.White, Man.King, Square('e', 1))))
    assertEquals(story.captureResult, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("pins_piece")
    )

  test("Pin-2 TacticPin writer blocks refuted unsupported and sibling-meaning rows"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story = TacticPin
      .write(
        facts = facts,
        pinningMove = Some(pinningMove),
        sliderSquare = Some(Square('e', 8)),
        targetSquare = Some(Square('e', 2)),
        kingSquare = Some(Square('e', 1))
      )
      .get
    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 1), Square('f', 1))))),
      evalBefore = Some(EngineEval(80)),
      evalAfter = Some(EngineEval(90)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refuted = TacticPin.withEngineCheck(story, refutes).get
    val targetKing =
      TacticPin.write(
        facts,
        Some(pinningMove),
        Some(Square('e', 8)),
        Some(Square('e', 1)),
        Some(Square('e', 1))
      )
    val noPin =
      TacticPin.write(
        facts,
        Some(Line(Square('a', 8), Square('a', 7))),
        Some(Square('a', 7)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      )
    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedRemoveGuard = story.copy(tactic = Some(Tactic.RemoveGuard))
    val forgedAbsPin = story.copy(tactic = Some(Tactic.AbsPin))

    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refuted)).head.leadAllowed, false)
    assertEquals(targetKing, None)
    assertEquals(noPin, None)
    Vector(forgedMaterial, forgedDefense).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)
    Vector(forgedRemoveGuard, forgedAbsPin).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      assertEquals(ExplanationPlan.fromSelected(verdict), None, forged.toString)

  test("Pin-3 negative corpus keeps incomplete pin relations silent"):
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val untrustedFacts =
      minimalBoardFacts(
        sideToMove = Side.Black,
        sideLegal = readyMoves(line = pinningMove),
        pieces = Vector(
          Piece(Side.Black, Man.Rook, Square('a', 8)),
          Piece(Side.Black, Man.King, Square('g', 8)),
          Piece(Side.White, Man.Knight, Square('e', 2)),
          Piece(Side.White, Man.King, Square('e', 1))
        )
      )
    val nonSliderFacts = BoardFacts.fromFen("n5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val targetWithoutKingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/3K4 b - - 0 1").toOption.get
    val oppositeKingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4n3/4K3 b - - 0 1").toOption.get
    val blockerFacts = BoardFacts.fromFen("r5k1/8/8/8/4B3/8/4N3/4K3 b - - 0 1").toOption.get
    val alreadyPinnedFacts = BoardFacts.fromFen("4r1k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val alreadyPinnedMove = Line(Square('g', 8), Square('h', 8))

    def write(
        facts: BoardFacts,
        move: Line,
        slider: Square,
        target: Square,
        king: Square
    ): Option[Story] =
      TacticPin.write(facts, Some(move), Some(slider), Some(target), Some(king))

    val sameBoardProof =
      PinProof.fromBoardFacts(
        untrustedFacts,
        Some(pinningMove),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      )

    Vector(
      "illegal move" ->
        write(pinFacts, Line(Square('a', 8), Square('a', 7)), Square('a', 7), Square('e', 2), Square('e', 1)),
      "missing same-board proof" ->
        write(untrustedFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "slider is not a slider" ->
        write(
          nonSliderFacts,
          Line(Square('a', 8), Square('b', 6)),
          Square('b', 6),
          Square('e', 2),
          Square('e', 1)
        ),
      "no king behind target" ->
        write(pinFacts, pinningMove, Square('e', 8), Square('e', 2), Square('d', 1)),
      "target and king are not same side" ->
        write(oppositeKingFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "line does not run through target to king" ->
        write(targetWithoutKingFacts, pinningMove, Square('e', 8), Square('e', 2), Square('d', 1)),
      "target is king" ->
        write(pinFacts, pinningMove, Square('e', 8), Square('e', 1), Square('e', 1)),
      "another blocker sits between slider and king" ->
        write(blockerFacts, pinningMove, Square('e', 8), Square('e', 2), Square('e', 1)),
      "pin-looking geometry already existed before move" ->
        write(alreadyPinnedFacts, alreadyPinnedMove, Square('e', 8), Square('e', 2), Square('e', 1))
    ).foreach: (label, story) =>
      assertEquals(story, None, label)

    assertEquals(sameBoardProof.sameBoardProof, false)
    assertEquals(sameBoardProof.complete, false)
    assert(sameBoardProof.missingEvidence.exists(_.missing.contains("same-board proof")))

  test("Pin-3 negative corpus keeps sibling line tactics and pin wording silent"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val skewerFacts = BoardFacts.fromFen("4q3/4k3/8/8/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pinStory =
      TacticPin
        .write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val pinVerdict = StoryTable.choose(Vector(pinStory)).head

    assertEquals(
      TacticPin.write(
        discoveredFacts,
        Some(discoveredMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 8))
      ),
      None,
      "discovered attack without king-behind-target relation is not Pin"
    )
    assertEquals(
      TacticPin.write(
        skewerFacts,
        Some(skewerMove),
        Some(Square('e', 1)),
        Some(Square('e', 8)),
        Some(Square('e', 7))
      ),
      None,
      "skewer-looking front-king line is not Pin"
    )
    assertEquals(pinVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(pinVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(pinVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("pins_piece")
    )

  test("Pin-4 EngineCheck reuses existing statuses for TacticPin only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticPin.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Pin-4 EngineCheck cannot create Pin or leak engine wording"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val wrongRoute = Line(Square('g', 8), Square('h', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(pinningMove),
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(pinningMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticPin.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val methodNames =
      TacticPin.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticPin.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticPin.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticPin.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticPin.withEngineCheck(story, routeMismatch), None)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(
      ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(
      ExplanationPlan.fromSelected(lowEvalVerdict).map(_.forbiddenWording),
      ExplanationPlan.fromSelected(highEvalVerdict).map(_.forbiddenWording)
    )
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))

  test("Pin-5 StoryTable keeps Pin stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get

    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(5), tied(2), tied(4), tied(0), tied(3), tied(1)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      forward.map(_.story.tactic).toSet,
      Set(Some(Tactic.Hanging), Some(Tactic.Fork), Some(Tactic.DiscoveredAttack), Some(Tactic.Pin), None)
    )
    assert(forward.exists(verdict => verdict.story.scene == Scene.Material))
    assert(forward.exists(verdict => verdict.story.scene == Scene.Defense))
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          verdict.toString
        )

  test("Pin-5 StoryTable keeps Pin out of Material king safety and Defense claim homes"):
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial
      .write(materialFacts, materialMove)
      .get
      .copy(proof =
        proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          persistence = 99,
          immediacy = 99,
          forcing = 99,
          conversionPrize = 99,
          counterplayRisk = 20,
          clarity = 99
        )
      )
    val forgedDefense =
      pin.copy(scene = Scene.Defense, tactic = None, writer = None, threatProof = None, defenseProof = None)
    val forgedKingSafety = pin.copy(scene = Scene.King, tactic = None, writer = None)

    val materialPinVerdicts = StoryTable.choose(Vector(pin, material))
    val materialVerdict = materialPinVerdicts.find(_.story.scene == Scene.Material).get
    val pinVerdict = materialPinVerdicts.find(_.story.tactic.contains(Tactic.Pin)).get
    val forgedDefenseVerdict =
      StoryTable.choose(Vector(pin, forgedDefense)).find(_.story == forgedDefense).get
    val forgedKingSafetyVerdict =
      StoryTable.choose(Vector(pin, forgedKingSafety)).find(_.story == forgedKingSafety).get

    assertEquals(materialVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(ExplanationPlan.fromSelected(pinVerdict), None)
    assertEquals(forgedDefenseVerdict.role, Role.Blocked)
    assertEquals(forgedDefenseVerdict.leadAllowed, false)
    val forgedDefensePlan = ExplanationPlan.fromSelected(forgedDefenseVerdict)
    assertEquals(forgedDefensePlan.flatMap(_.allowedClaim), None)
    assertEquals(forgedDefensePlan.flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(forgedKingSafetyVerdict.role, Role.Blocked)
    assertEquals(forgedKingSafetyVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedKingSafetyVerdict), None)

  test("Pin-5 StoryTable prevents duplicate Lead for same-line DiscoveredAttack and Pin"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pin = TacticPin
      .write(pinFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
      .get
    val forward = StoryTable.choose(Vector(discovered, pin))
    val reverse = StoryTable.choose(Vector(pin, discovered))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(revealMove)))
    assertEquals(
      forward.map(_.story.tactic).toSet,
      Set[Option[Tactic]](Some(Tactic.DiscoveredAttack), Some(Tactic.Pin))
    )
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Pin-6 ExplanationPlan lowers selected uncapped Pin Lead only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Pin))
    assertEquals(plan.side, Side.Black)
    assertEquals(plan.target, Some(Square('e', 2)))
    assertEquals(plan.anchor, Some(Square('e', 8)))
    assertEquals(plan.route, Some(pinningMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, pinningMove))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.PinsPiece))
    assertEquals(plan.evidenceLine, Some(pinningMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.PinAllowed.map(_.key), Vector("pins_piece"))
    assertEquals(
      ExplanationClaim.PinForbiddenKeys,
      Vector(
        "wins_material",
        "king_unsafe",
        "mate_threat",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative",
        "cannot_move"
      )
    )
    ExplanationClaim.PinForbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.PinAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)
    assertEquals(
      ExplanationClaim.PinAllowed.map(_.key).toSet.intersect(ExplanationClaim.PinForbiddenKeys.toSet),
      Set.empty
    )

  test("Pin-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Pin rows"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val reply = Line(Square('e', 1), Square('f', 1))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val verdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(pinningMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict =
      StoryTable.choose(Vector(TacticPin.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refutedVerdict =
      StoryTable
        .choose(
          Vector(
            TacticPin.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get
          )
        )
        .head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("Pin-7 DeterministicRenderer phrases selected Pin ExplanationPlan only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "cannot move",
        "the king is unsafe",
        "wins material",
        "winning",
        "decisive",
        "best move",
        "only move",
        "forces",
        "creates pressure",
        "threatens mate"
      )

    assertEquals(rendered.text, "Re8 pins the piece on e2.")
    assertEquals(rendered.claimKey, "pins_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromPinProof"))

  test("Pin-7 DeterministicRenderer refuses Pin without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      leadPlan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = Some(Square('e', 1))),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.PinsPiece)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Pin-8 LLM smoke reuses 8B prompt contract for Pin RenderedLine only"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assert(prompt.contains("renderedText: Re8 pins the piece on e2."))
    assert(prompt.contains("claimKey: pins_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(
      prompt.contains(
        "forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe"
      )
    )
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "PinProof", "LineProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures")
      .foreach: forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Pin-8 LLM smoke rejects Pin additions and stronger claims"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Re8 wins material by pinning the piece on e2." -> "forbidden_wording",
        "Re8 creates pressure on the piece on e2." -> "forbidden_wording",
        "Re8 takes the initiative by pinning e2." -> "forbidden_wording",
        "Re8 threatens mate after pinning e2." -> "forbidden_wording",
        "Re8 means the piece on e2 cannot move." -> "forbidden_wording",
        "Re8 pins the piece on e2, then Re1 adds a new line." -> "new_move_or_line",
        "Re8 pins the piece on e2 and Bg2 adds another line." -> "new_move_or_line",
        "Re8 pins the piece on e2 because Black is winning." -> "forbidden_wording",
        "Engine says Re8 pins the piece on e2." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      plan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "cannot_move")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Pin Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val story =
      TacticPin
        .write(facts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val pinProof = story.pinProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val pinProofSurface =
      classOf[PinProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    val tacticPinSurface =
      TacticPin.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticPin.getClass.getDeclaredFields.map(_.getName).toSet
    val writerNames = StoryWriter.values.map(_.toString).toVector
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("PinProof", "TacticPin", "Story", "Verdict", "ExplanationPlan", "RenderedLine").foreach:
      forbidden => assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector(
      "Story",
      "Verdict",
      "ExplanationPlan",
      "RenderedLine",
      "Material",
      "Defense",
      "RemoveGuard",
      "Skewer"
    ).foreach: forbidden =>
      assert(!pinProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromBoardFacts").foreach: forbidden =>
      assert(!tacticPinSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(pinProof.publicClaimAllowed, false)
    assertEquals(pinProof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.TacticPin))
    assertEquals(story.tactic, Some(Tactic.Pin))
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.pinProof.nonEmpty, true)
    assertEquals(story.lineProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assert(!writerNames.exists(name => Vector("LineTactic", "Ray", "XRay").exists(name.contains)))

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(ExplanationClaim.PinAllowed.map(_.key), Vector("pins_piece"))
    assert(
      !ExplanationClaim.PinAllowed
        .map(_.key)
        .exists(key => key.contains("material") || key.contains("defense"))
    )
    assertEquals(
      ExplanationClaim.PinAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.MaterialAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.PinAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.DefenseAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.PinAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(rendered.claimKey, "pins_piece")
    assertEquals(rendered.text, "Re8 pins the piece on e2.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Re8 wins material by pinning e2.",
      "Re8 creates pressure on e2.",
      "Re8 threatens mate after pinning e2.",
      "Re8 is winning and decisive.",
      "Re8 means the piece on e2 cannot move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("PinProof", "LineProof", "LineFact", "BoardFacts", "EngineCheck", "raw PV", "proofFailures")
      .foreach: forbidden =>
        assert(!prompt.contains(forbidden), forbidden)
    Vector(
      Tactic.AbsPin,
      Tactic.RelPin,
      Tactic.Skewer,
      Tactic.Xray,
      Tactic.RemoveGuard,
      Tactic.DiscoveredAttack
    ).foreach: tactic =>
      val forged = story.copy(tactic = Some(tactic))
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None, tactic.toString)

  test("RemoveGuard-1 RemoveGuardProof proves one legal defender capture without creating Story"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Some(Line(Square('g', 8), Square('c', 4)))
    val proof = RemoveGuardProof.fromBoardFacts(
      facts = facts,
      removeGuardMove = removeGuardMove,
      targetSquare = Some(Square('e', 5)),
      defenderSquare = Some(Square('c', 4))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.guardedTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 5))))
    assertEquals(proof.removedDefender, Some(Piece(Side.Black, Man.Knight, Square('c', 4))))
    assertEquals(proof.removeGuardMove, removeGuardMove)
    assertEquals(proof.removalKind, Some(RemoveGuardRemovalKind.DefenderCaptured))
    assertEquals(proof.targetNonKingMaterial, true)
    assertEquals(proof.defenderGuardedTargetBeforeMove, true)
    assertEquals(proof.afterMoveDefenderNoLongerGuardsTarget, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactBoardAfterMoveRelation, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      RemoveGuardProof.getClass.getDeclaredMethods.toVector ++ classOf[
        RemoveGuardProof
      ].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine")
          .exists(name => method.getReturnType.getSimpleName.contains(name))
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        RemoveGuardProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "materialResult",
      "materialGain",
      "winsMaterial",
      "winning",
      "decisive",
      "bestMove",
      "onlyMove",
      "pressure",
      "initiative",
      "overloaded",
      "sacrificeLure",
      "deflection"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-1 RemoveGuardProof admits exact guard-line block and rejects closed removal theories"):
    val captureFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val blockFacts = BoardFacts.fromFen("7k/8/8/r6q/5N2/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Some(Line(Square('f', 4), Square('d', 5)))
    val blockedGuard = RemoveGuardProof.fromBoardFacts(
      facts = blockFacts,
      removeGuardMove = blockMove,
      targetSquare = Some(Square('h', 5)),
      defenderSquare = Some(Square('a', 5))
    )
    val illegalMove = RemoveGuardProof.fromBoardFacts(
      facts = blockFacts,
      removeGuardMove = Some(Line(Square('f', 4), Square('f', 5))),
      targetSquare = Some(Square('h', 5)),
      defenderSquare = Some(Square('a', 5))
    )
    val defenderStillGuards = RemoveGuardProof.fromBoardFacts(
      facts = captureFacts,
      removeGuardMove = Some(Line(Square('g', 8), Square('f', 7))),
      targetSquare = Some(Square('e', 5)),
      defenderSquare = Some(Square('c', 4))
    )
    val targetIsKing = RemoveGuardProof.fromBoardFacts(
      facts = captureFacts,
      removeGuardMove = Some(Line(Square('g', 8), Square('c', 4))),
      targetSquare = Some(Square('h', 8)),
      defenderSquare = Some(Square('c', 4))
    )

    assertEquals(blockedGuard.complete, true)
    assertEquals(blockedGuard.removalKind, Some(RemoveGuardRemovalKind.GuardLineBlocked))
    assertEquals(blockedGuard.guardedTarget, Some(Piece(Side.Black, Man.Queen, Square('h', 5))))
    assertEquals(blockedGuard.removedDefender, Some(Piece(Side.Black, Man.Rook, Square('a', 5))))
    assertEquals(blockedGuard.defenderGuardedTargetBeforeMove, true)
    assertEquals(blockedGuard.afterMoveDefenderNoLongerGuardsTarget, true)
    assertEquals(blockedGuard.exactBoardAfterMoveRelation, true)

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.missingEvidence.exists(_.missing.contains("legal remove-guard move")), true)
    assertEquals(defenderStillGuards.complete, false)
    assertEquals(
      defenderStillGuards.missingEvidence.exists(
        _.missing.contains("after move defender no longer guards target")
      ),
      true
    )
    assertEquals(targetIsKing.complete, false)
    assertEquals(
      targetIsKing.missingEvidence.exists(_.missing.contains("target non-king material piece")),
      true
    )

    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.RemoveGuard),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('g', 8), Square('c', 4))),
      routeSan = BoardFacts.sanFor(captureFacts, Line(Square('g', 8), Square('c', 4))),
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99
      ),
      storyProof = StoryProof.fromBoardFacts(captureFacts, Line(Square('g', 8), Square('c', 4)))
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Skewer-1 SkewerProof proves one front and rear target relation without creating Story"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Some(Line(Square('a', 1), Square('e', 1)))
    val proof = SkewerProof.fromBoardFacts(
      facts = facts,
      skewerMove = skewerMove,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.side, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.skewerSlider, Some(Piece(Side.White, Man.Rook, Square('e', 1))))
    assertEquals(proof.frontTarget, Some(Piece(Side.Black, Man.Queen, Square('e', 5))))
    assertEquals(proof.rearTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 8))))
    assertEquals(proof.skewerMove, skewerMove)
    assertEquals(proof.lineKind, Some(BoardFacts.LineKind.File))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.frontTargetNonKingMaterial, true)
    assertEquals(proof.rearTargetNonKingMaterial, true)
    assertEquals(proof.frontAndRearSameRivalSide, true)
    assertEquals(proof.afterMoveSliderAttacksFrontTarget, true)
    assertEquals(proof.rearTargetBehindFrontTargetOnSameRay, true)
    assertEquals(proof.noExtraBlockerBreaksFrontToRearRelation, true)
    assertEquals(proof.beforeSkewerRelationAbsentOrBlocked, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      SkewerProof.getClass.getDeclaredMethods.toVector ++ classOf[SkewerProof].getDeclaredMethods.toVector
    val publicOutputReturningMethods = proofHomeMethods
      .filter(method =>
        Vector("Story", "Verdict", "ExplanationPlan", "RenderedLine")
          .exists(name => method.getReturnType.getSimpleName.contains(name))
      )
      .map(_.getName)
    assertEquals(publicOutputReturningMethods, Vector.empty)

    val surfaceNames =
      proofHomeMethods.map(_.getName).toSet ++
        SkewerProof.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "materialGain",
      "materialResult",
      "winsMaterial",
      "mustMove",
      "winsRear",
      "frontMustMove",
      "rearPieceWon",
      "winning",
      "decisive",
      "bestMove",
      "onlyMove",
      "pressure",
      "initiative"
    ).foreach: forbidden =>
      assert(!surfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Skewer-1 SkewerProof keeps false positives and diagnostics out of public output"):
    val positiveFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val extraBlockerFacts = BoardFacts.fromFen("4r2k/8/4b3/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val kingFrontFacts = BoardFacts.fromFen("4r3/8/8/4k3/8/8/8/R6K w - - 0 1").toOption.get
    val relationAlreadyPresentFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/4R2K w - - 0 1").toOption.get
    val move = Some(Line(Square('a', 1), Square('e', 1)))

    val illegalMove = SkewerProof.fromBoardFacts(
      facts = positiveFacts,
      skewerMove = Some(Line(Square('a', 1), Square('d', 1))),
      sliderSquare = Some(Square('d', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val targetIsKing = SkewerProof.fromBoardFacts(
      facts = kingFrontFacts,
      skewerMove = move,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val extraBlocker = SkewerProof.fromBoardFacts(
      facts = extraBlockerFacts,
      skewerMove = move,
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )
    val alreadyPresent = SkewerProof.fromBoardFacts(
      facts = relationAlreadyPresentFacts,
      skewerMove = Some(Line(Square('h', 1), Square('h', 2))),
      sliderSquare = Some(Square('e', 1)),
      frontTargetSquare = Some(Square('e', 5)),
      rearTargetSquare = Some(Square('e', 8))
    )

    assertEquals(illegalMove.complete, false)
    assertEquals(illegalMove.publicClaimAllowed, false)
    assertEquals(
      illegalMove.missingEvidence.exists(_.missing.contains("legal skewer or revealing move")),
      true
    )
    assertEquals(targetIsKing.complete, false)
    assertEquals(
      targetIsKing.missingEvidence.exists(_.missing.contains("front target non-king material piece")),
      true
    )
    assertEquals(extraBlocker.complete, false)
    assertEquals(
      extraBlocker.missingEvidence.exists(
        _.missing.contains("no extra blocker breaks front-to-rear relation")
      ),
      true
    )
    assertEquals(alreadyPresent.complete, false)
    assertEquals(
      alreadyPresent.missingEvidence.exists(
        _.missing.contains("before-move skewer relation absent or blocked")
      ),
      true
    )

    val highProof = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99
    )
    val forgedStory = Story(
      scene = Scene.Tactic,
      tactic = Some(Tactic.Skewer),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('e', 1)),
      route = Some(Line(Square('a', 1), Square('e', 1))),
      proof = highProof
    )
    val forgedVerdict = StoryTable.choose(Vector(forgedStory)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forgedVerdict), None)

  test("Skewer-2 TacticSkewer writer admits one proof-backed Skewer Story without downstream speech"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story = TacticSkewer
      .write(
        facts = facts,
        skewerMove = Some(skewerMove),
        sliderSquare = Some(Square('e', 1)),
        frontTargetSquare = Some(Square('e', 5)),
        rearTargetSquare = Some(Square('e', 8))
      )
      .get
    val proof = story.skewerProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Skewer))
    assertEquals(story.writer, Some(StoryWriter.TacticSkewer))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.secondaryTarget, Some(Square('e', 8)))
    assertEquals(story.anchor, Some(Square('e', 1)))
    assertEquals(story.route, Some(skewerMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, skewerMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.skewerMove, Some(skewerMove))
    assertEquals(proof.skewerSlider.map(_.square), Some(Square('e', 1)))
    assertEquals(proof.frontTarget.map(_.square), Some(Square('e', 5)))
    assertEquals(proof.rearTarget.map(_.square), Some(Square('e', 8)))
    assertEquals(proof.afterMoveSliderAttacksFrontTarget, true)
    assertEquals(proof.rearTargetBehindFrontTargetOnSameRay, true)
    assertEquals(proof.noExtraBlockerBreaksFrontToRearRelation, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Skewer-2 TacticSkewer writer blocks incomplete refuted material pin and rear king paths"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story = TacticSkewer
      .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
      .get
    val rearKingFacts = BoardFacts.fromFen("4k3/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val pinLookingFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get

    assertEquals(
      TacticSkewer.write(
        facts,
        Some(Line(Square('a', 1), Square('d', 1))),
        Some(Square('d', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      ),
      None
    )
    assertEquals(
      TacticSkewer.write(
        rearKingFacts,
        Some(skewerMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8))
      ),
      None
    )
    assertEquals(
      TacticSkewer.write(
        pinLookingFacts,
        Some(Line(Square('a', 8), Square('e', 8))),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1))
      ),
      None
    )

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(220)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    assertEquals(refutes.status, EngineCheckStatus.Refutes)
    val refutedStory = TacticSkewer.withEngineCheck(story, refutes).get
    val refutedVerdict = StoryTable.choose(Vector(refutedStory)).head
    assertEquals(refutedStory.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedPin = story.copy(tactic = Some(Tactic.Pin), secondaryTarget = None)
    Vector(forgedMaterial, forgedPin).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked)
      assertEquals(forgedVerdict.leadAllowed, false)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict).flatMap(_.allowedClaim), None)

  test("Skewer-3 negative corpus keeps incomplete skewer relations silent"):
    val legalMove = Line(Square('a', 1), Square('e', 1))
    val positiveFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val sameBoardMissingFacts = minimalBoardFacts(
      sideToMove = Side.White,
      sideLegal = readyMoves(line = legalMove),
      rivalLegal = readyMoves(line = Line(Square('h', 8), Square('h', 7))),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('h', 1)),
        Piece(Side.White, Man.Rook, Square('a', 1)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.Black, Man.Queen, Square('e', 5)),
        Piece(Side.Black, Man.Rook, Square('e', 8))
      )
    )
    val corpus = Vector(
      (
        "legal move missing",
        positiveFacts,
        Some(Line(Square('a', 1), Square('d', 1))),
        Some(Square('d', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "legal skewer or revealing move"
      ),
      (
        "same-board proof missing",
        sameBoardMissingFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "same-board proof"
      ),
      (
        "slider is not a slider",
        BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/N6K w - - 0 1").toOption.get,
        Some(Line(Square('a', 1), Square('e', 2))),
        Some(Square('e', 2)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "skewer slider"
      ),
      (
        "front target missing",
        positiveFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        None,
        Some(Square('e', 8)),
        "front target"
      ),
      (
        "rear target missing",
        positiveFacts,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        None,
        "rear target"
      ),
      (
        "front target is king",
        BoardFacts.fromFen("4r3/8/8/4k3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "front target non-king material piece"
      ),
      (
        "rear target is king",
        BoardFacts.fromFen("4k3/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "rear target non-king material piece"
      ),
      (
        "front and rear targets are not the same rival side",
        BoardFacts.fromFen("4B2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "front and rear target same rival side"
      ),
      (
        "rear target is not behind the front target on the same line",
        BoardFacts.fromFen("3r3k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('d', 8)),
        "rear target behind front target on same ray"
      ),
      (
        "middle blocker breaks the front-to-rear relation",
        BoardFacts.fromFen("4r2k/8/4b3/4q3/8/8/8/R6K w - - 0 1").toOption.get,
        Some(legalMove),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "no extra blocker breaks front-to-rear relation"
      ),
      (
        "discovered attack only is not a skewer",
        BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        Some(Line(Square('d', 3), Square('f', 4))),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        None,
        "rear target"
      ),
      (
        "pin-looking position is not a skewer",
        BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get,
        Some(Line(Square('a', 8), Square('e', 8))),
        Some(Square('e', 8)),
        Some(Square('e', 2)),
        Some(Square('e', 1)),
        "rear target non-king material piece"
      ),
      (
        "already-present relation needs front-piece-must-move assumption",
        BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/4R1NK w - - 0 1").toOption.get,
        Some(Line(Square('g', 1), Square('f', 3))),
        Some(Square('e', 1)),
        Some(Square('e', 5)),
        Some(Square('e', 8)),
        "before-move skewer relation absent or blocked"
      )
    )

    corpus.foreach: (label, facts, move, slider, front, rear, expectedMissing) =>
      val proof = SkewerProof.fromBoardFacts(facts, move, slider, front, rear)
      assertEquals(proof.complete, false, label)
      assertEquals(proof.publicClaimAllowed, false, label)
      assertEquals(proof.missingEvidence.exists(_.missing.contains(expectedMissing)), true, label)
      assertEquals(TacticSkewer.write(facts, move, slider, front, rear), None, label)

  test("Skewer-3 negative corpus blocks material forced best-move and front-must-move wording"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val verdict = StoryTable.choose(Vector(story)).head
    val writerSurface =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    val proofText = story.skewerProof.toString.toLowerCase

    assertEquals(verdict.role, Role.Context)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(verdict), None)
    Vector(
      "materialWin",
      "materialGain",
      "winsMaterial",
      "forced",
      "bestMove",
      "frontMustMove",
      "mustMove",
      "winsRear",
      "rearPieceWon"
    ).foreach: forbidden =>
      assert(!writerSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
      assert(!proofText.contains(forbidden.toLowerCase), forbidden)

  test("Skewer-4 EngineCheck reuses existing statuses for TacticSkewer only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticSkewer.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Context)
    assertEquals(unknownVerdict.leadAllowed, false)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict), None)

    assertEquals(supportsVerdict.role, Role.Context)
    assertEquals(supportsVerdict.leadAllowed, false)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict), None)

    assertEquals(capsVerdict.role, Role.Context)
    assertEquals(capsVerdict.leadAllowed, false)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("Skewer-4 EngineCheck cannot create Skewer or leak engine wording"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val wrongRoute = Line(Square('h', 1), Square('g', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(skewerMove),
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(skewerMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticSkewer.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val forgedFromEngine = story.copy(writer = None, skewerProof = None)
    val forgedVerdict = StoryTable.choose(Vector(forgedFromEngine)).head
    val methodNames =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticSkewer.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticSkewer.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticSkewer.withEngineCheck(story, routeMismatch), None)
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(lowEvalVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(highEvalVerdict), None)
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))
    Vector("engineSays", "bestMove", "onlyMove", "forcedWin", "winningTactic", "rawPv", "evalNumber").foreach:
      forbidden => assert(!methodNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Skewer-5 StoryTable keeps Skewer stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val material = SceneMaterial.write(materialFacts, hangingMove).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin
      .write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
      .get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
        .get
    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewerBase =
      TacticSkewer
        .write(
          skewerFacts,
          Some(skewerMove),
          Some(Square('e', 1)),
          Some(Square('e', 5)),
          Some(Square('e', 8))
        )
        .get
    val skewerHighProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val skewer = skewerBase.copy(proof = skewerHighProof)

    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high),
      removeGuard.copy(proof = high),
      skewer.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled =
      StoryTable.choose(Vector(tied(7), tied(2), tied(5), tied(0), tied(6), tied(3), tied(1), tied(4)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      forward.map(verdict => verdict.story.scene -> verdict.story.tactic).toSet,
      Set[(Scene, Option[Tactic])](
        Scene.Tactic -> Some(Tactic.Hanging),
        Scene.Tactic -> Some(Tactic.Fork),
        Scene.Material -> None,
        Scene.Defense -> None,
        Scene.Tactic -> Some(Tactic.DiscoveredAttack),
        Scene.Tactic -> Some(Tactic.Pin),
        Scene.Tactic -> Some(Tactic.RemoveGuard),
        Scene.Tactic -> Some(Tactic.Skewer)
      )
    )
    val skewerVerdict = forward.find(_.story.tactic.contains(Tactic.Skewer)).get
    assert(skewerVerdict.role != Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(skewerVerdict), None)
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          verdict.toString
        )

  test("Skewer-5 keeps Material Pin and RemoveGuard claim homes separate"):
    val materialSkewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val materialSkewerMove = Line(Square('a', 1), Square('e', 1))
    val materialSkewer =
      TacticSkewer
        .write(
          materialSkewerFacts,
          Some(materialSkewerMove),
          Some(Square('e', 1)),
          Some(Square('e', 5)),
          Some(Square('e', 8))
        )
        .get
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinMove = Line(Square('d', 3), Square('f', 4))
    val pin = TacticPin
      .write(pinFacts, Some(pinMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
      .get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
        .get

    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    val lowerSkewerForMaterial = materialSkewer.copy(proof = score(80))
    val highMaterial = material.copy(proof = score(99))
    val materialCollision = StoryTable.choose(Vector(lowerSkewerForMaterial, highMaterial))
    val materialVerdict = materialCollision.find(_.story == highMaterial).get
    val skewerMaterialVerdict = materialCollision.find(_.story == lowerSkewerForMaterial).get

    assertEquals(materialCollision.count(_.role == Role.Lead), 1)
    assertEquals(materialVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(materialVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(ExplanationPlan.fromSelected(skewerMaterialVerdict), None)

    val highPin = pin.copy(proof = score(99))
    val lowerSkewerForPin = materialSkewer.copy(proof = score(80))
    val pinCollision = StoryTable.choose(Vector(lowerSkewerForPin, highPin))
    val pinVerdict = pinCollision.find(_.story == highPin).get
    val skewerPinVerdict = pinCollision.find(_.story == lowerSkewerForPin).get

    assertEquals(pinCollision.count(_.role == Role.Lead), 1)
    assertEquals(pinVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(pinVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(ExplanationPlan.fromSelected(skewerPinVerdict), None)

    val highRemoveGuard = removeGuard.copy(proof = score(99))
    val lowerSkewerForRemoveGuard = materialSkewer.copy(proof = score(80))
    val removeGuardCollision = StoryTable.choose(Vector(lowerSkewerForRemoveGuard, highRemoveGuard))
    val removeGuardVerdict = removeGuardCollision.find(_.story == highRemoveGuard).get
    val skewerRemoveGuardVerdict = removeGuardCollision.find(_.story == lowerSkewerForRemoveGuard).get

    assertEquals(removeGuardCollision.count(_.role == Role.Lead), 1)
    assertEquals(removeGuardVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(removeGuardVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assertEquals(ExplanationPlan.fromSelected(skewerRemoveGuardVerdict), None)

  test("Skewer-5 separates DiscoveredAttack collision and keeps incomplete Skewer silent"):
    val skewerDiscoveredFacts = BoardFacts.fromFen("7k/7q/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val revealMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val skewer =
      TacticSkewer
        .write(
          skewerDiscoveredFacts,
          Some(revealMove),
          Some(Square('b', 1)),
          Some(Square('g', 6)),
          Some(Square('h', 7))
        )
        .get
    val forward = StoryTable.choose(Vector(discovered, skewer))
    val reverse = StoryTable.choose(Vector(skewer, discovered))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(revealMove)))
    assertEquals(
      forward.map(_.story.tactic).toSet,
      Set[Option[Tactic]](Some(Tactic.DiscoveredAttack), Some(Tactic.Skewer))
    )
    forward
      .filter(_.story.tactic.contains(Tactic.Skewer))
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None)

    val discoveredOnlyFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/KB6 w - - 0 1").toOption.get
    val discoveredOnly =
      TacticDiscoveredAttack
        .write(discoveredOnlyFacts, Some(revealMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val incompleteSkewer =
      TacticSkewer.write(
        discoveredOnlyFacts,
        Some(revealMove),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      )
    val discoveredOnlyVerdict = StoryTable.choose(Vector(discoveredOnly)).head

    assertEquals(incompleteSkewer, None)
    assertEquals(discoveredOnlyVerdict.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(discoveredOnlyVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )

  test("Skewer-6 ExplanationPlan lowers selected uncapped Skewer Lead only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenKeys =
      Vector(
        "wins_material",
        "wins_rear_piece",
        "front_piece_must_move",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "king_unsafe",
        "mate_threat",
        "creates_pressure",
        "takes_initiative"
      )

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Skewer))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('e', 8)))
    assertEquals(plan.anchor, Some(Square('e', 1)))
    assertEquals(plan.route, Some(skewerMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, skewerMove))
    assertEquals(plan.allowedClaim.map(_.key), Some("skewers_piece_to_piece"))
    assertEquals(plan.evidenceLine, Some(skewerMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(
      ExplanationClaim.values.map(_.key).filter(_.contains("skewer")).toVector,
      Vector("skewers_piece_to_piece")
    )
    forbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.values.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)

  test(
    "Skewer-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Skewer rows"
  ):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val verdict = StoryTable.choose(Vector(leadStory)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(leadStory),
        engineLine = Some(EngineLine(Vector(skewerMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict =
      StoryTable
        .choose(Vector(TacticSkewer.withEngineCheck(leadStory, check(EngineCheckStatus.Caps)).get))
        .head
    val refutedVerdict =
      StoryTable
        .choose(
          Vector(
            TacticSkewer
              .withEngineCheck(leadStory, check(EngineCheckStatus.Supports, before = 220, after = 20))
              .get
          )
        )
        .head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("Skewer-7 DeterministicRenderer phrases selected Skewer ExplanationPlan only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "wins the piece behind it",
        "front piece must move",
        "best move",
        "only move",
        "forces",
        "decisive",
        "king is unsafe",
        "threatens mate",
        "creates pressure"
      )

    assertEquals(rendered.text, s"${plan.routeSan.get} skewers the piece on e5 to the piece on e8.")
    assertEquals(rendered.claimKey, "skewers_piece_to_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromSkewerProof"))

  test("Skewer-7 DeterministicRenderer refuses Skewer without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      leadPlan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = None),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.SkewersPiece)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("Skewer-8 LLM smoke reuses 8B prompt contract for Skewer RenderedLine only"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assert(prompt.contains(s"renderedText: ${rendered.text}"))
    assert(prompt.contains("claimKey: skewers_piece_to_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(
      prompt.contains(
        "forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe"
      )
    )
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "SkewerProof", "LineProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures")
      .foreach: forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("Skewer-8 LLM smoke rejects Skewer additions and stronger claims"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story.copy(proof = highProof))).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Re1 wins material with a skewer on e5 and e8." -> "forbidden_wording",
        "Re1 wins the piece behind it." -> "forbidden_wording",
        "Re1 means the front piece must move." -> "forbidden_wording",
        "Re1 forces the skewer on e5 and e8." -> "forbidden_wording",
        "Re1 creates pressure with the skewer." -> "forbidden_wording",
        "Re1 takes the initiative with the skewer." -> "forbidden_wording",
        "Re1 threatens mate after the skewer." -> "forbidden_wording",
        "Re1 skewers the piece on e5 to e8, then Qxe5 adds a new line." -> "new_move_or_line",
        "Re1 skewers the piece on e5 to e8 and Bg2 adds another line." -> "new_move_or_line",
        "Engine says Re1 skewers e5 to e8." -> "engine_mention",
        "The raw PV confirms Re1 skewers e5 to e8." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.RevealsAttackOnPiece)),
      plan.copy(tactic = Some(Tactic.DiscoveredAttack)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("Skewer Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val story =
      TacticSkewer
        .write(facts, Some(skewerMove), Some(Square('e', 1)), Some(Square('e', 5)), Some(Square('e', 8)))
        .get
    val highProof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val leadStory = story.copy(proof = highProof)
    val skewerProof = leadStory.skewerProof.get
    val verdict = StoryTable.choose(Vector(leadStory)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val skewerProofSurface =
      classOf[SkewerProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[SkewerProof].getDeclaredFields.map(_.getName).toSet
    val tacticSkewerSurface =
      TacticSkewer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticSkewer.getClass.getDeclaredFields.map(_.getName).toSet
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!lineFactSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector("SkewerProof", "TacticSkewer", "Story", "Verdict", "ExplanationPlan", "RenderedLine").foreach:
      forbidden => assert(!lineProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector(
      "Story",
      "Verdict",
      "ExplanationPlan",
      "RenderedLine",
      "Hanging",
      "Defense",
      "Pin",
      "DiscoveredAttack",
      "RemoveGuard"
    ).foreach: forbidden =>
      assert(!skewerProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromLineProof").foreach: forbidden =>
      assert(!tacticSkewerSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    Vector(
      "materialGain",
      "winsMaterial",
      "winsRearPiece",
      "frontPieceMustMove",
      "forcedSkewer",
      "pressure",
      "initiative",
      "mateThreat"
    ).foreach: forbidden =>
      assert(!skewerProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(skewerProof.publicClaimAllowed, false)
    assertEquals(skewerProof.complete, true)
    assertEquals(leadStory.writer, Some(StoryWriter.TacticSkewer))
    assertEquals(leadStory.tactic, Some(Tactic.Skewer))
    assertEquals(leadStory.scene, Scene.Tactic)
    assertEquals(leadStory.skewerProof.nonEmpty, true)
    assertEquals(leadStory.captureResult, None)
    assertEquals(leadStory.multiTargetProof, None)
    assertEquals(leadStory.threatProof, None)
    assertEquals(leadStory.defenseProof, None)
    assertEquals(leadStory.lineProof, None)
    assertEquals(leadStory.pinProof, None)
    assertEquals(leadStory.removeGuardProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("skewers_piece_to_piece"))
    assertEquals(ExplanationClaim.SkewerAllowed.map(_.key), Vector("skewers_piece_to_piece"))
    Vector(
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.DefenseAllowed
    ).foreach: siblingAllowed =>
      assertEquals(
        ExplanationClaim.SkewerAllowed.map(_.key).toSet.intersect(siblingAllowed.map(_.key).toSet),
        Set.empty
      )
    Vector("wins_material", "wins_rear_piece", "front_piece_must_move", "forced").foreach: forbiddenKey =>
      assert(!ExplanationClaim.SkewerAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(ExplanationClaim.SkewerForbiddenKeys.contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)

    assertEquals(rendered.claimKey, "skewers_piece_to_piece")
    assertEquals(rendered.text, "Re1 skewers the piece on e5 to the piece on e8.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Re1 wins rear piece.",
      "Re1 wins the piece behind it.",
      "Re1 wins material with a skewer.",
      "Re1 is a forced skewer.",
      "Re1 means the front piece must move.",
      "Re1 creates pressure with the skewer.",
      "Re1 takes the initiative with the skewer.",
      "Re1 threatens mate after the skewer.",
      "Re1 starts a pin."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector(
      "raw Story",
      "SkewerProof",
      "LineProof",
      "LineFact",
      "BoardFacts",
      "EngineCheck",
      "raw PV",
      "proofFailures"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)

    Vector(
      leadStory.copy(scene = Scene.Material, tactic = None),
      leadStory.copy(scene = Scene.Defense, tactic = None),
      leadStory.copy(tactic = Some(Tactic.Hanging)),
      leadStory.copy(tactic = Some(Tactic.Pin), secondaryTarget = None),
      leadStory.copy(tactic = Some(Tactic.DiscoveredAttack), secondaryTarget = None),
      leadStory.copy(tactic = Some(Tactic.RemoveGuard), secondaryTarget = None)
    ).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, forged.toString)
      assertEquals(ExplanationPlan.fromSelected(forgedVerdict).flatMap(_.allowedClaim), None, forged.toString)

  test("RemoveGuard-2 TacticRemoveGuard writer admits one proof-backed RemoveGuard Story"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(
        facts = facts,
        removeGuardMove = Some(removeGuardMove),
        targetSquare = Some(Square('e', 5)),
        defenderSquare = Some(Square('c', 4))
      )
      .get
    val proof = story.removeGuardProof.get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(story.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.anchor, Some(Square('c', 4)))
    assertEquals(story.route, Some(removeGuardMove))
    assertEquals(story.routeSan, BoardFacts.sanFor(facts, removeGuardMove))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.exactBoardAfterMoveRelation, true)
    assertEquals(proof.removalKind, Some(RemoveGuardRemovalKind.DefenderCaptured))
    assertEquals(proof.guardedTarget, Some(Piece(Side.Black, Man.Rook, Square('e', 5))))
    assertEquals(proof.removedDefender, Some(Piece(Side.Black, Man.Knight, Square('c', 4))))
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("removes_defender")
    )

  test("RemoveGuard-2 TacticRemoveGuard writer blocks refuted unsupported and sibling-meaning rows"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get
    val refuted = story.copy(
      engineCheck = Some(
        EngineCheck(
          sameBoardProof = true,
          checkedMove = Some(removeGuardMove),
          engineLine = Some(EngineLine(Vector(removeGuardMove))),
          replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
          evalBefore = Some(EngineEval(0)),
          evalAfter = Some(EngineEval(0)),
          depth = Some(18),
          freshnessPly = Some(0),
          status = EngineCheckStatus.Refutes,
          missingEvidence = Vector.empty,
          storyBound = true
        )
      )
    )
    val illegal =
      TacticRemoveGuard.write(
        facts,
        Some(Line(Square('g', 8), Square('f', 7))),
        Some(Square('e', 5)),
        Some(Square('c', 4))
      )
    val targetKing =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('h', 8)), Some(Square('c', 4)))
    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedHanging = story.copy(tactic = Some(Tactic.Hanging))

    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refuted)).head.leadAllowed, false)
    assertEquals(illegal, None)
    assertEquals(targetKing, None)

    Vector(forgedMaterial, forgedDefense, forgedHanging).foreach: forged =>
      val verdict = StoryTable.choose(Vector(forged)).head
      assertEquals(verdict.role, Role.Blocked, forged.toString)
      assertEquals(verdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)

    val writerSurfaceNames =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet
    Vector(
      "materialClaim",
      "winsMaterial",
      "hanging",
      "refutesDefense",
      "noDefense",
      "pressure",
      "initiative"
    )
      .foreach: forbidden =>
        assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-3 negative corpus keeps incomplete guard-removal false positives silent"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val untrustedFacts =
      minimalBoardFacts(
        sideToMove = Side.White,
        sideLegal = readyMoves(line = removeGuardMove, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('h', 1)),
          Piece(Side.White, Man.Bishop, Square('g', 8)),
          Piece(Side.Black, Man.King, Square('h', 8)),
          Piece(Side.Black, Man.Rook, Square('e', 5)),
          Piece(Side.Black, Man.Knight, Square('c', 4))
        )
      )
    val corpus = Vector(
      (
        "legal move missing",
        facts,
        Some(Line(Square('g', 8), Square('g', 7))),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "legal remove-guard move"
      ),
      (
        "same-board proof missing",
        untrustedFacts,
        Some(removeGuardMove),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "same-board proof"
      ),
      (
        "target is king",
        facts,
        Some(removeGuardMove),
        Some(Square('h', 8)),
        Some(Square('c', 4)),
        "target non-king material piece"
      ),
      (
        "defender did not guard target",
        BoardFacts.fromFen("6Bk/8/7b/4r3/2n5/8/8/7K w - - 0 1").toOption.get,
        Some(removeGuardMove),
        Some(Square('h', 6)),
        Some(Square('c', 4)),
        "defender guarded target before move"
      ),
      (
        "defender still guards after move",
        facts,
        Some(Line(Square('g', 8), Square('f', 7))),
        Some(Square('e', 5)),
        Some(Square('c', 4)),
        "after move defender no longer guards target"
      )
    )

    corpus.foreach: (label, board, move, target, defender, expectedMissing) =>
      val proof = RemoveGuardProof.fromBoardFacts(board, move, target, defender)
      assertEquals(proof.complete, false, label)
      assertEquals(proof.missingEvidence.exists(_.missing.contains(expectedMissing)), true, label)
      assertEquals(TacticRemoveGuard.write(board, move, target, defender), None, label)

  test("RemoveGuard-3 negative corpus blocks broad claims and sibling tactic misclassification"):
    val defendedFacts = BoardFacts.fromFen("6Bk/6b1/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story = TacticRemoveGuard
      .write(defendedFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(story.removeGuardProof.exists(_.complete), true)
    assertEquals(defendedFacts.seen.guards.exists(_.target.square == Square('e', 5)), true)
    assertEquals(story.captureResult, None)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("removes_defender")
    )

    val forgedMaterial = story.copy(scene = Scene.Material, tactic = None)
    val forgedHanging = story.copy(tactic = Some(Tactic.Hanging))
    val forgedDefense = story.copy(scene = Scene.Defense, tactic = None)
    val forgedOverload = story.copy(tactic = Some(Tactic.Overload))
    val forgedDeflection = story.copy(tactic = Some(Tactic.Deflect))
    val forgedPin = story.copy(tactic = Some(Tactic.Pin))
    val forgedDiscoveredAttack = story.copy(tactic = Some(Tactic.DiscoveredAttack))
    val forgedSkewer = story.copy(tactic = Some(Tactic.Skewer))

    Vector(
      forgedMaterial,
      forgedHanging,
      forgedDefense,
      forgedOverload,
      forgedDeflection,
      forgedPin,
      forgedDiscoveredAttack,
      forgedSkewer
    ).foreach: forged =>
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, forged.toString)
      assertEquals(forgedVerdict.leadAllowed, false, forged.toString)
      val plan = ExplanationPlan.fromSelected(forgedVerdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, forged.toString)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, forged.toString)

    val surfaceText =
      (TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toVector ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toVector)
        .mkString(" ")
        .toLowerCase
    Vector("noDefense", "winsMaterial", "bestMove", "overloaded", "deflection", "refutesDefense").foreach:
      forbidden => assert(!surfaceText.contains(forbidden.toLowerCase), forbidden)

  test("RemoveGuard-4 EngineCheck reuses existing statuses for TacticRemoveGuard only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      TacticRemoveGuard
        .withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20))
        .get

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    Vector(unknown, supports, caps, refutes).foreach: checkedStory =>
      assertEquals(checkedStory.engineCheck.exists(_.publicClaimAllowed), false, checkedStory.toString)

    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("RemoveGuard-4 EngineCheck cannot create RemoveGuard or leak engine wording"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val wrongRoute = Line(Square('g', 8), Square('f', 7))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story = TacticRemoveGuard
      .write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
      .get

    val standaloneEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(removeGuardMove),
      engineLine = Some(EngineLine(Vector(removeGuardMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(removeGuardMove))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(-900)),
      evalAfter = Some(EngineEval(900)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(reply))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(200)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    def lowOrHighEval(before: Int, after: Int) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      StoryTable.choose(Vector(TacticRemoveGuard.withEngineCheck(story, check).get)).head

    val lowEvalVerdict = lowOrHighEval(-900, -850)
    val highEvalVerdict = lowOrHighEval(850, 900)
    val forgedFromEngine = story.copy(writer = None, removeGuardProof = None)
    val forgedVerdict = StoryTable.choose(Vector(forgedFromEngine)).head
    val methodNames =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(standaloneEvidence.evidenceReady, true)
    assertEquals(standaloneEvidence.storyBound, false)
    assertEquals(standaloneEvidence.status, EngineCheckStatus.Supports)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, standaloneEvidence), None)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, engineOnly), None)
    assertEquals(routeMismatch.status, EngineCheckStatus.Unknown)
    assertEquals(TacticRemoveGuard.withEngineCheck(story, routeMismatch), None)
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(lowEvalVerdict.role, highEvalVerdict.role)
    assertEquals(lowEvalVerdict.strength, highEvalVerdict.strength)
    assertEquals(lowEvalVerdict.values, highEvalVerdict.values)
    assertEquals(
      ExplanationPlan.fromSelected(lowEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assertEquals(
      ExplanationPlan.fromSelected(highEvalVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!methodNames.contains("fromEngineEvidence"))
    Vector("engineSays", "bestMove", "onlyMove", "winningTactic", "rawPv", "evalNumber").foreach: forbidden =>
      assert(!methodNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("RemoveGuard-5 StoryTable keeps RemoveGuard stable against existing open rows"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
        .get

    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    val high = score(99)
    val tied = Vector(
      hanging.copy(proof = high),
      fork.copy(proof = high),
      material.copy(proof = high),
      defense.copy(proof = high),
      discovered.copy(proof = high),
      pin.copy(proof = high),
      removeGuard.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(6), tied(3), tied(1), tied(5), tied(0), tied(4), tied(2)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      forward.map(verdict => verdict.story.scene -> verdict.story.tactic).toSet,
      Set[(Scene, Option[Tactic])](
        Scene.Tactic -> Some(Tactic.Hanging),
        Scene.Tactic -> Some(Tactic.Fork),
        Scene.Material -> None,
        Scene.Defense -> None,
        Scene.Tactic -> Some(Tactic.DiscoveredAttack),
        Scene.Tactic -> Some(Tactic.Pin),
        Scene.Tactic -> Some(Tactic.RemoveGuard)
      )
    )
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          verdict.toString
        )

  test("RemoveGuard-5 keeps material and hanging claim homes over same-route guard capture"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val material = SceneMaterial.write(facts, removeGuardMove).get
    val hanging = TacticHanging.write(facts, removeGuardMove).get

    def score(value: Int): Proof =
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
        clarity = value
      )

    val highRemoveGuard = removeGuard.copy(proof = score(99))
    val sameRouteMaterial = material.copy(proof = score(90))
    val sameRouteHanging = hanging.copy(proof = score(80))
    val materialCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteMaterial))
    val hangingCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteHanging))
    val allCollision = StoryTable.choose(Vector(highRemoveGuard, sameRouteMaterial, sameRouteHanging))

    assertEquals(materialCollision.count(_.role == Role.Lead), 1)
    assertEquals(materialCollision.find(_.story == sameRouteMaterial).map(_.role), Some(Role.Lead))
    assertEquals(materialCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)
    assertEquals(
      ExplanationPlan
        .fromSelected(materialCollision.find(_.story == sameRouteMaterial).get)
        .flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    assertEquals(ExplanationPlan.fromSelected(materialCollision.find(_.story == highRemoveGuard).get), None)

    assertEquals(hangingCollision.count(_.role == Role.Lead), 1)
    assertEquals(hangingCollision.find(_.story == sameRouteHanging).map(_.role), Some(Role.Lead))
    assertEquals(hangingCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)
    assertEquals(
      ExplanationPlan
        .fromSelected(hangingCollision.find(_.story == sameRouteHanging).get)
        .flatMap(_.allowedClaim),
      Some(ExplanationClaim.CanWinPiece)
    )

    assertEquals(allCollision.count(_.role == Role.Lead), 1)
    assertEquals(allCollision.find(_.story == sameRouteHanging).map(_.role), Some(Role.Lead))
    assertEquals(allCollision.find(_.story == sameRouteMaterial).map(_.role), Some(Role.Support))
    assertEquals(allCollision.find(_.story == highRemoveGuard).exists(_.role == Role.Lead), false)

  test("RemoveGuard-5 blocks incomplete Defense reaction and duplicate Pin RemoveGuard lead"):
    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4)))
        .get
    val forgedDefense =
      removeGuard.copy(
        scene = Scene.Defense,
        tactic = None,
        writer = Some(StoryWriter.SceneDefense),
        threatProof = None,
        defenseProof = None
      )
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinningMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinningMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get
    val pinRemoveGuard = StoryTable.choose(Vector(pin, removeGuard))
    val reverse = StoryTable.choose(Vector(removeGuard, pin))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.route, verdict.role))

    val forgedDefenseVerdict =
      StoryTable.choose(Vector(removeGuard, forgedDefense)).find(_.story == forgedDefense).get
    assertEquals(forgedDefenseVerdict.role, Role.Blocked)
    assertEquals(forgedDefenseVerdict.leadAllowed, false)
    val forgedDefensePlan = ExplanationPlan.fromSelected(forgedDefenseVerdict)
    assertEquals(forgedDefensePlan.flatMap(_.allowedClaim), None)
    assertEquals(forgedDefensePlan.flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(shape(pinRemoveGuard), shape(reverse))
    assertEquals(pinRemoveGuard.count(_.role == Role.Lead), 1)
    assertEquals(
      pinRemoveGuard.map(_.story.tactic).toSet,
      Set[Option[Tactic]](Some(Tactic.Pin), Some(Tactic.RemoveGuard))
    )
    pinRemoveGuard
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("RemoveGuard-6 ExplanationPlan lowers selected uncapped RemoveGuard Lead only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.selected, true)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.RemoveGuard))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('c', 4)))
    assertEquals(plan.route, Some(removeGuardMove))
    assertEquals(plan.routeSan, BoardFacts.sanFor(facts, removeGuardMove))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(plan.evidenceLine, Some(removeGuardMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key), Vector("removes_defender"))
    assertEquals(
      ExplanationClaim.RemoveGuardForbiddenKeys,
      Vector(
        "wins_material",
        "target_is_hanging",
        "no_defense",
        "refutes_defense",
        "best_move",
        "only_move",
        "forced",
        "decisive",
        "creates_pressure",
        "takes_initiative"
      )
    )
    ExplanationClaim.RemoveGuardForbiddenKeys.foreach: forbiddenKey =>
      assert(!ExplanationClaim.RemoveGuardAllowed.map(_.key).contains(forbiddenKey), forbiddenKey)
      assert(plan.forbiddenWording.map(_.key).contains(forbiddenKey), forbiddenKey)
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.RemoveGuardForbiddenKeys.toSet),
      Set.empty
    )

  test(
    "RemoveGuard-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected RemoveGuard rows"
  ):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val reply = Line(Square('h', 8), Square('h', 7))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val verdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(removeGuardMove))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supportVerdict = verdict.copy(role = Role.Support, leadAllowed = false)
    val contextVerdict = verdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = verdict.copy(role = Role.Blocked, leadAllowed = false)
    val unselectedVerdict = verdict.copy(selected = false)
    val cappedVerdict = StoryTable
      .choose(Vector(TacticRemoveGuard.withEngineCheck(story, check(EngineCheckStatus.Caps)).get))
      .head
    val refutedVerdict =
      StoryTable
        .choose(
          Vector(
            TacticRemoveGuard
              .withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20))
              .get
          )
        )
        .head

    assertEquals(ExplanationPlan.fromSelected(supportVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(contextVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(blockedVerdict), None)
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(cappedVerdict), None)
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict), None)

  test("RemoveGuard-7 DeterministicRenderer phrases selected RemoveGuard ExplanationPlan only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenPhrases =
      Vector(
        "wins material",
        "leaves it undefended",
        "no defender remains",
        "best move",
        "only move",
        "forces",
        "decisive",
        "refutes the defense",
        "creates pressure"
      )

    assertEquals(rendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendered.claimKey, "removes_defender")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assert(rendererMethods.contains("fromPlan"))
    assert(!rendererMethods.contains("fromStory"))
    assert(!rendererMethods.contains("fromVerdict"))
    assert(!rendererMethods.contains("fromBoardFacts"))
    assert(!rendererMethods.contains("fromRemoveGuardProof"))

  test("RemoveGuard-7 DeterministicRenderer refuses RemoveGuard without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val leadPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    Vector(
      leadPlan.copy(role = Role.Support),
      leadPlan.copy(role = Role.Context),
      leadPlan.copy(role = Role.Blocked, debugOnly = true),
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.PinsPiece)),
      leadPlan.copy(tactic = Some(Tactic.Pin)),
      leadPlan.copy(scene = Scene.Material, tactic = None),
      leadPlan.copy(strength = ExplanationStrength.Bounded, evidenceLine = None),
      leadPlan.copy(route = None),
      leadPlan.copy(routeSan = None),
      leadPlan.copy(target = None),
      leadPlan.copy(secondaryTarget = Some(Square('e', 1))),
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.RemovesDefender)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("RemoveGuard-8 LLM smoke reuses 8B prompt contract for RemoveGuard RenderedLine only"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assert(prompt.contains("renderedText: Bxc4 removes the defender of the piece on e5."))
    assert(prompt.contains("claimKey: removes_defender"))
    assert(prompt.contains("strength: bounded"))
    assert(
      prompt.contains(
        "forbiddenWording: wins material, winning, decisive, best move, only move, forced, cannot move, no defense, front piece must move, wins rear piece, creates pressure, takes initiative, mate threat, king unsafe, target is hanging, leaves it undefended, no defender remains, refutes defense"
      )
    )
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector("raw Story", "RemoveGuardProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach:
      forbiddenInput => assert(!prompt.contains(forbiddenInput), forbiddenInput)

  test("RemoveGuard-8 LLM smoke rejects RemoveGuard additions and stronger claims"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val invalidOutputs =
      Vector(
        "Bxc4 wins material by removing the defender of e5." -> "forbidden_wording",
        "Bxc4 leaves it undefended." -> "forbidden_wording",
        "Bxc4 means no defense remains for e5." -> "forbidden_wording",
        "Bxc4 creates pressure on the piece on e5." -> "forbidden_wording",
        "Bxc4 takes the initiative by removing the defender." -> "forbidden_wording",
        "Bxc4 removes the defender of e5, then Qxe5 adds a new line." -> "new_move_or_line",
        "Bxc4 removes the defender and Bg2 adds another line." -> "new_move_or_line",
        "Engine says Bxc4 removes the defender." -> "engine_mention"
      )

    invalidOutputs.foreach: (output, expectedViolation) =>
      val checked = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(checked.accepted, false, output)
      assert(checked.violations.contains(expectedViolation), output)

    Vector(
      plan.copy(allowedClaim = Some(ExplanationClaim.PinsPiece)),
      plan.copy(tactic = Some(Tactic.Pin)),
      plan.copy(role = Role.Support),
      plan.copy(debugOnly = true)
    ).foreach: mismatchedPlan =>
      assertEquals(LlmNarrationSmoke.mockNarrate(mismatchedPlan, rendered), None, mismatchedPlan.toString)

    val mismatchedRendered = rendered.copy(claimKey = "wins_material")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None)
    assertEquals(
      LlmNarrationSmoke.check(plan, mismatchedRendered, rendered.text).violations.contains("input_mismatch"),
      true
    )

  test("RemoveGuard Closeout hard cleanup keeps authority homes separated and public surfaces closed"):
    val facts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val story =
      TacticRemoveGuard.write(facts, Some(removeGuardMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val removeGuardProof = story.removeGuardProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val guardSurface =
      classOf[BoardFacts.Guard].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.Guard].getDeclaredFields.map(_.getName).toSet
    val removeGuardProofSurface =
      classOf[RemoveGuardProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet
    val tacticRemoveGuardSurface =
      TacticRemoveGuard.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticRemoveGuard.getClass.getDeclaredFields.map(_.getName).toSet
    val rendererMethodInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(!guardSurface.exists(_.toLowerCase.contains(forbidden)), forbidden)
    Vector(
      "Story",
      "Verdict",
      "ExplanationPlan",
      "RenderedLine",
      "Hanging",
      "Defense",
      "Pin",
      "DiscoveredAttack"
    ).foreach: forbidden =>
      assert(!removeGuardProofSurface.exists(_.contains(forbidden)), forbidden)
    Vector("test", "fixture", "helper", "mock", "fromEngineEvidence", "fromBoardFacts").foreach: forbidden =>
      assert(!tacticRemoveGuardSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    Vector("deflection", "overload", "overloaded", "noDefender", "winsMaterial", "materialGain").foreach:
      forbidden =>
        assert(!removeGuardProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    assertEquals(removeGuardProof.publicClaimAllowed, false)
    assertEquals(removeGuardProof.complete, true)
    assertEquals(story.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(story.tactic, Some(Tactic.RemoveGuard))
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.removeGuardProof.nonEmpty, true)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(ExplanationClaim.RemoveGuardAllowed.map(_.key), Vector("removes_defender"))
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.MaterialAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.HangingAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.DefenseAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.PinAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(
      ExplanationClaim.RemoveGuardAllowed
        .map(_.key)
        .toSet
        .intersect(ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet),
      Set.empty
    )
    assertEquals(rendered.claimKey, "removes_defender")
    assertEquals(rendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendererMethodInputs, Vector("ExplanationPlan"))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
    Vector(
      "Bxc4 wins material by removing the defender.",
      "Bxc4 leaves it undefended.",
      "Bxc4 means no defender remains.",
      "Bxc4 is a deflection tactic.",
      "Bxc4 overloads the defender.",
      "Bxc4 creates pressure on e5.",
      "Bxc4 takes the initiative."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector("RemoveGuardProof", "BoardFacts", "EngineCheck", "raw PV", "proofFailures").foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)
    Vector(
      Tactic.Hanging,
      Tactic.Fork,
      Tactic.DiscoveredAttack,
      Tactic.Pin,
      Tactic.Skewer,
      Tactic.Overload,
      Tactic.Deflect
    ).foreach: tactic =>
      val forged = story.copy(tactic = Some(tactic))
      val forgedVerdict = StoryTable.choose(Vector(forged)).head
      assertEquals(forgedVerdict.role, Role.Blocked, tactic.toString)
      ExplanationPlan
        .fromSelected(forgedVerdict)
        .foreach: forgedPlan =>
          assertEquals(forgedPlan.allowedClaim, None, tactic.toString)
          assertEquals(forgedPlan.debugOnly, true, tactic.toString)

  test("LDH-0 keeps same-board RemoveGuard from being stolen by DiscoveredAttack"):
    val facts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val move = Line(Square('d', 3), Square('e', 5))
    val discovered =
      TacticDiscoveredAttack.write(facts, Some(move), Some(Square('b', 1)), Some(Square('g', 6))).get
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(move), Some(Square('g', 6)), Some(Square('e', 5))).get
    val high =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val tied = Vector(discovered.copy(proof = high), removeGuard.copy(proof = high))
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.tactic, verdict.story.target, verdict.story.route, verdict.role))

    val removeGuardVerdict = forward.find(_.story.tactic.contains(Tactic.RemoveGuard)).get
    val discoveredVerdict = forward.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).get

    assertEquals(shape(forward), shape(reverse))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(forward.map(_.story.route).distinct, Vector(Some(move)))
    assertEquals(removeGuardVerdict.role, Role.Lead)
    assertEquals(discoveredVerdict.role == Role.Lead, false)
    assertEquals(
      ExplanationPlan.fromSelected(removeGuardVerdict).flatMap(_.allowedClaim).map(_.key),
      Some("removes_defender")
    )
    assertEquals(
      ExplanationPlan
        .fromSelected(removeGuardVerdict)
        .flatMap(DeterministicRenderer.fromPlan)
        .map(_.claimKey),
      Some("removes_defender")
    )
    assertEquals(ExplanationPlan.fromSelected(discoveredVerdict), None)

  test("LDH-0 complex same-board fixture keeps row roles stable and downstream smoke bounded"):
    val facts = BoardFacts.fromFen("7k/8/6r1/4n3/2q5/3N4/8/1B5K w - - 0 1").toOption.get
    val move = Line(Square('d', 3), Square('e', 5))
    val hanging = TacticHanging.write(facts, move).get
    val material = SceneMaterial.write(facts, move).get
    val fork = TacticFork.write(facts, Some(move), Some(Square('c', 4)), Some(Square('g', 6))).get
    val discovered =
      TacticDiscoveredAttack.write(facts, Some(move), Some(Square('b', 1)), Some(Square('g', 6))).get
    val removeGuard =
      TacticRemoveGuard.write(facts, Some(move), Some(Square('g', 6)), Some(Square('e', 5))).get
    val high =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )
    val tied =
      Vector(
        hanging.copy(proof = high),
        material.copy(proof = high),
        fork.copy(proof = high),
        discovered.copy(proof = high),
        removeGuard.copy(proof = high)
      )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(3), tied(1), tied(4), tied(0), tied(2)))

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.target, verdict.role))

    val lead = forward.find(_.role == Role.Lead).get
    val leadPlan = ExplanationPlan.fromSelected(lead).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get

    assertEquals(shape(forward), shape(reverse))
    assertEquals(shape(forward), shape(shuffled))
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(lead.story.tactic, Some(Tactic.Hanging))
    assertEquals(leadPlan.allowedClaim.map(_.key), Some("can_win_piece"))
    assertEquals(rendered.claimKey, "can_win_piece")
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(
          ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          verdict.toString
        )

    Vector(
      "Nxe5 reveals an attack.",
      "Nxe5 pins the piece.",
      "Nxe5 removes the defender.",
      "Nxe5 creates pressure.",
      "Nxe5 threatens mate.",
      "Nxe5 is the best move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(leadPlan, rendered, output).accepted, false, output)

  test("LDH-1 fixture map covers complex same-board line defender interactions"):
    final case class LdhFixture(
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

    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "wins_material" => s"$san wins material."
        case "best_move" => s"$san is the best move."
        case "creates_pressure" => s"$san creates pressure."
        case "takes_initiative" => s"$san takes the initiative."
        case "mate_threat" => s"$san threatens mate."
        case "king_unsafe" => s"$san makes the king unsafe."
        case "skewers_piece" => s"$san skewers the piece."
        case "xray" => s"$san is an XRay tactic."
        case "reveals_attack_on_piece" => s"$san reveals an attack."
        case "pins_piece" => s"$san pins the piece."
        case "removes_defender" => s"$san removes the defender."
        case "refutes_defense" => s"$san refutes the defense."
        case "no_defense" => s"$san leaves no defense."
        case other => s"$san adds $other."

    def assertFixture(fixture: LdhFixture): Unit =
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
        verdicts.map(verdict => (rowId(fixture.rows, verdict.story), verdict.role, verdict.engineCheckStatus))

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

    val discoveredPinFen = "6nk/8/6r1/r6q/5N2/3N4/8/1BR1K3 w - - 0 1"
    val discoveredPinMove = Line(Square('d', 3), Square('e', 5))
    val discoveredPinPinMove = Line(Square('c', 1), Square('c', 8))
    val discoveredPinFacts = BoardFacts.fromFen(discoveredPinFen).toOption.get
    val discoveredPinDiscovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredPinFacts, Some(discoveredPinMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get
      )
    val discoveredPinPin =
      strong(
        TacticPin
          .write(
            discoveredPinFacts,
            Some(discoveredPinPinMove),
            Some(Square('c', 8)),
            Some(Square('g', 8)),
            Some(Square('h', 8))
          )
          .get
      )

    val discoveredRemoveFen = "7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val discoveredRemoveMove = Line(Square('d', 3), Square('e', 5))
    val discoveredRemoveFacts = BoardFacts.fromFen(discoveredRemoveFen).toOption.get
    val discoveredRemoveDiscovered =
      strong(
        TacticDiscoveredAttack
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveMove),
            Some(Square('b', 1)),
            Some(Square('g', 6))
          )
          .get
      )
    val discoveredRemoveRemove =
      strong(
        TacticRemoveGuard
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveMove),
            Some(Square('g', 6)),
            Some(Square('e', 5))
          )
          .get
      )
    val discoveredRemoveMaterial =
      strong(SceneMaterial.write(discoveredRemoveFacts, discoveredRemoveMove).get)
    val discoveredRemoveHanging = strong(TacticHanging.write(discoveredRemoveFacts, discoveredRemoveMove).get)

    val pinRemoveFen = "8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val pinRemoveMove = Line(Square('d', 3), Square('e', 5))
    val pinRemoveFacts = BoardFacts.fromFen(pinRemoveFen).toOption.get
    val pinRemovePin =
      strong(
        TacticPin
          .write(
            pinRemoveFacts,
            Some(pinRemoveMove),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get
      )
    val pinRemoveRemove =
      strong(
        TacticRemoveGuard
          .write(pinRemoveFacts, Some(pinRemoveMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get
      )

    val allThreeFen = "6nk/8/6r1/r6q/5N2/3N4/8/1BR1K3 w - - 0 1"
    val allThreeFacts = BoardFacts.fromFen(allThreeFen).toOption.get
    val allThreeDiscoveredMove = Line(Square('d', 3), Square('e', 5))
    val allThreePinMove = Line(Square('c', 1), Square('c', 8))
    val allThreeRemoveMove = Line(Square('f', 4), Square('d', 5))
    val allThreeDiscovered =
      strong(
        TacticDiscoveredAttack
          .write(allThreeFacts, Some(allThreeDiscoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get
      )
    val allThreePin =
      strong(
        TacticPin
          .write(
            allThreeFacts,
            Some(allThreePinMove),
            Some(Square('c', 8)),
            Some(Square('g', 8)),
            Some(Square('h', 8))
          )
          .get
      )
    val allThreeRemove =
      strong(
        TacticRemoveGuard
          .write(allThreeFacts, Some(allThreeRemoveMove), Some(Square('h', 5)), Some(Square('a', 5)))
          .get
      )
    val allThreeSkewerBlocked = allThreePin.copy(tactic = Some(Tactic.Skewer), writer = None)

    val defenseFen = "4k1B1/8/8/4rn2/2nQ4/8/8/7K w - - 0 1"
    val defenseFacts = BoardFacts.fromFen(defenseFen).toOption.get
    val defenseRemoveMove = Line(Square('g', 8), Square('c', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseRemove =
      strong(
        TacticRemoveGuard
          .write(defenseFacts, Some(defenseRemoveMove), Some(Square('e', 5)), Some(Square('c', 4)))
          .get
      )
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get)

    Vector(
      LdhFixture(
        category = "DiscoveredAttack vs Pin",
        fen = discoveredPinFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredPinMove, discoveredPinPinMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> discoveredPinDiscovered,
          "Tactic.Pin" -> discoveredPinPin
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.Pin"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Tactic.DiscoveredAttack" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "removes_defender",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat",
          "skewers_piece"
        )
      ),
      LdhFixture(
        category = "DiscoveredAttack vs RemoveGuard",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> discoveredRemoveDiscovered,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.RemoveGuard" -> Role.Lead, "Tactic.DiscoveredAttack" -> Role.Support),
        expectedSelectedVerdict = "Tactic.RemoveGuard",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "pins_piece",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      LdhFixture(
        category = "Pin vs RemoveGuard",
        fen = pinRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(pinRemoveMove),
        rows = Vector(
          "Tactic.Pin" -> pinRemovePin,
          "Tactic.RemoveGuard" -> pinRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.Pin", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Pin" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "removes_defender",
          "king_unsafe",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      LdhFixture(
        category = "DiscoveredAttack + Pin + RemoveGuard same-board",
        fen = allThreeFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(allThreeDiscoveredMove, allThreePinMove, allThreeRemoveMove),
        rows = Vector(
          "Tactic.DiscoveredAttack" -> allThreeDiscovered,
          "Tactic.Pin" -> allThreePin,
          "Tactic.RemoveGuard" -> allThreeRemove,
          "Tactic.Skewer/blocked" -> allThreeSkewerBlocked
        ),
        expectedOpenRows = Set("Tactic.DiscoveredAttack", "Tactic.Pin", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set("Tactic.Skewer/blocked"),
        expectedRoles = Map(
          "Tactic.Pin" -> Role.Lead,
          "Tactic.DiscoveredAttack" -> Role.Support,
          "Tactic.RemoveGuard" -> Role.Support,
          "Tactic.Skewer/blocked" -> Role.Blocked
        ),
        expectedSelectedVerdict = "Tactic.Pin",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "removes_defender",
          "skewers_piece",
          "xray",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      LdhFixture(
        category = "Line/Defender row vs Material",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Scene.Material" -> discoveredRemoveMaterial,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Scene.Material", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Material" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Scene.Material",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "removes_defender",
          "pins_piece",
          "reveals_attack_on_piece",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      LdhFixture(
        category = "Line/Defender row vs Hanging",
        fen = discoveredRemoveFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredRemoveMove),
        rows = Vector(
          "Tactic.Hanging" -> discoveredRemoveHanging,
          "Tactic.RemoveGuard" -> discoveredRemoveRemove
        ),
        expectedOpenRows = Set("Tactic.Hanging", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Tactic.Hanging" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Tactic.Hanging",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "removes_defender",
          "pins_piece",
          "reveals_attack_on_piece",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      LdhFixture(
        category = "Line/Defender row vs Defense",
        fen = defenseFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(defenseRemoveMove, defenseMove),
        rows = Vector(
          "Scene.Defense" -> defense,
          "Tactic.RemoveGuard" -> defenseRemove
        ),
        expectedOpenRows = Set("Scene.Defense", "Tactic.RemoveGuard"),
        expectedBlockedRows = Set.empty,
        expectedRoles = Map("Scene.Defense" -> Role.Lead, "Tactic.RemoveGuard" -> Role.Support),
        expectedSelectedVerdict = "Scene.Defense",
        expectedSelectedRole = Role.Lead,
        forbiddenClaims = Vector(
          "wins_material",
          "refutes_defense",
          "no_defense",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      )
    ).foreach(assertFixture)

  test("LDH-1 fixture map covers EngineCheck statuses over existing line defender rows"):
    final case class EngineLdhFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        candidateLegalLines: Vector[Line],
        baseRowId: String,
        baseStory: Story,
        attach: (Story, EngineCheck) => Option[Story],
        expectedSupportsRole: Role,
        expectedCapsRole: Role,
        expectedClaim: Option[ExplanationClaim],
        forbiddenClaims: Vector[String]
    )

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
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def forbiddenOutput(claim: String, plan: ExplanationPlan): String =
      val san = plan.routeSan.getOrElse("Move")
      claim match
        case "wins_material" => s"$san wins material."
        case "best_move" => s"$san is the best move."
        case "creates_pressure" => s"$san creates pressure."
        case "takes_initiative" => s"$san takes the initiative."
        case "mate_threat" => s"$san threatens mate."
        case "reveals_attack_on_piece" => s"$san reveals an attack."
        case "pins_piece" => s"$san pins the piece."
        case "removes_defender" => s"$san removes the defender."
        case "wins_rear_piece" => s"$san wins the rear piece."
        case "front_piece_must_move" => s"$san forces the front piece to move."
        case other => s"$san adds $other."

    val discoveredFen = "7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1"
    val discoveredFacts = BoardFacts.fromFen(discoveredFen).toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pinFen = "r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1"
    val pinFacts = BoardFacts.fromFen(pinFen).toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin
      .write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
      .get
    val removeFen = "6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1"
    val removeFacts = BoardFacts.fromFen(removeFen).toOption.get
    val removeMove = Line(Square('g', 8), Square('c', 4))
    val remove =
      TacticRemoveGuard.write(removeFacts, Some(removeMove), Some(Square('e', 5)), Some(Square('c', 4))).get
    val skewerFen = "4r2k/8/8/4q3/8/8/8/R6K w - - 0 1"
    val skewerFacts = BoardFacts.fromFen(skewerFen).toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      TacticSkewer
        .write(
          skewerFacts,
          Some(skewerMove),
          Some(Square('e', 1)),
          Some(Square('e', 5)),
          Some(Square('e', 8))
        )
        .get

    val fixtures = Vector(
      EngineLdhFixture(
        category = "EngineCheck over Tactic.DiscoveredAttack",
        fen = discoveredFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(discoveredMove),
        baseRowId = "Tactic.DiscoveredAttack",
        baseStory = discovered,
        attach = TacticDiscoveredAttack.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.RevealsAttackOnPiece),
        forbiddenClaims = Vector(
          "wins_material",
          "pins_piece",
          "removes_defender",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.Pin",
        fen = pinFen,
        sideToMove = Side.Black,
        candidateLegalLines = Vector(pinMove),
        baseRowId = "Tactic.Pin",
        baseStory = pin,
        attach = TacticPin.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.PinsPiece),
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "removes_defender",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.RemoveGuard",
        fen = removeFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(removeMove),
        baseRowId = "Tactic.RemoveGuard",
        baseStory = remove,
        attach = TacticRemoveGuard.withEngineCheck,
        expectedSupportsRole = Role.Lead,
        expectedCapsRole = Role.Lead,
        expectedClaim = Some(ExplanationClaim.RemovesDefender),
        forbiddenClaims = Vector(
          "wins_material",
          "reveals_attack_on_piece",
          "pins_piece",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      ),
      EngineLdhFixture(
        category = "EngineCheck over Tactic.Skewer",
        fen = skewerFen,
        sideToMove = Side.White,
        candidateLegalLines = Vector(skewerMove),
        baseRowId = "Tactic.Skewer",
        baseStory = skewer,
        attach = TacticSkewer.withEngineCheck,
        expectedSupportsRole = Role.Context,
        expectedCapsRole = Role.Context,
        expectedClaim = None,
        forbiddenClaims = Vector(
          "wins_material",
          "wins_rear_piece",
          "front_piece_must_move",
          "best_move",
          "creates_pressure",
          "takes_initiative",
          "mate_threat"
        )
      )
    )

    fixtures.foreach: fixture =>
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.candidateLegalLines.foreach: line =>
        assert(facts.sideLegal.lines.contains(line), s"${fixture.category} legal line missing: $line")
      val line = fixture.candidateLegalLines.head
      val supports =
        fixture
          .attach(fixture.baseStory, engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Supports))
          .get
      val caps =
        fixture
          .attach(fixture.baseStory, engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Caps))
          .get
      val refutes =
        fixture
          .attach(
            fixture.baseStory,
            engineCheck(facts, fixture.baseStory, line, EngineCheckStatus.Supports, before = 220, after = 20)
          )
          .get

      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head

      assertEquals(supportsVerdict.role, fixture.expectedSupportsRole, fixture.category)
      assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), fixture.category)
      fixture.expectedClaim match
        case Some(expectedClaim) =>
          val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
          val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
          assertEquals(supportsPlan.allowedClaim, Some(expectedClaim), fixture.category)
          fixture.forbiddenClaims.foreach: claim =>
            assertEquals(
              LlmNarrationSmoke
                .check(supportsPlan, supportsRendered, forbiddenOutput(claim, supportsPlan))
                .accepted,
              false,
              s"${fixture.category}: $claim"
            )
        case None =>
          assertEquals(ExplanationPlan.fromSelected(supportsVerdict), None, fixture.category)

      assertEquals(capsVerdict.role, fixture.expectedCapsRole, fixture.category)
      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.category)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.category)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.category)

      assertEquals(refutesVerdict.role, Role.Blocked, fixture.category)
      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.category)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.category)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.category)

  test("LDH-2 Role Stability keeps selected Verdict stable across input order"):
    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

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
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredRemoveFen = "7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val discoveredRemoveFacts = BoardFacts.fromFen(discoveredRemoveFen).toOption.get
    val discoveredRemoveMove = Line(Square('d', 3), Square('e', 5))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveMove),
            Some(Square('b', 1)),
            Some(Square('g', 6))
          )
          .get
      )
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveMove),
            Some(Square('g', 6)),
            Some(Square('e', 5))
          )
          .get
      )

    val pinFen = "8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1"
    val pinFacts = BoardFacts.fromFen(pinFen).toOption.get
    val pinMove = Line(Square('d', 3), Square('e', 5))
    val pin =
      strong(
        TacticPin
          .write(pinFacts, Some(pinMove), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
          .get
      )
    val incompletePin = pin.copy(pinProof = None)
    val cappedPin =
      TacticPin.withEngineCheck(pin, engineCheck(pinFacts, pin, pinMove, EngineCheckStatus.Caps)).get
    val refutedRemoveGuard =
      TacticRemoveGuard
        .withEngineCheck(
          removeGuard,
          engineCheck(
            discoveredRemoveFacts,
            removeGuard,
            discoveredRemoveMove,
            EngineCheckStatus.Supports,
            before = 220,
            after = 20
          )
        )
        .get

    val rows = Vector(
      "Tactic.DiscoveredAttack" -> discovered,
      "Tactic.Pin" -> pin,
      "Tactic.RemoveGuard" -> removeGuard,
      "Tactic.Pin/incomplete" -> incompletePin,
      "Tactic.Pin/capped" -> cappedPin,
      "Tactic.RemoveGuard/refuted" -> refutedRemoveGuard
    )
    val permutations =
      Vector(
        rows,
        rows.reverse,
        Vector(rows(3), rows(0), rows(5), rows(2), rows(4), rows(1))
      )

    def rowId(story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.getOrElse(s"unknown:$story")

    def shape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict =>
        (rowId(verdict.story), verdict.role, verdict.engineCheckStatus, verdict.engineStrengthLimited)
      )

    val selectedShapes = permutations.map(input => shape(StoryTable.choose(input.map(_._2))))
    val baseline = selectedShapes.head
    val baselineVerdicts = StoryTable.choose(rows.map(_._2))
    val selected = baselineVerdicts.head

    selectedShapes.foreach: current =>
      assertEquals(current, baseline)
    assertEquals(rowId(selected.story), "Tactic.Pin")
    assertEquals(selected.role, Role.Lead)
    assertEquals(baselineVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(baselineVerdicts.find(_.story == incompletePin).map(_.role), Some(Role.Blocked))
    assertEquals(baselineVerdicts.find(_.story == refutedRemoveGuard).map(_.role), Some(Role.Blocked))
    assertEquals(baselineVerdicts.find(_.story == cappedPin).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(baselineVerdicts.find(_.story == cappedPin).get), None)

  test("LDH-2 Role Stability keeps line and defender meanings from duplicate Lead ownership"):
    def score(value: Int): Proof =
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
        pawnSupport = 0,
        clarity = value
      )

    def strong(story: Story): Story = story.copy(proof = score(99))

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val sharedLine = Line(Square('d', 3), Square('f', 4))
    val discoveredSameLine =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(sharedLine), Some(Square('b', 1)), Some(Square('g', 6)))
          .get
      )
    val pinSameLine =
      strong(
        TacticPin
          .write(pinFacts, Some(sharedLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
          .get
      )
    val sameLine = StoryTable.choose(Vector(discoveredSameLine, pinSameLine))

    assertEquals(sameLine.count(_.role == Role.Lead), 1)
    assertEquals(sameLine.map(_.story.route).distinct, Vector(Some(sharedLine)))
    assertEquals(sameLine.find(_.story.tactic.contains(Tactic.Pin)).map(_.role), Some(Role.Lead))
    assertEquals(
      sameLine.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).exists(_.role == Role.Lead),
      false
    )
    sameLine
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict), None, verdict.toString)

    val pinRemoveFacts = BoardFacts.fromFen("8/7k/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinRemoveLine = Line(Square('d', 3), Square('e', 5))
    val pin =
      strong(
        TacticPin
          .write(
            pinRemoveFacts,
            Some(pinRemoveLine),
            Some(Square('b', 1)),
            Some(Square('g', 6)),
            Some(Square('h', 7))
          )
          .get
      )
    val removeGuardOnPinLine =
      strong(
        TacticRemoveGuard
          .write(pinRemoveFacts, Some(pinRemoveLine), Some(Square('g', 6)), Some(Square('e', 5)))
          .get
      )
    val pinRemove = StoryTable.choose(Vector(removeGuardOnPinLine, pin))
    val pinLead = pinRemove.find(_.story.tactic.contains(Tactic.Pin)).get
    val removeGuardSupport = pinRemove.find(_.story.tactic.contains(Tactic.RemoveGuard)).get

    assertEquals(pinRemove.count(_.role == Role.Lead), 1)
    assertEquals(pinLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(pinLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.PinsPiece)
    )
    assertEquals(removeGuardSupport.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(removeGuardSupport), None)

    val discoveredRemoveFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredRemoveLine = Line(Square('d', 3), Square('e', 5))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveLine),
            Some(Square('b', 1)),
            Some(Square('g', 6))
          )
          .get
      )
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(
            discoveredRemoveFacts,
            Some(discoveredRemoveLine),
            Some(Square('g', 6)),
            Some(Square('e', 5))
          )
          .get
      )
    val discoveredRemove = StoryTable.choose(Vector(discovered, removeGuard))
    val discoveredSupport = discoveredRemove.find(_.story.tactic.contains(Tactic.DiscoveredAttack)).get
    val removeGuardLead = discoveredRemove.find(_.story.tactic.contains(Tactic.RemoveGuard)).get

    assertEquals(discoveredRemove.count(_.role == Role.Lead), 1)
    assertEquals(removeGuardLead.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(removeGuardLead).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RemovesDefender)
    )
    assertEquals(discoveredSupport.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(discoveredSupport), None)

    val duplicatePin = StoryTable.choose(Vector(pin, pin.copy(anchor = pin.anchor)))
    assertEquals(duplicatePin.count(_.role == Role.Lead), 1)

  test("LDH-3 Meaning Ownership Boundary keeps each open row on its own claim key"):
    def leadPlan(story: Story): (Verdict, ExplanationPlan, RenderedLine) =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      (verdict, plan, rendered)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val (_, discoveredPlan, discoveredRendered) =
      leadPlan(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)))
          .get
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val (_, pinPlan, pinRendered) =
      leadPlan(
        TacticPin
          .write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
          .get
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val (_, removeGuardPlan, removeGuardRendered) =
      leadPlan(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
          .get
      )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val (_, materialPlan, materialRendered) =
      leadPlan(SceneMaterial.write(materialFacts, materialLine).get)
    val (_, hangingPlan, hangingRendered) =
      leadPlan(TacticHanging.write(materialFacts, materialLine).get)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val (_, defensePlan, defenseRendered) =
      leadPlan(SceneDefense.write(defenseFacts, threatLine, defenseLine).get)

    assertEquals(discoveredPlan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(pinPlan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(removeGuardPlan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(materialPlan.allowedClaim.map(_.key), Some("material_balance_changes"))
    assertEquals(hangingPlan.allowedClaim.map(_.key), Some("can_win_piece"))
    assertEquals(defensePlan.allowedClaim.map(_.key), Some("defends_piece"))

    assertEquals(discoveredPlan.scene, Scene.Tactic)
    assertEquals(discoveredPlan.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(pinPlan.scene, Scene.Tactic)
    assertEquals(pinPlan.tactic, Some(Tactic.Pin))
    assertEquals(removeGuardPlan.scene, Scene.Tactic)
    assertEquals(removeGuardPlan.tactic, Some(Tactic.RemoveGuard))
    assertEquals(materialPlan.scene, Scene.Material)
    assertEquals(materialPlan.tactic, None)
    assertEquals(hangingPlan.scene, Scene.Tactic)
    assertEquals(hangingPlan.tactic, Some(Tactic.Hanging))
    assertEquals(defensePlan.scene, Scene.Defense)
    assertEquals(defensePlan.tactic, None)

    assert(hangingRendered.text.toLowerCase.contains("wins material"))
    assert(!discoveredRendered.text.toLowerCase.contains("wins material"))
    assert(!pinRendered.text.toLowerCase.contains("cannot move"))
    assert(!pinRendered.text.toLowerCase.contains("king unsafe"))
    assert(!removeGuardRendered.text.toLowerCase.contains("material"))
    assert(!materialRendered.text.toLowerCase.contains("pin"))
    assert(!materialRendered.text.toLowerCase.contains("discovered"))
    assert(!defenseRendered.text.toLowerCase.contains("best"))

  test("LDH-3 Meaning Ownership Boundary rejects cross-meaning public wording"):
    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard = TacticRemoveGuard
      .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
      .get
    val removeGuardPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(removeGuard)).head).get
    val removeGuardRendered = DeterministicRenderer.fromPlan(removeGuardPlan).get
    val removeGuardMaterialGain =
      LlmNarrationSmoke.check(
        removeGuardPlan,
        removeGuardRendered,
        s"${removeGuardPlan.routeSan.get} gains material."
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val pin = TacticPin
      .write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
      .get
    val pinPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(pin)).head).get
    val pinRendered = DeterministicRenderer.fromPlan(pinPlan).get
    val pinKingUnsafe =
      LlmNarrationSmoke.check(
        pinPlan,
        pinRendered,
        s"${pinPlan.routeSan.get} pins the piece on g6 and the king is unsafe."
      )
    val pinCannotMove =
      LlmNarrationSmoke.check(
        pinPlan,
        pinRendered,
        s"${pinPlan.routeSan.get} means the piece on g6 cannot move."
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val discoveredPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(discovered)).head).get
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    val discoveredMaterialGain =
      LlmNarrationSmoke.check(
        discoveredPlan,
        discoveredRendered,
        s"${discoveredPlan.routeSan.get} gains material."
      )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialLine = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialLine).get
    val materialPlan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(material)).head).get
    val materialRendered = DeterministicRenderer.fromPlan(materialPlan).get
    val materialLineTactic =
      LlmNarrationSmoke.check(
        materialPlan,
        materialRendered,
        s"After ${materialPlan.routeSan.get}, this line tactic leaves White ahead in material."
      )

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, threatLine, defenseLine).get
    val incompleteDefense = defense.copy(threatProof = None)
    val incompleteDefenseVerdict = StoryTable.choose(Vector(incompleteDefense)).head

    assertEquals(removeGuardPlan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(removeGuardMaterialGain.accepted, false)
    assert(removeGuardMaterialGain.violations.contains("forbidden_wording"), removeGuardMaterialGain.toString)
    assertEquals(pinKingUnsafe.accepted, false)
    assert(pinKingUnsafe.violations.contains("forbidden_wording"), pinKingUnsafe.toString)
    assertEquals(pinCannotMove.accepted, false)
    assert(pinCannotMove.violations.contains("forbidden_wording"), pinCannotMove.toString)
    assertEquals(discoveredMaterialGain.accepted, false)
    assert(discoveredMaterialGain.violations.contains("forbidden_wording"), discoveredMaterialGain.toString)
    assert(materialPlan.forbiddenWording.map(_.key).contains("line_tactic_identity"))
    assertEquals(materialLineTactic.accepted, false)
    assert(materialLineTactic.violations.contains("forbidden_wording"), materialLineTactic.toString)
    assertEquals(incompleteDefenseVerdict.role, Role.Blocked)
    assertEquals(incompleteDefenseVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(incompleteDefenseVerdict).flatMap(_.allowedClaim), None)
    assertEquals(
      ExplanationPlan.fromSelected(incompleteDefenseVerdict).flatMap(DeterministicRenderer.fromPlan),
      None
    )

  test("LDH-4 EngineCheck Interaction reuses existing statuses without engine-owned public claims"):
    final case class EngineOwnershipFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

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
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLine = Line(Square('d', 3), Square('f', 4))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get

    Vector(
      EngineOwnershipFixture(
        "Tactic.DiscoveredAttack",
        discoveredFacts,
        discovered,
        discoveredLine,
        TacticDiscoveredAttack.withEngineCheck,
        ExplanationClaim.RevealsAttackOnPiece
      ),
      EngineOwnershipFixture(
        "Tactic.Pin",
        pinFacts,
        pin,
        pinLine,
        TacticPin.withEngineCheck,
        ExplanationClaim.PinsPiece
      ),
      EngineOwnershipFixture(
        "Tactic.RemoveGuard",
        removeGuardFacts,
        removeGuard,
        removeGuardLine,
        TacticRemoveGuard.withEngineCheck,
        ExplanationClaim.RemovesDefender
      )
    ).foreach: fixture =>
      val baseVerdict = StoryTable.choose(Vector(fixture.story)).head
      val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
      val baseRendered = DeterministicRenderer.fromPlan(basePlan).get
      val supports = fixture
        .attach(
          fixture.story,
          engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports)
        )
        .get
      val caps = fixture
        .attach(
          fixture.story,
          engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)
        )
        .get
      val refutes =
        fixture
          .attach(
            fixture.story,
            engineCheck(
              fixture.facts,
              fixture.story,
              fixture.line,
              EngineCheckStatus.Supports,
              before = 220,
              after = 20
            )
          )
          .get
      val unknown = fixture
        .attach(
          fixture.story,
          engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Unknown)
        )
        .get
      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head
      val unknownVerdict = StoryTable.choose(Vector(unknown)).head
      val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
      val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
      val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
      val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get

      assertEquals(basePlan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports), fixture.label)
      assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(supportsRendered.claimKey, baseRendered.claimKey, fixture.label)
      assertEquals(supportsRendered.text, baseRendered.text, fixture.label)

      assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps), fixture.label)
      assertEquals(capsVerdict.engineStrengthLimited, true, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(capsVerdict), None, fixture.label)
      assertEquals(
        ExplanationPlan.fromSelected(capsVerdict).flatMap(DeterministicRenderer.fromPlan),
        None,
        fixture.label
      )

      assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes), fixture.label)
      assertEquals(refutesVerdict.role, Role.Blocked, fixture.label)
      assertEquals(refutesVerdict.leadAllowed, false, fixture.label)
      assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None, fixture.label)

      assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown), fixture.label)
      assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim, fixture.label)
      assertEquals(unknownRendered.text, baseRendered.text, fixture.label)

      Vector(supportsPlan -> supportsRendered, unknownPlan -> unknownRendered).foreach: (plan, rendered) =>
        Vector(
          s"${plan.routeSan.get} is supported because the engine says so." -> "engine_mention",
          s"${plan.routeSan.get} follows the principal variation." -> "engine_mention",
          s"${plan.routeSan.get} is +0.80." -> "engine_mention",
          s"${plan.routeSan.get} is the best move." -> "forbidden_wording",
          s"${plan.routeSan.get} is the only move." -> "forbidden_wording",
          s"${plan.routeSan.get} starts a forced line." -> "forbidden_wording"
        ).foreach: (output, violation) =>
          val checked = LlmNarrationSmoke.check(plan, rendered, output)
          assertEquals(checked.accepted, false, s"${fixture.label}: $output")
          assert(
            checked.violations.contains(violation),
            s"${fixture.label}: $output should include $violation, got ${checked.violations}"
          )

      Vector(
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} engine says")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} principal variation")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} +0.80")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} best move")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} only move")),
        basePlan.copy(routeSan = Some(s"${basePlan.routeSan.get} forced line"))
      ).foreach: plan =>
        assertEquals(DeterministicRenderer.fromPlan(plan), None, s"${fixture.label}: ${plan.routeSan}")

  test("LDH-5 Negative Corpus keeps close line defender false positives silent"):
    def assertNoPublicOutput(story: Story, label: String): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus = EngineCheckStatus.Supports,
        before: Int = 20,
        after: Int = 20,
        freshness: Option[Int] = Some(0)
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val captureDefenderLine = Line(Square('d', 3), Square('e', 5))

    val lineOpensNoAttackFacts = BoardFacts.fromFen("7k/8/7r/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val lineOpensNoAttack =
      LineProof.fromBoardFacts(
        lineOpensNoAttackFacts,
        Some(revealLine),
        Some(Square('b', 1)),
        Some(Square('h', 6))
      )
    assertEquals(lineOpensNoAttack.complete, false)
    assertEquals(lineOpensNoAttack.afterSliderAttacksTarget, false)
    assertEquals(
      TacticDiscoveredAttack
        .write(lineOpensNoAttackFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('h', 6))),
      None
    )

    val kingTargetFacts = BoardFacts.fromFen("8/8/6k1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val kingTarget =
      LineProof.fromBoardFacts(kingTargetFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
    assertEquals(kingTarget.complete, false)
    assertEquals(kingTarget.afterSliderAttacksTarget, true)
    assertEquals(kingTarget.targetNonKingMaterial, false)
    assertEquals(
      TacticDiscoveredAttack
        .write(kingTargetFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))),
      None
    )

    val pinLookingNoKingFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pinLookingNoKing =
      PinProof.fromBoardFacts(
        pinLookingNoKingFacts,
        Some(revealLine),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      )
    assertEquals(pinLookingNoKing.complete, false)
    assertEquals(pinLookingNoKing.kingBehindTarget, None)
    assertEquals(
      TacticPin.write(
        pinLookingNoKingFacts,
        Some(revealLine),
        Some(Square('b', 1)),
        Some(Square('g', 6)),
        Some(Square('h', 7))
      ),
      None
    )

    val stillGuardedFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val stillGuarded =
      RemoveGuardProof.fromBoardFacts(
        stillGuardedFacts,
        Some(revealLine),
        Some(Square('g', 6)),
        Some(Square('e', 5))
      )
    assertEquals(stillGuarded.complete, false)
    assertEquals(stillGuarded.defenderGuardedTargetBeforeMove, true)
    assertEquals(stillGuarded.afterMoveDefenderNoLongerGuardsTarget, false)
    assertEquals(
      TacticRemoveGuard
        .write(stillGuardedFacts, Some(revealLine), Some(Square('g', 6)), Some(Square('e', 5))),
      None
    )

    val defenderRemovedNoMaterialFacts =
      BoardFacts.fromFen("7k/4q3/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardOnly =
      TacticRemoveGuard
        .write(
          defenderRemovedNoMaterialFacts,
          Some(captureDefenderLine),
          Some(Square('g', 6)),
          Some(Square('e', 5))
        )
        .get
    val defenderCaptureResult =
      CaptureResult.fromBoardFacts(defenderRemovedNoMaterialFacts, captureDefenderLine)
    assertEquals(defenderCaptureResult.positiveMaterial, false)
    assertEquals(SceneMaterial.write(defenderRemovedNoMaterialFacts, captureDefenderLine), None)
    assertEquals(TacticHanging.write(defenderRemovedNoMaterialFacts, captureDefenderLine), None)
    val removeGuardOnlyVerdict = StoryTable.choose(Vector(removeGuardOnly)).head
    val removeGuardOnlyPlan = ExplanationPlan.fromSelected(removeGuardOnlyVerdict).get
    assertEquals(removeGuardOnlyPlan.allowedClaim, Some(ExplanationClaim.RemovesDefender))
    assertEquals(
      DeterministicRenderer.fromPlan(removeGuardOnlyPlan).exists(_.claimKey == "removes_defender"),
      true
    )

    val discoveredOnly =
      TacticDiscoveredAttack
        .write(pinLookingNoKingFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val discoveredOnlyVerdicts = StoryTable.choose(Vector(discoveredOnly))
    assertEquals(discoveredOnlyVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(discoveredOnlyVerdicts.head.story.tactic, Some(Tactic.DiscoveredAttack))
    assertEquals(
      ExplanationPlan.fromSelected(discoveredOnlyVerdicts.head).flatMap(_.allowedClaim),
      Some(ExplanationClaim.RevealsAttackOnPiece)
    )

    val positiveDiscoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val positiveDiscovered =
      TacticDiscoveredAttack
        .write(positiveDiscoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val wrongBoardCheck =
      engineCheck(
        BoardFacts.fromFen("8/7k/7r/8/8/3N4/8/1B5K w - - 0 1").toOption.get,
        positiveDiscovered,
        revealLine
      )
    val staleCheck = engineCheck(positiveDiscoveredFacts, positiveDiscovered, revealLine, freshness = Some(2))
    val routeMismatchCheck =
      engineCheck(positiveDiscoveredFacts, positiveDiscovered, captureDefenderLine)
    assertEquals(wrongBoardCheck.evidenceReady, false)
    assertEquals(wrongBoardCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, wrongBoardCheck), None)
    assertEquals(staleCheck.evidenceReady, false)
    assertEquals(staleCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, staleCheck), None)
    assertEquals(routeMismatchCheck.evidenceReady, false)
    assertEquals(routeMismatchCheck.status, EngineCheckStatus.Unknown)
    assertEquals(TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, routeMismatchCheck), None)

    val routeMismatchStory =
      positiveDiscovered.copy(route = Some(captureDefenderLine), routeSan = Some("Ne5"))
    assertNoPublicOutput(routeMismatchStory, "route mismatch")

    val refuteCheck =
      engineCheck(
        positiveDiscoveredFacts,
        positiveDiscovered,
        revealLine,
        EngineCheckStatus.Supports,
        before = 220,
        after = 20
      )
    val refuted = TacticDiscoveredAttack.withEngineCheck(positiveDiscovered, refuteCheck).get
    assertEquals(refuteCheck.status, EngineCheckStatus.Refutes)
    assertNoPublicOutput(refuted, "engine refutes plausible row")

    val positivePinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val positivePin =
      TacticPin
        .write(
          positivePinFacts,
          Some(revealLine),
          Some(Square('b', 1)),
          Some(Square('g', 6)),
          Some(Square('h', 7))
        )
        .get
    val skewerLooking = positivePin.copy(tactic = Some(Tactic.Skewer))
    assertNoPublicOutput(skewerLooking, "Skewer-looking relation before Skewer opens")

  test("LDH-6 Downstream Boundary Smoke sends only selected Lead Verdicts to text stages"):
    final case class DownstreamFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

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
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def lowContextProof =
      proof(
        boardProof = 20,
        lineProof = 0,
        ownerProof = 0,
        anchorProof = 0,
        routeProof = 0,
        persistence = 0,
        immediacy = 0,
        forcing = 0,
        conversionPrize = 0,
        kingHeat = 0,
        pieceSupport = 0,
        pawnSupport = 0,
        sourceFit = 0,
        novelty = 0,
        clarity = 0
      )

    def assertNoStandaloneText(verdict: Verdict, renderedFromLead: RenderedLine, label: String): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)
      plan.foreach: relationPlan =>
        assertEquals(LlmNarrationSmoke.mockNarrate(relationPlan, Some(renderedFromLead)), None, label)
        assertEquals(LlmNarrationSmoke.codexCliPrompt(relationPlan, renderedFromLead), None, label)

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      TacticPin
        .write(pinFacts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6)), Some(Square('h', 7)))
        .get

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardLine = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      TacticRemoveGuard
        .write(removeGuardFacts, Some(removeGuardLine), Some(Square('g', 6)), Some(Square('e', 5)))
        .get

    val fixtures = Vector(
      DownstreamFixture(
        "Tactic.DiscoveredAttack",
        discoveredFacts,
        discovered,
        revealLine,
        TacticDiscoveredAttack.withEngineCheck,
        ExplanationClaim.RevealsAttackOnPiece
      ),
      DownstreamFixture(
        "Tactic.Pin",
        pinFacts,
        pin,
        revealLine,
        TacticPin.withEngineCheck,
        ExplanationClaim.PinsPiece
      ),
      DownstreamFixture(
        "Tactic.RemoveGuard",
        removeGuardFacts,
        removeGuard,
        removeGuardLine,
        TacticRemoveGuard.withEngineCheck,
        ExplanationClaim.RemovesDefender
      )
    )

    val leadOutputs = fixtures.map: fixture =>
      val verdict = StoryTable.choose(Vector(fixture.story)).head
      val unselected = verdict.copy(selected = false)
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      assertEquals(verdict.role, Role.Lead, fixture.label)
      assertEquals(plan.role, Role.Lead, fixture.label)
      assertEquals(plan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      assertEquals(ExplanationPlan.fromSelected(unselected), None, fixture.label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text), fixture.label)
      assertEquals(
        LlmNarrationSmoke.check(plan, rendered, rendered.text),
        NarrationSmokeCheck(true, Vector.empty),
        fixture.label
      )

      Vector(
        s"renderedText: ${rendered.text}",
        s"claimKey: ${rendered.claimKey}",
        s"strength: ${rendered.strength}",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: allowedInput =>
        assert(prompt.contains(allowedInput), s"${fixture.label}: prompt must include $allowedInput")

      Vector(
        "ExplanationPlan",
        "Verdict",
        "Story",
        "StoryProof",
        "LineProof",
        "PinProof",
        "RemoveGuardProof",
        "EngineCheck",
        "EngineEval",
        "EngineLine",
        "BoardFacts",
        "BoardMood",
        "proofFailures",
        "source row",
        "role:",
        "scene:",
        "tactic:",
        "side:",
        "target:",
        "anchor:",
        "route:",
        "raw PV"
      ).foreach: forbiddenInput =>
        assert(!prompt.contains(forbiddenInput), s"${fixture.label}: prompt must not include $forbiddenInput")

      val capped =
        fixture
          .attach(
            fixture.story,
            engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)
          )
          .get
      val refuted = fixture
        .attach(
          fixture.story,
          engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Supports, 220, 20)
        )
        .get
      assertNoStandaloneText(StoryTable.choose(Vector(capped)).head, rendered, s"${fixture.label}: capped")
      assertNoStandaloneText(StoryTable.choose(Vector(refuted)).head, rendered, s"${fixture.label}: refuted")

      rendered

    val sameLineVerdicts = StoryTable.choose(Vector(discovered, pin))
    val supportVerdict = sameLineVerdicts.find(_.role == Role.Support).get
    val contextStory = Story(
      Scene.Quiet,
      proof = lowContextProof,
      side = Side.White,
      target = Some(Square('g', 6)),
      anchor = Some(Square('b', 1)),
      route = Some(revealLine),
      routeSan = Some("Nf4"),
      rival = Side.Black,
      storyProof = StoryProof.fromBoardFacts(discoveredFacts, revealLine)
    )
    val contextVerdict = StoryTable.choose(Vector(discovered, contextStory)).find(_.story == contextStory).get
    val blockedVerdict =
      StoryTable.choose(Vector(discovered.copy(route = Some(removeGuardLine), routeSan = Some("Ne5")))).head

    assertEquals(sameLineVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(sameLineVerdicts.count(_.role == Role.Support), 1)
    assertEquals(contextVerdict.role, Role.Context)
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertNoStandaloneText(supportVerdict, leadOutputs.head, "Support row")
    assertNoStandaloneText(contextVerdict, leadOutputs.head, "Context row")
    assertNoStandaloneText(blockedVerdict, leadOutputs.head, "Blocked row")

    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val rendererFromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val smokeParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assertEquals(
      fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    assertEquals(
      rendererFromPlanMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )
    Vector("fromStory", "fromProof", "fromEngineCheck", "fromVerdict").foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"Renderer must not expose $forbiddenMethod")
    Vector("canPhraseXRay", "canPhraseLineTactic", "canPhrasePressure", "canPhraseInitiative").foreach:
      forbiddenTemplate =>
        assert(!rendererMethodNames.contains(forbiddenTemplate), s"Renderer must not add $forbiddenTemplate")
    Vector(
      "Story",
      "Proof",
      "StoryProof",
      "LineProof",
      "PinProof",
      "RemoveGuardProof",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "MultiTargetProof",
      "ThreatProof",
      "DefenseProof"
    ).foreach: forbiddenType =>
      assert(!rendererParameterNames.contains(forbiddenType), s"Renderer must not accept $forbiddenType")
      assert(!smokeParameterNames.contains(forbiddenType), s"LLM smoke must not accept $forbiddenType")

  test("LDH-7 Diagnostics Boundary keeps diagnostics out of public meaning"):
    def engineCheck(facts: BoardFacts, story: Story, line: Line, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val revealLine = Line(Square('d', 3), Square('f', 4))
    val facts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val story =
      TacticDiscoveredAttack.write(facts, Some(revealLine), Some(Square('b', 1)), Some(Square('g', 6))).get
    val supported =
      TacticDiscoveredAttack
        .withEngineCheck(story, engineCheck(facts, story, revealLine, EngineCheckStatus.Supports))
        .get
    val leadVerdict = StoryTable.choose(Vector(supported)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
    val diagnosticMarkers =
      Vector(
        "ldh7 proof failure marker",
        "ldh7 raw proof text marker",
        "ldh7 enginecheck payload marker",
        "ldh7 storytable debug relation marker"
      )
    val diagnosticVerdict = leadVerdict.copy(
      proofFailures = Vector(BoardFacts.MissingEvidence("LDH-7 diagnostic", diagnosticMarkers)),
      engineCheckStatus = Some(EngineCheckStatus.Refutes),
      engineStrengthLimited = true
    )
    val diagnosticPlan = leadPlan.copy(
      allowedClaim = None,
      relations = Vector(ExplanationRelation.CappedSameStory, ExplanationRelation.BlockedByEngineRefute),
      debugOnly = true
    )

    assert(diagnosticVerdict.proofFailures.nonEmpty)
    assertEquals(diagnosticVerdict.values, leadVerdict.values)
    assertEquals(leadVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.RevealsAttackOnPiece))
    assertEquals(rendered.claimKey, "reveals_attack_on_piece")
    assertEquals(
      LlmNarrationSmoke.check(leadPlan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )

    diagnosticMarkers.foreach: marker =>
      assert(
        !diagnosticVerdict.values.mkString(" ").contains(marker),
        s"Verdict.values leaked diagnostic marker: $marker"
      )
      assert(!leadPlan.toString.contains(marker), s"ExplanationPlan leaked diagnostic marker: $marker")
      assert(!rendered.text.contains(marker), s"Renderer leaked diagnostic marker: $marker")
      assert(!prompt.contains(marker), s"LLM smoke prompt leaked diagnostic marker: $marker")

    Vector("EngineCheck", "EngineEval", "EngineLine", "Supports", "Refutes", "Caps", "h8", "h7").foreach:
      engineDiagnostic =>
        assert(
          !leadPlan.toString.contains(engineDiagnostic),
          s"EngineCheck diagnostic reached ExplanationPlan: $engineDiagnostic"
        )
        assert(
          !prompt.contains(engineDiagnostic),
          s"EngineCheck diagnostic reached LLM smoke prompt: $engineDiagnostic"
        )

    diagnosticPlan.relations.foreach: relation =>
      assert(
        !rendered.text.contains(relation.key),
        s"StoryTable relation leaked to renderer wording: ${relation.key}"
      )
      assert(
        !prompt.contains(relation.key),
        s"StoryTable relation leaked to LLM smoke prompt: ${relation.key}"
      )
    assertEquals(DeterministicRenderer.fromPlan(diagnosticPlan), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(diagnosticPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.codexCliPrompt(diagnosticPlan, rendered), None)

    val explanationSurface =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet ++
        ExplanationPlan.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererSurface =
      DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet ++
        DeterministicRenderer.getClass.getDeclaredFields.map(_.getName).toSet
    val smokeSurface =
      LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet ++
        LlmNarrationSmoke.getClass.getDeclaredFields.map(_.getName).toSet
    Vector(
      "proofFailures",
      "missingEvidence",
      "engineCheck",
      "sourceRow",
      "fromStory",
      "fromProof",
      "fromEngineCheck"
    ).foreach: forbiddenSurface =>
      assert(!explanationSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))
      assert(!rendererSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))
      assert(!smokeSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)))

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector ++
        Tactic.values.map(_.toString).toVector
    Vector("ldh", "diagnostics_boundary", "diagnostic_boundary", "fixture", "helper", "test_helper").foreach:
      forbidden =>
        assert(
          !runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)),
          s"test helper became runtime authority: $forbidden"
        )

  test("LDH Closeout Hard Cleanup keeps Line Defender authority separated and public surfaces closed"):
    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
        .get

    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinMove = Line(Square('a', 8), Square('e', 8))
    val pin =
      TacticPin
        .write(pinFacts, Some(pinMove), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
        .get

    val removeGuardFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('g', 8), Square('c', 4))
    val removeGuard =
      TacticRemoveGuard
        .write(
          removeGuardFacts,
          Some(removeGuardMove),
          Some(Square('e', 5)),
          Some(Square('c', 4))
        )
        .get

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(materialFacts, materialMove).get
    val material = SceneMaterial.write(materialFacts, materialMove).get

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val defense = SceneDefense
      .write(defenseFacts, Line(Square('f', 5), Square('d', 4)), Line(Square('e', 4), Square('f', 5)))
      .get

    def selectedPlan(story: Story): ExplanationPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    val discoveredPlan = selectedPlan(discovered)
    val pinPlan = selectedPlan(pin)
    val removeGuardPlan = selectedPlan(removeGuard)
    val discoveredRendered = DeterministicRenderer.fromPlan(discoveredPlan).get
    val pinRendered = DeterministicRenderer.fromPlan(pinPlan).get
    val removeGuardRendered = DeterministicRenderer.fromPlan(removeGuardPlan).get
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val rendererInputs =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .flatMap(_.getParameterTypes.map(_.getSimpleName))
        .toVector
    val storyFieldNames = classOf[Story].getDeclaredFields.map(_.getName).toSet
    val lineFactSurface =
      classOf[BoardFacts.LineFact].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts.LineFact].getDeclaredFields.map(_.getName).toSet
    val lineProofSurface =
      classOf[LineProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[LineProof].getDeclaredFields.map(_.getName).toSet
    val pinProofSurface =
      classOf[PinProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[PinProof].getDeclaredFields.map(_.getName).toSet
    val removeGuardProofSurface =
      classOf[RemoveGuardProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RemoveGuardProof].getDeclaredFields.map(_.getName).toSet

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
        StoryWriter.TacticDecoy,
        StoryWriter.TacticInterference,
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
    Vector(
      "xrayProof",
      "xRayProof",
      "deflectionProof",
      "pressureProof",
      "initiativeProof",
      "lineTacticProof",
      "rayProof",
      "kingSafetyProof",
      "mateThreatProof"
    ).foreach: forbiddenProofHome =>
      assert(
        !storyFieldNames.exists(_.equalsIgnoreCase(forbiddenProofHome)),
        s"new proof home opened: $forbiddenProofHome"
      )

    Vector("story", "verdict", "explanation", "rendered", "claim", "proof").foreach: forbidden =>
      assert(
        !lineFactSurface.exists(_.toLowerCase.contains(forbidden)),
        s"LineFact became authority surface: $forbidden"
      )
    Vector("PinProof", "RemoveGuardProof", "Hanging", "Defense", "Skewer", "XRay").foreach: forbidden =>
      assert(!lineProofSurface.exists(_.contains(forbidden)), s"LineProof mixed authority: $forbidden")
    Vector("winsMaterial", "materialGain", "materialBalance", "SceneMaterial").foreach: forbidden =>
      assert(
        !lineProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)),
        s"LineProof owns material claim: $forbidden"
      )
    Vector("LineProof", "RemoveGuardProof", "Material", "Hanging", "Defense", "Skewer", "XRay").foreach:
      forbidden =>
        assert(!pinProofSurface.exists(_.contains(forbidden)), s"PinProof mixed authority: $forbidden")
    Vector("LineProof", "PinProof", "Hanging", "Defense", "Skewer", "XRay").foreach: forbidden =>
      assert(
        !removeGuardProofSurface.exists(_.contains(forbidden)),
        s"RemoveGuardProof mixed authority: $forbidden"
      )
    Vector("winsMaterial", "materialGain", "materialBalance", "SceneMaterial").foreach: forbidden =>
      assert(
        !removeGuardProofSurface.exists(_.toLowerCase.contains(forbidden.toLowerCase)),
        s"RemoveGuardProof owns material claim: $forbidden"
      )

    assert(discovered.lineProof.exists(_.complete))
    assertEquals(discovered.lineProof.exists(_.publicClaimAllowed), false)
    assertEquals(discovered.pinProof, None)
    assertEquals(discovered.removeGuardProof, None)
    assertEquals(discovered.captureResult, None)
    assertEquals(discovered.threatProof, None)
    assertEquals(discovered.defenseProof, None)
    assertEquals(discovered.writer, Some(StoryWriter.TacticDiscoveredAttack))
    assertEquals(discovered.tactic, Some(Tactic.DiscoveredAttack))

    assert(pin.pinProof.exists(_.complete))
    assertEquals(pin.pinProof.exists(_.publicClaimAllowed), false)
    assertEquals(pin.lineProof, None)
    assertEquals(pin.removeGuardProof, None)
    assertEquals(pin.captureResult, None)
    assertEquals(pin.threatProof, None)
    assertEquals(pin.defenseProof, None)
    assertEquals(pin.writer, Some(StoryWriter.TacticPin))
    assertEquals(pin.tactic, Some(Tactic.Pin))

    assert(removeGuard.removeGuardProof.exists(_.complete))
    assertEquals(removeGuard.removeGuardProof.exists(_.publicClaimAllowed), false)
    assertEquals(removeGuard.lineProof, None)
    assertEquals(removeGuard.pinProof, None)
    assertEquals(removeGuard.captureResult, None)
    assertEquals(removeGuard.threatProof, None)
    assertEquals(removeGuard.defenseProof, None)
    assertEquals(removeGuard.writer, Some(StoryWriter.TacticRemoveGuard))
    assertEquals(removeGuard.tactic, Some(Tactic.RemoveGuard))

    assert(hanging.captureResult.exists(_.positiveMaterial))
    assertEquals(hanging.lineProof, None)
    assertEquals(hanging.pinProof, None)
    assertEquals(hanging.removeGuardProof, None)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertEquals(material.lineProof, None)
    assertEquals(material.pinProof, None)
    assertEquals(material.removeGuardProof, None)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertEquals(defense.lineProof, None)
    assertEquals(defense.pinProof, None)
    assertEquals(defense.removeGuardProof, None)

    val lineDefenderClaims =
      Map(
        "DiscoveredAttack" -> ExplanationClaim.DiscoveredAttackAllowed.map(_.key).toSet,
        "Pin" -> ExplanationClaim.PinAllowed.map(_.key).toSet,
        "RemoveGuard" -> ExplanationClaim.RemoveGuardAllowed.map(_.key).toSet
      )
    val openedClaimHomes =
      Map(
        "Hanging" -> ExplanationClaim.HangingAllowed.map(_.key).toSet,
        "Material" -> ExplanationClaim.MaterialAllowed.map(_.key).toSet,
        "Defense" -> ExplanationClaim.DefenseAllowed.map(_.key).toSet,
        "Decoy" -> ExplanationClaim.DecoyAllowed.map(_.key).toSet
      )
    assertEquals(lineDefenderClaims("DiscoveredAttack"), Set("reveals_attack_on_piece"))
    assertEquals(lineDefenderClaims("Pin"), Set("pins_piece"))
    assertEquals(lineDefenderClaims("RemoveGuard"), Set("removes_defender"))
    lineDefenderClaims.toVector
      .combinations(2)
      .foreach: pair =>
        val first = pair(0)
        val second = pair(1)
        assertEquals(first._2.intersect(second._2), Set.empty[String], s"${first._1} invaded ${second._1}")
    lineDefenderClaims.foreach: (owner, keys) =>
      openedClaimHomes.foreach: (claimHome, homeKeys) =>
        assertEquals(keys.intersect(homeKeys), Set.empty[String], s"$owner invaded $claimHome claim home")

    assertEquals(discoveredPlan.allowedClaim.map(_.key), Some("reveals_attack_on_piece"))
    assertEquals(pinPlan.allowedClaim.map(_.key), Some("pins_piece"))
    assertEquals(removeGuardPlan.allowedClaim.map(_.key), Some("removes_defender"))
    assertEquals(discoveredRendered.text, "Nf4 reveals an attack on the piece on g6.")
    assertEquals(pinRendered.text, "Re8 pins the piece on e2.")
    assertEquals(removeGuardRendered.text, "Bxc4 removes the defender of the piece on e5.")
    assertEquals(rendererInputs, Vector("ExplanationPlan"))
    Vector(
      "canPhraseXRay",
      "canPhraseLineTactic",
      "canPhraseDeflection",
      "canPhrasePressure",
      "canPhraseInitiative"
    ).foreach: forbiddenRenderer =>
      assert(
        !rendererMethodNames.contains(forbiddenRenderer),
        s"broad renderer authority opened: $forbiddenRenderer"
      )

    val liveLineDefenderAuthorityNames =
      StoryWriter.values
        .filter(writer =>
          writer == StoryWriter.TacticDiscoveredAttack ||
            writer == StoryWriter.TacticPin ||
            writer == StoryWriter.TacticRemoveGuard
        )
        .map(_.toString.toLowerCase)
        .toVector ++ lineDefenderClaims.values.flatten.toVector
    Vector(
      "line_tactic",
      "ray",
      "xray",
      "x_ray",
      "skewer",
      "deflect",
      "deflection",
      "overload",
      "pressure",
      "initiative"
    ).foreach: forbidden =>
      assert(
        !liveLineDefenderAuthorityNames.exists(_.contains(forbidden)),
        s"broad term became LDH authority: $forbidden"
      )

    Vector(
      discoveredPlan -> discoveredRendered,
      pinPlan -> pinRendered,
      removeGuardPlan -> removeGuardRendered
    ).foreach: (plan, rendered) =>
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
      assertEquals(LlmNarrationSmoke.check(plan, rendered, rendered.text).accepted, true)
      assert(
        LlmNarrationSmoke
          .codexCliPrompt(plan, rendered)
          .exists(_.contains("Rephrase only. Do not add chess facts."))
      )

    Vector(
      discovered -> Vector(
        Tactic.Pin,
        Tactic.RemoveGuard,
        Tactic.Hanging,
        Tactic.Skewer,
        Tactic.Xray,
        Tactic.Deflect,
        Tactic.Overload
      ),
      pin -> Vector(
        Tactic.DiscoveredAttack,
        Tactic.RemoveGuard,
        Tactic.Hanging,
        Tactic.Skewer,
        Tactic.Xray,
        Tactic.Deflect,
        Tactic.Overload
      ),
      removeGuard -> Vector(
        Tactic.DiscoveredAttack,
        Tactic.Pin,
        Tactic.Hanging,
        Tactic.Skewer,
        Tactic.Xray,
        Tactic.Deflect,
        Tactic.Overload
      )
    ).foreach: (story, tactics) =>
      tactics.foreach: tactic =>
        val forged = story.copy(tactic = Some(tactic))
        val verdict = StoryTable.choose(Vector(forged)).head
        assertEquals(verdict.role, Role.Blocked, s"${story.writer} accepted forged $tactic")
        ExplanationPlan
          .fromSelected(verdict)
          .foreach: forgedPlan =>
            assertEquals(forgedPlan.allowedClaim, None, s"${story.writer} claimed forged $tactic")
            assertEquals(forgedPlan.debugOnly, true, s"${story.writer} made forged $tactic public")
            assertEquals(
              DeterministicRenderer.fromPlan(forgedPlan),
              None,
              s"${story.writer} rendered forged $tactic"
            )

    Vector(
      "Nf4 wins material on g6.",
      "Nf4 is a skewer on g6.",
      "Nf4 creates pressure on g6.",
      "Re8 means the piece on e2 cannot move.",
      "Re8 creates a mate threat.",
      "Bxc4 is a deflection tactic.",
      "Bxc4 overloads the defender.",
      "Bxc4 takes the initiative."
    ).foreach: output =>
      val relevant =
        if output.startsWith("Nf4") then discoveredPlan -> discoveredRendered
        else if output.startsWith("Re8") then pinPlan -> pinRendered
        else removeGuardPlan -> removeGuardRendered
      assertEquals(LlmNarrationSmoke.check(relevant._1, relevant._2, output).accepted, false, output)

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector
    Vector("ldh_closeout", "public_route", "production_api", "public_llm", "user_facing_llm").foreach:
      forbidden =>
        assert(
          !runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)),
          s"closed surface became runtime authority: $forbidden"
        )

  test("LNC-5 Downstream Boundary Audit keeps Line Defender speech bounded"):
    final case class DownstreamFixture(
        label: String,
        facts: BoardFacts,
        story: Story,
        line: Line,
        attach: (Story, EngineCheck) => Option[Story],
        expectedClaim: ExplanationClaim
    )

    def highProof: Proof =
      proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        persistence = 99,
        immediacy = 99,
        forcing = 99,
        conversionPrize = 99,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = 99,
        pawnSupport = 0,
        clarity = 99
      )

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
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredLine = Line(Square('d', 3), Square('f', 4))
    val discovered =
      TacticDiscoveredAttack
        .write(discoveredFacts, Some(discoveredLine), Some(Square('b', 1)), Some(Square('g', 6)))
        .get
    val pinFacts = BoardFacts.fromFen("r5k1/8/8/8/8/8/4N3/4K3 b - - 0 1").toOption.get
    val pinLine = Line(Square('a', 8), Square('e', 8))
    val pin = TacticPin
      .write(pinFacts, Some(pinLine), Some(Square('e', 8)), Some(Square('e', 2)), Some(Square('e', 1)))
      .get
    val removeFacts = BoardFacts.fromFen("6Bk/8/8/4r3/2n5/8/8/7K w - - 0 1").toOption.get
    val removeLine = Line(Square('g', 8), Square('c', 4))
    val remove =
      TacticRemoveGuard.write(removeFacts, Some(removeLine), Some(Square('e', 5)), Some(Square('c', 4))).get
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
        .copy(
          proof = proof(
            boardProof = 99,
            lineProof = 99,
            ownerProof = 99,
            anchorProof = 99,
            routeProof = 99,
            pieceSupport = 99,
            clarity = 99
          )
        )
        .copy(proof = highProof)

    val requiredForbiddenKeys =
      Vector(
        "wins_material",
        "winning",
        "decisive",
        "best_move",
        "only_move",
        "forced",
        "cannot_move",
        "no_defense",
        "front_piece_must_move",
        "wins_rear_piece",
        "creates_pressure",
        "takes_initiative",
        "mate_threat",
        "king_unsafe"
      )
    val forbiddenOutputs =
      Vector(
        "wins material" -> ((san: String) => s"$san wins material."),
        "winning" -> ((san: String) => s"$san is winning."),
        "decisive" -> ((san: String) => s"$san is decisive."),
        "best move" -> ((san: String) => s"$san is the best move."),
        "only move" -> ((san: String) => s"$san is the only move."),
        "forced" -> ((san: String) => s"$san starts a forced line."),
        "cannot move" -> ((san: String) => s"$san means the piece cannot move."),
        "no defense" -> ((san: String) => s"$san leaves no defense."),
        "front piece must move" -> ((san: String) => s"$san means the front piece must move."),
        "wins rear piece" -> ((san: String) => s"$san wins the rear piece."),
        "pressure" -> ((san: String) => s"$san creates pressure."),
        "initiative" -> ((san: String) => s"$san takes the initiative."),
        "mate threat" -> ((san: String) => s"$san creates a mate threat."),
        "king unsafe" -> ((san: String) => s"$san makes the king unsafe.")
      )

    val fixtures = Vector(
      DownstreamFixture(
        "Tactic.DiscoveredAttack",
        discoveredFacts,
        discovered,
        discoveredLine,
        TacticDiscoveredAttack.withEngineCheck,
        ExplanationClaim.RevealsAttackOnPiece
      ),
      DownstreamFixture(
        "Tactic.Pin",
        pinFacts,
        pin,
        pinLine,
        TacticPin.withEngineCheck,
        ExplanationClaim.PinsPiece
      ),
      DownstreamFixture(
        "Tactic.RemoveGuard",
        removeFacts,
        remove,
        removeLine,
        TacticRemoveGuard.withEngineCheck,
        ExplanationClaim.RemovesDefender
      ),
      DownstreamFixture(
        "Tactic.Skewer",
        skewerFacts,
        skewer,
        skewerLine,
        TacticSkewer.withEngineCheck,
        ExplanationClaim.SkewersPieceToPiece
      )
    )

    fixtures.foreach: fixture =>
      val leadVerdict = StoryTable.choose(Vector(fixture.story)).head
      assertEquals(leadVerdict.role, Role.Lead, fixture.label)
      val plan = ExplanationPlan.fromSelected(leadVerdict).get
      assertEquals(plan.allowedClaim, Some(fixture.expectedClaim), fixture.label)
      val forbiddenKeys = plan.forbiddenWording.map(_.key)
      requiredForbiddenKeys.foreach: key =>
        assert(forbiddenKeys.contains(key), s"${fixture.label} missing forbidden wording key: $key")
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
      Vector(
        s"renderedText: ${rendered.text}",
        s"claimKey: ${rendered.claimKey}",
        s"strength: ${rendered.strength}",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: expectedInput =>
        assert(prompt.contains(expectedInput), s"${fixture.label} prompt missing $expectedInput")
      forbiddenOutputs.foreach: (label, outputFor) =>
        val result = LlmNarrationSmoke.check(plan, rendered, outputFor(plan.routeSan.get))
        assertEquals(result.accepted, false, s"${fixture.label}: $label")
        assert(result.violations.contains("forbidden_wording"), s"${fixture.label}: $label -> $result")

      val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false)
      val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
      val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
      val unselectedVerdict = leadVerdict.copy(selected = false)
      val cappedVerdict =
        StoryTable
          .choose(
            Vector(
              fixture
                .attach(
                  fixture.story,
                  engineCheck(fixture.facts, fixture.story, fixture.line, EngineCheckStatus.Caps)
                )
                .get
            )
          )
          .head
      val refutedVerdict =
        StoryTable
          .choose(
            Vector(
              fixture
                .attach(
                  fixture.story,
                  engineCheck(
                    fixture.facts,
                    fixture.story,
                    fixture.line,
                    EngineCheckStatus.Supports,
                    before = 220,
                    after = 20
                  )
                )
                .get
            )
          )
          .head
      Vector(supportVerdict, contextVerdict, blockedVerdict, unselectedVerdict, cappedVerdict, refutedVerdict)
        .foreach: verdict =>
          assertEquals(ExplanationPlan.fromSelected(verdict), None, s"${fixture.label}: $verdict")

  test("LNC-7 Test Helper Runtime Boundary Audit keeps helpers out of runtime authority"):
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
      "LNC-",
      "LDH-",
      "Closeout",
      "closeout",
      "fixture map",
      "negative corpus",
      "Line Defender Neighborhood",
      "Contact Neighborhood"
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
    Vector("Fixture", "Corpus", "Closeout", "Neighborhood", "Audit").foreach: helperName =>
      assert(
        !runtimeAuthorityNames.exists(_.contains(helperName)),
        s"test helper name became runtime authority: $helperName"
      )

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
        .copy(
          proof = proof(
            boardProof = 99,
            lineProof = 99,
            ownerProof = 99,
            anchorProof = 99,
            routeProof = 99,
            pieceSupport = 99,
            clarity = 99
          )
        )
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(skewer)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    Vector("front_piece_must_move", "wins_rear_piece", "mate_threat", "king_unsafe", "creates_pressure")
      .foreach: internalKey =>
        val result = LlmNarrationSmoke.check(plan, rendered, s"${rendered.text} $internalKey")
        assert(
          !result.violations.contains("forbidden_wording"),
          s"internal key must not be treated as public prose: $internalKey -> $result"
        )

