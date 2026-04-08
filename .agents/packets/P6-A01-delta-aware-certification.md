# Packet: P6-A01 delta-aware certification

## Scope

- phase anchor: `Phase 6`
- basis / surface: certification burden
- families in scope:
  - all families admitted to certification
  - emphasis on Tier 1 direct owners

## Goal

Move certification beyond readiness-only mapping. A claim should be certified,
support-only, or deferred based on readiness plus the quality of its typed
delta witness, comparative burden, and exact-board support.

## Non-goals

- no renderer wording
- no question phrasing
- no Tier 2 / Tier 3 direct-owner widening
- no raw board reopening

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/ClaimCertification.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDelta.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectFamilyContract.scala` if certification burden becomes canonical there
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/ClaimCertificationTest.scala`
  - `modules/llm/src/test/scala/lila/llm/strategicobject/StrategicObjectDeltaProjectorTest.scala`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - stable Tier 1 delta with strong witness
- negative:
  - weak-witness stable rows demoted from `Certified`
  - provisional rows that must stay `SupportOnly`

## Validation

- canonical `sbt testOnly` bundle
- `sbt -batch "llm/compile"`

## Exit Criteria

- `Stable` is no longer an automatic `Certified`
- weak delta quality can demote a stable family to support-only or deferred
- provisional typed delta still survives as support-only when appropriate

## Review Focus

- accidental collapse back to readiness-only mapping
- planner behavior widened before packet `P7-Q01`

## Certify Focus

- certification burden correctness
- trust-hardening consistency
- no overclaim from stable-but-weak deltas

## Status Notes

- start status: `ready`
- pass condition: certification reads typed delta quality, not readiness alone
- blocked condition: planner or delta projector must change first
