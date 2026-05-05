# Agent Instructions

For Chesstory commentary work on this branch, treat the backend as a
proof-first chess-story kernel. It is not the full commentary product yet.

Live commentary documentation authority is exactly and exhaustively:

- `lila-docker/repos/lila/modules/commentary/docs/ChessCommentarySSOT.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelArchitecture.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelContract.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessResetRationale.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardMoodCutLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardMoodSplitLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/StoryInteractionLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/StoryResurrectionLaw.md`
- `lila-docker/repos/lila/modules/commentary/docs/LegacyPruneManifest.md`
- `lila-docker/repos/lila/modules/commentary/docs/README.md`

`AGENTS.md`, `modules/commentary/docs/README.md`, `LegacyPruneManifest.md`,
and docs tests must agree on this list. Any mismatch is a no-go state, not a
second source of authority.

Documents under
`lila-docker/repos/lila/modules/commentary/docs/legacy-pre-semantic-reset/`
are historical reference only. They do not grant current runtime authority,
selection authority, renderer authority, public claim ownership, or test
acceptance.

## Branch Direction

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said; LLM phrasing
comes after that.

Engine lines, mate/tablebase proof, SEE, and bounded material results are
truth-oracle evidence for backend proof. Raw engine numbers and engine text are
never public claim owners. Backend policy owns proof, Story selection,
arbitration, and pedagogy. The LLM may phrase selected `Verdict` or
`Explanation IR` data only; it must not decide, prove, rank, repair, or invent
chess meaning.

The live authority chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

No other public path owns current chess meaning. Outline and renderer work is
downstream of selected `Verdict` data only and must not create chess meaning.

The branch slogan is:

- `BoardMood` observes.
- `Story` proves.
- `StoryTable` arbitrates.
- `Verdict` speaks.
- Renderer only phrases.

Core proof boundary:

- feature is not a claim
- claim is not a public `Story`
- public `Story` requires proof-bearing identity

Implementation must open in this order only:

`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer

Do not implement downstream product stages before earlier authority stages are
proven.

Current implementation scope is Stage 0 only. Stages 1-11 are a dependency map,
not permission to open those systems.

Forbidden dependency shortcuts:

- Renderer before Story proof sidecar is forbidden.
- LLM before Explanation IR is forbidden.
- Strategy before tactical/material proof is forbidden.
- Pedagogy before causal arbitration is forbidden.
- Personalization before stable Story taxonomy is forbidden.

## Current No-Go State

- Public route no-go: `/api/commentary/render` and
  `/internal/commentary/render-local-probe` are registered only as fail-closed
  tombstones until an explicit public-surface contract exists. No `200`,
  rendered payload, environment switch, or frontend mock can open them.
- No `BoardMood` Sxxx expansion or re-entry: closed, cut, and split slots stay
  `0`/silent; there is no expansion beyond `48` bits, `256` scalars, and
  `3,328` total values unless a live authority document and docs test
  explicitly open a smaller exact chess fact with same-board producer proof.
- No `Story` proof writers: proof numbers remain non-authoritative until
  same-root side, target, anchor, route, rival, required legal line, and proof
  sidecars and their tests exist.
- Proof no-go: missing side, target, anchor, route, rival, required legal line,
  or same-root proof sidecar is a hard public-output block, not weak scoring or
  renderer repair.
- No renderer opening: templates and LLM renderers may verbalize selected
  `Verdict` data only after prerequisite laws and tests exist; they cannot
  repair, upgrade, or invent chess meaning.
- Stage 0 only: do not implement renderer opening, LLM narration, public
  commentary route `200`, broad scalar re-entry, strategic Story openings,
  source/engine public-truth paths, or CTH-style family exceptions.
- Old-doc no-go: `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
  `LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
  return as root authority.
- Forbidden-name no-go: new core model, type, module, or docs-authority names
  must not use `Semantic`, non-pawn `Candidate`, `Certification`, `Object`,
  `Delta`, `Selector`, `Pipeline`, `Gate`, `ScoreVector`, or version suffixes.

## Implementation Guardrails

- Preserve exact-board validation, legal replay, owner/anchor/route/scope
  binding, raw-engine/source non-ownership, public-safe line evidence, and
  stale-evidence rejection as hard gates.
- Treat legacy artifacts as features or evidence only when a live authority
  document explicitly admits them into `BoardMood` or `Story` proof.
- Ask whether a feature can become a `Story` with side, target, anchor, route,
  rival, required legal line, and same-root proof. If not, do not speak it.
- Do not add new Sxx-style special cases as live authority.
- Do not promote lower atoms such as `pinned_piece`, `xray_target`, or
  `weak_pawn` into public commentary by themselves.
- Keep renderer changes downstream of selected `Verdict` data only.
- If a legacy rule is still needed, restate it in a live chess-model document
  before relying on it.

## Verification Discipline

- For runtime behavior changes, run targeted commentary tests or explain why
  legacy tests are expected to fail after the document reset.
- Do not treat legacy documentation tests as current acceptance unless they have
  been migrated to the live chess-model docs.
