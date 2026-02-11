# Book Commentary Corpus Report

- Corpus: `modules/llm/docs/AlphaZeroEraHumanGrunfeldCorpus.json`
- Results: 10/10 passed

## Quality Summary

- Avg quality score: 89.4 / 100
- Avg lexical diversity: 0.670
- Avg variation-anchor coverage: 0.800
- Opening precedent mentions: 11 across 9/10 cases
- Balanced gate: FAIL (repeated 5-gram [4+ cases]=2, target=0; precedent coverage=9/10, target>=8)
- Low-quality cases (<70): 0
- Advisory findings (non-blocking): 16 across 10 cases

## Cross-Case Repetition

- [3 cases] "this is a mistake that gives the opponent easier play"

### Repeated 5-gram Patterns

- [4 cases] "the engine score trails roughly"
- [4 cases] "engine score trails roughly pawns"

## azg_2018_01_13_kramnik_wei: Kramnik, Vladimir vs Wei, Yi (80th Tata Steel GpA)

- `ply`: 26
- `playedMove`: `g7d4`
- `analysisFen`: `r1bq1rk1/pp2ppbp/6p1/8/3Bn3/5NP1/PP2PPBP/R2Q1RK1 b - - 1 1`
- Metrics: 1151 chars, 6 paragraphs
- Precedent mentions: 0
- Quality: score=80/100, lexical=0.706, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=20, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=23, scoreTokens=3
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
13... Bxd4: On the board right now, handling the positional threat on g7 is the priority. Color complex imbalances are guiding both plans. The middlegame has fully started. Practical control can change after a single move-order slip. Black is just a touch better (≈-0.6). The practical roadmap centers on **Piece Activation**. Because of the pin on e2, coordinating that pawn now costs tempi.

Worth asking: After Bxd4, how should White recapture — Nxd4 or Qxd4?

a) Bxd4 (-0.6)
b) Be6 Bxg7 Qxd1 Rfxd1 (-0.1)
c) Nd6 e3 Re8 Bxg7 (+0.1)

**Bxd4** retains a sound technical setup for the next phase. It applies positional pressure. After **Bxd4**, the game trends toward a controlled strategic struggle.

**Be6** is a candidate move, but it comes with a concession: it concedes a small practical edge after Bxg7. Relative to **Bxd4**, the engine score trails by roughly 0.5 pawns.
**Nd6** is playable in a real game, yet it demands precise sequencing. This continuation stays in the 3rd engine group, while **Bxd4** keeps roughly a 0.6 pawns edge.

Be alert to the positional threat on g7. The game remains balanced, and precision will decide the result.
```

## azg_2018_01_16_kramnik_svidler: Kramnik, Vladimir vs Svidler, Peter (80th Tata Steel GpA)

- `ply`: 36
- `playedMove`: `c8c7`
- `analysisFen`: `r1r1n1k1/pp1Rppbp/6p1/4P3/8/1PN1B1P1/1P2P1KP/R7 b - - 2 1`
- Metrics: 1363 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.660, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=31, scoreTokens=3
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

**Rc7** keeps conversion tasks straightforward in practice. It poses serious practical questions. With **Rc7**, planning depth tends to matter more than short tactics. In the Rc7 branch, a reference game shows: In V. Kramnik-P. Svidler (2018), after 18... Rc7 19. Rxa7 Rb8 20. Rd5 b6..., V. Kramnik won (1-0). The turning point came after Rc7, Rxa7, then Rb8, when exchange timing started to define the evaluation.

From a practical angle, **Bxe5** is viable, yet after Rxb7, practical margins become thinner. Compared with **Rc7**, engine evaluation drops by roughly 0.5 pawns.
In practical play, **Kf8** is reasonable but needs accurate follow-up. Engine ranking is clear here: around 3rd for this line, while **Rc7** is ahead by 0.8 pawns.

The opponent may threaten positional on a7. The defensive burden is noticeable.
```

## azg_2018_03_26_mamedyarov_grischuk: Mamedyarov, Shakhriyar vs Grischuk, Alexander (FIDE Candidates 2018)

- `ply`: 72
- `playedMove`: `c3c2`
- `analysisFen`: `8/4pp1k/2B1P1p1/1Q5p/7P/q1p3P1/5PK1/8 b - - 0 1`
- Metrics: 1219 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.669, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=25, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=27, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.67
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

**c2** retains a sound technical setup for the next phase. It advances the trumping pawn. With **c2**, progress is mostly about methodical coordination. For this c2 structure, a relevant game was: In S. Mamedyarov-A. Grischuk (2018), after 36... c2 37. exf7 Kg7 38. Be4 c1=Q..., S. Mamedyarov won (1-0). The key turning point was c2, exf7, then Kg7, where structural shifts fixed the strategic roadmap.

**f5** is a candidate move, but it comes with a concession: after Qc4, practical margins become thinner. The engine still points to **c2** as cleaner, by about 0.5 pawns.
**f6** is viable over the board, though move-order precision matters. In engine terms this continuation drops to about 3rd, with **c2** up by 1.0 pawns.

Black has to defend accurately to stay afloat.
```

## azg_2018_06_01_aronian_mamedyarov: Aronian, Levon vs Mamedyarov, Shakhriyar (6th Norway Chess 2018)

- `ply`: 24
- `playedMove`: `e7e5`
- `analysisFen`: `r1bq1rk1/p3ppbp/1p4p1/n1p5/3PP3/2PBB3/P3NPPP/2RQ1RK1 b - - 1 1`
- Metrics: 1856 chars, 5 paragraphs
- Precedent mentions: 3
- Quality: score=74/100, lexical=0.601, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=34, dup=0, triRepeat=5, fourRepeat=4, maxNgramRepeat=4, boilerplate=0, mateToneConflict=0, moveTokens=42, scoreTokens=3
- Quality findings:
  - High repeated trigram patterns: 5
  - High repeated four-gram patterns: 4
- Advisory findings:
  - Advisory: quality score below target (90): 74
  - Advisory: repeated trigram templates present (5)
  - Advisory: repeated four-gram templates present (4)
  - Advisory: lexical diversity soft-low: 0.60
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2018.06.01 D87 1-0 | selected loser-side critical move (rank=5, cpLoss=94)
```

**Generated commentary**

```text
12... e5: Because of the pin on d4, coordinating that pawn now costs tempi. A bad bishop problem is limiting piece quality. The game remains in opening channels. Both sides are balancing development and structure. Black is just a touch better (≈-0.4). Strategic focus: **Piece Activation**. Clarify the pawn tension first; then the e-pawn break can be played with more effect.

a) e5 (-0.4)
b) Qd7 f3 Rd8 Qe1 (+0.1)
c) e6 dxc5 Bb7 f4 (+0.3)

**e5** preserves coordination and keeps the best practical structure. It improves piece routes for later plans. The line after **e5** is relatively clean and technical, with less tactical turbulence. Comparable master branches from this split: A) In Magnus Carlsen-Vassily Ivanchuk (2007), after 12... cxd4 13. cxd4 e6 14. Qd2 Bb7..., Magnus Carlsen won (1-0). Sequence focus: cxd4 -> cxd4 -> e6. Strategically, the game turned on pawn-structure transformation that redirected long-term plans. B) In Artur Jussupow-Yue Wang (2008), after 12... Be6 13. d5 Bd7 14. f4 e5..., Yue Wang won (0-1). Sequence focus: Be6 -> d5 -> Bd7. The practical turning factor was pawn-structure transformation that redirected long-term plans. C) In Veselin Topalov-Peter Svidler (2006), after 12... e5 13. dxc5 Be6 14. c4 bxc5..., Veselin Topalov won (1-0). Sequence focus: e5 -> dxc5 -> Be6. Strategic shift: pawn-structure transformation that redirected long-term plans. These precedent lines point to one key driver: pawn-structure transformation that redirected long-term plans.

**Qd7** stays in range, though it concedes a small practical edge after f3. The engine still points to **e5** as cleaner, by about 0.5 pawns.
In practical play, **e6** is reasonable but needs accurate follow-up. Engine ranking places this around 3rd, with **e5** ahead by about 0.7 pawns.

The practical chances are still shared between both players.
```

## azg_2018_06_01_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (6th Norway Chess 2018)

- `ply`: 72
- `playedMove`: `a2c4`
- `analysisFen`: `5rk1/6bp/p4p2/2p5/2Pq1N1Q/6RP/b5pK/4R3 b - - 4 1`
- Metrics: 1307 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=80/100, lexical=0.693, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=25, dup=0, triRepeat=0, fourRepeat=2, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=28, scoreTokens=3
- Quality findings:
  - High repeated four-gram patterns: 2
- Advisory findings:
  - Advisory: quality score below target (90): 80
  - Advisory: repeated four-gram templates present (2)
  - Advisory: lexical diversity soft-low: 0.69
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

**Bxc4** retains a sound technical setup for the next phase. It maintains the tension. **Bxc4** often leads to a stable structure with clear plans. One model continuation with Bxc4 was: In Sergey Karjakin-M. Vachier Lagrave (2018), after 36... Bxc4 37. Qh6 f5 38. Nh5, Sergey Karjakin won (1-0). The turning point came from Bxc4, Qh6, then f5, when initiative control shifted to one side.

**f5** can work, although it concedes a small practical edge after Re7. Relative to **Bxc4**, the engine score trails by roughly 0.5 pawns.
**g1=B+** is playable in a real game, yet it demands precise sequencing. Around **Bxc4**, the principal engine route stays cleaner; this 3rd option is behind by about 2.4 pawns.

Be alert to the material threat on g7. Black has to defend accurately to stay afloat.
```

## azg_2019_05_17_wojtaszek_mamedyarov: Wojtaszek, Radoslaw vs Mamedyarov, Shakhriyar (Moscow FIDE Grand Prix)

- `ply`: 24
- `playedMove`: `f6e4`
- `analysisFen`: `r1b2rk1/pp1nppbp/5np1/qN6/2B2B2/1Q2P3/PP2NPPP/2R2RK1 b - - 5 1`
- Metrics: 1408 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.692, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=28, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=32, scoreTokens=3
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

**Ne4** ?? is a blunder; it allows a forcing sequence against your position. **Ne8** was required. This sits below the principal engine candidates; **Ne8** gives a more reliable setup. Issue: it allows a fork by the knight on d2 against rook on f1 and queen on b3. Consequence: king safety and coordination collapse at once. Better is **Ne8**; it repositions the piece. For this Ne4 structure, a relevant game was: In R. Wojtaszek-S. Mamedyarov (2019), after 12... Ne4 13. Bc7 b6 14. Bxf7+ Rxf7..., R. Wojtaszek won (1-0). Ne4, Bc7, then b6 became the turning point once exchanges reshaped piece activity and defensive resources. This is a mistake that gives the opponent easier play.

From a practical angle, **e5** is viable, yet the line becomes losing after Bg5. Compared with **Ne8**, engine evaluation drops by roughly 3.1 pawns.

Black can follow the more straightforward practical plan.
```

## azg_2019_06_06_carlsen_grischuk: Carlsen, Magnus vs Grischuk, Alexander (7th Norway Chess 2019)

- `ply`: 38
- `playedMove`: `b6a4`
- `analysisFen`: `r2r2k1/p4pbp/1n2p1p1/1p1P3P/2p1PP2/q1P1BB2/P2Q2P1/2R2RK1 b - - 0 1`
- Metrics: 1207 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.686, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=25, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=1, boilerplate=0, mateToneConflict=0, moveTokens=27, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.69
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

**Na4** keeps to the strongest continuation. It establishes a strong outpost. After **Na4**, the game trends toward a controlled strategic struggle. For this Na4 structure, a relevant game was: In M. Carlsen-A. Grischuk (2019), after 19... Na4 20. hxg6 hxg6 21. f5 exf5..., M. Carlsen won (1-0). From Na4, hxg6, then hxg6, the decisive shift was structural transformation and piece rerouting.

**Rd7** stays in range, though the opponent finds Qf2 easier to handle in practice. **Na4** keeps the better engine score by roughly 0.5 pawns.
**Rac8** remains practical, but one inaccurate follow-up can change the assessment. Engine ranking places this around 3rd, with **Na4** ahead by about 0.6 pawns.

A single tempo can swing the position.
```

## azg_2019_06_12_ding_mamedyarov: Ding, Liren vs Mamedyarov, Shakhriyar (7th Norway Chess 2019)

- `ply`: 40
- `playedMove`: `c8c5`
- `analysisFen`: `2r1q1k1/pp1b1rbp/1n1P2p1/6B1/4P1n1/2N2N2/PP4P1/1K1RQB1R b - - 6 1`
- Metrics: 1265 chars, 5 paragraphs
- Precedent mentions: 1
- Quality: score=90/100, lexical=0.677, uniqueSent=1.000, anchorCoverage=0.667
- Quality details: sentences=26, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=28, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.68
- Outline: 6 beats (MoveHeader, Context, Evidence, MainMove, Alternatives, WrapUp)
- Evidence: purpose=engine_alternatives, branches=3

**Reference excerpt (for human comparison)**

```text
2019.06.12 D70 1-0 | selected loser-side critical move (rank=4, cpLoss=219)
```

**Generated commentary**

```text
20... Rc5: Keep g5 in mind as an outpost square. File control along the open line can dictate the middlegame. The opening has turned tactical quickly. There is little room for slow setup moves. Black is a bit better. Minority attack play on the queenside is becoming the most practical plan. Strategic focus: **Blockade on d7**. The structure often turns on the h-pawn break, a lever to keep in mind.

a) Rc5 (-2.8)
b) h6 Rc1 hxg5 Nxg5 (-2.3)
c) Nc4 Bxc4 Rxc4 Bd2 (-0.7)

**Rc5** fits the position's strategic demands cleanly. It prepares for the next phase. **Rc5** often leads to a stable structure with clear plans. One model continuation with Rxc3 was: In S. Geirnaert-F. Handke (2023), after 20... Rxc3 21. bxc3 Qc8 22. e5 Na4..., S. Geirnaert won (1-0). The turning point came after Rxc3, bxc3, then Qc8, when structural features began to dominate planning.

**h6** is a candidate move, but it comes with a concession: the opponent finds Rc1 easier to handle in practice. Relative to **Rc5**, the engine score trails by roughly 0.5 pawns.
**Nc4** can be handled in a game, provided the next moves are exact. In engine terms this continuation drops to about 3rd, with **Rc5** up by 2.2 pawns.

Black's strategic plan is easier to execute without tactical risk.
```

## azg_2019_08_25_karjakin_vachier_lagrave: Karjakin, Sergey vs Vachier-Lagrave, Maxime (7th Sinquefield Cup 2019)

- `ply`: 66
- `playedMove`: `g7h6`
- `analysisFen`: `8/3Q2kp/6p1/2p3q1/P1B5/4b1P1/7P/5K2 b - - 1 1`
- Metrics: 1382 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.675, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=33, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.68
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

**Kh6** ? is a clear mistake; it gives the opponent the initiative. Stronger is **Qe7**. The top engine continuation still starts with **Qe7**. Issue: significant disadvantage after Qf7. Consequence: practical control shifts and defense becomes uncomfortable. Better is **Qe7**; it improves the piece's scope. Precedent around Kh6: In Sergey Karjakin-M. Vachier Lagrave (2019), after 33... Kh6 34. Qh3+ Kg7 35. Qe6 Bd4..., Sergey Karjakin won (1-0). From Kh6, Qh3+, then Kg7, the decisive shift was initiative management rather than static factors. This is a mistake that gives the opponent easier play. Defending against a phantom threat, wasting a valuable tempo.

**Kf6** is serviceable over the board, but it allows a forcing collapse after Qe6+. In engine terms this continuation drops to about 3rd, with **Qe7** up by 8.3 pawns.

Precise defensive choices are needed to keep equality.
```

## azg_2019_08_28_giri_nepomniachtchi: Giri, Anish vs Nepomniachtchi, Ian (7th Sinquefield Cup 2019)

- `ply`: 42
- `playedMove`: `e6d7`
- `analysisFen`: `r1r3k1/p3ppbp/4b1p1/q1Pp4/3N1B2/2R1P2P/P1Q2PP1/5RK1 b - - 2 1`
- Metrics: 1374 chars, 6 paragraphs
- Precedent mentions: 1
- Quality: score=100/100, lexical=0.643, uniqueSent=1.000, anchorCoverage=1.000
- Quality details: sentences=27, dup=0, triRepeat=0, fourRepeat=0, maxNgramRepeat=2, boilerplate=0, mateToneConflict=0, moveTokens=34, scoreTokens=3
- Advisory findings:
  - Advisory: lexical diversity soft-low: 0.64
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

**Bd7** ?? is a clear mistake; it gives the opponent the initiative. Stronger is **Bf5**. This sits below the principal engine candidates; **Bf5** gives a more reliable setup. Issue: it allows a skewer: queen on b6 can hit knight on d4 and then pawn on e3. Consequence: practical control shifts and defense becomes uncomfortable. Better is **Bf5**; it repositions the piece. In the Bd7 branch, a reference game shows: In A. Giri-I. Nepomniachtchi (2019), after 21... Bd7 22. c6 Be8 23. Rc5 Qa6..., A. Giri won (1-0). The critical turning point was Bd7, c6, then Be8, where exchange decisions fixed the practical balance. This is a mistake that gives the opponent easier play.

**Kh8** is serviceable over the board, but the line becomes losing after g4. Relative to **Bf5**, the engine score trails by roughly 2.4 pawns.

The practical chances are still shared between both players.
```

