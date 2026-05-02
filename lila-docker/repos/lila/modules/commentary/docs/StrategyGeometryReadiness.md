# Strategy Projection Foundation Readiness

This file is the compact current-status ledger for the finite typed Sxx
projection foundation. It is intentionally replace-in-place: when status
changes, rewrite the relevant rows and keep only the current state.

The current model is not a smooth manifold and does not define a global chess
metric. It is a finite family-indexed algebra of certified board facts `K`
plus partial strategy classifiers. Existing `StrategyGeometry*` source names
are stable module names; semantically, `d` is a post-admission fit/ranking
functional and overlap is a public-selection preorder over already certified
memberships.

It does not supersede `StrategyProjectionBoundaryMatrix.md`. That matrix owns
the semantic meaning of `S01` through `S25`. This file tracks the common
foundation required before those bands can become descriptor-backed public
claims.

## Current Decision

S23 should not start as another band-local projection exception. Before any
new Sxx surface expansion, the shared foundation must define:

1. a finite-cover envelope for the certifiable board-fact algebra `M`
2. a closed disposition algebra for every candidate entering or failing to
   enter `M`
3. a certification spine that makes engine evidence role-bound and prevents
   projection or renderer code from reading raw lower evidence as truth
4. a descriptor contract for `A`, `mu`, `d`, and overlap, where `d` is a
   finite fit/ranking score only after hard admission and overlap never creates
   truth
5. a runtime `K` producer, binding law, engine proof identity, falsification
   burden, and phrase capability for each public slice

The first S23 region may then be used as the first consumer of that foundation,
not as a private runtime path.

## Finite-Cover Envelope

Let `C` be the universe of runtime claim candidates. Let `M` be the finite
family-indexed algebra of certifiable board facts admitted from `C`.

`M` is not an exhaustive set of all chess truth, not a topological manifold,
and not a source corpus. A value enters `M` only as a disposition-bound
`Certified(K)` or as an explicitly non-promotable sibling disposition. Public
strategy classifiers consume `Certified(K)` values; they do not read raw lower
evidence as truth.

`M` is covered by these finite families:

| Family | Meaning | Sxx coverage bands |
| --- | --- | --- |
| `BoardTransition` | exact current-board and move-transition facts | all |
| `MoveCausalTactic` | move-caused tactic or tactical release facts | `S24`, support/anti-case for many |
| `KingSafetyAttack` | king-ring pressure, shelter damage, king-facing attack routes | `S01-S04` |
| `LineAccessActivity` | file, diagonal, rank, and entry-access facts | `S09`, `S12`, `S25`, support for `S02/S03/S23` |
| `PawnStructurePasser` | levers, fixed targets, chain bases, passer creation/suppression | `S05`, `S11`, `S13-S16`, `S21` |
| `SpaceRestrictionMobility` | space bind, outpost, local restriction, mobility domination | `S06`, `S10`, `S12`, `S20` |
| `InitiativeCounterplay` | move-right, development conversion, denial, counterplay survival | `S07`, `S08`, `S21` |
| `PieceQualityMaterialTrade` | piece-liability repair, bishop-pair conversion, material/trade tasks | `S17-S19`, support for `S20/S24` |
| `EndgameConversionHolding` | king entry, opposition, fortress, perpetual, race/hold facts | `S16`, `S22`, `S23` |

Source provenance `Xi`, candidate-line packets, cache state, and raw engine
packets are not members of `M`. They may feed certification or context, but
they do not create a board fact.

`StrategyProjectionCoverageContract.CoverageGate` remains useful test/corpus
infrastructure, but it is not the runtime `M` boundary. The runtime boundary is
the family/disposition/descriptor path in `StrategyGeometryFoundation`,
`StrategyRuntimeKProducer`, `StrategyGeometryEngine`, and
`StrategyProjectionAdmissionProducer`.

## Disposition Algebra

Every candidate in `C` must end in exactly one public-path disposition:

| Disposition | Public meaning |
| --- | --- |
| `Certified(K)` | board-certified truth eligible for Sxx admission and public claim lowering |
| `SupportOnly(Ks)` | valid support evidence, explicitly non-promotable to primary claim |
| `ContextOnly(Xi)` | source or retrieval context; never board truth and never proof |
| `Deferred(reason, missingBurden)` | plausible candidate whose required proof burden is not implemented or not satisfied |
| `Rejected(reason)` | candidate contradicts an exact gate, stale binding, illegal replay, wrong owner/anchor/route, or failed role evidence |
| `AntiCase(reason, regressionId)` | known false-positive shape that must stay blocked and regression-tested |

No renderer prose may be emitted from `SupportOnly`, `ContextOnly`,
`Deferred`, `Rejected`, or `AntiCase`. Only `Certified(K)` may enter Sxx hard
admission. If hard admission fails, no finite fit score, membership, or public
strategy claim exists.

A renderer role label such as "Primary", "Support", "Context", or "Contrast"
is not public prose. It may not turn an otherwise silent scaffold into a public
move explanation.

## Descriptor Spine F0-F7

The current public-Sxx foundation is descriptor-driven:

| Step | Current state |
| --- | --- |
| `F0` | docs terminology is corrected to the finite algebra / partial classifier model; `M` is not a manifold, `d` is not a metric, and overlap is not truth resolution |
| `F1` | `StrategyProjectionSliceDescriptor` owns each public slice contract |
| `F2` | classifier and producer paths consume descriptors rather than parallel band-local constants |
| `F3` | `StrategyRuntimeKProducer` is the runtime `K` minting seam for descriptor-bound candidates, and producer/public paths consume only its `RuntimeK` output |
| `F4` | each descriptor carries executable carrier binding laws; S23 entry/contact laws now require matching carrier binding payload before public admission |
| `F5` | engine-role `K` requires `EngineProofIdentity` and a bound Certification carrier; descriptor engine policies fail closed if required roles are omitted |
| `F6` | each descriptor records falsification burdens and `StrategyRuntimeKProducer` derives terminal wrong-anchor, wrong-route, stale-root, and support/missing-carrier reasons before minting `K` |
| `F7` | each descriptor carries phrase capability, and descriptor forbidden terms flow through admission, Projection `CommentaryClaim`, and renderer phrase capability |

## Missing Or Defective Elements

Current inventory count: `29`.

| ID | Element | Current state | Decision | Priority |
| --- | --- | --- | --- | --- |
| `G01` | finite-cover envelope for `M` | implemented foundation: `StrategyGeometryFoundation.certifiedFactFamiliesByBand` pins the nine-family finite algebra over `S01-S25`; projection `CoverageGate` remains validation infrastructure | keep this as the shared envelope before adding band-local descriptors | `P0 done` |
| `G02` | closed disposition algebra | implemented foundation: `StrategyGeometryFoundation.PublicPathDisposition` closes six endpoints and selection treats `anti_case` as terminal | keep public-path dispositions separate from broad certification verdicts until `K` exists | `P0 done` |
| `G03` | unified exact transition identity `tau` | implemented foundation: `StrategyGeometryFoundation.ExactTransitionIdentity` carries `beforeFen + playedMove + currentFen + nodeId + ply + variant`; its cache key excludes source PGN/provenance labels | keep this as the exact transition identity input for future burden/probe derivation | `P1 done` |
| `G04` | source provenance `Xi` separation | implemented boundary: source context remains context-only and `tau` is exact replay identity, not source PGN provenance; `missing_pgn_file` is not a runtime replay gate | keep source PGN provenance separate from exact replay and Sxx admission | `P1 done` |
| `G05` | proof burden `beta` | implemented foundation: `StrategyGeometryFoundation.ProofBurden` records family, owner, anchor, route, scope, required engine roles, carrier kinds, transition requirement, and disposition | use `beta` as the single proof-burden shape before probes or projection | `P1 done` |
| `G06` | typed probe request `q = Q(beta, tau, policy)` | implemented request-only planner: `StrategyProbeRequestPlanner` derives role-bound internal probe requests only from `ProofBurden`, legal replay-validated `ExactTransitionIdentity`, and server-owned role policy; each request carries an independent Q policy fingerprint derived from the role policy and engine config fingerprint, and candidate-line probes remain separate | keep Q as internal scheduling/validation intent only; it must not create `K`, projection admission, public claims, renderer text, or candidate-line evidence by itself | `G06 fixed` |
| `G07` | full typed engine-role taxonomy `E_q` | implemented foundation: `CertificationEngineRole` maps certification evidence purposes into closed roles including confound, causality, best defense, release, persistence, ambiguity, tablebase, line support, and surface capability | preserve typed engine roles through certification evidence and downstream provenance | `P1 done` |
| `G08` | engine role reuse and invalidation law | P2 public boundary tightened and final Q binding added: accepted engine evidence carries roles and remains freshness/node/FEN/baseline/fingerprint gated; engine-role claims require a server-shaped `q-...` request id matching role, family, owner, anchor, node id, and ply plus independent Q policy fingerprint and server floor minima; public claims preserve `EngineCertification` provenance separately from lower board truth, and a generic exact-board carrier no longer counts as the concrete board reason for engine-backed public exposure | full cross-cache reuse matrix remains future work; no cross-role reuse may be inferred from matching family alone | `P1 foundation / P2 public guard / Q binding fixed` |
| `G09` | PV invariant preservation checks | implemented fail-closed runtime barrier: engine intake now requires Q request identity, Q-policy fingerprint binding, server role-policy floors, per-role invariant reports, bound baseline for comparative/conversion/counterplay roles, and transition binding for release/causality/persistence roles before Engine E evidence is minted | actual probe execution may compute richer reports later, but missing reports cannot create public evidence | `post-P3 fixed` |
| `G10` | defender coverage / best-defense survival | implemented fail-closed runtime barrier: `best_defense_survival` still requires `MultiPV >= 3` with distinct first moves and now also requires a satisfied semantic coverage report, so branch count alone cannot certify best defense | future work may improve how coverage is computed, not whether it is required | `post-P3 done` |
| `G11` | anti-causality / coincidence checks | implemented foundation: centralized anti-causality barriers include pre-existing fact, already-loose/pinned, pawn/king shape shift, mate dominance, material collapse, and standing-tactic coincidence | keep anti-causality as a terminal barrier before public move-causal claims | `P1 done` |
| `G12` | horizon, ambiguity, and tablebase finality | implemented fail-closed runtime cap: engine claim cap reports for horizon, ambiguity, tablebase, mate dominance, or material collapse block certification; mate scores cannot certify non-mate families and material-collapse scores cannot certify non-material families | richer tablebase/horizon producers remain future, but cap reports cannot pass through as public truth | `post-P3 done` |
| `G13` | categorical certified truth `K` | implemented runtime boundary: `StrategyGeometryFoundation.CertifiedTruth` is private-constructor/factory-minted, root-bound, disposition-bound, proof-burden checked, carries route-bound projection evidence kinds plus public-safe lower carrier refs, and engine-role K needs a bound Certification carrier before classifier membership; descriptor false-rival, wrong-evidence-kind, and missing-exact-board-fact cases now fail before K minting | keep `K` as the only object that can enter default public Sxx production; fake K construction/copy rebinding is outside the runtime contract | `P1 contract done / P2 strengthened / foundation hardening done / falsification harness fixed / descriptor standard fixed` |
| `G14` | projection consumes only `K` | implemented for production: `StrategyProjectionAdmissionProducer` consumes same-root `Certified(K)` plus explicit admitted region ids; `S01-S25` start-ready ids and raw projection evidence cannot open production. The older raw `StrategyProjectionAdmission` matcher has been removed from `src/main` and remains only as a `src/test` validation scaffold with `LegacyValidationScaffold` authority. | migrate each future public Sxx region through the K-only producer rather than through raw seed/evidence admission | `P2 surface done / production cleanup done / per-band migration future` |
| `G15` | first-class hard gates `H_{S,k}` | implemented fenced minimum: legacy `admitsSxx` functions are test-only validation scaffold; runtime public classification enters through named K-backed admitted regions only; `StrategyProjectionFalsificationHarness` now generates exact, near-miss, false-rival, and shortcut-negative checks for every live descriptor | expose additional hard gates as named K-backed regions only with per-slice falsification coverage | `P3 done for S23 minimum / production cleanup done / per-band future / falsification harness fixed` |
| `G16` | admitted regions `A_{S,k}` | implemented runtime minimum: S23 currently has `A_S23_endgame_entry_or_opposition` and `A_S23_line_access_activity`; default production remains disabled and explicit region ids are required | add concrete runtime regions only behind hard admission and explicit region enablement | `P3 done for S23 minimum / per-band future` |
| `G17` | centers/prototypes `mu_{S,k}` | implemented runtime minimum: `StrategyGeometryEngine` maps each live S23 admitted region to one concrete center, `mu_S23_endgame_entry_or_opposition` or `mu_S23_line_access_activity` | keep centers stable and one-to-one with region inventory; do not infer centers from band ids or prose | `P3 done for S23 minimum` |
| `G18` | fit/ranking `d_{S,k}` | implemented descriptor algebra: finite non-negative fit scores are assigned by each `StrategyProjectionSliceDescriptor.distanceAlgebra` only after same-root `Certified(K)`, descriptor-owned proof-burden minimums, descriptor carrier binding payload, engine proof identity, transition, route, scope, route-bound projection evidence kind, exact-board seed proof, and region gates pass; hard-admission failure remains non-finite/no membership | treat finite `d` as descriptor-owned classification only, never proof, metric truth, or truth creation | `P3 done for S23 minimum / F1-F7 refreshed / descriptor algebra fixed` |
| `G19` | overlap selection preorder | implemented descriptor algebra: overlap resolution consumes each descriptor's `overlapPolicy`, ranks finite memberships deterministically, picks one public-primary membership only when the truth family matches the center primary family, and leaves non-primary/broad-only memberships as support | overlap may order already certified memberships but may not strengthen truth, resolve semantic contradiction, or widen wording | `P3 done for S23 minimum / F1-F7 refreshed / descriptor algebra fixed` |
| `G20` | public claim lowering `phi(K, Sxx)` | implemented boundary: K-backed S23 admission lowers through `EvidenceClaimProducer`, `ClaimSelector`, and `CommentaryRendererContract.publicClaims` into public-safe `PublicClaim` objects | lower only admitted memberships into public-safe claim contracts | `P2 done` |
| `G21` | phrase capability lattice | implemented descriptor-to-render boundary: each `StrategyProjectionSliceDescriptor` owns phrase capability, admission carries it into the Projection `CommentaryClaim`, and `PublicClaim.PhraseCapability` records allowed predicates, wording ceiling, line-comment permission, result/best/engine language denial, and forbidden terms per public claim | keep capabilities as permission data, not prose templates | `P2 done / descriptor algebra fixed / descriptor standard fixed` |
| `G22` | renderer consumes `PublicClaim` only | implemented boundary: `CommentaryRendererContract.publicPlan` lowers `CommentaryPlan` to `PublicCommentaryPlan`, and final rendering maps `PublicClaim` objects to blocks; `CommentaryRenderer.render` can add only phrase-capability-gated safe line text | prevent renderer or annotation planner from inferring strategic meaning | `P2 done` |
| `G23` | mate/material collapse and anti-case harness | implemented foundation: anti-case families are centralized and exact-board regression tests pin pre-existing loose, pre-existing pin/xray, and mate-dominated tactical smells; material-collapse remains registered for future exact rows | keep anti-cases terminal and expand rows only when exact FEN cases are added | `P1 done` |
| `G24` | Sxx projection classifier | implemented runtime minimum: `StrategyGeometryEngine.classify` consumes `StrategyProjectionSliceDescriptor` values and produces concrete S23 memberships with center, finite `d`, classification, overlap role, and rank; `StrategyProjectionAdmissionProducer` consumes only public-primary memberships | extend descriptors per Sxx rather than adding band-local projection shortcuts | `P3 done for S23 minimum / F1-F7 refreshed / per-band future` |
| `G25` | projection evidence rebinding guard | retained as test-scaffold regression: legacy `StrategyProjectionAdmission.admit` rejects a stale original evidence bundle before any filtered rebinding can admit a same-shaped payload; production public lowering no longer consumes that legacy authority | keep original-bundle binding as a validation-scaffold invariant and re-express future production slices through descriptor/K binding laws | `P0 done / production cleanup done` |
| `G26` | default projection-admission producer boundary | implemented boundary: `StrategyProjectionAdmissionProducer` is wired into the default backend handoff and fails closed by default; producer enablement is region-based and separate from `startReadyBandIds` | keep this as the only default Sxx producer seam; future regions must opt in explicitly | `P1 done` |
| `G27` | engine-backed certification provenance preservation | implemented: certification evidence carries typed engine roles and default claim production emits bounded `EngineCertification` provenance; backend, selector, and renderer require accepted intake plus same-binding Certification and exact-board/board carrier before public render | keep engine provenance distinguishable from lower board truth and never treat raw engine/PV as a claim owner | `P1 done` |
| `G28` | renderer fallback public text | implemented: `CommentaryRenderer.render` emits public text only from a safe English line comment on the matching primary claim; role labels remain structured data only | future prose must enter through `PublicClaim` text or another explicit public-safe comment contract | `P0 done` |
| `G29` | xray home-pawn/start-style root anti-case | implemented foundation: `starting_position_home_pawn_xray` is a centralized anti-case family and exact-board producer tests keep starting-position xray roots off public surface | keep this non-promotable unless a future exact carrier-bound xray contract exists | `P1 done` |

## Implementation Order

`P0` is complete for the model envelope and immediate public-safety leaks:
`G01-G02`, `G25`, `G28`.

`P1` is complete for the certification spine and false-positive barrier:
`G03-G05`, `G07-G08` minimum provenance boundary, `G11`, `G13`,
`G16-G19` contract definitions, `G23`, `G26-G27`, and `G29`.
The post-P3 pass completed the remaining cross-cutting runtime barriers for
role invariants (`G09`), best-defense coverage (`G10`), and public caps (`G12`).

`P2` public surface migration is complete for the requested boundary:
K-only canonical projection production (`G14`), the first K-backed S23 runtime
region (`G15-G16` minimum), public claim lowering (`G20`), phrase capability
(`G21`), and renderer public-plan consumption (`G22`). Broader per-band hard
gates and richer probe-result producers remain future work before broad public
Sxx expansion.

`P3` is complete for the requested S23 minimum classifier layer:
`G17-G19` concrete values/fit/resolution and `G24`.
The runtime inventory is intentionally small: two S23 regions, two concrete
centers, finite `d` after hard admission only, and deterministic overlap
roles/ranks. Broad S23 line-access membership remains support-only unless the
focused line-access region is explicitly enabled and wins public-primary
resolution. This keeps P3 as classification of `Certified(K)`, not expansion of
truth ownership.

`G06` is complete as the first post-P3 cross-cutting blocker: it adds
`Q(beta, tau, policy)` request derivation without wiring probes into public
truth. The next post-P3 pass completed the remaining cross-cutting public-safety
barriers: `G09`, `G10`, and `G12`.

S23 starts only after `P0` and the required `P1` subset are in place. The
required subset is the exact transition identity, proof burden, typed engine
role boundary, certified-truth boundary, minimal `A/mu/d/overlap` contract,
anti-case harness, and the canonical projection-admission producer. Its first
public region should be one `A_{23,k}` consumer of the common gates, not a
band-local shortcut.
