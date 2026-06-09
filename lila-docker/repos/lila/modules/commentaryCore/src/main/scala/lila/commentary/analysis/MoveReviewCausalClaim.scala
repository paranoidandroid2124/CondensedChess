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

  enum EvidenceSource:
    case AdmissibleContrast
    case ExplicitReplyLoss
    case SecondaryPlan
    case TimingTension
    case BranchLine
    case PlannerConsequence
    case LineConsequenceSurface
    case TimingWitness

    def wireName: String =
      this match
        case AdmissibleContrast    => "admissible_contrast"
        case ExplicitReplyLoss     => "explicit_reply_loss"
        case SecondaryPlan         => "secondary_plan"
        case TimingTension         => "timing_tension"
        case BranchLine            => "branch_line"
        case PlannerConsequence    => "planner_consequence"
        case LineConsequenceSurface => "line_consequence_surface"
        case TimingWitness         => "timing_witness"

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

  final case class TypedEvidence(
      kind: EvidenceKind,
      source: EvidenceSource,
      text: Option[String],
      subjectRole: SubjectRole,
      lineBinding: MoveReviewLocalFact.LineBinding = MoveReviewLocalFact.LineBinding.None,
      guardrails: List[String] = Nil
  )

  final case class Candidate(
      plan: QuestionPlan,
      renderedClaim: String,
      evidences: List[TypedEvidence]
  )

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
          s", local_fact=${fact.family.key}/${fact.authority.key}, local_fact_producer=${fact.producer.key}, local_fact_strict=${fact.strictFallbackEligible}, local_fact_line=${fact.lineBinding.key}"
        )
      s"MoveReview causal claim: question=${questionKind.toString}, subject=${subjectRole.wireName}, evidence=$evidence, relations=$relations$embeddedSupport$local"

  final case class Decision(
      claim: Option[CertifiedClaim],
      rejectReasons: List[String],
      evidences: List[TypedEvidence] = Nil
  )

  def admit(candidate: Candidate): Decision =
    val plan = candidate.plan
    val supportRequired = requiresTypedSupport(plan.questionKind)
    val evidenceKinds = candidate.evidences.map(_.kind).distinct
    val relationKinds = causalRelations(candidate)
    val certifiedSurface =
      certifiedSurfaceText(
        plan.questionKind,
        candidate.renderedClaim,
        relationKinds,
        evidenceText(candidate, EvidenceKind.Contrast),
        lineConsequenceSurfaceText(candidate)
      )
    val localFact =
      MoveReviewLocalFact
        .admitPlanner(
          plan,
          evidenceKinds.map(_.wireName),
          relationKinds.map(_.wireName),
          lineConsequenceBacked = lineConsequenceSurfaceText(candidate).nonEmpty,
          lineBinding = dominantLineBinding(candidate.evidences)
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
        Option.when(openingRelationWhyThis(plan) && !hasEvidence(candidate, EvidenceSource.AdmissibleContrast))(
          "opening_relation_contrast_not_admissible"
        )
      ).flatten
    if rejectReasons.nonEmpty then Decision(None, rejectReasons, candidate.evidences)
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
              supportRenderedInClaim = certifiedSurface != candidate.renderedClaim,
              supportRequired = supportRequired,
              localFact = localFact
            )
          ),
        rejectReasons = Nil,
        evidences = candidate.evidences
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
    admit(
      candidate(
        plan = plan,
        renderedClaim = renderedClaim,
        contrastAdmissible = contrastAdmissible,
        supportPrimary = supportPrimary,
        supportSecondary = supportSecondary,
        tension = tension,
        evidenceHook = evidenceHook,
        coda = coda,
        surfaceConsequence = surfaceConsequence,
        typedTimingSupport = typedTimingSupport
      )
    )

  def candidate(
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
  ): Candidate =
    Candidate(
      plan = plan,
      renderedClaim = renderedClaim,
      evidences =
        typedEvidences(
          plan = plan,
          contrastAdmissible = contrastAdmissible,
          supportPrimary = supportPrimary,
          supportSecondary = supportSecondary,
          tension = tension,
          evidenceHook = evidenceHook,
          coda = coda,
          surfaceConsequence = surfaceConsequence,
          typedTimingSupport = typedTimingSupport
        )
    )

  private def hasConcreteCausalAnchor(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    LineScopedCitation.hasConcreteSanLine(text) ||
      LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
      low.matches(""".*\b(threat|break|counterplay|mate|wins|loses|drops|fork|pin|trade|recapture|reply)\b.*""")

  private def dominantLineBinding(evidences: List[TypedEvidence]): MoveReviewLocalFact.LineBinding =
    val bindings = evidences.map(_.lineBinding).filter(_ != MoveReviewLocalFact.LineBinding.None)
    if bindings.contains(MoveReviewLocalFact.LineBinding.PvCoupled) then MoveReviewLocalFact.LineBinding.PvCoupled
    else if bindings.contains(MoveReviewLocalFact.LineBinding.Replayed) then MoveReviewLocalFact.LineBinding.Replayed
    else if bindings.contains(MoveReviewLocalFact.LineBinding.BranchScoped) then MoveReviewLocalFact.LineBinding.BranchScoped
    else MoveReviewLocalFact.LineBinding.None

  private def subjectRole(plan: QuestionPlan): SubjectRole =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped ||
      plan.sourceKinds.exists(kind => kind.contains("reply") || kind.contains("threat"))
    then SubjectRole.LineOrReply
    else SubjectRole.PlayedMove

  private def typedEvidences(
      plan: QuestionPlan,
      contrastAdmissible: Boolean,
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      surfaceConsequence: Option[String],
      typedTimingSupport: Boolean
  ): List[TypedEvidence] =
    val explicitReplyLossContrast =
      typedTimingSupport && plan.timingWitness.isEmpty && contrastAdmissible
    List(
      Option.when(contrastAdmissible)(
        supportPrimary.map(text =>
          TypedEvidence(
            kind = EvidenceKind.Contrast,
            source = if explicitReplyLossContrast then EvidenceSource.ExplicitReplyLoss else EvidenceSource.AdmissibleContrast,
            text = Some(text),
            subjectRole = SubjectRole.PlayedMove
          )
        )
      ).flatten,
      supportSecondary.map(text =>
        TypedEvidence(EvidenceKind.SecondaryPlan, EvidenceSource.SecondaryPlan, Some(text), SubjectRole.PlayedMove)
      ),
      tension.map(text =>
        TypedEvidence(EvidenceKind.TimingTension, EvidenceSource.TimingTension, Some(text), SubjectRole.PlayedMove)
      ),
      evidenceHook.map(text =>
        TypedEvidence(
          EvidenceKind.BranchLine,
          EvidenceSource.BranchLine,
          Some(text),
          SubjectRole.LineOrReply,
          lineBinding = MoveReviewLocalFact.LineBinding.BranchScoped
        )
      ),
      surfaceConsequence.map(text =>
        TypedEvidence(
          EvidenceKind.CertifiedConsequence,
          EvidenceSource.LineConsequenceSurface,
          Some(text),
          SubjectRole.PlayedMove,
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled
        )
      ),
      coda
        .filterNot(text => surfaceConsequence.exists(sameText(_, text)))
        .map(text =>
          TypedEvidence(EvidenceKind.CertifiedConsequence, EvidenceSource.PlannerConsequence, Some(text), SubjectRole.PlayedMove)
        ),
      plan.timingWitness.map { witness =>
        TypedEvidence(
          EvidenceKind.TimingWitness,
          EvidenceSource.TimingWitness,
          text = witness.continuationMove.orElse(witness.namedBreak),
          subjectRole = SubjectRole.LineOrReply,
          lineBinding = witness.continuationMove.fold(MoveReviewLocalFact.LineBinding.None)(_ => MoveReviewLocalFact.LineBinding.Replayed),
          guardrails =
            List(
              s"timing_witness_source:${witness.source}",
              s"timing_witness_family:${witness.proofFamily}"
            ) ++ witness.witnessTokens.map(token => s"timing_witness_token:$token")
        )
      }
    ).flatten.distinct

  private def causalRelations(candidate: Candidate): List[RelationKind] =
    val plan = candidate.plan
    val playedMoveConsequence =
      playedMoveCausalOwner(plan) &&
        (hasEvidence(candidate, EvidenceKind.BranchLine) || hasEvidence(candidate, EvidenceKind.CertifiedConsequence))
    List(
      Option.when(hasEvidence(candidate, EvidenceKind.Contrast))(RelationKind.AlternativeContrast),
      Option.when(playedMoveConsequence)(RelationKind.PlayedMoveConsequence),
      Option.when(plan.questionKind == AuthorQuestionKind.WhyNow && hasTypedTimingSupport(candidate))(RelationKind.TimingConstraint),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
          plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
          (
            hasEvidence(candidate, EvidenceKind.BranchLine) ||
              hasEvidence(candidate, EvidenceKind.CertifiedConsequence) ||
              hasEvidence(candidate, EvidenceKind.TimingWitness)
          )
      )(RelationKind.DefensiveResource),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhosePlanIsFaster &&
          plan.plannerOwnerKind == PlannerOwnerKind.PlanRace &&
          (
            hasEvidence(candidate, EvidenceKind.Contrast) ||
              hasEvidence(candidate, EvidenceKind.BranchLine) ||
              hasEvidence(candidate, EvidenceKind.TimingTension) ||
              hasEvidence(candidate, EvidenceKind.CertifiedConsequence)
          )
      )(RelationKind.PlanRace),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatChanged &&
          (playedMoveConsequence || hasEvidence(candidate, EvidenceKind.Contrast))
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

  private def hasTypedTimingSupport(candidate: Candidate): Boolean =
    hasEvidence(candidate, EvidenceSource.TimingWitness) ||
      hasEvidence(candidate, EvidenceSource.ExplicitReplyLoss)

  private def hasEvidence(candidate: Candidate, kind: EvidenceKind): Boolean =
    candidate.evidences.exists(_.kind == kind)

  private def hasEvidence(candidate: Candidate, source: EvidenceSource): Boolean =
    candidate.evidences.exists(_.source == source)

  private def evidenceText(candidate: Candidate, kind: EvidenceKind): Option[String] =
    candidate.evidences.find(_.kind == kind).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def lineConsequenceSurfaceText(candidate: Candidate): Option[String] =
    candidate.evidences.find(_.source == EvidenceSource.LineConsequenceSurface).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def sameText(left: String, right: String): Boolean =
    Option(left).getOrElse("").trim.equalsIgnoreCase(Option(right).getOrElse("").trim)

  private def playedMoveCausalOwner(plan: QuestionPlan): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta ||
      plan.plannerOwnerKind == PlannerOwnerKind.TacticalFailure

  private def openingRelationWhyThis(plan: QuestionPlan): Boolean =
    plan.questionKind == AuthorQuestionKind.WhyThis &&
      (plan.plannerSource == "opening_relation_translator" ||
        plan.sourceKinds.contains("opening_relation_translator"))
