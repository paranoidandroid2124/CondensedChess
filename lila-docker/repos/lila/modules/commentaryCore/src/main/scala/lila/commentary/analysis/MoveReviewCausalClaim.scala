package lila.commentary.analysis

import lila.commentary.analysis.claim.*
import lila.commentary.analysis.semantic.RelationSurfaceRowKind
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility

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
    case Attack
    case Defense
    case Threat
    case Pressure
    case PlanSupport

    def wireName: String =
      this match
        case Contrast             => "contrast"
        case SecondaryPlan        => "secondary_plan"
        case TimingTension        => "timing_tension"
        case BranchLine           => "branch_line"
        case CertifiedConsequence => "certified_consequence"
        case TimingWitness        => "timing_witness"
        case Attack               => "attack"
        case Defense              => "defense"
        case Threat               => "threat"
        case Pressure             => "pressure"
        case PlanSupport          => "plan_support"

  enum EvidenceSource:
    case AdmissibleContrast
    case ExplicitReplyLoss
    case DelayedOnlyMove
    case PreventedResource
    case ExplicitAlternativeCollapse
    case TopEngineMoveWithConcreteConsequence
    case RoleAwareLineConsequence
    case CounterfactualCausalThreat
    case SecondaryPlan
    case TimingTension
    case BranchLine
    case PlannerConsequence
    case LineConsequenceSurface
    case TimingWitness
    case PvCoupledPlanSupport
    case CertifiedOwnerPacket
    case SupportedLocalPacket
    case RelationWitness
    case ForcedLineTruth
    case TypedLocalFact

    def wireName: String =
      this match
        case AdmissibleContrast    => "admissible_contrast"
        case ExplicitReplyLoss     => "explicit_reply_loss"
        case DelayedOnlyMove       => "delayed_only_move"
        case PreventedResource     => "prevented_resource"
        case ExplicitAlternativeCollapse => "explicit_alternative_collapse"
        case TopEngineMoveWithConcreteConsequence => "top_engine_move_with_concrete_consequence"
        case RoleAwareLineConsequence => "role_aware_line_consequence"
        case CounterfactualCausalThreat => "counterfactual_causal_threat"
        case SecondaryPlan         => "secondary_plan"
        case TimingTension         => "timing_tension"
        case BranchLine            => "branch_line"
        case PlannerConsequence    => "planner_consequence"
        case LineConsequenceSurface => "line_consequence_surface"
        case TimingWitness         => "timing_witness"
        case PvCoupledPlanSupport  => "pv_coupled_plan_support"
        case CertifiedOwnerPacket  => "certified_owner_packet"
        case SupportedLocalPacket  => "supported_local_packet"
        case RelationWitness       => "relation_witness"
        case ForcedLineTruth       => "forced_line_truth"
        case TypedLocalFact        => "typed_local_fact"

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

  enum CausalIntent:
    case WhyThisMove
    case WhyNow
    case WhatChanged
    case WhatMattersHere
    case WhatMustBeStopped
    case WhosePlanIsFaster
    case WhatIfNot

    def wireName: String =
      this match
        case WhyThisMove      => "why_this_move"
        case WhyNow           => "why_now"
        case WhatChanged      => "what_changed"
        case WhatMattersHere  => "what_matters_here"
        case WhatMustBeStopped => "what_must_be_stopped"
        case WhosePlanIsFaster => "whose_plan_is_faster"
        case WhatIfNot        => "what_if_not"

  object CausalIntent:
    def fromQuestionKind(kind: AuthorQuestionKind): CausalIntent =
      kind match
        case AuthorQuestionKind.WhyThis           => CausalIntent.WhyThisMove
        case AuthorQuestionKind.WhyNow            => CausalIntent.WhyNow
        case AuthorQuestionKind.WhatChanged       => CausalIntent.WhatChanged
        case AuthorQuestionKind.WhatMattersHere   => CausalIntent.WhatMattersHere
        case AuthorQuestionKind.WhatMustBeStopped => CausalIntent.WhatMustBeStopped
        case AuthorQuestionKind.WhosePlanIsFaster => CausalIntent.WhosePlanIsFaster

  final case class TypedEvidence(
      kind: EvidenceKind,
      source: EvidenceSource,
      text: Option[String],
      subjectRole: SubjectRole,
      lineBinding: MoveReviewLocalFact.LineBinding = MoveReviewLocalFact.LineBinding.None,
      evidenceRefs: List[String] = Nil,
      anchors: List[MoveReviewLocalFact.Anchor] = Nil,
      guardrails: List[String] = Nil,
      localFactFamily: Option[MoveReviewLocalFact.Family] = None,
      authorityTier: Option[ClaimAuthorityTier] = None,
      localFactCandidate: Option[MoveReviewLocalFact.Candidate] = None,
      relationSurface: Option[RelationSurfaceRowKind] = None
  )

  final case class Candidate(
      plan: QuestionPlan,
      renderedClaim: String,
      evidences: List[TypedEvidence]
  )

  final case class CausalRoles(
      subject: SubjectRole,
      playedMove: Boolean,
      lineOrReply: Boolean,
      alternative: Boolean,
      continuation: Boolean
  ):
    def labels: List[String] =
      List(
        Option.when(playedMove)("played_move"),
        Option.when(lineOrReply)("line_or_reply"),
        Option.when(alternative)("alternative"),
        Option.when(continuation)("continuation")
      ).flatten

  final case class SurfaceContract(
      mayUseCheckedLine: Boolean,
      mayUseForced: Boolean,
      mayCompareAlternative: Boolean,
      mayUseTimingClaim: Boolean,
      lineBinding: MoveReviewLocalFact.LineBinding
  ):
    def guardrails: List[String] =
      List(
        s"surface_checked_line=$mayUseCheckedLine",
        s"surface_forced=$mayUseForced",
        s"surface_alternative=$mayCompareAlternative",
        s"surface_timing=$mayUseTimingClaim",
        s"surface_line=${lineBinding.key}"
      )

  final case class SurfacePacket(
      claim: String,
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      supportRenderedInClaim: Boolean,
      guardrails: List[String]
  ):
    def hasSupport: Boolean =
      List(supportPrimary, supportSecondary, tension, evidenceHook, coda).flatten.exists(_.trim.nonEmpty)

  final case class CausalFrame(
      plan: QuestionPlan,
      intent: CausalIntent,
      renderedClaim: String,
      surfaceText: String,
      evidences: List[TypedEvidence],
      relationKinds: List[RelationKind],
      roles: CausalRoles,
      surfaceContract: SurfaceContract
  )

  final case class CertifiedClaim(
      questionKind: AuthorQuestionKind,
      intent: CausalIntent,
      surfaceText: String,
      subjectRole: SubjectRole,
      evidenceKinds: List[EvidenceKind],
      relationKinds: List[RelationKind],
      roles: CausalRoles,
      surfaceContract: SurfaceContract,
      surfacePacket: SurfacePacket,
      supportRenderedInClaim: Boolean,
      supportRequired: Boolean,
      localFact: Option[LocalFactAdmission]
  ):
    def guardrail: String =
      val evidence = evidenceKinds.map(_.wireName).distinct.mkString(",")
      val relations = relationKinds.map(_.wireName).distinct.mkString(",")
      val roleLabels = roles.labels.distinct.mkString(",")
      val embeddedSupport = if supportRenderedInClaim then ", support_embedded=true" else ""
      val surface = surfaceContract.guardrails.mkString(", ")
      val local =
        localFact.fold("")(fact =>
          val factGuardrails =
            Option
              .when(fact.guardrails.nonEmpty)(s", local_fact_guardrails=${fact.guardrails.mkString("+")}")
              .getOrElse("")
          s", local_fact=${fact.family.key}/${fact.authority.key}, local_fact_producer=${fact.producer.key}, local_fact_strict=${fact.strictFallbackEligible}, local_fact_line=${fact.lineBinding.key}$factGuardrails"
        )
      s"MoveReview causal claim: intent=${intent.wireName}, question=${questionKind.toString}, subject=${subjectRole.wireName}, roles=$roleLabels, evidence=$evidence, relations=$relations, $surface$embeddedSupport$local"

  final case class Decision(
      claim: Option[CertifiedClaim],
      rejectReasons: List[String],
      evidences: List[TypedEvidence] = Nil,
      evidenceKinds: List[EvidenceKind] = Nil,
      relationKinds: List[RelationKind] = Nil,
      localFact: Option[LocalFactAdmission] = None,
      localFactRejectReasons: List[String] = Nil,
      frame: Option[CausalFrame] = None
  )

  def admit(candidate: Candidate): Decision =
    admitFrame(assembleFrame(candidate))

  def assembleFrame(candidate: Candidate): CausalFrame =
    val relationKinds = causalRelations(candidate.plan, candidate.evidences)
    val surfaceText =
      certifiedSurfaceText(
        candidate.plan.questionKind,
        candidate.renderedClaim,
        relationKinds,
        evidenceText(candidate.evidences, EvidenceKind.Contrast),
        lineConsequenceSurfaceText(candidate.evidences)
      )
    val roles = causalRoles(candidate.plan, candidate.evidences)
    val frame = CausalFrame(
      plan = candidate.plan,
      intent = CausalIntent.fromQuestionKind(candidate.plan.questionKind),
      renderedClaim = candidate.renderedClaim,
      surfaceText = surfaceText,
      evidences = candidate.evidences,
      relationKinds = relationKinds,
      roles = roles,
      surfaceContract = surfaceContract(relationKinds, candidate.evidences)
    )
    if candidate.plan.plannerOwnerKind == PlannerOwnerKind.ConcreteTactical &&
        !hasConcreteTacticalSurfaceAnchor(frame.surfaceText) &&
        hasConcreteTacticalTypedFactAnchor(frame)
    then concreteTacticalTypedFactSurface(frame.evidences).fold(frame)(text => frame.copy(surfaceText = text))
    else frame

  private def admitFrame(frame: CausalFrame): Decision =
    val plan = frame.plan
    val supportRequired = requiresTypedSupport(plan.questionKind)
    val relationRequired = requiresCausalRelation(plan.questionKind)
    val evidenceKinds = frame.evidences.map(_.kind).distinct
    val relationKinds = frame.relationKinds
    val pvCoupledPlanSupportWithoutTypedEvidence =
      plan.sourceKinds.exists(_.trim == "pv_coupled_plan_support") &&
        !hasEvidence(frame.evidences, EvidenceSource.PvCoupledPlanSupport)
    val localFactDecision =
      promotedLocalFactDecision(frame).getOrElse(
        if pvCoupledPlanSupportWithoutTypedEvidence then
          MoveReviewLocalFact.Decision(None, List("pv_coupled_plan_support_evidence_missing"))
        else
          MoveReviewLocalFact
            .admitPlanner(
              plan,
              evidenceKinds.map(_.wireName),
              relationKinds.map(_.wireName),
              lineConsequenceBacked = lineConsequenceBacked(frame),
              lineBinding = frame.surfaceContract.lineBinding,
              evidenceRefs = localFactEvidenceRefs(frame.evidences),
              guardrails = localFactGuardrails(frame.evidences) ++ frame.surfaceContract.guardrails,
              anchors = frame.evidences.flatMap(_.anchors).distinct
            )
      )
    val localFact = localFactDecision.admission
    val packet = surfacePacket(frame)
    val rejectReasons =
      List(
        Option.when(supportRequired && evidenceKinds.isEmpty)("causal_support_missing"),
        Option.when(supportRequired && relationRequired && relationKinds.isEmpty)("causal_relation_missing"),
        questionRoleRejectReason(plan.questionKind, relationKinds),
        Option.when(
          supportRequired &&
            !hasConcreteCausalAnchor(frame.surfaceText) &&
            frame.evidences.flatMap(_.anchors).isEmpty
        )("causal_claim_anchor_missing"),
        Option.when(
          supportRequired &&
            plan.plannerOwnerKind == PlannerOwnerKind.ConcreteTactical &&
            !hasConcreteTacticalSurfaceAnchor(frame.surfaceText) &&
            !hasConcreteTacticalTypedFactAnchor(frame) &&
            !hasEvidence(frame.evidences, EvidenceSource.RelationWitness) &&
            !hasEvidence(frame.evidences, EvidenceSource.ForcedLineTruth)
        )("concrete_tactical_surface_anchor_missing"),
        Option.when(supportRequired && localFact.isEmpty)("local_fact_admission_missing"),
        Option.when(openingRelationWhyThis(plan) && !hasEvidence(frame.evidences, EvidenceSource.AdmissibleContrast))(
          "opening_relation_contrast_not_admissible"
        )
      ).flatten
    if rejectReasons.nonEmpty then
      Decision(
        None,
        rejectReasons,
        frame.evidences,
        evidenceKinds,
        relationKinds,
        localFact,
        localFactDecision.rejectReasons,
        Some(frame)
      )
    else
      Decision(
        claim =
          Some(
            CertifiedClaim(
              questionKind = plan.questionKind,
              intent = frame.intent,
              surfaceText = frame.surfaceText,
              subjectRole = frame.roles.subject,
              evidenceKinds = evidenceKinds,
              relationKinds = relationKinds,
              roles = frame.roles,
              surfaceContract = frame.surfaceContract,
              surfacePacket = packet,
              supportRenderedInClaim = packet.supportRenderedInClaim,
              supportRequired = supportRequired,
              localFact = localFact
            )
        ),
        rejectReasons = Nil,
        evidences = frame.evidences,
        evidenceKinds = evidenceKinds,
        relationKinds = relationKinds,
        localFact = localFact,
        localFactRejectReasons = localFactDecision.rejectReasons,
        frame = Some(frame)
      )

  def requiresTypedSupport(kind: AuthorQuestionKind): Boolean =
    kind match
      case AuthorQuestionKind.WhyThis | AuthorQuestionKind.WhyNow | AuthorQuestionKind.WhatChanged |
          AuthorQuestionKind.WhatMustBeStopped | AuthorQuestionKind.WhosePlanIsFaster |
          AuthorQuestionKind.WhatMattersHere =>
        true

  private def requiresCausalRelation(kind: AuthorQuestionKind): Boolean =
    kind != AuthorQuestionKind.WhatMattersHere

  private def questionRoleRejectReason(
      questionKind: AuthorQuestionKind,
      relations: List[RelationKind]
  ): Option[String] =
    questionKind match
      case AuthorQuestionKind.WhyThis =>
        Option.when(!whyThisRoleAuthorized(relations))("why_this_role_authority_missing")
      case AuthorQuestionKind.WhyNow =>
        Option.when(!relations.contains(RelationKind.TimingConstraint))("why_now_timing_authority_missing")
      case AuthorQuestionKind.WhatChanged =>
        Option.when(!relations.contains(RelationKind.ChangeConsequence))("what_changed_consequence_authority_missing")
      case AuthorQuestionKind.WhatMustBeStopped =>
        Option.when(!relations.contains(RelationKind.DefensiveResource))("what_must_be_stopped_defensive_authority_missing")
      case AuthorQuestionKind.WhosePlanIsFaster =>
        Option.when(!relations.contains(RelationKind.PlanRace))("plan_race_authority_missing")
      case AuthorQuestionKind.WhatMattersHere =>
        None

  def candidate(
      plan: QuestionPlan,
      renderedClaim: String,
      contrastAdmissible: Boolean,
      contrastSourceKind: Option[String],
      contrastAnchor: Option[String],
      contrastForcedReply: Boolean,
      contrastEvidenceRefs: List[String],
      contrastGuardrails: List[String],
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      surfaceConsequence: Option[String],
      lineConsequenceEvidence: Option[LineConsequenceEvidence],
      pvCoupledPlanSupport: Option[PvCoupledPlanSupport],
      promotedEvidence: List[TypedEvidence] = Nil,
      localFactResult: Option[MoveReviewExplanationBuilder.Result] = None
  ): Candidate =
    Candidate(
      plan = plan,
      renderedClaim = renderedClaim,
      evidences =
        typedEvidences(
          plan = plan,
          contrastAdmissible = contrastAdmissible,
          contrastSourceKind = contrastSourceKind,
          contrastAnchor = contrastAnchor,
          contrastForcedReply = contrastForcedReply,
          contrastEvidenceRefs = contrastEvidenceRefs,
          contrastGuardrails = contrastGuardrails,
          supportPrimary = supportPrimary,
          supportSecondary = supportSecondary,
          tension = tension,
          evidenceHook = evidenceHook,
          coda = coda,
          surfaceConsequence = surfaceConsequence,
          lineConsequenceEvidence = lineConsequenceEvidence,
          pvCoupledPlanSupport = pvCoupledPlanSupport
        ) ++ promotedEvidence ++ localFactResultTypedEvidences(localFactResult)
    )

  def promotedTypedEvidences(
      admissions: List[ClaimAuthorityResolver.PromotedLocalFactAdmission]
  ): List[TypedEvidence] =
    admissions.map { admission =>
      TypedEvidence(
        kind = evidenceKindForFamily(admission.family),
        source = evidenceSourceForDecision(admission.decision),
        text = admission.supportText,
        subjectRole = subjectRoleForPromotedFamily(admission.family, admission.claim),
        lineBinding = admission.lineBinding,
        evidenceRefs = admission.evidenceRefs,
        anchors = admission.anchors,
        guardrails = admission.guardrails,
        localFactFamily = Some(admission.family),
        authorityTier = Some(admission.decision.tier),
        localFactCandidate = Some(admission.localFactCandidate),
        relationSurface = admission.localFactCandidate.relationSurface
      )
    }.distinct

  def localFactResultTypedEvidences(
      result: Option[MoveReviewExplanationBuilder.Result]
  ): List[TypedEvidence] =
    result.toList.flatMap { value =>
      MoveReviewLocalFact.candidateFromAdmission(value.localFact).map { candidate =>
        TypedEvidence(
          kind = evidenceKindForFamily(value.localFact.family),
          source =
            if value.localFact.producer == MoveReviewLocalFact.Producer.RelationWitness then EvidenceSource.RelationWitness
            else if value.localFact.producer == MoveReviewLocalFact.Producer.ForcedLineTruth then EvidenceSource.ForcedLineTruth
            else EvidenceSource.TypedLocalFact,
          text = Option(value.explanation.prose).map(_.trim).filter(_.nonEmpty),
          subjectRole = subjectRoleForLocalFamily(value.localFact.family),
          lineBinding = value.localFact.lineBinding,
          evidenceRefs =
            (
              List(
                s"typed_local_fact_source:${value.explanation.source}",
                s"typed_local_fact_family:${value.localFact.family.key}",
                s"typed_local_fact_producer:${value.localFact.producer.key}"
              ) ++ value.localFact.evidenceRefs
            ).map(_.trim).filter(_.nonEmpty).distinct,
          anchors = value.localFact.anchors,
          guardrails =
            (
              List(
                s"typed_local_fact_source:${value.explanation.source}",
                s"typed_local_fact_producer:${value.localFact.producer.key}"
              ) ++ value.localFact.guardrails
            ).map(_.trim).filter(_.nonEmpty).distinct,
          localFactFamily = Some(value.localFact.family),
          localFactCandidate = Some(candidate),
          relationSurface = value.localFact.relationSurface
        )
      }
    }.distinct

  private def hasConcreteCausalAnchor(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    LineScopedCitation.hasConcreteSanLine(text) ||
      LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
      low.matches(""".*\b(threat|break|counterplay|mate|wins|loses|drops|fork|pin|trade|recapture|reply)\b.*""")

  private def hasConcreteTacticalSurfaceAnchor(text: String): Boolean =
    LineScopedCitation.hasConcreteSanLine(text) ||
      LiveNarrativeCompressionCore.hasConcreteAnchor(text)

  private def hasConcreteTacticalTypedFactAnchor(frame: CausalFrame): Boolean =
    concreteTacticalBranchLineAnchored(frame.evidences) &&
      concreteTacticalTypedFactEvidence(frame.evidences).nonEmpty

  private def concreteTacticalTypedFactSurface(evidences: List[TypedEvidence]): Option[String] =
    concreteTacticalTypedFactEvidence(evidences).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def concreteTacticalBranchLineAnchored(evidences: List[TypedEvidence]): Boolean =
    evidences.exists(evidence =>
      evidence.source == EvidenceSource.BranchLine &&
        (
          evidence.anchors.nonEmpty ||
            evidence.evidenceRefs.exists(_.startsWith("branch_line_first_san:")) ||
            evidence.text.exists(LineScopedCitation.hasConcreteSanLine)
        )
    )

  private def concreteTacticalTypedFactEvidence(evidences: List[TypedEvidence]): Option[TypedEvidence] =
    evidences.find { evidence =>
      val guardrails =
        (evidence.guardrails ++ evidence.localFactCandidate.toList.flatMap(_.guardrails))
          .map(_.trim.toLowerCase)
      val refs =
        (evidence.evidenceRefs ++ evidence.localFactCandidate.toList.flatMap(_.evidenceRefs))
          .map(_.trim.toLowerCase)
      evidence.source == EvidenceSource.TypedLocalFact &&
        evidence.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
        evidence.localFactCandidate.exists(_.strictFallbackCandidate) &&
        evidence.localFactFamily.exists(family =>
          family == MoveReviewLocalFact.Family.Attack ||
            family == MoveReviewLocalFact.Family.Threat ||
            family == MoveReviewLocalFact.Family.Pressure
        ) &&
        guardrails.exists(guardrail =>
          guardrail == "target_fact_attacked_by_played_move" ||
            guardrail == "target_fact_defended_by_played_move"
        ) &&
        refs.exists(_.startsWith("fact_square:"))
    }

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
      contrastSourceKind: Option[String],
      contrastAnchor: Option[String],
      contrastForcedReply: Boolean,
      contrastEvidenceRefs: List[String],
      contrastGuardrails: List[String],
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      surfaceConsequence: Option[String],
      lineConsequenceEvidence: Option[LineConsequenceEvidence],
      pvCoupledPlanSupport: Option[PvCoupledPlanSupport]
  ): List[TypedEvidence] =
    List(
      Option.when(contrastAdmissible)(
        supportPrimary.map(text =>
          val localFactCandidate =
            contrastLocalFactCandidate(plan, contrastSourceKind, contrastAnchor, contrastForcedReply, contrastEvidenceRefs, contrastGuardrails)
          TypedEvidence(
            kind = EvidenceKind.Contrast,
            source = contrastEvidenceSource(contrastSourceKind),
            text = Some(text),
            subjectRole = contrastSubjectRole(contrastSourceKind),
            lineBinding = contrastLineBinding(contrastSourceKind),
            evidenceRefs = (contrastAnchor.map(anchor => s"contrast_anchor:$anchor").toList ++ contrastEvidenceRefs).distinct,
            anchors = contrastAnchor.map(anchor => MoveReviewLocalFact.Anchor("contrast_anchor", anchor)).toList,
            guardrails = contrastGuardrails,
            localFactFamily = localFactCandidate.map(_.family),
            localFactCandidate = localFactCandidate
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
          lineBinding = MoveReviewLocalFact.LineBinding.BranchScoped,
          anchors = branchLineAnchors(text),
          evidenceRefs = branchLineEvidenceRefs(text)
        )
      ),
      lineConsequenceEvidence.map(evidence =>
        val localFactCandidate = lineConsequenceLocalFactCandidate(plan, evidence, surfaceConsequence)
        TypedEvidence(
          EvidenceKind.CertifiedConsequence,
          EvidenceSource.LineConsequenceSurface,
          surfaceConsequence,
          SubjectRole.PlayedMove,
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = MoveReviewLocalFact.lineConsequenceEvidenceRefs(evidence),
          anchors = MoveReviewLocalFact.lineConsequenceAnchors(evidence),
          guardrails = MoveReviewLocalFact.lineConsequenceGuardrails(evidence),
          localFactFamily = localFactCandidate.map(_.family),
          localFactCandidate = localFactCandidate
        )
      ),
      pvCoupledPlanSupport
        .filter(pvCoupledPlanSupportAdmissible(plan, _))
        .map(support =>
          val candidate =
            MoveReviewLocalFact.Candidate(
              family = MoveReviewLocalFact.Family.PlanSupport,
              source = MoveReviewLocalFact.Source.PvCoupledLine,
              producer = MoveReviewLocalFact.Producer.CertifiedStrategyDelta,
              subject = MoveReviewLocalFact.Subject.PlanResource,
              strictFallbackCandidate = false,
              anchors = pvCoupledPlanSupportAnchors(support),
              lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
              evidenceRefs = pvCoupledPlanSupportEvidenceRefs(support),
              guardrails = pvCoupledPlanSupportGuardrails(support)
            )
          TypedEvidence(
            EvidenceKind.PlanSupport,
            EvidenceSource.PvCoupledPlanSupport,
            Some(support.claim),
            SubjectRole.PlayedMove,
            lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
            evidenceRefs = pvCoupledPlanSupportEvidenceRefs(support),
            anchors = pvCoupledPlanSupportAnchors(support),
            guardrails = pvCoupledPlanSupportGuardrails(support),
            localFactFamily = Some(MoveReviewLocalFact.Family.PlanSupport),
            localFactCandidate = Some(candidate)
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
          evidenceRefs = timingWitnessEvidenceRefs(witness),
          anchors = timingWitnessAnchors(witness),
          guardrails =
            List(
              s"timing_witness_source:${witness.source}",
              s"timing_witness_family:${witness.proofFamily}"
            ) ++ witness.witnessTokens.map(token => s"timing_witness_token:$token")
        )
      }
    ).flatten.distinct

  private def lineConsequenceLocalFactCandidate(
      plan: QuestionPlan,
      evidence: LineConsequenceEvidence,
      surfaceConsequence: Option[String]
  ): Option[MoveReviewLocalFact.Candidate] =
    Option
      .when(lineConsequenceSurfaceOwnsClaim(plan, evidence, surfaceConsequence))(
        MoveReviewLocalFact.lineConsequenceCandidate(evidence, strictFallbackCandidate = false)
      )

  private def lineConsequenceSurfaceOwnsClaim(
      plan: QuestionPlan,
      evidence: LineConsequenceEvidence,
      surfaceConsequence: Option[String]
  ): Boolean =
    surfaceConsequence.exists(_.trim.nonEmpty) &&
      evidence.surfaceReady &&
      evidence.kind != LineConsequenceKind.PreviewOnly &&
      (
        plan.plannerOwnerKind == PlannerOwnerKind.LineConsequence ||
          plan.questionKind == AuthorQuestionKind.WhatChanged ||
          plan.questionKind == AuthorQuestionKind.WhyThis ||
          onlyMoveLineConsequenceTiming(plan)
      )

  private def onlyMoveLineConsequenceTiming(plan: QuestionPlan): Boolean =
    plan.questionKind == AuthorQuestionKind.WhyNow &&
      plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
      plan.plannerSource == "only_move_defense" &&
      plan.sourceKinds.exists(_.trim == "line_consequence")

  private def pvCoupledPlanSupportAdmissible(plan: QuestionPlan, support: PvCoupledPlanSupport): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta &&
      plan.sourceKinds.exists(_.trim == "pv_coupled_plan_support") &&
      support.planName.trim.nonEmpty &&
      support.playedSan.trim.nonEmpty &&
      support.anchorMatched &&
      LineScopedCitation.hasConcreteSanLine(support.evidenceLine) &&
      LineScopedCitation.firstConcreteSanToken(support.evidenceLine).exists(token => sameText(token, support.playedSan))

  private def pvCoupledPlanSupportEvidenceRefs(support: PvCoupledPlanSupport): List[String] =
    (
      List(
        "evidence_source:pv_coupled_plan_support",
        s"plan_name:${support.planName}",
        s"played_san:${support.playedSan}",
        "branch_line_meaningful:true",
        s"plan_anchor_matched:${support.anchorMatched}"
      ) ++
        LineScopedCitation.firstConcreteSanToken(support.evidenceLine).map(token => s"branch_line_first_san:$token") ++
        support.planAnchorLine.map(line => s"plan_anchor_line:${line.take(80)}") ++
        support.anchorTokens.take(4).map(token => s"plan_anchor_token:$token") ++
        support.matchedAnchorTokens.take(4).map(token => s"plan_anchor_matched_token:$token")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def pvCoupledPlanSupportAnchors(support: PvCoupledPlanSupport): List[MoveReviewLocalFact.Anchor] =
    (
      List(
        MoveReviewLocalFact.Anchor("plan_name", support.planName),
        MoveReviewLocalFact.Anchor("played_san", support.playedSan),
        MoveReviewLocalFact.Anchor("plan_anchor_matched", support.anchorMatched.toString)
      ) ++
        LineScopedCitation.firstConcreteSanToken(support.evidenceLine).map(token =>
          MoveReviewLocalFact.Anchor("branch_line_first_san", token)
        ) ++
        support.matchedAnchorTokens.headOption.map(token => MoveReviewLocalFact.Anchor("plan_anchor_matched_token", token))
    ).distinct

  private def pvCoupledPlanSupportGuardrails(support: PvCoupledPlanSupport): List[String] =
    (
      List(
        "pv_coupled_plan_support",
        s"plan_name:${support.planName}",
        s"played_san:${support.playedSan}",
        "played_first_branch_line",
        "meaningful_branch_line",
        s"plan_anchor_matched:${support.anchorMatched}"
      ) ++
        LineScopedCitation.firstConcreteSanToken(support.evidenceLine).map(token => s"branch_line_first_san:$token") ++
        support.anchorTokens.take(4).map(token => s"plan_anchor_token:$token") ++
        support.matchedAnchorTokens.take(4).map(token => s"plan_anchor_matched_token:$token")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def contrastEvidenceSource(contrastSourceKind: Option[String]): EvidenceSource =
    contrastSourceKind match
      case Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss) =>
        EvidenceSource.ExplicitReplyLoss
      case Some(ContrastiveSupportAdmissibility.SourceKind.DelayedOnlyMove) =>
        EvidenceSource.DelayedOnlyMove
      case Some(ContrastiveSupportAdmissibility.SourceKind.PreventedResource) =>
        EvidenceSource.PreventedResource
      case Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitAlternativeCollapse) =>
        EvidenceSource.ExplicitAlternativeCollapse
      case Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence) =>
        EvidenceSource.TopEngineMoveWithConcreteConsequence
      case Some(ContrastiveSupportAdmissibility.SourceKind.RoleAwareLineConsequence) =>
        EvidenceSource.RoleAwareLineConsequence
      case Some(ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat) =>
        EvidenceSource.CounterfactualCausalThreat
      case _ =>
        EvidenceSource.AdmissibleContrast

  private def contrastLineBinding(contrastSourceKind: Option[String]): MoveReviewLocalFact.LineBinding =
    contrastSourceKind match
      case Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence) =>
        MoveReviewLocalFact.LineBinding.PvCoupled
      case Some(ContrastiveSupportAdmissibility.SourceKind.RoleAwareLineConsequence) =>
        MoveReviewLocalFact.LineBinding.PvCoupled
      case Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitAlternativeCollapse) =>
        MoveReviewLocalFact.LineBinding.BranchScoped
      case _ =>
        MoveReviewLocalFact.LineBinding.None

  private def contrastSubjectRole(contrastSourceKind: Option[String]): SubjectRole =
    contrastSourceKind match
      case Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss) => SubjectRole.LineOrReply
      case _                                                                 => SubjectRole.PlayedMove

  private def contrastLocalFactCandidate(
      plan: QuestionPlan,
      contrastSourceKind: Option[String],
      contrastAnchor: Option[String],
      contrastForcedReply: Boolean,
      contrastEvidenceRefs: List[String],
      contrastGuardrails: List[String]
  ): Option[MoveReviewLocalFact.Candidate] =
    contrastSourceKind match
      case Some(source)
          if source == ContrastiveSupportAdmissibility.SourceKind.RoleAwareLineConsequence =>
        Some(
          MoveReviewLocalFact.Candidate(
            family = MoveReviewLocalFact.Family.LineConsequence,
            source = MoveReviewLocalFact.Source.AlternativeComparison,
            producer = MoveReviewLocalFact.Producer.AlternativeComparison,
            subject = MoveReviewLocalFact.Subject.LineOrReply,
            strictFallbackCandidate = true,
            anchors =
              (List(MoveReviewLocalFact.Anchor("contrast_source", source)) ++
                contrastAnchor.map(anchor => MoveReviewLocalFact.Anchor("engine_best_anchor", anchor))).distinct,
            lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
            evidenceRefs =
              (
                List(s"contrast_source:$source") ++
                  contrastAnchor.map(anchor => s"engine_best_anchor:$anchor") ++
                  contrastEvidenceRefs
              ).distinct,
            guardrails =
              (
                List(source, "alternative_role:engine_best_branch", "alternative_role:played_branch") ++
                  contrastGuardrails
              ).distinct
          )
        )
      case Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss) =>
        val source = ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss
        Some(
          MoveReviewLocalFact.Candidate(
            family =
              if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped then MoveReviewLocalFact.Family.Defense
              else MoveReviewLocalFact.Family.Timing,
            source = MoveReviewLocalFact.Source.ForcedReply,
            producer = MoveReviewLocalFact.Producer.ForcedReply,
            subject = MoveReviewLocalFact.Subject.LineOrReply,
            strictFallbackCandidate = true,
            anchors =
              (List(MoveReviewLocalFact.Anchor("contrast_source", source)) ++
                contrastAnchor.map(anchor => MoveReviewLocalFact.Anchor("reply_anchor", anchor))).distinct,
            evidenceRefs =
              (
                List(s"contrast_source:$source", s"forced_reply_unique:$contrastForcedReply") ++
                  contrastAnchor.map(anchor => s"reply_anchor:$anchor") ++
                  contrastEvidenceRefs
              ).distinct,
            guardrails =
              (
                List(source, if contrastForcedReply then "forced_reply_unique" else "forced_reply_non_unique") ++
                  contrastGuardrails
              ).distinct
          )
        )
      case Some(ContrastiveSupportAdmissibility.SourceKind.DelayedOnlyMove) =>
        val source = ContrastiveSupportAdmissibility.SourceKind.DelayedOnlyMove
        Some(
          MoveReviewLocalFact.Candidate(
            family = MoveReviewLocalFact.Family.Timing,
            source = MoveReviewLocalFact.Source.ForcedReply,
            producer = MoveReviewLocalFact.Producer.ForcedReply,
            subject = MoveReviewLocalFact.Subject.PlayedMove,
            strictFallbackCandidate = true,
            anchors =
              (List(MoveReviewLocalFact.Anchor("contrast_source", source)) ++
                contrastAnchor.map(anchor => MoveReviewLocalFact.Anchor("only_move_anchor", anchor))).distinct,
            evidenceRefs =
              (
                List(s"contrast_source:$source", "only_move_timing:true") ++
                  contrastAnchor.map(anchor => s"only_move_anchor:$anchor") ++
                  contrastEvidenceRefs
              ).distinct,
            guardrails =
              (
                List(source, "only_move_timing", "forced_reply_unique") ++
                  contrastGuardrails
              ).distinct
          )
        )
      case Some(ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat) =>
        val source = ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat
        Some(
          MoveReviewLocalFact.Candidate(
            family = MoveReviewLocalFact.Family.Threat,
            source = MoveReviewLocalFact.Source.CertifiedStrategy,
            producer = MoveReviewLocalFact.Producer.PlannerCausalClaim,
            subject = MoveReviewLocalFact.Subject.LineOrReply,
            strictFallbackCandidate = false,
            anchors =
              (List(MoveReviewLocalFact.Anchor("contrast_source", source)) ++
                contrastAnchor.map(anchor => MoveReviewLocalFact.Anchor("contrast_anchor", anchor))).distinct,
            evidenceRefs =
              (List(s"contrast_source:$source") ++ contrastAnchor.map(anchor => s"contrast_anchor:$anchor")).distinct,
            guardrails =
              List(
                "counterfactual_causal_threat:motif_backed",
                s"planner_owner:${plan.plannerOwnerKind.wireName}",
                s"planner_source:${plan.plannerSource}"
              )
          )
        )
      case _ =>
        None

  private def branchLineAnchors(text: String): List[MoveReviewLocalFact.Anchor] =
    firstBranchLineToken(text).map(token => MoveReviewLocalFact.Anchor("branch_line_first_san", token)).toList

  private def branchLineEvidenceRefs(text: String): List[String] =
    firstBranchLineToken(text).map(token => s"branch_line_first_san:$token").toList

  private def firstBranchLineToken(text: String): Option[String] =
    LineScopedCitation.firstConcreteSanToken(text)

  private def lineConsequenceBacked(frame: CausalFrame): Boolean =
    hasEvidence(frame.evidences, EvidenceSource.LineConsequenceSurface) ||
      hasEvidence(frame.evidences, EvidenceSource.RoleAwareLineConsequence) ||
      (
        frame.relationKinds.contains(RelationKind.ChangeConsequence) &&
          frame.evidences.exists(evidence =>
            evidence.source == EvidenceSource.TopEngineMoveWithConcreteConsequence ||
              evidence.source == EvidenceSource.RoleAwareLineConsequence ||
              evidence.source == EvidenceSource.ExplicitAlternativeCollapse
          )
      )

  private def localFactEvidenceRefs(evidences: List[TypedEvidence]): List[String] =
    evidences
      .flatMap(evidence =>
        List(
          s"evidence_source:${evidence.source.wireName}",
          s"evidence_subject:${evidence.subjectRole.wireName}"
        ) ++
          Option
            .when(evidence.lineBinding != MoveReviewLocalFact.LineBinding.None)(s"evidence_line_binding:${evidence.lineBinding.key}")
            .toList ++
          evidence.evidenceRefs
      )
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct

  private def localFactGuardrails(evidences: List[TypedEvidence]): List[String] =
    evidences.flatMap(_.guardrails).map(_.trim).filter(_.nonEmpty).distinct

  private def promotedLocalFactDecision(frame: CausalFrame): Option[MoveReviewLocalFact.Decision] =
    val evidences = frame.evidences.filter(_.localFactCandidate.nonEmpty)
    val preferred =
      if onlyMoveLineConsequenceTiming(frame.plan) then
        evidences
          .find(evidence =>
            evidence.source == EvidenceSource.LineConsequenceSurface &&
              evidence.localFactFamily.contains(MoveReviewLocalFact.Family.LineConsequence)
          )
          .orElse(evidences.find(_.source != EvidenceSource.DelayedOnlyMove))
          .orElse(evidences.headOption)
      else evidences.headOption
    preferred.flatMap { evidence =>
      val candidate = evidence.localFactCandidate.get
      val decision =
        MoveReviewLocalFact.admit(
          candidate.copy(
            evidenceRefs = localFactEvidenceRefs(List(evidence)),
            guardrails = (candidate.guardrails ++ localFactGuardrails(List(evidence)) ++ frame.surfaceContract.guardrails).distinct
          )
        )
      Option.when(decision.admission.nonEmpty)(decision)
    }

  private def timingWitnessEvidenceRefs(witness: QuestionPlanTimingWitness): List[String] =
    (
      List(
        s"timing_witness_source:${witness.source}",
        s"timing_witness_family:${witness.proofFamily}"
      ) ++
        witness.namedBreak.map(value => s"timing_witness_break:$value") ++
        witness.continuationMove.map(value => s"timing_witness_continuation:$value") ++
        witness.branchKey.map(value => s"timing_witness_branch:$value") ++
        witness.witnessTokens.map(token => s"timing_witness_token:$token")
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def timingWitnessAnchors(witness: QuestionPlanTimingWitness): List[MoveReviewLocalFact.Anchor] =
    (
      List(
        Some(MoveReviewLocalFact.Anchor("timing_source", witness.source)),
        Some(MoveReviewLocalFact.Anchor("timing_family", witness.proofFamily)),
        witness.namedBreak.map(value => MoveReviewLocalFact.Anchor("timing_break", value)),
        witness.continuationMove.map(value => MoveReviewLocalFact.Anchor("timing_continuation", value)),
        witness.branchKey.map(value => MoveReviewLocalFact.Anchor("timing_branch", value))
      ).flatten ++ witness.witnessTokens.map(token => MoveReviewLocalFact.Anchor("timing_token", token))
    ).distinct

  private def causalRelations(plan: QuestionPlan, evidences: List[TypedEvidence]): List[RelationKind] =
    val promotedPlayedMoveEffect =
      playedMoveCausalOwner(plan) &&
        evidences.exists(evidence =>
          evidence.localFactFamily.exists(family =>
            Set(
              MoveReviewLocalFact.Family.Attack,
              MoveReviewLocalFact.Family.Defense,
              MoveReviewLocalFact.Family.Threat,
              MoveReviewLocalFact.Family.Pressure,
              MoveReviewLocalFact.Family.PlanSupport,
              MoveReviewLocalFact.Family.LineConsequence
            ).contains(family)
          )
        )
    val playedMoveConsequence =
      playedMoveCausalOwner(plan) &&
        (hasEvidence(evidences, EvidenceKind.BranchLine) || hasEvidence(evidences, EvidenceKind.CertifiedConsequence) ||
          promotedPlayedMoveEffect)
    val alternativeContrast = hasAlternativeContrast(evidences)
    val onlyMoveLineConsequenceTimingSupport =
      onlyMoveLineConsequenceTiming(plan) &&
        hasEvidence(evidences, EvidenceSource.LineConsequenceSurface)
    List(
      Option.when(alternativeContrast)(RelationKind.AlternativeContrast),
      Option.when(playedMoveConsequence)(RelationKind.PlayedMoveConsequence),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhyNow &&
          (hasTypedTimingSupport(evidences) || onlyMoveLineConsequenceTimingSupport)
      )(RelationKind.TimingConstraint),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
          plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
          (
            hasEvidence(evidences, EvidenceKind.BranchLine) ||
              hasEvidence(evidences, EvidenceKind.CertifiedConsequence) ||
              hasEvidence(evidences, EvidenceKind.TimingWitness) ||
              hasPromotedFamily(evidences, MoveReviewLocalFact.Family.Defense)
          )
      )(RelationKind.DefensiveResource),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhosePlanIsFaster &&
          plan.plannerOwnerKind == PlannerOwnerKind.PlanRace &&
          (
            hasEvidence(evidences, EvidenceKind.Contrast) ||
            hasEvidence(evidences, EvidenceKind.BranchLine) ||
              hasEvidence(evidences, EvidenceKind.TimingTension) ||
              hasEvidence(evidences, EvidenceKind.CertifiedConsequence) ||
              hasPromotedFamily(evidences, MoveReviewLocalFact.Family.PlanSupport)
          )
      )(RelationKind.PlanRace),
      Option.when(
        plan.questionKind == AuthorQuestionKind.WhatChanged &&
          (playedMoveConsequence || alternativeContrast)
      )(RelationKind.ChangeConsequence)
    ).flatten.distinct

  private def causalRoles(plan: QuestionPlan, evidences: List[TypedEvidence]): CausalRoles =
    val subject = subjectRole(plan)
    CausalRoles(
      subject = subject,
      playedMove = subject == SubjectRole.PlayedMove || evidences.exists(_.subjectRole == SubjectRole.PlayedMove),
      lineOrReply = subject == SubjectRole.LineOrReply || evidences.exists(_.subjectRole == SubjectRole.LineOrReply),
      alternative = hasAlternativeContrast(evidences),
      continuation = evidences.exists(_.lineBinding != MoveReviewLocalFact.LineBinding.None)
    )

  private def surfaceContract(
      relations: List[RelationKind],
      evidences: List[TypedEvidence]
  ): SurfaceContract =
    val lineBinding = dominantLineBinding(evidences)
    SurfaceContract(
      mayUseCheckedLine =
        lineBinding != MoveReviewLocalFact.LineBinding.None &&
          (
            relations.contains(RelationKind.PlayedMoveConsequence) ||
              relations.contains(RelationKind.ChangeConsequence) ||
              relations.contains(RelationKind.PlanRace)
          ),
      mayUseForced = evidences.exists(evidence =>
        evidence.source == EvidenceSource.ExplicitReplyLoss &&
          evidence.guardrails.exists(_ == "forced_reply_unique")
      ),
      mayCompareAlternative = relations.contains(RelationKind.AlternativeContrast),
      mayUseTimingClaim = relations.contains(RelationKind.TimingConstraint),
      lineBinding = lineBinding
    )

  private def surfacePacket(frame: CausalFrame): SurfacePacket =
    val contract = frame.surfaceContract
    val claim = frame.surfaceText
    val normalizedClaim = normalizeSurfaceText(claim)
    val lineConsequenceRenderedAsClaim =
      frame.evidences.exists { evidence =>
        val ownedOnlyMoveLineConsequenceAnchor =
          onlyMoveLineConsequenceTiming(frame.plan) &&
            evidence.localFactFamily.contains(MoveReviewLocalFact.Family.LineConsequence) &&
            evidence.anchors.exists(anchor =>
              val normalizedAnchor = normalizeSurfaceText(anchor.value)
              normalizedAnchor.nonEmpty && normalizedClaim.contains(normalizedAnchor)
            )
        evidence.source == EvidenceSource.LineConsequenceSurface &&
          (evidence.text.exists(redundantSurfaceText(_, claim)) || ownedOnlyMoveLineConsequenceAnchor)
      }
    val promotableEvidences =
      if lineConsequenceRenderedAsClaim && frame.plan.plannerOwnerKind == PlannerOwnerKind.LineConsequence then
        frame.evidences.filterNot(evidence =>
          evidence.source == EvidenceSource.TypedLocalFact &&
            evidence.localFactFamily.contains(MoveReviewLocalFact.Family.Threat) &&
            evidence.localFactCandidate.exists(candidate =>
              candidate.source == MoveReviewLocalFact.Source.CanonicalFact &&
                candidate.producer == MoveReviewLocalFact.Producer.TacticalMotif
            ) &&
            evidence.evidenceRefs.exists(_ == "tactical_kind:discovered_attack")
        )
      else frame.evidences
    val promotedSupport =
      promotedEvidenceText(promotableEvidences).filterNot(sameText(_, claim))
    val typedEvidenceRenderedAsClaim =
      frame.evidences.exists(evidence =>
        evidence.localFactFamily.nonEmpty &&
          evidence.text.exists(text => sameText(text, claim) || redundantSurfaceText(text, claim)) &&
          (
            evidence.source == EvidenceSource.CertifiedOwnerPacket ||
              evidence.source == EvidenceSource.SupportedLocalPacket ||
              evidence.source == EvidenceSource.RelationWitness ||
              evidence.source == EvidenceSource.ForcedLineTruth ||
              evidence.source == EvidenceSource.TypedLocalFact
          )
      )
    val mayUseDefensiveSupport =
      frame.relationKinds.contains(RelationKind.DefensiveResource)
    val contrast =
      Option
        .when(contract.mayCompareAlternative || contract.mayUseForced || mayUseDefensiveSupport || contract.mayUseTimingClaim)(
          frame.evidences
            .find(_.kind == EvidenceKind.Contrast)
            .filterNot(evidence => lineConsequenceRenderedAsClaim && evidence.source == EvidenceSource.DelayedOnlyMove)
            .flatMap(_.text.map(_.trim).filter(_.nonEmpty))
            .filterNot(sameText(_, claim))
        )
        .flatten
    val secondary =
      Option
        .when(contract.mayCompareAlternative || contract.mayUseForced || mayUseDefensiveSupport || frame.relationKinds.contains(RelationKind.PlanRace))(
          evidenceText(frame.evidences, EvidenceKind.SecondaryPlan).filterNot(sameText(_, claim))
        )
        .flatten
    val tension =
      Option
        .when(contract.mayUseTimingClaim)(
          evidenceText(frame.evidences, EvidenceKind.TimingTension).filterNot(sameText(_, claim))
        )
        .flatten
    val evidenceHook =
      Option
        .when(contract.mayUseCheckedLine)(
          evidenceText(frame.evidences, EvidenceKind.BranchLine).filterNot(sameText(_, claim))
        )
        .flatten
    val coda =
      Option.unless(contract.mayCompareAlternative) {
        plannerConsequenceText(frame.evidences)
          .orElse(Option.when(contract.mayUseCheckedLine)(lineConsequenceSurfaceText(frame.evidences)).flatten)
          .filterNot(redundantSurfaceText(_, claim))
      }.flatten
    SurfacePacket(
      claim = claim,
      supportPrimary = contrast.orElse(promotedSupport),
      supportSecondary = secondary,
      tension = tension,
      evidenceHook = evidenceHook,
      coda = coda,
      supportRenderedInClaim = !sameText(frame.surfaceText, frame.renderedClaim) || typedEvidenceRenderedAsClaim || lineConsequenceRenderedAsClaim,
      guardrails = contract.guardrails
    )

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

  private def hasTypedTimingSupport(evidences: List[TypedEvidence]): Boolean =
    hasEvidence(evidences, EvidenceSource.TimingWitness) ||
      hasEvidence(evidences, EvidenceSource.ExplicitReplyLoss) ||
      hasEvidence(evidences, EvidenceSource.DelayedOnlyMove) ||
      evidences.exists(evidence =>
        evidence.kind == EvidenceKind.TimingWitness &&
          evidence.source == EvidenceSource.TypedLocalFact &&
          evidence.localFactFamily.contains(MoveReviewLocalFact.Family.Timing)
      ) ||
      evidences.exists(evidence =>
        evidence.kind == EvidenceKind.TimingWitness &&
          evidence.source == EvidenceSource.RelationWitness &&
          evidence.localFactFamily.contains(MoveReviewLocalFact.Family.Timing) &&
          evidence.relationSurface.contains(RelationSurfaceRowKind.MoveOrder)
      )

  private def hasAlternativeContrast(evidences: List[TypedEvidence]): Boolean =
    evidences.exists(evidence =>
      evidence.kind == EvidenceKind.Contrast &&
        (
            evidence.source == EvidenceSource.AdmissibleContrast ||
            evidence.source == EvidenceSource.ExplicitAlternativeCollapse ||
            evidence.source == EvidenceSource.TopEngineMoveWithConcreteConsequence ||
            evidence.source == EvidenceSource.RoleAwareLineConsequence ||
            evidence.source == EvidenceSource.CounterfactualCausalThreat
        )
    )

  private def hasPromotedFamily(evidences: List[TypedEvidence], family: MoveReviewLocalFact.Family): Boolean =
    evidences.exists(_.localFactFamily.contains(family))

  private def hasEvidence(evidences: List[TypedEvidence], kind: EvidenceKind): Boolean =
    evidences.exists(_.kind == kind)

  private def hasEvidence(evidences: List[TypedEvidence], source: EvidenceSource): Boolean =
    evidences.exists(_.source == source)

  private def evidenceText(evidences: List[TypedEvidence], kind: EvidenceKind): Option[String] =
    evidences.find(_.kind == kind).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def lineConsequenceSurfaceText(evidences: List[TypedEvidence]): Option[String] =
    evidences.find(_.source == EvidenceSource.LineConsequenceSurface).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def plannerConsequenceText(evidences: List[TypedEvidence]): Option[String] =
    evidences.find(_.source == EvidenceSource.PlannerConsequence).flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def promotedEvidenceText(evidences: List[TypedEvidence]): Option[String] =
    evidences
      .find(evidence =>
        evidence.source == EvidenceSource.CertifiedOwnerPacket ||
          evidence.source == EvidenceSource.SupportedLocalPacket ||
          evidence.source == EvidenceSource.RelationWitness ||
          evidence.source == EvidenceSource.ForcedLineTruth ||
          evidence.source == EvidenceSource.TypedLocalFact
      )
      .flatMap(_.text.map(_.trim).filter(_.nonEmpty))

  private def sameText(left: String, right: String): Boolean =
    Option(left).getOrElse("").trim.equalsIgnoreCase(Option(right).getOrElse("").trim)

  private def redundantSurfaceText(candidate: String, claim: String): Boolean =
    sameText(candidate, claim) ||
      {
        val normalizedCandidate = normalizeSurfaceText(candidate)
        val normalizedClaim = normalizeSurfaceText(claim)
        normalizedCandidate.nonEmpty && normalizedClaim.contains(normalizedCandidate)
      }

  private def normalizeSurfaceText(value: String): String =
    Option(value).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9]+""", "")

  private def playedMoveCausalOwner(plan: QuestionPlan): Boolean =
    plan.plannerOwnerKind == PlannerOwnerKind.MoveDelta ||
      plan.plannerOwnerKind == PlannerOwnerKind.ConcreteTactical ||
      plan.plannerOwnerKind == PlannerOwnerKind.LineConsequence ||
      (
        plan.questionKind == AuthorQuestionKind.WhyThis &&
          plan.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
          plan.sourceKinds.contains(MoveReviewLocalFact.Producer.ForkEntryDefense.key)
      )

  private def openingRelationWhyThis(plan: QuestionPlan): Boolean =
    plan.questionKind == AuthorQuestionKind.WhyThis &&
      (plan.plannerSource == "opening_relation_translator" ||
        plan.sourceKinds.contains("opening_relation_translator"))

  private def evidenceKindForFamily(family: MoveReviewLocalFact.Family): EvidenceKind =
    family match
      case MoveReviewLocalFact.Family.Attack          => EvidenceKind.Attack
      case MoveReviewLocalFact.Family.Defense         => EvidenceKind.Defense
      case MoveReviewLocalFact.Family.Threat          => EvidenceKind.Threat
      case MoveReviewLocalFact.Family.Pressure        => EvidenceKind.Pressure
      case MoveReviewLocalFact.Family.PlanSupport     => EvidenceKind.PlanSupport
      case MoveReviewLocalFact.Family.LineConsequence => EvidenceKind.CertifiedConsequence
      case MoveReviewLocalFact.Family.Timing          => EvidenceKind.TimingWitness
      case _                                          => EvidenceKind.CertifiedConsequence

  private def evidenceSourceForDecision(decision: ClaimAuthorityDecision): EvidenceSource =
    decision.tier match
      case ClaimAuthorityTier.CertifiedOwner => EvidenceSource.CertifiedOwnerPacket
      case ClaimAuthorityTier.SupportedLocal => EvidenceSource.SupportedLocalPacket
      case _                                 => EvidenceSource.SupportedLocalPacket

  private def subjectRoleForPromotedFamily(
      family: MoveReviewLocalFact.Family,
      claim: Option[MainPathScopedClaim]
  ): SubjectRole =
    if claim.exists(_.scope == PlayerFacingClaimScope.LineScoped) then SubjectRole.LineOrReply
    else subjectRoleForLocalFamily(family)

  private def subjectRoleForLocalFamily(family: MoveReviewLocalFact.Family): SubjectRole =
    if family == MoveReviewLocalFact.Family.Defense ||
      family == MoveReviewLocalFact.Family.Threat
    then SubjectRole.LineOrReply
    else SubjectRole.PlayedMove
