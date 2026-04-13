package lila.llm.strategicobject

import chess.{ Color, File, Pawn, Square }
import munit.FunSuite

class CounterplayAxisRivalRelationBoundaryTest extends FunSuite:

  test("counterplay exact rival relation boundary requires a live rival relation on the same object graph edge") {
    val rival = manualRivalObject()
    val counterplay = manualCounterplayObject()

    assert(
      CounterplayAxisRivalRelationBoundary.hasExactRivalRelation(counterplay, Map(rival.id -> rival)),
      clue(counterplay)
    )
    assert(
      !CounterplayAxisRivalRelationBoundary.hasExactRivalRelation(counterplay.copy(relations = Nil), Map(rival.id -> rival)),
      clue(counterplay)
    )
    assert(
      !CounterplayAxisRivalRelationBoundary.hasExactRivalRelation(counterplay, Map(disjointRivalObject().id -> disjointRivalObject())),
      clue(counterplay)
    )
  }

  test("counterplay move-local witness stays closed without an exact rival relation") {
    val rival = manualRivalObject()
    val counterplay = manualCounterplayObject()
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("e4e5")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("e4e5")

    val exactDeltas =
      CanonicalStrategicObjectDeltaProjector.project(contract, truth, List(counterplay, rival))
    val missingRelationDeltas =
      CanonicalStrategicObjectDeltaProjector.project(contract, truth, List(counterplay.copy(relations = Nil), rival))

    assert(
      exactDeltas.exists(delta =>
        delta.family == StrategicObjectFamily.CounterplayAxis &&
          delta.scope == StrategicDeltaScope.MoveLocal
      ),
      clue(exactDeltas)
    )
    assert(
      !missingRelationDeltas.exists(delta =>
        delta.family == StrategicObjectFamily.CounterplayAxis &&
          delta.scope == StrategicDeltaScope.MoveLocal
      ),
      clue(missingRelationDeltas)
    )
  }

  private def manualCounterplayObject(): StrategicObject =
    StrategicObject(
      id = "white-counterplay",
      family = StrategicObjectFamily.CounterplayAxis,
      owner = Color.White,
      locus = StrategicObjectLocus(squares = List(Square.E5), files = List(File.E)),
      sector = ObjectSector.Center,
      anchors =
        List(
          StrategicObjectAnchor(StrategicAnchorKind.Square, StrategicAnchorRole.Primary, squares = List(Square.E5)),
          StrategicObjectAnchor(StrategicAnchorKind.File, StrategicAnchorRole.Entry, file = Some(File.E))
        ),
      profile =
        StrategicObjectProfile.CounterplayAxis(
          resourceSquares = List(Square.E5),
          breakSquares = List(Square.E5),
          pressureSquares = List(Square.E5),
          typedAxes = Set(CounterplayAxisType.Break)
        ),
      supportingPrimitives =
        List(
          PrimitiveReference(
            kind = PrimitiveKind.CounterplayResourceSeed,
            owner = Color.White,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E),
            roles = Set(Pawn)
          ),
          PrimitiveReference(
            kind = PrimitiveKind.BreakCandidate,
            owner = Color.White,
            anchorSquares = List(Square.E5),
            contestedSquares = List(Square.E5),
            lane = Some(File.E),
            roles = Set(Pawn)
          )
        ),
      supportingPieces = Nil,
      rivalResourcesOrObjects =
        List(
          StrategicRivalReference(
            kind = RivalReferenceKind.Object,
            owner = Color.Black,
            squares = List(Square.E6),
            file = Some(File.E),
            primitiveKind = Some(PrimitiveKind.CriticalSquare),
            objectId = Some("black-rival"),
            objectFamily = Some(StrategicObjectFamily.RestrictionShell)
          )
        ),
      relations =
        List(
          StrategicRelation(
            StrategicRelationOperator.OverloadsOrUndermines,
            StrategicRelationTarget("black-rival", StrategicObjectFamily.RestrictionShell, Color.Black)
          )
        ),
      stateStrength = StrategicObjectStateStrength(StrategicStrengthBand.Established, 2, 2, 2),
      readiness = StrategicObjectReadiness.Stable,
      horizonClass = ObjectHorizonClass.Operational,
      evidenceFootprint =
        StrategicObjectEvidenceFootprint(
          primitiveKinds = Set(PrimitiveKind.CounterplayResourceSeed, PrimitiveKind.BreakCandidate),
          primitiveCount = 2,
          anchorSquares = List(Square.E5),
          contestedSquares = List(Square.E5),
          lanes = List(File.E),
          supportingPieceCount = 0,
          rivalCount = 1,
          supportBalance = 2,
          pressureBalance = 2,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Contested)
        )
    )

  private def manualRivalObject(): StrategicObject =
    StrategicObject(
      id = "black-rival",
      family = StrategicObjectFamily.RestrictionShell,
      owner = Color.Black,
      locus = StrategicObjectLocus(squares = List(Square.E6), files = List(File.E)),
      sector = ObjectSector.Center,
      anchors =
        List(
          StrategicObjectAnchor(StrategicAnchorKind.Square, StrategicAnchorRole.Constraint, squares = List(Square.E6))
        ),
      profile =
        StrategicObjectProfile.RestrictionShell(
          restrictedSquares = List(Square.E5),
          contestedSquares = List(Square.E5),
          constraintSquares = List(Square.E6)
        ),
      supportingPrimitives =
        List(
          PrimitiveReference(
            kind = PrimitiveKind.CriticalSquare,
            owner = Color.Black,
            anchorSquares = List(Square.E6),
            contestedSquares = List(Square.E5, Square.E6),
            lane = Some(File.E)
          )
        ),
      supportingPieces = Nil,
      rivalResourcesOrObjects = Nil,
      relations = Nil,
      stateStrength = StrategicObjectStateStrength(StrategicStrengthBand.Established, 1, 1, 1),
      readiness = StrategicObjectReadiness.Stable,
      horizonClass = ObjectHorizonClass.Operational,
      evidenceFootprint =
        StrategicObjectEvidenceFootprint(
          primitiveKinds = Set(PrimitiveKind.CriticalSquare),
          primitiveCount = 1,
          anchorSquares = List(Square.E6),
          contestedSquares = List(Square.E5, Square.E6),
          lanes = List(File.E),
          supportingPieceCount = 0,
          rivalCount = 0,
          supportBalance = 1,
          pressureBalance = 1,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Contested)
        )
    )

  private def disjointRivalObject(): StrategicObject =
    StrategicObject(
      id = "black-rival",
      family = StrategicObjectFamily.RestrictionShell,
      owner = Color.Black,
      locus = StrategicObjectLocus(squares = List(Square.H6), files = List(File.H)),
      sector = ObjectSector.Kingside,
      anchors =
        List(
          StrategicObjectAnchor(StrategicAnchorKind.Square, StrategicAnchorRole.Constraint, squares = List(Square.H6))
        ),
      profile =
        StrategicObjectProfile.RestrictionShell(
          restrictedSquares = List(Square.H5),
          contestedSquares = List(Square.H5),
          constraintSquares = List(Square.H6)
        ),
      supportingPrimitives =
        List(
          PrimitiveReference(
            kind = PrimitiveKind.CriticalSquare,
            owner = Color.Black,
            anchorSquares = List(Square.H6),
            contestedSquares = List(Square.H5, Square.H6),
            lane = Some(File.H)
          )
        ),
      supportingPieces = Nil,
      rivalResourcesOrObjects = Nil,
      relations = Nil,
      stateStrength = StrategicObjectStateStrength(StrategicStrengthBand.Established, 1, 1, 1),
      readiness = StrategicObjectReadiness.Stable,
      horizonClass = ObjectHorizonClass.Operational,
      evidenceFootprint =
        StrategicObjectEvidenceFootprint(
          primitiveKinds = Set(PrimitiveKind.CriticalSquare),
          primitiveCount = 1,
          anchorSquares = List(Square.H6),
          contestedSquares = List(Square.H5, Square.H6),
          lanes = List(File.H),
          supportingPieceCount = 0,
          rivalCount = 0,
          supportBalance = 1,
          pressureBalance = 1,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Contested)
        )
    )
