# B-Track Foundation Direct UCI Spot Check

Date: 2026-04-02

Scope:
- foundation-only B7/B8 verification support
- no runtime path, no public schema, no owner-lane opening, no prose-family opening
- mixed worktree, so this is a `B-track foundation slice-level` artifact only

Environment notes:
- `sbt` reruns were attempted but blocked by a Windows boot-server lock on
  `\\.\pipe\sbt-load92837960475134927_lock`
- `sbtn` could connect to a stale JDK 11 server, but that server failed Scala 3
  `-java-output-version 21`
- direct local `STOCKFISH_BIN` UCI spot checks still worked, so root-best and
  immediate defended-branch gates were rechecked locally from exact FEN
- these UCI spot checks do not replace the richer repo replay harness; they are
  best read as a current-session refresh of root-best / best-defense gates
  against the already-recorded same-branch matrices in
  `CommentaryTrustHardening.md` and
  `b8a_slight_edge_local_squeeze_20260402.md`

UCI settings used:
- engine: `Stockfish 17.1`
- source: `STOCKFISH_BIN`
- depth: `14`
- multipv: `3`
- method:
  - root analysis from exact FEN
  - second pass from exact FEN plus the candidate trigger move
  - short delay before `quit` so final `info depth ... pv ...` lines are emitted

## B7 gate refresh

| id | exact_fen | candidate_trigger | root_best | trigger_is_best | best_defense_after_trigger | root_pv1 |
| --- | --- | --- | --- | --- | --- | --- |
| `K09A` | `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13` | `Qb3` (`c2b3`) | `Nxe6` (`d4e6`) | `no` | `Nxe6` (`d4e6`) | `d4e6 f7e6 c3b5 ...` |
| `K09B` | `r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13` | `Nxe6` (`d4e6`) | `Nxe6` (`d4e6`) | `yes` | `...fxe6` (`f7e6`) | `d4e6 f7e6 c3b5 a8c8 g2h3 ...` |
| `K09F` | `2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14` | `Nxe6` (`d4e6`) | `Nxe6` (`d4e6`) | `yes` | `...fxe6` (`f7e6`) | `d4e6 f7e6 c3b5 a7a6 ...` |
| `MI2` | `r1b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12` | `Nb5` (`d4b5`) | `Nxc6` (`d4c6`) | `no` | `...a6` (`a7a6`) | `d4c6 b7c6 e3d4 ...` |
| `MI3` | `2b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12` | `Nb5` (`d4b5`) | `Nf3` (`d4f3`) | `no` | `...Ng4` (`f6g4`) | `d4f3 c8f5 h2h3 ...` |
| `B21` | `rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9` | `Nd2` (`f3d2`) | `O-O` (`e1g1`) | `no` | `...Re8` (`f8e8`) | `e1g1 f8e8 f3d2 ...` |
| `B21A` | `rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1` | `Nd2` (`f3d2`) | `Nd2` (`f3d2`) | `yes` | `...Nd7` (`b8d7`) | `f3d2 b8a6 f2f3 ...` |
| `K09I` | `1r1q1rk1/pp3ppp/2n2n2/3p1b2/3P4/2N2N1P/PP2BPP1/R2Q1RK1 w - - 1 13` | local B7 hunt lead | `Bb5` (`e2b5`) | n/a | n/a | `e2b5 d8a5 f1e1 ...` |
| `Rubinstein-Duras` | `r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27` | external historical lead | `Kf2` (`e3f2`) | n/a | n/a | `e3f2 a8b8 d3d1 ...` |
| `Shirov-Kinsman` | `8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1` | external historical lead | `h4` (`h2h4`) | n/a | n/a | `h2h4 g5h4 g3h4 ...` |
| `Alekhine-Vidmar` | `1r3k2/2n2ppp/2B1p3/8/RP6/4P3/5PPP/6K1 w q - 0 1` | external historical lead | `b5` (`b4b5`) | n/a | n/a | `b4b5 b8d8 a4a1 ...` |

Observed B7 consequences:
- `K09B` still passes the narrow root-best trigger gate and still gets the exact
  defended reply `...fxe6`
- `K09A` remains preparatory because the candidate trigger is not root-best
- `MI2`, `MI3`, and `B21` still fail before any bounded handoff story because
  their candidate triggers are not root-best
- `K09F` still has the trigger, but it remains only a near-miss shell rather
  than a clean second survivor
- `K09I` plus the three historical leads still fail the root-best
  simplification gate, so the second-survivor burden remains unmet

## B8 gate refresh

| id | exact_fen | candidate_trigger | root_best | trigger_is_best | best_defense_after_trigger | root_pv1 |
| --- | --- | --- | --- | --- | --- | --- |
| `YAN_Qa5` | `2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28` | `Qa5` (`d2a5`) | `Qf4` (`d2f4`) | `no` | `...Kg7` (`g8g7`) | `d2f4 c8e6 f4d4 ...` |
| `BOC_Ke7` | `3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27` | `Ke7` (`f8e7`) | `Ke7` (`f8e7`) | `yes` | `...Qb8`/rook-file counterplan equivalent root reply from follow-up pass (`d8b8`) | `f8e7 d8c8 g7g6 ...` |
| `RUB_Qd2` | `r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27` | `Qd2` (`d3d2`) | `Kf2` (`e3f2`) | `no` | `...Rc7` (`b7c7`) | `e3f2 a8b8 d3d1 ...` |
| `SHI_f4` | `8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1` | `f4` (`f3f4`) | `h4` (`h2h4`) | `no` | `...Bf7` (`g6f7`) | `h2h4 g5h4 g3h4 ...` |
| `B6_route_chain` | `2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24` | `b4` (`a3b4`) | `Nb4` (`c3b4`) | `no` | `...a5` (`a7a5`) | `c3b4 c6e7 g2g4 ...` |
| `B5_queen_infiltration` | `2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24` | `c4` (`c2c4`) | `Nc4` (`c3c4`) | `no` | `Nc4` (`c3c4`) | `c3c4 d5c4 c2c4 ...` |
| `lucena_blocker` | `2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1` | `Rb1` (`a1b1`) | `Rd1` (`a1d1`) | `no` | `...Ke6` (`e7e6`) | `a1d1 h2b2 d1e1 ...` |

Observed B8 consequences:
- `BOC_Ke7` is the only checked B8 shortlist row whose candidate trigger stayed
  root-best in this spot check
- `YAN_Qa5`, `RUB_Qd2`, and `SHI_f4` again fail already at the trigger gate
- the negative controls still fail exactly where expected:
  - `B6_route_chain` stays route/reroute shaped
  - `B5_queen_infiltration` stays heavy-piece release shaped
  - `lucena_blocker` stays pure-endgame inflation

## Tooling implications

Current test/tooling runners still leave a gap for B7/B8 foundation work:

- `runTaskShiftReviewWorkup`
  - good for planner/runtime owner inspection
  - no engine depth / MultiPV knobs
  - no structured branch-key / trigger / defense summary
  - no machine-readable output
- `runTransitionRouteCorpusWorkup`
  - good for exact FEN + engine + replay feature checks
  - has `--ids`, `--depth`, `--multipv`
  - still only outputs generic replay features and PV columns
  - does not emit B7 task-pair fit fields
  - does not emit B8 pre/post active-plan family diffs

Most useful test/tooling-only next step if the environment is repaired:
- keep runtime closed
- add one structured B-track foundation dump that combines:
  - root-best trigger check
  - best-defense reply
  - same-branch replay key
  - B7 task-pair fields
  - B8 pre/post active-plan-family fields
  - machine-readable TSV/JSON output
