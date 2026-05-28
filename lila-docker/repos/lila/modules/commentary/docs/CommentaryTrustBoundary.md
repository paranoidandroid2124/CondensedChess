# Commentary Trust Boundary

This file is the canonical trust-risk map for Chesstory commentary. Use it for
false positives, overclaim control, support-only reinflation, fallback rewrites,
surface-consumption risk, and lesson-readiness gating.

It complements `CommentaryPipelineSSOT.md` and `CommentaryTruthBoundary.md`.

## Current State

The live product trust boundary is MoveReview-only. Chronicle, Active, Game
Arc, and whole-game replay are legacy diagnostic/tooling surfaces, not runtime
trust infrastructure. Active bridge planning, Active thread selection,
Active strategic-note composition, and Chronicle compression are confined to
`modules/commentaryTools/src/test`; they must not supply MoveReview release
authority unless a new runtime audit explicitly reopens that boundary.
`GameChronicleResponse` and `GameChronicleMoment` are outside the runtime trust
boundary. Any legacy replay tooling that still needs their data must convert to
a compact, non-authority carrier before calling shared builders, and runtime
trust/signoff code must not consume those Chronicle DTOs directly.
Active-note DTO fields, active branch dossiers, strategic-thread lists, and
`ActivePlanRef` tags are likewise test/tooling-only; runtime `GameArc` must not
carry them as empty compatibility payloads.

Current operating posture:

- maintain existing exact-board promoted slices
- keep broad strategic expansion closed
- allow new runtime authority only through proof contracts and the
  claim-authority kernel
- keep support-only/deferred/latent carriers internal
- keep Track 5 lesson authority deferred

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
Only typed `ProbeBacked` and `TranspositionAligned` evaluated plans can enter
selected main-plan authority. `TranspositionAligned` is a separate provenance:
it must come from legal PV replay through `TranspositionPvAligner`, a terminal
`WeaknessTargetProfile` match for an expected target square, positive
attacker-minus-defender control, sufficient line horizon, and mover-loss/mate
veto checks. It must not be rewritten as `ProbeBacked` and does not satisfy
exact proof contracts or `check_qualifying`. `StructuralOnly` and
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

Opening-family prose claims are also kept out of the API presentation layer.
`OpeningFamilyClaimResolver` owns the claim-boundary decision from a structured
catalog family wire key plus `OpeningFamilyMatchProof` (`opening`, phase, ply,
FEN). The legacy `OpeningFamilyId` enum is a compatibility facade only; new
catalog family rows must not require resolver enum edits.
A structured family claim is `SupportedLocal` only when the opening label and
static `OpeningNameLookup` ECO/opening-book FEN result both match the requested
family. `OpeningFamilyCatalog` owns aliases, display labels, and target-square
allowlists as main-resource TSV data; malformed or unknown family/target pairs
fail closed in the sanitizer and unknown claim keys fail closed in the resolver.
Shallow piece-square structure predicates are not used as opening truth and
cannot independently certify transpositions or coincidental later positions.
Raw rendered sentences are not parsed for opening-family authority or
post-render suppression. `CommentaryApi` no longer splits rendered prose into
sentences or rewrites unsupported opening-family text after rendering; family
mismatch must be excluded or suppressed before surface prose is built.
The MoveReview public surface may expose an `Opening family` support row only
through `MoveReviewPlayerPayloadBuilder`, after the structured opening name is
matched through `OpeningFamilyCatalog` and the resolver admits the same family
as `SupportedLocal` for phase, ply, and FEN. This prevents stale explorer
labels, broad opening-phase text, or cached prose from becoming row-level
authority. Static book expansion is intentionally data-only: broader
`openings.tsv` variation coverage may make more real positions eligible for
the same resolver decision. The removed broad-variation Scala fixture floor is
not coverage authority; runtime rows still must not bypass the label-plus-FEN
proof pair or infer target authority from the variation name.
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
family, only broaden catalog matching for this `SupportedLocal` resolver path;
they do not create board truth or target proof by themselves.
Opening-goal prose expansion is also bounded to the existing carrier. New
`OpeningGoals` entries for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch
`...Ne4`, Queen's Indian `...Ne4`, and Bogo-Indian `...Ne4` may influence
outline/explanation wording only after the post-move board pattern and engine
score produce `openingGoalEvaluation`; they must not act as family admission,
target authority, or truth-contract evidence.

Current strict rules:

- `PositionLocal` scope alone never admits `WhatMattersHere`. A position probe
  must be a certified exact-slice packet or a supported-local packet with an
  accepted contract and no contract failure codes. `experimentConfidence` is
  not an admission bypass.
- exact owner slices require certified source/family predicates plus a typed
  `PlayerFacingExactSliceProof`; generic witness strings in anchor or
  structure terms do not satisfy `ExactSlice`, and downstream policy must fail
  closed instead of reconstructing a proof object from those strings.
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
  text, broad focus-square lists, or `targetPressureDelta` do not certify a
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
`MoveReviewPvLine` legal FEN replay can become `SurfaceCandidate`. Legal
engine-only `VariationLine` summaries can become `ReplayBackedInternal` for
internal decision evidence, narrative hooks, and ledger notes, while remaining
blocked from the product decision strip by release type and the engine-only
surface-blocking reason.
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
separate reject shapes, not product-visible `Central break` rows. The reviewed
played move may seed the exact witness when it is itself that same legal
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
descriptor data, `KnightRouteEvidence` legal UCI/PV support, and
`OpeningRouteTargetEvidence.checkRouteBoard` validation for the row's
`target_mode`. Benoni `d6`, reversed Benoni `d3`, and King's Indian `c5`
routes reuse the same witness path; FEN substrings and fixed branch-key text are
not admission gates. The starter route pack extends data coverage for major
openings including Sicilian, Queen's Gambit, Slav/Semi-Slav, Nimzo-Indian,
English, Dutch, Scandinavian, Pirc/Austrian, Catalan, London, Bird, Queen's
Indian, Bogo-Indian, King's Gambit, Caro-Kann, French, Open Games, Gruenfeld,
Alekhine, and Nimzowitsch. The current catalog has 48 descriptors; mined
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
are not emitted on the public MoveReview wire.
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
evidence satisfies `OpeningRouteCatalog`, `KnightRouteEvidence`, and
`OpeningRouteTargetEvidence.checkRouteBoard`; stale cached target metadata that
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

## Active Risk Map

| risk | current control |
| --- | --- |
| support-only becomes owner | claim-authority kernel, proof contracts, planner adapter |
| fallback truth rewrite | truth contract first; no-contract fallback is failure-only |
| broad strategic overclaim | exact packet/certified slice required |
| plan promotion blocked by bookkeeping drift | hard/soft probe validation split in `PlanEvidenceEvaluator` |
| raw evidence-tier string becomes authority | `StrategicPlanEvidenceView` is the current runtime read-model |
| sibling score treated as refutation | `alternativeDominance` remains ranking metadata, not `Refuted` |
| renderer leaks unsafe prose | `FragmentAuthority` plus validator scrub |
| frontend rebuilds omitted meaning | `moveReviewPlayerSurface` for MoveReview product UI; no raw-carrier reconstruction |
| frontend parses fallback prose for retry state | backend `diagnostics.status/sourceModeReason`; no prose regex gate |
| QC reports measure a virtual raw surface | `buildMoveReviewRows` uses `moveReviewPlayerSurface`; absent surface yields no MoveReview support rows |
| tactical neutralize support leaks through diagnostics | `ClaimAuthorityResolver` tactical veto plus QC veto rejection for `neutralize_key_break` |
| generic or self-referential break support leaks | typed-proof/named-token surface gate rejects tokenless, term-only, mismatch, and played-move-collision `neutralize_key_break` rows |
| color-complex premature release | typed exact-slice proof plus authority-closed failure |
| lesson overgeneralization | Track 5 deferred; scoped takeaway only |

## CTH Priority Summary

Detailed historical B-frontier logs are no longer canonical in this file. The
current conclusion is:

- B1/B2/B3 exact slices remain maintained.
- B4/B5/B6 remain narrow bounded-scope results only.
- B7/B8 and broad color-complex, heavy-piece, mobility-cage, or global squeeze
  expansion remain design/recon territory.
- New authority must start from exact board positions, exact witness extraction,
  best-defense evidence where relevant, and proof-contract promotion.

Underlying evidence lives in local generated artifacts and targeted test/tool
reports. Those artifacts are evidence, not authority predicates.

## Track 5 Defer Rationale

Broad lesson authority remains closed because current local proof can validate
only a reviewed move, FEN, branch, and evidence tier. It cannot safely state a
general chess lesson without additional corpus coverage, exception handling,
and user-facing scope wording.

Allowed today:

- exact factual fallback
- bounded `SupportedLocal` phrasing
- `MoveReviewScopedTakeaway` tied to the reviewed move and branch

Not allowed today:

- broad rules such as "always" or "in every position"
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
