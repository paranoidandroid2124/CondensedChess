package lila.chessjudgment.model.judgment

object MoveJudgmentCauseNarrativeProjection:
  def defaultNarrativeRole(role: MoveJudgmentCauseFrameRole): MoveJudgmentCauseNarrativeRole =
    role match
      case MoveJudgmentCauseFrameRole.PrimaryCause   => MoveJudgmentCauseNarrativeRole.RootCause
      case MoveJudgmentCauseFrameRole.SecondaryCause => MoveJudgmentCauseNarrativeRole.SupportingCause
      case MoveJudgmentCauseFrameRole.ContextCause   => MoveJudgmentCauseNarrativeRole.ContextCause

  def withNarrativeRoles(frames: List[MoveJudgmentCauseFrame]): List[MoveJudgmentCauseFrame] =
    val selectedRootsByComparison =
      selectedRootFramesByComparison(frames)
    val selectedRootIds =
      selectedRootsByComparison.values.flatten.map(causeFrameIdentity).toSet
    val selectedLongTermRootsByComparison =
      selectedRootsByComparison.view
        .mapValues(_.filter(longTermRootFrame))
        .toMap
    frames.map { frame =>
      val arbitrationTier = rootArbitrationProfile(frame).tier
      val frameWithTier = frame.copy(rootArbitrationTier = arbitrationTier)
      val matchingLongTermRoots = selectedLongTermRootsByComparison.getOrElse(comparisonFrameKey(frame), Nil)
      if selectedRootIds.contains(causeFrameIdentity(frame)) then
        if longTermRootFrame(frame) then
          val witnesses =
            frames
              .filter(candidate => sameComparison(candidate, frame) && tacticalWitnessNarrativeFrame(candidate))
              .filterNot(candidate => causeFrameIdentity(candidate) == causeFrameIdentity(frame))
          val witnessBindings = witnesses.map(witness => witness -> witnessBinding(frame, witness))
          val punishmentWitnesses =
            witnessBindings.collect { case (witness, binding) if binding.level == MoveJudgmentCauseWitnessBindingLevel.Punishment =>
              witness
            }
          val contextualWitnesses =
            witnessBindings.collect {
              case (witness, binding)
                  if binding.level != MoveJudgmentCauseWitnessBindingLevel.Punishment &&
                    binding.level != MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly =>
                witness
            }
          val boundWitnesses = (punishmentWitnesses ++ contextualWitnesses).distinctBy(causeFrameIdentity)
          frameWithTier.copy(
            narrativeRole = MoveJudgmentCauseNarrativeRole.RootCause,
            tacticalWitnessCauseEvidenceIds = boundWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
            tacticalWitnessCauseKinds = boundWitnesses.map(_.causeKind).distinct,
            punishmentWitnessCauseEvidenceIds = punishmentWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
            punishmentWitnessCauseKinds = punishmentWitnesses.map(_.causeKind).distinct,
            contextualTacticalWitnessCauseEvidenceIds = contextualWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
            contextualTacticalWitnessCauseKinds = contextualWitnesses.map(_.causeKind).distinct
          )
        else frameWithTier.copy(narrativeRole = MoveJudgmentCauseNarrativeRole.RootCause)
      else if tacticalWitnessNarrativeFrame(frame) && matchingLongTermRoots.nonEmpty then
        val binding = strongestWitnessBinding(frame, matchingLongTermRoots)
        if binding.level == MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly then
          frameWithTier.copy(
            narrativeRole = MoveJudgmentCauseNarrativeRole.ContextCause,
            witnessBindingLevel = binding.level,
            witnessBindingSignals = binding.signals,
            witnessBindingRootCauseEvidenceIds = binding.rootCauseEvidenceIds
          )
        else
          frameWithTier.copy(
            narrativeRole = MoveJudgmentCauseNarrativeRole.TacticalWitness,
            witnessBindingLevel = binding.level,
            witnessBindingSignals = binding.signals,
            witnessBindingRootCauseEvidenceIds = binding.rootCauseEvidenceIds
          )
      else if arbitrationManagedPrimaryFrame(frame) &&
          (selectedRootsByComparison.contains(comparisonFrameKey(frame)) ||
            arbitrationTier == MoveJudgmentCauseRootArbitrationTier.ContextOnly ||
            (arbitrationTier == MoveJudgmentCauseRootArbitrationTier.BroadOwnedRoot &&
              broadObjectSensitiveRootKind(frame.causeKind)))
      then
        frameWithTier.copy(narrativeRole = MoveJudgmentCauseNarrativeRole.SupportingCause)
      else
        frameWithTier.copy(narrativeRole = defaultNarrativeRole(frame.role))
    }

  private final case class RootArbitrationProfile(
      tier: MoveJudgmentCauseRootArbitrationTier,
      fallbackKind: Boolean,
      eventKind: Boolean
  )

  private def selectedRootFramesByComparison(
      frames: List[MoveJudgmentCauseFrame]
  ): Map[(CandidateComparisonKind, LineNodeRef, LineNodeRef), List[MoveJudgmentCauseFrame]] =
    frames
      .filter(arbitrationManagedPrimaryFrame)
      .groupBy(comparisonFrameKey)
      .view
      .mapValues(selectedRootFrames)
      .filter { case (_, selected) => selected.nonEmpty }
      .toMap

  private def selectedRootFrames(frames: List[MoveJudgmentCauseFrame]): List[MoveJudgmentCauseFrame] =
    val profiled = frames.map(frame => frame -> rootArbitrationProfile(frame))
    val qualifiedLongTermRoots =
      profiled.filter { case (frame, profile) =>
        longTermRootFrame(frame) &&
          !profile.fallbackKind &&
          (
            profile.tier == MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot ||
              profile.tier == MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot ||
              (profile.tier == MoveJudgmentCauseRootArbitrationTier.BroadOwnedRoot && !broadObjectSensitiveRootKind(frame.causeKind))
          )
      }
    val eventRoots = profiled.filter { case (_, profile) =>
      profile.eventKind &&
        (profile.tier == MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot || profile.tier == MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot)
    }
    val fallbackRoots = profiled.filter { case (_, profile) => profile.tier == MoveJudgmentCauseRootArbitrationTier.FallbackRoot }
    val selected =
      if qualifiedLongTermRoots.nonEmpty then qualifiedLongTermRoots.map(_._1)
      else if eventRoots.nonEmpty then selectedEventRootFrames(eventRoots.map(_._1))
      else selectedFallbackRootFrames(fallbackRoots.map(_._1))
    selected.distinctBy(causeFrameIdentity)

  private def selectedEventRootFrames(frames: List[MoveJudgmentCauseFrame]): List[MoveJudgmentCauseFrame] =
    frames.sortBy(eventRootSortKey).lastOption.toList

  private def selectedFallbackRootFrames(frames: List[MoveJudgmentCauseFrame]): List[MoveJudgmentCauseFrame] =
    frames.sortBy(fallbackRootSortKey).lastOption.toList

  private def eventRootSortKey(frame: MoveJudgmentCauseFrame): (Int, Int, Int, Int, Int, Int, Int, String) =
    (
      eventRootKindRank(frame),
      boolRank(frame.concreteObjectReady || directProofObjectReady(frame)),
      boolRank(frame.attributionRootMoveMatched),
      boolRank(frame.attributionDirectProofEligible),
      frame.proofDirectSourceIds.distinct.size,
      frame.objectBindingSignatures.distinct.size,
      sourceSideRank(frame.causeSourceSide),
      frame.causeKind.toString
    )

  private def fallbackRootSortKey(frame: MoveJudgmentCauseFrame): (Int, Int, Int, Int, Int, String) =
    (
      fallbackRootKindRank(frame.causeKind),
      boolRank(frame.concreteObjectReady || directProofObjectReady(frame)),
      frame.proofDirectSourceIds.distinct.size,
      frame.objectBindingSignatures.distinct.size,
      sourceSideRank(frame.causeSourceSide),
      frame.causeKind.toString
    )

  private def eventRootKindRank(frame: MoveJudgmentCauseFrame): Int =
    frame.causeKind match
      case RelativeCauseKind.WrongRecapturer if eventRootCaptureBound(frame) =>
        110
      case RelativeCauseKind.WrongMoveOrder | RelativeCauseKind.TempoLoss =>
        100
      case RelativeCauseKind.TacticalRefutationOfPlayed | RelativeCauseKind.CandidateTacticalLiability =>
        95
      case RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.KingForcing =>
        90
      case RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        70
      case RelativeCauseKind.RecaptureRecoveryWindow | RelativeCauseKind.MaterialSwing =>
        65
      case RelativeCauseKind.OnlyMoveNecessity | RelativeCauseKind.OnlyDefenseNecessity =>
        55
      case RelativeCauseKind.DefensiveResource | RelativeCauseKind.DrawResource =>
        45
      case RelativeCauseKind.SacrificeCompensation =>
        40
      case _ =>
        0

  private def fallbackRootKindRank(kind: RelativeCauseKind): Int =
    kind match
      case RelativeCauseKind.PlanContradiction => 2
      case RelativeCauseKind.PlanImprovement   => 1
      case _                                   => 0

  private def sourceSideRank(side: RelativeCauseSourceSide): Int =
    side match
      case RelativeCauseSourceSide.Candidate | RelativeCauseSourceSide.Reference => 1
      case RelativeCauseSourceSide.Mixed | RelativeCauseSourceSide.Shared        => 0

  private def eventRootSelectable(frame: MoveJudgmentCauseFrame): Boolean =
    frame.causeKind != RelativeCauseKind.WrongRecapturer || eventRootCaptureBound(frame)

  private def eventRootCaptureBound(frame: MoveJudgmentCauseFrame): Boolean =
    val moveToken = s"actor=Move:${frame.eventRootMove}"
    frame.objectBindingSignatures.exists(signature =>
      signature.contains(moveToken) &&
        (
          signature.contains("mechanism=Mechanism:capture") ||
            signature.contains("mechanism=Motif:capture") ||
            signature.contains("consequence=Consequence:capture")
        )
    )

  private def boolRank(value: Boolean): Int =
    if value then 1 else 0

  private def rootArbitrationProfile(frame: MoveJudgmentCauseFrame): RootArbitrationProfile =
    val lineOwned = frame.attributionDirectProofEligible && frame.attributionRootMoveMatched
    val fallbackKind = fallbackRootKind(frame.causeKind)
    val typedObjectReady =
      if broadObjectSensitiveRootKind(frame.causeKind) then directProofSpecificObjectReady(frame)
      else frame.concreteObjectReady || directProofObjectReady(frame)
    val eventKind = tacticalWitnessFrame(frame) && eventRootSelectable(frame)
    val tier =
      if !lineOwned then MoveJudgmentCauseRootArbitrationTier.ContextOnly
      else if fallbackKind && fallbackRootProofReady(frame) then MoveJudgmentCauseRootArbitrationTier.FallbackRoot
      else if fallbackKind then MoveJudgmentCauseRootArbitrationTier.ContextOnly
      else if longTermRootFrame(frame) && typedObjectReady then MoveJudgmentCauseRootArbitrationTier.ExactOwnedRoot
      else if eventKind then MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot
      else if typedObjectReady then MoveJudgmentCauseRootArbitrationTier.ConcreteOwnedRoot
      else if longTermRootFrame(frame) then MoveJudgmentCauseRootArbitrationTier.BroadOwnedRoot
      else MoveJudgmentCauseRootArbitrationTier.ContextOnly
    RootArbitrationProfile(
      tier = tier,
      fallbackKind = fallbackKind,
      eventKind = eventKind
    )

  private def directProofObjectReady(frame: MoveJudgmentCauseFrame): Boolean =
    val directSignatures = objectSignatures(frame, Some(RelativeCauseProofRole.DirectProof))
    directSignatures.exists(signature =>
      signature.contains("target=") &&
        signature.contains("mechanism=") &&
        (signature.contains("consequence=") || signature.contains("witness="))
    )

  private def directProofSpecificObjectReady(frame: MoveJudgmentCauseFrame): Boolean =
    val directSignatures = objectSignatures(frame, Some(RelativeCauseProofRole.DirectProof))
    directSignatures.exists(specificRootObjectSignature)

  private def specificRootObjectSignature(signature: String): Boolean =
    val parts = signature.split("\\|").toList
    parts.exists(specificRootTargetPart) &&
      parts.exists(_.startsWith("mechanism=")) &&
      parts.exists(part => part.startsWith("consequence=") || part.startsWith("witness="))

  private def specificRootTargetPart(part: String): Boolean =
    val normalized = part.trim.toLowerCase
    normalized.startsWith("target=square:") ||
      normalized.startsWith("target=file:") ||
      normalized.startsWith("target=pawn:") ||
      normalized.startsWith("target=piece:") ||
      (
        normalized.startsWith("target=plansubject:") && {
          val subject = normalized.stripPrefix("target=plansubject:")
          subject.contains(":") || subject.matches(".*[a-h][1-8].*")
        }
      )

  private def arbitrationManagedPrimaryFrame(frame: MoveJudgmentCauseFrame): Boolean =
    frame.role == MoveJudgmentCauseFrameRole.PrimaryCause &&
      frame.comparisonKind == CandidateComparisonKind.PlayedVsBest &&
      frame.causeRole == RelativeCauseRole.PrimaryPlayedCause &&
      frame.causeImportance == RelativeCauseImportance.Primary

  private def fallbackRootKind(kind: RelativeCauseKind): Boolean =
    kind == RelativeCauseKind.PlanContradiction ||
      kind == RelativeCauseKind.PlanImprovement

  private def fallbackRootProofReady(frame: MoveJudgmentCauseFrame): Boolean =
    frame.causeKind match
      case RelativeCauseKind.PlanContradiction | RelativeCauseKind.PlanImprovement =>
        directPlanCoherenceProofReady(frame)
      case _ =>
        true

  private def directPlanCoherenceProofReady(frame: MoveJudgmentCauseFrame): Boolean =
    val directSourceIds = frame.proofDirectSourceIds.toSet
    frame.proofStrategicAxisLineage.exists(lineage =>
      lineage.axisKind == StrategicAxisKind.PlanCoherence &&
        directSourceIds.contains(lineage.mechanismEvidenceId)
    )

  private def broadObjectSensitiveRootKind(kind: RelativeCauseKind): Boolean =
    kind == RelativeCauseKind.ActivityGain ||
      kind == RelativeCauseKind.ActivityLoss ||
      kind == RelativeCauseKind.TargetPressureGain ||
      kind == RelativeCauseKind.TargetPressureRelease ||
      kind == RelativeCauseKind.PawnBreakOpportunity ||
      kind == RelativeCauseKind.PawnWeaknessTarget ||
      kind == RelativeCauseKind.OpponentRestriction ||
      kind == RelativeCauseKind.KingSafetyConcession ||
      fallbackRootKind(kind)

  private def causeFrameIdentity(frame: MoveJudgmentCauseFrame): (Option[String], List[String], RelativeCauseKind, CandidateComparisonKind, RelativeCauseRole, RelativeCauseSourceSide, RelativeCauseImportance, LineNodeRef) =
    (
      frame.clusterId,
      frame.causeEvidenceIds.distinct.sorted,
      frame.causeKind,
      frame.comparisonKind,
      frame.causeRole,
      frame.causeSourceSide,
      frame.causeImportance,
      frame.eventLine
    )

  private final case class TacticalWitnessBinding(
      level: MoveJudgmentCauseWitnessBindingLevel,
      signals: List[MoveJudgmentCauseWitnessBindingSignal],
      rootCauseEvidenceIds: List[String]
  )

  private def strongestWitnessBinding(
      witness: MoveJudgmentCauseFrame,
      roots: List[MoveJudgmentCauseFrame]
  ): TacticalWitnessBinding =
    roots
      .filter(root => sameComparison(root, witness))
      .map(root => witnessBinding(root, witness))
      .sortBy(binding => witnessBindingRank(binding.level))
      .lastOption
      .getOrElse(
        TacticalWitnessBinding(
          MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly,
          List(MoveJudgmentCauseWitnessBindingSignal.SameComparison),
          Nil
        )
      )

  private def witnessBinding(
      root: MoveJudgmentCauseFrame,
      witness: MoveJudgmentCauseFrame
  ): TacticalWitnessBinding =
    val rootSignatures = root.objectBindingSignatures.toSet
    val witnessSignatures = witness.objectBindingSignatures.toSet
    val rootDirectSignatures = objectSignatures(root, Some(RelativeCauseProofRole.DirectProof))
    val witnessDirectSignatures = objectSignatures(witness, Some(RelativeCauseProofRole.DirectProof))
    val sharedExact = rootSignatures.intersect(witnessSignatures).nonEmpty
    val sharedActor = sharedObjectToken(root, witness, "actor")
    val sharedTarget = sharedObjectToken(root, witness, "target")
    val sharedMechanism = sharedObjectToken(root, witness, "mechanism")
    val sharedConsequence = sharedObjectToken(root, witness, "consequence")
    val sharedWitness = sharedObjectToken(root, witness, "witness")
    val sharedDirectExact = rootDirectSignatures.intersect(witnessDirectSignatures).nonEmpty
    val sharedDirectActor = sharedObjectToken(root, witness, "actor", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectTarget = sharedObjectToken(root, witness, "target", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectMechanism = sharedObjectToken(root, witness, "mechanism", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectConsequence = sharedObjectToken(root, witness, "consequence", Some(RelativeCauseProofRole.DirectProof))
    val sameEvent = root.eventLine == witness.eventLine
    val directProofOverlap = root.proofDirectSourceIds.toSet.intersect(witness.proofDirectSourceIds.toSet).nonEmpty
    val signals =
      List(
        Some(MoveJudgmentCauseWitnessBindingSignal.SameComparison),
        Option.when(sharedExact)(MoveJudgmentCauseWitnessBindingSignal.SharedExactObjectSignature),
        Option.when(sharedActor)(MoveJudgmentCauseWitnessBindingSignal.SharedActor),
        Option.when(sharedTarget)(MoveJudgmentCauseWitnessBindingSignal.SharedTarget),
        Option.when(sharedMechanism)(MoveJudgmentCauseWitnessBindingSignal.SharedMechanism),
        Option.when(sharedConsequence)(MoveJudgmentCauseWitnessBindingSignal.SharedConsequence),
        Option.when(sharedWitness)(MoveJudgmentCauseWitnessBindingSignal.SharedWitness),
        Option.when(sharedDirectExact)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectObjectSignature),
        Option.when(sharedDirectActor)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectActor),
        Option.when(sharedDirectTarget)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectTarget),
        Option.when(sharedDirectMechanism)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectMechanism),
        Option.when(sharedDirectConsequence)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectConsequence),
        Option.when(sameEvent)(MoveJudgmentCauseWitnessBindingSignal.SameEventLine),
        Option.when(directProofOverlap)(MoveJudgmentCauseWitnessBindingSignal.DirectProofSourceOverlap)
      ).flatten.distinct.sortBy(_.toString)
    TacticalWitnessBinding(
      level = witnessBindingLevel(
        objectBound = sharedExact || sharedActor || sharedTarget,
        directObjectBound = sharedDirectExact || sharedDirectActor || sharedDirectTarget,
        mechanismBound = sharedMechanism,
        consequenceBound = sharedConsequence,
        directMechanismBound = sharedDirectMechanism,
        directConsequenceBound = sharedDirectConsequence,
        lineOrEvalBound = sharedWitness || sameEvent || directProofOverlap,
        eventOrEvalBound = sameEvent || directProofOverlap,
        ownedTacticalProof = witness.hasOwnedTacticalProof
      ),
      signals = signals,
      rootCauseEvidenceIds = root.causeEvidenceIds.distinct.sorted
    )

  private def witnessBindingLevel(
      objectBound: Boolean,
      directObjectBound: Boolean,
      mechanismBound: Boolean,
      consequenceBound: Boolean,
      directMechanismBound: Boolean,
      directConsequenceBound: Boolean,
      lineOrEvalBound: Boolean,
      eventOrEvalBound: Boolean,
      ownedTacticalProof: Boolean
  ): MoveJudgmentCauseWitnessBindingLevel =
    if ownedTacticalProof && directObjectBound && directMechanismBound && directConsequenceBound && eventOrEvalBound then
      MoveJudgmentCauseWitnessBindingLevel.Punishment
    else if objectBound && (mechanismBound || consequenceBound) then
      MoveJudgmentCauseWitnessBindingLevel.ObjectContext
    else if lineOrEvalBound then MoveJudgmentCauseWitnessBindingLevel.LineContext
    else MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly

  private def sharedObjectToken(
      left: MoveJudgmentCauseFrame,
      right: MoveJudgmentCauseFrame,
      role: String,
      proofRole: Option[RelativeCauseProofRole] = None
  ): Boolean =
    objectTokens(left, role, proofRole).intersect(objectTokens(right, role, proofRole)).nonEmpty

  private def objectTokens(
      frame: MoveJudgmentCauseFrame,
      role: String,
      proofRole: Option[RelativeCauseProofRole]
  ): Set[String] =
    val prefix = s"$role="
    objectSignatures(frame, proofRole).flatMap { signature =>
      signature
        .split("\\|")
        .toList
        .collect { case part if part.startsWith(prefix) => part.stripPrefix(prefix) }
    }.toSet

  private def objectSignatures(
      frame: MoveJudgmentCauseFrame,
      proofRole: Option[RelativeCauseProofRole]
  ): Set[String] =
    val proofPart = proofRole.map(role => s"proof=$role")
    frame.objectBindingSignatures.filter(signature => proofPart.forall(signature.contains)).toSet

  private def witnessBindingRank(level: MoveJudgmentCauseWitnessBindingLevel): Int =
    level match
      case MoveJudgmentCauseWitnessBindingLevel.NotWitness          => 0
      case MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly => 1
      case MoveJudgmentCauseWitnessBindingLevel.LineContext        => 2
      case MoveJudgmentCauseWitnessBindingLevel.ObjectContext      => 3
      case MoveJudgmentCauseWitnessBindingLevel.Punishment         => 4

  private def longTermRootFrame(frame: MoveJudgmentCauseFrame): Boolean =
    frame.role == MoveJudgmentCauseFrameRole.PrimaryCause &&
      frame.hasOwnedAdmissibleLongTermProof &&
      ClaimEventCluster.kindForCause(frame.causeKind).isEmpty &&
      frame.comparisonKind == CandidateComparisonKind.PlayedVsBest &&
      frame.causeRole == RelativeCauseRole.PrimaryPlayedCause &&
      frame.causeImportance == RelativeCauseImportance.Primary &&
      frame.attributionDirectProofEligible

  private def tacticalWitnessFrame(frame: MoveJudgmentCauseFrame): Boolean =
    frame.role == MoveJudgmentCauseFrameRole.PrimaryCause &&
      ClaimEventCluster.kindForCause(frame.causeKind).exists(kind =>
        kind == ClaimEventClusterKind.TacticalEvent ||
          kind == ClaimEventClusterKind.DefensiveEvent ||
          kind == ClaimEventClusterKind.ConversionEvent ||
          kind == ClaimEventClusterKind.MaterialEvent
      )

  private def tacticalWitnessNarrativeFrame(frame: MoveJudgmentCauseFrame): Boolean =
    tacticalWitnessFrame(frame) && eventRootSelectable(frame)

  private def sameComparison(left: MoveJudgmentCauseFrame, right: MoveJudgmentCauseFrame): Boolean =
    comparisonFrameKey(left) == comparisonFrameKey(right)

  private def comparisonFrameKey(frame: MoveJudgmentCauseFrame): (CandidateComparisonKind, LineNodeRef, LineNodeRef) =
    (frame.comparisonKind, frame.referenceLine, frame.candidateLine)

