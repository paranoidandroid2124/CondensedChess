# Chesstory Strategic Research

This lane is for offline research on high-level strategic understanding for
Chesstory.

It is intentionally separate from the shipped commentary runtime.

## Non-Goals

This directory does not own:

- live commentary planner/build/replay behavior
- user-facing payload/schema changes
- frontend wording or runtime prompt contracts
- canonical runtime or trust signoff

## Safe Research Scope

This lane may contain:

- exact-position corpus preparation
- deduped FEN and PGN sampling
- search-conditioned label generation
- strategic feature mining
- offline model training
- offline evaluation and report building

## Directory Map

- `docs`
  - boundary notes, dataset design, experiment logs
- `configs`
  - reproducible run and dataset settings
- `scripts`
  - offline data prep and evaluation entry points
- `corpus`
  - local corpora and shards, ignored by default
- `labels`
  - generated labels and slices, ignored by default
- `models`
  - checkpoints and exports, ignored by default
- `eval`
  - local evaluation outputs, ignored by default
- `reports`
  - generated summaries, ignored by default

## Integration Rule

If a result here ever becomes good enough for runtime consideration, move it
through a later, explicit integration change. Do not wire this lane directly
into `lila-docker/repos/lila/modules/llm/src/main`.
