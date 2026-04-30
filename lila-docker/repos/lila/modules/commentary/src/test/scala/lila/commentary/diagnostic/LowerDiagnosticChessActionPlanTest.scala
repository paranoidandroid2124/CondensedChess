package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class LowerDiagnosticChessActionPlanTest extends munit.FunSuite:

  test("chess action plan turns diagnostic bottlenecks into ordered work items"):
    val plan = LowerDiagnosticChessActionPlan.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val byId = plan.actions.map(action => action.actionId -> action).toMap

    assert(plan.summary.totalRows >= 700, clues(plan.summary))
    assertEquals(plan.summary.standingRows, 286)
    assertEquals(plan.summary.exactTransitionSliceRows, 6)
    assertEquals(plan.summary.rootWitnessSchemaRows, 12)
    assert(byId.contains("open-narrow-exact-transition-tactical-slices"), clues(plan.actions))
    assertEquals(byId("open-narrow-exact-transition-tactical-slices").priority, 2)
    assert(byId("open-narrow-exact-transition-tactical-slices").requiredEvidence.contains("pre-existing anti-case"))
    assert(byId("keep-standing-tactical-smells-support-only").publicAdmission == "support_only")
    assert(byId("harden-current-board-immediate-capture-contract").publicAdmission == "current_board_only_not_move_causal")

  test("materialized-like action plan is blocked on transition reconstruction"):
    val plan = LowerDiagnosticChessActionPlan.fromRows(externalRows())
    val action = plan.actions.head

    assertEquals(plan.summary.totalRows, 3)
    assertEquals(plan.summary.inputBlockedRows, 3)
    assertEquals(plan.summary.exactTransitionSliceRows, 0)
    assertEquals(action.actionId, "recover-transition-identity")
    assertEquals(action.publicAdmission, "blocked")
    assert(action.requiredEvidence.contains("beforeFen"))

  private def externalRows(): Vector[LowerDiagnosticLargeCorpus.Row] =
    val file = Files.createTempFile("action-materialized-like", ".jsonl")
    try
      val lines =
        Vector(
          """{"sampleId":"sample:1","gameKey":"game-1","pgnPath":"C:\\tmp\\sample.pgn","fen":"8/8/8/8/8/8/4k3/7K w - - 0 1","ply":1,"playedUci":"e2e4","family":"DevelopmentCoordinationState"}""",
          """{"sampleId":"sample:2","gameKey":"game-1","pgnPath":"C:\\tmp\\sample.pgn","fen":"4k3/6b1/8/8/3N4/8/8/4K3 w - - 0 1","ply":2,"playedUci":"d4f5","family":"FixedTargetComplex"}""",
          """{"sampleId":"sample:3","gameKey":"game-1","pgnPath":"C:\\tmp\\sample.pgn","fen":"4r2k/8/8/8/8/8/4B3/4K3 w - - 0 1","ply":3,"playedUci":"e2d3","family":"DefenderDependencyNetwork"}"""
        ).mkString(System.lineSeparator())
      Files.writeString(file, lines + System.lineSeparator(), StandardCharsets.UTF_8)
      LowerDiagnosticLargeCorpus.loadExternalRows(file)
    finally Files.deleteIfExists(file)
