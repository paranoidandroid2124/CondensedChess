# Packet: P6-A02 comparative-quality certification

## Scope

- phase anchor: `Phase 6`
- basis / surface: comparative quality certification and shallow-contrast
  closure
- families in scope:
  - Tier 1 comparative lanes
  - emphasis on same-owner, same-target, same-campaign, or low-separation
    comparative pairs

## Goal

Absorb the blocked `P5-C02` shallow-contrast burden into the certification and
evaluation boundary. An admissible comparative may still exist at projector
level, but this packet must decide when that comparative is too shallow to be
player-facing and therefore must fall to `SupportOnly`, `Deferred`, or
`planner_none` rather than being treated as a real strategic contrast.

## Non-goals

- no projector-side separability widening unless exact-board evidence proves
  certification ownership is impossible
- no planner wording
- no renderer/UI work
- no Tier 2 / Tier 3 widening

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/ClaimCertification.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/QuestionPlanner.scala`
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/ClaimCertificationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/strategicobject/QuestionPlannerTest.scala`
  - `modules/llm/src/test/scala/lila/llm/tools/strategicobject/StrategicObjectExplanationTraceSupportTest.scala`
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - exact comparative rows that remain genuinely distinct in continuation,
    failure mode, or timing
- negative:
  - same-target or same-campaign comparative pairs with no pedagogical
    separability
- contrastive:
  - exact comparative rows whose strategic campaign genuinely differs
- near_miss:
  - admissible counterpart survives, but certification must still refuse a
    primary contrast
- comparative_false_rival:
  - remains blocked
- planner_negative:
  - certified comparative may survive while planner ownership stays closed

## Validation

- `sbt -batch "llm/testOnly lila.llm.strategicobject.ClaimCertificationTest lila.llm.strategicobject.QuestionPlannerTest lila.llm.tools.strategicobject.StrategicObjectExplanationTraceSupportTest"`
- `sbt -batch "llm/Test/runMain lila.llm.tools.strategicobject.StrategicObjectExplanationTraceRunner --tail-risk --tail-risk-threshold=0.98"`
- `sbt -batch "llm/compile"`

## Exit Criteria

- shallow-but-admissible comparative rows have a canonical certification
  outcome
- certified comparative no longer implies player-facing contrast by default
- trace and tail-risk outputs can localize low-separation failures below or at
  certification/planner rather than only at final payload observation
- `P5-C02` remains blocked only as historical ownership discovery, not as an
  unowned active burden

## Review Focus

- certification/planner boundary blur
- hidden projector widening
- wording or renderer creep

## Certify Focus

- comparative quality outcome is board-grounded
- strong exact contrast survives
- shallow exact contrast fails closed without killing legitimate comparative
  lanes

## Status Notes

- start status: `ready`
- pass condition: comparative quality is owned canonically by certification/eval
  rather than unresolved projector heuristics
- blocked condition: the certification boundary still cannot distinguish
  admissible-but-shallow comparative from a truly different strategic campaign
- current status: `blocked`
- latest blocker: `P6-A02a` is now landed, but the certification boundary
  still lacks `P6-A02b` trace localization and `P6-A02c` outcome closure for
  the exact shallow rows
- decomposition note:
  - this umbrella packet is now split into:
    - `P6-A02a-shallow-comparative-corpus`
    - `P6-A02b-shallow-comparative-trace`
    - `P6-A02c-comparative-certification-outcomes`
  - landed sub-packet:
    - `P6-A02a`
      - passed with canonical same-owner shallow-comparative rows on
        `DevelopmentCoordinationState` / `RedeploymentRoute` from
        `exact:redeployment-path`
  - do not retry the umbrella packet directly until those sub-packets land or
    fail conclusively
