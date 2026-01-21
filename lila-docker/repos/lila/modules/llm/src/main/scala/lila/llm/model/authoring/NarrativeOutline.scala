package lila.llm.model.authoring

/**
 * NarrativeOutline is an intermediate, explicit structure between:
 * (AuthorQuestions + VerifiedEvidence) and final prose.
 *
 * The intent is to make "what we decided to say" inspectable and testable,
 * while leaving "how we phrase it" to the renderer/lexicon.
 *
 * Phase 5: SSOT Architecture
 * - Builder creates beats with semantic metadata (conceptIds, anchors)
 * - Validator enforces hard gates (evidence requirements, must-mention)
 * - Renderer only assembles prose from validated beats
 */
enum OutlineBeatKind:
  case MoveHeader
  case Context
  case DecisionPoint
  case Evidence
  case ConditionalPlan
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
 * @param text The rendered text (may be empty if Renderer generates from Lexicon)
 * @param conceptIds Semantic identifiers (e.g., "Weakness_c6", "Outpost_f5")
 * @param anchors Must-mention tokens that should appear in final prose
 * @param questionIds Related AuthorQuestion IDs
 * @param questionKinds Related AuthorQuestionKind values (for Validator rules)
 * @param evidencePurposes Evidence purposes used (e.g., "free_tempo_branches")
 * @param evidenceSourceIds ProbeResult IDs that back this beat
 * @param requiresEvidence If true, Validator enforces proof exists
 * @param confidenceLevel 0.0-1.0, used for downgrade decisions
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
  evidenceSourceIds: List[String] = Nil,
  // Quality control
  requiresEvidence: Boolean = false,
  confidenceLevel: Double = 1.0,
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
    val parts = List(
      Some(s"Questions: ${selectedQuestions.size}"),
      Some(s"Evidence used: ${usedEvidencePurposes.mkString(", ")}"),
      Option.when(missingEvidencePurposes.nonEmpty)(s"Missing: ${missingEvidencePurposes.mkString(", ")}"),
      Option.when(droppedBeats.nonEmpty)(s"Dropped: ${droppedBeats.size}"),
      Option.when(downgradedBeats.nonEmpty)(s"Downgraded: ${downgradedBeats.size}"),
      Option.when(warnings.nonEmpty)(s"Warnings: ${warnings.size}")
    ).flatten
    parts.mkString(" | ")

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
