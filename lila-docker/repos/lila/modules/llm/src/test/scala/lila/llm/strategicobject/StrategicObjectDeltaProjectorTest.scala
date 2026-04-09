package lila.llm.strategicobject

import chess.{ Color, File, Square }
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class StrategicObjectDeltaProjectorTest extends FunSuite:

  import StrategicObjectDeltaProjectorTest.*

  test("delta fixture bank covers all Tier-1 direct owners, scopes, and nasty negatives") {
    assert(rows.size >= 80, clue(s"expected at least 80 delta rows, got ${rows.size}"))
    assertEquals(rows.groupBy(_.family).size, 14)
    assert(rows.forall(row => StrategicObjectFamily.directDeltaOwners.contains(parseFamily(row.family))))

    StrategicObjectFamily.directDeltaOwners.foreach { family =>
      val familyRows = rows.filter(row => parseFamily(row.family) == family)
      assert(familyRows.exists(_.caseType == "exact"), clue(s"missing exact row for $family"))
      assert(familyRows.exists(_.caseType == "negative"), clue(s"missing negative row for $family"))
      assert(familyRows.exists(_.caseType == "contrastive"), clue(s"missing contrastive row for $family"))
      assert(familyRows.exists(_.caseType == "near_miss"), clue(s"missing near_miss row for $family"))
      assert(
        familyRows.map(row => parseScope(row.scope)).toSet == Set(
          StrategicDeltaScope.MoveLocal,
          StrategicDeltaScope.PositionLocal,
          StrategicDeltaScope.Comparative
        ),
        clue(s"missing scope coverage for $family")
      )
    }
  }

  test("Tier-1 provisional families carry nasty-negative, false-witness, and false-rival delta coverage") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )

    provisionalFamilies.foreach { family =>
      val familyRows = rows.filter(row => parseFamily(row.family) == family)
      val caseTypes = familyRows.map(_.caseType).toSet
      assert(caseTypes.contains("exact"), clue(s"missing exact delta row for $family"))
      assert(caseTypes.contains("contrastive"), clue(s"missing contrastive delta row for $family"))
      assert(caseTypes.contains("near_miss"), clue(s"missing near_miss delta row for $family"))
      assert(caseTypes.contains("nasty_negative"), clue(s"missing nasty_negative delta row for $family"))
      assert(caseTypes.contains("move_local_false_witness"), clue(s"missing move_local_false_witness row for $family"))
      assert(caseTypes.contains("comparative_false_rival"), clue(s"missing comparative_false_rival row for $family"))
    }
  }

  test("Tier-1 provisional families carry explicit comparative near-miss rows distinct from contrastive and false-rival boards") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )

    provisionalFamilies.foreach { family =>
      val familyRows =
        rows.filter(row =>
          parseFamily(row.family) == family &&
            parseScope(row.scope) == StrategicDeltaScope.Comparative
        )
      val nearMiss = familyRows.find(_.caseType == "near_miss").getOrElse(
        fail(s"missing comparative near_miss row for $family")
      )
      val falseRival = familyRows.find(_.caseType == "comparative_false_rival").getOrElse(
        fail(s"missing comparative_false_rival row for $family")
      )
      val strongContrast = familyRows.find(_.caseType == "contrastive").getOrElse(
        fail(s"missing comparative contrastive row for $family")
      )

      assertEquals(nearMiss.expectation, "present", clue(s"${nearMiss.id}: shallow comparative should stay projector-visible"))
      assertEquals(nearMiss.plannerExpectation, Some("none"), clue(s"${nearMiss.id}: shallow comparative must stay planner-none"))
      assertEquals(
        nearMiss.localizationExpectation,
        Some("certification"),
        clue(s"${nearMiss.id}: shallow comparative should localize at certification")
      )
      assert(nearMiss.fen != falseRival.fen, clue(s"${nearMiss.id}: shallow comparative must not reuse the false-rival board"))
      assert(nearMiss.fen != strongContrast.fen, clue(s"${nearMiss.id}: shallow comparative must not reuse the strong-contrast board"))
    }
  }

  test("projector emits deltas only for Tier-1 direct owners even when later-tier objects exist") {
    val laterTierRows =
      StrategicObjectSynthesizerTest.rows.filter(row =>
        row.expectation == "present" &&
          !StrategicObjectFamily.directDeltaOwners.contains(parseFamily(row.family))
      )

    assert(laterTierRows.nonEmpty, clue("expected later-tier object rows"))
    laterTierRows.foreach { row =>
      val objects = StrategicObjectSynthesizerTest.objectsForRow(row)
      val deltas =
        CanonicalStrategicObjectDeltaProjector.project(
          PrimitiveExtractionTest.neutralContract,
          PrimitiveExtractionTest.neutralTruthFrame,
          objects
        )
      assert(
        !deltas.exists(delta => delta.family == parseFamily(row.family) && delta.owner == parseColor(row.owner)),
        clue(s"${row.id} leaked ${row.family} as a delta owner")
      )
    }
  }

  test("move-local deltas require transition truth and carry typed transition witness") {
    val row =
      rows.find(r => r.expectation == "present" && parseScope(r.scope) == StrategicDeltaScope.MoveLocal).getOrElse(
        fail("expected at least one positive move-local row")
      )
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val moveDeltas =
      CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val neutralDeltas =
      CanonicalStrategicObjectDeltaProjector.project(
        PrimitiveExtractionTest.neutralContract,
        PrimitiveExtractionTest.neutralTruthFrame,
        objects
      )
    val matched = expectedDeltas(row, moveDeltas)

    assert(matched.nonEmpty, clue(s"${row.id}: expected typed move-local delta"))
    assert(
      neutralDeltas.forall(delta => delta.objectId != matched.head.objectId || delta.scope != StrategicDeltaScope.MoveLocal),
      clue(s"${row.id}: move-local delta should stay closed without transition truth")
    )
    matched.foreach { delta =>
      delta.projection match
        case StrategicDeltaProjection.MoveLocal(_, witness) =>
          assert(witness.isTransitionAware, clue(s"${row.id}: expected transition-aware witness"))
          assert(witness.matchedSquares.nonEmpty, clue(s"${row.id}: expected matched squares"))
          assert(witness.hasAnchoredEvidence, clue(s"${row.id}: expected anchored move evidence"))
        case other =>
          fail(s"${row.id}: expected move-local projection, got $other")
    }
  }

  test("move-local near-miss rows reject family/owner/scope leakage under non-intersecting move truth") {
    rows.filter(row =>
      row.expectation == "absent" &&
        row.caseType == "near_miss" &&
        parseScope(row.scope) == StrategicDeltaScope.MoveLocal
    ).foreach { row =>
      assert(
        scopeDeltasForRow(row).isEmpty,
        clue(s"${row.id}: move-local near-miss must not emit any ${row.family}/${row.owner}/${row.scope} delta")
      )
    }
  }

  test("comparative deltas carry family-aware witnesses and avoid broad-overlap-only rivals") {
    rows.filter(row => row.expectation == "present" && parseScope(row.scope) == StrategicDeltaScope.Comparative).foreach { row =>
      val matched = deltasForRow(row)
      assert(matched.nonEmpty, clue(s"${row.id} expected comparative delta"))
      matched.foreach { delta =>
        delta.projection match
          case StrategicDeltaProjection.Comparative(_, balance, witness, counterpartObjectIds, profile) =>
            assert(witness.isFamilyAware, clue(s"${row.id}: expected family-aware comparative witness"))
            assert(witness.hasExactCounterpartWitness, clue(s"${row.id}: expected exact counterpart witness"))
            assert(witness.counterpartWitnessKinds.nonEmpty, clue(s"${row.id}: expected counterpart witness kinds"))
            assert(delta.rivalObjectIds.nonEmpty, clue(s"${row.id}: expected rival object ids"))
            assert(counterpartObjectIds.nonEmpty, clue(s"${row.id}: expected counterpart ids"))
            assert(profile.metrics.nonEmpty, clue(s"${row.id}: expected comparative metrics"))
            assertEquals(profile.axis, witness.axis, clue(s"${row.id}: profile axis drift"))
            assert(familyAwareAxis(delta.family, witness.axis), clue(s"${row.id}: axis mismatch for ${delta.family}"))
            assert(
              Set(
                ComparativeStanding.Ahead,
                ComparativeStanding.Behind,
                ComparativeStanding.Balanced,
                ComparativeStanding.Contested
              ).contains(balance.standing),
              clue(s"${row.id}: invalid standing")
            )
          case other =>
            fail(s"${row.id}: expected comparative projection, got $other")
      }
    }

    val broadOverlapObjects = List(manualAccessNetwork(), manualSpaceClampRival())
    val deltas =
      CanonicalStrategicObjectDeltaProjector.project(
        PrimitiveExtractionTest.moveTransitionContract,
        PrimitiveExtractionTest.moveTransitionTruthFrame,
        broadOverlapObjects
      )
    assert(
      !deltas.exists(delta => delta.family == StrategicObjectFamily.AccessNetwork && delta.owner == Color.White && delta.scope == StrategicDeltaScope.Comparative),
      clue("same-sector or shared-file overlap alone must not open white access comparative")
    )
    assert(
      !deltas.exists(delta => delta.family == StrategicObjectFamily.SpaceClamp && delta.owner == Color.Black && delta.scope == StrategicDeltaScope.Comparative),
      clue("same-sector or shared-file overlap alone must not open black clamp comparative")
    )
  }

  test("comparative near-miss rows stay exact-board admissible while remaining taxonomy-distinct from false rivals and strong contrast") {
    val shallowRows =
      rows.filter(row =>
        row.caseType == "near_miss" &&
          row.expectation == "present" &&
          parseScope(row.scope) == StrategicDeltaScope.Comparative
      )

    assert(shallowRows.nonEmpty, clue("expected at least one comparative near-miss row"))
    shallowRows.foreach { row =>
      val familyRows =
        rows.filter(candidate =>
          candidate.family == row.family &&
            parseScope(candidate.scope) == StrategicDeltaScope.Comparative
        )
      val falseRival = familyRows.find(_.caseType == "comparative_false_rival").getOrElse(
        fail(s"${row.id}: missing same-family comparative_false_rival row")
      )
      val strongContrast = familyRows.find(_.caseType == "contrastive").getOrElse(
        fail(s"${row.id}: missing same-family contrastive row")
      )
      val matched = deltasForRow(row)

      assert(matched.nonEmpty, clue(s"${row.id}: shallow comparative must remain projector-admissible"))
      assert(
        row.fen != falseRival.fen,
        clue(s"${row.id}: shallow comparative must not reuse the false-rival board")
      )
      assert(
        row.fen != strongContrast.fen,
        clue(s"${row.id}: shallow comparative must not reuse the strong-contrast board")
      )
      matched.foreach { delta =>
        delta.projection match
          case StrategicDeltaProjection.Comparative(_, _, witness, counterpartObjectIds, profile) =>
            assert(witness.isFamilyAware, clue(s"${row.id}: expected family-aware shallow witness"))
            assert(witness.hasExactCounterpartWitness, clue(s"${row.id}: expected exact counterpart witness"))
            assert(counterpartObjectIds.nonEmpty, clue(s"${row.id}: expected counterpart object ids"))
            assert(delta.rivalObjectIds.nonEmpty, clue(s"${row.id}: expected rival object ids"))
            assert(profile.metrics.nonEmpty, clue(s"${row.id}: expected typed comparative metrics"))
          case other =>
            fail(s"${row.id}: expected comparative projection, got $other")
      }
    }
  }

  test("provisional comparative near-miss rows remain family-complete and typed") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )
    val shallowFamilies =
      rows
        .filter(row =>
          row.caseType == "near_miss" &&
            row.expectation == "present" &&
            parseScope(row.scope) == StrategicDeltaScope.Comparative &&
            provisionalFamilies.contains(parseFamily(row.family))
        )
        .map(row => parseFamily(row.family))
        .toSet

    assertEquals(shallowFamilies, provisionalFamilies.toSet)
  }

  test("provisional direct owners stay move-local closed even with transition truth and reopen comparative only under visible truth") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )

    val moveRows =
      rows.filter(row =>
        row.expectation == "absent" &&
          parseScope(row.scope) == StrategicDeltaScope.MoveLocal &&
          provisionalFamilies.contains(parseFamily(row.family))
      )
    val visibleComparativeRows =
      rows.filter(row =>
        row.expectation == "present" &&
          row.truthCase.contains("primary_visible") &&
          parseScope(row.scope) == StrategicDeltaScope.Comparative &&
          provisionalFamilies.contains(parseFamily(row.family))
      )

    assert(moveRows.nonEmpty, clue("expected provisional move-local rows"))
    assert(visibleComparativeRows.nonEmpty, clue("expected visible comparative provisional rows"))
    moveRows.foreach(row =>
      assert(
        scopeDeltasForRow(row).isEmpty,
        clue(s"${row.id}: move-local should stay closed for the whole ${row.family}/${row.owner}/${row.scope} slice")
      )
    )
    visibleComparativeRows.foreach(row => assert(deltasForRow(row).nonEmpty, clue(s"${row.id}: visible comparative should open")))
  }

  test("P5-T02 audit stays blocked until provisional families gain move-local exact positives and move-local nasty negatives") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )

    provisionalFamilies.foreach { family =>
      val moveRows =
        rows.filter(row =>
          parseFamily(row.family) == family &&
            parseScope(row.scope) == StrategicDeltaScope.MoveLocal
        )
      val caseTypes = moveRows.map(_.caseType).toSet

      assertEquals(
        caseTypes,
        Set("near_miss", "move_local_false_witness"),
        clue(s"$family: move-local reopen should stay blocked without exact/nasty corpus rows")
      )
      assert(
        moveRows.forall(_.expectation == "absent"),
        clue(s"$family: provisional move-local rows should remain fail-closed")
      )
    }
  }

  test("provisional false-witness and false-rival rows stay closed even under forced-stable replay") {
    rows.filter(_.caseType == "move_local_false_witness").foreach { row =>
      val family = parseFamily(row.family)
      val owner = parseColor(row.owner)
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects =
        StrategicObjectSynthesizerTest
          .objectsForFen(row.fen, truth)
          .map(obj =>
            if obj.family == family && obj.owner == owner then obj.copy(readiness = StrategicObjectReadiness.Stable)
            else obj
          )
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      assert(
        scopeDeltas(row, deltas).isEmpty,
        clue(s"${row.id}: strict move-local witness should stay closed even if the family is forced stable")
      )
    }

    rows.filter(_.caseType == "comparative_false_rival").foreach { row =>
      val family = parseFamily(row.family)
      val owner = parseColor(row.owner)
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects =
        StrategicObjectSynthesizerTest
          .objectsForFen(row.fen, truth)
          .map(obj =>
            if obj.family == family && obj.owner == owner then obj.copy(readiness = StrategicObjectReadiness.Stable)
            else obj
          )
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      assert(
        scopeDeltas(row, deltas).isEmpty,
        clue(s"${row.id}: unsupported comparative rival should stay blocked even if the family is forced stable")
      )
    }
  }

  rows.foreach { row =>
    test(s"delta expectation ${row.id}") {
      val matched = deltasForRow(row)
      row.expectation match
        case "present" =>
          assert(matched.nonEmpty, clue(s"${row.id} expected ${row.family}/${row.scope}"))
          matched.foreach(assertRichDelta(row, _))
        case "absent" =>
          assert(
            scopeDeltasForRow(row).isEmpty,
            clue(s"${row.id} expected no ${row.family}/${row.owner}/${row.scope} delta")
          )
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

  private def deltasForRow(
      row: DeltaExpectationRow
  ): List[StrategicObjectDelta] =
    expectedDeltas(row, projectedDeltas(row))

  private def scopeDeltasForRow(
      row: DeltaExpectationRow
  ): List[StrategicObjectDelta] =
    scopeDeltas(row, projectedDeltas(row))

  private def projectedDeltas(
      row: DeltaExpectationRow
  ): List[StrategicObjectDelta] =
    val truth = truthFor(row)
    val contract = contractFor(row)
    CanonicalStrategicObjectDeltaProjector.project(
      contract,
      truth,
      StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    )

  private def expectedDeltas(
      row: DeltaExpectationRow,
      deltas: List[StrategicObjectDelta]
  ): List[StrategicObjectDelta] =
    val scope = parseScope(row.scope)
    val deltaTag = parseDeltaTag(row.deltaTag)

    scopeDeltas(row, deltas).filter(delta =>
      scope == StrategicDeltaScope.Comparative || delta.primaryTag == deltaTag
    )

  private def scopeDeltas(
      row: DeltaExpectationRow,
      deltas: List[StrategicObjectDelta]
  ): List[StrategicObjectDelta] =
    val scope = parseScope(row.scope)
    val family = parseFamily(row.family)
    val owner = parseColor(row.owner)
    val anchor = row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    deltas.filter { delta =>
      delta.family == family &&
      delta.owner == owner &&
      delta.scope == scope &&
      anchor.forall(deltaSquares(delta).contains)
    }

  private def assertRichDelta(
      row: DeltaExpectationRow,
      delta: StrategicObjectDelta
  ): Unit =
    assert(delta.objectId.nonEmpty, clue(s"${row.id}: expected object id"))
    assertEquals(delta.family, parseFamily(row.family), clue(s"${row.id}: family mismatch"))
    assertEquals(delta.owner, parseColor(row.owner), clue(s"${row.id}: owner mismatch"))
    assertEquals(delta.scope, parseScope(row.scope), clue(s"${row.id}: scope mismatch"))
    assertEquals(delta.profile.family, delta.family, clue(s"${row.id}: profile family mismatch"))
    if delta.scope == StrategicDeltaScope.Comparative then
      assert(
        Set(StrategicDeltaTag.ComparativeEdge, StrategicDeltaTag.ComparativeBalance).contains(delta.primaryTag),
        clue(s"${row.id}: invalid comparative tag")
      )
    else assertEquals(delta.primaryTag, parseDeltaTag(row.deltaTag), clue(s"${row.id}: tag mismatch"))
    assert(delta.changedAnchors.nonEmpty, clue(s"${row.id}: expected changed anchors"))
    assert(delta.evidenceRefs.nonEmpty, clue(s"${row.id}: expected evidence refs"))
    row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare).foreach { anchor =>
      assert(deltaSquares(delta).contains(anchor), clue(s"${row.id}: missing anchor $anchor"))
    }
    delta.projection match
      case StrategicDeltaProjection.MoveLocal(_, witness) =>
        assert(witness.isTransitionAware, clue(s"${row.id}: move-local witness must stay typed"))
      case StrategicDeltaProjection.PositionLocal(_, focalAnchorCount) =>
        assert(focalAnchorCount > 0, clue(s"${row.id}: expected focal anchors"))
      case StrategicDeltaProjection.Comparative(_, _, witness, counterpartObjectIds, profile) =>
        assert(witness.isFamilyAware, clue(s"${row.id}: expected comparative witness"))
        assert(witness.hasExactCounterpartWitness, clue(s"${row.id}: expected exact counterpart witness"))
        assert(witness.counterpartWitnessKinds.nonEmpty, clue(s"${row.id}: expected counterpart witness kinds"))
        assert(counterpartObjectIds.nonEmpty, clue(s"${row.id}: expected counterpart ids"))
        assert(delta.rivalObjectIds.nonEmpty, clue(s"${row.id}: expected rival object ids"))
        assert(profile.metrics.nonEmpty, clue(s"${row.id}: expected comparative metrics"))
        if StrategicObjectFamilyContract.forFamily(delta.family).defaultReadiness == StrategicObjectReadiness.Provisional then
          assert(
            profile.metrics.size >= 3,
            clue(s"${row.id}: provisional comparative rows must carry at least three typed metrics")
          )

  private def deltaSquares(
      delta: StrategicObjectDelta
  ): List[Square] =
    (
      delta.changedAnchors.flatMap(_.squares) ++
        delta.changedAnchors.flatMap(_.route.toList.flatMap(_.allSquares)) ++
        delta.evidenceRefs.flatMap(_.anchorSquares) ++
        delta.evidenceRefs.flatMap(_.contestedSquares)
    ).distinct.sortBy(_.key)

  private def familyAwareAxis(
      family: StrategicObjectFamily,
      axis: StrategicComparativeAxis
  ): Boolean =
    family match
      case StrategicObjectFamily.PawnStructureRegime =>
        Set(
          StrategicComparativeAxis.PawnBreakPressureContrast,
          StrategicComparativeAxis.PawnFixationContrast,
          StrategicComparativeAxis.PawnPasserPotentialContrast
        ).contains(axis)
      case StrategicObjectFamily.KingSafetyShell =>
        Set(
          StrategicComparativeAxis.KingShellIntegrityContrast,
          StrategicComparativeAxis.KingEntryPressureContrast
        ).contains(axis)
      case StrategicObjectFamily.DevelopmentCoordinationState =>
        axis == StrategicComparativeAxis.DevelopmentLeadContrast
      case StrategicObjectFamily.PieceRoleFitness =>
        axis == StrategicComparativeAxis.PieceRoleLiabilityContrast
      case StrategicObjectFamily.SpaceClamp =>
        axis == StrategicComparativeAxis.SpaceClampCoverageContrast
      case StrategicObjectFamily.CriticalSquareComplex =>
        axis == StrategicComparativeAxis.CriticalSquareControlContrast
      case StrategicObjectFamily.FixedTargetComplex =>
        Set(
          StrategicComparativeAxis.FixedTargetPressureContrast,
          StrategicComparativeAxis.FixedTargetDefenseContrast
        ).contains(axis)
      case StrategicObjectFamily.BreakAxis =>
        Set(
          StrategicComparativeAxis.BreakAvailabilityContrast,
          StrategicComparativeAxis.BreakSupportRace
        ).contains(axis)
      case StrategicObjectFamily.AccessNetwork =>
        Set(
          StrategicComparativeAxis.AccessRouteContestContrast,
          StrategicComparativeAxis.AccessEntryUsabilityContrast
        ).contains(axis)
      case StrategicObjectFamily.CounterplayAxis =>
        Set(
          StrategicComparativeAxis.CounterplayBreakPressureContrast,
          StrategicComparativeAxis.CounterplayReliefContrast
        ).contains(axis)
      case StrategicObjectFamily.RestrictionShell =>
        axis == StrategicComparativeAxis.RestrictionContainmentContrast
      case StrategicObjectFamily.MobilityCage =>
        axis == StrategicComparativeAxis.MobilityRestrictionContrast
      case StrategicObjectFamily.RedeploymentRoute =>
        Set(
          StrategicComparativeAxis.RedeploymentTempoContrast,
          StrategicComparativeAxis.RedeploymentRouteClarityContrast
        ).contains(axis)
      case StrategicObjectFamily.PasserComplex =>
        Set(
          StrategicComparativeAxis.PasserPromotionRouteContrast,
          StrategicComparativeAxis.PasserEscortContrast
        ).contains(axis)
      case other =>
        fail(s"unexpected direct owner family $other")

  private def truthFor(row: DeltaExpectationRow): lila.llm.analysis.MoveTruthFrame =
    if isVisibleTruthCase(row.truthCase) then
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionVisibleTruthFrame
    else
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionTruthFrameFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionTruthFrame

  private def contractFor(row: DeltaExpectationRow): lila.llm.analysis.DecisiveTruthContract =
    if isVisibleTruthCase(row.truthCase) then
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionVisibleContractFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionVisibleContract
    else
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionContractFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionContract

  private def isVisibleTruthCase(
      truthCase: Option[String]
  ): Boolean =
    truthCase.contains("primary_visible") || truthCase.contains("move_transition_visible")

  private def manualAccessNetwork(): StrategicObject =
    StrategicObject(
      id = "white-access",
      family = StrategicObjectFamily.AccessNetwork,
      owner = Color.White,
      locus = StrategicObjectLocus(files = List(File.C)),
      sector = ObjectSector.Queenside,
      anchors = List(StrategicObjectAnchor(StrategicAnchorKind.File, StrategicAnchorRole.Entry, file = Some(File.C))),
      profile = StrategicObjectProfile.AccessNetwork(Some(File.C), None, Set(chess.Rook), List(Square.C6)),
      supportingPrimitives =
        List(
          PrimitiveReference(
            kind = PrimitiveKind.AccessRoute,
            owner = Color.White,
            anchorSquares = List(Square.C1, Square.C6),
            lane = Some(File.C),
            roles = Set(chess.Rook)
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
          primitiveKinds = Set(PrimitiveKind.AccessRoute),
          primitiveCount = 1,
          anchorSquares = List(Square.C1, Square.C6),
          contestedSquares = List(Square.C6),
          lanes = List(File.C),
          supportingPieceCount = 0,
          rivalCount = 0,
          supportBalance = 1,
          pressureBalance = 1,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.OpenFile, StrategicEvidenceTag.RouteAccess)
        )
    )

  private def manualSpaceClampRival(): StrategicObject =
    StrategicObject(
      id = "black-space",
      family = StrategicObjectFamily.SpaceClamp,
      owner = Color.Black,
      locus = StrategicObjectLocus(files = List(File.C)),
      sector = ObjectSector.Queenside,
      anchors = List(StrategicObjectAnchor(StrategicAnchorKind.File, StrategicAnchorRole.Constraint, file = Some(File.C))),
      profile = StrategicObjectProfile.SpaceClamp(SpaceClampMode.QueensideClamp, List(Square.C5), Set(File.C)),
      supportingPrimitives =
        List(
          PrimitiveReference(
            kind = PrimitiveKind.CriticalSquare,
            owner = Color.Black,
            anchorSquares = List(Square.C5),
            contestedSquares = List(Square.C5)
          )
        ),
      supportingPieces = Nil,
      rivalResourcesOrObjects = Nil,
      relations = Nil,
      stateStrength = StrategicObjectStateStrength(StrategicStrengthBand.Established, 1, 1, 1),
      readiness = StrategicObjectReadiness.Provisional,
      horizonClass = ObjectHorizonClass.Operational,
      evidenceFootprint =
        StrategicObjectEvidenceFootprint(
          primitiveKinds = Set(PrimitiveKind.CriticalSquare),
          primitiveCount = 1,
          anchorSquares = List(Square.C5),
          contestedSquares = List(Square.C5),
          lanes = List(File.C),
          supportingPieceCount = 0,
          rivalCount = 0,
          supportBalance = 1,
          pressureBalance = 1,
          mobilityGain = 0,
          tags = Set(StrategicEvidenceTag.Central)
        )
    )

object StrategicObjectDeltaProjectorTest:

  final case class DeltaExpectationRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      family: String,
      owner: String,
      scope: String,
      expectation: String,
      deltaTag: String,
      anchor: Option[String],
      truthCase: Option[String],
      playedMove: Option[String],
      plannerExpectation: Option[String],
      localizationExpectation: Option[String]
  )

  private given Reads[DeltaExpectationRow] = Json.reads[DeltaExpectationRow]

  val rows: List[DeltaExpectationRow] =
    Source
      .fromResource("strategic-object-corpus/delta-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[DeltaExpectationRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid delta expectation row ${idx + 1}: $err")
      }

  def parseFamily(raw: String): StrategicObjectFamily =
    StrategicObjectSynthesizerTest.parseFamily(raw)

  def parseColor(raw: String): Color =
    StrategicObjectSynthesizerTest.parseColor(raw)

  def parseScope(raw: String): StrategicDeltaScope =
    StrategicDeltaScope.values.find(_.toString.equalsIgnoreCase(raw.replace("_", ""))).getOrElse {
      raw.toLowerCase match
        case "move_local"     => StrategicDeltaScope.MoveLocal
        case "position_local" => StrategicDeltaScope.PositionLocal
        case "comparative"    => StrategicDeltaScope.Comparative
        case _                => throw new IllegalArgumentException(s"unsupported delta scope=$raw")
    }

  def parseDeltaTag(raw: String): StrategicDeltaTag =
    StrategicDeltaTag.values.find(_.toString.equalsIgnoreCase(raw)).getOrElse(
      throw new IllegalArgumentException(s"unsupported delta tag=$raw")
    )
