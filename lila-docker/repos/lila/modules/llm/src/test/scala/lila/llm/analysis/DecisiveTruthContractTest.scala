package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*

class DecisiveTruthContractTest extends FunSuite:

  private def semanticSection(
      compensation: Option[CompensationInfo] = None,
      afterCompensation: Option[CompensationInfo] = None
  ): SemanticSection =
    SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = Nil,
      compensation = compensation,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = Nil,
      afterCompensation = afterCompensation
    )

  private def ctx(
      playedMove: String,
      playedSan: String,
      fen: String = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      semantic: Option[SemanticSection] = None
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Critical", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary("Central tension", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      semantic = semantic,
      renderMode = NarrativeRenderMode.FullGame
    )

  private def comparison(
      chosenMove: String,
      engineBestMove: Option[String],
      cpLoss: Int,
      deferredMove: Option[String] = None,
      deferredReason: Option[String] = None,
      chosenMatchesBest: Boolean = false
  ): DecisionComparison =
    DecisionComparison(
      chosenMove = Some(chosenMove),
      engineBestMove = engineBestMove,
      engineBestScoreCp = Some(220),
      engineBestPv = engineBestMove.toList,
      cpLossVsChosen = Some(cpLoss),
      deferredMove = deferredMove,
      deferredReason = deferredReason,
      deferredSource = Some("heuristic"),
      evidence = Some("engine evidence"),
      practicalAlternative = true,
      chosenMatchesBest = chosenMatchesBest
    )

  private def compensationInfo(
      investedMaterial: Int,
      conversionPlan: String,
      returnVector: Map[String, Double]
  ): CompensationInfo =
    CompensationInfo(
      investedMaterial = investedMaterial,
      returnVector = returnVector,
      expiryPly = None,
      conversionPlan = conversionPlan
    )

  test("verified-best played move is downgraded to best and loses benchmark naming") {
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 180,
        deferredMove = Some("h3"),
        deferredReason = Some("it keeps the structure tighter"),
        chosenMatchesBest = false
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx("d1d5", "Rxd5"),
      comparisonOverride = Some(raw)
    )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing comparison"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.chosenMatchesBest, true)
    assertEquals(contract.cpLoss, 0)
    assertEquals(contract.benchmarkProseAllowed, false)
    assertEquals(contract.benchmarkMove, None)
    assertEquals(sanitized.cpLossVsChosen, None)
    assertEquals(sanitized.deferredMove, None)
    assertEquals(sanitized.practicalAlternative, false)
    assertEquals(sanitized.chosenMatchesBest, true)
  }

  test("verified blunder strips compensation framing from context and signal digests") {
    val comp = compensationInfo(125, "central pressure", Map("central pressure" -> 0.82))
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = Some("a4"),
        cpLoss = 320,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it keeps the center under control")
      )
    val digest =
      NarrativeSignalDigest(
        compensation = Some("central pressure against fixed targets"),
        compensationVectors = List("central files"),
        investedMaterial = Some(125),
        decisionComparison = Some(raw.toDigest)
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx(
        "c7c6",
        "Qc6",
        semantic = Some(semanticSection(compensation = Some(comp), afterCompensation = Some(comp)))
      ),
      momentType = Some("AdvantageSwing"),
      cpBefore = Some(100),
      cpAfter = Some(420),
      comparisonOverride = Some(raw)
    )
    val sanitizedCtx =
      DecisiveTruth.sanitizeContext(
        ctx("c7c6", "Qc6", semantic = Some(semanticSection(compensation = Some(comp), afterCompensation = Some(comp)))),
        contract
      )
    val sanitizedDigest = DecisiveTruth.sanitizeDigest(Some(digest), contract).getOrElse(fail("missing digest"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(sanitizedCtx.semantic.flatMap(_.compensation), None)
    assertEquals(sanitizedCtx.semantic.flatMap(_.afterCompensation), None)
    assertEquals(sanitizedDigest.compensation, None)
    assertEquals(sanitizedDigest.compensationVectors, Nil)
    assertEquals(sanitizedDigest.investedMaterial, None)
    assertEquals(sanitizedDigest.decisionComparison.flatMap(_.deferredMove), Some("a4"))
    assertEquals(sanitizedDigest.decisionComparison.exists(_.practicalAlternative), false)
  }

  test("bad moves without a verified best alternative cannot name a benchmark") {
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = None,
        cpLoss = 150,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it coordinates more cleanly")
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx("c7c6", "Qc6"),
      comparisonOverride = Some(raw)
    )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing comparison"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Mistake)
    assertEquals(contract.benchmarkProseAllowed, false)
    assertEquals(contract.benchmarkMove, None)
    assertEquals(sanitized.engineBestMove, None)
    assertEquals(sanitized.deferredMove, None)
    assertEquals(sanitized.deferredReason, None)
    assertEquals(sanitized.practicalAlternative, false)
  }

  test("value-down capture with verified payoff becomes first investment commitment") {
    val comp =
      compensationInfo(
        investedMaterial = 200,
        conversionPlan = "open-file pressure on the d-file",
        returnVector = Map("open-file pressure" -> 0.76, "d-file control" -> 0.68)
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure on the d-file before recovering the material"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through initiative and continuing pressure"),
              compensationVectors = List("initiative", "continuing pressure", "d-file control"),
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic = Some(semanticSection(compensation = Some(comp)))
        ),
      cpBefore = Some(20),
      cpAfter = Some(220),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), contract).getOrElse(fail("missing pack"))

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.InvestmentExplain)
    assertEquals(contract.exemplarRole, TruthExemplarRole.VerifiedExemplar)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.compensationProseAllowed, true)
    assertEquals(contract.verifiedPayoffAnchor, Some("pressure on d5"))
    assertEquals(contract.benchmarkMove, None)
    assertEquals(
      sanitizedPack.signalDigest.flatMap(_.compensation),
      Some("return vector through initiative and continuing pressure")
    )
  }

  test("deriveFrame records full move-truth facts for a verified investment commitment") {
    val comp =
      compensationInfo(
        investedMaterial = 200,
        conversionPlan = "open-file pressure on the d-file",
        returnVector = Map("open-file pressure" -> 0.76, "d-file control" -> 0.68)
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure on the d-file before recovering the material"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through initiative and continuing pressure"),
              compensationVectors = List("initiative", "continuing pressure", "d-file control"),
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic = Some(semanticSection(compensation = Some(comp)))
        ),
      cpBefore = Some(20),
      cpAfter = Some(220),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.moveQuality.verdict, MoveQualityVerdict.Best)
    assertEquals(frame.benchmark.verifiedBestMove, Some("Rxd5"))
    assertEquals(frame.benchmark.chosenMatchesBest, true)
    assertEquals(frame.materialEconomics.sacrificeKind, Some("exchange_sac"))
    assertEquals(frame.materialEconomics.valueDownCapture, true)
    assertEquals(frame.strategicOwnership.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(frame.strategicOwnership.verifiedPayoffAnchor, Some("pressure on d5"))
    assertEquals(frame.punishConversion.conversionRoute, Some("pressure on d5"))
    assertEquals(frame.difficultyNovelty.verificationTier, "deep_candidate")
    assertEquals(frame.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(frame.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.InvestmentExplain)
    assertEquals(frame.exemplarRole, TruthExemplarRole.VerifiedExemplar)
  }

  test("commitment owner can stay primary-visible even when compensation prose is withheld") {
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("pressure on d5"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )
    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(20),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.VerifiedExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.verifiedPayoffAnchor, Some("pressure on d5"))
  }

  test("later preserving move becomes compensation maintenance and loses fresh investment ownership") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), contract).getOrElse(fail("missing pack"))

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.MaintenanceEcho)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.SupportingVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.MaintenancePreserve)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.isInvestment, false)
    assertEquals(contract.surfacedMoveOwnsTruth, false)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("central file pressure"))
    assertEquals(sanitizedPack.signalDigest.flatMap(_.compensation), None)
    assertEquals(sanitizedPack.signalDigest.flatMap(_.investedMaterial), None)
  }

  test("low-confidence real exemplar stays visible without claiming compensation ownership") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("pressure on e6"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.SupportingVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.ProvisionalExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.surfacedMoveOwnsTruth, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("pressure on e6"))
  }

  test("recovery move becomes conversion followthrough instead of fresh investment") {
    val raw =
      comparison(
        chosenMove = "exd5",
        engineBestMove = Some("exd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("a promotion race"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              dominantIdeaFocus = Some("a promotion race")
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "e4d5",
          playedSan = "exd5",
          fen = "6k1/8/8/3b4/4P3/8/8/6K1 w - - 0 1",
          semantic =
            Some(
              semanticSection(
                compensation =
                  Some(
                    compensationInfo(
                      investedMaterial = 100,
                      conversionPlan = "a promotion race",
                      returnVector = Map("promotion race" -> 0.71)
                    )
                  )
              )
            )
        ),
      transitionType = Some("PromotionConversion"),
      cpBefore = Some(110),
      cpAfter = Some(240),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.ConversionFollowthrough))
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.ConversionOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.ConversionExplain)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.isInvestment, false)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("a promotion race"))
  }

  test("inherited compensation shell without fresh payoff anchor is not a commitment") {
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("the material can wait while the pressure is still there"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("the material can wait while the pressure is still there"),
              compensationVectors = List("return vector"),
              investedMaterial = Some(100)
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(30),
      cpAfter = Some(30),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, None)
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, None)
  }

  test("deriveFrame records benchmark and punishment facts for a verified blunder") {
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = Some("a4"),
        cpLoss = 320,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it keeps the center under control")
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx = ctx("c7c6", "Qc6"),
      momentType = Some("AdvantageSwing"),
      cpBefore = Some(100),
      cpAfter = Some(420),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.moveQuality.verdict, MoveQualityVerdict.Blunder)
    assertEquals(frame.moveQuality.severityBand, "catastrophic")
    assertEquals(frame.benchmark.verifiedBestMove, Some("a4"))
    assertEquals(frame.benchmark.benchmarkNamingAllowed, true)
    assertEquals(frame.tactical.immediateRefutation, true)
    assertEquals(frame.punishConversion.concessionSummary, Some("hands over the initiative"))
    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.BlunderOwner)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.FailureExplain)
    assertEquals(frame.compensationProseAllowed, false)
  }
