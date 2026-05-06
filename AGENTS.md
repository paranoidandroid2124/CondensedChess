# Agent Instructions

For Chesstory commentary work on this branch, treat the backend as a
proof-first chess-story kernel. It is not the full commentary product yet.

Live commentary documentation authority is exactly and exhaustively:

- `lila-docker/repos/lila/modules/commentary/docs/ChessCommentarySSOT.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelArchitecture.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessModelContract.md`
- `lila-docker/repos/lila/modules/commentary/docs/ChessResetRationale.md`
- `lila-docker/repos/lila/modules/commentary/docs/BoardFacts.md`
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

Current implementation scope is Stage 3 first narrow positive Story only.
Stage 1 Board Facts and Stage 2 Story Proof are prerequisites. Stage 3 is open
only for Material proof kernel, `Tactic.Hanging`, and Hanging negative corpus;
Stages 4-11 remain a dependency map, not permission to open those systems.

`StoryInteractionLaw.md` is the single live authority for the Stage 3 charter.
All other documents may summarize scope, but must not create a second Stage 3
rule text. The active Stage 3 scope opens backend Material proof evidence and
the named `Tactic.Hanging` writer only; `Tactic.Fork`, `Scene.Material`,
`Scene.Defense`, Plan, Strategy, renderer, LLM, public route `200`, and strong
wording remain closed there.

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
- Only `Tactic.Hanging` positive `Story` writer is live: proof numbers remain
  non-authoritative for public speech unless the named `Tactic.Hanging` writer,
  complete StoryProof, same-board proof, and positive `CaptureResult` are all
  present.
- Proof no-go: missing side, target, anchor, route, rival, required legal line,
  or same-root proof sidecar is a hard public-output block, not weak scoring or
  renderer repair.
- No renderer opening: templates and LLM renderers may verbalize selected
  `Verdict` data only after prerequisite laws and tests exist; they cannot
  repair, upgrade, or invent chess meaning.
- Stage 3 first scope only: do not implement `Tactic.Fork`, `Scene.Material`,
  `Scene.Defense`, Plan, Strategy, King attack, Conversion, Blunder, renderer
  opening, LLM narration, public commentary route `200`, broad scalar re-entry,
  source/engine public-truth paths, or CTH-style family exceptions.
- Board Facts no-go: open file, pin, weak square, loose piece, pawn lever,
  attacked piece, king-ring attack, and legal move facts are observations only.
  They are not public claims and must not bypass `Story`.
- Old-doc no-go: `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
  `LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
  return as root authority.
- Forbidden-name no-go: new core model, type, module, or docs-authority names
  must not use `Semantic`, non-pawn `Candidate`, `Certification`, `Object`,
  `Delta`, `Selector`, `Pipeline`, `Gate`, `ScoreVector`, or version suffixes.

## Implementation Guardrails

- Authority consolidation is mandatory: `One chess meaning, one home`;
  `One observation family, one owner`; `One public claim, one proof path`.
- Stage 2 ownership split is mandatory: `Story owns identity.`
  `StoryProof owns proof and missing evidence.` `Verdict carries the result.`
  `StoryProof` must not own or duplicate `side`, `target`, `anchor`, `route`,
  or `rival`.
- Before adding any new core type, module, row, or docs-authority name, ask
  whether the chess meaning is truly new, whether an existing Fact can carry it
  as a field, whether the name creates new authority, whether the same board
  phenomenon would now have two owners, and whether a later `Story` proof would
  know which input to trust.
- Before adding any new type, module, row, or field, classify the information
  as Story identity, StoryProof evidence, or Verdict result. If it is side,
  target, anchor, route, or rival, it belongs to `Story`, not `StoryProof`.
- proofFailures are internal diagnostics only.
- They may be used for tests, debugging, and missing-proof coordinates, but
  must not become public JSON, renderer input, or LLM input.
- New names are the last resort. Prefer extending `FileFact`, `PieceContact`,
  `LineFact`, or the existing owner for the observation family when the chess
  meaning is already housed there.
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
