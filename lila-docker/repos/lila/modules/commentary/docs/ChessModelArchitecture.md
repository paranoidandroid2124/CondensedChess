# Chess Model Architecture

This document describes the current pre-render chess model target for this
branch.

## Current Scope

The current checkpoint covers board truth, primitive board geometry, Stage 1
board facts, Stage 2 Story Proof, Stage 3 first narrow positive Story, and
Stage 4 Engine Check closeout, Stage 5-1 Hanging Role Rules, Stage 5-2
Deterministic Ordering, Stage 5-3 Conflict and Block Rules, Stage 5-4
Verdict Diagnostic Boundary, Stage 5 Closeout Pass, Stage 6-0 Explanation Plan
Charter, Stage 6-1 Explanation Plan Shape, Stage 6-2 Tactic.Hanging Allowed
Claim Mapping, Stage 6-3 Forbidden Wording Boundary, Stage 6-4 Support /
Context Relation, Stage 6-5 Selected Verdict Only Guard, Stage 6 Closeout
Pass, Stage 7-0 Deterministic Renderer Charter, Stage 7-1 Renderer Input
Guard, Stage 7-2 Minimal Tactic.Hanging Template, Stage 7-3 Forbidden Wording
Enforcement, Stage 7-4 No Text for Support / Context / Blocked, Stage 7-5
Rendered Line Shape, Stage 7-6 Renderer Baseline Tests, Stage 7 Closeout Pass,
Stage 8 Prompt Smoke, Fork-8 Deterministic Renderer for Fork, and Fork-9 LLM
Smoke for Fork. It is
intentionally not a full product commentary backend.

The safe target is to make unproven commentary impossible before opening any
positive public claim. Broad strategy, plan labels, conversion claims, and
LLM or public route openings remain downstream of proof-backed Stories.
Deterministic renderer phrasing stays bounded by Explanation Plan.

The current branch owns only:

`Board Truth / Primitive Geometry / Story boundary / Verdict boundary / Explanation Plan boundary / Deterministic Renderer boundary`

Current implementation scope is Defense Slice Closeout Pass.
Defense-0 opened only the charter for the first narrow `Scene.Defense` slice.
Defense-1 opened only `ThreatProof`.
Defense-1 opens only `ThreatProof`.
Defense requires a threat. ThreatProof proves what must be stopped.
DefenseProof proves how the move stops it.
DefenseProof proves how a specific move stops a specific ThreatProof.
Defense is not no-counterplay.
Defense-2 opens only `DefenseProof`.
Defense-3 opens only the named `SceneDefense` writer for one narrow `Scene.Defense` Story.
Defense-4 opens only the Defense negative corpus.
Defense-looking false positives must stay silent without complete ThreatProof and complete DefenseProof.
Defense-5 opens only existing EngineCheck reuse for existing `Scene.Defense` Stories.
EngineCheck may support, cap, or refute an existing Defense Story, but it does not create Defense.
Defense-6 opens only StoryTable integration for existing Hanging, Fork, Material, and Defense rows.
StoryTable deterministically orders Hanging, Fork, Material, and Defense without creating new chess meaning.
Defense-7 opens only ExplanationPlan mapping for selected `Scene.Defense` Verdicts.
Defense ExplanationPlan creates no meaning stronger than the selected Verdict.
Defense-8 opens only deterministic renderer text for selected Defense ExplanationPlan.
Renderer text is no stronger than the Defense ExplanationPlan.
Defense-9 opens only LLM smoke for selected Defense ExplanationPlan and RenderedLine.
LLM smoke does not make Defense text stronger.
Defense Slice Closeout opens no new chess meaning beyond the narrow `Scene.Defense` vertical slice.
Defense closes as a narrow proof-backed attacked-piece material-loss defense slice only.
Public route `200`, production API, and public/user-facing LLM narration remain closed.
The completed Stage 8, Fork-9, Material Slice Closeout, Defense-0, Defense-1, Defense-2, Defense-3, Defense-4, Defense-5, Defense-6, Defense-7, Defense-8, and Defense-9 scopes remain closed baselines.
Stage 3 remains open
only for Material proof kernel, `Tactic.Hanging`, Hanging negative corpus, the
narrow `Tactic.Fork` vertical slice, the narrow `Scene.Material` writer, and
Material negative corpus and EngineCheck reuse.
Stage 4 is named `Engine Check`. Stage 4 opens
only `EngineCheck`, `EngineLine`, and `EngineEval` as internal evidence,
same-board and stale guards, `Tactic.Hanging` attachment, narrow
`Tactic.Fork` attachment, narrow `Scene.Material` EngineCheck reuse,
false-positive corpus, and conservative StoryTable diagnostics for existing
`Tactic.Hanging`, narrow `Tactic.Fork`, and single proof-backed
`Scene.Material` Stories. Stage 5 is named `Story Order` and opens only
StoryTable role ordering for existing `Tactic.Hanging` Story rows and existing
narrow `Tactic.Fork` Story rows; Material-3 adds only single-row StoryTable
admission for proof-backed `Scene.Material`; Material-4 adds only the Material
negative corpus; Material-5 reuses only existing `EngineCheck` for
`Scene.Material`; Material-6 adds only StoryTable integration for existing
Hanging, Fork, and Material rows. Stage 5-1 assigns Lead, Support, Context, and Blocked roles
inside that slice. Stage 5-2 fixes deterministic
ordering inputs for those rows. Stage
5-3 tightens close blockers and context relations for those rows. Stage 5-4
keeps Verdict diagnostics out of public numeric values and downstream public
surfaces. Stage 5 closeout confirmed Story ordering only and selected-Verdict
handoff only.
Stage 6-0 opens only the Explanation Plan charter and selected-Verdict speech
boundary; it does not open sentences, renderer, LLM, public route `200`,
pedagogy, new Story families, or engine explanation. Stage 6-1 opens only the
Explanation Plan shape for one selected `Tactic.Hanging` Lead Verdict.
`allowedClaim` stays a structured key such as `can_win_piece`; the first shape
carries `bounded` strength and forbidden wording, not public prose. Stage 6-2
opens only `Tactic.Hanging` allowed claim mapping. Uncapped Lead Verdict only
may carry an allowed claim key. Support, Context, Blocked, and engine-capped
Verdicts do not create standalone public claims. `engineStrengthLimited`
suppresses claim keys and strengthens forbidden wording. Stage 6-3 opens only
forbidden wording boundary. Explanation Plan must carry the default forbidden
wording set. `Tactic.Hanging` remains bounded material tactic wording only,
and `engineStrengthLimited` strengthens forbidden wording without carrying a
claim. Stage 6-4 opens only Support and Context relation structure. Uncapped
Lead only carries an allowed claim. Support and Context create no standalone
public claim. Blocked remains debug-only relation structure, and proofFailures
do not feed relation wording.
Stage 6-5 opens only the selected Verdict input guard. Explanation Plan accepts
selected Verdict only. Raw BoardFacts, BoardMood, root atoms, CaptureResult,
EngineCheck, EngineEval, EngineLine, raw PV, proofFailures text, unselected
Story, unselected Verdict, and source rows remain forbidden inputs.
Material-7 opens only ExplanationPlan mapping for selected `Scene.Material`
Verdicts. It admits selected Verdict only, emits `material_balance_changes`
first for uncapped Lead only, and opens no Material renderer text, LLM smoke,
public route `200`, production API, winning, decisive, conversion, blunder,
best-move, forced-win, or no-counterplay claim.
Material-8 opens only deterministic renderer text for selected
`Scene.Material` ExplanationPlan. Renderer receives `ExplanationPlan` only and
may render bounded Material text such as `After {route}, White comes out ahead
in material.` It opens no LLM smoke, public route `200`, production API,
winning, decisive, blunder, forced, best-move, no-counterplay, engine-says,
conversion, technically-winning, or full-evaluation claim.
Material-9 opens only LLM smoke for selected Material ExplanationPlan and
RenderedLine. 8B Material Codex CLI input is limited to renderedText,
claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess
facts.` It reads no raw Verdict, Story, material proof, CaptureResult,
ExchangeResult, EngineCheck, BoardFacts, engine eval, raw PV, proofFailures,
or source row data, and opens no public/user-facing LLM narration, public
route `200`, production API, winning, decisive, forced, blunder, best-move,
conversion, or stronger claim.
Material Slice Closeout opens no new chess meaning beyond the narrow
`Scene.Material` vertical slice. `CaptureResult` remains the simple capture
and immediate bounded recapture proof home, `ExchangeResult` remains unopened,
`Scene.Material` owns only the Story label, and `material_change` remains
speech-claim vocabulary with runtime key `material_balance_changes`. The next
named slice remains `Scene.Defense`; Material does not open Defense, Conversion,
Winning, Plan, Strategy, or Blunder.
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
data; RenderedLine is only the expression result of ExplanationPlan. Stage 7-6
opens only renderer baseline tests. Renderer output is no stronger than
ExplanationPlan; there is no new renderer wording, no new input, no public
route `200`, and no LLM narration. Stage 7 closeout confirms deterministic
renderer is closed as a template baseline. Stage 8 opens only 8A Mock narrator
and 8B Codex CLI prompt smoke test. Stage 8B Codex CLI prompt smoke may
receive renderedText, claimKey, strength, forbidden wording list, and the
instruction `Rephrase only. Do not add chess facts.` only. 8A Mock narrator may
receive ExplanationPlan and RenderedLine only. Production API validation
remains closed. Stage 8 must not read raw Verdict, Story, EngineCheck,
CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text, or source
rows directly. Fork-8 opens only deterministic renderer text for selected Fork
ExplanationPlan, using route, target, and secondaryTarget already lowered from
the selected Verdict. Fork-9 opens only LLM smoke for selected Fork
ExplanationPlan and RenderedLine; 8B may receive renderedText, claimKey,
strength, forbidden wording list, and the instruction `Rephrase only. Do not
add chess facts.` only. It does not open public/user-facing Fork LLM narration,
public route `200`, production API, pedagogy, material claims, wins-queen
claims, engine PV commentary, best-move explanation, or sibling tactic
families. Stages 9-11 below are a dependency map for product design; they are
not permission to implement those systems in this checkpoint.

Product north-star philosophy:

- Engine is the truth oracle.
- Backend is the proof and pedagogy system.
- LLM is the narrator.

The LLM does not judge chess. Engine truth, exact-board validation, legal
replay, proof sidecars, and `StoryTable` decide what can be said. Backend
policy owns proof, Story selection, arbitration, and pedagogy. The LLM may
phrase selected `Verdict` or `Explanation IR` data only; it must not decide,
prove, rank, repair, or invent chess meaning.

## Model Style

The model is an HCE-style deterministic chess scorer. It is not a trained
neural model. Input and output shapes are fixed so decisions can be logged,
audited, and later used for training.

The model replaces legacy selection authority, not renderer authority.

Code names must follow `ChessModelContract.md`: no type or module name may carry
version suffixes or developer-facing abstraction names for the new core model.
Schema numbers and shape sizes live in companion constants.

## Authority Consolidation

The architecture must reduce duplicated meaning, not multiply authority names.
Before adding a new type, module, row, or observation family, first test whether
the chess meaning already has a home and whether the existing owner can carry a
new field. When a file-related observation can live inside `FileFact`, a
piece-contact observation can live inside `PieceContact`, or a line-geometry
observation can live inside `LineFact`, prefer extending the existing owner over creating a second authority name.

Small implementation steps are good. Similar row families that each own a slice
of the same chess phenomenon are not. If a later `Story` proof would have to
choose between two similarly named inputs, the design has already split
authority and must be consolidated before proceeding.

## Live Authority Chain

The current chess authority path is:

`BoardMood` -> `Story` -> `StoryTable` -> `Verdict`

`BoardMood`, `Story`, `StoryTable`, and `Verdict` shapes, names, family counts,
and deletion rules are fixed by `ChessModelContract.md`.

Long term, the product backend may grow into this ordered structure:

`Board Truth` -> `Engine Truth` -> `Primitive Geometry` -> `Tactical/Strategic Story Birth` -> `Engine Check` -> `StoryTable Arbitration` -> `Pedagogical Policy` -> `Explanation IR` -> `LLM Narration` -> `Verifier`

That product structure does not open the current public surface. In this branch,
`BoardMood` supplies board facts and primitive geometry, `Story` is the first
public-meaning unit, `StoryTable` decides roles and lead eligibility, and
`Verdict` is the only language-neutral output visible downstream.

## Stage Opening Order

Stages open in this order only. Do not implement downstream product stages
before earlier authority stages are proven.

Only Stage 8 Prompt Smoke is active implementation authority now, and only for
8A Mock narrator over ExplanationPlan plus RenderedLine and 8B Codex CLI prompt
smoke over renderedText, claimKey, strength, forbidden wording list, and a
rephrase-only instruction. Stages 9-11 are dependency-map-only and stay closed
until their predecessor exits are proven.

Dependency order:

- Stage 1 depends on Stage 0.
- Stage 2 depends on Stage 1.
- Stage 3 depends on Stage 2.
- Stage 4 depends on Stage 3.
- Stage 5 depends on Stage 4.
- Stage 6 depends on Stage 5.
- Stage 7 depends on Stage 6.
- Stage 8 depends on Stage 7.
- Stage 9 depends on Stage 8.
- Stage 10 depends on Stage 9.
- Stage 11 depends on Stage 10.

| stage | opens | prerequisite proof | forbidden shortcut | exit criteria | proof-deficit coordinate |
|---|---|---|---|---|---|
| 0 Closed Kernel | Global no-go authority and fail-closed public boundary. | Live docs and tests prove missing Story tuple blocks public output. | Renderer, LLM, public route `200`, broad strategy, broad scalar re-entry, source/engine public truth, high numeric `Proof` lead. | Public chess meaning cannot escape without side, target, anchor, route, rival, required legal line, and same-root proof sidecar. | Missing tuple member or missing no-go authority. |
| 1 Board Facts | Small current-board observations. | Exact board identity, legal replay diagnostics, known/sane BoardFacts. | Plan speech, tactic verdict, conversion, counterplay, initiative, score/heat/pressure public meaning, engine eval as truth. | `BoardMood` creates no public chess meaning. | Missing board identity, producer proof, or observation identity. |
| 2 Story Proof | Code-enforced proof-bearing Story identity. | Stage 1 observations plus side, target, anchor, route, rival, required legal line, same-root proof. | Numeric proof score as public authority. | No Story Proof or incomplete Story Proof yields blocked/context only. | Missing sidecar field, stale sidecar, or wrong root. |
| 3 First Narrow Positive Story | Exactly one positive Story family at a time, with Material proof kernel as sub-proof only. | Complete Stage 2 sidecar, same-root legal replay, named writer, and family-specific proof. | Complete StoryProof opening a family, family piggybacking, renderer, LLM, public route `200`, or broad strategic family before tactical/material proof. | One narrow Hanging Story can speak with proof; close false positives stay silent. | Missing target, attack, legal capture, defender, recapture, positive bounded material result or SEE, same-board proof, named writer, or legal replay. |
| 4 Engine Check | Engine evidence attached to identified Story. | A Story already exists with identity and legal line. | Engine eval/PV/best move as explanation, strategy, or pedagogy by itself. | Engine supports, caps, or refutes the Story without speaking alone. | Missing same-board proof, checked move, engine line, reply line, eval before, eval after, depth or freshness, or fresh engine evidence. |
| 5 Story Order | Lead/support/context/blocked roles across existing Story rows, first scoped to `Tactic.Hanging`. | Multiple proof-backed Hanging Stories and Stage 4 EngineCheck diagnostics. | New Story creation, new positive family, engine eval as ranking truth, Board Facts direct public claim, `CaptureResult` public material story, pedagogy, Explanation IR, renderer, LLM, public route. | `StoryTable` deterministically decides Hanging roles without creating new chess meaning or a new public claim. | Missing role policy, blocker, cap, deterministic tie-break, or same Story row proof. |
| 6 Explanation Plan (Explanation IR) | Renderer-safe language-neutral speech bounds. | Selected Verdict data only. | Raw Board Facts, raw `BoardMood`, root atoms, capture evidence, engine sidecars, raw PV, proofFailures text, source rows, renderer wording, or LLM wording read by the plan. | The plan lists allowed claim, evidence, strength, role, support/context relation, and forbidden wording without writing sentences. | Missing allowed claim, evidence line, strength, role, relationship, or forbidden wording. |
| 7 Deterministic Renderer | Template baseline over Explanation IR. | Explanation IR is complete and renderer-safe. | Natural language that exceeds IR or repairs missing proof. | Deterministic text is no stronger than IR. | Missing template boundary, unsupported phrase, or unrepresented claim. |
| 8 LLM Narration | Prompt smoke over wording, tone, compression, and rephrase behavior. | Deterministic renderer baseline and safe IR. | New move, tactic, plan, causal explanation, evaluation, line, engine mention, production API, or claim strength. | Codex CLI prompt smoke checks whether LLM output verbalizes only allowed claims. | Unsupported chess fact, strengthened wording, invented line, or API path opened. |
| 9 Natural-Language Verifier | Claim extraction and rejection over rendered text. | IR and rendered text are available for comparison. | Accepting stronger natural language than IR. | Verifier rejects mate/win/only-move/tactic/plan/source claims without matching proof. | Extracted claim exceeds allowed claim, evidence, role, or strength. |
| 10 Pedagogical Policy | Level-aware teaching choices. | Stable causal arbitration and verified narration. | Pedagogy before causal truth. | Same Story can be explained differently by user level without changing chess truth. | Missing user level, learning objective, or allowed teaching transform. |
| 11 Personal Learning Loop | Motif memory, spaced repetition, custom practice, progress. | Stable Story taxonomy and pedagogical policy. | Personalization before reliable Story classification. | Repeated Story patterns can drive training without changing proof rules. | Missing motif identity, history window, review cadence, or progress evidence. |

### Stage 0 - Closed Kernel

Goal: prove that no public chess meaning escapes without required Story
bindings.

Required:

- public routes remain tombstones
- renderer remains closed
- `BoardMood` cut and split slots remain `0`/silent unless admitted by live law
- numeric `Proof` scores cannot create `Role.Lead`
- selected `Verdict` data cannot be rendered publicly

Completion standard: tests show that missing side, target, anchor, route,
rival, required legal line, or same-root proof sidecar blocks public output.

### Stage 1 - Board Facts

Goal: populate only small current-board observations.

Core sentence: Board state observes. Story proves.

Allowed facts are side, square, file, rank, piece, pawn, legal move, attack,
guard, `PieceContact`, `FileFact`, `LineFact`, defender, blocker, line, ray,
x-ray shape, pin-to-king geometry, open and semi-open file state, rook file
entry observation, unguarded non-pawn non-king piece observation, pawn lever,
pawn challenge, front blocker, current pawn structure observation, reachable
square, square guard map, king square, king-ring square, king-ring attack,
king-ring defender, legal king move, contact-check observation, non-public
king-line geometry, and promotion path.

Forbidden public meanings are plan speech, tactic verdict, conversion speech,
counterplay speech, initiative speech, score/heat/pressure speech, and engine
eval as truth. `BoardMood` facts are observations, not claims.

### Stage 2 - Story Proof

Goal: create the first real proof sidecar required for public Story birth.

A Story cannot lead unless it has side, target, anchor, route, rival, required
legal line, and same-root proof sidecar. A high numeric `Proof` score without
that sidecar remains blocked/context only.

Core sentence: Board Facts observes. Story Proof binds. Story may speak only
after proof.

### Stage 3 - First Narrow Positive Story

Goal: open exactly one narrow proof-backed Story family.

`StoryInteractionLaw.md` owns the Stage 3 charter. Architecture records the
dependency boundary: Stage 3 consumes Stage 2 StoryProof, adds backend Material
proof evidence, and opens only the named `Tactic.Hanging` writer. Stage 4
Engine Check may only attach internal engine evidence to existing
`Tactic.Hanging` Stories. Renderer, LLM, public route `200`, and every other
positive Story family remain closed.

The recommended implementation order is Material proof kernel,
`Tactic.Hanging`, Hanging negative corpus, `Tactic.Fork`, Fork negative corpus,
`Scene.Material`, then `Scene.Defense`. The first implementation scope is only
Material proof kernel, `Tactic.Hanging`, and Hanging negative corpus.

Material proof kernel is sub-proof, not public claim. It may compute legal
capture, captured piece, capturing piece, recapture map, material after the
immediate legal reply or a bounded line, and SEE-like exchange result.

`Tactic.Hanging` may speak only when target piece exists, target is attacked,
legal capture exists, target has no adequate defender or recapture, bounded
material result is positive, StoryProof is complete, same-board proof is
present, and the `Tactic.Hanging` writer is explicitly open.

Stage 3 first success means one narrow Hanging Story can speak with proof, and
all close false positives still stay silent.

The preferred first families after that scope are `Tactic.Hanging`,
`Tactic.Fork`, `Scene.Material`, and `Scene.Defense`, in that order. Broad
strategic families remain closed at this stage.

### Stage 4 - Engine Check

Goal: attach engine evidence only as proof for an already-identified Story.

`StoryInteractionLaw.md` owns the Stage 4 charter.

Story comes first. Engine checks, caps, or refutes. Engine never speaks alone.

Stage 4-1 opens only the internal evidence shape: `EngineCheck`, `EngineLine`,
and `EngineEval`. The evidence records same-board proof, checked move, engine
line, reply line, eval before, eval after, depth or freshness, and missing
evidence. It does not wire StoryTable consumption.

Stage 4-2 adds same-board and stale engine guards. Engine evidence must bind to
the same board, the same Story route, and the same legal line. Different-FEN
engine lines, route-mismatched engine lines, stale engine data, depth-missing
engine data, eval-only input without a Story, and PV-only input without a Story
remain diagnostic only.

Stage 4-3 attaches EngineCheck only to `Tactic.Hanging`. EngineCheck status is
exactly `Unknown`, `Supports`, `Caps`, or `Refutes`. `Supports` creates no new
claim and does not mean winning, best move, decisive, PV explanation, or public
truth. `Caps` keeps the Story available but forbids strong expression.
`Refutes` blocks the Hanging Story.

Engine data may support, cap, or refute a Story. It may not speak by itself.
Engine evidence may include legal replay, SEE or bounded material result,
engine line, mate proof, tablebase proof, or eval stability. Raw engine eval is
not public meaning.

Engine lines, mate/tablebase proof, SEE, and bounded material results are
truth-oracle evidence for backend proof. Raw engine numbers and engine text are
never public claim owners.

### Stage 5 - Story Order

Goal: allow `StoryTable` to decide which existing Story rows lead,
support, block, or stay context.

`StoryInteractionLaw.md` owns the Stage 5 charter.

The first scope is limited to existing `Tactic.Hanging` rows. StoryTable
may assign Lead, Support, Context, and Blocked roles with deterministic
ordering. Refuted Hanging is blocked, capped Hanging keeps only internal
strength limitation, support creates no new claim, and unknown engine evidence
creates no engine claim.

Stage 5 does not open a new Story writer, new positive family, pedagogical
advice, Explanation IR, renderer, LLM, public route, engine PV commentary, or
best-move explanation. It must not promote raw engine eval, Board Facts, or
`CaptureResult` into public meaning.

Stage 5-2 orders Story rows deterministically using role eligibility,
publicStrength, Story identity, writer presence, and blocked status. It does
not use raw engine eval, raw PV text, proofFailures text, Board Facts row
count, `CaptureResult` text, renderer wording, or input order.

Stage 5-3 tightens only close Hanging blockers and context relations:
EngineCheck refutation, missing proof, missing capture evidence, and missing
writer block Hanging-shaped rows, while Quiet, Source, and Opening remain
non-lead context around board-backed Hanging.

Stage 5-4 keeps Verdict diagnostics out of public numeric values:
proofFailures, EngineCheck status, and engineStrengthLimited remain internal
diagnostics, Verdict is not public text, and renderer, LLM, and public route
stay closed.

Stage 5 closeout confirms Story ordering only. Stage 6 may consume selected
Verdict data only; raw Board Facts, CaptureResult, EngineCheck, engine eval,
and PV text stay behind Stage 5's selected Verdict boundary.

### Stage 6 - Explanation Plan (Explanation IR)

Goal: convert selected Verdicts into renderer-safe language-neutral speech
bounds.

`StoryInteractionLaw.md` owns the Stage 6 charter.
Stage 6 is named `Explanation Plan`; `Explanation IR` is a permitted
parenthetical label for the data shape. Stage 6-0 is not natural-language work.
It may include allowed claim, allowed evidence line, allowed strength, role,
forbidden wording, and support/context relationship from selected Verdict data.
It must not read raw Board Facts, raw `BoardMood`, root atoms, source rows,
capture evidence, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures
text, renderer wording, or LLM wording directly.

Stage 6-0 does not open deterministic renderer, LLM narration, public route
`200`, user-facing prose, pedagogy, new Story families, or engine explanation.
Renderer and LLM layers remain downstream and may not turn observations into
advice.

Stage 6-1 adds the first internal `ExplanationPlan` shape. The first scope
accepts exactly one selected `Tactic.Hanging` Lead Verdict and produces role,
scene, tactic, side, target, anchor, route, allowedClaim, evidenceLine,
strength, forbiddenWording, and empty supportContextLinks. `allowedClaim` stays
a structured key such as `can_win_piece`; the first shape carries `bounded`
strength and forbidden wording, not public prose. It does not generate full
sentences, user-facing prose, engine-says wording, best-move wording, winning
wording, decisive wording, or public eval.

Stage 6-2 adds `Tactic.Hanging` allowed claim mapping only. Uncapped Lead
Verdict only may carry an allowed claim key. Support, Context, Blocked, and
engine-capped Verdicts do not create standalone public claims.
`engineStrengthLimited` suppresses claim keys, strengthens forbidden wording,
and creates no engine-approved, best-move, winning, decisive, forced,
no-counterplay, or blunder claim.

Stage 6-3 adds forbidden wording boundary only. Explanation Plan must carry the
default forbidden wording set for downstream renderer and LLM layers.
`Tactic.Hanging` remains bounded material tactic wording only.
`engineStrengthLimited` strengthens forbidden wording without carrying a claim.

Stage 6-4 adds Support and Context relation structure only. Lead only carries
an allowed claim only when uncapped. Support, Context, Blocked, and
engine-capped Verdicts create no standalone public claim. Blocked remains
debug-only relation structure, and proofFailures do not feed relation wording.

Stage 6-5 adds the selected Verdict input guard only. Explanation Plan accepts
selected Verdict only and creates no chess meaning beyond that Verdict. Raw
proof material, unselected Story, unselected Verdict, source rows, and
proofFailures text remain outside its input boundary.

Material-7 adds only `Scene.Material` ExplanationPlan lowering for selected
Verdicts. The allowed Material claim keys are `material_balance_changes`,
`line_leaves_material_gain`, and `exchange_leaves_side_ahead`; the first
emitted key is `material_balance_changes`. Support, Context, Blocked, capped,
and engine-refuted Material plans create no standalone public claim. Material
proof, `CaptureResult`, `ExchangeResult`, `EngineCheck`, `BoardFacts`, raw PV,
proofFailures, source rows, Material renderer text, LLM smoke, public route,
production API, winning, decisive, conversion, blunder, best-move, forced-win,
and no-counterplay remain closed.

Material-8 adds only deterministic renderer text for selected Material
ExplanationPlan. The route template is `After {route}, White comes out ahead
in material.` Renderer input remains `ExplanationPlan` only. It reads no raw
Verdict, Story, material proof, `CaptureResult`, `ExchangeResult`,
`EngineCheck`, `BoardFacts`, raw PV, proofFailures, or source rows, and it
does not open LLM smoke, public route, production API, winning, decisive,
blunder, forced, best-move, no-counterplay, engine-says, conversion, or
technically-winning wording.

Material-9 adds only LLM smoke for selected Material ExplanationPlan and
RenderedLine. 8B Material Codex CLI input is limited to renderedText,
claimKey, strength, forbidden wording, and `Rephrase only. Do not add chess
facts.` It reads no raw Verdict, Story, material proof, `CaptureResult`,
`ExchangeResult`, `EngineCheck`, `BoardFacts`, engine eval, raw PV,
proofFailures, or source rows, and it does not open public/user-facing LLM
narration, public route, production API, winning, decisive, forced, blunder,
best-move, conversion, or stronger claims.

Material Slice Closeout adds no runtime stage. It audits that `Scene.Material`
opened no Defense, Conversion, Winning, Plan, Strategy, or Blunder path; that
`CaptureResult`, `StoryProof`, `EngineCheck`, and `StoryTable` keep distinct
authority; that the existing proof home, Story writer, EngineCheck, StoryTable,
ExplanationPlan, Renderer, and LLM smoke skeleton was reused; and that
`Scene.Defense` remains the next named slice.

Stage 6 closeout confirms Explanation Plan only. It opens no renderer, LLM
narration, public route, user-facing prose, or pedagogy. Stage 7 deterministic
renderer may receive Explanation Plan only and must not read raw Verdict,
EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures
text, source rows, or raw engine evidence directly.

### Stage 7 - Deterministic Renderer

Goal: provide a template baseline over Explanation IR before LLM narration.

The deterministic renderer may receive Explanation Plan only. It must not read
raw Verdict, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV,
proofFailures text, source rows, or raw engine evidence directly. It must not
create chess meaning, repair missing proof, upgrade claim strength, infer new
tactics, infer new plans, or reinterpret source or engine context as truth. Its
text must be no stronger than the IR.

Stage 7-0 fixes the Deterministic Renderer charter only. It allows
`ExplanationPlan` only input, deterministic template, `Tactic.Hanging` bounded
claim phrasing, forbidden wording check, no LLM, and no public route. It keeps
raw Verdict, Story, Board Facts, CaptureResult, EngineCheck, EngineEval,
EngineLine, raw PV, proofFailures text, source rows, user-level pedagogy,
engine PV explanation, best-move explanation, engine-says wording, winning,
decisive, forced, blunder, and free-piece wording closed. No public route
`200` opens here.

Stage 7-1 adds only the Renderer input guard. Renderer receives
`ExplanationPlan` only. It exposes no raw Verdict, Story, BoardFacts,
BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV,
proofFailures, or source-row input, and it cannot create text without an
`ExplanationPlan`.

Stage 7-2 adds only the minimal `Tactic.Hanging` template for `can_win_piece`.
It requires Lead role, `Bounded` strength, non-debug plan, route, target,
evidenceLine, and forbidden wording. The first text must not exceed the
ExplanationPlan claim key or evidenceLine.

Stage 7-3 adds only forbidden wording enforcement. Renderer output must not
violate `ExplanationPlan.forbiddenWording`. `win material` wording is allowed
only when `allowedClaim` is `CanWinPiece`; `winning position` remains
forbidden; engine-strength-limited plans must fail strong wording output.

Stage 7-4 adds only the no-standalone-text boundary. Renderer phrases only
Lead plans with an allowed claim. Support, Context, Blocked, capped no-claim,
and engine-refuted relation plans produce no text.

Stage 7-5 adds only the RenderedLine shape. `RenderedLine` carries text, claim
key, strength, and forbidden-check result only. `RenderedLine` owns no chess
meaning, proof, or engine data; RenderedLine is only the expression result of
ExplanationPlan.

Stage 7-6 adds only renderer baseline tests. Renderer output is no stronger
than ExplanationPlan. It opens no new renderer wording, no new input, no route,
no public route `200`, and no LLM narration.

Stage 7 closeout confirms deterministic renderer is closed as a template
baseline. It opens no LLM narration, public route `200`, pedagogy, new Story
family, new renderer input, or new markdown authority. Stage 8 LLM Narration
may receive deterministic text and ExplanationPlan only.

### Stage 8 - LLM Narration

Goal: smoke-test LLM narration behavior over Explanation IR and deterministic
renderer baseline.

Stage 8 opens only 8A Mock narrator and 8B Codex CLI prompt smoke test. 8A may
receive ExplanationPlan and RenderedLine only. 8B may receive renderedText,
claimKey, strength, forbidden wording list, and the instruction `Rephrase only.
Do not add chess facts.` only. LLM narration may not add a new move, tactic,
plan, causal explanation, evaluation, line, engine mention, or claim strength.
LLM before Explanation IR is forbidden. Stage 8 must not read raw Verdict,
Story, EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV,
proofFailures text, or source rows directly. Production API validation, public
route `200`, pedagogy, and natural-language verifier remain closed.

### Stage 9 - Natural-Language Verifier

Goal: check that rendered text does not exceed Verdict or Explanation IR.

The verifier rejects forced mate without mate proof, winning claims without
conversion proof, only-move claims without benchmark proof, tactic claims
without legal line, plan claims without route/anchor/proof, and source/opening
claims that override board truth.

### Stage 10 - Pedagogical Policy

Goal: choose level-aware teaching emphasis over proof-backed, causally
arbitrated Verdicts.

Pedagogy is backend policy over selected proof-backed `Verdict` data.
Pedagogical policy depends on causal arbitration and verifier-safe language. It
may adapt explanation level, but it must not change chess truth or promote
context/observation rows into advice.

### Stage 11 - Personal Learning Loop

Goal: use stable Story taxonomy for motif memory, spaced repetition, custom
practice, next-game focus, and progress tracking.

Personalization depends on stable Story taxonomy and pedagogical policy. It
must not alter proof requirements or treat repeated observations as public
claims.

## Closed Boundaries

The architecture target does not open the public path. Public surface behavior,
renderer behavior, `BoardMood` Sxxx expansion/re-entry, and `Story` proof
writers stay closed until prerequisite laws and docs tests exist.

`/api/commentary/render` and `/internal/commentary/render-local-probe` are
registered only as fail-closed tombstones. No runtime FEN, environment switch,
frontend mock, or renderer payload opens those routes.

At this checkpoint, `StoryTable` lead decisions are closed. Numeric `Proof`
scores may rank blocked/context `Verdict` rows only; they cannot set
`leadAllowed=true` or produce `Role.Lead` until the full side, target, anchor,
route, rival, required legal line, and same-root proof sidecar tuple exists.

## BoardMood

`BoardMood` is the shared middle board representation:

- `48` bit slots
- `256` scalar slots
- `3,328` total values

B00..B45 are packed root transport words, B46..B47 are legal-destination
summaries, and S000..S255 are named scalar slots. Missing producer inputs are
zero-filled only through the explicit slot contract; there is no unnamed
expansion region in the live `BoardMood` contract.

`BoardMood` must remain an observation layer. It must not create broad scalar
claims for pressure, conversion, counterplay, strategy, or plan labels. Split
or cut ideas may re-enter only as smaller current-board observations admitted
by a live law; they must not be revived under old slot names as public
authority.

## Root Transport

`BoardFacts.fromFen` is the strict runtime entrypoint into `BoardMood`. It uses
the strict `RootExtractor.fromFenWithPositionFailClosed` helper, derives legal,
material, control, and pawn facts from the same exact board, and returns
`Either[String, BoardFacts]`.

`BoardFacts.fromPosition` is internal/test-only. It requires an explicit
positive fullmove number from the internal caller, may support parity checks,
and is not an external escape hatch around strict raw-FEN validation.

B46/B47 and `Moves.lines` remain diagnostic summaries. They do not prove
origin, route, castling, en-passant, promotion, tactic, line, or public claim
legality.

## Story

`Story` is the public chess unit. It carries:

- one public `Scene`
- optional `Plan`
- optional `Tactic`
- compact identity: `side`, `target`, `anchor`, `route`, and `rival`
- `Proof` scores using the exact names from `ChessModelContract.md`

Story owns identity. StoryProof owns proof and missing evidence. Verdict
carries the result. `StoryProof` checks whether the identity already carried by
`Story` is bound by legal line and same-board proof; it must not carry a second
copy of side, target, anchor, route, or rival.

`proofFailures` is internal diagnostic data only. It may explain why the kernel
cannot speak during tests or debugging, but it is not public payload, renderer
input, or LLM input.

`Story.values` is exactly `160` values. It one-hot encodes public families and
stores proof scores in the proof segment.

The current Fork implementation target opens deterministic renderer text only
after the selected Verdict has been lowered to `ExplanationPlan`. A Story
cannot lead until it has concrete side, target, anchor, route, rival, required
legal line, same-root proof sidecar, a named positive Story writer, and
family-specific proof. Numeric proof scores alone are not authority because
they can be forged without the bound sidecar tuple.

## StoryTable

`StoryTable` orders stories into at most `8` verdicts. Lead permission depends
on public strength, truth, counterplay risk, quiet/source restrictions, and
owner-aware tactical interaction.

A plan lead is blocked only by an opposing `Tactic` or `Blunder` story at the
public floor. Source stories cannot lead over a non-source board-backed story at
the public floor. Opening stories are context-only and cannot lead over a
board-backed story at the public floor.

## Verdict

`Verdict` is the language-neutral result handed to downstream display work. It
stores story reference, rank, role, lead permission, and strength.

`Verdict.values` is exactly `96` values.
