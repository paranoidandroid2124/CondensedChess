# Commentary Renderer Contract

This document freezes the first renderer contract for the current worktree.

It does not open API wiring, frontend wiring, live source adapters, raw engine
intake, or model-authored prose generation.

## Authority Boundary

The renderer ingress consumes `CommentaryPlan` only, then lowers it to
`PublicCommentaryPlan` before public blocks are emitted.

Short form: `CommentaryPlan only` at ingress; `PublicCommentaryPlan` only at
the final render boundary.

It must not consume:

- raw `CommentaryOutline` selector claims except through `CommentaryPlan`
- `RuntimeEnginePacket`
- `EngineEvidencePacket`
- raw source rows as truth
- opening source fixture rows or `OpeningContextCandidate` JSON directly
- motif source fixture rows, detector rows, or source tags directly
- endgame-study fixture rows, placement/relation evidence rows, or outcome
  fields directly
- retrieval example rows, snippets, player/event/result metadata, or raw
  citation rows directly

The renderer is a surface mapper. It does not select, rank, admit, suppress,
revive, reinterpret evidence, promote source context, merge opening source
vectors, upgrade wording strength, or create chess meaning.

## Output Shape

The stable contract names are:

- `CommentaryRenderer`
- `CommentaryRender`
- `RenderBlock`
- `RenderRole`
- `RenderStatus`
- `RenderLineRole`
- `RenderText`
- `RenderEvidenceRef`
- `RenderBoundary`
- `RenderSuppression`
- `RenderWording`
- `PublicClaim`
- `PublicClaimPredicate`
- `PhraseCapability`
- `PublicSurfaceTemplate`
- `PublicPhrase`
- `PublicCommentaryPlan`
- `RenderVariationEvidence`
- `RenderVariationMove`
- `BookAnnotationPlanner`
- `BookAnnotationPlan`
- `BookAnnotationUnit`
- `BookAnnotationBoundary`
- `BookAnnotationWordingRules`
- `LineCommentaryPlanner`
- `LineCommentaryPlan`
- `LineNote`
- `LineNoteKind`
- `LineNoteMeaning`
- `LineSupport`
- `LineCaution`
- `LineCommentaryDetail`
- `LineCommentaryBoundary`
- `EnglishLineCommentary`
- `EnglishLineComment`
- `EnglishLineCommentaryWriter`

`CommentaryRender` is structured-block-first. Text is a leaf field on a block,
not the contract itself.

The contracted render statuses are:

- `rendered`
- `contextOnly`
- `noCommentary`

`CommentaryRendererContract.publicPlan` lowers a `CommentaryPlan` into
`PublicCommentaryPlan`. `CommentaryRendererContract.render` remains the lower
deterministic structured contract and maps `PublicClaim` objects into public
blocks without consuming `PublicClaim.text` as prose. `CommentaryRenderer.render`
may pass a `PublicPhrase` created from the closed English line-comment writer
into `PublicSurfaceTemplate`; only that phrase-token handoff can populate a
primary block's public text, and only when the matching `PublicClaim` carries
`PhraseCapability.allowsLineCommentary` plus the `line_commentary` predicate.

When no safe English line comment exists, `RenderText.publicText` remains
empty. Structured roles such as `Primary`, `Support`, `Context`, and
`Contrast` stay available only as `RenderRole` data, not as fallback public
move-explanation text. The renderer does not emit broad chess narration,
role-label prose, or generated/model-authored prose.

## Section Mapping

The mapping is plan-preserving:

- `CommentaryPlan -> PublicCommentaryPlan`
- selected public claim data -> `PublicClaim`
- `plan.main -> RenderRole.Primary`
- `plan.support -> RenderRole.Supporting`
- `plan.context -> RenderRole.Context`
- `plan.contrast -> RenderRole.Contrast`
- `plan.blocked -> RenderSuppression(public = false)`
- eligible public refs from `plan.evidence -> RenderEvidenceRef`
- selected public-safe line proofs from
  `plan.variationEvidence -> RenderVariationEvidence`
- safe primary line comments from
  `plan.annotationSelections -> BookAnnotationPlanner -> LineCommentaryPlanner -> EnglishLineCommentaryWriter -> PublicPhrase -> PublicSurfaceTemplate -> RenderText.publicText`
- `plan.wordingRules.maxStrength -> RenderWording.maxStrength`

The renderer must preserve section order:

1. primary
2. supporting
3. context
4. contrast

It must not rerank, deduplicate, or relabel a claim into a stronger role.

Public `RenderEvidenceRef` output is stricter than raw `plan.evidence`: a ref
must already be present in `plan.evidence` and must also be directly referenced
by an unblocked public selected claim. This prevents plan-wide or malformed
evidence from becoming public without selected-claim ownership.
`EngineCertification` refs are renderable only for public Certification
claims with the same owner, anchor, route, and scope, a matching bounded
`Certification` ref, and a same-binding typed lower board reason (`Root`,
`Witness`, `Object`, or `Delta`). A generic exact-board carrier proves board
identity only and does not authorize the engine provenance ref to render.

Public `RenderVariationEvidence` output is similarly stricter than raw
`plan.variationEvidence`: a proof must be public-safe, must be bound to an
unblocked rendered claim, and must carry only the public subset frozen in
[VariationEvidenceContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/VariationEvidenceContract.md).
The lower `PreparedVariationEvidence` may retain internal
`VariationEvidenceRole` values and its internal `proves` token, but the
renderer public payload must not expose either field directly.
`RenderVariationEvidence.role` is the renderer-public `RenderLineRole` with
only these keys: `resource`, `caution`, `hold`, `conversion`, `pressure`, and
`simplification`. Internal `defender_resource` maps to `resource`; internal
`failed_tempting_move`, `premature_move`, and `release_risk` map to
`caution`; internal `persistence` maps to `pressure`; the other supported
roles keep their chess-friendly public names. The renderer must not serialize
`PreparedVariationDebug`, the prepared `proves` token, start FEN, UCI PV,
provenance refs, replay/freshness boundary fields, depth, MultiPV, engine
fingerprints, cache keys, branch ids, or raw move UCI. It may serialize only
SAN move/line fields, restrained test result, proof purpose, wording cap, and
surface allowance as structured public data. Public `RenderVariationEvidence`
requires `surfaceAllowance = public_line`; `boundary_only` proofs remain
non-rendered line evidence in this scaffold. It must filter `internal_only`,
raw-engine-provenance, source-context-provenance, unbounded, stale, illegal, or
overclaim-token line proofs.

## Wording Strength

`wordingRules.maxStrength` is a hard maximum.

`PhraseCapability` is the per-`PublicClaim` public-surface permission object.
It records the effective wording ceiling, allowed predicate family, line-comment
permission, result/best/engine-language denial, and forbidden terms. It is not a
prose template and must not contain generated chess explanation text. Public
blocks carry the capability so API/frontend consumers can fail closed on stale
or malformed payloads; they still must not invent text from the capability.
`PublicPhrase` is the separate closed phrase token. A phrase is renderable only
when it matches the block claim id, uses `PublicClaimPredicate.LineCommentary`,
stays within the block/cap wording ceilings, and does not contain forbidden
terms. Caller-supplied `PublicClaim.text`, evidence ids, role labels, and raw
variation/evidence fields are not phrase tokens.

The effective block strength is the weakest of:

- plan maximum strength
- section-role cap
- selected claim cap
- source-context cap

The caps are:

| Cap | Allowed public meaning | Forbidden public meaning |
| --- | --- | --- |
| `hidden` | no public text | any chess claim or fallback narration |
| `negative_only` | carried negative/boundary wording only | positive strategic claim, main plan, result, best move |
| `context_only` | non-authoritative context/reference only | current-position proof, theory truth, forced line, result, engine/oracle proof |
| `qualified_support` | subordinate support for an existing selected claim | independent lead, result, forced conversion, winning/drawing claim |
| `assertive_certified` | the exact selected certified/admitted claim inside its owner/anchor/route/scope | extrapolation beyond the selected claim; raw engine/PV/eval wording |

Source-context blocks remain capped at `context_only`, even when a board lead is
present.

## No Meaning Creation

The renderer must not:

- select, rank, or admit claims
- revive suppressed claims
- discard blocked reasons in a way that permits revival
- upgrade `wordingStrengthCap`
- convert source, opening, motif, endgame-study, or retrieval context to truth
- interpret raw engine evidence
- turn prepared variation evidence directly into book-style prose
- turn `BookAnnotationPlan` directly into English prose or public text
- turn source contexts or source-frame metadata into English chess claims
- turn defender-resource or failed-tempting-move evidence into a main
  recommendation
- turn opening sequence context or `opening-line-test:*:context` refs into
  opening prose, theory claims, recommendations, or proof ownership
- turn motif line context or `motif-line-test:*:context` refs into motif prose,
  threat claims, forcing claims, recommendations, or proof ownership
- turn endgame technique context or `endgame-line-test:*:context` refs into
  technique prose, result claims, forced-conversion claims, tablebase/oracle
  claims, recommendations, or proof ownership
- turn retrieval illustration context or `retrieval-line-test:*:context` refs
  into current-position proof, recommendations, verdicts, game-result
  evidence, citation display, or proof ownership
- create best-move, theory-truth, forced-line, result, winning, drawing, or
  oracle wording
- merge `master_reference` and `online_trend` rankings
- add Sxx labels not present in the plan
- infer evidence not present in `plan.evidence`
- publish plan-wide evidence that is not owned by an unblocked public selected
  claim
- turn support-only, deferred, stale, wrong-binding, raw-engine-only, or
  source-only material into user-facing claims

## Source And Opening Boundary

`OpeningContextCandidate` remains structured context only.

Aliases are display/context labels only. They do not rewrite source taxonomy,
candidate moves, source identity, position keys, or current-position truth.

Opening move statistics are statistical/context references only. They are not
best moves, theory truth, forced lines, result claims, engine evidence, oracle
truth, or current-position proof.

`master_reference` and `online_trend` source vectors remain separate. If both
are present, the renderer may preserve both evidence refs but must not merge or
rerank them into one candidate order.

Specific game, player, event, and game URL citation remains retrieval-lane
material. It is not opening aggregate context.

## Source And Motif Boundary

Motif source context is structured context only. The renderer may preserve
selected `motif-example:*`, `motif-detector-carrier:*`, `motif-line-context:*`,
and `motif-line-test:*:context` evidence refs that already survived selection,
but it must not render raw motif rows, detector internals, source tags, or
label-only motif prose.

If public-safe line evidence is rendered beside motif context, it must come
from `RenderVariationEvidence` owned by the exact-board claim. The motif
context ref does not authorize best, forced, result, threat, mate, perpetual,
or engine wording.

## Source And Endgame Boundary

Endgame-study context is structured context only. Study labels and technique
refs frame technique after exact applicability; they do not prove win, draw,
loss, forced conversion, tablebase/Syzygy/WDL/DTZ/DTM truth, or oracle truth.

The renderer may preserve selected `endgame-study:*:applicable`,
`endgame-study-applicability:*`, `endgame-technique:*`, and
`endgame-line-test:*:context` evidence refs that already survived selection,
but it must not render raw endgame rows, placement or relation evidence arrays,
candidate plans, outcome fields, or label-only technique prose.

If public-safe line evidence is rendered beside endgame context, it must come
from `RenderVariationEvidence` owned by the exact-board claim. The endgame
context ref does not authorize result, forced conversion, tablebase, oracle,
best-move, or engine wording.

## Source And Retrieval Boundary

Retrieval context is optional illustrative/example context only. It may remain
selected as non-authoritative context, but it is not proof of the current
position and does not outrank exact-board variation evidence, source
statistics, or certification.

The renderer may preserve selected `retrieval-example:*`,
`retrieval-illustration:*`, and `retrieval-line-test:*:context` evidence refs
that already survived selection, but it must not render raw retrieval rows,
snippets, player names, event names, game URLs, result metadata, citation
metadata, or display-candidate shortcuts.

If public-safe line evidence is rendered beside retrieval context, it must
come from `RenderVariationEvidence` owned by the exact-board claim. The
retrieval context ref does not authorize recommendation, verdict,
current-position proof, result, best-move, forced-line, theory, or engine
wording.

## Book Annotation Planner Boundary

`BookAnnotationPlanner` is a renderer-side language-neutral planner under
`lila.commentary.render.annotation`. It consumes `CommentaryPlan` only and
emits structured `BookAnnotationPlan` data only. It does not wire controller
routes, API fields, frontend UI, live source lookup, Stockfish/WASM execution,
cache persistence, phrase templates, or English prose.

`BookAnnotationPlan` contains:

- `BookAnnotationUnit` values for strong exact-board annotation units
- `BookAnnotationBoundary` values for fail-closed reasons
- `BookAnnotationWordingRules` carrying wording caps only

Strong unit creation requires an unblocked selected main claim that is
admitted, exact-board-bound, and not source, engine, or renderer owned. The
matching `PlanAnnotationSelection` must bind to the same claim, its primary
proof id must resolve to a public-safe candidate/root proof on that claim, and
at least one companion proof id must resolve to a public-safe
defender-resource or defender-reply proof on that claim. All proofs must pass
the same public proof-id, provenance, binding, boundary, and
`surfaceAllowance = public_line` checks used by line selection and public
rendering.

Candidate-only, defender-only, source-only/retrieval-only, blocked-claim,
`boundary_only`, `internal_only`, raw-engine/source-provenance, unmatched
source-frame, unsafe/internal proof-id, and missing-proof inputs fail closed to
boundaries rather than units. Support and negative proof ids may appear only on
an existing strong primary unit. Failed tempting, premature, and release-risk
proofs cannot become primary units. Retrieval frames remain illustrative,
non-authoritative metadata only.

Planner units carry structured data only: claim id, SAN/UCI line, resource and
reply moves, proof role, test result, source-frame metadata, wording cap, and
proof ids. They must not contain final sentence, template, phrase, or public
text fields.

## Line Commentary Planner Boundary

`LineCommentaryPlanner` is the renderer-side skeleton planner under
`lila.commentary.render.annotation`. It consumes `BookAnnotationPlan` only and
emits structured `LineCommentaryPlan` data only. It does not consume
`CommentaryPlan`, raw claims, source rows, engine data, controller/API routes,
frontend UI, live source lookup, Stockfish/WASM execution, cache persistence,
or lower-layer meaning.

`LineCommentaryPlan` contains `LineNote` values plus fail-closed
`LineCommentaryBoundary` values. The initial skeleton may emit only:

- a main-line note from `BookAnnotationUnit.lineSan`
- a defensive-resource note when the unit already carries resource or reply
  moves
- a line-result note only for this closed role/result table:
- optional coarse `LineContext` hints on already-admitted notes only, mapped
  from existing `BookAnnotationSourceFrame.kind` as `Opening` -> `opening`,
  `Motif` -> `pattern`, `EndgameStudy` -> `endgame`, and `Retrieval` ->
  `example`

| `BookAnnotationUnit.proofRole` | `BookAnnotationUnit.testResult` | `LineNoteMeaning` |
| --- | --- | --- |
| `persistence` | `pressure_persists` | `pressure_persists` |
| `defender_resource` | `does_not_restore_counterplay` | `does_not_restore_counterplay` |
| `defender_resource` | `resource_fails` | `resource_fails` |
| `defender_resource` | `resource_works` | `resource_works` |
| `hold` | `defensive_hold` | `defensive_hold` |
| `simplification` | `simplifies` | `simplifies` |
| `conversion` | `simplifies` | `simplifies` |
| `conversion` | `converts` | `converts` |

If the book annotation plan has no units, the effective wording cap is below
`qualified_support`, the main line is empty, the result is unsupported, or the
role/result pair is outside the table, the line planner must not invent
fallback notes. Defensive-resource notes are structural only and do not create
result meaning by themselves. Source frames from `BookAnnotationUnit` are not
meaning input for this skeleton. They may supply only non-authoritative context
hints to notes that already exist; source-only frames, authoritative frames, and
malformed frames must not create notes, prose, citation display, recommendations,
theory, result meaning, or proof ownership. The line skeleton does not expose
raw `sourceRefIds`. V7d adds separate line-scoped support/caution skeletons
from `BookAnnotationUnit.supportingLines` and
`BookAnnotationUnit.cautionLines`. These details may carry only SAN/UCI,
tested, reply, and resource line payloads already present in admitted
public-safe prepared support/negative lines behind the strong unit. Support
details may mirror the closed safe result table above when role/result matches.
Caution details are limited to early-move, premature-move, and
releases-counterplay caution kinds from the explicit negative role/result
pairs. Support/negative ids alone do not create notes, missing detail line data
fails closed, and caution notes must remain separate support-only skeletons
instead of primary line/result notes. Natural/tempting wording,
recommendations, final sentence, template, phrase, or public text fields remain
closed.

## English Line Commentary Boundary

`EnglishLineCommentaryWriter` is the first language-specific writer under
`lila.commentary.render.annotation`. It consumes `LineCommentaryPlan` only and
emits `EnglishLineCommentary` with at most one `EnglishLineComment` per
annotation id.

An English comment requires a usable `main_line` note and a compatible
`line_result` note. A defensive-resource note may add a compact resource/reply
clause only when both lines are present. Without the required main/result pair,
the writer emits no comment and no one-move fallback caption.

The closed result phrase table is:

| `LineNoteMeaning` | Public phrase |
| --- | --- |
| `pressure_persists` | `pressure remains in the line` |
| `does_not_restore_counterplay` | `counterplay does not return in the line` |
| `resource_fails` | `the resource fails in the line` |
| `resource_works` | `the resource works in the line` |
| `defensive_hold` | `the defense holds in the line` |
| `simplifies` | `the position simplifies in the line` |
| `converts` | `the line converts into a clearer continuation` |

Unsupported meanings emit no comment. Support notes may append short
`In {line}, ...` sentences only after the main comment exists. Caution notes
may append short `By contrast, ...` sentences only after the main comment
exists and only when the detail note is bound to the same primary proof id as
the main/result pair; caution never becomes the main sentence. The writer must not use
natural, tempting, best, only, forced, winning, drawn, decisive, refutes,
engine-says, theory-proves, tablebase, or oracle wording, and it must not
surface source refs, proof ids, raw/internal ids, or blank SAN tokens in the
comment text. SAN tokens, including Black-move ellipses such as `...Qb6`, are
preserved in public prose.

## Engine Boundary

The renderer may carry only bounded `Certification` evidence refs already
present in `plan.evidence`.

`EngineCertification` evidence refs may be public render evidence only when the
ref has owner, anchor, route, and scope binding and `plan.evidence` also
contains same-binding `Certification` evidence and the selected Certification
claim has a same-binding typed lower board reason (`Root`, `Witness`, `Object`,
or `Delta`). A generic exact-board carrier is insufficient. Unbound, stale-shaped,
Certification-unpaired, or carrier-unbacked `EngineCertification` refs are not
public render evidence.
The public selected claim that references an `EngineCertification` ref must be
a Certification-layer claim. Projection/Sxx claims cannot make plan-wide engine
certification evidence public.

It must not render raw evals, centipawns, mate scores, PVs, engine packets, or
raw engine labels. `RawEngine` evidence refs are not public render evidence.

Eval swing cannot become best-move, winning, forced, result, or theory wording
unless the plan already contains the certified owner claim and the plan cap
allows that strength. Even then, the wording remains limited to the selected
claim's owner, anchor, route, and scope.

## Blocked Handling

`plan.blocked` is do-not-say material.

The renderer preserves blocked claim ids and typed reasons as
`RenderSuppression(public = false)`. It does not expose them as public claims by
default and does not generate user-facing explanations from blocked material.
If a malformed `CommentaryPlan` places the same claim in a selected section and
`plan.blocked`, the blocked entry wins: the renderer must suppress the public
block, must not publish blocked-only evidence, and must not expose that claim
through public `RenderBoundary` metadata.

Debug consumers may inspect internal suppression metadata, but that metadata is
not a source of public commentary.

## noCommentary

When `CommentaryPlan.noCommentary` is true, or when `hidden` removes all public
blocks, the renderer returns `RenderStatus.NoCommentary` with no public blocks.

Blocked claims and safe evidence refs may still be retained by a future
internal/debug channel, but the first public `CommentaryRender` scaffold emits
no public blocks and no public evidence refs for `NoCommentary`. The renderer
must not create fallback chess narration.

Context-only plans are not no-commentary plans. They render context blocks with
`RenderStatus.ContextOnly`, no primary block, and `context_only` strength.

## Executable Validation

Executable validation lives in:

- `modules/commentary/src/main/scala/lila/commentary/render/CommentaryRenderer.scala`
- `modules/commentary/src/main/scala/lila/commentary/render/CommentaryRendererContract.scala`
- `modules/commentary/src/main/scala/lila/commentary/render/annotation/BookAnnotationPlanner.scala`
- `modules/commentary/src/main/scala/lila/commentary/render/annotation/LineCommentaryPlanner.scala`
- `modules/commentary/src/main/scala/lila/commentary/render/annotation/EnglishLineCommentary.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/CommentaryRendererContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/BookAnnotationPlannerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/LineCommentaryPlannerContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/render/annotation/EnglishLineCommentaryContractTest.scala`
- `modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl`

The scaffold validates:

- primary/support/context/contrast block mapping
- blocked metadata preservation as non-public suppression
- hidden and no-commentary silence
- wording cap preservation for all executable strengths
- source/opening context non-authority
- raw engine filtering
- bounded `Certification` / `EngineCertification` evidence handling
- public-safe prepared variation evidence carriage
- internal prepared variation debug filtering
- raw retrieval row, citation metadata, and internal proof-packet filtering
- no evidence invention from claim-local refs outside `plan.evidence`
- no merge of opening `master_reference` and `online_trend` refs
- deterministic role fragments as fallback labels when no safe English line
  comment exists
- renderer-side book annotation planning from `CommentaryPlan` only, with
  structured units/boundaries/caps and no book-style prose
- renderer-side line commentary skeleton planning from `BookAnnotationPlan`
  only, with structured notes/boundaries and no final English prose
- renderer-side English line commentary writing from `LineCommentaryPlan` only,
  with a closed phrase table, no lower evidence/source/probe/cache inputs, no
  unsupported-meaning fallback, support/caution appendage behind an existing
  main comment only, and grammar/forbidden-word guards
