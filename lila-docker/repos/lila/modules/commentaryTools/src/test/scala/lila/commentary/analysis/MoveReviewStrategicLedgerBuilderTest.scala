package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.{ DecisionComparisonDigest, MoveReviewMoveRef, MoveReviewRefs, MoveReviewVariationRef, NarrativeSignalDigest, StrategyPack, StrategyPieceRoute }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.ProbeResult
import lila.commentary.model.authoring.{ EvidenceBranch, QuestionEvidence }
import lila.commentary.model.strategic.{ EngineEvidence, PlanContinuity, PlanLifecyclePhase, VariationLine }
import _root_.chess.Color

class MoveReviewStrategicLedgerBuilderTest extends FunSuite:

  private def maybeBuild(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      refs: Option[MoveReviewRefs] = None,
      probeResults: List[ProbeResult] = Nil,
      planStateToken: Option[PlanStateTracker] = None,
      lineConsequence: Option[LineConsequenceEvidence] = None
  ) =
    MoveReviewStrategicLedgerBuilder.build(
      ctx = ctx,
      strategyPack = strategyPack,
      refs = refs,
      probeResults = probeResults,
      planStateToken = planStateToken,
      endgameStateToken = None,
      lineConsequence = lineConsequence
    )

  private def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      refs: Option[MoveReviewRefs] = None,
      probeResults: List[ProbeResult] = Nil,
      planStateToken: Option[PlanStateTracker] = None,
      lineConsequence: Option[LineConsequenceEvidence] = None
  ) =
    maybeBuild(
      ctx = ctx,
      strategyPack = strategyPack,
      refs = refs,
      probeResults = probeResults,
      planStateToken = planStateToken,
      lineConsequence = lineConsequence
    ).getOrElse(fail("missing ledger"))

  test("maps rook-pawn themes onto the rook_pawn_march motif") {
    val routePack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            side = "White",
            piece = "Rook",
            from = "h1",
            route = List("h3", "g3"),
            purpose = "support the h-pawn lever",
            confidence = 0.83
          )
        )
      )
    val ledger = build(MoveReviewProseGoldenFixtures.rookPawnMarch.ctx, strategyPack = Some(routePack))
    assertEquals(ledger.motifKey, "rook_pawn_march")
    assertEquals(ledger.motifLabel, "Rook-pawn march")
  }

  test("classifies prophylactic fixtures as restrain") {
    val ledger = build(MoveReviewProseGoldenFixtures.prophylacticCut.ctx)
    assertEquals(ledger.stageKey, "restrain")
    assert(ledger.stageReason.exists(_.toLowerCase.contains("counterplay")))
  }

  test("classifies compensation fixtures as convert") {
    val ledger = build(MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx)
    assertEquals(ledger.stageKey, "convert")
    assertEquals(ledger.motifKey, "compensation_attack")
    assertEquals(ledger.conversionTrigger, Some("Mating Attack"))
  }

  test("compensation ledger stays aligned with surfaced execution and objective") {
    val fixture = MoveReviewProseGoldenFixtures.exchangeSacrifice
    val ledger = build(fixture.ctx, strategyPack = fixture.strategyPack)
    val surface = StrategyPackSurface.from(fixture.strategyPack)
    assertEquals(ledger.motifKey, "compensation_attack")
    assert(surface.executionText.exists(_.trim.nonEmpty), clue(surface))
    assert(surface.objectiveText.exists(_.trim.nonEmpty), clue(surface))
  }

  test("selects opposite_bishops_conversion when conversion signals and plan family align") {
    val ledger = build(MoveReviewProseGoldenFixtures.oppositeBishopsConversion.ctx)
    assertEquals(ledger.motifKey, "opposite_bishops_conversion")
    assertEquals(ledger.stageKey, "convert")
  }

  test("does not upgrade opposite-color bishops without a conversion plan") {
    val ctx =
      MoveReviewProseGoldenFixtures.oppositeBishopsConversion.ctx.copy(
        mainStrategicPlans = Nil,
        planContinuity = None
      )
    val ledger = build(ctx, refs = Some(sampleRefs(ctx.fen)))
    assertEquals(ledger.motifKey, "color_complex")
  }

  test("prefers probe-backed primary lines and caps them at four SAN moves") {
    val legalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val probe =
      ProbeResult(
        id = "probe-rook-pawn",
        fen = Some(legalFen),
        evalCp = 28,
        bestReplyPv = List("e7e5", "g1f3", "b8c6", "f1b5"),
        deltaVsBaseline = 12,
        keyMotifs = List("rook_pawn_march"),
        purpose = Some("free_tempo_branches"),
        probedMove = Some("e2e4"),
        depth = Some(20)
      )
    val ledger =
      build(
        MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(fen = legalFen),
        probeResults = List(probe)
      )
    val primary = ledger.primaryLine.getOrElse(fail("missing primary line"))
    assertEquals(primary.source, "probe")
    assertEquals(primary.sanMoves.length, 4)
  }

  test("probe ledger notes do not project request-derived purpose or objective") {
    val legalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val rawPurpose = "raw request purpose text"
    val rawObjective = "raw request objective text"
    val probe =
      ProbeResult(
        id = "probe-rook-pawn",
        fen = Some(legalFen),
        evalCp = 28,
        bestReplyPv = List("e7e5", "g1f3"),
        deltaVsBaseline = 12,
        keyMotifs = List("rook_pawn_march"),
        purpose = Some(rawPurpose),
        objective = Some(rawObjective),
        probedMove = Some("e2e4"),
        depth = Some(20)
      )
    val ledger =
      build(
        MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(fen = legalFen),
        probeResults = List(probe)
      )
    val note = ledger.primaryLine.flatMap(_.note).getOrElse("")

    assert(!note.contains(rawPurpose), clue(note))
    assert(!note.contains(rawObjective), clue(note))
    assert(note.contains("12cp vs baseline"), clue(note))
  }

  test("falls back to decision-compare engine pv when no supportive probe exists") {
    val legalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ctx =
      MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        fen = legalFen,
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("e2e4", "e7e5", "g1f3", "b8c6"),
                scoreCp = 34,
                depth = 18
              )
            )
          )
        )
      )
    val ledger = build(ctx)
    val primary = ledger.primaryLine.getOrElse(fail("missing decision line"))
    assertEquals(primary.source, "decision_compare")
    assert(primary.sanMoves.nonEmpty)
  }

  test("decision-compare ledger note uses typed replayed line consequence when available") {
    val legalFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx =
      MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        fen = legalFen,
        playedMove = Some("g1f3"),
        playedSan = Some("Nf3"),
        engineEvidence = Some(
          EngineEvidence(
            depth = 20,
            variations = List(VariationLine(ucis, scoreCp = 42, depth = 20))
          )
        )
      )
    val ledger =
      build(
        ctx,
        refs = Some(replayedRefs(legalFen, "exchange", ucis, List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6")))
      )
    val primary = ledger.primaryLine.getOrElse(fail("missing decision line"))

    assertEquals(primary.source, "decision_compare")
    assert(primary.note.exists(_.toLowerCase.contains("exchange sequence")), clue(primary))
    assert(!primary.note.exists(_.contains("line_consequence:")), clue(primary))
  }

  test("decision-compare ledger line uses precomputed surface line consequence before a shallow engine path") {
    val fen = "r1bqkbnr/ppp1p1pp/2n5/3pP3/3P4/8/PPP3PP/RNBQKBNR b KQkq - 0 5"
    val shallowUcis = List("e7e6", "g1f3")
    val consequenceUcis = List("e7e6", "g1f3", "g8h6", "f1d3", "c6b4", "c1h6", "b4d3", "d1d3")
    val consequenceSans = List("e6", "Nf3", "Nh6", "Bd3", "Nb4", "Bxh6", "Nxd3+", "Qxd3")
    val refs =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).map(_ + 1).getOrElse(1),
        variations =
          replayedRefs(fen, "line_01", shallowUcis, List("e6", "Nf3")).variations ++
            replayedRefs(fen, "line_04", consequenceUcis, consequenceSans).variations
      )
    val consequence =
      LineConsequenceEvidence(
        lineId = Some("line_04"),
        sanMoves = consequenceSans,
        uciMoves = consequenceUcis,
        scoreCp = Some(180),
        mate = None,
        depth = Some(10),
        windowPly = 8,
        kind = LineConsequenceKind.ExchangeSequence,
        triggerSan = Some("Bxh6"),
        consequence = "this exchange sequence trades the bishop for the knight on h6",
        whyItMatters = Some("leaving Black with a backward pawn target on e6"),
        release = LineConsequenceRelease.SurfaceCandidate,
        rejectReasons = Nil
      )
    val ctx =
      MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        fen = fen,
        playedMove = Some("e7e6"),
        playedSan = Some("e6"),
        engineEvidence = Some(
          EngineEvidence(
            depth = 10,
            variations = List(VariationLine(shallowUcis, scoreCp = 80, depth = 10))
          )
        )
      )

    val ledger = build(ctx, refs = Some(refs), lineConsequence = Some(consequence))
    val primary = ledger.primaryLine.getOrElse(fail("missing decision line"))

    assertEquals(primary.source, "decision_compare")
    assertEquals(primary.sanMoves, consequenceSans.take(4))
    assert(primary.note.exists(_.contains("backward pawn target on e6")), clue(primary))
  }

  test("decision-compare ledger note can use replay-backed engine consequence without refs") {
    val legalFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx =
      MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        fen = legalFen,
        playedMove = Some("g1f3"),
        playedSan = Some("Nf3"),
        engineEvidence = Some(
          EngineEvidence(
            depth = 20,
            variations = List(VariationLine(ucis, scoreCp = 42, depth = 20))
          )
        )
      )
    val ledger = build(ctx, refs = None)
    val primary = ledger.primaryLine.getOrElse(fail("missing decision line"))

    assertEquals(primary.source, "decision_compare")
    assert(primary.note.exists(_.toLowerCase.contains("exchange sequence")), clue(primary))
    assert(!primary.note.exists(_.contains("line_consequence:")), clue(primary))
  }

  test("raw strategyPack signal digest decision does not create player ledger lines") {
    val rawEvidence = "raw signal digest evidence text"
    val pack =
      StrategyPack(
        sideToMove = "white",
        signalDigest =
          Some(
            NarrativeSignalDigest(
              decisionComparison =
                Some(
                  DecisionComparisonDigest(
                    engineBestPv = List("Nf3", "Nc6"),
                    engineBestScoreCp = Some(20),
                    evidence = Some(rawEvidence)
                  )
                )
            )
          )
      )
    val ledger =
      maybeBuild(
        MoveReviewProseGoldenFixtures.rookPawnMarch.ctx,
        strategyPack = Some(pack)
      )
    val rendered =
      ledger.toList.flatMap { value =>
        List(value.primaryLine, value.resourceLine).flatten.flatMap(line =>
          List(line.title, line.source) ++ line.note.toList ++ line.sanMoves
        )
      }.mkString(" ")

    assert(!rendered.contains(rawEvidence), clue(rendered))
    assert(!rendered.contains("decision_compare"), clue(rendered))
  }

  test("prefers authoring branches for the resource line") {
    val ledger = build(MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx)
    val resource = ledger.resourceLine.getOrElse(fail("missing resource line"))
    assertEquals(resource.source, "authoring")
    assert(resource.sanMoves.nonEmpty)
  }

  test("authoring branch ledger notes do not project probe-result purpose") {
    val rawPurpose = "raw result purpose from probe request"
    val ctx =
      MoveReviewProseGoldenFixtures.exchangeSacrifice.ctx.copy(
        authorEvidence =
          List(
            QuestionEvidence(
              questionId = "why_this_1",
              purpose = rawPurpose,
              branches =
                List(
                  EvidenceBranch(
                    keyMove = "Nf3",
                    line = "Nf3 Nc6",
                    depth = Some(18)
                  )
                )
            )
          )
      )
    val ledger = build(ctx)
    val note = ledger.resourceLine.flatMap(_.note).getOrElse("")

    assert(!note.contains(rawPurpose), clue(note))
    assert(note.contains("key move Nf3"), clue(note))
    assert(note.contains("depth 18"), clue(note))
  }

  test("uses variation fallback for the resource line when authoring evidence is absent") {
    val refs = sampleRefs(MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.fen)
    val ledger = build(MoveReviewProseGoldenFixtures.rookPawnMarch.ctx, refs = Some(refs))
    val resource = ledger.resourceLine.getOrElse(fail("missing variation resource line"))
    assertEquals(resource.source, "variation")
    assertEquals(resource.title, "Alternate line")
    assert(!resource.title.contains("Deferred branch"), clue(resource.title))
    assertEquals(resource.sanMoves.length, 3)
  }

  test("classifies aborted continuity as blocked") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        planContinuity = Some(
          PlanContinuity(
            planName = "Rook-Pawn March",
            planId = Some("rook_pawn_march"),
            consecutivePlies = 2,
            startingPly = 20,
            phase = PlanLifecyclePhase.Aborted,
            commitmentScore = 0.45,
            abortedReason = Some("forced pivot")
          )
        )
      )
    val ledger = build(ctx)
    assertEquals(ledger.stageKey, "blocked")
    assertEquals(ledger.stageReason, Some("forced pivot"))
  }

  test("detects carry-over from the previous plan token") {
    val continuity =
      PlanContinuity(
        planName = "Rook-Pawn March",
        planId = Some("rook_pawn_march"),
        consecutivePlies = 2,
        startingPly = 18,
        phase = PlanLifecyclePhase.Execution,
        commitmentScore = 0.7,
        abortedReason = None
      )
    val token =
      PlanStateTracker(
        history = PlanStateTracker.defaultHistory.updated(
          Color.White,
          PlanStateTracker.ColorPlanState(primary = Some(continuity))
        )
      )
    val ledger = build(MoveReviewProseGoldenFixtures.rookPawnMarch.ctx, planStateToken = Some(token))
    assertEquals(ledger.carryOver, true)
    assertEquals(ledger.stageKey, "build")
  }

  test("does not emit a ledger for practical-only positions without a supported motif") {
    assertEquals(maybeBuild(MoveReviewProseGoldenFixtures.practicalChoice.ctx), None)
  }

  private def sampleRefs(fen: String): MoveReviewRefs =
    MoveReviewRefs(
      startFen = fen,
      startPly = 12,
      variations = List(
        MoveReviewVariationRef(
          lineId = "main",
          scoreCp = 14,
          mate = None,
          depth = 18,
          moves = List(
            MoveReviewMoveRef("main-0", "Rad1", "a1d1", fen, 12, 6, Some("6.")),
            MoveReviewMoveRef("main-1", "O-O", "e8g8", fen, 13, 6, None),
            MoveReviewMoveRef("main-2", "e4", "e3e4", fen, 14, 7, Some("7."))
          )
        ),
        MoveReviewVariationRef(
          lineId = "resource",
          scoreCp = 8,
          mate = None,
          depth = 17,
          moves = List(
            MoveReviewMoveRef("resource-0", "e4", "e3e4", fen, 12, 6, Some("6.")),
            MoveReviewMoveRef("resource-1", "O-O", "e8g8", fen, 13, 6, None),
            MoveReviewMoveRef("resource-2", "Re1", "f1e1", fen, 14, 7, Some("7."))
          )
        )
      )
    )

  private def replayedRefs(
      fen: String,
      lineId: String,
      ucis: List[String],
      sans: List[String]
  ): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(fen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = fen,
      startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
      variations =
        List(
          MoveReviewVariationRef(
            lineId = lineId,
            scoreCp = 42,
            mate = None,
            depth = 20,
            moves =
              ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
                val ply = NarrativeUtils.plyFromFen(fen).map(_ + 1 + idx).getOrElse(idx + 1)
                MoveReviewMoveRef(
                  refId = s"$lineId-${idx + 1}",
                  san = san,
                  uci = uci,
                  fenAfter = fens(idx),
                  ply = ply,
                  moveNo = (ply + 1) / 2,
                  marker = None
                )
              }
          )
        )
    )
