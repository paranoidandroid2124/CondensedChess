# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/BookCommentaryCorpus.json`
- Results: 40/40 passed

## philidor_h6: Philidor / Legal's-mate motif: 4...h6?

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 3 1`
- Metrics: 456 chars, 7 paragraphs
- Outline: 8 beats (MoveHeader, Context, DecisionPoint, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
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
4... h6 Development is priority number one. The position is roughly equal. The main strategic idea is Pawn Chain Maintenance.

Perhaps does this fall into the Legal’s Mate?

a) Nf6 Nf6 d4 Be7 O-O (+0.3)
b) Nc6 Nc6 h3 Bh5 d4 (+0.3)
c) h6 h6 Nxe5 dxe5 Qxg4 (+1.0)

A slight mistake: the Castling was available. [Note: g8f6]

**h6** misses the better **Nf6** (70 cp loss).

**Nc6** is also playable here.
**h6** is also playable here.

The game remains tense.
```

## philidor_h6_no_legals_mate: Philidor-ish: 4.d3 ...h6?! (no Legal's mate)

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 1`
- Metrics: 409 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... h6 Development is priority number one. The position is roughly equal. The main strategic idea is Pawn Chain Maintenance.

a) Nf6 Nf6 O-O Be7 c3 (+0.2)
b) Nc6 Nc6 c3 Nf6 h3 (+0.3)
c) h6 h6 Nxe5 dxe5 Qxg4 (+0.6)

A slight mistake: the Castling was available. [Note: g8f6]

**h6** misses the better **Nf6** (40 cp loss).

**Nc6** is also playable here.
**h6** is also playable here.

The game remains tense.
```

## qid_tension_release: Queen's Indian: early tension release (9.cxd5)

- `ply`: 17
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/3p4/2PP1B2/2N2NP1/PP2PPBP/R2QK2R w KQkq - 0 1`
- Metrics: 510 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=keep_tension_branches, branches=2

**Reference excerpt (for human comparison)**

```text
Best practice is often to maintain the uncertainty (tension) in the centre as long as possible, developing for both scenarios. After an early release of tension, the cramped side can often place pieces on their most natural squares.
```

**Generated commentary**

```text
9. cxd5 Early skirmishes define the position. White has a small pull. Pawn Chain Maintenance is the critical objective.

One consideration is that after cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5? [Note: recapture choice] [Note: structure]

a)...dxc4 9... dxc4 10. Qc2 Nbd7 (+0.2)
b)...c5 9... c5 (+0.3)

Instead of **cxd5**, stronger is **O-O** (10 cp).

Alternatively, **Ne5** deserves attention.
Alternatively, **cxd5** deserves attention.

The position is easier to handle from this side.
```

## startpos_e4_best: Sanity check: best move still shows alternatives (1.e4)

- `ply`: 1
- `playedMove`: `e2e4`
- `analysisFen`: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`
- Metrics: 315 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
1. e4 Development is priority number one. The position is roughly equal. The main strategic idea is Central Control.

a) e4 e4 c5 Nf3 d6 (+0.2)
b) d4 d4 d5 c4 e6 (+0.2)
c) c4 c4 e5 Nc3 Nf6 (+0.1)

**e4** is thematic and accurate.

**d4** is also playable here.
**c4** is also playable here.

The game remains tense.
```

## ruy_c3_prepare_d4: Ruy Lopez Exchange: 7.c3! (preparing d4)

- `ply`: 13
- `playedMove`: `c2c3`
- `analysisFen`: `r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1`
- Metrics: 473 chars, 6 paragraphs
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
7. c3 Development is priority number one. White has a small pull. Strategy dictates focusing on Central Control. The focus shifts to the 3 break (Ignore).

a) c3 c3 O-O d4 Bd6 (+0.4)
b) Nbd2 Nbd2 O-O d4 exd4 (+0.3)
c) h3 h3 h6 Nbd2 O-O (+0.2)

**c3** is thematic and accurate.

The opening has transposed into the Ruy Lopez: Exchange Variation (412 games, White scores 50%).

**Nbd2** is also playable here.
**h3** is also playable here.

Practical chances favor this side.
```

## london_cxd4_recapture: London System: early ...cxd4 (recapture choice)

- `ply`: 10
- `playedMove`: `c5d4`
- `analysisFen`: `rnbqkb1r/pp3ppp/4pn2/2pp4/3P1B2/2P1PN2/PP3PPP/RN1QKB1R b KQkq - 0 1`
- Metrics: 543 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=recapture_branches, branches=2

**Generated commentary**

```text
5... cxd4 A sharp deflection to lure the pieces away. Development is priority number one. The position is roughly equal. Pursuing Pawn Chain Maintenance is the key theme. The focus shifts to the 2 break (Maintain).

It seems that after cxd4, how should White recapture — cxd4, exd4, or Nxd4? [Note: recapture choice] [Note: structure]

a)...exd4 6. exd4 Nc6 (+0.2)
b)...cxd4 6. cxd4 Nc6 (+0.3)

**cxd4** misses the better **Qb6** (5 cp loss).

**cxd4** is also playable here.
**Nc6** is also playable here.

The defensive burden is noticeable.
```

## catalan_open_dxc4: Open Catalan: early ...dxc4 (pawn grab)

- `ply`: 8
- `playedMove`: `d5c4`
- `analysisFen`: `rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 1 1`
- Metrics: 449 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... dxc4 The opening battle continues. The position is roughly equal. The position demands Pawn Chain Maintenance.

One consideration is that should Black release the central tension with dxc4?

a) dxc4 dxc4 Nf3 Be7 O-O (+0.1)
b) Be7 Be7 Nf3 O-O O-O (+0.2)
c) c5 c5 dxc5 Bxc5 Nf3 (+0.2)

**dxc4** is an excellent choice, following the main plan.

**Be7** is another good option.
**c5** is another good option.

The position is dynamically balanced.
```

## reversed_sicilian_cxd5: Reversed Sicilian (English): early tension release (cxd5)

- `ply`: 7
- `playedMove`: `c4d5`
- `analysisFen`: `rnbqkb1r/ppp2ppp/5n2/3pp3/2P5/2N3P1/PP1PPP1P/R1BQKBNR w KQkq - 0 1`
- Metrics: 537 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4. cxd5 The opening battle continues. The position is roughly equal. Strategy dictates focusing on Pawn Chain Maintenance. The central break 2 is Release.

One consideration is that after cxd5, how should Black recapture —...Nxd5 or...Qxd5? [Note: recapture choice] [Note: structure]

a) Nf3 Nf3 Nc6 cxd5 Nxd5 (+0.2)
b) cxd5 cxd5 Nxd5 Nf3 Nc6 (+0.2)
c) d4 d4 exd4 Qxd4 Nc6 (+0.1)

**cxd5** is slightly imprecise; stronger is **Nf3**.

**cxd5** is another good option.
**d4** is another good option.

The position is dynamically balanced.
```

## greek_gift_sac: Greek Gift Sacrifice: 1.Bxh7+ (Tactical explosion)

- `ply`: 15
- `playedMove`: `d3h7`
- `analysisFen`: `r1bq1rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 424 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Bxh7+ A classic Greek Gift sacrifice! Development is priority number one. White has a slight advantage. Pursuing Pawn Chain Maintenance is the key theme.

It seems that does this fall into the Greek Gift Sacrifice?

a) Bxh7+ Bxh7+ Kxh7 Qh5+ (+1.5)
b) O-O O-O b6 Re1 (+0.3)

**Bxh7+** is thematic and accurate.

**O-O** is worth considering, but significant disadvantage (1.2) after b6.

Practical chances favor this side.
```

## exchange_sac_c3: Sicilian Defense: ...Rxc3 (Strategic Exchange Sacrifice)

- `ply`: 20
- `playedMove`: `c8c3`
- `analysisFen`: `2r1k2r/pp2ppbp/3p1np1/q7/3NP3/2N1BP2/PPPQ2PP/R3KB1R b KQk - 0 1`
- Metrics: 588 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
10... Rxc3 The hanging pawns in the center create significant tension. The opening battle continues. The position is roughly equal. Pursuing Pawn Chain Maintenance is the key theme.

One consideration is that after Rxc3, how should White recapture — bxc3 or Qxc3? [Note: recapture choice] [Note: structure]

a) Rxc3 Rxc3 bxc3 O-O Bd3 (-0.2)
b) O-O O-O Be2 (+0.3)

**Rxc3** is an excellent choice, following the main plan.

**O-O** is another good option.

Practically, this side is under some pressure. Sufficient Positional Compensation provides sufficient compensation for the material.
```

## iqp_blockade: Isolated Queen Pawn: Blockading d4/d5

- `ply`: 15
- `playedMove`: `d5c4`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 b - - 0 1`
- Metrics: 436 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. dxc4 The battle revolves around the isolated queen's pawn. Development is priority number one. The position is roughly equal. bishop Sacrifice is the critical objective. The focus shifts to the 3 break (Maintain).

It seems that should Black release the central tension with dxc4?

a) dxc4 dxc4 Bxc4 b6 a3 (+0.2)
b) b8a6 (+0.3)

**dxc4** is thematic and accurate.

**b8a6** is also playable here.

The defensive burden is noticeable.
```

## minority_attack_carlsbad: Minority Attack in Carlsbad structure (b4-b5)

- `ply`: 18
- `playedMove`: `a2a3`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 0 1`
- Metrics: 306 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... a3 White launches a minority attack on the queenside. Development is priority number one. White has a small pull. Strategy dictates focusing on Pawn Chain Maintenance.

a) a3 a3 b6 b4 (+0.3)
b) b4 b4 cxb4 (+0.3)

**a3** is thematic and accurate.

**b4** is also playable here.

The game remains tense.
```

## bad_bishop_endgame: Bad Bishop vs Good Knight: 1.Nd4 (Conversion)

- `ply`: 40
- `playedMove`: `c4d6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 503 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
20... Nd6+ Black is burdened by a bad bishop, restricted by its own pawn structure. Precision is required in this endgame. White holds a clear advantage. The position demands Pawn Chain Maintenance.

One consideration is that how should White convert the advantage after Nd6+? [Note: conversion] [Note: simplify]

a) Nd6+ Nd6+ Kd7 Nxb7 (+4.0)
b) f3 f3 Kd7 Kf2 (+1.5)

**Nd6+** is thematic and accurate.

**f3** is worth considering, but decisive loss (2.5) after Kd7.

Practical chances favor this side.
```

## zugzwang_pawn_endgame: Pawn Endgame: Zugzwang (Precision)

- `ply`: 60
- `playedMove`: `e3d4`
- `analysisFen`: `8/8/6p1/5pP1/5P2/4K3/8/3k4 w - - 0 1`
- Metrics: 524 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
30... Kd4 The position is defined by zugzwang—every move weakens the defender! In this endgame, piece activity is paramount. White has a decisive advantage. Pursuing King Activation is the key theme.

Perhaps how should White convert the advantage after Kd4? [Note: conversion] [Note: simplify]

a) Kd4 Kd4 Kd2 Ke5 Ke3 (+10.0)
b) Kf3 Kf3 Ke1 Kg3 Kf1 (+0.0)

A strong move: **Kd4** aligns with the position's demands.

Alternatively, **Kf3** (decisive loss (10.0) after Ke1).

The position is easier to handle from this side.
```

## prophylaxis_kh1: Prophylaxis: 1.Kh1 (Deep defense)

- `ply`: 20
- `playedMove`: `h1g1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 519 chars, 6 paragraphs
- Outline: 9 beats (MoveHeader, Context, Evidence, ConditionalPlan, TeachingPoint, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
10... Rg1 A deep prophylactic move to shore up the defense! We are now in the middlegame phase. White has a small pull. Pursuing Central Control is the key theme.

a) g1h1 (+0.3)
b) h4 h4 f5 h5 (+0.3)
c) Rg1 Rg1 (-0.3)

A slight mistake: the inaccuracy was available. [Note: g1h1]

**Rg1** misses the better **g1h1** (55 cp loss). Defending against a phantom threat, wasting a valuable tempo.

**h4** is also playable here.
**Rg1** is worth considering, but slight inaccuracy (0.6).

The defensive burden is noticeable.
```

## interference_tactic: Interference Tactic: Cutting off defense

- `ply`: 25
- `playedMove`: `d1d8`
- `analysisFen`: `3r2k1/pp3ppp/4p3/1b6/4P3/P4P2/BP4PP/3R2K1 w - - 0 1`
- Metrics: 521 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13. Rxd8+ A tactical interference cutting off the defensive line. The position simplifies into an endgame. White has a decisive advantage. The position demands Mating Attack.

One consideration is that how should White convert the advantage after Rxd8+? [Note: conversion] [Note: simplify]

a) Rxd8+ Rxd8+ Be8 Rxe8# (+20.0)
b) a4 a4 Ba6 b4 (+0.2)

**Rxd8+** is an excellent choice, following the main plan.

**a4** is also possible, though decisive loss (19.8) after Ba6.

In practical terms, this is comfortable to play.
```

## passed_pawn_push: Passed Pawn: 1.d6 (Structural push)

- `ply`: 30
- `playedMove`: `d5d6`
- `analysisFen`: `3r2k1/pp3ppp/8/3P4/8/8/PP3PPP/3R2K1 w - - 0 1`
- Metrics: 427 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... d6 The passed pawn is a constant threat that must be addressed. In this endgame, piece activity is paramount. White has a slight advantage. Strategy dictates focusing on Passed Pawn Advance.

a) d6 d6 Kf8 f4 Ke8 (+1.2)
b) Kf1 Kf1 Kf8 Ke2 (+0.3)

A strong move: **d6** aligns with the position's demands.

Alternatively, **Kf1** (significant disadvantage (0.9) after Kf8).

The position is easier to handle from this side.
```

## deflection_sacrifice: Deflection: 1.Qd8+ (Forcing move)

- `ply`: 28
- `playedMove`: `d5d8`
- `analysisFen`: `6k1/pp3ppp/4r3/3Q4/8/5P2/PP4PP/6K1 w - - 0 1`
- Metrics: 496 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
14... Qd8+ A sharp deflection to lure the pieces away! The position simplifies into an endgame. White has a decisive advantage. Strategy dictates focusing on Mating Attack.

Perhaps how should White convert the advantage after Qd8+? [Note: conversion] [Note: simplify]

a) Qd8+ Qd8+ Re8 Qxe8# (+20.0)
b) g3 g3 h6 Kg2 (+0.2)

**Qd8+** is an excellent choice, following the main plan.

**g3** is also possible, though decisive loss (19.8) after h6.

In practical terms, this is comfortable to play.
```

## king_hunt_sac: King Hunt: 1.Bxf7+ (Luring the king)

- `ply`: 12
- `playedMove`: `c4f7`
- `analysisFen`: `r1bqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 467 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
6... Bxf7+ A relentless king hunt begins! Development is priority number one. White has a slight advantage. The main strategic idea is bishop Sacrifice.

It seems that what is the defensive task here — can White meet the threat with Bxf7+ (watch c6)? [Note: defense]

a) Bxf7+ Bxf7+ Kxf7 Ng5+ Kg8 (+2.5)
b) O-O O-O d6 exd6 (+0.4)

**Bxf7+** is thematic and accurate.

**O-O** is worth considering, but decisive loss (2.1) after d6.

Practical chances favor this side.
```

## battery_open_file: Rook Battery: Doubling on the d-file

- `ply`: 22
- `playedMove`: `d1d2`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 363 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
11... R1d2 A powerful battery on the open file exerts immense pressure. The game transitions into a complex middlegame. White has a small pull. The main strategic idea is Rook Activation.

a) R1d2 R1d2 b6 (+0.4)
b) a3 a3 b6 b4 (+0.3)

**R1d2** is an excellent choice, following the main plan.

**a3** is another good option.

The position is dynamically balanced.
```

## bishop_pair_open: Bishop Pair: Dominance in open position

- `ply`: 35
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 403 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
18. Rxe8# The bishop pair exerts immense pressure on the open board! The position simplifies into an endgame. White has a small pull. Pursuing Forced Mate is the key theme.

Possibly, does this fall into the Back Rank Mate?

a) Rxe8# Rxe8# (+0.5)
b) Bd4 Bd4 a6 Rd1 (+0.4)

**Rxe8#** is an excellent choice, following the main plan.

**Bd4** is another good option.

The position is dynamically balanced.
```

## hanging_pawns_center: Hanging Pawns: c4/d4 (Structural tension)

- `ply`: 15
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1p2pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 532 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. cxd5 The hanging pawns in the center create significant tension. The opening battle continues. The position is roughly equal. Minority Attack is the critical objective. The central break 2 is Maintain.

One consideration is that after cxd5, how should Black recapture —...exd5,...Nxd5, or...Bxd5? [Note: recapture choice] [Note: structure]

a) cxd5 cxd5 exd5 dxc5 bxc5 (+0.3)
b) b3 b3 O-O Bb2 (+0.2)

**cxd5** is an excellent choice, following the main plan.

**b3** is another good option.

The position is dynamically balanced.
```

## opposite_bishops_attack: Opposite-Colored Bishops: Attacking the King

- `ply`: 30
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3p1p/2b3p1/8/8/P1B2B2/1P3PPP/4R1K1 w - - 0 1`
- Metrics: 373 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... Rxe8+ Opposite-colored bishops signal an attack on the king. The position simplifies into an endgame. White has a small pull. The position demands Pawn Chain Maintenance.

a) Rxe8+ Rxe8+ Bxe8 Bxb7 (+0.6)
b) Bxc6 Bxc6 bxc6 Rxe8# (+0.6)

**Rxe8+** is an excellent choice, following the main plan.

**Bxc6** is another good option.

The position is dynamically balanced.
```

## simplification_trade: Simplification: Trading into a won endgame

- `ply`: 45
- `playedMove`: `d5b5`
- `analysisFen`: `4r1k1/1p3ppp/8/3R4/8/P4P2/1P4PP/6K1 w - - 0 1`
- Metrics: 391 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
23. Rb5 Endgame technique and simplification will be the decisive factors! The position simplifies into an endgame. White has a slight advantage. Pursuing Pawn Chain Maintenance is the key theme.

a) Rb5 Rb5 Re7 Kf2 f6 (+1.5)
b) Kf2 Kf2 f6 Rb5 (+1.4)

**Rb5** is an excellent choice, following the main plan.

**Kf2** is another good option.

In practical terms, this is comfortable to play.
```

## zugzwang_mutual: Mutual Zugzwang: Pawn Endgame (Precision)

- `ply`: 55
- `playedMove`: `d4c4`
- `analysisFen`: `8/8/p1p5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 475 chars, 6 paragraphs
- Outline: 6 beats (Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
The position is defined by zugzwang—every move weakens the defender. The position simplifies into an endgame. The position is roughly equal. The position demands King Activation.

a) Kc3 Kc3 (+0.0)
b) Ke4 Ke4 (+0.0)
c) d4c4 (-0.5)

Missing the KingStep was a slight oversight (50 cp). [Note: d4c3]

**Kc3**! repositions the piece. The position is roughly equal.

**Ke4** is another good option.
**d4c4** is another good option.

Practically, this side is under some pressure.
```

## smothered_mate_prep: Smothered Mate Prep: 1.Nb5 (Tactical motif)

- `ply`: 15
- `playedMove`: `c3b5`
- `analysisFen`: `r1bq1rk1/pp1n1ppp/2n1p3/2ppP3/3P1B2/2N5/PPP1QPPP/2KR1BNR w - - 0 1`
- Metrics: 354 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Nb5 Setting up a potential smothered mate! Early skirmishes define the position. White has a small pull. Strategy dictates focusing on Piece Activation.

a) Nb5 Nb5 a6 Nd6 (+0.5)
b) Nf3 Nf3 (+0.3)

A strong move: **Nb5** aligns with the position's demands.

Alternatively, **e1c1** deserves attention.

The position is easier to handle from this side.
```

## knight_domination_endgame: Knight Domination: 1.Nb6 (Outplaying a bishop)

- `ply`: 42
- `playedMove`: `c4b6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 320 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
21... Nb6 The knight dominates its counterpart on this outpost. Precision is required in this endgame. White has a slight advantage. Pursuing Minority Attack is the key theme.

a) Nb6 Nb6 (+1.8)
b) f3 f3 Kd7 Kf2 (+1.5)

**Nb6** is thematic and accurate.

**f3** is also playable here.

Practical chances favor this side.
```

## opening_novelty_e5: Opening Novelty: Handling an early ...e5 break

- `ply`: 10
- `playedMove`: `e1g1`
- `analysisFen`: `rnbqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 403 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
5... O-O An interesting opening novelty that changes the character of the game. The opening battle continues. White has a small pull. The position demands Pawn Chain Maintenance. The central break 4 is Ignore.

a) O-O O-O d6 exd6 (+0.4)
b) Qd5 Qd5 O-O O-O (+0.4)

**O-O** is an excellent choice, following the main plan.

**Qd5** is another good option.

In practical terms, this is comfortable to play.
```

## rook_lift_attack: Rook Lift: 1.Rd3 (Attacking the king)

- `ply`: 24
- `playedMove`: `d3d4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 310 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12... Rd4 A powerful rook lift to join the assault! We are now in the middlegame phase. White has a small pull. Strategy dictates focusing on Pawn Chain Maintenance.

a) Rd4 Rd4 f5 Rh4 (+0.4)
b) R1d2 R1d2 b6 a3 (+0.3)

**Rd4** is thematic and accurate.

**R1d2** is also playable here.

The game remains tense.
```

## outside_passed_pawn: Outside Passed Pawn: a-pawn advantage

- `ply`: 58
- `playedMove`: `d4e5`
- `analysisFen`: `8/8/p1k5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 410 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
29... Ke5 The passed pawn is a constant threat that must be addressed! The position simplifies into an endgame. White has a slight advantage. Pursuing King Activation is the key theme.

a) Ke5 Ke5 Kc7 Kd5 (+1.0)
b) Kc3 Kc3 Kd5 Kd3 (+0.0)

**Ke5** is an excellent choice, following the main plan.

**Kc3** is also possible, though significant disadvantage (1.0) after Kd5.

The position is dynamically balanced.
```

## zwischenzug_tactics: Zwischenzug: 1.h4 (Tactical intermediate)

- `ply`: 26
- `playedMove`: `h4h5`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/7P/2NRBN2/PPP2PP1/3R2K1 w - - 0 1`
- Metrics: 326 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13... h5 A clever zwischenzug or intermediate move to seize the initiative! We are now in the middlegame phase. White has a small pull. Pursuing Pawn Chain Maintenance is the key theme.

a) h5 h5 h6 g4 (+0.4)
b) R1d2 R1d2 b6 a3 (+0.3)

**h5** is thematic and accurate.

**R1d2** is also playable here.

The game remains tense.
```

## pawn_break_f4: Pawn Break: 1.f4 (Kingside storm prep)

- `ply`: 21
- `playedMove`: `f2f4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 400 chars, 5 paragraphs
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A massive pawn storm is brewing on the kingside! The game transitions into a complex middlegame. White has a small pull. Strategy dictates focusing on Central Control.

a) f2f4 (+0.4)
b) a3 a3 b6 b4 (+0.3)

**f2f4**! maintains central tension. White has a small pull. Crucially, this stops the opponent's idea of Tactical Threat.

**a3** is another good option.

The position is dynamically balanced.
```

## trapped_piece_rim: Trapped Piece: 1.g4 (Minor piece on the rim)

- `ply`: 18
- `playedMove`: `g4h5`
- `analysisFen`: `r1bqk2r/ppppbppp/2n1p3/4P2n/2B3P1/2P2N2/PP1P1P1P/RNBQK2R w KQkq - 0 1`
- Metrics: 456 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... gxh5 A critical piece is trapped on the rim with no escape! Early skirmishes define the position. White holds a clear advantage. Pursuing Pawn Chain Maintenance is the key theme. Preparing the 6 lever is the immediate task (Ignore).

a) gxh5 gxh5 g6 hxg6 (+3.5)
b) h3 h3 d6 exd6 (+0.3)

A strong move: **gxh5** aligns with the position's demands.

Alternatively, **h3** (decisive loss (3.2) after d6).

The position is easier to handle from this side.
```

## stalemate_resource: Stalemate Trick: 1.Rh1+ (Defensive resource)

- `ply`: 65
- `playedMove`: `h2h1`
- `analysisFen`: `8/8/8/8/8/5k2/7r/5K2 b - - 0 1`
- Metrics: 608 chars, 7 paragraphs
- Outline: 9 beats (MoveHeader, Context, DecisionPoint, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
33. Rh1# A desperate but clever stalemate resource. In this endgame, piece activity is paramount. Black has a decisive advantage. The main strategic idea is Forced Mate.

One consideration is that what is the defensive task here — can Black meet the threat with Rh1# (watch d2)? [Note: defense]

a) Rd2 Rd2 Ke1 Rd1+ (-10.0)
b) Rh1# Rh1# (+0.0)

The KingCutOff would have saved 1000 cp. [Note: h2d2]

Instead of **Rh1#**, stronger is **Rd2** (1000 cp). Defending against a phantom threat, wasting a valuable tempo.

Alternatively, **Rh1#** deserves attention.

The position is easier to handle from this side.
```

## underpromotion_knight: Underpromotion: 1.e8=N+ (Tactical necessity)

- `ply`: 55
- `playedMove`: `e7e8n`
- `analysisFen`: `8/4P1k1/8/8/8/8/8/1K4n1 w - - 0 1`
- Metrics: 503 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
28. e8=N+ A rare and necessary underpromotion! In this endgame, piece activity is paramount. White has a slight advantage. The main strategic idea is Passed Pawn Advance.

Perhaps how should White convert the advantage after e8=N+? [Note: conversion] [Note: simplify]

a) e8=N+ e8=N+ Kf8 Nf6 (+2.0)
b) e8=Q e8=Q Nf3 (+0.0)

A strong move: **e8=N+** aligns with the position's demands.

Alternatively, **e8=Q** (significant disadvantage (2.0) after Nf3).

The position is easier to handle from this side.
```

## kingside_pawn_storm: Pawn Storm: 1.g4 (Attacking the king)

- `ply`: 23
- `playedMove`: `g2g4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 344 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12. g4 A massive pawn storm is brewing on the kingside. Middlegame complications begin to arise. White has a small pull. The position demands Pawn Chain Maintenance.

a) g4 g4 h6 g5 (+0.5)
b) a3 a3 b6 b4 (+0.3)

A strong move: **g4** aligns with the position's demands.

Alternatively, **a3** deserves attention.

Both sides have their chances.
```

## quiet_improvement_move: Quiet Improvement: 1.Re1 (Improving the rook)

- `ply`: 25
- `playedMove`: `d3e1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 397 chars, 5 paragraphs
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A deep prophylactic move to shore up the defense! The game transitions into a complex middlegame. White has a small pull. Central Control is the critical objective.

a) d3e1 (+0.4)
b) a3 a3 b6 b4 (+0.3)

**d3e1**! maintains central tension. White has a small pull. Crucially, this stops the opponent's idea of Tactical Threat.

**a3** is another good option.

The position is dynamically balanced.
```

## repetition_decision_move: Repetition: Deciding for/against a draw

- `ply`: 38
- `playedMove`: `e1d1`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 427 chars, 5 paragraphs
- Outline: 8 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
19... Rd1 The game heads toward a repeat of the position. The position simplifies into an endgame. The position is roughly equal. Pursuing Pawn Chain Maintenance is the key theme.

a) Bd4 Bd4 a6 Rd1 (+0.2)
b) Rd1 Rd1 Bc6 Re1 (+0.1)

**Rd1** is slightly imprecise; stronger is **Bd4**. Defending against a phantom threat, wasting a valuable tempo.

**Rd1** is another good option.

Practically, this side is under some pressure.
```

## central_liquidation_move: Central Liquidation: 1.cxd5 (Clearing the center)

- `ply`: 18
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 631 chars, 6 paragraphs
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... cxd5 The central liquidation changes the character of the struggle. Early skirmishes define the position. White has a small pull. Pawn Chain Maintenance is the critical objective. Preparing the 2 lever is the immediate task (Maintain).

It seems that after cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5? [Note: recapture choice] [Note: structure]

a) cxd5 cxd5 exd5 dxc5 (+0.3)
b) b3 b3 O-O Bb2 (+0.2)

A strong move: **cxd5** aligns with the position's demands.

Alternatively, **b3** deserves attention.

Both sides have their chances. White has Sufficient Positional Compensation in exchange for the deficit.
```

## final_sanity_check: Final Sanity Check: Complex position

- `ply`: 30
- `playedMove`: `a2a3`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 270 chars, 5 paragraphs
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... a3 We are now in the middlegame phase. White has a small pull. Strategy dictates focusing on Pawn Chain Maintenance.

a) R1d2 R1d2 (+0.4)
b) a3 a3 b6 b4 (+0.3)

**a3** misses the better **R1d2** (10 cp loss).

**a3** is also playable here.

The game remains tense.
```

