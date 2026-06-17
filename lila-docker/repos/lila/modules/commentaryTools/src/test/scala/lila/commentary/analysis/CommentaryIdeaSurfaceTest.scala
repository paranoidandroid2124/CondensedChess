package lila.commentary.analysis

import _root_.chess.{ Bishop, Color, Knight, Pawn, Piece, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
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
      capturedRole: Option[_root_.chess.Role] = None,
      afterFen: String = "after"
  ): CommentaryIdeaSurface.PlayedMove =
    CommentaryIdeaSurface.PlayedMove(
      uci = uci,
      san = san,
      from = from,
      to = to,
      piece = piece,
      afterFen = afterFen,
      capturedRole = capturedRole
    )

  private def evidence(
      facts: List[Fact] = Nil,
      motifs: List[Motif] = Nil,
      openingGoal: Option[OpeningGoals.Evaluation] = None,
      openingName: Option[String] = None,
      strategicDeltas: List[PlayerFacingMoveDeltaEvidence] = Nil,
      practicalPositionFacts: List[CommentaryIdeaSurface.PracticalPositionFact] = Nil
  ): CommentaryIdeaSurface.MoveReviewEvidence =
    CommentaryIdeaSurface.MoveReviewEvidence(
      facts = facts,
      motifs = motifs,
      openingGoal = openingGoal,
      openingName = openingName,
      strategicDeltas = strategicDeltas,
      practicalPositionFacts = practicalPositionFacts
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

  test("fact surface wording does not promote weak-square or endgame anchors into stronger families") {
    val weak = Fact.WeakSquare(Square.D5, Color.Black, "no pawn defense", FactScope.Now)
    val outpost = Fact.Outpost(Square.D5, Knight, FactScope.Now)
    val opposition = Fact.Opposition(Square.D4, Square.D6, distance = 2, isDirect = true, "Direct", FactScope.Now)
    val kingActivity = Fact.KingActivity(Square.D4, mobility = 5, proximityToCenter = 1, FactScope.Now)
    val zugzwang = Fact.Zugzwang(Color.Black, FactScope.Now)
    val ctx = forcingCtx(List(weak, outpost, opposition, kingActivity))

    val weakTexts = (0 to 2).flatMap(bead => CommentaryIdeaSurface.statement(bead, weak, ctx)).map(_.toLowerCase)
    val outpostTexts = (0 to 2).flatMap(bead => CommentaryIdeaSurface.statement(bead, outpost, ctx)).map(_.toLowerCase)
    val oppositionTexts = (0 to 2).flatMap(bead => CommentaryIdeaSurface.statement(bead, opposition, ctx)).map(_.toLowerCase)
    val kingActivityTexts = (0 to 2).flatMap(bead => CommentaryIdeaSurface.statement(bead, kingActivity, ctx)).map(_.toLowerCase)

    assert(weakTexts.nonEmpty, clue(weakTexts))
    assert(!weakTexts.exists(_.contains("outpost")), clue(weakTexts))
    assert(outpostTexts.forall(_.contains("outpost")), clue(outpostTexts))
    assert(!oppositionTexts.exists(text => text.contains("important") || text.contains("key factor")), clue(oppositionTexts))
    assert(!kingActivityTexts.exists(text => text.contains("well-placed") || text.contains("safe steps")), clue(kingActivityTexts))
    assertEquals(CommentaryIdeaSurface.tags(zugzwang), Nil)
    assertEquals(CommentaryIdeaSurface.canonicalFactId(zugzwang), None)
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
    assert(descriptor.movePurpose.contains("opening context"), clue(descriptor.movePurpose))
    assert(!descriptor.movePurpose.contains("Italian Game"), clue(descriptor.movePurpose))
    assertEquals(descriptor.requiresPvForAdmission, true, clue(descriptor))
    assert(descriptor.reasonTags.contains("opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("review_intent:normal_development"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("character_band:neutral"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("line_proof:opening_goal"), clue(descriptor.reasonTags))
    assert(descriptor.reasonTags.contains("line_subject:f1c4"), clue(descriptor.reasonTags))
    assert(descriptor.baseProse.contains("opening context"), clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("Italian Game"), clue(descriptor.baseProse))
    assert(descriptor.title.contains("develops the bishop toward opening coordination"), clue(descriptor.title))
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
    assert(italian.learningPoint.exists(_.contains("d3")), clue(italian.learningPoint))
    assertEquals(ruy.reviewIntent, "normal_development", clue(ruy))
    assert(ruy.learningPoint.exists(_.contains("Ba4")), clue(ruy.learningPoint))
    assertEquals(qg.reviewIntent, "normal_development", clue(qg))
    assertEquals(qg.linePurpose, Some("challenge_center"), clue(qg))
    assert(qg.confirms.contains("opening_goal"), clue(qg.confirms))
    assert(!italian.title.contains("Development Logic"), clue(italian.title))
    assert(!italian.baseProse.contains("Development Logic"), clue(italian.baseProse))
    assert(!qg.title.contains("Center Challenge"), clue(qg.title))
    assert(!qg.baseProse.contains("Center Challenge"), clue(qg.baseProse))
    assert(!italian.baseProse.contains("Italian Game"), clue(italian.baseProse))
    assert(!ruy.baseProse.contains("Ruy Lopez"), clue(ruy.baseProse))
    assert(!qg.baseProse.contains("Queen's Gambit"), clue(qg.baseProse))
    assert(!italian.baseProse.contains("minor-piece development"), clue(italian.baseProse))
    assert(!qg.baseProse.contains("queen-pawn tension"), clue(qg.baseProse))
  }

  test("opening goal surface drops raw unsupported evidence strings") {
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          played("f1c4", "Bc4", Square.F1, Square.C4, Piece(Color.White, Bishop)),
          evidence(
            openingGoal = Some(
              OpeningGoals.Evaluation(
                goalName = "Development Logic",
                status = OpeningGoals.Status.Achieved,
                supportedEvidence = List("shared carrier", "Minor piece developed"),
                missingEvidence = Nil,
                confidence = 0.86
              )
            ),
            openingName = Some("Italian Game")
          ),
          Some(exactLineFacts(
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "f1c4",
            List("f1c4", "g8f6", "d2d3"),
            List("Bc4", "Nf6", "d3"),
            "italian"
          ))
        )
        .getOrElse(fail("expected PV-proved opening descriptor"))

    assert(descriptor.baseProse.contains("the minor piece develops"), clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("shared carrier"), clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("Italian Game"), clue(descriptor.baseProse))
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
    val forkFen =
      "6k1/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
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
          Some(exactLineFacts(forkFen, "d4f5", List("d4f5", "h4g5"), List("Nf5", "Qg5+"), "fork"))
        )
        .getOrElse(fail("expected tactical descriptor"))
    val leakedTactical =
      CommentaryIdeaSurface.describe(
        played("d4f5", "Nf5", Square.D4, Square.F5, Piece(Color.White, Knight)),
        evidence(
          facts = List(fork),
          motifs = List(Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 1, Some("Nf5")))
        ),
        Some(exactLineFacts(forkFen, "d4f5", List("d4f5", "h4g5"), List("Nf5", "Qg5+"), "fork_leaked"))
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
    assert(!tactical.prose.contains("opponent"), clue(tactical.prose))
    assert(!tactical.prose.contains("next checked move"), clue(tactical.prose))
    assertEquals(tactical.ideaKind, "fork", clue(tactical))
    assertEquals(tactical.source, "canonical_fact", clue(tactical))
    assert(tactical.reasonTags.contains("fork"), clue(tactical.reasonTags))
    assert(tactical.reasonTags.contains("review_intent:creates_threat"), clue(tactical.reasonTags))
    assert(tactical.confirms.contains("fork"), clue(tactical.confirms))
    assert(tactical.confirms.contains("creates_threat"), clue(tactical.confirms))
    assert(tactical.title.contains("fork"), clue(tactical.title))
    assert(tactical.learningPoint.exists(_.contains("fork targets e7 rook and h4 queen")), clue(tactical.learningPoint))
    assert(tactical.localFact.anchors.exists(anchor => anchor.key == "target_1_square" && anchor.value == "e7"), clue(tactical.localFact.anchors))
    assert(tactical.localFact.anchors.exists(anchor => anchor.key == "target_2_square" && anchor.value == "h4"), clue(tactical.localFact.anchors))
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
    assert(!directThreat.prose.contains("opponent"), clue(directThreat.prose))
    assert(!directThreat.prose.contains("next checked move"), clue(directThreat.prose))
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

  test("endgame opposition and rule-of-square facts must include the played square") {
    val unrelatedOpposition =
      Fact.Opposition(Square.E4, Square.E6, distance = 2, isDirect = true, oppositionType = "Direct", FactScope.CandidatePv)
    val ownedOpposition =
      Fact.Opposition(Square.D2, Square.D4, distance = 2, isDirect = true, oppositionType = "Direct", FactScope.CandidatePv)
    val unrelatedRule =
      Fact.RuleOfSquare(Square.D4, Square.E5, Square.E8, status = "Holds", FactScope.CandidatePv)
    val ownedRule =
      Fact.RuleOfSquare(Square.D2, Square.E5, Square.E8, status = "Holds", FactScope.CandidatePv)
    val line = Some(lineFacts(moveRef("m1", "Kd2", "e1d2", 60), Some(moveRef("m2", "Ke6", "e7e6", 61))))
    val kingMove = played("e1d2", "Kd2", Square.E1, Square.D2, Piece(Color.White, _root_.chess.King))

    assertEquals(
      CommentaryIdeaSurface.describe(kingMove, evidence(facts = List(unrelatedOpposition)), line),
      None,
      clue("opposition fact not involving the played king square should stay closed")
    )
    assert(CommentaryIdeaSurface.describe(kingMove, evidence(facts = List(ownedOpposition)), line).isDefined)
    assertEquals(
      CommentaryIdeaSurface.describe(kingMove, evidence(facts = List(unrelatedRule)), line),
      None,
      clue("rule-of-square fact not involving the played king square should stay closed")
    )
    assert(CommentaryIdeaSurface.describe(kingMove, evidence(facts = List(ownedRule)), line).isDefined)
  }

  test("king-pawn fork shape stays closed without PV material follow-up") {
    val fen =
      "1r4k1/2r5/p3q2p/2p1P1p1/5B2/3P1P2/P4K2/3R2R1 b - - 0 39"
    val playedMove =
      played("b8b2", "Rb2+", Square.B8, Square.B2, Piece(Color.Black, Rook))
    val forkFact =
      Fact.Fork(Square.B2, Rook, List(Square.A2 -> Pawn, Square.F2 -> _root_.chess.King), FactScope.CandidatePv)
    val forkMotif =
      Motif.Fork(
        Rook,
        List(Pawn, _root_.chess.King),
        Square.B2,
        List(Square.A2, Square.F2),
        Color.Black,
        0,
        Some("Rb2+")
      )
    val line =
      exactLineFacts(
        fen,
        "b8b2",
        List("b8b2", "f2e3", "b2b8", "g1g4", "e6e7"),
        List("Rb2+", "Ke3", "Rb8", "Rg4", "Qe7"),
        "king_pawn_fork"
      )

    val factBacked =
      CommentaryIdeaSurface.describe(playedMove, evidence(facts = List(forkFact), motifs = List(forkMotif)), Some(line))
    val motifOnly =
      CommentaryIdeaSurface.describe(playedMove, evidence(motifs = List(forkMotif)), Some(line))

    assertEquals(factBacked, None, clue("king+pawn fork needs a PV capture or stronger target before becoming a tactical claim"))
    assertEquals(motifOnly, None, clue("motif-only king+pawn fork should not bypass the same producer gate"))
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
    val delayedPin =
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
          Some(
            exactLineFacts(
              pinFen,
              "f8b4",
              List("f8b4", "e1f1", "b4a5", "f1g1", "a5c3"),
              List("Bb4", "Kf1", "Ba5", "Kg1", "Bxc3"),
              "pin_delayed_confirmation"
            )
          )
        )
        .getOrElse(fail("expected exact pin descriptor from checked continuation tail"))

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
    assertEquals(delayedPin.ideaKind, "pin", clue(delayedPin))
    assert(delayedPin.reasonTags.contains("line_proof:tactical_threat"), clue(delayedPin.reasonTags))
  }

  test("direct queen capture is not demoted behind a pawn-back skewer motif") {
    val fen =
      "6k1/R7/1r6/1Q3Np1/1KP5/4r3/PP4P1/8 b - - 0 46"
    val skewerFact =
      Fact.Skewer(Square.B5, Rook, Square.B4, _root_.chess.King, Square.B2, Pawn, FactScope.CandidatePv)
    val descriptor =
      CommentaryIdeaSurface
        .describe(
          played(
            "b6b5",
            "Rxb5+",
            Square.B6,
            Square.B5,
            Piece(Color.Black, Rook),
            capturedRole = Some(Queen)
          ),
          evidence(
            facts = List(skewerFact),
            motifs = List(
              Motif.Skewer(
                attackingPiece = Rook,
                frontPiece = _root_.chess.King,
                backPiece = Pawn,
                color = Color.Black,
                plyIndex = 0,
                move = Some("Rxb5+"),
                attackingSq = Some(Square.B5),
                frontSq = Some(Square.B4),
                backSq = Some(Square.B2)
              ),
              Motif.Capture(Rook, Queen, Square.B5, Motif.CaptureType.Winning, Color.Black, 0, Some("Rxb5+"))
            )
          ),
          Some(
            exactLineFacts(
              fen,
              "b6b5",
              List("b6b5", "c4b5", "e3e4", "b4a5", "g8h8"),
              List("Rxb5+", "cxb5", "Re4+", "Ka5", "Kh8"),
              "capture_not_skewer"
            )
          )
        )
        .getOrElse(fail("expected capture descriptor"))

    assertEquals(descriptor.source, "basic_move_explanation", clue(descriptor))
    assertEquals(descriptor.localFact.family, MoveReviewLocalFact.Family.Capture, clue(descriptor.localFact))
    assertEquals(descriptor.localFact.producer, MoveReviewLocalFact.Producer.CaptureSequence, clue(descriptor.localFact))
    assert(descriptor.prose.contains("captures the queen on b5"), clue(descriptor.prose))
    assert(!descriptor.prose.toLowerCase.contains("skewer"), clue(descriptor.prose))
  }

  test("discovered attack and trapped-piece motifs require current move and exact PV proof") {
    val discoveredFen =
      "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1"
    val trappedFen =
      "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1"

    val discovered =
      CommentaryIdeaSurface
        .describe(
          played("d3f4", "Nf4", Square.D3, Square.F4, Piece(Color.White, Knight)),
          evidence(motifs = List(
            Motif.DiscoveredAttack(
              movingPiece = Knight,
              attackingPiece = Bishop,
              target = Queen,
              color = Color.White,
              plyIndex = 0,
              move = Some("Nf4"),
              movingSq = Some(Square.F4),
              attackingSq = Some(Square.B1),
              targetSq = Some(Square.H7)
            )
          )),
          Some(exactLineFacts(discoveredFen, "d3f4", List("d3f4", "h7h2"), List("Nf4", "Qh2+"), "discovered"))
        )
        .getOrElse(fail("expected exact discovered-attack descriptor"))

    val trapped =
      CommentaryIdeaSurface
        .describe(
          played("c3g7", "Bg7", Square.C3, Square.G7, Piece(Color.White, Bishop)),
          evidence(motifs = List(
            Motif.TrappedPiece(
              trappedRole = Rook,
              trappedSquare = Square.H8,
              color = Color.Black,
              plyIndex = 0,
              move = Some("Bg7")
            )
          )),
          Some(exactLineFacts(trappedFen, "c3g7", List("c3g7", "h7h6"), List("Bg7", "h6"), "trapped"))
        )
        .getOrElse(fail("expected exact trapped-piece descriptor"))

    val staleTrapped =
      CommentaryIdeaSurface.describe(
        played("c3g7", "Bg7", Square.C3, Square.G7, Piece(Color.White, Bishop)),
        evidence(motifs = List(
          Motif.TrappedPiece(
            trappedRole = Rook,
            trappedSquare = Square.H8,
            color = Color.Black,
            plyIndex = 0,
            move = Some("Bg7")
          )
        )),
        Some(lineFacts(moveRef("m1", "Bg7", "c3g7", 1), Some(moveRef("m2", "h6", "h7h6", 2))))
      )

    assertEquals(discovered.ideaKind, "discovered_attack", clue(discovered))
    assertEquals(discovered.localFact.family, MoveReviewLocalFact.Family.Threat, clue(discovered.localFact))
    assert(discovered.localFact.guardrails.contains("tactical_kind:discovered_attack"), clue(discovered.localFact.guardrails))
    assert(discovered.localFact.anchors.exists(anchor => anchor.key == "tactical_square" && anchor.value == "h7"), clue(discovered.localFact.anchors))
    assertEquals(trapped.ideaKind, "trapped_piece", clue(trapped))
    assertEquals(trapped.localFact.family, MoveReviewLocalFact.Family.Threat, clue(trapped.localFact))
    assert(trapped.localFact.guardrails.contains("tactical_kind:trapped_piece"), clue(trapped.localFact.guardrails))
    assert(trapped.localFact.anchors.exists(anchor => anchor.key == "tactical_square" && anchor.value == "h8"), clue(trapped.localFact.anchors))
    assertEquals(staleTrapped, None, clue("trapped-piece motif without replayed post-move board attack should stay closed"))
  }

  test("endgame activity descriptors cover exact passed-pawn and rook-behind-passer PVs") {
    val passedPawnFen =
      "8/4k3/8/8/8/8/4P3/4K3 w - - 0 1"
    val rookFen =
      "6k1/8/1P6/8/8/R7/8/6K1 w - - 0 1"
    val frontRookFen =
      "6k1/R7/1P6/8/8/8/8/6K1 w - - 0 1"
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
          played("a3b3", "Rb3", Square.A3, Square.B3, Piece(Color.White, Rook)),
          evidence(facts = List(Fact.RookEndgamePattern("RookBehindPassedPawn", FactScope.CandidatePv))),
          Some(exactLineFacts(rookFen, "a3b3", List("a3b3", "g8f8"), List("Rb3", "Kf8"), "rook_activity"))
        )
        .getOrElse(fail("expected exact rook-activity descriptor"))

    assertEquals(passedPawn.reviewIntent, "improves_endgame_activity", clue(passedPawn))
    assertEquals(passedPawn.linePurpose, Some("improve_endgame_activity"), clue(passedPawn))
    assert(passedPawn.reasonTags.contains("pawn_promotion"), clue(passedPawn.reasonTags))
    assert(passedPawn.confirms.contains("endgame_activity"), clue(passedPawn.confirms))
    assertEquals(rookActivity.reviewIntent, "improves_endgame_activity", clue(rookActivity))
    assert(rookActivity.reasonTags.contains("rook_endgame_pattern"), clue(rookActivity.reasonTags))
    assert(rookActivity.localFact.anchors.exists(anchor => anchor.key == "rook_square" && anchor.value == "b3"), clue(rookActivity.localFact))
    assert(rookActivity.localFact.anchors.exists(anchor => anchor.key == "passed_pawn_square" && anchor.value == "b6"), clue(rookActivity.localFact))

    val staleRookPattern =
      CommentaryIdeaSurface.describe(
        played("a7b7", "Rb7", Square.A7, Square.B7, Piece(Color.White, Rook)),
        evidence(facts = List(Fact.RookEndgamePattern("RookBehindPassedPawn", FactScope.CandidatePv))),
        Some(exactLineFacts(frontRookFen, "a7b7", List("a7b7", "g8f8"), List("Rb7", "Kf8"), "rook_activity"))
      )

    assertEquals(staleRookPattern, None, clue("rook pattern fact without a rook-behind-passer after-board should stay closed"))
  }

  test("capture descriptors stay PV-backed and owned by the idea surface") {
    val pawnCaptureFen =
      "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
    val bishopCaptureFen =
      "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val queenTradePreludeFen =
      "rnbqkbnr/ppp2ppp/3p4/4P3/4P3/8/PPP2PPP/RNBQKBNR b KQkq - 0 3"
    val delayedQueenTradeFen =
      "r1bqkb1r/pp1p1pp1/2nN1n1p/4p3/4P3/2N5/PPP2PPP/R1BQKB1R b KQkq - 1 7"
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
    val queenTradePrelude =
      CommentaryIdeaSurface
        .describe(
          played("d6e5", "dxe5", Square.D6, Square.E5, Piece(Color.Black, Pawn), Some(Pawn)),
          evidence(motifs =
            List(Motif.Capture(Pawn, Pawn, Square.E5, Motif.CaptureType.Normal, Color.Black, 6, Some("dxe5")))
          ),
          Some(
            exactLineFacts(
              queenTradePreludeFen,
              "d6e5",
              List("d6e5", "d1d8", "e8d8", "c1e3"),
              List("dxe5", "Qxd8+", "Kxd8", "Be3"),
              "queen_trade_prelude"
            )
          )
        )
        .getOrElse(fail("expected queen-trade prelude capture descriptor"))
    val delayedQueenTrade =
      CommentaryIdeaSurface
        .describe(
          played("f8d6", "Bxd6", Square.F8, Square.D6, Piece(Color.Black, Bishop), Some(Knight)),
          evidence(motifs =
            List(Motif.Capture(Bishop, Knight, Square.D6, Motif.CaptureType.Exchange, Color.Black, 14, Some("Bxd6")))
          ),
          Some(
            exactLineFacts(
              delayedQueenTradeFen,
              "f8d6",
              List("f8d6", "d1d6", "d8e7", "d6e7", "e8e7"),
              List("Bxd6", "Qxd6", "Qe7", "Qxe7+", "Kxe7"),
              "delayed_queen_trade"
            )
          )
        )
        .getOrElse(fail("expected delayed queen-trade capture descriptor"))
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
    assertEquals(queenTradePrelude.ideaKind, "exchange_clarified", clue(queenTradePrelude))
    assertEquals(queenTradePrelude.linePurpose, Some("clarify_exchange"), clue(queenTradePrelude))
    assert(
      queenTradePrelude.localFact.anchors.exists(anchor =>
        anchor.key == "followup_queen_trade_square" && anchor.value == "d8"
      ),
      clue(queenTradePrelude.localFact.anchors)
    )
    assert(queenTradePrelude.localFact.anchors.exists(anchor => anchor.key == "followup_queen_trade_capture_san" && anchor.value == "Qxd8+"), clue(queenTradePrelude.localFact.anchors))
    assert(queenTradePrelude.localFact.anchors.exists(anchor => anchor.key == "followup_queen_trade_recapture_san" && anchor.value == "Kxd8"), clue(queenTradePrelude.localFact.anchors))
    assert(queenTradePrelude.localFact.evidenceRefs.contains("followup_queen_trade_square:d8"), clue(queenTradePrelude.localFact.evidenceRefs))
    assert(queenTradePrelude.scopedTakeaway.exists(_.text.contains("Qxd8+ and Kxd8 trade queens on d8")), clue(queenTradePrelude.scopedTakeaway))
    assertEquals(delayedQueenTrade.ideaKind, "exchange_clarified", clue(delayedQueenTrade))
    assert(delayedQueenTrade.localFact.evidenceRefs.contains("followup_queen_trade_square:e7"), clue(delayedQueenTrade.localFact.evidenceRefs))
    assert(delayedQueenTrade.localFact.anchors.exists(anchor => anchor.key == "followup_queen_trade_capture_san" && anchor.value == "Qxe7+"), clue(delayedQueenTrade.localFact.anchors))
    assert(delayedQueenTrade.localFact.anchors.exists(anchor => anchor.key == "followup_queen_trade_recapture_san" && anchor.value == "Kxe7"), clue(delayedQueenTrade.localFact.anchors))
    assert(delayedQueenTrade.scopedTakeaway.exists(_.text.contains("Qxe7+ and Kxe7 trade queens on e7")), clue(delayedQueenTrade.scopedTakeaway))
    assertEquals(noReply, None)
  }

  test("defensive intent requires defense truth and PV proof instead of target creation") {
    val current =
      played("g1f3", "Nf3", Square.G1, Square.F3, Piece(Color.White, Knight))
    val target =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.F3), Nil, FactScope.CandidatePv)
    val defensiveTarget =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.F3), Nil, FactScope.ThreatLine)
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
        .describe(current, evidence(facts = List(defensiveTarget)), line, truthContract = Some(defensiveContract))
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
      CommentaryIdeaSurface.describe(current, evidence(strategicDeltas = List(certified)), None)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(certified)), line)
        .getOrElse(fail("expected certified strategic support descriptor"))
    val risky =
      certified.copy(packet = certified.packet.copy(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease)))

    assertEquals(packetOnly, None)
    assertEquals(
      CommentaryIdeaSurface.describe(current, evidence(strategicDeltas = List(risky)), line),
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
          evidence(strategicDeltas = List(strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.ExchangeForcing))),
          line
        )
        .getOrElse(fail("expected exchange descriptor"))
    val plan =
      CommentaryIdeaSurface
        .describe(
          current,
          evidence(strategicDeltas = List(strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.PlanAdvance))),
          line
        )
        .getOrElse(fail("expected plan descriptor"))

    assert(exchange.baseProse.contains("h3 clarifies the exchange around b5."), clue(exchange.baseProse))
    assert(plan.baseProse.contains("h3 is tied to local plan support around b5."), clue(plan.baseProse))
    assert(!exchange.baseProse.contains("certified"), clue(exchange.baseProse))
    assert(!plan.baseProse.contains("certified"), clue(plan.baseProse))
  }

  test("certified strategic support surfaces exact knight-outpost proof anchors") {
    val startFen =
      "rnbqk2r/ppp1ppbp/5np1/3p4/3P1B2/2N2N1P/PPP1PPP1/R2QKB1R b KQkq - 3 5"
    val current =
      played("f6e4", "Ne4", Square.F6, Square.E4, Piece(Color.Black, Knight))
    val line =
      Some(exactLineFacts(startFen, "f6e4", List("f6e4", "e2e3", "e4c3", "b2c3", "c7c5"), List("Ne4", "e3", "Nxc3", "bxc3", "c5"), "strategy"))
    val proofFamily = ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.OutpostEntrenchment).get.wireKey
    val exactProof = PlayerFacingExactSliceProof.OutpostOccupation("knight", "e4")
    val baseDelta =
      strategicDelta(
        proofFamily,
        proofFamily,
        PlayerFacingMoveDeltaClass.PressureIncrease,
        exactProof = Some(exactProof)
      )
    val outpostDelta =
      baseDelta.copy(
        anchorTerms = List("e4"),
        packet =
          baseDelta.packet.copy(
            triggerKind = PlanTaxonomy.PlanKind.OutpostEntrenchment.id,
            anchorTerms = List("e4"),
            proofPathWitness =
              baseDelta.packet.proofPathWitness.copy(
                ownerSeedTerms = List("e4", "outpost:e4", "piece:knight", "outpost_occupation:knight:e4", proofFamily),
                continuationTerms = List("e3"),
                structureTransitionTerms = List("outpost_occupation", "outpost:e4", "piece:knight", "outpost_occupation:knight:e4"),
                exactSliceProof = Some(exactProof)
              )
          )
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(outpostDelta)), line)
        .getOrElse(fail("expected exact knight-outpost descriptor"))

    assertEquals(descriptor.baseProse, "Ne4 puts the knight on the e4 outpost.", clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "outpost_square" && anchor.value == "e4"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "outpost_piece_role" && anchor.value == "knight"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.evidenceRefs.contains("outpost_square:e4"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("outpost_piece_role:knight"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("outpost_occupation_exact_proof"), clue(descriptor.localFact.evidenceRefs))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("knight outpost on e4 local to Ne4")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("certified strategic support surfaces exact target-fixation proof anchors") {
    val startFen =
      "r2qk2r/pp3ppp/2n1pn2/1B1pPb2/3P4/2P5/PP4PP/RNBQK2R b KQkq - 0 10"
    val current =
      played(
        "f6e4",
        "Ne4",
        Square.F6,
        Square.E4,
        Piece(Color.Black, Knight),
        afterFen = "r2qk2r/pp3ppp/2n1p3/1B1pPb2/3Pn3/2P5/PP4PP/RNBQK2R w KQkq - 1 1"
      )
    val line =
      Some(exactLineFacts(startFen, "f6e4", List("f6e4", "e1g1", "d8b6", "d1e2", "e8g8"), List("Ne4", "O-O", "Qb6", "Qe2", "O-O"), "strategy"))
    val proofFamily = ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).get.wireKey
    val proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource
    val targetSquare = "c3"
    val exactProof = PlayerFacingExactSliceProof.ExactTargetFixation(targetSquare)
    val baseDelta =
      strategicDelta(
        proofFamily,
        proofSource,
        PlayerFacingMoveDeltaClass.PressureIncrease,
        exactProof = Some(exactProof)
      )
    val targetDelta =
      baseDelta.copy(
        anchorTerms = List("control of the open file", targetSquare),
        packet =
          baseDelta.packet.copy(
            triggerKind = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
            anchorTerms = List("control of the open file", targetSquare),
            proofPathWitness =
              baseDelta.packet.proofPathWitness.copy(
                ownerSeedTerms = List("control of the open file", s"fixed_target:$targetSquare", "exact_target_fixation"),
                continuationTerms = List("exact_target_fixation", s"fixed_target:$targetSquare", "best_branch:f6e4|e1g1"),
                structureTransitionTerms =
                  List(
                    "exact_target_fixation",
                    s"fixed_target:$targetSquare",
                    s"target_persistent_after_line:$targetSquare",
                    s"target_attacked_after_line:$targetSquare"
                  ),
                exactSliceProof = Some(exactProof)
              )
          )
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(targetDelta)), line)
        .getOrElse(fail("expected exact target-fixation descriptor"))

    assertEquals(descriptor.baseProse, "Ne4 pressures the c3 pawn target.", clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("open file"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "exact_target_square" && anchor.value == "c3"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "exact_target_kind" && anchor.value == "pawn_target"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "exact_target_piece" && anchor.value == "pawn"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.evidenceRefs.contains("exact_target_square:c3"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("exact_target_piece:pawn"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("fixed_target:c3"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("exact_target_board_attack"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("target_persistent_after_line:c3"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("target_attacked_after_line:c3"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("exact_target_fixation_exact_proof"), clue(descriptor.localFact.evidenceRefs))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("pressure on the c3 pawn target local to Ne4")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("certified strategic support does not surface target fixation when the proof square is the moved piece") {
    val startFen =
      "r2qk2r/pp3ppp/2n1pn2/1B1pPb2/3P4/2P5/PP4PP/RNBQK2R b KQkq - 0 10"
    val current =
      played(
        "f6e4",
        "Ne4",
        Square.F6,
        Square.E4,
        Piece(Color.Black, Knight),
        afterFen = "r2qk2r/pp3ppp/2n1p3/1B1pPb2/3Pn3/2P5/PP4PP/RNBQK2R w KQkq - 1 1"
      )
    val line =
      Some(exactLineFacts(startFen, "f6e4", List("f6e4", "e1g1", "d8b6", "d1e2", "e8g8"), List("Ne4", "O-O", "Qb6", "Qe2", "O-O"), "strategy"))
    val proofFamily = ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).get.wireKey
    val proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource
    val exactProof = PlayerFacingExactSliceProof.ExactTargetFixation("e4")
    val baseDelta =
      strategicDelta(
        proofFamily,
        proofSource,
        PlayerFacingMoveDeltaClass.PressureIncrease,
        exactProof = Some(exactProof)
      )
    val targetDelta =
      baseDelta.copy(
        anchorTerms = List("control of the open file", "e4"),
        packet =
          baseDelta.packet.copy(
            triggerKind = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
            anchorTerms = List("control of the open file", "e4"),
            proofPathWitness =
              baseDelta.packet.proofPathWitness.copy(
                ownerSeedTerms = List("control of the open file", "fixed_target:e4", "exact_target_fixation"),
                continuationTerms = List("exact_target_fixation", "fixed_target:e4", "best_branch:f6e4|e1g1"),
                structureTransitionTerms = List("exact_target_fixation", "fixed_target:e4"),
                exactSliceProof = Some(exactProof)
              )
          )
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(targetDelta)), line)
        .getOrElse(fail("expected generic certified strategy descriptor"))

    assert(!descriptor.baseProse.contains("fixes"), clue(descriptor.baseProse))
    assert(!descriptor.baseProse.contains("pawn target"), clue(descriptor.baseProse))
    assert(!descriptor.localFact.anchors.exists(_.key == "exact_target_square"), clue(descriptor.localFact.anchors))
    assert(!descriptor.localFact.evidenceRefs.exists(_.startsWith("exact_target_square:")), clue(descriptor.localFact.evidenceRefs))
  }

  test("color-complex position probe surfaces only when the played move owns the minor-piece attack") {
    val startFen =
      "1r1q1rk1/ppp2ppp/2n1pb2/3p1b2/3P2P1/1QP1PN1P/PP1N1P2/R3KB1R b KQ - 0 11"
    val current =
      played(
        "f5g6",
        "Bg6",
        Square.F5,
        Square.G6,
        Piece(Color.Black, Bishop),
        afterFen = "1r1q1rk1/ppp2ppp/2n1pbb1/3p4/3P2P1/1QP1PN1P/PP1N1P2/R3KB1R w KQ - 1 12"
      )
    val line =
      Some(exactLineFacts(startFen, "f5g6", List("f5g6", "h3h4", "h7h5"), List("Bg6", "h4", "h5"), "strategy"))
    val proof = PlayerFacingExactSliceProof.ColorComplexSqueeze("e4", "light", "bishop", "g6")
    val delta = colorComplexPositionProbeDelta(proof)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(delta)), line)
        .getOrElse(fail("expected played-owned color-complex descriptor"))

    assertEquals(
      descriptor.baseProse,
      "The checked line keeps the bishop on g6 attacking e4 in the light-square complex.",
      clue(descriptor.baseProse)
    )
    assertEquals(descriptor.title, "Bg6 has checked positional support", clue(descriptor.title))
    assert(!descriptor.title.toLowerCase.contains("color complex"), clue(descriptor.title))
    assertEquals(descriptor.localFact.authority, MoveReviewLocalFact.Authority.CertifiedStrategy)
    assert(descriptor.localFact.evidenceRefs.contains("proof_family:color_complex_squeeze"), clue(descriptor.localFact.evidenceRefs))
    assert(descriptor.localFact.evidenceRefs.contains("target:e4"), clue(descriptor.localFact.evidenceRefs))
  }

  test("color-complex position probe does not create a MoveReview local fact for an unmoved minor piece") {
    val startFen =
      "2r2rk1/pp3ppp/3qpn2/n2p1b2/3P4/2P2N1P/PP1NBPP1/R2QR1K1 w - - 6 13"
    val current =
      played(
        "d2f1",
        "Nf1",
        Square.D2,
        Square.F1,
        Piece(Color.White, Knight),
        afterFen = "2r2rk1/pp3ppp/3qpn2/n2p1b2/3P4/2P2N1P/PP2BPP1/R2QRNK1 b - - 7 13"
      )
    val line =
      Some(exactLineFacts(startFen, "d2f1", List("d2f1", "d6b6", "d1c1"), List("Nf1", "Qb6", "Qc1"), "strategy"))
    val proof = PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "dark", "knight", "f3")
    val descriptor =
      CommentaryIdeaSurface.describe(current, evidence(strategicDeltas = List(colorComplexPositionProbeDelta(proof))), line)

    assertEquals(descriptor, None)
  }

  test("practical line occupation fact surfaces file and target from certified strategy evidence") {
    val startFen =
      "r3k2r/pp3ppp/2nqpn2/3p1b2/3P4/2P2N1P/PP1NBPP1/R2QK2R b KQkq - 1 10"
    val current =
      played("a8c8", "Rc8", Square.A8, Square.C8, Piece(Color.Black, Rook))
    val line =
      Some(exactLineFacts(startFen, "a8c8", List("a8c8", "e1g1", "h7h6", "a2a4"), List("Rc8", "O-O", "h6", "a4"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_semi_open_c_file",
        ownerSide = "black",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c8", "c4"),
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.84,
        evidenceRefs = List("source:semi_open_file_control", "semi_open_file_c")
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val fact = facts.headOption.getOrElse(fail("expected practical line occupation fact"))
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected practical position descriptor"))

    assertEquals(fact.text, "Rc8 puts the rook on the semi-open c-file toward c4.", clue(fact))
    assert(fact.evidenceRefs.contains("line_occupation_file:c"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("line_occupation_status:semi_open"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("line_occupation_target:c4"), clue(fact.evidenceRefs))
    assertEquals(descriptor.title, "Rc8 has checked positional support", clue(descriptor.title))
    assert(!descriptor.title.toLowerCase.contains("line occupation"), clue(descriptor.title))
    assert(descriptor.baseProse.contains("semi-open c-file toward c4"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "line_file" && anchor.value == "c"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "line_file_status" && anchor.value == "semi_open"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "line_target" && anchor.value == "c4"), clue(descriptor.localFact.anchors))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("semi-open c-file occupation toward c4 local to Rc8")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("practical pawn-break fact surfaces played pawn file from certified strategy evidence") {
    val startFen =
      "rnbqkb1r/ppp1pppp/5n2/3P4/2P5/8/PP1P1PPP/RNBQKBNR b KQkq - 0 3"
    val current =
      played("c7c5", "c5", Square.C7, Square.C5, Piece(Color.Black, Pawn))
    val line =
      Some(exactLineFacts(startFen, "c7c5", List("c7c5", "d1a4", "c8d7", "a4b3"), List("c5", "Qa4+", "Bd7", "Qb3"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_c_file_break",
        ownerSide = "black",
        kind = StrategicIdeaKind.PawnBreak,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Ready,
        focusFiles = List("c", "d"),
        focusZone = Some("center"),
        confidence = 0.88,
        evidenceRefs =
          List(
            "source:file_opening_consequence",
            "contested_file_c",
            "source:pawn_analysis_break_ready",
            "source:pawn_play_break_ready",
            "pawn_analysis_break_ready_shape",
            "pawn_play_break_ready_shape",
            "break_file_c",
            "break_file_d"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val fact = facts.headOption.getOrElse(fail("expected practical pawn-break fact"))
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected practical position descriptor"))
    val genericFacts =
      CommentaryIdeaSurface.practicalPositionFacts(
        current,
        surface.copy(allIdeas = List(idea.copy(ideaId = "idea_wrong_break_file", evidenceRefs = List("source:pawn_analysis_break_ready", "break_file_d"))))
      )

    assertEquals(fact.text, "c5 plays the c-pawn break and contests the c-file.", clue(fact))
    assert(fact.evidenceRefs.contains("pawn_break_file:c"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("pawn_break_contested_file:c"), clue(fact.evidenceRefs))
    assert(descriptor.baseProse.contains("c-pawn break"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "pawn_break_file" && anchor.value == "c"), clue(descriptor.localFact.anchors))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("c-pawn break local to c5")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
    assert(!genericFacts.exists(_.evidenceRefs.exists(_.startsWith("pawn_break_file:"))), clue(genericFacts))
  }

  test("practical target-fixing fact surfaces weak square only when the played piece attacks it") {
    val startFen =
      "r1bq1rk1/p3ppbp/2p2np1/3p4/3P4/2N3P1/PP2PPBP/R1BQ1RK1 b - - 1 10"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("c8a6"))
    val current =
      played("c8a6", "Ba6", Square.C8, Square.A6, Piece(Color.Black, Bishop), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "c8a6", List("c8a6", "c1f4", "f6d7", "d1a4"), List("Ba6", "Bf4", "Nd7", "Qa4"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_bishop_pressure_c4",
        ownerSide = "black",
        kind = StrategicIdeaKind.TargetFixing,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c4", "d4", "h2"),
        confidence = 0.84,
        evidenceRefs =
          List(
            "source:plan_match_target_fixing",
            "source:enemy_weak_square",
            "enemy_weak_square_c4",
            "enemy_weak_square_d4",
            "source:weak_complex_fixation"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = List(StrategyPieceMoveRef("black", "B", "c8", "a6", "target_fixing")),
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val fact = facts.headOption.getOrElse(fail("expected practical target-fixing fact"))
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected practical target-fixing descriptor"))

    assertEquals(fact.text, "Ba6 puts the bishop on a line toward the weak c4 square.", clue(fact))
    assert(fact.evidenceRefs.contains("target_fixing_square:c4"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("target_fixing_target_kind:weak_square"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("target_fixing_board_attack"), clue(fact.evidenceRefs))
    assert(descriptor.baseProse.contains("weak c4 square"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "target_fixing_square" && anchor.value == "c4"), clue(descriptor.localFact.anchors))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "target_fixing_attacker_square" && anchor.value == "a6"), clue(descriptor.localFact.anchors))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("pressure on the weak c4 square local to Ba6")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("practical target-fixing fact stays generic when the played move does not attack the weak square") {
    val startFen =
      "r2qk1nr/3nppbp/2pp2p1/pp6/3P1B2/2PBPP2/PPQNKP1P/R6R b kq - 1 10"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("c6c5"))
    val current =
      played("c6c5", "c5", Square.C6, Square.C5, Piece(Color.Black, Pawn), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "c6c5", List("c6c5", "d3b5", "a8c8", "c2d3"), List("c5", "Bxb5", "Rc8", "Qd3"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_pawn_does_not_attack_c4",
        ownerSide = "black",
        kind = StrategicIdeaKind.TargetFixing,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c4", "d3"),
        confidence = 0.84,
        evidenceRefs =
          List(
            "source:plan_match_target_fixing",
            "source:directional_target_fixation",
            "directional_target_c4",
            "source:enemy_weak_square",
            "enemy_weak_square_c4",
            "enemy_weak_square_d3"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = List(StrategyPieceMoveRef("black", "P", "c6", "c5", "target_fixing")),
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected generic practical target-fixing descriptor"))

    assert(facts.nonEmpty, clue(facts))
    assert(!facts.exists(_.evidenceRefs.contains("target_fixing_board_attack")), clue(facts))
    assert(!descriptor.baseProse.contains("weak c4 square"), clue(descriptor.baseProse))
    assert(!descriptor.scopedTakeaway.exists(_.text.contains("weak c4 square")), clue(descriptor.scopedTakeaway.map(_.text)))
  }

  test("practical space fact surfaces a board-proved rook-pawn space gain") {
    val startFen =
      "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R b KQkq - 0 5"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("h7h6"))
    val current =
      played("h7h6", "h6", Square.H7, Square.H6, Piece(Color.Black, Pawn), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "h7h6", List("h7h6", "c3d5", "d7d6", "c2c3", "e8g8"), List("h6", "Nd5", "d6", "c3", "O-O"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_h_pawn_space_gain",
        ownerSide = "black",
        kind = StrategicIdeaKind.SpaceGainOrRestriction,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Build,
        focusFiles = List("g", "h"),
        confidence = 0.86,
        evidenceRefs =
          List(
            "source:pawn_chain_space_motif",
            "pawn_chain_space_shape",
            "pawn_chain_g_h",
            "source:plan_match_space_advantage",
            "plan_spaceadvantage"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val fact = facts.headOption.getOrElse(fail("expected practical space-gain fact"))
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected practical space-gain descriptor"))

    assertEquals(fact.text, "h6 advances the h-pawn for kingside space.", clue(fact))
    assert(fact.evidenceRefs.contains("space_gain_file:h"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("space_gain_side:kingside"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("space_gain_rook_pawn_advance"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("space_gain_board_pawn_advance"), clue(fact.evidenceRefs))
    assert(descriptor.baseProse.contains("h-pawn for kingside space"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "space_gain_file" && anchor.value == "h"), clue(descriptor.localFact.anchors))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("h-pawn space gain on the kingside local to h6")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("practical space fact stays generic without pawn-chain shape proof") {
    val startFen =
      "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R b KQkq - 0 5"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("h7h6"))
    val current =
      played("h7h6", "h6", Square.H7, Square.H6, Piece(Color.Black, Pawn), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "h7h6", List("h7h6", "c3d5", "d7d6", "c2c3", "e8g8"), List("h6", "Nd5", "d6", "c3", "O-O"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_h_pawn_space_without_shape",
        ownerSide = "black",
        kind = StrategicIdeaKind.SpaceGainOrRestriction,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Build,
        focusFiles = List("g", "h"),
        confidence = 0.86,
        evidenceRefs =
          List(
            "source:pawn_chain_space_motif",
            "source:plan_match_space_advantage",
            "plan_spaceadvantage"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = Nil,
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected generic practical space descriptor"))

    assert(facts.nonEmpty, clue(facts))
    assert(!facts.exists(_.evidenceRefs.contains("space_gain_rook_pawn_advance")), clue(facts))
    assert(!descriptor.baseProse.contains("advances the h-pawn"), clue(descriptor.baseProse))
    assert(!descriptor.scopedTakeaway.exists(_.text.contains("space gain")), clue(descriptor.scopedTakeaway.map(_.text)))
  }

  test("practical king-attack fact surfaces a board-proved directional attack lane") {
    val startFen =
      "r2qkb1r/pp3ppp/2n1bn2/2p5/2P5/6N1/PP1PBPPP/RNBQK2R b KQkq - 3 7"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("f8d6"))
    val current =
      played("f8d6", "Bd6", Square.F8, Square.D6, Piece(Color.Black, Bishop), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "f8d6", List("f8d6", "b1c3", "d6e5", "d2d3", "h7h5"), List("Bd6", "Nc3", "Be5", "d3", "h5"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_bishop_attack_lane_f4",
        ownerSide = "black",
        kind = StrategicIdeaKind.KingAttackBuildUp,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c3"),
        focusFiles = List("h"),
        confidence = 0.88,
        evidenceRefs =
          List(
            "source:compensation_king_window",
            "material_deficit_compensation",
            "uncastled_or_unsettled_king_window",
            "source:directional_attack_lane",
            "directional_attack_lane_shape",
            "source:enemy_weak_back_rank",
            "enemy_weak_back_rank_shape"
          )
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = List(StrategyPieceRoute("black", "B", "f8", List("d6", "f4"), "directional_attack_lane", 0.9, List("directional_attack_lane_shape"))),
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val fact = facts.headOption.getOrElse(fail("expected practical attack-lane fact"))
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected practical attack-lane descriptor"))

    assertEquals(fact.text, "Bd6 puts the bishop on a diagonal toward f4.", clue(fact))
    assert(fact.evidenceRefs.contains("attack_lane_square:f4"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("attack_lane_axis:diagonal"), clue(fact.evidenceRefs))
    assert(fact.evidenceRefs.contains("attack_lane_board_attack"), clue(fact.evidenceRefs))
    assert(descriptor.baseProse.contains("diagonal toward f4"), clue(descriptor.baseProse))
    assert(descriptor.localFact.anchors.exists(anchor => anchor.key == "attack_lane_square" && anchor.value == "f4"), clue(descriptor.localFact.anchors))
    assert(
      descriptor.scopedTakeaway.exists(_.text.contains("diagonal attack lane toward f4 local to Bd6")),
      clue(descriptor.scopedTakeaway.map(_.text))
    )
  }

  test("practical king-attack fact stays generic when the only lane anchor is the origin square") {
    val startFen =
      "r1b1kb1r/pp2nppp/1qn1p3/2ppP3/5B2/3P1N2/PPPN1PPP/R1Q1KB1R b KQkq - 6 7"
    val afterFen = NarrativeUtils.uciListToFen(startFen, List("c8d7"))
    val current =
      played("c8d7", "Bd7", Square.C8, Square.D7, Piece(Color.Black, Bishop), afterFen = afterFen)
    val line =
      Some(exactLineFacts(startFen, "c8d7", List("c8d7", "f1e2", "f7f6", "e5f6", "g7f6"), List("Bd7", "Be2", "f6", "exf6", "gxf6"), "strategy"))
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_origin_square_is_not_attack_lane",
        ownerSide = "black",
        kind = StrategicIdeaKind.KingAttackBuildUp,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c3"),
        confidence = 0.88,
        evidenceRefs = List("source:directional_attack_lane", "directional_attack_lane_shape")
      )
    val surface =
      StrategyPackSurface.Snapshot(
        sideToMove = Some("black"),
        dominantIdea = Some(idea),
        secondaryIdea = None,
        campaignOwner = Some("black"),
        ownerMismatch = false,
        allRoutes = List(StrategyPieceRoute("black", "B", "c8", List("d7", "c8"), "directional_attack_lane", 0.9, List("directional_attack_lane_shape"))),
        topRoute = None,
        allMoveRefs = Nil,
        topMoveRef = None,
        allDirectionalTargets = Nil,
        topDirectionalTarget = None,
        longTermFocus = None,
        evidenceHints = Nil,
        compensationSummary = None,
        compensationVectors = Nil,
        investedMaterial = None,
        compensationSubtype = None,
        allIdeas = List(idea)
      )
    val facts = CommentaryIdeaSurface.practicalPositionFacts(current, surface)
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(practicalPositionFacts = facts), line)
        .getOrElse(fail("expected generic practical king-attack descriptor"))

    assert(facts.nonEmpty, clue(facts))
    assert(!facts.exists(_.evidenceRefs.contains("attack_lane_board_attack")), clue(facts))
    assert(!descriptor.baseProse.contains("attack lane"), clue(descriptor.baseProse))
    assert(!descriptor.scopedTakeaway.exists(_.text.contains("attack lane")), clue(descriptor.scopedTakeaway.map(_.text)))
  }

  test("MoveReviewLocalFact centralizes strategic move-delta family admission") {
    val cases =
      List(
        PlayerFacingMoveDeltaClass.CounterplayReduction -> MoveReviewLocalFact.Family.Defense,
        PlayerFacingMoveDeltaClass.ExchangeForcing -> MoveReviewLocalFact.Family.LineConsequence,
        PlayerFacingMoveDeltaClass.PlanAdvance -> MoveReviewLocalFact.Family.PlanSupport,
        PlayerFacingMoveDeltaClass.ResourceRemoval -> MoveReviewLocalFact.Family.Defense,
        PlayerFacingMoveDeltaClass.PressureIncrease -> MoveReviewLocalFact.Family.Pressure,
        PlayerFacingMoveDeltaClass.NewAccess -> MoveReviewLocalFact.Family.LineConsequence
      )

    cases.foreach { case (deltaClass, expectedFamily) =>
      val delta =
        strategicDelta("neutralize_key_break", "counterplay_axis_suppression", deltaClass, exactProof = None)
      val candidate =
        MoveReviewLocalFact.strategicMoveDeltaCandidate(
          delta,
          anchors = List(MoveReviewLocalFact.Anchor("preferred_subject", "b5")),
          guardrails =
            List(
              s"proof_family:${delta.packet.proofFamily}",
              s"proof_source:${delta.packet.proofSource}",
              "strict_requires_causal_or_exact_fallback"
            )
        )
      val decision = MoveReviewLocalFact.admit(candidate)

      assertEquals(decision.admission.map(_.family), Some(expectedFamily), clue(deltaClass))
      assertEquals(decision.admission.map(_.authority), Some(MoveReviewLocalFact.Authority.CertifiedStrategy), clue(deltaClass))
      assertEquals(decision.admission.map(_.strictFallbackEligible), Some(false), clue(deltaClass))
      assertEquals(candidate.subject, MoveReviewLocalFact.Subject.PlanResource, clue(deltaClass))
      assertEquals(candidate.lineBinding, MoveReviewLocalFact.LineBinding.PvCoupled, clue(deltaClass))
    }
  }

  test("MoveReviewLocalFact prefers exact proof family over strategic move-delta class") {
    val delta =
      strategicDelta(
        ProofFamilyId.HalfOpenFilePressure.wireKey,
        ProofSourceId.LocalFileEntryBind.wireKey,
        PlayerFacingMoveDeltaClass.NewAccess,
        exactProof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
      )
    val candidate =
      MoveReviewLocalFact.strategicMoveDeltaCandidate(
        delta,
        anchors = List(MoveReviewLocalFact.Anchor("preferred_subject", "c6")),
        guardrails =
          List(
            s"proof_family:${delta.packet.proofFamily}",
            s"proof_source:${delta.packet.proofSource}",
            "strict_requires_causal_or_exact_fallback"
          )
      )
    val decision = MoveReviewLocalFact.admit(candidate)

    assertEquals(decision.admission.map(_.family), Some(MoveReviewLocalFact.Family.Pressure), clue(candidate))
    assertEquals(decision.admission.map(_.authority), Some(MoveReviewLocalFact.Authority.CertifiedStrategy), clue(candidate))
  }

  test("certified strategic support ignores generic main-plan anchor text") {
    val startFen =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val current =
      played("h2h3", "h3", Square.H2, Square.H3, Piece(Color.White, Pawn))
    val line =
      Some(exactLineFacts(startFen, "h2h3", List("h2h3", "b7b5", "a2a4"), List("h3", "b5", "a4"), "strategy"))
    val delta = strategicDelta("neutralize_key_break", "counterplay_axis_suppression", PlayerFacingMoveDeltaClass.PlanAdvance)
    val genericDelta =
      delta.copy(
        anchorTerms = List("plan activation lane"),
        packet =
          delta.packet.copy(
            anchorTerms = List("plan activation lane")
          )
      )
    val descriptor =
      CommentaryIdeaSurface
        .describe(current, evidence(strategicDeltas = List(genericDelta)), line)
        .getOrElse(fail("expected plan descriptor"))

    assert(!descriptor.baseProse.contains("main plan"), clue(descriptor.baseProse))
    assertEquals(descriptor.baseProse, "h3 is tied to local plan support around b5.", clue(descriptor.baseProse))
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
      deltaClass: PlayerFacingMoveDeltaClass,
      exactProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("b5"))
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
          exactSliceProof = exactProof
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

  private def colorComplexPositionProbeDelta(
      proof: PlayerFacingExactSliceProof.ColorComplexSqueeze
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
        proofSource = ProofSourceId.ColorComplexSqueezeProbe.wireKey,
        proofFamily = ProofFamilyId.ColorComplexSqueeze.wireKey,
        scope = PlayerFacingPacketScope.PositionLocal,
        triggerKind = "position_probe",
        anchorTerms = List(proof.targetSquare),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness = PlayerFacingProofPathWitness(
          ownerSeedTerms =
            List(
              proof.targetSquare,
              s"weak_square:${proof.targetSquare}",
              s"color_complex:${proof.squareColor}",
              s"minor_piece:${proof.minorPieceRole}_${proof.minorPieceSquare}",
              s"minor_piece_attack:${proof.minorPieceSquare}-${proof.targetSquare}"
            ),
          continuationTerms = List("color_complex_squeeze_probe"),
          structureTransitionTerms =
            List(
              "color_complex_squeeze_probe",
              s"weak_square:${proof.targetSquare}",
              s"minor_piece_attack:${proof.minorPieceSquare}-${proof.targetSquare}"
            ),
          exactSliceProof = Some(proof)
        ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    PlayerFacingMoveDeltaEvidence(
      deltaClass = PlayerFacingMoveDeltaClass.CounterplayReduction,
      anchorTerms = List(proof.targetSquare),
      quantifier = PlayerFacingClaimQuantifier.BestResponse,
      modalityTier = PlayerFacingClaimModalityTier.Supports,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
      provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
      certificateStatus = PlayerFacingCertificateStatus.Valid,
      ontologyFamily = PlayerFacingClaimOntologyKind.ColorComplexSqueeze,
      packet = packet
    )
