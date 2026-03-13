package lila.llm.analysis

import lila.llm.*
import munit.FunSuite

class StrategicIdeaSelectorTest extends FunSuite:

  private def noisyPack(): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      plans = List(
        StrategySidePlan(
          side = "white",
          horizon = "long",
          planName = "Build kingside clamp and space gain"
        )
      ),
      longTermFocus = List("misleading long-term focus about exchange play"),
      signalDigest = Some(
        NarrativeSignalDigest(
          structuralCue = Some("misleading space gain text"),
          latentPlan = Some("misleading favorable exchange text"),
          decision = Some("misleading prophylaxis text"),
          prophylaxisPlan = Some("misleading prevent text"),
          opponentPlan = Some("misleading counterplay text")
        )
      )
    )

  test("typed selector beats legacy keyword fallback on text-only ambiguity") {
    val pack = noisyPack()

    val typed = StrategicIdeaSelector.select(pack, StrategicIdeaSemanticContext.empty("white"))
    val legacy = LegacyStrategicIdeaTextClassifier.select(pack)

    assertEquals(typed, Nil)
    assertEquals(legacy.headOption.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
  }

  test("source registry keeps prose-only fields out of authoritative selector inputs") {
    assert(StrategicIdeaSourceRegistry.authoritative.contains("pawn_analysis"))
    assert(StrategicIdeaSourceRegistry.derivedTyped.contains("classification"))
    assert(StrategicIdeaSourceRegistry.proseOnly.contains("plan_name"))
    assert(!StrategicIdeaSourceRegistry.authoritative.contains("route_purpose"))
  }
