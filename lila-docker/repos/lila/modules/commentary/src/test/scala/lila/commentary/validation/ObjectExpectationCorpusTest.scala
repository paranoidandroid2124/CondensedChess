package lila.commentary.validation

import lila.commentary.root.RootExtractor

class ObjectExpectationCorpusTest extends munit.FunSuite:

  private val rows = ObjectExpectationCorpus.loadAll()
  private val caseTypesByFamily = rows.groupMap(_.family)(_.caseType).view.mapValues(_.toSet).toMap

  test("commentary corpus keeps the minimum layered files present"):
    ObjectExpectationCorpus.minimumCorpusFiles.foreach: fileName =>
      assert(
        ObjectExpectationCorpus.resourceExists(fileName),
        s"Missing commentary corpus file $fileName"
      )

  test("object scaffold keeps exactly one exact, near_miss, and nasty_negative row per family"):
    ObjectExpectationCorpus.requiredFamilies.foreach: family =>
      val actualRows = rows.filter(_.family == family)

      assertEquals(actualRows.size, ObjectExpectationCorpus.requiredCaseTypes.size)
      assertEquals(
        actualRows.map(_.caseType).sorted,
        ObjectExpectationCorpus.requiredCaseTypes.toVector.sorted
      )

  rows.foreach: row =>
    test(s"object scaffold row parses for ${row.id}"):
      row.validatedCaseType
      row.validatedExpectation
      row.validatedFamily
      row.validatedOwner
      row.anchorKind
      row.validatedPressureTarget
      row.validatedHelpers

      RootExtractor.fromFen(row.normalizedFen).fold(
        message => fail(s"Row ${row.id} FEN parse failed: $message"),
        _ => ()
      )

      if row.caseType == "exact" then assertEquals(row.expectation, "present")
      else assertEquals(row.expectation, "absent")

  test("Object 7 scaffold carries exact, near_miss, and nasty_negative coverage for every family"):
    ObjectExpectationCorpus.requiredFamilies.foreach: family =>
      val actualCaseTypes = caseTypesByFamily.getOrElse(family, Set.empty)
      assert(
        actualCaseTypes == ObjectExpectationCorpus.requiredCaseTypes,
        s"$family case types = ${actualCaseTypes.toVector.sorted.mkString(",")}"
      )
