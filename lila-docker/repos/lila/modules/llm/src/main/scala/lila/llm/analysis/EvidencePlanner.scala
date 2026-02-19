package lila.llm.analysis

import lila.llm.model.authoring.{ AuthorQuestion, AuthorQuestionKind, QuestionEvidence }
import lila.llm.model.ProbeResult

/**
 * EvidencePlanner: SSOT for question-kind → evidence requirements mapping.
 *
 * This module centralizes the logic that determines what evidence
 * is required for each type of AuthorQuestion. The Validator uses this
 * to enforce hard gates.
 *
 * Key Design:
 * - Each AuthorQuestionKind maps to required evidence purposes
 * - Missing evidence triggers downgrade or drop in Validator
 * - This is called by both NarrativeContextBuilder and NarrativeOutlineValidator
 */
object EvidencePlanner:

  /**
   * EvidenceRequirement: What evidence is needed for a given question.
   *
   * @param questionId The AuthorQuestion ID
   * @param questionKind The type of question
   * @param requiredPurposes Set of probe purposes that must be satisfied
   * @param minBranches Minimum number of evidence branches (usually 2)
   * @param priority Higher = more important to have evidence
   */
  case class EvidenceRequirement(
    questionId: String,
    questionKind: AuthorQuestionKind,
    requiredPurposes: Set[String],
    minBranches: Int = 2,
    priority: Int = 1
  )

  /**
   * EvidenceCheck: Result of checking if requirements are satisfied.
   */
  case class EvidenceCheck(
    satisfied: List[EvidenceRequirement],
    missing: List[EvidenceRequirement],
    partiallyMet: List[(EvidenceRequirement, Set[String])] // (req, missing purposes)
  ):
    def allSatisfied: Boolean = missing.isEmpty && partiallyMet.isEmpty
    def hasCriticalGaps: Boolean = missing.exists(_.priority >= 2)
  // Question Kind → Evidence Mapping (SSOT)

  /**
   * Get required evidence purposes for a question kind.
   */
  def requiredPurposes(kind: AuthorQuestionKind): Set[String] =
    kind match
      case AuthorQuestionKind.TacticalTest =>
        // TruthGate line OR reply_multipv
        Set("reply_multipv")

      case AuthorQuestionKind.TensionDecision =>
        // keep_tension_branches (preferred) OR reply_multipv
        Set("keep_tension_branches", "reply_multipv")

      case AuthorQuestionKind.PlanClash =>
        // keep_tension_branches (required)
        Set("keep_tension_branches")

      case AuthorQuestionKind.StructuralCommitment =>
        // recapture_branches (required)
        Set("recapture_branches")

      case AuthorQuestionKind.DefensiveTask =>
        // defense_reply_multipv (required)
        Set("defense_reply_multipv")

      case AuthorQuestionKind.ConversionPlan =>
        // convert_reply_multipv (required)
        Set("convert_reply_multipv")

      case AuthorQuestionKind.LatentPlan =>
        // free_tempo_branches OR latent_plan_refutation (at least one)
        Set("free_tempo_branches", "latent_plan_refutation", "latent_plan_immediate")

  /**
   * Check if a question kind is satisfied by a set of evidence purposes.
   * For most kinds, ANY matching purpose is sufficient.
   * For LatentPlan, we need at least one of the viability/refutation proofs.
   */
  def isSatisfied(kind: AuthorQuestionKind, availablePurposes: Set[String]): Boolean =
    val required = requiredPurposes(kind)
    kind match
      case AuthorQuestionKind.LatentPlan =>
        // Need at least one of the latent plan evidence types
        required.exists(availablePurposes.contains)
      case _ =>
        // For other kinds, any matching purpose is enough
        required.exists(availablePurposes.contains)

  /**
   * Get minimum required branches for evidence beat.
   */
  def minBranches(kind: AuthorQuestionKind): Int =
    kind match
      case AuthorQuestionKind.StructuralCommitment => 2
      case AuthorQuestionKind.TensionDecision => 2
      case AuthorQuestionKind.PlanClash => 2
      case AuthorQuestionKind.LatentPlan => 2
      case _ => 1

  /**
   * Get priority level for evidence requirement.
   * Higher = more critical to have.
   */
  def priority(kind: AuthorQuestionKind): Int =
    kind match
      case AuthorQuestionKind.TacticalTest => 3 // Critical: tactics must be proven
      case AuthorQuestionKind.StructuralCommitment => 2
      case AuthorQuestionKind.LatentPlan => 2
      case _ => 1
  // Planning & Checking

  /**
   * Plan evidence requirements for a list of questions.
   */
  def plan(questions: List[AuthorQuestion]): List[EvidenceRequirement] =
    questions.map { q =>
      EvidenceRequirement(
        questionId = q.id,
        questionKind = q.kind,
        requiredPurposes = requiredPurposes(q.kind),
        minBranches = minBranches(q.kind),
        priority = priority(q.kind)
      )
    }

  /**
   * Check which evidence requirements are satisfied by available probe results.
   */
  def checkSatisfied(
    requirements: List[EvidenceRequirement],
    probeResults: List[ProbeResult]
  ): EvidenceCheck =
    val availablePurposes = probeResults.flatMap(_.purpose).toSet
    val availableBranchCounts = probeResults
      .filter(_.replyPvs.nonEmpty)
      .groupBy(_.purpose.getOrElse(""))
      .view.mapValues(_.flatMap(_.replyPvs).size)
      .toMap

    val (satisfied, notSatisfied) = requirements.partition { req =>
      val hasMatchingPurpose = req.requiredPurposes.exists(availablePurposes.contains)
      val hasSufficientBranches = req.requiredPurposes.exists { p =>
        availableBranchCounts.getOrElse(p, 0) >= req.minBranches
      }
      hasMatchingPurpose && (req.minBranches <= 1 || hasSufficientBranches)
    }

    val (partial, missing) = notSatisfied.partition { req =>
      req.requiredPurposes.exists(availablePurposes.contains)
    }

    val partialDetails = partial.map { req =>
      val found = req.requiredPurposes.intersect(availablePurposes)
      val stillNeeded = req.requiredPurposes -- found
      (req, stillNeeded)
    }

    EvidenceCheck(satisfied, missing, partialDetails)

  /**
   * Get the missing purposes for diagnostics (using QuestionEvidence).
   */
  def getMissingPurposesFromEvidence(
    questions: List[AuthorQuestion],
    evidence: List[QuestionEvidence]
  ): Set[String] =
    val availablePurposes = evidence.map(_.purpose).toSet
    questions.flatMap { q =>
      val required = requiredPurposes(q.kind)
      if required.exists(availablePurposes.contains) then Set.empty[String]
      else required
    }.toSet
