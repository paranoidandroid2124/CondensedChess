package lila.llm.strategicobject

import chess.{ Color, File, Square }
import munit.FunSuite

class QuestionPlannerAccessArbitrationTest extends FunSuite:

  private val visibleMoveContract =
    PrimitiveExtractionTest.moveTransitionVisibleContractFor("d4e5")

  test("planner demotes overlapping access-network ownership behind a more specific certified causal claim") {
    val accessClaim =
      moveLocalClaim(
        id = "access-overlap",
        objectId = "AccessNetwork-white-e",
        family = StrategicObjectFamily.AccessNetwork,
        profile = StrategicObjectProfile.AccessNetwork(
          lane = Some(File.E),
          route = None,
          roles = Set.empty,
          contestedSquares = List(Square.E5)
        ),
        primaryTag = StrategicDeltaTag.AccessOpened,
        axis = StrategicMoveTransitionAxis.AccessRouteActivation,
        anchorSquares = List(Square.E5),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.AccessRoute,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E)
          )
        )
      )
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-overlap",
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
    assertEquals(planned.claimIds, List(counterplayClaim.id))
    assertEquals(planned.supportClaimIds, List(accessClaim.id))
  }

  test("support-only counterplay residual can demote access-network primary into support") {
    val accessClaim = mkAccessClaim("access-support-demotion", Square.E5, File.E)
    val counterplayClaim =
      moveLocalClaim(
        id = "counterplay-support",
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
        ),
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(visibleMoveContract, List(accessClaim, counterplayClaim))

    assertEquals(planned.axis, QuestionAxis.WhyThis)
    assertEquals(planned.claimIds, Nil)
    assert(planned.supportClaimIds.contains(accessClaim.id), clue(planned))
    assert(planned.supportClaimIds.contains(counterplayClaim.id), clue(planned))
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

  test("support-only plan-race residual can demote access-network primary into support") {
    val accessClaim = mkAccessClaim("access-plan-race", Square.E5, File.E)
    val planRaceClaim =
      moveLocalClaim(
        id = "plan-race-support",
        objectId = "PlanRace-white-e5",
        family = StrategicObjectFamily.PlanRace,
        profile = StrategicObjectProfile.PlanRace(
          rivalOwner = Color.Black,
          raceSquares = List(Square.E5, Square.E6),
          raceFiles = Set(File.E),
          ownGoalSquares = List(Square.E6),
          rivalGoalSquares = List(Square.E4),
          features = Set(PlanRaceFeature.BilateralCounterplay)
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

  private def moveLocalClaim(
      id: String,
      objectId: String,
      family: StrategicObjectFamily,
      profile: StrategicObjectProfile,
      primaryTag: StrategicDeltaTag,
      axis: StrategicMoveTransitionAxis,
      anchorSquares: List[Square],
      evidenceRefs: List[StrategicDeltaEvidenceRef],
      status: ClaimStatus = ClaimStatus.Certified
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
      )
    )
