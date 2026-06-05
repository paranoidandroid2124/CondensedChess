# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Removed
Chronicle/Active surfaces follow the canonical boundary in
`CommentaryPipelineSSOT.md`: they are excluded from signoff inputs, Chronicle
fallback semantics are gone, and released truth paths consume only the current
MoveReview runtime models and proof-contract projections.

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
board-backed link from the played move. The packet witness terms must mirror
the typed `CentralBreakTiming` proof's `break_move`, `break_token`, and central
break square, so a bare typed proof cannot be attached to unrelated packet
terms. The played move itself can be that exact witness when it legally makes
the same central break; top-PV omission is
not by itself a truth failure for this bounded support row. Diagonal captures
and prep/challenge pawn moves, including Sicilian c-pawn and King's Gambit
f-pawn opening breaks, are not truth for the product-visible `Central break`
row. Those opening-break rows may surface only as lower-authority practical
support when the reviewed pawn move legally replays, the post-move opening goal
is achieved or partial, the selected `OpeningGoals` trigger matches that played
move, and the legal top PV starts with that same played move.
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
claim or enough to publish a practical target row. For lower-authority
practical target display, `WeaknessTargetProfile` must also classify a
non-empty best-line continuation endpoint from legal replay or a trusted
`resultingFen`. Current-position targets remain the preferred row source; a
target that exists only at the checked-line endpoint can surface only with an
exact square target hint and the same trusted endpoint or five-ply horizon
boundary. Bounded practical plans can bind a specific display target only
with exact square evidence tokens
`weakness_target:<square>`, `fixed_target:<square>`,
`coordinated_target:<square>`, exact `target:<square>`, `target_fixing:<square>`,
`enemy_weak_square:<square>`, or `weak_complex:<square>`; role-bearing target
tokens such as `target:e5:queen` are relation/material facts, not weakness
target truth. Same-square persistence keeps the target visible only when the
line carries a `resultingFen` or at least five UCI plies, pressure-side capture
marks the target resolved, and defender liquidation suppresses the target row
instead of treating a vanished pawn as a fixed objective. Shorter persistent
lines are not enough to prove target persistence for display. Best-vs-chosen
target comparison metadata follows the same continuation boundary: an empty
best PV cannot create a target contrast from `resultingFen` alone.
Transposition-aligned strategic truth is a separate main-plan provenance, not
`ProbeBacked` truth. `TranspositionPvAligner` must legally replay the supplied
PV from the current FEN, reject illegal or too-shallow lines, recompute the
terminal `WeaknessTargetProfile`, bind only an expected target square carried
by the plan evidence as an exact `target:<square>`,
`weakness_target:<square>`, `fixed_target:<square>`, or
`coordinated_target:<square>` token, or a route/weak-complex exact square token
such as `target_fixing:<square>`, `enemy_weak_square:<square>`, or
`weak_complex:<square>`, require positive attacker-minus-defender
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
Author evidence and forced-line truth use the same legal post-move replay for
reviewed, best, and branch-entry UCI moves; illegal or stale UCI fails closed
instead of reusing the original FEN as a synthetic post-move board.
Quiet-move intent is also display support, not truth signoff. Public
`Piece improvement`, `King safety`, or `Technical conversion` rows require the
existing user-facing packet gate plus legal replay of the reviewed UCI move and
a matching square anchor. The packet proof family, proof source, and ontology
family must also match the quiet-intent class, so a mismatched support packet
cannot relabel itself as a different practical row. They keep `PracticalPlan`
authority with no token and do not become owner truth, lesson readiness,
opening-family truth, central-break truth, or counterplay/prophylaxis truth.
Tactical truth vetoes and release risks fail closed.
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
exchange witness proves the branch. Line-scoped strategic support may recognize
concrete SAN capture tokens only after the replay-backed exchange packet is
present. Defender-trade truth additionally requires a declared focal or
structural weakness target; the exchanged square and arbitrary defended pieces
cannot stand in as the removed target.
For prophylactic restraint, exact-slice truth is limited to a square/route or
typed denied-resource token derived from runtime resource classification.
English plan labels and generic counterplay phrases do not certify the exact
resource.
Named route-network public truth is limited to the certified
`RouteNetworkBindProof.SurfaceNetwork` tuple selected from legal replay and
probe-backed route evidence. The MoveReview `Route denial` row may state only
that the checked line keeps the entry square closed, removes the named file,
and cuts off the reroute square. Intermediate route chains, heavy-piece blocked
local-bind shells, tactical truth modes, and raw route prose do not certify a
public route-network claim.
Dual-axis bind public truth is limited to a certified
`TwoAxisBindProof.Contract` whose scope is `dual_axis_local`, archetype is
`break_plus_entry`, reinflation risk is `bounded_dual_axis_only`, and whose
contract carries both a measured break axis and an independent entry axis. The
MoveReview `Break and entry` row may state only that checked pair, not broad
local-bind, global squeeze, or "no counterplay" truth. Heavy-piece blocked
local-bind shells, tactical truth modes, uncertified contracts, and raw
hypothesis prose do not certify this row.
IQP inducement public truth is a bounded structure-transition statement. Legal
FEN/PV replay must show that the reviewed move or checked prefix creates a new
opponent isolated central pawn, and the packet must carry
`PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves)` plus the
induced target terms `after_isolated:<square>` and `isolated_pawn:<square>`,
the `central_isolated_pawn` marker, and the checked line moves. The public `IQP target`
row may name only that square; generic IQP wording, source precedent, an
existing isolated pawn without the induced transition, or transition terms
without the typed exact-slice proof are not truth for this row.
Simplification-window public truth is limited to a replayed exchange-square
continuation. Legal branch/persistence truth must already satisfy the existing
`simplification_window` packet contract, and the public row also requires an
`exchange_square:<square>` term from that checked continuation. Generic
favorable-exchange wording, source precedent, and "same local edge" prose are
not truth for the public `Simplification` row without that exchange-square
witness.
Carlsbad fixed-target truth is mirror-slice based: White-side pressure targets
Black `c6` only when the board has the `c6`/`d5` enemy chain, a friendly `d4`
pawn, and a friendly minority pawn on `b2`, `b4`, or `b5`; Black-side pressure
targets White `c3` only when the mirrored `c3`/`d4` enemy chain, a friendly `d5`
pawn, and a friendly minority pawn on `b7`, `b5`, or `b4` are present. Both
sides still require minority-attack semantic consequence and typed
`CarlsbadFixedTarget` proof. Public MoveReview row and position-probe main-claim
wording may call that exact slice a bounded `Minority attack` target /
minority-attack fixed target only when the typed proof carries
`minoritySupport = true`; generic minority-attack semantics remain support-only
and do not open the standalone `minority_attack_fixation` proof family.
MoveReview position-probe rows consume the same exact-slice truth boundary:
`ExactTargetFixation`, `CarlsbadFixedTarget`, `TargetFocusedCoordination`, and
`ColorComplexSqueeze` rows require a PositionLocal packet admitted by
`decidePositionProbe` plus the matching typed proof case. They cannot be minted
from focus-square prose, route text, coordination terms, or MoveLocal target
packets. The typed target must also be mirrored by the packet witness terms:
`ExactTargetFixation` and `CarlsbadFixedTarget` require
`fixed_target:<square>`, while `TargetFocusedCoordination` requires
`coordinated_target:<square>`.
Opening-route target-fixation truth is board/PV based. The reviewed UCI/PV
replay must legally satisfy an `OpeningRouteCatalog` row through
`PieceRouteEvidence`; `KnightRouteEvidence` is only the knight-route
compatibility facade. Route replay uses raw
`VariationLine.moves` before stale parsed metadata and carries the replayed
terminal `Position`; that terminal board must satisfy the row's `target_mode`
through `OpeningRouteTargetEvidence.checkRouteEvidence`.
`attack_weak_pawn` requires role-correct attack geometry from the route
destination to an enemy weak/fixed pawn target; `occupy_target` requires the
route role to occupy the target square. `PlayerFacingTruthModePolicy.findRouteWitness` scans catalog
rows, but if the slice declares a focal, directional, idea-focus, move-ref,
semantic structural-weakness, or root-board weak-pawn target, the selected route
target must match that declared target. If `openingData.name` or an
`OpeningEvent.Intro` name resolves to a family, the route must also belong to
that same `OpeningFamilyCatalog` family and target allowlist; positions without
an opening family label keep the legal-replay and target gates. Benoni `d6`, reversed Benoni `d3`, and King's Indian `c5` use
the same truth path. The route alone is not a truth claim without the slice
board witness. Starter routes for
Caro-Kann, French, Open Games, Sicilian, Queen's
Gambit, Slav/Semi-Slav, Nimzo-Indian, King's Indian, Gruenfeld, English, Reti,
Dutch, Scandinavian, Pirc/Austrian, Alekhine, Nimzowitsch, Catalan, London,
Bird, Queen's Indian, Bogo-Indian, and King's Gambit are data coverage
for legal replay support, not automatic public truth promotion. Queen's Indian
and Bogo-Indian direct `Nf6-e4` descriptors are still route support, not owner
truth. The current route catalog contains 52 descriptors, including 4 bishop
fianchetto descriptors that may enter exact target-fixation only through the
same legal replay, family/target allowlist, declared-target, and terminal
target-mode witness gates. The mined additions
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
are gated to opening phase/event context or the early opening-data window, so
opening labels alone cannot keep producing goal prose in later
middlegame/endgame slices. They are not `CertifiedOwner`, target-fixation
truth, exact central-break timing truth, or a replacement for the label-plus-FEN
opening-family resolver. Achieved or partial opening-break goals may surface
only as lower-authority `Opening break` practical rows with `PracticalPlan`
authority, no token, a matching selected `OpeningGoals` trigger, a legal top-PV
played-move witness, and the same tactical-truth veto used for practical central
rows. Achieved Dutch, Queen's Indian, and Bogo-Indian e4 outpost goals may
surface only as a separate lower-authority `Opening outpost` practical row when
the goal evidence names the outpost square and the reviewed UCI move legally
replays as a knight move to that square, with the legal top PV starting from
that same reviewed move; they do not mint central-break, opening-family, target,
or lesson truth.

General outpost occupation truth is a separate exact-slice path, not an opening
goal shortcut. `OutpostEntrenchment` can surface only as supported-local
MoveReview `Knight outpost` support when the packet carries
`PlayerFacingExactSliceProof.OutpostOccupation("knight", square)`, the reviewed
UCI move legally replays from the current FEN as the moving side's knight move
to that square, the legal top PV starts with that reviewed move, and
`Fact.Outpost(square, Knight, Now)` is present in current/main-PV facts. The
same square, `piece:knight`, and `outpost_occupation:knight:<square>` terms
must be mirrored in packet witnesses, and release still needs proven same
branch plus stable persistence. Broad outpost prose, semantic tags, route access
evidence, and opening-goal evidence remain support-only for this exact-slice
truth boundary.
Opening-relation planner ownership also cannot rest on
a self-only `OpeningReference.sampleGames` replay; replay support needs at least
two total/sample games and an opening event or the early opening-data window.
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
The MoveReview `Color complex` summary row is a consumer of that exact proof,
not a separate truth source: it requires position-probe admission plus a typed
`PlayerFacingExactSliceProof.ColorComplexSqueeze` matching the packet
source/family. The typed proof must also be internally coherent: the declared
light/dark complex must match the target square, and the named bishop/knight
geometry must attack that target. The packet witness terms must mirror the same
weak square, color-complex token, role-specific
`minor_piece:<role>_<square>` term, and minor-piece attack pair, so a bare typed
proof case cannot be spliced onto unrelated packet evidence. Term-only,
malformed, term-mismatched, or tactical-vetoed packets remain non-public.

Local-file entry truth is exact file/entry-pair only. The live proof source is
`local_file_entry_bind` under `half_open_file_pressure`; release requires the
typed `PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare)` to
match the packet source/family and the same owner/continuation/branch/stable
persistence witnesses used by other exact move-delta packets. The packet witness
terms must also carry the same file/entry pair as a single
`file-entry:<file>:<square>` or `file_entry:<file>:<square>` witness term. Split
file and entry terms under the `local_file_entry_bind` marker are not public
release proof. The entry square must be on the claimed file. Public release
also requires context-aware board support from the current FEN and legal
reviewed-move replay: a major piece must be on the claimed file, and the named
entry square must be occupied or controlled by the moving side on the before or
after board. The FEN-less pair extractor is not truth authority. The MoveReview
`File entry` summary row consumes the certified proof through supported
move-delta admission and may only state pressure through the named file on the
named entry square. It cannot certify broader heavy-piece bind, global squeeze,
or no-counterplay truth.

Counterplay-restraint truth is named-resource only. The live exact proof is
`PlayerFacingExactSliceProof.ProphylacticRestraint`, and release requires the
`prophylactic_move` / `counterplay_restraint` packet path plus the same
owner/continuation/branch/stable persistence checks used by exact move-delta
packets. The proof token must be a board/resource token such as a square, route,
or `denied_resource:<class>`; free-form labels are not truth. The packet witness
terms must mirror that typed resource token before a surface row can consume the
proof. The MoveReview `Counterplay restraint` row consumes only that admitted
packet and may state that the named resource is restrained, not that all
counterplay is gone.

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
The MoveReview `Defender trade`, `Bad piece trade`, and `Queen trade` summary
rows consume those admitted packets only. Defender/bad-piece rows require the
structure-transition witness, proven same-branch state, and stable persistence
to satisfy the proof contract, plus matching typed exact-slice proof and
analyzer branch fact terms
(`defender_trade_branch` with defender, exchange,
and defended-target terms; `bad_piece_liquidation_branch` with bad-piece and
exchange terms). Policy-built defender/bad-piece packets additionally carry
typed exact-slice proof payloads (`DefenderTrade` or `BadPieceLiquidation`),
and the row builder validates those payloads against packet source/family and
witness terms before using their squares. Packets without the matching typed
exact proof fail supported-local exchange-ownership admission and do not project
public exchange-ownership rows; exact-proof/term mismatches do not fall back to
generic exchange ownership. Queen-trade rows require the typed
`PlayerFacingExactSliceProof.QueenTradeShield` to match the packet and mirror
every typed line move in packet witness terms. These rows cannot certify truth
from raw exchange labels, generic branch labels, term-mismatched packet
evidence, malformed square-prefixed branch terms, or tactical truth-mode
packets. Defender/bad-piece row wording may repeat only the parsed exchange,
defender/target, and bad-piece squares from the typed proof terms.
The shared `RelationWitness` read-model is not a new truth shortcut. The
implemented relation observations inventoried in `CommentaryPipelineSSOT.md`
may support selector ranking or practical prose only after legal replay over
the current FEN proves the local attack/defense relation. Semantic relation
producers share a context-owned relation-witness list assembled from the
six-ply standard top-PV replay plus the dedicated twelve-ply/probe-PV
draw-resource exception, so relation support rows are not produced from
divergent per-producer replay, duplicate draw-resource projection, or local
kind-filtering attempts. They do not certify owner
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
producer admission. The runtime inventory keeps a non-public deferred list for
motif families that are present in adjacent model or detector assets but lack a
board-replayed relation witness; that list is currently empty. Deferred entries,
when present, are not cataloged selector rows, not relation producer inputs, and
not frontend authority tokens. Fallback projection is not truth admission.
`stalemate_trap` is a public relation only through its
analyzer-owned PV witness: the played move must start the bounded legal replay,
the replay must end in actual stalemate, and the engine score must stay
draw-stable with no mate score. `perpetual_check` is a public relation only
through its analyzer-owned PV witness: the played move must start as a legal
check, the line must contain at least three checks by the same side, a
repetition-compatible position key must return inside the replay, and the engine
score must stay draw-stable with no mate score. Raw draw-resource motif text,
detector labels, unvalidated probe lines, unbound probe candidate moves, and
helper notation remain PV-only support and are not truth admission. A validated
root `ProbeResult` reply PV can feed this draw-resource witness only when its
FEN and probed/candidate move bind to the reviewed position and played move. The
top-PV version of those draw-resource witnesses may also project bounded
MoveReview summary support rows (`Stalemate resource` or `Perpetual check`);
validated probe reply PVs remain relation metadata unless another product path
consumes them. The
`trapped_piece` relation is public only through its analyzer-owned board
witness: the reviewed move must legally replay, the moved attacker must attack a
valid explicit target, the target must be a higher-value enemy non-pawn/non-king
piece, and the target must have no legal escape that preserves the piece outside
attack by the pressure side. Generic trapped-piece activity, raw motif text, and helper
notation remain below truth admission.
For these implemented relation descriptors whose raw motif tags are
witness-only, the analyzer projection boundary also requires typed
`RelationDetails`; raw focus/fact strings on a `RelationWitness` cannot become
semantic relation evidence or a player-facing relation row.
The `domination` relation is public only through an analyzer-owned board witness:
the reviewed move must legally replay, the moved attacker must attack a valid
explicit same/lower-value target, every pseudo-escape square must be controlled
by the pressure side, the target must have no legal safe escape, and ray
pin/skewer geometry remains owned by line-relation witnesses. Raw domination
motifs, helper notation, and generic restriction prose are not truth admission.
The supported-local `Trapped piece`, `Domination`, and `Zwischenzug` rows are
only surface labels over those existing relation witnesses; they do not admit a
generic mobility-cage or move-order claim. Generic advanced relation metadata
may still describe the same relation family, but a practical supported-local
row for the same relation kind suppresses that duplicate advanced row.
The `zwischenzug` relation is public only through an analyzer-owned board
witness: the reviewed move must legally replay as the first move, explicit
targets must parse to a non-empty expected recapture square set, the side to
move must have had a legal non-pawn capture on that square before the reviewed
move, the reviewed move must not be that capture, and the moved piece must give
direct check. Broader `MoveAnalyzer.detectZwischenzug` motif output and raw
`Zwischenzug(...)` helper notation are not truth admission.
Mate-net practical labels are also only surface labels over analyzer-owned
relation witnesses. A `Smothered mate`, `Arabian mate`, `Boden's mate`,
`Anastasia's mate`, `Hook mate`, or `Corner mate` row requires the same legal
top-PV `MateNet` witness and the matching `TacticalPatternDetectors` id in the
typed mate-pattern details; raw detector vocabulary or motif notation is not
truth admission, and the label does not create a new proof family.
Discovered-attack practical rows follow the same analyzer-owned boundary: the
reviewed move must be the legal first top-PV move, the typed witness must name
the long-range attacker, cleared square, and newly attacked target, and the row
does not create owner proof.
Defender-trade and bad-piece trade practical rows also follow that
analyzer-owned boundary. A defender-trade fallback row needs the legal top-PV
capture/capture/recapture branch, a declared focal or current weakness-profile
target that is not the exchange square, and typed proof that the defender count
for that target is strictly reduced after the recapture. A bad-piece trade
fallback row needs the legal top-PV bad-bishop liquidation branch and the typed
bad-piece details. A queen-trade fallback row needs the legal first two top-PV
plies to be the reviewed queen capture and the opponent king's recapture on
that same square. These rows are lower-authority `PracticalPlan` support; they
do not certify exact owner truth for the exchange family.
The legacy motif-prefix table, theme-keyword path, canonical motif-term path, and
motif appears/fades delta prose use the same catalog boundary, emitting
only softer non-relation practical/thematic text for deferred lanes when present
or suppressing witness-only raw relation tags such as `trapped_piece`,
`domination`, `zwischenzug`, `stalemate_trap`, and `perpetual_check`, so
deferred relation names and witness-only raw relation tags cannot become
truth claims through prose or `conceptSummary` aliasing.
Legacy plan evidence still falls back to `key-square restriction` for raw
domination-style material instead of naming `domination` as a plan proof term.
That fallback label is catalog-owned through the implemented descriptor's
witness-only fallback field. User-facing helper-notation cleanup follows the
same boundary: deferred practical/thematic helpers are rewritten to fallback
wording when present, `Zwischenzug(...)` is rewritten to the descriptor's
`move-order caution` fallback, and PV-only `StalemateTrap(...)` and
`PerpetualCheck(...)` helpers plus mate-pattern-only `SmotheredMate(...)` are
suppressed rather than softened into named public relation labels.
Threat-summary labels follow that catalog boundary as well: deferred relation
motifs from `ThreatAnalysis` may only surface as softer fallback wording when
available, not as raw relation names. PV-only draw-resource motif tags such as
`stalemate_trap` and `perpetual_check` do not get raw public threat-summary
labels.
Candidate tactic evidence uses the same catalog-owned witness-only fallback
labels for raw `Domination` and `TrappedPiece` motifs; those annotations are
support prose only and do not create relation truth.
Strategy-pack and structure-arc piece-activity evidence does not convert generic
trapped-piece activity into a relation fallback term;
raw `trapped_piece` relation labels require the board-replayed relation witness.
Cached or legacy strategy-pack support metadata and move-review ledger
prerequisites receive the same sanitizer treatment: deferred relation motif
terms, stale `deferred_*` fallback tokens, and witness-only raw relation tags
are stripped from evidence lists, long-term focus, side-plan priorities and risk
triggers, and directional target reasons and prerequisites, while ordinary
non-deferred support evidence can remain lower authority.
Deferred relation tags and witness-only raw relation tags also cannot raise
generic context prose into a high-tension tactical opening frame without an
implemented relation witness, non-deferred motif, or actual threat evidence, and
they are not generic motif-prefix signals for future consumers. Generic fact
corroboration follows the same boundary, so an ordinary
tactical fact cannot certify a witness-only raw relation tag as board-proven motif
support. The witness-only flag and any short fallback label used by threat
summaries are descriptor-owned catalog data; consumers must not maintain their
own relation label lists. The
catalog inventory can name both implemented and deferred motif families, but
truth admission is still descriptor-based and descriptor lookup resolves only
implemented board-replayed relations. The selector evidence carrier mirrors
that rule by preserving relation identity/focus only for implemented catalog
kinds; unknown or uncataloged names cannot survive into a relation candidate.
Unknown relation witnesses do not become target-pressure or other selector
evidence; missing catalog entries fail closed.
The relation semantic producer consumes the implemented catalog relation set, so
producer reachability follows the routing catalog.
`StrategyPackBuilder` may preserve a relation-only selected idea so the support
metadata reaches the MoveReview surface, but that carrier does not promote the
relation into proof/source/family truth.
When a supported-local practical row already exposes the same relation kind,
`moveReviewPlayerSurface` treats that practical row as the concrete display and
does not duplicate it with a generic strategic-relation advanced row. Exact
defender-trade and bad-piece-liquidation owner packets may expose that relation
metadata on the summary row after resolver admission and exact-slice packet
matching. This is a surface precedence rule only; it does not alter relation
truth, owner proof, or catalog admission.
Cached MoveReview payloads pass through the same backend sanitizer rule after
summary-row sanitization: only practical-plan authority or exact defender-trade
/ bad-piece-liquidation summary `strategic_relation` authority with a matching
label, catalog token, and square target can suppress a matching advanced
`strategic_relation` token, so stale unauthoritative labels do not erase valid
relation metadata.
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
Every implemented relation support row requires matching typed relation
details. If details are missing, or present but not for the same relation kind,
the relation support row is rejected; raw witness focus, target, or fact fields
are not a projection fallback for implemented relation kinds.
Semantic observation admission consumes the analyzer-owned
`relationProjectionFromWitness` carrier after that check, so downstream
relation rows receive one canonical kind/focus/target/fact projection instead
of reconstructing truth from raw witness strings.
`moveReviewPlayerSurface` may expose those implemented relation rows only as
`strategic_relation` support metadata. The relation token and required target
square are public display metadata, not proof-family/source truth and not a
frontend permission to rebuild omitted relation claims from prose. Projection
requires the catalog source ref, the matching semantic observation fact, the
descriptor-owned analyzer witness fact, and a relation-focus-derived target; a
source-only or source-plus-semantic-only idea remains internal support.
The surface may show up to four implemented relation rows, but row count is not
truth evidence; each row still needs its own catalog source/semantic/witness
admission triple, relation-specific focus, and bounded analyzer witness.
The row sentence may describe draw resources, move-order turns, key-square
restriction, line geometry, or a tactical motif, but that wording is not a new
truth source and cannot widen the catalog token, focus, or target.
`RelationObservationCatalog` owns that source/semantic/witness admission helper and the
selected-`relationKind` no-fallback rule, so downstream builders do not repeat
or widen relation matching. It also owns public target fallback for selected
relations that lack an analyzer target, so surface projection does not recreate
motif-specific target order from raw relation strings or promote a generic
`targetSquare` into relation target. Each relation descriptor exposes its own source ref,
semantic fact, and required witness fact, keeping selector prioritization, surface priority, and
public projection on the same catalog admission tokens. The catalog may order
PV-backed draw-resource relations ahead of generic relation rows inside the
display cap so stalemate-trap and perpetual-check support is not hidden, but
this ordering is not truth signoff and does not bypass the legal top-PV replay
or draw-stability gates. Practical fallback replay uses the normalized reviewed
UCI and the already-certified first `BoundedReplayStep`; raw `playedMove` text
is not reparsed as a stricter or looser truth source after the replay match.
Standard relation witnesses remain bounded to the
six-ply relation replay prefix; the only deeper relation replay exception is
the twelve-ply draw-resource prefix for stalemate-trap and perpetual-check
witnesses.
Relation row prose selection is also descriptor-typed: the row kind, not the
display label, chooses draw-resource, move-order, mobility-restriction,
line-geometry, or tactical-relation wording. Descriptor-specific relation rows
may name only the analyzer-projected focus-order squares already carried by the
selected relation witness; that display detail does not recover piece roles,
branch ownership, or conversion truth from raw strings. Defender-trade or
bad-piece wording in this row remains support-only; exact owner proof still
requires the dedicated typed owner packet.
Line-geometry relation rows may likewise name focus-order squares as
role-neutral geometry only; pin/skewer rows do not claim absolute pins,
material wins, axes, or piece roles unless another typed public contract admits
that stronger claim.
Relation witness support facts cannot manufacture catalog source or semantic admission tokens:
dynamic facts reject semantic observation ids, evidence-source ids,
proof-source ids, proof-family ids, and `source:` wire keys. Only the catalog
descriptor may provide the relation source ref, matching semantic fact, and
required witness fact.
When the analyzer supplies a relation target, that square is carried through
semantic evidence and selected idea metadata to the surface row only while it
remains bound to the relation focus squares; descriptor target fallback may use
that same relation-focus ordering, but generic idea focus and legacy
`targetSquare` metadata are not truth evidence.
When multiple relation observations merge into one selected idea, the selector
keeps a representative `relationKind`; the surface row uses that kind only with
its matching catalog source, semantic fact, and required witness fact, so
catalog ordering cannot invent a different public relation. The selector also preserves relation-specific focus
squares for that representative relation, so public relation prose does not use
focus squares borrowed from another merged relation witness. A selected relation
kind without relation-specific focus remains unprojected instead of borrowing
merged idea focus. Selector merge does not synthesize relation focus from
generic `focusSquares` or relation target from generic `targetSquare`; missing
relation-focus stays missing, while missing relation-target evidence may use
catalog fallback over the selected relation's own focus at the public surface.
It also prioritizes the representative relation's source ref, semantic fact, and
required witness fact before evidence truncation, keeping the catalog admission
triple intact while other support refs remain lower priority.
The same selected-`relationKind`, catalog-evidence, raw-carrier stripping, and
target-required lane rules apply at backend sanitization and cached/legacy
frontend decoding; the summary-lane exception is limited to exact defender-trade
and bad-piece-liquidation owner rows, so frontend code neither widens nor
narrows backend relation authority.
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

Resolved compensation display may appear in `moveReviewPlayerSurface` only as
bounded `practical_plan` support. The truth object is the resolved compensation
display contract from `StrategyPackSurface` plus
`CompensationDisplayPhrasing`'s strong-anchor eligibility, not raw
`signalDigest.compensation`, the word `initiative`, or prose about material.
Those rows may explain why material can wait and what concrete pressure must
remain only when the strict subtype is durable pressure, but they do not certify
investment ownership, conversion truth, strategic-plan truth, or compensation
after a blunder/failed-truth boundary. The support condition row prefers the
resolved persistence wording over the broader objective fallback. When a
`DecisiveTruthContract` is
present with `compensationProseAllowed=false`, compensation rows stay closed
even if raw strategy-pack data still contains a resolved compensation surface.
Unresolved subtype paths,
transition-only conversion stories, non-durable tactical windows, and generic
compensation shells stay silent on the player surface.
Restricted-defense conversion display may appear in `moveReviewPlayerSurface`
only as bounded `practical_plan` support from a certified
`RestrictedDefenseConversionProof.Contract` admitted by
`ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision`. The truth
object is the typed contract's direct best-defense, same-branch persistence,
compressed defender-resource, and continuing suppression facts, not the plan
name, generic conversion prose, or the engine PV alone. This row does not
certify global conversion truth, compensation truth, or investment ownership.
The same bad-truth surface veto applies to MoveReview player support rows:
blunder, missed-win, bad tactical-refutation, or tactical-failure contracts
close strategic relation, practical plan, opening-family, compensation, and
supported-local support rows instead of mixing plan-positive support with a
failure explanation. `DecisiveTruthContract.blocksStrategicSupport` is the
runtime predicate for that boundary and for tactical-failure planner scene
ownership. The MoveReview prose compression basic-explanation lane also honors
that predicate: positive `opening_goal`, `certified_strategy_support`, and
`basic_move_explanation` sources do not surface when the contract blocks
strategic support, so fallback prose remains tactical or exact-board factual.

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
- MoveReview player support projects strategic/practical support beside a
  blunder, missed-win, bad tactical-refutation, or tactical-failure truth
  contract
- strategic intent is invented for a low-confidence or tactical-first failure

## Scoped Takeaway Boundary

`MoveReviewScopedTakeaway` is local instruction only. It must be tied to:

- reviewed UCI
- post-move FEN
- same coupled PV branch
- evidence tier
- guardrail tags

It may project into the existing MoveReview learning-point compatibility field,
but it is not broad lesson authority and must not generalize into broad rules.

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
