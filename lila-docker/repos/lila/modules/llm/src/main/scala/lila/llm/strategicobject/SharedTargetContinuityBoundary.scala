package lila.llm.strategicobject

import chess.{ Color, Square }

private[strategicobject] object SharedTargetContinuityBoundary:

  private val packetOwner: Color =
    Color.White

  private val packetTargetSquare: Square =
    Square.fromKey("d6").getOrElse(
      throw new IllegalStateException("missing packet continuity target d6")
    )

  private val packetFixationWitnessSquare: Square =
    Square.fromKey("d5").getOrElse(
      throw new IllegalStateException("missing packet fixation witness d5")
    )

  private val packetComparativeSupportBundle: Set[String] =
    Set(
      "DefenderDependencyNetwork-white-kingside-f3-fgh"
    )

  private val packetWitness: CertifiedBoundaryWitness =
    CertifiedBoundaryWitness.SharedTargetContinuity(packetTargetSquare)

  def certify(
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    val packetClaimIds =
      packetCurrentPositionClaimIds(claims) ++
        packetWhyThisClaimIds(claims) ++
        packetWhatChangedClaimIds(claims)

    claims.map(claim =>
      if packetClaimIds.contains(claim.id) then
        claim.copy(boundaryWitnesses = claim.boundaryWitnesses + packetWitness)
      else claim
    )

  def sharesPacketContinuity(
      primaryClaim: CertifiedClaim,
      supportClaim: CertifiedClaim
  ): Boolean =
    primaryClaim.boundaryWitnesses.contains(packetWitness) &&
      supportClaim.boundaryWitnesses.contains(packetWitness)

  def hasPacketContinuity(
      claim: CertifiedClaim
  ): Boolean =
    claim.boundaryWitnesses.contains(packetWitness)

  def projectorSupportObjectIds(
      obj: StrategicObject,
      scope: StrategicDeltaScope,
      projection: StrategicDeltaProjection,
      defaultSupportObjectIds: List[String],
      objectsById: Map[String, StrategicObject]
  ): List[String] =
    Option
      .when(
        isPacketComparativeSupportSlice(
          obj = obj,
          scope = scope,
          projection = projection,
          defaultSupportObjectIds = defaultSupportObjectIds,
          objectsById = objectsById
        )
      )(packetComparativeSupportBundle.toList.sorted)
      .getOrElse(defaultSupportObjectIds)

  private def packetCurrentPositionClaimIds(
      claims: List[CertifiedClaim]
  ): Set[String] =
    claims.collect {
      case claim
          if claim.status == ClaimStatus.Certified &&
            claim.hasTypedDelta &&
            claim.delta.exists(CurrentPositionProbeSlice.isPacketOwnedD6FixedTargetProbeDelta) =>
        claim.id
    }.toSet

  private def packetWhyThisClaimIds(
      claims: List[CertifiedClaim]
  ): Set[String] =
    claims.collect {
      case claim
          if claim.status == ClaimStatus.Certified &&
            claim.hasTypedDelta &&
            claim.delta.exists(isPacketOwnedD6TargetFixationDelta) =>
        claim.id
    }.toSet

  private def packetWhatChangedClaimIds(
      claims: List[CertifiedClaim]
  ): Set[String] =
    val primaryClaims =
      claims.filter(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.hasTypedDelta &&
          isCertifiedPacketComparativePrimaryClaim(claim)
      )
    val supportClaims =
      claims.filter(claim =>
        claim.hasTypedDelta &&
        isCertifiedPacketComparativeSupportClaim(claim)
      )

    bestPacketComparativeContinuityCandidate(primaryClaims, supportClaims)
      .map(candidate => Set(candidate.primaryClaimId, candidate.supportClaimId))
      .getOrElse(Set.empty)

  private final case class ComparativeContinuityCandidate(
      primaryClaimId: String,
      supportClaimId: String,
      primaryTargetRankScore: Int,
      supportRestrictionScore: Int,
      sharedTargetCount: Int,
      supportWitnessScore: Int,
      supportTargetOverlapScore: Int,
      primaryStandingScore: Int,
      primaryCentralityScore: Int
  )

  private def bestPacketComparativeContinuityCandidate(
      primaryClaims: List[CertifiedClaim],
      supportClaims: List[CertifiedClaim]
  ): Option[ComparativeContinuityCandidate] =
    primaryClaims
      .flatMap(primaryClaim =>
        supportClaims.collect {
          case supportClaim if isPacketComparativeContinuityPair(primaryClaim, supportClaim) =>
            comparativeContinuityCandidate(primaryClaim, supportClaim)
        }
      )
      .sortBy(candidate =>
        (
          -candidate.primaryCentralityScore,
          -candidate.primaryTargetRankScore,
          -candidate.supportRestrictionScore,
          -candidate.sharedTargetCount,
          -candidate.supportTargetOverlapScore,
          -candidate.supportWitnessScore,
          -candidate.primaryStandingScore,
          candidate.primaryClaimId,
          candidate.supportClaimId
        )
      )
      .headOption

  private def isCertifiedPacketComparativePrimaryClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(isPacketComparativePrimaryDelta)

  private def isCertifiedPacketComparativeSupportClaim(
      claim: CertifiedClaim
  ): Boolean =
    claim.delta.exists(isPacketComparativeSupportDelta)

  private def isPacketOwnedD6TargetFixationDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.FixedTargetComplex,
            owner,
            StrategicDeltaScope.MoveLocal,
            StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, fixed, _),
            StrategicDeltaProjection.MoveLocal(StrategicDeltaTag.TargetFixed, witness),
            _,
            _,
            _,
            evidenceRefs
          ) =>
        owner == packetOwner &&
          fixed &&
          targetSquare == packetTargetSquare &&
          witness.isTransitionAware &&
          witness.hasAnchoredEvidence &&
          witness.matchedSquares.contains(packetFixationWitnessSquare) &&
          evidenceRefs.nonEmpty
      case _ =>
        false

  private def isPacketComparativePrimaryDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.FixedTargetComplex,
            owner,
            StrategicDeltaScope.Comparative,
            StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, fixed, _),
            StrategicDeltaProjection.Comparative(_, _, _, _, profile),
            _,
            _,
            _,
            evidenceRefs
          ) =>
        owner == packetOwner &&
          fixed &&
          targetSquare == packetTargetSquare &&
          profile.metrics.size >= 2 &&
          evidenceRefs.nonEmpty &&
          Set(
            StrategicComparativeAxis.FixedTargetPressureContrast,
            StrategicComparativeAxis.FixedTargetDefenseContrast
          ).contains(profile.axis)
      case _ =>
        false

  private def isPacketComparativeSupportDelta(
      delta: StrategicObjectDelta
  ): Boolean =
    delta match
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.RestrictionShell,
            owner,
            StrategicDeltaScope.Comparative,
            StrategicObjectProfile.RestrictionShell(_, _, _),
            StrategicDeltaProjection.Comparative(_, _, witness, _, profile),
            _,
            _,
            _,
            evidenceRefs
          ) =>
        owner == packetOwner &&
          isPacketComparativeSupportDescriptor(delta) &&
          profile.metrics.nonEmpty &&
          profile.axis == StrategicComparativeAxis.RestrictionContainmentContrast &&
          witness.axis == StrategicComparativeAxis.RestrictionContainmentContrast &&
          evidenceRefs.nonEmpty
      case _ =>
        false

  private def isPacketComparativeSupportDescriptor(
      delta: StrategicObjectDelta
  ): Boolean =
    delta.supportingObjectIds.toSet == packetComparativeSupportBundle

  private def isPacketComparativeSupportSlice(
      obj: StrategicObject,
      scope: StrategicDeltaScope,
      projection: StrategicDeltaProjection,
      defaultSupportObjectIds: List[String],
      objectsById: Map[String, StrategicObject]
  ): Boolean =
    scope == StrategicDeltaScope.Comparative &&
      obj.family == StrategicObjectFamily.RestrictionShell &&
      obj.owner == packetOwner &&
      packetComparativeSupportBundle.subsetOf(objectsById.keySet) &&
      defaultSupportObjectIds.nonEmpty &&
      defaultSupportObjectIds.toSet.subsetOf(packetComparativeSupportBundle) &&
      restrictionShellTouchesPacketTarget(obj.profile) &&
      (projection match
        case StrategicDeltaProjection.Comparative(_, _, witness, _, profile) =>
          profile.axis == StrategicComparativeAxis.RestrictionContainmentContrast &&
            witness.axis == StrategicComparativeAxis.RestrictionContainmentContrast &&
            witness.hasExactCounterpartWitness
        case _ =>
          false)

  private def isPacketComparativeContinuityPair(
      primaryClaim: CertifiedClaim,
      supportClaim: CertifiedClaim
  ): Boolean =
    (primaryClaim.delta, supportClaim.delta) match
      case (Some(primaryDelta), Some(supportDelta)) =>
        primaryDelta.scope == StrategicDeltaScope.Comparative &&
          supportDelta.scope == StrategicDeltaScope.Comparative &&
        primaryDelta.family == StrategicObjectFamily.FixedTargetComplex &&
          supportDelta.family == StrategicObjectFamily.RestrictionShell &&
          primaryDelta.owner == supportDelta.owner &&
          isPacketComparativePrimaryDelta(primaryDelta) &&
          isPacketComparativeSupportDelta(supportDelta) &&
          comparativeSupportCorroborationCount(supportDelta) >= packetComparativeSupportBundle.size &&
          primaryTargetSquares(primaryDelta).contains(packetTargetSquare.key)
      case _ =>
        false

  private def comparativeContinuityCandidate(
      primaryClaim: CertifiedClaim,
      supportClaim: CertifiedClaim
  ): ComparativeContinuityCandidate =
    val primaryDelta = primaryClaim.delta.getOrElse(
      throw new IllegalStateException(s"missing primary delta for ${primaryClaim.id}")
    )
    val supportDelta = supportClaim.delta.getOrElse(
      throw new IllegalStateException(s"missing support delta for ${supportClaim.id}")
    )
    val sharedTargetSquares = sharedComparativeTargetSquares(primaryDelta, supportDelta)

    ComparativeContinuityCandidate(
      primaryClaimId = primaryClaim.id,
      supportClaimId = supportClaim.id,
      primaryTargetRankScore = primaryTargetRankScore(primaryDelta),
      supportRestrictionScore = restrictionSupportOverlapScore(primaryDelta, supportDelta),
      sharedTargetCount = sharedTargetSquares.size,
      supportWitnessScore = comparativeWitnessSpecificityScore(supportDelta),
      supportTargetOverlapScore = supportTargetOverlapScore(supportDelta, sharedTargetSquares),
      primaryStandingScore = comparativeStandingScore(primaryDelta),
      primaryCentralityScore = comparativeCentralityScore(primaryDelta)
    )

  private def sharedComparativeTargetSquares(
      primaryDelta: StrategicObjectDelta,
      supportDelta: StrategicObjectDelta
  ): Set[String] =
    primaryTargetSquares(primaryDelta).intersect(exactComparativeSupportSquares(supportDelta))

  private def primaryTargetSquares(
      delta: StrategicObjectDelta
  ): Set[String] =
    delta.changedAnchors
      .flatMap(_.squares)
      .map(_.key)
      .toSet

  private def exactComparativeSupportSquares(
      delta: StrategicObjectDelta
  ): Set[String] =
    delta.profile match
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares) =>
        (restrictedSquares ++ constraintSquares).map(_.key).toSet
      case _ =>
        delta.changedAnchors
          .flatMap(_.squares)
          .map(_.key)
          .toSet

  private def restrictionShellTouchesPacketTarget(
      profile: StrategicObjectProfile
  ): Boolean =
    profile match
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares) =>
        (restrictedSquares ++ constraintSquares).contains(packetTargetSquare)
      case _ =>
        false

  private def comparativeSupportCorroborationCount(
      delta: StrategicObjectDelta
  ): Int =
    delta.supportingObjectIds.distinct.size

  private def comparativeStandingScore(
      delta: StrategicObjectDelta
  ): Int =
    delta.projection match
      case StrategicDeltaProjection.Comparative(_, balance, _, _, _) =>
        balance.standing match
          case ComparativeStanding.Ahead     => 8
          case ComparativeStanding.Contested => 4
          case ComparativeStanding.Balanced  => 2
          case ComparativeStanding.Behind    => 0
      case _ =>
        0

  private def comparativeCentralityScore(
      delta: StrategicObjectDelta
  ): Int =
    primaryTargetSquares(delta)
      .flatMap(squareKey => chess.Square.fromKey(squareKey).toList)
      .map(square => 3 - math.abs(square.file.char.toInt - 'd'.toInt))
      .maxOption
      .getOrElse(0)

  private def primaryTargetRankScore(
      delta: StrategicObjectDelta
  ): Int =
    primaryTargetSquares(delta)
      .flatMap(squareKey => squareKey.drop(1).toIntOption)
      .maxOption
      .getOrElse(0)

  private def restrictionSupportOverlapScore(
      primaryDelta: StrategicObjectDelta,
      supportDelta: StrategicObjectDelta
  ): Int =
    supportDelta.profile match
      case StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares) =>
        val primaryTargets = primaryTargetSquares(primaryDelta)
        if primaryTargets.intersect(restrictedSquares.map(_.key).toSet).nonEmpty then 2
        else if primaryTargets.intersect(constraintSquares.map(_.key).toSet).nonEmpty then 1
        else 0
      case _ =>
        0

  private def comparativeWitnessSpecificityScore(
      delta: StrategicObjectDelta
  ): Int =
    delta.comparativeWitness.map { witness =>
      witness.counterpartWitnessKinds.toList.map {
        case StrategicCounterpartWitnessKind.SharedTarget         => 16
        case StrategicCounterpartWitnessKind.SharedSquare         => 8
        case StrategicCounterpartWitnessKind.SharedFile           => 4
        case StrategicCounterpartWitnessKind.SharedRoute          => 2
        case StrategicCounterpartWitnessKind.DirectRivalReference => 2
        case StrategicCounterpartWitnessKind.SharedPiece          => 1
      }.sum + witness.matchedSquares.size + witness.matchedFiles.size + witness.relationWitnesses.size
    }.getOrElse(0)

  private def supportTargetOverlapScore(
      delta: StrategicObjectDelta,
      sharedTargetSquares: Set[String]
  ): Int =
    exactComparativeSupportSquares(delta).intersect(sharedTargetSquares).size
