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
  motif-local parsers. A relation witness with non-empty typed details must
  match its relation kind before semantic emission, and semantic consumers cross
  that boundary through the analyzer-owned relation projection rather than raw
  witness fields. Defender/bad-piece owner seed and transition terms are
  expanded through the analyzer relation projection, and policy continuation
  terms consume those analyzer-built structure terms instead of rebuilding
  relation markers. Branch-key and `branch:*` fact formatting is
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
  `PlanTaxonomy.ThemeResolver`, including embedded subplan annotations on
  probe plan names, user-facing stripping of those annotations, and embedded
  theme preconditions, not planner- or detector-local prefix slicing.
  Taxonomy-backed proof contract ids use the same resolver boundary.
  Structural state and latent-seed evidence tags use the same resolver
  boundary.
  Policy-local rival assessment tags (`secondary_plan:*`,
  `secondary_idea:*`, and `exact:*`) remain suppression/release-risk traces
  inside `PlayerFacingTruthModePolicy`; they are not expansion owner names,
  proof sources, proof families, or product row kinds.
  Deferred relation motifs stay in the same
  catalog inventory with required witness and fallback-lane metadata, projected
  through a catalog-owned `DeferredRelationFallback` read-model, but only
  implemented board-replayed descriptors can emit relation authority. Legacy
  motif-prefix, theme-keyword, canonical motif-term, and motif delta prose
  consume that fallback projection for softer non-relation text or
  diagnostic-only suppression. Legacy plan evidence also uses the fallback
  label for deferred domination. User-facing helper notation for deferred
  relations is rewritten or suppressed through the same catalog fallback, and
  threat-summary labels consume the same fallback instead of raw deferred motif
  names.
  Strategy-pack/structure-arc piece-activity evidence uses the catalog fallback
  evidence term for trapped-piece activity.
  The final sanitizer removes deferred relation motif terms from cached or
  legacy strategy-pack evidence lists while preserving the fallback term.
  Deferred tags do not raise the generic context beat into high-tension
  tactical tone or pass as generic motif-prefix signals. Generic
  fact-corroboration helpers also check the deferred relation catalog before
  treating a motif as board-supported.
- `StrategicSemanticObservationPipeline` emits typed semantic observations
  through minority-attack and a catalog-driven relation producer.
  `StrategicSemanticObservationContext` owns the normalized reviewed move, exact
  target extraction, one bounded top-PV replay, and one extracted
  relation-witness set; the relation producer consumes the implemented catalog
  set from that shared context instead of replaying or filtering locally.
  Replay-backed relation observations require the reviewed `playedMove` and do not
  infer it from the engine top line. Selector use of these observations does
  not create proof authority by itself. Deferred relation motifs stay in the
  same catalog inventory as non-public rows with required witness shape and
  defer reason; they have no implemented descriptor, selector evidence, or
  frontend authority token until a board-replayed witness graduates them.
  Implemented descriptors also own bounded public target fallback over
  relation-specific focus, keeping surface projection policy out of the payload
  builder. Any public relation carrier must preserve relation-specific focus
  before it can reach the public row, and selector merge does not synthesize
  relation focus or relation target from generic focus/target squares; legacy
  carriers without a selected relation kind must also have exactly one matching
  catalog source/fact pair and cannot promote generic `targetSquare` metadata as
  analyzer target.
  `StrategicIdeaEvidence` preserves relation identity and relation focus only
  for implemented catalog kinds; deferred or unknown relation names are stripped
  before selector candidates are built.
- `QuestionFirstCommentaryPlanner` ranks questions; it does not own proof
  authority.
- `FragmentAuthority` decides renderer release safety.
- `CommentaryApi` and frontend code consume typed payloads only.

## Maintained Scope

Open for maintenance:

- exact-board promoted slices already covered by proof contracts
- source/test tooling that improves exact witness quality
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
