package lila.commentary.analysis

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite
import lila.commentary.{ DirectionalTargetReadiness, NarrativeSignalDigest, StrategyDirectionalTarget, StrategyPack }
import lila.commentary.analysis.strategic.BreakClampMaterializer
import lila.commentary.model.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }

class BreakPreventionWitnessTest extends FunSuite:

  test("exact named-break witness succeeds with a stable defended branch") {
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(namedBreakPlan())
    )

    assertEquals(diagnosis.failureCodes, Nil)
    assert(diagnosis.witness.exists(_.breakToken == "...c5"), clues(diagnosis))
    assert(diagnosis.exactReady, clues(diagnosis))
    assertEquals(diagnosis.witness.map(_.ownerSeedTerms), Some(List("...c5", "c5")))
  }

  test("materialized break-clamp plan can become an exact named-break witness") {
    val materialized = materializedBreakInfo()
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(materialized)
    )

    assertEquals(diagnosis.failureCodes, Nil)
    assert(diagnosis.witness.exists(_.breakToken == "...b4"), clues(diagnosis))
    assert(diagnosis.exactReady, clues(diagnosis))
    assertEquals(diagnosis.witness.map(_.ownerSeedTerms), Some(List("...b4", "b4")))
  }

  test("clean route evidence can prove persistence without a separate probe experiment") {
    val materialized = materializedBreakInfo()
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(
        fen = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1",
        playedMove = "b2b4",
        engineMoves = List("b2b4", "e8e7", "e1e2", "e7e6"),
        includeStableExperiment = false
      ),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(materialized)
    )

    assertEquals(diagnosis.failureCodes, Nil)
    assert(diagnosis.witness.exists(_.breakToken == "...b4"), clues(diagnosis))
    assert(diagnosis.exactReady, clues(diagnosis))
  }

  test("short clean route evidence still requires persistence proof") {
    val materialized = materializedBreakInfo(line = List("b2b4", "e8e7"))
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(
        fen = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1",
        playedMove = "b2b4",
        engineMoves = List("b2b4", "e8e7"),
        includeStableExperiment = false
      ),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(materialized)
    )

    assert(diagnosis.witness.exists(_.breakToken == "...b4"), clues(diagnosis))
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.PersistenceUnstable))
    assert(!diagnosis.exactReady, clues(diagnosis))
  }

  test("route-level witness with recapturable capture transform stays non-exact") {
    val fen = "4k3/8/8/1pp5/8/2P5/1P6/4K3 w - - 0 1"
    val routeInfo = materializedBreakInfo(
      fen = fen,
      line = List("b2b4", "e8e7", "e1e2", "e7e6"),
      expectedBreak = "...b5-b4"
    )
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(
        fen = fen,
        playedMove = "b2b4",
        engineMoves = List("b2b4", "e8e7", "e1e2", "e7e6")
      ),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(routeInfo)
    )

    assert(diagnosis.witness.exists(_.breakToken == "...b5-b4"), clues(diagnosis))
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.CaptureTransformRecaptureUnproven))
    assert(!diagnosis.exactReady, clues(diagnosis))
  }

  test("route-level carrier fails closed when the same route is still legal") {
    val fen = "4k3/8/8/1p6/P7/8/7P/4K3 w - - 0 1"
    val staleCarrier =
      namedBreakPlan().copy(
        planId = "stale_route_carrier",
        deniedSquares = List("b4"),
        breakNeutralized = Some("...b5-b4"),
        mobilityDelta = -1,
        counterplayScoreDrop = 0
      )
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(
        fen = fen,
        playedMove = "h2h3",
        engineMoves = List("h2h3", "e8e7", "e1e2", "e7e6")
      ),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(staleCarrier)
    )

    assert(diagnosis.witness.exists(_.breakToken == "...b5-b4"), clues(diagnosis))
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.RouteStillLegal))
    assert(!diagnosis.exactReady, clues(diagnosis))
  }

  test("missing named break fails before branch proof can promote") {
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(namedBreakPlan().copy(breakNeutralized = None))
    )

    assertEquals(diagnosis.witness, None)
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.NoNamedBreak))
    assert(!diagnosis.exactReady)
  }

  test("named break without local counterplay loss is not a witness") {
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(namedBreakPlan().copy(mobilityDelta = 0, counterplayScoreDrop = 0))
    )

    assertEquals(diagnosis.witness, None)
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.NoNamedBreak))
    assert(!diagnosis.exactReady)
  }

  test("missing best-defense branch keeps the witness non-exact") {
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(engineMoves = List("c1c8")),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(namedBreakPlan())
    )

    assert(diagnosis.witness.nonEmpty, clues(diagnosis))
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.BranchMissing))
    assert(!diagnosis.exactReady)
  }

  test("rival family evidence does not become break-prevention authority") {
    val diagnosis = BreakPreventionWitness.diagnose(
      ctx = breakCtx(
        subplanId = ThemeTaxonomy.SubplanId.IQPInducement.id,
        counterBreakNeutralized = false
      ),
      surface = StrategyPackSurface.from(Some(breakPack())),
      preventedNow = List(namedBreakPlan())
    )

    assertEquals(diagnosis.witness, None)
    assertEquals(diagnosis.failureCodes, List(BreakPreventionWitness.Failure.FamilyMismatch))
    assert(!diagnosis.exactReady)
  }

  private def breakCtx(
      fen: String = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23",
      playedMove: String = "c1c8",
      engineMoves: List[String] = List("c1c8", "f8e8"),
      subplanId: String = ThemeTaxonomy.SubplanId.BreakPrevention.id,
      counterBreakNeutralized: Boolean = true,
      includeStableExperiment: Boolean = true
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 46,
      playedMove = Some(playedMove),
      playedSan = Some("Rc8"),
      summary = NarrativeSummary("Break prevention", None, "StyleChoice", "Maintain", "0.80"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Normal middlegame"),
      candidates = Nil,
      mainStrategicPlans = List(evidenceBackedPlan("break_plan", subplanId)),
      strategicPlanExperiments = Option.when(includeStableExperiment)(
        StrategicPlanExperiment(
          planId = "break_plan",
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(subplanId),
          evidenceTier = "evidence_backed",
          supportProbeCount = 1,
          refuteProbeCount = 0,
          bestReplyStable = true,
          futureSnapshotAligned = true,
          counterBreakNeutralized = counterBreakNeutralized,
          moveOrderSensitive = false,
          experimentConfidence = 0.86
        )
      ).toList,
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = List(namedBreakPlan()),
          conceptSummary = Nil
        )
      ),
      engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(engineMoves, scoreCp = 88, depth = 18))))
    )

  private def evidenceBackedPlan(planId: String, subplanId: String): PlanHypothesis =
    PlanHypothesis(
      planId = planId,
      planName = "Clamp the ...c5 break",
      rank = 1,
      score = 0.82,
      preconditions = Nil,
      executionSteps = List("Keep the opponent's main counterplay route closed first."),
      failureModes = Nil,
      viability = PlanViability(score = 0.8, label = "high", risk = "test"),
      evidenceSources = List(s"theme:${ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id}"),
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(subplanId)
    )

  private def namedBreakPlan(): PreventedPlanInfo =
    PreventedPlanInfo(
      planId = "deny_counterplay",
      deniedSquares = List("c5"),
      breakNeutralized = Some("...c5"),
      mobilityDelta = -2,
      counterplayScoreDrop = 140,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("break"),
      deniedEntryScope = Some("file"),
      sourceScope = FactScope.Now,
      citationLine = Some("The ...c5 break never becomes available on the defended branch.")
    )

  private def materializedBreakInfo(
      fen: String = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1",
      line: List[String] = List("b2b4", "e8e7", "e1e2", "e7e6"),
      expectedBreak: String = "...b4"
  ): PreventedPlanInfo =
    val plan =
      BreakClampMaterializer
        .materialize(
          fen = fen,
          board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
          color = Color.White,
          mainLine = VariationLine(line, scoreCp = 24, depth = 12)
        )
        .find(_.breakNeutralized.contains(expectedBreak))
        .getOrElse(fail(s"missing materialized $expectedBreak break clamp"))

    PreventedPlanInfo(
      planId = plan.planId,
      deniedSquares = plan.deniedSquares.map(_.key),
      breakNeutralized = plan.breakNeutralized,
      mobilityDelta = plan.mobilityDelta,
      counterplayScoreDrop = plan.counterplayScoreDrop,
      preventedThreatType = plan.preventedThreatType,
      deniedResourceClass = plan.deniedResourceClass,
      deniedEntryScope = plan.deniedEntryScope,
      sourceScope = plan.sourceScope,
      citationLine = plan.sourceLine.map(_.moves.mkString(" "))
    )

  private def breakPack(): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_c5",
          ownerSide = "white",
          piece = "R",
          from = "c1",
          targetSquare = "c5",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("deny the ...c5 break"),
          evidence = List("probe")
        )
      ),
      signalDigest = Some(NarrativeSignalDigest(decision = Some("deny the ...c5 break")))
    )
