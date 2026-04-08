# Packet: P7-Q02 WhyNow admission

## Scope

- phase anchor: `Phase 7`
- basis / surface: timing-sensitive planner admission
- question kind in scope:
  - `WhyNow`

## Goal

Open `WhyNow` only when timing, transition, release, or delay-sensitive witness
survives certification. Same-sector activity, generic pressure, or broad
position-local salience must not be enough.

## Non-goals

- no general planner wording work
- no external practical metadata layer
- no new timing prose beyond admission semantics

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/QuestionPlanner.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/ClaimCertification.scala`
  - `modules/llm/src/main/scala/lila/llm/strategicobject/StrategicObjectDelta.scala` if timing witness needs typed exposure
- tests:
  - `modules/llm/src/test/scala/lila/llm/strategicobject/QuestionPlannerTest.scala`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - `modules/llm/docs/StrategicObjectModel.md`
  - `modules/llm/docs/CommentaryTrustHardening.md`

## Exact Rows

- positive:
  - transition or release-sensitive rows with real timing witness
- negative:
  - same-sector activity without timing
  - pressure-without-window
  - generic position-local salience

## Validation

- canonical `sbt testOnly` bundle
- `sbt -batch "llm/compile"`

## Exit Criteria

- `WhyNow` is independently admitted from timing witness, not by aliasing
  `WhyThis`
- generic pressure or state-only rows do not open `WhyNow`

## Review Focus

- accidental reopening of already-deferred initiative family semantics
- wording leakage

## Certify Focus

- timing witness correctness
- question admission discipline

## Status Notes

- start status: `ready`
- pass condition: `WhyNow` becomes a real certified question lane
- blocked condition: no stable timing witness contract exists yet
