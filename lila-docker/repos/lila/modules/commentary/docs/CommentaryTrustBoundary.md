# Commentary Trust Boundary

This file is the canonical trust-risk map for Chesstory commentary. Use it for
false positives, overclaim control, support-only reinflation, fallback rewrites,
surface-consumption risk, and lesson-readiness gating.

It complements `CommentaryPipelineSSOT.md` and `CommentaryTruthBoundary.md`.

## Current State

The live product trust boundary is MoveReview-only. Removed product surfaces,
whole-game replay, and historical diagnostics are not runtime trust
infrastructure. Their helpers are confined to `modules/commentaryTools/src/test`;
they must not supply MoveReview release authority unless a new runtime audit
explicitly reopens that boundary. Historical replay tooling that still needs
retired data must convert to a compact, non-authority carrier before calling
shared builders, and runtime trust/signoff code must not consume retired DTOs
directly.

Current operating posture:

- maintain existing exact-board promoted slices
- keep umbrella strategic expansion closed
- allow new runtime authority only through proof contracts and the
  claim-authority kernel
- keep support-only/deferred/latent carriers internal
- keep lesson authority deferred unless exact scoped-takeaway rules admit it

## Authority Ladder

Internal trust decisions use this ladder:

| tier | user-facing meaning | allowed use |
| --- | --- | --- |
| `CertifiedOwner` | exact proof may own the claim | main MoveReview explanation |
| `SupportedLocal` | bounded local evidence may speak | qualified local reading/support |
| `DiagnosticOnly` | useful for review, not release | traces, corpus review, tests |
| `Suppressed` | blocked | no released claim |

`ClaimAuthorityResolver` owns final authority decisions from packet, plan, and
truth contract. `PlannerClaimAdmission` is the planner adapter.

The planner may rank and select questions, but it must not decide proof-family,
source, scope, tactical-veto, or fallback authority by itself.

## Plan Promotion Trust Boundary

`PlanEvidenceEvaluator` owns strategic plan promotion. `ProbeBacked`,
`TranspositionAligned`, `StructuralOnly`, `PvCoupledOnly`, `Deferred`, and
`Refuted` are typed evaluation outcomes, not downstream string matches.

Downstream user-facing consumers must read the typed
`StrategicPlanEvidenceView` projected into `NarrativeContext`. Raw
`StrategicPlanExperiment.evidenceTier` strings and the
`probe_backed:validated_support` marker are diagnostic/compatibility carriers,
not release, provenance, quantifier, stability, or outline-selection
authority.
`PlanSemanticsContract` owns plan-semantic policy that can affect user-facing
implication: strategic idea-kind mapping, subplan probe-purpose routing, and
thematic fallback eligibility. `PlanTaxonomy.ThemeResolver` remains a tag/parser
and proposal/debug/compatibility classifier, not a user-facing authority source
from aliases, plan names, evidence-source strings, execution steps, or
failure-mode prose.
`PlanClaimBoundary` is the typed lifecycle boundary for plan claims. It keeps
`PlanProposal` source provenance separate from `PlanSupport`,
`AdmittedPlanClaim`, and `RenderedPlanText`, so alias-derived themes and default
subplans can remain diagnostic/proposal metadata without becoming supportable
or admitted user-facing plan claims.
Only typed `ProbeBacked` and `TranspositionAligned` evaluated plans can enter
selected main-plan authority. `TranspositionAligned` is a separate provenance:
it must come from legal PV replay through `TranspositionPvAligner`, a terminal
`WeaknessTargetProfile` match for an expected target square, positive
attacker-minus-defender control, sufficient line horizon, exact
`target:<square>`/`weakness_target:<square>` evidence-token parsing, and
mover-loss/mate veto checks. It must not be rewritten as `ProbeBacked` and does
not satisfy exact proof contracts or `check_qualifying`. `StructuralOnly` and
`PvCoupledOnly` may remain in diagnostics or support context, but they cannot
own a main claim or satisfy `check_qualifying`. Structural-only plans must also
remain structural-only in compatibility carriers; they must not serialize as
`evidence_backed`.
`PlanEvidenceEvaluator.isMainAdmittedPlan` is the shared typed predicate for
selected main-plan admission and downstream payload/sanitizer sanity checks;
`TranspositionAligned` requires both a transposition proof id and
`transposition_aligned` typed provenance. A raw proof id, evidence tier, or
compatibility marker alone is not enough.
`UserFacingPayloadSanitizer` also treats that marker as non-authoritative:
MoveReview plan payloads are retained only when `CommentaryApi` passes a
matching typed `EvaluatedPlan` whose eligibility is main-admitted and whose
evidence id set is non-empty: support probe ids for `ProbeBacked`, or
transposition proof ids plus `transposition_aligned` provenance for
`TranspositionAligned`. Cached/default sanitizer paths and chronicle moments
have no typed admission carrier and therefore fail closed for
`mainStrategicPlans` and their plan-experiment metadata.
`MoveReviewPlayerPayloadBuilder` additionally treats probe/test/candidate
request wording inside plan hypothesis detail fields as internal diagnostics,
not player prose by consuming
`UserFacingSignalSanitizer.sanitizePlanDetail`. It may replace such suppressed
detail text with the typed contract objective or admitted plan name only when
the detail field was non-empty; this cleanup cannot turn a detail-less
support-only plan into a new advanced row. Its weakness-target, prophylaxis,
and promoted/practical sibling gates consume only `PlanProposal.fallbackTheme`
and `supportKind`; inferred proposal themes from plan names, execution steps,
or failure-mode prose cannot create or suppress player rows. A promoted-plan
`Plan status` row may consume `StrategicPlanExperiment` flow-state booleans for
stable-reply, later-position, counter-break, or move-order wording, but only
after `PlanEvidenceEvaluator.isMainAdmittedPlan` has admitted the plan and the
experiment exact plan id matches the promoted plan. If the promoted plan has a
typed support kind, the matching experiment must carry the same support kind
through `PlanClaimBoundary.PlanProposal`; broad theme matches and same-subplan
sibling matches are not enough. `evidenceTier`, support/refute counts, and raw
probe metadata do not become authority tokens or proof claims through that row.
`UserFacingPayloadSanitizer` applies the same detail filter to retained
sanitized plan hypotheses and strategy-pack plan priorities/risk/long-term
focus, preventing cached/internal support carriers from reintroducing raw
probe/test/candidate-request text as strategic guidance.
`StructuralOnly` and `PvCoupledOnly` evaluated plans may speak only through
bounded practical-guidance rows on `moveReviewPlayerSurface`; they do not
become selected main plans, `check_qualifying` inputs, retained plan metadata,
or owner claims. Their preconditions and execution steps may also appear as
`Practical objective` and `Practical steps` advanced rows when sanitized
surface capacity allows, but those rows remain practical guidance rather than
proof authority. Those practical advanced rows are deduplicated against
promoted plan siblings: a matching top-level theme or at least 70% overlap with
the practical plan's execution steps keeps the softer row silent.
The one cache-hit exception is a previously sanitized MoveReview response:
`sanitizeCachedMoveReview` may preserve cached `mainStrategicPlans`,
matching plan experiments, continuity token, and ledger only when the cached
response already has `moveReviewPlayerSurface` and all retained plan evidence
source markers have been removed. Marker-bearing cached plans still fail
closed.

Probe validation separates chess evidence from bookkeeping:

- hard failures: missing or mismatched FEN/probed move/id for a board-bound
  request, malformed FEN or probed move echo, missing required board signal,
  missing purpose contract, depth-floor missing or unmet,
  mate/refutation/cp-loss beyond contract
- soft diagnostics: purpose/objective label drift and hash/fingerprint echo
  drift

Soft diagnostics may remain in audit output, but they must not by themselves
block a board-valid supporting probe. `alternativeDominance` is ranking
metadata and must not be reported as refutation.
Multi-move probe requests are board-bound only when the result certifies a
`probedMove` or candidate move that is one of the requested moves. Unknown
request purposes have no authority contract and fail closed even when explicit
required signals are present; unknown-purpose results must not refute plans
through the default 0cp bound. Probe requests with explicit plan id, seed, or
plan name must not relink by substring or by one matching sibling field. If
more than one explicit binding is supplied, every supplied binding must match
the same plan hypothesis. Refutation-purpose probes are negative tests: they
may mark a plan `Refuted`, but a non-refuting result does not become positive
`ProbeBacked` support without a separate affirmative support probe.
Client-generated `keyMotifs`, `l1Delta`, and `futureSnapshot` values marked by
`generatedRequiredSignals` or `motifInferenceMode=purpose_only` /
`purpose_plus_compat` do not satisfy required authority signals. Unknown
required probe signals and unknown/no-contract probe purposes fail closed.
`keyMotifs` remain display/diagnostic text. Forcing-modality promotion may only
consume canonical `motifTags` values such as `forcing`, `exchange`, `trade`, or
`simplification`; absent tags fail closed for motif-based forcing.

Exact-family trust is witness-bound. A subplan id such as
`central_break_timing` may explain the plan taxonomy, but it must not open the
exact central-break owner path unless the exact witness is present; otherwise
the typed evidence may only support the bounded generic plan-advance path.

## Proof And Packet Boundary

`PlayerFacingClaimPacket` is the runtime carrier for exact claim candidates.
`ProofContractRules` decides which proof families can be certified or supported
locally. For `ExactSlice`, the trust boundary is the typed
`PlayerFacingProofPathWitness.exactSliceProof` ADT produced by the
board/probe witness path. Generic owner, anchor, structure, continuation, or
prose terms are diagnostics and surface wording support only; they cannot be
parsed or promoted into exact-slice proof authority.

Required proof witnesses are fail-closed contract predicates, not descriptive
labels. `NoTacticalVeto` fails when tactical veto or missing tactical-context
codes are present, and `ClaimOnlySurface` fails unless the packet remains in the
bounded weak-main claim surface. A required witness that is absent or contradicted
must produce a failure code before any supported-local or certified admission.
`ProphylacticRestraint` exact proof uses board/resource tokens such as a square
or `denied_resource:<class>` from the prevented-plan resource class. Arbitrary
plan labels such as "counterplay window" are prose only and fail the exact-slice
contract.
For favorable-exchange move-local packets, `defender_trade` and
`bad_piece_liquidation` must also carry board-replayed structure-transition
witnesses and the matching typed exact-slice relation proof before
`SupportedLocal` admission. A defender-trade packet must prove that the exchanged
defender actually defended a non-exchange target and that the defense relation
disappears after the replayed recapture with the target's defender count
strictly reduced. If the specific defender is gone but a newly opened
replacement line preserves the defender count, exact defender-trade ownership
stays closed and the line may only degrade to generic exchange or
material-transition guidance. The target set is limited to declared
focal/weakness targets; arbitrary defended pieces and the exchange square are
not fallback targets. A bad-piece liquidation packet must prove a constrained
bishop on the current board and a legal bishop capture followed by recapture.
Broad exchange-forcing source identity, owner terms, structure terms, and prose
cannot substitute for `PlayerFacingExactSliceProof.DefenderTrade` or
`PlayerFacingExactSliceProof.BadPieceLiquidation`. If these fail, the
owner/support packet is suppressed; safe exchange prose can only degrade to
line-consequence or practical guidance.
`overload`, `deflection`, `discovered_attack`, `double_check`,
`back_rank_mate`, `mate_net`, `greek_gift`, `fork`, `hanging_piece`,
`trapped_piece`, `domination`, `stalemate_trap`, `perpetual_check`,
`zwischenzug`, `decoy`, `xray`, `clearance`, `battery`, `pin`, `skewer`, and
`interference` have a separate `relation_transformation` supported-local
contract. It admits only typed exact relation proofs produced from the analyzer
witness and matched against the selected structured `StrategyRelationSupport`;
support prose, relation labels, fact strings, or helper names cannot open this
path. Greek-gift and perpetual-check continuation support is taken from
structured PV move lists, and the targetless king/mate/draw/pattern detectors
do not borrow material-target fallback squares. The claim remains bounded
support, not certified owner authority.
Support-level activity analysis uses the same constrained-bishop rule, so
same-color central pawn count alone is no longer trusted even for bad-bishop
ranking hints.

The shared favorable-exchange replay boundary is `MoveReviewExchangeAnalyzer`.
Its PV identity source is the raw engine `VariationLine.moves` list; parsed PV
metadata is accepted only as a no-raw-moves fallback and cannot supersede a
supplied engine line.
The owner witnesses consume only proof-sized legal prefixes from that replay:
three plies for defender trade, and the direct or delayed legal bishop
capture/recapture prefix for bad-piece liquidation. Illegal or stale PV text
after that proven prefix is not trusted or emitted as continuation proof, but it
does not silence the already proven local witness. Illegal text before the
required prefix still fails closed.
Player-facing SAN citations, decision comparisons, and close-alternative labels
share that boundary: they may render SAN from the raw UCI line, but stale
`parsedMoves` cannot rename the engine's first move or preview branch.
`StrategicSemanticObservationPipeline` may expose defender-trade or
bad-piece-liquidation observations as typed selector evidence when that analyzer
can replay the branch legally and the semantic context supplies the reviewed
`playedMove`; a missing played move fails closed instead of borrowing the top
PV first move. Those observations are not proof packets.
The same analyzer-owned `RelationWitness` read-model now also carries overload,
deflection, discovered-attack, double-check, back-rank mate, mate-net, Greek gift,
zwischenzug, fork, hanging-piece, trapped-piece, domination, stalemate-trap,
perpetual-check, x-ray, clearance, battery, pin, skewer, interference, and
decoy relation observations. These rows are
selector/support evidence only: they prove local board relations for ranking and
softer phrasing, but they do not create `CertifiedOwner` or `SupportedLocal`
authority unless a separate proof contract admits that family. The shared
semantic producer context reuses one bounded top-PV replay and one extracted
relation-witness set; the relation producer consumes only implemented catalog
kinds through that shared context, so trust decisions do not depend on separate
replay, witness, or local kind-filtering attempts. Implemented extraction is
registered through `MoveReviewExchangeAnalyzer.ImplementedRelationWitnessTemplates`;
future relation motifs must enter through that registry and then through
`RelationObservationCatalog`, not through motif-local replay or surface parsing.
When an explicit target list is present, the relation witness is target-bound and may not fall
through to a material-target fallback, including discovered-attack target
selection. Double-check support requires a replayed reviewed move that leaves
the opposing king checked by at least two checker squares, and explicit target
lists close this king-targeted relation instead of acting as fallback targets.
Back-rank mate support reuses the existing tactical pattern detector and
requires the replayed reviewed move to end in `checkMate` with the defender's
king still on the back rank; explicit target lists close this king-targeted
relation instead of acting as fallback targets.
Mate-net support reuses the existing mate-required tactical pattern detectors
other than back-rank mate, requires the replayed reviewed move to end in
`checkMate`, and keeps the detector id as support-only evidence; explicit
target lists close this king-targeted relation instead of acting as fallback
targets.
Greek-gift support reuses the existing tactical pattern detector, requires a
legal reviewed bishop sacrifice on h7/h2 that gives check, and accepts only
immediate kingside support or support that appears by replaying the supplied PV
continuation. Explicit target lists close this pattern relation instead of
acting as fallback targets.
Fork support requires a replayed move attacking at least two bound targets.
Hanging-piece support requires the replayed mover to attack an
undefended bound target. Trapped-piece support requires zero legal safe or
defense-preserving reply routes for a bound non-pawn target. Domination support
requires the replayed mover to control a bound non-pawn target whose legal
reply routes are reduced to at most one. Stalemate-trap support requires the
replayed reviewed move to leave the defender stalemated. Zwischenzug support
requires a replayed forcing check, legal reply, and non-pawn material payoff on
the same line. Perpetual-check support requires a legal continuation line whose
checking side repeats the same checking-position key. Pin and skewer support use the same replayed relation boundary and
require long-range ray geometry through the pinned/front piece and the piece
behind it. Invalid
explicit target tokens close the candidate rather than
reopening structural or material fallback. Downstream selection consumes relation
rows through `RelationObservationCatalog`, which centralizes the mapping from
witness kind to semantic id, selector source, strategic idea kind, readiness, and
confidence, bounded public row/text labels, and producer admission. The runtime
relation inventory has no remaining deferred relation kind; unknown relation
names still fail closed. Legacy motif-prefix prose, theme-keyword and canonical
motif-term prose, and motif appears/fades delta prose keep old relation-shaped
labels generic or suppressed instead of treating them as catalog evidence.
Legacy trapped-piece, domination, and zwischenzug helper notation is lowered to
piece-mobility, key-square restriction, or move-order caution wording unless
the analyzer relation witness is present. Legacy stalemate-trap and
perpetual-check helper notation is suppressed unless the matching replayed
witness exists.
Threat-summary labels in `NarrativeContextBuilder` consume the same boundary,
so legacy relation-shaped motifs from `ThreatAnalysis` do not surface as raw
key-threat labels.
Strategy-pack and structure-arc piece-activity evidence no longer promotes
legacy trapped-piece activity into relation evidence. The user-facing sanitizer
applies the same boundary to legacy strategy-pack evidence lists, removing
legacy relation-shaped motif terms.
The
same legacy-relation check blocks relation-shaped tags from raising the context beat
into high-tension tactical opening tone; that tone still needs a non-legacy
motif or concrete threat evidence.
The generic motif-prefix allowlist also blocks legacy relation-shaped tags
before accepting a motif signal, so later prefix consumers inherit the same
boundary.
Generic fact-corroboration helpers share that catalog check: a tactical fact can
still support implemented or generic motifs such as battery, but cannot certify
a legacy relation-shaped tag as board-proven motif support.
Only implemented descriptors can satisfy runtime evidence admission.
`StrategicIdeaEvidence` also strips unknown relation names instead of preserving
them as relation candidates. Unknown relation witnesses fail
closed at semantic observation emission instead of falling back to
target-pressure or other selector sources.
The relation semantic producer uses the implemented catalog relation set rather
than maintaining a separate relation-kind allowlist.
The analyzer-owned relation target is preserved as `targetSquare` through the
semantic observation, selector evidence, and selected `StrategyIdeaSignal`;
the selected idea also keeps the representative `relationKind`, so public
projection does not re-pick a different relation merely because of catalog
ordering.
If a selected relation carrier lacks an analyzer target, public target fallback
also comes from that relation's catalog descriptor; the payload builder does not
own motif-specific target order rules. Legacy evidence-only carriers without a
selected `relationKind` cannot promote their generic `targetSquare` into a
public relation target; they use only the catalog fallback over relation focus.
The selector prioritizes that relation's source ref and semantic fact before
generic evidence truncation for selector ranking and diagnostics, but public
projection requires the selected implemented `relationKind` plus matching
structured `relationSupport`.
When a stronger idea family or lower-priority relation would otherwise consume
the visible idea slots, the selector may reserve or replace the secondary idea
with the highest-priority implemented relation candidate from the selected and
merged candidate sets, but only if that candidate still carries
relation-specific focus and matching structured `relationSupport`. This keeps
board-replayed relation support player-facing, but it is presentation/support
preservation, not proof promotion.
Catalog-order fallback is not a public relation admission path for legacy ideas:
source/fact string matches and non-empty `relationFocusSquares` remain internal
support unless structured relation support is attached.
The selector also preserves `relationFocusSquares`; surface projection uses
only that relation-specific focus and never borrows merged idea focus. If a
selected or legacy carrier lacks relation-specific focus, the public relation
row stays silent. A selected relation target is used only if it is still present
in the relation focus squares, then falls back to the catalog descriptor's
target policy over that same relation focus. Selector merge does not upgrade ordinary focus squares
into relation focus or ordinary target squares into relation target after
evidence grouping.
Relation support wording is centralized in `RelationSurfaceText`; it may read
the structured `relationSupport` carrier to explain the already admitted
support row, to enrich selector idea/focus text used by `StrategyPack` digest,
long-term focus, evidence hints, and prompt hints, and to produce the bounded
player-surface summary cue for why/next-check review wording, but only after the
selected relation kind and relation focus match that carrier. It must not parse
`evidenceRefs`, dynamic fact strings, or prose, and the enriched wording remains
support-only. It cannot widen relation admission, target selection, or
proof-family authority. Its player-row and summary-cue formatters are also
fail-closed: without matching structured `relationSupport`, or when relation
focus and support focus diverge, they return no relation evidence text even if a
catalog label, relation kind, or focus squares are present. Role,
fork target-role, axis, absolute-pin, tactical-duty, king-pattern cycle, lure,
and execution wording is display detail carried by the analyzer support model,
not a new authority tier.
`StrategyPackBuilder` may keep a strategy pack alive from relation-only
selector ideas, but only after that same replay-backed semantic producer and
catalog mapping have emitted typed evidence. The pack's existence is therefore
support carriage, not proof authority.
When several implemented relation witnesses merge under the same selector idea
family, the representative relation is chosen by catalog-owned structured
selector priority and narrow typed-support priority before confidence. This
prevents exact move-order, mobility, draw-resource, exchange-transformation, and
absolute-pin witnesses from being hidden by a more generic material or line
relation from the same replay, while still requiring matching `relationSupport`
and relation focus before any player-surface row is admitted. The absolute-pin
priority comes from `StrategyRelationSupport.absolutePin == true`, not from a
pin label, helper name, dynamic fact string, or prose.
They may improve practical ranking or softer labels; owner release still
requires the contract witnesses and authority resolver admission above.
For defender-trade, bad-piece, and relation-transformation owner witnesses,
`PlayerFacingTruthModePolicy` now uses the analyzer `RelationWitness` typed
details as the shared branch read-model before adding owner-only transition
terms, so semantic support rows and owner packets are not built from divergent
branch strings. Policy code consumes analyzer branch-extraction helpers instead
of reading relation detail internals directly; shared owner-seed and transition
term expansion comes from the projection's structured `relationSupport`, not
from policy-local reads of relation facts/focus squares or dynamic support fact
strings. Supported-local summary rows for those relation transformations
additionally require `ClaimAuthorityResolver` admission, proven same-branch
state, stable persistence, and no release risks before
`MoveReviewSupportedLocalSurfaceRows` emits `strategic_relation` authority;
`RelationSurfaceText` owns that proof-backed row wording so the builder does
not add motif-specific relation text branches.
Other implemented relation witnesses also carry typed king,
attacker, defender-or-cleared-square, mate-pattern, material-target, line-target,
or lure-and-win details;
they remain support authority unless a separate proof contract admits them, but
downstream code must still consume those details through analyzer helpers
instead of reparsing support facts.
Branch keys and `branch:*` fact tokens are formatted by
`MoveReviewExchangeAnalyzer` helpers as well, so owner packets do not rebuild
the replay branch with policy-local UCI slicing or ad hoc pipe-joined strings.
Semantic observation projection now goes through the analyzer-owned
`relationProjectionFromWitness` carrier for relation kind, focus, target, and
support fact terms, so support rows cannot be steered by stale raw focus/target
values or fact strings when typed relation details are present.
Non-empty typed details must match the relation kind; a mismatch is rejected
before semantic emission rather than softened through raw witness fields.
Semantic fact projection uses the analyzer relation projection, preferring
canonical detail terms over raw witness facts for typed relation witnesses; raw
fact strings remain fallback-only for untyped witnesses. Owner/transition term
expansion reads structured `relationSupport` from that projection instead of
reusing dynamic fact strings. A typed-detail mismatch emits no branch,
owner-seed, or transition terms.
Owner seed-term expansion therefore keeps defender-trade and bad-piece owner
packets on the same typed support source instead of raw witness focus strings.
Discovered-attack support keeps enough selector priority to survive alongside
the generic clearance signal that often shares the same cleared ray; this is a
support-label preservation rule only and does not promote discovered attack to
owner proof.
When these rows reach `moveReviewPlayerSurface`, the only public relation
authority is `strategic_relation`: a sanitized relation key plus a required
square target. It may appear on the bounded summary cue and advanced support
row. It is support metadata for badges and chips, not a proof packet and not a
raw carrier that the frontend may reinterpret as ownership. The backend
projection requires a selected implemented `relationKind`, relation-specific
focus, and matching structured `relationSupport`; source/fact evidence strings
cannot mint a public relation row.
The summary cue may be ordered before plan/opening/practical rows as the
review's immediate why-this-move explanation. That ordering is presentation
priority only and does not upgrade the relation to proof authority.
That descriptor lookup and target-fallback check is centralized in
`RelationObservationCatalog`, including the rule that a selected `relationKind`
must not fall through to another catalog entry. The descriptor itself still
exposes the source ref and semantic fact pair for selector prioritization and
diagnostics, but those strings are not player-surface admission tokens.
Supplemental analyzer witness facts cannot mint catalog authority: dynamic
facts reject semantic observation ids, evidence-source ids, proof-source ids,
proof-family ids, and `source:` wire keys.
The final user-facing sanitizer also strips raw top-level
`strategyPack.strategicIdeas`, and `MoveReviewResponsePayload` applies the same
sanitizer to `moveReviewPlayerSurface` before wire serialization. Relation
evidence therefore does not leak as an alternate public carrier beside the
bounded surface row, and row `source` metadata remains internal even when
summary/advanced relation authority is retained.
Both backend and frontend sanitizers gate `strategic_relation` tokens against
the implemented relation catalog; syntactically valid but uncataloged relation
keys are not trusted, and untargeted relation tokens lose structured authority.
Backend sanitization preserves `strategic_relation` authority on
`moveReviewPlayerSurface.summaryRows` and `advancedRows`; probe rows and author
branches remain closed to relation authority. The analyse frontend decoder
mirrors that lane boundary for cached or legacy payloads, so relation tokens
outside summary/advanced player-surface rows are display text only and not
structured authority.
The analyse player-surface renderer may show relation and target chips from
that decoded authority, but it is a display consumer only; it does not parse row
text, fallback prose, or cached raw carriers to recover relation authority.
`PlayerProseBoundary` may use the full relation inventory only as a negative
helper-symbol denylist, rejecting leaked helper calls such as
`Overload(...)` or `Zwischenzug(...)`; that prose gate does not admit relation
authority or soften unknown relation status.
The same reviewed-move identity rule applies to `PlayerFacingTruthModePolicy`
owner witnesses for defender trade, bad-piece liquidation, and IQP inducement:
legal top-line replay is necessary but not sufficient when the reviewed move is
absent. To guarantee type-safe verification and prevent leakage of unverified positional claims, all IQP review groups (including inducement and simplification variants like `"C:iqp_simplification"`) are mapped directly to the unified `IqpInducementContract` descriptor. This ensures that any simplification candidate is aligned against the exact `IQPInducement` proof-family contract, rejecting missing-owner rows as `RejectOwnerMissing` rather than letting them pass based on superficial engine scores. Defender-trade owner visibility also ignores raw carrier prose:
`defender`/`guard` plus `trade`/`remove` text may feed support or practical
context, but exact defender-trade ownership needs typed family context and the
replayed defender-target witness. Generic plan-owner fallback must also strip
the exact defender-trade, bad-piece-liquidation, and queen-trade-shield
families unless the corresponding replay witness is present; those family names
alone are not release authority.
`queen_trade_shield` and bounded simplification exchange-square witnesses also
consume this analyzer: legal queen capture plus king recapture is required for
queen-trade shield, and legal immediate capture plus recapture is required for a
bounded simplification exchange square. Raw matching destination strings are
not trust evidence. For these replay-backed favorable-exchange owner packets,
the branch key is taken from the replayed witness line. Generic MoveReview
best-defense branch keys, best-defense move terms, exact-slice continuation
terms, and IQP inducement prefixes also use bounded legal replay; raw PV move
strings may remain display text, but cannot create branch proof, continuation
proof, isolated-pawn transition evidence, or tactical line-only proof. SAN
capture/check glyphs in candidate text are display hints unless a replayed
engine PV satisfies the tactical window. Exchange-forcing line evidence also
requires a replay-backed exchange packet with branch and continuation/structure
witnesses; line text containing `x`, `trade`, or `exchange` cannot create that
line hook. Exact-slice target witness labels (`fixed_target`,
`coordinated_target`, `weak_square`) and local file-entry structure markers are
formatted through `PlayerFacingExactSliceProofFacts`; target-focused support
markers and color-complex minor-piece attack markers use the same boundary.
They may support trace/prose context but do not replace the typed proof check.
Strategic delta text containing exchange/trade/simplification language also
cannot open `ExchangeForcing` truth mode without one of the
replay-backed exchange witnesses.
The same rule now covers quiet-move best-defense metadata and central-break
timing branch/PV witness terms. Break-prevention route evidence accepts only a
legal replayed played line, best-line prefix, or played+continuation splice;
probe reply coverage and reply-line/resource reads for strategic-plan
promotion/experiment code, authoring/candidate evidence, strategic feature
extraction, strategic ledger line candidates, and the heavy-piece, local-file,
route-network, two-axis, counterplay, and restricted-conversion proofs also go
through `MoveReviewExchangeAnalyzer` helpers rather than local `replyPvs` or
`bestReplyPv` parsing. `branch:*`, `best_branch:*`, and `exchange_square:*`
fact formatting also stays behind that analyzer boundary rather than
policy-local UCI string assembly. Defender-trade and bad-piece continuation
terms consume analyzer-built relation structure terms instead of policy-local
relation marker assembly.
Probe purpose predicates for those consumers are centralized in
`ThemePlanProbePurpose`; local raw purpose sets or substring checks do not carry
trust authority for route-validation, conversion, author-evidence,
played-counterfactual, or null-move threat semantics; author evidence
branch-cardinality gates and request budgets, required signals, objectives,
and horizons for these purpose families use the same helper boundary.
Latent hypothesis/refutation purpose profiles, including required signals,
objectives, horizons, and default cp-loss gates, are interpreted through
`ProbePurposeClassifier`, not detector/evaluator-local raw matches.
Prevented-plan evidence terms (`counterplay_drop`, `neutralized_break`,
`denied_squares`, `denied_resource`, `denied_entry_scope`) are projected through
`PlanEvidenceEvaluator`, not proof-local string assembly. Prophylactic
denied-resource class normalization and exact-slice token validation use the
same evaluator boundary. Plan certification trace terms and `support_probe:*`
markers use the same evaluator projection boundary.
Theme/subplan support tags are interpreted through
`PlanTaxonomy.ThemeResolver`, not detector-, planner-, or hypothesis-local raw
prefix slicing. Legacy embedded subplan annotations are interpreted/stripped
through the same resolver boundary for compatibility, but live probe generation
does not encode subplan authority in display `planName`; probe contract
authority lives in purpose/objective/required-signals/horizon metadata and
stable plan binding. Embedded theme preconditions and taxonomy-backed proof
contract ids, structural-state tags, and latent-seed evidence tags are likewise
interpreted through
`PlanTaxonomy.ThemeResolver`.
User-facing semantic policy is interpreted through `PlanSemanticsContract`.
Alias/default-subplan inference may classify proposal/debug/compatibility
carriers, but it cannot open strategic support wording, probe-purpose authority,
or thematic fallback eligibility without the typed semantics contract and the
downstream truth/proof boundary.
`PlanClaimBoundary.PlanProposal.fallbackTheme` is now the MoveReview thematic
fallback gate: name-only plan rows and broad aliases do not expose fallback
themes, while explicit theme metadata and typed `theme:`/`subplan:` support
tags can be considered by `PlanSemanticsContract`.
`PlanEvidenceEvaluator.planSupport`, `admittedPlanClaim`, and
`StrategicPlanEvidenceView.mainAdmittedClaims` are the typed support/admission
projection points; user-facing consumers should not rebuild admitted plan
meaning from evaluated-plan strings or diagnostics.
Promoted-plan row strings pass through `RenderedPlanText` in
`MoveReviewPlayerPayloadBuilder` after admission; rendered text is display
output, not a runtime plan-semantic input.
The same boundary now blocks support-only reinflation across producer and
consumer paths: `PlanProposalEngine` does not mint default/L2 subplans from a
broad theme or seed alias, `ProbeDetector` does not derive subplan probe
contracts from plan id/name aliases, `PlanEvidenceEvaluator` does not match
unbound probes through broad request aliases, `NarrativeOutlineBuilder` opens
subplan slots only from supportable plan kinds, `MoveReviewStrategicLedgerBuilder`
scores motifs from fallback themes/support kinds rather than continuity/name
aliases, and `TranspositionPvAligner` requires supportable weakness
theme/subplan provenance before creating a transposition proof.
For weak plan families with historically broad candidate sets, the live
producer/probe path now uses board-backed typed candidate helpers before any
theme fallback: `PieceRedeploymentEvidence`, `AdvantageTransformationEvidence`,
`FlankInfrastructureEvidence`, `PawnBreakEvidence`,
`RestrictionPlanEvidence`, and `WeaknessFixationEvidence`, plus
`FavorableExchangeEvidence` for concrete queen-trade, defender-trade,
bad-piece, and bounded recapture-like simplification-window capture candidates.
Large tactical material wins stay out of `simplification_window` support. Shared
pawn/target/material primitives for those helpers are centralized in
`PlanMoveEvidenceSupport`, while weakness targets themselves come from
`WeaknessTargetProfile`. Minority-attack fixation candidates additionally
consume `StrategicConceptSemantics` only when the observation is
`SemanticReady`, has a concrete primary break, target, and structural
consequence, and maps to a legal current prep/break move. A specific subplan such as
`passer_conversion`, `passed_pawn_manufacture`, `rook_pawn_march`,
`hook_creation`, `central_break_timing`, `wing_break_timing`,
`tension_maintenance` whose quiet move supports preserved central tension by
castling, minor-piece centralization/mobility gain, or a major piece on a
tension file, `prophylaxis_restraint`, `break_prevention`,
`key_square_denial`, `mobility_suppression`, `central_space_bind`,
`flank_clamp`,
`static_weakness_fixation`, `backward_pawn_targeting`, `minority_attack_fixation`, `iqp_inducement`,
`simplification_window`, `defender_trade`, `queen_trade_shield`,
`bad_piece_liquidation`, `simplification_conversion`, `invasion_transition`,
or `opposite_bishops_conversion` may request
probe moves from those legal board candidates. Covered weak subplans fail
closed when refined moves are absent; `ProbeDetector` must not re-append
generic theme pawn/flank/piece/weakness/rook moves. These helpers improve
candidate quality and support selection, but they are not proof-family
authority by themselves and still require evaluator/probe admission before
user-facing plan claims.
Piece-redeployment, flank-infrastructure, pawn-break, restriction, and
weakness-fixation helpers may preserve raw reason tokens such as
mobility/file-target/flank-file/tension/break-count/weakness-target diagnostics
in evidence metadata, but player-facing hypothesis detail text is generated
separately from typed candidate move, square, mobility, tension, flank, and
target fields. Those diagnostic tokens remain support metadata, not prose
authority.
`worst_piece_improvement` support is limited to non-pawn/non-king
redeployment. Active queen centralization is not enough; a queen candidate must
be a low-activity recovery rather than a generic central move.
`rook_file_transfer` support requires the rook to change files; same-file rook
moves belong to another helper path or stay closed.
`rook_lift_scaffold` support requires a lift-rank rook move plus enemy
king-file pressure, check, or a non-king resource target; flank-file lift alone
stays closed.
`invasion_transition` support is not admitted from material edge alone: the
heavy piece must reach an invasion rank/file square that gives check or attacks
a non-king resource target.
Move-level restriction evidence does not duplicate a specific
`break_prevention`, `key_square_denial`, `mobility_suppression`,
`central_space_bind`, or `flank_clamp` candidate as generic
`prophylaxis_restraint`; broad prophylaxis remains structural/seed support
unless a typed exact denied-resource path admits it. `wing_break_timing` stays
inside the `pawn_break_preparation` taxonomy theme; it is not re-themed as
generic flank infrastructure. `open_file_pressure` targets non-king enemy
material on a clear open/semi-open file, so a king-file tactic is not promoted
as long-strategy file pressure.
`rook_pawn_march` is not a generic a/h-pawn push: the move must be aligned
with the attacking king flank or create hook contact. `mobility_suppression`
is not a material-capture label: it requires a quiet restriction move with a
measured opponent-mobility drop and a denied/key-resource shape.
`queen_trade_shield` is not a generic queen win: the support candidate requires
a queen-vs-queen capture with a non-king recapture-like defender on the captured
queen's square. Minor-piece queen wins and undefended queen captures must remain
outside the queen-trade shield lane.
Broad structural booleans such as entrenched-piece, rook-pawn-ready, or
hook-chance state do not directly mint their helper-owned specific subplans in
`PlanProposalEngine`; exact plan support must come from the family helper's
board/legal-move candidates or from a later admitted proof path.
Helper ownership remains theme/family scoped; adding a PlanKind should extend
the owning helper before creating a new runtime helper.
`generic_center_plan` remains theme/debug metadata only and must not be
re-inflated into `central_break_timing` without `PawnBreakEvidence` support.
Legacy candidate probe id families also do not carry trust authority directly:
competitive/aggressive probe ids must be interpreted through
`ProbePurposeClassifier` before candidate tags, plan-alignment labels, or
why-not prose are produced.
invalid splices are dropped instead of becoming route proof. `L3.ThreatAnalyzer`
can use MultiPV score gaps as numeric context, but capture-derived threat
claims, attack squares, and best-defense UCI metadata must come from a legal
first-step replay. `StructurePlanArcBuilder` route-contribution prose also
requires the reviewed move to replay from the current FEN before it can say the
move started, reached, guarded, or connected a deployment route. Resource-removal line authority is also replay-only:
`citationLine`, `whyNot`, and rendered resource phrases cannot promote a
`ResourceRemoval` owner without a replayed break/file/square hit. Strategic
delta text about resources or defensive cover is therefore
only a softer counterplay-reduction signal unless replay supplies that hit.
Plan-level favorable-exchange matching is intentionally softer: raw
`RemovingTheDefender` motifs and generic defender/removal reason-code text cannot
mint a `defender_trade` subplan. They remain generic simplification support
unless the replay-backed semantic producer or owner witness proves the branch.
MoveReview owner seeding applies the same boundary. Generic defender/trade prose
does not mint `trade_key_defender`; a legal immediate exchange may only degrade
to `simplification_window` when it also has a move-linked exchange cue and a
narrative anchor.
The same rule applies to concept-summary exposure: raw
`RemovingTheDefender` positional tags are downgraded to generic `Exchange
pressure` instead of exposing a defender-removal concept without replay proof.
MoveReview basic-lane prose follows the same physical-board boundary for
capture semantics. A reply is a recapture, and a continuation is a center
capture, only when replay from the coupled line FEN says the move captures on
that square. Pin/skewer line confirmation likewise requires a replayed
continuation move that actually touches the relevant tactical square. SAN `x`
and UCI file-change patterns cannot create those labels.
Practical weakness-target rows are support-only, but their branch outcome still
must be board-coherent: a cached `resultingFen` is trusted only when the PV
legally replays to the same board state.

Opening-family prose claims are also kept out of the API presentation layer.
`OpeningFamilyClaimResolver` owns the claim-boundary decision from a structured
catalog family wire key plus `OpeningFamilyMatchProof` (`opening`, phase, ply,
FEN). The legacy `OpeningFamilyId` enum is a compatibility facade only; new
catalog family rows must not require resolver enum edits.
A structured family claim is `SupportedLocal` only when the opening label and
static `OpeningNameLookup` ECO/opening-book FEN result both match the requested
family. Static book matching may use same-EPD aliases retained by
`OpeningNameLookup.lookupAll`, so move-order transposition endpoints are not
lost when canonical display lookup picks a different row. `OpeningFamilyCatalog`
owns aliases, display labels, and target-square allowlists as main-resource TSV
data; malformed or unknown family/target pairs fail closed in the sanitizer and
unknown claim keys fail closed in the resolver. Shallow piece-square structure
predicates and strategic `TranspositionPvAligner` proofs are not used as opening
truth and cannot independently certify transpositions or coincidental later
positions.
Raw rendered sentences are not parsed for opening-family authority or
post-render suppression. `CommentaryApi` no longer splits rendered prose into
sentences or rewrites unsupported opening-family text after rendering; family
mismatch must be excluded or suppressed before surface prose is built.
The MoveReview public surface may expose an `Opening family` support row only
through `MoveReviewPlayerPayloadBuilder`, after the structured opening name is
matched through `OpeningFamilyCatalog` and the resolver admits the same family
as `SupportedLocal` for phase, ply, and FEN. This prevents stale explorer
labels, generic opening-phase text, or cached prose from becoming row-level
authority. Static book expansion is intentionally data-only: wider
`openings.tsv` variation coverage may make more real positions eligible for
the same resolver decision. Removed fixture floors are
not coverage authority; runtime rows still must not bypass the label-plus-FEN
proof pair or infer target authority from the variation name. Optional
`openingBook` metadata on that row is sanitized aggregate display data only:
ECO, positive total-game count, and up to three SAN top moves from
`OpeningReference`. It is allowed only on `opening_family` authority; sanitizer
and frontend decoder drop it from other authority shapes and never expose raw
explorer responses, sample games, source ids, or audit-cache provenance.
Static opening expansion is paused while the pool is provenance-cleaned.
`OpeningPoolAudit` and `OpeningMasterDbAuditRunner` classify malformed PGN
tails, normalized endpoint transposition duplicates, and optional masters
evidence from the configured endpoint (`--base-url`, defaulting to Lichess).
The current pool is pruned to 1276 rows that replay against captured Lichess
masters evidence as `master-backed`; 438 live-audited `not-found-in-masters`
expansion rows were removed rather than treated as real-game occurrence.
The query window may be pinned with `--since`/`--until` only for endpoints that
accept date windows; current Lichess `/masters` live audit should normally run
without them because date-windowed master queries can return `HTTP 400`. Live
master evidence requires OAuth; live runs may write replayable raw-response
JSONL with `--write-evidence-cache`, and cached evidence must be a replayable
JSONL row containing the same masters response keyed by endpoint-stable audit
`rowId`; legacy line-number rowIds are replay-only compatibility. Fetch or parse
failures are `master-fetch-error`, not clean absence evidence. Both evidence
forms remain tooling support; without them, rows are `unverified`,
`not-found-in-masters`, `transposition-duplicate`, `master-fetch-error`, or
`quarantine`, not player-facing truth. `provenanceStatusCounts` and
`--only-status` are cleanup triage aids only.
`opening_families.tsv` aliases, including Benko/Volga labels under the Benoni
family, only widen catalog matching for this `SupportedLocal` resolver path;
they do not create board truth or target proof by themselves.
Opening-goal prose expansion is also bounded to the existing carrier. New
`OpeningGoals` entries for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch
`...Ne4`, Queen's Indian `...Ne4`, Bogo-Indian `...Ne4`, Catalan `dxc5`
tension release, Open Catalan `c4` pawn recovery, Sicilian `...c5` c-pawn
challenge, and King's Gambit `f4` break may influence outline/explanation
wording only after the post-move board pattern and engine score produce
`openingGoalEvaluation`; they must not act as family admission, target
authority, exact central-break timing authority, or truth-contract evidence.

Current strict rules:

- `PositionLocal` scope alone never admits `WhatMattersHere`. A position probe
  must be a certified exact-slice packet or a supported-local packet with an
  accepted contract and no contract failure codes. `experimentConfidence` is
  not an admission bypass.
- exact owner slices require certified source/family predicates plus a typed
  `PlayerFacingExactSliceProof`; generic witness strings in anchor or
  structure terms do not satisfy `ExactSlice`, and downstream policy must fail
  closed instead of reconstructing a proof object from those strings.
- favorable-exchange labels do not prove their own owner path. Defender-trade
  and bad-piece-liquidation claims require bounded legal PV replay plus the
  structure-transition witness described above; otherwise they may remain
  diagnostic or degrade to `ExchangeSequence`, `MaterialTransition`, practical
  target, or semantics-contract-approved thematic fallback wording when tactical
  truth and missed-win truth allow it.
- break/file-axis admission uses the centralized `BreakFileToken` parser; a
  plain prose word or incidental `a`-`h` letter is not evidence for a file.
- position-probe question seeds must use the exact FEN being generated. The
  Carlsbad fixed-target seed remains closed unless the exact mirror board
  target (`c6` for White pressure, `c3` for Black pressure) and
  minority-support predicate both pass. That predicate allows the b-pawn to be
  on the original or advanced minority-attack squares (`b2`/`b4`/`b5` for White,
  `b7`/`b5`/`b4` for Black) but still requires the matching d-pawn chain support.
- dynamic weakness targets are board facts, not owner claims by themselves. The
  canonical `WeaknessTargetProfile` may feed bounded practical target rows,
  guard exact target-fixation witnesses, and serve as the endpoint fact for
  `TranspositionAligned` only after legal PV replay and veto checks pass. Target
  text, generic focus-square lists, or `targetPressureDelta` do not certify a
  claim without an accepted typed proof path. A generic exact target-fixation
  surface with multiple focus squares must name the selected square in the same
  idea id or typed evidence refs before it can become public owner evidence.
- support material never enters the owner pool directly.
- tactical truth veto outranks strategic authority. The only soft path is a
  non-tactical surface with a present narrative context, a present truth
  contract, no tactical-failure contract, no severe counterfactual, and an
  observed win-percentage drop of < 10.0pp (with further limits down to 5.0pp
  for forcing moves). Missing tactical context or truth contract fails
  closed for supported-local position probes and tactical-vetoable surfaces.
- line-scoped claims may survive only as subordinate evidence unless a main
  path strategic claim is independently admitted.
- support-only carriers may not re-inflate after certification failed closed.
- timing-witness admission is structured-token only. UCI moves, exact board
  squares, and piece-square anchors may couple non-neutralize timing witnesses
  only when their proof contract explicitly allows it. For
  `neutralize_key_break` / `counterplay_axis_suppression`, only the planner
  named-break token and typed exact-slice packet token may match; packet owner,
  anchor, structure, continuation, and raw claim terms are diagnostics, not
  authority.

Heavy-piece local-bind release vetoes are exact-replay risks, not generic
heavy-piece movement heuristics. A release may be signaled by a true deep queen
infiltration, a rook lift/switch away from the back-rank shuffle case,
repeated heavy-piece checks, or an exchange sacrifice where a rook captures
lower material and is then recaptured on the replayed branch. A queen
centralization/single check, an unrecaptured rook capture, or a back-rank rook
shuffle must not by itself create `heavy_piece_release_illusion`. File and
entry persistence must come from typed route validation signals: validation
purpose plus exact `Counterplay` resolution, exact denied file removal, and
exact denied square removal for the entry. English phrases in `keyMotifs`,
`planBlockersRemoved`, `planPrereqsMet`, or target text are diagnostics only.

Color-complex squeeze is promoted only through an exact board-backed position
probe:

- proof family: `color_complex_squeeze`
- proof source: `color_complex_squeeze_probe`
- status: `Releasable`
- certified eligible: true
- supported-local eligible: true
- default failure: `color_complex_authority_closed`
- requirement: FEN parses; the opponent owns the semantic weak square; the
  weakness is color-complex/hole/fianchetto tagged; a friendly bishop or knight
  actually attacks that weak square on the board; surface/semantic evidence
  points to the same square; best-defense branch, same-branch proof, and stable
  persistence survive the packet boundary. Coordinate and minor-piece words are
  trace terms only, not authority.

Readiness scans and exact-FEN review artifacts are local evidence for future
authority review, not runtime admission.

## Render Trust Boundary

`FragmentAuthority` owns render-release tags:

- render-only text may release
- support-only text may release only when sufficiently grounded
- unsafe truth text is dropped
- unsafe lesson text is dropped
- future-lesson candidates are dropped
- move-linked-anchor fragments require their anchor

`NarrativeOutlineBuilder` assembles beats; it does not own release legality.
`NarrativeOutlineValidator` remains the final scrubber.
`UserFacingSignalSanitizer` strips raw diagnostic labels such as `theme:` and
`subplan:`, but preserves canonical player-facing headings such as
`Key theme:`. `FullGameDraftNormalizer` may rewrite sanitized key-theme
scaffolding into prose; that rewrite is cleanup only and does not create
authority.

## API And Frontend Trust Boundary

`CommentaryApi` must serialize typed surviving payloads without recomputing
authority.

MoveReview fallback/retry handling is backend-owned. `CommentaryController`
emits `diagnostics.status` and `diagnostics.sourceModeReason` from
`MoveReviewResponseDiagnostics`, which evaluates final fallback prose through
`PlayerProseBoundary` and existing polish/source-mode codes. The frontend uses
`diagnostics.status == retryable_fallback` to retry or ignore a fallback
response; it must not parse commentary prose, English phrases, helper labels,
or source-mode prefixes to decide retryability.
Template-quality polish skipping is language-aware. English marker scoring may
skip polish only for English requests; non-English text is not judged by the
English marker list.

MoveReview player-visible support UI is owned by the backend-certified
`MoveReviewPlayerSurface` payload built by `MoveReviewPlayerPayloadBuilder`.
The public MoveReview wire is built by `MoveReviewResponsePayload` and no
longer emits raw `strategyPack`, top-level `signalDigest`, author
question/evidence summaries, concepts, plan-tier/commentary-mode controls,
full `mainStrategicPlans`, or `strategicPlanExperiments`. It exposes
`mainStrategicPlanCount` for UI metadata instead of plan arrays. `probeRequests`
remain a compatibility array on the public schema, but the current sanitized
MoveReview response emits it empty; public MoveReview does not expose raw probe
orchestration requests. Public polish metadata excludes validation reasons,
token counts, cost estimates, and strategy-coverage diagnostics; fallback/retry
state is owned by `diagnostics.status/sourceModeReason` instead. Frontend
decoding and stored snapshot restore strip those internal polish fields from
stale payloads before they can become DOM metadata.
`MoveReviewPlayerPayloadBuilder` must not read raw `strategyPack`, raw
`signalDigest`, or outbound `probeRequests` to create product rows; it may
project only already bounded surface inputs such as selected evaluated plans,
the certified explanation, the strategic ledger, refs, and authoring summaries.
Decision-comparison UI follows the same boundary. `LineConsequenceEvaluator`
may summarize a PV/ref line, but only `MoveReviewRefs` lines that pass
`MoveReviewPvLine` legal FEN replay can become `SurfaceCandidate`. PV support
markers (`pv:*`) are projected through
`MoveReviewPvLine.pvMoveTerms`; witness producers must not reassemble that
prefix locally.
Legal engine-only `VariationLine` summaries can become `ReplayBackedInternal` for
internal decision evidence, narrative hooks, and ledger notes, while remaining
blocked from the product decision strip by release type and the engine-only
surface-blocking reason. The ref path stays strict across the checked line.
Engine-only internal summaries may use only the legal prefix that already proves
a concrete local consequence such as an exchange sequence, material transition,
central pawn advance, or replayed checks. Stale tail moves are removed from the
internal evidence and cannot carry mate/check proof; a legal prefix that is only
preview text remains diagnostic-only.
Close-candidate alternative PV prose may support MoveReview text only through
`AlternativeNarrativeSupport` plus `ContrastiveSupportAdmissibility`. The
admission boundary requires an enriched comparative sentence (`while`,
`whereas`, or `both`) backed by exact-FEN replayed branches; raw close-candidate
support remains rejected and cannot re-inflate into an owner claim. Enriched
close-candidate prose must preserve the near-top/two-good-moves boundary by
describing viable branch contrast instead of projecting a hard `best`/inferior
claim from support-only MultiPV material; planner shadow traces label enriched
close candidates separately from raw close alternatives.
`MoveReviewPlayerPayloadBuilder.decisionComparisonSurface` is the release gate
for `moveReviewPlayerSurface.decisionComparison`; it requires two comparable
move labels or a same-first-move comparison with a typed later consequence, a
>=35cp gap or exact comparative/practical alternative reason, a
surface-candidate line consequence, and no surface-blocking line reject reasons.
Gaps below 60cp are labeled as slight rather than as a clear engine preference.
It does not expose deferred moves or raw proof/source metadata, and it does not
promote plan authority. It may attach `targetComparison` metadata only after
the decision strip is already admitted. That metadata compares the engine-best
line and reviewed/chosen line endpoints through `WeaknessTargetProfile`; it is
not a proof packet and not a new source of certified plan ownership. Backend
and frontend parsing require legal square tokens plus authority-key-shaped
target kinds; malformed target comparison data is dropped for that optional
field rather than crashing the MoveReview surface or synthesizing substitute
guidance. Frontend rendering may use the validated metadata only as a compact
subordinate endpoint-target note inside the already admitted decision strip; it
must not create a plan claim or recover missing target comparison from raw
engine fields, prose, or fallback carriers.
Practical-guidance rows are lower authority than supported-local exact rows.
They may describe structural or PV-coupled practical plans, central
liquidation, or central challenge with cautious wording, but they cannot label
themselves as `CertifiedOwner`, `SupportedLocal`, `Central break`, or a main
plan. The frontend may style practical row tone and authority kind differently,
but that styling is a display affordance rather than a proof reconstruction
path.
`Practical target` rows are in the same lower-authority family: they can name a
board-derived weak pawn target, but they keep `practical_plan` authority and do
not publish `authority.target`. They are suppressed when the best available
line shows the target being liquidated by the defender. If the pressure side
captures the target, the target is considered resolved rather than transient.
If the best line merely preserves the same target, that persistence can support
display only when the line carries a `resultingFen` or at least five UCI plies;
shorter persistent lines are too shallow to overcome horizon risk.
The current supported-local product projections are narrow.
`neutralize_key_break`: `MoveReviewSupportedLocalSurfaceRows` can add a
`Counterplay break` summary row only from the same planner runtime used to
build prose, and only after `ClaimAuthorityResolver` returns `SupportedLocal`
for a `neutralize_key_break` timing plan or main-path packet claim from
`counterplay_axis_suppression` with an exact owner path and no tactical veto.
This is a bounded local-reading support row, not a `CertifiedOwner` upgrade,
not a proof-contract expansion, and not a tactical-veto relaxation. The row
must be built from the timing witness named-break token and/or the packet's
typed `PlayerFacingExactSliceProof.CounterplayAxisSuppression` token. On the
plan+packet path, those two structured tokens must match; on the packet-only
path, packet terms are not parsed to recover a token. Raw claim prose and
packet owner/structure/anchor terms are not token authority. If
`BreakClampMaterializer` proves that the played move occupies the opponent
break destination, the trusted witness is the full route token (`e4-e5`,
`...b5-b4`), not the self-referential destination square. Tokenless packets,
generic fallback wording, and single-square tokens that collide with the played
move must not become product-visible `Counterplay break` rows. Collision
checking uses legal UCI replay from the current FEN and `move.dest.key`; SAN is
display-only, and missing or illegal UCI fails closed for single-square tokens.
The row must not
expose the internal `SupportedLocal`/local-reading label or raw proof
family/source metadata through public row `source`; it may expose only the
public row authority `{ kind: "counterplay_break", token: ... }`.
`central_break_timing`: product-visible `Central break` rows require the exact
runtime witness, including a non-capturing same-file d/e pawn advance to
`d4`, `e4`, `d5`, or `e5`, board link, source/family match, route-shaped break
token, and an admitted `SupportedLocal` main-path packet. Diagonal captures
(`d4-e5`, `...e5-d4`) and prep/challenge pushes (`...d7-d6`, `e2-e3`) are
separate reject shapes, not product-visible `Central break` rows; Sicilian
`...c5` and King's Gambit `f4` remain opening-goal prose support only. The
reviewed played move may seed the exact witness when it is itself that same legal
central break, even if the top PV does not replay it; this path still requires
the legal board move and the same packet/source/family boundary. PV gap and
two-move branch key are diagnostics for this row, not proof of monopoly value
and not hard release gates. Taxonomy, strategy-pack labels, raw claim prose,
and signal-digest text do not admit the row. The public row authority is limited
to `{ kind: "central_break", token: ... }`; raw proof ids stay private.
Forcing-defense scenes may only receive subordinate support wording; tactical
truth veto remains higher priority.

Compensation subtype matching is fail-closed for unknown subtype dimensions.
Generic compensation prose can still mention the theme, but it cannot satisfy a
specific subtype contract unless the theater, mode, recovery policy, and
stability class are recognized and the text carries the matching subtype
anchors. Delayed/deferred compensation requires an explicit recovery/defer
anchor rather than the word `compensation` alone. Target-fixing compensation
cannot be authorized by generic `pressure` or file-occupation route prose; it
requires target/fixed-pawn/weak-pawn language or typed target evidence, coupled
with FEN-based structural verification of target board states. Shared pawn
targets are resolved by `structure.PawnStructureTargets`, not by duplicate
Carlsbad or Benoni pawn-shape checks inside the compensation interpreter.
Opening-route target fixation additionally requires `OpeningRouteCatalog`
descriptor data and `PieceRouteEvidence` legal UCI/PV support. Route evidence
uses raw `VariationLine.moves` before parsed metadata and carries the replayed
terminal `Position`; `KnightRouteEvidence` remains the knight-only exact-slice
facade. `OpeningRouteTargetEvidence.checkRouteEvidence` validates the row's
`target_mode` on that terminal board, not on the root board before the route is
completed. If the slice declares a focal, directional, idea-focus, move-ref,
semantic structural-weakness, or root-board weak-pawn target, `findRouteWitness`
may only select catalog rows for that declared target. Benoni `d6`, reversed Benoni `d3`, and King's
Indian `c5` routes reuse the same witness path; FEN substrings and fixed branch-key text are not
admission gates. The starter route pack extends data coverage for major
openings including Sicilian, Queen's Gambit, Slav/Semi-Slav, Nimzo-Indian,
English, Dutch, Scandinavian, Pirc/Austrian, Catalan, London, Bird, Queen's
Indian, Bogo-Indian, King's Gambit, Caro-Kann, French, Open Games, Gruenfeld,
Alekhine, and Nimzowitsch. The current catalog has 52 descriptors, including 4
bishop fianchetto descriptors that may project support metadata through
`PieceRouteEvidence` but do not by themselves promote exact target-fixation
truth. Mined
additions require at least five master-backed opening-row witnesses, and
lower-support route candidates remain deferred. Route targets are mirrored in
the family target allowlist so public target metadata is not blocked by stale
catalog data after legal route evidence passes. Direct Queen's Indian and
Bogo-Indian `Nf6-e4` rows handle positions where book proof starts after
`...Nf6`, but route catalog membership
remains support evidence, not standalone claim authority. Battery
formation predicates require the moved piece and partner to share the declared
line with no blocker between them.
`CommentaryApi` passes those same selected evaluated plans into
`UserFacingPayloadSanitizer`; sanitizer does not admit strategic plans from
`probe_backed:validated_support` or `StrategicPlanExperiment.evidenceTier`
strings. Strategic-plan experiments may survive only in the internal sanitized
response model when their plan key matches a retained typed-admitted plan; they
are not emitted on the public MoveReview wire. Fresh `MoveReviewPlayerPayloadBuilder`
may lower exact-id-matched experiment flow booleans into a bounded `Plan status`
advanced row, but the row carries no experiment object, evidence tier, probe id,
or public authority token and cannot be reconstructed by the frontend from
cached raw carriers.
On cache hits, `CommentaryApi` uses the cache-specific sanitizer path so the
already sanitized fresh response does not lose continuity state or certified
ledger data on read, while legacy/marker-bearing strategic metadata remains
closed.
When authoring summaries were assembled from pending probe requests, raw request
purpose/objective/plan/seed metadata remains support-only and must not be
rendered as player authoring meta. Probe-backed and authoring-backed ledger
rows may use certified line/eval evidence, but not request/result
purpose/objective metadata, raw source IDs, row provenance/source metadata, or
`signalDigest` decision fallback text. Deferred decision moves are also not
admitted to the player surface; sanitizer and frontend decoding must ignore
them.
`MoveReviewPlayerPayloadBuilder.ledgerRows` treats the strategic ledger as a
bounded source of player-surface probe rows only when each line has a sanctioned
source (`probe`, `decision_compare`, `variation`, or `authoring`) and non-empty
SAN moves. Malformed ledger lines are dropped rather than repaired from raw
probe, authoring, signal-digest, or prose data.
The frontend must not decode raw probe or authoring carriers for orchestration
fallback. Public `probeRequests` stay an empty compatibility field, and the
post-response refined probe fetch path is closed; player support, advanced,
probe, authoring, and decision-comparison sections are not rebuilt from raw
carriers.
No Gzip/Base64 opaque strategic token is treated as a security or trust
boundary. Structured continuity tokens remain compatibility state until a
server-signed, versioned, expiring, request-bound token contract exists.
QC/report queue tooling follows the same rule: when `moveReviewPlayerSurface`
exists, support rows come from that surface rather than raw `signalDigest`,
`mainStrategicPlans`, or `strategicPlanExperiments`.

Frontend code must not rebuild strategic meaning from:

- `topEngineMove`
- `cpLossVsChosen`
- latent/deferred fields
- support-only carriers
- raw `strategyPack`
- raw `signalDigest`
- raw `authorEvidence`, `probeRequests`, or `mainStrategicPlans`
- omitted decision-comparison data
- free-form helper prose

MoveReview and narrative views render only typed payload fields that survived
backend authority. For MoveReview support panels, that typed field is
`moveReviewPlayerSurface` schema `chesstory.move_review.player_surface.v2`.
Rows may carry sanitized `authority` with only `kind`, `token`,
`openingFamily`, and `target`; malformed authority is removed by the backend
sanitizer, and the frontend decoder downgrades unsupported or malformed
authority shapes from cached/stale surfaces while preserving the row text.
Only `counterplay_break` may carry a square token; `central_break`,
`central_liquidation`, and `central_challenge` require route-shaped tokens.
Cached v1 rows decode with no authority. Opening-family
authority may keep `openingFamily` only for sanctioned key shapes and may keep
`target` only for backend allowlist pairs from `OpeningFamilyCatalog`;
unsupported targets are stripped while the opening row may remain. The current
builder projection emits target metadata only when same-family legal route
evidence satisfies `OpeningRouteCatalog`, `PieceRouteEvidence`, and
`OpeningRouteTargetEvidence.checkRouteEvidence`; stale cached target metadata that
does not pass shape/allowlist checks is still downgraded. Legacy top-level
`moveReviewExplanation` is not a public fact-fragment authority: backend
sanitization strips `factFragments`, and frontend decoding ignores that field.
Legacy top-level `moveReviewLedger` may provide only metadata/signal attributes
after schema/key validation; malformed line rows are dropped and no top-level
ledger line renderer reconstructs probe/support rows. If the surface is
missing, no support panel is reconstructed.

MoveReview corpus and QC reports must measure the same player surface. When
`moveReviewPlayerSurface` is present, performance rows come from that field
only; raw carrier reconstruction is not used for MoveReview QC support rows.
Tactical-failure `neutralize_key_break` diagnostics must remain rejected or
veto-bucketed in QC and must not count as admitted product-visible support.
Tokenless or played-move-collision `neutralize_key_break` diagnostics are also
QC rejections, not admitted product-visible support.
Missing MoveReview raw artifacts or missing canonical surfaces do not authorize
raw-carrier or chronicle metadata fallback for MoveReview support rows, and
active-note QC rows must not export chronicle objective/focus/execution metadata
as support rows.

## Risk Map

| risk | current control |
| --- | --- |
| support-only becomes owner | claim-authority kernel, proof contracts, planner adapter |
| fallback truth rewrite | truth contract first; `PlanClaimBoundary` exposes fallback themes and `PlanSemanticsContract` gates thematic fallback; no-contract fallback is failure-only |
| umbrella strategic overclaim | exact packet/certified slice required |
| plan promotion blocked by bookkeeping drift | hard/soft probe validation split in `PlanEvidenceEvaluator` |
| raw evidence-tier string becomes authority | `StrategicPlanEvidenceView` is the current runtime read-model |
| alias/default subplan becomes user-facing authority | `PlanClaimBoundary` keeps broad aliases/default subplans proposal-only; producer/probe/outline/ledger/transposition consumers use boundary constructors; `PlanSemanticsContract` owns semantic policy |
| internal plan diagnostic text leaks to player rows or sanitized plan carriers | `UserFacingSignalSanitizer.sanitizePlanDetail` is shared by `MoveReviewPlayerPayloadBuilder` and `UserFacingPayloadSanitizer`; fallback is only for already-present suppressed details |
| inferred proposal theme creates or hides MoveReview plan rows | player-surface family gates consume `PlanProposal.fallbackTheme` and `supportKind`, not plan-name/execution/failure prose inference |
| plan flow-state row attaches to the wrong strategic idea | `MoveReviewPlayerPayloadBuilder` requires exact plan-id match and typed support-kind compatibility before consuming `StrategicPlanExperiment` flow booleans |
| sibling score treated as refutation | `alternativeDominance` remains ranking metadata, not `Refuted` |
| renderer leaks unsafe prose | `FragmentAuthority` plus validator scrub |
| favorable-exchange label becomes owner | bounded legal replay plus defense/mobility structure-transition witness; otherwise lower-tier line/practical wording only |
| frontend rebuilds omitted meaning | `moveReviewPlayerSurface` for MoveReview product UI; no raw-carrier reconstruction |
| frontend parses fallback prose for retry state | backend `diagnostics.status/sourceModeReason`; no prose regex gate |
| QC reports measure a virtual raw surface | `buildMoveReviewRows` uses `moveReviewPlayerSurface`; absent surface yields no MoveReview support rows |
| tactical neutralize support leaks through diagnostics | `ClaimAuthorityResolver` tactical veto plus QC veto rejection for `neutralize_key_break` |
| generic or self-referential break support leaks | typed-proof/named-token surface gate rejects tokenless, term-only, mismatch, and played-move-collision `neutralize_key_break` rows |
| color-complex premature release | typed exact-slice proof plus authority-closed failure |
| lesson overgeneralization | scoped takeaway only |

## Naming Risk

Runtime implementation names should identify the chess asset and proof lane:
`LocalFileEntryBind`, `CounterplayAxisSuppression`,
`ProphylacticRestraint`, `ColorComplexSqueeze`, or a cataloged relation witness.
Do not introduce rollout, history, or umbrella expansion labels as runtime
modules, proof families, public authority tokens, package names, or product row
kinds. If historical terms remain in tests or diagnostics, the consuming
runtime path must translate them to a stable domain/proof boundary before any
authority decision.
Policy-local rival assessment tags such as `secondary_plan:*`,
`secondary_idea:*`, and `exact:*` remain suppression/release-risk traces inside
`PlayerFacingTruthModePolicy`; they are not authority tokens and cannot open a
strategic expansion path.

## Lesson Defer Rationale

General lesson authority remains closed because current local proof can validate
only a reviewed move, FEN, branch, and evidence tier. It cannot safely state a
general chess lesson without additional corpus coverage, exception handling,
and user-facing scope wording.

Allowed today:

- exact factual fallback
- bounded `SupportedLocal` phrasing
- `MoveReviewScopedTakeaway` tied to the reviewed move and branch

Not allowed today:

- general rules such as "always" or "in every position"
- shared-lesson helper labels
- whole-position strategic truth from local support rows
- lesson claims from color-complex readiness rows

## Maintenance Triggers

Update this file in the same change when trust-relevant behavior changes in:

- fallback truth projection or rewrite behavior
- cross-surface contract consumption
- support-only carrier exposure
- proof-contract eligibility
- claim-authority resolution
- planner admission
- renderer release tags
- lexicon/template authority boundaries
- frontend support rendering
- scoped takeaway or lesson-readiness guards

Lexicon rule tables are wording infrastructure only. Adding or reordering
`NarrativeMotifPrefixTable` templates must not be treated as new proof authority
unless the upstream detector, proof contract, and surface gate are updated in
the same change. `NarrativeLexicon.getMotifPrefix` is only the consumer of that
table, not a branching authority surface.

Report future cleanup as either `boundary cleanup only` or
`boundary cleanup + verified compile/test`; do not claim product quality gains
from mixed or unverified diffs.
