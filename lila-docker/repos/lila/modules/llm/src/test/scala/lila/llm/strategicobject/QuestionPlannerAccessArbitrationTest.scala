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

  test("planner keeps access-network primary when no specific residual overlap is proven") {
    val accessClaim =
      moveLocalClaim(
        id = "access-disjoint",
        objectId = "AccessNetwork-white-h",
        family = StrategicObjectFamily.AccessNetwork,
        profile = StrategicObjectProfile.AccessNetwork(
          lane = Some(File.H),
          route = None,
          roles = Set.empty,
          contestedSquares = List(Square.H7)
        ),
        primaryTag = StrategicDeltaTag.AccessOpened,
        axis = StrategicMoveTransitionAxis.AccessRouteActivation,
        anchorSquares = List(Square.H7),
        evidenceRefs = List(
          StrategicDeltaEvidenceRef(
            primitiveKind = PrimitiveKind.AccessRoute,
            anchorSquares = List(Square.H7),
            contestedSquares = List(Square.H7),
            lane = Some(File.H)
          )
        )
      )
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

  private def moveLocalClaim(
      id: String,
      objectId: String,
      family: StrategicObjectFamily,
      profile: StrategicObjectProfile,
      primaryTag: StrategicDeltaTag,
      axis: StrategicMoveTransitionAxis,
      anchorSquares: List[Square],
      evidenceRefs: List[StrategicDeltaEvidenceRef]
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
      status = ClaimStatus.Certified,
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

