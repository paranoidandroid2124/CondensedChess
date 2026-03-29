package lila.llm.tools

import java.nio.file.Paths

import munit.FunSuite
import play.api.libs.json.Json

class ChronicleActivePlannerSliceRunnerTest extends FunSuite:

  test("SliceSurfaceEntry serializes planner owner trace fields") {
    val entry =
      ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
        sampleId = "g1:10",
        gameKey = "g1",
        mixBucket = "master_classical",
        sliceKind = "strategic_choice",
        targetPly = 10,
        playedSan = "Re1",
        momentPresent = true,
        authorQuestionKinds = List("WhyNow"),
        authorEvidenceKinds = List("WhyNow"),
        chronicleMode = "planner_owned",
        chroniclePrimaryKind = Some("WhyNow"),
        chronicleSecondaryKind = Some("WhatChanged"),
        chronicleNarrative = Some("This had to be done now before counterplay arrived."),
        chronicleBlankLike = false,
        activeMode = "attached",
        activePrimaryKind = Some("WhyThis"),
        activeSecondaryKind = None,
        activeFinalizationStage = Some("attached"),
        activeHardReasons = Nil,
        activeWarningReasons = Nil,
        activeNoteStatus = Some("rule"),
        activeNote = Some("This move keeps the queenside plan on track."),
        activeBlankLike = false,
        plannerSceneType = Some("forcing_defense"),
        plannerOwnerCandidates = List("ForcingDefense:threat:move_linked"),
        plannerAdmittedFamilies = List("ForcingDefense:threat:move_linked"),
        plannerDroppedFamilies = List("PlanRace:evidence_backed_plan:missing_certified_race_pair"),
        plannerSupportMaterialSeparation = List("close_candidate:support_material"),
        plannerProposedFamilyMappings = List("close_candidate->DecisionTiming:DecisionTiming/support_only"),
        plannerDemotionReasons = List("generic_urgency_only"),
        plannerSelectedQuestion = Some("WhyNow"),
        plannerSelectedOwnerFamily = Some("ForcingDefense"),
        plannerSelectedOwnerSource = Some("threat"),
        chronicleSurfaceReplayOutcome = Some("planner_owned"),
        activeSurfaceReplayOutcome = Some("attached")
      )

    val js = Json.toJson(entry)

    assertEquals((js \ "plannerSceneType").asOpt[String], Some("forcing_defense"))
    assertEquals((js \ "plannerSelectedOwnerFamily").asOpt[String], Some("ForcingDefense"))
    assertEquals((js \ "plannerSupportMaterialSeparation").asOpt[List[String]], Some(List("close_candidate:support_material")))
    assertEquals((js \ "chronicleSurfaceReplayOutcome").asOpt[String], Some("planner_owned"))
  }

  test("buildSummary separates planner-owned chronicle, factual fallback, and active omit modes") {
    val config =
      ChronicleActivePlannerSliceRunner.Config(
        manifestPath = Paths.get("slice_manifest.jsonl"),
        entriesPath = Paths.get("entries.jsonl"),
        jsonPath = Paths.get("summary.json"),
        markdownPath = Paths.get("summary.md"),
        anomalyPath = Paths.get("anomalies.jsonl"),
        depth = 8,
        multiPv = 2,
        perBucketGames = 2,
        maxGames = Some(2),
        enginePath = Paths.get("stockfish")
      )
    val entries =
      List(
        ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
          sampleId = "g1:10",
          gameKey = "g1",
          mixBucket = "master_classical",
          sliceKind = "strategic_choice",
          targetPly = 10,
          playedSan = "Re1",
          momentPresent = true,
          authorQuestionKinds = List("WhyNow"),
          authorEvidenceKinds = List("WhyNow"),
          chronicleMode = "planner_owned",
          chroniclePrimaryKind = Some("WhyNow"),
          chronicleSecondaryKind = Some("WhatChanged"),
          chronicleNarrative = Some("This had to be done now before counterplay arrived."),
          chronicleBlankLike = false,
          activeMode = "attached",
          activePrimaryKind = Some("WhyThis"),
          activeSecondaryKind = None,
          activeFinalizationStage = Some("attached"),
          activeHardReasons = Nil,
          activeWarningReasons = Nil,
          activeNoteStatus = Some("rule"),
          activeNote = Some("This move keeps the queenside plan on track."),
          activeBlankLike = false
        ),
        ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
          sampleId = "g1:20",
          gameKey = "g1",
          mixBucket = "master_classical",
          sliceKind = "prophylaxis",
          targetPly = 20,
          playedSan = "h3",
          momentPresent = true,
          authorQuestionKinds = List("WhatMustBeStopped"),
          authorEvidenceKinds = List("WhatMustBeStopped"),
          chronicleMode = "factual_fallback",
          chroniclePrimaryKind = None,
          chronicleSecondaryKind = None,
          chronicleNarrative = Some("This stops the immediate tactic."),
          chronicleBlankLike = true,
          activeMode = "omitted_no_primary",
          activePrimaryKind = None,
          activeSecondaryKind = None,
          activeFinalizationStage = Some("no_primary"),
          activeHardReasons = Nil,
          activeWarningReasons = Nil,
          activeNoteStatus = Some("omitted"),
          activeNote = None,
          activeBlankLike = false
        ),
        ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
          sampleId = "g2:30",
          gameKey = "g2",
          mixBucket = "club",
          sliceKind = "tactical_turn",
          targetPly = 30,
          playedSan = "Qh5+",
          momentPresent = false,
          authorQuestionKinds = List("WhyNow"),
          authorEvidenceKinds = List("WhyNow"),
          chronicleMode = "omitted",
          chroniclePrimaryKind = None,
          chronicleSecondaryKind = None,
          chronicleNarrative = None,
          chronicleBlankLike = false,
          activeMode = "omitted_after_primary",
          activePrimaryKind = Some("WhyNow"),
          activeSecondaryKind = None,
          activeFinalizationStage = Some("validator_failed"),
          activeHardReasons = List("active_note_rule_failed"),
          activeWarningReasons = List("active_note_sentence_count"),
          activeNoteStatus = Some("omitted"),
          activeNote = None,
          activeBlankLike = false
        )
      )

    val visibleMoments =
      List(
        ChronicleActivePlannerSliceRunner.MomentSurfaceState(
          gameKey = "g1",
          mixBucket = "master_classical",
          ply = 10,
          chronicleMode = "planner_owned",
          chroniclePrimaryKind = Some("WhyNow"),
          chronicleBlankLike = false,
          activeMode = "attached",
          activePrimaryKind = Some("WhyThis"),
          activeBlankLike = false,
          activeFinalizationStage = Some("attached"),
          activeHardReasons = Nil,
          activeWarningReasons = Nil,
          authorQuestionKinds = List("WhyNow"),
          authorEvidenceKinds = List("WhyNow"),
          authorEvidenceStatuses = List("resolved"),
          rejectedKinds = Nil,
          rejectedReasons = Nil,
          activeNoteStatus = Some("rule"),
          activeNote = Some("This move keeps the queenside plan on track."),
          chronicleNarrative = Some("This had to be done now before counterplay arrived.")
        ),
        ChronicleActivePlannerSliceRunner.MomentSurfaceState(
          gameKey = "g1",
          mixBucket = "master_classical",
          ply = 20,
          chronicleMode = "factual_fallback",
          chroniclePrimaryKind = None,
          chronicleBlankLike = true,
          activeMode = "omitted_no_primary",
          activePrimaryKind = None,
          activeBlankLike = false,
          activeFinalizationStage = Some("no_primary"),
          activeHardReasons = Nil,
          activeWarningReasons = Nil,
          authorQuestionKinds = Nil,
          authorEvidenceKinds = Nil,
          authorEvidenceStatuses = Nil,
          rejectedKinds = List("WhatMustBeStopped"),
          rejectedReasons = List("generic_urgency_only"),
          activeNoteStatus = Some("omitted"),
          activeNote = None,
          chronicleNarrative = Some("This stops the immediate tactic.")
        )
      )

    val summary = ChronicleActivePlannerSliceRunner.buildSummary(entries, visibleMoments, config)

    assertEquals(summary.selectedGames, 2)
    assertEquals(summary.totalTargets, 3)

    assertEquals(summary.chronicle.plannerOwnedCount, 1)
    assertEquals(summary.chronicle.factualFallbackCount, 1)
    assertEquals(summary.chronicle.omittedCount, 1)
    assertEquals(summary.chronicle.surfacedKinds.get("WhyNow"), Some(1))
    assertEquals(summary.chronicle.visibleMomentCount, 2)
    assertEquals(summary.chronicle.visiblePlannerOwnedCount, 1)
    assertEquals(summary.chronicle.visibleKinds.get("WhyNow"), Some(1))

    assertEquals(summary.active.plannerApprovedCount, 2)
    assertEquals(summary.active.attachedCount, 1)
    assertEquals(summary.active.omittedNoPrimaryCount, 1)
    assertEquals(summary.active.omittedAfterPrimaryCount, 1)
    assertEquals(summary.active.approvedKinds.get("WhyThis"), Some(1))
    assertEquals(summary.active.approvedKinds.get("WhyNow"), Some(1))
    assertEquals(summary.active.attachedKinds.get("WhyThis"), Some(1))
    assertEquals(summary.active.visibleMomentCount, 2)
    assertEquals(summary.active.visiblePlannerApprovedCount, 1)
    assertEquals(summary.active.visibleAttachedCount, 1)
    assertEquals(summary.active.visibleAttachedKinds.get("WhyThis"), Some(1))
    assertEquals(summary.active.approvedButNotAttachedStages.get("validator_failed"), Some(1))
    assertEquals(summary.active.approvedButNotAttachedHardReasons.get("active_note_rule_failed"), Some(1))
    assertEquals(summary.active.approvedButNotAttachedWarningReasons.get("active_note_sentence_count"), Some(1))

    assertEquals(summary.questionCoverage("WhyNow").targetCarriedCount, 2)
    assertEquals(summary.questionCoverage("WhyNow").targetChroniclePlannerOwnedCount, 1)
    assertEquals(summary.questionCoverage("WhyNow").visibleChroniclePlannerOwnedCount, 1)
    assertEquals(summary.questionCoverage("WhatMustBeStopped").targetChronicleFailClosedCount, 1)
    assertEquals(summary.targetSliceCoverage.get("question_why_now"), Some(
      ChronicleActivePlannerSliceRunner.TargetSliceCoverage(
        targetCount = 0,
        carriedCount = 0,
        chroniclePlannerOwnedCount = 0,
        activeApprovedCount = 0,
        activeAttachedCount = 0
      )
    ))
    assertEquals(summary.visibleRejectedReasons.get("generic_urgency_only"), Some(1))
  }
