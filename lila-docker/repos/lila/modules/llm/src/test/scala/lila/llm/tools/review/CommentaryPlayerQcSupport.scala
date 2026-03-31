package lila.llm.tools.review

import play.api.libs.json.*

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.Instant
import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import lila.llm.*
import lila.llm.analysis.{ BookStyleRenderer, BookmakerLiveCompressionPolicy, BookmakerPolishSlotsBuilder, CommentaryEngine, DecisiveTruth, EarlyOpeningNarrationPolicy, LineScopedCitation, LiveNarrativeCompressionCore, NarrativeContextBuilder, NarrativeSignalDigestBuilder, NarrativeUtils, QuestionFirstCommentaryPlanner, QuestionPlannerInputsBuilder, QuietMoveIntentBuilder, StrategyPackBuilder, UserFacingSignalSanitizer }
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.model.NarrativeRenderMode
import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.AuthorQuestionKind
import lila.llm.model.strategic.VariationLine

object CommentaryPlayerQcSupport:

  val ExternalRoot: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc")
  val DefaultRawPgnDir: Path = ExternalRoot.resolve("raw-pgn")
  val DefaultCatalogDir: Path = ExternalRoot.resolve("catalog")
  val DefaultManifestDir: Path = ExternalRoot.resolve("manifests")
  val DefaultBookmakerRunDir: Path = ExternalRoot.resolve("runs").resolve("bookmaker")
  val DefaultChronicleRunDir: Path = ExternalRoot.resolve("runs").resolve("chronicle")
  val DefaultReviewDir: Path = ExternalRoot.resolve("reviews")
  val DefaultReportDir: Path = ExternalRoot.resolve("reports")
  val DefaultTruthInventoryDir: Path = ExternalRoot.resolve("inventory")
  val DefaultTruthInventoryPath: Path =
    DefaultTruthInventoryDir.resolve("RealPgnNarrativeEvalTruthInventory.json")
  val TruthInventoryLookupPaths: List[Path] =
    List(DefaultTruthInventoryPath)

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
    val Bookmaker = "bookmaker"
    val Chronicle = "chronicle"
    val ActiveNote = "active_note"

  object ReviewKind:
    val FocusMoment = "focus_moment"
    val WholeGame = "whole_game"
    val ActiveParity = "active_parity"
    val BookmakerFocus = "bookmaker_focus"

  object WholeGameSliceKind:
    val ChronicleWholeGame = "chronicle_whole_game"
    val ChronicleFocus = "chronicle_focus"
    val ActiveParity = "active_parity"
    val BookmakerFocus = "bookmaker_focus"

  object FixFamily:
    val WholeGamePlanDrift = "whole_game_plan_drift"
    val SideAsymmetryOrMissingSidePlan = "side_asymmetry_or_missing_side_plan"
    val TurningPointUnderexplained = "turning_point_underexplained"
    val BlunderWithoutPunishFeedback = "blunder_without_punish_feedback"
    val MissedPunishUnderexplained = "missed_punish_underexplained"
    val ResultPayoffVerdictMismatch = "result_payoff_verdict_mismatch"
    val GenericTensionPeakOverload = "generic_tensionpeak_overload"
    val ConcreteAnchorMissingInLongTermStory = "concrete_anchor_missing_in_long_term_story"
    val GenericFillerMainProse = "generic_filler_main_prose"
    val AnchoredSupportMissingFromProse = "anchored_support_missing_from_prose"
    val ConditionalityBlur = "conditionality_blur"
    val MisanchoredConcreteClaim = "misanchored_concrete_claim"
    val StrategicFlattening = "strategic_flattening"
    val ChronicleActiveStoryDrift = "chronicle_active_story_drift"
    val ActiveNoteMissingContract = "active_note_missing_contract"
    val AnchorlessActiveContinuation = "anchorless_active_continuation"
    val DryContractNote = "dry_contract_note"

    val all = List(
      WholeGamePlanDrift,
      SideAsymmetryOrMissingSidePlan,
      TurningPointUnderexplained,
      BlunderWithoutPunishFeedback,
      MissedPunishUnderexplained,
      ResultPayoffVerdictMismatch,
      GenericTensionPeakOverload,
      ConcreteAnchorMissingInLongTermStory,
      GenericFillerMainProse,
      AnchoredSupportMissingFromProse,
      ConditionalityBlur,
      MisanchoredConcreteClaim,
      StrategicFlattening,
      ChronicleActiveStoryDrift,
      ActiveNoteMissingContract,
      AnchorlessActiveContinuation,
      DryContractNote
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

  final case class ChronicleCorpusGame(
      id: String,
      tier: String,
      family: String,
      label: String,
      notes: List[String],
      expectedThemes: List[String],
      pgn: String
  )
  object ChronicleCorpusGame:
    given Format[ChronicleCorpusGame] = Json.format[ChronicleCorpusGame]

  final case class ChronicleCorpus(
      version: Int = 1,
      generatedAt: String,
      asOfDate: String,
      title: String,
      description: String,
      games: List[ChronicleCorpusGame]
  )
  object ChronicleCorpus:
    given Format[ChronicleCorpus] = Json.format[ChronicleCorpus]

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

  final case class BookmakerQuietSupportTrace(
      liftApplied: Boolean = false,
      rejectReasons: List[String] = Nil,
      runtimeGatePassed: Option[Boolean] = None,
      runtimeGateRejectReasons: List[String] = Nil,
      runtimeSceneType: Option[String] = None,
      runtimeSelectedOwnerFamily: Option[String] = None,
      runtimeSelectedOwnerSource: Option[String] = None,
      runtimePvDeltaAvailable: Option[Boolean] = None,
      runtimeSignalDigestAvailable: Option[Boolean] = None,
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = None,
      candidateBucket: Option[String] = None,
      candidateSourceKinds: List[String] = Nil,
      candidateVerbFamily: Option[String] = None,
      candidateText: Option[String] = None,
      factualSentence: Option[String] = None
  )
  object BookmakerQuietSupportTrace:
    given Format[BookmakerQuietSupportTrace] = Json.format[BookmakerQuietSupportTrace]

  final case class ChronicleQuietSupportTrace(
      applied: Boolean = false,
      rejectReasons: List[String] = Nil,
      runtimeGatePassed: Option[Boolean] = None,
      runtimeGateRejectReasons: List[String] = Nil,
      runtimeSceneType: Option[String] = None,
      runtimeSelectedOwnerFamily: Option[String] = None,
      runtimeSelectedOwnerSource: Option[String] = None,
      runtimePvDeltaAvailable: Option[Boolean] = None,
      runtimeSignalDigestAvailable: Option[Boolean] = None,
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = None,
      candidateBucket: Option[String] = None,
      candidateSourceKinds: List[String] = Nil,
      candidateVerbFamily: Option[String] = None,
      candidateText: Option[String] = None
  )
  object ChronicleQuietSupportTrace:
    given Format[ChronicleQuietSupportTrace] = Json.format[ChronicleQuietSupportTrace]

  final case class BookmakerOutputEntry(
      sampleId: String,
      gameKey: String,
      sliceKind: String,
      targetPly: Int,
      fen: String,
      playedSan: String,
      playedUci: String,
      opening: Option[String],
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
      bookmakerFallbackMode: String = "unknown",
      plannerSceneType: Option[String] = None,
      plannerSceneReasons: List[String] = Nil,
      plannerOwnerCandidates: List[String] = Nil,
      plannerAdmittedFamilies: List[String] = Nil,
      plannerDroppedFamilies: List[String] = Nil,
      plannerSupportMaterialSeparation: List[String] = Nil,
      plannerProposedFamilyMappings: List[String] = Nil,
      plannerDemotionReasons: List[String] = Nil,
      plannerSelectedQuestion: Option[String] = None,
      plannerSelectedOwnerFamily: Option[String] = None,
      plannerSelectedOwnerSource: Option[String] = None,
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
      bookmakerSnapshotDigestHash: Option[String] = None,
      bookmakerCarryDigestHash: Option[String] = None,
      bookmakerAugmentationDigestHash: Option[String] = None,
      bookmakerBundleDigestHash: Option[String] = None,
      quietSupportLiftApplied: Option[Boolean] = None,
      quietSupportRejectReasons: List[String] = Nil,
      quietSupportRuntimeGatePassed: Option[Boolean] = None,
      quietSupportRuntimeGateRejectReasons: List[String] = Nil,
      quietSupportRuntimeSceneType: Option[String] = None,
      quietSupportRuntimeSelectedOwnerFamily: Option[String] = None,
      quietSupportRuntimeSelectedOwnerSource: Option[String] = None,
      quietSupportRuntimePvDeltaAvailable: Option[Boolean] = None,
      quietSupportRuntimeSignalDigestAvailable: Option[Boolean] = None,
      quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean] = None,
      quietSupportCandidateBucket: Option[String] = None,
      quietSupportCandidateSourceKinds: List[String] = Nil,
      quietSupportCandidateVerbFamily: Option[String] = None,
      quietSupportCandidateText: Option[String] = None,
      quietSupportFactualSentence: Option[String] = None
  ):
    def withQuietSupportTrace(quietSupportTrace: BookmakerQuietSupportTrace): BookmakerOutputEntry =
      copy(
        quietSupportLiftApplied = Some(quietSupportTrace.liftApplied),
        quietSupportRejectReasons = quietSupportTrace.rejectReasons,
        quietSupportRuntimeGatePassed = quietSupportTrace.runtimeGatePassed,
        quietSupportRuntimeGateRejectReasons = quietSupportTrace.runtimeGateRejectReasons,
        quietSupportRuntimeSceneType = quietSupportTrace.runtimeSceneType,
        quietSupportRuntimeSelectedOwnerFamily = quietSupportTrace.runtimeSelectedOwnerFamily,
        quietSupportRuntimeSelectedOwnerSource = quietSupportTrace.runtimeSelectedOwnerSource,
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
  object BookmakerOutputEntry:
    private val LegacyToStableQuietSupportFields = List(
      "track3QuietSupportLiftApplied" -> "quietSupportLiftApplied",
      "track3QuietSupportRejectReasons" -> "quietSupportRejectReasons",
      "track3RuntimeGatePassed" -> "quietSupportRuntimeGatePassed",
      "track3RuntimeGateRejectReasons" -> "quietSupportRuntimeGateRejectReasons",
      "track3RuntimeSceneType" -> "quietSupportRuntimeSceneType",
      "track3RuntimeSelectedOwnerFamily" -> "quietSupportRuntimeSelectedOwnerFamily",
      "track3RuntimeSelectedOwnerSource" -> "quietSupportRuntimeSelectedOwnerSource",
      "track3RuntimePvDeltaAvailable" -> "quietSupportRuntimePvDeltaAvailable",
      "track3RuntimeSignalDigestAvailable" -> "quietSupportRuntimeSignalDigestAvailable",
      "track3RuntimeMoveLinkedPvDeltaAnchorAvailable" -> "quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable",
      "track3QuietSupportCandidateBucket" -> "quietSupportCandidateBucket",
      "track3QuietSupportCandidateSourceKinds" -> "quietSupportCandidateSourceKinds",
      "track3QuietSupportCandidateVerbFamily" -> "quietSupportCandidateVerbFamily",
      "track3QuietSupportCandidateText" -> "quietSupportCandidateText",
      "track3QuietSupportFactualSentence" -> "quietSupportFactualSentence"
    )

    private val reads0: Reads[BookmakerOutputEntry] = Json.using[Json.WithDefaultValues].reads[BookmakerOutputEntry]
    private val writes0: OWrites[BookmakerOutputEntry] = Json.writes[BookmakerOutputEntry]

    private def normalizeForRead(js: JsObject): JsObject =
      LegacyToStableQuietSupportFields.foldLeft(js) { case (acc, (legacyKey, stableKey)) =>
        if acc.keys.contains(stableKey) then acc
        else
          (acc \ legacyKey).toOption match
            case Some(value) => acc + (stableKey -> value)
            case None        => acc
      }

    given Format[BookmakerOutputEntry] = Format(
      Reads {
        case obj: JsObject => reads0.reads(normalizeForRead(obj))
        case _             => JsError("error.expected.jsobject")
      },
      writes0
    )

  final case class BookmakerPlannerTrace(
      primaryKind: Option[String] = None,
      primaryFallbackMode: Option[String] = None,
      secondaryKind: Option[String] = None,
      secondarySurfaced: Boolean = false,
      bookmakerFallbackMode: String = "unknown",
      sceneType: Option[String] = None,
      sceneReasons: List[String] = Nil,
      ownerCandidates: List[String] = Nil,
      admittedFamilies: List[String] = Nil,
      droppedFamilies: List[String] = Nil,
      supportMaterialSeparation: List[String] = Nil,
      proposedFamilyMappings: List[String] = Nil,
      demotionReasons: List[String] = Nil,
      selectedQuestion: Option[String] = None,
      selectedOwnerFamily: Option[String] = None,
      selectedOwnerSource: Option[String] = None,
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
      contrastReplacementUsed: Boolean = false
  )

  final case class BookmakerRuntimeTrace(
      planner: BookmakerPlannerTrace,
      prose: String,
      quietSupport: BookmakerQuietSupportTrace = BookmakerQuietSupportTrace()
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
      reviewKind: String = ReviewKind.FocusMoment,
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
          surface = (js \ "surface").asOpt[String].getOrElse(ReviewSurface.Bookmaker),
          reviewKind = (js \ "reviewKind").asOpt[String].getOrElse(ReviewKind.FocusMoment),
          sliceKind = (js \ "sliceKind").asOpt[String].getOrElse(SliceKind.StrategicChoice),
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

  final case class ChronicleQueueReport(
      version: Int,
      generatedAt: String,
      bookmakerOutputCount: Int,
      chronicleMomentCount: Int,
      wholeGameReviewCount: Int = 0,
      mandatoryReviewCount: Int,
      sampledReviewCount: Int,
      reviewedCount: Int,
      fullReview: Boolean = false,
      auditSetGameCount: Int = 0
  )
  object ChronicleQueueReport:
    given Writes[ChronicleQueueReport] = Json.writes[ChronicleQueueReport]

  final case class SliceSnapshot(
      entry: CatalogEntry,
      plyData: PgnAnalysisHelper.PlyData,
      openingLabel: Option[String],
      phase: String,
      evalBeforeCp: Int,
      evalAfterCp: Int,
      evalSwingCp: Int,
      data: lila.llm.model.ExtendedAnalysisData,
      rawCtx: Option[lila.llm.model.NarrativeContext] = None,
      ctx: lila.llm.model.NarrativeContext,
      strategyPack: Option[StrategyPack],
      signalDigest: Option[NarrativeSignalDigest],
      truthContract: Option[lila.llm.analysis.DecisiveTruthContract],
      refs: Option[BookmakerRefsV1]
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

  def chronicleCorpusFromCatalog(entries: List[CatalogEntry]): ChronicleCorpus =
    ChronicleCorpus(
      generatedAt = Instant.now().toString,
      asOfDate = java.time.LocalDate.now().toString,
      title = "Commentary Player QC Corpus",
      description = "External mixed PGN corpus for Bookmaker/Game Chronicle qualitative signoff.",
      games =
        entries.map { entry =>
          val pgn = Files.readString(Paths.get(entry.pgnPath), StandardCharsets.UTF_8)
          ChronicleCorpusGame(
            id = entry.gameKey,
            tier = entry.mixBucket,
            family = entry.openingMacroFamily.getOrElse("other"),
            label =
              List(entry.white.getOrElse("?"), entry.black.getOrElse("?"))
                .mkString(" vs ") + entry.event.fold("")(event => s" ($event)"),
            notes = entry.notes,
            expectedThemes = entry.familyTags,
            pgn = pgn
          )
        }
    )

  def minimalOpeningReference(entry: CatalogEntry): Option[lila.llm.model.OpeningReference] =
    val name = entry.opening.orElse(entry.openingMacroFamily)
    Option.when(name.isDefined || entry.eco.isDefined) {
      lila.llm.model.OpeningReference(
        eco = entry.eco,
        name = name,
        totalGames = 0,
        topMoves = Nil,
        sampleGames = Nil,
        description = entry.variation
      )
    }

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
      openingRef: Option[lila.llm.model.OpeningReference]
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
            renderMode = NarrativeRenderMode.Bookmaker,
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
        val refs = buildBookmakerRefs(plyData.fen, data.alternatives)
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
      case lila.llm.model.OpeningEvent.BranchPoint(_, _, _) => true
      case lila.llm.model.OpeningEvent.OutOfBook(_, _, _)   => true
      case lila.llm.model.OpeningEvent.TheoryEnds(_, _)     => true
      case lila.llm.model.OpeningEvent.Novelty(_, _, _, _)  => true
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
          Option.when(snapshot.data.preventedPlans.exists(_.sourceScope == lila.llm.model.FactScope.Now))(
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
            case Some(lila.llm.model.OpeningEvent.BranchPoint(_, _, _)) => true
            case Some(lila.llm.model.OpeningEvent.OutOfBook(_, _, _))   => true
            case Some(lila.llm.model.OpeningEvent.TheoryEnds(_, _))     => true
            case Some(lila.llm.model.OpeningEvent.Novelty(_, _, _, _))  => true
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
        snapshot.data.preventedPlans.exists(_.sourceScope == lila.llm.model.FactScope.Now) ||
        snapshot.signalDigest.exists(d =>
          d.prophylaxisPlan.exists(_.trim.nonEmpty) || d.prophylaxisThreat.exists(_.trim.nonEmpty)
        )
      }

    val prophylaxisRestraint =
      choose(SliceKind.ProphylaxisRestraint) { snapshot =>
        snapshot.plyData.ply >= 14 &&
        snapshot.plyData.ply <= 42 &&
        snapshot.evalSwingCp < 120 &&
        snapshot.data.preventedPlans.exists(_.sourceScope == lila.llm.model.FactScope.Now) &&
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
    List("bookmaker", "chronicle").map { surface =>
      SliceManifestEntry(
        sampleId = s"$baseId:$surface",
        gameKey = snapshot.entry.gameKey,
        surface = surface,
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
    }

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

  def reviewFlags(
      prose: String,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow],
      sliceKind: String
  ): List[String] =
    val combined = List(prose) ++ supportRows.map(_.text) ++ advancedRows.map(_.text)
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

  def buildBookmakerRows(response: CommentResponse): (List[SupportRow], List[SupportRow]) =
    val support = scala.collection.mutable.ListBuffer.empty[SupportRow]
    val advanced = scala.collection.mutable.ListBuffer.empty[SupportRow]
    val compensationContext =
      response.signalDigest.exists(d =>
        d.compensation.exists(_.trim.nonEmpty) ||
          d.investedMaterial.exists(_ > 0) ||
          d.compensationVectors.exists(_.trim.nonEmpty)
      )
    def sanitizeRowText(raw: String): Option[String] =
      Option(raw)
        .map(UserFacingSignalSanitizer.sanitize)
        .map(_.trim)
        .filter(text => !compensationContext || UserFacingSignalSanitizer.allowCompensationSupportText(text))
        .filter(_.nonEmpty)

    val experimentsByPlan =
      response.strategicPlanExperiments.map { exp =>
        (exp.planId, exp.subplanId.getOrElse("")) -> exp
      }.toMap

    val planRows =
      response.mainStrategicPlans.flatMap { plan =>
        val key = (plan.planId, plan.subplanId.getOrElse(""))
        val badge = experimentsByPlan.get(key).map(_.evidenceTier.replace('_', ' '))
        val label = badge.fold(plan.planName)(tier => s"${plan.planName} [$tier]")
        sanitizeRowText(label)
      }
    if planRows.nonEmpty then support += SupportRow("Main plans", planRows.mkString(", "))

    response.signalDigest.flatMap(_.decisionComparison).foreach { compare =>
      val text =
        List(
          compare.chosenMove.map(move => s"played $move"),
          compare.engineBestMove.filterNot(compare.chosenMove.contains).map(move => s"engine looked at $move"),
          compare.cpLossVsChosen.map(loss => s"gap ${loss}cp")
        ).flatten.mkString(", ")
      if text.nonEmpty then support += SupportRow("Decision compare", text)
    }

    response.signalDigest.flatMap(_.opening).flatMap(sanitizeRowText).filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence).foreach { opening =>
      support += SupportRow("Opening", opening)
    }
    response.signalDigest.flatMap(_.opponentPlan).flatMap(sanitizeRowText).filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence).foreach { opponent =>
      support += SupportRow("Opponent", opponent)
    }

    response.signalDigest.foreach { digest =>
      val structure =
        List(digest.structureProfile, digest.structuralCue, digest.centerState).flatten.flatMap(sanitizeRowText).distinct.mkString("; ")
      if structure.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(structure) then
        support += SupportRow("Structure", structure)

      val deployment =
        List(
          digest.deploymentPiece,
          Option.when(digest.deploymentRoute.nonEmpty)(digest.deploymentRoute.mkString(" -> ")),
          digest.deploymentPurpose
        ).flatten.flatMap(sanitizeRowText).mkString(" ")
      if deployment.nonEmpty && LiveNarrativeCompressionCore.hasConcreteAnchor(deployment) then
        support += SupportRow("Piece deployment", deployment)

      val practicalText =
        List(
          digest.practicalVerdict.flatMap(sanitizeRowText),
          digest.practicalFactors.flatMap(f => LiveNarrativeCompressionCore.renderPracticalBiasPlayer(f, f).toList).headOption.flatMap(sanitizeRowText)
        ).flatten.mkString("; ")
      if practicalText.nonEmpty && LiveNarrativeCompressionCore.keepPlayerFacingSentence(practicalText) then
        support += SupportRow("Practical", practicalText)

      List(
        digest.compensation.flatMap(sanitizeRowText).map(text => SupportRow("Compensation", text)),
        digest.authoringEvidence.flatMap(sanitizeRowText).map(text => SupportRow("Evidence note", text)),
        Option.when(digest.preservedSignals.nonEmpty) {
          val preserved = digest.preservedSignals.flatMap(sanitizeRowText).mkString("; ")
          SupportRow("Preserved signals", preserved)
        }.filter(_.text.nonEmpty)
      ).flatten.foreach { row =>
        if LiveNarrativeCompressionCore.keepPlayerFacingSentence(row.text) then advanced += row
      }
    }

    (support.toList.distinct, advanced.toList.distinct)

  def buildChronicleRows(moment: GameChronicleMoment): (List[SupportRow], List[SupportRow]) =
    val proxy =
      CommentResponse(
        commentary = moment.narrative,
        concepts = moment.concepts,
        variations = moment.variations,
        probeRequests = moment.probeRequests,
        authorQuestions = moment.authorQuestions,
        authorEvidence = moment.authorEvidence,
        mainStrategicPlans = moment.mainStrategicPlans,
        strategicPlanExperiments = moment.strategicPlanExperiments,
        latentPlans = moment.latentPlans,
        whyAbsentFromTopMultiPV = moment.whyAbsentFromTopMultiPV,
        sourceMode = moment.activeStrategicSourceMode.getOrElse("rule"),
        strategyPack = moment.strategyPack,
        signalDigest = moment.signalDigest
      )
    buildBookmakerRows(proxy)

  def bookmakerPlannerRuntime(snapshot: SliceSnapshot): BookmakerRuntimeTrace =
    val rawCtx = snapshot.rawCtx.getOrElse(snapshot.ctx)
    val outline =
      BookStyleRenderer.validatedOutline(
        snapshot.ctx,
        truthContract = snapshot.truthContract,
        strategyPack = snapshot.strategyPack
      )
    val candidateEvidence =
      BookmakerLiveCompressionPolicy.candidateEvidenceLines(snapshot.refs, snapshot.ctx)
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
      BookmakerLiveCompressionPolicy.renderSelection(
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
    val plannerOwnedSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx = snapshot.ctx,
        outline = outline,
        refs = snapshot.refs,
        strategyPack = snapshot.strategyPack,
        truthContract = snapshot.truthContract
      )
    val quietSupportTrace =
      if plannerOwnedSlots.nonEmpty then
        BookmakerQuietSupportTrace(
          rejectReasons = List("planner_owned_row"),
          runtimeGatePassed = Some(quietSupportGateTrace.gatePassed),
          runtimeGateRejectReasons = quietSupportGateTrace.gate.rejectReasons,
          runtimeSceneType = Some(quietSupportGateTrace.gate.sceneType),
          runtimeSelectedOwnerFamily = quietSupportGateTrace.gate.selectedOwnerFamily,
          runtimeSelectedOwnerSource = quietSupportGateTrace.gate.selectedOwnerSource,
          runtimePvDeltaAvailable = Some(quietSupportGateTrace.gate.pvDeltaAvailable),
          runtimeSignalDigestAvailable = Some(quietSupportGateTrace.gate.signalDigestAvailable),
          runtimeMoveLinkedPvDeltaAnchorAvailable = Some(quietSupportGateTrace.gate.moveLinkedPvDeltaAnchorAvailable)
        )
      else
        val trace =
          BookmakerLiveCompressionPolicy.exactFactualQuietSupportTrace(
            ctx = snapshot.ctx,
            refs = snapshot.refs,
            strategyPack = snapshot.strategyPack,
            truthContract = snapshot.truthContract
          )
        BookmakerQuietSupportTrace(
          liftApplied = trace.liftApplied,
          rejectReasons = trace.rejectReasons,
          runtimeGatePassed = Some(trace.composerTrace.gatePassed),
          runtimeGateRejectReasons = trace.composerTrace.gate.rejectReasons,
          runtimeSceneType = Some(trace.composerTrace.gate.sceneType),
          runtimeSelectedOwnerFamily = trace.composerTrace.gate.selectedOwnerFamily,
          runtimeSelectedOwnerSource = trace.composerTrace.gate.selectedOwnerSource,
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
        BookmakerPolishSlotsBuilder.buildOrFallback(
          ctx = snapshot.ctx,
          outline = outline,
          refs = snapshot.refs,
          strategyPack = snapshot.strategyPack,
          truthContract = snapshot.truthContract
        )
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
        lila.llm.RuleTemplateSanitizer.sanitize(
          if deterministicProse.nonEmpty then deterministicProse
          else exactFactualReviewProse(snapshot),
          opening = snapshot.openingLabel,
          phase = snapshot.phase,
          ply = snapshot.plyData.ply,
          fen = Some(snapshot.plyData.fen)
        ),
        snapshot.truthContract
      )
    BookmakerRuntimeTrace(
      planner =
        BookmakerPlannerTrace(
          primaryKind = rankedPlans.primary.map(_.questionKind.toString),
          primaryFallbackMode =
            rankedPlans.primary.map(_.fallbackMode.toString)
              .orElse(rankedPlans.rejected.headOption.map(_.fallbackMode.toString)),
          secondaryKind = rankedPlans.secondary.map(_.questionKind.toString),
          secondarySurfaced =
            rankedPlans.secondary.nonEmpty && plannerOwnedSlots.exists(_.supportSecondary.nonEmpty),
          bookmakerFallbackMode = if plannerOwnedSlots.nonEmpty then "planner_owned" else "exact_factual",
          sceneType = Some(rankedPlans.ownerTrace.sceneType.wireName),
          sceneReasons = rankedPlans.ownerTrace.sceneReasons,
          ownerCandidates = rankedPlans.ownerTrace.ownerCandidateLabels,
          admittedFamilies = rankedPlans.ownerTrace.admittedFamilyLabels,
          droppedFamilies = rankedPlans.ownerTrace.droppedFamilyLabels,
          supportMaterialSeparation = rankedPlans.ownerTrace.supportMaterialSeparationLabels,
          proposedFamilyMappings = rankedPlans.ownerTrace.proposedFamilyMappingLabels,
          demotionReasons = rankedPlans.ownerTrace.demotionReasons,
          selectedQuestion = rankedPlans.ownerTrace.selectedQuestion.map(_.toString),
          selectedOwnerFamily = rankedPlans.ownerTrace.selectedOwnerFamily.map(_.wireName),
          selectedOwnerSource = rankedPlans.ownerTrace.selectedOwnerSource,
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
            Some(if plannerOwnedSlots.nonEmpty then "bookmaker_planner_owned" else "bookmaker_exact_factual"),
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
              )
        ),
      prose = prose,
      quietSupport = quietSupportTrace
    )

  def bookmakerPlannerTrace(snapshot: SliceSnapshot): BookmakerPlannerTrace =
    bookmakerPlannerRuntime(snapshot).planner

  private def plannerCandidateSources(
      rankedPlans: lila.llm.analysis.RankedQuestionPlans,
      familyWireName: String
  ): List[String] =
    rankedPlans.ownerTrace.ownerCandidates
      .filter(_.family.wireName == familyWireName)
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

  private def buildBookmakerRefs(
      fenBefore: String,
      variations: List[VariationLine]
  ): Option[BookmakerRefsV1] =
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
          MoveRefV1(
            refId = f"l${lineIdx + 1}%02d_m${i + 1}%02d",
            san = sanList(i),
            uci = uciList(i),
            fenAfter = fensAfter(i),
            ply = ply,
            moveNo = (ply + 1) / 2,
            marker = Some(markerForPly(ply))
          )
        }
        VariationRefV1(
          lineId = f"line_${lineIdx + 1}%02d",
          scoreCp = line.scoreCp,
          mate = line.mate,
          depth = line.depth,
          moves = moves
        )
      }
      Some(
        BookmakerRefsV1(
          startFen = fenBefore,
          startPly = startPly,
          variations = lines
        )
      )

  final class LocalUciEngine(enginePath: Path, timeoutMs: Long):
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
