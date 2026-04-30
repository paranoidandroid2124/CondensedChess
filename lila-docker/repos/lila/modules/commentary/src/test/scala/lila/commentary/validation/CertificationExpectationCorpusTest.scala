package lila.commentary.validation

import lila.commentary.root.RootExtractor

class CertificationExpectationCorpusTest extends munit.FunSuite:

  private val rows = CertificationExpectationCorpus.loadAll()
  private val engineProbeRows = EngineProbeExpectationCorpus.loadAll()
  private val caseTypesByFamily = rows.groupMap(_.family)(_.caseType).view.mapValues(_.toSet).toMap
  private val expectationsByFamily =
    rows.groupMap(_.validatedFamily)(_.validatedExpectation).view.mapValues(_.toSet).toMap
  private val engineProbeIds = engineProbeRows.map(_.id).toSet

  test("certification scaffold carries exact, near_miss, nasty_negative, and best_defense_breaks_claim rows for every certification family"):
    CertificationExpectationCorpus.requiredFamilies.foreach: family =>
      val actualCaseTypes = caseTypesByFamily.getOrElse(family, Set.empty)
      assertEquals(
        actualCaseTypes,
        CertificationExpectationCorpus.requiredCaseTypes,
        clues(s"$family case types = ${actualCaseTypes.toVector.sorted.mkString(",")}")
      )

  test("certification scaffold carries the frozen verdict coverage for every certification family"):
    CertificationExpectationCorpus.requiredFamilies.foreach: family =>
      val actualExpectations = expectationsByFamily.getOrElse(family, Set.empty)
      val expectedExpectations =
        CertificationExpectationCorpus.requiredExpectationCoverageByFamily.getOrElse(
          family,
          Set.empty
        )
      assertEquals(
        actualExpectations,
        expectedExpectations,
        clues(s"$family expectations = ${actualExpectations.toVector.sorted.mkString(",")}")
      )

  rows.foreach: row =>
    test(s"certification row parses and matches the frozen certification scaffold for ${row.id}"):
      row.validatedCaseType
      row.validatedExpectation
      row.validatedFamily
      row.validatedOwner
      row.validatedScope
      row.expectedAnchor
      row.validatedBurdenTag
      row.validatedHelpers
      row.validatedRequiredSupportFamilies
      row.validatedEngineRequirement
      row.validatedEnginePurposes
      row.validatedForbiddenShortcuts

      RootExtractor.fromFen(row.normalizedFen).fold(
        message => fail(s"Row ${row.id} FEN parse failed: $message"),
        identity
      )

      assert(
        engineProbeIds.contains(row.id),
        clues(s"${row.id} is missing an engine/probe bundle row")
      )

      row.caseType match
        case "exact" =>
          assertEquals(row.expectation, "certified")
        case "best_defense_breaks_claim" =>
          val allowed =
            if row.family == "CertifiedKingSafetyEdge" then Set("rejected")
            else Set("support_only", "deferred")
          assert(
            allowed.contains(row.expectation),
            clues(
              s"${row.id} best-defense row must degrade to ${allowed.toVector.sorted.mkString(" or ")}"
            )
          )
        case "near_miss" | "nasty_negative" =>
          assertEquals(row.expectation, "rejected")
        case other =>
          fail(s"Unhandled certification case type $other")

  test("fortress certification rows carry explicit hold-oriented probe budgets"):
    val probeRowsById = engineProbeRows.map(row => row.id -> row).toMap
    rows.filter(_.family == "FortressDrawCertification").foreach: row =>
      val probeRow =
        probeRowsById.getOrElse(row.id, fail(s"Missing engine/probe row for ${row.id}"))
      assert(
        probeRow.maxAbsCp.nonEmpty,
        clues(s"${row.id} must keep an explicit fortress hold-oriented eval budget")
      )

  test("certification scaffold keeps the layered corpus directory present"):
    ObjectExpectationCorpus.minimumCorpusFiles.foreach: fileName =>
      assert(
        ObjectExpectationCorpus.resourceExists(fileName),
        clues(s"Missing commentary corpus file $fileName")
      )
