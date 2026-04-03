package lila.llm.analysis

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.time.LocalDate

object NamedRouteValidationMatrixWorkup:

  enum FamilyHint:
    case BroaderB6
    case B4Blocker
    case B5Blocker
    case B7Blocker
    case PostureBlocker
    case ExternalLead

  final case class Candidate(
      id: String,
      source: String,
      fen: String,
      trigger: String,
      family: FamilyHint,
      note: String,
      bestDefensePrefix: List[String] = Nil,
      intermediateMove: Option[String] = None,
      downstreamMove: Option[String] = None,
      baselineSameBranch: String = "unknown",
      baselineIntermediateDetour: String = "unknown",
      baselineDownstreamRerouteDenial: String = "unknown",
      baselineContinuationBound: String = "unknown",
      baselineReleaseSuppression: String = "unknown",
      distinctFromB4: String = "yes",
      distinctFromB5: String = "yes",
      distinctFromB7: String = "yes",
      depth: Int = 18,
      multiPv: Int = 5
  )

  final case class MatrixRow(
      candidateId: String,
      source: String,
      exactFen: String,
      trigger: String,
      rootBest: String,
      triggerIsRootBest: String,
      bestDefense: String,
      sameBranch: String,
      intermediateDetour: String,
      downstreamRerouteDenial: String,
      continuationBound: String,
      releaseSuppression: String,
      distinctFromB4: String,
      distinctFromB5: String,
      distinctFromB7: String,
      containmentRisk: String,
      verdict: String,
      failReason: String
  ):
    def values: List[String] =
      List(
        candidateId,
        source,
        exactFen,
        trigger,
        rootBest,
        triggerIsRootBest,
        bestDefense,
        sameBranch,
        intermediateDetour,
        downstreamRerouteDenial,
        continuationBound,
        releaseSuppression,
        distinctFromB4,
        distinctFromB5,
        distinctFromB7,
        containmentRisk,
        verdict,
        failReason
      )

  private val RequiredColumns =
    List(
      "candidate_id",
      "source",
      "exact_fen",
      "trigger",
      "root_best",
      "trigger_is_root_best",
      "best_defense",
      "same_branch",
      "intermediate_detour",
      "downstream_reroute_denial",
      "continuationBound",
      "releaseSuppression",
      "distinct_from_B4",
      "distinct_from_B5",
      "distinct_from_B7",
      "containment_risk",
      "verdict",
      "fail_reason"
    )

  private val HeavyReleaseFeatures =
    Set(
      "queen_infiltration",
      "rook_lift",
      "perpetual_check",
      "forcing_checks",
      "exchange_sac_release"
    )

  private val candidates =
    List(
      Candidate(
        id = "b6_control",
        source = "modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.BroaderB6,
        note = "after-trigger detour survives but root-best stayed c3b4",
        bestDefensePrefix = List("a7a5", "b4a5", "c6a5", "f3e5"),
        intermediateMove = Some("c6a5"),
        downstreamMove = Some("a5c4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "yes",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_k09c",
        source = "StrategicIdeaFenFixtures.K09C / modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "r1b1r1k1/pp1qbpp1/2n2n1p/3p4/3N4/P1N1B1P1/1P2PPBP/R2Q1RK1 w - - 1 13",
        trigger = "d1b3",
        family = FamilyHint.BroaderB6,
        note = "trigger is not root-best and row drifts into K09 simplification pressure",
        bestDefensePrefix = List("c6a5"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "weak",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "b6_k09g",
        source = "StrategicIdeaFenFixtures.K09G / modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "r2qr1k1/pp2bpp1/4bn1p/n2p4/3N4/1QN1B1P1/PP2PPBP/R4RK1 w - - 6 14",
        trigger = "b3c2",
        family = FamilyHint.BroaderB6,
        note = "after-trigger route releases directly into Nc4 and heavy-piece play",
        bestDefensePrefix = List("a5c4"),
        downstreamMove = Some("a5c4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "b6_k09h",
        source = "StrategicIdeaFenFixtures.K09H / modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "r1b1r1k1/pp1qbpp1/5n1p/3pn3/1P1N4/P1N1B1P1/4PPBP/R2Q1RK1 w - - 1 14",
        trigger = "d1b3",
        family = FamilyHint.BroaderB6,
        note = "root-best stayed Nxb3 and the branch never becomes B6-clean",
        bestDefensePrefix = List("e5c4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "b6_b21a",
        source = "StrategicIdeaFenFixtures.B21A / modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
        trigger = "f3d2",
        family = FamilyHint.BroaderB6,
        note = "root-best survives but the row stays target-fixing/prophylaxis",
        bestDefensePrefix = List("b8a6"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_rubinstein_duras",
        source = "modules/llm/tmp/b6-second-survivor-campaign-20260402.md",
        fen = "r4bk1/1r2n1p1/p2p1p1p/1q1Pp3/2N1P3/RP1QBPP1/6KP/R7 w - - 0 27",
        trigger = "d3d2",
        family = FamilyHint.ExternalLead,
        note = "root-best survives but heavy-piece release dominates and B5 distinctness fails",
        bestDefensePrefix = List("b7c7"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_shirov_kinsman",
        source = "modules/llm/tmp/b7-second-survivor-hunt-20260402.md",
        fen = "8/1prrk1p1/p1p1ppb1/P1P3p1/2BPP3/4KPP1/1R5P/1R6 w - - 0 1",
        trigger = "h2h4",
        family = FamilyHint.ExternalLead,
        note = "second-weakness fight, not same-branch reroute denial",
        bestDefensePrefix = List("g5h4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_alekhine_vidmar",
        source = "modules/llm/tmp/b7-second-survivor-hunt-20260402.md",
        fen = "1r3k2/2n2ppp/2B1p3/8/RP6/4P3/5PPP/6K1 w q - 0 1",
        trigger = "b4b5",
        family = FamilyHint.ExternalLead,
        note = "endgame-adjacent two-weakness row, not B6 route-chain truth",
        bestDefensePrefix = List("b8d8"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "n/a",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_ext_karpov_kavalek",
        source =
          "modules/llm/tmp/frontier-proving-handoff-20260403.md / https://dgriffinchess.wordpress.com/wp-content/uploads/2021/09/karpov-kavalek-21st-olympiad-nice-1974.pdf",
        fen = "2r3k1/1pr1ppbp/p2p2p1/2nP3P/2P2P2/1P2B3/P2KB1P1/2R4R w - - 1 23",
        trigger = "e2g4",
        family = FamilyHint.BroaderB6,
        note =
          "exact foothold recovered, but Bg4/g4/Rxc4 all stayed non-root and the branch keeps heavy-piece contamination live",
        bestDefensePrefix = List("c5e4", "d2d3", "f7f5"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "yes",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_ext_karpov_timman_1979",
        source =
          "modules/llm/tmp/frontier-proving-handoff-20260403.md / https://dgriffinchess.wordpress.com/wp-content/uploads/2019/05/karpov-timman-montreal-1979.pdf",
        fen = "3rb3/2q1rpbk/n1pp1npp/p7/4PPP1/2P2NNP/1PQ2BB1/3RR1K1 w - - 7 25",
        trigger = "c2d3",
        family = FamilyHint.BroaderB6,
        note =
          "Qd3 is root-best on the exact board, but the defended line stays prophylactic and never hardens into a B6-clean reroute denial",
        bestDefensePrefix = List("d8a8", "d3d2", "f6d7"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "yes",
        distinctFromB4 = "no",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_ext_karpov_unzicker",
        source =
          "modules/llm/tmp/frontier-proving-handoff-20260403.md / https://dgriffinchess.wordpress.com/wp-content/uploads/2021/09/karpov-unzicker-21st-olympiad-nice-1974.pdf",
        fen = "r1rq1bk1/Bnnb1p1p/3p2p1/1p1Pp3/1Pp1P3/2P2NNP/R1BQ1PP1/4R1K1 w - - 6 26",
        trigger = "e1a1",
        family = FamilyHint.BroaderB6,
        note =
          "Rea1 is root-best only after the exact Ne8-Nc7 corridor, but the defended branch still allows a downstream reroute and the same-game B8 overlap stays live",
        bestDefensePrefix = List("d8e7", "c2d1", "c7e8"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "yes",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "yes",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_ext_kasparov_karpov_1985",
        source =
          "modules/llm/tmp/frontier-proving-handoff-20260403.md / https://www.latimes.com/archives/la-xpm-1985-09-16-mn-22008-story.html",
        fen = "b1rn1bk1/3n1ppp/8/1p6/1q2P3/5N1P/BB1N1PP1/3QR1K1 w - - 3 25",
        trigger = "d2b3",
        family = FamilyHint.BroaderB6,
        note =
          "the exact Game 5 corridor is recovered, but Nb3 is not root-best and the branch drifts into the wrong-game-same-players / after-trigger-only trap",
        bestDefensePrefix = List("d7c5", "b2e5", "a8e4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "yes",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "boc_ke7",
        source = "modules/llm/tmp/b8a_slight_edge_local_squeeze_20260402.md",
        fen = "3R1k2/5ppp/ppb2n2/2r5/P1p5/2N1P1N1/1PP3PP/6K1 b - - 0 27",
        trigger = "f8e7",
        family = FamilyHint.PostureBlocker,
        note = "clean exact-FEN row but it belongs to B8 squeeze posture, not B6",
        bestDefensePrefix = List("d8b8"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "yan_posture_blocker",
        source = "modules/llm/tmp/b8a_slight_edge_local_squeeze_20260402.md",
        fen = "2b3k1/p3qp1p/6p1/4P3/3p4/P2B3P/3Q1PP1/6K1 w - - 0 28",
        trigger = "d2a5",
        family = FamilyHint.PostureBlocker,
        note = "slight-edge shell with queen activity; posture inflation blocks reuse",
        bestDefensePrefix = List("c8e6"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "weak",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b6_blocker_file_entry_restatement",
        source = "NamedRouteNetworkBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "c3b4",
        family = FamilyHint.B4Blocker,
        note = "only file-entry truth survives; reroute witness is not independent",
        bestDefensePrefix = List("h6h5"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "no",
        distinctFromB5 = "yes",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "same_first_move_divergent_branch",
        source = "NamedRouteNetworkBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "first defended reply matches but later proof is stitched from another branch",
        bestDefensePrefix = List("a7a5"),
        baselineSameBranch = "no",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "ambiguous_defended_branch",
        source = "NamedRouteNetworkBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "multiple defended branches survive, so there is no unique exportable branch identity",
        baselineSameBranch = "no",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "b6_blocker_chain_only_nonbest",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "the attractive chain only survives off the defended best branch",
        baselineSameBranch = "no",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "b6_blocker_fake_route_chain",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "chain wording survives after edge evidence is stripped",
        baselineSameBranch = "n/a",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "redundant_intermediate_node",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "same square is counted as both intermediate node and reroute denial",
        baselineSameBranch = "n/a",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "untouched_sector_escape",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "local shell is irrelevant if another sector route stays live",
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "partial",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "no"
      ),
      Candidate(
        id = "b6_blocker_route_network_mirage",
        source = "NamedRouteNetworkBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "reroute signal exists without exact same-branch edge proof",
        baselineSameBranch = "no",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "engine_pv_paraphrase",
        source = "NamedRouteChainBindBroadValidationTest",
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24",
        trigger = "a3b4",
        family = FamilyHint.B4Blocker,
        note = "direct-reply motifs remain but intermediate route proof is missing",
        baselineSameBranch = "n/a",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "no",
        baselineReleaseSuppression = "n/a"
      ),
      Candidate(
        id = "b5_queen_infiltration",
        source = "HeavyPieceLocalBindNegativeValidationTest",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24",
        trigger = "c2c4",
        family = FamilyHint.B5Blocker,
        note = "heavy-piece release remains the real story and the trigger is not root-best",
        bestDefensePrefix = List("c3c4"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "b5_rook_lift_switch",
        source = "TaskShiftProvingFixtures / HeavyPieceLocalBindNegativeValidationTest",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24",
        trigger = "c2e2",
        family = FamilyHint.B5Blocker,
        note = "rook-lift and cross-rank release survive the branch",
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "yes"
      ),
      Candidate(
        id = "k09b_b7_blocker",
        source = "TaskShiftProvingFixtures.K09B",
        fen = "r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13",
        trigger = "d4e6",
        family = FamilyHint.B7Blocker,
        note = "simplification/task-handoff control, not route-chain truth",
        bestDefensePrefix = List("f7e6"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "k09f_b7_blocker",
        source = "TaskShiftProvingFixtures.K09F",
        fen = "2rqr1k1/pp2bpp1/2n1bn1p/3p4/3N4/P1N1B1P1/1P2PPBP/2RQ1RK1 w - - 1 14",
        trigger = "d4e6",
        family = FamilyHint.B7Blocker,
        note = "holdable simplification shell remains too broad after the recapture",
        bestDefensePrefix = List("f7e6"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "k09e_b7_blocker",
        source = "TaskShiftProvingFixtures.K09E",
        fen = "r1bq1rk1/pp3ppp/5n2/3p4/1PnP4/2N2N2/P3BPPP/R2Q1RK1 w - - 1 13",
        trigger = "a1c1",
        family = FamilyHint.B7Blocker,
        note = "file pressure and release features stay primary",
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "no",
        distinctFromB4 = "yes",
        distinctFromB5 = "no",
        distinctFromB7 = "no"
      ),
      Candidate(
        id = "b21_target_fixation",
        source = "TaskShiftProvingFixtures.B21",
        fen = "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 0 9",
        trigger = "f3d2",
        family = FamilyHint.B7Blocker,
        note = "target-fixation/prophylaxis row with no task handoff",
        bestDefensePrefix = List("b8a6"),
        baselineSameBranch = "yes",
        baselineIntermediateDetour = "no",
        baselineDownstreamRerouteDenial = "no",
        baselineContinuationBound = "yes",
        baselineReleaseSuppression = "n/a",
        distinctFromB4 = "yes",
        distinctFromB5 = "yes",
        distinctFromB7 = "no"
      )
    )

  @main def runNamedRouteValidationMatrixWorkup(args: String*): Unit =
    val requestedIds =
      args
        .find(_.startsWith("--ids="))
        .map(_.stripPrefix("--ids=").split(",").toList.map(_.trim).filter(_.nonEmpty).toSet)
        .getOrElse(Set.empty)
    val requestedDepth =
      args
        .find(_.startsWith("--depth="))
        .flatMap(arg => arg.stripPrefix("--depth=").toIntOption)
    val requestedMultiPv =
      args
        .find(_.startsWith("--multipv="))
        .flatMap(arg => arg.stripPrefix("--multipv=").toIntOption)
    val outputPath =
      args
        .find(_.startsWith("--output="))
        .map(arg => Paths.get(arg.stripPrefix("--output=")))
        .getOrElse(defaultOutputPath())

    val selected =
      candidates.filter(candidate =>
        requestedIds.isEmpty || requestedIds.contains(candidate.id)
      )

    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("No engine available. Set STOCKFISH_BIN or LLM_ACTIVE_CORPUS_ENGINE_PATH.")
      case Some(enginePath) =>
        println(s"engine_path=$enginePath")
        val rows =
          selected.map { candidate =>
            val depth = requestedDepth.getOrElse(candidate.depth)
            val multiPv = requestedMultiPv.getOrElse(candidate.multiPv)
            buildRow(candidate, depth, multiPv)
          }
        println(RequiredColumns.mkString("\t"))
        rows.foreach(row => println(row.values.map(escape).mkString("\t")))
        writeRows(outputPath, rows)
        println(s"matrix_path=${outputPath.toAbsolutePath.normalize}")

  private def buildRow(
      candidate: Candidate,
      depth: Int,
      multiPv: Int
  ): MatrixRow =
    val root =
      HeavyPieceLocalBindEngineVerifier
        .analyze(
          fen = candidate.fen,
          depth = depth,
          multiPv = multiPv
        )
        .getOrElse(sys.error(s"missing root analysis for ${candidate.id}"))
    val after =
      HeavyPieceLocalBindEngineVerifier
        .analyze(
          fen = candidate.fen,
          depth = depth,
          multiPv = multiPv,
          moves = List(candidate.trigger)
        )
        .getOrElse(sys.error(s"missing after-trigger analysis for ${candidate.id}"))
    val afterLine =
      chooseAfterLine(candidate, after)
    val replay =
      afterLine.flatMap { line =>
        HeavyPieceLocalBindValidation.replayBranchLine(
          candidate.fen,
          candidate.trigger :: line.moves
        )
      }
    val replayFeatures =
      replay.map(_.features.toSet.intersect(HeavyReleaseFeatures)).getOrElse(Set.empty)
    val triggerIsRootBest =
      root.bestMove.contains(candidate.trigger)
    val sameBranch =
      observedSameBranch(candidate, afterLine, replay.exists(_.complete))
    val intermediateDetour =
      observedIntermediateDetour(candidate, afterLine, replay)
    val downstreamRerouteDenial =
      observedDownstreamRerouteDenial(candidate, afterLine, replay)
    val continuationBound =
      observedContinuationBound(candidate, replay)
    val releaseSuppression =
      observedReleaseSuppression(candidate, replayFeatures, replay.exists(_.complete))
    val containmentRisk =
      classifyContainmentRisk(
        triggerIsRootBest = triggerIsRootBest,
        sameBranch = sameBranch,
        downstreamRerouteDenial = downstreamRerouteDenial,
        releaseSuppression = releaseSuppression,
        family = candidate.family
      )
    val verdict =
      classifyVerdict(
        candidate = candidate,
        triggerIsRootBest = triggerIsRootBest,
        sameBranch = sameBranch,
        intermediateDetour = intermediateDetour,
        downstreamRerouteDenial = downstreamRerouteDenial,
        continuationBound = continuationBound,
        releaseSuppression = releaseSuppression
      )

    MatrixRow(
      candidateId = candidate.id,
      source = candidate.source,
      exactFen = candidate.fen,
      trigger = candidate.trigger,
      rootBest = root.bestMove.getOrElse(""),
      triggerIsRootBest = yesNo(triggerIsRootBest),
      bestDefense =
        afterLine
          .flatMap(_.moves.headOption)
          .orElse(after.bestMove)
          .getOrElse(""),
      sameBranch = sameBranch,
      intermediateDetour = intermediateDetour,
      downstreamRerouteDenial = downstreamRerouteDenial,
      continuationBound = continuationBound,
      releaseSuppression = releaseSuppression,
      distinctFromB4 = candidate.distinctFromB4,
      distinctFromB5 = candidate.distinctFromB5,
      distinctFromB7 = candidate.distinctFromB7,
      containmentRisk = containmentRisk,
      verdict = verdict,
      failReason =
        buildFailReason(
          candidate = candidate,
          triggerIsRootBest = triggerIsRootBest,
          sameBranch = sameBranch,
          intermediateDetour = intermediateDetour,
          downstreamRerouteDenial = downstreamRerouteDenial,
          continuationBound = continuationBound,
          releaseSuppression = releaseSuppression,
          replayFeatures = replayFeatures
        )
    )

  private def chooseAfterLine(
      candidate: Candidate,
      analysis: HeavyPieceLocalBindEngineVerifier.EngineAnalysis
  ): Option[HeavyPieceLocalBindEngineVerifier.EngineLine] =
    if candidate.bestDefensePrefix.nonEmpty then
      analysis.lines.find(_.moves.take(candidate.bestDefensePrefix.size) == candidate.bestDefensePrefix)
        .orElse(analysis.lines.headOption)
    else analysis.lines.headOption

  private def observedSameBranch(
      candidate: Candidate,
      afterLine: Option[HeavyPieceLocalBindEngineVerifier.EngineLine],
      replayComplete: Boolean
  ): String =
    if candidate.bestDefensePrefix.nonEmpty then
      yesNo(
        replayComplete &&
          afterLine.exists(_.moves.take(candidate.bestDefensePrefix.size) == candidate.bestDefensePrefix)
      )
    else candidate.baselineSameBranch

  private def observedIntermediateDetour(
      candidate: Candidate,
      afterLine: Option[HeavyPieceLocalBindEngineVerifier.EngineLine],
      replay: Option[HeavyPieceLocalBindValidation.ExactBranchReplay]
  ): String =
    candidate.intermediateMove match
      case Some(move) if containsMove(afterLine, replay, move) => "yes"
      case Some(_) if candidate.baselineIntermediateDetour != "unknown" =>
        candidate.baselineIntermediateDetour
      case Some(_) => "no"
      case None    => candidate.baselineIntermediateDetour

  private def observedDownstreamRerouteDenial(
      candidate: Candidate,
      afterLine: Option[HeavyPieceLocalBindEngineVerifier.EngineLine],
      replay: Option[HeavyPieceLocalBindValidation.ExactBranchReplay]
  ): String =
    candidate.downstreamMove match
      case Some(move) if containsMove(afterLine, replay, move) => "no"
      case Some(_) if candidate.baselineDownstreamRerouteDenial != "unknown" =>
        candidate.baselineDownstreamRerouteDenial
      case Some(_) => "unknown"
      case None    => candidate.baselineDownstreamRerouteDenial

  private def observedContinuationBound(
      candidate: Candidate,
      replay: Option[HeavyPieceLocalBindValidation.ExactBranchReplay]
  ): String =
    if replay.exists(_.complete) then
      if candidate.baselineContinuationBound != "unknown" then candidate.baselineContinuationBound
      else "yes"
    else "no"

  private def observedReleaseSuppression(
      candidate: Candidate,
      replayFeatures: Set[String],
      replayComplete: Boolean
  ): String =
    if replayFeatures.nonEmpty then "no"
    else if candidate.baselineReleaseSuppression != "unknown" then candidate.baselineReleaseSuppression
    else if replayComplete then "unknown"
    else "n/a"

  private def classifyContainmentRisk(
      triggerIsRootBest: Boolean,
      sameBranch: String,
      downstreamRerouteDenial: String,
      releaseSuppression: String,
      family: FamilyHint
  ): String =
    if !triggerIsRootBest || sameBranch == "no" || downstreamRerouteDenial == "no" || releaseSuppression == "no" then
      "high"
    else
      family match
        case FamilyHint.PostureBlocker => "high"
        case FamilyHint.BroaderB6      => "medium"
        case _                         => "medium"

  private def classifyVerdict(
      candidate: Candidate,
      triggerIsRootBest: Boolean,
      sameBranch: String,
      intermediateDetour: String,
      downstreamRerouteDenial: String,
      continuationBound: String,
      releaseSuppression: String
  ): String =
    val positiveSurvivor =
      candidate.family == FamilyHint.BroaderB6 &&
        triggerIsRootBest &&
        sameBranch == "yes" &&
        intermediateDetour == "yes" &&
        downstreamRerouteDenial == "yes" &&
        continuationBound == "yes" &&
        releaseSuppression == "yes" &&
        candidate.distinctFromB4 == "yes" &&
        candidate.distinctFromB5 == "yes" &&
        candidate.distinctFromB7 == "yes"
    if positiveSurvivor then "positive_survivor"
    else
      candidate.family match
        case FamilyHint.BroaderB6 | FamilyHint.ExternalLead => "near_miss"
        case _                                              => "blocker"

  private def buildFailReason(
      candidate: Candidate,
      triggerIsRootBest: Boolean,
      sameBranch: String,
      intermediateDetour: String,
      downstreamRerouteDenial: String,
      continuationBound: String,
      releaseSuppression: String,
      replayFeatures: Set[String]
  ): String =
    val reasons =
      List(
        Option.when(!triggerIsRootBest)("trigger_not_root_best"),
        Option.when(sameBranch != "yes" && sameBranch != "n/a")("same_branch_missing"),
        Option.when(intermediateDetour == "no")("intermediate_detour_unproven"),
        Option.when(downstreamRerouteDenial == "no")("downstream_reroute_visible"),
        Option.when(continuationBound == "no")("bounded_continuation_missing"),
        Option.when(candidate.distinctFromB4 == "no")("b4_relabel"),
        Option.when(candidate.distinctFromB5 == "no")("b5_heavy_piece_leakage"),
        Option.when(candidate.distinctFromB7 == "no")("b7_task_shift_drift"),
        Option.when(candidate.family == FamilyHint.PostureBlocker)("posture_inflation"),
        Option.when(releaseSuppression == "no" && replayFeatures.nonEmpty)(
          s"release_features:${replayFeatures.toList.sorted.mkString(",")}"
        ),
        Option.when(releaseSuppression == "no" && replayFeatures.isEmpty)("release_suppression_failed")
      ).flatten
    (reasons :+ candidate.note).mkString("; ")

  private def containsMove(
      afterLine: Option[HeavyPieceLocalBindEngineVerifier.EngineLine],
      replay: Option[HeavyPieceLocalBindValidation.ExactBranchReplay],
      move: String
  ): Boolean =
    afterLine.exists(_.moves.contains(move)) ||
      replay.exists(_.replayedUci.contains(move))

  private def writeRows(
      outputPath: Path,
      rows: List[MatrixRow]
  ): Unit =
    val normalized = outputPath.toAbsolutePath.normalize
    Option(normalized.getParent).foreach(parent => Files.createDirectories(parent))
    val body =
      (RequiredColumns +: rows.map(_.values))
        .map(_.map(escape).mkString("\t"))
        .mkString(System.lineSeparator())
    Files.writeString(normalized, body + System.lineSeparator(), StandardCharsets.UTF_8)

  private def defaultOutputPath(): Path =
    val stamp = LocalDate.now().toString.replace("-", "")
    Paths.get("modules", "llm", "tmp", s"named-route-validation-matrix-$stamp.tsv")

  private def yesNo(value: Boolean): String =
    if value then "yes" else "no"

  private def escape(value: String): String =
    Option(value).getOrElse("").replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
