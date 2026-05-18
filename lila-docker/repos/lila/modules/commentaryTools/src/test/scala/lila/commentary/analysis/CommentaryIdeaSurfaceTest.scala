package lila.commentary.analysis

import _root_.chess.{ Bishop, Color, Knight, Pawn, Piece, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.model.*
import munit.FunSuite

final class CommentaryIdeaSurfaceTest extends FunSuite:

  private def quietOpeningCtx(facts: List[Fact]): NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 4,
      playedMove = Some("e7e5"),
      playedSan = Some("e5"),
      summary = NarrativeSummary("Development", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Normal development"),
      candidates = Nil,
      openingEvent = Some(OpeningEvent.Intro("C20", "Open Game", "central development", List("e4", "e5", "Nf3"))),
      facts = facts,
      renderMode = NarrativeRenderMode.MoveReview,
      variantKey = EarlyOpeningNarrationPolicy.StandardVariant
    )

  private def forcingCtx(facts: List[Fact]): NarrativeContext =
    quietOpeningCtx(facts).copy(
      header = ContextHeader("Opening", "Critical", "OnlyMove", "High", "ExplainDefense"),
      threats = ThreatTable(
        List(ThreatRow("Material", "US", Some("f3"), 250, 1, Some("Nd2"), 1, insufficientData = false)),
        Nil
      )
    )

  private def played(
      uci: String,
      san: String,
      from: Square,
      to: Square,
      piece: Piece,
      capturedRole: Option[_root_.chess.Role] = None
  ): CommentaryIdeaSurface.PlayedMove =
    CommentaryIdeaSurface.PlayedMove(
      uci = uci,
      san = san,
      from = from,
      to = to,
      piece = piece,
      afterFen = "after",
      capturedRole = capturedRole
    )

  private def evidence(
      facts: List[Fact] = Nil,
      motifs: List[Motif] = Nil,
      openingGoal: Option[OpeningGoals.Evaluation] = None,
      openingName: Option[String] = None
  ): CommentaryIdeaSurface.MoveReviewEvidence =
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = openingName
    )

  private def moveRef(refId: String, san: String, uci: String, ply: Int): MoveReviewMoveRef =
    MoveReviewMoveRef(
      refId = refId,
      san = san,
      uci = uci,
      fenAfter = s"fen-$refId",
      ply = ply,
      moveNo = (ply + 1) / 2,
      marker = None
    )

  private def lineFacts(
      first: MoveReviewMoveRef,
      reply: Option[MoveReviewMoveRef],
      continuation: Option[MoveReviewMoveRef] = None
  ): MoveReviewPvLine.LineFacts =
    val moves = List(Some(first), reply, continuation).flatten
    MoveReviewPvLine.LineFacts(
      line = MoveReviewVariationRef(
        lineId = "line_01",
        scoreCp = 12,
        mate = None,
        depth = 16,
        moves = moves
      ),
      first = first,
      reply = reply,
      continuation = continuation
    )

  test("fact wording tags priority and consequence come from the single idea surface") {
    val fork = Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.Now)
    val pin = Fact.Pin(Square.B4, Bishop, Square.C3, Knight, Square.E1, _root_.chess.King, isAbsolute = true, FactScope.Now)
    val weak = Fact.WeakSquare(Square.D5, Color.Black, "no pawn defense", FactScope.Now)
    val kingActivity = Fact.KingActivity(Square.D4, mobility = 5, proximityToCenter = 1, FactScope.Now)
    val ctx = forcingCtx(List(fork, pin, weak, kingActivity))

    assertEquals(CommentaryIdeaSurface.tags(fork), List("fork"))
    assertEquals(CommentaryIdeaSurface.tags(pin), List("pin"))
    assertEquals(CommentaryIdeaSurface.tags(weak), List("weak_square"))
    assertEquals(CommentaryIdeaSurface.tags(kingActivity), List("king_activity"))
    assertEquals(CommentaryIdeaSurface.factPriority(Fact.HangingPiece(Square.C4, Bishop, Nil, Nil, FactScope.Now)), 0)
    assertEquals(CommentaryIdeaSurface.factPriority(pin), 1)
    assertEquals(CommentaryIdeaSurface.factPriority(fork), 2)
    assertEquals(CommentaryIdeaSurface.factPriority(weak), 4)
    assert(CommentaryIdeaSurface.statement(0, fork, ctx).exists(_.toLowerCase.contains("fork")), clue(fork))
    assert(CommentaryIdeaSurface.statement(0, pin, ctx).exists(_.toLowerCase.contains("pin")), clue(pin))
    assert(CommentaryIdeaSurface.statement(0, weak, ctx).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryIdeaSurface.statement(0, kingActivity, ctx).exists(_.toLowerCase.contains("king")), clue(kingActivity))
    assert(CommentaryIdeaSurface.branchReason(pin).exists(_.toLowerCase.contains("pin pressure")), clue(pin))
    assert(CommentaryIdeaSurface.branchReason(weak).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryIdeaSurface.consequenceBody(fork).exists(_.toLowerCase.contains("fork")), clue(fork))
    assert(CommentaryIdeaSurface.consequenceBody(pin).exists(_.toLowerCase.contains("pin")), clue(pin))
    assert(CommentaryIdeaSurface.consequenceBody(weak).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryIdeaSurface.issueConsequence(weak).exists(_.startsWith("Consequence:")), clue(weak))
  }

  test("hanging and target facts keep quiet suppression and forcing tags in the shared surface") {
    val hanging = Fact.HangingPiece(Square.E5, Pawn, List(Square.E4), Nil, FactScope.Now)
    val target = Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.Now)
    val quiet = quietOpeningCtx(List(hanging, target))
    val forcing = forcingCtx(List(Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now)))

    assertEquals(CommentaryIdeaSurface.statement(0, hanging, quiet), None)
    assertEquals(CommentaryIdeaSurface.statement(0, target, quiet), None)
    assert(
      CommentaryIdeaSurface
        .statement(0, forcing.facts.head, forcing)
        .exists(text => text.toLowerCase.contains("hanging") || text.toLowerCase.contains("underdefended")),
      clue(forcing.facts.head)
    )
    assertEquals(CommentaryIdeaSurface.tags(hanging), List("hanging_piece"))
    assertEquals(CommentaryIdeaSurface.tags(target), List("direct_threat"))
  }

  test("motif corroboration is owned by the same idea surface as fact priority") {
    assert(CommentaryIdeaSurface.motifCorroboratedByFact("pin_xray_pressure", Fact.Pin(Square.B4, Bishop, Square.C3, Knight, Square.E1, _root_.chess.King, true, FactScope.Now)))
    assert(CommentaryIdeaSurface.motifCorroboratedByFact("fork_deflection", Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook), FactScope.Now)))
    assert(CommentaryIdeaSurface.motifCorroboratedByFact("outpost_maneuver", Fact.Outpost(Square.D5, Knight, FactScope.Now)))
    assert(!CommentaryIdeaSurface.motifCorroboratedByFact("opposition", Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook), FactScope.Now)))
  }

  test("surface fact eligibility helpers centralize outline selection policy") {
    val fork = Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook), FactScope.Now)
    val pin = Fact.Pin(Square.B4, Bishop, Square.C3, Knight, Square.E1, _root_.chess.King, true, FactScope.Now)
    val skewer = Fact.Skewer(Square.B5, Bishop, Square.E8, _root_.chess.King, Square.A8, Rook, FactScope.Now)
    val hanging = Fact.HangingPiece(Square.E5, Pawn, List(Square.E4), Nil, FactScope.Now)
    val target = Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.Now)
    val doubleCheck = Fact.DoubleCheck(List(Square.F7), FactScope.Now)
    val activates = Fact.ActivatesPiece(Bishop, Square.F1, Square.C4, openedRay = false, FactScope.Now)
    val weak = Fact.WeakSquare(Square.D5, Color.Black, "no pawn defense", FactScope.Now)

    assert(List(fork, pin, skewer, hanging).forall(CommentaryIdeaSurface.isTacticalProofFact))
    assert(!List(target, doubleCheck, activates, weak).exists(CommentaryIdeaSurface.isTacticalProofFact))
    assert(!List(target, doubleCheck, activates).exists(CommentaryIdeaSurface.isKeyFactEligible))
    assert(List(fork, pin, skewer, hanging, weak).forall(CommentaryIdeaSurface.isKeyFactEligible))
  }

  test("canonical fact IDs stay independent from player-facing surface tags") {
    assertEquals(
      CommentaryIdeaSurface.canonicalFactId(Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.Now)),
      Some("target_piece")
    )
    assertEquals(CommentaryIdeaSurface.tags(Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.Now)), List("direct_threat"))
    assertEquals(CommentaryIdeaSurface.canonicalFactId(Fact.DoubleCheck(List(Square.F7), FactScope.Now)), Some("double_check"))
    assertEquals(CommentaryIdeaSurface.canonicalFactId(Fact.ActivatesPiece(Bishop, Square.F1, Square.C4, openedRay = false, FactScope.Now)), None)
  }

  test("opening label alone is not a descriptor but grounded opening goal is") {
    val current =
      played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, _root_.chess.Bishop))
    val labelOnly = evidence(openingName = Some("Italian Game"))
    val grounded =
      evidence(
        openingName = Some("Italian Game"),
        openingGoal = Some(
          OpeningGoals.Evaluation(
            goalName = "Development Logic",
            status = OpeningGoals.Status.Achieved,
            supportedEvidence = List("Minor piece developed"),
            missingEvidence = Nil,
            confidence = 0.86
          )
        )
      )

    assertEquals(CommentaryIdeaSurface.describe(current, labelOnly, None), None)
    val descriptor = CommentaryIdeaSurface.describe(current, grounded, None).getOrElse(fail("expected opening descriptor"))

    assertEquals(descriptor.source, "opening_goal", clue(descriptor))
    assertEquals(descriptor.reviewIntent, "normal_development", clue(descriptor))
    assertEquals(descriptor.moveCharacterBand, CommentaryIdeaSurface.MoveCharacterBand.Neutral, clue(descriptor))
    assert(descriptor.movePurpose.contains("Italian Game"), clue(descriptor.movePurpose))
    assertEquals(descriptor.requiresPvForAdmission, false, clue(descriptor))
    assert(descriptor.reasonTags.contains("opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("review_intent:normal_development"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("character_band:neutral"), clue(descriptor.reasonTags))
    assert(descriptor.baseProse.contains("Italian Game"), clue(descriptor.baseProse))
  }

  test("descriptor rule order keeps grounded opening before tactical fallback") {
    val current =
      played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, _root_.chess.Bishop))
    val fork =
      Fact.Fork(Square.C4, Bishop, List(Square.F7 -> Pawn), FactScope.CandidatePv)
    val grounded =
      evidence(
        facts = List(fork),
        openingName = Some("Italian Game"),
        openingGoal = Some(
          OpeningGoals.Evaluation(
            goalName = "Development Logic",
            status = OpeningGoals.Status.Achieved,
            supportedEvidence = List("Minor piece developed"),
            missingEvidence = Nil,
            confidence = 0.86
          )
        )
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          current,
          grounded,
          Some(lineFacts(moveRef("m1", "Bc4", "f1c4", 5), Some(moveRef("m2", "Nf6", "g8f6", 6))))
        )
        .getOrElse(fail("expected opening descriptor"))

    assertEquals(descriptor.source, "opening_goal", clue(descriptor))
    assertEquals(descriptor.ideaKind, "opening_goal", clue(descriptor))
    assertEquals(descriptor.reviewIntent, "normal_development", clue(descriptor))
    assert(descriptor.confirms.contains("fork"), clue(descriptor.confirms))
  }

  test("PV-backed tactical direct-threat and endgame descriptors keep current admission gates") {
    val fork =
      Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.CandidatePv)
    val target =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val kingActivity =
      Fact.KingActivity(Square.D2, mobility = 5, proximityToCenter = 1, FactScope.CandidatePv)

    val tactical =
      CommentaryIdeaSurface
        .describe(
          played("d4f5", "Nf5", Square.D4, Square.F5, Piece(Color.White, Knight)),
          evidence(facts = List(fork)),
          Some(lineFacts(moveRef("m1", "Nf5", "d4f5", 1), Some(moveRef("m2", "Qg5", "h4g5", 2))))
        )
        .getOrElse(fail("expected tactical descriptor"))
    val directThreatNoPv =
      CommentaryIdeaSurface.describe(
        played("d1h5", "Qh5", Square.D1, Square.H5, Piece(Color.White, Queen)),
        evidence(facts = List(target)),
        None
      )
    val endgame =
      CommentaryIdeaSurface
        .describe(
          played("e1d2", "Kd2", Square.E1, Square.D2, Piece(Color.White, _root_.chess.King)),
          evidence(facts = List(kingActivity)),
          Some(lineFacts(moveRef("m1", "Kd2", "e1d2", 60), Some(moveRef("m2", "Kg2", "h1g2", 61))))
        )
        .getOrElse(fail("expected endgame descriptor"))

    assertEquals(tactical.linePurpose, Some("create_tactical_threat"), clue(tactical))
    assertEquals(tactical.reviewIntent, "creates_threat", clue(tactical))
    assert(tactical.opponentQuestion.exists(_.contains("Qg5")), clue(tactical.opponentQuestion))
    assert(tactical.lineResolution.exists(_.toLowerCase.contains("tactical")), clue(tactical.lineResolution))
    assertEquals(tactical.ideaKind, "fork", clue(tactical))
    assertEquals(tactical.source, "canonical_fact", clue(tactical))
    assert(tactical.reasonTags.contains("fork"), clue(tactical.reasonTags))
    assert(tactical.reasonTags.contains("review_intent:creates_threat"), clue(tactical.reasonTags))
    assert(tactical.confirms.contains("fork"), clue(tactical.confirms))
    assert(tactical.confirms.contains("creates_threat"), clue(tactical.confirms))
    assert(tactical.title.contains("fork"), clue(tactical.title))
    assert(tactical.learningPoint.exists(_.toLowerCase.contains("tactical")), clue(tactical.learningPoint))
    assertEquals(directThreatNoPv, None)
    val directThreat =
      CommentaryIdeaSurface
        .describe(
          played("d1h5", "Qh5", Square.D1, Square.H5, Piece(Color.White, Queen)),
          evidence(facts = List(target)),
          Some(lineFacts(moveRef("m1", "Qh5", "d1h5", 3), Some(moveRef("m2", "g6", "g7g6", 4))))
        )
        .getOrElse(fail("expected direct-threat descriptor"))
    assertEquals(directThreat.linePurpose, Some("answer_direct_threat"), clue(directThreat))
    assertEquals(directThreat.reviewIntent, "answers_threat", clue(directThreat))
    assert(directThreat.opponentQuestion.exists(_.contains("g6")), clue(directThreat.opponentQuestion))
    assert(directThreat.confirms.contains("direct_threat"), clue(directThreat.confirms))
    assert(directThreat.confirms.contains("answers_threat"), clue(directThreat.confirms))
    assert(directThreat.learningPoint.exists(_.contains("g6")), clue(directThreat.learningPoint))
    assertEquals(endgame.linePurpose, Some("improve_endgame_activity"), clue(endgame))
    assertEquals(endgame.reviewIntent, "improves_endgame_activity", clue(endgame))
    assert(endgame.reasonTags.contains("king_activity"), clue(endgame.reasonTags))
    assertEquals(
      CommentaryIdeaSurface.describe(
        played("e1d2", "Kd2", Square.E1, Square.D2, Piece(Color.White, _root_.chess.King)),
        evidence(),
        Some(lineFacts(moveRef("m1", "Kd2", "e1d2", 60), Some(moveRef("m2", "Kg2", "h1g2", 61))))
      ),
      None,
      clue("phase-only endgame evidence should stay closed")
    )
  }

  test("capture descriptors stay PV-backed and owned by the idea surface") {
    val pawnCapture =
      CommentaryIdeaSurface
        .describe(
          played("e4d5", "exd5", Square.E4, Square.D5, Piece(Color.White, Pawn), Some(Pawn)),
          evidence(motifs =
            List(Motif.Capture(Pawn, Pawn, Square.D5, Motif.CaptureType.Normal, Color.White, 12, Some("exd5")))
          ),
          Some(lineFacts(moveRef("m1", "exd5", "e4d5", 12), Some(moveRef("m2", "Qxd5", "d8d5", 13))))
        )
        .getOrElse(fail("expected capture tension descriptor"))
    val pieceCapture =
      CommentaryIdeaSurface
        .describe(
          played("c4f7", "Bxf7+", Square.C4, Square.F7, Piece(Color.White, Bishop), Some(Pawn)),
          evidence(motifs =
            List(Motif.Capture(Bishop, Pawn, Square.F7, Motif.CaptureType.Exchange, Color.White, 16, Some("Bxf7+")))
          ),
          Some(lineFacts(moveRef("m1", "Bxf7+", "c4f7", 16), Some(moveRef("m2", "Kxf7", "e8f7", 17))))
        )
        .getOrElse(fail("expected exchange clarification descriptor"))
    val noReply =
      CommentaryIdeaSurface.describe(
        played("e4d5", "exd5", Square.E4, Square.D5, Piece(Color.White, Pawn), Some(Pawn)),
        evidence(motifs =
          List(Motif.Capture(Pawn, Pawn, Square.D5, Motif.CaptureType.Normal, Color.White, 12, Some("exd5")))
        ),
        Some(lineFacts(moveRef("m1", "exd5", "e4d5", 12), None))
      )

    assertEquals(pawnCapture.ideaKind, "capture_tension", clue(pawnCapture))
    assertEquals(pawnCapture.reviewIntent, "keeps_tension", clue(pawnCapture))
    assertEquals(pawnCapture.linePurpose, Some("resolve_capture_tension"), clue(pawnCapture))
    assert(pawnCapture.confirms.contains("capture_tension"), clue(pawnCapture.confirms))
    assert(pawnCapture.confirms.contains("keeps_tension"), clue(pawnCapture.confirms))
    assertEquals(pieceCapture.ideaKind, "exchange_clarified", clue(pieceCapture))
    assertEquals(pieceCapture.reviewIntent, "clarifies_exchange", clue(pieceCapture))
    assertEquals(pieceCapture.linePurpose, Some("clarify_exchange"), clue(pieceCapture))
    assert(pieceCapture.confirms.contains("exchange_clarified"), clue(pieceCapture.confirms))
    assert(pieceCapture.confirms.contains("clarifies_exchange"), clue(pieceCapture.confirms))
    assertEquals(noReply, None)
  }

  test("castling descriptor stays local king-safety surface without requiring PV") {
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          played("e1g1", "O-O", Square.E1, Square.G1, Piece(Color.White, _root_.chess.King)),
          evidence(),
          None
        )
        .getOrElse(fail("expected castling descriptor"))

    assertEquals(descriptor.ideaKind, "king_safety", clue(descriptor))
    assertEquals(descriptor.reviewIntent, "king_safety", clue(descriptor))
    assertEquals(descriptor.requiresPvForAdmission, false, clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("king_safety_first"), clue(descriptor))
    assert(descriptor.reasonTags.contains("king_safety"), clue(descriptor.reasonTags))
  }

  test("truth contract only derives an internal character band") {
    val badContract =
      truthContract(DecisiveTruthClass.Blunder, DecisiveReasonKind.TacticalRefutation)
    val necessaryContract =
      truthContract(
        DecisiveTruthClass.Best,
        DecisiveReasonKind.OnlyMoveDefense,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        benchmarkCriticalMove = true
      )
    val current =
      played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, _root_.chess.Bishop))
    val grounded =
      evidence(
        openingName = Some("Italian Game"),
        openingGoal = Some(
          OpeningGoals.Evaluation(
            goalName = "Development Logic",
            status = OpeningGoals.Status.Achieved,
            supportedEvidence = List("Minor piece developed"),
            missingEvidence = Nil,
            confidence = 0.86
          )
        )
      )

    val bad =
      CommentaryIdeaSurface
        .describe(current, grounded, None, truthContract = Some(badContract))
        .getOrElse(fail("expected bad-band descriptor"))
    val necessary =
      CommentaryIdeaSurface
        .describe(current, grounded, None, truthContract = Some(necessaryContract))
        .getOrElse(fail("expected necessary-band descriptor"))

    assertEquals(bad.moveCharacterBand, CommentaryIdeaSurface.MoveCharacterBand.Bad, clue(bad))
    assert(bad.reasonTags.contains("character_band:bad"), clue(bad.reasonTags))
    assertEquals(necessary.moveCharacterBand, CommentaryIdeaSurface.MoveCharacterBand.Necessary, clue(necessary))
    assert(necessary.reasonTags.contains("character_band:necessary"), clue(necessary.reasonTags))
  }

  private def truthContract(
      truthClass: DecisiveTruthClass,
      reasonFamily: DecisiveReasonKind,
      visibilityRole: TruthVisibilityRole = TruthVisibilityRole.Hidden,
      benchmarkCriticalMove: Boolean = false
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("f1c4"),
      verifiedBestMove = Some("f1c4"),
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = visibilityRole,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = benchmarkCriticalMove,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )
