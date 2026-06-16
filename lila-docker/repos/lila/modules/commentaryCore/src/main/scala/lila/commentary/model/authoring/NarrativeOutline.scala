package lila.commentary.model.authoring

/**
 * NarrativeOutline is an intermediate, explicit structure between:
 * (AuthorQuestions + VerifiedEvidence) and final prose.
 *
 * The intent is to make "what we decided to say" inspectable and testable,
 * while leaving "how we phrase it" to the renderer/lexicon.
 *
 * * - Builder creates beats with semantic metadata (conceptIds, anchors)
 * - Validator enforces hard gates (evidence requirements, must-mention)
 * - Renderer only assembles prose from validated beats
 */
enum OutlineBeatKind:
  case MoveHeader
  case Context
  case DecisionPoint
  case Evidence
  case OpeningTheory
  case TeachingPoint
  case MainMove
  case PsychologicalVerdict
  case Alternatives
  case WrapUp

/**
 * OutlineBeat: A semantic unit of narrative content.
 *
 * @param kind The structural role of this beat
 * @param text The released beat text produced by builder/producers; empty beats are dropped before rendering.
 * @param conceptIds Semantic identifiers (e.g., "Weakness_c6", "Outpost_f5")
 * @param anchors Must-mention tokens that should appear in final prose
 * @param questionIds Related AuthorQuestion IDs
 * @param questionKinds Related AuthorQuestionKind values (for Validator rules)
 * @param evidencePurposes Evidence purposes used (e.g., "free_tempo_branches")
 * @param evidenceSourceIds ProbeResult IDs that back this beat
 * @param requiresEvidence If true, Validator enforces proof exists
 * @param confidenceLevel 0.0-1.0, used for downgrade decisions
 * @param focusPriority Full-game focus-preservation score (higher survives clipping)
 * @param fullGameEssential Preserve this beat when building full-game moment bodies
 * @param mustMention Legacy field, use anchors instead
 */
case class OutlineBeat(
  kind: OutlineBeatKind,
  text: String = "",
  // Semantic payload
  conceptIds: List[String] = Nil,
  anchors: List[String] = Nil,
  questionIds: List[String] = Nil,
  questionKinds: List[AuthorQuestionKind] = Nil,
  evidencePurposes: List[String] = Nil,
  expectedEvidencePurposes: List[String] = Nil,
  evidenceSourceIds: List[String] = Nil,
  // Quality control
  requiresEvidence: Boolean = false,
  confidenceLevel: Double = 1.0,
  focusPriority: Int = 0,
  fullGameEssential: Boolean = false,
  branchScoped: Boolean = false,
  supportKinds: List[OutlineBeatKind] = Nil,
  // Legacy compatibility
  mustMention: List[String] = Nil
):
  /** Merged anchors for validation (combines new anchors + legacy mustMention) */
  def allAnchors: List[String] = (anchors ++ mustMention).distinct

/**
 * OutlineDiagnostics: Debug/trace information for the outline generation process.
 * Used by tests and trace output to verify correctness.
 */
case class OutlineDiagnostics(
  selectedQuestions: List[AuthorQuestion] = Nil,
  usedEvidencePurposes: Set[String] = Set.empty,
  missingEvidencePurposes: Set[String] = Set.empty,
  plannerPrimary: Option[String] = None,
  plannerSecondary: Option[String] = None,
  plannerRejected: List[String] = Nil,
  plannerSceneType: Option[String] = None,
  plannerOwnerCandidates: List[String] = Nil,
  plannerAdmittedOwners: List[String] = Nil,
  plannerDroppedOwners: List[String] = Nil,
  plannerSupportMaterialSeparation: List[String] = Nil,
  plannerProposedOwnerMappings: List[String] = Nil,
  plannerDemotionReasons: List[String] = Nil,
  plannerSelectedQuestion: Option[String] = None,
  plannerSelectedOwnerKind: Option[String] = None,
  plannerSelectedSource: Option[String] = None,
  surfaceReplayOutcome: Option[String] = None,
  droppedBeats: List[(OutlineBeatKind, String)] = Nil, // (kind, reason)
  downgradedBeats: List[(OutlineBeatKind, String)] = Nil, // (kind, reason)
  warnings: List[String] = Nil
):
  def addDropped(kind: OutlineBeatKind, reason: String): OutlineDiagnostics =
    copy(droppedBeats = droppedBeats :+ (kind, reason))

  def addDowngraded(kind: OutlineBeatKind, reason: String): OutlineDiagnostics =
    copy(downgradedBeats = downgradedBeats :+ (kind, reason))

  def addWarning(msg: String): OutlineDiagnostics =
    copy(warnings = warnings :+ msg)

  def addMissing(purpose: String): OutlineDiagnostics =
    copy(missingEvidencePurposes = missingEvidencePurposes + purpose)

  def summary: String =
    val parts = List.newBuilder[String]
    parts += s"Questions: ${selectedQuestions.size}"
    parts += s"Evidence used: ${usedEvidencePurposes.mkString(", ")}"
    if missingEvidencePurposes.nonEmpty then parts += s"Missing: ${missingEvidencePurposes.mkString(", ")}"
    plannerPrimary.foreach(value => parts += s"Planner primary: $value")
    plannerSecondary.foreach(value => parts += s"Planner secondary: $value")
    if plannerRejected.nonEmpty then parts += s"Planner rejected: ${plannerRejected.mkString("; ")}"
    plannerSceneType.foreach(value => parts += s"Planner scene: $value")
    plannerSelectedOwnerKind.foreach(value => parts += s"Planner owner kind: $value")
    plannerSelectedSource.foreach(value => parts += s"Planner source: $value")
    surfaceReplayOutcome.foreach(value => parts += s"Surface replay: $value")
    if droppedBeats.nonEmpty then parts += s"Dropped: ${droppedBeats.size}"
    if downgradedBeats.nonEmpty then parts += s"Downgraded: ${downgradedBeats.size}"
    if warnings.nonEmpty then parts += s"Warnings: ${warnings.size}"
    parts.result().mkString(" | ")

/**
 * NarrativeOutline: The complete narrative structure for a position.
 */
case class NarrativeOutline(
  beats: List[OutlineBeat],
  diagnostics: Option[OutlineDiagnostics] = None
):
  def prose: String =
    beats.map(_.text).filter(_.trim.nonEmpty).mkString("\n\n")

  def hasBeat(kind: OutlineBeatKind): Boolean =
    beats.exists(_.kind == kind)

  def getBeat(kind: OutlineBeatKind): Option[OutlineBeat] =
    beats.find(_.kind == kind)

  def beatsOfKind(kind: OutlineBeatKind): List[OutlineBeat] =
    beats.filter(_.kind == kind)

  def evidencePurposes: Set[String] =
    beats.flatMap(_.evidencePurposes).toSet

  def questionKinds: Set[AuthorQuestionKind] =
    beats.flatMap(_.questionKinds).toSet
