package lila.commentary.model

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

  test("validateAgainstRequest keeps bookkeeping drift as soft diagnostics") {
    val request = ProbeRequest(
      id = "probe-3",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 16,
      purpose = Some("recapture_branches"),
      objective = Some("compare_recaptures"),
      requiredSignals = List("replyPvs"),
      candidateMove = Some("h2h4"),
      depthFloor = Some(16),
      variationHash = Some("expected-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=16:multipv=1")
    )
    val result = ProbeResult(
      id = "probe-3",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 8,
      bestReplyPv = List("h7h5"),
      replyPvs = Some(List(List("h7h5"))),
      deltaVsBaseline = 0,
      keyMotifs = List("recapture_branching"),
      purpose = Some("keep_tension_branches"),
      objective = Some("compare_tension"),
      probedMove = Some("h2h4"),
      depth = Some(16),
      variationHash = Some("other-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=16:multipv=2")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, true)
    assertEquals(validation.hardReasonCodes, Nil)
    assert(validation.softReasonCodes.contains("PURPOSE_MISMATCH"))
    assert(validation.softReasonCodes.contains("OBJECTIVE_MISMATCH"))
    assert(validation.softReasonCodes.contains("VARIATION_HASH_MISMATCH"))
    assert(validation.softReasonCodes.contains("ENGINE_CONFIG_MISMATCH"))
    assert(validation.reasonCodes.contains("PURPOSE_MISMATCH"))
  }

  test("validateAgainstRequest rejects stale position-bound probe certificates") {
    val request = ProbeRequest(
      id = "probe-4",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_theme_plan"),
      requiredSignals = List("replyPvs"),
      candidateMove = Some("e2e4"),
      depthFloor = Some(18),
      variationHash = Some("req-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=18:multipv=2")
    )
    val result = ProbeResult(
      id = "probe-4",
      fen = Some("4k3/8/8/8/8/8/8/3K4 w - - 0 1"),
      evalCp = 24,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 6,
      keyMotifs = List("theme_plan_validation"),
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_theme_plan"),
      probedMove = Some("e2e4"),
      depth = Some(18),
      variationHash = Some("other-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=18:multipv=2")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assertEquals(
      validation.certificateStatus,
      ProbeContractValidator.ProbeCertificateStatus.Invalid
    )
    assert(validation.hardReasonCodes.contains("FEN_MISMATCH"))
    assert(validation.softReasonCodes.contains("VARIATION_HASH_MISMATCH"))
  }

  test("validateAgainstRequest rejects missing board and move certificate fields") {
    val request = ProbeRequest(
      id = "probe-missing-board",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      requiredSignals = List("replyPvs"),
      candidateMove = Some("e2e4")
    )
    val result = ProbeResult(
      id = "probe-missing-board",
      evalCp = 24,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 6,
      keyMotifs = List("theme_plan_validation"),
      purpose = Some("theme_plan_validation")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)

    assertEquals(validation.isValid, false)
    assert(validation.hardReasonCodes.contains("FEN_UNVERIFIED"))
    assert(validation.hardReasonCodes.contains("PROBED_MOVE_UNVERIFIED"))
  }

  test("validateAgainstRequest requires multi-move probes to certify one requested move") {
    val request = ProbeRequest(
      id = "probe-multi-move",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4", "d2d4"),
      depth = 18,
      purpose = Some("reply_multipv"),
      requiredSignals = List("replyPvs")
    )
    val result = ProbeResult(
      id = "probe-multi-move",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 24,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 6,
      keyMotifs = Nil,
      purpose = Some("reply_multipv"),
      depth = Some(18)
    )

    val missingMove = ProbeContractValidator.validateAgainstRequest(request, result)
    val wrongMove = ProbeContractValidator.validateAgainstRequest(request, result.copy(probedMove = Some("c2c4")))
    val certified = ProbeContractValidator.validateAgainstRequest(request, result.copy(probedMove = Some("d2d4")))

    assertEquals(missingMove.isValid, false)
    assert(missingMove.hardReasonCodes.contains("PROBED_MOVE_UNVERIFIED"))
    assertEquals(wrongMove.isValid, false)
    assert(wrongMove.hardReasonCodes.contains("PROBED_MOVE_MISMATCH"))
    assertEquals(certified.isValid, true, clue(certified))
  }

  test("unknown purpose without required signals fails closed") {
    val result = ProbeResult(
      id = "probe-unknown-purpose",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 10,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 0,
      keyMotifs = Nil,
      purpose = Some("unknown_probe")
    )

    val validation = ProbeContractValidator.validate(result)

    assertEquals(validation.isValid, false)
    assert(validation.hardReasonCodes.contains("NO_REQUIRED_SIGNALS"))
  }

  test("validateAgainstRequest rejects unknown request purpose even with explicit required signals") {
    val request = ProbeRequest(
      id = "probe-unknown-request-purpose",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      purpose = Some("unknown_probe"),
      requiredSignals = List("replyPvs"),
      candidateMove = Some("e2e4"),
      depthFloor = Some(18)
    )
    val result = ProbeResult(
      id = "probe-unknown-request-purpose",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 24,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 6,
      keyMotifs = Nil,
      purpose = Some("unknown_probe"),
      probedMove = Some("e2e4"),
      depth = Some(18)
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)

    assertEquals(validation.isValid, false)
    assert(validation.hardReasonCodes.contains("PURPOSE_CONTRACT_MISSING"))
  }

  test("validateAgainstRequest treats missing depth verification as hard invalid") {
    val request = ProbeRequest(
      id = "probe-5",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      requiredSignals = List("replyPvs"),
      candidateMove = Some("e2e4"),
      depthFloor = Some(18)
    )
    val result = ProbeResult(
      id = "probe-5",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 24,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 6,
      keyMotifs = List("theme_plan_validation"),
      purpose = Some("theme_plan_validation"),
      probedMove = Some("e2e4")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assert(validation.hardReasonCodes.contains("DEPTH_FLOOR_UNVERIFIED"))
  }

  test("purpose-generated motifs and future snapshots do not satisfy required authority signals") {
    val request = ProbeRequest(
      id = "probe-generated",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("c4e5"),
      depth = 18,
      purpose = Some("color_complex_squeeze_validation"),
      requiredSignals = List("replyPvs", "keyMotifs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe-generated",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 20,
      bestReplyPv = List("e8f8"),
      replyPvs = Some(List(List("e8f8"))),
      deltaVsBaseline = 0,
      keyMotifs = List("color_complex_squeeze_validation"),
      purpose = Some("color_complex_squeeze_validation"),
      futureSnapshot =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = Nil,
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("e5"), Nil),
            planBlockersRemoved = Nil,
            planPrereqsMet = List("color_complex_squeeze_validation")
          )
        ),
      generatedRequiredSignals = List("keyMotifs", "futureSnapshot"),
      motifInferenceMode = Some("purpose_only")
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assert(validation.missingSignals.contains("keyMotifs"))
    assert(validation.missingSignals.contains("futureSnapshot"))
    assert(!validation.missingSignals.contains("replyPvs"))
  }

  test("unknown required probe signals fail closed") {
    val request = ProbeRequest(
      id = "probe-unknown-signal",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      requiredSignals = List("replyPvs", "madeUpSignal")
    )
    val result = ProbeResult(
      id = "probe-unknown-signal",
      evalCp = 10,
      bestReplyPv = List("e7e5"),
      replyPvs = Some(List(List("e7e5"))),
      deltaVsBaseline = 0,
      keyMotifs = Nil
    )

    val validation = ProbeContractValidator.validateAgainstRequest(request, result)
    assertEquals(validation.isValid, false)
    assertEquals(validation.missingSignals, List("madeUpSignal"))
  }
