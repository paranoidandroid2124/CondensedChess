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
      val summary = dir.resolve("phase3_audit_summary_current_chunk01_rows001-001.json")
      assert(Files.exists(output))
      assert(Files.exists(archive))
      assert(Files.exists(summary))
      val replay = Json.parse(Files.readString(archive, StandardCharsets.UTF_8))
      assertEquals((replay \ "sampleId").as[String], "sample-main")
      assertEquals((replay \ "input" \ "playedMoveUci").as[String], "e2e4")
      val summaryJson = Json.parse(Files.readString(summary, StandardCharsets.UTF_8))
      assertEquals((summaryJson \ "schemaVersion").as[String], "move_review_phase3_audit_summary.v1")
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

  test("comparison diagnostics keep contrast plan technique frames on their owning comparison"):
    val root = PositionNodeRef("8/8/8/8/8/8/4P3/4K3 w - - 0 1", 1, Some(chess.Color.White), Some("root"))
    val referenceLineA = LineNodeRef("reference-line-a", "e2e4", 1, LineNodeRole.BestReference)
    val referenceLineB = LineNodeRef("reference-line-b", "d2d4", 1, LineNodeRole.BestReference)
    val sharedCandidateLine = LineNodeRef("played-line", "g1f3", 2, LineNodeRole.Played)
    def comparison(referenceLine: LineNodeRef) =
      CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = sharedCandidateLine,
        comparison = EvalComparison(
          mover = chess.Color.White,
          referenceLine = referenceLine,
          candidateLine = sharedCandidateLine,
          rawCandidateDeltaCpForDiagnostics = -90,
          candidateWinPercentDeltaForMover = -9.0,
          rawCpLossForDiagnostics = 90,
          winPercentLossForMover = 9.0,
          verdict = MoveChoiceVerdict.Inaccuracy
        )
      )
    val comparisonA = comparison(referenceLineA)
    val comparisonB = comparison(referenceLineB)
    def comparisonRef(id: String, fact: CandidateComparisonFact) =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.RelativeMoveProducer,
        layer = EvidenceLayer.CandidateComparison,
        position = root,
        line = Some(fact.candidateLine),
        scope = EvidenceScope.Counterfactual,
        confidence = EvidenceConfidence.EngineBacked
      )
    val contrastRef =
      EvidenceRef(
        id = "strategic-contrast:central-break-owner",
        producer = EvidenceProducer.StrategicMechanismProducer,
        layer = EvidenceLayer.StrategicMechanism,
        position = root,
        line = Some(sharedCandidateLine),
        scope = EvidenceScope.Counterfactual,
        confidence = EvidenceConfidence.Mixed
      )
    val axis = StrategicAxisDetail(StrategicAxisKind.PawnBreak, StrategicAxisPolarity.Support, "central-break-timing")
    val contrast =
      StrategicMechanismContrastEvidence(
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLineA,
        candidateLine = sharedCandidateLine,
        axisComparisons = List(
          StrategicAxisComparison(
            axis = axis,
            outcome = StrategicAxisComparisonOutcome.ReferenceStronger,
            referenceStrength = 2,
            candidateStrength = 0,
            referenceSources = Nil,
            candidateSources = Nil
          )
        ),
        planComparison = None,
        sustainability = StrategicSustainabilityAssessment(
          horizon = StrategicSustainabilityHorizon.ShortPv,
          lineMaintained = true,
          pvMaintained = true,
          referencePlyCount = 4,
          candidatePlyCount = 4
        ),
        support = StrategicContrastSupport(Nil, Nil, Nil)
      )
    val contrastFrame =
      PositionPlanTechniqueFrame(
        id = "position-plan-technique:strategic-contrast:central-break-owner",
        units = List(PositionPlanTechniqueUnit.TensionBreakPolicyRoute),
        position = root,
        line = Some(sharedCandidateLine),
        moveUci = Some("g1f3"),
        scope = EvidenceScope.Counterfactual,
        mechanismKinds = Nil,
        strategicAxisKeys = List(axis.stableKey),
        semanticAnchors = Nil,
        objectBindingSignatures = Nil,
        semanticDetails = List(
          PositionPlanTechniqueSemanticDetail(
            unit = PositionPlanTechniqueUnit.TensionBreakPolicyRoute,
            axisKey = Some(axis.stableKey),
            axisKind = Some(axis.kind),
            axisPolarity = Some(axis.polarity),
            label = Some(axis.label),
            contrastOutcome = Some(StrategicAxisComparisonOutcome.ReferenceStronger),
            referenceStrength = Some(2),
            candidateStrength = Some(0),
            sourceEvidenceIds = List(contrastRef.id)
          )
        ),
        evidenceIds = List(contrastRef.id),
        mechanismEvidenceIds = List(contrastRef.id),
        sourceEvidenceIds = Nil,
        relativeCauseEvidenceIds = Nil,
        ideaIds = Nil,
        claimIds = Nil,
        planComparison = None,
        relationToVerdict = None,
        confidence = EvidenceConfidence.Mixed,
        salience = 3
      )
    val packet =
      EvidenceBackedJudgmentPacket(
        root = root,
        positions = List(PositionNode(PositionNodeRole.Before, root, facts = Nil, features = None, assessment = None, evidence = Nil)),
        candidateLines = Nil,
        transitions = Nil,
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(comparisonRef("candidate-comparison:owner", comparisonA), CandidateComparisonEvidence(comparisonA)),
            EvidenceRecord(comparisonRef("candidate-comparison:shared-candidate-other-reference", comparisonB), CandidateComparisonEvidence(comparisonB)),
            EvidenceRecord(contrastRef, contrast)
          )
        ),
        ideas = Nil,
        claims = Nil,
        ideaVerdict = None,
        moveJudgmentView = Some(
          MoveJudgmentView(
            verdict = None,
            verdictCarriers = Nil,
            primaryCauses = Nil,
            secondaryCauses = Nil,
            contextCauses = Nil,
            positionPlanTechniqueFrames = List(contrastFrame),
            supportContextClusterIds = Nil,
            overriddenLocalIdeas = Nil,
            preservedLocalIdeas = Nil
          )
        )
      )

    val diagnostics = CandidateComparisonDiagnostic.fromPacket(packet).map(diagnostic => diagnostic.id -> diagnostic).toMap

    assertEquals(
      diagnostics("candidate-comparison:owner").moveJudgmentView.positionPlanTechniqueFrameIds,
      List(contrastFrame.id)
    )
    assertEquals(
      diagnostics("candidate-comparison:shared-candidate-other-reference").moveJudgmentView.positionPlanTechniqueFrameIds,
      Nil
    )

  test("root arbitration quality summary separates fallback broad and context-only tiers"):
    val exact =
      comparisonDiagnostic(
        id = "cmp-exact-root",
        referenceLeadAxes = List("Activity:Gain:outpost-gain"),
        producedKinds = List(RelativeCauseKind.ActivityGain),
        flows = List(
          causeFlow(
            causeId = "cause-exact",
            kind = RelativeCauseKind.ActivityGain,
            proofAxisKeys = List("Activity:Gain:outpost-gain"),
            claimIds = List("claim-exact")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.ActivityGain),
        primaryRootIds = List("cause-exact"),
        rootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot),
        primaryRootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot)
      )
    val fallback =
      comparisonDiagnostic(
        id = "cmp-plan-fallback",
        referenceLeadAxes = List("PlanCoherence:Preserve:main-plan"),
        producedKinds = List(RelativeCauseKind.PlanContradiction),
        flows = List(
          causeFlow(
            causeId = "cause-plan",
            kind = RelativeCauseKind.PlanContradiction,
            proofAxisKeys = List("PlanCoherence:Preserve:main-plan"),
            claimIds = List("claim-plan")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.PlanContradiction),
        primaryRootIds = List("cause-plan"),
        rootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.FallbackRoot),
        primaryRootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.FallbackRoot)
      )
    val broadAndContext =
      comparisonDiagnostic(
        id = "cmp-broad-context",
        referenceLeadAxes = List("Activity:Gain:mobility-gain"),
        producedKinds = List(RelativeCauseKind.ActivityGain, RelativeCauseKind.TacticalRefutationOfPlayed),
        flows = List(
          causeFlow(
            causeId = "cause-broad",
            kind = RelativeCauseKind.ActivityGain,
            proofAxisKeys = List("Activity:Gain:mobility-gain"),
            claimIds = List("claim-broad")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.TacticalRefutationOfPlayed),
        primaryRootIds = List("cause-tactical"),
        rootArbitrationTiers = List(
          MoveJudgmentCauseRootArbitrationTier.BroadOwnedRoot,
          MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot,
          MoveJudgmentCauseRootArbitrationTier.ContextOnly
        ),
        primaryRootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot)
      )

    val summary = MoveReviewPhase3AuditRunner.rootArbitrationQualitySummaryJson(List(exact, fallback, broadAndContext))

    assertEquals((summary \ "classification").as[String], "audit_only")
    assertEquals((summary \ "comparisonCount").as[Int], 3)
    assertEquals((summary \ "tierCounts" \ "FallbackRoot").as[Int], 1)
    assertEquals((summary \ "tierCounts" \ "BroadOwnedRoot").as[Int], 1)
    assertEquals((summary \ "tierCounts" \ "ContextOnly").as[Int], 1)
    assertEquals((summary \ "primaryRootTierCounts" \ "ExactOwnedRoot").as[Int], 1)
    assertEquals((summary \ "primaryRootTierCounts" \ "FallbackRoot").as[Int], 1)
    assertEquals((summary \ "primaryRootTierCounts" \ "ConcreteOwnedRoot").as[Int], 1)
    assertEquals((summary \ "fallbackRootComparisonIds").as[List[String]], List("cmp-plan-fallback"))
    assertEquals((summary \ "broadOwnedRootComparisonIds").as[List[String]], List("cmp-broad-context"))
    assertEquals((summary \ "contextOnlyComparisonIds").as[List[String]], List("cmp-broad-context"))

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

  test("plan technique eligibility funnel separates playable tactical and structural no-axis bad rows"):
    val structuralAxis =
      comparisonDiagnostic(
        id = "cmp-structural-axis",
        referenceLeadAxes = List("Activity:Gain:outpost-gain"),
        producedKinds = List(RelativeCauseKind.ActivityGain),
        flows = List(
          causeFlow(
            causeId = "cause-activity",
            kind = RelativeCauseKind.ActivityGain,
            proofAxisKeys = List("Activity:Gain:outpost-gain"),
            claimIds = List("claim-activity")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.ActivityGain),
        primaryRootIds = List("cause-activity")
      )
    val playableNoAxis =
      comparisonDiagnostic(
        id = "cmp-playable-no-axis",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil,
        verdict = MoveChoiceVerdict.PlayableLoss
      )
    val tacticalNoAxis =
      comparisonDiagnostic(
        id = "cmp-tactical-no-axis",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil,
        relationKinds = List(RelationFactKind.Pin)
      )
    val structuralMissingAxis =
      comparisonDiagnostic(
        id = "cmp-structural-missing-axis",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil
      )

    val summary =
      MoveReviewPhase3AuditRunner.relativeCauseQualitySummaryJson(
        List(structuralAxis, playableNoAxis, tacticalNoAxis, structuralMissingAxis)
      )
    val funnel = summary \ "planTechniqueEligibilityFunnel"

    assertEquals((funnel \ "comparisonCount").as[Int], 4)
    assertEquals((funnel \ "structuralAxisPresentCount").as[Int], 1)
    assertEquals((funnel \ "playableLossNoAxisCount").as[Int], 1)
    assertEquals((funnel \ "tacticalOnlyNoAxisCount").as[Int], 1)
    assertEquals((funnel \ "structuralAxisMissingCandidateCount").as[Int], 1)
    assertEquals((funnel \ "classificationCounts" \ "structural_axis_present").as[Int], 1)
    assertEquals((funnel \ "classificationCounts" \ "playable_loss_no_axis").as[Int], 1)
    assertEquals((funnel \ "classificationCounts" \ "tactical_only_no_axis").as[Int], 1)
    assertEquals((funnel \ "classificationCounts" \ "structural_axis_missing_candidate").as[Int], 1)
    assertEquals(
      (funnel \ "structuralAxisMissingCandidateComparisonIds").as[List[String]],
      List("cmp-structural-missing-axis")
    )

  test("plan technique anchor eligibility only escalates axisless inventory when a structural missing candidate exists"):
    val playableNoAxis =
      comparisonDiagnostic(
        id = "cmp-playable-no-axis",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil,
        verdict = MoveChoiceVerdict.PlayableLoss
      )
    val structuralMissingAxis =
      comparisonDiagnostic(
        id = "cmp-structural-missing-axis",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil
      )
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
    val axislessGraph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(
            evidenceRef("structural-delta:axisless", root),
            StructuralDeltaEvidence(
              transition = transition,
              signals = Nil,
              consequences = List(
                TransitionConsequence(TransitionConsequenceKind.PassedPawnProgress, StructuralSignalPolarity.Gain, 3),
                TransitionConsequence(TransitionConsequenceKind.PromotionPressureGain, StructuralSignalPolarity.Gain, 3)
              )
            )
          )
        )
      )

    val inventoryOnly =
      MoveReviewPhase3AuditRunner.planTechniqueAnchorEligibilityJson(List(playableNoAxis), axislessGraph)
    val missingWithInventory =
      MoveReviewPhase3AuditRunner.planTechniqueAnchorEligibilityJson(List(playableNoAxis, structuralMissingAxis), axislessGraph)

    assertEquals((inventoryOnly \ "resolution").as[String], "inventory_only_no_structural_missing_candidate")
    assertEquals((inventoryOnly \ "axislessStructuralAnchorSignalCount").as[Int], 2)
    assertEquals((inventoryOnly \ "axislessStructuralAnchorPlanTechniqueUnitCandidateCounts" \ "StructuralTransformation").as[Int], 2)
    assertEquals((inventoryOnly \ "structuralAxisMissingCandidateCount").as[Int], 0)
    assertEquals((inventoryOnly \ "structuralAxisMissingCandidateWithAnyPacketAxislessAnchorCount").as[Int], 0)
    assertEquals((missingWithInventory \ "resolution").as[String], "upstream_axis_generation_candidate")
    assertEquals((missingWithInventory \ "structuralAxisMissingCandidateCount").as[Int], 1)
    assertEquals((missingWithInventory \ "structuralAxisMissingCandidateWithAnyPacketAxislessAnchorCount").as[Int], 1)
    assertEquals(
      (missingWithInventory \ "axislessStructuralAnchorPlanTechniqueUnitCandidateLabels" \ "StructuralTransformation").as[List[String]],
      List("passed-pawn-progress", "promotion-pressure-gain")
    )
    assertEquals((missingWithInventory \ "axislessStructuralAnchorUnmappedLabelCounts").as[play.api.libs.json.JsObject].value.size, 0)
    assertEquals(
      (missingWithInventory \ "structuralAxisMissingCandidateWithAnyPacketAxislessAnchorComparisonIds").as[List[String]],
      List("cmp-structural-missing-axis")
    )

  test("plan technique anchor eligibility separates broad activity inventory from object-bound reroute candidates"):
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
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(
            evidenceRef("structural-delta:activity-axisless", root),
            StructuralDeltaEvidence(
              transition = transition,
              signals = Nil,
              consequences = List(
                TransitionConsequence(TransitionConsequenceKind.MobilityGain, StructuralSignalPolarity.Gain, 2),
                TransitionConsequence(TransitionConsequenceKind.MobilityLoss, StructuralSignalPolarity.Loss, 2)
              )
            )
          )
        )
      )

    val summary = MoveReviewPhase3AuditRunner.planTechniqueAnchorEligibilityJson(Nil, graph)

    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitCandidateCounts" \ "PieceRerouteRoute").as[Int], 2)
    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitObjectBoundCandidateCounts").as[play.api.libs.json.JsObject].value.size, 0)
    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitBroadCandidateCounts" \ "PieceRerouteRoute").as[Int], 2)
    assertEquals(
      (summary \ "axislessStructuralAnchorPlanTechniqueUnitBroadLabels" \ "PieceRerouteRoute").as[List[String]],
      List("activity-gain", "activity-loss")
    )

  test("plan technique anchor eligibility does not borrow object subjects from a different structural consequence"):
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
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(
            evidenceRef("structural-delta:mixed-subjects", root),
            StructuralDeltaEvidence(
              transition = transition,
              signals = Nil,
              consequences = List(
                TransitionConsequence(TransitionConsequenceKind.PassedPawnProgress, StructuralSignalPolarity.Gain, 3),
                TransitionConsequence(
                  TransitionConsequenceKind.OutpostGain,
                  StructuralSignalPolarity.Gain,
                  3,
                  subjects = List("outpost:knight:e5")
                )
              )
            )
          )
        )
      )

    val summary = MoveReviewPhase3AuditRunner.planTechniqueAnchorEligibilityJson(Nil, graph)

    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitCandidateCounts" \ "StructuralTransformation").as[Int], 1)
    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitObjectBoundCandidateCounts").as[play.api.libs.json.JsObject].value.size, 0)
    assertEquals((summary \ "axislessStructuralAnchorPlanTechniqueUnitBroadCandidateCounts" \ "StructuralTransformation").as[Int], 1)
    assertEquals(
      (summary \ "axislessStructuralAnchorPlanTechniqueUnitBroadLabels" \ "StructuralTransformation").as[List[String]],
      List("passed-pawn-progress")
    )

  test("semantic rubric funnel strict lineage does not treat row-level cause ownership as detail-owned"):
    def diagnosticWithDetailTokens(
        id: String,
        detailTokens: List[String]
    ): CandidateComparisonDiagnostic =
      comparisonDiagnostic(
        id = id,
        referenceLeadAxes = List("PawnBreak:Support:central-break-timing"),
        producedKinds = List(RelativeCauseKind.PawnBreakOpportunity),
        flows = List(
          causeFlow(
            causeId = "cause-central-break",
            kind = RelativeCauseKind.PawnBreakOpportunity,
            proofAxisKeys = List("PawnBreak:Support:central-break-timing"),
            claimIds = List("claim-central-break")
          )
        ),
        primaryRootKinds = List(RelativeCauseKind.PawnBreakOpportunity),
        primaryRootIds = List("cause-central-break"),
        positionPlanTechniqueFrameIds = List(s"frame-$id"),
        positionPlanTechniqueUnits = List(PositionPlanTechniqueUnit.TensionBreakPolicyRoute),
        positionPlanTechniqueAxisKeys = List("PawnBreak:Support:central-break-timing"),
        positionPlanTechniqueSemanticDetailUnits = List(PositionPlanTechniqueUnit.TensionBreakPolicyRoute),
        positionPlanTechniqueSemanticDetailAxisKeys = List("PawnBreak:Support:central-break-timing"),
        positionPlanTechniqueSemanticDetailTokens = detailTokens,
        positionPlanTechniqueSemanticDetailTokenGroups = List(detailTokens),
        positionPlanTechniqueObjectBindingSignatures = List("target=Pawn:e4|mechanism=Mechanism:pawn-break|proof=DirectProof"),
        positionPlanTechniqueRelativeCauseEvidenceIds = List("cause-central-break"),
        primaryRootArbitrationTiers = List(MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot)
      )
    val borrowed =
      diagnosticWithDetailTokens("borrowed-row-cause", List("pawnBreakReady:true", "objectTarget:Pawn:e4"))
    val coalesced =
      diagnosticWithDetailTokens(
        "coalesced-row-cause",
        List("pawnBreakReady:true", "objectTarget:Pawn:e4", "causeEvidenceId:cause-central-break")
      )

    val borrowedFunnel = MoveReviewPhase3AuditRunner.semanticRubricFunnelJson(List(borrowed))
    val coalescedFunnel = MoveReviewPhase3AuditRunner.semanticRubricFunnelJson(List(coalesced))

    assertEquals((borrowedFunnel \ "looseStageCounts" \ "clustered_coherent").as[Int], 1)
    assertEquals((borrowedFunnel \ "stageCounts" \ "clustered_coherent").as[Int], 0)
    assertEquals((borrowedFunnel \ "strictLineageStageCounts" \ "clustered_coherent").as[Int], 0)
    assertEquals((borrowedFunnel \ "strictLineageTerminalStageCounts" \ "exact_axis_or_pattern").as[Int], 1)
    assertEquals((borrowedFunnel \ "strictCauseLineageBoundCount").as[Int], 0)
    assertEquals((coalescedFunnel \ "strictLineageStageCounts" \ "clustered_coherent").as[Int], 1)
    assertEquals((coalescedFunnel \ "strictCauseLineageBoundCount").as[Int], 1)

  test("semantic rubric does not treat a frame id alone as object-bound"):
    val viewOnlyContext =
      comparisonDiagnostic(
        id = "cmp-view-only-context",
        referenceLeadAxes = Nil,
        producedKinds = Nil,
        flows = Nil,
        primaryRootKinds = Nil,
        primaryRootIds = Nil,
        positionPlanTechniqueFrameIds = List("frame-root-context"),
        positionPlanTechniqueUnits = List(PositionPlanTechniqueUnit.PlanOptionSet)
      )
    val slot =
      MoveReviewPhase3AuditRunner.ExpectedSemanticSlot(
        id = "view-only-plan-context",
        unit = PositionPlanTechniqueUnit.PlanOptionSet
      )

    val coverage = MoveReviewPhase3AuditRunner.semanticRubricExpectedSlotCoverageJson(List(slot), List(viewOnlyContext))

    assertEquals((coverage \ "slots" \ 0 \ "objectBound").as[Boolean], false)
    assertEquals((coverage \ "slots" \ 0 \ "terminalStage").as[String], "missing_semantic_slot")

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
          ),
          rootArbitrationTier = MoveJudgmentCauseRootArbitrationTier.FallbackRoot
        ),
        causeFrame(
          causeId = "cause-context-only",
          axisKeys = List("Activity:Gain:outpost-gain"),
          objectSignatures = List(
            "target=Square:e5|mechanism=Mechanism:activity|consequence=Consequence:activity|proof=ContextSupport"
          ),
          rootArbitrationTier = MoveJudgmentCauseRootArbitrationTier.ContextOnly
        ),
        causeFrame(
          causeId = "cause-outpost-direct",
          axisKeys = List("Activity:Gain:outpost-gain"),
          objectSignatures = List(
            "target=Square:d5|mechanism=Mechanism:activity|consequence=Consequence:activity|proof=DirectProof"
          ),
          rootArbitrationTier = MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot
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
    assertEquals((audit \ "rootArbitrationTierCounts" \ "ExactOwnedRoot").as[Int], 1)
    assertEquals((audit \ "rootArbitrationTierCounts" \ "FallbackRoot").as[Int], 1)
    assertEquals((audit \ "rootArbitrationTierCounts" \ "ContextOnly").as[Int], 2)

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
      witnessBindingLevel: MoveJudgmentCauseWitnessBindingLevel = MoveJudgmentCauseWitnessBindingLevel.NotWitness,
      rootArbitrationTier: MoveJudgmentCauseRootArbitrationTier = MoveJudgmentCauseRootArbitrationTier.ContextOnly
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
      rootArbitrationTier = rootArbitrationTier,
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
      primaryRootIds: List[String],
      positionPlanTechniqueFrameIds: List[String] = Nil,
      positionPlanTechniqueUnits: List[PositionPlanTechniqueUnit] = Nil,
      positionPlanTechniqueAxisKeys: List[String] = Nil,
      positionPlanTechniqueSemanticDetailUnits: List[PositionPlanTechniqueUnit] = Nil,
      positionPlanTechniqueSemanticDetailAxisKeys: List[String] = Nil,
      positionPlanTechniqueSemanticDetailMechanismKinds: List[StrategicMechanismKind] = Nil,
      positionPlanTechniqueSemanticDetailAnchorKeys: List[String] = Nil,
      positionPlanTechniqueSemanticDetailTokens: List[String] = Nil,
      positionPlanTechniqueSemanticDetailTokenGroups: List[List[String]] = Nil,
      positionPlanTechniqueObjectBindingSignatures: List[String] = Nil,
      positionPlanTechniqueEvidenceIds: List[String] = Nil,
      positionPlanTechniqueRelativeCauseEvidenceIds: List[String] = Nil,
      rootArbitrationTiers: List[MoveJudgmentCauseRootArbitrationTier] = Nil,
      primaryRootArbitrationTiers: List[MoveJudgmentCauseRootArbitrationTier] = Nil,
      primaryRootCauseEvidenceIdTierSignatures: List[String] = Nil,
      verdict: MoveChoiceVerdict = MoveChoiceVerdict.Mistake,
      relationKinds: List[RelationFactKind] = Nil
  ): CandidateComparisonDiagnostic =
    val axes = semanticAxes(referenceLeadAxes)
    val rootCauseEvidenceIdTierSignatures =
      if primaryRootCauseEvidenceIdTierSignatures.nonEmpty then primaryRootCauseEvidenceIdTierSignatures
      else
        primaryRootIds
          .zip(primaryRootArbitrationTiers)
          .map { case (causeEvidenceId, tier) => s"$causeEvidenceId|tier=$tier" }
    CandidateComparisonDiagnostic(
      id = id,
      comparisonFingerprint = id,
      dedupeKey = id,
      dedupeClass = CandidateComparisonDedupeClass.Unique,
      subjectBinding = SubjectBindingClass.PrimaryPlayedCause,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = lineDiagnostic(referenceLine),
      candidateLine = lineDiagnostic(candidateLine),
      verdict = verdict,
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
        rootArbitrationTiers = rootArbitrationTiers,
        primaryRootArbitrationTiers = primaryRootArbitrationTiers,
        primaryRootCauseEvidenceIdTierSignatures = rootCauseEvidenceIdTierSignatures,
        secondaryCauseEvidenceIds = Nil,
        contextCauseEvidenceIds = Nil,
        projectedContextCauseNoViewIds = Nil,
        playableLossPrimaryCauseEvidenceIds = Nil,
        objectlessPrimaryCauseEvidenceIds = Nil,
        objectlessSecondaryCauseEvidenceIds = Nil,
        objectlessContextCauseEvidenceIds = Nil,
        positionPlanTechniqueFrameIds = positionPlanTechniqueFrameIds,
        positionPlanTechniqueUnits = positionPlanTechniqueUnits,
        positionPlanTechniqueAxisKeys = positionPlanTechniqueAxisKeys,
        positionPlanTechniqueSemanticDetailUnits = positionPlanTechniqueSemanticDetailUnits,
        positionPlanTechniqueSemanticDetailAxisKeys = positionPlanTechniqueSemanticDetailAxisKeys,
        positionPlanTechniqueSemanticDetailMechanismKinds = positionPlanTechniqueSemanticDetailMechanismKinds,
        positionPlanTechniqueSemanticDetailAnchorKeys = positionPlanTechniqueSemanticDetailAnchorKeys,
        positionPlanTechniqueSemanticDetailTokens = positionPlanTechniqueSemanticDetailTokens,
        positionPlanTechniqueSemanticDetailTokenGroups = positionPlanTechniqueSemanticDetailTokenGroups,
        positionPlanTechniqueObjectBindingSignatures = positionPlanTechniqueObjectBindingSignatures,
        positionPlanTechniqueEvidenceIds = positionPlanTechniqueEvidenceIds,
        positionPlanTechniqueRelativeCauseEvidenceIds = positionPlanTechniqueRelativeCauseEvidenceIds
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
        referenceRelationKinds = relationKinds,
        candidateRelationKinds = relationKinds,
        relationKinds = relationKinds,
        referenceMotifs = Nil,
        candidateMotifs = Nil
      ),
      tacticalLossTrace = TacticalLossTrace(false, 0.0, None, None, Nil),
      failureClass = CandidateComparisonFailureClass.NoFailure,
      failureReasons = Nil
    )
