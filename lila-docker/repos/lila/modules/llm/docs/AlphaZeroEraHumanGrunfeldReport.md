# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/AlphaZeroEraHumanGrunfeldCorpus.json`
- Results: 10/10 passed

## Quality Summary

- Avg quality score: 90.0 / 100
- Avg lexical diversity: 0.589
- Avg variation-anchor coverage: 0.800
- Opening precedent mentions: 10 across 8/10 cases
- Balanced gate: PASS (repeated 5-gram [4+ cases]=0, target=0; precedent coverage=8/10, target>=8)
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 19 across 10 cases

## Cross-Case Repetition

- No sentence repeated across 3+ cases.

- No 5-gram pattern repeated across 4+ cases.

## azg_2018_01_13_kramnik_wei: Kramnik, Vladimir vs Wei, Yi (80th Tata Steel GpA)

- `ply`: 26
- `playedMove`: `g7d4`
- `analysisFen`: `r1bq1rk1/pp2ppbp/6p1/8/3Bn3/5NP1/PP2PPBP/R2Q1RK1 b - - 1 1`
- Metrics: 2506 chars, 6 paragraphs
- Precedent mentions: 0
- Quality: score=90/100, lexical=0.584, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=32, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=41, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.58
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.01.13 D77 1-0 | selected loser-side critical move (rank=5, cpLoss=65)
```

**Generated commentary**

```text
13... Bxd4: The immediate concrete issue is the positional threat on g7. Color complex control is the key strategic battleground. Middlegame priorities now hinge on concrete calculation. Small structural concessions become long-term targets. Black has the easier side to press with (≈-0.6). Strategic priority: **Piece Activation**. The pawn on e2 is pinned to the knight on f3.

One practical question: After Bxd4, how should White recapture — Nxd4 or Qxd4?

a) Bxd4 (-0.6)
b) Be6 Bxg7 Qxd1 Rfxd1 (-0.1)
c) Nd6 e3 Re8 Bxg7 (+0.1)

**Bxd4** fits the position's strategic demands, and it limits tactical drift. It forces uncomfortable defensive choices. With **Bxd4**, the structure stays stable and plan choices become clearer. From the board, positional pressure on g7 remains unresolved. Clearest read: Bxd4 slows the initiative race deliberately, betting that the resulting position is easier to control The explanatory lens is initiative control with short-horizon consequences. Validation lines up with Centralization(Bishop on 27) and engine gap 0.0 pawns. A supporting hypothesis is that Bxd4 keeps the position tied to piece activation, delaying unnecessary plan detours It supports the strategic route reading. In practical terms, the split should appear in the next few moves, especially around tempo initiative handling.

**Be6** can work, although after Bxg7, execution around Bxg7 eases the defensive task. The practical gap to **Bxd4** is around 0.5 pawns. With **Be6**, conversion around **Be6** can stay smoother, but initiative around **Be6** can swing when **Be6** hands away a tempo. Compared with **Bxd4**, **Be6** likely rebalances the plan toward long-plan map instead of momentum balance. After **Be6**, concrete commitments harden and coordination plans must be rebuilt.
**Nd6** is viable over the board, though move-order precision matters. The 3rd choice is workable, but **Bxd4** still leads by roughly 0.6 pawns. With **Nd6**, a move-order slip can expose coordination gaps, and recovery windows are short. Versus the principal choice **Bxd4**, **Nd6** plausibly redirects emphasis to piece coordination while reducing tempo initiative priority. The key practical fork is likely during the first serious middlegame regrouping after **Nd6**.

The opponent may threaten positional on g7. Fine margins mean technical accuracy still determines practical outcomes. Key difference: **Bxd4** and **Be6** separate by initiative timing versus plan cadence across short vs medium horizon.
```

## azg_2018_01_16_kramnik_svidler: Kramnik, Vladimir vs Svidler, Peter (80th Tata Steel GpA)

- `ply`: 36
- `playedMove`: `c8c7`
- `analysisFen`: `r1r1n1k1/pp1Rppbp/6p1/4P3/8/1PN1B1P1/1P2P1KP/R7 b - - 2 1`
- Metrics: 2838 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=80/100, lexical=0.548, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=39, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=48, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 2
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated four-gram templates present (2)
  - Advisory: lexical diversity soft-low: 0.55
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.01.16 D78 1-0 | selected loser-side critical move (rank=5, cpLoss=87)
```

**Generated commentary**

```text
18... Rc7: The position currently hinges on the positional threat on a7. Queenside minority attack ideas are now practical. Plans and tactics now bite at every move. Small inaccuracies can hand over momentum immediately. Black can keep up mild pressure. Key theme: **Queenside Attack**. The pawn on b7 is underdefended: 1 attacker, no defenders.

A key question: What is the defensive task here — can Black meet the threat with Bxe5 (watch c3)?

a) Rc7 (-0.4)
b) Bxe5 Rxb7 Bxc3 bxc3 (+0.1)
c) Kf8 Rxb7 Rc7 Rxc7 (+0.4)

**Rc7** keeps conversion tasks straightforward in practice, because structure and activity stay connected. It keeps the pressure on. With **Rc7**, the structure stays stable and plan choices become clearer. Observed directly: positional pressure on a7 remains unresolved. Strongest read: Rc7 slows the initiative race deliberately, betting that the resulting position is easier to control The explanatory lens is momentum balance with short-horizon consequences. Validation lines up with engine gap 0.0 pawns and RookLift(Rook to rank 7). A supporting hypothesis is that Rc7 keeps the position tied to queenside attack, delaying unnecessary plan detours This reinforces the strategic route perspective. Short-horizon test: the next move-order around tempo initiative will determine whether **Rc7** holds up. A model game in the Rc7 line runs: In V. Kramnik-P. Svidler (2018), after 18... Rc7 19. Rxa7 Rb8 20. Rd5 b6..., V. Kramnik won (1-0). Rc7, Rxa7, then Rb8 became the turning point once exchanges reshaped piece activity and defensive resources.

From a practical angle, **Bxe5** is viable, yet it grants a cleaner practical route to the opponent after Rxb7. The practical gap to **Rc7** is around 0.5 pawns. With **Bxe5**, conversion around **Bxe5** can stay smoother around piece centralization, but initiative around **Bxe5** can swing when **Bxe5** hands away a tempo. In contrast to **Rc7**, **Bxe5** likely redirects emphasis to plan cadence while reducing momentum balance priority. The key practical fork is likely during the first serious middlegame regrouping after **Bxe5**.
**Kf8** remains practical, but one inaccurate follow-up can change the assessment. As a lower-tier option (around 3rd), this trails **Rc7** by roughly 0.8 pawns. After **Kf8**, sequence accuracy matters because coordination and activity can separate quickly around queenside attack preparation. Relative to **Rc7**, **Kf8** likely redirects emphasis to plan direction while reducing initiative control priority. From a medium-horizon view, **Kf8** often diverges once plan commitments become irreversible.

The opponent may threaten positional on a7. Precise defensive choices are needed to keep equality. Practical key difference: **Rc7** against **Bxe5** is momentum balance versus long-plan map under short vs medium horizon.
```

## azg_2018_03_26_mamedyarov_grischuk: Mamedyarov, Shakhriyar vs Grischuk, Alexander (FIDE Candidates 2018)

- `ply`: 72
- `playedMove`: `c3c2`
- `analysisFen`: `8/4pp1k/2B1P1p1/1Q5p/7P/q1p3P1/5PK1/8 b - - 0 1`
- Metrics: 2528 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=80/100, lexical=0.596, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=37, dup=0, triRepeat=0, fourRepeat=5, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=39, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 5
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated four-gram templates present (5)
  - Advisory: lexical diversity soft-low: 0.60
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.03.26 D77 1-0 | selected loser-side critical move (rank=5, cpLoss=471)
```

**Generated commentary**

```text
36... c2: Keep an eye on the pawn on f7 — 1 attacker, no defenders. The open file is the key channel for major-piece activity. The game has entered a conversion-oriented endgame. King activity and tempi become decisive. White is a bit better. Key theme: **postponing pawn commitments until pieces are ready**. Once the structure is clarified, the e-pawn break tends to carry more weight.

a) c2 (+1.9)
b) f5 Qc4 Qa5 Ba4 (+2.4)
c) f6 Qc4 Kg7 Qd4 (+2.9)

**c2** is a reliable move that maintains the reference continuation, while king safety stays stable. It pushes the passed pawn. **c2** keeps the structure stable and highlights endgame technique. Concrete observation first: c2 changes which plan family is easier to execute. Strongest read: c2 prioritizes stability over momentum, making initiative handoff the central practical risk The underlying axis is momentum balance, and the payoff window is short-horizon. Validation evidence includes OpenFileControl(Rook on 0-file) and engine gap 0.0 pawns. A supporting hypothesis is that c2 preserves the pawn chain maintenance framework and avoids premature route changes This reinforces the plan cadence perspective. In practical terms, the split should appear in the next few moves, especially around momentum balance handling. A model game in the c2 line runs: In S. Mamedyarov-A. Grischuk (2018), after 36... c2 37. exf7 Kg7 38. Be4 c1=Q..., S. Mamedyarov won (1-0). From c2, exf7, then Kg7, the decisive shift was structural transformation and piece rerouting.

From a practical angle, **f5** is viable, yet it yields a modest practical concession once Qc4 appears. The practical gap to **c2** is around 0.5 pawns. **f5** keeps practical burden manageable by preserving coordination before exchanges. Measured against **c2**, **f5** likely places pawn-break timing ahead of momentum balance in the practical order. The split should surface in immediate move-order fights.
Over the board, **f6** is acceptable if tactical details are controlled. This 3rd line leaves a large practical deficit versus **c2** (about 1.0 pawns). With **f6**, a move-order slip can expose coordination gaps, and recovery windows are short. Relative to **c2**, **f6** likely places plan cadence ahead of momentum balance in the practical order. A few moves later, **f6** often shifts which side controls the strategic transition.

Black is under practical pressure and must be precise. Decisive split: **c2** versus **f5** on tempo initiative versus pawn-lever timing with a shared short horizon.
```

## azg_2018_06_01_aronian_mamedyarov: Aronian, Levon vs Mamedyarov, Shakhriyar (6th Norway Chess 2018)

- `ply`: 24
- `playedMove`: `e7e5`
- `analysisFen`: `r1bq1rk1/p3ppbp/1p4p1/n1p5/3PP3/2PBB3/P3NPPP/2RQ1RK1 b - - 1 1`
- Metrics: 3035 chars, 5 paragraphs
- Precedent mentions: 3
- Quality: score=100/100, lexical=0.604, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=41, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=45, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.60
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.06.01 D87 1-0 | selected loser-side critical move (rank=5, cpLoss=94)
```

**Generated commentary**

```text
12... e5: The pin on d4 slows coordination of that pawn, costing valuable tempi. The bad bishop is restricted by its own pawn chain. Opening development is still in progress. Piece coordination remains central. Black has a modest edge (≈-0.4). Strategic focus: **Piece Activation**. Releasing the tension can make the e-pawn break more potent.

a) e5 (-0.4)
b) Qd7 f3 Rd8 Qe1 (+0.1)
c) e6 dxc5 Bb7 f4 (+0.3)

**e5** is a reliable move that maintains the reference continuation, while king safety stays stable. It repositions the piece. With **e5**, planning depth tends to matter more than short tactics. Observed directly: e5 changes which plan family is easier to execute. Working hypothesis: e5 anchors play around piece activation, so follow-up choices stay structurally coherent The underlying axis is strategic route, and the payoff window is medium-horizon. Validation lines up with plan table confidence 1.75 and sampled line rank is 1. A supporting hypothesis is that e5 tries to preserve structure first, postponing irreversible pawn commitments It supports the structure management reading. After development, plan direction decisions are likely to determine whether **e5** remains robust. At this branch, master games diverged in three practical directions: A) In Ruslan Ponomariov-Alexander Kovchan (2011), after 12... e5 13. dxc5 Be6 14. c4 bxc5..., Ruslan Ponomariov won (1-0). The practical route is e5 -> dxc5 -> Be6. B) In Alexey Korotylev-Artyom Timofeev (2009), a similar branch ended with Alexey Korotylev winning (1-0). The practical turning factor was structural pawn shifts that changed long-plan priorities. C) In Krishnan Sasikiran-Gata Kamsky (2008), after 12... e5 13. Qa4 Bd7 14. Qa3 Be6..., Gata Kamsky won (0-1). Results hinged on who managed pawn-skeleton changes that rerouted strategic plans more accurately. These precedent lines point to one key driver: pawn-structure transformation and plan rerouting.

**Qd7** is serviceable over the board, but it yields a modest practical concession once f3 appears. The engine still points to **e5** as cleaner, by about 0.5 pawns. **Qd7** keeps practical burden manageable around positional pressure by preserving coordination before exchanges. Set against **e5**, **Qd7** likely stays on the plan cadence route, yet it sequences commitments differently. Middlegame pressure around **Qd7** is where this route starts to separate from the main plan.
Over the board, **e6** is acceptable if tactical details are controlled. The 3rd choice is workable, but **e5** still leads by roughly 0.7 pawns. In practical terms, **e6** is judged by conversion ease, because defensive coordination can diverge quickly. Measured against **e5**, **e6** likely changes the center of gravity from strategic route to central break timing. Concrete tactical play after **e6** should expose the split quickly.

Neither side has converted small edges into a stable advantage. Decisively, **e5** and **Qd7** diverge through the same plan direction axis, with a shared medium horizon.
```

## azg_2018_06_01_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (6th Norway Chess 2018)

- `ply`: 72
- `playedMove`: `a2c4`
- `analysisFen`: `5rk1/6bp/p4p2/2p5/2Pq1N1Q/6RP/b5pK/4R3 b - - 4 1`
- Metrics: 2712 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=85/100, lexical=0.567, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=38, dup=0, triRepeat=0, fourRepeat=1, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=44, scoreTokens=3
- Advisory findings:
  - Advisory: quality score below target (90): 85
  - Advisory: repeated four-gram templates present (1)
  - Advisory: lexical diversity soft-low: 0.57
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.06.01 D86 1-0 | selected loser-side critical move (rank=5, cpLoss=312)
```

**Generated commentary**

```text
36... Bxc4: On the board right now, handling the material threat on g7 is the priority. Open-board dynamics favor the bishop pair. This middlegame phase rewards concrete, well-timed choices. Automatic play is dangerous in this position. White holds a clear advantage (≈+3.3). A bad bishop handicap is still shaping the strategic plans. Key theme: **Exploiting Space Advantage**. The pawn on g2 is hanging (3 attackers, no defenders).

a) Bxc4 (+3.3)
b) f5 Re7 Bxc4 Rexg7+ (+3.8)
c) g1=B+ Rexg1 Rf7 Qh6 (+5.7)

**Bxc4** steers the position into a stable plan with clear follow-up, and defensive duties remain light. It maintains the tension. After **Bxc4**, strategy tightens; tactics recede. Initial board read: material pressure on g7 remains unresolved. Working hypothesis: Bxc4 prioritizes stability over momentum, making initiative handoff the central practical risk Interpret this through initiative timing, where short-horizon tradeoffs dominate. Validation evidence points to engine gap 0.0 pawns and Pin(Bishop on g7 to King on g8). A supporting hypothesis is that Bxc4 preserves the exploiting space advantage framework and avoids premature route changes It supports the plan direction reading. In practical terms, the split should appear in the next few moves, especially around initiative control handling. In Sergey Karjakin-M. Vachier Lagrave (2018), after 36... Bxc4 37. Qh6 f5 38. Nh5, Sergey Karjakin won (1-0). This line is anchored by Bxc4. Bxc4, Qh6, then f5 marked the initiative turning point, as initiative control shifted to one side.

From a practical angle, **f5** is viable, yet after Re7, execution around Re7 eases the defensive task. The practical gap to **Bxc4** is around 0.5 pawns. **f5** keeps practical burden manageable around passed pawn advance by preserving coordination before exchanges. In contrast to **Bxc4**, **f5** likely shifts priority toward defensive king timing rather than initiative timing. Concrete tactical play after **f5** should expose the split quickly.
Over the board, **g1=B+** is acceptable if tactical details are controlled. The score gap to **Bxc4** is substantial here (about 2.4 pawns). In practical terms, **g1=B+** is judged by conversion ease around sharp attack with check, because defensive coordination can diverge quickly. Compared with **Bxc4**, **g1=B+** plausibly shifts priority toward strategic route rather than tempo initiative. The real test for **g1=B+** appears when middlegame plans must be fixed to one structure.

The opponent may threaten material on g7. Black has to defend accurately to stay afloat. Decisively, **Bxc4** and **f5** diverge through initiative timing versus defensive king timing, with a shared short horizon.
```

## azg_2019_05_17_wojtaszek_mamedyarov: Wojtaszek, Radoslaw vs Mamedyarov, Shakhriyar (Moscow FIDE Grand Prix)

- `ply`: 24
- `playedMove`: `f6e4`
- `analysisFen`: `r1b2rk1/pp1nppbp/5np1/qN6/2B2B2/1Q2P3/PP2NPPP/2R2RK1 b - - 5 1`
- Metrics: 2580 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.614, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=36, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=44, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.61
- Outline: 7 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.05.17 D82 1-0 | selected loser-side critical move (rank=3, cpLoss=342)
```

**Generated commentary**

```text
12... Ne4: f7 is pinned, leaving the pawn with limited mobility. Rook lift geometry is becoming available. Opening priorities are still being negotiated move by move. Small developmental choices can still reshape the structure. Black has a comfortable plus. A battery setup is building pressure on key lines. Key theme: **keeping the pawn chain intact**.

a) Ne8 Rc2 a6 (-3.4)
b) e5 Bg5 Nc5 Qc3 (-0.2)
c) Ne4 Nc7 Nd2 Qb5 (+0.1)

A significant oversight: pin was available.

**Ne4** ?? is a blunder; it allows a forcing sequence, so your position loses tactical control. **Ne8** was required. This is outside the top engine choices, and **Ne8** remains the stable reference for conversion. In strategic terms, Issue: it allows a fork by the knight on d2 against rook on f1 and queen on b3. As a result, Consequence: tactical control flips immediately and conversion becomes straightforward. Therefore, Better is **Ne8**; it improves the piece's scope. From the board, decisive loss (3.4) after Nc7. A likely explanation is that Ne4 selects a different knight route, shifting c-pawn flexibility, central tension timing The explanatory lens is coordination lanes with medium-horizon consequences. Verification remains conditional: knight development route diverges from main line and piece move directly changes coordination map back the claim, while coordination route is slower than principal line is unresolved. A supporting hypothesis is that Ne8 concedes some initiative for stability, so the practical test is whether counterplay can be contained It supports the momentum balance reading. The practical burden appears in the middlegame phase, once coordination lanes tradeoffs become concrete. In R. Wojtaszek-S. Mamedyarov (2019), after 12... Ne4 13. Bc7 b6 14. Bxf7+ Rxf7..., R. Wojtaszek won (1-0). This line is anchored by Ne4. The critical turning point was Ne4, Bc7, then b6, where exchange decisions fixed the practical balance.

**e5** stays in range, though it allows a forcing collapse after Bg5. In engine terms, **Ne8** holds roughly a 3.1 pawns edge. With **e5**, conversion around **e5** can stay smoother around prophylactic defense, but initiative around **e5** can swing when **e5** hands away a tempo. Versus **Ne8**, **e5** plausibly keeps the same plan direction focus, but the timing window shifts. The real test for **e5** appears when middlegame plans must be fixed to one structure.

Black has the more comfortable practical route here. Decisively, **Ne8** and **e5** diverge through tempo initiative versus plan direction, with short vs medium horizon.
```

## azg_2019_06_06_carlsen_grischuk: Carlsen, Magnus vs Grischuk, Alexander (7th Norway Chess 2019)

- `ply`: 38
- `playedMove`: `b6a4`
- `analysisFen`: `r2r2k1/p4pbp/1n2p1p1/1p1P3P/2p1PP2/q1P1BB2/P2Q2P1/2R2RK1 b - - 0 1`
- Metrics: 2665 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.595, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=37, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=40, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.60
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.06.06 D85 1-0 | selected loser-side critical move (rank=5, cpLoss=174)
```

**Generated commentary**

```text
19... Na4: The pin restrains the pawn on d5, reducing practical flexibility. The good bishop has unobstructed diagonals and lasting influence. Plans and tactics now bite at every move. Practical control can change after a single move-order slip. White has a small pull (≈+0.3). The practical roadmap centers on **synchronizing development with future pawn breaks**. Keep the pawn tension; the e-pawn break is a useful lever.

a) Na4 (+0.3)
b) Rd7 Qf2 exd5 Bc5 (+0.8)
c) Rac8 Qf2 exd5 hxg6 (+0.9)

**Na4** keeps the technical roadmap compact and stable, and move-order risks stay manageable. It places the piece on an unassailable square. After **Na4**, strategy tightens; tactics recede. From the board, Na4 reshapes the practical balance. Working hypothesis: Na4 changes the structural balance, trading immediate activity for longer-term square commitments Interpret this through structure management, where long-horizon tradeoffs dominate. Validation lines up with pawn tension context is active and fact-level structural weakness signal. A supporting hypothesis is that Na4 delays direct break action so supporting pieces can coordinate first This reinforces the pawn-break timing perspective. The implication is long-term; pawn-structure handling tradeoffs here are likely to resurface in the ending. In M. Carlsen-A. Grischuk (2019), after 19... Na4 20. hxg6 hxg6 21. f5 exf5..., M. Carlsen won (1-0). This line is anchored by Na4. From Na4, hxg6, then hxg6, the decisive shift was structural transformation and piece rerouting.

**Rd7** is playable, but after Qf2, execution around Qf2 eases the defensive task. Compared with **Na4**, engine evaluation drops by roughly 0.5 pawns. Handled precisely, **Rd7** keeps coordination and king safety linked around positional pressure through the next phase. Measured against **Na4**, **Rd7** likely rebalances the plan toward plan direction instead of structural control. This contrast tends to become visible when **Rd7** reaches concrete middlegame commitments.
In practical play, **Rac8** is viable, and the next two moves must stay accurate. As a 3rd practical-tier choice, this trails **Na4** by about 0.6 pawns. After **Rac8**, king safety and tempo stay linked, so one inaccurate sequence can hand over initiative. Versus the principal choice **Na4**, **Rac8** likely redirects emphasis to pawn-break timing while reducing structural control priority. This difference is expected to matter right away in concrete sequencing.

Defensive accuracy is the main practical requirement in this phase. Decisive split: **Na4** versus **Rd7** on structure management versus pawn-break timing with long vs short horizon.
```

## azg_2019_06_12_ding_mamedyarov: Ding, Liren vs Mamedyarov, Shakhriyar (7th Norway Chess 2019)

- `ply`: 40
- `playedMove`: `c8c5`
- `analysisFen`: `2r1q1k1/pp1b1rbp/1n1P2p1/6B1/4P1n1/2N2N2/PP4P1/1K1RQB1R b - - 6 1`
- Metrics: 2401 chars, 5 paragraphs
- Precedent mentions: 0
- Quality: score=85/100, lexical=0.592, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=31, dup=0, triRepeat=0, fourRepeat=1, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=32, scoreTokens=3
- Advisory findings:
  - Advisory: quality score below target (90): 85
  - Advisory: repeated four-gram templates present (1)
  - Advisory: lexical diversity soft-low: 0.59
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.06.12 D70 1-0 | selected loser-side critical move (rank=4, cpLoss=219)
```

**Generated commentary**

```text
20... Rc5: An outpost on g5 could be valuable for a knight. The open file is the key channel for major-piece activity. Development now intersects with concrete calculation. One inaccurate developing move can concede practical control. Black is a bit better (≈-2.8). Minority attack play on the queenside is becoming the most practical plan. Strategic focus: **Blockade on d7**. Watch for the h-pawn break — it is a useful lever.

a) Rc5 (-2.8)
b) h6 Rc1 hxg5 Nxg5 (-2.3)
c) Nc4 Bxc4 Rxc4 Bd2 (-0.7)

**Rc5** fits the position's strategic demands, and it limits tactical drift. It prepares for the next phase. With **Rc5**, planning depth tends to matter more than short tactics. Initial board read: Rc5 changes which plan family is easier to execute. Working hypothesis: Rc5 trades immediate initiative for structure, and the key question is if counterplay arrives in time Interpret this through tempo initiative, where short-horizon tradeoffs dominate. Validation evidence includes engine gap 0.0 pawns and RookLift(Rook to rank 5). A supporting hypothesis is that Rc5 anchors play around knight sacrifice, so follow-up choices stay structurally coherent It supports the plan direction reading. Immediate practical impact is expected: initiative control timing in the next sequence is critical.

**h6** is serviceable over the board, but it yields a modest practical concession once Rc1 appears. The practical gap to **Rc5** is around 0.5 pawns. **h6** keeps practical burden manageable by preserving coordination before exchanges. Compared with **Rc5**, **h6** likely rebalances the plan toward square-complex management instead of initiative control. With **h6**, this split should reappear in long-term conversion phases.
**Nc4** remains practical, but one inaccurate follow-up can change the assessment. This 3rd line leaves a large practical deficit versus **Rc5** (about 2.2 pawns). In practical terms, **Nc4** is judged by conversion ease around establishing outpost, because defensive coordination can diverge quickly. Versus **Rc5**, **Nc4** plausibly rebalances the plan toward structure management instead of momentum balance. The divergence after **Nc4** is expected to surface later in the endgame trajectory.

Black can improve with lower practical risk move by move. Decisively, **Rc5** and **h6** diverge through tempo initiative versus structural control, with short vs long horizon.
```

## azg_2019_08_25_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (7th Sinquefield Cup 2019)

- `ply`: 66
- `playedMove`: `g7h6`
- `analysisFen`: `8/3Q2kp/6p1/2p3q1/P1B5/4b1P1/7P/5K2 b - - 1 1`
- Metrics: 2769 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.587, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=36, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=42, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 2
- Advisory findings:
  - Advisory: repeated four-gram templates present (2)
  - Advisory: lexical diversity soft-low: 0.59
- Outline: 8 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.08.25 D86 1-0 | selected loser-side critical move (rank=2, cpLoss=167)
```

**Generated commentary**

```text
33... Kh6: A fork motif is in the air: e7 can attack bishop on e3 and king on g7. Managing the hanging pawns will decide the middlegame plans. Endgame precision now outweighs broad strategic plans. Precision matters more than ambition here. The position is practically balanced with chances for both sides. Opposite-colored bishops increase attacking chances for both sides. Strategic priority: **Perpetual Check**.

a) Qe7 Qxe7+ Kh6 Qxe3+ (+0.0)
b) Kh6 Qf7 Bd4 a5 (+1.7)
c) Kf6 Qe6+ Kg7 Qf7+ (+8.3)

A noticeable oversight: check was available.

**Kh6** ? is a clear mistake; it gives the opponent initiative, so practical defense becomes harder. Stronger is **Qe7**. In engine ordering, **Qe7** remains first, while this line requires tighter coordination. That makes the practical picture clear: Issue: significant disadvantage after Qf7. So Consequence: the opponent improves with forcing moves while your position stays passive. For that reason, Better is **Qe7**; it improves the piece's scope. From the board, significant disadvantage (1.7) after Qf7. One possibility is that Kh6 redirects play toward prophylactic defense, creating a new strategic branch from the main continuation Interpret this through long-plan map, where medium-horizon tradeoffs dominate. Validation is mixed: engine list position is 2 and primary plan score sits at 1.50 support the idea, but engine gap is significant for this route keeps caution necessary. A supporting hypothesis is that Qe7 alters king-safety tempo, so defensive coordination must stay synchronized with the next forcing move This reinforces the king-safety timing perspective. Practically, this should influence middlegame choices where long-plan map commitments are tested. In the Kh6 branch, a reference game shows: In Sergey Karjakin-M. Vachier Lagrave (2019), after 33... Kh6 34. Qh3+ Kg7 35. Qe6 Bd4..., Sergey Karjakin won (1-0). From Kh6, Qh3+, then Kg7, the decisive shift was initiative management rather than static factors. This concedes initiative, and as a result your defensive options narrow. Defending against a phantom threat, wasting a valuable tempo.

**Kf6** is playable, but it allows a forcing collapse after Qe6+. In engine terms this continuation drops to about 3rd, with **Qe7** up by 8.3 pawns. From a practical-conversion view, **Kf6** stays reliable around prophylactic defense when defensive coverage remains synchronized. Relative to **Qe7**, **Kf6** plausibly stays on the strategic route route, yet it sequences commitments differently. A few moves later, **Kf6** often shifts which side controls the strategic transition.

The defensive burden is noticeable. Final decisive split: **Qe7** vs **Kh6**, defined by defensive king timing versus long-plan map and short vs medium horizon.
```

## azg_2019_08_28_giri_nepomniachtchi: Giri, Anish vs Nepomniachtchi, Ian (7th Sinquefield Cup 2019)

- `ply`: 42
- `playedMove`: `e6d7`
- `analysisFen`: `r1r3k1/p3ppbp/4b1p1/q1Pp4/3N1B2/2R1P2P/P1Q2PP1/5RK1 b - - 2 1`
- Metrics: 2580 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.601, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=34, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=46, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.60
- Outline: 7 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.08.28 D83 1-0 | selected loser-side critical move (rank=3, cpLoss=253)
```

**Generated commentary**

```text
21... Bd7: The pin on e5 slows coordination of that pawn, costing valuable tempi. Open-board dynamics favor the bishop pair. Middlegame complexity is now front and center. Initiative and defensive resources are closely balanced. Black has a slight advantage (≈-1.4). The bad bishop remains a lasting positional burden. Strategic priority: **keeping the pawn chain intact**.

a) Bf5 Qb2 e5 Ra3 (-1.4)
b) Kh8 g4 Bd7 c6 (+1.0)
c) Bd7 c6 Be8 Rc1 (+1.2)

A significant oversight: overloading was available.

**Bd7** ?? is a mistake that worsens coordination, while **Bf5** keeps the structure easier to manage. This sits below the principal engine candidates, so **Bf5** gives the more reliable setup. In strategic terms, Issue: it allows a skewer: queen on b6 can hit knight on d4 and then pawn on e3. As a result, Consequence: practical control shifts and defense becomes uncomfortable. Therefore, Better is **Bf5**; it prepares for the next phase. Observed directly: decisive loss (2.5) after c6. A cautious hypothesis is that Bd7 shifts the game into a prophylactic defense route, with a different plan cadence from the principal line The explanatory lens is plan direction with medium-horizon consequences. Validation is mixed: plan match score registers 0.90 and principal-variation rank reads 3 support the idea, but engine gap is significant for this route keeps caution necessary. A supporting hypothesis is that Bf5 concedes some initiative for stability, so the practical test is whether counterplay can be contained It supports the initiative timing reading. After development, plan direction decisions are likely to determine whether **Bd7** remains robust. A model game in the Bd7 line runs: In A. Giri-I. Nepomniachtchi (2019), after 21... Bd7 22. c6 Be8 23. Rc5 Qa6..., A. Giri won (1-0). Bd7, c6, then Be8 marked the exchange turning point, with exchange timing starting to define the evaluation.

With **Kh8**, the tradeoff is concrete: it runs into a decisive sequence after g4. The practical gap to **Bf5** is around 2.4 pawns. With **Kh8**, conversion around **Kh8** can stay smoother around prophylactic defense, but initiative around **Kh8** can swing when **Kh8** hands away a tempo. Set against **Bf5**, **Kh8** plausibly leans on the same long-plan map logic, with a different timing profile. From a medium-horizon view, **Kh8** often diverges once plan commitments become irreversible.

Counterplay exists for both sides. From a key-difference angle, **Bf5** and **Kh8** contrast through momentum balance versus strategic route under short vs medium horizon.
```

