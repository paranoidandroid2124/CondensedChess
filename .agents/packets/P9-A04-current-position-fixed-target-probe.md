# Packet: P9-A04 current-position fixed-target probe

## Scope

- phase anchor: `Phase 9`
- basis / surface: current-position fixed-target probe
- families in scope:
  - `FixedTargetComplex`

## Goal

Open one current-position fixed-target probe only after exact comparative
support and earlier exact slices are stable. The probe must remain certified
position-local only and avoid reintroducing broad scope-shell ownership.

## Non-goals

- no move-local reopening in this packet
- no broad fixed-target doctrine rollout
- no renderer/UI work

## Touched Files

- runtime:
  - `modules/llm/src/main/scala/lila/llm/strategicobject/QuestionPlanner.scala`
  - certification files only if the slice requires it
- tests:
  - planner and certification tests for the chosen probe
- corpus:
  - position-local probe rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`

## Exact Rows

- positive:
  - one exact current-position fixed-target probe
- negative:
  - target exists but probe should not own `WhatMattersHere`
- near_miss:
  - adjacent pressure picture with no fixed target

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- one current-position fixed-target probe is admitted cleanly
- scope-shell or support-only material still cannot choose a primary lane

## Review Focus

- position-local over-widening
- planner admission drift

## Certify Focus

- current-position fixed-target probe remains certified and bounded

## Status Notes

- start status: `ready`
- pass condition: one bounded current-position fixed-target probe is reopened
- blocked condition: the probe still depends on broader planner salvage
