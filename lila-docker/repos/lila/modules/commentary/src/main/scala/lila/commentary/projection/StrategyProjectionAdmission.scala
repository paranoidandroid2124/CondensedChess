package lila.commentary.projection

import chess.{ Bishop, Color, File, Pawn, Position, Rank, Square }
import chess.variant

import scala.util.Try

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.certification.{
  Certification,
  CertificationEvidenceBundle,
  CertificationExtractor,
  CertificationVerdict
}
import lila.commentary.delta.{ StrategicDelta, StrategicDeltaExtraction, StrategicDeltaExtractor }
import lila.commentary.strategic.{ StrategicObject, StrategicObjectExtraction, StrategicObjectExtractor }
import lila.commentary.witness.{ Witness, WitnessAnchor, WitnessDescriptorId, WitnessSector, WitnessValue }
import lila.commentary.witness.u.{ UExtractionContext, UWitnessExtractor }
import lila.commentary.witness.seed.{
  StrategySupportSeed,
  StrategySupportSeedExtraction,
  StrategySupportSeedId,
  StrategySupportSeedScopeContract
}

object StrategyProjectionAdmission:

  def admits(
      bandId: StrategyProjectionBandId,
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle = CertificationEvidenceBundle.empty,
      deltaExtraction: Option[StrategicDeltaExtraction] = None
  ): Either[String, Boolean] =
    if !StrategyProjectionScopeContract.isStartReadyBandId(bandId) then
      Left(s"Unsupported projection admission band: ${bandId.value}")
    else if !evidence.matches(extraction.rootState) then
      Left("Strategy projection admission rejected stale evidence bundle")
    else if !certificationEvidence.isEmpty && !certificationEvidence.matches(extraction.rootState) then
      Left("Strategy projection admission rejected stale certification evidence bundle")
    else
      validatedDeltaEvidence(extraction, deltaExtraction).map: canonicalDelta =>
        bandId.value match
          case "S05" => admitsS05(extraction, evidence, owner)
          case "S06" => admitsS06(extraction, evidence, owner, certificationEvidence)
          case "S07" => admitsS07(extraction, evidence, owner, certificationEvidence)
          case "S08" => admitsS08(extraction, evidence, owner, certificationEvidence)
          case "S11" => admitsS11(extraction, evidence, owner)
          case "S13" => admitsS13(extraction, evidence, owner)
          case "S14" => admitsS14(extraction, evidence, owner)
          case "S15" => admitsS15(extraction, evidence, owner)
          case "S16" => admitsS16(extraction, evidence, owner, certificationEvidence)
          case "S17" => admitsS17(extraction, evidence, owner)
          case "S18" => admitsS18(extraction, evidence, owner, certificationEvidence)
          case "S19" => admitsS19(extraction, evidence, owner, certificationEvidence, canonicalDelta)
          case "S21" => admitsS21(extraction, evidence, owner, certificationEvidence)
          case "S22" => admitsS22(extraction, evidence, owner, certificationEvidence)
          case "S23" => admitsS23(extraction, evidence, owner)
          case "S24" => admitsS24(extraction, evidence, owner)
          case "S25" => admitsS25(extraction, evidence, owner)
          case other => throw MatchError(other)

  private final case class S05CenterReleaseCarrier(
      contactSource: Square,
      target: Square,
      centerReleaseRoute: String
  )

  private def admitsS05(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val current = UWitnessExtractor.fromRoot(extraction.rootState)
    s05CenterReleaseCarriers(current, owner).exists: carrier =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S05,
          StrategyProjectionScopeContract.CenterReleaseRouteCertified,
          owner,
          WitnessAnchor.SquareAnchor(carrier.target)
        )
        .exists(claim => s05EvidenceBindsCarrierAndTask(claim, carrier))

  private def s05CenterReleaseCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color
  ): Vector[S05CenterReleaseCarrier] =
    pawnPushBreakContactSourceTargetPairs(current, owner).flatMap: (source, target) =>
      Option.when(
        isCenterFile(source) &&
          isCenterFile(target) &&
          hasAvailableLeverTrigger(current, owner, source, target)
      )(
        Vector(
          S05CenterReleaseCarrier(source, target, "center_pawn_target"),
          S05CenterReleaseCarrier(source, target, "central_axis_continuation")
        )
      )
    .flatten
      .distinct
      .sortBy(carrier => (carrier.contactSource.value, carrier.target.value, carrier.centerReleaseRoute))

  private def s05EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S05CenterReleaseCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.SquareAnchor(carrier.target) &&
      square(claim.payload, "contact_source_square").contains(carrier.contactSource) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      token(claim.payload, "center_release_route").contains(carrier.centerReleaseRoute)

  private final case class S06SpaceBindCarrier(
      routeAnchor: Square,
      spaceBindRoute: String,
      structuralSector: String,
      structuralHostId: String,
      outpostSquare: Option[Square],
      restrictionAnchorSquare: Option[Square]
  )

  private final case class S06StructuralHost(
      sector: String,
      hostId: String,
      claimedSquares: Set[Square]
  )

  private def admitsS06(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      certifiedClaim(current, certificationEvidence, "SpaceBindRestrictionCertification", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          s06SpaceBindCarriers(current, owner).exists: carrier =>
            certification.support.targetSquares.contains(carrier.routeAnchor) &&
              tokenList(certification.payload, "route_host_links")
                .contains(s06RouteHostLink(carrier)) &&
              evidence
                .evidenceFor(
                  StrategyProjectionScopeContract.S06,
                  StrategyProjectionScopeContract.SpaceBindRestrictionRouteCertified,
                  owner,
                  WitnessAnchor.PieceSquareAnchor(carrier.routeAnchor)
                )
                .exists(claim => s06EvidenceBindsCarrierAndTask(claim, carrier))

  private def s06SpaceBindCarriers(
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[S06SpaceBindCarrier] =
    val structuralHosts =
      current.attachedWitnesses.all
        .filter(witness =>
          witness.descriptorId == WitnessDescriptorId("structural_space_claim") &&
            witness.color.contains(owner)
        )
        .flatMap: witness =>
          for
            sector <- witness.anchor match
              case WitnessAnchor.SectorAnchor(sector) => Some(sector.key)
              case _                                  => None
            hostId <- token(witness.payload, "host_id")
          yield S06StructuralHost(
            sector,
            hostId,
            squareList(witness.payload, "claimed_squares").toSet
          )
        .distinct
        .sortBy(host => (host.sector, host.hostId))

    val outpostCarriers =
      current.primaryWitnesses.all
        .filter(witness =>
          witness.descriptorId == WitnessDescriptorId("knight_on_outpost_square") &&
            witness.color.contains(owner)
        )
        .flatMap: witness =>
          pieceAnchorSquare(witness).toVector.flatMap: outpost =>
            structuralHosts
              .filter(host => s06OutpostBelongsToHost(outpost, host))
              .map: host =>
                S06SpaceBindCarrier(
                  outpost,
                  "outpost_anchor",
                  host.sector,
                  host.hostId,
                  Some(outpost),
                  None
                )

    val restrictionCarriers =
      current.primaryWitnesses.all
        .filter(witness =>
          witness.descriptorId == WitnessDescriptorId("short_run_slider_gate_restriction") &&
            witness.color.contains(owner)
        )
        .flatMap: witness =>
          pieceAnchorSquare(witness).toVector.flatMap: restrictionAnchor =>
            structuralHosts
              .filter(host => s06RestrictionBelongsToHost(witness, host))
              .map: host =>
                S06SpaceBindCarrier(
                  restrictionAnchor,
                  "non_outpost_space_bind",
                  host.sector,
                  host.hostId,
                  None,
                  Some(restrictionAnchor)
                )

    (outpostCarriers ++ restrictionCarriers)
      .distinct
      .sortBy(carrier =>
        (
          carrier.routeAnchor.value,
          carrier.spaceBindRoute,
          carrier.structuralSector,
          carrier.structuralHostId
        )
      )

  private def s06OutpostBelongsToHost(outpost: Square, host: S06StructuralHost): Boolean =
    host.claimedSquares.contains(outpost) ||
      outpost.knightAttacks.exists(host.claimedSquares.contains)

  private def s06RestrictionBelongsToHost(witness: Witness, host: S06StructuralHost): Boolean =
    witness.support.targetSquares.exists(square =>
      host.claimedSquares.contains(square) ||
        square.kingAttacks.exists(host.claimedSquares.contains)
    )

  private def s06RouteHostLink(carrier: S06SpaceBindCarrier): String =
    s"${carrier.routeAnchor.key}|${carrier.spaceBindRoute}|${carrier.structuralSector}|${carrier.structuralHostId}"

  private def s06EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S06SpaceBindCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.PieceSquareAnchor(carrier.routeAnchor) &&
      square(claim.payload, "route_anchor_square").contains(carrier.routeAnchor) &&
      token(claim.payload, "space_bind_route").contains(carrier.spaceBindRoute) &&
      token(claim.payload, "structural_sector").contains(carrier.structuralSector) &&
      token(claim.payload, "structural_host_id").contains(carrier.structuralHostId) &&
      token(claim.payload, "certification_family").contains("SpaceBindRestrictionCertification") &&
      (carrier.outpostSquare match
        case Some(outpost) => square(claim.payload, "outpost_square").contains(outpost)
        case None          => square(claim.payload, "outpost_square").isEmpty
      ) &&
      (carrier.restrictionAnchorSquare match
        case Some(anchor) => square(claim.payload, "restriction_anchor_square").contains(anchor)
        case None         => square(claim.payload, "restriction_anchor_square").isEmpty
      )

  private def admitsS07(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      val development =
        certifiedClaim(current, certificationEvidence, "DevelopmentComparison", owner)
      val initiative =
        certifiedClaim(current, certificationEvidence, "InitiativeWindow", owner)
      val currentWitnesses = UWitnessExtractor.fromRoot(extraction.rootState)
      development.exists(_.verdict == CertificationVerdict.Certified) &&
        initiative.exists(certification =>
          certification.verdict == CertificationVerdict.Certified &&
            certification.anchor == WitnessAnchor.BoardAnchor &&
            certification.support.targetSquares.nonEmpty
        ) &&
        !hasOwnerWeakPawnTarget(currentWitnesses, owner) &&
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S07,
            StrategyProjectionScopeContract.InitiativeConversionRouteCertified,
            owner,
            WitnessAnchor.BoardAnchor
          )
          .exists(s07EvidenceBindsCarrierAndTask)

  private def s07EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim
  ): Boolean =
    token(claim.payload, "certification_family").contains("InitiativeWindow") &&
      token(claim.payload, "initiative_conversion_route").exists:
        case "development_led_window" | "move_right_window" => true
        case _ => false

  private def hasOwnerWeakPawnTarget(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color
  ): Boolean =
    current.witnesses.all.exists(witness =>
      witness.descriptorId == WitnessDescriptorId("weak_pawn_target_state") &&
        witness.color.contains(owner)
    )

  private final case class S08DenialCarrier(
      contactSource: Square,
      target: Square,
      counterplayDenialRoute: String
  )

  private def admitsS08(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      certifiedClaim(current, certificationEvidence, "InitiativeWindow", owner).exists: certification =>
          certification.verdict == CertificationVerdict.Certified &&
          certification.anchor == WitnessAnchor.BoardAnchor &&
          certification.support.targetSquares.nonEmpty &&
          s21SurvivalCarriers(UWitnessExtractor.fromRoot(extraction.rootState), owner).isEmpty &&
          s08DenialCarriers(UWitnessExtractor.fromRoot(extraction.rootState), UExtractionContext(extraction.rootState), owner)
            .exists: carrier =>
            evidence
              .evidenceFor(
                StrategyProjectionScopeContract.S08,
                StrategyProjectionScopeContract.CounterplayDenialRouteCertified,
                owner,
                WitnessAnchor.PieceSquareAnchor(carrier.contactSource)
              )
              .exists(claim => s08EvidenceBindsCarrierAndTask(claim, carrier))

  private def s08DenialCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      context: UExtractionContext,
      owner: Color
  ): Vector[S08DenialCarrier] =
    pawnPushBreakContactSourceTargetPairs(current, !owner).flatMap: (source, target) =>
      Option
        .when(hasS08RivalReleaseReserve(context, !owner))(target)
        .flatMap(s08DenialRouteFor)
        .map(route => S08DenialCarrier(source, target, route))
    .distinct
      .sortBy(carrier => (carrier.contactSource.value, carrier.target.value, carrier.counterplayDenialRoute))

  private def hasS08RivalReleaseReserve(context: UExtractionContext, rival: Color): Boolean =
    val reserveSquare =
      if rival.black then Square(File.A, Rank.Seventh) else Square(File.H, Rank.Second)
    context.hasPieceOn(rival, Pawn, reserveSquare)

  private def s08DenialRouteFor(target: Square): Option[String] =
    target.file match
      case File.C => Some("rival_break_source_suppressed")
      case File.E => Some("rival_counterplay_source_suppressed")
      case _      => None

  private def s08EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S08DenialCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.PieceSquareAnchor(carrier.contactSource) &&
      square(claim.payload, "contact_source_square").contains(carrier.contactSource) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      token(claim.payload, "counterplay_denial_route").contains(carrier.counterplayDenialRoute) &&
      token(claim.payload, "certification_family").contains("InitiativeWindow")

  private final case class S21SurvivalCarrier(
      contactSource: Square,
      target: Square,
      counterplaySurvivalRoute: String
  )

  private def admitsS21(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      certifiedClaim(current, certificationEvidence, "InitiativeWindow", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          certification.anchor == WitnessAnchor.BoardAnchor &&
          certification.support.targetSquares.nonEmpty &&
          s21SurvivalCarriers(UWitnessExtractor.fromRoot(extraction.rootState), owner).exists: carrier =>
            evidence
              .evidenceFor(
                StrategyProjectionScopeContract.S21,
                StrategyProjectionScopeContract.CounterplaySurvivalRouteCertified,
                owner,
                WitnessAnchor.PieceSquareAnchor(carrier.contactSource)
              )
              .exists(claim => s21EvidenceBindsCarrierAndTask(claim, carrier))

  private def s21SurvivalCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color
  ): Vector[S21SurvivalCarrier] =
    pawnPushBreakContactSourceTargetPairs(current, owner).flatMap: (source, target) =>
      if isNonCenterFile(source) && isCenterFile(target) then
        Vector(S21SurvivalCarrier(source, target, "center_source_survives"))
      else if isNonCenterFile(source) && isNonCenterFile(target) then
        Vector(S21SurvivalCarrier(source, target, "far_wing_source_survives"))
      else Vector.empty
    .distinct
      .sortBy(carrier => (carrier.contactSource.value, carrier.target.value, carrier.counterplaySurvivalRoute))

  private def s21EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S21SurvivalCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.PieceSquareAnchor(carrier.contactSource) &&
      square(claim.payload, "contact_source_square").contains(carrier.contactSource) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      token(claim.payload, "counterplay_survival_route").contains(carrier.counterplaySurvivalRoute) &&
      token(claim.payload, "certification_family").contains("InitiativeWindow")

  private def admitsS11(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val current = UWitnessExtractor.fromRoot(extraction.rootState)
    val context = UExtractionContext(extraction.rootState)
    fixedWeakPawnTargets(current, owner).exists: target =>
      val attackers = legalOwnerAttackersTo(context, owner, target).toSet
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S11,
          StrategyProjectionScopeContract.WeakPawnTargetPressurePersistenceCertified,
          owner,
          WitnessAnchor.SquareAnchor(target)
        )
        .exists(claim => s11EvidenceBindsCarrierAndTask(claim, target, attackers))

  private def fixedWeakPawnTargets(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color
  ): Vector[Square] =
    current.witnesses.all
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("weak_pawn_target_state") &&
          witness.color.contains(owner) &&
          witness.payload
            .get("weakness_tags")
            .collect { case WitnessValue.TokenListValue(values) => values.contains("fixed") }
            .contains(true)
      )
      .flatMap(witness =>
        witness.payload.get("square").collect { case WitnessValue.SquareValue(square) => square }
      )
      .distinct
      .sortBy(_.value)

  private def legalOwnerAttackersTo(
      context: UExtractionContext,
      owner: Color,
      target: Square
  ): Vector[Square] =
    val position = Position(context.board.toBoard, variant.Standard, owner)
    Square.all
      .filter(square => position.pieceAt(square).exists(_.color == owner))
      .filter(square => position.generateMovesAt(square).exists(_.dest == target))
      .toVector
      .sortBy(_.value)

  private def s11EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      target: Square,
      pressureSourceSquares: Set[Square]
  ): Boolean =
    square(claim.payload, "target_square").contains(target) &&
      token(claim.payload, "persistence_kind").contains("fixed") &&
      sameSquareSet(squareList(claim.payload, "pressure_source_squares"), pressureSourceSquares) &&
      token(claim.payload, "pressure_route").exists:
        case "same_target_fixation" => pressureSourceSquares.size == 1
        case "same_target_repeated_pressure" => pressureSourceSquares.sizeCompare(2) >= 0
        case _ => false

  private final case class S13DamageCarrier(
      contactSource: Square,
      target: Square,
      sector: WitnessSector,
      damageRoute: String
  )

  private def admitsS13(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val current = UWitnessExtractor.fromRoot(extraction.rootState)
    val context = UExtractionContext(extraction.rootState)
    s13DamageCarriers(current, context, owner).exists: carrier =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S13,
          StrategyProjectionScopeContract.WingDamageRouteCertified,
          owner,
          WitnessAnchor.SquareAnchor(carrier.target)
        )
        .exists(claim => s13EvidenceBindsCarrierAndTask(claim, carrier))

  private def s13DamageCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      context: UExtractionContext,
      owner: Color
  ): Vector[S13DamageCarrier] =
    val defender = !owner
    pawnPushBreakContactSourceTargetPairs(current, owner).flatMap: (source, target) =>
      val sector = sectorOf(target.file)
      Option.when(
        isWingSector(sector) &&
          sectorOf(source.file) == sector &&
          hasAvailableLeverTrigger(current, owner, source, target) &&
          hasDefenderMajoritySectorAsymmetry(current, sector, defender)
      ):
        val route =
          if isPhalanxEdgeTarget(context, defender, target) then Some("phalanx_edge_target")
          else if isStructurallyBurdenedTarget(context, defender, target) then Some("structurally_burdened_target")
          else None
        route.map(S13DamageCarrier(source, target, sector, _))
    .flatten
      .distinct
      .sortBy(carrier => (carrier.contactSource.value, carrier.target.value, carrier.damageRoute))

  private def s13EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S13DamageCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.SquareAnchor(carrier.target) &&
      square(claim.payload, "contact_source_square").contains(carrier.contactSource) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      token(claim.payload, "damage_route").contains(carrier.damageRoute) &&
      token(claim.payload, "damage_sector").contains(carrier.sector.key)

  private final case class S14ChainBaseCarrier(
      contactSource: Square,
      target: Square,
      chainBaseRoute: String,
      forwardSupportSquares: Vector[Square]
  )

  private def admitsS14(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val current = UWitnessExtractor.fromRoot(extraction.rootState)
    val context = UExtractionContext(extraction.rootState)
    s14ChainBaseCarriers(current, context, owner).exists: carrier =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S14,
          StrategyProjectionScopeContract.ChainBaseContactRouteCertified,
          owner,
          WitnessAnchor.SquareAnchor(carrier.target)
        )
        .exists(claim => s14EvidenceBindsCarrierAndTask(claim, carrier))

  private def s14ChainBaseCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      context: UExtractionContext,
      owner: Color
  ): Vector[S14ChainBaseCarrier] =
    val defender = !owner
    pawnPushBreakContactSourceTargetPairs(current, owner).flatMap: (source, target) =>
      val forwardSupportSquares = chainBaseForwardSupportSquares(context, defender, target)
      Option.when(
        isNonCenterFile(target) &&
          hasAvailableLeverTrigger(current, owner, source, target) &&
          isChainBaseTarget(context, defender, target) &&
          forwardSupportSquares.nonEmpty
      )(
        S14ChainBaseCarrier(
          source,
          target,
          s14ChainBaseRoute(context, defender, forwardSupportSquares),
          forwardSupportSquares
        )
      )
    .distinct
      .sortBy(carrier => (carrier.contactSource.value, carrier.target.value, carrier.chainBaseRoute))

  private def s14EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S14ChainBaseCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.SquareAnchor(carrier.target) &&
      square(claim.payload, "contact_source_square").contains(carrier.contactSource) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      token(claim.payload, "chain_base_route").contains(carrier.chainBaseRoute) &&
      sameSquareSet(squareList(claim.payload, "chain_base_forward_squares"), carrier.forwardSupportSquares.toSet)

  private def s14ChainBaseRoute(
      context: UExtractionContext,
      defender: Color,
      forwardSupportSquares: Vector[Square]
  ): String =
    if forwardSupportSquares.exists(square => chainBaseForwardSupportSquares(context, defender, square).nonEmpty) then
      "base_contact_continuation"
    else "chain_base_target"

  private sealed trait S15CreationCarrier:
    def candidate: Square
    def target: Square
    def creationRoute: String

  private final case class S15WingDamageCarrier(
      candidate: Square,
      target: Square,
      sector: WitnessSector,
      damageRoute: String
  ) extends S15CreationCarrier:
    val creationRoute: String = "s13_wing_damage"

  private final case class S15ChainBaseCarrier(
      candidate: Square,
      target: Square,
      chainBaseRoute: String,
      forwardSupportSquares: Vector[Square]
  ) extends S15CreationCarrier:
    val creationRoute: String = "s14_chain_base"

  private def admitsS15(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val current = UWitnessExtractor.fromRoot(extraction.rootState)
    val context = UExtractionContext(extraction.rootState)
    s15CreationCarriers(current, context, owner).exists: carrier =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S15,
          StrategyProjectionScopeContract.PasserCreationRouteCertified,
          owner,
          WitnessAnchor.PieceSquareAnchor(carrier.candidate)
        )
        .exists(claim => s15EvidenceBindsCarrierAndTask(claim, carrier))

  private def s15CreationCarriers(
      current: lila.commentary.witness.u.UWitnessExtraction,
      context: UExtractionContext,
      owner: Color
  ): Vector[S15CreationCarrier] =
    val candidatePassers =
      context.activeColorPawnSquares(SchemaId.CandidatePasser, owner).toSet
    val wingDamage =
      s13DamageCarriers(current, context, owner).collect:
        case carrier if candidatePassers.contains(carrier.contactSource) =>
          S15WingDamageCarrier(
            candidate = carrier.contactSource,
            target = carrier.target,
            sector = carrier.sector,
            damageRoute = carrier.damageRoute
          )
    val chainBase =
      s14ChainBaseCarriers(current, context, owner).collect:
        case carrier if candidatePassers.contains(carrier.contactSource) =>
          S15ChainBaseCarrier(
            candidate = carrier.contactSource,
            target = carrier.target,
            chainBaseRoute = carrier.chainBaseRoute,
            forwardSupportSquares = carrier.forwardSupportSquares
          )
    (wingDamage ++ chainBase)
      .distinct
      .sortBy(carrier => (carrier.candidate.value, carrier.target.value, carrier.creationRoute))

  private def s15EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S15CreationCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.PieceSquareAnchor(carrier.candidate) &&
      token(claim.payload, "creation_route").contains(carrier.creationRoute) &&
      square(claim.payload, "contact_source_square").contains(carrier.candidate) &&
      square(claim.payload, "target_square").contains(carrier.target) &&
      (carrier match
        case wing: S15WingDamageCarrier =>
          token(claim.payload, "damage_route").contains(wing.damageRoute) &&
            token(claim.payload, "damage_sector").contains(wing.sector.key)
        case chain: S15ChainBaseCarrier =>
          token(claim.payload, "chain_base_route").contains(chain.chainBaseRoute) &&
            sameSquareSet(squareList(claim.payload, "chain_base_forward_squares"), chain.forwardSupportSquares.toSet)
      )

  private final case class S16SuppressionCarrier(
      passer: Square,
      blocker: Option[Square],
      suppressionRoute: String,
      certificationFamily: String
  )

  private def admitsS16(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      val uCurrent = UWitnessExtractor.fromRoot(extraction.rootState)
      val context = UExtractionContext(extraction.rootState)
      s16SuppressionCarriers(current, uCurrent, context, owner, certificationEvidence).exists: carrier =>
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S16,
            StrategyProjectionScopeContract.PasserSuppressionRouteCertified,
            owner,
            WitnessAnchor.PieceSquareAnchor(carrier.passer)
          )
          .exists(claim => s16EvidenceBindsCarrierAndTask(claim, carrier))

  private def s16SuppressionCarriers(
      current: StrategicObjectExtraction,
      uCurrent: lila.commentary.witness.u.UWitnessExtraction,
      context: UExtractionContext,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Vector[S16SuppressionCarrier] =
    val enemy = !owner
    val enemyPassers = enemyPassedPawnSquares(uCurrent, enemy)
    val certifiedFortress = certifiedClaim(current, certificationEvidence, "FortressDrawCertification", owner)
      .filter(_.verdict == CertificationVerdict.Certified)
    val certifiedPerpetual = certifiedClaim(current, certificationEvidence, "PerpetualCheckHolding", owner)
      .filter(_.verdict == CertificationVerdict.Certified)
    val certifiedRace = certifiedClaim(current, certificationEvidence, "PromotionRace", owner)
      .filter(_.verdict == CertificationVerdict.Certified)

    val blockadeOrHold =
      enemyPassers.flatMap: passer =>
        val blocker =
          context.forwardSquare(enemy, passer).filter(square => context.pieceAt(square).exists(_.color == owner))
        Vector(
          Option.when(blocker.exists(square => certifiedFortress.exists(s16HoldCertificationTargets(passer, square))))(
            S16SuppressionCarrier(passer, blocker, "blockade_hold", "FortressDrawCertification")
          ),
          Option.when(
            blocker.exists(square =>
              hasOwnerShortRunRestrictionOnBlocker(uCurrent, owner, square) &&
                certifiedPerpetual.nonEmpty
            )
          )(
            S16SuppressionCarrier(passer, blocker, "restriction_hold", "PerpetualCheckHolding")
          )
        ).flatten

    val race =
      certifiedRace.toVector.flatMap: certification =>
        enemyPassers
          .filter(passer => certification.support.targetSquares.contains(passer))
          .map(passer => S16SuppressionCarrier(passer, None, "non_losing_race", "PromotionRace"))

    (blockadeOrHold ++ race)
      .distinct
      .sortBy(carrier =>
        (
          carrier.passer.value,
          carrier.blocker.map(_.value).getOrElse(-1),
          carrier.suppressionRoute,
          carrier.certificationFamily
        )
      )

  private def enemyPassedPawnSquares(
      current: lila.commentary.witness.u.UWitnessExtraction,
      enemy: Color
  ): Vector[Square] =
    current.witnesses.all
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("passed_pawn_entity_state") &&
          witness.color.contains(enemy)
      )
      .flatMap:
        _.anchor match
          case WitnessAnchor.PieceSquareAnchor(square) => Vector(square)
          case _                                      => Vector.empty
      .distinct
      .sortBy(_.value)

  private def hasOwnerShortRunRestrictionOnBlocker(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color,
      blocker: Square
  ): Boolean =
    current.witnesses.all.exists(witness =>
      witness.descriptorId == WitnessDescriptorId("short_run_slider_gate_restriction") &&
        witness.color.contains(owner) &&
        (
          squareList(witness.payload, "beneficiary_occupied_gate_squares").contains(blocker) ||
            squareList(witness.payload, "beneficiary_controlled_gate_squares").contains(blocker)
        )
    )

  private def s16HoldCertificationTargets(
      passer: Square,
      blocker: Square
  )(certification: Certification): Boolean =
    val targets = certification.support.targetSquares.toSet
    targets.contains(passer) || targets.contains(blocker)

  private def s16EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      carrier: S16SuppressionCarrier
  ): Boolean =
    claim.anchor == WitnessAnchor.PieceSquareAnchor(carrier.passer) &&
      token(claim.payload, "suppression_route").contains(carrier.suppressionRoute) &&
      square(claim.payload, "passer_square").contains(carrier.passer) &&
      token(claim.payload, "certification_family").contains(carrier.certificationFamily) &&
      (carrier.blocker match
        case Some(blocker) => square(claim.payload, "blocker_square").contains(blocker)
        case None          => square(claim.payload, "blocker_square").isEmpty
      )

  private def pawnPushBreakContactSourceTargetPairs(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color
  ): Vector[(Square, Square)] =
    current.witnesses.all
      .filter(witness =>
        witness.descriptorId == WitnessDescriptorId("pawn_push_break_contact_source") &&
          witness.color.contains(owner)
      )
      .flatMap: witness =>
        witness.anchor match
          case WitnessAnchor.PieceSquareAnchor(source) =>
            contactTargetSquares(witness).map(source -> _)
          case _ => Vector.empty
      .distinct
      .sortBy((source, target) => (source.value, target.value))

  private def contactTargetSquares(witness: Witness): Vector[Square] =
    witness.payload
      .get("contact_variants")
      .collect:
        case WitnessValue.ListValue(values) =>
          values.flatMap:
            case WitnessValue.ObjectValue(payload) =>
              payload
                .get("target_pawn_squares")
                .collect { case WitnessValue.SquareListValue(squares) => squares }
                .getOrElse(Vector.empty)
            case _ => Vector.empty
      .getOrElse(Vector.empty)
      .distinct
      .sortBy(_.value)

  private def hasAvailableLeverTrigger(
      current: lila.commentary.witness.u.UWitnessExtraction,
      owner: Color,
      source: Square,
      target: Square
  ): Boolean =
    current.witnesses.all.exists(witness =>
      witness.descriptorId == WitnessDescriptorId("available_lever_trigger") &&
        witness.color.contains(owner) &&
        witness.anchor == WitnessAnchor.PieceSquareAnchor(source) &&
        witness.support.targetSquares.contains(target)
    )

  private def hasDefenderMajoritySectorAsymmetry(
      current: lila.commentary.witness.u.UWitnessExtraction,
      sector: WitnessSector,
      defender: Color
  ): Boolean =
    current.witnesses.all.exists(witness =>
      witness.descriptorId == WitnessDescriptorId("sector_asymmetry_state") &&
        witness.anchor == WitnessAnchor.SectorAnchor(sector) &&
        witness.payload.get("majority_side").contains(WitnessValue.ColorValue(defender))
    )

  private def isStructurallyBurdenedTarget(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Boolean =
    context.hasPieceOn(defender, Pawn, target) &&
      !isChainBaseTarget(context, defender, target) &&
      (context.hasColorPawnSquare(lila.commentary.root.RootAtomRegistry.SchemaId.FixedPawn, defender, target) ||
        context.hasColorPawnSquare(lila.commentary.root.RootAtomRegistry.SchemaId.BackwardPawn, defender, target) ||
        context.hasColorPawnSquare(lila.commentary.root.RootAtomRegistry.SchemaId.IsolatedPawn, defender, target))

  private def isChainBaseTarget(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Boolean =
    context.hasPieceOn(defender, Pawn, target) &&
      chainBaseForwardSupportSquares(context, defender, target).nonEmpty &&
      !supportedFromRear(context, defender, target)

  private def isPhalanxEdgeTarget(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Boolean =
    context.hasPieceOn(defender, Pawn, target) &&
      !isChainBaseTarget(context, defender, target) &&
      sameRankAdjacentCount(context, defender, target) == 1

  private def chainBaseForwardSupportSquares(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Vector[Square] =
    if !context.hasPieceOn(defender, Pawn, target) then Vector.empty
    else
      target.pawnAttacks(defender).squares
        .filter(square => context.hasPieceOn(defender, Pawn, square))
        .toVector
        .sortBy(_.value)

  private def isNonCenterFile(square: Square): Boolean =
    !Set(2, 3, 4, 5).contains(square.file.value)

  private def isCenterFile(square: Square): Boolean =
    Set(2, 3, 4, 5).contains(square.file.value)

  private def supportedFromRear(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Boolean =
    context.hasPieceOn(defender, Pawn, target) &&
      target.pawnAttacks(!defender).squares.exists(square => context.hasPieceOn(defender, Pawn, square))

  private def sameRankAdjacentCount(
      context: UExtractionContext,
      defender: Color,
      target: Square
  ): Int =
    Vector(target.file.value - 1, target.file.value + 1)
      .flatMap(File(_))
      .count(file => context.hasPieceOn(defender, Pawn, Square(file, target.rank)))

  private def sectorOf(file: File): WitnessSector =
    file.value match
      case 0 | 1 | 2 => WitnessSector.Queenside
      case 3 | 4 => WitnessSector.Center
      case _ => WitnessSector.Kingside

  private def isWingSector(sector: WitnessSector): Boolean =
    sector != WitnessSector.Center

  private def admitsS17(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val anchors =
      seeds(extraction, StrategySupportSeedScopeContract.S17SamePieceLiabilityAnchor, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }

    anchors.exists: anchor =>
      val liveReliefKinds =
        Vector(
          Option.when(
            hasSeed(extraction, StrategySupportSeedScopeContract.S17SamePieceRepairRoute, owner, anchor)
          )("repair_route"),
          Option.when(
            hasSeed(extraction, StrategySupportSeedScopeContract.S17SamePieceExchangeRelief, owner, anchor)
          )("exchange_relief")
        ).flatten

      liveReliefKinds.exists: reliefKind =>
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S17,
            StrategyProjectionScopeContract.LiabilityReliefCertified,
            owner,
            anchor
          )
          .exists(claim => token(claim.payload, "relief_kind").contains(reliefKind))

  private def admitsS23(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    admitsKingEntry(extraction, evidence, owner) ||
      admitsKingOpposition(extraction, evidence, owner)

  private def admitsKingEntry(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val entrySquares =
      seeds(extraction, StrategySupportSeedScopeContract.S23KingEntrySquare, owner)
        .map(_.anchor)
        .collect { case WitnessAnchor.SquareAnchor(square) => square }

    entrySquares.exists: entrySquare =>
      seeds(extraction, StrategySupportSeedScopeContract.S23KingAccessRoute, owner)
        .exists(seed => square(seed.payload, "entry_square").contains(entrySquare)) &&
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S23,
            StrategyProjectionScopeContract.KingEntryConversionCertified,
            owner,
            WitnessAnchor.SquareAnchor(entrySquare)
          )
          .nonEmpty

  private def admitsKingOpposition(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    seeds(extraction, StrategySupportSeedScopeContract.S23KingOppositionContact, owner)
      .exists(seed =>
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S23,
            StrategyProjectionScopeContract.KingOppositionCertified,
            owner,
            seed.anchor
          )
          .nonEmpty
      )

  private def admitsS24(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    val dependencyAnchors =
      seeds(extraction, StrategySupportSeedScopeContract.S24TargetResourceDependency, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }
        .toSet
    val convergenceAnchors =
      seeds(extraction, StrategySupportSeedScopeContract.S24TargetAttackConvergence, owner)
        .map(_.anchor)
        .collect { case anchor: WitnessAnchor.PieceSquareAnchor => anchor }
        .toSet

    dependencyAnchors.intersect(convergenceAnchors).exists: anchor =>
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S24,
          StrategyProjectionScopeContract.SameTargetForcingRealization,
          owner,
          anchor
        )
        .nonEmpty &&
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S24,
            StrategyProjectionScopeContract.SameTargetConversionCertified,
            owner,
            anchor
          )
          .nonEmpty

  private def admitsS25(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color
  ): Boolean =
    seeds(extraction, StrategySupportSeedScopeContract.S25RankCorridorState, owner)
      .exists: seed =>
        val entrySquare = square(seed.payload, "entry_square")
        val corridorKind = token(seed.payload, "corridor_kind")
        evidence
          .evidenceFor(
            StrategyProjectionScopeContract.S25,
            StrategyProjectionScopeContract.RankAccessConsequenceCertified,
            owner,
            seed.anchor
          )
          .exists: claim =>
            entrySquare.nonEmpty &&
              entrySquare == square(claim.payload, "entry_square") &&
              corridorKind.contains("cross_wing_rank_switch") &&
              corridorKind == token(claim.payload, "corridor_kind")

  private def admitsS22(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      admitsFortressHold(current, evidence, owner, certificationEvidence) ||
        admitsPerpetualHold(current, evidence, owner, certificationEvidence)

  private def admitsS18(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      val bishopSquares = bishopPairMemberSquares(current, owner).toSet
      val activeBishopSquares =
        bishopSquares.filter(_.rank != homeRank(owner))

      activeBishopSquares.nonEmpty &&
        (admitsS18Initiative(current, evidence, owner, certificationEvidence, bishopSquares, activeBishopSquares) ||
          admitsS18Structure(current, evidence, owner, certificationEvidence, bishopSquares, activeBishopSquares) ||
          admitsS18Material(current, evidence, owner, certificationEvidence, bishopSquares, activeBishopSquares))

  private def admitsS18Initiative(
      current: StrategicObjectExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle,
      bishopSquares: Set[Square],
      activeBishopSquares: Set[Square]
  ): Boolean =
    evidence
      .evidenceFor(
        StrategyProjectionScopeContract.S18,
        StrategyProjectionScopeContract.BishopPairInitiativeConversionCertified,
        owner,
        WitnessAnchor.BoardAnchor
      )
      .exists: claim =>
      certifiedClaim(current, certificationEvidence, "InitiativeWindow", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          certification.anchor == WitnessAnchor.BoardAnchor &&
          certification.support.targetSquares.nonEmpty &&
          s18EvidenceBindsCarrierAndTask(
            claim,
            "InitiativeWindow",
            bishopSquares,
            activeBishopSquares,
            certification.support.targetSquares
          )

  private def admitsS18Structure(
      current: StrategicObjectExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle,
      bishopSquares: Set[Square],
      activeBishopSquares: Set[Square]
  ): Boolean =
    evidence
      .evidenceFor(
        StrategyProjectionScopeContract.S18,
        StrategyProjectionScopeContract.BishopPairStructureConversionCertified,
        owner,
        WitnessAnchor.BoardAnchor
      )
      .exists: claim =>
      certifiedClaim(current, certificationEvidence, "MobilityComparison", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          certification.anchor == WitnessAnchor.BoardAnchor &&
          certification.support.targetSquares.nonEmpty &&
          s18EvidenceBindsCarrierAndTask(
            claim,
            "MobilityComparison",
            bishopSquares,
            activeBishopSquares,
            certification.support.targetSquares
          )

  private def admitsS18Material(
      current: StrategicObjectExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle,
      bishopSquares: Set[Square],
      activeBishopSquares: Set[Square]
  ): Boolean =
    certifiedClaim(current, certificationEvidence, "MaterialHarvest", owner).exists: certification =>
      val captureFrom = square(certification.payload, "capture_from")
      val captureTo = square(certification.payload, "capture_to")
      certification.verdict == CertificationVerdict.Certified &&
        certification.anchor == WitnessAnchor.BoardAnchor &&
        role(certification.payload, "capturing_role").contains(Bishop) &&
        captureFrom.exists(activeBishopSquares.contains) &&
        captureTo.exists(certification.support.targetSquares.contains) &&
        captureFrom.exists: from =>
          evidence
            .evidenceFor(
              StrategyProjectionScopeContract.S18,
              StrategyProjectionScopeContract.BishopPairMaterialConversionCertified,
              owner,
              WitnessAnchor.PieceSquareAnchor(from)
            )
            .exists: claim =>
              s18EvidenceBindsCarrierAndTask(
                claim,
                "MaterialHarvest",
                bishopSquares,
                activeBishopSquares,
                certification.support.targetSquares
              )

  private def s18EvidenceBindsCarrierAndTask(
      claim: StrategyProjectionEvidenceClaim,
      certificationFamily: String,
      bishopSquares: Set[Square],
      activeBishopSquares: Set[Square],
      certificationTargetSquares: Vector[Square]
  ): Boolean =
    token(claim.payload, "certification_family").contains(certificationFamily) &&
      sameSquareSet(squareList(claim.payload, "bishop_member_squares"), bishopSquares) &&
      square(claim.payload, "active_bishop_square").exists(activeBishopSquares.contains) &&
      certificationTargetSquares.nonEmpty &&
      sameSquareSet(squareList(claim.payload, "conversion_target_squares"), certificationTargetSquares.toSet)

  private def admitsS19(
      extraction: StrategySupportSeedExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle,
      deltaExtraction: Option[StrategicDeltaExtraction]
  ): Boolean =
    currentObjectExtraction(extraction).exists: current =>
      deltaExtraction.exists: delta =>
        admitsS19MaterialRoute(current, delta, evidence, owner, certificationEvidence) ||
          admitsS19HoldRoute(current, delta, evidence, owner, certificationEvidence)

  private def admitsS19MaterialRoute(
      current: StrategicObjectExtraction,
      delta: StrategicDeltaExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    current.rootState == delta.before.rootState &&
      hasTradeInvariant(delta, owner) &&
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S19,
          StrategyProjectionScopeContract.TradeInvariantMaterialSimplificationCertified,
          owner,
          WitnessAnchor.BoardAnchor
        )
        .nonEmpty &&
      certifiedClaim(current, certificationEvidence, "MaterialHarvest", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          square(certification.payload, "capture_from").contains(delta.playedMove.orig) &&
          square(certification.payload, "capture_to").contains(delta.playedMove.dest) &&
          certification.support.targetSquares.contains(delta.playedMove.dest)

  private def admitsS19HoldRoute(
      current: StrategicObjectExtraction,
      delta: StrategicDeltaExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    current.rootState == delta.after.rootState &&
      hasTradeInvariant(delta, owner) &&
      evidence
        .evidenceFor(
          StrategyProjectionScopeContract.S19,
          StrategyProjectionScopeContract.TradeInvariantHoldSimplificationCertified,
          owner,
          WitnessAnchor.BoardAnchor
        )
        .nonEmpty &&
      certifiedClaim(current, certificationEvidence, "FortressDrawCertification", owner).exists: certification =>
        certification.verdict == CertificationVerdict.Certified &&
          certification.support.targetSquares.contains(delta.playedMove.dest) &&
          fortressShells(current, owner).exists(shell =>
            shell.support.targetSquares.contains(delta.playedMove.dest) &&
              square(certification.payload, "king_square").exists(king =>
                shell.anchor == WitnessAnchor.SquareAnchor(king)
              )
          )

  private def admitsFortressHold(
      current: StrategicObjectExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    fortressShells(current, owner).exists: shell =>
      shell.anchor match
        case WitnessAnchor.SquareAnchor(holderKing) =>
          evidence
            .evidenceFor(
              StrategyProjectionScopeContract.S22,
              StrategyProjectionScopeContract.FortressHoldCertified,
              owner,
              shell.anchor
            )
            .exists(_ =>
              certifiedClaim(current, certificationEvidence, "FortressDrawCertification", owner).exists: certification =>
                certification.verdict == CertificationVerdict.Certified &&
                  square(certification.payload, "king_square").contains(holderKing) &&
                  certification.support.targetSquares.nonEmpty
            )
        case _ => false

  private def admitsPerpetualHold(
      current: StrategicObjectExtraction,
      evidence: StrategyProjectionEvidence,
      owner: Color,
      certificationEvidence: CertificationEvidenceBundle
  ): Boolean =
    evidence
      .evidenceFor(
        StrategyProjectionScopeContract.S22,
        StrategyProjectionScopeContract.PerpetualHoldCertified,
        owner,
        WitnessAnchor.BoardAnchor
      )
      .exists(_ =>
        certifiedClaim(current, certificationEvidence, "PerpetualCheckHolding", owner).exists: certification =>
          certification.verdict == CertificationVerdict.Certified &&
            certification.anchor == WitnessAnchor.BoardAnchor &&
            squareList(certification.payload, "checking_piece_squares").nonEmpty &&
            certification.support.targetSquares.nonEmpty
      )

  private def currentObjectExtraction(
      extraction: StrategySupportSeedExtraction
  ): Option[StrategicObjectExtraction] =
    Try(StrategicObjectExtractor.fromRoot(extraction.rootState)).toOption

  private def validatedDeltaEvidence(
      extraction: StrategySupportSeedExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction]
  ): Either[String, Option[StrategicDeltaExtraction]] =
    deltaExtraction match
      case Some(delta)
          if delta.before.rootState != extraction.rootState &&
            delta.after.rootState != extraction.rootState =>
        Left("Strategy projection admission rejected stale strategic delta evidence")
      case Some(delta) =>
        StrategicDeltaExtractor
          .validateCanonical(delta)
          .left
          .map(message => s"Strategy projection admission rejected non-canonical strategic delta evidence: $message")
          .map(Some(_))
      case None => Right(None)

  private def hasTradeInvariant(delta: StrategicDeltaExtraction, owner: Color): Boolean =
    tradeInvariantDeltas(delta, owner).exists: claim =>
      claim.anchor == WitnessAnchor.BoardAnchor &&
        claim.support.targetSquares.contains(delta.playedMove.dest)

  private def tradeInvariantDeltas(
      delta: StrategicDeltaExtraction,
      owner: Color
  ): Vector[StrategicDelta] =
    delta.deltas.forFamilyId("TradeInvariant").filter(_.color.contains(owner))

  private def fortressShells(
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[StrategicObject] =
    current.objects.forFamilyId("FortressHoldingShell").filter(_.color.contains(owner))

  private def bishopPairMemberSquares(
      current: StrategicObjectExtraction,
      owner: Color
  ): Vector[Square] =
    current.primaryWitnesses
      .forDescriptorId(WitnessDescriptorId("bishop_pair_state"))
      .find(_.color.contains(owner))
      .map(witness => squareList(witness.payload, "bishop_member_squares"))
      .getOrElse(Vector.empty)

  private def homeRank(owner: Color): Rank =
    if owner.white then Rank.First else Rank.Eighth

  private def certifiedClaim(
      current: StrategicObjectExtraction,
      certificationEvidence: CertificationEvidenceBundle,
      familyId: String,
      owner: Color
  ): Option[Certification] =
    CertificationExtractor
      .fromObjectExtractionFailClosed(current, certificationEvidence)
      .toOption
      .flatMap(_.claims.forFamilyId(familyId).find(_.owner.contains(owner)))

  private def hasSeed(
      extraction: StrategySupportSeedExtraction,
      seedId: StrategySupportSeedId,
      owner: Color,
      anchor: WitnessAnchor
  ): Boolean =
    seeds(extraction, seedId, owner).exists(_.anchor == anchor)

  private def seeds(
      extraction: StrategySupportSeedExtraction,
      seedId: StrategySupportSeedId,
      owner: Color
  ): Vector[StrategySupportSeed] =
    extraction.seeds.forSeedId(seedId).filter(_.color.contains(owner))

  private def pieceAnchorSquare(witness: Witness): Option[Square] =
    witness.anchor match
      case WitnessAnchor.PieceSquareAnchor(square) => Some(square)
      case _                                      => None

  private def token(payload: lila.commentary.witness.WitnessPayload, field: String): Option[String] =
    payload.get(field).collect { case WitnessValue.Token(value) => value }

  private def square(payload: lila.commentary.witness.WitnessPayload, field: String): Option[Square] =
    payload.get(field).collect { case WitnessValue.SquareValue(value) => value }

  private def tokenList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[String] =
    payload.get(field).collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)

  private def role(payload: lila.commentary.witness.WitnessPayload, field: String): Option[chess.Role] =
    payload.get(field).collect { case WitnessValue.RoleValue(value) => value }

  private def squareList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  private def sameSquareSet(left: Iterable[Square], right: Set[Square]): Boolean =
    val normalized = left.iterator.toSet
    normalized.nonEmpty && normalized == right
