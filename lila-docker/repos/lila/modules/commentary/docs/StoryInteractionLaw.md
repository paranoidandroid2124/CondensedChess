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
ply or a bounded line, same-board proof, and missing evidence. It must not say
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
- target exists
- evidenceLine exists
- forbidden wording set exists

Allowed first template:

- `d4xe5 wins material against the piece on e5.`

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
evidenceLine, allowed claim key, strength, and forbidden wording. It must not
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
| Tactic.Skewer | line piece, valuable front target, rear target | legal checking or attack line | front target can escape with tempo |
| Tactic.Xray | `white_xray_line` or `black_xray_line` | legal uncovering or pressure line | screen not movable, target defended |
| Tactic.Fork | attacker, two targets | legal move to fork square | fork square unsafe or targets can respond |
| Tactic.Discover | screened line and moving piece | legal discovered move | moving piece illegal or line target absent |
| Tactic.RemoveGuard | guard-capture move | legal capture of guard and follow-up target | guard not sole defender |
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

`Tactic.Hanging` and the narrow `Tactic.Fork` vertical slice are the only live
positive tactics. All other tactic names below are closed until their proof home
and writer are explicitly admitted.
`CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`,
`ExplanationPlan`, and Renderer remain in their current ownership boundaries.
They may be reused only through selected Verdict handoff and family-specific
charters; none of them may read raw proof material or create a new tactic.

Proof-home width:

| proof home | covered tactic names | shared proof shape | reusable current homes | new home needed |
|---|---|---|---|---|
| CaptureProof | `Tactic.Hanging`, `Tactic.Loose`, capture-shaped `Tactic.QueenHit` | legal capture, target identity, defender or recapture map, bounded material result | `CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`, Renderer after claim mapping | no for live Hanging; yes before Loose or QueenHit speech broadens capture meaning |
| TargetProof | `Tactic.Fork`, `Tactic.PawnFork`, `Tactic.Skewer`, `Tactic.QueenHit`, `Tactic.Tempo`, `Tactic.InBetween` | one legal move creates target relation or tempo pressure; reply map proves rival cannot answer all relevant targets | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` for material ending | yes |
| LineProof | `Tactic.AbsPin`, `Tactic.RelPin`, `Tactic.Xray`, `Tactic.Discover`, `Tactic.Clear`, line-shaped `Tactic.Skewer` | same-board ray, screen, front or rear target, reveal or restriction, legal exploitation line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes |
| DefenderProof | `Tactic.RemoveGuard`, `Tactic.Overload`, `Tactic.Deflect`, `Tactic.Decoy` | defender identity, protected target, dependency relation, legal test that removes or distracts the defender | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes |
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
| W06 | `Tactic.Skewer` | TargetProof and LineProof over front target and rear target | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof or TargetProof pairing | front target escapes with tempo, rear target not reachable, line can be blocked, check claim too strong | yes | no | LineProof and TargetProof pairing charter |
| W07 | `Tactic.Tempo` | TargetProof over gained turn and restricted rival reply | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, TargetProof with reply restriction | rival has equal forcing move, gained turn has no target, move is only a threat label | yes | no | TargetProof reply-map charter |
| W08 | `Tactic.InBetween` | TargetProof over forcing intermezzo and ignored-loss test | `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan`; optional `CaptureResult` | yes, TargetProof with forcing reply map | rival can ignore, in-between move loses material, first threat was not real, move order is illegal | yes | no | TargetProof reply-map charter plus intermezzo corpus |
| W09 | `Tactic.AbsPin` | LineProof with king behind screen and legal restriction | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | pinned piece can legally move, line can be blocked, tactic does not exploit pin, king line is stale | yes | no | LineProof charter plus pin exploitation corpus |
| W10 | `Tactic.RelPin` | LineProof with valuable rear target and screen | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | rear target not valuable enough, screen can move with gain, line pressure has no legal use | yes | no | LineProof charter plus relative-pin corpus |
| W11 | `Tactic.Xray` | LineProof over screened ray and legal reveal or pressure | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | screen not movable, target defended, reveal loses tempo, ray observation is only geometry | yes | no | LineProof charter plus xray negative corpus |
| W12 | `Tactic.Discover` | LineProof over screened line and moving piece | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | moving piece illegal, line target absent, discovered attack can be answered, moving piece hangs | yes | no | LineProof reveal charter |
| W13 | `Tactic.Clear` | LineProof over clearance move and newly usable line | BoardFacts `LineFact`, `StoryProof`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, LineProof | cleared line has no target, clearance loses material, rival closes the line, route not legal | yes | no | LineProof clearance charter |
| W14 | `Tactic.RemoveGuard` | DefenderProof plus optional CaptureProof | `StoryProof`, optional `CaptureResult`, `EngineCheck`, `StoryTable`, `ExplanationPlan` | yes, DefenderProof | guard is not sole defender, recapture equalizes, protected target irrelevant, guard capture is illegal | yes | no | DefenderProof identity and dependency corpus |
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
5. Admit DefenderProof only after defender identity, sole-dependency, and
   target-gain tests exist.
6. Admit PromotionProof only after pawn route, stop-square, capture-stop, and
   tempo-count tests exist.
7. Admit KingProof only after escape, interposition, capture reply, and mate
   wording boundaries are proven.
8. Admit MobilityProof only after legal mobility and counter-threat tests
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
