# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/BookCommentaryCorpus.json`
- Results: 40/40 passed

## Quality Summary

- Avg quality score: 100.0 / 100
- Avg lexical diversity: 0.750
- Avg variation-anchor coverage: 1.000
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 3 across 3 cases

## Cross-Case Repetition

- [4 cases] "neither side has established a clear advantage"
- [3 cases] "a minority attack is becoming a practical queenside plan"
- [3 cases] "the practical chances are still shared between both players"
- [3 cases] "defending against a phantom threat wasting a valuable tempo"
- [3 cases] "white can improve with lower practical risk move by move"
- [3 cases] "defensive precision is more important than active looking moves"
- [3 cases] "the position is finely balanced"
- [3 cases] "white is just a touch better"
- [3 cases] "the struggle has shifted into technical endgame play"
- [3 cases] "it challenges the center"
- [3 cases] "file control along the open line can dictate the middlegame"
- [3 cases] "the queenside minority attack is turning into the key practical route"

## philidor_h6: Philidor / Legal's-mate motif: 4...h6?

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 3 1`
- Metrics: 950 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.701, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=22, scoreTokens=4
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
4... h6: The pawn on f7 is pinned (absolute) to the knight on g8. The bad bishop is restricted by its own pawn chain. Opening development is still in progress. Small developmental choices can still reshape the structure. The evaluation is essentially balanced. Key theme: **holding the center without clarifying too early**.

A key question: Does this fall into the Legal’s Mate?

a) Nf6 d4 Be7 O-O (+0.3)
b) Nc6 h3 Bh5 d4 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+1.0)

**h6** ?! is an inaccuracy that concedes practical ease; **Nf6** is tighter. Engine ranking puts this in a lower tier, while **Nf6** keeps tighter control. Issue: it allows a pin on g7, tying the pawn to the knight on g8. Consequence: the opponent gets the easier plan and more comfortable piece play. Better is **Nf6**; it repositions the piece.

**Nc6** favors structural clarity and methodical handling over complications.

The game remains balanced, and precision will decide the result.
```

## philidor_h6_no_legals_mate: Philidor-ish: 4.d3 ...h6?! (no Legal's mate)

- `ply`: 8
- `playedMove`: `h7h6`
- `analysisFen`: `rn1qkbnr/ppp2ppp/3p4/4p3/2B1P1b1/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 1`
- Metrics: 754 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.778, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=20, scoreTokens=3
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... h6: A bad bishop problem is limiting piece quality. Early plans are still being defined. Piece coordination remains central. Neither side has established a clear advantage. Strategic focus: **coordinating piece play before structural commitments**.

a) Nf6 O-O Be7 c3 (+0.2)
b) Nc6 c3 Nf6 h3 (+0.3)
c) h6 Nxe5 dxe5 Qxg4 (+0.6)

**h6** is not the top choice here; **Nf6** remains the reference move. This sits below the principal engine candidates; **Nf6** gives a more reliable setup. Issue: it allows a pin on g7, tying the pawn to the knight on g8. Better is **Nf6**; it improves the piece's scope.

**Nc6** is a practical technical route that simplifies defensive duties.

Fine margins mean technical accuracy still determines practical outcomes.
```

## qid_tension_release: Queen's Indian: early tension release (9.cxd5)

- `ply`: 17
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/3p4/2PP1B2/2N2NP1/PP2PPBP/R2QK2R w KQkq - 0 1`
- Metrics: 874 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.760, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=keep_tension_branches, branches=2

**Reference excerpt (for human comparison)**

```text
Best practice is often to maintain the uncertainty (tension) in the centre as long as possible, developing for both scenarios. After an early release of tension, the cramped side can often place pieces on their most natural squares.
```

**Generated commentary**

```text
9. cxd5: The pawn on c4 is underdefended: 1 attacker, no defenders. A switch in piece placement can improve control. The opening phase is still fluid. Neither side wants to clarify the center too early. White is just a touch better (≈+0.3). The most reliable roadmap here is built around **preserving the central pawn structure**.

A key question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) 9... dxc4 10. Qc2 Nbd7 (+0.2)
b) 9... c5 (+0.3)

**cxd5** is playable but second-best; **O-O** keeps the position simpler. The engine still prefers **O-O**, though the practical gap stays small. Issue: this is only the 3rd engine option and gives the opponent easier play. Better is **O-O**; it castles to safety.

**Ne5** favors structural clarity and methodical handling over complications.

The position allows methodical play without forcing tactics.
```

## startpos_e4_best: Sanity check: best move still shows alternatives (1.e4)

- `ply`: 1
- `playedMove`: `e2e4`
- `analysisFen`: `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`
- Metrics: 658 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.787, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=17, scoreTokens=3
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
1. e4: Early plans are still being defined. Both sides are balancing development and structure. Neither side has established a clear advantage. The most reliable roadmap here is built around **space advantage in the center**.

a) e4 c5 Nf3 d6 (+0.2)
b) d4 d5 c4 e6 (+0.2)
c) c4 e5 Nc3 Nf6 (+0.1)

**e4** keeps to the strongest continuation. It challenges the center. After **e4**, the game trends toward a controlled strategic struggle.

**d4** favors structural clarity and methodical handling over complications.
**c4** points to a different strategic plan around central control.

The position stays tense, and one careless tempo can swing the initiative.
```

## ruy_c3_prepare_d4: Ruy Lopez Exchange: 7.c3! (preparing d4)

- `ply`: 13
- `playedMove`: `c2c3`
- `analysisFen`: `r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1`
- Metrics: 883 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.706, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=17, scoreTokens=3
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
7. c3: d-file pressure is the concrete lever in the current position. The queenside minority attack is becoming a concrete lever. Opening development is still in progress. Move-order precision matters more than forcing lines. White can keep up mild pressure. Current play is organized around **control of key central squares**. Watch for the d-pawn break — it is a useful lever.

a) c3 O-O d4 Bd6 (+0.4)
b) Nbd2 O-O d4 exd4 (+0.3)
c) h3 h6 Nbd2 O-O (+0.2)

**c3** keeps to the strongest continuation. It challenges the center. **c3** aims for a tidy continuation with fewer forcing turns.

This is a well-known position from the Ruy Lopez: Exchange Variation (412 games, White scores 50%).

**Nbd2** favors structural clarity and methodical handling over complications.
**h3** points to a different strategic plan around central control.

Plans are relatively clear for both players.
```

## london_cxd4_recapture: London System: early ...cxd4 (recapture choice)

- `ply`: 10
- `playedMove`: `c5d4`
- `analysisFen`: `rnbqkb1r/pp3ppp/4pn2/2pp4/3P1B2/2P1PN2/PP3PPP/RN1QKB1R b KQkq - 0 1`
- Metrics: 928 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.702, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=16, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=recapture_branches, branches=2

**Generated commentary**

```text
5... cxd4: The structure around the c-file break is now the practical focal point. A minority attack structure is emerging on the queenside. The opening is already testing tactical accuracy. One inaccurate developing move can concede practical control. The position is finely balanced. The bad bishop remains a lasting positional burden. Strategic priority: **keeping central levers available for later timing**. With tension preserved, the c-pawn break becomes a thematic lever.

Worth asking: After cxd4, how should White recapture —exd4 or cxd4?

a) 6. exd4 Nc6 (+0.2)
b) 6. cxd4 Nc6 (+0.3)

**cxd4** is playable but second-best; **Qb6** keeps the position simpler. The engine line order is close here; **Qb6** is still the cleaner reference. Better is **Qb6**; it maintains the tension.

**Nc6** keeps the game in a technical channel where precise handling is rewarded.

A small timing error can hand over practical control.
```

## catalan_open_dxc4: Open Catalan: early ...dxc4 (pawn grab)

- `ply`: 8
- `playedMove`: `d5c4`
- `analysisFen`: `rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 1 1`
- Metrics: 893 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.687, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=19, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.69
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4... dxc4: The pin on f2 makes that pawn awkward to handle. The bad bishop is restricted by its own pawn chain. Early plans are still being defined. Central tension is still unresolved. The position is finely balanced. The queenside minority attack is turning into the key practical route. Key theme: **keeping the pawn chain intact**.

Worth asking: Should Black release the central tension with dxc4?

a) dxc4 Nf3 Be7 O-O (+0.1)
b) Be7 Nf3 O-O O-O (+0.2)
c) c5 dxc5 Bxc5 Nf3 (+0.2)

**dxc4** is fully sound and matches the position's demands. It tests the pawn structure directly. The line after **dxc4** is relatively clean and technical, with less tactical turbulence.

**Be7** aims for a technically manageable position with clear conversion paths.
**c5** is a strategic alternative around positional maneuvering.

Fine margins mean technical accuracy still determines practical outcomes.
```

## reversed_sicilian_cxd5: Reversed Sicilian (English): early tension release (cxd5)

- `ply`: 7
- `playedMove`: `c4d5`
- `analysisFen`: `rnbqkb1r/ppp2ppp/5n2/3pp3/2P5/2N3P1/PP1PPP1P/R1BQKBNR w KQkq - 0 1`
- Metrics: 907 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.715, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=16, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=22, scoreTokens=3
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
4. cxd5: Keep an eye on the pawn on c4 — 1 attacker, no defenders. A rerouting maneuver is now the practical plan. Development goals are clear, but the structure is still flexible. Piece coordination remains central. Neither side has established a clear advantage. Move-order choices are justified by **coordinating piece play before structural commitments**. Releasing the tension can make the c-pawn break more potent.

Worth asking: After cxd5, how should Black recapture —...Nxd5 or...Qxd5?

a) Nf3 Nc6 cxd5 Nxd5 (+0.2)
b) cxd5 Nxd5 Nf3 Nc6 (+0.2)
c) d4 exd4 Qxd4 Nc6 (+0.1)

**cxd5** is playable but second-best; **Nf3** keeps the position simpler. The engine line order is close here; **Nf3** is still the cleaner reference. Better is **Nf3**; it prevents counterplay.

**d4** is the cleaner technical route, aiming for a stable structure.

Both players still need concrete accuracy before committing.
```

## greek_gift_sac: Greek Gift Sacrifice: 1.Bxh7+ (Tactical explosion)

- `ply`: 15
- `playedMove`: `d3h7`
- `analysisFen`: `r1bq1rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 733 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.743, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Bxh7+: The struggle revolves around weak color complex squares. Early tactical motifs are now dictating move-order. Every developing move now has tactical consequences. White is a bit better (≈+1.5). A minority attack is becoming a practical queenside plan. Move-order choices are justified by **avoiding premature pawn breaks**.

A key question: Does this fall into the Greek Gift Sacrifice?

a) Bxh7+ Kxh7 Qh5+ (+1.5)
b) O-O b6 Re1 (+0.3)

**Bxh7+** is accurate and consistent with the main plan. It seizes the initiative. The line after **Bxh7+** is relatively clean and technical, with less tactical turbulence.

**O-O** is playable, but significant disadvantage after b6.

White has the more comfortable practical route here.
```

## exchange_sac_c3: Sicilian Defense: ...Rxc3 (Strategic Exchange Sacrifice)

- `ply`: 20
- `playedMove`: `c8c3`
- `analysisFen`: `2r1k2r/pp2ppbp/3p1np1/q7/3NP3/2N1BP2/PPPQ2PP/R3KB1R b KQk - 0 1`
- Metrics: 805 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.746, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=14, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
10... Rxc3: Central hanging pawns keep the position tense. Development now intersects with concrete calculation. There is little room for slow setup moves. The evaluation is essentially balanced. Minority-attack play on the queenside is becoming the most practical plan. Key theme: **keeping central levers available for later timing**.

Worth asking: After Rxc3, how should White recapture — bxc3 or Qxc3?

a) Rxc3 bxc3 O-O Bd3 (-0.2)
b) O-O Be2 (+0.3)

**Rxc3** is fully sound and matches the position's demands. It improves piece routes for later plans. **Rxc3** often leads to a stable structure with clear plans.

**O-O** is playable, but slight practical concession after Be2.

Defensive precision is more important than active-looking moves. White gets a Positional return for the material deficit.
```

## iqp_blockade: Isolated Queen Pawn: Blockading d4/d5

- `ply`: 15
- `playedMove`: `d5c4`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N2N2/PP2BPPP/R1BQ1RK1 b - - 0 1`
- Metrics: 830 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.695, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=16, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.69
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. dxc4: Plans on both sides revolve around the d-file pawn break. The queenside minority attack is becoming a concrete lever. Opening choices are already forcing concrete decisions. One tempo can trigger forcing play. The position is finely balanced. The isolated queen pawn structure is a key strategic reference point. Most sensible plans here converge on **bishop Sacrifice**. Keep the pawn tension; the d-pawn break is a useful lever.

One practical question: Should Black release the central tension with dxc4?

a) dxc4 Bxc4 b6 a3 (+0.2)
b) b8a6 (+0.3)

**dxc4** keeps to the strongest continuation. It opens the position. The line after **dxc4** is relatively clean and technical, with less tactical turbulence.

**b8a6** is a practical technical route that simplifies defensive duties.

The defensive burden is noticeable.
```

## minority_attack_carlsbad: Minority Attack in Carlsbad structure (b4-b5)

- `ply`: 18
- `playedMove`: `a2a3`
- `analysisFen`: `r1bq1rk1/pp2bppp/2n1pn2/2pp4/2PP4/2N1PN2/PP2BPPP/R1BQ1RK1 w - - 0 1`
- Metrics: 625 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.775, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... a3: The bad bishop's scope is a strategic weakness here. Opening priorities are still being negotiated move by move. Piece coordination remains central. White has the easier side to press with. The queenside minority attack is turning into the key practical route. Strategic priority: **maintaining pawn tension**.

a) a3 b6 b4 (+0.3)
b) b4 cxb4 (+0.3)

**a3** is accurate and consistent with the main plan. It prepares for the next phase. **a3** often leads to a stable structure with clear plans.

**b4** favors structural clarity and methodical handling over complications.

The position remains dynamically balanced.
```

## bad_bishop_endgame: Bad Bishop vs Good Knight: 1.Nd4 (Conversion)

- `ply`: 40
- `playedMove`: `c4d6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 666 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
20... Nd6+: A fork motif is in the air: d6 can attack pawn on b7 and king on e8. The struggle has shifted into technical endgame play. Piece activity outweighs broad strategic plans. White holds a clear advantage. A bad bishop handicap is still shaping the strategic plans. Key theme: **maintaining pawn tension**.

Worth asking: How should White convert the advantage after Nd6+?

a) Nd6+ Kd7 Nxb7 (+4.0)
b) f3 Kd7 Kf2 (+1.5)

**Nd6+** keeps to the strongest continuation. It maintains active momentum. **Nd6+** guides play into a precise conversion phase.

**f3** is playable, but decisive loss after Kd7.

White can improve with lower practical risk move by move.
```

## zugzwang_pawn_endgame: Pawn Endgame: Zugzwang (Precision)

- `ply`: 60
- `playedMove`: `e3d4`
- `analysisFen`: `8/8/6p1/5pP1/5P2/4K3/8/3k4 w - - 0 1`
- Metrics: 672 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.713, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
30... Kd4: The king on e3 is active (mobility: 7). Zugzwang ideas are central: useful moves are running out. The position has simplified into an endgame. One tempo can decide the technical outcome. White has a decisive advantage. Key theme: **King Activation**.

A key question: How should White convert the advantage after Kd4?

a) Kd4 Kd2 Ke5 Ke3 (+10.0)
b) Kf3 Ke1 Kg3 Kf1 (+0.0)

**Kd4** keeps to the strongest continuation. It improves piece routes for later plans. The line after **Kd4** is clean and technical, where subtle king routes and tempi matter.

**Kf3** is playable, but decisive loss after Ke1.

From a practical standpoint, White has the cleaner roadmap.
```

## prophylaxis_kh1: Prophylaxis: 1.Kh1 (Deep defense)

- `ply`: 20
- `playedMove`: `h1g1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3pP3/5B2/2PBP3/PP1N1PPP/R2QK2R w KQ - 0 1`
- Metrics: 867 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.793, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=3
- Outline: 8 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
10... Rg1: The bishop pair increases pressure across both wings. This middlegame phase rewards concrete, well-timed choices. Move-order nuance can shift practical control. White is just a touch better. A prophylactic idea is central to limiting counterplay. Strategic focus: **a grip on the centre**.

a) g1h1 (+0.3)
b) h4 f5 h5 (+0.3)
c) Rg1 (-0.3)

**Rg1** ?! is an inaccuracy that drifts from the best plan; the cleaner move is **h1**. This is outside the top engine choices, and **h1** is the stable reference. Issue: slight practical concession. Consequence: piece coordination loosens and counterplay appears. Better is **h1**; it challenges the center. Defending against a phantom threat, wasting a valuable tempo.

**h4** aims for a technically manageable position with clear conversion paths.

Defensive precision is more important than active-looking moves.
```

## interference_tactic: Interference Tactic: Cutting off defense

- `ply`: 25
- `playedMove`: `d1d8`
- `analysisFen`: `3r2k1/pp3ppp/4p3/1b6/4P3/P4P2/BP4PP/3R2K1 w - - 0 1`
- Metrics: 788 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13. Rxd8+: Keep an eye on the rook on d1 — 1 attacker, no defenders. File control along the open line can dictate the middlegame. Endgame precision now outweighs broad strategic plans. Pawn-structure details carry extra weight. White is winning. Interference is a live tactical resource in this position. The most reliable roadmap here is built around **Mating Attack**.

Worth asking: How should White convert the advantage after Rxd8+?

a) Rxd8+ Be8 Rxe8# (+20.0)
b) a4 Ba6 b4 (+0.2)

**Rxd8+** is accurate and consistent with the main plan. It maintains active momentum. The line after **Rxd8+** is clean and technical, where subtle king routes and tempi matter.

**a4** can work; the tradeoff is that decisive loss after Ba6.

White can improve with lower practical risk move by move.
```

## passed_pawn_push: Passed Pawn: 1.d6 (Structural push)

- `ply`: 30
- `playedMove`: `d5d6`
- `analysisFen`: `3r2k1/pp3ppp/8/3P4/8/8/PP3PPP/3R2K1 w - - 0 1`
- Metrics: 539 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=11, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... d6: This is a technical endgame phase. Practical endgame technique matters more than broad plans. White is slightly ahead in the evaluation. Strategic priority: **Passed Pawn Advance**.

a) d6 Kf8 f4 Ke8 (+1.2)
b) Kf1 Kf8 Ke2 (+0.3)

**d6** is fully sound and matches the position's demands. It pushes the passed pawn. After **d6**, technical details matter more than tactical tricks.

Practically speaking, **Kf1** is viable, but significant disadvantage after Kf8.

White's practical choices are easier to execute with fewer risks.
```

## deflection_sacrifice: Deflection: 1.Qd8+ (Forcing move)

- `ply`: 28
- `playedMove`: `d5d8`
- `analysisFen`: `6k1/pp3ppp/4r3/3Q4/8/5P2/PP4PP/6K1 w - - 0 1`
- Metrics: 767 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.721, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=13, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
14... Qd8+: The king on g1 is active (mobility: 3). File control along the open line can dictate the middlegame. Endgame precision now outweighs broad strategic plans. One tempo can decide the technical outcome. White is completely on top (≈+20.0). Deflection is a concrete tactical theme to calculate carefully. Strategic focus: **Mating Attack**.

A key question: How should White convert the advantage after Qd8+?

a) Qd8+ Re8 Qxe8# (+20.0)
b) g3 h6 Kg2 (+0.2)

**Qd8+** keeps to the strongest continuation. It poses serious practical questions. The line after **Qd8+** is clean and technical, where subtle king routes and tempi matter.

Practically speaking, **g3** is viable, but decisive loss after h6.

White can improve with lower practical risk move by move.
```

## king_hunt_sac: King Hunt: 1.Bxf7+ (Luring the king)

- `ply`: 12
- `playedMove`: `c4f7`
- `analysisFen`: `r1bqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 813 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.763, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=16, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
6... Bxf7+: There's a pin: the pawn on d7 cannot move without exposing the queen on d8. Color complex imbalances are guiding both plans. The opening is already testing tactical accuracy. One inaccurate developing move can concede practical control. White is slightly ahead in the evaluation (≈+2.5). A king hunt can emerge quickly if king safety loosens. Strategic focus: **bishop Sacrifice**.

Worth asking: What is the defensive task here — can White meet the threat with Bxf7+ (watch c6)?

a) Bxf7+ Kxf7 Ng5+ Kg8 (+2.5)
b) O-O d6 exd6 (+0.4)

**Bxf7+** keeps to the strongest continuation. It maintains active momentum. **Bxf7+** often leads to a stable structure with clear plans.

Practically speaking, **O-O** is viable, but decisive loss after d6.

White can follow the more straightforward practical plan.
```

## battery_open_file: Rook Battery: Doubling on the d-file

- `ply`: 22
- `playedMove`: `d1d2`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 606 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.826, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
11... R1d2: Doubled rooks can generate immediate file pressure. Plans and tactics now bite at every move. Piece coordination and king safety both matter. White has a modest edge (≈+0.4). A battery setup is building pressure on key lines. Strategic priority: **Rook Activation**.

a) R1d2 b6 (+0.4)
b) a3 b6 b4 (+0.3)

**R1d2** is accurate and consistent with the main plan. It solidifies the position. **R1d2** often leads to a stable structure with clear plans.

**a3** keeps the game in a technical channel where precise handling is rewarded.

Both players still need concrete accuracy before committing.
```

## bishop_pair_open: Bishop Pair: Dominance in open position

- `ply`: 35
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 744 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.771, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=4
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
18. Rxe8#: King activity matters: g1 has 3 safe steps. The open file is the key channel for major-piece activity. This is a technical endgame phase. Practical endgame technique matters more than broad plans. White is just a touch better. Long-term play favors the bishop pair as an enduring strategic asset. Move-order choices are justified by **Forced Mate**.

Worth asking: Does this fall into the Back Rank Mate?

a) Rxe8# (+0.5)
b) Bd4 a6 Rd1 (+0.4)

**Rxe8#** is fully sound and matches the position's demands. It poses serious practical questions. **Rxe8#** ends the game sequence on the spot.

**Bd4** favors structural clarity and methodical handling over complications.

The game remains balanced, and precision will decide the result.
```

## hanging_pawns_center: Hanging Pawns: c4/d4 (Structural tension)

- `ply`: 15
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1p2pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 928 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.706, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=16, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=16, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. cxd5: The pawn on c4 is hanging (1 attacker, no defenders). The hanging pawns in the center are a major strategic factor. This is still an opening structure. Small developmental choices can still reshape the structure. Neither side has established a clear advantage. A minority attack is becoming a practical queenside plan. Most sensible plans here converge on **Minority Attack**. Don't rush to clarify the pawn structure; the c-pawn break is a lever to keep in mind.

A key question: After cxd5, how should Black recapture —...exd5,...Nxd5, or...Bxd5?

a) cxd5 exd5 dxc5 bxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

**cxd5** is accurate and consistent with the main plan. It breaks through the center. The line after **cxd5** is relatively clean and technical, with less tactical turbulence.

**b3** aims for a technically manageable position with clear conversion paths.

The practical chances are still shared between both players.
```

## opposite_bishops_attack: Opposite-Colored Bishops: Attacking the King

- `ply`: 30
- `playedMove`: `e1e8`
- `analysisFen`: `4r1k1/pp3p1p/2b3p1/8/8/P1B2B2/1P3PPP/4R1K1 w - - 0 1`
- Metrics: 762 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.786, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... Rxe8+: King activity matters: g1 has 2 safe steps. The bishop pair increases pressure across both wings. The position has simplified into an endgame. The evaluation now depends on accurate technical handling. White has a small pull (≈+0.6). With opposite-colored bishops, both kings can come under sharper pressure. Strategic priority: **coordinating piece play before structural commitments**.

a) Rxe8+ Bxe8 Bxb7 (+0.6)
b) Bxc6 bxc6 Rxe8# (+0.6)

**Rxe8+** keeps to the strongest continuation. It continues the attack. The line after **Rxe8+** is clean and technical, where subtle king routes and tempi matter.

**Bxc6** aims for a technically manageable position with clear conversion paths.

The practical chances are still shared between both players.
```

## simplification_trade: Simplification: Trading into a won endgame

- `ply`: 45
- `playedMove`: `d5b5`
- `analysisFen`: `4r1k1/1p3ppp/8/3R4/8/P4P2/1P4PP/6K1 w - - 0 1`
- Metrics: 694 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.740, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
23. Rb5: The king on g1 is active (mobility: 3). Endgame precision now outweighs broad strategic plans. Piece activity outweighs broad strategic plans. White is slightly ahead in the evaluation. Simplification is a practical route to consolidate the position. The most reliable roadmap here is built around **keeping central levers available for later timing**.

a) Rb5 Re7 Kf2 f6 (+1.5)
b) Kf2 f6 Rb5 (+1.4)

**Rb5** is accurate and consistent with the main plan. It prevents counterplay. After **Rb5**, technical details matter more than tactical tricks.

**Kf2** favors structural clarity and methodical handling over complications.

White can follow the more straightforward practical plan.
```

## zugzwang_mutual: Mutual Zugzwang: Pawn Endgame (Precision)

- `ply`: 55
- `playedMove`: `d4c4`
- `analysisFen`: `8/8/p1p5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 847 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.694, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.69
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Generated commentary**

```text
28. c4: The king on d4 is well-placed for the endgame. Zugzwang ideas are central: useful moves are running out. The struggle has shifted into technical endgame play. Practical endgame technique matters more than broad plans. It's close to equal, with play for both sides. Move-order choices are justified by **King Activation**.

a) Kc3 (+0.0)
b) Ke4 (+0.0)
c) d4c4 (-0.5)

**c4** ?! is an inaccuracy; **Kc3** keeps better control. This is outside the top engine choices, and **Kc3** is the stable reference. Issue: slight practical concession. Consequence: piece coordination loosens and counterplay appears. Better is **Kc3**; it prepares for the next phase.

**Ke4** is a practical technical route that simplifies defensive duties.
Practically speaking, **d4c4** is viable, but slight practical concession.

It is easy to misstep if you relax.
```

## smothered_mate_prep: Smothered Mate Prep: 1.Nb5 (Tactical motif)

- `ply`: 15
- `playedMove`: `c3b5`
- `analysisFen`: `r1bq1rk1/pp1n1ppp/2n1p3/2ppP3/3P1B2/2N5/PPP1QPPP/2KR1BNR w - - 0 1`
- Metrics: 659 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.862, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=3
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
8. Nb5: Keep b5 in mind as an outpost square. The bishop pair is a long-term strategic asset. Opening choices are already forcing concrete decisions. One tempo can trigger forcing play. White is just a touch better. Smothered mate patterns are part of the tactical background. Most sensible plans here converge on **Piece Activation**.

a) Nb5 a6 Nd6 (+0.5)
b) Nf3 (+0.3)

**Nb5** keeps to the strongest continuation. It establishes a strong outpost. With **Nb5**, planning depth tends to matter more than short tactics.

**e1c1** aims for a technically manageable position with clear conversion paths.

Practical handling is relatively direct for both sides.
```

## knight_domination_endgame: Knight Domination: 1.Nb6 (Outplaying a bishop)

- `ply`: 42
- `playedMove`: `c4b6`
- `analysisFen`: `4k3/1p4p1/2p1p2p/p1P1P1p1/2NP2P1/1P6/5P1P/6K1 w - - 0 1`
- Metrics: 653 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.713, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
21... Nb6: Keep b6 in mind as an outpost square. Outpost control gives the knight a stable edge. Endgame details now dominate the game. King activity and tempi become decisive. White is slightly ahead in the evaluation (≈+1.8). The queenside minority attack is turning into the key practical route. The practical roadmap centers on **Minority Attack**.

a) Nb6 (+1.8)
b) f3 Kd7 Kf2 (+1.5)

**Nb6** keeps to the strongest continuation. It anchors a piece on a durable outpost. **Nb6** guides play into a precise conversion phase.

**f3** is the cleaner technical route, aiming for a stable structure.

White has the more comfortable practical route here.
```

## opening_novelty_e5: Opening Novelty: Handling an early ...e5 break

- `ply`: 10
- `playedMove`: `e1g1`
- `analysisFen`: `rnbqk2r/ppppbppp/2n5/4P3/2B5/2P2N2/PP3PPP/RNBQK2R w KQkq - 0 1`
- Metrics: 800 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.704, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
5... O-O: e-file pressure is the concrete lever in the current position. Color complex imbalances are guiding both plans. Early tactical motifs are now dictating move-order. Piece placement and tactics are now intertwined. White has a small pull. This position carries novelty value compared with the usual mainline treatment. The practical roadmap centers on **keeping the structure flexible**. The structure often turns on the e-pawn break, a lever to keep in mind.

a) O-O d6 exd6 (+0.4)
b) Qd5 O-O O-O (+0.4)

**O-O** is accurate and consistent with the main plan. It castles to safety. **O-O** lowers immediate tactical volatility and rewards precise coordination.

**Qd5** heads for a controlled position with fewer tactical swings.

There are no immediate tactical emergencies for either side.
```

## rook_lift_attack: Rook Lift: 1.Rd3 (Attacking the king)

- `ply`: 24
- `playedMove`: `d3d4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 697 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.828, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12... Rd4: There's a pin: the pawn on d5 cannot move without exposing the knight on d7. Doubled rooks can generate immediate file pressure. This middlegame phase rewards concrete, well-timed choices. Routine moves can quickly backfire here. White has a modest edge. A rook lift can become a key attacking mechanism. Strategic focus: **keeping central levers available for later timing**.

a) Rd4 f5 Rh4 (+0.4)
b) R1d2 b6 a3 (+0.3)

**Rd4** keeps to the strongest continuation. It keeps the opponent under pressure. **Rd4** aims for a tidy continuation with fewer forcing turns.

**R1d2** keeps the game in a technical channel where precise handling is rewarded.

Counterplay exists for both sides.
```

## outside_passed_pawn: Outside Passed Pawn: a-pawn advantage

- `ply`: 58
- `playedMove`: `d4e5`
- `analysisFen`: `8/8/p1k5/PpP5/1P1K4/8/8/8 w - - 0 1`
- Metrics: 622 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.742, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
29... Ke5: The king on d4 is well-placed for the endgame. The passed pawn is now a central practical factor. The game has entered a conversion-oriented endgame. Minor king-route details can decide the evaluation. White has the more pleasant position. Strategic priority: **King Activation**.

a) Ke5 Kc7 Kd5 (+1.0)
b) Kc3 Kd5 Kd3 (+0.0)

**Ke5** is accurate and consistent with the main plan. It repositions the piece. **Ke5** guides play into a precise conversion phase.

**Kc3** can work; the tradeoff is that significant disadvantage after Kd5.

Each side still has tactical resources that punish inaccurate move order.
```

## zwischenzug_tactics: Zwischenzug: 1.h4 (Tactical intermediate)

- `ply`: 26
- `playedMove`: `h4h5`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/7P/2NRBN2/PPP2PP1/3R2K1 w - - 0 1`
- Metrics: 643 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.779, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13... h5: The position now demands active middlegame decision-making. Move-order nuance can shift practical control. White can keep up mild pressure. The key tactical resource is a zwischenzug that disrupts automatic recapture. The practical roadmap centers on **keeping the structure flexible**.

a) h5 h6 g4 (+0.4)
b) R1d2 b6 a3 (+0.3)

**h5** is fully sound and matches the position's demands. It solidifies the position. **h5** aims for a tidy continuation with fewer forcing turns.

**R1d2** aims for a technically manageable position with clear conversion paths.

Fine margins mean technical accuracy still determines practical outcomes.
```

## pawn_break_f4: Pawn Break: 1.f4 (Kingside storm prep)

- `ply`: 21
- `playedMove`: `f2f4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 631 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.761, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=8, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
11. f4: Stacking rooks on one file is the practical plan. Middlegame priorities now hinge on concrete calculation. Piece coordination and king safety both matter. White has a modest edge. A pawn storm is the direct attacking plan around the king. Current play is organized around **a grip on the centre**.

a) f2f4 (+0.4)
b) a3 b6 b4 (+0.3)

**f4** keeps to the strongest continuation. It challenges the center. **f2f4** lowers immediate tactical volatility and rewards precise coordination.

**a3** favors structural clarity and methodical handling over complications.

The practical chances are still shared between both players.
```

## trapped_piece_rim: Trapped Piece: 1.g4 (Minor piece on the rim)

- `ply`: 18
- `playedMove`: `g4h5`
- `analysisFen`: `r1bqk2r/ppppbppp/2n1p3/4P2n/2B3P1/2P2N2/PP1P1P1P/RNBQK2R w KQkq - 0 1`
- Metrics: 782 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.739, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=15, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... gxh5: g-file pressure is the concrete lever in the current position. Color complex control is the key strategic battleground. The game remains in opening channels. Move-order precision matters more than forcing lines. White is pressing with a stable edge. A trapped piece motif is becoming relevant. Strategic priority: **coordinating piece play before structural commitments**. The structure often turns on the g-pawn break, a lever to keep in mind.

a) gxh5 g6 hxg6 (+3.5)
b) h3 d6 exd6 (+0.3)

**gxh5** is accurate and consistent with the main plan. It tests the pawn structure directly. With **gxh5**, planning depth tends to matter more than short tactics.

**h3** is playable, but decisive loss after d6.

White's practical choices are easier to execute with fewer risks.
```

## stalemate_resource: Stalemate Trick: 1.Rh1+ (Defensive resource)

- `ply`: 65
- `playedMove`: `h2h1`
- `analysisFen`: `8/8/8/8/8/5k2/7r/5K2 b - - 0 1`
- Metrics: 969 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.738, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=18, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=3
- Outline: 8 beats (MoveHeader, Context, DecisionPoint, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
33. Rh1#: The kings are in direct opposition. Rook activity on the seventh rank is becoming practical. The struggle has shifted into technical endgame play. Minor king-route details can decide the evaluation. Black is completely on top. Stalemate resources are part of the endgame calculation. The most reliable roadmap here is built around **Forced Mate**.

A key question: What is the defensive task here — can Black meet the threat with Rh1# (watch d2)?

a) Rd2 Ke1 Rd1+ (-10.0)
b) Rh1# (+0.0)

A significant oversight: king cut off was available.

**Rh1#** ?? is a blunder that hands over the game flow; **Rd2** was the only stable route. The top engine continuation still starts with **Rd2**. Issue: decisive loss. Consequence: king safety and coordination collapse at once. Better is **Rd2**; it poses serious practical questions. Defending against a phantom threat, wasting a valuable tempo.

Black can progress with measured moves and minimal tactical exposure.
```

## underpromotion_knight: Underpromotion: 1.e8=N+ (Tactical necessity)

- `ply`: 55
- `playedMove`: `e7e8n`
- `analysisFen`: `8/4P1k1/8/8/8/8/8/1K4n1 w - - 0 1`
- Metrics: 681 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.750, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=11, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
28. e8=N+: We are firmly in endgame territory. Minor king-route details can decide the evaluation. White is a bit better (≈+2.0). An underpromotion resource is a concrete tactical possibility. Move-order choices are justified by **Passed Pawn Advance**.

One practical question: How should White convert the advantage after e8=N+?

a) e8=N+ Kf8 Nf6 (+2.0)
b) e8=Q Nf3 (+0.0)

**e8=N+** is accurate and consistent with the main plan. It keeps the pressure on. The line after **e8=N+** is clean and technical, where subtle king routes and tempi matter.

**e8=Q** is playable, but significant disadvantage after Nf3.

White's strategic plan is easier to execute without tactical risk.
```

## kingside_pawn_storm: Pawn Storm: 1.g4 (Attacking the king)

- `ply`: 23
- `playedMove`: `g2g4`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 649 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.800, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=10, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
12. g4: Middlegame complexity is now front and center. Both sides must balance activity with king safety at each step. White is just a touch better (≈+0.5). A pawn storm is the direct attacking plan around the king. Most sensible plans here converge on **preserving the central pawn structure**.

a) g4 h6 g5 (+0.5)
b) a3 b6 b4 (+0.3)

**g4** is accurate and consistent with the main plan. It keeps defensive resources coordinated. With **g4**, planning depth tends to matter more than short tactics.

**a3** favors structural clarity and methodical handling over complications.

The margin is narrow enough that practical accuracy remains decisive.
```

## quiet_improvement_move: Quiet Improvement: 1.Re1 (Improving the rook)

- `ply`: 25
- `playedMove`: `d3e1`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 673 chars, 5 paragraphs
- Quality: score=100/100, lexical=0.784, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=13, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=8, scoreTokens=2
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
13. e1: Stacking rooks on one file is the practical plan. Middlegame priorities now hinge on concrete calculation. Small structural concessions become long-term targets. White has a small pull. A prophylactic idea is central to limiting counterplay. The most reliable roadmap here is built around **control of key central squares**.

a) d3e1 (+0.4)
b) a3 b6 b4 (+0.3)

**e1** is accurate and consistent with the main plan. It fights for the center. With **d3e1**, planning depth tends to matter more than short tactics.

**a3** favors structural clarity and methodical handling over complications.

The position stays tense, and one careless tempo can swing the initiative.
```

## repetition_decision_move: Repetition: Deciding for/against a draw

- `ply`: 38
- `playedMove`: `e1d1`
- `analysisFen`: `4r1k1/pp3ppp/8/3b4/8/P1B2P2/1P4PP/4R1K1 w - - 0 1`
- Metrics: 732 chars, 4 paragraphs
- Quality: score=100/100, lexical=0.720, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=14, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=12, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, Evidence, ConditionalPlan, MainMove, PsychologicalVerdict, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
19... Rd1: The king on g1 is well-placed for the endgame. File control along the open line can dictate the middlegame. This phase is now about endgame conversion technique. Precision matters more than ambition here. The position is about level. A repeat remains a practical fallback if neither side can improve safely. The practical roadmap centers on **keeping the pawn chain intact**.

a) Bd4 a6 Rd1 (+0.2)
b) Rd1 Bc6 Re1 (+0.1)

**Rd1** is slightly less direct; **Bd4** is cleaner. Engine preference still leans to **Bd4**, but the practical gap is modest. Better is **Bd4**; it centralizes the piece. Defending against a phantom threat, wasting a valuable tempo.

Defensive precision is more important than active-looking moves.
```

## central_liquidation_move: Central Liquidation: 1.cxd5 (Clearing the center)

- `ply`: 18
- `playedMove`: `c4d5`
- `analysisFen`: `rn1qk2r/pb2bppp/1pp1pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 w KQkq - 0 1`
- Metrics: 919 chars, 6 paragraphs
- Quality: score=100/100, lexical=0.748, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=17, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=15, scoreTokens=2
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
9... cxd5: The pawn on c4 is hanging (1 attacker, no defenders). Central liquidation is changing the position's character. The position remains in a developmental opening stage. The middlegame plans are only now taking shape. White has a small pull. A minority attack is becoming a practical queenside plan. The practical roadmap centers on **holding the center without clarifying too early**. With tension preserved, the c-pawn break becomes a thematic lever.

One practical question: After cxd5, how should Black recapture —...cxd5,...exd5, or...Nxd5?

a) cxd5 exd5 dxc5 (+0.3)
b) b3 O-O Bb2 (+0.2)

**cxd5** is fully sound and matches the position's demands. It opens the position. **cxd5** often leads to a stable structure with clear plans.

**b3** is a practical technical route that simplifies defensive duties.

The position remains dynamically balanced. White gets a Positional return for the material deficit.
```

## final_sanity_check: Final Sanity Check: Complex position

- `ply`: 30
- `playedMove`: `a2a3`
- `analysisFen`: `r4rk1/pp1nbppp/2p1p3/3p4/8/2NRBN2/PPP2PPP/3R2K1 w - - 0 1`
- Metrics: 576 chars, 4 paragraphs
- Quality: score=100/100, lexical=0.788, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=12, dup=0, boilerplate=0, mateToneConflict=0, moveTokens=9, scoreTokens=2
- Outline: 5 beats (MoveHeader, Context, Evidence, MainMove, WrapUp)
- Evidence: purpose=engine_alternatives, branches=2

**Generated commentary**

```text
15... a3: A minority attack structure is emerging on the queenside. Plans and tactics now bite at every move. Move-order nuance can shift practical control. White has a small pull. A rook lift can become a key attacking mechanism. The practical roadmap centers on **avoiding premature pawn breaks**.

a) R1d2 (+0.4)
b) a3 b6 b4 (+0.3)

**a3** is slightly less direct; **R1d2** is cleaner. Engine-wise, **R1d2** stays first, with only a small practical difference. Better is **R1d2**; it solidifies the position.

Neither side has converted small edges into a stable advantage.
```

