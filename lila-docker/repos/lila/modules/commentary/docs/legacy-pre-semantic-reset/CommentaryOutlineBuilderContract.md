# Commentary Outline Builder Contract

This document freezes the structured outline-builder contract.

It does not open renderer prose, API/frontend payload wiring, or source live
integration.

## Authority Boundary

`CommentaryOutlineBuilder` consumes `CommentaryOutline` emitted by selection.

It does not:

- select a lead
- rank support
- admit claims
- suppress claims
- revive suppressed claims
- reinterpret evidence
- promote source context to truth
- upgrade wording strength
- generate prose

The builder is a stable typed mapper from selector-owned data to
`CommentaryPlan`.

## Plan Shape

The stable output types are:

- `CommentaryPlan`
- `PlanSection`
- `PlanRole`
- `PlanEvidence`
- `PlanBoundary`
- `PlanAnnotationSelection`
- `PlanAnnotationFrame`
- `PlanAnnotationStrength`
- `WordingRules`
- `BlockedClaim`

The stable plan sections are:

- `main`
- `support`
- `context`
- `contrast`
- `blocked`
- `evidence`
- `variationEvidence`
- `annotationSelections`
- `wordingRules`

`CommentaryPlan.noCommentary` is true when the selected commentary sections
are empty:

- no `main`
- empty `support`
- empty `context`
- empty `contrast`

Blocked do-not-say material and evidence references may still be preserved on a
no-commentary plan.

## Mapping Rules

The mapping is lossless and selector-preserving:

- selector `lead` maps to `main`
- selector `support` maps to `support`
- selector `context` maps to `context`
- selector `contrast` maps to `contrast`
- selector `suppressedClaims` maps to `blocked`
- selector `evidenceRefs` maps to `evidence`
- selector `variationEvidence` maps to `variationEvidence`
- selector `annotationSelections` maps to `annotationSelections`
- selector `wordingStrengthCap` maps to `wordingRules.maxStrength`

`CommentaryOutlineBuilder` must copy `evidenceRefs` from the outline itself.
It must not recompute evidence from selected claims or lower carriers.

Prepared variation evidence maps losslessly from selector-owned
`CommentaryOutline.variationEvidence` to
`CommentaryPlan.variationEvidence` as `PlanVariationEvidence`. The builder
must not infer a new line, extend a continuation, reinterpret the proof purpose,
reinterpret defender-resource / failed-tempting-move / release-risk / hold /
conversion / simplification / persistence roles, raise the wording cap, or
convert a line proof into chess meaning.
Source-context line-test refs such as `opening-line-test:*:context`,
`motif-line-test:*:context`, `endgame-line-test:*:context`, and
`retrieval-line-test:*:context` remain ordinary context evidence refs. The
builder must not resolve those refs into
`PlanVariationEvidence`; only selector-owned `PreparedVariationEvidence` from
an exact-board claim may enter the plan variation-evidence field.

Language-neutral annotation selections map losslessly from
`CommentaryOutline.annotationSelections` to
`CommentaryPlan.annotationSelections`. The builder must not reorder proof ids,
rerank support/negative ids, parse source line-test ref strings into chess
meaning, add source frames, raise the wording cap, or generate phrase templates
or English prose. A candidate-only weak line remains absent because selection
leaves the annotation handoff empty.

## Context Boundary

Selected source context remains context only.

Selected source-context `softReasons` are preserved as `PlanBoundary` metadata
attached to the context claim id. These soft reasons do not become suppression
or truth. They remain context-boundary metadata.

Hard rejected claims remain in `blocked`.

Context-only outlines use the no-main policy:

- `main = None`
- context claims remain in the `context` section
- `wordingRules.maxStrength = context_only`
- `noCommentary = false`

If no lead, support, context, or contrast exists, the plan is a no-commentary
plan. Suppressed claims still map to `blocked`, and evidence references still
map to `evidence`.

## Blocked Claims

`suppressedClaims` are do-not-say material.

The builder must preserve every suppressed claim and its typed reasons in
`blocked`. It must not drop, reinterpret, weaken, or revive them.

Raw engine shortcuts, source-truth shortcuts, renderer strength requests,
support-only endpoints, deferred endpoints, and stale/wrong-binding evidence
remain blocked exactly as selection emitted them.

## Wording Rules

`wordingStrengthCap` is a maximum.

The builder maps it to `WordingRules.maxStrength` without raising it. Renderer
or caller requests for stronger wording stay blocked by the selector and cannot
change the plan cap.

## Evidence Boundary

`PlanEvidence` carries `EvidenceRef` handles only.

Evidence references are not proof rewriting, prose snippets, engine
interpretation, source-stat ranking, or admission law.

`EngineCertification` and `Certification` references remain structured
metadata. Raw engine packets, PVs, centipawn values, and mate scores are never
parsed by the builder.

## Opening Context Boundary

Opening context remains structured context only.

Opening evidence and candidate references must not become:

- best move
- theory truth
- forced line
- result claim
- engine evidence
- oracle truth
- current-position proof

`master_reference` and `online_trend` references remain separate source
vectors. The builder must not merge, rank, or compare them.

Specific game, player, event, or URL citation remains a retrieval-lane concern,
not an opening-source fact.

## Endgame Context Boundary

Endgame-study context remains structured context only.

Endgame technique refs must not become:

- win, draw, or loss claims
- forced conversion
- tablebase, Syzygy, WDL, DTZ, DTM, or oracle truth
- current-position proof
- prepared variation evidence ownership

The builder may carry selected `endgame-technique:*` and
`endgame-line-test:*:context` evidence refs, but it must not infer technique
meaning, method prose, exceptions, or defender-resource/hold lines from them.

## Retrieval Context Boundary

Retrieval context remains optional illustrative/example context only.

Retrieval illustration refs must not become:

- current-position proof
- recommendation
- verdict
- game-result evidence
- player, event, or citation display
- prepared variation evidence ownership

The builder may carry selected `retrieval-illustration:*` and
`retrieval-line-test:*:context` evidence refs, but it must not infer a
comparable line, similar plan, theme, citation prose, or claim transfer from
them. Public line evidence, if present, must already be selector-owned
`PreparedVariationEvidence` from an exact-board claim.

## Executable Validation

Executable validation lives in:

- `modules/commentary/src/test/scala/lila/commentary/selection/CommentaryOutlineBuilderContractTest.scala`

The executable validation proves that the builder copies sections, blocked
material, evidence refs, prepared variation evidence, wording caps, soft source
boundaries, and selector-owned annotation handoff without reinterpretation. It
also proves source frame refs remain metadata only and candidate-only weak
lines produce no public annotation handoff.

The corpus rows that freeze the planner/surface boundary live in:

- `modules/commentary/src/test/resources/commentary-corpus/planner-expectations.jsonl`
- `modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl`
