# Commentary Renderer Contract

This document freezes the first renderer contract for the current worktree.

It does not open API wiring, frontend wiring, live source adapters, raw engine
intake, or model-authored prose generation.

## Authority Boundary

The renderer consumes `CommentaryPlan` only.

Short form: `CommentaryPlan only`.

It must not consume:

- raw `CommentaryOutline` selector claims except through `CommentaryPlan`
- `RuntimeEnginePacket`
- `EngineEvidencePacket`
- raw source rows as truth
- opening source fixture rows or `OpeningContextCandidate` JSON directly

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
- `RenderText`
- `RenderEvidenceRef`
- `RenderBoundary`
- `RenderSuppression`
- `RenderWording`

`CommentaryRender` is structured-block-first. Text is a leaf field on a block,
not the contract itself.

The contracted render statuses are:

- `rendered`
- `contextOnly`
- `noCommentary`

The minimal renderer emits only short deterministic role fragments:

- `Primary`
- `Support`
- `Context`
- `Contrast`

These fragments are display labels, not chess claims. The renderer does not
emit broad chess narration or generated prose.

## Section Mapping

The mapping is plan-preserving:

- `plan.main -> RenderRole.Primary`
- `plan.support -> RenderRole.Supporting`
- `plan.context -> RenderRole.Context`
- `plan.contrast -> RenderRole.Contrast`
- `plan.blocked -> RenderSuppression(public = false)`
- eligible public refs from `plan.evidence -> RenderEvidenceRef`
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

## Wording Strength

`wordingRules.maxStrength` is a hard maximum.

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

## Engine Boundary

The renderer may carry only bounded `Certification` evidence refs already
present in `plan.evidence`.

`EngineCertification` evidence refs may be public render evidence only when the
ref has owner, anchor, route, and scope binding and `plan.evidence` also
contains same-binding `Certification` evidence and a same-binding typed board
reason (`Root`, `Witness`, `Object`, or `Delta`). Unbound, stale-shaped,
Certification-unpaired, or board-reason-unbacked `EngineCertification` refs are
not public render evidence.
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
- `modules/commentary/src/test/scala/lila/commentary/render/CommentaryRendererContractTest.scala`
- `modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl`

The scaffold validates:

- primary/support/context/contrast block mapping
- blocked metadata preservation as non-public suppression
- hidden and no-commentary silence
- wording cap preservation for all executable strengths
- source/opening context non-authority
- raw engine filtering
- bounded `Certification` / `EngineCertification` evidence handling
- no evidence invention from claim-local refs outside `plan.evidence`
- no merge of opening `master_reference` and `online_trend` refs
- deterministic role fragments only, with no chess narration
