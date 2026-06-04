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
      truthContract: Option[DecisiveTruthContract] = None
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
      truthContract = truthContract
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
  }

  test("pv-coupled evaluated plans create practical plan rows but not main plans") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("central pressure"),
              UserFacingPlanEligibility.PvCoupledOnly
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
    assertEquals(surface.summaryRows.map(_.label), List("Practical plan"))
    assertEquals(
      surface.summaryRows.head.text,
      "The checked line keeps central pressure viable as a practical plan."
    )
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

  test("pv-coupled evaluated plans create practical advanced detail rows") {
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

    assertEquals(surface.advancedRows.map(_.label), List("Practical objective", "Practical steps"))
    assert(surface.advancedRows.exists(row => row.text == "The e-file remains tense"))
    assert(surface.advancedRows.exists(row => row.text == "Keep the knight centralized"))
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

  test("weakness practical target row requires a best-line persistence witness") {
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

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
  }

  test("weakness practical target row requires a non-empty best-line continuation") {
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

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
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

  test("weakness practical target row is suppressed when the best line horizon is too short") {
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

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
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
    val builderTextOutsidePracticalRelationMap =
      builderText.replaceAll(
        """(?s)private val PracticalRelationKindByLabel =\s*Map\(.+?\)\s*private val IqpInducementFamily""",
        "private val IqpInducementFamily"
      )
    assert(!builderTextOutsidePracticalRelationMap.contains("MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade"), clues(builderText))
    assert(!builderTextOutsidePracticalRelationMap.contains("MoveReviewExchangeAnalyzer.RelationKind.Decoy"), clues(builderText))
    assert(!builderTextOutsidePracticalRelationMap.contains("def relationTarget("), clues(builderText))
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
      if descriptor.surfaceRowKind == RelationSurfaceRowKind.MobilityRestriction then
        assert(row.text.startsWith("The checked line limits piece mobility with "), clue(row))
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
