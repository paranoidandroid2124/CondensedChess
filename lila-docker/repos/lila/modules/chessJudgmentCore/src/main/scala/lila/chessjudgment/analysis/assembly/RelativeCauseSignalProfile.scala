package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
import lila.chessjudgment.model.Motif
import lila.chessjudgment.model.judgment.*

private[chessjudgment] final case class RelativeCauseDraft(
    kind: RelativeCauseKind,
    support: List[EvidenceRecord],
    sourceSide: Option[RelativeCauseSourceSide] = None
)

private[chessjudgment] final case class RelativeCauseSignalProfile(
    fact: CandidateComparisonFact,
    referenceRecords: List[EvidenceRecord],
    candidateRecords: List[EvidenceRecord],
    sharedRecords: List[EvidenceRecord]
):
  val involvedRecords: List[EvidenceRecord] =
    (referenceRecords ++ candidateRecords).distinctBy(_.ref.id)
  val allRecords: List[EvidenceRecord] =
    (referenceRecords ++ candidateRecords ++ sharedRecords).distinctBy(_.ref.id)
  val badLoss: Boolean =
    fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP
  val tacticalLoss: Boolean =
    fact.comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP
  val majorLoss: Boolean =
    fact.comparison.winPercentLossForMover >= JudgmentThresholds.MATERIAL_THREAT_WP
  val candidateBetter: Boolean =
    fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP
  val primaryPlayedPositive: Boolean =
    fact.kind == CandidateComparisonKind.PlayedVsBest && candidateBetter
  val playedCandidateSideComparison: Boolean =
    RelativeCauseDraftPlanner.playedMoveCandidateSideComparison(fact.kind)

  val referenceOnlyDefense: List[EvidenceRecord] =
    RelativeCauseSignalProfile.onlyDefenseRecords(referenceRecords)
  val candidateOnlyDefense: List[EvidenceRecord] =
    RelativeCauseSignalProfile.onlyDefenseRecords(candidateRecords)
  val referenceOnlyDefenseFunction: List[EvidenceRecord] =
    RelativeCauseSignalProfile.referenceOnlyDefenseFunctionRecords(fact, sharedRecords)
  val referenceTacticalRisk: List[EvidenceRecord] =
    RelativeCauseSignalProfile.tacticalRiskRecords(referenceRecords)
  val referenceConversionWindow: List[EvidenceRecord] =
    RelativeCauseSignalProfile.conversionWindowRecords(referenceRecords)
  val candidateConversionWindow: List[EvidenceRecord] =
    RelativeCauseSignalProfile.conversionWindowRecords(candidateRecords)
  val referenceRecaptureResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.recaptureResourceRecords(referenceRecords)
  val candidateRecaptureResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.recaptureResourceRecords(candidateRecords)
  val referenceMoveOrderResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.moveOrderResourceRecords(referenceRecords)
  val candidateMoveOrderResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.moveOrderResourceRecords(candidateRecords)
  val referenceForcingLineResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.forcingLineResourceRecords(referenceRecords)
  val candidateForcingLineResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.forcingLineResourceRecords(candidateRecords)
  val candidateWrongRecapturerChoice: List[EvidenceRecord] =
    RelativeCauseSignalProfile.wrongRecapturerChoiceRecords(fact, referenceRecords, candidateRecords)
  val referenceDefensiveResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.defensiveResourceRecords(referenceRecords)
  val candidateDefensiveResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.defensiveResourceRecords(candidateRecords)
  val sharedDefensiveResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.defensiveResourceRecords(sharedRecords)
  val referenceLooseMaterialExploit: List[EvidenceRecord] =
    RelativeCauseSignalProfile.looseMaterialExploitRecords(referenceRecords, sharedRecords)
  val candidateLooseMaterialLiability: List[EvidenceRecord] =
    RelativeCauseSignalProfile.looseMaterialLiabilityRecords(candidateRecords, sharedRecords)
  val referencePromotionResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.promotionRaceRecords(referenceRecords)
  val candidatePromotionResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.promotionRaceRecords(candidateRecords)
  val referencePassedPawnResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.passedPawnResourceRecords(referenceRecords)
  val candidatePassedPawnResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.passedPawnResourceRecords(candidateRecords)
  val candidatePassedPawnConcession: List[EvidenceRecord] =
    RelativeCauseSignalProfile.passedPawnConcessionRecords(candidateRecords)
  val referenceEndgameResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.endgameResourceRecords(referenceRecords)
  val candidateEndgameResource: List[EvidenceRecord] =
    RelativeCauseSignalProfile.endgameResourceRecords(candidateRecords)
  val referenceStructuralTargetRelease: List[EvidenceRecord] =
    RelativeCauseSignalProfile.structuralTargetReleaseRecords(referenceRecords)
  val candidateStructuralImprovement: List[EvidenceRecord] =
    RelativeCauseSignalProfile.structuralImprovementRecords(candidateRecords)
  val candidatePawnStructureImprovement: List[EvidenceRecord] =
    RelativeCauseSignalProfile.pawnStructureImprovementRecords(candidateRecords)
  val candidateTargetPressureGain: List[EvidenceRecord] =
    RelativeCauseSignalProfile.targetPressureGainRecords(candidateRecords)
  val candidateCenterControlGain: List[EvidenceRecord] =
    RelativeCauseSignalProfile.centerControlGainRecords(candidateRecords)
  val candidateKingSafetyConcession: List[EvidenceRecord] =
    RelativeCauseSignalProfile.kingSafetyConcessionRecords(candidateRecords)
  val candidatePawnWeaknessTarget: List[EvidenceRecord] =
    RelativeCauseSignalProfile.pawnWeaknessTargetRecords(candidateRecords)
  val candidateActivityLoss: List[EvidenceRecord] =
    RelativeCauseSignalProfile.activityLossRecords(candidateRecords)
  val candidatePlanCause: List[EvidenceRecord] =
    RelativeCauseSignalProfile.planCauseRecords(candidateRecords)
  val candidateStrategicConcession: List[EvidenceRecord] =
    RelativeCauseSignalProfile.strategicConcessionRecords(candidateRecords)
  val referenceTacticalMechanism: List[EvidenceRecord] =
    RelativeCauseSignalProfile.tacticalMechanismRecords(referenceRecords)
  val candidateTacticalMechanism: List[EvidenceRecord] =
    RelativeCauseSignalProfile.tacticalMechanismRecords(candidateRecords)
  val materialSwingSupport: List[EvidenceRecord] =
    RelativeCauseSignalProfile.materialSwingSupportRecords(referenceRecords, candidateRecords)
  val materialDeteriorationSupport: List[EvidenceRecord] =
    RelativeCauseSignalProfile.materialDeteriorationSupportRecords(referenceRecords, candidateRecords)
  val materialLossSupport: List[EvidenceRecord] =
    (materialSwingSupport ++ materialDeteriorationSupport).distinctBy(_.ref.id)
  val sacrificeCompensationSupport: List[EvidenceRecord] =
    RelativeCauseSignalProfile.sacrificeCompensationSupportRecords(candidateRecords)
  val structuralImprovementSupport: List[EvidenceRecord] =
    (
      referenceStructuralTargetRelease ++
        candidateStructuralImprovement ++
        candidatePawnStructureImprovement
    ).distinctBy(_.ref.id)
  val shortTermEvidenceCompetesWithStrategic: Boolean =
    RelativeCauseSignalProfile.candidateConcreteTacticalBridgeRecords(candidateRecords).nonEmpty ||
      referenceTacticalRisk.nonEmpty ||
      materialLossSupport.nonEmpty ||
      referenceConversionWindow.nonEmpty ||
      candidateConversionWindow.nonEmpty
  val missedStrategicSupport: List[EvidenceRecord] =
    RelativeCauseSignalProfile.missedStrategicImprovementSupport(referenceRecords, candidateRecords)

private[chessjudgment] object RelativeCauseDraftPlanner:
  def drafts(profile: RelativeCauseSignalProfile): List[RelativeCauseDraft] =
    suppressGenericCompanions(rawDrafts(profile))

  def producedCauseBindableRecords(profile: RelativeCauseSignalProfile): List[EvidenceRecord] =
    drafts(profile).flatMap(_.support).distinctBy(_.ref.id)

  def playedMoveCandidateSideComparison(kind: CandidateComparisonKind): Boolean =
    kind == CandidateComparisonKind.PlayedVsBest || kind == CandidateComparisonKind.PlayedVsAlternative

  private def rawDrafts(profile: RelativeCauseSignalProfile): List[RelativeCauseDraft] =
    import profile.*
    List(
      causeDraft(
        RelativeCauseKind.OnlyMoveNecessity,
        Nil,
        fact.comparison.candidateSet.exists(_.onlyMove) && badLoss,
        Some(RelativeCauseSourceSide.Reference)
      ),
      causeDraft(
        RelativeCauseKind.OnlyDefenseNecessity,
        referenceOnlyDefense,
        referenceOnlyDefense.nonEmpty && badLoss,
        Some(RelativeCauseSourceSide.Reference)
      ),
      causeDraft(
        RelativeCauseKind.OnlyDefenseNecessity,
        candidateOnlyDefense,
        candidateOnlyDefense.nonEmpty && candidateBetter,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(
        RelativeCauseKind.OnlyDefenseNecessity,
        referenceOnlyDefenseFunction,
        referenceOnlyDefenseFunction.nonEmpty && badLoss,
        Some(RelativeCauseSourceSide.Reference)
      ),
      causeDraft(
        RelativeCauseKind.DefensiveResource,
        referenceDefensiveResource,
        referenceDefensiveResource.nonEmpty && badLoss,
        Some(RelativeCauseSourceSide.Reference)
      ),
      causeDraft(
        RelativeCauseKind.DefensiveResource,
        candidateDefensiveResource,
        candidateDefensiveResource.nonEmpty && candidateBetter,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(
        RelativeCauseKind.DefensiveResource,
        sharedDefensiveResource,
        sharedDefensiveResource.nonEmpty && badLoss,
        Some(RelativeCauseSourceSide.Shared)
      ),
      causeDraft(
        RelativeCauseKind.MissedTacticalResource,
        referenceRecaptureResource,
        referenceRecaptureResource.nonEmpty && candidateRecaptureResource.isEmpty && badLoss
      ),
      causeDraft(
        RelativeCauseKind.WrongRecapturer,
        candidateWrongRecapturerChoice,
        candidateWrongRecapturerChoice.nonEmpty && badLoss
      ),
      causeDraft(
        RelativeCauseKind.RecaptureRecoveryWindow,
        candidateRecaptureResource,
        candidateRecaptureResource.nonEmpty && candidateBetter
      ),
      causeDraft(
        RelativeCauseKind.WrongMoveOrder,
        referenceMoveOrderResource,
        referenceMoveOrderResource.nonEmpty && candidateMoveOrderResource.isEmpty && badLoss
      ),
      causeDraft(
        RelativeCauseKind.TempoLoss,
        candidateMoveOrderResource,
        candidateMoveOrderResource.nonEmpty && (badLoss || candidateBetter)
      ),
      causeDraft(
        RelativeCauseKind.KingForcing,
        referenceForcingLineResource,
        referenceForcingLineResource.nonEmpty && badLoss,
        Some(RelativeCauseSourceSide.Reference)
      ),
      causeDraft(
        RelativeCauseKind.KingForcing,
        candidateForcingLineResource,
        candidateForcingLineResource.nonEmpty && candidateBetter,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(RelativeCauseKind.ConversionMiss, referenceConversionWindow, referenceConversionWindow.nonEmpty && badLoss),
      causeDraft(RelativeCauseKind.ConversionSecured, candidateConversionWindow, candidateConversionWindow.nonEmpty && candidateBetter),
      causeDraft(RelativeCauseKind.ConversionMiss, referencePromotionResource, referencePromotionResource.nonEmpty && badLoss),
      causeDraft(RelativeCauseKind.ConversionSecured, candidatePromotionResource, candidatePromotionResource.nonEmpty && candidateBetter),
      causeDraft(RelativeCauseKind.MissedTacticalResource, referenceLooseMaterialExploit, referenceLooseMaterialExploit.nonEmpty && badLoss),
      causeDraft(
        if playedCandidateSideComparison then RelativeCauseKind.TacticalRefutationOfPlayed
        else RelativeCauseKind.CandidateTacticalLiability,
        candidateLooseMaterialLiability,
        candidateLooseMaterialLiability.nonEmpty && badLoss
      ),
      causeDraft(
        RelativeCauseKind.StructuralImprovement,
        structuralImprovementSupport ++ candidatePassedPawnResource ++ candidateEndgameResource,
        candidateBetter &&
          (structuralImprovementSupport.nonEmpty || candidatePassedPawnResource.nonEmpty || candidateEndgameResource.nonEmpty)
      ),
      causeDraft(RelativeCauseKind.TargetPressureGain, candidateTargetPressureGain, primaryPlayedPositive && candidateTargetPressureGain.nonEmpty),
      causeDraft(RelativeCauseKind.CenterControlGain, candidateCenterControlGain, primaryPlayedPositive && candidateCenterControlGain.nonEmpty),
      causeDraft(
        RelativeCauseKind.KingSafetyConcession,
        candidateKingSafetyConcession,
        badLoss && candidateKingSafetyConcession.nonEmpty,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(
        RelativeCauseKind.PawnWeaknessTarget,
        candidatePawnWeaknessTarget,
        primaryPlayedPositive && candidatePawnWeaknessTarget.nonEmpty,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(
        RelativeCauseKind.ActivityLoss,
        candidateActivityLoss,
        badLoss && candidateActivityLoss.nonEmpty,
        Some(RelativeCauseSourceSide.Candidate)
      ),
      causeDraft(RelativeCauseKind.PlanImprovement, candidatePlanCause, candidatePlanCause.nonEmpty && candidateBetter),
      causeDraft(RelativeCauseKind.PlanContradiction, candidatePlanCause, candidatePlanCause.nonEmpty && badLoss),
      causeDraft(
        RelativeCauseKind.StrategicConcession,
        candidateStrategicConcession ++ candidatePassedPawnConcession,
        badLoss &&
          !shortTermEvidenceCompetesWithStrategic &&
          (candidateStrategicConcession.nonEmpty || candidatePassedPawnConcession.nonEmpty)
      ),
      causeDraft(
        RelativeCauseKind.MissedStrategicImprovement,
        missedStrategicSupport ++ referencePassedPawnResource ++ referenceEndgameResource,
        badLoss && (missedStrategicSupport.nonEmpty || referencePassedPawnResource.nonEmpty || referenceEndgameResource.nonEmpty)
      ),
      causeDraft(
        RelativeCauseKind.MaterialSwing,
        materialLossSupport,
        majorLoss &&
          materialLossSupport.nonEmpty
      ),
      causeDraft(
        RelativeCauseKind.SacrificeCompensation,
        sacrificeCompensationSupport,
        (primaryPlayedPositive || candidateBetter) &&
          sacrificeCompensationSupport.nonEmpty
      )
    ).flatten ++ mechanismDrafts(profile)

  private def causeDraft(
      kind: RelativeCauseKind,
      support: List[EvidenceRecord],
      condition: Boolean,
      sourceSide: Option[RelativeCauseSourceSide] = None
  ): Option[RelativeCauseDraft] =
    Option.when(condition)(RelativeCauseDraft(kind, support.distinctBy(_.ref.id), sourceSide))

  private def mechanismDrafts(profile: RelativeCauseSignalProfile): List[RelativeCauseDraft] =
    val actionableLoss =
      profile.fact.comparison.verdict match
        case MoveChoiceVerdict.Inaccuracy | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Blunder => true
        case _                                                                                   => false
    val referenceCauses =
      Option
        .when(actionableLoss && profile.tacticalLoss)(mechanismCauseKinds(profile.referenceTacticalMechanism, badLoss = false))
        .getOrElse(Nil)
    val candidateBadCauses =
      Option
        .when(actionableLoss && profile.tacticalLoss)(
          mechanismCauseKinds(
            profile.candidateTacticalMechanism,
            badLoss = true,
            playedCandidate = profile.playedCandidateSideComparison
          )
        )
        .getOrElse(Nil)
    val candidateBetterCauses =
      Option.when(profile.candidateBetter)(mechanismCauseKinds(profile.candidateTacticalMechanism, badLoss = false)).getOrElse(Nil)
    referenceCauses ++ candidateBadCauses ++ candidateBetterCauses

  private def mechanismCauseKinds(
      records: List[EvidenceRecord],
      badLoss: Boolean,
      playedCandidate: Boolean = false
  ): List[RelativeCauseDraft] =
    records.collect {
      case record @ EvidenceRecord(_, payload: TacticalMechanismEvidence, _) if payload.canAnchorTacticalIdea =>
        RelativeCauseDraft(
          TacticalMechanismKind.relativeCauseKind(payload.kind, badLoss, playedCandidate),
          List(record)
        )
    }

  private def suppressGenericCompanions(
      drafts: List[RelativeCauseDraft]
  ): List[RelativeCauseDraft] =
    val hasShortTermCause = drafts.exists(draft => shortTermCause(draft.kind))
    val hasNonConcessionCause = drafts.exists(draft => draft.kind != RelativeCauseKind.StrategicConcession)
    val hasSpecificMaterialCause = drafts.exists(draft => specificMaterialCause(draft.kind))
    val hasSpecificStructuralCause = drafts.exists(draft => specificStructuralCause(draft.kind))
    drafts.filterNot {
      case RelativeCauseDraft(RelativeCauseKind.StrategicConcession, _, _) =>
        hasShortTermCause || hasNonConcessionCause
      case RelativeCauseDraft(RelativeCauseKind.MissedStrategicImprovement, _, _) =>
        hasShortTermCause
      case RelativeCauseDraft(RelativeCauseKind.MaterialSwing, _, _) =>
        hasSpecificMaterialCause
      case RelativeCauseDraft(RelativeCauseKind.StructuralImprovement, _, _) =>
        hasSpecificStructuralCause
      case draft @ RelativeCauseDraft(RelativeCauseKind.RecaptureRecoveryWindow, _, _) =>
        drafts.exists(other =>
          other.kind == RelativeCauseKind.WrongRecapturer &&
            supportOverlaps(draft.support, other.support)
        )
      case draft @ RelativeCauseDraft(RelativeCauseKind.TempoLoss, _, _) =>
        drafts.exists(other =>
          other.kind == RelativeCauseKind.WrongMoveOrder &&
            supportOverlaps(draft.support, other.support)
        )
      case _ =>
        false
    }

  private def supportOverlaps(left: List[EvidenceRecord], right: List[EvidenceRecord]): Boolean =
    val leftIds = left.map(_.ref.id).toSet
    leftIds.nonEmpty && right.exists(record => leftIds.contains(record.ref.id))

  private def specificMaterialCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.WrongRecapturer | RelativeCauseKind.RecaptureRecoveryWindow |
          RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured |
          RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.TacticalRefutationOfPlayed |
          RelativeCauseKind.CandidateTacticalLiability =>
        true
      case _ =>
        false

  private def specificStructuralCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.TargetPressureGain | RelativeCauseKind.CenterControlGain |
          RelativeCauseKind.KingSafetyConcession | RelativeCauseKind.PawnWeaknessTarget |
          RelativeCauseKind.ActivityLoss |
          RelativeCauseKind.PlanImprovement | RelativeCauseKind.PlanContradiction |
          RelativeCauseKind.MissedStrategicImprovement | RelativeCauseKind.StrategicConcession |
          RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        true
      case _ =>
        false

  private def shortTermCause(kind: RelativeCauseKind): Boolean =
    kind != RelativeCauseKind.DrawResource && ClaimEventCluster.kindForCause(kind).nonEmpty

private[chessjudgment] object RelativeCauseSignalProfile:
  def from(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): RelativeCauseSignalProfile =
    RelativeCauseSignalProfile(
      fact = fact,
      referenceRecords = referenceRecords,
      candidateRecords = candidateRecords,
      sharedRecords = sharedRecords
    )

  private[chessjudgment] def tacticalMechanismRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea || payload.canAnchorDefensiveIdea
      case _ =>
        false
    }

  private[chessjudgment] def tacticalRiskRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea
      case _ =>
        false
    }

  private[chessjudgment] def onlyDefenseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        payload.onlyDefense.nonEmpty && payload.isProofSignalDefensivePressure
      case _ => false
    }

  private[chessjudgment] def defensiveResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        !payload.insufficientData &&
          (payload.defenseRequired ||
            payload.prophylaxisNeeded ||
            payload.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.SIGNIFICANT_THREAT_WP))
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorDefensiveIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def recaptureResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasRecaptureRecoveryConsequence ||
          payload.hasMaterialRecaptureChain ||
          payload.hasMaterialRecoveryWindow
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.RecaptureChoice && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def moveOrderResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: MoveMotifEvidence, _) =>
        TacticalMotifClassifier.isRootCauseEligible(payload) && (payload.motif match
          case _: Motif.Zwischenzug => true
          case _                    => false
        )
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        payload.kind == RelationFactKind.Zwischenzug && payload.hasConcreteRelationProof
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence(LineConsequenceKind.ImmediateReplyCheck)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.Tempo && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def forcingLineResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence(LineConsequenceKind.ForcedTheme) ||
          payload.hasLineEvent(LineEventKind.Mate) ||
          payload.hasProofSignalConsequence(LineConsequenceKind.Mate)
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.KingForcing && payload.hasConcreteProof
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def wrongRecapturerChoiceRecords(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val referenceMove = normalizeMove(fact.referenceLine.rootMove)
    val candidateMove = normalizeMove(fact.candidateLine.rootMove)
    val referenceRecovery = recaptureResourceRecords(referenceRecords)
    val candidateLoss = materialLossRecords(candidateRecords)
    Option
      .when(
        EvidenceRef.sameDestinationDifferentOrigin(referenceMove, candidateMove) &&
          referenceRecovery.nonEmpty &&
          candidateLoss.nonEmpty &&
          EvidenceRecord.hasRootCaptureEvent(referenceRecords, referenceMove) &&
          EvidenceRecord.hasRootCaptureEvent(candidateRecords, candidateMove)
      )(
        EvidenceRecord.rootCaptureRecords(referenceRecords, referenceMove) ++
          EvidenceRecord.rootCaptureRecords(candidateRecords, candidateMove) ++
          referenceRecovery ++
          candidateLoss ++
          recaptureResourceRecords(candidateRecords)
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private[chessjudgment] def conversionWindowRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, SinglePositionEvidence(assessment), _) =>
        assessment.simplifyBias.shouldSimplify
      case EvidenceRecord(_, payload: RelationFactEvidence, _) if payload.kind == RelationFactKind.BadPieceLiquidation =>
        true
      case _ => false
    }

  private[chessjudgment] def promotionRaceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence(LineConsequenceKind.PromotionRace) ||
          payload.materialOutcomeProfile.gainSignals.contains(LineMaterialOutcomeSignal.PromotionGain) ||
          payload.materialOutcomeProfile.lossSignals.contains(LineMaterialOutcomeSignal.PromotionLoss)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.PawnPromotion && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def looseMaterialExploitRecords(
      records: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val material = materialGainRecords(records)
    val relation = looseMaterialRelationRecords(records)
    Option
      .when((material.nonEmpty || relation.nonEmpty) && looseMaterialContextPresent(records ++ sharedRecords))(
        material ++ relation
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private[chessjudgment] def looseMaterialLiabilityRecords(
      records: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val material = materialLossRecords(records)
    val relation = looseMaterialRelationRecords(records)
    Option
      .when((material.nonEmpty || relation.nonEmpty) && looseMaterialContextPresent(records ++ sharedRecords))(
        material ++ relation
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def looseMaterialContextPresent(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
        payload.looseMaterialAnchors.nonEmpty
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        looseMaterialRelation(payload)
      case _ =>
        false
    }

  private def looseMaterialRelationRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        looseMaterialRelation(payload)
      case _ =>
        false
    }

  private def looseMaterialRelation(payload: RelationFactEvidence): Boolean =
    payload.hasConcreteRelationProof &&
      (
        payload.kind == RelationFactKind.HangingPiece ||
          payload.kind == RelationFactKind.TrappedPiece ||
          payload.kind == RelationFactKind.Domination
      )

  private def materialGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.materialOutcomeProfile.gainMagnitude != LineMaterialOutcomeMagnitude.None ||
          payload.hasProofSignalConsequence(LineConsequenceKind.MaterialGain)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.MaterialGain && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def materialLossRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.materialOutcomeProfile.lossMagnitude != LineMaterialOutcomeMagnitude.None ||
          payload.hasProofSignalConsequence(LineConsequenceKind.MaterialLoss)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.MaterialGain && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def passedPawnResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StrategicMechanismEvidence, _) =>
        payload.kind == StrategicMechanismKind.PawnStructure &&
          payload.canAnchorPawnStructureIdea &&
          payload.signals.exists(signal =>
            signal.label == "passed-pawn-progress" ||
              signal.label == "promotion-pressure-gain"
          )
      case EvidenceRecord(ref, payload: MoveMotifEvidence, _) if payload.recordLineBound(ref) =>
        payload.motif match
          case _: Motif.PassedPawnPush | _: Motif.PassedPawn | _: Motif.PawnPromotion => true
          case _                                                                      => false
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def passedPawnConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StrategicMechanismEvidence, _) =>
        payload.kind == StrategicMechanismKind.StrategicConcession &&
          payload.canSupportStrategicCause &&
          payload.signals.exists(signal =>
            signal.label == "passed-pawn-concession" ||
              signal.label == "promotion-pressure-concession"
          )
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def endgameResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.Endgame && payload.canSupportStrategicCause
    )

  private[chessjudgment] def structuralTargetReleaseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.TargetPressure &&
        payload.signals.exists(_.label == "target-pressure-release")
    )

  private def strategicMechanismRecords(
      records: List[EvidenceRecord]
  )(mechanismPredicate: StrategicMechanismEvidence => Boolean): List[EvidenceRecord] =
    records.collect {
      case record @ EvidenceRecord(_, payload: StrategicMechanismEvidence, _) if mechanismPredicate(payload) =>
        record
    }.distinctBy(_.ref.id)

  private[chessjudgment] def structuralImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.StructuralImprovement && payload.canSupportStrategicCause
    )

  private[chessjudgment] def targetPressureGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.TargetPressure &&
        payload.canSupportStrategicCause &&
        payload.signals.exists(_.label == "target-pressure-gain")
    )

  private[chessjudgment] def centerControlGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.CenterControl && payload.canSupportStrategicCause
    )

  private[chessjudgment] def kingSafetyConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.KingSafety && payload.canSupportStrategicCause
    )

  private[chessjudgment] def pawnWeaknessTargetRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.PawnWeakness && payload.canSupportStrategicCause
    )

  private[chessjudgment] def activityLossRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.Activity &&
        payload.canSupportStrategicCause &&
        payload.signals.exists(_.label == "activity-loss")
    )

  private[chessjudgment] def pawnStructureImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(_.canAnchorPawnStructureIdea)

  private[chessjudgment] def planCauseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(_.canAnchorPlanIdea)

  private[chessjudgment] def strategicConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.kind == StrategicMechanismKind.StrategicConcession && payload.canSupportStrategicCause
    )

  private def strategicImprovementSupport(records: List[EvidenceRecord]): List[EvidenceRecord] =
    (
      structuralImprovementRecords(records) ++
        pawnStructureImprovementRecords(records) ++
        planCauseRecords(records)
    ).distinctBy(_.ref.id)

  private[chessjudgment] def missedStrategicImprovementSupport(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val scoreBased =
      Option
        .when(referenceStrategicImprovementOutperformsCandidate(referenceRecords, candidateRecords))(
          strategicImprovementSupport(referenceRecords)
        )
        .getOrElse(Nil)
    val axisBased =
      List(
        unmatchedAxisSupport(strategicImprovementSupport(referenceRecords), strategicImprovementSupport(candidateRecords)),
        unmatchedAxisSupport(pawnStructureImprovementRecords(referenceRecords), pawnStructureImprovementRecords(candidateRecords)),
        unmatchedAxisSupport(planCauseRecords(referenceRecords), planCauseRecords(candidateRecords))
      ).flatten
    (scoreBased ++ axisBased).distinctBy(_.ref.id)

  private def unmatchedAxisSupport(
      referenceSupport: List[EvidenceRecord],
      candidateSupport: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option.when(referenceSupport.nonEmpty && candidateSupport.isEmpty)(referenceSupport).getOrElse(Nil)

  private[chessjudgment] def referenceOnlyDefenseFunctionRecords(
      fact: CandidateComparisonFact,
      records: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        !payload.insufficientData &&
          payload.onlyDefense.exists(move => normalizeMove(move) == normalizeMove(fact.referenceLine.rootMove)) &&
          (payload.defenseRequired ||
            payload.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP))
      case _ =>
        false
    }

  private[chessjudgment] def candidateConcreteTacticalBridgeRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private[chessjudgment] def materialSwingSupportRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option
      .when(typedMaterialConsequenceSwing(referenceRecords, candidateRecords))(
        (proofSignalMaterialSummaryRecords(referenceRecords) ++ proofSignalMaterialSummaryRecords(candidateRecords)).distinctBy(_.ref.id)
      )
      .getOrElse(Nil)

  private[chessjudgment] def materialDeteriorationSupportRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option
      .when(materialDeteriorates(referenceRecords, candidateRecords))(
        proofSignalMaterialSummaryRecords(referenceRecords) ++ proofSignalMaterialSummaryRecords(candidateRecords)
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private[chessjudgment] def sacrificeCompensationSupportRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    val material = sacrificeMaterialRecords(records)
    val compensation = compensationSupportRecords(records)
    Option
      .when(material.nonEmpty && compensation.nonEmpty)((material ++ compensation).distinctBy(_.ref.id))
      .getOrElse(Nil)

  private def sacrificeMaterialRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasSacrificeConsequence
      case _ =>
        false
    }

  private def compensationSupportRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    (
      strategicCompensationRecords(records) ++
        strategicImprovementSupport(records) ++
        compensationAnchorRecords(records)
    ).distinctBy(_.ref.id)

  private def strategicCompensationRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(_.canSupportCompensation)

  private def compensationAnchorRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    strategicMechanismRecords(records)(payload =>
      payload.canSupportCompensation || payload.canAnchorPlanIdea || payload.canAnchorPawnStructureIdea
    )

  private def proofSignalMaterialSummaryRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasMaterialConsequence || payload.hasRecaptureRecoveryConsequence
      case _ =>
        false
    }

  private[chessjudgment] def referenceStrategicImprovementOutperformsCandidate(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    val referenceScore = strategicImprovementScore(referenceRecords)
    val candidateScore = strategicImprovementScore(candidateRecords)
    (referenceScore >= 3 && referenceScore >= candidateScore + 2) ||
      (referenceScore >= 2 && candidateScore <= 1)

  private[chessjudgment] def strategicImprovementScore(records: List[EvidenceRecord]): Int =
    records.collect { case EvidenceRecord(_, payload: StrategicMechanismEvidence, _) =>
      payload.directStrength
    }.sum

  private def typedMaterialConsequenceSwing(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    materialGainMagnitude(referenceRecords).ordinal > materialGainMagnitude(candidateRecords).ordinal ||
      materialLossMagnitude(candidateRecords).ordinal > materialLossMagnitude(referenceRecords).ordinal

  private def materialDeteriorates(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    typedMaterialConsequenceSwing(referenceRecords, candidateRecords)

  private def materialGainMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    LineFactEvidence.materialOutcomeProfile(records).gainMagnitude

  private def materialLossMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    LineFactEvidence.materialOutcomeProfile(records).lossMagnitude

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
