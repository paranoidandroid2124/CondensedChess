# Commentary Program Map

This is the onboarding map for the current Chesstory commentary-analysis work.
It points to the live authority documents and summarizes the current operating
state.

Use this map to avoid stale or branch-external assumptions, but do not treat it
as approval to preserve duplicated implementation shapes. When cleanup or
deduplication is the task, inspect the current source and call sites first,
then simplify in place while keeping the runtime truth/trust boundary intact.
Adding a new helper, `Evidence`, `Support`, `Boundary`, `Contract`, or policy
object requires a live-code reason that an existing boundary cannot carry the
same responsibility.

## Current Status

Current work is boundary cleanup plus source-backed public-witness expansion
inside the existing MoveReview authority model:

- claim authority is centralized under `analysis.claim`
- planner authority checks consume `ClaimAuthorityResolver` directly
- render release safety is centralized under `analysis.render.FragmentAuthority`
- public MoveReview wire is minimized around backend-owned structured
  diagnostics and the certified `moveReviewPlayerSurface`
- opening-family, opening-route, and opening-goal coverage reuse existing
  catalog/resolver/evaluator paths; runtime gates and tooling provenance details
  live in `CommentaryPipelineSSOT.md` and `CommentaryTruthBoundary.md`
- canonical docs stay as four files; historical CTH logs are compressed into
  current-state rules and summary tables

Do not use branch-external or branch-removed documents as authority for this
worktree.

## Live Coverage Map

The current motif and authority vocabulary is intentionally layered. A raw
motif name, tactical detector id, semantic source id, relation witness, and
proof family do not carry the same release authority.

| layer | live source | current coverage | authority role |
| --- | --- | --- | --- |
| raw motif model | `modules/commentaryCore/src/main/scala/lila/commentary/model/Motif.scala` | 55 `Motif` case classes | detector/model vocabulary only; not public authority by itself |
| tactical pattern detectors | `modules/commentaryCore/src/main/scala/lila/commentary/analysis/tactical/TacticalPatternDetectors.scala` | 9 detector ids | board-pattern support; named mate ids may narrow a top-PV `MateNet` practical label, but only after the analyzer-owned relation witness |
| relation witness inventory | `MoveReviewExchangeAnalyzer.RelationKind` plus `RelationObservationCatalog` | 23 implemented relation kinds, 0 deferred relation kinds | public `strategic_relation` rows are limited to implemented descriptors with analyzer-owned board replay |
| semantic observation ids | `StrategicObservationIds.SemanticObservationId` | 25 ids | normalized semantic ids; relation admission uses only catalog descriptors |
| evidence source ids | `StrategicObservationIds.EvidenceSourceId` | 128 ids | support/proof vocabulary; source-shaped strings cannot mint relation authority |
| plan taxonomy | `PlanTaxonomy` | 35 plan kinds, 10 ranked themes plus `Unknown` | planning/proof-family vocabulary; a plan label is not exact-board proof |

The deferred relation set is currently empty, but it is not the whole motif map:
relation-shaped motif families that already exist in nearby model or detector
vocabulary remain non-public until they receive a public relation witness,
source, semantic id, and catalog descriptor.

Current five-motif decision:

| relation motif | current decision | live authority |
| --- | --- | --- |
| `trapped_piece` | public when board-backed | implemented `Trapped piece` supported-local row, plus catalog relation metadata, only from the reviewed legal move attacking an explicit bound non-pawn/non-king target that has no safe legal escape |
| `zwischenzug` | public when board-backed | implemented `Zwischenzug` supported-local row, plus catalog relation metadata, only from the reviewed legal move giving direct check while a legal non-pawn capture on the explicit bound recapture square was available and the reviewed move did not take that square |
| `domination` | public when board-backed | implemented `Domination` supported-local row, plus catalog relation metadata, only from the reviewed legal move attacking an explicit bound same/lower-value non-pawn/non-king target whose pseudo-escape squares are all controlled and whose legal escapes all fail |
| `stalemate_trap` | public when PV-backed | implemented draw-resource relation only from legal replay ending in actual stalemate with draw-stable engine score; top-PV witnesses may also project a bounded `Stalemate resource` summary row |
| `perpetual_check` | public when PV-backed | implemented draw-resource relation only from legal replay proving a repeated checking cycle, repeated position key, and draw-stable engine score; top-PV witnesses may also project a bounded `Perpetual check` summary row |

The table above is only the map-level decision. Runtime replay ownership and
row ordering live in `CommentaryPipelineSSOT.md`; exact truth gates live in
`CommentaryTruthBoundary.md`; trust/fallback risks live in
`CommentaryTrustBoundary.md`. At this level the invariant is simple:
implemented exact-board relations require analyzer-owned typed details that
match the catalog kind, draw-resource relations require analyzer-owned PV
replay, top-PV draw-resource witnesses are the only draw-resource summary-row
fallback, and raw motif/helper text remains support-only.

## Document Roles

Read the documents in this order:

1. `CommentaryProgramMap.md`
   - onboarding, current status, current maintenance scope, document navigation
2. `CommentaryPipelineSSOT.md`
   - canonical runtime path and module ownership
3. `CommentaryTruthBoundary.md`
   - chess-truth signoff and no-go failures
4. `CommentaryTrustBoundary.md`
   - trust-risk map, CTH summary, authority ladder, defer rationale

## Live Architecture Boundary

Current authority is internal and MoveReview-first:

- `ProofContractRules` defines proof-family eligibility.
- `ClaimAuthorityResolver` resolves `CertifiedOwner`, `SupportedLocal`,
  `DiagnosticOnly`, or `Suppressed`.
- `MoveReviewExchangeAnalyzer` owns bounded legal PV replay, exact-slice branch
  metadata, relation details, and relation projection read-models consumed by
  later policy and surface code. Reviewed-move witnesses must receive
  `playedMove`; the top PV first move is evidence, not identity.
- `StrategicSemanticObservationPipeline` emits typed selector/support
  observations from analyzer-owned replay context and cataloged relation
  descriptors. Selector evidence cannot create proof authority by itself.
- `RelationObservationCatalog` owns implemented relation descriptors and
  descriptor-level fallback metadata. Non-cataloged or deferred relation motifs
  stay support-only until they receive a board-replayed witness, source,
  semantic id, and catalog descriptor.
- `QuestionFirstCommentaryPlanner` ranks questions; it does not own proof
  authority.
- `MoveReviewCausalClaim` owns high-risk MoveReview causal release. A selected
  question remains diagnostic until the gate certifies typed support, causal
  relation, subject role, concrete anchor, and an admitted local fact for
  support-required planner questions. It submits planner evidence/relation
  inputs to `MoveReviewLocalFact`; it does not infer local-fact family,
  authority, or strict fallback eligibility from rendered claim words.
- `MoveReviewLocalFact` owns the shared typed local-fact family and authority
  vocabulary plus the candidate/admit policy used by planner, basic, and scoped
  MoveReview surfaces. Candidate inputs carry source, subject, anchors, line
  binding, and evidence refs so later motif/strategic producers can supply
  evidence without giving renderers claim authority. It also owns the
  `PlayerFacingMoveDeltaClass` -> local-fact-family mapping.
- `CommentaryIdeaSurface` submits `MoveReviewLocalFact` candidates and attaches
  admitted facts to MoveReview basic descriptors before fallback rendering. In
  strict rejected fallback, only strict-eligible `local_fact_family` /
  `local_fact_authority` descriptors may render; soft line-only or
  strategic-plan candidates fall through to exact factual fallback.
- `MoveReviewPolishSlots.localFact` preserves the admitted local fact for
  runtime/tooling diagnostics; `reasonTags` remain display/compatibility tags.
- `FragmentAuthority` decides renderer release safety.
- `CommentaryApi` and frontend code consume typed payloads only.

## Current Maintenance Scope

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality. `AuthoritySurfaceLedger`
  may include controlled `SourceReview` samples only after engine-backed
  admission and existing typed public-surface gates pass; these rows are
  surface-ledger witnesses, not external-game provenance claims.
- trigger hardening that removes generic string overlap, unknown-subtype
  fail-open, or blocked line-geometry without expanding public authority
- relation-witness hardening that centralizes legal replay plus attack/defense
  evidence in `MoveReviewExchangeAnalyzer`, routes semantic producer access
  through `StrategicSemanticObservationContext`, and keeps new motif rows
  selector/support-only until a proof contract admits them; deferred motif
  families are tracked in the relation inventory but remain non-public and
  uncataloged until they receive a board-replayed witness
- MoveReview player-surface relation metadata that exposes cataloged relation
  support as `strategic_relation` advanced rows without creating proof authority
- docs and package cleanup that preserves the current authority boundary

Closed unless a new audit explicitly opens them:

- broad heavy-piece/local-bind/global-squeeze expansion
- broad lesson authority
- Chronicle/Active runtime reopening; see the naming boundary below
- public API/frontend wire expansion except audited typed diagnostics or
  payload minimization that does not create product authority
- support-only or deferred carrier promotion

## Strategic Expansion Naming Boundary

Expansion labels are planning vocabulary, not runtime module names or authority
tokens. This map names the risk; the canonical details live in:

- `CommentaryPipelineSSOT.md` for the stable runtime/proof boundary that each
  umbrella label must map to before implementation.
- `CommentaryTrustBoundary.md` for the trust risk of broad, rollout, frontier,
  Active, and Chronicle names becoming authority tokens.

Admission-unit planning follows the same boundary: the catalog may queue only
plan-kind work units that already resolve through a public runtime contract.
Current cataloged families include static/backward target fixation, local-file
entry, color-complex flank clamp, outpost occupation, IQP inducement,
simplification, defender/queen/bad-piece exchange ownership, and central-break
timing. Runtime-only proof families such as `target_focused_coordination` remain
covered by contract/surface tests rather than by a `PlanTaxonomy.PlanKind`
admission unit.

## High-Frequency Motif Authority Map

The strategic motif cleanup currently treats common motifs as a ladder, not as a
single promotion target. The current audit split is:

| motif family | current defer / risk cause | support-only / practical tier | exact / target-authority tier |
| --- | --- | --- | --- |
| Carlsbad / minority attack | exact target authority needs the mirrored board slice and minority-support predicate; source-only Carlsbad or minority semantics used to risk overclaiming pressure | `Practical pressure` may describe structural pressure only from Carlsbad/minority source plus target-pressure facts, pack-side owner match, and no target metadata | `Fixed target` / `Minority attack` target metadata only from `CarlsbadFixedTarget(..., minoritySupport = true)` or exact fixed-target proof |
| fixed target / weak square | lower-authority target context can be useful, but target metadata implies a durable exact target | `Practical target` and `Practical pressure` may describe current-board weakness context from bounded plan hints or typed weakness facts without proof metadata | exact target rows expose only the typed proof square and builder-owned wording |
| local file entry | line support is common, but same-file entry authority needs exact file/entry geometry; stale file labels must not suppress unrelated context | `Practical line` may use occupied-line, open/semi-open file, doubled-rook, route-line, or compensation line facts with pack-side owner match and fact-matched file prose | `File entry` target metadata only when the exact text names the same target square through that target file |
| outpost | route/opening outpost evidence is useful context but not exact occupation; broad outpost prose stays lower authority | `Practical outpost` may use typed outpost, strong-knight, exact route-outpost, or directional outpost support with minor-piece, readiness, confidence, and pack-side gates | `Knight outpost` target metadata only from the exact outpost occupation witness with builder-owned knight wording; `Opening outpost` remains lower-authority and targetless |
| color complex | structure-context grammar is separate from target authority; broad dark/light complex tags must not imply a forced target | `Practical space` may describe color-complex clamp from enemy weakness, dark/light token, at least two focus squares, and pack-side owner match without target metadata | `Color complex` target metadata only from builder-owned bishop/knight attack wording that matches target-square color and minor-piece geometry |

The recurring cleanup categories are: required truth gate, over-narrow exact-only
gate, missing lower-authority construction, duplicate or inconsistent downstream
gate, and sanitizer/frontend fail-open or fail-closed drift. Changes should keep
using the existing producer, selector, packet/proof contract, player payload
builder, sanitizer, frontend decoder, and existing tests before adding a new
runtime boundary.

New strategic work should use domain/proof names, not rollout or breadth names.
Acceptable names describe the chess asset and proof boundary, for example
`LocalFileEntryBind`, `CounterplayAxisSuppression`,
`ColorComplexSqueeze`, `OutpostOccupation`, or a cataloged relation witness.
Avoid `broad`, `global`, `Track`, `Frontier`, `B7`, `B8`, `Active`, and
`Chronicle` in new runtime source modules, proof families, public authority
tokens, or product row kinds unless they are documenting legacy/test-only
boundaries.

## Verification Expectations

For package/naming/boundary work:

- compile and run targeted tests serially
- keep runtime and test/tooling boundaries separate
- update the relevant canonical doc in the same change
- report unverified work as boundary cleanup only

For claim authority, proof-contract, planner, renderer, or trust behavior,
choose the targeted serial tests from `CommentaryPipelineSSOT.md`'s current
verification targets. Do not use this map as a second, divergent test matrix.

## Maintenance Triggers

Re-audit the runtime docs only when:

- the user asks for a fresh audit
- code changed after the relevant snapshot in commentary runtime, controller,
  API, or analyse frontend paths
- a task introduces a runtime path not covered by the current SSoT

When changing the audited pipeline, update `CommentaryPipelineSSOT.md`.
When changing trust-relevant behavior, update `CommentaryTrustBoundary.md`.
When changing truth/signoff behavior, update `CommentaryTruthBoundary.md`.
