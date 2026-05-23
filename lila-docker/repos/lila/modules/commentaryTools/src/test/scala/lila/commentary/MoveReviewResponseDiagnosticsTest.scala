package lila.commentary

import munit.FunSuite

class MoveReviewResponseDiagnosticsTest extends FunSuite:

  private def polishMeta(
      sourceMode: String,
      reasons: List[String]
  ): MoveReviewPolishMeta =
    MoveReviewPolishMeta(
      provider = "openai",
      model = Some("gpt-test"),
      sourceMode = sourceMode,
      validationPhase = "middlegame",
      validationReasons = reasons,
      cacheHit = false,
      promptTokens = None,
      cachedTokens = None,
      completionTokens = None,
      estimatedCostUsd = None
    )

  private def response(
      commentary: String,
      sourceMode: String,
      reasons: List[String]
  ): CommentResponse =
    CommentResponse(
      commentary = commentary,
      concepts = Nil,
      sourceMode = sourceMode,
      polishMeta = Some(polishMeta(sourceMode, reasons))
    )

  test("marks fallback boundary failures as retryable diagnostics") {
    val diagnostics =
      MoveReviewResponseDiagnostics.json(
        response(
          commentary = "theme: rook lift keeps {seed} alive through a return vector.",
          sourceMode = "fallback_rule_invalid",
          reasons = List("contract_violation")
        )
      )

    assertEquals((diagnostics \ "status").as[String], "retryable_fallback", clue(diagnostics))
    assertEquals((diagnostics \ "sourceModeReason").as[String], "placeholder_leak_detected", clue(diagnostics))
  }

  test("keeps clean deterministic fallback displayable with stable source reason") {
    val diagnostics =
      MoveReviewResponseDiagnostics.json(
        response(
          commentary = "This move keeps the position stable while Black finishes development.",
          sourceMode = "fallback_rule_invalid",
          reasons = List("contract_violation")
        )
      )

    assertEquals((diagnostics \ "status").as[String], "fallback_available", clue(diagnostics))
    assertEquals((diagnostics \ "sourceModeReason").as[String], "contract_violation", clue(diagnostics))
  }

  test("maps circuit-open fallback to a source-mode reason") {
    val diagnostics =
      MoveReviewResponseDiagnostics.json(
        response(
          commentary = "This move develops a piece and keeps the king safer.",
          sourceMode = "rule_circuit_open",
          reasons = List("circuit_open")
        )
      )

    assertEquals((diagnostics \ "status").as[String], "fallback_available", clue(diagnostics))
    assertEquals((diagnostics \ "sourceModeReason").as[String], "polish_circuit_open", clue(diagnostics))
  }

  test("maps empty polish fallback without treating clean fallback prose as retryable") {
    val diagnostics =
      MoveReviewResponseDiagnostics.json(
        response(
          commentary = "This move improves the knight and leaves the position easy to play.",
          sourceMode = "fallback_rule_empty",
          reasons = List("empty_polish")
        )
      )

    assertEquals((diagnostics \ "status").as[String], "fallback_available", clue(diagnostics))
    assertEquals((diagnostics \ "sourceModeReason").as[String], "empty_polish", clue(diagnostics))
  }

  test("normalizes unknown fallback source modes into stable reason codes") {
    val diagnostics =
      MoveReviewResponseDiagnostics.json(
        response(
          commentary = "This move keeps the pressure without changing the material balance.",
          sourceMode = "fallback_rule.Provider Timeout!",
          reasons = Nil
        )
      )

    assertEquals((diagnostics \ "status").as[String], "fallback_available", clue(diagnostics))
    assertEquals((diagnostics \ "sourceModeReason").as[String], "provider_timeout", clue(diagnostics))
  }
