package lila.llm.tools.strategicobject

import munit.FunSuite
import play.api.libs.json.Json

import StrategicObjectExplanationTraceSupport.ExplanationTraceRow

class StrategicObjectExplanationTraceSupportTest extends FunSuite:

  test("trace rows cover packet-required case types with row-local localization") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val caseTypes = rows.map(_.caseType).toSet

    assert(rows.nonEmpty, clue("expected non-empty explanation trace"))
    assert(caseTypes.contains("exact"), clue("missing exact trace rows"))
    assert(caseTypes.contains("contrastive"), clue("missing contrastive trace rows"))
    assert(caseTypes.contains("near_miss"), clue("missing near_miss trace rows"))
    assert(caseTypes.contains("nasty_negative"), clue("missing nasty_negative trace rows"))
    assert(rows.forall(_.localization.localizedStage.nonEmpty), clue("every row must localize a pipeline stage"))
  }

  test("exact move-local trace keeps typed projection, witness, certification, and planner bucket") {
    val row =
      StrategicObjectExplanationTraceSupport.traceRows.find(_.rowId == "access-network-move-exact").getOrElse(
        fail("expected access-network-move-exact trace row")
      )

    assertEquals(row.caseType, "exact")
    assertEquals(row.family, "AccessNetwork")
    assertEquals(row.readiness, Some("Stable"))
    assertEquals(row.scope, "move_local")
    assertEquals(row.projection.kind, Some("MoveLocal"))
    assertEquals(row.witness.kind, Some("MoveLocal"))
    assert(row.witness.transitionAware, clue("move-local trace must keep transition-aware witness"))
    assertEquals(row.certification.status, Some("Certified"))
    assertEquals(row.planner.axis, "WhyThis")
    assertEquals(row.planner.admission, "primary")
  }

  test("contrastive trace keeps counterpart witness and certification before sanitization") {
    val row =
      StrategicObjectExplanationTraceSupport.traceRows.find(_.rowId == "fixed-target-comparative-contrastive").getOrElse(
        fail("expected fixed-target comparative trace row")
      )

    assertEquals(row.caseType, "contrastive")
    assertEquals(row.scope, "comparative")
    assertEquals(row.projection.kind, Some("Comparative"))
    assert(row.witness.familyAware, clue("comparative trace must keep family-aware witness"))
    assert(row.witness.exactCounterpartWitness, clue("comparative trace must keep exact counterpart witness"))
    assert(row.projection.metricCount >= 2, clue("comparative trace should keep typed metric count"))
    assertEquals(row.certification.status, Some("Certified"))
    assertEquals(row.planner.admission, "none")
    assertEquals(row.localization.localizedStage, "planner_none")
  }

  test("shared-target comparative rows stay upstream under the live k10 trace") {
    val fixedTarget =
      StrategicObjectExplanationTraceSupport.traceRows.find(_.rowId == "fixed-target-comparative-contrastive").getOrElse(
        fail("expected fixed-target comparative trace row")
      )
    val restriction =
      StrategicObjectExplanationTraceSupport.traceRows.find(_.rowId == "restriction-shell-comparative-contrastive").getOrElse(
        fail("expected restriction-shell comparative trace row")
      )

    assertEquals(fixedTarget.certification.status, Some("Certified"))
    assertEquals(fixedTarget.planner.admission, "none")
    assertEquals(fixedTarget.localization.localizedStage, "planner_none")

    assertEquals(restriction.certification.status, Some("SupportOnly"))
    assertEquals(restriction.planner.admission, "none")
    assertEquals(restriction.localization.localizedStage, "certification")
  }

  test("near-miss and nasty-negative rows stay localized without planner admission drift") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val selected =
      rows.filter(row =>
        row.rowId == "access-network-move-near-miss" ||
          row.rowId == "king-safety-position-nasty-negative"
      )

    assertEquals(selected.size, 2)
    selected.foreach { row =>
      assertEquals(row.planner.admission, "none")
      assert(
        Set("object", "delta", "certification", "absent").contains(row.localization.localizedStage),
        clue(s"${row.rowId} should localize upstream of planner, got ${row.localization.localizedStage}")
      )
    }
  }

  test("shallow comparative rows stay upstream-present while carrying explicit planner-none expectations") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val selected =
      rows.filter(row =>
        row.rowId == "development-comparative-near-miss" ||
          row.rowId == "redeployment-route-comparative-near-miss"
      )

    assertEquals(selected.size, 2)
    selected.foreach { row =>
      assertEquals(row.expectation, "present")
      assertEquals(row.scope, "comparative")
      assertEquals(row.certification.status, Some("SupportOnly"))
      assertEquals(row.planner.admission, "none")
      assertEquals(row.localization.localizedStage, "certification")
      assertEquals(row.traceExpectation.plannerAdmission, Some("none"))
      assertEquals(row.traceExpectation.localizationStage, Some("certification"))
      assert(row.traceExpectationMatch.satisfied, clue(row))
      assertEquals(row.traceExpectationMatch.plannerAdmission, Some(true))
      assertEquals(row.traceExpectationMatch.localizationStage, Some(true))
    }
  }

  test("tail-risk report splits macro averages from hardest-slice planner leak metrics") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val evaluation = StrategicObjectExplanationTraceSupport.tailRiskEvaluation(rows)

    assert(evaluation.passed, clue(evaluation.failures))
    assertEquals(evaluation.macroMetrics.totalRows, rows.size)
    assertEquals(evaluation.macroMetrics.passedRows, rows.size)
    assertEquals(evaluation.tailRisk.totalRows, rows.size)
    assert(evaluation.tailRisk.hardestRows >= 0)
    assert(
      evaluation.macroMetrics.passRate >= 0.98,
      clue(s"expected macro pass rate >=98%, got ${evaluation.macroMetrics.passRate}")
    )
    assertEquals(evaluation.tailRisk.byCaseType.map(_.caseType).toSet, StrategicObjectExplanationTraceSupport.tailRiskCaseTypes)
    assertEquals(evaluation.tailRisk.plannerLeakRows, 0)
  }

  test("hard negative planner-negative rows are blocked from planner admission") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val hardNegativeRows =
      rows.filter(row => StrategicObjectExplanationTraceSupport.tailRiskCaseTypes.contains(row.caseType) && StrategicObjectExplanationTraceSupport.expectsPlannerBlock(row))
    val leaked = hardNegativeRows.filter(_.planner.admission != "none")

    assert(hardNegativeRows.nonEmpty, clue("expected hardest negative rows from trace corpus"))
    assertEquals(leaked.size, 0, clue(leaked.map(_.rowId).mkString(", ")))
  }

  test("tail-risk gate fails on planner leak even with macro pass above threshold") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val seedRow = rows.find(_.rowId == "king-safety-position-nasty-negative").getOrElse(
      fail("expected planner-blocked hardest negative row")
    )
    val corrupted =
      rows.map { row =>
        if row.rowId == seedRow.rowId then withPlannerLeak(row)
        else row
      }
    val evaluation = StrategicObjectExplanationTraceSupport.tailRiskEvaluation(corrupted, 0.98)

    assert(!evaluation.passed, clue(evaluation.failures))
    assert(evaluation.failures.exists(_.contains("planner_negative_leaks")))
    assert(
      evaluation.macroMetrics.passRate >= 0.98,
      clue(s"macro still must stay high: ${evaluation.macroMetrics.passRate}")
    )
  }

  test("tail-risk gate treats packet planner negatives as hard failures without dragging macro below threshold") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows
    val plannerNegative =
      withPlannerLeak(
        rows.find(_.rowId == "development-comparative-near-miss").getOrElse(
          fail("expected shallow comparative trace row")
        ),
        rowId = Some("packet-planner-negative"),
        caseType = Some("planner_negative"),
        expectation = Some("present"),
        admission = "support",
        axis = "WhatChanged",
        claimId = "shallow-comparative-planner-leak",
        plannerExpectation = Some("none"),
        localizationExpectation = Some("planner_none")
      )
    val evaluation = StrategicObjectExplanationTraceSupport.tailRiskEvaluation(rows :+ plannerNegative, 0.98)

    assert(!evaluation.passed, clue(evaluation.failures))
    assert(
      evaluation.macroMetrics.passRate >= 0.98,
      clue(s"macro should stay high: ${evaluation.macroMetrics.passRate}")
    )
    assertEquals(evaluation.tailRisk.plannerLeakRowIds, List("packet-planner-negative"))
    assert(
      evaluation.tailRisk.byCaseType.find(_.caseType == "planner_negative").exists(metric =>
        metric.expectedPlannerBlockedRows == 1 && metric.plannerLeakRows == 1
      ),
      clue(evaluation.tailRisk.byCaseType)
    )
  }

  test("jsonl render keeps one structured line per trace row") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows.take(3)
    val rendered = StrategicObjectExplanationTraceSupport.renderJsonl(rows)
    val parsed = rendered.trim.split("\n").toList.filter(_.nonEmpty).map(Json.parse)

    assertEquals(parsed.size, rows.size)
    parsed.foreach(js => assert((js \ "rowId").asOpt[String].nonEmpty, clue(js)))
  }

  private def withPlannerLeak(
      row: ExplanationTraceRow,
      rowId: Option[String] = None,
      caseType: Option[String] = None,
      expectation: Option[String] = None,
      admission: String = "primary",
      axis: String = "WhatMattersHere",
      claimId: String = "hard-negative-leak",
      plannerExpectation: Option[String] = None,
      localizationExpectation: Option[String] = None
  ): ExplanationTraceRow =
    row.copy(
      rowId = rowId.getOrElse(row.rowId),
      caseType = caseType.getOrElse(row.caseType),
      expectation = expectation.getOrElse("absent"),
      certification = row.certification.copy(status = Some("Certified"), claimId = Some(claimId)),
      planner = row.planner.copy(
        axis = axis,
        admission = admission,
        primaryClaimIds = if admission == "primary" then List(claimId) else Nil,
        supportClaimIds = if admission == "support" then List(claimId) else Nil
      ),
      localization = row.localization.copy(
        localizedStage = if admission == "primary" then "planner_primary" else "planner_support",
        claimMatchCount = 1
      ),
      traceExpectation = row.traceExpectation.copy(
        plannerAdmission = plannerExpectation.orElse(row.traceExpectation.plannerAdmission),
        localizationStage = localizationExpectation.orElse(row.traceExpectation.localizationStage)
      ),
      traceExpectationMatch =
        row.traceExpectationMatch.copy(
          plannerAdmission = plannerExpectation.orElse(row.traceExpectation.plannerAdmission).map(_ => false),
          localizationStage = localizationExpectation.orElse(row.traceExpectation.localizationStage).map(_ => false),
          satisfied = false
        )
    )
