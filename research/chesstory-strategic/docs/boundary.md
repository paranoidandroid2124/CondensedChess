# Boundary

This project is an offline research lane for strategic-engine work.

## Inputs Allowed

- exported FEN, PGN, JSON, CSV, parquet, or similar offline artifacts
- engine analysis snapshots
- exact-position fixtures
- manually curated evaluation slices

## Inputs Not Allowed

- direct ownership of runtime planner/build/replay control flow
- hidden coupling to live `modules/llm/src/main` behavior
- assumptions that a research score is already trusted user-facing truth

## Output Contract

Outputs from this lane are support artifacts only:

- datasets
- labels
- checkpoints
- evaluation reports
- experiment notes

These outputs are not user-facing truth by default.

## Promotion Requirement

Promotion from research output to runtime behavior requires a separate change
that:

- defines the new runtime boundary
- proves exact-position truth on the intended slice
- updates canonical docs if shipped behavior changes
- passes the relevant trust-hardening gate
