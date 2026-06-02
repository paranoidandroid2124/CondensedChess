# Commentary Program Map

This is the onboarding map for the current Chesstory commentary-analysis work.
It points to the live authority documents and summarizes the current operating
state.

## Current Status

Current work is a boundary redesign plus docs compression:

- claim authority is centralized under `analysis.claim`
- planner authority checks are delegated through `PlannerClaimAdmission`
- render release safety is centralized under `analysis.render.FragmentAuthority`
- public MoveReview wire is minimized around backend-owned structured
  diagnostics and the certified `moveReviewPlayerSurface`
- opening-family support rows now enter that player surface only through
  `OpeningFamilyCatalog` plus `OpeningFamilyClaimResolver` `SupportedLocal`
  admission, not through rendered prose or frontend reconstruction. They may
  expose bounded `openingBook` metadata from `OpeningReference` aggregates
  (ECO, total master-game count, and up to three SAN top moves), but never raw
  explorer/source/sample-game payloads.
- static opening coverage now comes from `openings.tsv` runtime rows plus
  tooling provenance reports, while preserving the same label-plus-FEN family
  proof boundary; removed fixture floors are no longer
  treated as coverage authority. The pool is currently pruned to 1276 rows that
  replay against captured Lichess masters evidence as `master-backed`; the live
  audit found and removed 438 `not-found-in-masters` expansion rows. Same-EPD
  transposed endpoint aliases remain available to the family resolver even when
  canonical display lookup chooses one row.
- static opening coverage expansion is paused for provenance cleanup:
  `lila.commentary.tools.opening.OpeningPoolAudit` and
  `OpeningMasterDbAuditRunner` now classify parse issues, normalized endpoint
  duplicates/transpositions, and optional master DB evidence under
  `modules/commentaryTools/src/test`; `--base-url` selects the masters endpoint
  while the default remains Lichess. `--since`/`--until` are optional query
  window parameters for endpoints that accept them; current `/masters` live
  audit should normally run without dates because date-windowed master queries
  can return `HTTP 400`. Live runs can write replayable raw-response JSONL with
  `--write-evidence-cache`; fetch or parse failures are reported as
  `master-fetch-error`. Reports include `provenanceStatusCounts`, and
  `--only-status` narrows the output rows for cleanup triage. `--skip-rows`,
  `--max-rows`, and `--request-timeout-seconds` support chunked/resumable live
  audit. Rows remain unverified unless a live OAuth-backed master DB report or
  replayable JSONL evidence cache keyed by endpoint-stable `rowId` marks them
  `master-backed`
- opening route coverage is expanding through `opening_routes.tsv`
  descriptors while preserving legal replay plus target-mode proof gates.
  `OpeningRouteMiningRunner` is tooling-only support under
  `modules/commentaryTools/src/test`; it mines candidate knight routes from the
  master-backed `openings.tsv` pool, filters out one-ply generic development
  and repeated-square paths, and leaves low-support candidates deferred. The
  current runtime route catalog contains 52 descriptors: 48 knight-route rows
  plus 4 bishop fianchetto rows for Catalan, English, King's Indian, and
  Queen's Indian support metadata. Every route target is present in the
  corresponding `OpeningFamilyCatalog` target allowlist.
- opening goal/prose coverage is expanding inside the existing `OpeningGoals`
  evaluator for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch `...Ne4`,
  Queen's Indian `...Ne4`, Bogo-Indian `...Ne4`, Catalan `dxc5` tension
  release, Open Catalan `c4` pawn recovery, Sicilian `...c5` c-pawn challenge,
  and King's Gambit `f4` break structures; those evaluations still flow only
  through `openingGoalEvaluation` into outline/explanation consumers and do not
  expand exact `central_break` authority beyond the d/e-pawn witness
- canonical docs stay as four files, but historical CTH logs are compressed
  into current-state rules and summary tables

Do not use branch-external or branch-removed documents as authority for this
worktree.

## Document Roles

Read the documents in this order:

1. `CommentaryProgramMap.md`
   - onboarding, current status, active frontier, document navigation
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
- `PlannerClaimAdmission` connects planner plans/inputs to that resolver.
- `MoveReviewExchangeAnalyzer` owns bounded legal PV replay for favorable
  exchange witnesses, queen-trade/simplification geometry, exact-slice branch
  keys/continuation terms, IQP inducement prefixes, quiet/central-break
  best-defense metadata consumers, L3 threat first-step evidence, and shared
  semantic observations. Owner witnesses that describe the reviewed move must
  receive `playedMove`; the top PV first move is replay evidence, not identity.
  Defender-trade owner visibility is typed-context plus replay proof, not raw
  defender/trade prose. Other implemented relation support also carries
  analyzer-owned typed relation details for king-attack, material-target, line,
  and lure-and-win geometry, so later consumers can
  reuse the same legal replay/attack-defense witness without introducing
  motif-local parsers. Implemented relation extraction is registered through
  `MoveReviewExchangeAnalyzer.ImplementedRelationWitnessTemplates`; adding a
  relation motif means adding one analyzer hook to that registry, then wiring
  the catalog descriptor, not creating a parallel replay path. A relation
  witness with non-empty typed details must
  match its relation kind before semantic emission, and semantic consumers cross
  that boundary through the analyzer-owned relation projection rather than raw
  witness fields. Defender/bad-piece owner seed and transition terms are
  expanded from the projection's structured `relationSupport`, and policy
  continuation terms consume those analyzer-built structure terms instead of
  rebuilding relation markers or reusing dynamic support fact strings as
  runtime inputs. The same structured support helper enriches selector
  idea/focus text for `StrategyPack` digest, and relation prompt hints use the
  player-facing idea text when matching support is present, without granting
  proof authority. Branch-key and `branch:*` fact formatting is
  also analyzer-owned through shared branch helpers rather than policy-local UCI
  slicing; `best_branch:*` and `exchange_square:*` fact formatting use the same
  analyzer boundary.
  `PlayerFacingExactSliceProofFacts` owns typed exact-slice proof shape/path
  checks and target witness label formatting for fixed targets, coordinated
  targets, weak squares, local file-entry structure markers, target-focused
  support markers, and color-complex minor-piece attack markers.
  Probe reply coverage, best-reply heads, and defender-resource reply
  heads are likewise read through analyzer helpers (`probeHasReplyCoverage`,
  `probeBestReplyHead`, `probeDistinctReplyHeads`,
  `probeBestReplyLineDisplay`, `probeDisplayReplyLines`,
  `probeAllReplyLines`, `probeBestReplyLines`, `probeBestReplyPrefix`,
  `probeBestReplyLength`) instead of proof-local `replyPvs` parsing.
  Probe purpose families for reply, conversion, route-validation,
  route-continuity, author-evidence, played-counterfactual, and null-move
  threat probes are classified by `ThemePlanProbePurpose` helpers rather than
  proof- or feature-local raw purpose sets; author branch-cardinality gates
  and request budgets, required signals, objectives, and horizons for these
  purpose families use the same helper boundary. Latent hypothesis/refutation purpose profiles,
  including required signals, objectives, horizons, and default cp-loss gates,
  are classified through `ProbePurposeClassifier`. Legacy candidate probe id
  families such as competitive/aggressive probes are classified through
  `ProbePurposeClassifier` helpers before candidate tagging or why-not prose.
  Prevented-plan evidence term projection (`counterplay_drop`,
  `neutralized_break`, `denied_squares`, `denied_resource`,
  `denied_entry_scope`) is centralized through `PlanEvidenceEvaluator` rather
  than repeated in proof modules. Prophylactic denied-resource class
  normalization and exact-slice token validation use the same evaluator
  boundary. Plan certification trace terms and `support_probe:*` markers use
  the same evaluator projection boundary. PV support markers (`pv:*`) are
  projected through `MoveReviewPvLine.pvMoveTerms` rather than witness-local raw
  prefix assembly.
  Theme/subplan support tags are created and parsed through
  `PlanTaxonomy.ThemeResolver`, including legacy embedded subplan annotation
  parsing/stripping and embedded theme preconditions, not planner- or
  detector-local prefix slicing. Live probe generation no longer encodes
  subplan authority in display `planName`; request contracts carry purpose,
  objective, required signals, horizon, and stable plan binding separately.
  Taxonomy-backed proof contract ids use the same resolver boundary.
  Structural state and latent-seed evidence tags use the same resolver
  boundary.
  User-facing plan semantic policy is owned by `PlanSemanticsContract`.
  `PlanTaxonomy.ThemeResolver` may classify and tag proposal/debug/compatibility
  carriers, but strategic idea-kind mapping, probe-purpose routing, and
  thematic fallback eligibility consume the typed semantics contract rather
  than raw plan names, aliases, evidence-source strings, execution steps, or
  failure-mode prose.
  The runtime admission boundary now starts at `PlanClaimBoundary`:
  `PlanProposal` records whether a theme/subplan came from explicit typed
  metadata, support tags, or proposal-only inference; broad aliases and default
  subplans do not mint supportable subplans. `PlanSupport`,
  `AdmittedPlanClaim`, and `RenderedPlanText` are the typed lifecycle names for
  the planned support -> user-facing claim -> prose boundary. MoveReview
  producer/probe/outline/ledger/transposition consumers now use this boundary:
  `PlanMatch`, `ProbeRequest`, `PlanContinuity`, `LatentSeed`, `PlanRow`,
  `StrategicPlanExperiment`, and `PlanHypothesis` are projected through
  `PlanProposal` before their theme/subplan meaning is consumed. Thematic
  fallback, player-surface plan family checks, truth-mode proof-family and
  trigger-kind projection, strategic ledger motif profile, outline slot
  selection, and transposition weakness alignment consume the typed boundary
  rather than reparsing plan names.
  Weak long-plan candidate quality is improved inside the same runtime path by
  board/legal-move evidence helpers rather than new planner surfaces:
  `PieceRedeploymentEvidence` for worst-piece, bishop, rook-file,
  non-king open-file-pressure targets, and outpost reroutes; it excludes
  pawn/king reroutes, same-file rook moves from rook-file transfer, and active
  queen centralization from worst-piece improvement unless the queen is a
  low-activity recovery. `AdvantageTransformationEvidence` for passer
  conversion/manufacture, simplification-conversion, invasion transition with
  check or a non-king resource target, and
  opposite-bishops conversion; `FlankInfrastructureEvidence` for aligned or
  hook-backed rook-pawn march, hook creation, and rook-lift scaffolds;
  `PawnBreakEvidence` for concrete central/wing pawn breaks under the
  pawn-break taxonomy theme and tension-maintaining quiet moves that support
  the preserved tension by castling, minor-piece centralization/mobility gain,
  or a major piece on a tension file;
  `RestrictionPlanEvidence` for board-measured break prevention,
  key-square denial, quiet mobility suppression, central-space bind, and flank clamp
  without duplicating those specific move-level candidates as generic
  prophylaxis;
  `WeaknessFixationEvidence` for `WeaknessTargetProfile`-anchored
  static/backward/IQP target moves and semantic-ready minority-attack
  prep/break support moves; and `FavorableExchangeEvidence` for
  defended queen-vs-queen exchange, defender-trade, bad-piece liquidation, and
  bounded recapture-like simplification-window candidates rather than large
  tactical material wins.
  Piece-redeployment, flank-infrastructure, pawn-break, restriction, and
  weakness-fixation helpers keep diagnostic reason tokens in evidence/probe
  metadata, while `PlanHypothesis` preconditions and execution steps are
  phrased from board-backed candidate fields before MoveReview rows render
  them.
  Helper ownership is theme/family scoped; do not introduce one helper per
  PlanKind when an existing family helper can own the typed evidence.
  Rook-lift scaffolds need a lift-rank rook move plus enemy king-file pressure,
  check, or a non-king resource target; a flank-file lift alone is not support.
  Helper-owned specific long-plan subplans are not emitted directly from
  `PlanProposalEngine` broad structural booleans; outpost, rook-pawn, hook,
  pawn-break, weakness, exchange, restriction, and transformation specifics
  should come from their family helper candidates.
  Shared pawn/target/material primitives live in `PlanMoveEvidenceSupport`.
  `ProbeDetector` consumes these refined subplan candidates before broad theme
  move families, and covered weak subplans fail closed rather than falling back
  to generic theme moves when no refined candidate exists.
  `generic_center_plan` is retained only as theme/debug metadata and is not a
  live `CentralBreakTiming` subplan projection.
  `PlanEvidenceEvaluator.planSupport`, `admittedPlanClaim`, and
  `StrategicPlanEvidenceView.mainAdmittedClaims` are the runtime projections
  into the support/admission lifecycle types. `MoveReviewPlayerPayloadBuilder`
  uses `RenderedPlanText` for promoted-plan row strings after admission rather
  than rebuilding plan meaning from rendered prose. It also owns final player
  detail cleanup for plan rows: probe/test/candidate-request wording from
  hypothesis detail fields is suppressed through
  `UserFacingSignalSanitizer.sanitizePlanDetail`, and typed contract/name
  fallback is allowed only when a non-empty detail field was suppressed rather
  than when a plan had no detail. Its player-surface family gates for weakness targets,
  prophylaxis rows, and promoted/practical sibling suppression consume
  `PlanProposal.fallbackTheme` and `supportKind`; inferred proposal themes from
  plan names, execution steps, or failure-mode prose are not row-creation or
  row-suppression authority. A promoted plan may also get a `Plan status`
  advanced row from matching `StrategicPlanExperiment` flow state, but only
  when the experiment's exact plan id matches the admitted plan and any typed
  support kind remains compatible through `PlanClaimBoundary`; broad theme or
  same-subplan sibling matches do not attach flow wording.
  `UserFacingPayloadSanitizer` uses the same plan-detail sanitizer for retained
  sanitized plan hypotheses and strategy-pack plan priorities/risk/long-term
  focus, so cached/internal carriers do not keep probe/test/candidate request
  wording after payload cleanup.
  Policy-local rival assessment tags (`secondary_plan:*`,
  `secondary_idea:*`, and `exact:*`) remain suppression/release-risk traces
  inside `PlayerFacingTruthModePolicy`; they are not expansion owner names,
  proof sources, proof families, or product row kinds.
  `RelationKind.Deferred` is currently empty; only implemented board-replayed
  descriptors can emit relation authority. Legacy motif-prefix, theme-keyword,
  canonical motif-term, and motif delta prose keep old relation-shaped labels
  generic or suppressed instead of treating them as catalog evidence. Legacy
  trapped-piece, domination, and zwischenzug text is lowered to generic
  piece-mobility, key-square restriction, or move-order caution wording unless
  the analyzer witness exists; legacy stalemate-trap and perpetual-check text is
  suppressed unless the matching replayed witness exists.
  Strategy-pack/structure-arc piece-activity evidence does not promote legacy
  trapped-piece activity into relation evidence. The final sanitizer removes
  legacy relation-shaped evidence terms from cached or legacy strategy-pack
  evidence lists. Legacy relation-shaped tags do not raise the generic context
  beat into high-tension tactical tone or pass as generic motif-prefix signals.
  Generic fact-corroboration helpers also block those tags before treating a
  motif as board-supported.
  `trapped_piece`, `domination`, `stalemate_trap`, `zwischenzug`, and
  `perpetual_check` are now implemented only through analyzer relation
  witnesses: trapped-piece requires no safe or defense-preserving reply route,
  domination requires a bound target reduced to at most one legal reply route,
  stalemate-trap requires the replayed move to leave the defender stalemated,
  zwischenzug requires a forcing check plus material payoff on the same line,
  and perpetual-check requires a repeated legal checking-position key.
- `StrategicSemanticObservationPipeline` emits typed semantic observations
  through minority-attack and a catalog-driven relation producer.
  `StrategicSemanticObservationContext` owns the normalized reviewed move, exact
  target extraction, one bounded top-PV replay, and one extracted
  relation-witness set; the relation producer consumes the implemented catalog
  set from that shared context instead of replaying or filtering locally.
  Replay-backed relation observations require the reviewed `playedMove` and do not
  infer it from the engine top line. Selector use of these observations does
  not create proof authority by itself. Unknown relation motifs have no
  descriptor, selector evidence, or frontend authority token.
  The selector may preserve one implemented structured relation candidate as a
  secondary idea when stronger non-relation family evidence would otherwise hide
  it from the player surface; this is support visibility only and still
  requires matching `relationSupport`.
  Implemented descriptors also own bounded public target fallback over
  relation-specific focus, keeping surface projection policy out of the payload
  builder. Any public relation carrier must preserve relation-specific focus
  before it can reach the public row, and selector merge does not synthesize
  relation focus or relation target from generic focus/target squares.
  `MoveReviewPlayerPayloadBuilder` admits a public relation row only from a
  selected implemented `relationKind` plus matching structured
  `relationSupport`; source/fact evidence strings and legacy target metadata are
  not row-admission inputs. `RelationSurfaceText` follows the same boundary and
  returns no player-row or summary-cue wording when matching structured support
  is absent. It may enrich support text with structured role, fork target-role,
  line-axis, absolute-pin, tactical-duty, king-pattern cycle, lure, and
  execution-route detail from that carrier, including bounded why/next-check
  move-review cues. Those relation cues lead the summary surface when present,
  while remaining support-only metadata.
  `StrategicIdeaEvidence` preserves relation identity and relation focus only
  for implemented catalog kinds; unknown relation names are stripped before
  selector candidates are built.
- `QuestionFirstCommentaryPlanner` ranks questions; it does not own proof
  authority.
- `FragmentAuthority` decides renderer release safety.
- `CommentaryApi` and frontend code consume typed payloads only.
  `ui/analyse/src/moveReview/playerSurfaceRendering.ts` renders decoded
  `moveReviewPlayerSurface` rows, including relation/target chips, without
  reconstructing relation authority from text.

## Maintained Scope

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality
- trigger hardening that removes generic string overlap, unknown-subtype
  fail-open, or blocked line-geometry without expanding public authority
- relation-witness hardening that centralizes legal replay plus attack/defense
  evidence in `MoveReviewExchangeAnalyzer`, routes semantic producer access
  through `StrategicSemanticObservationContext`, and keeps new motif rows
  selector/support-only until a proof contract admits them
- MoveReview player-surface relation metadata that exposes cataloged relation
  support as `strategic_relation` summary cues and advanced rows without
  creating proof authority
- docs and package cleanup that preserves the current authority boundary

Do not introduce rollout, history, or umbrella expansion labels as runtime
modules, proof families, public authority tokens, package names, or product row
kinds.

Color-complex has an explicit exact-board contract. It is closed to generic
`color_complex_squeeze` source packets and opens only through
`color_complex_squeeze_probe`, where the board proves that a friendly bishop or
knight attacks the opponent-owned semantic weak square and the packet branch is
proven/stable. The packet contract consumes a typed exact-slice proof, not
generic coordinate/minor-piece strings:

| proof family | proof source | status | certified | supported local | failure |
| --- | --- | --- | --- | --- | --- |
| `color_complex_squeeze` | `color_complex_squeeze_probe` | `Releasable` | true | true | `color_complex_authority_closed` |

## Verification Expectations

For package/naming/boundary work:

- compile and run targeted tests serially
- keep runtime and test/tooling boundaries separate
- update the relevant canonical doc in the same change
- report unverified work as boundary cleanup only

For claim authority or trust behavior:

- include claim/proof tests
- include planner tests when admission changes
- include renderer or parity tests when release text changes
- include color-complex tests when deferred authority is touched

## Maintenance Triggers

Re-audit the runtime docs only when:

- the user asks for a fresh audit
- code changed after the relevant snapshot in commentary runtime, controller,
  API, or analyse frontend paths
- a task introduces a runtime path not covered by the current SSoT

When changing the audited pipeline, update `CommentaryPipelineSSOT.md`.
When changing trust-relevant behavior, update `CommentaryTrustBoundary.md`.
When changing truth/signoff behavior, update `CommentaryTruthBoundary.md`.
