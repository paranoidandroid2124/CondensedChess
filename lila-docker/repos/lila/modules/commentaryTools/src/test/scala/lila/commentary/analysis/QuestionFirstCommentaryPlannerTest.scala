package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.authoring.*
import lila.commentary.model.strategic.{ EngineEvidence, PvMove, VariationLine }
import lila.commentary.analysis.claim.PlayerFacingClaimPrefixKind

class QuestionFirstCommentaryPlannerTest extends FunSuite:

  private def baseCtx(
      questions: List[AuthorQuestion],
      evidence: List[QuestionEvidence] = Nil,
      opponentPlan: Option[PlanRow] = None
  ): NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some("e2e4"),
      playedSan = Some("e4"),
      summary = NarrativeSummary("Central pressure", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      opponentPlan = opponentPlan,
      authorQuestions = questions,
      authorEvidence = evidence,
      renderMode = NarrativeRenderMode.FullGame
    )

  private def question(
      id: String,
      kind: AuthorQuestionKind,
      priority: Int = 100,
      evidencePurposes: List[String] = Nil
  ): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = kind,
      priority = priority,
      question = s"placeholder-$id",
      evidencePurposes = evidencePurposes
    )

  private def evidence(questionId: String, purpose: String, lines: List[String]): QuestionEvidence =
    QuestionEvidence(
      questionId = questionId,
      purpose = purpose,
      branches = lines.zipWithIndex.map { case (line, idx) =>
        EvidenceBranch(
          keyMove = s"line_${idx + 1}",
          line = line,
          evalCp = Some(30 - idx * 10)
        )
      }
    )

  private def mainClaim(
      text: String,
      mode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      sourceKind: String = "main_delta",
      packet: Option[PlayerFacingClaimPacket] = None,
      deltaClass: PlayerFacingMoveDeltaClass = PlayerFacingMoveDeltaClass.PlanAdvance
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = mode,
      deltaClass = Some(deltaClass),
      claimText = text,
      anchorTerms = List("e5"),
      evidenceLines = List("14...Rc8 15.Re1 Qc7"),
      sourceKind = sourceKind,
      tacticalOwnership = Option.when(mode == PlayerFacingTruthMode.Tactical)("tactical"),
      packet = packet
    )

  private def positionLocalClaim(
      text: String,
      sourceKind: String = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
      packet: PlayerFacingClaimPacket,
      prefixKind: PlayerFacingClaimPrefixKind = PlayerFacingClaimPrefixKind.KeyStrategicFact,
      anchorTerms: List[String] = List("c6")
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.PositionLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.PressureIncrease),
      claimText = text,
      anchorTerms = anchorTerms,
      evidenceLines = List("14...Qb6 15.Rb1"),
      sourceKind = sourceKind,
      tacticalOwnership = None,
      prefixKind = prefixKind,
      packet = Some(packet)
    )


  private def lineClaim(text: String, sourceKind: String = "line_delta"): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.LineScoped,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.PlanAdvance),
      claimText = text,
      anchorTerms = List("e5"),
      evidenceLines = List(text),
      sourceKind = sourceKind,
      tacticalOwnership = None
    )

  private def certifiedPositionProbePacket(
      proofSource: String = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
      proofFamily: String = PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
      anchorSquare: String = "c6",
      ownerSeedTerms: List[String] =
        List(
          "c6",
          "fixed_target:c6",
          PlanTaxonomy.PlanKind.BackwardPawnTargeting.id
        ),
      continuationTerms: List[String] =
        List("carlsbad_fixed_target_probe", "fixed_target:c6", "best_branch:b5|Qb6"),
      structureTransitionTerms: List[String] = List("queenside_fixed_chain", "c6_target", "d5_chain"),
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.PositionLocal,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      bestDefenseMove: String = "b5",
      bestDefenseBranchKey: String = "h4f2|b7b5"
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.Pressure
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = "position_probe",
      anchorTerms = List(anchorSquare),
      bestDefenseMove = Some(bestDefenseMove),
      bestDefenseBranchKey = Some(bestDefenseBranchKey),
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = continuationTerms,
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = Some(positionProbeProof(proofSource, anchorSquare))
        ),
      fallbackMode = fallbackMode
    )

  private def positionProbeProof(proofSource: String, anchorSquare: String): PlayerFacingExactSliceProof =
    proofSource match
      case PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource =>
        PlayerFacingExactSliceProof.ColorComplexSqueeze(
          targetSquare = anchorSquare,
          squareColor = "dark",
          minorPieceRole = "knight",
          minorPieceSquare = "c4"
        )
      case PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource =>
        PlayerFacingExactSliceProof.TargetFocusedCoordination(
          targetSquare = anchorSquare,
          supportFromSquares = List("e3", "g2"),
          targetPieces = List("target_knight")
        )
      case PlayerFacingTruthModePolicy.ExactTargetFixationProofSource =>
        PlayerFacingExactSliceProof.ExactTargetFixation(anchorSquare)
      case _ =>
        PlayerFacingExactSliceProof.CarlsbadFixedTarget(anchorSquare, minoritySupport = true)

  private def queenTradeShieldPacket(): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.WeaklyValid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.Exchange
        ),
      proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
      proofFamily = PlanTaxonomy.PlanKind.QueenTradeShield.id,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = PlanTaxonomy.PlanKind.QueenTradeShield.id,
      anchorTerms = List("c6", "d8"),
      bestDefenseMove = Some("d7c6"),
      bestDefenseBranchKey = Some("d4c6|d7c6|d3d8|e8d8"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("queen_trade_shield", "queenless_branch", "c6", "d8"),
          continuationTerms = List("d4c6", "d7c6", "d3d8", "e8d8"),
          structureTransitionTerms = List("queenless_branch", "queen_trade"),
          exactSliceProof =
            Some(
              PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
            )
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

  private def threat(
      kind: String,
      lossIfIgnoredCp: Int,
      bestDefense: Option[String] = None,
      turnsToImpact: Int = 1
  ): ThreatRow =
    ThreatRow(
      kind = kind,
      side = "US",
      square = None,
      lossIfIgnoredCp = lossIfIgnoredCp,
      turnsToImpact = turnsToImpact,
      bestDefense = bestDefense,
      defenseCount = 1,
      insufficientData = false
    )

  private def preventedPlan(
      break: Option[String] = Some("d"),
      counterplayScoreDrop: Int = 140,
      threatType: Option[String] = Some("counterplay")
  ): PreventedPlanInfo =
    PreventedPlanInfo(
      planId = "deny_break",
      deniedSquares = List("d5"),
      breakNeutralized = break,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = threatType,
      sourceScope = FactScope.Now,
      citationLine = Some("14...Rc8 15.Re1 Qc7")
    )

  private def decisionFrame(
      intent: Option[String] = None,
      battlefront: Option[String] = None,
      urgency: Option[String] = None
  ): CertifiedDecisionFrame =
    CertifiedDecisionFrame(
      intent = intent.map(text => CertifiedDecisionSupport(CertifiedDecisionFrameAxis.Intent, text, 90, "intent")),
      battlefront =
        battlefront.map(text => CertifiedDecisionSupport(CertifiedDecisionFrameAxis.Battlefront, text, 86, "battlefront")),
      urgency = urgency.map(text => CertifiedDecisionSupport(CertifiedDecisionFrameAxis.Urgency, text, 72, "urgency")),
      ownerSide = Some("white"),
      alignmentKeys = Set("g7"),
      carrierAlignmentKeys = Set("g7")
    )

  private def inputs(
      mainBundle: Option[MainPathClaimBundle] = None,
      quietIntent: Option[QuietMoveIntentClaim] = None,
      decisionFrame: CertifiedDecisionFrame = CertifiedDecisionFrame(),
      decisionComparison: Option[DecisionComparison] = None,
      alternativeNarrative: Option[AlternativeNarrative] = None,
      preventedPlansNow: List[PreventedPlanInfo] = Nil,
      pvDelta: Option[PVDelta] = None,
      opponentThreats: List[ThreatRow] = Nil,
      evidenceByQuestionId: Map[String, List[QuestionEvidence]] = Map.empty,
      evidenceBackedPlans: List[PlanHypothesis] = Nil,
      opponentPlan: Option[PlanRow] = None,
      openingRelationClaim: Option[String] = None,
      endgameTransitionClaim: Option[String] = None
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = mainBundle,
      quietIntent = quietIntent,
      decisionFrame = decisionFrame,
      decisionComparison = decisionComparison,
      alternativeNarrative = alternativeNarrative,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = preventedPlansNow,
      pvDelta = pvDelta,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = opponentThreats,
      forcingThreats = Nil,
      evidenceByQuestionId = evidenceByQuestionId,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = evidenceBackedPlans,
      opponentPlan = opponentPlan,
      factualFallback = Some("This castles."),
      openingRelationClaim = openingRelationClaim,
      endgameTransitionClaim = endgameTransitionClaim
    )

  private def evidenceBackedPlan(name: String = "Kingside pressure"): PlanHypothesis =
    PlanHypothesis(
      planId = "plan_race",
      planName = name,
      rank = 1,
      score = 0.84,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = 0.84, label = "high", risk = "race"),
      evidenceSources = List("probe"),
      themeL1 = "kingside_attack",
      subplanId = None
    )

  private def localFileEntryPlan: PlanHypothesis =
    PlanHypothesis(
      planId = "local_file_entry_bind",
      planName = "Local file-entry bind",
      rank = 1,
      score = 0.84,
      preconditions = Nil,
      executionSteps = List("Keep the c-file closed and hold b4."),
      failureModes = Nil,
      viability = PlanViability(score = 0.84, label = "high", risk = "route"),
      evidenceSources = List("probe"),
      themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
      subplanId = Some(PlanTaxonomy.PlanKind.OpenFilePressure.id)
    )

  private def localFileEntryPreventedPlan: PreventedPlanInfo =
    PreventedPlanInfo(
      planId = "queenside_counterplay",
      deniedSquares = List("b4"),
      breakNeutralized = Some("...c5"),
      mobilityDelta = 0,
      counterplayScoreDrop = 140,
      preventedThreatType = Some("counterplay"),
      sourceScope = FactScope.Now,
      citationLine = Some("The ...c5 route and b4 entry are no longer available."),
      deniedResourceClass = Some("entry_square"),
      deniedEntryScope = Some("file")
    )

  private def semanticWithPrevented(plan: PreventedPlanInfo): SemanticSection =
    SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = List(plan),
      conceptSummary = List("prophylaxis", "counterplay_cut")
    )

  private def truthContract(
      truthClass: DecisiveTruthClass = DecisiveTruthClass.Best,
      reasonFamily: DecisiveReasonKind = DecisiveReasonKind.OnlyMoveDefense,
      verifiedBestMove: Option[String] = Some("Qe2"),
      benchmarkCriticalMove: Boolean = true
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("e2e4"),
      verifiedBestMove = verifiedBestMove,
      truthClass = truthClass,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = true,
      chosenMatchesBest = verifiedBestMove.contains("e2e4"),
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = true,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = true,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = benchmarkCriticalMove,
      failureMode = FailureInterpretationMode.OnlyMoveFailure,
      failureIntentConfidence = 1.0,
      failureIntentAnchor = verifiedBestMove,
      failureInterpretationAllowed = true
    )

  test("WhyThis admits a move-owned claim with contrast and author evidence") {
    val q = question("q1", AuthorQuestionKind.WhyThis, evidencePurposes = List("reply_multipv"))
    val ctx = baseCtx(List(q), evidence = List(evidence("q1", "reply_multipv", List("14...Rc8 15.Re1 Qc7", "14...Rc8 15.a4 Qc7"))))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This increases the pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(12),
                deferredMove = Some("Qe2"),
                deferredReason = Some("the queen route stays quieter"),
                deferredSource = Some("verified_best"),
                evidence = None,
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            ),
          evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId)
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary plan"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assert(primary.claim.contains("pressure on e5"), clues(primary))
    assert(primary.evidence.exists(_.text.contains("a)")), clues(primary.evidence))
    assert(primary.contrast.exists(_.contains("Qe2")), clues(primary.contrast))
  }

  test("single-line fallback evidence strips existing line labels before adding a branch label") {
    val q = question("q_line_label", AuthorQuestionKind.WhyThis)
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        baseCtx(List(q)),
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This keeps the forcing line under control.")),
                Some(lineClaim("Line: a) dxe5 Qxd8+ Kxd8"))
              )
            )
        ),
        None
      )

    val plan = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(plan.evidence.map(_.text), Some("a) dxe5 Qxd8+ Kxd8"))
  }

  test("single-line fallback evidence does not promote probe reminders as concrete lines") {
    val q = question("q_probe_reminder", AuthorQuestionKind.WhyThis)
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        baseCtx(List(q)),
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This keeps the forcing line under control.")),
                Some(lineClaim("Further probe work still targets Improving piece placement through Na6 and Nc6."))
              )
            )
        ),
        None
      )

    val plan = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(plan.evidence, None)
  }

  test("WhyThis fails closed when only decision comparison exists") {
    val q = question("q1", AuthorQuestionKind.WhyThis)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(12),
                deferredMove = Some("Qe2"),
                deferredReason = Some("the queen route stays quieter"),
                deferredSource = Some("verified_best"),
                evidence = None,
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            )
        ),
        Some(truthContract())
      )

    assertEquals(plans.primary, None)
    assertEquals(plans.rejected.headOption.map(_.questionKind), Some(AuthorQuestionKind.WhyThis))
  }

  test("WhatMattersHere admits only a certified position probe packet") {
    val q = question("q_probe", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "c6 is the fixed target.",
                    packet = certifiedPositionProbePacket()
                  )
                ),
                Some(lineClaim("14...Qb6 15.Rb1"))
              )
            )
        ),
        Some(truthContract(reasonFamily = DecisiveReasonKind.QuietTechnicalMove, benchmarkCriticalMove = false))
      )

    val primary = plans.primary.getOrElse(fail("missing primary position probe"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMattersHere)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.PositionProbe)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource)
    assert(primary.admissibilityReasons.contains("certified_position_probe"), clues(primary))
    assertEquals(
      primary.consequence.map(_.text),
      Some("So the task is to keep the queenside pressure trained on c6 instead of rushing a conversion.")
    )
  }

  test("Carlsbad pressure claim uses bounded minority-attack wording") {
    assertEquals(
      PlayerFacingTruthModePolicy.pressureIncreaseMainClaim(
        packet = certifiedPositionProbePacket(),
        modalityTier = PlayerFacingClaimModalityTier.Supports,
        fallbackAnchor = None
      ),
      Some("c6 is the minority-attack fixed target.")
    )
  }

  test("WhatMattersHere admits certified carlsbad probe in a non-tactical forcing scene") {
    val q = question("q_probe_forcing", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "c6 is the fixed target.",
                    packet = certifiedPositionProbePacket()
                  )
                ),
                Some(lineClaim("14...Qb6 15.Rb1"))
              )
            ),
          opponentThreats = List(threat("Material", 900, Some("b5")))
        ),
        Some(truthContract(reasonFamily = DecisiveReasonKind.QuietTechnicalMove, benchmarkCriticalMove = false))
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.ForcingDefense)
    val primary = plans.primary.getOrElse(fail("missing primary position probe"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMattersHere)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource)
    assert(primary.admissibilityReasons.contains("certified_position_probe"), clues(primary))
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("PositionProbe") &&
          label.contains("source_kind=carlsbad_fixed_target_probe") &&
          label.contains("admission_decision=PrimaryAllowed") &&
          label.contains("certified_position_probe_non_tactical_forcing_scene")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhyThis downgrades queen trade shield move-local owner to claim-only SupportedLocal") {
    val q = question("q_queen_trade", AuthorQuestionKind.WhyThis)
    val ctx = baseCtx(List(q))
    val rawClaim = "This exchange moves the game into the queenless branch."
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  mainClaim(
                    rawClaim,
                    sourceKind = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
                    packet = Some(queenTradeShieldPacket()),
                    deltaClass = PlayerFacingMoveDeltaClass.ExchangeForcing
                  )
                ),
                Some(lineClaim("8.Nxc6 dxc6 9.Qxd8+ Kxd8"))
              )
            )
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing queen trade shield primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.MoveDelta)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.QueenTradeShieldProofSource)
    assertEquals(primary.claim, "This exchange moves the game into the queenless branch.")
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.SupportedLocal)
    assert(primary.admissibilityReasons.contains("strategic_claim_supported_local"), clues(primary))
    assertEquals(primary.evidence, None)
    assertEquals(primary.contrast, None)
    assertEquals(primary.consequence, None)
  }

  test("WhatMattersHere keeps slice-specific wording for target-focused coordination probes") {
    val q = question("q_probe_coordination", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "the pressure is coordinated on c6.",
                    sourceKind = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
                    packet =
                      certifiedPositionProbePacket(
                        proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
                        proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
                        ownerSeedTerms =
                          List(
                            "c6",
                            "coordinated_target:c6",
                            PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
                            "rook_on_c1"
                          ),
                        continuationTerms =
                          List(
                            PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
                            "coordinated_target:c6",
                            "best_branch:d2|Qd7"
                          ),
                        structureTransitionTerms =
                          List("support_from:e3", "support_from:g2", "coordinated_piece_pressure"),
                        bestDefenseMove = "Qd7",
                        bestDefenseBranchKey = "d1b3|d8d7"
                      )
                  )
                ),
                Some(lineClaim("13.Qb3 Qd7 14.Rfd1"))
              )
            )
        ),
        Some(truthContract(reasonFamily = DecisiveReasonKind.QuietTechnicalMove, benchmarkCriticalMove = false))
      )

    val primary = plans.primary.getOrElse(fail("missing target-focused coordination probe"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMattersHere)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.PositionProbe)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource)
    assertEquals(primary.claim, "the pressure is coordinated on c6.")
    assertEquals(primary.prefixKind, PlayerFacingClaimPrefixKind.KeyStrategicFact)
    assertEquals(
      primary.consequence.map(_.text),
      Some("So the task is to keep the pressure coordinated on c6 until the target has to give way.")
    )
  }

  test("WhatMattersHere rejects an uncertified PositionLocal shell even when the claim text looks right") {
    val q = question("q_probe_uncertified", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "c6 is the fixed target.",
                    packet =
                      certifiedPositionProbePacket(
                        sameBranchState = PlayerFacingSameBranchState.Ambiguous,
                        persistence = PlayerFacingClaimPersistence.BestDefenseOnly,
                        fallbackMode = PlayerFacingClaimFallbackMode.ExactFactual
                      )
                  )
                ),
                Some(lineClaim("14...Qb6 15.Rb1"))
              )
            )
        ),
        Some(truthContract())
      )

    assertEquals(plans.primary, None)
    assert(plans.ownerTrace.ownerCandidates.forall(_.plannerOwnerKind != PlannerOwnerKind.PositionProbe), clues(plans.ownerTrace))
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatMattersHere &&
          rejected.reasons.contains("position_probe_not_certified")
      ),
      clues(plans.rejected)
    )
  }

  test("WhatMattersHere rejects position probes when branch and persistence are not certified") {
    val q = question("q_probe_supported", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "c6 is the fixed target.",
                    packet =
                      certifiedPositionProbePacket(
                        sameBranchState = PlayerFacingSameBranchState.Ambiguous,
                        persistence = PlayerFacingClaimPersistence.BestDefenseOnly
                      )
                  )
                ),
                Some(lineClaim("14...Qb6 15.Rb1"))
              )
            )
        ),
        Some(truthContract())
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatMattersHere &&
          rejected.reasons.contains("position_probe_not_certified")
      ),
      clues(plans.rejected)
    )
    assert(
      plans.ownerTrace.ownerCandidateLabels.forall(label => !label.contains("admission_decision=SupportedLocal")),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhatMattersHere admits certified color-complex squeeze position probe") {
    val q = question("q_probe_color_complex", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val packet =
      certifiedPositionProbePacket(
        proofSource = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
        proofFamily = PlayerFacingTruthModePolicy.ColorComplexSqueezeProofFamily,
        anchorSquare = "e5",
        ownerSeedTerms =
          List(
            "e5",
            "weak_square:e5",
            "color_complex:dark",
            "minor_piece:knight_c4",
            "attacks:e5",
            "minor_piece_attack:c4-e5"
          ),
        continuationTerms = List("color_complex_squeeze_probe", "weak_square:e5", "best_branch:c4e5|e8f8"),
        structureTransitionTerms = List("color_complex_squeeze_probe", "weak_square:e5", "minor_piece_attack:c4-e5"),
        bestDefenseMove = "e8f8",
        bestDefenseBranchKey = "c4e5|e8f8"
      )
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "A minor piece keeps the color-complex pressure on e5.",
                    sourceKind = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
                    packet = packet,
                    anchorTerms = List("e5")
                  )
                ),
                Some(lineClaim("1.Nxe5 Kf8"))
              )
            )
        ),
        Some(truthContract(reasonFamily = DecisiveReasonKind.QuietTechnicalMove, benchmarkCriticalMove = false))
      )

    val primary = plans.primary.getOrElse(fail("missing color-complex position probe"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMattersHere)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.PositionProbe)
    assertEquals(primary.plannerSource, PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource)
    assert(primary.admissibilityReasons.contains("certified_position_probe"), clues(primary))
    assertEquals(
      primary.consequence.map(_.text),
      Some("So the task is to keep the minor-piece color-complex pressure centered on e5 stable.")
    )
  }

  test("WhatMattersHere suppresses B/C position probes under a tactical failure veto") {
    val q = question("q_probe_vetoed", AuthorQuestionKind.WhatMattersHere)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(
                  positionLocalClaim(
                    "c6 is the fixed target.",
                    packet =
                      certifiedPositionProbePacket(
                        sameBranchState = PlayerFacingSameBranchState.Ambiguous,
                        persistence = PlayerFacingClaimPersistence.BestDefenseOnly
                      )
                  )
                ),
                Some(lineClaim("14...Qb6 15.Rb1"))
              )
            )
        ),
        Some(truthContract(truthClass = DecisiveTruthClass.Blunder, reasonFamily = DecisiveReasonKind.TacticalRefutation))
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatMattersHere &&
          rejected.reasons.contains("strategic_claim_tactical_veto")
      ),
      clues(plans.rejected)
    )
  }

  test("WhyNow requires a concrete timing reason") {
    val q = question("q_now", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv"))
    val ctx = baseCtx(List(q), evidence = List(evidence("q_now", "reply_multipv", List("14...Rc8 15.Re1 Qc7", "14...Rc8 15.a4 Qc7"))))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This keeps the center under control.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          opponentThreats = List(threat("Mate", 900, Some("Qd8"))),
          evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId)
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyNow)
    assert(primary.claim.toLowerCase.contains("now"), clues(primary.claim))
    assert(primary.contrast.exists(_.contains("Qd8")), clues(primary.contrast))
  }

  test("WhyNow keeps decision-comparison timing out of the primary pool in Step 4a") {
    val q = question("q_now_cmp", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv"))
    val ctx = baseCtx(List(q), evidence = List(evidence("q_now_cmp", "reply_multipv", List("14...Rc8 15.Re1 Qc7"))))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(71),
                deferredMove = Some("Qe2"),
                deferredReason = Some("other moves allow the position to slip away"),
                deferredSource = Some("verified_best"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            ),
          evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId)
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhyNow &&
          rejected.fallbackMode == QuestionPlanFallbackMode.Suppressed &&
          rejected.reasons.contains("decision_timing_support_only")
      ),
      clues(plans.rejected)
    )
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=decision_comparison") &&
          label.contains("timing_source=decision_comparison") &&
          label.contains("decision_comparison_detail=concrete_reply_or_reason") &&
          label.contains("admission_decision=SupportOnly")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhyNow still traces engine-best timing loss even when Step 4a suppresses it") {
    val q = question("q_now_cmp_best", AuthorQuestionKind.WhyNow, evidencePurposes = List("reply_multipv"))
    val ctx = baseCtx(List(q), evidence = List(evidence("q_now_cmp_best", "reply_multipv", List("14...Rc8 15.Re1 Qc7"))))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(MainPathClaimBundle(Some(mainClaim("This keeps pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(74),
                deferredMove = None,
                deferredReason = None,
                deferredSource = Some("top_engine_move"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            ),
          evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId)
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=decision_comparison") &&
          label.contains("timing_source=decision_comparison") &&
          label.contains("decision_comparison_detail=bare_engine_gap") &&
          label.contains("admission_decision=SupportOnly")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhyNow demotes to WhyThis when timing collapses to boilerplate") {
    val q = question("q_now", AuthorQuestionKind.WhyNow)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7"))))),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.fallbackMode, QuestionPlanFallbackMode.DemotedToWhyThis)
    assertEquals(plans.ownerTrace.demotionReasons, List("generic_urgency_only"))
  }

  test("WhyNow records an intentional drop when demotion cannot build a real WhyThis fallback") {
    val q = question("q_now_drop", AuthorQuestionKind.WhyNow)
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        baseCtx(List(q)),
        inputs(),
        None
      )

    assertEquals(plans.primary, None)
    val rejected = plans.rejected.find(_.questionId == q.id).getOrElse(fail("missing rejected plan"))
    assertEquals(rejected.fallbackMode, QuestionPlanFallbackMode.Suppressed)
    assert(rejected.reasons.contains("demotion_intentional_drop"), clues(rejected))
    assert(rejected.reasons.contains("demote_target_unavailable"), clues(rejected))
    assertEquals(rejected.demotedTo, Some(AuthorQuestionKind.WhyThis))
  }

  test("planner owner trace records scene, planner owner kind, and planner source") {
    val q = question("q_now_trace", AuthorQuestionKind.WhyNow)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(opponentThreats = List(threat("Mate", 900, Some("Qd8")))),
        None
    )

    assertEquals(plans.ownerTrace.sceneType, SceneType.ForcingDefense)
    assertEquals(plans.ownerTrace.sceneReasons, List("proof_family=ForcingDefense"))
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("ForcingDefense") && label.contains("source_kind=threat")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
    assertEquals(plans.ownerTrace.selectedQuestion, Some(AuthorQuestionKind.WhyNow))
    assertEquals(plans.ownerTrace.selectedPlannerOwnerKind, Some(PlannerOwnerKind.ForcingDefense))
    assertEquals(plans.ownerTrace.selectedPlannerSource, Some("threat"))
  }

  test("shadow normalization keeps raw close alternatives as DecisionTiming support material") {
    val q = question("q_shadow_alt", AuthorQuestionKind.WhyThis)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(72),
                deferredMove = Some("Qe2"),
                deferredReason = Some("it trails the engine line by about 72cp"),
                deferredSource = Some("close_candidate"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = true,
                chosenMatchesBest = false
              )
            ),
          alternativeNarrative =
            Some(
              AlternativeNarrative(
                move = Some("Qe2"),
                reason = "the queen route stays quieter",
                sentence = "The practical alternative Qe2 stays secondary because the queen route stays quieter.",
                source = "close_candidate"
              )
            )
        ),
        None
      )

    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=decision_comparison") &&
          label.contains("decision_comparison_detail=bare_engine_gap")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=close_candidate") &&
          label.contains("materiality=support_material") &&
          label.contains("move_linked=false") &&
          label.contains("support_material=true") &&
          label.contains("timing_source=close_candidate") &&
          label.contains("mapping=DecisionTiming/support_only") &&
          label.contains("admission_decision=SupportOnly")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("shadow normalization does not mark enriched close alternatives as raw blockers") {
    val q = question("q_shadow_enriched_alt", AuthorQuestionKind.WhyThis)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("e4"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = None,
                deferredMove = Some("Qe2"),
                deferredReason = Some("different strategic branches"),
                deferredSource = Some("close_candidate"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = true,
                chosenMatchesBest = true
              )
            ),
          alternativeNarrative =
            Some(
              AlternativeNarrative(
                move = Some("Qe2"),
                reason = "different strategic branches",
                sentence =
                  "Both candidate branches are viable: the played e4 follows 1. e4 e5, whereas Qe2 opts for 1. Qe2 e5.",
                source = "close_candidate"
              )
            )
        ),
        None
      )

    val closeCandidateLabels =
      plans.ownerTrace.ownerCandidateLabels.filter(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=close_candidate") &&
          label.contains("timing_source=close_candidate")
      )

    assert(closeCandidateLabels.nonEmpty, clues(plans.ownerTrace.ownerCandidateLabels))
    assert(closeCandidateLabels.exists(_.contains("enriched_close_candidate")), clues(closeCandidateLabels))
    assert(!closeCandidateLabels.exists(_.contains("raw_close_alternative")), clues(closeCandidateLabels))
  }

  test("shadow normalization distinguishes raw opening/endgame signals from move-linked translators") {
    val q = question("q_shadow_domain", AuthorQuestionKind.WhyThis)
    val ctx =
      baseCtx(List(q)).copy(
        openingEvent = Some(OpeningEvent.Novelty("e4", 18, "novelty", 24)),
        openingData =
          Some(
            OpeningReference(
              eco = Some("C20"),
              name = Some("King's Pawn Game"),
              totalGames = 120,
              topMoves = List(ExplorerMove("e2e4", "e4", 60, 24, 18, 18, 52)),
              sampleGames =
                List(
                  ExplorerGame(
                    id = "g1",
                    winner = None,
                    white = ExplorerPlayer("Capablanca", 2700),
                    black = ExplorerPlayer("Lasker", 2700),
                    year = 1924,
                    month = 1,
                    event = Some("Test"),
                    pgn = Some("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6")
                  )
                )
            )
          ),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures =
                Some(
                  EndgameInfo(
                    hasOpposition = false,
                    isZugzwang = false,
                    keySquaresControlled = Nil,
                    theoreticalOutcomeHint = "Draw",
                    confidence = 0.88,
                    primaryPattern = Some("PhilidorDefense"),
                    patternAge = 3,
                    transition = Some("Lucena(Win) → PhilidorDefense(Draw)")
                  )
                ),
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7"))))),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.TransitionConversion)
    val labels = plans.ownerTrace.ownerCandidateLabels
    assert(labels.exists(label =>
      label.contains("OpeningRelation") &&
        label.contains("source_kind=opening_precedent_summary") &&
        label.contains("materiality=support_material") &&
        label.contains("move_linked=false") &&
        label.contains("support_material=true") &&
        label.contains("admission_decision=SupportOnly")
    ), clues(labels))
    assert(labels.exists(label =>
      label.contains("OpeningRelation") &&
        label.contains("source_kind=opening_relation_translator") &&
        label.contains("materiality=owner_candidate") &&
        label.contains("move_linked=true") &&
        label.contains("support_material=false") &&
        label.contains("admission_decision=SupportOnly")
    ), clues(labels))
    assert(labels.exists(label =>
      label.contains("EndgameTransition") &&
        label.contains("source_kind=endgame_theoretical_hint") &&
        label.contains("materiality=support_material") &&
        label.contains("move_linked=false") &&
        label.contains("support_material=true") &&
        label.contains("admission_decision=SupportOnly")
    ), clues(labels))
    assert(labels.exists(label =>
      label.contains("EndgameTransition") &&
        label.contains("source_kind=endgame_transition_translator") &&
        label.contains("materiality=owner_candidate") &&
        label.contains("move_linked=true") &&
        label.contains("support_material=false") &&
        label.contains("admission_decision=SupportOnly")
    ), clues(labels))
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhyThis))
  }

  test("scene trace keeps pure opening translators under opening relation") {
    val q = question("q_shadow_opening_only", AuthorQuestionKind.WhyThis)
    val ctx =
      baseCtx(List(q)).copy(
        openingEvent = Some(OpeningEvent.Novelty("e4", 18, "novelty", 24)),
        openingData =
          Some(
            OpeningReference(
              eco = Some("C20"),
              name = Some("King's Pawn Game"),
              totalGames = 120,
              topMoves = List(ExplorerMove("e2e4", "e4", 60, 24, 18, 18, 52)),
              sampleGames =
                List(
                  ExplorerGame(
                    id = "g_opening_only",
                    winner = None,
                    white = ExplorerPlayer("Capablanca", 2700),
                    black = ExplorerPlayer("Lasker", 2700),
                    year = 1924,
                    month = 1,
                    event = Some("Test"),
                    pgn = Some("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6")
                  )
                )
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This improves pressure on e5.", sourceKind = "main_delta")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            )
        ),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.OpeningRelation)
    assert(plans.ownerTrace.ownerCandidateLabels.exists(_.contains("source_kind=opening_relation_translator")))
    assert(!plans.ownerTrace.ownerCandidateLabels.exists(_.contains("EndgameTransition")))
    assert(
      plans.ownerTrace.admittedPlannerOwnerLabels.exists(label =>
        label.contains("OpeningRelation") &&
          label.contains("source_kind=opening_relation_translator") &&
          label.contains("admission_decision=PrimaryAllowed")
      ),
      clues(plans.ownerTrace.admittedPlannerOwnerLabels)
    )
    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.OpeningRelation)
    assertEquals(primary.plannerSource, "opening_relation_translator")
  }

  test("self-only opening samples do not create late opening relation owners") {
    val q = question("q_late_self_only_opening", AuthorQuestionKind.WhyThis)
    val ctx =
      baseCtx(List(q)).copy(
        ply = 35,
        phase = PhaseContext("Endgame", "Technical conversion"),
        openingEvent = None,
        openingData =
          Some(
            OpeningReference(
              eco = Some("B07"),
              name = Some("King's Pawn Game: Maróczy Defense"),
              totalGames = 1,
              topMoves = Nil,
              sampleGames =
                List(
                  ExplorerGame(
                    id = "current_game_only",
                    winner = None,
                    white = ExplorerPlayer("White", 1800),
                    black = ExplorerPlayer("Black", 1800),
                    year = 2026,
                    month = 3,
                    event = Some("Current game"),
                    pgn = Some("1. e4 d6 2. d4 Nf6 3. Nc3 e5 4. Nf3")
                  )
                )
            )
          )
      )

    assertEquals(QuestionFirstCommentaryPlanner.openingRelationReplayClaim(ctx), None)
    assertEquals(
      QuestionFirstCommentaryPlanner.openingRelationReplayClaim(
        ctx.copy(
          ply = 14,
          phase = PhaseContext("Opening", "Opening branch point"),
          openingEvent = Some(OpeningEvent.Novelty("Ke7", 14, "development logic", 1))
        )
      ),
      None
    )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This improves pressure on e5.", sourceKind = "main_delta")),
                Some(lineClaim("18. c3 Na5 19.Ne3"))
              )
            )
        ),
        None
      )

    assert(plans.ownerTrace.sceneType != SceneType.OpeningRelation, clues(plans.ownerTrace))
    assert(
      !plans.ownerTrace.ownerCandidateLabels.exists(_.contains("source_kind=opening_relation_translator")),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("scene trace keeps pure endgame translators under endgame transition") {
    val q = question("q_shadow_endgame_only", AuthorQuestionKind.WhyThis)
    val ctx =
      baseCtx(List(q)).copy(
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures =
                Some(
                  EndgameInfo(
                    hasOpposition = false,
                    isZugzwang = false,
                    keySquaresControlled = Nil,
                    theoreticalOutcomeHint = "Draw",
                    confidence = 0.88,
                    primaryPattern = Some("PhilidorDefense"),
                    patternAge = 3,
                    transition = Some("Lucena(Win) → PhilidorDefense(Draw)")
                  )
                ),
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This improves pressure on e5.", sourceKind = "main_delta")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            )
        ),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.EndgameTransition)
    assert(plans.ownerTrace.ownerCandidateLabels.exists(_.contains("source_kind=endgame_transition_translator")))
    assert(!plans.ownerTrace.ownerCandidateLabels.exists(_.contains("OpeningRelation")))
    assert(
      plans.ownerTrace.admittedPlannerOwnerLabels.exists(label =>
        label.contains("EndgameTransition") &&
          label.contains("source_kind=endgame_transition_translator") &&
          label.contains("admission_decision=PrimaryAllowed")
      ),
      clues(plans.ownerTrace.admittedPlannerOwnerLabels)
    )
    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.EndgameTransition)
    assertEquals(primary.plannerSource, "endgame_transition_translator")
  }

  test("opening relation scene keeps WhyThis ahead of WhatChanged inside the legal pool") {
    val whyThis = question("q_open_why", AuthorQuestionKind.WhyThis, priority = 40)
    val whatChanged = question("q_open_change", AuthorQuestionKind.WhatChanged, priority = 90)
    val ctx =
      baseCtx(List(whyThis, whatChanged)).copy(
        openingEvent = Some(OpeningEvent.Novelty("e4", 18, "novelty", 24)),
        openingData =
          Some(
            OpeningReference(
              eco = Some("C20"),
              name = Some("King's Pawn Game"),
              totalGames = 120,
              topMoves = List(ExplorerMove("e2e4", "e4", 60, 24, 18, 18, 52)),
              sampleGames =
                List(
                  ExplorerGame(
                    id = "g_opening_rank",
                    winner = None,
                    white = ExplorerPlayer("Capablanca", 2700),
                    black = ExplorerPlayer("Lasker", 2700),
                    year = 1924,
                    month = 1,
                    event = Some("Test"),
                    pgn = Some("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6")
                  )
                )
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This improves pressure on e5.", sourceKind = "main_delta")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            )
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(plans.ownerTrace.sceneType, SceneType.OpeningRelation)
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.OpeningRelation)
  }

  test("endgame transition scene keeps WhatChanged ahead of WhyThis inside the legal pool") {
    val whyThis = question("q_end_why", AuthorQuestionKind.WhyThis, priority = 90)
    val whatChanged = question("q_end_change", AuthorQuestionKind.WhatChanged, priority = 30)
    val ctx =
      baseCtx(List(whyThis, whatChanged)).copy(
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures =
                Some(
                  EndgameInfo(
                    hasOpposition = false,
                    isZugzwang = false,
                    keySquaresControlled = Nil,
                    theoreticalOutcomeHint = "Draw",
                    confidence = 0.88,
                    primaryPattern = Some("PhilidorDefense"),
                    patternAge = 3,
                    transition = Some("Lucena(Win) → PhilidorDefense(Draw)")
                  )
                ),
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This improves pressure on e5.", sourceKind = "main_delta")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            )
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(plans.ownerTrace.sceneType, SceneType.EndgameTransition)
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatChanged)
    assertEquals(primary.plannerOwnerKind, PlannerOwnerKind.EndgameTransition)
  }

  test("scene trace prefers plan clash over forcing defense when both are present") {
    val q = question("q_plan_clash_shadow", AuthorQuestionKind.WhosePlanIsFaster)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx = baseCtx(List(q), opponentPlan = Some(opponent))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionFrame =
            decisionFrame(
              intent = Some("White is playing for pressure on g7."),
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The reply window is short now.")
            ),
          preventedPlansNow = List(preventedPlan()),
          opponentThreats = List(threat("Mate", 320, Some("Qd8"))),
          opponentPlan = Some(opponent)
        ),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.PlanClash)
    assert(plans.ownerTrace.ownerCandidateLabels.exists(_.contains("PlanRace")), clues(plans.ownerTrace.ownerCandidateLabels))
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("ForcingDefense") && label.contains("source_kind=threat")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhosePlanIsFaster))
  }

  test("shadow timing traces split prevented resources and only-move windows from decision comparison") {
    val q = question("q_timing_shadow_split", AuthorQuestionKind.WhyNow)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          preventedPlansNow = List(preventedPlan(counterplayScoreDrop = 140))
        ),
        Some(truthContract())
      )

    val labels = plans.ownerTrace.ownerCandidateLabels
    assert(labels.exists(label =>
      label.contains("DecisionTiming") &&
        label.contains("source_kind=prevented_plan") &&
        label.contains("materiality=support_material") &&
        label.contains("timing_source=prevented_resource")
    ), clues(labels))
    assert(labels.exists(label =>
      label.contains("DecisionTiming") &&
        label.contains("source_kind=truth_contract") &&
        label.contains("materiality=owner_candidate") &&
        label.contains("timing_source=only_move")
    ), clues(labels))
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhyNow))
  }

  test("PlanRace is demoted out of the owner pool when tactical failure owns the scene") {
    val q = question("q_race_tactical", AuthorQuestionKind.WhosePlanIsFaster)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx = baseCtx(List(q), opponentPlan = Some(opponent))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This drops a tactical resource.", mode = PlayerFacingTruthMode.Tactical, sourceKind = "main_delta")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            ),
          decisionFrame =
            decisionFrame(
              intent = Some("White is playing for pressure on g7."),
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The timing matters now.")
            ),
          opponentPlan = Some(opponent)
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.Blunder,
            reasonFamily = DecisiveReasonKind.TacticalRefutation,
            verifiedBestMove = Some("Qe2")
          )
        )
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.TacticalFailure)
    assertEquals(plans.primary, None)
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("PlanRace") &&
          label.contains("admission_decision=Demote") &&
          label.contains("demoted_to=WhyThis")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("scene trace keeps tactical failure ahead of opening and endgame translators") {
    val q = question("q_tactical_over_domain", AuthorQuestionKind.WhyThis)
    val ctx =
      baseCtx(List(q)).copy(
        openingEvent = Some(OpeningEvent.Novelty("e4", 18, "novelty", 24)),
        openingData =
          Some(
            OpeningReference(
              eco = Some("C20"),
              name = Some("King's Pawn Game"),
              totalGames = 120,
              topMoves = List(ExplorerMove("e2e4", "e4", 60, 24, 18, 18, 52)),
              sampleGames = Nil
            )
          ),
        semantic =
          Some(
            SemanticSection(
              structuralWeaknesses = Nil,
              pieceActivity = Nil,
              positionalFeatures = Nil,
              compensation = None,
              endgameFeatures =
                Some(
                  EndgameInfo(
                    hasOpposition = false,
                    isZugzwang = false,
                    keySquaresControlled = Nil,
                    theoreticalOutcomeHint = "Draw",
                    confidence = 0.88,
                    primaryPattern = Some("PhilidorDefense"),
                    patternAge = 3,
                    transition = Some("Lucena(Win) → PhilidorDefense(Draw)")
                  )
                ),
              practicalAssessment = None,
              preventedPlans = Nil,
              conceptSummary = Nil
            )
          )
      )

    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle =
            Some(
              MainPathClaimBundle(
                Some(mainClaim("This wins material tactically.", PlayerFacingTruthMode.Tactical, "main_tactical")),
                Some(lineClaim("14...Rc8 15.Re1 Qc7"))
              )
            )
        ),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.TacticalFailure)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhyThis))
  }

  test("failure-mode tactical refutation owns the scene without tactical reason-family text") {
    val q = question("q_failure_mode_tactical", AuthorQuestionKind.WhyThis)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          quietIntent =
            Some(
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.PieceImprovement,
                claimText = "This keeps the rook active on the file.",
                evidenceLine = Some("29...Rc8 30.Re1 Qc7"),
                sourceKind = "quiet_intent"
              )
            )
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.Mistake,
            reasonFamily = DecisiveReasonKind.Conversion,
            verifiedBestMove = Some("Qe2")
          ).copy(
            failureMode = FailureInterpretationMode.TacticalRefutation
          )
        )
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.TacticalFailure)
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(_.contains("TacticalFailure")),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("best tactical refutation does not create tactical failure ownership without a bad move") {
    val q = question("q_changed_best_hold", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          quietIntent =
            Some(
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.PieceImprovement,
                claimText = "This keeps the rook active on the file.",
                evidenceLine = Some("29...Rc8 30.Re1 Qc7"),
                sourceKind = "quiet_intent"
              )
            ),
          pvDelta =
            Some(
              PVDelta(
                resolvedThreats = List("back-rank mate"),
                newOpportunities = List("c-file pressure"),
                planAdvancements = Nil,
                concessions = Nil
              )
            )
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.Best,
            reasonFamily = DecisiveReasonKind.TacticalRefutation,
            verifiedBestMove = Some("Rc8"),
            benchmarkCriticalMove = false
          )
        )
      )

    assertNotEquals(plans.ownerTrace.sceneType, SceneType.TacticalFailure)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
    assertEquals(plans.ownerTrace.selectedPlannerOwnerKind, Some(PlannerOwnerKind.MoveDelta))
  }

  test("best tactical refutation hold without concrete move delta keeps truth-contract forcing defense") {
    val q = question("q_now_best_hold", AuthorQuestionKind.WhyNow)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          pvDelta = Some(PVDelta(Nil, Nil, Nil, Nil))
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.Best,
            reasonFamily = DecisiveReasonKind.TacticalRefutation,
            verifiedBestMove = Some("e2e4"),
            benchmarkCriticalMove = false
          ).copy(
            failureMode = FailureInterpretationMode.NoClearPlan
          )
        )
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.ForcingDefense)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhyNow))
    assertEquals(plans.ownerTrace.selectedPlannerOwnerKind, Some(PlannerOwnerKind.ForcingDefense))
    assertEquals(plans.ownerTrace.selectedPlannerSource, Some("truth_contract"))
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("ForcingDefense") &&
          label.contains("source_kind=truth_contract") &&
          label.contains("admission_decision=PrimaryAllowed")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhatChanged requires move-attributed change rather than state summary") {
    val q = question("q_changed", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          quietIntent =
            Some(
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.PieceImprovement,
                claimText = "This improves the bishop on g2.",
                evidenceLine = Some("14...Rc8 15.Re1 Qc7"),
                sourceKind = "quiet_intent"
              )
            ),
          pvDelta =
            Some(
              PVDelta(
                resolvedThreats = List("back-rank mate"),
                newOpportunities = List("e5 pressure"),
                planAdvancements = List("rook lift"),
                concessions = Nil
              )
            )
        ),
        None
      )

    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
    assert(plans.primary.flatMap(_.contrast).exists(_.contains("Before the move")), clues(plans.primary))
  }

  test("quiet move-delta ingress keeps threat pressure from promoting forcing defense") {
    val q = question("q_changed_threat", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          quietIntent =
            Some(
              QuietMoveIntentClaim(
                intentClass = QuietMoveIntentClass.PieceImprovement,
                claimText = "This improves the rook on c8.",
                evidenceLine = Some("29...Rc8 30.Re1 Qc7"),
                sourceKind = "quiet_intent"
              )
            ),
          pvDelta =
            Some(
              PVDelta(
                resolvedThreats = List("back-rank mate"),
                newOpportunities = List("c-file pressure"),
                planAdvancements = List("rook lift"),
                concessions = Nil
              )
            ),
          opponentThreats = List(threat("Material", 160, Some("Qd8")))
        ),
        None
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.TransitionConversion)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
    assertEquals(plans.ownerTrace.selectedPlannerOwnerKind, Some(PlannerOwnerKind.MoveDelta))
    assertEquals(plans.ownerTrace.selectedPlannerSource, Some("pv_delta"))
    assert(!plans.ownerTrace.ownerCandidateLabels.exists(_.contains("source_kind=threat")), clues(plans.ownerTrace.ownerCandidateLabels))
  }

  test("empty pv delta keeps concrete threat pressure as forcing defense") {
    val q = question("q_stop_blocked_threat", AuthorQuestionKind.WhatMustBeStopped)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          pvDelta = Some(PVDelta(Nil, Nil, Nil, Nil)),
          opponentThreats = List(threat("Material", 320, Some("Qd8")))
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.CompensatedInvestment,
            reasonFamily = DecisiveReasonKind.InvestmentSacrifice,
            verifiedBestMove = Some("Qe2"),
            benchmarkCriticalMove = false
          ).copy(
            failureMode = FailureInterpretationMode.NoClearPlan
          )
        )
      )

    assertEquals(plans.ownerTrace.sceneType, SceneType.ForcingDefense)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatMustBeStopped))
    assertEquals(plans.ownerTrace.selectedPlannerOwnerKind, Some(PlannerOwnerKind.ForcingDefense))
    assertEquals(plans.ownerTrace.selectedPlannerSource, Some("threat"))
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("ForcingDefense") &&
          label.contains("source_kind=threat") &&
          label.contains("admission_decision=PrimaryAllowed")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("MoveDelta") &&
          label.contains("source_kind=pv_delta") &&
          label.contains("admission_decision=SupportOnly")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("only-move investment with empty pv delta keeps move delta primary") {
    val q = question("q_changed_only_move_quiet", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          pvDelta = Some(PVDelta(Nil, Nil, Nil, Nil)),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("a5"),
                engineBestMove = Some("g3"),
                engineBestScoreCp = Some(127),
                engineBestPv = Nil,
                cpLossVsChosen = Some(127),
                deferredMove = Some("g3"),
                deferredReason = Some("the cleaner version of the position runs through g3"),
                deferredSource = Some("verified_best"),
                evidence = Some("34.a5 g3"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            ),
          opponentThreats = List(threat("Material", 160, Some("g3")))
        ),
        Some(
          truthContract(
            truthClass = DecisiveTruthClass.Mistake,
            reasonFamily = DecisiveReasonKind.InvestmentSacrifice,
            verifiedBestMove = Some("g3"),
            benchmarkCriticalMove = true
          )
        )
      )

    assertNotEquals(plans.ownerTrace.sceneType, SceneType.ForcingDefense)
    assertEquals(plans.primary, None)
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("MoveDelta") &&
          label.contains("source_kind=pv_delta") &&
          label.contains("admission_decision=PrimaryAllowed")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
    assert(
      !plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("ForcingDefense") && label.contains("source_kind=threat")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhatChanged fails closed on state-only summary") {
    val q = question("q_changed", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans = QuestionFirstCommentaryPlanner.plan(ctx, inputs(), None)

    assertEquals(plans.primary, None)
    assertEquals(plans.rejected.headOption.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
  }

  test("WhatChanged does not owner-promote an uncertified prevented counterplay window") {
    val q = question("q_changed_counterplay", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          preventedPlansNow =
            List(
              PreventedPlanInfo(
                planId = "counterplay_window",
                deniedSquares = Nil,
                breakNeutralized = None,
                mobilityDelta = 0,
                counterplayScoreDrop = 95,
                preventedThreatType = None,
                sourceScope = FactScope.Now,
                citationLine = Some("14...Rc8 15.Re1 Qc7")
              )
            )
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatChanged &&
          rejected.reasons.contains("state_truth_only")
      ),
      clues(plans.rejected)
    )
  }

  test("WhatChanged can project a local file-entry pair with a FEN-backed board witness") {
    val q = question("q_changed_local_file", AuthorQuestionKind.WhatChanged)
    val preventedPlan = localFileEntryPreventedPlan
    val moveBundle =
      MainPathClaimBundle(
        Some(mainClaim("This improves the rook before the opponent's counterplay starts.")),
        Some(lineClaim("23.a3 Rc8"))
      )
    val ctx =
      MoveReviewProseGoldenFixtures.prophylacticCut.ctx.copy(
        authorQuestions = List(q),
        semantic = Some(semanticWithPrevented(preventedPlan))
      )
    val certifiedPair =
      LocalFileEntryProof.certifiedSurfacePair(
        ctx = ctx,
        preventedPlans = List(preventedPlan),
        evidenceBackedPlans = List(localFileEntryPlan)
      )
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(moveBundle),
          preventedPlansNow = List(preventedPlan),
          evidenceBackedPlans = List(localFileEntryPlan)
        ),
        None
      )

    assertEquals(certifiedPair, Some(LocalFileEntryProof.SurfacePair("c-file", "b4", 140)))
    assertEquals(
      plans.primary.map(_.questionKind),
      Some(AuthorQuestionKind.WhatChanged),
      clues(plans.rejected, plans.ownerTrace.ownerCandidateLabels)
    )
    assert(
      plans.primary.exists(plan => plan.claim.contains("c-file") && plan.claim.contains("b4")),
      clues(plans.primary)
    )
  }

  test("WhatChanged does not use a FEN-less local file-entry pair as public authority") {
    val q = question("q_changed_local_file_bad_fen", AuthorQuestionKind.WhatChanged)
    val preventedPlan = localFileEntryPreventedPlan
    val moveBundle =
      MainPathClaimBundle(
        Some(mainClaim("This improves the rook before the opponent's counterplay starts.")),
        Some(lineClaim("23.a3 Rc8"))
      )
    val ctx =
      baseCtx(List(q)).copy(
        playedMove = Some("e1e2"),
        playedSan = Some("Ke2"),
        semantic = Some(semanticWithPrevented(preventedPlan))
      )
    val certifiedPair =
      LocalFileEntryProof.certifiedSurfacePair(
        ctx = ctx,
        preventedPlans = List(preventedPlan),
        evidenceBackedPlans = List(localFileEntryPlan)
      )
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(moveBundle),
          preventedPlansNow = List(preventedPlan),
          evidenceBackedPlans = List(localFileEntryPlan)
        ),
        None
      )

    assertEquals(certifiedPair, None)
    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
    assert(plans.primary.exists(_.claim.contains("improves the rook")), clues(plans.primary))
    assert(!plans.primary.exists(plan => plan.claim.contains("c-file") || plan.claim.contains("b4")), clues(plans.primary))
  }

  test("WhatChanged keeps decision-comparison timing change out of the primary pool in Step 4a") {
    val q = question("q_changed_cmp", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("e4"),
                engineBestMove = Some("Qe2"),
                engineBestScoreCp = Some(18),
                engineBestPv = Nil,
                cpLossVsChosen = Some(95),
                deferredMove = Some("Qe2"),
                deferredReason = Some("other moves allow the position to slip away"),
                deferredSource = Some("verified_best"),
                evidence = Some("14...Rc8 15.Re1 Qc7"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            )
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatChanged &&
          rejected.fallbackMode == QuestionPlanFallbackMode.FactualFallback &&
          rejected.reasons.contains("state_truth_only")
      ),
      clues(plans.rejected)
    )
    assert(
      plans.ownerTrace.ownerCandidateLabels.exists(label =>
        label.contains("DecisionTiming") &&
          label.contains("source_kind=decision_comparison") &&
          label.contains("decision_comparison_detail=concrete_reply_or_reason") &&
          label.contains("admission_decision=SupportOnly")
      ),
      clues(plans.ownerTrace.ownerCandidateLabels)
    )
  }

  test("WhatChanged does not owner-promote decision comparison when pvDelta is empty") {
    val q = question("q_changed_empty_delta", AuthorQuestionKind.WhatChanged)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          pvDelta = Some(PVDelta(Nil, Nil, Nil, Nil)),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("a5"),
                engineBestMove = Some("g3"),
                engineBestScoreCp = Some(127),
                engineBestPv = Nil,
                cpLossVsChosen = Some(127),
                deferredMove = Some("g3"),
                deferredReason = Some("the cleaner version of the position runs through g3"),
                deferredSource = Some("verified_best"),
                evidence = Some("34.a5 g3"),
                practicalAlternative = false,
                chosenMatchesBest = false
              )
            )
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhatChanged &&
          rejected.fallbackMode == QuestionPlanFallbackMode.FactualFallback &&
          rejected.reasons.contains("state_truth_only")
      ),
      clues(plans.rejected)
    )
  }

  test("WhyNow does not owner-promote an uncertified prevented counterplay window") {
    val q = question("q_now_counterplay", AuthorQuestionKind.WhyNow)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          preventedPlansNow =
            List(
              PreventedPlanInfo(
                planId = "counterplay_window",
                deniedSquares = Nil,
                breakNeutralized = None,
                mobilityDelta = 0,
                counterplayScoreDrop = 88,
                preventedThreatType = None,
                sourceScope = FactScope.Now,
                citationLine = Some("14...Rc8 15.Re1 Qc7")
              )
            )
        ),
        None
      )

    assertEquals(plans.primary, None)
    assert(
      plans.rejected.exists(rejected =>
        rejected.questionKind == AuthorQuestionKind.WhyNow &&
          rejected.reasons.contains("generic_urgency_only")
      ),
      clues(plans.rejected)
    )
  }

  test("WhatMustBeStopped admits urgent defensive ownership") {
    val q = question("q_stop", AuthorQuestionKind.WhatMustBeStopped)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(opponentThreats = List(threat("Material", 320, Some("Qd8")))),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMustBeStopped)
    assert(primary.claim.toLowerCase.contains("stop"), clues(primary.claim))
  }

  test("WhatMustBeStopped does not owner-promote a generic opponent plan") {
    val q = question("q_stop", AuthorQuestionKind.WhatMustBeStopped)
    val ctx =
      baseCtx(
        List(q),
        opponentPlan = Some(PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file")))
      )
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This improves pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          opponentPlan = ctx.opponentPlan
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(primary.fallbackMode, QuestionPlanFallbackMode.DemotedToWhyThis)
  }

  test("WhosePlanIsFaster requires certified intent battlefront and timing anchor") {
    val q = question("q_race", AuthorQuestionKind.WhosePlanIsFaster)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx = baseCtx(List(q), opponentPlan = Some(opponent))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionFrame =
            decisionFrame(
              intent = Some("White is playing for pressure on g7."),
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The timing matters now.")
            ),
          preventedPlansNow = List(preventedPlan()),
          opponentPlan = Some(opponent)
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhosePlanIsFaster)
    assert(primary.claim.contains("Queenside counterplay"), clues(primary.claim))
  }

  test("WhosePlanIsFaster can use a concrete opponent threat as the race participant") {
    val q = question("q_race_threat", AuthorQuestionKind.WhosePlanIsFaster)
    val ctx = baseCtx(List(q))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionFrame =
            decisionFrame(
              intent = Some("White is playing for pressure on g7."),
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The reply window is short now.")
            ),
          opponentThreats = List(threat("Mate", 320, Some("Qd8"))),
          preventedPlansNow = List(preventedPlan())
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhosePlanIsFaster)
    assert(primary.claim.toLowerCase.contains("threat"), clues(primary.claim))
  }

  test("WhosePlanIsFaster can use a probe-backed plan with urgency against an opponent plan") {
    val q = question("q_race_plan", AuthorQuestionKind.WhosePlanIsFaster)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx = baseCtx(List(q), opponentPlan = Some(opponent))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          decisionFrame =
            decisionFrame(
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The timing matters now.")
            ),
          evidenceBackedPlans = List(evidenceBackedPlan()),
          opponentPlan = Some(opponent)
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhosePlanIsFaster)
    assert(primary.claim.contains("Queenside counterplay"), clues(primary.claim))
    assert(primary.admissibilityReasons.contains("probe_backed_plan_intent"), clues(primary))
  }

  test("WhosePlanIsFaster demotes to WhatMustBeStopped when only opponent pressure survives") {
    val q = question("q_race", AuthorQuestionKind.WhosePlanIsFaster)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx = baseCtx(List(q), opponentPlan = Some(opponent))
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          preventedPlansNow = List(preventedPlan()),
          opponentThreats = List(threat("Material", 280, Some("Qd8"))),
          opponentPlan = Some(opponent)
        ),
        None
      )

    val primary = plans.primary.getOrElse(fail("missing primary"))
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatMustBeStopped)
    assertEquals(primary.fallbackMode, QuestionPlanFallbackMode.DemotedToWhatMustBeStopped)
  }

  test("planner ranks primary and secondary without duplicating framing") {
    val whyNow = question("q_now", AuthorQuestionKind.WhyNow, priority = 95, evidencePurposes = List("reply_multipv"))
    val race = question("q_race", AuthorQuestionKind.WhosePlanIsFaster, priority = 90)
    val opponent =
      PlanRow(1, "Queenside counterplay", 0.72, List("pressure on the c-file"))
    val ctx =
      baseCtx(
        List(whyNow, race),
        evidence = List(evidence("q_now", "reply_multipv", List("14...Rc8 15.Re1 Qc7", "14...Rc8 15.a4 Qc7"))),
        opponentPlan = Some(opponent)
      )
    val plans =
      QuestionFirstCommentaryPlanner.plan(
        ctx,
        inputs(
          mainBundle = Some(MainPathClaimBundle(Some(mainClaim("This keeps pressure on e5.")), Some(lineClaim("14...Rc8 15.Re1 Qc7")))),
          decisionFrame =
            decisionFrame(
              intent = Some("White is playing for pressure on g7."),
              battlefront = Some("The battlefront stays on the kingside."),
              urgency = Some("The timing matters now.")
            ),
          preventedPlansNow = List(preventedPlan()),
          opponentThreats = List(threat("Material", 280, Some("Qd8"))),
          evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId),
          opponentPlan = Some(opponent)
        ),
        None
      )

    assertEquals(plans.primary.map(_.questionKind), Some(AuthorQuestionKind.WhosePlanIsFaster))
    assertEquals(plans.secondary, None)
  }

  test("outline uses planner claim text and exact factual fallback instead of raw question text") {
    val q = question("q_now", AuthorQuestionKind.WhyNow)
    val ctx =
      baseCtx(List(q)).copy(
        playedMove = Some("e1g1"),
        playedSan = Some("O-O")
      )
    val rec = new TraceRecorder()
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec)
    val decision = outline.getBeat(OutlineBeatKind.DecisionPoint).getOrElse(fail("missing decision beat"))

    assertEquals(decision.text, "This castles.")
    assert(!decision.text.contains("placeholder-q_now"), clues(decision.text))
  }

  test("outline factual fallback keeps ambiguous captures literal instead of adding simplification meaning") {
    val q = question("q_capture", AuthorQuestionKind.WhyNow)
    val ctx =
      baseCtx(List(q)).copy(
        playedMove = Some("c4f7"),
        playedSan = Some("Bx")
      )
    val rec = new TraceRecorder()
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec)
    val decision = outline.getBeat(OutlineBeatKind.DecisionPoint).getOrElse(fail("missing decision beat"))

    assertEquals(decision.text, "This captures.")
    assert(!decision.text.toLowerCase.contains("simplifying"), clues(decision.text))
  }

  test("outline falls straight to exact factual fallback when no question survives to own the decision beat") {
    val ctx =
      baseCtx(Nil).copy(
        playedMove = Some("e1g1"),
        playedSan = Some("O-O"),
        decision =
          Some(
            DecisionRationale(
              focalPoint = None,
              logicSummary = "Castle before starting the kingside pressure.",
              delta = PVDelta(Nil, Nil, Nil, Nil),
              confidence = ConfidenceLevel.Engine
            )
          ),
        meta =
          Some(
            MetaSignals(
              choiceType = ChoiceType.NarrowChoice,
              targets = Targets(Nil, Nil),
              planConcurrency = PlanConcurrency("kingside pressure", None, "independent"),
              whyNot = Some("The rook lift is too slow.")
            )
          ),
        candidates = List(
          CandidateInfo("O-O", annotation = "!", planAlignment = "King safety", tacticalAlert = None, practicalDifficulty = "clean", whyNot = None),
          CandidateInfo("Rc3", annotation = "", planAlignment = "Rook lift", tacticalAlert = None, practicalDifficulty = "clean", whyNot = Some("it slows the direct attack"))
        ),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 20,
              variations = List(
                VariationLine(moves = List("e1g1", "a7a6"), scoreCp = 44),
                VariationLine(
                  moves = List("a1c3", "a7a6"),
                  scoreCp = 30,
                  parsedMoves = List(PvMove("a1c3", "Rc3", "a1", "c3", "R", isCapture = false, capturedPiece = None, givesCheck = false))
                )
              )
            )
          )
      )
    val rec = new TraceRecorder()
    val (outline, _) = NarrativeOutlineBuilder.build(ctx, rec)
    val decision = outline.getBeat(OutlineBeatKind.DecisionPoint).getOrElse(fail("missing decision beat"))

    assertEquals(decision.text, "This castles.")
    assert(!decision.text.contains("Rc3"), clues(decision.text))
    assert(!decision.text.toLowerCase.contains("direct attack"), clues(decision.text))
    assert(!decision.text.toLowerCase.contains("castle before"), clues(decision.text))
  }
