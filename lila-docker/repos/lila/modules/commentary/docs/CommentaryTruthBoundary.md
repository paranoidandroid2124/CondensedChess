# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Removed product
surfaces and historical diagnostics are isolated to
`modules/commentaryTools/src/test` and may not participate in released truth
signoff unless a current MoveReview consumer and runtime audit explicitly
reopen that boundary. Runtime truth projection must not accept retired replay
DTOs, note payloads, or branch dossiers as signoff inputs; released truth paths
consume the current MoveReview/arc runtime models and proof-contract
projections.

Future `SupportedLocal` or scoped-takeaway expansion must enter through
MoveReview first and must preserve the audited payload contract.

## Truth Stack

The runtime must resolve truth in this order:

1. Build `MoveTruthFrame` before prose exists.
2. Classify move quality, benchmark truth, tactics, material economics,
   strategic ownership, punishment/conversion, and difficulty/novelty.
3. Run selective deep verification for truth-critical moves.
4. Synthesize commitment, maintenance, and conversion chains.
5. Project `DecisiveTruthContract`.
6. Resolve ownership, visibility, surface mode, and exemplar role.
7. Admit claims only through the claim-authority kernel and proof contracts.
8. Render and validate prose after truth and authority are fixed.

Serialized/debug fields may aid audit, but they may not recreate ownership when
canonical truth data is available.

## Authority And Truth Interaction

`CertifiedOwner` and `SupportedLocal` are authority decisions, not chess-truth
shortcuts. They are legal only when the current board truth, proof contract,
and tactical veto checks agree.

`DiagnosticOnly`, `Suppressed`, support-only, deferred, latent, and fallback
carriers may not emit owner claims. They may remain in internal reports or
debug traces.

`ReplayBackedInternal` line-consequence evidence is not a truth-owner
promotion. It means the PV was legally replayed for bounded narrative or ledger
use, but it cannot emit player decision-strip claims unless the line also comes
from `MoveReviewRefs` as a `SurfaceCandidate`. A replay-observed central pawn
advance without `CentralBreakTimingWitness.exact` is only a local line
transition, not the `central_break_timing` proof family.
Likewise, a replayed exchange can support `ExchangeSequence` or
`MaterialTransition` wording without becoming defender-trade or bad-piece
liquidation truth. Those favorable-exchange owner paths require their own
board-backed witness: real defender-target relation removal for defender trade,
and current-board bishop constraint plus legal capture/recapture for bad-piece
liquidation.
`overload`, `deflection`, `discovered_attack`, `double_check`,
`back_rank_mate`, `mate_net`, `greek_gift`, `fork`, `hanging_piece`,
`trapped_piece`, `domination`, `stalemate_trap`, `perpetual_check`,
`zwischenzug`, `decoy`, `xray`, `clearance`, `battery`, `pin`, `skewer`, and
`interference` may speak as bounded `SupportedLocal` relation-transformation
claims only when the board/PV analyzer witness matches the selected structured
`StrategyRelationSupport`, carries the matching typed
`PlayerFacingExactSliceProof`, and survives the proven/stable branch gate.
King/mate/draw/pattern checks consume the exact analyzer witness and structured
PV continuation lines where needed; no target prose, helper name, evidence ref,
or fact string can substitute. They remain support authority, not certified
owner truth or lesson authority.

Strategic plan refutation must be exact-board/probe truth. Sibling plan ranking
or alternative-dominance metadata can demote selection, but it is not a chess
refutation and must not be reported as `Refuted`.

Strategic plan promotion and demotion must be consumed through the typed
`StrategicPlanEvidenceView` derived from `PlanEvidenceEvaluator`. Raw
`StrategicPlanExperiment.evidenceTier` strings, compatibility markers, and
subplan ids cannot by themselves certify truth, refutation, provenance,
quantifier, stability, or exact-family ownership.
`PlanSemanticsContract` may permit only the semantic shape of a plan claim,
such as strategic idea-kind mapping, subplan probe-purpose routing, or thematic
fallback eligibility. It does not certify board truth, refutation, exact proof
family, provenance, quantifier, stability, or owner authority.
`PlanClaimBoundary.PlanProposal` records where plan theme/subplan meaning came
from. Explicit subplans and typed support tags can become support candidates;
broad aliases, plan names, and default subplan inference remain proposal-only
and cannot certify truth or user-facing ownership. `PlanSupport`,
`AdmittedPlanClaim`, and `RenderedPlanText` name the intended downstream stages,
but exact board truth still requires evaluator/probe/replay evidence at the
admission boundary.
This applies before producer and consumer code can create downstream authority:
`PlanMatch`, `ProbeRequest`, `PlanContinuity`, `LatentSeed`, plan rows,
experiments, and hypotheses must pass through `PlanClaimBoundary.PlanProposal`
before their plan meaning is consumed. `PlanProposalEngine` must not promote a
broad theme or seed alias into a default subplan or L2 subplan variant without
typed support. Probe display names are not truth carriers; live probe requests
carry contract metadata in purpose/objective/required-signals/horizon fields
and stable plan binding, not in parsable `planName` prose.
Board-backed plan candidate helpers can improve the support candidates for
weak PlanKind families, but they do not certify strategic truth on their own.
`PieceRedeploymentEvidence`, `AdvantageTransformationEvidence`,
`FlankInfrastructureEvidence`, `PawnBreakEvidence`,
`RestrictionPlanEvidence`, and `WeaknessFixationEvidence`, plus
`FavorableExchangeEvidence`, may propose or probe legal moves for piece
non-pawn/non-king piece reroutes, rook-file transfer only after an actual file
change, active-queen centralization excluded from worst-piece improvement
unless it is a low-activity recovery, open-file pressure
against non-king file targets, passed-pawn conversion/manufacture, simplification-conversion,
invasion with check or a non-king resource target, opposite-bishops conversion, aligned/hook-backed rook-pawn or hook infrastructure,
rook-lift scaffolds with enemy king-file pressure, check, or a non-king
resource target,
concrete pawn breaks or tension-maintaining quiet moves that support the
preserved tension by castling, minor-piece centralization/mobility gain, or a
major piece on a tension file, board-measured
specific restriction/space-clamp candidates, quiet mobility-suppression candidates,
`WeaknessTargetProfile`-anchored static/backward/IQP targets,
semantic-ready minority-attack prep/break support moves, and bounded defended
queen-vs-queen exchange/defender-trade/bad-piece/bounded recapture-like
simplification-window captures. Large tactical material wins do not become
`simplification_window` support through this helper. Shared pawn/target/material
primitives for these helpers live in
`PlanMoveEvidenceSupport`; helper-local prose or duplicated predicates are not
truth inputs. Their candidates must still pass probe/evaluator admission before
becoming user-facing plan claims, and exact-family rows such as
product-visible central-break or exact target-fixation truth still require
their separate exact witness. Favorable-exchange candidates do not replace the
existing defender-trade, queen-trade, simplification, or bad-piece-liquidation
proof witnesses.
Move-level restriction candidates are not also re-emitted as generic
`prophylaxis_restraint`; broad prophylaxis needs structural/seed support or the
separate exact denied-resource proof path. `wing_break_timing` remains
`pawn_break_preparation` support rather than a flank-infrastructure truth
projection.
`queen_trade_shield` support requires a queen-vs-queen capture with a
non-king recapture-like defender; a minor-piece queen win or undefended queen
capture is not queen-trade truth. Helper-owned specific long-plan subplans are
not created from `PlanProposalEngine` broad structural booleans alone.
Minority-attack fixation support from `WeaknessFixationEvidence` remains
support-only unless the separate Carlsbad fixed-target exact-slice proof path
admits it.
The `generic_center_plan` structural state is not a central-break truth source;
it stays a theme-level hint unless a concrete `PawnBreakEvidence` candidate or
exact central-break witness exists.
`PlanEvidenceEvaluator.planSupport` and `admittedPlanClaim` are the runtime
projection points from evaluated evidence into `PlanSupport` and
`AdmittedPlanClaim`; downstream user-facing consumers should prefer
`StrategicPlanEvidenceView.mainAdmittedClaims` when they need admitted plan
meaning rather than diagnostic/evaluated-plan metadata.
For MoveReview promoted-plan rows, `MoveReviewPlayerPayloadBuilder` creates
`RenderedPlanText` only after `admittedPlanClaim`; rendered strings are not
parsed back into chess or plan meaning. The builder also filters internal
diagnostic/probe/test/candidate-request wording out of promoted-plan and
practical-plan detail rows through
`UserFacingSignalSanitizer.sanitizePlanDetail`. Fallback wording may come only
from the typed plan contract objective or admitted plan name after a non-empty
detail field was suppressed; fallback text does not certify board truth, and an
empty detail field does not authorize a new detail row. The same builder uses only
`PlanProposal.fallbackTheme` and `supportKind` for player-surface plan-family
gates such as weakness-target practical rows, prophylaxis rows, and
promoted/practical sibling suppression; inferred themes from plan names,
execution steps, or failure-mode prose remain proposal hints, not truth or
surface authority.
`UserFacingPayloadSanitizer` applies the same plan-detail filter to retained
sanitized plan hypotheses and strategy-pack plan priorities/risk/long-term
focus. This cleanup does not certify the plan; it only prevents support/probe
request diagnostics from surviving as user-facing strategic detail.
Board-bound probe truth requires the returned probe certificate to echo the
requested FEN and probed move; missing or malformed values are not truth. Both
echoed FEN strings must parse and echoed moves must be valid UCI. For
multi-move requests, the certified move must be one of the requested moves.
Unknown request purposes have no truth contract and cannot be rescued by
explicit signal names or by the result's own purpose. Unknown-purpose probes
also cannot be treated as refutations through the default cp-loss bound.
Refutation probes are negative evidence only: if they do not refute a plan,
they clear that specific punishment path but do not certify the plan as true.

Exact-family strategic truth remains witness-bound. A plan taxonomy label such
as `central_break_timing` does not certify the exact central-break proof family
unless the exact board witness is present. A product surface row is allowed
only after that exact witness and a `SupportedLocal` packet both survive the
runtime boundary; this does not make the family `CertifiedOwner`. For this
SupportedLocal row, equal or near-equal PV scores and missing two-move branch
keys do not by themselves refute the witness; the required truth is the legal
non-capturing same-file d/e pawn advance to `d4`, `e4`, `d5`, or `e5`, plus a
board-backed link from the played move. The played move itself can be that
exact witness when it legally makes the same central break; top-PV omission is
not by itself a truth failure for this bounded support row. Diagonal captures
and prep/challenge pawn moves, including Sicilian c-pawn and King's Gambit
f-pawn opening breaks, are not truth for the product-visible `Central break`
row.
Central-break runtime truth is generic board/PV truth only. Historical source
rows may remain in test/tooling precedent catalogs, but runtime witness
construction must not stamp source-specific FEN markers into `sourceTags`,
owner seeds, or release terms.

Opening-family player-surface truth is bounded support only.
`OpeningFamilyClaimResolver` may return `SupportedLocal` only for a structured
catalog family wire key whose opening label and static `OpeningNameLookup` FEN
result both match the same family inside the opening proof window. The FEN
result may match through a same-EPD static book alias retained for
transposition-aware canonicalization, but not through board-shape predicates,
strategic transposition proofs, or rendered prose.
`MoveReviewPlayerPayloadBuilder` may emit an `Opening family` support row only
after that resolver decision; label-only opening data, generic opening phase, raw
rendered prose, or FEN lookup without the structured opening label must remain
suppressed. The row is not a `CertifiedOwner` claim and does not by itself
certify a strategic target square. The row may carry a target square only as
bounded metadata from same-family legal route evidence plus target-mode board
validation and catalog allowlisting; this remains support metadata, not a
standalone exact-slice owner claim. It may also carry sanitized `openingBook`
metadata from the admitted `OpeningReference` aggregate (ECO, total games, and
top SAN moves), but this is display support only and is not master-game truth,
family admission, or source provenance. Expanding `openings.tsv` with common
variation endpoints only widens the static FEN/name lookup pool; it does not
lower this truth boundary.

Exact-slice signoff must consume the typed `PlayerFacingExactSliceProof`
created by the board/probe witness branch. Owner, anchor, structure,
continuation, and prose terms may explain or diagnose the claim, but they are
not truth objects and must not be parsed back into exact proof.
Weakness target truth is centralized in `WeaknessTargetProfile`, which reads
the current legal FEN and reuses board-level pawn primitives for backward,
isolated, IQP, doubled, and fixed pawn targets. A generic exact target-fixation
witness may bind only a square present in that profile for the pressure side.
This profile is a board fact source; it is not by itself a certified strategic
claim. For lower-authority practical target display, `WeaknessTargetProfile`
may also classify a short legal PV endpoint: same-square persistence keeps the
target visible only when the line carries a `resultingFen` or at least five UCI
plies, pressure-side capture marks the target resolved, and defender
liquidation suppresses the target row instead of treating a vanished pawn as a
fixed objective. Shorter persistent lines are not enough to prove target
persistence for display.
Transposition-aligned strategic truth is a separate main-plan provenance, not
`ProbeBacked` truth. `TranspositionPvAligner` must legally replay the supplied
PV from the current FEN, reject illegal or too-shallow lines, recompute the
terminal `WeaknessTargetProfile`, bind only an expected target square carried
by the plan evidence as an exact `target:<square>` or
`weakness_target:<square>` token, require positive attacker-minus-defender
control on that target, and veto mate or large mover-loss lines. Passing this
boundary can admit a main strategic plan with `transposition_aligned`
provenance, but it does not satisfy exact-slice proof contracts,
`check_qualifying`, or probe-backed claim provenance.
Proof checks that bind a move to a square must use the chess UCI parser and the
typed destination square, not string slices over move text. SAN remains display
text.
MoveReview branch proof follows the same rule. Best-defense branch keys,
exact-slice continuation terms, favorable-exchange witnesses, queen-trade
shield checks, simplification exchange squares, and IQP inducement prefixes are
accepted only from bounded legal replay over the current FEN. Best-defense move
terms are likewise derived from the replayed legal move. Raw
`VariationLine.moves` is the preferred UCI input to that replay; parsed PV
metadata is a fallback only when raw moves are absent and cannot override the
engine line. Raw move text by itself can still support display and citation
wording, but it cannot certify same-branch state, continuation persistence, or
pawn-structure transition truth without legal replay.
Exact-slice target witness labels (`fixed_target`, `coordinated_target`,
`weak_square`), local file-entry structure markers, target-focused support
markers, and color-complex minor-piece attack markers are formatted by
`PlayerFacingExactSliceProofFacts`; producers must not create parallel label
formats while the typed proof remains the authority check.
Decision comparison and branch-citation wording use the same identity rule:
stale parsed SAN cannot rename a raw engine PV move or create a different
alternative branch label.
PV-derived resource-removal support also uses legal replayed origin and
destination squares for break or denied-square hits; generic threat words in PV
move text do not certify resource loss.
Generic exchange-forcing support must see a replayed legal capture on the
anchored branch. Parsed capture flags or same-destination text are insufficient
without legal replay from the current FEN.
Bad-bishop truth requires both the same-color central pawn constraint and a
mobility or own-pawn ray-blocker constraint. The strategic activity analyzer
reuses that rule for support signals; central-pawn count alone is not board
truth.
Defender-trade truth cannot originate in `PlanMatcher` from a raw
`RemovingTheDefender` motif or generic text labels. Those are generic exchange
support only; defender-trade ownership requires the replayed defense-relation
branch and typed exact-slice relation proof described above. Broad
`exchange_forcing_delta` source identity is not sufficient for the
defender-trade proof contract.
Generic defender/trade prose also cannot create `trade_key_defender` ownership in
MoveReview truth mode. When it is paired with a legal immediate
capture/recapture, move-linked exchange cue, and narrative anchor, it may speak
only as the softer `simplification_window` exchange sequence.
Raw `RemovingTheDefender` positional tags also cannot surface as a precise
concept-summary claim; they degrade to generic `Exchange pressure` unless the
replay-backed branch supplies defender-trade truth.
MoveReview basic surface recapture and center-capture labels are also board
truth. They require legal replay from the coupled PV FEN; SAN capture markers
or UCI file deltas are insufficient. Pin/skewer continuation confirmation is
also replay-bound; a sliced UCI coordinate cannot certify that the line touches
the pinned, behind, front, or back square.
Line-consequence wording keeps the public/internal split. Public player-surface
line consequences require a `MoveReviewRefs` line that passes strict legal FEN
replay. Engine-only `VariationLine` material may support internal narrative or
ledger wording from a legal concrete prefix, but stale tail moves are not proof,
are not kept as continuation evidence, and cannot promote mate/check metadata.
If the legal prefix is only a preview, the engine-only consequence stays
diagnostic.
Weakness-target practical guidance is lower authority, but it still cannot use
a stale or non-replayable `resultingFen` as board truth. Persistence or
liquidation display requires legal replay to the claimed board state, or the row
falls back/suppresses.
Tactical-window gates in MoveReview, including sacrifice settlement, forcing
proof availability, and IQP tactical-first veto, derive capture/check counts
from legal replay. Mate or forced-line metadata does not become tactical proof
when the PV branch itself cannot be replayed from the current FEN. Line-only
tactical proof follows the same boundary: candidate SAN text with capture/check
glyphs cannot stand in for a replayed engine PV. Exchange-forcing line evidence
requires a replay-backed exchange packet with branch and continuation/structure
witnesses; SAN or prose exchange words do not prove the line.
Neutralize-key-break exact readiness also requires a replayed best-defense
branch key; raw PV move text cannot satisfy the branch witness.
Break-prevention route proof cannot be built from an invalid played+PV splice:
candidate route lines must replay legally from the current FEN. Central-break
best-branch and PV transition terms, quiet-move best-defense metadata, and
L3 threat attack-square/capture evidence follow the same board-replay rule.
Structure route-contribution prose is support text, but it is still
board-bound: stale UCI coordinates cannot claim that the reviewed move started
or connected a deployment route unless that move legally replays.
ResourceRemoval owner truth cannot come from `citationLine`, `whyNot`, or
rendered resource phrases; it requires a replayed branch touching the denied
square, break file, or resource square. Resource-like resolved-threat or
lost-motif prose is a counterplay-reduction hint, not ResourceRemoval truth,
unless that replayed hit is present.
ExchangeForcing truth follows the same boundary: exchange/trade/simplification
phrases in active delta text are practical hints only unless a legal replayed
exchange witness proves the branch. Defender-trade truth additionally requires
a declared focal or structural weakness target; the exchanged square and
arbitrary defended pieces cannot stand in as the removed target.
For prophylactic restraint, exact-slice truth is limited to a square/route or
typed denied-resource token derived from runtime resource classification.
`PlanEvidenceEvaluator` owns denied-resource term projection, class
normalization, and exact-slice resource-token validation. English plan labels
and generic counterplay phrases do not certify the exact resource.
Carlsbad fixed-target truth is mirror-slice based: White-side pressure targets
Black `c6` only when the board has the `c6`/`d5` enemy chain, a friendly `d4`
pawn, and a friendly minority pawn on `b2`, `b4`, or `b5`; Black-side pressure
targets White `c3` only when the mirrored `c3`/`d4` enemy chain, a friendly `d5`
pawn, and a friendly minority pawn on `b7`, `b5`, or `b4` are present. Both
sides still require minority-attack semantic consequence and typed
`CarlsbadFixedTarget` proof.
Opening-route target-fixation truth is board/PV based. The reviewed UCI/PV
replay must legally satisfy an `OpeningRouteCatalog` row through
`PieceRouteEvidence`; the exact target-fixation facade remains
`KnightRouteEvidence` for knight routes. Route replay uses raw
`VariationLine.moves` before stale parsed metadata and carries the replayed
terminal `Position`; that terminal board must satisfy the row's `target_mode`
through `OpeningRouteTargetEvidence.checkRouteEvidence`.
`attack_weak_pawn` requires role-correct attack geometry from the route
destination to an enemy weak/fixed pawn target; `occupy_target` requires the
route role to occupy the target square. `PlayerFacingTruthModePolicy.findRouteWitness` scans catalog
rows, but if the slice declares a focal, directional, idea-focus, move-ref,
semantic structural-weakness, or root-board weak-pawn target, the selected route
target must match that declared target. Benoni `d6`, reversed Benoni `d3`, and King's Indian `c5` use
the same truth path. The route alone is not a truth claim without the slice
board witness. Starter routes for
Caro-Kann, French, Open Games, Sicilian, Queen's
Gambit, Slav/Semi-Slav, Nimzo-Indian, King's Indian, Gruenfeld, English, Reti,
Dutch, Scandinavian, Pirc/Austrian, Alekhine, Nimzowitsch, Catalan, London,
Bird, Queen's Indian, Bogo-Indian, and King's Gambit are data coverage
for legal replay support, not automatic public truth promotion. Queen's Indian
and Bogo-Indian direct `Nf6-e4` descriptors are still route support, not owner
truth. The current route catalog contains 52 descriptors, including 4 bishop
fianchetto descriptors that are bounded to opening-family support metadata
unless a separate exact-slice witness admits them. The mined additions
come from master-backed opening rows with at least five row witnesses, while
lower-support route candidates stay deferred. Each route target is also present
in the matching opening-family target allowlist, but that allowlist only permits
bounded target metadata after legal route evidence and board target-mode
validation. Likewise, the `openings.tsv`
static book pool and player target projection samples expand resolver lookup
coverage only; removed fixture floors are not coverage
authority, and FEN substrings or fixed branch-key text are not truth.
The current opening pool is pruned to 1276 rows that replay against captured
Lichess masters evidence as `master-backed`; 438 live-audited
`not-found-in-masters` expansion rows were removed. This proves master DB
occurrence for those endpoints, not strategic truth beyond the label-plus-FEN
resolver boundary.
Opening pool rows are not master-game truth unless the tooling-only
`OpeningMasterDbAuditRunner` has live OAuth-backed masters evidence for the
normalized endpoint or play sequence from its configured masters endpoint
(`--base-url`, defaulting to Lichess; `--since`/`--until` only when that endpoint
accepts date windows), or a replayable JSONL cache produced from that same
masters response.
`book-canonical`, `unverified`,
`not-found-in-masters`, `transposition-duplicate`, `master-fetch-error`, and
`quarantine` rows can support lookup cleanup but must not be cited as actual
master-game occurrence.
Opening-goal prose support is separate from truth signoff. `OpeningGoals`
evaluations such as `GruenfeldCenterChallenge`, `SlavFreeingBreak`,
`DutchE4Outpost`, `QueensIndianE4Outpost`, `BogoIndianE4Outpost`,
`CatalanTensionRelease`, `OpenCatalanPawnRecovery`, `SicilianC5Challenge`, and
`KingsGambitF4Break` are admitted from the parsed post-move board pattern plus
available engine score, then consumed through `openingGoalEvaluation`; they
are not `CertifiedOwner`, target-fixation truth, exact central-break timing
truth, or a replacement for the label-plus-FEN opening-family resolver.
Generic exact target-fixation witnesses must also bind the chosen square back
to the same target-fixing idea. If an idea lists several focus squares, runtime
may not select the only current `WeaknessTargetProfile` square unless the idea
id or typed evidence refs name that exact target; generic focus lists remain
support-only and fail closed.

Break/file-axis truth must come from structured square or file-marker evidence
parsed by the shared runtime boundary, not from incidental letters inside
English prose or underscore-separated plan ids. Heavy-piece release truth must
come from exact branch replay: queen centralization, one check, an unrecaptured
rook capture, or a back-rank rook shuffle is not enough to refute a local bind.
Heavy-piece file/entry persistence truth must not be parsed from English
denial phrases; it requires typed route-validation purpose, exact counterplay
resolution, exact denied file removal, and exact denied-square removal signals.
Defender-trade truth must not fall back to the exchange square as its target:
the removed piece must have defended a separate board square before the legal
exchange, and that defense relation must disappear after the recapture while
the target's defender count is strictly reduced. If the exchange merely swaps
one defender for a newly opened replacement defender, exact defender-trade truth
remains closed and the replayed exchange can only support lower-authority
exchange/material wording.
Bad-piece-liquidation truth must not classify a bishop from central pawn counts
alone; the current board must also show low bishop mobility or a central
own-pawn diagonal blocker before the legal liquidation branch can own the claim.
Forced-line trap truth may not release a named trap from a legal move string
alone. A named opening trap requires the reviewed move to be the trap entry and
a coupled PV/variation line to confirm the remaining sequence before the line is
replayed. Non-mating trap lines must also show a material-balance gain for the
side that makes the final material-winning move in the verified line.

Greek Gift truth must be board-backed by the reviewed move: the move must
actually capture on `h7`/`h2` with a bishop, the captured square must have held
the opponent pawn before the move, the resulting position must be check, and
kingside support geometry must be visible either immediately or after legal PV
replay from the post-sacrifice position. This allows the standard timing where
`Bxh7+` is followed by `...Kxh7 Ng5+` and then `Qh5`, while still rejecting a
queen, rook, or knight check on the same square as Greek Gift truth.
Named mate and tactical pattern labels are detected by ordered
`TacticalPatternDetector` implementations. The polymorphic shape is an
extension boundary only; each detector still has to read the parsed before/after
board state and may not infer truth from motif text.

Color-complex squeeze truth is exact-board only. The live proof source is
`color_complex_squeeze_probe`, and release requires a parsed FEN, an
opponent-owned semantic weak square, matching color-complex/hole/fianchetto
evidence, and a friendly bishop or knight that actually attacks that square on
the board. Coordinate and minor-piece words in packet terms are trace labels
only; they cannot certify truth. Final release still requires proven
same-branch state and stable persistence.

Favorable-exchange truth is replay-bound. `MoveReviewExchangeAnalyzer` is the
shared board engine for `defender_trade`, `bad_piece_liquidation`,
`queen_trade_shield`, and bounded simplification exchange-square checks: every
owner witness must come from legal FEN/PV replay, not UCI string geometry.
For defender trade and bad-piece liquidation, the truth unit is the legal
proof-sized prefix, not the entire displayed PV tail. A stale tail after the
three-ply defender-trade exchange or after the legal bishop capture/recapture
cannot add proof, but it also does not suppress the already proven local
witness. If the required prefix itself fails to replay, the owner path remains
closed.
Defender-trade truth cannot fall back to the exchange square as target; it must
show a real defended target whose defender relation is removed and whose
defender count is reduced. Replacement-defender geometry is not exact
defender-trade truth even though the captured piece no longer defends the
target. Bad-piece liquidation truth cannot come from same-color central pawn
counts alone; it must show a constrained bishop and a legal branch where that
bishop is traded away after entering the line. Queen-trade shield truth requires
a queen capture and king recapture on the replayed board; same-destination move
strings are not truth. Replay-backed favorable-exchange branch keys are
witness-line facts, not raw PV display strings. Semantic observations from the
same analyzer are selector evidence only unless the proof contract admits the
owner packet, and they require the reviewed played move identity rather than
inferring it from the engine top line.
The shared `RelationWitness` read-model is not a new truth shortcut. Overload,
deflection, discovered-attack, double-check, back-rank mate, mate-net, Greek gift,
zwischenzug, fork, hanging-piece, trapped-piece, domination, stalemate-trap,
perpetual-check, x-ray, clearance, battery, pin, skewer, interference, and
decoy relation observations may
support selector ranking or practical prose only after legal replay over the
current FEN proves the local attack/defense relation. Semantic relation
producers share one bounded top-PV replay context and one extracted
relation-witness set, so relation support rows are not produced from divergent
per-producer replay, witness, or local kind-filtering attempts. They do not certify owner
truth without a separate proof contract. Implemented relation truth must originate
from a `MoveReviewExchangeAnalyzer.ImplementedRelationWitnessTemplates` entry
and `RelationObservationCatalog`; unknown motif names cannot become relation
truth. When an explicit target set is
supplied, the relation witness must prove that target and may not fall back to
an arbitrary material piece; discovered-attack witnesses follow the same target
binding. Double-check support requires a replayed reviewed move that leaves the
opposing king checked by at least two checker squares, and explicit target
lists close this king-targeted relation instead of reopening material fallback.
Back-rank mate support reuses the existing tactical pattern detector and
requires the replayed reviewed move to end in `checkMate` with the defender's
king still on the back rank; explicit target lists close this king-targeted
relation instead of reopening material fallback.
Mate-net support reuses the existing mate-required tactical pattern detectors
other than back-rank mate, requires the replayed reviewed move to end in
`checkMate`, and keeps the detector id as support-only evidence; explicit
target lists close this king-targeted relation instead of reopening material
fallback.
Greek-gift support reuses the existing tactical pattern detector, requires a
legal reviewed bishop sacrifice on h7/h2 that gives check, and accepts only
immediate kingside support or support that appears by replaying the supplied PV
continuation. Explicit target lists close this pattern relation instead of
reopening material fallback.
Fork support requires a replayed move attacking at least two bound targets.
Hanging-piece support requires the replayed mover to attack an
undefended bound target. Trapped-piece support requires a legal replayed move,
a bound non-pawn target under attack, and zero legal safe or defense-preserving
reply routes for that target. Domination support requires the replayed mover to
control a bound non-pawn target whose legal reply routes are reduced to at most
one. Stalemate-trap support requires the replayed reviewed move to leave the
defender stalemated. Zwischenzug support requires a replayed forcing check,
legal reply, and non-pawn material payoff on the same line. Perpetual-check
support requires a legal continuation line whose checking side repeats the same
checking-position key. Pin and skewer support also stay on this boundary: a long-range
replayed move must create actual ray geometry through the pinned/front piece and
the piece behind it. Invalid explicit target tokens close the relation witness instead of
triggering structural or material fallback. `RelationObservationCatalog` is a routing catalog,
not a truth source: it can map implemented relation witnesses into
semantic/source/idea metadata, bounded public relation labels, and relation
producer admission. The runtime relation inventory has no remaining deferred
relation kind; unknown relation names are not cataloged selector rows, relation
producer inputs, or frontend authority tokens. Legacy motif-prefix prose,
theme-keyword prose, canonical motif-term prose, and motif appears/fades delta
prose still keep old relation-shaped labels generic or suppressed instead of
treating them as catalog evidence. Legacy trapped-piece, domination, and
zwischenzug helper notation is lowered to piece-mobility, key-square
restriction, or move-order caution wording unless the matching analyzer
relation witness exists. Legacy stalemate-trap and perpetual-check helper
notation is suppressed unless the matching replayed witness exists.
Threat-summary labels follow that same boundary: legacy relation-shaped motifs
from `ThreatAnalysis` may only surface as softer generic wording or
suppression, not as raw relation names.
Strategy-pack and structure-arc piece-activity evidence no longer promotes
legacy trapped-piece activity into relation evidence. Cached or legacy
strategy-pack evidence receives the same sanitizer treatment: legacy
relation-shaped evidence terms are stripped.
Legacy relation-shaped tags also cannot raise generic context prose into a
high-tension tactical opening frame without a non-legacy motif or actual
threat evidence, and they are not generic motif-prefix signals for future
consumers. Generic fact corroboration follows the same boundary, so an ordinary
tactical fact cannot certify a legacy relation-shaped tag as board-proven motif
support. Truth admission is still descriptor-based and descriptor lookup
resolves only implemented board-replayed relations. The selector evidence
carrier mirrors that rule by preserving relation identity/focus only for
implemented catalog kinds; unknown names cannot survive into a relation
candidate.
Unknown relation witnesses do not become target-pressure or other selector
evidence; missing catalog entries fail closed.
The relation semantic producer consumes the implemented catalog relation set, so
producer reachability follows the routing catalog.
`StrategyPackBuilder` may preserve a relation-only selected idea so the support
metadata reaches the MoveReview surface, but that carrier does not promote the
relation into proof/source/family truth.
Move-local defender-trade and bad-piece owner witnesses reuse the analyzer
`RelationWitness` typed details for branch, exchange, target, and PV terms;
policy-local extras can add owner transition wording but cannot replace the
board-replayed relation details. The analyzer owns decoding of those details
back into branch fields and expanding shared owner/transition terms through the
structured `relationSupport` carrier attached to `relationProjectionFromWitness`;
policy code does not parse raw detail keys, read relation facts/focus squares
directly, or consume dynamic fact strings as runtime truth for these owner
packets. A typed-detail mismatch emits no owner-seed, transition terms, or typed
exact-slice relation proof.
Branch-key construction and `branch:*` fact projection are also analyzer-owned;
policy code consumes the helper output from replayed witness line moves instead
of rebuilding branch identity with UCI substring or local pipe-join logic.
Other implemented relation witnesses also carry typed relation details for their
replayed king, attacker, defender or cleared square, mate pattern, material or
line target, lure/execution, and front/back or pinned/behind coordinates. Those
details do not grant owner truth by themselves; they preserve the board witness
so later certified or softer-label consumers do not recreate the relation from
strings.
If typed details are present but do not match the relation kind, the relation
support row is rejected; raw witness focus, target, or fact fields are fallback
only for intentionally untyped legacy witnesses.
Semantic observation admission consumes the analyzer-owned
`relationProjectionFromWitness` carrier after that check, so downstream
relation rows receive one canonical kind/focus/target/support projection instead
of reconstructing truth from raw witness strings.
`moveReviewPlayerSurface` may expose those implemented relation rows only as
`strategic_relation` support metadata. The relation token and required target
square are public display metadata, not proof-family/source truth and not a
frontend permission to rebuild omitted relation claims from prose. Projection
requires a selected implemented `relationKind`, matching structured
`relationSupport`, and a relation-focus-derived target; source/fact strings
alone remain internal support metadata. API serialization re-sanitizes the
player surface before emission; that step preserves the bounded metadata but
does not create, widen, or reclassify relation truth.
The exception is not a support-row shortcut: defender-trade and
bad-piece-liquidation can appear as supported-local summary relation rows only
when the claim packet already carries matching
`PlayerFacingExactSliceProof.DefenderTrade` or
`PlayerFacingExactSliceProof.BadPieceLiquidation`, proven same-branch state,
stable persistence, no release risk, and `ClaimAuthorityResolver` admission.
That row is claim-packet consumption, not a promotion of support prose.
`RelationObservationCatalog` owns the selected-`relationKind` no-fallback rule
and public target fallback, so downstream builders do not repeat or widen
relation matching. Surface projection does not recreate motif-specific target
order from raw relation strings or promote a legacy carrier's generic
`targetSquare` into relation target. Each relation descriptor exposes its source
ref and semantic fact pair for selector prioritization and diagnostics, not for
player-surface row admission.
Relation witness support facts cannot manufacture those admission tokens:
dynamic facts reject semantic observation ids, evidence-source ids,
proof-source ids, proof-family ids, and `source:` wire keys.
`RelationSurfaceText` may use only kind/focus-matching structured
`relationSupport` to make bounded relation wording more specific for relation
rows, selector idea/focus text, and why/next-check move-review summary cues; it
cannot parse `evidenceRefs`, admit a relation row, change the relation token, or
choose a target outside the relation-focus boundary.
Its row-text and summary-cue entrypoints also fail closed when matching
structured support is missing, so relation evidence prose cannot be produced
from label/focus metadata alone. Fork target roles, king-pattern moves or
cycles, overload duties, controller roles, and trapped-piece attacker lists are
display detail from that support carrier; they do not create proof-family truth.
Putting the summary cue before plan/opening/practical rows is a player-review
presentation rule, not a truth upgrade.
Selector digest and long-term-focus enrichment may consume that exact support
wording, but it remains support evidence and does not mint proof authority.
When the analyzer supplies a relation target, that square is carried through
semantic evidence and selected idea metadata to the surface row only while it
remains bound to the relation focus squares; descriptor target fallback may use
that same relation-focus ordering, but generic idea focus and legacy
`targetSquare` metadata are not truth evidence.
When multiple relation observations merge into one selected idea, the selector
keeps a representative `relationKind`; representative choice uses
catalog-owned structured selector priority plus any narrow typed-support
priority before confidence, but the surface row uses that kind only when
matching structured `relationSupport` remains attached, so catalog ordering
cannot invent a different public relation. The same priority is used when the
selector preserves the visible secondary relation from the selected and merged
candidate sets, so an already selected generic relation can be replaced by a
higher-priority replay-backed relation candidate. Typed-support priority is
limited to structured support detail such as
`StrategyRelationSupport.absolutePin == true`, not a label or fact string. The
selector also preserves relation-specific focus squares for that representative
relation, so public relation prose does not use focus squares borrowed from
another merged relation witness. A selected relation kind without
relation-specific focus remains unprojected instead of borrowing merged idea
focus. Selector merge does not synthesize relation focus from generic
`focusSquares` or relation target from generic `targetSquare`; missing
relation-focus or relation-target evidence stays missing until catalog fallback
over relation focus is applied at the public surface.
It also prioritizes the representative relation's source ref and semantic fact
before evidence truncation, keeping the catalog admission pair intact while
other support refs remain lower priority.
Selector secondary-slot preservation for a structured relation candidate is not
truth promotion. It only keeps an already replay-backed, cataloged,
`relationSupport`-matched relation visible to downstream player-surface
projection when a stronger idea family or a lower-priority relation would
otherwise occupy the visible slots. Catalog and typed-support selector priority
likewise choose among already replay-backed relation witnesses; they do not
create relation truth and cannot promote uncataloged or support-mismatched
evidence.
Catalog-order fallback is not a public row admission path for legacy ideas that
carry no `relationKind`; source/fact string matches and non-empty
`relationFocusSquares` still remain unprojected without structured support.
Raw top-level `strategyPack.strategicIdeas` are stripped from final
user-facing payloads and cannot serve as a parallel relation-truth channel.
The public `strategic_relation` token is catalog-bound: an uncataloged relation
key is not relation truth even if it has the right wire-key shape.
Backend sanitization preserves that token on builder-owned relation surface
lanes: the summary why/next-check cue and advanced support row. Probe rows and
author branches still drop relation authority rather than reinterpreting it.
The analyse frontend decoder enforces the same constraint when decoding cached
or legacy payloads, so frontend code does not widen backend relation authority.
The analyse player-surface renderer may display decoded relation and target
chips, but it is not a truth source and must not infer relation authority from
row prose or fallback commentary.
Move-local owner witnesses follow that same rule: defender trade, bad-piece
liquidation, and IQP inducement fail closed when the reviewed played move is
missing, even if the top engine line legally replays.
Raw carrier prose does not identify the defender-trade family: text mentioning
defenders, guards, trades, or removal is not truth for the exact owner path
without typed family context and the replayed defender-target relation.
Generic plan-owner fallback is subject to the same boundary: exact
defender-trade, bad-piece-liquidation, and queen-trade-shield families are
stripped unless their board-replayed witness exists, so a generic plan label
cannot re-promote after the exact witness fails closed.

## No-Go Truth Failures

These are release blockers:

- verified-best played move is called wrong, loose, inaccurate, or blunder-like
- benchmark move is named when it is not verified best
- blunder truth is softened into compensation-positive framing
- compensation-positive prose appears without investment ownership
- maintenance is narrated as the original investment
- conversion is narrated as compensation
- support-only or deferred carriers revive owner claims after certification
  failed closed
- fallback projection reconstructs investment, maintenance, conversion, or
  strategic plan truth from serialized strings
- frontend fallback retry/drop behavior is inferred from commentary prose
  rather than backend `diagnostics.status`
- general lesson or rule wording appears without local scoped-takeaway authority
- decision comparison is rebuilt from `topEngineMove`, `cpLossVsChosen`, or
  latent/deferred carriers
- line/PV consequence wording is surfaced from engine-only `VariationLine`
  interpretation or failed ref replay instead of a `MoveReviewRefs` line that
  passed legal FEN replay
- strategic plan refutation is inferred from sibling score/ranking rather than
  exact board/probe evidence
- strategic plan truth is inferred from raw `evidenceTier`, compatibility
  markers, aliases, plan names, or subplan ids instead of
  `PlanSemanticsContract` plus typed evaluator evidence and exact witness where
  required
- local bind truth is rejected from generic movement labels without
  exact replay evidence of a real release
- MoveReview strategic support, probe, authoring, or decision-comparison UI is
  rebuilt from raw `strategyPack`, `signalDigest`, `authorEvidence`,
  `probeRequests`, full `mainStrategicPlans`, or strategic-plan experiment
  metadata instead of the backend-certified `moveReviewPlayerSurface`
- strategic intent is invented for a low-confidence or tactical-first failure

## Scoped Takeaway Boundary

`MoveReviewScopedTakeaway` is local instruction only. It must be tied to:

- reviewed UCI
- post-move FEN
- same coupled PV branch
- evidence tier
- guardrail tags

It may project into the existing MoveReview learning-point compatibility field,
but it is not general lesson authority and must not generalize into rules.

## Audit Gate

Automatic checks are necessary but not sufficient. Truth signoff requires:

- targeted compile/tests for touched runtime paths
- family-wise truth metrics
- GM-style manual review on the master audit corpus when release signoff is in
  scope
- explicit separation of target slice, aggregate behavior, runtime behavior,
  and test/tooling artifacts

Primary truth metrics:

- verified-best mislabeled count
- wrong benchmark naming count
- blunder softening count
- fake compensation count
- missed commitment count
- maintenance-as-commitment count
- conversion-as-compensation count
- visible exemplar lost count

Health signals such as parity, release-gate flags, provider-none stability, and
cross-surface agreement remain secondary to chess truth.

## Maintenance Triggers

Update this file when any change affects:

- truth/signoff behavior
- fallback truth projection
- claim authority signoff
- benchmark naming
- compensation/investment/conversion ownership
- scoped takeaway or lesson-readiness guards
- frontend/API behavior that could alter user-facing truth
