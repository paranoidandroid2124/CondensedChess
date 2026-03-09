package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*

class CriticalAnnotationPolicyTest extends FunSuite:

  private val baseCtx = BookmakerProseGoldenFixtures.rookPawnMarch.ctx

  test("claim prioritization follows tactical meta signals") {
    val tacticalCtx =
      baseCtx.copy(
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Rook-Pawn March", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("Fork"),
                errorSummary = "전술"
              )
            )
          )
        )
      )
    assert(CriticalAnnotationPolicy.shouldPrioritizeClaim(tacticalCtx))
  }

  test("tactical emphasis still requires meaningful loss plus motif, forcing reply, or tactical meta") {
    assert(!CriticalAnnotationPolicy.shouldUseTacticalEmphasis(baseCtx, Thresholds.MISTAKE_CP - 1, missedMotifPresent = true, hasForcingReply = true))
    assert(CriticalAnnotationPolicy.shouldUseTacticalEmphasis(baseCtx, Thresholds.MISTAKE_CP, missedMotifPresent = true, hasForcingReply = false))
    assert(CriticalAnnotationPolicy.shouldUseTacticalEmphasis(baseCtx, Thresholds.MISTAKE_CP, missedMotifPresent = false, hasForcingReply = true))
  }
