package lila.commentary.projection

import chess.Color

import lila.commentary.CommentaryCore
import lila.commentary.certification.CertificationEngineRole
import lila.commentary.certification.CertificationEvidenceBundle
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.WitnessAnchor

class StrategyGeometryEngineContractTest extends munit.FunSuite:

  test("geometry centers cover admitted regions and expose stable finite metrics"):
    val s23Regions = StrategyGeometryFoundation.admittedRegionsByBand("S23")

    assertEquals(
      s23Regions.map(_.regionId),
      Vector("A_S23_endgame_entry_or_opposition", "A_S23_line_access_activity")
    )
    assertEquals(
      s23Regions.flatMap(StrategyGeometryEngine.centerFor).map(_.key),
      Vector("mu_S23_endgame_entry_or_opposition", "mu_S23_line_access_activity")
    )
    assertEquals(
      StrategyGeometryEngine.centers.map(_.key).toSet,
      s23Regions.map(_.prototypeKey).toSet
    )
    assertEquals(StrategyGeometryFoundation.GeometryDistance.finite(0.0).valueOption, Some(0.0))
    intercept[IllegalArgumentException](StrategyGeometryFoundation.GeometryDistance.finite(-0.1))
    intercept[IllegalArgumentException](StrategyGeometryFoundation.GeometryDistance.finite(Double.NaN))
    intercept[IllegalArgumentException](StrategyGeometryFoundation.GeometryDistance.finite(Double.PositiveInfinity))

  test("same-root certified truth classifies against concrete mu with finite distance"):
    val current = objectExtraction(kingEntryFen)
    val region = regionById("A_S23_endgame_entry_or_opposition")
    val result =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(current)),
        enabledRegionIds = Vector(region.regionId)
      )

    assertEquals(result.memberships.map(_.region.regionId), Vector(region.regionId))
    assertEquals(result.memberships.map(_.center.key), Vector("mu_S23_endgame_entry_or_opposition"))
    assertEquals(result.memberships.map(_.distance.valueOption), Vector(Some(0.0)))
    assertEquals(result.memberships.map(_.classification), Vector(StrategyGeometryEngine.GeometryClassification.ExactCenter))
    assertEquals(result.memberships.map(_.overlapRole), Vector(StrategyGeometryEngine.OverlapRole.PublicPrimary))
    assertEquals(result.memberships.map(_.rank), Vector(1))

  test("geometry does not treat start-ready Sxx ids as enabled regions and rejects stale or non-certified truth"):
    val current = objectExtraction(kingEntryFen)
    val stale = objectExtraction(afterE4Fen)
    val region = regionById("A_S23_endgame_entry_or_opposition")

    val startReadyAttempt =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(current)),
        enabledRegionIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
      )
    val staleAttempt =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(stale)),
        enabledRegionIds = Vector(region.regionId)
      )
    val supportOnlyAttempt =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(
          s23CertifiedTruth(current, disposition = StrategyGeometryFoundation.PublicPathDisposition.SupportOnly)
        ),
        enabledRegionIds = Vector(region.regionId)
      )

    assertEquals(startReadyAttempt.memberships, Vector.empty)
    assertEquals(staleAttempt.memberships, Vector.empty)
    assertEquals(supportOnlyAttempt.memberships, Vector.empty)

  test("geometry enforces required engine roles and exact transition identity before finite distance"):
    val current = objectExtraction(kingEntryFen)
    val transitionCurrent = objectExtraction(kingEntryTransitionFen)
    val region = regionById("A_S23_endgame_entry_or_opposition")
    val roleMissingTruth =
      StrategyGeometryFoundation.CertifiedTruth.fromTrustedCarriers(
        id = "k-s23-role-missing",
        rootState = Some(current.rootState),
        burden = StrategyGeometryFoundation.ProofBurden(
          family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
          owner = Some(Color.White),
          anchor = WitnessAnchor.BoardAnchor,
          route = "king_entry_conversion",
          scope = "exact_current_board",
          requiredEngineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
          requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
        ),
        engineRoles = Set.empty,
        lowerCarrierRefs = Vector(
          StrategyProjectionCarrierRef(
            kind = StrategyProjectionCarrierKind.ExactBoard,
            id = "k-s23-exact-board-carrier",
            owner = "white",
            anchor = "board",
            route = "king_entry_conversion",
            scope = "exact_current_board"
          )
        )
      )
    val roleMissing =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = roleMissingTruth.toVector,
        enabledRegionIds = Vector(region.regionId)
      )
    val transitionMissing =
      val missingTruth =
        maybeS23CertifiedTruth(
          transitionCurrent,
          id = "k-s23-transition-missing",
          scope = "exact_transition",
          requiresExactTransition = true
        )
      StrategyGeometryEngine.classify(
        currentRootState = transitionCurrent.rootState,
        certifiedTruths = missingTruth.toVector,
        enabledRegionIds = Vector(region.regionId)
      )
    val transitionBound =
      StrategyGeometryEngine.classify(
        currentRootState = transitionCurrent.rootState,
        certifiedTruths = Vector(
          s23CertifiedTruth(
            transitionCurrent,
            id = "k-s23-transition-bound",
            scope = "exact_transition",
            requiresExactTransition = true,
            exactTransitionIdentity = Some(
              StrategyGeometryFoundation.ExactTransitionIdentity(
                beforeFen = kingEntryBeforeFen,
                playedMove = "f3f4",
                currentFen = kingEntryTransitionFen,
                nodeId = "p3-transition-node",
                ply = 1,
                variant = "standard"
              )
            )
          )
        ),
        enabledRegionIds = Vector(region.regionId)
      )

    assertEquals(roleMissingTruth, None)
    assertEquals(roleMissing.memberships, Vector.empty)
    assertEquals(transitionMissing.memberships, Vector.empty)
    assertEquals(transitionBound.memberships.map(_.distance.valueOption), Vector(Some(0.05)))

  test("geometry rejects unbound lower carriers before finite distance"):
    val current = objectExtraction(kingEntryFen)
    val region = regionById("A_S23_endgame_entry_or_opposition")
    val invalidTruth =
      maybeS23CertifiedTruth(
        current,
        id = "k-s23-unbound-carrier",
        lowerCarrierRoute = Some("king_opposition")
      )
    val result =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = invalidTruth.toVector,
        enabledRegionIds = Vector(region.regionId)
      )

    assertEquals(invalidTruth, None)
    assertEquals(result.memberships, Vector.empty)

  test("entry and opposition routes classify while centralization-only near miss stays outside"):
    val entry = objectExtraction(kingEntryFen)
    val opposition = objectExtraction(kingOppositionFen)
    val region = regionById("A_S23_endgame_entry_or_opposition")
    val entryResult =
      StrategyGeometryEngine.classify(
        currentRootState = entry.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(entry, route = "king_entry_conversion")),
        enabledRegionIds = Vector(region.regionId)
      )
    val oppositionResult =
      StrategyGeometryEngine.classify(
        currentRootState = opposition.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(opposition, route = "king_opposition")),
        enabledRegionIds = Vector(region.regionId)
      )
    val nearMiss =
      StrategyGeometryEngine.classify(
        currentRootState = entry.rootState,
        certifiedTruths = Vector(s23CertifiedTruth(entry, route = "king_centralization")),
        enabledRegionIds = Vector(region.regionId)
      )

    assertEquals(entryResult.memberships.map(_.distance.valueOption), Vector(Some(0.0)))
    assertEquals(oppositionResult.memberships.map(_.distance.valueOption), Vector(Some(0.05)))
    assertEquals(nearMiss.memberships, Vector.empty)

  test("overlap resolves by finite distance before producer lowers public admissions"):
    val current = objectExtraction(kingEntryFen)
    val broad = regionById("A_S23_endgame_entry_or_opposition")
    val focused = regionById("A_S23_line_access_activity")
    val truth = s23CertifiedTruth(
      current,
      id = "k-s23-line-access",
      family = StrategyGeometryFoundation.CertifiedFactFamily.LineAccessActivity
    )
    val candidate = s23RuntimeKCandidate(
      id = "k-s23-line-access",
      regionId = focused.regionId,
      family = StrategyGeometryFoundation.CertifiedFactFamily.LineAccessActivity
    )
    val enabled = Vector(broad.regionId, focused.regionId)
    val result =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(truth),
        enabledRegionIds = enabled
      )
    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(candidate)
        ),
        enabled
      )

    assertEquals(result.memberships.map(_.region.regionId), Vector(focused.regionId, broad.regionId))
    assertEquals(
      result.memberships.map(_.overlapRole),
      Vector(StrategyGeometryEngine.OverlapRole.PublicPrimary, StrategyGeometryEngine.OverlapRole.SupportMembership)
    )
    assert(result.memberships.head.distance.valueOption.exists(_ < result.memberships(1).distance.valueOption.getOrElse(1.0)))
    assertEquals(result.memberships.map(_.rank), Vector(1, 2))
    assertEquals(admissions.map(_.bandId.value), Vector("S23"))
    assertEquals(admissions.map(_.runtimeKId), Vector(Some("k-s23-line-access")))
    assertEquals(admissions.flatMap(_.lowerCarrierRefs.map(_.id)), Vector("k-s23-exact-board-carrier"))

  test("broad region line-access membership is support-only unless the focused region is enabled"):
    val current = objectExtraction(kingEntryFen)
    val broad = regionById("A_S23_endgame_entry_or_opposition")
    val truth = s23CertifiedTruth(
      current,
      id = "k-s23-line-access-broad-only",
      family = StrategyGeometryFoundation.CertifiedFactFamily.LineAccessActivity
    )
    val candidate = s23RuntimeKCandidate(
      id = "k-s23-line-access-broad-only",
      regionId = broad.regionId,
      family = StrategyGeometryFoundation.CertifiedFactFamily.LineAccessActivity
    )
    val result =
      StrategyGeometryEngine.classify(
        currentRootState = current.rootState,
        certifiedTruths = Vector(truth),
        enabledRegionIds = Vector(broad.regionId)
      )
    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(candidate)
        ),
        Vector(broad.regionId)
      )

    assertEquals(result.memberships.map(_.overlapRole), Vector(StrategyGeometryEngine.OverlapRole.SupportMembership))
    assertEquals(result.publicPrimaryMemberships, Vector.empty)
    assertEquals(admissions, Vector.empty)

  test("producer route evidence mapping is exact and rejects opposition-shaped route substrings"):
    val current = objectExtraction(kingEntryFen)
    val region = regionById("A_S23_endgame_entry_or_opposition")
    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(s23RuntimeKCandidate("k-s23-route-substring-negative", route = "not_king_opposition"))
        ),
        Vector(region.regionId)
      )

    assertEquals(admissions, Vector.empty)

  private val kingEntryFen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"
  private val kingEntryBeforeFen = "6k1/8/8/3p4/8/5K2/8/8 w - - 0 1"
  private val kingEntryTransitionFen = "6k1/8/8/3p4/5K2/8/8/8 b - - 1 1"
  private val kingOppositionFen = "8/8/4k3/8/4K3/8/8/8 b - - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"

  private def objectExtraction(fen: String): StrategicObjectExtraction =
    CommentaryCore.extractStrategicObjectsFromFenFailClosed(fen).fold(fail(_), identity)

  private def regionById(regionId: String): StrategyGeometryFoundation.AdmittedRegion =
    StrategyGeometryFoundation.admittedRegionsByBand.values.flatten.find(_.regionId == regionId).getOrElse:
      fail(s"missing region $regionId")

  private def s23CertifiedTruth(
      current: StrategicObjectExtraction,
      id: String = "k-s23-white-entry",
      family: StrategyGeometryFoundation.CertifiedFactFamily =
        StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
      route: String = "king_entry_conversion",
      scope: String = "exact_current_board",
      requiredEngineRoles: Set[CertificationEngineRole] = Set.empty,
      requiresExactTransition: Boolean = false,
      exactTransitionIdentity: Option[StrategyGeometryFoundation.ExactTransitionIdentity] = None,
      lowerCarrierRoute: Option[String] = None,
      disposition: StrategyGeometryFoundation.PublicPathDisposition = StrategyGeometryFoundation.PublicPathDisposition.Certified
  ): StrategyGeometryFoundation.CertifiedTruth =
    maybeS23CertifiedTruth(
      current,
      id,
      family,
      route,
      scope,
      requiredEngineRoles,
      requiresExactTransition,
      exactTransitionIdentity,
      lowerCarrierRoute,
      disposition
    ).getOrElse(fail(s"failed to mint test CertifiedTruth $id"))

  private def maybeS23CertifiedTruth(
      current: StrategicObjectExtraction,
      id: String,
      family: StrategyGeometryFoundation.CertifiedFactFamily =
        StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
      route: String = "king_entry_conversion",
      scope: String = "exact_current_board",
      requiredEngineRoles: Set[CertificationEngineRole] = Set.empty,
      requiresExactTransition: Boolean = false,
      exactTransitionIdentity: Option[StrategyGeometryFoundation.ExactTransitionIdentity] = None,
      lowerCarrierRoute: Option[String] = None,
      disposition: StrategyGeometryFoundation.PublicPathDisposition = StrategyGeometryFoundation.PublicPathDisposition.Certified
  ): Option[StrategyGeometryFoundation.CertifiedTruth] =
    val burden =
      StrategyGeometryFoundation.ProofBurden(
        family = family,
        owner = Some(Color.White),
        anchor = WitnessAnchor.BoardAnchor,
        route = route,
        scope = scope,
        requiredEngineRoles = requiredEngineRoles,
        requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard),
        requiresExactTransition = requiresExactTransition,
        disposition = disposition
      )
    StrategyGeometryFoundation.CertifiedTruth.fromTrustedCarriers(
      id = id,
      rootState = Some(current.rootState),
      burden = burden,
      engineRoles = Set.empty,
      exactTransitionIdentity = exactTransitionIdentity,
      projectionEvidenceKinds = evidenceKindsForRoute(route),
      lowerCarrierRefs = Vector(
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.ExactBoard,
          id = "k-s23-exact-board-carrier",
          owner = "white",
          anchor = "board",
          route = lowerCarrierRoute.getOrElse(route),
          scope = scope,
          binding = bindingForRoute(route)
        )
      )
    )

  private def s23RuntimeKCandidate(
      id: String,
      regionId: String = "A_S23_endgame_entry_or_opposition",
      family: StrategyGeometryFoundation.CertifiedFactFamily =
        StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
      route: String = "king_entry_conversion",
      scope: String = "exact_current_board"
  ): StrategyRuntimeKProducer.Candidate =
    StrategyRuntimeKProducer.Candidate(
      id = id,
      regionId = regionId,
      family = family,
      owner = Color.White,
      anchor = WitnessAnchor.BoardAnchor,
      route = route,
      scope = scope,
      evidenceKinds = evidenceKindsForRoute(route),
      lowerCarrierRefs = Vector(
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.ExactBoard,
          id = "k-s23-exact-board-carrier",
          owner = "white",
          anchor = "board",
          route = route,
          scope = scope,
          binding = bindingForRoute(route)
        )
      )
    )

  private val s23Binding: Map[String, String] =
    Map("entry_square" -> "e5", "contact_square" -> "d5")

  private def bindingForRoute(route: String): Map[String, String] =
    route match
      case "king_opposition" => Map("entry_square" -> "e5", "contact_square" -> "e5")
      case _ => s23Binding

  private def evidenceKindsForRoute(route: String): Vector[StrategyProjectionEvidenceKind] =
    route match
      case "king_opposition" => Vector(StrategyProjectionScopeContract.KingOppositionCertified)
      case _ => Vector(StrategyProjectionScopeContract.KingEntryConversionCertified)
