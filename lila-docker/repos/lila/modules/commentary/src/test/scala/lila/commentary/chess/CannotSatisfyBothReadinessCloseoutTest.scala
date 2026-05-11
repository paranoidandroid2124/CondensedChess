package lila.commentary.chess

import java.nio.file.{ Files, Paths }

class CannotSatisfyBothReadinessCloseoutTest extends munit.FunSuite:

  private def law: String =
    Files.readString(Paths.get("modules/commentary/docs/StoryInteractionLaw.md"))

  test("Stage-9 closeout keeps CannotSatisfyBoth internal to proof-readiness"):
    val text = law

    Vector(
      "### Stage-9 CannotSatisfyBoth Readiness Closeout",
      "Stage-9 closes only internal proof-readiness that no legal reply after a complete OverloadTest preserves both duty targets.",
      "- CannotSatisfyBoth proof shape is internal only.",
      "`publicClaimAllowed=false` on CannotSatisfyBoth readiness or",
      "- BoardFacts observes only.",
      "Story writers do not consume CannotSatisfyBoth readiness directly; public",
      "- StoryTable does not order it.",
      "- ExplanationPlan does not lower it.",
      "- Renderer does not phrase it.",
      "- LLM does not see it.",
      "- no public CannotSatisfyBoth Story label",
      "- no CannotSatisfyBoth writer",
      "- no CannotSatisfyBoth speech key",
      "- no CannotSatisfyBoth ExplanationPlan claim key",
      "- no CannotSatisfyBoth renderer template",
      "- no CannotSatisfyBoth LLM prompt path"
    ).foreach: marker =>
      assert(text.contains(marker), s"Stage-9 closeout must pin: $marker")

  test("Stage-9 duplication audit keeps existing proof homes separate"):
    val text = law

    Vector(
      "- CannotSatisfyBoth may depend on OverloadTest readiness, but it does not replace it.",
      "- CannotSatisfyBoth may depend on DualDefenderDuty readiness, but it does not replace it.",
      "- CannotSatisfyBoth may depend on DefenderDuty-style guard relations, but it does not replace them.",
      "- CannotSatisfyBoth is not DefenderDuty.",
      "- CannotSatisfyBoth is not DualDefenderDuty.",
      "- CannotSatisfyBoth is not OverloadTest.",
      "- CannotSatisfyBoth is not `Tactic.Overload`.",
      "- CannotSatisfyBoth is not `Tactic.RemoveGuard`.",
      "- CannotSatisfyBoth is not `Scene.Defense`.",
      "- CannotSatisfyBoth is not `Scene.Material`.",
      "- CannotSatisfyBoth is not `Tactic.Hanging`.",
      "- CannotSatisfyBoth is not `Tactic.Loose`.",
      "- CannotSatisfyBoth is not `Tactic.QueenHit`.",
      "- CannotSatisfyBoth is not `Tactic.Fork`.",
      "- CannotSatisfyBoth is not `Tactic.Skewer`.",
      "- CannotSatisfyBoth is not `Tactic.Pin`.",
      "- CannotSatisfyBoth is not `Tactic.DiscoveredAttack`.",
      "- no second proof home for DefenderDuty relation",
      "- no second proof home for DualDefenderDuty relation",
      "- no second proof home for OverloadTest relation"
    ).foreach: marker =>
      assert(text.contains(marker), s"Stage-9 duplication audit must pin: $marker")
