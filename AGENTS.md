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

Current implementation scope is Stage 6 Closeout Pass.
Stage 1 Board Facts, Stage 2 Story Proof, and Stage 3 first narrow positive
Story are prerequisites. Stage 3 remains open only for Material proof kernel,
`Tactic.Hanging`, and Hanging negative corpus. Stage 4 opens only
`EngineCheck`, `EngineLine`, and `EngineEval` as internal evidence, same-board
and stale guards, `Tactic.Hanging` attachment, false-positive corpus, and
conservative StoryTable diagnostics for existing `Tactic.Hanging` Stories.
Stage 5 opens only StoryTable role ordering for existing `Tactic.Hanging`
Story rows. Stage 5-1 assigns Lead, Support, Context, and Blocked roles for
those rows only. Stage 5-2 fixes deterministic ordering inputs for those
StoryTable rows. Stage 5-3 tightens close Hanging blockers and context
relations for those StoryTable rows. Stage 5-4 keeps Verdict diagnostics out
of public numeric values and downstream public surfaces. Stage 5 closeout
confirmed Story ordering only and selected-Verdict handoff only. Stage 6-0
opens only the Explanation Plan charter and selected-Verdict speech boundary.
Stage 6 is named `Explanation Plan`; docs may also say `Explanation IR` when
describing the downstream data shape. Stage 6-0 does not write sentences or
open renderer, LLM, public route `200`, pedagogy, a new Story family, or
engine explanation. Stage 6-1 opens only the Explanation Plan shape for one
selected `Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key
such as `can_win_piece`; the first shape carries `bounded` strength and
forbidden wording, not public prose. Stage 6-2 opens only `Tactic.Hanging`
allowed claim mapping. Uncapped Lead Verdict only may carry an allowed claim
key. Support, Context, Blocked, and engine-capped Verdicts do not create
standalone public claims; `engineStrengthLimited` suppresses claim keys and
strengthens forbidden wording. Stage 6-3 opens only forbidden wording
boundary. Explanation Plan must carry the default forbidden wording set.
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
Stages 7-11 remain a dependency map, not permission to open those systems.

`StoryInteractionLaw.md` is the single live authority for the Stage 3 charter.
All other documents may summarize scope, but must not create a second Stage 3
rule text. The active Stage 3 scope opens backend Material proof evidence and
the named `Tactic.Hanging` writer only; `Tactic.Fork`, `Scene.Material`,
`Scene.Defense`, Plan, Strategy, renderer, LLM, public route `200`, and strong
wording remain closed there.

`StoryInteractionLaw.md` is the single live authority for the Stage 4 charter.
Stage 4 is named `Engine Check`. Story comes first. Engine checks, caps, or
refutes. Engine never speaks alone.

Stage 4-2 adds same-board and stale engine guards. Engine evidence must bind
to the same board, the same Story route, and the same legal line. Different
FENs, route-mismatched engine lines, stale engine data, depth-missing engine
data, eval-only input without a Story, and PV-only input without a Story are
diagnostic only.

Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`. EngineCheck status is
exactly `Unknown`, `Supports`, `Caps`, or `Refutes`. `Unknown` forbids engine
expression, `Supports` creates no new claim, `Caps` forbids strong expression,
and `Refutes` blocks the Hanging Story. Engine support must not become winning,
best-move, decisive, PV explanation, or public truth wording.

`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter.
All other documents may summarize scope, but must not create a second Stage 5
rule text. Stage 5 is named `Story Order` and may be described as StoryTable
Arbitration. In this first scope, StoryTable assigns Lead, Support, Context,
and Blocked only among existing `Tactic.Hanging` Story rows. It does not
create Stories or new public claims. Renderer, LLM, Explanation IR, public
route `200`, engine PV commentary, best-move explanation, `Tactic.Fork`,
`Scene.Material`, `Scene.Defense`, Plan, Strategy, and pedagogy remain closed.
Stage 5-1 Hanging role rules also live there; other documents may summarize
that complete Hanging rows can lead or support, refuted/incomplete/captureless
rows are blocked, unknown engine checks create no engine claim, and roles do
not open renderer or LLM.
Stage 5-2 deterministic ordering rules also live there; other documents may
summarize that ordering may use role eligibility, publicStrength, Story
identity, writer presence, and blocked status, but not raw engine eval, raw PV
text, proofFailures text, Board Facts row count, `CaptureResult` text, renderer
wording, or input order.
Stage 5-3 conflict and block rules also live there; other documents may
summarize that close Hanging blockers, Quiet fallback, and Source/Opening
context cannot outrank board-backed Hanging, while Plan, Blunder, Defense,
Counterplay-beyond-Caps, and Strategy relations remain closed.
Stage 5-4 Verdict diagnostic boundary also lives there; other documents may
summarize that `Verdict.values` keeps its fixed shape, proofFailures and
EngineCheck diagnostics stay out of values, `engineStrengthLimited` is
internal, Verdict is not public text, and renderer, LLM, and public route stay
closed.
Stage 5 closeout also lives there; other documents may summarize that Stage 5
closed as StoryTable ordering only, with no new chess meaning, no downstream
public surface, and Stage 6 limited to selected Verdict handoff rather than raw
facts or engine sidecars.

`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter.
All other documents may summarize scope, but must not create a second Stage 6
rule text. Stage 6 is named `Explanation Plan`, with `Explanation IR`
permitted as a parenthetical data-shape label. Stage 6-0 receives selected `Verdict` data only;
it may bound claim, evidence, strength, role, support/context relation, and
forbidden wording. Raw Board Facts, raw
BoardMood, root atoms, `CaptureResult`, `EngineCheck`, `EngineEval`,
`EngineLine`, raw PV, proofFailures text, source rows, renderer wording, and
LLM wording remain forbidden inputs. Deterministic renderer, LLM narration,
public route `200`, user-facing prose, pedagogy, new Story families, and engine
explanation remain closed.
Stage 6-1 may summarize only that one selected `Tactic.Hanging` Lead Verdict
can become an internal `ExplanationPlan` shape with role, scene, tactic, side,
target, anchor, route, allowedClaim, evidenceLine, strength,
forbiddenWording, and empty supportContextLinks. `allowedClaim` stays a
structured key such as `can_win_piece`; the first shape carries `bounded`
strength and forbidden wording, not public prose. Full sentence generation,
user-facing prose, `engine says`, best move, winning, decisive, and public eval
remain closed.
Stage 6-2 may summarize only that uncapped `Tactic.Hanging` Lead Verdicts can
carry a safe allowed claim key, while Support, Context, Blocked, and
engine-capped Verdicts create no standalone public claim.
`engineStrengthLimited` suppresses claim keys and tightens forbidden wording;
it must not expose engine approval, best-move, winning, decisive, forced-win,
no-counterplay, or blunder claims.
Stage 6-3 may summarize only that Explanation Plan carries a default forbidden
wording set for downstream renderer and LLM layers. `Tactic.Hanging` remains
bounded material tactic wording only. `engineStrengthLimited` strengthens
forbidden wording; it creates no engine approval, best-move, only-move,
winning, decisive, forced, no-counterplay, strategic, conversion, king-safety,
file-control, outpost, or mate-net claim.
Stage 6-4 may summarize only that Support and Context enter Explanation Plan
as structured relations to the Lead. The allowed relation keys are
`same_family_lower_rank`, `alternative_hanging_candidate`, `capped_same_story`,
and `blocked_by_engine_refute`. Support, Context, and Blocked create no
standalone public claim. Blocked remains debug-only relation structure, and
proofFailures text must not become relation wording.
Stage 6-5 may summarize only that Explanation Plan receives selected Verdict
only. It must not accept raw BoardFacts, BoardMood, root atoms, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, unselected
Story, unselected Verdict, or source rows. It creates no chess meaning beyond
the selected Verdict.
Stage 6 closeout also lives there; other documents may summarize only that
Stage 6 closes with Explanation Plan only, no renderer/LLM/public route or
pedagogy opening, no duplicated StoryTable/Verdict/EngineCheck/CaptureResult
authority, no allowed claim for Blocked, Support, Context, engine-capped, or
engine-refuted Verdicts, and Stage 7 deterministic renderer may receive
Explanation Plan only.

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
- No renderer opening: deterministic templates and LLM renderers remain closed.
  They may verbalize selected Explanation Plan data only after prerequisite
  laws and tests exist; they cannot repair, upgrade, or invent chess meaning.
- Stage 6 Closeout Explanation Plan scope only: do not implement deterministic
  renderer, LLM narration, public route `200`, user-facing prose, pedagogy,
  new Story families, engine explanation, `Tactic.Fork`, `Scene.Material`,
  `Scene.Defense`, Plan, Strategy, King attack, Conversion, Blunder, engine PV
  commentary, best-move explanation, broad scalar re-entry,
  source/engine public-truth paths, or CTH-style family exceptions. Stage 7
  deterministic renderer may later receive Explanation Plan only.
- Story Order no-go: StoryTable may order existing `Tactic.Hanging` Story rows
  into roles, but it must not create a Story, open a new positive
  family, or promote engine eval, Board Facts, or `CaptureResult` into direct
  public claims.
- Engine Check no-go: engine eval, engine line, reply line, and checked move
  data may support, cap, or refute only an already existing `Tactic.Hanging`
  Story after same-board and freshness evidence exists. They must not create a
  Story, rank a Story, write a `Verdict`, feed a renderer, feed an LLM, or
  become public truth.
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
