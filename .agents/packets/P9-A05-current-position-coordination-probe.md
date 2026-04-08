# Packet: P9-A05 current-position coordination probe

## Scope

- phase anchor: `Phase 9`
- basis / surface: current-position coordination probe
- families in scope:
  - `DevelopmentCoordinationState`
  - support only where the chosen probe requires it

## Goal

Open one current-position coordination probe only after earlier Phase 9 slices
have demonstrated slice parity on the new spine. The probe must remain exact-
board grounded and avoid overreading generic activity as coordination truth.

## Non-goals

- no move-local reopening in this packet
- no provisional promotion by default
- no renderer/UI work

## Touched Files

- runtime:
  - only the files required by the selected coordination probe
- tests:
  - exact coordination probe tests
- corpus:
  - exact, near-miss, and nasty-negative coordination rows
- docs:
  - `modules/llm/docs/StrategicObjectRoadmap.md`

## Exact Rows

- positive:
  - one exact current-position coordination probe
- negative:
  - active-piece picture without real coordination burden
- near_miss:
  - same-sector activity that should not surface as coordination
- nasty_negative:
  - single-active-piece mirage

## Validation

- packet-specific `sbt testOnly`
- `sbt -batch "llm/compile"`

## Exit Criteria

- one bounded coordination probe is reopened cleanly
- activity-only pictures do not own the probe

## Review Focus

- coordination overread
- hidden move-local reopening

## Certify Focus

- coordination probe stays exact-board bounded
- nasty activity-only negatives remain closed

## Status Notes

- start status: `ready`
- pass condition: one current-position coordination probe is reopened without
  generic-activity inflation
- blocked condition: coordination still cannot be separated from activity-only
  pictures
