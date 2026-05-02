package lila.commentary.projection

class StrategyProjectionFalsificationHarnessContractTest extends munit.FunSuite:

  test("per-slice falsification harness covers exact near-miss false-rival and shortcut-negative cases"):
    val cases = StrategyProjectionFalsificationHarness.cases
    val casesByRegion = cases.groupBy(_.descriptor.region.regionId).view.mapValues(_.map(_.kind).toSet).toMap

    assertEquals(
      casesByRegion,
      StrategyGeometryFoundation.sliceDescriptors.map(descriptor =>
        descriptor.region.regionId -> StrategyProjectionFalsificationHarness.requiredCaseKinds
      ).toMap
    )

    cases.map(StrategyProjectionFalsificationHarness.run).foreach: result =>
      val label = s"${result.testCase.descriptor.region.regionId}:${result.testCase.kind.key}"
      assertEquals(
        result.runtimeKs.nonEmpty,
        result.testCase.expectedCertifiedTruth,
        clues(label, result.falsificationReasons)
      )
      assertEquals(
        result.admissions.exists(_.admitted),
        result.testCase.expectedPublicAdmission,
        clues(label, result.falsificationReasons, result.admissions.map(_.rejectionReason))
      )
      if !result.testCase.expectedCertifiedTruth then
        assert(result.falsificationReasons.nonEmpty, clues(label))

  test("false-rival harness cases are blocked before K minting, not merely after geometry classification"):
    val falseRivalResults =
      StrategyProjectionFalsificationHarness.cases
        .filter(_.kind == StrategyProjectionFalsificationHarness.CaseKind.FalseRival)
        .map(StrategyProjectionFalsificationHarness.run)

    assert(falseRivalResults.nonEmpty)
    falseRivalResults.foreach: result =>
      assertEquals(result.runtimeKs, Vector.empty, clues(result.testCase.descriptor.region.regionId))
      assert(result.falsificationReasons.contains("sibling_band_rival"), clues(result.falsificationReasons))
