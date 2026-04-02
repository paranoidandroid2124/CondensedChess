# Research Workspace

This directory is the root umbrella for offline and non-shipped research in
this repository.

Use it when work should stay physically and operationally separate from the
live Chesstory commentary runtime.

## Why This Lives At Repo Root

- It keeps research out of
  `lila-docker/repos/lila/modules/llm/src/main`.
- It keeps research separate from existing runtime-adjacent tooling under
  `src/test`.
- It gives future research lanes one stable home instead of creating more
  top-level one-off directories.

## Root Folder Rule

Create new research efforts as children of this directory, not as new
top-level repo folders.

Current lane:

- `research/chesstory-strategic`

If more lanes appear later, keep them as siblings here, for example:

- `research/opening-study`
- `research/endgame-labeling`
- `research/commentary-eval`

## Hard Boundary

Anything in `research/` is offline-only unless a later, explicit runtime
integration change proves otherwise.

Research work here must not:

- change shipped API or frontend contracts
- become a dependency of the live planner/build/replay path
- rewrite canonical runtime docs as if research output were already live
- bypass truth-gate or trust-hardening review

## Typical Contents

Track code and docs that define the research process:

- scripts
- configs
- small fixtures
- reproducible notes

Do not track bulky generated artifacts by default:

- large corpora
- model checkpoints
- training caches
- raw eval dumps
