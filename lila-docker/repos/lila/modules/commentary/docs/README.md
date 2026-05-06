# Commentary Docs

This branch is a chess commentary model reset.

Only the documents in this directory root are live authority for current
commentary backend work. There is no live archive authority in this branch.

Live authority is exactly and exhaustively:

- `ChessCommentarySSOT.md`
- `BoardFacts.md`
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

Current implementation scope is Stage 5 Closeout Pass.
Stage 1 Board Facts, Stage 2 Story Proof, and Stage 3 first narrow positive
Story are prerequisites. Stage 3 remains open only for Material proof kernel,
`Tactic.Hanging`, and Hanging negative corpus. Stage 4 is named `Engine Check`.
Stage 4 opens only internal EngineCheck evidence, same-board and stale guards,
`Tactic.Hanging` attachment, false-positive corpus, and conservative StoryTable
diagnostics for existing `Tactic.Hanging` Stories. Stage 5 is named
`Story Order` and opens only role ordering for existing `Tactic.Hanging` Story
rows. Stage 5-1 assigns Lead, Support, Context, and Blocked roles inside that
Hanging-only slice. Stage 5-2 fixes deterministic ordering inputs for those
StoryTable rows. Stage 5-3 tightens close blockers and context relations for
those rows. Stage 5-4 keeps Verdict diagnostics out of public numeric values
and downstream public surfaces. Stage 5 closeout confirms Story ordering only
and selected-Verdict handoff only; Stages 6-11 remain a dependency map, not
permission to open those systems in this branch checkpoint.

`StoryInteractionLaw.md` owns the Stage 3 charter. This README only summarizes
the current scope: backend Material proof evidence plus the named
`Tactic.Hanging` writer are open; other positive families, renderer, LLM, and
public route `200` are still closed.

`StoryInteractionLaw.md` owns the Stage 4 charter. This README only summarizes
the current scope: Story comes first, and engine evidence may check, cap, or
refute only an existing `Tactic.Hanging` Story. Engine eval, engine line, reply
line, and checked move data cannot create a Story, rank a Story, write a
`Verdict`, feed a renderer, feed an LLM, or become public truth.

`StoryInteractionLaw.md` owns the Stage 5 charter. This README only summarizes
the current scope: StoryTable may assign roles among existing
`Tactic.Hanging` Story rows. It does not create Stories, open a new
positive family, or turn engine eval, Board Facts, or `CaptureResult` into
direct public claims.
This README summarizes Stage 5-1 only: complete Hanging rows may lead or
support, refuted/incomplete/captureless rows are blocked, unknown engine checks
create no engine claim, and roles do not open renderer or LLM.
This README summarizes Stage 5-2 only: ordering may use role eligibility,
publicStrength, Story identity, writer presence, and blocked status; it must
not use raw engine eval, raw PV text, proofFailures text, Board Facts row
count, `CaptureResult` text, renderer wording, or input order.
This README summarizes Stage 5-3 only: close Hanging blockers are enforced,
Quiet remains fallback-only, and Source/Opening context cannot outrank
board-backed Hanging; plan, blunder, defense, extra counterplay, and strategy
relations remain closed.
This README summarizes Stage 5-4 only: Verdict values keep their fixed shape,
proofFailures and EngineCheck diagnostics stay out of values,
engineStrengthLimited stays internal, Verdict is not public text, and renderer,
LLM, and public route remain closed.
This README summarizes Stage 5 closeout only: Stage 5 closed as StoryTable
ordering over existing Hanging rows, with no new chess meaning or public
surface, and Stage 6 may receive only selected Verdict data rather than raw
facts, engine sidecars, or capture evidence.

- Public route no-go: `/api/commentary/render` and
  `/internal/commentary/render-local-probe` are registered only as fail-closed
  tombstones until an explicit public-surface contract exists. No `200`,
  rendered payload, environment switch, or frontend mock can open them.
- `BoardMood` no-go: no expansion beyond `48` bits, `256` scalars, and `3,328`
  total values. Split/cut re-entry requires a named law and same-board producer
  proof; closed, cut, and split slots otherwise stay `0`/silent.
- Board Facts no-go: open file, pin, weak square, loose piece, pawn lever,
  attacked piece, king-ring attack, and legal move facts are observations only.
  They are not public claims and must not bypass `Story`.
- Story Proof no-go: `StoryProof` records the minimum proof bundle and missing
  evidence, but only the named `Tactic.Hanging` writer may open a positive
  Story family. Numeric `Proof` scores may rank blocked/context `Verdict` rows
  only. They cannot set `leadAllowed=true` or produce `Role.Lead` without the
  named writer, complete StoryProof, same-board proof, and positive
  `CaptureResult`.
- Proof no-go: missing side, target, anchor, route, rival, required legal line,
  or same-root proof sidecar is a hard public-output block, not weak scoring,
  deferred work, or renderer repair.
- Engine Check no-go: `EngineCheck`, `EngineLine`, and `EngineEval` are
  internal evidence only. Same-board proof, checked move, engine line, reply
  line, eval before, eval after, depth or freshness, status, and missing
  evidence are diagnostics, not public claim ownership.
- Stage 4-2 guard no-go: engine evidence must bind to the same board, the same
  Story route, and the same legal line. Different-FEN engine lines,
  route-mismatched engine lines, stale engine data, depth-missing engine data,
  eval-only input without a Story, and PV-only input without a Story remain
  diagnostic only.
- Stage 4-3 status no-go: EngineCheck attaches only to `Tactic.Hanging`.
  Status is exactly `Unknown`, `Supports`, `Caps`, or `Refutes`. `Supports`
  cannot become winning, best-move, decisive, PV explanation, or public truth;
  `Caps` forbids strong expression; `Refutes` blocks Hanging.
- Stage 5 Story Order no-go: role ordering is limited to existing
  `Tactic.Hanging` Story rows. It does not open `Tactic.Fork`,
  `Scene.Material`, `Scene.Defense`, Plan, Strategy, pedagogical advice,
  Explanation IR, renderer, LLM, public route `200`, engine PV commentary, or
  best-move explanation.
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

Current implementation blockers are documented, not excused: Stage 2 enforces
the full public-output tuple before lead candidacy, and Stage 3 opens only the
named `Tactic.Hanging` writer over positive `CaptureResult`. Stage 4-1 records
engine evidence shape only; Stage 5 role ordering consumes only existing
`Tactic.Hanging` Story rows.
`Scene.Opening` is context-only and must not lead over a board-backed `Story`.
Old failing tests proved lower/scaffold/renderer non-upgrade; they did not
prove default runtime FEN to rendered commentary.

Authority summary:

- `ChessCommentarySSOT.md` defines the single public chess meaning chain:
  `BoardMood` -> `Story` -> `StoryTable` -> `Verdict`.
- `BoardFacts.md` is the Stage 1 charter for board observations that remain
  below public claims.
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
