package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*
import lila.commentary.analysis.{ PlanTaxonomy, PlayerFacingTruthModePolicy }
import munit.FunSuite

class AdmissionUnitReviewTest extends FunSuite:

  private val samplePgn =
    """[Event "Prophylaxis sample"]
      |[Site "?"]
      |[Date "2026.01.01"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |
      |1. d4 Nf6 2. c4 e6 3. Nc3 c5 4. d5 d6 5. Nf3 Be7 6. a3 O-O *
      |""".stripMargin

  private def source(
      id: String,
      reviewGroup: String = "A:prophylaxis_restraint",
      ply: Int = 1
  ): SourceWitnessCatalog.SourceCandidate =
    SourceWitnessCatalog.SourceCandidate(
      id = id,
      gameName = "Prophylaxis sample",
      sourceUrl = "https://example.invalid/prophylaxis.pgn",
      pgn = samplePgn,
      candidatePlyRange = SourceWitnessCatalog.CandidatePlyRange(ply, ply),
      reviewGroup = reviewGroup,
      intendedVerdict = SourceReview.Verdict.ScreenOnly,
      validationNote = "test source"
    )

  private def observation(
      id: String,
      verdict: String = SourceReview.Verdict.RejectOwnerMissing,
      diagnosis: String = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
      engineAgreement: String = "top_pv_matches_played",
      blockers: String = "owner:prophylaxis_restraint_witness_missing",
      reviewGroup: String = "A:prophylaxis_restraint",
      mainProofSource: String = "-",
      packetSummary: String = "-",
      contractId: String = "-",
      contractStatus: String = "-",
      release: String = "-",
      primary: String = "-",
      playedUci: String = "a2a3",
      ply: Int = 45,
      surfaceGate: String = "supported_local_surface_passed"
  ): SourceReview.Observation =
    SourceReview.Observation(
      source = source(id, reviewGroup),
      verdict = verdict,
      diagnosis = diagnosis,
      engineAgreement = engineAgreement,
      plannerOwnership = "primary_SupportedLocal",
      surfaceGate = surfaceGate,
      release = release,
      taxonomy = "source_prophylaxis_restraint",
      ply = Some(ply),
      fen = Some("2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23"),
      playedUci = Some(playedUci),
      enginePv = List(playedUci, "b7b5", "a3a4", "c6b4"),
      primary = primary,
      moveReview = primary,
      reason = "test observation",
      mainProofSource = mainProofSource,
      mainClaimScope = "MoveLocal",
      packetSummary = packetSummary,
      contractId = contractId,
      contractStatus = contractStatus,
      contractFailures = "-",
      sceneType = "quiet_improvement",
      ownerTraceSummary = "selected=WhyThis:MoveDelta:prophylactic_move",
      ownerFailureCodes =
        if blockers == "none" then "-" else blockers.stripPrefix("owner:"),
      admissionBlockers = blockers
    )

  test("loads the prophylaxis restraint admission-unit spec from the shared catalog") {
    val contract =
      AdmissionUnitReview
        .specForPlanKind("prophylaxis_restraint")
        .getOrElse(fail("missing prophylaxis_restraint contract"))

    assertEquals(contract.planKindId, "prophylaxis_restraint")
    assertEquals(contract.proofSource, "prophylactic_move")
    assertEquals(contract.proofFamily, "counterplay_restraint")
    assertEquals(contract.surfaceAuthorityTiers, List("SupportedLocal"))
  }

  test("builds transient prophylaxis source candidates with exact one-ply ranges") {
    val game =
      AdmissionUnitReview.SourceGame(
        id = "modern-benoni-0001",
        label = "Modern Benoni sample",
        pgn = samplePgn,
        sourceUrl = "https://example.invalid/source.pgn"
      )
    val candidate =
      AdmissionUnitReview.sourceCandidateFor(
        game = game,
        ply = lila.commentary.PgnAnalysisHelper.PlyData(
          ply = 3,
          fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/P1P1PN1P/1P4P1/2R2RK1 b - - 0 23",
          playedMove = "a4",
          playedUci = "a3a4",
          color = chess.White
        ),
        planKind = "prophylaxis_restraint"
      )

    assertEquals(candidate.id, "source-modern-benoni-0001-prophylaxis-restraint-ply-3")
    assertEquals(candidate.reviewGroup, "A:prophylaxis_restraint")
    assertEquals(candidate.candidatePlyRange.start, 3)
    assertEquals(candidate.candidatePlyRange.end, 3)
  }

  test("transient source candidates use the review group for their proof family") {
    val game =
      AdmissionUnitReview.SourceGame(
        id = "review-group-sample",
        label = "Review group sample",
        pgn = samplePgn,
        sourceUrl = "https://example.invalid/source.pgn"
      )
    val ply =
      lila.commentary.PgnAnalysisHelper.PlyData(
        ply = 9,
        fen = "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 23",
        playedMove = "Nxd5",
        playedUci = "c3d5",
        color = chess.White
      )

    val cases =
      List(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id -> "B:static_weakness_fixation",
        PlanTaxonomy.PlanKind.BackwardPawnTargeting.id -> "B:carlsbad_fixed_target",
        PlanTaxonomy.PlanKind.IQPInducement.id -> "C:iqp_inducement",
        PlanTaxonomy.PlanKind.SimplificationWindow.id -> "C:simplification_window",
        PlanTaxonomy.PlanKind.DefenderTrade.id -> "C:defender_trade",
        PlanTaxonomy.PlanKind.QueenTradeShield.id -> "C:queen_trade_boundary",
        PlanTaxonomy.PlanKind.BadPieceLiquidation.id -> "C:bad_piece_liquidation",
        PlanTaxonomy.PlanKind.OpenFilePressure.id -> "A:open_file_pressure"
      )

    cases.foreach { case (planKind, reviewGroup) =>
      val candidate = AdmissionUnitReview.sourceCandidateFor(game, ply, planKind)
      assertEquals(candidate.reviewGroup, reviewGroup, clue(candidate))
    }
  }

  test("bad-piece liquidation transient candidates use C review group and allow captures") {
    val capturePgn =
      """[Event "Bad piece liquidation sample"]
        |[Site "?"]
        |[Date "2026.01.01"]
        |[Round "?"]
        |[White "White"]
        |[Black "Black"]
        |[Result "*"]
        |[SetUp "1"]
        |[FEN "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1"]
        |
        |1. Ba3 Kf7 2. Bxf8 Kxf8 *
        |""".stripMargin
    val report =
      AdmissionUnitReview.admit(
        games = List(
          AdmissionUnitReview.SourceGame(
            id = "bad-piece-sample",
            label = "Bad piece sample",
            pgn = capturePgn,
            sourceUrl = "https://example.invalid/bad-piece.pgn"
          )
        ),
        engine = None,
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = "bad_piece_liquidation",
          maxCandidates = 4
        )
      )

    assert(report.rows.exists(_.observation.playedUci.contains("a3f8")), clues(report.tsv))
    assert(report.rows.forall(_.observation.source.reviewGroup == "C:bad_piece_liquidation"), clues(report.tsv))
  }

  test("transition admission units do not pre-screen away capture candidates") {
    val capturePgn =
      """[Event "Transition capture sample"]
        |[Site "?"]
        |[Date "2026.01.01"]
        |[Round "?"]
        |[White "White"]
        |[Black "Black"]
        |[Result "*"]
        |
        |1. e4 d5 2. exd5 Qxd5 3. Nc3 Qe5+ 4. Be2 *
        |""".stripMargin
    val game =
      AdmissionUnitReview.SourceGame(
        id = "transition-capture",
        label = "Transition capture sample",
        pgn = capturePgn,
        sourceUrl = "https://example.invalid/transition.pgn"
      )
    val transitionReport =
      AdmissionUnitReview.admit(
        games = List(game),
        engine = None,
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = PlanTaxonomy.PlanKind.SimplificationWindow.id,
          maxCandidates = 8
        )
      )
    val quietReport =
      AdmissionUnitReview.admit(
        games = List(game),
        engine = None,
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
          maxCandidates = 8
        )
      )

    assert(transitionReport.rows.exists(_.observation.playedUci.contains("e4d5")), clues(transitionReport.tsv))
    assert(!quietReport.rows.exists(_.observation.playedUci.contains("e4d5")), clues(quietReport.tsv))
  }

  test("ranks exact counterplay-restraint SupportedLocal rows ahead of mismatches and noise") {
    val admitted =
      observation(
        id = "source-admitted-prophylaxis",
        verdict = SourceReview.Verdict.AdmitAuthorityRow,
        diagnosis = SourceReview.Diagnosis.AdmitReady,
        blockers = "none",
        mainProofSource = "prophylactic_move",
        packetSummary = "proof_source=prophylactic_move;proof_family=counterplay_restraint;scope=MoveLocal",
        contractId = "runtime:counterplay_restraint",
        contractStatus = "Releasable",
        release = "SupportedLocal",
        primary = "A key idea is that this slows down queenside counterplay before it gets started."
      )
    val mismatch =
      observation(
        id = "source-mismatch",
        blockers = "proof:prophylaxis_restraint_contract_mismatch",
        mainProofSource = "counterplay_axis_suppression",
        packetSummary = "proof_source=counterplay_axis_suppression;proof_family=neutralize_key_break"
      )
    val openingNoise =
      observation(
        id = "source-opening-noise",
        engineAgreement = "near_top_multipv_contains_played_top=g8f6_gap=22cp",
        ply = 8
      )

    val ranked =
      AdmissionUnitReview.rankObservations(
        List(mismatch, openingNoise, admitted),
        AdmissionUnitReview.AdmissionConfig(planKind = "prophylaxis_restraint")
      )

    assertEquals(ranked.head.observation.source.id, "source-admitted-prophylaxis")
    assert(ranked.head.scoreReasons.contains("exact_admission_unit_authority"), clues(ranked.head))
    assert(ranked.exists(_.scoreReasons.contains("opening_downranked")), clues(ranked))
  }

  test("report treats CertifiedOwner exact rows as admitted authority rows") {
    val admitted =
      observation(
        id = "source-admitted-static-weakness",
        verdict = SourceReview.Verdict.AdmitAuthorityRow,
        diagnosis = SourceReview.Diagnosis.AdmitReady,
        blockers = "none",
        reviewGroup = "B:static_weakness_fixation",
        mainProofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
        packetSummary = "proof_source=exact_target_fixation;proof_family=static_weakness_fixation;scope=PositionLocal",
        contractId = "subplan:static_weakness_fixation",
        contractStatus = "Releasable",
        release = "CertifiedOwner",
        surfaceGate = "certified_owner_surface_not_blocking",
        primary = "This keeps d6 fixed as the target."
      )
    val blocked =
      observation(
        id = "source-blocked-static-weakness",
        reviewGroup = "B:static_weakness_fixation",
        blockers = "owner:static_weakness_fixation_witness_missing"
      )

    val report =
      AdmissionUnitReview.reportFromObservations(
        observations = List(blocked, admitted),
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
          admitLimit = 2
        )
      )

    assertEquals(report.admittedRows.map(_.ranked.observation.source.id), List("source-admitted-static-weakness"))
    assert(report.admittedRows.head.ranked.scoreReasons.contains("exact_admission_unit_authority"), clues(report.tsv))
  }

  test("report rejects CertifiedOwner rows for supported-only admission units") {
    val unsupportedAuthority =
      observation(
        id = "source-bad-piece-wrong-authority",
        verdict = SourceReview.Verdict.AdmitAuthorityRow,
        diagnosis = SourceReview.Diagnosis.AdmitReady,
        blockers = "none",
        reviewGroup = "C:bad_piece_liquidation",
        mainProofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
        packetSummary = "proof_source=bad_piece_liquidation;proof_family=bad_piece_liquidation;scope=MoveLocal",
        contractId = "subplan:bad_piece_liquidation",
        contractStatus = "Releasable",
        release = "CertifiedOwner",
        surfaceGate = "certified_owner_surface_not_blocking",
        primary = "This trades off the bad bishop."
      )

    val report =
      AdmissionUnitReview.reportFromObservations(
        observations = List(unsupportedAuthority),
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
          admitLimit = 1
        )
      )

    assertEquals(report.admittedRows, Nil)
    assert(!report.rows.head.ranked.scoreReasons.contains("exact_admission_unit_authority"), clues(report.tsv))
  }

  test("report keeps certified-eligible move-delta units on SupportedLocal surface authority") {
    val unsupportedAuthority =
      observation(
        id = "source-simplification-wrong-authority",
        verdict = SourceReview.Verdict.AdmitAuthorityRow,
        diagnosis = SourceReview.Diagnosis.AdmitReady,
        blockers = "none",
        reviewGroup = "C:simplification_window",
        mainProofSource = PlanTaxonomy.PlanKind.SimplificationWindow.id,
        packetSummary = "proof_source=simplification_window;proof_family=simplification_window;scope=MoveLocal",
        contractId = "subplan:simplification_window",
        contractStatus = "Releasable",
        release = "CertifiedOwner",
        surfaceGate = "certified_owner_surface_not_blocking",
        primary = "This exchange keeps the same local edge."
      )
    val supportedAuthority =
      unsupportedAuthority.copy(
        source = source("source-simplification-supported", reviewGroup = "C:simplification_window"),
        release = "SupportedLocal",
        surfaceGate = "supported_local_surface_passed"
      )

    val report =
      AdmissionUnitReview.reportFromObservations(
        observations = List(unsupportedAuthority, supportedAuthority),
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = PlanTaxonomy.PlanKind.SimplificationWindow.id,
          admitLimit = 2
        )
      )

    assertEquals(report.admittedRows.map(_.observation.source.id), List("source-simplification-supported"))
    assert(report.rows.exists(_.observation.release == "CertifiedOwner"), clues(report.tsv))
  }

  test("report applies admit limit and keeps non-admitted blocker evidence") {
    val admittedA =
      observation(
        id = "source-admitted-a",
        verdict = SourceReview.Verdict.AdmitAuthorityRow,
        diagnosis = SourceReview.Diagnosis.AdmitReady,
        blockers = "none",
        mainProofSource = "prophylactic_move",
        packetSummary = "proof_family=counterplay_restraint",
        contractId = "runtime:counterplay_restraint",
        contractStatus = "Releasable",
        release = "SupportedLocal",
        primary = "A key idea is that this slows down queenside counterplay before it gets started."
      )
    val admittedB = admittedA.copy(source = source("source-admitted-b"), ply = Some(47))
    val blocked =
      observation(
        id = "source-blocked",
        blockers = "owner:prophylaxis_restraint_route_persistence_missing"
      )

    val report =
      AdmissionUnitReview.reportFromObservations(
        observations = List(blocked, admittedB, admittedA),
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = "prophylaxis_restraint",
          admitLimit = 1
        )
      )

    assertEquals(report.admittedRows.map(_.ranked.observation.source.id), List("source-admitted-a"))
    assert(report.markdown.contains("owner:prophylaxis_restraint_route_persistence_missing"), clues(report.markdown))
  }

  test("dry-runs transient candidates through SourceReview and reports engine blockers") {
    val report =
      AdmissionUnitReview.admit(
        games = List(
          AdmissionUnitReview.SourceGame(
            id = "sample",
            label = "Prophylaxis sample",
            pgn = samplePgn,
            sourceUrl = "https://example.invalid/prophylaxis.pgn"
          )
        ),
        engine = None,
        config = AdmissionUnitReview.AdmissionConfig(
          planKind = "prophylaxis_restraint",
          maxCandidates = 2
        )
      )

    assert(report.rows.nonEmpty, clues(report.markdown))
    assert(report.rows.exists(_.observation.admissionBlockers == "engine:missing"), clues(report.tsv))
  }
