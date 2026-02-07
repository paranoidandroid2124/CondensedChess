# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/BookCommentaryCorpus.json`
- Results: 40/40 passed

## Quality Summary

- Avg quality score: 100.0 / 100
- Avg lexical diversity: 0.780
- Avg variation-anchor coverage: 1.000
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 0 across 0 cases

## Cross-Case Repetition

- [5 cases] "from a practical standpoint white has the easier roadmap"
- [5 cases] "the position is about level"
- [4 cases] "white can press with a comparatively straightforward conversion scheme"
- [4 cases] "neither side has stabilized a lasting edge"
- [4 cases] "counterplay exists for both sides"
- [4 cases] "we are firmly in endgame territory"
- [4 cases] "white has the easier side to press with"
- [4 cases] "white is just a touch better"
- [4 cases] "precision is required in this endgame"
- [3 cases] "it s close to equal with play for both sides"
- [3 cases] "it drives the initiative"
- [3 cases] "plans and tactics start to bite"

## philidor_h6: Philidor / Legal's-mate motif: 4...h6?

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 3 1`
- Metrics: 551 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.826, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=18, scoreTokens=5
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
4 ... h6?
Definitely an unnecessary pawn move! ...
5 Nxe5!!
Now if Black captures the knight with 5 ... dxe5 White replies with 6 Qxg4 ...
```

**Generated commentary**

```text
4... h6: The initial deployment phase is not over yet. It's close to equal, with play for both sides. Key theme: **keeping the structure flexible**. The pin on f7 makes that pawn awkward to handle.

One practical question: Does this fall into the Legal’s Mate?

a) Nf6 d4 Be7 O-O (+0.3)
b) Nc6 h3 Bh5 d4 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+1.0)

Instead of **h6**, stronger is **Nf6** (70 cp). Better is **Nf6**; it improves the piece's scope.

**Nc6** heads for a controlled position with fewer tactical swings.

Neither side has stabilized a lasting edge.
```

## philidor_h6_no_legals_mate: Philidor-ish: 4.d3 ...h6?! (no Legal's mate)

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 1`
- Metrics: 421 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.774, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=10, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=17, scoreTokens=4
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... h6: Early skirmishes are shaping the plans. The position is near parity. Strategic focus: **keeping the pawn chain intact**.

a) Nf6 O-O Be7 c3 (+0.2)
b) Nc6 c3 Nf6 h3 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+0.6)

Instead of **h6**, stronger is **Nf6** (40 cp). Better is **Nf6**; it prepares for the next phase.

**Nc6** heads for a controlled position with fewer tactical swings.

Neither side has stabilized a lasting edge.
```

## qid_tension_release: Queen's Indian: early tension release (9.cxd5)

- `ply`: 17
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/3p4/2PP1B2/2N2NP1/PP2PPBP/R2QK2R w KQkq - 0 1`
- Metrics: 630 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.828, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=10, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=14, scoreTokens=3
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=keep_tension_branches, branches=2

**Reference excerpt (for human comparison)**

```text
Best practice is often to maintain the uncertainty (tension) in the centre as long as possible, developing for both scenarios. After an early release of tension, the cramped side can often place pieces on their most natural squares.
```

**Generated commentary**

```text
9. cxd5: Early skirmishes are shaping the plans. White has the easier side to press with. The most reliable roadmap here is built around **postponing pawn commitments until pieces are ready**. The pawn on c4 is hanging (1 attacker, no defenders).

A key question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) 9... dxc4 10. Qc2 Nbd7 (+0.2)
b) 9... c5 (+0.3)

Instead of **cxd5**, stronger is **O-O** (10 cp). Better is **O-O**; it completes the king's evacuation.

**Ne5** favors structural clarity and methodical handling over complications.

The position allows methodical play without forcing tactics.
```

## startpos_e4_best: Sanity check: best move still shows alternatives (1.e4)

- `ply`: 1
- `playedMove`: `e2e4`
- `analysisFen`: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`
- Metrics: 621 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.756, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=17, scoreTokens=3
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
1. e4: Piece coordination and development are still in progress. The position is near parity. Current play is organized around **central space and control**.

a) e4 c5 Nf3 d6 (+0.2)
b) d4 d5 c4 e6 (+0.2)
c) c4 e5 Nc3 Nf6 (+0.1)

**e4** is an excellent choice, following the main plan. It fights for the center. **e4** lowers immediate tactical volatility and rewards precise coordination.

**d4** favors structural clarity and methodical handling over complications.
**c4** takes the game into another strategic channel around central control.

The position stays tense, and one inaccurate tempo can swing the initiative.
```

## ruy_c3_prepare_d4: Ruy Lopez Exchange: 7.c3! (preparing d4)

- `ply`: 13
- `playedMove`: `c2c3`
- `analysisFen`: `r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1`
- Metrics: 715 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.708, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=17, scoreTokens=3
- Outline: 7 beats (MoveHeader, Context, Evidence, MainMove, OpeningTheory, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
7 c3!
Preparing to play d4 and gain control of the centre.
...
```

**Generated commentary**

```text
7. c3: The initial deployment phase is not over yet. White can keep up mild pressure (≈+0.4). The most reliable roadmap here is built around **dominating the centre**. The d-pawn break is a thematic lever in this structure.

a) c3 O-O d4 Bd6 (+0.4)
b) Nbd2 O-O d4 exd4 (+0.3)
c) h3 h6 Nbd2 O-O (+0.2)

A strong move: **c3** aligns with the position's demands. It maintains central tension. **c3** often leads to a stable structure with clear plans.

We are now in the Ruy Lopez: Exchange Variation (412 games, White scores 50%).

**Nbd2** is the cleaner technical route, aiming for a stable structure.
**h3** points to a different strategic plan around central control.

Plans are relatively clear for both players.
```

## london_cxd4_recapture: London System: early ...cxd4 (recapture choice)

- `ply`: 10
- `playedMove`: `c5d4`
- `analysisFen`: `rnbqkb1r/pp3ppp/4pn2/2pp4/3P1B2/2P1PN2/PP3PPP/RN1QKB1R b KQkq - 0 1`
- Metrics: 590 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.756, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=recapture_branches, branches=2

**Generated commentary**

```text
5... cxd4: A sharp deflection to lure the pieces away! This opening has moved beyond quiet setup ideas. It's close to equal, with play for both sides. Strategic priority: **maintaining pawn tension**. Keep the pawn tension; the c-pawn break is a useful lever.

A key question: After cxd4, how should White recapture —exd4 or cxd4?

a) 6. exd4 Nc6 (+0.2)
b) 6. cxd4 Nc6 (+0.3)

**cxd4** is slightly imprecise; stronger is **Qb6**. Better is **Qb6**; it maintains the tension.

**Nc6** favors structural clarity and methodical handling over complications.

It is easy to misstep if you relax.
```

## catalan_open_dxc4: Open Catalan: early ...dxc4 (pawn grab)

- `ply`: 8
- `playedMove`: `d5c4`
- `analysisFen`: `rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 1 1`
- Metrics: 701 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.728, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=20, scoreTokens=3
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... dxc4: Development remains the first priority. The position is about level. Strategic focus: **holding the center without clarifying too early**. The pawn on f2 is pinned to the knight on g1.

A key question: Should Black release the central tension with dxc4?

a) dxc4 Nf3 Be7 O-O (+0.1)
b) Be7 Nf3 O-O O-O (+0.2)
c) c5 dxc5 Bxc5 Nf3 (+0.2)

A strong move: **dxc4** aligns with the position's demands. It opens the position. With **dxc4**, planning depth tends to matter more than short tactics.

**Be7** is the cleaner technical route, aiming for a stable structure.
**c5** takes the game into another strategic channel around positional maneuvering.

Neither side has stabilized a lasting edge.
```

## reversed_sicilian_cxd5: Reversed Sicilian (English): early tension release (cxd5)

- `ply`: 7
- `playedMove`: `c4d5`
- `analysisFen`: `rnbqkb1r/ppp2ppp/5n2/3pp3/2P5/2N3P1/PP1PPP1P/R1BQKBNR w KQkq - 0 1`
- Metrics: 681 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.720, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=21, scoreTokens=4
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4. cxd5: It is early, but the strategic contours are visible. The position is about level. Most sensible plans here converge on **maintaining pawn tension**. The pawn on c4 is hanging (1 attacker, no defenders). Releasing the tension can make the c-pawn break more potent.

A key question: After cxd5, how should Black recapture —...Nxd5 or...Qxd5?

a) Nf3 Nc6 cxd5 Nxd5 (+0.2)
b) cxd5 Nxd5 Nf3 Nc6 (+0.2)
c) d4 exd4 Qxd4 Nc6 (+0.1)

Instead of **cxd5**, stronger is **Nf3** (5 cp). Better is **Nf3**; it solidifies the position.

**d4** is the cleaner technical route, aiming for a stable structure.

Both sides retain tactical resources, so concrete move-order precision matters.
```

## greek_gift_sac: Greek Gift Sacrifice: 1.Bxh7+ (Tactical explosion)

- `ply`: 15
- `playedMove`: `d3h7`
- `analysisFen`: `r1bq1rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 589 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.778, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Bxh7+: A classic Greek Gift sacrifice! Both kings still need care as the position sharpens. White is a bit better (≈+1.5). Move-order choices are justified by **avoiding premature pawn breaks**.

A key question: Does this fall into the Greek Gift Sacrifice?

a) Bxh7+ Kxh7 Qh5+ (+1.5)
b) O-O b6 Re1 (+0.3)

**Bxh7+** is thematic and accurate. It drives the initiative. The line after **Bxh7+** is relatively clean and technical, with less tactical turbulence.

**O-O** is playable, but significant disadvantage (1.2) after b6.

From a practical standpoint, White has the easier roadmap.
```

## exchange_sac_c3: Sicilian Defense: ...Rxc3 (Strategic Exchange Sacrifice)

- `ply`: 20
- `playedMove`: `c8c3`
- `analysisFen`: `2r1k2r/pp2ppbp/3p1np1/q7/3NP3/2N1BP2/PPPQ2PP/R3KB1R b KQk - 0 1`
- Metrics: 665 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.758, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
10... Rxc3: The hanging pawns in the center create significant tension! The initial deployment phase is not over yet. The position is about level. Key theme: **postponing pawn commitments until pieces are ready**.

A key question: After Rxc3, how should White recapture — bxc3 or Qxc3?

a) Rxc3 bxc3 O-O Bd3 (-0.2)
b) O-O Be2 (+0.3)

A strong move: **Rxc3** aligns with the position's demands. It improves the piece's scope. After **Rxc3**, the game trends toward a controlled strategic struggle.

**O-O** is the cleaner technical route, aiming for a stable structure.

A single tempo can swing the position. White gets a Positional return for the material deficit.
```

## iqp_blockade: Isolated Queen Pawn: Blockading d4/d5

- `ply`: 15
- `playedMove`: `d5c4`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 b - - 0 1`
- Metrics: 686 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.758, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. dxc4: The battle revolves around the isolated queen's pawn! Opening choices now can trigger forcing sequences. It's close to equal, with play for both sides. Move-order choices are justified by **bishop Sacrifice**. With tension preserved, the d-pawn break becomes a thematic lever.

One practical question: Should Black release the central tension with dxc4?

a) dxc4 Bxc4 b6 a3 (+0.2)
b) b8a6 (+0.3)

A strong move: **dxc4** aligns with the position's demands. It opens the position. After **dxc4**, the game trends toward a controlled strategic struggle.

**b8a6** heads for a controlled position with fewer tactical swings.

Precise defensive choices are needed to keep equality.
```

## minority_attack_carlsbad: Minority Attack in Carlsbad structure (b4-b5)

- `ply`: 18
- `playedMove`: `a2a3`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 0 1`
- Metrics: 512 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.806, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... a3: White launches a minority attack on the queenside! The opening phase is still fluid. White can keep up mild pressure. Strategic priority: **preserving the central pawn structure**.

a) a3 b6 b4 (+0.3)
b) b4 cxb4 (+0.3)

**a3** is an excellent choice, following the main plan. It repositions the piece. The line after **a3** is relatively clean and technical, with less tactical turbulence.

**b4** favors structural clarity and methodical handling over complications.

Counterplay exists for both sides.
```

## bad_bishop_endgame: Bad Bishop vs Good Knight: 1.Nd4 (Conversion)

- `ply`: 40
- `playedMove`: `c4d6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 684 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.703, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
20... Nd6+: Black is burdened by a bad bishop, restricted by its own pawn structure! Precision is required in this endgame. White has a comfortable plus (≈+4.0). Key theme: **keeping the pawn chain intact**. A fork motif is in the air: d6 can attack pawn on b7 and king on e8.

A key question: How should White convert the advantage after Nd6+?

a) Nd6+ Kd7 Nxb7 (+4.0)
b) f3 Kd7 Kf2 (+1.5)

A strong move: **Nd6+** aligns with the position's demands. It continues the attack. With **Nd6+**, progress is mostly about methodical coordination.

**f3** can work; the tradeoff is that decisive loss (2.5) after Kd7.

White can press with a comparatively straightforward conversion scheme.
```

## zugzwang_pawn_endgame: Pawn Endgame: Zugzwang (Precision)

- `ply`: 60
- `playedMove`: `e3d4`
- `analysisFen`: `8/8/6p1/5pP1/5P2/4K3/8/3k4 w - - 0 1`
- Metrics: 649 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.745, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
30... Kd4: A pure zugzwang scenario: each legal move worsens the defender's coordination. Precision is required in this endgame. White is winning (≈+10.0). Key theme: **King Activation**. King activity matters: e3 has 7 safe steps.

A key question: How should White convert the advantage after Kd4?

a) Kd4 Kd2 Ke5 Ke3 (+10.0)
b) Kf3 Ke1 Kg3 Kf1 (+0.0)

**Kd4** is an excellent choice, following the main plan. It improves the piece's scope. The line after **Kd4** is clean and technical, where subtle king routes and tempi matter.

**Kf3** is playable, but decisive loss (10.0) after Ke1.

From a practical standpoint, White has the easier roadmap.
```

## prophylaxis_kh1: Prophylaxis: 1.Kh1 (Deep defense)

- `ply`: 20
- `playedMove`: `h1g1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 561 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.887, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=4
- Outline: 8 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
10... Rg1: A deep prophylactic move to shore up the defense! Middlegame complications begin to arise. White has a modest edge. Key theme: **restricting counterplay through central presence**.

a) g1h1 (+0.3)
b) h4 f5 h5 (+0.3)
c) Rg1 (-0.3)

Instead of **Rg1**, stronger is **g1h1** (55 cp). Issue: slight inaccuracy (0.6). Better is **g1h1**; it maintains central tension. Defending against a phantom threat, wasting a valuable tempo.

**h4** heads for a controlled position with fewer tactical swings.

Defensive technique matters more than raw activity here.
```

## interference_tactic: Interference Tactic: Cutting off defense

- `ply`: 25
- `playedMove`: `d1d8`
- `analysisFen`: `3r2k1/pp3ppp/4p3/1b6/4P3/P4P2/BP4PP/3R2K1 w - - 0 1`
- Metrics: 668 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.729, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13. Rxd8+: A tactical interference cutting off the defensive line! We are firmly in endgame territory. White has a decisive advantage. The most reliable roadmap here is built around **Mating Attack**. The rook on d1 is hanging (1 attacker, no defenders).

Worth asking: How should White convert the advantage after Rxd8+?

a) Rxd8+ Be8 Rxe8# (+20.0)
b) a4 Ba6 b4 (+0.2)

**Rxd8+** is thematic and accurate. It drives the initiative. The line after **Rxd8+** is clean and technical, where subtle king routes and tempi matter.

**a4** can work; the tradeoff is that decisive loss (19.8) after Ba6.

White can press with a comparatively straightforward conversion scheme.
```

## passed_pawn_push: Passed Pawn: 1.d6 (Structural push)

- `ply`: 30
- `playedMove`: `d5d6`
- `analysisFen`: `3r2k1/pp3ppp/8/3P4/8/8/PP3PPP/3R2K1 w - - 0 1`
- Metrics: 556 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... d6: The passed pawn is a constant threat that must be addressed. In this endgame, piece activity is paramount. White can play for two results. The practical roadmap centers on **Passed Pawn Advance**.

a) d6 Kf8 f4 Ke8 (+1.2)
b) Kf1 Kf8 Ke2 (+0.3)

A strong move: **d6** aligns with the position's demands. It advances the trumping pawn. The line after **d6** is clean and technical, where subtle king routes and tempi matter.

Practically speaking, **Kf1** is viable, but significant disadvantage (0.9) after Kf8.

White has the easier game to play.
```

## deflection_sacrifice: Deflection: 1.Qd8+ (Forcing move)

- `ply`: 28
- `playedMove`: `d5d8`
- `analysisFen`: `6k1/pp3ppp/4r3/3Q4/8/5P2/PP4PP/6K1 w - - 0 1`
- Metrics: 659 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.755, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
14... Qd8+: A sharp deflection to lure the pieces away. Endgame technique will decide a lot here. White is completely on top (≈+20.0). Strategic focus: **Mating Attack**. The king on g1 is well-placed for the endgame.

A key question: How should White convert the advantage after Qd8+?

a) Qd8+ Re8 Qxe8# (+20.0)
b) g3 h6 Kg2 (+0.2)

**Qd8+** is an excellent choice, following the main plan. It poses serious questions. The line after **Qd8+** is clean and technical, where subtle king routes and tempi matter.

Practically speaking, **g3** is viable, but decisive loss (19.8) after h6.

White can press with a comparatively straightforward conversion scheme.
```

## king_hunt_sac: King Hunt: 1.Bxf7+ (Luring the king)

- `ply`: 12
- `playedMove`: `c4f7`
- `analysisFen`: `r1bqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 660 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.760, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=16, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
6... Bxf7+: A relentless king hunt begins! Opening choices now can trigger forcing sequences. White can play for two results (≈+2.5). Key theme: **bishop Sacrifice**. The pawn on d7 is pinned to the queen on d8.

A key question: What is the defensive task here — can White meet the threat with Bxf7+ (watch c6)?

a) Bxf7+ Kxf7 Ng5+ Kg8 (+2.5)
b) O-O d6 exd6 (+0.4)

A strong move: **Bxf7+** aligns with the position's demands. It continues the attack. After **Bxf7+**, the game trends toward a controlled strategic struggle.

Practically speaking, **O-O** is viable, but decisive loss (2.1) after d6.

From a practical standpoint, White has the easier roadmap.
```

## battery_open_file: Rook Battery: Doubling on the d-file

- `ply`: 22
- `playedMove`: `d1d2`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 520 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.812, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
11... R1d2: A powerful battery on the open file exerts immense pressure! Middlegame complications begin to arise. White is just a touch better. Strategic priority: **Rook Activation**.

a) R1d2 b6 (+0.4)
b) a3 b6 b4 (+0.3)

**R1d2** is thematic and accurate. It stops the enemy ideas. **R1d2** lowers immediate tactical volatility and rewards precise coordination.

**a3** favors structural clarity and methodical handling over complications.

The position stays tense, and one inaccurate tempo can swing the initiative.
```

## bishop_pair_open: Bishop Pair: Dominance in open position

- `ply`: 35
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 597 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.780, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=4
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
18. Rxe8#: The bishop pair exerts immense pressure on the open board. We are firmly in endgame territory. White has the easier side to press with. Move-order choices are justified by **Forced Mate**. The king on g1 is well-placed for the endgame.

Worth asking: Does this fall into the Back Rank Mate?

a) Rxe8# (+0.5)
b) Bd4 a6 Rd1 (+0.4)

**Rxe8#** is an excellent choice, following the main plan. It poses serious questions. **Rxe8#** ends the game sequence on the spot.

**Bd4** favors structural clarity and methodical handling over complications.

Neither side has stabilized a lasting edge.
```

## hanging_pawns_center: Hanging Pawns: c4/d4 (Structural tension)

- `ply`: 15
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1p2pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 741 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.741, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=16, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. cxd5: The hanging pawns in the center create significant tension! It is early, but the strategic contours are visible. The position is about level. Move-order choices are justified by **Minority Attack**. The pawn on c4 is hanging (1 attacker, no defenders). With tension preserved, the c-pawn break becomes a thematic lever.

Worth asking: After cxd5, how should Black recapture —...exd5,...Nxd5, or...Bxd5?

a) cxd5 exd5 dxc5 bxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

A strong move: **cxd5** aligns with the position's demands. It opens the position. **cxd5** often leads to a stable structure with clear plans.

**b3** is the cleaner technical route, aiming for a stable structure.

The practical chances are still shared between both players.
```

## opposite_bishops_attack: Opposite-Colored Bishops: Attacking the King

- `ply`: 30
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3p1p/2b3p1/8/8/P1B2B2/1P3PPP/4R1K1 w - - 0 1`
- Metrics: 592 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.788, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... Rxe8+: Opposite-colored bishops signal an attack on the king! Small details matter in this endgame. White is just a touch better (≈+0.6). Strategic priority: **postponing pawn commitments until pieces are ready**. King activity matters: g1 has 2 safe steps.

a) Rxe8+ Bxe8 Bxb7 (+0.6)
b) Bxc6 bxc6 Rxe8# (+0.6)

**Rxe8+** is thematic and accurate. It poses serious questions. The line after **Rxe8+** is clean and technical, where subtle king routes and tempi matter.

**Bxc6** favors structural clarity and methodical handling over complications.

The position is dynamically balanced.
```

## simplification_trade: Simplification: Trading into a won endgame

- `ply`: 45
- `playedMove`: `d5b5`
- `analysisFen`: `4r1k1/1p3ppp/8/3R4/8/P4P2/1P4PP/6K1 w - - 0 1`
- Metrics: 616 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.747, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
23. Rb5: Endgame technique and simplification will be the decisive factors. The position simplifies into an endgame. White is a bit better (≈+1.5). The most reliable roadmap here is built around **postponing pawn commitments until pieces are ready**. The king on g1 is active (mobility: 3).

a) Rb5 Re7 Kf2 f6 (+1.5)
b) Kf2 f6 Rb5 (+1.4)

**Rb5** is an excellent choice, following the main plan. It prevents counterplay. With **Rb5**, progress is mostly about methodical coordination.

**Kf2** is the cleaner technical route, aiming for a stable structure.

From a practical standpoint, White has the easier roadmap.
```

## zugzwang_mutual: Mutual Zugzwang: Pawn Endgame (Precision)

- `ply`: 55
- `playedMove`: `d4c4`
- `analysisFen`: `8/8/p1p5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 592 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.872, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=7, scoreTokens=3
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
A pure zugzwang scenario: each legal move worsens the defender's coordination. Precision is required in this endgame. The position is about level. Move-order choices are justified by **King Activation**. The king on d4 is active (mobility: 7).

a) Kc3 (+0.0)
b) Ke4 (+0.0)
c) d4c4 (-0.5)

**Kc3**! prepares for the next phase. It remains a precise choice. Neither side has a clear advantage.

**Ke4** favors structural clarity and methodical handling over complications.
**d4c4** points to a different strategic plan around positional maneuvering.

One slip can change the evaluation quickly.
```

## smothered_mate_prep: Smothered Mate Prep: 1.Nb5 (Tactical motif)

- `ply`: 15
- `playedMove`: `c3b5`
- `analysisFen`: `r1bq1rk1/pp1n1ppp/2n1p3/2ppP3/3P1B2/2N5/PPP1QPPP/2KR1BNR w - - 0 1`
- Metrics: 522 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.892, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=3
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Nb5: Setting up a potential smothered mate. This opening has moved beyond quiet setup ideas. White has a modest edge. Move-order choices are justified by **Piece Activation**. Keep b5 in mind as an outpost square.

a) Nb5 a6 Nd6 (+0.5)
b) Nf3 (+0.3)

**Nb5** is thematic and accurate. It establishes a strong outpost. **Nb5** aims for a tidy continuation with fewer forcing turns.

**e1c1** favors structural clarity and methodical handling over complications.

The position is easier to navigate than it first appears.
```

## knight_domination_endgame: Knight Domination: 1.Nb6 (Outplaying a bishop)

- `ply`: 42
- `playedMove`: `c4b6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 548 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.822, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
21... Nb6: The knight dominates its counterpart on this outpost! We are firmly in endgame territory. White has the more pleasant position (≈+1.8). Strategic priority: **Minority Attack**. b6 can serve as an outpost for a knight.

a) Nb6 (+1.8)
b) f3 Kd7 Kf2 (+1.5)

A strong move: **Nb6** aligns with the position's demands. It establishes a strong outpost. **Nb6** guides play into a precise conversion phase.

**f3** favors structural clarity and methodical handling over complications.

From a practical standpoint, White has the easier roadmap.
```

## opening_novelty_e5: Opening Novelty: Handling an early ...e5 break

- `ply`: 10
- `playedMove`: `e1g1`
- `analysisFen`: `rnbqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 596 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.719, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
5... O-O: An interesting opening novelty that changes the character of the game. The game is still in opening channels. White is just a touch better. The practical roadmap centers on **keeping central levers available for later timing**. The structure often turns on the e-pawn break, a lever to keep in mind.

a) O-O d6 exd6 (+0.4)
b) Qd5 O-O O-O (+0.4)

**O-O** is thematic and accurate. It castles to safety. **O-O** often leads to a stable structure with clear plans.

**Qd5** favors structural clarity and methodical handling over complications.

Plans are relatively clear for both players.
```

## rook_lift_attack: Rook Lift: 1.Rd3 (Attacking the king)

- `ply`: 24
- `playedMove`: `d3d4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 551 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.793, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12... Rd4: A powerful rook lift to join the assault. We are now in the middlegame phase. White is just a touch better. Key theme: **keeping the structure flexible**. There's a pin: the pawn on d5 cannot move without exposing the knight on d7.

a) Rd4 f5 Rh4 (+0.4)
b) R1d2 b6 a3 (+0.3)

A strong move: **Rd4** aligns with the position's demands. It maintains the tension. After **Rd4**, the game trends toward a controlled strategic struggle.

**R1d2** is the cleaner technical route, aiming for a stable structure.

Counterplay exists for both sides.
```

## outside_passed_pawn: Outside Passed Pawn: a-pawn advantage

- `ply`: 58
- `playedMove`: `d4e5`
- `analysisFen`: `8/8/p1k5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 554 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.829, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
29... Ke5: The passed pawn is a constant threat that must be addressed! We are firmly in endgame territory. White has the more pleasant position. Strategic priority: **King Activation**. The king on d4 is active (mobility: 7).

a) Ke5 Kc7 Kd5 (+1.0)
b) Kc3 Kd5 Kd3 (+0.0)

**Ke5** is thematic and accurate. It repositions the piece. **Ke5** guides play into a precise conversion phase.

Practically speaking, **Kc3** is viable, but significant disadvantage (1.0) after Kd5.

Both sides retain tactical resources, so concrete move-order precision matters.
```

## zwischenzug_tactics: Zwischenzug: 1.h4 (Tactical intermediate)

- `ply`: 26
- `playedMove`: `h4h5`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/7P/2NRBN2/PPP2PP1/3R2K1 w - - 0 1`
- Metrics: 532 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.831, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13... h5: A clever zwischenzug or intermediate move to seize the initiative. Plans and tactics start to bite. White has a modest edge (≈+0.4). The practical roadmap centers on **maintaining pawn tension**.

a) h5 h6 g4 (+0.4)
b) R1d2 b6 a3 (+0.3)

**h5** is thematic and accurate. It solidifies the position. **h5** lowers immediate tactical volatility and rewards precise coordination.

**R1d2** favors structural clarity and methodical handling over complications.

The evaluation is close enough that accuracy still matters most.
```

## pawn_break_f4: Pawn Break: 1.f4 (Kingside storm prep)

- `ply`: 21
- `playedMove`: `f2f4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 462 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.773, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=6, scoreTokens=2
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A massive pawn storm is brewing on the kingside! This is a full middlegame now. White can keep up mild pressure (≈+0.4). Current play is organized around **space advantage in the center**.

a) f2f4 (+0.4)
b) a3 b6 b4 (+0.3)

With **f2f4**!, White fights for the center. White has a modest edge. A key prophylactic benefit is preventing Tactical Threat.

**a3** is the cleaner technical route, aiming for a stable structure.

The position is dynamically balanced.
```

## trapped_piece_rim: Trapped Piece: 1.g4 (Minor piece on the rim)

- `ply`: 18
- `playedMove`: `g4h5`
- `analysisFen`: `r1bqk2r/ppppbppp/2n1p3/4P2n/2B3P1/2P2N2/PP1P1P1P/RNBQK2R w KQkq - 0 1`
- Metrics: 582 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.761, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... gxh5: A critical piece is trapped on the rim with no escape. Opening decisions now will define the middlegame plan. White is pressing with a stable edge. Strategic priority: **coordinating piece play before structural commitments**. The structure often turns on the g-pawn break, a lever to keep in mind.

a) gxh5 g6 hxg6 (+3.5)
b) h3 d6 exd6 (+0.3)

**gxh5** is thematic and accurate. It breaks through the center. With **gxh5**, planning depth tends to matter more than short tactics.

**h3** is playable, but decisive loss (3.2) after d6.

White has the easier game to play.
```

## stalemate_resource: Stalemate Trick: 1.Rh1+ (Defensive resource)

- `ply`: 65
- `playedMove`: `h2h1`
- `analysisFen`: `8/8/8/8/8/5k2/7r/5K2 b - - 0 1`
- Metrics: 597 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.759, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=4
- Outline: 8 beats (MoveHeader, Context, DecisionPoint, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
33. Rh1#: A desperate but clever stalemate resource! Precision is required in this endgame. Black has a decisive advantage. Current play is organized around **Forced Mate**. King opposition becomes a key factor.

A key question: What is the defensive task here — can Black meet the threat with Rh1# (watch d2)?

a) Rd2 Ke1 Rd1+ (-10.0)
b) Rh1# (+0.0)

A significant oversight: king cut off was available.

**Rh1#** misses the better **Rd2** (1000 cp loss). Better is **Rd2**; it continues the attack. Defending against a phantom threat, wasting a valuable tempo.

Black can play on intuition here.
```

## underpromotion_knight: Underpromotion: 1.e8=N+ (Tactical necessity)

- `ply`: 55
- `playedMove`: `e7e8n`
- `analysisFen`: `8/4P1k1/8/8/8/8/8/1K4n1 w - - 0 1`
- Metrics: 586 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.766, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
28. e8=N+: A rare and necessary underpromotion! The position simplifies into an endgame. White can play for two results. Move-order choices are justified by **Passed Pawn Advance**.

Worth asking: How should White convert the advantage after e8=N+?

a) e8=N+ Kf8 Nf6 (+2.0)
b) e8=Q Nf3 (+0.0)

**e8=N+** is an excellent choice, following the main plan. It drives the initiative. With **e8=N+**, progress is mostly about methodical coordination.

**e8=Q** is playable, but significant disadvantage (2.0) after Nf3.

White can press with a comparatively straightforward conversion scheme.
```

## kingside_pawn_storm: Pawn Storm: 1.g4 (Attacking the king)

- `ply`: 23
- `playedMove`: `g2g4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 508 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.813, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12. g4: A massive pawn storm is brewing on the kingside. Plans and tactics start to bite. White is just a touch better. Move-order choices are justified by **postponing pawn commitments until pieces are ready**.

a) g4 h6 g5 (+0.5)
b) a3 b6 b4 (+0.3)

**g4** is thematic and accurate. It provides necessary defense. **g4** often leads to a stable structure with clear plans.

**a3** heads for a controlled position with fewer tactical swings.

The evaluation is close enough that accuracy still matters most.
```

## quiet_improvement_move: Quiet Improvement: 1.Re1 (Improving the rook)

- `ply`: 25
- `playedMove`: `d3e1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 510 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.770, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=6, scoreTokens=2
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A deep prophylactic move to shore up the defense! Plans and tactics start to bite. White has the easier side to press with. Current play is organized around **a grip on the centre**.

a) d3e1 (+0.4)
b) a3 b6 b4 (+0.3)

With **d3e1**!, White fights for the center. White has a modest edge (≈+0.4). A key prophylactic benefit is preventing Tactical Threat.

**a3** favors structural clarity and methodical handling over complications.

The position stays tense, and one inaccurate tempo can swing the initiative.
```

## repetition_decision_move: Repetition: Deciding for/against a draw

- `ply`: 38
- `playedMove`: `e1d1`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 509 chars, 4 paragraphs
- Quality: score=100/100, lexical=0.792, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=3
- Outline: 7 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
19... Rd1: The game heads toward a repeat of the position. Small details matter in this endgame. The evaluation is essentially balanced. The practical roadmap centers on **holding the center without clarifying too early**. King activity matters: g1 has 3 safe steps.

a) Bd4 a6 Rd1 (+0.2)
b) Rd1 Bc6 Re1 (+0.1)

Instead of **Rd1**, stronger is **Bd4** (5 cp). Better is **Bd4**; it occupies a strong square. Defending against a phantom threat, wasting a valuable tempo.

A single tempo can swing the position.
```

## central_liquidation_move: Central Liquidation: 1.cxd5 (Clearing the center)

- `ply`: 18
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 817 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.776, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... cxd5: The central liquidation changes the character of the struggle! The initial deployment phase is not over yet. White has a modest edge. The practical roadmap centers on **keeping central levers available for later timing**. Keep an eye on the pawn on c4 — 1 attacker, no defenders. With tension preserved, the c-pawn break becomes a thematic lever.

A key question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) cxd5 exd5 dxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

A strong move: **cxd5** aligns with the position's demands. It opens the position. **cxd5** lowers immediate tactical volatility and rewards precise coordination.

**b3** is the cleaner technical route, aiming for a stable structure.

Counterplay exists for both sides. White gets a Positional return for the material deficit.
```

## final_sanity_check: Final Sanity Check: Complex position

- `ply`: 30
- `playedMove`: `a2a3`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 326 chars, 4 paragraphs
- Quality: score=100/100, lexical=0.844, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=8, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=8, scoreTokens=3
- Outline: 5 beats (MoveHeader, Context, Evidence, MainMove, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... a3: Middlegame complications begin to arise. White has the easier side to press with. The practical roadmap centers on **maintaining pawn tension**.

a) R1d2 (+0.4)
b) a3 b6 b4 (+0.3)

Instead of **a3**, stronger is **R1d2** (10 cp). Better is **R1d2**; it provides necessary defense.

Counterplay exists for both sides.
```

