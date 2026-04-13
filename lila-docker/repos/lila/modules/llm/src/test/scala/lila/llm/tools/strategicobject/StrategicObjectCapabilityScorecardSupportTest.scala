package lila.llm.tools.strategicobject

import munit.FunSuite
import play.api.libs.json.Json

class StrategicObjectCapabilityScorecardSupportTest extends FunSuite:

  test("capability scorecard covers the full 24-family vocabulary") {
    val rows = StrategicObjectCapabilityScorecardSupport.evidenceRows
    val report = StrategicObjectCapabilityScorecardSupport.scorecard(rows)

    assert(rows.nonEmpty, clue("expected non-empty capability evidence rows"))
    assertEquals(report.families.size, 24)
    assertEquals(report.system.familyCount, 24)
  }

  test("frontier families reflect current object-vs-delivery maturity split") {
    val report = StrategicObjectCapabilityScorecardSupport.scorecard()

    val access =
      report.families.find(_.family == "AccessNetwork").getOrElse(
        fail("expected AccessNetwork summary")
      )
    val tradeInvariant =
      report.families.find(_.family == "TradeInvariant").getOrElse(
        fail("expected TradeInvariant summary")
      )
    val counterplay =
      report.families.find(_.family == "CounterplayAxis").getOrElse(
        fail("expected CounterplayAxis summary")
      )
    val conversion =
      report.families.find(_.family == "ConversionFunnel").getOrElse(
        fail("expected ConversionFunnel summary")
      )
    val planRace =
      report.families.find(_.family == "PlanRace").getOrElse(
        fail("expected PlanRace summary")
      )

    assertEquals(access.objectStage, "object")
    assertEquals(access.deliveryStage, "planner_primary")
    assertEquals(access.maturity.level, 3)
    assertEquals(access.maturity.key, "bounded_primary")
    assert(access.byScope.exists(scope => scope.scope == "move_local" && scope.primaryRows >= 1), clue(access.byScope))

    assertEquals(tradeInvariant.objectStage, "object")
    assertEquals(tradeInvariant.deliveryStage, "planner_primary")
    assertEquals(tradeInvariant.maturity.level, 3)
    assertEquals(tradeInvariant.maturity.key, "bounded_primary")

    assertEquals(counterplay.objectStage, "absent")
    assertEquals(counterplay.deliveryStage, "certification")
    assertEquals(counterplay.maturity.level, 2)
    assertEquals(counterplay.maturity.key, "support_only")

    assertEquals(conversion.objectStage, "object")
    assertEquals(conversion.deliveryStage, "absent")
    assertEquals(conversion.maturity.level, 1)
    assertEquals(conversion.maturity.key, "object_only")

    assertEquals(planRace.objectStage, "object")
    assertEquals(planRace.deliveryStage, "absent")
    assertEquals(planRace.maturity.level, 1)
    assertEquals(planRace.maturity.key, "object_only")
  }

  test("fixed-target summary keeps both move-local and position-local primary slices without over-promoting maturity") {
    val report = StrategicObjectCapabilityScorecardSupport.scorecard()
    val fixedTarget =
      report.families.find(_.family == "FixedTargetComplex").getOrElse(
        fail("expected FixedTargetComplex summary")
      )

    assertEquals(fixedTarget.maturity.level, 3)
    assertEquals(fixedTarget.maturity.key, "bounded_primary")
    assert(fixedTarget.primaryRows >= 3, clue(fixedTarget))
    assert(fixedTarget.distinctPrimarySources >= 3, clue(fixedTarget))
    assert(fixedTarget.negativeLeakRows > 0, clue("repeatable promotion should stay blocked while negatives still leak"))
    assert(
      fixedTarget.byScope.exists(scope => scope.scope == "move_local" && scope.primaryRows >= 2),
      clue(fixedTarget.byScope)
    )
    assert(
      fixedTarget.byScope.exists(scope => scope.scope == "position_local" && scope.primaryRows >= 1),
      clue(fixedTarget.byScope)
    )
    assert(
      fixedTarget.byAxis.exists(axis => axis.axis == "WhyThis" && axis.primaryRows >= 2),
      clue(fixedTarget.byAxis)
    )
    assert(
      fixedTarget.byAxis.exists(axis => axis.axis == "WhatMattersHere" && axis.primaryRows >= 1),
      clue(fixedTarget.byAxis)
    )
  }

  test("jsonl evidence rendering keeps one structured row per evidence record") {
    val rows = StrategicObjectCapabilityScorecardSupport.evidenceRows.take(4)
    val rendered = StrategicObjectCapabilityScorecardSupport.renderJsonl(rows)
    val parsed = rendered.trim.split("\n").toList.filter(_.nonEmpty).map(Json.parse)

    assertEquals(parsed.size, rows.size)
    parsed.foreach(js => assert((js \ "rowId").asOpt[String].nonEmpty, clue(js)))
  }
