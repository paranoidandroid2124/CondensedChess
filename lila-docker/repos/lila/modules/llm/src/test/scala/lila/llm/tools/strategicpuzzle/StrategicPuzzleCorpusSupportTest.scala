package lila.llm.tools.strategicpuzzle

import munit.FunSuite
import play.api.libs.json.Json

import lila.llm.*
import lila.llm.model.authoring.{ PlanHypothesis, PlanViability }
import lila.llm.tools.strategicpuzzle.StrategicPuzzleCorpusSupport.*

class StrategicPuzzleCorpusSupportTest extends FunSuite:

  test("structural filter rejects tablebase-like material and passes rich middlegame") {
    val barren =
      CetSeed(
        id = "bare",
        fen = "8/8/8/8/8/8/4k3/4K3 w - - 0 1",
        evaluation = Some(0.0),
        whiteIsBetter = Some(false),
        whiteToMove = true,
        opening = None,
        eco = None,
        lichessURL = None,
        firestoreName = None,
        createTime = None,
        updateTime = None
      )
    val middlegame =
      CetSeed(
        id = "mid",
        fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14",
        evaluation = Some(0.3),
        whiteIsBetter = Some(true),
        whiteToMove = true,
        opening = Some("Queen's Gambit Declined"),
        eco = Some("D37"),
        lichessURL = None,
        firestoreName = None,
        createTime = None,
        updateTime = None
      )

    val barrenResult = structuralComputation(barren).fold(fail(_), identity)
    val middleResult = structuralComputation(middlegame).fold(fail(_), identity)

    assert(!barrenResult.screening.passed)
    assert(barrenResult.screening.reasons.contains("tablebase_like_piece_count"))
    assert(barrenResult.screening.reasons.contains("too_few_total_pieces"))
    assert(middleResult.screening.passed, clue(middleResult.screening.reasons))
    assertEquals(middleResult.screening.metrics.sumNonPawnMaterial, 44)
    assertEquals(middleResult.position.sideToMove, "white")
  }

  test("family derivation uses dominant idea plus anchor priority and assignCredits keeps single family full credit") {
    val main = mkCommentResponse(
      dominantIdeaKind = Some("line_occupation"),
      directionalTarget = Some("e5"),
      commentary = longCommentary
    )
    val alt = mkCommentResponse(
      dominantIdeaKind = Some("prophylaxis"),
      directionalTarget = Some("h3"),
      commentary = longCommentary
    )

    val mainFamily = familySummary(main, cpLoss = 20)
    assertEquals(mainFamily.key, Some("line_occupation|e5"))
    assert(mainFamily.robust)

    val rows = List(
      mkStage03Row("s1", "e5a", 20, main),
      mkStage03Row("s1", "e5b", 35, main),
      mkStage03Row("s1", "h3", 30, alt)
    )
    val dominant = chooseDominantFamily(rows).getOrElse(fail("dominant family missing"))
    val (accepted, partial, alternate) = assignCredits(dominant.summary, rows)

    assertEquals(dominant.summary.key, "line_occupation|e5")
    assertEquals(accepted.map(_.uci), List("e5a", "e5b"))
    assertEquals(partial.map(_.uci), List("h3"))
    assertEquals(alternate.map(_.uci), List("h3"))
  }

  test("explanation gate enforces llm polished mode, length, signal richness, and strategy coverage") {
    val failing =
      mkCommentResponse(
        dominantIdeaKind = Some("line_occupation"),
        directionalTarget = Some("e5"),
        commentary = "Too short.",
        sourceMode = "rule",
        coveragePass = Some(false),
        includeSignals = true
      )
    val passing =
      mkCommentResponse(
        dominantIdeaKind = Some("line_occupation"),
        directionalTarget = Some("e5"),
        commentary = longCommentary,
        sourceMode = "llm_polished",
        coveragePass = Some(true),
        includeSignals = true
      )

    val failedGate = explanationGate(failing)
    val passedGate = explanationGate(passing)

    assert(!failedGate.passed)
    assert(failedGate.reasons.contains("source_mode_not_llm_polished"))
    assert(failedGate.reasons.contains("commentary_too_short"))
    assert(failedGate.reasons.contains("strategy_coverage_failed"))
    assert(passedGate.passed, clue(passedGate))
    assertEquals(passedGate.sourceMode, "llm_polished")
    assert(passedGate.signalCategoryCount >= 2)
  }

  test("pre-polish gate allows rule-mode strategy signals to qualify for family clustering") {
    val ruleMode =
      mkCommentResponse(
        dominantIdeaKind = Some("line_occupation"),
        directionalTarget = Some("e5"),
        commentary = longCommentary,
        sourceMode = "rule",
        coveragePass = None,
        includeSignals = true
      )

    val gate = prePolishExplanationGate(ruleMode)
    val family = prePolishFamilySummary(ruleMode, cpLoss = 35)

    assert(gate.passed, clue(gate))
    assert(!gate.reasons.contains("source_mode_not_llm_polished"))
    assert(family.robust, clue(family))
    assertEquals(family.key, Some("line_occupation|e5"))
  }

  test("auto publish selection respects opening cap and side floor") {
    val whiteDocs =
      (1 to 20).toList.map { idx =>
        mkPuzzleDoc(
          id = s"w$idx",
          opening = Some("French Defense"),
          side = "white",
          family = if idx <= 10 then "line_occupation" else "prophylaxis",
          total = 95 - idx
        )
      }
    val blackDocs =
      (1 to 20).toList.map { idx =>
        mkPuzzleDoc(
          id = s"b$idx",
          opening = Some(if idx <= 10 then "French Defense" else "Queen's Indian Defense"),
          side = "black",
          family = if idx <= 10 then "line_occupation" else "counterplay_suppression",
          total = 90 - idx
        )
      }

    val outcome = selectAutoPublish(whiteDocs ::: blackDocs, targetCount = 20)
    val publish = outcome.autoPublish

    assertEquals(publish.size, 20)
    assert(publish.count(_.position.sideToMove == "white") >= 7, clue(publish.groupBy(_.position.sideToMove).view.mapValues(_.size).toMap))
    assert(publish.count(_.position.sideToMove == "black") >= 7, clue(publish.groupBy(_.position.sideToMove).view.mapValues(_.size).toMap))
    assert(publish.count(_.source.opening.contains("French Defense")) <= 15)
  }

  private def mkStage03Row(seedId: String, uci: String, cpLoss: Int, response: CommentResponse): Stage03MoveAnalysisRow =
    Stage03MoveAnalysisRow(
      seedId = seedId,
      fenKey = "fenkey",
      move =
        CandidateMove(
          rank = 1,
          uci = uci,
          san = uci,
          scoreCp = 20,
          mate = None,
          cpLoss = cpLoss,
          depth = 14,
          afterFen = "afterFen"
        ),
      family = familySummary(response, cpLoss),
      explanation = explanationGate(response),
      response = responseJson(response),
      generatedAt = isoNow()
    )

  private def mkPuzzleDoc(
      id: String,
      opening: Option[String],
      side: String,
      family: String,
      total: Int
  ): StrategicPuzzleDoc =
    StrategicPuzzleDoc(
      id = id,
      schema = Schema,
      source =
        SourcePayload(
          provider = "test",
          dataset = "test",
          seedId = id,
          opening = opening,
          eco = None,
          seedEvalPawns = Some(0.2),
          whiteIsBetter = Some(true),
          lichessURL = None,
          importedAt = None
        ),
      position = PositionPayload("fen", s"$id-key", "middlegame", 22, side),
      screening =
        StructuralScreening(
          passed = true,
          reasons = Nil,
          metrics = StructuralMetrics(s"$id-key", 20, 12, 12, 24, 3, 3, 26, Some(0.2))
        ),
      rootAnalysis =
        RootAnalysis(
          depth = 14,
          multiPv = 6,
          evalCp = 25,
          gapCp = Some(30),
          nearBestCount = 2,
          candidateMoves =
            List(
              CandidateMove(1, "e2e4", "e4", 25, None, 10, 14, "afterFen")
            ),
          variations = Nil
        ),
      dominantFamily = DominantFamilySummary(s"$family|e5", family, "e5", 2, 25.0, 6.0),
      acceptedMoves = List(mkPuzzleMove("e2e4")),
      partialMoves = Nil,
      alternateMoves = Nil,
      qualityScore = QualityScore(30, 30, 25, 15, total),
      generationMeta = GenerationMeta(isoNow(), RunnerVersion, "candidate")
    )

  private def mkPuzzleMove(uci: String): PuzzleMoveDoc =
    PuzzleMoveDoc(
      uci = uci,
      san = uci,
      cpLoss = 10,
      scoreCp = 30,
      mate = None,
      depth = 14,
      familyKey = Some("line_occupation|e5"),
      dominantIdeaKind = Some("line_occupation"),
      anchor = Some("e5"),
      signalRichness = 6,
      explanation =
        ExplanationGate(
          passed = true,
          reasons = Nil,
          commentaryChars = 200,
          signalCategoryCount = 3,
          strategyCoveragePassed = Some(true),
          sourceMode = "llm_polished"
        ),
      afterFen = "afterFen",
      commentResponse = Json.obj("commentary" -> longCommentary)
    )

  private def mkCommentResponse(
      dominantIdeaKind: Option[String],
      directionalTarget: Option[String],
      commentary: String,
      sourceMode: String = "llm_polished",
      coveragePass: Option[Boolean] = Some(true),
      includeSignals: Boolean = true
  ): CommentResponse =
    val digest =
      NarrativeSignalDigest(
        dominantIdeaKind = dominantIdeaKind,
        dominantIdeaFocus = Some("e5"),
        deploymentRoute = if includeSignals then List("e3", "e4") else Nil,
        deploymentPurpose = Option.when(includeSignals)("centralize"),
        opponentPlan = Option.when(includeSignals)("contest the center"),
        practicalVerdict = Option.when(includeSignals)("stable edge")
      )
    val strategyPack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets =
          directionalTarget.toList.map { square =>
            StrategyDirectionalTarget(
              targetId = "t1",
              ownerSide = "white",
              piece = "knight",
              from = "f3",
              targetSquare = square,
              readiness = "ready"
            )
          },
        strategicIdeas =
          dominantIdeaKind.toList.map { kind =>
            StrategyIdeaSignal(
              ideaId = "i1",
              ownerSide = "white",
              kind = kind,
              group = "positional",
              readiness = "ready",
              confidence = 0.9
            )
          },
        signalDigest = Some(digest)
      )
    CommentResponse(
      commentary = commentary,
      concepts = Nil,
      mainStrategicPlans =
        List(
          PlanHypothesis(
            planId = "occupy_e5",
            planName = "Occupy e5",
            rank = 1,
            score = 0.9,
            preconditions = List("stable center"),
            executionSteps = List("jump to e5"),
            failureModes = List("tactical refutation"),
            viability = PlanViability(0.9, "high", "counterplay")
          )
        ),
      sourceMode = sourceMode,
      polishMeta =
        Some(
          PolishMetaV1(
            provider = "test",
            model = Some("gpt-test"),
            sourceMode = sourceMode,
            validationPhase = "test",
            validationReasons = Nil,
            cacheHit = false,
            promptTokens = None,
            cachedTokens = None,
            completionTokens = None,
            estimatedCostUsd = None,
            strategyCoverage =
              coveragePass.map { passes =>
                StrategyCoverageMetaV1(
                  mode = "test",
                  enforced = true,
                  threshold = 0.7,
                  availableCategories = 4,
                  coveredCategories = if passes then 4 else 1,
                  requiredCategories = 3,
                  coverageScore = if passes then 1.0 else 0.2,
                  passesThreshold = passes,
                  planSignals = 2,
                  planHits = if passes then 2 else 0,
                  routeSignals = 1,
                  routeHits = if passes then 1 else 0,
                  focusSignals = 1,
                  focusHits = if passes then 1 else 0
                )
              }
          )
        ),
      strategyPack = Some(strategyPack),
      signalDigest = Some(digest)
    )

  private val longCommentary =
    "White improves piece placement first, keeps the long-term pressure on the central dark squares, " +
      "and forces Black to answer a durable positional problem before any simplification becomes comfortable."
