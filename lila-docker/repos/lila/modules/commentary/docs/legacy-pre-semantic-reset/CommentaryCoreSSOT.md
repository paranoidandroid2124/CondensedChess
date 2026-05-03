# Commentary Core SSOT

This file is the canonical source of truth for the
`codex/24-61-3016+35-structural-experiments` branch.

It supersedes the removed `modules/llm/docs/*` material on this branch.

## Branch Intent

This branch explicitly rejects incremental repair of the old `llm` backend.

The frontend structure may survive, but the backend commentary engine is reset
as a new `commentary` module.

Compile-red intermediate states are accepted on this branch.

## Core Decision

The branch adopts the following working thesis:

- current `master` is structurally bottlenecked by distributed semantic
  admission, late suppression, and legacy carrier paths
- current strategic-object rewrite was directionally stronger than `master`, but
  still bottlenecked by narrow exact-slice calibration and certification cost
- therefore the backend should be rebuilt as a new commentary core rather than
  patched as a legacy `llm` subsystem

## Canonical Pipeline

The new commentary backend is modeled as:

`root truths -> typed witnesses -> strategic objects -> deltas -> certification -> strategy projections -> renderer`

Truth ownership above `U` sits on the `object -> delta -> certification`
chain.

`projection` and `renderer` are downstream consumers, not owners of truth.

## Interim Vertical-Slice Progress Record

The `V` labels used in current coordination notes mean "vertical slice". They
are orchestration labels only and must not become source package, class, object,
or runtime contract names.

The current intermediate roadmap is:

- `V1`: exact-board Object/Delta claim producer.
  - Status: complete for the minimum bounded current-board and transition
    slice.
- `V2`: Certification, Projection, and Source-context claim producer boundary.
  - Status: complete at producer-boundary level. Projection consumption is
    enabled only for descriptor-certified runtime
    `StrategyProjectionAdmissionResult` values; source context enters only
    through normalized source-context handoff.
- `V3`: candidate-line evidence internal infrastructure.
  - Status: complete as internal infrastructure, with `V3a` through `V3j`
    complete as coordination sub-slices:
    - `V3a`: `CandidateLineEvidence` internal contract.
    - `V3b`: backend exact-FEN UCI-to-SAN line normalizer.
    - `V3c`: sanitized root/child multi-packet handoff.
    - `V3d`: `CandidateLineEvidence` to public-safe
      `PreparedVariationEvidence` lowering policy.
    - `V3e`: internal probe orchestration and cache policy boundary.
    - `V3f`: controlled external probe/cache adapter.
    - `V3g`: internal backend seam integration of prepared variation evidence.
    - `V3h`: minimal analyse frontend bridge pass-through for public
      `RenderVariationEvidence`.
    - `V3i`: internal candidate-line pipeline contract from sanitized
      probe/cache payloads through backend render and bridge pass-through.
    - `V3j`: full closeout audit and targeted verification of the internal
      candidate-line infrastructure.
  - Next: `V4`, live candidate-line integration beyond the internal seam.
    Public route/controller wiring and product frontend remain closed.
- `V4`: live candidate-line integration beyond the internal seam.
  - Status: internal provider boundary started. `V4a` adds the
    `CandidateLineAssemblyProvider` completed-probe/cache provider around the
    controlled adapter.
  - Status: `V4b` adds the stable internal `CandidateLineProofCache` facade
    and deterministic in-memory contract implementation for candidate-line
    proof-cache reads/writes. Actual Stockfish/WASM execution, production cache
    persistence backend, public controller transport, product frontend UI, and
    prose remain deferred.
  - Status: `V4c` adds the internal `CandidateRootProbeIntegration` root-intake
    boundary for completed root probe payloads and valid proof-cache root hits.
    It returns internal assembly plus next child probe requests only. Actual
    Stockfish/WASM execution, controller/public API transport, product frontend
    UI, claim creation, prose, and raw probe exposure remain deferred.
  - Status: `V4d` adds the internal `CandidateChildProbeIntegration`
    child-intake boundary for completed child probe payloads and valid
    proof-cache child hits corresponding to the root stage's planned child
    requests. It returns internal assembly plus satisfied/unsatisfied child
    requests. Actual Stockfish/WASM execution, controller/public API
    transport, product frontend UI, claim creation, prose, and raw child
    probe/cache exposure remain deferred.
- `V5`: cross-family line selection across engine line evidence, projection,
  opening, motif, endgame, and retrieval context.
  - Status: first contract/model boundary introduced as
    `CandidateLineSelection`. It consumes already selected outline
    claims/context plus public-safe `PreparedVariationEvidence` only.
  - Status: `ClaimSelector` now uses `CandidateLineSelection` to filter public
    `CommentaryOutline.variationEvidence` to the selected strong line set. It
    still does not create claims, normalize SAN, run probes, consult
    cache/source lookup, write prose, or expose branch/cache/probe internals.
- `V6`: book-style annotation prose contract.
  - Status: language-neutral annotation planning is started. The current slice
    carries selector-owned `PlanAnnotationSelection` from
    `CandidateLineSelection` through `CommentaryOutline` into
    `CommentaryPlan`, and adds renderer-side `BookAnnotationPlanner` from
    `CommentaryPlan` to structured `BookAnnotationPlan`. English prose,
    phrase templates, controller/API route wiring, frontend UI, source live
    lookup, Stockfish/WASM execution, and cache persistence remain closed.
- `V7`: book-style English renderer implementation.
  - Status: `V7a` starts only the minimum safe renderer-side line commentary
    skeleton. `LineCommentaryPlanner` consumes `BookAnnotationPlan` only and
    emits structured `LineCommentaryPlan` / `LineNote` data for main-line,
    defensive-resource, and restrained line-result notes. Final English prose,
    phrase templates, controller/API route wiring, frontend UI, live source
    lookup, Stockfish/WASM execution, cache persistence, and lower-layer
    meaning changes remain closed.
  - Status: `V7b` extends only the closed renderer-side line-result table for
    already-carried `BookAnnotationUnit` role/result pairs:
    `persistence/pressure_persists`,
    `defender_resource/does_not_restore_counterplay`,
    `defender_resource/resource_fails`,
    `defender_resource/resource_works`, `hold/defensive_hold`,
    `simplification/simplifies`, `conversion/simplifies`, and
    `conversion/converts`. Support/caution detail skeletons are handled only by
    the narrower V7d boundary below.
  - Status: `V7c` adds restrained `LineContext` hints to already-admitted
    `LineNote` values only. Existing `BookAnnotationSourceFrame.kind` values
    may map to coarse non-authoritative contexts: `Opening` -> `opening`,
    `Motif` -> `pattern`, `EndgameStudy` -> `endgame`, and `Retrieval` ->
    `example`. Source frames do not create notes, source authority, source
    prose, citations, recommendations, theory, result meaning, or proof
    ownership; authoritative or malformed frames attach no context.
  - Status: `V7d` carries support/caution line skeletons only from
    already-admitted public-safe prepared support/negative lines behind an
    admitted strong `BookAnnotationUnit`. `BookAnnotationUnit.supportingLines`
    and `BookAnnotationUnit.cautionLines` carry only SAN/UCI/tested/reply/
    resource line payloads plus closed support/caution kinds. Missing detail
    line data fails closed for that detail. `LineCommentaryPlanner` may emit
    separate `supporting_line` and `caution` notes from those structured
    details only; support/negative ids alone do not create notes. Caution
    details never become primary line/result notes, use no natural/tempting
    wording, and do not create recommendations.
  - Status: `V7e` adds the first closed English line-comment writer above the
    skeleton. `EnglishLineCommentaryWriter` consumes `LineCommentaryPlan` only,
    groups by annotation id, requires a main-line note plus compatible
    line-result note, uses only the closed `LineNoteMeaning` phrase table, and
    emits at most one compact book-style comment per annotation. It may append
    support/caution sentences only behind that main comment. Missing main/result
    notes, unsupported meanings, caution-only, and support-only inputs remain
    silent. `CommentaryRenderer.render` converts the English comment into a
    `PublicPhrase`; `PublicSurfaceTemplate` may use that phrase for the
    primary block claim id only when phrase capability allows
    `line_commentary`. If no safe phrase-token handoff exists, public text
    remains empty rather than falling back to raw `PublicClaim.text`, evidence
    ids, or role labels. The lower `CommentaryRendererContract.render` remains
    the deterministic structured public contract.
- `V8`: backend controller/API wiring.
  - Status: backend controller/API wiring is open only for the narrow public
    render transport. `POST /api/commentary/render` maps to
    `controllers.Commentary.render`, registered from `app/Lila.scala` through
    the existing MacWire controller pattern. The controller is thin: it decodes
    through the existing `CommentaryRequest` shape via
    `CommentaryPublicJsonTransport`, which calls `CommentaryBackendSeam.render`,
    not `renderDebug`. Caller-supplied `debug` remains ignored by the public
    route and cannot expose `internal`.
    `CommentaryPublicJsonTransport` owns the public JSON guard. It lowercases
    field names and removes separators before matching, then rejects
    completed-probe, root/child probe, probe request, candidate-line assembly,
    branch id, parent id/prefix, cache key, source row,
    caller-supplied/internal proof id/proves, and raw probe/cache-shaped
    transport fields at top level or nested under `enginePacket`. Valid typed
    `RuntimeEnginePacket` fields such as `engineConfigFingerprint` and
    `pvLines` remain accepted as certification runtime intake only when the
    public request carries no caller-supplied certification claims; public
    `enginePacket.claims` and unpaired baseline packets are rejected. The
    public route remains closed to product frontend proof fields, public
    completed-probe transport, live source lookup, cache persistence, prose
    changes, and frontend SAN authority.
- `V9`: product frontend UI.
  - Status: first move-focused analyse product surface plus local-probe
    integration is open. The stable runtime names are `moveExplanation.ts`,
    `moveExplanationView.ts`, and `localProbe.ts`. `AnalyseCtrl` refreshes the
    surface from exact current node identity (`currentFen`, `nodeId`, `ply`) on
    path changes. By default it posts only through `POST /api/commentary/render`
    via `commentaryBridge.ts`. When explicit local probe mode is enabled, it
    first requires the server-provided analyse config to mark local probing as
    available, then runs the existing local ceval/Stockfish path for root
    `MultiPV 3` at target depth `18` with floor `16`, then child defender
    probes for the first two root candidates at `MultiPV 2`, packages only
    completed UCI lines beside a clean move request subset with no nested
    `enginePacket` or `debug`, and sends that wrapper to the non-production
    internal route
    `POST /internal/commentary/render-local-probe`. The backend
    remains the exact FEN/UCI replay, SAN, proof, and claim authority. The
    surface may show backend-owned `RenderText.publicText` and public SAN line
    notation decoded as public variation evidence. It does not parse sources,
    create claims, rank/admit/suppress, generate chess prose, or expose
    debug/internal/probe/cache/raw PV, depth, eval, or engine-label fields. If
    local probe construction fails or the internal response has no public line
    evidence tied to a visible block, output remains quiet.
    In production mode the internal route is rejected before a JSON body parser
    is installed.
- `V10`: full aggregate validation and release hardening.
  - Status: V10 commentary-path validation and hardening completed for the
    current move-focused product slice. Broad checks covered commentary module
    tests, backend compile, controller route registration, public/local-probe
    transport boundaries, frontend typecheck, move-explanation frontend tests,
    retrieval quality tooling, and whitespace checks. Two boundary defects were
    fixed: the non-production local-probe transport now rejects nested
    `enginePacket` / `debug` request fields, and the internal local-probe
    controller rejects production mode before installing the JSON body parser.
    Visible aggregate UI test drift remains outside this commentary V10
    acceptance because it is tied to unrelated legacy/account-intel surfaces,
    not to the move commentary path.

Post-`V10` product ambition record:

These items are recorded as future product-quality directions, not as live
runtime authority and not as permission to reopen lower Object, Delta,
Certification, Sxx, source, or renderer meaning contracts.

- Whole-game review/report pass:
  - Future goal: walk a whole game and select the moves that deserve notes.
  - Boundary: do not annotate every move by default and do not create broad
    fallback prose for quiet or unproved positions.
  - Current status: deferred. There is no live post-`V10` runtime review/report
    layer in this worktree. Whole-game note selection must not be rebuilt until
    the lower claim/admission/root-witness path can expose complete,
    move-local, exact-board evidence traces for the relevant phenomenon
    classes.
  - Required lower trace before reintroduction: exact input identity
    (`beforeFen`, `currentFen`, `playedMove`, and move metadata where present),
    admitted Root/Witness/Object/Delta/Certification facts, any transition or
    continuation binding, selected public claim identity, renderer specificity,
    and negative/suppressed alternatives when false-positive control depends on
    them. Engine evidence may help audit or triage those rows only after exact
    move identity is verified; it remains certification evidence and never the
    public claim owner.
  - Temporary report buckets such as player-facing note names, severity,
    reason-family counts, and silent/covered totals are not current runtime
    authority and are not acceptance proxies. They may return only after the
    lower trace above is explicit enough to distinguish true lower admission
    gaps from renderer wording gaps, selection ranking gaps, stale input, and
    engine/PV-only diagnoses.
  - `Better Plan` text may be carried only when already public-bound wording is
    supplied by lower evidence. A future review/report layer must not invent a
    plan from a raw engine move or SAN token.
- Critical moment selection:
  - Future goal: choose decision points, turning points, missed resources,
    candidate-line divergence, and repeated strategic patterns from existing
    exact-board evidence.
  - Boundary: selection value does not create new chess truth. `Critical
    Moment` is a priority label over an already selected board-backed
    `Decision Point`; it is not a separate truth owner.
- Source-enriched annotation:
  - Future goal: use opening, motif, endgame-study, and retrieval context to
    make already-proved line annotations more readable.
  - Boundary: source rows remain context only and never become theory,
    recommendation, current-position proof, result, or forced-line authority.
- Plan continuity / long arc:
  - Future goal: connect the same target, file, piece, king shell, passer, or
    simplification route across multiple plies when exact evidence remains
    bound throughout the line.
  - Boundary: no invented long-form strategic story when continuity evidence
    is absent or stale.
- Book-style English refinement:
  - Future goal: improve rhythm, concision, and chess-player readability of the
    existing English commentary layer.
  - Boundary: prose refinement may only express meanings already present in
    the selected plan/annotation/line evidence.
- Acceptance corpus:
  - Future goal: run large real PGN/FEN sweeps and evaluate quality by exact
    cell, including correctness, usefulness, silence discipline, and player
    readability.
  - Boundary: one aggregate score must not hide weak opening, middlegame,
    transition, endgame, quiet-position, or counterplay cells.
  - Current preparation: post-`V10` review/report quality tooling is removed
    from this worktree. Future corpus tooling should restart as lower-layer
    diagnostics first: classify exact-board phenomena, record admitted and
    rejected lower facts, and only then ask whether any product note should be
    selected. Generated local reports remain test/tooling evidence only and do
    not become runtime claim, renderer, API, source, or frontend authority.
- Product UX layer:
  - Future goal: turn the move note path into player-facing review products
    such as decision points, recurring patterns, weak spots, training cards,
    example games, and study export.
  - Boundary: UI labels and product groupings do not upgrade evidence or
    create new claim authority.
- Production engine operations:
  - Future goal: harden WASM availability, timeout behavior, queueing, cache
    reuse, mobile behavior, and non-disruptive coexistence with the normal
    analyse engine.
  - Boundary: engine output remains raw input until backend exact FEN/UCI/SAN
    replay and proof binding have accepted it.

This record is intentionally descriptive. The authoritative runtime names
remain the stable role-based contracts documented below, such as
`ExactBoardClaimProducer`, `EvidenceClaimProducer`,
`CandidateLineEvidence`, `CandidateLineNormalizer`,
`CandidateLinePacketHandoff`, `CandidateProbePlan`,
`CandidateLineProofCache`, `CandidateProbeControlledAdapter`,
`CandidateLineAssemblyProvider`, `CandidateRootProbeIntegration`,
`CandidateChildProbeIntegration`, `CandidateLineEvidenceLowering`, and
`CandidateLineSelection`.
`CommentaryPublicJsonTransport` is the stable public JSON transport boundary
above `CommentaryBackendSeam`: it parses only `CommentaryRequest`, calls the
public `render` entrypoint, rejects normalized internal request field names,
and returns sanitized bad-request JSON.
`CommentaryLocalProbeJsonTransport` is the non-production local-probe JSON
transport boundary: it parses a `{ request, completedProbe }` wrapper, calls
`CommentaryBackendSeam.renderInternal`, returns the same public
`CommentaryResponse` shape, and remains separate from `CommentaryRequest`.
`BookAnnotationPlanner` is the renderer-side language-neutral annotation
planner over `CommentaryPlan` only; it emits structured units and boundaries,
not prose.
`LineCommentaryPlanner` is the renderer-side line-commentary skeleton planner
over `BookAnnotationPlan` only; it emits structured notes, optional
non-authoritative context hints, and boundaries, not final English prose.
`EnglishLineCommentaryWriter` is the renderer-side English writer over
`LineCommentaryPlan` only; it emits compact comments from closed note meanings,
not from claims, prepared evidence, source rows, probes, caches, or lower
Object/Delta/Certification/Sxx state.
The V3g internal seam is `CommentaryBackendSeam` taking an optional
`CandidateProbeControlledAdapter.AssemblyResult` as an attach-only side input
after normal claim production and engine-certification fail-closed filtering
have already run. `CommentaryPipelineInput` remains untainted by candidate-line
assembly, and the assembly provider is invoked after `claimProvider`, so claim
production cannot observe it directly or through shared callback state. The
seam attaches only already-lowered `PreparedVariationEvidence` to matching
existing filtered claims by `boundClaimId`.

## Public Consumption Boundary

The current worktree exposes a deliberately narrow public extraction boundary
at [CommentaryCore.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/main/scala/lila/commentary/CommentaryCore.scala).

This boundary currently authorizes external consumption of:

- active `U-primary 18` descriptor ids
- active `U-attached 1` descriptor id
- active `Object 7` family ids
- active `Delta 2` family ids
- root-backed `U-primary` witness extraction from exact `Fen` or
  `RootStateVector`
- root-backed `U-attached` witness extraction from exact `Fen` or
  `RootStateVector`
- root-backed strategic-object extraction from exact `Fen` or
  `RootStateVector`
- strategic-delta extraction from exact before/after `Fen` pairs or object
  extractions plus a played move
- fail-closed extraction for exact-board input discipline on both public
  witness facades
- fail-closed extraction for the public strategic-object facade
- fail-closed extraction for the public strategic-delta facade

The public boundary now publishes only the live `U-attached`
`structural_space_claim` contract.

It also publishes the seven live strategic-object families through
`activeObjectFamilyIds` plus the `extractStrategicObjects*` public overloads.

It also publishes the two live strategic-delta families through
`activeDeltaFamilyIds` plus the `extractStrategicDeltas*` public overloads.

Those object overloads currently return a bundled
`StrategicObjectExtraction` carrying `rootState`, the primary/attached witness
snapshots used for extraction, and the extracted strategic objects.

Those delta overloads currently return a bundled
`StrategicDeltaExtraction` carrying the before/after strategic-object
extractions, the played move, and the extracted strategic deltas.

The remaining attached `10` rows stay shell-only and remain outside standalone
public/runtime descriptor registration and extraction. Their host vocabulary may
still surface only as payload under the live `structural_space_claim` contract.

It does **not** authorize claims that planner, outline, renderer, API, or
frontend are already wired to the same truth path.

## Source Context Boundary

Offline source-context validation for opening, motif, endgame-study, and
retrieval references is frozen in
[SourceContextContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/SourceContextContract.md).

This is a test/tooling contract only.

It does not add a public `CommentaryCore` facade and does not make external
source rows Root, U, Object, Delta, Certification, or Sxx truth owners.

Game-context source input is deferred to a separate lane. Clock, rating, time-control,
repetition, tournament, study-chapter, draw-offer, and human-risk context are
outside the current source-context contract.

Endgame result-service source input is deferred. Result-table storage, probing,
result-distance fields, and remote fallback are outside the current
source-context contract.

Endgame-study context is admitted only as named-pattern applicability with exact
material and placement rules. It does not own win, draw, loss, best move,
forced-line, or conversion truth.

See
[ExternalConsumptionAuditEvidence.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ExternalConsumptionAuditEvidence.md)
and
[CommentaryCoreBoundaryTest.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/test/scala/lila/commentary/CommentaryCoreBoundaryTest.scala)
for the current-worktree verification boundary.

## Selection Boundary

The first synthesis/editor selection contract is frozen in
[CommentarySelectionContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentarySelectionContract.md).

It introduces structured selector data only:

- `CommentaryClaim`
- `ClaimImpact`
- `ClaimBucket`
- `SelectedClaim`
- `SuppressedClaim`
- `SuppressionReason`
- `EvidenceRef`
- `CommentaryOutline`
- `WordingStrength`

This does not open renderer, API, or frontend wiring.

Runtime backend claim production is split by evidence boundary. The default
backend seam calls `EvidenceClaimProducer`, which composes the narrow
`ExactBoardClaimProducer` with a sanitized higher-evidence handoff. Exact-board
claims may come only from sanitized current Object extraction and optional
same-current Delta extraction. When `CertificationEngineRuntimeIntake` accepts
a bounded engine packet, the seam may feed the resulting same-root
`CertificationEvidenceBundle` through `CertificationExtractor` and into that
handoff; rejected or empty engine intake still contributes no higher claims.
Accepted engine evidence carries typed `CertificationEngineRole` provenance.
Default claim production preserves that provenance as bounded
`EngineCertification` refs, but those refs are not lower board truth and cannot
stand alone: public rendering also requires same-binding Certification plus an
explicit typed lower board reason (`Root`, `Witness`, `Object`, or `Delta`).
A generic exact-board carrier proves board identity only; it does not by itself
authorize engine provenance as public board explanation.
Other higher-layer runtime claims require a private sanitized
`EvidenceClaimHandoff`: same-current `CertificationExtraction`,
typed `StrategyProjectionAdmissionResult` values, or normalized
`SourceContextCandidate` values. Projection claims may be emitted only from an
admitted `StrategyProjectionAdmissionResult` whose authority is
`DescriptorCertifiedRuntime`. The canonical default producer is
`StrategyProjectionAdmissionProducer`: it consumes same-root
`StrategyGeometryFoundation.CertifiedTruth` (`K`) plus explicit admitted-region
ids through `StrategyGeometryEngine.classify`, and it remains fail-closed by
default. The public-Sxx foundation is descriptor-driven:
`StrategyProjectionSliceDescriptor` owns the region id, center/prototype,
allowed evidence kinds, carrier binding laws, engine proof-identity policy,
distance algebra `d`, overlap policy, phrase capability, falsification burden,
rival bands, and rollout flag for each slice. The descriptor is a
function/algebra contract, not a visual geometry object: `A` names the admitted
region, `mu` names the center/prototype, `d` is the descriptor-owned
route/scope/family fit function, overlap policy orders already certified
memberships, phrase capability bounds public wording, and falsification burden
names the anti-cases that must fail before public lowering.
`StrategyGeometryEngine.classify` is a projection classifier over those
descriptors. It classifies only already certified truth into region memberships
with concrete center, finite `d` fit/ranking score, classification, overlap
role, and rank. `d` is not a chess metric and overlap is not truth resolution;
both apply only after hard admission. Classification rechecks the descriptor's
carrier burden, route-bound projection evidence kind, and exact-board seed
proof before any finite membership exists, so an injected or synthetic `K`
cannot bypass the descriptor contract by choosing a weaker burden. The
classifier does not read raw lower evidence, source rows, renderer state, or
frontend/API input, and it does not create truth. The producer lowers only public-primary
memberships to typed admitted projection results. Each live descriptor is also
covered by the test-only per-slice falsification harness: exact cases must
admit, while near-miss, false-rival, and shortcut-negative cases must fail; a
false-rival family is blocked before `CertifiedTruth(K)` is minted. The older raw
`StrategyProjectionAdmission.admit` path is not production main code. It is a
`src/test` validation scaffold for existing Sxx contracts, produces
`LegacyValidationScaffold` admissions, and is explicitly non-lowerable by
`EvidenceClaimProducer`.

Canonical Sxx terminology is frozen as follows:

| Term | Meaning |
| --- | --- |
| `semantic boundary` | Sxx meaning, rival separation, and false-positive shape; not public admission |
| `validation scaffold` | exact-board `src/test` coverage for legacy Sxx contracts, including raw matcher checks and corpus rows |
| `LegacyValidationScaffold` | non-public authority attached to validation-scaffold admissions; never lowerable to public claims |
| `CertifiedTruth(K)` | factory-minted same-root carrier bundle consumed by projection geometry |
| `StrategyProjectionSliceDescriptor` | production owner of `A`, `mu`, descriptor-owned `d` algebra, carrier binding law, overlap policy, phrase capability, falsification burden, rival bands, and rollout flag |
| `DescriptorCertifiedRuntime` | only projection admission authority that may lower into public claims |
| `production public admission` | descriptor/K path from `CertifiedTruth(K)` through geometry and producer into `EvidenceClaimProducer` |

For Sxx projection work, `start-ready`, `live admission`, `runtime admission`,
and `live runtime` are obsolete production terms. They may survive only in
historical test names or lower-layer Object/Delta/Certification runtime
sections. They must not be used to authorize Sxx public surface.

`CertifiedTruth` is factory-minted only: callers must use
`CertifiedTruth.fromTrustedCarriers`, which requires a same-root state,
matching proof burden, required exact transition identity,
owner/anchor/route/scope-bound lower carriers, and a bound `Certification`
carrier plus `EngineProofIdentity` whenever engine roles are required. Runtime
descriptor-bound candidates are minted through `StrategyRuntimeKProducer`
before classification. `StrategyProjectionAdmissionProducer` accepts only those
runtime `K` candidates, not caller-supplied pre-minted `CertifiedTruth`, and
public projection claims carry `projectionRuntimeKId` from the producer-created
`RuntimeK` token. The producer applies descriptor-owned proof-burden
minimums rather than trusting candidate-supplied burdens, derives per-slice
falsification reasons for wrong anchor/route/evidence kind, stale root, missing
exact-board fact, support disposition, and missing required carrier kinds, and
enforces carrier binding payload laws such as same entry square or same contact
square across every lower carrier in the K bundle. A Certification carrier may
not rebind a different square, target, piece, or defender-resource payload than
the exact-board/object/delta carrier for the same descriptor law. Descriptor
phrase capability is carried through admission into the Projection
`CommentaryClaim`; the renderer clamps public predicates, line-comment
permission, and forbidden terms from that capability. Direct fake K
construction or copy-style rebinding is not part of the runtime contract.

Typed strategy probe requests are internal request intent only.
`StrategyProbeRequestPlanner` derives `Q(beta, tau, policy)` from a
`ProofBurden`, a legal replay-validated `ExactTransitionIdentity`, and
server-owned role policy. These requests carry engine role, exact board or
transition start position, depth, MultiPV, and fingerprint requirements, but
they do not create `CertifiedTruth`, projection admission, public claims,
candidate-line evidence, renderer prose, source provenance, or public API
fields. Candidate-line `CandidateProbeRequest` remains a separate line
orchestration model and is not the certification proof-burden request type.
Runtime Engine E evidence that claims a certification role must bind back to
this server policy shape through a `q-...` request id whose role, certification
family, owner, anchor, node id, and ply match the packet claim, a policy
fingerprint matching the engine search fingerprint, server floor/target depth
and role MultiPV/PV-length minima, and a satisfied role invariant report.
`EngineProofIdentity` must carry the same Q request id/policy/engine identity,
match the K proof burden family/owner/anchor/route, and name the bound
Certification carrier id before an engine-role K can enter geometry.
Best-defense roles require a semantic coverage report in addition to MultiPV
shape. Comparative, conversion, and counterplay roles require a bound baseline;
release, causality, and persistence roles require a transition-bound packet.
Public cap reports for horizon, ambiguity, tablebase, mate dominance, or
material collapse are terminal and cannot mint certification evidence.
Claim-shaped Sxx candidates are still accepted only as an unsafe handoff field
and remain ignored by the producer. This handoff is not a
frontend/request payload, not live source lookup, and not a raw engine/source
escape hatch. Accepted engine intake status alone cannot create a claim; raw
source rows cannot create a claim; prepared variation evidence cannot create a
claim. Explicit injected backend `claimProvider`s still override the default
producer path for contract tests and controlled future producer paths.
The default backend now calls the canonical
`StrategyProjectionAdmissionProducer`, but that producer is fail-closed by
default and region-enabled rather than band-enabled. `S01-S25`
`startReadyBandIds` do not themselves open projection production. Current S23
classifier has two live admitted regions,
`A_S23_endgame_entry_or_opposition` and `A_S23_line_access_activity`; broad
line-access membership remains support-only unless the focused region is
enabled and wins overlap resolution.

Selection consumes already admitted typed claims from the current branch
pipeline. It may choose `mustLead`, `shouldLead`, `canLead`, `support`,
`contextOnly`, or `suppress`, but it must not create chess truth. Engine E only
reaches selection through Certification; an `EngineCertification` evidence ref
is selectable only when same-root `Certification` evidence with matching owner,
anchor, route, and scope is also present, and the claim has a same-binding typed
lower board reason (`Root`, `Witness`, `Object`, or `Delta`). Missing scope on the claim,
Certification ref, EngineCertification ref, or engine-certified carrier fails
closed before ranking. Source context remains context-only
unless an exact-board lead is already selected, and even then it remains
non-authoritative. Source context refs cannot serve as board-claim evidence.
Raw engine refs cannot be smuggled through another layer or through Sxx lower
carriers, and they cannot be hidden as lower carriers on non-Projection board
claims. Sxx lower carriers must carry owner, anchor, route, and scope binding.
S24 is not the generic tactic owner and remains public-closed as the current
anti-case/blocker: even both same-target forcing and same-target conversion
scaffold evidence are suppressed at selection until a future descriptor-backed
RuntimeK region exists. Selected claim
payloads are clamped to the computed outline `wordingStrengthCap`; selected
source-context payloads remain capped at `context_only`.
Lead bucket priority is frozen as `mustLead > shouldLead > canLead`; impact
axes, including `novelty`, rank claims only inside the selected lead bucket.
Generic transition carriers (`last_move_transition` and non-capture
`pawn_structure_transition`) are exact evidence but are emitted as
`SupportOnly`, so they do not mask a concrete selected current-board reason
and do not create a generic primary-block product note.
Standing position-local tactical roots are lower support facts, not public
opportunity claims and not move-causal claims. The generic
`tactical_liability` route is selector-forbidden as a lead route. Tactical
board refs may become move-local public claims only through narrow
validated-transition slices such as a moved non-pawn left loose or a legal
non-slider royal fork. The moved-piece-left-loose slice is not admitted from a
root ref alone: it requires exact replay, same-piece origin/destination
identity, a non-pawn/non-king moved piece, no pre-existing `loose_piece` fact on
the moved piece at the origin, a bound Delta evidence id
`moved_piece_left_loose_transition`, a bound `loose_piece` lower carrier, and a
bound `immediate_capture` lower carrier proving a legal side-to-move capture on
the destination. Capture moves must also pass a whole-transition material gate:
the opponent's best immediate exchange gain on the destination must exceed the
value of any piece captured by the played move. Equal recaptures and already
favorable captures are not `moved_piece_left_loose` public reasons. If the
opponent has exact mate in one on the after-board, the loose-piece reason is
suppressed behind the mate truth. Created pin geometry remains
measurement/deferred material until a strategy-facing reason needs a narrower
contract. A current-board opportunity may be public only through a
dedicated exact-board Object route, currently the bounded `immediate_capture`
contract: the side to move must have a legal capture onto a non-king target
that is also a same-square `loose_piece` lower carrier. Plain
`immediate_check` is also public board text only; exact mate-in-one may support
king-safety wording because the mate is legal-move board truth, not raw engine
mate/PV truth.
Duplicate suppression is scoped to competing Sxx projections; lower Object,
Delta, and Certification carrier/support claims are not duplicate-suppressed
just because they share an owner, anchor, route, and scope key.

`CandidateLineSelection` is the stable cross-family line-selection model
boundary. It consumes `CommentaryOutline` after claim selection and may choose
structured line explanation decisions over already public-safe
`PreparedVariationEvidence`. A strong primary line can come only from the
selected lead exact-board claim and only when the same bound claim has both a
main/root candidate-style proof and a defender-resource/defender-reply style
proof. A candidate/root proof without defender-resource proof remains
weak/context only and is not emitted as public outline variation evidence.
`CommentaryOutline.variationEvidence` is the selected strong public set:
primary proof, its defender/resource companion proofs, and selected
support/negative proofs only when a strong primary exists. Source-context,
retrieval, and raw-engine owned proofs cannot own or select a line. Opening,
motif, endgame-study, and retrieval line-test refs may annotate only proof ids
already selected from an exact-board claim; retrieval remains illustrative
only. Failed tempting, premature, and release-risk proofs remain
negative/support evidence and cannot become the primary recommendation. Empty
safe line sets remain silent.
`CommentaryOutline.annotationSelections` is the strong-only
language-neutral annotation handoff. It carries `PlanAnnotationSelection`
values with claim id, primary proof id, companion/support/negative proof ids,
coarse source frame kinds (`opening`, `motif`, `endgameStudy`, `retrieval`),
source line-test ref ids as metadata, `PlanAnnotationStrength.Strong`, and a
wording cap. It is absent for candidate-only weak lines and must not contain
phrase templates, English prose, source-ref chess parsing, or new truth
ownership.
`BookAnnotationPlanner` consumes `CommentaryPlan` only and may produce a
structured `BookAnnotationUnit` only when the selected main claim is unblocked,
admitted, exact-board-bound, non-source, non-engine, non-renderer, has a
same-claim strong annotation selection, has a public-line candidate/root
primary proof, and has at least one public-line defender-resource or
defender-reply companion proof. Candidate-only, defender-only, blocked,
source-only, retrieval-only, `boundary_only`, `internal_only`, unsafe/internal
proof-id, missing-proof, raw-provenance, and unmatched-frame cases fail closed
to `BookAnnotationBoundary`. Support/negative proof ids are carried only on an
existing strong unit; failed tempting, premature, and release-risk proofs
cannot become primary units. Retrieval frames remain illustrative metadata.
The planner emits no English prose, phrase templates, controller/API fields,
frontend UI, source lookup, engine execution, or cache persistence.
`LineCommentaryPlanner` consumes `BookAnnotationPlan` only and may produce
structured `LineNote` values from `BookAnnotationUnit.lineSan`, already-carried
resource/reply moves, and the closed safe result table:
`persistence/pressure_persists`,
`defender_resource/does_not_restore_counterplay`,
`defender_resource/resource_fails`, `defender_resource/resource_works`,
`hold/defensive_hold`, `simplification/simplifies`,
`conversion/simplifies`, and `conversion/converts`. Empty unit sets, low
wording caps, empty main lines, unsupported results, and role/result
mismatches create no fallback notes. Defensive-resource notes remain
structural only and do not create result meaning by themselves.
V7d may carry separate support/caution skeleton details from already-admitted
support/negative prepared lines behind the same strong unit. These details are
line-scoped only: `supportingLines` may mirror the closed safe result table
when the prepared line role/result matches, and `cautionLines` may represent
only early-move, premature-move, or releases-counterplay caution kinds from the
explicit negative role/result pairs. Missing detail line payloads fail closed,
and support/negative ids alone do not create notes. Caution skeletons do not
become primary line/result notes and must not use natural/tempting wording.
Source frames are not parsed into line meaning; they may attach only
coarse non-authoritative `LineContext` hints (`opening`, `pattern`, `endgame`,
`example`) to notes that already exist. Raw `sourceRefIds` are not exposed by
the line skeleton. The line skeleton emits no final English prose, phrase
templates, controller/API fields, frontend UI, source lookup, engine
execution, cache persistence, or lower-layer meaning.
`EnglishLineCommentaryWriter` sits strictly above that skeleton. It consumes
only `LineCommentaryPlan`, requires both a main-line note and a compatible
line-result note, maps the closed safe `LineNoteMeaning` values to short
English result phrases, and emits no fallback comment for unsupported or
detail-only inputs. Source contexts remain non-authoritative metadata and do
not create prose claims. Support/caution notes can append short sentences only
after the strong main comment exists and only when they are bound to the same
primary proof id as the main/result pair; caution never becomes the main
comment. SAN tokens, including Black-move ellipses, are preserved.
`CommentaryRenderer.render` may use this comment for a primary block claim id.
If no safe English line comment exists, public text remains empty rather than
falling back to renderer role labels.
`PublicVariationEvidenceSafety` is the package-private commentary helper
shared by `ClaimSelector`, `CandidateLineSelection`, and
`BookAnnotationPlanner` for public proof-id safety, prepared proof safety,
claim binding, and source line-test proof-id parsing, so internal
branch/cache/probe-shaped tokens fail consistently across these gates.

The current selection runtime expansion covers the S07/S08/S21
initiative/release/counterplay rival cluster. It consumes only already
admitted Projection, Certification, Engine, and SourceContext claims. Exact S07
initiative conversion may lead, nonredundant S21 counterplay survival may
support, adjacent S08 rivals can be suppressed with `rival_band`, and
support-only, raw-engine, source-context, wrong-binding, and renderer-strength
shortcuts remain fail-closed.

Selection also covers the S15/S16 passer creation/suppression cluster with
S13/S14 structural support. It consumes already admitted S13/S14/S15/S16 typed
Projection claims only. Exact S15 can lead only when the same-owner
Root `candidate_passer` and Witness same-candidate creation-route lower support
are bound to the selected candidate anchor on frozen `s13_wing_damage` or
`s14_chain_base` routes. Exact S16 can lead only when the defender-owned enemy
Witness `passed_pawn_entity_state` and route-specific same-owner certification
support are bound: `FortressDrawCertification` for `blockade_hold`,
`short_run_slider_gate_restriction` plus `PerpetualCheckHolding` for
`restriction_hold`, and `PromotionRace` for `non_losing_race`.
S13/S14 can support S15 only with the same owner, candidate anchor, and scope;
they remain support and do not become S15 truth owners, even when their impact
score is higher than the admitted S15 claim. Existing-passer-only,
blocker-shell-only, split-anchor, forged carrier-kind, unsupported-route,
S16 self-owned passer suppression where `owner` and `defender` name the same
side, ambiguous or incoherent side/beneficiary/defender rival bindings,
raw-engine, source-context,
retrieval, and renderer-strength shortcuts remain fail-closed.

Selection also covers the S17/S18/S19/S22 conversion/simplification/hold
cluster. It consumes already admitted S17/S18/S19/S22 typed Projection claims
only. Exact S17 can lead only with same-owner Witness
`same_piece_liability_anchor_seed`, typed `same_piece_repair_route_seed` or
`same_piece_exchange_relief_seed` support, and `liability_relief_certified`
projection evidence. Exact S18 can lead only with same-owner Witness
`bishop_pair_state` and route-specific Certification support for
`bishop_pair_to_initiative`, `bishop_pair_to_structure`, or
`bishop_pair_to_material`. Exact S19 can lead only with same-owner Delta
`TradeInvariant` and route-specific Certification support for
`trade_invariant_to_material` or `trade_invariant_to_hold`. Exact S22 can lead
only with same-owner certified `fortress_draw_hold` or `perpetual_hold`
support. Declared Projection evidence must be bound to the same owner, anchor,
route, and scope as the S17/S18/S19/S22 claim. S19 cannot steal
same-owner/anchor/scope S17 liability-relief, S18 bishop-pair conversion, or
S22 hold ownership. Conversion claims remain below certified hold unless
result/material impact and the `bishop_pair_to_material` route with bound
`bishop_pair_material_conversion_certified` evidence justify conversion.
Generic relief wording, unbacked conversion, wrong-binding, raw-engine,
source-context, retrieval, and renderer-strength shortcuts remain fail-closed.

Selection also covers the S01/S02/S03/S04 king-attack cluster. It consumes
already admitted S01/S02/S03/S04 typed Projection claims only; selector tests
that pass synthetic admitted claims verify downstream ranking and
non-redundancy, not current admission production. Current large-corpus
false-positive hardening keeps the corpus S01, S03, and S04 exact rows deferred
unless their `AttackScaffold` support is stronger than geometry/shelter/duty
alone. Their frozen route names remain reserved, but diagnostic probe evidence
is not public admission. An exact S01 public claim would still require
same-owner Witness `available_lever_trigger`, Witness
`pawn_push_break_contact_source`, Certification `CertifiedKingSafetyEdge`,
bound `king_wing_storm_route_certified` projection evidence, a frozen
`same_wing_contact` or `attack_edge_same_king` route, and a future scaffold
contract binding the pawn contact source and target to the attack host.
Exact S02 can lead only with same-owner Object `AttackScaffold`,
Certification `CertifiedKingSafetyEdge`, a loose-bound scaffold support
fragment whose `loose_support_squares` intersects the public king-ring target
set and is directly attacked by one of the public source squares, bound
`king_ring_concentration_route_certified` projection evidence, and a frozen
`direct_piece_concentration` or `lane_strengthened_concentration` route. Exact
S03 can lead only with same-owner Witness `diagonal_lane_only`, Object
`AttackScaffold` carrying loose-bound support whose loose square is one of the
public diagonal endpoints and is directly attacked by the public diagonal
source, Certification
`ComparativeKingFragility`, Certification `CertifiedKingSafetyEdge`, bound
`diagonal_king_attack_route_certified` projection evidence, and a frozen
`king_facing_diagonal_entry` or `fragility_linked_diagonal` route. The current
corpus keeps S03 deferred unless that loose-bound scaffold support is present.
Exact S04 still requires defender-owned Object `KingSafetyShell`, same-owner
Certification `CertifiedKingSafetyEdge`, bound
`king_shelter_breach_route_certified` projection evidence, and a frozen
`shell_payload_breach` or `support_break_breach` route; the current corpus rows
remain deferred until the shelter-breach support contract is strong enough for
public admission. The
`support_break_breach` route also requires same-owner Witness
`diagonal_lane_only`. Owner, defending king anchor, defender, route, and scope
must remain clear; king-attack projection fails closed when `owner` and
`defender` identify the same side. S01 storm, S02 concentration, S03
diagonal lane, and S04 shelter breach remain distinct when route or anchor
differs. Certified result/conversion owners may outrank king-attack projection;
raw engine, generic attack wording, source/retrieval motif tags, wrong-binding,
and renderer-strength shortcuts remain fail-closed.

Selection also covers the engine-certified eval swing versus board-explainable
Sxx conflict boundary. Raw engine eval swing is always suppressed. Bounded
`EngineCertification` can influence ranking only inside a Certification-layer
claim that also carries same-root `Certification` evidence and a same-binding
typed Root, Witness, Object, or Delta board reason. A generic exact-board
carrier is not enough. An
engine-certified eval swing without that carrier is `no_board_reason` and
cannot become lead, even if its numeric swing is larger than an admitted Sxx
projection.
Board-explainable admitted Sxx can therefore beat opaque engine swing.
Engine-certified result/material Certification can beat Sxx only when
result/material impact, same-root Certification, and same-binding exact-board
or lower board carrier justify the `mustLead` bucket. Stale, wrong-node, wrong-FEN,
wrong-route, wrong-engine-config, MultiPV ambiguity, and untyped mate/cp score
boundaries remain fail-closed before ranking. The renderer still cannot turn
eval swing into best-move, winning, or forced-result wording.

Selection also covers context-only fallback breadth for source-context rows.
Opening, motif, endgame-study, and retrieval remain sidecar context only, not
Root, U, Object, Delta, Certification, or Sxx truth owners. Opening context can
enter `contextOnly` only when no exact-board lead exists and never as
best-move, theory-truth, forced-line, result, or current-position proof.
Endgame-study context requires exact material/placement applicability and
cannot claim win, draw, loss, forced line, or forced conversion. Motif context
requires exact detector carrier parity; motif tags alone are suppressed, and
motif line-test refs remain source-context links rather than proof ownership.
`mate_net` and `perpetual_check` require certification carriers, while
deferred/helper-required motifs remain blocked until their exact helper/carrier
exists; the current runtime adapter blocks those certification-only motifs
until that exact certification carrier is part of the normalized input.
Retrieval remains non-authoritative example context. Ambiguous opening transpositions,
retrieval truth-promotion, endgame result-language shortcuts, gameContext,
practicality, tablebase, and endgame result-service source families remain
fail-closed with typed suppression reasons such as `source_context_only`,
`ambiguous_transposition`, `retrieval_non_authoritative`,
`forbidden_shortcut`, and `no_board_reason`. Selector fallback derives those
hard reasons from source-row evidence ids: canonical opening refs must use
`opening-position:*:canonical`, motif refs must match
`motif-example:*` to `motif-detector-carrier:*`, endgame-study refs must match
`endgame-study:*:applicable` to `endgame-study-applicability:*`, and retrieval
refs must stay `retrieval-example:*` without truth-promotion ids. Optional
`retrieval-illustration:*` and `retrieval-line-test:*:context` refs are
source-context links only; they cannot transfer proof from a retrieved game,
recommend a move, create a verdict, expose game result/player/event metadata,
or own prepared variation evidence. The current
runtime reconciliation boundary is `SourceContextAdapter` to
`SourceContextCandidate` to `SourceContextClaimBoundary`. The adapter normalizes
source-family inputs and rejects forbidden family claims before selection;
`SourceContextClaimBoundary` converts normalized candidates into
`ClaimLayer.SourceContext` claims only. This path does not parse raw source rows
inside Phase 3, create board truth, open controller/product frontend wiring, or
bypass selector fallback. Source-family internals stay on the
adapter/source-context side of that boundary. Endgame-study Phase 4 fixture ids
may differ from study ids only when the normalized exact applicability carrier
is route-bound to the study id; unbound fixture ids remain fail-closed. When an
opening source-context ref carries specific game, player, event, URL, or raw
HTTP citation material, it remains retrieval-lane material and fails closed
before context fallback. When an exact-board lead exists, source context may only remain bounded
support/context and cannot outrank the lead. Renderer strength still cannot
convert source context into current-position truth.
Safe selected source context carries soft selector reasons such as
`source_context_only` or `retrieval_non_authoritative` on the selected context
item. It is not duplicated in `suppressedClaims`; that field is reserved for
claims the renderer must not revive.

The renderer contract is frozen in
[CommentaryRendererContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryRendererContract.md).
The renderer consumes only `CommentaryPlan`, not raw selector claims, raw
engine packets, or raw source rows. It must not upgrade `wordingStrengthCap`,
admit a lead, rank claims, convert source context to truth, interpret raw
engine, merge opening source vectors, infer missing evidence, or discard
suppression reasons.

The outline-builder contract is frozen in
[CommentaryOutlineBuilderContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryOutlineBuilderContract.md).
`CommentaryOutlineBuilder` is a stable structured mapper from selector-owned
`CommentaryOutline` to `CommentaryPlan`. It may map `lead` to `main`,
`support` to `support`, `context` to `context`, `contrast` to `contrast`,
`suppressedClaims` to `blocked`, `evidenceRefs` to `evidence`,
`variationEvidence` to `variationEvidence`, `annotationSelections` to
`annotationSelections`, and `wordingStrengthCap` to
`wordingRules.maxStrength`. It must not select, rank, admit, suppress, revive,
reinterpret evidence, promote source context, merge opening source vectors,
parse source line-test refs into chess meaning, upgrade wording strength,
generate renderer prose, or define API/frontend payload shape. If no lead,
support, context, or contrast exists, the resulting plan is `noCommentary`;
blocked do-not-say material and evidence references are still carried.

The public-safe prepared variation evidence contract is frozen in
[VariationEvidenceContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/VariationEvidenceContract.md).
That contract now also defines the internal `CandidateLineEvidence` boundary
under `lila.commentary.line`. Candidate line evidence is the stable
branch/probe-tree evidence shape before lowering to public-safe prepared
variation evidence. It may preserve root alternatives, child defender branches,
candidate rank, MultiPV index, branch id, optional parent branch id, start FEN,
node id, ply, side to move, SAN/UCI lines, requested and realized depth,
freshness, engine fingerprint, legal replay status, provenance kind, source
hint refs, surface allowance, wording cap, explicit failure reasons when
rejected, and a prepared-variation target link. It is not a public prose
object, not a renderer/backend payload, and not a truth owner by itself. Source
hints are non-proof metadata, retrieval is not a current-position candidate
source, and failed/premature/release-risk roles remain internal/deferred at the
candidate-line boundary.
`CandidateLineNormalizer` is the backend-owned exact-FEN UCI-to-SAN replay
boundary for this internal evidence. It may normalize raw UCI PV fixtures into
`CandidateLineEvidence`, but it must not create claims, lower to public
prepared variation evidence, wire controller/frontend payloads, schedule child
probes, cache output, or expose raw PV through renderer/backend public JSON.
`CandidateLinePacketHandoff` is the internal sanitized multi-packet handoff for
candidate-line evidence. It can bind one root packet to the current FEN/node/ply
and attach child defender-resource or defender-reply packets only when their
parent branch id exists, their node/ply matches the declared parent prefix, and
their start FEN matches exact replay of that prefix. Duplicate root branch ids
fail closed before child binding, and source-hint provenance remains non-proof
metadata rather than engine proof provenance. It delegates line replay to
`CandidateLineNormalizer` and does not change public request/response shape,
lower to `PreparedVariationEvidence`, schedule probes, create claims, or open
renderer prose.
`CandidateProbePlan` is the internal candidate probe orchestration and
cache boundary under `lila.commentary.line`. It freezes the backend/internal
plan shape only: root request `MultiPV 3`, target depth `18`, public floor
`16`, strong/cache target depth `20`, child defender-resource requests for root
ranks `1` and `2` at `MultiPV 2`, and rank `3` child proof only from explicit
expanded budget or revalidated strong/cache target-depth-20 cache. It derives
child starts from public-line-eligible sanitized root `CandidateLineEvidence`
by exact replay of the parent candidate prefix and then feeds accepted packets
through `CandidateLinePacketHandoff`. Shallow, stale, illegal, source-hint, or
invalid-rank root evidence cannot spawn child probe requests. Cache keys
must bind normalized full FEN, variant, node/ply consumption binding, engine
fingerprint, target/floor/realized depth, MultiPV, role, parent branch/prefix,
parent root rank for child probes, normalized first move/prefix when applicable,
and freshness; cache hits remain
provenance until revalidated, target-depth-18 cache entries do not open the
rank-3 default child path, and proof cache cannot be populated from source
rows, source context, or retrieval. This boundary does not run Stockfish, call
frontend WASM, look up sources, create `CommentaryClaim`, lower evidence, write
prose, or change public request/response shape. A main candidate without
defender resource remains insufficient for strong line explanation promotion,
and natural/tempting wording remains deferred/public-default-off.
`CandidateLineProofCache` is the stable internal cache facade for candidate-line
proof entries. It mediates reads and writes only for
`CandidateProbeCacheKey` / `CandidateProbeRequest`-derived proof entries and
does not reuse the generic eval, source, or retrieval cache. Reads return
typed `CandidateProbeCacheLookupResult` values only after the existing
`CandidateProbeCacheEntry.revalidateFor` logic accepts the request binding.
Writes are committed only through the provider/adapter-owned receipt path for
accepted engine-probe results whose key exactly matches the completed payload;
ordinary callers cannot populate the cache by handing the facade an arbitrary
`CandidateProbeCacheWriteCandidate`. Source-context, retrieval, source-row,
revalidated-cache, stale, under-depth, wrong-FEN, wrong-variant,
wrong-node/ply, wrong-engine, wrong-depth, wrong-MultiPV, wrong-role, and
wrong-parent-prefix entries fail closed before storage. Cache hits are never
rewritten as new writes. The current implementation is deterministic in-memory
only for contract tests and optional internal provider wiring; production
persistence remains deferred.
`CandidateProbeControlledAdapter` is the internal adapter that consumes
already completed external probe payloads and revalidated cache lookup results.
It converts accepted root/child payloads into existing candidate-line packets,
assembles them through `CandidateLinePacketHandoff`, may lower sanitized
candidate evidence through `CandidateLineEvidenceLowering` when supplied an
explicit binding, and returns cache write candidates for accepted completed
probe results only. Cache lookup payloads must match the effective revalidated
request and cache entry key before use. It does not execute Stockfish/WASM,
persist cache, perform live source lookup, create `CommentaryClaim`, generate
prose, or change public request/response shape. Rank `3` child cache reuse
stays behind strong/cache target depth `20` unless an explicit expanded budget
is supplied.
`CandidateLineAssemblyProvider` is the internal-only provider boundary for
completed candidate-line probes and proof-cache hits. It accepts exact
`currentFen`, `nodeId`, `ply`, `variant`, server-owned engine fingerprint,
budget, typed completed root/child payloads, an optional internal
`CandidateLineProofCache` facade for cache lookup, and an optional explicit
`CandidateLineEvidenceLowering.Binding` when prepared evidence is expected. It
derives the root `MultiPV 3` request and child `MultiPV 2` requests through
`CandidateProbePlan`, reads facade cache hits when supplied, does not expose
direct root/child cache-hit input fields, delegates to
`CandidateProbeControlledAdapter`, writes back only through the returned
provider/adapter-owned cache receipt, and returns only
`CandidateProbeControlledAdapter.AssemblyResult`. It does not execute
Stockfish, call WASM, perform source lookup, does not add a production cache
persistence backend, does not wire controller routes, does not open product
frontend UI, does not create `CommentaryClaim`, does not write prose, and does
not expose raw probe/cache/PV internals publicly. Rank `3` child proof remains
closed by default except via strong/cache depth `20` or explicit expanded
budget.
`CandidateRootProbeIntegration` is the internal-only root probe intake boundary
for a completed root probe payload from a future local executor or for a
revalidated proof-cache root hit. It uses the backend-owned root request policy
of `MultiPV 3`, target depth `18`, floor `16`, exact current FEN/node/ply/
variant, and backend-owned engine fingerprint. Backend SAN remains authoritative
through the existing normalizer/provider path. A valid root input yields three
public-line-eligible internal root `CandidateLineEvidence` values and returns
the unsatisfied next child defender-resource requests for root ranks `1` and
`2` only, at child `MultiPV 2`, target depth `18`, and floor `16`. Rank `3`
is not scheduled by the default root integration path and remains cache/
expanded-budget only. Prepared variation evidence is emitted only when an
explicit lowering binding is supplied and only through
`CandidateLineEvidenceLowering`. Cache hits can satisfy the root path but do
not emit new writes. Wrong FEN/node/ply/variant/engine, stale, shallow,
wrong-count root `MultiPV`, duplicate first move, malformed or illegal PV,
source/retrieval/root-hint provenance, and raw/untrusted root shape fail
closed to no child requests, no prepared evidence, and no cache commit.
`CandidateChildProbeIntegration` is the internal-only child probe intake
boundary for completed child payloads and proof-cache child hits corresponding
to the root stage's planned child requests. It consumes exact current context
and a valid root payload or proof-cache root hit, delegates only through the
existing provider, controlled adapter, normalizer, proof-cache, and lowering
boundaries, and returns internal assembly plus satisfied and unsatisfied child
requests. Default live child scheduling remains root ranks `1` and `2` only at
child `MultiPV 2`, target depth `18`, and floor `16`; rank `3` direct child
payloads remain closed by default while strong target-depth-20 cache hits can
satisfy the existing cache-only path. Invalid child payloads fail closed for
that child only: valid root evidence remains, no child proof lowers, no child
cache write is committed, and the request remains unsatisfied. The boundary
does not execute Stockfish/WASM, wire controller or product UI, create claims,
write prose, or expose raw child probe/cache internals publicly.
`CandidateLineEvidenceLowering` is now the deterministic internal lowering
boundary from sanitized candidate-line trees to public-safe
`PreparedVariationEvidence`. It consumes only `CandidateLineEvidence` plus an
explicit exact-board claim binding and keeps the public response shape
unchanged. The frozen policy is depth floor `16`, preferred target depth `18`,
strong/cache target depth `20`, root MultiPV public-safe target exactly `3`
with hard cap `3`, child MultiPV public-safe target exactly `2` with hard cap
`2`, and child defender replies considered only for top-two root candidates
unless third-root child proof is explicitly enabled. Rank `1` root evidence
lowers to main candidate line evidence, rank
`2` to alternative line evidence, and rank `3` to context line evidence.
Failed, premature, release-risk, source-hint, stale, shallow, illegal,
internal-only, incomplete, duplicate-rank, duplicate-MultiPV-index,
under-budget root, or under-budget child candidate evidence remains non-public.
Public proof ids are stable lowering-owned line ids and must not expose
internal branch ids, parent branch ids, ranks, or MultiPV indexes. Raw-engine,
source-context, unbound, or mismatched binding provenance fails closed before
prepared evidence is emitted.
It adds typed `PreparedVariationEvidence` carriage through selection, outline,
plan, renderer, and backend response as a bounded line-proof object. It does
not expose raw engine PV packets, raw source rows, evals, mate scores, engine
configuration fingerprints, variation hashes, or debug fields, and it does not
create Root, U, Object, Delta, Certification, Projection, source-context,
best-move, forced-line, result, winning, drawn, or oracle truth.
Lower prepared evidence may retain internal `VariationEvidenceRole` values,
UCI PV, start FEN, provenance refs, replay/freshness boundary fields, depth,
MultiPV, and the prepared `proves` token for selector/planner authority.
Public render and bridge payloads are stricter: `RenderVariationEvidence.role`
is the renderer-public `RenderLineRole` (`resource`, `caution`, `hold`,
`conversion`, `pressure`, `simplification`) and `RenderVariationEvidence`
exposes only public SAN move/line fields, test result, proof purpose, wording
cap, and surface allowance. It does not expose `proves`, start FEN, UCI PV,
provenance refs, boundary/depth/MultiPV data, engine fingerprints, cache keys,
branch ids, or raw move UCI.
The same contract now covers defender-resource, failed-tempting-move,
release-risk, hold, conversion, simplification, and persistence line-test
roles. These roles require exact-board legal/fresh/depth-bounded line evidence
plus same-binding provenance, and failed tempting or premature move evidence
remains negative/support-only rather than a main recommendation.
Opening source context may now carry structured sequence refs for move order,
pawn breaks, development lag, file ownership, king safety, transposition,
restrained compensation, and master/online divergence. These refs are
source-context material only. They may link to a prepared proof id as
`opening-line-test:*:context`, but they do not own the proof and do not expose
raw opening rows, source frequencies, theory claims, or recommendations.
Motif source context may carry `motif-line-context:*` refs and
`motif-line-test:*:context` links for attacked/restricted pieces, defender
resources, failed resources, held resources, and partial resources only after
detector carrier parity. These refs are context only; the prepared variation
proof must remain on the exact-board claim, and renderer/backend output must
not expose raw motif rows or internal proof packets.
Endgame-study context may carry `endgame-technique:*` refs and
`endgame-line-test:*:context` links for opposition, checking distance, rook
activity, pawn-ending transition risk, wrong exchange, bridge setup,
third-rank setup, side-checking setup, method/exception, defender resource, or
hold line only after exact material/placement/relation/side-to-move
applicability. These refs are context only; the prepared variation proof must
remain on the exact-board claim, and renderer/backend output must not expose
raw endgame rows, outcome fields, tablebase-style verdicts, or internal proof
packets.
Retrieval source context may carry `retrieval-illustration:*` refs and
`retrieval-line-test:*:context` links for comparable lines, similar plan
sequences, theme examples, or bounded citation context. These refs are context
only; the prepared variation proof must remain on the exact-board claim, and
renderer/backend output must not expose raw retrieval rows, snippets,
player/event/result metadata, display candidates, citation metadata, or
internal proof packets.

The first renderer executable implementation maps `CommentaryPlan` into
structured role-based output only:

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
- `RenderVariationEvidence`
- `RenderVariationMove`

`CommentaryRendererContract.render` remains the lower deterministic structured
contract. `CommentaryRenderer.render` may pass the closed English line comment
from `EnglishLineCommentaryWriter` as a `PublicPhrase` to
`PublicSurfaceTemplate`, which can populate a primary block's
`RenderText.publicText` only when the phrase matches the claim id, uses the
`line_commentary` predicate, stays within block/cap wording ceilings, and
passes forbidden-term checks. Without that phrase-token handoff
`RenderText.publicText` remains empty; raw `PublicClaim.text`, evidence ids,
and structured roles such as `Primary`, `Support`, `Context`, and `Contrast`
remain data only and are not fallback public move-explanation text. It does
not generate broad chess narration, role-label prose, raw-evidence prose, or
model-authored prose, and it does not open API/frontend wiring.
`main` becomes a primary block, `support` becomes
supporting blocks, `context` remains non-authoritative context, `contrast`
remains contrast, and `blocked` is retained only as non-public suppression
metadata by default.
`RawEngine` refs are not public render evidence; `EngineCertification` refs are
public render evidence only when the ref is directly owned by a public
Certification-layer selected claim and same-binding `Certification` evidence is
also present in `plan.evidence` with a same-binding typed lower board reason.
Public render evidence is limited to refs that are both present in `plan.evidence` and
directly referenced by an unblocked public selected claim; plan-wide evidence
without public selected-claim ownership remains non-public. If a
malformed plan puts the same claim in a selected section and `blocked`, the
blocked entry wins and the public block is not emitted. `NoCommentary` and
`hidden` renders emit no public blocks and no public evidence refs.
The renderer may not use selected public evidence ids such as
`MaterialHarvest`, `CertifiedKingSafetyEdge`, `InitiativeWindow`,
`same_target_forcing_realization`, `liability_relief_certified`,
`weak_pawn_target_pressure_persistence_certified`, `fortress_hold_certified`,
or `mobility_domination_route_certified` to synthesize fallback public text.
Those ids remain structured evidence only unless a separate safe `PublicPhrase`
lowers them. The renderer boundary now lowers
`CommentaryPlan` to `PublicCommentaryPlan`; final rendering maps only
`PublicClaim` objects to public blocks, with `PhraseCapability` carrying the
allowed predicate set, wording ceiling, line-comment permission, and forbidden
terms.

The backend commentary seam contract is frozen in
[CommentaryBackendSeamContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryBackendSeamContract.md).
`CommentaryBackendSeam` defines the stable backend request/response boundary
now reachable through the narrow public backend route
`POST /api/commentary/render` without opening frontend wiring, product UI, or
live source integration. The request shape is `CommentaryRequest`: `currentFen`,
optional `beforeFen`, optional `playedMove`, `nodeId`, `ply`, optional
`RuntimeEnginePacket` for certification runtime intake only, and an ignored
caller `debug` field. The response shape is `CommentaryResponse`: status, public-safe
`CommentaryRender`, `noCommentary`, and optional debug/internal metadata.
Malformed FEN or invalid transition input fails closed to `invalidRequest` plus
a silent `RenderStatus.NoCommentary` render. Public responses hide blocked and
suppression metadata by default; internal diagnostics are exposed only through
the server-owned debug entrypoint, not by caller request flags. The public route
calls only `CommentaryPublicJsonTransport.renderJson`, which parses through
`CommentaryRequest` and then calls `CommentaryBackendSeam.render`;
`renderDebug` remains server-owned diagnostic code and is not routed. The
transport guard normalizes request field names by lowercasing and removing
separators, then rejects completed-probe, probe/cache, candidate-line,
branch/parent id or prefix, source row, caller-supplied/internal proof
id/proves, and raw internal-shaped JSON before request decoding, so unknown
future probe-shaped transport cannot be silently interpreted. Valid typed
`RuntimeEnginePacket` fields, including `engineConfigFingerprint` and
`pvLines`, remain accepted only as certification runtime intake. Renderer-owned
public `RenderVariationEvidence.proofId` remains allowed in responses as a
stable line-proof id; request proof fields and the lower `proves` token remain
non-public. The seam
composes exact-board intake, optional certification runtime intake,
`EvidenceClaimProducer`, selection, outline builder, and renderer. The
producer composes the narrow `ExactBoardClaimProducer` with typed
certification, projection-admission, and source-context handoffs. It may emit
only bounded Object claims for current material/piece-inventory, immediate
check, exact mate-in-one, support-only tactical-liability root facts
(`loose_piece`, `pinned_piece`, `overloaded_piece`, `trapped_piece`, and
`xray_target`), and the separate current-board `immediate_capture` contract
when a legal side-to-move capture lands on a locally loose piece. It may also
emit bounded Delta claims for last-move, capture, pawn-transition,
`moved_piece_left_loose_transition`, and legal non-slider royal fork facts from
a validated transition. The moved-piece-left-loose Delta claim must carry its
`loose_piece` and `immediate_capture` facts as lower carriers rather than
renaming the root fact as the public claim owner.
Those Delta claims require the transition after-state to match the current
extraction root state, and the backend seam rejects board-equivalent but
full-FEN-clock-stale transition endpoints before claim production.
Projection claims require typed admitted `StrategyProjectionAdmissionResult`
input with `DescriptorCertifiedRuntime` authority; Sxx labels, broad chess
concepts, claim-shaped projection candidates, and legacy validation-scaffold
admissions do not create claims. It does not create engine, prepared-variation,
candidate-line, best, forced, result, theory, recommendation, or oracle claims.
The seam does
not expose raw engine, pass raw engine packets or raw played-move strings to
claim production, serialize raw engine rejection details, accept
`EngineEvidencePacket` directly, accept raw source rows as truth, merge opening
source vectors, upgrade wording strength, or require frontend
reranking/reinterpretation. Accepted engine intake is still not a truth owner:
`EngineCertification` refs survive this seam only when they match bounded
engine evidence refs produced by the accepted certification runtime intake
result by canonical id, owner, anchor, route, scope, and required purpose set.
Accepted engine intake may, however, provide the same-root certification
evidence bundle that lets existing certification families such as
`MaterialHarvest` become ordinary bounded Certification claims through the
standard producer path. This does not bypass certification verdicts, public
family allowlists, selection board-reason rules, or renderer evidence filters.

The minimal frontend bridge contract is frozen in
[CommentaryFrontendBridgeContract.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryFrontendBridgeContract.md).
`ui/analyse/src/chesstory/commentaryBridge.ts` is the adapter bridge for
analyse consumption of backend `CommentaryResponse` / `CommentaryRender`
payloads. `ui/analyse/src/chesstory/moveExplanation.ts` and
`ui/analyse/src/chesstory/moveExplanationView.ts` are the first product
consumers, and remain display-only. They do not create a frontend rewrite,
source live integration, local chess meaning, engine execution, completed-probe
transport, or generated prose. The bridge can build the stable backend request
fields, decode public `CommentaryRender` fields, and discard stale
node/ply/FEN responses. The move explanation controller adds only request
sequencing so late responses cannot overwrite newer node state. The bridge also
preserves backend-prepared public
`RenderVariationEvidence` when present by copying only the renderer-owned
public line-proof fields and keeping structured blocks that have public
evidence ids or variation evidence ids even before prose text exists. The
bridge type accepts only renderer-public line roles (`resource`, `caution`,
`hold`, `conversion`, `pressure`, `simplification`) and does not copy the
prepared `proves` token, start FEN, UCI PV, provenance refs, replay/freshness
boundary fields, depth, MultiPV, engine fingerprints, cache keys, branch ids,
or raw move UCI. Raw candidate-line/probe/cache/debug fields remain ignored.
It must not rank, admit, revive suppressed material, promote source context,
render raw engine values, merge opening source vectors, send debug/internal
toggles, invent fallback text, or upgrade wording strength.
`noCommentary`, `hidden`, `negative_only`, `invalidRequest`, and stale-node
states remain silent public output. The move explanation view may show only
backend-owned `RenderText.publicText` that survived phrase-capability
validation plus public SAN line notation from decoded public variation
evidence tied to the displayed block and authorized by the same block phrase
capability; it must not display fallback captions, role-label prose, proof ids,
claim ids, internal ids, boundaries, UCI-only raw PV, depth, eval, engine
labels, cache/probe fields, or debug/internal metadata.
Selection global closure is now executable for the full `S01-S25`
validation-scaffold set owned by `StrategyProjectionScopeContract`. Each band has a
selection outcome when the claim is already admitted as a typed Projection with
exact owner, anchor, route, scope, row-specific lower-carrier role shape, and
its frozen allowed projection evidence kind. This closure is selection-only: it
does not add a new projection admission law, does not make support rows truth
owners, and does not weaken the cluster-specific rival/suppression tests for
S24, S13/S14/S15, S17/S18/S19/S22, king-attack, Engine E, or source context.
Selection derives S07/S08 and S15/S16 `rival_band` suppression itself rather
than relying on preloaded suppression hints, and opening context rejects
best-move, theory-truth, current-position-proof, forced-line, or result-style
source refs before they can enter selected context evidence.
The global selection closure keeps S06 tied to same-route
`SpaceBindRestrictionCertification`, and S16 tied to route-specific
`FortressDrawCertification`, `PerpetualCheckHolding`, or `PromotionRace`
support, matching the current projection authority.

## Count Freeze

The planning discussion started from the shorthand `24 / 61 / 3016+35`.

This branch now freezes the low-layer count as:

- `R0-R3 root atoms = 2856`
- `Aux state atoms = 35`
- `root-state vector = 2891`
- `R4` does not survive as a root tier
- `break_square` does not survive as a root atom

The original `3016` count remains useful only as the historical proposal that
still included `R4`.

The current branch decision is:

- `R0-R3 + Aux` are the root-state layer
- `R4` is dissolved upward into witness derivation
- `break_square` is dissolved upward into witness-level break-point payload
- the descriptor inventory stays fixed at `61`

See [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
and [DecisionFreezeLedger.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DecisionFreezeLedger.md)
and [Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md)
and [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md)
and [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md)
and [StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md)
and [BlockedUPrimaryDiscriminatorInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/BlockedUPrimaryDiscriminatorInventory.md)
and [RootIndexFreeze.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootIndexFreeze.md)
for the frozen low-layer contract.

Past failure lessons and the current validation charter are fixed in:

- [LegacyFailureTaxonomy.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/LegacyFailureTaxonomy.md)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md)

## Layer Definitions

### Layer 1: Root Truths

Root truths are exact-board predicates or direct low-level evidence.

They must not already claim strategy ownership.

This branch currently keeps only `R0-R3` plus auxiliary move-state in the root
layer.

Root truth is additionally constrained by these semantic rules:

- `controlled_by` is attacked-square, not legal-move, semantics
- square-oriented atoms use beneficiary polarity
- entity-oriented atoms use owner polarity
- `candidate_passer` uses the frozen three-file forward-cone
  support/opposition balance; friendly pawns ahead in the cone count as
  support, and same-rank adjacent friendly pawns also count for the first
  advance
- `loose_piece` is decided by local exchange loss, not raw attacker/defender
  counts
- `pinned_piece` includes relative slider pins to a more valuable friendly
  anchor, not only king pins
- `xray_target` is a high-value rook/queen target behind exactly one blocker,
  not a generic one-blocker line to any non-pawn piece
- `trapped_piece` is an extreme high-precision non-pawn atom with zero safe
  exits under the local safety rule

Root breadth status is narrower than this semantic freeze.

Current root `broad-confidence-green` set:

- `piece_on`
- `controlled_by`
- `pawn_controlled_by`
- `contested`
- `open_file`
- `half_open_file`
- `king_ring_square`
- `weak_square`
- `isolated_pawn`
- `backward_pawn`
- `doubled_file`
- `candidate_passer`
- `fixed_pawn`
- `en_passant_state`
- `lever_available`
- `loose_piece`
- `overloaded_piece`
- `outpost_square`
- `pinned_piece`
- `passed_pawn`
- `trapped_piece`
- `xray_target`
- `king_shelter_hole`
- `side_to_move`
- `castling_rights`

A future root `broad-confidence-green` claim is allowed only when:

- the schema-local breadth and floor ledger in
  `RootCoverageMatrix.scala` is closed and its tracked markdown snapshot
  remains synchronized
- the exact meaning remains the one frozen in
  [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
- engine, when present at all, is still only a confound filter for the
  explicitly frozen high-risk root schemas
  - for engine-required schemas, any selected confound-probe buckets used by
    the green claim must also be frozen in `RootCoverageMatrix.scala`

Projection and renderer stay out of scope for this root breadth state.

### Layer 2: Typed Witnesses

The branch keeps a fixed `61`-descriptor inventory above root truths.

The canonical primary owner-layer split is now:

- `Witness / U-primary`
  - active deterministic witness instances
- `Witness / U-attached`
  - deterministic descriptors that require a host
- `Object`
  - stable strategic owners built from witnesses
- `Delta`
  - move-local or scope-aware change owners built from objects
- `Certification`
  - comparative, persistence, denial, and conversion verdict owners
- `Projection`
  - human strategy vocabulary only

`upper-layer` survives only as a historical umbrella phrase for older notes.

Renderer owns zero descriptor-inventory rows.

#### Witness Boundary Rules

The historical inventory label is not automatically the runtime contract id.

If a legacy inventory label carries comparative or verdict drift, the runtime
contract may use a narrower witness id while the `61` table keeps the legacy
label for continuity.

The same narrowing rule also applies when a legacy `U-primary` row mixes local
witness meaning with upper-layer wording or with multiple anchor shapes.

If a legacy row collapses to a raw root alias with no distinct witness
discriminator, it leaves `U` rather than surviving as a degenerate runtime id.

An attached descriptor does not inherit host polarity by default.

If the host is only a neutral scope provider, beneficiary polarity must be
derived by an explicit root-level rule inside the host-projected scope.

An attached descriptor must not absorb parallel `U-primary` witnesses into its
payload.

Cross-witness composition begins only above `U`.

U broad validation is a test-side confidence state, not a runtime expansion.

Current U broad scope is limited to:

- active `U-primary 18`
- active `U-attached 1`
  - `structural_space_claim`

The owner for U broad buckets, floors, status, and R dependency gates
is `UBroadCoverageMatrix.scala`, mirrored by `u-coverage-matrix.md`.

Current U `broad-confidence-green` descriptors:

- `available_lever_trigger`
- `bishop_pair_state`
- `diagonal_lane_only`
- `duty_bound_defender`
- `file_lane_state`
- `fork`
- `knight_on_outpost_square`
- `loose_piece_target_state`
- `overload`
- `pawn_push_break_contact_source`
- `passed_pawn_entity_state`
- `pin`
- `rook_on_open_file_state`
- `sector_asymmetry_state`
- `short_run_slider_gate_restriction`
- `skewer`
- `structural_space_claim`
- `weak_outpost_square_state`
- `weak_pawn_target_state`

The formal `witness-expectations.jsonl` corpus has `218` descriptor-local broad
rows. The R dependency gates are closed by the Root 25/25
`broad-confidence-green` result, and no live U descriptor remains `thin` or
root-blocked under the current descriptor-local broad corpus.
The U corpus gate is descriptor-local: formal rows must use active runtime
`descriptorId` values and match the per-descriptor counts in the U broad matrix,
with frozen `coverageAxis` / `coverageBucket` breadth tags; aggregate row count
alone cannot promote a descriptor.

Engine/probe evidence must not participate in U witness derivation. If a U row
appears tactically confounded, the row is rejected or moved to a root/object/
certification validation channel; U truth remains `phi(R)` only.

Current code freeze:

- active attached runtime ids are now frozen to `structural_space_claim` only
- `material gain`, `structural damage`, `center`, `kingside`, `queenside`,
  `whole-board`, `closed center`, `fixed chain`, `open line`, and
  `create passer` are code-frozen shell-only rows
- `closed center` and `fixed chain` remain host vocabulary only; they are not
  standalone attached extractors

Current recorded examples:

- inventory label `opening-tempo`
- the row leaves `U`
- continuity meaning lives in `OpeningDevelopmentRegime`
- `phase_gate` is an upper release guard only and is not an admission source
- exact phase-witness admission does not survive as a runtime contract
- inventory label `middlegame-positional`
- the row leaves `U`
- continuity meaning lives in `DistributedContactRegime`
- `contested_sectors` remains payload geometry only and never became a
  standalone lower witness
- phase/posture inflation and unproven axis-independence remain forbidden
- inventory label `transition-liquidation`
- the row leaves `U`
- continuity meaning lives in delta-layer `TradeCompressionCorridor`
- `TransitionBridge`, `MoveLocal`, `PlanRace`, `InitiativeWindow`,
  `ConversionFunnel`, and `PasserComplex` stay outside raw witness admission
- `TradeCompressionCorridor` is frozen to a `move_local` board-anchored delta
  only
- the first live slice must stay on:
  - `reciprocal_exchange_corridor`
  - `compressed_trade_window`
  - `trade_compression_transition`
  - the first live slice requires:
    - a board-coherent non-king capture on the played move
  - no queens on the after-board
  - at most `4` total non-king non-pawn pieces on the after-board
  - one canonical opposing non-king pair that currently attacks each other
    along one shared file or diagonal corridor on the after-board
  - the before-board failed either the corridor predicate or the compressed
    window
  - forbidden-rival rejection must track the actual current-worktree
    `TradeInvariant` first slice rather than bare board-level
    `EndgameRaceScaffold` persistence
- generic liquidation, quiet corridor alignment, and broad transition-storyline
  wording remain negative only
- generic liquidation only and phase/posture inflation across cells remain
  forbidden
- inventory label `endgame-race`
- the row leaves `U`
- continuity meaning lives in `EndgameRaceScaffold`
- `low-material regime` stays contextual only and is not a conversion claim
- `PlanRace`, `PasserComplex`, `ConversionFunnel`, and `promotion race` remain
  above `U`
- phase/posture inflation across cells remains forbidden
- inventory label `space gain`
- runtime contract id `structural_space_claim`
- anchor `sector`
- allowed hosts `closed center` and `fixed chain`
- disallowed hosts `majority/minority asymmetry` and `restriction geometry`
- the present claim is a beneficiary-controlled connected square set attached
  to a host-supplied structural frontier
- runtime closure is now fixed in code:
  - `closed center` host requires a connected fixed central frontier that
    spans both `d` and `e` files and contains fixed pawns from both colors
  - `fixed chain` host requires a same-color rear-supported fixed-pawn segment
    of length `>= 2` inside one sector
  - when several same-owner fixed-chain segments survive in one sector, runtime
    keeps the strongest segment that yields a live claim
  - `fixed chain` host is emitted per host owner color and never as a mixed
    white/black boundary
  - `frontier_seed` remains `controlled_by(beneficiary, forward(host_boundary))`
  - `claimed_square_set` is one deterministic strongest connected component of
    empty beneficiary-controlled sector squares attached to those frontier
    seeds; occupied frontier squares are not claim squares
  - presence still requires `|claimed_square_set| >= 2`
- inventory label `open/semi-open file`
- runtime contract id `file_lane_state`
- primary anchor `file`
- variants are `open_file_state` and `semi_open_file_state`
- local meaning is pure structural file-state substrate only, not pressure,
  penetration, access, or utility
- `open_for_color` exists only on the `semi_open_file_state` payload and does
  not create a witness polarity
- inventory label `center`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- exact center meaning stays in narrower structural or helper rows; the label
  must not be widened into `open center`, `closed center`, `central tension`,
  `SpaceClamp`, `FixedTargetComplex`, or `TensionState`
- inventory label `kingside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- the label must not leak into `king attack`, `king shelter`, or other
  kingside-specific upper families
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  kingside-specific upper family is overfit
- inventory label `queenside`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- the label must not leak into castling-provenance, `wing_asymmetry_state`, or
  `king attack`
- any assignment to `SpaceClamp`, `FixedTargetComplex`, `TensionState`, or a
  queenside-specific upper family is overfit
- inventory label `whole-board`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a neutral `U-attached` theater shell only
- code freeze keeps the row outside attached runtime registration
- broad whole-board access-shadow aggregates are not canonical conversion state
- local scope may not widen into whole-board language without a later dedicated
  matrix
- inventory label `rook on open file`
- runtime contract id `rook_on_open_file_state`
- primary anchor `piece-square`
- local meaning is a rook placement on an open file only, not pressure,
  penetration, access, king-theater relevance, or initiative
- semi-open files stay outside this witness
- inventory label `bishop pair`
- runtime contract id `bishop_pair_state`
- primary anchor `board`
- local meaning is bishop-pair possession only, not bishop-pair advantage,
  color-complex dominance, mobility edge, attack strength, or king-safety
  implication
- admission is `>= 2` same-owner bishops; promoted extra bishops do not
  invalidate the witness
- inventory label `knight outpost`
- runtime contract id `knight_on_outpost_square`
- primary anchor `piece-square`
- local meaning is owner-side knight occupancy on an already certified
  `outpost_square`, not generic good-knight value, clamp, pressure, or
  superiority
- the square-side target meaning remains in `weak_outpost_square_state`; this
  row is occupancy/configuration only
- inventory label `weak pawn`
- runtime contract id `weak_pawn_target_state`
- primary anchor `piece-square`
- local meaning is a beneficiary-facing local weak-pawn target class only, not
  fixation, attack plan, conversion, or broader pawn-quality meaning
- `defender` is the owner of the pawn on the anchor square and `beneficiary`
  is derived as `not defender`
- admission is `piece_on(defender, pawn, square)` plus at least one of:
  - `fixed_pawn(defender, square)`
  - `backward_pawn(defender, square)`
  - `isolated_pawn(defender, square)`
- this row does not collapse into `passed pawn`, `candidate_passer`, or
  `weak square/outpost`
- inventory label `passed pawn`
- runtime contract id `passed_pawn_entity_state`
- primary anchor `piece-square`
- local meaning is an owner-side exact-board passed-pawn entity only, not
  promotion-readiness, conversion, route, or broader passer-play meaning
- `owner` is the owner of the pawn on the anchor square
- admission is `piece_on(owner, pawn, square)` plus `passed_pawn(owner, square)`
- this row does not collapse into `candidate_passer`, `promotion/passer`, or
  broader passer-play above `U`
- inventory label `promotion/passer`
- there is no admitted `U` runtime contract for this label on the current
  branch
- exact lower support remains in `passed_pawn_entity_state` and root
  `candidate_passer(c, s)`
- broader passer conversion or promotion meaning belongs above `U`, inside
  `PasserComplex`, `ConversionFunnel`, or `promotion race`
- any surviving support/render use is review-only and must not be presented as
  admitted runtime ownership
- inventory label `structural damage`
- there is no active runtime contract for this label on the current branch
- the row remains a `U-attached` host-scoped objective shell only
- code freeze keeps the row outside attached runtime registration
- exact lower support remains in structural-cause roots such as:
  - `isolated_pawn(c, s)`
  - `backward_pawn(c, s)`
  - `fixed_pawn(c, s)`
  - `doubled_file(c, f)`
- related lower witness examples may include `weak_pawn_target_state`
- all support examples are illustrative only and must not become a required
  bundle
- any broad structural-damage verdict must not be presented as a lower-layer
  truth claim
- inventory label `material gain`
- there is no active runtime contract for this label on the current branch
- the row remains a `U-attached` host-scoped objective shell only
- code freeze keeps the row outside attached runtime registration
- it is surface/projection vocabulary only, not a truth-owning material-swing
  predicate
- lower examples, if named, are illustrative consequence motifs only
- such examples are non-exhaustive, non-conjunctive, and not polarity proof
- realized conversion/result meaning belongs above `U` in `material harvest`
  or `winning endgame`
- inventory label `draw/hold`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy inventory wording at the descriptor surface; it is not
  itself an exact-board witness, host owner, standalone object runtime id,
  transformation row, or result verdict
- the descriptor row's canonical semantic home on this branch is object layer
  `FortressHoldingShell`
- bounded favorable simplification stays on the distinct `TradeInvariant` lane
- `neutralization / consolidation / fortress holding` remains projection-band
  vocabulary above that object home
- `simplify`, `perpetual/fortress`, and `winning endgame` remain related but
  distinct rows and must not collapse into `draw/hold`
- inventory label `king attack`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy inventory wording at the descriptor surface; it is not
  itself an exact-board witness, host shell, standalone object runtime id, or
  king-safety verdict
- the descriptor row's canonical semantic home on this branch is object layer
  `AttackScaffold`
- `direct piece concentration king attack` and `color-complex king attack`
  remain projection bands above that object home
- king-theater-linked file/diagonal access geometry remains a documentation
  shorthand only, while `file_lane_state`, `diagonal_lane_only`, and
  `rook_on_open_file_state` remain the live lower carriers
- `king safety edge`, `initiative`, and `mate net` remain related but distinct
  upper rows and must not collapse into `king attack`
- inventory label `simplify`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy surface transformation wording only, not an exact-board
  witness, transformation owner, object family, or result verdict
- certified simplification-side semantic ownership stays in `TradeInvariant`
- bounded favorable simplification remains a same-task move-local slice only
- `TradeInvariant` is frozen to a `move_local` board-anchored delta only
- the first live slice must stay on:
  - `bounded_material_reduction`
  - `persistent_object_carrier`
  - `trade_invariant_transition`
  - the first live slice requires:
    - a board-coherent non-king capture on the played move
  - total non-king non-pawn material count drops by exactly `1`
  - one same-family same-anchor object persists from before-board to
    after-board
  - the mover-side clear-run carrier must stay continuous across the move:
    - either the same clear runner remains on the same square
    - or the moving pawn itself remains the clear runner on its destination
- the current-worktree first live slice admits only
  `EndgameRaceScaffold` persistence on the `board` anchor with mover-side
  clear-run carrier continuity
- `FortressHoldingShell`, `AttackScaffold`, and `KingSafetyShell`
  generalization remain deferred until they carry separate delta corpus rows
- generic favorable exchange, task-switch creation, and upper result wording
  remain negative only
- `favorable simplification` is projection-band vocabulary only
- `draw/hold`, `winning endgame`, `perpetual/fortress`, `material gain`, and
  `promotion/passer` remain related but distinct rows and must not collapse into
  `simplify`
- inventory label `open line`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- code freeze keeps the row outside attached runtime registration
- attachment mode is `host-scoped` and polarity remains `host`
- exact lower line geometry stays on `open_file_state`,
  `semi_open_file_state`, `rook_on_open_file_state`, and
  `diagonal_lane_only`
- the label must not be treated as a truth-owning lane, access, pressure,
  attack, or simplification witness
- the shell must not invent explicit certified hosts or a runtime witness id
- inventory label `create passer`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the row remains a `U-attached` host-scoped transformation shell only
- code freeze keeps the row outside attached runtime registration
- attachment mode is `host-scoped` and polarity remains `host`
- exact lower support stays in `candidate_passer(c, s)`
- `passed_pawn_entity_state` is downstream entity truth, not lower truth for
  this row
- `promotion/passer` remains only a neighboring upper-layer legacy projection
  row
- broader passer conversion meaning remains above `U` inside `PasserComplex`,
  `ConversionFunnel`, or `promotion race`
- the label must not be treated as a truth-owning support, entity,
  conversion, or result witness
- inventory label `exchange defender`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy surface transformation wording only, not an exact-board
  witness, transformation owner, tactic owner, object family, or result shell
- `exchange square` and `defended resource` remain related primitive seeds only
- `TradeInvariant` and `DefenderDependencyNetwork` remain related but distinct
  upper families and must not be collapsed into one canonical owner for this
  label
- `duty_bound_defender` and `overload` remain related but distinct lower rows
- `deflection/decoy` and `demolition/undermining` remain related but distinct
  upper-layer tactical labels
- `material gain` and `simplify` remain related but distinct upper rows and
  must not collapse into `exchange defender`
- inventory label `weak square/outpost`
- runtime contract id `weak_outpost_square_state`
- primary anchor `square`
- variants are `outpost_square_state` and residual `weak_square_state`
- `outpost_square_state` has priority over `weak_square_state` so a stronger
  outpost square is not double-counted as a separate weak-square witness
- this row is beneficiary-side square target state only, not owner occupancy,
  strategic value, pressure, or king-theater meaning
- inventory label `loose/overloaded piece`
- runtime contract id `loose_piece_target_state`
- primary anchor `piece-square`
- local meaning is beneficiary-facing loose-piece target only, not overload,
  defender-duty, pin/trap coupling, pressure, or initiative
- the admitted root is `loose_piece(defender, square)` with beneficiary derived
  as `not defender`
- `overloaded_piece` does not survive inside this row; overload meaning stays
  outside it
- inventory label `bad piece`
- there is no active `U` runtime contract for this label on the current branch
- exact-board liability slices stay in narrower witnesses such as
  `loose_piece_target_state`, `duty_bound_defender`, and
  `weak_outpost_square_state`
- the lower support list is illustrative only; it is not exhaustive and does
  not define a required bundle
- live `S17` lower support seeds are owned by `StrategySupportSeedInventory.md`;
  exact band validation-scaffold status is owned by
  `StrategyProjectionBoundaryMatrix.md`
- broad piece-quality meaning belongs above `U`, inside `improve_worst_piece`,
  `minor_piece_liability`, or `favorable_minor_piece_relation` support
- inventory label `improve worst piece`
- there is no admitted `U` runtime contract for this label on the current
  branch
- the label is legacy piece-quality projection wording only, not a witness or
  transformation shell
- exact liabilities remain below `U` in narrower witnesses such as
  `loose_piece_target_state`, `weak_outpost_square_state`, and
  `duty_bound_defender`
- support-only exact failure material may also include `pinned_piece` and
  `trapped_piece`
- live `S17` lower support seeds are owned by `StrategySupportSeedInventory.md`;
  exact band validation-scaffold status is owned by
  `StrategyProjectionBoundaryMatrix.md`
- broad improvement meaning remains above `U`, alongside `improve_worst_piece`
  and `minor_piece_liability`
- `favorable_minor_piece_relation(c)` remains broader upstream support, not a
  lower witness for this label
- inventory label `available lever`
- runtime contract id `available_lever_trigger`
- primary anchor `piece-square`
- local meaning is pure one-move pawn trigger only, not strategic success,
  readiness, counterplay, or break-point meaning
- active variants are `single_push_lever_state` and `double_push_lever_state`
- the variant split keeps the witness on immediate push-contact geometry rather
  than letting it drift into `pawn_push_break_contact_source`
- inventory label `majority/minority asymmetry`
- runtime contract id `sector_asymmetry_state`
- primary anchor `sector`
- local meaning is count-only sector pawn imbalance with side information in
  payload
- minority-attack, majority-play, favorable-imbalance, and route/conversion
  meaning stay above `U`
- inventory label `opposite-side castling/wing asymmetry`
- there is no active `U` runtime contract for this label on the current branch
- lower support examples such as `castling_rights`, `king_shelter_hole`,
  king-theater-linked file/diagonal access geometry, and `central tension` remain
  illustrative only, not a required bundle
- mixed castling-provenance and wing-asymmetry meaning stays above `U`; attack
  race, pawn-storm, initiative, and king-safety readings do not survive as raw
  witnesses
- inventory label `open center`
- there is no active `U` runtime contract for this label on the current branch
- the row is projection-only above `U`
- lower support examples such as `file_lane_state` on central files,
  `fixed_pawn`, `central tension`, and `available_lever_trigger` remain
  illustrative only, not a required bundle
- broad central-openness meaning stays above `U`; central break, initiative,
  and king-theater projection must not be restated as raw witness admission
- inventory label `closed center`
- the reviewed narrowing proposal `closed_center_barrier_state` is rejected on
  the current branch
- the proposed mixed-color `8`-connected barrier topology was too
  motif-specific and under-fired on ordinary closed-center positions
- the row no longer remains a `U-primary` placeholder
- it survives only as neutral host vocabulary for `structural_space_claim`
- broader clamp, break-denial, initiative, and king-safety readings must stay
  outside this row
- inventory label `fixed chain`
- the reviewed narrowing proposal `fixed_chain_state` is rejected on the
  current branch
- the proposed connected-component chain topology was too loose for a true
  chess chain and too rich relative to current roots
- the current branch does not certify a cleaner exact-board chain slice as an
  active runtime contract
- the row no longer remains a `U-primary` placeholder
- `structural_space_claim` remains the surviving `U-attached` host contract,
  and `fixed chain` stays allowed host vocabulary inside it
- broader clamp, break-denial, initiative, king-safety, and pawn-plan readings
  must stay outside this row
- inventory label `central tension`
- the reviewed narrowing proposal `central_pawn_contact_state` is rejected on
  the current branch
- the proposed mutual pawn-contact admission was too pawn-specific and
  under-fired on broader central contact states
- the row no longer remains a `U-primary` placeholder
- continuity meaning now lives in object-side `CentralContactFront`
- unlike `closed center` and `fixed chain`, it is not host vocabulary for
  `structural_space_claim`
- openness, closure, break, space, initiative, king-safety, and `TensionState`
  readings must stay outside this row
- inventory label `key file/rank`
- there is no active `U` runtime contract for this label on the current branch
- the legacy mixed-anchor row does not survive because it bundles:
  - `file`
  - horizontal `ray`
- file-side substrate is absorbed by:
  - `file_lane_state`
  - `open_file_state`
  - `semi_open_file_state`
- broad horizontal-rank meaning remains outside `U`; the current live
  first-slice support for projection is only `rank_corridor_state_seed` under
  `S25`, not a `U` witness
- `key` usefulness wording belongs above `U`, not inside a raw witness id
- inventory label `diagonal/color complex`
- runtime contract id `diagonal_lane_only`
- primary anchor `ray`
  - `source square + one diagonal direction`
- local meaning is one exact diagonal projection only, not color-complex
  weakness, bishop quality, king access, or carrier configuration
- `color_complex_only` is deferred outside `U` on the current branch
- inventory label `king shelter`
- there is no active `U` runtime contract for this label on the current branch
- exact local fact stays in root `king_shelter_hole`
- broad shelter-shell meaning belongs above `U`, inside `KingSafetyShell` /
  upper-layer king-safety composition
- `king_shelter_hole_target` is not a valid active witness id on this branch
- inventory label `rook lift`
- there is no active `U` runtime contract for this label on the current branch
- the reviewed `lift_corridor_seed` proposal stayed too thin and is deferred
  to primitive/support territory rather than admitted as a witness
- broad lift meaning belongs above `U`, inside route/access/attack composition
  such as `RedeploymentRoute`, `AccessNetwork`, or `AttackScaffold`
- current `S25` support does not revive move-history `rook lift`; it admits
  only legal current-board cross-wing rank switching through
  `rank_corridor_state_seed`
- inventory label `queen-bishop battery`
- there is no active `U` runtime contract for this label on the current branch
- the reviewed `queen_bishop_battery_ray` proposal still overclaimed `battery`
  meaning and collapsed into geometry-only ordered alignment
- the surviving exact-board relation is deferred as `qb_diagonal_alignment_seed`
  in primitive/support territory, not as an admitted witness or root schema
- inventory label `domination net/restriction geometry`
- runtime contract id `short_run_slider_gate_restriction`
- primary anchor `piece-square`
- anchor target is an enemy `bishop`, `rook`, or `queen`
- divisor uses `testable_directions`, not all on-board directions
- local meaning is short-run gate throttling only, not domination or
  no-counterplay
- inventory label `counterplay source/break-point`
- runtime contract id `pawn_push_break_contact_source`
- primary anchor `piece-square`
- anchor target is an owner `pawn`
- contact is push-only; capture-arrival and en-passant are excluded
- break-point remains payload only and is not a witness id
- local meaning is strategic pawn-contact source only, not counterplay
  readiness or initiative
- inventory label `pin`
- runtime contract id `pin`
- primary anchor `ray`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board pin witness only
- attacker slider, pinned blocker, and hidden anchor must lie on one exact ray
- the blocker is the sole blocker on that ray; moving it concedes the line to
  the king or to a more valuable friendly anchor
- `pinned_piece` is required root support, but `pin` is not identical to
  `pinned_piece` alone
- `duty_bound_defender`, `short_run_slider_gate_restriction`, `xray_target`,
  and `skewer` remain distinct
- inventory label `fork`
- runtime contract id `fork`
- primary anchor `piece-square`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board fork witness only
- one beneficiary piece on the anchor square attacks two or more distinct
  enemy occupied squares from the current board
- the fork payload is an aligned list of enemy-occupied `target_squares` and
  `target_roles`, all pairwise distinct and all attacked by the anchored piece
- `overload`, `duty_bound_defender`, `deflection/decoy`, `pin`, `skewer`, and
  `xray` remain distinct
- value/support/undefended filters remain detector heuristics, not admitted
  witness law
- inventory label `skewer`
- runtime contract id `skewer`
- primary anchor `ray`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board skewer witness only
- attacker slider must be a `bishop`, `rook`, or `queen`
- attacker slider, front target, and rear target must lie on one exact ray
- the front target is the sole blocker between attacker and rear target
- the front target is a non-king piece that is strictly more valuable than the
  rear target
- moving the front target off that ray exposes the rear target to the same ray
  attack
- `pin`, `pinned_piece`, `duty_bound_defender`, `xray_target`,
  `short_run_slider_gate_restriction`, and `restriction geometry` remain
  distinct
- inventory label `overload`
- runtime contract id `overload`
- primary anchor `piece-square`
- polarity `beneficiary`
- local meaning is beneficiary-side exact-board overloaded-defender witness
  only
- one anchored enemy non-king defender currently covers two or more distinct
  same-color non-king occupied squares from its present square
- removing the anchor causes every listed duty square to become numerically
  under-defended against the opposing side
- `MoveAnalyzer` / `Motif.Overloading` heuristic handling remains negative
  boundary only, not admitted witness law
- `duty_bound_defender`, `loose_piece_target_state`, `fork`, and
  `deflection/decoy` remain distinct
- inventory label `deflection/decoy`
- there is no active `U` runtime contract for this row
- `Motif.Deflection`, `Motif.Decoy`, and `Motif.RemovingTheDefender` remain
  heuristic siblings only, not admitted lower facts
- the merged label collapses attacked-defender `from-square`,
  lure-destination `to-square`, and capture-removal route logic, so it stays
  `upper-layer`
- `overload`, `duty_bound_defender`, and `fork` remain distinct
- inventory label `interference`
- there is no active `U` runtime contract for this row
- `Motif.Interference` remains a heuristic sibling only, not an admitted lower
  fact
- `Witnesses61` still carries a legacy `U-primary` row, but no
  `RootAtoms`/`RootAtomRegistry`/`RootExtractor`/corpus admission path survives
- `overload` and `fork` remain distinct lower rows
- `clearance`, `deflection/decoy`, and `demolition/undermining` remain
  distinct neighboring tactical rows
- inventory label `clearance`
- there is no active `U` runtime contract for this row
- baseline-head `Motif.Clearance` remains reference-only, not an admitted lower
  fact on the current branch
- `Witnesses61` still carries a legacy `U-primary` row, but no
  `RootAtoms`/`RootAtomRegistry`/`RootExtractor`/corpus admission path survives
- `overload` and `fork` remain distinct lower rows
- `interference`, `deflection/decoy`, and `demolition/undermining` remain
  distinct neighboring tactical rows
- inventory label `demolition/undermining`
- there is no active `U` runtime contract for this row
- `Motif.RemovingTheDefender` remains a heuristic sibling only, not an
  admitted lower fact
- `overloads_or_undermines` remains relation/operator context only, not a
  lower exact family
- the broad removal/support-breaking label stays `upper-layer` because no exact
  current-branch admission path survives
- `overload`, `duty_bound_defender`, `fork`, and `deflection/decoy` remain
  distinct
- inventory label `defender shortage`
- runtime contract id `duty_bound_defender`
- primary anchor `piece-square`
- anchor target is a defender `knight`, `bishop`, `rook`, or `queen`
- local meaning is a physically bound load-bearing defender, not defender-count
  shortage or overload-style dependency
- absolute king pin cases use current attacked-square geometry duty, not legal
  move generation
- occupied-pressure duty keeps `xray_target` on beneficiary-side polarity to
  match the attacking-side root contract
- the previous documentation phrase for a king-theater entry axis does not
  survive as a live runtime id on this branch
- it is not a new `61` inventory label or an admitted host-scoped lower
  fragment below `king safety edge` and `initiative`
- any such access meaning is represented compositionally through
  `file_lane_state` / `diagonal_lane_only` under the existing
  `king_theater_link` gate
- admission core uses king-ring entry plus lane-compatible source and lane
  reach, not generic attacked-square pressure
- `king_shelter_hole` is a strengthener only, not an entry admission core
- horizontal rank access and rook-swing corridor access stay outside this
  fragment
- inventory label `king safety edge`
- there is no valid `U` runtime witness id for king-safety comparison
- upper-layer contracts split into `comparative_king_fragility` and
  `certified_king_safety_edge`
- lower fragments must be linked to the relevant king theater before they may
  participate in an upper-layer king-safety comparison
- `king attack` or attack-map pressure never self-certifies a king-safety edge
- phase, material, attack-host, and best-defense gates are required before a
  certified king-safety edge is released

### Layer 3: Strategic Objects

This is the first truth-owning commentary layer.

A strategic object is a stable strategic state unit on the board.

Objects are formed from witness material plus exact-board root support routed
through the shared strategic-object context.

### Object 7 Runtime Contract

The current branch now carries live runtime extraction for the seven `Object`
homes in `modules/commentary/src/main/scala/lila/commentary/strategic`.

The helper and admission laws below are now implemented and exact-board
verified. They are no longer scaffold-only design notes.

Shared helpers:

- `occupied(square)` means `exists piece_on(_, _, square)`
- `sector_mask(sector, square)` follows the canonical file split already
  implied by `WitnessSector`
- `contact_square(square)` means `contested(square)` or an occupied square
  directly attacked by the opponent of the occupant
- `front_connectivity(square_a, square_b)` means the two squares lie in the
  same maximal orthogonally connected component of `contact_square` inside the
  chosen mask
- `central_sector_mask(square)` is the extended center band on files `c-f` and
  ranks `3-6`
- `king_theater_link(fragment, defending_king)` remains the canonical
  king-theater gate reused unchanged by `AttackScaffold`
- `KingSafetyShell` reuses `home_shelter_mask` geometry but adds its own
  home-wing king proxy rather than sharing the full `king_theater_link`

Frozen object homes:

- `OpeningDevelopmentRegime`
  - helper: `opening_development_window`
  - present iff:
    - each side still keeps at least one home-rank bishop or knight on an
      original start square
    - at least one side still keeps at least two such minors
    - each side still keeps at least one home-rank rook on an original corner
    - neither the `d` file nor the `e` file is open
    - no live `CentralContactFront`, `DistributedContactRegime`, or
      `EndgameRaceScaffold` already owns the same board
  - forbids:
    - move-count or tempo narration
    - `phase_gate` admission
    - release-guard or king-safety wording

- `DistributedContactRegime`
  - helper: `distributed_contact_spread`
  - present iff:
    - both colors already have at least one non-pawn piece off the home rank
    - at least two distinct `sector_mask` sectors each contain a connected
      `contact_square` component under `front_connectivity` with at least two
      squares
    - every admitted sector component contains both a contested square and an
      occupied contact square
    - both colors contribute occupancy or current control to every admitted
      sector component
    - one admitted component lies outside the central-only contact band so the
      row does not collapse into `CentralContactFront`
  - forbids:
    - `contested_sectors` as admission proof
    - one-sector tactical shells narrated as regime continuity
    - axis-independence claims that are not board-proven

- `EndgameRaceScaffold`
  - helper: `dual_run_endgame_trigger`
  - `advanced_run_resource(color, square)` means an owner pawn already sits on
    or beyond the fifth rank relative to that color; `passed_pawn_entity_state`
    and root `candidate_passer` remain optional support when present
  - `forward_run_clear(color, square)` means the immediate next square on that
    pawn's advance file is empty on the current board
  - present iff:
    - no queens remain on the board
    - both colors have at least one `advanced_run_resource`
    - each color has at least one such resource with `forward_run_clear` on the
      current board
  - forbids:
    - low-material context alone
    - one-sided passer presence alone
    - directly blockaded runner geometry
    - direct collapse into `promotion_race`, `PasserComplex`, or
      `ConversionFunnel`

- `AttackScaffold`
  - helper: `attack_host_core`
  - present iff one attacking color has at least two distinct
    `king_theater_link` fragments aimed at the same defending king, where:
    - at least one fragment is a carrier from `rook_on_open_file_state` or a
      king-theater-linked `file_lane_state` / `diagonal_lane_only`
    - at least one fragment is a vulnerability/support fragment from
      `king_shelter_hole`, `duty_bound_defender`,
      `short_run_slider_gate_restriction`, `xray_target`, `pinned_piece`, or
      `loose_piece`
    - `loose_piece` is admission support only when an existing carrier source
      directly attacks the loose square; the admitted object payload preserves
      the exact `loose_support_squares` so projection consumers can bind the
      support to their public source/target rather than accepting scaffold-wide
      smell
    - `pinned_piece` is admission support only through a same-owner `pin`
      witness whose attacker square is a carrier source, whose blocker is the
      support square, and whose anchor is the defending king
    - `xray_target` remains support-only until a separate carrier-bound x-ray
      recomputation contract exists; a standing x-ray root in the same king
      theater is not `AttackScaffold` support
    - shelter-hole-only support still needs either a second distinct carrier
      admission unit or at least two carrier entry squares; lone local
      diagonal/file pressure plus holes stays outside the host core
    - pinned-plus-shelter without a direct loose/certified support fragment is
      not enough to create the host core
  - forbids:
    - attack-map pressure alone
    - carrier-only admission
    - self-certification into `certified_king_safety_edge`, `initiative`, or
      `mate net`

- `FortressHoldingShell`
  - helper: `fortress_entry_denial_shell`
  - `fortress_shell_mask(holder_king, square)` means the square lies on the
    holder king's file or an adjacent file, and on the king's home rank or the
    next two ranks toward the board center
  - present iff one holding side shows all of:
    - no queens remain on the board
    - the holder king remains on its home rank
    - at least two friendly occupied non-king squares lie in
      `fortress_shell_mask`
    - the attacker occupies no square in `fortress_shell_mask`
    - the attacker lacks a current file or diagonal entry axis into any square
      in `fortress_shell_mask`
    - no open or semi-open file on the holder king file or an adjacent file
      currently carries an attacker rook or queen into that shell theater by
      live attack geometry on a shell square
    - any attacker passed pawn on the holder king file or an adjacent shell
      file is already blockaded by immediate holder occupancy
  - forbids:
    - shell shape alone
    - `TradeInvariant` alone
    - `perpetual/fortress` certification alone

- `KingSafetyShell`
  - helper: `home_shelter_shell`
  - `home_shelter_mask(defending_king, square)` means the square lies on the
    defending king file or an adjacent file, and one or two ranks toward the
    board center from the home edge
  - present iff:
    - the defending king remains on its home rank on the current exact-board
      home-wing proxy file (`c` or `g`), so central or uncastled home-rank
      kings stay outside this shell object
    - at least two distinct `king_shelter_hole` squares for the same defender
      lie inside `home_shelter_mask`
    - at least one such pair is edge-adjacent inside that mask
  - forbids:
    - `king attack` wording alone
    - generic pressure away from the home shelter
    - direct collapse into `comparative_king_fragility` or
      `certified_king_safety_edge`

- `CentralContactFront`
  - helper: `central_contact_front_state`
  - present iff one connected `contact_square` component inside
    `central_sector_mask` contains at least two squares, contains both a
    contested square and an occupied contact square, and both colors contribute
    occupancy or current control to that same component
  - if multiple disconnected qualifying components exist, runtime keeps one
    canonical strongest component rather than merging disconnected fronts into
    one sector identity
  - forbids:
    - `open center` narration
    - `closed center` or `fixed chain` host vocabulary
    - initiative, king-safety, or `TensionState` wording

These contracts are now live in the current worktree.

Current-worktree evidence is carried by
`StrategicObject7RuleTest`,
`StrategicObjectCorpusRuntimeTest`,
and the public-boundary coverage in
`CommentaryCoreBoundaryTest`.

### Layer 4: Deltas

A delta is the typed change or scope statement about an object:

- what changed because of the move
- what matters in the current position
- what local or comparative scope now applies

`Delta` remains truth-owning.

Current-worktree `Delta 2` now has live runtime code in
`modules/commentary/src/main/scala/lila/commentary/delta`, with both delta
families registered together and `TradeCompressionCorridor` ordered before
`TradeInvariant`.

Frozen family ids:

- `TradeCompressionCorridor`
- `TradeInvariant`

Runtime boundary:

- current runtime package:
  `modules/commentary/src/main/scala/lila/commentary/delta`
- present files:
  - `TradeCompressionCorridorRule.scala`
  - `TradeInvariantRule.scala`
  - `StrategicDeltaModel.scala`
  - `StrategicDeltaContext.scala`
  - `StrategicDeltaScopeContract.scala`
  - `StrategicDeltaRuntime.scala`
  - `StrategicDeltaExtractor.scala`
- the public `CommentaryCore` facade exposes `activeDeltaFamilyIds`,
  `extractStrategicDeltas(...)` overloads from object extractions and from
  before/after `Fen` plus `playedMove`, and fail-closed delta extraction
  overloads
- delta extraction must consume exact before/after position truth rather than a
  single static board:
  - before `StrategicObjectExtraction`
  - after `StrategicObjectExtraction`
    - both supplied object carriers must remain canonical:
      - witness/object payloads must exactly match the live object extractor
        output for their root states
    - one exact-board `playedMove`
      - side-to-move, castling-rights, and en-passant auxiliary state must be
        rehydrated from the root-state vector rather than guessed from piece
        placement alone
      - legal castling and legal en-passant transitions are part of the live
        exact-board boundary
- `StrategicDeltaExtractor.scala` is live
- `StrategicDeltaRuntime.scala` registers both families together with corridor
  before invariant ordering
- `TradeCompressionCorridorRuleTest`, `TradeInvariantRuleTest`,
  `StrategicDeltaBoundaryTest`, `DeltaExpectationCorpusTest`, and
  `CommentaryCoreBoundaryTest` are live
- `DeltaExpectationCorpusTest` now asserts live runtime extraction against the
  delta corpus rows

### Layer 5: Certification

Certification is where the branch decides whether an object or delta survives as
actionable, comparative, denial-bearing, or conversion-bearing truth.

This is where best-defense, persistence, superiority, and route-survival burdens
are paid.

### Certification Verdict Lattice

Certification owns four verdict outcomes only:

- `Certified`
- `SupportOnly`
- `Deferred`
- `Rejected`

Meaning:

- `Certified`:
  - the row's exact-board burden is met at the currently frozen depth and reply
    standard
- `SupportOnly`:
  - the semantic idea remains real on the exact board, but the branch burden is
    too thin for release as a full verdict
- `Deferred`:
  - the row stays fail-closed because best-defense, comparative, or route
    survival is still reply-incomplete or depth-unstable
- `Rejected`:
  - the exact board fails the row's admission or negative boundary outright

Support and deferred are both real endpoints.

They are not planner hints and they are not projection seeds.

### Certification Runtime Boundary Design

Current-worktree status:

- certification is now live in `src/main` on one canonical package boundary:
  `modules/commentary/src/main/scala/lila/commentary/certification`
- `CommentaryCore` now exposes:
  - `activeCertificationFamilyIds`
  - typed `extractCertifications(...)`
  - typed `extractCertificationsFailClosed(...)`
- those public helpers require a current `StrategicObjectExtraction` or
  `StrategicDeltaExtraction` plus one explicit certification-side engine/probe
  evidence bundle
- any supplied current `StrategicObjectExtraction` must remain canonical:
  - witness/object payload must exactly match the live object extractor output
    for the current root state

The live certification runtime stays on one canonical package
boundary:

- `modules/commentary/src/main/scala/lila/commentary/certification`

The 61-row descriptor ownership ledger still counts the historical
certification-owned descriptor rows by row, but the current live certification
registry exposes `12 active certification families` through
`CertificationScopeContract.activeCertificationFamilyIds`:

- `DevelopmentComparison`
- `InitiativeWindow`
- `MobilityComparison`
- `ComparativeKingFragility`
- `CertifiedKingSafetyEdge`
- `MateNetCertification`
- `MaterialHarvest`
- `WinningEndgame`
- `FortressDrawCertification`
- `PerpetualCheckHolding`
- `PromotionRace`
- `SpaceBindRestrictionCertification`

Inventory mapping stays frozen as:

- `development lag` and `development lead` both map to
  `DevelopmentComparison`
- `king safety edge` remains one inventory row but splits into:
  - `ComparativeKingFragility`
  - `CertifiedKingSafetyEdge`
- `perpetual/fortress` remains one inventory row but splits into:
  - `FortressDrawCertification`
  - `PerpetualCheckHolding`

The live runtime extractor consumes only:

- current `StrategicObjectExtraction`
- current `StrategicDeltaExtraction`
- one explicit certification evidence bundle; `CertificationEvidenceBundle.empty`
  is the explicit unbound fail-closed sentinel, while any non-empty bundle
  created by `forObjectExtraction` or `forDeltaExtraction` must be bound to
  the same current root state
- any supplied `StrategicDeltaExtraction` must be canonical:
  - exact before/after/move validation must still pass
  - the carried `deltas` set must exactly match the canonical delta runtime
    result for that transition
- live certification extraction must reject any non-empty evidence bundle
  whose bound root state does not exactly match the current extraction
- live legal-move reconstruction inside certification must also rebuild the
  exact board from root auxiliary state:
  - side to move
  - castling rights
  - current en-passant availability
  This state must not be guessed from piece placement or board geometry alone.

The live runtime has a certification-only Engine E intake adapter:

- `CertificationEngineEvidence.fromProbe(...)` stays fail-closed empty until a
  raw-probe adapter lands
- `CertificationEngineRuntimeIntake` is the current analyse/backend packet
  intake model; it normalizes runtime packet fields into
  `EngineEvidencePacket` and then delegates to
  `CertificationEngineEvidenceContract`
- current certification Engine E admission does not consume UI/API raw probe
  sidecars; those sidecars may exist elsewhere in the product, but they are not
  certification evidence until normalized through the typed contract below

### Engine E certification evidence freeze

Engine E is certification evidence only. It is not a
`Root/U/Object/Delta/Sxx truth owner`, and it must not create, widen, or promote
root facts, U witnesses, strategic objects, deltas, projection bands, renderer
wording, or Sxx projection admission.

The frozen live admission boundary for typed E evidence is
`CertificationEngineEvidenceContract` in the certification package.
`CertificationEngineRuntimeIntake` may accept an optional analyse/backend
runtime packet, normalize it into an `EngineEvidencePacket`, and delegate to
that contract. It may produce a `CertificationEvidenceBundle` only after exact
FEN, node identity, freshness, depth, MultiPV, score, and legal-PV checks pass
for the same current extraction.
In short: exact FEN, node identity, freshness, depth, MultiPV, score, and legal-PV checks are the live intake gate.

Packet identity is exact-board:

- object-backed evidence binds to the requested exact normalized full-FEN
  string, including halfmove/fullmove clock fields, plus expected `nodeId` and
  `ply`; `StrategicObjectExtraction` remains root-state-bound, so the
  requested full-FEN is the clock-field authority for live intake
- delta-backed evidence must additionally bind `beforeFen`, `playedMove`, and
  `afterFen`; both endpoint FEN strings must match the requested normalized
  full-FEN strings, and the move and both endpoint boards must match the
  canonical `StrategicDeltaExtraction`
- transition-bound packets are rejected on the object path

Freshness and search state are fail-closed:

- incomplete search, stale `generatedAtEpochMs` beyond `maxAgeMs`, insufficient
  realized depth, insufficient `MultiPV`, or too-short PV lines are rejected
- best-defense and reply-branch claims require at least `MultiPV 3` with
  distinct first moves, plus a satisfied semantic coverage report
- each engine-role bounded certification claim must bind a server-shaped
  `q-...` request id to role, family, owner, anchor, node id, and ply; it must
  also carry the Q policy fingerprint derived from the server role policy and
  engine config fingerprint, not the engine config fingerprint alone
- each engine role in a bounded certification claim must have a satisfied role
  invariant report tied to that Q policy fingerprint
- comparative, conversion, and counterplay roles require a bound baseline;
  release, causality, and persistence roles require transition-bound evidence
- raw probe objects remain untrusted; `CertificationEngineEvidence.fromProbe`
  still returns `CertificationEvidenceBundle.empty`

Score normalization is typed:

- centipawn and mate scores are separate domains
- side-to-move scores are normalized through the bound FEN's side to move before
  applying owner-side burdens
- eval-swing and conversion burdens require typed thresholds; a mate score must
  not masquerade as centipawns
- scoreless bounded claims are rejected
- eval-swing claims require a bound, fresh, same-engine-config baseline packet
  whose FEN matches the transition `beforeFen`; an unbound baseline is not
  evidence
- eval-swing baselines must also bind node/ply: if a transition `beforeNode` is
  supplied it must match exactly, otherwise the baseline ply must immediately
  precede the transition packet ply
- inactive certification family ids, unknown purpose/strength keys, invalid
  owner colors, and invalid UCI PV moves are rejected during runtime intake
- public cap reports for `horizon_limited`, `multipv_ambiguous`,
  `tablebase_required`, `mate_dominated`, or `material_collapse` block
  certification evidence before selection or rendering
- mate scores can certify only mate-owned families; material-collapse scores can
  certify only material-owned families

PV legality is exact-board:

- every PV line must replay legally from the bound FEN
- illegal continuations and truncation below the claim's `minPvPlies` fail
  closed
- best-defense, eval-shift, conversion, or persistence claims may be admitted
  only when the bounded claim declares its purpose, has a role invariant report,
  and its typed score/PV/baseline/transition burden passes

It must not reopen raw root or witness admission from inside certification.

The live public facade stays fail-closed:

- do not add certification convenience helpers that fabricate missing
  certification evidence from raw FEN alone
- `CertificationEngineEvidence` is an opaque Engine E evidence facade and
  exposes no direct claim constructors; E-named evidence creation must go
  through `CertificationEngineEvidenceContract` or the fail-closed
  `fromProbe(...)` placeholder
- keep the public certification surface limited to typed object/delta extraction
  entry points with an explicit certification evidence bundle, where
  `CertificationEvidenceBundle.empty` is the explicit unbound fail-closed
  sentinel and any non-empty bundle must stay exact-position-bound
- missing, rejected, or insufficient runtime E packets leave the existing
  object/delta/certification path on the base evidence bundle; they do not
  fabricate a stronger certification
- the projection admission path consumes only certified lower carriers and an
  externally supplied same-root `CertificationEvidenceBundle`; projection does
  not consume Engine E directly
- no planner, projection, or renderer layer may revive `SupportOnly` or
  `Deferred` rows

### Certification Runtime Freeze And Projection Handoff

Certification families own certified lower facts. They do not own Sxx public
strategy claims.

The certification boundary stays narrow by freezing executable certification
slices and their declared support dependencies. When a certification slice
depends on a lower object, delta, or certification-side support family, that
dependency must be represented as `requiredSupportFamilies`; prose dependency
is not enough. Support-family presence is satisfied only by a live non-`Rejected`
claim of the required family for the same owner polarity.

Projection handoff follows descriptor/K law instead of row-local production
admission rules:

| Source family kind | Lower truth role | Projection role |
| --- | --- | --- |
| comparative certifications | owner-relative board fact | optional typed carrier for a descriptor |
| current-position certifications | exact-board verdict or burden | optional typed carrier for a descriptor |
| certification evidence bundle | engine/probe-backed proof material | input to server-issued `CertifiedTruth(K)` only |
| strategy descriptor | owns `A`, `mu`, `d` algebra, overlap/rival policy, phrase capability, and falsification burden | only production source of Sxx public claim eligibility |
| validation scaffold | executable legacy expectation check | non-public migration evidence only |

A strategy projection can reach public surface only through:

```text
CertifiedTruth(K)
  -> StrategyGeometryEngine
  -> StrategyProjectionAdmissionProducer
  -> EvidenceClaimProducer
  -> renderer/API/frontend
```

The production path consumes descriptor-certified K only. It does not consume raw
certification rows, raw witnesses, support seeds, or `LegacyValidationScaffold`
admissions directly. Row-specific scaffold burdens and evidence-kind names live
in `StrategyProjectionBoundaryMatrix.md` and `ValidationMethodology.md`, where
they are tracked as validation scaffold / migration status, not as public
runtime contracts.

Current production migration status:

| Band range | Status |
| --- | --- |
| S23 | first thin descriptor/K slice exists and remains default-disabled |
| S01-S22, S24-S25 | semantic boundary plus validation scaffold only |
| relative/rival/overlap expansions | pending descriptor/K migration |

### Layer 6: Strategy Projections

The `S01-S25` strategy labels are projection vocabulary only.

They are not released directly from raw features or raw witnesses.

They are released only from descriptor-certified `CertifiedTruth(K)` produced by
the projection admission path.

### Layer 7: Renderer

The renderer does not own strategy truth.

It verbalizes already certified claims.

Model-authored wording, if any survives later, is limited to presentation and
cannot own strategy truth.

### Engine / Probe Sidecar

Engine and probe evidence are not part of `R` and do not participate in witness
admission.

They are a separate side evidence channel consumed only in validation-side
checks at:

- `Root` broad-validation confound filtering for selected engine-required rows
- `Delta`
- `Certification`

## Strategy Projection Vocabulary

The strategy projection vocabulary is the `S01-S25` semantic band set. It is a
projection vocabulary, not a truth-owning layer.

| ID | Strategy projection |
| --- | --- |
| `S01` | opposite-side castling pawn storm |
| `S02` | direct piece concentration king attack |
| `S03` | color-complex / diagonal king attack |
| `S04` | king shelter demolition |
| `S05` | central break |
| `S06` | space clamp |
| `S07` | development lead into initiative |
| `S08` | prophylaxis / counterplay denial |
| `S09` | open or semi-open file penetration |
| `S10` | outpost occupation |
| `S11` | weak pawn fixation and attack |
| `S12` | weak-square or color-complex domination |
| `S13` | minority / wing-damage attack |
| `S14` | pawn-chain base attack |
| `S15` | passer creation |
| `S16` | enemy passer blockade and suppression |
| `S17` | worst-piece improvement / bad-piece exchange |
| `S18` | bishop-pair or minor-piece edge conversion |
| `S19` | favorable simplification |
| `S20` | domination / mobility collapse |
| `S21` | central or opposite-wing counterplay |
| `S22` | neutralization / consolidation / fortress holding |
| `S23` | king activation / opposition / penetration |
| `S24` | tactical conversion of a prepared target |
| `S25` | horizontal rank access / cross-wing rank switch |

### Projection Independence Contract

Every `Sxx` must remain semantically separable from its rivals on exact boards.
Overlap is allowed; wording collapse is not. A band may be discussed publicly
only after its claim is constructed from certified lower carriers and admitted
through the descriptor/K production path.

Forbidden inputs for projection truth:

- legacy inventory labels
- broad chess concepts
- raw tactics
- raw engine packets or PVs
- source rows
- renderer text
- planner lane selection
- claim-shaped projection candidates
- test-only `LegacyValidationScaffold` admissions

### Production Admission Contract

Production projection admission is K-only:

`CertifiedTruth(K) -> StrategyGeometryEngine -> StrategyProjectionAdmissionProducer -> EvidenceClaimProducer`

`EvidenceClaimProducer` lowers only admitted `StrategyProjectionAdmissionResult`
values with `DescriptorCertifiedRuntime` authority. The raw
`StrategyProjectionAdmission` matcher is not production main code; it lives only
as `src/test` validation scaffold and cannot create public claims.

The descriptor owns the public contract for a production slice:

- region id `A_{S,k}`
- center/prototype `mu_{S,k}`
- descriptor-owned distance algebra `d_{S,k}`
- allowed evidence kinds
- carrier binding laws
- engine proof-identity policy
- overlap policy
- phrase capability
- falsification burden
- rival bands
- rollout/default flag

Every live descriptor must have per-slice falsification-harness coverage:
`exact`, `near_miss`, `false_rival`, and `shortcut_negative`. This harness is
test-only; it validates descriptor/K migration discipline and does not become a
production admission source.

`d` and overlap ranking are classifier outputs over already certified truth.
They are not proof, not a metric truth, and not a substitute for exact board
certification.

### Current Migration Status

| Scope | Status |
| --- | --- |
| `S23` | thin descriptor/K production slice exists: `A_S23_endgame_entry_or_opposition` and `A_S23_line_access_activity`; default production remains disabled unless explicit region ids are supplied |
| `S01-S22`, `S24`, `S25` | validation-scaffold-only for projection admission; each future public slice must be migrated into a descriptor-owned K-backed region |
| `S01-S25` corpus rows | coverage/countability authority only; they do not widen production runtime |
| `StrategyProjectionAdmissionScaffold` | test-only raw matcher for exact-board scaffold regression; produces non-public `LegacyValidationScaffold` results |

Detailed semantic boundaries live in
[StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md).
Exact validation methodology lives in
[ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md).
## Transcript-Derived Design Claims

The motivating discussion established the following claims as authoritative for
this branch.

### Claim A

`24 x 61 x 3016+35` was a useful ontology proposal, but the current branch no
longer treats `R4` as root truth.

The living low-layer contract is `24 x 61 x 2856+35`.

### Claim B

The safe release law is:

- exact-board evidence first
- typed witness second
- object third
- delta fourth
- certification fifth
- strategy projection only after certification

### Claim C

The old `llm` backend should not be preserved just because it existed.

Its semantics were treated as sufficiently bottlenecked that this branch
chooses demolition rather than repair.

### Claim D

The old `llm` docs on this branch are not migration references anymore.

They are removed so they cannot silently keep authority.

## Output Contract

The transcript also fixed one preferred output shape for future commentary
rendering.

| Field | Meaning |
| --- | --- |
| Primary strategy | strongest long-plan projection |
| Secondary strategy | one or two subordinate projections |
| Tactical converter | tactical gate that realizes the edge |
| Opponent counterplay | the opponent's surviving release resource |
| Critical root truths | the exact low-layer truths supporting the claim |

This output contract is presentation-level only.

It must not bypass certified object/delta ownership.

## Operating Rules For This Branch

- backend demolition is allowed
- compile-red states are allowed
- frontend preservation is allowed
- old `llm` semantic authority is not allowed
- new commentary backend work should accumulate under `modules/commentary`

## Immediate Construction Priorities

1. freeze the closed `R0-R3 + Aux` root-state vocabulary
2. freeze the closed `61`-descriptor inventory and its ownership map
3. freeze past-failure taxonomy and canonical owner boundaries
4. freeze large-scale exact-position validation methodology
5. define canonical object contracts
6. define delta certification contracts
7. define strategy projection rules
8. reconnect surviving frontend/backend seams later

## Experimental Status

This branch is an intentional demolition and reconstruction branch.

It is not a maintenance branch.
