# Packet: P9-A03 bounded favorable simplification

## Scope

- phase anchor: `Phase 9`
- basis / surface: one bounded favorable-simplification slice
- families in scope:
  - the minimal set needed for a single exact simplification slice

## Goal

Re-earn one bounded favorable-simplification slice on the new spine without
letting the claim expand into theorem-level endgame or conversion rhetoric. The
slice must stay narrow, exact-board grounded, and planner-bounded.

## Non-goals

- no endgame theorem rollout
- no broad conversion doctrine
- no replay or renderer widening

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDelta.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDeltaProjector.scala`
- tests:
  - `build.sbt`
  - `modules/llm/src/test/scala/lila/llm/strategicobject/FavorableSimplificationAdmissionTest.scala`
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/favorable-simplification-expectations.jsonl`
- docs:
  - `.agents/packets/queue.md`
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md` if trust meaning moves

## Exact Rows

- positive:
  - one exact bounded favorable-simplification slice
- negative:
  - simplification picture without favorable contract
- contrastive:
  - same tactical shell but different strategic outcome
- near_miss:
  - simplification exists but the bounded thesis should not surface

## Validation

- packet-specific `sbt testOnly`
- serial targeted regressions through `TargetFixationAdmissionTest`,
  `ClaimCertificationTest`, and `QuestionPlannerTest`
- `sbt -batch "llm/compile"`

## Exit Criteria

- bounded favorable simplification is re-earned on the new spine
- the slice remains bounded and does not reinflate into broad ending rhetoric

## Review Focus

- endgame verdict inflation
- replay or renderer reopening

## Certify Focus

- simplification thesis is exact-board grounded
- bounded containment is preserved

## Status Notes

- start status: `ready`
- finish status: `passed` on `2026-04-09`
- exact slice:
  - one same-task `TradeInvariant` move-local `TradePreserved` slice on
    `curated-exact:k09b` (`d4e6`) now survives object -> delta ->
    certification -> planner as `WhyThis`
- fail-closed closure:
  - `hardening:b16-target-led` stays planner `none`
  - `curated-exact:k10` stays move-local closed and localizes at `object`
  - `hardening:b5-heavy-piece-release` stays absent
- validation:
  - `sbt -batch "llm/testOnly lila.llm.strategicobject.FavorableSimplificationAdmissionTest"` passed
  - `sbt -batch "llm/testOnly lila.llm.strategicobject.TargetFixationAdmissionTest"` passed
  - `sbt -batch "llm/testOnly lila.llm.strategicobject.ClaimCertificationTest"` passed
  - `sbt -batch "llm/testOnly lila.llm.strategicobject.QuestionPlannerTest"` passed
  - `sbt -batch "llm/compile"` passed
- pass condition: one bounded favorable-simplification slice is recovered
  cleanly
- blocked condition: the slice cannot stay bounded on the new spine
