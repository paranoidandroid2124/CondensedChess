# Commentary Truth Gate

This document defines the canonical chess-truth signoff gate for Chesstory.

For program status, Step 1-7 closure, CQF track state, and document roles, see
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md).

It complements
[`CommentaryPipelineSSOT.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryPipelineSSOT.md)
by specifying how decisive-move truth is judged after the runtime pipeline
produces its surfaces.

Current program mode is maintenance-only. This file is policy, not frontier
status; use
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
and
[`CommentaryTrustHardening.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryTrustHardening.md)
for active status and re-review triggers.

## Core Rule

Truth comes before prose richness.

If Chesstory cannot verify a decisive claim, a concrete benchmark, or a
compensation story, it must become less specific instead of inventing a more
vivid explanation.

The current Step 4 / 5 authority slice reinforces that rule inside outline
assembly:

- `unsafe_as_lesson` opening-precedent fragments may not surface as released
  explanatory prose
- `unsafe_as_truth` annotation suffixes may not strengthen the released move
  claim by final-stitch append
- `unsafe_as_truth` generalized Context opening leads may not surface as
  released Context truth
- generalized `support_only` fragments may release only when they stay
  move-linked, scene-grounded, evidence-backed, planner-owned, or otherwise
  contract-consistent with the owning scene
- validator cleanup must still remove any leaked `Shared lesson:` sentence or
  raw helper-label prefix before render, and it may drop ungrounded
  generalized fallback families outside anchored / opening-theory scopes

## Canonical Signoff Order

1. Derive a `MoveTruthFrame` for every candidate key moment.
2. The frame must classify seven fact families before prose exists:
   - move quality
   - benchmark truth
   - tactical truth
   - material economics
   - strategic ownership
   - punishment / conversion
   - difficulty / novelty
3. Run the truth stack in three stages:
   - baseline truth pass
   - selective deep verification for truth-critical moves
   - chain synthesis for commitment / maintenance / conversion ownership
4. Canonical key-moment anchoring must rescue engine-severe CP / mate swings
   even when WPA saturation alone would miss them; if such a move survives only
   as a candidate bridge, canonical internal admission must still rescue it
   from runtime truth instead of leaving it witness-only, and decisive
   `blunder` / `missed_win` labels beat softer same-ply labels.
5. Project `DecisiveTruthContract` from the finalized `MoveTruthFrame`.
6. Resolve `TruthOwnershipRole` so commitment, maintenance, conversion, and
   blunder truth do not share the same decisive explanation.
7. Resolve `TruthVisibilityRole` so real exemplars can stay visible even when
   prose permission is too weak to let them overclaim.
8. Resolve `TruthSurfaceMode` so Chronicle / Bookmaker / Active know whether a
   row may explain investment, preserve it, explain conversion, explain
   failure, or stay neutral.
9. Resolve `TruthExemplarRole` so visible exemplar preservation is measured
   separately from ownership and separately from compensation-positive prose.
10. Promote decisive moments using truth and exemplar preservation, not only
   salience.
11. Realize Chronicle / Bookmaker / Active from that truth-bound state.
12. Allow optional wording polish only after truth-bound prose exists.
13. Judge release readiness with both automatic health checks and GM-style
   manual audit.

Automatic green checks alone do not imply chess-truth signoff.

## Primary Truth Metrics

The first-class automatic metrics are family-wise truth metrics, not prose-only
health metrics:

- verified-best move mislabeled count
- wrong benchmark naming count
- blunder softening count
- fake compensation count
- commitment missed count
- maintenance-as-commitment count
- conversion-as-compensation count
- visible exemplar lost count

The older release/parity metrics remain required, but they are secondary.

## Master-140 GM Audit

The required truth gate is the master-only 140-game corpus under the local
quality-audit manifest root.

This corpus is the mandatory manual audit gate for decisive-move truth.

Its merged signoff must preserve the canonical positive-exemplar gate carried in
the truth inventory even when some exemplar games live outside the main
140-game corpus. Exemplar coverage is judged against that audited gate, not
only against the intersection of exemplar keys with the current shard/game
list, and not only against compensation-positive prose permission.

Verified `WinningInvestment` / `CompensatedInvestment` moments stay eligible as
positive compensation exemplars during audit calibration when Game Arc and
Bookmaker agree on the same compensation contract. Legacy suppression rules may
still remove fake compensation, but they must not erase truth-bound investment
exemplars.

## Manual Outcome Labels

- `hard_fail`
  - decisive explanation is materially false
  - named benchmark is wrong
  - a blunder is softened into compensation or technical-neutral language
  - a verified-best played move is called inaccurate, loose, or blunder-like
- `partial_fail`
  - broad direction is roughly correct, but the decisive reason, punishment, or
    benchmark is underexplained or partly distorted
- `quiet_pass`
  - explanation is truthful but compact; no major decisive falsehood
- `strong_pass`
  - explanation identifies the real decisive move, why it matters, and the
    actual punishment / conversion route in player language

## Zero-Tolerance Catastrophic Families

The following are automatic truth-gate blockers:

- played move is verified best, but a surface calls it wrong / loose /
  inaccurate / blunder-like
- a surface names a benchmark move that is not the verified best move
- internal blunder truth is softened into compensation-positive Bookmaker or
  Active framing
- whole-game decisive shift / punishment story contradicts the decisive-truth
  contract
- a verified or provisional positive investment exemplar is missing from the
  focus-moment exemplar set
- a maintenance move is surfaced as if it were the original investment
- a conversion move is surfaced as compensation rather than conversion
- a restricted-defense conversion claim is surfaced as stable followthrough
  without direct best-defense branch proof plus same-branch persistence, or is
  revived from support-only carriers after certification failed closed
- a bounded counterplay-axis suppression claim is surfaced as whole-position
  `no-counterplay`, squeeze, or winning-route truth, without direct
  best-defense branch proof, or is revived from support-only carriers after
  axis certification failed closed
- a bounded dual-axis bind claim is surfaced without both axes individually
  carrying measurable burden, without direct best-defense proof, or with
  persistence / continuation stitched from a different defended branch
- compensation-positive prose appears on a move that does not own investment
  truth
- a low-confidence blunder is narrated as if the player had a verified
  strategic intent

## Health Signals vs Truth Gate

The following remain useful, but they are secondary:

- `crossSurfaceAgreementRate`
- `releaseGatePassed`
- parity metrics
- provider-none rerun stability

These are runtime health signals, not the final chess-truth gate.

For the same reason, `pathVsPayoffDivergenceCount` is only meaningful for
unresolved divergence. If display normalization has already resolved a path vs
payoff split by selecting a canonical subtype, that resolved split is not a
truth-gate failure and must not count as remaining divergence.

Historical bookmaker thesis snapshot tools are also non-authoritative:
`BookmakerProseGoldenTest`, `BookmakerProseGoldenDump`,
`BookmakerThesisQaRunner`, and `src/test/resources/bookmaker_thesis_goldens`
are stale regression artifacts, not chess-truth signoff gates.

## Quality-Audit Internal Scaffold

The parity-baseline lane and quality-rubric lane are allowed as internal
selectors, but they remain below the truth gate:

- same-ply parity taxonomy is diagnostic only
- digest mismatch vs replay rewrite classification does not change legality
- the internal LLM judge rubric (`clarity`, `move_attribution_correctness`,
  `contrast_usefulness`, `practical_usefulness`, `dry_but_true_penalty`,
  `overclaim_penalty`) is not a human-eval replacement
- the internal selector may zero `contrast_usefulness` and
  `practical_usefulness` credit when `move_attribution_correctness < 4`; that
  gating is subordinate to the truth gate and exists only to keep vivid but
  misattributed prose out of `keep`
- current internal keep threshold is `selectorScore >= 12` with
  `overclaim_penalty <= 1`
- no quality-audit metric may justify overclaim, unsupported benchmark naming,
  or owner
  revival outside the shared planner
- the only closed-by-design `surface_only_augmentation` tags are
  `active_no_primary_vs_chronicle_factual_fallback` and
  `active_attached_vs_chronicle_planner_owned`; anything else stays review-only
- the contrast-support lane is legal only as a support-slot replacement for
  Bookmaker / Chronicle signoff surfaces and for Active diagnostic traces on
  `WhyThis` / `WhyNow`; it may consume shared-planner `decisionComparison`,
  opponent-threat, prevented-resource, or decisive-truth inputs, but it may
  not mint a new owner or reopen DecisionTiming recovery
- contrast-support must stay traceable through
  `contrast_source_kind`, `contrast_anchor`, `contrast_consequence`,
  `contrast_admissible`, and `contrast_reject_reason`
- forbidden contrast-support sources remain:
  vague engine preference, generic `better was ...`, explanation-free eval
  gaps, raw `close_candidate`, and line-conditioned branches promoted to
  general prose
- rows that fall from planner-owned before-state to Bookmaker
  `afterBookmakerFallbackMode=exact_factual` after the rerun are truth-clean
  fail-closed rows, not contrast-support gain rows; the selector must classify
  them as
  `after_fallback_blocked`
- current real16 Bookmaker contrast-support evidence is signoff-ready, not
  rollout-ready:
  `contrastEligibleRows=40`, `eligibleKeepCount=9`, `eligibleRejectCount=31`,
  `afterFallbackCount=0`, `afterFallbackBlockedRows=6`, `degradedCount=0`, and
  `track1RegressionStatus=no_track1_regression`
- current real16 Chronicle contrast-support evidence is signoff-ready on the
  after-state planner-selected Why rows:
  `totalWhyRows=46`, `plannerOwnedRows=46`, `factualFallbackRows=0`,
  `primaryMismatchRows=0`, `ownerMismatchRows=0`
- same-key before/after Chronicle deltas on the original Why slice classify as
  upstream, not replay:
  `upstream_input_bundle_mismatch=2`,
  `intentional_suppression=4`,
  `replay_layer_bug=0`
- current real16 Active contrast-support evidence shows Active is
  diagnostic-only for
  this narrow experiment:
  on the after-state planner-selected Why rows,
  `totalWhyRows=46`, `attachedRows=1`, `composeRows=32`,
  `contrastRawRows=1`, `contrastSelectedRows=1`,
  `primaryMismatchRows=0`, `ownerMismatchRows=0`
- forcing Active to remain inside contrast-support scope leaves only
  diagnostic blocker
  counts:
  `validator_independence_fail_with_no_contrast=21`,
  `missing_required_plan_with_no_contrast=7`,
  `note_body_empty_from_support_duplicate_claim=3`,
  `legacy_preselection_blocked=14`
- because Active contributes only one contrast-bearing Why row on the current
  real16 shard, and that row already attaches, Active is cut from the
  contrast-support signoff scope until a later explicit surface-expansion lane
  reopens it
- remaining eligible-slice admissibility rejections are
  `missing_concrete_consequence=30` and `vague_engine_preference=1`;
  `question_outside_scope=0`
- current cross-surface contrast-support verdict is:
  Bookmaker `signoff-ready`, Chronicle `signoff-ready`,
  Active `scope-cut (diagnostic-only)`

Parity-baseline, quality-rubric, contrast-support, and quiet-support lane status
now lives in
[`CommentaryProgramMap.md`](C:/Codes/CondensedChess/lila-docker/repos/lila/modules/llm/docs/CommentaryProgramMap.md)
plus the local quality-audit rerun artifacts referenced there.

## Required Runtime Behavior

- Chronicle, Bookmaker, and Active must all consume the same decisive-truth
  contract for a given key moment.
  - for Active, `LlmApi.attachActiveStrategicNotes` must thread the backend-only
    per-ply contract sidecar into planner replay and support admissibility;
    validator attach gates may still omit the note, but they may not replace
    or reinterpret that contract
- Ownership, visibility, and prose permission are separate:
  - a move may be `supporting_visible` without owning the decisive
    explanation
  - a move may remain visible while `TruthSurfaceMode` is still `neutral`
  - visibility is not permission to emit compensation-positive prose
  - visibility is not itself ownership; `TruthExemplarRole` may be
    `provisional_exemplar` while ownership remains `none`
  - a truth-bound `compensation_maintenance` move stays `non_exemplar` in the
    public/runtime contract; a private maintenance-exemplar candidate may keep
    it focus-visible without becoming the commitment owner
  - public/runtime `maintenance_echo + supporting_visible +
    maintenance_preserve` is reserved for `critical maintenance`: verified
    payoff anchor, verified investment payoff, direct current semantic/carrier
    anchor match, and pressure evidence (`only_move`, `unique_good_move`,
    forcing proof, bad followthrough, or accepted current-semantic match)
  - routine maintenance may keep the internal truth phase while the public
    projection is forced back to `none / hidden / neutral`
  - a proof-backed best `only_move_defense` may still project
    `none / primary_visible / neutral`; this is a visible critical hold, not a
    failure owner and not a compensation owner
  - a benchmark-critical quiet hold without proof stays `hidden / neutral`
- fresh commitment seeding happens before any explicit `investedMaterial`
  carrier is required:
  - current-move material loss plus a move-local payoff anchor may produce a
    `provisional_exemplar`
  - that path may classify as `first_investment_commitment` even when the raw
    carrier has no `investedMaterial`
  - ownership remains stricter than visibility
- serialized/debug truth fields are audit aids only. Signoff-path runtime logic
  must not reconstruct ownership or surface permission from those strings when
  canonical `MoveTruthFrame` / `DecisiveTruthContract` data is available.
- when the canonical contract is absent, fallback projection must still fail
  closed:
  serialized `truthPhase`, `surfacedMoveOwnsTruth`, payoff-anchor/chain
  strings, and conversion-like `transitionType` tags may not recreate
  investment / maintenance / conversion ownership; only narrow raw
  `blunder` / `missed_win` failure classification may remain visible as an
  exact-factual residual.
- truth-first runtime semantics are mandatory when a contract exists:
  - raw `momentType`, `moveClassification`, `criticality`, and `choiceType`
    may remain in payloads for compatibility, display, or fallback only
  - raw `StrategyPackSurface` compensation flags may help extract carriers or
    support no-contract fallback, but they may not recreate compensation
    significance once the contract is present
  - whole-game binders, selector scoring, dossiers, and prose policies must use
    canonical truth semantics first and treat raw strings as fallback only
- Investment families must consume the same truth-phase ownership:
  - `first_investment_commitment`
  - `compensation_maintenance`
  - `conversion_followthrough`
- restricted-defense conversion followthrough is planner-gated:
  - only `AdvantageTransformation` strategic experiments with backend-only
    certification may remain `evidence_backed` for player-facing conversion
    followthrough
  - that certification must be based on bounded direct-reply evidence
    (`convert_reply_multipv` / `defense_reply_multipv`), compressed defender
    resources, concrete/stable best defense, future-snapshot persistence on
    that same defended branch, and counterplay suppression
  - PV paraphrase alone is not certification
  - cross-branch stitched persistence is not certification
  - uncertified or move-order-fragile conversion experiments must downgrade
    before Chronicle / Bookmaker / Active / whole-game replay, and support-only
    carriers may not re-inflate them afterward
- bounded counterplay-axis suppression is planner-gated:
  - only `RestrictionProphylaxis` strategic experiments with backend-only
    named-break / entry-axis certification may remain `evidence_backed` for
    player-facing prophylactic clamp / route-denial claims
  - the certified claim scope is local only:
    one named break or entry axis, not whole-position `no-counterplay`
  - certification must be based on the narrow B2b slice:
    late middlegame, clearly-better eval posture, route-denial or
    long-term-restraint validation, concrete direct best defense,
    future-snapshot persistence on that same defended branch, no hidden freeing
    break, no tactical release, and no move-order fragility
  - PV paraphrase, quiet waiting moves, or local restraint without measured
    counterplay compression are not certification
  - validation-only corroboration or cross-branch stitched persistence are not
    certification
  - certified suppression may stay only on the existing move-delta planner
    lane (`WhyThis`, and existing move-delta `WhatChanged` rules when they are
    otherwise legal); raw `prevented_plan` forcing shells may not override that
    certification into broader `WhyNow` / whole-position ownership
  - uncertified suppression experiments must downgrade before Chronicle /
    Bookmaker / Active / whole-game replay, and support-only carriers may not
    re-inflate them afterward
- bounded dual-axis bind is planner-gated too:
  - only `RestrictionProphylaxis` strategic experiments with backend-only
    dual-axis certification may remain `evidence_backed` for B3b player-facing
    bind claims
  - the certified claim scope is still local only:
    one named break axis plus one independent entry axis, not whole-position
    `no-counterplay`, global squeeze, or winning-route discovery
  - certification must be based on the narrow B3b slice:
    queen-light late middlegame, clearly-better eval posture, measurable
    `break suppression + entry denial` on both axes individually, concrete
    direct best defense, future-snapshot persistence plus bounded continuation
    on that same defended branch, no hidden freeing axis, no tactical release,
    no fortress-like static hold, and no move-order fragility
  - PV paraphrase, quiet waiting moves, static binds without bounded progress,
    mobility-cage/color-complex overreading, or route denial without
    independent-axis proof are not certification
  - validation-only corroboration may not stand in for direct best-defense
    evidence, and cross-branch stitched proof bundles are not certification
  - certified dual-axis bind may stay only on bounded planner-owned `WhyThis`;
    existing move-delta `WhatChanged` remains legal only when independently
    admissible, and B3b opens no new `WhyNow`, whole-position, or whole-game
    owner lane
  - uncertified dual-axis shells must downgrade before Chronicle / Bookmaker /
    Active / whole-game replay, and support-only carriers may not re-inflate
    them afterward
- bounded local file-entry bind is planner-gated too:
  - only `RestrictionProphylaxis` strategic experiments with backend-only
    local file-entry certification may remain `evidence_backed` for B4b
    player-facing file-entry bind claims
  - the certified claim scope is still local only:
    one denied file corridor plus one independent corroborating entry square,
    not whole-position `no-counterplay`, not a standalone square network, and
    not a heavy-piece clamp thesis
  - certification must be based on the narrow B4b slice:
    clearly-better late middlegame only, with an extra queen-light guard;
    `deniedEntryScope = file` plus a measured independent entry square,
    opponent-facing file usability loss rather than our file occupancy,
    concrete direct best defense, same-defended-branch file persistence plus
    same-defended-branch entry persistence, bounded continuation, and no
    off-file release, tactical release, fortress-like static hold,
    slight-edge posture, or move-order fragility; explicit hard-fail reasons
    include `entry_axis_persistence_missing`
  - same-defended-branch proof must now be replayable:
    `bestDefenseBranchKey` may come from `variationHash`, `seedId`, or an exact
    two-ply reply-line key, but not from first-move-only matching or
    candidate/probed-move fallback; ambiguous direct-reply branch sets must
    also fail closed instead of exporting a selected defended branch downstream
  - PV paraphrase, file occupancy alone, route pressure without actual file
    usability loss, or a corroborating square that simply restates the file
    axis are not certification
  - validation-only corroboration may not stand in for direct best-defense
    evidence, and cross-branch stitched proof bundles are not certification
  - certified local file-entry bind may stay only on bounded planner-owned
    `WhyThis` and bounded move-linked `WhatChanged`; B4b opens no new `WhyNow`,
    whole-position, Active-owner, or whole-game owner lane
  - uncertified local file-entry shells must downgrade before Chronicle /
    Bookmaker / Active / whole-game replay, and support-only or residual
    main-plan carriers may not re-inflate them afterward; runtime surface
    helpers must read experiment-filtered evidence-backed plans before they may
    reconstruct any file-entry claim, and they may use only affirmative plan
    text (`planName`, `executionSteps`) rather than negative fields such as
    `failureModes` or `refutation` when checking whether a file-entry pair was
    actually named
  - status interpretation:
    this bounded B4b slice is now `current bounded scope complete` only inside
    the current local charter; broader B4 family work, heavier postures, and
    wider owner/replay reuse remain closed until a new bounded charter is
    approved
- named route-network bind is now a narrower planner-only extension of that same
  lane:
  - only backend-only `NamedRouteNetworkBindCertification` may keep a B6b claim
    `evidence_backed`, and only when a certified B4 file-entry pair plus one
    non-redundant reroute denial survive the same defended branch under clear
    advantage in a late middlegame
  - same-defended-branch proof now requires replayable branch identity:
    `variationHash`, `seedId`, or a two-ply reply-line key; first-move-only
    branch fallback is not truth-clean, and missing / ambiguous branch identity
    must fail closed as `same_branch_identity_missing` or
    `ambiguous_defended_branch`
  - certification is stricter than B4, not a rename:
    `file_entry_restatement_only`, `route_network_mirage`,
    `same_branch_identity_missing`, `ambiguous_defended_branch`,
    `redundant_square_counting`, `untouched_sector_reroute`,
    `color_complex_escape`, `cross_branch_stitching`,
    `static_net_without_progress`, `engine_pv_paraphrase`,
    `move_order_fragility`, `heavy_piece_release_shell`, and
    `surface_reinflation` all hard-fail before any positive wording survives
  - the only positive owner scope is planner-owned `WhyThis`, keeping the
    existing move-delta owner lane and tagging the plan with
    `sourceKinds += named_route_network_bind`; planner `WhyThis` must consume
    only the exact certified file / entry / reroute triplet carried from the B6
    contract path, not a raw prevented-plan recomposition; `WhatChanged`,
    Chronicle, Bookmaker, Active, and whole-game replay remain closed to that
    tag
  - replay selection must fail closed on route-network planner plans:
    Chronicle / Bookmaker / Active may not reuse a
    `named_route_network_bind` primary, and whole-game / wrap-up remain outside
    the slice
  - close-review status:
    the exact FEN positive control, the same-first-move divergent /
    ambiguous-branch negatives, and the planner triplet-recomposition negative
    now pass together, so the bounded B6b slice is close-ready inside its
    narrow charter; broader B6 route-chain / replay / whole-position rollout
    remains closed
  - broader route-chain expansion on that same lane has now been narrowed back
    at the live owner boundary:
    the same backend-only helper may still inspect one exact intermediate node
    plus one downstream reroute candidate, but planner-owned truth is closed
    again for that broader slice because the helper-backed 2026-04-02 closeout
    rerun reproduced the after-move defended branch
    `a7a5 b4a5 c6a5 f3e5 ... a5c4` while still keeping `c4` reachable, and did
    not reproduce the earlier `a3b4` root-best reading on the original FEN
  - broader route-chain hard-fails must stay explicit:
    `fake_route_chain`, `redundant_intermediate_node`,
    `chain_only_on_nonbest_branch`, `untouched_sector_escape`,
    `posture_inflation`, `heavy_piece_route_shell`,
    `replay_reinflation`, `whole_game_wrapper_leak`, and
    `engine_pv_paraphrase` all block positive wording
  - positive owner scope does not widen:
    planner-owned `WhyThis` no longer consumes the carried intermediate route
    wording on the current branch. The exact `a5` detour witness remains
    backend-visible only until a future repair session adds a new exact-FEN
    root-best control and a real positive suite; `WhatChanged`, Chronicle,
    Bookmaker, Active, and whole-game / wrap-up all remain closed
  - current truth-gate status for route-chain closeout is `plateau / do not
    widen`:
    the exact FEN control still proves the backend after-trigger detour, but
    the helper-backed matrix rerun returned `c3b4`, not `a3b4`, as root best on
    the original FEN and no second independent exact-FEN survivor was found in
    the screened `K09*`, `B21A`, and `Rubinstein-Duras` pool. Treat the current
    branch as one-example heavy rather than closeout-ready
- heavy-piece local bind is now guarded only as a negative-first B5b slice:
  - only queen-on clearly-better late-middlegame
    `RestrictionProphylaxis -> file-entry reuse` shells are inspected by the
    backend-only `HeavyPieceLocalBindValidation` contract
  - heavy-piece release proof is exact-branch first:
    release inventory must come from replayable FEN-backed UCI branches, and
    best-defense survivors must stay pinned to PV1 / same-branch best replies;
    same-branch identity itself must come from a replayable two-ply defended-
    branch key rather than a one-move fragment or candidate/probed fallback,
    so illegal, paraphrased, wrong-base, short-fragment, or
    legal-prefix-plus-illegal-tail heavy-piece lines do not certify release
    claims by text motif alone
  - that contract does not certify a new positive thesis:
    it may only downgrade the experiment to `deferred` and block planner /
    move-delta / quiet-intent / replay reinflation when
    `heavy_piece_release_illusion`, `hidden_off_sector_break`,
    `pressure_only_waiting_move`, `direct_best_defense_missing`,
    `stitched_heavy_piece_bundle`, `move_order_fragility`,
    `fortress_like_but_not_progressing`, `engine_pv_paraphrase`, or
    `surface_reinflation` appears
  - B4 queen-light local file-entry certification remains the only live
    positive slice on this lane; B5b opens no heavy-piece positive wording,
    no new owner lane, and no public payload change
  - fixed-depth engine verification is now part of the negative-first audit:
    exact FEN fixtures must reproduce the full pinned PV1 path and its
    replay-derived feature set under the local UCI verifier before we treat
    the best-path anchor as reproduced, and the verifier should prefer an
    explicit configured engine binary over any local fallback
  - status interpretation:
    this bounded B5b containment slice is `current bounded scope complete`
    only inside the negative-first charter; maintenance-only watch applies to
    that containment slice only, while heavy-piece positive wording and broader
    B5/B6 frontier work stay deferred
- Bookmaker and Active may use compensation-positive language only when the
  contract allows compensation framing.
- fake compensation suppression and real investment exemplar preservation are
  both required. Passing one while failing the other is not truth-clean.
- only the truth-owning commitment move may explain why the investment works;
  maintenance preserves it and conversion cashes it out.
- current-move evidence and inherited shell are distinct:
  - `investedMaterial` or durable-pressure residue may keep a row visible
  - they may not, by themselves, certify a fresh commitment move
- provenance rules are explicit:
  - `current_material` may originate a fresh seed
  - `current_semantic` may verify and upgrade that seed into ownership
  - `current_semantic` also covers concrete current-move payoff-route
    carriers that keep a maintenance move focus-visible only when they directly
    match the verified payoff anchor; generic route/target scaffolding does not
    qualify
  - `after_semantic` may support maintenance or conversion continuity
  - `legacy_shell` may preserve visibility only
  - `after_semantic` and `legacy_shell` may not create fresh commitment
    ownership
- bad-move intent is separately gated:
  - `failure_intent` is internal and evidence-backed
  - move-local route / purpose / target evidence must directly match the
    verified payoff anchor before intent is allowed; bare square-access
    scaffolding is not enough
  - accepted current semantic support may keep intent alive only when it also
    directly matches the verified payoff anchor
  - `speculative_investment_failed` requires fresh current-move matched
    investment evidence; inherited maintenance shells alone may not trigger it
  - low-confidence or `no_clear_plan` failures may not keep route / target /
    plan carriers alive in sanitized commentary surfaces
  - commentary may describe tactical collapse or speculative failure, but it
    may not attribute a concrete player plan unless that plan survived the
    intent-confidence gate
  - sanitized commentary surfaces are release-gated as well as truth-gated:
    helper/debug notation, placeholder/meta leakage, broken fragments,
    duplicated sentences, and wrapper/truncation artifacts may not cross the
    user-facing payload boundary
  - sanitized commentary surfaces are also canonicalized before emission:
    exact duplicates, near-duplicate claim-family restatements, and
    whole-game wrapper restatements collapse to one canonical representative
  - sanitized commentary surfaces also obey one shared player-facing truth mode
    policy:
    - `Minimal` for quiet / weak-evidence moments; it suppresses synthetic
      strategic route / target narration instead of inventing a long story
    - `Tactical` only for contract-owned blunders, missed wins, proof-backed
      `only_move_defense`, explicit tactical refutations, or tactical
      sacrifices whose material imbalance settles inside the current search
      horizon; raw `momentType`, `criticality`, `choiceType`, or cp swing size
      alone may not force tactical narration
    - `only_move_defense` / `tactical_refutation` are not enough by
      themselves; player-facing tactical narration requires a shared forcing
      proof inside the current horizon before those classes may own the move
    - `Tactical` moments must lead with the immediate tactical truth; cited
      lines may survive only when they prove that tactical point
    - `Strategic` only when current semantics or verified benchmark evidence
      can back a concrete strategic claim; raw theme labels, legacy shells,
      weak support rows, and MultiPV alternatives alone may not promote a
      strategic story
    - strategic move commentary is move-delta-owned rather than state-only:
      a claim must be able to say what this move changed (`new_access`,
      `pressure_increase`, `exchange_forcing`, `counterplay_reduction`,
      `resource_removal`, or `plan_advance`)
    - route / target / concrete-square narration needs move-linked evidence;
      “concrete” wording alone is not a truth gate
    - route / pressure / exchange / prophylaxis wording is only admitted when
      that move-linked evidence is also delta-backed; existing board features
      alone do not own the current move's strategic explanation
    - sacrifice naming is truth-gated as well:
      tactical sacrifice requires immediate tactical settlement, strategic
      sacrifice requires verified compensation ownership, and unsupported
      sacrifice romance collapses to `Minimal`
  - support promotion is no longer canonical behavior:
    weak support may justify an admitted claim, but it may not be promoted into
    a stronger claim of its own
  - bookmaker / chronicle main-path move commentary now uses one shared
    ownership carrier (`MainPathMoveDeltaClaimBuilder`):
    tactical ownership is evaluated before strategic delta ownership, and
    strategic text may surface only after move-linked evidence survives
  - question-first outline composition is also bound to the same carrier
    boundary:
    `QuestionPlannerInputsBuilder` and `QuestionFirstCommentaryPlanner` may
    read only live move-owned carriers (`MainPathMoveDeltaClaimBuilder`,
    `QuietMoveIntentBuilder`, `CertifiedDecisionFrameBuilder`, sanitized
    `DecisionComparison`, current-board `preventedPlans`, `PVDelta`, threat
    tables, and exact factual move semantics); raw author-question text,
    latent carriers, and support-only shell prose may not own `DecisionPoint`
    claims
  - planner factual fallback reasons are diagnostic-only:
    if planner admission fails and only `exactFactualSentence` survives, the
    user-facing claim must stay at literal move-shape scope and may not turn
    ambiguous capture shape into generalized “simplifying” or exchange meaning;
    an empty author-question list may not rebuild decision/meta/close-candidate
    shell prose as a substitute owner
  - Bookmaker prose now consumes that same planner output directly:
    `primary.claim` is the only owner of the main Bookmaker claim,
    `secondary` is support-only, and planner admission failure or slot
    contract failure must fall straight to the exact factual one-liner
    instead of reviving `mainBundle` / `quietIntent` compression as a separate
    prose owner path
  - the Bookmaker-side contrast-support render guard is narrow and
    trace-preserving:
    when raw planner `primary` is `WhatMustBeStopped`, shared-planner
    `secondary` is `WhyNow`, both stay in `ForcingDefense`, and the raw primary
    contrast trace rejects with `question_outside_scope`, Bookmaker may render
    the `WhyNow` secondary for visible prose/contrast while keeping the raw
    planner trace unchanged; this prevents support-slot leakage without
    reopening owner choice or ranking
  - Chronicle contrast-support runtime is signoff-scoped and Active
    contrast-support runtime is diagnostic-only, both only at the support
    slot:
    Chronicle may replace only `primary.contrast`, and Active may replace only
    the selected support sentence in the deterministic note body, when
    `ContrastiveSupportAdmissibility` certifies the replacement from
    preexisting shared-planner/truth inputs; primary selection, owner family,
    and ranking stay unchanged
  - Chronicle moment prose now consumes that same planner output directly:
    Chronicle may only reorder planner top-2 locally, `primary` still owns the
    opening sentence, `secondary` is support-only, and planner admission or
    surface-composition failure must fall to compact factual prose or omission
    rather than reviving raw bridge text
  - Chronicle exact-factual fallback is claim-only:
    quiet-support diagnostics may still run for audit, but fallback rows may
    not append a support sentence once planner-owned surface composition failed
  - shared-planner scene-first admission is shared-planner owned:
    Chronicle / Bookmaker / Active may consume the same planner trace
    (`scene_type`, `owner_candidates`, `admitted_families`,
    `dropped_families`, `demotion_reasons`, `selected_question`,
    `selected_owner_family`, `selected_owner_source`) but they may not mint a
    new owner family locally from that trace
  - shared-planner admission is now live in v1:
    every normalized candidate carries `admission_decision`,
    `admission_reason`, and optional `demoted_to`, and ranking only sees
    `PrimaryAllowed`
  - shadow normalization must keep raw-domain material visibly separate from
    move-linked owner candidates:
    raw close alternatives, opening precedent summaries, and endgame
    theoretical hints may be traced with a proposed family mapping, but they
    stay `support_material=true` until shared-planner admission explicitly
    legalizes them
  - move-linked `OpeningRelation` / `EndgameTransition` translators may own
    only planner-admitted domain questions in their own scenes:
    `OpeningRelation` may surface through planner-owned `WhyThis`,
    `EndgameTransition` may surface through planner-owned `WhatChanged`, and
    raw opening/endgame support text never becomes a direct owner
  - `DecisionTiming` trace must preserve source and materiality before
    admission:
    `decision_comparison`, `close_candidate`, `prevented_resource`, and
    `only_move` timing sources are distinct inputs, and
    `decision_comparison` is now further split into
    `concrete_reply_or_reason` vs `bare_engine_gap`; raw close alternatives
    remain support material even when a concrete timing-owner path also exists
  - scene-hint precedence is now trace-first
    `tactical_failure > plan_clash > forcing_defense > transition_conversion >
    opening_relation > endgame_transition > quiet_improvement`
    so `plan_clash` is no longer shadow-hidden behind a concurrent
    forcing-defense trace
  - when opening and endgame translators both survive as move-linked
    candidates, shared-planner trace prefers the common
    `transition_conversion` prior if a move-local transition anchor is also
    present; this avoids arbitrary domain precedence while keeping tactical /
    race / forcing scenes above it
  - v1 legality is intentionally conservative:
    `support_material` never becomes a primary owner,
    `DecisionTiming(close_candidate)` is always `SupportOnly`,
    `DecisionTiming` is kept out of the primary pool,
    `PlanRace` is primary only in `plan_clash`,
    and `MoveDelta` is primary only in
    `quiet_improvement | transition_conversion`
  - `WhyNow` timing ownership therefore remains strict after planner-first
    uplift:
    concrete timing loss may still be traced, including the
    `concrete_reply_or_reason` decision-comparison subtype, but timing-only
    `WhyNow` / `WhatChanged` plans are filtered from the legal pool until a
    later admission phase explicitly widens them
  - demotion must close on a real planner path:
    if `WhyNow`, `WhatMustBeStopped`, or `WhosePlanIsFaster` demotes into a
    legal `WhyThis` fallback, that fallback remains planner-owned; if no legal
    fallback can be synthesized, the planner records an explicit suppressed
    intentional drop rather than carrying a dead-end demotion forward
  - Chronicle / Active replay may inspect the carried `DecisionComparison`
    trace, but replay may not promote generic urgency, unnamed drift language,
    or raw domain summaries into a timing owner
  - no-contract truth fallback is failure-only:
    raw investment / maintenance / conversion shells in serialized fields may
    not be read back as owner-bearing truth once the canonical contract is
    missing
  - Chronicle / Active replay may reuse carried
    `openingRelationClaim` / `endgameTransitionClaim` digest fields only to
    reconstruct the same already-legal planner result; replay may not mint a
    new owner family or re-promote support-only domain text
  - `WhatChanged` move ownership remains separate from state summary:
    move-attributed `preventedPlans`, counterplay-window removal, or concrete
    decision-comparison balance shift may own `WhatChanged`, but a bare
    structural summary still fails closed as `state_truth_only`
  - Bookmaker race framing remains especially strict:
    `WhosePlanIsFaster` may surface only after certified intent, certified
    battlefront, and a concrete timing anchor survive planner admission; a
    generic race shell is not a player-facing owner even if support rows exist
  - main-path ownership is scope-aware:
    `MoveLocal` claims may own the main sentence; `LineScoped` claims may
    survive only as explicit subordinate lines, and strategic line-scoped
    consequences are not allowed to stand alone without a move-local strategic
    claim
  - user-facing state-summary thesis / fallback revival is not canonical:
    old `StrategicThesisBuilder`-style main-path promotion may not recreate a
    Bookmaker / Chronicle claim once tactical / strategic ownership failed
  - runtime also removed legacy thesis carriers outside the main sentence
    path:
    `dominant thesis:` / `dominant_thesis:` enrichment and hybrid thesis
    bridge helpers are no longer canonical truth inputs for outline,
    strategy-pack, full-game evidence, or bookmaker-ledger consumers
  - `ExchangeForcing` / `ResourceRemoval` require current-move causal proof:
    a thematic exchange, a generic pressure map, or a vague defensive easing
    is insufficient; runtime needs prevented-resource identity, or a proving
    line that is backed by move-linked exchange evidence that this move
    actually made that exchange / resource change more forcing or unavailable;
    a direct capture on the anchor square does not qualify as
    `ExchangeForcing` by itself
  - quiet recovery is narrow and move-local:
    after tactical and strategic ownership fail, runtime may emit only a
    canonical quiet-intent one-liner (`piece_improvement`, `king_safety`,
    `counterplay_restraint`, or `technical_conversion_step`), and only from
    exact move semantics, explicit `preventedPlans`, or move-linked engine
    evidence
  - theme names, shell names, state-only structure summaries, and generic
    route / target possibility do not own quiet recovery
  - user-facing strategic plan prose is probe-backed only:
    validated supportive probe results are required before a strategic plan
    may own user-facing runtime prose; `PlayablePvCoupled`, `Deferred`, and
    structural-only escalations remain diagnostic-only
  - latent / pv-coupled / deferred ideas are not runtime carriers:
    `latentPlans`, `whyAbsentFromTopMultiPV`, and signal-digest latent hints
    are absent from user-facing runtime payloads and surfaced Chronicle moment
    payloads; their structured diagnostics live only in local raw/debug sidecars
  - latent / hold-reason branches are also removed from upstream narrative
    assembly:
    runtime outline/debug/main-moment builders may not derive strategic prose
    or semantic keys from `latentPlans` / `whyAbsentFromTopMultiPV`
  - Chronicle-side strategic metadata is not allowed to reintroduce latent
    ideas:
    unbacked moment concepts, response themes, thread summaries/counterplans,
    and signal-digest strategic idea / opponent-plan hints are cleared by the
    user-facing sanitizer
  - direct Bookmaker requests do not surface blank prose:
    if tactical / strategic / quiet-intent all fail, runtime may still emit a
    final exact factual one-liner from current move semantics
    - that fallback floor must stay literal:
      when target-square anchoring is absent, capture fallback may say only
      `This captures.` and may not infer simplification / exchange semantics
  - direct Bookmaker requests also do not surface half-admitted planner prose:
    if planner-owned slots lose their support/evidence owner or fail the hard
    gate, runtime must still fall back to the exact factual one-liner rather
    than keeping a bare certified shell
  - surfaced Chronicle moments also do not surface blank prose:
    after rendering, `CommentaryEngine` performs a post-selection
    commentability pass that reselects the next commentable move in the same
    thread or drops the surfaced move when no replacement exists
  - if an active strategic note does not survive that release gate, the note
    prose itself and its aligned side surfaces are omitted instead of rendering
    an empty strategic note surface
  - if an active strategic note survives the release gate but only restates the
    main prose, it is omitted as non-distinct user-facing content
  - if a moment resolves to `Minimal`, active-note user-facing surfaces stay
    omitted even when the selector retained the moment internally
  - Active note ownership is planner-first:
    the note body may come only from one planner-approved `primary` question,
    `secondary` is support-only, and `WhosePlanIsFaster` may never own the note
    body
  - planner-owned `WhyNow` is the only Active exception that may survive note
    selection without legacy `strategicBranch` preselection or visible
    route/move support, and even then the note must still pass the same hard
    gate with a concrete timing anchor
  - planner-approved `TacticalFailure`, `MoveDelta`, `OpeningRelation`, and
    `EndgameTransition` notes use a planner-aware minimum contract at attach
    time:
    the note must preserve the selected claim plus one anchored support or
    consequence, but it does not need to satisfy the old full coaching-card
    completeness contract
  - planner-owned `WhyNow` may reuse a carried proof sentence only when the
    note's lead is a new, concrete timing claim; if the lead still reuses the
    prior shell or the support line is the only surviving content, the Active
    note fails closed
  - attach gating for those planner-approved minimal-contract notes is note
    first, not side-surface first:
    missing route / plan / focus / execution completeness becomes warning-level
    side incompleteness (`side_route_missing`, `side_plan_missing`,
    `side_focus_missing`, `side_execution_or_objective_missing`,
    `side_coverage_low`) rather than a hard attach blocker
  - Active finalization must collapse attach failure to one mutually exclusive
    user-facing reject reason (`no_planner_primary`, `preselection_blocked`,
    `visible_support_missing`, `note_body_empty`,
    `validator_independence_fail`, `missing_required_route`,
    `missing_required_plan`, `missing_required_focus`,
    `note_body_too_generic`) even if lower-level validator details remain in
    debug traces
  - whole-game Chronicle carry must keep planner questions aligned with carried
    author-evidence rows:
    runtime may not keep an evidence row for `WhyNow` / `WhatChanged` while
    silently dropping the matching question summary and then claim that the
    planner had no admissible question to surface
  - active/support strategic narration is move-delta-owned at the carrier
    level as well:
    `PlayerFacingMoveDeltaBuilder` is the canonical runtime source for active
    dossier / route / move / target survival, and builder-level reads of raw
    state-summary fields (`structuralCue`, `dominantIdeaFocus`,
    `deploymentPurpose`, `deploymentContribution`, `opponentPlan`,
    `prophylaxisThreat`, raw `longTermFocus`, `executionText/objectiveText/
    focusText`) do not own player-facing strategic note prose
  - generic active/support sentence families such as `The key idea is ...`,
    `A likely follow-up is ...`, `A concrete target is ...`, and `Further
    probe work still targets ...` are not truth-owning active-note output
  - note survival is not enough to restore strategy side-panels:
    active-support dossier / route / move / target payloads must survive the
    same planner-selected decision frame, and the frontend may not rebuild
    those surfaces from `strategyPack` fallback once they were omitted upstream
  - frontend decision-comparison support is canonical-digest only:
    `signalDigest.decisionComparison` may render when present, but
    `topEngineMove` or other fallback-only engine-alternative fields may not be
    rebuilt into a user-facing decision-comparison support box when that
    canonical digest is absent
  - conversely, side-surface incompleteness may not kill a planner-valid note:
    note attach is decided from the validated note contract first, and side
    surfaces stay best-effort on that same selected planner result
  - conversely, note omission also removes those aligned side surfaces:
    there is no canonical `carrier_only` active-note payload mode anymore
  - draw / repetition / perpetual-style whole-game summaries may not invent a
    decisive shift, punishment story, or conversion route unless a concrete
    truth-owning anchor exists
  - decisive result alone is not enough for whole-game payoff prose; decisive
    shift / punishment wrappers still require a truth-owning anchor or
    verified payoff / conversion ownership
  - weak-evidence fallback may omit prose entirely rather than emitting a
    stronger-looking but unsupported tactical / strategic claim
  - proving lines may survive only when they prove the tactical truth or the
    admitted strategic delta; boilerplate keep-the-idea-alive citations are not
    truth-owning evidence
- tactical tension is also canonicalized:
  - forced / critical / tactical-pressure checks must come from one shared
    truth-first policy
  - when the contract exists, `reasonFamily`, `failureMode`, benchmark
    pressure, and intent-confidence gating outrank raw `criticality` /
    `choiceType` strings
  - raw tension strings may remain fallback inputs only when no decisive-truth
    contract is available
- selector/runtime significance is also canonicalized:
  - selector control flow is split:
    thread-local representative replacement may use hidden tactical /
    technical evidence, but global surfaced visible / active-note admission may
    consume only protected or globally eligible rows
  - threaded representative selection must be truth-aware and now flows
    through a single canonical picker per thread: strong visible /
    failure-significant candidates first, then seed/build/finisher stage
    coverage, then existing ply order
  - hidden `benchmarkCriticalMove` rows are diagnostic/thread-local only; they
    do not earn global visible-slot or active-note protection on their own, and
    quiet benchmark-critical holds may only replace thread shells when the
    thread lacks stronger truth-visible/failure representatives
  - representative ordering remains fixed-cap and truth-first, while protected
    visible selection and the base active-note protected pass now share the
    same canonical protected ordering:
    severe failures first, then one threaded representative promoted
    `Best OnlyMoveDefense` occupant per thread, then non-threaded promoted
    holds, then remaining same-thread promoted duplicates, then
    verified/provisional exemplar and commitment/conversion ownership
  - selector assembly computes that canonical protected stream, visible merge,
    and base active-note protected seed once in a shared core path; runtime
    truth tracing now records only canonical survival outcomes from that path:
    final-internal / visible / active / whole-game promotion plus truth class,
    reason family, surfaced thread id, representative selection, and the
    private maintenance-exemplar candidate bit
  - the protected pass reserves visible / active-note space first, but the
    selector must still backfill remaining visible slots with truth-eligible
    fallback moments instead of collapsing the visible set to the protected
    family only
  - inside the fixed visible `12`, severe failures remain absolute first, and
    the remaining protected visible seats follow that shared canonical
    protected ordering
  - when a protected visible seat already occupies a thread, the selector may
    not spend an additional same-thread `visibleThread` seat on a non-protected
    representative from that thread
  - thread occupancy / same-thread dedupe in protected visible and active-note
    overflow is keyed only by surfaced strategic threads from the audited
    thread builder; raw `activePlan.subplanId` tags that do not survive into a
    surfaced thread may not act as hidden thread ids
  - below the protected and surfaced-thread layers, visible selection now uses
    one unified non-protected stream instead of separate
    compensation/core/fallback ladders; active-note selection mirrors the same
    three-layer shape (`protected base -> thread note -> non-protected stream`)
  - that unified non-protected stream still keys “threaded” only from those
    surfaced thread refs; a fourth-or-lower strategic thread that misses the
    `MaxThreads` cut may still compete through generic visible selection
    instead of being hidden by pre-cap thread tagging
  - generic hidden best tactical/technical moves may help same-thread
    replacement, but they may not outrank those protected families in the
    global visible / active-note caps
  - active-note overflow is permitted only for `Blunder`, `MissedWin`, and
    promoted `Best OnlyMoveDefense`; exemplar/owner families remain inside the
    base `8`, and overflow must still dedupe repeated notes by thread / chain
  - investment-chain dedupe keeps one supporting-visible move per chain by
    default, but a second support is allowed for a private
    maintenance-exemplar candidate or a non-best / failure-significant follow-up
- Concrete benchmark naming is allowed only when the verified best move exists
  and the contract explicitly allows it.
- Whole-game Chronicle binders must anchor decisive shift / punishment prose in
  the same decisive-truth contract, or omit those sentences.
- Whole-game support reuse may consume only authority-cleared projected support
  carriers or verified payoff anchors; raw helper-labeled carriers are not
  admissible whole-game truth owners.
- `support_only` whole-game carriers may remain support, but they may not seed
  decisive-shift / payoff wrappers; those wrappers require verified payoff
  anchors or equivalent structured proof inside the same contract.
