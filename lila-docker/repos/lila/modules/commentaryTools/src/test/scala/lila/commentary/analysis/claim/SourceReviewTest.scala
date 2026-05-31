package lila.commentary.analysis.claim

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite
import lila.commentary.model.strategic.VariationLine

class SourceReviewTest extends FunSuite:

  private def sourceReviewToolingSource: String =
    Files.readString(
      Paths.get("modules/commentaryTools/src/test/scala/lila/commentary/tools/claim/SourceReview.scala"),
      StandardCharsets.UTF_8
    )

  private final class StaticSourceReviewEngine(linesByFen: Map[String, List[VariationLine]])
      extends SourceReview.SourceReviewEngine:
    override def newGame(): Unit = ()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      linesByFen.getOrElse(fen, Nil)

  test("source review centralizes packet proof-family contract checks") {
    val text = sourceReviewToolingSource
    val directPacketSummaryChecks = "\\.packetSummary\\.exists".r.findAllIn(text).toList

    assert(text.contains("SurfaceContractDescriptor"), clues("missing SourceReview surface contract descriptor"))
    assertEquals(directPacketSummaryChecks, Nil)
  }

  test("source review classifies tactical-first examples as non-strategic") {
    val observations = SourceReview.observations(engine = None)
    val tacticalRows =
      observations.filter(obs => obs.source.reviewGroup.toLowerCase.contains("tactical"))

    assert(tacticalRows.nonEmpty, clues(observations.map(obs => obs.source.id -> obs.source.reviewGroup)))
    assert(
      tacticalRows.forall(obs =>
        obs.verdict == SourceReview.Verdict.RejectTacticalFirst ||
          obs.verdict == SourceReview.Verdict.RejectOwnerMissing ||
          obs.verdict == SourceReview.Verdict.ScreenOnly
      ),
      clues(tacticalRows)
    )
    assert(!tacticalRows.exists(_.verdict == SourceReview.Verdict.AdmitAuthorityRow), clues(tacticalRows))
  }

  test("carlsbad source candidates reach exact replay or documented rejection") {
    val observations = SourceReview.observations(engine = None)
    val carlsbad =
      observations.filter(obs => obs.source.reviewGroup.toLowerCase.contains("carlsbad"))

    assert(carlsbad.nonEmpty, clues(observations.map(_.source.reviewGroup)))
    assert(
      carlsbad.exists(obs =>
        obs.verdict == SourceReview.Verdict.AdmitAuthorityRow ||
          obs.verdict == SourceReview.Verdict.RejectOwnerMissing
      ),
      clues(carlsbad)
    )
    assert(carlsbad.forall(_.fen.nonEmpty), clues(carlsbad))
  }

  test("source review report documents admitted SupportedLocal absence") {
    val observations = SourceReview.observations(engine = None)
    val report = SourceReview.markdown(observations)

    if !observations.exists(obs => obs.verdict == SourceReview.Verdict.AdmitAuthorityRow && obs.release == "SupportedLocal") then
      assert(report.contains("Admitted SupportedLocal source rows: none found"), clues(report))
  }

  test("break-prevention source candidates stay screen-only without exact owner proof") {
    val observations = SourceReview.observations(engine = None)
    val breakRows =
      observations.filter(_.source.reviewGroup == "A:break_prevention")

    assertEquals(
      breakRows.map(_.source.id),
      List(
        "source-karpov-unzicker-1974-break-prevention",
        "source-karpov-andersson-1975-hedgehog-break-screen",
        "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        "source-maderna-palermo-1955-a6-a5-break-prevention",
        "source-camara-bazan-1960-b7-b5-break-prevention",
        "source-sliwa-gromek-1960-a6-a5-break-prevention",
        "source-luckis-bielicki-1961-a6-a5-break-prevention",
        "source-pfleger-maalouf-1961-a6-a5-break-prevention",
        "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention"
      )
    )
    assert(breakRows.forall(_.verdict == SourceReview.Verdict.RejectOwnerMissing), clues(breakRows))
    assert(breakRows.forall(_.diagnosis == SourceReview.Diagnosis.EngineMissingBeforeAdmission), clues(breakRows))
    assert(breakRows.forall(_.taxonomy == "source_break_prevention"), clues(breakRows))
    assert(breakRows.forall(_.admissionBlockers == "engine:missing"), clues(breakRows))
    assert(!breakRows.exists(_.release == "SupportedLocal"), clues(breakRows))
  }

  test("source review keeps admission diagnosis separate from coarse verdict") {
    val observations = SourceReview.observations(engine = None)
    val report = SourceReview.markdown(observations)
    val nonTactical =
      observations.filterNot(_.source.reviewGroup.toLowerCase.contains("tactical"))

    assert(nonTactical.nonEmpty, clues(observations.map(_.source.id)))
    assert(
      nonTactical.forall(_.diagnosis == SourceReview.Diagnosis.EngineMissingBeforeAdmission),
      clues(nonTactical.map(obs => obs.source.id -> obs.diagnosis))
    )
    assert(report.contains("Admission diagnostics: engine_missing_before_admission="), clues(report))
    assert(report.contains("Surface contract blocked: none found"), clues(report))
  }

  test("source review artifacts expose owner proof diagnostics") {
    val observations = SourceReview.observations(engine = None)
    val report = SourceReview.markdown(observations)
    val row = observations.headOption.getOrElse(fail("missing intake row"))

    assert(row.tsv.contains("\t-\t-\t-\t-\t-"), clues(row.tsv))
    assert(report.contains("source="), clues(report))
    assert(report.contains("scope="), clues(report))
    assert(report.contains("scene="), clues(report))
    assert(report.contains("packet="), clues(report))
    assert(report.contains("contract="), clues(report))
    assert(report.contains("contractStatus="), clues(report))
    assert(report.contains("contractFailures="), clues(report))
    assert(report.contains("ownerTrace="), clues(report))
  }

  test("source review separates contract proof status from planner and surface gates") {
    val observations = SourceReview.observations(engine = None)
    val row = observations.headOption.getOrElse(fail("missing intake row"))
    val report = SourceReview.markdown(observations)

    assertEquals(row.contractId, "-")
    assertEquals(row.contractStatus, "-")
    assertEquals(row.contractFailures, "-")
    assert(report.contains("Contract proof:"), clues(report))
  }

  test("admission classifier distinguishes owner extraction, scene blocking, and surface failure") {
    assertEquals(
      SourceReview.classifyAdmission(
        admitted = false,
        release = "-",
        rejected = "WhatMattersHere:position_probe_missing",
        mainClaimScope = None,
        ownerTrace = PlannerOwnerTrace(),
        supportedLocalSurfaceOk = false
      ),
      SourceReview.Diagnosis.RootVocabularyOrExtractionGap
    )
    assertEquals(
      SourceReview.classifyAdmission(
        admitted = false,
        release = "-",
        rejected = "WhatMattersHere:admission_SupportOnly+position_probe_support_only_outside_quiet_scene",
        mainClaimScope = Some("PositionLocal"),
        ownerTrace = PlannerOwnerTrace(
          droppedPlannerOwners = List(
            DroppedPlannerOwnerTrace(
              plannerOwnerKind = PlannerOwnerKind.PositionProbe,
              source = "carlsbad_fixed_target_probe",
              reasons = List("position_probe_support_only_outside_quiet_scene"),
              questionKinds = Nil
            )
          )
        ),
        supportedLocalSurfaceOk = false
      ),
      SourceReview.Diagnosis.PlannerOwnerSceneBlocked
    )
    assertEquals(
      SourceReview.classifyAdmission(
        admitted = false,
        release = "SupportedLocal",
        rejected = "",
        mainClaimScope = Some("PositionLocal"),
        ownerTrace = PlannerOwnerTrace(),
        supportedLocalSurfaceOk = false
      ),
      SourceReview.Diagnosis.SurfaceContractBlocked
    )
  }

  test("engine authority gate separates absent source moves from multipv-only source moves") {
    val capablanca =
      SourceWitnessCatalog.all
        .find(_.id == "source-capablanca-golombek-1939")
        .getOrElse(fail("missing Capablanca source row"))
    val botvinnik =
      SourceWitnessCatalog.all
        .find(_.id == "source-botvinnik-vidmar-1936")
        .getOrElse(fail("missing Botvinnik source row"))

    val absentGate =
      SourceReview.engineAuthorityGate(
        playedUci = "b2b4",
        engineLines =
          List(
            VariationLine(List("g2g4", "e7d6"), scoreCp = 20, depth = 16),
            VariationLine(List("f2f4", "f7f5"), scoreCp = 12, depth = 16)
          )
      )
    assertEquals(absentGate, SourceReview.EngineGate.SourceMoveAbsentFromMultiPv)
    assertEquals(
      SourceReview.admissionBlockers(
        source = capablanca,
        admitted = false,
        engineGate = absentGate,
        ownerDiagnosis = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
        surfaceGate = "not_reached_no_release"
      ),
      "engine:source_move_absent_from_multipv;owner:carlsbad_probe_missing"
    )

    val nearTopMultiPvGate =
      SourceReview.engineAuthorityGate(
        playedUci = "c1a3",
        engineLines =
          List(
            VariationLine(List("c2b1", "f8c5"), scoreCp = 58, depth = 16),
            VariationLine(List("c1a3", "f8a3"), scoreCp = 44, depth = 16)
          )
      )
    assertEquals(nearTopMultiPvGate, "source_move_near_top_multipv")
    assertEquals(
      SourceReview.admissionBlockers(
        source = SourceWitnessCatalog.all
          .find(_.id == "source-aronian-andreikin-2014-defender-trade")
          .getOrElse(fail("missing DefenderTrade source row")),
        admitted = true,
        engineGate = nearTopMultiPvGate,
        ownerDiagnosis = SourceReview.Diagnosis.AdmitReady,
        surfaceGate = "supported_local_surface_passed"
      ),
      "none"
    )

    val multipvOnlyGate =
      SourceReview.engineAuthorityGate(
        playedUci = "d7b6",
        engineLines =
          List(
            VariationLine(List("a7a6", "a2a4"), scoreCp = 18, depth = 16),
            VariationLine(List("d7b6", "c4b3"), scoreCp = -40, depth = 16)
          )
      )
    assertEquals(multipvOnlyGate, SourceReview.EngineGate.SourceMoveMultiPvOnly)
    assertEquals(
      SourceReview.admissionBlockers(
        source = botvinnik,
        admitted = false,
        engineGate = multipvOnlyGate,
        ownerDiagnosis = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
        surfaceGate = "not_reached_no_release"
      ),
      "engine:source_move_multipv_only;owner:iqp_not_induced_or_side_mismatch"
    )

    val breakPrevention =
      SourceWitnessCatalog.all
        .find(_.id == "source-karpov-unzicker-1974-break-prevention")
        .getOrElse(fail("missing break_prevention source row"))
    assertEquals(
      SourceReview.admissionBlockers(
        source = breakPrevention,
        admitted = false,
        engineGate = SourceReview.EngineGate.SourceMoveTopPv,
        ownerDiagnosis = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
        surfaceGate = "not_reached_no_release",
        ownerFailureCodes = List("break_prevention_no_named_break")
      ),
      "owner:break_prevention_no_named_break"
    )
  }

  test("source review exposes prophylaxis-restraint taxonomy and owner blockers") {
    val source =
      SourceWitnessCatalog.SourceCandidate(
        id = "source-prophylaxis-restraint-smoke",
        gameName = "Prophylaxis restraint smoke",
        sourceUrl = "https://example.invalid/prophylaxis.pgn",
        pgn =
          """[Event "Prophylaxis smoke"]
            |[Site "?"]
            |[Date "2026.01.01"]
            |[Round "?"]
            |[White "White"]
            |[Black "Black"]
            |[Result "*"]
            |
            |1. d4 Nf6 2. c4 e6 3. Nc3 c5 4. d5 d6 5. Nf3 Be7 6. a3 O-O *
            |""".stripMargin,
        candidatePlyRange = SourceWitnessCatalog.CandidatePlyRange(1, 1),
        reviewGroup = "A:prophylaxis_restraint",
        intendedVerdict = SourceReview.Verdict.ScreenOnly,
        validationNote = "smoke"
      )

    val engineMissing =
      SourceReview.observationsForSources(List(source), engine = None).head

    assertEquals(engineMissing.taxonomy, "source_prophylaxis_restraint")
    assertEquals(engineMissing.admissionBlockers, "engine:missing")
    assertEquals(
      SourceReview.admissionBlockers(
        source = source,
        admitted = false,
        engineGate = SourceReview.EngineGate.SourceMoveTopPv,
        ownerDiagnosis = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
        surfaceGate = "not_reached_no_release",
        ownerFailureCodes = List("prophylaxis_restraint_contract_mismatch")
      ),
      "proof:prophylaxis_restraint_contract_mismatch"
    )
    assertEquals(
      SourceReview.admissionBlockers(
        source = source,
        admitted = false,
        engineGate = SourceReview.EngineGate.SourceMoveTopPv,
        ownerDiagnosis = SourceReview.Diagnosis.RootVocabularyOrExtractionGap,
        surfaceGate = "not_reached_no_release",
        ownerFailureCodes = List("prophylaxis_restraint_route_persistence_missing")
      ),
      "owner:prophylaxis_restraint_route_persistence_missing"
    )
  }

  test("break-prevention fixed source rows expose concrete witness blockers with engine evidence") {
    val unzickerFen = "1rbn1rk1/2q1bppp/3p1n2/1ppPp3/4P3/2P2N1P/1PBN1PP1/R1BQR1K1 w - - 0 16"
    val anderssonFen = "bq1rrbk1/3n1pp1/pp1ppn1p/8/2P1P3/2N1BP2/PP1NBQPP/2RR3K w - - 6 24"
    val engine =
      StaticSourceReviewEngine(
        Map(
          unzickerFen ->
            List(
              VariationLine(
                List("b2b4", "f6e8", "d2f1", "f7f6", "d1e2", "g7g6"),
                scoreCp = 28,
                depth = 16
              )
            ),
          anderssonFen ->
            List(
              VariationLine(
                List("a2a3", "a6a5", "d2f1", "d8c8", "f1g3", "a8b7"),
                scoreCp = 20,
                depth = 16
              )
            )
        )
      )

    val rows =
      SourceReview.observationsWithEngine(
        Some(engine),
        sourceIds = Set(
          "source-karpov-unzicker-1974-break-prevention",
          "source-karpov-andersson-1975-hedgehog-break-screen"
        )
      )

    assertEquals(rows.map(_.engineAgreement).distinct, List("top_pv_matches_played"), clues(rows))
    assert(
      rows.forall(row =>
        row.verdict == SourceReview.Verdict.RejectOwnerMissing ||
          row.verdict == SourceReview.Verdict.AdmitAuthorityRow
      ),
      clues(rows)
    )
    val admitted = rows.filter(_.verdict == SourceReview.Verdict.AdmitAuthorityRow)
    assert(admitted.size <= 1, clues(admitted))
    val byId = rows.map(row => row.source.id -> row).toMap
    val unzicker = byId("source-karpov-unzicker-1974-break-prevention")
    assertEquals(unzicker.admissionBlockers, "owner:break_prevention_capture_transform_recapture_unproven")
    assertEquals(unzicker.ownerFailureCodes, "break_prevention_capture_transform_recapture_unproven")
    val andersson = byId("source-karpov-andersson-1975-hedgehog-break-screen")
    assertEquals(andersson.admissionBlockers, "owner:break_prevention_no_prevented_plan")
    admitted.foreach { row =>
      assertEquals(row.mainProofSource, "counterplay_axis_suppression")
      assert(row.packetSummary.contains("proof_family=neutralize_key_break"), clues(row))
      val expectedText = PlayerFacingClaimPrefixKind.SupportedLocal.render(row.primary)
      assert(row.moveReview.contains(expectedText), clues(row))
      assert(row.chronicle.contains(expectedText.replace(" ...", "..").replace("...", "..")), clues(row))
    }
    rows.filterNot(row => admitted.exists(_.source.id == row.source.id)).foreach { row =>
      assert(row.admissionBlockers.startsWith("owner:break_prevention_"), clues(row))
    }
  }

  test("clean break-clamp source rows can reach neutralize-key-break owner packets") {
    val lokvencFen = "r1bqr1k1/p4pbp/np1p1np1/2pP4/4P3/2N2N2/PPQ1BPPP/R1B1R1K1 w - - 2 12"
    val madernaFen = "1rbqr1k1/1p1n1pbp/pn1p2p1/2pP4/P3PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 5 15"
    val camaraFen = "1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14"
    val sliwaFen = "1r1r3k/1p1q1pbp/pn1p2p1/2pP4/Pn2PP2/NQ4PP/1P3B2/3RRBK1 w - - 2 28"
    val pflegerFen = "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17"
    val polugaevskyFen = "rnbqnrk1/5ppp/pp1p1b2/2pP4/P3P3/2N5/1P1NBPPP/R1BQ1RK1 w - - 0 12"
    val engine =
      StaticSourceReviewEngine(
        Map(
          lokvencFen ->
            List(
              VariationLine(
                List(
                  "e2b5",
                  "a6b4",
                  "c2d1",
                  "c8d7",
                  "b5f1",
                  "b6b5",
                  "a2a3",
                  "b4a6",
                  "c1f4",
                  "d8b6",
                  "h2h3",
                  "c5c4",
                  "e4e5",
                  "d6e5",
                  "f3e5",
                  "d7f5"
                ),
                scoreCp = 37,
                depth = 16
              )
            ),
          madernaFen ->
            List(
              VariationLine(
                List("a4a5", "b6a8", "d2c4", "d7f8", "e4e5", "d6e5", "f4e5", "b7b5"),
                scoreCp = 82,
                depth = 16
              )
            ),
          camaraFen ->
            List(
              VariationLine(
                List(
                  "d3b5",
                  "b4a6",
                  "e4e5",
                  "a6c7",
                  "e5e6",
                  "f7e6",
                  "d5e6",
                  "c7e6",
                  "f4f5",
                  "g6f5",
                  "e2g3",
                  "a7a6",
                  "b5d7",
                  "c8d7",
                  "g3f5",
                  "e8f8",
                  "f5g7",
                  "f8f1",
                  "d1f1",
                  "e6g7"
                ),
                scoreCp = 20,
                depth = 16
              )
            ),
          sliwaFen ->
            List(
              VariationLine(
                List(
                  "a4a5",
                  "d7a4",
                  "b3f3",
                  "a4a5",
                  "e4e5",
                  "d6e5",
                  "f4e5",
                  "b4d5",
                  "h3h4",
                  "a5b4",
                  "h4h5",
                  "g6h5"
                ),
                scoreCp = 34,
                depth = 16
              )
            ),
          pflegerFen ->
            List(
              VariationLine(
                List(
                  "a4a5",
                  "b6d7",
                  "c3a4",
                  "f6g4",
                  "d2c3",
                  "g4e5",
                  "f3e5",
                  "g7e5",
                  "c3e5",
                  "d7e5",
                  "d3g3",
                  "d8g5",
                  "a4b6",
                  "g5g3",
                  "h2g3",
                  "a8d8",
                  "f2f4",
                  "e5d3",
                  "e1e3",
                  "d3b4",
                  "a1d1",
                  "b4c2"
                ),
                scoreCp = 20,
                depth = 16
              )
            ),
          polugaevskyFen ->
            List(
              VariationLine(
                List("d2c4", "b8d7", "f2f4", "a8b8", "c1e3", "b6b5", "a4b5", "a6b5"),
                scoreCp = 64,
                depth = 16
              )
            )
        )
      )
    val rows =
      SourceReview.observationsWithEngine(
        Some(engine),
        sourceIds = Set(
          "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
          "source-maderna-palermo-1955-a6-a5-break-prevention",
          "source-camara-bazan-1960-b7-b5-break-prevention",
          "source-sliwa-gromek-1960-a6-a5-break-prevention",
          "source-pfleger-maalouf-1961-a6-a5-break-prevention",
          "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention"
        )
      )

    assertEquals(rows.map(_.engineAgreement).distinct, List("top_pv_matches_played"), clues(rows))
    val byId = rows.map(row => row.source.id -> row).toMap
    val admittedIds =
      List(
        "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        "source-maderna-palermo-1955-a6-a5-break-prevention",
        "source-camara-bazan-1960-b7-b5-break-prevention",
        "source-sliwa-gromek-1960-a6-a5-break-prevention",
        "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention"
      )
    admittedIds.map(byId).foreach { row =>
      assertEquals(row.verdict, SourceReview.Verdict.AdmitAuthorityRow, clues(row))
      assertEquals(row.diagnosis, SourceReview.Diagnosis.AdmitReady, clues(row))
      assertEquals(row.admissionBlockers, "none", clues(row))
      assertEquals(row.mainProofSource, "counterplay_axis_suppression", clues(row))
      assert(row.packetSummary.contains("proof_family=neutralize_key_break"), clues(row))
      assertEquals(row.release, "SupportedLocal", clues(row))
      val expectedRendered = PlayerFacingClaimPrefixKind.SupportedLocal.render(row.primary)
      assert(row.moveReview.contains(expectedRendered), clues(row))
      assert(row.chronicle.contains(expectedRendered.replace(" ...", "..").replace("...", "..")), clues(row))
      assert(!row.primary.toLowerCase.contains("counterplay"), clues(row))
    }
    val pfleger = byId("source-pfleger-maalouf-1961-a6-a5-break-prevention")
    assertEquals(pfleger.verdict, SourceReview.Verdict.RejectOwnerMissing, clues(pfleger))
    assertEquals(pfleger.admissionBlockers, "proof:break_prevention_contract_mismatch", clues(pfleger))
    assertEquals(pfleger.mainProofSource, "color_complex_squeeze_probe", clues(pfleger))
  }

  test("central-break timing review exposes one exact Maderna row and one plan-only prep row") {
    val exactFen = "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
    val semanticScreenFen = "nrbqr1k1/1p1n1pbp/p2p2p1/P1pP4/4PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 1 16"
    val engine =
      StaticSourceReviewEngine(
        Map(
          exactFen ->
            List(
              VariationLine(
                List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"),
                scoreCp = 82,
                depth = 16
              ),
              VariationLine(
                List("c1e3", "b7b5", "a5b6"),
                scoreCp = 36,
                depth = 16
              )
            ),
          semanticScreenFen ->
            List(
              VariationLine(
                List("d2c4", "d7f8", "e4e5", "d6e5", "f4e5"),
                scoreCp = 82,
                depth = 16
              ),
              VariationLine(
                List("b2b3", "d7f8", "e4e5"),
                scoreCp = 44,
                depth = 16
              )
            )
        )
      )
    val rows =
      SourceReview.observationsWithEngine(
        Some(engine),
        sourceIds = Set(
          "source-maderna-palermo-1955-central-break-timing",
          "source-maderna-palermo-1955-central-break-prep-review"
        )
      )
    val byId = rows.map(row => row.source.id -> row).toMap

    val exact = byId("source-maderna-palermo-1955-central-break-timing")
    assertEquals(exact.verdict, SourceReview.Verdict.AdmitAuthorityRow)
    assertEquals(exact.diagnosis, SourceReview.Diagnosis.AdmitReady)
    assertEquals(exact.admissionBlockers, "none")
    assertEquals(exact.release, "SupportedLocal")
    assertEquals(exact.mainProofSource, PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    assert(exact.packetSummary.contains(s"proof_family=${PlanTaxonomy.PlanKind.CentralBreakTiming.id}"), clues(exact))
    assertEquals(exact.taxonomy, "source_central_break_timing")

    val prep = byId("source-maderna-palermo-1955-central-break-prep-review")
    assertEquals(prep.verdict, SourceReview.Verdict.RejectOwnerMissing)
    assertEquals(prep.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap)
    assertEquals(prep.release, "-")
    assertEquals(prep.taxonomy, "source_central_break_timing")
    assertEquals(prep.admissionBlockers, "owner:central_break_timing_witness_missing")
  }

  test("natural SupportedLocal source rows admit with top or near-top engine authority") {
    val evansFen = "r1b1rnk1/pp2qppp/2p5/3p4/3Pn3/2NBPN2/PPQ2PPP/1R3RK1 w - - 0 13"
    val evansIqpFen = "r3rnk1/1p3ppp/p1p5/3p2q1/PP1P2b1/2QBP3/3N1PPP/1R3RK1 w - - 3 17"
    val capablancaFen = "r3r1k1/pp3pn1/2pq2pp/3p4/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 0 23"
    val originalBotvinnikFen = "r1bq1rk1/pp1nbppp/4pn2/6B1/2BP4/2N2N2/PP3PPP/R2Q1RK1 b - - 0 10"
    val botvinnikFen = "r2q1rk1/pp2bppp/4pn2/3bN1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 w - - 11 16"
    val carlsenFen = "r1bqk2r/1p1p1ppp/p1n1pn2/8/1bPNP3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 5 8"
    val alekhineFen = "rnb1k2r/pp3ppp/4p3/2pqP3/PbpPn3/2N2N2/1PQ1BPPP/R1B2RK1 b kq - 1 10"
    val najdorfFen = "r1b2rk1/pp2qppp/4p3/2nn4/3N4/2N1P3/PPQ2PPP/3RKB1R w K - 0 12"
    val salovSimplificationFen = "7k/p4qp1/8/1Q1pR3/3P1P2/2r3P1/7P/6K1 w - - 0 36"
    val boleslavskyStaticWeaknessFen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 6 10"
    val aronianDefenderTradeFen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    val badPieceLiquidationFen = "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1"
    val engine =
      StaticSourceReviewEngine(
        Map(
          evansFen ->
            List(
              VariationLine(
                List("b2b4", "a7a6", "a2a4", "e4c3", "c2c3", "f8g6", "b4b5", "c6b5", "a4b5", "c8g4", "b5a6", "b7a6", "f3d2", "g6h4", "c3c5", "e7g5"),
                scoreCp = 20,
                depth = 16
              )
            ),
          evansIqpFen ->
            List(
              VariationLine(
                List("f1c1", "h7h5", "b4b5", "c6b5", "a4b5", "a6a5", "d3f1", "h5h4", "h2h3", "g4h3", "d2f3", "g5f6", "g2h3", "f6f3", "f1g2", "f3f5"),
                scoreCp = 20,
                depth = 16
              )
            ),
          capablancaFen ->
            List(
              VariationLine(
                List("b4b5", "e8c8", "b5c6", "b7b6", "d3a6", "c8c6", "a4b2", "c6c7", "b2d3", "g7e8", "c1c7", "d6c7", "b1c1", "c7e7", "d3f4"),
                scoreCp = 20,
                depth = 16
              )
            ),
          originalBotvinnikFen ->
            List(
              VariationLine(
                List("a7a6", "a2a4", "h7h6", "g5h4", "b7b6", "d4d5", "e6d5", "c3d5", "c8b7", "d5e7", "d8e7", "h4g3", "f8e8", "d1b3", "b7f3", "b3f3"),
                scoreCp = 18,
                depth = 16
              ),
              VariationLine(
                List("d7b6", "c4b3", "a7a6"),
                scoreCp = 10,
                depth = 16
              )
            ),
          botvinnikFen ->
            List(
              VariationLine(
                List("b3a4", "d5c6", "e5c6", "b7c6", "f2f4", "f6d5", "a2a3", "d5c3", "b2c3", "e7g5", "f4g5", "b4d5", "a4c6"),
                scoreCp = 20,
                depth = 16
              ),
              VariationLine(
                List("c3d5", "f6d5", "g5e7", "d8e7", "f2f4", "f7f6"),
                scoreCp = 12,
                depth = 16
              )
            ),
          carlsenFen ->
            List(
              VariationLine(
                List("d4c6", "d7c6", "d3d8", "e8d8", "e4e5", "f6d7", "c1f4", "b7b5", "e1c1", "d8c7", "c3e4", "d7b6", "a2a3", "b4e7", "e4d6", "b5c4"),
                scoreCp = 20,
                depth = 16
              )
            ),
          alekhineFen ->
            List(
              VariationLine(
                List("e4c3", "b2c3", "c5d4", "c3b4", "d4d3", "c2a2", "d3e2", "a2e2", "d5d3", "e2b2", "b7b5", "a1a3", "d3e4", "a4b5", "c8b7", "f1d1", "b8d7", "h2h3", "b7d5"),
                scoreCp = 20,
                depth = 16
              )
            ),
          najdorfFen ->
            List(
              VariationLine(
                List("c3d5", "e6d5", "f1e2", "b7b6", "e1g1", "c8b7", "d1c1", "f8c8", "h2h3", "g7g6", "e2f3", "c5e6", "c2d2"),
                scoreCp = 20,
                depth = 16
              )
            ),
          salovSimplificationFen ->
            List(
              VariationLine(
                List("b5d5", "f7d5", "e5d5", "h8h7", "g1g2", "c3c2", "g2h3", "c2d2", "d5d7", "a7a5", "d7a7", "d2d4", "a7a5", "d4d2", "a5h5", "h7g8", "h3g4", "d2b2", "h2h4", "b2b8", "h5d5"),
                scoreCp = 388,
                depth = 16
              )
            ),
          boleslavskyStaticWeaknessFen ->
            List(
              VariationLine(
                List(
                  "f3d2",
                  "b8a6",
                  "g1h1",
                  "a6c7",
                  "a2a4",
                  "b7b6",
                  "f2f3",
                  "d8e7",
                  "d2c4",
                  "c8a6",
                  "c1g5",
                  "a6c4",
                  "e2c4",
                  "h7h6",
                  "g5h4",
                  "a7a6",
                  "f3f4",
                  "b6b5",
                  "a4b5",
                  "a6b5",
                  "a1a8",
                  "e8a8"
                ),
                scoreCp = 119,
                depth = 16
              )
            ),
          aronianDefenderTradeFen ->
            List(
              VariationLine(
                List("c2b1", "f8c5", "c1a3", "c5d4", "b3d3", "e2g4", "f1c1", "h8e8"),
                scoreCp = 58,
                depth = 16
              ),
              VariationLine(
                List(
                  "c1a3",
                  "f8a3",
                  "b3a3",
                  "e2c4",
                  "c2c4",
                  "b6c4",
                  "a3a7",
                  "h8e8",
                  "a2a4",
                  "c4d2",
                  "f1d1",
                  "d2b3",
                  "a4a5",
                  "b3c5",
                  "g3g4",
                  "h7h6",
                  "h2h4",
                  "e8e6"
                ),
                scoreCp = 44,
                depth = 16
              )
            ),
          badPieceLiquidationFen ->
            List(
              VariationLine(
                List("c1a3", "e7f7", "a3f8", "f7f8"),
                scoreCp = 38,
                depth = 18
              )
            )
        )
      )
    val observations =
      SourceReview.observationsWithEngine(
        Some(engine),
        sourceIds = Set(
          "source-capablanca-golombek-1939-iqp-inducement",
          "source-evans-opsahl-1950-iqp-inducement",
          "source-alekhine-bogoljubow-1936-iqp-inducement",
          "source-najdorf-sergeant-1939-iqp-inducement",
          "source-botvinnik-vidmar-1936-iqp-multipv-screen",
          "source-botvinnik-vidmar-1936",
          "source-evans-opsahl-1950",
          "source-carlsen-anand-2014-g6",
          "source-salov-ljubojevic-1992-simplification-window",
          "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
          "source-aronian-andreikin-2014-defender-trade",
          "source-bad-piece-liquidation-pilot"
        )
      )
    val byId = observations.map(obs => obs.source.id -> obs).toMap

    val capablanca = byId("source-capablanca-golombek-1939-iqp-inducement")
    assertEquals(capablanca.verdict, SourceReview.Verdict.AdmitAuthorityRow)
    assertEquals(capablanca.diagnosis, SourceReview.Diagnosis.AdmitReady)
    assertEquals(capablanca.admissionBlockers, "none")
    assertEquals(capablanca.engineAgreement, "top_pv_matches_played")
    assertEquals(capablanca.mainProofSource, PlayerFacingTruthModePolicy.IQPInducementProbeProofSource)
    assertEquals(capablanca.mainClaimScope, "MoveLocal")
    assertEquals(capablanca.contractId, s"subplan:${PlanTaxonomy.PlanKind.IQPInducement.id}")
    assertEquals(capablanca.release, "SupportedLocal")
    assertEquals(capablanca.primary, "This sequence leaves an isolated pawn as the local target.")

    List(
      "source-evans-opsahl-1950-iqp-inducement",
      "source-alekhine-bogoljubow-1936-iqp-inducement",
      "source-najdorf-sergeant-1939-iqp-inducement"
    ).foreach { id =>
      val row = byId(id)
      assertEquals(row.verdict, SourceReview.Verdict.AdmitAuthorityRow)
      assertEquals(row.diagnosis, SourceReview.Diagnosis.AdmitReady)
      assertEquals(row.admissionBlockers, "none")
      assertEquals(row.engineAgreement, "top_pv_matches_played")
      assertEquals(row.mainProofSource, PlayerFacingTruthModePolicy.IQPInducementProbeProofSource)
      assertEquals(row.mainClaimScope, "MoveLocal")
      assertEquals(row.contractId, s"subplan:${PlanTaxonomy.PlanKind.IQPInducement.id}")
      assertEquals(row.release, "SupportedLocal")
      assertEquals(row.primary, "This sequence leaves an isolated pawn as the local target.")
    }

    val botvinnikScreen = byId("source-botvinnik-vidmar-1936-iqp-multipv-screen")
    assertEquals(botvinnikScreen.verdict, SourceReview.Verdict.RejectOwnerMissing)
    assertEquals(botvinnikScreen.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap)
    assertEquals(botvinnikScreen.engineAgreement, "near_top_multipv_contains_played_top=b3a4_gap=8cp")
    assertEquals(botvinnikScreen.admissionBlockers, "owner:iqp_not_induced_or_side_mismatch")
    assertEquals(botvinnikScreen.ownerFailureCodes, "-")
    assertEquals(botvinnikScreen.release, "CertifiedOwner")

    val originalBotvinnik = byId("source-botvinnik-vidmar-1936")
    assertEquals(originalBotvinnik.verdict, SourceReview.Verdict.RejectOwnerMissing)
    assert(originalBotvinnik.admissionBlockers.contains("owner:iqp_not_induced"), clues(originalBotvinnik))

    val evansCarlsbad = byId("source-evans-opsahl-1950")
    assertEquals(evansCarlsbad.verdict, SourceReview.Verdict.RejectOwnerMissing, clues(evansCarlsbad))
    assertEquals(evansCarlsbad.diagnosis, SourceReview.Diagnosis.PlannerOwnerSuppressed, clues(evansCarlsbad))
    assertEquals(evansCarlsbad.admissionBlockers, "owner:planner_suppressed", clues(evansCarlsbad))
    assert(evansCarlsbad.packetSummary.contains("proof_family=backward_pawn_targeting"), clues(evansCarlsbad))
    val carlsenQueenTrade = byId("source-carlsen-anand-2014-g6")
    assertEquals(carlsenQueenTrade.verdict, SourceReview.Verdict.RejectOwnerMissing, clues(carlsenQueenTrade))
    assertEquals(carlsenQueenTrade.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap, clues(carlsenQueenTrade))
    assertEquals(carlsenQueenTrade.admissionBlockers, "owner:root_vocabulary_or_extraction_gap", clues(carlsenQueenTrade))

    val salovSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    assertEquals(salovSimplification.verdict, SourceReview.Verdict.RejectOwnerMissing)
    assertEquals(salovSimplification.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap)
    assertEquals(salovSimplification.admissionBlockers, "owner:root_vocabulary_or_extraction_gap")
    assertEquals(salovSimplification.engineAgreement, "top_pv_matches_played")
    assertEquals(salovSimplification.mainProofSource, "-")
    assertEquals(salovSimplification.contractId, "-")
    assertEquals(salovSimplification.contractStatus, "-")
    assertEquals(salovSimplification.release, "-")
    assertEquals(salovSimplification.taxonomy, "source_simplification_window")
    assertEquals(salovSimplification.primary, "-")

    val boleslavskyStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    assertEquals(boleslavskyStaticWeakness.verdict, SourceReview.Verdict.AdmitAuthorityRow)
    assertEquals(boleslavskyStaticWeakness.diagnosis, SourceReview.Diagnosis.AdmitReady)
    assertEquals(boleslavskyStaticWeakness.admissionBlockers, "none")
    assertEquals(boleslavskyStaticWeakness.engineAgreement, "top_pv_matches_played")
    assertEquals(boleslavskyStaticWeakness.mainProofSource, "exact_target_fixation")
    assertEquals(boleslavskyStaticWeakness.contractId, "subplan:static_weakness_fixation")
    assertEquals(boleslavskyStaticWeakness.contractStatus, "Releasable")
    assertEquals(boleslavskyStaticWeakness.release, "CertifiedOwner")
    assertEquals(boleslavskyStaticWeakness.taxonomy, "source_static_weakness_fixation")

    val aronianDefenderTrade = byId("source-aronian-andreikin-2014-defender-trade")
    assertEquals(aronianDefenderTrade.verdict, SourceReview.Verdict.RejectOwnerMissing, clues(aronianDefenderTrade))
    assertEquals(aronianDefenderTrade.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap)
    assertEquals(aronianDefenderTrade.engineAgreement, "near_top_multipv_contains_played_top=c2b1_gap=14cp")
    assert(aronianDefenderTrade.admissionBlockers.contains("owner:defender_trade_owner_missing"))
    assertEquals(aronianDefenderTrade.mainProofSource, "-")
    assertEquals(aronianDefenderTrade.mainClaimScope, "-")
    assertEquals(aronianDefenderTrade.contractId, "-")
    assertEquals(aronianDefenderTrade.contractStatus, "-")
    assertEquals(aronianDefenderTrade.release, "-")
    assertEquals(aronianDefenderTrade.taxonomy, "source_defender_trade")

    val badPieceLiquidation = byId("source-bad-piece-liquidation-pilot")
    assertEquals(badPieceLiquidation.verdict, SourceReview.Verdict.AdmitAuthorityRow, clues(badPieceLiquidation.tsv))
    assertEquals(badPieceLiquidation.diagnosis, SourceReview.Diagnosis.AdmitReady)
    assertEquals(badPieceLiquidation.admissionBlockers, "none")
    assertEquals(badPieceLiquidation.engineAgreement, "top_pv_matches_played")
    assertEquals(badPieceLiquidation.mainProofSource, PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource)
    assertEquals(badPieceLiquidation.mainClaimScope, "MoveLocal")
    assertEquals(badPieceLiquidation.contractId, s"subplan:${PlanTaxonomy.PlanKind.BadPieceLiquidation.id}")
    assertEquals(badPieceLiquidation.contractStatus, "Releasable")
    assertEquals(badPieceLiquidation.release, "SupportedLocal")
    assertEquals(
      badPieceLiquidation.primary,
      "This trade clears the bad piece from the local branch."
    )
    val expectedBadPieceSurface = PlayerFacingClaimPrefixKind.SupportedLocal.render(badPieceLiquidation.primary)
    assertEquals(badPieceLiquidation.moveReview, expectedBadPieceSurface)
    assertEquals(badPieceLiquidation.chronicle, expectedBadPieceSurface)
    assertEquals(badPieceLiquidation.taxonomy, "source_bad_piece_liquidation")
  }

  test("break-prevention window scan does not admit incidental IQP rows") {
    val anderssonBreakFen = "bq1rrbk1/3n1pp1/pp1ppn1p/8/2P1P3/P1N1BP2/1P1NBQPP/2RR3K b - - 0 24"
    val engine =
      StaticSourceReviewEngine(
        Map(
          anderssonBreakFen ->
            List(
              VariationLine(
                List("a8b7", "b2b4", "d6d5", "e4d5", "e6d5", "c4d5", "b6b5", "d2f1"),
                scoreCp = 20,
                depth = 16
              ),
              VariationLine(
                List("d6d5", "e4d5", "e6d5", "c4d5", "b6b5", "d2f1", "d7e5", "e3b6"),
                scoreCp = 12,
                depth = 16
              )
            )
        )
      )
    val windows =
      SourceReview.windowObservationsWithEngine(
        Some(engine),
        sourceIds = Set("source-karpov-andersson-1975-hedgehog-break-screen")
      )
    val row =
      windows
        .find(_.ply.contains(48))
        .getOrElse(fail(s"missing Andersson break-window ply 48: ${windows.map(obs => obs.ply -> obs.engineAgreement)}"))

    assertEquals(row.engineAgreement, "near_top_multipv_contains_played_top=a8b7_gap=8cp")
    assertEquals(row.release, "SupportedLocal")
    assertEquals(row.mainProofSource, PlayerFacingTruthModePolicy.IQPInducementProbeProofSource)
    assertEquals(row.verdict, SourceReview.Verdict.RejectOwnerMissing)
    assertEquals(row.diagnosis, SourceReview.Diagnosis.RootVocabularyOrExtractionGap)
    assertEquals(row.admissionBlockers, "proof:break_prevention_contract_mismatch")
  }

  test("window probe scans every ply in a candidate range instead of collapsing to the head ply") {
    val capablanca =
      SourceWitnessCatalog.all
        .find(_.id == "source-capablanca-golombek-1939")
        .getOrElse(fail("missing Capablanca source row"))
    val probes =
      SourceReview.windowObservations(
        engine = None,
        sourceIds = Set(capablanca.id)
      )

    assertEquals(probes.size, 15)
    assertEquals(probes.flatMap(_.ply).minOption, Some(33))
    assertEquals(probes.flatMap(_.ply).maxOption, Some(47))
    assert(probes.forall(_.source.id == capablanca.id), clues(probes.map(_.source.id).distinct))
    assert(probes.exists(_.playedUci.contains("b2b4")), clues(probes.map(obs => obs.ply -> obs.playedUci)))
    assert(
      probes.forall(_.diagnosis == SourceReview.Diagnosis.EngineMissingBeforeAdmission),
      clues(probes.map(obs => obs.ply -> obs.diagnosis))
    )
  }

  test("window probe markdown groups scanned plies by source and blocker taxonomy") {
    val probes =
      SourceReview.windowObservations(
        engine = None,
        sourceIds = Set("source-capablanca-golombek-1939")
      )
    val report = SourceReview.windowMarkdown(probes)

    assert(report.contains("source-capablanca-golombek-1939: scanned=15"), clues(report))
    assert(report.contains("diagnostics=engine_missing_before_admission=15"), clues(report))
    assert(report.contains("blockers=engine:missing=15"), clues(report))
    assert(report.contains("Acceptance rule: only verdict=admit_authority_row is source acceptance"), clues(report))
  }
