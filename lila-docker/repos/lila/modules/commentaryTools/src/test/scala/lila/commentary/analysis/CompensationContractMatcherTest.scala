package lila.commentary.analysis

import munit.FunSuite

class CompensationContractMatcherTest extends FunSuite:

  import StrategyPackSurface.CompensationSubtype

  test("compensation subtype support fails closed for unknown subtype dimensions") {
    val text = "The compensation comes from fixed queenside targets."

    assert(
      !CompensationContractMatcher.supportsSubtype(
        text,
        CompensationSubtype("unknown", "target_fixing", "immediate", "durable_pressure")
      )
    )
    assert(
      !CompensationContractMatcher.supportsSubtype(
        text,
        CompensationSubtype("queenside", "unknown", "immediate", "durable_pressure")
      )
    )
    assert(
      !CompensationContractMatcher.supportsSubtype(
        text,
        CompensationSubtype("queenside", "target_fixing", "unknown", "durable_pressure")
      )
    )
    assert(
      !CompensationContractMatcher.supportsSubtype(
        text,
        CompensationSubtype("queenside", "target_fixing", "immediate", "unknown")
      )
    )
  }

  test("delayed compensation support needs an explicit recovery/defer anchor") {
    val subtype = CompensationSubtype("queenside", "target_fixing", "delayed", "durable_pressure")

    assert(!CompensationContractMatcher.supportsSubtype("The compensation comes from fixed queenside targets.", subtype))
    assert(
      CompensationContractMatcher.supportsSubtype(
        "Material can wait while the fixed queenside targets stay tied down.",
        subtype
      )
    )
  }

  test("immediate structural compensation still accepts matching concrete subtype text") {
    assert(
      CompensationContractMatcher.supportsSubtype(
        "The compensation comes from fixed queenside targets.",
        CompensationSubtype("queenside", "target_fixing", "immediate", "durable_pressure")
      )
    )
  }
