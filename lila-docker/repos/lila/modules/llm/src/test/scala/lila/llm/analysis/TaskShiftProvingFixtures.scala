package lila.llm.analysis

private[llm] object TaskShiftProvingFixtures:

  final case class TransitionCandidate(
      id: String,
      source: String,
      fen: String,
      expectedTags: List[String],
      note: String,
      depth: Int = 20,
      multiPv: Int = 5
  )

  final case class ReviewFixture(
      id: String,
      label: String,
      fen: String,
      phase: String,
      ply: Int,
      scoreCp: Int,
      pvMoves: List[String],
      expectedTags: List[String],
      note: String
  )

  val transitionCandidates =
    List(
      TransitionCandidate(
        id = "k09a_trade_down_continuation_1",
        source = "StrategicIdeaFenFixtures.K09A",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13",
        expectedTags = List("preparatory_only"),
        note = "Setup move only; the owned handoff still belongs to the later Nxe6 branch."
      ),
      TransitionCandidate(
        id = "k09b_trade_down_continuation_2",
        source = "StrategicIdeaFenFixtures.K09B",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        expectedTags = List("positive_control"),
        note = "Lone exact-board positive control for SecureFavorableSimplification -> PressureFixedWeakComplex."
      ),
      TransitionCandidate(
        id = "k09f_trade_down_continuation_4",
        source = "StrategicIdeaFenFixtures.K09F",
        fen = "2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14",
        expectedTags = List("holdable_simplification"),
        note = "The simplification lands, but the branch stays too broad to isolate the shifted task."
      ),
      TransitionCandidate(
        id = "k09d_trade_down_continuation_8",
        source = "StrategicIdeaFenFixtures.K09D",
        fen = "1r1q1rk1/pp3ppp/2n2n2/3p4/3P2b1/2N2N2/PP2BPPP/2RQ1RK1 w - - 3 13",
        expectedTags = List("non_simplification_root_best"),
        note = "Root-best is prophylactic coordination rather than simplification ownership."
      ),
      TransitionCandidate(
        id = "k09e_trade_down_continuation_9",
        source = "StrategicIdeaFenFixtures.K09E",
        fen = "r1bq1rk1/pp3ppp/5n2/3p4/1PnP4/2N2N2/P3BPPP/R2Q1RK1 w - - 1 13",
        expectedTags = List("non_simplification_root_best", "heavy_piece_release_survives"),
        note = "Route and release pressure survive the branch, so the task never narrows into B7."
      ),
      TransitionCandidate(
        id = "k03a_carlsbad_fixed_targets_continuation",
        source = "StrategicIdeaFenFixtures.K03A",
        fen = "r1bqrnk1/4bppp/2p2n2/pp1p2B1/3P4/P1NBP3/1PQ1NPPP/3R1RK1 b - - 1 13",
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "Carlsbad target pressure persists without a simplification-led handoff."
      ),
      TransitionCandidate(
        id = "b15a_carlsbad_fixed_chain_pressure",
        source = "StrategicIdeaFenFixtures.B15A",
        fen = "r1bqr1k1/pp2bpp1/2p1nn1p/3p4/3P3B/2NBPP2/PPQ1N1PP/3R1RK1 w - - 0 13",
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "Fixed-chain pressure stays the task; no bounded shift appears after the best move."
      ),
      TransitionCandidate(
        id = "b16b_carlsbad_pressure_resists_trade",
        source = "StrategicIdeaFenFixtures.B16B",
        fen = "r1b1rnk1/pp2qppp/2p2n2/3p4/3P4/3BPP2/PPQ1N1PP/R2N1RK1 w - - 2 14",
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "Line management and fixed-target pressure resist any B7-style handoff."
      ),
      TransitionCandidate(
        id = "mi2_shell_minus_queen",
        source = "PlanPriorityFenFixtureTest.MI2",
        fen = "r1b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        expectedTags = List("non_simplification_root_best"),
        note = "The branch converts generic edge and cash-in pressure rather than certifying a task shift."
      ),
      TransitionCandidate(
        id = "mi3_shell_minus_queen_and_rook",
        source = "PlanPriorityFenFixtureTest.MI3",
        fen = "2b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        expectedTags = List("non_simplification_root_best"),
        note = "High-edge conversion dominates the line, so B7 would be a relabel drift."
      ),
      TransitionCandidate(
        id = "b21_modern_benoni_target_fixing",
        source = "StrategicIdeaFenFixtures.B21",
        fen = "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9",
        expectedTags = List("target_fixation_without_handoff"),
        note = "The row is a clean target-fixation control with no simplification-led handoff."
      ),
      TransitionCandidate(
        id = "b21a_modern_benoni_target_fixing_followup",
        source = "StrategicIdeaFenFixtures.B21A",
        fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
        expectedTags = List("target_fixation_without_handoff"),
        note = "The follow-up row stays target-led and never acquires an owned pressure-shift."
      ),
      TransitionCandidate(
        id = "b6_route_chain_near_miss",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        expectedTags = List("route_restatement_only"),
        note = "Fail-close row for B6: route denial remains the truth after exact replay."
      ),
      TransitionCandidate(
        id = "b5_queen_infiltration_shell",
        source = "HeavyPieceLocalBindNegativeValidationTest",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24",
        expectedTags = List("heavy_piece_release_survives"),
        note = "Heavy-piece release and infiltration remain primary even when the line replays cleanly."
      ),
      TransitionCandidate(
        id = "b5_rook_lift_switch",
        source = "HeavyPieceLocalBindNegativeValidationTest",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24",
        expectedTags = List("heavy_piece_release_survives"),
        note = "Rook-lift and queen-infiltration release features outlive any B7 wording."
      ),
      TransitionCandidate(
        id = "k19_target_led_endgame",
        source = "StrategicIdeaFenFixtures.K19",
        fen = "8/1k1r4/4p3/1P1pP2p/R2P3P/3K2P1/8/8 w - - 1 45",
        expectedTags = List("endgame_inflation"),
        note = "Exact branch may persist, but the whole row is already pure endgame inflation."
      ),
      TransitionCandidate(
        id = "lucena_blocker",
        source = "endgame_goldset_v2_patterns.jsonl",
        fen = "2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1",
        expectedTags = List("endgame_inflation"),
        note = "Lucena theorem blocker: exact replay is strong precisely because it must stay out of B7."
      ),
      TransitionCandidate(
        id = "philidor_blocker",
        source = "endgame_goldset_v2_patterns.jsonl",
        fen = "8/4k3/r7/4PK2/8/8/8/R7 b - - 0 1",
        expectedTags = List("endgame_inflation"),
        note = "Philidor theorem blocker; exactness here would only inflate B7 beyond its slice."
      ),
      TransitionCandidate(
        id = "vancura_blocker",
        source = "endgame_goldset_v2_patterns.jsonl",
        fen = "6k1/7R/PKr5/8/8/8/8/8 w - - 0 1",
        expectedTags = List("endgame_inflation"),
        note = "Vancura theorem blocker; exact replay does not rescue the family mismatch."
      )
    )

  val reviewFixtures =
    List(
      ReviewFixture(
        id = "K09A",
        label = "preparatory trade-down window",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13",
        phase = "middlegame",
        ply = 26,
        scoreCp = 92,
        pvMoves = List("d1b3", "d8d7", "f1d1", "a8c8", "d4e6", "f7e6", "e3f4", "e7b4", "c3e4"),
        expectedTags = List("preparatory_only"),
        note = "Setup move only; the B7 handoff still belongs to the later simplifying branch."
      ),
      ReviewFixture(
        id = "K09B",
        label = "candidate trade-down continuation",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        phase = "middlegame",
        ply = 26,
        scoreCp = 60,
        pvMoves = List("d4e6", "f7e6", "a1d1", "g8h8", "e3f4", "d8b6", "a2a3", "a8c8", "e2e4", "c6d4", "c2d2", "d4b3"),
        expectedTags = List("positive_control"),
        note = "Lone positive control; exact branch still fails runtime ownership."
      ),
      ReviewFixture(
        id = "K09F",
        label = "holdable simplification shell",
        fen = "2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14",
        phase = "middlegame",
        ply = 28,
        scoreCp = 40,
        pvMoves = List("d4e6", "f7e6", "g2h3", "d8d7", "c1c2", "e7f8", "c3b5", "a7a6", "b5d4"),
        expectedTags = List("holdable_simplification"),
        note = "The recapture is exact, but the post-trade task remains too broad."
      ),
      ReviewFixture(
        id = "K09D",
        label = "prophylactic coordination blocker",
        fen = "1r1q1rk1/pp3ppp/2n2n2/3p4/3P2b1/2N2N2/PP2BPPP/2RQ1RK1 w - - 3 13",
        phase = "middlegame",
        ply = 26,
        scoreCp = 15,
        pvMoves = List("h2h3", "g4f3", "e2f3", "f8e8", "d1d2", "d8d7", "f1d1", "h7h6", "a2a3", "c6e7", "d1e1", "b7b5", "b2b4", "a7a6", "e1e5", "e7g6", "e5e8", "b8e8", "f3e2"),
        expectedTags = List("non_simplification_root_best"),
        note = "Root-best keeps the branch in coordination and prophylaxis rather than simplification ownership."
      ),
      ReviewFixture(
        id = "K09E",
        label = "file-pressure and release blocker",
        fen = "r1bq1rk1/pp3ppp/5n2/3p4/1PnP4/2N2N2/P3BPPP/R2Q1RK1 w - - 1 13",
        phase = "middlegame",
        ply = 26,
        scoreCp = 8,
        pvMoves = List("a1c1", "c4d6", "f3e5", "c8f5", "h2h3", "a8c8", "d1d2", "f5e6", "e5d3", "c8c4", "d3c5", "c4b4", "c5e6", "f7e6", "d2e3", "d8c8"),
        expectedTags = List("non_simplification_root_best", "heavy_piece_release_survives"),
        note = "The best branch stays anchored to file pressure and release features."
      ),
      ReviewFixture(
        id = "K03A",
        label = "carlsbad fixed-target control",
        fen = "r1bqrnk1/4bppp/2p2n2/pp1p2B1/3P4/P1NBP3/1PQ1NPPP/3R1RK1 b - - 1 13",
        phase = "middlegame",
        ply = 25,
        scoreCp = 17,
        pvMoves = List("g7g6", "e2f4", "f8e6", "f4e6", "c8e6", "c3e2", "a8c8", "h2h3", "f6d7", "g5e7", "d8e7", "a3a4", "b5b4", "e2f4", "c6c5", "d4c5", "d7c5", "d3b5", "e8d8", "d1d4", "e7d6", "c2d1", "d6e5"),
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "Fixed-target Carlsbad pressure stays the family truth."
      ),
      ReviewFixture(
        id = "B15A",
        label = "carlsbad fixed-chain pressure control",
        fen = "r1bqr1k1/pp2bpp1/2p1nn1p/3p4/3P3B/2NBPP2/PPQ1N1PP/3R1RK1 w - - 0 13",
        phase = "middlegame",
        ply = 25,
        scoreCp = 71,
        pvMoves = List("h4f2", "b7b5", "e3e4", "d5e4", "f3e4", "f6g4", "e4e5", "e7h4", "g2g3", "h4e7", "d3h7", "g8h8", "h7e4", "b5b4", "e4c6", "c8d7", "c6d7", "d8d7", "c3e4", "a8c8", "c2d3", "f7f5", "e4d2", "g4e5", "d3f5", "e6g5", "f5d7", "e5d7", "d1c1", "e7d6", "c1c8", "e8c8", "f1c1"),
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "The branch stays in fixed-chain pressure and queen-side release."
      ),
      ReviewFixture(
        id = "B16B",
        label = "trade-resistance pressure control",
        fen = "r1b1rnk1/pp2qppp/2p2n2/3p4/3P4/3BPP2/PPQ1N1PP/R2N1RK1 w - - 2 14",
        phase = "middlegame",
        ply = 27,
        scoreCp = 15,
        pvMoves = List("f1e1", "c8d7", "c2d2", "b7b6", "e2c3", "c6c5", "d3f1", "d7c6", "d1f2", "a8d8", "b2b3", "h7h5", "a1c1", "f8g6", "c3e2", "c5c4", "e2f4", "g6f4", "e3f4"),
        expectedTags = List("non_simplification_root_best", "target_fixation_without_handoff"),
        note = "Trade resistance still serves fixed-target pressure, not a new B7 task."
      ),
      ReviewFixture(
        id = "MI2",
        label = "non-best-branch trade illusion",
        fen = "r1b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        phase = "middlegame",
        ply = 24,
        scoreCp = 1366,
        pvMoves = List("g1h1", "e7c5", "d4c6", "c5e3", "c6e7", "e8e7", "d1b3", "e3f2"),
        expectedTags = List("non_simplification_root_best"),
        note = "Trade ideas exist only off the root best branch, so the row fail-closes."
      ),
      ReviewFixture(
        id = "MI3",
        label = "high-edge cash-in shell",
        fen = "2b1r1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12",
        phase = "middlegame",
        ply = 24,
        scoreCp = 1819,
        pvMoves = List("d4b5", "c8e6", "b5c7", "e8d8", "d1b3", "d5d4", "c7e6", "f7e6", "b3b7"),
        expectedTags = List("non_simplification_root_best"),
        note = "Cash-in conversion dominates the branch and would relabel B1 into B7."
      ),
      ReviewFixture(
        id = "B21",
        label = "target-fixation control",
        fen = "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9",
        phase = "middlegame",
        ply = 24,
        scoreCp = 64,
        pvMoves = List("f3d2", "b8a6", "e1g1", "f8e8", "f2f3", "a6c7", "a2a4"),
        expectedTags = List("target_fixation_without_handoff"),
        note = "Target fixation is the whole task; no simplification handoff appears."
      ),
      ReviewFixture(
        id = "B21A",
        label = "target-fixation follow-up",
        fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
        phase = "middlegame",
        ply = 24,
        scoreCp = 119,
        pvMoves = List("f3d2", "b8a6", "d2c4", "a6c7", "a2a4", "b7b6", "e2f1"),
        expectedTags = List("target_fixation_without_handoff"),
        note = "The follow-up persists in the same target-fixation family."
      )
    )
