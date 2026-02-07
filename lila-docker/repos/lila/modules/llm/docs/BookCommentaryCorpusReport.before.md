# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/BookCommentaryCorpus.json`
- Results: 40/40 passed

## Quality Summary

- Avg quality score: 97.2 / 100
- Avg lexical diversity: 0.771
- Avg variation-anchor coverage: 1.000
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 15 across 13 cases

## Cross-Case Repetition

- [7 cases] "the resulting position is fairly technical and calm"
- [6 cases] "it often simplifies into a manageable structure"
- [6 cases] "this tends to lead to a simpler position"
- [6 cases] "white is just a touch better"
- [5 cases] "the opening battle continues"
- [5 cases] "the position offers counterplay for both players"
- [4 cases] "both sides have their chances"
- [4 cases] "in practical terms white is more comfortable here"
- [4 cases] "the position is about level"
- [4 cases] "the position simplifies into an endgame"
- [4 cases] "this line reduces immediate tactical volatility"
- [3 cases] "keep an eye on the pawn on c4 1 attacker no defenders"

## philidor_h6: Philidor / Legal's-mate motif: 4...h6?

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 3 1`
- Metrics: 556 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.762, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=1, moveTokens=19, scoreTokens=5
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
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
4... h6: The opening phase continues; piece placement matters. The position is about level. Strategic priority: **keeping the pawn chain intact**. There's a pin: the pawn on f7 cannot move without exposing the knight on g8.

One practical question: Does this fall into the Legal’s Mate?

a) Nf6 d4 Be7 O-O (+0.3)
b) Nc6 h3 Bh5 d4 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+1.0)

**h6** misses the better **Nf6** (70 cp loss). Better is **Nf6**; it repositions the piece.

**Nc6** heads for a more controlled position with fewer tactical swings.

The game remains tense.
```

## philidor_h6_no_legals_mate: Philidor-ish: 4.d3 ...h6?! (no Legal's mate)

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 1`
- Metrics: 427 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.719, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=10, dup=0, boilerplate=0, moveTokens=17, scoreTokens=4
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... h6: Early skirmishes define the position. The position is about level. The plan is clear: **keeping the structure flexible**.

a) Nf6 O-O Be7 c3 (+0.2)
b) Nc6 c3 Nf6 h3 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+0.6)

Instead of **h6**, stronger is **Nf6** (40 cp). Better is **Nf6**; it improves the piece's scope.

**Nc6** heads for a more controlled position with fewer tactical swings.

It's roughly level, with play on both sides.
```

## qid_tension_release: Queen's Indian: early tension release (9.cxd5)

- `ply`: 17
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/3p4/2PP1B2/2N2NP1/PP2PPBP/R2QK2R w KQkq - 0 1`
- Metrics: 564 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.816, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=10, dup=0, boilerplate=1, moveTokens=14, scoreTokens=2
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=keep_tension_branches, branches=2

**Reference excerpt (for human comparison)**

```text
Best practice is often to maintain the uncertainty (tension) in the centre as long as possible, developing for both scenarios. After an early release of tension, the cramped side can often place pieces on their most natural squares.
```

**Generated commentary**

```text
9. cxd5: The opening battle continues. White has the easier side to press with. Play revolves around **maintaining pawn tension**. The pawn on c4 is underdefended: 1 attacker, no defenders.

A key question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) 9... dxc4 10. Qc2 Nbd7 (+0.2)
b) 9... c5 (+0.3)

**cxd5** is slightly imprecise; stronger is **O-O**. Better is **O-O**; it completes the king's evacuation.

**Ne5** favors structure and technical handling over immediate complications.

In practical terms, this is comfortable to play.
```

## startpos_e4_best: Sanity check: best move still shows alternatives (1.e4)

- `ply`: 1
- `playedMove`: `e2e4`
- `analysisFen`: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`
- Metrics: 528 chars, 5 paragraphs
- Quality: score=92/100, lexical=0.833, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=1, moveTokens=16, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
1. e4: It's still early, but plans are taking shape. The evaluation is essentially balanced. Play revolves around **central space and control**.

a) e4 c5 Nf3 d6 (+0.2)
b) d4 d5 c4 e6 (+0.2)
c) c4 e5 Nc3 Nf6 (+0.1)

A strong move: **e4** aligns with the position's demands. It fights for the center. This tends to lead to a simpler position.

**d4** favors structure and technical handling over immediate complications.
**c4** takes the game into another strategic channel around central control.

Both sides have their chances.
```

## ruy_c3_prepare_d4: Ruy Lopez Exchange: 7.c3! (preparing d4)

- `ply`: 13
- `playedMove`: `c2c3`
- `analysisFen`: `r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1`
- Metrics: 673 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.719, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=1, moveTokens=16, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
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
7. c3: The opening phase continues; piece placement matters. White has a small pull. Strategic priority: **central control**. The structure often turns on the d-pawn break, a lever to keep in mind.

a) c3 O-O d4 Bd6 (+0.4)
b) Nbd2 O-O d4 exd4 (+0.3)
c) h3 h6 Nbd2 O-O (+0.2)

**c3** is thematic and accurate. It challenges the center. This line reduces immediate tactical volatility.

The opening has transposed into the Ruy Lopez: Exchange Variation (412 games, White scores 50%).

**Nbd2** is the cleaner technical route, aiming for a simpler structure.
**h3** points to a different strategic plan around central control.

In practical terms, this is comfortable to play.
```

## london_cxd4_recapture: London System: early ...cxd4 (recapture choice)

- `ply`: 10
- `playedMove`: `c5d4`
- `analysisFen`: `rnbqkb1r/pp3ppp/4pn2/2pp4/3P1B2/2P1PN2/PP3PPP/RN1QKB1R b KQkq - 0 1`
- Metrics: 583 chars, 6 paragraphs
- Quality: score=84/100, lexical=0.714, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=2, moveTokens=12, scoreTokens=2
- Quality findings:
  - Boilerplate phrase hits: 2
- Advisory findings:
  - Advisory: quality score below target (90): 84
  - Advisory: boilerplate phrase present (2)
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=recapture_branches, branches=2

**Generated commentary**

```text
5... cxd4: A sharp deflection to lure the pieces away! Both sides are still getting pieces out. The position is roughly equal. The plan is clear: **maintaining pawn tension**. Keep the pawn tension; the c-pawn break is a useful lever.

A key question: After cxd4, how should White recapture —exd4 or cxd4?

a) 6. exd4 Nc6 (+0.2)
b) 6. cxd4 Nc6 (+0.3)

**cxd4** is slightly imprecise; stronger is **Qb6**. Better is **Qb6**; it keeps the opponent under pressure.

**Nc6** favors structure and technical handling over immediate complications.

Accuracy is required to hold the balance.
```

## catalan_open_dxc4: Open Catalan: early ...dxc4 (pawn grab)

- `ply`: 8
- `playedMove`: `d5c4`
- `analysisFen`: `rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 1 1`
- Metrics: 698 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.699, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=1, moveTokens=19, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
  - Advisory: lexical diversity soft-low: 0.70
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... dxc4: Both sides are still getting pieces out. The position is roughly equal. The plan is clear: **keeping the structure flexible**. There's a pin: the pawn on f2 cannot move without exposing the knight on g1.

A key question: Should Black release the central tension with dxc4?

a) dxc4 Nf3 Be7 O-O (+0.1)
b) Be7 Nf3 O-O O-O (+0.2)
c) c5 dxc5 Bxc5 Nf3 (+0.2)

**dxc4** is an excellent choice, following the main plan. It breaks through the center. It often simplifies into a manageable structure.

**Be7** is the cleaner technical route, aiming for a simpler structure.
**c5** takes the game into another strategic channel around positional maneuvering.

The position is dynamically balanced.
```

## reversed_sicilian_cxd5: Reversed Sicilian (English): early tension release (cxd5)

- `ply`: 7
- `playedMove`: `c4d5`
- `analysisFen`: `rnbqkb1r/ppp2ppp/5n2/3pp3/2P5/2N3P1/PP1PPP1P/R1BQKBNR w KQkq - 0 1`
- Metrics: 641 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.704, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=1, moveTokens=21, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4. cxd5: The opening battle continues. It's close to equal, with play for both sides. Key theme: **preserving the central pawn structure**. Keep an eye on the pawn on c4 — 1 attacker, no defenders. Clarify the pawn tension first; then the c-pawn break can be played with more effect.

A key question: After cxd5, how should Black recapture —...Nxd5 or...Qxd5?

a) Nf3 Nc6 cxd5 Nxd5 (+0.2)
b) cxd5 Nxd5 Nf3 Nc6 (+0.2)
c) d4 exd4 Qxd4 Nc6 (+0.1)

**cxd5** is slightly imprecise; stronger is **Nf3**. Better is **Nf3**; it solidifies the position.

**d4** is the cleaner technical route, aiming for a simpler structure.

The game remains tense.
```

## greek_gift_sac: Greek Gift Sacrifice: 1.Bxh7+ (Tactical explosion)

- `ply`: 15
- `playedMove`: `d3h7`
- `analysisFen`: `r1bq1rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 494 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.729, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=10, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Bxh7+: A classic Greek Gift sacrifice! The opening battle continues. White has a slight advantage (≈+1.5). Play revolves around **keeping the pawn chain intact**.

A key question: Does this fall into the Greek Gift Sacrifice?

a) Bxh7+ Kxh7 Qh5+ (+1.5)
b) O-O b6 Re1 (+0.3)

**Bxh7+** is an excellent choice, following the main plan. It continues the attack. The line is relatively clean.

**O-O** is playable, but significant disadvantage (1.2) after b6.

White has the easier game to play.
```

## exchange_sac_c3: Sicilian Defense: ...Rxc3 (Strategic Exchange Sacrifice)

- `ply`: 20
- `playedMove`: `c8c3`
- `analysisFen`: `2r1k2r/pp2ppbp/3p1np1/q7/3NP3/2N1BP2/PPPQ2PP/R3KB1R b KQk - 0 1`
- Metrics: 653 chars, 6 paragraphs
- Quality: score=92/100, lexical=0.747, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=1, moveTokens=12, scoreTokens=2
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
10... Rxc3: The hanging pawns in the center create significant tension. The opening battle continues. Neither side has a clear advantage. Play revolves around **maintaining pawn tension**.

A key question: After Rxc3, how should White recapture — bxc3 or Qxc3?

a) Rxc3 bxc3 O-O Bd3 (-0.2)
b) O-O Be2 (+0.3)

**Rxc3** is an excellent choice, following the main plan. It prepares for the next phase. The resulting position is fairly technical and calm.

**O-O** is the cleaner technical route, aiming for a simpler structure.

Accuracy is required to hold the balance. Sufficient Positional Compensation provides sufficient compensation for the material.
```

## iqp_blockade: Isolated Queen Pawn: Blockading d4/d5

- `ply`: 15
- `playedMove`: `d5c4`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 b - - 0 1`
- Metrics: 628 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.720, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=9, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. dxc4: The battle revolves around the isolated queen's pawn! The opening phase continues; piece placement matters. The position is about level. Strategic priority: **bishop Sacrifice**. Don't rush to clarify the pawn structure; the d-pawn break is a lever to keep in mind.

One practical question: Should Black release the central tension with dxc4?

a) dxc4 Bxc4 b6 a3 (+0.2)
b) b8a6 (+0.3)

**dxc4** is thematic and accurate. It challenges the pawn structure. The resulting position is fairly technical and calm.

**b8a6** heads for a more controlled position with fewer tactical swings.

It is easy to misstep if you relax.
```

## minority_attack_carlsbad: Minority Attack in Carlsbad structure (b4-b5)

- `ply`: 18
- `playedMove`: `a2a3`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 0 1`
- Metrics: 426 chars, 5 paragraphs
- Quality: score=92/100, lexical=0.803, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=1, moveTokens=8, scoreTokens=2
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... a3: White launches a minority attack on the queenside. The opening battle continues. White is just a touch better. Key theme: **keeping the pawn chain intact**.

a) a3 b6 b4 (+0.3)
b) b4 cxb4 (+0.3)

**a3** is an excellent choice, following the main plan. It repositions the piece. The line is relatively clean.

**b4** favors structure and technical handling over immediate complications.

Both sides have their chances.
```

## bad_bishop_endgame: Bad Bishop vs Good Knight: 1.Nd4 (Conversion)

- `ply`: 40
- `playedMove`: `c4d6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 638 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.706, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=14, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
20... Nd6+: Black is burdened by a bad bishop, restricted by its own pawn structure! We are firmly in endgame territory. White is pressing with a stable edge. The plan is clear: **avoiding premature pawn breaks**. Watch for a fork by the knight on d6 hitting pawn on b7 and king on e8.

A key question: How should White convert the advantage after Nd6+?

a) Nd6+ Kd7 Nxb7 (+4.0)
b) f3 Kd7 Kf2 (+1.5)

**Nd6+** is an excellent choice, following the main plan. It keeps the pressure on. It often simplifies into a manageable structure.

**f3** can work; the tradeoff is that decisive loss (2.5) after Kd7.

White can play on intuition here.
```

## zugzwang_pawn_endgame: Pawn Endgame: Zugzwang (Precision)

- `ply`: 60
- `playedMove`: `e3d4`
- `analysisFen`: `8/8/6p1/5pP1/5P2/4K3/8/3k4 w - - 0 1`
- Metrics: 576 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.713, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=14, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
30... Kd4: The position is defined by zugzwang—every move weakens the defender. The position simplifies into an endgame. White is close to a winning position. Key theme: **King Activation**. The king on e3 is active (mobility: 7).

A key question: How should White convert the advantage after Kd4?

a) Kd4 Kd2 Ke5 Ke3 (+10.0)
b) Kf3 Ke1 Kg3 Kf1 (+0.0)

**Kd4** is an excellent choice, following the main plan. It repositions the piece. You can aim for a tidy, low-drama continuation.

**Kf3** is playable, but decisive loss (10.0) after Ke1.

White can play on intuition here.
```

## prophylaxis_kh1: Prophylaxis: 1.Kh1 (Deep defense)

- `ply`: 20
- `playedMove`: `h1g1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 526 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.792, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=10, scoreTokens=4
- Outline: 8 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
10... Rg1: A deep prophylactic move to shore up the defense. We are now in the middlegame phase. White is just a touch better. Play revolves around **dominating the centre**.

a) g1h1 (+0.3)
b) h4 f5 h5 (+0.3)
c) Rg1 (-0.3)

**Rg1** misses the better **g1h1** (55 cp loss). Issue: slight inaccuracy (0.6). Better is **g1h1**; it challenges the center. Defending against a phantom threat, wasting a valuable tempo.

**h4** heads for a more controlled position with fewer tactical swings.

A single tempo can swing the position.
```

## interference_tactic: Interference Tactic: Cutting off defense

- `ply`: 25
- `playedMove`: `d1d8`
- `analysisFen`: `3r2k1/pp3ppp/4p3/1b6/4P3/P4P2/BP4PP/3R2K1 w - - 0 1`
- Metrics: 593 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.765, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=12, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13. Rxd8+: A tactical interference cutting off the defensive line. Endgame technique will decide a lot here. White has a decisive advantage. Play revolves around **Mating Attack**. The rook on d1 is underdefended: 1 attacker, no defenders.

Worth asking: How should White convert the advantage after Rxd8+?

a) Rxd8+ Be8 Rxe8# (+20.0)
b) a4 Ba6 b4 (+0.2)

A strong move: **Rxd8+** aligns with the position's demands. It poses serious questions. This tends to lead to a simpler position.

**a4** can work; the tradeoff is that decisive loss (19.8) after Ba6.

White has the easier game to play.
```

## passed_pawn_push: Passed Pawn: 1.d6 (Structural push)

- `ply`: 30
- `playedMove`: `d5d6`
- `analysisFen`: `3r2k1/pp3ppp/8/3P4/8/8/PP3PPP/3R2K1 w - - 0 1`
- Metrics: 514 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.701, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... d6: The passed pawn is a constant threat that must be addressed! We are firmly in endgame territory. White has the more pleasant position (≈+1.2). The plan is clear: **Passed Pawn Advance**.

a) d6 Kf8 f4 Ke8 (+1.2)
b) Kf1 Kf8 Ke2 (+0.3)

**d6** is an excellent choice, following the main plan. It advances the trumping pawn. This tends to lead to a simpler position.

From a practical angle, **Kf1** is fine, yet significant disadvantage (0.9) after Kf8.

In practical terms, White is more comfortable here.
```

## deflection_sacrifice: Deflection: 1.Qd8+ (Forcing move)

- `ply`: 28
- `playedMove`: `d5d8`
- `analysisFen`: `6k1/pp3ppp/4r3/3Q4/8/5P2/PP4PP/6K1 w - - 0 1`
- Metrics: 601 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=12, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
14... Qd8+: A sharp deflection to lure the pieces away. The position simplifies into an endgame. White should convert with correct play (≈+20.0). Key theme: **Mating Attack**. King activity matters: g1 has 3 safe steps.

A key question: How should White convert the advantage after Qd8+?

a) Qd8+ Re8 Qxe8# (+20.0)
b) g3 h6 Kg2 (+0.2)

**Qd8+** is an excellent choice, following the main plan. It keeps the pressure on. The resulting position is fairly technical and calm.

From a practical angle, **g3** is fine, yet decisive loss (19.8) after h6.

In practical terms, White is more comfortable here.
```

## king_hunt_sac: King Hunt: 1.Bxf7+ (Luring the king)

- `ply`: 12
- `playedMove`: `c4f7`
- `analysisFen`: `r1bqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 587 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.753, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=14, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
6... Bxf7+: A relentless king hunt begins! The opening battle continues. White is a bit better (≈+2.5). Key theme: **bishop Sacrifice**. The pin on d7 makes that pawn awkward to handle.

A key question: What is the defensive task here — can White meet the threat with Bxf7+ (watch c6)?

a) Bxf7+ Kxf7 Ng5+ Kg8 (+2.5)
b) O-O d6 exd6 (+0.4)

**Bxf7+** is an excellent choice, following the main plan. It continues the attack. The resulting position is fairly technical and calm.

From a practical angle, **O-O** is fine, yet decisive loss (2.1) after d6.

White can play on intuition here.
```

## battery_open_file: Rook Battery: Doubling on the d-file

- `ply`: 22
- `playedMove`: `d1d2`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 460 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.844, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, moveTokens=8, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
11... R1d2: A powerful battery on the open file exerts immense pressure. This is a full middlegame now. White has a modest edge. Play revolves around **Rook Activation**.

a) R1d2 b6 (+0.4)
b) a3 b6 b4 (+0.3)

A strong move: **R1d2** aligns with the position's demands. It provides necessary defense. This tends to lead to a simpler position.

**a3** favors structure and technical handling over immediate complications.

The balance is delicate and can shift.
```

## bishop_pair_open: Bishop Pair: Dominance in open position

- `ply`: 35
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 596 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.782, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=8, scoreTokens=4
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
18. Rxe8#: The bishop pair exerts immense pressure on the open board. Endgame technique will decide a lot here. White has the easier side to press with. Play revolves around **Forced Mate**. The king on g1 is well-placed for the endgame.

Worth asking: Does this fall into the Back Rank Mate?

a) Rxe8# (+0.5)
b) Bd4 a6 Rd1 (+0.4)

A strong move: **Rxe8#** aligns with the position's demands. It continues the attack. It often simplifies into a manageable structure.

**Bd4** favors structure and technical handling over immediate complications.

The position offers counterplay for both players.
```

## hanging_pawns_center: Hanging Pawns: c4/d4 (Structural tension)

- `ply`: 15
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1p2pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 709 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.790, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. cxd5: The hanging pawns in the center create significant tension! Early skirmishes define the position. The evaluation is essentially balanced. Strategic priority: **Minority Attack**. Keep an eye on the pawn on c4 — 1 attacker, no defenders. With tension preserved, the c-pawn break becomes a thematic lever.

Worth asking: After cxd5, how should Black recapture —...exd5,...Nxd5, or...Bxd5?

a) cxd5 exd5 dxc5 bxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

A strong move: **cxd5** aligns with the position's demands. It challenges the pawn structure. This line reduces immediate tactical volatility.

**b3** is the cleaner technical route, aiming for a simpler structure.

It's roughly level, with play on both sides.
```

## opposite_bishops_attack: Opposite-Colored Bishops: Attacking the King

- `ply`: 30
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3p1p/2b3p1/8/8/P1B2B2/1P3PPP/4R1K1 w - - 0 1`
- Metrics: 546 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.822, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... Rxe8+: Opposite-colored bishops signal an attack on the king. Endgame technique will decide a lot here. White has a modest edge. Key theme: **avoiding premature pawn breaks**. King activity matters: g1 has 2 safe steps.

a) Rxe8+ Bxe8 Bxb7 (+0.6)
b) Bxc6 bxc6 Rxe8# (+0.6)

A strong move: **Rxe8+** aligns with the position's demands. It keeps the pressure on. It often simplifies into a manageable structure.

**Bxc6** favors structure and technical handling over immediate complications.

The position offers counterplay for both players.
```

## simplification_trade: Simplification: Trading into a won endgame

- `ply`: 45
- `playedMove`: `d5b5`
- `analysisFen`: `4r1k1/1p3ppp/8/3R4/8/P4P2/1P4PP/6K1 w - - 0 1`
- Metrics: 559 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.704, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
23. Rb5: Endgame technique and simplification will be the decisive factors! In this endgame, piece activity is paramount. White has the more pleasant position. The plan is clear: **maintaining pawn tension**. The king on g1 is active (mobility: 3).

a) Rb5 Re7 Kf2 f6 (+1.5)
b) Kf2 f6 Rb5 (+1.4)

A strong move: **Rb5** aligns with the position's demands. It prevents counterplay. The resulting position is fairly technical and calm.

**Kf2** is the cleaner technical route, aiming for a simpler structure.

In practical terms, White is more comfortable here.
```

## zugzwang_mutual: Mutual Zugzwang: Pawn Endgame (Precision)

- `ply`: 55
- `playedMove`: `d4c4`
- `analysisFen`: `8/8/p1p5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 557 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.805, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, moveTokens=7, scoreTokens=3
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
The position is defined by zugzwang—every move weakens the defender! Small details matter in this endgame. Neither side has a clear advantage. Strategic priority: **King Activation**. The king on d4 is active (mobility: 7).

a) Kc3 (+0.0)
b) Ke4 (+0.0)
c) d4c4 (-0.5)

**Kc3**! prepares for the next phase. It remains a precise choice. The position is about level.

**Ke4** favors structure and technical handling over immediate complications.
**d4c4** points to a different strategic plan around positional maneuvering.

It is easy to misstep if you relax.
```

## smothered_mate_prep: Smothered Mate Prep: 1.Nb5 (Tactical motif)

- `ply`: 15
- `playedMove`: `c3b5`
- `analysisFen`: `r1bq1rk1/pp1n1ppp/2n1p3/2ppP3/3P1B2/2N5/PPP1QPPP/2KR1BNR w - - 0 1`
- Metrics: 503 chars, 5 paragraphs
- Quality: score=92/100, lexical=0.847, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=1, moveTokens=8, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Nb5: Setting up a potential smothered mate. The opening battle continues. White has a small pull. Play revolves around **Piece Activation**. b5 can serve as an outpost for a knight.

a) Nb5 a6 Nd6 (+0.5)
b) Nf3 (+0.3)

**Nb5** is an excellent choice, following the main plan. It places the piece on an unassailable square. You can aim for a tidy, low-drama continuation.

**e1c1** favors structure and technical handling over immediate complications.

In practical terms, this is comfortable to play.
```

## knight_domination_endgame: Knight Domination: 1.Nb6 (Outplaying a bishop)

- `ply`: 42
- `playedMove`: `c4b6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 522 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.747, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=8, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
21... Nb6: The knight dominates its counterpart on this outpost. The position simplifies into an endgame. White is slightly ahead in the evaluation. Play revolves around **Minority Attack**. Keep b6 in mind as an outpost square.

a) Nb6 (+1.8)
b) f3 Kd7 Kf2 (+1.5)

**Nb6** is an excellent choice, following the main plan. It establishes a strong outpost. The resulting position is fairly technical and calm.

**f3** favors structure and technical handling over immediate complications.

White's plan is easier to execute.
```

## opening_novelty_e5: Opening Novelty: Handling an early ...e5 break

- `ply`: 10
- `playedMove`: `e1g1`
- `analysisFen`: `rnbqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 586 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.800, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
5... O-O: An interesting opening novelty that changes the character of the game. It's still early, but plans are taking shape. White is just a touch better. Play revolves around **keeping the pawn chain intact**. The e-pawn break is a thematic lever in this structure.

a) O-O d6 exd6 (+0.4)
b) Qd5 O-O O-O (+0.4)

A strong move: **O-O** aligns with the position's demands. It completes the king's evacuation. This line reduces immediate tactical volatility.

**Qd5** favors structure and technical handling over immediate complications.

The position is easier to handle than it looks.
```

## rook_lift_attack: Rook Lift: 1.Rd3 (Attacking the king)

- `ply`: 24
- `playedMove`: `d3d4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 513 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.788, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12... Rd4: A powerful rook lift to join the assault. We are now in the middlegame phase. White is just a touch better. Play revolves around **avoiding premature pawn breaks**. The pin on d5 makes that pawn awkward to handle.

a) Rd4 f5 Rh4 (+0.4)
b) R1d2 b6 a3 (+0.3)

**Rd4** is thematic and accurate. It keeps the opponent under pressure. The resulting position is fairly technical and calm.

**R1d2** is the cleaner technical route, aiming for a simpler structure.

It's roughly level, with play on both sides.
```

## outside_passed_pawn: Outside Passed Pawn: a-pawn advantage

- `ply`: 58
- `playedMove`: `d4e5`
- `analysisFen`: `8/8/p1k5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 521 chars, 5 paragraphs
- Quality: score=92/100, lexical=0.766, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=1, moveTokens=11, scoreTokens=2
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
29... Ke5: The passed pawn is a constant threat that must be addressed. The position simplifies into an endgame. White is a bit better. Play revolves around **King Activation**. The king on d4 is well-placed for the endgame.

a) Ke5 Kc7 Kd5 (+1.0)
b) Kc3 Kd5 Kd3 (+0.0)

**Ke5** is an excellent choice, following the main plan. It repositions the piece. You can aim for a tidy, low-drama continuation.

From a practical angle, **Kc3** is fine, yet significant disadvantage (1.0) after Kd5.

Both sides have their chances.
```

## zwischenzug_tactics: Zwischenzug: 1.h4 (Tactical intermediate)

- `ply`: 26
- `playedMove`: `h4h5`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/7P/2NRBN2/PPP2PP1/3R2K1 w - - 0 1`
- Metrics: 503 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.821, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13... h5: A clever zwischenzug or intermediate move to seize the initiative. The game transitions into a complex middlegame. White has a small pull (≈+0.4). Play revolves around **keeping the structure flexible**.

a) h5 h6 g4 (+0.4)
b) R1d2 b6 a3 (+0.3)

**h5** is an excellent choice, following the main plan. It provides necessary defense. This tends to lead to a simpler position.

**R1d2** favors structure and technical handling over immediate complications.

The position is dynamically balanced.
```

## pawn_break_f4: Pawn Break: 1.f4 (Kingside storm prep)

- `ply`: 21
- `playedMove`: `f2f4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 436 chars, 5 paragraphs
- Quality: score=92/100, lexical=0.850, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=1, moveTokens=6, scoreTokens=2
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A massive pawn storm is brewing on the kingside. The game transitions into a complex middlegame. White has a modest edge (≈+0.4). Key theme: **dominating the centre**.

a) f2f4 (+0.4)
b) a3 b6 b4 (+0.3)

**f2f4**! maintains central tension. White is just a touch better. Crucially, this stops the opponent's idea of Tactical Threat.

**a3** is the cleaner technical route, aiming for a simpler structure.

Both sides have their chances.
```

## trapped_piece_rim: Trapped Piece: 1.g4 (Minor piece on the rim)

- `ply`: 18
- `playedMove`: `g4h5`
- `analysisFen`: `r1bqk2r/ppppbppp/2n1p3/4P2n/2B3P1/2P2N2/PP1P1P1P/RNBQK2R w KQkq - 0 1`
- Metrics: 532 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.769, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... gxh5: A critical piece is trapped on the rim with no escape. It's still early, but plans are taking shape. White holds a clear advantage. Key theme: **preserving the central pawn structure**. The g-pawn break is a thematic lever in this structure.

a) gxh5 g6 hxg6 (+3.5)
b) h3 d6 exd6 (+0.3)

A strong move: **gxh5** aligns with the position's demands. It breaks through the center. It often simplifies into a manageable structure.

**h3** is playable, but decisive loss (3.2) after d6.

The position is comfortable for White.
```

## stalemate_resource: Stalemate Trick: 1.Rh1+ (Defensive resource)

- `ply`: 65
- `playedMove`: `h2h1`
- `analysisFen`: `8/8/8/8/8/5k2/7r/5K2 b - - 0 1`
- Metrics: 575 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.780, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, moveTokens=10, scoreTokens=4
- Outline: 8 beats (MoveHeader, Context, DecisionPoint, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
33. Rh1#: A desperate but clever stalemate resource! We are firmly in endgame territory. Black is winning. Strategic priority: **Forced Mate**. The kings are in direct opposition.

A key question: What is the defensive task here — can Black meet the threat with Rh1# (watch d2)?

a) Rd2 Ke1 Rd1+ (-10.0)
b) Rh1# (+0.0)

Missing king cut off was a significant oversight (1000 cp).

**Rh1#** is a serious error; stronger is **Rd2**. Better is **Rd2**; it poses serious questions. Defending against a phantom threat, wasting a valuable tempo.

Black has the easier game to play.
```

## underpromotion_knight: Underpromotion: 1.e8=N+ (Tactical necessity)

- `ply`: 55
- `playedMove`: `e7e8n`
- `analysisFen`: `8/4P1k1/8/8/8/8/8/1K4n1 w - - 0 1`
- Metrics: 537 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.771, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, moveTokens=10, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
28. e8=N+: A rare and necessary underpromotion! In this endgame, piece activity is paramount. White has a slight advantage. Strategic priority: **Passed Pawn Advance**.

Worth asking: How should White convert the advantage after e8=N+?

a) e8=N+ Kf8 Nf6 (+2.0)
b) e8=Q Nf3 (+0.0)

A strong move: **e8=N+** aligns with the position's demands. It continues the attack. It often simplifies into a manageable structure.

**e8=Q** is playable, but significant disadvantage (2.0) after Nf3.

In practical terms, White is more comfortable here.
```

## kingside_pawn_storm: Pawn Storm: 1.g4 (Attacking the king)

- `ply`: 23
- `playedMove`: `g2g4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 483 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.794, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12. g4: A massive pawn storm is brewing on the kingside. This is a full middlegame now. White is just a touch better. Play revolves around **preserving the central pawn structure**.

a) g4 h6 g5 (+0.5)
b) a3 b6 b4 (+0.3)

A strong move: **g4** aligns with the position's demands. It provides necessary defense. This line reduces immediate tactical volatility.

**a3** heads for a more controlled position with fewer tactical swings.

The position offers counterplay for both players.
```

## quiet_improvement_move: Quiet Improvement: 1.Re1 (Improving the rook)

- `ply`: 25
- `playedMove`: `d3e1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 456 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.871, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, moveTokens=6, scoreTokens=2
- Outline: 5 beats (Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
A deep prophylactic move to shore up the defense. The game transitions into a complex middlegame. White can keep up mild pressure. Key theme: **dominating the centre**.

a) d3e1 (+0.4)
b) a3 b6 b4 (+0.3)

**d3e1**! maintains central tension. White has a small pull. Crucially, this stops the opponent's idea of Tactical Threat.

**a3** favors structure and technical handling over immediate complications.

The position offers counterplay for both players.
```

## repetition_decision_move: Repetition: Deciding for/against a draw

- `ply`: 38
- `playedMove`: `e1d1`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 457 chars, 4 paragraphs
- Quality: score=92/100, lexical=0.773, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=1, moveTokens=11, scoreTokens=3
- Advisory findings:
  - Advisory: boilerplate phrase present (1)
- Outline: 7 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
19... Rd1: The game heads toward a repeat of the position. Precision is required in this endgame. The position is roughly equal. Key theme: **keeping the pawn chain intact**. King activity matters: g1 has 3 safe steps.

a) Bd4 a6 Rd1 (+0.2)
b) Rd1 Bc6 Re1 (+0.1)

**Rd1** misses the better **Bd4** (5 cp loss). Better is **Bd4**; it improves piece placement. Defending against a phantom threat, wasting a valuable tempo.

The defensive burden is noticeable.
```

## central_liquidation_move: Central Liquidation: 1.cxd5 (Clearing the center)

- `ply`: 18
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 778 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.714, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, moveTokens=14, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... cxd5: The central liquidation changes the character of the struggle! Both sides are still getting pieces out. White has a modest edge. Strategic priority: **maintaining pawn tension**. Keep an eye on the pawn on c4 — 1 attacker, no defenders. Keep the pawn tension; the c-pawn break is a useful lever.

A key question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) cxd5 exd5 dxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

**cxd5** is an excellent choice, following the main plan. It breaks through the center. This tends to lead to a simpler position.

**b3** is the cleaner technical route, aiming for a simpler structure.

The position offers counterplay for both players. Sufficient Positional Compensation provides sufficient compensation for the material.
```

## final_sanity_check: Final Sanity Check: Complex position

- `ply`: 30
- `playedMove`: `a2a3`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 300 chars, 4 paragraphs
- Quality: score=100/100, lexical=0.841, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=8, dup=0, boilerplate=0, moveTokens=8, scoreTokens=3
- Outline: 5 beats (MoveHeader, Context, Evidence, MainMove, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... a3: Middlegame complications begin to arise. White can keep up mild pressure. The plan is clear: **maintaining pawn tension**.

a) R1d2 (+0.4)
b) a3 b6 b4 (+0.3)

Instead of **a3**, stronger is **R1d2** (10 cp). Better is **R1d2**; it stops the enemy ideas.

Neither side has a stable edge yet.
```

