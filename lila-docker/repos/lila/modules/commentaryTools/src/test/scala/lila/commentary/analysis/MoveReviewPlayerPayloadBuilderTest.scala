package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.*
import lila.commentary.model.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ ClaimCertification, EvaluatedPlan, PlanEvidenceStatus, UserFacingPlanEligibility }
import lila.commentary.analysis.semantic.{ RelationObservationCatalog, RelationSurfaceRowKind }
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceRef

final class MoveReviewPlayerPayloadBuilderTest extends FunSuite:

  private val emptyAuthoringSurface =
    AuthoringEvidenceSurface(questions = Nil, evidence = Nil, headline = None)

  private val InitialFen =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private def persistentTargetEvidence(moves: List[String] = List("e1d1", "e8d8", "d1e1", "d8e8", "e1d1")): EngineEvidence =
    EngineEvidence(
      depth = 18,
      variations =
        List(
          VariationLine(
            moves = moves,
            scoreCp = 12,
            depth = 18
          )
        )
    )

  private def anchoredCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas =
        List(
          StrategyIdeaSignal(
            ideaId = "idea_benko_targets",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = "slow_structural",
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = List("b2", "a6"),
            focusFiles = List("a", "b"),
            focusZone = Some("queenside"),
            beneficiaryPieces = List("R"),
            confidence = 0.79
          )
        ),
      pieceMoveRefs =
        List(
          StrategyPieceMoveRef(
            ownerSide = "black",
            piece = "Q",
            from = "d8",
            target = "b6",
            idea = "fix the queenside targets"
          )
        ),
      directionalTargets =
        List(
          StrategyDirectionalTarget(
            targetId = "target_b2",
            ownerSide = "black",
            piece = "R",
            from = "d8",
            targetSquare = "b2",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("backward pawn")
          )
        ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest =
        Some(
          NarrativeSignalDigest(
            compensation = Some("return vector through line pressure and delayed recovery"),
            compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
            investedMaterial = Some(100)
          )
        )
    )

  private def genericCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest =
        Some(
          NarrativeSignalDigest(
            compensation = Some("initiative against the king"),
            compensationVectors = List("Initiative (0.6)"),
            investedMaterial = Some(100)
          )
        )
    )

  private def build(
      ctx: NarrativeContext = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx,
      moveReviewExplanation: Option[MoveReviewExplanation] = None,
      moveReviewLedger: Option[MoveReviewStrategicLedger] = None,
      refs: Option[MoveReviewRefs] = None,
      evaluatedPlans: List[EvaluatedPlan] = Nil,
      authoringSurface: AuthoringEvidenceSurface = emptyAuthoringSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None,
      lineConsequence: Option[LineConsequenceEvidence] = None
  ): MoveReviewPlayerSurface =
    MoveReviewPlayerPayloadBuilder.build(
      ctx = ctx,
      moveReviewExplanation = moveReviewExplanation,
      moveReviewLedger = moveReviewLedger,
      refs = refs,
      evaluatedPlans = evaluatedPlans,
      authoringSurface = authoringSurface,
      supportedLocalRows = supportedLocalRows,
      decisionComparisonSurface = decisionComparisonSurface,
      strategyPack = strategyPack,
      truthContract = truthContract,
      lineConsequence = lineConsequence
    )

  private def plan(
      name: String,
      evidenceSources: List[String] = Nil,
      executionSteps: List[String] = Nil,
      preconditions: List[String] = Nil,
      failureModes: List[String] = Nil,
      themeL1: String = PlanTaxonomy.PlanTheme.PieceRedeployment.id,
      subplanId: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = name.toLowerCase.replaceAll("\\s+", "_"),
      planName = name,
      rank = 1,
      score = 0.82,
      preconditions = preconditions,
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = PlanViability(score = 0.82, label = "high", risk = "needs proof"),
      evidenceSources = evidenceSources,
      themeL1 = themeL1,
      subplanId = subplanId
    )

  private def evaluated(
      hypothesis: PlanHypothesis,
      eligibility: UserFacingPlanEligibility,
      supportProbeIds: List[String] = Nil,
      transpositionProofIds: List[String] = Nil,
      claimCertification: ClaimCertification = ClaimCertification()
  ): EvaluatedPlan =
    EvaluatedPlan(
      hypothesis = hypothesis,
      status =
        if eligibility == UserFacingPlanEligibility.Refuted then PlanEvidenceStatus.Refuted
        else if eligibility == UserFacingPlanEligibility.TranspositionAligned then PlanEvidenceStatus.PlayableTranspositionAligned
        else if eligibility == UserFacingPlanEligibility.PvCoupledOnly then PlanEvidenceStatus.PlayablePvCoupled
        else if eligibility == UserFacingPlanEligibility.Deferred then PlanEvidenceStatus.Deferred
        else if eligibility == UserFacingPlanEligibility.StructuralOnly then PlanEvidenceStatus.PlayableStructuralOnly
        else PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = eligibility,
      reason = "test",
      supportProbeIds = supportProbeIds,
      transpositionProofIds = transpositionProofIds,
      themeL1 = hypothesis.themeL1,
      subplanId = hypothesis.subplanId,
      claimCertification = claimCertification
    )

  private def tacticalTruthContract(): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("e4e5"),
      verifiedBestMove = Some("e4e5"),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def relationOnlyData(fen: String, playedMove: String): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = fen,
      nature = PositionNature(NatureType.Dynamic, 0.6, 0.4, "relation test"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = Nil,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = Nil,
      prevMove = Some(playedMove),
      ply = 1,
      evalCp = 42,
      isWhiteToMove = true
    )

  private def relationOnlyContext(fen: String, playedMove: String): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 1,
      playedMove = Some(playedMove),
      playedSan = Some("Be4"),
      summary = NarrativeSummary("Relation test", None, "StyleChoice", "Maintain", "+0.4"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "none", "Background", None, false, "quiet"),
      plans = PlanTable(top5 = Nil, suppressed = Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "relation test"),
      candidates = Nil,
      engineEvidence = Some(
        EngineEvidence(
          depth = 18,
          variations = List(VariationLine(moves = List(playedMove), scoreCp = 42))
        )
      )
    )

  private def relationIdeaSignal(
      kind: String,
      ideaId: String = "idea_1",
      group: String = "relation",
      focusSquares: List[String] = List("e4", "f5", "g6"),
      evidenceRefs: Option[List[String]] = None,
      targetSquare: Option[String] = None,
      relationFocusSquares: Option[List[String]] = None
  ): StrategyIdeaSignal =
    val descriptor = RelationObservationCatalog.descriptorForKind(kind).get
    StrategyIdeaSignal(
      ideaId = ideaId,
      ownerSide = "white",
      kind = descriptor.ideaKind,
      group = group,
      readiness = descriptor.readiness,
      focusSquares = focusSquares,
      confidence = descriptor.confidence,
      evidenceRefs = evidenceRefs.getOrElse(descriptor.wireEvidenceRefs),
      targetSquare = targetSquare,
      relationKind = Some(descriptor.relationKind),
      relationFocusSquares = relationFocusSquares.getOrElse(focusSquares)
    )

  test("does not build player decision comparison without certified surface input") {
    val surface =
      build(
        moveReviewExplanation =
          Some(MoveReviewExplanation(title = "Move review title", prose = "Short explanation"))
      )

    assertEquals(surface.schema, "chesstory.move_review.player_surface.v2")
    assertEquals(surface.title, Some("Move review title"))
    assertEquals(surface.decisionComparison, None)
  }

  test("uses move review explanation short line as player support") {
    val surface =
      build(
        ctx = relationOnlyContext(InitialFen, "e2e4"),
        moveReviewExplanation =
          Some(
            MoveReviewExplanation(
              title = "Move review: e4",
              prose = "e4 keeps the center question concrete.",
              shortLine = Some(MoveReviewShortLine(san = List("e4", "e5", "Nf3"))),
              pvInterpretation =
                Some(
                  MoveReviewPvInterpretation(
                    linePurpose = "central test",
                    tension = "center",
                    learningPoint = "The checked line keeps the center question concrete."
                  )
                )
            )
          )
      )

    assertEquals(surface.summaryRows.map(_.label), List("Checked line"))
    assertEquals(
      surface.summaryRows.head.text,
      "The checked line keeps the center question concrete. Short line: e4 e5 Nf3."
    )
    assertEquals(surface.summaryRows.head.refSans, List("e4", "e5", "Nf3"))
    assertEquals(surface.summaryRows.head.authority, None)
  }

  test("uses move review refs as player support when explanation has no short line") {
    val refs =
      MoveReviewRefs(
        startFen = InitialFen,
        startPly = 1,
        variations =
          List(
            MoveReviewVariationRef(
              lineId = "line_00",
              scoreCp = 22,
              mate = None,
              depth = 8,
              moves =
                List(
                  MoveReviewMoveRef("l00_m01", "d4", "d2d4", InitialFen, 1, 1, Some("1.")),
                  MoveReviewMoveRef("l00_m02", "d5", "d7d5", InitialFen, 2, 1, Some("1..."))
                )
            ),
            MoveReviewVariationRef(
              lineId = "line_01",
              scoreCp = 15,
              mate = None,
              depth = 8,
              moves =
                List(
                  MoveReviewMoveRef("l01_m01", "e4", "e2e4", InitialFen, 1, 1, Some("1.")),
                  MoveReviewMoveRef("l01_m02", "e5", "e7e5", InitialFen, 2, 1, Some("1...")),
                  MoveReviewMoveRef("l01_m03", "Nf3", "g1f3", InitialFen, 3, 2, Some("2."))
                )
            )
          )
      )
    val surface =
      build(
        ctx = relationOnlyContext(InitialFen, "e2e4"),
        refs = Some(refs)
      )

    assertEquals(surface.summaryRows.map(_.label), List("Checked line"))
    assertEquals(surface.summaryRows.head.text, "Short line: e4 e5 Nf3.")
    assertEquals(surface.summaryRows.head.refSans, List("e4", "e5", "Nf3"))
    assertEquals(surface.summaryRows.head.authority, None)
  }

  test("uses certified line-consequence ref as player support before a shallow reviewed ref") {
    val fen = "r1bqkbnr/ppp1p1pp/2n5/3pP3/3P4/8/PPP3PP/RNBQKBNR b KQkq - 0 5"

    def variation(lineId: String, ucis: List[String], sans: List[String]): MoveReviewVariationRef =
      val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(fen, ucis.take(idx + 1)))
      MoveReviewVariationRef(
        lineId = lineId,
        scoreCp = 16,
        mate = None,
        depth = 10,
        moves =
          ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
            val ply = NarrativeUtils.plyFromFen(fen).map(_ + 1 + idx).getOrElse(idx + 1)
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

    val shallowLine = variation("line_01", List("e7e6", "g1f3"), List("e6", "Nf3"))
    val consequenceUcis = List("e7e6", "g1f3", "g8h6", "f1d3", "c6b4", "c1h6", "b4d3", "d1d3")
    val consequenceSans = List("e6", "Nf3", "Nh6", "Bd3", "Nb4", "Bxh6", "Nxd3+", "Qxd3")
    val refs =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).map(_ + 1).getOrElse(1),
        variations = List(shallowLine, variation("line_04", consequenceUcis, consequenceSans))
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

    val surface =
      build(
        ctx = relationOnlyContext(fen, "e7e6"),
        refs = Some(refs),
        lineConsequence = Some(consequence)
      )

    assertEquals(surface.summaryRows.map(_.label), List("Checked line"))
    assertEquals(surface.summaryRows.head.text, "Short line: e6 Nf3 Nh6 Bd3 Nb4.")
    assertEquals(surface.summaryRows.head.refSans, List("e6", "Nf3", "Nh6", "Bd3", "Nb4"))
  }

  test("uses only certified decision comparison surface input for the player strip") {
    val certified =
      MoveReviewPlayerDecisionComparison(
        kicker = "Decision point",
        gapLabel = Some("220cp"),
        chosenSan = Some("h4"),
        engineSan = Some("g4"),
        comparedSan = None,
        secondaryText = Some("The checked line reaches an exchange sequence after Bxc6, so the decision is about which structure remains."),
        chosenMatchesBest = false
      )

    val surface = build(decisionComparisonSurface = Some(certified))

    assertEquals(surface.decisionComparison, Some(certified))
  }

  test("builds a player decision strip only from surface-candidate line consequence") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        engineBestPv = evidence.sanMoves,
        cpLossVsChosen = Some(220),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing decision surface"))

    assertEquals(surface.kicker, "Decision point")
    assertEquals(surface.gapLabel, Some("220cp"))
    assertEquals(surface.chosenSan, Some("h4"))
    assertEquals(surface.engineSan, Some("g4"))
    assert(surface.secondaryText.exists(_.contains("exchange sequence")), clue(surface))
    assertEquals(surface.refSans, List("Nf3", "Nc6", "Bb5", "a6", "Bxc6"))
  }

  test("does not surface diagnostic-only line consequence") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        chosenMatchesBest = false
      )
    val diagnostic = evidence.copy(
      release = LineConsequenceRelease.DiagnosticOnly,
      rejectReasons = List("line_consequence:engine_only")
    )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(diagnostic)), None)
  }

  test("does not surface bare small cp gaps without exact comparison or practical alternative") {
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(30),
        chosenMatchesBest = false
      )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(surfaceLineEvidence())), None)
  }

  test("does not use preview-only line consequence text for the player decision strip") {
    val preview =
      surfaceLineEvidence().copy(
        kind = LineConsequenceKind.PreviewOnly,
        consequence = "This proves this plan.",
        whyItMatters = Some("It wins strategically.")
      )
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        chosenMatchesBest = false
      )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(preview)), None)
  }

  test("surfaces moderate cp gaps with bounded line consequence wording") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(45),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing moderate-gap decision surface"))

    assertEquals(surface.gapLabel, Some("45cp slight"))
    assert(surface.secondaryText.exists(_.contains("exchange sequence")), clue(surface))
  }

  test("allows same-first-move branch comparison when replayed line diverges later") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("Nf3"),
        engineBestMove = Some("Nf3"),
        comparedMove = Some("Nf3"),
        cpLossVsChosen = Some(45),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = true
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing same-first-move decision surface"))

    assertEquals(surface.chosenSan, Some("Nf3"))
    assertEquals(surface.engineSan, Some("Nf3"))
    assertEquals(surface.comparedSan, Some("Nf3"))
    assert(surface.secondaryText.exists(_.contains("which structure remains")), clue(surface))
  }

  test("non-blocking line consequence diagnostic tags do not close the player strip") {
    val evidence =
      surfaceLineEvidence().copy(rejectReasons = List("line_consequence:low_gap_diagnostic"))
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing decision surface with non-blocking diagnostic tag"))

    assertEquals(surface.gapLabel, Some("220cp"))
    assert(!surface.secondaryText.exists(_.contains("line_consequence:")), clue(surface))
  }

  test("role-aware player decision strip uses admitted alternative-comparison local fact") {
    val comparison = roleAwareDecisionComparison()
    val surface =
      MoveReviewPlayerPayloadBuilder
        .roleAwareDecisionComparisonSurface(Some(comparison), Some(roleAwareAlternativeLocalFact))
        .getOrElse(fail("missing role-aware player decision strip"))

    assertEquals(surface.kicker, "Decision point")
    assertEquals(surface.gapLabel, Some("96cp"))
    assertEquals(surface.chosenSan, Some("gxf5"))
    assertEquals(surface.engineSan, Some("exf5"))
    assertEquals(surface.comparedSan, Some("gxf5"))
    assert(surface.secondaryText.exists(_.contains("engine-best branch")), clue(surface))
    assertEquals(surface.refSans, List("exf5", "Rfd8", "Ne4", "Bd5", "gxf5", "a4", "h4", "Bh5"))
  }

  test("role-aware player decision strip requires CausalFrame local-fact admission") {
    val comparison = roleAwareDecisionComparison()

    assertEquals(MoveReviewPlayerPayloadBuilder.roleAwareDecisionComparisonSurface(Some(comparison), None), None)
    assertEquals(
      MoveReviewPlayerPayloadBuilder.roleAwareDecisionComparisonSurface(
        Some(comparison),
        Some(roleAwareAlternativeLocalFact.copy(authority = MoveReviewLocalFact.Authority.PvCoupledLine))
      ),
      None
    )
  }

  test("probe rows come from the certified ledger only") {
    val surface =
      build(
        moveReviewLedger =
          Some(
            MoveReviewStrategicLedger(
              motifKey = "validated_plan",
              motifLabel = "Validated plan",
              stageKey = "conversion",
              stageLabel = "Conversion",
              carryOver = false,
              primaryLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Aligned line",
                    sanMoves = List("Nf3", "Nc6"),
                    note = Some("certified line note"),
                    source = "probe"
                  )
                )
            )
          )
      )

    assert(!surface.probeRows.exists(_.source.contains("probe_request")), clue(surface.probeRows))
    assert(surface.probeRows.exists(_.text.contains("certified line note")), clue(surface.probeRows))
  }

  test("malformed ledger lines do not create player probe rows") {
    val rawNote = "raw request purpose should not surface"
    val surface =
      build(
        moveReviewLedger =
          Some(
            MoveReviewStrategicLedger(
              motifKey = "validated_plan",
              motifLabel = "Validated plan",
              stageKey = "conversion",
              stageLabel = "Conversion",
              carryOver = false,
              primaryLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Raw request line",
                    sanMoves = List("Nf3", "Nc6"),
                    note = Some(rawNote),
                    source = "probe_request"
                  )
                ),
              resourceLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Empty line",
                    sanMoves = Nil,
                    note = Some("note without SAN"),
                    source = "probe"
                  )
                )
            )
          )
      )

    assertEquals(surface.probeRows, Nil)
  }

  test("raw context probe requests do not create player rows or author meta") {
    val rawPurpose = "raw purpose from outbound request"
    val rawObjective = "raw objective from outbound request"
    val rawPlan = "Raw request-only plan"
    val rawSeed = "raw_request_seed"
    val question =
      AuthorQuestion(
        id = "why_this_raw_probe",
        kind = AuthorQuestionKind.WhyThis,
        priority = 1,
        question = "Why choose this route now?",
        why = Some("Needs certified evidence before surfacing request details."),
        confidence = ConfidenceLevel.Probe
      )
    val rawRequest =
      ProbeRequest(
        id = "raw_probe_request_1",
        fen = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.fen,
        moves = List("g2g4"),
        depth = 18,
        purpose = Some(rawPurpose),
        questionId = Some(question.id),
        questionKind = Some("WhyThis"),
        planName = Some(rawPlan),
        objective = Some(rawObjective),
        seedId = Some(rawSeed)
      )
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        probeRequests = List(rawRequest),
        authorQuestions = List(question)
      )
    val surface =
      build(
        ctx = ctx,
        authoringSurface = AuthoringEvidenceSummaryBuilder.build(ctx)
      )
    val rendered =
      (
        surface.summaryRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.advancedRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.probeRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.authorRows.flatMap(row => List(row.title, row.status, row.question) ++ row.meta)
      ).mkString(" ")

    assertEquals(surface.probeRows, Nil)
    assert(!rendered.contains(rawPurpose), clue(rendered))
    assert(!rendered.contains(rawObjective), clue(rendered))
    assert(!rendered.contains(rawPlan), clue(rendered))
    assert(!rendered.contains(rawSeed), clue(rendered))
  }

  test("author question titles split camel-case question kinds") {
    val surface =
      build(
        authoringSurface =
          AuthoringEvidenceSurface(
            questions =
              List(
                AuthorQuestionSummary(
                  id = "race",
                  kind = "WhosePlanIsFaster",
                  priority = 1,
                  question = "After Nb4, whose plan is faster?",
                  why = Some("Both sides have active ideas."),
                  anchors = Nil,
                  confidence = "Medium"
                ),
                AuthorQuestionSummary(
                  id = "why",
                  kind = "WhyThis",
                  priority = 2,
                  question = "Why choose Nb4?",
                  why = Some("The alternative also looks natural."),
                  anchors = Nil,
                  confidence = "Medium"
                )
              ),
            evidence = Nil,
            headline = None
          )
      )

    assertEquals(surface.authorRows.map(_.title), List("Whose Plan Is Faster", "Why This"))
    assert(!surface.authorRows.exists(row => row.title == "Whoseplanisfaster" || row.title == "Whythis"), clue(surface.authorRows))
  }

  test("authoring branch source ids are not exposed as player surface row sources") {
    val rawSourceId = "probe_result_raw_id"
    val surface =
      build(
        authoringSurface =
          AuthoringEvidenceSurface(
            questions = Nil,
            evidence =
              List(
                AuthorEvidenceSummary(
                  questionId = "why_this_1",
                  questionKind = "WhyThis",
                  question = "Why does this branch matter?",
                  status = "resolved",
                  branchCount = 1,
                  branches =
                    List(
                      EvidenceBranchSummary(
                        keyMove = "Nf3",
                        line = "Nf3 Nc6",
                        sourceId = Some(rawSourceId)
                      )
                    )
                )
              ),
            headline = None
          )
      )
    val sources = surface.authorRows.flatMap(_.branches.flatMap(_.source))

    assert(!sources.contains(rawSourceId), clue(sources))
    assertEquals(sources, Nil)
  }

  test("only typed probe-backed evaluated plans are promoted into player support rows") {
    val unverified =
      build(
        evaluatedPlans =
          List(evaluated(plan("Raw kingside pressure"), UserFacingPlanEligibility.Deferred))
      )
    assert(!unverified.summaryRows.exists(_.label == "Main plans"))
    assert(!unverified.advancedRows.exists(_.label == "Execution"))

    val verified =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift", evidenceSources = Nil),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    assert(
      verified.summaryRows.exists(row =>
        row.label == "Main plans" && row.text.contains("Validated rook lift")
      ),
      clue(verified.summaryRows)
    )
  }

  test("transposition-aligned evaluated plans are promoted without pretending to be probe-backed") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/2p5/3p4/1P1P4/8/8/4K3 w - - 0 1",
          engineEvidence = Some(persistentTargetEvidence())
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan("Transposed d5 pressure", evidenceSources = Nil),
              UserFacingPlanEligibility.TranspositionAligned,
              transpositionProofIds = List("transposition:staticweakness:d5:fixed_pawn"),
              claimCertification =
                ClaimCertification(
                  provenanceClass = PlayerFacingClaimProvenanceClass.TranspositionAligned
                )
            )
          )
      )

    assert(
      surface.summaryRows.exists(row =>
        row.label == "Main plans" && row.text.contains("Transposed d5 pressure")
      ),
      clue(surface.summaryRows)
    )
    assert(!surface.summaryRows.exists(_.label == "Practical plan"), clue(surface.summaryRows))
  }

  test("transposition-aligned evaluated plans require typed transposition provenance") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Malformed transposition pressure", evidenceSources = Nil),
              UserFacingPlanEligibility.TranspositionAligned,
              transpositionProofIds = List("transposition:staticweakness:d5:fixed_pawn")
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
  }

  test("structural-only evaluated plans create practical plan rows but not main plans") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Carlsbad pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
    assertEquals(surface.summaryRows.map(_.label), List("Practical plan"))
    assertEquals(
      surface.summaryRows.head.text,
      "The structure points toward Carlsbad pressure as a practical plan."
    )
    assertEquals(
      surface.summaryRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.summaryRows.head.refSans, Nil)
  }

  test("structural-only practical plan rows can include typed structure context") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Carlsbad", 0.82, Nil, "Locked", List("MAJORITY"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 74,
                    band = "Playable",
                    matchedPlanIds = List("MinorityAttack"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("minority attack"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.summaryRows.map(_.label), List("Practical plan"))
    assertEquals(
      surface.summaryRows.head.text,
      "In the Carlsbad structure with the center locked, minority attack is a practical plan."
    )
    assertEquals(
      surface.summaryRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.summaryRows.head.authority.flatMap(_.target), None)
  }

  test("low-confidence structural context does not enrich practical plan rows") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Carlsbad", 0.62, Nil, "Locked", List("MAJORITY"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 74,
                    band = "Playable",
                    matchedPlanIds = List("minority_attack"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("minority attack"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(
      surface.summaryRows.head.text,
      "The structure points toward minority attack as a practical plan."
    )
  }

  test("pv-coupled evaluated plans do not create practical plan rows without local fact authority") {
    val refs =
      MoveReviewRefs(
        startFen = InitialFen,
        startPly = 1,
        variations =
          List(
            MoveReviewVariationRef(
              lineId = "line_01",
              scoreCp = 15,
              mate = None,
              depth = 8,
              moves =
                List(
                  MoveReviewMoveRef("l01_m01", "e4", "e2e4", InitialFen, 1, 1, Some("1.")),
                  MoveReviewMoveRef("l01_m02", "e5", "e7e5", InitialFen, 2, 1, Some("1...")),
                  MoveReviewMoveRef("l01_m03", "Nf3", "g1f3", InitialFen, 3, 2, Some("2."))
                )
            )
          )
      )
    val surface =
      build(
        ctx = relationOnlyContext(InitialFen, "e2e4"),
        refs = Some(refs),
        evaluatedPlans =
          List(
            evaluated(
              plan("central pressure"),
              UserFacingPlanEligibility.PvCoupledOnly
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
    assert(!surface.summaryRows.exists(_.label == "Practical plan"), clue(surface.summaryRows))
  }

  test("structural-only evaluated plans create practical advanced detail rows") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Carlsbad pressure",
                preconditions = List("The queenside pawn chain stays fixed"),
                executionSteps = List("Prepare b4", "Pressure c6")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical objective", "Practical steps"))
    assert(surface.advancedRows.exists(row => row.text == "The queenside pawn chain stays fixed"))
    assert(surface.advancedRows.exists(row => row.text == "Prepare b4 - Pressure c6"))
    assert(surface.advancedRows.forall(_.tone.contains("practical")), clue(surface.advancedRows))
  }

  test("structural-only practical advanced rows can include structure route context") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        playedMove = Some("a1b1"),
        playedSan = Some("Rb1"),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Rook", "a1", 0.40, false, false, List("b1", "b3"), List("b4"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Carlsbad", 0.87, Nil, "Locked", List("MAJORITY"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 74,
                    band = "Playable",
                    matchedPlanIds = List("minority_attack"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH"),
                    narrativeIntent = Some("build queenside pressure"),
                    narrativeRisk = Some("the center can open if the pawn break is rushed")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("build queenside pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical move"))
    assertEquals(
      surface.advancedRows.head.text,
      "The rook wants the b-file because that is where queenside pressure gains force."
    )
    assertEquals(
      surface.advancedRows(1).text,
      "The played move starts that structure route immediately."
    )
    assertEquals(
      surface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val genericSurface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("queenside pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    assert(!genericSurface.advancedRows.exists(_.label == "Practical route"), clue(genericSurface.advancedRows))

    val unrelatedMoveSurface =
      build(
        ctx = ctx.copy(playedMove = Some("a1a8")),
        evaluatedPlans =
          List(
            evaluated(
              plan("build queenside pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    assert(unrelatedMoveSurface.advancedRows.exists(_.label == "Practical route"), clue(unrelatedMoveSurface.advancedRows))
    assert(!unrelatedMoveSurface.advancedRows.exists(_.label == "Practical move"), clue(unrelatedMoveSurface.advancedRows))
  }

  test("structural-only practical task rides only on a matched structure arc") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
        playedMove = Some("a1b1"),
        playedSan = Some("Rb1"),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Rook", "a1", 0.40, false, false, List("b1", "b3"), List("b4"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = Some(PracticalInfo(12, 0.64, "keep queenside pressure")),
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Carlsbad", 0.87, Nil, "Locked", List("MAJORITY"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 74,
                    band = "Playable",
                    matchedPlanIds = List("minority_attack"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH"),
                    narrativeIntent = Some("build queenside pressure"),
                    narrativeRisk = Some("the center can open if the pawn break is rushed")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("build queenside pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical move", "Practical task"))
    assertEquals(surface.advancedRows(2).text, "In practical terms the resulting task is keep queenside pressure.")
    assertEquals(surface.advancedRows(2).authority.flatMap(_.target), None)

    val unmatchedSurface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("king attack"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    assert(!unmatchedSurface.advancedRows.exists(_.label == "Practical task"), clue(unmatchedSurface.advancedRows))
  }

  test("structural-only route matching accepts typed matched plan ids") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/3N4/4K3 w - - 0 1",
        playedMove = Some("d2f3"),
        playedSan = Some("Nf3"),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Knight", "d2", 0.33, false, false, List("f3", "e5"), List("e5"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("IQP", 0.83, Nil, "Open", List("IQP"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 79,
                    band = "OnBook",
                    matchedPlanIds = List("central_break"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH"),
                    narrativeIntent = Some("prepare the central break"),
                    narrativeRisk = Some("the break fails if White is still underdeveloped")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("Central Break"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical move"))
    assert(surface.advancedRows.head.text.toLowerCase.contains("break"), clue(surface.advancedRows))
    assertEquals(
      surface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val genericSurface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("Break"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    assert(!genericSurface.advancedRows.exists(_.label == "Practical route"), clue(genericSurface.advancedRows))
  }

  test("structural-only practical advanced rows can include current-board restraint context") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1",
        playedMove = Some("c1d2"),
        playedSan = Some("Bd2"),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Bishop", "c1", 0.28, false, false, List("d2", "e3"), List("e3", "c5"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans =
                List(
                  PreventedPlanInfo("...b5 break", Nil, Some("b5"), 0, 120, Some("counterplay"))
                ),
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Hedgehog", 0.78, Nil, "Closed", List("HEDGEHOG"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 68,
                    band = "Playable",
                    matchedPlanIds = List("counterplay_restraint"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH"),
                    narrativeIntent = Some("hold the structure and restrain counterplay"),
                    narrativeRisk = Some("premature pawn breaks loosen the shell")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("hold the structure and restrain counterplay"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical move", "Practical restraint"))
    assertEquals(
      surface.advancedRows(2).text,
      "That deployment also matters because it cuts out counterplay."
    )
    assertEquals(surface.advancedRows(2).authority.flatMap(_.target), None)

    val lineScopedSurface =
      build(
        ctx = ctx.copy(
          semantic =
            ctx.semantic.map(semantic =>
              semantic.copy(
                preventedPlans =
                  semantic.preventedPlans.map(
                    _.copy(sourceScope = FactScope.ThreatLine, citationLine = Some("1...b5 2.Bd2"))
                  )
              )
            )
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan("hold the structure and restrain counterplay"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    assert(!lineScopedSurface.advancedRows.exists(_.label == "Practical restraint"), clue(lineScopedSurface.advancedRows))

    val unsupportedNowSurface =
      build(
        ctx = ctx.copy(
          semantic =
            ctx.semantic.map(semantic =>
              semantic.copy(
                preventedPlans =
                  semantic.preventedPlans.map(_.copy(counterplayScoreDrop = 0, mobilityDelta = 0))
              )
            )
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan("hold the structure and restrain counterplay"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
    )
    assert(!unsupportedNowSurface.advancedRows.exists(_.label == "Practical restraint"), clue(unsupportedNowSurface.advancedRows))
  }

  test("structural-only practical move can reach a later same-origin route square directly") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1",
        playedMove = Some("c1e3"),
        playedSan = Some("Be3"),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Bishop", "c1", 0.28, false, false, List("d2", "e3"), List("e3", "c5"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("Hedgehog", 0.78, Nil, "Closed", List("HEDGEHOG"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 68,
                    band = "Playable",
                    matchedPlanIds = List("counterplay_restraint"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH"),
                    narrativeIntent = Some("hold the structure and restrain counterplay"),
                    narrativeRisk = Some("premature pawn breaks loosen the shell")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("hold the structure and restrain counterplay"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical move"))
    assertEquals(
      surface.advancedRows(1).text,
      "The played move reaches that structure route directly."
    )

    val otherOriginSurface =
      build(
        ctx = ctx.copy(playedMove = Some("d2e3")),
        evaluatedPlans =
          List(
            evaluated(
              plan("hold the structure and restrain counterplay"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
    )
    assert(!otherOriginSurface.advancedRows.exists(_.label == "Practical move"), clue(otherOriginSurface.advancedRows))
  }

  test("structural-only practical fit can explain missing structure preconditions") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/8/8/8/3N4/4K3 w - - 0 1",
        playedMove = None,
        playedSan = None,
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity =
                List(
                  PieceActivityInfo("Knight", "d2", 0.33, false, false, List("f3", "e5"), List("e5"))
                ),
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures = None,
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil,
              structureProfile = Some(StructureProfileInfo("IQP", 0.83, Nil, "Open", List("IQP"))),
              planAlignment =
                Some(
                  PlanAlignmentInfo(
                    score = 61,
                    band = "Playable",
                    matchedPlanIds = List("central_break"),
                    missingPlanIds = Nil,
                    reasonCodes = List("PA_MATCH", "PRECOND_MISS"),
                    narrativeIntent = Some("prepare the central break"),
                    narrativeRisk = Some("the break fails if White is still underdeveloped")
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan("Central Break"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical route", "Practical fit"))
    assertEquals(
      surface.advancedRows(1).text,
      "The structure fit is still partial: some structural preconditions are missing."
    )
    assertEquals(surface.advancedRows(1).authority.flatMap(_.target), None)
  }

  test("pv-coupled evaluated plans do not create practical advanced detail rows without local fact authority") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "central pressure",
                preconditions = List("The e-file remains tense"),
                executionSteps = List("Keep the knight centralized")
              ),
              UserFacingPlanEligibility.PvCoupledOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("weakness practical plans include dynamic board target detail") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1",
          engineEvidence = Some(persistentTargetEvidence())
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(surface.advancedRows.exists(_.text.contains("weak isolated queen pawn on d5")), clue(surface.advancedRows))
    assert(surface.advancedRows.forall(_.authority.contains(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))))
  }

  test("weakness practical target row honors typed target hints over the first board target") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p1p2/8/8/8/4K3 w - - 0 1",
          engineEvidence = Some(persistentTargetEvidence())
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:f5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(surface.advancedRows.exists(_.text.contains("on f5")), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.text.contains("on d5")), clue(surface.advancedRows))
  }

  test("typed weakness target hints can publish practical target rows for non-weakness plans") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
          engineEvidence = Some(persistentTargetEvidence())
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Piece pressure on the weak pawn",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(surface.advancedRows.exists(_.text.contains("on e5")), clue(surface.advancedRows))
  }

  test("typed weakness target hints can publish practical target rows from legal best-line endpoints") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/4p3/8/8/8/8/8/4K3 w - - 0 1",
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations =
                  List(
                    VariationLine(
                      moves = List("e1e2", "e7e5", "e2e1", "e8e7", "e1e2"),
                      scoreCp = 12,
                      depth = 18
                    )
                  )
              )
            )
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Piece pressure on the future weak pawn",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(_.text.contains("checked line leaves Black a weak isolated pawn on e5")),
      clue(surface.advancedRows)
    )
  }

  test("new best-line endpoint target rows require a durable endpoint witness") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/4p3/8/8/8/8/8/4K3 w - - 0 1",
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations =
                  List(
                    VariationLine(
                      moves = List("e1e2", "e7e5"),
                      scoreCp = 12,
                      depth = 18
                    )
                  )
              )
            )
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Piece pressure on the future weak pawn",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
  }

  test("current-board weakness practical target context does not require a best-line persistence witness") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(_.text.contains("current structure gives Black a weak isolated pawn on e5")),
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("current-board weakness practical target context can publish without exact square hints") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(_.text.contains("current structure gives Black a weak isolated pawn on e5")),
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("current-board weakness practical target context can survive an empty best-line continuation") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations =
                  List(
                    VariationLine(
                      moves = Nil,
                      scoreCp = 12,
                      depth = 18,
                      resultingFen = Some("4k3/8/8/4p3/8/8/8/4K3 w - - 0 1")
                    )
                  )
              )
            )
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(_.text.contains("current structure gives Black a weak isolated pawn on e5")),
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("fixed target hints can surface Carlsbad board context without exact target authority") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "6k1/8/2p5/3p4/3P4/8/1P6/6K1 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("fixed_target:c6"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(
        _.text.contains("Carlsbad-type pawn shape makes c6 a natural queenside target")
      ),
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("relation-style material target tokens do not create practical target rows") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Piece pressure on a target",
                evidenceSources = List("target:e5:queen"),
                themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
  }

  test("weakness practical target row is suppressed when the best line liquidates the target") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(
                    moves = List("e1e2", "e5e4"),
                    scoreCp = 12,
                    depth = 18
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness fixation",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
                preconditions = List("Pressure the weak pawn"),
                executionSteps = List("Improve toward the target")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(_.label == "Practical objective"), clue(surface.advancedRows))
  }

  test("current-board weakness practical target context is not blocked by a short persistent best line") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(
                    moves = List("e1d1", "e8d8", "d1e1", "d8e8"),
                    scoreCp = 12,
                    depth = 18
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness fixation",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
                preconditions = List("Pressure the weak pawn")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target", "Practical objective"))
    assert(
      surface.advancedRows.exists(_.text.contains("current structure gives Black a weak isolated pawn on e5")),
      clue(surface.advancedRows)
    )
  }

  test("decision comparison carries best-vs-chosen target contrast metadata") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/3pp3/2N2N2/8/8/4K3 w - - 0 1",
        playedMove = Some("f4d5"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(moves = List("c4e5"), scoreCp = 80, depth = 18),
                  VariationLine(moves = List("f4d5"), scoreCp = 20, depth = 18)
                )
            )
          )
      )

    val surface =
      build(
        ctx = ctx,
        decisionComparisonSurface =
          Some(
            MoveReviewPlayerDecisionComparison(
              kicker = "Decision point",
              chosenSan = Some("Nxd5"),
              engineSan = Some("Nxe5"),
              secondaryText = Some("The checked line changes which pawn remains."),
              chosenMatchesBest = false
            )
          )
      )

    assertEquals(
      surface.decisionComparison.flatMap(_.targetComparison),
      Some(
        MoveReviewDecisionTargetComparison(
          chosenTarget = "e5",
          chosenTargetKind = "isolated_pawn",
          bestTarget = "d5",
          bestTargetKind = "iqp"
        )
      )
    )
  }

  test("decision target comparison requires a non-empty best-line move") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/3pp3/2N2N2/8/8/4K3 w - - 0 1",
        playedMove = Some("f4d5"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(
                    moves = Nil,
                    scoreCp = 80,
                    depth = 18,
                    resultingFen = Some("4k3/8/8/3pp3/2N2N2/8/8/4K3 w - - 0 1")
                  ),
                  VariationLine(moves = List("f4d5"), scoreCp = 20, depth = 18)
                )
            )
          )
      )

    val surface =
      build(
        ctx = ctx,
        decisionComparisonSurface =
          Some(
            MoveReviewPlayerDecisionComparison(
              kicker = "Decision point",
              chosenSan = Some("Nxd5"),
              engineSan = None,
              secondaryText = Some("The checked line changes which pawn remains."),
              chosenMatchesBest = false
            )
          )
      )

    assertEquals(surface.decisionComparison.flatMap(_.targetComparison), None)
  }

  test("practical advanced rows are skipped when practical plans have no details") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Carlsbad pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows, Nil)
  }

  test("structural-only Carlsbad plans can publish board context without exact target authority") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/2p5/3p4/3P4/8/1P6/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Carlsbad minority attack",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assertEquals(
      surface.advancedRows.head.text,
      "The Carlsbad-type pawn shape makes c6 a natural queenside target for White's minority-attack ideas."
    )
    assertEquals(
      surface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("structural-only backward-pawn targeting can publish Carlsbad board context without exact target authority") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/2p5/3p4/3P4/8/1P6/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Backward-pawn targeting",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assertEquals(
      surface.advancedRows.head.text,
      "The Carlsbad-type pawn shape makes c6 a natural queenside target for White's minority-attack ideas."
    )
    assertEquals(
      surface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("structural-only Carlsbad context supports the black-side mirror target") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/1p6/8/3p4/3P4/2P5/8/4K3 b - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Minority attack pressure",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assertEquals(
      surface.advancedRows.head.text,
      "The Carlsbad-type pawn shape makes c3 a natural queenside target for Black's minority-attack ideas."
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("structural-only Carlsbad context falls back to generic weakness context without the mirrored board shape") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/3P4/8/1P6/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Carlsbad minority attack",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(!surface.advancedRows.head.text.contains("Carlsbad-type"), clue(surface.advancedRows))
    assert(
      surface.advancedRows.exists(_.text.contains("current IQP structure gives Black a weak isolated queen pawn on d5")),
      clue(surface.advancedRows)
    )
  }

  test("current-board weakness target context can name a concrete typed structure") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/4p3/3pP3/3P4/8/8/4K3 w - - 0 1",
          engineEvidence = None
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(
      surface.advancedRows.exists(_.text.contains("current French Advance Chain structure gives Black")),
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)
  }

  test("probe-backed plans do not duplicate as practical plan rows") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assertEquals(surface.summaryRows.map(_.label), List("Main plans"))
    assert(!surface.summaryRows.exists(row => row.label == "Practical plan"), clue(surface.summaryRows))
  }

  test("probe-backed plans do not duplicate practical advanced detail rows") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The third rank is clear"),
        executionSteps = List("Lift the rook", "Swing to g3")
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows drop sibling plans with the same promoted theme") {
    val promoted =
      plan(
        "Validated kingside clamp",
        preconditions = List("The kingside dark squares are weak"),
        executionSteps = List("h4-h5", "h5-h6"),
        themeL1 = PlanTaxonomy.PlanTheme.SpaceClamp.id
      )
    val practical =
      plan(
        "Kingside file pressure",
        preconditions = List("The h-file can be opened"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.SpaceClamp.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assert(surface.summaryRows.exists(_.label == "Practical plan"), clue(surface.summaryRows))
    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows drop sibling plans with overlapping execution") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The rook can swing across"),
        executionSteps = List("h4-h5-h6"),
        themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
      )
    val practical =
      plan(
        "Kingside target fixation",
        preconditions = List("The h-pawn remains a hook"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows remain when promoted plan is not a sibling") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The rook can swing across"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
      )
    val practical =
      plan(
        "Central weakness pressure",
        preconditions = List("The d-pawn can become fixed"),
        executionSteps = List("King closer", "Pressure d5", "Keep central tension"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
      )
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1",
          engineEvidence = Some(persistentTargetEvidence())
        ),
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assertEquals(
      surface.advancedRows.map(_.label),
      List("Execution", "Objective", "Practical target", "Practical objective", "Practical steps")
    )
    assert(surface.advancedRows.exists(row => row.text == "King closer - Pressure d5"), clue(surface.advancedRows))
  }

  test("opening family row carries bounded public opening book metadata") {
    val fen =
      NarrativeUtils.uciListToFen(InitialFen, List("d2d4", "d7d5", "c2c4"))
    val openingRef =
      OpeningReference(
        eco = Some("D06"),
        name = Some("Queen's Gambit"),
        totalGames = 12345,
        topMoves =
          List(
            ExplorerMove("e7e6", "e6", 6000, 2200, 1600, 2200, 2450),
            ExplorerMove("g8f6", "Nf6", 4000, 1500, 1100, 1400, 2440),
            ExplorerMove("bad", "raw note", 1, 0, 0, 1, 1200)
          ),
        sampleGames = Nil
      )
    val surface =
      build(
        ctx =
          MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
            fen = fen,
            header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
            phase = PhaseContext("Opening", "Opening structure"),
            ply = 3,
            openingData = Some(openingRef)
          )
      )

    val authority =
      surface.summaryRows
        .find(_.label == "Opening family")
        .flatMap(_.authority)
        .getOrElse(fail(s"missing opening-family authority: ${surface.summaryRows}"))

    assertEquals(authority.kind, MoveReviewSurfaceAuthority.OpeningFamily)
    assertEquals(authority.openingFamily, Some("queens_gambit"))
    assertEquals(
      authority.openingBook,
      Some(MoveReviewOpeningBookMetadata(eco = Some("D06"), totalGames = Some(12345), topMoves = List("e6", "Nf6")))
    )
  }

  test("supported-local rows append to summary rows without leaking source metadata") {
    val supported =
      MoveReviewPlayerSurfaceRow(
        label = "Counterplay break",
        text = "A key idea is that this move stops the d5 break.",
        source = Some("counterplay_axis_suppression"),
        refSans = List("exd5"),
        authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          ),
        supportedLocalRows = List(supported, supported)
      )

    assertEquals(surface.summaryRows.map(_.label), List("Main plans", "Counterplay break"))
    assertEquals(surface.summaryRows.count(_.label == "Counterplay break"), 1)
    assertEquals(surface.summaryRows.flatMap(_.source), Nil)
    assertEquals(
      surface.summaryRows.find(_.label == "Counterplay break").flatMap(_.authority),
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
    )
    assert(surface.summaryRows.exists(_.text.contains("stops the d5 break")), clue(surface.summaryRows))
  }

  test("neutralize-key-break surface gate rejects route tokens ending on the played destination") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        playedMove = Some("e7e5")
      )

    assertEquals(
      NeutralizeKeyBreakSurfaceGate.decideForPacket(neutralizePacket("e4-e5"), ctx).rejectReason,
      Some(NeutralizeKeyBreakSurfaceGate.PlayedMoveCollision)
    )
  }

  test("target-pressure strategic ideas create bounded practical pressure rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_carlsbad_pressure",
        ownerSide = "white",
        kind = StrategicIdeaKind.TargetFixing,
        group = "slow_structural",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c6"),
        focusZone = Some("queenside"),
        confidence = 0.90,
        evidenceRefs =
          List(
            "source:carlsbad_fixation_profile",
            "minority_attack_semantic",
            "target_pressure_semantic",
            "target_pressure_new_weak_pawn_c6"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(surface.advancedRows.head.text, "The current pawn structure points queenside pressure toward c6.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val minoritySupportSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_minority_attack_support",
                    confidence = 0.56,
                    evidenceRefs = List("source:minority_attack_support", "minority_attack_support_queenside")
                  )
                )
            )
          )
      )
    assertEquals(minoritySupportSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(
      minoritySupportSurface.advancedRows.head.text,
      "The minority-attack structure points queenside pressure toward c6."
    )
    assertEquals(minoritySupportSurface.advancedRows.head.authority.flatMap(_.target), None)

    val minoritySupportSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_minority_attack_support_source_only",
                    evidenceRefs = List("source:minority_attack_support")
                  )
                )
            )
          )
    )
    assert(!minoritySupportSourceOnlySurface.advancedRows.exists(_.label == "Practical pressure"), clue(minoritySupportSourceOnlySurface.advancedRows))

    val carlsbadSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_carlsbad_source_only",
                    evidenceRefs = List("source:carlsbad_fixation_profile")
                  )
                )
            )
          )
      )
    assert(!carlsbadSourceOnlySurface.advancedRows.exists(_.label == "Practical pressure"), clue(carlsbadSourceOnlySurface.advancedRows))

    val carlsbadWrongSideSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_carlsbad_wrong_side",
                    ownerSide = "black",
                    evidenceRefs = List("source:carlsbad_fixation_profile", "target_pressure_semantic")
                  )
                )
            )
          )
      )
    assert(!carlsbadWrongSideSurface.advancedRows.exists(_.label == "Practical pressure"), clue(carlsbadWrongSideSurface.advancedRows))

    val minoritySemanticWrongSideSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_minority_semantic_wrong_side",
                    ownerSide = "black",
                    evidenceRefs = List("source:minority_attack_semantic", "target_pressure_semantic")
                  )
                )
            )
          )
      )
    assert(!minoritySemanticWrongSideSurface.advancedRows.exists(_.label == "Practical pressure"), clue(minoritySemanticWrongSideSurface.advancedRows))

    val minoritySupportWithoutTargetSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_minority_attack_support_without_target",
                    focusSquares = Nil,
                    evidenceRefs = List("source:minority_attack_support", "minority_attack_support_queenside")
                  )
                )
            )
          )
      )
    assert(!minoritySupportWithoutTargetSurface.advancedRows.exists(_.label == "Practical pressure"), clue(minoritySupportWithoutTargetSurface.advancedRows))

    val genericIdea =
      idea.copy(
        ideaId = "idea_generic_target",
        evidenceRefs = List("source:plan_match_target_fixing"),
        focusSquares = List("c6")
      )
    val genericSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(genericIdea)
            )
          )
    )
    assert(!genericSurface.advancedRows.exists(_.label == "Practical pressure"), clue(genericSurface.advancedRows))

    val compensationTargetSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_target_pressure",
                    focusSquares = List("b7"),
                    focusZone = Some("queenside"),
                    confidence = 0.78,
                    evidenceRefs =
                      List(
                        "source:compensation_target_fixation",
                        "compensation_target_fixation",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assertEquals(compensationTargetSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(
      compensationTargetSurface.advancedRows.head.text,
      "The compensation structure keeps practical queenside pressure on b7."
    )
    assertEquals(compensationTargetSurface.advancedRows.head.authority.flatMap(_.target), None)

    val compensationSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_target_source_only",
                    focusSquares = List("b7"),
                    confidence = 0.90,
                    evidenceRefs = List("source:compensation_target_fixation", "material_deficit_compensation")
                  )
                )
            )
          )
      )
    assert(!compensationSourceOnlySurface.advancedRows.exists(_.label == "Practical pressure"), clue(compensationSourceOnlySurface.advancedRows))

    val weakSquareIdea =
      idea.copy(
        ideaId = "idea_enemy_weak_square",
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5"),
        focusZone = Some("center"),
        confidence = 0.74,
        evidenceRefs = List("source:enemy_weak_square", "enemy_weak_square_d5")
      )
    val weakSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(weakSquareIdea)
            )
          )
      )
    assertEquals(weakSquareSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(weakSquareSurface.advancedRows.head.text, "The current weak-square map gives a practical pressure cue around d5.")
    assertEquals(weakSquareSurface.advancedRows.head.authority.flatMap(_.target), None)

    val weakComplexSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  weakSquareIdea.copy(
                    ideaId = "idea_weak_complex",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("c6", "d5"),
                    focusZone = Some("queenside"),
                    confidence = 0.72,
                    evidenceRefs = List("source:weak_complex_fixation", "weak_complex_backward_pawn")
                  )
                )
            )
          )
      )
    assertEquals(weakComplexSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(weakComplexSurface.advancedRows.head.text, "The current pawn weaknesses give a practical pressure cue around c6, d5.")
    assertEquals(weakComplexSurface.advancedRows.head.authority.flatMap(_.target), None)

    val doubledPawnIdea =
      weakSquareIdea.copy(
        ideaId = "idea_doubled_pawn_pressure",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = Nil,
        focusFiles = List("d"),
        focusZone = Some("center"),
        confidence = 0.70,
        evidenceRefs =
          List(
            "source:doubled_pawn_pressure_motif",
            "doubled_pawn_pressure_shape",
            "doubled_pawn_file_d"
          )
      )
    val doubledPawnSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(doubledPawnIdea)
            )
          )
      )
    assertEquals(doubledPawnSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(doubledPawnSurface.advancedRows.head.text, "The doubled-pawn structure gives a practical pressure cue on the d-file.")
    assertEquals(doubledPawnSurface.advancedRows.head.authority.flatMap(_.target), None)

    val doubledPawnMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  doubledPawnIdea.copy(
                    ideaId = "idea_doubled_pawn_matched_fact",
                    focusFiles = List("c", "d")
                  )
                )
            )
          )
      )
    assertEquals(doubledPawnMatchedFactSurface.advancedRows.map(_.label), List("Practical pressure"))
    assertEquals(doubledPawnMatchedFactSurface.advancedRows.head.text, "The doubled-pawn structure gives a practical pressure cue on the d-file.")
    assertEquals(doubledPawnMatchedFactSurface.advancedRows.head.authority.flatMap(_.target), None)

    val doubledPawnWithTargetPlanSurface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:d5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(doubledPawnIdea.copy(focusSquares = List("d5")))
            )
          )
      )
    assert(doubledPawnWithTargetPlanSurface.advancedRows.exists(_.label == "Practical target"), clue(doubledPawnWithTargetPlanSurface.advancedRows))
    assertEquals(
      doubledPawnWithTargetPlanSurface.advancedRows.find(_.label == "Practical pressure").map(_.text),
      Some("The doubled-pawn structure gives a practical pressure cue on the d-file.")
    )

    val doubledPawnSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  doubledPawnIdea.copy(
                    ideaId = "idea_doubled_pawn_source_only",
                    evidenceRefs = List("source:doubled_pawn_pressure_motif")
                  )
                )
            )
          )
      )
    assert(!doubledPawnSourceOnlySurface.advancedRows.exists(_.label == "Practical pressure"), clue(doubledPawnSourceOnlySurface.advancedRows))

    val doubledPawnShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  doubledPawnIdea.copy(
                    ideaId = "idea_doubled_pawn_shape_only",
                    evidenceRefs = List("source:doubled_pawn_pressure_motif", "doubled_pawn_pressure_shape")
                  )
                )
            )
          )
      )
    assert(!doubledPawnShapeOnlySurface.advancedRows.exists(_.label == "Practical pressure"), clue(doubledPawnShapeOnlySurface.advancedRows))

    val doubledPawnNoFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  doubledPawnIdea.copy(
                    ideaId = "idea_doubled_pawn_no_file",
                    focusFiles = Nil
                  )
                )
            )
          )
      )
    assert(!doubledPawnNoFileSurface.advancedRows.exists(_.label == "Practical pressure"), clue(doubledPawnNoFileSurface.advancedRows))

    val sourceOnlyWeakSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  weakSquareIdea.copy(
                    ideaId = "idea_weak_square_source_only",
                    evidenceRefs = List("source:enemy_weak_square")
                  )
                )
            )
          )
      )
    assert(!sourceOnlyWeakSquareSurface.advancedRows.exists(_.label == "Practical pressure"), clue(sourceOnlyWeakSquareSurface.advancedRows))

    val targetPlanSurface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:d5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(weakSquareIdea)
            )
          )
    )
    assert(targetPlanSurface.advancedRows.exists(_.label == "Practical target"), clue(targetPlanSurface.advancedRows))
    assert(!targetPlanSurface.advancedRows.exists(_.text == weakSquareSurface.advancedRows.head.text), clue(targetPlanSurface.advancedRows))

    val differentTargetPlanSurface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:e5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(weakSquareIdea)
            )
          )
      )
    assert(differentTargetPlanSurface.advancedRows.exists(_.label == "Practical pressure"), clue(differentTargetPlanSurface.advancedRows))
    assertEquals(
      differentTargetPlanSurface.advancedRows.find(_.label == "Practical pressure").map(_.text),
      Some("The current weak-square map gives a practical pressure cue around d5.")
    )

    val partialPracticalTargetSurface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                evidenceSources = List("weakness_target:d5"),
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  weakSquareIdea.copy(
                    ideaId = "idea_enemy_weak_square_partial_practical_target",
                    focusSquares = List("d5", "e6"),
                    evidenceRefs = List("source:enemy_weak_square", "enemy_weak_square_d5", "enemy_weak_square_e6")
                  )
                )
            )
          )
      )
    assert(partialPracticalTargetSurface.advancedRows.exists(_.label == "Practical target"), clue(partialPracticalTargetSurface.advancedRows))
    assertEquals(
      partialPracticalTargetSurface.advancedRows.find(_.label == "Practical pressure").map(_.text),
      Some("The current weak-square map gives a practical pressure cue around e6.")
    )

    val partialExactTargetSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Fixed target",
              text = "The checked line keeps d5 fixed as the target.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  weakSquareIdea.copy(
                    ideaId = "idea_enemy_weak_square_partial_exact",
                    focusSquares = List("d5", "e6"),
                    evidenceRefs = List("source:enemy_weak_square", "enemy_weak_square_d5", "enemy_weak_square_e6")
                  )
                )
            )
          )
      )
    assertEquals(
      partialExactTargetSurface.advancedRows.find(_.label == "Practical pressure").map(_.text),
      Some("The current weak-square map gives a practical pressure cue around e6.")
    )

    val exactFixedTargetSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Fixed target",
              text = "The checked line keeps c6 fixed as the target.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(!exactFixedTargetSurface.advancedRows.exists(_.label == "Practical pressure"), clue(exactFixedTargetSurface.advancedRows))

    val exactMinorityTargetSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Minority attack",
              text = "The checked line keeps c6 as the minority-attack fixed target.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(!exactMinorityTargetSurface.advancedRows.exists(_.label == "Practical pressure"), clue(exactMinorityTargetSurface.advancedRows))
  }

  test("color-complex clamp strategic ideas create bounded practical space rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_dark_square_clamp",
        ownerSide = "white",
        kind = StrategicIdeaKind.SpaceGainOrRestriction,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("f6", "h6", "g7"),
        focusZone = Some("dark-square complex"),
        confidence = 0.80,
        evidenceRefs =
          List(
            "source:color_complex_clamp",
            "enemy_color_complex_weakness",
            "color_complex_dark"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(surface.advancedRows.head.text, "The current structure clamps the dark-square complex around f6, h6, g7.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val tokenOnlyZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_dark_square_clamp_token_only", focusZone = None))
            )
          )
      )
    assertEquals(tokenOnlyZoneSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(tokenOnlyZoneSurface.advancedRows.head.text, "The current structure clamps the dark-square complex around f6, h6, g7.")

    val staleFocusZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_dark_square_clamp_stale_zone", focusZone = Some("light-square complex")))
            )
          )
      )
    assertEquals(staleFocusZoneSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(staleFocusZoneSurface.advancedRows.head.text, "The current structure clamps the dark-square complex around f6, h6, g7.")

    val ambiguousTokenSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_color_complex_ambiguous_tokens",
                    focusZone = Some("dark-square complex"),
                    evidenceRefs =
                      List(
                        "source:color_complex_clamp",
                        "enemy_color_complex_weakness",
                        "color_complex_dark",
                        "color_complex_light"
                      )
                  )
                )
            )
          )
      )
    assert(!ambiguousTokenSurface.advancedRows.exists(_.label == "Practical space"), clue(ambiguousTokenSurface.advancedRows))

    val wrongSideColorComplexSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_dark_square_clamp_wrong_side", ownerSide = "black"))
            )
          )
      )
    assert(!wrongSideColorComplexSurface.advancedRows.exists(_.label == "Practical space"), clue(wrongSideColorComplexSurface.advancedRows))

    val exactColorComplexSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Color complex",
              text = "The checked line keeps the knight on c4 attacking e5 in the dark-square complex.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(!exactColorComplexSurface.advancedRows.exists(_.label == "Practical space"), clue(exactColorComplexSurface.advancedRows))

    val malformedRoleColorComplexSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Color complex",
              text = "The checked line keeps the queen on c4 attacking e5 in the dark-square complex.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(malformedRoleColorComplexSurface.advancedRows.exists(_.label == "Practical space"), clue(malformedRoleColorComplexSurface.advancedRows))

    val malformedGeometryColorComplexSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Color complex",
              text = "The checked line keeps the bishop on c4 attacking e5 in the dark-square complex.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(malformedGeometryColorComplexSurface.advancedRows.exists(_.label == "Practical space"), clue(malformedGeometryColorComplexSurface.advancedRows))

    val staleColorComplexSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Color complex",
              text = "The current structure clamps the dark-square complex around f6, h6, g7.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(staleColorComplexSurface.advancedRows.exists(_.label == "Practical space"), clue(staleColorComplexSurface.advancedRows))

    val genericSpace =
      idea.copy(
        ideaId = "idea_generic_space",
        focusSquares = Nil,
        focusZone = Some("center"),
        evidenceRefs = List("source:central_space_edge")
      )
    val genericSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(genericSpace)
            )
          )
      )
    assert(!genericSurface.advancedRows.exists(_.label == "Practical space"), clue(genericSurface.advancedRows))

    val centralSpaceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_central_space_edge",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.74,
                    evidenceRefs = List("source:central_space_edge", "central_space_edge_shape")
                  )
                )
            )
          )
      )
    assertEquals(centralSpaceSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(centralSpaceSurface.advancedRows.head.text, "The current position gives a practical central-space edge.")
    assertEquals(centralSpaceSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifSpaceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_motif_space_advantage",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.76,
                    evidenceRefs = List("source:space_advantage_motif", "space_advantage_motif_shape", "space_pawn_delta_3")
                  )
                )
            )
          )
      )
    assertEquals(motifSpaceSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(motifSpaceSurface.advancedRows.head.text, "The current motif map gives a practical central-space cue.")
    assertEquals(motifSpaceSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralPawnAdvanceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_central_pawn_advance",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:central_pawn_advance_motif",
                        "central_pawn_advance_shape",
                        "central_pawn_file_d",
                        "central_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assertEquals(centralPawnAdvanceSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(centralPawnAdvanceSurface.advancedRows.head.text, "The central pawn advance gives a practical d-file space cue.")
    assertEquals(centralPawnAdvanceSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralPawnAdvanceMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_central_pawn_advance_matched_fact",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g", "d"),
                    focusZone = Some("center"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:central_pawn_advance_motif",
                        "central_pawn_advance_shape",
                        "central_pawn_file_d",
                        "central_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assertEquals(centralPawnAdvanceMatchedFactSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(centralPawnAdvanceMatchedFactSurface.advancedRows.head.text, "The central pawn advance gives a practical d-file space cue.")
    assertEquals(centralPawnAdvanceMatchedFactSurface.advancedRows.head.authority.flatMap(_.target), None)

    val pawnChainSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_pawn_chain_space",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g", "h"),
                    focusZone = Some("kingside"),
                    confidence = 0.74,
                    evidenceRefs = List("source:pawn_chain_space_motif", "pawn_chain_space_shape", "pawn_chain_g_h")
                  )
                )
            )
          )
      )
    assertEquals(pawnChainSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(pawnChainSurface.advancedRows.head.text, "The pawn chain gives a practical kingside-space cue.")
    assertEquals(pawnChainSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiFlankPawnChainSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_multi_flank_pawn_chain_space",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("a", "b", "g", "h"),
                    focusZone = Some("kingside"),
                    confidence = 0.74,
                    evidenceRefs =
                      List(
                        "source:pawn_chain_space_motif",
                        "pawn_chain_space_shape",
                        "pawn_chain_a_b",
                        "pawn_chain_g_h"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiFlankPawnChainSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(multiFlankPawnChainSurface.advancedRows.head.text, "The pawn chain gives a practical flank-space cue.")
    assertEquals(multiFlankPawnChainSurface.advancedRows.head.authority.flatMap(_.target), None)

    val pawnChainSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_pawn_chain_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g", "h"),
                    focusZone = Some("kingside"),
                    confidence = 0.74,
                    evidenceRefs = List("source:pawn_chain_space_motif")
                  )
                )
            )
          )
      )
    assert(!pawnChainSourceOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(pawnChainSourceOnlySurface.advancedRows))

    val pawnChainShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_pawn_chain_shape_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g", "h"),
                    focusZone = Some("kingside"),
                    confidence = 0.74,
                    evidenceRefs = List("source:pawn_chain_space_motif", "pawn_chain_space_shape")
                  )
                )
            )
          )
      )
    assert(!pawnChainShapeOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(pawnChainShapeOnlySurface.advancedRows))

    val motifSpaceSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_motif_space_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.76,
                    evidenceRefs = List("source:space_advantage_motif")
                  )
                )
            )
          )
      )
    assert(!motifSpaceSourceOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(motifSpaceSourceOnlySurface.advancedRows))

    val motifSpaceWeakDeltaSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_motif_space_weak_delta",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.70,
                    evidenceRefs = List("source:space_advantage_motif", "space_advantage_motif_shape", "space_pawn_delta_1")
                  )
                )
            )
          )
      )
    assert(!motifSpaceWeakDeltaSurface.advancedRows.exists(_.label == "Practical space"), clue(motifSpaceWeakDeltaSurface.advancedRows))

    val centralPawnAdvanceSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_central_pawn_advance_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.70,
                    evidenceRefs = List("source:central_pawn_advance_motif")
                  )
                )
            )
          )
      )
    assert(!centralPawnAdvanceSourceOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(centralPawnAdvanceSourceOnlySurface.advancedRows))

    val centralPawnAdvanceFlankFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_central_pawn_advance_flank",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g"),
                    focusZone = Some("center"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:central_pawn_advance_motif",
                        "central_pawn_advance_shape",
                        "central_pawn_file_g",
                        "central_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assert(!centralPawnAdvanceFlankFileSurface.advancedRows.exists(_.label == "Practical space"), clue(centralPawnAdvanceFlankFileSurface.advancedRows))

    val motifSpaceMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_motif_space_mirrored",
                    ownerSide = "white",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.76,
                    evidenceRefs = List("source:space_advantage_motif", "space_advantage_motif_shape", "space_pawn_delta_3")
                  )
                )
            )
          )
      )
    assert(!motifSpaceMirroredSurface.advancedRows.exists(_.label == "Practical space"), clue(motifSpaceMirroredSurface.advancedRows))

    val mobilityBindSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_mobility_restriction",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.72,
                    evidenceRefs = List("source:mobility_restriction", "mobility_restriction_shape")
                  )
                )
            )
          )
      )
    assertEquals(mobilityBindSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(mobilityBindSurface.advancedRows.head.text, "The current position gives a practical mobility bind.")
    assertEquals(mobilityBindSurface.advancedRows.head.authority.flatMap(_.target), None)

    val onePieceMobilityGapSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  genericSpace.copy(
                    ideaId = "idea_one_piece_mobility_gap",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.70,
                    evidenceRefs = List("source:mobility_restriction", "mobility_restriction_shape")
                  )
                )
            )
          )
      )
    assert(!onePieceMobilityGapSurface.advancedRows.exists(_.label == "Practical space"), clue(onePieceMobilityGapSurface.advancedRows))

    val lockedCenterSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_locked_center_bind",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.70,
                    evidenceRefs = List("source:locked_center_bind", "structure_locked_center")
                  )
                )
            )
          )
      )
    assertEquals(lockedCenterSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(lockedCenterSurface.advancedRows.head.text, "The locked center gives a practical central-space bind.")
    assertEquals(lockedCenterSurface.advancedRows.head.authority.flatMap(_.target), None)

    val lockedCenterFactOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_locked_center_fact_only",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.76,
                    evidenceRefs = List("structure_locked_center")
                  )
                )
            )
          )
      )
    assert(!lockedCenterFactOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(lockedCenterFactOnlySurface.advancedRows))

    val maroczySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_maroczy_bind",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.86,
                    evidenceRefs = List("source:maroczy_bind_profile", "structure_maroczy_bind")
                  )
                )
            )
          )
      )
    assertEquals(maroczySurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(maroczySurface.advancedRows.head.text, "The current Maroczy bind gives a practical central-space grip.")
    assertEquals(maroczySurface.advancedRows.head.authority.flatMap(_.target), None)

    val wrongPackMaroczySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_maroczy_bind_wrong_pack",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.86,
                    evidenceRefs = List("source:maroczy_bind_profile", "structure_maroczy_bind")
                  )
                )
            )
          )
      )
    assert(!wrongPackMaroczySurface.advancedRows.exists(_.label == "Practical space"), clue(wrongPackMaroczySurface.advancedRows))

    val iqpSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_presence",
                    focusSquares = List("d4"),
                    focusZone = Some("center"),
                    confidence = 0.82,
                    evidenceRefs = List("source:iqp_central_presence", "structure_iqp_white", "iqp_central_presence_shape")
                  )
                )
            )
          )
      )
    assertEquals(iqpSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(iqpSurface.advancedRows.head.text, "The current IQP structure gives a practical central-space cue around d4.")
    assertEquals(iqpSurface.advancedRows.head.authority.flatMap(_.target), None)

    val wrongPackIqpSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_presence_wrong_pack",
                    focusSquares = List("d4"),
                    focusZone = Some("center"),
                    confidence = 0.82,
                    evidenceRefs = List("source:iqp_central_presence", "structure_iqp_white", "iqp_central_presence_shape")
                  )
                )
            )
          )
      )
    assert(!wrongPackIqpSurface.advancedRows.exists(_.label == "Practical space"), clue(wrongPackIqpSurface.advancedRows))

    val mirroredProfileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_mirrored_iqp_presence",
                    ownerSide = "black",
                    focusSquares = List("d4"),
                    focusZone = Some("center"),
                    confidence = 0.82,
                    evidenceRefs = List("source:iqp_central_presence", "structure_iqp_white", "iqp_central_presence_shape")
                  )
                )
            )
          )
      )
    assert(!mirroredProfileSurface.advancedRows.exists(_.label == "Practical space"), clue(mirroredProfileSurface.advancedRows))

    val iqpBridgeSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_bridge",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.84,
                    evidenceRefs = List("source:iqp_space_bridge", "structure_iqp_white")
                  )
                )
            )
          )
      )
    assertEquals(iqpBridgeSurface.advancedRows.map(_.label), List("Practical space"))
    assertEquals(
      iqpBridgeSurface.advancedRows.head.text,
      "The current IQP structure gives a practical central-space cue."
    )
    assertEquals(iqpBridgeSurface.advancedRows.head.authority.flatMap(_.target), None)

    val iqpBridgeSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_bridge_source_only",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.84,
                    evidenceRefs = List("source:iqp_space_bridge")
                  )
                )
            )
          )
      )
    assert(!iqpBridgeSourceOnlySurface.advancedRows.exists(_.label == "Practical space"), clue(iqpBridgeSourceOnlySurface.advancedRows))

    val mirroredIqpBridgeSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_mirrored_iqp_bridge",
                    ownerSide = "black",
                    focusSquares = Nil,
                    focusZone = Some("center"),
                    confidence = 0.84,
                    evidenceRefs = List("source:iqp_space_bridge", "structure_iqp_white")
                  )
                )
            )
          )
      )
    assert(!mirroredIqpBridgeSurface.advancedRows.exists(_.label == "Practical space"), clue(mirroredIqpBridgeSurface.advancedRows))
  }

  test("structural support surface keeps three independent practical idea families") {
    val pressureIdea =
      StrategyIdeaSignal(
        ideaId = "idea_carlsbad_pressure",
        ownerSide = "white",
        kind = StrategicIdeaKind.TargetFixing,
        group = "slow_structural",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c6"),
        focusZone = Some("queenside"),
        confidence = 0.90,
        evidenceRefs = List("source:carlsbad_fixation_profile", "target_pressure_semantic")
      )
    val secondPressureIdea =
      pressureIdea.copy(
        ideaId = "idea_second_weak_square_pressure",
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5"),
        focusZone = Some("center"),
        confidence = 0.74,
        evidenceRefs = List("source:enemy_weak_square", "enemy_weak_square_d5")
      )
    val routeLineIdea =
      StrategyIdeaSignal(
        ideaId = "idea_route_line_control",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c4"),
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.60,
        evidenceRefs =
          List(
            "source:line_control_features",
            "line_control_shape",
            "source:route_line_access",
            "route_surface_exact",
            "semi_open_file_c"
          )
      )
    val tradeIdea =
      StrategyIdeaSignal(
        ideaId = "idea_iqp_trade_down",
        ownerSide = "white",
        kind = StrategicIdeaKind.FavorableTradeOrTransformation,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("d5"),
        confidence = 0.78,
        evidenceRefs = List("source:iqp_simplification_profile", "structure_iqp_black", "capture_or_exchange")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(pressureIdea, secondPressureIdea, routeLineIdea, tradeIdea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical pressure", "Practical pressure", "Practical line", "Practical trade"))
    assertEquals(surface.advancedRows.head.text, "The current pawn structure points queenside pressure toward c6.")
    assertEquals(surface.advancedRows(1).text, "The current weak-square map gives a practical pressure cue around d5.")
    assertEquals(surface.advancedRows(2).text, "The rook route gives a practical c-file line cue.")
    assertEquals(surface.advancedRows(3).text, "The IQP structure gives White a practical trade-down cue around d5.")
    assert(surface.advancedRows.forall(_.authority.flatMap(_.target).isEmpty), clue(surface.advancedRows))
  }

  test("outer advanced cap preserves relation compensation and three structural families") {
    val relationIdeas =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        MoveReviewExchangeAnalyzer.RelationKind.Fork,
        MoveReviewExchangeAnalyzer.RelationKind.Pin
      ).zipWithIndex.map { case (kind, idx) =>
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_dense_relation_$idx",
          focusSquares = List(s"a${idx + 1}", s"b${idx + 1}", s"c${idx + 1}"),
          targetSquare = Some(s"c${idx + 1}")
        )
      }
    val pressureIdea =
      StrategyIdeaSignal(
        ideaId = "idea_dense_carlsbad_pressure",
        ownerSide = "black",
        kind = StrategicIdeaKind.TargetFixing,
        group = "slow_structural",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c3"),
        focusZone = Some("queenside"),
        confidence = 0.90,
        evidenceRefs = List("source:carlsbad_fixation_profile", "target_pressure_semantic")
      )
    val lineIdea =
      StrategyIdeaSignal(
        ideaId = "idea_dense_route_line",
        ownerSide = "black",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c4"),
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.60,
        evidenceRefs =
          List(
            "source:line_control_features",
            "line_control_shape",
            "source:route_line_access",
            "route_surface_exact",
            "semi_open_file_c"
          )
      )
    val conversionIdea =
      StrategyIdeaSignal(
        ideaId = "idea_dense_conversion",
        ownerSide = "black",
        kind = StrategicIdeaKind.FavorableTradeOrTransformation,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d4"),
        confidence = 0.80,
        evidenceRefs = List("source:winning_endgame_transition", "winning_endgame_transition_shape")
      )
    val densePack =
      anchoredCompensationPack.copy(
        strategicIdeas =
          anchoredCompensationPack.strategicIdeas ++
            relationIdeas ++
            List(pressureIdea, lineIdea, conversionIdea)
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                name = "Dense extra plan",
                executionSteps = List("Keep the extra detail visible"),
                preconditions = List("The extra plan condition holds")
              ),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          ),
        strategyPack = Some(densePack)
      )

    assertEquals(surface.advancedRows.size, 10, clue(surface.advancedRows))
    assertEquals(
      surface.advancedRows.filter(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)).size,
      4,
      clue(surface.advancedRows)
    )
    assertEquals(surface.advancedRows.count(_.label.startsWith("Compensation")), 2, clue(surface.advancedRows))
    assertEquals(
      surface.advancedRows.filter(_.label.startsWith("Practical")).map(_.label),
      List("Practical pressure", "Practical line"),
      clue(surface.advancedRows)
    )
    assert(surface.advancedRows.filter(_.label.startsWith("Practical")).forall(_.authority.flatMap(_.target).isEmpty), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(row => row.label == "Execution"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(row => row.label == "Objective"), clue(surface.advancedRows))
  }

  test("pawn-break strategic ideas create bounded practical break rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_d_file_break",
        ownerSide = "white",
        kind = StrategicIdeaKind.PawnBreak,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Ready,
        focusFiles = List("d"),
        focusZone = Some("center"),
        confidence = 0.92,
        evidenceRefs =
          List(
            "source:pawn_analysis_break_ready",
            "pawn_analysis_break_ready_shape",
            "break_file_d"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(surface.advancedRows.head.text, "The current pawn structure gives a practical d-file break cue.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val breakReadyMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_break_ready_matched_fact", focusFiles = List("c", "d")))
            )
          )
      )
    assertEquals(breakReadyMatchedFactSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(breakReadyMatchedFactSurface.advancedRows.head.text, "The current pawn structure gives a practical d-file break cue.")
    assertEquals(breakReadyMatchedFactSurface.advancedRows.head.authority.flatMap(_.target), None)

    val genericBreak =
      idea.copy(
        ideaId = "idea_generic_break",
        confidence = 0.94,
        evidenceRefs = List("source:plan_match_break_preparation", "plan_match_break_preparation")
      )
    val genericSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(genericBreak)
            )
          )
      )
    assert(!genericSurface.advancedRows.exists(_.label == "Practical break"), clue(genericSurface.advancedRows))

    val centralTensionSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_central_tension", confidence = 0.89))
            )
          )
      )
    assert(!centralTensionSurface.advancedRows.exists(_.label == "Practical break"), clue(centralTensionSurface.advancedRows))

    val fileOpeningSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_file_opening",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("e"),
                    confidence = 0.72,
                    evidenceRefs = List("source:file_opening_consequence", "contested_file_e")
                  )
                )
            )
          )
      )
    assertEquals(fileOpeningSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(fileOpeningSurface.advancedRows.head.text, "The current pawn tension gives a practical e-file opening cue.")
    assertEquals(fileOpeningSurface.advancedRows.head.authority.flatMap(_.target), None)

    val fileOpeningMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_file_opening_matched_fact",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c", "e"),
                    confidence = 0.72,
                    evidenceRefs = List("source:file_opening_consequence", "contested_file_e")
                  )
                )
            )
          )
      )
    assertEquals(fileOpeningMatchedFactSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(fileOpeningMatchedFactSurface.advancedRows.head.text, "The current pawn tension gives a practical e-file opening cue.")
    assertEquals(fileOpeningMatchedFactSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralBreakTensionSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_central_break_tension",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.78,
                    evidenceRefs = List("source:central_break_tension", "locked_center")
                  )
                )
            )
          )
      )
    assertEquals(centralBreakTensionSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(centralBreakTensionSurface.advancedRows.head.text, "The central tension gives a practical d-file break cue.")
    assertEquals(centralBreakTensionSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralBreakTensionMatchedFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_central_break_tension_matched_file",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("g", "d"),
                    focusZone = Some("center"),
                    confidence = 0.78,
                    evidenceRefs = List("source:central_break_tension", "locked_center")
                  )
                )
            )
          )
      )
    assertEquals(centralBreakTensionMatchedFileSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(centralBreakTensionMatchedFileSurface.advancedRows.head.text, "The central tension gives a practical d-file break cue.")
    assertEquals(centralBreakTensionMatchedFileSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralBreakTensionMultiCentralFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_central_break_tension_multi_central_file",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c", "d"),
                    focusZone = Some("center"),
                    confidence = 0.78,
                    evidenceRefs = List("source:central_break_tension", "locked_center")
                  )
                )
            )
          )
      )
    assertEquals(centralBreakTensionMultiCentralFileSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(
      centralBreakTensionMultiCentralFileSurface.advancedRows.head.text,
      "The central tension gives a practical central-break cue."
    )
    assertEquals(centralBreakTensionMultiCentralFileSurface.advancedRows.head.authority.flatMap(_.target), None)

    val pawnBreakMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_pawn_break_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d", "e"),
                    focusZone = Some("center"),
                    confidence = 0.76,
                    evidenceRefs =
                      List(
                        "source:pawn_break_motif",
                        "pawn_break_motif_shape",
                        "break_file_d",
                        "break_target_file_e"
                      )
                  )
                )
            )
          )
      )
    assertEquals(pawnBreakMotifSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(pawnBreakMotifSurface.advancedRows.head.text, "The pawn break motif gives a practical d-file break cue.")
    assertEquals(pawnBreakMotifSurface.advancedRows.head.authority.flatMap(_.target), None)

    val pawnBreakMotifMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_pawn_break_motif_matched_fact",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c", "d"),
                    focusZone = Some("center"),
                    confidence = 0.76,
                    evidenceRefs =
                      List(
                        "source:pawn_break_motif",
                        "pawn_break_motif_shape",
                        "break_file_d"
                      )
                  )
                )
            )
          )
      )
    assertEquals(pawnBreakMotifMatchedFactSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(pawnBreakMotifMatchedFactSurface.advancedRows.head.text, "The pawn break motif gives a practical d-file break cue.")
    assertEquals(pawnBreakMotifMatchedFactSurface.advancedRows.head.authority.flatMap(_.target), None)

    val pawnBreakMotifSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_pawn_break_motif_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.76,
                    evidenceRefs = List("source:pawn_break_motif")
                  )
                )
            )
          )
      )
    assert(!pawnBreakMotifSourceOnlySurface.advancedRows.exists(_.label == "Practical break"), clue(pawnBreakMotifSourceOnlySurface.advancedRows))

    val pawnBreakMotifShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_pawn_break_motif_shape_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.76,
                    evidenceRefs = List("source:pawn_break_motif", "pawn_break_motif_shape")
                  )
                )
            )
          )
      )
    assert(!pawnBreakMotifShapeOnlySurface.advancedRows.exists(_.label == "Practical break"), clue(pawnBreakMotifShapeOnlySurface.advancedRows))

    val centralBreakSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_central_break_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.90,
                    evidenceRefs = List("source:central_break_tension")
                  )
                )
            )
          )
      )
    assert(!centralBreakSourceOnlySurface.advancedRows.exists(_.label == "Practical break"), clue(centralBreakSourceOnlySurface.advancedRows))

    val centralBreakMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_central_break_mirrored",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("d"),
                    focusZone = Some("center"),
                    confidence = 0.90,
                    evidenceRefs = List("source:central_break_tension", "locked_center")
                  )
                )
            )
          )
      )
    assert(!centralBreakMirroredSurface.advancedRows.exists(_.label == "Practical break"), clue(centralBreakMirroredSurface.advancedRows))

    val sourceOnlyFileOpeningSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_file_opening_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("e"),
                    confidence = 0.90,
                    evidenceRefs = List("source:file_opening_consequence")
                  )
                )
            )
          )
      )
    assert(!sourceOnlyFileOpeningSurface.advancedRows.exists(_.label == "Practical break"), clue(sourceOnlyFileOpeningSurface.advancedRows))

    val frenchSeed =
      idea.copy(
        ideaId = "idea_french_f6_seed",
        ownerSide = "black",
        readiness = StrategicIdeaReadiness.Build,
        focusFiles = List("f"),
        focusSquares = List("e5", "f6"),
        confidence = 0.92,
        evidenceRefs =
          List(
            "source:french_f6_break_seed",
            "french_f6_break_seed_shape",
            "white_e5_chain",
            "black_f7_break_pawn"
          )
      )
    val frenchSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(frenchSeed)
            )
          )
      )
    assertEquals(frenchSurface.advancedRows.map(_.label), List("Practical break"))
    assertEquals(frenchSurface.advancedRows.head.text, "The French Advance chain gives Black a practical ...f6 break cue.")
    assertEquals(frenchSurface.advancedRows.head.authority.flatMap(_.target), None)

    val frenchProfileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  frenchSeed.copy(
                    ideaId = "idea_french_profile_only",
                    evidenceRefs = List("source:french_counterbreak_profile", "structure_french_advance_chain", "french_f6_break")
                  )
                )
            )
          )
      )
    assert(!frenchProfileSurface.advancedRows.exists(_.label == "Practical break"), clue(frenchProfileSurface.advancedRows))

    val mirroredFrenchSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(frenchSeed.copy(ideaId = "idea_mirrored_french_seed", ownerSide = "white"))
            )
          )
      )
    assert(!mirroredFrenchSurface.advancedRows.exists(_.label == "Practical break"), clue(mirroredFrenchSurface.advancedRows))
  }

  test("counterplay geometry strategic ideas create bounded practical restraint rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_hedgehog_break_denial",
        ownerSide = "white",
        kind = StrategicIdeaKind.CounterplaySuppression,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Build,
        focusFiles = List("b", "d"),
        focusZone = Some("queenside"),
        confidence = 0.92,
        evidenceRefs =
          List(
            "source:hedgehog_break_denial_geometry",
            "structure_hedgehog",
            "hedgehog_break_denial_shape"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(surface.advancedRows.head.text, "The Hedgehog structure gives White a practical brake on Black's queenside breaks.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val broadHedgehogSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_broad_hedgehog",
                    confidence = 0.94,
                    evidenceRefs = List("source:hedgehog_containment_profile", "structure_hedgehog", "hedgehog_containment_profile")
                  )
                )
            )
          )
      )
    assert(!broadHedgehogSurface.advancedRows.exists(_.label == "Practical restraint"), clue(broadHedgehogSurface.advancedRows))

    val maroczySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_maroczy_break_denial",
                    focusFiles = List("c", "d"),
                    focusZone = Some("center"),
                    confidence = 0.88,
                    evidenceRefs =
                      List(
                        "source:maroczy_break_denial_geometry",
                        "structure_maroczy_bind",
                        "maroczy_break_denial_shape"
                      )
                  )
                )
            )
          )
      )
    assertEquals(maroczySurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(maroczySurface.advancedRows.head.text, "The Maroczy bind gives White a practical brake on Black's central breaks.")
    assertEquals(maroczySurface.advancedRows.head.authority.flatMap(_.target), None)

    val broadMaroczySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_broad_maroczy",
                    focusFiles = List("c", "d"),
                    focusZone = Some("center"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:maroczy_counterplay_suppression",
                        "structure_maroczy_bind",
                        "maroczy_counterplay_suppression"
                      )
                  )
                )
            )
          )
      )
    assert(!broadMaroczySurface.advancedRows.exists(_.label == "Practical restraint"), clue(broadMaroczySurface.advancedRows))

    val opponentCounterbreakSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opponent_counterbreak",
                    confidence = 0.94,
                    evidenceRefs = List("source:opponent_counterbreak_denial", "opponent_counterbreak_denial")
                  )
                )
            )
          )
      )
    assert(!opponentCounterbreakSurface.advancedRows.exists(_.label == "Practical restraint"), clue(opponentCounterbreakSurface.advancedRows))

    val opponentCounterbreakDenialSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opponent_counterbreak_denial",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.80,
                    evidenceRefs = List("source:opponent_counterbreak_denial", "opponent_counter_break")
                  )
                )
            )
          )
      )
    assertEquals(opponentCounterbreakDenialSurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(
      opponentCounterbreakDenialSurface.advancedRows.head.text,
      "The current structure gives a practical brake on the opponent's c-file counterbreak."
    )
    assertEquals(opponentCounterbreakDenialSurface.advancedRows.head.authority.flatMap(_.target), None)

    val counterplaySuppressionSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_break",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.82,
                    evidenceRefs =
                      List(
                        "source:counterplay_suppression",
                        "counterplay_suppression_shape",
                        "counterplay_break_denial",
                        "break_neutralized",
                        "denied_break_resource"
                      )
                  )
                )
            )
          )
      )
    assertEquals(counterplaySuppressionSurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(
      counterplaySuppressionSurface.advancedRows.head.text,
      "The current structure gives a practical brake on the opponent's c-file break."
    )
    assertEquals(counterplaySuppressionSurface.advancedRows.head.authority.flatMap(_.target), None)

    val compensationCounterplayDenialSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_denial",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.78,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "material_deficit_compensation",
                        "break_neutralized"
                      )
                  )
                )
            )
          )
      )
    assertEquals(compensationCounterplayDenialSurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(
      compensationCounterplayDenialSurface.advancedRows.head.text,
      "The compensation structure gives a practical brake on the opponent's c-file break."
    )
    assertEquals(compensationCounterplayDenialSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiFileCompensationCounterplayDenialSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_multi_file_compensation_counterplay_denial",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c", "d"),
                    focusZone = Some("center"),
                    confidence = 0.78,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "material_deficit_compensation",
                        "break_neutralized"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiFileCompensationCounterplayDenialSurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(
      multiFileCompensationCounterplayDenialSurface.advancedRows.head.text,
      "The compensation structure gives a practical brake on the opponent's breaks."
    )
    assertEquals(multiFileCompensationCounterplayDenialSurface.advancedRows.head.authority.flatMap(_.target), None)

    val passerBlockadeIdea =
      idea.copy(
        ideaId = "idea_passer_blockade_restraint",
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5", "d4"),
        focusFiles = List("d"),
        focusZone = Some("center"),
        confidence = 0.76,
        beneficiaryPieces = List("N"),
        evidenceRefs =
          List(
            "source:passer_blockade_motif",
            "passer_blockade_shape",
            "blockade_square_d5",
            "blockaded_pawn_d4"
          )
      )
    val passerBlockadeSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(passerBlockadeIdea)
            )
          )
      )
    assertEquals(passerBlockadeSurface.advancedRows.map(_.label), List("Practical restraint"))
    assertEquals(
      passerBlockadeSurface.advancedRows.head.text,
      "The blockade gives a practical brake on the passed pawn on d4."
    )
    assertEquals(passerBlockadeSurface.advancedRows.head.authority.flatMap(_.target), None)

    val passerBlockadeWithRouteDenialSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Route denial",
              text = "The checked line keeps c5 closed, takes the c-file away, and cuts off the d6 reroute.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(passerBlockadeIdea.copy(ideaId = "idea_passer_blockade_with_route_denial"))
            )
          )
      )
    assert(passerBlockadeWithRouteDenialSurface.advancedRows.exists(_.label == "Practical restraint"), clue(passerBlockadeWithRouteDenialSurface.advancedRows))
    assertEquals(
      passerBlockadeWithRouteDenialSurface.advancedRows.find(_.label == "Practical restraint").map(_.text),
      Some("The blockade gives a practical brake on the passed pawn on d4.")
    )

    val passerBlockadeSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  passerBlockadeIdea.copy(
                    ideaId = "idea_passer_blockade_source_only",
                    evidenceRefs = List("source:passer_blockade_motif")
                  )
                )
            )
          )
      )
    assert(!passerBlockadeSourceOnlySurface.advancedRows.exists(_.label == "Practical restraint"), clue(passerBlockadeSourceOnlySurface.advancedRows))

    val passerBlockadeShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  passerBlockadeIdea.copy(
                    ideaId = "idea_passer_blockade_shape_only",
                    evidenceRefs = List("source:passer_blockade_motif", "passer_blockade_shape")
                  )
                )
            )
          )
      )
    assert(!passerBlockadeShapeOnlySurface.advancedRows.exists(_.label == "Practical restraint"), clue(passerBlockadeShapeOnlySurface.advancedRows))

    val passerBlockadeWithoutPawnSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  passerBlockadeIdea.copy(
                    ideaId = "idea_passer_blockade_without_pawn",
                    evidenceRefs =
                      List(
                        "source:passer_blockade_motif",
                        "passer_blockade_shape",
                        "blockade_square_d5"
                      )
                  )
                )
            )
          )
      )
    assert(!passerBlockadeWithoutPawnSurface.advancedRows.exists(_.label == "Practical restraint"), clue(passerBlockadeWithoutPawnSurface.advancedRows))

    val compensationCounterplaySourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.90,
                    evidenceRefs = List("source:compensation_counterplay_denial")
                  )
                )
            )
          )
      )
    assert(
      !compensationCounterplaySourceOnlySurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(compensationCounterplaySourceOnlySurface.advancedRows)
    )

    val compensationCounterplayWithoutBreakSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_without_break",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assert(
      !compensationCounterplayWithoutBreakSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(compensationCounterplayWithoutBreakSurface.advancedRows)
    )

    val compensationCounterplayWithoutMaterialSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_without_material",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c"),
                    focusZone = Some("queenside"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "break_neutralized"
                      )
                  )
                )
            )
          )
      )
    assert(
      !compensationCounterplayWithoutMaterialSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(compensationCounterplayWithoutMaterialSurface.advancedRows)
    )

    val counterplaySuppressionSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs = List("source:counterplay_suppression")
                  )
                )
            )
          )
      )
    assert(
      !counterplaySuppressionSourceOnlySurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(counterplaySuppressionSourceOnlySurface.advancedRows)
    )

    val counterplaySuppressionScoreOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_score_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:counterplay_suppression",
                        "counterplay_suppression_shape",
                        "counterplay_score_drop"
                      )
                  )
                )
            )
          )
      )
    assert(
      !counterplaySuppressionScoreOnlySurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(counterplaySuppressionScoreOnlySurface.advancedRows)
    )

    val counterplaySuppressionMergedRefsSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_merged_refs",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:counterplay_suppression",
                        "counterplay_suppression_shape",
                        "break_neutralized",
                        "denied_break_resource"
                      )
                  )
                )
            )
          )
      )
    assert(
      !counterplaySuppressionMergedRefsSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(counterplaySuppressionMergedRefsSurface.advancedRows)
    )

    val exactCounterplaySurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Route denial",
              text = "The checked line keeps c5 closed, takes the c-file away, and cuts off the d6 reroute.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_with_exact_row",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:counterplay_suppression",
                        "counterplay_suppression_shape",
                        "counterplay_break_denial",
                        "break_neutralized",
                        "denied_break_resource"
                      )
                  )
                )
            )
          )
      )
    assert(!exactCounterplaySurface.advancedRows.exists(_.label == "Practical restraint"), clue(exactCounterplaySurface.advancedRows))

    val staleCounterplaySurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Route denial",
              text = "The checked line denies the key break.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_counterplay_suppression_with_stale_route_label",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:counterplay_suppression",
                        "counterplay_suppression_shape",
                        "counterplay_break_denial",
                        "break_neutralized",
                        "denied_break_resource"
                      )
                  )
                )
            )
          )
      )
    assert(staleCounterplaySurface.advancedRows.exists(_.label == "Practical restraint"), clue(staleCounterplaySurface.advancedRows))

    val opponentCounterbreakWithoutFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opponent_counterbreak_without_file",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusFiles = Nil,
                    confidence = 0.84,
                    evidenceRefs = List("source:opponent_counterbreak_denial", "opponent_counter_break")
                  )
                )
            )
          )
      )
    assert(
      !opponentCounterbreakWithoutFileSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(opponentCounterbreakWithoutFileSurface.advancedRows)
    )

    val compensationCounterplayWithoutFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_without_file",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = Nil,
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "material_deficit_compensation",
                        "break_neutralized"
                      )
                  )
                )
            )
          )
      )
    assert(
      !compensationCounterplayWithoutFileSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(compensationCounterplayWithoutFileSurface.advancedRows)
    )

    val compensationCounterplayMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_counterplay_mirrored",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c"),
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:compensation_counterplay_denial",
                        "material_deficit_compensation",
                        "break_neutralized"
                      )
                  )
                )
            )
          )
      )
    assert(
      !compensationCounterplayMirroredSurface.advancedRows.exists(_.label == "Practical restraint"),
      clue(compensationCounterplayMirroredSurface.advancedRows)
    )

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(idea.copy(ideaId = "idea_mirrored_hedgehog", ownerSide = "black"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical restraint"), clue(mirroredSurface.advancedRows))
  }

  test("occupied rook line strategic ideas create bounded practical line rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_occupied_c_file",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("c1"),
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.81,
        evidenceRefs =
          List(
            "source:occupied_line_control",
            "occupied_r_c1",
            "open_file_c"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(surface.advancedRows.head.text, "The rook already has a practical c-file post.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val openFileTagSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_open_file_tag",
                    focusSquares = Nil,
                    evidenceRefs = List("source:open_file_control", "open_file_c")
                  )
                )
            )
          )
      )
    assertEquals(openFileTagSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(openFileTagSurface.advancedRows.head.text, "The open c-file gives a practical major-piece line cue.")
    assertEquals(openFileTagSurface.advancedRows.head.authority.flatMap(_.target), None)

    val openFileMatchedFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_open_file_matched_fact",
                    focusSquares = Nil,
                    focusFiles = List("d", "c"),
                    evidenceRefs = List("source:open_file_control", "open_file_c")
                  )
                )
            )
          )
      )
    assertEquals(openFileMatchedFactSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(openFileMatchedFactSurface.advancedRows.head.text, "The open c-file gives a practical major-piece line cue.")

    val semiOpenFileTagSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_semi_open_file_tag",
                    focusSquares = Nil,
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.78,
                    evidenceRefs = List("source:semi_open_file_control", "semi_open_file_c")
                  )
                )
            )
          )
      )
    assertEquals(semiOpenFileTagSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(semiOpenFileTagSurface.advancedRows.head.text, "The semi-open c-file gives a practical major-piece line cue.")
    assertEquals(semiOpenFileTagSurface.advancedRows.head.authority.flatMap(_.target), None)

    val broadOpenFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_open_file_without_file_fact",
                    focusSquares = Nil,
                    evidenceRefs = List("source:open_file_control")
                  )
                )
            )
          )
      )
    assert(!broadOpenFileSurface.advancedRows.exists(_.label == "Practical line"), clue(broadOpenFileSurface.advancedRows))

    val openFileWithoutMajorPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_open_file_without_major_piece",
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:open_file_control", "open_file_c")
                  )
                )
            )
          )
      )
    assert(!openFileWithoutMajorPieceSurface.advancedRows.exists(_.label == "Practical line"), clue(openFileWithoutMajorPieceSurface.advancedRows))

    val broadSemiOpenFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_semi_open_file_without_file_fact",
                    focusSquares = Nil,
                    confidence = 0.80,
                    evidenceRefs = List("source:semi_open_file_control")
                  )
                )
            )
          )
      )
    assert(!broadSemiOpenFileSurface.advancedRows.exists(_.label == "Practical line"), clue(broadSemiOpenFileSurface.advancedRows))

    val semiOpenFileWithoutMajorPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_semi_open_file_without_major_piece",
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.80,
                    evidenceRefs = List("source:semi_open_file_control", "semi_open_file_c")
                  )
                )
            )
          )
      )
    assert(!semiOpenFileWithoutMajorPieceSurface.advancedRows.exists(_.label == "Practical line"), clue(semiOpenFileWithoutMajorPieceSurface.advancedRows))

    val routeLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_route_line",
                    evidenceRefs = List("source:route_line_access", "route_line_access", "open_file_c")
                  )
                )
            )
          )
    )
    assert(!routeLineSurface.advancedRows.exists(_.label == "Practical line"), clue(routeLineSurface.advancedRows))

    val directionalRookLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_rook_line",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("c4"),
                    focusFiles = List("c"),
                    confidence = 0.50,
                    evidenceRefs = List("source:directional_line_access", "directional_line_access_shape", "semi_open_file_c")
                  )
                )
            )
          )
      )
    assertEquals(directionalRookLineSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(directionalRookLineSurface.advancedRows.head.text, "The rook is aimed at practical line-play on the c-file.")
    assertEquals(directionalRookLineSurface.advancedRows.head.authority.flatMap(_.target), None)

    val routeLineControlSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_route_line_control",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = List("c4"),
                    focusFiles = List("c"),
                    confidence = 0.60,
                    evidenceRefs =
                      List(
                        "source:line_control_features",
                        "line_control_shape",
                        "source:route_line_access",
                        "route_surface_exact",
                        "semi_open_file_c"
                      )
                  )
                )
            )
          )
      )
    assertEquals(routeLineControlSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(routeLineControlSurface.advancedRows.head.text, "The rook route gives a practical c-file line cue.")
    assertEquals(routeLineControlSurface.advancedRows.head.authority.flatMap(_.target), None)

    val lineControlSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_line_control_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = List("c4"),
                    focusFiles = List("c"),
                    confidence = 0.70,
                    evidenceRefs = List("source:line_control_features", "line_control_shape")
                  )
                )
            )
          )
      )
    assert(!lineControlSourceOnlySurface.advancedRows.exists(_.label == "Practical line"), clue(lineControlSourceOnlySurface.advancedRows))

    val directionalLineWithoutFileFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_line_without_file_fact",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("c4"),
                    focusFiles = List("c"),
                    confidence = 0.56,
                    evidenceRefs = List("source:directional_line_access", "directional_line_access_shape")
                  )
                )
            )
          )
      )
    assert(
      !directionalLineWithoutFileFactSurface.advancedRows.exists(_.label == "Practical line"),
      clue(directionalLineWithoutFileFactSurface.advancedRows)
    )

    val directionalQueenLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_queen_line",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("c4"),
                    focusFiles = List("c"),
                    beneficiaryPieces = List("Q"),
                    confidence = 0.56,
                    evidenceRefs = List("source:directional_line_access", "directional_line_access_shape", "semi_open_file_c")
                  )
                )
            )
          )
      )
    assert(!directionalQueenLineSurface.advancedRows.exists(_.label == "Practical line"), clue(directionalQueenLineSurface.advancedRows))

    val doubledRooksSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_doubled_rooks_line",
                    focusSquares = Nil,
                    focusFiles = List("c"),
                    confidence = 0.74,
                    evidenceRefs = List("source:doubled_rooks", "doubled_rooks_c")
                  )
                )
            )
          )
      )
    assertEquals(doubledRooksSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(doubledRooksSurface.advancedRows.head.text, "The doubled rooks give a practical c-file line cue.")
    assertEquals(doubledRooksSurface.advancedRows.head.authority.flatMap(_.target), None)

    val rookOnSeventhSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_on_seventh_line",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    focusZone = Some("back rank"),
                    confidence = 0.72,
                    evidenceRefs = List("source:rook_on_seventh", "rook_on_seventh_shape")
                  )
                )
            )
          )
      )
    assertEquals(rookOnSeventhSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(rookOnSeventhSurface.advancedRows.head.text, "The rook on the seventh rank gives a practical line cue.")
    assertEquals(rookOnSeventhSurface.advancedRows.head.authority.flatMap(_.target), None)

    val connectedRooksSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_connected_rooks_line",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.64,
                    evidenceRefs = List("source:connected_rooks", "connected_rooks_shape")
                  )
                )
            )
          )
      )
    assertEquals(connectedRooksSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(
      connectedRooksSurface.advancedRows.head.text,
      "The connected rooks give a practical major-piece coordination cue."
    )
    assertEquals(connectedRooksSurface.advancedRows.head.authority.flatMap(_.target), None)

    val compensationOpenLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_open_line",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.70,
                    beneficiaryPieces = List("R", "Q"),
                    evidenceRefs =
                      List(
                        "source:compensation_open_lines",
                        "compensation_open_lines_shape",
                        "material_deficit_compensation",
                        "semi_open_file_c"
                      )
                  )
                )
            )
          )
      )
    assertEquals(compensationOpenLineSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(compensationOpenLineSurface.advancedRows.head.text, "The compensation gives practical line-play on the c-file.")
    assertEquals(compensationOpenLineSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiFileCompensationOpenLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_multi_file_compensation_open_line",
                    readiness = StrategicIdeaReadiness.Build,
                    focusFiles = List("c", "d"),
                    confidence = 0.74,
                    beneficiaryPieces = List("R", "Q"),
                    evidenceRefs =
                      List(
                        "source:compensation_open_lines",
                        "compensation_open_lines_shape",
                        "material_deficit_compensation",
                        "semi_open_file_c",
                        "open_file_d"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiFileCompensationOpenLineSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(multiFileCompensationOpenLineSurface.advancedRows.head.text, "The compensation gives practical line-play.")
    assertEquals(multiFileCompensationOpenLineSurface.advancedRows.head.authority.flatMap(_.target), None)

    val delayedRecoveryLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_delayed_recovery_line",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.74,
                    beneficiaryPieces = List("Q"),
                    evidenceRefs =
                      List(
                        "source:delayed_recovery_window",
                        "delayed_material_recovery",
                        "development_lead_compensation",
                        "material_deficit_compensation",
                        "open_file_c"
                      )
                  )
                )
            )
          )
      )
    assertEquals(delayedRecoveryLineSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(
      delayedRecoveryLineSurface.advancedRows.head.text,
      "The c-file pressure gives practical line-play before winning the material back."
    )
    assertEquals(delayedRecoveryLineSurface.advancedRows.head.authority.flatMap(_.target), None)

    val compensationSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_compensation_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.82,
                    beneficiaryPieces = List("R", "Q"),
                    focusFiles = Nil,
                    evidenceRefs =
                      List(
                        "source:compensation_open_lines",
                        "compensation_open_lines_shape",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assert(
      !compensationSourceOnlySurface.advancedRows.exists(_.label == "Practical line"),
      clue(compensationSourceOnlySurface.advancedRows)
    )

    val rookOnSeventhSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_on_seventh_source_only",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    confidence = 0.80,
                    evidenceRefs = List("source:rook_on_seventh")
                  )
                )
            )
          )
      )
    assert(
      !rookOnSeventhSourceOnlySurface.advancedRows.exists(_.label == "Practical line"),
      clue(rookOnSeventhSourceOnlySurface.advancedRows)
    )

    val connectedRooksSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_connected_rooks_source_only",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    confidence = 0.80,
                    evidenceRefs = List("source:connected_rooks")
                  )
                )
            )
          )
      )
    assert(
      !connectedRooksSourceOnlySurface.advancedRows.exists(_.label == "Practical line"),
      clue(connectedRooksSourceOnlySurface.advancedRows)
    )

    val doubledRooksWithoutFileFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_doubled_rooks_without_file_fact",
                    focusSquares = Nil,
                    focusFiles = List("c"),
                    confidence = 0.80,
                    evidenceRefs = List("source:doubled_rooks")
                  )
                )
            )
          )
      )
    assert(
      !doubledRooksWithoutFileFactSurface.advancedRows.exists(_.label == "Practical line"),
      clue(doubledRooksWithoutFileFactSurface.advancedRows)
    )

    val exactSeventhRankSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Seventh-rank entry",
              text = "The checked line puts the rook on the seventh rank at c7.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_on_seventh_with_exact_entry",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    confidence = 0.80,
                    evidenceRefs = List("source:rook_on_seventh", "rook_on_seventh_shape")
                  )
                )
            )
          )
    )
    assert(!exactSeventhRankSurface.advancedRows.exists(_.label == "Practical line"), clue(exactSeventhRankSurface.advancedRows))

    val exactRookSeventhWithQueenSeventhSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Seventh-rank entry",
              text = "The checked line puts the rook on the seventh rank at c7.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_queen_on_seventh_with_exact_rook_entry",
                    focusSquares = List("d7"),
                    focusFiles = Nil,
                    beneficiaryPieces = List("Q"),
                    confidence = 0.80,
                    evidenceRefs = List("source:occupied_line_control", "occupied_q_d7", "occupied_seventh_rank")
                  )
                )
            )
          )
      )
    assertEquals(
      exactRookSeventhWithQueenSeventhSurface.advancedRows.find(_.label == "Practical line").map(_.text),
      Some("The queen on the seventh rank gives a practical line cue.")
    )

    val staleSeventhRankSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Seventh-rank entry",
              text = "The rook on the seventh rank gives a practical line cue.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_on_seventh_with_stale_entry",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    confidence = 0.80,
                    evidenceRefs = List("source:rook_on_seventh", "rook_on_seventh_shape")
                  )
                )
            )
          )
      )
    assert(staleSeventhRankSurface.advancedRows.exists(_.label == "Practical line"), clue(staleSeventhRankSurface.advancedRows))

    val exactConnectedRooksSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Connected rooks",
              text = "The checked line connects the rooks on the first rank.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_connected_rooks_with_exact_row",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.80,
                    evidenceRefs = List("source:connected_rooks", "connected_rooks_shape")
                  )
                )
            )
          )
      )
    assert(!exactConnectedRooksSurface.advancedRows.exists(_.label == "Practical line"), clue(exactConnectedRooksSurface.advancedRows))

    val staleConnectedRooksSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Connected rooks",
              text = "The connected rooks give a practical major-piece coordination cue.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_connected_rooks_with_stale_row",
                    focusSquares = Nil,
                    focusFiles = Nil,
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.80,
                    evidenceRefs = List("source:connected_rooks", "connected_rooks_shape")
                  )
                )
            )
          )
      )
    assert(staleConnectedRooksSurface.advancedRows.exists(_.label == "Practical line"), clue(staleConnectedRooksSurface.advancedRows))

    val exactDoubledRooksSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Doubled rooks",
              text = "The checked line doubles the rooks on the c-file.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_doubled_rooks_with_exact_row",
                    focusSquares = Nil,
                    focusFiles = List("c"),
                    confidence = 0.80,
                    evidenceRefs = List("source:doubled_rooks", "doubled_rooks_c")
                  )
                )
            )
          )
    )
    assert(!exactDoubledRooksSurface.advancedRows.exists(_.label == "Practical line"), clue(exactDoubledRooksSurface.advancedRows))

    val differentFileDoubledRooksSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Doubled rooks",
              text = "The checked line doubles the rooks on the d-file.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_doubled_rooks_with_other_file_exact_row",
                    focusSquares = Nil,
                    focusFiles = List("c"),
                    confidence = 0.80,
                    evidenceRefs = List("source:doubled_rooks", "doubled_rooks_c")
                  )
                )
            )
          )
      )
    assert(differentFileDoubledRooksSurface.advancedRows.exists(_.label == "Practical line"), clue(differentFileDoubledRooksSurface.advancedRows))
    assertEquals(
      differentFileDoubledRooksSurface.advancedRows.find(_.label == "Practical line").map(_.text),
      Some("The doubled rooks give a practical c-file line cue.")
    )

    val staleDoubledRooksSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Doubled rooks",
              text = "The doubled rooks give a practical c-file line cue.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_doubled_rooks_with_stale_row",
                    focusSquares = Nil,
                    focusFiles = List("c"),
                    confidence = 0.80,
                    evidenceRefs = List("source:doubled_rooks", "doubled_rooks_c")
                  )
                )
            )
          )
      )
    assert(staleDoubledRooksSurface.advancedRows.exists(_.label == "Practical line"), clue(staleDoubledRooksSurface.advancedRows))

    val queenLineSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_queen_file",
                    beneficiaryPieces = List("Q"),
                    evidenceRefs =
                      List(
                        "source:occupied_line_control",
                        "occupied_line_control",
                        "occupied_q_c1",
                        "open_file_c"
                      )
                  )
                )
            )
          )
      )
    assertEquals(queenLineSurface.advancedRows.map(_.label), List("Practical line"))
    assertEquals(queenLineSurface.advancedRows.head.text, "The queen already has a practical c-file post.")
    assertEquals(queenLineSurface.advancedRows.head.authority.flatMap(_.target), None)

    val queenLineWithoutSquareFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_queen_file_without_square_fact",
                    beneficiaryPieces = List("Q"),
                    evidenceRefs =
                      List(
                        "source:occupied_line_control",
                        "occupied_line_control",
                        "open_file_c"
                      )
                  )
                )
            )
          )
      )
    assert(
      !queenLineWithoutSquareFactSurface.advancedRows.exists(_.label == "Practical line"),
      clue(queenLineWithoutSquareFactSurface.advancedRows)
    )

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(idea.copy(ideaId = "idea_mirrored_line"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical line"), clue(mirroredSurface.advancedRows))

    val exactLineSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "File entry",
              text = "The checked line keeps pressure on c6 through the c-file.",
              tone = Some("practical"),
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("c6")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_line_with_exact_file_entry"))
            )
          )
    )
    assert(!exactLineSurface.advancedRows.exists(_.label == "Practical line"), clue(exactLineSurface.advancedRows))
  }

  test("exact file entry on another file does not suppress lower-authority line context") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_open_file_tag",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.80,
        evidenceRefs = List("source:open_file_control", "open_file_c")
      )
    val surface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "File entry",
              text = "The checked line keeps pressure on e6 through the e-file.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(surface.advancedRows.exists(_.label == "Practical line"), clue(surface.advancedRows))
    assertEquals(
      surface.advancedRows.find(_.label == "Practical line").map(_.text),
      Some("The open c-file gives a practical major-piece line cue.")
    )

    val mismatchedFileSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "File entry",
              text = "The checked line keeps pressure on e6 through the c-file.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e6")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_open_file_tag_with_mismatched_file_entry"))
            )
          )
      )

    assert(mismatchedFileSurface.advancedRows.exists(_.label == "Practical line"), clue(mismatchedFileSurface.advancedRows))
    assertEquals(
      mismatchedFileSurface.advancedRows.find(_.label == "Practical line").map(_.text),
      Some("The open c-file gives a practical major-piece line cue.")
    )
  }

  test("stale exact-looking file entry labels do not suppress lower-authority line context") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_open_file_tag",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusFiles = List("c"),
        beneficiaryPieces = List("R"),
        confidence = 0.80,
        evidenceRefs = List("source:open_file_control", "open_file_c")
      )
    val surface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "File entry",
              text = "The rook already has a practical c-file post.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(surface.advancedRows.exists(_.label == "Practical line"), clue(surface.advancedRows))
    assertEquals(surface.advancedRows.find(_.label == "Practical line").map(_.text), Some("The open c-file gives a practical major-piece line cue."))

    val differentSubtypeSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Seventh-rank entry",
              text = "The checked line puts the rook on the seventh rank at c7.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea.copy(ideaId = "idea_open_file_with_exact_seventh_rank"))
            )
          )
      )

    assert(differentSubtypeSurface.advancedRows.exists(_.label == "Practical line"), clue(differentSubtypeSurface.advancedRows))
    assertEquals(
      differentSubtypeSurface.advancedRows.find(_.label == "Practical line").map(_.text),
      Some("The open c-file gives a practical major-piece line cue.")
    )
  }

  test("outpost strategic ideas create bounded practical outpost rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_outpost_tag",
        ownerSide = "white",
        kind = StrategicIdeaKind.OutpostCreationOrOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5"),
        beneficiaryPieces = List("N", "B"),
        confidence = 0.84,
        evidenceRefs = List("source:outpost_tag", "outpost_d5")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical outpost"), clue(surface.advancedRows))

    val routeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_route_outpost_only",
                    confidence = 0.90,
                    evidenceRefs = List("source:route_outpost_access", "route_outpost_access_shape", "outpost_d5")
                  )
                )
            )
          )
    )
    assert(!routeOnlySurface.advancedRows.exists(_.label == "Practical outpost"), clue(routeOnlySurface.advancedRows))

    val exactRouteSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_exact_route_outpost",
                    confidence = 0.66,
                    evidenceRefs = List("source:route_outpost_access", "route_outpost_access_shape", "route_surface_exact")
                  )
                )
            )
          )
      )
    assertEquals(exactRouteSurface.advancedRows.map(_.label), List("Practical outpost"))
    assertEquals(exactRouteSurface.advancedRows.head.text, "The minor-piece route points toward a practical outpost cue around d5.")
    assertEquals(exactRouteSurface.advancedRows.head.authority.flatMap(_.target), None)

    val exactRouteAmbiguousSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_exact_route_outpost_ambiguous_square",
                    focusSquares = List("d5", "e5"),
                    confidence = 0.66,
                    evidenceRefs = List("source:route_outpost_access", "route_outpost_access_shape", "route_surface_exact")
                  )
                )
            )
          )
      )
    assertEquals(exactRouteAmbiguousSquareSurface.advancedRows.map(_.label), List("Practical outpost"))
    assertEquals(exactRouteAmbiguousSquareSurface.advancedRows.head.text, "The minor-piece route points toward a practical outpost cue.")
    assertEquals(exactRouteAmbiguousSquareSurface.advancedRows.head.authority.flatMap(_.target), None)

    val directionalOutpostSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_outpost",
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("N"),
                    confidence = 0.64,
                    evidenceRefs = List("source:directional_outpost_access", "directional_outpost_access_shape")
                  )
                )
            )
          )
      )
    assertEquals(directionalOutpostSurface.advancedRows.map(_.label), List("Practical outpost"))
    assertEquals(
      directionalOutpostSurface.advancedRows.head.text,
      "The minor piece is aimed at a practical outpost cue around d5."
    )
    assertEquals(directionalOutpostSurface.advancedRows.head.authority.flatMap(_.target), None)

    val directionalOutpostAmbiguousSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_outpost_ambiguous_square",
                    focusSquares = List("d5", "e5"),
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("N"),
                    confidence = 0.64,
                    evidenceRefs = List("source:directional_outpost_access", "directional_outpost_access_shape")
                  )
                )
            )
          )
      )
    assertEquals(directionalOutpostAmbiguousSquareSurface.advancedRows.map(_.label), List("Practical outpost"))
    assertEquals(
      directionalOutpostAmbiguousSquareSurface.advancedRows.head.text,
      "The minor piece is aimed at a practical outpost cue."
    )
    assertEquals(directionalOutpostAmbiguousSquareSurface.advancedRows.head.authority.flatMap(_.target), None)

    val directionalOutpostMissingFactSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_outpost_missing_fact",
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs = List("source:directional_outpost_access")
                  )
                )
            )
          )
      )
    assert(
      !directionalOutpostMissingFactSurface.advancedRows.exists(_.label == "Practical outpost"),
      clue(directionalOutpostMissingFactSurface.advancedRows)
    )

    val directionalOutpostNonMinorSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_outpost_non_minor",
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("R"),
                    confidence = 0.70,
                    evidenceRefs = List("source:directional_outpost_access", "directional_outpost_access_shape")
                  )
                )
            )
          )
      )
    assert(!directionalOutpostNonMinorSurface.advancedRows.exists(_.label == "Practical outpost"), clue(directionalOutpostNonMinorSurface.advancedRows))

    val directionalOutpostLowConfidenceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_directional_outpost_low_confidence",
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("N"),
                    confidence = 0.60,
                    evidenceRefs = List("source:directional_outpost_access", "directional_outpost_access_shape")
                  )
                )
            )
          )
      )
    assert(
      !directionalOutpostLowConfidenceSurface.advancedRows.exists(_.label == "Practical outpost"),
      clue(directionalOutpostLowConfidenceSurface.advancedRows)
    )

    val buildRouteSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                    idea.copy(
                      ideaId = "idea_build_route_outpost",
                      confidence = 0.68,
                    evidenceRefs = List("source:route_outpost_access", "route_outpost_access_shape", "route_surface_build")
                  )
                )
            )
          )
      )
    assert(!buildRouteSurface.advancedRows.exists(_.label == "Practical outpost"), clue(buildRouteSurface.advancedRows))

    val factOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_outpost_fact_only",
                    evidenceRefs = List("outpost_d5")
                  )
                )
            )
          )
      )
    assert(!factOnlySurface.advancedRows.exists(_.label == "Practical outpost"), clue(factOnlySurface.advancedRows))

    val strongKnightSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_strong_knight_outpost",
                    beneficiaryPieces = List("N"),
                    confidence = 0.76,
                    evidenceRefs = List("source:strong_knight", "strong_knight_d5")
                  )
                )
            )
          )
      )
    assert(!strongKnightSurface.advancedRows.exists(_.label == "Practical outpost"), clue(strongKnightSurface.advancedRows))

    val unoccupiedStrongKnightSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_unoccupied_strong_knight_outpost",
                    readiness = StrategicIdeaReadiness.Build,
                    beneficiaryPieces = List("N"),
                    confidence = 0.76,
                    evidenceRefs = List("source:strong_knight", "strong_knight_d5")
                  )
                )
            )
          )
      )
    assert(
      !unoccupiedStrongKnightSurface.advancedRows.exists(_.label == "Practical outpost"),
      clue(unoccupiedStrongKnightSurface.advancedRows)
    )

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(idea.copy(ideaId = "idea_mismatched_outpost_owner"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical outpost"), clue(mirroredSurface.advancedRows))

    val exactOutpostSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Knight outpost",
              text = "The checked line puts the knight on the d5 outpost.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
    )
    assert(!exactOutpostSurface.advancedRows.exists(_.label == "Practical outpost"), clue(exactOutpostSurface.advancedRows))

    val exactOpeningOutpostSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Opening outpost",
              text = "The checked opening structure puts a knight on the pawn-supported d5 outpost square.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(!exactOpeningOutpostSurface.advancedRows.exists(_.label == "Practical outpost"), clue(exactOpeningOutpostSurface.advancedRows))
  }

  test("tag-only outpost context stays hidden beside exact outpost rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_outpost_tag",
        ownerSide = "white",
        kind = StrategicIdeaKind.OutpostCreationOrOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5"),
        beneficiaryPieces = List("N"),
        confidence = 0.84,
        evidenceRefs = List("source:outpost_tag", "outpost_d5")
      )
    val surface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Knight outpost",
              text = "The checked line puts the knight on the e5 outpost.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("e5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical outpost"), clue(surface.advancedRows))

    val openingSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Opening outpost",
              text = "The checked opening structure has put a knight on the e5 outpost.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!openingSurface.advancedRows.exists(_.label == "Practical outpost"), clue(openingSurface.advancedRows))

    val malformedPieceSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Knight outpost",
              text = "The checked line puts the queen on the d5 outpost.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!malformedPieceSurface.advancedRows.exists(_.label == "Practical outpost"), clue(malformedPieceSurface.advancedRows))
  }

  test("stale exact-looking outpost labels do not revive tag-only outpost context") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_outpost_tag",
        ownerSide = "white",
        kind = StrategicIdeaKind.OutpostCreationOrOccupation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("d5"),
        beneficiaryPieces = List("N"),
        confidence = 0.84,
        evidenceRefs = List("source:outpost_tag", "outpost_d5")
      )
    val surface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Knight outpost",
              text = "The strong knight gives a practical outpost cue around d5.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical outpost"), clue(surface.advancedRows))

    val openingLabelSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Opening outpost",
              text = "The opening plan has a possible outpost idea around d5.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!openingLabelSurface.advancedRows.exists(_.label == "Practical outpost"), clue(openingLabelSurface.advancedRows))
  }

  test("IQP trade-down strategic ideas create bounded practical trade rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_iqp_trade_down",
        ownerSide = "white",
        kind = StrategicIdeaKind.FavorableTradeOrTransformation,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("d5"),
        confidence = 0.78,
        evidenceRefs =
          List(
            "source:iqp_simplification_profile",
            "structure_iqp_black",
            "capture_or_exchange"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical trade"))
    assertEquals(surface.advancedRows.head.text, "The IQP structure gives White a practical trade-down cue around d5.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val multiTargetSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_trade_down_multi_target",
                    focusSquares = List("d5", "e5")
                  )
                )
            )
          )
      )
    assertEquals(multiTargetSurface.advancedRows.map(_.label), List("Practical trade"))
    assertEquals(multiTargetSurface.advancedRows.head.text, "The IQP structure gives White a practical trade-down cue.")
    assertEquals(multiTargetSurface.advancedRows.head.authority.flatMap(_.target), None)

    val planBackedSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_trade_down_plan",
                    focusSquares = Nil,
                    evidenceRefs =
                      List(
                        "source:iqp_simplification_profile",
                        "structure_iqp_black",
                        "iqp_trade_down_plan"
                      )
                  )
                )
            )
          )
      )
    assertEquals(planBackedSurface.advancedRows.map(_.label), List("Practical trade"))
    assertEquals(planBackedSurface.advancedRows.head.text, "The IQP structure gives White a practical trade-down cue.")
    assertEquals(planBackedSurface.advancedRows.head.authority.flatMap(_.target), None)

    val exchangeAvailabilitySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_exchange_availability",
                    focusSquares = Nil,
                    confidence = 0.64,
                    evidenceRefs =
                      List(
                        "source:exchange_availability_bridge",
                        "structure_iqp_black"
                      )
                  )
                )
            )
          )
      )
    assertEquals(exchangeAvailabilitySurface.advancedRows.map(_.label), List("Practical trade"))
    assertEquals(
      exchangeAvailabilitySurface.advancedRows.head.text,
      "The IQP structure gives White a practical exchange-availability cue."
    )
    assertEquals(exchangeAvailabilitySurface.advancedRows.head.authority.flatMap(_.target), None)

    val exchangeAvailabilitySourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_exchange_availability_source_only",
                    confidence = 0.90,
                    evidenceRefs = List("source:exchange_availability_bridge")
                  )
                )
            )
          )
      )
    assert(!exchangeAvailabilitySourceOnlySurface.advancedRows.exists(_.label == "Practical trade"), clue(exchangeAvailabilitySourceOnlySurface.advancedRows))

    val exchangeAvailabilityLowConfidenceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_exchange_availability_low_confidence",
                    confidence = 0.63,
                    evidenceRefs =
                      List(
                        "source:exchange_availability_bridge",
                        "structure_iqp_black"
                      )
                  )
                )
            )
          )
      )
    assert(!exchangeAvailabilityLowConfidenceSurface.advancedRows.exists(_.label == "Practical trade"), clue(exchangeAvailabilityLowConfidenceSurface.advancedRows))

    val exchangeAvailabilityMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_iqp_exchange_availability_mismatched_side",
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:exchange_availability_bridge",
                        "structure_iqp_black"
                      )
                  )
                )
            )
          )
      )
    assert(!exchangeAvailabilityMirroredSurface.advancedRows.exists(_.label == "Practical trade"), clue(exchangeAvailabilityMirroredSurface.advancedRows))

    val broadTransformationSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_broad_transformation",
                    confidence = 0.90,
                    evidenceRefs = List("source:plan_match_transformation", "plan_match_transformation", "capture_or_exchange")
                  )
                )
            )
          )
      )
    assert(!broadTransformationSurface.advancedRows.exists(_.label == "Practical trade"), clue(broadTransformationSurface.advancedRows))

    val lowConfidenceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_low_confidence_iqp",
                    confidence = 0.64
                  )
                )
            )
          )
      )
    assert(!lowConfidenceSurface.advancedRows.exists(_.label == "Practical trade"), clue(lowConfidenceSurface.advancedRows))

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(idea.copy(ideaId = "idea_mismatched_iqp_trade"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical trade"), clue(mirroredSurface.advancedRows))

    val exactSimplificationSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Simplification",
              text = "The checked line keeps the same local edge after the exchange on d5.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan, target = Some("d5")))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
    )
    assert(!exactSimplificationSurface.advancedRows.exists(_.label == "Practical trade"), clue(exactSimplificationSurface.advancedRows))

    val staleSimplificationSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Simplification",
              text = "The IQP structure gives White a practical exchange-availability cue.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )
    assert(staleSimplificationSurface.advancedRows.exists(_.label == "Practical trade"), clue(staleSimplificationSurface.advancedRows))
    assertEquals(
      staleSimplificationSurface.advancedRows.find(_.label == "Practical trade").map(_.text),
      Some("The IQP structure gives White a practical trade-down cue around d5.")
    )
  }

  test("endgame cue rows require typed anchors, not win or conversion labels") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_winning_endgame_conversion",
        ownerSide = "white",
        kind = StrategicIdeaKind.FavorableTradeOrTransformation,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        focusSquares = List("e6"),
        confidence = 0.80,
        evidenceRefs =
          List(
            "source:winning_endgame_transition",
            "winning_endgame_transition_shape"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Endgame cue"), clue(surface.advancedRows))

    val multiSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_winning_endgame_multi_square_conversion",
                    focusSquares = List("e4", "e6")
                  )
                )
            )
          )
      )
    assert(!multiSquareSurface.advancedRows.exists(_.label == "Endgame cue"), clue(multiSquareSurface.advancedRows))

    val rookEndgameSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_endgame_pattern",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    focusFiles = List("e"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:rook_endgame_pattern",
                        "rook_endgame_pattern_shape",
                        "rook_behind_passed_pawn"
                      )
                  )
                )
            )
          )
      )
    assertEquals(rookEndgameSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(rookEndgameSurface.advancedRows.head.text, "The rook-behind-passer structure is the relevant endgame cue.")
    assertEquals(rookEndgameSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiRookEndgameSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_multi_rook_endgame_pattern",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    focusFiles = List("e"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:rook_endgame_pattern",
                        "rook_endgame_pattern_shape",
                        "rook_behind_passed_pawn",
                        "king_cut_off"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiRookEndgameSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(multiRookEndgameSurface.advancedRows.head.text, "The rook endgame map stays as endgame structure.")
    assertEquals(multiRookEndgameSurface.advancedRows.head.authority.flatMap(_.target), None)

    val oppositionSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_opposition_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e4", "e6"),
                    focusFiles = Nil,
                    focusZone = Some("endgame"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "opposition_direct"
                      )
                  )
                )
            )
          )
      )
    assertEquals(oppositionSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(oppositionSurface.advancedRows.head.text, "The direct opposition is the relevant endgame technique cue.")
    assertEquals(oppositionSurface.advancedRows.head.authority.flatMap(_.target), None)

    val zugzwangSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_zugzwang_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    focusFiles = Nil,
                    focusZone = Some("endgame"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "zugzwang_shape"
                      )
                  )
                )
            )
          )
      )
    assertEquals(zugzwangSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(zugzwangSurface.advancedRows.head.text, "The zugzwang shape is the relevant endgame technique cue.")
    assertEquals(zugzwangSurface.advancedRows.head.authority.flatMap(_.target), None)

    val activeKingSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_king_activity_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    focusFiles = Nil,
                    focusZone = Some("endgame"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "king_activity_shape"
                      )
                  )
                )
            )
          )
      )
    assertEquals(activeKingSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(activeKingSurface.advancedRows.head.text, "The active king is the relevant endgame technique cue.")
    assertEquals(activeKingSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiTechniqueSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_multi_technique_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e4", "e6"),
                    focusFiles = Nil,
                    focusZone = Some("endgame"),
                    confidence = 0.72,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "opposition_direct",
                        "zugzwang_shape",
                        "king_activity_shape"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiTechniqueSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(multiTechniqueSurface.advancedRows.head.text, "The endgame technique map stays result-neutral.")
    assertEquals(multiTechniqueSurface.advancedRows.head.authority.flatMap(_.target), None)

    val passedPawnSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_passed_pawn_conversion_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e6"),
                    focusFiles = List("e"),
                    confidence = 0.76,
                    evidenceRefs =
                      List(
                        "source:passed_pawn_conversion_motif",
                        "passed_pawn_conversion_shape",
                        "passed_pawn_e6",
                        "protected_passed_pawn"
                      )
                  )
                )
            )
          )
      )
    assertEquals(passedPawnSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(passedPawnSurface.advancedRows.head.text, "The passed-pawn structure is the relevant endgame cue around e6.")
    assertEquals(passedPawnSurface.advancedRows.head.authority.flatMap(_.target), None)

    val matchedPassedPawnSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_passed_pawn_matched_square",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e4", "e6"),
                    focusZone = Some("kingside"),
                    confidence = 0.76,
                    evidenceRefs =
                      List(
                        "source:passed_pawn_conversion_motif",
                        "passed_pawn_conversion_shape",
                        "passed_pawn_e6"
                      )
                  )
                )
            )
          )
      )
    assertEquals(matchedPassedPawnSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(matchedPassedPawnSurface.advancedRows.head.text, "The passed-pawn structure is the relevant endgame cue around e6.")
    assertEquals(matchedPassedPawnSurface.advancedRows.head.authority.flatMap(_.target), Option.empty[String])

    val promotionSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_pawn_promotion_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("a8"),
                    focusFiles = List("a"),
                    confidence = 0.76,
                    evidenceRefs =
                      List(
                        "source:passed_pawn_conversion_motif",
                        "passed_pawn_conversion_shape",
                        "passed_pawn_a8",
                        "pawn_promotion",
                        "promotion_piece_q"
                      )
                  )
                )
            )
          )
      )
    assertEquals(promotionSurface.advancedRows.map(_.label), List("Endgame cue"))
    assertEquals(promotionSurface.advancedRows.head.text, "The promotion motif is the relevant endgame cue on a8.")
    assertEquals(promotionSurface.advancedRows.head.authority.flatMap(_.target), None)

    val rookEndgameSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_endgame_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.80,
                    evidenceRefs = List("source:rook_endgame_pattern")
                  )
                )
            )
          )
      )
    assert(!rookEndgameSourceOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(rookEndgameSourceOnlySurface.advancedRows))

    val rookEndgameShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_rook_endgame_shape_only",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.80,
                    evidenceRefs = List("source:rook_endgame_pattern", "rook_endgame_pattern_shape")
                  )
                )
            )
          )
      )
    assert(!rookEndgameShapeOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(rookEndgameShapeOnlySurface.advancedRows))

    val oppositionSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_opposition_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.80,
                    focusZone = Some("endgame"),
                    evidenceRefs = List("source:endgame_technique_motif")
                  )
                )
            )
          )
      )
    assert(!oppositionSourceOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(oppositionSourceOnlySurface.advancedRows))

    val passedPawnSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_passed_pawn_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e6"),
                    confidence = 0.76,
                    evidenceRefs = List("source:passed_pawn_conversion_motif")
                  )
                )
            )
          )
      )
    assert(!passedPawnSourceOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(passedPawnSourceOnlySurface.advancedRows))

    val passedPawnShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_passed_pawn_shape_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e6"),
                    confidence = 0.76,
                    evidenceRefs = List("source:passed_pawn_conversion_motif", "passed_pawn_conversion_shape")
                  )
                )
            )
          )
      )
    assert(!passedPawnShapeOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(passedPawnShapeOnlySurface.advancedRows))

    val sourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_winning_endgame_source_only",
                    evidenceRefs = List("source:winning_endgame_transition")
                  )
                )
            )
          )
      )
    assert(!sourceOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(sourceOnlySurface.advancedRows))

    val classificationOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_classification_window_only",
                    confidence = 0.90,
                    evidenceRefs =
                      List(
                        "source:classification_transformation_window",
                        "classification_transformation_window",
                        "convert_mode"
                      )
                  )
                )
            )
          )
      )
    assert(!classificationOnlySurface.advancedRows.exists(_.label == "Endgame cue"), clue(classificationOnlySurface.advancedRows))

    val exactConversionSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Technical conversion",
              text = "The checked line keeps the best defense narrow and the conversion route intact.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_opposition_exact_visible",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e4", "e6"),
                    focusZone = Some("endgame"),
                    confidence = 0.80,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "opposition_direct"
                      )
                  )
                )
            )
      )
    )
    assert(!exactConversionSurface.advancedRows.exists(_.label == "Endgame cue"), clue(exactConversionSurface.advancedRows))

    val staleConversionLabelSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Technical conversion",
              text = "The conversion route is still intact.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_endgame_opposition_stale_conversion_label",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = List("e4", "e6"),
                    focusZone = Some("endgame"),
                    confidence = 0.80,
                    evidenceRefs =
                      List(
                        "source:endgame_technique_motif",
                        "endgame_technique_shape",
                        "opposition_direct"
                      )
                  )
                )
            )
          )
      )
    assertEquals(staleConversionLabelSurface.advancedRows.map(_.label), List("Endgame cue"))
  }

  test("French minor-piece strategic ideas create bounded practical minor rows") {
    val idea =
      StrategyIdeaSignal(
        ideaId = "idea_french_knight_vs_bishop",
        ownerSide = "white",
        kind = StrategicIdeaKind.MinorPieceImbalanceExploitation,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("d5"),
        beneficiaryPieces = List("N", "B"),
        confidence = 0.80,
        evidenceRefs =
          List(
            "source:french_minor_piece_profile",
            "structure_french_advance_chain",
            "source:strong_knight_vs_bad_bishop",
            "strong_knight_d5"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(idea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(surface.advancedRows.head.text, "The French pawn chain gives White a practical knight-vs-bishop cue around d5.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val activityBadBishopSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_french_bad_bishop_activity",
                    focusSquares = List("e7"),
                    confidence = 0.74,
                    evidenceRefs =
                      List(
                        "source:french_minor_piece_profile",
                        "structure_french_advance_chain",
                        "source:piece_activity_bad_bishop",
                        "enemy_bad_bishop_e7"
                      )
                  )
                )
            )
          )
      )
    assertEquals(activityBadBishopSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      activityBadBishopSurface.advancedRows.head.text,
      "The French pawn chain gives White a practical minor-piece cue against the bad bishop on e7."
    )
    assertEquals(activityBadBishopSurface.advancedRows.head.authority.flatMap(_.target), None)

    val frenchProfileOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_french_profile_only",
                    focusSquares = Nil,
                    evidenceRefs =
                      List(
                        "source:french_minor_piece_profile",
                        "structure_french_advance_chain"
                      )
                  )
                )
            )
          )
      )
    assert(!frenchProfileOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(frenchProfileOnlySurface.advancedRows))

    val bishopPairSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bishop_pair",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.82,
                    evidenceRefs = List("source:bishop_pair_advantage", "bishop_pair_advantage_shape")
                  )
                )
            )
          )
      )
    assertEquals(bishopPairSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(bishopPairSurface.advancedRows.head.text, "The current minor-piece map gives a practical bishop-pair cue.")
    assertEquals(bishopPairSurface.advancedRows.head.authority.flatMap(_.target), None)

    val bishopPairSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bishop_pair_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.82,
                    evidenceRefs = List("source:bishop_pair_advantage")
                  )
                )
            )
          )
      )
    assert(!bishopPairSourceOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(bishopPairSourceOnlySurface.advancedRows))

    val exactBishopPairSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Bishop pair",
              text = "The checked capture keeps the bishop pair on the board.",
              tone = Some("practical"),
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bishop_pair_with_exact_row",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.82,
                    evidenceRefs = List("source:bishop_pair_advantage", "bishop_pair_advantage_shape")
                  )
                )
            )
      )
    )
    assert(!exactBishopPairSurface.advancedRows.exists(_.label == "Practical minor"), clue(exactBishopPairSurface.advancedRows))

    val staleBishopPairLabelSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Bishop pair",
              text = "The bishop pair gives a practical minor-piece cue.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bishop_pair_with_stale_row",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.82,
                    evidenceRefs = List("source:bishop_pair_advantage", "bishop_pair_advantage_shape")
                  )
                )
            )
          )
      )
    assertEquals(staleBishopPairLabelSurface.advancedRows.map(_.label), List("Practical minor"))

    val badBishopOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bad_bishop_only",
                    evidenceRefs = List("source:enemy_bad_bishop", "enemy_bad_bishop_shape")
                  )
                )
            )
          )
      )
    assertEquals(badBishopOnlySurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      badBishopOnlySurface.advancedRows.head.text,
      "The current minor-piece map gives a practical cue against the opponent's bad bishop."
    )
    assertEquals(badBishopOnlySurface.advancedRows.head.authority.flatMap(_.target), None)

    val badBishopSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_bad_bishop_source_only",
                    evidenceRefs = List("source:enemy_bad_bishop")
                  )
                )
            )
          )
      )
    assert(!badBishopSourceOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(badBishopSourceOnlySurface.advancedRows))

    val goodBishopCountSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_good_bishop_count_edge",
                    beneficiaryPieces = List("B"),
                    confidence = 0.74,
                    evidenceRefs =
                      List(
                        "source:good_bishop",
                        "good_bishop_shape",
                        "source:minor_piece_count_imbalance",
                        "minor_piece_count_imbalance_shape",
                        "good_bishop_count_edge"
                      )
                  )
                )
            )
          )
      )
    assertEquals(goodBishopCountSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(goodBishopCountSurface.advancedRows.head.text, "The current minor-piece map gives a practical good-bishop cue.")
    assertEquals(goodBishopCountSurface.advancedRows.head.authority.flatMap(_.target), None)

    val goodBishopOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_good_bishop_only",
                    beneficiaryPieces = List("B"),
                    confidence = 0.74,
                    evidenceRefs = List("source:good_bishop", "good_bishop_shape")
                  )
                )
            )
          )
      )
    assert(!goodBishopOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(goodBishopOnlySurface.advancedRows))

    val countOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_count_only",
                    beneficiaryPieces = List("B"),
                    confidence = 0.74,
                    evidenceRefs =
                      List(
                        "source:minor_piece_count_imbalance",
                        "minor_piece_count_imbalance_shape",
                        "good_bishop_count_edge"
                      )
                  )
                )
            )
          )
      )
    assert(!countOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(countOnlySurface.advancedRows))

    val centralizationSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_centralization",
                    focusSquares = List("e5"),
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:piece_centralization_motif",
                        "piece_centralization_shape",
                        "centralized_piece_e5"
                      )
                  )
                )
            )
          )
      )
    assertEquals(centralizationSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      centralizationSurface.advancedRows.head.text,
      "The centralized N on e5 gives a practical minor-piece cue."
    )
    assertEquals(centralizationSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiPieceCentralizationSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_multi_piece_centralization",
                    focusSquares = List("e5", "d4"),
                    beneficiaryPieces = List("N", "B"),
                    confidence = 0.75,
                    evidenceRefs =
                      List(
                        "source:piece_centralization_motif",
                        "piece_centralization_shape",
                        "centralized_piece_e5",
                        "centralized_piece_d4"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiPieceCentralizationSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      multiPieceCentralizationSurface.advancedRows.head.text,
      "The centralized minor pieces give a practical minor-piece cue."
    )
    assertEquals(multiPieceCentralizationSurface.advancedRows.head.authority.flatMap(_.target), None)

    val maneuverSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_maneuver",
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:piece_maneuver_motif",
                        "piece_maneuver_shape",
                        "piece_maneuver_rerouting"
                      )
                  )
                )
            )
          )
      )
    assertEquals(maneuverSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      maneuverSurface.advancedRows.head.text,
      "The N maneuver gives a practical minor-piece cue."
    )
    assertEquals(maneuverSurface.advancedRows.head.authority.flatMap(_.target), None)

    val centralizationSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_centralization_source_only",
                    focusSquares = List("e5"),
                    beneficiaryPieces = List("N"),
                    confidence = 0.80,
                    evidenceRefs = List("source:piece_centralization_motif", "centralized_piece_e5")
                  )
                )
            )
          )
      )
    assert(!centralizationSourceOnlySurface.advancedRows.exists(_.label == "Practical minor"), clue(centralizationSourceOnlySurface.advancedRows))

    val centralizationNoSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_centralization_no_square",
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.80,
                    evidenceRefs = List("source:piece_centralization_motif", "piece_centralization_shape")
                  )
                )
            )
          )
      )
    assert(!centralizationNoSquareSurface.advancedRows.exists(_.label == "Practical minor"), clue(centralizationNoSquareSurface.advancedRows))

    val centralizationNoMinorSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_centralization_no_minor",
                    focusSquares = List("e5"),
                    beneficiaryPieces = List("R"),
                    confidence = 0.80,
                    evidenceRefs =
                      List(
                        "source:piece_centralization_motif",
                        "piece_centralization_shape",
                        "centralized_piece_e5"
                      )
                  )
                )
            )
          )
      )
    assert(!centralizationNoMinorSurface.advancedRows.exists(_.label == "Practical minor"), clue(centralizationNoMinorSurface.advancedRows))

    val centralizationMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_piece_centralization_mirrored",
                    focusSquares = List("e5"),
                    beneficiaryPieces = List("N"),
                    confidence = 0.80,
                    evidenceRefs =
                      List(
                        "source:piece_centralization_motif",
                        "piece_centralization_shape",
                        "centralized_piece_e5"
                      )
                  )
                )
            )
          )
      )
    assert(!centralizationMirroredSurface.advancedRows.exists(_.label == "Practical minor"), clue(centralizationMirroredSurface.advancedRows))

    val activityWithoutFrenchSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_activity_without_french",
                    focusSquares = List("e7"),
                    evidenceRefs = List("source:piece_activity_bad_bishop", "enemy_bad_bishop_e7")
                  )
                )
            )
          )
      )
    assertEquals(activityWithoutFrenchSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      activityWithoutFrenchSurface.advancedRows.head.text,
      "The current minor-piece map gives a practical cue against the bad bishop on e7."
    )
    assertEquals(activityWithoutFrenchSurface.advancedRows.head.authority.flatMap(_.target), None)

    val strongKnightWithoutFrenchSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_strong_knight_without_french",
                    readiness = StrategicIdeaReadiness.Ready,
                    focusSquares = List("d5"),
                    evidenceRefs = List("source:strong_knight_vs_bad_bishop", "strong_knight_d5")
                  )
                )
            )
          )
      )
    assertEquals(strongKnightWithoutFrenchSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      strongKnightWithoutFrenchSurface.advancedRows.head.text,
      "The current minor-piece map gives a practical knight-vs-bishop cue around d5."
    )

    val knightVsBishopMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_knight_vs_bishop_motif",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs =
                      List(
                        "source:knight_vs_bishop_motif",
                        "knight_vs_bishop_motif_shape",
                        "knight_preferred_over_bishop"
                      )
                  )
                )
            )
          )
      )
    assertEquals(knightVsBishopMotifSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      knightVsBishopMotifSurface.advancedRows.head.text,
      "The current minor-piece map gives a practical knight-vs-bishop cue."
    )
    assertEquals(knightVsBishopMotifSurface.advancedRows.head.authority.flatMap(_.target), None)

    val knightVsBishopMotifSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_knight_vs_bishop_motif_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs = List("source:knight_vs_bishop_motif")
                  )
                )
            )
          )
      )
    assert(
      !knightVsBishopMotifSourceOnlySurface.advancedRows.exists(_.label == "Practical minor"),
      clue(knightVsBishopMotifSourceOnlySurface.advancedRows)
    )

    val knightVsBishopMotifShapeOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_knight_vs_bishop_motif_shape_only",
                    readiness = StrategicIdeaReadiness.Build,
                    focusSquares = Nil,
                    beneficiaryPieces = List("N"),
                    confidence = 0.70,
                    evidenceRefs = List("source:knight_vs_bishop_motif", "knight_vs_bishop_motif_shape")
                  )
                )
            )
          )
      )
    assert(
      !knightVsBishopMotifShapeOnlySurface.advancedRows.exists(_.label == "Practical minor"),
      clue(knightVsBishopMotifShapeOnlySurface.advancedRows)
    )

    val activityWithoutMinorBeneficiariesSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_activity_without_minor_beneficiaries",
                    focusSquares = List("e7"),
                    beneficiaryPieces = Nil,
                    evidenceRefs = List("source:piece_activity_bad_bishop", "enemy_bad_bishop_e7")
                  )
                )
            )
          )
      )
    assert(
      !activityWithoutMinorBeneficiariesSurface.advancedRows.exists(_.label == "Practical minor"),
      clue(activityWithoutMinorBeneficiariesSurface.advancedRows)
    )

    val oppositeColorBishopsSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opposite_color_bishops",
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.68,
                    evidenceRefs = List("source:opposite_color_bishops", "opposite_color_bishops_shape")
                  )
                )
            )
          )
      )
    assertEquals(oppositeColorBishopsSurface.advancedRows.map(_.label), List("Practical minor"))
    assertEquals(
      oppositeColorBishopsSurface.advancedRows.head.text,
      "The current minor-piece map gives a practical opposite-colored-bishops cue."
    )
    assertEquals(oppositeColorBishopsSurface.advancedRows.head.authority.flatMap(_.target), None)

    val oppositeColorBishopsSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opposite_color_bishops_source_only",
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.68,
                    evidenceRefs = List("source:opposite_color_bishops")
                  )
                )
            )
          )
      )
    assert(
      !oppositeColorBishopsSourceOnlySurface.advancedRows.exists(_.label == "Practical minor"),
      clue(oppositeColorBishopsSourceOnlySurface.advancedRows)
    )

    val exactOppositeColorBishopsSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Opposite-color bishops",
              text = "The checked capture leaves opposite-colored bishops on the board.",
              tone = Some("practical"),
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opposite_color_bishops_with_exact_row",
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.68,
                    evidenceRefs = List("source:opposite_color_bishops", "opposite_color_bishops_shape")
                  )
                )
            )
      )
    )
    assert(!exactOppositeColorBishopsSurface.advancedRows.exists(_.label == "Practical minor"), clue(exactOppositeColorBishopsSurface.advancedRows))

    val staleOppositeColorBishopsLabelSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Opposite-color bishops",
              text = "The opposite-colored bishops give a practical minor-piece cue.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  idea.copy(
                    ideaId = "idea_opposite_color_bishops_with_stale_row",
                    focusSquares = Nil,
                    beneficiaryPieces = List("B"),
                    confidence = 0.68,
                    evidenceRefs = List("source:opposite_color_bishops", "opposite_color_bishops_shape")
                  )
                )
            )
          )
      )
    assertEquals(staleOppositeColorBishopsLabelSurface.advancedRows.map(_.label), List("Practical minor"))

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(idea.copy(ideaId = "idea_mirrored_french_minor", ownerSide = "black"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical minor"), clue(mirroredSurface.advancedRows))
  }

  test("prophylaxis strategic ideas create bounded practical prophylaxis rows") {
    val bishopPinIdea =
      StrategyIdeaSignal(
        ideaId = "idea_bishop_pin_watch",
        ownerSide = "white",
        kind = StrategicIdeaKind.Prophylaxis,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("g4"),
        focusZone = Some("kingside"),
        confidence = 0.84,
        evidenceRefs = List("source:bishop_pin_watch", "bishop_pin_watch")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(bishopPinIdea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical prophylaxis"))
    assertEquals(surface.advancedRows.head.text, "The current piece layout gives a practical prophylaxis cue against the bishop pin.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val queensideSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  bishopPinIdea.copy(
                    ideaId = "idea_queenside_counterbreak_watch",
                    focusSquares = Nil,
                    focusFiles = List("b"),
                    focusZone = Some("queenside"),
                    confidence = 0.90,
                    evidenceRefs = List("source:queenside_counterbreak_watch", "queenside_counterbreak_watch")
                  )
                )
            )
          )
      )
    assertEquals(queensideSurface.advancedRows.map(_.label), List("Practical prophylaxis"))
    assertEquals(
      queensideSurface.advancedRows.head.text,
      "The queenside structure gives a practical prophylaxis cue against the ...b5 break."
    )
    assertEquals(queensideSurface.advancedRows.head.authority.flatMap(_.target), None)

    val planOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  bishopPinIdea.copy(
                    ideaId = "idea_plan_prophylaxis_only",
                    focusSquares = Nil,
                    focusFiles = List("g"),
                    confidence = 0.90,
                    evidenceRefs = List("source:plan_match_prophylaxis", "plan_match_prophylaxis", "plan_prophylaxis")
                  )
                )
            )
          )
      )
    assert(!planOnlySurface.advancedRows.exists(_.label == "Practical prophylaxis"), clue(planOnlySurface.advancedRows))

    val threatOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  bishopPinIdea.copy(
                    ideaId = "idea_threat_prophylaxis",
                    focusSquares = List("g4"),
                    confidence = 0.90,
                    evidenceRefs = List("source:threat_analysis_prophylaxis", "threat_analysis_prophylaxis", "prophylaxis_needed")
                  )
                )
            )
          )
      )
    assert(!threatOnlySurface.advancedRows.exists(_.label == "Practical prophylaxis"), clue(threatOnlySurface.advancedRows))

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(bishopPinIdea.copy(ideaId = "idea_mismatched_bishop_pin"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical prophylaxis"), clue(mirroredSurface.advancedRows))

    val promotedProphylaxisSurface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                name = "Deny the pin",
                themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
                subplanId = Some(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id)
              ),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(bishopPinIdea)
            )
          )
      )
    assert(!promotedProphylaxisSurface.advancedRows.exists(_.label == "Practical prophylaxis"), clue(promotedProphylaxisSurface.advancedRows))
  }

  test("fianchetto assault strategic ideas create bounded practical attack rows") {
    val fianchettoIdea =
      StrategyIdeaSignal(
        ideaId = "idea_fianchetto_assault",
        ownerSide = "white",
        kind = StrategicIdeaKind.KingAttackBuildUp,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Build,
        focusZone = Some("kingside"),
        confidence = 0.90,
        evidenceRefs =
          List(
            "source:fianchetto_assault_profile",
            "source:opposite_side_storm",
            "structure_fianchetto_shell"
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(fianchettoIdea)
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical attack"))
    assertEquals(surface.advancedRows.head.text, "The fianchetto-shell structure gives a practical opposite-side attack cue.")
    assertEquals(surface.advancedRows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
    assertEquals(surface.advancedRows.head.authority.flatMap(_.target), None)

    val stormOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_opposite_side_storm_only",
                    confidence = 0.84,
                    evidenceRefs = List("source:opposite_side_storm")
                  )
                )
            )
          )
      )
    assert(!stormOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(stormOnlySurface.advancedRows))

    val planOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_plan_attack_only",
                    confidence = 0.90,
                    evidenceRefs = List("source:plan_match_king_attack", "plan_match_king_attack", "plan_kingsideattack")
                  )
                )
            )
          )
    )
    assert(!planOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(planOnlySurface.advancedRows))

    val kingRingPressureSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_king_ring_pressure",
                    confidence = 0.78,
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:king_ring_pressure",
                        "king_ring_pressure_shape",
                        "source:initiative_motif",
                        "initiative_motif_shape",
                        "initiative_score_12"
                      )
                  )
                )
            )
          )
      )
    assert(!kingRingPressureSurface.advancedRows.exists(_.label == "Practical attack"), clue(kingRingPressureSurface.advancedRows))

    val enemyKingStuckSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_king_stuck_center",
                    confidence = 0.80,
                    focusZone = Some("center"),
                    evidenceRefs = List("source:enemy_king_stuck_center", "enemy_king_central_exposure")
                  )
                )
            )
          )
      )
    assertEquals(enemyKingStuckSurface.advancedRows.map(_.label), List("Practical attack"), clue(enemyKingStuckSurface.advancedRows))
    assertEquals(
      enemyKingStuckSurface.advancedRows.head.text,
      "The enemy king's central exposure gives a practical attacking cue."
    )
    assertEquals(enemyKingStuckSurface.advancedRows.head.authority.flatMap(_.target), None)

    val enemyKingSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_king_stuck_source_only",
                    confidence = 0.80,
                    focusZone = Some("center"),
                    evidenceRefs = List("source:enemy_king_stuck_center")
                  )
                )
            )
          )
      )
    assert(!enemyKingSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(enemyKingSourceOnlySurface.advancedRows))

    val weakBackRankSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_weak_back_rank",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:enemy_weak_back_rank",
                        "enemy_weak_back_rank_shape",
                        "source:directional_attack_lane",
                        "directional_attack_lane_shape",
                        "source:initiative_motif",
                        "initiative_motif_shape",
                        "initiative_score_12"
                      )
                  )
                )
            )
          )
      )
    assert(!weakBackRankSurface.advancedRows.exists(_.label == "Practical attack"), clue(weakBackRankSurface.advancedRows))

    val initiativeMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_initiative_motif",
                    confidence = 0.70,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:initiative_motif", "initiative_motif_shape", "initiative_score_12")
                  )
                )
            )
          )
      )
    assert(!initiativeMotifSurface.advancedRows.exists(_.label == "Practical attack"), clue(initiativeMotifSurface.advancedRows))

    val initiativeMotifSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_initiative_motif_source_only",
                    confidence = 0.70,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:initiative_motif")
                  )
                )
            )
          )
      )
    assert(!initiativeMotifSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(initiativeMotifSourceOnlySurface.advancedRows))

    val weakBackRankSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_weak_back_rank_source_only",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:enemy_weak_back_rank")
                  )
                )
            )
          )
      )
    assert(!weakBackRankSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(weakBackRankSourceOnlySurface.advancedRows))

    val routeAttackLaneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_route_attack_lane",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.78,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:route_attack_lane", "route_attack_lane_shape", "route_surface_exact")
                  )
                )
            )
          )
      )
    assertEquals(routeAttackLaneSurface.advancedRows.map(_.label), List("Practical attack"), clue(routeAttackLaneSurface.advancedRows))
    assertEquals(routeAttackLaneSurface.advancedRows.head.text, "The h7 route gives a practical attacking lane.")
    assertEquals(routeAttackLaneSurface.advancedRows.head.authority.flatMap(_.target), None)

    val routeAttackAmbiguousSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_route_attack_lane_ambiguous_square",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.78,
                    focusSquares = List("h7", "g7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs =
                      List(
                        "source:route_attack_lane",
                        "route_attack_lane_shape",
                        "route_surface_exact",
                        "source:motif_check_pressure",
                        "check_type_normal"
                      )
                  )
                )
            )
          )
      )
    assert(!routeAttackAmbiguousSquareSurface.advancedRows.exists(_.label == "Practical attack"), clue(routeAttackAmbiguousSquareSurface.advancedRows))

    val routeAttackTowardSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_route_attack_lane_toward",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.78,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:route_attack_lane", "route_attack_lane_shape", "route_surface_toward")
                  )
                )
            )
          )
      )
    assert(!routeAttackTowardSurface.advancedRows.exists(_.label == "Practical attack"), clue(routeAttackTowardSurface.advancedRows))

    val routeAttackSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_route_attack_lane_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.78,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:route_attack_lane")
                  )
                )
            )
          )
      )
    assert(!routeAttackSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(routeAttackSourceOnlySurface.advancedRows))

    val directionalAttackLaneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_lane",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.72,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:directional_attack_lane", "directional_attack_lane_shape")
                  )
                )
            )
          )
      )
    assertEquals(directionalAttackLaneSurface.advancedRows.map(_.label), List("Practical attack"), clue(directionalAttackLaneSurface.advancedRows))
    assertEquals(directionalAttackLaneSurface.advancedRows.head.text, "The h7 target gives a practical attacking lane.")
    assertEquals(directionalAttackLaneSurface.advancedRows.head.authority.flatMap(_.target), None)

    val directionalAttackAmbiguousSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_lane_ambiguous_square",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.72,
                    focusSquares = List("h7", "g7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs =
                      List(
                        "source:directional_attack_lane",
                        "directional_attack_lane_shape",
                        "source:initiative_motif",
                        "initiative_motif_shape",
                        "initiative_score_12"
                      )
                  )
                )
            )
          )
      )
    assert(!directionalAttackAmbiguousSquareSurface.advancedRows.exists(_.label == "Practical attack"), clue(directionalAttackAmbiguousSquareSurface.advancedRows))

    val directionalAttackSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_source_only",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.90,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:directional_attack_lane")
                  )
                )
            )
          )
      )
    assert(!directionalAttackSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(directionalAttackSourceOnlySurface.advancedRows))

    val directionalAttackNoZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_no_zone",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.90,
                    focusSquares = List("h7"),
                    focusZone = None,
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:directional_attack_lane", "directional_attack_lane_shape")
                  )
                )
            )
          )
      )
    assert(!directionalAttackNoZoneSurface.advancedRows.exists(_.label == "Practical attack"), clue(directionalAttackNoZoneSurface.advancedRows))

    val directionalAttackNoPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_no_piece",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.90,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = Nil,
                    evidenceRefs = List("source:directional_attack_lane", "directional_attack_lane_shape")
                  )
                )
            )
          )
      )
    assert(!directionalAttackNoPieceSurface.advancedRows.exists(_.label == "Practical attack"), clue(directionalAttackNoPieceSurface.advancedRows))

    val directionalAttackMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_directional_attack_mirrored",
                    readiness = StrategicIdeaReadiness.Build,
                    confidence = 0.90,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:directional_attack_lane", "directional_attack_lane_shape")
                  )
                )
            )
          )
      )
    assert(!directionalAttackMirroredSurface.advancedRows.exists(_.label == "Practical attack"), clue(directionalAttackMirroredSurface.advancedRows))

    val exactBackRankMateSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Back-rank mate",
              text = "The checked line ends in back-rank mate on g8 after Re8#.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_weak_back_rank_exact_visible",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:enemy_weak_back_rank", "enemy_weak_back_rank_shape")
                  )
                )
            )
          )
      )
    assert(!exactBackRankMateSurface.advancedRows.exists(_.label == "Practical attack"), clue(exactBackRankMateSurface.advancedRows))

    val staleBackRankMateSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Back-rank mate",
              text = "The checked line lands a back-rank mate.",
              tone = Some("tactical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_enemy_weak_back_rank_stale_label",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:enemy_weak_back_rank", "enemy_weak_back_rank_shape")
                  )
                )
            )
          )
      )
    assert(!staleBackRankMateSurface.advancedRows.exists(_.label == "Practical attack"), clue(staleBackRankMateSurface.advancedRows))

    val kingRingSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_king_ring_source_only",
                    confidence = 0.78,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:king_ring_pressure")
                  )
                )
            )
          )
      )
    assert(!kingRingSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(kingRingSourceOnlySurface.advancedRows))

    val kingRingNoZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_king_ring_no_zone",
                    confidence = 0.78,
                    focusZone = None,
                    evidenceRefs = List("source:king_ring_pressure", "king_ring_pressure_shape")
                  )
                )
            )
          )
      )
    assert(!kingRingNoZoneSurface.advancedRows.exists(_.label == "Practical attack"), clue(kingRingNoZoneSurface.advancedRows))

    val flankHookSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_hook_pressure",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure", "hook_creation_chance")
                  )
                )
            )
          )
      )
    assertEquals(flankHookSurface.advancedRows.map(_.label), List("Practical attack"), clue(flankHookSurface.advancedRows))
    assertEquals(
      flankHookSurface.advancedRows.head.text,
      "The current flank-pawn map gives a practical hook-creation cue."
    )
    assertEquals(flankHookSurface.advancedRows.head.authority.flatMap(_.target), None)

    val flankHookWithExactMateSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Back-rank mate",
              text = "The checked line ends in back-rank mate on g8 after Re8#.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_hook_with_exact_mate",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure", "hook_creation_chance")
                  )
                )
            )
          )
      )
    assert(flankHookWithExactMateSurface.advancedRows.exists(_.label == "Practical attack"), clue(flankHookWithExactMateSurface.advancedRows))
    assertEquals(
      flankHookWithExactMateSurface.advancedRows.find(_.label == "Practical attack").map(_.text),
      Some("The current flank-pawn map gives a practical hook-creation cue.")
    )

    val flankSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_source_only",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure")
                  )
                )
            )
          )
      )
    assert(!flankSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(flankSourceOnlySurface.advancedRows))

    val rookPawnOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_rook_pawn_ready_only",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure", "rook_pawn_march_ready")
                  )
                )
            )
          )
      )
    assert(!rookPawnOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(rookPawnOnlySurface.advancedRows))

    val exactHookSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Hook creation",
              text = "The checked rook-pawn move creates a flank hook on h4.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_hook_exact_visible",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure", "hook_creation_chance")
                  )
                )
            )
          )
      )
    assert(!exactHookSurface.advancedRows.exists(_.label == "Practical attack"), clue(exactHookSurface.advancedRows))

    val staleHookSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Hook creation",
              text = "The current flank-pawn map gives a practical hook-creation cue.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_hook_stale_label",
                    confidence = 0.77,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_pressure", "hook_creation_chance")
                  )
                )
            )
          )
      )
    assert(staleHookSurface.advancedRows.exists(_.label == "Practical attack"), clue(staleHookSurface.advancedRows))

    val compensationBatterySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_diagonal_battery",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs =
                      List(
                        "source:compensation_diagonal_battery",
                        "compensation_diagonal_battery",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assertEquals(compensationBatterySurface.advancedRows.map(_.label), List("Compensation pressure"), clue(compensationBatterySurface.advancedRows))
    assertEquals(
      compensationBatterySurface.advancedRows.head.text,
      "The material-compensation structure gives practical diagonal-battery pressure."
    )
    assertEquals(
      compensationBatterySurface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )

    val compensationSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_diagonal_source_only",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:compensation_diagonal_battery", "material_deficit_compensation")
                  )
                )
            )
          )
      )
    assert(!compensationSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationSourceOnlySurface.advancedRows))

    val compensationNoMaterialSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_diagonal_without_material_deficit",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:compensation_diagonal_battery")
                  )
                )
            )
          )
      )
    assert(!compensationNoMaterialSurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationNoMaterialSurface.advancedRows))

    val compensationDevelopmentLeadSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_development_lead",
                    confidence = 0.76,
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:compensation_development_lead",
                        "development_lead_compensation",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assertEquals(compensationDevelopmentLeadSurface.advancedRows.map(_.label), List("Compensation pressure"), clue(compensationDevelopmentLeadSurface.advancedRows))
    assertEquals(
      compensationDevelopmentLeadSurface.advancedRows.head.text,
      "The material-compensation structure gives practical development-led pressure."
    )
    assertEquals(
      compensationDevelopmentLeadSurface.advancedRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(compensationDevelopmentLeadSurface.advancedRows.head.authority.flatMap(_.target), None)

    val compensationDevelopmentSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_development_source_only",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:compensation_development_lead", "material_deficit_compensation")
                  )
                )
            )
          )
    )
    assert(!compensationDevelopmentSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationDevelopmentSourceOnlySurface.advancedRows))

    val compensationKingWindowSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_king_window",
                    confidence = 0.74,
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:compensation_king_window",
                        "uncastled_or_unsettled_king_window",
                        "material_deficit_compensation",
                        "source:initiative_motif",
                        "initiative_motif_shape",
                        "initiative_score_12"
                      )
                  )
                )
            )
          )
      )
    assert(!compensationKingWindowSurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationKingWindowSurface.advancedRows))

    val compensationKingWindowSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_king_window_source_only",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:compensation_king_window", "material_deficit_compensation")
                  )
                )
            )
          )
      )
    assert(!compensationKingWindowSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationKingWindowSourceOnlySurface.advancedRows))

    val compensationKingWindowNoZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_king_window_no_zone",
                    confidence = 0.90,
                    focusZone = None,
                    evidenceRefs =
                      List(
                        "source:compensation_king_window",
                        "uncastled_or_unsettled_king_window",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assert(!compensationKingWindowNoZoneSurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationKingWindowNoZoneSurface.advancedRows))

    val compensationKingWindowMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_compensation_king_window_mirrored",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:compensation_king_window",
                        "uncastled_or_unsettled_king_window",
                        "material_deficit_compensation"
                      )
                  )
                )
            )
          )
      )
    assert(!compensationKingWindowMirroredSurface.advancedRows.exists(_.label == "Practical attack"), clue(compensationKingWindowMirroredSurface.advancedRows))


    val motifBatterySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_battery",
                    confidence = 0.74,
                    focusSquares = List("d3", "f3"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:motif_battery", "battery_axis_diagonal")
                  )
                )
            )
          )
      )
    assertEquals(motifBatterySurface.advancedRows.map(_.label), List("Practical attack"), clue(motifBatterySurface.advancedRows))
    assertEquals(motifBatterySurface.advancedRows.head.text, "The current diagonal battery gives a practical attacking cue.")
    assertEquals(motifBatterySurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifBatteryMultiAxisSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_battery_multi_axis",
                    confidence = 0.74,
                    focusSquares = List("d3", "f3", "d1"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q", "R"),
                    evidenceRefs = List("source:motif_battery", "battery_axis_diagonal", "battery_axis_file")
                  )
                )
            )
          )
      )
    assertEquals(motifBatteryMultiAxisSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifBatteryMultiAxisSurface.advancedRows))
    assertEquals(motifBatteryMultiAxisSurface.advancedRows.head.text, "The current battery gives a practical attacking cue.")
    assertEquals(motifBatteryMultiAxisSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifBatterySourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_battery_source_only",
                    confidence = 0.90,
                    focusSquares = List("d3", "f3"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:motif_battery")
                  )
                )
            )
          )
      )
    assert(!motifBatterySourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(motifBatterySourceOnlySurface.advancedRows))

    val motifBatteryNoSquaresSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_battery_no_squares",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:motif_battery", "battery_axis_diagonal")
                  )
                )
            )
          )
      )
    assert(!motifBatteryNoSquaresSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifBatteryNoSquaresSurface.advancedRows))

    val motifBatteryExactSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Battery pressure",
              text = "The checked line forms a bishop-queen battery on the diagonal toward h7.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_battery_exact_visible",
                    confidence = 0.90,
                    focusSquares = List("d3", "f3"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B", "Q"),
                    evidenceRefs = List("source:motif_battery", "battery_axis_diagonal")
                  )
                )
            )
          )
    )
    assert(!motifBatteryExactSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifBatteryExactSurface.advancedRows))

    val motifRookLiftSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift",
                    confidence = 0.78,
                    focusFiles = List("f"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("R"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
      )
    assertEquals(motifRookLiftSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifRookLiftSurface.advancedRows))
    assertEquals(motifRookLiftSurface.advancedRows.head.text, "The rook lift on the f-file gives a practical attacking cue.")
    assertEquals(motifRookLiftSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifRookLiftMultiFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift_multi_file",
                    confidence = 0.78,
                    focusFiles = List("f", "h"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("R"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
      )
    assertEquals(motifRookLiftMultiFileSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifRookLiftMultiFileSurface.advancedRows))
    assertEquals(motifRookLiftMultiFileSurface.advancedRows.head.text, "The rook lift gives a practical attacking cue.")
    assertEquals(motifRookLiftMultiFileSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifRookLiftNoFileSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift_no_file",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("R"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
      )
    assert(!motifRookLiftNoFileSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifRookLiftNoFileSurface.advancedRows))

    val motifRookLiftNoRookSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift_no_rook",
                    confidence = 0.90,
                    focusFiles = List("f"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
      )
    assert(!motifRookLiftNoRookSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifRookLiftNoRookSurface.advancedRows))

    val motifRookLiftExactSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Rook lift",
              text = "The checked line lifts the rook to f3 as attacking infrastructure.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift_exact_visible",
                    confidence = 0.90,
                    focusFiles = List("f"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("R"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
    )
    assert(!motifRookLiftExactSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifRookLiftExactSurface.advancedRows))

    val motifRookLiftStaleSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Rook lift",
              text = "The checked line lifts the rook on the f-file toward the king.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_rook_lift_stale_label",
                    confidence = 0.90,
                    focusFiles = List("f"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("R"),
                    evidenceRefs = List("source:motif_rook_lift")
                  )
                )
            )
          )
      )
    assert(motifRookLiftStaleSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifRookLiftStaleSurface.advancedRows))

    val motifPieceLiftSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift",
                    confidence = 0.72,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:motif_piece_lift", "motif_piece_lift_shape")
                  )
                )
            )
          )
      )
    assertEquals(motifPieceLiftSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifPieceLiftSurface.advancedRows))
    assertEquals(motifPieceLiftSurface.advancedRows.head.text, "The N lift gives a practical attacking cue.")
    assertEquals(motifPieceLiftSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifPieceLiftMultiPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift_multi_piece",
                    confidence = 0.76,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("N", "B"),
                    evidenceRefs = List("source:motif_piece_lift", "motif_piece_lift_shape")
                  )
                )
            )
          )
      )
    assertEquals(motifPieceLiftMultiPieceSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifPieceLiftMultiPieceSurface.advancedRows))
    assertEquals(motifPieceLiftMultiPieceSurface.advancedRows.head.text, "The piece lift gives a practical attacking cue.")
    assertEquals(motifPieceLiftMultiPieceSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifPieceLiftSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift_source_only",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:motif_piece_lift")
                  )
                )
            )
          )
      )
    assert(!motifPieceLiftSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(motifPieceLiftSourceOnlySurface.advancedRows))

    val motifPieceLiftNoZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift_no_zone",
                    confidence = 0.90,
                    focusZone = None,
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:motif_piece_lift", "motif_piece_lift_shape")
                  )
                )
            )
          )
      )
    assert(!motifPieceLiftNoZoneSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifPieceLiftNoZoneSurface.advancedRows))

    val motifPieceLiftNoPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift_no_piece",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = Nil,
                    evidenceRefs = List("source:motif_piece_lift", "motif_piece_lift_shape")
                  )
                )
            )
          )
      )
    assert(!motifPieceLiftNoPieceSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifPieceLiftNoPieceSurface.advancedRows))

    val motifPieceLiftMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_piece_lift_mirrored",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:motif_piece_lift", "motif_piece_lift_shape")
                  )
                )
            )
          )
      )
    assert(!motifPieceLiftMirroredSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifPieceLiftMirroredSurface.advancedRows))

    val motifCheckPressureSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_pressure",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.68,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assertEquals(motifCheckPressureSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifCheckPressureSurface.advancedRows))
    assertEquals(motifCheckPressureSurface.advancedRows.head.text, "The Q check on h7 gives a practical attacking cue.")
    assertEquals(motifCheckPressureSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifCheckMultiPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_multi_piece",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.72,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q", "N"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assertEquals(motifCheckMultiPieceSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifCheckMultiPieceSurface.advancedRows))
    assertEquals(motifCheckMultiPieceSurface.advancedRows.head.text, "The check on h7 gives a practical attacking cue.")
    assertEquals(motifCheckMultiPieceSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifCheckAmbiguousSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_ambiguous_square",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.68,
                    focusSquares = List("h7", "g7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assertEquals(motifCheckAmbiguousSquareSurface.advancedRows.map(_.label), List("Practical attack"), clue(motifCheckAmbiguousSquareSurface.advancedRows))
    assertEquals(motifCheckAmbiguousSquareSurface.advancedRows.head.text, "The Q check motif gives a practical attacking cue.")
    assertEquals(motifCheckAmbiguousSquareSurface.advancedRows.head.authority.flatMap(_.target), None)

    val motifCheckSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_source_only",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.80,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure")
                  )
                )
            )
          )
      )
    assert(!motifCheckSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(motifCheckSourceOnlySurface.advancedRows))

    val motifCheckNoSquareSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_no_square",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.80,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assert(!motifCheckNoSquareSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifCheckNoSquareSurface.advancedRows))

    val motifCheckNoZoneSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_no_zone",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.80,
                    focusSquares = List("h7"),
                    focusZone = None,
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assert(!motifCheckNoZoneSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifCheckNoZoneSurface.advancedRows))

    val motifCheckNoPieceSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_check_no_piece",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.80,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = Nil,
                    evidenceRefs = List("source:motif_check_pressure", "check_type_normal")
                  )
                )
            )
          )
      )
    assert(!motifCheckNoPieceSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifCheckNoPieceSurface.advancedRows))

    val motifMateCheckSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_motif_mate_check",
                    readiness = StrategicIdeaReadiness.Ready,
                    confidence = 0.80,
                    focusSquares = List("h7"),
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("Q"),
                    evidenceRefs = List("source:motif_check_pressure", "check_type_mate")
                  )
                )
            )
          )
      )
    assert(!motifMateCheckSurface.advancedRows.exists(_.label == "Practical attack"), clue(motifMateCheckSurface.advancedRows))

    val flankPawnAdvanceMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_pawn_advance_motif",
                    confidence = 0.70,
                    focusFiles = List("g"),
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:flank_pawn_advance_motif",
                        "flank_pawn_advance_shape",
                        "flank_pawn_file_g",
                        "flank_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assertEquals(flankPawnAdvanceMotifSurface.advancedRows.map(_.label), List("Practical attack"), clue(flankPawnAdvanceMotifSurface.advancedRows))
    assertEquals(
      flankPawnAdvanceMotifSurface.advancedRows.head.text,
      "The g-pawn advance gives a practical kingside attacking cue."
    )
    assertEquals(flankPawnAdvanceMotifSurface.advancedRows.head.authority.flatMap(_.target), None)

    val multiFlankPawnAdvanceMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_multi_flank_pawn_advance_motif",
                    confidence = 0.74,
                    focusFiles = List("a", "g"),
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:flank_pawn_advance_motif",
                        "flank_pawn_advance_shape",
                        "flank_pawn_file_a",
                        "flank_pawn_file_g",
                        "flank_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assertEquals(multiFlankPawnAdvanceMotifSurface.advancedRows.map(_.label), List("Practical attack"), clue(multiFlankPawnAdvanceMotifSurface.advancedRows))
    assertEquals(
      multiFlankPawnAdvanceMotifSurface.advancedRows.head.text,
      "The flank-pawn advances give a practical attacking cue."
    )
    assertEquals(multiFlankPawnAdvanceMotifSurface.advancedRows.head.authority.flatMap(_.target), None)

    val flankPawnAdvanceSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_pawn_advance_source_only",
                    confidence = 0.90,
                    focusFiles = List("g"),
                    focusZone = Some("kingside"),
                    evidenceRefs = List("source:flank_pawn_advance_motif")
                  )
                )
            )
          )
      )
    assert(!flankPawnAdvanceSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(flankPawnAdvanceSourceOnlySurface.advancedRows))

    val flankPawnAdvanceExactSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Hook creation",
              text = "The checked rook-pawn move creates a flank hook on h4.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_flank_pawn_advance_exact_visible",
                    confidence = 0.90,
                    focusFiles = List("g"),
                    focusZone = Some("kingside"),
                    evidenceRefs =
                      List(
                        "source:flank_pawn_advance_motif",
                        "flank_pawn_advance_shape",
                        "flank_pawn_file_g",
                        "flank_pawn_to_rank_4"
                      )
                  )
                )
            )
          )
      )
    assert(!flankPawnAdvanceExactSurface.advancedRows.exists(_.label == "Practical attack"), clue(flankPawnAdvanceExactSurface.advancedRows))

    val fianchettoMotifSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_fianchetto_motif",
                    confidence = 0.70,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B"),
                    evidenceRefs = List("source:fianchetto_motif", "fianchetto_motif_shape", "fianchetto_side_kingside")
                  )
                )
            )
          )
      )
    assertEquals(fianchettoMotifSurface.advancedRows.map(_.label), List("Practical attack"), clue(fianchettoMotifSurface.advancedRows))
    assertEquals(fianchettoMotifSurface.advancedRows.head.text, "The fianchettoed bishop gives a practical long-diagonal cue.")
    assertEquals(fianchettoMotifSurface.advancedRows.head.authority.flatMap(_.target), None)

    val fianchettoMotifSourceOnlySurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_fianchetto_motif_source_only",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B"),
                    evidenceRefs = List("source:fianchetto_motif")
                  )
                )
            )
          )
      )
    assert(!fianchettoMotifSourceOnlySurface.advancedRows.exists(_.label == "Practical attack"), clue(fianchettoMotifSourceOnlySurface.advancedRows))

    val fianchettoMotifNoBishopSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_fianchetto_motif_no_bishop",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("N"),
                    evidenceRefs = List("source:fianchetto_motif", "fianchetto_motif_shape", "fianchetto_side_kingside")
                  )
                )
            )
          )
      )
    assert(!fianchettoMotifNoBishopSurface.advancedRows.exists(_.label == "Practical attack"), clue(fianchettoMotifNoBishopSurface.advancedRows))

    val fianchettoMotifMirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_fianchetto_motif_mirrored",
                    confidence = 0.90,
                    focusZone = Some("kingside"),
                    beneficiaryPieces = List("B"),
                    evidenceRefs = List("source:fianchetto_motif", "fianchetto_motif_shape", "fianchetto_side_kingside")
                  )
                )
            )
          )
      )
    assert(!fianchettoMotifMirroredSurface.advancedRows.exists(_.label == "Practical attack"), clue(fianchettoMotifMirroredSurface.advancedRows))

    val missingStructureSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas =
                List(
                  fianchettoIdea.copy(
                    ideaId = "idea_fianchetto_without_structure",
                    evidenceRefs = List("source:fianchetto_assault_profile", "source:opposite_side_storm")
                  )
                )
            )
          )
      )
    assert(!missingStructureSurface.advancedRows.exists(_.label == "Practical attack"), clue(missingStructureSurface.advancedRows))

    val mirroredSurface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "black",
              strategicIdeas = List(fianchettoIdea.copy(ideaId = "idea_mismatched_fianchetto_assault"))
            )
          )
      )
    assert(!mirroredSurface.advancedRows.exists(_.label == "Practical attack"), clue(mirroredSurface.advancedRows))

    val exactMateSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Mate net",
              text = "The checked line ends in mate net on g8 after Qg7#.",
              authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(fianchettoIdea)
            )
          )
    )
    assert(!exactMateSurface.advancedRows.exists(_.label == "Practical attack"), clue(exactMateSurface.advancedRows))

    val staleMateSurface =
      build(
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Mate net",
              text = "The checked line creates a mate net around g8.",
              tone = Some("practical")
            )
          ),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(fianchettoIdea)
            )
          )
      )
    assert(staleMateSurface.advancedRows.exists(_.label == "Practical attack"), clue(staleMateSurface.advancedRows))
  }

  test("resolved compensation surface reaches bounded advanced rows") {
    val surface =
      build(strategyPack = Some(anchoredCompensationPack))
    val compensationRows =
      surface.advancedRows.filter(_.label.startsWith("Compensation"))

    assertEquals(compensationRows.map(_.label), List("Compensation", "Compensation condition"), clue(surface.advancedRows))
    assert(compensationRows.exists(_.text.contains("gives up material")), clue(compensationRows))
    assert(compensationRows.exists(_.text.toLowerCase.contains("queenside targets")), clue(compensationRows))
    assert(
      compensationRows.find(_.label == "Compensation condition").exists(_.text.toLowerCase.contains("stay under pressure")),
      clue(compensationRows)
    )
    assert(
      compensationRows.forall(
        _.authority.contains(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
      ),
      clue(compensationRows)
    )
  }

  test("truth contract compensation prose veto suppresses resolved compensation rows") {
    val contract =
      tacticalTruthContract().copy(
        truthClass = DecisiveTruthClass.Acceptable,
        cpLoss = 0,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        visibilityRole = TruthVisibilityRole.Hidden,
        surfaceMode = TruthSurfaceMode.Neutral,
        failureMode = FailureInterpretationMode.NoClearPlan
      )

    assert(!contract.blocksStrategicSupport)

    val surface =
      build(
        strategyPack = Some(anchoredCompensationPack),
        truthContract = Some(contract)
      )

    assert(!surface.advancedRows.exists(_.label.startsWith("Compensation")), clue(surface.advancedRows))
  }

  test("generic compensation shells do not create player surface rows") {
    val surface =
      build(strategyPack = Some(genericCompensationPack))

    assert(!surface.advancedRows.exists(_.label.startsWith("Compensation")), clue(surface.advancedRows))
  }

  test("strategic relation evidence appears as bounded advanced metadata") {
    val relationIdea =
      relationIdeaSignal(
        kind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        group = "line_occupation",
        evidenceRefs =
          Some(
            RelationObservationCatalog
              .descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay)
              .get
              .wireEvidenceRefs ++ List("blocker:f5")
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.label, "Line relation")
    assertEquals(row.tone, Some("relation"))
    assertEquals(row.source, None)
    assertEquals(
      row.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    )
    assert(row.text.contains("x-ray geometry"), clue(row))
    assert(!row.text.contains("gives"), clue(row))
  }

  test("specific supported-local relation rows suppress duplicate strategic relation rows without stale clearance suppression") {
    val ideas =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
        MoveReviewExchangeAnalyzer.RelationKind.MateNet
      ).map { kind =>
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$kind",
          focusSquares = List("b1", "d3", "h7"),
          targetSquare = Some("h7")
        )
      }
    val supportedClearance =
      MoveReviewPlayerSurfaceRow(
        label = "Clearance",
        text = "The checked line clears d3, opening the bishop on b1 toward h7.",
        authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
      )
    val supportedDefenderTrade =
      MoveReviewPlayerSurfaceRow(
        label = "Defender trade",
        text = "The checked line trades on d4 to remove the defender from c5, loosening e5.",
        authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
      )
    val supportedSmotheredMate =
      MoveReviewPlayerSurfaceRow(
        label = "Smothered mate",
        text = "The checked line ends in smothered mate on h8 after h6f7.",
        authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
      )
    val surface =
      build(
        supportedLocalRows = List(supportedClearance, supportedDefenderTrade, supportedSmotheredMate),
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val relationTokens =
      surface.advancedRows.flatMap(row => row.authority.flatMap(_.token))

    assertEquals(surface.summaryRows.map(_.label), List("Clearance", "Defender trade", "Smothered mate"), clue(surface.summaryRows))
    assertEquals(
      relationTokens,
      List(MoveReviewExchangeAnalyzer.RelationKind.XRay, MoveReviewExchangeAnalyzer.RelationKind.Clearance),
      clue(surface.advancedRows)
    )
    assert(!relationTokens.contains(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade), clue(surface.advancedRows))
    assert(!relationTokens.contains(MoveReviewExchangeAnalyzer.RelationKind.MateNet), clue(surface.advancedRows))
  }

  test("strategic relation surface can publish four typed relation witnesses") {
    val relationKinds =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        MoveReviewExchangeAnalyzer.RelationKind.Fork,
        MoveReviewExchangeAnalyzer.RelationKind.Pin,
        MoveReviewExchangeAnalyzer.RelationKind.Skewer
    )
    val ideas =
      relationKinds.zipWithIndex.map { case (kind, idx) =>
        val focus = List(s"a${idx + 1}", s"b${idx + 1}", s"c${idx + 1}")
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$idx",
          focusSquares = focus,
          targetSquare = focus.lastOption
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val relationRows =
      surface.advancedRows.filter(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))

    assertEquals(relationRows.flatMap(_.authority.flatMap(_.token)), relationKinds.take(4))
    assert(!relationRows.exists(_.authority.flatMap(_.token).contains(MoveReviewExchangeAnalyzer.RelationKind.Skewer)), clue(relationRows))
  }

  test("draw-resource strategic relation rows survive the four-row cap") {
    val relationKinds =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        MoveReviewExchangeAnalyzer.RelationKind.Fork,
        MoveReviewExchangeAnalyzer.RelationKind.Pin,
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck
    )
    val ideas =
      relationKinds.zipWithIndex.map { case (kind, idx) =>
        val focus = List(s"a${idx + 1}", s"b${idx + 1}", s"c${idx + 1}")
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$idx",
          focusSquares = focus,
          targetSquare = focus.lastOption
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val relationRows =
      surface.advancedRows.filter(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
    val tokens = relationRows.flatMap(_.authority.flatMap(_.token))

    assertEquals(
      tokens,
      List(
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance
      )
    )
    assertEquals(relationRows.take(2).map(_.label), List("Draw resource", "Draw resource"))
    assert(!tokens.contains(MoveReviewExchangeAnalyzer.RelationKind.Fork), clue(relationRows))
  }

  test("witness-only board relation rows survive the four-row cap") {
    val relationKinds =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.XRay,
        MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        MoveReviewExchangeAnalyzer.RelationKind.Fork,
        MoveReviewExchangeAnalyzer.RelationKind.Pin,
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
        MoveReviewExchangeAnalyzer.RelationKind.Domination,
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug
    )
    val ideas =
      relationKinds.zipWithIndex.map { case (kind, idx) =>
        val focus = List(s"a${idx + 1}", s"b${idx + 1}", s"c${idx + 1}")
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$idx",
          focusSquares = focus,
          targetSquare = focus.lastOption
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val relationRows =
      surface.advancedRows.filter(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
    val tokens = relationRows.flatMap(_.authority.flatMap(_.token))

    assertEquals(
      tokens,
      List(
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
        MoveReviewExchangeAnalyzer.RelationKind.Domination,
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
        MoveReviewExchangeAnalyzer.RelationKind.XRay
      )
    )
    assertEquals(relationRows.take(2).map(_.label), List("Mobility restriction", "Mobility restriction"))
    assertEquals(relationRows.lift(2).map(_.label), Some("Move-order relation"))
    assert(!tokens.contains(MoveReviewExchangeAnalyzer.RelationKind.Fork), clue(relationRows))
  }

  test("strategic relation row requires catalog source, semantic observation fact, and witness fact") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val sourceOnlyIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "blocker:f5"),
        relationKind = Some(xray.relationKind),
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val semanticOnlyIdea =
      sourceOnlyIdea.copy(
        ideaId = "idea_2",
        evidenceRefs = List("xray_semantic", "blocker:f5")
      )
    val sourceSemanticOnlyIdea =
      sourceOnlyIdea.copy(
        ideaId = "idea_3",
        evidenceRefs = List("source:xray_relation", "xray_semantic")
      )

    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(sourceOnlyIdea, semanticOnlyIdea, sourceSemanticOnlyIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("strategic relation projection requires selected relation kind") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val unselectedIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs,
        relationKind = None,
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(unselectedIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation projection requires relation-specific focus") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs,
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = Nil
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation surface target prefers analyzer-carried target over focus order") {
    val relationIdea =
      relationIdeaSignal(
        kind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        group = "line_occupation",
        focusSquares = List("g6", "f5", "e4"),
        targetSquare = Some("g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assertEquals(
      surface.advancedRows.flatMap(_.authority.flatMap(_.target)).headOption,
      Some("g6"),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation surface target ignores targetSquare outside relation focus") {
    val relationIdea =
      relationIdeaSignal(
        kind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        group = "line_occupation",
        focusSquares = List("e4", "f5", "g6"),
        targetSquare = Some("a1")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assertEquals(
      surface.advancedRows.flatMap(_.authority.flatMap(_.target)).headOption,
      Some("g6"),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation projection rejects unselected legacy target carriers") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = xray.ideaKind,
        group = "line_occupation",
        readiness = xray.readiness,
        focusSquares = List("e4", "f5", "g6"),
        confidence = xray.confidence,
        evidenceRefs = xray.wireEvidenceRefs,
        targetSquare = Some("f5"),
        relationKind = None,
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("strategic relation fallback target policy is catalog-owned") {
    val ideas =
      List(
        MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade -> List("c5", "a3"),
        MoveReviewExchangeAnalyzer.RelationKind.Decoy -> List("f4", "d3", "d5")
      ).map { case (kind, focus) =>
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$kind",
          group = StrategicIdeaGroup.InteractionAndTransformation,
          focusSquares = focus
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val targetsByRelation =
      surface.advancedRows.flatMap(row =>
        row.authority.flatMap(authority => authority.token.map(_ -> authority.target))
      ).toMap

    assertEquals(targetsByRelation.get(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade).flatten, Some("c5"))
    assertEquals(targetsByRelation.get(MoveReviewExchangeAnalyzer.RelationKind.Decoy).flatten, Some("d3"))

    val root = java.nio.file.Paths.get("").toAbsolutePath
    val builderText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala")
      )
    val builderTextOutsideRelationPolicy =
      builderText.replaceAll(
        """(?s)private val PracticalRelationKindByLabel =\s*Map\(.+?\)\s*private val IqpInducementFamily""",
        "private val IqpInducementFamily"
      ).replaceAll(
        """(?s)private def exchangeOwnershipRow\(.+?\)\s*:\s*Option\[MoveReviewPlayerSurfaceRow\]\s*=\s*.+?\s*private def defenderTradeBranch""",
        "private def defenderTradeBranch"
      )
    assert(!builderTextOutsideRelationPolicy.contains("MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade"), clues(builderText))
    assert(!builderTextOutsideRelationPolicy.contains("MoveReviewExchangeAnalyzer.RelationKind.Decoy"), clues(builderText))
    assert(!builderTextOutsideRelationPolicy.contains("def relationTarget("), clues(builderText))
  }

  test("strategic relation projection honors selected relationKind before catalog order") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6", "d7"),
        confidence = 0.72,
        evidenceRefs =
          xray.wireEvidenceRefs ++ clearance.wireEvidenceRefs,
        targetSquare = Some("d7"),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = List("d1", "d3", "d7")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Clearance), clue(row))
    assert(row.text.contains("clearance geometry"), clue(row))
    assert(row.text.contains("d3 clearing the line from d1 toward d7"), clue(row))
    assert(!row.text.contains("e4, f5, g6"), clue(row))
  }

  test("selected strategic relation rows require relation-specific focus") {
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = clearance.wireEvidenceRefs,
        targetSquare = Some("g6"),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = Nil
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation projection preserves typed relation geometry") {
    val relationShapes: List[(String, List[String], String, String, List[String])] =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
          List("c5", "d4"),
          "c5",
          "defender-trade around c5 through the exchange on d4",
          List("c5", "d4")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation,
          List("c8", "d7"),
          "d7",
          "bad-piece liquidation from c8 through the exchange on d7",
          List("c8", "d7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Overload,
          List("f7", "h7", "d7"),
          "h7",
          "overload pressure on f7 across h7 and d7",
          List("f7", "h7", "d7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          List("g7", "f8", "a3"),
          "g7",
          "deflection motif on g7 by attacking f8 from a3",
          List("g7", "f8", "a3")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
          List("b1", "d3", "h7"),
          "h7",
          "discovered-attack from b1 through d3 toward h7",
          List("b1", "d3", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          List("e8", "a4", "d5"),
          "e8",
          "double-check pressure on e8 from a4 and d5",
          List("e8", "a4", "d5")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          List("h8", "h1"),
          "h8",
          "back-rank mate pattern around h8 from h1",
          List("h8", "h1")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.MateNet,
          List("g8", "f7", "h6"),
          "g8",
          "mate net around g8 from f7 and h6",
          List("g8", "f7", "h6")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
          List("d3", "h7"),
          "h7",
          "Greek gift sacrifice from d3 toward h7",
          List("d3", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          List("h8", "h7"),
          "h8",
          "stalemate resource available around h8 via h7",
          List("h8", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          List("g8", "g6", "g7"),
          "g8",
          "perpetual-check resource available around g8 from g6 and g7",
          List("g8", "g6", "g7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Fork,
          List("e4", "c5", "g5"),
          "c5",
          "fork from e4 across c5 and g5",
          List("e4", "c5", "g5")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
          List("b1", "h7"),
          "h7",
          "hanging piece pressure from b1 on h7",
          List("b1", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          List("f4", "d3", "d5"),
          "d3",
          "decoy motif on d3 that pulls from d5",
          List("d3", "d5")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          List("g7", "h8"),
          "h8",
          "trapped-piece pressure on h8 from g7",
          List("g7", "h8")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Domination,
          List("b7", "a8", "b6", "c7"),
          "a8",
          "key-square restriction on a8 from b7",
          List("b7", "a8")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          List("g6", "f5", "g8"),
          "f5",
          "zwischenzug from g6 before the recapture on f5",
          List("g6", "f5")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.XRay,
          List("e4", "f5", "g6"),
          "g6",
          "x-ray geometry from e4 through f5 toward g6",
          List("e4", "f5", "g6")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          List("b1", "d3", "h7"),
          "h7",
          "clearance geometry with d3 clearing the line from b1 toward h7",
          List("b1", "d3", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Battery,
          List("d3", "b1", "h7"),
          "h7",
          "battery geometry between d3 and b1 toward h7",
          List("d3", "b1", "h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Pin,
          List("b4", "c3", "e1"),
          "c3",
          "pin geometry from b4 through c3 toward e1",
          List("b4", "c3", "e1")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          List("e4", "d5", "c6"),
          "d5",
          "skewer geometry from e4 through d5 toward c6",
          List("e4", "d5", "c6")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Interference,
          List("d6", "d8", "d5"),
          "d5",
          "interference geometry with d6 between d8 and d5",
          List("d6", "d8", "d5")
        )
      )
    relationShapes.foreach { case (kind, focus, target, labelText, requiredFragments) =>
      val idea =
        relationIdeaSignal(
          kind = kind,
          ideaId = s"idea_$kind",
          group = StrategicIdeaGroup.InteractionAndTransformation,
          focusSquares = List("a1", "a2", "a3"),
          targetSquare = Some(target),
          relationFocusSquares = Some(focus)
        )
      val surface =
        build(
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                strategicIdeas = List(idea)
              )
            )
        )
      val row =
        surface.advancedRows
          .find(_.authority.flatMap(_.token).contains(kind))
          .getOrElse(fail(s"missing $kind relation row: ${surface.advancedRows}"))

      assert(row.text.contains(labelText), clue(row))
      requiredFragments.foreach(fragment => assert(row.text.contains(fragment), clues(fragment, row)))
      assert(!row.text.contains("a1, a2, a3"), clue(row))
      assertEquals(row.authority.flatMap(_.target), Some(target), clue(row))
    }
  }

  test("strategic relation projection does not fall back when selected relationKind lacks matching evidence") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs =
          List(
            EvidenceRef.Source(xray.source).wireKey,
            xray.observationId.wireKey
          ),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("every cataloged relation descriptor can project through the bounded surface row") {
    RelationObservationCatalog.Implemented.foreach { descriptor =>
      val relationIdea =
        relationIdeaSignal(
          kind = descriptor.relationKind,
          ideaId = s"idea_${descriptor.relationKind}",
          group =
            if descriptor.ideaKind == StrategicIdeaKind.LineOccupation then StrategicIdeaGroup.PieceAndLineManagement
            else StrategicIdeaGroup.InteractionAndTransformation,
          focusSquares = List("e4", "f5", "g6"),
          relationFocusSquares = Some(List("e4", "f5", "g6"))
        )
      val surface =
        build(
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                strategicIdeas = List(relationIdea)
              )
            )
        )
      val row =
        surface.advancedRows
          .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
          .getOrElse(fail(s"missing ${descriptor.relationKind} relation row: ${surface.advancedRows}"))

      assertEquals(
        row.label,
        descriptor.surfaceRowLabel,
        clue(descriptor.relationKind)
      )
      assert(row.text.contains(descriptor.publicLabel), clue(row))
      assert(!row.text.contains("gives"), clue(row))
      assert(!row.text.contains("checked line"), clue(row))
      if descriptor.surfaceRowKind == RelationSurfaceRowKind.MobilityRestriction then
        assert(row.text.startsWith("The mobility relation is "), clue(row))
      assertEquals(row.authority.flatMap(_.token), Some(descriptor.relationKind), clue(row))
    }
  }

  test("deferred relation kinds never project through strategic relation surface rows") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get

    RelationObservationCatalog.DeferredRelationKinds.foreach { deferredKind =>
      val relationIdea =
        StrategyIdeaSignal(
          ideaId = s"idea_$deferredKind",
          ownerSide = "white",
          kind = StrategicIdeaKind.LineOccupation,
          group = StrategicIdeaGroup.PieceAndLineManagement,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("e4", "f5", "g6"),
          confidence = 0.72,
          evidenceRefs = List(EvidenceRef.Source(xray.source).wireKey, xray.observationId.wireKey),
          relationKind = Some(deferredKind),
          relationFocusSquares = List("e4", "f5", "g6")
        )
      val surface =
        build(
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                strategicIdeas = List(relationIdea)
              )
            )
        )

      assert(
        !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
        clue(deferredKind)
      )
    }
  }

  test("relation-only StrategyPackBuilder output reaches strategic relation advanced row") {
    val fen = "6k1/5ppp/6q1/5n2/8/8/8/1B5K w - - 0 1"
    val playedMove = "b1e4"
    val ctx = relationOnlyContext(fen, playedMove)
    val strategyPack =
      StrategyPackBuilder
        .build(relationOnlyData(fen, playedMove), ctx)
        .getOrElse(fail("relation-only strategy pack missing"))

    assert(strategyPack.strategicIdeas.exists(_.evidenceRefs.contains("source:xray_relation")), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.targetSquare.contains("g6")), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.XRay)), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.relationFocusSquares.contains("g6")), clue(strategyPack))

    val surface = build(ctx = ctx, strategyPack = Some(strategyPack))
    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.label, "Line relation")
    assertEquals(row.source, None)
    assertEquals(
      row.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    )
  }

  test("bad tactical truth contract suppresses player strategic and practical support rows") {
    val relationIdea =
      relationIdeaSignal(
        kind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        ideaId = "idea_xray",
        group = StrategicIdeaGroup.PieceAndLineManagement,
        focusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                name = "Validated central plan",
                executionSteps = List("Keep the rook on d1"),
                preconditions = List("The d-file remains controlled")
              ),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            ),
            evaluated(plan("Structural backup"), UserFacingPlanEligibility.StructuralOnly)
          ),
        supportedLocalRows =
          List(
            MoveReviewPlayerSurfaceRow(
              label = "Central break",
              text = "On the checked line, this also plays the d4-d5 break at this moment."
            )
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", strategicIdeas = List(relationIdea))),
        truthContract = Some(tacticalTruthContract())
      )

    assertEquals(surface.summaryRows, Nil)
    assertEquals(surface.advancedRows, Nil)
  }

  test("probe-backed plans only release rows from the promoted plan") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated central plan"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Execution"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Objective"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Idea"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Prophylaxis"), clue(surface.advancedRows))
  }

  test("execution objective and prophylaxis rows come from the promoted plan itself") {
    val promoted =
      plan(
        name = "Deny the counter-break",
        executionSteps = List("Keep the rook on d1", "Answer ...d5 with exd5"),
        preconditions = List("The d-file remains controlled"),
        failureModes = List("If ...d5 works, counterplay returns"),
        themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
        subplanId = Some(PlanTaxonomy.PlanKind.BreakPrevention.id)
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(surface.advancedRows.exists(row => row.label == "Execution" && row.text.contains("Keep the rook on d1")))
    assert(surface.advancedRows.exists(row => row.label == "Objective" && row.text.contains("d-file remains controlled")))
    assert(surface.advancedRows.exists(row => row.label == "Prophylaxis" && row.text.contains("Deny the counter-break")))
  }

  private def surfaceLineEvidence(): LineConsequenceEvidence =
    LineConsequenceEvidence(
      lineId = Some("exchange"),
      sanMoves = List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6"),
      uciMoves = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6"),
      scoreCp = Some(42),
      mate = None,
      depth = Some(20),
      windowPly = 6,
      kind = LineConsequenceKind.ExchangeSequence,
      triggerSan = Some("Bxc6"),
      consequence = "The checked line reaches an exchange sequence after Bxc6.",
      whyItMatters = Some("The decision is about which structure remains."),
      release = LineConsequenceRelease.SurfaceCandidate,
      rejectReasons = Nil
    )

  private def roleAwareDecisionComparison(): DecisionComparison =
    DecisionComparison(
      chosenMove = Some("gxf5"),
      engineBestMove = Some("exf5"),
      engineBestScoreCp = Some(-44),
      engineBestPv = List("exf5", "Rfd8", "Ne4", "Bd5"),
      cpLossVsChosen = Some(96),
      deferredMove = Some("exf5"),
      deferredReason = Some("role-aware line consequence"),
      deferredSource = Some("verified_best"),
      evidence = Some("exf5 reaches a material transition; gxf5 stays on the played branch."),
      practicalAlternative = false,
      chosenMatchesBest = false,
      comparedMove = Some("gxf5"),
      comparativeConsequence =
        Some(
          "exf5 reaches a material transition on the engine-best branch; gxf5 reaches a different material transition on the played branch."
        ),
      comparativeSource = Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
      roleAwareBranchEvidence =
        Some(
          RoleAwareLineConsequenceEvidence(
            engineBest =
              surfaceLineEvidence().copy(
                lineId = Some("best"),
                sanMoves = List("exf5", "Rfd8", "Ne4", "Bd5"),
                uciMoves = List("e4f5", "f8d8", "d2e4", "f7d5"),
                scoreCp = Some(-44),
                triggerSan = Some("exf5"),
                kind = LineConsequenceKind.MaterialTransition,
                consequence = "The engine-best branch reaches a material transition after exf5.",
                whyItMatters = Some("The engine-best branch keeps the cleaner material transition.")
              ),
            played =
              surfaceLineEvidence().copy(
                lineId = Some("played"),
                sanMoves = List("gxf5", "a4", "h4", "Bh5"),
                uciMoves = List("g4f5", "a5a4", "h3h4", "f7h5"),
                scoreCp = Some(-140),
                triggerSan = Some("gxf5"),
                kind = LineConsequenceKind.MaterialTransition,
                consequence = "The played branch reaches a different material transition after gxf5.",
                whyItMatters = Some("The played branch is the checked alternative branch.")
              )
          )
        )
    )

  private def roleAwareAlternativeLocalFact: MoveReviewLocalFact.Admission =
    MoveReviewLocalFact.Admission(
      family = MoveReviewLocalFact.Family.LineConsequence,
      authority = MoveReviewLocalFact.Authority.AlternativeComparison,
      producer = MoveReviewLocalFact.Producer.AlternativeComparison,
      strictFallbackEligible = true,
      lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled
    )

  private def neutralizePacket(token: String): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List(token),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression(token))
        )
    )
