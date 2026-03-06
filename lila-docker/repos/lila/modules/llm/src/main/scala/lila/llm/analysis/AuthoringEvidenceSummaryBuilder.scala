package lila.llm.analysis

import lila.llm.{ AuthorEvidenceSummary, AuthorQuestionSummary, EvidenceBranchSummary }
import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.AuthorQuestion

object AuthoringEvidenceSummaryBuilder:

  private val MaxQuestions = 3
  private val MaxBranches = 2
  private val MaxPurposes = 4
  private val MaxLinkedPlans = 3

  def summarizeQuestions(ctx: NarrativeContext): List[AuthorQuestionSummary] =
    ctx.authorQuestions
      .sortBy(_.priority)
      .take(MaxQuestions)
      .map(summarizeQuestion)

  def summarizeEvidence(ctx: NarrativeContext): List[AuthorEvidenceSummary] =
    val selectedQuestions = ctx.authorQuestions.sortBy(_.priority).take(MaxQuestions)
    val evidenceByQuestion = ctx.authorEvidence.groupBy(_.questionId)
    val requestsByQuestion =
      ctx.probeRequests
        .flatMap(req => req.questionId.map(_ -> req))
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
        .toMap

    selectedQuestions.map { q =>
      val evidence = evidenceByQuestion.getOrElse(q.id, Nil)
      val requests = requestsByQuestion.getOrElse(q.id, Nil)
      val branches =
        evidence
          .flatMap(_.branches)
          .sortBy(branch => (-branch.depth.getOrElse(0), -branch.evalCp.getOrElse(Int.MinValue), branch.keyMove))
          .take(MaxBranches)
          .map { branch =>
            EvidenceBranchSummary(
              keyMove = branch.keyMove,
              line = branch.line,
              evalCp = branch.evalCp,
              mate = branch.mate,
              depth = branch.depth,
              sourceId = branch.sourceId
            )
          }
      val purposes =
        (evidence.map(_.purpose) ++ requests.flatMap(_.purpose))
          .map(_.trim)
          .filter(_.nonEmpty)
          .distinct
          .take(MaxPurposes)
      val linkedPlans =
        (requests.flatMap(_.planName) ++ requests.flatMap(_.seedId) ++ q.latentPlan.toList.map(_.seedId))
          .map(_.trim)
          .filter(_.nonEmpty)
          .distinct
          .take(MaxLinkedPlans)
      val status =
        if branches.nonEmpty then "resolved"
        else if requests.nonEmpty then "pending"
        else "question_only"

      AuthorEvidenceSummary(
        questionId = q.id,
        questionKind = q.kind.toString,
        question = q.question,
        why = q.why,
        status = status,
        purposes = purposes,
        branchCount = evidence.flatMap(_.branches).size,
        branches = branches,
        pendingProbeIds = requests.map(_.id).distinct.take(3),
        pendingProbeCount = requests.size,
        probeObjectives =
          requests
            .flatMap(_.objective)
            .map(_.trim)
            .filter(_.nonEmpty)
            .distinct
            .take(3),
        linkedPlans = linkedPlans
      )
    }.filter(summary =>
      summary.question.trim.nonEmpty &&
        (
          summary.branchCount > 0 ||
            summary.pendingProbeCount > 0 ||
            summary.why.exists(_.trim.nonEmpty)
        )
    )

  def headline(ctx: NarrativeContext): Option[String] =
    val summaries = summarizeEvidence(ctx)
    val resolved = summaries.count(_.status == "resolved")
    val pending = summaries.count(_.status == "pending")
    val latentEvidence = summaries.find(_.questionKind == "LatentPlan")
    latentEvidence
      .map { summary =>
        if summary.status == "resolved" then
          s"latent plan evidence is resolved via ${summary.branchCount} branch${if summary.branchCount == 1 then "" else "es"}"
        else if summary.pendingProbeCount > 0 then
          s"latent plan evidence is pending across ${summary.pendingProbeCount} probe${if summary.pendingProbeCount == 1 then "" else "s"}"
        else
          "latent plan framing remains heuristic"
      }
      .orElse {
        Option.when(resolved > 0 || pending > 0)(
          s"author evidence: $resolved resolved, $pending pending"
        )
      }

  private def summarizeQuestion(q: AuthorQuestion): AuthorQuestionSummary =
    AuthorQuestionSummary(
      id = q.id,
      kind = q.kind.toString,
      priority = q.priority,
      question = q.question,
      why = q.why,
      anchors = q.anchors.take(4),
      confidence = q.confidence.toString,
      latentPlanName = q.latentPlan.map(_.seedId.replace('_', ' ')),
      latentSeedId = q.latentPlan.map(_.seedId)
    )
