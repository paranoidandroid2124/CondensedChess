package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.{ MoveReviewMoveRef, MoveReviewRefs, MoveReviewVariationRef }
import lila.commentary.model.*
import lila.commentary.model.strategic.{ CounterfactualMatch, EngineEvidence, PvMove, VariationLine }

class DecisionComparisonBuilderTest extends FunSuite:

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some("h2h4"),
      playedSan = Some("h4"),
      summary = NarrativeSummary("Kingside expansion", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  test("build falls back to engine-gap deferred branch when engine evidence and counterfactual diverge") {
    val best =
      VariationLine(
        moves = List("g2g4", "a7a6", "h4h5"),
        scoreCp = 28,
        parsedMoves = List(
          PvMove("g2g4", "g4", "g2", "g4", "P", false, None, false),
          PvMove("a7a6", "...a6", "a7", "a6", "p", false, None, false),
          PvMove("h4h5", "h5", "h4", "h5", "P", false, None, false)
        )
      )
    val userLine =
      VariationLine(
        moves = List("h2h4", "a7a6"),
        scoreCp = 0,
        parsedMoves = List(
          PvMove("h2h4", "h4", "h2", "h4", "P", false, None, false),
          PvMove("a7a6", "...a6", "a7", "a6", "p", false, None, false)
        )
      )

    val ctx = baseContext.copy(
      engineEvidence = Some(EngineEvidence(depth = 20, variations = List(best))),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "h4",
          bestMove = "g4",
          cpLoss = 220,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "Mistake",
          userLine = userLine
        )
      )
    )

    val comparison = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    assertEquals(comparison.chosenMove, Some("h4"))
    assertEquals(comparison.engineBestMove, Some("g4"))
    assertEquals(comparison.deferredMove, Some("g4"))
    assertEquals(comparison.deferredSource, Some("engine_gap"))
    assert(comparison.deferredReason.exists(_.contains("220cp")))
    assertEquals(comparison.cpLossVsChosen, Some(220))
    assertEquals(comparison.engineBestPv, List("g4", "a6", "h5"))
    assert(!comparison.chosenMatchesBest)
  }

  test("engine best comparison derives move identity from raw PV before stale parsed metadata") {
    val line =
      VariationLine(
        moves = List("e2e4"),
        scoreCp = 24,
        parsedMoves = List(
          PvMove("d2d4", "d4", "d2", "d4", "P", false, None, false)
        )
      )
    val ctx =
      baseContext.copy(
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        playedMove = Some("e2e4"),
        playedSan = Some("e4"),
        engineEvidence = Some(EngineEvidence(depth = 20, variations = List(line)))
      )

    val comparison = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))

    assertEquals(comparison.engineBestMove, Some("e4"))
    assertEquals(comparison.engineBestPv, List("e4"))
    assertEquals(comparison.chosenMatchesBest, true)
    assertEquals(comparison.deferredMove, None)
  }

  test("build marks close candidate as practical alternative") {
    val ctx = baseContext.copy(
      fen = "r3k3/p7/8/8/8/8/6PP/R3K3 w Q - 0 1",
      candidates = List(
        CandidateInfo("h4", annotation = "!", planAlignment = "Kingside expansion", tacticalAlert = None, practicalDifficulty = "clean", whyNot = None),
        CandidateInfo("Rc1", annotation = "", planAlignment = "Rook lift", tacticalAlert = None, practicalDifficulty = "clean", whyNot = Some("it slows the direct attack"))
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 20,
          variations = List(
            VariationLine(moves = List("h2h4", "a7a6"), scoreCp = 44),
            VariationLine(
              moves = List("a1c1", "a7a6"),
              scoreCp = 30,
              parsedMoves = List(PvMove("a1c1", "Rc1", "a1", "c1", "R", false, None, false))
            )
          )
        )
      )
    )

    val comparison = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    assertEquals(comparison.deferredMove, Some("Rc1"))
    assertEquals(comparison.deferredSource, Some("close_candidate"))
    assertEquals(comparison.practicalAlternative, true)
    assert(comparison.deferredReason.exists(_.nonEmpty))
  }

  test("build prefers typed replayed line consequence over bare PV preview evidence") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx =
      baseContext.copy(
        fen = fen,
        playedMove = Some("g1f3"),
        playedSan = Some("Nf3"),
        engineEvidence = Some(EngineEvidence(depth = 20, variations = List(VariationLine(ucis, scoreCp = 42, depth = 20))))
      )
    val refs = replayedRefs(fen, "exchange", ucis, List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6"))

    val comparison = DecisionComparisonBuilder.build(ctx = ctx, refs = Some(refs)).getOrElse(fail("missing comparison"))

    assert(comparison.evidence.exists(_.toLowerCase.contains("exchange sequence")), clue(comparison))
    assert(!comparison.evidence.exists(_.startsWith("The engine line begins")), clue(comparison))
    assertEquals(comparison.engineBestPv, List("Nf3", "Nc6", "Bb5", "a6"))
  }

  test("build can use replay-backed engine evidence internally when refs are absent") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx =
      baseContext.copy(
        fen = fen,
        playedMove = Some("g1f3"),
        playedSan = Some("Nf3"),
        engineEvidence = Some(EngineEvidence(depth = 20, variations = List(VariationLine(ucis, scoreCp = 42, depth = 20))))
      )

    val comparison = DecisionComparisonBuilder.build(ctx = ctx, refs = None).getOrElse(fail("missing comparison"))

    assert(comparison.evidence.exists(_.toLowerCase.contains("exchange sequence")), clue(comparison))
    assert(!comparison.evidence.exists(_.startsWith("The engine line begins")), clue(comparison))
    assertEquals(comparison.engineBestPv, List("Nf3", "Nc6", "Bb5", "a6"))
  }

  test("exact target-fixation row carries a comparative consequence against the top multipv alternative") {
    val variations =
      List(
        VariationLine(
          moves =
            List(
              "f3d2",
              "b8a6",
              "f2f3",
              "a6c7",
              "a2a4",
              "b7b6",
              "d2c4",
              "c8a6",
              "c1g5",
              "h7h6",
              "g5h4",
              "d8d7",
              "h4e1",
              "a6c4",
              "e2c4",
              "a7a6",
              "d1d3",
              "f6h5"
            ),
          scoreCp = 65,
          depth = 24
        ),
        VariationLine(
          moves =
            List(
              "d1c2",
              "c8g4",
              "c1f4",
              "f6h5",
              "f4e3",
              "h5f6",
              "h2h3",
              "g4f3",
              "e2f3",
              "b8d7",
              "a2a4",
              "a7a6",
              "a4a5",
              "a8b8",
              "a1a2",
              "h7h5",
              "f1e1",
              "d8e7",
              "e3d2",
              "c5c4",
              "d2e3",
              "e8c8"
            ),
          scoreCp = 54,
          depth = 24
        )
      )
    val data =
      CommentaryEngine
        .assessExtended(
          fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
          variations = variations,
          phase = Some("middlegame"),
          ply = 20
        )
        .getOrElse(fail("missing exact comparative data"))
    val ctx =
      NarrativeContextBuilder
        .build(data, data.toContext, None)
        .copy(
          playedMove = Some("f3d2"),
          playedSan = Some("Nd2")
        )
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail("missing strategy pack"))
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val mainBundle =
      MainPathMoveDeltaClaimBuilder
        .build(ctx, Some(pack), truthContract = None)
        .getOrElse(fail("missing main bundle"))

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(raw),
          ctx = ctx,
          strategyPack = Some(pack),
          truthContract = None,
          mainBundleOverride = Some(mainBundle)
        )
        .getOrElse(fail("missing comparative support"))

    assertEquals(raw.chosenMove, Some("Nd2"))
    assertEquals(raw.engineBestMove, Some("Nd2"))
    assertEquals(raw.chosenMatchesBest, true)
    assertEquals(comparison.comparedMove, Some("Qc2"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.ExactTargetFixationSource))
    assertEquals(
      comparison.comparativeConsequence,
      Some("Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.")
    )
  }

  test("role-aware line consequence compares verified best branch against played branch from refs") {
    val fen = "r1b1k1nr/pppq2p1/2n1p2p/b2pP3/3P3B/2PB1N2/PP4PP/RN1QK2R b KQkq - 1 10"
    val bestLine = List("g7g5", "b2b4", "g5h4", "b4a5")
    val playedLine = List("g8e7", "e1g1", "a5b6", "a2a4")
    val ctx =
      baseContext.copy(
        fen = fen,
        ply = 20,
        playedMove = Some("g8e7"),
        playedSan = Some("Nge7"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = 207, depth = 10),
                  VariationLine(playedLine, scoreCp = 228, depth = 10)
                )
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      tacticalRefutationContract(
        playedMove = Some("Nge7"),
        verifiedBestMove = Some("g5"),
        cpLoss = 21
      )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing sanitized comparison"))
    val refs =
      replayedRefs(
        fen,
        List(
          ("best", bestLine, List("g5", "b4", "gxh4", "bxa5"), 207),
          ("played", playedLine, List("Nge7", "O-O", "Bb6", "a4"), 228)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(sanitized),
          ctx = ctx,
          refs = Some(refs),
          strategyPack = None,
          truthContract = Some(contract)
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, Some("Nge7"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
    assert(comparison.comparativeConsequence.exists(_.contains("g5 reaches an exchange sequence")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("Nge7 stays on the played branch")), clue(comparison))
    val branchEvidence = comparison.roleAwareBranchEvidence.getOrElse(fail("missing branch evidence"))
    assert(branchEvidence.evidenceRefs.contains("engine_best:line_consequence_line_id:best"), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("played:line_consequence_line_id:played"), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("engine_best:line_consequence_kind:exchange_sequence")), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("played:line_consequence_kind:preview_only")), clue(branchEvidence))
  }

  test("role-aware line consequence can compare engine-best minor-piece reroute against played preview branch") {
    val fen = "r1bqk2r/ppp2ppp/3p1n2/4N1B1/4P3/3P4/P1P1KPPP/Q6R w kq - 0 11"
    val bestLine = List("e5c4", "a7a5", "c4e3")
    val playedLine = List("e5f3", "a7a5", "h2h4", "e8g8", "h4h5", "a8a6")
    val ctx =
      baseContext.copy(
        fen = fen,
        ply = 21,
        playedMove = Some("e5f3"),
        playedSan = Some("Nf3"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = -442, depth = 10),
                  VariationLine(playedLine, scoreCp = -504, depth = 10)
                )
            )
          ),
        counterfactual =
          Some(
            CounterfactualMatch(
              userMove = "Nf3",
              bestMove = "Nc4",
              cpLoss = 62,
              missedMotifs = Nil,
              userMoveMotifs = Nil,
              severity = "Inaccuracy",
              userLine = VariationLine(playedLine, scoreCp = -504, depth = 10)
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      tacticalRefutationContract(
        playedMove = Some("Nf3"),
        verifiedBestMove = Some("Nc4"),
        cpLoss = 62
      )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing sanitized comparison"))
    val refs =
      replayedRefs(
        fen,
        List(
          ("best", bestLine, List("Nc4", "a5", "Ne3"), -442),
          ("played", playedLine, List("Nf3", "a5", "h4", "O-O", "h5", "Ra6"), -504)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(sanitized),
          ctx = ctx,
          refs = Some(refs),
          strategyPack = None,
          truthContract = Some(contract)
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, Some("Nf3"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
    assert(comparison.comparativeConsequence.exists(_.contains("Nc4 reaches a minor-piece reroute")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("Nf3 stays on the played branch")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("without that concrete minor-piece reroute")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(DecisionComparisonComparativeSupport.roleAwareLineConsequenceText), clue(comparison))
    assert(DecisionComparisonComparativeSupport.roleAwareLineConsequenceAccepted(comparison, Some(contract)), clue(comparison))
    val branchEvidence = comparison.roleAwareBranchEvidence.getOrElse(fail("missing branch evidence"))
    assert(branchEvidence.evidenceRefs.contains("engine_best:line_consequence_line_id:best"), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("played:line_consequence_line_id:played"), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("engine_best:line_consequence_kind:minor_piece_reroute")), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("played:line_consequence_kind:preview_only")), clue(branchEvidence))
  }

  test("role-aware minor-piece reroute comparison stays closed below the checked eval gap") {
    val fen = "r1bqk2r/ppp2ppp/3p1n2/4N1B1/4P3/3P4/P1P1KPPP/Q6R w kq - 0 11"
    val bestLine = List("e5c4", "a7a5", "c4e3")
    val playedLine = List("e5f3", "a7a5", "h2h4", "e8g8", "h4h5", "a8a6")
    val ctx =
      baseContext.copy(
        fen = fen,
        ply = 21,
        playedMove = Some("e5f3"),
        playedSan = Some("Nf3"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = -442, depth = 10),
                  VariationLine(playedLine, scoreCp = -452, depth = 10)
                )
            )
          ),
        counterfactual =
          Some(
            CounterfactualMatch(
              userMove = "Nf3",
              bestMove = "Nc4",
              cpLoss = 10,
              missedMotifs = Nil,
              userMoveMotifs = Nil,
              severity = "Inaccuracy",
              userLine = VariationLine(playedLine, scoreCp = -452, depth = 10)
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      tacticalRefutationContract(
        playedMove = Some("Nf3"),
        verifiedBestMove = Some("Nc4"),
        cpLoss = 10
      )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing sanitized comparison"))
    val refs =
      replayedRefs(
        fen,
        List(
          ("best", bestLine, List("Nc4", "a5", "Ne3"), -442),
          ("played", playedLine, List("Nf3", "a5", "h4", "O-O", "h5", "Ra6"), -452)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport.enrich(
        comparison = Some(sanitized),
        ctx = ctx,
        refs = Some(refs),
        strategyPack = None,
        truthContract = Some(contract)
      )

    assertEquals(comparison.flatMap(_.comparativeSource), None)
    assertEquals(comparison.flatMap(_.roleAwareBranchEvidence), None)
    assert(!comparison.flatMap(_.comparativeConsequence).exists(_.contains("minor-piece reroute")))
  }

  test("role-aware line consequence compares quiet positional-collapse style-choice branches with replayed proof") {
    val fen = "7r/pppr3p/2n1kp2/3Np3/4P3/P4P2/1PP3PP/2R1K2R w K - 0 18"
    val bestLine = List("d5e3", "h8d8", "h1f1", "h7h5", "f1f2", "h5h4", "c1d1", "d7d1")
    val playedLine = List("c2c3", "c6a5", "e1e2", "a5c4", "c1c2")
    val ctx =
      baseContext.copy(
        fen = fen,
        header = ContextHeader("Endgame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
        ply = 35,
        playedMove = Some("c2c3"),
        playedSan = Some("c3"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = 144, depth = 10),
                  VariationLine(playedLine, scoreCp = 54, depth = 10)
                )
            )
          ),
        counterfactual =
          Some(
            CounterfactualMatch(
              userMove = "c3",
              bestMove = "Ne3",
              cpLoss = 55,
              missedMotifs = Nil,
              userMoveMotifs = Nil,
              severity = "Inaccuracy",
              userLine = VariationLine(playedLine, scoreCp = 54, depth = 10)
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      DecisiveTruthContract(
        playedMove = Some("c3"),
        verifiedBestMove = Some("Ne3"),
        truthClass = DecisiveTruthClass.Inaccuracy,
        cpLoss = 55,
        swingSeverity = 55,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
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
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.QuietPositionalCollapse,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val refs =
      replayedRefs(
        fen,
        List(
          ("line_01", bestLine, List("Ne3", "Rhd8", "Rf1", "h5", "Rf2", "h4", "Rd1", "Rxd1+"), 144),
          ("line_04", playedLine, List("c3", "Na5", "Ke2", "Nc4", "Rc2"), 54)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(raw),
          ctx = ctx,
          refs = Some(refs),
          strategyPack = None,
          truthContract = Some(contract)
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, Some("c3"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
    assert(DecisionComparisonComparativeSupport.roleAwareLineConsequenceAccepted(comparison, Some(contract)), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("Ne3 reaches a capture leaving Black with an isolated pawn on h4")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("Rxd1+")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("c3 stays on the played branch")), clue(comparison))
    val branchEvidence = comparison.roleAwareBranchEvidence.getOrElse(fail("missing branch evidence"))
    assertEquals(branchEvidence.engineBest.kind, LineConsequenceKind.CaptureStructureTransition)
    assert(branchEvidence.engineBest.structureDetails.exists(detail => detail.kind == "isolated_pawn" && detail.square.contains("h4")), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("engine_best:line_consequence_line_id:line_01"), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("played:line_consequence_line_id:line_04"), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("engine_best:line_consequence_kind:capture_structure_transition")), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("played:line_consequence_kind:preview_only")), clue(branchEvidence))
  }

  test("role-aware line consequence can compare acceptable tactical narrow-choice branches with UCI proof") {
    val fen = "r2qk1nr/3nppbp/2pp2p1/pp6/3P1B2/2PBPP2/PPQN1P1P/R3K2R w KQkq - 0 10"
    val bestLine = List("a2a4", "a8b8", "a4b5", "c6b5", "c2b3")
    val playedLine = List("e1e2", "a5a4", "f4g3", "g8f6", "f3f4")
    val ctx =
      baseContext.copy(
        fen = fen,
        header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
        ply = 19,
        playedMove = Some("e1e2"),
        playedSan = Some("Ke2"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = 93, depth = 10),
                  VariationLine(playedLine, scoreCp = 8, depth = 10)
                )
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      acceptableTacticalStyleContract(
        playedMove = Some("Ke2"),
        verifiedBestMove = Some("a4"),
        cpLoss = 85
      )
    val refs =
      replayedRefs(
        fen,
        List(
          ("line_01", bestLine, List("a4", "Rb8", "axb5", "cxb5", "Qb3"), 93),
          ("line_04", playedLine, List("Ke2", "a4", "Bg3", "Ngf6", "f4"), 8)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(raw),
          ctx = ctx,
          refs = Some(refs),
          strategyPack = None,
          truthContract = Some(contract)
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, Some("Ke2"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
    assert(DecisionComparisonComparativeSupport.roleAwareLineConsequenceAccepted(comparison, Some(contract)), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("a4 reaches an exchange sequence")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("Ke2 stays on the played branch")), clue(comparison))
    val branchEvidence = comparison.roleAwareBranchEvidence.getOrElse(fail("missing branch evidence"))
    assert(branchEvidence.evidenceRefs.contains("engine_best:line_consequence_line_id:line_01"), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("played:line_consequence_line_id:line_04"), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("engine_best:line_consequence_kind:exchange_sequence")), clue(branchEvidence))
    assert(branchEvidence.guardrails.exists(_.contains("played:line_consequence_kind:preview_only")), clue(branchEvidence))
  }

  test("role-aware line consequence does not claim absence when the best move appears later in the played branch") {
    val fen = "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2"
    val bestLine = List("d7d6", "g1f3", "g8f6", "f1d3")
    val playedLine = List("g7g6", "f1d3", "d7d6", "g1f3")
    val ctx =
      baseContext.copy(
        fen = fen,
        ply = 4,
        playedMove = Some("g7g6"),
        playedSan = Some("g6"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = 69, depth = 10),
                  VariationLine(playedLine, scoreCp = 88, depth = 10)
                )
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      tacticalRefutationContract(
        playedMove = Some("g6"),
        verifiedBestMove = Some("d6"),
        cpLoss = 19
      )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing sanitized comparison"))
    val refs =
      replayedRefs(
        fen,
        List(
          ("best", bestLine, List("d6", "Nf3", "Nf6", "Bd3"), 69),
          ("played", playedLine, List("g6", "Bd3", "d6", "Nf3"), 88)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport.enrich(
        comparison = Some(sanitized),
        ctx = ctx,
        refs = Some(refs),
        strategyPack = None,
        truthContract = Some(contract)
      )

    assertEquals(comparison.flatMap(_.comparativeSource), None)
    assertEquals(comparison.flatMap(_.roleAwareBranchEvidence), None)
    assert(!comparison.flatMap(_.comparativeConsequence).exists(_.contains("without that concrete central pawn advance")))
  }

  test("role-aware line consequence compares missed benchmark against played concrete branch") {
    val fen = "r4rk1/p4bpp/5p2/p3pn2/4P1P1/7P/PP1N1P2/1K1R3R w - - 0 21"
    val bestLine = List("e4f5", "f8d8", "d2e4", "f7d5")
    val playedLine = List("g4f5", "a5a4", "h3h4", "f7h5")
    val ctx =
      baseContext.copy(
        fen = fen,
        ply = 40,
        playedMove = Some("g4f5"),
        playedSan = Some("gxf5"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 10,
              variations =
                List(
                  VariationLine(bestLine, scoreCp = -44, depth = 10),
                  VariationLine(playedLine, scoreCp = -140, depth = 10)
                )
            )
          )
      )
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val contract =
      missedOnlyMoveContract(
        playedMove = Some("g4f5"),
        verifiedBestMove = Some("e4f5"),
        cpLoss = 96
      )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing sanitized comparison"))
    val refs =
      replayedRefs(
        fen,
        List(
          ("best", bestLine, List("exf5", "Rfd8", "Ne4", "Bd5"), -44),
          ("played", playedLine, List("gxf5", "a4", "h4", "Bh5"), -140)
        )
      )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(sanitized),
          ctx = ctx,
          refs = Some(refs),
          strategyPack = None,
          truthContract = Some(contract)
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, Some("gxf5"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
    assert(comparison.comparativeConsequence.exists(_.contains("exf5 reaches a capture leaving White with doubled pawns on the f-file and a semi-open e-file for White")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("gxf5 reaches a capture leaving White with doubled pawns on the f-file and White with an isolated pawn on h4")), clue(comparison))
    assert(comparison.comparativeConsequence.exists(_.contains("about 96cp")), clue(comparison))
    val branchEvidence = comparison.roleAwareBranchEvidence.getOrElse(fail("missing branch evidence"))
    assertEquals(branchEvidence.engineBest.kind, LineConsequenceKind.CaptureStructureTransition)
    assertEquals(branchEvidence.played.kind, LineConsequenceKind.CaptureStructureTransition)
    assert(branchEvidence.engineBest.structureDetails.exists(detail => detail.kind == "semi_open_file" && detail.file.contains("e")), clue(branchEvidence))
    assert(branchEvidence.played.structureDetails.exists(detail => detail.kind == "isolated_pawn" && detail.square.contains("h4")), clue(branchEvidence))
    assert(branchEvidence.evidenceRefs.contains("engine_best:line_consequence_line_id:best"), clue(comparison))
    assert(branchEvidence.evidenceRefs.contains("played:line_consequence_line_id:played"), clue(comparison))
  }

  test("cp-gap-only comparison does not invent a comparative consequence") {
    val best =
      VariationLine(
        moves = List("g2g4", "a7a6", "h4h5"),
        scoreCp = 28,
        parsedMoves = List(
          PvMove("g2g4", "g4", "g2", "g4", "P", false, None, false),
          PvMove("a7a6", "...a6", "a7", "a6", "p", false, None, false),
          PvMove("h4h5", "h5", "h4", "h5", "P", false, None, false)
        )
      )
    val ctx = baseContext.copy(
      engineEvidence = Some(EngineEvidence(depth = 20, variations = List(best))),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "h4",
          bestMove = "g4",
          cpLoss = 220,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "Mistake",
          userLine = VariationLine(moves = List("h2h4", "a7a6"), scoreCp = 0)
        )
      )
    )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = DecisionComparisonBuilder.build(ctx),
          ctx = ctx,
          strategyPack = None,
          truthContract = None
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, None)
    assertEquals(comparison.comparativeConsequence, None)
    assertEquals(comparison.comparativeSource, None)
  }

  private def replayedRefs(
      fen: String,
      lineId: String,
      ucis: List[String],
      sans: List[String]
  ): MoveReviewRefs =
    replayedRefs(fen, List((lineId, ucis, sans, 42)))

  private def replayedRefs(
      fen: String,
      lines: List[(String, List[String], List[String], Int)]
  ): MoveReviewRefs =
    val startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1)
    MoveReviewRefs(
      startFen = fen,
      startPly = startPly,
      variations =
        lines.map { case (lineId, ucis, sans, scoreCp) =>
          val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(fen, ucis.take(idx + 1)))
          MoveReviewVariationRef(
            lineId = lineId,
            scoreCp = scoreCp,
            mate = None,
            depth = 20,
            moves =
              ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
                val ply = startPly + 1 + idx
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
        }
    )

  private def tacticalRefutationContract(
      playedMove: Option[String],
      verifiedBestMove: Option[String],
      cpLoss: Int
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = playedMove,
      verifiedBestMove = verifiedBestMove,
      truthClass = DecisiveTruthClass.Mistake,
      cpLoss = cpLoss,
      swingSeverity = cpLoss,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = true,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = true,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 1.0,
      failureIntentAnchor = verifiedBestMove,
      failureInterpretationAllowed = true
    )

  private def acceptableTacticalStyleContract(
      playedMove: Option[String],
      verifiedBestMove: Option[String],
      cpLoss: Int
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = playedMove,
      verifiedBestMove = verifiedBestMove,
      truthClass = DecisiveTruthClass.Acceptable,
      cpLoss = cpLoss,
      swingSeverity = cpLoss,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
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
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def missedOnlyMoveContract(
      playedMove: Option[String],
      verifiedBestMove: Option[String],
      cpLoss: Int
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = playedMove,
      verifiedBestMove = verifiedBestMove,
      truthClass = DecisiveTruthClass.Acceptable,
      cpLoss = cpLoss,
      swingSeverity = cpLoss,
      reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
      allowConcreteBenchmark = true,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = true,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = true,
      failureMode = FailureInterpretationMode.OnlyMoveFailure,
      failureIntentConfidence = 1.0,
      failureIntentAnchor = verifiedBestMove,
      failureInterpretationAllowed = true
    )
