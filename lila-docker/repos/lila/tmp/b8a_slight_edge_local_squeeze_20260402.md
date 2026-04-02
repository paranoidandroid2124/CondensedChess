# B8a Slight-Edge Local Squeeze Design + Corpus Validation

Date: 2026-04-02

Scope:
- design + corpus validation only
- no runtime path, no public schema, no new owner lane, no new prose family
- mixed worktree, so this is a `B8 design+validation slice-level verdict`, not product acceptance

Required docs used first:
- `modules/llm/docs/CommentaryProgramMap.md`
- `modules/llm/docs/CommentaryPipelineSSOT.md`
- `modules/llm/docs/CommentaryTruthGate.md`
- `modules/llm/docs/CommentaryTrustHardening.md`

Parallel workstreams:
- Agent A: local fixture miner across exact-FEN tests and strategic fixtures
- Agent B: local PGN / artifact miner across QC manifests, reports, and PGN catalog
- Agent C: web-assisted discovery for externally recoverable exact-PGN/FEN leads
- Agent D: failure-taxonomy hunter for B1/B6/B7/release/fortress/reinflation relabel drift
- Main agent: shortlist, reruns, contract synthesis, matrix, verdict

Local verification commands actually run:
- `sbt -batch "llm/Test / runMain lila.llm.analysis.runTransitionRouteCorpusWorkup --ids=b21_modern_benoni_target_fixing,shirov_kinsman_1990_second_weaknesses,rubinstein_duras_1911_trade_shell,b6_route_chain_near_miss,lucena_blocker --depth=16 --multipv=4"`
- `sbt -batch "llm/Test / runMain lila.llm.analysis.runTransitionRouteCorpusWorkup --ids=b5_queen_infiltration_shell,b6_route_chain_near_miss,k09b_trigger_d4e6,b21_modern_benoni_target_fixing --depth=16 --multipv=4"`
- `sbt -batch "llm/Test / runMain lila.llm.analysis.runTaskShiftReviewWorkup B21 K09A K09B K09F"`
- direct local Stockfish UCI reruns from exact FEN on:
  - `YAN_Qa5`
  - `BOC_Ke7`
  - `MEN_h6`
  - `B21_Nd2`
  - `RUB_Qd2`
  - `SHI_f4`

Web-assisted discovery was used only for candidate discovery / PGN recovery hints.
Final verdicts below use only local exact FEN + local Stockfish lines.

## 1. B8 candidate slice map

| candidate_slice | expected_positive_scope | product_value | trust_risk | reusable_signals | main_failure_modes | surface_risk | verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sector_local_active_plan_collapse` | one bounded local sector in late middlegame, same defended branch, defender goes from 2+ active families to at most 1 holding family for a bounded continuation | high | high | reply MultiPV, same-branch key, future snapshot, release inventory, route continuity | progress visibility missing; other-wing escape; heavy-piece leakage; family relabel drift | very high | `only plausible first slice, but blocked by corpus` |
| `local_weakness_complex_squeeze` | one fixed weakness complex where the defender's repair plans collapse | medium | high | target fixation, weakness persistence, continuation motifs | drifts to generic target fixing or B2/B4 restraint | high | `deferred` |
| `local_file_entry_squeeze` | one file or entry point where access gets reduced to holding only | medium | very high | B4 file usability, entry denial, same-branch persistence | this is B4/B6 truth under another name | very high | `unsafe as B8 owner slice` |
| `piece_improvement_denial_squeeze` | one bounded piece-improvement route gets reduced to passive shuffling | medium | high | route denial, reroute inventory, branch key | collapses into B6 route/reroute denial or generic waiting-move rhetoric | high | `deferred` |
| `fake_tactical_freeze` | line looks quiet, but real story is forcing release or tactical refutation | low | very high | tactical release inventory, `newThreatKinds`, forcing-defense checks | tactical freeze, PV paraphrase, heavy-piece leakage | very high | `reject` |
| `generic_unpleasantness` | side looks uncomfortable with no measured local resource loss | low | extreme | none beyond generic eval drift | unpleasantness inflation; no measurable collapse | extreme | `reject` |
| `whole_board_no_good_moves` | broad `no good moves` or global squeeze thesis | low | extreme | none admissible | whole-board overclaim; impossible containment on first contact | extreme | `reject` |

Narrowest plausible unit after this pass:
- `sector_local_active_plan_collapse`

Why it is still blocked:
- exact slight-edge candidates do not show a clean drop to holding-only on the root-best branch
- when collapse does look visible, the position usually drifts into B6 route truth, B1 conversion follow-through, B7 task-shift rhetoric, heavy-piece release, or static-fortress ambiguity

## 2. Backend-only local-squeeze contract draft

Status:
- backend-only draft only
- no public schema
- no owner-path opening
- no positive runtime admission from this draft

| field | role in B8 draft | first-slice rule |
| --- | --- | --- |
| `claimScope` | local, non-winning, one bounded sector only | must hard-cap the claim to one local sector and forbid whole-board wording |
| `localZone` | name the defended sector or weakness complex | must be one sector, not a global shell |
| `preMoveActivePlans` | enumerate defender active families before the move | must be concrete and board-specific, not prose vibes |
| `postMoveActivePlans` | enumerate defender active families after the move on the root-best branch | must come from the exact best-defense branch |
| `collapsedPlanFamilies` | active families that disappear after the move | must be non-empty for any future positive reopening |
| `holdingOnlyFamilies` | remaining families that survive only as passive guarding | must be explicitly separated from active counterplay |
| `candidateCounterplayFamilies` | still-live active families after the move | if non-empty in the same local sector, fail positive B8 |
| `bestDefenseBranchKey` | one exact defended branch identity | mandatory; no cross-branch stitching |
| `sameDefendedBranch` | yes/no proof that trigger, defense, and continuation stay on one branch | must be `true` |
| `continuationBound` | bounded continuation window where the collapse remains visible | must be explicit; no open-ended future story |
| `squeezeWitness` | move-linked witness of local plan collapse | must be opponent-facing and local |
| `releaseSuppressionWitness` | witness that tactical/heavy-piece release does not remain the real story | mandatory before any positive wording |
| `otherWingEscapeRisk` | risk that another sector still gives the defender real play | any live other-wing counterplay keeps B8 fail-closed |
| `routeBindRelabelRisk` | risk that the truth is really route/reroute denial | high risk fails to B6 or exact factual |
| `conversionRelabelRisk` | risk that the truth is really same-job conversion follow-through | high risk fails to B1 or exact factual |
| `tacticalFreezeRisk` | risk that the shell is just PV-level discomfort or a forcing freeze | high risk fails closed |
| `surfaceContainmentMarker` | containment boundary for any future reuse | if ever reopened, start at planner `WhyThis` only |
| `claimCertification` | `candidate`, `deferred`, `fail_close` | current pass yields only `deferred` or `fail_close` |
| `confidence` | bounded confidence from measured evidence only | no prose-derived confidence |
| `evidenceSources` | exact FEN, trigger, best defense, continuation, local corpus pointers | must stay auditable |

Current design conclusion:
- existing `slightEdgeLocalSqueeze` draft in `CommentaryTrustHardening.md` was directionally right
- first-slice work needs explicit pre/post active-plan enumeration to be meaningful
- even with that addition, the current corpus does not justify positive reopening

## 3. Active-plan taxonomy draft

Closed vocabulary draft for validation only:

| plan_family | chess meaning | why B8 needs it | active vs passive cut |
| --- | --- | --- | --- |
| `break_activation` | a freeing pawn break or lever in the local sector | measures whether the defender still has a real release | active if it reopens files/squares; passive if it only holds a pawn chain |
| `entry_creation` | opening or occupying an entry square, file, or rank | many fake squeezes still leave a clean entry plan | active if it creates a new access square or file; passive if it only guards an existing square |
| `piece_improvement` | rerouting a piece to a better active square in the zone | captures cases where the squeeze is supposed to kill improvement routes | active if the reroute changes pressure or access; passive if it is mere guarding shuffle |
| `countertarget_creation` | creating an immediate target or counter-pressure point | detects whether the defender still makes the attacker answer something | active if it creates a fresh duty; passive if it only protects a weakness |
| `release_operation` | tactical or heavy-piece release such as rook lift, infiltration, perpetual, exchange sac, or forcing checks | main anti-B8 guard against fake squeezes | active if it changes the forcing status or geometry; passive if it merely exchanges into an equal hold |
| `holding_setup` | consolidating pieces and pawns to hold the sector | needed because B8 is about active plans collapsing into this residue | active never; this is the baseline passive family |
| `route_repair` | rebuilding a denied route through a reroute square or detour | separates B6-style route truth from B8 | active if the reroute restores travel or access; passive if it only freezes the route in place |
| `trade_relief` | simplifying into an easier hold or clearer technical task | catches B1/B7 relabel drift | active if it changes the job or materially improves the hold; passive if it is just a neutral exchange |
| `other_wing_counterplay` | real activity in another sector or wing | prevents local blindness | active if it creates practical play elsewhere; passive if it is only prophylaxis on the far wing |

Working validation rule:
- B8 needs at least one concrete family from `preMoveActivePlans` to disappear
- B8 also needs every surviving local family except `holding_setup` to be either gone or explicitly downgraded
- if the surviving truth is route repair, trade relief, or release operation, the row is almost always B6, B1/B7, or not-B8

## 4. Family distinction table

| family | what truth it owns | evidence shape | must not claim | common relabel drift | fail-close rule |
| --- | --- | --- | --- | --- | --- |
| `B1 restricted-defense conversion` | same conversion job still works because defender resources are compressed on the surviving branch | direct best defense, same-branch persistence, conversion follow-through | must not claim the job changed | favorable simplification renamed as a new squeeze | if same job still governs, keep it in B1 |
| `B6 named route-network bind` | one bounded route/reroute network is denied on the same defended branch | named route nodes/edges, reroute denial, same-branch continuity | must not claim a new technical task beyond route loss | route denial renamed as local squeeze | if owned truth is route loss or reroute denial, keep it in B6 |
| `B7 task shift after simplification` | one bounded job ends and a different bounded job now governs the same branch | exact trigger, best defense, continuation, closed task pair | must not claim generic good ending or generic squeeze | favorable trade or route truth renamed as task shift | if `currentTask != shiftedTask` is not proven, fail out of B7 |
| `B8 slight-edge local squeeze` | one bounded local sector loses active defender plan families and degrades toward holding-only on the root-best branch | pre/post active-plan sets, same-branch best defense, bounded continuation, progress visibility | must not claim whole-board no-moves, winning route, route-network truth, or task-shift truth | generic unpleasantness, B6 route bind, B1 conversion, B7 task shift | if active-plan collapse is not measurable, keep it deferred or exact factual |

## 5. Positive-control shortlist

These were the best exact-FEN candidates worth rerunning, not certified survivors.

| id | source | exact_fen | trigger | why shortlisted |
| --- | --- | --- | --- | --- |
| `YAN_Qa5` | local QC manifest / PGN | `2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28` | `Qa5` (`d2a5`) | cleanest local slight-edge quiet reroute already mined under `long_structural_squeeze` |
| `BOC_Ke7` | local QC manifest / PGN | `3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27` | `Ke7` (`f8e7`) | most mature local seed and exact-FEN recoverable from QC |
| `B21_Nd2` | local fixture | `rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9` | `Nd2` (`f3d2`) | clean local target-fixing / restraint control in slight-edge territory |
| `RUB_Qd2` | external exact-FEN lead already in local workup | `r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27` | `Qd2` (`d3d2`) | external candidate with local exact FEN and quiet-pressure shell |
| `SHI_f4` | external exact-FEN lead already in local workup | `8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1` | `f4` (`f3f4`) | classic second-weakness flavor with local exact FEN already available |

Dropped before survivor review:
- `MEN_h6` because the exact line is already much stronger than a slight edge and keeps tactical/forcing content front and center

## 6. Validation matrix

Notes:
- `same_branch=yes` means trigger, best defense, and bounded continuation were read from one exact local Stockfish branch
- rows marked as B5/B6/B7 blockers are negative controls, not positive candidates
- plan-family labels are manual abstractions from exact FEN plus engine lines; they are not runtime semantics

| candidate_id | source | exact_fen | trigger | best_defense | same_branch | preMoveActivePlans | postMoveActivePlans | collapsedFamilies | holdingOnlyFamilies | otherWingEscapeRisk | releaseSuppression | distinct_from_B1 | distinct_from_B6 | distinct_from_B7 | containment_risk | verdict | fail_reason |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `YAN_Qa5` | local QC manifest | `2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28` | `Qa5` | `...Be6` | yes | `piece_improvement; countertarget_creation; break_activation` | `piece_improvement; countertarget_creation; trade_relief` | `none` | `none` | `medium` | `no` | `yes` | `unclear` | `yes` | `high` | `fail_close` | slight-edge row equalizes and black keeps multiple active families |
| `BOC_Ke7` | local QC manifest | `3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27` | `Ke7` | `Rc8` | yes | `entry_creation; piece_improvement; trade_relief` | `entry_creation; piece_improvement; trade_relief` | `none` | `none` | `medium` | `no` | `yes` | `yes` | `yes` | `high` | `fail_close` | white still has several rook-activity plans; no holding-only downgrade |
| `B21_Nd2` | local fixture | `rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9` | `Nd2` | `...Re8` | yes | `piece_improvement; entry_creation; countertarget_creation` | `piece_improvement; entry_creation; countertarget_creation` | `none` | `none` | `high` | `partial` | `yes` | `yes` | `yes` | `high` | `fail_close` | target-fixing/prophylaxis signal only; black retains several active plans |
| `RUB_Qd2` | external exact-FEN lead | `r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27` | `Qd2` | `...Rc7` | yes | `piece_improvement; trade_relief; break_activation` | `piece_improvement; trade_relief; break_activation` | `none` | `none` | `medium` | `no` | `yes` | `yes` | `yes` | `medium` | `fail_close` | pressure is real, but this is clearly-better pressure with no measurable local plan collapse |
| `SHI_f4` | external exact-FEN lead | `8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1` | `f4` | `...gxf4` | yes | `release_operation; break_activation; piece_improvement` | `release_operation; trade_relief; piece_improvement` | `none` | `none` | `medium` | `no` | `yes` | `yes` | `yes` | `medium` | `fail_close` | too strong and too release-heavy to certify as a slight-edge local squeeze |
| `MEN_h6` | local QC manifest | `2r1bk2/R5pp/1pp1p3/4Np2/1PPPp3/4P1P1/5P1P/6K1 b - - 0 28` | `...h6` | `h4` | yes | `release_operation; break_activation; trade_relief` | `break_activation; entry_creation; release_operation` | `none` | `none` | `medium` | `no` | `yes` | `yes` | `yes` | `high` | `fail_close` | winning/forcing geometry dominates; not slight-edge and not holding-only |
| `B6_route_chain` | repo negative control | `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` | `b4` | `...a5` | yes | `route_repair; other_wing_counterplay; piece_improvement` | `route_repair; other_wing_counterplay; piece_improvement` | `entry_creation` | `none` | `high` | `no` | `yes` | `no` | `yes` | `high` | `fail_close_to_B6` | owned truth is reroute denial, not local active-plan collapse |
| `B5_queen_infiltration` | repo negative control | `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24` | `c4` | `...g6` | yes | `release_operation; countertarget_creation; piece_improvement` | `release_operation; countertarget_creation; piece_improvement` | `none` | `none` | `high` | `no` | `yes` | `yes` | `yes` | `high` | `fail_close` | heavy-piece release and queen infiltration remain the real story |
| `K09B_Nxe6` | repo negative control | `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13` | `Nxe6` | `...fxe6` | yes | `trade_relief; technical_conversion_pressure; piece_improvement` | `trade_relief; technical_conversion_pressure` | `piece_improvement` | `none` | `low` | `partial` | `no` | `yes` | `no` | `high` | `fail_close_to_B7` | exact row is task-shift / simplification-adjacent, not B8 |
| `lucena_blocker` | repo negative control | `2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1` | `Rb1` | `...Kd6` | yes | `king_route; rook_activity; checking_distance` | `king_route; rook_activity; checking_distance` | `none` | `none` | `low` | `no` | `yes` | `yes` | `yes` | `extreme` | `fail_close` | pure-endgame inflation; fortress/king-route ambiguity dominates |

## 7. Engine-backed validation summary

Root-best / same-branch takeaways:
- `YAN_Qa5`: root-best trigger is quiet and slight-edge enough, but after `Qa5` black still has `...Be6`, `...h5`, `...Kg7`, and other active continuations. No measurable collapse to holding-only.
- `BOC_Ke7`: exact move is root-best enough to keep in review, but after `...Ke7` white still keeps several rook-entry and king-activity families. No active-plan collapse.
- `B21_Nd2`: best local fixture seed for slight-edge restraint, but exact lines remain target-fixing/prophylaxis with plural black activity families.
- `RUB_Qd2` and `SHI_f4`: both are good exact-FEN web/local leads, but they either live in clearly-better pressure rather than slight-edge squeeze or keep too much release/tactical energy.

Negative-control reruns:
- `B5_queen_infiltration_shell`: heavy-piece leakage survives
- `B6_route_chain_near_miss`: same-branch reroute denial survives as B6 truth
- `K09B_Nxe6`: simplification-adjacent task-shift relabel remains B7-side
- `B21_Nd2` and `YAN_Qa5`: best local inflation checks; both fail because the defender still has active plans

Independent-survivor count:
- exact-FEN survivors that satisfy the proposed B8 burden: `0`
- plausible shortlist rows worth rerunning: `5`
- rows that remain genuinely B8-adjacent after rerun: `0`

## 8. Self-critique pass

Applied objections:
- `unpleasantness inflation`: local discomfort repeatedly appeared without concrete defender-resource worsening
- `whole-board overclaim`: several rows were easy to narrate as `no good moves`, but exact lines never justified that
- `route-bind relabel`: the best clean local shell was still B6 route/reroute truth
- `task-shift relabel`: simplification rows drifted into B7
- `conversion relabel`: stronger pressure rows drifted into B1-style follow-through
- `other-wing blindness`: multiple rows kept a live escape or counterplay family elsewhere
- `heavy-piece leakage`: queen/rook release stayed live in the heavy-piece cases
- `line-preview drift`: current local QC seeds often survived only as support-only or exact-factual rows, not as an owned strategic truth
- `survivor scarcity honesty`: after local fixtures, QC seeds, and external exact-FEN leads, there are still no two independent survivors; current count is zero

What got narrower after self-critique:
- first-slice unit is no longer `local weakness squeeze`, `file squeeze`, or `piece-improvement squeeze`
- the only admissible draft cell left is `sector_local_active_plan_collapse`
- even that cell remains blocked because current corpus does not prove the collapse on exact root-best branches

## 9. Step verdict

Verdict:
- `B8 still too fuzzy / more corpus work needed`

One-sentence reason:
- the only plausible B8 unit is one same-branch sector-local active-plan collapse, but current exact-FEN corpus reruns do not produce two independent survivors and instead fail on progress visibility, surviving active counterplay, or relabel drift into B1/B6/B7.
