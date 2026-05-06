# Chess Commentary SSOT

This branch rebuilds commentary around a pre-render deterministic chess model.
The live backend authority chain is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

No other path owns current public chess meaning.

This branch is not yet a product-level commentary backend. Its current job is
the proof-first Story kernel: prevent unproven chess meaning from reaching the
public surface. Good commentary is deferred until the public boundary is closed
against false claims.

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said. LLM phrasing
comes only after a safe downstream payload exists.

Engine lines, mate/tablebase proof, SEE, and bounded material results are
truth-oracle evidence for backend proof. Raw engine numbers and engine text are
never public claim owners. Backend policy owns proof, Story selection,
arbitration, and pedagogy. The LLM may phrase selected `Verdict` or
`Explanation IR` data only; it must not decide, prove, rank, repair, or invent
chess meaning.

## Core Decision

`BoardMood` gathers shared board facts. `Story` is the only chess unit allowed
to compete for commentary. `StoryTable` applies deterministic interaction and
ordering rules. `Verdict` is the language-neutral result consumed downstream.

The renderer is explicitly downstream. It may express selected structured
intent, but it must not create chess meaning, repair missing evidence, upgrade
wording strength, or turn source and engine context into truth.

The branch invariant is:

`BoardMood` observes.
`Story` proves.
`StoryTable` arbitrates.
`Verdict` speaks.
Renderer only phrases.

`BoardMood` observations such as weak squares, open files, attacked pieces,
legal pawn levers, and king-ring attack maps are not public claims. For
example, a rook/open-file/entry-square observation does not prove that a rook
controls or can use the open file. Public meaning begins only at `Story`.

The proof boundary is:

- feature is not a claim
- claim is not a public `Story`
- public `Story` requires proof-bearing identity

The authority-consolidation boundary is:

- One chess meaning, one home.
- One observation family, one owner.
- One public claim, one proof path.

Too many small modules and duplicated roles caused authority explosion in the
old commentary stack. Splitting work into small implementation steps is still
allowed, but splitting one chess meaning across several similarly named types,
modules, rows, or docs authority paths is not allowed. A new type, module, row, or docs-authority name is the last resort.

Before adding a new name, ask:

- is this really a different chess meaning from an existing one?
- can the existing Fact owner carry this as a field?
- does this name create a new authority by sounding like one?
- will two names now own the same board phenomenon?
- will a later `Story` proof know exactly which input to trust?

The implementation question is not "which tactic or strategy feature is
visible?" The implementation question is whether that feature can become a
Story with side, target, anchor, route, rival, required legal line, and
same-root proof. If the answer is missing, the system must stay silent or keep
the row blocked/context-only.

Implementation must open in this order only:

`observation` -> `proof sidecar` -> `Story` -> `Verdict` -> `Explanation IR` -> Renderer

The current branch owns the early kernel: board truth, primitive geometry,
Story boundary, and Verdict boundary. Downstream product stages stay closed
until earlier authority stages are proven.

Current implementation scope is Stage 7 Closeout Pass.
Stage 1 Board Facts, Stage 2 Story Proof, and Stage 3 first narrow positive
Story are prerequisites. Stage 3 remains open only for Material proof kernel,
`Tactic.Hanging`, and Hanging negative corpus. Stage 4 opens only
`EngineCheck`, `EngineLine`, and `EngineEval` as internal evidence, same-board
and stale guards, `Tactic.Hanging` attachment, false-positive corpus, and
conservative StoryTable diagnostics for existing `Tactic.Hanging` Stories.
Stage 5 opens only StoryTable role ordering for existing `Tactic.Hanging`
Story rows. Stage 5-1 assigns Lead, Support, Context, and Blocked roles inside
that Hanging-only slice. Stage 5-2 fixes deterministic ordering inputs for
those StoryTable rows. Stage 5-3 tightens close blockers and context
relations for those rows. Stage 5-4 keeps Verdict diagnostics out of public
numeric values and downstream public surfaces. Stage 5 closeout confirmed
Story ordering only and selected-Verdict handoff only. Stage 6-0 opens only the
Explanation Plan charter and selected-Verdict speech boundary; it does not open
sentences, renderer, LLM, public route `200`, pedagogy, new Story families, or
engine explanation. Stage 6-1 opens only the Explanation Plan shape for one
selected `Tactic.Hanging` Lead Verdict. `allowedClaim` stays a structured key
such as `can_win_piece`; the first shape carries `bounded` strength and
forbidden wording, not public prose. Stage 6-2 opens only `Tactic.Hanging`
allowed claim mapping. Uncapped Lead Verdict only may carry an allowed claim
key. Support, Context, Blocked, and engine-capped Verdicts do not create
standalone public claims; `engineStrengthLimited` suppresses claim keys and
strengthens forbidden wording. Stage 6-3 opens only forbidden wording boundary.
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
Stage 7-0 opens only the Deterministic Renderer charter: `ExplanationPlan`
only input, deterministic template, `Tactic.Hanging` bounded claim phrasing,
forbidden wording check, no LLM, and no public route. Raw Verdict, Story,
Board Facts, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
proofFailures text, source rows, user-level pedagogy, best-move wording,
engine-says wording, winning, decisive, forced, blunder, and free-piece
wording remain closed. Stage 7-1 opens only the Renderer input guard:
Renderer receives `ExplanationPlan` only and exposes no raw Verdict, Story,
BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw
PV, proofFailures, or source-row input. Stage 7-2 opens only the minimal
`Tactic.Hanging` template for the `can_win_piece` claim key; the text must not
exceed the ExplanationPlan claim key or evidenceLine. Stage 7-3 opens only
forbidden wording enforcement. Renderer output must not violate
`ExplanationPlan.forbiddenWording`; `win material` wording is allowed only when
`allowedClaim` is `CanWinPiece`, `winning position` remains forbidden, and
engine-strength-limited plans must fail strong wording output. Stage 7-4
opens only the no-standalone-text boundary. Renderer phrases only Lead plans
with an allowed claim; Support, Context, Blocked, capped no-claim, and
engine-refuted relation plans produce no text. Stage 7-5 opens only the
RenderedLine shape. `RenderedLine` owns no chess meaning, proof, or engine
data; RenderedLine is only the expression result of ExplanationPlan. Stage
7-6 opens only renderer baseline tests. Renderer output is no stronger than
ExplanationPlan; there is no new renderer wording, no new input, no public
route `200`, and no LLM narration. Stages 8-11 remain a dependency map, not
permission to open those systems. Stage 7 closeout confirms deterministic
renderer is closed as a template baseline. Stage 8 LLM Narration may receive
deterministic text and ExplanationPlan only. Stage 8 must not read raw Verdict,
Story, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV,
proofFailures text, or source rows directly.

`StoryInteractionLaw.md` is the single live authority for the Stage 3 charter.
This SSOT states the stage scope only: Stage 3 opens backend Material proof
evidence and the named `Tactic.Hanging` writer, while every other positive
family and every downstream public/rendering/LLM surface remains closed.

`StoryInteractionLaw.md` is the single live authority for the Stage 4 charter.
This SSOT states the stage scope only: Stage 4 is named `Engine Check`. Story
comes first. Engine checks, caps, or refutes. Engine never speaks alone.

`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter.
This SSOT states the stage scope only: Stage 5 is named `Story Order`.
StoryTable may assign roles among existing `Tactic.Hanging` Story rows,
but it does not create Stories or new public claims.
Stage 5-1 Hanging role rules also live in `StoryInteractionLaw.md`; this SSOT
summarizes only that refuted, incomplete, and captureless Hanging rows are
blocked, unknown engine checks create no engine claim, and roles do not open
renderer or LLM.
Stage 5-2 deterministic ordering rules also live in `StoryInteractionLaw.md`;
this SSOT summarizes only that ordering may use role eligibility,
publicStrength, Story identity, writer presence, and blocked status, while raw
engine eval, raw PV text, proofFailures text, Board Facts row count,
`CaptureResult` text, renderer wording, and input order remain non-authority.
Stage 5-3 conflict and block rules also live in `StoryInteractionLaw.md`; this
SSOT summarizes only that close Hanging blockers, Quiet fallback, and
Source/Opening context cannot outrank board-backed Hanging, while Plan,
Blunder, Defense, extra counterplay, and Strategy relations remain closed.
Stage 5-4 Verdict diagnostic boundary also lives in `StoryInteractionLaw.md`;
this SSOT summarizes only that Verdict values keep their fixed shape,
proofFailures and EngineCheck diagnostics stay out of values,
engineStrengthLimited stays internal, Verdict is not public text, and renderer,
LLM, and public route remain closed.
Stage 5 closeout also lives in `StoryInteractionLaw.md`; this SSOT summarizes
only that Stage 5 closed as StoryTable ordering over existing Hanging rows,
with no new chess meaning, no downstream public surface, and a selected Verdict
handoff for Stage 6.

`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter.
This SSOT summarizes only that Stage 6 is named `Explanation Plan`, with
`Explanation IR` as a parenthetical data-shape label, and Stage 6-0 receives
selected Verdict data only to bound claim, evidence, strength, role,
support/context relation, and forbidden wording. Raw Board Facts, raw
BoardMood, root atoms, `CaptureResult`, engine sidecars, raw PV, proofFailures
text, source rows, renderer wording, and LLM wording remain outside the Stage 6
input boundary.
Stage 6-1 summary only: one selected `Tactic.Hanging` Lead Verdict may become
an internal `ExplanationPlan` shape with role, scene, tactic, side, target,
anchor, route, allowedClaim, evidenceLine, strength, forbiddenWording, and
empty supportContextLinks. It still writes no sentence.
Stage 6-2 summary only: uncapped `Tactic.Hanging` Lead Verdicts may map to
safe structured claim keys, while Support, Context, Blocked, and engine-capped
Verdicts do not create standalone public claims. `engineStrengthLimited`
suppresses claim keys and strengthens forbidden wording.
Stage 6-3 summary only: Explanation Plan carries the default forbidden wording
set for downstream renderer and LLM layers. `Tactic.Hanging` remains bounded
material tactic wording only, and `engineStrengthLimited` strengthens
forbidden wording without carrying a claim.
Stage 6-4 summary only: Support and Context enter Explanation Plan as
structured relations to Lead. Support, Context, Blocked, and engine-capped
Verdicts create no standalone public claim. Blocked remains debug-only
relation structure, and proofFailures text does not feed relation wording.
Stage 6-5 summary only: Explanation Plan receives selected Verdict only. It
accepts no raw proof material, unselected Story, unselected Verdict, source
row, or proofFailures wording.
Stage 6 closeout summary only: Stage 6 closes with Explanation Plan only,
without renderer, LLM, public route, user-facing prose, or pedagogy. Blocked,
Support, Context, engine-capped, and engine-refuted Verdicts create no allowed
claim, and Stage 7 deterministic renderer may receive Explanation Plan only.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-0
charter. This SSOT summarizes only that Stage 7 is named `Deterministic
Renderer` and Stage 7-0 opens `ExplanationPlan` only input, deterministic
template, `Tactic.Hanging` bounded claim phrasing, forbidden wording check, no
LLM, and no public route. It does not open raw Verdict, Story, Board Facts,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures
text, source rows, user-level pedagogy, engine PV explanation, best-move
explanation, engine-says wording, winning, decisive, forced, blunder, or
free-piece wording.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-1 input
guard. This SSOT summarizes only that Stage 7-1 opens the Renderer input
guard: Renderer receives `ExplanationPlan` only, exposes no raw Verdict,
Story, BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures, or source-row input, and cannot create a
sentence without an `ExplanationPlan`.
Renderer exposes no raw Verdict, Story, BoardFacts, BoardMood, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures, or source-row
input.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-2
template. This SSOT summarizes only that Stage 7-2 opens the minimal
`Tactic.Hanging` template for `can_win_piece`, with Lead, bounded strength,
non-debug, route, target, evidenceLine, and forbidden wording present. The
first deterministic text does not exceed the ExplanationPlan claim key or
evidenceLine.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-3
forbidden wording enforcement. This SSOT summarizes only that Stage 7-3 opens
only forbidden wording enforcement, Renderer output must not violate
`ExplanationPlan.forbiddenWording`, `win material` wording is allowed only
when `allowedClaim` is `CanWinPiece`, `winning position` remains forbidden,
engine-strength-limited plans fail strong wording output, and plans with no
allowedClaim or debugOnly true produce no output.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-4
no-standalone-text boundary. This SSOT summarizes only that Stage 7-4 opens
only the no-standalone-text boundary, Renderer phrases only Lead plans with an
allowed claim, and Support, Context, Blocked, capped no-claim, and
engine-refuted relation plans produce no text.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-5
RenderedLine shape. This SSOT summarizes only that Stage 7-5 opens only the
RenderedLine shape, `RenderedLine` owns no chess meaning, proof, or engine
data, and RenderedLine is only the expression result of ExplanationPlan. It
must not include CaptureResult, EngineCheck, BoardFacts, proofFailures, raw
route analysis, or source-row material.

`StoryInteractionLaw.md` is the single live authority for the Stage 7-6
renderer baseline tests. This SSOT summarizes only that Stage 7-6 opens only
renderer baseline tests, Renderer output is no stronger than ExplanationPlan,
and there is no new renderer wording, no new input, no public route `200`, and
no LLM narration.

`StoryInteractionLaw.md` is the single live authority for the Stage 7 closeout
pass. This SSOT summarizes only that deterministic renderer is closed as a
template baseline, Stage 8 LLM Narration may receive deterministic text and
ExplanationPlan only, and Stage 8 must not read raw Verdict, Story,
EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text,
or source rows directly.

## Current No-Go State

The current checkpoint is closed at the public route and LLM boundary.
Stage 1 `Board Facts` organizes small current-board observations under
`BoardFacts.md`, Stage 2 `Story Proof` binds the minimum public-output evidence
tuple, Stage 3 opens only the named `Tactic.Hanging` writer over positive
`CaptureResult`, and Stage 4 closes with internal EngineCheck evidence, guards,
Hanging-only attachment, negative corpus, and conservative StoryTable diagnostics only. Stage 5
opens only role ordering for existing Hanging rows. Stage 6-1 opens only
Explanation Plan shape over one selected `Tactic.Hanging` Lead Verdict, and
Stage 6-2 opens only Hanging allowed claim mapping, Stage 6-3 opens only
forbidden wording boundary, Stage 6-4 opens only Support and Context relation
structure, Stage 6-5 opens only the selected Verdict input guard, Stage 6
closeout confirms Explanation Plan only, Stage 7-0 opens only the
Deterministic Renderer charter, Stage 7-1 opens only the Renderer input guard,
Stage 7-2 opens only the minimal `Tactic.Hanging` template, Stage 7-3 opens
only forbidden wording enforcement, and Stage 7-4 opens only the
no-standalone-text boundary. Stage 7-5 opens only the RenderedLine shape.
Stage 7-6 opens only renderer baseline tests. Stage 7 closeout confirms the
deterministic renderer is closed as a template baseline. There is no other positive Story opening, no public surface opening, no
`BoardMood` Sxxx expansion or re-entry, no engine PV commentary, no best-move
explanation, no LLM narration, and no renderer authority beyond
`ExplanationPlan` only deterministic phrasing.

`/api/commentary/render` and `/internal/commentary/render-local-probe` are
registered only as fail-closed tombstones. No `200`, rendered payload,
environment switch, or frontend mock can open them.

`BoardMood` remains fixed at `48` bits, `256` scalars, and `3,328` total values.
No `BoardMood` Sxxx expansion or re-entry is admitted in this checkpoint.
Split/cut re-entry requires a named law and same-board producer proof.

Open file, pin, weak square, loose piece, pawn lever, attacked piece,
king-ring attack, and legal move facts remain observations only. They do not
become public claims until `Story` supplies side, target, anchor, route, rival,
required legal line, and same-root proof sidecar.

Known blockers are authority blockers, not implementation permission:

- Numeric `Proof` scores may rank blocked/context `Verdict` rows only. They
  cannot set `leadAllowed=true` or produce `Role.Lead` until same-root side,
  target, anchor, route, rival, required legal line, and proof sidecars exist.
- Missing side, target, anchor, route, rival, required legal line, or same-root
  proof sidecar is a hard public-output block, not weak scoring or renderer
  repair.
- Runtime `StoryProof` records that full tuple and its missing evidence before
  lead candidacy, but only the named `Tactic.Hanging` writer can turn complete
  proof plus positive `CaptureResult` into a positive Story.
- `EngineCheck`, `EngineLine`, and `EngineEval` are internal evidence only.
  They may check the existing `Tactic.Hanging` route after same-board and
  freshness evidence exists, but they cannot create a Story, rank a Story,
  write a `Verdict`, feed a renderer, feed an LLM, or become public truth.
- Stage 4-2 requires engine evidence to bind to the same board, the same Story route, and the same legal line. Wrong-board facts, route-mismatched engine lines, stale engine data, missing depth/freshness, eval-only input without a Story, and PV-only input without a Story remain diagnostic only.
- Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`, with `Unknown`, `Supports`, `Caps`, and `Refutes` as the only statuses. `Supports` does not mean winning, best move, decisive, PV explanation, or public truth. `Caps` forbids strong expression. `Refutes` blocks the Hanging Story.
- Stage 5 role ordering is limited to existing `Tactic.Hanging` Story rows.
  StoryTable must not create a Story, open a new positive family,
  or promote engine eval, Board Facts, or `CaptureResult` into public truth.
- `Scene.Opening` is context-only and must not lead over a board-backed
  `Story`.
- Old failing tests proved lower/scaffold/renderer non-upgrade, not default
  runtime FEN to public `Verdict`.

## Authority Model

Live authority now sits here:

- `BoardMood` holds shared board facts, bit maps, and scalar slots.
- `Story` binds public scene, optional plan, optional tactic, identity, and
  proof.
- `StoryTable` decides lead eligibility, support/context/blocking roles, owner
  interaction, source limits, quiet limits, and deterministic top-K ordering.
- `Verdict` encodes rank, role, lead permission, strength, and story references.
- Chess-facing model names, fixed shape counts, story families, and legacy
  survival rules are governed by `ChessModelContract.md`.

Opening tags, source rows, raw engine eval, prepared lines, pins, xrays, weak
pawns, and similar extracted facts are not standalone public truth owners. They
may feed `BoardMood` or same-root Story sidecars only when a live authority
document admits that path; they do not supply proof authority by name and do not
bypass `StoryTable`.

Root atoms, source rows, engine evals, and support-only carriers must not be
shown directly to renderers or LLM phrasing layers. They may become evidence
only after a `Story` binds the required identity and proof tuple.

Forbidden shortcut patterns:

- feature to public claim
- root atom to public claim
- `BoardMood` scalar to public claim
- source row to public truth
- raw engine eval to public truth
- high numeric `Proof` score to `Role.Lead` without sidecar
- renderer repair of missing chess evidence
- LLM inference of chess meaning
- broad strategic `Story` before narrow tactical/material `Story`
- family-specific shortcut growth before common proof coordinates exist

If a change makes commentary richer but weakens proof ownership, it is rejected.

Dependency shortcuts are forbidden:

- Renderer before Story proof sidecar is forbidden.
- LLM before Explanation IR is forbidden.
- Strategy before tactical/material proof is forbidden.
- Pedagogy before causal arbitration is forbidden.
- Personalization before stable Story taxonomy is forbidden.

## Required Bindings

These are non-negotiable bindings, not soft preferences:

- exact board identity
- legal replay
- owner, defender, anchor, route, and scope binding
- same-root proof sidecar binding
- raw engine and source context non-ownership
- public-safe line evidence
- stale evidence rejection
- forbidden shortcut rejection
- renderer non-authority

The public binding tuple is fail-closed. Missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar is a hard public-output
block even if other evidence looks strong.

## Scoring Responsibilities

The chess model scores what is worth saying after required bindings:

- `Proof` computes `truth`, `tacticHeat`, `planHeat`, and `publicStrength`.
- `Story` encodes scene, plan, tactic, identity, and proof slots in exactly
  `160` values.
- `StoryTable` keeps at most `8` verdicts and owns deterministic ordering.
- `Verdict` encodes rank, role, lead permission, strength, and story references
  in exactly `96` values.

## Renderer Boundary

Templates and LLM renderers remain closed. When their contracts open, they
must consume only selected Explanation Plan data. They may paraphrase; they may
not infer new claims.

Pedagogy is backend policy over selected proof-backed `Verdict` data. Renderer
and LLM layers may not choose instructional emphasis, promote context into
lessons, or turn observations into advice.
