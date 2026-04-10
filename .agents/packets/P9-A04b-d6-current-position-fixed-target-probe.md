# Packet: P9-A04b d6 current-position fixed-target probe

## Scope

- phase anchor: `Phase 9`
- basis / surface: current-position fixed-target probe
- families in scope:
  - `FixedTargetComplex`

## Goal

Add the packet-owned `d6` current-position fixed-target probe on the same
centralized runtime boundary while preserving the existing B15A `c6` exact
probe.

## Non-goals

- no broad fixed-target doctrine rollout
- no generic campaign-threading
- no planner salvage
- no renderer/UI work

## Exact Rows

- positive:
  - packet-owned `d6` current-position fixed-target exact row
- preserved:
  - existing B15A `c6` exact row
- negative:
  - target exists but probe should not own `WhatMattersHere`
- near_miss:
  - adjacent pressure picture with no fixed target

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- the packet-owned `d6` current-position fixed-target probe is admitted cleanly
- the existing B15A `c6` probe still admits cleanly on the same boundary

## Status Notes

- status: `passed`
- result:
  - `CurrentPositionProbeSlice` now preserves the existing B15A `c6` exact
    probe and admits the packet-owned `d6` exact probe on the same centralized
    boundary
  - packet-owned `d6` current-position fixed-target admission is exact and
    bounded; generic fixed-target doctrine, campaign-threading, planner
    salvage, and renderer wording reopening remain closed
