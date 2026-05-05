# Commentary Docs

This branch is a chess commentary model reset.

Only the documents in this directory root are live authority for current
commentary backend work. There is no live archive authority in this branch.

Live authority is exactly and exhaustively:

- `ChessCommentarySSOT.md`
- `BoardMoodCutLaw.md`
- `BoardMoodSplitLaw.md`
- `ChessModelArchitecture.md`
- `ChessModelContract.md`
- `ChessResetRationale.md`
- `LegacyPruneManifest.md`
- `README.md`
- `StoryInteractionLaw.md`
- `StoryResurrectionLaw.md`

`AGENTS.md`, docs tests, and manifest text must agree with this list. Any
mismatch is a no-go state, not a second source of authority.

## Current No-Go State

This reset is intentionally closed at the public boundary.

Current implementation scope is Stage 0 only. Stages 1-11 are a dependency map
for later work, not permission to open those systems in this branch checkpoint.

- Public route no-go: `/api/commentary/render` and
  `/internal/commentary/render-local-probe` are registered only as fail-closed
  tombstones until an explicit public-surface contract exists. No `200`,
  rendered payload, environment switch, or frontend mock can open them.
- `BoardMood` no-go: no expansion beyond `48` bits, `256` scalars, and `3,328`
  total values. Split/cut re-entry requires a named law and same-board producer
  proof; closed, cut, and split slots otherwise stay `0`/silent.
- No `Story` proof writers: numeric `Proof` scores may rank blocked/context
  `Verdict` rows only. They cannot set `leadAllowed=true` or produce
  `Role.Lead` until same-root side, target, anchor, route, rival, required
  legal line, and proof sidecars exist.
- Proof no-go: missing side, target, anchor, route, rival, required legal line,
  or same-root proof sidecar is a hard public-output block, not weak scoring,
  deferred work, or renderer repair.
- No renderer opening: templates and LLM renderers may verbalize selected
  `Verdict` data only after prerequisite laws and tests exist; they must not
  create, repair, or upgrade chess meaning.
- Old-doc no-go: `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
  `LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
  return as root authority.
- Forbidden-name no-go: new core model, type, module, or docs-authority names
  must not use `Semantic`, non-pawn `Candidate`, `Certification`, `Object`,
  `Delta`, `Selector`, `Pipeline`, `Gate`, `ScoreVector`, or version suffixes.
- Stage order no-go: implementation opens only
  `observation` -> `proof sidecar` -> `Story` -> `Verdict` ->
  `Explanation IR` -> Renderer. Downstream product stages stay closed until
  the earlier authority stage is proven.
- LLM no-go: Engine truth, exact-board validation, legal replay, proof
  sidecars, and `StoryTable` decide what can be said. LLM narration remains
  closed and must not judge chess.

Current implementation blockers are documented, not excused: runtime
enforcement of the full public-output tuple is incomplete, so public output
remains closed until side, target, anchor, route, rival, required legal line,
and same-root proof sidecar are enforced. `Scene.Opening` is context-only and
must not lead over a board-backed `Story`. Old failing tests proved
lower/scaffold/renderer non-upgrade; they did not prove default runtime FEN to
public `Verdict`.

Authority summary:

- `ChessCommentarySSOT.md` defines the single public chess meaning chain:
  `BoardMood` -> `Story` -> `StoryTable` -> `Verdict`.
- `BoardMoodCutLaw.md` closes BoardMood slots that have no live chess fact.
- `BoardMoodSplitLaw.md` closes broad BoardMood slots unless they re-enter as
  smaller exact chess facts.
- `ChessModelArchitecture.md` describes the HCE-style deterministic chess
  model architecture.
- `ChessModelContract.md` is the shape, naming, family, and deletion contract.
- `ChessResetRationale.md` records why the reset exists and which old failure
  modes the new model must not repeat.
- `LegacyPruneManifest.md` records what was pruned and what is intentionally
  preserved during the reset.
- `StoryInteractionLaw.md` defines upper-layer Story family classification and
  nonlinear support, blocker, cap, and override rules.
- `StoryResurrectionLaw.md` defines when a cut BoardMood idea may be spoken by
  a proof-backed Story instead.
