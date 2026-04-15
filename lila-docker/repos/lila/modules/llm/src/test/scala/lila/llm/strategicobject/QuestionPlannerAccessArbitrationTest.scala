package lila.llm.strategicobject

import chess.{ Color, File, Square }
import munit.FunSuite

class QuestionPlannerAccessArbitrationTest extends FunSuite:

  private val visibleMoveContract =
    PrimitiveExtractionTest.moveTransitionVisibleContractFor("d4e5")

  test("planner demotes overlapping access-network ownership behind a more specific certified causal claim") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-overlap",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-overlap",
        objectId = "CounterplayAxis-white-e5",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.C4, Square.E5),
          breakSquares = List(Square.C4, Square.E5),
          pressureSquares = List(Square.C4, Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.C4, Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.C4),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      )

    assertEquals(
      AccessNetworkBridgeAdmissionBoundary.assess(accessClaim, counterplayClaim),
      AccessNetworkBridgeAdmissionAssessment(
        traceAdmittedSlice = true,
        routeWitnessRetained = true,
        contestedTargetRetained = true
      )
    )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assertEquals(planned.axis, QuestionAxis.WhyThis)
    assertEquals(planned.claimIds, List(counterplayClaim.id))
    assertEquals(planned.supportClaimIds, List(accessClaim.id))
  }

  test("support-only counterplay residual can demote access-network primary into support") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-support-demotion",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-support",
        objectId = "CounterplayAxis-white-e5",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.C4, Square.E5),
          breakSquares = List(Square.C4, Square.E5),
          pressureSquares = List(Square.C4, Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.C4, Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.C4),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        ),
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assertEquals(planned.axis, QuestionAxis.WhyThis)
    assertEquals(planned.claimIds, Nil)
    assert(planned.supportClaimIds.contains(accessClaim.id), clue(planned))
    assert(planned.supportClaimIds.contains(counterplayClaim.id), clue(planned))
  }

  test("planner keeps access-network primary when residual retains contested target without route witness") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-target-only",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-target-only",
        objectId = "CounterplayAxis-white-e5",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.E5),
          breakSquares = List(Square.E5),
          pressureSquares = List(Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      )

    assertEquals(
      AccessNetworkBridgeAdmissionBoundary.assess(accessClaim, counterplayClaim),
      AccessNetworkBridgeAdmissionAssessment(
        traceAdmittedSlice = true,
        routeWitnessRetained = false,
        contestedTargetRetained = true
      )
    )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.claimIds.contains(counterplayClaim.id), clue(planned))
    assertEquals(planned.supportClaimIds, Nil)
  }

  test("planner keeps access-network primary when residual retains route witness without contested target") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-route-only",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-route-only",
        objectId = "CounterplayAxis-white-c4",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.C4),
          breakSquares = List(Square.C4),
          pressureSquares = List(Square.C4),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.C4),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.C4),
            contestedSquares = List(Square.C4),
            lane = Some(File.C)
          )
        )
      )

    assertEquals(
      AccessNetworkBridgeAdmissionBoundary.assess(accessClaim, counterplayClaim),
      AccessNetworkBridgeAdmissionAssessment(
        traceAdmittedSlice = true,
        routeWitnessRetained = true,
        contestedTargetRetained = false
      )
    )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.claimIds.contains(counterplayClaim.id), clue(planned))
    assertEquals(planned.supportClaimIds, Nil)
  }

  test("planner keeps access-network primary when no specific residual overlap is proven") {
    val accessClaim = mkAccessClaim("access-disjoint", Square.H7, File.H)
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-disjoint",
        objectId = "CounterplayAxis-white-e5",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.E5),
          breakSquares = List(Square.E5),
          pressureSquares = List(Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assertEquals(planned.axis, QuestionAxis.WhyThis)
    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.claimIds.contains(counterplayClaim.id), clue(planned))
    assertEquals(planned.supportClaimIds, Nil)
  }

  test("support-only trade invariant does not demote access-network primary") {
    val accessClaim = mkAccessClaim("access-trade-support", Square.E5, File.E)
    val tradeClaim =
      moveLocalClaim(
        id = "trade-support",
        objectId = "TradeInvariant-white-e5",
        family = StrategicObjectFamily.TradeInvariant,
        profile = StrategicObjectProfile.TradeInvariant(
          exchangeSquares = List(Square.E5),
          invariantSquares = List(Square.E5, Square.D6),
          preservedFiles = Set(File.E, File.D),
          preservedFamilies = Set(StrategicObjectFamily.FixedTargetComplex),
          features = Set(TradeInvariantFeature.FixedTargetAnchor)
        ),
        primaryTag = StrategicDeltaTag.TradePreserved,
        axis = StrategicMoveTransitionAxis.BreakActivation,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.ExchangeSquare,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        ),
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, tradeClaim))

    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.supportClaimIds.contains(tradeClaim.id), clue(planned))
  }

  test("non-primary trade invariant semantics do not demote access-network primary even if a certified residual claim is present") {
    val accessClaim = mkAccessClaim("access-trade-non-primary", Square.E5, File.E)
    val tradeClaim =
      moveLocalClaim(
        id = "trade-certified-non-primary",
        objectId = "TradeInvariant-white-e5",
        family = StrategicObjectFamily.TradeInvariant,
        profile = StrategicObjectProfile.TradeInvariant(
          exchangeSquares = List(Square.E5),
          invariantSquares = List(Square.E5, Square.D6),
          preservedFiles = Set(File.E),
          preservedFamilies = Set(StrategicObjectFamily.AccessNetwork),
          features = Set(TradeInvariantFeature.AccessAnchor)
        ),
        primaryTag = StrategicDeltaTag.TradePreserved,
        axis = StrategicMoveTransitionAxis.TradeSimplification,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.ExchangeSquare,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, tradeClaim))

    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.claimIds.contains(tradeClaim.id), clue(planned))
    assertEquals(planned.supportClaimIds, Nil)
  }

  test("support-only plan-race residual can demote access-network primary into support") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-plan-race",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val planRaceClaim =
      moveLocalClaim(
        id = "plan-race-support",
        objectId = "PlanRace-white-e5",
        family = StrategicObjectFamily.PlanRace,
        profile = StrategicObjectProfile.PlanRace(
          rivalOwner = Color.Black,
          raceSquares = List(Square.C4, Square.E5, Square.E6),
          raceFiles = Set(File.E),
          ownGoalSquares = List(Square.E6),
          rivalGoalSquares = List(Square.E4),
          features = Set(PlanRaceFeature.BilateralCounterplay)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.C4, Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.C4),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        ),
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, planRaceClaim))

    assertEquals(planned.claimIds, Nil)
    assert(planned.supportClaimIds.contains(accessClaim.id), clue(planned))
    assert(planned.supportClaimIds.contains(planRaceClaim.id), clue(planned))
  }

  test("support-only conversion funnel does not demote access-network primary") {
    val accessClaim = mkAccessClaim("access-conversion", Square.E5, File.E)
    val conversionClaim =
      moveLocalClaim(
        id = "conversion-support",
        objectId = "ConversionFunnel-white-e5",
        family = StrategicObjectFamily.ConversionFunnel,
        profile = StrategicObjectProfile.ConversionFunnel(
          entrySquares = List(Square.E5),
          channelSquares = List(Square.E6),
          exitSquares = List(Square.E7),
          funnelFiles = Set(File.E),
          features = Set(ConversionFunnelFeature.AccessChannel)
        ),
        primaryTag = StrategicDeltaTag.RouteShortened,
        axis = StrategicMoveTransitionAxis.AccessRouteActivation,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.AccessRoute,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        ),
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, conversionClaim))

    assert(planned.claimIds.contains(accessClaim.id), clue(planned))
    assert(planned.supportClaimIds.contains(conversionClaim.id), clue(planned))
  }

  test("claim certification stamps counterplay residual specificity metadata on exact typed move-local claims") {
    val claim = certifiedCounterplayResidualClaim()

    assertEquals(claim.status, ClaimStatus.Certified, clue(claim))
    assertEquals(
      claim.plannerMetadata.residualSpecificityClass,
      Some(CertifiedResidualSpecificityClass.CounterplayExact),
      clue(claim)
    )
  }

  test("planner consumes certified residual specificity metadata instead of re-deriving move-local exactness") {
    val accessClaim =
      moveLocalRoutedAccessClaim(
        id = "access-metadata-residual",
        objectId = "AccessNetwork-white-e",
        route = StrategicRouteGeometry(
          origin = Square.C3,
          via = List(Square.C4),
          target = Square.E5
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-metadata-residual",
        objectId = "CounterplayAxis-white-e5",
        family = StrategicObjectFamily.CounterplayAxis,
        profile = StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.C4, Square.E5),
          breakSquares = List(Square.C4, Square.E5),
          pressureSquares = List(Square.C4, Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
        primaryTag = StrategicDeltaTag.CounterplayOpened,
        axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
        anchorSquares = List(Square.C4, Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.CounterplayResourceSeed,
            anchorSquares = List(Square.C4),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      ).copy(
        delta = moveLocalClaimDeltaWithoutResidualWitness(
          moveLocalClaim(
            id = "counterplay-metadata-residual",
            objectId = "CounterplayAxis-white-e5",
            family = StrategicObjectFamily.CounterplayAxis,
            profile = StrategicObjectProfile.CounterplayAxis(
              resourceSquares = List(Square.C4, Square.E5),
              breakSquares = List(Square.C4, Square.E5),
              pressureSquares = List(Square.C4, Square.E5),
              typedAxes = Set(CounterplayAxisType.Break)
            ),
            primaryTag = StrategicDeltaTag.CounterplayOpened,
            axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
            anchorSquares = List(Square.C4, Square.E5),
            evidenceRefs = List(
              StrategicDeltaEvidenceRef(
                primitiveKind = PrimitiveKind.CounterplayResourceSeed,
                anchorSquares = List(Square.C4),
                contestedSquares = List(Square.E5),
                lane = Some(File.E)
              )
            )
          ).delta
        ),
        plannerMetadata =
          CertifiedPlannerMetadata(
            residualSpecificityClass = Some(CertifiedResidualSpecificityClass.CounterplayExact)
          )
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assertEquals(planned.axis, QuestionAxis.WhyThis)
    assertEquals(planned.claimIds, List(counterplayClaim.id))
    assertEquals(planned.supportClaimIds, List(accessClaim.id))
  }

  test("planner consumes certified probe-kind metadata plus preserved fixed-target cluster witness for current-position admission and support pairing") {
    val fixedTargetWitness =
      FixedTargetClusterWitness(
        focalTargetSquare = Square.D6,
        clusterSquares = Set(Square.D6, Square.D5),
        matchingAccessRoutes = Set("AccessNetwork-white-d6"),
        matchingRestrictionShells = Set("RestrictionShell-white-d6"),
        matchingDefenderDependencies = Set("DefenderDependencyNetwork-white-d6"),
        disambiguation = "test-fixed-target-cluster"
      )
    val primaryClaim =
      positionLocalClaim(
        id = "probe-primary-metadata",
        objectId = "FixedTargetComplex-white-d6",
        family = StrategicObjectFamily.FixedTargetComplex,
        profile = StrategicObjectProfile.FixedTargetComplex(
          targetSquare = Square.D6,
          targetOwner = Color.Black,
          occupantRoles = Set.empty,
          fixed = true,
          defended = true
        ),
        primaryTag = StrategicDeltaTag.TargetFixed,
        anchorSquares = List(Square.D6),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.TargetSquare,
            anchorSquares = List(Square.D6),
            contestedSquares = List(Square.D6),
            lane = Some(File.D)
          )
        ),
        metadata =
          CertifiedPlannerMetadata(
            currentPositionProbeKind = Some(CertifiedCurrentPositionProbeKind.FixedTarget)
          )
      ).copy(
        boundaryWitnesses = Set(CertifiedBoundaryWitness.FixedTargetCluster(fixedTargetWitness))
      )
    val supportClaim =
      positionLocalClaim(
        id = "probe-support-metadata",
        objectId = "RestrictionShell-white-d6",
        family = StrategicObjectFamily.RestrictionShell,
        profile = StrategicObjectProfile.RestrictionShell(
          restrictedSquares = List(Square.D6),
          contestedSquares = List(Square.D5),
          constraintSquares = List(Square.D6)
        ),
        primaryTag = StrategicDeltaTag.RestrictionTightened,
        anchorSquares = List(Square.D6),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.TargetSquare,
            anchorSquares = List(Square.D6),
            contestedSquares = List(Square.D6),
            lane = Some(File.D)
          )
        ),
        status = ClaimStatus.SupportOnly,
        metadata =
          CertifiedPlannerMetadata(
            currentPositionProbeKind = Some(CertifiedCurrentPositionProbeKind.FixedTarget)
          )
      ).copy(
        boundaryWitnesses = Set(CertifiedBoundaryWitness.FixedTargetCluster(fixedTargetWitness))
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(primaryClaim, supportClaim))

    assert(FixedTargetClusterWitnessBoundary.sharesClusterWitness(primaryClaim, supportClaim))
    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assertEquals(planned.claimIds, List(primaryClaim.id))
    assertEquals(planned.supportClaimIds, List(supportClaim.id))
  }

  test("top CounterplayAxis max8 rows stay support-closed when an exact rival edge is touched but provisional move-local remains closed") {
    val rows =
      List(
        RuntimeSample(
          id = "2020_05_01_abhijeetgupta1016_infernal_xam_chesscom_titled_practical_60:ply:33",
          fen = "2bk1bnr/4qppp/pr3n2/3B4/3PpB2/7N/1PQ2PPP/R4RK1 w - - 6 17",
          playedUci = "a1c1"
        ),
        RuntimeSample(
          id = "2021_07_25_airgun1_apotatointhekitchen_chesscom_titled_practical_130:ply:17",
          fen = "r1bq1rk1/pp2ppbp/2n3p1/2pp2B1/3Pn3/2P1PN2/PP1NBPPP/R2Q1RK1 w - - 4 9",
          playedUci = "d2e4"
        ),
        RuntimeSample(
          id = "2022_01_04_kontosnik_1_absentzest_chesscom_titled_practical_69:ply:41",
          fen = "3r1rk1/pp3ppp/1b6/2pPNq2/2P1bP2/1P5P/1P4P1/R1BQ1R1K w - - 1 21",
          playedUci = "d1g4"
        ),
        RuntimeSample(
          id = "2021_07_25_airgun1_apotatointhekitchen_chesscom_titled_practical_130:ply:33",
          fen = "r2q1rk1/pp2p1b1/2n4p/3b1pp1/3P4/4PN2/PP2BBPP/2RQ1RK1 w - - 2 17",
          playedUci = "e2c4"
        ),
        RuntimeSample(
          id = "2022_01_04_kontosnik_1_absentzest_chesscom_titled_practical_69:ply:9",
          fen = "r1bqkb1r/pppp1ppp/5n2/1B2p3/3nP3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 6 5",
          playedUci = "b5a4"
        )
      )

    val results =
      rows.map { row =>
        val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
        val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
        val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
        val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
        val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
        val planned = CanonicalQuestionPlanner.plan(contract, claims)
        val accessClaims = claims.filter(_.delta.exists(_.family == StrategicObjectFamily.AccessNetwork))
        val counterplayClaims = claims.filter(_.delta.exists(_.family == StrategicObjectFamily.CounterplayAxis))
        val accessMoveClaims = accessClaims.filter(_.deltaScope == StrategicDeltaScope.MoveLocal)
        val counterplayMoveClaims = counterplayClaims.filter(_.deltaScope == StrategicDeltaScope.MoveLocal)
        val counterplayNonMoveClaims = counterplayClaims.filterNot(_.deltaScope == StrategicDeltaScope.MoveLocal)
        val move = moveTrace(row.playedUci)
        val ownCounterplayObjects =
          objects.filter(obj =>
            obj.family == StrategicObjectFamily.CounterplayAxis &&
              obj.owner == Color.White
          )
        val assessments =
          ownCounterplayObjects.flatMap(obj =>
            CounterplayMoveLocalBoundary
              .assess(obj, move, objects.map(other => other.id -> other).toMap)
              .map(obj.id -> _)
          )

        assert(accessClaims.nonEmpty, clue(row.id))
        assert(counterplayClaims.nonEmpty, clue(row.id))
        assertEquals(planned.axis, QuestionAxis.WhyThis, clue(row.id))
        assert(accessMoveClaims.nonEmpty, clue(row.id))
        assertEquals(counterplayMoveClaims, Nil, clue(s"${row.id}: ${counterplayNonMoveClaims.map(_.id)}"))
        assert(counterplayNonMoveClaims.nonEmpty, clue(row.id))
        assert(
          planned.supportClaimIds.toSet.intersect(counterplayClaims.map(_.id).toSet).isEmpty,
          clue(s"${row.id}: unexpected Counterplay support reopen ${planned.supportClaimIds}")
        )
        RuntimeSampleResult(
          id = row.id,
          exactRivalEdgeTouched =
            assessments.exists { case (_, assessment) =>
              assessment.exactRivalAdmitted &&
                assessment.moveTouchesCore &&
                assessment.relationTouch
            },
          provisionalScopeClosed = assessments.exists(_._2.blockedByProvisionalScope),
          counterplayMoveClaimCount = counterplayMoveClaims.size
        )
      }

    assert(
      results.count(result =>
        result.exactRivalEdgeTouched &&
          result.provisionalScopeClosed &&
          result.counterplayMoveClaimCount == 0
      ) >= 3,
      clue(results)
    )
  }

  test("exact rival-edge counterplay rows localize move-local blockage at provisional scope once the admitted rival edge is touched") {
    val rows =
      List(
        RuntimeSample(
          id = "2020_05_01_abhijeetgupta1016_infernal_xam_chesscom_titled_practical_60:ply:33",
          fen = "2bk1bnr/4qppp/pr3n2/3B4/3PpB2/7N/1PQ2PPP/R4RK1 w - - 6 17",
          playedUci = "a1c1"
        ),
        RuntimeSample(
          id = "2021_07_25_airgun1_apotatointhekitchen_chesscom_titled_practical_130:ply:17",
          fen = "r1bq1rk1/pp2ppbp/2n3p1/2pp2B1/3Pn3/2P1PN2/PP1NBPPP/R2Q1RK1 w - - 4 9",
          playedUci = "d2e4"
        ),
        RuntimeSample(
          id = "2022_01_04_kontosnik_1_absentzest_chesscom_titled_practical_69:ply:41",
          fen = "3r1rk1/pp3ppp/1b6/2pPNq2/2P1bP2/1P5P/1P4P1/R1BQ1R1K w - - 1 21",
          playedUci = "d1g4"
        ),
        RuntimeSample(
          id = "2021_07_25_airgun1_apotatointhekitchen_chesscom_titled_practical_130:ply:33",
          fen = "r2q1rk1/pp2p1b1/2n4p/3b1pp1/3P4/4PN2/PP2BBPP/2RQ1RK1 w - - 2 17",
          playedUci = "e2c4"
        ),
        RuntimeSample(
          id = "2022_01_04_kontosnik_1_absentzest_chesscom_titled_practical_69:ply:9",
          fen = "r1bqkb1r/pppp1ppp/5n2/1B2p3/3nP3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 6 5",
          playedUci = "b5a4"
        )
      )

    val results =
      rows.map { row =>
        val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
        val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
        val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
        val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
        val move = moveTrace(row.playedUci)
        val assessments =
          objects
            .filter(_.family == StrategicObjectFamily.CounterplayAxis)
            .flatMap(obj =>
              CounterplayMoveLocalBoundary
                .assess(obj, move, objects.map(other => other.id -> other).toMap)
                .map(obj.id -> _)
            )

        val moveLocalDeltas =
          deltas.filter(delta =>
            delta.family == StrategicObjectFamily.CounterplayAxis &&
              delta.scope == StrategicDeltaScope.MoveLocal
          )

        assertEquals(moveLocalDeltas, Nil, clue(s"${row.id}: unexpected Counterplay move-local reopen"))

        ExactCounterplayBoundaryResult(
          id = row.id,
          exactRivalAdmission = assessments.exists(_._2.exactRivalAdmitted),
          moveTouchesCore = assessments.exists(_._2.moveTouchesCore),
          relationTouch = assessments.exists(_._2.relationTouch),
          provisionalScopeClosed = assessments.exists(_._2.blockedByProvisionalScope),
          certificationBlocked = assessments.exists(_._2.blockedByCertification)
        )
      }

    assert(
      results.count(result =>
        result.exactRivalAdmission &&
          result.moveTouchesCore &&
          result.relationTouch &&
          result.provisionalScopeClosed &&
          !result.certificationBlocked
      ) >= 3,
      clue(results)
    )
  }

  test("exact positive counterplay slice reopens provisional move-local as a support-only claim without planner loosening") {
    val row =
      RuntimeSample(
        id = "counterplay-positive-pack",
        fen = "rn2qrk1/pbp1b1pp/1p1p4/3Ppp2/2P5/2NN2P1/PP1QPPBP/R4RK1 w - - 0 13",
        playedUci = "f2f4"
      )

    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    val move = moveTrace(row.playedUci)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    val assessment =
      objects
        .filter(_.id == "CounterplayAxis-black-kingside-f4-fg")
        .flatMap(obj => CounterplayMoveLocalBoundary.assess(obj, move, objectsById))
        .headOption
        .getOrElse(fail("expected exact positive CounterplayAxis assessment"))

    val counterplayMoveDeltas =
      deltas.filter(delta =>
        delta.objectId == "CounterplayAxis-black-kingside-f4-fg" &&
          delta.scope == StrategicDeltaScope.MoveLocal
      )
    val counterplayMoveClaims =
      claims.filter(claim =>
        claim.objectId == "CounterplayAxis-black-kingside-f4-fg" &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal
      )

    assertEquals(assessment.exactRivalAdmitted, true, clue(assessment))
    assertEquals(assessment.moveTouchesCore, true, clue(assessment))
    assertEquals(assessment.relationTouch, true, clue(assessment))
    assertEquals(assessment.moveWitnessSatisfied, true, clue(assessment))
    assertEquals(assessment.blockedByProvisionalScope, false, clue(assessment))
    assertEquals(assessment.blockedByCertification, false, clue(assessment))
    assertEquals(assessment.blocker, None, clue(assessment))
    assert(counterplayMoveDeltas.nonEmpty, clue(counterplayMoveDeltas))
    assert(counterplayMoveClaims.nonEmpty, clue(counterplayMoveClaims))
    assert(counterplayMoveClaims.forall(_.status == ClaimStatus.SupportOnly), clue(counterplayMoveClaims))
    assert(
      counterplayMoveClaims.flatMap(_.delta).exists(_.projection match
        case StrategicDeltaProjection.MoveLocal(_, witness) =>
          witness.matchedSquares.contains(Square.F4) &&
            witness.relationWitnesses.contains(StrategicRelationOperator.Denies)
        case _ =>
          false
      ),
      clue(counterplayMoveClaims)
    )
    assert(
      planned.claimIds.intersect(counterplayMoveClaims.map(_.id)).isEmpty,
      clue(planned)
    )
  }

  test("one exact central break row may reopen counterplay move-local support without widening the family") {
    val row =
      RuntimeSample(
        id = "counterplay-central-break-pack",
        fen = "r1bq1rk1/pp2npp1/4p2p/7n/2BP4/P1N2N2/1P3PPP/R2Q1RK1 w - - 0 13",
        playedUci = "f3e5"
      )

    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    val move = moveTrace(row.playedUci)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    val assessment =
      objects
        .filter(_.id == "CounterplayAxis-white-center-d4-de")
        .flatMap(obj => CounterplayMoveLocalBoundary.assess(obj, move, objectsById))
        .headOption
        .getOrElse(fail("expected exact central CounterplayAxis assessment"))

    val counterplayMoveDeltas =
      deltas.filter(delta =>
        delta.objectId == "CounterplayAxis-white-center-d4-de" &&
          delta.scope == StrategicDeltaScope.MoveLocal
      )
    val counterplayMoveClaims =
      claims.filter(claim =>
        claim.objectId == "CounterplayAxis-white-center-d4-de" &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal
      )

    assertEquals(assessment.exactRivalAdmitted, true, clue(assessment))
    assertEquals(assessment.moveTouchesCore, true, clue(assessment))
    assertEquals(assessment.relationTouch, true, clue(assessment))
    assertEquals(assessment.moveWitnessSatisfied, true, clue(assessment))
    assertEquals(assessment.blockedByProvisionalScope, false, clue(assessment))
    assertEquals(assessment.blockedByCertification, false, clue(assessment))
    assertEquals(assessment.blocker, None, clue(assessment))
    assertEquals(
      assessment.relationMatches.map(_.targetId).distinct.sorted,
      List(
        "KingSafetyShell-black-wholeboard-a7-abcef",
        "RestrictionShell-black-center-d4-de"
      ),
      clue(assessment)
    )
    assert(counterplayMoveDeltas.nonEmpty, clue(counterplayMoveDeltas))
    assert(counterplayMoveClaims.nonEmpty, clue(counterplayMoveClaims))
    assert(counterplayMoveClaims.forall(_.status == ClaimStatus.SupportOnly), clue(counterplayMoveClaims))
    assert(
      planned.claimIds.intersect(counterplayMoveClaims.map(_.id)).isEmpty,
      clue(planned)
    )
  }

  test("counterplay false survivors stay provisional-scope closed on the exact rival edge instead of reopening move-local") {
    val rows =
      List(
        (
          RuntimeSample(
            id = "2024_10_22_3_3_tsolakidou_stavroula_goryachkina_a_twic_master_classical_26:ply:17",
            fen = "r1bq1rk1/ppp1bpp1/2np1n1p/4p3/P1B1P3/3P1NB1/1PP2PPP/RN1QK2R w KQ - 1 9",
            playedUci = "a4a5"
          ),
          "CounterplayAxis-white-queenside-a4-abc",
          List("RestrictionShell-black-queenside-a4-abc")
        ),
        (
          RuntimeSample(
            id = "2024_10_24_6_1_stein_robert_alexakis_dimitris_twic_master_classical_58:ply:41",
            fen = "2b1k2r/3p1ppp/p5n1/2Np4/4P3/P7/1r2BPPP/R3K2R w KQk - 0 21",
            playedUci = "c5d3"
          ),
          "CounterplayAxis-white-center-d5-de",
          List("TensionState-black-center-d5-de")
        ),
        (
          RuntimeSample(
            id = "2024_10_20_1_16_schreiner_pe_subelj_jan_twic_master_classical_18:ply:9",
            fen = "rnbqkb1r/pp3ppp/4pn2/2pp4/8/5NP1/PPPPPPBP/RNBQ1RK1 w kq - 0 5",
            playedUci = "d2d4"
          ),
          "CounterplayAxis-black-center-d4-de",
          List("RestrictionShell-white-center-d4-de")
        ),
        (
          RuntimeSample(
            id = "2_19_hjartarson_johann_stefansson_vignir_vatnar_lichess_broadcast_master_classical_33:ply:49",
            fen = "5rk1/p1q1npp1/Qp2p2p/3nN3/1P1P4/P7/B4PPP/4R1K1 w - - 5 25",
            playedUci = "a2c4"
          ),
          "CounterplayAxis-black-queenside-b4-bc",
          List("RestrictionShell-white-queenside-b4-abc")
        ),
        (
          RuntimeSample(
            id = "bharath_subramaniyam_h_abdilkhair_abilmansur_lichess_broadcast_master_classical_12:ply:41",
            fen = "3r1rk1/pp2nppp/2bNp3/4P3/q4P2/P1PBQ3/1P4PP/1R3RK1 w - - 7 21",
            playedUci = "c3c4"
          ),
          "CounterplayAxis-black-queenside-a3-abc",
          List("RestrictionShell-white-queenside-a3-abc")
        )
      )

    rows.foreach { case (row, objectId, expectedTargets) =>
      val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
      val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val objectsById = objects.map(obj => obj.id -> obj).toMap
      val move = moveTrace(row.playedUci)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)

      val assessment =
        objects
          .filter(_.id == objectId)
          .flatMap(obj => CounterplayMoveLocalBoundary.assess(obj, move, objectsById))
          .headOption
          .getOrElse(fail(s"expected exact CounterplayAxis assessment for ${row.id}"))

      val counterplayMoveDeltas =
        deltas.filter(delta =>
          delta.objectId == objectId &&
            delta.scope == StrategicDeltaScope.MoveLocal
        )
      val counterplayMoveClaims =
        claims.filter(claim =>
          claim.objectId == objectId &&
            claim.deltaScope == StrategicDeltaScope.MoveLocal
        )

      assertEquals(assessment.exactRivalAdmitted, true, clue(row.id))
      assertEquals(assessment.moveTouchesCore, true, clue(row.id))
      assertEquals(assessment.relationTouch, true, clue(row.id))
      assertEquals(assessment.moveWitnessSatisfied, true, clue(row.id))
      assertEquals(assessment.blockedByProvisionalScope, true, clue(assessment))
      assertEquals(assessment.blockedByCertification, false, clue(assessment))
      assertEquals(assessment.blocker, Some(CounterplayMoveLocalBlocker.ProvisionalScopeClosed), clue(assessment))
      assertEquals(assessment.relationMatches.map(_.targetId).distinct, expectedTargets, clue(assessment))
      assert(counterplayMoveDeltas.isEmpty, clue(counterplayMoveDeltas))
      assert(counterplayMoveClaims.isEmpty, clue(counterplayMoveClaims))
    }
  }

  test("counterplay near-miss rows stay move-local closed when only a king-safety relation is touched") {
    val row =
      RuntimeSample(
        id = "counterplay-near-miss",
        fen = "1r1r2k1/6p1/1qb1p1p1/p1ppPp2/5Q1P/1PP1RN2/P4PP1/1R4K1 w - - 0 25",
        playedUci = "f3g5"
      )

    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val objectsById = objects.map(obj => obj.id -> obj).toMap
    val move = moveTrace(row.playedUci)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)

    val assessment =
      objects
        .filter(_.id == "CounterplayAxis-white-center-d5-de")
        .flatMap(obj => CounterplayMoveLocalBoundary.assess(obj, move, objectsById))
        .headOption
        .getOrElse(fail("expected near-miss CounterplayAxis assessment"))

    val counterplayMoveDeltas =
      deltas.filter(delta =>
        delta.objectId == "CounterplayAxis-white-center-d5-de" &&
          delta.scope == StrategicDeltaScope.MoveLocal
      )
    val counterplayMoveClaims =
      claims.filter(claim =>
        claim.objectId == "CounterplayAxis-white-center-d5-de" &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal
      )

    assertEquals(assessment.exactRivalAdmitted, true, clue(assessment))
    assertEquals(assessment.moveTouchesCore, false, clue(assessment))
    assertEquals(assessment.relationTouch, true, clue(assessment))
    assertEquals(assessment.moveWitnessSatisfied, false, clue(assessment))
    assertEquals(assessment.blockedByProvisionalScope, false, clue(assessment))
    assertEquals(assessment.blockedByCertification, false, clue(assessment))
    assertEquals(assessment.blocker, Some(CounterplayMoveLocalBlocker.MissingMoveEdgeTouch), clue(assessment))
    assert(counterplayMoveDeltas.isEmpty, clue(counterplayMoveDeltas))
    assert(counterplayMoveClaims.isEmpty, clue(counterplayMoveClaims))
    assertEquals(
      assessment.relationMatches.map(_.targetId).distinct,
      List("KingSafetyShell-black-wholeboard-a5-acdefg"),
      clue(assessment)
    )
  }

  private def mkAccessClaim(
      id: String,
      square: Square,
      file: File
  ): CertifiedClaim =
    moveLocalClaim(
      id = id,
      objectId = s"AccessNetwork-white-${file.char.toString.toLowerCase}",
      family = StrategicObjectFamily.AccessNetwork,
      profile = StrategicObjectProfile.AccessNetwork(
        lane = Some(file),
        route = None,
        roles = Set.empty,
        contestedSquares = List(square)
      ),
      primaryTag = StrategicDeltaTag.AccessOpened,
      axis = StrategicMoveTransitionAxis.AccessRouteActivation,
      anchorSquares = List(square),
      evidenceRefs = List(
        StrategicDeltaEvidenceRef(
          primitiveKind = PrimitiveKind.AccessRoute,
          anchorSquares = List(square),
          contestedSquares = List(square),
          lane = Some(file)
        )
      )
    )

  private def moveLocalRoutedAccessClaim(
      id: String,
      objectId: String,
      route: StrategicRouteGeometry,
      status: ClaimStatus = ClaimStatus.Certified,
      plannerMetadata: CertifiedPlannerMetadata = CertifiedPlannerMetadata()
  ): CertifiedClaim =
    val entrySquare =
      route.via.lastOption.getOrElse(route.origin)
    moveLocalClaim(
      id = id,
      objectId = objectId,
      family = StrategicObjectFamily.AccessNetwork,
      profile = StrategicObjectProfile.AccessNetwork(
        lane = Some(route.target.file),
        route = Some(route),
        roles = Set.empty,
        contestedSquares = List(route.target)
      ),
      primaryTag = StrategicDeltaTag.AccessOpened,
      axis = StrategicMoveTransitionAxis.AccessRouteActivation,
      anchorSquares = List(entrySquare, route.target),
      evidenceRefs = List(
        StrategicDeltaEvidenceRef(
          primitiveKind = PrimitiveKind.AccessRoute,
          anchorSquares = List(entrySquare),
          contestedSquares = List(route.target),
          lane = Some(route.target.file)
        )
      ),
      status = status,
      plannerMetadata = plannerMetadata
    )

  private def moveLocalClaim(
      id: String,
      objectId: String,
      family: StrategicObjectFamily,
      profile: StrategicObjectProfile,
      primaryTag: StrategicDeltaTag,
      axis: StrategicMoveTransitionAxis,
      anchorSquares: List[Square],
      evidenceRefs: List[StrategicDeltaEvidenceRef],
      status: ClaimStatus = ClaimStatus.Certified,
      plannerMetadata: CertifiedPlannerMetadata = CertifiedPlannerMetadata()
  ): CertifiedClaim =
    val anchor =
      StrategicObjectAnchor(
        kind = StrategicAnchorKind.Square,
        role = StrategicAnchorRole.Primary,
        squares = anchorSquares
      )
    val witness =
      StrategicMoveTransitionWitness(
        move = StrategicPlayedMoveTrace(Square.D4, Square.E5),
        axis = axis,
        matchedSquares = anchorSquares,
        matchedFiles = anchorSquares.map(_.file),
        relationWitnesses = Set(StrategicRelationOperator.Enables),
        primitiveKinds = evidenceRefs.map(_.primitiveKind).toSet
      )

    val defaultPlannerMetadata =
      plannerMetadata.copy(
        residualSpecificityClass =
          plannerMetadata.residualSpecificityClass.orElse(
            family match
              case StrategicObjectFamily.CounterplayAxis
                  if status == ClaimStatus.Certified || status == ClaimStatus.SupportOnly =>
                Some(CertifiedResidualSpecificityClass.CounterplayExact)
              case StrategicObjectFamily.PlanRace
                  if status == ClaimStatus.Certified || status == ClaimStatus.SupportOnly =>
                Some(CertifiedResidualSpecificityClass.PlanRaceExact)
              case StrategicObjectFamily.ConversionFunnel
                  if status == ClaimStatus.Certified =>
                Some(CertifiedResidualSpecificityClass.ConversionFunnelExact)
              case _ =>
                None
          )
      )

    CertifiedClaim(
      id = id,
      objectId = objectId,
      deltaScope = StrategicDeltaScope.MoveLocal,
      status = status,
      readiness = StrategicObjectReadiness.Stable,
      delta = Some(
        StrategicObjectDelta(
          objectId = objectId,
          family = family,
          owner = Color.White,
          scope = StrategicDeltaScope.MoveLocal,
          profile = profile,
          projection = StrategicDeltaProjection.MoveLocal(primaryTag, witness),
          changedAnchors = List(anchor),
          evidenceRefs = evidenceRefs
        )
      ),
      plannerMetadata = defaultPlannerMetadata
    )

  private def moveLocalClaimDeltaWithoutResidualWitness(
      delta: Option[StrategicObjectDelta]
  ): Option[StrategicObjectDelta] =
    delta.map(existing =>
      existing.copy(
        projection =
          existing.projection match
            case StrategicDeltaProjection.MoveLocal(change, witness) =>
              StrategicDeltaProjection.MoveLocal(
                change,
                witness.copy(
                  relationWitnesses = Set.empty
                )
              )
            case other =>
              other
      )
    )

  private def positionLocalClaim(
      id: String,
      objectId: String,
      family: StrategicObjectFamily,
      profile: StrategicObjectProfile,
      primaryTag: StrategicDeltaTag,
      anchorSquares: List[Square],
      evidenceRefs: List[StrategicDeltaEvidenceRef],
      status: ClaimStatus = ClaimStatus.Certified,
      metadata: CertifiedPlannerMetadata
  ): CertifiedClaim =
    val anchor =
      StrategicObjectAnchor(
        kind = StrategicAnchorKind.Square,
        role = StrategicAnchorRole.Primary,
        squares = anchorSquares
      )

    CertifiedClaim(
      id = id,
      objectId = objectId,
      deltaScope = StrategicDeltaScope.PositionLocal,
      status = status,
      readiness = StrategicObjectReadiness.Stable,
      delta = Some(
        StrategicObjectDelta(
          objectId = objectId,
          family = family,
          owner = Color.White,
          scope = StrategicDeltaScope.PositionLocal,
          profile = profile,
          projection =
            StrategicDeltaProjection.PositionLocal(
              primaryTag,
              focalAnchorCount = anchorSquares.size,
              witnesses = Set.empty
            ),
          changedAnchors = List(anchor),
          evidenceRefs = evidenceRefs
        )
      ),
      plannerMetadata = metadata
    )

  private def certifiedCounterplayResidualClaim(): CertifiedClaim =
    val objectId = "CounterplayAxis-white-e5"
    val anchor =
      StrategicObjectAnchor(
        kind = StrategicAnchorKind.Square,
        role = StrategicAnchorRole.Primary,
        squares = List(Square.E5)
      )
    val profile =
      StrategicObjectProfile.CounterplayAxis(
        resourceSquares = List(Square.E5),
        breakSquares = List(Square.E5),
        pressureSquares = List(Square.E5),
        typedAxes = Set(CounterplayAxisType.Break)
      )
    val primitive =
      PrimitiveReference(
        kind = PrimitiveKind.CounterplayResourceSeed,
        owner = Color.White,
        anchorSquares = List(Square.E5),
        contestedSquares = List(Square.E5),
        lane = Some(File.E)
      )
    val obj =
      StrategicObject(
        id = objectId,
        family = StrategicObjectFamily.CounterplayAxis,
        owner = Color.White,
        locus = StrategicObjectLocus(squares = List(Square.E5)),
        sector = ObjectSector.Center,
        anchors = List(anchor),
        profile = profile,
        supportingPrimitives = List(primitive),
        supportingPieces = Nil,
        rivalResourcesOrObjects = Nil,
        relations = Nil,
        stateStrength =
          StrategicObjectStateStrength(
            band = StrategicStrengthBand.Established,
            coverage = 1,
            supportBalance = 1,
            pressureBalance = 1
          ),
        readiness = StrategicObjectReadiness.Stable,
        horizonClass = ObjectHorizonClass.Operational,
        evidenceFootprint =
          StrategicObjectEvidenceFootprint(
            primitiveKinds = Set(PrimitiveKind.CounterplayResourceSeed),
            primitiveCount = 1,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lanes = List(File.E),
            supportingPieceCount = 0,
            rivalCount = 1,
            supportBalance = 1,
            pressureBalance = 1,
            mobilityGain = 0
          )
      )
    val delta =
      StrategicObjectDelta(
        objectId = objectId,
        family = StrategicObjectFamily.CounterplayAxis,
        owner = Color.White,
        scope = StrategicDeltaScope.MoveLocal,
        profile = profile,
        projection =
          StrategicDeltaProjection.MoveLocal(
            StrategicDeltaTag.CounterplayOpened,
            StrategicMoveTransitionWitness(
              move = StrategicPlayedMoveTrace(Square.E4, Square.E5),
              axis = StrategicMoveTransitionAxis.CounterplayResourceShift,
              matchedSquares = List(Square.E5),
              matchedFiles = List(File.E),
              relationWitnesses = Set(StrategicRelationOperator.Enables),
              primitiveKinds = Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.BreakCandidate)
            )
          ),
        changedAnchors = List(anchor),
        evidenceRefs =
          List(
            StrategicDeltaEvidenceRef(
              primitiveKind = PrimitiveKind.CounterplayResourceSeed,
              anchorSquares = List(Square.E5),
              contestedSquares = List(Square.E5),
              lane = Some(File.E)
            )
          )
      )

    CanonicalClaimCertification
      .certify(visibleMoveContract, List(obj), List(delta))
      .find(_.objectId == objectId)
      .getOrElse(fail("expected certified counterplay residual claim"))

  private def moveTrace(
      playedUci: String
  ): StrategicPlayedMoveTrace =
    StrategicPlayedMoveTrace(
      from = Square.fromKey(playedUci.take(2)).getOrElse(fail(s"invalid move: $playedUci")),
      to = Square.fromKey(playedUci.slice(2, 4)).getOrElse(fail(s"invalid move: $playedUci"))
    )

  private case class RuntimeSample(
      id: String,
      fen: String,
      playedUci: String
  )

  private case class RuntimeSampleResult(
      id: String,
      exactRivalEdgeTouched: Boolean,
      provisionalScopeClosed: Boolean,
      counterplayMoveClaimCount: Int
  )

  private case class ExactCounterplayBoundaryResult(
      id: String,
      exactRivalAdmission: Boolean,
      moveTouchesCore: Boolean,
      relationTouch: Boolean,
      provisionalScopeClosed: Boolean,
      certificationBlocked: Boolean
  )
