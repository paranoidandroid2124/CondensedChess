package lila.llm.analysis

import lila.llm.model.authoring.{ AuthorQuestion, QuestionEvidence }

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

  private val MultiBranchPurposes = Set(
    "keep_tension_branches",
    "recapture_branches"
  )

  def requiredPurposes(question: AuthorQuestion): Set[String] =
    question.evidencePurposes.map(_.trim).filter(_.nonEmpty).toSet

  def isSatisfied(expectedPurposes: Set[String], availablePurposes: Set[String]): Boolean =
    expectedPurposes.isEmpty || expectedPurposes.exists(availablePurposes.contains)

  def isSatisfied(question: AuthorQuestion, availablePurposes: Set[String]): Boolean =
    isSatisfied(requiredPurposes(question), availablePurposes)

  def minBranches(expectedPurposes: Set[String]): Int =
    if expectedPurposes.exists(MultiBranchPurposes.contains) then 2 else 1

  def minBranches(question: AuthorQuestion): Int =
    minBranches(requiredPurposes(question))

  /**
   * Get the missing purposes for diagnostics (using QuestionEvidence).
   */
  def getMissingPurposesFromEvidence(
    questions: List[AuthorQuestion],
    evidence: List[QuestionEvidence]
  ): Set[String] =
    val availablePurposes = evidence.map(_.purpose).toSet
    questions.flatMap { q =>
      val required = requiredPurposes(q)
      if isSatisfied(required, availablePurposes) then Set.empty[String]
      else required
    }.toSet
