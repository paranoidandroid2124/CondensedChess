# Packet: P6-A02a shallow comparative corpus

## Scope

- phase anchor: `Phase 6`
- basis / surface: exact-board shallow-comparative corpus
- families in scope:
  - Tier 1 comparative lanes
  - emphasis on same-owner / same-target / same-campaign comparative pairs

## Goal

Create canonical exact-board rows for the case that blocked `P6-A02`: the rival
is admissible and the comparative exists, but the two sides do not differ
enough instructionally to justify a player-facing contrast. This packet owns
the corpus and case taxonomy only; it does not decide certification outcomes.

## Non-goals

- no certification outcome logic in this packet
- no planner semantics widening
- no projector separability widening
- no renderer/UI work

## Touched Files

- tests:
  - `modules/llm/src/test/scala/lila/llm/tools/strategicobject/...` as needed
- corpus:
  - `modules/llm/src/test/resources/strategic-object-corpus/delta-expectations.jsonl`
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`
  - packet docs if the case taxonomy changes materially

## Exact Rows

- positive:
  - exact comparative rows that remain strongly distinct
- negative:
  - same-target / same-campaign shallow-comparative rows
- contrastive:
  - rows where continuation, failure mode, or timing genuinely differs
- near_miss:
  - admissible counterpart survives, but explanatory separation is weak
- planner_negative:
  - packet-owned shallow comparative rows that should never become player-facing
    primary contrast

## Validation

- packet-specific `sbt testOnly`
- any corpus sanity check needed to confirm the new row classes are distinct

## Exit Criteria

- at least one canonical exact-board shallow-comparative row exists that is
  not just a `comparative_false_rival` duplicate
- the row taxonomy distinguishes:
  - fake rival
  - real rival but shallow contrast
  - real rival and strong contrast

## Review Focus

- duplicate rows disguised as new shallow-comparative evidence
- packet drift into certification logic

## Certify Focus

- row classes are board-grounded and genuinely distinct
- shallow-comparative rows are not just counterpart-gate failures

## Status Notes

- start status: `ready`
- pass condition: canonical shallow-comparative exact-board corpus exists
- blocked condition: every candidate row collapses back into
  `comparative_false_rival` or ordinary contrastive support
- current status: `passed`
- landed rows:
  - `development-comparative-near-miss`
  - `redeployment-route-comparative-near-miss`
- landed basis:
  - both rows use the exact board from `exact:redeployment-path`
  - both rows keep an admissible same-owner comparative counterpart with exact
    `SharedFile`, `SharedRoute`, and `SharedSquare` witness
  - both rows remain taxonomy-distinct from the same-family
    `comparative_false_rival` rows and the stronger
    `contrastive:redeployment-path` rows
