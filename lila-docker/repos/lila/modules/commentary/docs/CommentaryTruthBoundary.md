# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Chronicle,
Active, and Game Arc are legacy diagnostic/tooling surfaces. Their bridge,
thread, note-composition, and compression helpers are isolated to
`modules/commentaryTools/src/test` and may not participate in released truth
signoff unless a current MoveReview consumer and runtime audit explicitly
reopen that boundary.
Runtime truth projection must not accept `GameChronicleMoment` or
`GameChronicleResponse` as signoff inputs. Chronicle fallback semantics may
exist only in test/tooling diagnostics; released truth paths consume the current
MoveReview/arc runtime models and proof-contract projections.
Active-note payload fields and active branch dossiers are not truth inputs in
runtime `GameArc`; tooling that reconstructs them must do so outside `src/main`.

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

Strategic plan refutation must be exact-board/probe truth. Sibling plan ranking
or alternative-dominance metadata can demote selection, but it is not a chess
refutation and must not be reported as `Refuted`.

Strategic plan promotion and demotion must be consumed through the typed
`StrategicPlanEvidenceView` derived from `PlanEvidenceEvaluator`. Raw
`StrategicPlanExperiment.evidenceTier` strings, compatibility markers, and
subplan ids cannot by themselves certify truth, refutation, provenance,
quantifier, stability, or exact-family ownership.
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
after that resolver decision; label-only opening data, broad opening phase, raw
rendered prose, or FEN lookup without the structured opening label must remain
suppressed. The row is not a `CertifiedOwner` claim and does not by itself
certify a strategic target square. The row may carry a target square only as
bounded metadata from same-family legal route evidence plus target-mode board
validation and catalog allowlisting; this remains support metadata, not a
standalone exact-slice owner claim. It may also carry sanitized `openingBook`
metadata from the admitted `OpeningReference` aggregate (ECO, total games, and
top SAN moves), but this is display support only and is not master-game truth,
family admission, or source provenance. Expanding `openings.tsv` with common
variation endpoints only broadens the static FEN/name lookup pool; it does not
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
`RemovingTheDefender` motif or broad text labels. Those are generic exchange
support only; defender-trade ownership requires the replayed defense-relation
branch or typed semantic observation described above.
Broad defender/trade prose also cannot create `trade_key_defender` ownership in
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
coverage only; removed broad-variation Scala fixtures are not coverage
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
id or typed evidence refs name that exact target; broad focus lists remain
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
deflection, discovered-attack, double-check, back-rank mate, mate-net, Greek gift, fork, hanging-piece, x-ray, clearance, battery, pin, skewer, interference, and decoy relation observations may
support selector ranking or practical prose only after legal replay over the
current FEN proves the local attack/defense relation. Semantic relation
producers share one bounded top-PV replay context and one extracted
relation-witness set, so relation support rows are not produced from divergent
per-producer replay, witness, or local kind-filtering attempts. They do not certify owner
truth without a separate proof contract. When an explicit target set is
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
undefended bound target. Pin and skewer support also stay on this boundary: a long-range
replayed move must create actual ray geometry through the pinned/front piece and
the piece behind it. Invalid explicit target tokens close the relation witness instead of
triggering structural or material fallback. `RelationObservationCatalog` is a routing catalog,
not a truth source: it can map implemented relation witnesses into
semantic/source/idea metadata, bounded public relation labels, and relation
producer admission. The runtime inventory keeps a non-public deferred list
for motif families that are present in adjacent model or detector assets but
lack a board-replayed relation witness: `zwischenzug`, `domination`,
`trapped_piece`, `stalemate_trap`, and `perpetual_check`.
Deferred entries record the required witness shape, defer reason, and fallback
lane, and project a catalog-owned `DeferredRelationFallback` read-model for
non-relation consumers; they are not cataloged selector rows, not relation
producer inputs, and not frontend authority tokens. They remain outside public
relation metadata until a legal board witness exists. The fallback projection
is not truth admission:
`zwischenzug` and `trapped_piece` may fall to practical guidance, `domination`
to thematic fallback, while `stalemate_trap` and `perpetual_check` are
diagnostic-only because unproven drawing resources must stay fail-closed. The
legacy motif-prefix table, theme-keyword path, canonical motif-term path, and
motif appears/fades delta prose use the same fallback projection, emitting
only softer non-relation practical/thematic text or suppressing diagnostic-only
motifs, so deferred relation names cannot become truth claims through prose.
Legacy plan evidence also consumes the deferred domination fallback label
instead of naming `domination` as a plan proof term.
User-facing helper-notation cleanup follows the same boundary: deferred
practical/thematic helpers are rewritten to catalog fallback wording, while
diagnostic-only deferred helpers are suppressed.
Threat-summary labels follow that catalog boundary as well: deferred relation
motifs from `ThreatAnalysis` may only surface as softer fallback wording, not
as raw relation names.
Strategy-pack and structure-arc piece-activity evidence consumes the catalog
fallback evidence term for trapped-piece activity instead of emitting raw
`trapped_piece` relation labels as public support metadata. Cached or legacy
strategy-pack evidence receives the same sanitizer treatment: deferred relation
motif terms are stripped, while catalog fallback evidence terms can remain as
lower-authority support.
Deferred relation tags also cannot raise generic context prose into a
high-tension tactical opening frame without a non-deferred motif or actual
threat evidence, and they are not generic motif-prefix signals for future
consumers. Generic fact corroboration follows the same boundary, so an ordinary
tactical fact cannot certify a deferred relation tag as board-proven motif
support. The
catalog inventory can name both implemented and deferred motif families, but
truth admission is still descriptor-based and descriptor lookup resolves only
implemented board-replayed relations. The selector evidence carrier mirrors
that rule by preserving relation identity/focus only for implemented catalog
kinds; deferred or unknown names cannot survive into a relation candidate.
Unknown relation witnesses do not become target-pressure or other selector
evidence; missing catalog entries fail closed.
The relation semantic producer consumes the implemented catalog relation set, so
producer reachability follows the routing catalog.
`StrategyPackBuilder` may preserve a relation-only selected idea so the support
metadata reaches the MoveReview surface, but that carrier does not promote the
relation into proof/source/family truth.
Move-local defender-trade and bad-piece owner witnesses reuse the analyzer
`RelationWitness` typed details and fact vocabulary for branch, exchange, target,
and PV terms; policy-local extras can add owner transition wording but cannot
replace the board-replayed relation facts. The analyzer owns decoding of those
details back into branch fields and expanding shared owner/transition terms
through `relationProjectionFromWitness`; policy code does not parse raw detail
keys or read relation facts/focus squares directly for these owner packets. A
typed-detail mismatch emits no owner-seed or transition terms.
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
relation rows receive one canonical kind/focus/target/fact projection instead
of reconstructing truth from raw witness strings.
`moveReviewPlayerSurface` may expose those implemented relation rows only as
`strategic_relation` support metadata. The relation token and required target
square are public display metadata, not proof-family/source truth and not a
frontend permission to rebuild omitted relation claims from prose. Projection
requires the catalog source ref plus the matching semantic observation fact and
a relation-focus-derived target; a source-only idea remains internal support.
`RelationObservationCatalog` owns that source/fact admission helper and the
selected-`relationKind` no-fallback rule, so downstream builders do not repeat
or widen relation matching. It also owns public target fallback for older
carriers that lack an analyzer target, so surface projection does not recreate
motif-specific target order from raw relation strings or promote a legacy
carrier's generic `targetSquare` into relation target. Each relation descriptor exposes its own source ref
and semantic fact pair, keeping selector prioritization and public projection on
the same catalog admission tokens.
Relation witness support facts cannot manufacture those admission tokens:
dynamic facts reject semantic observation ids, evidence-source ids,
proof-source ids, proof-family ids, and `source:` wire keys. Only the catalog
descriptor may provide the relation source ref and matching semantic fact.
When the analyzer supplies a relation target, that square is carried through
semantic evidence and selected idea metadata to the surface row only while it
remains bound to the relation focus squares; descriptor target fallback may use
that same relation-focus ordering, but generic idea focus and legacy
`targetSquare` metadata are not truth evidence.
When multiple relation observations merge into one selected idea, the selector
keeps a representative `relationKind`; the surface row uses that kind only with
its matching catalog source and semantic fact, so catalog ordering cannot invent
a different public relation. The selector also preserves relation-specific focus
squares for that representative relation, so public relation prose does not use
focus squares borrowed from another merged relation witness. A selected relation
kind without relation-specific focus remains unprojected instead of borrowing
merged idea focus. Selector merge does not synthesize relation focus from
generic `focusSquares` or relation target from generic `targetSquare`; missing
relation-focus or relation-target evidence stays missing until catalog fallback
over relation focus is applied at the public surface.
It also prioritizes the representative relation's source ref and semantic fact
before evidence truncation, keeping the catalog admission pair intact while
other support refs remain lower priority.
Catalog-order fallback is reserved for legacy ideas that carry no
`relationKind`, exactly one matching relation source/fact pair, and non-empty
`relationFocusSquares`; ambiguous or relation-focusless legacy relation evidence
remains unprojected. A named relation whose source/fact pair does not match also
remains unprojected.
Raw top-level `strategyPack.strategicIdeas` are stripped from final
user-facing payloads and cannot serve as a parallel relation-truth channel.
The public `strategic_relation` token is catalog-bound: an uncataloged relation
key is not relation truth even if it has the right wire-key shape.
Backend sanitization preserves that token only on `advancedRows`, the sole
builder-owned relation surface lane; stale summary, probe, or author-row
relation authorities are dropped rather than reinterpreted.
The analyse frontend decoder enforces the same constraint when decoding cached
or legacy payloads, so frontend code does not widen backend relation authority.
Move-local owner witnesses follow that same rule: defender trade, bad-piece
liquidation, and IQP inducement fail closed when the reviewed played move is
missing, even if the top engine line legally replays.
Raw carrier prose does not identify the defender-trade family: text mentioning
defenders, guards, trades, or removal is not truth for the exact owner path
without typed family context and the replayed defender-target relation.
Generic plan-owner fallback is subject to the same boundary: exact
defender-trade, bad-piece-liquidation, and queen-trade-shield families are
stripped unless their board-replayed witness exists, so a broad plan label
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
- broad lesson or rule wording appears without local scoped-takeaway authority
- decision comparison is rebuilt from `topEngineMove`, `cpLossVsChosen`, or
  latent/deferred carriers
- line/PV consequence wording is surfaced from engine-only `VariationLine`
  interpretation or failed ref replay instead of a `MoveReviewRefs` line that
  passed legal FEN replay
- strategic plan refutation is inferred from sibling score/ranking rather than
  exact board/probe evidence
- strategic plan truth is inferred from raw `evidenceTier`, compatibility
  markers, or subplan ids instead of typed evaluator evidence plus exact
  witness where required
- local bind truth is rejected from broad heavy-piece movement labels without
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
but it is not Track 5 lesson authority and must not generalize into broad rules.

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
