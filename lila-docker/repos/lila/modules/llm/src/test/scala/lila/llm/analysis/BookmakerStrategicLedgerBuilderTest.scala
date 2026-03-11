package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ BookmakerRefsV1, MoveRefV1, StrategyPack, StrategyPieceRoute, VariationRefV1 }
import lila.llm.model.NarrativeContext
import lila.llm.model.ProbeResult
import lila.llm.model.strategic.{ EngineEvidence, PlanContinuity, PlanLifecyclePhase, VariationLine }
import _root_.chess.Color

class BookmakerStrategicLedgerBuilderTest extends FunSuite:

  private def maybeBuild(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      refs: Option[BookmakerRefsV1] = None,
      probeResults: List[ProbeResult] = Nil,
      planStateToken: Option[PlanStateTracker] = None
  ) =
    BookmakerStrategicLedgerBuilder.build(
      ctx = ctx,
      strategyPack = strategyPack,
      refs = refs,
      probeResults = probeResults,
      planStateToken = planStateToken,
      endgameStateToken = None
    )

  private def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack] = None,
      refs: Option[BookmakerRefsV1] = None,
      probeResults: List[ProbeResult] = Nil,
      planStateToken: Option[PlanStateTracker] = None
  ) =
    maybeBuild(
      ctx = ctx,
      strategyPack = strategyPack,
      refs = refs,
      probeResults = probeResults,
      planStateToken = planStateToken
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
    val ledger = build(BookmakerProseGoldenFixtures.rookPawnMarch.ctx, strategyPack = Some(routePack))
    assertEquals(ledger.motifKey, "rook_pawn_march")
    assertEquals(ledger.motifLabel, "Rook-pawn march")
  }

  test("classifies prophylactic fixtures as restrain") {
    val ledger = build(BookmakerProseGoldenFixtures.prophylacticCut.ctx)
    assertEquals(ledger.stageKey, "restrain")
    assert(ledger.stageReason.exists(_.toLowerCase.contains("counterplay")))
  }

  test("classifies compensation fixtures as convert") {
    val ledger = build(BookmakerProseGoldenFixtures.exchangeSacrifice.ctx)
    assertEquals(ledger.stageKey, "convert")
    assertEquals(ledger.motifKey, "compensation_attack")
    assertEquals(ledger.conversionTrigger, Some("Mating Attack"))
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
        BookmakerProseGoldenFixtures.rookPawnMarch.ctx.copy(fen = legalFen),
        probeResults = List(probe)
      )
    val primary = ledger.primaryLine.getOrElse(fail("missing primary line"))
    assertEquals(primary.source, "probe")
    assertEquals(primary.sanMoves.length, 4)
  }

  test("falls back to decision-compare engine pv when no supportive probe exists") {
    val legalFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ctx =
      BookmakerProseGoldenFixtures.exchangeSacrifice.ctx.copy(
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

  test("prefers authoring branches for the resource line") {
    val ledger = build(BookmakerProseGoldenFixtures.exchangeSacrifice.ctx)
    val resource = ledger.resourceLine.getOrElse(fail("missing resource line"))
    assertEquals(resource.source, "authoring")
    assert(resource.sanMoves.nonEmpty)
  }

  test("uses variation fallback for the resource line when authoring evidence is absent") {
    val refs = sampleRefs(BookmakerProseGoldenFixtures.rookPawnMarch.ctx.fen)
    val ledger = build(BookmakerProseGoldenFixtures.rookPawnMarch.ctx, refs = Some(refs))
    val resource = ledger.resourceLine.getOrElse(fail("missing variation resource line"))
    assertEquals(resource.source, "variation")
    assertEquals(resource.sanMoves.length, 3)
  }

  test("classifies aborted continuity as blocked") {
    val ctx =
      BookmakerProseGoldenFixtures.rookPawnMarch.ctx.copy(
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
    val ledger = build(BookmakerProseGoldenFixtures.rookPawnMarch.ctx, planStateToken = Some(token))
    assertEquals(ledger.carryOver, true)
    assertEquals(ledger.stageKey, "build")
  }

  test("does not emit a ledger for practical-only positions without a supported motif") {
    assertEquals(maybeBuild(BookmakerProseGoldenFixtures.practicalChoice.ctx), None)
  }

  private def sampleRefs(fen: String): BookmakerRefsV1 =
    BookmakerRefsV1(
      startFen = fen,
      startPly = 12,
      variations = List(
        VariationRefV1(
          lineId = "v1",
          scoreCp = 14,
          mate = None,
          depth = 18,
          moves = List(
            MoveRefV1("v1-0", "Rad1", "a1d1", fen, 12, 6, Some("6.")),
            MoveRefV1("v1-1", "O-O", "e8g8", fen, 13, 6, None),
            MoveRefV1("v1-2", "e4", "e3e4", fen, 14, 7, Some("7."))
          )
        ),
        VariationRefV1(
          lineId = "v2",
          scoreCp = 8,
          mate = None,
          depth = 17,
          moves = List(
            MoveRefV1("v2-0", "e4", "e3e4", fen, 12, 6, Some("6.")),
            MoveRefV1("v2-1", "O-O", "e8g8", fen, 13, 6, None),
            MoveRefV1("v2-2", "Re1", "f1e1", fen, 14, 7, Some("7."))
          )
        )
      )
    )
