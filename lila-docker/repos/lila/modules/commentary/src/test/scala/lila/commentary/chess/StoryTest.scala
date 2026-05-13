package lila.commentary.chess

import chess.format.Fen

class StoryTest extends ChessTestSupport:

  test("Proof validates scores and computes the public strength formulas"):
    val strong = proof(
      boardProof = 75,
      ownerProof = 85,
      anchorProof = 90,
      routeProof = 80,
      forcing = 90,
      conversionPrize = 70,
      kingHeat = 80,
      lineProof = 66,
      immediacy = 60
    )

    assertEquals(strong.truth, 66)
    assertEqualsDouble(strong.tacticHeat, 76.4, 0.0001)
    assertEqualsDouble(strong.planHeat, 74.0, 0.0001)
    assertEqualsDouble(strong.publicStrength, 66.0, 0.0001)

    intercept[IllegalArgumentException]:
      proof(boardProof = -1)
    intercept[IllegalArgumentException]:
      proof(counterplayRisk = 101)

  test("Proof constructor uses contract field names"):
    val exact = Proof(
      boardProof = 70,
      lineProof = 71,
      ownerProof = 72,
      anchorProof = 73,
      routeProof = 74,
      persistence = 75,
      immediacy = 76,
      forcing = 77,
      conversionPrize = 78,
      counterplayRisk = 20,
      kingHeat = 79,
      pieceSupport = 80,
      pawnSupport = 81,
      sourceFit = 82,
      novelty = 83,
      clarity = 84
    )

    assertEquals(exact.values.size, Proof.Size)
    assertEquals(exact.values(Proof.Slots.BoardProof), 70)
    assertEquals(exact.values(Proof.Slots.LineProof), 71)
    assertEquals(exact.values(Proof.Slots.OwnerProof), 72)
    assertEquals(exact.values(Proof.Slots.RouteProof), 74)
    assertEquals(exact.values(Proof.Slots.ConversionPrize), 78)
    assertEquals(exact.values(Proof.Slots.CounterplayRisk), 20)
    assertEquals(exact.values(Proof.Slots.KingHeat), 79)
    assertEquals(exact.values(Proof.Slots.PieceSupport), 80)
    assertEquals(exact.values(Proof.Slots.PawnSupport), 81)
    assertEquals(exact.values(Proof.Slots.SourceFit), 82)

  test("Story constants preserve the fixed story shape"):
    assertEquals(Story.Size, 177)
    assertEquals(Story.SceneSlots, 30)
    assertEquals(Story.PlanSlots, 32)
    assertEquals(Story.TacticSlots, 27)
    assertEquals(Story.PawnSlots, 16)
    assertEquals(Story.PieceSlots, 16)
    assertEquals(Story.KingSlots, 16)
    assertEquals(Story.OpeningSlots, 8)
    assertEquals(Story.ProofSlots, 32)
    assertEquals(Story.Slots.Scene, 0)
    assertEquals(Story.Slots.Plan, 30)
    assertEquals(Story.Slots.Tactic, 62)
    assertEquals(Story.Slots.Pawn, 89)
    assertEquals(Story.Slots.Piece, 105)
    assertEquals(Story.Slots.King, 121)
    assertEquals(Story.Slots.Opening, 137)
    assertEquals(Story.Slots.Proof, 145)
    assertEquals(Story.Slots.End, Story.Size)

  test("Story values encode exact shape, public family, identity, and proof"):
    val e5 = Square('e', 5)
    val d4 = Square('d', 4)
    val route = Line(Square('c', 4), e5)
    val story = Story(
      Scene.Plan,
      side = Side.White,
      target = Some(e5),
      anchor = Some(d4),
      route = Some(route),
      rival = Side.Black,
      plan = Some(Plan.CenterBreak),
      proof = proof(boardProof = 91, lineProof = 62, routeProof = 88, conversionPrize = 84)
    )
    val values = story.values

    assertEquals(values.size, Story.Size)
    assertEquals(values(Story.Slots.Scene + Scene.Plan.ordinal), 1)
    assertEquals(values(Story.Slots.Plan + Plan.CenterBreak.ordinal), 1)
    assertEquals(values(Story.Slots.Tactic + Tactic.Fork.ordinal), 0)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Side), Side.White.ordinal)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Rival), Side.Black.ordinal)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Target), e5.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.Anchor), d4.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.RouteFrom), route.from.index + 1)
    assertEquals(values(Story.Slots.Pawn + Story.Identity.RouteTo), route.to.index + 1)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.BoardProof), 91)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.LineProof), 62)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.RouteProof), 88)
    assertEquals(values(Story.Slots.Proof + Proof.Slots.ConversionPrize), 84)
    assert(values.exists(_ != 0))

  test("Story Proof lists the full missing evidence tuple before any Story can speak"):
    val story = Story(
      Scene.Material,
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99
      )
    )
    val verdict = StoryTable.choose(Vector(story)).head
    val expectedMissing =
      Vector("side", "target", "anchor", "route", "rival", "legal line", "same-board proof")

    assertEquals(story.proofFailures, Vector(BoardFacts.MissingEvidence("Story Proof", expectedMissing)))
    assertEquals(verdict.proofFailures, story.proofFailures)
    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Story Proof does not own or duplicate Story identity"):
    val storyProofFields =
      classOf[StoryProof].getDeclaredFields
        .map(_.getName)
        .filterNot(name => name.startsWith("$") || name.contains("bitmap"))
        .toVector
        .sorted
    val storyProofMethods = classOf[StoryProof].getDeclaredMethods.map(_.getName).toVector
    val identityTerms = Vector("side", "target", "anchor", "route", "rival")

    assertEquals(storyProofFields, Vector("legalLine", "sameBoardProof"))
    identityTerms.foreach: term =>
      assert(!storyProofFields.exists(_.toLowerCase.contains(term)), s"StoryProof must not own $term")
    assert(!storyProofMethods.contains("copy"))
    assert(!classOf[Product].isAssignableFrom(classOf[StoryProof]))

  test("Story Proof legal line binding reports each missing evidence case"):
    def completeStory(
        route: Option[Line] = Some(safeRoute),
        storyProof: StoryProof = storyProof(),
        side: Side = Side.White,
        rival: Side = Side.Black,
        target: Option[Square] = Some(Square('a', 2)),
        anchor: Option[Square] = Some(safeAnchor)
    ) =
      Story(
        Scene.Material,
        side = side,
        target = target,
        anchor = anchor,
        route = route,
        rival = rival,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99
        ),
        storyProof = storyProof
      )

    val mismatchedRoute = Line(Square('b', 1), Square('b', 2))
    val cases = Vector(
      ("route missing", completeStory(route = None), Vector("route", "legal line")),
      (
        "legal line missing",
        completeStory(storyProof = StoryProof.empty),
        Vector(
          "legal line",
          "same-board proof"
        )
      ),
      (
        "untrusted facts cannot forge same-board proof",
        completeStory(
          storyProof = StoryProof.fromBoardFacts(minimalBoardFacts(), safeRoute)
        ),
        Vector(
          "legal line",
          "same-board proof"
        )
      ),
      ("legal line mismatch", completeStory(route = Some(mismatchedRoute)), Vector("legal line")),
      (
        "same-board proof missing",
        completeStory(storyProof = untrustedStoryProof()),
        Vector(
          "same-board proof"
        )
      ),
      ("side missing", completeStory(side = Side.None), Vector("side", "rival")),
      ("side both", completeStory(side = Side.Both), Vector("side", "rival")),
      ("rival missing", completeStory(rival = Side.None), Vector("rival")),
      ("rival both", completeStory(rival = Side.Both), Vector("rival")),
      ("rival same as side", completeStory(rival = Side.White), Vector("rival")),
      ("target missing", completeStory(target = None), Vector("target")),
      ("anchor missing", completeStory(anchor = None), Vector("anchor"))
    )

    cases.foreach: (label, story, missing) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(story.proofFailures, Vector(BoardFacts.MissingEvidence("Story Proof", missing)), label)
      assertEquals(verdict.proofFailures, story.proofFailures, label)
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

  test("Complete Story Proof is necessary but not sufficient for public Lead in Stage 2"):
    def completeStory(
        scene: Scene,
        plan: Option[Plan] = None,
        tactic: Option[Tactic] = None,
        route: Line = safeRoute,
        target: Square = Square('a', 2),
        anchor: Square = safeAnchor,
        proofScore: Proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          kingHeat = 99,
          immediacy = 99
        )
    ) =
      Story(
        scene,
        side = Side.White,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        rival = Side.Black,
        plan = plan,
        tactic = tactic,
        proof = proofScore,
        storyProof = storyProof(route)
      )

    val completeStories = Vector(
      "Material" -> completeStory(Scene.Material),
      "Tactic" -> completeStory(Scene.Tactic, tactic = Some(Tactic.Fork)),
      "Tactic.Hanging" -> completeStory(Scene.Tactic, tactic = Some(Tactic.Hanging)),
      "Plan" -> completeStory(Scene.Plan, plan = Some(Plan.CenterBreak)),
      "King" -> completeStory(Scene.King)
    )

    completeStories.foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(story.proofFailures, Vector.empty, label)
      assertEquals(verdict.proofFailures, Vector.empty, label)
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    val boardBacked = completeStory(Scene.Material, target = Square('b', 2))
    val source = completeStory(Scene.Source, target = Square('c', 2))
    val opening = completeStory(Scene.Opening, target = Square('d', 2))
    val sourceVerdicts = StoryTable.choose(Vector(source, boardBacked))
    val openingVerdicts = StoryTable.choose(Vector(opening, boardBacked))

    assertEquals(source.proofFailures, Vector.empty)
    assertEquals(opening.proofFailures, Vector.empty)
    assertEquals(sourceVerdicts.head.story, boardBacked)
    assertEquals(openingVerdicts.head.story, boardBacked)
    assert(sourceVerdicts.forall(_.role != Role.Lead))
    assert(openingVerdicts.forall(_.role != Role.Lead))
    assert(sourceVerdicts.find(_.story == source).exists(!_.leadAllowed))
    assert(openingVerdicts.find(_.story == opening).exists(!_.leadAllowed))

  test("CaptureResult records legal capture material evidence without public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))

    val result = CaptureResult.fromBoardFacts(facts, capture)
    val publicSurfaceNames =
      classOf[CaptureResult].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult].getDeclaredFields.map(_.getName).toSet

    assertEquals(result.side, Side.White)
    assertEquals(result.capturingPiece, Some(Piece(Side.White, Man.Pawn, Square('d', 4))))
    assertEquals(result.targetPiece, Some(Piece(Side.Black, Man.Knight, Square('e', 5))))
    assertEquals(result.captureLine, capture)
    assertEquals(result.capturedValue, Some(320))
    assertEquals(result.recaptureCandidates, Vector.empty)
    assertEquals(result.materialResult, Some(320))
    assertEquals(result.sameBoardProof, true)
    assertEquals(result.missingEvidence, Vector.empty)
    assertEquals(result.publicClaimAllowed, false)
    Vector("leadAllowed", "publicText", "render", "llm", "verdict").foreach: publicName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(publicName.toLowerCase)))

  test("CaptureResult bounded recapture check can cancel the material result"):
    val facts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('e', 4), Square('d', 5))

    val result = CaptureResult.fromBoardFacts(facts, capture)

    assertEquals(result.capturingPiece.map(_.man), Some(Man.Queen))
    assertEquals(result.targetPiece.map(_.man), Some(Man.Queen))
    assertEquals(result.capturedValue, Some(900))
    assertEquals(result.recaptureCandidates.map(_.piece.man), Vector(Man.Knight))
    assertEquals(result.materialResult, Some(0))
    assertEquals(result.positiveMaterial, false)
    assertEquals(result.missingEvidence, Vector.empty)

  test("Material-2 CaptureResult carries bounded material proof shape without Story or sentence"):
    val whiteFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val blackFacts = BoardFacts.fromFen("4k3/8/8/4p3/3N4/8/8/4K3 b - - 0 1").toOption.get
    val whiteCapture = Line(Square('d', 4), Square('e', 5))
    val blackCapture = Line(Square('e', 5), Square('d', 4))
    val whiteResult = CaptureResult.fromBoardFacts(whiteFacts, whiteCapture)
    val blackResult = CaptureResult.fromBoardFacts(blackFacts, blackCapture)
    val publicSurfaceNames =
      classOf[CaptureResult].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult].getDeclaredFields.map(_.getName).toSet ++
        classOf[CaptureResult.ExchangeStep].getDeclaredMethods.map(_.getName).toSet ++
        classOf[CaptureResult.ExchangeStep].getDeclaredFields.map(_.getName).toSet

    assertEquals(whiteResult.side, Side.White)
    assertEquals(whiteResult.captureLine, whiteCapture)
    assertEquals(whiteResult.capturedPieces, Vector(Piece(Side.Black, Man.Knight, Square('e', 5))))
    assertEquals(
      whiteResult.boundedExchangeSequence,
      Vector(CaptureResult.ExchangeStep(whiteCapture, Piece(Side.Black, Man.Knight, Square('e', 5)), 320))
    )
    assertEquals(whiteResult.materialResult, Some(320))
    assertEquals(whiteResult.sameBoardProof, true)
    assertEquals(whiteResult.missingEvidence, Vector.empty)

    assertEquals(blackResult.side, Side.Black)
    assertEquals(blackResult.captureLine, blackCapture)
    assertEquals(blackResult.capturedPieces, Vector(Piece(Side.White, Man.Knight, Square('d', 4))))
    assertEquals(
      blackResult.boundedExchangeSequence,
      Vector(CaptureResult.ExchangeStep(blackCapture, Piece(Side.White, Man.Knight, Square('d', 4)), 320))
    )
    assertEquals(blackResult.materialResult, Some(320))
    assertEquals(blackResult.sameBoardProof, true)
    assertEquals(blackResult.missingEvidence, Vector.empty)

    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(whiteResult.publicClaimAllowed, false)
    assertEquals(blackResult.publicClaimAllowed, false)

  test("Material-2 bounded exchange sequence records recapture candidates and final material result"):
    val facts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('e', 4), Square('d', 5))
    val recapture = Line(Square('f', 4), Square('d', 5))
    val result = CaptureResult.fromBoardFacts(facts, capture)

    assertEquals(result.side, Side.White)
    assertEquals(result.captureLine, capture)
    assertEquals(
      result.recaptureCandidates,
      Vector(CaptureResult.Recapture(Piece(Side.Black, Man.Knight, Square('f', 4)), recapture))
    )
    assertEquals(
      result.capturedPieces,
      Vector(
        Piece(Side.Black, Man.Queen, Square('d', 5)),
        Piece(Side.White, Man.Queen, Square('d', 5))
      )
    )
    assertEquals(
      result.boundedExchangeSequence,
      Vector(
        CaptureResult.ExchangeStep(capture, Piece(Side.Black, Man.Queen, Square('d', 5)), 900),
        CaptureResult.ExchangeStep(recapture, Piece(Side.White, Man.Queen, Square('d', 5)), 0)
      )
    )
    assertEquals(result.materialResult, Some(0))
    assertEquals(result.positiveMaterial, false)
    assertEquals(result.sameBoardProof, true)
    assertEquals(result.missingEvidence, Vector.empty)

  test("Defense-1 ThreatProof proves immediate legal material threat without Story or public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val proof = ThreatProof.fromBoardFacts(facts, threatLine)
    val publicSurfaceNames =
      classOf[ThreatProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ThreatProof].getDeclaredFields.map(_.getName).toSet
    val threatMethods = ThreatProof.getClass.getDeclaredMethods.toVector

    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.threatenedTarget, Some(Piece(Side.White, Man.Queen, Square('d', 4))))
    assertEquals(proof.attackingPiece, Some(Piece(Side.Black, Man.Knight, Square('f', 5))))
    assertEquals(proof.legalThreatLine, Some(threatLine))
    assertEquals(proof.targetValue, Some(900))
    assertEquals(proof.materialLossIfUnanswered, Some(900))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm",
      "engine"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(
      threatMethods.filter(_.getName == "fromBoardFacts").map(_.getReturnType.getSimpleName),
      Vector("ThreatProof")
    )
    assertEquals(threatMethods.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Defense-1 ThreatProof keeps non-immediate and non-material threats incomplete"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Q3/8/5N2/4K3 w - - 0 1").toOption.get
    val recaptureCancelledThreat = Line(Square('d', 5), Square('e', 4))
    val untrusted =
      minimalBoardFacts(
        rivalLegal = readyMoves(line = threatLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Queen, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        rivalLegal = readyMoves(line = Line(Square('f', 5), Square('e', 3)), captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 3)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val attackOnly =
      ThreatProof.fromBoardFacts(positiveFacts, Line(Square('f', 5), Square('h', 4)))
    val illegalThreat =
      ThreatProof.fromBoardFacts(positiveFacts, Line(Square('f', 5), Square('f', 4)))
    val untrustedThreat = ThreatProof.fromBoardFacts(untrusted, threatLine)
    val kingThreat = ThreatProof.fromBoardFacts(kingTarget, Line(Square('f', 5), Square('e', 3)))
    val cancelledThreat = ThreatProof.fromBoardFacts(defendedFacts, recaptureCancelledThreat)
    val highProofOnly =
      Story(
        Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('e', 2)),
        route = Some(safeRoute),
        rival = Side.Black,
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
        storyProof = storyProof()
      )

    Vector(attackOnly, illegalThreat, untrustedThreat, kingThreat, cancelledThreat).foreach: proof =>
      assertEquals(proof.complete, false)
      assert(proof.missingEvidence.nonEmpty)
      assertEquals(proof.publicClaimAllowed, false)
    assertEquals(attackOnly.missingEvidence.exists(_.missing.contains("legal threat line")), true)
    assertEquals(illegalThreat.missingEvidence.exists(_.missing.contains("legal threat line")), true)
    assertEquals(untrustedThreat.missingEvidence.exists(_.missing.contains("same-board proof")), true)
    assertEquals(kingThreat.missingEvidence.exists(_.missing.contains("threatened non-king target")), true)
    assertEquals(cancelledThreat.materialLossIfUnanswered, Some(0))
    assertEquals(
      cancelledThreat.missingEvidence.exists(_.missing.contains("material loss if unanswered")),
      true
    )
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.leadAllowed, false)

  test("Defense-2 DefenseProof proves target moves away without Story or public claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val proof = DefenseProof.fromBoardFacts(facts, threat, defenseMove)
    val publicSurfaceNames =
      classOf[DefenseProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[DefenseProof].getDeclaredFields.map(_.getName).toSet
    val defenseMethods = DefenseProof.getClass.getDeclaredMethods.toVector

    assertEquals(proof.defendingSide, Side.White)
    assertEquals(proof.defenseMove, Some(defenseMove))
    assertEquals(proof.defendedTarget, Some(Piece(Side.White, Man.Queen, Square('d', 4))))
    assertEquals(proof.originalThreat, threat)
    assertEquals(proof.afterDefenseTargetStatus, Some(DefenseTargetStatus.TargetMovedAway))
    assertEquals(proof.materialLossPrevented, Some(900))
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)
    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    Vector(
      "Story",
      "Sentence",
      "RenderedLine",
      "ExplanationPlan",
      "Verdict",
      "publicText",
      "render",
      "llm",
      "engine"
    ).foreach: forbidden =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    assertEquals(
      defenseMethods.filter(_.getName == "fromBoardFacts").map(_.getReturnType.getSimpleName),
      Vector("DefenseProof")
    )
    assertEquals(defenseMethods.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Defense-2 DefenseProof proves guarding blocking and attacker capture only"):
    val guardedFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/7B/4K3 w - - 0 1").toOption.get
    val guardedThreatLine = Line(Square('d', 5), Square('d', 4))
    val guardedMove = Line(Square('h', 2), Square('e', 5))
    val guardedThreat = ThreatProof.fromBoardFacts(guardedFacts, guardedThreatLine)
    val guardedProof = DefenseProof.fromBoardFacts(guardedFacts, guardedThreat, guardedMove)
    val blockedFacts = BoardFacts.fromFen("3qk3/8/8/8/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val blockedThreatLine = Line(Square('d', 8), Square('d', 4))
    val blockedMove = Line(Square('e', 4), Square('d', 5))
    val blockedThreat = ThreatProof.fromBoardFacts(blockedFacts, blockedThreatLine)
    val blockedProof = DefenseProof.fromBoardFacts(blockedFacts, blockedThreat, blockedMove)
    val capturedFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val capturedThreatLine = Line(Square('f', 5), Square('d', 4))
    val capturedMove = Line(Square('e', 4), Square('f', 5))
    val capturedThreat = ThreatProof.fromBoardFacts(capturedFacts, capturedThreatLine)
    val capturedProof = DefenseProof.fromBoardFacts(capturedFacts, capturedThreat, capturedMove)

    assertEquals(guardedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(guardedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.TargetGuarded))
    assertEquals(guardedProof.materialLossPrevented, Some(500))
    assertEquals(guardedProof.complete, true)
    assertEquals(blockedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(blockedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.AttackerLineBlocked))
    assertEquals(blockedProof.materialLossPrevented, Some(500))
    assertEquals(blockedProof.complete, true)
    assertEquals(capturedThreat.materialLossIfUnanswered, Some(500))
    assertEquals(capturedProof.afterDefenseTargetStatus, Some(DefenseTargetStatus.AttackerCaptured))
    assertEquals(capturedProof.materialLossPrevented, Some(500))
    assertEquals(capturedProof.complete, true)
    Vector(guardedProof, blockedProof, capturedProof).foreach: proof =>
      assertEquals(proof.missingEvidence, Vector.empty)
      assertEquals(proof.publicClaimAllowed, false)

  test("Defense-2 DefenseProof keeps unsupported defense-looking moves incomplete"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val illegalMove = DefenseProof.fromBoardFacts(facts, threat, Line(Square('a', 1), Square('a', 2)))
    val quietMove = DefenseProof.fromBoardFacts(facts, threat, Line(Square('e', 1), Square('e', 2)))
    val stillCapturableTargetMove =
      DefenseProof.fromBoardFacts(facts, threat, Line(Square('d', 4), Square('h', 4)))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = Line(Square('d', 4), Square('e', 4))),
        rivalLegal = readyMoves(line = threatLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Queen, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    val untrustedThreat = ThreatProof.fromBoardFacts(untrusted, threatLine)
    val untrustedProof =
      DefenseProof.fromBoardFacts(untrusted, untrustedThreat, Line(Square('d', 4), Square('e', 4)))
    val incompleteThreat =
      ThreatProof.fromBoardFacts(facts, Line(Square('f', 5), Square('f', 4)))
    val incompleteThreatProof =
      DefenseProof.fromBoardFacts(facts, incompleteThreat, Line(Square('d', 4), Square('e', 4)))
    val highProofOnly =
      Story(
        Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(Line(Square('d', 4), Square('e', 4))),
        rival = Side.Black,
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
        storyProof = storyProof()
      )

    Vector(illegalMove, quietMove, stillCapturableTargetMove, untrustedProof, incompleteThreatProof).foreach:
      proof =>
        assertEquals(proof.complete, false)
        assert(proof.missingEvidence.nonEmpty)
        assertEquals(proof.publicClaimAllowed, false)
    assertEquals(illegalMove.missingEvidence.exists(_.missing.contains("legal defense move")), true)
    assertEquals(quietMove.missingEvidence.exists(_.missing.contains("after-defense target status")), true)
    assertEquals(
      stillCapturableTargetMove.missingEvidence.exists(_.missing.contains("after-defense target status")),
      true
    )
    assertEquals(untrustedProof.missingEvidence.exists(_.missing.contains("same-board proof")), true)
    assertEquals(
      incompleteThreatProof.missingEvidence.exists(_.missing.contains("complete ThreatProof")),
      true
    )
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(highProofOnly)).head.leadAllowed, false)

  test("Defense-3 SceneDefense writer admits one proof-backed Defense Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val defense = DefenseProof.fromBoardFacts(facts, threat, defenseLine)
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(threat.complete, true)
    assertEquals(defense.complete, true)
    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('d', 4)))
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.route, Some(defenseLine))
    assertEquals(story.writer, Some(StoryWriter.SceneDefense))
    assertEquals(story.threatProof, Some(threat))
    assertEquals(story.defenseProof, Some(defense))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.engineCheck.exists(_.status == EngineCheckStatus.Refutes), false)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.story.scene, Scene.Defense)

  test("Defense-3 SceneDefense writer blocks refuted and unsupported Defense rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val quietDefense = Line(Square('e', 1), Square('e', 2))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val refute = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refutedStory = SceneDefense.withEngineCheck(story, refute).get
    val forged =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
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
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )

    assertEquals(SceneDefense.write(facts, threatLine, quietDefense), None)
    assertEquals(refute.status, EngineCheckStatus.Refutes)
    assertEquals(StoryTable.choose(Vector(refutedStory)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(refutedStory)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(forged)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(forged)).head.leadAllowed, false)

  test("Defense-4 negative corpus keeps defense-looking false positives silent"):
    val baselineFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val quietDefense = Line(Square('e', 1), Square('e', 2))
    val wrongGuardFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/B6B/4K3 w - - 0 1").toOption.get
    val alreadyDefendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Q3/8/5N2/4K3 w - - 0 1").toOption.get
    val stillLosesFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val recaptureFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RBq2/8/8/4K3 w - - 0 1").toOption.get
    val tacticFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val noThreat = SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('h', 4)), defenseLine)
    val illegalThreat = SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('f', 4)), defenseLine)
    val alreadyDefended =
      SceneDefense.write(
        alreadyDefendedFacts,
        Line(Square('d', 5), Square('e', 4)),
        Line(Square('e', 4), Square('e', 3))
      )
    val doesNotAffectTarget = SceneDefense.write(baselineFacts, threatLine, quietDefense)
    val guardsWrongPiece =
      SceneDefense.write(
        wrongGuardFacts,
        Line(Square('d', 5), Square('d', 4)),
        Line(Square('a', 2), Square('b', 3))
      )
    val stillLoses = SceneDefense.write(stillLosesFacts, threatLine, Line(Square('d', 4), Square('h', 4)))
    val allowsEquivalentRecapture =
      SceneDefense.write(
        recaptureFacts,
        Line(Square('f', 5), Square('d', 4)),
        Line(Square('e', 4), Square('f', 5))
      )
    val prophylaxis =
      Story(
        scene = Scene.Defense,
        plan = Some(Plan.Prophy),
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('e', 2)),
        route = Some(quietDefense),
        routeSan = Some("Ke2"),
        rival = Side.Black,
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
        storyProof = StoryProof.fromBoardFacts(baselineFacts, quietDefense),
        writer = Some(StoryWriter.SceneDefense)
      )
    val tacticMaterialGain = SceneDefense.write(
      tacticFacts,
      Line(Square('e', 5), Square('d', 4)),
      Line(Square('d', 4), Square('e', 5))
    )
    val kingSafetyClaim =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('e', 1)),
        anchor = Some(Square('g', 2)),
        route = Some(Line(Square('g', 2), Square('g', 3))),
        routeSan = Some("g3"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          kingHeat = 99
        ),
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense)
      )
    val mateDefenseClaim =
      Story(
        scene = Scene.Defense,
        tactic = Some(Tactic.MateNet),
        side = Side.White,
        target = Some(Square('e', 1)),
        anchor = Some(Square('g', 2)),
        route = Some(Line(Square('g', 2), Square('g', 3))),
        routeSan = Some("g3"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          kingHeat = 99
        ),
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense)
      )
    val onlyMoveClaim =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(baselineFacts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )
    val storyProofIncomplete =
      SceneDefense.write(
        minimalBoardFacts(
          sideLegal = readyMoves(line = defenseLine),
          rivalLegal = readyMoves(line = threatLine)
        ),
        threatLine,
        defenseLine
      )
    val threatProofIncomplete =
      SceneDefense.write(baselineFacts, Line(Square('f', 5), Square('f', 4)), defenseLine)
    val defenseProofIncomplete = SceneDefense.write(baselineFacts, threatLine, quietDefense)
    val refuted = SceneDefense
      .write(baselineFacts, threatLine, defenseLine)
      .flatMap: story =>
        val check = EngineCheck.fromStory(
          facts = baselineFacts,
          story = Some(story),
          engineLine = Some(EngineLine(Vector(defenseLine))),
          replyLine = Some(EngineLine(Vector(threatLine))),
          evalBefore = Some(EngineEval(120)),
          evalAfter = Some(EngineEval(-120)),
          depth = Some(12),
          freshnessPly = Some(0),
          requestedStatus = EngineCheckStatus.Supports
        )
        SceneDefense.withEngineCheck(story, check)
    val highProofOnly =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(baselineFacts, defenseLine)
      )

    Vector(
      "no actual threat" -> noThreat,
      "threat is illegal" -> illegalThreat,
      "attacked piece is already adequately defended" -> alreadyDefended,
      "defense move does not affect the target" -> doesNotAffectTarget,
      "defense move guards wrong piece" -> guardsWrongPiece,
      "defense move still loses material" -> stillLoses,
      "defense move allows equivalent recapture" -> allowsEquivalentRecapture,
      "defense is actually a tactic / material gain" -> tacticMaterialGain,
      "StoryProof incomplete" -> storyProofIncomplete,
      "ThreatProof incomplete" -> threatProofIncomplete,
      "DefenseProof incomplete" -> defenseProofIncomplete
    ).foreach: (label, defenseStory) =>
      assertEquals(defenseStory, None, label)

    Vector(
      "defense only looks like prophylaxis" -> prophylaxis,
      "king safety claim tries to enter" -> kingSafetyClaim,
      "mate defense tries to enter" -> mateDefenseClaim,
      "only-move claim tries to enter" -> onlyMoveClaim,
      "EngineCheck Refutes" -> refuted.get,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

  test("Defense-5 reuses EngineCheck statuses for existing SceneDefense Stories only"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(defenseLine))),
        replyLine = Some(EngineLine(Vector(threatLine))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val unknown = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val supports = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes =
      SceneDefense.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 100, after = -100)).get
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)

  test("Defense-5 EngineCheck cannot create or explain Defense without same-board Story binding"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val wrongRoute = Line(Square('d', 4), Square('h', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val staleOrDepthMissing = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = None,
      freshnessPly = Some(2),
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatched = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(wrongRoute),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val differentBoard = EngineCheck.fromStory(
      facts = otherFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(SceneDefense.withEngineCheck(story, engineOnly), None)
    assertEquals(SceneDefense.withEngineCheck(story, staleOrDepthMissing), None)
    assertEquals(SceneDefense.withEngineCheck(story, routeMismatched), None)
    assertEquals(SceneDefense.withEngineCheck(story, differentBoard), None)
    assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead)

  test("Defense-6 StoryTable orders Hanging Fork Material and Defense deterministically"):
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
        conversionPrize = 0,
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
      defense.copy(proof = high)
    )
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(tied(3), tied(1), tied(2), tied(0)))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.map(_.story.scene).toSet, Set(Scene.Tactic, Scene.Material, Scene.Defense))
    assertEquals(forward.map(_.story.tactic).toSet, Set(Some(Tactic.Hanging), Some(Tactic.Fork), None))
    assertEquals(forward.map(_.role), Vector(Role.Lead, Role.Support, Role.Support, Role.Support))
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)

  test("Defense-6 StoryTable blocks invalid Defense rows without creating meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val refute = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(defenseLine))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(14),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refuted = SceneDefense.withEngineCheck(story, refute).get
    val writerless = story.copy(writer = None)
    val withoutThreatProof = story.copy(threatProof = None)
    val withoutDefenseProof = story.copy(defenseProof = None)
    val incompleteDefense =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = story.proof,
        storyProof = StoryProof.empty,
        writer = Some(StoryWriter.SceneDefense),
        threatProof = story.threatProof,
        defenseProof = story.defenseProof
      )
    val highProofOnly =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine)
      )
    val alternateDefenseLine = Line(Square('d', 4), Square('c', 4))
    val alternateStory = SceneDefense.write(facts, threatLine, alternateDefenseLine).get
    val rawEvalFavoredLowProof = story
      .copy(
        proof = proof(boardProof = 72, lineProof = 72, ownerProof = 72, anchorProof = 72, routeProof = 72),
        engineCheck = Some(
          EngineCheck.fromEvidence(
            sameBoardProof = true,
            checkedMove = Some(defenseLine),
            engineLine = Some(EngineLine(Vector(defenseLine))),
            replyLine = Some(EngineLine(Vector(threatLine))),
            evalBefore = Some(EngineEval(-900)),
            evalAfter = Some(EngineEval(900)),
            depth = Some(20),
            freshnessPly = Some(0),
            requestedStatus = EngineCheckStatus.Supports
          )
        )
      )
    val proofFavored = alternateStory.copy(
      proof = proof(boardProof = 95, lineProof = 95, ownerProof = 95, anchorProof = 95, routeProof = 95)
    )

    Vector(
      "Refuted Defense" -> refuted,
      "incomplete Defense" -> incompleteDefense,
      "writerless Defense" -> writerless,
      "Defense without ThreatProof" -> withoutThreatProof,
      "Defense without DefenseProof" -> withoutDefenseProof,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertEquals(verdict.role, Role.Blocked, label)
      assertEquals(verdict.leadAllowed, false, label)

    val evalOrder = StoryTable.choose(Vector(rawEvalFavoredLowProof, proofFavored))
    assertEquals(evalOrder.head.story, proofFavored)
    assertEquals(evalOrder.exists(_.story == rawEvalFavoredLowProof), true)
    assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(story)).exists(_.story.scene == Scene.Defense), true)

  test("Defense-7 ExplanationPlan lowers selected Defense Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val fromSelectedParameterNames =
      fromSelectedMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getName)
        .mkString(" ")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(
      fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    Vector(
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "EngineLine",
      "Source",
      "String"
    ).foreach: forbiddenType =>
      assert(
        !fromSelectedParameterNames.contains(forbiddenType),
        s"ExplanationPlan must not accept $forbiddenType"
      )

    val plan = maybePlan.get
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Defense)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('d', 4)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(defenseLine))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(plan.allowedClaim.map(_.key), Some("defends_piece"))
    assertEquals(
      ExplanationClaim.DefenseAllowed.map(_.key),
      Vector("defends_piece", "prevents_material_loss", "protects_target")
    )
    assertEquals(
      ExplanationClaim.DefenseForbiddenKeys,
      Vector(
        "only_move",
        "best_defense",
        "refutes_attack",
        "stops_counterplay",
        "solves_position",
        "king_safe",
        "mate_defense",
        "no_counterplay"
      )
    )
    assertEquals(plan.evidenceLine, Some(defenseLine))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    Vector(
      "only_move",
      "best_move",
      "best_defense",
      "refutes_attack",
      "stops_counterplay",
      "solves_position",
      "king_safe",
      "mate_defense",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    Vector(
      "threatProof",
      "defenseProof",
      "engineCheck",
      "boardFacts",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Defense-7 Defense non Lead capped and refuted plans create no stronger claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val leftMove = Line(Square('d', 4), Square('e', 4))
    val rightMove = Line(Square('d', 4), Square('c', 4))
    val leftDefense = SceneDefense.write(facts, threatLine, leftMove).get
    val rightDefense = SceneDefense.write(facts, threatLine, rightMove).get.copy(proof = leftDefense.proof)
    val verdicts = StoryTable.choose(Vector(leftDefense, rightDefense))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false)
    val cappedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftDefense),
      engineLine = Some(EngineLine(Vector(leftMove))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val refuteCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftDefense),
      engineLine = Some(EngineLine(Vector(leftMove))),
      replyLine = Some(EngineLine(Vector(threatLine))),
      evalBefore = Some(EngineEval(100)),
      evalAfter = Some(EngineEval(-100)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val cappedPlan =
      ExplanationPlan
        .fromSelected(
          StoryTable.choose(Vector(SceneDefense.withEngineCheck(leftDefense, cappedCheck).get)).head
        )
        .get
    val refutedPlan =
      ExplanationPlan
        .fromSelected(
          StoryTable.choose(Vector(SceneDefense.withEngineCheck(leftDefense, refuteCheck).get)).head
        )
        .get
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get

    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    Vector(supportPlan, contextPlan, blockedPlan, cappedPlan, refutedPlan).foreach: plan =>
      assertEquals(plan.allowedClaim, None)
    assertEquals(supportPlan.relations, Vector(ExplanationRelation.SameFamilyLowerRank))
    assertEquals(contextPlan.relations, Vector.empty)
    assertEquals(blockedPlan.debugOnly, true)
    assertEquals(cappedPlan.forbiddenWording.contains(ForbiddenWording.StrongWording), true)
    assertEquals(refutedPlan.relations, Vector(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(refutedPlan.debugOnly, true)

  test("Defense-8 DeterministicRenderer phrases Defense ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val materialLossPlan = plan.copy(allowedClaim = Some(ExplanationClaim.PreventsMaterialLoss))
    val materialLossRendered = DeterministicRenderer.fromPlan(materialLossPlan).get
    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods
        .filter(_.getName == "fromPlan")
        .map(_.getParameterTypes.toVector.map(_.getSimpleName))
        .toVector

    assertEquals(rendererMethods, Vector(Vector("ExplanationPlan")))
    assertEquals(rendered.text, "Qe4+ defends the piece on d4.")
    assertEquals(rendered.claimKey, "defends_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    assertEquals(materialLossRendered.text, "Qe4+ prevents the piece on d4 from being lost immediately.")
    assertEquals(materialLossRendered.claimKey, "prevents_material_loss")
    Vector(
      "only move",
      "best move",
      "refutes the attack",
      "stops all counterplay",
      "solves the position",
      "king is safe",
      "mate is stopped",
      "winning",
      "decisive",
      "forced"
    ).foreach: forbidden =>
      assert(!rendered.text.toLowerCase.contains(forbidden))
      assert(!materialLossRendered.text.toLowerCase.contains(forbidden))

  test("Defense-8 Renderer refuses Defense text without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val alternateDefenseLine = Line(Square('d', 4), Square('c', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val alternateDefense =
      SceneDefense.write(facts, threatLine, alternateDefenseLine).get.copy(proof = defense.proof)
    val verdicts = StoryTable.choose(Vector(defense, alternateDefense))
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val blockedPlan = leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val cappedPlan = leadPlan.copy(
      allowedClaim = None,
      forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.StrongWording
    )
    val wrongScenePlan = leadPlan.copy(scene = Scene.Material)
    val wrongClaimPlan = leadPlan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges))
    val strongerRoutePlan = leadPlan.copy(routeSan = Some("Qe4 wins material"))

    Vector(supportPlan, blockedPlan, cappedPlan, wrongScenePlan, wrongClaimPlan, strongerRoutePlan).foreach:
      plan => assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Defense-9 LLM smoke accepts Defense RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val smokeMethods =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
    val smokeParameterNames =
      smokeMethods.flatMap(_.getParameterTypes.toVector).map(_.getName).mkString(" ")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assert(prompt.contains("renderedText: Qe4+ defends the piece on d4."))
    assert(prompt.contains("claimKey: defends_piece"))
    assert(prompt.contains("strength: bounded"))
    assert(prompt.contains("instruction: Rephrase only. Do not add chess facts."))
    Vector(
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "EngineEval",
      "EngineLine",
      "Story",
      "Verdict"
    ).foreach: forbiddenType =>
      assert(!smokeParameterNames.contains(forbiddenType), s"LLM smoke must not accept $forbiddenType")
    Vector(
      "raw Verdict",
      "Story",
      "ThreatProof",
      "DefenseProof",
      "EngineCheck",
      "BoardFacts",
      "engine eval",
      "raw PV",
      "proofFailures"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), forbidden)

  test("Defense-9 LLM smoke rejects stronger Defense rephrases"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(defense)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val cases = Vector(
      "Qe4+ is the only move to defend the piece on d4." -> "forbidden_wording",
      "Qe4+ is the best move to defend the piece on d4." -> "forbidden_wording",
      "Qe4+ gives White no counterplay problems." -> "forbidden_wording",
      "Qe4+ keeps the king safe while defending d4." -> "forbidden_wording",
      "Qe4+ stops mate and defends d4." -> "forbidden_wording",
      "Qe4+ refutes the attack on d4." -> "forbidden_wording",
      "The engine says Qe4+ defends d4." -> "engine_mention",
      "Qe4+ defends d4 and Nc6 is next." -> "new_move_or_line",
      "Qe4+ defends d4 with a fork." -> "new_tactic_or_plan",
      "Qe4+ starts a plan to defend d4." -> "new_tactic_or_plan",
      "Qe4+ wins material by defending d4." -> "stronger_claim"
    )

    cases.foreach: (output, violation) =>
      val check = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(check.accepted, false, output)
      assert(
        check.violations.contains(violation),
        s"$output should include $violation, got ${check.violations}"
      )

  test("Defense slice closeout keeps proof homes separate and unopened meanings silent"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val threat = ThreatProof.fromBoardFacts(facts, threatLine)
    val defenseProof = DefenseProof.fromBoardFacts(facts, threat, defenseLine)
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(threat.publicClaimAllowed, false)
    assertEquals(defenseProof.publicClaimAllowed, false)
    assertEquals(threat.complete, true)
    assertEquals(defenseProof.complete, true)
    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.threatProof.exists(_.complete), true)
    assertEquals(story.defenseProof.exists(_.complete), true)
    assertEquals(story.proof.kingHeat, 0)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))
    Vector(
      "only move",
      "best move",
      "no counterplay",
      "king safety",
      "mate defense",
      "refutes",
      "strategy",
      "prophylaxis",
      "winning",
      "decisive",
      "forced"
    ).foreach: forbidden =>
      assert(!rendered.text.toLowerCase.contains(forbidden), forbidden)

  test("Defense slice closeout real game smoke accepts attacked bishop defense"):
    // Fischer-Spassky, World Championship 1972 game 6 after 1.c4 e6 2.Nf3 d5 3.d4 Nf6 4.Nc3 Be7 5.Bg5 O-O 6.e3 h6.
    val facts =
      BoardFacts.fromFen("rnbq1rk1/ppp1bpp1/4pn1p/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 7").toOption.get
    val threatLine = Line(Square('h', 6), Square('g', 5))
    val defenseLine = Line(Square('g', 5), Square('h', 4))
    val story = SceneDefense.write(facts, threatLine, defenseLine).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.Defense)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.threatProof.exists(_.complete), true)
    assertEquals(story.defenseProof.exists(_.complete), true)
    assertEquals(
      story.defenseProof.flatMap(_.afterDefenseTargetStatus),
      Some(DefenseTargetStatus.TargetMovedAway)
    )
    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertEquals(rendered.text, "Bh4 defends the piece on g5.")
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )

  test(
    "Middlegame interaction hardening lets same-board Material outrank Defense when the move changes material now"
  ):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val captureDefense = Line(Square('e', 4), Square('f', 5))
    val defense = SceneDefense.write(facts, threatLine, captureDefense).get
    val material = SceneMaterial.write(facts, captureDefense).get
    val forward = StoryTable.choose(Vector(defense, material))
    val reverse = StoryTable.choose(Vector(material, defense))

    assertEquals(
      defense.defenseProof.flatMap(_.afterDefenseTargetStatus),
      Some(DefenseTargetStatus.AttackerCaptured)
    )
    assertEquals(material.captureResult.exists(_.positiveMaterial), true)
    assertEquals(forward.map(v => (v.story.scene, v.role)), reverse.map(v => (v.story.scene, v.role)))
    assertEquals(forward.head.story.scene, Scene.Material)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward(1).story.scene, Scene.Defense)
    assertEquals(forward(1).role, Role.Support)

  test("Middlegame interaction hardening keeps immediate Defense lead separate from speculative future loss"):
    val facts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val threatLine = Line(Square('f', 5), Square('d', 4))
    val defenseLine = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(facts, threatLine, defenseLine).get
    val speculativeFutureLoss =
      Story(
        scene = Scene.Defense,
        side = Side.White,
        target = Some(Square('d', 4)),
        anchor = Some(Square('d', 4)),
        route = Some(defenseLine),
        routeSan = Some("Qe4"),
        rival = Side.Black,
        proof = proof(
          boardProof = 100,
          lineProof = 100,
          ownerProof = 100,
          anchorProof = 100,
          routeProof = 100,
          forcing = 100,
          conversionPrize = 100,
          counterplayRisk = 0,
          kingHeat = 0,
          pieceSupport = 100,
          clarity = 100
        ),
        storyProof = StoryProof.fromBoardFacts(facts, defenseLine),
        writer = Some(StoryWriter.SceneDefense)
      )
    val verdicts = StoryTable.choose(Vector(speculativeFutureLoss, defense))

    assertEquals(verdicts.head.story, defense)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts(1).story, speculativeFutureLoss)
    assertEquals(verdicts(1).role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(verdicts(1)).flatMap(DeterministicRenderer.fromPlan), None)

  test("Middlegame interaction hardening covers DefenseProof move away guard block and capture cases"):
    val movedFacts =
      BoardFacts.fromFen("rnbq1rk1/ppp1bpp1/4pn1p/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 7").toOption.get
    val moved = SceneDefense
      .write(
        movedFacts,
        Line(Square('h', 6), Square('g', 5)),
        Line(Square('g', 5), Square('h', 4))
      )
      .get
    val guardedFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/7B/4K3 w - - 0 1").toOption.get
    val guarded = SceneDefense
      .write(
        guardedFacts,
        Line(Square('d', 5), Square('d', 4)),
        Line(Square('h', 2), Square('e', 5))
      )
      .get
    val blockedFacts = BoardFacts.fromFen("3qk3/8/8/8/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val blocked = SceneDefense
      .write(
        blockedFacts,
        Line(Square('d', 8), Square('d', 4)),
        Line(Square('e', 4), Square('d', 5))
      )
      .get
    val capturedFacts = BoardFacts.fromFen("4k3/8/8/5n2/3RB3/8/8/4K3 w - - 0 1").toOption.get
    val captured = SceneDefense
      .write(
        capturedFacts,
        Line(Square('f', 5), Square('d', 4)),
        Line(Square('e', 4), Square('f', 5))
      )
      .get
    val cases = Vector(
      DefenseTargetStatus.TargetMovedAway -> moved,
      DefenseTargetStatus.TargetGuarded -> guarded,
      DefenseTargetStatus.AttackerLineBlocked -> blocked,
      DefenseTargetStatus.AttackerCaptured -> captured
    )

    cases.foreach: (status, story) =>
      assertEquals(story.defenseProof.flatMap(_.afterDefenseTargetStatus), Some(status), status.toString)
      assertEquals(StoryTable.choose(Vector(story)).head.role, Role.Lead, status.toString)

  test("Middlegame interaction hardening keeps open Story families deterministically ordered"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defense = SceneDefense
      .write(
        defenseFacts,
        Line(Square('f', 5), Square('d', 4)),
        Line(Square('d', 4), Square('e', 4))
      )
      .get
    val matrix = Vector(
      "Material vs Defense" -> Vector(material, defense),
      "Hanging vs Defense" -> Vector(hanging, defense),
      "Fork vs Defense" -> Vector(fork, defense),
      "Hanging vs Material" -> Vector(hanging, material),
      "Hanging vs Fork" -> Vector(hanging, fork)
    )

    matrix.foreach: (label, rows) =>
      val forward = StoryTable.choose(rows)
      val reverse = StoryTable.choose(rows.reverse)
      assertEquals(
        forward.map(v => (v.story.scene, v.story.tactic, v.story.route, v.role)),
        reverse.map(v => (v.story.scene, v.story.tactic, v.story.route, v.role)),
        label
      )
      assertEquals(forward.count(_.role == Role.Lead), 1, label)
      assert(
        forward.forall(v => Set(Role.Lead, Role.Support, Role.Context, Role.Blocked).contains(v.role)),
        label
      )
      forward
        .filter(_.role != Role.Lead)
        .foreach: verdict =>
          assertEquals(
            ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
            None,
            label
          )

  test("Middlegame interaction hardening applies EngineCheck Supports Caps and Refutes across open rows"):
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(materialFacts, materialMove).get
    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val hanging = TacticHanging.write(materialFacts, materialMove).get

    def check(
        facts: BoardFacts,
        story: Story,
        reply: Line,
        status: EngineCheckStatus,
        before: Int = 20,
        after: Int = 20
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = story.route.map(route => EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val rows = Vector(
      (materialFacts, material, materialMove, SceneMaterial.withEngineCheck),
      (materialFacts, hanging, materialMove, TacticHanging.withEngineCheck),
      (forkFacts, fork, forkMove, TacticFork.withEngineCheck),
      (defenseFacts, defense, defenseThreat, SceneDefense.withEngineCheck)
    )

    rows.foreach: (facts, story, reply, attach) =>
      val supports = attach(story, check(facts, story, reply, EngineCheckStatus.Supports)).get
      val caps = attach(story, check(facts, story, reply, EngineCheckStatus.Caps)).get
      val refutes =
        attach(story, check(facts, story, reply, EngineCheckStatus.Supports, before = 200, after = 0)).get
      val supportsVerdict = StoryTable.choose(Vector(supports)).head
      val capsVerdict = StoryTable.choose(Vector(caps)).head
      val refutesVerdict = StoryTable.choose(Vector(refutes)).head

      assertEquals(supportsVerdict.role, Role.Lead, story.toString)
      assertEquals(supportsVerdict.engineStrengthLimited, false, story.toString)
      assertEquals(capsVerdict.role, Role.Lead, story.toString)
      assertEquals(capsVerdict.engineStrengthLimited, true, story.toString)
      assertEquals(refutesVerdict.role, Role.Blocked, story.toString)

  test("MIH-1 fixture map covers every allowed middlegame hardening category"):
    final case class MihFixture(
        category: String,
        fen: String,
        sideToMove: Side,
        legalCandidateLines: Vector[Line],
        expectedOpenRows: Vector[String],
        expectedBlockedRows: Vector[String],
        expectedRoles: Vector[(String, Role)],
        expectedSelectedVerdict: String,
        forbiddenClaims: Vector[String],
        rows: BoardFacts => Vector[(String, Story)]
    )

    val hanging = "Tactic.Hanging"
    val fork = "Tactic.Fork"
    val material = "Scene.Material"
    val defense = "Scene.Defense"
    val materialSupports = "Scene.Material#EngineCheck.Supports"
    val materialCaps = "Scene.Material#EngineCheck.Caps"
    val materialRefutes = "Scene.Material#EngineCheck.Refutes"

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

    def checkedMaterial(
        facts: BoardFacts,
        story: Story,
        status: EngineCheckStatus,
        before: Int,
        after: Int
    ): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('g', 8), Square('f', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      SceneMaterial.withEngineCheck(story, check).get

    val queenTakesH6 = Line(Square('d', 2), Square('h', 6))
    val knightFork = Line(Square('f', 3), Square('d', 4))
    val materialQueenTakesE5 = Line(Square('d', 4), Square('e', 5))
    val pawnThreat = Line(Square('h', 6), Square('g', 5))
    val bishopStepsAway = Line(Square('g', 5), Square('h', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))

    val sharedHangingForkFen = "6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1"
    val materialDefenseFen = "6k1/8/7p/4n1B1/3Q4/8/8/6K1 w - - 0 1"
    val forkDefenseFen = "7k/8/7p/1q3rB1/8/5N2/8/7K w - - 0 1"
    val sameBoardMaterialDefenseFen = "4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1"

    val defaultForbiddenClaims =
      Vector(
        "winning",
        "decisive",
        "forced",
        "conversion",
        "new_story_family",
        "new_proof_home",
        "new_renderer_wording",
        "public_route_200",
        "production_api",
        "public_llm_narration"
      )

    val fixtureMap = Vector(
      MihFixture(
        category = "Hanging vs Material",
        fen = sharedHangingForkFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(queenTakesH6),
        expectedOpenRows = Vector(hanging, material),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(hanging -> Role.Lead, material -> Role.Support),
        expectedSelectedVerdict = hanging,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            hanging -> TacticHanging.write(facts, queenTakesH6).get,
            material -> SceneMaterial.write(facts, queenTakesH6).get.copy(proof = score(99))
          )
      ),
      MihFixture(
        category = "Hanging vs Fork",
        fen = sharedHangingForkFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(queenTakesH6, knightFork),
        expectedOpenRows = Vector(hanging, fork),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(hanging -> Role.Lead, fork -> Role.Support),
        expectedSelectedVerdict = hanging,
        forbiddenClaims = defaultForbiddenClaims :+ "wins_queen",
        rows = facts =>
          Vector(
            hanging -> TacticHanging.write(facts, queenTakesH6).get.copy(proof = score(99)),
            fork -> TacticFork.write(facts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5))).get
          )
      ),
      MihFixture(
        category = "Material vs Defense",
        fen = materialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(materialQueenTakesE5, pawnThreat, bishopStepsAway),
        expectedOpenRows = Vector(defense, material),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(material -> Role.Lead, defense -> Role.Support),
        expectedSelectedVerdict = material,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            material -> SceneMaterial.write(facts, materialQueenTakesE5).get,
            defense -> SceneDefense.write(facts, pawnThreat, bishopStepsAway).get
          )
      ),
      MihFixture(
        category = "Fork vs Defense",
        fen = forkDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(knightFork, pawnThreat, bishopStepsAway),
        expectedOpenRows = Vector(defense, fork),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(defense -> Role.Lead, fork -> Role.Support),
        expectedSelectedVerdict = defense,
        forbiddenClaims = defaultForbiddenClaims :+ "wins_queen",
        rows = facts =>
          Vector(
            fork -> TacticFork.write(facts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5))).get,
            defense -> SceneDefense.write(facts, pawnThreat, bishopStepsAway).get
          )
      ),
      MihFixture(
        category = "Material vs Defense on same board",
        fen = sameBoardMaterialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(knightThreatensRook, bishopTakesAttacker),
        expectedOpenRows = Vector(material, defense),
        expectedBlockedRows = Vector.empty,
        expectedRoles = Vector(material -> Role.Lead, defense -> Role.Support),
        expectedSelectedVerdict = material,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          Vector(
            defense -> SceneDefense.write(facts, knightThreatensRook, bishopTakesAttacker).get,
            material -> SceneMaterial.write(facts, bishopTakesAttacker).get
          )
      ),
      MihFixture(
        category = "EngineCheck Supports/Caps/Refutes over existing rows",
        fen = materialDefenseFen,
        sideToMove = Side.White,
        legalCandidateLines = Vector(materialQueenTakesE5),
        expectedOpenRows = Vector(materialSupports, materialCaps),
        expectedBlockedRows = Vector(materialRefutes),
        expectedRoles = Vector(
          materialSupports -> Role.Lead,
          materialCaps -> Role.Support,
          materialRefutes -> Role.Blocked
        ),
        expectedSelectedVerdict = materialSupports,
        forbiddenClaims = defaultForbiddenClaims,
        rows = facts =>
          val base = SceneMaterial.write(facts, materialQueenTakesE5).get
          Vector(
            materialSupports ->
              checkedMaterial(facts, base, EngineCheckStatus.Supports, before = 20, after = 80)
                .copy(proof = score(99)),
            materialCaps ->
              checkedMaterial(facts, base, EngineCheckStatus.Caps, before = 20, after = 80)
                .copy(proof = score(98)),
            materialRefutes ->
              checkedMaterial(facts, base, EngineCheckStatus.Refutes, before = 20, after = 80)
                .copy(proof = score(100))
          )
      )
    )

    assertEquals(
      fixtureMap.map(_.category),
      Vector(
        "Hanging vs Material",
        "Hanging vs Fork",
        "Material vs Defense",
        "Fork vs Defense",
        "Material vs Defense on same board",
        "EngineCheck Supports/Caps/Refutes over existing rows"
      )
    )

    val allowedRows: Set[(Scene, Option[Tactic])] =
      Set(
        (Scene.Tactic, Some(Tactic.Hanging)),
        (Scene.Tactic, Some(Tactic.Fork)),
        (Scene.Material, None),
        (Scene.Defense, None)
      )
    val forbiddenExpectationTerms =
      Vector("pressure", "initiative", "best move", "only move", "proofFailures")

    fixtureMap.foreach: fixture =>
      val facts = BoardFacts.fromFen(fixture.fen).toOption.get
      val rows = fixture.rows(facts)
      val verdicts = StoryTable.choose(rows.map(_._2))
      val rowIdByStory = rows.map((id, story) => story -> id).toMap
      val roleByRow = verdicts.map(verdict => rowIdByStory(verdict.story) -> verdict.role)
      val openRows = roleByRow.collect { case (id, role) if role != Role.Blocked => id }
      val blockedRows = roleByRow.collect { case (id, Role.Blocked) => id }
      val fixtureText =
        (Vector(fixture.category) ++ fixture.expectedOpenRows ++ fixture.expectedBlockedRows ++
          fixture.expectedRoles.map(_._1) ++ Vector(
            fixture.expectedSelectedVerdict
          ) ++ fixture.forbiddenClaims)
          .mkString(" ")
          .toLowerCase

      assertEquals(facts.sideToMove, fixture.sideToMove, fixture.category)
      fixture.legalCandidateLines.foreach: line =>
        assert(
          facts.sideLegal.lines.contains(line) || facts.rivalLegal.lines.contains(line),
          s"${fixture.category} candidate line must be legal: $line"
        )
      rows.foreach: (_, story) =>
        assert(
          allowedRows.contains((story.scene, story.tactic)),
          s"${fixture.category} must not imply unopened Story family: ${story.scene}/${story.tactic}"
        )
        assertEquals(story.proofFailures, Vector.empty, fixture.category)
      assertEquals(openRows.toSet, fixture.expectedOpenRows.toSet, fixture.category)
      assertEquals(blockedRows.toSet, fixture.expectedBlockedRows.toSet, fixture.category)
      assertEquals(roleByRow, fixture.expectedRoles, fixture.category)
      assertEquals(rowIdByStory(verdicts.head.story), fixture.expectedSelectedVerdict, fixture.category)
      assertEquals(verdicts.head.role, Role.Lead, fixture.category)
      assert(fixture.forbiddenClaims.nonEmpty, fixture.category)
      forbiddenExpectationTerms.foreach: term =>
        assert(!fixtureText.contains(term), s"${fixture.category} must not expect forbidden term: $term")

      val selectedPlan = ExplanationPlan.fromSelected(verdicts.head).get
      val rendered = DeterministicRenderer.fromPlan(selectedPlan).get
      val mockText = LlmNarrationSmoke.mockNarrate(selectedPlan, rendered).get
      assert(LlmNarrationSmoke.codexCliPrompt(selectedPlan, rendered).nonEmpty, fixture.category)
      val publicSmokeText = Vector(rendered.text, mockText).mkString(" ").toLowerCase
      fixture.forbiddenClaims.foreach: claim =>
        val normalizedClaim = claim.replace('_', ' ').toLowerCase
        assert(
          !publicSmokeText.contains(normalizedClaim),
          s"${fixture.category} output must not contain forbidden claim: $claim"
        )
      verdicts
        .filterNot(_ == verdicts.head)
        .foreach: verdict =>
          assertEquals(
            ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan),
            None,
            fixture.category
          )

  test("MIH-2 Role Stability keeps existing rows deterministic without duplicate Lead"):
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

    def checkedMaterial(facts: BoardFacts, story: Story, status: EngineCheckStatus): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('g', 8), Square('f', 8))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      SceneMaterial.withEngineCheck(story, check).get

    def rowId(rows: Vector[(String, Story)], story: Story): String =
      rows.collectFirst { case (id, row) if row == story => id }.get

    def roleById(rows: Vector[(String, Story)], verdicts: Vector[Verdict]): Map[String, Role] =
      verdicts.map(verdict => rowId(rows, verdict.story) -> verdict.role).toMap

    def roleShape(rows: Vector[(String, Story)], verdicts: Vector[Verdict]) =
      verdicts.map: verdict =>
        (
          rowId(rows, verdict.story),
          verdict.role,
          verdict.leadAllowed,
          verdict.engineCheckStatus,
          verdict.engineStrengthLimited,
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim)
        )

    val hangingMaterialFacts =
      BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val queenTakesH6 = Line(Square('d', 2), Square('h', 6))
    val knightFork = Line(Square('f', 3), Square('d', 4))
    val hanging = TacticHanging.write(hangingMaterialFacts, queenTakesH6).get.copy(proof = strongProof(96))
    val materialSameRoute =
      SceneMaterial.write(hangingMaterialFacts, queenTakesH6).get.copy(proof = strongProof(99))
    val fork = TacticFork
      .write(hangingMaterialFacts, Some(knightFork), Some(Square('b', 5)), Some(Square('f', 5)))
      .get
      .copy(proof = strongProof(94))

    val refutedMaterial =
      checkedMaterial(hangingMaterialFacts, materialSameRoute, EngineCheckStatus.Refutes)
        .copy(proof = strongProof(100))
    val incompleteMaterial = materialSameRoute.copy(storyProof = StoryProof.empty, proof = strongProof(100))
    val cappedMaterial =
      checkedMaterial(hangingMaterialFacts, materialSameRoute, EngineCheckStatus.Caps)
        .copy(proof = strongProof(98))

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val validDefense = SceneDefense.write(defenseFacts, knightThreatensRook, bishopTakesAttacker).get
    val defenseWithoutThreat = validDefense.copy(threatProof = None, proof = strongProof(100))
    val forkWithoutTwoTargetProof = fork.copy(multiTargetProof = None, proof = strongProof(100))

    val roleStabilityRows =
      Vector(
        "Tactic.Hanging" -> hanging,
        "Scene.Material.sameRoute" -> materialSameRoute,
        "Tactic.Fork" -> fork,
        "Scene.Material.refuted" -> refutedMaterial,
        "Scene.Material.incomplete" -> incompleteMaterial,
        "Scene.Defense.noThreat" -> defenseWithoutThreat
      )
    val forward = StoryTable.choose(roleStabilityRows.map(_._2))
    val reverse = StoryTable.choose(roleStabilityRows.reverse.map(_._2))
    val shuffled =
      StoryTable.choose(
        Vector(
          roleStabilityRows(3),
          roleStabilityRows(1),
          roleStabilityRows(5),
          roleStabilityRows(0),
          roleStabilityRows(2),
          roleStabilityRows(4)
        ).map(_._2)
      )

    assertEquals(roleShape(roleStabilityRows, forward), roleShape(roleStabilityRows, reverse))
    assertEquals(roleShape(roleStabilityRows, forward), roleShape(roleStabilityRows, shuffled))
    assertEquals(rowId(roleStabilityRows, forward.head.story), "Tactic.Hanging")
    assertEquals(rowId(roleStabilityRows, reverse.head.story), "Tactic.Hanging")
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.sameRoute"), Role.Support)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.incomplete"), Role.Blocked)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Material.refuted"), Role.Blocked)
    assertEquals(roleById(roleStabilityRows, forward)("Scene.Defense.noThreat"), Role.Blocked)

    val sameRouteVerdicts = StoryTable.choose(Vector(hanging, materialSameRoute))
    assertEquals(sameRouteVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(sameRouteVerdicts.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(sameRouteVerdicts.find(_.story == materialSameRoute).map(_.role), Some(Role.Support))

    val defenseWithoutThreatVerdict = StoryTable.choose(Vector(defenseWithoutThreat)).head
    assertEquals(defenseWithoutThreatVerdict.role, Role.Blocked)
    assertEquals(defenseWithoutThreatVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(defenseWithoutThreatVerdict).flatMap(_.allowedClaim), None)
    assertEquals(
      ExplanationPlan.fromSelected(defenseWithoutThreatVerdict).flatMap(DeterministicRenderer.fromPlan),
      None
    )

    val forkWithoutTwoTargetVerdict = StoryTable.choose(Vector(forkWithoutTwoTargetProof)).head
    assertEquals(forkWithoutTwoTargetVerdict.role, Role.Blocked)
    assertEquals(forkWithoutTwoTargetVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(forkWithoutTwoTargetVerdict), None)

    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)

  test("MIH-3 Material vs Defense collision separates material change now from prevented loss"):
    def publicSmokeText(plan: ExplanationPlan, rendered: RenderedLine): String =
      Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get)
        .mkString(" ")
        .toLowerCase

    def assertNoForbidden(text: String, forbidden: Vector[String], label: String): Unit =
      forbidden.foreach: phrase =>
        assert(!text.contains(phrase), s"$label must not contain forbidden phrase: $phrase")

    val collisionFacts =
      BoardFacts.fromFen("6k1/8/7p/4n1B1/3Q4/8/8/6K1 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val threatLine = Line(Square('h', 6), Square('g', 5))
    val defenseMove = Line(Square('g', 5), Square('h', 4))
    val material = SceneMaterial.write(collisionFacts, materialMove).get
    val defense = SceneDefense.write(collisionFacts, threatLine, defenseMove).get
    val forward = StoryTable.choose(Vector(defense, material))
    val reverse = StoryTable.choose(Vector(material, defense))

    assertEquals(material.captureResult.exists(_.positiveMaterial), true)
    assertEquals(defense.threatProof.exists(_.complete), true)
    assertEquals(defense.defenseProof.exists(_.complete), true)
    assertEquals(
      defense.defenseProof.flatMap(_.afterDefenseTargetStatus),
      Some(DefenseTargetStatus.TargetMovedAway)
    )
    assertEquals(
      forward.map(verdict => (verdict.story.scene, verdict.story.route, verdict.role)),
      reverse.map(verdict => (verdict.story.scene, verdict.story.route, verdict.role))
    )
    assertEquals(forward.head.story, material)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.find(_.story == defense).map(_.role), Some(Role.Support))
    assertEquals(
      ExplanationPlan
        .fromSelected(forward.find(_.story == defense).get)
        .flatMap(DeterministicRenderer.fromPlan),
      None
    )

    val materialPlan = ExplanationPlan.fromSelected(forward.head).get
    val materialRendered = DeterministicRenderer.fromPlan(materialPlan).get
    assertEquals(materialPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertNoForbidden(
      publicSmokeText(materialPlan, materialRendered),
      Vector("winning", "conversion", "decisive"),
      "Material"
    )
    assertEquals(
      LlmNarrationSmoke
        .check(materialPlan, materialRendered, "Qxe5 is winning and decisive conversion.")
        .accepted,
      false
    )

    val defenseOnlyVerdict = StoryTable.choose(Vector(defense)).head
    val defensePlan = ExplanationPlan.fromSelected(defenseOnlyVerdict).get
    val defenseRendered = DeterministicRenderer.fromPlan(defensePlan).get
    assertEquals(defenseOnlyVerdict.role, Role.Lead)
    assertEquals(defensePlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))
    assertNoForbidden(
      publicSmokeText(defensePlan, defenseRendered),
      Vector("best defense", "only move", "refutes attack"),
      "Defense"
    )
    assertEquals(
      LlmNarrationSmoke
        .check(defensePlan, defenseRendered, "Bh4 is the only move and refutes the attack.")
        .accepted,
      false
    )

    val speculativeFutureLoss = defense.copy(threatProof = None, defenseProof = None)
    val speculativeVerdict = StoryTable.choose(Vector(speculativeFutureLoss)).head
    assertEquals(speculativeVerdict.role, Role.Blocked)
    assertEquals(speculativeVerdict.leadAllowed, false)
    assertEquals(ExplanationPlan.fromSelected(speculativeVerdict).flatMap(_.allowedClaim), None)
    assertEquals(
      ExplanationPlan.fromSelected(speculativeVerdict).flatMap(DeterministicRenderer.fromPlan),
      None
    )

  test("MIH-4 EngineCheck interaction reuses status boundaries without public engine meaning"):
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

    def publicSmokeText(plan: ExplanationPlan, rendered: RenderedLine): String =
      Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get)
        .mkString(" ")
        .toLowerCase

    def assertNoEngineExpression(text: String, label: String): Unit =
      Vector("engine", "eval", "pv", "engine says", "best move", "1234", "1478", "d7").foreach: phrase =>
        assert(!text.contains(phrase), s"$label must not expose engine phrase or raw evidence: $phrase")

    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, materialMove).get

    def check(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get, Line(Square('e', 8), Square('d', 7))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(1234)),
        evalAfter = Some(EngineEval(1478)),
        depth = Some(19),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val baseVerdict = StoryTable.choose(Vector(material)).head
    val supports = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Caps)).get
    val refutes = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Refutes)).get
    val unknown = SceneMaterial.withEngineCheck(material, check(material, EngineCheckStatus.Unknown)).get

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head

    val basePlan = ExplanationPlan.fromSelected(baseVerdict).get
    val supportsPlan = ExplanationPlan.fromSelected(supportsVerdict).get
    val capsPlan = ExplanationPlan.fromSelected(capsVerdict).get
    val refutesPlan = ExplanationPlan.fromSelected(refutesVerdict).get
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get

    Vector(supportsVerdict, capsVerdict, refutesVerdict, unknownVerdict).foreach: verdict =>
      assert(!verdict.values.contains(1234.0), "evalBefore must stay out of public Verdict values")
      assert(!verdict.values.contains(1478.0), "evalAfter must stay out of public Verdict values")

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(supportsPlan.allowedClaim, basePlan.allowedClaim)
    assertEquals(supportsPlan.relations.exists(_.key.contains("engine")), false)
    val supportsRendered = DeterministicRenderer.fromPlan(supportsPlan).get
    assertNoEngineExpression(publicSmokeText(supportsPlan, supportsRendered), "Supports")
    val supportsPrompt = LlmNarrationSmoke.codexCliPrompt(supportsPlan, supportsRendered).get
    Vector("1234", "1478", "d7").foreach: rawEvidence =>
      assert(
        !supportsPrompt.toLowerCase.contains(rawEvidence),
        s"Supports prompt must not expose raw evidence: $rawEvidence"
      )
    assertEquals(
      LlmNarrationSmoke
        .check(supportsPlan, supportsRendered, "The engine says Qxe5 is the best move at +14.78.")
        .accepted,
      false
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(capsPlan.allowedClaim, None)
    assert(capsPlan.forbiddenWording.contains(ForbiddenWording.StrongWording))
    assertEquals(DeterministicRenderer.fromPlan(capsPlan), None)

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesPlan.allowedClaim, None)
    assertEquals(refutesPlan.debugOnly, true)
    assert(refutesPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(DeterministicRenderer.fromPlan(refutesPlan), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(unknownPlan.allowedClaim, basePlan.allowedClaim)
    val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get
    assertNoEngineExpression(publicSmokeText(unknownPlan, unknownRendered), "Unknown")
    assertEquals(
      LlmNarrationSmoke
        .check(unknownPlan, unknownRendered, "The engine says Qxe5 is the best move at +14.78.")
        .accepted,
      false
    )

    val orderingFacts = facts
    val highProofLowEval = material.copy(proof = score(96))
    val lowProofHighEval = material.copy(proof = score(72))

    def orderingCheck(story: Story, after: Int): EngineCheck =
      EngineCheck.fromStory(
        facts = orderingFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(0)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(19),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    val lowEvalRow = SceneMaterial.withEngineCheck(highProofLowEval, orderingCheck(highProofLowEval, 1)).get
    val highEvalRow =
      SceneMaterial.withEngineCheck(lowProofHighEval, orderingCheck(lowProofHighEval, 3000)).get
    val ordered = StoryTable.choose(Vector(highEvalRow, lowEvalRow))
    assertEquals(ordered.head.story, lowEvalRow)
    assertEquals(ordered.head.role, Role.Lead)

  test("MIH-5 Negative Corpus keeps close false positives silent on complex boards"):
    def assertNoPublicOutput(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    val attackedButRecapturedFacts =
      BoardFacts.fromFen("6k1/ppp2ppp/8/3q4/4Qn2/2N2B2/PPP2PPP/6K1 w - - 0 1").toOption.get
    val defendedQueenCapture = Line(Square('e', 4), Square('d', 5))
    val attackedButRecaptured = TacticHanging.write(attackedButRecapturedFacts, defendedQueenCapture)
    val equalMaterialResult = CaptureResult.fromBoardFacts(attackedButRecapturedFacts, defendedQueenCapture)

    val materialLostAfterReplyFacts =
      BoardFacts.fromFen("6k1/ppp2ppp/8/3p4/4Qn2/2N2B2/PPP2PPP/6K1 w - - 0 1").toOption.get
    val queenTakesDefendedPawn = Line(Square('e', 4), Square('d', 5))
    val lostAfterReply = SceneMaterial.write(materialLostAfterReplyFacts, queenTakesDefendedPawn)
    val lostMaterialResult = CaptureResult.fromBoardFacts(materialLostAfterReplyFacts, queenTakesDefendedPawn)

    val forkLookingFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get
    val forkLookingMove = Line(Square('f', 3), Square('d', 4))
    val oneRealTarget = TacticFork.write(forkLookingFacts, Some(forkLookingMove), Some(Square('b', 5)), None)

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val knightThreatensRook = Line(Square('f', 5), Square('d', 4))
    val bishopTakesAttacker = Line(Square('e', 4), Square('f', 5))
    val noCompleteThreat =
      SceneDefense.write(defenseFacts, Line(Square('f', 5), Square('h', 4)), bishopTakesAttacker)
    val stillLeavesLoss =
      SceneDefense.write(defenseFacts, knightThreatensRook, Line(Square('d', 4), Square('h', 4)))

    val wrongTargetFacts = BoardFacts.fromFen("4k3/8/8/3q4/3R4/8/B6B/4K3 w - - 0 1").toOption.get
    val wrongTargetGuard =
      SceneDefense.write(
        wrongTargetFacts,
        Line(Square('d', 5), Square('d', 4)),
        Line(Square('a', 2), Square('b', 3))
      )

    Vector(
      "attacked-looking piece but adequate recapture exists" -> attackedButRecaptured,
      "fork-looking move but only one real target" -> oneRealTarget,
      "material-looking capture but equal/lost after immediate reply" -> lostAfterReply,
      "defense-looking move but no complete ThreatProof" -> noCompleteThreat,
      "defense move guards wrong target" -> wrongTargetGuard,
      "defense move still leaves material loss" -> stillLeavesLoss
    ).foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, label)

    assertEquals(equalMaterialResult.materialResult, Some(0))
    assertEquals(lostMaterialResult.materialResult.exists(_ < 0), true)

    val plausibleMaterial = SceneMaterial.write(defenseFacts, bishopTakesAttacker).get
    val refutingCheck = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(bishopTakesAttacker))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(200)),
      evalAfter = Some(EngineEval(0)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refutedPlausible = SceneMaterial.withEngineCheck(plausibleMaterial, refutingCheck).get
    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertNoPublicOutput("engine refutes otherwise plausible Story", refutedPlausible)

    val sameBoardProofMissing =
      minimalBoardFacts(
        sideLegal = readyMoves(line = bishopTakesAttacker, captureCount = 1),
        rivalLegal = readyMoves(line = knightThreatensRook, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Bishop, Square('e', 4)),
          Piece(Side.Black, Man.Knight, Square('f', 5))
        )
      )
    assertEquals(
      SceneMaterial.write(sameBoardProofMissing, bishopTakesAttacker),
      None,
      "same-board proof missing"
    )

    val routeMismatch = plausibleMaterial.copy(route = Some(Line(Square('e', 4), Square('h', 7))))
    assertNoPublicOutput("route mismatch", routeMismatch)

    val wrongEngineLine = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(Line(Square('e', 4), Square('h', 7))))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val staleEngineLine = EngineCheck.fromStory(
      facts = defenseFacts,
      story = Some(plausibleMaterial),
      engineLine = Some(EngineLine(Vector(bishopTakesAttacker))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(3),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector("wrong engine line" -> wrongEngineLine, "stale engine line" -> staleEngineLine).foreach:
      (label, check) =>
        assertEquals(check.status, EngineCheckStatus.Unknown, label)
        assertEquals(SceneMaterial.withEngineCheck(plausibleMaterial, check), None, label)
        assertEquals(
          StoryTable.choose(Vector(plausibleMaterial)).exists(_.story.engineCheck.nonEmpty),
          false,
          label
        )

  test(
    "MIH-6 Downstream Boundary Smoke passes only selected Lead Verdict through existing downstream stages"
  ):
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

    val materialFacts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get

    def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = materialFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val hangingMove = Line(Square('d', 2), Square('h', 6))
    val hanging = TacticHanging.write(materialFacts, hangingMove).get.copy(proof = score(99))
    val overlappingMaterial = SceneMaterial.write(materialFacts, hangingMove).get.copy(proof = score(98))
    val lowStrengthMaterial = overlappingMaterial.copy(proof = score(60))
    val cappedMaterial =
      SceneMaterial
        .withEngineCheck(overlappingMaterial, engineCheck(overlappingMaterial, EngineCheckStatus.Caps))
        .get
    val refutedMaterial =
      SceneMaterial
        .withEngineCheck(overlappingMaterial, engineCheck(overlappingMaterial, EngineCheckStatus.Refutes))
        .get
    val incompleteMaterial = overlappingMaterial.copy(storyProof = StoryProof.empty)

    val leadVerdict = StoryTable.choose(Vector(hanging)).head
    val supportVerdict =
      StoryTable.choose(Vector(hanging, overlappingMaterial)).find(_.story == overlappingMaterial).get
    val contextVerdict = StoryTable.choose(Vector(lowStrengthMaterial)).head
    val blockedVerdict = StoryTable.choose(Vector(incompleteMaterial)).head
    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedMaterial)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      assertEquals(plan.flatMap(_.allowedClaim), None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)
      plan.foreach: relationPlan =>
        assertEquals(LlmNarrationSmoke.mockNarrate(relationPlan, Some(rendered)), None, label)
        assertEquals(LlmNarrationSmoke.codexCliPrompt(relationPlan, rendered), None, label)

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(LlmNarrationSmoke.mockNarrate(leadPlan, rendered), Some(rendered.text))
    assertEquals(
      LlmNarrationSmoke.check(leadPlan, rendered, rendered.text),
      NarrationSmokeCheck(true, Vector.empty)
    )
    assertEquals(ExplanationPlan.fromSelected(leadVerdict.copy(selected = false)), None)

    Vector(
      "renderedText:",
      "claimKey:",
      "strength:",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Codex CLI prompt must include allowed field: $required")

    Vector(
      "ExplanationPlan",
      "Verdict",
      "Story",
      "Proof",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "route:",
      "target:",
      "anchor:",
      "role:",
      "scene:",
      "tactic:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"Codex CLI prompt must not include raw input: $forbidden")

    val explanationMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    assertEquals(
      explanationMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    val rendererMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    assertEquals(
      rendererMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector("fromStory", "fromProof", "fromEngineCheck", "fromVerdict", "fromBoardFacts").foreach:
      forbiddenMethod =>
        assert(!rendererMethodNames.contains(forbiddenMethod), s"renderer must not expose $forbiddenMethod")
        assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke must not expose $forbiddenMethod")

    assertEquals(supportVerdict.role, Role.Support)
    assertEquals(contextVerdict.role, Role.Context)
    assertEquals(blockedVerdict.role, Role.Blocked)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(refutedVerdict.role, Role.Blocked)

    Vector(
      "Support row" -> supportVerdict,
      "Context row" -> contextVerdict,
      "Blocked row" -> blockedVerdict,
      "Capped row" -> cappedVerdict,
      "Refuted row" -> refutedVerdict
    ).foreach: (label, verdict) =>
      assertNoStandaloneText(label, verdict)

  test("MIH-7 Diagnostics Boundary keeps diagnostics out of public meaning"):
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

    val facts = BoardFacts.fromFen("6k1/8/7n/1q3r2/8/5N2/3Q4/6K1 w - - 0 1").toOption.get

    def engineCheck(story: Story, status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('d', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val route = Line(Square('d', 2), Square('h', 6))
    val hanging = TacticHanging.write(facts, route).get.copy(proof = score(99))
    val material = SceneMaterial.write(facts, route).get.copy(proof = score(98))
    val cappedMaterial =
      SceneMaterial.withEngineCheck(material, engineCheck(material, EngineCheckStatus.Caps)).get
    val refutedMaterial =
      SceneMaterial.withEngineCheck(material, engineCheck(material, EngineCheckStatus.Refutes)).get

    val leadVerdict = StoryTable.choose(Vector(hanging)).head
    val supportVerdict = StoryTable.choose(Vector(hanging, material)).find(_.story == material).get
    val cappedVerdict = StoryTable.choose(Vector(cappedMaterial)).head
    val refutedVerdict = StoryTable.choose(Vector(refutedMaterial)).head
    val diagnosticTerms =
      Vector(
        "diagnostic proof failure should never render",
        "raw engine text should never render",
        "source row payload should never render",
        "proofFailures",
        "EngineCheck"
      )
    val diagnosticVerdict = supportVerdict.copy(
      proofFailures = Vector(BoardFacts.MissingEvidence("MIH-7 diagnostic", diagnosticTerms)),
      engineCheckStatus = Some(EngineCheckStatus.Refutes),
      engineStrengthLimited = true
    )
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(leadPlan, rendered).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    val refutedPlan = ExplanationPlan.fromSelected(refutedVerdict).get
    val diagnosticPlan = ExplanationPlan.fromSelected(diagnosticVerdict).get

    assert(diagnosticVerdict.proofFailures.nonEmpty)
    assertEquals(diagnosticVerdict.values, supportVerdict.values)
    diagnosticTerms.foreach: term =>
      assert(
        !diagnosticVerdict.values.mkString(" ").contains(term),
        s"Verdict.values leaked diagnostic term: $term"
      )
      assert(!diagnosticPlan.toString.contains(term), s"ExplanationPlan leaked diagnostic term: $term")
      assert(!rendered.text.contains(term), s"RenderedLine leaked diagnostic term: $term")
      assert(!prompt.contains(term), s"LLM smoke prompt leaked diagnostic term: $term")

    val explanationSurface =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet ++
        ExplanationPlan.getClass.getDeclaredMethods.map(_.getName).toSet
    Vector(
      "sourceRow",
      "source_row",
      "proofFailures",
      "engineCheck",
      "fromStory",
      "fromProof",
      "fromEngineCheck"
    ).foreach: forbiddenSurface =>
      assert(
        !explanationSurface.exists(_.toLowerCase.contains(forbiddenSurface.toLowerCase)),
        s"ExplanationPlan must not expose diagnostic/source-row surface: $forbiddenSurface"
      )

    assertEquals(diagnosticPlan.allowedClaim, None)
    assertEquals(diagnosticPlan.supportContextLinks, Vector.empty)
    assert(diagnosticPlan.relations.contains(ExplanationRelation.CappedSameStory))
    assert(refutedPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))

    Vector(
      "Support relation" -> supportPlan,
      "Capped relation" -> cappedPlan,
      "Refuted relation" -> refutedPlan,
      "Injected diagnostic relation" -> diagnosticPlan
    ).foreach: (label, plan) =>
      assert(plan.relations.nonEmpty, label)
      assertEquals(DeterministicRenderer.fromPlan(plan), None, label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, Some(rendered)), None, label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, rendered), None, label)
      plan.relations
        .map(_.key)
        .foreach: relationKey =>
          assert(!rendered.text.contains(relationKey), s"$label relation key leaked to renderer wording")
          assert(!prompt.contains(relationKey), s"$label relation key leaked to LLM prompt")

  test("MIH Closeout Hard Cleanup keeps ownership separated without new runtime authority"):
    val hangingMaterialFacts =
      BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingMaterialFacts, capture).get
    val material = SceneMaterial.write(hangingMaterialFacts, capture).get

    val defenseFacts =
      BoardFacts.fromFen("4k3/ppp2ppp/8/5n2/3RB3/2N2Q2/PPP2PPP/4K2R w - - 0 1").toOption.get
    val threat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('e', 4), Square('f', 5))
    val defense = SceneDefense.write(defenseFacts, threat, defenseMove).get

    def selectedPlan(story: Story): ExplanationPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get

    val hangingPlan = selectedPlan(hanging)
    val materialPlan = selectedPlan(material)
    val defensePlan = selectedPlan(defense)

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
        StoryWriter.TacticRookHit,
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
        StoryWriter.SceneStalemate,
        StoryWriter.SceneMateThreat
      )
    )
    assertEquals(ExplanationStrength.values.toVector, Vector(ExplanationStrength.Bounded))
    assertEquals(
      ExplanationRelation.values.toVector,
      Vector(
        ExplanationRelation.SameFamilyLowerRank,
        ExplanationRelation.AlternativeHangingCandidate,
        ExplanationRelation.AlternativeForkCandidate,
        ExplanationRelation.CappedSameStory,
        ExplanationRelation.BlockedByEngineRefute
      )
    )

    assertEquals(hanging.writer, Some(StoryWriter.TacticHanging))
    assertEquals(hanging.scene, Scene.Tactic)
    assertEquals(hanging.tactic, Some(Tactic.Hanging))
    assert(hanging.captureResult.exists(_.positiveMaterial))
    assertEquals(hanging.threatProof, None)
    assertEquals(hanging.defenseProof, None)
    assertEquals(hangingPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))

    assertEquals(material.writer, Some(StoryWriter.SceneMaterial))
    assertEquals(material.scene, Scene.Material)
    assertEquals(material.tactic, None)
    assert(material.captureResult.exists(_.positiveMaterial))
    assertEquals(material.threatProof, None)
    assertEquals(material.defenseProof, None)
    assertEquals(materialPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))

    assertEquals(defense.writer, Some(StoryWriter.SceneDefense))
    assertEquals(defense.scene, Scene.Defense)
    assertEquals(defense.tactic, None)
    assertEquals(defense.captureResult, None)
    assert(defense.threatProof.exists(_.complete))
    assert(defense.defenseProof.exists(_.complete))
    assertEquals(defensePlan.allowedClaim, Some(ExplanationClaim.DefendsPiece))

    val overlapVerdicts = StoryTable.choose(Vector(material, hanging))
    assertEquals(overlapVerdicts.count(_.role == Role.Lead), 1)
    assertEquals(overlapVerdicts.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(overlapVerdicts.find(_.story == material).map(_.role), Some(Role.Support))

    val speechKeysByOwner =
      Map(
        "Tactic.Hanging" -> ExplanationClaim.HangingAllowed.map(_.key),
        "Scene.Material" -> ExplanationClaim.MaterialAllowed.map(_.key),
        "Scene.Defense" -> ExplanationClaim.DefenseAllowed.map(_.key)
      )
    assertEquals(
      speechKeysByOwner.values.flatten.toVector.distinct.size,
      speechKeysByOwner.values.flatten.size
    )
    speechKeysByOwner.values.flatten.foreach: key =>
      Vector("pressure", "initiative", "strategy", "plan", "winning", "decisive", "conversion").foreach:
        broadTerm =>
          assert(!key.contains(broadTerm), s"speech key must not promote broad term authority: $key")

    val runtimeAuthorityNames =
      StoryWriter.values.map(_.toString).toVector ++
        ExplanationClaim.values.map(_.key).toVector ++
        ExplanationRelation.values.map(_.key).toVector
    Vector("mih", "hardening", "closeout", "diagnostic_boundary", "public_route", "production_api").foreach:
      forbidden =>
        assert(
          !runtimeAuthorityNames.exists(_.toLowerCase.contains(forbidden)),
          s"MIH helper became runtime authority: $forbidden"
        )

  test("Material-3 Scene.Material writer admits one narrow proof-backed Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val writerSurfaceNames =
      SceneMaterial.getClass.getDeclaredMethods.map(_.getName).toSet ++
        SceneMaterial.getClass.getDeclaredFields.map(_.getName).toSet

    assertEquals(SceneMaterial.WriterOpen, true)
    assertEquals(story.scene, Scene.Material)
    assertEquals(story.tactic, None)
    assertEquals(story.writer, Some(StoryWriter.SceneMaterial))
    assertEquals(story.captureResult.exists(_.positiveMaterial), true)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.route, Some(capture))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    Vector(
      "winning",
      "decisive",
      "blunder",
      "conversion",
      "bestMove",
      "forced",
      "noCounterplay",
      "engineSays",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("Material-3 writer keeps false positives and aliases silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    assertEquals(
      SceneMaterial.write(defendedFacts, defendedCapture),
      None,
      "equal recapture result is closed"
    )
    assertEquals(
      SceneMaterial.write(positiveFacts, Line(Square('d', 4), Square('d', 5))),
      None,
      "illegal capture"
    )
    assertEquals(SceneMaterial.write(untrusted, capture), None, "same-board proof missing")
    assertEquals(SceneMaterial.write(unclearMaterial, capture), None, "material result unknown")
    assertEquals(SceneMaterial.write(kingTarget, capture), None, "king target is closed")

    val material = SceneMaterial.write(positiveFacts, capture).get
    val noWriter = material.copy(writer = None)
    val missingCapture = material.copy(captureResult = None)
    val mismatchedTarget = material.copy(target = Some(Square('d', 4)))
    val tacticAlias = material.copy(scene = Scene.Tactic, tactic = Some(Tactic.Hanging))
    val hangingAlias = material.copy(writer = Some(StoryWriter.TacticHanging))

    Vector(noWriter, missingCapture, mismatchedTarget, tacticAlias, hangingAlias).foreach: story =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

  test("Material-3 EngineCheck Refutes blocks Scene.Material writer"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = SceneMaterial.write(facts, capture).get

    def check(status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supports.engineCheck.map(_.status), Option(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Option(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Option(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Option(EngineCheckStatus.Refutes))
    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)

  test("Material-4 negative corpus keeps material-looking false positives silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val material = SceneMaterial.write(positiveFacts, capture).get
    val zeroResult = CaptureResult.fromBoardFacts(defendedFacts, defendedCapture)
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    def noLeadOrBlocked(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = positiveFacts,
        story = Some(material),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    assertEquals(
      SceneMaterial.write(positiveFacts, Line(Square('d', 4), Square('d', 5))),
      None,
      "no legal line"
    )
    assertEquals(SceneMaterial.write(untrusted, capture), None, "same-board proof missing")
    assertEquals(
      SceneMaterial.write(defendedFacts, defendedCapture),
      None,
      "recapture erases material result"
    )
    assertEquals(SceneMaterial.write(unclearMaterial, capture), None, "exchange result unclear")
    assertEquals(SceneMaterial.write(kingTarget, capture), None, "king target")
    assertEquals(zeroResult.materialResult, Some(0))

    val zeroMaterialStory =
      material.copy(
        target = zeroResult.targetPiece.map(_.square),
        anchor = zeroResult.capturingPiece.map(_.square),
        route = Some(defendedCapture),
        storyProof = StoryProof.fromBoardFacts(defendedFacts, defendedCapture),
        captureResult = Some(zeroResult)
      )
    val noLegalLine = material.copy(route = None)
    val storyProofIncomplete = material.copy(storyProof = StoryProof.empty)
    val proofWithoutMaterialResult =
      material.copy(captureResult = material.captureResult.map(_.copy(materialResult = None)))
    val proofWithoutExchange =
      material.copy(captureResult =
        material.captureResult.map(_.copy(boundedExchangeSequence = Vector.empty))
      )
    val proofMissing = material.copy(captureResult = None)
    val tacticWriterAsMaterial = material.copy(writer = Some(StoryWriter.TacticHanging))
    val forkWriterAsMaterial = material.copy(writer = Some(StoryWriter.TacticFork))
    val highProofOnly =
      Story(
        Scene.Material,
        side = Side.White,
        target = Some(Square('a', 3)),
        anchor = Some(Square('a', 2)),
        route = Some(safeRoute),
        rival = Side.Black,
        proof = proof(
          boardProof = 99,
          lineProof = 99,
          ownerProof = 99,
          anchorProof = 99,
          routeProof = 99,
          conversionPrize = 99,
          forcing = 99,
          kingHeat = 99,
          immediacy = 99
        ),
        storyProof = storyProof()
      )
    val refuted = SceneMaterial.withEngineCheck(material, check(EngineCheckStatus.Refutes)).get

    Vector(
      "legal line missing" -> noLegalLine,
      "material result zero" -> zeroMaterialStory,
      "engine refutes" -> refuted,
      "StoryProof incomplete" -> storyProofIncomplete,
      "material proof lacks result" -> proofWithoutMaterialResult,
      "material proof lacks bounded exchange" -> proofWithoutExchange,
      "material proof missing" -> proofMissing,
      "tactic writer tries Material" -> tacticWriterAsMaterial,
      "Fork writer tries Material" -> forkWriterAsMaterial,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      noLeadOrBlocked(label, story)

  test("Material-4 prevents automatic duplicate Material speech from tactic Stories"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingCapture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingCapture).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    def noMaterialRow(label: String, story: Story): Unit =
      val verdicts = StoryTable.choose(Vector(story))
      assertEquals(verdicts.exists(_.story.scene == Scene.Material), false, label)

    def blockedMaterialAlias(label: String, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assertEquals(verdict.role, Role.Blocked, label)

    noMaterialRow("Hanging alone does not create Material", hanging)
    noMaterialRow("Fork alone does not create Material", fork)
    blockedMaterialAlias(
      "Hanging copied as Material is blocked",
      hanging.copy(scene = Scene.Material, tactic = None)
    )
    blockedMaterialAlias(
      "Fork copied as Material is blocked",
      fork.copy(scene = Scene.Material, tactic = None)
    )

  test("Material-5 reuses EngineCheck only for existing same-board Material Stories"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val reply = EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))
    val story = SceneMaterial.write(facts, capture).get

    def check(
        status: EngineCheckStatus,
        engineLine: Option[EngineLine] = Some(EngineLine(Vector(capture))),
        checkedFacts: BoardFacts = facts,
        storyInput: Option[Story] = Some(story),
        depth: Option[Int] = Some(18),
        freshnessPly: Option[Int] = Some(0)
    ) =
      EngineCheck.fromStory(
        facts = checkedFacts,
        story = storyInput,
        engineLine = engineLine,
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = depth,
        freshnessPly = freshnessPly,
        requestedStatus = status
      )

    val supports = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = SceneMaterial.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get

    assertEquals(supports.engineCheck.exists(_.storyBound), true)
    assertEquals(StoryTable.choose(Vector(supports)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(caps)).head.engineStrengthLimited, true)
    assertEquals(StoryTable.choose(Vector(unknown)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(refutes)).head.role, Role.Blocked)

    val engineOnly = check(EngineCheckStatus.Supports, storyInput = None)
    val differentFen = check(EngineCheckStatus.Supports, checkedFacts = otherFacts)
    val routeMismatch = check(EngineCheckStatus.Supports, engineLine = Some(EngineLine(Vector(wrongRoute))))
    val missingDepth = check(EngineCheckStatus.Supports, depth = None, freshnessPly = None)
    val stale = check(EngineCheckStatus.Supports, freshnessPly = Some(2))
    val rawEvidence = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(capture),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector(engineOnly, differentFen, routeMismatch, missingDepth, stale).foreach: engineCheck =>
      assertEquals(engineCheck.status, EngineCheckStatus.Unknown)
      assertEquals(SceneMaterial.withEngineCheck(story, engineCheck), None)
    assertEquals(rawEvidence.evidenceReady, true)
    assertEquals(rawEvidence.storyBound, false)
    assertEquals(SceneMaterial.withEngineCheck(story, rawEvidence), None)
    assertEquals(StoryTable.choose(Vector(story)).exists(_.story.engineCheck.nonEmpty), false)
    assertEquals(
      intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.MaterialEngineCheck")).getClass,
      classOf[ClassNotFoundException]
    )

  test("Material-6 StoryTable lets Hanging Fork and Material compete deterministically"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val material = SceneMaterial.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

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

    val low = score(72)
    val high = score(99)
    val hangingHigh = hanging.copy(proof = high)
    val forkHigh = fork.copy(proof = high)
    val materialHigh = material.copy(proof = high)
    val hangingLow = hanging.copy(proof = low)
    val forkLow = fork.copy(proof = low)
    val materialLow = material.copy(proof = low)

    assertEquals(StoryTable.choose(Vector(hangingHigh, forkLow, materialLow)).head.story, hangingHigh)
    assertEquals(StoryTable.choose(Vector(hangingLow, forkHigh, materialLow)).head.story, forkHigh)
    assertEquals(StoryTable.choose(Vector(forkLow, materialHigh)).head.story, materialHigh)

    val overlapping = StoryTable.choose(Vector(materialHigh, hanging))
    val overlappingHanging = overlapping.find(_.story == hanging).get
    val overlappingMaterial = overlapping.find(_.story == materialHigh).get

    assertEquals(materialHigh.route, hanging.route)
    assertEquals(materialHigh.target, hanging.target)
    assertEquals(
      materialHigh.captureResult.flatMap(_.materialResult),
      hanging.captureResult.flatMap(_.materialResult)
    )
    assertEquals(overlappingHanging.role, Role.Lead)
    assertEquals(overlappingMaterial.role, Role.Support)
    assertEquals(
      ExplanationPlan.fromSelected(overlappingMaterial).flatMap(DeterministicRenderer.fromPlan),
      None
    )

    val tied = Vector(hangingHigh, forkHigh, materialHigh)
    val forward = StoryTable.choose(tied)
    val reverse = StoryTable.choose(tied.reverse)
    val shuffled = StoryTable.choose(Vector(materialHigh, hangingHigh, forkHigh))

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    assertEquals(orderShape(forward), orderShape(reverse))
    assertEquals(orderShape(forward), orderShape(shuffled))
    assertEquals(forward.map(_.role), Vector(Role.Lead, Role.Support, Role.Support))
    forward
      .filter(_.role != Role.Lead)
      .foreach: verdict =>
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None)
    assertEquals(
      StoryTable.choose(Vector(hangingHigh, forkHigh)).exists(_.story.scene == Scene.Material),
      false
    )

  test("Material-6 blocks invalid Material rows and ignores raw engine eval ordering"):
    val materialFacts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(materialFacts, leftCapture).get
    val rightMaterial = SceneMaterial.write(materialFacts, rightCapture).get.copy(proof = leftMaterial.proof)
    val reply = EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))

    def checked(story: Story, before: Int, after: Int): Story =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = materialFacts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      SceneMaterial.withEngineCheck(story, check).get

    val lowEvalLeft = checked(leftMaterial, before = 20, after = 80)
    val highEvalRight = checked(rightMaterial, before = 400, after = 460)
    val highEvalLeft = checked(leftMaterial, before = 400, after = 460)
    val lowEvalRight = checked(rightMaterial, before = 20, after = 80)

    assertEquals(
      StoryTable.choose(Vector(highEvalRight, lowEvalLeft)).map(_.story.route),
      StoryTable.choose(Vector(lowEvalRight, highEvalLeft)).map(_.story.route)
    )

    val refutedCheck = EngineCheck.fromStory(
      facts = materialFacts,
      story = Some(leftMaterial),
      engineLine = Some(EngineLine(Vector(leftCapture))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedMaterial = SceneMaterial.withEngineCheck(leftMaterial, refutedCheck).get
    val incompleteMaterial = leftMaterial.copy(storyProof = StoryProof.empty)
    val writerlessMaterial = leftMaterial.copy(writer = None)
    val noProofMaterial = leftMaterial.copy(captureResult = None)
    val highProofConversionLike = leftMaterial.copy(
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99,
        kingHeat = 99
      )
    )

    val verdicts = StoryTable.choose(
      Vector(
        highProofConversionLike,
        refutedMaterial,
        incompleteMaterial,
        writerlessMaterial,
        noProofMaterial
      )
    )

    assertEquals(verdicts.find(_.story == highProofConversionLike).map(_.role), Some(Role.Lead))
    assertEquals(verdicts.find(_.story == refutedMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == incompleteMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == writerlessMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == noProofMaterial).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.exists(_.story.scene == Scene.Convert), false)
    verdicts.foreach: verdict =>
      assertEquals(verdict.values.size, Verdict.Size)
    assertEquals(
      verdicts
        .find(_.story == highProofConversionLike)
        .flatMap(ExplanationPlan.fromSelected)
        .flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    Vector(refutedMaterial, incompleteMaterial, writerlessMaterial, noProofMaterial).foreach: blockedStory =>
      val blockedPlan = verdicts.find(_.story == blockedStory).flatMap(ExplanationPlan.fromSelected)
      assertEquals(blockedPlan.flatMap(_.allowedClaim), None)
      assertEquals(blockedPlan.flatMap(DeterministicRenderer.fromPlan), None)

  test("Material-7 ExplanationPlan lowers selected Material Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val fromSelectedParameterNames =
      fromSelectedMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getName)
        .mkString(" ")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(
      fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    Vector(
      "BoardFacts",
      "CaptureResult",
      "ExchangeResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "String",
      "Source"
    ).foreach: forbiddenType =>
      assert(
        !fromSelectedParameterNames.contains(forbiddenType),
        s"ExplanationPlan must not accept $forbiddenType"
      )

    val plan = maybePlan.get
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Material)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(plan.allowedClaim.map(_.key), Some("material_balance_changes"))
    assertEquals(
      ExplanationClaim.MaterialAllowed.map(_.key),
      Vector("material_balance_changes", "line_leaves_material_gain", "exchange_leaves_side_ahead")
    )
    assertEquals(
      ExplanationClaim.MaterialForbiddenKeys,
      Vector(
        "winning_position",
        "decisive_advantage",
        "conversion",
        "blunder",
        "best_move",
        "forced_win",
        "no_counterplay",
        "line_tactic_identity"
      )
    )
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    Vector("winning", "decisive", "conversion", "blunder", "best_move", "forced_win", "no_counterplay")
      .foreach: forbidden =>
        assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    Vector("sentence", "prose", "rendered").foreach: forbiddenName =>
      assert(!plan.productElementNames.exists(name => name.toLowerCase.contains(forbiddenName)))
    Vector(
      "materialProof",
      "captureResult",
      "exchangeResult",
      "engineCheck",
      "boardFacts",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Material-7 Material non Lead capped and refuted plans create no stronger claim"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(facts, leftCapture).get
    val rightMaterial = SceneMaterial.write(facts, rightCapture).get.copy(proof = leftMaterial.proof)
    val verdicts = StoryTable.choose(Vector(leftMaterial, rightMaterial))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    def checked(status: EngineCheckStatus): Verdict =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(leftMaterial),
        engineLine = Some(EngineLine(Vector(leftCapture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(SceneMaterial.withEngineCheck(leftMaterial, check).get)).head

    val cappedVerdict = checked(EngineCheckStatus.Caps)
    val refutedVerdict = checked(EngineCheckStatus.Refutes)
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get
    val refutedPlan = ExplanationPlan.fromSelected(refutedVerdict).get

    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    Vector(supportPlan, contextPlan, blockedPlan, cappedPlan, refutedPlan).foreach: plan =>
      assertEquals(plan.allowedClaim, None)
      assertEquals(DeterministicRenderer.fromPlan(plan), None)
    assertEquals(supportPlan.relations.map(_.key), Vector("same_family_lower_rank"))
    assertEquals(contextPlan.relations, Vector.empty)
    assertEquals(blockedPlan.debugOnly, true)
    assertEquals(cappedPlan.relations.map(_.key), Vector("capped_same_story"))
    assert(cappedPlan.forbiddenWording.map(_.key).contains("strong_wording"))
    assertEquals(refutedPlan.relations.map(_.key), Vector("blocked_by_engine_refute"))
    assertEquals(refutedPlan.debugOnly, true)

  test("Material-8 DeterministicRenderer phrases Material ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val maybeRendered = DeterministicRenderer.fromPlan(plan)
    val forbiddenPhrases =
      Vector(
        "winning",
        "technically winning",
        "decisive",
        "blunder",
        "forced",
        "best move",
        "no counterplay",
        "engine says",
        "conversion"
      )
    val rendererMethods = DeterministicRenderer.getClass.getDeclaredMethods.toVector

    assertEquals(plan.scene, Scene.Material)
    assertEquals(plan.tactic, None)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(facts.sideLegal.sanFor(capture), Some("dxe5"))
    assertEquals(plan.routeSan, Some("dxe5"))
    assertEquals(maybeRendered.map(_.text), Some("After dxe5, White comes out ahead in material."))
    val rendered = maybeRendered.get
    assertEquals(rendered.text, "After dxe5, White comes out ahead in material.")
    assertEquals(rendered.claimKey, "material_balance_changes")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), phrase)
    assertEquals(
      rendererMethods
        .filter(_.getName == "fromPlan")
        .map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("ExplanationPlan"))
    )

  test("Material-8 refuses Material renderer text without bounded Lead claim permission"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val leftMaterial = SceneMaterial.write(facts, leftCapture).get
    val rightMaterial = SceneMaterial.write(facts, rightCapture).get.copy(proof = leftMaterial.proof)
    val verdicts = StoryTable.choose(Vector(leftMaterial, rightMaterial))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val blockedPlan =
      ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get.copy(role = Role.Blocked)).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(leftMaterial),
      engineLine = Some(EngineLine(Vector(leftCapture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val cappedPlan =
      ExplanationPlan
        .fromSelected(StoryTable.choose(Vector(SceneMaterial.withEngineCheck(leftMaterial, check).get)).head)
        .get

    assert(DeterministicRenderer.fromPlan(leadPlan).nonEmpty)
    Vector(
      supportPlan,
      blockedPlan,
      cappedPlan,
      leadPlan.copy(allowedClaim = None),
      leadPlan.copy(allowedClaim = Some(ExplanationClaim.LineLeavesMaterialGain)),
      leadPlan.copy(strength = ExplanationStrength.Bounded, forbiddenWording = Vector.empty),
      leadPlan.copy(scene = Scene.Tactic),
      leadPlan.copy(debugOnly = true)
    ).foreach: plan =>
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Material-9 LLM smoke accepts Material RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered)
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered)
    val checked = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead in material.")

    assertEquals(mockText, Some(rendered.text))
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    Vector(
      "renderedText: After dxe5, White comes out ahead in material.",
      "claimKey: material_balance_changes",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.exists(_.contains(required)), s"Material prompt must include allowed input: $required")
    Vector(
      "Verdict",
      "Story",
      "material proof",
      "CaptureResult",
      "ExchangeResult",
      "EngineCheck",
      "BoardFacts",
      "engine eval",
      "raw PV",
      "proofFailures",
      "source row",
      "materialResult:",
      "capturedPieces:",
      "recaptureCandidates:"
    ).foreach: forbidden =>
      assert(
        !prompt.exists(_.contains(forbidden)),
        s"Material prompt must not include forbidden raw input label: $forbidden"
      )

  test("Material-9 LLM smoke rejects stronger Material rephrases"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val safe = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead in material.")
    val inventedMove = LlmNarrationSmoke.check(plan, rendered, "After dxe5 Ke7, White is ahead in material.")
    val inventedLine = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White is ahead, and Ke7 follows.")
    val inventedTactic = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White starts a fork.")
    val inventedPlan = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White starts a plan.")
    val engineBestWinning =
      LlmNarrationSmoke.check(
        plan,
        rendered,
        "The engine says dxe5 is the best move and technically winning."
      )
    val forcedDecisiveBlunder =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 is a forced decisive result after a blunder.")
    val conversion = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White converts the material edge.")
    val strongerMaterialWin = LlmNarrationSmoke.check(plan, rendered, "After dxe5, White wins material.")
    val noCounterplay = LlmNarrationSmoke.check(plan, rendered, "After dxe5, Black has no counterplay.")
    val coordinateRoute =
      LlmNarrationSmoke.check(plan, rendered, "After the move from d4 to e5, White is ahead in material.")
    val compactCoordinateRoute =
      LlmNarrationSmoke.check(plan, rendered, "After d4e5, White is ahead in material.")

    assertEquals(safe.accepted, true)
    assertEquals(safe.violations, Vector.empty)
    assertEquals(inventedMove.accepted, false)
    assertEquals(inventedMove.violations.contains("new_move_or_line"), true)
    assertEquals(inventedLine.accepted, false)
    assertEquals(inventedLine.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedPlan.accepted, false)
    assertEquals(inventedPlan.violations.contains("new_tactic_or_plan"), true)
    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("engine_mention"), true)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(forcedDecisiveBlunder.accepted, false)
    assertEquals(forcedDecisiveBlunder.violations.contains("forbidden_wording"), true)
    assertEquals(forcedDecisiveBlunder.violations.contains("stronger_claim"), true)
    assertEquals(conversion.accepted, false)
    assertEquals(conversion.violations.contains("forbidden_wording"), true)
    assertEquals(strongerMaterialWin.accepted, false)
    assertEquals(strongerMaterialWin.violations.contains("stronger_claim"), true)
    assertEquals(noCounterplay.accepted, false)
    assertEquals(noCounterplay.violations.contains("forbidden_wording"), true)
    assertEquals(coordinateRoute.accepted, false)
    assertEquals(coordinateRoute.violations.contains("non_san_move_text"), true)
    assertEquals(compactCoordinateRoute.accepted, false)
    assertEquals(compactCoordinateRoute.violations.contains("non_san_move_text"), true)

  test("Material slice closeout keeps sibling scenes and proof homes closed"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val material = SceneMaterial.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(material)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.story.scene, Scene.Material)
    assertEquals(material.tactic, None)
    assertEquals(material.plan, None)
    assertEquals(material.captureResult.exists(_.publicClaimAllowed), false)
    assertEquals(material.storyProof.failures(material), Vector.empty)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.MaterialBalanceChanges))
    assertEquals(rendered.text.contains("winning"), false)
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, "After dxe5, White has a winning position.").accepted,
      false
    )
    assertEquals(
      intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.ExchangeResult")).getClass,
      classOf[ClassNotFoundException]
    )
    assertEquals(
      intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.MaterialEngineCheck")).getClass,
      classOf[ClassNotFoundException]
    )

    val engineCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(material),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val checkedMaterial = SceneMaterial.withEngineCheck(material, engineCheck).get
    val checkedVerdict = StoryTable.choose(Vector(checkedMaterial)).head
    val checkedPlan = ExplanationPlan.fromSelected(checkedVerdict).get
    val checkedRendered = DeterministicRenderer.fromPlan(checkedPlan).get

    assertEquals(checkedMaterial.engineCheck.exists(_.storyBound), true)
    assertEquals(checkedVerdict.role, Role.Lead)
    assertEquals(checkedPlan.scene, Scene.Material)
    assertEquals(checkedRendered.claimKey, "material_balance_changes")
    assertEquals(LlmNarrationSmoke.mockNarrate(checkedPlan, checkedRendered), Some(checkedRendered.text))

    val siblingScenes = Vector(
      "Defense" -> material.copy(scene = Scene.Defense),
      "Plan" -> material.copy(scene = Scene.Plan, plan = Some(Plan.CenterBreak)),
      "Convert" -> material.copy(scene = Scene.Convert),
      "Blunder" -> material.copy(scene = Scene.Blunder)
    )

    siblingScenes.foreach: (label, story) =>
      val siblingVerdict = StoryTable.choose(Vector(story)).head
      assertEquals(siblingVerdict.role, Role.Blocked, label)
      assertEquals(siblingVerdict.leadAllowed, false, label)
      assertEquals(
        ExplanationPlan.fromSelected(siblingVerdict).flatMap(DeterministicRenderer.fromPlan),
        None,
        label
      )

  test("CaptureResult leaves missing evidence for capture false positives"):
    val legalButUntrustedLine = Line(Square('d', 4), Square('e', 5))
    val legalButUntrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = legalButUntrustedLine, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val illegalCapture = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val ownTarget = BoardFacts.fromFen("4k3/8/8/4N3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = Line(Square('d', 4), Square('e', 5)), captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )
    val unclearMaterial =
      minimalBoardFacts(
        sideLegal = readyMoves(line = legalButUntrustedLine, captureCount = 1),
        material = readyMaterial.copy(known = false),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )

    val cases = Vector(
      "attacked but illegal capture" ->
        CaptureResult.fromBoardFacts(illegalCapture, Line(Square('d', 4), Square('d', 5))) ->
        Vector("legal capture", "target piece"),
      "target is own piece" ->
        CaptureResult.fromBoardFacts(ownTarget, legalButUntrustedLine) ->
        Vector("legal capture", "target enemy piece"),
      "target is king" ->
        CaptureResult.fromBoardFacts(kingTarget, Line(Square('d', 4), Square('e', 5))) ->
        Vector("legal capture", "target non-king"),
      "same-board proof missing" ->
        CaptureResult.fromBoardFacts(legalButUntrusted, legalButUntrustedLine) ->
        Vector("same-board proof"),
      "capture material result unclear" ->
        CaptureResult.fromBoardFacts(unclearMaterial, legalButUntrustedLine) ->
        Vector("same-board proof", "material result")
    )

    cases.foreach:
      case ((label, result), expectedMissing) =>
        assertEquals(result.publicClaimAllowed, false, label)
        assertEquals(result.positiveMaterial, false, label)
        assert(result.missingEvidence.nonEmpty, label)
        val missing = result.missingEvidence.flatMap(_.missing)
        expectedMissing.foreach: expected =>
          assert(missing.contains(expected), s"$label must report missing $expected, got $missing")

    val highProofHanging = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Hanging),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(legalButUntrustedLine),
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
      storyProof = StoryProof.fromBoardFacts(
        BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get,
        legalButUntrustedLine
      )
    )
    val verdict = StoryTable.choose(Vector(highProofHanging)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("EngineCheck records internal engine evidence without public claim authority"):
    val checkedMove = Line(Square('d', 4), Square('e', 5))
    val replyMove = Line(Square('e', 8), Square('e', 7))
    val engineLine = EngineLine(Vector(checkedMove, replyMove))
    val replyLine = EngineLine(Vector(replyMove))

    val check = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(checkedMove),
      engineLine = Some(engineLine),
      replyLine = Some(replyLine),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(85)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    val engineSurfaceNames =
      classOf[EngineCheck].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineCheck].getDeclaredFields.map(_.getName).toSet ++
        classOf[EngineLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineLine].getDeclaredFields.map(_.getName).toSet ++
        classOf[EngineEval].getDeclaredMethods.map(_.getName).toSet ++
        classOf[EngineEval].getDeclaredFields.map(_.getName).toSet

    assertEquals(engineLine.moves, Vector(checkedMove, replyMove))
    assertEquals(replyLine.moves, Vector(replyMove))
    assertEquals(check.sameBoardProof, true)
    assertEquals(check.checkedMove, Some(checkedMove))
    assertEquals(check.engineLine, Some(engineLine))
    assertEquals(check.replyLine, Some(replyLine))
    assertEquals(check.evalBefore, Some(EngineEval(20)))
    assertEquals(check.evalAfter, Some(EngineEval(85)))
    assertEquals(check.depth, Some(18))
    assertEquals(check.freshnessPly, Some(0))
    assertEquals(check.missingEvidence, Vector.empty)
    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, true)
    Vector("best", "strategy", "commentary", "render", "llm", "publicText", "verdict").foreach: forbidden =>
      assert(!engineSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

  test("EngineCheck reports missing same-board stale and move-binding evidence"):
    val checkedMove = Line(Square('d', 4), Square('e', 5))
    val otherMove = Line(Square('d', 4), Square('d', 5))
    val stale = EngineCheck.fromEvidence(
      sameBoardProof = false,
      checkedMove = Some(checkedMove),
      engineLine = Some(EngineLine(Vector(otherMove))),
      replyLine = None,
      evalBefore = None,
      evalAfter = None,
      depth = None,
      freshnessPly = Some(2)
    )

    assertEquals(stale.publicClaimAllowed, false)
    assertEquals(stale.evidenceReady, false)
    assertEquals(
      stale.missingEvidence,
      Vector(
        BoardFacts.MissingEvidence(
          "EngineCheck",
          Vector(
            "same-board proof",
            "checked move in engine line",
            "reply line",
            "eval before",
            "eval after",
            "depth or freshness",
            "fresh engine evidence"
          )
        )
      )
    )

  test("EngineLine rejects empty PV-shaped evidence"):
    intercept[IllegalArgumentException]:
      EngineLine(Vector.empty)

  test("EngineCheck rejects engine evidence from a different FEN"):
    val storyFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(storyFacts, capture).get

    val check = EngineCheck.fromStory(
      facts = otherFacts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(check.sameBoardProof, false)
    assert(check.missingEvidence.flatMap(_.missing).contains("same-board proof"))
    assert(check.missingEvidence.flatMap(_.missing).contains("same legal line"))

  test("EngineCheck rejects engine lines that do not start with the Story route"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val story = TacticHanging.write(facts, capture).get

    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(wrongRoute))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(check.sameBoardProof, true)
    assert(check.missingEvidence.flatMap(_.missing).contains("same Story route"))
    assert(check.missingEvidence.flatMap(_.missing).contains("checked move in engine line"))

  test("EngineCheck keeps stale or depth-missing engine data diagnostic only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = None,
      freshnessPly = Some(2)
    )

    assertEquals(check.publicClaimAllowed, false)
    assertEquals(check.evidenceReady, false)
    assertEquals(
      check.missingEvidence.flatMap(_.missing),
      Vector("depth or freshness", "fresh engine evidence")
    )

  test("EngineCheck cannot speak from eval or PV without a Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val evalOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = None,
      replyLine = None,
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0)
    )
    val pvOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = None,
      evalAfter = None,
      depth = Some(18),
      freshnessPly = Some(0)
    )

    Vector(evalOnly, pvOnly).foreach: check =>
      assertEquals(check.publicClaimAllowed, false)
      assertEquals(check.evidenceReady, false)
      assertEquals(check.checkedMove, None)
      assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
      assert(check.missingEvidence.flatMap(_.missing).contains("Story"))

  test("EngineCheck status stays Unknown unless same-board Story guard passes"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val unknown = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val supports = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(unknown.status, EngineCheckStatus.Unknown)
    assertEquals(supports.status, EngineCheckStatus.Supports)
    assertEquals(supports.missingEvidence, Vector.empty)

  test("Tactic.Hanging attaches EngineCheck statuses and Refutes blocks lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head

    def check(status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refutes = TacticHanging.withEngineCheck(story, check(EngineCheckStatus.Refutes)).get
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(refutesVerdict.leadAllowed, false)
    assert(refutesVerdict.role != Role.Lead)
    assertEquals(supportsVerdict.values, baseVerdict.values)
    assertEquals(capsVerdict.values, baseVerdict.values)

  test("StoryTable integrates EngineCheck conservatively without creating public engine claims"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(capture))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      TacticHanging.withEngineCheck(story, check).get

    val unknownVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Unknown))).head
    val supportsVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Supports))).head
    val capsVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head
    val refutesVerdict = StoryTable.choose(Vector(checked(EngineCheckStatus.Refutes))).head

    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)
    Vector(unknownVerdict, supportsVerdict, capsVerdict).foreach: verdict =>
      assertEquals(verdict.leadAllowed, true)
      assertEquals(verdict.role, Role.Lead)
      assertEquals(verdict.strength, baseVerdict.strength)
      assertEquals(verdict.values, baseVerdict.values)

    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(refutesVerdict.engineStrengthLimited, false)
    assertEquals(refutesVerdict.leadAllowed, false)
    assert(refutesVerdict.role != Role.Lead)

  test("Stage 5-1 verified Hanging alone becomes Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(verdict.story, story)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.proofFailures, Vector.empty)

  test("Stage 5-1 chooses deterministic Lead from two verified Hanging rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val leftCapture = Line(Square('d', 4), Square('c', 5))
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val left = TacticHanging.write(facts, leftCapture).get
    val right = TacticHanging.write(facts, rightCapture).get
    val forward = StoryTable.choose(Vector(right, left))
    val reverse = StoryTable.choose(Vector(left, right))

    assertEquals(forward.map(_.story), reverse.map(_.story))
    assertEquals(forward.head.story, left)
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.head.leadAllowed, true)
    assertEquals(forward(1).story, right)
    assertEquals(forward(1).role, Role.Support)
    assertEquals(forward(1).leadAllowed, false)

  test("Stage 5-1 Refuted Hanging is Blocked while supported Hanging may Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val supportedCapture = Line(Square('d', 4), Square('c', 5))
    val refutedCapture = Line(Square('d', 4), Square('e', 5))
    val supported = TacticHanging.write(facts, supportedCapture).get
    val refuted = TacticHanging.write(facts, refutedCapture).get

    def check(story: Story, status: EngineCheckStatus) =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(story.route.get))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val checkedSupported =
      TacticHanging.withEngineCheck(supported, check(supported, EngineCheckStatus.Supports)).get
    val checkedRefuted = TacticHanging.withEngineCheck(refuted, check(refuted, EngineCheckStatus.Refutes)).get
    val verdicts = StoryTable.choose(Vector(checkedRefuted, checkedSupported))

    assertEquals(verdicts.head.story, checkedSupported)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts.head.leadAllowed, true)
    assertEquals(verdicts.find(_.story == checkedRefuted).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == checkedRefuted).map(_.leadAllowed), Some(false))
    assertEquals(
      verdicts.find(_.story == checkedRefuted).flatMap(_.engineCheckStatus),
      Some(EngineCheckStatus.Refutes)
    )

  test("Stage 5-1 Capped Hanging keeps strength-limited diagnostic"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val capped = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(capped)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(verdict.engineStrengthLimited, true)

  test("Stage 5-1 Unknown EngineCheck creates no engine wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val baseVerdict = StoryTable.choose(Vector(story)).head
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Unknown
    )
    val unknown = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(unknown)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(verdict.values, baseVerdict.values)

  test("Stage 5-1 incomplete or unsupported Hanging rows are not Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val lowStrength = story.copy(
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        conversionPrize = 60
      )
    )
    val incompleteProof = story.copy(storyProof = StoryProof.empty)
    val missingCapture = story.copy(captureResult = None)
    val noWriter = story.copy(writer = None)

    val verdicts = StoryTable.choose(Vector(lowStrength, incompleteProof, missingCapture, noWriter))

    assertEquals(verdicts.find(_.story == lowStrength).map(_.role), Some(Role.Context))
    assertEquals(verdicts.find(_.story == incompleteProof).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == missingCapture).map(_.role), Some(Role.Blocked))
    assert(
      verdicts
        .find(_.story == noWriter)
        .exists(verdict => verdict.role == Role.Context || verdict.role == Role.Blocked)
    )
    assert(verdicts.forall(_.role != Role.Lead))
    assert(verdicts.forall(!_.leadAllowed))

  test("Stage 5-2 keeps deterministic Lead independent of input order"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val forward = StoryTable.choose(Vector(right, left))
    val reverse = StoryTable.choose(Vector(left, right))

    assertEquals(forward.map(_.story), reverse.map(_.story))
    assertEquals(forward.head.role, Role.Lead)
    assertEquals(forward.head.story, left)

  test("Stage 5-2 uses target anchor and route as deterministic equal-strength tie-breaks"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val c5 = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val e5 = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val routeA = c5.copy(
      target = Some(Square('c', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('c', 4), Square('c', 5))),
      storyProof = StoryProof.untrustedLegalLine(Line(Square('c', 4), Square('c', 5)))
    )
    val routeB = c5.copy(
      target = Some(Square('c', 5)),
      anchor = Some(Square('c', 4)),
      route = Some(Line(Square('d', 4), Square('c', 5))),
      storyProof = StoryProof.untrustedLegalLine(Line(Square('d', 4), Square('c', 5)))
    )

    assertEquals(StoryTable.choose(Vector(e5, c5)).head.story, c5)
    assertEquals(StoryTable.choose(Vector(routeB, routeA)).head.story, routeA)

  test("Stage 5-2 uses blocked status without proofFailures text as public ordering"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val context = story.copy(
      target = Some(Square('a', 1)),
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        conversionPrize = 60
      )
    )
    val blocked = story.copy(
      target = Some(Square('h', 8)),
      storyProof = StoryProof.empty,
      proof = proof(
        boardProof = 99,
        lineProof = 99,
        ownerProof = 99,
        anchorProof = 99,
        routeProof = 99,
        conversionPrize = 99,
        forcing = 99,
        immediacy = 99
      )
    )
    val verdicts = StoryTable.choose(Vector(blocked, context))

    assertEquals(context.proofFailures, Vector.empty)
    assert(blocked.proofFailures.nonEmpty)
    assertEquals(verdicts.head.story, context)
    assertEquals(verdicts.head.role, Role.Context)
    assertEquals(verdicts(1).story, blocked)
    assertEquals(verdicts(1).role, Role.Blocked)

  test("Stage 5-2 raw engine eval does not reorder Hanging rows"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get

    def checked(story: Story, before: Int, after: Int) =
      val route = story.route.get
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(route))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      TacticHanging.withEngineCheck(story, check).get

    val lowEvalLeft = checked(left, before = 20, after = 80)
    val highEvalRight = checked(right, before = 400, after = 460)
    val highEvalLeft = checked(left, before = 400, after = 460)
    val lowEvalRight = checked(right, before = 20, after = 80)

    assertEquals(
      StoryTable.choose(Vector(highEvalRight, lowEvalLeft)).map(_.story.route),
      StoryTable.choose(Vector(lowEvalRight, highEvalLeft)).map(_.story.route)
    )

  test("Stage 5-3 Refuted Hanging remains Blocked"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(80)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refuted = TacticHanging.withEngineCheck(story, check).get
    val verdict = StoryTable.choose(Vector(refuted)).head

    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)
    assertEquals(verdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))

  test("Stage 5-3 Quiet cannot Lead when positive Hanging exists"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val quiet = Story(
      Scene.Quiet,
      side = Side.White,
      target = Some(Square('a', 2)),
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.Black,
      proof =
        proof(boardProof = 90, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90),
      storyProof = storyProof()
    )
    val verdicts = StoryTable.choose(Vector(quiet, hanging))

    assertEquals(verdicts.head.story, hanging)
    assertEquals(verdicts.head.role, Role.Lead)
    assertEquals(verdicts.find(_.story == quiet).map(_.leadAllowed), Some(false))
    assert(verdicts.find(_.story == quiet).exists(_.role != Role.Lead))

  test("Stage 5-3 Source and Opening cannot outrank board-backed Hanging"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    def context(scene: Scene, target: Square) =
      Story(
        scene,
        side = Side.White,
        target = Some(target),
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        rival = Side.Black,
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
        storyProof = storyProof()
      )

    val sourceVerdicts = StoryTable.choose(Vector(context(Scene.Source, Square('b', 2)), hanging))
    val openingVerdicts = StoryTable.choose(Vector(context(Scene.Opening, Square('c', 2)), hanging))

    assertEquals(sourceVerdicts.head.story, hanging)
    assertEquals(openingVerdicts.head.story, hanging)
    assert(sourceVerdicts.forall(verdict => verdict.story.scene != Scene.Source || !verdict.leadAllowed))
    assert(openingVerdicts.forall(verdict => verdict.story.scene != Scene.Opening || !verdict.leadAllowed))

  test("Stage 5-3 no-writer Story cannot behave like Hanging"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hanging = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val noWriter = hanging.copy(
      writer = None,
      proof = proof(
        boardProof = 60,
        lineProof = 60,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        forcing = 60,
        immediacy = 60,
        conversionPrize = 60,
        kingHeat = 60
      )
    )
    val verdict = StoryTable.choose(Vector(noWriter)).head

    assertEquals(noWriter.proofFailures, Vector.empty)
    assertEquals(verdict.role, Role.Blocked)
    assertEquals(verdict.leadAllowed, false)

  test("EngineCheck negative corpus blocks or weakens false positive Hanging evidence"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val otherFacts = BoardFacts.fromFen(Fen.initial).toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val wrongRoute = Line(Square('d', 4), Square('d', 5))
    val reply = Line(Square('e', 8), Square('e', 7))
    val story = TacticHanging.write(facts, capture).get
    val positiveCapture = CaptureResult.fromBoardFacts(facts, capture)
    val noCaptureResult = story.copy(captureResult = None)
    val incompleteStoryProof = story.copy(storyProof = StoryProof.empty)
    val noNamedWriter = story.copy(writer = None)

    def check(
        storyInput: Option[Story] = Some(story),
        factsInput: BoardFacts = facts,
        engineMoves: Vector[Line] = Vector(capture, reply),
        replyMoves: Vector[Line] = Vector(reply),
        before: EngineEval = EngineEval(80),
        after: EngineEval = EngineEval(120),
        depthInput: Option[Int] = Some(18),
        freshnessInput: Option[Int] = Some(0),
        requested: EngineCheckStatus = EngineCheckStatus.Supports
    ) =
      EngineCheck.fromStory(
        facts = factsInput,
        story = storyInput,
        engineLine = Some(EngineLine(engineMoves)),
        replyLine = Some(EngineLine(replyMoves)),
        evalBefore = Some(before),
        evalAfter = Some(after),
        depth = depthInput,
        freshnessPly = freshnessInput,
        requestedStatus = requested
      )

    val localGainButLargerTactic = check(after = EngineEval(-420))
    val replyRefutes = check(after = EngineEval(-260), requested = EngineCheckStatus.Refutes)
    val evalCollapse = check(before = EngineEval(120), after = EngineEval(-500))
    val wrongBoard = check(factsInput = otherFacts)
    val stale = check(freshnessInput = Some(3))
    val routeMismatch = check(engineMoves = Vector(wrongRoute, reply))
    val engineOnlyNoCapture = check(storyInput = Some(noCaptureResult))
    val engineOnlyIncompleteProof = check(storyInput = Some(incompleteStoryProof))
    val engineOnlyNoNamedWriter = check(storyInput = Some(noNamedWriter))

    Vector(localGainButLargerTactic, replyRefutes, evalCollapse).foreach: refutingCheck =>
      assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
      val checkedStory = TacticHanging.withEngineCheck(story, refutingCheck).get
      val verdict = StoryTable.choose(Vector(checkedStory)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

    Vector(
      wrongBoard -> "same-board proof",
      stale -> "fresh engine evidence",
      routeMismatch -> "same Story route",
      engineOnlyNoCapture -> "same-board proof",
      engineOnlyIncompleteProof -> "same-board proof",
      engineOnlyNoNamedWriter -> "same-board proof"
    ).foreach: (check, missing) =>
      assertEquals(check.status, EngineCheckStatus.Unknown)
      assertEquals(check.publicClaimAllowed, false)
      assertEquals(check.evidenceReady, false)
      assert(check.missingEvidence.flatMap(_.missing).contains(missing), missing)

    assertEquals(noCaptureResult.copy(engineCheck = Some(engineOnlyNoCapture)).captureResult, None)
    assertEquals(incompleteStoryProof.proofFailures.nonEmpty, true)
    assertEquals(noNamedWriter.writer, None)
    assertEquals(positiveCapture.missingEvidence, Vector.empty)

  test("EngineCheck attaches only to named tactic writers"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(facts, capture).get
    val hangingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(hanging),
      engineLine = Some(EngineLine(Vector(capture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkCheck = EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val material = hanging.copy(scene = Scene.Material, tactic = None)
    val noWriter = hanging.copy(writer = None)

    assertEquals(
      TacticHanging.withEngineCheck(hanging, hangingCheck).map(_.engineCheck),
      Some(Some(hangingCheck))
    )
    assertEquals(TacticFork.withEngineCheck(fork, forkCheck).map(_.engineCheck), Some(Some(forkCheck)))
    assertEquals(TacticHanging.withEngineCheck(material, hangingCheck), None)
    assertEquals(TacticHanging.withEngineCheck(noWriter, hangingCheck), None)
    assertEquals(TacticFork.withEngineCheck(hanging, hangingCheck), None)

  test("Fork-5 EngineCheck reuses existing sidecar without creating Fork"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val reply = EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get

    def check(status: EngineCheckStatus): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(fork),
        engineLine = Some(EngineLine(Vector(forkMove))),
        replyLine = Some(reply),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val supports = check(EngineCheckStatus.Supports)
    val caps = check(EngineCheckStatus.Caps)
    val unknown = check(EngineCheckStatus.Unknown)
    val refutes = check(EngineCheckStatus.Refutes)

    Vector(supports, caps, unknown, refutes).foreach: engineCheck =>
      assertEquals(engineCheck.evidenceReady, true)
      assertEquals(engineCheck.sameBoardProof, true)
      assertEquals(engineCheck.checkedMove, Some(forkMove))
      assertEquals(TacticFork.withEngineCheck(fork, engineCheck).map(_.engineCheck), Some(Some(engineCheck)))

    assertEquals(
      StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, supports).get)).head.role,
      Role.Lead
    )
    val cappedVerdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, caps).get)).head
    assertEquals(cappedVerdict.role, Role.Lead)
    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(
      StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, unknown).get)).head.role,
      Role.Lead
    )
    assertEquals(
      StoryTable.choose(Vector(TacticFork.withEngineCheck(fork, refutes).get)).head.role,
      Role.Blocked
    )

    val engineOnly = EngineCheck.fromStory(
      facts = facts,
      story = None,
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val missingDepth = EngineCheck.fromStory(
      facts = facts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = None,
      freshnessPly = None,
      requestedStatus = EngineCheckStatus.Supports
    )
    val routeMismatch = EngineCheck.fromEvidence(
      sameBoardProof = true,
      checkedMove = Some(Line(Square('h', 1), Square('h', 2))),
      engineLine = Some(EngineLine(Vector(Line(Square('h', 1), Square('h', 2))))),
      replyLine = Some(reply),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.evidenceReady, false)
    assertEquals(engineOnly.checkedMove, None)
    assertEquals(TacticFork.withEngineCheck(fork, engineOnly), None)
    assertEquals(missingDepth.status, EngineCheckStatus.Unknown)
    assertEquals(missingDepth.evidenceReady, false)
    assertEquals(TacticFork.withEngineCheck(fork, missingDepth), None)
    assertEquals(routeMismatch.evidenceReady, true)
    assertEquals(TacticFork.withEngineCheck(fork, routeMismatch), None)
    assertEquals(
      intercept[ClassNotFoundException](Class.forName("lila.commentary.chess.ForkEngineCheck")).getClass,
      classOf[ClassNotFoundException]
    )

    val engineCheckSurfaces =
      EngineCheck.getClass.getDeclaredMethods.toVector ++ classOf[EngineCheck].getDeclaredMethods.toVector
    assertEquals(engineCheckSurfaces.exists(_.getReturnType.getSimpleName.contains("Story")), false)

  test("Fork-6 StoryTable orders Hanging and Fork without creating meaning"):
    val hangingFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val hangingMove = Line(Square('d', 4), Square('e', 5))
    val hanging = TacticHanging.write(hangingFacts, hangingMove).get
    val forkFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(forkFacts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val forkTied = fork.copy(proof = hanging.proof)

    assertEquals(StoryTable.choose(Vector(hanging)).head.role, Role.Lead)
    assertEquals(StoryTable.choose(Vector(fork)).head.role, Role.Lead)

    def orderShape(verdicts: Vector[Verdict]) =
      verdicts.map(verdict => (verdict.story.scene, verdict.story.tactic, verdict.story.route, verdict.role))

    val hangingFork = StoryTable.choose(Vector(hanging, forkTied))
    val forkHanging = StoryTable.choose(Vector(forkTied, hanging))
    assertEquals(orderShape(hangingFork), orderShape(forkHanging))
    assertEquals(hangingFork.head.story, hanging)
    assertEquals(hangingFork.head.role, Role.Lead)
    assertEquals(hangingFork.find(_.story == forkTied).map(_.role), Some(Role.Support))
    assertEquals(ExplanationPlan.fromSelected(hangingFork.find(_.story == forkTied).get), None)

    val lowProof = proof(
      boardProof = 60,
      lineProof = 60,
      ownerProof = 90,
      anchorProof = 90,
      routeProof = 90,
      conversionPrize = 60,
      forcing = 60,
      immediacy = 60
    )
    val writerlessFork = fork.copy(writer = None, proof = lowProof)
    val forkWithoutMultiTargetProof = fork.copy(multiTargetProof = None, proof = lowProof)
    val incompleteFork = fork.copy(storyProof = StoryProof.empty, proof = lowProof)
    val refuted = EngineCheck.fromStory(
      facts = forkFacts,
      story = Some(fork),
      engineLine = Some(EngineLine(Vector(forkMove))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedFork = TacticFork.withEngineCheck(fork, refuted).get
    val blocked = StoryTable.choose(
      Vector(hanging, writerlessFork, forkWithoutMultiTargetProof, incompleteFork, refutedFork)
    )

    assertEquals(blocked.find(_.story == hanging).map(_.role), Some(Role.Lead))
    assertEquals(blocked.find(_.story == writerlessFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == forkWithoutMultiTargetProof).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == incompleteFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == refutedFork).map(_.role), Some(Role.Blocked))
    assertEquals(blocked.find(_.story == forkWithoutMultiTargetProof).get.proofFailures, Vector.empty)

    def checkedFork(evalBefore: Int, evalAfter: Int, pvTail: Line): Story =
      val check = EngineCheck.fromStory(
        facts = forkFacts,
        story = Some(forkTied),
        engineLine = Some(EngineLine(Vector(forkMove, pvTail))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
        evalBefore = Some(EngineEval(evalBefore)),
        evalAfter = Some(EngineEval(evalAfter)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )
      TacticFork.withEngineCheck(forkTied, check).get

    val lowEvalFork = checkedFork(20, 80, Line(Square('h', 8), Square('h', 7)))
    val highEvalFork = checkedFork(400, 460, Line(Square('h', 8), Square('h', 6)))
    assertEquals(
      orderShape(StoryTable.choose(Vector(hanging, lowEvalFork))),
      orderShape(StoryTable.choose(Vector(hanging, highEvalFork)))
    )

  test("Fork-7 ExplanationPlan lowers selected Fork Verdict without raw proof input"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val maybePlan = ExplanationPlan.fromSelected(verdict)
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.selected, true)
    assertEquals(maybePlan.flatMap(_.allowedClaim.map(_.key)), Some("forks_two_targets"))
    val plan = maybePlan.get
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('b', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('f', 5)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(forkMove))
    assertEquals(plan.evidenceLine, Some(forkMove))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assert(planSurfaceNames.contains("secondaryTarget"))
    assertEquals(ExplanationClaim.ForkAllowed.map(_.key), Vector("forks_two_targets", "attacks_two_targets"))
    assertEquals(
      ExplanationClaim.ForkForbiddenKeys,
      Vector(
        "wins_material_by_fork",
        "wins_queen",
        "decisive_fork",
        "forced_win",
        "best_move",
        "no_counterplay"
      )
    )
    Vector(
      "wins_material_by_fork",
      "wins_queen",
      "decisive_fork",
      "forced_win",
      "best_move",
      "no_counterplay"
    ).foreach: forbidden =>
      assert(plan.forbiddenWording.map(_.key).contains(forbidden), forbidden)
    Vector(
      "MultiTargetProof",
      "EngineCheck",
      "CaptureResult",
      "BoardFacts",
      "rawPv",
      "proofFailures",
      "sourceRow"
    )
      .foreach: forbiddenName =>
        assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Fork-7 Support Context and Blocked Fork plans stay silent"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val leadVerdict = StoryTable.choose(Vector(fork)).head
    val supportVerdict = leadVerdict.copy(role = Role.Support, leadAllowed = false, rank = 2)
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)
    val engineBlockedVerdict =
      blockedVerdict.copy(engineCheckStatus = Some(EngineCheckStatus.Refutes))

    Vector(supportVerdict, contextVerdict, blockedVerdict, engineBlockedVerdict).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict), None)

  test("Fork-8 DeterministicRenderer phrases Fork ExplanationPlan without stronger meaning"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "wins queen",
        "wins material",
        "decisive",
        "forced",
        "best move",
        "engine says",
        "no counterplay",
        "blunder"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.tactic, Some(Tactic.Fork))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.ForksTwoTargets))
    assertEquals(plan.target, Some(Square('b', 5)))
    assertEquals(plan.secondaryTarget, Some(Square('f', 5)))
    assertEquals(plan.route, Some(forkMove))
    assertEquals(facts.sideLegal.sanFor(forkMove), Some("Nd4"))
    assertEquals(plan.routeSan, Some("Nd4"))
    assertEquals(plan.evidenceLine, Some(forkMove))
    assertEquals(rendered.text, "Nd4 forks the pieces on b5 and f5.")
    assertEquals(rendered.claimKey, "forks_two_targets")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(
        !rendered.text.toLowerCase.contains(phrase),
        s"Fork renderer must not contain forbidden phrase: $phrase"
      )

  test("Fork-9 LLM smoke accepts Fork RenderedLine without raw proof input"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get
    val checked = LlmNarrationSmoke.check(plan, rendered, mockText)

    assertEquals(mockText, rendered.text)
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    Vector(
      "renderedText: Nd4 forks the pieces on b5 and f5.",
      "claimKey: forks_two_targets",
      "strength: bounded",
      "forbiddenWording:",
      "Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"Fork prompt must include allowed input: $required")
    Vector(
      "Verdict",
      "Story",
      "MultiTargetProof",
      "EngineCheck",
      "BoardFacts",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "target:",
      "secondaryTarget:",
      "reply map:"
    ).foreach: forbidden =>
      assert(
        !prompt.contains(forbidden),
        s"Fork prompt must not include forbidden raw input label: $forbidden"
      )

  test("Fork-9 LLM smoke rejects stronger Fork rephrases"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val safe = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces on b5 and f5.")
    val inventedMove = LlmNarrationSmoke.check(plan, rendered, "After Nd4 Kh7, the fork stays.")
    val inventedTactic = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and starts a skewer.")
    val inventedPlan = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and starts a plan.")
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says Nd4 is the best move and a winning fork.")
    val forcedDecisiveBlunder =
      LlmNarrationSmoke.check(plan, rendered, "Nd4 is a forced decisive fork after a blunder.")
    val winsQueen = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and wins the queen.")
    val winsMaterial = LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the pieces and wins material.")
    val namesTargets =
      LlmNarrationSmoke.check(plan, rendered, "Nd4 forks the queen on b5 and rook on f5.")
    val coordinateRoute =
      LlmNarrationSmoke.check(plan, rendered, "After the move from f3 to d4, the fork stays.")
    val compactCoordinateRoute = LlmNarrationSmoke.check(plan, rendered, "After f3d4, the fork stays.")

    assertEquals(safe.accepted, true)
    assertEquals(safe.violations, Vector.empty)
    assertEquals(inventedMove.accepted, false)
    assertEquals(inventedMove.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedPlan.accepted, false)
    assertEquals(inventedPlan.violations.contains("new_tactic_or_plan"), true)
    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("engine_mention"), true)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(forcedDecisiveBlunder.accepted, false)
    assertEquals(forcedDecisiveBlunder.violations.contains("forbidden_wording"), true)
    assertEquals(forcedDecisiveBlunder.violations.contains("stronger_claim"), true)
    Vector(winsQueen, winsMaterial, namesTargets).foreach: checked =>
      assertEquals(checked.accepted, false)
      assert(
        checked.violations.contains("forbidden_wording") || checked.violations.contains("stronger_claim")
      )
    assertEquals(coordinateRoute.accepted, false)
    assertEquals(coordinateRoute.violations.contains("non_san_move_text"), true)
    assertEquals(compactCoordinateRoute.accepted, false)
    assertEquals(compactCoordinateRoute.violations.contains("non_san_move_text"), true)

  test("Fork-8 refuses Fork renderer text without structural Fork plan permission"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Line(Square('f', 3), Square('d', 4))
    val fork = TacticFork.write(facts, Some(forkMove), Some(Square('b', 5)), Some(Square('f', 5))).get
    val verdict = StoryTable.choose(Vector(fork)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val invalidPlans =
      Vector(
        plan.copy(role = Role.Support),
        plan.copy(allowedClaim = Some(ExplanationClaim.AttacksTwoTargets)),
        plan.copy(allowedClaim = None),
        plan.copy(debugOnly = true),
        plan.copy(target = None),
        plan.copy(secondaryTarget = None),
        plan.copy(route = None),
        plan.copy(evidenceLine = None),
        plan.copy(forbiddenWording = Vector.empty)
      )

    invalidPlans.foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

  test("Tactic.Hanging writer opens the first narrow positive Story"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))

    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(TacticHanging.WriterOpen, true)
    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Hanging))
    assertEquals(story.writer, Some(StoryWriter.TacticHanging))
    assertEquals(story.captureResult.exists(_.positiveMaterial), true)
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.target, Some(Square('e', 5)))
    assertEquals(story.route, Some(capture))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(verdict.proofFailures, Vector.empty)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.role, Role.Lead)

  test("Tactic.Hanging writer keeps the negative corpus silent"):
    val positiveFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val defendedFacts = BoardFacts.fromFen("7k/8/8/3q4/4Qn2/8/8/4K3 w - - 0 1").toOption.get
    val pawnTargetFacts = BoardFacts.fromFen("4k3/8/8/4p3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val defendedCapture = Line(Square('e', 4), Square('d', 5))
    val untrusted =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 8)),
          Piece(Side.White, Man.Pawn, Square('d', 4)),
          Piece(Side.Black, Man.Knight, Square('e', 5))
        )
      )
    val kingTarget =
      minimalBoardFacts(
        sideLegal = readyMoves(line = capture, captureCount = 1),
        pieces = Vector(
          Piece(Side.White, Man.King, Square('e', 1)),
          Piece(Side.Black, Man.King, Square('e', 5)),
          Piece(Side.White, Man.Queen, Square('d', 4))
        )
      )

    assertEquals(TacticHanging.write(defendedFacts, defendedCapture), None, "attacked and recaptured equally")
    assertEquals(
      TacticHanging.write(positiveFacts, Line(Square('d', 4), Square('d', 5))),
      None,
      "capture illegal"
    )
    assertEquals(TacticHanging.write(pawnTargetFacts, capture), None, "pawn target scope is closed")
    assertEquals(TacticHanging.write(kingTarget, capture), None, "king target is closed")
    assertEquals(TacticHanging.write(untrusted, capture), None, "same-board proof missing")

    val positiveCapture = CaptureResult.fromBoardFacts(positiveFacts, capture)
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
    val noWriter = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Hanging),
      side = Side.White,
      rival = Side.Black,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(capture),
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(positiveFacts, capture),
      captureResult = Some(positiveCapture)
    )
    val writerWithoutCapture = noWriter.copy(writer = Some(StoryWriter.TacticHanging), captureResult = None)
    val captureWithoutStoryProof =
      noWriter.copy(writer = Some(StoryWriter.TacticHanging), storyProof = StoryProof.empty)
    val boardFactOnly = noWriter.copy(writer = None, captureResult = None, storyProof = StoryProof.empty)
    val mismatchedCaptureIdentity =
      noWriter.copy(
        writer = Some(StoryWriter.TacticHanging),
        target = Some(Square('d', 4))
      )

    Vector(noWriter, writerWithoutCapture, captureWithoutStoryProof, boardFactOnly, mismatchedCaptureIdentity)
      .foreach: story =>
        val verdict = StoryTable.choose(Vector(story)).head
        assertEquals(verdict.leadAllowed, false)
        assert(verdict.role != Role.Lead)

    val fork = noWriter.copy(tactic = Some(Tactic.Fork), writer = Some(StoryWriter.TacticHanging))
    val material =
      noWriter.copy(scene = Scene.Material, tactic = None, writer = Some(StoryWriter.TacticHanging))
    val defense =
      noWriter.copy(scene = Scene.Defense, tactic = None, writer = Some(StoryWriter.TacticHanging))
    val planStory =
      noWriter.copy(
        scene = Scene.Plan,
        tactic = None,
        plan = Some(Plan.CenterBreak),
        writer = Some(StoryWriter.TacticHanging)
      )

    Vector(fork, material, defense, planStory).foreach: story =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false)
      assert(verdict.role != Role.Lead)

  test("Fork geometry readiness stays inside MultiTargetProof"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val proof = MultiTargetProof.fromBoardFacts(facts, forkMove, targetA, targetB)

    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.forkMove, forkMove)
    assertEquals(proof.attacker, Some(Piece(Side.White, Man.Knight, Square('f', 3))))
    assertEquals(proof.attackerAfterMove, Some(Piece(Side.White, Man.Knight, Square('d', 4))))
    assertEquals(proof.targetSquares, Vector(Square('b', 5), Square('f', 5)))
    assertEquals(
      proof.targets,
      Vector(Piece(Side.Black, Man.Queen, Square('b', 5)), Piece(Side.Black, Man.Rook, Square('f', 5)))
    )
    assertEquals(proof.targetValues, Vector(900, 500))
    assertEquals(proof.attackedTargetSquaresAfterMove, Vector(Square('b', 5), Square('f', 5)))
    assertEquals(proof.publicClaimAllowed, false)

    val proofSurfaceNames =
      classOf[MultiTargetProof].getDeclaredMethods.map(_.getName).toSet ++
        classOf[MultiTargetProof].getDeclaredFields.map(_.getName).toSet
    Vector(
      "forkWorks",
      "forkSucceeds",
      "winsMaterial",
      "winsQueen",
      "decisive",
      "forced",
      "bestMove",
      "story",
      "verdict",
      "render",
      "llm"
    ).foreach: forbidden =>
      assert(!proofSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)

    val boardFactsSurfaceNames =
      classOf[BoardFacts].getDeclaredMethods.map(_.getName).toSet ++
        classOf[BoardFacts].getDeclaredFields.map(_.getName).toSet
    assert(!boardFactsSurfaceNames.exists(_.toLowerCase.contains("fork")))

  test("Fork-2 MultiTargetProof owns Fork proof evidence without Story construction"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val proof =
      MultiTargetProof.fromBoardFacts(facts, forkMove, Some(Square('b', 5)), Some(Square('f', 5)))

    assertEquals(proof.complete, true)
    assertEquals(proof.attacker, Some(Piece(Side.White, Man.Knight, Square('f', 3))))
    assertEquals(proof.forkMove, forkMove)
    assertEquals(proof.forkSquare, Some(Square('d', 4)))
    assertEquals(proof.targetA, Some(Piece(Side.Black, Man.Queen, Square('b', 5))))
    assertEquals(proof.targetB, Some(Piece(Side.Black, Man.Rook, Square('f', 5))))
    assertEquals(proof.targetValues, Vector(900, 500))
    assertEquals(proof.replyMap.map(_.target), proof.targets)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.missingEvidence, Vector.empty)

    val proofHomeMethods =
      MultiTargetProof.getClass.getDeclaredMethods.toVector ++ classOf[
        MultiTargetProof
      ].getDeclaredMethods.toVector
    val storyReturningMethods =
      proofHomeMethods.filter(_.getReturnType.getSimpleName.contains("Story")).map(_.getName)
    val storyConstructingMethods =
      proofHomeMethods
        .map(_.getName)
        .filter(name => name == "write" || name == "toStory" || name == "fromStory")
    assertEquals(storyReturningMethods, Vector.empty)
    assertEquals(storyConstructingMethods, Vector.empty)

  test("Fork-3 TacticFork writer admits only proven two-target Stories"):
    val facts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val story = TacticFork.write(facts, forkMove, targetA, targetB).get
    val proof = story.multiTargetProof.get

    assertEquals(story.scene, Scene.Tactic)
    assertEquals(story.tactic, Some(Tactic.Fork))
    assertEquals(story.writer, Some(StoryWriter.TacticFork))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.route, forkMove)
    assertEquals(story.anchor, Some(Square('d', 4)))
    assertEquals(story.target, targetA)
    assertEquals(story.secondaryTarget, targetB)
    assertEquals(proof.complete, true)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.forkSquare, Some(Square('d', 4)))
    assertEquals(proof.targetA.map(_.square), Some(Square('b', 5)))
    assertEquals(proof.targetB.map(_.square), Some(Square('f', 5)))
    assertEquals(proof.attackedTargetSquaresAfterMove, proof.targetSquares)

    val verdict = StoryTable.choose(Vector(story)).head
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)

    val refutes = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )
    val refutedVerdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(story, refutes).get)).head
    assertEquals(refutedVerdict.role, Role.Blocked)
    assertEquals(refutedVerdict.leadAllowed, false)

    val forgedUnprovenRelation =
      story.copy(multiTargetProof = Some(proof.copy(attackedTargetSquaresAfterMove = Vector.empty)))
    val forgedVerdict = StoryTable.choose(Vector(forgedUnprovenRelation)).head
    assertEquals(forgedVerdict.role, Role.Blocked)
    assertEquals(forgedVerdict.leadAllowed, false)

    val writerSurfaceNames =
      TacticFork.getClass.getDeclaredMethods.map(_.getName).toSet ++
        TacticFork.getClass.getDeclaredFields.map(_.getName).toSet
    Vector(
      "winsQueen",
      "winsMaterial",
      "decisive",
      "forced",
      "bestMove",
      "onlyMove",
      "noCounterplay",
      "blunder",
      "engineSays"
    ).foreach: forbidden =>
      assert(!writerSurfaceNames.exists(_.toLowerCase.contains(forbidden.toLowerCase)), forbidden)
    val plan = ExplanationPlan.fromSelected(verdict).get
    assertEquals(plan.allowedClaim.map(_.key), Some("forks_two_targets"))
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty)

  test("Fork negative corpus keeps fork-looking false positives silent"):
    val forkMove = Some(Line(Square('f', 3), Square('d', 4)))
    val targetA = Some(Square('b', 5))
    val targetB = Some(Square('f', 5))
    val positiveFacts = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val positive = TacticFork.write(positiveFacts, forkMove, targetA, targetB).get

    assertEquals(positive.scene, Scene.Tactic)
    assertEquals(positive.tactic, Some(Tactic.Fork))
    assertEquals(positive.writer, Some(StoryWriter.TacticFork))
    assertEquals(positive.anchor, Some(Square('d', 4)))
    assertEquals(positive.route, forkMove)
    assertEquals(positive.target, targetA)
    assertEquals(positive.secondaryTarget, targetB)
    assertEquals(positive.multiTargetProof.exists(_.complete), true)
    assertEquals(StoryTable.choose(Vector(positive)).head.role, Role.Lead)

    val noLegalMove = BoardFacts.fromFen("7k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val noAttacker = BoardFacts.fromFen("7k/8/8/1q3r2/8/8/8/7K w - - 0 1").toOption.get
    val ownTarget = BoardFacts.fromFen("7k/8/8/1q3R2/8/5N2/8/7K w - - 0 1").toOption.get
    val targetNotAttacked = BoardFacts.fromFen("7k/8/8/1q3rb1/8/5N2/8/7K w - - 0 1").toOption.get
    val unsafeForkSquare = BoardFacts.fromFen("7k/6b1/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val oneReplySavesBoth = BoardFacts.fromFen("3r3k/8/8/1q3r2/8/5N2/8/7K w - - 0 1").toOption.get
    val pawnFork = BoardFacts.fromFen("7k/8/3n1n2/8/4P3/8/8/7K w - - 0 1").toOption.get
    val skewer = BoardFacts.fromFen("r6k/8/q7/8/8/8/8/R6K w - - 0 1").toOption.get
    val queenHitOnly = BoardFacts.fromFen("7k/8/8/1q6/8/5N2/8/7K w - - 0 1").toOption.get
    val admittedPawnAttacker =
      TacticFork.write(
        pawnFork,
        Some(Line(Square('e', 4), Square('e', 5))),
        Some(Square('d', 6)),
        Some(Square('f', 6))
      ).get

    Vector(
      "legal move 없음" ->
        TacticFork.write(noLegalMove, Some(Line(Square('f', 3), Square('d', 5))), targetA, targetB),
      "attacker 없음" ->
        TacticFork.write(noAttacker, forkMove, targetA, targetB),
      "fork square 없음" ->
        TacticFork.write(positiveFacts, None, targetA, targetB),
      "two targets 없음" ->
        TacticFork.write(positiveFacts, forkMove, targetA, None),
      "duplicated target" ->
        TacticFork.write(positiveFacts, forkMove, targetA, targetA),
      "own piece target" ->
        TacticFork.write(ownTarget, forkMove, targetA, targetB),
      "target not attacked after move" ->
        TacticFork.write(targetNotAttacked, forkMove, targetA, Some(Square('g', 5))),
      "fork square unsafe with no compensation" ->
        TacticFork.write(unsafeForkSquare, forkMove, targetA, targetB),
      "both targets saved by one reply" ->
        TacticFork.write(oneReplySavesBoth, forkMove, targetA, targetB),
      "skewer tries to enter Tactic.Fork" ->
        TacticFork.write(
          skewer,
          Some(Line(Square('a', 1), Square('a', 4))),
          Some(Square('a', 6)),
          Some(Square('a', 8))
        ),
      "queen-hit-only tries to enter Tactic.Fork" ->
        TacticFork.write(queenHitOnly, forkMove, targetA, targetB)
    ).foreach: (label, maybeStory) =>
      assertEquals(maybeStory, None, label)

    assertEquals(admittedPawnAttacker.scene, Scene.Tactic)
    assertEquals(admittedPawnAttacker.tactic, Some(Tactic.Fork))
    assertEquals(admittedPawnAttacker.writer, Some(StoryWriter.TacticFork))
    assertEquals(admittedPawnAttacker.anchor, Some(Square('e', 5)))
    assertEquals(admittedPawnAttacker.route, Some(Line(Square('e', 4), Square('e', 5))))
    assertEquals(admittedPawnAttacker.target, Some(Square('d', 6)))
    assertEquals(admittedPawnAttacker.secondaryTarget, Some(Square('f', 6)))
    assertEquals(admittedPawnAttacker.multiTargetProof.flatMap(_.attacker).map(_.man), Some(Man.Pawn))
    assertEquals(admittedPawnAttacker.multiTargetProof.exists(_.complete), true)
    assertEquals(StoryTable.choose(Vector(admittedPawnAttacker)).head.role, Role.Lead)
    assertEquals(
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(admittedPawnAttacker)).head).flatMap(_.allowedClaim.map(_.key)),
      Some("forks_two_targets")
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
    val storyProofIncomplete = positive.copy(storyProof = StoryProof.empty)
    val multiTargetProofIncomplete = positive.copy(multiTargetProof = None)
    val bothTargetsSavedByOneReply =
      positive.copy(
        multiTargetProof = positive.multiTargetProof.map: proof =>
          proof.copy(replyMap = proof.replyMap.map(reply => reply.copy(savedByOneReply = true)))
      )
    val highProofOnly = Story(
      Scene.Tactic,
      tactic = Some(Tactic.Fork),
      side = Side.White,
      rival = Side.Black,
      target = targetA,
      secondaryTarget = targetB,
      anchor = Some(Square('f', 3)),
      route = forkMove,
      proof = highProof,
      storyProof = StoryProof.fromBoardFacts(positiveFacts, forkMove.get)
    )
    val engineRefutes = EngineCheck.fromStory(
      facts = positiveFacts,
      story = Some(positive),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(120)),
      evalAfter = Some(EngineEval(-400)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val requestedRefute = EngineCheck.fromStory(
      facts = positiveFacts,
      story = Some(positive),
      engineLine = Some(EngineLine(Vector(forkMove.get))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('h', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Refutes
    )

    Vector(
      "StoryProof incomplete" -> storyProofIncomplete,
      "MultiTargetProof incomplete" -> multiTargetProofIncomplete,
      "both targets saved by one reply" -> bothTargetsSavedByOneReply,
      "high Proof score only" -> highProofOnly
    ).foreach: (label, story) =>
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

    Vector(engineRefutes, requestedRefute).foreach: check =>
      assertEquals(check.status, EngineCheckStatus.Refutes)
      val verdict = StoryTable.choose(Vector(TacticFork.withEngineCheck(positive, check).get)).head
      assertEquals(verdict.leadAllowed, false)
      assertEquals(verdict.role, Role.Blocked)

