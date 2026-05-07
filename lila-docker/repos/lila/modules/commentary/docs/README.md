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
Stage 1 Board Facts, Stage 2 Story Proof, and Stage 3 first narrow positive
Story are prerequisites. Stage 3 remains open only for Material proof kernel,
`Tactic.Hanging`, Hanging negative corpus, the narrow `Tactic.Fork` vertical
slice, the narrow `Scene.Material` writer, and Material negative corpus.
Stage 4 is named `Engine Check`. Stage 4 opens only internal EngineCheck
evidence, same-board and stale guards, `Tactic.Hanging` attachment, narrow `Tactic.Fork`
attachment, narrow `Scene.Material` EngineCheck reuse, false-positive corpus, and
conservative StoryTable diagnostics for existing `Tactic.Hanging`, narrow
`Tactic.Fork`, and single proof-backed `Scene.Material` Stories. Stage 5 is
named `Story Order` and opens only role ordering for existing
`Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows;
Material-3 adds only single-row StoryTable admission for proof-backed
`Scene.Material`; Material-4 adds only the Material negative corpus;
Material-5 reuses only existing `EngineCheck` for `Scene.Material`;
Material-6 adds only StoryTable integration for existing Hanging, Fork, and
Material rows. Stage 5-1
assigns Lead, Support, Context, and
Blocked roles inside that slice. Stage 5-2 fixes deterministic ordering inputs
for those StoryTable rows. Stage 5-3 tightens close blockers and context
relations for those rows. Stage 5-4 keeps Verdict diagnostics out of public
numeric values and downstream public surfaces. Stage 5 closeout confirmed
Story ordering only and selected-Verdict handoff only. Stage 6-0 opens only
the Explanation Plan
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
families. Stages 9-11 remain a dependency map, not permission to open those
systems in this branch checkpoint.

`StoryInteractionLaw.md` owns the Stage 3 charter. This README only summarizes
the current scope: backend Material proof evidence, the named
`Tactic.Hanging` writer, the narrow `Tactic.Fork` proof/writer vertical slice,
and the narrow `Scene.Material` writer are open; other positive families,
public/user-facing Fork LLM
narration, and public route `200` are still closed.

`StoryInteractionLaw.md` owns the Stage 4 charter. This README only summarizes
the current scope: Story comes first, and engine evidence may check, cap, or
refute only an existing `Tactic.Hanging`, narrow `Tactic.Fork`, or narrow
`Scene.Material` Story. Engine eval, engine line, reply line, and checked move
data cannot create a Story, rank a Story, write a `Verdict`, feed a renderer,
feed an LLM, or become public truth.

`StoryInteractionLaw.md` owns the Stage 5 charter. This README only summarizes
the current scope: StoryTable may assign roles among existing
`Tactic.Hanging` Story rows and existing narrow `Tactic.Fork` Story rows.
Material-3 adds only single-row StoryTable admission for proof-backed
`Scene.Material`; Material-4 adds only the Material negative corpus;
Material-5 reuses only existing `EngineCheck` for `Scene.Material`;
Material-6 adds only StoryTable integration for existing Hanging, Fork, and
Material rows. It does
not create Stories, open another positive family, or turn engine eval, Board
Facts, `CaptureResult`, or MultiTargetProof into direct public claims.
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
This README summarizes Material-7 only: selected `Scene.Material` Verdicts may
lower into ExplanationPlan data with bounded strength, forbidden wording, and
the first emitted `material_balance_changes` claim key for uncapped Lead only.
Support, Context, Blocked, capped, and engine-refuted Material plans create no
standalone public claim. Material proof, `CaptureResult`, `ExchangeResult`,
`EngineCheck`, `BoardFacts`, raw PV, proofFailures, source rows, Material
renderer text, LLM smoke, public route `200`, production API, winning,
decisive, conversion, blunder, best-move, forced-win, and no-counterplay remain
closed.
This README summarizes Material-8 only: deterministic renderer text may phrase
selected Material ExplanationPlan through `ExplanationPlan` input only. The
first route template is `After {route}, White comes out ahead in material.`
The text must be no stronger than the Material ExplanationPlan and must not say
winning, decisive, blunder, forced, best move, no counterplay, engine says,
conversion, or technically winning.
This README summarizes Stage 6 closeout only: Stage 6 closes with Explanation
Plan only, without renderer, LLM, public route, user-facing prose, or pedagogy.
Blocked, Support, Context, engine-capped, and engine-refuted Verdicts create
no allowed claim, and Stage 7 deterministic renderer may receive Explanation
Plan only.

`StoryInteractionLaw.md` owns the Stage 7-0 charter. This README only
summarizes the current scope: Stage 7 is named `Deterministic Renderer`, and
Stage 7-0 opens only `ExplanationPlan` input, deterministic template,
`Tactic.Hanging` bounded claim phrasing, forbidden wording check, no LLM, and
no public route. It does not open raw Verdict, Story, Board Facts,
CaptureResult, EngineCheck, EngineEval, EngineLine, raw PV, proofFailures
text, source rows, user-level pedagogy, engine PV explanation, best-move
explanation, engine-says wording, winning, decisive, forced, blunder, or
free-piece wording.

`StoryInteractionLaw.md` owns the Stage 7-1 input guard. This README only
summarizes the current scope: Stage 7-1 opens only the Renderer input guard.
Renderer receives `ExplanationPlan` only, exposes no raw Verdict, Story,
BoardFacts, BoardMood, CaptureResult, EngineCheck, EngineEval, EngineLine, raw
PV, proofFailures, or source-row input, and cannot create a sentence without
an `ExplanationPlan`.

`StoryInteractionLaw.md` owns the Stage 7-2 template. This README only
summarizes the current scope: Stage 7-2 opens only the minimal
`Tactic.Hanging` template for the `can_win_piece` claim key, with Lead,
bounded strength, non-debug, route, target, evidenceLine, and forbidden
wording present. The text must not exceed the ExplanationPlan claim key or
evidenceLine.

`StoryInteractionLaw.md` owns the Stage 7-3 forbidden wording enforcement.
This README only summarizes the current scope: Stage 7-3 opens only forbidden
wording enforcement. Renderer output must not violate
`ExplanationPlan.forbiddenWording`; `win material` wording is allowed only
when `allowedClaim` is `CanWinPiece`, `winning position` remains forbidden,
engine-strength-limited plans fail strong wording output, and plans with no
allowedClaim or debugOnly true produce no output.

`StoryInteractionLaw.md` owns the Stage 7-4 no-standalone-text boundary. This
README only summarizes the current scope: Stage 7-4 opens only the
no-standalone-text boundary. Renderer phrases only Lead plans with an allowed
claim, and Support, Context, Blocked, capped no-claim, and engine-refuted
relation plans produce no text.

`StoryInteractionLaw.md` owns the Stage 7-5 RenderedLine shape. This README
only summarizes the current scope: Stage 7-5 opens only the RenderedLine
shape. `RenderedLine` owns no chess meaning, proof, or engine data, and
RenderedLine is only the expression result of ExplanationPlan. It must not
include CaptureResult, EngineCheck, BoardFacts, proofFailures, raw route
analysis, or source-row material.

`StoryInteractionLaw.md` owns the Stage 7-6 renderer baseline tests. This
README only summarizes the current scope: Stage 7-6 opens only renderer
baseline tests. Renderer output is no stronger than ExplanationPlan, and there
is no new renderer wording, no new input, no public route `200`, and no LLM
narration.

`StoryInteractionLaw.md` owns the Stage 7 closeout pass. This README only
summarizes the current scope: deterministic renderer is closed as a template
baseline. Stage 8 LLM Narration may receive deterministic text and
ExplanationPlan only, and Stage 8 must not read raw Verdict, Story,
EngineCheck, CaptureResult, Board Facts, BoardMood, raw PV, proofFailures text,
or source rows directly.

`StoryInteractionLaw.md` owns the Stage 8 prompt smoke. This README only
summarizes the current scope: Stage 8 opens only 8A Mock narrator and 8B Codex
CLI prompt smoke test. LLM narration behavior smoke may rephrase deterministic
text from RenderedLine only in 8B, while 8A receives ExplanationPlan and
RenderedLine only. It must respect the forbidden wording checker, must not
strengthen deterministic text, and must not add a move, line, tactic, plan,
engine mention, or chess meaning absent from ExplanationPlan. Production API
validation remains closed.

`StoryInteractionLaw.md` owns Fork-8 Deterministic Renderer for Fork. This
README only summarizes the current scope: Fork deterministic renderer text may
phrase only a selected Fork ExplanationPlan with route, target,
secondaryTarget, bounded strength, `forks_two_targets`, and forbidden wording
present. It must not read raw Verdict, Story, MultiTargetProof, EngineCheck,
CaptureResult, BoardFacts, BoardMood, raw PV, proofFailures, or source rows,
and it does not open Fork LLM smoke itself, public route `200`, production API,
material claims, wins-queen claims, engine-says wording, best-move wording, or
sibling tactic families.

`StoryInteractionLaw.md` owns Fork-9 LLM Smoke for Fork. This README only
summarizes the current scope: Fork LLM smoke may receive selected Fork
ExplanationPlan and RenderedLine. 8B may receive renderedText, claimKey,
strength, forbidden wording list, and the instruction `Rephrase only. Do not
add chess facts.` only. It must not read raw Verdict, Story, MultiTargetProof,
EngineCheck, CaptureResult, BoardFacts, BoardMood, EngineEval, EngineLine,
engine eval, raw PV, proofFailures, or source rows. Production API,
public/user-facing LLM narration, public route `200`, pedagogy, engine PV
commentary, best-move explanation, material-win wording, wins-queen wording,
and sibling tactic families remain closed.

`StoryInteractionLaw.md` owns the Fork Slice Closeout Pass. This README only
summarizes the current scope: Fork closeout opens no sibling tactic,
`Scene.Defense`, Plan, Strategy, public route, production API, or
public/user-facing LLM narration. Fork does not open `Scene.Material` by
implication; Material-3 opens only the narrow named `Scene.Material` writer.
MultiTargetProof does not replace CaptureResult, StoryProof, EngineCheck, or
StoryTable.

`StoryInteractionLaw.md` owns Material-0 and Material-1. This README only
summarizes the current scope: Material-0 fixes `Scene.Material` as a scene
Story label, not a tactic and not proof. Material-1 reuses `CaptureResult` for
the first simple capture and immediate bounded recapture proof home decision.
`material_change` is the first Material speech claim key, but Material writer,
StoryTable role rules, ExplanationPlan mapping, renderer text, LLM smoke,
public route `200`, production API, winning, decisive, blunder, conversion,
best-move, forced, no-counterplay, engine-says, and full-evaluation claims
remain closed.
`StoryInteractionLaw.md` owns Material-2. This README only summarizes that
Material-2 extends `CaptureResult` with captured pieces and bounded exchange
sequence proof fields. It calculates material result as proof only and does
not create a public Story, sentence, Material writer, ExplanationPlan mapping,
renderer text, LLM input, winning-position, decisive-advantage, conversion,
blunder, best-move, or forced-line claim.
`StoryInteractionLaw.md` owns Material-3. This README only summarizes that
Material-3 opens `StoryWriter.SceneMaterial` as the named Material writer over
complete `CaptureResult` proof, complete StoryProof, same-board proof, legal
line, known material result, and no EngineCheck Refutes result. It opens no
ExplanationPlan mapping, renderer text, LLM input, public route `200`,
production API, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.
`StoryInteractionLaw.md` owns Material-4. This README only summarizes that
Material-4 adds the Material negative corpus. Legal-line missing, same-board
missing, erased material result, unclear exchange result, king target, zero
material result, EngineCheck Refutes, incomplete StoryProof, incomplete
material proof, tactic-writer duplication, Hanging/Fork auto-duplication, and
high Proof score only produce no Lead or Blocked. It opens no
ExplanationPlan mapping, renderer text, LLM input, public route `200`,
production API, winning, decisive, blunder, conversion, best-move, forced,
no-counterplay, engine-says, or full-evaluation claims.
`StoryInteractionLaw.md` owns Material-5. This README only summarizes that
Material-5 reuses existing `EngineCheck` for `Scene.Material`. It may support,
cap, or refute an already existing Material Story when same-board proof, same
Story route, same legal line, and fresh or depth evidence are present. It
creates no Material Story, public engine truth, PV explanation, best-move
explanation, winning claim, or `MaterialEngineCheck` duplicate type.

`StoryInteractionLaw.md` owns Material-6. This README only summarizes that
Material-6 adds StoryTable integration for existing Hanging, Fork, and Material
rows. Refuted, incomplete, writerless, or material-proof-missing Material rows
are Blocked; Hanging, Fork, and Material can compete for Lead; Support and
Context remain non-sentence roles. StoryTable creates no Material Story, raw
engine eval does not rank Material, material proof text and renderer wording
remain non-public, and Material does not open conversion or winning.

`StoryInteractionLaw.md` owns Material-7. This README only summarizes that
Material-7 opens ExplanationPlan mapping for selected `Scene.Material`
Verdicts. Allowed Material claim keys are `material_balance_changes`,
`line_leaves_material_gain`, and `exchange_leaves_side_ahead`; the first
emitted key is `material_balance_changes`. It reads no material proof,
`CaptureResult`, `ExchangeResult`, `EngineCheck`, `BoardFacts`, raw PV,
proofFailures, or source row input, and it opens no Material renderer text,
LLM smoke, public route, production API, winning, decisive, conversion,
blunder, best-move, forced-win, or no-counterplay claim.

`StoryInteractionLaw.md` owns Material-8. This README only summarizes that
Material-8 opens deterministic renderer text for selected `Scene.Material`
ExplanationPlan. Renderer input remains `ExplanationPlan` only, the first
route template is `After {route}, White comes out ahead in material.`, and the
text must not exceed the Material ExplanationPlan or use winning, decisive,
blunder, forced, best-move, no-counterplay, engine-says, conversion, or
technically-winning wording.

`StoryInteractionLaw.md` owns Material-9. This README only summarizes that
Material-9 opens LLM smoke for selected Material ExplanationPlan and
RenderedLine. 8B receives only renderedText, claimKey, strength, forbidden
wording, and `Rephrase only. Do not add chess facts.` Material LLM smoke must
reject new moves, new lines, new tactics, new plans, engine mentions, winning,
decisive, forced, blunder, best-move, conversion, and stronger claims.

`StoryInteractionLaw.md` owns Material Slice Closeout. This README only
summarizes that the closeout opened no sibling family or stronger result path.
`CaptureResult`, `StoryProof`, `EngineCheck`, `StoryTable`, `Scene.Material`,
and `material_change` keep distinct homes, and `Scene.Defense` remains the
next named slice. Material reuses the existing proof home, Story writer,
EngineCheck, StoryTable, ExplanationPlan, Renderer, and LLM smoke skeleton
before adding any new structure.

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
  evidence, but only the named `Tactic.Hanging` writer, narrow `Tactic.Fork`
  writer, and narrow `Scene.Material` writer may open positive Story families
  in this checkpoint. Numeric `Proof` scores may rank blocked/context
  `Verdict` rows only. They cannot set `leadAllowed=true` or produce
  `Role.Lead` without the named writer, complete StoryProof, same-board proof,
  and family proof: `CaptureResult` for Hanging or Material, or
  `MultiTargetProof` for Fork.
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
- Stage 4-3 status no-go: EngineCheck first attached only to
  `Tactic.Hanging`; Fork-5 reuses the same sidecar for existing narrow
  `Tactic.Fork` Stories. Status is exactly `Unknown`, `Supports`, `Caps`, or
  `Refutes`. `Supports` cannot become winning, best-move, decisive, PV
  explanation, or public truth; `Caps` forbids strong expression; `Refutes`
  blocks the Story.
- Stage 7-0 Deterministic Renderer no-go: the current scope may fix
  `ExplanationPlan` only input, deterministic template, `Tactic.Hanging`
  bounded claim phrasing, and forbidden wording checks only. It does not open
  `Tactic.Fork`, Material renderer wording, `Scene.Defense`, Plan, Strategy,
  pedagogical advice, LLM, public route `200`, engine PV commentary,
  best-move explanation, engine explanation, or raw proof input.
- Stage 7-1 Renderer Input Guard no-go: Renderer may accept
  `ExplanationPlan` only. It must not expose `fromVerdict`, `fromStory`,
  `fromBoardFacts`, `fromEngineCheck`, or any raw proof-material renderer
  entrypoint.
- Stage 7-2 Minimal Tactic.Hanging Template no-go: Renderer may render only
  the `can_win_piece` claim key for a Lead, bounded, non-debug
  `Tactic.Hanging` ExplanationPlan with route, target, evidenceLine, and
  forbidden wording. Other claim keys, stronger wording, raw proof input, LLM,
  and public route `200` remain closed.
- Stage 7-3 Forbidden Wording Enforcement no-go: Renderer must reject
  deterministic text that violates `ExplanationPlan.forbiddenWording`. `win
  material` wording is allowed only when `allowedClaim` is `CanWinPiece`;
  winning-position, best-move, engine-says, strategic, conversion, mate-net,
  and other forbidden wording remains closed.
- Stage 7-4 No Text for Support / Context / Blocked no-go: Renderer may phrase
  only Lead plans with an allowed claim. Support, Context, Blocked, debug-only,
  capped no-claim, and engine-refuted relation plans produce no text.
- Stage 7-5 Rendered Line Shape no-go: `RenderedLine` may carry text, claim
  key, strength, and forbidden-check result only. It must not carry
  CaptureResult, EngineCheck, BoardFacts, proofFailures, raw route analysis,
  source rows, proof ownership, engine data ownership, or new chess meaning.
- Stage 7-6 Renderer Baseline Tests no-go: baseline tests may prove renderer
  output is no stronger than ExplanationPlan. They must not add renderer
  wording, new inputs, relation narration, public route `200`, or LLM
  narration.
- Stage 7 Closeout Pass no-go: closeout may audit only that deterministic
  renderer is the sole Stage 7 opening. It must not open LLM narration, public
  route `200`, pedagogy, new Story families, new renderer inputs, or a new
  markdown authority file.
- Stage 8 Prompt Smoke no-go: only 8A Mock narrator and 8B Codex CLI prompt
  smoke test are open. 8A receives ExplanationPlan and RenderedLine only. 8B
  receives renderedText, claimKey, strength, forbidden wording list, and the
  instruction `Rephrase only. Do not add chess facts.` only. Production API
  validation, public route `200`, pedagogy, natural-language verifier, raw
  proof input, and new chess meaning remain closed.
- Renderer boundary no-go: deterministic templates may phrase
  `ExplanationPlan` only. LLM renderers remain closed, public route `200`
  remains closed, and no renderer may create, repair, or upgrade chess
  meaning.
- Old-doc no-go: `CommentaryCoreSSOT.md`, `SemanticModelArchitecture.md`,
  `LegacyArchiveIndex.md`, and `CommentaryFrontendBridgeContract.md` must not
  return as root authority.
- Forbidden-name no-go: new core model, type, module, or docs-authority names
  must not use `Semantic`, non-pawn `Candidate`, `Certification`, `Object`,
  `Delta`, `Selector`, `Pipeline`, `Gate`, `ScoreVector`, or version suffixes.
- Stage order no-go: implementation opens only
  `observation` -> `proof sidecar` -> `Story` -> `Verdict` ->
  `Explanation IR` -> Renderer -> LLM narration smoke. Downstream product
  stages stay closed until the earlier authority stage is proven.
- LLM no-go: Engine truth, exact-board validation, legal replay, proof
  sidecars, and `StoryTable` decide what can be said. LLM narration behavior
  smoke may rephrase only deterministic text bounded by ExplanationPlan and
  must remain closed and must not judge chess outside that prompt smoke scope.

Current implementation blockers are documented, not excused: Stage 2 enforces
the full public-output tuple before lead candidacy, and Stage 3 opens only the
named `Tactic.Hanging` writer over positive `CaptureResult`. Stage 4-1 records
engine evidence shape only; Stage 5 role ordering consumes only existing
`Tactic.Hanging` Story rows; Stage 6 closeout confirms Explanation Plan only,
Stage 7-0 opens only the Deterministic Renderer charter, Stage 7-1 opens only
the Renderer input guard over `ExplanationPlan`, and Stage 7-2 opens only the
minimal `Tactic.Hanging` template. Stage 7-3 opens only forbidden wording
enforcement. Stage 7-4 opens only the no-standalone-text boundary. Stage 7-5
opens only the RenderedLine shape. Stage 7-6 opens only renderer baseline
tests. Stage 7 closeout confirms deterministic renderer is closed as a
template baseline. Stage 8 opens only 8A Mock narrator and 8B Codex CLI prompt
smoke test.
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
