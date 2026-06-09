package lila.commentary.tools.review

import play.api.libs.json.*

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import lila.commentary.*
import lila.commentary.analysis.{ BookStyleRenderer, MoveReviewCompressionPolicy, MoveReviewPolishSlotsBuilder, CommentaryEngine, DecisiveTruth, EarlyOpeningNarrationPolicy, LineScopedCitation, LiveNarrativeCompressionCore, NarrativeContextBuilder, NarrativeSignalDigestBuilder, NarrativeUtils, OpeningFamilyCatalog, QuestionFirstCommentaryPlanner, QuestionPlannerInputsBuilder, QuietMoveIntentBuilder, StrategyPackBuilder, UserFacingSignalSanitizer }
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility
import lila.commentary.analysis.render.QuietStrategicSupportComposer
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.model.NarrativeRenderMode
import lila.commentary.model.NarrativeContext
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.moveReview.MoveReviewCoverageDiagnostics

object CommentaryPlayerQcSupport:

  val ExternalRoot: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc")
  val DefaultRawPgnDir: Path = ExternalRoot.resolve("raw-pgn")
  val DefaultCatalogDir: Path = ExternalRoot.resolve("catalog")
  val DefaultManifestDir: Path = ExternalRoot.resolve("manifests")
  val DefaultMoveReviewRunDir: Path = ExternalRoot.resolve("runs").resolve("move_review")
  val DefaultReviewDir: Path = ExternalRoot.resolve("reviews")
  val DefaultReportDir: Path = ExternalRoot.resolve("reports")
  val DefaultTruthInventoryDir: Path = ExternalRoot.resolve("inventory")
  val DefaultTruthInventoryPath: Path =
    DefaultTruthInventoryDir.resolve("RealPgnNarrativeEvalTruthInventory.json")
  val TruthInventoryLookupPaths: List[Path] =
    List(DefaultTruthInventoryPath)
  val DefaultUciTimeoutMs: Long =
    sys.env.get("AI_ACTIVE_CORPUS_ENGINE_TIMEOUT_MS").flatMap(_.toLongOption).filter(_ > 0).getOrElse(60000L)

  object MixBucket:
    val MasterClassical = "master_classical"
    val TitledPractical = "titled_practical"
    val Club = "club"
    val EdgeCase = "edge_case"
    val all = List(MasterClassical, TitledPractical, Club, EdgeCase)

  object RatingBucket:
    val Elite = "elite"
    val Titled = "titled"
    val Club = "club"
    val Unknown = "unknown"

  object TimeControlBucket:
    val Classical = "classical"
    val Rapid = "rapid"
    val Blitz = "blitz"
    val Bullet = "bullet"
    val Correspondence = "correspondence"
    val Unknown = "unknown"

  object SliceKind:
    val OpeningTransition = "opening_transition"
    val StrategicChoice = "strategic_choice"
    val Prophylaxis = "prophylaxis"
    val ProphylaxisRestraint = "prophylaxis_restraint"
    val LongStructuralSqueeze = "long_structural_squeeze"
    val OpeningDeviationAfterMiddlegamePlanClash = "opening_deviation_after_middlegame_plan_clash"
    val TacticalTurn = "tactical_turn"
    val CompensationOrExchangeSac = "compensation_or_exchange_sac"
    val PracticalSimplification = "practical_simplification"
    val EndgameConversion = "endgame_conversion"
    val TransitionHeavyEndgames = "transition_heavy_endgames"
    val QuestionWhyNow = "question_why_now"
    val MoveReviewFocus = "move_review_focus"
    val sceneCoverageLaneKinds = Set(
      ProphylaxisRestraint,
      LongStructuralSqueeze,
      OpeningDeviationAfterMiddlegamePlanClash,
      TransitionHeavyEndgames
    )
    val all = List(
      OpeningTransition,
      StrategicChoice,
      Prophylaxis,
      ProphylaxisRestraint,
      LongStructuralSqueeze,
      OpeningDeviationAfterMiddlegamePlanClash,
      TacticalTurn,
      CompensationOrExchangeSac,
      PracticalSimplification,
      EndgameConversion,
      TransitionHeavyEndgames,
      QuestionWhyNow
    )

  object ReviewSeverity:
    val Blocker = "Blocker"
    val Major = "Major"
    val Minor = "Minor"
    val Good = "Good"
    val Excellent = "Excellent"

  object BlockerType:
    val FalseCurrentFact = "false_current_fact"
    val MetaLanguageLeak = "meta_language_leak"
    val MissingMovePurpose = "missing_move_purpose"
    val OverloadedMainClaim = "overloaded_main_claim"
    val BadCitation = "bad_citation"
    val SidecarDependency = "sidecar_dependency"
    val ToneNotPlayerFacing = "tone_not_player_facing"

  object ReviewSurface:
    val MoveReview = "moveReview"

  object ReviewKind:
    val MoveReviewFocus = "move_review_focus"

  object FixFamily:
    val GenericFillerMainProse = "generic_filler_main_prose"
    val AnchoredSupportMissingFromProse = "anchored_support_missing_from_prose"
    val ConditionalityBlur = "conditionality_blur"
    val MisanchoredConcreteClaim = "misanchored_concrete_claim"
    val StrategicFlattening = "strategic_flattening"

    val all = List(
      GenericFillerMainProse,
      AnchoredSupportMissingFromProse,
      ConditionalityBlur,
      MisanchoredConcreteClaim,
      StrategicFlattening
    )

  final case class CatalogEntry(
      gameKey: String,
      source: String,
      sourceId: String,
      pgnPath: String,
      mixBucket: String,
      familyTags: List[String],
      ratingBucket: String,
      timeControlBucket: String,
      notes: List[String] = Nil,
      openingMacroFamily: Option[String] = None,
      opening: Option[String] = None,
      eco: Option[String] = None,
      variation: Option[String] = None,
      white: Option[String] = None,
      black: Option[String] = None,
      event: Option[String] = None,
      date: Option[String] = None,
      round: Option[String] = None,
      result: Option[String] = None,
      timeControl: Option[String] = None,
      variant: String = "standard",
      whiteElo: Option[Int] = None,
      blackElo: Option[Int] = None,
      totalPlies: Int = 0
  )
  object CatalogEntry:
    given Format[CatalogEntry] = Json.format[CatalogEntry]

  final case class SliceManifestEntry(
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      fen: String,
      playedSan: String,
      opening: Option[String],
      tags: List[String],
      pgnPath: String,
      playedUci: String,
      variant: String = "standard",
      mixBucket: Option[String] = None
  )
  object SliceManifestEntry:
    given Format[SliceManifestEntry] = Json.format[SliceManifestEntry]

  final case class SupportRow(label: String, text: String)
  object SupportRow:
    given Format[SupportRow] = Json.format[SupportRow]

  final case class MoveReviewQuietSupportTrace(
      liftApplied: Boolean = false,
      rejectReasons: List[String] = Nil,
      runtimeGatePassed: Option[Boolean] = None,
      runtimeGateRejectReasons: List[String] = Nil,
      runtimeSceneType: Option[String] = None,
      runtimeSelectedOwnerKind: Option[String] = None,
      runtimeSelectedSource: Option[String] = None,
      runtimePvDeltaAvailable: Option[Boolean] = None,
      runtimeSignalDigestAvailable: Option[Boolean] = None,
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = None,
      candidateBucket: Option[String] = None,
      candidateSourceKinds: List[String] = Nil,
      candidateVerbFamily: Option[String] = None,
      candidateText: Option[String] = None,
      factualSentence: Option[String] = None
  )
  object MoveReviewQuietSupportTrace:
    given Format[MoveReviewQuietSupportTrace] = Json.format[MoveReviewQuietSupportTrace]

  final case class MoveReviewOutputEntry(
      sampleId: String,
      gameKey: String,
      sliceKind: String,
      targetPly: Int,
      fen: String,
      playedSan: String,
      playedUci: String,
      opening: Option[String],
      openingReferenceTotalGames: Option[Int] = None,
      openingReferenceSampleGameIds: List[String] = Nil,
      openingReferencePeerSampleIds: List[String] = Nil,
      commentary: String,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow],
      sourceMode: String,
      model: Option[String],
      rawResponsePath: String,
      variationCount: Int,
      cacheHit: Boolean,
      plannerPrimaryKind: Option[String] = None,
      plannerPrimaryFallbackMode: Option[String] = None,
      plannerSecondaryKind: Option[String] = None,
      plannerSecondarySurfaced: Boolean = false,
      moveReviewFallbackMode: String = "unknown",
      plannerSceneType: Option[String] = None,
      plannerSceneReasons: List[String] = Nil,
      plannerOwnerCandidates: List[String] = Nil,
      plannerAdmittedOwners: List[String] = Nil,
      plannerDroppedOwners: List[String] = Nil,
      plannerSupportMaterialSeparation: List[String] = Nil,
      plannerProposedOwnerMappings: List[String] = Nil,
      plannerDemotionReasons: List[String] = Nil,
      plannerSelectedQuestion: Option[String] = None,
      plannerSelectedOwnerKind: Option[String] = None,
      plannerSelectedSource: Option[String] = None,
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
      surfaceReplayOutcome: Option[String] = None,
      contrast_source_kind: Option[String] = None,
      contrast_anchor: Option[String] = None,
      contrast_consequence: Option[String] = None,
      contrast_admissible: Option[Boolean] = None,
      contrast_reject_reason: Option[String] = None,
      contrast_replacement_used: Option[Boolean] = None,
      moveReviewSnapshotDigestHash: Option[String] = None,
      moveReviewCarryDigestHash: Option[String] = None,
      moveReviewAugmentationDigestHash: Option[String] = None,
      moveReviewBundleDigestHash: Option[String] = None,
      quietSupportLiftApplied: Option[Boolean] = None,
      quietSupportRejectReasons: List[String] = Nil,
      quietSupportRuntimeGatePassed: Option[Boolean] = None,
      quietSupportRuntimeGateRejectReasons: List[String] = Nil,
      quietSupportRuntimeSceneType: Option[String] = None,
      quietSupportRuntimeSelectedProofFamily: Option[String] = None,
      quietSupportRuntimeSelectedProofSource: Option[String] = None,
      quietSupportRuntimePvDeltaAvailable: Option[Boolean] = None,
      quietSupportRuntimeSignalDigestAvailable: Option[Boolean] = None,
      quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = None,
      quietSupportCandidateBucket: Option[String] = None,
      quietSupportCandidateSourceKinds: List[String] = Nil,
      quietSupportCandidateVerbFamily: Option[String] = None,
      quietSupportCandidateText: Option[String] = None,
      quietSupportFactualSentence: Option[String] = None,
      moveReviewSourceKind: Option[String] = None,
      basicEvidenceStatus: Option[String] = None,
      basicEvidenceRejectReasons: List[String] = Nil,
      supportedLocalCandidateFamilies: List[String] = Nil,
      supportedLocalAdmittedFamilies: List[String] = Nil,
      supportedLocalRejectReasons: List[String] = Nil,
      moveReviewLocalFactStatus: Option[String] = None,
      moveReviewLocalFactFamilies: List[String] = Nil,
      moveReviewLocalFactAuthorities: List[String] = Nil,
      moveReviewLocalFactStrictFallbackEligible: Option[Boolean] = None,
      moveReviewLocalFactRejectReasons: List[String] = Nil,
      moveReviewCausalClaimStatus: Option[String] = None,
      moveReviewCausalClaimQuestion: Option[String] = None,
      moveReviewCausalClaimSubject: Option[String] = None,
      moveReviewCausalClaimEvidence: List[String] = Nil,
      moveReviewCausalClaimRelations: List[String] = Nil,
      moveReviewCausalClaimRejectReasons: List[String] = Nil,
      moveReviewCausalClaimSupportEmbedded: Option[Boolean] = None,
      moveReviewCausalClaimGuardrail: Option[String] = None
  ):
    def withQuietSupportTrace(quietSupportTrace: MoveReviewQuietSupportTrace): MoveReviewOutputEntry =
      copy(
        quietSupportLiftApplied = Some(quietSupportTrace.liftApplied),
        quietSupportRejectReasons = quietSupportTrace.rejectReasons,
        quietSupportRuntimeGatePassed = quietSupportTrace.runtimeGatePassed,
        quietSupportRuntimeGateRejectReasons = quietSupportTrace.runtimeGateRejectReasons,
        quietSupportRuntimeSceneType = quietSupportTrace.runtimeSceneType,
        quietSupportRuntimeSelectedProofFamily = quietSupportTrace.runtimeSelectedOwnerKind,
        quietSupportRuntimeSelectedProofSource = quietSupportTrace.runtimeSelectedSource,
        quietSupportRuntimePvDeltaAvailable = quietSupportTrace.runtimePvDeltaAvailable,
        quietSupportRuntimeSignalDigestAvailable = quietSupportTrace.runtimeSignalDigestAvailable,
        quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable =
          quietSupportTrace.runtimeMoveLinkedPvDeltaAnchorAvailable,
        quietSupportCandidateBucket = quietSupportTrace.candidateBucket,
        quietSupportCandidateSourceKinds = quietSupportTrace.candidateSourceKinds,
        quietSupportCandidateVerbFamily = quietSupportTrace.candidateVerbFamily,
        quietSupportCandidateText = quietSupportTrace.candidateText,
        quietSupportFactualSentence = quietSupportTrace.factualSentence
      )
  object MoveReviewOutputEntry:
    private val LegacyToStableQuietSupportFields = List(
      "track3QuietSupportLiftApplied" -> "quietSupportLiftApplied",
      "track3QuietSupportRejectReasons" -> "quietSupportRejectReasons",
      "track3RuntimeGatePassed" -> "quietSupportRuntimeGatePassed",
      "track3RuntimeGateRejectReasons" -> "quietSupportRuntimeGateRejectReasons",
      "track3RuntimeSceneType" -> "quietSupportRuntimeSceneType",
      "track3RuntimeSelectedProofFamily" -> "quietSupportRuntimeSelectedProofFamily",
      "track3RuntimeSelectedProofSource" -> "quietSupportRuntimeSelectedProofSource",
      "track3RuntimePvDeltaAvailable" -> "quietSupportRuntimePvDeltaAvailable",
      "track3RuntimeSignalDigestAvailable" -> "quietSupportRuntimeSignalDigestAvailable",
      "track3RuntimeMoveLinkedPvDeltaAnchorAvailable" -> "quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable",
      "track3QuietSupportCandidateBucket" -> "quietSupportCandidateBucket",
      "track3QuietSupportCandidateSourceKinds" -> "quietSupportCandidateSourceKinds",
      "track3QuietSupportCandidateVerbFamily" -> "quietSupportCandidateVerbFamily",
      "track3QuietSupportCandidateText" -> "quietSupportCandidateText",
      "track3QuietSupportFactualSentence" -> "quietSupportFactualSentence"
    )

    private val reads0: Reads[MoveReviewOutputEntry] = Json.using[Json.WithDefaultValues].reads[MoveReviewOutputEntry]
    private val writes0: OWrites[MoveReviewOutputEntry] = Json.writes[MoveReviewOutputEntry]

    private def normalizeForRead(js: JsObject): JsObject =
      LegacyToStableQuietSupportFields.foldLeft(js) { case (acc, (legacyKey, stableKey)) =>
        if acc.keys.contains(stableKey) then acc
        else
          (acc \ legacyKey).toOption match
            case Some(value) => acc + (stableKey -> value)
            case None        => acc
      }

    given Format[MoveReviewOutputEntry] = Format(
      Reads {
        case obj: JsObject => reads0.reads(normalizeForRead(obj))
        case _             => JsError("error.expected.jsobject")
      },
      writes0
    )

  final case class MoveReviewPlannerTrace(
      primaryKind: Option[String] = None,
      primaryFallbackMode: Option[String] = None,
      secondaryKind: Option[String] = None,
      secondarySurfaced: Boolean = false,
      moveReviewFallbackMode: String = "unknown",
      sceneType: Option[String] = None,
      sceneReasons: List[String] = Nil,
      ownerCandidates: List[String] = Nil,
      admittedOwners: List[String] = Nil,
      droppedOwners: List[String] = Nil,
      supportMaterialSeparation: List[String] = Nil,
      proposedOwnerMappings: List[String] = Nil,
      demotionReasons: List[String] = Nil,
      selectedQuestion: Option[String] = None,
      selectedOwnerKind: Option[String] = None,
      selectedSource: Option[String] = None,
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
      tacticalFailureSources: List[String] = Nil,
      forcingDefenseSources: List[String] = Nil,
      moveDeltaSources: List[String] = Nil,
      surfaceReplayOutcome: Option[String] = None,
      contrastSourceKind: Option[String] = None,
      contrastAnchor: Option[String] = None,
      contrastConsequence: Option[String] = None,
      contrastAdmissible: Boolean = false,
      contrastRejectReason: Option[String] = None,
      contrastReplacementUsed: Boolean = false,
      causalClaimStatus: Option[String] = None,
      causalClaimQuestion: Option[String] = None,
      causalClaimSubject: Option[String] = None,
      causalClaimEvidence: List[String] = Nil,
      causalClaimRelations: List[String] = Nil,
      causalClaimRejectReasons: List[String] = Nil,
      causalClaimSupportEmbedded: Option[Boolean] = None,
      causalClaimGuardrail: Option[String] = None
  )

  final case class MoveReviewRuntimeTrace(
      planner: MoveReviewPlannerTrace,
      prose: String,
      quietSupport: MoveReviewQuietSupportTrace = MoveReviewQuietSupportTrace(),
      coverage: MoveReviewCoverageDiagnostics.Result = MoveReviewCoverageDiagnostics.Result.empty
  )

  final case class AuditSetEntry(
      auditId: String,
      gameId: String,
      tier: String,
      openingFamily: String,
      label: String,
      reportPath: String,
      rawDir: String,
      sourceTag: String,
      corpusPath: Option[String] = None,
      event: Option[String] = None,
      date: Option[String] = None,
      result: Option[String] = None
  )
  object AuditSetEntry:
    given Format[AuditSetEntry] = Json.format[AuditSetEntry]

  final case class AuditSetManifest(
      version: Int = 1,
      generatedAt: String,
      title: String,
      description: String,
      games: List[AuditSetEntry]
  )
  object AuditSetManifest:
    given Format[AuditSetManifest] = Json.format[AuditSetManifest]

  final case class ReviewQueueEntry(
      sampleId: String,
      auditId: Option[String] = None,
      gameId: String,
      surface: String,
      reviewKind: String = ReviewKind.MoveReviewFocus,
      sliceKind: String,
      tier: Option[String] = None,
      openingFamily: Option[String] = None,
      label: Option[String] = None,
      pairedSampleId: Option[String] = None,
      fen: String,
      playedSan: String,
      mainProse: String,
      supportRows: List[String],
      advancedRows: List[String],
      flags: List[String]
  )
  object ReviewQueueEntry:
    given Reads[ReviewQueueEntry] = Reads { js =>
      (js \ "sampleId").validate[String].map { sampleId =>
        ReviewQueueEntry(
          sampleId = sampleId,
          auditId = (js \ "auditId").asOpt[String],
          gameId = (js \ "gameId").asOpt[String].getOrElse(sampleId),
          surface = (js \ "surface").asOpt[String].getOrElse(ReviewSurface.MoveReview),
          reviewKind = (js \ "reviewKind").asOpt[String].getOrElse(ReviewKind.MoveReviewFocus),
          sliceKind = (js \ "sliceKind").asOpt[String].getOrElse(SliceKind.MoveReviewFocus),
          tier = (js \ "tier").asOpt[String],
          openingFamily = (js \ "openingFamily").asOpt[String],
          label = (js \ "label").asOpt[String],
          pairedSampleId = (js \ "pairedSampleId").asOpt[String],
          fen = (js \ "fen").asOpt[String].getOrElse(""),
          playedSan = (js \ "playedSan").asOpt[String].getOrElse(""),
          mainProse = (js \ "mainProse").asOpt[String].getOrElse(""),
          supportRows = (js \ "supportRows").asOpt[List[String]].getOrElse(Nil),
          advancedRows = (js \ "advancedRows").asOpt[List[String]].getOrElse(Nil),
          flags = (js \ "flags").asOpt[List[String]].getOrElse(Nil)
        )
      }
    }
    given OWrites[ReviewQueueEntry] = Json.writes[ReviewQueueEntry]
    given OFormat[ReviewQueueEntry] = OFormat(summon[Reads[ReviewQueueEntry]], summon[OWrites[ReviewQueueEntry]])

  final case class JudgmentEntry(
      sampleId: String,
      reviewer: String,
      severity: String,
      surface: Option[String] = None,
      reviewKind: Option[String] = None,
      tier: Option[String] = None,
      openingFamily: Option[String] = None,
      rubric: List[String],
      blockerType: Option[String],
      notes: String,
      fixFamily: List[String] = Nil
  )
  object JudgmentEntry:
    given Reads[JudgmentEntry] = Reads { js =>
      for
        sampleId <- (js \ "sampleId").validate[String]
        reviewer <- (js \ "reviewer").validateOpt[String]
        severity <- (js \ "severity").validate[String]
        rubric <- (js \ "rubric").validateOpt[List[String]]
        blockerType <- (js \ "blockerType").validateOpt[String]
        notes <- (js \ "notes").validateOpt[String]
        fixFamily <- (js \ "fixFamily").validateOpt[List[String]]
      yield JudgmentEntry(
        sampleId = sampleId,
        reviewer = reviewer.getOrElse("unknown"),
        severity = severity,
        surface = (js \ "surface").asOpt[String],
        reviewKind = (js \ "reviewKind").asOpt[String],
        tier = (js \ "tier").asOpt[String],
        openingFamily = (js \ "openingFamily").asOpt[String],
        rubric = rubric.getOrElse(Nil),
        blockerType = blockerType,
        notes = notes.getOrElse(""),
        fixFamily = fixFamily.getOrElse(Nil)
      )
    }
    given OWrites[JudgmentEntry] = Json.writes[JudgmentEntry]
    given OFormat[JudgmentEntry] = OFormat(summon[Reads[JudgmentEntry]], summon[OWrites[JudgmentEntry]])

  final case class ReviewQueueReport(
      version: Int,
      generatedAt: String,
      moveReviewOutputCount: Int,
      mandatoryReviewCount: Int,
      sampledReviewCount: Int,
      reviewedCount: Int,
      fullReview: Boolean = false,
      auditSetGameCount: Int = 0
  )
  object ReviewQueueReport:
    given Writes[ReviewQueueReport] = Json.writes[ReviewQueueReport]

  final case class MoveReviewFocusReport(
      ply: Int,
      moveReviewCommentary: String
  )
  object MoveReviewFocusReport:
    given OFormat[MoveReviewFocusReport] = Json.format[MoveReviewFocusReport]

  final case class GameReport(
      id: String,
      tier: String,
      family: String,
      label: String,
      event: Option[String],
      date: Option[String],
      opening: Option[String],
      result: Option[String],
      totalPlies: Int,
      probeCandidateRequests: Int,
      probeExecutedRequests: Int,
      probeUnsupportedRequests: Int,
      usedProbeRefinement: Boolean,
      overallThemes: List[String],
      moveReviewFocusRows: List[MoveReviewFocusReport]
  )
  object GameReport:
    given Reads[GameReport] = Reads { js =>
      for
        id <- (js \ "id").validate[String]
        tier <- (js \ "tier").validate[String]
        family <- (js \ "family").validate[String]
        label <- (js \ "label").validate[String]
      yield GameReport(
        id = id,
        tier = tier,
        family = family,
        label = label,
        event = (js \ "event").asOpt[String],
        date = (js \ "date").asOpt[String],
        opening = (js \ "opening").asOpt[String],
        result = (js \ "result").asOpt[String],
        totalPlies = (js \ "totalPlies").asOpt[Int].getOrElse(0),
        probeCandidateRequests = (js \ "probeCandidateRequests").asOpt[Int].getOrElse(0),
        probeExecutedRequests = (js \ "probeExecutedRequests").asOpt[Int].getOrElse(0),
        probeUnsupportedRequests = (js \ "probeUnsupportedRequests").asOpt[Int].getOrElse(0),
        usedProbeRefinement = (js \ "usedProbeRefinement").asOpt[Boolean].getOrElse(false),
        overallThemes = (js \ "overallThemes").asOpt[List[String]].getOrElse(Nil),
        moveReviewFocusRows =
          (js \ "moveReviewFocusRows").asOpt[List[MoveReviewFocusReport]]
            .orElse((js \ "focusMoments").asOpt[List[MoveReviewFocusReport]])
            .getOrElse(Nil)
      )
    }
    given OWrites[GameReport] = Json.writes[GameReport]
    given OFormat[GameReport] = OFormat(summon[Reads[GameReport]], summon[OWrites[GameReport]])

  final case class Summary(
      totalGames: Int,
      gamesUsingProbeRefinement: Int,
      totalProbeCandidateRequests: Int,
      totalProbeExecutedRequests: Int,
      totalProbeUnsupportedRequests: Int,
      familyCounts: Map[String, Int],
      compensationSubtypeCounts: Map[String, Int]
  )
  object Summary:
    given OFormat[Summary] = Json.format[Summary]

  final case class NegativeGuardResult(
      id: String,
      label: String,
      targetPly: Int,
      compensationPosition: Boolean,
      moveReviewCompensationMention: Boolean,
      passed: Boolean
  )
  object NegativeGuardResult:
    given OFormat[NegativeGuardResult] = Json.format[NegativeGuardResult]

  final case class AuditFailure(
      category: String,
      severity: String,
      key: String,
      gameId: Option[String],
      ply: Option[Int],
      detail: String
  )
  object AuditFailure:
    given OFormat[AuditFailure] = Json.format[AuditFailure]

  final case class SignoffSummary(
      falsePositiveCount: Int,
      falseNegativeCount: Int,
      positiveExemplarExpectedCount: Int,
      positiveExemplarEvaluatedCount: Int,
      crossSurfaceAgreementRate: Double,
      subtypeAgreementRate: Double,
      payoffTheaterAgreementRate: Double,
      pathVsPayoffDivergenceCount: Int,
      displaySubtypeSourceDistribution: Map[String, Int],
      mustFixFailureCount: Int,
      mustFixFailureCounts: Map[String, Int],
      negativeGuardPassCount: Int,
      negativeGuardFailCount: Int,
      negativeGuards: List[NegativeGuardResult],
      releaseGatePassed: Boolean,
      mustFixFailures: List[AuditFailure]
  )
  object SignoffSummary:
    given Reads[SignoffSummary] = Reads { js =>
      for
        falsePositiveCount <- (js \ "falsePositiveCount").validate[Int]
        falseNegativeCount <- (js \ "falseNegativeCount").validate[Int]
        positiveExemplarExpectedCount <- (js \ "positiveExemplarExpectedCount").validateOpt[Int]
        positiveExemplarEvaluatedCount <- (js \ "positiveExemplarEvaluatedCount").validateOpt[Int]
        crossSurfaceAgreementRate <- (js \ "crossSurfaceAgreementRate").validate[Double]
        subtypeAgreementRate <- (js \ "subtypeAgreementRate").validate[Double]
        payoffTheaterAgreementRate <- (js \ "payoffTheaterAgreementRate").validate[Double]
        pathVsPayoffDivergenceCount <- (js \ "pathVsPayoffDivergenceCount").validate[Int]
        displaySubtypeSourceDistribution <- (js \ "displaySubtypeSourceDistribution").validateOpt[Map[String, Int]]
        mustFixFailureCount <- (js \ "mustFixFailureCount").validateOpt[Int]
        mustFixFailureCounts <- (js \ "mustFixFailureCounts").validateOpt[Map[String, Int]]
        negativeGuardPassCount <- (js \ "negativeGuardPassCount").validate[Int]
        negativeGuardFailCount <- (js \ "negativeGuardFailCount").validate[Int]
        negativeGuards <- (js \ "negativeGuards").validate[List[NegativeGuardResult]]
        releaseGatePassed <- (js \ "releaseGatePassed").validateOpt[Boolean]
        mustFixFailures <- (js \ "mustFixFailures").validateOpt[List[AuditFailure]]
      yield SignoffSummary(
        falsePositiveCount = falsePositiveCount,
        falseNegativeCount = falseNegativeCount,
        positiveExemplarExpectedCount = positiveExemplarExpectedCount.getOrElse(6),
        positiveExemplarEvaluatedCount = positiveExemplarEvaluatedCount.getOrElse(0),
        crossSurfaceAgreementRate = crossSurfaceAgreementRate,
        subtypeAgreementRate = subtypeAgreementRate,
        payoffTheaterAgreementRate = payoffTheaterAgreementRate,
        pathVsPayoffDivergenceCount = pathVsPayoffDivergenceCount,
        displaySubtypeSourceDistribution = displaySubtypeSourceDistribution.getOrElse(Map.empty),
        mustFixFailureCount = mustFixFailureCount.getOrElse(0),
        mustFixFailureCounts = mustFixFailureCounts.getOrElse(Map.empty),
        negativeGuardPassCount = negativeGuardPassCount,
        negativeGuardFailCount = negativeGuardFailCount,
        negativeGuards = negativeGuards,
        releaseGatePassed = releaseGatePassed.getOrElse(false),
        mustFixFailures = mustFixFailures.getOrElse(Nil)
      )
    }
    given OWrites[SignoffSummary] = Json.writes[SignoffSummary]
    given OFormat[SignoffSummary] = OFormat(summon[Reads[SignoffSummary]], summon[OWrites[SignoffSummary]])

  final case class RunReport(
      version: Int = 1,
      generatedAt: String,
      corpusTitle: String,
      corpusAsOfDate: String,
      depth: Int,
      multiPv: Int,
      enginePath: String,
      summary: Summary,
      signoff: SignoffSummary,
      games: List[GameReport]
  )
  object RunReport:
    given OFormat[RunReport] = Json.format[RunReport]

  final case class Corpus(
      version: Int,
      generatedAt: String,
      asOfDate: String,
      title: String,
      description: String,
      games: List[CorpusGame]
  )
  object Corpus:
    given OFormat[Corpus] = Json.format[Corpus]

  final case class CorpusGame(
      id: String,
      tier: String,
      family: String,
      label: String,
      notes: List[String],
      expectedThemes: List[String],
      pgn: String
  )
  object CorpusGame:
    given OFormat[CorpusGame] = Json.format[CorpusGame]

  final case class SliceSnapshot(
      entry: CatalogEntry,
      plyData: PgnAnalysisHelper.PlyData,
      openingLabel: Option[String],
      phase: String,
      evalBeforeCp: Int,
      evalAfterCp: Int,
      evalSwingCp: Int,
      data: lila.commentary.model.ExtendedAnalysisData,
      rawCtx: Option[lila.commentary.model.NarrativeContext] = None,
      ctx: lila.commentary.model.NarrativeContext,
      strategyPack: Option[StrategyPack],
      signalDigest: Option[NarrativeSignalDigest],
      truthContract: Option[lila.commentary.analysis.DecisiveTruthContract],
      refs: Option[MoveReviewRefs]
  )

  final case class SceneCoverageCollectionCandidate(
      bucket: String,
      snapshot: SliceSnapshot,
      score: Int,
      reasons: List[String]
  )

  private val tagPattern = """(?m)^\[(\w+)\s+"(.*)"\]\s*$""".r
  private val resultTokenPattern = """(1-0|0-1|1/2-1/2|\*)\s*$""".r
  private val moveCountPattern = """(?i)(\d+)\+(\d+)""".r
  private val mixTargets = Map(
    MixBucket.MasterClassical -> 140,
    MixBucket.TitledPractical -> 100,
    MixBucket.Club -> 80,
    MixBucket.EdgeCase -> 40
  )
  // The corpus selector buckets openings into seven macro families. A 43-game
  // cap makes a 360-game target mathematically unreachable (7 * 43 = 301), so
  // the internal QA selector uses a looser ceiling here while still preventing
  // any single macro family from dominating the corpus.
  private val maxOpeningMacroFamilyGames = 60

  def parseHeaders(pgn: String): Map[String, String] =
    tagPattern
      .findAllMatchIn(Option(pgn).getOrElse(""))
      .map(m => m.group(1).trim -> m.group(2).trim)
      .toMap

  def splitPgnGames(raw: String): List[String] =
    val normalized = Option(raw).getOrElse("").replace("\r\n", "\n").replace('\r', '\n').trim
    if normalized.isEmpty then Nil
    else
      """(?m)(?=^\[Event\s+")""".r
        .split(normalized)
        .toList
        .map(_.trim)
        .filter(chunk => chunk.nonEmpty && chunk.startsWith("[Event "))

  def readPgnFiles(root: Path): List[Path] =
    if !Files.exists(root) then Nil
    else
      Files
        .walk(root)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path) && path.getFileName.toString.toLowerCase.endsWith(".pgn"))
        .toList
        .sortBy(_.toAbsolutePath.normalize.toString)

  def writeJson(path: Path, value: JsValue): Unit =
    ensureParent(path)
    Files.writeString(path, Json.prettyPrint(value) + "\n", StandardCharsets.UTF_8)

  def writeText(path: Path, value: String): Unit =
    ensureParent(path)
    Files.writeString(path, value, StandardCharsets.UTF_8)

  def writeJsonLines[T](path: Path, entries: List[T])(using Writes[T]): Unit =
    ensureParent(path)
    val text =
      entries
        .map(entry => Json.stringify(Json.toJson(entry)))
        .mkString("", "\n", if entries.nonEmpty then "\n" else "")
    Files.writeString(path, text, StandardCharsets.UTF_8)

  def readJsonLines[T](path: Path)(using Reads[T]): Either[String, List[T]] =
    if !Files.exists(path) then Left(s"missing file `${path.toAbsolutePath.normalize}`")
    else
      val lines =
        Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList.map(_.trim).filter(_.nonEmpty)
      val parsed =
        lines.zipWithIndex.map { case (line, idx) =>
          Json.parse(line).validate[T].asEither.left.map(err => s"line ${idx + 1}: $err")
        }
      val errors = parsed.collect { case Left(err) => err }
      if errors.nonEmpty then Left(errors.mkString("; ")) else Right(parsed.collect { case Right(value) => value })

  def ensureParent(path: Path): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))

  def ensureDir(path: Path): Unit =
    Files.createDirectories(path)

  def slug(raw: String): String =
    Option(raw)
      .map(_.toLowerCase.replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
      .getOrElse("unknown")

  def gameKeyFor(headers: Map[String, String], sourceId: String, ordinal: Int): String =
    slug(
      List(
        headers.getOrElse("Date", "unknown"),
        headers.getOrElse("Round", "?"),
        headers.getOrElse("White", "white"),
        headers.getOrElse("Black", "black"),
        sourceId,
        ordinal.toString
      ).mkString("__")
    )

  def openingLabel(headers: Map[String, String]): Option[String] =
    val opening = headers.get("Opening").map(_.trim).filter(_.nonEmpty)
    val variation = headers.get("Variation").map(_.trim).filter(_.nonEmpty)
    (opening, variation) match
      case (Some(o), Some(v)) if !o.equalsIgnoreCase(v) => Some(s"$o: $v")
      case (Some(o), _)                                 => Some(o)
      case _                                            => None

  def finalResultToken(pgn: String): Option[String] =
    resultTokenPattern.findFirstMatchIn(Option(pgn).getOrElse("").trim).map(_.group(1))

  def headerResultMatchesPgn(headers: Map[String, String], pgn: String): Boolean =
    val headerResult = headers.get("Result").map(_.trim).filter(_.nonEmpty)
    val token = finalResultToken(pgn)
    headerResult.isEmpty || headerResult.contains("*") || token.isEmpty || headerResult == token

  def parseIntHeader(headers: Map[String, String], key: String): Option[Int] =
    headers.get(key).flatMap(_.trim.toIntOption)

  def ratingBucketOf(headers: Map[String, String]): String =
    val avg =
      List(parseIntHeader(headers, "WhiteElo"), parseIntHeader(headers, "BlackElo")).flatten match
        case Nil    => None
        case values => Some(values.sum / values.size)
    avg match
      case Some(value) if value >= 2600 => RatingBucket.Elite
      case Some(value) if value >= 2300 => RatingBucket.Titled
      case Some(_)                      => RatingBucket.Club
      case None                         => RatingBucket.Unknown

  def timeControlBucketOf(headers: Map[String, String]): String =
    headers.get("TimeControl").map(_.trim).filter(_.nonEmpty) match
      case Some(moveCountPattern(main, inc)) =>
        val mainSeconds = main.toIntOption.getOrElse(0)
        val increment = inc.toIntOption.getOrElse(0)
        val total = mainSeconds + increment * 40
        timeBucketFromSeconds(total)
      case Some(raw) if raw.contains("/") =>
        val base =
          raw
            .split(":", 2)
            .headOption
            .flatMap(_.split("/").lastOption)
            .flatMap(_.trim.toIntOption)
            .getOrElse(0)
        timeBucketFromSeconds(base)
      case Some(raw) =>
        timeBucketFromSeconds(raw.split(":").headOption.flatMap(_.trim.toIntOption).getOrElse(0))
      case None => TimeControlBucket.Unknown

  private def timeBucketFromSeconds(seconds: Int): String =
    if seconds >= 3600 then TimeControlBucket.Classical
    else if seconds >= 600 then TimeControlBucket.Rapid
    else if seconds >= 180 then TimeControlBucket.Blitz
    else if seconds > 0 then TimeControlBucket.Bullet
    else TimeControlBucket.Unknown

  def inferMixBucket(path: Path, headers: Map[String, String]): String =
    val tags = pathSegments(path)
    if tags.contains("edge_case") || tags.contains("edgecase") then MixBucket.EdgeCase
    else if tags.contains("master_classical") || tags.contains("masters") then MixBucket.MasterClassical
    else if tags.contains("titled_practical") || tags.contains("titled") then MixBucket.TitledPractical
    else if tags.contains("club") then MixBucket.Club
    else
      (ratingBucketOf(headers), timeControlBucketOf(headers)) match
        case (RatingBucket.Elite, TimeControlBucket.Classical)   => MixBucket.MasterClassical
        case (RatingBucket.Elite, _)                             => MixBucket.TitledPractical
        case (RatingBucket.Titled, TimeControlBucket.Classical)  => MixBucket.MasterClassical
        case (RatingBucket.Titled, _)                            => MixBucket.TitledPractical
        case _                                                   => MixBucket.Club

  def pathSegments(path: Path): Set[String] =
    path
      .toAbsolutePath
      .normalize
      .iterator()
      .asScala
      .map(_.toString.toLowerCase.replace('-', '_'))
      .toSet

  def openingMacroFamily(headers: Map[String, String]): Option[String] =
    val eco = headers.get("ECO").map(_.trim.toUpperCase)
    val opening = openingLabel(headers).map(_.toLowerCase)
    val combined = List(eco.getOrElse(""), opening.getOrElse("")).mkString(" ")
    Option.when(combined.nonEmpty) {
      if combined.contains("sicilian") || eco.exists(_.startsWith("B2")) then "sicilian"
      else if combined.contains("italian") || combined.contains("ruy") || combined.contains("open game") || eco.exists(_.startsWith("C")) then "open_game"
      else if combined.contains("french") || combined.contains("caro") || combined.contains("pirc") || combined.contains("alekhine") then "french_caro_pirc_alekhine"
      else if combined.contains("slav") || combined.contains("qga") || combined.contains("queen's gambit declined") || combined.contains("qgd") || eco.exists(_.startsWith("D")) then "qgd_slav_qga"
      else if combined.contains("indian") || combined.contains("nimzo") || combined.contains("grunfeld") || combined.contains("king's indian") || combined.contains("queen's indian") || eco.exists(_.startsWith("E")) then "indian_complex"
      else if combined.contains("english") || combined.contains("reti") || combined.contains("catalan") || combined.contains("flank") || eco.exists(_.startsWith("A")) then "english_reti_catalan_flank"
      else "other"
    }

  def familyTagsFor(path: Path, headers: Map[String, String], totalPlies: Int): List[String] =
    val tags = scala.collection.mutable.ListBuffer.empty[String]
    val segments = pathSegments(path)
    if segments.contains("endgame_heavy") || totalPlies >= 90 then tags += "endgame_heavy"
    if segments.contains("compensation") || segments.contains("exchange_sac") || segments.contains("gambit") then tags += "compensation_exemplar"
    openingMacroFamily(headers).foreach(tags += _)
    tags.toList.distinct

  def selectCorpus(entries: List[CatalogEntry]): List[CatalogEntry] =
    val byBucket = entries.groupBy(_.mixBucket).withDefaultValue(Nil)
    val playerCounts = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val eventCounts = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val familyCounts = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val selected = scala.collection.mutable.ListBuffer.empty[CatalogEntry]

    def canTake(entry: CatalogEntry): Boolean =
      val white = entry.white.map(slug).getOrElse("white")
      val black = entry.black.map(slug).getOrElse("black")
      val eventKey = slug(entry.event.getOrElse(entry.sourceId))
      val familyKey = entry.openingMacroFamily.getOrElse("other")
      playerCounts(white) < 6 &&
      playerCounts(black) < 6 &&
      eventCounts(eventKey) < 12 &&
      familyCounts(familyKey) < maxOpeningMacroFamilyGames

    def accept(entry: CatalogEntry): Unit =
      val white = entry.white.map(slug).getOrElse("white")
      val black = entry.black.map(slug).getOrElse("black")
      val eventKey = slug(entry.event.getOrElse(entry.sourceId))
      val familyKey = entry.openingMacroFamily.getOrElse("other")
      playerCounts.update(white, playerCounts(white) + 1)
      playerCounts.update(black, playerCounts(black) + 1)
      eventCounts.update(eventKey, eventCounts(eventKey) + 1)
      familyCounts.update(familyKey, familyCounts(familyKey) + 1)
      selected += entry

    MixBucket.all.foreach { bucket =>
      val target = mixTargets.getOrElse(bucket, 0)
      val ranked =
        byBucket(bucket).sortBy { entry =>
          val priority =
            (if entry.familyTags.contains("endgame_heavy") then 0 else 1) +
              (if entry.familyTags.contains("compensation_exemplar") then 0 else 2)
          (
            priority,
            entry.event.getOrElse(""),
            entry.date.getOrElse(""),
            entry.gameKey
          )
        }
      ranked.iterator.filter(canTake).take(target).foreach(accept)
    }

    selected.toList

  def minimalOpeningReference(entry: CatalogEntry): Option[lila.commentary.model.OpeningReference] =
    val name = entry.opening.orElse(entry.openingMacroFamily)
    Option.when(name.isDefined || entry.eco.isDefined) {
      lila.commentary.model.OpeningReference(
        eco = entry.eco,
        name = name,
        totalGames = 0,
        topMoves = Nil,
        sampleGames = Nil,
        description = entry.variation
      )
    }

  def buildOpeningIndex(catalog: List[CatalogEntry]): Map[String, List[CatalogEntry]] =
    catalog
      .flatMap(entry => openingLookupKeys(entry).map(key => key -> entry))
      .groupMap(_._1)(_._2)

  def corpusBackedOpeningReference(
      entry: CatalogEntry,
      catalog: List[CatalogEntry]
  ): Option[lila.commentary.model.OpeningReference] =
    corpusBackedOpeningReference(entry, buildOpeningIndex(catalog), maxSampleGames = 5)

  def corpusBackedOpeningReference(
      entry: CatalogEntry,
      openingIndex: Map[String, List[CatalogEntry]],
      maxSampleGames: Int
  ): Option[lila.commentary.model.OpeningReference] =
    val peers =
      openingLookupKeys(entry)
        .flatMap(key => openingIndex.getOrElse(key, Nil))
        .filterNot(_.gameKey == entry.gameKey)
        .distinct
        .sortBy(other => (-other.totalPlies, other.gameKey))
        .take(maxSampleGames)

    val sampleEntries =
      if peers.nonEmpty then peers
      else List(entry)

    val sampleGames =
      sampleEntries.flatMap(catalogEntryToExplorerGame)

    Option.when(sampleGames.nonEmpty) {
      lila.commentary.model.OpeningReference(
        eco = entry.eco,
        name = entry.opening.orElse(entry.openingMacroFamily),
        totalGames = sampleEntries.size,
        topMoves = Nil,
        sampleGames = sampleGames,
        description = entry.variation
      )
    }.orElse(minimalOpeningReference(entry))

  private def openingLookupKeys(entry: CatalogEntry): List[String] =
    List(entry.eco, entry.opening, entry.openingMacroFamily)
      .flatten
      .map(_.trim.toLowerCase)
      .filter(key => key.nonEmpty && key != "other" && key != "unknown")
      .distinct

  private def catalogEntryToExplorerGame(entry: CatalogEntry): Option[lila.commentary.model.ExplorerGame] =
    val pgn =
      try Some(Files.readString(Paths.get(entry.pgnPath)))
      catch case NonFatal(_) => None
    val (year, month) = parseYearMonth(entry.date)
    pgn.map { raw =>
      lila.commentary.model.ExplorerGame(
        id = entry.gameKey,
        winner =
          entry.result.flatMap {
            case "1-0" => Some(chess.White)
            case "0-1" => Some(chess.Black)
            case _     => None
          },
        white = lila.commentary.model.ExplorerPlayer(entry.white.getOrElse("?"), entry.whiteElo.getOrElse(0)),
        black = lila.commentary.model.ExplorerPlayer(entry.black.getOrElse("?"), entry.blackElo.getOrElse(0)),
        year = year,
        month = month,
        event = entry.event.map(_.trim).filter(_.nonEmpty),
        pgn = Some(raw)
      )
    }

  private def parseYearMonth(date: Option[String]): (Int, Int) =
    date match
      case Some(raw) =>
        val normalized = raw.trim.replace('-', '.')
        val parts = normalized.split("\\.").toList
        val year = parts.headOption.flatMap(_.toIntOption).getOrElse(0)
        val month = parts.lift(1).flatMap(_.toIntOption).getOrElse(1)
        (year, month)
      case None => (0, 1)

  def guessPhase(fen: String, ply: Int): String =
    val pieces = fen.takeWhile(_ != ' ').count(_.isLetter)
    val queens = fen.takeWhile(_ != ' ').count(ch => ch == 'Q' || ch == 'q')
    if pieces <= 12 || (pieces <= 16 && queens <= 1) then "endgame"
    else if ply <= 20 then "opening"
    else "middlegame"

  def buildAfterMoveEvals(
      plyData: List[PgnAnalysisHelper.PlyData],
      engine: LocalUciEngine,
      depth: Int,
      multiPv: Int
  ): List[MoveEval] =
    engine.newGame()
    plyData.map { pd =>
      val afterFen = NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
      val variations = engine.analyze(afterFen, depth, multiPv)
      val best = variations.headOption
      MoveEval(
        ply = pd.ply,
        cp = best.map(_.scoreCp).getOrElse(0),
        mate = best.flatMap(_.mate),
        pv = best.map(_.moves).getOrElse(Nil),
        variations = variations
      )
    }

  def analyzePly(
      entry: CatalogEntry,
      plyData: PgnAnalysisHelper.PlyData,
      beforeVariations: List[VariationLine],
      afterEval: Option[MoveEval]
  ): Option[SliceSnapshot] =
    analyzePlyWithOpeningRef(
      entry = entry,
      plyData = plyData,
      beforeVariations = beforeVariations,
      afterEval = afterEval,
      openingRef = minimalOpeningReference(entry)
    )

  def analyzePlyWithOpeningRef(
      entry: CatalogEntry,
      plyData: PgnAnalysisHelper.PlyData,
      beforeVariations: List[VariationLine],
      afterEval: Option[MoveEval],
      openingRef: Option[lila.commentary.model.OpeningReference]
  ): Option[SliceSnapshot] =
    val phase = guessPhase(plyData.fen, plyData.ply)
    val beforeCp = beforeVariations.headOption.map(_.scoreCp).getOrElse(0)
    val afterCp = afterEval.map(_.cp).getOrElse(beforeCp)
    val evalSwing = (afterCp - beforeCp).abs
    CommentaryEngine
      .assessExtended(
        fen = plyData.fen,
        variations = beforeVariations,
        playedMove = Some(plyData.playedUci),
        opening = entry.opening,
        phase = Some(phase),
        ply = plyData.ply,
        prevMove = Some(plyData.playedUci),
        evalDeltaCp = Some(evalSwing)
      )
      .map { data =>
        val afterData =
          afterEval.flatMap { eval =>
            val afterFen = NarrativeUtils.uciListToFen(plyData.fen, List(plyData.playedUci))
            val vars = eval.getVariations
            Option.when(vars.nonEmpty) {
              CommentaryEngine.assessExtended(
                fen = afterFen,
                variations = vars,
                playedMove = None,
                opening = entry.opening,
                phase = Some(guessPhase(afterFen, plyData.ply)),
                ply = plyData.ply
              )
            }.flatten
          }

        val rawCtx =
          NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            probeResults = Nil,
            openingRef = openingRef,
            afterAnalysis = afterData,
            renderMode = NarrativeRenderMode.MoveReview,
            variantKey = entry.variant
          )
        val rawStrategyPack = StrategyPackBuilder.build(data, rawCtx)
        val truthContract =
          Some(
            DecisiveTruth.derive(
              ctx = rawCtx,
              transitionType = data.planSequence.map(_.transitionType.toString),
              strategyPack = rawStrategyPack
            )
          )
        val ctx = truthContract.fold(rawCtx)(DecisiveTruth.sanitizeContext(rawCtx, _))
        val strategyPack = truthContract.fold(rawStrategyPack)(DecisiveTruth.sanitizeStrategyPack(rawStrategyPack, _))
        val signalDigest =
          strategyPack.flatMap(_.signalDigest).orElse(NarrativeSignalDigestBuilder.build(ctx))
        val afterFen = NarrativeUtils.uciListToFen(plyData.fen, List(plyData.playedUci))
        val refVariations =
          CommentaryApi.appendMoveReviewAfterPvProofVariation(
            fenBefore = plyData.fen,
            lastMove = Some(plyData.playedUci),
            afterFen = Some(afterFen),
            base = data.alternatives,
            afterVars = afterEval.map(_.getVariations).getOrElse(Nil)
          )
        val refs = buildMoveReviewRefs(plyData.fen, refVariations)
        SliceSnapshot(
          entry = entry,
          plyData = plyData,
          openingLabel = entry.opening,
          phase = phase,
          evalBeforeCp = beforeCp,
          evalAfterCp = afterCp,
          evalSwingCp = evalSwing,
          data = data,
          rawCtx = Some(rawCtx),
          ctx = ctx,
          strategyPack = strategyPack,
          signalDigest = signalDigest,
          truthContract = truthContract,
          refs = refs
        )
      }

  private def openingEventCarriesBranching(snapshot: SliceSnapshot): Boolean =
    snapshot.ctx.openingEvent.exists {
      case lila.commentary.model.OpeningEvent.BranchPoint(_, _, _) => true
      case lila.commentary.model.OpeningEvent.OutOfBook(_, _, _)   => true
      case lila.commentary.model.OpeningEvent.TheoryEnds(_, _)     => true
      case lila.commentary.model.OpeningEvent.Novelty(_, _, _, _)  => true
      case _                                                => false
    }

  private def openingPrecedentAvailable(snapshot: SliceSnapshot): Boolean =
    snapshot.ctx.openingData.exists(ref => ref.sampleGames.nonEmpty || ref.topMoves.nonEmpty)

  private def openingPlanClashCollectionScore(snapshot: SliceSnapshot): Option[(Int, List[String])] =
    val inWindow =
      snapshot.phase == "middlegame" &&
        snapshot.plyData.ply >= 18 &&
        snapshot.plyData.ply <= 36
    if !inWindow then None
    else
      val openingReasons =
        List(
          Option.when(snapshot.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty)))(
            "opening_relation_claim"
          ),
          Option.when(openingEventCarriesBranching(snapshot))("opening_branch_event"),
          Option.when(openingPrecedentAvailable(snapshot))("opening_precedent_available")
        ).flatten
      val supportReasons =
        List(
          Option.when(snapshot.signalDigest.exists(_.opening.exists(_.trim.nonEmpty)))("opening_label"),
          Option.when(snapshot.ctx.mainStrategicPlans.nonEmpty)("main_strategic_plans"),
          Option.when(snapshot.data.planAlignment.nonEmpty)("plan_alignment"),
          Option.when(snapshot.signalDigest.exists(_.opponentPlan.exists(_.trim.nonEmpty)))("opponent_plan"),
          Option.when(snapshot.signalDigest.exists(_.decision.exists(_.trim.nonEmpty)))("decision"),
          Option.when(snapshot.signalDigest.exists(_.structuralCue.exists(_.trim.nonEmpty)))("structural_cue"),
          Option.when(snapshot.strategyPack.exists(_.longTermFocus.nonEmpty))("long_term_focus")
        ).flatten
      val penalties =
        List(
          Option.when(snapshot.evalSwingCp >= 150)("high_eval_swing"),
          Option.when(
            snapshot.ctx.header.criticality == "Forced" ||
              snapshot.ctx.header.criticality == "Critical"
          )("forced_or_critical")
        ).flatten
      val score =
        openingReasons.foldLeft(0) {
          case (acc, "opening_relation_claim") => acc + 5
          case (acc, "opening_branch_event")   => acc + 3
          case (acc, "opening_precedent_available") => acc + 3
          case (acc, _)                        => acc
        } +
          supportReasons.foldLeft(0) {
            case (acc, "main_strategic_plans") => acc + 3
            case (acc, "plan_alignment")       => acc + 2
            case (acc, "long_term_focus")      => acc + 2
            case (acc, _)                      => acc + 1
          } -
          penalties.foldLeft(0) {
            case (acc, "high_eval_swing")     => acc + 2
            case (acc, "forced_or_critical")  => acc + 1
            case (acc, _)                     => acc
          }
      Option.when(openingReasons.nonEmpty && supportReasons.nonEmpty && score >= 5) {
        (score, openingReasons ++ supportReasons ++ penalties)
      }

  private[tools] def collectOpeningPlanClashCandidates(
      snapshots: List[SliceSnapshot],
      perGameLimit: Int = 1
  ): List[SceneCoverageCollectionCandidate] =
    snapshots
      .sortBy(_.plyData.ply)
      .flatMap { snapshot =>
        openingPlanClashCollectionScore(snapshot).map { case (score, reasons) =>
          SceneCoverageCollectionCandidate(
            bucket = SliceKind.OpeningDeviationAfterMiddlegamePlanClash,
            snapshot = snapshot,
            score = score,
            reasons = reasons
          )
        }
      }
      .sortBy(candidate => (-candidate.score, candidate.snapshot.plyData.ply))
      .take(perGameLimit)

  private def prophylaxisRestraintCollectionScore(snapshot: SliceSnapshot): Option[(Int, List[String])] =
    val inWindow =
      snapshot.phase != "opening" &&
        snapshot.plyData.ply >= 16 &&
        snapshot.plyData.ply <= 72 &&
        snapshot.evalSwingCp < 140
    if !inWindow then None
    else
      val restraintReasons =
        List(
          Option.when(snapshot.data.preventedPlans.exists(_.sourceScope == lila.commentary.model.FactScope.Now))(
            "prevented_plan_now"
          ),
          Option.when(snapshot.signalDigest.exists(_.prophylaxisPlan.exists(_.trim.nonEmpty)))(
            "prophylaxis_plan"
          ),
          Option.when(snapshot.signalDigest.exists(_.prophylaxisThreat.exists(_.trim.nonEmpty)))(
            "prophylaxis_threat"
          ),
          Option.when(snapshot.signalDigest.exists(_.counterplayScoreDrop.exists(_ >= 40)))(
            "counterplay_score_drop"
          )
        ).flatten
      val supportReasons =
        List(
          Option.when(snapshot.ctx.mainStrategicPlans.nonEmpty)("main_strategic_plans"),
          Option.when(snapshot.data.planAlignment.nonEmpty)("plan_alignment"),
          Option.when(snapshot.signalDigest.exists(_.structureProfile.exists(_.trim.nonEmpty)))("structure_profile"),
          Option.when(snapshot.signalDigest.exists(_.structuralCue.exists(_.trim.nonEmpty)))("structural_cue"),
          Option.when(snapshot.signalDigest.exists(_.decision.exists(_.trim.nonEmpty)))("decision"),
          Option.when(snapshot.signalDigest.exists(_.opponentPlan.exists(_.trim.nonEmpty)))("opponent_plan"),
          Option.when(snapshot.strategyPack.exists(_.longTermFocus.nonEmpty))("long_term_focus")
        ).flatten
      val penalties =
        List(
          Option.when(
            snapshot.ctx.header.criticality == "Forced" ||
              snapshot.ctx.header.criticality == "Critical"
          )("forced_or_critical"),
          Option.when(snapshot.ctx.header.choiceType == "OnlyMove")("only_move")
        ).flatten
      val score =
        restraintReasons.foldLeft(0) {
          case (acc, "prevented_plan_now")     => acc + 5
          case (acc, "counterplay_score_drop") => acc + 4
          case (acc, "prophylaxis_plan")       => acc + 4
          case (acc, "prophylaxis_threat")     => acc + 3
          case (acc, _)                        => acc
        } +
          supportReasons.foldLeft(0) {
            case (acc, "main_strategic_plans") => acc + 3
            case (acc, "long_term_focus")      => acc + 2
            case (acc, "plan_alignment")       => acc + 2
            case (acc, _)                      => acc + 1
          } -
          penalties.foldLeft(0) {
            case (acc, "forced_or_critical") => acc + 2
            case (acc, "only_move")          => acc + 2
            case (acc, _)                    => acc
          }
      Option.when(restraintReasons.nonEmpty && supportReasons.nonEmpty && score >= 6) {
        (score, restraintReasons ++ supportReasons ++ penalties)
      }

  private[tools] def collectProphylaxisRestraintCandidates(
      snapshots: List[SliceSnapshot],
      perGameLimit: Int = 1
  ): List[SceneCoverageCollectionCandidate] =
    snapshots
      .sortBy(_.plyData.ply)
      .flatMap { snapshot =>
        prophylaxisRestraintCollectionScore(snapshot).map { case (score, reasons) =>
          SceneCoverageCollectionCandidate(
            bucket = SliceKind.ProphylaxisRestraint,
            snapshot = snapshot,
            score = score,
            reasons = reasons
          )
        }
      }
      .sortBy(candidate => (-candidate.score, candidate.snapshot.plyData.ply))
      .take(perGameLimit)

  def selectSlices(snapshots: List[SliceSnapshot]): List[(String, SliceSnapshot)] =
    val sorted = snapshots.sortBy(_.plyData.ply)

    def choose(kind: String)(predicate: SliceSnapshot => Boolean): Option[(String, SliceSnapshot)] =
      sorted.find(predicate).map(kind -> _)

    val opening =
      choose(SliceKind.OpeningTransition) { snapshot =>
        val ply = snapshot.plyData.ply
        val eventBoost =
          snapshot.ctx.openingEvent match
            case Some(lila.commentary.model.OpeningEvent.BranchPoint(_, _, _)) => true
            case Some(lila.commentary.model.OpeningEvent.OutOfBook(_, _, _))   => true
            case Some(lila.commentary.model.OpeningEvent.TheoryEnds(_, _))     => true
            case Some(lila.commentary.model.OpeningEvent.Novelty(_, _, _, _))  => true
            case _                                                      => false
        ply >= 8 && ply <= 16 && (eventBoost || ply == 10)
      }

    val strategicChoice =
      choose(SliceKind.StrategicChoice) { snapshot =>
        val header = snapshot.ctx.header
        val hasStrategicSignal =
          snapshot.ctx.mainStrategicPlans.nonEmpty ||
            snapshot.data.planAlignment.nonEmpty ||
            snapshot.signalDigest.exists(d =>
              d.structureProfile.nonEmpty ||
                d.deploymentPurpose.nonEmpty ||
                d.decision.nonEmpty
            )
        snapshot.plyData.ply >= 14 &&
        snapshot.plyData.ply <= 35 &&
        header.criticality == "Normal" &&
        header.choiceType != "OnlyMove" &&
        snapshot.evalSwingCp < 150 &&
        hasStrategicSignal
      }

    val prophylaxis =
      choose(SliceKind.Prophylaxis) { snapshot =>
        snapshot.data.preventedPlans.exists(_.sourceScope == lila.commentary.model.FactScope.Now) ||
        snapshot.signalDigest.exists(d =>
          d.prophylaxisPlan.exists(_.trim.nonEmpty) || d.prophylaxisThreat.exists(_.trim.nonEmpty)
        )
      }

    val prophylaxisRestraint =
      choose(SliceKind.ProphylaxisRestraint) { snapshot =>
        snapshot.plyData.ply >= 14 &&
        snapshot.plyData.ply <= 42 &&
        snapshot.evalSwingCp < 120 &&
        snapshot.data.preventedPlans.exists(_.sourceScope == lila.commentary.model.FactScope.Now) &&
        snapshot.signalDigest.exists(d =>
          d.prophylaxisPlan.exists(_.trim.nonEmpty) ||
            d.prophylaxisThreat.exists(_.trim.nonEmpty) ||
            d.counterplayScoreDrop.exists(_ >= 40)
        )
      }

    val longStructuralSqueeze =
      choose(SliceKind.LongStructuralSqueeze) { snapshot =>
        snapshot.plyData.ply >= 18 &&
        snapshot.plyData.ply <= 60 &&
        snapshot.evalSwingCp < 120 &&
        snapshot.ctx.header.criticality == "Normal" &&
        snapshot.signalDigest.exists(d =>
          d.structureProfile.exists(_.trim.nonEmpty) ||
            d.structuralCue.exists(_.trim.nonEmpty) ||
            d.centerState.exists(_.trim.nonEmpty)
        ) &&
        (
          snapshot.ctx.mainStrategicPlans.nonEmpty ||
            snapshot.strategyPack.exists(pack => pack.longTermFocus.nonEmpty || pack.plans.nonEmpty)
        )
      }

    val tacticalTurn =
      choose(SliceKind.TacticalTurn) { snapshot =>
        snapshot.evalSwingCp >= 150 ||
          snapshot.ctx.header.choiceType == "OnlyMove" ||
          snapshot.ctx.header.criticality == "Forced" ||
          snapshot.ctx.header.criticality == "Critical"
      }

    val openingDeviationAfterMiddlegamePlanClash =
      choose(SliceKind.OpeningDeviationAfterMiddlegamePlanClash) { snapshot =>
        snapshot.plyData.ply >= 18 &&
        snapshot.plyData.ply <= 36 &&
        snapshot.signalDigest.exists(_.openingRelationClaim.exists(_.trim.nonEmpty)) &&
        (
          snapshot.ctx.mainStrategicPlans.nonEmpty ||
            snapshot.data.planAlignment.nonEmpty ||
            snapshot.signalDigest.exists(d => d.opponentPlan.exists(_.trim.nonEmpty) || d.decision.exists(_.trim.nonEmpty))
        )
      }

    val compensation =
      choose(SliceKind.CompensationOrExchangeSac) { snapshot =>
        snapshot.data.compensation.exists(_.investedMaterial >= 100) ||
        snapshot.signalDigest.exists(d => d.compensation.exists(_.trim.nonEmpty) && d.investedMaterial.exists(_ >= 100))
      }

    val practical =
      choose(SliceKind.PracticalSimplification) { snapshot =>
        snapshot.plyData.ply >= 20 &&
        snapshot.data.practicalAssessment.exists(_.verdict.trim.nonEmpty) &&
        !snapshot.data.endgameFeatures.isDefined
      }

    val endgame =
      choose(SliceKind.EndgameConversion) { snapshot =>
        snapshot.plyData.ply >= 35 &&
        (snapshot.phase == "endgame" || snapshot.data.endgameFeatures.isDefined)
      }

    val transitionHeavyEndgames =
      choose(SliceKind.TransitionHeavyEndgames) { snapshot =>
        snapshot.plyData.ply >= 28 &&
        (snapshot.phase == "endgame" || snapshot.data.endgameFeatures.isDefined) &&
        snapshot.signalDigest.exists(d =>
          d.endgameTransitionClaim.exists(_.trim.nonEmpty) ||
            d.practicalVerdict.exists(_.trim.nonEmpty) ||
            d.preservedSignals.exists(_.toLowerCase.contains("endgame"))
        )
      }

    val baseFamilySlices =
      List(
        opening,
        strategicChoice,
        prophylaxis,
        tacticalTurn,
        compensation,
        practical,
        endgame
      )
        .flatten
        .groupBy(_._2.plyData.ply)
        .values
        .map(_.head)
        .toList
        .sortBy(_._2.plyData.ply)

    val sceneCoverageSlices =
      List(
        prophylaxisRestraint,
        longStructuralSqueeze,
        openingDeviationAfterMiddlegamePlanClash,
        transitionHeavyEndgames
      )
        .flatten
        .toList
        .sortBy(_._2.plyData.ply)

    val questionWhyNow = selectQuestionWhyNowSnapshot(sorted).map(SliceKind.QuestionWhyNow -> _)

    (baseFamilySlices ++ sceneCoverageSlices ++ questionWhyNow.toList)
      .groupBy { case (kind, snapshot) =>
        if kind == SliceKind.QuestionWhyNow || SliceKind.sceneCoverageLaneKinds.contains(kind) then
          s"$kind:${snapshot.plyData.ply}"
        else snapshot.plyData.ply.toString
      }
      .values
      .map(_.head)
      .toList
      .sortBy { case (_, snapshot) => snapshot.plyData.ply }

  private def carriesQuestion(snapshot: SliceSnapshot, kind: AuthorQuestionKind): Boolean =
    snapshot.ctx.authorQuestions.exists(_.kind == kind) ||
      snapshot.ctx.authorEvidence.exists(evidence =>
        snapshot.ctx.authorQuestions.exists(q => q.id == evidence.questionId && q.kind == kind)
      )

  private[tools] def selectQuestionWhyNowSnapshot(
      snapshots: List[SliceSnapshot],
      allowedPlies: Option[Set[Int]] = None
  ): Option[SliceSnapshot] =
    snapshots
      .sortBy(_.plyData.ply)
      .filter(isConcreteWhyNowCandidate)
      .filter(snapshot => allowedPlies.forall(_.contains(snapshot.plyData.ply)))
      .sortBy(questionWhyNowScore)
      .lastOption

  private[tools] def isConcreteWhyNowCandidate(snapshot: SliceSnapshot): Boolean =
    carriesQuestion(snapshot, AuthorQuestionKind.WhyNow) &&
    snapshot.plyData.ply >= 6 &&
    QuestionFirstCommentaryPlanner.hasConcreteWhyNowOwner(
      inputs = QuestionPlannerInputsBuilder.build(snapshot.ctx, snapshot.strategyPack, snapshot.truthContract),
      truthContract = snapshot.truthContract
    )

  private[tools] def questionWhyNowScore(snapshot: SliceSnapshot): (Int, Int, Int, Int, Int) =
    (
      questionWhyNowCriticalityScore(snapshot),
      questionWhyNowChoiceScore(snapshot),
      Option.when(snapshot.data.preventedPlans.nonEmpty || snapshot.ctx.threats.toUs.nonEmpty)(1).getOrElse(0),
      snapshot.evalSwingCp.min(400),
      snapshot.plyData.ply
    )

  private def questionWhyNowCriticalityScore(snapshot: SliceSnapshot): Int =
    snapshot.ctx.header.criticality match
      case "Critical" => 3
      case "Forced"   => 2
      case _          => 0

  private def questionWhyNowChoiceScore(snapshot: SliceSnapshot): Int =
    snapshot.ctx.header.choiceType match
      case "OnlyMove"     => 3
      case "NarrowChoice" => 2
      case _              => 0

  def manifestEntriesFor(sliceKind: String, snapshot: SliceSnapshot): List[SliceManifestEntry] =
    val baseId = s"${snapshot.entry.gameKey}:${sliceKind}:${snapshot.plyData.ply}"
    List(
      SliceManifestEntry(
        sampleId = s"$baseId:moveReview",
        gameKey = snapshot.entry.gameKey,
        surface = ReviewSurface.MoveReview,
        sliceKind = sliceKind,
        targetPly = snapshot.plyData.ply,
        fen = snapshot.plyData.fen,
        playedSan = snapshot.plyData.playedMove,
        opening = snapshot.openingLabel,
        tags = snapshot.entry.familyTags ++ List(snapshot.entry.mixBucket, sliceKind),
        pgnPath = snapshot.entry.pgnPath,
        playedUci = snapshot.plyData.playedUci,
        variant = snapshot.entry.variant,
        mixBucket = Some(snapshot.entry.mixBucket)
      )
    )

  def sentenceCount(text: String): Int =
    protectChessMoveNumbers(Option(text).getOrElse(""))
      .split("""(?<=[.!?])\s+""")
      .map(_.trim)
      .count(_.nonEmpty)

  def flattenRows(rows: List[SupportRow]): List[String] =
    rows.map(row => s"${row.label}: ${row.text}".trim)

  def isMandatoryReview(sliceKind: String, flags: List[String]): Boolean =
    flags.exists(flag =>
      flag.startsWith("meta_language") ||
        flag.startsWith("internal_label") ||
        flag.startsWith("uci_leak") ||
        flag == "sentence_budget_exceeded" ||
        flag == "missing_concrete_anchor" ||
        flag == "generic_filler_main_prose" ||
        flag == "anchored_support_missing_from_prose" ||
        flag == "conditionality_blur" ||
        flag == "branch_citation_present" ||
        flag == "sidecar_empty_prose_risk" ||
        flag.startsWith("taxonomy_residue")
    ) || Set(
      SliceKind.CompensationOrExchangeSac,
      SliceKind.Prophylaxis,
      SliceKind.TacticalTurn
    ).contains(sliceKind)

  def sampleByHash(sampleId: String, modulo: Int = 4): Boolean =
    math.abs(Option(sampleId).getOrElse("").hashCode) % modulo == 0

  private val genericFillerFragments = List(
    "this is still normal",
    "still normal development",
    "the opening phase is still fluid",
    "the game remains in opening channels",
    "piece coordination remains central",
    "there is little room for slow setup moves"
  )

  private val conditionalityFragments = List(
    "acceptable",
    "playable",
    "secondary",
    "conditional",
    "engine preference still leans",
    "stays secondary",
    "still cleaner",
    "less direct"
  )

  private val taxonomyResidueFragments = List(
    "theme:",
    "subplan:",
    "support:engine_hypothesis",
    "playablebypv",
    "strict evidence mode",
    "current evidence threshold",
    "engine-coupled continuation",
    "confirmation is still pending",
    "supported by the current engine line"
  )
  private val internalLabelFragments = List(
    "movedelta.",
    "pv_delta",
    "neutralize_key_break",
    "proof_family",
    "proof_source",
    "source_kind",
    "candidate_bucket",
    "runtime_gate",
    "owner_kind",
    "opening_branch_event",
    "support_only"
  )
  private val uciLeakRegex = """(?i)(?<![A-Za-z0-9])(?:[a-h][1-8][a-h][1-8][qrbn]?)(?![A-Za-z0-9])""".r
  private val chessSquareRegex = """[a-h][1-8]""".r
  private val surfaceAuthorityKeyRegex = """[a-z][a-z0-9_]{1,40}""".r
  private val surfaceAuthorityTokenRegex = """(?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?""".r
  private val surfaceAuthorityRouteTokenRegex = """(?:\.\.\.)?[a-h][1-8]-[a-h][1-8]""".r

  private def splitNarrativeSentences(raw: String): List[String] =
    protectChessMoveNumbers(Option(raw).getOrElse(""))
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def protectChessMoveNumbers(text: String): String =
    text
      .replaceAll(
        """\b(\d+)\.\.\.(?=\s*(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|[a-h][1-8]))""",
        "$1<ELLIPSIS>"
      )
      .replaceAll(
        """\b(\d+)\.(?=\s*(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|[a-h][1-8]))""",
        "$1<PLY>"
      )

  private def isLineEvidenceSentence(raw: String): Boolean =
    val low = Option(raw).getOrElse("").trim.toLowerCase
    low.startsWith("a concrete line is ")

  private def leadingMainProse(raw: String, take: Int): List[String] =
    val nonLineEvidence = splitNarrativeSentences(raw).filterNot(isLineEvidenceSentence)
    if nonLineEvidence.nonEmpty then nonLineEvidence.take(take)
    else splitNarrativeSentences(raw).take(take)

  private def proseLooksGenericFiller(raw: String): Boolean =
    val leading = leadingMainProse(raw, take = 1)
    leading.nonEmpty &&
    leading.forall { sentence =>
      val low = sentence.toLowerCase
      LiveNarrativeCompressionCore.isLowValueNarrativeSentence(sentence) ||
      genericFillerFragments.exists(low.contains)
    }

  private def hasAnchoredSupport(rows: List[SupportRow]): Boolean =
    rows.exists(row =>
      LiveNarrativeCompressionCore.keepPlayerFacingSentence(row.text) &&
        LiveNarrativeCompressionCore.hasConcreteAnchor(row.text)
    )

  private def mentionsConditionality(raw: String): Boolean =
    val low = Option(raw).getOrElse("").toLowerCase
    conditionalityFragments.exists(low.contains)

  private def hasConditionalitySupport(rows: List[SupportRow]): Boolean =
    rows.exists(row =>
      row.label == "Decision compare" || mentionsConditionality(row.text)
    )

  private def taxonomyResidueHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    taxonomyResidueFragments.filter(low.contains)

  private def internalLabelHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    internalLabelFragments.filter(low.contains)

  private def uciLeakHits(raw: String): List[String] =
    uciLeakRegex.findAllMatchIn(Option(raw).getOrElse("")).map(_.matched).toList.distinct

  def reviewFlags(
      prose: String,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow],
      sliceKind: String
  ): List[String] =
    val combined =
      List(prose) ++
        supportRows.flatMap(row => List(row.label, row.text)) ++
        advancedRows.flatMap(row => List(row.label, row.text))
    val mainProse = leadingMainProse(prose, take = 2).mkString(" ")
    val hits =
      combined.flatMap(text => LiveNarrativeCompressionCore.systemLanguageHits(text) ++ LiveNarrativeCompressionCore.playerLanguageHits(text)).distinct
    val flags = scala.collection.mutable.ListBuffer.empty[String]
    if hits.nonEmpty then flags += s"meta_language:${hits.mkString(",")}"
    if sentenceCount(prose) > 4 then flags += "sentence_budget_exceeded"
    if LiveNarrativeCompressionCore.requiresConcreteAnchor(prose) && !LiveNarrativeCompressionCore.hasConcreteAnchor(prose) then
      flags += "missing_concrete_anchor"
    if proseLooksGenericFiller(prose) then flags += "generic_filler_main_prose"
    if hasAnchoredSupport(supportRows ++ advancedRows) && mainProse.nonEmpty && !LiveNarrativeCompressionCore.hasConcreteAnchor(mainProse) then
      flags += "anchored_support_missing_from_prose"
    if hasConditionalitySupport(supportRows ++ advancedRows) && !mentionsConditionality(mainProse) then
      flags += "conditionality_blur"
    if LineScopedCitation.hasConcreteSanLine(prose) then flags += "branch_citation_present"
    if supportRows.isEmpty && prose.trim.nonEmpty && !LiveNarrativeCompressionCore.hasConcreteAnchor(prose) then
      flags += "sidecar_empty_prose_risk"
    val taxonomyHits = combined.flatMap(taxonomyResidueHits).distinct
    if taxonomyHits.nonEmpty then flags += s"taxonomy_residue:${taxonomyHits.mkString(",")}"
    val internalHits = combined.flatMap(internalLabelHits).distinct
    if internalHits.nonEmpty then flags += s"internal_label:${internalHits.mkString(",")}"
    val uciHits = combined.flatMap(uciLeakHits).distinct
    if uciHits.nonEmpty then flags += s"uci_leak:${uciHits.mkString(",")}"
    if Set(
        SliceKind.CompensationOrExchangeSac,
        SliceKind.Prophylaxis,
        SliceKind.ProphylaxisRestraint,
        SliceKind.TacticalTurn,
        SliceKind.TransitionHeavyEndgames
      ).contains(sliceKind)
    then
      flags += s"priority_slice:$sliceKind"
    flags.toList.distinct

  private def sanitizeSurfaceText(raw: String): Option[String] =
    Option(raw)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def surfaceSupportRow(row: MoveReviewPlayerSurfaceRow): Option[SupportRow] =
    val label = Option(row.label).map(_.trim).filter(_.nonEmpty).getOrElse("Detail")
    surfaceReviewText(row.text, row.refSans, row.authority).map(text => SupportRow(label, text))

  private def surfaceSupportRow(
      label: String,
      text: String,
      refSans: List[String],
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[SupportRow] =
    val cleanLabel = Option(label).map(_.trim).filter(_.nonEmpty).getOrElse("Detail")
    surfaceReviewText(text, refSans, authority).map(clean => SupportRow(cleanLabel, clean))

  private def surfaceReviewText(
      rawText: String,
      refSans: List[String],
      authority: Option[MoveReviewSurfaceAuthority]
  ): Option[String] =
    sanitizeSurfaceText(rawText).map { text =>
      (text :: surfaceMetadataSentences(refSans, authority)).mkString(" ")
    }

  private def surfaceMetadataSentences(
      refSans: List[String],
      authority: Option[MoveReviewSurfaceAuthority]
  ): List[String] =
    val target =
      authority
        .flatMap(cleanAuthorityTarget)
        .map(square => s"Target: $square.")
    val openingBook =
      authority
        .filter(_.kind == MoveReviewSurfaceAuthority.OpeningFamily)
        .flatMap(_.openingBook)
        .flatMap(MoveReviewOpeningBookMetadata.sanitize)
        .map(meta => meta.copy(topMoves = meta.topMoves.filter(m => uciLeakRegex.findFirstIn(m).isEmpty)))
        .flatMap(openingBookSentence)
    val refs = cleanRefSans(refSans)
    val refSentence = Option.when(refs.nonEmpty)(s"Refs: ${refs.mkString(" ")}.")
    List(target, openingBook, refSentence).flatten

  private def cleanAuthorityTarget(authority: MoveReviewSurfaceAuthority): Option[String] =
    authority.target
      .map(_.trim.toLowerCase)
      .filter(chessSquareRegex.matches)
      .filter(square =>
        authority.kind != MoveReviewSurfaceAuthority.OpeningFamily ||
          authority.openingFamily.exists(family => OpeningFamilyCatalog.default.targetAllowed(family, square))
      )

  private def cleanRefSans(values: List[String]): List[String] =
    values
      .flatMap(sanitizeSurfaceText)
      .map(_.trim)
      .filter(value => value.nonEmpty && value.length <= 16 && uciLeakRegex.findFirstIn(value).isEmpty)
      .distinct
      .take(6)

  private def openingBookSentence(metadata: MoveReviewOpeningBookMetadata): Option[String] =
    val bits =
      List(
        metadata.eco.map(eco => s"ECO $eco"),
        metadata.totalGames.map(games => s"${formatOpeningGameCount(games)} games"),
        Option.when(metadata.topMoves.nonEmpty)(s"Book: ${metadata.topMoves.take(3).mkString(" / ")}")
      ).flatten
    Option.when(bits.nonEmpty)(s"Opening book: ${bits.mkString("; ")}.")

  private def formatOpeningGameCount(value: Int): String =
    val count = math.max(0, value)
    if count >= 1000000 then s"${formatDecimal(count / 1000000.0, if count >= 10000000 then 0 else 1)}M"
    else if count >= 1000 then s"${formatDecimal(count / 1000.0, if count >= 10000 then 0 else 1)}k"
    else count.toString

  private def formatDecimal(value: Double, decimals: Int): String =
    java.lang.String.format(java.util.Locale.ROOT, s"%.${decimals}f", java.lang.Double.valueOf(value))

  private def surfaceDecisionComparisonRow(compare: MoveReviewPlayerDecisionComparison): Option[SupportRow] =
    val label = Option(compare.kicker).map(_.trim).filter(_.nonEmpty).getOrElse("Decision compare")
    val engineSan =
      compare.engineSan
        .filterNot(engine => compare.chosenMatchesBest || compare.chosenSan.contains(engine))
        .map(move => s"engine looked at $move")
    val text =
      List(
        compare.chosenSan.map(move => s"played $move"),
        engineSan,
        compare.comparedSan.map(move => s"compared $move"),
        compare.gapLabel.map(gap => s"gap $gap"),
        compare.secondaryText
      ).flatten.mkString(", ")
    sanitizeSurfaceText(text).map(clean => SupportRow(label, clean))

  private def surfaceAuthorRow(row: MoveReviewPlayerAuthorRow): Option[SupportRow] =
    val label = Option(row.title).map(_.trim).filter(_.nonEmpty).getOrElse("Author check")
    val branchTexts =
      row.branches.flatMap { branch =>
        surfaceReviewText(branch.text, branch.refSans, branch.authority).map { text =>
          Option(branch.label).map(_.trim).filter(_.nonEmpty).fold(text)(label => s"$label: $text")
        }
      }
    val text =
      (List(Some(row.question), row.why).flatten ++ branchTexts)
        .flatMap(sanitizeSurfaceText)
        .distinct
        .mkString("; ")
    sanitizeSurfaceText(text).map(clean => SupportRow(label, clean))

  private def buildMoveReviewRowsFromPlayerSurface(surface: MoveReviewPlayerSurface): (List[SupportRow], List[SupportRow]) =
    val support =
      (surface.summaryRows.flatMap(surfaceSupportRow) ++ surface.decisionComparison.flatMap(surfaceDecisionComparisonRow).toList)
        .distinct
    val advanced =
      (surface.advancedRows.flatMap(surfaceSupportRow) ++
        surface.probeRows.flatMap(surfaceSupportRow) ++
        surface.authorRows.flatMap(surfaceAuthorRow)).distinct
    (support, advanced)

  def buildMoveReviewRows(response: CommentResponse): (List[SupportRow], List[SupportRow]) =
    response.moveReviewPlayerSurface match
      case Some(surface) => buildMoveReviewRowsFromPlayerSurface(surface)
      case None          => (Nil, Nil)

  private[tools] def buildMoveReviewRowsFromPlayerSurfaceJson(js: JsValue): Option[(List[SupportRow], List[SupportRow])] =
    val surface = js \ "moveReviewPlayerSurface"
    val schema = (surface \ "schema").asOpt[String]
    Option.when(schema.exists(value =>
      value == "chesstory.move_review.player_surface.v1" ||
        value == "chesstory.move_review.player_surface.v2"
    )) {
      val support =
        rowsFromSurfaceJson(surface \ "summaryRows", allowStrategicRelation = false) ++
          decisionComparisonRowFromJson(surface \ "decisionComparison")
      val advanced =
        rowsFromSurfaceJson(surface \ "advancedRows", allowStrategicRelation = true) ++
          rowsFromSurfaceJson(surface \ "probeRows", allowStrategicRelation = false) ++
          authorRowsFromJson(surface \ "authorRows")
      (support.distinct, advanced.distinct)
    }

  private def rowsFromSurfaceJson(value: JsLookupResult, allowStrategicRelation: Boolean): List[SupportRow] =
    value.asOpt[List[JsObject]].getOrElse(Nil).flatMap { row =>
      for
        label <- (row \ "label").asOpt[String].map(_.trim).filter(_.nonEmpty)
        text <- (row \ "text").asOpt[String]
      yield surfaceSupportRow(
        label = label,
        text = text,
        refSans = (row \ "refSans").asOpt[List[String]].getOrElse(Nil),
        authority = surfaceAuthorityFromJson(row \ "authority", allowStrategicRelation)
      )
    }.flatten

  private def decisionComparisonRowFromJson(value: JsLookupResult): List[SupportRow] =
    value.asOpt[JsObject].toList.flatMap { row =>
      val label = (row \ "kicker").asOpt[String].map(_.trim).filter(_.nonEmpty).getOrElse("Decision compare")
      val chosenSan = (row \ "chosenSan").asOpt[String].map(_.trim).filter(_.nonEmpty)
      val engineSan =
        (row \ "engineSan").asOpt[String].map(_.trim).filter(_.nonEmpty).filterNot { engine =>
          (row \ "chosenMatchesBest").asOpt[Boolean].contains(true) || chosenSan.contains(engine)
        }
      val text =
        List(
          chosenSan.map(move => s"played $move"),
          engineSan.map(move => s"engine looked at $move"),
          (row \ "comparedSan").asOpt[String].map(move => s"compared ${move.trim}"),
          (row \ "gapLabel").asOpt[String].map(gap => s"gap ${gap.trim}"),
          (row \ "secondaryText").asOpt[String]
        ).flatten.mkString(", ")
      sanitizeSurfaceText(text).map(clean => SupportRow(label, clean))
    }

  private def authorRowsFromJson(value: JsLookupResult): List[SupportRow] =
    value.asOpt[List[JsObject]].getOrElse(Nil).flatMap { row =>
      val label = (row \ "title").asOpt[String].map(_.trim).filter(_.nonEmpty).getOrElse("Author check")
      val branchText =
        (row \ "branches").asOpt[List[JsObject]].getOrElse(Nil).flatMap { branch =>
          val branchLabel = (branch \ "label").asOpt[String].map(_.trim).filter(_.nonEmpty)
          (branch \ "text").asOpt[String].flatMap { text =>
            surfaceReviewText(
              text,
              refSans = (branch \ "refSans").asOpt[List[String]].getOrElse(Nil),
              authority = surfaceAuthorityFromJson(branch \ "authority", allowStrategicRelation = false)
            )
          }.map { text =>
            branchLabel.fold(text)(value => s"$value: $text")
          }
        }
      val text =
        (List((row \ "question").asOpt[String], (row \ "why").asOpt[String]).flatten ++ branchText)
          .flatMap(sanitizeSurfaceText)
          .distinct
          .mkString("; ")
      Option.when(text.nonEmpty)(SupportRow(label, text))
    }

  private def surfaceAuthorityFromJson(
      value: JsLookupResult,
      allowStrategicRelation: Boolean
  ): Option[MoveReviewSurfaceAuthority] =
    value.asOpt[JsObject].flatMap { row =>
      val kind = (row \ "kind").asOpt[String].map(_.trim).getOrElse("")
      val token = (row \ "token").asOpt[String].map(_.trim).filter(_.nonEmpty)
      val openingFamily = (row \ "openingFamily").asOpt[String].map(_.trim).filter(surfaceAuthorityKeyRegex.matches)
      val target = (row \ "target").asOpt[String].map(_.trim.toLowerCase).filter(chessSquareRegex.matches)
      val openingBook =
        Option.when(kind == MoveReviewSurfaceAuthority.OpeningFamily)(
          MoveReviewOpeningBookMetadata(
            eco = (row \ "openingBook" \ "eco").asOpt[String],
            totalGames = (row \ "openingBook" \ "totalGames").asOpt[Int],
            topMoves = (row \ "openingBook" \ "topMoves").asOpt[List[String]].getOrElse(Nil)
          )
        ).flatMap(MoveReviewOpeningBookMetadata.sanitize)
      val authority =
        MoveReviewSurfaceAuthority(
          kind = kind,
          token = token,
          openingFamily = openingFamily,
          target = target,
          openingBook = openingBook
        )
      Option.when(validReviewSurfaceAuthority(authority, allowStrategicRelation))(authority)
    }

  private def validReviewSurfaceAuthority(
      authority: MoveReviewSurfaceAuthority,
      allowStrategicRelation: Boolean
  ): Boolean =
    authority.kind match
      case MoveReviewSurfaceAuthority.CounterplayBreak =>
        authority.token.exists(surfaceAuthorityTokenRegex.matches) &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.CentralBreak |
          MoveReviewSurfaceAuthority.CentralLiquidation |
          MoveReviewSurfaceAuthority.CentralChallenge =>
        authority.token.exists(surfaceAuthorityRouteTokenRegex.matches) &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.PracticalPlan =>
        authority.token.isEmpty &&
          authority.openingFamily.isEmpty &&
          authority.target.isEmpty &&
          authority.openingBook.isEmpty
      case MoveReviewSurfaceAuthority.OpeningFamily =>
        authority.openingFamily.nonEmpty &&
          authority.token.isEmpty
      case MoveReviewSurfaceAuthority.StrategicRelation =>
        allowStrategicRelation &&
          authority.token.exists(token =>
            surfaceAuthorityKeyRegex.matches(token) && RelationObservationCatalog.ImplementedKinds.contains(token)
          ) &&
          authority.target.nonEmpty &&
          authority.openingFamily.isEmpty &&
          authority.openingBook.isEmpty
      case _ =>
        false

  def moveReviewPlannerRuntime(snapshot: SliceSnapshot): MoveReviewRuntimeTrace =
    val rawCtx = snapshot.rawCtx.getOrElse(snapshot.ctx)
    val outline =
      BookStyleRenderer.validatedOutline(
        snapshot.ctx,
        truthContract = snapshot.truthContract,
        strategyPack = snapshot.strategyPack
      )
    val candidateEvidence =
      MoveReviewCompressionPolicy.candidateEvidenceLines(snapshot.refs, snapshot.ctx)
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(
        snapshot.ctx,
        snapshot.strategyPack,
        truthContract = snapshot.truthContract,
        candidateEvidenceLines = candidateEvidence
      )
    val rankedPlans =
      QuestionFirstCommentaryPlanner.plan(snapshot.ctx, plannerInputs, truthContract = snapshot.truthContract)
    val renderSelection =
      MoveReviewCompressionPolicy.renderSelection(
        plannerInputs,
        rankedPlans,
        truthContract = snapshot.truthContract
      )
    val quietSupportGateTrace =
      QuietStrategicSupportComposer.diagnose(
        snapshot.ctx,
        plannerInputs,
        rankedPlans,
        snapshot.strategyPack
      )
    val contrastTrace =
      renderSelection
        .map(_.contrastTrace)
        .getOrElse(ContrastiveSupportAdmissibility.ContrastSupportTrace())
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        snapshot.ctx,
        plannerInputs,
        rankedPlans,
        snapshot.truthContract,
        snapshot.refs
      )
    val plannerOwnedSlots =
      MoveReviewCompressionPolicy.buildSlots(
        ctx = snapshot.ctx,
        outline = outline,
        refs = snapshot.refs,
        strategyPack = snapshot.strategyPack,
        truthContract = snapshot.truthContract
      )
    val quietSupportTrace =
      if plannerOwnedSlots.nonEmpty then
        MoveReviewQuietSupportTrace(
          rejectReasons = List("planner_owned_row"),
          runtimeGatePassed = Some(quietSupportGateTrace.gatePassed),
          runtimeGateRejectReasons = quietSupportGateTrace.gate.rejectReasons,
          runtimeSceneType = Some(quietSupportGateTrace.gate.sceneType),
          runtimeSelectedOwnerKind = quietSupportGateTrace.gate.selectedOwnerKind,
          runtimeSelectedSource = quietSupportGateTrace.gate.selectedSource,
          runtimePvDeltaAvailable = Some(quietSupportGateTrace.gate.pvDeltaAvailable),
          runtimeSignalDigestAvailable = Some(quietSupportGateTrace.gate.signalDigestAvailable),
          runtimeMoveLinkedPvDeltaAnchorAvailable = Some(quietSupportGateTrace.gate.moveLinkedPvDeltaAnchorAvailable)
        )
      else
        val trace =
          MoveReviewCompressionPolicy.exactFactualQuietSupportTrace(
            ctx = snapshot.ctx,
            refs = snapshot.refs,
            strategyPack = snapshot.strategyPack,
            truthContract = snapshot.truthContract
          )
        MoveReviewQuietSupportTrace(
          liftApplied = trace.liftApplied,
          rejectReasons = trace.rejectReasons,
          runtimeGatePassed = Some(trace.composerTrace.gatePassed),
          runtimeGateRejectReasons = trace.composerTrace.gate.rejectReasons,
          runtimeSceneType = Some(trace.composerTrace.gate.sceneType),
          runtimeSelectedOwnerKind = trace.composerTrace.gate.selectedOwnerKind,
          runtimeSelectedSource = trace.composerTrace.gate.selectedSource,
          runtimePvDeltaAvailable = Some(trace.composerTrace.gate.pvDeltaAvailable),
          runtimeSignalDigestAvailable = Some(trace.composerTrace.gate.signalDigestAvailable),
          runtimeMoveLinkedPvDeltaAnchorAvailable = Some(trace.composerTrace.gate.moveLinkedPvDeltaAnchorAvailable),
          candidateBucket = trace.composerTrace.line.map(_.bucket),
          candidateSourceKinds = trace.composerTrace.line.map(_.sourceKinds).getOrElse(Nil),
          candidateVerbFamily = trace.composerTrace.line.map(_.verbFamily),
          candidateText = trace.composerTrace.line.map(_.text),
          factualSentence = trace.factualSentence
        )
    val slots =
      plannerOwnedSlots.getOrElse(
        MoveReviewPolishSlotsBuilder.buildOrFallback(
          ctx = snapshot.ctx,
          outline = outline,
          refs = snapshot.refs,
          strategyPack = snapshot.strategyPack,
          truthContract = snapshot.truthContract
        )
      )
    val coverageTrace =
      MoveReviewCoverageDiagnostics.build(
        ctx = snapshot.ctx,
        refs = snapshot.refs,
        strategyPack = snapshot.strategyPack,
        truthContract = snapshot.truthContract,
        slots = slots,
        plannerInputs = plannerInputs,
        causalTrace = causalTrace
      )
    val deterministicProse =
      Option(LiveNarrativeCompressionCore.deterministicProse(slots)).map(_.trim).getOrElse("")
    val rawPvDelta = rawCtx.decision.map(_.delta)
    val sanitizedPvDelta = snapshot.ctx.decision.map(_.delta)
    val tacticalFailureSources = plannerCandidateSources(rankedPlans, "TacticalFailure")
    val forcingDefenseSources = plannerCandidateSources(rankedPlans, "ForcingDefense")
    val moveDeltaSources = plannerCandidateSources(rankedPlans, "MoveDelta")
    val prose =
      EarlyOpeningNarrationPolicy.clampNarrative(
        snapshot.ctx,
        lila.commentary.CommentaryApi.sanitizeMoveReviewProse(
          if deterministicProse.nonEmpty then deterministicProse
          else exactFactualReviewProse(snapshot)
        ),
        snapshot.truthContract
      )
    MoveReviewRuntimeTrace(
      planner =
        MoveReviewPlannerTrace(
          primaryKind = rankedPlans.primary.map(_.questionKind.toString),
          primaryFallbackMode =
            rankedPlans.primary.map(_.fallbackMode.toString)
              .orElse(rankedPlans.rejected.headOption.map(_.fallbackMode.toString)),
          secondaryKind = rankedPlans.secondary.map(_.questionKind.toString),
          secondarySurfaced =
            rankedPlans.secondary.nonEmpty && plannerOwnedSlots.exists(_.supportSecondary.nonEmpty),
          moveReviewFallbackMode = if plannerOwnedSlots.nonEmpty then "planner_owned" else "exact_factual",
          sceneType = Some(rankedPlans.ownerTrace.sceneType.wireName),
          sceneReasons = rankedPlans.ownerTrace.sceneReasons,
          ownerCandidates = rankedPlans.ownerTrace.ownerCandidateLabels,
          admittedOwners = rankedPlans.ownerTrace.admittedPlannerOwnerLabels,
          droppedOwners = rankedPlans.ownerTrace.droppedPlannerOwnerLabels,
          supportMaterialSeparation = rankedPlans.ownerTrace.supportMaterialSeparationLabels,
          proposedOwnerMappings = rankedPlans.ownerTrace.proposedOwnerMappingLabels,
          demotionReasons = rankedPlans.ownerTrace.demotionReasons,
          selectedQuestion = rankedPlans.ownerTrace.selectedQuestion.map(_.toString),
          selectedOwnerKind = rankedPlans.ownerTrace.selectedPlannerOwnerKind.map(_.wireName),
          selectedSource = rankedPlans.ownerTrace.selectedPlannerSource,
          rawChoiceType = Some(rawCtx.header.choiceType),
          rawDecisionPresent = Some(rawCtx.decision.nonEmpty),
          rawDecisionIngressReason = Some(decisionIngressReason(rawCtx)),
          rawPvDeltaAvailable = Some(rawPvDelta.nonEmpty),
          rawPvDeltaIngressReason = Some(pvDeltaIngressReason(rawCtx)),
          rawPvDeltaResolvedThreatsPresent = Some(rawPvDelta.exists(_.resolvedThreats.nonEmpty)),
          rawPvDeltaNewOpportunitiesPresent = Some(rawPvDelta.exists(_.newOpportunities.nonEmpty)),
          rawPvDeltaPlanAdvancementsPresent = Some(rawPvDelta.exists(_.planAdvancements.nonEmpty)),
          rawPvDeltaConcessionsPresent = Some(rawPvDelta.exists(_.concessions.nonEmpty)),
          sanitizedDecisionPresent = Some(snapshot.ctx.decision.nonEmpty),
          sanitizedDecisionIngressReason = Some(decisionIngressReason(snapshot.ctx)),
          sanitizedPvDeltaAvailable = Some(sanitizedPvDelta.nonEmpty),
          sanitizedPvDeltaIngressReason = Some(pvDeltaIngressReason(snapshot.ctx)),
          truthClass = snapshot.truthContract.map(_.truthClass.toString),
          truthReasonFamily = snapshot.truthContract.map(_.reasonFamily.toString),
          truthFailureMode = snapshot.truthContract.map(_.failureMode.toString),
          truthChosenMatchesBest = snapshot.truthContract.map(_.chosenMatchesBest),
          truthOnlyMoveDefense = snapshot.truthContract.map(_.reasonFamily.toString == "OnlyMoveDefense"),
          truthBenchmarkCriticalMove = snapshot.truthContract.map(_.benchmarkCriticalMove),
          tacticalFailureSources = tacticalFailureSources,
          forcingDefenseSources = forcingDefenseSources,
          moveDeltaSources = moveDeltaSources,
          surfaceReplayOutcome =
            Some(if plannerOwnedSlots.nonEmpty then "move_review_planner_owned" else "move_review_exact_factual"),
          contrastSourceKind = contrastTrace.contrast_source_kind,
          contrastAnchor = contrastTrace.contrast_anchor,
          contrastConsequence = contrastTrace.contrast_consequence,
          contrastAdmissible = contrastTrace.contrast_admissible,
          contrastRejectReason = contrastTrace.contrast_reject_reason,
          contrastReplacementUsed =
            contrastTrace.contrast_admissible &&
              contrastTrace.contrast_sentence.exists(sentence =>
                renderSelection.flatMap(_.primary.contrast).forall(existing =>
                  !sentence.equalsIgnoreCase(existing.trim)
                )
              ),
          causalClaimStatus = causalTrace.map(_.status),
          causalClaimQuestion = causalTrace.map(_.questionKind),
          causalClaimSubject = causalTrace.flatMap(_.subjectRole),
          causalClaimEvidence = causalTrace.map(_.evidenceKinds).getOrElse(Nil),
          causalClaimRelations = causalTrace.map(_.relationKinds).getOrElse(Nil),
          causalClaimRejectReasons = causalTrace.map(_.rejectReasons).getOrElse(Nil),
          causalClaimSupportEmbedded = causalTrace.flatMap(_.supportRenderedInClaim),
          causalClaimGuardrail = causalTrace.flatMap(_.guardrail)
      ),
      prose = prose,
      quietSupport = quietSupportTrace,
      coverage = coverageTrace
    )

  def moveReviewPlannerTrace(snapshot: SliceSnapshot): MoveReviewPlannerTrace =
    moveReviewPlannerRuntime(snapshot).planner

  private def plannerCandidateSources(
      rankedPlans: lila.commentary.analysis.RankedQuestionPlans,
      ownerKindWireName: String
  ): List[String] =
    rankedPlans.ownerTrace.ownerCandidates
      .filter(_.plannerOwnerKind.wireName == ownerKindWireName)
      .map(_.source)
      .distinct
      .sorted

  private def decisionIngressReason(ctx: NarrativeContext): String =
    val choiceType = Option(ctx.header.choiceType).getOrElse("")
    if ctx.decision.nonEmpty then "decision_present"
    else if choiceType == "StyleChoice" then "style_choice_decision_omitted"
    else "decision_missing_before_planner"

  private def pvDeltaIngressReason(ctx: NarrativeContext): String =
    val delta = ctx.decision.map(_.delta)
    if delta.isEmpty then
      if Option(ctx.header.choiceType).contains("StyleChoice") then "style_choice_pv_delta_unavailable"
      else "pv_delta_unavailable_without_decision"
    else if delta.exists(pv =>
        pv.resolvedThreats.nonEmpty ||
          pv.newOpportunities.nonEmpty ||
          pv.planAdvancements.nonEmpty ||
          pv.concessions.nonEmpty
      )
    then "pv_delta_present_with_content"
    else "pv_delta_present_but_empty"

  private def fenAfterEachMove(startFen: String, ucis: List[String]): List[String] =
    var current = startFen
    ucis.map { uci =>
      current = NarrativeUtils.uciListToFen(current, List(uci))
      current
    }

  private def markerForPly(ply: Int): String =
    val moveNo = (ply + 1) / 2
    if ply % 2 == 1 then s"$moveNo."
    else s"$moveNo..."

  private def exactFactualReviewProse(snapshot: SliceSnapshot): String =
    val san = snapshot.ctx.playedSan.orElse(Option(snapshot.plyData.playedMove).filter(_.trim.nonEmpty))
    val header =
      san.map(move => s"${markerForPly(snapshot.plyData.ply)} $move:")
        .getOrElse("")
    QuietMoveIntentBuilder.exactFactualSentence(snapshot.ctx)
      .map(sentence => List(header, sentence).filter(_.nonEmpty).mkString(" ").trim)
      .orElse {
        san.map(move => s"${markerForPly(snapshot.plyData.ply)} $move: This is the move played.")
      }
      .getOrElse("")

  private def buildMoveReviewRefs(
      fenBefore: String,
      variations: List[VariationLine]
  ): Option[MoveReviewRefs] =
    if variations.isEmpty then None
    else
      val startPly = NarrativeUtils.plyFromFen(fenBefore).map(_ + 1).getOrElse(1)
      val lines = variations.zipWithIndex.map { case (line, lineIdx) =>
        val sanList = NarrativeUtils.uciListToSan(fenBefore, line.moves)
        val uciList = line.moves.take(sanList.size)
        val fensAfter = fenAfterEachMove(fenBefore, uciList).take(sanList.size)
        val size = List(sanList.size, uciList.size, fensAfter.size).min
        val moves = (0 until size).toList.map { i =>
          val ply = startPly + i
          MoveReviewMoveRef(
            refId = f"l${lineIdx + 1}%02d_m${i + 1}%02d",
            san = sanList(i),
            uci = uciList(i),
            fenAfter = fensAfter(i),
            ply = ply,
            moveNo = (ply + 1) / 2,
            marker = Some(markerForPly(ply))
          )
        }
        MoveReviewVariationRef(
          lineId = f"line_${lineIdx + 1}%02d",
          scoreCp = line.scoreCp,
          mate = line.mate,
          depth = line.depth,
          moves = moves
        )
      }
      Some(
        MoveReviewRefs(
          startFen = fenBefore,
          startPly = startPly,
          variations = lines
        )
      )

  final class LocalUciEngine(enginePath: Path, timeoutMs: Long = DefaultUciTimeoutMs):
    private val process =
      new ProcessBuilder(enginePath.toAbsolutePath.normalize.toString)
        .redirectErrorStream(true)
        .start()
    private val lines = LinkedBlockingQueue[String]()
    private val writer =
      new BufferedWriter(new OutputStreamWriter(process.getOutputStream, StandardCharsets.UTF_8))
    private val reader = new Thread(() => pumpOutput(), "commentary-player-qc-engine")
    @volatile private var closed = false
    private var resolvedEngineName = enginePath.getFileName.toString

    reader.setDaemon(true)
    reader.start()
    initialize()

    def engineName: String = resolvedEngineName

    def newGame(): Unit =
      send("ucinewgame")
      ready()

    def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      drainPending()
      send(s"setoption name MultiPV value $multiPv")
      send(s"position fen $fen")
      send(s"go depth $depth")

      val perspectiveSign = whitePerspectiveSign(fen)
      val byPv = scala.collection.mutable.Map.empty[Int, ParsedInfo]
      var bestMove: Option[String] = None
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var done = false

      while !done do
        val line = awaitLine(deadline)
        if line.startsWith("info ") then
          parseInfoLine(line).foreach { info =>
            val prev = byPv.get(info.multiPv)
            if prev.forall(p => info.depth > p.depth || (info.depth == p.depth && info.moves.size >= p.moves.size)) then
              byPv.update(info.multiPv, info)
          }
        else if line.startsWith("bestmove") then
          bestMove =
            line
              .split("\\s+")
              .lift(1)
              .map(_.trim)
              .filter(move => move.nonEmpty && move != "(none)")
          done = true

      val normalized =
        byPv.toList.sortBy(_._1).map { case (_, info) =>
          if info.scoreType == "mate" then
            VariationLine(
              moves = info.moves,
              scoreCp = 0,
              mate = Some(perspectiveSign * info.scoreValue),
              depth = info.depth
            )
          else
            VariationLine(
              moves = info.moves,
              scoreCp = perspectiveSign * info.scoreValue,
              mate = None,
              depth = info.depth
            )
        }
      if normalized.nonEmpty then normalized
      else bestMove.toList.map(move => VariationLine(moves = List(move), scoreCp = 0, mate = None, depth = 0))

    def close(): Unit =
      if !closed then
        closed = true
        try send("quit")
        catch case _: Throwable => ()
        writer.close()
        if process.isAlive then process.destroy()

    private def initialize(): Unit =
      send("uci")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var uciOk = false
      while !uciOk do
        val line = awaitLine(deadline)
        if line.startsWith("id name ") then resolvedEngineName = line.stripPrefix("id name ").trim
        else if line == "uciok" then uciOk = true
      send("setoption name Threads value 1")
      send("setoption name Hash value 64")
      ready()

    private def ready(): Unit =
      send("isready")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var isReady = false
      while !isReady do
        val line = awaitLine(deadline)
        if line == "readyok" then isReady = true

    private def drainPending(): Unit =
      while lines.poll() != null do ()

    private def send(cmd: String): Unit =
      writer.write(cmd)
      writer.newLine()
      writer.flush()

    private def pumpOutput(): Unit =
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream, StandardCharsets.UTF_8))
      try
        var line = reader.readLine()
        while line != null do
          lines.offer(line)
          line = reader.readLine()
      catch case NonFatal(_) => ()
      finally reader.close()

    private def awaitLine(deadlineNs: Long): String =
      val remainingMs = math.max(1L, (deadlineNs - System.nanoTime()) / 1000000L)
      val line = lines.poll(remainingMs, TimeUnit.MILLISECONDS)
      if line == null then throw new IllegalStateException(s"uci engine timed out after ${timeoutMs}ms")
      line

    private def whitePerspectiveSign(fen: String): Int =
      val whiteToMove = PgnAnalysisHelper.sideToMoveFromFen(fen).contains(chess.Color.White)
      if whiteToMove then 1 else -1

    private final case class ParsedInfo(
        depth: Int,
        multiPv: Int,
        scoreType: String,
        scoreValue: Int,
        moves: List[String]
    )

    private def parseInfoLine(line: String): Option[ParsedInfo] =
      val tokens = line.split("\\s+").toList
      val depth = tokenValue(tokens, "depth").flatMap(_.toIntOption)
      val multiPv = tokenValue(tokens, "multipv").flatMap(_.toIntOption).orElse(Some(1))
      val scoreIdx = tokens.indexOf("score")
      val scoreType = if scoreIdx >= 0 then tokens.lift(scoreIdx + 1) else None
      val scoreValue = if scoreIdx >= 0 then tokens.lift(scoreIdx + 2).flatMap(_.toIntOption) else None
      val pvIdx = tokens.indexOf("pv")
      val moves = if pvIdx >= 0 then tokens.drop(pvIdx + 1).filter(_.nonEmpty) else Nil
      for
        d <- depth
        mpv <- multiPv
        st <- scoreType
        sv <- scoreValue
        if moves.nonEmpty
      yield ParsedInfo(d, mpv, st, sv, moves)

    private def tokenValue(tokens: List[String], name: String): Option[String] =
      val idx = tokens.indexOf(name)
      if idx >= 0 then tokens.lift(idx + 1) else None
