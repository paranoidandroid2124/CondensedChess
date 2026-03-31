package lila.llm.tools.quality

import java.nio.file.{ Files, Path }

import scala.jdk.CollectionConverters.*

import munit.FunSuite
import play.api.libs.json.{ JsObject, Json }
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

class CommentaryQualityQuietSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualityQuietSupport.*

  private def withTempDir[A](run: Path => A): A =
    val dir = Files.createTempDirectory("quality-quiet-support-")
    try run(dir)
    finally
      Files
        .walk(dir)
        .iterator()
        .asScala
        .toList
        .sortBy(_.getNameCount)
        .reverse
        .foreach(path => Files.deleteIfExists(path))

  private def writeRawPayload(
      dir: Path,
      fileName: String,
      topSan: String,
      topScoreCp: Int,
      secondSan: String,
      secondScoreCp: Int,
      concepts: List[String] = Nil,
      signalDigest: JsObject = Json.obj()
  ): String =
    val raw =
      Json.obj(
        "concepts" -> concepts,
        "variations" -> Json.arr(
          Json.obj(
            "scoreCp" -> topScoreCp,
            "parsedMoves" ->
              Json.arr(
                Json.obj(
                  "san" -> topSan,
                  "isCapture" -> topSan.contains("x"),
                  "givesCheck" -> topSan.contains("+")
                )
              )
          ),
          Json.obj(
            "scoreCp" -> secondScoreCp,
            "parsedMoves" ->
              Json.arr(
                Json.obj(
                  "san" -> secondSan,
                  "isCapture" -> secondSan.contains("x"),
                  "givesCheck" -> secondSan.contains("+")
                )
              )
          )
        ),
        "signalDigest" -> signalDigest
      )
    val path = dir.resolve(s"$fileName.json")
    Files.writeString(path, Json.prettyPrint(raw))
    path.toString

  private def bookmakerEntry(
      sampleId: String,
      sliceKind: String,
      playedSan: String,
      rawResponsePath: String,
      commentary: String = "",
      bookmakerFallbackMode: String = "planner_owned",
      plannerSelectedQuestion: Option[String] = Some("WhyThis"),
      plannerSceneType: Option[String] = Some("quiet_improvement"),
      plannerSelectedOwnerFamily: Option[String] = None,
      plannerSelectedOwnerSource: Option[String] = Some("truth_contract"),
      plannerOwnerCandidates: List[String] = Nil,
      plannerAdmittedFamilies: List[String] = Nil,
      plannerProposedFamilyMappings: List[String] = Nil,
      rawChoiceType: Option[String] = None,
      rawDecisionPresent: Option[Boolean] = None,
      rawDecisionIngressReason: Option[String] = None,
      rawPvDeltaAvailable: Option[Boolean] = None,
      rawPvDeltaIngressReason: Option[String] = None,
      rawPvDeltaResolvedThreatsPresent: Option[Boolean] = None,
      rawPvDeltaNewOpportunitiesPresent: Option[Boolean] = None,
      rawPvDeltaPlanAdvancementsPresent: Option[Boolean] = None,
      rawPvDeltaConcessionsPresent: Option[Boolean] = None,
      sanitizedDecisionPresent: Option[Boolean] = None,
      sanitizedDecisionIngressReason: Option[String] = None,
      sanitizedPvDeltaAvailable: Option[Boolean] = None,
      sanitizedPvDeltaIngressReason: Option[String] = None,
      truthClass: Option[String] = None,
      truthReasonFamily: Option[String] = None,
      truthFailureMode: Option[String] = None,
      truthChosenMatchesBest: Option[Boolean] = None,
      truthOnlyMoveDefense: Option[Boolean] = None,
      truthBenchmarkCriticalMove: Option[Boolean] = None,
      plannerTacticalFailureSources: List[String] = Nil,
      plannerForcingDefenseSources: List[String] = Nil,
      plannerMoveDeltaSources: List[String] = Nil,
      quietSupportLiftApplied: Option[Boolean] = None,
      quietSupportRejectReasons: List[String] = Nil,
      runtimeGatePassed: Option[Boolean] = None,
      runtimeGateRejectReasons: List[String] = Nil,
      runtimeSceneType: Option[String] = Some("quiet_improvement"),
      runtimeSelectedOwnerFamily: Option[String] = Some("MoveDelta"),
      runtimeSelectedOwnerSource: Option[String] = Some("pv_delta"),
      runtimePvDeltaAvailable: Option[Boolean] = Some(true),
      runtimeSignalDigestAvailable: Option[Boolean] = Some(true),
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = Some(true),
      quietSupportCandidateBucket: Option[String] = None,
      quietSupportCandidateSourceKinds: List[String] = Nil,
      quietSupportCandidateVerbFamily: Option[String] = None,
      quietSupportCandidateText: Option[String] = None
  ): BookmakerOutputEntry =
    BookmakerOutputEntry(
      sampleId = sampleId,
      gameKey = sampleId.takeWhile(_ != ':'),
      sliceKind = sliceKind,
      targetPly = 42,
      fen = "fen",
      playedSan = playedSan,
      playedUci = "a2a3",
      opening = None,
      commentary = Option(commentary).filter(_.nonEmpty).getOrElse(s"$playedSan keeps the position under control."),
      supportRows = Nil,
      advancedRows = Nil,
      sourceMode = "rule",
      model = None,
      rawResponsePath = rawResponsePath,
      variationCount = 2,
      cacheHit = false,
      plannerPrimaryKind = plannerSelectedQuestion,
      plannerPrimaryFallbackMode = Some("PlannerOwned"),
      plannerSecondaryKind = None,
      plannerSecondarySurfaced = false,
      bookmakerFallbackMode = bookmakerFallbackMode,
      plannerSceneType = plannerSceneType,
      plannerOwnerCandidates = plannerOwnerCandidates,
      plannerAdmittedFamilies = plannerAdmittedFamilies,
      plannerDroppedFamilies = Nil,
      plannerSupportMaterialSeparation = Nil,
      plannerProposedFamilyMappings = plannerProposedFamilyMappings,
      plannerDemotionReasons = Nil,
      plannerSelectedQuestion = plannerSelectedQuestion,
      plannerSelectedOwnerFamily = plannerSelectedOwnerFamily,
      plannerSelectedOwnerSource = plannerSelectedOwnerSource,
      rawChoiceType = rawChoiceType,
      rawDecisionPresent = rawDecisionPresent,
      rawDecisionIngressReason = rawDecisionIngressReason,
      rawPvDeltaAvailable = rawPvDeltaAvailable,
      rawPvDeltaIngressReason = rawPvDeltaIngressReason,
      rawPvDeltaResolvedThreatsPresent = rawPvDeltaResolvedThreatsPresent,
      rawPvDeltaNewOpportunitiesPresent = rawPvDeltaNewOpportunitiesPresent,
      rawPvDeltaPlanAdvancementsPresent = rawPvDeltaPlanAdvancementsPresent,
      rawPvDeltaConcessionsPresent = rawPvDeltaConcessionsPresent,
      sanitizedDecisionPresent = sanitizedDecisionPresent,
      sanitizedDecisionIngressReason = sanitizedDecisionIngressReason,
      sanitizedPvDeltaAvailable = sanitizedPvDeltaAvailable,
      sanitizedPvDeltaIngressReason = sanitizedPvDeltaIngressReason,
      truthClass = truthClass,
      truthReasonFamily = truthReasonFamily,
      truthFailureMode = truthFailureMode,
      truthChosenMatchesBest = truthChosenMatchesBest,
      truthOnlyMoveDefense = truthOnlyMoveDefense,
      truthBenchmarkCriticalMove = truthBenchmarkCriticalMove,
      plannerTacticalFailureSources = plannerTacticalFailureSources,
      plannerForcingDefenseSources = plannerForcingDefenseSources,
      plannerMoveDeltaSources = plannerMoveDeltaSources,
      surfaceReplayOutcome =
        Some(if bookmakerFallbackMode == "planner_owned" then "bookmaker_planner_owned" else "bookmaker_exact_factual"),
      quietSupportLiftApplied = quietSupportLiftApplied,
      quietSupportRejectReasons = quietSupportRejectReasons,
      quietSupportRuntimeGatePassed = runtimeGatePassed,
      quietSupportRuntimeGateRejectReasons = runtimeGateRejectReasons,
      quietSupportRuntimeSceneType = runtimeSceneType,
      quietSupportRuntimeSelectedOwnerFamily = runtimeSelectedOwnerFamily,
      quietSupportRuntimeSelectedOwnerSource = runtimeSelectedOwnerSource,
      quietSupportRuntimePvDeltaAvailable = runtimePvDeltaAvailable,
      quietSupportRuntimeSignalDigestAvailable = runtimeSignalDigestAvailable,
      quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable = runtimeMoveLinkedPvDeltaAnchorAvailable,
      quietSupportCandidateBucket = quietSupportCandidateBucket,
      quietSupportCandidateSourceKinds = quietSupportCandidateSourceKinds,
      quietSupportCandidateVerbFamily = quietSupportCandidateVerbFamily,
      quietSupportCandidateText = quietSupportCandidateText
    )

  private def chronicleEntry(
      sampleId: String,
      sliceKind: String,
      playedSan: String,
      chronicleNarrative: Option[String] = None,
      plannerSelectedQuestion: Option[String] = Some("WhatChanged"),
      plannerSelectedOwnerFamily: Option[String] = Some("MoveDelta"),
      plannerSelectedOwnerSource: Option[String] = Some("pv_delta"),
      chronicleReplayPrimaryKind: Option[String] = Some("WhatChanged"),
      chronicleReplaySelectedOwnerFamily: Option[String] = Some("MoveDelta"),
      chronicleReplaySelectedOwnerSource: Option[String] = Some("pv_delta"),
      chronicleReplayNarrative: Option[String] = None,
      chronicleReplayQuietSupport: ChronicleQuietSupportTrace = ChronicleQuietSupportTrace()
  ): ChronicleActivePlannerSliceRunner.SliceSurfaceEntry =
    ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
      sampleId = sampleId,
      gameKey = sampleId.takeWhile(_ != ':'),
      mixBucket = "club",
      sliceKind = sliceKind,
      targetPly = 42,
      playedSan = playedSan,
      momentPresent = chronicleNarrative.nonEmpty,
      authorQuestionKinds = Nil,
      authorEvidenceKinds = Nil,
      chronicleMode = "planner_owned",
      chroniclePrimaryKind = chronicleReplayPrimaryKind,
      chronicleSecondaryKind = None,
      chronicleSelectedOwnerFamily = plannerSelectedOwnerFamily,
      chronicleSelectedOwnerSource = plannerSelectedOwnerSource,
      chronicleNarrative = chronicleNarrative,
      chronicleBlankLike = false,
      activeMode = "omitted_no_moment",
      activePrimaryKind = None,
      activeSecondaryKind = None,
      activeSelectedOwnerFamily = None,
      activeSelectedOwnerSource = None,
      activePlannerApproved = false,
      activeNoteBuilt = false,
      activeValidatorPassed = false,
      activeFinalizationStage = None,
      activeRejectReason = None,
      activeHardReasons = Nil,
      activeWarningReasons = Nil,
      activeNoteStatus = None,
      activeNote = None,
      activeNoteCandidate = None,
      activeBlankLike = false,
      plannerSelectedQuestion = plannerSelectedQuestion,
      plannerSelectedOwnerFamily = plannerSelectedOwnerFamily,
      plannerSelectedOwnerSource = plannerSelectedOwnerSource,
      chronicleReplayMode = "planner_owned",
      chronicleReplayPrimaryKind = chronicleReplayPrimaryKind,
      chronicleReplaySelectedOwnerFamily = chronicleReplaySelectedOwnerFamily,
      chronicleReplaySelectedOwnerSource = chronicleReplaySelectedOwnerSource,
      chronicleReplayNarrative = chronicleReplayNarrative,
      chronicleReplayBlankLike = false,
      chronicleReplayQuietSupport = chronicleReplayQuietSupport
    )

  test("opening deviation bucket stays upstream-blocked without opening translator") {
    withTempDir { dir =>
      val rawPath = writeRawPayload(dir, "opening-blocked", "h3", 22, "a3", 18)
      val report =
        buildQuietRichReport(
          bookmakerEntries =
            List(
              bookmakerEntry(
                sampleId = "g-opening:opening_transition:42:bookmaker",
                sliceKind = SliceKind.OpeningTransition,
                playedSan = "h3",
                rawResponsePath = rawPath
              )
            ),
          realShardSource = dir.toString
        )

      val row =
        report.rows.find(_.bucket == Bucket.OpeningDeviationAfterStableDevelopment)
          .getOrElse(fail("missing opening deviation row"))

      assertEquals(row.status, Status.UpstreamBlocked, clues(row))
      assert(row.statusReasons.contains("opening_relation_translator_missing"), clues(row))
      assertEquals(row.lane, Lane.Blocked, clues(row))
    }
  }

  test("long structural squeeze row is eligible when quietness and move-linked support both exist") {
    withTempDir { dir =>
      val rawPath =
        writeRawPayload(
          dir,
          "long-eligible",
          "Rc8",
          34,
          "Kh7",
          16,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("Rc8-c4"),
              "structuralCue" -> "queenside bind",
              "practicalVerdict" -> "pressure remains on the c-file"
            )
        )
      val report =
        buildQuietRichReport(
          bookmakerEntries =
            List(
              bookmakerEntry(
                sampleId = "g-long:long_structural_squeeze:42:bookmaker",
                sliceKind = SliceKind.LongStructuralSqueeze,
                playedSan = "Rc8",
                rawResponsePath = rawPath,
                plannerOwnerCandidates = List("source_kind=pv_delta")
              )
            ),
          realShardSource = dir.toString
        )

      val row =
        report.rows.find(_.bucket == Bucket.LongStructuralSqueeze)
          .getOrElse(fail("missing long structural squeeze row"))

      assertEquals(row.status, Status.Eligible, clues(row))
      assertEquals(row.directSources, List("MoveDelta.pv_delta"), clues(row))
      assert(row.supportSources.contains("Digest.route"), clues(row))
      assert(row.supportSources.contains("Digest.structure"), clues(row))
      assert(row.supportSources.contains("Digest.pressure"), clues(row))
      assertEquals(row.quietnessVerified, true, clues(row))
    }
  }

  test("closed forcing-defense family keeps a quiet row out of quiet-support eligibility") {
    withTempDir { dir =>
      val rawPath =
        writeRawPayload(
          dir,
          "closed-family",
          "Rc8",
          30,
          "Kh7",
          20,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("Rc8-c4"),
              "structuralCue" -> "queenside bind"
            )
        )
      val report =
        buildQuietRichReport(
          bookmakerEntries =
            List(
              bookmakerEntry(
                sampleId = "g-closed:long_structural_squeeze:42:bookmaker",
                sliceKind = SliceKind.LongStructuralSqueeze,
                playedSan = "Rc8",
                rawResponsePath = rawPath,
                plannerSelectedOwnerFamily = Some("ForcingDefense"),
                plannerOwnerCandidates = List("source_kind=pv_delta")
              )
            ),
          realShardSource = dir.toString
        )

      val row =
        report.rows.find(_.bucket == Bucket.LongStructuralSqueeze)
          .getOrElse(fail("missing blocked long structural squeeze row"))

      assertEquals(row.status, Status.UpstreamBlocked, clues(row))
      assert(row.statusReasons.contains("closed_owner_family_already_selected"), clues(row))
    }
  }

  test("prophylaxis concept alone is blocked without move-linked restriction digest") {
    withTempDir { dir =>
      val rawPath =
        writeRawPayload(
          dir,
          "prophylaxis-blocked",
          "a3",
          18,
          "h3",
          10,
          concepts = List("Prophylaxis against counterplay")
        )
      val report =
        buildQuietRichReport(
          bookmakerEntries =
            List(
              bookmakerEntry(
                sampleId = "g-prophylaxis:strategic_choice:42:bookmaker",
                sliceKind = SliceKind.StrategicChoice,
                playedSan = "a3",
                rawResponsePath = rawPath
              )
            ),
          realShardSource = dir.toString
        )

      val row =
        report.rows.find(_.bucket == Bucket.ProphylaxisRestraint)
          .getOrElse(fail("missing prophylaxis row"))

      assertEquals(row.status, Status.UpstreamBlocked, clues(row))
      assert(row.statusReasons.contains("move_linked_restriction_signal_missing"), clues(row))
      assertEquals(report.realShardOnly, true)
    }
  }

  test("bookmaker output entry reader tolerates legacy rows without upstream isolation fields") {
    withTempDir { dir =>
      val rawPath = writeRawPayload(dir, "legacy-schema", "Rc8", 24, "Kh7", 18)
      val legacyJson =
        Json.toJson(
          bookmakerEntry(
            sampleId = "g-legacy:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = rawPath
          )
        ).as[JsObject]
          - "rawChoiceType"
          - "rawDecisionPresent"
          - "rawPvDeltaAvailable"
          - "rawPvDeltaResolvedThreatsPresent"
          - "rawPvDeltaNewOpportunitiesPresent"
          - "rawPvDeltaPlanAdvancementsPresent"
          - "rawPvDeltaConcessionsPresent"
          - "sanitizedDecisionPresent"
          - "sanitizedPvDeltaAvailable"
          - "truthClass"
          - "truthReasonFamily"
          - "truthFailureMode"
          - "truthChosenMatchesBest"
          - "truthOnlyMoveDefense"
          - "truthBenchmarkCriticalMove"
          - "plannerTacticalFailureSources"
          - "plannerForcingDefenseSources"
          - "plannerMoveDeltaSources"

      val parsed = legacyJson.as[BookmakerOutputEntry]

      assertEquals(parsed.rawChoiceType, None, clues(parsed))
      assertEquals(parsed.rawDecisionPresent, None, clues(parsed))
      assertEquals(parsed.truthReasonFamily, None, clues(parsed))
      assertEquals(parsed.plannerMoveDeltaSources, Nil, clues(parsed))
    }
  }

  test("baseline selector locks the exact-factual eligible subset from the before artifact") {
    withTempDir { dir =>
      val eligibleRaw =
        writeRawPayload(
          dir,
          "phase-a-eligible",
          "Rc8",
          26,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("c8", "c4"),
              "structuralCue" -> "queenside bind"
            )
        )
      val blockedRaw =
        writeRawPayload(
          dir,
          "phase-a-blocked",
          "Rc8",
          24,
          "Kh7",
          20
        )
      val selectorRows =
        buildQuietSupportSelectorRows(
          beforeEntries =
            List(
              bookmakerEntry(
                sampleId = "g-phasea:long_structural_squeeze:42:bookmaker",
                sliceKind = SliceKind.LongStructuralSqueeze,
                playedSan = "Rc8",
                rawResponsePath = eligibleRaw,
                commentary = "This puts the rook on c8.",
                bookmakerFallbackMode = "exact_factual",
                plannerSelectedQuestion = None,
                plannerSelectedOwnerSource = None,
                plannerOwnerCandidates = List("source_kind=pv_delta")
              ),
              bookmakerEntry(
                sampleId = "g-blocked:long_structural_squeeze:43:bookmaker",
                sliceKind = SliceKind.LongStructuralSqueeze,
                playedSan = "Rc8",
                rawResponsePath = blockedRaw,
                commentary = "This puts the rook on c8.",
                bookmakerFallbackMode = "exact_factual",
                plannerSelectedQuestion = None,
                plannerSelectedOwnerSource = None,
                plannerOwnerCandidates = List("source_kind=pv_delta")
              ),
              bookmakerEntry(
                sampleId = "g-mismatch:long_structural_squeeze:44:bookmaker",
                sliceKind = SliceKind.LongStructuralSqueeze,
                playedSan = "Qa5",
                rawResponsePath = eligibleRaw,
                commentary = "This puts the queen on a5.",
                bookmakerFallbackMode = "exact_factual",
                plannerSceneType = Some("tactical_failure"),
                plannerSelectedQuestion = None,
                plannerSelectedOwnerSource = None,
                plannerOwnerCandidates = List("source_kind=pv_delta")
              )
            ),
          beforeSource = dir.toString
        )

      val eligibleRow =
        selectorRows.find(_.sampleId.startsWith("g-phasea")).getOrElse(fail("missing eligible selector row"))
      val blockedRow =
        selectorRows.find(_.sampleId.startsWith("g-blocked")).getOrElse(fail("missing blocked selector row"))
      val mismatchRow =
        selectorRows.find(_.sampleId.startsWith("g-mismatch")).getOrElse(fail("missing mismatch selector row"))

      assertEquals(eligibleRow.selected, true, clues(eligibleRow))
      assertEquals(eligibleRow.lane, Lane.Eligible, clues(eligibleRow))
      assert(eligibleRow.selectionReasons.contains("before_exact_factual"), clues(eligibleRow))
      assertEquals(blockedRow.selected, false, clues(blockedRow))
      assertEquals(blockedRow.lane, Lane.Blocked, clues(blockedRow))
      assertEquals(mismatchRow.selected, false, clues(mismatchRow))
      assert(mismatchRow.selectionReasons.exists(_.startsWith("before_scene_not_quiet:tactical_failure")), clues(mismatchRow))
    }
  }

  test("baseline evaluator compares before and after rows and detects stronger-verb leakage") {
    withTempDir { dir =>
      val eligibleRaw =
        writeRawPayload(
          dir,
          "phase-a-eligible-eval",
          "Rc8",
          28,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("c8", "c4"),
              "structuralCue" -> "queenside bind"
            )
        )
      val blockedRaw =
        writeRawPayload(
          dir,
          "phase-a-blocked-eval",
          "Rc8",
          22,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "structuralCue" -> "queenside bind"
            )
        )
      val beforeEntries =
        List(
          bookmakerEntry(
            sampleId = "g-phasea:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = eligibleRaw,
            commentary = "This puts the rook on c8.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta")
          ),
          bookmakerEntry(
            sampleId = "g-blocked:long_structural_squeeze:43:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = blockedRaw,
            commentary = "This puts the rook on c8.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None
          )
        )
      val afterEntries =
        List(
          bookmakerEntry(
            sampleId = "g-phasea:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = eligibleRaw,
            commentary = "This puts the rook on c8.\n\nThis keeps the route toward c4 available.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta"),
            quietSupportLiftApplied = Some(true),
            runtimeGatePassed = Some(true),
            quietSupportCandidateBucket = Some(Bucket.SlowRouteImprovement),
            quietSupportCandidateSourceKinds = List("MoveDelta.pv_delta", "Digest.route"),
            quietSupportCandidateVerbFamily = Some("keeps available"),
            quietSupportCandidateText = Some("This keeps the route toward c4 available.")
          ),
          bookmakerEntry(
            sampleId = "g-blocked:long_structural_squeeze:43:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = blockedRaw,
            commentary = "This puts the rook on c8.\n\nThis secures the squeeze.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons = List("move_linked_pv_delta_anchor_missing"),
            runtimeMoveLinkedPvDeltaAnchorAvailable = Some(false)
          )
        )

      val (selectorRows, evalRows, summary) =
        buildQuietSupportEvaluation(
          beforeEntries = beforeEntries,
          afterEntries = afterEntries,
          beforeSource = s"${dir.toString}/before",
          afterSource = s"${dir.toString}/after"
        )

      val eligibleEval =
        evalRows.find(_.sampleId.startsWith("g-phasea")).getOrElse(fail("missing eligible eval row"))
      val blockedEval =
        evalRows.find(_.sampleId.startsWith("g-blocked")).getOrElse(fail("missing blocked eval row"))

      assert(selectorRows.exists(row => row.sampleId.startsWith("g-phasea") && row.selected), clues(selectorRows))
      assert(eligibleEval.selectorDelta > 0, clues(eligibleEval))
      assertEquals(eligibleEval.changeFamily, QuietSupportLane.EligibleStable, clues(eligibleEval))
      assertEquals(eligibleEval.runtimeGateClassification, QuietSupportLane.RuntimeGatePass, clues(eligibleEval))
      assertEquals(eligibleEval.isolationClassification, IsolationCategory.RuntimeGatePass, clues(eligibleEval))
      assertEquals(eligibleEval.quietSupportLiftApplied, true, clues(eligibleEval))
      assertEquals(eligibleEval.quietSupportRejectReasons, Nil, clues(eligibleEval))
      assertEquals(eligibleEval.strongerVerbLeakage, false, clues(eligibleEval))
      assertEquals(blockedEval.runtimeGateClassification, Lane.Eligible, clues(blockedEval))
      assertEquals(blockedEval.strongerVerbLeakage, true, clues(blockedEval))
      assert(blockedEval.strongerVerbLeakageTerms.contains("secure"), clues(blockedEval))
      assert(summary.acceptanceNotes.nonEmpty, clues(summary))
    }
  }

  test("chronicle mirror summary stays replay-safe while counting quiet-support attaches") {
    withTempDir { dir =>
      val eligibleRaw =
        writeRawPayload(
          dir,
          "phase-b-eligible",
          "a5",
          24,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("a5", "a6"),
              "structuralCue" -> "fluid center",
              "practicalVerdict" -> "space remains under control"
            )
        )
      val blockedRaw =
        writeRawPayload(
          dir,
          "phase-b-blocked",
          "Ke7",
          28,
          "Kf8",
          20,
          signalDigest =
            Json.obj(
              "structuralCue" -> "center tension"
            )
        )
      val beforeEntries =
        List(
          bookmakerEntry(
            sampleId = "g-phaseb:long_structural_squeeze:68:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "a5",
            rawResponsePath = eligibleRaw,
            commentary = "This is a pawn move to a5.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta")
          ),
          bookmakerEntry(
            sampleId = "g-phaseb-blocked:long_structural_squeeze:69:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            rawResponsePath = blockedRaw,
            commentary = "This is a king move to e7.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("threat")
          )
        )
      val afterEntries =
        List(
          bookmakerEntry(
            sampleId = "g-phaseb:long_structural_squeeze:68:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "a5",
            rawResponsePath = eligibleRaw,
            commentary = "This is a pawn move to a5.\n\nThis reinforces the fluid center.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta"),
            quietSupportLiftApplied = Some(true),
            runtimeGatePassed = Some(true),
            runtimeSelectedOwnerFamily = Some("MoveDelta"),
            runtimeSelectedOwnerSource = Some("pv_delta"),
            quietSupportCandidateBucket = Some(Bucket.LongStructuralSqueeze),
            quietSupportCandidateSourceKinds = List("MoveDelta.pv_delta", "Digest.structure"),
            quietSupportCandidateVerbFamily = Some("reinforces"),
            quietSupportCandidateText = Some("This reinforces the fluid center.")
          ),
          bookmakerEntry(
            sampleId = "g-phaseb-blocked:long_structural_squeeze:69:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            rawResponsePath = blockedRaw,
            commentary = "This is a king move to e7.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("threat"),
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons = List("scene_type_not_allowed:forcing_defense")
          )
        )
      val beforeChronicleEntries =
        List(
          chronicleEntry(
            sampleId = "g-phaseb:long_structural_squeeze:68:chronicle",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "a5",
            chronicleNarrative = Some("This is a pawn move to a5."),
            plannerSelectedQuestion = Some("WhatChanged"),
            plannerSelectedOwnerFamily = Some("MoveDelta"),
            plannerSelectedOwnerSource = Some("pv_delta")
          ),
          chronicleEntry(
            sampleId = "g-phaseb-blocked:long_structural_squeeze:69:chronicle",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            chronicleNarrative = Some("The move has to happen now."),
            plannerSelectedQuestion = Some("WhyNow"),
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("threat"),
            chronicleReplayPrimaryKind = Some("WhyNow"),
            chronicleReplaySelectedOwnerFamily = Some("ForcingDefense"),
            chronicleReplaySelectedOwnerSource = Some("threat")
          )
        )
      val afterChronicleEntries =
        List(
          chronicleEntry(
            sampleId = "g-phaseb:long_structural_squeeze:68:chronicle",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "a5",
            chronicleNarrative = Some("This is a pawn move to a5."),
            plannerSelectedQuestion = Some("WhatChanged"),
            plannerSelectedOwnerFamily = Some("MoveDelta"),
            plannerSelectedOwnerSource = Some("pv_delta"),
            chronicleReplayNarrative = Some("This is a pawn move to a5. This reinforces the fluid center."),
            chronicleReplayQuietSupport =
              ChronicleQuietSupportTrace(
                applied = true,
                runtimeGatePassed = Some(true),
                runtimeSceneType = Some("quiet_improvement"),
                runtimeSelectedOwnerFamily = Some("MoveDelta"),
                runtimeSelectedOwnerSource = Some("pv_delta"),
                runtimePvDeltaAvailable = Some(true),
                runtimeSignalDigestAvailable = Some(true),
                runtimeMoveLinkedPvDeltaAnchorAvailable = Some(true),
                candidateBucket = Some(Bucket.LongStructuralSqueeze),
                candidateSourceKinds = List("MoveDelta.pv_delta", "Digest.structure"),
                candidateVerbFamily = Some("reinforces"),
                candidateText = Some("This reinforces the fluid center.")
              )
          ),
          chronicleEntry(
            sampleId = "g-phaseb-blocked:long_structural_squeeze:69:chronicle",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            chronicleNarrative = Some("The move has to happen now."),
            plannerSelectedQuestion = Some("WhyNow"),
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("threat"),
            chronicleReplayPrimaryKind = Some("WhyNow"),
            chronicleReplaySelectedOwnerFamily = Some("ForcingDefense"),
            chronicleReplaySelectedOwnerSource = Some("threat"),
            chronicleReplayNarrative = Some("The move has to happen now."),
            chronicleReplayQuietSupport =
              ChronicleQuietSupportTrace(
                applied = false,
                rejectReasons = List("surface_primary_not_movedelta_pv_delta"),
                runtimeGatePassed = Some(false),
                runtimeSceneType = Some("forcing_defense"),
                runtimeSelectedOwnerFamily = Some("ForcingDefense"),
                runtimeSelectedOwnerSource = Some("threat")
              )
          )
        )

      val (_, _, summary) =
        buildQuietSupportEvaluation(
          beforeEntries = beforeEntries,
          afterEntries = afterEntries,
          beforeSource = s"${dir.toString}/before-bookmaker",
          afterSource = s"${dir.toString}/after-bookmaker",
          beforeChronicleEntries = beforeChronicleEntries,
          afterChronicleEntries = afterChronicleEntries,
          beforeChronicleSource = Some(s"${dir.toString}/before-chronicle"),
          afterChronicleSource = Some(s"${dir.toString}/after-chronicle")
        )

      val chronicle = summary.chronicleMirror.getOrElse(fail("missing chronicle mirror summary"))

      assertEquals(chronicle.selectedQuietRowCount, 1, clues(chronicle))
      assertEquals(chronicle.quietSupportAppliedCount, 1, clues(chronicle))
      assertEquals(chronicle.ownerDivergenceCount, 0, clues(chronicle))
      assertEquals(chronicle.questionDivergenceCount, 0, clues(chronicle))
      assertEquals(chronicle.strongerVerbLeakageCount, 0, clues(chronicle))
      assertEquals(chronicle.blockedLaneContaminationCount, 0, clues(chronicle))
      assertEquals(chronicle.crossSurfaceOwnerDivergenceCount, 0, clues(chronicle))
      assert(chronicle.representatives.exists(_.chronicleSampleId.endsWith(":chronicle")), clues(chronicle))
    }
  }

  test("baseline evaluator splits stable quiet-support rows from planner drift rows") {
    withTempDir { dir =>
      val eligibleRaw =
        writeRawPayload(
          dir,
          "phase-a-stable",
          "Rc8",
          28,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("c8", "c4"),
              "structuralCue" -> "queenside bind"
            )
        )
      val driftRaw =
        writeRawPayload(
          dir,
          "phase-a-drift",
          "a5",
          24,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("a5", "a4"),
              "practicalVerdict" -> "pressure remains on the queenside"
            )
        )
      val beforeEntries =
        List(
          bookmakerEntry(
            sampleId = "g-stable:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = eligibleRaw,
            commentary = "This puts the rook on c8.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta")
          ),
          bookmakerEntry(
            sampleId = "g-drift:pressure_maintenance_without_immediate_tactic:43:bookmaker",
            sliceKind = SliceKind.EndgameConversion,
            playedSan = "a5",
            rawResponsePath = driftRaw,
            commentary = "This is a pawn move to a5.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta")
          )
        )
      val afterEntries =
        List(
          bookmakerEntry(
            sampleId = "g-stable:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = eligibleRaw,
            commentary = "This puts the rook on c8.\n\nThis keeps the route toward c4 available.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("source_kind=pv_delta"),
            quietSupportLiftApplied = Some(true),
            runtimeGatePassed = Some(true),
            quietSupportCandidateBucket = Some(Bucket.SlowRouteImprovement),
            quietSupportCandidateSourceKinds = List("MoveDelta.pv_delta", "Digest.route"),
            quietSupportCandidateVerbFamily = Some("keeps available"),
            quietSupportCandidateText = Some("This keeps the route toward c4 available.")
          ),
          bookmakerEntry(
            sampleId = "g-drift:pressure_maintenance_without_immediate_tactic:43:bookmaker",
            sliceKind = SliceKind.EndgameConversion,
            playedSan = "a5",
            rawResponsePath = driftRaw,
            commentary = "This forces the opponent to stay passive.",
            bookmakerFallbackMode = "planner_owned",
            plannerSelectedQuestion = Some("WhatMustBeStopped"),
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("threat"),
            plannerOwnerCandidates = List("source_kind=pv_delta"),
            quietSupportLiftApplied = Some(false),
            quietSupportRejectReasons = List("planner_owned_row"),
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons = List("scene_type_not_allowed:forcing_defense"),
            runtimeSceneType = Some("forcing_defense"),
            runtimeSelectedOwnerFamily = Some("ForcingDefense"),
            runtimeSelectedOwnerSource = Some("threat"),
            runtimeMoveLinkedPvDeltaAnchorAvailable = Some(false)
          )
        )

      val (_, evalRows, summary) =
        buildQuietSupportEvaluation(
          beforeEntries = beforeEntries,
          afterEntries = afterEntries,
          beforeSource = s"${dir.toString}/before",
          afterSource = s"${dir.toString}/after"
        )

      val stableEval =
        evalRows.find(_.sampleId.startsWith("g-stable")).getOrElse(fail("missing stable eval row"))
      val driftEval =
        evalRows.find(_.sampleId.startsWith("g-drift")).getOrElse(fail("missing drift eval row"))

      assertEquals(stableEval.changeFamily, QuietSupportLane.EligibleStable, clues(stableEval))
      assertEquals(stableEval.runtimeGateClassification, QuietSupportLane.RuntimeGatePass, clues(stableEval))
      assertEquals(stableEval.isolationClassification, IsolationCategory.RuntimeGatePass, clues(stableEval))
      assertEquals(stableEval.quietSupportLiftApplied, true, clues(stableEval))
      assertEquals(driftEval.changeFamily, QuietSupportLane.EligibleDrift, clues(driftEval))
      assertEquals(driftEval.runtimeGateClassification, QuietSupportLane.EligibleDrift, clues(driftEval))
      assertEquals(driftEval.isolationClassification, IsolationCategory.NonTargetDrift, clues(driftEval))
      assertEquals(driftEval.quietSupportLiftApplied, false, clues(driftEval))
      assert(summary.runtimeGatePassCount > 0, clues(summary))
      assert(summary.eligibleStableSelectedCount > 0, clues(summary))
      assert(summary.eligibleDriftSelectedCount > 0, clues(summary))
      assert(
        summary.acceptanceNotes.exists(note =>
          note.contains("baseline selected owner/question divergence count") && note.contains("[review]")
        ),
        clues(summary)
      )
    }
  }

  test("baseline isolation categories distinguish selector mismatch, ingress regression, and blocked fallback spike") {
    withTempDir { dir =>
      val selectorMismatchRaw =
        writeRawPayload(
          dir,
          "selector-mismatch",
          "Qa5",
          24,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("a5", "a7"),
              "structuralCue" -> "queenside bind"
            )
        )
      val ingressRegressionRaw =
        writeRawPayload(
          dir,
          "ingress-regression",
          "Rc8",
          28,
          "Kh7",
          18,
          signalDigest =
            Json.obj(
              "deploymentRoute" -> Json.arr("c8", "c4"),
              "structuralCue" -> "queenside bind"
            )
        )
      val blockedSpikeRaw =
        writeRawPayload(
          dir,
          "blocked-spike",
          "Ke7",
          22,
          "Kh7",
          18
        )
      val beforeEntries =
        List(
          bookmakerEntry(
            sampleId = "g-selector:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Qa5",
            rawResponsePath = selectorMismatchRaw,
            commentary = "This puts the queen on a5.",
            bookmakerFallbackMode = "exact_factual",
            plannerSceneType = Some("tactical_failure"),
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerMoveDeltaSources = Nil
          ),
          bookmakerEntry(
            sampleId = "g-ingress:long_structural_squeeze:43:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = ingressRegressionRaw,
            commentary = "This puts the rook on c8.",
            bookmakerFallbackMode = "exact_factual",
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List("MoveDelta:source_kind=pv_delta"),
            rawChoiceType = Some("NarrowChoice"),
            rawDecisionPresent = Some(true),
            rawDecisionIngressReason = Some("decision_present"),
            rawPvDeltaAvailable = Some(true),
            rawPvDeltaIngressReason = Some("pv_delta_present_with_content"),
            sanitizedDecisionPresent = Some(true),
            sanitizedDecisionIngressReason = Some("decision_present"),
            sanitizedPvDeltaAvailable = Some(true),
            sanitizedPvDeltaIngressReason = Some("pv_delta_present_with_content"),
            truthClass = Some("Best"),
            truthReasonFamily = Some("QuietTechnicalMove"),
            truthFailureMode = Some("NoClearPlan"),
            plannerMoveDeltaSources = List("pv_delta")
          ),
          bookmakerEntry(
            sampleId = "g-blocked:long_structural_squeeze:44:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            rawResponsePath = blockedSpikeRaw,
            commentary = "The timing matters now because Other moves allow the position to slip away.",
            bookmakerFallbackMode = "planner_owned",
            plannerSceneType = Some("forcing_defense"),
            plannerSelectedQuestion = Some("WhyNow"),
            plannerSelectedOwnerFamily = Some("ForcingDefense"),
            plannerSelectedOwnerSource = Some("truth_contract"),
            plannerForcingDefenseSources = List("truth_contract")
          )
        )
      val afterEntries =
        List(
          bookmakerEntry(
            sampleId = "g-selector:long_structural_squeeze:42:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Qa5",
            rawResponsePath = selectorMismatchRaw,
            commentary = "This puts the queen on a5.",
            bookmakerFallbackMode = "exact_factual",
            plannerSceneType = Some("tactical_failure"),
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons = List("scene_type_not_allowed:tactical_failure")
          ),
          bookmakerEntry(
            sampleId = "g-ingress:long_structural_squeeze:43:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Rc8",
            rawResponsePath = ingressRegressionRaw,
            commentary = "This puts the rook on c8.",
            bookmakerFallbackMode = "exact_factual",
            plannerSceneType = Some("tactical_failure"),
            plannerSelectedQuestion = None,
            plannerSelectedOwnerSource = None,
            plannerOwnerCandidates = List(
              "DecisionTiming:source_kind=truth_contract",
              "TacticalFailure:source_kind=truth_contract"
            ),
            rawChoiceType = Some("StyleChoice"),
            rawDecisionPresent = Some(false),
            rawDecisionIngressReason = Some("style_choice_decision_omitted"),
            rawPvDeltaAvailable = Some(false),
            rawPvDeltaIngressReason = Some("style_choice_pv_delta_unavailable"),
            rawPvDeltaResolvedThreatsPresent = Some(false),
            rawPvDeltaNewOpportunitiesPresent = Some(false),
            rawPvDeltaPlanAdvancementsPresent = Some(false),
            rawPvDeltaConcessionsPresent = Some(false),
            sanitizedDecisionPresent = Some(false),
            sanitizedDecisionIngressReason = Some("style_choice_decision_omitted"),
            sanitizedPvDeltaAvailable = Some(false),
            sanitizedPvDeltaIngressReason = Some("style_choice_pv_delta_unavailable"),
            truthClass = Some("Best"),
            truthReasonFamily = Some("TacticalRefutation"),
            truthFailureMode = Some("TacticalRefutation"),
            plannerTacticalFailureSources = List("truth_contract"),
            plannerMoveDeltaSources = Nil,
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons =
              List("pv_delta_missing", "scene_type_not_allowed:tactical_failure"),
            runtimeSceneType = Some("tactical_failure"),
            runtimePvDeltaAvailable = Some(false),
            runtimeMoveLinkedPvDeltaAnchorAvailable = Some(false)
          ),
          bookmakerEntry(
            sampleId = "g-blocked:long_structural_squeeze:44:bookmaker",
            sliceKind = SliceKind.LongStructuralSqueeze,
            playedSan = "Ke7",
            rawResponsePath = blockedSpikeRaw,
            commentary = "This puts the king on e7.",
            bookmakerFallbackMode = "exact_factual",
            plannerSceneType = Some("tactical_failure"),
            plannerSelectedQuestion = None,
            plannerSelectedOwnerFamily = None,
            plannerSelectedOwnerSource = None,
            rawChoiceType = Some("NarrowChoice"),
            rawDecisionPresent = Some(true),
            rawDecisionIngressReason = Some("decision_present"),
            rawPvDeltaAvailable = Some(true),
            rawPvDeltaIngressReason = Some("pv_delta_present_but_empty"),
            sanitizedDecisionPresent = Some(true),
            sanitizedDecisionIngressReason = Some("decision_present"),
            sanitizedPvDeltaAvailable = Some(true),
            sanitizedPvDeltaIngressReason = Some("pv_delta_present_but_empty"),
            truthReasonFamily = Some("TacticalRefutation"),
            truthFailureMode = Some("NoClearPlan"),
            plannerTacticalFailureSources = List("truth_contract"),
            runtimeGatePassed = Some(false),
            runtimeGateRejectReasons = List("scene_type_not_allowed:tactical_failure")
          )
        )

      val (_, evalRows, summary) =
        buildQuietSupportEvaluation(
          beforeEntries = beforeEntries,
          afterEntries = afterEntries,
          beforeSource = s"${dir.toString}/before",
          afterSource = s"${dir.toString}/after"
        )

      val selectorMismatchEval =
        evalRows.find(_.sampleId.startsWith("g-selector")).getOrElse(fail("missing selector mismatch row"))
      val ingressRegressionEval =
        evalRows.find(_.sampleId.startsWith("g-ingress")).getOrElse(fail("missing ingress regression row"))
      val blockedSpikeEval =
        evalRows.find(_.sampleId.startsWith("g-blocked")).getOrElse(fail("missing blocked fallback spike row"))

      assertEquals(selectorMismatchEval.isolationClassification, IsolationCategory.SelectorMismatch, clues(selectorMismatchEval))
      assertEquals(
        selectorMismatchEval.upstreamPrimarySubsystem,
        Some("QuietSupportSelector"),
        clues(selectorMismatchEval)
      )
      assertEquals(
        selectorMismatchEval.upstreamPrimaryCause,
        Some("baseline_selected_despite_preexisting_tactical_scene"),
        clues(selectorMismatchEval)
      )
      assertEquals(ingressRegressionEval.isolationClassification, IsolationCategory.IngressRegression, clues(ingressRegressionEval))
      assertEquals(ingressRegressionEval.beforeRawDecisionPresent, Some(true), clues(ingressRegressionEval))
      assertEquals(ingressRegressionEval.afterRawDecisionPresent, Some(false), clues(ingressRegressionEval))
      assertEquals(
        ingressRegressionEval.afterRawDecisionIngressReason,
        Some("style_choice_decision_omitted"),
        clues(ingressRegressionEval)
      )
      assertEquals(
        ingressRegressionEval.afterRawPvDeltaIngressReason,
        Some("style_choice_pv_delta_unavailable"),
        clues(ingressRegressionEval)
      )
      assertEquals(ingressRegressionEval.beforeTruthReasonFamily, Some("QuietTechnicalMove"), clues(ingressRegressionEval))
      assertEquals(ingressRegressionEval.afterTruthReasonFamily, Some("TacticalRefutation"), clues(ingressRegressionEval))
      assertEquals(ingressRegressionEval.beforePlannerMoveDeltaSources, List("pv_delta"), clues(ingressRegressionEval))
      assertEquals(ingressRegressionEval.afterPlannerMoveDeltaSources, Nil, clues(ingressRegressionEval))
      assertEquals(
        ingressRegressionEval.upstreamPrimarySubsystem,
        Some("NarrativeContextBuilder"),
        clues(ingressRegressionEval)
      )
      assertEquals(
        ingressRegressionEval.upstreamPrimaryCause,
        Some("style_choice_skipped_decision_and_pv_delta"),
        clues(ingressRegressionEval)
      )
      assertEquals(blockedSpikeEval.isolationClassification, IsolationCategory.BlockedFallbackSpike, clues(blockedSpikeEval))
      assertEquals(
        blockedSpikeEval.upstreamPrimarySubsystem,
        Some("DecisiveTruth"),
        clues(blockedSpikeEval)
      )
      assertEquals(
        blockedSpikeEval.upstreamPrimaryCause,
        Some("truth_contract_reclassified_blocked_row_as_tactical_failure"),
        clues(blockedSpikeEval)
      )
      assert(summary.selectorMismatchCount >= 1, clues(summary))
      assert(summary.ingressRegressionCount >= 1, clues(summary))
      assert(summary.blockedFallbackSpikeCount >= 1, clues(summary))
    }
  }

