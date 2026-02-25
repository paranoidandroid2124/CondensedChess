package lila.llm.model

import munit.FunSuite

class ProbeContractValidatorTest extends FunSuite:

  test("latent_plan_refutation requires l1Delta and futureSnapshot") {
    val result = ProbeResult(
      id = "probe-1",
      evalCp = 35,
      bestReplyPv = List("e7e5"),
      deltaVsBaseline = -42,
      keyMotifs = List("latent_plan_refutation"),
      purpose = Some("latent_plan_refutation")
    )

    val validation = ProbeContractValidator.validate(result)
    assertEquals(validation.isValid, false)
    assert(validation.missingSignals.contains("l1Delta"))
    assert(validation.missingSignals.contains("futureSnapshot"))
  }

  test("validateAgainstRequest enforces requiredSignals from request") {
    val request = ProbeRequest(
      id = "probe-2",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("a2a4"),
      depth = 18,
      purpose = Some("free_tempo_branches"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe-2",
      evalCp = 12,
      bestReplyPv = List("a7a5"),
      deltaVsBaseline = 5,
      keyMotifs = List("free_tempo_trajectory"),
      purpose = Some("free_tempo_branches")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assertEquals(validation.missingSignals, List("futureSnapshot"))
  }

  test("validateAgainstRequest fails on purpose mismatch") {
    val request = ProbeRequest(
      id = "probe-3",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 16,
      purpose = Some("recapture_branches")
    )
    val result = ProbeResult(
      id = "probe-3",
      evalCp = 8,
      bestReplyPv = List("h7h5"),
      deltaVsBaseline = 0,
      keyMotifs = List("recapture_branching"),
      purpose = Some("keep_tension_branches")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assert(validation.reasonCodes.contains("PURPOSE_MISMATCH"))
  }
