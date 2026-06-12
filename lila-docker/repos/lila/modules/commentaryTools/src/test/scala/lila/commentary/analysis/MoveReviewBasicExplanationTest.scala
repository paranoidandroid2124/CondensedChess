package lila.commentary.analysis

import _root_.chess.{ Color, Knight, Pawn, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.analysis.semantic.RelationSurfaceRowKind
import lila.commentary.model.*
import lila.commentary.model.authoring.OutlineBeatKind
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine, VariationTag }
import munit.FunSuite

final class MoveReviewBasicExplanationTest extends FunSuite:

  private val italianBeforeBc4 =
    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

  private val italianOpening =
    OpeningReference(
      eco = Some("C50"),
      name = Some("Italian Game"),
      totalGames = 420000,
      topMoves = List(ExplorerMove("f1c4", "Bc4", 210000, 93000, 52000, 65000, 2460)),
      sampleGames = Nil
    )

  private def openingRef(name: String, eco: String, move: String, san: String): OpeningReference =
    OpeningReference(
      eco = Some(eco),
      name = Some(name),
      totalGames = 250000,
      topMoves = List(ExplorerMove(move, san, 120000, 52000, 31000, 37000, 2450)),
      sampleGames = Nil
    )

  private def developmentGoal: OpeningGoals.Evaluation =
    OpeningGoals.Evaluation(
      goalName = "Development Logic",
      status = OpeningGoals.Status.Achieved,
      supportedEvidence = List("Minor piece developed"),
      missingEvidence = Nil,
      confidence = 0.86
    )

  private def ctx(
      fen: String,
      playedMove: String,
      playedSan: String,
      phase: String = "Middlegame",
      ply: Int = 20,
      phaseReason: String,
      opening: Option[OpeningReference] = None,
      facts: List[Fact] = Nil,
      candidateFacts: List[Fact] = Nil,
      candidateMotifs: List[Motif] = Nil,
      openingGoalEvaluation: Option[OpeningGoals.Evaluation] = None,
      engineEvidence: Option[EngineEvidence] = None
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader(phase, "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(phaseReason, None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext(phase, phaseReason),
      candidates = List(
        CandidateInfo(
          move = playedSan,
          uci = Some(playedMove),
          annotation = "",
          planAlignment = phaseReason,
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None,
          facts = candidateFacts,
          lineMotifs = candidateMotifs
        )
      ),
      facts = facts,
      openingEvent = opening.map(ref => OpeningEvent.Intro(ref.eco.getOrElse(""), ref.name.getOrElse("Opening"), phaseReason, List(playedSan))),
      openingData = opening,
      openingGoalEvaluation =
        openingGoalEvaluation.orElse(Option.when(opening.exists(_.name.contains("Italian Game")) && playedMove == "f1c4")(developmentGoal)),
      engineEvidence = engineEvidence,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def italianCtx: NarrativeContext =
    ctx(
      fen = italianBeforeBc4,
      playedMove = "f1c4",
      playedSan = "Bc4",
      phase = "Opening",
      ply = 5,
      phaseReason = "Italian Game development",
      opening = Some(italianOpening)
    )

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String = "line_01"): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = lineId,
          scoreCp = 16,
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

  private def variationForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String): MoveReviewVariationRef =
    refsForLine(startFen, ucis, sans, lineId).variations.head

  test("grounded opening explanation requires validated PV proof") {
    assertEquals(MoveReviewExplanationBuilder.build(italianCtx, None), None)
    val explanation =
      MoveReviewExplanationBuilder
        .build(italianCtx, Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))))
        .getOrElse(fail("expected PV-proved opening goal explanation"))

    assertEquals(explanation.source, "opening_goal", clue(explanation))
    assert(explanation.title.contains("board-backed development goal"), clue(explanation.title))
    assert(!explanation.title.contains("Development Logic"), clue(explanation.title))
    assert(!explanation.prose.contains("Development Logic"), clue(explanation.prose))
    assert(explanation.prose.contains("Italian Game"), clue(explanation.prose))
    assert(!explanation.prose.contains("supports the explanation"), clue(explanation.prose))
    assert(!explanation.prose.contains("admit it by itself"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("opening_goal"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("development_logic"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("review_intent:normal_development"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("character_band:neutral"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("line_proof:opening_goal"), clue(explanation.reasonTags))
    assertEquals(explanation.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(explanation.shortLine))
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("quiet_development"), clue(explanation.pvInterpretation))
  }

  test("Ruy Lopez and Queen's Gambit opening explanations require exact PV-backed goals") {
    val ruyFen =
      "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val qgFen =
      "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"
    val ruy =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = ruyFen,
            playedMove = "f1b5",
            playedSan = "Bb5",
            phase = "Opening",
            ply = 5,
            phaseReason = "Ruy Lopez development",
            opening = Some(openingRef("Ruy Lopez", "C60", "f1b5", "Bb5")),
            openingGoalEvaluation = Some(developmentGoal)
          ),
          Some(refsForLine(ruyFen, List("f1b5", "a7a6", "b5a4"), List("Bb5", "a6", "Ba4")))
        )
        .getOrElse(fail("expected exact Ruy Lopez opening explanation"))
    val qg =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = qgFen,
            playedMove = "c2c4",
            playedSan = "c4",
            phase = "Opening",
            ply = 3,
            phaseReason = "Queen's Gambit center challenge",
            opening = Some(openingRef("Queen's Gambit", "D06", "c2c4", "c4")),
            openingGoalEvaluation = Some(
              OpeningGoals.Evaluation(
                goalName = "Center Challenge",
                status = OpeningGoals.Status.Achieved,
                supportedEvidence = List("queen-pawn tension"),
                missingEvidence = Nil,
                confidence = 0.82
              )
            )
          ),
          Some(refsForLine(qgFen, List("c2c4", "e7e6", "b1c3"), List("c4", "e6", "Nc3")))
        )
        .getOrElse(fail("expected exact Queen's Gambit opening explanation"))

    assertEquals(ruy.source, "opening_goal", clue(ruy))
    assert(ruy.pvInterpretation.exists(_.learningPoint.contains("Ba4")), clue(ruy.pvInterpretation))
    assertEquals(qg.source, "opening_goal", clue(qg))
    assertEquals(qg.pvInterpretation.map(_.linePurpose), Some("challenge_center"), clue(qg.pvInterpretation))
    assert(qg.reasonTags.contains("review_intent:normal_development"), clue(qg.reasonTags))
  }

  test("opening name alone does not admit opening prose without a grounded goal") {
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(
          fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          playedMove = "b1c3",
          playedSan = "Nc3",
          phase = "Opening",
          ply = 1,
          phaseReason = "Italian label without matching board requirements",
          opening = Some(openingRef("Italian Game", "C50", "b1c3", "Nc3"))
        ),
        None
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("PV-backed line-only opening context does not admit basic prose without opening-goal evidence") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(
          fen = fen,
          playedMove = "c7c5",
          playedSan = "c5",
          phase = "Opening",
          ply = 2,
          phaseReason = "Sicilian center challenge",
          opening = Some(openingRef("Sicilian Defense", "B20", "c7c5", "c5")),
          openingGoalEvaluation = None
        ),
        Some(refsForLine(fen, List("c7c5", "g1f3", "b8c6"), List("c5", "Nf3", "Nc6")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("strict local fact mode blocks soft line-backed opening descriptors") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(
          fen = fen,
          playedMove = "c7c5",
          playedSan = "c5",
          phase = "Opening",
          ply = 2,
          phaseReason = "Sicilian center challenge",
          opening = Some(openingRef("Sicilian Defense", "B20", "c7c5", "c5")),
          openingGoalEvaluation = None
        ),
        Some(refsForLine(fen, List("c7c5", "g1f3", "b8c6"), List("c5", "Nf3", "Nc6"))),
        strictLocalFacts = true
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("validated PV enriches an opening goal only through admitted scoped takeaway") {
    val explanation =
      MoveReviewExplanationBuilder
        .build(italianCtx, Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))))
        .getOrElse(fail("expected PV-backed opening explanation"))

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("quiet_development"), clue(explanation))
    assertEquals(explanation.pvInterpretation.map(_.tension), Some("scoped_local"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("normal_development")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("d3")), clue(explanation.pvInterpretation))
    assert(explanation.prose.contains("Nf6"), clue(explanation.prose))
    assert(explanation.prose.contains("d3"), clue(explanation.prose))
    assertEquals(explanation.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(explanation.shortLine))
  }

  test("outline and MoveReview consume the same precomputed opening goal evaluation") {
    val injectedGoal =
      OpeningGoals.Evaluation(
        goalName = "Injected Opening Goal",
        status = OpeningGoals.Status.Achieved,
        supportedEvidence = List("shared carrier"),
        missingEvidence = Nil,
        confidence = 0.99
      )
    val ctx = italianCtx.copy(openingGoalEvaluation = Some(injectedGoal))
    val headerText = BookStyleRenderer.validatedOutline(ctx).getBeat(OutlineBeatKind.MoveHeader).map(_.text).getOrElse("")
    val explanation =
      MoveReviewExplanationBuilder
        .build(ctx, Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))))
        .getOrElse(fail("expected opening explanation"))

    assert(headerText.contains("Injected Opening Goal"), clue(headerText))
    assert(explanation.factFragments.toList.flatten.collectFirst {
      case fragment: FactFragment.OpeningGoalFragment => fragment.goalName
    }.contains("Injected Opening Goal"), clue(explanation.factFragments))
    assert(!explanation.title.contains("Injected Opening Goal"), clue(explanation.title))
    assert(!explanation.prose.contains("Injected Opening Goal"), clue(explanation.prose))
  }

  test("tactical fork fact admits PV-backed basic prose outside opening") {
    val fen = "4k3/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
    val forkFact =
      Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.CandidatePv)
    val forkMotif =
      Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 0, Some("Nf5"))
    val explanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = fen,
            playedMove = "d4f5",
            playedSan = "Nf5",
            phaseReason = "fork evidence",
            candidateFacts = List(forkFact),
            candidateMotifs = List(forkMotif)
          ),
          Some(refsForLine(fen, List("d4f5", "h4g5"), List("Nf5", "Qg5")))
        )
        .getOrElse(fail("expected fact-backed tactical explanation"))

    assertEquals(explanation.source, "canonical_fact", clue(explanation))
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("create_tactical_threat"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("fork")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("creates_threat")), clue(explanation.pvInterpretation))
    assert(explanation.reasonTags.contains("review_intent:creates_threat"), clue(explanation.reasonTags))
    assert(explanation.prose.toLowerCase.contains("fork"), clue(explanation.prose))
  }

  test("forced-line truth admits a PV-coupled tactical local fact") {
    val fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1"
    val lineUcis = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5")
    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          ctx(
            fen = fen,
            playedMove = "d3h7",
            playedSan = "Bxh7+",
            phaseReason = "forced tactical line",
            engineEvidence = Some(
              EngineEvidence(
                depth = 18,
                variations = List(
                  VariationLine(
                    moves = lineUcis,
                    scoreCp = 120,
                    mate = None,
                    depth = 18,
                    tags = List(VariationTag.Forced)
                  )
                )
              )
            )
          ),
          Some(refsForLine(fen, lineUcis, List("Bxh7+", "Kxh7", "Ng5+", "Kg8", "Qh5")))
        )
        .getOrElse(fail("expected forced-line local fact"))

    assertEquals(result.explanation.source, "forced_line_truth", clue(result.explanation))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.ForcedLineTruth, clue(result.localFact))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Threat, clue(result.localFact))
    assertEquals(result.localFact.lineBinding, MoveReviewLocalFact.LineBinding.PvCoupled, clue(result.localFact))
    assert(result.localFact.evidenceRefs.contains("forced_line_theme:greek_gift"), clue(result.localFact.evidenceRefs))
  }

  test("stale motif ownership can be replaced only by a replayed tactical relation witness") {
    val fen = "4k3/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
    val leakedFork =
      Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.CandidatePv)
    val wrongPlyMotif =
      Motif.Fork(Knight, List(Rook, Queen), Square.F5, List(Square.E7, Square.H4), Color.White, 1, Some("Nf5"))
    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          ctx(
            fen = fen,
            playedMove = "d4f5",
            playedSan = "Nf5",
            phaseReason = "leaked fork evidence",
            candidateFacts = List(leakedFork),
            candidateMotifs = List(wrongPlyMotif)
          ),
          Some(refsForLine(fen, List("d4f5", "h4g5"), List("Nf5", "Qg5")))
        )
        .getOrElse(fail("expected replayed relation witness to replace stale motif ownership"))

    assertEquals(result.explanation.source, "relation_witness", clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Threat, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.RelationWitness, clue(result.localFact))
    assertEquals(result.localFact.relationSurface, Some(RelationSurfaceRowKind.TacticalRelation), clue(result.localFact))
    assert(result.explanation.reasonTags.contains("relation_kind:fork"), clue(result.explanation.reasonTags))
  }

  test("same tactical-looking PV text does not admit prose without canonical evidence") {
    val fen = "4k3/4r3/8/8/3N3q/8/8/6K1 w - - 0 1"
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "d4f5", "Nf5", phaseReason = "fork text without fact"),
        Some(refsForLine(fen, List("d4f5", "h4g5"), List("Nf5", "Qg5")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("target-piece fact creates target pressure and does not claim defensive answer") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val targetFact =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val explanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = fen,
            playedMove = "d1h5",
            playedSan = "Qh5",
            phaseReason = "direct target evidence",
            candidateFacts = List(targetFact)
          ),
          Some(refsForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+")))
        )
        .getOrElse(fail("expected direct-threat explanation"))

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("create_tactical_threat"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("direct_threat")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("creates_threat")), clue(explanation.pvInterpretation))
    assert(!explanation.pvInterpretation.exists(_.confirms.contains("answers_threat")), clue(explanation.pvInterpretation))
    assert(explanation.reasonTags.contains("review_intent:creates_threat"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("local_fact_family:pressure"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("local_fact_authority:canonical_fact"), clue(explanation.reasonTags))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("g6")), clue(explanation.pvInterpretation))
    assert(explanation.prose.contains("concrete local target"), clue(explanation.prose))
    assert(!explanation.prose.contains("puts pressure"), clue(explanation.prose))
    assert(!explanation.prose.contains("creates a concrete target"), clue(explanation.prose))
    assert(!explanation.prose.contains("first answer to the target"), clue(explanation.prose))
    assert(!explanation.prose.contains("creates the verified target pattern"), clue(explanation.prose))
    assert(!explanation.prose.contains("first reply"), clue(explanation.prose))
    assert(!explanation.prose.contains("tests the point"), clue(explanation.prose))
    assert(!explanation.prose.contains("target evidence"), clue(explanation.prose))
    assert(!explanation.prose.contains("shown by the local evidence"), clue(explanation.prose))
  }

  test("strict local fact mode keeps only played-move-owned target pressure") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val ownedTarget =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val unownedTarget =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.F3), Nil, FactScope.CandidatePv)

    val owned =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "d1h5", "Qh5", phaseReason = "owned target evidence", candidateFacts = List(ownedTarget)),
        Some(refsForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+"))),
        strictLocalFacts = true
      )
    val unowned =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "d1h5", "Qh5", phaseReason = "unowned target evidence", candidateFacts = List(unownedTarget)),
        Some(refsForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+"))),
        strictLocalFacts = true
      )

    assert(owned.exists(_.reasonTags.contains("local_fact_family:pressure")), clue(owned))
    assertEquals(unowned, None, clue(unowned))
  }

  test("only-move defense truth admits answers-threat only with coupled PV proof") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val targetFact =
      Fact.TargetPiece(Square.E4, Pawn, List(Square.F3), Nil, FactScope.ThreatLine)
    val defenseContract =
      DecisiveTruthContract(
        playedMove = Some("d2d3"),
        verifiedBestMove = Some("d2d3"),
        truthClass = DecisiveTruthClass.Best,
        cpLoss = 0,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
        allowConcreteBenchmark = false,
        chosenMatchesBest = true,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = true,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val noPv =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "d2d3", "d3", phase = "Opening", ply = 5, phaseReason = "defensive support", facts = List(targetFact)),
        None,
        Some(defenseContract)
      )
    val explanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(fen, "d2d3", "d3", phase = "Opening", ply = 5, phaseReason = "defensive support", facts = List(targetFact)),
          Some(refsForLine(fen, List("d2d3", "g8f6", "f1e2"), List("d3", "Nf6", "Be2"))),
          Some(defenseContract)
        )
        .getOrElse(fail("expected PV-proved defensive answer"))

    assertEquals(noPv, None)
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("answer_direct_threat"), clue(explanation))
    assert(explanation.reasonTags.contains("review_intent:answers_threat"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("local_fact_family:defense"), clue(explanation.reasonTags))
    assert(!explanation.prose.contains("addresses the immediate threat"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("line_proof:defensive_answer"), clue(explanation.reasonTags))
  }

  test("move-order or mobility relation preemption does not hide only-move defensive target truth") {
    val fen = "n6k/8/B7/1K6/8/8/8/2R5 w - - 0 1"
    val targetFact =
      Fact.TargetPiece(Square.A8, Knight, List(Square.B7), Nil, FactScope.ThreatLine)
    val defenseContract =
      DecisiveTruthContract(
        playedMove = Some("a6b7"),
        verifiedBestMove = Some("a6b7"),
        truthClass = DecisiveTruthClass.Best,
        cpLoss = 0,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
        allowConcreteBenchmark = false,
        chosenMatchesBest = true,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = true,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val context =
      ctx(
        fen = fen,
        playedMove = "a6b7",
        playedSan = "Bb7",
        phaseReason = "defensive move that also dominates a target",
        facts = List(targetFact),
        engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = List("a6b7", "h8g8"), scoreCp = 42))))
      ).copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("a8")),
            logicSummary = "Control the knight's escape squares.",
            delta = PVDelta(Nil, Nil, Nil, Nil),
            confidence = ConfidenceLevel.Engine
          )
        )
      )

    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          context,
          Some(refsForLine(fen, List("a6b7", "h8g8"), List("Bb7", "Kg8"))),
          Some(defenseContract),
          strictLocalFacts = true
        )
        .getOrElse(fail("expected defensive target truth to survive relation preemption"))

    assertEquals(result.explanation.source, "canonical_fact", clue(result.explanation))
    assertEquals(result.explanation.pvInterpretation.map(_.linePurpose), Some("answer_direct_threat"), clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Defense, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.DefensiveTruth, clue(result.localFact))
    assertEquals(result.localFact.authority, MoveReviewLocalFact.Source.OnlyMoveDefense.authority, clue(result.localFact))
  }

  test("castling explanation requires exact PV-backed king-safety sequencing") {
    val fen =
      "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val noPv =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "e1g1", "O-O", phase = "Opening", ply = 7, phaseReason = "castle without PV"),
        None
      )
    val explanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(fen, "e1g1", "O-O", phase = "Opening", ply = 7, phaseReason = "king safety with PV"),
          Some(refsForLine(fen, List("e1g1", "f8e7", "f1e1"), List("O-O", "Be7", "Re1")))
        )
        .getOrElse(fail("expected exact castle explanation"))

    assertEquals(noPv, None)
    assertEquals(explanation.source, "basic_move_explanation", clue(explanation))
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("king_safety_first"), clue(explanation.pvInterpretation))
    assert(explanation.reasonTags.contains("review_intent:king_safety"), clue(explanation.reasonTags))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("king-safety")), clue(explanation.pvInterpretation))

    val aliasExplanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(fen, "e1h1", "O-O", phase = "Opening", ply = 7, phaseReason = "king safety with castling alias"),
          Some(refsForLine(fen, List("e1g1", "f8e7", "f1e1"), List("O-O", "Be7", "Re1")))
        )
        .getOrElse(fail("expected castling alias to normalize into exact castle explanation"))

    assertEquals(aliasExplanation.source, "basic_move_explanation", clue(aliasExplanation))
    assertEquals(aliasExplanation.pvInterpretation.map(_.linePurpose), Some("king_safety_first"), clue(aliasExplanation.pvInterpretation))
  }

  test("endgame facts admit activity prose without an endgame idea catalog") {
    val fen = "8/8/8/8/8/8/4P3/4K2k w - - 0 1"
    val kingActivity =
      Fact.KingActivity(Square.D2, mobility = 5, proximityToCenter = 1, FactScope.CandidatePv)
    val explanation =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = fen,
            playedMove = "e1d2",
            playedSan = "Kd2",
            phase = "Endgame",
            ply = 60,
            phaseReason = "king activity evidence",
            candidateFacts = List(kingActivity)
          ),
          Some(refsForLine(fen, List("e1d2", "h1g2", "e2e4"), List("Kd2", "Kg2", "e4")))
        )
        .getOrElse(fail("expected endgame fact explanation"))

    assertEquals(explanation.source, "canonical_fact", clue(explanation))
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("improve_endgame_activity"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("king_activity")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("improves_endgame_activity")), clue(explanation.pvInterpretation))
    assert(explanation.reasonTags.contains("review_intent:improves_endgame_activity"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("local_fact_family:endgame"), clue(explanation.reasonTags))
    assert(explanation.prose.toLowerCase.contains("king-activity detail"), clue(explanation.prose))
  }

  test("endgame fact must be owned by the played move before basic prose is admitted") {
    val fen = "8/8/8/8/8/8/4P1B1/4K2k w - - 0 1"
    val kingActivity =
      Fact.KingActivity(Square.D2, mobility = 5, proximityToCenter = 1, FactScope.CandidatePv)
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(
          fen = fen,
          playedMove = "g2h3",
          playedSan = "Bh3",
          phase = "Endgame",
          ply = 60,
          phaseReason = "unowned king activity evidence",
          candidateFacts = List(kingActivity)
        ),
        Some(refsForLine(fen, List("g2h3", "h1g2", "e1d2"), List("Bh3", "Kg2", "Kd2"))),
        strictLocalFacts = true
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("passed-pawn and rook endgame activity require exact facts plus legal PV") {
    val passedPawnFen =
      "8/4k3/8/8/8/8/4P3/4K3 w - - 0 1"
    val rookFen =
      "4k3/8/8/8/8/8/R3P3/4K3 w - - 0 1"
    val passedPawn =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = passedPawnFen,
            playedMove = "e2e4",
            playedSan = "e4",
            phase = "Endgame",
            ply = 60,
            phaseReason = "passed pawn support",
            candidateFacts = List(Fact.PawnPromotion(Square.E4, promotedTo = None, FactScope.CandidatePv))
          ),
          Some(refsForLine(passedPawnFen, List("e2e4", "e7d6", "e1d2"), List("e4", "Kd6", "Kd2")))
        )
        .getOrElse(fail("expected exact passed-pawn endgame explanation"))
    val rookActivity =
      MoveReviewExplanationBuilder
        .build(
          ctx(
            fen = rookFen,
            playedMove = "a2a7",
            playedSan = "Ra7",
            phase = "Endgame",
            ply = 60,
            phaseReason = "rook activity",
            candidateFacts = List(Fact.RookEndgamePattern("RookBehindPassedPawn", FactScope.CandidatePv))
          ),
          Some(refsForLine(rookFen, List("a2a7", "e8d8", "a7a8"), List("Ra7", "Kd8", "Ra8")))
        )
        .getOrElse(fail("expected exact rook endgame explanation"))

    assertEquals(passedPawn.pvInterpretation.map(_.linePurpose), Some("improve_endgame_activity"), clue(passedPawn))
    assert(passedPawn.reasonTags.contains("pawn_promotion"), clue(passedPawn.reasonTags))
    assertEquals(rookActivity.pvInterpretation.map(_.linePurpose), Some("improve_endgame_activity"), clue(rookActivity))
    assert(rookActivity.reasonTags.contains("rook_endgame_pattern"), clue(rookActivity.reasonTags))
    assert(rookActivity.pvInterpretation.exists(_.learningPoint.contains("Ra8")), clue(rookActivity.pvInterpretation))
  }

  test("phase-only endgame move stays closed without exact facts") {
    val fen = "8/8/8/8/8/8/4P3/4K2k w - - 0 1"
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(fen, "e1d2", "Kd2", phase = "Endgame", ply = 60, phaseReason = "phase-only king move"),
        Some(refsForLine(fen, List("e1d2", "h1g2", "e2e4"), List("Kd2", "Kg2", "e4")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("capture can admit line consequence when the checked line owns a material transition") {
    val fen =
      "r2qk2r/pp3ppp/2nBpn2/3p1b2/3P4/2P2N1P/PP2BPP1/RN1QK2R b KQkq - 0 9"
    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          ctx(fen, "d8d6", "Qxd6", phaseReason = "capture with checked material transition"),
          Some(
            refsForLine(
              fen,
              List("d8d6", "e1g1", "b7b6", "b1d2", "e8g8", "e2b5"),
              List("Qxd6", "O-O", "b6", "Nbd2", "O-O", "Bb5")
            )
          )
        )
        .getOrElse(fail("expected capture-backed line consequence"))
    val explanation = result.explanation

    assertEquals(explanation.source, "line_consequence", clue(explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.LineConsequence, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.LineConsequence, clue(result.localFact))
    assertEquals(result.localFact.lineBinding, MoveReviewLocalFact.LineBinding.PvCoupled, clue(result.localFact))
    assert(explanation.reasonTags.contains("line_consequence_kind:material_transition"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("line_consequence_trigger_san:Qxd6"), clue(explanation.reasonTags))
    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("clarify_exchange"), clue(explanation.pvInterpretation))
  }

  test("relation witness can come from a validated played-first ref line outside the engine top line") {
    val fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1"
    val context =
      ctx(fen, "f5d6", "Nd6", phaseReason = "interference against a defender")
        .copy(
          decision = Some(
            DecisionRationale(
              focalPoint = Some(TargetSquare("d5")),
              logicSummary = "Block the rook's defense of the queen.",
              delta = PVDelta(Nil, Nil, Nil, Nil),
              confidence = ConfidenceLevel.Engine
            )
          ),
          engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = List("d1a4"), scoreCp = 0))))
        )

    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          context,
          Some(refsForLine(fen, List("f5d6", "a8b8"), List("Nd6", "Kb8"))),
          strictLocalFacts = true
        )
        .getOrElse(fail("expected relation witness from checked ref line"))

    assertEquals(result.explanation.source, "relation_witness", clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Pressure, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.RelationWitness, clue(result.localFact))
    assertEquals(result.localFact.lineBinding, MoveReviewLocalFact.LineBinding.PvCoupled, clue(result.localFact))
    assertEquals(result.localFact.relationSurface, Some(RelationSurfaceRowKind.LineGeometry), clue(result.localFact))
    assert(result.explanation.reasonTags.contains("relation_kind:interference"), clue(result.explanation.reasonTags))
    assert(result.explanation.reasonTags.contains("line_move:f5d6"), clue(result.explanation.reasonTags))
    assert(result.localFact.guardrails.contains("fen_validated_line_replayed"), clue(result.localFact.guardrails))
  }

  test("mobility-restriction relation witness becomes a pressure local fact") {
    val fen = "n6k/8/B7/1K6/8/8/8/2R5 w - - 0 1"
    val context =
      ctx(
        fen = fen,
        playedMove = "a6b7",
        playedSan = "Bb7",
        phaseReason = "domination against a knight",
        engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = List("a6b7", "h8g8"), scoreCp = 42))))
      ).copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("a8")),
            logicSummary = "Control the knight's escape squares.",
            delta = PVDelta(Nil, Nil, Nil, Nil),
            confidence = ConfidenceLevel.Engine
          )
        )
      )

    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          context,
          Some(refsForLine(fen, List("a6b7", "h8g8"), List("Bb7", "Kg8"))),
          strictLocalFacts = true
        )
        .getOrElse(fail("expected mobility-restriction relation witness"))

    assertEquals(result.explanation.source, "relation_witness", clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Pressure, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.RelationWitness, clue(result.localFact))
    assertEquals(result.localFact.relationSurface, Some(RelationSurfaceRowKind.MobilityRestriction), clue(result.localFact))
    assert(result.explanation.reasonTags.contains("relation_kind:domination"), clue(result.explanation.reasonTags))
    assert(result.explanation.reasonTags.contains("review_intent:restricts_mobility"), clue(result.explanation.reasonTags))
    assertEquals(result.explanation.pvInterpretation.map(_.linePurpose), Some("restrict_piece_mobility"), clue(result.explanation))
    assert(result.explanation.pvInterpretation.exists(_.confirms.contains("mobility_restriction")), clue(result.explanation))
    assert(result.explanation.prose.contains("mobility restriction"), clue(result.explanation.prose))
  }

  test("move-order relation witness becomes a timing local fact") {
    val fen = "4k3/8/8/8/3b4/5N2/8/3QK3 w - - 0 1"
    val context =
      ctx(
        fen = fen,
        playedMove = "d1a4",
        playedSan = "Qa4+",
        phaseReason = "in-between check before recapture",
        engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = List("d1a4", "e8f8"), scoreCp = 42))))
      ).copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("d4")),
            logicSummary = "Insert a check before the expected recapture.",
            delta = PVDelta(Nil, Nil, Nil, Nil),
            confidence = ConfidenceLevel.Engine
          )
        )
      )

    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          context,
          Some(refsForLine(fen, List("d1a4", "e8f8"), List("Qa4+", "Kf8"))),
          strictLocalFacts = true
        )
        .getOrElse(fail("expected move-order relation witness"))
    val typed = MoveReviewCausalClaim.localFactResultTypedEvidences(Some(result))

    assertEquals(result.explanation.source, "relation_witness", clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Timing, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.RelationWitness, clue(result.localFact))
    assertEquals(result.localFact.relationSurface, Some(RelationSurfaceRowKind.MoveOrder), clue(result.localFact))
    assert(result.explanation.reasonTags.contains("relation_kind:zwischenzug"), clue(result.explanation.reasonTags))
    assert(result.explanation.reasonTags.contains("review_intent:times_move_order"), clue(result.explanation.reasonTags))
    assertEquals(result.explanation.pvInterpretation.map(_.linePurpose), Some("move_order_timing"), clue(result.explanation))
    assert(typed.exists(_.kind == MoveReviewCausalClaim.EvidenceKind.TimingWitness), clues(typed))
    assert(typed.flatMap(_.relationSurface).contains(RelationSurfaceRowKind.MoveOrder), clues(typed))
  }

  test("draw-resource relation witness becomes a defense local fact in the main builder path") {
    val fen = "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1"
    val line = List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1")
    val context =
      ctx(
        fen = fen,
        playedMove = "h4e1",
        playedSan = "Qe1+",
        phaseReason = "perpetual-check resource in a checked line",
        engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = line, scoreCp = 0))))
      )

    val result =
      MoveReviewExplanationBuilder
        .buildWithLocalFact(
          context,
          Some(refsForLine(fen, line, List("Qe1+", "Kh2", "Qh4+", "Kg1", "Qe1+")))
        )
        .getOrElse(fail("expected draw-resource relation witness"))
    val typed = MoveReviewCausalClaim.localFactResultTypedEvidences(Some(result))

    assertEquals(result.explanation.source, "relation_witness", clue(result.explanation))
    assertEquals(result.localFact.family, MoveReviewLocalFact.Family.Defense, clue(result.localFact))
    assertEquals(result.localFact.producer, MoveReviewLocalFact.Producer.RelationWitness, clue(result.localFact))
    assertEquals(result.localFact.lineBinding, MoveReviewLocalFact.LineBinding.PvCoupled, clue(result.localFact))
    assertEquals(result.localFact.relationSurface, Some(RelationSurfaceRowKind.DrawResource), clue(result.localFact))
    assert(result.explanation.reasonTags.contains("relation_kind:perpetual_check"), clue(result.explanation.reasonTags))
    assert(result.explanation.reasonTags.contains("relation_fact:draw_resource"), clue(result.explanation.reasonTags))
    assert(result.localFact.guardrails.contains("relation_surface:draw_resource"), clue(result.localFact.guardrails))
    assert(result.explanation.prose.contains("perpetual-check resource"), clue(result.explanation.prose))
    assert(QuestionFirstCommentaryPlanner.localFactResultWhyThisEligible(result), clue(result.localFact))
    assert(typed.exists(_.kind == MoveReviewCausalClaim.EvidenceKind.Defense), clues(typed))
    assert(typed.flatMap(_.relationSurface).contains(RelationSurfaceRowKind.DrawResource), clues(typed))
  }

  test("non-tactical relation witness still waits for the strict fallback gate") {
    val fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1"
    val context =
      ctx(fen, "f5d6", "Nd6", phaseReason = "interference against a defender")
        .copy(
          decision = Some(
            DecisionRationale(
              focalPoint = Some(TargetSquare("d5")),
              logicSummary = "Block the rook's defense of the queen.",
              delta = PVDelta(Nil, Nil, Nil, Nil),
              confidence = ConfidenceLevel.Engine
            )
          ),
          engineEvidence = Some(EngineEvidence(depth = 16, variations = List(VariationLine(moves = List("d1a4"), scoreCp = 0))))
        )

    val result =
      MoveReviewExplanationBuilder.buildWithLocalFact(
        context,
        Some(refsForLine(fen, List("f5d6", "a8b8"), List("Nd6", "Kb8")))
      )

    assertEquals(result, None, clue(result))
  }

  test("PV interpretation is omitted for invalid line while shortLine remains visible") {
    val valid = refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))
    val corruptedMove = valid.variations.head.moves(1).copy(fenAfter = valid.variations.head.moves.head.fenAfter)
    val corrupted =
      valid.copy(variations = List(valid.variations.head.copy(moves = valid.variations.head.moves.updated(1, corruptedMove))))

    val explanation = MoveReviewExplanationBuilder.build(italianCtx, Some(corrupted))

    assertEquals(explanation, None, clue(explanation))
    assertEquals(MoveReviewPvLine.shortLine(Some(corrupted), None).map(_.san), Some(List("Bc4", "Nf6", "d3")))
  }

  test("illegal current move creates no basic move explanation") {
    val explanation =
      MoveReviewExplanationBuilder.build(
        ctx(
          fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          playedMove = "e2e5",
          playedSan = "e5",
          phase = "Opening",
          ply = 1,
          phaseReason = "illegal pawn jump",
          opening = Some(openingRef("King's Pawn Game", "C20", "e2e5", "e5"))
        ),
        None
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("shortLine follows the same coupled PV line as pvInterpretation") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val targetFact =
      Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.CandidatePv)
    val refs =
      MoveReviewRefs(
        startFen = fen,
        startPly = 3,
        variations = List(
          variationForLine(fen, List("g1f3", "g8f6"), List("Nf3", "Nf6"), "line_uncoupled"),
          variationForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+"), "line_coupled")
        )
      )
    val explanation =
      MoveReviewExplanationBuilder
        .build(ctx(fen, "d1h5", "Qh5", phaseReason = "direct target evidence", candidateFacts = List(targetFact)), Some(refs))
        .getOrElse(fail("expected direct-threat explanation"))

    assertEquals(explanation.pvInterpretation.flatMap(_.supportedByLineId), Some("line_coupled"), clue(explanation.pvInterpretation))
    assertEquals(explanation.shortLine.flatMap(_.lineId), Some("line_coupled"), clue(explanation.shortLine))
  }

  test("basic move explanation is admitted before exact factual fallback") {
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val slots = MoveReviewPolishSlotsBuilder.buildOrFallback(
      italianCtx,
      outline,
      refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
      strategyPack = None,
      truthContract = None
    )

    assertEquals(slots.sourceKind, "basic_move_explanation", clue(slots))
    assert(slots.claim.contains("Italian Game"), clue(slots.claim))
    assertNotEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This puts the bishop on c4.",
      clue(slots.claim)
    )
  }
