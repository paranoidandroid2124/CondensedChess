package lila.llm.analysis

import munit.FunSuite

import lila.llm.analysis.L3.*
import lila.llm.model.*
import lila.llm.model.authoring.AuthorQuestionKind
import lila.llm.model.authoring.{ PlanHypothesis, PlanViability }
import lila.llm.model.strategic.PreventedPlan

class AuthorQuestionGeneratorTest extends FunSuite:

  private val testFen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
  private val playedUci = "e1g1"
  private val playedSan = "O-O"

  private def minimalData(ctx: IntegratedContext): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = testFen,
      nature = PositionNature(lila.llm.model.NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      prevMove = Some(playedUci),
      ply = 7,
      evalCp = 40,
      isWhiteToMove = true,
      phase = "opening",
      integratedContext = Some(ctx)
    )

  private def playedCandidate(
      downstreamTactic: Option[String] = None,
      lineSanMoves: List[String] = Nil
  ): CandidateInfo =
    CandidateInfo(
      move = playedSan,
      uci = Some(playedUci),
      annotation = "!",
      planAlignment = "Development",
      tacticalAlert = None,
      practicalDifficulty = "clean",
      whyNot = None,
      downstreamTactic = downstreamTactic,
      lineSanMoves = lineSanMoves
    )

  test("generate seeds WhyThis from a played-candidate signal even without tension release") {
    val ctx = IntegratedContext(evalCp = 40, isWhiteToMove = true)
    val questions =
      AuthorQuestionGenerator.generate(
        data = minimalData(ctx),
        ctx = ctx,
        candidates = List(playedCandidate(downstreamTactic = Some("rook pressure on the e-file"), lineSanMoves = List("O-O", "Be7"))),
        playedSan = Some(playedSan)
      )

    assert(questions.exists(_.kind == AuthorQuestionKind.WhyThis), clues(questions))
  }

  test("generate seeds WhyThis from a live alternative candidate even when the played move is engine-best") {
    val ctx = IntegratedContext(evalCp = 40, isWhiteToMove = true)
    val questions =
      AuthorQuestionGenerator.generate(
        data = minimalData(ctx),
        ctx = ctx,
        candidates =
          List(
            playedCandidate(),
            CandidateInfo(
              move = "d4",
              uci = Some("d2d4"),
              annotation = "!?",
              planAlignment = "Center",
              tacticalAlert = None,
              practicalDifficulty = "dynamic",
              whyNot = Some("changes the center immediately")
            )
          ),
        playedSan = Some(playedSan)
      )

    assert(questions.exists(question =>
      question.kind == AuthorQuestionKind.WhyThis &&
        question.question.contains("instead of d4")
    ), clues(questions))
  }

  test("generate seeds WhyNow when the move lands into a live timing window") {
    val threat =
      Threat(
        kind = ThreatKind.Material,
        lossIfIgnoredCp = 180,
        turnsToImpact = 2,
        motifs = List("Fork"),
        attackSquares = List("e5"),
        targetPieces = List("Knight"),
        bestDefense = Some("Nxe5"),
        defenseCount = 2
      )
    val threatAnalysis =
      ThreatAnalysis(
        threats = List(threat),
        defense = DefenseAssessment(ThreatSeverity.Important, None, List("Nxe5"), false, false, 70, "Test"),
        threatSeverity = ThreatSeverity.Important,
        immediateThreat = true,
        strategicThreat = false,
        threatIgnorable = false,
        defenseRequired = true,
        counterThreatBetter = false,
        prophylaxisNeeded = false,
        resourceAvailable = true,
        maxLossIfIgnored = 180,
        primaryDriver = "material_threat",
        insufficientData = false
      )
    val ctx = IntegratedContext(evalCp = 40, isWhiteToMove = true, threatsToThem = Some(threatAnalysis))
    val questions =
      AuthorQuestionGenerator.generate(
        data = minimalData(ctx),
        ctx = ctx,
        candidates = List(playedCandidate()),
        playedSan = Some(playedSan)
      )

    assert(questions.exists(_.kind == AuthorQuestionKind.WhyNow), clues(questions))
  }

  test("generate seeds quiet WhyThis from plan intent when no tactical trigger exists") {
    val ctx = IntegratedContext(evalCp = 40, isWhiteToMove = true)
    val questions =
      AuthorQuestionGenerator.generate(
        data =
          minimalData(ctx).copy(
            planHypotheses =
              List(
                PlanHypothesis(
                  planId = "quiet_redeploy",
                  planName = "improve the rook on the e-file",
                  rank = 1,
                  score = 0.74,
                  preconditions = Nil,
                  executionSteps = Nil,
                  failureModes = Nil,
                  viability = PlanViability(score = 0.74, label = "medium", risk = "slow"),
                  evidenceSources = List("quiet intent")
                )
              )
          ),
        ctx = ctx,
        candidates = List(playedCandidate()),
        playedSan = Some(playedSan)
      )

    assert(questions.exists(question =>
      question.kind == AuthorQuestionKind.WhyThis &&
        question.id.startsWith("quiet_")
    ), clues(questions))
  }

  test("generate seeds WhatChanged when the move clearly changes available counterplay") {
    val ctx = IntegratedContext(evalCp = 40, isWhiteToMove = true)
    val questions =
      AuthorQuestionGenerator.generate(
        data =
          minimalData(ctx).copy(
            preventedPlans =
              List(
                PreventedPlan(
                  planId = "deny_break",
                  deniedSquares = Nil,
                  breakNeutralized = Some("d5"),
                  mobilityDelta = 2,
                  counterplayScoreDrop = 120
                )
              )
          ),
        ctx = ctx,
        candidates = List(playedCandidate()),
        playedSan = Some(playedSan)
      )

    assert(questions.exists(_.kind == AuthorQuestionKind.WhatChanged), clues(questions))
  }

  test("generate keeps diverse question kinds instead of truncating to duplicate high-priority kinds") {
    val threat =
      Threat(
        kind = ThreatKind.Material,
        lossIfIgnoredCp = 180,
        turnsToImpact = 2,
        motifs = List("Fork"),
        attackSquares = List("e5"),
        targetPieces = List("Knight"),
        bestDefense = Some("Nxe5"),
        defenseCount = 2
      )
    val threatAnalysis =
      ThreatAnalysis(
        threats = List(threat),
        defense = DefenseAssessment(ThreatSeverity.Important, None, List("Nxe5"), false, false, 70, "Test"),
        threatSeverity = ThreatSeverity.Important,
        immediateThreat = true,
        strategicThreat = false,
        threatIgnorable = false,
        defenseRequired = true,
        counterThreatBetter = false,
        prophylaxisNeeded = false,
        resourceAvailable = true,
        maxLossIfIgnored = 180,
        primaryDriver = "material_threat",
        insufficientData = false
      )
    val ctx =
      IntegratedContext(
        evalCp = 40,
        isWhiteToMove = true,
        threatsToUs = Some(threatAnalysis),
        threatsToThem = Some(threatAnalysis)
      )
    val questions =
      AuthorQuestionGenerator.generate(
        data =
          minimalData(ctx).copy(
            preventedPlans =
              List(
                PreventedPlan(
                  planId = "deny_break",
                  deniedSquares = Nil,
                  breakNeutralized = Some("d5"),
                  mobilityDelta = 2,
                  counterplayScoreDrop = 120
                )
              )
          ),
        ctx = ctx,
        candidates =
          List(
            playedCandidate(
              downstreamTactic = Some("rook pressure on the e-file"),
              lineSanMoves = List("O-O", "Be7")
            )
          ),
        playedSan = Some(playedSan)
      )

    val kinds = questions.map(_.kind).distinct
    assert(kinds.contains(AuthorQuestionKind.WhyThis), clues(questions))
    assert(kinds.contains(AuthorQuestionKind.WhyNow), clues(questions))
    assert(kinds.contains(AuthorQuestionKind.WhatChanged), clues(questions))
    assert(kinds.contains(AuthorQuestionKind.WhosePlanIsFaster), clues(questions))
    assertEquals(kinds.size, questions.size, clues(questions))
    assert(questions.size >= 4, clues(questions))
  }
