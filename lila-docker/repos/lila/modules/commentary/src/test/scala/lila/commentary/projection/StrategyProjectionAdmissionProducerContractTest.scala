package lila.commentary.projection

import chess.Color

import lila.commentary.CommentaryCore
import lila.commentary.api.{ CommentaryBackendSeam, CommentaryRequest, CommentaryResponseStatus }
import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole, CertificationEvidenceBundle }
import lila.commentary.claim.{ EvidenceClaimHandoff, EvidenceClaimProducer }
import lila.commentary.render.{ CommentaryRendererContract, PublicClaimPredicate, RenderRole }
import lila.commentary.selection.{ ClaimLayer, ClaimSelector, CommentaryOutlineBuilder, EvidenceRefKind }
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.WitnessAnchor

class StrategyProjectionAdmissionProducerContractTest extends munit.FunSuite:

  test("default projection-admission producer is wired but opens no broad Sxx surface"):
    val current = objectExtraction(kingEntryFen)
    val input =
      StrategyProjectionAdmissionProducer.Input(
        currentExtraction = current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty
      )
    val response = CommentaryBackendSeam.render(request(centerReleaseFen))

    assertEquals(StrategyProjectionAdmissionProducer.defaultEnabledRegionIds, Vector.empty)
    assertEquals(StrategyProjectionAdmissionProducer.produce(input), Vector.empty)
    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(!response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Projection))

  test("producer enablement is region based and separate from Sxx start-ready coverage"):
    val startReadyIds = StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet
    val enabledIds = StrategyProjectionAdmissionProducer.defaultEnabledRegionIds.toSet
    val s23Region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head

    assert(startReadyIds.contains("S23"))
    assertEquals(enabledIds.intersect(startReadyIds), Set.empty[String])
    assertEquals(s23Region.bandId.value, "S23")
    assertEquals(s23Region.enabledForDefaultProducer, false)

  test("S24 has no production descriptor region and cannot be opened by band id"):
    val current = objectExtraction(kingEntryFen)
    val input =
      StrategyProjectionAdmissionProducer.Input(
        currentExtraction = current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty,
        runtimeKCandidates = Vector(
          s23CertifiedTruthCandidate("k-s24-band-id-attempt").copy(
            regionId = StrategyProjectionScopeContract.S24.value
          )
        )
      )

    assertEquals(StrategyGeometryFoundation.admittedRegionsByBand("S24"), Vector.empty)
    assertEquals(
      StrategyProjectionAdmissionProducer.produce(input, Vector(StrategyProjectionScopeContract.S24.value)),
      Vector.empty
    )

  test("P2 producer opens a region only from descriptor-minted runtime K"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val input =
      StrategyProjectionAdmissionProducer.Input(
        currentExtraction = current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty,
        runtimeKCandidates = Vector(s23CertifiedTruthCandidate("k-s23-runtime-open-region"))
      )

    val startReadyAttempt =
      StrategyProjectionAdmissionProducer.produce(
        input,
        StrategyProjectionScopeContract.startReadyBandIds.map(_.value)
      )
    val regionAdmission =
      StrategyProjectionAdmissionProducer.produce(input, Vector(region.regionId))

    assertEquals(startReadyAttempt, Vector.empty)
    assertEquals(regionAdmission.map(_.bandId.value), Vector("S23"))
    assertEquals(regionAdmission.forall(_.admitted), true)
    assertEquals(regionAdmission.map(_.runtimeKId), Vector(Some("k-s23-runtime-open-region")))
    assertEquals(regionAdmission.flatMap(_.lowerCarrierRefs.map(_.id)), Vector("k-s23-exact-board-carrier"))
    assertEquals(regionAdmission.exists(admission => forbiddenWords.exists(token => admission.projectionId.toLowerCase.contains(token))), false)

  test("P2 K-backed S23 admission can lower to public projection surface"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val admission =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(s23CertifiedTruthCandidate("k-s23-runtime-public-surface"))
        ),
        Vector(region.regionId)
      )
    val claims =
      EvidenceClaimProducer.produce(
        current,
        None,
        EvidenceClaimHandoff(projectionAdmissions = admission)
      )
    val outline = ClaimSelector.select(claims.filter(_.layer == ClaimLayer.Projection))
    val publicClaims = CommentaryRendererContract.publicClaims(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead.map(_.claim.layer), Some(ClaimLayer.Projection))
    assertEquals(outline.lead.map(_.claim.band), Some(Some("S23")))
    assertEquals(publicClaims.map(_.role), Vector(RenderRole.Primary))
    assert(publicClaims.head.phraseCapability.allowedPredicates.contains(PublicClaimPredicate.StrategyProjection))
    assertEquals(publicClaims.head.phraseCapability.allowsLineCommentary, false)
    assert(publicClaims.head.phraseCapability.forbiddenTerms.contains("engine"))

  test("P2 producer rejects non-public-disposition runtime K before projection surface"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val blockedCandidates =
      Vector(
        s23CertifiedTruthCandidate("k-s23-runtime-support-only").copy(disposition = StrategyGeometryFoundation.PublicPathDisposition.SupportOnly),
        s23CertifiedTruthCandidate("k-s23-runtime-deferred").copy(disposition = StrategyGeometryFoundation.PublicPathDisposition.Deferred),
        s23CertifiedTruthCandidate("k-s23-runtime-anti-case").copy(disposition = StrategyGeometryFoundation.PublicPathDisposition.AntiCase)
      )
    val input =
      StrategyProjectionAdmissionProducer.Input(
        currentExtraction = current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty,
        runtimeKCandidates = blockedCandidates
      )

    assertEquals(StrategyProjectionAdmissionProducer.produce(input, Vector(region.regionId)), Vector.empty)

  test("producer rejects engine-role K that has no bound certification carrier"):
    val current = objectExtraction(kingEntryFen)
    val roleBoundTruth =
      StrategyGeometryFoundation.CertifiedTruth
        .fromTrustedCarriers(
          id = "k-s23-engine-role-without-certification-carrier",
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
          engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
          lowerCarrierRefs = Vector(
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.ExactBoard,
              id = "k-s23-exact-board-carrier",
              owner = "white",
              anchor = "board",
              route = "king_entry_conversion",
              scope = "exact_current_board",
              binding = s23Binding
            )
          )
        )

    assertEquals(roleBoundTruth, None)

  test("producer admits only K minted by the runtime K producer"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val input =
      StrategyProjectionAdmissionProducer.Input(
        currentExtraction = current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty,
        runtimeKCandidates = Vector(s23CertifiedTruthCandidate("k-s23-factory-minted"))
      )

    val admissions = StrategyProjectionAdmissionProducer.produce(input, Vector(region.regionId))

    assertEquals(admissions.map(_.bandId.value), Vector("S23"))
    assertEquals(admissions.map(_.runtimeKId), Vector(Some("k-s23-factory-minted")))

  test("descriptor-certified runtime admission cannot be forged without runtime K provenance"):
    val current = objectExtraction(kingEntryFen)
    val forged =
      StrategyProjectionAdmissionResult.fromDecision(
        projectionId = "projection-s23-white-board-king-entry-conversion",
        authority = StrategyProjectionAdmissionAuthority.DescriptorCertifiedRuntime,
        bandId = StrategyProjectionScopeContract.S23,
        sourceRootState = current.rootState,
        currentRootState = current.rootState,
        evidenceKinds = Vector(StrategyProjectionScopeContract.KingEntryConversionCertified),
        owner = Color.White,
        beneficiary = Some(Color.White),
        defender = Some(Color.Black),
        anchor = WitnessAnchor.BoardAnchor,
        route = "king_entry_conversion",
        scope = "exact_current_board",
        lowerCarrierRefs = s23CertifiedTruthCandidate("k-s23-forged-admission").lowerCarrierRefs,
        wordingStrengthCap = lila.commentary.selection.WordingStrength.QualifiedSupport,
        decision = Right(true)
      )

    assertEquals(forged.admitted, false)
    assertEquals(forged.rejectionReason, Some("projection_runtime_k_required"))

  test("runtime K producer mints only descriptor-bound unfalsified candidates"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val validCandidate = s23CertifiedTruthCandidate("k-s23-runtime-candidate")
    val falsifiedCandidate =
      validCandidate.copy(id = "k-s23-runtime-falsified", falsificationReasons = Set("sibling_band_rival"))
    val wrongRegionCandidate =
      validCandidate.copy(id = "k-s23-runtime-wrong-region", regionId = "A_S23_missing_region")
    val minted =
      StrategyRuntimeKProducer.produce(
        StrategyRuntimeKProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          candidates = Vector(validCandidate, falsifiedCandidate, wrongRegionCandidate)
        )
      )
    val descriptor = StrategyGeometryFoundation.sliceDescriptorsByRegionId(region.regionId)

    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(validCandidate, falsifiedCandidate, wrongRegionCandidate)
        ),
        Vector(region.regionId)
      )

    assertEquals(minted.map(_.truth.id), Vector("k-s23-runtime-candidate"))
    assertEquals(minted.map(_.regionId), Vector(region.regionId))
    assertEquals(minted.forall(_.truth.canEnterGeometry), true)
    assertEquals(descriptor.descriptorBurdenSatisfiedBy(minted.head.truth), true)
    assertEquals(descriptor.carrierBindingSatisfiedBy(minted.head.truth), true)
    assertEquals(descriptor.exactBoardEvidenceSatisfiedBy(current.rootState, minted.head.truth), true)
    assertEquals(admissions.map(_.bandId.value), Vector("S23"))
    assertEquals(admissions.map(_.runtimeKId), Vector(Some("k-s23-runtime-candidate")))
    assertEquals(admissions.flatMap(_.lowerCarrierRefs.map(_.id)), Vector("k-s23-exact-board-carrier"))

  test("runtime K producer enforces descriptor-owned burden and binding laws"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val validCandidate = s23CertifiedTruthCandidate("k-s23-runtime-valid")
    val wrongEvidenceKindCandidate =
      validCandidate.copy(
        id = "k-s23-runtime-wrong-evidence-kind",
        evidenceKinds = Vector(StrategyProjectionScopeContract.KingOppositionCertified)
      )
    val wrongAnchorCandidate =
      validCandidate.copy(
        id = "k-s23-runtime-wrong-anchor",
        anchor = WitnessAnchor.SquareAnchor(square("e4"))
      )
    val weakenedCarrierCandidate =
      validCandidate.copy(
        id = "k-s23-runtime-weakened-carrier",
        lowerCarrierRefs = validCandidate.lowerCarrierRefs.map(_.copy(kind = StrategyProjectionCarrierKind.Root))
      )
    val missingBindingCandidate =
      validCandidate.copy(
        id = "k-s23-runtime-missing-binding",
        lowerCarrierRefs = validCandidate.lowerCarrierRefs.map(_.copy(binding = Map("entry_square" -> "e4")))
      )
    val transitionWithoutTau =
      validCandidate.copy(
        id = "k-s23-runtime-transition-without-tau",
        scope = "exact_transition",
        lowerCarrierRefs = validCandidate.lowerCarrierRefs.map(_.copy(scope = "exact_transition"))
      )
    val candidates =
      Vector(
        validCandidate,
        wrongEvidenceKindCandidate,
        wrongAnchorCandidate,
        weakenedCarrierCandidate,
        missingBindingCandidate,
        transitionWithoutTau
      )
    val minted =
      StrategyRuntimeKProducer.produce(
        StrategyRuntimeKProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          candidates = candidates
        )
      )

    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = candidates
        ),
        Vector(region.regionId)
      )

    assertEquals(minted.map(_.truth.id), Vector("k-s23-runtime-valid"))
    assertEquals(admissions.map(_.projectionId), Vector("projection-s23-white-board-king-entry-conversion"))

  test("descriptor K path rejects synthetic exact-board claims and injected weak burdens"):
    val current = objectExtraction(kingEntryFen)
    val falseBoard = objectExtraction(centerReleaseFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val falseBoardCandidate =
      s23CertifiedTruthCandidate("k-s23-runtime-false-board")
    val noProjectionEvidenceCandidate =
      s23CertifiedTruthCandidate("k-s23-injected-no-projection-evidence").copy(evidenceKinds = Vector.empty)
    val weakCarrierCandidate =
      s23CertifiedTruthCandidate("k-s23-injected-weak-burden").copy(
        lowerCarrierRefs = Vector(
          StrategyProjectionCarrierRef(
            kind = StrategyProjectionCarrierKind.Root,
            id = "k-s23-root-carrier",
            owner = "white",
            anchor = "board",
            route = "king_entry_conversion",
            scope = "exact_current_board",
            binding = s23Binding
          )
        )
      )

    val falseBoardAdmissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = falseBoard,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(falseBoardCandidate)
        ),
        Vector(region.regionId)
      )
    val injectedAdmissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(noProjectionEvidenceCandidate, weakCarrierCandidate)
        ),
        Vector(region.regionId)
      )

    assertEquals(falseBoardAdmissions, Vector.empty)
    assertEquals(injectedAdmissions, Vector.empty)

  test("runtime K producer binds engine proof identity to the certification carrier"):
    val current = objectExtraction(kingEntryFen)
    val region = StrategyGeometryFoundation.admittedRegionsByBand("S23").head
    val validCandidate =
      s23CertifiedTruthCandidate("k-s23-runtime-engine-valid").copy(
        engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
        engineProofIdentity = Some(engineProofIdentity("k-s23-certification-carrier")),
        lowerCarrierRefs =
          s23CertifiedTruthCandidate("tmp").lowerCarrierRefs :+
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.Certification,
              id = "k-s23-certification-carrier",
              owner = "white",
              anchor = "board",
              route = "king_entry_conversion",
              scope = "exact_current_board",
              binding = s23Binding
            )
      )
    val mismatchedIdentity =
      validCandidate.copy(
        id = "k-s23-runtime-engine-mismatched",
        engineProofIdentity = Some(engineProofIdentity("other-certification-carrier"))
      )
    val forgedRequestIdentity =
      validCandidate.copy(
        id = "k-s23-runtime-engine-forged-q",
        engineProofIdentity = Some(
          engineProofIdentity("k-s23-certification-carrier")
            .copy(probeRequestId = Some("q-best-defense-survival-forged-family-white-board-king-entry-conversion"))
        )
      )

    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(validCandidate, mismatchedIdentity, forgedRequestIdentity)
        ),
        Vector(region.regionId)
      )

    assertEquals(admissions.map(_.projectionId), Vector("projection-s23-white-board-king-entry-conversion"))

  test("runtime K producer covers every live descriptor candidate path"):
    val current = objectExtraction(kingEntryFen)
    val lineRegion = StrategyGeometryFoundation.sliceDescriptorsByRegionId("A_S23_line_access_activity").region
    val lineCandidate =
      s23CertifiedTruthCandidate("k-s23-runtime-line-access").copy(
        regionId = lineRegion.regionId,
        family = StrategyGeometryFoundation.CertifiedFactFamily.LineAccessActivity
      )

    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(lineCandidate)
        ),
        Vector(lineRegion.regionId)
      )

    assertEquals(admissions.map(_.projectionId), Vector("projection-s23-white-board-king-entry-conversion"))

  private val centerReleaseFen = "6k1/6pp/5n2/3pp3/3PP3/5N2/2P3PP/6K1 w - - 0 1"
  private val kingEntryFen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"
  private val forbiddenWords: Vector[String] =
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend", "engine")
  private val s23Binding: Map[String, String] =
    Map("entry_square" -> "e5", "contact_square" -> "d5")

  private def objectExtraction(fen: String): StrategicObjectExtraction =
    CommentaryCore.extractStrategicObjectsFromFenFailClosed(fen).fold(fail(_), identity)

  private def s23CertifiedTruthCandidate(id: String): StrategyRuntimeKProducer.Candidate =
    StrategyRuntimeKProducer.Candidate(
      id = id,
      regionId = "A_S23_endgame_entry_or_opposition",
      family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
      owner = Color.White,
      anchor = WitnessAnchor.BoardAnchor,
      route = "king_entry_conversion",
      scope = "exact_current_board",
      evidenceKinds = Vector(StrategyProjectionScopeContract.KingEntryConversionCertified),
      lowerCarrierRefs = Vector(
        StrategyProjectionCarrierRef(
          kind = StrategyProjectionCarrierKind.ExactBoard,
          id = "k-s23-exact-board-carrier",
          owner = "white",
          anchor = "board",
          route = "king_entry_conversion",
          scope = "exact_current_board",
          binding = s23Binding
        )
      )
    )

  private def square(key: String): chess.Square =
    chess.Square.fromKey(key).getOrElse(fail(s"bad square $key"))

  private def engineProofIdentity(certificationCarrierId: String): StrategyGeometryFoundation.EngineProofIdentity =
    StrategyGeometryFoundation.EngineProofIdentity(
      probeRequestId = Some("q-best-defense-survival-endgame-conversion-holding-white-board-king-entry-conversion"),
      probePolicyFingerprint = Some(
        CertificationEnginePolicyFingerprint.defaultForRole(
          "default-strategy-probe-engine",
          CertificationEngineRole.BestDefenseSurvival
        )
      ),
      engineConfigFingerprint = Some("default-strategy-probe-engine"),
      targetDepth = Some(18),
      floorDepth = Some(16),
      multiPv = Some(3),
      certificationEvidenceId = Some(certificationCarrierId),
      satisfiedRoleInvariants = Set(CertificationEngineRole.BestDefenseSurvival),
      semanticCoverageRoles = Set(CertificationEngineRole.BestDefenseSurvival)
    )

  private def request(currentFen: String): CommentaryRequest =
    CommentaryRequest(
      currentFen = currentFen,
      beforeFen = None,
      playedMove = None,
      nodeId = "projection-producer-node",
      ply = 0
    )
