package lila.commentary.chess

import java.nio.file.{ Files, Paths }

import chess.format.Fen
import scala.jdk.CollectionConverters.*

class OutputTest extends ChessTestSupport:

  test("Board Facts legal rows cannot become public claims without Story Proof"):
    val facts = BoardFacts.fromFen(Fen.initial).toOption.get
    val legalRow = facts.seen.legalMoves.head
    val story = Story(
      Scene.Material,
      side = legalRow.side,
      target = Some(legalRow.line.to),
      anchor = Some(legalRow.piece.square),
      route = Some(legalRow.line),
      rival = rivalOf(legalRow.side),
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

    assertEquals(
      story.proofFailures,
      Vector(BoardFacts.MissingEvidence("Story Proof", Vector("legal line", "same-board proof")))
    )
    assertEquals(verdict.proofFailures, story.proofFailures)
    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Board Facts observations cannot speak without Story Proof"):
    def highProof =
      proof(
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

    def claimLikeStory(side: Side, target: Square, anchor: Square, route: Line, rival: Side): Story =
      Story(
        Scene.Material,
        side = side,
        target = Some(target),
        anchor = Some(anchor),
        route = Some(route),
        rival = rival,
        proof = highProof
      )

    def cannotSpeak(label: String, boardFactExists: Boolean, story: Story): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      assertEquals(boardFactExists, true, s"$label Board Fact must exist")
      assert(story.proof.publicStrength >= 65, s"$label must use a high Proof score")
      assertEquals(
        story.proofFailures,
        Vector(BoardFacts.MissingEvidence("Story Proof", Vector("legal line", "same-board proof"))),
        label
      )
      assertEquals(verdict.proofFailures, story.proofFailures, label)
      assertEquals(verdict.leadAllowed, false, label)
      assert(verdict.role != Role.Lead, label)

    val contactFacts =
      BoardFacts.fromFen(Fen.Full("4k3/8/8/4n3/3P4/2N5/1P6/4K3 w - - 0 1")).toOption.get
    val contactSeen = contactFacts.seen
    val whitePawn = Piece(Side.White, Man.Pawn, Square('d', 4))
    val whiteKnight = Piece(Side.White, Man.Knight, Square('c', 3))
    val whiteGuard = Piece(Side.White, Man.Pawn, Square('b', 2))
    val blackKnight = Piece(Side.Black, Man.Knight, Square('e', 5))
    val attacked = contactSeen.attacks.find(row => row.attacker == whitePawn && row.target == blackKnight).get
    val guarded = contactSeen.guards.find(row => row.guard == whiteGuard && row.target == whiteKnight).get
    val loose = contactSeen.pieceContacts.find(row => row.piece == blackKnight && row.attackedUnguarded).get

    cannotSpeak(
      "attacked piece",
      boardFactExists = contactSeen.attacks.contains(attacked),
      claimLikeStory(
        attacked.attacker.side,
        attacked.target.square,
        attacked.attacker.square,
        Line(attacked.attacker.square, attacked.target.square),
        attacked.target.side
      )
    )
    cannotSpeak(
      "guarded piece",
      boardFactExists = contactSeen.guards.contains(guarded),
      claimLikeStory(
        guarded.guard.side,
        guarded.target.square,
        guarded.guard.square,
        Line(guarded.guard.square, guarded.target.square),
        rivalOf(guarded.guard.side)
      )
    )
    cannotSpeak(
      "loose unguarded piece",
      boardFactExists = contactSeen.pieceContacts.contains(loose),
      claimLikeStory(
        loose.attackers.head.side,
        loose.piece.square,
        loose.attackers.head.square,
        Line(loose.attackers.head.square, loose.piece.square),
        loose.piece.side
      )
    )

    val pinSeen = BoardFacts.fromFen(Fen.Full("4r1k1/8/8/8/8/8/4N3/4K3 w - - 0 1")).toOption.get.seen
    val pinLine = pinSeen.lineFacts.find(_.shapes.contains(BoardFacts.LineShape.PinToKing)).get
    cannotSpeak(
      "pin-to-king line",
      boardFactExists = pinSeen.lineFacts.contains(pinLine),
      claimLikeStory(
        pinLine.attacker.get.side,
        pinLine.pinned.get.square,
        pinLine.attacker.get.square,
        pinLine.line,
        pinLine.pinned.get.side
      )
    )

    val xraySeen =
      BoardFacts.fromFen(Fen.Full("7k/8/q7/2b5/8/B7/8/R6K w - - 0 1")).toOption.get.seen
    val xrayLine = xraySeen.lineFacts.find(_.shapes.contains(BoardFacts.LineShape.XRay)).get
    cannotSpeak(
      "x-ray shape",
      boardFactExists = xraySeen.lineFacts.contains(xrayLine),
      claimLikeStory(
        xrayLine.from.get.side,
        xrayLine.target.get.square,
        xrayLine.from.get.square,
        xrayLine.line,
        xrayLine.target.get.side
      )
    )

    val openFileSeen = BoardFacts.fromFen(Fen.Full("4k3/8/8/8/8/8/R7/4K3 w - - 0 1")).toOption.get.seen
    val openFileEntry = openFileSeen.fileFacts.find(_.file == 2).get.rookOpenFileEntries.head
    cannotSpeak(
      "open file entry",
      boardFactExists = openFileSeen.fileFacts.exists(_.rookOpenFileEntries.contains(openFileEntry)),
      claimLikeStory(
        openFileEntry.side,
        openFileEntry.line.to,
        openFileEntry.piece.square,
        openFileEntry.line,
        rivalOf(openFileEntry.side)
      )
    )

    val pawnSeen =
      BoardFacts.fromFen(Fen.Full("7k/8/8/3pnp2/1P2P3/2P2N2/P7/4K3 w - - 0 1")).toOption.get.seen
    val pawnLever = pawnSeen.pawnLevers.find(_.pawn.square == Square('e', 4)).get
    val pawnSafe = pawnSeen.pawnSafeSquareObservations.find(_.square == Square('f', 3)).get
    cannotSpeak(
      "pawn lever",
      boardFactExists = pawnSeen.pawnLevers.contains(pawnLever),
      claimLikeStory(
        pawnLever.side,
        pawnLever.target.square,
        pawnLever.pawn.square,
        pawnLever.line,
        pawnLever.target.side
      )
    )
    cannotSpeak(
      "pawn-safe square",
      boardFactExists = pawnSeen.pawnSafeSquareObservations.contains(pawnSafe),
      claimLikeStory(
        pawnSafe.side,
        pawnSafe.square,
        pawnSafe.square,
        Line(pawnSafe.square, pawnSafe.square),
        pawnSafe.by
      )
    )

    val kingSeen =
      BoardFacts.fromFen(Fen.Full("4r1k1/6br/8/8/8/8/4N1R1/4K3 b - - 0 1")).toOption.get.seen
    val kingRingAttack = kingSeen.kingRingAttacks.find(_.square == Square('g', 7)).get
    val legalEscape = kingSeen.legalEscapeSquares.find(_.square == Square('h', 8)).get
    val contactCheck = kingSeen.contactCheckObservations.head
    cannotSpeak(
      "king-ring attack",
      boardFactExists = kingSeen.kingRingAttacks.contains(kingRingAttack),
      claimLikeStory(
        kingRingAttack.attacker.side,
        kingRingAttack.square,
        kingRingAttack.attacker.square,
        Line(kingRingAttack.attacker.square, kingRingAttack.square),
        kingRingAttack.side
      )
    )
    cannotSpeak(
      "legal escape square",
      boardFactExists = kingSeen.legalEscapeSquares.contains(legalEscape),
      claimLikeStory(
        legalEscape.side,
        legalEscape.square,
        legalEscape.king.square,
        legalEscape.line,
        rivalOf(legalEscape.side)
      )
    )
    cannotSpeak(
      "contact check observation",
      boardFactExists = kingSeen.contactCheckObservations.contains(contactCheck),
      claimLikeStory(
        contactCheck.attacker.side,
        contactCheck.king.square,
        contactCheck.attacker.square,
        contactCheck.line,
        contactCheck.side
      )
    )

  test("Story route endpoints encode differently and order deterministically"):
    val forward = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('d', 4)),
      anchor = Some(Square('e', 5)),
      route = Some(Line(Square('a', 2), Square('a', 4))),
      rival = Side.Black,
      proof =
        proof(boardProof = 82, ownerProof = 82, anchorProof = 82, routeProof = 82, conversionPrize = 82),
      storyProof = storyProof(Line(Square('a', 2), Square('a', 4)))
    )
    val reverse = forward.copy(
      route = Some(Line(Square('a', 4), Square('a', 2))),
      storyProof = StoryProof.empty
    )

    assertEquals(
      forward.values(Story.Slots.Pawn + Story.Identity.RouteFrom),
      Square('a', 2).index + 1
    )
    assertEquals(
      reverse.values(Story.Slots.Pawn + Story.Identity.RouteFrom),
      Square('a', 4).index + 1
    )
    assert(forward.values != reverse.values)
    assertEquals(StoryTable.choose(Vector(reverse, forward)).head.story, forward)
    assertEquals(StoryTable.choose(Vector(forward, reverse)).head.story, forward)

  test("Verdict values encode the fixed dense layout and all proof values"):
    val target = Square('f', 7)
    val anchor = Square('e', 6)
    val exactProof = Proof(
      boardProof = 61,
      lineProof = 62,
      ownerProof = 63,
      anchorProof = 64,
      routeProof = 65,
      persistence = 66,
      immediacy = 67,
      forcing = 68,
      conversionPrize = 69,
      counterplayRisk = 10,
      kingHeat = 71,
      pieceSupport = 72,
      pawnSupport = 73,
      sourceFit = 74,
      novelty = 75,
      clarity = 76
    )
    val story = Story(
      Scene.Tactic,
      side = Side.Black,
      target = Some(target),
      anchor = Some(anchor),
      rival = Side.White,
      tactic = Some(Tactic.Fork),
      proof = exactProof
    )
    val verdict = Verdict(story, rank = 2, leadAllowed = false, strength = 87.5, role = Role.Blocked)
    val values = verdict.values

    assertEquals(Verdict.Size, 110)
    assertEquals(values.size, Verdict.Size)
    assertEquals(Verdict.Slots.Role, 0)
    assertEquals(Verdict.Slots.Rank, 1)
    assertEquals(Verdict.Slots.LeadAllowed, 2)
    assertEquals(Verdict.Slots.Strength, 3)
    assertEquals(Verdict.Slots.Side, 4)
    assertEquals(Verdict.Slots.Rival, 5)
    assertEquals(Verdict.Slots.Target, 6)
    assertEquals(Verdict.Slots.Anchor, 7)
    assertEquals(Verdict.Slots.Scene, 8)
    assertEquals(Verdict.Slots.Plan, 37)
    assertEquals(Verdict.Slots.Tactic, 69)
    assertEquals(Verdict.Slots.Proof, 94)
    assertEquals(Verdict.Slots.End, Verdict.Size)
    assertEqualsDouble(values(Verdict.Slots.Role), Role.Blocked.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Rank), 2.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.LeadAllowed), 0.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Strength), 87.5, 0.0001)
    assertEqualsDouble(values(Verdict.Slots.Side), Side.Black.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Rival), Side.White.ordinal.toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Target), (target.index + 1).toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Anchor), (anchor.index + 1).toDouble, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Scene + Scene.Tactic.ordinal), 1.0, 0.0)
    assertEqualsDouble(values(Verdict.Slots.Tactic + Tactic.Fork.ordinal), 1.0, 0.0)
    assertEquals(values.slice(Verdict.Slots.Proof, Verdict.Slots.End).map(_.toInt), exactProof.values)
    assert(values.exists(_ != 0.0))

  test("Verdict proofFailures are internal diagnostics, not public payload"):
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
    val cleared = verdict.copy(proofFailures = Vector.empty)
    val verdictSlotNames =
      Verdict.Slots.getClass.getDeclaredMethods
        .map(_.getName)
        .filterNot(name => name.startsWith("$") || name.contains("bitmap"))
        .toVector
    val debugTerms = Vector("proofFailures", "MissingEvidence", "missing evidence", "renderer", "prompt")

    assert(verdict.proofFailures.nonEmpty)
    assertEquals(verdict.values, cleared.values)
    debugTerms.foreach: term =>
      assert(
        !verdictSlotNames.exists(_.toLowerCase.contains(term.toLowerCase)),
        s"Verdict public slots must not expose $term"
      )

  test("Stage 5-4 Verdict diagnostics do not enter public values"):
    val story = Story(
      Scene.Tactic,
      side = Side.White,
      target = Some(Square('e', 5)),
      anchor = Some(Square('d', 4)),
      route = Some(Line(Square('d', 4), Square('e', 5))),
      rival = Side.Black,
      tactic = Some(Tactic.Hanging),
      proof = proof(),
      storyProof = storyProof()
    )
    val base =
      Verdict(story, rank = 1, leadAllowed = true, strength = 80.0, role = Role.Lead)
    val withProofFailure =
      base.copy(proofFailures = Vector(BoardFacts.MissingEvidence("Story Proof", Vector("same-board proof"))))
    val withUnknownEngine =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Unknown))
    val withRefutingEngine =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Refutes))
    val withStrengthLimit =
      base.copy(engineCheckStatus = Some(EngineCheckStatus.Caps), engineStrengthLimited = true)

    assertEquals(base.values.size, Verdict.Size)
    assertEquals(withProofFailure.values, base.values)
    assertEquals(withUnknownEngine.values, base.values)
    assertEquals(withRefutingEngine.values, base.values)
    assertEquals(withStrengthLimit.values, base.values)

  test("Stage 6-1 builds structured ExplanationPlan for selected Hanging Lead Verdict"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head

    val plan = ExplanationPlan.fromSelected(verdict).get

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.Tactic)
    assertEquals(plan.tactic, Some(Tactic.Hanging))
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 5)))
    assertEquals(plan.anchor, Some(Square('d', 4)))
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.allowedClaim.map(_.key), Some("can_win_piece"))
    assert(!plan.allowedClaim.exists(_.key.contains(" ")))
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(
      plan.forbiddenWording.map(_.key),
      Vector(
        "free_piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best_move",
        "only_move",
        "engine_says",
        "no_counterplay",
        "king_unsafe",
        "file_control",
        "outpost",
        "strategic_key",
        "conversion",
        "mate_net"
      )
    )
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)

  test("Stage 6-1 ExplanationPlan rejects unselected or unsupported Verdicts"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val supportVerdict = StoryTable.choose(Vector(left, right))(1)
    val forgedForkLead = Verdict(
      story = right.copy(tactic = Some(Tactic.Fork)),
      rank = 1,
      leadAllowed = true,
      strength = 80.0,
      role = Role.Lead
    )
    val publicSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(supportVerdict.role, Role.Support)
    assertEquals(ExplanationPlan.fromSelected(forgedForkLead), None)
    Vector("sentence", "prose", "engineCheck", "EngineEval", "proofFailures").foreach: forbiddenName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6-2 maps Hanging Lead to safe allowed claim keys"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val allowedClaimKeys = ExplanationClaim.HangingAllowed.map(_.key)
    val forbiddenClaimKeys = ExplanationClaim.HangingForbiddenKeys

    assertEquals(
      allowedClaimKeys,
      Vector("can_win_piece", "piece_can_be_taken_with_gain", "capture_leaves_material_gain")
    )
    assertEquals(
      forbiddenClaimKeys,
      Vector(
        "free_piece",
        "blunder",
        "winning_tactic",
        "decisive_tactic",
        "forced_win",
        "best_move",
        "no_counterplay",
        "engine_approved"
      )
    )
    assert(plan.allowedClaim.exists(claim => allowedClaimKeys.contains(claim.key)))
    assert(!plan.allowedClaim.exists(claim => forbiddenClaimKeys.contains(claim.key)))

  test("Stage 6-2 only Lead Verdict produces an allowed claim plan"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    assert(
      ExplanationPlan
        .fromSelected(leadVerdict)
        .exists(plan => plan.allowedClaim.contains(ExplanationClaim.CanWinPiece))
    )
    Vector(supportVerdict, contextVerdict, blockedVerdict).foreach: verdict =>
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), None)

  test("Stage 6-2 engineStrengthLimited suppresses claim and strengthens forbidden wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val plainVerdict = StoryTable.choose(Vector(story)).head
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
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val plainPlan = ExplanationPlan.fromSelected(plainVerdict).get
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get

    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(plainPlan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(cappedPlan.allowedClaim, None)
    assert(!plainPlan.forbiddenWording.map(_.key).contains("strong_wording"))
    assert(cappedPlan.forbiddenWording.map(_.key).contains("strong_wording"))

  test("Stage 6-3 gives ExplanationPlan a stronger forbidden wording boundary"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenKeys = plan.forbiddenWording.map(_.key)

    assertEquals(
      ForbiddenWording.Basic.map(_.key),
      Vector(
        "free_piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best_move",
        "only_move",
        "engine_says",
        "no_counterplay",
        "king_unsafe",
        "file_control",
        "outpost",
        "strategic_key",
        "conversion",
        "mate_net"
      )
    )
    assertEquals(forbiddenKeys, ForbiddenWording.Basic.map(_.key))
    assert(forbiddenKeys.size > ExplanationClaim.HangingAllowed.size)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)

  test("Stage 6-3 engineStrengthLimited adds strong wording prohibition without a claim"):
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
    val cappedVerdict = StoryTable.choose(Vector(capped)).head
    val cappedPlan = ExplanationPlan.fromSelected(cappedVerdict).get

    assertEquals(cappedVerdict.engineStrengthLimited, true)
    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(
      cappedPlan.forbiddenWording.map(_.key),
      ForbiddenWording.Basic.map(_.key) :+ "strong_wording"
    )

  test("Stage 6-4 keeps Support and Context as relation-only plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val supportPlan = ExplanationPlan.fromSelected(supportVerdict).get
    val contextPlan = ExplanationPlan.fromSelected(contextVerdict).get

    assertEquals(
      ExplanationPlan.fromSelected(leadVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.CanWinPiece)
    )
    assertEquals(supportPlan.allowedClaim, None)
    assertEquals(supportPlan.relations.map(_.key), Vector("same_family_lower_rank"))
    assertEquals(supportPlan.debugOnly, false)
    assertEquals(contextPlan.allowedClaim, None)
    assertEquals(contextPlan.relations.map(_.key), Vector("alternative_hanging_candidate"))
    assertEquals(contextPlan.debugOnly, false)

  test("Stage 6-4 records capped and engine-refuted relations without proofFailure wording"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

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

    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head).get
    val blockedVerdict = StoryTable
      .choose(Vector(checked(EngineCheckStatus.Refutes)))
      .head
      .copy(
        proofFailures = Vector(BoardFacts.MissingEvidence("Story Proof", Vector("must never become wording")))
      )
    val blockedPlan = ExplanationPlan.fromSelected(blockedVerdict).get
    val publicSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(cappedPlan.allowedClaim, None)
    assertEquals(cappedPlan.relations.map(_.key), Vector("capped_same_story"))
    assertEquals(blockedPlan.allowedClaim, None)
    assertEquals(blockedPlan.relations.map(_.key), Vector("blocked_by_engine_refute"))
    assertEquals(blockedPlan.debugOnly, true)
    Vector("proofFailures", "must never become wording", "debugText").foreach: forbiddenName =>
      assert(!publicSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6-5 ExplanationPlan accepts selected Verdict only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val selectedVerdict = StoryTable.choose(Vector(story)).head
    val unselectedVerdict = selectedVerdict.copy(selected = false)

    assertEquals(selectedVerdict.selected, true)
    assert(
      ExplanationPlan
        .fromSelected(selectedVerdict)
        .exists(_.allowedClaim.contains(ExplanationClaim.CanWinPiece))
    )
    assertEquals(ExplanationPlan.fromSelected(unselectedVerdict), None)
    assertEquals(selectedVerdict.values, unselectedVerdict.values)

  test("Stage 6-5 ExplanationPlan exposes no raw proof material input"):
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

    assertEquals(
      fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    Vector(
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "MultiTargetProof",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "Story",
      "String",
      "Source"
    ).foreach: forbiddenType =>
      assert(
        !fromSelectedParameterNames.contains(forbiddenType),
        s"ExplanationPlan must not accept $forbiddenType"
      )
    Vector(
      "boardFacts",
      "boardMood",
      "rootAtoms",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 6 closeout negative corpus creates no public claim outside uncapped Lead"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadVerdict = verdicts.find(_.role == Role.Lead).get
    val supportVerdict = verdicts.find(_.role == Role.Support).get
    val contextVerdict = leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3)
    val blockedVerdict = leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)

    def checked(status: EngineCheckStatus) =
      val check = EngineCheck.fromStory(
        facts = facts,
        story = Some(right),
        engineLine = Some(EngineLine(Vector(Line(Square('d', 4), Square('e', 5))))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(20)),
        evalAfter = Some(EngineEval(80)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )
      StoryTable.choose(Vector(TacticHanging.withEngineCheck(right, check).get)).head

    val cappedVerdict = checked(EngineCheckStatus.Caps)
    val refutedVerdict = checked(EngineCheckStatus.Refutes)
    val negativePlans =
      Vector(supportVerdict, contextVerdict, blockedVerdict, cappedVerdict, refutedVerdict)
        .flatMap(ExplanationPlan.fromSelected)

    assertEquals(
      ExplanationPlan.fromSelected(leadVerdict).flatMap(_.allowedClaim),
      Some(ExplanationClaim.CanWinPiece)
    )
    assertEquals(negativePlans.map(_.allowedClaim), Vector.fill(negativePlans.size)(None))
    assert(negativePlans.exists(_.relations.contains(ExplanationRelation.CappedSameStory)))
    assert(negativePlans.exists(_.relations.contains(ExplanationRelation.BlockedByEngineRefute)))
    assertEquals(ExplanationPlan.fromSelected(refutedVerdict).map(_.debugOnly), Some(true))

  test("Stage 6 closeout runtime surface stays pre-render"):
    val fromSelectedMethods =
      ExplanationPlan.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromSelected")
    val planSurfaceNames =
      classOf[ExplanationPlan].getDeclaredMethods.map(_.getName).toSet ++
        classOf[ExplanationPlan].getDeclaredFields.map(_.getName).toSet

    assertEquals(
      fromSelectedMethods.map(_.getParameterTypes.toVector.map(_.getSimpleName)),
      Vector(Vector("Verdict"))
    )
    Vector(
      "render",
      "renderer",
      "llm",
      "sentence",
      "prose",
      "publicRoute",
      "pedagogy",
      "engineExplanation",
      "bestMove",
      "engineEval",
      "captureResult",
      "boardFacts"
    ).foreach: forbiddenName =>
      assert(!planSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 7-1 DeterministicRenderer accepts ExplanationPlan only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val fromPlanParameterNames =
      fromPlanMethods
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet

    assert(rendered.text.nonEmpty)
    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    assertEquals(fromPlanParameterNames, Vector("ExplanationPlan"))
    Vector(
      "fromVerdict",
      "fromStory",
      "fromBoardFacts",
      "fromBoardMood",
      "fromCaptureResult",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine"
    ).foreach: forbiddenMethod =>
      assert(
        !rendererMethodNames.contains(forbiddenMethod),
        s"DeterministicRenderer must not expose $forbiddenMethod"
      )

  test("Stage 7-1 DeterministicRenderer cannot create text without an ExplanationPlan"):
    val rendererNoArgTextMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter: method =>
        method.getParameterTypes.isEmpty && method.getReturnType.getSimpleName.contains("RenderedLine")
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .mkString(" ")
    val textSurfaceNames =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet

    assertEquals(rendererNoArgTextMethods, Vector.empty)
    Vector(
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine"
    ).foreach: forbiddenType =>
      assert(
        !rendererParameterNames.contains(forbiddenType),
        s"DeterministicRenderer must not accept $forbiddenType"
      )
    Vector(
      "verdict",
      "story",
      "boardFacts",
      "boardMood",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow"
    ).foreach: forbiddenName =>
      assert(!textSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 7-2 renders only the minimal CanWinPiece Hanging template"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "free piece",
        "blunder",
        "winning",
        "decisive",
        "forced",
        "best move",
        "only move",
        "engine says",
        "no counterplay",
        "king unsafe",
        "file control",
        "outpost"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.route, Some(capture))
    assertEquals(plan.routeSan, Some("dxe5"))
    assertEquals(plan.evidenceLine, Some(capture))
    assertEquals(plan.target, Some(Square('e', 5)))
    assert(plan.forbiddenWording.nonEmpty)
    assertEquals(rendered.text, "dxe5 wins material against the piece on e5.")
    forbiddenPhrases.foreach: phrase =>
      assert(
        !rendered.text.toLowerCase.contains(phrase),
        s"template must not contain forbidden phrase: $phrase"
      )

  test("Stage 7-2 refuses missing or non CanWinPiece template prerequisites"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val invalidPlans =
      Vector(
        plan.copy(role = Role.Support),
        plan.copy(allowedClaim = Some(ExplanationClaim.PieceCanBeTakenWithGain)),
        plan.copy(allowedClaim = None),
        plan.copy(debugOnly = true),
        plan.copy(route = None),
        plan.copy(routeSan = None),
        plan.copy(evidenceLine = None),
        plan.copy(target = None),
        plan.copy(forbiddenWording = Vector.empty)
      )

    invalidPlans.foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None)

  test("Stage 7-3 enforces forbidden wording before renderer output"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val lowerText = rendered.text.toLowerCase
    val forbiddenMeanings =
      Vector(
        "free piece",
        "blunder",
        "winning position",
        "decisive",
        "forced",
        "best move",
        "only move",
        "engine says",
        "no counterplay",
        "king unsafe",
        "file control",
        "outpost",
        "strategic key",
        "conversion",
        "mate net"
      )
    val strongBlockedPlan =
      plan.copy(forbiddenWording = plan.forbiddenWording :+ ForbiddenWording.StrongWording)

    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assert(lowerText.contains("wins material"))
    forbiddenMeanings.foreach: phrase =>
      assert(!lowerText.contains(phrase), s"renderer output must not contain forbidden phrase: $phrase")
    assertEquals(DeterministicRenderer.fromPlan(strongBlockedPlan), None)

  test("Stage 7-3 refuses engine-limited no-claim and debug-only plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
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
    val cappedPlan =
      ExplanationPlan
        .fromSelected(StoryTable.choose(Vector(TacticHanging.withEngineCheck(story, check).get)).head)
        .get

    assertEquals(cappedPlan.allowedClaim, None)
    assert(cappedPlan.forbiddenWording.contains(ForbiddenWording.StrongWording))
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(plan.copy(allowedClaim = None)), None)
    assertEquals(DeterministicRenderer.fromPlan(plan.copy(debugOnly = true)), None)

  test("Stage 7-4 renders no standalone text for Support Context or Blocked plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val right = TacticHanging.write(facts, Line(Square('d', 4), Square('e', 5))).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val contextPlan =
      leadPlan.copy(
        role = Role.Context,
        allowedClaim = None,
        relations = Vector(ExplanationRelation.AlternativeHangingCandidate)
      )
    val blockedPlan =
      leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)

    assert(leadPlan.allowedClaim.nonEmpty)
    assert(DeterministicRenderer.fromPlan(leadPlan).nonEmpty)
    assertEquals(DeterministicRenderer.fromPlan(supportPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(contextPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(blockedPlan), None)
    assertEquals(DeterministicRenderer.fromPlan(leadPlan.copy(role = Role.Lead, allowedClaim = None)), None)

  test("Stage 7-4 renders no text for capped or engine-refuted relation plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get

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

    val cappedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Caps))).head).get
    val refutedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(checked(EngineCheckStatus.Refutes))).head).get

    assertEquals(cappedPlan.allowedClaim, None)
    assert(cappedPlan.relations.contains(ExplanationRelation.CappedSameStory))
    assertEquals(DeterministicRenderer.fromPlan(cappedPlan), None)
    assertEquals(refutedPlan.allowedClaim, None)
    assert(refutedPlan.relations.contains(ExplanationRelation.BlockedByEngineRefute))
    assertEquals(DeterministicRenderer.fromPlan(refutedPlan), None)

  test("Stage 7-5 RenderedLine carries only text claim strength and forbidden check"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fieldNames =
      rendered.getClass.getDeclaredFields.map(_.getName).filterNot(_.startsWith("$")).toVector
    def fieldValue(name: String) =
      val field = rendered.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(rendered)

    assertEquals(rendered.getClass.getSimpleName, "RenderedLine")
    assertEquals(fieldNames, Vector("text", "claimKey", "strength", "forbiddenCheckPassed"))
    assertEquals(fieldValue("text"), "dxe5 wins material against the piece on e5.")
    assertEquals(fieldValue("claimKey"), "can_win_piece")
    assertEquals(fieldValue("strength"), "bounded")
    assertEquals(fieldValue("forbiddenCheckPassed").asInstanceOf[Boolean], true)

  test("Stage 7-5 RenderedLine owns no proof engine board source or route analysis fields"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val renderedLineSurfaceNames =
      (rendered.getClass.getDeclaredFields.map(_.getName).toSet ++
        rendered.getClass.getDeclaredMethods.map(_.getName).toSet)
        .map(_.toLowerCase)

    Vector(
      "captureResult",
      "engineCheck",
      "boardFacts",
      "proofFailures",
      "rawRouteAnalysis",
      "sourceRow",
      "engineEval",
      "engineLine",
      "rawPv",
      "route",
      "proof"
    ).foreach: forbiddenName =>
      assert(
        !renderedLineSurfaceNames.exists(_.contains(forbiddenName.toLowerCase)),
        s"RenderedLine must not expose $forbiddenName"
      )

  test("Stage 7-6 baseline renders Lead CanWinPiece bounded text no stronger than plan"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenPhrases =
      Vector(
        "engine",
        "best move",
        "blunder",
        "free piece",
        "decisive",
        "forced",
        "winning position"
      )

    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.CanWinPiece))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(rendered.text, "dxe5 wins material against the piece on e5.")
    assertEquals(rendered.claimKey, "can_win_piece")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)
    forbiddenPhrases.foreach: phrase =>
      assert(!rendered.text.toLowerCase.contains(phrase), s"baseline renderer must not mention: $phrase")

  test("Stage 7-6 baseline rejects non Lead no-claim engine-limited and forbidden plans"):
    val facts = BoardFacts.fromFen("4k3/8/8/2n1n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val left = TacticHanging.write(facts, Line(Square('d', 4), Square('c', 5))).get
    val rightCapture = Line(Square('d', 4), Square('e', 5))
    val right = TacticHanging.write(facts, rightCapture).get
    val verdicts = StoryTable.choose(Vector(left, right))
    val leadPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Lead).get).get
    val supportPlan = ExplanationPlan.fromSelected(verdicts.find(_.role == Role.Support).get).get
    val contextPlan =
      leadPlan.copy(
        role = Role.Context,
        allowedClaim = None,
        relations = Vector(ExplanationRelation.AlternativeHangingCandidate)
      )
    val blockedPlan =
      leadPlan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)
    val debugOnlyPlan = leadPlan.copy(debugOnly = true)
    val noClaimPlan = leadPlan.copy(allowedClaim = None)
    val forbiddenPlan =
      leadPlan.copy(forbiddenWording = leadPlan.forbiddenWording :+ ForbiddenWording.StrongWording)
    val cappedCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(right),
      engineLine = Some(EngineLine(Vector(rightCapture))),
      replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
      evalBefore = Some(EngineEval(20)),
      evalAfter = Some(EngineEval(80)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Caps
    )
    val cappedPlan =
      ExplanationPlan
        .fromSelected(StoryTable.choose(Vector(TacticHanging.withEngineCheck(right, cappedCheck).get)).head)
        .get

    Vector(
      supportPlan,
      contextPlan,
      blockedPlan,
      debugOnlyPlan,
      noClaimPlan,
      cappedPlan,
      forbiddenPlan
    ).foreach: plan =>
      assertEquals(DeterministicRenderer.fromPlan(plan), None)

  test("Stage 7-6 baseline renderer exposes no Verdict or EngineCheck input"):
    val methodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)

    assert(!methodNames.contains("fromVerdict"))
    assert(!methodNames.contains("fromEngineCheck"))
    assert(!parameterNames.contains("Verdict"))
    assert(!parameterNames.contains("EngineCheck"))

  test("Stage 8A mock narrator echoes safe RenderedLine text only"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val mockText = LlmNarrationSmoke.mockNarrate(plan, rendered).get
    val checked = LlmNarrationSmoke.check(plan, rendered, mockText)
    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    val contextPlan = plan.copy(role = Role.Context, allowedClaim = None)
    val blockedPlan = plan.copy(role = Role.Blocked, allowedClaim = None, debugOnly = true)

    assertEquals(mockText, rendered.text)
    assertEquals(checked.accepted, true)
    assertEquals(checked.violations, Vector.empty)
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, Option.empty[RenderedLine]), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(contextPlan, Some(rendered)), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(blockedPlan, Some(rendered)), None)

  test("Stage 8B Codex CLI prompt smoke uses only rendered text contract"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: dxe5 wins material against the piece on e5.",
      "claimKey: can_win_piece",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"prompt must include allowed input: $required")
    Vector(
      "ExplanationPlan",
      "FEN",
      "PGN",
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "raw PV",
      "proofFailures",
      "source row",
      "role:",
      "scene:",
      "tactic:",
      "side:",
      "target:",
      "route:",
      "evidence line:"
    ).foreach: forbidden =>
      assert(!prompt.contains(forbidden), s"prompt must not include forbidden raw input label: $forbidden")

  test("Stage 8 smoke checker rejects forbidden wording stronger claims and invented lines"):
    val facts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val capture = Line(Square('d', 4), Square('e', 5))
    val story = TacticHanging.write(facts, capture).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val engineBestWinning =
      LlmNarrationSmoke.check(plan, rendered, "The engine says dxe5 is the best move and a winning position.")
    val inventedLine =
      LlmNarrationSmoke.check(plan, rendered, "After dxe5 Ke7, White wins material.")
    val inventedTactic =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 starts a fork and a strategic plan.")
    val inventedCauseAndEval =
      LlmNarrationSmoke.check(plan, rendered, "dxe5 works because White is better afterward.")
    val freePiece =
      LlmNarrationSmoke.check(plan, rendered, "White wins a free piece on e5.")

    assertEquals(engineBestWinning.accepted, false)
    assertEquals(engineBestWinning.violations.contains("forbidden_wording"), true)
    assertEquals(engineBestWinning.violations.contains("stronger_claim"), true)
    assertEquals(inventedLine.accepted, false)
    assertEquals(inventedLine.violations.contains("new_move_or_line"), true)
    assertEquals(inventedTactic.accepted, false)
    assertEquals(inventedTactic.violations.contains("new_tactic_or_plan"), true)
    assertEquals(inventedCauseAndEval.accepted, false)
    assertEquals(inventedCauseAndEval.violations.contains("new_cause_or_evaluation"), true)
    assertEquals(freePiece.accepted, false)
    assertEquals(freePiece.violations.contains("forbidden_wording"), true)
    assertEquals(freePiece.violations.contains("stronger_claim"), true)

  test("Stage 8 narration smoke exposes no raw proof or production API input"):
    val methodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val parameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
    val resultSurfaceNames =
      classOf[NarrationSmokeCheck].getDeclaredFields.map(_.getName).toSet ++
        classOf[NarrationSmokeCheck].getDeclaredMethods.map(_.getName).toSet

    assertEquals(parameterNames.contains("ExplanationPlan"), true)
    assertEquals(parameterNames.contains("RenderedLine"), true)
    Vector(
      "fromVerdict",
      "fromStory",
      "fromBoardFacts",
      "fromBoardMood",
      "fromCaptureResult",
      "fromEngineCheck",
      "fromEngineEval",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: forbiddenMethod =>
      assert(!methodNames.contains(forbiddenMethod), s"Stage 8 smoke must not expose $forbiddenMethod")
    Vector(
      "Verdict",
      "Story",
      "BoardFacts",
      "BoardMood",
      "CaptureResult",
      "EngineCheck",
      "EngineEval",
      "EngineLine"
    ).foreach: forbiddenType =>
      assert(!parameterNames.contains(forbiddenType), s"Stage 8 smoke must not accept $forbiddenType")
    Vector(
      "verdict",
      "story",
      "boardFacts",
      "boardMood",
      "captureResult",
      "engineCheck",
      "engineEval",
      "engineLine",
      "rawPv",
      "proofFailures",
      "sourceRow",
      "model",
      "temperature",
      "responseFormat",
      "retry",
      "timeout"
    ).foreach: forbiddenName =>
      assert(!resultSurfaceNames.exists(_.toLowerCase.contains(forbiddenName.toLowerCase)))

  test("Stage 2 ordering does not use proofFailures as public sort input"):
    val proofScore = proof(
      boardProof = 99,
      lineProof = 99,
      ownerProof = 99,
      anchorProof = 99,
      routeProof = 99,
      conversionPrize = 99
    )
    val incomplete = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('a', 1)),
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.Black,
      proof = proofScore
    )
    val complete = incomplete.copy(
      target = Some(Square('b', 1)),
      storyProof = storyProof()
    )
    val verdicts = StoryTable.choose(Vector(complete, incomplete))

    assert(incomplete.proofFailures.nonEmpty)
    assertEquals(complete.proofFailures, Vector.empty)
    assertEquals(verdicts.head.story, incomplete)
    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))

  test("StoryTable chooses at most eight deterministic verdicts"):
    val stories =
      Vector.tabulate(10): i =>
        Story(
          Scene.Material,
          side = Side.White,
          route = Some(safeRoute),
          proof = proof(
            boardProof = 90 - i,
            ownerProof = 90,
            anchorProof = 90,
            routeProof = 90,
            conversionPrize = 90 - i
          )
        )

    val verdicts = StoryTable.choose(stories)

    assertEquals(StoryTable.TopK, 8)
    assertEquals(verdicts.size, StoryTable.TopK)
    assert(verdicts.map(_.strength).sliding(2).forall(pair => pair.size == 1 || pair(0) >= pair(1)))
    assertEquals(verdicts.map(_.rank), (1 to 8).toVector)

  test("StoryTable requires named positive Story writers for public leads"):
    val low =
      Story(
        Scene.Material,
        side = Side.White,
        proof =
          proof(boardProof = 64, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90)
      )
    val risky =
      Story(
        Scene.King,
        side = Side.White,
        route = Some(safeRoute),
        proof = proof(
          boardProof = 80,
          ownerProof = 90,
          anchorProof = 90,
          routeProof = 90,
          conversionPrize = 90,
          counterplayRisk = 71
        )
      )
    val solid =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 90)
      )

    val verdicts = StoryTable.choose(Vector(low, risky, solid))

    assertEquals(StoryTable.PublicStoryLeadsRequireNamedProofWriters, true)
    assertEquals(verdicts.find(_.story == solid).map(_.role), Some(Role.Blocked))
    assertEquals(verdicts.find(_.story == solid).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == low).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == risky).map(_.leadAllowed), Some(false))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Quiet remains blocked without same-root proof sidecars"):
    val quiet =
      Story(
        Scene.Quiet,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90)
      )
    val material =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90)
      )

    assertEquals(StoryTable.choose(Vector(quiet)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(quiet)).head.leadAllowed, false)
    assertEquals(
      StoryTable.choose(Vector(quiet, material)).find(_.story == quiet).map(_.leadAllowed),
      Some(false)
    )

  test("Tactic heat does not open Stage 3 priority without named Hanging writer"):
    val plan = Story(
      Scene.Plan,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      plan = Some(Plan.Minority),
      rival = Side.Black,
      target = Some(Square('a', 2)),
      proof =
        proof(boardProof = 80, ownerProof = 80, anchorProof = 80, routeProof = 80, conversionPrize = 90),
      storyProof = storyProof()
    )
    val tactic = Story(
      Scene.Tactic,
      side = Side.Black,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      rival = Side.White,
      target = Some(Square('a', 2)),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 80,
        ownerProof = 80,
        anchorProof = 80,
        routeProof = 80,
        lineProof = 70,
        forcing = 95,
        conversionPrize = 95,
        kingHeat = 95,
        immediacy = 95
      ),
      storyProof = storyProof()
    )

    val verdicts = StoryTable.choose(Vector(plan, tactic))

    assertEquals(verdicts.head.story, plan)
    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Plan ordering remains deterministic while only Hanging writer can lead"):
    val plan = Story(
      Scene.Plan,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      target = Some(Square('a', 2)),
      rival = Side.Black,
      plan = Some(Plan.CenterBreak),
      proof =
        proof(boardProof = 85, ownerProof = 85, anchorProof = 85, routeProof = 85, conversionPrize = 95),
      storyProof = storyProof()
    )
    val sameSideTactic = Story(
      Scene.Tactic,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      target = Some(Square('a', 2)),
      rival = Side.Black,
      tactic = Some(Tactic.Fork),
      proof =
        proof(boardProof = 75, ownerProof = 75, anchorProof = 75, routeProof = 75, conversionPrize = 95),
      storyProof = storyProof()
    )
    val opposingTactic = sameSideTactic.copy(side = Side.Black, rival = Side.White)

    assertEquals(
      StoryTable.choose(Vector(plan, sameSideTactic)).find(_.story == plan).map(_.leadAllowed),
      Some(false)
    )

    val verdicts = StoryTable.choose(Vector(plan, opposingTactic))

    assertEquals(verdicts.find(_.story == plan).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.head.story, plan)
    assert(verdicts.forall(_.role != Role.Lead))

  test("Source remains non-lead while only Hanging writer can lead"):
    val source =
      Story(
        Scene.Source,
        side = Side.White,
        route = Some(safeRoute),
        proof =
          proof(boardProof = 90, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 95)
      )
    val boardBacked =
      Story(
        Scene.Material,
        side = Side.White,
        anchor = Some(safeAnchor),
        route = Some(safeRoute),
        proof =
          proof(boardProof = 70, ownerProof = 90, anchorProof = 90, routeProof = 90, conversionPrize = 95)
      )

    val verdicts = StoryTable.choose(Vector(source, boardBacked))

    assertEquals(verdicts.find(_.story == source).map(_.leadAllowed), Some(false))
    assertEquals(verdicts.find(_.story == boardBacked).map(_.leadAllowed), Some(false))
    assert(verdicts.forall(_.role != Role.Lead))

  test("Broad pin stories and writerless Pin remain blocked"):
    val absPin = Story(
      Scene.Tactic,
      side = Side.White,
      anchor = Some(safeAnchor),
      route = Some(safeRoute),
      tactic = Some(Tactic.AbsPin),
      proof = proof(
        boardProof = 80,
        ownerProof = 80,
        anchorProof = 80,
        routeProof = 80,
        lineProof = 70,
        forcing = 90
      )
    )
    val relPin = absPin.copy(tactic = Some(Tactic.RelPin))
    val writerlessPin = absPin.copy(tactic = Some(Tactic.Pin))

    assertEquals(StoryTable.choose(Vector(absPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(relPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(writerlessPin)).head.role, Role.Blocked)
    assertEquals(StoryTable.choose(Vector(absPin)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(relPin)).head.leadAllowed, false)
    assertEquals(StoryTable.choose(Vector(writerlessPin)).head.leadAllowed, false)
    assertEquals(absPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)
    assertEquals(relPin.tactic.exists(t => t == Tactic.AbsPin || t == Tactic.RelPin), true)
    assertEquals(writerlessPin.tactic.contains(Tactic.Pin), true)

  test("Pin tactic tags on non-Tactic scenes cannot lead"):
    val falsePin = Story(
      Scene.Plan,
      side = Side.White,
      route = Some(safeRoute),
      plan = Some(Plan.Reroute),
      tactic = Some(Tactic.AbsPin),
      proof = proof(
        boardProof = 85,
        ownerProof = 85,
        anchorProof = 85,
        routeProof = 85,
        lineProof = 75,
        forcing = 90
      )
    )

    val verdict = StoryTable.choose(Vector(falsePin)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("Non-Tactic scenes with tactic tags do not get tactical lead treatment"):
    val falseFork = Story(
      Scene.Material,
      side = Side.White,
      route = Some(safeRoute),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 85,
        ownerProof = 85,
        anchorProof = 85,
        routeProof = 85,
        lineProof = 75,
        forcing = 90
      )
    )

    val verdict = StoryTable.choose(Vector(falseFork)).head

    assertEquals(verdict.leadAllowed, false)
    assert(verdict.role != Role.Lead)

  test("StoryTable tie break includes route identity"):
    val bRoute = Story(
      Scene.Material,
      side = Side.White,
      target = Some(Square('d', 4)),
      anchor = Some(Square('e', 5)),
      route = Some(Line(Square('b', 1), Square('b', 8))),
      rival = Side.Black,
      proof = proof(boardProof = 82, ownerProof = 82, anchorProof = 82, routeProof = 82, conversionPrize = 82)
    )
    val aRoute = bRoute.copy(
      route = Some(Line(Square('a', 1), Square('a', 8)))
    )

    assertEquals(StoryTable.choose(Vector(bRoute, aRoute)).head.story, aRoute)
    assertEquals(StoryTable.choose(Vector(aRoute, bRoute)).head.story, aRoute)

  test("StoryTable fail-closes forged owner, anchor, route, line, and tactic claims"):
    val ownerless = Story(
      Scene.Material,
      side = Side.None,
      route = Some(safeRoute),
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 90,
        conversionPrize = 95
      )
    )
    val anchorless = ownerless.copy(side = Side.White, anchor = None, route = Some(safeRoute))
    val routeless = ownerless.copy(side = Side.White, route = None)
    val lineMissing = Story(
      Scene.Tactic,
      side = Side.White,
      route = Some(safeRoute),
      tactic = Some(Tactic.Fork),
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 0,
        conversionPrize = 95
      )
    )
    val motifMissing = lineMissing.copy(
      tactic = None,
      proof = proof(
        boardProof = 90,
        ownerProof = 90,
        anchorProof = 90,
        routeProof = 90,
        lineProof = 90,
        conversionPrize = 95
      )
    )

    val verdicts = StoryTable.choose(Vector(ownerless, anchorless, routeless, lineMissing, motifMissing))

    assert(verdicts.forall(!_.leadAllowed))
    assert(verdicts.forall(_.role != Role.Lead))

  test("FileOpened-0 proves only a legal pawn move opens its origin file"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val proof = FileOpenedProof.fromBoardFacts(facts, move)
    val story = SceneFileOpened.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.sameBoardProof, true)
    assertEquals(proof.legalPawnMove, true)
    assertEquals(proof.leavesOriginFile, true)
    assertEquals(proof.originFileOccupiedBeforeByMovingPawn, true)
    assertEquals(proof.originFileOpenBefore, false)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.originFileOpenAfter, true)
    assertEquals(proof.originFile, Some(4))
    assertEquals(proof.openedFile, Some(4))

    assertEquals(story.scene, Scene.FileOpened)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.SceneFileOpened))
    assertEquals(story.target, Some(Square('d', 6)))
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.route, Some(move))
    assertEquals(story.openedFile, Some(4))
    assertEquals(story.fileOpenedProof.exists(_.complete), true)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.passedPawnCreatedProof, None)
    assertEquals(story.captureResult, None)

    assertEquals(verdict.role, Role.Lead)
    assertEquals(plan.allowedClaim.map(_.key), Some("opens_file"))
    assertEquals(ExplanationClaim.FileOpenedAllowed.map(_.key), Vector("opens_file"))
    assertEquals(rendered.text, "exd6 opens the e-file.")
    assertEquals(rendered.claimKey, "opens_file")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

  test("FileOpened-1 FileOpenedProof owns exact ordinary non-promotion open-file event proof"):
    val captureFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureMove = Line(Square('e', 5), Square('d', 6))
    val captureProof = FileOpenedProof.fromBoardFacts(captureFacts, captureMove)

    assertEquals(captureProof.complete, true)
    assertEquals(captureProof.publicClaimAllowed, false)
    assertEquals(captureProof.side, Side.White)
    assertEquals(captureProof.rivalSide, Side.Black)
    assertEquals(captureProof.pawnBefore, Some(Piece(Side.White, Man.Pawn, Square('e', 5))))
    assertEquals(captureProof.pawnAfter, Some(Piece(Side.White, Man.Pawn, Square('d', 6))))
    assertEquals(captureProof.originSquare, Some(Square('e', 5)))
    assertEquals(captureProof.destinationSquare, Some(Square('d', 6)))
    assertEquals(captureProof.originFile, Some(4))
    assertEquals(captureProof.openedFile, Some(4))
    assertEquals(captureProof.openingMove, Some(captureMove))
    assertEquals(captureProof.legalPawnMove, true)
    assertEquals(captureProof.nonPromotionMove, true)
    assertEquals(captureProof.ordinaryPawnMoveOrCapture, true)
    assertEquals(captureProof.enPassantMove, false)
    assertEquals(captureProof.sameBoardProof, true)
    assertEquals(captureProof.originFileOccupiedBeforeByMovingPawn, true)
    assertEquals(captureProof.exactAfterBoardReplay, true)
    assertEquals(captureProof.afterBoardHasNoWhitePawnOnOriginFile, true)
    assertEquals(captureProof.afterBoardHasNoBlackPawnOnOriginFile, true)
    assertEquals(captureProof.openedFileIsOriginFile, true)

  test("FileOpened-1 FileOpenedProof rejects promotion en passant and destination-file openings"):
    val promotionFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('d', 8))
    val enPassantFacts = BoardFacts.fromFen("k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val enPassantMove = Line(Square('e', 5), Square('d', 6))
    val destinationFileFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val destinationFileMove = Line(Square('e', 5), Square('d', 6))
    val destinationFileProof = FileOpenedProof
      .fromBoardFacts(destinationFileFacts, destinationFileMove)
      .copy(openedFile = Some(3), openedFileIsOriginFile = false)

    val promotionProof = FileOpenedProof.fromBoardFacts(promotionFacts, promotionMove)
    val enPassantProof = FileOpenedProof.fromBoardFacts(enPassantFacts, enPassantMove)

    assertEquals(promotionProof.complete, false)
    assertEquals(promotionProof.nonPromotionMove, false)
    assertEquals(SceneFileOpened.write(promotionFacts, promotionMove), None)

    assertEquals(enPassantProof.complete, false)
    assertEquals(enPassantProof.enPassantMove, true)
    assertEquals(enPassantProof.ordinaryPawnMoveOrCapture, false)
    assertEquals(SceneFileOpened.write(enPassantFacts, enPassantMove), None)

    assertEquals(destinationFileProof.complete, false)
    assertEquals(destinationFileProof.openedFileIsOriginFile, false)
    assertEquals(
      StoryTable.choose(Vector(SceneFileOpened.write(destinationFileFacts, destinationFileMove).get.copy(fileOpenedProof = Some(destinationFileProof)))).head.role,
      Role.Blocked
    )

  test("FileOpened-2 SceneFileOpened writer pins destination target and origin-file identity"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val proof = story.fileOpenedProof.get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(story.scene, Scene.FileOpened)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.SceneFileOpened))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('d', 6)))
    assertEquals(story.anchor, Some(Square('e', 5)))
    assertEquals(story.route, Some(move))
    assertEquals(story.openedFile, Some(4))
    assertEquals(proof.destinationSquare, story.target)
    assertEquals(proof.originSquare, story.anchor)
    assertEquals(proof.openedFile, proof.originFile)
    assertEquals(proof.openedFile, Some(4))
    assertEquals(plan.target, Some(Square('d', 6)))
    assertEquals(plan.anchor, Some(Square('e', 5)))
    assertEquals(rendered.text, "exd6 opens the e-file.")

  test("FileOpened-2 SceneFileOpened blocks forged identities and refuted engine rows"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(move))),
      evalBefore = Some(EngineEval(200)),
      evalAfter = Some(EngineEval(-200)),
      depth = Some(12),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )

    Vector(
      "material scene" -> story.copy(scene = Scene.Material),
      "passed pawn scene" -> story.copy(scene = Scene.PassedPawnCreated),
      "pawn break proof contamination" -> story.copy(pawnBreakProof = Some(PawnBreakProof.fromBoardFacts(facts, move))),
      "pawn capture proof contamination" -> story.copy(pawnCaptureProof = Some(PawnCaptureProof.fromBoardFacts(facts, move))),
      "wrong target" -> story.copy(target = Some(Square('e', 5))),
      "wrong anchor" -> story.copy(anchor = Some(Square('d', 6)))
    ).foreach: (label, forged) =>
      assertEquals(StoryTable.choose(Vector(forged)).head.role, Role.Blocked, label)

    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertEquals(SceneFileOpened.withEngineCheck(story, refutingCheck).isDefined, true)
    assertEquals(StoryTable.choose(Vector(SceneFileOpened.withEngineCheck(story, refutingCheck).get)).head.role, Role.Blocked)

  test("FileOpened-3 negative corpus requires complete FileOpenedProof or silence"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val proof = story.fileOpenedProof.get

    val illegalMove = Line(Square('e', 5), Square('e', 7))
    val untrustedFacts = minimalBoardFacts(
      sideLegal = readyMoves(line = move),
      pieces = Vector(Piece(Side.White, Man.Pawn, Square('e', 5)), Piece(Side.Black, Man.Knight, Square('d', 6)))
    )
    val sameSidePawnFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/4P3/4K3 w - - 0 1").toOption.get
    val rivalPawnFacts = BoardFacts.fromFen("k7/4p3/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val enPassantFacts = BoardFacts.fromFen("k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1").toOption.get
    val promotionFacts = BoardFacts.fromFen("k2n4/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnCaptureFacts = BoardFacts.fromFen("4k3/8/4n3/4R3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnCapture = Line(Square('e', 5), Square('e', 6))

    val illegalProof = FileOpenedProof.fromBoardFacts(facts, illegalMove)
    val untrustedProof = FileOpenedProof.fromBoardFacts(untrustedFacts, move)
    val sameSidePawnProof = FileOpenedProof.fromBoardFacts(sameSidePawnFacts, move)
    val rivalPawnProof = FileOpenedProof.fromBoardFacts(rivalPawnFacts, move)
    val enPassantProof = FileOpenedProof.fromBoardFacts(enPassantFacts, move)
    val promotionProof = FileOpenedProof.fromBoardFacts(promotionFacts, Line(Square('e', 7), Square('d', 8)))
    val nonPawnCaptureProof = FileOpenedProof.fromBoardFacts(nonPawnCaptureFacts, nonPawnCapture)

    assertEquals(illegalProof.complete, false)
    assertEquals(illegalProof.legalPawnMove, false)
    assertEquals(SceneFileOpened.write(facts, illegalMove), None)

    assertEquals(untrustedProof.complete, false)
    assertEquals(untrustedProof.sameBoardProof, false)
    assertEquals(untrustedProof.exactAfterBoardReplay, false)
    assertEquals(SceneFileOpened.write(untrustedFacts, move), None)

    assertEquals(sameSidePawnProof.complete, false)
    assertEquals(sameSidePawnProof.afterBoardHasNoWhitePawnOnOriginFile, false)
    assertEquals(SceneFileOpened.write(sameSidePawnFacts, move), None)

    assertEquals(rivalPawnProof.complete, false)
    assertEquals(rivalPawnProof.afterBoardHasNoWhitePawnOnOriginFile, true)
    assertEquals(rivalPawnProof.afterBoardHasNoBlackPawnOnOriginFile, false)
    assertEquals(rivalPawnProof.originFileOpenAfter, false)
    assertEquals(SceneFileOpened.write(rivalPawnFacts, move), None)

    assertEquals(enPassantProof.complete, false)
    assertEquals(enPassantProof.enPassantMove, true)
    assertEquals(SceneFileOpened.write(enPassantFacts, move), None)

    assertEquals(promotionProof.complete, false)
    assertEquals(promotionProof.nonPromotionMove, false)
    assertEquals(SceneFileOpened.write(promotionFacts, Line(Square('e', 7), Square('d', 8))), None)

    assertEquals(nonPawnCaptureProof.complete, false)
    assertEquals(nonPawnCaptureProof.legalPawnMove, false)
    assertEquals(SceneFileOpened.write(nonPawnCaptureFacts, nonPawnCapture), None)

    Vector(
      "after-board replay missing" -> story.copy(fileOpenedProof = Some(proof.copy(exactAfterBoardReplay = false))),
      "destination file mistaken for opened origin file" -> story.copy(
        openedFile = Some(3),
        fileOpenedProof = Some(proof.copy(openedFile = Some(3), openedFileIsOriginFile = false))
      ),
      "material claim contamination" -> story.copy(captureResult = Some(CaptureResult.fromBoardFacts(facts, move))),
      "passed-pawn-created claim contamination" -> story.copy(
        passedPawnCreatedProof = Some(PassedPawnCreatedProof.fromBoardFacts(facts, move))
      )
    ).foreach: (label, forged) =>
      assertEquals(StoryTable.choose(Vector(forged)).head.role, Role.Blocked, label)

  test("FileOpened-3 downstream wording rejects pressure rook weakness breakthrough half-open and strategy claims"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val forbiddenKeys = plan.forbiddenWording.map(_.key).toSet

    Vector(
      "rook_activity",
      "controls_file",
      "creates_pressure",
      "takes_initiative",
      "weakens_structure",
      "breaks_through",
      "wins_material",
      "creates_passed_pawn",
      "strategic_key"
    ).foreach: forbiddenKey =>
      assert(forbiddenKeys.contains(forbiddenKey), s"FileOpened-3 must forbid $forbiddenKey")

    Vector(
      "exd6 leaves the e-file half-open.",
      "exd6 creates a weakness on the e-file.",
      "exd6 gives the rook activity on the e-file.",
      "exd6 controls the file.",
      "exd6 creates pressure on the e-file.",
      "exd6 takes the initiative.",
      "exd6 breaks through.",
      "exd6 is a strategic pawn break.",
      "exd6 wins material.",
      "exd6 creates a passed pawn."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("FileOpened-4 reuses EngineCheck without engine-owned open-file claims"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get

    def check(status: EngineCheckStatus, storyInput: Option[Story] = Some(story), before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = storyInput,
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(move))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val engineOnly = check(EngineCheckStatus.Supports, storyInput = None)
    val contaminated = story.copy(captureResult = Some(CaptureResult.fromBoardFacts(facts, move)))
    val contaminatedCheck = check(EngineCheckStatus.Supports, storyInput = Some(contaminated))
    val supports = SceneFileOpened.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = SceneFileOpened.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = SceneFileOpened.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes = SceneFileOpened.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get

    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(contaminatedCheck.status, EngineCheckStatus.Unknown)
    assertEquals(contaminatedCheck.storyBound, false)
    assertEquals(SceneFileOpened.withEngineCheck(contaminated, contaminatedCheck), None)

    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("opens_file"))

    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    val unknownPlan = ExplanationPlan.fromSelected(unknownVerdict).get
    val unknownRendered = DeterministicRenderer.fromPlan(unknownPlan).get
    assertEquals(unknownRendered.text.toLowerCase.contains("engine"), false)
    assertEquals(unknownRendered.text.toLowerCase.contains("eval"), false)

    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

    val rendered = DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(supportsVerdict).get).get
    Vector(
      "exd6 opens the e-file because the engine says so.",
      "exd6 opens the e-file with a +1.2 eval.",
      "exd6 is the best move to control the file.",
      "exd6 is the only move to create pressure.",
      "exd6 creates file control because the engine likes it.",
      "exd6 takes the initiative.",
      "exd6 creates a weakness.",
      "exd6 breaks through.",
      "exd6 wins material.",
      "exd6 is the strategic idea."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(unknownPlan, rendered, output).accepted, false, output)

  test("FileOpened-5 StoryTable keeps FileOpened below existing claim homes"):
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
      val fileOpenedVerdict = forward.find(verdict => rowId(rows, verdict.story) == "Scene.FileOpened").get
      val rendered = assertLeadClaim(expectedLead, existingVerdict, expectedClaim)
      assertEquals(fileOpenedVerdict.role, Role.Support, expectedLead)
      assertNoStandaloneText(s"$expectedLead keeps FileOpened support silent", fileOpenedVerdict)
      assertEquals(rendered.claimKey == ExplanationClaim.OpensFile.key, false, expectedLead)

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 40,
        after: Int = 40
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val fileOpened = strong(SceneFileOpened.write(openedFacts, openedMove).get, 99)
    val pawnCapture = strong(ScenePawnCapture.write(openedFacts, openedMove).get, 90)
    val passedPawnCreated = strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get, 90)
    val material = strongMaterial(SceneMaterial.write(openedFacts, openedMove).get, 90)

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

    val targetRows = Vector(
      ("Scene.PawnBreak", pawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      ("Scene.PawnCapture", pawnCapture, ExplanationClaim.CapturesPawn),
      ("Scene.PassedPawnCreated", passedPawnCreated, ExplanationClaim.CreatesPassedPawn),
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
    )

    targetRows.foreach: (label, existing, expectedClaim) =>
      assertExistingHomeKeepsLead(
        Vector(label -> existing, "Scene.FileOpened" -> fileOpened),
        label,
        expectedClaim
      )

    val allRows = targetRows.map((label, story, _) => label -> story) :+ ("Scene.FileOpened" -> fileOpened)
    val allForward = StoryTable.choose(allRows.map(_._2))
    val allReverse = StoryTable.choose(allRows.reverse.map(_._2))
    val allShuffled = StoryTable.choose(allRows.sortBy(_._1).map(_._2))
    assertEquals(shape(allRows, allReverse), shape(allRows, allForward), "input order stable")
    assertEquals(shape(allRows, allShuffled), shape(allRows, allForward), "input order stable")
    assertEquals(allForward.count(_.role == Role.Lead), 1)
    assertNoStandaloneText(
      "FileOpened support is silent in full collision set",
      allForward.find(verdict => rowId(allRows, verdict.story) == "Scene.FileOpened").get
    )

    Vector(
      "actual material change now" -> (material, ExplanationClaim.MaterialBalanceChanges),
      "pawn contact/challenge" -> (pawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      "pawn-captures-rival-pawn event" -> (pawnCapture, ExplanationClaim.CapturesPawn),
      "newly-created passer" -> (passedPawnCreated, ExplanationClaim.CreatesPassedPawn),
      "promotion-next meaning" -> (promotionThreat, ExplanationClaim.CreatesPromotionThreat),
      "actual promotion meaning" -> (promotion, ExplanationClaim.PromotesPawn)
    ).foreach: (label, expected) =>
      val (home, claim) = expected
      val collision = StoryTable.choose(Vector(home, fileOpened))
      val homeVerdict = collision.find(_.story == home).get
      val fileVerdict = collision.find(_.story == fileOpened).get
      val rendered = assertLeadClaim(label, homeVerdict, claim)
      assertEquals(rendered.claimKey, claim.key, label)
      assertNoStandaloneText(s"$label keeps FileOpened silent", fileVerdict)

    val standalone = StoryTable.choose(Vector(fileOpened)).head
    val standalonePlan = ExplanationPlan.fromSelected(standalone).get
    assertEquals(standalonePlan.allowedClaim, Some(ExplanationClaim.OpensFile))
    assertEquals(standalonePlan.allowedClaim.contains(ExplanationClaim.MaterialBalanceChanges), false)
    assertEquals(standalonePlan.allowedClaim.contains(ExplanationClaim.CreatesPassedPawn), false)
    assertEquals(standalonePlan.allowedClaim.contains(ExplanationClaim.ChallengesPawnDirectly), false)
    assertEquals(standalonePlan.allowedClaim.contains(ExplanationClaim.CapturesPawn), false)

    val blackOpenedFacts = BoardFacts.fromFen("4k3/8/8/8/3p4/4N3/8/4K3 b - - 0 1").toOption.get
    val blackOpenedMove = Line(Square('d', 4), Square('e', 3))
    val blackFileOpened = strong(SceneFileOpened.write(blackOpenedFacts, blackOpenedMove).get, 99)
    val fileRows = Vector("White Scene.FileOpened" -> fileOpened, "Black Scene.FileOpened" -> blackFileOpened)
    val fileForward = StoryTable.choose(fileRows.map(_._2))
    val fileReverse = StoryTable.choose(fileRows.reverse.map(_._2))
    assertEquals(shape(fileRows, fileReverse), shape(fileRows, fileForward), "FileOpened input order stable")
    assertEquals(fileForward.count(_.role == Role.Lead), 1)
    fileForward.foreach: verdict =>
      if verdict.role == Role.Lead then
        assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), Some(ExplanationClaim.OpensFile))
      else assertNoStandaloneText("non-lead FileOpened is silent", verdict)

    val capped = SceneFileOpened
      .withEngineCheck(
        fileOpened,
        engineCheck(openedFacts, fileOpened, openedMove, EngineCheckStatus.Caps)
      )
      .get
    val refuted = SceneFileOpened
      .withEngineCheck(
        fileOpened,
        engineCheck(openedFacts, fileOpened, openedMove, EngineCheckStatus.Supports, before = 240, after = 0)
      )
      .get
    Vector("capped FileOpened" -> capped, "refuted FileOpened" -> refuted).foreach: (label, row) =>
      val verdict = StoryTable.choose(Vector(row)).head
      assertNoStandaloneText(label, verdict)

  test("FPSNC-3 cross-slice collision fixtures keep file and pawn event homes separate"):
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
          ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key)
        )

    def assertNoStandaloneText(label: String, verdict: Verdict): Unit =
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

    def assertStandaloneClaim(story: Story, expected: ExplanationClaim, expectedText: String): Unit =
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(verdict.role, Role.Lead)
      assertEquals(plan.allowedClaim, Some(expected))
      assertEquals(rendered.claimKey, expected.key)
      assertEquals(rendered.text, expectedText)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get, 90)
    assertEquals(pawnBreak.pawnBreakProof.exists(_.complete), true)
    assertEquals(pawnBreak.pawnCaptureProof, None)
    assertEquals(pawnBreak.passedPawnCreatedProof, None)
    assertEquals(pawnBreak.fileOpenedProof, None)
    assertEquals(SceneFileOpened.write(pawnBreakFacts, pawnBreakMove), None)
    assertStandaloneClaim(
      pawnBreak,
      ExplanationClaim.ChallengesPawnDirectly,
      "e5 challenges the pawn on d6."
    )

    val captureOpensContactFacts = BoardFacts.fromFen("4k3/2p5/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureOpensMove = Line(Square('e', 5), Square('d', 6))
    val captureOpens = strong(ScenePawnCapture.write(captureOpensContactFacts, captureOpensMove).get, 90)
    val fileOpenedFromCapture = strong(SceneFileOpened.write(captureOpensContactFacts, captureOpensMove).get, 90)
    val recapturableCapture = CaptureResult.fromBoardFacts(captureOpensContactFacts, captureOpensMove)
    val rejectedPawnBreak = PawnBreakProof.fromBoardFacts(captureOpensContactFacts, captureOpensMove)
    val rejectedPassed = PassedPawnCreatedProof.fromBoardFacts(captureOpensContactFacts, captureOpensMove)

    assertEquals(captureOpens.pawnCaptureProof.exists(_.complete), true)
    assertEquals(captureOpens.fileOpenedProof, None)
    assertEquals(fileOpenedFromCapture.fileOpenedProof.exists(_.complete), true)
    assertEquals(fileOpenedFromCapture.pawnCaptureProof, None)
    assertEquals(recapturableCapture.positiveMaterial, false)
    assertEquals(SceneMaterial.write(captureOpensContactFacts, captureOpensMove), None)
    assertEquals(rejectedPawnBreak.complete, false)
    assertEquals(rejectedPawnBreak.nonCapturingMove, false)
    assertEquals(ScenePawnBreak.write(captureOpensContactFacts, captureOpensMove), None)
    assertEquals(rejectedPassed.complete, false)
    assertEquals(rejectedPassed.passedAfter, false)
    assertEquals(ScenePassedPawnCreated.write(captureOpensContactFacts, captureOpensMove), None)

    assertStandaloneClaim(
      captureOpens,
      ExplanationClaim.CapturesPawn,
      "exd6 captures the pawn on d6."
    )
    assertStandaloneClaim(
      fileOpenedFromCapture,
      ExplanationClaim.OpensFile,
      "exd6 opens the e-file."
    )

    val captureCreatesPasserFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureCreatesMove = Line(Square('e', 5), Square('d', 6))
    val captureCreates = strong(ScenePawnCapture.write(captureCreatesPasserFacts, captureCreatesMove).get, 90)
    val passedCreated = strong(ScenePassedPawnCreated.write(captureCreatesPasserFacts, captureCreatesMove).get, 90)
    val fileOpened = strong(SceneFileOpened.write(captureCreatesPasserFacts, captureCreatesMove).get, 90)
    val material = strongMaterial(SceneMaterial.write(captureCreatesPasserFacts, captureCreatesMove).get, 99)

    assertEquals(captureCreates.pawnCaptureProof.exists(_.complete), true)
    assertEquals(passedCreated.passedPawnCreatedProof.exists(_.complete), true)
    assertEquals(fileOpened.fileOpenedProof.exists(_.complete), true)
    assertEquals(material.captureResult.exists(_.positiveMaterial), true)
    assertEquals(ScenePawnBreak.write(captureCreatesPasserFacts, captureCreatesMove), None)
    assertStandaloneClaim(
      passedCreated,
      ExplanationClaim.CreatesPassedPawn,
      "exd6 creates a passed pawn on d6."
    )

    val collisionRows =
      Vector(
        "Scene.Material" -> material,
        "Scene.PawnCapture" -> captureCreates,
        "Scene.PassedPawnCreated" -> passedCreated,
        "Scene.FileOpened" -> fileOpened
      )
    val forward = StoryTable.choose(collisionRows.map(_._2))
    val reverse = StoryTable.choose(collisionRows.reverse.map(_._2))
    val shuffled = StoryTable.choose(collisionRows.sortBy(_._1).map(_._2))
    assertEquals(shape(collisionRows, reverse), shape(collisionRows, forward), "input order stable")
    assertEquals(shape(collisionRows, shuffled), shape(collisionRows, forward), "input order stable")
    assertEquals(rowId(collisionRows, forward.head.story), "Scene.Material")
    assertEquals(forward.count(_.role == Role.Lead), 1)
    assertEquals(
      ExplanationPlan.fromSelected(forward.head).flatMap(_.allowedClaim),
      Some(ExplanationClaim.MaterialBalanceChanges)
    )
    forward.filterNot(_.story.scene == Scene.Material).foreach: verdict =>
      assertNoStandaloneText(s"${rowId(collisionRows, verdict.story)} stays in support", verdict)

    val directContactPasserCandidate = PassedPawnCreatedProof.fromBoardFacts(captureOpensContactFacts, captureOpensMove)
    val createdPasserBreakCandidate = PawnBreakProof.fromBoardFacts(captureCreatesPasserFacts, captureCreatesMove)
    assertEquals(directContactPasserCandidate.complete, false)
    assertEquals(directContactPasserCandidate.passedAfter, false)
    assertEquals(createdPasserBreakCandidate.complete, false)
    assertEquals(createdPasserBreakCandidate.nonCapturingMove, false)

    val halfOpenFacts = BoardFacts.fromFen("4k3/4p3/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val halfOpenProof = FileOpenedProof.fromBoardFacts(halfOpenFacts, captureOpensMove)
    assertEquals(halfOpenProof.complete, false)
    assertEquals(halfOpenProof.afterBoardHasNoWhitePawnOnOriginFile, true)
    assertEquals(halfOpenProof.afterBoardHasNoBlackPawnOnOriginFile, false)
    assertEquals(SceneFileOpened.write(halfOpenFacts, captureOpensMove), None)

    val occupiedOriginFileFacts =
      BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/4P3/4K3 w - - 0 1").toOption.get
    val occupiedOriginProof = FileOpenedProof.fromBoardFacts(occupiedOriginFileFacts, captureOpensMove)
    assertEquals(occupiedOriginProof.complete, false)
    assertEquals(occupiedOriginProof.afterBoardHasNoWhitePawnOnOriginFile, false)
    assertEquals(occupiedOriginProof.afterBoardHasNoBlackPawnOnOriginFile, true)
    assertEquals(SceneFileOpened.write(occupiedOriginFileFacts, captureOpensMove), None)

    val speechKeys =
      Vector(
        ExplanationClaim.ChallengesPawnDirectly.key,
        ExplanationClaim.CapturesPawn.key,
        ExplanationClaim.CreatesPassedPawn.key,
        ExplanationClaim.OpensFile.key
      )
    assertEquals(speechKeys.distinct.size, 4)

  test("FPSNC-4 StoryTable interaction audit keeps File Pawn Structure rows in existing homes"):
    final case class AuditRow(label: String, story: Story, claim: ExplanationClaim)

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

    def strong(story: Story, value: Int = 90): Story =
      story.copy(proof = eventProof(value))

    def strongMaterial(story: Story, value: Int): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[AuditRow], story: Story): String =
      rows.collectFirst { case row if row.story == story => row.label }.getOrElse(s"unknown:$story")

    def expectedClaim(rows: Vector[AuditRow], story: Story): ExplanationClaim =
      rows.collectFirst { case row if row.story == story => row.claim }.get

    def shape(rows: Vector[AuditRow], verdicts: Vector[Verdict]) =
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

    def assertRowsKeepHomes(label: String, rows: Vector[AuditRow]): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_.story))
      val reverse = StoryTable.choose(rows.reverse.map(_.story))
      val shuffled = StoryTable.choose(rows.sortBy(_.label).map(_.story))
      val forwardShape = shape(rows, forward)
      assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
      assertEquals(shape(rows, shuffled), forwardShape, s"$label sorted input order")
      assertEquals(forward.count(_.role == Role.Lead), 1, label)
      forward.foreach: verdict =>
        ExplanationPlan.fromSelected(verdict) match
          case Some(plan) if verdict.role == Role.Lead && plan.allowedClaim.nonEmpty =>
            val expected = expectedClaim(rows, verdict.story)
            val rendered = DeterministicRenderer.fromPlan(plan).get
            assertEquals(plan.allowedClaim, Some(expected), s"$label ${rowId(rows, verdict.story)}")
            assertEquals(rendered.claimKey, expected.key, s"$label ${rowId(rows, verdict.story)}")
          case Some(plan) =>
            assertEquals(plan.allowedClaim, None, s"$label ${rowId(rows, verdict.story)}")
            assertEquals(DeterministicRenderer.fromPlan(plan), None, s"$label ${rowId(rows, verdict.story)}")
          case None =>
            assertNoStandaloneText(s"$label ${rowId(rows, verdict.story)}", verdict)
      forward

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 40,
        after: Int = 40
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture = strong(ScenePawnCapture.write(openedFacts, openedMove).get)
    val passedPawnCreated = strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get)
    val fileOpened = strong(SceneFileOpened.write(openedFacts, openedMove).get)

    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak = strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get)

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance = strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop = strong(ScenePawnStop.write(stopFacts, stopMove).get)

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat = strong(ScenePromotionThreat.write(threatFacts, threatMove).get, 99)

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion = strong(ScenePromotion.write(promotionFacts, promotionMove).get, 99)

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material = strongMaterial(SceneMaterial.write(materialFacts, materialMove).get, 99)
    val hanging = strongMaterial(TacticHanging.write(materialFacts, materialMove).get, 99)

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense = strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get, 99)

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      strong(
        TacticDiscoveredAttack
          .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
          .get,
        99
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
        99
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      strong(
        TacticRemoveGuard
          .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
          .get,
        99
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
        99
      )

    val fpsncRows = Vector(
      AuditRow("Scene.PawnBreak", pawnBreak, ExplanationClaim.ChallengesPawnDirectly),
      AuditRow("Scene.PawnCapture", pawnCapture, ExplanationClaim.CapturesPawn),
      AuditRow("Scene.PassedPawnCreated", passedPawnCreated, ExplanationClaim.CreatesPassedPawn),
      AuditRow("Scene.FileOpened", fileOpened, ExplanationClaim.OpensFile)
    )
    val existingRows = Vector(
      AuditRow("Scene.PawnAdvance", pawnAdvance, ExplanationClaim.AdvancesPassedPawn),
      AuditRow("Scene.PawnStop", pawnStop, ExplanationClaim.StopsPassedPawnNextAdvance),
      AuditRow("Scene.PromotionThreat", promotionThreat, ExplanationClaim.CreatesPromotionThreat),
      AuditRow("Scene.Promotion", promotion, ExplanationClaim.PromotesPawn),
      AuditRow("Scene.Material", material, ExplanationClaim.MaterialBalanceChanges),
      AuditRow("Tactic.Hanging", hanging, ExplanationClaim.CanWinPiece),
      AuditRow("Scene.Defense", defense, ExplanationClaim.DefendsPiece),
      AuditRow("Tactic.DiscoveredAttack", discovered, ExplanationClaim.RevealsAttackOnPiece),
      AuditRow("Tactic.Pin", pin, ExplanationClaim.PinsPiece),
      AuditRow("Tactic.RemoveGuard", removeGuard, ExplanationClaim.RemovesDefender),
      AuditRow("Tactic.Skewer", skewer, ExplanationClaim.SkewersPieceToPiece)
    )

    fpsncRows.foreach: row =>
      assertRowsKeepHomes(s"${row.label} standalone", Vector(row))

    fpsncRows.combinations(2).foreach: pair =>
      assertRowsKeepHomes(s"${pair.head.label} vs ${pair.last.label}", pair)

    fpsncRows.foreach: fpsnc =>
      existingRows.foreach: existing =>
        assertRowsKeepHomes(s"${fpsnc.label} vs ${existing.label}", Vector(fpsnc, existing))

    val fullRows = fpsncRows ++ existingRows
    val fullVerdicts = assertRowsKeepHomes("FPSNC-4 full collision", fullRows)
    fullVerdicts.filterNot(_.role == Role.Lead).foreach: verdict =>
      assertNoStandaloneText(s"non-lead ${rowId(fullRows, verdict.story)}", verdict)

    Vector(
      material -> ExplanationClaim.MaterialBalanceChanges,
      promotionThreat -> ExplanationClaim.CreatesPromotionThreat,
      promotion -> ExplanationClaim.PromotesPawn
    ).foreach: (home, claim) =>
      fpsncRows.foreach: fpsnc =>
        val homeRow = AuditRow(claim.key, home, claim)
        val verdicts = assertRowsKeepHomes(s"${claim.key} vs ${fpsnc.label}", Vector(homeRow, fpsnc))
        val homeVerdict = verdicts.find(_.story == home).get
        val plan = ExplanationPlan.fromSelected(homeVerdict).get
        assertEquals(homeVerdict.role, Role.Lead, claim.key)
        assertEquals(plan.allowedClaim, Some(claim), claim.key)

    assertEquals(pawnBreak.fileOpenedProof, None)
    assertEquals(pawnBreak.pawnCaptureProof, None)
    assertEquals(pawnBreak.passedPawnCreatedProof, None)
    assertEquals(pawnCapture.fileOpenedProof, None)
    assertEquals(pawnCapture.passedPawnCreatedProof, None)
    assertEquals(pawnCapture.captureResult, None)
    assertEquals(passedPawnCreated.fileOpenedProof, None)
    assertEquals(passedPawnCreated.pawnCaptureProof, None)
    assertEquals(passedPawnCreated.promotionThreatProof, None)
    assertEquals(passedPawnCreated.promotionProof, None)
    assertEquals(fileOpened.pawnBreakProof, None)
    assertEquals(fileOpened.pawnCaptureProof, None)
    assertEquals(fileOpened.passedPawnCreatedProof, None)
    assertEquals(fileOpened.captureResult, None)

    val checkedRows = Vector(
      "PawnBreak" -> (pawnBreakFacts, pawnBreakMove, pawnBreak, ScenePawnBreak.withEngineCheck),
      "PawnCapture" -> (openedFacts, openedMove, pawnCapture, ScenePawnCapture.withEngineCheck),
      "PassedPawnCreated" -> (openedFacts, openedMove, passedPawnCreated, ScenePassedPawnCreated.withEngineCheck),
      "FileOpened" -> (openedFacts, openedMove, fileOpened, SceneFileOpened.withEngineCheck)
    )
    checkedRows.foreach: (label, checked) =>
      val (facts, move, story, attach) = checked
      val capped = attach(story, engineCheck(facts, story, move, EngineCheckStatus.Caps)).get
      val refuted =
        attach(story, engineCheck(facts, story, move, EngineCheckStatus.Supports, before = 240, after = 0)).get
      Vector(s"$label capped" -> capped, s"$label refuted" -> refuted).foreach: (rowLabel, row) =>
        val verdict = StoryTable.choose(Vector(row)).head
        assertNoStandaloneText(rowLabel, verdict)

    fpsncRows.foreach: row =>
      val leadVerdict = StoryTable.choose(Vector(row.story)).head
      Vector(
        "Support" -> leadVerdict.copy(role = Role.Support, leadAllowed = false),
        "Context" -> leadVerdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
        "Blocked" -> leadVerdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)
      ).foreach: (roleLabel, verdict) =>
        assertNoStandaloneText(s"${row.label} $roleLabel", verdict)

  test("FPSNC-5 ExplanationPlan boundary audit keeps only selected Lead FPSNC claim keys"):
    final case class BoundaryRow(
        label: String,
        facts: BoardFacts,
        move: Line,
        story: Story,
        claim: ExplanationClaim,
        attachEngineCheck: (Story, EngineCheck) => Option[Story]
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
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(90))

    def rowId(rows: Vector[BoundaryRow], story: Story): String =
      rows.collectFirst { case row if row.story == story => row.label }.getOrElse(s"unknown:$story")

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 40,
        after: Int = 40
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    def assertNoStandaloneClaim(label: String, verdict: Verdict): Unit =
      val plan = ExplanationPlan.fromSelected(verdict)
      plan.foreach: relationOnly =>
        assertEquals(relationOnly.allowedClaim, None, label)
      assertEquals(plan.flatMap(DeterministicRenderer.fromPlan), None, label)

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))

    val rows = Vector(
      BoundaryRow(
        "Scene.PawnBreak",
        pawnBreakFacts,
        pawnBreakMove,
        strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get),
        ExplanationClaim.ChallengesPawnDirectly,
        ScenePawnBreak.withEngineCheck
      ),
      BoundaryRow(
        "Scene.PawnCapture",
        openedFacts,
        openedMove,
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        ExplanationClaim.CapturesPawn,
        ScenePawnCapture.withEngineCheck
      ),
      BoundaryRow(
        "Scene.PassedPawnCreated",
        openedFacts,
        openedMove,
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        ExplanationClaim.CreatesPassedPawn,
        ScenePassedPawnCreated.withEngineCheck
      ),
      BoundaryRow(
        "Scene.FileOpened",
        openedFacts,
        openedMove,
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        ExplanationClaim.OpensFile,
        SceneFileOpened.withEngineCheck
      )
    )
    val allowedKeys = Vector(
      "challenges_pawn",
      "captures_rival_pawn",
      "creates_passed_pawn",
      "opens_file"
    )
    val forbiddenKeys = Vector(
      "controls_file",
      "uses_open_file",
      "rook_activity",
      "creates_pressure",
      "takes_initiative",
      "weakens_structure",
      "creates_weakness",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "promotion_threat",
      "unstoppable_pawn",
      "converts_advantage",
      "best_move",
      "only_move",
      "forced"
    )
    val allClaimKeys = ExplanationClaim.values.map(_.key).toSet

    assertEquals(rows.map(_.claim.key), allowedKeys)
    assertEquals(allClaimKeys.intersect(forbiddenKeys.toSet), Set.empty[String])

    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val siblingKeys = allowedKeys.filterNot(_ == row.claim.key)

      assertEquals(verdict.selected, true, row.label)
      assertEquals(verdict.role, Role.Lead, row.label)
      assertEquals(verdict.leadAllowed, true, row.label)
      assertEquals(verdict.engineStrengthLimited, false, row.label)
      assertEquals(plan.allowedClaim, Some(row.claim), row.label)
      assertEquals(rendered.claimKey, row.claim.key, row.label)
      assertEquals(siblingKeys.contains(rendered.claimKey), false, row.label)
      assertEquals(forbiddenKeys.contains(rendered.claimKey), false, row.label)

      Vector(
        "Support" -> verdict.copy(role = Role.Support, leadAllowed = false),
        "Context" -> verdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
        "Blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4)
      ).foreach: (roleLabel, nonLead) =>
        assertNoStandaloneClaim(s"${row.label} $roleLabel", nonLead)

      val capped =
        row.attachEngineCheck(row.story, engineCheck(row.facts, row.story, row.move, EngineCheckStatus.Caps)).get
      val refuted =
        row
          .attachEngineCheck(
            row.story,
            engineCheck(row.facts, row.story, row.move, EngineCheckStatus.Supports, before = 240, after = 0)
          )
          .get
      Vector("capped" -> capped, "refuted" -> refuted).foreach: (statusLabel, checkedStory) =>
        val checkedVerdict = StoryTable.choose(Vector(checkedStory)).head
        assertNoStandaloneClaim(s"${row.label} $statusLabel", checkedVerdict)

    val verdicts = StoryTable.choose(rows.map(_.story))
    assertEquals(verdicts.count(_.role == Role.Lead), 1)
    verdicts.foreach: verdict =>
      val label = rowId(rows, verdict.story)
      val plan = ExplanationPlan.fromSelected(verdict)
      if verdict.role == Role.Lead then
        val row = rows.find(_.story == verdict.story).get
        assertEquals(plan.flatMap(_.allowedClaim), Some(row.claim), label)
      else assertNoStandaloneClaim(s"non-selected $label", verdict)

  test("FPSNC-6 renderer boundary audit keeps only bounded FPSNC templates"):
    final case class RendererRow(
        label: String,
        facts: BoardFacts,
        move: Line,
        story: Story,
        expectedClaim: ExplanationClaim,
        expectedText: String,
        attachEngineCheck: (Story, EngineCheck) => Option[Story]
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
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(90))

    def engineCheck(
        facts: BoardFacts,
        story: Story,
        line: Line,
        status: EngineCheckStatus,
        before: Int = 40,
        after: Int = 40
    ): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(line))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val rows = Vector(
      RendererRow(
        "Scene.PawnBreak",
        pawnBreakFacts,
        pawnBreakMove,
        strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get),
        ExplanationClaim.ChallengesPawnDirectly,
        "e5 challenges the pawn on d6.",
        ScenePawnBreak.withEngineCheck
      ),
      RendererRow(
        "Scene.PawnCapture",
        openedFacts,
        openedMove,
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        ExplanationClaim.CapturesPawn,
        "exd6 captures the pawn on d6.",
        ScenePawnCapture.withEngineCheck
      ),
      RendererRow(
        "Scene.PassedPawnCreated",
        openedFacts,
        openedMove,
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        ExplanationClaim.CreatesPassedPawn,
        "exd6 creates a passed pawn on d6.",
        ScenePassedPawnCreated.withEngineCheck
      ),
      RendererRow(
        "Scene.FileOpened",
        openedFacts,
        openedMove,
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        ExplanationClaim.OpensFile,
        "exd6 opens the e-file.",
        SceneFileOpened.withEngineCheck
      )
    )
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .mkString(" ")
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val renderedSurfaceNames =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet
    val forbiddenOutputs = Vector(
      "exd6 controls the file.",
      "exd6 uses the open file.",
      "exd6 activates the rook.",
      "exd6 creates pressure.",
      "exd6 takes the initiative.",
      "exd6 weakens the structure.",
      "exd6 creates a weakness.",
      "exd6 breaks through.",
      "exd6 wins space.",
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 will promote.",
      "exd6 is unstoppable.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 forces a win."
    )

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    Vector(
      "Verdict",
      "Story",
      "Proof",
      "StoryProof",
      "PawnBreakProof",
      "PawnCaptureProof",
      "PassedPawnCreatedProof",
      "FileOpenedProof",
      "BoardFacts",
      "BoardMood",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "MissingEvidence",
      "SourceRow"
    ).foreach: forbiddenType =>
      assert(
        !rendererParameterNames.contains(forbiddenType),
        s"DeterministicRenderer must not accept $forbiddenType"
      )
    Vector(
      "fromVerdict",
      "fromStory",
      "fromProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromProofFailures",
      "fromSourceRows"
    ).foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"renderer exposed $forbiddenMethod")
    Vector(
      "story",
      "proof",
      "boardFacts",
      "engineCheck",
      "proofFailures",
      "sourceRows"
    ).foreach: forbiddenField =>
      assert(
        !renderedSurfaceNames.exists(_.toLowerCase.contains(forbiddenField.toLowerCase)),
        s"RenderedLine exposed $forbiddenField"
      )

    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get

      assertEquals(rendered.text, row.expectedText, row.label)
      assertEquals(rendered.claimKey, row.expectedClaim.key, row.label)
      assertEquals(rendered.strength, "bounded", row.label)
      assertEquals(rendered.forbiddenCheckPassed, true, row.label)

      forbiddenOutputs.foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)
        assertEquals(DeterministicRenderer.fromPlan(plan.copy(forbiddenWording = plan.forbiddenWording)), Some(rendered))

      Vector(
        plan.copy(role = Role.Support),
        plan.copy(role = Role.Context),
        plan.copy(role = Role.Blocked, debugOnly = true),
        plan.copy(allowedClaim = None),
        plan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges)),
        plan.copy(route = None),
        plan.copy(routeSan = None),
        plan.copy(evidenceLine = None),
        plan.copy(evidenceLine = Some(Line(Square('a', 1), Square('a', 2)))),
        plan.copy(target = None),
        plan.copy(anchor = None),
        plan.copy(secondaryTarget = Some(Square('h', 8))),
        plan.copy(forbiddenWording = Vector.empty)
      ).foreach: malformedPlan =>
        assertEquals(DeterministicRenderer.fromPlan(malformedPlan), None, malformedPlan.toString)

      val capped =
        row.attachEngineCheck(row.story, engineCheck(row.facts, row.story, row.move, EngineCheckStatus.Caps)).get
      val refuted =
        row
          .attachEngineCheck(
            row.story,
            engineCheck(row.facts, row.story, row.move, EngineCheckStatus.Supports, before = 240, after = 0)
          )
          .get
      Vector(
        "support" -> verdict.copy(role = Role.Support, leadAllowed = false),
        "context" -> verdict.copy(role = Role.Context, leadAllowed = false, rank = 3),
        "blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false, rank = 4),
        "capped" -> StoryTable.choose(Vector(capped)).head,
        "refuted" -> StoryTable.choose(Vector(refuted)).head
      ).foreach: (label, blockedVerdict) =>
        assertEquals(
          ExplanationPlan.fromSelected(blockedVerdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          s"${row.label} $label"
        )

  test("FPSNC-7 LLM smoke boundary audit reuses only existing 8B contract"):
    final case class SmokeRow(label: String, story: Story, expectedClaim: ExplanationClaim, expectedText: String)

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

    def strong(story: Story): Story =
      story.copy(proof = strongProof(90))

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val rows = Vector(
      SmokeRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get),
        ExplanationClaim.ChallengesPawnDirectly,
        "e5 challenges the pawn on d6."
      ),
      SmokeRow(
        "Scene.PawnCapture",
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        ExplanationClaim.CapturesPawn,
        "exd6 captures the pawn on d6."
      ),
      SmokeRow(
        "Scene.PassedPawnCreated",
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        ExplanationClaim.CreatesPassedPawn,
        "exd6 creates a passed pawn on d6."
      ),
      SmokeRow(
        "Scene.FileOpened",
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        ExplanationClaim.OpensFile,
        "exd6 opens the e-file."
      )
    )
    val llmParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .mkString(" ")
    val llmMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val forbiddenInputs = Vector(
      "Story(",
      "raw Story",
      "PawnBreakProof",
      "PawnCaptureProof",
      "PassedPawnCreatedProof",
      "FileOpenedProof",
      "PassedPawnObservation",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "principal variation",
      "proofFailures",
      "sourceRow",
      "source rows"
    )
    val forbiddenOutputByName = Vector(
      "new move" -> "Nf3 also improves the position.",
      "new line" -> "The move works and Re1 follows.",
      "file control" -> "The move controls the file.",
      "rook activity" -> "The move activates the rook.",
      "pressure" -> "The move creates pressure.",
      "initiative" -> "The move takes the initiative.",
      "weakness" -> "The move creates a weakness.",
      "breakthrough" -> "The move breaks through.",
      "material" -> "The move wins material.",
      "promotion" -> "The pawn will promote.",
      "unstoppable" -> "The pawn is unstoppable.",
      "conversion" -> "The move converts the advantage.",
      "best move" -> "This is the best move.",
      "only move" -> "This is the only move.",
      "forced" -> "The move is forced.",
      "strategy claim" -> "This is a strategic file plan.",
      "why it matters" -> "The move matters because it improves the position."
    )

    Vector(
      "Story",
      "PawnBreakProof",
      "PawnCaptureProof",
      "PassedPawnCreatedProof",
      "FileOpenedProof",
      "PassedPawnObservation",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "SourceRow"
    ).foreach: forbiddenType =>
      assert(!llmParameterNames.contains(forbiddenType), s"LLM smoke must not accept $forbiddenType")
    Vector(
      "fromStory",
      "fromPawnBreakProof",
      "fromPawnCaptureProof",
      "fromPassedPawnCreatedProof",
      "fromFileOpenedProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromSourceRows"
    ).foreach: forbiddenMethod =>
      assert(!llmMethodNames.contains(forbiddenMethod), s"LLM smoke exposed $forbiddenMethod")

    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

      Vector(
        s"renderedText: ${row.expectedText}",
        s"claimKey: ${row.expectedClaim.key}",
        "strength: bounded",
        "forbiddenWording:",
        "instruction: Rephrase only. Do not add chess facts."
      ).foreach: required =>
        assert(prompt.contains(required), s"${row.label} prompt missing $required")
      forbiddenInputs.foreach: forbiddenInput =>
        assertEquals(prompt.contains(forbiddenInput), false, s"${row.label} prompt leaked $forbiddenInput")

      assertEquals(rendered.text, row.expectedText, row.label)
      assertEquals(rendered.claimKey, row.expectedClaim.key, row.label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(row.expectedText), row.label)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, row.expectedText).accepted, true, row.label)

      forbiddenOutputByName.foreach: (name, output) =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"${row.label}: $name")

      if row.expectedClaim == ExplanationClaim.CreatesPassedPawn then
        assertEquals(
          LlmNarrationSmoke.check(plan, rendered, "exd6 creates a passed pawn on d6.").accepted,
          true,
          row.label
        )
      else
        assertEquals(
          LlmNarrationSmoke.check(plan, rendered, "exd6 creates a passed pawn on d6.").accepted,
          false,
          row.label
        )
      if row.expectedClaim == ExplanationClaim.OpensFile then
        assertEquals(
          LlmNarrationSmoke.check(plan, rendered, "exd6 opens the e-file.").accepted,
          true,
          row.label
        )
      else
        assertEquals(
          LlmNarrationSmoke.check(plan, rendered, "exd6 opens the e-file.").accepted,
          false,
          row.label
        )

      Vector(
        "raw Story says this is good.",
        "PawnBreakProof proves it.",
        "PawnCaptureProof proves it.",
        "PassedPawnCreatedProof proves it.",
        "FileOpenedProof proves it.",
        "PassedPawnObservation found it.",
        "BoardFacts show it.",
        "EngineCheck supports it.",
        "The raw PV confirms it.",
        "proofFailures are empty.",
        "source rows explain it."
      ).foreach: output =>
        assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, s"${row.label}: $output")

      val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
      val mismatchedRendered = rendered.copy(claimKey = "wins_material")
      assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None, row.label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(supportPlan, rendered), None, row.label)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, mismatchedRendered), None, row.label)
      assertEquals(LlmNarrationSmoke.codexCliPrompt(plan, mismatchedRendered), None, row.label)

  test("FPSNC-8 forbidden wording audit keeps closed wording non-authoritative"):
    final case class ForbiddenAuditRow(
        label: String,
        story: Story,
        allowedClaim: ExplanationClaim,
        expectedText: String,
        allowedExceptions: Set[String]
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
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def strong(story: Story): Story =
      story.copy(proof = strongProof(90))

    def normalize(text: String): String =
      text
        .toLowerCase(java.util.Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .replaceAll("\\s+", " ")
        .trim

    def containsPhrase(text: String, phrase: String): Boolean =
      s" ${normalize(text)} ".contains(s" ${normalize(phrase)} ")

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val pawnAdvanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val pawnAdvanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnStopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val pawnStopMove = Line(Square('g', 7), Square('e', 6))
    val promotionThreatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionThreatMove = Line(Square('e', 6), Square('e', 7))

    val rows = Vector(
      ForbiddenAuditRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get),
        ExplanationClaim.ChallengesPawnDirectly,
        "e5 challenges the pawn on d6.",
        Set.empty
      ),
      ForbiddenAuditRow(
        "Scene.PawnCapture",
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        ExplanationClaim.CapturesPawn,
        "exd6 captures the pawn on d6.",
        Set.empty
      ),
      ForbiddenAuditRow(
        "Scene.PassedPawnCreated",
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        ExplanationClaim.CreatesPassedPawn,
        "exd6 creates a passed pawn on d6.",
        Set("passed pawn")
      ),
      ForbiddenAuditRow(
        "Scene.FileOpened",
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        ExplanationClaim.OpensFile,
        "exd6 opens the e-file.",
        Set("open file")
      ),
      ForbiddenAuditRow(
        "Scene.PawnAdvance",
        strong(ScenePawnAdvance.write(pawnAdvanceFacts, pawnAdvanceMove).get),
        ExplanationClaim.AdvancesPassedPawn,
        "e6 advances the passed pawn.",
        Set("passed pawn")
      ),
      ForbiddenAuditRow(
        "Scene.PawnStop",
        strong(ScenePawnStop.write(pawnStopFacts, pawnStopMove).get),
        ExplanationClaim.StopsPassedPawnNextAdvance,
        "Ne6 stops the passed pawn from advancing next.",
        Set("passed pawn")
      ),
      ForbiddenAuditRow(
        "Scene.PromotionThreat",
        strong(ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).get),
        ExplanationClaim.CreatesPromotionThreat,
        "e7 threatens to promote next.",
        Set("promotion threat")
      )
    )

    val forbiddenSentenceByName = Vector(
      "controls file" -> "The move controls file.",
      "file control" -> "The move claims file control.",
      "rook activity" -> "The move creates rook activity.",
      "rook lift" -> "The move starts a rook lift.",
      "pressure" -> "The move creates pressure.",
      "initiative" -> "The move takes the initiative.",
      "weak square" -> "The move creates a weak square.",
      "weak pawn" -> "The move leaves a weak pawn.",
      "weakens structure" -> "The move weakens structure.",
      "breakthrough" -> "The move is a breakthrough.",
      "breaks through" -> "The move breaks through.",
      "wins space" -> "The move wins space.",
      "wins pawn" -> "The move wins pawn.",
      "wins material" -> "The move wins material.",
      "promotion threat" -> "The move creates a promotion threat.",
      "passed pawn" -> "The move creates a passed pawn.",
      "open file" -> "The move creates an open file.",
      "unstoppable" -> "The pawn is unstoppable.",
      "conversion" -> "The move starts conversion.",
      "best move" -> "This is the best move.",
      "only move" -> "This is the only move.",
      "forced" -> "The move is forced."
    )
    val forbiddenClaimKeys = Vector(
      "controls_file",
      "file_control",
      "rook_activity",
      "rook_lift",
      "creates_pressure",
      "takes_initiative",
      "weak_square",
      "weak_pawn",
      "weakens_structure",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "promotion_threat",
      "unstoppable_pawn",
      "converts_advantage",
      "best_move",
      "only_move",
      "forced"
    )
    val allowedClaimKeys = Set(
      "challenges_pawn",
      "captures_rival_pawn",
      "creates_passed_pawn",
      "opens_file",
      "advances_passed_pawn",
      "stops_pawn_advance",
      "threatens_promotion_next"
    )
    val forbiddenAuthorityNames = Vector(
      "FileControl",
      "RookActivity",
      "RookLift",
      "WeakSquare",
      "WeakPawn",
      "WeakensStructure",
      "Breakthrough",
      "WinsSpace",
      "WinsPawn",
      "WinsMaterial",
      "Unstoppable",
      "Conversion",
      "BestMove",
      "OnlyMove",
      "Forced"
    )
    val runtimeAuthorityNames =
      (Scene.values.map(_.toString) ++ StoryWriter.values.map(_.toString) ++ ExplanationClaim.values.map(_.key)).toVector

    forbiddenAuthorityNames.foreach: forbiddenName =>
      assert(
        !runtimeAuthorityNames.exists(_.contains(forbiddenName)),
        s"forbidden wording became runtime authority name: $forbiddenName"
      )
    forbiddenClaimKeys.foreach: forbiddenKey =>
      assertEquals(
        ExplanationClaim.values.map(_.key).contains(forbiddenKey),
        false,
        s"forbidden wording became claim key: $forbiddenKey"
      )

    rows.foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      val smoke = LlmNarrationSmoke.mockNarrate(plan, rendered)
      val publicValueText = verdict.values.mkString(" ")
      val claimKey = plan.allowedClaim.map(_.key).getOrElse("")

      assertEquals(verdict.role, Role.Lead, row.label)
      assertEquals(plan.allowedClaim, Some(row.allowedClaim), row.label)
      assertEquals(rendered.text, row.expectedText, row.label)
      assertEquals(rendered.claimKey, row.allowedClaim.key, row.label)
      assertEquals(rendered.forbiddenCheckPassed, true, row.label)
      assertEquals(smoke, Some(row.expectedText), row.label)
      assert(allowedClaimKeys.contains(claimKey), s"${row.label} unexpected claim key $claimKey")
      forbiddenClaimKeys.foreach: forbiddenKey =>
        assertEquals(claimKey == forbiddenKey, false, s"${row.label} used forbidden claim key $forbiddenKey")

      forbiddenSentenceByName.foreach: (name, sentence) =>
        if row.allowedExceptions.contains(name) then
          assertEquals(
            LlmNarrationSmoke.check(plan, rendered, row.expectedText).accepted,
            true,
            s"${row.label} selected bounded exception must remain allowed: $name"
          )
        else
          assertEquals(
            LlmNarrationSmoke.check(plan, rendered, s"${row.expectedText} $sentence").accepted,
            false,
            s"${row.label} accepted forbidden LLM-added fact: $name"
          )

      forbiddenSentenceByName.map(_._1).filterNot(row.allowedExceptions.contains).foreach: phrase =>
        assertEquals(
          containsPhrase(rendered.text, phrase),
          false,
          s"${row.label} renderer output leaked forbidden phrase: $phrase"
        )
        smoke.foreach: text =>
          assertEquals(
            containsPhrase(text, phrase),
            false,
            s"${row.label} LLM smoke output leaked forbidden phrase: $phrase"
          )
        assertEquals(
          containsPhrase(publicValueText, phrase),
          false,
          s"${row.label} public values leaked forbidden phrase: $phrase"
        )

    val fileOpenedPlan =
      ExplanationPlan.fromSelected(StoryTable.choose(Vector(strong(SceneFileOpened.write(openedFacts, openedMove).get))).head).get
    val fileOpenedRendered = DeterministicRenderer.fromPlan(fileOpenedPlan).get
    assertEquals(LlmNarrationSmoke.check(fileOpenedPlan, fileOpenedRendered, "exd6 opens the e-file.").accepted, true)
    assertEquals(
      LlmNarrationSmoke.check(fileOpenedPlan, fileOpenedRendered, "exd6 controls the open file.").accepted,
      false
    )

  test("FPSNC-10 Runtime Boundary Audit keeps helpers out of runtime authority"):
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
      "FPSNC-",
      "closeout",
      "Runtime Boundary Audit",
      "File / Pawn Structure Neighborhood Closeout",
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

    Vector("FPSNC", "Closeout", "RuntimeBoundary", "Fixture", "Corpus", "Audit", "Neighborhood")
      .foreach: helperOnlyName =>
        assert(
          !runtimeAuthorityNames.exists(_.contains(helperOnlyName)),
          s"test helper name became runtime authority: $helperOnlyName"
        )

    Vector(
      "PawnTactic",
      "PawnStructure",
      "FileControl",
      "OpenFileStrategy",
      "RookActivity",
      "RookLift",
      "Breakthrough",
      "WeakSquare",
      "WeakPawn",
      "WeakensStructure",
      "UnstoppablePawn",
      "BestMove",
      "OnlyMove",
      "ForcedMove"
    ).foreach: forbiddenAuthorityName =>
      assert(
        !runtimeAuthorityNames.exists(_.contains(forbiddenAuthorityName)),
        s"closed file/pawn authority name opened at runtime: $forbiddenAuthorityName"
      )

    Vector(
      "controls_file",
      "uses_open_file",
      "rook_activity",
      "rook_lift",
      "creates_pressure",
      "takes_initiative",
      "weak_square",
      "weak_pawn",
      "weakens_structure",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "promotion_threat",
      "unstoppable_pawn",
      "converts_advantage",
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
      StoryWriter.values.map(_.toString).toVector.filter(
        Set("ScenePawnBreak", "ScenePawnCapture", "ScenePassedPawnCreated", "SceneFileOpened")
      ),
      Vector("ScenePawnBreak", "ScenePawnCapture", "ScenePassedPawnCreated", "SceneFileOpened")
    )
    assertEquals(
      Scene.values.toVector.filter(Set(Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, Scene.FileOpened)),
      Vector(Scene.PawnBreak, Scene.PawnCapture, Scene.PassedPawnCreated, Scene.FileOpened)
    )
    assertEquals(ExplanationClaim.PawnBreakAllowed.map(_.key), Vector("challenges_pawn"))
    assertEquals(ExplanationClaim.PawnCaptureAllowed.map(_.key), Vector("captures_rival_pawn"))
    assertEquals(ExplanationClaim.PassedPawnCreatedAllowed.map(_.key), Vector("creates_passed_pawn"))
    assertEquals(ExplanationClaim.FileOpenedAllowed.map(_.key), Vector("opens_file"))

    Vector(
      "FPSNCProof",
      "CloseoutProof",
      "PawnTacticProof",
      "PawnStructureProof",
      "FileControlProof",
      "OpenFileStrategyProof",
      "RookActivityProof",
      "RookLiftProof",
      "BreakthroughProof",
      "WeakSquareProof",
      "WeakPawnProof"
    ).foreach: forbiddenProofHome =>
      assert(
        !runtimeText.contains(forbiddenProofHome),
        s"closed proof home opened at runtime: $forbiddenProofHome"
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnBreakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val pawnBreakMove = Line(Square('e', 4), Square('e', 5))
    val promotionThreatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionThreatMove = Line(Square('e', 6), Square('e', 7))
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val selectedRows =
      Vector(
        ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get -> "e5 challenges the pawn on d6.",
        ScenePawnCapture.write(openedFacts, openedMove).get -> "exd6 captures the pawn on d6.",
        ScenePassedPawnCreated.write(openedFacts, openedMove).get -> "exd6 creates a passed pawn on d6.",
        SceneFileOpened.write(openedFacts, openedMove).get -> "exd6 opens the e-file."
      )

    selectedRows.foreach: (story, expectedText) =>
      val verdict = StoryTable.choose(Vector(story)).head
      val plan = ExplanationPlan.fromSelected(verdict).get
      val rendered = DeterministicRenderer.fromPlan(plan).get
      assertEquals(rendered.text, expectedText)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(expectedText))

    val siblingPawnCaptureProof = PawnCaptureProof.fromBoardFacts(openedFacts, openedMove)
    val siblingPassedPawnCreatedProof = PassedPawnCreatedProof.fromBoardFacts(openedFacts, openedMove)
    val siblingFileOpenedProof = FileOpenedProof.fromBoardFacts(openedFacts, openedMove)

    def check(facts: BoardFacts, story: Story, move: Line): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('e', 8), Square('e', 7))))),
        evalBefore = Some(EngineEval(40)),
        evalAfter = Some(EngineEval(40)),
        depth = Some(16),
        freshnessPly = Some(0),
        requestedStatus = EngineCheckStatus.Supports
      )

    final case class ContaminatedRow(
        label: String,
        facts: BoardFacts,
        move: Line,
        story: Story,
        attach: (Story, EngineCheck) => Option[Story]
    )

    Vector(
      ContaminatedRow(
        "PawnBreak with PawnCaptureProof",
        pawnBreakFacts,
        pawnBreakMove,
        ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get.copy(pawnCaptureProof = Some(siblingPawnCaptureProof)),
        ScenePawnBreak.withEngineCheck
      ),
      ContaminatedRow(
        "PawnBreak with PassedPawnCreatedProof",
        pawnBreakFacts,
        pawnBreakMove,
        ScenePawnBreak.write(pawnBreakFacts, pawnBreakMove).get.copy(passedPawnCreatedProof = Some(siblingPassedPawnCreatedProof)),
        ScenePawnBreak.withEngineCheck
      ),
      ContaminatedRow(
        "PawnCapture with PassedPawnCreatedProof",
        openedFacts,
        openedMove,
        ScenePawnCapture.write(openedFacts, openedMove).get.copy(passedPawnCreatedProof = Some(siblingPassedPawnCreatedProof)),
        ScenePawnCapture.withEngineCheck
      ),
      ContaminatedRow(
        "PawnCapture with FileOpenedProof",
        openedFacts,
        openedMove,
        ScenePawnCapture.write(openedFacts, openedMove).get.copy(fileOpenedProof = Some(siblingFileOpenedProof)),
        ScenePawnCapture.withEngineCheck
      ),
      ContaminatedRow(
        "PassedPawnCreated with FileOpenedProof",
        openedFacts,
        openedMove,
        ScenePassedPawnCreated.write(openedFacts, openedMove).get.copy(fileOpenedProof = Some(siblingFileOpenedProof)),
        ScenePassedPawnCreated.withEngineCheck
      ),
      ContaminatedRow(
        "PromotionThreat with PawnCaptureProof",
        promotionThreatFacts,
        promotionThreatMove,
        ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).get
          .copy(pawnCaptureProof = Some(siblingPawnCaptureProof)),
        ScenePromotionThreat.withEngineCheck
      ),
      ContaminatedRow(
        "PromotionThreat with PassedPawnCreatedProof",
        promotionThreatFacts,
        promotionThreatMove,
        ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).get
          .copy(passedPawnCreatedProof = Some(siblingPassedPawnCreatedProof)),
        ScenePromotionThreat.withEngineCheck
      ),
      ContaminatedRow(
        "PromotionThreat with FileOpenedProof",
        promotionThreatFacts,
        promotionThreatMove,
        ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).get.copy(fileOpenedProof = Some(siblingFileOpenedProof)),
        ScenePromotionThreat.withEngineCheck
      ),
      ContaminatedRow(
        "Promotion with PawnCaptureProof",
        promotionFacts,
        promotionMove,
        ScenePromotion.write(promotionFacts, promotionMove).get.copy(pawnCaptureProof = Some(siblingPawnCaptureProof)),
        ScenePromotion.withEngineCheck
      ),
      ContaminatedRow(
        "Promotion with PassedPawnCreatedProof",
        promotionFacts,
        promotionMove,
        ScenePromotion.write(promotionFacts, promotionMove).get
          .copy(passedPawnCreatedProof = Some(siblingPassedPawnCreatedProof)),
        ScenePromotion.withEngineCheck
      ),
      ContaminatedRow(
        "Promotion with FileOpenedProof",
        promotionFacts,
        promotionMove,
        ScenePromotion.write(promotionFacts, promotionMove).get.copy(fileOpenedProof = Some(siblingFileOpenedProof)),
        ScenePromotion.withEngineCheck
      )
    ).foreach: row =>
      val verdict = StoryTable.choose(Vector(row.story)).head
      assertEquals(verdict.role, Role.Blocked, row.label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, row.label)
      assertEquals(row.attach(row.story, check(row.facts, row.story, row.move)), None, row.label)

    Vector("public route", "production API", "user-facing LLM narration").foreach: publicSurfaceTerm =>
      assert(
        !runtimeText.contains(publicSurfaceTerm),
        s"closed public surface term reached runtime source: $publicSurfaceTerm"
      )

  test("FileOpened-6 ExplanationPlan admits only selected uncapped Lead opens-file claim"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val leadVerdict = StoryTable.choose(Vector(story)).head
    val leadPlan = ExplanationPlan.fromSelected(leadVerdict).get
    val rendered = DeterministicRenderer.fromPlan(leadPlan).get
    val forbiddenKeys = leadPlan.forbiddenWording.map(_.key).toSet

    assertEquals(leadVerdict.selected, true)
    assertEquals(leadVerdict.role, Role.Lead)
    assertEquals(leadVerdict.leadAllowed, true)
    assertEquals(leadPlan.scene, Scene.FileOpened)
    assertEquals(leadPlan.tactic, None)
    assertEquals(leadPlan.allowedClaim, Some(ExplanationClaim.OpensFile))
    assertEquals(ExplanationClaim.FileOpenedAllowed.map(_.key), Vector("opens_file"))
    assertEquals(rendered.claimKey, "opens_file")
    assertEquals(rendered.strength, "bounded")

    Vector(
      "controls_file",
      "uses_open_file",
      "rook_activity",
      "creates_pressure",
      "takes_initiative",
      "weakens_structure",
      "creates_weakness",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "creates_passed_pawn",
      "promotion_threat",
      "best_move",
      "only_move",
      "forced"
    ).foreach: forbiddenKey =>
      assert(forbiddenKeys.contains(forbiddenKey), s"FileOpened-6 must forbid $forbiddenKey")
      assert(
        ExplanationClaim.FileOpenedForbiddenKeys.contains(forbiddenKey),
        s"FileOpened-6 public forbidden key list missing $forbiddenKey"
      )
      assert(
        !ExplanationClaim.FileOpenedAllowed.map(_.key).contains(forbiddenKey),
        s"FileOpened-6 allowed list must not contain $forbiddenKey"
      )

    Vector(
      "exd6 controls the e-file.",
      "exd6 uses the open file.",
      "exd6 gives the rook activity.",
      "exd6 creates pressure.",
      "exd6 takes the initiative.",
      "exd6 weakens the structure.",
      "exd6 creates a weakness.",
      "exd6 breaks through.",
      "exd6 wins space.",
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 creates a promotion threat.",
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

    def check(status: EngineCheckStatus, before: Int = 40, after: Int = 40): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(move))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(12),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped =
      StoryTable.choose(Vector(SceneFileOpened.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable
        .choose(
          Vector(
            SceneFileOpened
              .withEngineCheck(story, check(EngineCheckStatus.Supports, before = 240, after = 0))
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

  test("FileOpened-7 DeterministicRenderer phrases only bounded open-file text"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val plan = ExplanationPlan.fromSelected(StoryTable.choose(Vector(story)).head).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val fromPlanMethods =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector.filter(_.getName == "fromPlan")
    val fromPlanParameterShapes =
      fromPlanMethods.map(method => method.getParameterTypes.toVector.map(_.getSimpleName).toVector).toVector
    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .mkString(" ")
    val rendererMethodNames = DeterministicRenderer.getClass.getDeclaredMethods.map(_.getName).toSet
    val renderedSurfaceNames =
      classOf[RenderedLine].getDeclaredMethods.map(_.getName).toSet ++
        classOf[RenderedLine].getDeclaredFields.map(_.getName).toSet

    assertEquals(fromPlanParameterShapes, Vector(Vector("ExplanationPlan")))
    assertEquals(rendered.text, "exd6 opens the e-file.")
    assertEquals(rendered.claimKey, "opens_file")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "Verdict",
      "Story",
      "FileOpenedProof",
      "BoardFacts",
      "BoardMood",
      "EngineCheck",
      "EngineEval",
      "EngineLine",
      "MissingEvidence",
      "SourceRow"
    ).foreach: forbiddenType =>
      assert(
        !rendererParameterNames.contains(forbiddenType),
        s"DeterministicRenderer must not accept $forbiddenType"
      )
    Vector(
      "fromVerdict",
      "fromStory",
      "fromFileOpenedProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromSourceRows"
    ).foreach: forbiddenMethod =>
      assert(!rendererMethodNames.contains(forbiddenMethod), s"renderer exposed $forbiddenMethod")
    Vector(
      "story",
      "fileOpenedProof",
      "boardFacts",
      "engineCheck",
      "proofFailures",
      "sourceRows"
    ).foreach: forbiddenField =>
      assert(
        !renderedSurfaceNames.exists(_.toLowerCase.contains(forbiddenField.toLowerCase)),
        s"RenderedLine exposed $forbiddenField"
      )

    Vector(
      "exd6 takes control of the file.",
      "exd6 opens a route for the rook.",
      "exd6 creates pressure.",
      "exd6 takes the initiative.",
      "exd6 weakens the structure.",
      "exd6 creates a weakness.",
      "exd6 breaks through.",
      "exd6 wins space.",
      "exd6 wins a pawn.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 forces the issue."
    ).foreach: output =>
      val candidate = rendered.copy(text = output)
      assertEquals(LlmNarrationSmoke.mockNarrate(plan, candidate), None, output)
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    Vector(
      plan.copy(role = Role.Support),
      plan.copy(role = Role.Context),
      plan.copy(role = Role.Blocked, debugOnly = true),
      plan.copy(allowedClaim = None),
      plan.copy(allowedClaim = Some(ExplanationClaim.MaterialBalanceChanges)),
      plan.copy(allowedClaim = Some(ExplanationClaim.CreatesPassedPawn)),
      plan.copy(route = None),
      plan.copy(routeSan = None),
      plan.copy(evidenceLine = None),
      plan.copy(target = None),
      plan.copy(anchor = None),
      plan.copy(forbiddenWording = Vector.empty)
    ).foreach: invalidPlan =>
      assertEquals(DeterministicRenderer.fromPlan(invalidPlan), None, invalidPlan.toString)

  test("FileOpened-8 LLM smoke reuses 8B boundary without adding why the file matters"):
    val facts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: exd6 opens the e-file.",
      "claimKey: opens_file",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"FileOpened-8 prompt missing $required")

    Vector(
      "Story(",
      "raw Story",
      "FileOpenedProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "principal variation",
      "proofFailures",
      "sourceRow",
      "source rows"
    ).foreach: forbiddenInput =>
      assertEquals(prompt.contains(forbiddenInput), false, s"FileOpened-8 prompt leaked $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("exd6 opens the e-file."))
    assertEquals(LlmNarrationSmoke.check(plan, rendered, "exd6 opens the e-file.").accepted, true)

    Vector(
      "exd6 controls the e-file.",
      "exd6 gives the rook activity on the e-file.",
      "exd6 creates pressure on the e-file.",
      "exd6 takes the initiative.",
      "exd6 creates a weakness.",
      "exd6 weakens the structure.",
      "exd6 breaks through.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 threatens promotion.",
      "exd6 is the best move.",
      "exd6 is the only move.",
      "exd6 is forced.",
      "exd6 is a strategic claim.",
      "exd6 opens the e-file for later play.",
      "exd6 opens the e-file for future play.",
      "exd6 opens the e-file and improves the position.",
      "exd6 opens the e-file because that file matters.",
      "Nf3 also opens the e-file.",
      "exd6 opens the e-file and then Re1 uses it."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

  test("PBFN-1 PawnBlockProof proves only a legal move landing on rival pawn next square"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val proof = PawnBlockProof.fromBoardFacts(facts, move)

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(proof.blockingSide, Side.White)
    assertEquals(proof.rivalSide, Side.Black)
    assertEquals(proof.blockMove, Some(move))
    assertEquals(proof.legalMove, true)
    assertEquals(proof.movingPieceBefore, Some(Piece(Side.White, Man.Knight, Square('g', 5))))
    assertEquals(proof.movingPieceAfter, Some(Piece(Side.White, Man.Knight, Square('e', 6))))
    assertEquals(proof.originSquare, Some(Square('g', 5)))
    assertEquals(proof.destinationSquare, Some(Square('e', 6)))
    assertEquals(proof.blockedPawn, Some(Piece(Side.Black, Man.Pawn, Square('e', 7))))
    assertEquals(proof.blockedPawnSquare, Some(Square('e', 7)))
    assertEquals(proof.blockedPawnNextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(proof.nextAdvanceSquareOccupiedAfter, true)
    assertEquals(proof.occupyingPieceBelongsToBlockingSide, true)
    assertEquals(proof.exactAfterBoardReplay, true)
    assertEquals(proof.blockCreatedByMove, true)
    assertEquals(proof.ordinaryDirectOneSquarePawnBlock, true)
    assertEquals(facts.seen.passedPawnObservations.exists(_.pawn == proof.blockedPawn.get), false)

  test("PBFN-1 PawnBlockProof rejects pre-existing blocks pawn contact passed pawn stops and pawn capture"):
    val preExistingFacts = BoardFacts.fromFen("7k/4p3/4N3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val preExistingMove = Line(Square('e', 1), Square('f', 1))
    val contactFacts = BoardFacts.fromFen("7k/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val contactMove = Line(Square('e', 4), Square('e', 5))
    val passedStopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val passedStopMove = Line(Square('g', 7), Square('e', 6))
    val captureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureMove = Line(Square('e', 5), Square('d', 6))

    val preExistingProof = PawnBlockProof.fromBoardFacts(preExistingFacts, preExistingMove)
    val contactProof = PawnBlockProof.fromBoardFacts(contactFacts, contactMove)
    val passedStopBlockProof = PawnBlockProof.fromBoardFacts(passedStopFacts, passedStopMove)
    val captureProof = PawnBlockProof.fromBoardFacts(captureFacts, captureMove)

    assertEquals(preExistingProof.complete, false)
    assertEquals(preExistingProof.nextAdvanceSquareOccupiedAfter, false)
    assertEquals(preExistingProof.blockCreatedByMove, false)

    assertEquals(PawnBreakProof.fromBoardFacts(contactFacts, contactMove).complete, true)
    assertEquals(contactProof.complete, false)
    assertEquals(contactProof.ordinaryDirectOneSquarePawnBlock, false)

    assertEquals(PawnStopProof.fromBoardFacts(passedStopFacts, passedStopMove).complete, true)
    assertEquals(passedStopBlockProof.complete, false)
    assertEquals(passedStopBlockProof.blockedPawn, None)

    assertEquals(PawnCaptureProof.fromBoardFacts(captureFacts, captureMove).complete, true)
    assertEquals(captureProof.complete, false)
    assertEquals(captureProof.blockedPawn, None)

  test("PBFN-2 ScenePawnBlock writer pins exact block Story identity"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val proof = PawnBlockProof.fromBoardFacts(facts, move)
    val story = ScenePawnBlock.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(proof.complete, true)
    assertEquals(story.scene, Scene.PawnBlock)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBlock))
    assertEquals(story.side, Side.White)
    assertEquals(story.rival, Side.Black)
    assertEquals(story.target, Some(Square('e', 6)))
    assertEquals(story.anchor, Some(Square('g', 5)))
    assertEquals(story.route, Some(move))
    assertEquals(story.routeSan, Some("Ne6"))
    assertEquals(story.pawnBlockProof, Some(proof))
    assertEquals(story.proofFailures, Vector.empty)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.passedPawnCreatedProof, None)
    assertEquals(story.fileOpenedProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.story.scene, Scene.PawnBlock)

  test("PBFN-2 ScenePawnBlock blocks refuted engine checks and sibling pawn claims"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get
    val refutingCheck = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(220)),
      evalAfter = Some(EngineEval(20)),
      depth = Some(14),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val refuted = ScenePawnBlock.withEngineCheck(story, refutingCheck).get

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val breakFacts = BoardFacts.fromFen("7k/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val createdFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val createdMove = Line(Square('e', 5), Square('d', 6))
    val openedFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))

    assertEquals(refutingCheck.status, EngineCheckStatus.Refutes)
    assertEquals(refuted.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))
    assertEquals(StoryTable.choose(Vector(refuted)).head.role, Role.Blocked)

    assertEquals(ScenePawnStop.write(stopFacts, stopMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(stopFacts, stopMove), None)

    assertEquals(ScenePawnBreak.write(breakFacts, breakMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(breakFacts, breakMove), None)

    assertEquals(ScenePassedPawnCreated.write(createdFacts, createdMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(createdFacts, createdMove), None)

    assertEquals(SceneFileOpened.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(openedFacts, openedMove), None)

  test("PBFN-3 PawnBlockProof negative corpus stays silent outside exact block"):
    val positiveFacts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Line(Square('g', 5), Square('e', 6))

    def assertNoPawnBlock(label: String, facts: BoardFacts, move: Line, expectedMissing: String): Unit =
      val proof = PawnBlockProof.fromBoardFacts(facts, move)
      assertEquals(proof.complete, false, label)
      assert(
        proof.missingEvidence.exists(_.missing.contains(expectedMissing)),
        s"$label missing evidence did not include $expectedMissing: ${proof.missingEvidence}"
      )
      assertEquals(ScenePawnBlock.write(facts, move), None, label)

    assertNoPawnBlock(
      "illegal move",
      positiveFacts,
      Line(Square('g', 5), Square('g', 7)),
      "legal move identity"
    )

    val sameBoardMissing = minimalBoardFacts(
      sideLegal = readyMoves(line = blockMove),
      pieces = Vector(
        Piece(Side.White, Man.King, Square('e', 1)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.White, Man.Knight, Square('g', 5)),
        Piece(Side.Black, Man.Pawn, Square('e', 7))
      )
    )
    assertNoPawnBlock("same-board proof missing", sameBoardMissing, blockMove, "same-board proof")

    assertNoPawnBlock(
      "blocked pawn missing",
      BoardFacts.fromFen("7k/8/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get,
      blockMove,
      "one blocked rival pawn"
    )
    assertNoPawnBlock(
      "blocked piece is not pawn",
      BoardFacts.fromFen("7k/4n3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get,
      blockMove,
      "one blocked rival pawn"
    )
    assertNoPawnBlock(
      "blocked pawn is not rival",
      BoardFacts.fromFen("7k/8/8/3PP1N1/8/8/8/4K3 w - - 0 1").toOption.get,
      blockMove,
      "one blocked rival pawn"
    )

    val impossibleNextSquare = minimalBoardFacts(
      pieces = Vector(
        Piece(Side.White, Man.King, Square('e', 1)),
        Piece(Side.Black, Man.King, Square('h', 8)),
        Piece(Side.White, Man.Knight, Square('g', 2)),
        Piece(Side.Black, Man.Pawn, Square('e', 1))
      )
    )
    assertNoPawnBlock(
      "next advance square cannot be calculated",
      impossibleNextSquare,
      Line(Square('g', 2), Square('e', 1)),
      "blocked pawn next advance square"
    )

    assertNoPawnBlock(
      "next advance square empty after replay",
      sameBoardMissing,
      blockMove,
      "next advance square occupied after move"
    )
    assertNoPawnBlock(
      "occupying piece is not proven as blocking side",
      sameBoardMissing,
      blockMove,
      "occupying piece belongs to blocking side"
    )

    val preExistingFacts = BoardFacts.fromFen("7k/4p3/4N3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val preExistingMove = Line(Square('e', 1), Square('f', 1))
    assertNoPawnBlock("block not created by move", preExistingFacts, preExistingMove, "block created by this move")
    assertNoPawnBlock("pre-existing blocked pawn", preExistingFacts, preExistingMove, "ordinary direct one-square pawn block")

    val contactFacts = BoardFacts.fromFen("7k/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val contactMove = Line(Square('e', 4), Square('e', 5))
    assertEquals(PawnBreakProof.fromBoardFacts(contactFacts, contactMove).complete, true)
    assertNoPawnBlock("diagonal pawn contact only", contactFacts, contactMove, "ordinary direct one-square pawn block")

    val captureFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val captureMove = Line(Square('e', 5), Square('d', 6))
    assertEquals(PawnCaptureProof.fromBoardFacts(captureFacts, captureMove).complete, true)
    assertNoPawnBlock("capture event", captureFacts, captureMove, "one blocked rival pawn")

    val passedStopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val passedStopMove = Line(Square('g', 7), Square('e', 6))
    assertEquals(PawnStopProof.fromBoardFacts(passedStopFacts, passedStopMove).complete, true)
    assertNoPawnBlock("passed pawn stop home", passedStopFacts, passedStopMove, "passed pawn stop")

  test("PBFN-3 ScenePawnBlock stays silent for adjacent pawn/material/downstream meanings"):
    val blockFacts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(blockFacts, blockMove).get
    val verdict = StoryTable.choose(Vector(story)).head

    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("blocks_pawn"))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("blocks_pawn")
    )

    Vector(Plan.Blockade, Plan.Cramp, Plan.WeakSquare, Plan.Isolani, Plan.BackwardPawn).foreach: plan =>
      val contaminated = story.copy(plan = Some(plan))
      val contaminatedVerdict = StoryTable.choose(Vector(contaminated)).head
      assertEquals(contaminatedVerdict.role, Role.Blocked, s"PawnBlock accepted contaminated plan $plan")
      assertEquals(ExplanationPlan.fromSelected(contaminatedVerdict), None, s"plan leaked for $plan")

    val promotionThreatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionThreatMove = Line(Square('e', 6), Square('e', 7))
    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val fileOpenedFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val fileOpenedMove = Line(Square('e', 5), Square('d', 6))
    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))

    assertEquals(ScenePromotionThreat.write(promotionThreatFacts, promotionThreatMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(promotionThreatFacts, promotionThreatMove), None)

    assertEquals(ScenePromotion.write(promotionFacts, promotionMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(promotionFacts, promotionMove), None)

    assertEquals(SceneFileOpened.write(fileOpenedFacts, fileOpenedMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(fileOpenedFacts, fileOpenedMove), None)

    assertEquals(SceneMaterial.write(materialFacts, materialMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(materialFacts, materialMove), None)

  test("PBFN-4 reuses EngineCheck without creating engine-owned PawnBlock claims"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get
    val reply = Line(Square('h', 8), Square('g', 8))

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
        replyLine = Some(EngineLine(Vector(reply))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = freshness,
        requestedStatus = status
      )

    val engineOnly = check(EngineCheckStatus.Supports, storyInput = None)
    val supports = ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports)).get
    val caps = ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val unknown = ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Unknown)).get
    val refutes =
      ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, before = 220, after = 20)).get
    val wrongRoute = check(
      EngineCheckStatus.Supports,
      engineLine = Some(EngineLine(Vector(Line(Square('g', 5), Square('f', 7)))))
    )

    assertEquals(engineOnly.status, EngineCheckStatus.Unknown)
    assertEquals(engineOnly.storyBound, false)
    assertEquals(engineOnly.publicClaimAllowed, false)
    assertEquals(ScenePawnBlock.withEngineCheck(story, wrongRoute), None)
    assertEquals(ScenePawnBlock.withEngineCheck(story.copy(pawnBlockProof = None), check(EngineCheckStatus.Supports)), None)
    assertEquals(StoryTable.choose(Vector.empty), Vector.empty, "EngineCheck alone cannot create PawnBlock")

    assertEquals(supports.engineCheck.map(_.status), Some(EngineCheckStatus.Supports))
    assertEquals(caps.engineCheck.map(_.status), Some(EngineCheckStatus.Caps))
    assertEquals(unknown.engineCheck.map(_.status), Some(EngineCheckStatus.Unknown))
    assertEquals(refutes.engineCheck.map(_.status), Some(EngineCheckStatus.Refutes))

    val baseVerdict = StoryTable.choose(Vector(story)).head
    val supportsVerdict = StoryTable.choose(Vector(supports)).head
    val capsVerdict = StoryTable.choose(Vector(caps)).head
    val unknownVerdict = StoryTable.choose(Vector(unknown)).head
    val refutesVerdict = StoryTable.choose(Vector(refutes)).head

    assertEquals(supportsVerdict.role, Role.Lead)
    assertEquals(supportsVerdict.leadAllowed, true)
    assertEquals(supportsVerdict.engineCheckStatus, Some(EngineCheckStatus.Supports))
    assertEquals(supportsVerdict.engineStrengthLimited, false)
    assertEquals(ExplanationPlan.fromSelected(supportsVerdict).flatMap(_.allowedClaim).map(_.key), Some("blocks_pawn"))
    assertEquals(
      ExplanationPlan.fromSelected(supportsVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("blocks_pawn")
    )

    assertEquals(capsVerdict.role, Role.Lead)
    assertEquals(capsVerdict.leadAllowed, true)
    assertEquals(capsVerdict.engineCheckStatus, Some(EngineCheckStatus.Caps))
    assertEquals(capsVerdict.engineStrengthLimited, true)
    assertEquals(ExplanationPlan.fromSelected(capsVerdict), None)

    assertEquals(unknownVerdict.role, Role.Lead)
    assertEquals(unknownVerdict.leadAllowed, true)
    assertEquals(unknownVerdict.engineCheckStatus, Some(EngineCheckStatus.Unknown))
    assertEquals(unknownVerdict.engineStrengthLimited, false)
    assertEquals(unknownVerdict.values, baseVerdict.values)
    assertEquals(ExplanationPlan.fromSelected(unknownVerdict).flatMap(_.allowedClaim).map(_.key), Some("blocks_pawn"))
    assertEquals(
      ExplanationPlan.fromSelected(unknownVerdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("blocks_pawn")
    )

    assertEquals(refutesVerdict.role, Role.Blocked)
    assertEquals(refutesVerdict.leadAllowed, false)
    assertEquals(refutesVerdict.engineCheckStatus, Some(EngineCheckStatus.Refutes))
    assertEquals(ExplanationPlan.fromSelected(refutesVerdict), None)

  test("PBFN-4 PawnBlock engine reuse forbids engine eval and strategy wording"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get
    val check = EngineCheck.fromStory(
      facts = facts,
      story = Some(story),
      engineLine = Some(EngineLine(Vector(move))),
      replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
      evalBefore = Some(EngineEval(31337)),
      evalAfter = Some(EngineEval(31338)),
      depth = Some(18),
      freshnessPly = Some(0),
      requestedStatus = EngineCheckStatus.Supports
    )
    val verdict = StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check).get)).head

    assertEquals(verdict.role, Role.Lead)
    assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim).map(_.key), Some("blocks_pawn"))
    assertEquals(
      ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan).map(_.claimKey),
      Some("blocks_pawn")
    )
    assertEquals(verdict.values.contains(31337.0), false)
    assertEquals(verdict.values.contains(31338.0), false)

    val forbidden = Vector(
      "engine says",
      "eval",
      "best move",
      "only move",
      "wins pawn",
      "restricts opponent",
      "creates pressure",
      "strategic blockade"
    )
    val runtimeSource =
      Vector(
        "ScenePawnBlock.scala",
        "PawnBlockProof.scala"
      ).map(name =>
        Files
          .readString(Paths.get(s"modules/commentary/src/main/scala/lila/commentary/chess/$name"))
          .toLowerCase
      ).mkString("\n")

    forbidden.foreach: phrase =>
      assert(!runtimeSource.contains(phrase), s"PBFN-4 forbidden wording reached runtime source: $phrase")

  test("PBFN-5 StoryTable keeps PawnBlock below existing claim homes"):
    final case class CollisionRow(label: String, story: Story, claim: Option[ExplanationClaim])

    def eventProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = 0,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def materialProof(value: Int): Proof =
      eventProof(value).copy(forcing = value, conversionPrize = value)

    def strong(story: Story, value: Int = 99): Story =
      story.copy(proof = eventProof(value))

    def strongMaterial(story: Story, value: Int = 99): Story =
      story.copy(proof = materialProof(value))

    def rowId(rows: Vector[CollisionRow], story: Story): String =
      rows.collectFirst { case row if row.story == story => row.label }.getOrElse(s"unknown:$story")

    def shape(rows: Vector[CollisionRow], verdicts: Vector[Verdict]) =
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

    def assertStableCollision(label: String, rows: Vector[CollisionRow], expectedLead: String): Vector[Verdict] =
      val forward = StoryTable.choose(rows.map(_.story))
      val reverse = StoryTable.choose(rows.reverse.map(_.story))
      val sorted = StoryTable.choose(rows.sortBy(_.label).map(_.story))
      val forwardShape = shape(rows, forward)

      assertEquals(shape(rows, reverse), forwardShape, s"$label reverse input order")
      assertEquals(shape(rows, sorted), forwardShape, s"$label sorted input order")
      assertEquals(forward.count(_.role == Role.Lead), 1, label)
      assertEquals(rowId(rows, forward.head.story), expectedLead, label)

      forward.foreach: verdict =>
        val id = rowId(rows, verdict.story)
        val claim = rows.find(_.story == verdict.story).flatMap(_.claim)
        if verdict.role == Role.Lead then
          assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(_.allowedClaim), claim, s"$label $id")
          claim.foreach: expected =>
            val rendered = DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(verdict).get).get
            assertEquals(rendered.claimKey, expected.key, s"$label $id")
        else if id == "Scene.PawnBlock" then assertNoStandaloneText(s"$label PawnBlock non-lead", verdict)
      forward

    val blockFacts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock = CollisionRow("Scene.PawnBlock", strong(ScenePawnBlock.write(blockFacts, blockMove).get), None)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop =
      CollisionRow(
        "Scene.PawnStop",
        strong(ScenePawnStop.write(stopFacts, stopMove).get),
        Some(ExplanationClaim.StopsPassedPawnNextAdvance)
      )

    val breakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak =
      CollisionRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(breakFacts, breakMove).get),
        Some(ExplanationClaim.ChallengesPawnDirectly)
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture =
      CollisionRow(
        "Scene.PawnCapture",
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.CapturesPawn)
      )
    val passedPawnCreated =
      CollisionRow(
        "Scene.PassedPawnCreated",
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.CreatesPassedPawn)
      )
    val fileOpened =
      CollisionRow(
        "Scene.FileOpened",
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        Some(ExplanationClaim.OpensFile)
      )

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      CollisionRow(
        "Scene.PawnAdvance",
        strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get),
        Some(ExplanationClaim.AdvancesPassedPawn)
      )

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow(
        "Scene.PromotionThreat",
        strong(ScenePromotionThreat.write(threatFacts, threatMove).get),
        Some(ExplanationClaim.CreatesPromotionThreat)
      )

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion =
      CollisionRow(
        "Scene.Promotion",
        strong(ScenePromotion.write(promotionFacts, promotionMove).get),
        Some(ExplanationClaim.PromotesPawn)
      )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material =
      CollisionRow(
        "Scene.Material",
        strongMaterial(SceneMaterial.write(materialFacts, materialMove).get),
        Some(ExplanationClaim.MaterialBalanceChanges)
      )
    val hanging =
      CollisionRow(
        "Tactic.Hanging",
        strongMaterial(TacticHanging.write(materialFacts, materialMove).get),
        Some(ExplanationClaim.CanWinPiece)
      )

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow(
        "Scene.Defense",
        strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get),
        Some(ExplanationClaim.DefendsPiece)
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      CollisionRow(
        "Tactic.DiscoveredAttack",
        strong(
          TacticDiscoveredAttack
            .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
            .get
        ),
        Some(ExplanationClaim.RevealsAttackOnPiece)
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      CollisionRow(
        "Tactic.Pin",
        strong(
          TacticPin
            .write(
              pinFacts,
              Some(discoveredMove),
              Some(Square('b', 1)),
              Some(Square('g', 6)),
              Some(Square('h', 7))
            )
            .get
        ),
        Some(ExplanationClaim.PinsPiece)
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      CollisionRow(
        "Tactic.RemoveGuard",
        strong(
          TacticRemoveGuard
            .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
            .get
        ),
        Some(ExplanationClaim.RemovesDefender)
      )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      CollisionRow(
        "Tactic.Skewer",
        strong(
          TacticSkewer
            .write(
              skewerFacts,
              Some(skewerMove),
              Some(Square('e', 1)),
              Some(Square('e', 5)),
              Some(Square('e', 8))
            )
            .get
        ),
        Some(ExplanationClaim.SkewersPieceToPiece)
      )

    val existingRows = Vector(
      pawnStop,
      pawnBreak,
      pawnCapture,
      passedPawnCreated,
      fileOpened,
      pawnAdvance,
      promotionThreat,
      promotion,
      material,
      hanging,
      defense,
      discovered,
      pin,
      removeGuard,
      skewer
    )

    existingRows.foreach: existing =>
      assertStableCollision(s"PawnBlock vs ${existing.label}", Vector(pawnBlock, existing), existing.label)

    val fullRows = pawnBlock +: existingRows
    val fullVerdicts = assertStableCollision("PawnBlock full collision", fullRows, "Tactic.Hanging")
    fullVerdicts.filter(verdict => rowId(fullRows, verdict.story) == "Scene.PawnBlock").foreach: verdict =>
      assertNoStandaloneText("PawnBlock full collision has no standalone text", verdict)

    assertEquals(PawnStopProof.fromBoardFacts(stopFacts, stopMove).complete, true)
    assertEquals(ScenePawnBlock.write(stopFacts, stopMove), None, "actual passed-pawn stop remains PawnStop")
    assertEquals(PawnBreakProof.fromBoardFacts(breakFacts, breakMove).complete, true)
    assertEquals(ScenePawnBlock.write(breakFacts, breakMove), None, "pawn contact remains PawnBreak")
    assertEquals(SceneMaterial.write(materialFacts, materialMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(materialFacts, materialMove), None, "material remains Material")
    assertEquals(ScenePassedPawnCreated.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(openedFacts, openedMove), None, "created passer remains PassedPawnCreated")
    assertEquals(SceneFileOpened.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(openedFacts, openedMove), None, "file opening remains FileOpened")

  test("PBFN-5 capped and refuted PawnBlock never produce standalone text"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped = ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Caps)).get
    val refuted = ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, 220, 20)).get

    Vector("capped" -> capped, "refuted" -> refuted).foreach: (label, checkedStory) =>
      val verdict = StoryTable.choose(Vector(checkedStory)).head
      assertEquals(ExplanationPlan.fromSelected(verdict), None, label)
      assertEquals(ExplanationPlan.fromSelected(verdict).flatMap(DeterministicRenderer.fromPlan), None, label)

  test("PBFN-6 ExplanationPlan admits only selected uncapped PawnBlock Lead"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90, 90, 90, 90, 90, 90, 90))
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val forbiddenClaimKeys = Vector(
      "stops_passed_pawn",
      "stops_pawn_advance",
      "challenges_pawn",
      "captures_rival_pawn",
      "creates_passed_pawn",
      "opens_file",
      "wins_pawn",
      "wins_material",
      "weakens_structure",
      "fixes_pawn",
      "creates_blockade",
      "restricts_opponent",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced"
    )

    assertEquals(verdict.role, Role.Lead)
    assertEquals(verdict.leadAllowed, true)
    assertEquals(verdict.selected, true)
    assertEquals(verdict.engineStrengthLimited, false)
    assertEquals(plan.role, Role.Lead)
    assertEquals(plan.scene, Scene.PawnBlock)
    assertEquals(plan.tactic, None)
    assertEquals(plan.side, Side.White)
    assertEquals(plan.target, Some(Square('e', 6)))
    assertEquals(plan.anchor, Some(Square('g', 5)))
    assertEquals(plan.route, Some(move))
    assertEquals(plan.routeSan, Some("Ne6"))
    assertEquals(plan.secondaryTarget, None)
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(plan.evidenceLine, Some(move))
    assertEquals(plan.strength, ExplanationStrength.Bounded)
    assertEquals(plan.relations, Vector.empty)
    assertEquals(plan.debugOnly, false)
    assertEquals(plan.supportContextLinks, Vector.empty)
    assertEquals(ExplanationClaim.values.map(_.key).contains("blocks_pawn"), true)
    forbiddenClaimKeys.foreach: key =>
      assertEquals(plan.allowedClaim.exists(_.key == key), false, s"PawnBlock allowed forbidden claim key $key")
      assert(plan.forbiddenWording.map(_.key).contains(key), s"PawnBlock plan must forbid $key")

    Vector(
      "unselected" -> verdict.copy(selected = false),
      "support" -> verdict.copy(role = Role.Support, leadAllowed = false),
      "context" -> verdict.copy(role = Role.Context, leadAllowed = false),
      "blocked" -> verdict.copy(role = Role.Blocked, leadAllowed = false)
    ).foreach: (label, nonLeadVerdict) =>
      assertEquals(ExplanationPlan.fromSelected(nonLeadVerdict), None, label)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped = StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, 220, 20)).get)).head

    assertEquals(ExplanationPlan.fromSelected(capped), None, "capped PawnBlock")
    assertEquals(ExplanationPlan.fromSelected(refuted), None, "refuted PawnBlock")

  test("PBFN-7 DeterministicRenderer phrases only bounded PawnBlock text"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90, 90, 90, 90, 90, 90, 90))
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(rendered.text, "Ne6 blocks the pawn from advancing.")
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.forbiddenCheckPassed, true)

    Vector(
      "fixes the pawn",
      "weakens the pawn",
      "creates a weakness",
      "creates a blockade",
      "stops the passed pawn",
      "restricts Black",
      "takes space",
      "creates pressure",
      "wins a tempo",
      "best move",
      "only move",
      "forces"
    ).foreach: forbidden =>
      assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains(forbidden.toLowerCase(java.util.Locale.ROOT)))

    Vector(
      "support" -> plan.copy(role = Role.Support, allowedClaim = None),
      "context" -> plan.copy(role = Role.Context, allowedClaim = None),
      "blocked" -> plan.copy(role = Role.Blocked, debugOnly = true),
      "wrong claim" -> plan.copy(allowedClaim = Some(ExplanationClaim.StopsPassedPawnNextAdvance)),
      "pawn stop scene" -> plan.copy(scene = Scene.PawnStop),
      "tactic set" -> plan.copy(tactic = Some(Tactic.Hanging)),
      "secondary target" -> plan.copy(secondaryTarget = Some(Square('e', 7))),
      "missing target" -> plan.copy(target = None),
      "missing anchor" -> plan.copy(anchor = None),
      "missing route" -> plan.copy(route = None),
      "missing san" -> plan.copy(routeSan = None),
      "wrong evidence" -> plan.copy(evidenceLine = Some(Line(Square('g', 5), Square('f', 7)))),
      "no forbidden boundary" -> plan.copy(forbiddenWording = Vector.empty)
    ).foreach: (label, malformedPlan) =>
      assertEquals(DeterministicRenderer.fromPlan(malformedPlan), None, label)

    def check(status: EngineCheckStatus, before: Int = 20, after: Int = 20): EngineCheck =
      EngineCheck.fromStory(
        facts = facts,
        story = Some(story),
        engineLine = Some(EngineLine(Vector(move))),
        replyLine = Some(EngineLine(Vector(Line(Square('h', 8), Square('g', 8))))),
        evalBefore = Some(EngineEval(before)),
        evalAfter = Some(EngineEval(after)),
        depth = Some(18),
        freshnessPly = Some(0),
        requestedStatus = status
      )

    val capped = StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Caps)).get)).head
    val refuted =
      StoryTable.choose(Vector(ScenePawnBlock.withEngineCheck(story, check(EngineCheckStatus.Supports, 220, 20)).get)).head

    assertEquals(ExplanationPlan.fromSelected(capped).flatMap(DeterministicRenderer.fromPlan), None, "capped")
    assertEquals(ExplanationPlan.fromSelected(refuted).flatMap(DeterministicRenderer.fromPlan), None, "refuted")

  test("PBFN-8 LLM smoke reuses 8B boundary for PawnBlock without new chess facts"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get.copy(proof = proof(90, 90, 90, 90, 90, 90, 90))
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    Vector(
      "renderedText: Ne6 blocks the pawn from advancing.",
      "claimKey: blocks_pawn",
      "strength: bounded",
      "forbiddenWording:",
      "instruction: Rephrase only. Do not add chess facts."
    ).foreach: required =>
      assert(prompt.contains(required), s"PBFN-8 prompt must include allowed input: $required")

    Vector(
      "Story(",
      "raw Story",
      "PawnBlockProof",
      "PawnStopProof",
      "BoardFacts",
      "EngineCheck",
      "EngineLine",
      "EngineEval",
      "raw PV",
      "principal variation",
      "proofFailures",
      "source row",
      "blocked pawn square",
      "exact after-board replay"
    ).foreach: forbiddenInput =>
      assert(!prompt.contains(forbiddenInput), s"PBFN-8 prompt must not expose $forbiddenInput")

    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some("Ne6 blocks the pawn from advancing."))
    assertEquals(
      LlmNarrationSmoke.check(plan, rendered, "Ne6 blocks the pawn from advancing."),
      NarrationSmokeCheck(true, Vector.empty)
    )

    Vector(
      "raw Story" -> "Raw Story says Ne6 blocks the pawn.",
      "raw PawnBlockProof" -> "PawnBlockProof proves Ne6 blocks the pawn.",
      "raw PawnStopProof" -> "PawnStopProof says Ne6 stops a pawn.",
      "BoardFacts" -> "BoardFacts show the pawn is blocked.",
      "EngineCheck" -> "EngineCheck supports Ne6.",
      "raw PV" -> "Raw PV: Ne6 Kg8.",
      "proofFailures" -> "proofFailures are empty, so Ne6 works."
    ).foreach: (label, output) =>
      val result = LlmNarrationSmoke.check(plan, rendered, output)
      assertEquals(result.accepted, false, label)
      assert(result.violations.contains("raw_input"), s"$label must be rejected as raw input leak: $result")

    Vector(
      "new move" -> "Nf5 also blocks the pawn.",
      "new line" -> "After Ne6 Kd7 the pawn is blocked.",
      "weakness" -> "Ne6 weakens the pawn.",
      "blockade" -> "Ne6 creates a blockade.",
      "restriction" -> "Ne6 restricts Black.",
      "pressure" -> "Ne6 creates pressure.",
      "initiative" -> "Ne6 takes the initiative.",
      "material" -> "Ne6 wins material.",
      "passed-pawn claim" -> "Ne6 stops the passed pawn.",
      "best move" -> "Ne6 is the best move.",
      "only move" -> "Ne6 is the only move.",
      "forced" -> "Ne6 forces the pawn to stay put."
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
    val mismatchedRendered = rendered.copy(claimKey = "stops_passed_pawn")
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
      "fromPawnBlockProof",
      "fromPawnStopProof",
      "fromBoardFacts",
      "fromEngineCheck",
      "fromEngineLine",
      "callApi",
      "productionApi"
    ).foreach: method =>
      assert(!methodNames.contains(method), s"PBFN-8 LLM smoke must not expose $method")
    Vector("Story", "PawnBlockProof", "PawnStopProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach:
      parameter => assert(!parameterNames.contains(parameter), s"PBFN-8 LLM smoke must not accept $parameter")

  test("PBFNC-0 Pawn Blocking Fixed Pawn Neighborhood closeout keeps one narrow event separated"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val pawnBlockProof = PawnBlockProof.fromBoardFacts(facts, move)
    val story = ScenePawnBlock.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(pawnBlockProof.publicClaimAllowed, false)
    assertEquals(pawnBlockProof.complete, true)
    assertEquals(pawnBlockProof.ordinaryDirectOneSquarePawnBlock, true)
    assertEquals(pawnBlockProof.blockedPawn.exists(_.man == Man.Pawn), true)
    assertEquals(pawnBlockProof.blockedPawnSquare, Some(Square('e', 7)))
    assertEquals(pawnBlockProof.blockedPawnNextAdvanceSquare, Some(Square('e', 6)))
    assertEquals(pawnBlockProof.destinationSquare, pawnBlockProof.blockedPawnNextAdvanceSquare)
    assertEquals(pawnBlockProof.nextAdvanceSquareOccupiedAfter, true)
    assertEquals(pawnBlockProof.occupyingPieceBelongsToBlockingSide, true)
    assertEquals(pawnBlockProof.blockCreatedByMove, true)

    assertEquals(story.scene, Scene.PawnBlock)
    assertEquals(story.tactic, None)
    assertEquals(story.plan, None)
    assertEquals(story.writer, Some(StoryWriter.ScenePawnBlock))
    assertEquals(story.side, pawnBlockProof.blockingSide)
    assertEquals(story.rival, pawnBlockProof.rivalSide)
    assertEquals(story.target, pawnBlockProof.blockedPawnNextAdvanceSquare)
    assertEquals(story.anchor, pawnBlockProof.originSquare)
    assertEquals(story.route, pawnBlockProof.blockMove)
    assertEquals(story.pawnBlockProof, Some(pawnBlockProof))
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.passedPawnCreatedProof, None)
    assertEquals(story.fileOpenedProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)
    assertEquals(story.proof.forcing, 0)
    assertEquals(story.proof.conversionPrize, 0)
    assertEquals(story.proof.kingHeat, 0)
    assertEquals(story.proof.pieceSupport, 0)

    assertEquals(ExplanationClaim.PawnBlockAllowed.map(_.key), Vector("blocks_pawn"))
    Vector(
      ExplanationClaim.PawnStopAllowed,
      ExplanationClaim.PawnBreakAllowed,
      ExplanationClaim.PawnCaptureAllowed,
      ExplanationClaim.PassedPawnCreatedAllowed,
      ExplanationClaim.FileOpenedAllowed,
      ExplanationClaim.MaterialAllowed,
      ExplanationClaim.HangingAllowed,
      ExplanationClaim.DefenseAllowed,
      ExplanationClaim.DiscoveredAttackAllowed,
      ExplanationClaim.PinAllowed,
      ExplanationClaim.RemoveGuardAllowed,
      ExplanationClaim.SkewerAllowed
    ).foreach: siblingKeys =>
      assert(!siblingKeys.map(_.key).contains("blocks_pawn"), s"blocks_pawn leaked to sibling key home $siblingKeys")

    assertEquals(plan.scene, Scene.PawnBlock)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.BlocksPawn))
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(rendered.strength, "bounded")
    assertEquals(rendered.text, "Ne6 blocks the pawn from advancing.")
    assertEquals(LlmNarrationSmoke.mockNarrate(plan, rendered), Some(rendered.text))

    val forbiddenSurface =
      Vector(
        "fixed pawn",
        "weak pawn",
        "blockade",
        "restrict",
        "pressure",
        "initiative",
        "best move",
        "only move",
        "wins material",
        "passed pawn"
      )
    val publicText = Vector(rendered.text, LlmNarrationSmoke.mockNarrate(plan, rendered).get).mkString("\n").toLowerCase(java.util.Locale.ROOT)
    forbiddenSurface.foreach: phrase =>
      assert(!publicText.contains(phrase), s"PawnBlock downstream text is stronger than blocks_pawn: $phrase")

    Vector(
      "Ne6 creates a fixed pawn.",
      "Ne6 weakens the pawn.",
      "Ne6 creates a blockade.",
      "Ne6 restricts Black.",
      "Ne6 creates pressure.",
      "Ne6 takes the initiative.",
      "Ne6 wins material.",
      "Ne6 stops the passed pawn.",
      "Ne6 is the best move.",
      "Ne6 is the only move."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val proofAndWriterText =
      Vector("PawnBlockProof.scala", "ScenePawnBlock.scala")
        .map(file => Files.readString(Paths.get("modules/commentary/src/main/scala/lila/commentary/chess").resolve(file)))
        .mkString("\n")
        .toLowerCase(java.util.Locale.ROOT)
    Vector(
      "fixed pawn",
      "weak pawn",
      "blockade",
      "restrict",
      "pressure",
      "initiative",
      "best move",
      "only move",
      "wins material"
    ).foreach: phrase =>
      assert(!proofAndWriterText.contains(phrase), s"proof/writer source gained closed live authority phrase: $phrase")

    val smokeMethodNames = LlmNarrationSmoke.getClass.getDeclaredMethods.map(_.getName).toSet
    val smokeParameterNames =
      LlmNarrationSmoke.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .toSet
    Vector("fromStory", "fromPawnBlockProof", "fromBoardFacts", "fromEngineCheck", "callApi", "productionApi")
      .foreach: method =>
        assert(!smokeMethodNames.contains(method), s"PBFNC-0 closeout must not expose $method")
    Vector("Story", "PawnBlockProof", "BoardFacts", "EngineCheck", "EngineLine", "EngineEval").foreach: parameter =>
      assert(!smokeParameterNames.contains(parameter), s"PBFNC-0 closeout must not accept $parameter")

  test("PBFNC-1 scope audit keeps only PawnBlock label proof home and speech key open"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val story = ScenePawnBlock.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    val openedPawnBlockingStoryLabels =
      Scene.values.map(_.toString).filter(_ == "PawnBlock").toVector
    assertEquals(openedPawnBlockingStoryLabels, Vector("PawnBlock"))
    Vector("FixedPawn", "WeakPawn", "Blockade", "Restriction").foreach: closedLabel =>
      assert(!Scene.values.map(_.toString).contains(closedLabel), s"closed Scene.$closedLabel must not exist")

    assertEquals(story.scene, Scene.PawnBlock)
    assertEquals(story.pawnBlockProof.exists(_.complete), true)
    assertEquals(story.pawnAdvanceProof, None)
    assertEquals(story.pawnStopProof, None)
    assertEquals(story.pawnBreakProof, None)
    assertEquals(story.pawnCaptureProof, None)
    assertEquals(story.passedPawnCreatedProof, None)
    assertEquals(story.fileOpenedProof, None)
    assertEquals(story.captureResult, None)
    assertEquals(story.threatProof, None)
    assertEquals(story.defenseProof, None)
    assertEquals(story.multiTargetProof, None)
    assertEquals(story.lineProof, None)
    assertEquals(story.pinProof, None)
    assertEquals(story.removeGuardProof, None)
    assertEquals(story.skewerProof, None)

    val pawnBlockKeys = ExplanationClaim.PawnBlockAllowed.map(_.key)
    assertEquals(pawnBlockKeys, Vector("blocks_pawn"))
    val allClaimKeys = ExplanationClaim.values.map(_.key).toSet
    Vector(
      "fixed_pawn",
      "weak_pawn",
      "creates_blockade",
      "restricts_opponent",
      "creates_pressure",
      "takes_initiative"
    ).foreach: closedKey =>
      assert(!allClaimKeys.contains(closedKey), s"closed speech key $closedKey must not exist")

    assertEquals(plan.scene, Scene.PawnBlock)
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(rendered.text, "Ne6 blocks the pawn from advancing.")
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("fixed"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("weak"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("blockade"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("restrict"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("pressure"))
    assert(!rendered.text.toLowerCase(java.util.Locale.ROOT).contains("initiative"))

  test("PBFNC-2 authority duplication audit keeps proof Story and speech key separated"):
    val facts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('g', 5), Square('e', 6))
    val proof = PawnBlockProof.fromBoardFacts(facts, move)
    val story = ScenePawnBlock.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get

    assertEquals(proof.complete, true)
    assertEquals(proof.publicClaimAllowed, false)
    assertEquals(story.scene, Scene.PawnBlock)
    assertEquals(story.pawnBlockProof, Some(proof))
    assertEquals(story.target, proof.blockedPawnNextAdvanceSquare)
    assertEquals(story.route, proof.blockMove)
    assertEquals(story.routeSan, Some("Ne6"))
    assertEquals(story.proofFailures, Vector.empty)

    assertEquals(ExplanationClaim.PawnBlockAllowed, Vector(ExplanationClaim.BlocksPawn))
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.BlocksPawn))
    assertEquals(plan.allowedClaim.map(_.key), Some("blocks_pawn"))
    assertEquals(rendered.claimKey, "blocks_pawn")
    assertEquals(story.scene.toString, "PawnBlock")
    assert(story.scene.toString != "blocks_pawn")

    val rendererParameterNames =
      DeterministicRenderer.getClass.getDeclaredMethods.toVector
        .flatMap(_.getParameterTypes.toVector)
        .map(_.getSimpleName)
        .toSet
    Vector("PawnBlockProof", "Story", "BoardFacts", "PieceContact", "PawnStopProof", "PawnBreakProof").foreach:
      parameter =>
        assert(
          !rendererParameterNames.contains(parameter),
          s"PBFNC-2 renderer must not accept $parameter as public authority"
        )

    val contactFacts = BoardFacts.fromFen("7k/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val contactMove = Line(Square('e', 4), Square('e', 5))
    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))

    assertEquals(PawnBreakProof.fromBoardFacts(contactFacts, contactMove).complete, true)
    assertEquals(PawnBlockProof.fromBoardFacts(contactFacts, contactMove).complete, false)
    assertEquals(ScenePawnBlock.write(contactFacts, contactMove), None)

    assertEquals(PawnStopProof.fromBoardFacts(stopFacts, stopMove).complete, true)
    assertEquals(PawnBlockProof.fromBoardFacts(stopFacts, stopMove).complete, false)
    assertEquals(ScenePawnBlock.write(stopFacts, stopMove), None)

  test("PBFNC-3 cross meaning collision audit keeps existing rows in their homes"):
    final case class CollisionRow(label: String, story: Story, claim: ExplanationClaim)

    def eventProof(value: Int): Proof =
      proof(
        boardProof = value,
        lineProof = value,
        ownerProof = value,
        anchorProof = value,
        routeProof = value,
        persistence = value,
        immediacy = value,
        forcing = 0,
        conversionPrize = 0,
        counterplayRisk = 20,
        kingHeat = 0,
        pieceSupport = value,
        pawnSupport = value,
        clarity = value
      )

    def materialProof(value: Int): Proof =
      eventProof(value).copy(forcing = value, conversionPrize = value)

    def strong(story: Story, value: Int = 99): Story =
      story.copy(proof = eventProof(value))

    def strongMaterial(story: Story, value: Int = 99): Story =
      story.copy(proof = materialProof(value))

    def assertPawnBlockDoesNotOwn(row: CollisionRow, pawnBlock: Story): Unit =
      val verdicts = StoryTable.choose(Vector(pawnBlock, row.story))
      val lead = verdicts.head
      assertEquals(lead.story, row.story, s"PawnBlock must not own ${row.label}")
      assertEquals(ExplanationPlan.fromSelected(lead).flatMap(_.allowedClaim), Some(row.claim), row.label)
      val rendered = DeterministicRenderer.fromPlan(ExplanationPlan.fromSelected(lead).get).get
      assertEquals(rendered.claimKey, row.claim.key, row.label)

      verdicts.filter(_.story == pawnBlock).foreach: pawnBlockVerdict =>
        assertEquals(ExplanationPlan.fromSelected(pawnBlockVerdict), None, row.label)
        assertEquals(
          ExplanationPlan.fromSelected(pawnBlockVerdict).flatMap(DeterministicRenderer.fromPlan),
          None,
          row.label
        )

    val blockFacts = BoardFacts.fromFen("7k/4p3/8/3P2N1/8/8/8/4K3 w - - 0 1").toOption.get
    val blockMove = Line(Square('g', 5), Square('e', 6))
    val pawnBlock = strong(ScenePawnBlock.write(blockFacts, blockMove).get)

    val stopFacts = BoardFacts.fromFen("4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1").toOption.get
    val stopMove = Line(Square('g', 7), Square('e', 6))
    val pawnStop =
      CollisionRow(
        "Scene.PawnStop",
        strong(ScenePawnStop.write(stopFacts, stopMove).get),
        ExplanationClaim.StopsPassedPawnNextAdvance
      )

    val breakFacts = BoardFacts.fromFen("4k3/8/3p4/8/4P3/8/8/4K3 w - - 0 1").toOption.get
    val breakMove = Line(Square('e', 4), Square('e', 5))
    val pawnBreak =
      CollisionRow(
        "Scene.PawnBreak",
        strong(ScenePawnBreak.write(breakFacts, breakMove).get),
        ExplanationClaim.ChallengesPawnDirectly
      )

    val openedFacts = BoardFacts.fromFen("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val openedMove = Line(Square('e', 5), Square('d', 6))
    val pawnCapture =
      CollisionRow(
        "Scene.PawnCapture",
        strong(ScenePawnCapture.write(openedFacts, openedMove).get),
        ExplanationClaim.CapturesPawn
      )
    val passedPawnCreated =
      CollisionRow(
        "Scene.PassedPawnCreated",
        strong(ScenePassedPawnCreated.write(openedFacts, openedMove).get),
        ExplanationClaim.CreatesPassedPawn
      )
    val fileOpened =
      CollisionRow(
        "Scene.FileOpened",
        strong(SceneFileOpened.write(openedFacts, openedMove).get),
        ExplanationClaim.OpensFile
      )

    val advanceFacts = BoardFacts.fromFen("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val advanceMove = Line(Square('e', 5), Square('e', 6))
    val pawnAdvance =
      CollisionRow(
        "Scene.PawnAdvance",
        strong(ScenePawnAdvance.write(advanceFacts, advanceMove).get),
        ExplanationClaim.AdvancesPassedPawn
      )

    val threatFacts = BoardFacts.fromFen("k7/8/4P3/8/8/8/8/4K3 w - - 0 1").toOption.get
    val threatMove = Line(Square('e', 6), Square('e', 7))
    val promotionThreat =
      CollisionRow(
        "Scene.PromotionThreat",
        strong(ScenePromotionThreat.write(threatFacts, threatMove).get),
        ExplanationClaim.CreatesPromotionThreat
      )

    val promotionFacts = BoardFacts.fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1").toOption.get
    val promotionMove = Line(Square('e', 7), Square('e', 8))
    val promotion =
      CollisionRow(
        "Scene.Promotion",
        strong(ScenePromotion.write(promotionFacts, promotionMove).get),
        ExplanationClaim.PromotesPawn
      )

    val materialFacts = BoardFacts.fromFen("4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1").toOption.get
    val materialMove = Line(Square('d', 4), Square('e', 5))
    val material =
      CollisionRow(
        "Scene.Material",
        strongMaterial(SceneMaterial.write(materialFacts, materialMove).get),
        ExplanationClaim.MaterialBalanceChanges
      )
    val hanging =
      CollisionRow(
        "Tactic.Hanging",
        strongMaterial(TacticHanging.write(materialFacts, materialMove).get),
        ExplanationClaim.CanWinPiece
      )

    val defenseFacts = BoardFacts.fromFen("4k3/8/8/5n2/3Q4/8/8/4K3 w - - 0 1").toOption.get
    val defenseThreat = Line(Square('f', 5), Square('d', 4))
    val defenseMove = Line(Square('d', 4), Square('e', 4))
    val defense =
      CollisionRow(
        "Scene.Defense",
        strong(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).get),
        ExplanationClaim.DefendsPiece
      )

    val discoveredFacts = BoardFacts.fromFen("7k/8/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val discoveredMove = Line(Square('d', 3), Square('f', 4))
    val discovered =
      CollisionRow(
        "Tactic.DiscoveredAttack",
        strong(
          TacticDiscoveredAttack
            .write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6)))
            .get
        ),
        ExplanationClaim.RevealsAttackOnPiece
      )

    val pinFacts = BoardFacts.fromFen("8/7k/6r1/8/8/3N4/8/1B5K w - - 0 1").toOption.get
    val pin =
      CollisionRow(
        "Tactic.Pin",
        strong(
          TacticPin
            .write(
              pinFacts,
              Some(discoveredMove),
              Some(Square('b', 1)),
              Some(Square('g', 6)),
              Some(Square('h', 7))
            )
            .get
        ),
        ExplanationClaim.PinsPiece
      )

    val removeGuardFacts = BoardFacts.fromFen("7k/8/6r1/4n3/8/3N4/8/1B5K w - - 0 1").toOption.get
    val removeGuardMove = Line(Square('d', 3), Square('e', 5))
    val removeGuard =
      CollisionRow(
        "Tactic.RemoveGuard",
        strong(
          TacticRemoveGuard
            .write(removeGuardFacts, Some(removeGuardMove), Some(Square('g', 6)), Some(Square('e', 5)))
            .get
        ),
        ExplanationClaim.RemovesDefender
      )

    val skewerFacts = BoardFacts.fromFen("4r2k/8/8/4q3/8/8/8/R6K w - - 0 1").toOption.get
    val skewerMove = Line(Square('a', 1), Square('e', 1))
    val skewer =
      CollisionRow(
        "Tactic.Skewer",
        strong(
          TacticSkewer
            .write(
              skewerFacts,
              Some(skewerMove),
              Some(Square('e', 1)),
              Some(Square('e', 5)),
              Some(Square('e', 8))
            )
            .get
        ),
        ExplanationClaim.SkewersPieceToPiece
      )

    Vector(
      pawnStop,
      pawnBreak,
      pawnCapture,
      passedPawnCreated,
      fileOpened,
      pawnAdvance,
      promotionThreat,
      promotion,
      material,
      hanging,
      defense,
      discovered,
      pin,
      removeGuard,
      skewer
    ).foreach(row => assertPawnBlockDoesNotOwn(row, pawnBlock))

    assertEquals(PawnStopProof.fromBoardFacts(stopFacts, stopMove).complete, true)
    assertEquals(ScenePawnBlock.write(stopFacts, stopMove), None)
    assertEquals(PawnBreakProof.fromBoardFacts(breakFacts, breakMove).complete, true)
    assertEquals(ScenePawnBlock.write(breakFacts, breakMove), None)
    assertEquals(ScenePawnCapture.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(openedFacts, openedMove), None)
    assertEquals(ScenePassedPawnCreated.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(SceneFileOpened.write(openedFacts, openedMove).nonEmpty, true)
    assertEquals(ScenePawnAdvance.write(advanceFacts, advanceMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(advanceFacts, advanceMove), None)
    assertEquals(ScenePromotionThreat.write(threatFacts, threatMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(threatFacts, threatMove), None)
    assertEquals(ScenePromotion.write(promotionFacts, promotionMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(promotionFacts, promotionMove), None)
    assertEquals(SceneMaterial.write(materialFacts, materialMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(materialFacts, materialMove), None)
    assertEquals(SceneDefense.write(defenseFacts, defenseThreat, defenseMove).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(defenseFacts, defenseMove), None)
    assertEquals(TacticDiscoveredAttack.write(discoveredFacts, Some(discoveredMove), Some(Square('b', 1)), Some(Square('g', 6))).nonEmpty, true)
    assertEquals(ScenePawnBlock.write(discoveredFacts, discoveredMove), None)
    assertEquals(ScenePawnBlock.write(pinFacts, discoveredMove), None)
    assertEquals(ScenePawnBlock.write(removeGuardFacts, removeGuardMove), None)
    assertEquals(ScenePawnBlock.write(skewerFacts, skewerMove), None)

  test("FileOpened-0 stays silent outside the exact open-file event"):
    val straightFacts = BoardFacts.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1").toOption.get
    val straightMove = Line(Square('e', 2), Square('e', 4))
    val occupiedFacts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/4P3/4K3 w - - 0 1").toOption.get
    val occupiedMove = Line(Square('e', 5), Square('d', 6))
    val nonPawnFacts = BoardFacts.fromFen("4k3/8/3n4/4N3/8/8/8/4K3 w - - 0 1").toOption.get
    val nonPawnMove = Line(Square('e', 5), Square('d', 7))

    val straightProof = FileOpenedProof.fromBoardFacts(straightFacts, straightMove)
    val occupiedProof = FileOpenedProof.fromBoardFacts(occupiedFacts, occupiedMove)
    val nonPawnProof = FileOpenedProof.fromBoardFacts(nonPawnFacts, nonPawnMove)

    assertEquals(straightProof.complete, false)
    assertEquals(straightProof.leavesOriginFile, false)
    assertEquals(straightProof.originFileOpenAfter, false)
    assertEquals(SceneFileOpened.write(straightFacts, straightMove), None)

    assertEquals(occupiedProof.complete, false)
    assertEquals(occupiedProof.originFileOpenAfter, false)
    assertEquals(SceneFileOpened.write(occupiedFacts, occupiedMove), None)

    assertEquals(nonPawnProof.complete, false)
    assertEquals(nonPawnProof.legalPawnMove, false)
    assertEquals(SceneFileOpened.write(nonPawnFacts, nonPawnMove), None)

  test("FileOpened-0 downstream wording forbids activity weakness and strategy overclaims"):
    val facts = BoardFacts.fromFen("4k3/8/3n4/4P3/8/8/8/4K3 w - - 0 1").toOption.get
    val move = Line(Square('e', 5), Square('d', 6))
    val story = SceneFileOpened.write(facts, move).get
    val verdict = StoryTable.choose(Vector(story)).head
    val plan = ExplanationPlan.fromSelected(verdict).get
    val rendered = DeterministicRenderer.fromPlan(plan).get
    val prompt = LlmNarrationSmoke.codexCliPrompt(plan, rendered).get

    assertEquals(plan.scene, Scene.FileOpened)
    assertEquals(plan.allowedClaim, Some(ExplanationClaim.OpensFile))
    assertEquals(rendered.forbiddenCheckPassed, true)
    assert(!prompt.contains("FileOpenedProof"))
    assert(!prompt.contains("BoardFacts"))
    assert(!prompt.contains("EngineCheck"))

    Vector(
      "exd6 gives the rook activity on the e-file.",
      "exd6 controls the open file.",
      "exd6 creates pressure.",
      "exd6 takes the initiative.",
      "exd6 weakens Black's structure.",
      "exd6 breaks through.",
      "exd6 wins material.",
      "exd6 creates a passed pawn.",
      "exd6 is the best move.",
      "exd6 is forced."
    ).foreach: output =>
      assertEquals(LlmNarrationSmoke.check(plan, rendered, output).accepted, false, output)

    val supportPlan = plan.copy(role = Role.Support, allowedClaim = None)
    assertEquals(DeterministicRenderer.fromPlan(supportPlan), None)
    assertEquals(LlmNarrationSmoke.mockNarrate(supportPlan, rendered), None)
