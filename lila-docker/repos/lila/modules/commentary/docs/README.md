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

Current implementation scope is Stage 6 Closeout Pass.
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
and downstream public surfaces. Stage 5 closeout confirmed Story ordering only
and selected-Verdict handoff only. Stage 6-0 opens only the Explanation Plan
charter and selected-Verdict speech boundary; it does not open sentences,
renderer, LLM, public route `200`, pedagogy, new Story families, or engine
explanation. Stage 6-1 opens only the Explanation Plan shape for one selected
`Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key such as
`can_win_piece`; the first shape carries `bounded` strength and forbidden
wording, not public prose. Stage 6-2 opens only `Tactic.Hanging` allowed claim
mapping. Uncapped Lead Verdict only may carry an allowed claim key. Support,
Context, Blocked, and engine-capped Verdicts do not create standalone public
claims; `engineStrengthLimited` suppresses claim keys and strengthens
forbidden wording. Stage 6-3 opens only forbidden wording boundary.
Explanation Plan must carry the default forbidden wording set.
`Tactic.Hanging` remains bounded material tactic wording only, and
`engineStrengthLimited` strengthens forbidden wording without carrying a claim.
Stage 6-4 opens only Support and Context relation structure. Uncapped Lead
only carries an allowed claim. Support and Context create no standalone public
claim. Blocked remains debug-only relation structure, and proofFailures do not
feed relation wording.
Stage 6-5 opens only the selected Verdict input guard. Explanation Plan accepts
selected Verdict only. Raw BoardFacts, BoardMood, root atoms, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, unselected
Story, unselected Verdict, and source rows remain forbidden inputs.
Stage 6 closeout confirms Explanation Plan only. Blocked, Support, Context,
engine-capped, and engine-refuted Verdicts create no allowed claim or public
claim. Stage 7 deterministic renderer may receive Explanation Plan only and
must not read raw Verdict, EngineCheck, CaptureResult, Board Facts, BoardMood,
raw PV, proofFailures text, source rows, or raw engine evidence directly.
Stages 7-11 remain a dependency map, not permission to open those systems in
this branch checkpoint.

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

`StoryInteractionLaw.md` owns the Stage 6 charter. This README only summarizes
the current scope: Stage 6 is named `Explanation Plan`, with `Explanation IR`
as a parenthetical data-shape label. Stage 6-0 receives selected Verdict data
only to bound claim, evidence, strength, role, support/context relation, and
forbidden wording; raw board facts, engine sidecars, source rows, diagnostics,
renderer wording, and LLM wording remain outside the input boundary.
This README summarizes Stage 6-1 only: one selected `Tactic.Hanging` Lead
Verdict may become an internal `ExplanationPlan` shape with role, scene,
tactic, side, target, anchor, route, allowedClaim, evidenceLine, strength,
forbiddenWording, and empty supportContextLinks. It still writes no sentence.
This README summarizes Stage 6-2 only: uncapped `Tactic.Hanging` Lead
Verdicts may map to a safe structured claim key, while Support, Context,
Blocked, and engine-capped Verdicts create no standalone public claim.
`engineStrengthLimited` suppresses claim keys and tightens forbidden wording.
This README summarizes Stage 6-3 only: Explanation Plan carries the default
forbidden wording set for downstream renderer and LLM layers. `Tactic.Hanging`
remains bounded material tactic wording only, and `engineStrengthLimited`
strengthens forbidden wording without carrying a claim.
This README summarizes Stage 6-4 only: Support and Context enter Explanation
Plan as structured relations to Lead. Support, Context, Blocked, and
engine-capped Verdicts create no standalone public claim. Blocked remains
debug-only relation structure, and proofFailures text does not feed relation
wording.
This README summarizes Stage 6-5 only: Explanation Plan receives selected
Verdict only. It accepts no raw proof material, unselected Story, unselected
Verdict, source row, or proofFailures wording.
This README summarizes Stage 6 closeout only: Stage 6 closes with Explanation
Plan only, without renderer, LLM, public route, user-facing prose, or pedagogy.
Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create
no allowed claim, and Stage 7 deterministic renderer may receive Explanation
Plan only.

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
- Stage 6 Closeout Explanation Plan no-go: the current scope may enforce
  selected Verdict input and closeout boundaries only. It does not open
  `Tactic.Fork`,
  `Scene.Material`, `Scene.Defense`, Plan, Strategy, pedagogical advice,
  deterministic renderer, LLM, public route `200`, engine PV commentary,
  best-move explanation, user-facing prose, or engine explanation.
- No renderer opening: deterministic templates and LLM renderers remain closed.
  They may verbalize selected Explanation Plan data only after prerequisite
  laws and tests exist; they must not create, repair, or upgrade chess meaning.
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
`Tactic.Hanging` Story rows; Stage 6 closeout confirms Explanation Plan only
and Stage 7 deterministic renderer may receive Explanation Plan only.
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
