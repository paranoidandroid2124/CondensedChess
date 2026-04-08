package lila.llm.tools.strategicobject

import munit.FunSuite
import play.api.libs.json.Json

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

  test("jsonl render keeps one structured line per trace row") {
    val rows = StrategicObjectExplanationTraceSupport.traceRows.take(3)
    val rendered = StrategicObjectExplanationTraceSupport.renderJsonl(rows)
    val parsed = rendered.trim.split("\n").toList.filter(_.nonEmpty).map(Json.parse)

    assertEquals(parsed.size, rows.size)
    parsed.foreach(js => assert((js \ "rowId").asOpt[String].nonEmpty, clue(js)))
  }
