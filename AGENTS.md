# Agent Instructions

For Chesstory commentary work on this branch, treat the backend as a semantic
commentary model reset.

Live commentary documentation lives at:

- `lila-docker/repos/lila/modules/commentary/docs/README.md`
- `lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md`
- `lila-docker/repos/lila/modules/commentary/docs/SemanticModelArchitecture.md`
- `lila-docker/repos/lila/modules/commentary/docs/LegacyArchiveIndex.md`

Documents under
`lila-docker/repos/lila/modules/commentary/docs/legacy-pre-semantic-reset/`
are historical reference only. They do not grant current runtime authority,
selector authority, renderer authority, public claim ownership, or test
acceptance.

## Branch Direction

The target pipeline is:

`feature extraction -> candidate generation -> semantic scoring/gating -> outline -> renderer`

The renderer is deferred and must not create chess meaning.

## Implementation Guardrails

- Preserve exact-board validation, legal replay, owner/anchor/route/scope
  binding, raw-engine/source non-ownership, public-safe line evidence, and
  stale-evidence rejection as hard gates.
- Treat Root/U/Object/Delta/Certification/Projection/Source/Engine artifacts as
  features or evidence unless the new semantic selector explicitly admits them.
- Do not add new Sxx-style selector special cases as live authority.
- Do not promote lower atoms such as `pinned_piece`, `xray_target`, or
  `weak_pawn` into public commentary by themselves.
- Keep renderer changes downstream of `CommentaryOutline`/`CommentaryPlan`.
- If a legacy rule is still needed, restate it in a live semantic-model document
  before relying on it.

## Verification Discipline

- For runtime behavior changes, run targeted commentary tests or explain why
  legacy tests are expected to fail after the document reset.
- Do not treat legacy documentation tests as current acceptance unless they have
  been migrated to the semantic model docs.

