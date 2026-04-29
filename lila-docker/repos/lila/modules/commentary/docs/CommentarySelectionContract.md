# Commentary Selection Contract

This document freezes the first commentary synthesis/editor selection
contract.

It does not open renderer, API, or frontend wiring.

The selector consumes already admitted typed claims and emits structured data
only. It does not generate natural-language commentary.

## Authority Boundary

Selection sits after:

`Root -> U -> Object -> Delta -> Certification -> Sxx projection`

It may rank, suppress, bucket, and outline claims. It must not create chess
truth.

Engine E only reaches selection through Certification. A raw engine packet,
raw `EngineEvidencePacket`, centipawn number, or PV line is never a selectable
claim. Engine-derived selection input must already be bounded same-root
Certification evidence. An `EngineCertification` evidence reference by itself
is not enough: selector admission requires a same-root `Certification` evidence
reference with the same owner, anchor, route, and scope.
If the claim, `Certification` ref, `EngineCertification` ref, or engine-certified
board reason is missing scope, the candidate is suppressed before ranking.

Source context is not a truth owner. Opening, motif, endgame-study, and
retrieval rows may become context or reference material only. They cannot beat
an exact-board lead. If no stronger exact-board lead exists, the selector may
emit a context-only outline.

`EvidenceRefKind.SourceContext` is invalid as board-claim evidence. A board
claim carrying only source or retrieval refs is suppressed with
`source_context_only` and `no_board_reason`.

Prepared variation evidence is frozen separately in
[VariationEvidenceContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/VariationEvidenceContract.md).
Selection may carry `PreparedVariationEvidence` only as a public-safe
line-proof object already bound to an admitted exact-board claim. It suppresses
raw-style, unbound, stale-shaped, source-context-owned, and raw-engine-owned
variation evidence before outline. Prepared variation evidence may affect only
evidence carriage and wording caps; it does not create Root, U, Object, Delta,
Certification, Projection, source-context, best-move, forced-line, result, or
oracle truth.
Defender-resource, release-risk, hold, conversion, simplification,
persistence, failed-tempting-move, and premature-move roles are evidence roles,
not new claim owners. A proof marked `internal_only`, a proof without bounded
same-binding provenance, or a proof whose provenance is raw engine/source
context is suppressed before outline. Failed-tempting-move and premature-move
evidence is negative/support evidence only and cannot by itself become the lead
or a main recommendation.
Opening source context may include move-order, pawn-break, development-lag,
file-ownership, king-safety, transposition, compensation, and master/online
divergence sequence references. These remain `SourceContext` evidence refs.
An `opening-line-test:*:context` ref may point at a public-safe variation proof
id, but selection admits the actual proof only from the separately admitted
exact-board claim that owns `PreparedVariationEvidence`.
Motif source context may likewise carry `motif-line-context:*` and
`motif-line-test:*:context` refs after exact detector carrier parity. These
refs remain `SourceContext` evidence and cannot admit or rank a proof; the
actual `PreparedVariationEvidence` must come from the exact-board claim.
Endgame-study context may carry `endgame-technique:*` and
`endgame-line-test:*:context` refs only after exact material, placement,
relation, and side-to-move applicability. These refs remain `SourceContext`
evidence and cannot admit or rank a proof, result, forced conversion, or
tablebase-style verdict; the actual `PreparedVariationEvidence` must come from
the exact-board claim.
Retrieval context may carry `retrieval-illustration:*` and
`retrieval-line-test:*:context` refs only as optional illustrative context.
These refs remain `SourceContext` evidence and cannot admit or rank a proof,
recommend a move, transfer a retrieved game result to the current position, or
turn player/event/result metadata into a verdict. The actual
`PreparedVariationEvidence` must come from the exact-board claim, and
display-candidate citation remains deferred until a renderer citation contract
exists.

`CandidateLineSelection` freezes the post-outline line explanation boundary and
is now used by `ClaimSelector` to decide public `CommentaryOutline`
variation-evidence carriage. It consumes `CommentaryOutline` only after claim
selection and selects over already public-safe `PreparedVariationEvidence`
attached to already selected claims/context. It must not create claims,
normalize SAN, run probes/cache/source lookup, write prose, or expose
branch/cache/probe/debug internals. Strong primary line explanation requires a
candidate-style proof and a defender-resource/defender-reply style proof bound
to the same selected exact-board lead claim. Candidate-only proof remains
weak/context only and is not emitted as public outline variation evidence.
The same strong-only decision may produce a language-neutral
`PlanAnnotationSelection` handoff on `CommentaryOutline.annotationSelections`.
That handoff carries proof ids, coarse source frame kinds (`opening`, `motif`,
`endgameStudy`, `retrieval`), raw source line-test ref ids as metadata,
`PlanAnnotationStrength.Strong`, and the selected wording cap only. It does
not contain phrase templates, English prose, source-string chess parsing, or
new admission meaning. Candidate-only weak lines produce no public annotation
handoff, and support/negative proof ids can appear there only when the strong
primary exists.
Source-context, retrieval, and raw-engine owned proofs cannot select or own a
line. Opening, motif, endgame-study, and retrieval line-test refs can annotate
only a proof id already selected from an exact-board claim; retrieval remains
illustrative. Failed tempting, premature, and release-risk proofs are
negative/support only and cannot become the primary recommendation. With no
safe exact-board strong line set, public outline variation evidence is
empty/silent.
`PublicVariationEvidenceSafety` is the shared package-private selector helper
for public-safe prepared proof checks and source line-test proof-id parsing.
`ClaimSelector` and `CandidateLineSelection` must use the same proof-id and
provenance-token rejection rules so branch/cache/probe/debug handles cannot
enter selected lines or source context refs through only one boundary.

S24 is not the generic tactic owner. S24 covers same-target forcing/realization
projection only. A concrete tactic, forced conversion, or blunder lead must be
owned by the actual current exact-board or transition family that admitted it.

Renderer may only verbalize the `CommentaryOutline`. It must not perform
admission, ranking, suppression, best-defense, persistence, conversion,
source-truth conversion, or wording-strength upgrade.

## ClaimImpact

`ClaimImpact` is ordered by these axes:

1. `resultMaterialImpact`
2. `forcedness`
3. `immediacy`
4. `persistenceAfterDefense`
5. `evidenceConfidence`
6. `evalSwing`
   - only via Certification
   - raw engine swing is `raw_engine_only`
7. `boardExplainability`
8. `pedagogicalClarity`
9. `novelty`
   - final tie-break only

The first five and last three are bounded selector ranks. `evalSwing` may carry
engine-scale magnitude but is capped for ranking and is ignored unless the
claim is already admitted through Certification.

## ClaimBucket

The executable bucket keys are:

- `mustLead`
- `shouldLead`
- `canLead`
- `support`
- `contextOnly`
- `suppress`

`mustLead` is reserved for admitted certified result, conversion, or immediate
material/forcing claims.

`shouldLead` is for admitted strategic deltas or admitted Sxx projections with
exact lower carrier/evidence.

`canLead` is for lower-impact admitted board claims.

`support` explains or orders an already selected lead. It may include lower
witnesses, support seeds, objects, deltas, certifications, related admitted
Sxx, and source context after truth lead selection. `SupportOnly` remains a
non-lead endpoint. In this frozen selector scaffold it is emitted through
`suppressedClaims` with `support_only`, not as selected support truth.

`contextOnly` is allowed only when no stronger exact-board lead exists. It must
not claim best move, result, forced line, current-position strategic truth,
engine judgment, or Sxx admission.

`suppress` is used when a claim cannot be selected or used as support truth.

## Lead Admission

A lead candidate must already be an admitted typed claim.

Allowed lead sources:

- forced mate/result only if owned/admitted by a Certification/result family
- decisive tactic or material loss only if owned by the exact current-board or
  certified transition family that admitted it
- engine-certified eval swing only through Certification
- certified strategic delta with owner, anchor, route, and scope
- admitted Sxx projection with exact lower carrier and allowed projection
  evidence kind

For S24, the allowed projection evidence must include both
`same_target_forcing_realization` and `same_target_conversion_certified`.

The backend producer handoff for runtime Sxx claims is
`StrategyProjectionAdmissionResult`. Only results produced by
`StrategyProjectionAdmission.admit` and marked `admitted` may become
Projection-layer `CommentaryClaim`s. The producer does not infer Sxx admission
from a band id, prose label, broad concept, selector-shaped claim, source row,
raw engine packet, or prepared variation evidence. The typed result must bind
source root to current root, carry same-binding exact lower carrier refs, expose
only allowed projection evidence kinds, keep an exact scope, and preserve a
bounded wording cap.

Sxx lower carriers must be exact typed refs with owner, anchor, route, and
scope binding. Unbound lower carriers are not enough to admit a Projection
claim at selection.

Source context can never become truth lead. With no stronger exact-board lead,
source context may produce a `contextOnly` outline.

For source-context fallback, `source_context_only` is a soft selector reason
only for rows that remain safe context. Hard reasons such as
`ambiguous_transposition`, `forbidden_shortcut`, or `no_board_reason` prevent
the source row from entering `context`.

Safe source context rows carry their soft reason on the selected context item.
They must not also appear as suppressed claims. `suppressedClaims` is reserved
for rows the renderer must not revive.

## Support Admission

Support must strengthen or explain the selected lead without creating truth.

Allowed support:

- lower witnesses
- support seeds
- object context
- certification evidence
- related admitted Sxx
- opening and endgame-study context after truth lead selection
- retrieval example as reference only

Forbidden support behavior:

- upgrading `support_only` to truth lead
- upgrading source text or retrieval snippets to current-position truth
- smuggling raw engine swing into a truth claim
- smuggling raw engine refs through another layer or through Sxx lower carriers
- smuggling raw engine refs through non-Projection board-claim lower carriers
- smuggling source/retrieval refs through a board claim

## SuppressionReason

The executable reason keys are:

- `support_only`
- `deferred`
- `stale_evidence`
- `wrong_owner`
- `wrong_anchor`
- `wrong_route`
- `scope_mismatch`
- `rival_band`
- `forbidden_shortcut`
- `duplicate_weaker_claim`
- `source_context_only`
- `raw_engine_only`
- `no_board_reason`
- `ambiguous_transposition`
- `retrieval_non_authoritative`
- `renderer_not_allowed`

Suppression reasons are part of the selector output. A renderer or later
surface may not discard them and revive the suppressed claim.

## Conflict Resolution

Concrete certified result or conversion beats opening/source context.

Certified tactical/result owner beats slow strategic context. A generic tactic
picture does not become S24.

Raw engine swing loses unless it has already become bounded Certification
evidence.

Opening, endgame-study, motif, and retrieval context never beats exact-board
truth.

Multiple Sxx claims resolve by owner, anchor, route, and scope nonredundancy
before impact ranking. A weaker duplicate is suppressed with
`duplicate_weaker_claim`.

Lead bucket priority is applied before impact score:

`mustLead > shouldLead > canLead`

Impact axes rank claims only inside the same lead bucket. `novelty` remains the
last tie-break inside that same-bucket impact comparison.

Duplicate suppression is scoped to competing Sxx Projection claims. Object,
Delta, and Certification claims sharing the same owner, anchor, route, and
scope remain eligible support/carrier claims unless another admission rule
suppresses them.

Object, Delta, and Certification disagreement suppresses the dependent
Projection claim.

Short-term certified forcing beats long-term structure.

`side_to_move`, owner, beneficiary, and defender are carried separately.
Ambiguous side-to-move, owner, beneficiary, or defender fails closed.

## CommentaryOutline

The selector emits structured data:

```scala
CommentaryOutline(
  context,
  lead,
  support,
  contrast,
  suppressedClaims,
  evidenceRefs,
  variationEvidence,
  wordingStrengthCap,
  annotationSelections
)
```

`context` contains source or board context that is explicitly non-authoritative
when it comes from source lanes.

`lead` contains at most one `SelectedClaim`.

`support` contains subordinate typed claims that may explain the lead.

`contrast` is reserved for rival bands, opponent counterplay, or exact negative
separators carried from lower validation.

`suppressedClaims` records every suppressed claim plus typed reasons.

`evidenceRefs` records structured evidence handles, not prose snippets.

`variationEvidence` records only the `CandidateLineSelection` public strong
line set. It includes the primary proof, its defender/resource companion proof
ids as proof objects, and selected support/negative proofs only when a strong
primary exists. It does not blindly copy every selected claim proof. Selected
claim payloads still retain their clamped `claim.variationEvidence` for payload
integrity.

`annotationSelections` records the language-neutral annotation handoff selected
by `CandidateLineSelection`. It is empty unless the strong primary condition is
met. It carries proof ids and coarse source frame metadata only; downstream
planner/renderer code must not rerank, reinterpret, parse source ref strings
into chess meaning, or invent prose from this field.

`wordingStrengthCap` is computed before rendering. The current executable
values are:

- `hidden`
- `negative_only`
- `context_only`
- `qualified_support`
- `assertive_certified`

Renderer output must not exceed `wordingStrengthCap`.
Selected claim payloads must also be clamped to the outline cap before any
renderer sees them. Source-context claims are clamped to `context_only` even
when emitted in a bounded support/context slot after a board lead.

## Outline Builder Contract

The stable outline-builder contract is frozen in
[CommentaryOutlineBuilderContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryOutlineBuilderContract.md).

`CommentaryOutlineBuilder` consumes only `CommentaryOutline` from selection and
emits `CommentaryPlan`.

It maps selector-owned structure into stable sections:

- `lead -> main`
- `support -> support`
- `context -> context`
- `contrast -> contrast`
- `suppressedClaims -> blocked`
- `evidenceRefs -> evidence`
- `variationEvidence -> variationEvidence`
- `wordingStrengthCap -> wordingRules.maxStrength`
- `annotationSelections -> annotationSelections`

The builder does not select, rank, admit, suppress, revive, reinterpret, or
generate prose.

Selected source-context `softReasons` are preserved as context
`PlanBoundary` metadata. `suppressedClaims` are preserved as blocked
do-not-say material. `evidenceRefs` are copied from the outline as references,
not recomputed from claims. Context-only outlines use the no-main policy:
`main = None`, context stays in `context`, and the maximum wording strength
remains `context_only`. If no lead, support, context, or contrast exists, the
plan is `noCommentary`; blocked do-not-say material and evidence references
are still preserved.

Opening context remains structured context only. The builder must not turn
opening references into best-move, theory-truth, forced-line, result, engine,
oracle, or current-position proof. `master_reference` and `online_trend`
references remain separate and must not be merged. Specific game, player,
event, or URL citation remains retrieval-lane material.

## Executable Validation

The first slice is the certified conversion / tactic-like claim versus
opening/source context cluster, with S24 generic-tactic leakage guarded
explicitly.

The first runtime expansion slice adds the S07/S08/S21
initiative/release/counterplay rival cluster. It keeps the same frozen
contract and adds executable cases for:

- `mustLead` certified conversion beating a higher-scored Sxx `shouldLead`
  projection by bucket priority
- exact admitted S07 initiative conversion selected as `shouldLead`
- nonredundant S21 counterplay survival retained as `support`
- adjacent S08 release-denial rival suppressed with `rival_band`
- support-only InitiativeWindow material suppressed with `support_only`
- raw-engine initiative shortcuts suppressed with `raw_engine_only` and
  `no_board_reason`
- opening/source context held to `source_context_only`
- wrong S21 owner, anchor, route, or scope suppressed fail-closed
- renderer strength requests suppressed with `renderer_not_allowed`

The second runtime expansion slice adds the S15/S16 passer creation /
suppression cluster with S13/S14 structural support. It does not add projection
admission law. It consumes already admitted S13, S14, S15, and S16 projection
claims and adds selection-layer checks for:

- exact admitted S15 selected as `shouldLead` only when lower carriers include
  same-owner Root `candidate_passer` and Witness same-candidate
  creation-route support on frozen `s13_wing_damage` or `s14_chain_base`
  routes
- exact admitted S16 selected as `shouldLead` only when lower carriers include
  defender-owned enemy Witness `passed_pawn_entity_state` plus route-specific
  same-owner certification support: `FortressDrawCertification` for
  `blockade_hold`, `short_run_slider_gate_restriction` plus
  `PerpetualCheckHolding` for `restriction_hold`, and `PromotionRace` for
  `non_losing_race`
- S13/S14 retained as support for S15 only when owner, candidate anchor, and
  scope match the selected S15 claim
- S13/S14 never becoming S15 truth owners
- same-candidate S13/S14 support cannot outrank the admitted S15 owner even
  when its impact score is higher
- existing-passer-only, blocker-shell-only, split-anchor, forged carrier-kind,
  and unsupported-route shortcuts suppressed with typed reasons
- S15/S16 same-bucket conflicts remaining fail-closed when beneficiary,
  defender, or side-to-move is ambiguous or incoherent across the rival pair
- S16 same-enemy suppression remaining fail-closed when `owner` and `defender`
  identify the same side
- raw engine, source context, and retrieval shortcuts staying non-authoritative
- renderer strength requests suppressed with `renderer_not_allowed`

The third runtime expansion slice adds the S17/S18/S19/S22 conversion /
simplification / hold cluster. It does not add projection admission law. It
consumes already admitted S17, S18, S19, and S22 projection claims and adds
selection-layer checks for:

- exact admitted S17 selected as `shouldLead` only when lower carriers include
  same-owner Witness `same_piece_liability_anchor_seed` plus a typed
  `same_piece_repair_route_seed` or `same_piece_exchange_relief_seed` carrier
  and `liability_relief_certified` projection evidence
- exact admitted S18 selected as `shouldLead` only when lower carriers include
  same-owner Witness `bishop_pair_state` plus route-specific Certification
  support for `bishop_pair_to_initiative`, `bishop_pair_to_structure`, or
  `bishop_pair_to_material`
- exact admitted S19 selected as `shouldLead` only when lower carriers include
  same-owner Delta `TradeInvariant` plus route-specific Certification support
  for `trade_invariant_to_material` or `trade_invariant_to_hold`
- exact admitted S22 selected as `shouldLead` only when lower carriers include
  same-owner certified `fortress_draw_hold` or `perpetual_hold` support
- declared Projection evidence must be bound to the same owner, anchor, route,
  and scope as the selected S17/S18/S19/S22 claim
- S19 simplification remaining below S17 liability relief, S18 bishop-pair
  conversion, and S22 certified hold when owner, anchor, and scope collide
- conversion claims staying below certified hold unless result/material impact
  and the `bishop_pair_to_material` route with bound
  `bishop_pair_material_conversion_certified` evidence justify conversion
- generic relief wording, unbacked conversion, source/retrieval, raw engine,
  wrong owner/anchor/route/scope, and renderer-strength shortcuts staying
  fail-closed

The fourth runtime expansion slice adds the S01/S02/S03/S04 king-attack
cluster. It does not add projection admission law. It consumes already admitted
S01, S02, S03, and S04 projection claims and adds selection-layer checks for:

- exact admitted S01 selected as `shouldLead` only when lower carriers include
  same-owner Witness `available_lever_trigger`, Witness
  `pawn_push_break_contact_source`, Object `AttackScaffold`, Certification
  `CertifiedKingSafetyEdge`, and bound `king_wing_storm_route_certified`
  projection evidence on `same_wing_contact` or `attack_edge_same_king`
- exact admitted S02 selected as `shouldLead` only when lower carriers include
  same-owner Object `AttackScaffold`, Certification `CertifiedKingSafetyEdge`,
  and bound `king_ring_concentration_route_certified` projection evidence on
  `direct_piece_concentration` or `lane_strengthened_concentration`
- exact admitted S03 selected as `shouldLead` only when lower carriers include
  same-owner Witness `diagonal_lane_only`, Object `AttackScaffold`,
  Certification `ComparativeKingFragility`, Certification
  `CertifiedKingSafetyEdge`, and bound
  `diagonal_king_attack_route_certified` projection evidence on
  `king_facing_diagonal_entry` or `fragility_linked_diagonal`
- exact admitted S04 selected as `shouldLead` only when lower carriers include
  defender-owned Object `KingSafetyShell`, same-owner Certification
  `CertifiedKingSafetyEdge`, and bound `king_shelter_breach_route_certified`
  projection evidence on `shell_payload_breach` or `support_break_breach`;
  `support_break_breach` also requires same-owner Witness
  `diagonal_lane_only`
- owner, defending king anchor, defender, route, and scope clarity; king-attack
  projection fails closed when `owner` and `defender` identify the same side
- S01 storm, S02 concentration, S03 diagonal lane, and S04 shelter breach
  remaining distinct, nonredundant claims when route or anchor differs
- certified result/conversion owners outranking king-attack projections where
  their Certification bucket justifies it, while raw engine eval never does
- generic attack wording, king-side vibes, source/retrieval motif tags, raw
  engine eval, wrong owner/anchor/route/scope, and renderer-strength shortcuts
  staying fail-closed

The fifth runtime expansion slice adds the engine-certified eval swing versus
board-explainable Sxx conflict boundary. It does not open a raw Engine E path.
It consumes only already bounded `EngineCertification` evidence refs attached
to Certification-layer claims and adds selection-layer checks for:

- raw engine eval swing always suppressed with `raw_engine_only` and
  `no_board_reason`
- bounded `EngineCertification` influencing ranking only when the claim is a
  Certification claim with same-root `Certification` evidence and a
  same-owner/anchor/route/scope typed Root, Witness, Object, or Delta board
  reason
- engine-certified eval swing without that typed board reason suppressed with
  `no_board_reason`, even when its numeric swing is larger than an admitted
  Sxx projection
- board-explainable admitted Sxx projection outranking opaque engine-certified
  eval swing
- engine-certified result/material Certification outranking Sxx only when
  result/material impact, same-root Certification, and same-binding typed
  board reason justify the `mustLead` bucket
- stale, wrong-node, wrong-FEN, wrong-route, and wrong-engine-config
  `EngineCertification` refs suppressed before ranking via
  `stale_evidence`, `wrong_owner`, `wrong_anchor`, `wrong_route`, or
  `scope_mismatch`
- MultiPV ambiguity and untyped mate/cp score comparison staying
  `deferred`, `support_only`, or `no_board_reason` unless already normalized
  and admitted by `CertificationEngineEvidenceContract`
- renderer-strength shortcuts staying fail-closed; renderer cannot turn an
  eval swing into best-move, winning, or forced-result wording

The sixth runtime expansion slice adds context-only fallback breadth for
opening, motif, endgame-study, and retrieval source-context families. It does
not open raw source ingestion, renderer prose, API, or frontend wiring. Source
family internals must first normalize through `SourceContextAdapter` to
`SourceContextCandidate`; the selector side sees that candidate only through
`SourceContextClaimBoundary`, which emits only `ClaimLayer.SourceContext`
claims. The selector consumes only those already prepared source-context claims
and adds selection-layer checks for:

- opening context selected as `contextOnly` only when no exact-board lead
  exists; opening context never becomes best-move, theory-truth, forced-line,
  result, or current-position proof; selector fallback requires a canonical
  `opening-position:*:canonical` source ref and suppresses
  `opening-position:*:ambiguous`
- motif examples selected as `contextOnly` only when a `motif-example:*`
  source ref matches an exact `motif-detector-carrier:*` evidence ref
- motif line-test refs retained only as source context after that exact
  source/carrier parity, never as proof ownership
- endgame-study context selected as `contextOnly` only when a bound exact
  `endgame-study-applicability:*` material/placement carrier matches the
  `endgame-study:*:applicable` source ref; Phase 4 fixture ids may differ from
  study ids only when the exact carrier route metadata binds the fixture to the
  study id and carries `scope=exact_endgame_applicability`; unverified or
  scope-less fixture ids are suppressed; study context cannot claim win, draw,
  loss, forced line, or forced conversion
- endgame technique refs retained only as source context after that exact
  applicability, never as proof or result ownership
- retrieval selected only as non-authoritative reference context; retrieval
  snippets require `retrieval-example:*` source refs and never become
  current-position truth
- retrieval illustration and line-test refs retained only as source context,
  never as proof ownership, recommendation, verdict, or display-candidate
  citation
- source context downgraded to support/context when an exact-board lead exists
  and never outranking that lead
- ambiguous opening transpositions suppressed with
  `ambiguous_transposition`
- motif tags without an exact detector carrier suppressed with
  `forbidden_shortcut` and `no_board_reason`
- motif detector-carrier id mismatch suppressed with `forbidden_shortcut` and
  `no_board_reason`
- deferred/helper-required motif ids and certification-only motif shortcuts
  without a certified carrier suppressed before they can enter line explanation
- deferred endgame-study tokens, result/oracle wording, and forced-conversion
  technique shortcuts suppressed before they can enter line explanation
- gameContext, practicality, tablebase, and endgame result-service source
  families staying deferred or rejected with `forbidden_shortcut`
- renderer-strength shortcuts staying fail-closed; renderer cannot turn source
  context into current-position truth, best move, result, forced line, or
  theory-proof wording

Synthetic selector and surface contract rows live in:

- `modules/commentary/src/test/resources/commentary-corpus/planner-expectations.jsonl`
- `modules/commentary/src/test/resources/commentary-corpus/surface-expectations.jsonl`

Executable contract tests live in:

- `modules/commentary/src/test/scala/lila/commentary/selection/CommentarySelectionContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/PreparedVariationEvidenceContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/CandidateLineSelectionContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/source/SourceContextAdapterContractTest.scala`
- `modules/commentary/src/test/scala/lila/commentary/selection/SourceContextClaimBoundaryTest.scala`

The rows are executable parity fixtures for selector inputs and expected
outline buckets/reasons. They do not replace upstream FEN/PGN-backed
exact-board proof in Root, Object, Delta, Certification, or Sxx admission
tests.

The selector contract tests cover:

- exact-board lead beats opening context
- raw engine swing without bounded Certification is suppressed
- Engine E only influences selection through Certification
- `EngineCertification` without same-root `Certification` evidence is
  suppressed
- S24 is not treated as generic tactic owner
- S24 requires both same-target forcing and conversion evidence
- admitted Sxx can lead only with exact lower carrier/evidence
- raw engine refs cannot be smuggled through another layer or Sxx lower carrier
- raw engine lower carriers cannot be smuggled through non-Projection board
  claims
- source context refs cannot be smuggled through a board claim
- unbound lower carriers cannot admit an Sxx projection
- `support_only` and `deferred` cannot become lead
- context-only output is allowed only when no stronger exact-board lead exists
- opening context-only fallback is non-authoritative
- motif context-only fallback requires a matching detector carrier
- motif line-test context requires a matching detector carrier and remains
  non-authoritative support/context
- endgame-study context-only fallback requires exact applicability, including
  route-bound study/fixture reconciliation when ids differ
- endgame technique line-test context requires exact applicability and remains
  non-authoritative support/context
- retrieval context-only fallback remains non-authoritative reference
- retrieval illustration line-test context remains subordinate to exact-board
  variation evidence and cannot transfer proof from the retrieved example
- exact-board lead bounds source context to support/context
- ambiguous opening transpositions are suppressed
- motif tags without detector carrier are suppressed
- endgame-study result language is suppressed
- retrieval truth-promotion is suppressed
- deferred gameContext/practicality/tablebase/result-service source families
  are suppressed
- retrieval snippets cannot become current-position truth
- wrong owner, anchor, route, or scope suppresses the claim
- ambiguous side-to-move, beneficiary, and defender fail closed
- competing Sxx claims require nonredundancy by owner, anchor, route, and scope
- lead bucket priority is applied before impact score
- S07/S08/S21 initiative/release/counterplay cluster selection keeps S07 lead,
  nonredundant S21 support, and adjacent S08/source/raw-engine false leads
  suppressed
- S15/S16 passer creation/suppression cluster selection keeps S15/S16 lead
  admission tied to same-candidate or same-enemy-passer lower carriers while
  keeping S13/S14 support below S15 truth ownership
- S17/S18/S19/S22 conversion/simplification/hold cluster selection keeps
  relief, conversion, simplification, and hold lead admission tied to exact
  lower carriers while preventing S19 from stealing S17/S18/S22 ownership
- S01/S02/S03/S04 king-attack cluster selection keeps storm,
  concentration, diagonal attack, and shelter breach lead admission tied to
  exact lower carriers while preventing generic attack/source/raw-engine truth
  promotion
- engine-certified eval swing cannot lead without same-root Certification and
  same-binding typed board reason
- board-explainable admitted Sxx can beat larger opaque engine-certified eval
  swing
- engine-certified result/material Certification can beat Sxx only with typed
  board reason support
- non-Certification eval swing is ignored for ranking
- cross-layer same-key carrier/support claims are not duplicate-suppressed
- every `S01-S25` start-ready band has a selection closure path when already
  admitted with exact owner, anchor, route, scope, row-specific lower-carrier
  role shape, plus its frozen allowed projection evidence kind; this is a
  selection-only closure and does not widen projection admission law
- the global closure keeps S06 tied to same-route
  `SpaceBindRestrictionCertification`, and S16 tied to the route-specific
  `FortressDrawCertification` / `PerpetualCheckHolding` / `PromotionRace`
  support required by the current projection authority
- S07/S08 and S15/S16 rival suppression is selector-derived from typed admitted
  claims, not supplied by suppression hints
- opening source-context fallback rejects extra best-move, theory-truth,
  current-position-proof, forced-line, or result-style source refs even when a
  canonical opening-position ref is also present
- normalized source-context handoff shape emits only source-context claims
  capped at `context_only`; selection does not consume raw source-family rows
- adapter rejects source-family forbidden claims before selection sees a
  normalized candidate
- renderer cannot upgrade `wordingStrengthCap`
- selected claims cannot carry a stronger cap than the computed outline cap;
  source-context selected claims remain capped at `context_only`
- renderer cannot upgrade source context into current-position truth
- rejected source context is not emitted as outline context
- selected source context records `source_context_only` or
  `retrieval_non_authoritative` as a soft context reason, not as a suppressed
  claim
- cross-family candidate line selection requires same-claim candidate plus
  defender-resource proof for strong primary public explanation, filters
  `CommentaryOutline.variationEvidence` to that selected strong line set, keeps
  candidate-only proof weak/context and non-public, blocks SourceContext and
  RawEngine proof ownership, uses source line-test refs only as annotations of
  selected exact-board proof ids, exposes a strong-only language-neutral
  `PlanAnnotationSelection` handoff through outline/plan without prose or
  source-string chess parsing, keeps retrieval illustrative, keeps failed
  tempting/premature/release-risk proof non-primary, returns silence for
  retrieval-only/source-only line tests, and strips internal debug handles from
  the public model result

Current surface rows are renderer safety scaffolds only. They freeze that the
renderer cannot admit a lead, rank claims, convert source context to truth,
discard suppression, or upgrade `wordingStrengthCap`; live renderer wiring
remains outside this slice.
