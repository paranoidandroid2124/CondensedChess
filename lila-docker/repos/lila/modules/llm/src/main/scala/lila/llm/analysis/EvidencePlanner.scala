package lila.llm.analysis

import lila.llm.model.authoring.{ AuthorQuestion, AuthorQuestionKind, QuestionEvidence }

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
