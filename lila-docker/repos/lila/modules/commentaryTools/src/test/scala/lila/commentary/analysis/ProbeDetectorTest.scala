package lila.commentary.analysis

import chess.{ Color, Square }
import lila.commentary.analysis.L3.PvLine
import lila.commentary.model.{ Plan, PlanMatch, PlanScoringResult }
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.PreventedPlan
import munit.FunSuite

class ProbeDetectorTest extends FunSuite:

  private val StartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private val whyThisQuestion = AuthorQuestion(
    id = "why_this_q",
    kind = AuthorQuestionKind.WhyThis,
    priority = 1,
    question = "Why choose the central advance here?",
    evidencePurposes = List(ThemePlanProbePurpose.ReplyMultiPv)
  )

  test("theme probe purpose helpers own reply and validation family classification") {
    assert(ThemePlanProbePurpose.isDirectReplyPurpose(ThemePlanProbePurpose.ReplyMultiPv))
    assert(ThemePlanProbePurpose.isDirectReplyPurpose(ThemePlanProbePurpose.DefenseReplyMultiPv))
    assert(!ThemePlanProbePurpose.isDirectReplyPurpose(ThemePlanProbePurpose.ConvertReplyMultiPv))
    assert(ThemePlanProbePurpose.isConversionReplyPurpose(ThemePlanProbePurpose.ConvertReplyMultiPv))
    assert(ThemePlanProbePurpose.isConversionReplyPurpose(ThemePlanProbePurpose.DefenseReplyMultiPv))
    assert(ThemePlanProbePurpose.isRouteValidationPurpose(ThemePlanProbePurpose.RouteDenialValidation))
    assert(ThemePlanProbePurpose.isRouteContinuityPurpose(ThemePlanProbePurpose.ConvertReplyMultiPv))
    assert(ThemePlanProbePurpose.isAuthorEvidencePurpose(ThemePlanProbePurpose.KeepTensionBranches))
    assert(ThemePlanProbePurpose.requiresMultipleBranches(ThemePlanProbePurpose.KeepTensionBranches))
    assert(ThemePlanProbePurpose.requiresMultipleBranches(ThemePlanProbePurpose.RecaptureBranches))
    assert(!ThemePlanProbePurpose.requiresMultipleBranches(ThemePlanProbePurpose.ReplyMultiPv))
    assertEquals(ThemePlanProbePurpose.requiredSignalsForPurpose(ThemePlanProbePurpose.ReplyMultiPv), Some(List("replyPvs")))
    assertEquals(
      ThemePlanProbePurpose.objectiveForPurpose(ThemePlanProbePurpose.NullMoveThreat),
      Some("validate_restriction_prophylaxis")
    )
    assertEquals(ThemePlanProbePurpose.horizonForPurpose(ThemePlanProbePurpose.ConvertReplyMultiPv), Some("medium"))
    assertEquals(ThemePlanProbePurpose.budgetForPurpose(ThemePlanProbePurpose.KeepTensionBranches), Some(1))
    assert(ThemePlanProbePurpose.isPlayedMoveCounterfactualPurpose(ThemePlanProbePurpose.PlayedMoveCounterfactual))
    assert(ThemePlanProbePurpose.isNullMoveThreatPurpose(ThemePlanProbePurpose.NullMoveThreat.toLowerCase))
  }

  test("probe purpose classifier owns legacy candidate probe id families") {
    assert(ProbePurposeClassifier.isCompetitiveProbeId("competitive_e2e4"))
    assert(ProbePurposeClassifier.isAggressiveProbeId("aggressive_why_not_e2e4"))
    assert(ProbePurposeClassifier.isCompetitiveProbeId(" Competitive_d2d4 "))
    assert(!ProbePurposeClassifier.isAggressiveProbeId("competitive_e2e4"))
  }

  test("probe purpose classifier owns latent purpose profiles") {
    assertEquals(
      ProbePurposeClassifier.requiredSignalsForPurpose(ProbePurposeClassifier.LatentPlanRefutation),
      Some(List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"))
    )
    assertEquals(
      ProbePurposeClassifier.objectiveForPurpose(ProbePurposeClassifier.FreeTempoBranches),
      Some("validate_latent_plan")
    )
    assertEquals(
      ProbePurposeClassifier.horizonForPurpose(ProbePurposeClassifier.LatentPlanImmediate),
      Some("medium")
    )
    assertEquals(
      ProbePurposeClassifier.defaultMaxCpLossForPurpose(ProbePurposeClassifier.LatentPlanImmediate),
      Some(80)
    )
    assert(ProbePurposeClassifier.isKnownSupportPurpose(ProbePurposeClassifier.FreeTempoBranches))
    assert(ProbePurposeClassifier.isKnownProbePurpose(ProbePurposeClassifier.LatentPlanRefutation))
    assert(!ProbePurposeClassifier.isKnownSupportPurpose(ProbePurposeClassifier.LatentPlanRefutation))
  }

  test("plan evidence evaluator owns prevented-plan signal terms") {
    val prevented =
      PreventedPlan(
        planId = "deny_entry",
        deniedSquares = List(Square.D5, Square.C4, Square.D5),
        breakNeutralized = Some(" f5 "),
        mobilityDelta = 0,
        counterplayScoreDrop = 42,
        deniedResourceClass = Some(" entry_square "),
        deniedEntryScope = Some(" sector ")
      )

    assertEquals(
      PlanEvidenceEvaluator.preventedPlanSignalTerms(prevented),
      List("counterplay_drop:42", "neutralized_break:f5", "denied_resource:entry_square")
    )
    assertEquals(
      PlanEvidenceEvaluator.preventedPlanSignalTerms(
        prevented,
        includeDeniedSquares = true,
        includeDeniedEntryScope = true
      ),
      List(
        "counterplay_drop:42",
        "neutralized_break:f5",
        "denied_squares:c4,d5",
        "denied_resource:entry_square",
        "denied_entry_scope:sector"
      )
    )
    assertEquals(PlanEvidenceEvaluator.prophylacticDeniedResourceTerm("Entry Square"), Some("denied_resource:entry_square"))
    assert(PlanEvidenceEvaluator.isProphylacticDeniedResourceTerm("denied_resource:entry_square"))
    assert(!PlanEvidenceEvaluator.isProphylacticDeniedResourceTerm("denied_resource:generic_note"))
    assertEquals(
      PlanEvidenceEvaluator.claimCertificationTerms(
        PlanEvidenceEvaluator.ClaimCertification(
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        )
      ),
      List("best_response", "stable", "probe_backed")
    )
    assertEquals(PlanEvidenceEvaluator.supportProbeTerms(List(" probe-a ", "")), List("support_probe:probe-a"))
  }

  test("theme resolver owns theme and subplan support tags") {
    val tags =
      PlanTaxonomy.ThemeResolver.canonicalSupportTags(
        List(" theme:Restriction_Prophylaxis ", "subplan:Prophylaxis_Restraint", "other")
      )

    assertEquals(tags, List("theme:restriction_prophylaxis", "subplan:prophylaxis_restraint"))
    assertEquals(
      PlanTaxonomy.ThemeResolver.themeIdFromSupport("theme:Pawn_Break_Preparation"),
      Some("pawn_break_preparation")
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.subplanFromSupport("subplan:central_break_timing"),
      Some(PlanTaxonomy.PlanKind.CentralBreakTiming)
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.subplanAnnotation("Central plan", PlanTaxonomy.PlanKind.CentralBreakTiming),
      "Central plan [subplan:central_break_timing]"
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.subplanFromAnnotatedText("Central plan [subplan:central_break_timing]"),
      Some(PlanTaxonomy.PlanKind.CentralBreakTiming)
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.structuralStateTag("Rook Pawn March"),
      "structural_state:rook_pawn_march"
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.latentSeedTag("Rook-Pawn-March"),
      "latent_seed:rook_pawn_march"
    )
    assertEquals(
      PlanTaxonomy.ThemeResolver.seedIdFromEvidenceSource("latent_seed:rook_pawn_march"),
      Some("rook_pawn_march")
    )
    assert(PlanTaxonomy.ThemeResolver.hasStructuralStateEvidence(List("structural_state:rook_pawn_march")))
  }

  private def emptyScoring: PlanScoringResult =
    PlanScoringResult(
      topPlans = Nil,
      confidence = 0.0,
      phase = "opening"
    )

  private def matchPlan(plan: Plan, score: Double): PlanMatch =
    PlanMatch(plan = plan, score = score, evidence = Nil)

  private def hypothesis(
      id: String,
      name: String,
      score: Double,
      sources: List[String],
      subplanId: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 0,
      score = score,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = score, label = "medium", risk = "test"),
      refutation = None,
      evidenceSources = sources,
      subplanId = subplanId
    )

  test("detect emits question-first probes with purpose-aware contracts") {
    val ctx = IntegratedContext(evalCp = 30, isWhiteToMove = true)
    val multiPv = List(
      PvLine(List("e2e4", "e7e5"), evalCp = 30, mate = None, depth = 20),
      PvLine(List("d2d4", "d7d5"), evalCp = 15, mate = None, depth = 20)
    )

    val requests = ProbeDetector.detect(
      ctx = ctx,
      planScoring = emptyScoring,
      multiPv = multiPv,
      fen = StartFen,
      playedMove = Some("e2e4"),
      authorQuestions = List(whyThisQuestion)
    )

    val byPurpose = requests.groupBy(_.purpose.getOrElse(""))
    val reply = byPurpose.getOrElse("reply_multipv", Nil)

    assert(reply.nonEmpty, clue(requests))
    assert(reply.head.requiredSignals.contains("replyPvs"))
    assertEquals(reply.head.questionKind, Some("WhyThis"))
    assertEquals(reply.head.objective, Some("compare_reply_branches"))
  }

  test("question-first evidence probes remain present under mixed probe pressure") {
    val ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true)
    val multiPv = List(
      PvLine(List("g1h3", "d7d5"), evalCp = 20, mate = None, depth = 20),
      PvLine(List("b1a3", "d7d5"), evalCp = 8, mate = None, depth = 20),
      PvLine(List("c2c3", "d7d5"), evalCp = 5, mate = None, depth = 20)
    )
    val crowdedScoring = PlanScoringResult(
      topPlans = List(
        matchPlan(Plan.CentralControl(Color.White), 0.91),
        matchPlan(Plan.PieceActivation(Color.White), 0.89),
        matchPlan(Plan.KingsideAttack(Color.White), 0.87),
        matchPlan(Plan.QueensideAttack(Color.White), 0.85),
        matchPlan(Plan.RookActivation(Color.White), 0.83)
      ),
      confidence = 0.9,
      phase = "opening"
    )

    val requests = ProbeDetector.detect(
      ctx = ctx,
      planScoring = crowdedScoring,
      multiPv = multiPv,
      fen = StartFen,
      playedMove = Some("e2e4"),
      authorQuestions = List(whyThisQuestion)
    )

    val purposes = requests.flatMap(_.purpose).toSet
    assert(purposes.contains("reply_multipv"), clue(requests))
    assert(requests.size >= 2, clue(requests))
    assert(requests.size <= 8, clue(requests.map(_.id)))
  }

  test("competitive and defensive probes are shadow-only by default") {
    val quietCtx = IntegratedContext(evalCp = 10, isWhiteToMove = true)
    val competitivePv = List(
      PvLine(List("e2e4", "e7e5"), evalCp = 10, mate = None, depth = 20),
      PvLine(List("d2d4", "d7d5"), evalCp = -5, mate = None, depth = 20)
    )

    val competitiveRequests = ProbeDetector.detect(
      ctx = quietCtx,
      planScoring = emptyScoring,
      multiPv = competitivePv,
      fen = StartFen
    )
    assert(!competitiveRequests.exists(req => ProbePurposeClassifier.isCompetitiveProbeId(req.id)), clue(competitiveRequests))

    val tacticalFen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
    val defensivePv = List(
      PvLine(List("e4e5"), evalCp = 20, mate = None, depth = 20),
      PvLine(List("e4d5"), evalCp = -60, mate = None, depth = 20)
    )
    val defensiveRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = emptyScoring,
      multiPv = defensivePv,
      fen = tacticalFen
    )
    assert(!defensiveRequests.exists(req => ProbePurposeClassifier.isAggressiveProbeId(req.id)), clue(defensiveRequests))
  }

  test("low-confidence ghost probes are shadow-only while high-confidence ghosts remain emitted") {
    val lowConfidencePlan = matchPlan(
      Plan.KingsideAttack(Color.White),
      score = 0.79
    ).copy(
      evidence = List(
        lila.commentary.model.EvidenceAtom(
          motif = lila.commentary.model.Motif.PawnAdvance(
            file = chess.File.G,
            fromRank = 2,
            toRank = 4,
            color = Color.White,
            plyIndex = 0,
            move = Some("g4")
          ),
          weight = 1.0,
          description = "g-pawn push"
        )
      )
    )
    val lowScoring = PlanScoringResult(
      topPlans = List(lowConfidencePlan),
      confidence = 0.79,
      phase = "opening"
    )
    val lowRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = lowScoring,
      multiPv = List(PvLine(List("e2e4"), evalCp = 20, mate = None, depth = 20)),
      fen = StartFen
    )
    val lowGhostRequests = lowRequests.filter(_.planId.nonEmpty)
    assert(
      lowGhostRequests.forall(req => req.purpose.exists(ThemePlanProbePurpose.isThemeValidationPurpose)),
      clue(lowRequests)
    )

    val highConfidencePlan = lowConfidencePlan.copy(score = 0.85)
    val highScoring = PlanScoringResult(
      topPlans = List(highConfidencePlan),
      confidence = 0.85,
      phase = "opening"
    )
    val highRequests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 20, isWhiteToMove = true),
      planScoring = highScoring,
      multiPv = List(PvLine(List("e2e4"), evalCp = 20, mate = None, depth = 20)),
      fen = StartFen
    )
    assert(
      highRequests.exists(req =>
        req.planId.nonEmpty &&
          req.purpose.exists(ThemePlanProbePurpose.isThemeValidationPurpose)
      ),
      clue(highRequests)
    )
  }

  test("restriction subplans use family-specific probe purposes instead of generic theme validation") {
    val fen = "4k3/8/8/5n2/8/5N2/8/4K3 w - - 0 1"
    val keySquareDenial =
      hypothesis(
        id = "KeySquareClamp",
        name = "Key-square denial around d4",
        score = 0.81,
        sources = List("theme:restriction_prophylaxis"),
        subplanId = Some(PlanTaxonomy.PlanKind.KeySquareDenial.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(keySquareDenial),
      multiPv = List(PvLine(List("f3d4"), evalCp = 18, mate = None, depth = 20)),
      fen = fen,
      playedMove = Some("f3d4")
    )

    val routeDenial = requests.find(_.planId.contains("KeySquareClamp")).getOrElse(fail(requests.toString))
    assertEquals(routeDenial.purpose, Some("route_denial_validation"))
    assertEquals(routeDenial.objective, Some("validate_route_denial"))
    assert(routeDenial.requiredSignals.contains("futureSnapshot"), clue(routeDenial))
    assert(routeDenial.requiredSignals.contains("l1Delta"), clue(routeDenial))
    assert(!routeDenial.planName.exists(_.contains("[subplan:")), clue(routeDenial))
  }

  test("specific pawn-conversion subplans do not reinflate into generic theme pawn probes") {
    val fen = "4k3/8/8/1p2P3/8/8/1P6/4K3 w - - 0 1"
    val passerConversion =
      hypothesis(
        id = "PasserConversion",
        name = "advance the e-passer",
        score = 0.84,
        sources = List("theme:advantage_transformation", "subplan:passer_conversion"),
        subplanId = Some(PlanTaxonomy.PlanKind.PasserConversion.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(passerConversion),
      multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("PasserConversion")).getOrElse(fail(requests.toString))
    assertEquals(planProbe.moves, List("e5e6"))
  }

  test("specific rook-pawn subplans do not reinflate into generic flank pawn probes") {
    val fen = "4k3/8/8/8/8/8/1P5P/6K1 w - - 0 1"
    val rookPawnMarch =
      hypothesis(
        id = "RookPawnMarch",
        name = "advance the rook pawn",
        score = 0.84,
        sources = List("theme:flank_infrastructure", "subplan:rook_pawn_march"),
        subplanId = Some(PlanTaxonomy.PlanKind.RookPawnMarch.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(rookPawnMarch),
      multiPv = List(PvLine(List("g1f1"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("RookPawnMarch")).getOrElse(fail(requests.toString))
    assert(planProbe.moves.nonEmpty, clue(planProbe))
    assert(planProbe.moves.forall(_.startsWith("h2")), clue(planProbe))
    assert(!planProbe.moves.exists(_.startsWith("b2")), clue(planProbe))
  }

  test("specific pawn-break subplans do not reinflate into quiet central pawn probes") {
    val fen = "4k3/8/8/3p4/4P3/8/4P3/4K3 w - - 0 1"
    val centralBreak =
      hypothesis(
        id = "CentralBreakTiming",
        name = "time the central break",
        score = 0.84,
        sources = List("theme:pawn_break_preparation", "subplan:central_break_timing"),
        subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(centralBreak),
      multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("CentralBreakTiming")).getOrElse(fail(requests.toString))
    assertEquals(planProbe.moves, List("e4d5"))
  }

  test("covered weak subplans fail closed instead of falling back to broad theme moves") {
    val cases = List(
      (
        "4k3/8/3p1p2/8/4P3/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.PasserConversion,
        "PasserConversion"
      ),
      (
        "4k3/8/8/8/8/8/1P6/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.RookPawnMarch,
        "RookPawnMarch"
      ),
      (
        "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.CentralBreakTiming,
        "CentralBreakTiming"
      ),
      (
        StartFen,
        PlanTaxonomy.PlanKind.ProphylaxisRestraint,
        "ProphylaxisRestraint"
      ),
      (
        StartFen,
        PlanTaxonomy.PlanKind.BreakPrevention,
        "BreakPrevention"
      ),
      (
        StartFen,
        PlanTaxonomy.PlanKind.KeySquareDenial,
        "KeySquareDenial"
      ),
      (
        StartFen,
        PlanTaxonomy.PlanKind.MobilitySuppression,
        "MobilitySuppression"
      ),
      (
        "4k3/8/8/8/4P3/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.CentralSpaceBind,
        "CentralSpaceBind"
      ),
      (
        "4k3/8/8/8/8/7P/8/6K1 w - - 0 1",
        PlanTaxonomy.PlanKind.FlankClamp,
        "FlankClamp"
      ),
      (
        "4k3/8/8/8/4P3/5N2/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.TensionMaintenance,
        "TensionMaintenance"
      ),
      (
        "4k3/8/8/8/8/8/1P6/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.StaticWeaknessFixation,
        "StaticWeaknessFixation"
      ),
      (
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.SimplificationWindow,
        "SimplificationWindow"
      ),
      (
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.QueenTradeShield,
        "QueenTradeShield"
      ),
      (
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.DefenderTrade,
        "DefenderTrade"
      ),
      (
        "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.RookLiftScaffold,
        "RookLiftScaffold"
      ),
      (
        "r3k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.SimplificationConversion,
        "SimplificationConversion"
      ),
      (
        "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.InvasionTransition,
        "InvasionTransition"
      ),
      (
        "4k3/8/8/4P3/8/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.OppositeBishopsConversion,
        "OppositeBishopsConversion"
      ),
      (
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.BadPieceLiquidation,
        "BadPieceLiquidation"
      )
    )

    cases.foreach { case (fen, subplan, id) =>
      val requests = ProbeDetector.detect(
        ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
        planScoring = emptyScoring,
        planHypotheses = List(
          hypothesis(
            id = id,
            name = subplan.id,
            score = 0.84,
            sources = List(s"subplan:${subplan.id}"),
            subplanId = Some(subplan.id)
          )
        ),
        multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
        fen = fen
      )

      assert(!requests.exists(_.planId.contains(id)), clue(subplan, requests))
    }
  }

  test("specific restriction subplans use board-backed candidate moves") {
    val fen = "4k3/8/8/5n2/8/5N2/8/4K3 w - - 0 1"
    val keySquareDenial =
      hypothesis(
        id = "KeySquareDenial",
        name = "deny d4",
        score = 0.84,
        sources = List("theme:restriction_prophylaxis", "subplan:key_square_denial"),
        subplanId = Some(PlanTaxonomy.PlanKind.KeySquareDenial.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(keySquareDenial),
      multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("KeySquareDenial")).getOrElse(fail(requests.toString))
    assert(planProbe.moves.contains("f3d4"), clue(planProbe))
  }

  test("specific tension-maintenance subplans probe quiet moves that keep central pawn tension") {
    val fen = "4k3/8/8/3p4/4P3/5N2/8/4K3 w - - 0 1"
    val tensionMaintenance =
      hypothesis(
        id = "TensionMaintenance",
        name = "keep central tension",
        score = 0.84,
        sources = List("theme:pawn_break_preparation", "subplan:tension_maintenance"),
        subplanId = Some(PlanTaxonomy.PlanKind.TensionMaintenance.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(tensionMaintenance),
      multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("TensionMaintenance")).getOrElse(fail(requests.toString))
    assert(planProbe.moves.nonEmpty, clue(planProbe))
    assert(planProbe.moves.forall(!_.startsWith("e4")), clue(planProbe))
  }

  test("specific rook-lift subplans use lift-rank rook moves") {
    val fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1"
    val rookLift =
      hypothesis(
        id = "RookLiftScaffold",
        name = "lift the rook",
        score = 0.84,
        sources = List("theme:flank_infrastructure", "subplan:rook_lift_scaffold"),
        subplanId = Some(PlanTaxonomy.PlanKind.RookLiftScaffold.id)
      )

    val requests = ProbeDetector.detect(
      ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
      planScoring = emptyScoring,
      planHypotheses = List(rookLift),
      multiPv = List(PvLine(List("h1h3"), evalCp = 18, mate = None, depth = 20)),
      fen = fen
    )

    val planProbe = requests.find(_.planId.contains("RookLiftScaffold")).getOrElse(fail(requests.toString))
    assert(planProbe.moves.exists(move => move == "h1h3" || move == "h1h4"), clue(planProbe))
    assert(!planProbe.moves.contains("h1a1"), clue(planProbe))
  }

  test("specific transformation subplans use board-backed conversion moves") {
    val cases = List(
      (
        "InvasionTransition",
        PlanTaxonomy.PlanKind.InvasionTransition,
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        Set("a1a7", "a1a8")
      ),
      (
        "OppositeBishopsConversion",
        PlanTaxonomy.PlanKind.OppositeBishopsConversion,
        "4k1b1/8/8/4P3/8/8/8/2B1K3 w - - 0 1",
        Set("e5e6")
      )
    )

    cases.foreach { case (id, subplan, fen, expectedMoves) =>
      val h =
        hypothesis(
          id = id,
          name = subplan.id,
          score = 0.84,
          sources = List(s"theme:${subplan.theme.id}", s"subplan:${subplan.id}"),
          subplanId = Some(subplan.id)
        )

      val requests = ProbeDetector.detect(
        ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
        planScoring = emptyScoring,
        planHypotheses = List(h),
        multiPv = List(PvLine(List(expectedMoves.head), evalCp = 18, mate = None, depth = 20)),
        fen = fen
      )

      val planProbe = requests.find(_.planId.contains(id)).getOrElse(fail(requests.toString))
      assert(planProbe.moves.exists(expectedMoves.contains), clue(subplan, planProbe))
    }
  }

  test("detect handles planner-era subplans without MatchError and keeps validation probes live") {
    val problematicSubplans =
      List(
        PlanTaxonomy.PlanKind.BishopReanchor -> "4k3/8/8/2pp4/3BP3/5Q2/4R3/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.OpenFilePressure -> "4k3/8/8/2pp4/3BP3/5Q2/4R3/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.IQPInducement -> "4k3/8/8/3p4/5N2/8/8/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.BadPieceLiquidation -> "4k3/8/8/8/8/1n6/8/N3K3 w - - 0 1",
        PlanTaxonomy.PlanKind.PassedPawnManufacture -> "4k3/8/8/2pp4/3BP3/5Q2/4R3/4K3 w - - 0 1",
        PlanTaxonomy.PlanKind.BatteryPressure -> "4k3/8/8/2pp4/3BP3/5Q2/4R3/4K3 w - - 0 1"
      )
    problematicSubplans.foreach { case (subplan, fen) =>
      val requests =
        ProbeDetector.detect(
          ctx = IntegratedContext(evalCp = 18, isWhiteToMove = true),
          planScoring = emptyScoring,
          planHypotheses =
            List(
              hypothesis(
                id = s"plan_${subplan.id}",
                name = subplan.id,
                score = 0.84,
                sources = List(s"subplan:${subplan.id}"),
                subplanId = Some(subplan.id)
              )
            ),
          multiPv = List(PvLine(List("e1d2"), evalCp = 18, mate = None, depth = 20)),
          fen = fen
        )
      assert(
        requests.exists(_.planId.contains(s"plan_${subplan.id}")),
        clue(subplan, requests)
      )
    }
  }
