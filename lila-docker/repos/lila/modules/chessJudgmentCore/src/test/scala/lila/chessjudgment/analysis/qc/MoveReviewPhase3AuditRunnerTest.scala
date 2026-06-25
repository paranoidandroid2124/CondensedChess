package lila.chessjudgment.analysis.qc

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import lila.chessjudgment.analysis.assembly.{ RawMoveReviewInput, RawOpeningContext }
import lila.chessjudgment.model.judgment.*
import lila.chessjudgment.model.strategic.VariationLine
import play.api.libs.json.Json

import scala.jdk.CollectionConverters.*

class MoveReviewPhase3AuditRunnerTest extends munit.FunSuite:

  test("main archives replay input when output path is provided"):
    val dir = Files.createTempDirectory("phase3-audit-runner-main")
    try
      val input = dir.resolve("input.jsonl")
      val output = dir.resolve("phase3_audit_output_current_chunk01_rows001-001.jsonl")
      val row =
        Json.obj(
          "sampleId" -> "sample-main",
          "input" -> Json.obj(
            "fen" -> "8/8/8/8/8/8/4P3/4K3 w - - 0 1",
            "playedMoveUci" -> "e2e4",
            "variations" -> Json.arr(
              Json.obj(
                "moves" -> Json.arr("e2e4"),
                "scoreCp" -> 20,
                "depth" -> 16
              )
            ),
            "currentEvalCp" -> 20,
            "ply" -> 1,
            "movePrefixUci" -> Json.arr("g1f3")
          ),
          "opening" -> "Test Opening",
          "targetPly" -> 1,
          "playedSan" -> "e4"
        )
      Files.writeString(input, Json.stringify(row), StandardCharsets.UTF_8)

      MoveReviewPhase3AuditRunner.main(Array(input.toString, output.toString))

      val archive = dir.resolve("phase3_audit_input_replay_current_chunk01_rows001-001.jsonl")
      assert(Files.exists(output))
      assert(Files.exists(archive))
      val replay = Json.parse(Files.readString(archive, StandardCharsets.UTF_8))
      assertEquals((replay \ "sampleId").as[String], "sample-main")
      assertEquals((replay \ "input" \ "playedMoveUci").as[String], "e2e4")
    finally deleteRecursively(dir)

  test("writes replay input archive next to audit output"):
    val dir = Files.createTempDirectory("phase3-audit-runner")
    try
      val output = dir.resolve("phase3_audit_output_current_chunk01_rows001-001.jsonl")
      val raw =
        RawMoveReviewInput(
          fen = "8/8/8/8/8/8/4P3/4K3 w - - 0 1",
          playedMoveUci = "e2e4",
          variations = List(VariationLine(List("e2e4"), scoreCp = 20, depth = 16)),
          currentEvalCp = Some(20),
          ply = Some(1),
          openingContext = Some(RawOpeningContext(eco = Some("A00"), name = Some("Test Opening"), family = Some("A"))),
          movePrefixUci = List("g1f3")
        )
      val sample =
        MoveReviewPhase3AuditRunner.AuditInputSample(
          sampleId = "sample-1",
          raw = raw,
          opening = Some("Test Opening"),
          sliceKind = Some("eco"),
          targetPly = Some(1),
          playedSan = Some("e4")
        )

      val archive = MoveReviewPhase3AuditRunner.writeReplayInputArchive(output, List(sample))

      assertEquals(archive.getFileName.toString, "phase3_audit_input_replay_current_chunk01_rows001-001.jsonl")
      val rows = Files.readAllLines(archive, StandardCharsets.UTF_8).asScala.toList
      assertEquals(rows.size, 1)
      val json = Json.parse(rows.head)
      assertEquals((json \ "schemaVersion").as[String], "move_review_phase3_replay_input.v1")
      assertEquals((json \ "sampleId").as[String], "sample-1")
      assertEquals((json \ "input" \ "playedMoveUci").as[String], "e2e4")
      assertEquals((json \ "input" \ "variations" \ 0 \ "moves" \ 0).as[String], "e2e4")
      assertEquals((json \ "input" \ "movePrefixUci" \ 0).as[String], "g1f3")
      assertEquals((json \ "opening").as[String], "Test Opening")
    finally deleteRecursively(dir)

  test("structural opportunity funnel counts concrete axis masked by plan fallback"):
    val diagnostic =
      comparisonDiagnostic(
        id = "cmp-outpost-plan-fallback",
        referenceLeadAxes = List("Target:Gain:target-pressure-gain"),
        producedKinds = List(RelativeCauseKind.PlanContradiction),
        flows = List(
          causeFlow(
            causeId = "cause-plan",
            kind = RelativeCauseKind.PlanContradiction,
            proofAxisKeys = List("Target:Gain:target-pressure-gain", "PlanCoherence:Preserve:main-plan"),
            claimIds = List("claim-plan")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.PlanContradiction),
        primaryRootIds = List("cause-plan")
      )

    val funnel = MoveReviewPhase3AuditRunner.structuralOpportunityGenerationFunnelJson(List(diagnostic))

    assertEquals((funnel \ "axisOpportunityCount").as[Int], 1)
    assertEquals((funnel \ "terminalStageCounts" \ "fallback_masked_concrete_axis").as[Int], 1)
    assertEquals(
      (funnel \ "fallbackMaskedConcreteAxisComparisonIds").as[List[String]],
      List("cmp-outpost-plan-fallback")
    )
    assertEquals((funnel \ "causeClassCounts" \ "plan_fallback").as[Int], 1)
    assertEquals((funnel \ "expectedCauseKindCounts" \ "TargetPressureGain").as[Int], 1)
    assertEquals((funnel \ "byAxis" \ "Target:Gain:target-pressure-gain" \ "terminalStageCounts" \ "fallback_masked_concrete_axis").as[Int], 1)

  test("structural opportunity funnel reports outpost evidence preservation"):
    val preserved =
      comparisonDiagnostic(
        id = "cmp-outpost-preserved",
        referenceLeadAxes = List("Activity:Gain:outpost-gain"),
        referenceStructuralConsequences = List(TransitionConsequenceKind.OutpostGain),
        producedKinds = List(RelativeCauseKind.ActivityGain),
        flows = List(
          causeFlow(
            causeId = "cause-outpost-activity",
            kind = RelativeCauseKind.ActivityGain,
            proofAxisKeys = List("Activity:Gain:outpost-gain"),
            claimIds = List("claim-outpost-activity")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.ActivityGain),
        primaryRootIds = List("cause-outpost-activity")
      )
    val lostBeforeAxis =
      comparisonDiagnostic(
        id = "cmp-outpost-lost-before-axis",
        referenceLeadAxes = List("PlanCoherence:Support:outpost-plan"),
        referenceStructuralConsequences = List(TransitionConsequenceKind.OutpostGain),
        producedKinds = List(RelativeCauseKind.PlanContradiction),
        flows = List(
          causeFlow(
            causeId = "cause-plan",
            kind = RelativeCauseKind.PlanContradiction,
            proofAxisKeys = List("PlanCoherence:Support:outpost-plan"),
            claimIds = List("claim-plan")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.PlanContradiction),
        primaryRootIds = List("cause-plan")
      )

    val funnel = MoveReviewPhase3AuditRunner.structuralOpportunityGenerationFunnelJson(List(preserved, lostBeforeAxis))
    val outpost = funnel \ "outpostDetail"

    assertEquals((outpost \ "evidenceComparisonCount").as[Int], 2)
    assertEquals((outpost \ "axisPreservedComparisonCount").as[Int], 1)
    assertEquals((outpost \ "axisOpportunityCount").as[Int], 1)
    assertEquals((outpost \ "evidenceWithoutAxisComparisonIds").as[List[String]], List("cmp-outpost-lost-before-axis"))
    assertEquals((outpost \ "terminalStageCounts" \ "final_structural_root").as[Int], 1)
    assertEquals((outpost \ "expectedCauseKindCounts" \ "ActivityGain").as[Int], 1)

  test("structural opportunity funnel reports opponent restriction detail preservation"):
    val preserved =
      comparisonDiagnostic(
        id = "cmp-opponent-restriction-preserved",
        referenceLeadAxes = List("Counterplay:Restrain:opponent-low-mobility"),
        producedKinds = List(RelativeCauseKind.OpponentRestriction),
        flows = List(
          causeFlow(
            causeId = "cause-opponent-restriction",
            kind = RelativeCauseKind.OpponentRestriction,
            proofAxisKeys = List("Counterplay:Restrain:opponent-low-mobility"),
            claimIds = List("claim-opponent-restriction")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.OpponentRestriction),
        primaryRootIds = List("cause-opponent-restriction")
      )
    val planMasked =
      comparisonDiagnostic(
        id = "cmp-opponent-restriction-plan-fallback",
        referenceLeadAxes = List("Counterplay:Restrain:opponent-low-mobility"),
        producedKinds = List(RelativeCauseKind.PlanContradiction),
        flows = List(
          causeFlow(
            causeId = "cause-plan",
            kind = RelativeCauseKind.PlanContradiction,
            proofAxisKeys = List("Counterplay:Restrain:opponent-low-mobility", "PlanCoherence:Support:prophylaxis"),
            claimIds = List("claim-plan")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.PlanContradiction),
        primaryRootIds = List("cause-plan")
      )

    val funnel = MoveReviewPhase3AuditRunner.structuralOpportunityGenerationFunnelJson(List(preserved, planMasked))
    val restriction = funnel \ "opponentRestrictionDetail"

    assertEquals((restriction \ "axisOpportunityCount").as[Int], 2)
    assertEquals((restriction \ "axisOpportunityComparisonCount").as[Int], 2)
    assertEquals((restriction \ "terminalStageCounts" \ "final_structural_root").as[Int], 1)
    assertEquals((restriction \ "terminalStageCounts" \ "fallback_masked_concrete_axis").as[Int], 1)
    assertEquals((restriction \ "expectedCauseKindCounts" \ "OpponentRestriction").as[Int], 2)
    assertEquals(
      (restriction \ "fallbackMaskedConcreteAxisComparisonIds").as[List[String]],
      List("cmp-opponent-restriction-plan-fallback")
    )

  test("structural opportunity funnel tracks exact axis lineage instead of same-kind roots"):
    val diagnostic =
      comparisonDiagnostic(
        id = "cmp-same-kind-different-axis",
        referenceLeadAxes = List("Activity:Gain:outpost-gain", "Activity:Gain:mobility-gain"),
        producedKinds = List(RelativeCauseKind.ActivityGain),
        flows = List(
          causeFlow(
            causeId = "cause-activity-mobility",
            kind = RelativeCauseKind.ActivityGain,
            proofAxisKeys = List("Activity:Gain:mobility-gain"),
            claimIds = List("claim-activity-mobility")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.ActivityGain),
        primaryRootIds = List("cause-activity-mobility")
      )

    val funnel = MoveReviewPhase3AuditRunner.structuralOpportunityGenerationFunnelJson(List(diagnostic))
    val exact = funnel \ "axisExactLineage"

    assertEquals((exact \ "axisOpportunityCount").as[Int], 2)
    assertEquals((exact \ "exactRootCount").as[Int], 1)
    assertEquals((exact \ "kindOnlyRootWithoutAxisCount").as[Int], 1)
    assertEquals(
      (exact \ "byAxis" \ "Activity:Gain:mobility-gain" \ "terminalStageCounts" \ "final_structural_root").as[Int],
      1
    )
    assertEquals(
      (exact \ "byAxis" \ "Activity:Gain:outpost-gain" \ "terminalStageCounts" \ "kind_only_root_without_axis").as[Int],
      1
    )

  test("binding width audit reports broad and colocated graph bindings"):
    val view = MoveJudgmentView(
      verdict = None,
      verdictCarriers = Nil,
      primaryCauses = List(
        causeFrame(
          causeId = "cause-side-only",
          axisKeys = List("Counterplay:Restrain:opponent-low-mobility"),
          objectSignatures = List(
            "target=Side:black|mechanism=Mechanism:counterplayrestraint|consequence=Consequence:counterplayrestraint|proof=DirectProof"
          )
        ),
        causeFrame(
          causeId = "cause-context-only",
          axisKeys = List("Activity:Gain:outpost-gain"),
          objectSignatures = List(
            "target=Square:e5|mechanism=Mechanism:activity|consequence=Consequence:activity|proof=ContextSupport"
          )
        ),
        causeFrame(
          causeId = "cause-outpost-direct",
          axisKeys = List("Activity:Gain:outpost-gain"),
          objectSignatures = List(
            "target=Square:d5|mechanism=Mechanism:activity|consequence=Consequence:activity|proof=DirectProof"
          )
        ),
        causeFrame(
          causeId = "cause-colocated",
          axisKeys = Nil,
          objectSignatures = Nil,
          witnessBindingLevel = MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly
        )
      ),
      secondaryCauses = Nil,
      contextCauses = Nil,
      supportContextClusterIds = Nil,
      overriddenLocalIdeas = Nil,
      preservedLocalIdeas = Nil
    )

    val audit = MoveReviewPhase3AuditRunner.bindingWidthAuditJson(view)

    assertEquals((audit \ "sideOnlyTargetFrameCount").as[Int], 1)
    assertEquals((audit \ "contextSupportOnlyBindingFrameCount").as[Int], 1)
    assertEquals((audit \ "axisWithMultipleObjectFingerprintsCount").as[Int], 1)
    assertEquals((audit \ "sameComparisonOnlyBindingFrameCount").as[Int], 1)
    assertEquals((audit \ "witnessBindingLevelCounts" \ "SameComparisonOnly").as[Int], 1)

  test("axisless structural anchor inventory reports structural signals that cannot enter axis lineage"):
    val root = PositionNodeRef("8/8/8/8/8/8/8/8 w - - 0 1", 1, Some(chess.Color.White), Some("root"))
    val after = PositionNodeRef("8/8/8/8/8/8/8/8 b - - 1 1", 2, Some(chess.Color.Black), Some("after"))
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = after,
      line = Some(candidateLine),
      perspective = chess.Color.White
    )
    val axislessRef = evidenceRef("structural-delta:axisless", root)
    val axislessPayload = StructuralDeltaEvidence(
      transition = transition,
      signals = Nil,
      consequences = List(
        TransitionConsequence(TransitionConsequenceKind.PassedPawnProgress, StructuralSignalPolarity.Gain, 3),
        TransitionConsequence(TransitionConsequenceKind.PromotionPressureGain, StructuralSignalPolarity.Gain, 3)
      )
    )
    val axisRef = evidenceRef("structural-delta:outpost", root)
    val axisPayload = StructuralDeltaEvidence(
      transition = transition,
      signals = Nil,
      consequences = List(
        TransitionConsequence(
          TransitionConsequenceKind.OutpostGain,
          StructuralSignalPolarity.Gain,
          3,
          subjects = List("outpost:knight:e5")
        )
      )
    )
    val inventory = MoveReviewPhase3AuditRunner.axislessStructuralAnchorInventoryJson(
      TypedEvidenceGraph(
        List(
          EvidenceRecord(axislessRef, axislessPayload),
          EvidenceRecord(axisRef, axisPayload)
        )
      )
    )

    assertEquals((inventory \ "axislessSignalCount").as[Int], 2)
    assertEquals((inventory \ "axislessRecordCount").as[Int], 1)
    assertEquals((inventory \ "axislessSignalLabelCounts" \ "passed-pawn-progress").as[Int], 1)
    assertEquals((inventory \ "axislessSignalLabelCounts" \ "promotion-pressure-gain").as[Int], 1)
    assertEquals((inventory \ "axislessRecordIds").as[List[String]], List("structural-delta:axisless"))

  private def deleteRecursively(path: Path): Unit =
    if Files.exists(path) then
      Files
        .walk(path)
        .iterator()
        .asScala
        .toList
        .sortWith((left, right) => left.getNameCount > right.getNameCount)
        .foreach(Files.deleteIfExists)

  private def lineRef(id: String, rootMove: String, rank: Int, role: LineNodeRole): LineNodeRef =
    LineNodeRef(id, rootMove, rank, role)

  private val referenceLine = lineRef("best", "e2e4", 1, LineNodeRole.BestReference)
  private val candidateLine = lineRef("played", "e2e3", 2, LineNodeRole.Played)

  private def lineDiagnostic(ref: LineNodeRef): CandidateLineDiagnostic =
    CandidateLineDiagnostic(
      id = ref.id,
      rootMove = ref.rootMove,
      role = ref.role,
      rank = ref.rank,
      moves = List(ref.rootMove),
      whitePovEvalCp = Some(20),
      mate = None,
      depth = Some(16)
    )

  private def evidenceRef(id: String, position: PositionNodeRef): EvidenceRef =
    EvidenceRef(
      id = id,
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = position,
      line = Some(candidateLine),
      scope = EvidenceScope.PlayedTransition,
      confidence = EvidenceConfidence.EngineBacked
    )

  private def semanticAxes(referenceLeadAxes: List[String]): SemanticAxisDiagnostics =
    SemanticAxisDiagnostics(
      referenceAxisStrengths = referenceLeadAxes.map(_ -> 2).toMap,
      candidateAxisStrengths = Map.empty,
      referenceAxisSourceIds = referenceLeadAxes.map(_ -> List("axis-src")).toMap,
      candidateAxisSourceIds = Map.empty,
      referenceAxisSourceLayers = referenceLeadAxes.map(_ -> List(EvidenceLayer.StrategicMechanism)).toMap,
      candidateAxisSourceLayers = Map.empty,
      referenceAxes = referenceLeadAxes,
      candidateAxes = Nil,
      sharedAxes = Nil,
      referenceOnlyAxes = referenceLeadAxes,
      candidateOnlyAxes = Nil,
      referenceLeadAxes = referenceLeadAxes,
      candidateLeadAxes = Nil
    )

  private def causeFlow(
      causeId: String,
      kind: RelativeCauseKind,
      proofAxisKeys: List[String],
      claimIds: List[String]
  ): RelativeCauseFlowDiagnostic =
    RelativeCauseFlowDiagnostic(
      causeId = causeId,
      causeKind = kind,
      causeRole = RelativeCauseRole.PrimaryPlayedCause,
      causeComparisonKind = CandidateComparisonKind.PlayedVsBest,
      causeSourceSide = RelativeCauseSourceSide.Candidate,
      causeEventLine = candidateLine,
      proofStrategicAxisKeys = proofAxisKeys,
      familyMismatchKinds = Set.empty,
      expectedIdeaFamilies = Set(ChessIdeaFamily.Strategic),
      actualIdeaFamilies = Set(ChessIdeaFamily.Strategic),
      expectedClaimFamilies = Set(ClaimFamily.Strategic),
      claimCandidateFamilies = Set(ClaimFamily.Strategic),
      finalClaimFamilies = Set(ClaimFamily.Strategic),
      directProofSourceIds = List("direct-proof"),
      contrastProofSourceIds = Nil,
      contextSupportSourceIds = Nil,
      directProofKinds = proofAxisKeys.map(axis => s"StrategicAxis:$axis"),
      contrastProofKinds = Nil,
      contextSupportKinds = Nil,
      hasOwnedTypedDepth = true,
      hasOwnedAdmissibleLongTermProof = true,
      eventClusterExpected = false,
      ideaIds = List("idea-plan"),
      claimCandidateIds = List("claim-candidate-plan"),
      claimCandidateStages = Set(ClaimLifecycleStage.CandidateCreated, ClaimLifecycleStage.TruthCertified),
      claimCandidateTruthStatuses = Set(ClaimLifecycleTruthStatus.Certified),
      claimCandidateDroppedStages = Set.empty,
      claimIds = claimIds,
      eventClusterIds = Nil,
      eventClusterMissingSupportEvidenceIds = Nil,
      causeWithoutIdea = false,
      ideaWithoutClaimCandidate = false,
      ideaWithoutFinalClaim = false,
      claimWithoutEventCluster = false,
      strategicCauseWithoutContrast = false,
      contextSupportUsedAsDirectProof = false,
      contextOnlyAttribution = false,
      unattributedCause = false,
      rootMismatchedAttribution = false,
      supportPromotedToDirectProof = false,
      objectBindingSignatures = List("axis:outpost"),
      relativeCauseWithoutObjectSignature = false,
      objectLostBetweenEvidenceAndCause = false,
      objectLostBetweenCauseAndClaim = false
    )

  private def causeFrame(
      causeId: String,
      axisKeys: List[String],
      objectSignatures: List[String],
      witnessBindingLevel: MoveJudgmentCauseWitnessBindingLevel = MoveJudgmentCauseWitnessBindingLevel.NotWitness
  ): MoveJudgmentCauseFrame =
    MoveJudgmentCauseFrame(
      role = MoveJudgmentCauseFrameRole.PrimaryCause,
      clusterId = None,
      framed = false,
      causeEvidenceIds = List(causeId),
      causeKind = RelativeCauseKind.ActivityGain,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      causeRole = RelativeCauseRole.PrimaryPlayedCause,
      causeSourceSide = RelativeCauseSourceSide.Candidate,
      causeImportance = RelativeCauseImportance.Primary,
      attributionKind = CauseAttributionKind.CandidateAllowsLiability,
      attributionRootMoveMatched = true,
      attributionDirectProofEligible = true,
      referenceLine = referenceLine,
      candidateLine = candidateLine,
      eventLine = candidateLine,
      eventRootMove = candidateLine.rootMove,
      causeClaimIds = Nil,
      evaluationClaimIds = Nil,
      witnessClaimIds = Nil,
      ideaIds = Nil,
      supportIdeaIds = Nil,
      claimCandidateIds = Nil,
      finalClaimIds = Nil,
      relatedSupportClusterIds = Nil,
      evidenceIds = List(causeId),
      proofDirectSourceIds = Nil,
      proofContrastSourceIds = Nil,
      proofContextSupportSourceIds = Nil,
      proofStrategicAxisLineage = Nil,
      proofStrategicAxisKeys = axisKeys,
      proofStrategicMechanismKinds = Nil,
      proofStrategicMechanismSourceIds = Nil,
      proofStrategicMechanismSignalSourceIds = Nil,
      supportEvidenceSourceIds = Nil,
      objectBindingSignatures = objectSignatures,
      concreteObjectReady = objectSignatures.nonEmpty,
      witnessBindingLevel = witnessBindingLevel,
      witnessBindingSignals =
        if witnessBindingLevel == MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly then
          List(MoveJudgmentCauseWitnessBindingSignal.SameComparison)
        else Nil
    )

  private def comparisonDiagnostic(
      id: String,
      referenceLeadAxes: List[String],
      referenceStructuralConsequences: List[TransitionConsequenceKind] = Nil,
      candidateStructuralConsequences: List[TransitionConsequenceKind] = Nil,
      producedKinds: List[RelativeCauseKind],
      flows: List[RelativeCauseFlowDiagnostic],
      primaryRootKinds: List[RelativeCauseKind],
      primaryRootIds: List[String]
  ): CandidateComparisonDiagnostic =
    val axes = semanticAxes(referenceLeadAxes)
    CandidateComparisonDiagnostic(
      id = id,
      comparisonFingerprint = id,
      dedupeKey = id,
      dedupeClass = CandidateComparisonDedupeClass.Unique,
      subjectBinding = SubjectBindingClass.PrimaryPlayedCause,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = lineDiagnostic(referenceLine),
      candidateLine = lineDiagnostic(candidateLine),
      verdict = MoveChoiceVerdict.Mistake,
      mover = "White",
      rawCpLossForDiagnostics = 120,
      rawCandidateDeltaCpForDiagnostics = -120,
      winPercentLossForMover = 18.0,
      candidateWinPercentDeltaForMover = -18.0,
      causeKinds = producedKinds,
      causeRoles = List(RelativeCauseRole.PrimaryPlayedCause),
      causeSourceSides = List(RelativeCauseSourceSide.Candidate),
      causeImportances = Nil,
      causeEventLines = List(candidateLine),
      causeSupport = Nil,
      relativeCauseDiagnostics = ComparisonRelativeCauseDiagnostics(
        expectedCauseHints = producedKinds,
        missingExpectedCauseHints = Nil,
        producedCauseIds = flows.map(_.causeId),
        producedCauseKinds = producedKinds,
        producedCauseRoles = List(RelativeCauseRole.PrimaryPlayedCause),
        producedCauseSourceSides = List(RelativeCauseSourceSide.Candidate),
        producedCauseEventLines = List(candidateLine),
        missingCause = false,
        shallowProofCauseIds = Nil,
        genericCauseIds = Nil,
        ownedTypedDepthCauseIds = flows.map(_.causeId),
        nonGenericCauseIds = flows.map(_.causeId),
        unboundEvidenceIds = Nil,
        wrongRoleCauseIds = Nil,
        wrongSourceSideCauseIds = Nil,
        wrongEventLineCauseIds = Nil,
        wrongImportanceCauseIds = Nil,
        causeWithoutIdeaIds = Nil,
        ideaWithoutClaimCandidateCauseIds = Nil,
        ideaWithoutFinalClaimCauseIds = Nil,
        ideaWithoutClaimCauseIds = Nil,
        claimWithoutEventClusterCauseIds = Nil,
        eventClusterSupportMissingCauseIds = Nil,
        strategicCauseWithoutContrastIds = Nil,
        strategicClaimWithoutComparativeCauseIds = Nil,
        genericStructuralImprovementWithoutStrategicContrastIds = Nil,
        contextSupportUsedAsDirectProofIds = Nil,
        contextOnlyCauseIds = Nil,
        unattributedCauseIds = Nil,
        rootMismatchedCauseIds = Nil,
        supportPromotedToDirectProofCauseIds = Nil,
        relativeCauseWithoutObjectSignatureIds = Nil,
        objectLostBetweenEvidenceAndCauseIds = Nil,
        objectLostBetweenCauseAndClaimIds = Nil,
        causeFlow = flows
      ),
      moveJudgmentView = ComparisonMoveJudgmentViewDiagnostics(
        primaryCauseKinds = primaryRootKinds,
        secondaryCauseKinds = Nil,
        contextCauseKinds = Nil,
        primaryCauseEvidenceIds = primaryRootIds,
        primaryIdeaFamilies = Set(ChessIdeaFamily.Strategic),
        primaryClaimCandidateFamilies = Set(ClaimFamily.Strategic),
        primaryFinalClaimFamilies = Set(ClaimFamily.Strategic),
        primaryFramedCauseKinds = Nil,
        primaryUnframedCauseKinds = primaryRootKinds,
        primaryRootCauseKinds = primaryRootKinds,
        primaryTacticalWitnessCauseKinds = Nil,
        primaryPunishmentWitnessCauseKinds = Nil,
        primaryContextualTacticalWitnessCauseKinds = Nil,
        primaryRootCauseEvidenceIds = primaryRootIds,
        primaryTacticalWitnessCauseEvidenceIds = Nil,
        primaryPunishmentWitnessCauseEvidenceIds = Nil,
        primaryContextualTacticalWitnessCauseEvidenceIds = Nil,
        secondaryCauseEvidenceIds = Nil,
        contextCauseEvidenceIds = Nil,
        projectedContextCauseNoViewIds = Nil,
        playableLossPrimaryCauseEvidenceIds = Nil,
        objectlessPrimaryCauseEvidenceIds = Nil,
        objectlessSecondaryCauseEvidenceIds = Nil,
        objectlessContextCauseEvidenceIds = Nil
      ),
      advisoryCauseHints = Nil,
      significanceReasons = List(CandidateComparisonSignificanceReason.PlayedLoss),
      lowSignalReasons = Nil,
      comparisonConfidence = EvidenceConfidence.EngineBacked,
      causeConfidences = List(EvidenceConfidence.EngineBacked),
      hasLowDepthCause = false,
      hasUnexplainedEngineGap = false,
      hasSecondaryContextEngineGap = false,
      evidenceLayers = ComparisonEvidenceLayerDiagnostics(
        reference = EvidenceLayerNeighborhood(Set.empty, Map.empty, Map.empty),
        candidate = EvidenceLayerNeighborhood(Set.empty, Map.empty, Map.empty)
      ),
      decisionTrace = CandidateCauseDecisionTrace(
        badLoss = true,
        tacticalLoss = false,
        majorLoss = true,
        candidateBetter = false,
        requiresExplanatoryCause = true,
        positiveContextAlternative = false,
        referenceTacticalMechanismKinds = Nil,
        candidateTacticalMechanismKinds = Nil,
        referenceConcreteLine = true,
        candidateConcreteLine = true,
        candidateTacticalRefutationBridge = false,
        referenceTacticalRisk = false,
        hasOnlyDefense = false,
        hasThreatResource = false,
        referenceProphylacticResource = false,
        referenceKingStepResource = false,
        candidateKingStepResource = false,
        referenceCastlingResource = false,
        candidateCastlingResource = false,
        referencePreventivePawnResource = false,
        candidatePreventivePawnResource = false,
        hasConversionWindow = false,
        referenceConversionWindow = false,
        candidateConversionWindow = false,
        referenceStructuralTargetRelease = false,
        referenceReleasedTargets = Nil,
        candidateReleasedTargets = Nil,
        referenceCreatedTargets = Nil,
        candidateCreatedTargets = Nil,
        referenceStructuralImprovement = true,
        candidateStructuralImprovement = false,
        candidatePawnStructureImprovement = false,
        candidateTargetPressureGain = false,
        candidateCenterControlGain = false,
        candidateDevelopmentActivation = false,
        candidatePieceActivityGain = false,
        sameDestinationCaptureChoice = false,
        referenceStructuralSignals = referenceLeadAxes,
        candidateStructuralSignals = Nil,
        referenceStructuralConsequences = referenceStructuralConsequences,
        candidateStructuralConsequences = candidateStructuralConsequences,
        candidatePawnStructureSignals = Nil,
        referenceSemanticAxisCount = referenceLeadAxes.size,
        candidateSemanticAxisCount = 0,
        referenceSemanticAxisLead = referenceLeadAxes.nonEmpty,
        semanticAxisDiagnostics = axes,
        candidatePlanEvidence = false,
        candidateStrategicEvidence = false,
        candidateStrategicConcessionEvidence = true,
        referencePassedPawnResource = false,
        candidatePassedPawnResource = false,
        referenceEndgameResource = false,
        candidateEndgameResource = false,
        referenceLooseMaterialExploit = false,
        candidateLooseMaterialExploit = false,
        materialSwingEvidence = false,
        referenceMaterialNetCp = 0,
        candidateMaterialNetCp = 0,
        referenceMaterialMaxGainCp = 0,
        candidateMaterialMaxGainCp = 0,
        referenceMaterialPromotionGainCp = 0,
        candidateMaterialPromotionGainCp = 0,
        referenceMaterialRecapture = false,
        candidateMaterialRecapture = false,
        referenceMaterialRecovery = false,
        candidateMaterialRecovery = false,
        referenceMaterialComplete = false,
        candidateMaterialComplete = false,
        referenceRelationKinds = Nil,
        candidateRelationKinds = Nil,
        relationKinds = Nil,
        referenceMotifs = Nil,
        candidateMotifs = Nil
      ),
      tacticalLossTrace = TacticalLossTrace(false, 0.0, None, None, Nil),
      failureClass = CandidateComparisonFailureClass.NoFailure,
      failureReasons = Nil
    )
