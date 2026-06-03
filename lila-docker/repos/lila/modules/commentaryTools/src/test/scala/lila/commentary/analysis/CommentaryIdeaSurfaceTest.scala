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
      openingName: Option[String] = None,
      strategicDelta: Option[PlayerFacingMoveDeltaEvidence] = None
  ): CommentaryIdeaSurface.MoveReviewEvidence =
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = openingName,
      strategicDelta = strategicDelta
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

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = lineId,
          scoreCp = 12,
          mate = None,
          depth = 16,
          moves =
            ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
              val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
              MoveReviewMoveRef(
                refId = s"${lineId}_m${idx + 1}",
                san = san,
                uci = uci,
                fenAfter = fens(idx),
                ply = ply,
                moveNo = (ply + 1) / 2,
                marker = Some(if ply % 2 == 1 then s"${(ply + 1) / 2}." else s"${(ply + 1) / 2}...")
              )
            }
        )
      )
    )

  private def exactLineFacts(
      startFen: String,
      playedUci: String,
      ucis: List[String],
      sans: List[String],
      lineId: String
  ): MoveReviewPvLine.LineFacts =
    MoveReviewPvLine
      .firstCoupled(startFen, playedUci, Some(refsForLine(startFen, ucis, sans, lineId)))
      .getOrElse(fail(s"expected legal coupled PV for $playedUci from $startFen"))

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
    assert(CommentaryIdeaSurface.motifCorroboratedByFact("battery", Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now)))
    assert(!CommentaryIdeaSurface.motifCorroboratedByFact("trapped_piece", Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now)))
    assert(!CommentaryIdeaSurface.motifCorroboratedByFact("trapped_piece_queen", Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now)))
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

  test("opening label and grounded goal both require PV proof for a descriptor") {
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
    assertEquals(CommentaryIdeaSurface.describe(current, grounded, None), None)
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          current,
          grounded,
          Some(lineFacts(moveRef("m1", "Bc4", "f1c4", 5), Some(moveRef("m2", "Nf6", "g8f6", 6)), Some(moveRef("m3", "d3", "d2d3", 7))))
        )
        .getOrElse(fail("expected PV-proved opening descriptor"))

    assertEquals(descriptor.source, "opening_goal", clue(descriptor))
    assertEquals(descriptor.reviewIntent, "normal_development", clue(descriptor))
    assertEquals(descriptor.moveCharacterBand, CommentaryIdeaSurface.MoveCharacterBand.Neutral, clue(descriptor))
    assert(descriptor.movePurpose.contains("Italian Game"), clue(descriptor.movePurpose))
    assertEquals(descriptor.requiresPvForAdmission, true, clue(descriptor))
    assert(descriptor.reasonTags.contains("opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("review_intent:normal_development"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("character_band:neutral"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("line_proof:opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("line_subject:f1c4"), clue(descriptor.reasonTags))
    assert(descriptor.baseProse.contains("Italian Game"), clue(descriptor.baseProse))
    assert(descriptor.title.contains("development idea"), clue(descriptor.title))
    assert(!descriptor.title.contains("Development Logic"), clue(descriptor.title))
    assert(!descriptor.baseProse.contains("Development Logic"), clue(descriptor.baseProse))
  }

  test("normal development descriptors are pinned to exact legal opening PVs") {
    val italianFen =
      "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val ruyFen = italianFen
    val qgFen =
      "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"
    val goal =
      OpeningGoals.Evaluation(
        goalName = "Development Logic",
        status = OpeningGoals.Status.Achieved,
        supportedEvidence = List("minor-piece development"),
        missingEvidence = Nil,
        confidence = 0.86
      )
    val centerGoal =
      OpeningGoals.Evaluation(
        goalName = "Center Challenge",
        status = OpeningGoals.Status.Achieved,
        supportedEvidence = List("queen-pawn tension"),
        missingEvidence = Nil,
        confidence = 0.82
      )

    val italian =
      CommentaryIdeaSurface
        .describe(
          played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, Bishop)),
          evidence(openingGoal = Some(goal), openingName = Some("Italian Game")),
          Some(exactLineFacts(italianFen, "f1c4", List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"), "italian"))
        )
        .getOrElse(fail("expected exact Italian development descriptor"))
    val ruy =
      CommentaryIdeaSurface
        .describe(
          played("f1b5", "Bb5", Square.F1, Square.B5, Piece(Color.White, Bishop)),
          evidence(openingGoal = Some(goal), openingName = Some("Ruy Lopez")),
          Some(exactLineFacts(ruyFen, "f1b5", List("f1b5", "a7a6", "b5a4"), List("Bb5", "a6", "Ba4"), "ruy"))
        )
        .getOrElse(fail("expected exact Ruy Lopez development descriptor"))
    val qg =
      CommentaryIdeaSurface
        .describe(
          played("c2c4", "c4", Square.C2, Square.C4, Piece(Color.White, Pawn)),
          evidence(openingGoal = Some(centerGoal), openingName = Some("Queen's Gambit")),
          Some(exactLineFacts(qgFen, "c2c4", List("c2c4", "e7e6", "b1c3"), List("c4", "e6", "Nc3"), "qg"))
        )
        .getOrElse(fail("expected exact Queen's Gambit development descriptor"))

    assertEquals(italian.reviewIntent, "normal_development", clue(italian))
    assertEquals(italian.opponentReplyMeaning, Some("attacks_center_pawn"), clue(italian))
    assert(italian.learningPoint.exists(_.contains("d3")), clue(italian.learningPoint))
    assertEquals(ruy.reviewIntent, "normal_development", clue(ruy))
    assertEquals(ruy.opponentReplyMeaning, Some("asks_piece_commitment"), clue(ruy))
    assert(ruy.learningPoint.exists(_.contains("Ba4")), clue(ruy.learningPoint))
    assertEquals(qg.reviewIntent, "normal_development", clue(qg))
    assertEquals(qg.linePurpose, Some("challenge_center"), clue(qg))
    assert(qg.confirms.contains("opening_goal"), clue(qg.confirms))
    assert(!italian.title.contains("Development Logic"), clue(italian.title))
    assert(!italian.baseProse.contains("Development Logic"), clue(italian.baseProse))
    assert(!qg.title.contains("Center Challenge"), clue(qg.title))
    assert(!qg.baseProse.contains("Center Challenge"), clue(qg.baseProse))
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
          evidence(
            facts = List(fork),
            motifs = List(Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 0, Some("Nf5")))
          ),
          Some(lineFacts(moveRef("m1", "Nf5", "d4f5", 1), Some(moveRef("m2", "Qg5", "h4g5", 2))))
        )
        .getOrElse(fail("expected tactical descriptor"))
    val leakedTactical =
      CommentaryIdeaSurface.describe(
        played("d4f5", "Nf5", Square.D4, Square.F5, Piece(Color.White, Knight)),
        evidence(
          facts = List(fork),
          motifs = List(Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 1, Some("Nf5")))
        ),
        Some(lineFacts(moveRef("m1", "Nf5", "d4f5", 1), Some(moveRef("m2", "Qg5", "h4g5", 2))))
      )
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
    assertEquals(leakedTactical, None, clue("ambient tactical fact without ply-0 current-move motif should stay closed"))
    assertEquals(directThreatNoPv, None)
    val directThreat =
      CommentaryIdeaSurface
        .describe(
          played("d1h5", "Qh5", Square.D1, Square.H5, Piece(Color.White, Queen)),
          evidence(facts = List(target)),
          Some(lineFacts(moveRef("m1", "Qh5", "d1h5", 3), Some(moveRef("m2", "g6", "g7g6", 4))))
        )
        .getOrElse(fail("expected direct-threat descriptor"))
    assertEquals(directThreat.linePurpose, Some("create_tactical_threat"), clue(directThreat))
    assertEquals(directThreat.reviewIntent, "creates_threat", clue(directThreat))
    assert(directThreat.opponentQuestion.exists(_.contains("g6")), clue(directThreat.opponentQuestion))
    assert(directThreat.confirms.contains("direct_threat"), clue(directThreat.confirms))
    assert(directThreat.confirms.contains("creates_threat"), clue(directThreat.confirms))
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

  test("pin and skewer intent fixtures use exact legal PV proof") {
    val pinFen =
      "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1"
    val skewerFen =
      "r3k3/8/8/8/8/8/8/4K2Q b - - 0 1"
    val pinFact =
      Fact.Pin(Square.B4, Bishop, Square.C3, Knight, Square.E1, _root_.chess.King, isAbsolute = true, FactScope.CandidatePv)
    val skewerFact =
      Fact.Skewer(Square.A1, Rook, Square.E1, _root_.chess.King, Square.H1, Queen, FactScope.CandidatePv)

    val pin =
      CommentaryIdeaSurface
        .describe(
          played("f8b4", "Bb4", Square.F8, Square.B4, Piece(Color.Black, Bishop)),
          evidence(
            facts = List(pinFact),
            motifs = List(
              Motif.Pin(
                pinningPiece = Bishop,
                pinnedPiece = Knight,
                targetBehind = _root_.chess.King,
                color = Color.Black,
                plyIndex = 0,
                move = Some("Bb4"),
                pinningSq = Some(Square.B4),
                pinnedSq = Some(Square.C3),
                behindSq = Some(Square.E1)
              )
            )
          ),
          Some(exactLineFacts(pinFen, "f8b4", List("f8b4", "e1f1", "b4c3"), List("Bb4", "Kf1", "Bxc3"), "pin"))
        )
        .getOrElse(fail("expected exact pin descriptor"))
    val skewer =
      CommentaryIdeaSurface
        .describe(
          played("a8a1", "Ra1+", Square.A8, Square.A1, Piece(Color.Black, Rook)),
          evidence(
            facts = List(skewerFact),
            motifs = List(
              Motif.Skewer(
                attackingPiece = Rook,
                frontPiece = _root_.chess.King,
                backPiece = Queen,
                color = Color.Black,
                plyIndex = 0,
                move = Some("Ra1+"),
                attackingSq = Some(Square.A1),
                frontSq = Some(Square.E1),
                backSq = Some(Square.H1)
              )
            )
          ),
          Some(exactLineFacts(skewerFen, "a8a1", List("a8a1", "e1f2", "a1h1"), List("Ra1+", "Kf2", "Rxh1"), "skewer"))
        )
        .getOrElse(fail("expected exact skewer descriptor"))
    val leakedPin =
      CommentaryIdeaSurface.describe(
        played("f8b4", "Bb4", Square.F8, Square.B4, Piece(Color.Black, Bishop)),
        evidence(facts = List(pinFact)),
        Some(exactLineFacts(pinFen, "f8b4", List("f8b4", "e1f1", "b4c3"), List("Bb4", "Kf1", "Bxc3"), "pin_no_motif"))
      )

    assertEquals(pin.reviewIntent, "creates_threat", clue(pin))
    assertEquals(pin.ideaKind, "pin", clue(pin))
    assertEquals(pin.linePurpose, Some("create_tactical_threat"), clue(pin))
    assert(pin.confirms.contains("pin"), clue(pin.confirms))
    assert(pin.reasonTags.contains("line_proof:tactical_threat"), clue(pin.reasonTags))
    assertEquals(skewer.reviewIntent, "creates_threat", clue(skewer))
    assertEquals(skewer.ideaKind, "skewer", clue(skewer))
    assertEquals(skewer.linePurpose, Some("create_tactical_threat"), clue(skewer))
    assert(skewer.confirms.contains("skewer"), clue(skewer.confirms))
    assertEquals(leakedPin, None, clue("pin fact without current-move motif ownership should stay closed"))
  }

  test("endgame activity descriptors cover exact passed-pawn and rook-activity PVs") {
    val passedPawnFen =
      "8/4k3/8/8/8/8/4P3/4K3 w - - 0 1"
    val rookFen =
      "4k3/8/8/8/8/8/R3P3/4K3 w - - 0 1"
    val passedPawn =
      CommentaryIdeaSurface
        .describe(
          played("e2e4", "e4", Square.E2, Square.E4, Piece(Color.White, Pawn)),
          evidence(facts = List(Fact.PawnPromotion(Square.E4, promotedTo = None, FactScope.CandidatePv))),
          Some(exactLineFacts(passedPawnFen, "e2e4", List("e2e4", "e7d6", "e1d2"), List("e4", "Kd6", "Kd2"), "passed_pawn"))
        )
        .getOrElse(fail("expected exact passed-pawn activity descriptor"))
    val rookActivity =
      CommentaryIdeaSurface
        .describe(
          played("a2a7", "Ra7", Square.A2, Square.A7, Piece(Color.White, Rook)),
          evidence(facts = List(Fact.RookEndgamePattern("RookBehindPassedPawn", FactScope.CandidatePv))),
          Some(exactLineFacts(rookFen, "a2a7", List("a2a7", "e8d8", "a7a8"), List("Ra7", "Kd8", "Ra8"), "rook_activity"))
        )
        .getOrElse(fail("expected exact rook-activity descriptor"))

    assertEquals(passedPawn.reviewIntent, "improves_endgame_activity", clue(passedPawn))
    assertEquals(passedPawn.linePurpose, Some("improve_endgame_activity"), clue(passedPawn))
    assert(passedPawn.reasonTags.contains("pawn_promotion"), clue(passedPawn.reasonTags))
    assert(passedPawn.confirms.contains("endgame_activity"), clue(passedPawn.confirms))
    assertEquals(rookActivity.reviewIntent, "improves_endgame_activity", clue(rookActivity))
    assert(rookActivity.reasonTags.contains("rook_endgame_pattern"), clue(rookActivity.reasonTags))
    assert(rookActivity.learningPoint.exists(_.contains("Ra8")), clue(rookActivity.learningPoint))
  }

  test("capture descriptors stay PV-backed and owned by the idea surface") {
    val pawnCaptureFen =
      "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
    val bishopCaptureFen =
      "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val pawnCapture =
      CommentaryIdeaSurface
        .describe(
          played("e4d5", "exd5", Square.E4, Square.D5, Piece(Color.White, Pawn), Some(Pawn)),
          evidence(motifs =
            List(Motif.Capture(Pawn, Pawn, Square.D5, Motif.CaptureType.Normal, Color.White, 12, Some("exd5")))
          ),
          Some(exactLineFacts(pawnCaptureFen, "e4d5", List("e4d5", "d8d5", "b1c3"), List("exd5", "Qxd5", "Nc3"), "pawn_capture"))
        )
        .getOrElse(fail("expected capture tension descriptor"))
    val pieceCapture =
      CommentaryIdeaSurface
        .describe(
          played("c4f7", "Bxf7+", Square.C4, Square.F7, Piece(Color.White, Bishop), Some(Pawn)),
          evidence(motifs =
            List(Motif.Capture(Bishop, Pawn, Square.F7, Motif.CaptureType.Exchange, Color.White, 16, Some("Bxf7+")))
          ),
          Some(exactLineFacts(bishopCaptureFen, "c4f7", List("c4f7", "e8f7", "e1g1"), List("Bxf7+", "Kxf7", "O-O"), "bishop_capture"))
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

  test("defensive intent requires defense truth and PV proof instead of target creation") {
    val current =
      played("g1f3", "Nf3", Square.G1, Square.F3, Piece(Color.White, Knight))
    val target =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val line =
      Some(lineFacts(moveRef("m1", "Nf3", "g1f3", 3), Some(moveRef("m2", "Nc6", "b8c6", 4)), Some(moveRef("m3", "d4", "d2d4", 5))))
    val defensiveContract =
      truthContract(
        DecisiveTruthClass.Best,
        DecisiveReasonKind.OnlyMoveDefense,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        benchmarkCriticalMove = true
      )
    val targetCreation =
      CommentaryIdeaSurface
        .describe(current, evidence(facts = List(target)), line)
        .getOrElse(fail("expected target creation descriptor"))
    val defensive =
      CommentaryIdeaSurface
        .describe(current, evidence(facts = List(target)), line, truthContract = Some(defensiveContract))
        .getOrElse(fail("expected defensive descriptor"))

    assertEquals(targetCreation.reviewIntent, "creates_threat", clue(targetCreation))
    assertEquals(defensive.reviewIntent, "answers_threat", clue(defensive))
    assertEquals(defensive.linePurpose, Some("answer_direct_threat"), clue(defensive))
    assert(defensive.reasonTags.contains("line_proof:defensive_answer"), clue(defensive.reasonTags))
    assert(defensive.confirms.contains("answers_threat"), clue(defensive.confirms))
  }

  test("castling descriptor requires PV proof for king-safety surface") {
    val castleFen =
      "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val noPv =
      CommentaryIdeaSurface.describe(
        played("e1g1", "O-O", Square.E1, Square.G1, Piece(Color.White, _root_.chess.King)),
        evidence(),
        None
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          played("e1g1", "O-O", Square.E1, Square.G1, Piece(Color.White, _root_.chess.King)),
          evidence(),
          Some(exactLineFacts(castleFen, "e1g1", List("e1g1", "f8e7", "f1e1"), List("O-O", "Be7", "Re1"), "castle"))
        )
        .getOrElse(fail("expected PV-proved castling descriptor"))

    assertEquals(noPv, None)
    assertEquals(descriptor.ideaKind, "king_safety", clue(descriptor))
    assertEquals(descriptor.reviewIntent, "king_safety", clue(descriptor))
    assertEquals(descriptor.requiresPvForAdmission, true, clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("king_safety_first"), clue(descriptor))
    assert(descriptor.reasonTags.contains("king_safety"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("line_proof:king_safety"), clue(descriptor.reasonTags))
  }

  test("certified strategic support needs packet proof and PV proof") {
    val startFen =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val current =
      played("h2h3", "h3", Square.H2, Square.H3, Piece(Color.White, Pawn))
    val line =
      Some(exactLineFacts(startFen, "h2h3", List("h2h3", "b7b5", "a2a4"), List("h3", "b5", "a4"), "strategy"))
    val certified =
      strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.CounterplayReduction)
    val packetOnly =
      CommentaryIdeaSurface.describe(current, evidence(strategicDelta = Some(certified)), None)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDelta = Some(certified)), line)
        .getOrElse(fail("expected certified strategic support descriptor"))
    val risky =
      certified.copy(packet = certified.packet.copy(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease)))

    assertEquals(packetOnly, None)
    assertEquals(
      CommentaryIdeaSurface.describe(current, evidence(strategicDelta = Some(risky)), line),
      None
    )
    assertEquals(descriptor.reviewIntent, "prevents_counterplay", clue(descriptor))
    assertEquals(descriptor.linePurpose, Some("prevent_counterplay"), clue(descriptor))
    assertEquals(descriptor.source, "certified_strategy_support", clue(descriptor))
    assert(descriptor.baseProse.contains("h3 keeps counterplay restrained around b5."), clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("certified"), clue(descriptor.baseProse))
    assert(descriptor.reasonTags.contains("line_proof:certified_strategy"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("proof_family:neutralize_key_break"), clue(descriptor.reasonTags))
  }

  test("certified strategic support prose maps internal proof labels to player-facing chess language") {
    val startFen =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val current =
      played("h2h3", "h3", Square.H2, Square.H3, Piece(Color.White, Pawn))
    val line =
      Some(exactLineFacts(startFen, "h2h3", List("h2h3", "b7b5", "a2a4"), List("h3", "b5", "a4"), "strategy"))
    val exchange =
      CommentaryIdeaSurface
        .describe(
          current,
          evidence(strategicDelta = Some(strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.ExchangeForcing))),
          line
        )
        .getOrElse(fail("expected exchange descriptor"))
    val plan =
      CommentaryIdeaSurface
        .describe(
          current,
          evidence(strategicDelta = Some(strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.PlanAdvance))),
          line
        )
        .getOrElse(fail("expected plan descriptor"))

    assert(exchange.baseProse.contains("h3 clarifies the exchange around b5."), clue(exchange.baseProse))
    assert(plan.baseProse.contains("h3 supports the plan around b5."), clue(plan.baseProse))
    assert(!exchange.baseProse.contains("certified"), clue(exchange.baseProse))
    assert(!plan.baseProse.contains("certified"), clue(plan.baseProse))
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

    val pv =
      Some(lineFacts(moveRef("m1", "Bc4", "f1c4", 5), Some(moveRef("m2", "Nf6", "g8f6", 6)), Some(moveRef("m3", "d3", "d2d3", 7))))
    val bad =
      CommentaryIdeaSurface
        .describe(current, grounded, pv, truthContract = Some(badContract))
        .getOrElse(fail("expected bad-band descriptor"))
    val necessary =
      CommentaryIdeaSurface
        .describe(current, grounded, pv, truthContract = Some(necessaryContract))
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

  private def strategicDelta(
      proofFamily: String,
      proofSource: String,
      deltaClass: PlayerFacingMoveDeltaClass
  ): PlayerFacingMoveDeltaEvidence =
    val packet =
      PlayerFacingClaimPacket(
        claimGate = PlanEvidenceEvaluator.ClaimCertification(
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          certificateStatus = PlayerFacingCertificateStatus.Valid
        ),
        proofSource = proofSource,
        proofFamily = proofFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        triggerKind = "test",
        anchorTerms = List("b5"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms = List("b5"),
          continuationTerms = List("a4"),
          structureTransitionTerms = List("b5"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("b5"))
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    PlayerFacingMoveDeltaEvidence(
      deltaClass = deltaClass,
      anchorTerms = List("b5"),
      quantifier = PlayerFacingClaimQuantifier.BestResponse,
      modalityTier = PlayerFacingClaimModalityTier.Supports,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
      provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
      certificateStatus = PlayerFacingCertificateStatus.Valid,
      ontologyFamily = PlayerFacingClaimOntologyKind.CounterplayRestraint,
      packet = packet
    )
