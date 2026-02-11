# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/AlphaZeroEraHumanGrunfeldCorpus.json`
- Results: 10/10 passed

## Quality Summary

- Avg quality score: 90.0 / 100
- Avg lexical diversity: 0.669
- Avg variation-anchor coverage: 0.800
- Opening precedent mentions: 11 across 9/10 cases
- Balanced gate: PASS (repeated 5-gram [4+ cases]=0, target=0; precedent coverage=9/10, target>=8)
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 16 across 10 cases

## Cross-Case Repetition

- No sentence repeated across 3+ cases.

- No 5-gram pattern repeated across 4+ cases.

## azg_2018_01_13_kramnik_wei: Kramnik, Vladimir vs Wei, Yi (80th Tata Steel GpA)

- `ply`: 26
- `playedMove`: `g7d4`
- `analysisFen`: `r1bq1rk1/pp2ppbp/6p1/8/3Bn3/5NP1/PP2PPBP/R2Q1RK1 b - - 1 1`
- Metrics: 1329 chars, 6 paragraphs
- Precedent mentions: 0
- Quality: score=80/100, lexical=0.703, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=22, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=25, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 2
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated four-gram templates present (2)
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.01.13 D77 1-0 | selected loser-side critical move (rank=5, cpLoss=65)
```

**Generated commentary**

```text
13... Bxd4: The immediate concrete issue is the positional threat on g7. Color complex control is the key strategic battleground. Plans and tactics now bite at every move. Automatic play is dangerous in this position. Black is just a touch better. The practical roadmap centers on **Piece Activation**. The pin restrains the pawn on e2, reducing practical flexibility.

Worth asking: After Bxd4, how should White recapture — Nxd4 or Qxd4?

a) Bxd4 (-0.6)
b) Be6 Bxg7 Qxd1 Rfxd1 (-0.1)
c) Nd6 e3 Re8 Bxg7 (+0.1)

**Bxd4** follows the principal engine roadmap, while preserving structural clarity. It applies positional pressure. With **Bxd4**, planning depth tends to matter more than short tactics.

**Be6** is playable, but it yields a modest practical concession once Bxg7 appears. Compared with **Bxd4**, engine evaluation drops by roughly 0.5 pawns. The strategic test after **Be6** is precise conversion timing, because disconnected coordination quickly undermines the plan.
**Nd6** is playable in practice, but concrete calculation is required. As a 3rd practical-tier choice, this trails **Bxd4** by about 0.6 pawns. After **Nd6**, king safety and tempo are linked, so one inexact sequence can hand over initiative.

Be alert to the positional threat on g7. The game remains balanced, and precision will decide the result.
```

## azg_2018_01_16_kramnik_svidler: Kramnik, Vladimir vs Svidler, Peter (80th Tata Steel GpA)

- `ply`: 36
- `playedMove`: `c8c7`
- `analysisFen`: `r1r1n1k1/pp1Rppbp/6p1/4P3/8/1PN1B1P1/1P2P1KP/R7 b - - 2 1`
- Metrics: 1689 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.664, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=29, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=34, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.66
- Outline: 7 beats (MoveHeader, Context, DecisionPoint, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.01.16 D78 1-0 | selected loser-side critical move (rank=5, cpLoss=87)
```

**Generated commentary**

```text
18... Rc7: On the board right now, handling the positional threat on a7 is the priority. The queenside minority attack is becoming a concrete lever. The position now demands active middlegame decision-making. Move-order nuance can shift practical control. Black has a small pull. Key theme: **Queenside Attack**. The pawn on b7 is hanging (1 attacker, no defenders).

A key question: What is the defensive task here — can Black meet the threat with Bxe5 (watch c3)?

a) Rc7 (-0.4)
b) Bxe5 Rxb7 Bxc3 bxc3 (+0.1)
c) Kf8 Rxb7 Rc7 Rxc7 (+0.4)

**Rc7** preserves coordination and keeps the best practical structure, which eases conversion. It poses serious practical questions. With **Rc7**, planning depth tends to matter more than short tactics. A model game in the Rc7 line runs: In V. Kramnik-P. Svidler (2018), after 18... Rc7 19. Rxa7 Rb8 20. Rd5 b6..., V. Kramnik won (1-0). The turning point came after Rc7, Rxa7, then Rb8, when exchange timing started to define the evaluation.

**Bxe5** is a candidate move, but it comes with a concession: after Rxb7, practical margins become thinner for your side. In engine terms, **Rc7** holds roughly a 0.5 pawns edge. Strategically, after **Bxe5**, coordination must stay tight around piece centralization, so exchanges do not drift out of control.
**Kf8** can be handled in a game, provided the next moves are exact. This 3rd line leaves a large practical deficit versus **Rc7** (about 0.8 pawns). With **Kf8**, a tempo-order slip around **Kf8** can expose conversion-side coordination gaps around queenside attack preparation, because recovery windows are short.

The opponent may threaten positional on a7. The defensive burden is noticeable.
```

## azg_2018_03_26_mamedyarov_grischuk: Mamedyarov, Shakhriyar vs Grischuk, Alexander (FIDE Candidates 2018)

- `ply`: 72
- `playedMove`: `c3c2`
- `analysisFen`: `8/4pp1k/2B1P1p1/1Q5p/7P/q1p3P1/5PK1/8 b - - 0 1`
- Metrics: 1473 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.662, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=29, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.66
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.03.26 D77 1-0 | selected loser-side critical move (rank=5, cpLoss=471)
```

**Generated commentary**

```text
36... c2: Keep an eye on the pawn on f7 — 1 attacker, no defenders. The open file is the key channel for major-piece activity. This is a technical endgame phase. Precision matters more than ambition here. White has the more pleasant position (≈+1.9). Key theme: **postponing pawn commitments until pieces are ready**. Releasing the tension can make the e-pawn break more potent.

a) c2 (+1.9)
b) f5 Qc4 Qa5 Ba4 (+2.4)
c) f6 Qc4 Kg7 Qd4 (+2.9)

**c2** keeps the game strategically tidy and easier to handle, even if the position stays tense. It advances the trumping pawn. With **c2**, progress is mostly about methodical coordination. In S. Mamedyarov-A. Grischuk (2018), after 36... c2 37. exf7 Kg7 38. Be4 c1=Q..., S. Mamedyarov won (1-0). This line is anchored by c2. The key turning point was c2, exf7, then Kg7, where structural shifts fixed the strategic roadmap.

From a practical angle, **f5** is viable, yet after Qc4, practical margins become thinner for your side. The engine still points to **c2** as cleaner, by about 0.5 pawns. The strategic test after **f5** is precise conversion timing, because disconnected coordination quickly undermines the plan.
**f6** is playable in practice, but concrete calculation is required. This 3rd line leaves a large practical deficit versus **c2** (about 1.0 pawns). In practical terms, **f6** is judged by conversion ease, because defensive coordination can diverge quickly.

Black has to defend accurately to stay afloat.
```

## azg_2018_06_01_aronian_mamedyarov: Aronian, Levon vs Mamedyarov, Shakhriyar (6th Norway Chess 2018)

- `ply`: 24
- `playedMove`: `e7e5`
- `analysisFen`: `r1bq1rk1/p3ppbp/1p4p1/n1p5/3PP3/2PBB3/P3NPPP/2RQ1RK1 b - - 1 1`
- Metrics: 1974 chars, 5 paragraphs
- Precedent mentions: 3
- Quality: score=80/100, lexical=0.636, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=33, dup=0, triRepeat=4, fourRepeat=3, maxNgramRepeat=3, boilerplate=0, mateToneConflict=0, moveTokens=38, scoreTokens=3
- Quality findings:
  - High repeated trigram patterns: 4
  - High repeated four-gram patterns: 3
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated trigram templates present (4)
  - Advisory: repeated four-gram templates present (3)
  - Advisory: lexical diversity soft-low: 0.64
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.06.01 D87 1-0 | selected loser-side critical move (rank=5, cpLoss=94)
```

**Generated commentary**

```text
12... e5: The pin restrains the pawn on d4, reducing practical flexibility. The bad bishop's scope is a strategic weakness here. Development goals are clear, but the structure is still flexible. Central commitments are still being timed with care. Black has the easier side to press with. Key theme: **Piece Activation**. Once the structure is clarified, the e-pawn break tends to carry more weight.

a) e5 (-0.4)
b) Qd7 f3 Rd8 Qe1 (+0.1)
c) e6 dxc5 Bb7 f4 (+0.3)

**e5** keeps the technical roadmap compact and stable, and move-order risks stay manageable. It improves piece routes for later plans. **e5** lowers immediate tactical volatility and rewards precise coordination. Comparable master branches from this split: A) In Artur Jussupow-Yue Wang (2008), after 12... Be6 13. d5 Bd7 14. f4 e5..., Yue Wang won (0-1). Line route: Be6 -> d5 -> Bd7. B) In Krishnan Sasikiran-Gata Kamsky (2008), after 12... e5 13. Qa4 Bd7 14. Qa3 Be6..., Gata Kamsky won (0-1). The practical turning factor was pawn-structure transformation that redirected long-term plans. C) In Loek Van Wely-Alexei Shirov (2008), after 12... e5 13. dxc5 Be6 14. c4 bxc5..., Alexei Shirov won (0-1). The decisive practical driver was control of pawn-structure transformation that redirected long-term plans. All cited branches revolve around pawn-structure transformation that redirected long-term plans.

**Qd7** is playable, but it yields a modest practical concession once f3 appears. The engine still points to **e5** as cleaner, by about 0.5 pawns. The strategic test after **Qd7** is precise conversion timing, because disconnected coordination quickly undermines the plan around positional pressure.
In practical play, **e6** is reasonable but needs accurate follow-up. The 3rd choice is workable, but **e5** still leads by roughly 0.7 pawns. Strategically, **e6** needs connected coordination through conversion, so practical control does not leak away.

The position remains dynamically balanced.
```

## azg_2018_06_01_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (6th Norway Chess 2018)

- `ply`: 72
- `playedMove`: `a2c4`
- `analysisFen`: `5rk1/6bp/p4p2/2p5/2Pq1N1Q/6RP/b5pK/4R3 b - - 4 1`
- Metrics: 1614 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=80/100, lexical=0.655, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=30, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 2
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated four-gram templates present (2)
  - Advisory: lexical diversity soft-low: 0.66
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.06.01 D86 1-0 | selected loser-side critical move (rank=5, cpLoss=312)
```

**Generated commentary**

```text
36... Bxc4: On the board right now, handling the material threat on g7 is the priority. Open-board dynamics favor the bishop pair. The position now demands active middlegame decision-making. Small inaccuracies can hand over momentum immediately. White holds a clear advantage. The bad bishop remains a lasting positional burden. Strategic focus: **Exploiting Space Advantage**. Keep an eye on the pawn on g2 — 3 attackers, no defenders.

a) Bxc4 (+3.3)
b) f5 Re7 Bxc4 Rexg7+ (+3.8)
c) g1=B+ Rexg1 Rf7 Qh6 (+5.7)

**Bxc4** keeps to the strongest continuation, so coordination remains intact. It maintains the tension. **Bxc4** often leads to a stable structure with clear plans. Historical guidance around Bxc4 is clear: In Sergey Karjakin-M. Vachier Lagrave (2018), after 36... Bxc4 37. Qh6 f5 38. Nh5, Sergey Karjakin won (1-0). The turning point came from Bxc4, Qh6, then f5, when initiative control shifted to one side.

**f5** stays in range, though it yields a modest practical concession once Re7 appears. Relative to **Bxc4**, the engine score trails by roughly 0.5 pawns. In practical terms, tempo control after **f5** must stay aligned with king safety timing around passed pawn advance, so the next phase remains manageable.
**g1=B+** remains practical, but one inaccurate follow-up can change the assessment. As a lower-tier option (around 3rd), this trails **Bxc4** by roughly 2.4 pawns. After **g1=B+**, sequence accuracy matters, because king safety and structure can swing quickly around sharp attack with check.

Be alert to the material threat on g7. Black has to defend accurately to stay afloat.
```

## azg_2019_05_17_wojtaszek_mamedyarov: Wojtaszek, Radoslaw vs Mamedyarov, Shakhriyar (Moscow FIDE Grand Prix)

- `ply`: 24
- `playedMove`: `f6e4`
- `analysisFen`: `r1b2rk1/pp1nppbp/5np1/qN6/2B2B2/1Q2P3/PP2NPPP/2R2RK1 b - - 5 1`
- Metrics: 1470 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.693, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=34, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.69
- Outline: 7 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.05.17 D82 1-0 | selected loser-side critical move (rank=3, cpLoss=342)
```

**Generated commentary**

```text
12... Ne4: The pin restrains the pawn on f7, reducing practical flexibility. Rook lift geometry is becoming available. Development goals are clear, but the structure is still flexible. Both sides are balancing development and structure. Black has a comfortable plus (≈-3.4). A battery setup is building pressure on key lines. Strategic focus: **keeping central levers available for later timing**.

a) Ne8 Rc2 a6 (-3.4)
b) e5 Bg5 Nc5 Qc3 (-0.2)
c) Ne4 Nc7 Nd2 Qb5 (+0.1)

Missing pin was a significant oversight.

**Ne4** ?? is a blunder that hands over game flow, while **Ne8** was the only stable route. This sits below the principal engine candidates, so **Ne8** gives the more reliable setup. From a practical perspective, Issue: it allows a fork by the knight on d2 against rook on f1 and queen on b3. Therefore, Consequence: king safety and coordination collapse at once. As a result, Better is **Ne8**; it repositions the piece. In R. Wojtaszek-S. Mamedyarov (2019), after 12... Ne4 13. Bc7 b6 14. Bxf7+ Rxf7..., R. Wojtaszek won (1-0). This line is anchored by Ne4. Ne4, Bc7, then b6 became the turning point once exchanges reshaped piece activity and defensive resources.

**e5** can work, although the line becomes losing after Bg5. The engine still points to **Ne8** as cleaner, by about 3.1 pawns. After **e5**, initiative around **e5** depends on precise simplification around prophylactic defense.

Black can follow the more straightforward practical plan.
```

## azg_2019_06_06_carlsen_grischuk: Carlsen, Magnus vs Grischuk, Alexander (7th Norway Chess 2019)

- `ply`: 38
- `playedMove`: `b6a4`
- `analysisFen`: `r2r2k1/p4pbp/1n2p1p1/1p1P3P/2p1PP2/q1P1BB2/P2Q2P1/2R2RK1 b - - 0 1`
- Metrics: 1477 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.673, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=31, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.67
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.06.06 D85 1-0 | selected loser-side critical move (rank=5, cpLoss=174)
```

**Generated commentary**

```text
19... Na4: d5 is pinned, leaving the pawn with limited mobility. The good bishop has unobstructed diagonals and lasting influence. Middlegame priorities now hinge on concrete calculation. Practical control can change after a single move-order slip. White can keep up mild pressure. Strategic priority: **keeping the pawn chain intact**. Keep the pawn tension; the e-pawn break is a useful lever.

a) Na4 (+0.3)
b) Rd7 Qf2 exd5 Bc5 (+0.8)
c) Rac8 Qf2 exd5 hxg6 (+0.9)

**Na4** keeps conversion tasks straightforward in practice, because structure and activity stay connected. It establishes a strong outpost. After **Na4**, the game trends toward a controlled strategic struggle. In M. Carlsen-A. Grischuk (2019), after 19... Na4 20. hxg6 hxg6 21. f5 exf5..., M. Carlsen won (1-0). This line is anchored by Na4. From Na4, hxg6, then hxg6, the decisive shift was structural transformation and piece rerouting.

**Rd7** can work, although the opponent gets a slightly easier practical route after Qf2. In engine terms, **Na4** holds roughly a 0.5 pawns edge. After **Rd7**, initiative around **Rd7** depends on precise simplification around positional pressure.
**Rac8** remains practical, but one inaccurate follow-up can change the assessment. As a 3rd option, this line trails **Na4** by around 0.6 pawns. With **Rac8**, a tempo-order slip around **Rac8** can expose conversion-side coordination gaps, because recovery windows are short.

A single tempo can swing the position.
```

## azg_2019_06_12_ding_mamedyarov: Ding, Liren vs Mamedyarov, Shakhriyar (7th Norway Chess 2019)

- `ply`: 40
- `playedMove`: `c8c5`
- `analysisFen`: `2r1q1k1/pp1b1rbp/1n1P2p1/6B1/4P1n1/2N2N2/PP4P1/1K1RQB1R b - - 6 1`
- Metrics: 1480 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.673, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=25, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=31, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.67
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.06.12 D70 1-0 | selected loser-side critical move (rank=4, cpLoss=219)
```

**Generated commentary**

```text
20... Rc5: Keep g5 in mind as an outpost square. File control along the open line can dictate the middlegame. Opening choices are already forcing concrete decisions. Piece placement and tactics are now intertwined. Black is a bit better. A minority attack is becoming a practical queenside plan. Strategic focus: **Blockade on d7**. The h-pawn break is a thematic lever in this structure.

a) Rc5 (-2.8)
b) h6 Rc1 hxg5 Nxg5 (-2.3)
c) Nc4 Bxc4 Rxc4 Bd2 (-0.7)

**Rc5** preserves coordination and keeps the best practical structure, which eases conversion. It improves piece routes for later plans. **Rc5** often leads to a stable structure with clear plans. A model game in the Rc5 line runs: In Ta Baron-Or Globus (2018), after 20... Rc5 21. Be7 Rxc3 22. bxc3 Qc8..., Ta Baron won (1-0). Rc5, Be7, then Rxc3 marked the turning point because tempo and initiative started to decide the play.

**h6** is playable, but after Rc1, practical margins become thinner for your side. The engine still points to **Rc5** as cleaner, by about 0.5 pawns. After **h6**, initiative around **h6** depends on precise simplification.
In practical play, **Nc4** is reasonable but needs accurate follow-up. This 3rd line leaves a large practical deficit versus **Rc5** (about 2.2 pawns). Strategically, **Nc4** needs connected coordination through conversion around establishing outpost, so practical control does not leak away.

Black can press with a comparatively straightforward conversion scheme.
```

## azg_2019_08_25_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (7th Sinquefield Cup 2019)

- `ply`: 66
- `playedMove`: `g7h6`
- `analysisFen`: `8/3Q2kp/6p1/2p3q1/P1B5/4b1P1/7P/5K2 b - - 1 1`
- Metrics: 1642 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.668, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=35, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.67
- Outline: 8 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, PsychologicalVerdict, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.08.25 D86 1-0 | selected loser-side critical move (rank=2, cpLoss=167)
```

**Generated commentary**

```text
33... Kh6: Watch for a fork by the queen on e7 hitting bishop on e3 and king on g7. The hanging pawns in the center are a major strategic factor. This is a technical endgame phase. One tempo can decide the technical outcome. The position is finely balanced. Opposite-colored bishops often amplify direct attacking chances. Strategic priority: **Perpetual Check**.

a) Qe7 Qxe7+ Kh6 Qxe3+ (+0.0)
b) Kh6 Qf7 Bd4 a5 (+1.7)
c) Kf6 Qe6+ Kg7 Qf7+ (+8.3)

Missing check was a noticeable oversight.

**Kh6** ? is serious enough to shift practical control, and **Qe7** was the preferable route. The top engine continuation starts with **Qe7**, because it keeps the structure more resilient. From a practical perspective, Issue: significant disadvantage after Qf7. Therefore, Consequence: practical control shifts and defense becomes uncomfortable. As a result, Better is **Qe7**; it improves the piece's scope. In the Kh6 branch, a reference game shows: In Sergey Karjakin-M. Vachier Lagrave (2019), after 33... Kh6 34. Qh3+ Kg7 35. Qe6 Bd4..., Sergey Karjakin won (1-0). From Kh6, Qh3+, then Kg7, the decisive shift was initiative management rather than static factors. This mistake yields an easier conversion plan, because your coordination is slower. Defending against a phantom threat, wasting a valuable tempo.

From a practical angle, **Kf6** is viable, yet it allows a forcing collapse after Qe6+. This sits in a lower engine tier (about 3rd), and **Qe7** leads by roughly 8.3 pawns. After **Kf6**, initiative around **Kf6** depends on precise simplification around prophylactic defense.

Precise defensive choices are needed to keep equality.
```

## azg_2019_08_28_giri_nepomniachtchi: Giri, Anish vs Nepomniachtchi, Ian (7th Sinquefield Cup 2019)

- `ply`: 42
- `playedMove`: `e6d7`
- `analysisFen`: `r1r3k1/p3ppbp/4b1p1/q1Pp4/3N1B2/2R1P2P/P1Q2PP1/5RK1 b - - 2 1`
- Metrics: 1527 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.661, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=26, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=35, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.66
- Outline: 7 beats (MoveHeader, Context, Evidence, TeachingPoint, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.08.28 D83 1-0 | selected loser-side critical move (rank=3, cpLoss=253)
```

**Generated commentary**

```text
21... Bd7: The pawn on e5 is pinned to the queen on c7. The bishop pair increases pressure across both wings. The middlegame has fully started. Routine moves can quickly backfire here. Black can play for two results. A bad bishop handicap is still shaping the strategic plans. The practical roadmap centers on **postponing pawn commitments until pieces are ready**.

a) Bf5 Qb2 e5 Ra3 (-1.4)
b) Kh8 g4 Bd7 c6 (+1.0)
c) Bd7 c6 Be8 Rc1 (+1.2)

Missing overloading was a significant oversight.

**Bd7** ?? is a mistake because it loosens tempo order; **Bf5** keeps the position connected. This sits below the principal engine candidates, so **Bf5** gives the more reliable setup. That makes the practical picture clear: Issue: it allows a skewer: queen on b6 can hit knight on d4 and then pawn on e3. So Consequence: practical control shifts and defense becomes uncomfortable. For that reason, Better is **Bf5**; it repositions the piece. A model game in the Bd7 line runs: In A. Giri-I. Nepomniachtchi (2019), after 21... Bd7 22. c6 Be8 23. Rc5 Qa6..., A. Giri won (1-0). The critical turning point was Bd7, c6, then Be8, where exchange decisions fixed the practical balance.

**Kh8** stays in range, though the line becomes losing after g4. Compared with **Bf5**, engine evaluation drops by roughly 2.4 pawns. In practical terms, tempo control after **Kh8** must stay aligned with king safety timing around prophylactic defense, so the next phase remains manageable.

The practical chances are still shared between both players.
```

