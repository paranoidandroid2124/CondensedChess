# Story Interaction Law

Prepared on 2026-05-05 for the public layer above BoardMood.

Stage 2 name is `Story Proof`.

Core sentence: Board Facts observes. Story Proof binds. Story may speak only
after proof.

Stage 3 core sentence: StoryProof is necessary. A named Story writer gives
permission. Family proof gives the reason.

Ownership split:

- Story owns identity.
- StoryProof owns proof and missing evidence.
- Verdict carries the result.

StoryProof may inspect Story identity, but it must not own or duplicate side,
target, secondary target, anchor, route, or rival. Legal line and same-board
proof are evidence for the Story identity, not a second identity home.

Legal line binding is not tactical success proof. In Stage 2 it proves only
that the Story route is tied to a same-board legal path.

Stage 2 StoryProof proves only the minimum form of evidence. Material, tactic,
plan, king, source, and opening Stories remain blocked or context-only until
Stage 3 explicitly opens the first positive Story family through a named Story
writer and family-specific proof.

proofFailures are internal diagnostics only. They exist for tests, debugging,
and locating the next missing proof coordinate. They are not public payload,
renderer input, or LLM input, and missing evidence text must not become user
commentary.

## Law

`Story` is the first public chess unit, but a Story family name is not enough.
Every public Story must bind side, target, anchor, route, rival, required legal
line, and same-root proof sidecar. Multi-target Stories such as Fork must also
bind a secondary target in Story identity. A family row below adds the
chess-specific facts that must support that binding. Missing side, target,
anchor, route, rival, required legal line, same-root proof sidecar, or required
secondary target is a hard public-output block. If a row does not need a move
sequence, its required legal line is the same-root legal replay proving the
named position fact can be spoken.

Nonlinear interaction is explicit. A support fact can help a Story only when no
hard blocker applies. A blocker can cap or silence a Story even when several
support facts are present. A stronger tactical or blunder Story can override a
strategic Story. Engine context can confirm, cap, or contradict a Story, but it
does not speak without the Story identity tuple.

## Stage 3 Charter

Stage 3 opens exactly one positive Story family at a time. In the first scope,
Stage 3 opens exactly one narrow proof-backed Story family. Complete StoryProof
is necessary but not sufficient. A complete StoryProof does not open a family.
A positive Story requires a named Story writer and family-specific proof. No
other Story family may piggyback on the first family.

Renderer, LLM narration, public route `200`, `/api/commentary/render`, and
`/internal/commentary/render-local-probe` remain closed. The Stage 3 charter
opens backend proof and writer permission only; it does not open public
transport, template rendering, frontend mocks, or LLM phrasing.

Stage 4 Engine Check opens only after the Stage 3 charter and only as internal
evidence for existing Stories. Renderer and LLM remain closed after the Stage 3
charter.

Opening Tactic.Hanging does not open Fork, Material, Defense, Plan, Strategy,
renderer, LLM, or public route. The current Fork vertical slice opens only the
named `Tactic.Fork` backend proof and writer path described below. Material-3
opens only the narrow named `Scene.Material` writer described below.

The following remain forbidden: renderer opening outside named
ExplanationPlan-only templates, LLM narration, public route `200`, Board Facts
direct public claim, Proof score alone as Lead, StoryProof alone as Lead, and
positive Story families other than `Tactic.Hanging`, the narrow
`Tactic.Fork` vertical slice, and the narrow `Scene.Material` writer.

Opening `Tactic.Hanging` did not open `Scene.Material`, `Tactic.Fork`,
`Scene.Defense`, `Scene.Plan`, or any Plan row. The current Fork slice opens
`Tactic.Fork` only through MultiTargetProof and the named Fork writer. Opening
`Scene.Material` does not open `Scene.Blunder`, `Scene.Convert`, winning claims,
conversion claims, or decisive-advantage wording.

Board Facts direct public claim, Proof score alone as Lead, StoryProof alone as
Lead, and positive Story families other than `Tactic.Hanging` were forbidden in
the first Stage 3 scope. The current Fork slice opens only
`MultiTargetProof`, the named `Tactic.Fork` writer, EngineCheck attachment,
StoryTable ordering, negative corpus checks, ExplanationPlan mapping, and
Fork-8 deterministic renderer text from ExplanationPlan. The current Material
slice opens only the narrow proof-backed `Scene.Material` vertical slice
through Material-9 LLM smoke.

proofFailures remain internal diagnostics only. They may locate missing proof
coordinates for tests and debugging, but they are not public JSON, renderer
input, LLM input, or text that may be spoken.

The Stage 3 implementation order is:

1. Material proof kernel
2. `Tactic.Hanging`
3. Hanging negative corpus
4. `Tactic.Fork`
5. Fork negative corpus
6. `Scene.Material`
7. `Scene.Defense`

The first implementation scope is only steps 1-3: create the material proof
kernel, open one narrow `Tactic.Hanging` writer on top of that kernel, and prove
near false positives still stay silent.

Material proof kernel is not a public Story family. `CaptureResult` is internal
evidence only. It may compute side, capturing piece, target piece, legal
capture line, captured value, recapture candidates, material result after one
immediate legal reply or a bounded line, same-board proof, and missing evidence. It must not say
wins material, free piece, hanging, best move, blunder, decisive, winning, or
tactic works. A capture line can show a material result; a recapture can cancel
that result; unclear recapture or equality means no positive material proof.
Failed `CaptureResult` rows leave missing evidence and stay silent.

`Tactic.Hanging` is the first positive Story family. It may speak only when all
of these are present:

1. target piece exists
2. target is attacked
3. legal capture exists
4. target has no adequate defender or recapture
5. bounded material result is positive
6. StoryProof is complete
7. same-board proof is present
8. `Tactic.Hanging` writer is explicitly open

`Tactic.Hanging` must not use free piece, blunder, winning, decisive, forced,
king unsafe, file control, plan, counterplay, strategy, or conversion wording.

Stage 3 first success means one narrow Hanging Story can speak with proof, and
all close false positives still stay silent.

The preferred family opening order after the first scope is:

1. `Tactic.Hanging`
2. `Tactic.Fork`
3. `Scene.Material`
4. `Scene.Defense`

`Plan.Convert`, `Plan.Prophy`, `Plan.Initiative`, `Plan.ColorBind`,
`Plan.WeakSquare`, and route-only strategic claims remain closed until their
proof sidecars and line validation are live.

For `Tactic.Hanging`, an attacked-piece guard map is only an observation. A
public hanging-piece Story requires the attacked piece identity, legal capture,
defender and recapture map, positive bounded material result or SEE,
same-board proof, same-root legal replay, complete StoryProof, and a
forbidden-wording boundary. Without that tuple it must degrade to observation,
context, or blocked Story; it must not say that a piece is hanging or that a
move wins material.

## Stage 4 Charter

Stage 4 name is `Engine Check`.

Story comes first. Engine checks, caps, or refutes. Engine never speaks alone.

Stage 4-1 opens only the internal engine evidence shape. The open evidence
names are `EngineCheck`, `EngineLine`, and `EngineEval`. Their job is to record
same-board proof, checked move, engine line, reply line, eval before, eval
after, depth or freshness, and missing evidence.

Engine eval, engine line, reply line, and checked move data cannot create a Story. They cannot open `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, renderer, LLM, public route `200`, engine PV commentary, or best-move explanation.

An engine line may check only an existing Story identity and legal route.
Missing same-board proof, checked move, engine line, reply line, eval before,
eval after, depth or freshness, or fresh engine evidence is internal missing
evidence. It is not public payload, renderer input, LLM input, or text that may
be spoken.

Stage 4-1 does not wire StoryTable consumption. `Tactic.Hanging` remains the only positive Story writer. Renderer, LLM, and public route `200` remain closed.

Stage 4-2 adds same-board and stale engine guards.

Engine evidence must bind to the same board, the same Story route, and the same legal line.

Different-FEN engine lines, route-mismatched engine lines, stale engine data, depth-missing engine data, eval-only input without a Story, and PV-only input without a Story are diagnostic only.

The guard result may confirm that an `EngineCheck` sidecar is usable for an existing Story. It still does not create a Story, does not choose a Story, does not write a `Verdict`, and does not open renderer, LLM, or public route `200`.

Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`.

EngineCheck status starts with exactly `Unknown`, `Supports`, `Caps`, and `Refutes`.

- `Unknown`: engine evidence is absent or unusable. Engine-related expression is forbidden.
- `Supports`: engine evidence does not contradict the current Hanging Story. `Supports` does not mean winning, best move, decisive, PV explanation, or public truth.
- `Caps`: the Hanging Story may remain available, but strong expression is forbidden.
- `Refutes`: `Refutes` blocks the Hanging Story.

`Caps` leaves the Story available but forbids strong expression.
`Supports` does not create a new claim. Engine PV remains evidence only and must not become explanation. Engine eval remains internal evidence only and must not become public truth.

Stage 4 negative corpus covers local material gain that fails to a larger
tactic, refuting engine replies, eval collapse after capture, different-FEN
engine lines, stale engine lines, route-mismatched engine lines, engine data
without `CaptureResult`, engine data without complete StoryProof, and engine
data without a named writer.

A complete same-board Hanging Story may be blocked by `Refutes`. Incomplete,
wrong-board, stale, route-mismatched, writerless, or proofless engine evidence
remains `Unknown` and cannot support.

Stage 4 StoryTable integration is conservative.

EngineCheck never creates a Story in StoryTable. `Refutes` blocks only the
Hanging Lead path. `Caps` records only the internal `engineStrengthLimited`
marker. `Supports` preserves the existing Story without creating a new claim.
`Unknown` leaves no engine-related claim. Renderer and LLM wording remain
closed.

## Stage 4 Closeout

One chess meaning, one home. One rule, one live authority.

Stage 4 ends with `Tactic.Hanging` as the only EngineCheck consumer.
`EngineCheck` is not a Story writer and does not create public truth.
`StoryInteractionLaw.md` owns this closeout; other live docs may summarize scope only.

The closeout state is:

- `Refutes` blocks the Hanging Lead path.
- `Caps` records only an internal strength-limited diagnostic.
- `Supports` preserves an existing Hanging Story without adding a claim.
- `Unknown` carries no engine-related claim.
- Wrong-board, stale, route-mismatched, captureless, proofless, and writerless
  engine evidence cannot support.

Stage 5 Story Order may receive internal EngineCheck diagnostics from selected
Verdict rows. Stage 5 must not open renderer, LLM, public route `200`,
`Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan, Strategy, engine PV
commentary, or best-move explanation.

## Stage 5 Charter

Stage 5 name is `Story Order`.

Stage 5 may be described as `StoryTable Arbitration` when a document needs to
explain the role of `StoryTable`.

Core sentence: StoryTable orders. It does not invent.

Many Stories may exist. StoryTable chooses roles. No new chess meaning is
created.

Stage 5-0 fixes this charter before broader arbitration work. Stage 5 first
scope is limited to the existing `Tactic.Hanging` vertical slice.

Allowed:

- Lead
- Support
- Context
- Blocked
- deterministic ordering
- `Refutes` -> blocked
- `Caps` -> strength-limited diagnostic
- `Supports` -> no new claim
- `Unknown` -> no engine claim

Forbidden:

- new Story creation
- new positive family
- engine eval as ranking truth
- Board Facts as direct public claim
- `CaptureResult` as public material story
- pedagogical advice
- Explanation IR
- renderer
- LLM
- public route
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- Plan / Strategy
- engine PV commentary
- best-move explanation

StoryTable may order only existing Story rows. It must not manufacture a row
from EngineCheck, EngineEval, Board Facts, `CaptureResult`, a proof number, or
a missing sidecar diagnostic.

For the first scope, multiple proof-backed `Tactic.Hanging` rows may be
assigned stable roles. One may become Lead. Other Hanging rows may become
Support, Context, or Blocked. A `Refutes` EngineCheck sends the Hanging Story
to Blocked. A `Caps` EngineCheck preserves only internal strength limitation.
`Supports` preserves an existing Hanging Story without adding a claim.
`Unknown` creates no engine claim and forbids engine expression.

Stage 5 completion standard: when multiple Hanging Story rows exist,
StoryTable deterministically decides Lead, Support, Context, and Blocked roles
without creating new chess meaning or a new public claim.

`StoryInteractionLaw.md` is the single live authority for the Stage 5 charter.
Other live documents may summarize Stage 5 scope only.

## Stage 5-1 Hanging Role Rules

Stage 5-1 goal: assign roles for existing `Tactic.Hanging` Story rows.

Hanging role rules:

- complete `Tactic.Hanging` Story, positive `CaptureResult`, and no `Refutes`
  may enter Lead selection.
- exactly one selected Hanging row may be Lead.
- lower-strength complete Hanging may become Support or Context.
- `EngineCheck.Refutes` sends Hanging to Blocked.
- incomplete StoryProof sends Hanging to Blocked.
- missing `CaptureResult` sends Hanging to Blocked.
- no writer sends Hanging to Context or Blocked.
- `EngineCheck.Unknown` creates no engine claim.

Support is not yet a public sentence. Context is not yet a public sentence.
Role assignment does not open renderer or LLM.

## Stage 5-2 Deterministic Ordering

Stage 5-2 goal: when multiple Story rows exist, StoryTable always returns the
same order for the same rows.

Allowed sort input:

- Story role eligibility
- publicStrength
- scene / tactic identity
- side
- target
- anchor
- route
- writer presence
- blocked status

Forbidden sort input:

- raw engine eval
- raw PV text
- proofFailures text
- Board Facts row count
- `CaptureResult` text
- renderer wording

Input order must not decide Lead. Equal-strength rows must fall through to
target, anchor, and route identity. proofFailures may explain why a row is
Blocked, but proofFailures text must not sort public rows. Raw engine eval and
raw PV text may affect EngineCheck diagnostics only through admitted status;
they must not order Stories by themselves.

## Stage 5-3 Conflict and Block Rules

Stage 5-3 goal: resolve close blocker relationships for Hanging Story rows
only.

Allowed in the current scope:

- `EngineCheck.Refutes` blocks Hanging.
- Missing StoryProof blocks Hanging.
- Missing `CaptureResult` blocks Hanging.
- Missing writer blocks Hanging.
- Quiet only if no positive Hanging exists.
- `Scene.Source` and `Scene.Opening` cannot outrank board-backed Hanging.

Not implemented in Stage 5-3:

- Tactic vs Plan override.
- Blunder override.
- Defense vs Threat relation.
- Counterplay cap beyond existing `EngineCheck.Caps`.
- Strategy suppression.

Stage 5-3 does not create a Story, open a new positive family, create a new
public claim, open renderer or LLM, or turn Source, Opening, Quiet, Board
Facts, `CaptureResult`, raw engine eval, or raw PV text into public truth.

## Stage 5-4 Verdict Diagnostic Boundary

Stage 5-4 goal: keep StoryTable results from being mistaken for renderer or LLM input.

Diagnostic boundary:

- `Verdict.values` shape stays fixed.
- proofFailures do not enter `Verdict.values`.
- EngineCheck diagnostics do not enter `Verdict.values`.
- `engineStrengthLimited` is an internal diagnostic.
- `Verdict` is not public text.
- renderer, LLM, and public route remain closed.

StoryTable may return Verdict diagnostics for tests and debugging. Those
diagnostics do not become renderer input, LLM input, public route payload,
public text, or public chess claims.

## Stage 5 Closeout

Stage 5 closes with Story ordering only.
Explanation IR, renderer, LLM, and pedagogical advice remain closed.
StoryTable creates no chess meaning. It orders existing Stories.
`EngineCheck`, `CaptureResult`, and Board Facts keep their existing homes.
Refuted, incomplete, writerless, captureless, source-only, opening-only, and Quiet fallback rows cannot become Lead over proof-backed Hanging.
No new type, row, or live md authority is introduced by Stage 5 closeout.
Stage 6 handoff receives selected Verdict only.
Stage 6 must not read raw Board Facts, `CaptureResult`, `EngineCheck`, raw engine eval, or raw PV text directly.

Stage 5 closeout does not open Explanation IR, renderer, LLM, public route,
pedagogical advice, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, Plan,
Strategy, engine PV commentary, or best-move explanation.

## Stage 6 Charter

Stage 6 name is `Explanation Plan`.

Documents may write `Explanation IR` parenthetically when describing the
renderer-safe data shape.

Core sentence: Verdict decides. Explanation Plan bounds speech.

Stage 6-0 fixes this charter before renderer or narration work. The goal is not
natural language. The goal is to receive selected Verdict data and organize
claim, evidence, strength, role, support/context relation, and forbidden
wording that downstream stages may phrase only after their own contracts open.

Allowed input:

- selected Verdict
- Verdict role
- Verdict strength
- Verdict story identity
- Verdict scene / tactic
- Verdict Lead, Support, Context, or Blocked state

Forbidden input:

- raw Board Facts
- raw BoardMood
- root atoms
- `CaptureResult`
- `EngineCheck`
- `EngineEval`
- `EngineLine`
- raw PV
- proofFailures text
- source row
- renderer wording
- LLM wording

Still closed:

- deterministic renderer
- LLM narration
- public route `200`
- user-facing prose
- pedagogy
- new Story family
- engine explanation

Explanation Plan must not decide, prove, rank, repair, or invent chess meaning.
It may bound speech only from selected Verdict data. It must not read raw board
facts, raw engine evidence, source rows, diagnostics, renderer wording, or LLM
wording to expand that boundary.

Stage 6-0 completion standard: Explanation Plan defines what may be said from
the selected Verdict, but it writes no sentence.

`StoryInteractionLaw.md` is the single live authority for the Stage 6 charter.
Other live documents may summarize Stage 6 scope only.

## Stage 6-1 Explanation Plan Shape

Stage 6-1 goal: turn one selected Verdict into a small pre-speech plan.

First scope handles exactly one `Tactic.Hanging` Lead Verdict.

Allowed fields:

- role
- scene
- tactic
- side
- target
- secondaryTarget
- anchor
- route
- allowedClaim
- evidenceLine
- strength
- forbiddenWording
- supportContextLinks

`allowedClaim` is a structured claim key, not a natural-language sentence.
The first live claim key is `can_win_piece`.
The first live strength key is `bounded`.
support/context links stay empty in the first scope.

Stage 6-1 must not read raw Board Facts, raw BoardMood, root atoms,
`CaptureResult`, `EngineCheck`, `EngineEval`, `EngineLine`, raw PV,
proofFailures text, source rows, renderer wording, or LLM wording.

Still closed:

- full sentence generation
- user-facing prose
- `engine says`
- best move
- winning
- decisive
- public eval

The first shape may bound only this claim: the selected side can win the named
target piece along the selected route. It must not call the move free,
blunder, winning, decisive, forced, best, engine-approved, or evaluated.

## Stage 6-2 Tactic.Hanging Allowed Claim Mapping

Stage 6-2 goal: define which claim keys a `Tactic.Hanging` Verdict may lower to.

Allowed claim keys:

- `can_win_piece`
- `piece_can_be_taken_with_gain`
- `capture_leaves_material_gain`

Forbidden claim keys:

- `free_piece`
- `blunder`
- `winning_tactic`
- `decisive_tactic`
- `forced_win`
- `best_move`
- `no_counterplay`
- `engine_approved`

Only uncapped Lead Verdict may carry an allowed claim key.
Support and Context are not standalone claims.
Blocked creates no allowed claim.
`engineStrengthLimited` suppresses allowed claim keys and strengthens forbidden
wording.

`engineStrengthLimited` may add a strong-wording ban only from the selected
Verdict. It must not expose `EngineCheck`, raw engine eval, raw PV, engine
approval, best-move, winning, decisive, forced-win, no-counterplay, or blunder
claims.

Stage 6-2 completion standard: one selected uncapped `Tactic.Hanging` Lead
Verdict may lower to a safe structured claim key. Other roles create no public
claim.

## Stage 6-3 Forbidden Wording Boundary

Stage 6-3 goal: Explanation Plan carries forbidden wording that renderer or
LLM layers must not say.

Default forbidden wording:

- `free piece`
- `blunder`
- `winning`
- `decisive`
- `forced`
- `best move`
- `only move`
- `engine says`
- `no counterplay`
- `king unsafe`
- `file control`
- `outpost`
- `strategic key`
- `conversion`
- `mate net`

`Tactic.Hanging` first allowed claim remains bounded material tactic only.
`engineStrengthLimited=true` strengthens the forbidden wording boundary.
`engineStrengthLimited=true` carries no allowed claim key.
Explanation Plan must make forbidden wording clearer than allowed speech.

Stage 6-3 must not open renderer wording, LLM wording, public route `200`,
user-facing prose, pedagogy, new Story families, engine explanation, engine
approval, best-move explanation, strategic explanation, conversion advice, or
king-safety claims.

Stage 6-3 completion standard: Explanation Plan carries a stronger forbidden
wording boundary than its allowed claim boundary. It still writes no sentence.

## Stage 6-4 Support / Context Relation

Stage 6-4 goal: carry Support and Context as structure-only relations inside
Explanation Plan.

Rules:

- Uncapped Lead only carries an allowed claim.
- Support carries relation to Lead only.
- Context creates no public claim.
- Blocked may enter Explanation Plan only as debug-only relation structure.
- proofFailures must not feed Explanation Plan wording or relation text.

Allowed relation keys:

- `same_family_lower_rank`
- `alternative_hanging_candidate`
- `capped_same_story`
- `blocked_by_engine_refute`

Forbidden:

- Support standalone sentence
- Context standalone sentence
- Blocked debug text as user explanation
- proofFailures text as wording

Stage 6-4 must not read proofFailures text, raw Board Facts, raw BoardMood,
root atoms, `CaptureResult`, `EngineCheck`, `EngineEval`, `EngineLine`, raw
PV, source rows, renderer wording, or LLM wording to create relation text.

Stage 6-4 completion standard: Support and Context enter Explanation Plan only
as relation structure. Uncapped Lead remains the only role with an allowed
claim.

## Stage 6-5 Selected Verdict Only Guard

Stage 6-5 goal: Explanation Plan receives selected Verdict only.

Allowed input:

- selected Verdict only

Forbidden input:

- raw BoardFacts
- BoardMood
- root atoms
- MultiTargetProof
- CaptureResult
- EngineCheck
- EngineEval
- EngineLine
- raw PV
- proofFailures text
- unselected Story
- unselected Verdict
- source row

Explanation Plan must not expose overloads, constructors, fields, or relation
text paths for raw proof material. It may read only the selected Verdict value
and the fields already carried by that Verdict.

Stage 6-5 completion standard: Explanation Plan does not read raw proof
material directly. It creates no chess meaning beyond the selected Verdict.

## Stage 6 Closeout

Stage 6 closes with Explanation Plan only.

Renderer, LLM, public route `200`, user-facing prose, and pedagogy remain
closed.

Explanation Plan creates no chess meaning. StoryTable and Verdict keep
selection authority. `EngineCheck` and `CaptureResult` keep evidence authority.
Explanation Plan must not duplicate their roles.

Negative corpus:

- Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create
  no allowed claim or public claim.
- Engine-capped Verdicts may carry `capped_same_story` relation structure and
  stronger forbidden wording only.
- Engine-refuted Verdicts may carry debug-only `blocked_by_engine_refute`
  relation structure only.
- proofFailures text remains diagnostic only and must not become explanation
  wording.

Cleanup / consolidation:

- No new Story family opens.
- No renderer, LLM, public route, or pedagogy opens.
- No new row or md authority is introduced by Stage 6 closeout.
- Existing Explanation Plan type names remain shape names only; they do not
  decide, prove, rank, repair, or invent.

Next-stage handoff:

- Stage 7 deterministic renderer may receive Explanation Plan only.
- Stage 7 must not read raw Verdict, `EngineCheck`, `CaptureResult`, Board
  Facts, BoardMood, raw PV, proofFailures text, source rows, or raw engine
  evidence directly.
- Stage 7 must not create chess meaning, repair missing proof, upgrade claim
  strength, or open public route `200` without its own contract and tests.

One chess meaning, one home.
One rule, one live authority.
Verdict decides. Explanation Plan bounds speech.

## Stage 7-0 Deterministic Renderer Charter

Stage 7 name is `Deterministic Renderer`.

Core sentence: Explanation Plan bounds speech. Renderer only phrases it.

Stage 7-0 fixes what the Deterministic Renderer may open before LLM narration
or public route work. The goal is a deterministic template baseline that turns
`ExplanationPlan` into internal text without making chess meaning.

Allowed:

- `ExplanationPlan` only input
- deterministic template
- `Tactic.Hanging` bounded claim
- forbidden wording check
- no LLM
- no public route

Forbidden input / authority:

- raw Verdict
- Story
- Board Facts
- CaptureResult
- EngineCheck
- EngineEval / EngineLine
- raw PV
- proofFailures text
- source row

Forbidden speech:

- user-level pedagogy
- best-move wording
- engine-says wording
- winning wording
- decisive wording
- forced wording
- blunder wording
- free-piece wording

The Deterministic Renderer may phrase only fields already present in the
`ExplanationPlan`. It must not read, recover, repair, or reinterpret raw proof
material. It must not create a new Story, choose a Verdict, explain engine PV,
name a best move, or upgrade bounded material wording into stronger speech.

Every deterministic template must pass the forbidden wording boundary before
text can leave the renderer boundary. LLM narration, public route `200`,
pedagogy, new Story families, engine explanation, engine-says wording,
best-move explanation, winning, decisive, forced, blunder, and free-piece
wording remain closed.

Stage 7-0 completion standard: Stage 7 charter fixes that Deterministic
Renderer creates no chess meaning and phrases Explanation Plan only.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-1 Renderer Input Guard

Stage 7-1 goal: Renderer receives ExplanationPlan only.

Allowed input:

- ExplanationPlan

Forbidden input:

- Verdict
- Story
- BoardFacts
- BoardMood
- CaptureResult
- EngineCheck
- EngineEval
- EngineLine
- raw PV
- proofFailures
- source row

Renderer must expose no `fromVerdict`, `fromStory`, `fromBoardFacts`, or
`fromEngineCheck` path.

Renderer must not create a sentence without an `ExplanationPlan`.

Renderer must not read raw Verdict, Story, BoardFacts, BoardMood,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures, or
source-row material directly. It may phrase only the `ExplanationPlan` already
bounded by Stage 6.

LLM narration, public route `200`, pedagogy, new Story families, engine PV
explanation, best-move wording, engine-says wording, winning, decisive,
forced, blunder, and free-piece wording remain closed.

Stage 7-1 completion standard: Renderer does not read proof material directly.

Player notation boundary:

- SAN formats an already-approved legal move only
- legal `Line` endpoints remain proof binding only
- route SAN is the speech notation carried from Story into ExplanationPlan
- SAN check or mate marks are legal-replay notation only
- SAN does not create Story, Proof, Verdict, ExplanationPlan, or public claims
- Renderer and LLM smoke must not phrase moves as origin-destination routes
- SAN text owns no proof, ranking, or chess meaning by itself

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-2 Minimal Tactic.Hanging Template

Stage 7-2 goal: turn only the `can_win_piece` claim key into deterministic text.

Input conditions:

- role is Lead
- allowedClaim is `CanWinPiece`
- strength is `Bounded`
- debugOnly is false
- route exists
- route SAN exists
- target exists
- evidenceLine exists
- forbidden wording set exists

Allowed first template:

- `dxe5 wins material against the piece on e5.`

Forbidden wording:

- free piece
- blunder
- winning
- decisive
- forced
- best move
- only move
- engine says
- no counterplay
- king unsafe
- file control
- outpost

Renderer must refuse Support, Context, Blocked, debug-only, missing-route,
missing-target, missing-evidenceLine, missing-forbidden-wording, and
non-`CanWinPiece` plans.

The template may use only the selected `ExplanationPlan` side, target, route,
route SAN, evidenceLine, allowed claim key, strength, and forbidden wording. It must not
read raw Verdict, Story, BoardFacts, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures, or source-row material.

LLM narration, public route `200`, pedagogy, engine explanation, best-move
explanation, engine-says wording, winning, decisive, forced, blunder,
free-piece, no-counterplay, king-safety, file-control, outpost, and strategic
wording remain closed.

Stage 7-2 completion standard: the first `Tactic.Hanging` deterministic text
does not exceed the ExplanationPlan claim key or evidenceLine.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-3 Forbidden Wording Enforcement

Stage 7-3 goal: Renderer output must not violate `ExplanationPlan.forbiddenWording`.

Forbidden wording meanings:

- free piece
- blunder
- winning as position verdict
- decisive
- forced
- best move
- only move
- engine says
- no counterplay
- king unsafe
- file control
- outpost
- strategic key
- conversion
- mate net

Renderer must reject output when any forbidden wording meaning appears in
deterministic text.

`win material` wording is allowed only when `allowedClaim` is `CanWinPiece`.
`winning position` remains forbidden.

Engine-strength-limited plans must fail strong wording output.

Plans with no allowedClaim or debugOnly true must produce no output.

Stage 7-3 opens only forbidden wording enforcement. It does not open LLM
narration, public route `200`, pedagogy, new Story families, raw Verdict,
Story, BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures text, source rows, engine explanation,
best-move explanation, or user-level pedagogy.

Stage 7-3 completion standard: Renderer automatically refuses forbidden wording.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-4 No Text for Support / Context / Blocked

Stage 7-4 goal: non-Lead ExplanationPlan rows must not create public text.

Rules:

- Lead with allowedClaim may create text
- Support creates no standalone text
- Context creates no standalone text
- Blocked creates no public text
- debugOnly true creates no public text
- engineStrengthLimited with no allowedClaim creates no public text
- engine-refuted relation creates no public text

Renderer must return no text for Support, Context, Blocked, capped no-claim,
and engine-refuted relation plans.

Stage 7-4 opens only the no-standalone-text boundary. It does not open Support
phrasing, Context phrasing, Blocked phrasing, relation narration, LLM
narration, public route `200`, pedagogy, raw Verdict, Story, BoardFacts,
BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
proofFailures text, source rows, engine explanation, best-move explanation, or
user-level pedagogy.

Stage 7-4 completion standard: Renderer phrases only Lead plans with an allowed claim.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-5 Rendered Line Shape

Stage 7-5 goal: deterministic renderer output stays small and verifiable.

`RenderedLine` fields:

- text
- claim key
- strength
- forbidden check passed

`RenderedLine` owns no chess meaning.
`RenderedLine` owns no proof.
`RenderedLine` owns no engine data.
`RenderedLine` is only the expression result of an `ExplanationPlan`.

Forbidden `RenderedLine` fields:

- CaptureResult
- EngineCheck
- BoardFacts
- proofFailures
- raw route analysis
- source row

`RenderedLine` must not include raw route analysis, source rows, proofFailure
text, board facts, capture evidence, engine checks, engine evals, engine
lines, raw PV, Story rows, or Verdict rows. It may carry only the text that
passed deterministic rendering, the selected claim key, the selected strength,
and the forbidden-wording check result.

Stage 7-5 opens only the RenderedLine shape. It does not open public route
`200`, LLM narration, relation narration, pedagogy, new Story families, raw
proof input, raw engine evidence, engine explanation, best-move explanation,
or source-row public claims.

Stage 7-5 completion standard: RenderedLine is only the expression result of ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7-6 Renderer Baseline Tests

Stage 7-6 goal: renderer baseline tests prove output is no stronger than ExplanationPlan.

Baseline tests:

- Lead + CanWinPiece + bounded strength renders safe deterministic text
- Support renders no text
- Context renders no text
- Blocked renders no text
- debugOnly renders no text
- no allowedClaim renders no text
- engineStrengthLimited without allowedClaim renders no text
- forbidden wording appearing in output is rejected
- renderer cannot read Verdict directly
- renderer cannot read EngineCheck directly
- renderer cannot mention engine
- renderer cannot say best move, blunder, free piece, decisive, forced, or winning position

Stage 7-6 opens only renderer baseline tests.

It opens no new renderer wording, no new input, no route, no public route `200`, and no LLM narration.

Stage 7-6 completion standard: Renderer output is no stronger than ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 7 Closeout Pass

Stage 7 closeout goal: audit that deterministic renderer is the only opened Stage 7 surface.

Explanation Plan bounds speech.
Renderer only phrases it.
One rule, one live authority.

Scope audit:

- only the deterministic renderer is open
- LLM narration remains closed
- public route `200` remains closed
- pedagogy remains closed
- new Story families remain closed

Authority audit:

- renderer creates no chess meaning
- renderer does not overlap ExplanationPlan ownership
- renderer does not overlap Verdict ownership
- renderer does not overlap StoryTable ownership
- renderer does not overlap EngineCheck ownership
- renderer does not overlap CaptureResult ownership

Negative corpus audit:

- Support plans produce no text
- Context plans produce no text
- Blocked plans produce no text
- capped plans produce no text
- refuted plans produce no text
- no-claim plans produce no text

Cleanup / consolidation:

Stage 7 closeout adds no new Story family, row, route, public payload, or markdown authority file.

Renderer rules live in `StoryInteractionLaw.md` only; other documents may summarize scope only.

Next-stage handoff:

Stage 8 LLM Narration may receive deterministic text and ExplanationPlan only.

Stage 8 must not read raw Verdict, Story, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, or source rows directly.

Stage 7 completion criteria:

1. Renderer accepts `ExplanationPlan` only.
2. Only Lead `Tactic.Hanging` plans with allowedClaim create deterministic text.
3. Support, Context, Blocked, and debugOnly plans create no text.
4. Forbidden wording boundary violations are rejected.
5. Renderer output is no stronger than ExplanationPlan.
6. Renderer never creates engine, best-move, blunder, decisive, forced, free-piece, or winning-position wording.
7. Public route remains closed.
8. LLM narration remains closed.
9. Stage 8 handoff is limited to ExplanationPlan plus deterministic text.
10. Cleanup pass consolidated duplicate authority and over-documentation.

Stage 7 closeout completion standard: deterministic renderer is closed as a template baseline, and Stage 8 handoff is bounded to deterministic text plus ExplanationPlan.

One chess meaning, one home.
One rule, one live authority.
Explanation Plan bounds speech. Renderer only phrases it.

## Stage 8 LLM Narration Prompt Smoke

Stage 8 name is `LLM Narration`.

Core sentence: LLM rephrases. It does not add chess meaning.

Stage 8 opens narration behavior smoke only.

It is not production API validation.

The prompt smoke input should match the production Stage 8 prompt shape as
closely as this checkpoint permits.

Open lanes:

- 8A Mock narrator
- 8B Codex CLI prompt smoke test
- 8C Production API micro-test remains closed
- 8D Nightly eval remains closed

8A Mock narrator allowed input:

- ExplanationPlan
- RenderedLine

8B Codex CLI prompt smoke allowed input:

- renderedText
- claimKey
- strength
- forbidden wording list
- instruction: "Rephrase only. Do not add chess facts."

Forbidden input:

- FEN
- PGN
- engine line
- eval
- CaptureResult
- EngineCheck
- BoardFacts
- raw Verdict
- Story
- BoardMood
- engine eval
- raw PV
- proofFailures
- source row

Forbidden output:

- new move
- new line
- new tactic
- new plan
- new cause or causal explanation
- new evaluation
- engine mention
- `engine says`
- best move
- forced
- winning
- decisive
- blunder
- free piece
- claim stronger than deterministic text
- chess meaning absent from ExplanationPlan

The mock narrator may echo deterministic text or perform a rules-based
rephrase. It receives only `ExplanationPlan` and `RenderedLine`. It must return
no narration when `RenderedLine` is absent, when the plan is Support, Context,
or Blocked, when forbidden wording appears, or when output would be stronger
than deterministic text.

The Codex CLI prompt smoke test is an LLM behavior check. Passing it means the
prompt contract is broadly safe enough for this checkpoint. It does not replace
production API validation.

Production API validation remains closed until a separate checkpoint proves:

- same system prompt
- same model / temperature / response format
- stable schema
- forbidden wording checker applied to API output
- cost and latency acceptable
- failure / retry / timeout fail closed

Stage 8B Codex CLI prompt smoke must check whether output:

1. rephrases only from renderedText, claimKey, strength, forbidden wording
   list, and the rephrase-only instruction.
2. respects the forbidden wording list.
3. stays no stronger than deterministic text.
4. adds no move, line, tactic, plan, engine mention, or chess meaning absent
   from ExplanationPlan.

8A/8B closeout checks:

- scope stayed limited to mock narrator plus Codex CLI smoke test.
- production API integration stayed closed.
- Stage 8C production API micro-test remains closed.
- streaming stayed closed.
- public route `200` stayed closed.
- user-facing LLM output stayed closed.
- raw proof material did not enter narration.
- forbidden wording is rejected.
- new move is rejected.
- new line is rejected.
- new tactic or plan is rejected.
- new cause or evaluation is rejected.
- engine mention is rejected.
- strengthened claim is rejected.
- no new Story family opened.
- no pedagogy opened.
- no meaning-duplicating type or markdown authority opened.

Closeout standard:

- Deterministic text is the ceiling.
- LLM only polishes below it.
- No raw proof material enters narration.

Stage 8 opens no public route `200`, no renderer input, no raw proof input, no
production API path, no pedagogy, no new Story family, no engine explanation,
and no natural-language verifier.

Stage 8 completion standard: Codex CLI prompt smoke can check narration behavior without opening production API, public route `200`, or new chess meaning.

One chess meaning, one home.
One rule, one live authority.
LLM rephrases. It does not add chess meaning.

## Scene Law

| scene | class | required support | blockers and caps | public wording |
|---|---|---|---|---|
| Scene.Tactic | live forcing | concrete tactic family, legal line, target, attacker, defender or reply | missing line, missing tactic, illegal route, engine contradiction | tactic wording only inside the legal line. |
| Scene.Blunder | live forcing | engine swing or forced material/mate line, losing move identity, rival refutation | no legal refutation, shallow engine only, no previous-position comparison | blunder wording only with swing or forced line. |
| Scene.Material | live static | material count, target piece or asset, capture or trade identity if dynamic | compensation Story with proof, tactical override, unclear recapture | material wording only for named asset. |
| Scene.King | split public | king square, king-ring attacks, escape issue, route or line proof | legal defense exists, no route, no escape proof, engine contradiction | king safety wording, no mate wording without mate proof. |
| Scene.Defense | live response | rival threat, defensive move or resource, protected target | rival threat unproven, defense illegal, stronger tactic override | defense wording only against named threat. |
| Scene.Opening | context-only | opening label, current board match, known line source | board mismatch, tactical refutation, source-only claim, board-backed Story at public floor | opening context only, no truth override or lead over board-backed Story. |
| Scene.Pawns | split public | exact pawn feature, square/file, blocker, lever, guard or passer identity | tactic override, no pawn identity, engine contradiction for plan wording | pawn structure wording for named feature. |
| Scene.PawnAdvance | split public | already-passed pawn, legal one-square non-capturing non-promotion route, exact after-board passed-pawn persistence | pawn not already passed, capture, promotion, double push, after-board passer missing, conversion or strategy wording | bounded passed-pawn advance wording only. |
| Scene.Plan | split public | one Plan row, affordance fact, route, anchor, proof | opposing tactic or blunder at public floor, missing route, engine contradiction | plan wording only as a plan, not as forced truth. |
| Scene.Pieces | split public | exact piece, mobility or route fact, target or anchor | no named piece, route illegal, tactical override | piece wording for named piece only. |
| Scene.Space | live static | controlled squares, safe mobility, space region, side identity | no same-board control, immediate tactic override | space wording only as board condition. |
| Scene.Initiative | conditional | repeated forcing moves, restricted rival replies, line proof | no forcing route, rival has equal resource, engine contradiction | initiative label only with forcing proof. |
| Scene.Convert | conditional | named advantage, legal route to named gain, rival defense | no route, no gain identity, counterplay risk high, engine contradiction | conversion wording only within proven gain. |
| Scene.Endgame | conditional | material shape, king/pawn route, promotion or fortress identity | middlegame tactics dominate, no route, tablebase or engine contradiction | endgame wording only for named shape. |
| Scene.Counterplay | conditional | resource against rival plan, target, route, timing | rival plan unproven, resource illegal, engine contradiction | counterplay wording only as named resource. |
| Scene.Source | context | source row, opening/game reference, same-board fit | board proof story at public floor, source mismatch | source context only; never leads over board proof. |
| Scene.Quiet | fallback | no other Story reaches public floor | any non-Quiet Story at public floor | quiet wording only as absence of stronger story. |

## Plan Law

Every Plan row is split public. A Plan can support `Scene.Plan`, but the plan
name cannot speak unless the row support is present and no hard blocker applies.

| plan | required affordance | required proof | blockers and caps |
|---|---|---|---|
| Plan.Minority | `queenside_minority_lever` | route, anchor, pawnSupport | no legal lever, tactical override, engine contradiction |
| Plan.Majority | `wing_pawn_majority` | target, anchor, pawnSupport | no passer route, rival blockade, tactical override |
| Plan.CenterBreak | `center_pawn_lever` | route, line or source, pawnSupport | lever illegal, center tactic loses material |
| Plan.FlankBreak | `flank_pawn_lever` | route, anchor, pawnSupport | king exposure without proof, lever illegal |
| Plan.Storm | `king_flank_pawn_step` | route, kingHeat, line if forcing | own king weak, no target, engine contradiction |
| Plan.Expansion | `space_gain_pawn_step` | route, clarity, pawnSupport | overextension tactic, no safe square |
| Plan.Cramp | `piece_mobility_clamp` | target, anchor, persistence | rival break exists, no named clamped piece |
| Plan.Outpost | `pawn_safe_outpost_square` | piece route, anchor, pieceSupport | enemy pawn can chase, no occupying or reachable piece |
| Plan.BadPiece | `boxed_piece` | target piece, route, clarity | no improvement route, tactical override |
| Plan.Reroute | StoryResurrectionLaw S201 | piece route, destination, proof | no legal path, no destination reason |
| Plan.Bishops | `bishop_pair_or_color` | target color, anchor, pieceSupport | closed diagonals without route, tactic override |
| Plan.Blockade | `passer_blockade_square` | blocker route, anchor, line if dynamic | blocker illegal, pawn can advance with gain |
| Plan.OpenFile | `rook_open_file_entry` | rook route, entry square, pieceSupport | entry square controlled with tactic, no rook |
| Plan.Seventh | `rook_seventh_targets` | rook route, targets, line if forcing | no seventh target, rook trapped |
| Plan.ColorBind | `fixed_color_weak_squares` | target color, anchor, persistence | opposite-color counterplay, no fixed weakness |
| Plan.WeakSquare | `pawn_safe_weak_square` | target square, route, pieceSupport | no reachable piece, enemy pawn can contest |
| Plan.Isolani | `isolated_pawn_front_square` | target pawn, blockade or lever route | dynamic break solves it, no attack route |
| Plan.BackwardPawn | `backward_pawn_front_square` | target pawn, anchor, route | pawn can advance safely, no pressure route |
| Plan.HangingPawns | `hanging_pawn_pair` | target pair, lever or blockade route | pair can advance with tempo, tactic override |
| Plan.ChainBase | `pawn_chain_base` | base pawn, attack route, anchor | no attack route, counterplay higher |
| Plan.PasserMake | `candidate_passer_lever` | pawn route, blocker identity, line if dynamic | lever illegal, rival blockade proof |
| Plan.PasserBlock | `passer_block_move` | blocker route, passed pawn target | blocker illegal, pawn promotes by force |
| Plan.Race | StoryResurrectionLaw S214 | both routes, tempo count, proof | tempo not proven, engine contradiction |
| Plan.Trade | StoryResurrectionLaw S215 | pieces, recapture chain, purpose | recapture unclear, tactic loses material |
| Plan.Simplify | StoryResurrectionLaw S216 | exchange path, preserved edge, proof | edge unproven, rival tactic after exchange |
| Plan.KeepPieces | StoryResurrectionLaw S217 | refused trade, preserved resource, proof | resource unproven, trade is forced |
| Plan.Overload | `dual_target_defender` | two targets, defender, legal test | no legal test, defender not tied to targets |
| Plan.Prophy | StoryResurrectionLaw S219 | rival threat, preventing move, proof | threat unproven, prevention illegal |
| Plan.Counterplay | StoryResurrectionLaw S220 | resource, rival plan, timing, proof | rival plan absent, resource too slow |
| Plan.Initiative | StoryResurrectionLaw S221 | forcing route, restricted replies, proof | quiet move breaks sequence, engine contradiction |
| Plan.KingConvert | StoryResurrectionLaw S222 | king route, target gain, proof | mate/material route absent, defense works |
| Plan.Convert | StoryResurrectionLaw S223 | advantage, route to gain, proof | no gain identity, high counterplay risk |

## Tactic Law

Every Tactic row is live forcing only when it has a legal line. Motif facts from
BoardMood can support a tactic, but they cannot prove it.

| tactic | required support | required line | blockers and caps |
|---|---|---|---|
| Tactic.Loose | loose target and attacker | legal capture or pressure line | target defended or recapture equalizes |
| Tactic.Hanging | attacked piece guard map | legal capture and recapture map | defender recaptures favorably |
| Tactic.AbsPin | pinned king line | legal line piece, pinned piece, king behind | line blocked or pinned piece can legally move |
| Tactic.RelPin | pinned valuable target line | legal line piece, screen, target | screen can move with gain or target not valuable |
| Tactic.Pin | pinned-to-king line relation | legal pinning or revealing move | target is king, relation incomplete, or line claim becomes material/king-safety wording |
| Tactic.Skewer | slider, front non-king material target, rear non-king material target | legal move creates or reveals front/rear same-line relation | missing front/rear relation or material/forced wording |
| Tactic.Xray | `white_xray_line` or `black_xray_line` | legal uncovering or pressure line | screen not movable, target defended |
| Tactic.Fork | attacker, two targets | legal move to fork square | fork square unsafe or targets can respond |
| Tactic.DiscoveredAttack | screened line and moving piece | legal discovered move | moving piece illegal or line target absent |
| Tactic.RemoveGuard | one defender guard relation removed | legal remove-guard move | incomplete RemoveGuardProof or stronger material/no-defense wording |
| Tactic.Overload | dual-target guard | legal test of one target | defender can cover both or recapture |
| Tactic.BackRank | back-rank line and flight squares | legal rook or queen check line | king has escape or interposition |
| Tactic.MateNet | king ring, checks, escapes | mate proof or decisive checking line | any legal defense not answered |
| Tactic.SafeCheck | checking move and safe attacker | legal check with reply map | checking piece lost without gain |
| Tactic.PawnFork | pawn move attacks two targets | legal pawn move | target can move with stronger threat |
| Tactic.PawnPush | promotion or tempo pawn step | legal pawn move and reply map | blocker or capture stops it |
| Tactic.Trap | piece mobility clamp and target | legal chase or no-escape line | escape square or counter-threat exists |
| Tactic.QueenHit | queen target and attacker | legal attack or tempo line | queen has stronger reply |
| Tactic.KingOpen | king line or shelter break | legal capture/push/check line | king remains defended or line closes |
| Tactic.Promote | promotion step | legal promotion route | stop square or capture refutes |
| Tactic.InBetween | forcing intermezzo target | legal in-between move | rival can ignore without loss |
| Tactic.Clear | clearance move and line piece | legal clearance route | cleared line has no target |
| Tactic.Decoy | target lure square | legal forcing move to lure | target can decline safely |
| Tactic.Deflect | defender and target | legal deflection move | defender not needed or recaptures |
| Tactic.Tempo | forcing move with gained turn | legal move and rival reply restriction | rival has equal or stronger forcing move |

## Tactics Family Width Map

The width map is a proof-home map, not permission to open a new positive
family. It opens no Story writer, no renderer input, no LLM input, and no
public route.

One tactic name is not one proof system. Similar tactics must share the same
proof home when they rely on the same chess meaning. A proof home name in this
map is a planning label only unless a stage charter, sidecar, negative corpus,
and same-board legal-line tests admit it.

`Tactic.Hanging`, the narrow `Tactic.Fork` vertical slice, the narrow
`Tactic.DiscoveredAttack` vertical slice, the narrow `Tactic.Pin` writer
slice, and the narrow `Tactic.RemoveGuard` writer slice are the only live
positive tactic writers. `Tactic.Skewer` is admitted at Skewer-2 writer scope
only; StoryTable Lead admission and downstream public surfaces are closed. All other
tactic names below are closed until their proof home and writer are explicitly
admitted.
`CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`,
`ExplanationPlan`, and Renderer remain in their current ownership boundaries.
They may be reused only through selected Verdict handoff and family-specific
charters; none of them may read raw proof material or create a new tactic.

Proof-home width:

| proof home | covered tactic names | shared proof shape | reusable current homes | new home needed |
|---|---|---|---|---|
| CaptureProof | `Tactic.Hanging`, `Tactic.Loose`, capture-shaped `Tactic.QueenHit` | legal capture, target identity, defender or recapture map, bounded material result | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer after claim mapping | no for live Hanging; yes before Loose or QueenHit speech broadens capture meaning |
| TargetProof | `Tactic.Fork`, `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`, `Tactic.InBetween` | one legal move creates target relation or tempo pressure; reply map proves rival cannot answer all relevant targets | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` for material ending | yes |
| LineProof | `Tactic.AbsPin`, `Tactic.RelPin`, `Tactic.Xray`, `Tactic.DiscoveredAttack`, `Tactic.Clear`, line-shaped `Tactic.Skewer` | same-board ray, screen, front or rear target, reveal or restriction, legal exploitation line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| PinProof | `Tactic.Pin` | one legal move creates or reveals a pinned-to-king relation over one non-king target | BoardFacts `LineFact`, `LineProof`, `PinProof`, `StoryProof`, `EngineCheck`, `StoryTable`; downstream remains closed | no for narrow Pin-2; yes before broad pin family |
| RemoveGuardProof | `Tactic.RemoveGuard` | one legal move removes one defender guard relation from one non-king material target | BoardFacts guard relation, `RemoveGuardProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; downstream remains bounded | no for narrow RemoveGuard Closeout; yes before broad defender family |
| SkewerProof | `Tactic.Skewer` | one legal move creates or reveals a slider attack on one front non-king material target with a second non-king material target behind it on the same line | BoardFacts `LineFact`, `SkewerProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `TacticSkewer`; StoryTable Lead admission and downstream remain closed | no for Skewer-1 proof home and Skewer-2 writer; yes before StoryTable Lead admission or public speech |
| DefenderProof | `Tactic.Overload`, `Tactic.Deflect`, `Tactic.Decoy` | defender identity, protected target, dependency relation, legal test that removes or distracts the defender | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes |
| KingProof | `Tactic.SafeCheck`, `Tactic.BackRank`, `Tactic.MateNet`, `Tactic.KingOpen` | legal check or king-line action, escape map, interposition or capture replies, no unchecked king claim | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| PromotionProof | `Tactic.PawnPush`, `Tactic.Promote` | legal pawn route, stop squares, capture stops, tempo count, promotion prize | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| MobilityProof | `Tactic.Trap` | target piece mobility map, chase route or no-escape line, rival counter-threat check | BoardFacts legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |

Per-tactic width map:

| map | tactic name | proof shape | existing reusable homes | needs new home | false positive risks | EngineCheck needed | open now | blocked by |
|---|---|---|---|---|---|---|---|---|
| W01 | `Tactic.Hanging` | CaptureProof plus StoryProof plus EngineCheck | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer | no for current slice | recapture equalizes, target is pawn or king, wrong board, illegal capture, engine refutes | attached now for cap or refute | live positive only | stronger wording, other capture tactics, public route, and LLM stay closed |
| W02 | `Tactic.Loose` | CaptureProof or pressure line over an underdefended target | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, if pressure without capture is admitted | loose observation is not a gain, defender exists, pressure is too slow, capture equalizes | yes for cap or refute | no | CaptureProof broadening plus loose negative corpus |
| W03 | `Tactic.QueenHit` | TargetProof with optional CaptureProof | `StoryProof`, optional `CaptureResult`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, TargetProof | queen can move with tempo, attacker is unsafe, rival has stronger forcing reply, target hit has no gain | yes | no | TargetProof charter plus queen-target negative corpus |
| W04 | `Tactic.Fork` | TargetProof over two targets and reply map | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, `MultiTargetProof` | fork square unsafe, one target can move with tempo, rival has equal or stronger forcing move, no gain remains | yes | narrow Fork-4 backend only | renderer, LLM, public route, PawnFork, Skewer, QueenHit, Tempo, InBetween |
| W05 | `Tactic.PawnFork` | TargetProof with pawn legal move and two target attacks | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, TargetProof with pawn route rule | pawn move illegal, pawn is pinned, target can reply with stronger threat, promotion context contaminates proof | yes | no | TargetProof after Fork plus pawn-fork negative corpus |
| W06 | `Tactic.Skewer` | SkewerProof over one front target and one rear target on a slider line | BoardFacts `LineFact`, `SkewerProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `TacticSkewer` | no for Skewer-2 writer; yes before StoryTable Lead admission or public speech | front/rear relation missing, rear target not reachable, line can be blocked, material or forced wording too strong | yes before StoryTable Lead admission | Skewer-2 writer only | StoryTable Lead admission, ExplanationPlan, renderer, LLM, public route, production API |
| W07 | `Tactic.Tempo` | TargetProof over gained turn and restricted rival reply | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, TargetProof with reply restriction | rival has equal forcing move, gained turn has no target, move is only a threat label | yes | no | TargetProof reply-map charter |
| W08 | `Tactic.InBetween` | TargetProof over forcing intermezzo and ignored-loss test | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, TargetProof with forcing reply map | rival can ignore, in-between move loses material, first threat was not real, move order is illegal | yes | no | TargetProof reply-map charter plus intermezzo corpus |
| W09 | `Tactic.AbsPin` | LineProof with king behind screen and legal restriction | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | pinned piece can legally move, line can be blocked, tactic does not exploit pin, king line is stale | yes | no | LineProof charter plus pin exploitation corpus |
| W10 | `Tactic.RelPin` | LineProof with valuable rear target and screen | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | rear target not valuable enough, screen can move with gain, line pressure has no legal use | yes | no | LineProof charter plus relative-pin corpus |
| W25 | `Tactic.Pin` | PinProof plus StoryProof over one legal pinned-to-king relation | BoardFacts `LineFact`, `LineProof`, `PinProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for narrow Pin Closeout; yes before broad pin family | target is king, relation incomplete, slider absent, king-behind-target missing, material claim leaks, Defense or RemoveGuard meaning leaks | yes | Pin Closeout hard cleanup only | no Material claim, king-safety claim, mate threat, cannot-move wording, Defense ownership, RemoveGuard ownership, broad AbsPin or RelPin family, public route, production API, or public/user-facing LLM narration |
| W11 | `Tactic.Xray` | LineProof over screened ray and legal reveal or pressure | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | screen not movable, target defended, reveal loses tempo, ray observation is only geometry | yes | no | LineProof charter plus xray negative corpus |
| W12 | `Tactic.DiscoveredAttack` | LineProof over screened line and moving piece | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer, LLM smoke; public narration remains closed for Line Closeout | yes, LineProof | moving piece illegal, line target absent, discovered attack can be answered, moving piece hangs, target is king | yes | Line Closeout hard cleanup only | no Material claim, Pin, Skewer, XRay, RemoveGuard, public/user-facing LLM narration, public route, or production API |
| W13 | `Tactic.Clear` | LineProof over clearance move and newly usable line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | cleared line has no target, clearance loses material, rival closes the line, route not legal | yes | no | LineProof clearance charter |
| W14 | `Tactic.RemoveGuard` | RemoveGuardProof plus StoryProof | BoardFacts guard relation, `RemoveGuardProof`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, `DeterministicRenderer`, LLM smoke | no for narrow RemoveGuard Closeout; yes before broad defender family | legal move missing, same-board proof missing, target is king, defender did not guard target, defender still guards target after move, Material or Hanging claim leaks, Defense or Pin meaning leaks, DiscoveredAttack meaning leaks | yes | RemoveGuard Closeout hard cleanup only | no broad deflection tactic, overloaded defender theory, no-defender claim, wins-material claim, Material claim, Hanging claim, Defense claim, Pin ownership, DiscoveredAttack ownership, public route, production API, or public/user-facing LLM narration |
| W15 | `Tactic.Overload` | DefenderProof over dual target dependency and legal test | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof | defender can cover both, one target irrelevant, reply order solves both threats, relation is only a plan label | yes | no | DefenderProof dual-target charter |
| W16 | `Tactic.Deflect` | DefenderProof over defender displacement and target gain | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof | defender not needed, defender can decline, displacement gives counterplay, target gain absent | yes | no | DefenderProof displacement corpus |
| W17 | `Tactic.Decoy` | DefenderProof or TargetProof over lure square and forced acceptance | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, DefenderProof with lure relation | target can decline safely, lure square is not exploitable, follow-up is illegal, gain is unproven | yes | no | DefenderProof lure charter |
| W18 | `Tactic.SafeCheck` | KingProof over legal check and reply map | BoardFacts king facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | checking piece is lost without gain, legal reply wins tempo, check text becomes best-move or engine wording | yes | no | KingProof safe-check charter |
| W19 | `Tactic.BackRank` | KingProof plus LineProof over back rank and escape map | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | escape square exists, interposition works, checking line illegal, mate wording leaks in | yes | no | KingProof escape corpus |
| W20 | `Tactic.MateNet` | KingProof over checks, escape map, and answered defenses | BoardFacts king facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | any legal defense survives, net is only pressure, engine depth missing, mate wording too strong | yes, and mate proof must be explicit | no | KingProof mate-net charter and mate-negative corpus |
| W21 | `Tactic.KingOpen` | KingProof plus LineProof over shelter break or king line | BoardFacts king and line facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, KingProof | king remains defended, opened line closes, sacrifice lacks proof, attack wording outruns evidence | yes | no | KingProof shelter-break corpus |
| W22 | `Tactic.PawnPush` | PromotionProof or TargetProof over pawn step, stop squares, and reply map | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, PromotionProof | blocker stops pawn, capture refutes, tempo step has no gain, promotion claim leaks in | yes | no | PromotionProof pawn-route charter |
| W23 | `Tactic.Promote` | PromotionProof over legal promotion route and stop-square proof | BoardFacts pawn and legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, PromotionProof | stop square exists, pawn is captured, promotion race lost, tablebase or engine refutes | yes | no | PromotionProof promotion-route corpus |
| W24 | `Tactic.Trap` | MobilityProof over target mobility and chase or no-escape line | BoardFacts legal move facts, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, MobilityProof | escape square exists, counter-threat outranks trap, chase line illegal, trapped piece not valuable | yes | no | MobilityProof mobility corpus |

Safe opening order:

1. Keep `Tactic.Hanging` as the first live positive tactic while this map closes
   the width question.
2. Admit TargetProof/MultiTargetProof for `Tactic.Fork` only, with reply-map proof, same-board
   legal move proof, EngineCheck cap/refute attachment, and fork negative
   corpus. `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`,
   `Tactic.Tempo`, and `Tactic.InBetween` stay closed even though they share
   TargetProof shape.
3. Admit the remaining TargetProof tactics one by one only after the shared
   reply-map shape proves it does not open sibling tactics by name alone.
4. Admit LineProof for pin and ray tactics only after line exploitation is
   proven, not merely line geometry.
5. RemoveGuardProof admitted for narrow `Tactic.RemoveGuard`; broader DefenderProof for overload, deflection, and decoy remains closed.
6. Admit broader DefenderProof only after defender identity, sole-dependency,
   and target-gain tests exist.
7. Admit PromotionProof only after pawn route, stop-square, capture-stop, and
   tempo-count tests exist.
8. Admit KingProof only after escape, interposition, capture reply, and mate
   wording boundaries are proven.
9. Admit MobilityProof only after legal mobility and counter-threat tests
   exist.

Opening a proof home does not open all tactic names in that home. Each tactic
still needs a named Story writer, family proof sidecar, negative corpus,
EngineCheck attachment rule, StoryTable role rule, selected-Verdict
ExplanationPlan mapping, renderer wording boundary, and any LLM smoke boundary.
Production or user-facing LLM narration and public route `200` remain closed
for every non-Hanging tactic. Renderer wording opens tactic by tactic only; the
current Fork slice opens deterministic Fork renderer text in Fork-8 and Fork
LLM smoke in Fork-9.

## Fork-0 Tactic.Fork Charter

Fork is a multi-target Story, not a capture Story.

MultiTargetProof gives the reason. TacticFork writer gives permission.

The first `Tactic.Fork` scope opens only a simple non-pawn multi-target tactic:

- legal move to fork square
- attacker after move attacks two named targets
- two targets have bounded value or importance
- bounded reply map exists
- complete StoryProof
- same-board proof
- named `Tactic.Fork` writer
- EngineCheck support/cap/refute path
- StoryTable ordering with Hanging vs Fork
- ExplanationPlan mapping from selected Fork Verdict
- deterministic renderer text from Fork ExplanationPlan only
- LLM smoke from Fork ExplanationPlan and RenderedLine only

The first Fork scope does not open public material, winning, decisive, forced,
best-move, only-move, no-counterplay, blunder, free-piece, or engine-says
claims.

`Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`,
`Tactic.InBetween`, `Scene.Material`, `Scene.Defense`, Plan, Strategy,
public/user-facing LLM narration, public route `200`, production API, engine
PV commentary, and best-move explanation remain closed.

Fork deterministic renderer text opens only in Fork-8 and only from
ExplanationPlan. Fork LLM smoke opens only in Fork-9 and only from
ExplanationPlan plus RenderedLine. Stage 8 output boundaries remain unchanged:
renderer and LLM smoke may not read raw Story, MultiTargetProof, EngineCheck,
BoardFacts, BoardMood, raw PV, proofFailures text, or source rows directly.

## Fork-1 Geometry Readiness

Fork geometry readiness is internal `MultiTargetProof` evidence only.
BoardFacts may provide observation facts, current board, legal moves, pieces,
and attack tests, but BoardFacts must not say that a fork works.

`MultiTargetProof` may record:

- current board same-board proof
- legal move to the fork square
- attacker piece
- attacker-after-move on the fork square
- post-move attacked target squares
- two named target pieces

Allowed geometry findings are limited to legal move exists, attacker can move
to the fork square, and after that move the attacker attacks target A and
target B.

Fork-1 does not create a public Story, Verdict, renderer sentence, LLM
narration, public route `200`, or public material claim by itself. It does not
say fork succeeds, wins material, wins queen, decisive fork, forced fork, or
best move.

## Fork-2 MultiTargetProof

`MultiTargetProof` is the family-specific proof home for the first Fork slice.
It creates Fork evidence, but it does not directly create a public Story.
`TacticFork` writer permission remains a separate permission step.

`MultiTargetProof` must carry:

- attacker
- legal move to fork square
- fork square
- target A
- target B
- target value or target class
- reply map
- same-board proof
- missing evidence

`MultiTargetProof` is complete only when the move is legal, the attacker
exists, two distinct targets exist, targets are enemy pieces or valid tactical
targets, the attacker attacks both targets after the move, and same-board proof
exists.

The first Fork proof scope is non-pawn attacker only, preferably knight-shaped,
with no pawn fork, no skewer, no queen-hit-only tactic, and no king or mate
claim. It must not create material-win, best-move, forced, decisive, or
no-counterplay claims.

## Fork-3 TacticFork Writer

`TacticFork` is the named positive writer for the first narrow `Tactic.Fork`
Story.

`TacticFork` may create a Story only when:

- Story scene is `Scene.Tactic`
- tactic is `Tactic.Fork`
- StoryProof is complete
- MultiTargetProof is complete
- same-board proof is present
- legal move to the fork square is present
- two named targets are present
- target relation is proven after the move
- writer is `StoryWriter.TacticFork`
- EngineCheck does not `Refutes`

The first allowed Fork meaning is limited to move attacks two targets and move
creates a fork on named targets.

`TacticFork` does not open wins-queen, wins-material, decisive, forced,
best-move, only-move, no-counterplay, blunder, or engine-says wording. It does
not open Fork renderer text, public/user-facing Fork LLM narration, public
route `200`, pedagogy, Strategy, Plan, Scene.Material, Scene.Defense, or broad
tactic-family speech.

Completion standard: one narrow Fork Story with complete StoryProof,
complete MultiTargetProof, same-board proof, and no EngineCheck Refutes result
may enter StoryTable as a Lead. A Fork-looking row with unproven target
relation, missing writer, incomplete proof, or EngineCheck Refutes result must
not lead.

## Fork-4 Negative Corpus

Fork-looking false positives stay silent unless `MultiTargetProof`,
StoryProof, the named `Tactic.Fork` writer, same-board proof, and EngineCheck
guards all pass.

The Fork negative corpus must cover:

- no legal move
- missing attacker
- missing fork square
- missing two targets
- duplicated target
- own-piece target
- target not attacked after the move
- fork square unsafe with no compensation
- one reply saving both targets
- stronger rival reply refuting the Story
- pawn fork trying to enter `Tactic.Fork`
- skewer trying to enter `Tactic.Fork`
- queen-hit-only trying to enter `Tactic.Fork`
- incomplete StoryProof
- incomplete MultiTargetProof
- EngineCheck Refutes
- high Proof score only

Completion standard: a fork-looking shape with insufficient proof produces no
Lead or becomes Blocked, and sibling tactics cannot piggyback on Fork.

## Fork-5 EngineCheck For Tactic.Fork

Fork reuses the existing `EngineCheck` sidecar. There is no `ForkEngineCheck`
type.

`EngineCheck` may attach to an existing Fork Story with status `Supports`,
`Caps`, `Refutes`, or `Unknown` only when:

- the Fork Story already exists
- same-board proof is present
- checked move binds the same Story route
- checked move is the same legal line
- fresh or depth evidence is present
- EngineCheck evidence is ready

`Supports` creates no new claim. `Caps` limits strength but creates no engine
wording. `Refutes` blocks the Fork Story. `Unknown` is diagnostic only and
creates no engine expression.

Engine evidence cannot create Fork by itself. Engine PV explanation,
best-move explanation, winning, decisive, forced, no-counterplay, public eval,
and engine-says claims remain forbidden.

Completion standard: EngineCheck can support, cap, or refute an existing Fork
Story, but engine lines, engine eval, PV-shaped data, missing-depth checks,
route-mismatched checks, and engine-only checks cannot create Fork or attach as
Fork evidence.

## Fork-6 StoryTable Hanging Vs Fork

Fork-6 opens only StoryTable role ordering for existing `Tactic.Hanging` Story
rows and existing narrow `Tactic.Fork` Story rows.

StoryTable may assign `Lead`, `Support`, `Context`, and `Blocked` roles with
deterministic ordering. A refuted Fork, incomplete Fork, writerless Fork, or
Fork without `MultiTargetProof` becomes `Blocked`. Hanging and Fork may both be
eligible for `Lead`; StoryTable chooses one deterministic `Lead` without
creating a new Story or new chess meaning.

Support and Context are not sentences. MultiTargetProof text stays internal and
does not become public wording.

StoryTable must not create Fork, rank Fork by raw engine eval or raw PV, expose
MultiTargetProof text, or use renderer wording as an ordering input.

Completion standard: when Hanging and Fork exist together, StoryTable performs
deterministic role ordering only. It creates no new chess meaning, public
claim, renderer text, LLM narration, public route `200`, pedagogy, Strategy,
Plan, `Scene.Material`, `Scene.Defense`, or sibling tactic family.

## Fork-7 ExplanationPlan For Fork

Fork-7 opens only ExplanationPlan mapping for a selected narrow `Tactic.Fork`
Verdict.

Allowed input:

- selected Verdict only

Forbidden input:

- MultiTargetProof
- EngineCheck
- CaptureResult
- BoardFacts
- raw PV
- proofFailures
- source row

The first allowed Fork claim keys are:

- `forks_two_targets`
- `attacks_two_targets`

The selected Fork plan may carry role, scene, tactic, side, target,
secondaryTarget, anchor, route, evidenceLine, bounded strength, forbidden
wording, and relation structure. It does not read MultiTargetProof,
EngineCheck, CaptureResult, BoardFacts, raw PV, proofFailures, or source rows
directly.

Forbidden Fork claim keys and wording include:

- `wins_material_by_fork`
- `wins_queen`
- `decisive_fork`
- `forced_win`
- `best_move`
- `no_counterplay`

Support, Context, and Blocked Fork Verdicts create no standalone claim and no
public sentence. They may enter ExplanationPlan only as relation structure;
Blocked remains debug-only. `engineStrengthLimited` suppresses allowed claim
keys and strengthens forbidden wording.

Fork-7 does not open Fork renderer text, public/user-facing Fork LLM
narration, public route `200`, pedagogy, Strategy, Plan, `Scene.Material`,
`Scene.Defense`, engine PV commentary, best-move explanation, or sibling
tactic families.

Completion standard: Fork ExplanationPlan creates no meaning stronger than the
selected Verdict.

## Fork-8 Deterministic Renderer For Fork

Fork-8 opens only deterministic renderer text for a selected Fork
ExplanationPlan.

Allowed input:

- ExplanationPlan only

Forbidden input:

- MultiTargetProof
- EngineCheck
- CaptureResult
- BoardFacts
- BoardMood
- raw PV
- proofFailures
- source row
- raw Verdict
- source Story

The first Fork renderer template is:

`{route} forks the pieces on {targetA} and {targetB}.`

`targetA` and `targetB` must come from structured `target` and
`secondaryTarget` fields already present in the Fork ExplanationPlan. The
renderer must not read MultiTargetProof, EngineCheck, CaptureResult,
BoardFacts, BoardMood, raw PV, proofFailures, source rows, raw Verdict, or
source Story to recover missing targets.

The first Fork renderer permission requires:

- role is `Lead`
- scene is `Scene.Tactic`
- tactic is `Tactic.Fork`
- allowed claim is `forks_two_targets`
- bounded strength
- non-debug plan
- route and evidenceLine match
- target and secondaryTarget are present
- forbidden wording is present

Forbidden Fork renderer wording includes:

- wins queen
- wins material
- decisive
- forced
- best move
- engine says
- no counterplay
- blunder

Support, Context, Blocked, capped, and engine-refuted Fork plans produce no
text. The `attacks_two_targets` claim key remains an internal allowed claim key
but does not open the first Fork renderer template.

Fork-8 itself does not open Fork LLM smoke, public/user-facing LLM narration,
public route `200`, production API, pedagogy, Strategy, Plan,
`Scene.Material`, `Scene.Defense`, engine PV commentary, best-move explanation,
material-win wording, wins-queen wording, or sibling tactic families.

Completion standard: Fork renderer text is no stronger than the selected Fork
ExplanationPlan.

## Fork-9 LLM Smoke For Fork

Fork-9 opens only LLM smoke for selected Fork ExplanationPlan and RenderedLine.
It does not open production API validation, user-facing LLM output, public
route `200`, streaming, pedagogy, Strategy, Plan, `Scene.Material`,
`Scene.Defense`, engine PV commentary, best-move explanation, material-win
wording, wins-queen wording, or sibling tactic families.

Allowed input:

- ExplanationPlan
- RenderedLine

8B Codex CLI prompt smoke allowed input for Fork:

- renderedText
- claimKey
- strength
- forbidden wording list
- instruction: "Rephrase only. Do not add chess facts."

Forbidden input:

- raw Verdict
- Story
- MultiTargetProof
- EngineCheck
- BoardFacts
- BoardMood
- CaptureResult
- EngineEval
- EngineLine
- engine eval
- raw PV
- proofFailures
- source row

Fork LLM smoke must reject output that adds:

- new move
- new line
- new tactic
- new plan
- engine mention
- best move
- forced
- winning
- decisive
- blunder
- wins queen
- wins material
- target piece identity absent from renderedText
- claim stronger than deterministic text

Completion standard: Fork LLM smoke does not strengthen Fork deterministic
text.

## Fork Slice Closeout Pass

Fork slice closeout goal: audit that the Fork closeout opened only the narrow `Tactic.Fork` vertical slice.

Scope audit:

- opened by Fork closeout: narrow non-pawn `Tactic.Fork` only
- closed: `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`, `Tactic.InBetween`
- not opened by Fork closeout: `Scene.Material`, `Scene.Defense`, Plan, Strategy
- closed: public route `200`, production API, public/user-facing LLM narration, pedagogy, engine PV commentary, best-move explanation, material-win wording, wins-queen wording, and sibling tactic-family speech

Authority audit:

- MultiTargetProof does not replace CaptureResult.
- MultiTargetProof does not replace StoryProof.
- MultiTargetProof does not replace EngineCheck.
- MultiTargetProof does not replace StoryTable.
- MultiTargetProof supplies the TargetProof-shaped reason: legal move, attacker, fork square, two target identities, post-move target relation, reply map, same-board proof, bounded result, and missing evidence.
- `TacticFork` writer gives Story permission.
- `StoryProof` remains the same-board Story proof tuple.
- `EngineCheck` checks, caps, or refutes only an existing Fork Story.
- `StoryTable` assigns Lead, Support, Context, or Blocked without creating Fork.
- `ExplanationPlan`, Renderer, and LLM smoke receive only their downstream inputs and create no new chess meaning.

Negative corpus audit:

Fork-looking false positives either produce no Story, no Lead, or Blocked.

The closeout negative corpus remains:

- no legal move
- missing attacker
- missing fork square
- missing two targets
- duplicated target
- own-piece target
- target not attacked after the move
- fork square unsafe with no compensation
- one reply saving both targets
- stronger rival reply or EngineCheck Refutes
- pawn fork trying to enter `Tactic.Fork`
- skewer trying to enter `Tactic.Fork`
- queen-hit-only trying to enter `Tactic.Fork`
- incomplete StoryProof
- incomplete MultiTargetProof
- high Proof score only
- LLM rephrase stronger than deterministic Fork text

Cleanup and consolidation:

No new markdown authority file, public row family, public route, production API, or sibling tactic writer opens in Fork closeout.

`MultiTargetProof` is the first live TargetProof-shaped home. It remains a
proof shape, not a tactic family. The current constructor is scoped to narrow
non-pawn Fork, but the shape can support subsequent target-relation tactics only
after each named family opens its own permission path.

The proof shape remains reusable for subsequent PawnFork, Skewer, QueenHit, Tempo, or InBetween work only after each family gets its own named writer, negative corpus, EngineCheck rule, StoryTable rule, ExplanationPlan mapping, renderer boundary, and LLM smoke boundary.

Next-stage handoff at Fork closeout:

The next family candidates were `Scene.Material` or `Scene.Defense`.

Fork does not open `Scene.Material` or `Scene.Defense` by implication.
Material-3 separately opens only the narrow named `Scene.Material` writer.

Fork does not create material, defense, plan, strategy, or tempo public
meaning. Target values and bounded result stay internal proof evidence until a
separate family authority admits a public claim.

`One tactic name is not one proof system.`
`One proof shape may support multiple tactics.`
`One chess meaning, one home.`

## Material-0 Scene.Material Charter

Material-0 opens only the charter for the first narrow `Scene.Material` slice.
It does not open a runtime writer, StoryTable role rule, ExplanationPlan
mapping, deterministic renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, or any winning claim.

Core sentences:

`Material is a scene, not a tactic.`
`CaptureResult` or `ExchangeResult` is the proof home.
`Scene.Material` is the Story label.
`material_change` is the speech claim.

First scope:

- simple capture or exchange result
- same-board proof
- legal line
- bounded recapture or exchange check
- known material result
- no tactic label required

`Scene.Material` may label a proof-backed material change without calling that
change a tactic. `CaptureResult` or `ExchangeResult` supplies the proof. The
Material Story supplies the scene label. The speech claim remains a structured
claim key only and starts as `material_change` when the downstream
ExplanationPlan slice opens.

Forbidden Material-0 meanings:

- winning
- decisive
- blunder
- conversion
- best move
- forced
- no counterplay
- engine says
- full evaluation claim

Material gain is not winning. A positive bounded material result may identify a
material change only. It must not become advantage, evaluation, conversion,
decisive result, blunder diagnosis, best-move claim, forced-line claim,
no-counterplay claim, or engine wording.

`Scene.Material` is not another name for `Tactic.Hanging`. Hanging remains a
tactic Story whose permission comes from the named `TacticHanging` writer and
positive `CaptureResult`. Material is a scene Story whose permission requires a
separate Material writer slice. `CaptureResult` remains proof evidence, not a
Story label and not public speech.

Completion standard: `Scene.Material` must not become another name for `Tactic.Hanging` or `CaptureResult`.

## Material-1 Proof Home Decision

Material-1 decides the proof home for the first narrow Material scope.

Decision: the first `Scene.Material` scope reuses `CaptureResult`.

Reasons:

- the first scope is centered on one legal capture line
- `CaptureResult` already records same-board proof, legal capture, capturer,
  target, captured value, recapture candidates, bounded material result, and
  missing evidence
- immediate bounded recapture is still capture proof in this scope
- adding `ExchangeResult` now would split the same simple capture material
  meaning across two proof homes

`CaptureResult is capture proof.`
`ExchangeResult is bounded exchange proof.`
`Scene.Material is not proof.`

Do not create `ExchangeResult` in Material-1.

A new `ExchangeResult` proof home opens only when Material needs a bounded multi-move exchange sequence that `CaptureResult` cannot own without overloading capture meaning.

Material-1 keeps one material meaning in one proof home:

- single legal capture and immediate bounded recapture use `CaptureResult`
- multi-move exchange sequence is outside the first proof scope
- `Scene.Material` may consume only the selected proof-backed result after the
  Material writer slice opens
- `Scene.Material` does not repair missing `CaptureResult` evidence
- `Scene.Material` does not read raw BoardFacts, raw BoardMood, raw engine
  evidence, proofFailures text, source rows, renderer wording, or LLM wording
  to create proof

Completion standard: one material meaning has one proof home.

## Material-2 Material / Exchange Proof Shape

Material-2 opens only the bounded material proof shape. It does not open a
Material writer, StoryTable role rule, ExplanationPlan mapping, deterministic
renderer text, LLM smoke, public route `200`, production API, `Scene.Defense`,
Plan, Strategy, or any public sentence.

The first proof shape lives in `CaptureResult`. It may record exchange-shaped
fields for the simple capture and immediate bounded recapture scope, but it
does not create `ExchangeResult`.

Required proof information:

- side
- legal line
- captured pieces
- recapture candidates
- bounded exchange sequence
- material result
- same-board proof
- missing evidence

Allowed Material-2 proof outcomes:

- line leaves White up material
- line leaves Black up material
- exchange result is known

Forbidden Material-2 meanings:

- winning position
- decisive advantage
- conversion
- blunder
- best move
- forced line

`CaptureResult` may calculate the bounded material result for the checked
capture line. `capturedPieces` and `boundedExchangeSequence` are proof-shape
fields only. They do not create a public Story, a Material writer, a speech
claim, a renderer sentence, or LLM input.

Unknown material, illegal capture, own-piece target, king target, missing
same-board proof, or missing legal line stays missing evidence. The proof shape
may report the missing coordinate internally, but it must not repair the proof
or produce text.

Completion standard: the proof calculates material result, but it does not create a public Story or sentence.

## Material-3 Scene.Material Writer

Material-3 opens the named `SceneMaterial` writer only. It does not open
ExplanationPlan mapping, deterministic renderer text, LLM smoke, public route
`200`, production API, `Scene.Defense`, Plan, Strategy, winning, decisive,
blunder, conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

`SceneMaterial` may create a Story only when:

- scene is `Scene.Material`
- StoryProof is complete
- material proof is complete
- same-board proof is present
- legal line is present
- material result is known
- writer is `StoryWriter.SceneMaterial`
- EngineCheck does not `Refutes`

The first allowed Material meanings are:

- this line changes material balance
- this exchange leaves one side ahead in material

Forbidden Material-3 meanings:

- winning
- decisive
- blunder
- conversion
- best move
- forced
- no counterplay
- engine says

The writer consumes `CaptureResult` as proof. It does not make
`CaptureResult` public speech, and it does not turn Material into
`Tactic.Hanging`. A Hanging Story still requires the `TacticHanging` writer and
the tactic label. A Material Story requires the `SceneMaterial` writer and no
tactic label.

EngineCheck may support, cap, or refute only an existing Material Story.
`Supports` creates no engine claim. `Caps` creates only the internal
strength-limited diagnostic. `Refutes` blocks the Material Story. Engine eval
and engine line data still cannot create Material by themselves.

Support, Context, ExplanationPlan, renderer, and LLM wording for Material stay
closed in Material-3.

Completion standard: one narrow Material Story with proof can enter StoryTable.

## Material-4 Material Negative Corpus

Material-4 opens only the Material negative corpus. It does not open
ExplanationPlan mapping, deterministic renderer text, LLM smoke, public route
`200`, production API, `Scene.Defense`, Plan, Strategy, winning, decisive,
blunder, conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Material-looking false positives:

- legal line missing
- same-board proof missing
- capture exists but bounded recapture erases the material result
- exchange result unclear
- target is king
- material result is zero
- EngineCheck `Refutes`
- StoryProof incomplete
- material proof incomplete
- tactic writer tries to speak Material
- Hanging tries to auto-duplicate as Material
- Fork tries to auto-duplicate as Material
- high Proof score only

If `SceneMaterial.write` cannot build a complete proof-backed Story, no
Material Story is created. If a forged or duplicated Material-looking row
enters StoryTable, it must not become Lead. Rows with high Proof scores but no
named `SceneMaterial` writer and complete bounded material proof are Blocked.

Hanging and Fork do not auto-create Material speech. A tactic Story may carry
material-shaped evidence, but it remains in its tactic family unless the named
`SceneMaterial` writer independently creates a complete `Scene.Material`
Story. A tactic writer must not speak Material by changing only the scene
label.

Completion standard: material-looking rows without bounded material proof become no Lead or Blocked.

## Material-5 EngineCheck for Scene.Material

Material-5 opens only existing `EngineCheck` reuse for `Scene.Material`. It
does not open a new engine proof type, ExplanationPlan mapping, deterministic
renderer text, LLM smoke, public route `200`, production API, `Scene.Defense`,
Plan, Strategy, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.

Allowed Material-5 statuses:

- `Unknown`
- `Supports`
- `Caps`
- `Refutes`

Material `EngineCheck` attachment requires:

- Material Story already exists
- same-board proof
- same Story route
- same legal line
- fresh or depth evidence

Forbidden Material-5 meanings and shortcuts:

- engine creates Material Story
- engine eval becomes public truth
- PV becomes explanation
- best move explanation
- winning claim
- `MaterialEngineCheck` duplicate type

`EngineCheck.fromStory` is the Material attachment path. Raw
`EngineCheck.fromEvidence`, eval-only evidence, PV-only evidence,
route-mismatched evidence, different-board evidence, stale evidence, and
depth-missing evidence stay diagnostic only and cannot attach to
`Scene.Material`.

`Supports` creates no engine claim. `Caps` creates only the internal
strength-limited diagnostic. `Refutes` blocks the existing Material Story.
Engine eval and PV remain evidence only and must not become public truth or
explanation.

Completion standard: `EngineCheck` may support, cap, or refute an existing Material Story, but it must not create Material.

## Material-6 StoryTable Integration

Material-6 opens only StoryTable integration for existing Hanging, Fork, and Material rows.
It does not open Material Story creation by StoryTable, ExplanationPlan
mapping, deterministic renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, winning, decisive, blunder,
conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Allowed Material-6 StoryTable roles and inputs:

- Lead
- Support
- Context
- Blocked
- deterministic ordering

Material-6 StoryTable rules:

- Refuted Material becomes Blocked
- incomplete Material becomes Blocked
- writerless Material becomes Blocked
- Material without material proof becomes Blocked
- Hanging, Fork, and Material can compete for Lead
- Material with the same route, target, and material result as positive Hanging orders behind Hanging
- Support and Context are not sentences

Forbidden Material-6 meanings and shortcuts:

- StoryTable creates Material
- raw engine eval ranks Material
- material proof text becomes public
- renderer wording affects order
- Material silently opens conversion or winning

StoryTable may order only existing proof-backed Story rows. It may use role
eligibility, publicStrength, Story identity, writer presence, and blocked
status. It must not create Material, read raw engine eval as ranking
authority, speak material proof text, use renderer wording, or upgrade Material
into conversion, winning, decisive, blunder, best-move, forced,
no-counterplay, or engine-says meaning.

Completion standard: StoryTable deterministically orders the three Story families without creating new chess meaning.

## Material-7 ExplanationPlan for Scene.Material

Material-7 opens only ExplanationPlan mapping for selected `Scene.Material` Verdicts.
It does not open Material renderer text, LLM smoke, public route `200`,
production API, `Scene.Defense`, Plan, Strategy, winning, decisive, blunder,
conversion, best-move, forced, no-counterplay, engine-says, or
full-evaluation claims.

Allowed Material-7 input:

- selected Verdict only

Forbidden Material-7 inputs:

- material proof directly
- `CaptureResult`
- `ExchangeResult`
- `EngineCheck`
- `BoardFacts`
- raw PV
- proofFailures
- source row

Allowed Material-7 claim keys:

- `material_balance_changes`
- `line_leaves_material_gain`
- `exchange_leaves_side_ahead`

The first emitted Material claim key is `material_balance_changes`.

Forbidden Material-7 claim keys:

- `winning_position`
- `decisive_advantage`
- `conversion`
- `blunder`
- `best_move`
- `forced_win`
- `no_counterplay`

The Material ExplanationPlan may copy selected Verdict identity into the speech
boundary: role, scene, side, target, anchor, route, evidenceLine, bounded
strength, relation diagnostics, and forbidden wording. It must not read
material proof directly, `CaptureResult`, `ExchangeResult`, `EngineCheck`,
`BoardFacts`, raw PV, proofFailures, or source row data to create a claim.

Only an uncapped selected Lead Material Verdict may carry the first emitted
Material claim key.
Support, Context, Blocked, capped, and engine-refuted Material plans create no standalone public claim.
Material-7 does not open Material renderer text, LLM smoke, public route `200`, production API, or public sentence output.

Completion standard: Material ExplanationPlan creates no meaning stronger than the selected Verdict.

## Material-8 Deterministic Renderer

Material-8 opens only deterministic renderer text for selected `Scene.Material` ExplanationPlan.
It does not open LLM smoke, public route `200`, production API,
`Scene.Defense`, Plan, Strategy, winning, decisive, blunder, conversion,
best-move, forced, no-counterplay, engine-says, technically-winning, or
full-evaluation claims.

Allowed Material-8 input:

- ExplanationPlan only

First Material-8 templates:

`This line leaves White ahead in material.`
`After {route}, White comes out ahead in material.`

The route template may use only the route, side, allowed claim key, strength,
and forbidden wording already present in the selected Material
ExplanationPlan. It must not read Material Story, raw Verdict, material proof,
`CaptureResult`, `ExchangeResult`, `EngineCheck`, `BoardFacts`, raw PV,
proofFailures, source row data, or renderer wording from any earlier row.

Forbidden Material-8 wording:

- winning
- decisive
- blunder
- forced
- best move
- no counterplay
- engine says
- conversion
- technically winning

Only a Lead, non-debug, bounded Material ExplanationPlan with
`material_balance_changes`, route, side, and forbidden wording present may
render deterministic Material text. Support, Context, Blocked, capped,
engine-refuted, no-claim, wrong-claim, debug-only, missing-route, or
forbidden-wording Material plans render no standalone text.

Material-8 does not open LLM smoke, public/user-facing narration, public route
`200`, production API, pedagogy, engine PV commentary, best-move explanation,
winning-position wording, decisive wording, conversion wording, or sibling
Story families.

Completion standard: Renderer text is no stronger than the Material ExplanationPlan.

## Material-9 LLM Smoke

Material-9 opens only LLM smoke for selected Material ExplanationPlan and RenderedLine.
It does not open production API validation, public/user-facing LLM narration,
public route `200`, streaming, pedagogy, `Scene.Defense`, Plan, Strategy,
engine PV commentary, best-move explanation, winning-position wording,
decisive wording, forced wording, blunder wording, conversion wording,
engine-says wording, full-evaluation claims, or sibling Story families.

Allowed Material-9 input:

- ExplanationPlan
- RenderedLine

8B Material Codex CLI input:

- renderedText
- claimKey
- strength
- forbidden wording
- instruction: Rephrase only. Do not add chess facts.

Forbidden Material-9 input:

- raw Verdict
- Story
- material proof
- CaptureResult
- ExchangeResult
- EngineCheck
- BoardFacts
- engine eval
- raw PV
- proofFailures

Material LLM smoke must reject output that adds:

- new move
- new line
- new tactic
- new plan
- engine mention
- winning, decisive, forced, blunder, or best-move wording
- conversion claim
- stronger claim

The Material smoke prompt may receive only renderedText, claimKey, strength,
forbidden wording, and the instruction `Rephrase only. Do not add chess facts.`
It must not receive raw Verdict, Story, material proof, `CaptureResult`,
`ExchangeResult`, `EngineCheck`, `BoardFacts`, engine eval, raw PV,
proofFailures, or source row data.

Material-9 does not open public/user-facing LLM narration, public route `200`,
production API, pedagogy, engine PV commentary, best-move explanation,
winning, decisive, forced, blunder, conversion, no-counterplay, or
full-evaluation claims.

Completion standard: LLM smoke does not strengthen Material text.

## Material Slice Closeout Pass

Material slice closeout opens no new chess meaning beyond the narrow `Scene.Material` vertical slice.

Scope audit:

- opened: `Scene.Material` only
- still closed: `Scene.Defense`
- still closed: Plan
- still closed: Strategy
- still closed: Conversion
- still closed: Blunder
- still closed: public route `200`
- still closed: production API
- still closed: public/user-facing LLM narration

Authority audit:

- `CaptureResult` owns simple capture and immediate bounded recapture material proof.
- `ExchangeResult` remains unopened and is reserved for bounded multi-move exchange proof outside this slice if needed.
- `StoryProof` owns identity completeness, same-board proof, and legal-line binding.
- `EngineCheck` supports, caps, or refutes only an existing Material Story.
- `StoryTable` orders existing Material rows but creates no Material Story or material proof.
- `Scene.Material` owns the Story label only.
- `material_change` is speech-claim vocabulary; current emitted key is `material_balance_changes`.

Negative corpus audit:

- material-looking false positives produce no Lead or Blocked.
- legal-line-missing Material is silent or Blocked.
- same-board-proof-missing Material is silent or Blocked.
- recapture-cancelled Material is silent or Blocked.
- unclear exchange result is silent or Blocked.
- king target is silent or Blocked.
- zero material result is silent or Blocked.
- engine-refuted Material is Blocked.
- incomplete StoryProof is Blocked.
- incomplete material proof is silent or Blocked.
- tactic writers do not duplicate Material speech.
- Hanging does not automatically duplicate as Material.
- Fork does not automatically duplicate as Material.
- high Proof score alone remains insufficient.

Cleanup and consolidation audit:

- no `ExchangeResult` type was created in this slice.
- no `MaterialEngineCheck` type was created.
- no new markdown authority document was created for Material.
- no new StoryTable creation path was added for Material.
- no direct material proof, `CaptureResult`, `ExchangeResult`, `EngineCheck`,
  `BoardFacts`, raw PV, proofFailures, or source-row input reaches
  ExplanationPlan, Renderer, or LLM smoke.
- Material proof text does not become renderer or LLM input.

Shared skeleton audit:

- Material reuses proof home -> Story writer -> EngineCheck -> StoryTable -> ExplanationPlan -> Renderer -> LLM smoke.
- Reuse the skeleton before adding a new one.
- no second Story writer path, EngineCheck type, StoryTable route, ExplanationPlan input, renderer input, or LLM prompt shape was added.
- if `ExchangeResult` opens in its own slice, it must state how bounded multi-move exchange proof differs from `CaptureResult`.

Home map:

- `CaptureResult`: proof home for simple capture and immediate bounded recapture material result.
- `ExchangeResult`: unopened reserved proof-home name for bounded multi-move exchange proof outside this slice.
- `Scene.Material`: Story label for bounded material-result meaning.
- `material_change`: speech-claim vocabulary; current runtime claim key is `material_balance_changes`.

Next-stage handoff:

- next named slice remains `Scene.Defense`.
- Material does not open Defense, Conversion, Winning, Plan, Strategy, or Blunder.
- Material does not open best move, forced line, no counterplay, decisive,
  conversion, or full engine PV commentary.

`Material is a scene, not a tactic.`
`Material gain is not winning.`
`One chess meaning, one home.`

Completion standard: Material slice is closed as a narrow bounded material-result Story label.

## Defense-0 Scene.Defense Charter

Defense-0 opens only the charter for the first narrow `Scene.Defense` slice.
It does not open a Defense writer, proof sidecar implementation, StoryTable
integration, ExplanationPlan mapping, deterministic renderer, LLM smoke,
public route `200`, production API, king safety, mate defense, strategic
defense, prophylaxis, winning, conversion, best-move, only-move, refutation,
or no-counterplay claim.

Defense requires a threat.

ThreatProof proves what must be stopped.

DefenseProof proves how the move stops it.

Defense is not no-counterplay.

First Defense scope:

- attacked piece exists
- opponent has an immediate material threat
- the threat is same-board legal
- the defended move removes, guards, or saves the target
- material loss is prevented in a bounded way
- no claim of best move or only move

Core distinction:

`ThreatProof = what must be stopped`
`DefenseProof = how it is stopped`
`Scene.Defense = Story label`
`defends_piece / prevents_material_loss = speech claim`

Forbidden Defense-0 claims:

- only move
- best move
- no counterplay
- refutes the attack
- solves the position
- king safety
- mate defense
- strategic defense
- prophylaxis
- winning
- conversion

Completion standard: `Scene.Defense` must not become another name for a good move or for stopping all counterplay.

## Defense-1 ThreatProof

Defense-1 opens only `ThreatProof`.

ThreatProof proves what must be stopped.

ThreatProof does not create a Defense Story.

ThreatProof does not create a public claim.

DefenseProof, the named Scene.Defense writer, StoryTable integration,
ExplanationPlan, renderer, LLM smoke, public route `200`, and production API
remain closed.

Required ThreatProof fields:

- rival side
- threatened target
- attacking piece
- legal threat line
- target value
- material loss if unanswered
- same-board proof
- missing evidence

Allowed Defense-1 meanings:

- rival can capture the target
- target is attacked
- capture would cause material loss
- threat is immediate and legal

Forbidden Defense-1 meanings:

- opponent has an attack
- king is unsafe
- no counterplay
- mate threat
- long-term pressure
- strategic threat
- engine says this is a threat

Completion standard: ThreatProof proves what must be stopped, but it does not create a Defense Story or public claim.

## Defense-2 DefenseProof

Defense-2 opens only `DefenseProof`.

DefenseProof proves how a specific move stops a specific ThreatProof.

DefenseProof does not create a Defense Story.

DefenseProof does not create a public claim.

Allowed Defense-2 move types:

1. target moves away
2. target becomes guarded
3. attacker line is blocked or attacker is captured

Required DefenseProof fields:

- defending side
- defense move
- defended target
- original threat
- after-defense target status
- material loss prevented
- same-board proof
- missing evidence

Allowed Defense-2 meanings:

- the target is no longer capturable for gain
- the target is defended after the move
- the attacker's line is blocked
- the attacker is captured

Forbidden Defense-2 meanings:

- solves the position
- refutes the attack
- stops all threats
- only move
- best defense
- no counterplay
- king safety
- mate defense

Completion standard: DefenseProof proves whether a specific threat is stopped, but it does not create a Defense Story or public claim.

## Defense-3 SceneDefense Writer

Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.

Required SceneDefense writer evidence:

- scene = Defense
- StoryProof complete
- ThreatProof complete
- DefenseProof complete
- same-board proof present
- defense move legal
- protected target identified
- material loss prevented
- writer = SceneDefense
- EngineCheck does not Refute

First allowed Defense-3 meanings:

- this move defends the attacked piece
- this move prevents immediate material loss

Forbidden Defense-3 meanings:

- only move
- best move
- refutes attack
- stops counterplay
- solves position
- king safe
- mate stopped
- winning
- decisive

Completion standard: one narrow proof-backed Defense Story can enter StoryTable.

## Defense-4 Defense Negative Corpus

Defense-4 opens only the Defense negative corpus.

Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof.

Defense-4 negative cases:

- no actual threat
- threat is illegal
- attacked piece is already adequately defended
- defense move does not affect the target
- defense move guards wrong piece
- defense move still loses material
- defense move allows equivalent recapture
- defense only looks like prophylaxis
- defense is actually a tactic / material gain
- king safety claim tries to enter
- mate defense tries to enter
- only-move claim tries to enter
- StoryProof incomplete
- ThreatProof incomplete
- DefenseProof incomplete
- EngineCheck Refutes
- high Proof score only

Completion standard: defense-looking rows have no Lead or are Blocked unless ThreatProof and DefenseProof are complete.

## Defense-5 EngineCheck for Scene.Defense

Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.

Allowed Defense-5 EngineCheck statuses:

- Unknown
- Supports
- Caps
- Refutes

Required Defense-5 EngineCheck evidence:

- Defense Story already exists
- same-board proof
- same Story route
- same legal line
- fresh/depth evidence

Forbidden Defense-5 meanings and shortcuts:

- engine creates Defense Story
- engine eval becomes public truth
- PV becomes explanation
- best move explanation
- only move claim
- refutes attack claim
- DefenseEngineCheck duplicate type

Completion standard: EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense.

## Defense-6 StoryTable Integration

Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.

Allowed Defense-6 roles and behavior:

- Lead
- Support
- Context
- Blocked
- deterministic ordering

Defense-6 StoryTable rules:

- Refuted Defense -> Blocked
- incomplete Defense -> Blocked
- writerless Defense -> Blocked
- Defense without ThreatProof -> Blocked
- Defense without DefenseProof -> Blocked
- Defense can compete for Lead only if it has complete proof
- Support / Context are not sentences

Forbidden Defense-6 shortcuts:

- StoryTable creates Defense
- raw engine eval ranks Defense
- Defense proof text becomes public
- renderer wording affects order
- Defense silently opens only move or no counterplay

Completion standard: StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning.

## Defense-7 ExplanationPlan for Scene.Defense

Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.

Defense-7 allowed input:

- selected Verdict only

Defense-7 forbidden inputs:

- ThreatProof directly
- DefenseProof directly
- EngineCheck
- BoardFacts
- raw PV
- proofFailures
- source row

Defense-7 first allowed claim keys:

- defends_piece
- prevents_material_loss
- protects_target

Defense-7 forbidden claim keys:

- only_move
- best_defense
- refutes_attack
- stops_counterplay
- solves_position
- king_safe
- mate_defense
- no_counterplay

Completion standard: Defense ExplanationPlan creates no meaning stronger than the selected Verdict.

## Defense-8 Deterministic Renderer

Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.

Defense-8 allowed renderer input:

- ExplanationPlan only

Defense-8 first deterministic templates:

- `{route} defends the piece on {target}.`
- `{route} prevents the piece on {target} from being lost immediately.`

Defense-8 forbidden renderer wording:

- only move
- best move
- refutes the attack
- stops all counterplay
- solves the position
- king is safe
- mate is stopped
- winning
- decisive
- forced

Completion standard: Renderer text is no stronger than the Defense ExplanationPlan.

## Defense-9 LLM Smoke

Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.

Defense-9 allowed LLM smoke input:

- ExplanationPlan
- RenderedLine

Defense-9 Codex CLI smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- instruction: Rephrase only. Do not add chess facts.

Defense-9 forbidden LLM smoke inputs:

- raw Verdict
- Story
- ThreatProof
- DefenseProof
- EngineCheck
- BoardFacts
- engine eval
- raw PV
- proofFailures

Defense-9 smoke rejection checks:

- no new move
- no new line
- no new tactic
- no new plan
- no engine mention
- no only move
- no best move
- no no-counterplay
- no king safety
- no mate defense
- no refutes-attack wording
- no stronger claim

Completion standard: LLM smoke does not make Defense text stronger.

## Defense Slice Closeout Pass

Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice.

Defense closeout scope audit:

- only `Scene.Defense` opened
- king safety remains closed
- mate defense remains closed
- Plan remains closed
- Strategy remains closed
- Counterplay remains closed beyond existing EngineCheck Caps
- Prophylaxis remains closed

Defense closeout authority audit:

- ThreatProof owns what must be stopped
- DefenseProof owns how the move stops it
- StoryProof owns same-board Story identity evidence
- EngineCheck supports, caps, or refutes an existing Defense Story only
- StoryTable arbitrates roles without creating Defense

Defense closeout negative corpus audit: defense-looking false positives stay silent without complete ThreatProof and DefenseProof.

Defense closeout shared skeleton audit: charter, proof home, named writer, negative corpus, EngineCheck reuse, StoryTable integration, ExplanationPlan, deterministic renderer, LLM smoke, and closeout reused the existing vertical-slice skeleton.

Defense closeout cleanup audit: `ThreatProof`, `DefenseProof`, `Scene.Defense`, and `defends_piece` each have one home.

Defense closeout real-game smoke: Fischer-Spassky 1972 game 6 after 6...h6, 7.Bh4 is covered as an attacked-piece defense smoke.

Defense closeout next-stage handoff: next candidates remain line-based tactic or king-forcing tactic; Defense does not open king safety, mate defense, or counterplay.

Defense requires a threat.
Defense is not no-counterplay.
Reuse the skeleton before adding a new one.
One chess meaning, one home.

Completion standard: Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only.

## Middlegame Interaction Hardening

### MIH-0 Charter

Middlegame Interaction Hardening opens no chess meaning. It stress-tests already-open meanings.

MIH-0 opens only interaction hardening among existing Hanging, Fork, Material, and Defense rows.

MIH-0 opens complex middlegame fixture based role stability checks.

MIH-0 checks selected Verdict, ExplanationPlan, and renderer/LLM smoke boundary stability without opening new speech.

MIH-0 may apply only the minimum StoryTable ordering fix if an existing ordering bug is exposed.

Allowed MIH-0 rows:

- Tactic.Hanging
- Tactic.Fork
- Scene.Material
- Scene.Defense

MIH-0 hardening matrix:

- Material vs Defense
- Hanging vs Defense
- Fork vs Defense
- Hanging vs Material
- Hanging vs Fork
- EngineCheck Supports / Caps / Refutes across these rows

MIH-0 forbidden openings:

- Line/Ray
- RemoveGuard
- Pin
- Skewer
- Pawn
- BackRank
- Plan
- Strategy
- Initiative
- Pressure
- public route 200
- production API
- public/user-facing LLM narration
- new Story family
- new proof home
- new renderer wording

Material vs Defense is the first and highest-risk case because `Scene.Defense` prevents immediate material loss while `Scene.Material` describes material balance changing now.

Hardening role rules:

- If the move actually wins or changes material now, `Scene.Material` may lead.
- If the move prevents an immediate material loss, `Scene.Defense` may lead.
- If both are present on the same-board route, same-board outcome decides:
- actual material gain/change outranks Defense as Material
- prevented immediate loss without actual material gain/change remains Defense
- speculative material loss remains Blocked

Authority boundary:

- StoryTable must not create Defense, Material, Hanging, or Fork rows during this hardening.
- EngineCheck may support, cap, or refute existing rows only.
- Raw engine eval and renderer wording must not rank rows.
- Support and Context remain relation roles, not sentences.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

### MIH-1 Fixture Map

MIH-1 opens only complex middlegame test fixtures for already-open Hanging, Fork, Material, and Defense rows.

Each MIH-1 fixture must state:

- same-board FEN
- side to move
- legal fixture lines
- expected open rows
- expected blocked rows
- expected Lead / Support / Context / Blocked role
- expected selected Verdict
- forbidden claims

Allowed MIH-1 fixture categories:

- Hanging vs Material
- Hanging vs Fork
- Material vs Defense
- Fork vs Defense
- Material vs Defense on same board
- EngineCheck Supports/Caps/Refutes over existing rows

MIH-1 forbidden fixture shortcuts:

- fixture implies a new Story family
- pressure expectation
- initiative expectation
- best move expectation
- only move expectation
- proofFailures text as expected public output

Completion standard: Fixture Map names board, rows, roles, selected Verdict, and forbidden claims without opening new meaning.

### MIH-2 Role Stability

MIH-2 opens only StoryTable role stability checks over existing Hanging, Fork, Material, and Defense rows.

MIH-2 verifies:

- selected Verdict remains stable across input order changes
- same-board collisions create no duplicate Lead
- incomplete rows cannot Lead
- refuted rows become Blocked
- capped rows create no standalone strong claim

MIH-2 specific checks:

- `Scene.Material` and `Tactic.Hanging` on the same capture route cannot both Lead.
- `Scene.Defense` cannot Lead without an actual ThreatProof.
- `Tactic.Fork` cannot create a Material claim without complete two-target proof.

MIH-2 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Role Stability keeps selected Verdict deterministic, prevents duplicate Lead, blocks incomplete or refuted rows, and keeps capped rows from standalone strong claims without opening new meaning.

### MIH-3 Material vs Defense Collision

MIH-3 opens only the Material vs Defense collision rule over existing `Scene.Material` and `Scene.Defense` rows.

MIH-3 rules:

- actual material balance change now gives `Scene.Material` priority
- `Scene.Defense` may speak only when it prevents immediate material loss
- speculative material loss does not open Defense
- same-board Material and Defense rows must distinguish actual material change now from prevented immediate loss

MIH-3 forbidden upgrades:

- Defense as best defense
- Defense as only move
- Defense as refutes attack
- Material as winning
- Material as conversion
- Material as decisive

MIH-3 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Material vs Defense collision selects actual material change now over prevented immediate loss, blocks speculative material loss, and keeps both public boundaries bounded.

### MIH-4 EngineCheck Interaction

MIH-4 opens only existing EngineCheck interaction checks over already-open Hanging, Fork, Material, and Defense rows.

MIH-4 reuses existing `EngineCheck` only.

MIH-4 verifies:

- `Supports` creates no new claim
- `Caps` weakens or suppresses allowed claim
- `Refutes` blocks the checked Story
- `Unknown` creates no engine-related expression

MIH-4 forbidden shortcuts:

- engine eval ordering
- raw PV explanation
- engine says wording
- best-move wording
- eval numbers in public values

MIH-4 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: EngineCheck Interaction reuses existing EngineCheck statuses, keeps Supports and Unknown non-speaking, suppresses or weakens Caps, blocks Refutes, and prevents engine eval, raw PV, engine-says, best-move, and eval-number public leakage.

### MIH-5 Negative Corpus

MIH-5 opens only close false-positive negative corpus tests over already-open Hanging, Fork, Material, Defense, and EngineCheck rows.

Looks plausible is not enough. Complete proof or silence.

MIH-5 must include:

- attacked-looking piece but adequate recapture exists
- fork-looking move but only one real target
- material-looking capture but equal or lost after immediate reply
- defense-looking move but no complete ThreatProof
- defense move guards wrong target
- defense move still leaves material loss
- engine refutes otherwise plausible Story
- same-board proof missing
- route mismatch
- stale or wrong engine line

MIH-5 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Negative Corpus keeps close false positives silent unless complete proof exists, and no plausible-looking row may reach selected public output through StoryTable, ExplanationPlan, renderer, or LLM smoke boundaries.

### MIH-6 Downstream Boundary Smoke

MIH-6 opens only downstream boundary smoke over selected Verdict, existing ExplanationPlan, existing DeterministicRenderer, and existing LLM smoke.

MIH-6 verifies:

- ExplanationPlan input is selected Verdict only
- Renderer input is ExplanationPlan only
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only
- Support, Context, Blocked, capped, and refuted rows create no standalone public text

MIH-6 forbidden openings:

- new renderer template
- new LLM behavior
- raw Story to renderer or LLM
- raw Proof to renderer or LLM
- raw EngineCheck to renderer or LLM
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Downstream Boundary Smoke passes only selected Lead Verdict data through existing ExplanationPlan, renderer, and LLM smoke boundaries, while non-Lead, capped, and refuted rows stay silent.

### MIH-7 Diagnostics Boundary

MIH-7 opens only diagnostics boundary smoke over already-open Hanging, Fork, Material, Defense, StoryTable, selected Verdict, ExplanationPlan, renderer, and LLM smoke.

MIH-7 verifies:

- proofFailures are internal diagnostic only
- Verdict.values do not include raw proof failure text or engine text
- source row data does not flow directly into ExplanationPlan
- StoryTable debug relation does not become renderer wording

MIH-7 forbidden openings:

- new Story family
- new proof home
- new renderer wording
- new LLM behavior
- raw Story to renderer or LLM
- raw Proof to renderer or LLM
- raw EngineCheck to renderer or LLM
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: Diagnostics Boundary keeps proofFailures, raw proof failure text, engine text, source row data, and StoryTable debug relations out of public meaning, Verdict.values, ExplanationPlan source inputs, renderer wording, and LLM smoke prompts.

### MIH Closeout Hard Cleanup Pass

MIH Closeout opens no chess meaning. It only audits the MIH hardening surface.

MIH Closeout audit checklist:

- no new chess meaning opened
- no new proof home opened
- Hanging, Material, and Defense do not duplicate ownership of the same public meaning
- Story label, proof home, and speech key remain separate
- broad terms remain closed and do not become authority
- detailed MIH rules live in StoryInteractionLaw.md only
- other live docs summarize MIH scope without duplicating rule text
- test helpers remain test-only and do not become runtime authority
- public route 200 remains closed
- production API remains closed
- public/user-facing LLM narration remains closed

MIH Closeout ownership map:

- Tactic.Hanging owns the Story label for the hanging tactic; CaptureResult remains the capture proof home; can_win_piece remains the speech key.
- Scene.Material owns the Story label for current material balance change; CaptureResult remains the simple capture proof home; material_balance_changes remains the first speech key.
- Scene.Defense owns the Story label for preventing immediate material loss; ThreatProof and DefenseProof remain the proof homes; defends_piece remains the first speech key.

Completion standard: MIH closes as interaction hardening only, with no new Story family, no new proof home, no duplicate meaning owner, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no promoted test helper, no public route 200, no production API, and no public/user-facing LLM narration.

## Line / Ray Slice

### Line-0 Charter

Current implementation scope is Line / Ray Slice.

Line-0 opens only the charter for the first narrow line/ray proof slice.

First Line scope: a legal move reveals one slider attack on one non-king material target.

LineFact observes geometry.

LineProof binds the revealed line.

Tactic.DiscoveredAttack may speak only after proof.

Allowed Line-0 opening:

- LineFact observation as existing geometry input
- first narrow LineProof proof slice
- one legal move that moves or removes the blocker from a slider line
- one revealed slider attack
- one non-king material target
- same-board proof

Line-0 forbidden openings:

- broad LineTactic
- Pin
- Skewer
- XRay public Story
- RemoveGuard
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- public route `200`
- production API

Line-0 opens no broad LineTactic, Pin, Skewer, XRay public Story, RemoveGuard, mate threat, king safety, pressure, initiative, best move, forced line, winning, decisive, blunder, public route `200`, or production API.

Completion standard: Line-0 keeps line/ray work at charter scope and opens no public Story, renderer wording, LLM narration, public route `200`, or production API.

### Line-1 LineProof

Line-1 opens only `LineProof` as a narrow proof home.

LineProof proves side, slider piece, blocker or moved piece, revealed target, legal revealing move, line kind, same-board proof, before-move blocked or inactive line, after-move slider attack, and non-king material target.

LineProof must prove:

- side
- slider piece
- blocker or moved piece
- revealed target
- legal revealing move
- line kind: file / rank / diagonal
- same-board proof
- before move: line blocked or not active
- after move: slider attacks target
- target is non-king material piece

LineProof is not a public Story.

LineFact is not a public Story.

LineProof must not directly say pin, pressure, attack works, or wins material.

LineProof proof failure text must not become renderer or LLM input.

Line-1 forbidden openings:

- broad LineTactic
- Tactic.DiscoveredAttack writer
- Pin
- Skewer
- XRay public Story
- RemoveGuard
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: `LineProof` proves only a legal revealed slider attack on one non-king material target, while LineFact, LineProof, proof failures, renderer, and LLM boundaries remain non-speaking.

### Line-2 Tactic.DiscoveredAttack Writer

Line-2 opens only the named `TacticDiscoveredAttack` writer for one narrow `Tactic.DiscoveredAttack` Story.

Tactic.DiscoveredAttack writer conditions:

- complete StoryProof
- complete LineProof
- same-board legal replay
- legal revealing move
- target exists
- after move slider attacks target
- writer = TacticDiscoveredAttack
- EngineCheck does not Refute

Line-2 Story identity:

- tactic = DiscoveredAttack
- side = revealing side
- target = revealed target square
- anchor = moved piece or slider anchor
- route = revealing move
- rival = opposite side

Line-2 opened runtime pieces:

- `Tactic.DiscoveredAttack` tactic identity
- `StoryWriter.TacticDiscoveredAttack`
- `TacticDiscoveredAttack.write`
- `TacticDiscoveredAttack.withEngineCheck`
- StoryTable admission for complete, non-refuted `Tactic.DiscoveredAttack` rows

Line-2 forbidden openings:

- Tactic.Pin
- Tactic.Skewer
- Tactic.XRay
- RemoveGuard
- king target speech
- broad LineTactic
- XRay public Story
- mate threat
- king safety
- pressure
- initiative
- best move
- forced line
- winning
- decisive
- blunder
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Target king remains silent in Line-2.

Completion standard: Tactic.DiscoveredAttack may become a Story only through complete StoryProof plus complete LineProof for one legal revealed slider attack on one non-king material target, while Pin, Skewer, XRay, RemoveGuard, king target speech, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### Line-3 Negative Corpus

Line-3 opens only the negative corpus for the narrow `Tactic.DiscoveredAttack` slice.

Line-3 negative corpus must close:

- legal move is absent
- same-board proof is absent
- line is not actually opened
- target is still not attacked after the move
- slider is not a slider
- target is king
- blocker moved but another piece still blocks
- discovered-looking move has no target
- pressure, initiative, or mate wording tries to enter
- Pin, Skewer, or XRay classification tries to enter

Geometry is not enough. Revealed attack proof or silence.

Line-3 opens no new Story family, proof home, renderer wording, LLM smoke, public route `200`, production API, pressure, initiative, mate threat, Pin, Skewer, XRay public Story, or RemoveGuard.

Completion standard: Line-3 keeps discovered-attack-looking false positives silent unless complete StoryProof and complete LineProof prove one legal revealed slider attack on one non-king material target.

### Line-4 EngineCheck Reuse

Line-4 opens only existing `EngineCheck` reuse for existing `Tactic.DiscoveredAttack` Stories.

Line-4 EngineCheck rules:

- EngineCheck cannot create DiscoveredAttack
- Supports creates no new claim
- Caps suppresses strong expression
- Refutes blocks the Story
- Unknown creates no engine expression

Line-4 forbidden openings:

- raw eval ordering
- raw PV explanation
- engine says
- best move
- winning tactic
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.DiscoveredAttack` Story; it never creates DiscoveredAttack, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Line-5 StoryTable Integration

Line-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, and `Tactic.DiscoveredAttack` rows.

Line-5 verification:

- selected Verdict remains stable when input order changes
- DiscoveredAttack does not own a Material claim
- Hanging and Material on the same target do not both become Lead
- Defense without an actual threat cannot create a claim that blocks DiscoveredAttack
- Fork without two-target proof cannot absorb DiscoveredAttack

Line-5 opens no Material claim for DiscoveredAttack, no Defense claim without ThreatProof, no Fork claim without two-target proof, no renderer wording, no LLM smoke, no public route `200`, and no production API.

Completion standard: StoryTable may arbitrate existing Hanging, Fork, Material, Defense, and DiscoveredAttack rows deterministically, while claim ownership remains with each already-open proof-backed Story family and DiscoveredAttack still has no downstream speech.

### Line-6 ExplanationPlan

Line-6 opens only ExplanationPlan mapping for selected Lead `Tactic.DiscoveredAttack` Verdicts.

Line-6 allowed claim key:

- reveals_attack_on_piece

Line-6 forbidden claim keys:

- wins_material
- pins_piece
- skewers_piece
- creates_pressure
- takes_initiative
- mate_threat
- best_move
- forced
- decisive

Support, Context, Blocked, capped, and refuted DiscoveredAttack rows create no standalone claim.

Line-6 opens no renderer wording, LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: selected uncapped Lead DiscoveredAttack Verdicts may lower only to the internal `reveals_attack_on_piece` claim key with the listed forbidden wording boundary; all non-Lead, capped, and refuted DiscoveredAttack rows remain claimless, and renderer and LLM stages remain closed.

### Line-7 Deterministic Renderer

Line-7 opens only deterministic renderer text for selected `Tactic.DiscoveredAttack` ExplanationPlan.

Renderer input is `ExplanationPlan` only.

Line-7 allowed template:

- `{route} reveals an attack on the piece on {target}.`

Line-7 forbidden renderer wording:

- wins material
- winning
- decisive
- best move
- forces
- pins
- skewers
- puts pressure
- creates a mating threat

Line-7 opens no LLM smoke, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: DeterministicRenderer may phrase only the selected Lead `reveals_attack_on_piece` ExplanationPlan through the bounded template, must reject stronger wording through forbidden wording checks, and must keep Support, Context, Blocked, capped, refuted, and no-claim DiscoveredAttack plans silent.

### Line-8 LLM Smoke

Line-8 opens only LLM smoke for selected DiscoveredAttack ExplanationPlan and RenderedLine.

Line-8 reuses existing 8B prompt smoke only.

Line-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- Rephrase only. Do not add chess facts.

Line-8 forbidden inputs and additions:

- raw Story
- raw LineProof
- LineFact
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- mate
- pressure
- initiative
- winning claim

Line-8 opens no raw Story, raw LineProof, LineFact, BoardFacts, EngineCheck, raw PV, proofFailures, public/user-facing LLM narration, public route `200`, production API, Material claim, Pin, Skewer, XRay public Story, RemoveGuard, pressure, initiative, mate threat, best-move, forced-line, winning, or decisive claim.

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and the instruction `Rephrase only. Do not add chess facts.` for selected DiscoveredAttack RenderedLine; it must reject raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, or winning claims.

### Line Closeout Hard Cleanup

Line Closeout opens no new chess meaning. It only audits the Line / Ray Slice hard cleanup surface.

Line Closeout must confirm:

- LineFact observes geometry only.
- LineProof binds the revealed line only.
- Tactic.DiscoveredAttack owns only the proof-backed Story identity.
- `reveals_attack_on_piece` owns only the bounded speech claim key.
- Broad Line, Ray, XRay, Pin, and Skewer are not live public authority for this slice.
- LineProof does not duplicate StoryProof, CaptureResult, MultiTargetProof, ThreatProof, DefenseProof, or EngineCheck.
- Renderer and LLM smoke cannot create wording stronger than the selected DiscoveredAttack ExplanationPlan.
- Detailed Line authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Line Closeout opens no broad LineTactic, Ray tactic, XRay public Story, Pin, Skewer, RemoveGuard, Material claim, pressure, initiative, mate threat, best-move, forced-line, winning, decisive, blunder, new proof home, new Story family, renderer wording beyond Line-7, LLM input beyond Line-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Line closes as a narrow proof-backed discovered attack slice only, with LineFact, LineProof, Tactic.DiscoveredAttack, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `reveals_attack_on_piece`.

## Line / Defender Contact Neighborhood

### Pin-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood.

Pin-0 opens only the charter for the second narrow line/defender contact vertical slice.

Pin first positive scope is not a broad pin family.

Pin first scope: a legal move creates or reveals a line where one non-king piece is pinned to its king.

LineFact observes geometry.

LineProof binds the line.

PinProof proves the pinned relation.

Tactic.Pin may speak only after proof.

Pin-0 allowed opening:

- narrow `Tactic.Pin`
- king-behind line relation
- one non-king pinned target
- legal move that creates or reveals the pin relation
- bounded pin wording after selected Verdict only

Pin-0 forbidden openings:

- broad LineTactic
- broad AbsPin or RelPin family
- Skewer
- XRay public Story
- RemoveGuard
- DiscoveredAttack expansion
- mate threat
- king safety
- winning material
- decisive tactic
- forced move
- best move
- cannot move wording
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

Pin-0 opens no broad LineTactic, broad AbsPin or RelPin family, Skewer, XRay public Story, RemoveGuard, DiscoveredAttack expansion, mate threat, king safety, winning material, decisive tactic, forced move, best move, cannot move wording, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Pin-0 keeps pin work at charter scope and opens no PinProof runtime, no Tactic.Pin writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### Pin-1 PinProof

Pin-1 opens only `PinProof` as a narrow proof home.

PinProof proves side creating the pin, pinned target, pinning slider, king behind target, legal pinning or revealing move, line kind, same-board proof, before/after relation, target non-king, target and king same side, and slider attacks through target toward king after move.

PinProof must prove:

- side creating the pin
- pinned target
- pinning slider
- king behind target
- legal pinning or revealing move
- line kind: file / rank / diagonal
- same-board proof
- before/after relation
- target is non-king
- target and king are same side
- slider attacks through target toward king after move

PinProof is not a public Story.

LineFact is not a public Story.

LineProof is not a public Story.

PinProof must not say material gain, king unsafe, mate, pressure, or initiative.

PinProof proof failure text must not become renderer or LLM input.

Pin-1 forbidden openings:

- Tactic.Pin writer
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- material gain claim
- king unsafe claim
- mate threat
- pressure
- initiative
- public route `200`
- production API

Completion standard: `PinProof` proves only a legal move creating or revealing one pinned-to-king relation over one non-king target, while LineFact, LineProof, PinProof, proof failures, renderer, and LLM boundaries remain non-speaking.

### Pin-2 Tactic.Pin Writer

Pin-2 opens only the named `TacticPin` writer for one narrow `Tactic.Pin` Story.

TacticPin writer conditions:

- complete StoryProof
- complete PinProof
- same-board legal replay
- legal pinning or revealing move
- pinned target exists
- pinning slider exists
- king-behind-target relation complete
- writer = TacticPin
- EngineCheck does not Refute

Pin-2 Story identity:

- tactic = Pin
- scene = Tactic
- side = pinning side
- rival = pinned side
- target = pinned target square
- anchor = pinning slider square or moved piece square
- route = pinning/revealing move

Pin-2 opened runtime pieces:

- `Tactic.Pin` tactic identity
- `StoryWriter.TacticPin` writer identity
- `TacticPin.write`
- `TacticPin.withEngineCheck`
- StoryTable admission for complete non-refuted `Tactic.Pin` rows

Pin-2 forbidden openings:

- Material claim
- Defense ownership
- RemoveGuard ownership
- king target speech
- broad AbsPin or RelPin family
- Skewer
- XRay public Story
- DiscoveredAttack expansion
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Target king remains silent in Pin-2.

Completion standard: Tactic.Pin may become a Story only through complete StoryProof plus complete PinProof for one legal move creating or revealing one pinned-to-king relation over one non-king target, while Material, Defense, RemoveGuard, king target speech, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### Pin-3 Negative Corpus

Pin-3 opens only the negative corpus for the narrow `Tactic.Pin` slice.

A line to a king is not enough. Complete pinned relation or silence.

Pin-3 required silent counterexamples:

- legal move absent
- same-board proof absent
- slider is not a slider
- no king behind target
- target and king are not same side
- line does not continue through target to king
- target is king
- another blocker is between slider and king
- pin-looking geometry but no post-move relation
- discovered attack only
- skewer-looking position classified as Pin
- mate wording
- king safety wording
- pressure wording

Pin-3 forbidden openings:

- new Pin writer behavior
- broad AbsPin or RelPin family
- Skewer
- DiscoveredAttack expansion
- Material claim
- Defense ownership
- RemoveGuard ownership
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Pin-looking rows stay silent unless complete StoryProof and complete PinProof prove one legal move creates or reveals one pinned-to-king relation over one non-king target.

### Pin-4 EngineCheck Reuse

Pin-4 opens only existing `EngineCheck` reuse for existing `Tactic.Pin` Stories.

EngineCheck must not create Pin.

`Supports` creates no new Pin claim.

`Caps` suppresses allowed claim or weakens expression to bounded strength when downstream speech opens.

`Refutes` blocks the Pin Story.

`Unknown` creates no engine expression.

Pin-4 forbidden openings:

- engine says
- best move
- only move
- winning tactic
- forced win
- raw PV explanation
- eval number public value
- new EngineCheck type
- Pin from engine evidence
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Pin` Story; it never creates Pin, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Pin-5 StoryTable Integration

Pin-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, and `Tactic.Pin` rows.

Pin-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- Pin does not own Material claim
- Pin does not own king safety claim
- DiscoveredAttack and Pin on the same line do not both become Lead
- actual material change now remains owned by Scene.Material
- Defense creates no defense claim without complete ThreatProof and complete DefenseProof

Pin-5 forbidden openings:

- new Pin proof home
- new Story family
- broad AbsPin or RelPin family
- Material claim from Pin
- king safety claim from Pin
- Defense claim from incomplete Defense rows
- duplicate Lead for same-line DiscoveredAttack and Pin
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: StoryTable orders existing open rows with Pin deterministically, keeps one selected Lead, and keeps Material, Defense, DiscoveredAttack, and Pin claim homes separate.

### Pin-6 ExplanationPlan

Pin-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Pin` Verdicts.

Pin-6 ExplanationPlan input is selected uncapped Lead Verdict only.

Pin-6 allowed claim key:

- pins_piece

Pin-6 forbidden claim keys:

- wins_material
- king_unsafe
- mate_threat
- best_move
- only_move
- forced
- decisive
- creates_pressure
- takes_initiative
- cannot_move

Support, Context, Blocked, capped, and refuted Pin rows create no standalone claim.

Pin-6 forbidden openings:

- wins_material claim
- king_unsafe claim
- mate_threat claim
- best_move claim
- only_move claim
- forced claim
- decisive claim
- creates_pressure claim
- takes_initiative claim
- cannot_move wording
- renderer wording
- LLM smoke
- public route `200`
- production API

Completion standard: selected uncapped Lead `Tactic.Pin` Verdicts may lower only to bounded `pins_piece`; all non-Lead, capped, refuted, and unselected Pin rows remain without standalone claim.

### Pin-7 Deterministic Renderer

Pin-7 opens only deterministic renderer text for selected `Tactic.Pin` ExplanationPlan.

Pin-7 renderer input is `ExplanationPlan` only.

Pin-7 allowed renderer template:

- `{route} pins the piece on {target}.`

Pin-7 forbidden renderer wording:

- cannot move
- the king is unsafe
- wins material
- winning
- decisive
- best move
- only move
- forces
- creates pressure
- threatens mate

Pin-7 forbidden openings:

- raw Verdict input
- raw Story input
- PinProof input
- LineFact input
- LineProof input
- EngineCheck input
- proofFailures input
- LLM smoke
- public route `200`
- production API

Completion standard: Renderer phrases only selected bounded `pins_piece` ExplanationPlan data and refuses wording stronger than the Pin-6 claim boundary.

### Pin-8 LLM Smoke

Pin-8 opens only LLM smoke for selected Pin ExplanationPlan and RenderedLine.

Pin-8 reuses only the existing 8B Codex CLI prompt smoke contract.

Pin-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Pin-8 forbidden inputs and additions:

- raw Story
- raw PinProof
- raw LineProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- mate claim
- pressure claim
- initiative claim
- winning claim
- cannot-move claim

Pin-8 forbidden openings:

- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected Pin RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and mate, pressure, initiative, winning, or cannot-move claims.

### Pin Closeout Hard Cleanup

Pin Closeout opens no new chess meaning. It only audits the Pin hard cleanup surface.

Pin Closeout must confirm:

- LineFact observes geometry only.
- LineProof binds line evidence only and does not own Pin speech.
- PinProof proves only the pinned-to-king relation.
- Tactic.Pin owns only the proof-backed Story identity.
- `pins_piece` owns only the bounded speech claim key.
- Pin does not own Material, Defense, DiscoveredAttack, Skewer, or RemoveGuard meaning.
- Broad Line, Ray, XRay, and broad Pin family terms are not live public authority for this slice.
- Renderer and LLM smoke cannot create wording stronger than `pins_piece`.
- Test helpers are not promoted into runtime authority.
- Detailed Pin authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

Pin Closeout opens no broad LineTactic, broad Ray tactic, XRay public Story, broad AbsPin or RelPin family, Skewer, RemoveGuard, Material claim, Defense claim, DiscoveredAttack expansion, pressure, initiative, mate threat, cannot-move wording, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Pin-7, LLM input beyond Pin-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Pin closes as a narrow proof-backed pinned-to-king slice only, with LineFact, LineProof, PinProof, Tactic.Pin, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `pins_piece`.

### RemoveGuard-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood.

RemoveGuard-0 opens only the charter for the third narrow line/defender contact vertical slice.

First RemoveGuard positive scope is not a broad remove-the-guard motif.

RemoveGuard first scope: a legal move removes one defender from one non-king material target.

First runtime positive path stays centered on defender capture when possible.

Deflection is allowed only when exact-board proof immediately after the same move shows the defender no longer guards the target.

BoardFacts observes guard relation.

RemoveGuardProof proves the guard was removed.

Tactic.RemoveGuard may speak only after proof.

RemoveGuard-0 allowed opening:

- narrow `Tactic.RemoveGuard`
- one non-king material target
- one defender
- one legal move that removes the defender guard relation
- bounded remove-guard wording after selected Verdict only

RemoveGuard-0 forbidden openings:

- broad deflection tactic
- overloaded defender theory
- discovered attack expansion
- Pin expansion
- Skewer expansion
- XRay expansion
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- refutes defense
- collapses position
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

RemoveGuard-0 opens no broad deflection tactic, overloaded defender theory, discovered attack expansion, Pin/Skewer/XRay expansion, material win claim, winning, decisive, forced, best move, only move, no defense, refutes defense, collapses position, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: RemoveGuard-0 keeps remove-guard work at charter scope and opens no RemoveGuardProof runtime, no Tactic.RemoveGuard writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### RemoveGuard-1 RemoveGuardProof

RemoveGuard-1 opens only `RemoveGuardProof` as a narrow proof home.

RemoveGuardProof must prove:

- side removing the guard
- rival side
- guarded target
- removed defender
- legal remove-guard move
- target is non-king material piece
- defender guarded target before move
- after move defender no longer guards target
- same-board proof
- exact-board after-move relation

RemoveGuard-1 first allowed removal kind:

- DefenderCaptured

RemoveGuard-1 conditional removal kind:

- GuardLineBlocked, only when one legal move blocks a slider defender guard line and exact-board proof shows the defender no longer guards the target

RemoveGuard-1 closed removal kinds:

- opponent-reply deflection
- sacrifice lure
- overloaded defender
- remove guard by long tactic sequence
- defender cannot defend general theory

RemoveGuardProof is not a public Story.

RemoveGuardProof owns no material result.

RemoveGuardProof proof failure text must not become renderer or LLM input.

RemoveGuard-1 forbidden openings:

- Tactic.RemoveGuard writer
- StoryTable integration
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- refutes defense
- pressure
- initiative
- public route `200`
- production API

Completion standard: `RemoveGuardProof` proves only that one legal same-board move removes one defender guard relation from one non-king material target, while RemoveGuardProof, proof failures, renderer, LLM, public route `200`, and production API remain non-speaking.

### RemoveGuard-2 Tactic.RemoveGuard Writer

RemoveGuard-2 opens only the named `TacticRemoveGuard` writer for one narrow `Tactic.RemoveGuard` Story.

TacticRemoveGuard writer conditions:

- complete StoryProof
- complete RemoveGuardProof
- same-board legal replay
- legal remove-guard move
- guarded target exists
- removed defender existed and guarded target before move
- defender no longer guards target after move
- writer = TacticRemoveGuard
- EngineCheck does not Refute

RemoveGuard-2 Story identity:

- tactic = RemoveGuard
- scene = Tactic
- side = guard-removing side
- rival = target/defender side
- target = guarded target square
- anchor = removed defender square or moving piece square
- route = remove-guard move

RemoveGuard-2 opened runtime pieces:

- `Tactic.RemoveGuard` tactic identity
- `StoryWriter.TacticRemoveGuard` writer identity
- `TacticRemoveGuard.write`
- `Story.removeGuardProof`
- StoryTable admission for complete non-refuted `Tactic.RemoveGuard` rows

RemoveGuard-2 forbidden openings:

- Scene.Material claim
- Tactic.Hanging replacement
- Defense refutation wording
- material win claim
- winning
- decisive
- forced
- best move
- only move
- no defense
- pressure
- initiative
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: `Tactic.RemoveGuard` may become a Story only through complete StoryProof plus complete RemoveGuardProof for one legal move removing one defender guard relation from one non-king material target, while Material, Hanging, Defense-refutation wording, ExplanationPlan, renderer, LLM, public route `200`, and production API remain closed.

### RemoveGuard-3 Negative Corpus

RemoveGuard-3 opens only the negative corpus for the narrow `Tactic.RemoveGuard` slice.

RemoveGuard-3 required silent counterexamples:

- legal move missing
- same-board proof missing
- target is king
- defender did not guard target
- defender still guards target after move
- another defender remains and broad claim is attempted
- direct material gain claim
- Pin misclassified as RemoveGuard
- DiscoveredAttack misclassified as RemoveGuard
- Skewer misclassified as RemoveGuard
- opponent-reply deflection
- overloaded defender claim
- no defense wording
- wins material wording
- best move wording

Removing one guard is not winning material. Complete guard-removal proof or silence.

RemoveGuard-3 forbidden openings:

- new proof home
- new writer
- StoryTable ordering change
- Scene.Material claim
- Tactic.Hanging replacement
- Defense refutation wording
- Pin ownership
- DiscoveredAttack ownership
- Skewer ownership
- overloaded defender theory
- broad deflection tactic
- material win claim
- no defense
- wins material
- best move
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: RemoveGuard-3 adds only close false-positive corpus coverage; no plausible-looking row may speak unless complete StoryProof plus complete RemoveGuardProof proves one legal same-board move removes one defender guard relation from one non-king material target.

### RemoveGuard-4 EngineCheck Reuse

RemoveGuard-4 opens only existing `EngineCheck` reuse for existing `Tactic.RemoveGuard` Stories.

RemoveGuard-4 EngineCheck rules:

- EngineCheck cannot create RemoveGuard
- Supports creates no new claim
- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens
- Refutes blocks the RemoveGuard Story
- Unknown creates no engine expression

RemoveGuard-4 forbidden openings:

- engine says
- best move
- only move
- winning tactic
- raw PV explanation
- eval number public value
- new EngineCheck type
- RemoveGuard from engine evidence
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.RemoveGuard` Story; it never creates RemoveGuard, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### RemoveGuard-5 StoryTable Integration

RemoveGuard-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, and `Tactic.RemoveGuard` rows.

RemoveGuard-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- RemoveGuard does not own Material claim
- RemoveGuard does not replace Hanging claim
- Defense creates no response claim without complete ThreatProof and complete DefenseProof
- Pin and RemoveGuard on the same defender or line do not both become Lead
- actual material change now remains owned by Scene.Material

RemoveGuard-5 forbidden openings:

- new RemoveGuard proof home
- new Story family
- Material claim from RemoveGuard
- Hanging claim from RemoveGuard
- Defense response from incomplete Defense rows
- duplicate Lead for same-line Pin and RemoveGuard
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: StoryTable orders existing open rows with RemoveGuard deterministically, keeps one selected Lead, and keeps Material, Hanging, Defense, Pin, DiscoveredAttack, Fork, and RemoveGuard claim homes separate.

### RemoveGuard-6 ExplanationPlan

RemoveGuard-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.RemoveGuard` Verdicts.

RemoveGuard-6 ExplanationPlan input is selected uncapped Lead Verdict only.

RemoveGuard-6 allowed claim key:

- removes_defender

RemoveGuard-6 forbidden claim keys:

- wins_material
- target_is_hanging
- no_defense
- refutes_defense
- best_move
- only_move
- forced
- decisive
- creates_pressure
- takes_initiative

Support, Context, Blocked, capped, and refuted RemoveGuard rows create no standalone claim.

RemoveGuard-6 forbidden openings:

- wins_material claim
- target_is_hanging claim
- no_defense claim
- refutes_defense claim
- best_move claim
- only_move claim
- forced claim
- decisive claim
- creates_pressure claim
- takes_initiative claim
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: selected uncapped Lead `Tactic.RemoveGuard` Verdicts may lower only to bounded `removes_defender`; all non-Lead, capped, refuted, and unselected RemoveGuard rows remain without standalone claim.

### RemoveGuard-7 Deterministic Renderer

RemoveGuard-7 opens only deterministic renderer text for selected `Tactic.RemoveGuard` ExplanationPlan.

RemoveGuard-7 renderer input is `ExplanationPlan` only.

RemoveGuard-7 allowed renderer template:

- `{route} removes the defender of the piece on {target}.`

RemoveGuard-7 forbidden renderer wording:

- wins material
- leaves it undefended
- no defender remains
- best move
- only move
- forces
- decisive
- refutes the defense
- creates pressure

RemoveGuard-7 forbidden openings:

- raw Verdict input
- raw Story input
- RemoveGuardProof input
- BoardFacts input
- EngineCheck input
- proofFailures input
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Renderer phrases only selected bounded `removes_defender` ExplanationPlan data and refuses wording stronger than the RemoveGuard-6 claim boundary.

### RemoveGuard-8 LLM Smoke

RemoveGuard-8 opens only LLM smoke for selected RemoveGuard ExplanationPlan and RenderedLine.

RemoveGuard-8 reuses only the existing 8B Codex CLI prompt smoke contract.

RemoveGuard-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

RemoveGuard-8 forbidden inputs and additions:

- raw Story
- raw RemoveGuardProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- material win claim
- no-defense claim
- pressure claim
- initiative claim

RemoveGuard-8 forbidden openings:

- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: LLM smoke may receive only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.` for selected RemoveGuard RenderedLine; it rejects raw proof/board/engine inputs, new moves, new lines, and material-win, no-defense, pressure, or initiative claims.

### RemoveGuard Closeout Hard Cleanup

RemoveGuard Closeout opens no new chess meaning. It only audits the RemoveGuard hard cleanup surface.

RemoveGuard Closeout must confirm:

- BoardFacts guard relation observes only same-side guard contact.
- RemoveGuardProof proves only that one defender guard relation was removed from one non-king material target.
- Tactic.RemoveGuard owns only the proof-backed Story identity.
- `removes_defender` owns only the bounded speech claim key.
- RemoveGuard does not own Material, Hanging, Defense, Pin, or DiscoveredAttack meaning.
- Deflection, overload, no-defender, and wins-material terms are not live public authority for this slice.
- Renderer and LLM smoke cannot create wording stronger than `removes_defender`.
- Test helpers are not promoted into runtime authority.
- Detailed RemoveGuard authority lives only in `StoryInteractionLaw.md`; AGENTS.md, README.md, ChessCommentarySSOT.md, ChessModelArchitecture.md, and ChessModelContract.md may summarize only.
- Public route `200`, production API, and public/user-facing LLM narration remain closed.

RemoveGuard Closeout opens no broad deflection tactic, overloaded defender theory, no-defender claim, wins-material claim, Material claim, Hanging claim, Defense claim, Pin expansion, DiscoveredAttack expansion, Skewer, XRay, pressure, initiative, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond RemoveGuard-7, LLM input beyond RemoveGuard-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: RemoveGuard closes as a narrow proof-backed guard-removal slice only, with BoardFacts guard relation, RemoveGuardProof, Tactic.RemoveGuard, StoryTable, ExplanationPlan, Renderer, and LLM smoke keeping separate authority and no downstream layer speaking beyond selected proof-backed `removes_defender`.

## Line / Defender Interaction Hardening

Line/Defender hardening opens no new Story. It proves that existing line and defender Stories do not steal each other's meaning.

### LDH-0 Charter

LDH-0 opens only existing Line/Defender rows interaction hardening.

LDH-0 opens only complex same-board fixture checks.

LDH-0 opens only StoryTable role stability checks.

LDH-0 opens only downstream no-overclaim smoke.

LDH-0 may apply only the minimum StoryTable ordering fix if an existing DiscoveredAttack ordering bug is exposed.

Allowed LDH-0 line and defender rows:

- Tactic.DiscoveredAttack
- Tactic.Pin
- Tactic.RemoveGuard

Allowed LDH-0 collision targets:

- Tactic.Hanging
- Tactic.Fork
- Scene.Material
- Scene.Defense

LDH-0 forbidden openings:

- Tactic.Skewer
- Tactic.XRay
- broad LineTactic
- broad deflection
- overloaded defender
- pressure
- initiative
- mate threat
- king safety
- material win claim
- public route 200
- production API
- public/user-facing LLM narration

Completion standard: LDH-0 hardens only existing row interaction, keeps exactly one selected Lead, keeps non-Lead line/defender rows from standalone downstream claims, and keeps renderer/LLM smoke no stronger than the selected ExplanationPlan.

### LDH-1 Fixture Map

LDH-1 opens only complex same-board Fixture Map coverage.

Each LDH-1 fixture must state:

- same-board FEN
- side to move
- candidate_passer legal lines
- expected open rows
- expected blocked rows
- expected Lead / Support / Context / Blocked role
- expected selected Verdict
- forbidden claims

Required LDH-1 fixture categories:

- DiscoveredAttack vs Pin
- DiscoveredAttack vs RemoveGuard
- Pin vs RemoveGuard
- DiscoveredAttack + Pin + RemoveGuard same-board
- Line/Defender row vs Material
- Line/Defender row vs Hanging
- Line/Defender row vs Defense
- EngineCheck Supports/Caps/Refutes over existing Line/Defender rows

LDH-1 fixture map forbids:

- expecting a Skewer-looking fixture as positive Skewer
- using `wins material`, `best move`, `pressure`, or `initiative` as expected output
- using proofFailures text as public expected output

LDH-1 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-1 records fixture-map coverage only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-2 Role Stability

LDH-2 opens only StoryTable role stability checks over existing Line/Defender rows.

LDH-2 role stability must verify:

- input order changes must keep the same selected Verdict
- same meaning must not become duplicate Lead
- incomplete rows must not become Lead
- refuted rows must become Blocked
- capped rows must create no standalone claim

LDH-2 must specifically check:

- Pin and DiscoveredAttack on the same line must not both become Lead
- RemoveGuard must not own Pin line relation
- DiscoveredAttack must not own RemoveGuard defender relation

LDH-2 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-2 hardens StoryTable role stability only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-3 Meaning Ownership Boundary

LDH-3 opens only Meaning Ownership Boundary checks over existing Line/Defender and collision rows.

LDH-3 owned meanings:

- Tactic.DiscoveredAttack owns only a legal move reveals one slider attack on one material target.
- Tactic.Pin owns only a non-king piece is pinned to its own king on a line.
- Tactic.RemoveGuard owns only one defender no longer guards one target after a legal move.
- Scene.Material owns only actual material balance change now.
- Tactic.Hanging owns only a capturable target with bounded material gain proof.
- Scene.Defense owns only complete ThreatProof plus DefenseProof prevents immediate material loss.

LDH-3 forbidden ownership leaks:

- RemoveGuard must not say material gain.
- Pin must not say cannot-move or king unsafe.
- DiscoveredAttack must not say wins-material.
- Material must not own line tactic identity.
- Defense must not say it stopped the threat without complete threat proof.

LDH-3 opens no Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-3 hardens meaning ownership boundaries only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-4 EngineCheck Interaction

LDH-4 opens only existing EngineCheck interaction checks over existing Line/Defender rows.

LDH-4 must verify:

- Supports must not create a new claim.
- Caps must suppress allowed claim or keep downstream speech bounded.
- Refutes must make the checked Story Blocked.
- Unknown must create no engine-related expression.

LDH-4 forbidden public engine wording:

- engine says
- raw PV explanation
- eval number public value
- best move
- only move
- forced line

LDH-4 opens no new EngineCheck proof home, new Story family, engine-says wording, raw PV explanation, eval number public value, best move, only move, forced line, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-4 hardens existing EngineCheck status interaction only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-5 Negative Corpus

LDH-5 opens only close false-positive negative corpus tests over existing Line/Defender rows and already-open collision rows.

LDH-5 close false positives must stay silent:

- line opens but no actual attack
- attack appears but target is king
- pin-looking line but no king behind target
- remove-guard-looking move but defender still guards target
- defender removed but Material or Hanging proof is incomplete
- discovered attack and pin both plausible but one proof is incomplete
- wrong-board or stale same-board proof
- route mismatch
- engine refutes plausible row
- Skewer-looking relation tries to enter before Skewer opens

LDH-5 rule: Looks like a line tactic is not enough. Existing complete proof or silence.

LDH-5 opens no new Story family, proof home, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-5 hardens close false-positive silence only; it does not add a Story family, proof home, renderer phrase, public route, production API, or public/user-facing LLM narration.

### LDH-6 Downstream Boundary Smoke

LDH-6 opens only downstream boundary smoke over selected Lead Verdicts from existing Line/Defender rows.

LDH-6 must verify:

- ExplanationPlan input is selected Verdict only.
- Renderer input is ExplanationPlan only.
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and the rephrase-only instruction only.
- Support, Context, Blocked, capped, and refuted rows create no standalone text.

LDH-6 forbidden downstream inputs or changes:

- no new renderer template
- no new LLM behavior
- no raw Story, Proof, or EngineCheck reaches renderer or LLM smoke

LDH-6 opens no new Story family, proof home, renderer template, LLM behavior, raw Story, Proof, or EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-6 hardens downstream selected Lead handoff only; it does not add a Story family, proof home, renderer template, LLM behavior, public route, production API, or public/user-facing LLM narration.

### LDH-7 Diagnostics Boundary

LDH-7 opens only diagnostics boundary smoke over existing Line/Defender rows, StoryTable, selected Verdict, ExplanationPlan, renderer, LLM smoke, and test-helper authority.

LDH-7 must verify:

- proofFailures are internal diagnostic only.
- raw proof text does not enter Verdict.values.
- EngineCheck text does not flow directly into ExplanationPlan.
- StoryTable debug relation does not become renderer wording.
- test helpers do not become runtime authority.

LDH-7 opens no new Story family, proof home, renderer wording, LLM behavior, runtime authority helper, raw Story, raw Proof, raw EngineCheck downstream path, Skewer, XRay, broad LineTactic, broad deflection, overloaded defender, pressure, initiative, mate threat, king safety, material win claim, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: LDH-7 keeps proofFailures, raw proof text, EngineCheck text, StoryTable debug relations, and test helpers out of public meaning, Verdict.values, ExplanationPlan direct inputs, renderer wording, LLM smoke prompts, and runtime authority.

### LDH Closeout Hard Cleanup Pass

LDH Closeout opens no chess meaning. It only audits the Line / Defender Interaction Hardening surface.

LDH Closeout audit checklist:

- no new Story family opened.
- no new proof home opened.
- LineFact, LineProof, PinProof, and RemoveGuardProof authority stay separated.
- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not steal each other's meaning.
- Tactic.DiscoveredAttack, Tactic.Pin, and Tactic.RemoveGuard do not invade Scene.Material, Tactic.Hanging, or Scene.Defense claim homes.
- broad Line, Ray, XRay, Skewer, deflection, overload, pressure, and initiative do not become live authority.
- detailed LDH interaction rules live in StoryInteractionLaw.md only.
- other live docs summarize LDH scope without duplicating rule text.
- public route 200 remains closed.
- production API remains closed.
- public/user-facing LLM narration remains closed.

LDH Closeout ownership map:

- BoardFacts LineFact observes geometry only; it is not a Story or proof home.
- LineProof belongs only to Tactic.DiscoveredAttack for this hardening surface.
- PinProof belongs only to Tactic.Pin.
- RemoveGuardProof belongs only to Tactic.RemoveGuard.
- Scene.Material keeps actual material balance change now.
- Tactic.Hanging keeps capturable target with bounded material gain proof.
- Scene.Defense keeps complete ThreatProof plus DefenseProof preventing immediate material loss.

Completion standard: LDH closes as interaction hardening only, with no new Story family, no new proof home, no mixed LineFact, LineProof, PinProof, or RemoveGuardProof authority, no Line/Defender meaning theft, no Material, Hanging, or Defense claim-home invasion, no broad-term authority, no duplicated live rule authority outside StoryInteractionLaw.md, no public route 200, no production API, and no public/user-facing LLM narration.

## Skewer Slice

### Skewer-0 Charter

Current implementation scope is Line / Defender Contact Neighborhood late vertical slice.

Skewer-0 opens only the charter for the fourth narrow line/defender contact vertical slice.

First Skewer positive scope is not a broad skewer tactic.

Skewer first scope: a legal move creates or reveals a slider attack on one front non-king material target, with a second non-king material target behind it on the same line.

LineFact observes geometry.

SkewerProof proves the front-and-back target relation.

Tactic.Skewer may speak only after proof.

Skewer-0 allowed opening:

- narrow `Tactic.Skewer`
- one slider
- one front target
- one rear target
- front/rear target same-line relation
- legal move that creates or reveals the front/rear relation
- bounded skewer wording after selected Verdict only

Skewer-0 forbidden openings:

- broad LineTactic
- XRay public Story
- Pin expansion
- RemoveGuard expansion
- material win claim
- front piece must move
- wins rear piece
- forced line
- best move
- only move
- winning
- decisive
- king safety
- mate threat
- pressure
- initiative
- public route `200`
- production API
- public/user-facing LLM narration

Skewer-0 opens only narrow Tactic.Skewer, one slider, one front target, one rear target, front/rear target same-line relation, a legal move that creates or reveals the front/rear relation, and bounded skewer wording after selected Verdict only.

Skewer-0 opens no broad LineTactic, XRay public Story, Pin expansion, RemoveGuard expansion, material win claim, front piece must move, wins rear piece, forced line, best move, only move, winning, decisive, king safety, mate threat, pressure, initiative, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Skewer-0 keeps skewer work at charter scope and opens no SkewerProof runtime, no Tactic.Skewer writer, no StoryTable integration, no ExplanationPlan mapping, no renderer wording, no LLM smoke, no public route `200`, and no production API.

### Skewer-1 SkewerProof

Skewer-1 opens only `SkewerProof` as a narrow proof home.

SkewerProof proves side creating the skewer, rival side, skewer slider, front target, rear target, legal skewer or revealing move, line kind, same-board proof, front target non-king material piece, rear target non-king material piece, front and rear target same rival side, after move slider attacks front target, rear target behind front target on the same ray, no extra blocker breaks the front-to-rear relation, and before move the skewer relation was absent or blocked.

SkewerProof is not a public Story.

LineFact and LineProof are not public Stories.

SkewerProof says no material gain, front piece must move, or wins rear piece.

SkewerProof proofFailures stay out of renderer/LLM input.

Skewer-1 opens no Tactic.Skewer writer, StoryTable integration, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-2 TacticSkewer Writer

Skewer-2 opens only the named `TacticSkewer` writer for one narrow `Tactic.Skewer` Story.

TacticSkewer requires complete StoryProof, complete SkewerProof, same-board legal replay, legal skewer or revealing move, front target, rear target, slider, complete front-and-back line relation, writer identity, and no EngineCheck Refutes status.

Skewer Story identity is tactic Skewer, scene Tactic, skewer-creating side, front/rear target side as rival, front target square, rear target square as secondaryTarget, slider or moved-piece anchor, and skewer/revealing route.

TacticSkewer creates no Scene.Material claim.

TacticSkewer does not replace Tactic.Pin.

Skewer-2 keeps rear-target king positions silent.

Skewer-2 opens no StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, material gain claim, front piece must move wording, wins rear piece claim, Pin replacement, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-3 Negative Corpus

Skewer-3 opens only the negative corpus for the narrow `Tactic.Skewer` slice.

Skewer-3 keeps illegal moves, missing same-board proof, non-sliders, missing front target, missing rear target, front or rear king targets, front/rear targets not on the same rival side, rear targets not behind the front target on the same line, extra blockers between front and rear target, DiscoveredAttack-only lines, Pin-looking positions, front-piece-must-move assumptions, and material-win, forced, or best-move wording silent.

Skewer-3 rule: Two pieces on a line is not enough. Complete front-and-back skewer proof or silence.

Skewer-3 opens no new proof home, new writer, StoryTable Lead admission, ExplanationPlan mapping, renderer wording, LLM smoke, Scene.Material claim, Pin replacement, front piece must move wording, wins rear piece claim, public route `200`, production API, or public/user-facing LLM narration.

### Skewer-4 EngineCheck Reuse

Skewer-4 opens only existing `EngineCheck` reuse for existing `Tactic.Skewer` Stories.

Skewer-4 EngineCheck rules:

- EngineCheck cannot create Skewer
- Supports creates no new claim
- Caps suppresses standalone claim or weakens expression to bounded strength when downstream speech opens
- Refutes blocks the Skewer Story
- Unknown creates no engine expression

Skewer-4 forbidden openings:

- engine says
- best move
- only move
- forced win
- winning tactic
- raw PV explanation
- eval number public value
- new EngineCheck type
- Skewer from engine evidence
- StoryTable Lead admission
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Existing EngineCheck may only support, cap, or refute an already proof-backed `Tactic.Skewer` Story; it never creates Skewer, never ranks by raw eval or raw PV, and never adds engine wording or stronger tactic wording.

### Skewer-5 StoryTable Integration

Skewer-5 opens only StoryTable integration for existing `Tactic.Hanging`, `Tactic.Fork`, `Scene.Material`, `Scene.Defense`, `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` rows.

Skewer-5 StoryTable checks:

- selected Verdict remains stable when input order changes
- Skewer does not own Material claim
- Skewer does not turn DiscoveredAttack into a duplicate Lead
- Skewer does not own Pin king relation
- Skewer does not own RemoveGuard defender relation
- actual material change now remains owned by Scene.Material
- incomplete front/rear relation leaves DiscoveredAttack or another existing row and keeps Skewer silent

Skewer-5 forbidden openings:

- new Skewer proof home
- new Story family
- broad LineTactic
- broad XRay
- Material claim from Skewer
- DiscoveredAttack duplicate Lead from Skewer
- Pin king relation from Skewer
- RemoveGuard defender relation from Skewer
- ExplanationPlan mapping
- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: StoryTable orders existing open rows with Skewer deterministically, keeps one selected Lead, and keeps Material, DiscoveredAttack, Pin, RemoveGuard, Defense, Fork, Hanging, and Skewer claim homes separate.

### Skewer-6 ExplanationPlan

Skewer-6 opens only ExplanationPlan mapping for selected uncapped Lead `Tactic.Skewer` Verdicts.

Skewer-6 ExplanationPlan input is selected uncapped Lead Verdict only.

Skewer-6 allowed claim key:

- `skewers_piece_to_piece`

Skewer-6 forbidden claim keys:

- `wins_material`
- `wins_rear_piece`
- `front_piece_must_move`
- `best_move`
- `only_move`
- `forced`
- `decisive`
- `king_unsafe`
- `mate_threat`
- `creates_pressure`
- `takes_initiative`

Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim.

Skewer-6 forbidden openings:

- renderer wording
- LLM smoke
- public route `200`
- production API
- public/user-facing LLM narration

Completion standard: Support, Context, Blocked, capped, and refuted Skewer rows create no standalone claim; selected uncapped Lead Skewer rows may lower only the bounded `skewers_piece_to_piece` claim key.

### Skewer-7 Deterministic Renderer

Skewer-7 opens only deterministic renderer text for selected `Tactic.Skewer` ExplanationPlan.

Skewer-7 renderer input is ExplanationPlan only.

Skewer-7 may render `{route} skewers the piece on {target} to the piece on {secondaryTarget}.`

Skewer-7 forbidden wording:

- wins material
- wins the piece behind it
- the front piece must move
- best move
- only move
- forces
- decisive
- king is unsafe
- threatens mate
- creates pressure

Skewer-7 opens no raw Verdict, raw Story, SkewerProof, LineFact, LineProof, BoardFacts, EngineCheck, proofFailures, LLM smoke, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: DeterministicRenderer may speak only from selected Skewer ExplanationPlan and only with bounded `skewers_piece_to_piece` wording.

### Skewer-8 LLM Smoke

Skewer-8 opens only LLM smoke for selected Skewer ExplanationPlan and RenderedLine.

Skewer-8 reuses only the existing 8B Codex CLI prompt smoke contract with renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Skewer-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Skewer-8 forbidden openings:

- raw Story
- raw SkewerProof
- raw LineProof
- BoardFacts
- EngineCheck
- raw PV
- proofFailures
- new move
- new line
- material win
- forced claim
- pressure claim
- initiative claim
- mate claim
- public/user-facing LLM narration
- public route `200`
- production API
- raw proof repair
- engine explanation

Completion standard: Skewer LLM smoke accepts only rephrases no stronger than renderedText and rejects raw proof, engine, new-move, new-line, material-win, forced, pressure, initiative, and mate additions.

### Skewer Closeout Hard Cleanup

Skewer Closeout opens no new chess meaning. It only audits the Skewer hard cleanup surface.

Skewer Closeout must confirm:

- LineFact, LineProof, SkewerProof, Tactic.Skewer, and the speech key do not invade each other's authority.
- Skewer owns no Material, Hanging, Pin, DiscoveredAttack, RemoveGuard, or Defense meaning.
- `front piece must move`, `wins rear piece`, `wins material`, and `forced skewer` are not live authority.
- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.
- renderer/LLM wording stays no stronger than `skewers_piece_to_piece`.
- test helpers do not become runtime authority.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

Skewer Closeout duplicate checks:

- meaning duplication: no same chess meaning appears under two Story labels or two proof homes.
- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, and renderer do not jointly own the same decision.
- terminology duplication: LineTactic, RayTactic, XRay, SkewerFamily, or equivalent broad names do not become live authority.
- document duplication: detailed rules repeat only here; other live docs summarize.

Skewer Closeout opens no broad LineTactic, broad Skewer family, XRay public Story, Material claim, Hanging claim, Pin expansion, DiscoveredAttack expansion, RemoveGuard expansion, Defense claim, front-piece-must-move wording, wins-rear-piece claim, wins-material claim, forced-skewer wording, pressure, initiative, mate threat, king safety, best-move, only-move, forced-line, winning, decisive, new proof home, new Story family, renderer wording beyond Skewer-7, LLM input beyond Skewer-8, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Skewer closes as a narrow proof-backed front-and-rear target slice, with no sibling meaning ownership, no broad-term authority, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, and no public route, production API, or public/user-facing LLM narration.

## Line / Defender Neighborhood Closeout

Line/Defender closes as four narrow proof-backed slices. It opens no broad LineTactic, XRay, pressure, initiative, material-win tactic, or public surface.

### LNC-0 Closeout Charter

LNC-0 opens only Line / Defender neighborhood closeout.

LNC-0 closing targets:

- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

LNC-0 related proof homes:

- `LineProof`
- `PinProof`
- `RemoveGuardProof`
- `SkewerProof`

LNC-0 existing collision targets:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`

LNC-0 allowed audit work:

- scope audit
- duplication audit
- authority audit
- docs simplification
- downstream no-overclaim audit
- next-neighborhood handoff

LNC-0 must confirm:

- DiscoveredAttack owns only one revealed slider attack on one non-king material target.
- Pin owns only one non-king piece pinned to its own king on a line.
- RemoveGuard owns only one removed defender guard relation from one non-king material target.
- Skewer owns only one front non-king material target and one rear non-king material target on the same line.
- LineFact observes geometry and owns no public Story.
- LineProof, PinProof, RemoveGuardProof, and SkewerProof are proof homes, not public Stories.
- Hanging, Fork, Material, and Defense keep their existing claim homes.
- ExplanationPlan, renderer, and LLM smoke stay downstream of selected Verdict data.
- detailed docs authority lives only in StoryInteractionLaw.md; other live docs summarize it.
- public route `200`, production API, and public/user-facing LLM narration remain closed.

LNC-0 duplicate checks:

- meaning duplication: no Line / Defender chess meaning appears under two Story labels or two proof homes.
- proof duplication: no proof home proves a sibling Story's distinct public claim.
- authority duplication: BoardFacts, proof home, Story writer, StoryTable, ExplanationPlan, renderer, and LLM smoke do not jointly own the same decision.
- terminology duplication: LineTactic, Ray, XRay, broad deflection, overload, pressure, initiative, and material-win tactic names do not become live authority.
- document duplication: detailed closeout rules repeat only here; other live docs summarize.

LNC-0 opens no new Story family, new proof home, new Story writer, new renderer template, new LLM behavior, XRay, broad LineTactic, broad Ray, broad deflection, overload, pressure, initiative, material win by line tactic, forced response, public route `200`, production API, or public/user-facing LLM narration.

Completion standard: Line / Defender Contact Neighborhood closes as four narrow proof-backed slices, with no sibling meaning ownership, no broad-term authority, no collision-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no new downstream wording or LLM behavior, no public route `200`, no production API, and no public/user-facing LLM narration.

### LNC-1 Scope Audit

LNC-1 opens only Line / Defender Contact Neighborhood scope audit.

LNC-1 confirms the neighborhood has exactly four opened positive Story labels:

- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

LNC-1 first-scope audit:

- `Tactic.DiscoveredAttack` speaks only one revealed slider attack on one non-king material target.
- `Tactic.Pin` speaks only one non-king piece pinned to its own king on a line.
- `Tactic.RemoveGuard` speaks only one removed defender guard relation from one non-king material target.
- `Tactic.Skewer` speaks only one front non-king material target and one rear non-king material target on the same line.

LNC-1 closed runtime positives:

- XRay is not an opened positive Story in this neighborhood.
- broad Ray is not an opened positive Story in this neighborhood.
- broad LineTactic is not an opened positive Story in this neighborhood.
- broad deflection is not an opened positive Story in this neighborhood.
- overload is not an opened positive Story in this neighborhood.

Closed names are not backlog inside this neighborhood. They remain closed until a separate charter opens them.

LNC-1 opens no new Story family, proof home, Story writer, renderer wording, LLM behavior, XRay, broad Ray, broad LineTactic, broad deflection, overload, pressure, initiative, material-win tactic, public route `200`, production API, or public/user-facing LLM narration.

### LNC-2 Duplication Audit

One chess meaning, one proof home, one Story label, one speech key, one live authority document.

LNC-2 opens only Line / Defender Contact Neighborhood duplication audit.

LNC-2 duplication audit checks:

- no same chess meaning is duplicated under two Story labels.
- no same proof responsibility is duplicated under two proof homes.
- no same speech claim is split across two claim keys.
- no same detailed rule repeats across multiple live documents.

LNC-2 specific ownership checks:

- DiscoveredAttack and Skewer do not duplicate ownership of `line attack`; DiscoveredAttack owns revealed slider attack on one non-king material target, while Skewer owns front-and-rear non-king material targets on one line.
- Pin and Skewer do not duplicate ownership of `front/rear line relation`; Pin owns pinned-to-own-king relation, while Skewer owns front target plus rear target relation.
- RemoveGuard does not grow into Material or Hanging precondition ownership; RemoveGuard owns only removed defender guard relation, Material owns actual material balance change, and Hanging owns capturable target with bounded material gain proof.
- LineProof does not absorb PinProof, RemoveGuardProof, or SkewerProof family-specific relations; LineProof binds only the revealed slider attack line admitted by DiscoveredAttack.
- XRay, Ray, LineTactic, and LineFamily terms are not live authority names for this neighborhood.

LNC-2 speech-key audit:

- `reveals_attack_on_piece` belongs only to `Tactic.DiscoveredAttack`.
- `pins_piece` belongs only to `Tactic.Pin`.
- `removes_defender` belongs only to `Tactic.RemoveGuard`.
- `skewers_piece_to_piece` belongs only to `Tactic.Skewer`.

LNC-2 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, XRay, Ray, LineTactic, LineFamily, broad deflection, overload, Material or Hanging precondition path, public route `200`, production API, or public/user-facing LLM narration.

### LNC-3 Authority Audit

LNC-3 opens only Line / Defender Contact Neighborhood authority audit.

LNC-3 layer authority:

- `BoardFacts.LineFact`: geometry observation only.
- `LineProof`: revealed line / attack binding only.
- `PinProof`: pinned relation only.
- `RemoveGuardProof`: guard relation removal only.
- `SkewerProof`: front/rear target relation only.
- Story writers: named proof-backed Story permission only.
- StoryTable: ordering only.
- Verdict: selected result only.
- ExplanationPlan: bounded speech claim only.
- Renderer: phrasing only.
- LLM smoke: rephrase only.

LNC-3 forbidden authority shortcuts:

- proof home must not speak like a Story label.
- Story writer must not own material result.
- StoryTable must not create new chess meaning.
- ExplanationPlan must not read raw proof.
- Renderer and LLM smoke must not repair or upgrade proof.

LNC-3 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, raw proof downstream path, material-result ownership by Story writer, StoryTable-created meaning, public route `200`, production API, or public/user-facing LLM narration.

### LNC-4 Collision Audit

LNC-4 opens only Line / Defender Contact Neighborhood collision audit.

LNC-4 collision targets:

- DiscoveredAttack vs Pin.
- DiscoveredAttack vs Skewer.
- Pin vs Skewer.
- RemoveGuard vs Material.
- RemoveGuard vs Hanging.
- Skewer vs Material.
- Line/Defender row vs Defense.
- EngineCheck Caps/Refutes over each line/defender row.

LNC-4 verification criteria:

- input order stable.
- no duplicate Lead.
- incomplete row is not Lead.
- capped and refuted rows create no standalone text.
- actual material change now stays in Scene.Material home.
- material gain proof stays in Hanging or Material home.
- line/defender rows speak only their own relation.

LNC-4 existing runtime coverage:

- `Pin-5 StoryTable prevents duplicate Lead for same-line DiscoveredAttack and Pin` covers DiscoveredAttack vs Pin.
- `Skewer-5 separates DiscoveredAttack collision and keeps incomplete Skewer silent` covers DiscoveredAttack vs Skewer.
- `Skewer-5 keeps Material Pin and RemoveGuard claim homes separate` covers Pin vs Skewer and Skewer vs Material.
- `LDH-1 fixture map covers complex same-board line defender interactions` covers RemoveGuard vs Material, RemoveGuard vs Hanging, and Line/Defender row vs Defense.
- `LDH-1 fixture map covers EngineCheck statuses over existing line defender rows` covers EngineCheck Caps/Refutes over DiscoveredAttack, Pin, RemoveGuard, and Skewer.

LNC-4 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, material-win tactic, public route `200`, production API, or public/user-facing LLM narration.

### LNC-5 Downstream Boundary Audit

LNC-5 opens only Line / Defender Contact Neighborhood downstream boundary audit.

LNC-5 downstream authority:

- selected uncapped Lead Verdict only may lower to ExplanationPlan.
- ExplanationPlan input is selected Verdict only.
- Renderer input is ExplanationPlan only.
- LLM smoke input is renderedText, claimKey, strength, forbidden wording, and rephrase-only instruction only.
- Support, Context, Blocked, capped, and refuted rows create no standalone text.

LNC-5 forbidden downstream wording:

- wins material.
- winning / decisive.
- best move / only move.
- forced.
- cannot move.
- no defense.
- front piece must move.
- wins rear piece.
- pressure / initiative.
- mate threat / king unsafe.

LNC-5 existing runtime coverage:

- `LNC-5 Downstream Boundary Audit keeps Line Defender speech bounded` covers DiscoveredAttack, Pin, RemoveGuard, and Skewer downstream handoff and forbidden wording.
- `LDH-6 Downstream Boundary Smoke sends only selected Lead Verdicts to text stages` covers existing Line/Defender selected Lead handoff.
- `Skewer-6 ExplanationPlan gives no standalone claim to non Lead capped refuted or unselected Skewer rows` covers Skewer non-Lead silence.

LNC-5 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC-6 Documentation Simplification

LNC-6 opens only Line / Defender Contact Neighborhood documentation simplification.

LNC-6 documentation authority:

- detailed closeout authority lives only in `StoryInteractionLaw.md`.
- README, SSOT, Architecture, Contract, AGENTS, and LegacyPruneManifest carry summaries only.
- the same closeout rule must not repeat across live documents.
- closed terms must read as closed summaries, not backlog.
- Line family, Ray family, XRay, and line tactic wording must not read as live authority.

LNC-6 verification:

- docs authority tests enforce summary-only downstream documents.
- detailed closeout checklist appears in exactly one live document.
- live authority lists stay aligned across AGENTS.md, README, LegacyPruneManifest, and docs tests.

LNC-6 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC-7 Test Helper / Runtime Boundary Audit

LNC-7 opens only Line / Defender Contact Neighborhood test helper / runtime boundary audit.

LNC-7 runtime boundary:

- test helpers must not become runtime authority.
- fixture names must not read like new Story families.
- negative corpus helpers must not become production concepts.
- forbidden wording checks must reject public wording without treating snake_case internal field names as public prose.
- runtime source must not contain closeout-only terms.

LNC-7 verification:

- docs authority tests pin this section in StoryInteractionLaw only.
- runtime boundary tests scan production source for closeout-only terminology.
- LLM smoke tests keep forbidden wording matching on public phrases, not internal field keys.

LNC-7 opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

### LNC Closeout Final Completion Standard

LNC Closeout final completion standard:

- no new Story family.
- no new proof home.
- no new renderer wording.
- no new LLM behavior.
- only `Tactic.DiscoveredAttack`, `Tactic.Pin`, `Tactic.RemoveGuard`, and `Tactic.Skewer` remain as the positive closeout baseline slices.
- no duplicate meaning.
- no duplicate authority.
- no duplicate terminology.
- no duplicate detailed docs.
- public route, production API, and public/user-facing LLM narration remain closed.

LNC final verification:

- docs authority tests pass.
- chess foundation tests pass.
- `git diff --check` is clean.

Final LNC closeout opens no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, fixture-derived runtime authority, negative-corpus production concept, broad LineTactic, XRay, public route `200`, production API, or public/user-facing LLM narration.

## Pawn / Promotion Neighborhood

### PawnAdvance-0 Charter

PawnAdvance-0 opens only the first narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad PawnTactic or passed-pawn strategy.

Opened positive Story label:

- `Scene.PawnAdvance`

Opened proof home:

- `PawnAdvanceProof`

Core sentence:

Pawn facts observe structure. PawnAdvanceProof binds this legal advance. Scene.PawnAdvance may speak only bounded pawn progress, not conversion.

PawnAdvance-0 positive scope:

- an already-passed non-king pawn.
- legal one-square non-capturing pawn advance.
- non-promotion advance.
- exact after-board still has the same pawn as a passed pawn.
- complete StoryProof and same-board legal replay.
- selected Lead Verdict may lower to bounded pawn-advance wording only.

Allowed bounded claim:

- `advances_passed_pawn`

Allowed deterministic wording:

- `{route} advances the passed pawn.`

PawnAdvance-0 must stay silent for:

- pawn that was not already passed before the move.
- illegal pawn move.
- capturing pawn move.
- promotion move.
- double pawn push.
- missing same-board proof.
- missing StoryProof.
- after-board where the moved pawn is not a passed pawn.

PawnAdvance-0 opens no broad PawnTactic, `Tactic.PawnPush`, `Tactic.PawnFork`,
passed-pawn strategy, promotion threat, unstoppable pawn, winning endgame,
conversion, pawn race, pawn majority plan, king route, opposition, tablebase
claim, best move, only move, forced move, pressure, initiative, public route
`200`, production API, or public/user-facing LLM narration.

Authority split:

- `BoardFacts.PassedPawnObservation` observes current pawn structure only.
- `PawnAdvanceProof` proves the legal non-capturing non-promotion one-step advance and after-board passed-pawn persistence.
- `ScenePawnAdvance` is the only writer that may create `Scene.PawnAdvance`.
- `StoryTable` orders an existing `Scene.PawnAdvance`; it does not create it.
- `ExplanationPlan` may expose only `advances_passed_pawn` from selected Verdict data.
- Renderer phrases only from ExplanationPlan.
- LLM smoke may rephrase only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Duplication audit:

- Passed-pawn structure stays in Board Facts.
- Legal advance persistence stays in `PawnAdvanceProof`.
- Public claim identity stays in `Scene.PawnAdvance`.
- Speech key stays `advances_passed_pawn`.
- Promotion, conversion, race, strategy, and tablebase claims remain closed.

Completion standard: PawnAdvance-0 closes as one narrow proof-backed passed-pawn progress slice with no broad pawn tactic, promotion-route, strategy, conversion, public route, production API, or public/user-facing LLM narration opening.

### PawnAdvance-1 PawnAdvanceProof

`PawnAdvanceProof` is the proof home for narrow `Scene.PawnAdvance`.

PawnAdvanceProof must prove:

- advancing side
- pawn identity
- from square
- to square
- legal pawn advance
- move is non-capture
- move is non-promotion
- pawn was passed before move
- pawn remains passed after move
- same-board proof
- exact after-board replay

`PawnAdvanceProof` is not a public `Story`.

`PassedPawnObservation` is not a public `Story`.

PawnAdvanceProof may emit internal missing evidence, but those diagnostics do
not create a Story, rank a Story, repair a Story, or feed speech.
proofFailures must not become renderer input or LLM input.

PawnAdvance-1 forbidden public wording:

- `unstoppable`
- `wins`
- `queens`
- `promotes next`
- `conversion`
- `clear path`
- `cannot be stopped`

PawnAdvanceProof owns only the proof that this exact legal move advanced an
already-passed pawn and that the moved pawn remains passed on the exact
after-board. It does not own promotion threat, queening, conversion,
unstoppable-pawn, strategy, tablebase, race, best-move, only-move, forced, or
winning claims.

Completion standard: PawnAdvance-1 closes when PawnAdvanceProof exposes the
named proof bits, remains diagnostic below public Story identity, treats
PassedPawnObservation as observation only, and keeps proofFailures and stronger
pawn-conversion wording out of renderer and LLM inputs.

### PawnAdvance-2 Scene.PawnAdvance Writer

Named writer: `ScenePawnAdvance`.

ScenePawnAdvance writer conditions:

- complete `StoryProof`
- complete `PawnAdvanceProof`
- same-board legal replay
- legal non-capturing non-promotion pawn advance
- passed-before and passed-after proof
- writer = `ScenePawnAdvance`
- `EngineCheck` does not `Refute`

ScenePawnAdvance Story identity:

- scene = `PawnAdvance`
- tactic = `None`
- plan = `None`
- side = advancing side
- rival = opposite side
- target = destination square
- anchor = pawn origin square
- route = pawn advance

ScenePawnAdvance must not create a PromotionThreat.

ScenePawnAdvance must not create winning, conversion, or material claims.

ScenePawnAdvance must not create pawn-race or unstoppable-pawn claims.

ScenePawnAdvance owns only the proof-backed public identity that this move
advances an already-passed pawn. It does not own queening, promotion threat,
passed-pawn strategy, race, conversion, material gain, best-move, only-move,
forced, tablebase, winning, or unstoppable-pawn meaning.

Completion standard: PawnAdvance-2 closes when `ScenePawnAdvance` is the only
named writer for `Scene.PawnAdvance`, writer output carries only the named
identity fields above, refuted rows cannot lead, and no stronger pawn,
promotion, material, race, conversion, or winning claim can be created by the
writer.

### PawnAdvance-3 Negative Corpus

PawnAdvance-3 keeps pawn-looking moves silent unless the narrow proof home is
complete.

Required silent negative corpus:

- legal move is absent
- same-board proof is absent
- moving piece is not a pawn
- move is a capture
- move is a promotion
- pawn was not passed before the move
- moved pawn is not passed after the move
- en passant complexity enters the proof attempt
- proof attempt expands into immediate promotion threat
- `unstoppable`, `winning`, or `conversion` wording enters

A pawn move is not a passed-pawn Story. Complete PawnAdvanceProof or silence.

En passant is capture complexity for this slice. A pawn move near promotion may
still only be considered through the bounded `advances_passed_pawn` claim when
`PawnAdvanceProof` is complete; it must not become promotion threat, promotes
next, queening, conversion, unstoppable, winning, or race wording.

Completion standard: PawnAdvance-3 closes when every listed false positive is
silent as a Story without complete `PawnAdvanceProof`, and downstream wording
rejects immediate-promotion, unstoppable, winning, and conversion expansions.

### PawnAdvance-4 EngineCheck Reuse

PawnAdvance-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PawnAdvance`.

EngineCheck status meaning for PawnAdvance:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `advances_passed_pawn` speech or weakens it to relation-only bounded evidence.
- `Refutes` blocks the PawnAdvance Story.
- `Unknown` creates no engine expression.

Forbidden EngineCheck-derived wording:

- `engine says`
- eval numbers
- best move
- only move
- winning endgame
- tablebase-like claim

Completion standard: PawnAdvance-4 closes when `EngineCheck` can attach only to
an existing same-board `ScenePawnAdvance` Story, cannot create PawnAdvance by
itself, preserves bounded passed-pawn advance identity under `Supports`,
suppresses standalone speech under `Caps`, blocks the Story under `Refutes`,
and leaves no engine expression under `Unknown`.

### PawnAdvance-5 StoryTable Integration

PawnAdvance-5 integrates `Scene.PawnAdvance` into `StoryTable` as a lower
bounded scene claim when existing opened claim homes are present.

Collision rows:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`
- `Scene.PawnAdvance`

StoryTable requirements:

- input order must not change selected role shape
- PawnAdvance must not own tactical or material claims
- actual material change now stays in `Scene.Material`
- immediate tactic meaning stays in the tactic row home
- PawnAdvance remains a lower bounded scene claim only
- capped or refuted PawnAdvance has no standalone text

Completion standard: PawnAdvance-5 closes when `StoryTable` remains stable
across input order, existing immediate tactic, material, and defense homes stay
ahead of `Scene.PawnAdvance`, and non-lead, capped, or refuted PawnAdvance rows
cannot produce renderer or LLM standalone speech.

### PawnAdvance-6 ExplanationPlan

PawnAdvance-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnAdvance`.

`ExplanationPlan` may admit only the `advances_passed_pawn` claim key.

Allowed claim key:

- `advances_passed_pawn`

Forbidden claim keys:

- `promotion_threat`
- `unstoppable_pawn`
- `wins_endgame`
- `converts_advantage`
- `best_move`
- `only_move`
- `forced`
- `decisive`
- `creates_pressure`
- `takes_initiative`

Support, Context, Blocked, capped, and refuted PawnAdvance rows have no standalone claim.

Completion standard: PawnAdvance-6 closes when only selected uncapped Lead PawnAdvance Verdicts
lower to `advances_passed_pawn`, every forbidden claim key remains unavailable,
and Support, Context, Blocked, capped, or refuted rows stay silent as standalone claims.

### PawnAdvance-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed deterministic wording:

- `{route} advances the passed pawn.`

Forbidden renderer wording:

- `cannot be stopped`
- `will promote`
- `wins`
- `winning endgame`
- `converts`
- `best move`
- `only move`
- `forces`
- `decisive`
- `creates pressure`

Renderer must not read `Story`, `Verdict`, `PawnAdvanceProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.

Completion standard: PawnAdvance-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`,
phrases selected PawnAdvance plans only as bounded passed-pawn advance text,
and rejects promotion, unstoppable, winning, conversion, best-move, only-move,
forced, decisive, or pressure wording.

### PawnAdvance-8 LLM Smoke

PawnAdvance-8 reuses only the existing 8B LLM smoke boundary.

Allowed LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

Forbidden LLM smoke input or output expansion:

- raw `Story`
- raw `PawnAdvanceProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- new move
- new line
- promotion claim
- unstoppable claim
- winning claim
- conversion claim

LLM smoke must not receive proof homes, BoardFacts, EngineCheck, raw PV, or diagnostics.
It may only rephrase the rendered line without adding chess facts.

Completion standard: PawnAdvance-8 closes when LLM smoke receives only rendered text contract fields,
rejects raw proof, board, engine, or diagnostic leaks,
and rejects new moves, new lines, promotion, unstoppable, winning, or conversion claims.

### PawnAdvance Closeout Hard Cleanup

PawnAdvance closes as one narrow bounded scene only.

Authority ownership:

- `PassedPawnObservation` observes same-board pawn structure only.
- `PawnAdvanceProof` binds the legal non-capturing non-promotion advance and exact after-board passed-pawn status.
- `Scene.PawnAdvance` is the only Story label for this bounded pawn-progress meaning.
- `advances_passed_pawn` is the only speech key for this meaning.

PawnAdvance owns no PromotionThreat, Promotion, PawnStop, PawnBreak, Material, Hanging,
Defense, DiscoveredAttack, Pin, RemoveGuard, Skewer, broad Line/Defender tactic,
passed-pawn strategy, unstoppable-pawn, conversion, clear-path, pawn-race, best-move,
only-move, forced, decisive, pressure, initiative, or winning-endgame meaning.

Duplication audit:

- one chess meaning: bounded passed-pawn advance only
- one proof home: `PawnAdvanceProof`
- one Story label: `Scene.PawnAdvance`
- one speech key: `advances_passed_pawn`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnAdvance only.
They must not carry detailed PawnAdvance stage law, negative corpus, renderer law,
LLM law, or closeout completion authority.

Renderer and LLM wording must remain no stronger than `advances_passed_pawn`.
They may say only bounded pawn progress and must reject promotion, unstoppable,
winning, conversion, clear-path, strategy, pressure, initiative, best-move,
only-move, forced, decisive, raw proof, raw BoardFacts, EngineCheck, raw PV,
and proofFailures wording.

Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnAdvance Closeout closes when `PassedPawnObservation`,
`PawnAdvanceProof`, `Scene.PawnAdvance`, and `advances_passed_pawn` stay in separate
authority layers; PawnAdvance owns no promotion, stopping, breaking, material,
tactical, strategic, conversion, or public-surface meaning; detailed authority remains
only in `StoryInteractionLaw.md`; renderer and LLM smoke remain bounded to
`advances_passed_pawn`.

### PawnStop-0 Charter

PawnStop-0 opens only the second narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad pawn defense or endgame hold.

Opened positive Story label:

- `Scene.PawnStop`

Opened proof home:

- `PawnStopProof`

Core sentence:

Pawn facts observe structure. PawnStopProof proves the next square is stopped. Scene.PawnStop may speak only bounded immediate stop, not endgame defense.

PawnStop-0 positive scope:

- an already-passed target pawn.
- the target pawn's next advance square.
- a legal move directly stops that next advance square on the exact after-board.
- the target pawn remains present on the exact after-board.
- the next advance square is non-promotion.
- complete StoryProof and same-board legal replay.
- selected Lead Verdict may lower to bounded pawn-stop wording only.

Allowed bounded claim:

- `stops_pawn_advance`

Allowed deterministic wording:

- `{route} stops the passed pawn from advancing next.`

PawnStop-0 must stay silent for:

- pawn that was not already passed before the move.
- move that does not stop the target pawn's next advance square.
- promotion stop.
- permanent stop.
- tablebase draw.
- best defense or only move.
- winning or losing endgame.
- conversion stopped.
- pawn race.
- king route or opposition.
- broad pawn strategy.
- missing same-board proof.
- missing StoryProof.

PawnStop-0 opens no promotion stop, permanent stop, tablebase draw, best
defense, only move, winning/losing endgame, conversion stopped, pawn race, king
route, opposition, broad pawn strategy, public route `200`, production API, or
public/user-facing LLM narration.

Authority split:

- `BoardFacts.PassedPawnObservation` observes current pawn structure only.
- `PawnStopProof` proves the legal move directly stops the already-passed pawn's next advance square on the exact after-board.
- `ScenePawnStop` is the only writer that may create `Scene.PawnStop`.
- `StoryTable` orders an existing `Scene.PawnStop`; it does not create it.
- `ExplanationPlan` may expose only `stops_pawn_advance` from selected Verdict data.
- Renderer phrases only from ExplanationPlan.
- LLM smoke may rephrase only renderedText, claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess facts.`

Duplication audit:

- Passed-pawn structure stays in Board Facts.
- Next-square stop proof stays in `PawnStopProof`.
- Public claim identity stays in `Scene.PawnStop`.
- Speech key stays `stops_pawn_advance`.
- Promotion stop, permanent stop, endgame defense, race, strategy, king-route, opposition, and tablebase claims remain closed.

Completion standard: PawnStop-0 closes as one narrow proof-backed passed-pawn
next-square stop slice with no broad pawn defense, promotion-stop, permanent-stop,
endgame-hold, tablebase, strategy, conversion, public route, production API, or
public/user-facing LLM narration opening.

### PawnStop-1 PawnStopProof

`PawnStopProof` is the proof home for narrow `Scene.PawnStop`.

PawnStopProof must prove:

- stopping side
- passed pawn side
- passed pawn identity
- pawn current square
- pawn next advance square
- legal stopping move
- exact after-board replay
- pawn was passed before move
- stopping move directly occupies, attacks, or controls the next advance square
- same-board proof

First allowed stop kinds:

- `NextSquareOccupied`
- `NextSquareAttacked`
- `NextSquareControlledByPawn`

Closed stop kinds:

- long-term blockade
- king opposition
- tablebase draw
- promotion race stop
- tactic sequence stop
- `cannot ever advance` claim

`PawnStopProof` is not a public `Story`.

`PassedPawnObservation` is not a public `Story`.

PawnStopProof may emit internal missing evidence, but those diagnostics do
not create a Story, rank a Story, repair a Story, or feed speech.
proofFailures must not become renderer input or LLM input.

PawnStopProof owns only the proof that this exact legal move immediately stops
an already-passed pawn's next advance square by one of the admitted stop kinds
on the exact after-board. It does not own promotion stop, permanent blockade,
king opposition, tablebase draw, pawn-race, tactic-sequence, cannot-ever-advance,
best-defense, only-move, forced, winning, losing, conversion, or endgame-hold
claims.

Completion standard: PawnStop-1 closes when PawnStopProof exposes the named
proof bits, admits only `NextSquareOccupied`, `NextSquareAttacked`, and
`NextSquareControlledByPawn`, remains diagnostic below public Story identity,
treats `PassedPawnObservation` as observation only, and keeps proofFailures and
stronger pawn-defense wording out of renderer and LLM inputs.

### PawnStop-2 Scene.PawnStop Writer

`ScenePawnStop` is the named writer for `Scene.PawnStop`.

Writer conditions:

- complete StoryProof
- complete PawnStopProof
- same-board legal replay
- legal stopping move
- passed pawn exists
- next square stop relation complete
- writer = `ScenePawnStop`
- EngineCheck does not `Refute`

Story identity:

- `scene = PawnStop`
- `tactic = None`
- `plan = None`
- `side = stopping side`
- `rival = passed pawn side`
- `target = pawn next advance square`
- `anchor = stopping move origin square`
- `route = stopping move`

ScenePawnStop must not own `Scene.Defense` meaning.

ScenePawnStop must not say it stopped `PromotionThreat`.

ScenePawnStop must not create endgame-result or tablebase claims.

Completion standard: PawnStop-2 closes when only `ScenePawnStop` can write the
bounded `Scene.PawnStop` identity above from complete StoryProof and
PawnStopProof, EngineCheck refutation prevents writer-backed public speech, and
defense, promotion-threat, endgame-result, and tablebase meanings remain closed.

### PawnStop-3 Negative Corpus

PawnStop-3 keeps pawn-stop-looking moves silent unless the narrow proof home is
complete.

Required silent negative corpus:

- legal move is absent
- same-board proof is absent
- target pawn is not a passed pawn
- target pawn next advance square cannot be calculated
- after the stopping move, the next square remains empty and safely advanceable
- stop expands into promotion-threat stop
- king opposition, tablebase, or draw claim enters
- `permanently stopped`, `cannot advance`, or `only move` wording enters

Stopping the next square is not stopping the pawn forever.

Complete PawnStopProof or silence.

PawnStop-3 opens no promotion-threat stop, permanent stop, cannot-advance
claim, only-move claim, king opposition, tablebase draw, endgame result,
best-defense, pawn-race, conversion, public route `200`, production API, or
public/user-facing LLM narration.

Completion standard: PawnStop-3 closes when every listed false positive is
silent at the writer/proof boundary or rejected by downstream forbidden wording,
and no incomplete PawnStopProof can create `Scene.PawnStop` speech.

### PawnStop-4 EngineCheck Reuse

PawnStop-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PawnStop`.

EngineCheck status handling:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `stops_pawn_advance` speech or weakens it to bounded relation-only evidence.
- `Refutes` blocks the PawnStop Story.
- `Unknown` creates no engine expression.

PawnStop-4 forbidden engine wording:

- `engine says`
- eval numbers
- best defense
- only move
- tablebase draw
- winning or losing endgame

Completion standard: PawnStop-4 closes when `EngineCheck` can attach only to
an existing same-board `ScenePawnStop` Story route, cannot create PawnStop,
cannot add a new claim under Supports, suppresses or bounds capped rows, blocks
refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval
numbers, best-defense, only-move, tablebase, or endgame-result claims.

### PawnStop-5 StoryTable Integration

PawnStop-5 integrates `Scene.PawnStop` into `StoryTable` as a lower
bounded scene claim when existing opened claim homes are present.

Collision rows:

- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`
- `Scene.PawnAdvance`
- `Scene.PawnStop`

StoryTable requirements:

- input order must not change selected role shape
- PawnStop must not own `Scene.Defense` claims
- PawnStop must not create `PromotionThreat` claims
- PawnAdvance and PawnStop over the same pawn must not both compete as Lead
- immediate tactic meaning stays in the tactic row home
- actual material change stays in `Scene.Material`
- PawnStop remains a lower bounded scene claim only
- capped or refuted PawnStop has no standalone text

Completion standard: PawnStop-5 closes when `StoryTable` remains stable across input order, existing immediate tactic, material, defense, and same-pawn PawnAdvance homes stay ahead of `Scene.PawnStop`, and non-lead, capped, or refuted PawnStop rows cannot produce renderer or LLM standalone speech.

### PawnStop-6 ExplanationPlan

PawnStop-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PawnStop`.

`ExplanationPlan` may admit only the `stops_pawn_advance` claim key.

Allowed claim key:

- `stops_pawn_advance`

Forbidden claim keys:

- `stops_promotion`
- `permanently_stops_pawn`
- `draws_endgame`
- `best_defense`
- `only_move`
- `tablebase_draw`
- `wins_endgame`
- `converts_advantage`
- `forced`

Support, Context, Blocked, capped, and refuted PawnStop rows have no standalone claim.

Completion standard: PawnStop-6 closes when only selected uncapped Lead PawnStop Verdicts
lower to `stops_pawn_advance`, every forbidden claim key remains unavailable,
and Support, Context, Blocked, capped, or refuted rows stay silent as standalone claims.

### PawnStop-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

Allowed deterministic wording:

- `{route} stops the passed pawn from advancing next.`

Forbidden renderer wording:

- `stops promotion`
- `stops the pawn for good`
- `draws`
- `holds the endgame`
- `best defense`
- `only move`
- `forces`
- `wins`
- `tablebase`

Renderer must not read `Story`, `Verdict`, `PawnStopProof`, `EngineCheck`, `proofFailures`, or `BoardFacts`.

Completion standard: PawnStop-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`,
phrases selected PawnStop plans only as bounded next-advance stop text,
rejects every forbidden renderer wording above, and opens no raw Story,
raw Verdict, PawnStopProof, EngineCheck, proofFailures, BoardFacts, public route
`200`, production API, or public/user-facing LLM narration.

### PawnStop-8 LLM Smoke

PawnStop-8 reuses only the existing 8B LLM smoke boundary.

PawnStop-8 LLM smoke input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PawnStop-8 forbidden LLM smoke inputs:

- raw `Story`
- raw `PawnStopProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`

PawnStop-8 LLM smoke must reject output that adds:

- new move
- new line
- promotion claim
- permanent stop claim
- draw claim
- tablebase claim
- winning claim

Completion standard: PawnStop-8 closes when LLM smoke receives only rendered text contract fields,
returns only bounded rephrasing of selected uncapped PawnStop rendered text,
rejects raw Story, raw PawnStopProof, BoardFacts, EngineCheck, raw PV, proofFailures,
new move, new line, promotion, permanent stop, draw, tablebase, and winning claims,
and opens no new LLM channel, production API, public route `200`, or chess-fact creation path.

### PawnStop Closeout Hard Cleanup

PawnStop Closeout opens no new chess meaning. It only audits the PawnStop hard cleanup surface.

PawnStop Closeout must confirm:

- `PassedPawnObservation` observes same-board passed-pawn structure only.
- `PawnStopProof` proves the legal move directly stops the already-passed pawn's next non-promotion advance square on the exact after-board.
- `Scene.PawnStop` is the only Story label for this bounded immediate next-square stop meaning.
- `stops_pawn_advance` is the only speech key for this meaning.

PawnStop owns no `Scene.PawnAdvance`, PromotionThreat, Promotion, or PawnBreak meaning.
PawnStop owns no `Scene.Defense`, `Scene.Material`, `Tactic.Hanging`, or Line / Defender tactic meaning.
`permanent stop`, `draw`, `tablebase`, `best defense`, and `only move` remain forbidden wording only, not live authority.

PawnStop Closeout duplicate checks:

- one chess meaning: bounded immediate passed-pawn next-square stop only
- one proof home: `PawnStopProof`
- one Story label: `Scene.PawnStop`
- one speech key: `stops_pawn_advance`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PawnStop only.
Renderer and LLM wording must remain no stronger than `stops_pawn_advance`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PawnStop Closeout closes when `PassedPawnObservation`,
`PawnStopProof`, `Scene.PawnStop`, and `stops_pawn_advance` remain separate authority homes,
PawnStop owns no PawnAdvance, PromotionThreat, Promotion, PawnBreak, Defense, Material,
Hanging, or Line / Defender tactic meaning, closed endgame and defense wording remains forbidden only,
renderer and LLM smoke stay bounded to `stops_pawn_advance`, detailed authority stays only in
`StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing LLM
narration remain closed.

### PIH-0 Pawn Interaction Hardening Charter

Pawn Interaction Hardening opens no new pawn meaning. It proves PawnAdvance
and PawnStop do not steal promotion, conversion, defense, or tactic meaning.

PIH-0 opens only:

- existing `Scene.PawnAdvance` and `Scene.PawnStop` interaction hardening
- complex same-board fixtures over already-opened rows
- `StoryTable` role stability
- downstream no-overclaim smoke
- minimum fixes for discovered ordering or boundary bugs

PIH-0 collision rows are only:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- `Tactic.Hanging`
- `Tactic.Fork`
- `Scene.Material`
- `Scene.Defense`
- `Tactic.DiscoveredAttack`
- `Tactic.Pin`
- `Tactic.RemoveGuard`
- `Tactic.Skewer`

PIH-0 opens no `Scene.PromotionThreat`, `Scene.Promotion`, `Scene.PawnBreak`,
broad PawnTactic, unstoppable pawn, conversion, winning endgame, tablebase,
pawn race, king route, opposition, best move, only move, public route `200`,
production API, or public/user-facing LLM narration.

Completion standard: PIH-0 closes when existing PawnAdvance and PawnStop rows
remain lower bounded scene claims under opened tactic, material, and defense
homes; same-board role shape is input-order stable; PawnAdvance and PawnStop
proof sidecars cannot contaminate each other; renderer and LLM smoke reject
promotion, conversion, defense, tactic, engine, best-move, only-move, and
public-surface overclaims; and no new pawn Story label, proof home, or speech
key appears.

### PIH-1 Fixture Map

PIH-1 opens only the fixture map for complex same-board Pawn Interaction
Hardening over already-open rows. It does not open PromotionThreat, Promotion,
PawnBreak, broad PawnTactic, unstoppable pawn, conversion, winning endgame,
tablebase, pawn race, king route, opposition, best move, only move, public
route `200`, production API, or public/user-facing LLM narration.

Every PIH-1 fixture must state:

- same-board FEN
- side to move
- legal lines under test
- expected open rows
- expected blocked rows
- expected Lead, Support, Context, or Blocked role for each row
- expected selected Verdict
- forbidden claims

The fixture map is:

| fixture | same-board FEN | side to move | legal lines under test | expected open rows | expected blocked rows | expected roles | expected selected Verdict | forbidden claims |
|---|---|---|---|---|---|---|---|---|
| PawnAdvance vs PawnStop | `4k3/8/8/4P3/8/8/8/4K3 w - - 0 1` | White | `e5e6` | `Scene.PawnAdvance` | `Scene.PawnStop/blocked` | `Scene.PawnAdvance` Lead; `Scene.PawnStop/blocked` Blocked | `Scene.PawnAdvance` | promotion, promotion threat, pawn break, permanent stop, best move, only move |
| PawnAdvance vs Material | `4k3/8/8/4n3/3P4/8/8/4K3 w - - 0 1` | White | `d4d5`, `d4e5` | `Scene.PawnAdvance`, `Scene.Material` | none | `Scene.Material` Lead; `Scene.PawnAdvance` Support | `Scene.Material` | promotion, tactic, fork, best move, only move |
| PawnAdvance vs Defense | `4k3/8/8/5n1P/3Q4/8/8/4K3 w - - 0 1` | White | `h5h6`, `d4e4` | `Scene.PawnAdvance`, `Scene.Defense` | none | `Scene.Defense` Lead; `Scene.PawnAdvance` Support | `Scene.Defense` | promotion, tactic, fork, discovered attack, best move, only move |
| PawnStop vs Defense | `4k3/6n1/8/3qP3/5N2/8/8/4K3 b - - 0 1` | Black | `g7e6`, `d5c6` | `Scene.PawnStop`, `Scene.Defense` | none | `Scene.Defense` Lead; `Scene.PawnStop` Support | `Scene.Defense` | promotion stop, permanent stop, draw, tablebase, best defense, only move |
| PawnStop vs Line/Defender tactic | `r5k1/5n2/8/7P/8/8/4N3/4K3 b - - 0 1` | Black | `f7h6`, `a8e8` | `Scene.PawnStop`, `Tactic.Pin` | none | `Tactic.Pin` Lead; `Scene.PawnStop` Support | `Tactic.Pin` | promotion stop, permanent stop, skewer, discovered attack, best move, only move |
| Pawn row vs EngineCheck Supports/Caps/Refutes | `4k3/8/8/4P3/8/8/8/4K3 w - - 0 1` | White | `e5e6` | `Scene.PawnAdvance#Supports`, `Scene.PawnAdvance#Caps`, `Scene.PawnAdvance#Refutes` | `Scene.PawnAdvance#Refutes` | `Scene.PawnAdvance#Supports` Lead; `Scene.PawnAdvance#Caps` Support; `Scene.PawnAdvance#Refutes` Blocked | `Scene.PawnAdvance#Supports` | engine says, best move, only move, winning, conversion |
| Promotion-looking but no PromotionThreat yet | `4k3/8/4P3/8/8/8/8/4K3 w - - 0 1` | White | `e6e7` | `Scene.PawnAdvance` | `promotion-looking/blocked` | `Scene.PawnAdvance` Lead; `promotion-looking/blocked` Blocked | `Scene.PawnAdvance` | promotion, promotion threat, queens, unstoppable, winning, conversion |
| tablebase-looking but no tablebase authority | `4k3/6n1/8/4P3/8/8/8/4K3 b - - 0 1` | Black | `g7e6` | `Scene.PawnStop` | `endgame-result/blocked` | `Scene.PawnStop` Lead; `endgame-result/blocked` Blocked | `Scene.PawnStop` | tablebase, draw, permanent stop, best move, only move |

PIH-1 negative rows are fixture probes only. They do not create source-row
authority, proof homes, Story labels, speech keys, public JSON, renderer input,
LLM input, or public expected output. `PromotionThreat` must not appear as a
positive expected row. Endgame-result and tablebase-looking probes must remain
blocked and must not become public claims. `proofFailures` text remains internal
diagnostic material only.

### PIH-2 Role Stability

PIH-2 opens only `StoryTable` role stability checks over already-open
`Scene.PawnAdvance`, `Scene.PawnStop`, Defense, Material, Hanging, Fork,
DiscoveredAttack, Pin, RemoveGuard, and Skewer rows. It opens no new pawn
meaning, no PromotionThreat, no Promotion, no PawnBreak, no conversion, no
tablebase, no endgame-result authority, no public route `200`, no production
API, and no public/user-facing LLM narration.

PIH-2 requires:

- selected Verdict identity remains stable when input order changes
- the same pawn meaning cannot become duplicate Lead
- incomplete PawnAdvance and PawnStop rows cannot Lead
- EngineCheck Refutes rows become Blocked
- EngineCheck Caps rows create no standalone claim, ExplanationPlan, renderer
  output, or LLM claim
- PawnAdvance and PawnStop over the same passed pawn cannot both Lead
- PawnStop cannot own Defense claim meaning
- PawnAdvance cannot own promotion or conversion claim meaning

PIH-2 may fix only ordering or downstream-boundary bugs exposed by those
checks. Such fixes must not use `proofFailures` text as public ordering, must
not make raw engine status public meaning, and must not add a new Story label,
proof home, speech key, renderer route, production API, or public surface.

### PIH-3 Meaning Ownership Boundary

PIH-3 opens only Meaning Ownership Boundary checks over already-open
PawnAdvance, PawnStop, Material, Defense, and Line/Defender tactic rows. It
does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
conversion, unstoppable pawn, tablebase, endgame-result, public route `200`,
production API, or public/user-facing LLM narration.

PIH-3 fixes each row to one meaning:

- `Scene.PawnAdvance` owns only an already-passed pawn making a legal
  non-capturing non-promotion advance and remaining passed.
- `Scene.PawnStop` owns only a legal move directly stopping the passed pawn's
  next advance square.
- `Scene.Material` owns only an actual material balance change now.
- `Scene.Defense` owns only complete `ThreatProof` plus `DefenseProof`
  preventing immediate material loss.
- Line/Defender tactics own only their own line or guard relation:
  DiscoveredAttack reveals an attack, Pin pins, RemoveGuard removes a defender,
  and Skewer skewers one front and one rear target.

PIH-3 forbidden ownership:

- PawnAdvance must not speak will-promote, unstoppable-pawn, conversion,
  winning-endgame, best-move, or only-move meaning.
- PawnStop must not speak promotion-stop, draw, endgame-hold, permanent-stop,
  best-defense, best-move, or only-move meaning.
- PawnStop must not replace general Defense or own `defends_piece` meaning.
- Material and Hanging must not own pawn progress, pawn stop, promotion, or
  conversion meaning.
- Line/Defender tactics must not steal sibling line or guard relation meaning
  and must not own pawn progress or material-win meaning.

PIH-3 downstream smoke is allowed only as a boundary check over selected Lead
Verdicts from existing rows. Support, Context, Blocked, capped, and refuted
rows create no standalone claim, ExplanationPlan, renderer text, LLM input, or
public expected output.

### PIH-4 EngineCheck Interaction

PIH-4 opens only EngineCheck interaction checks over already-open
`Scene.PawnAdvance` and `Scene.PawnStop` rows. It reuses the existing
EngineCheck sidecar and status semantics only. EngineCheck remains incapable of
creating a Story, choosing a Verdict, owning a public claim, feeding renderer or
LLM stages directly, or replacing pawn proof.

PIH-4 requires:

- `Supports` creates no new pawn, engine, promotion, conversion, defense, or
  tactic claim. It may leave an already-selected uncapped pawn Story with the
  same bounded pawn claim.
- `Caps` suppresses standalone pawn expression or weakens it through the
  existing bounded engine-limited path. It must not create renderer text,
  LLM input, or an engine-owned claim.
- `Refutes` makes the corresponding PawnAdvance or PawnStop Story `Blocked`.
  The blocked row must not create an ExplanationPlan, renderer text, LLM input,
  or public expected output.
- `Unknown` creates no engine-related expression. If the underlying pawn row is
  otherwise selected, its text must be identical to the non-engine bounded pawn
  text and must not mention engine evidence.

PIH-4 forbids public or downstream wording that says or implies `engine says`,
raw PV explanation, public eval numbers, best move, only move, tablebase-like
authority, promotion, conversion, unstoppable pawn, draw, or winning endgame.
Raw engine evals, engine lines, source rows, EngineCheck diagnostics, and
`proofFailures` remain internal diagnostics and must not enter `Verdict.values`,
ExplanationPlan, renderer text, LLM prompt input, or public JSON.

PIH-4 does not open a new EngineCheck proof home, Story label, speech key,
public route `200`, production API, public/user-facing LLM narration, or any
new pawn meaning.

### PIH-5 Negative Corpus

PIH-5 opens only the negative corpus for close PawnAdvance and PawnStop false
positives. It proves that looking like pawn progress, a pawn stop, a promotion
race, an endgame tablebase case, or a king-route idea is not enough. Existing
complete proof or silence is the rule.

PIH-5 must keep these rows silent:

- a pawn advances but was not already passed
- a passed pawn advances but lacks exact after-board replay proof
- a pawn-stop-looking move leaves the passed pawn's next advance square still
  available
- a pawn-stop-looking move only claims long-term blockade instead of directly
  stopping the next advance square
- a promotion-looking position before PromotionThreat opens
- a tablebase-looking position
- a king opposition-looking position
- a pawn race-looking position
- a route mismatch between Story identity, proof sidecar, and EngineCheck line
- stale or wrong-board engine evidence
- EngineCheck `Refutes` over a plausible pawn row

PIH-5 negative rows may use proofFailures and diagnostic fields internally, but
they must not become public expected output, `Verdict.values`, ExplanationPlan,
renderer text, LLM prompt input, public JSON, or source-row authority.

PIH-5 does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
long-term blockade, unstoppable pawn, conversion, winning or drawn endgame,
tablebase, pawn race, king route, opposition, best move, only move, public route
`200`, production API, public/user-facing LLM narration, a new proof home, a new
Story label, or a new speech key.

### PIH-6 Downstream Boundary Smoke

PIH-6 opens only downstream boundary smoke over already-open PawnAdvance and
PawnStop rows. It proves that selected Lead Verdict data is the only pawn data
allowed to reach expression stages. Support, Context, Blocked, capped, and
refuted pawn rows remain relation or diagnostic rows and create no standalone
text.

PIH-6 requires:

- ExplanationPlan receives selected Lead Verdict data only for pawn expression.
- DeterministicRenderer receives ExplanationPlan only.
- LLM smoke receives only rendered text, claim key, strength, forbidden wording,
  and the rephrase-only instruction: `Rephrase only. Do not add chess facts.`
- Support, Context, Blocked, capped, and refuted pawn rows produce no standalone
  ExplanationPlan, renderer text, LLM input, prompt, public JSON, or public
  expected output.
- Prompt smoke must not expose BoardFacts, Story rows, Verdict internals,
  EngineCheck, EngineEval, EngineLine, proofFailures, source rows, FEN, raw PV,
  route internals, proof sidecars, or diagnostics.

PIH-6 forbidden downstream wording includes: will promote, unstoppable, wins,
winning, conversion, draws, holds, tablebase, best move, only move, forced,
pressure, and initiative. These are forbidden for both PawnAdvance and PawnStop
selected Lead expression, even when the raw row looks promotion-like,
endgame-like, engine-supported, or strategically suggestive.

PIH-6 does not open PromotionThreat, Promotion, PawnBreak, broad PawnTactic,
unstoppable pawn, conversion, winning or drawn endgame, tablebase, pawn race,
king route, opposition, pressure, initiative, best move, only move, public route
`200`, production API, public/user-facing LLM narration, new renderer routes,
new LLM authority, a new proof home, a new Story label, or a new speech key.

### PIH-7 Diagnostics Boundary

PIH-7 opens only diagnostics boundary hardening over already-open PawnAdvance
and PawnStop rows.

PIH-7 requires:

- proofFailures remain internal diagnostics only.
- raw proof text does not enter `Verdict.values`.
- EngineCheck text, raw PV, eval numbers, and engine sidecar text do not lower
  directly into ExplanationPlan, renderer text, or LLM smoke output.
- StoryTable debug relations, including capped and refuted relation keys, do not
  become renderer wording.
- test helper names, fixture-map language, negative-corpus language, and PIH
  stage labels do not become runtime authority names or production source
  concepts.

PIH-7 diagnostic wording forbidden in public expression includes:

- proofFailures.
- missing evidence.
- same-board proof.
- exact after-board replay.
- EngineCheck, EngineLine, and EngineEval.
- StoryTable debug relation.
- blocked_by_engine_refute.
- capped_same_story.
- same_family_lower_rank.
- raw eval numbers or PV text.

PIH-7 may use diagnostic text inside tests and debugging only. It does not open
a public diagnostic explanation, public JSON diagnostic payload, public route
`200`, production API, public/user-facing LLM narration, a new proof home, a new
Story label, or a new speech key.

### PIH Closeout Hard Cleanup

PIH Closeout opened no new Story family, proof home, Story writer, claim key, renderer wording, LLM behavior, public route `200`, production API, or public/user-facing LLM narration at the PawnAdvance/PawnStop baseline. PromotionThreat-0 is the third Pawn / Promotion Neighborhood slice.

PIH Closeout must confirm:

- no new Story family was added beyond already-open `Scene.PawnAdvance` and `Scene.PawnStop`.
- no new proof home was added beyond already-open `PawnAdvanceProof` and `PawnStopProof`.
- `PassedPawnObservation`, `PawnAdvanceProof`, and `PawnStopProof` authority remains separated.
- `Scene.PawnAdvance` and `Scene.PawnStop` do not invade each other's meaning.
- PromotionThreat, Promotion, PawnBreak, tablebase, pawn race, king route, and opposition remain closed authority and forbidden wording only.
- Material, Defense, Hanging, and Line / Defender tactic homes are not invaded.

PIH Closeout duplicate audit:

- one chess meaning: bounded passed-pawn advance belongs to `Scene.PawnAdvance`; bounded immediate next-square stop belongs to `Scene.PawnStop`.
- one proof home for each pawn meaning: `PawnAdvanceProof` for advance; `PawnStopProof` for stop.
- one Story label for each pawn meaning: `Scene.PawnAdvance` and `Scene.PawnStop`.
- one speech key for each pawn meaning: `advances_passed_pawn` and `stops_pawn_advance`.
- one detailed live authority document: `StoryInteractionLaw.md`.

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PIH Closeout only.

Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PIH Closeout closes when PawnAdvance and PawnStop interaction hardening has no new pawn meaning, no mixed proof authority, no sibling claim-home invasion, no duplicated detailed authority outside StoryInteractionLaw.md, no promoted test helper, no public route `200`, no production API, and no public/user-facing LLM narration.

### PromotionThreat-0 Charter

PromotionThreat-0 opens only the third narrow Pawn / Promotion Neighborhood vertical slice.

First positive scope is not broad promotion race, winning pawn, conversion, or
actual Promotion Story.

Core sentence:

Pawn facts observe structure. PromotionThreatProof proves the next-move
promotion threat. Scene.PromotionThreat may speak only immediate threat, not
unstoppable conversion.

PromotionThreat-0 opens:

- narrow `Scene.PromotionThreat`
- legal pawn move
- exact after-board
- next move promotion is legal for the same pawn on that after-board
- promotion square
- promotion route
- selected Verdict after bounded promotion-threat wording
- the bounded speech key `threatens_promotion_next`

PromotionThreat-0 does not open:

- actual Promotion Story
- unstoppable pawn
- cannot be stopped
- winning endgame
- conversion
- tablebase claim
- pawn race result
- best move or only move
- forced win
- no counterplay
- public route `200`
- production API
- public/user-facing LLM narration

`PromotionThreatProof` is the proof home for this slice. It is not a public
`Story` and does not own conversion, endgame result, tablebase, race, or
unstoppable-pawn meaning.

### PromotionThreat-1 PromotionThreatProof

`PromotionThreatProof` is the proof home for the narrow
`Scene.PromotionThreat` slice. It proves immediate next-move promotion-threat
evidence only; it is not a public `Story`.

PromotionThreatProof must prove:

- threatening side
- rival side
- pawn identity
- pawn move that creates the threat
- exact after-board replay
- next promotion move
- promotion square
- promotion route
- next promotion move is legal on the after-board
- pawn is non-promoted before the creating move
- same-board proof

`PromotionThreatProof` is not a public `Story`.
`PawnAdvanceProof` does not own `PromotionThreat`.
`PawnStopProof` does not own `PromotionThreat`.

PromotionThreat-1 forbids:

- `unstoppable`
- `cannot be stopped`
- `will queen`
- `wins`
- `conversion`
- `forced`
- `tablebase`

proofFailures must not become renderer input or LLM input.

### PromotionThreat-2 ScenePromotionThreat Writer

`ScenePromotionThreat` is the named writer for narrow `Scene.PromotionThreat`.

ScenePromotionThreat writer conditions:

- complete `StoryProof`
- complete `PromotionThreatProof`
- same-board legal replay
- legal creating pawn move
- legal next promotion move on exact after-board
- writer = `ScenePromotionThreat`
- `EngineCheck` does not `Refute`

ScenePromotionThreat Story identity:

- `scene = PromotionThreat`
- `tactic = None`
- `plan = None`
- `side = threatening side`
- `rival = rival side`
- `target = promotion square`
- `anchor = pawn origin square`
- `route = creating pawn move`
- `secondaryTarget = None` unless the fixed identity shape requires the promotion destination

`ScenePromotionThreat` must not create actual Promotion.
`ScenePromotionThreat` must not replace `PawnAdvance` or `PawnStop` meaning.
`ScenePromotionThreat` must not create `unstoppable` or `wins` meaning.

### PromotionThreat-3 Negative Corpus

A pawn near promotion is not a PromotionThreat. Legal next-move promotion proof or silence.

PromotionThreat-3 keeps these rows closed:

- legal creating move missing
- same-board proof missing
- move is not a pawn move
- creating move itself is promotion
- next promotion move is not legal on the exact after-board
- promotion square cannot be computed
- next move is not promotion because two or more moves are still needed
- rival stoppability check tries to expand into `unstoppable`
- tablebase, winning, or conversion wording enters the path

`ScenePromotionThreat` must return no Story for every negative corpus row.
`PromotionThreatProof` may record internal missing evidence only.
Renderer and LLM smoke must reject `unstoppable`, `wins`, `tablebase`, and `conversion` wording.

### PromotionThreat-4 EngineCheck Reuse

PromotionThreat-4 reuses only the existing `EngineCheck` sidecar.

`EngineCheck` cannot create `Scene.PromotionThreat`.

EngineCheck status handling for PromotionThreat:

- `Supports` does not create a new claim.
- `Caps` suppresses standalone `threatens_promotion_next` speech or weakens it to bounded relation-only evidence.
- `Refutes` blocks the PromotionThreat Story.
- `Unknown` creates no engine expression.

PromotionThreat-4 forbidden engine wording:

- `engine says`
- eval numbers
- tablebase result
- best move
- only move
- winning endgame
- forced win

Completion standard: PromotionThreat-4 closes when `EngineCheck` can attach only to an existing same-board `ScenePromotionThreat` Story route, cannot create PromotionThreat, cannot add a new claim under Supports, suppresses or bounds capped rows, blocks refuted rows, keeps Unknown engine-silent, and exposes no engine wording, eval numbers, tablebase, best-move, only-move, winning-endgame, or forced-win claims.

### PromotionThreat-5 StoryTable Integration

PromotionThreat-5 integrates `Scene.PromotionThreat` into `StoryTable` collision behavior only.

PromotionThreat-5 collision rows:

- `Scene.PawnAdvance`
- `Scene.PawnStop`
- existing Material / Defense / Hanging / Line rows
- `Scene.PromotionThreat`

PromotionThreat-5 verification:

- input order remains stable
- PromotionThreat does not own PawnAdvance meaning
- PromotionThreat does not own PawnStop meaning
- PawnStop must not create a `stops promotion` claim
- actual material or tactical claim remains in the existing home
- capped or refuted PromotionThreat creates no standalone text

Completion standard: PromotionThreat-5 closes when `StoryTable` orders PromotionThreat deterministically against PawnAdvance, PawnStop, Material, Defense, Hanging, and Line rows; no row borrows another row's claim meaning; actual material and tactical claims stay in their existing homes; and capped or refuted PromotionThreat rows do not reach standalone ExplanationPlan, renderer, or LLM wording.

### PromotionThreat-6 ExplanationPlan

PromotionThreat-6 lowers only a selected uncapped `Lead` Verdict for `Scene.PromotionThreat`.

`ExplanationPlan` may admit only the `threatens_promotion_next` claim key.

PromotionThreat-6 allowed claim keys:

- `threatens_promotion_next`

PromotionThreat-6 forbidden claim keys:

- `unstoppable_pawn`
- `will_promote`
- `cannot_be_stopped`
- `wins_endgame`
- `converts_advantage`
- `best_move`
- `only_move`
- `forced`
- `tablebase_win`
- `no_counterplay`

Support, Context, Blocked, capped, and refuted PromotionThreat rows have no standalone claim.

Completion standard: PromotionThreat-6 closes when only selected uncapped Lead PromotionThreat Verdicts lower to `threatens_promotion_next`; Support, Context, Blocked, capped, and refuted rows create no standalone claim; and forbidden promotion, unstoppable, winning, conversion, best/only, forced, tablebase, and no-counterplay claim keys remain absent.

### PromotionThreat-7 Deterministic Renderer

Renderer input is `ExplanationPlan` only.

PromotionThreat-7 allowed deterministic wording:

- `{route} threatens to promote next.`

PromotionThreat-7 forbidden renderer wording:

- `will promote`
- `cannot be stopped`
- `unstoppable`
- `wins`
- `winning endgame`
- `converts`
- `best move`
- `only move`
- `forces`
- `tablebase win`
- `no counterplay`

Renderer must not read `Story`, `Verdict`, `PromotionThreatProof`, `BoardFacts`, `EngineCheck`, `proofFailures`, raw legal continuations, or source rows.

Completion standard: PromotionThreat-7 closes when `DeterministicRenderer` accepts only `ExplanationPlan`, phrases selected uncapped Lead PromotionThreat as `{route} threatens to promote next.`, rejects Support, Context, Blocked, capped, refuted, no-claim, and malformed plans, and emits none of the forbidden promotion, unstoppable, winning, conversion, best/only, forced, tablebase, or no-counterplay wording.

### PromotionThreat-8 LLM Smoke

PromotionThreat-8 reuses only the existing 8B LLM smoke boundary.

PromotionThreat-8 LLM input:

- renderedText
- claimKey
- strength
- forbidden wording
- `Rephrase only. Do not add chess facts.`

PromotionThreat-8 forbidden LLM input and additions:

- raw `Story`
- raw `PromotionThreatProof`
- `BoardFacts`
- `EngineCheck`
- raw PV
- `proofFailures`
- new move
- new line
- actual promotion claim
- unstoppable claim
- winning claim
- conversion claim
- tablebase claim

Completion standard: PromotionThreat-8 closes when LLM smoke receives only rendered text contract fields for PromotionThreat, rejects raw Story, PromotionThreatProof, BoardFacts, EngineCheck, raw PV, proofFailures, new moves, new lines, actual promotion, unstoppable, winning, conversion, and tablebase claims, and opens no public/user-facing LLM narration, production API, or public route 200.

PromotionThreatProof must prove:

- moving side
- rival side
- pawn identity before the creating move
- pawn identity after the creating move
- legal pawn creating move
- creating move is not itself a promotion
- exact after-board replay
- pawn is on the penultimate rank after the creating move
- promotion square
- promotion route
- same-board proof
- legal next-move promotion continuation on the exact after-board

`ScenePromotionThreat` is the only writer that may create
`Scene.PromotionThreat`.

ScenePromotionThreat writer conditions:

- complete `StoryProof`
- complete `PromotionThreatProof`
- same-board legal replay
- legal pawn creating move
- legal next-move promotion route
- writer = `ScenePromotionThreat`
- `scene = PromotionThreat`
- `tactic = None`
- `plan = None`
- `side = creating pawn side`
- `rival = opposite side`
- `target = promotion square`
- `anchor = creating pawn origin square`
- `route = creating pawn move`

`Scene.PromotionThreat` must not create `Scene.PawnAdvance`,
`Scene.PawnStop`, `Scene.Promotion`, `Tactic.Promote`, broad PawnTactic,
conversion, race, tablebase, or endgame-result meaning.

ExplanationPlan may admit only selected uncapped Lead
`Scene.PromotionThreat` Verdict data. The only allowed claim key is
`threatens_promotion_next`. Support, Context, Blocked, capped, and refuted rows have no
standalone claim.

Allowed deterministic wording:

- `{route} threatens to promote next.`

Forbidden wording includes:

- `will promote`
- `queens`
- `unstoppable`
- `cannot be stopped`
- `wins`
- `winning endgame`
- `conversion`
- `tablebase`
- `pawn race`
- `best move`
- `only move`
- `forced`
- `no counterplay`

Renderer input is `ExplanationPlan` only. Renderer must not read `Story`,
`Verdict`, `PromotionThreatProof`, `BoardFacts`, `EngineCheck`,
`proofFailures`, raw legal continuations, or source rows.

The existing LLM smoke boundary may rephrase only rendered text, claim key,
strength, forbidden wording, and the instruction `Rephrase only. Do not add
chess facts.` It does not open public/user-facing LLM narration.

PromotionThreat-0 duplicate audit:

- one chess meaning: immediate next-move promotion threat only
- one proof home: `PromotionThreatProof`
- one Story label: `Scene.PromotionThreat`
- one speech key: `threatens_promotion_next`
- one detailed live authority document: `StoryInteractionLaw.md`

Completion standard: PromotionThreat-0 closes when a legal pawn move creating a
legal next-move promotion route on the exact after-board can produce only
bounded `Scene.PromotionThreat` wording, while actual promotion, unstoppable
conversion, endgame, tablebase, pawn-race, best/only/forced, public route
`200`, production API, and public/user-facing LLM narration remain closed.

### PromotionThreat Closeout Hard Cleanup

PromotionThreat Closeout opens no new chess meaning. It only audits the PromotionThreat hard cleanup surface.

PromotionThreat Closeout must confirm:

- `PromotionThreatProof` proves only immediate next-move promotion-threat evidence on the exact after-board.
- `Scene.PromotionThreat` is the only Story label for this immediate threat meaning.
- `threatens_promotion_next` is the only speech key for this meaning.

PromotionThreat owns no `Scene.PawnAdvance`, `Scene.PawnStop`, actual Promotion, or PawnBreak meaning.
PromotionThreat owns no `Scene.Material`, `Scene.Defense`, `Tactic.Hanging`, or Line / Defender tactic meaning.
`unstoppable`, `will promote`, `cannot be stopped`, `conversion`, and `tablebase` remain forbidden wording only, not live authority.

PromotionThreat Closeout duplicate checks:

- one chess meaning: immediate next-move promotion threat only
- one proof home: `PromotionThreatProof`
- one Story label: `Scene.PromotionThreat`
- one speech key: `threatens_promotion_next`
- one detailed live authority document: `StoryInteractionLaw.md`

`README.md`, SSOT, Architecture, Contract, and Manifest may summarize PromotionThreat only.
Renderer and LLM wording must remain no stronger than `threatens_promotion_next`.
Public route `200`, production API, and public/user-facing LLM narration remain closed.

Completion standard: PromotionThreat Closeout closes when `PromotionThreatProof`,
`Scene.PromotionThreat`, and `threatens_promotion_next` remain separate authority homes,
PromotionThreat owns no PawnAdvance, PawnStop, actual Promotion, PawnBreak, Material,
Defense, Hanging, or Line / Defender tactic meaning, closed promotion, unstoppable,
conversion, and tablebase wording remains forbidden only, renderer and LLM smoke stay
bounded to `threatens_promotion_next`, detailed authority stays only in
`StoryInteractionLaw.md`, and public route `200`, production API, and public/user-facing
LLM narration remain closed.

## Proof And Interaction Law

| proof field | live class | raises | caps or blocks |
|---|---|---|---|
| boardProof | board truth | every non-source Story | zero blocks board-backed speech. |
| lineProof | line truth | tactics, conversion, initiative, trade, race | zero blocks tactic and line-backed stories. |
| ownerProof | side truth | lead eligibility | high ownerProof with no side blocks lead. |
| anchorProof | anchor truth | structural and plan Stories | high anchorProof with no anchor blocks lead. |
| routeProof | route truth | plans, tactics, conversion | high routeProof with no route blocks lead. |
| persistence | long pressure | planHeat | immediate tactic override can cap it. |
| immediacy | near-term force | tacticHeat | no legal line caps it to context. |
| forcing | move pressure | tacticHeat | missing reply map blocks forcing wording. |
| conversionPrize | gain size | tacticHeat and planHeat | no gain identity blocks conversion wording. |
| counterplayRisk | rival resource | caps plan and conversion | above 70 blocks plan lead. |
| kingHeat | king attack support | tacticHeat | no legal line blocks mate or attack claim. |
| pieceSupport | piece support | planHeat | no named piece caps piece wording. |
| pawnSupport | pawn support | planHeat | no named pawn caps pawn wording. |
| sourceFit | source context | source/opening context | source cannot replace board proof. |
| novelty | source context | novelty wording | no source row blocks novelty wording. |
| clarity | explanation clarity | planHeat | missing identity caps clarity to context. |

## Nonlinear Rules

| rule | effect |
|---|---|
| Hard proof blocker | Missing side, target, anchor, route, rival, required legal line, or same-root proof sidecar is a hard public-output block. |
| Tactical override | An opposing Tactic or Blunder at public floor blocks Plan lead. |
| Same-side tactic priority | A same-side Tactic with tacticHeat at least 70 and lineProof at least 65 outranks Plan. |
| Source cap | Source and Opening context cannot lead over a board-backed Story at public floor. |
| Engine cap | Engine context can cap or confirm wording only after Story identity and legal line are bound. |
| Board-only cap | BoardMood facts without Story proof allow observed/possible wording only, not advice or verdict. |
| Blunder override | A proven blunder or losing tactic suppresses strategic praise about the same side. |
| Counterplay cap | Counterplay risk above 70 blocks conversion and plan lead unless the Story proves the rival resource is answered. |
| Quiet fallback | Quiet can lead only when every non-Quiet Story is below public floor. |
| Render cap | Render may only verbalize selected Verdicts and cannot repair missing identity or proof. |

## Proof-Deficit Logs

Every blocked Story must report proof deficit, not only family pass/fail.
Validation output must explain which identity or proof tuple members are absent
and which board observations were present.

This diagnostic shape is internal validation, test, and debugging output only.
It is not API/public JSON, renderer input, LLM input, or user commentary. It
shows why the kernel cannot speak; it does not provide text that may be spoken.

Required blocked-story diagnostic shape:

```json
{
  "story": "...",
  "leadAllowed": false,
  "blockedBy": ["..."],
  "boardFactsPresent": ["..."],
  "proofCoordinates": {
    "root": "...",
    "side": "...",
    "target": "...",
    "anchor": "...",
    "route": "...",
    "rival": "...",
    "requiredLegalLine": "...",
    "sameRootProofSidecar": "..."
  },
  "missingSidecar": ["..."],
  "reason": "..."
}
```

Example:

```json
{
  "story": "Plan.OpenFile",
  "leadAllowed": false,
  "blockedBy": ["routeProof", "sameRootLine"],
  "boardFactsPresent": ["rook_open_file_entry"],
  "proofCoordinates": {
    "root": "current-position-root",
    "side": "White",
    "target": "open file entry square",
    "anchor": "rook square",
    "route": null,
    "rival": "Black",
    "requiredLegalLine": null,
    "sameRootProofSidecar": null
  },
  "missingSidecar": ["legal file-entry line", "same-root route proof"],
  "reason": "The rook has an entry-square observation, but no same-root line proves usable file entry."
}
```

This keeps false positives actionable. The fix should be to supply the missing
proof coordinate or keep the Story blocked, not to add another family-specific
shortcut.

## Fixture Duties

The interaction law must be tested with positions or synthetic Stories that
cover these failure modes:

- Opening context with a tactical refutation: tactic or blunder overrides
  source/opening speech.
- Strategic plan with no route: plan support exists but public plan speech is
  blocked.
- Mate-net shape with legal escape: king pressure is capped below mate wording.
- Material gain with compensation line: material Story is capped by
  counterplay/conversion proof.
- Source row matching the opening but board proof contradicting it: source stays
  context.
- Quiet position with no public floor Story: Quiet may lead.
