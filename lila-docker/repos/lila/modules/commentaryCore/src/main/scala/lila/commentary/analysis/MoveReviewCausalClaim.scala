package lila.commentary.analysis

import lila.commentary.model.authoring.AuthorQuestionKind

private[commentary] object MoveReviewCausalClaim:
  import MoveReviewLocalFact.Admission as LocalFactAdmission

  enum SubjectRole:
    case PlayedMove
    case LineOrReply

    def wireName: String =
      this match
        case PlayedMove  => "played_move"
        case LineOrReply => "line_or_reply"

  enum EvidenceKind:
    case Contrast
    case SecondaryPlan
    case TimingTension
    case BranchLine
    case CertifiedConsequence
    case TimingWitness

    def wireName: String =
      this match
        case Contrast             => "contrast"
        case SecondaryPlan        => "secondary_plan"
        case TimingTension        => "timing_tension"
        case BranchLine           => "branch_line"
        case CertifiedConsequence => "certified_consequence"
        case TimingWitness        => "timing_witness"

  enum RelationKind:
    case AlternativeContrast
    case PlayedMoveConsequence
    case TimingConstraint
    case DefensiveResource
    case PlanRace
    case ChangeConsequence

    def wireName: String =
      this match
        case AlternativeContrast => "alternative_contrast"
        case PlayedMoveConsequence => "played_move_consequence"
        case TimingConstraint => "timing_constraint"
        case DefensiveResource => "defensive_resource"
        case PlanRace => "plan_race"
        case ChangeConsequence => "change_consequence"

  final case class CertifiedClaim(
      questionKind: AuthorQuestionKind,
      surfaceText: String,
      subjectRole: SubjectRole,
      evidenceKinds: List[EvidenceKind],
      relationKinds: List[RelationKind],
      supportRenderedInClaim: Boolean,
      supportRequired: Boolean,
      localFact: Option[LocalFactAdmission]
  ):
    def guardrail: String =
      val evidence = evidenceKinds.map(_.wireName).distinct.mkString(",")
      val relations = relationKinds.map(_.wireName).distinct.mkString(",")
      val embeddedSupport = if supportRenderedInClaim then ", support_embedded=true" else ""
      val local =
        localFact.fold("")(fact =>
          s", local_fact=${fact.family.key}/${fact.authority.key}, local_fact_strict=${fact.strictFallbackEligible}"
        )
      s"MoveReview causal claim: question=${questionKind.toString}, subject=${subjectRole.wireName}, evidence=$evidence, relations=$relations$embeddedSupport$local"

  final case class Decision(
      claim: Option[CertifiedClaim],
      rejectReasons: List[String]
  )

  def requiresTypedSupport(kind: AuthorQuestionKind): Boolean =
    kind match
      case AuthorQuestionKind.WhyThis | AuthorQuestionKind.WhyNow | AuthorQuestionKind.WhatChanged |
          AuthorQuestionKind.WhatMustBeStopped | AuthorQuestionKind.WhosePlanIsFaster =>
        true
      case AuthorQuestionKind.WhatMattersHere =>
        false

  def decide(
      plan: QuestionPlan,
      renderedClaim: String,
      contrastAdmissible: Boolean,
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      surfaceConsequence: Option[String],
      typedTimingSupport: Boolean
  ): Decision =
    val supportRequired = requiresTypedSupport(plan.questionKind)
    val evidenceKinds =
      List(
        Option.when(contrastAdmissible)(supportPrimary.map(_ => EvidenceKind.Contrast)).flatten,
        supportSecondary.map(_ => EvidenceKind.SecondaryPlan),
        tension.map(_ => EvidenceKind.TimingTension),
        evidenceHook.map(_ => EvidenceKind.BranchLine),
        coda.map(_ => EvidenceKind.CertifiedConsequence),
        plan.timingWitness.map(_ => EvidenceKind.TimingWitness)
      ).flatten.distinct
    val relationKinds = causalRelations(plan, contrastAdmissible, supportPrimary, tension, evidenceHook, coda, typedTimingSupport)
    val certifiedSurface =
      certifiedSurfaceText(plan.questionKind, renderedClaim, relationKinds, supportPrimary, surfaceConsequence)
    val localFact =
      MoveReviewLocalFact
        .admitPlanner(
          plan,
          evidenceKinds.map(_.wireName),
          relationKinds.map(_.wireName),
          lineConsequenceBacked = surfaceConsequence.nonEmpty
        )
        .admission
    val rejectReasons =
      List(
        Option.when(supportRequired && evidenceKinds.isEmpty)("causal_support_missing"),
        Option.when(supportRequired && relationKinds.isEmpty)("causal_relation_missing"),
        Option.when(supportRequired && !hasConcreteCausalAnchor(certifiedSurface))("causal_claim_anchor_missing"),
        Option.when(supportRequired && localFact.isEmpty)("local_fact_admission_missing"),
        Option.when(plan.questionKind == AuthorQuestionKind.WhyThis && !whyThisRoleAuthorized(relationKinds))(
          "why_this_role_authority_missing"
        ),
        Option.when(openingRelationWhyThis(plan) && !contrastAdmissible)("opening_relation_contrast_not_admissible")
      ).flatten
    if rejectReasons.nonEmpty then Decision(None, rejectReasons)
    else
      Decision(
        claim =
          Some(
            CertifiedClaim(
              questionKind = plan.questionKind,
              surfaceText = certifiedSurface,
              subjectRole = subjectRole(plan),
              evidenceKinds = evidenceKinds,
              relationKinds = relationKinds,
              supportRenderedInClaim = certifiedSurface != renderedClaim,
              supportRequired = supportRequired,
              localFact = localFact
            )
          ),
        rejectReasons = Nil
      )

  private def hasConcreteCausalAnchor(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    LineScopedCitation.hasConcreteSanLine(text) ||
      LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
      low.matches(""".*\b(threat|break|counterplay|mate|wins|loses|drops|fork|pin|trade|recapture|reply)\b.*""")

  private def subjectRole(plan: QuestionPlan): SubjectRole =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped ||
      plan.sourceKinds.exists(kind => kind.contains("reply") || kind.contains("threat"))
    then SubjectRole.LineOrReply
    else SubjectRole.PlayedMove

  private def causalRelations(
      plan: QuestionPlan,
      contrastAdmissible: Boolean,
      supportPrimary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      typedTimingSupport: Boolean
  ): List[RelationKind] =
    val playedMoveConsequence =
      playedMoveCausalOwner(plan) && (evidenceHook.nonEmpty || coda.nonEmpty)
    List(
      Option.when(contrastAdmissible && supportPrimary.nonEmpty)(RelationKind.AlternativeContrast),
      Option.when(playedMoveConsequence)(RelationKind.PlayedMoveConsequence),
      Option.when(plan.questionKind == AuthorQuestionKind.WhyNow && typedTimingSupport)(RelationKind.TimingConstraint),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
          plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
          (evidenceHook.nonEmpty || coda.nonEmpty || plan.timingWitness.nonEmpty)
      )(RelationKind.DefensiveResource),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhosePlanIsFaster &&
          plan.plannerOwnerKind == PlannerOwnerKind.PlanRace &&
          (supportPrimary.nonEmpty || tension.nonEmpty || coda.nonEmpty)
      )(RelationKind.PlanRace),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatChanged &&
          (playedMoveConsequence || (contrastAdmissible && supportPrimary.nonEmpty))
      )(RelationKind.ChangeConsequence)
    ).flatten.distinct

  private def whyThisRoleAuthorized(relations: List[RelationKind]): Boolean =
    relations.exists(relation =>
      relation == RelationKind.AlternativeContrast ||
        relation == RelationKind.PlayedMoveConsequence
    )

  private def certifiedSurfaceText(
      questionKind: AuthorQuestionKind,
      renderedClaim: String,
      relations: List[RelationKind],
      supportPrimary: Option[String],
      surfaceConsequence: Option[String]
  ): String =
    if questionKind == AuthorQuestionKind.WhyThis && relations.contains(RelationKind.AlternativeContrast) then
      supportPrimary.getOrElse(renderedClaim)
    else if
      (questionKind == AuthorQuestionKind.WhyThis || questionKind == AuthorQuestionKind.WhatChanged) &&
        relations.contains(RelationKind.PlayedMoveConsequence)
    then surfaceConsequence.getOrElse(renderedClaim)
    else renderedClaim

  private def playedMoveCausalOwner(plan: QuestionPlan): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta ||
      plan.plannerOwnerKind == PlannerOwnerKind.TacticalFailure

  private def openingRelationWhyThis(plan: QuestionPlan): Boolean =
    plan.questionKind == AuthorQuestionKind.WhyThis &&
      (plan.plannerSource == "opening_relation_translator" ||
        plan.sourceKinds.contains("opening_relation_translator"))
