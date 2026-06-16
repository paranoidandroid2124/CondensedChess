package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite

class AuthoritySurfaceLedgerTest extends FunSuite:

  override val munitTimeout = scala.concurrent.duration.Duration(90, "s")

  private val sourceIqpInducementIds =
    List(
      "source-capablanca-golombek-1939-iqp-inducement",
      "source-evans-opsahl-1950-iqp-inducement",
      "source-karpov-andersson-1975-iqp-inducement",
      "source-alekhine-bogoljubow-1936-iqp-inducement",
      "source-najdorf-sergeant-1939-iqp-inducement",
      "source-botvinnik-vidmar-1936-iqp-opening-inducement"
    )
  private val sourceFlankClampIds =
    List(
      "source-botvinnik-vidmar-1936-flank-clamp",
      "source-botvinnik-vidmar-1936-e4-color-complex-squeeze",
      "source-camara-bazan-1960-d5-color-complex-squeeze",
      "source-pfleger-maalouf-1961-d5-color-complex-squeeze"
    )
  private val sourceCentralBreakTimingId =
    "source-maderna-palermo-1955-central-break-timing"
  private val sourceBadPieceLiquidationId =
    "source-capablanca-golombek-1939-bad-piece-liquidation"
  private val sourceQueenTradeBoundaryIds =
    List(
      "source-carlsen-anand-2014-g6",
      "source-carlsen-anand-2014-g6-queen-trade-completion"
    )
  private val suppressedSourceBreakPreventionIds =
    List(
      "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
      "source-maderna-palermo-1955-a6-a5-break-prevention",
      "source-camara-bazan-1960-b7-b5-break-prevention",
      "source-sliwa-gromek-1960-a6-a5-break-prevention",
      "source-pfleger-maalouf-1961-a6-a5-break-prevention",
      "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention"
    )
  private val sourceBreakPreventionFixtureIds =
    suppressedSourceBreakPreventionIds
  private val expectedSourceSurfaceFixtureIds =
    List(
      "source-evans-opsahl-1950"
    ) ++ sourceQueenTradeBoundaryIds ++ sourceIqpInducementIds ++ sourceFlankClampIds ++ List(sourceBadPieceLiquidationId) ++ List(
      "source-bad-piece-liquidation-pilot"
    ) ++ sourceBreakPreventionFixtureIds.take(2) ++ List(sourceCentralBreakTimingId) ++
      sourceBreakPreventionFixtureIds.drop(2) ++ List(
        "source-botvinnik-vidmar-1936-simplification-window",
        "source-salov-ljubojevic-1992-simplification-window",
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation"
      )
  private val expectedSourceReleases =
    Map(
      "source-evans-opsahl-1950" -> "Suppressed",
      "source-bad-piece-liquidation-pilot" -> "SupportedLocal",
      sourceBadPieceLiquidationId -> "SupportedLocal",
      sourceCentralBreakTimingId -> "SupportedLocal",
      "source-botvinnik-vidmar-1936-simplification-window" -> "SupportedLocal",
      "source-salov-ljubojevic-1992-simplification-window" -> "Suppressed",
      "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation" -> "Suppressed"
    ) ++
      sourceQueenTradeBoundaryIds.map(_ -> "SupportedLocal") ++
      sourceIqpInducementIds.map(_ -> "Suppressed") ++
      sourceFlankClampIds.map(_ -> "Suppressed") ++
      suppressedSourceBreakPreventionIds.map(_ -> "Suppressed")

  test("B/C authority surface ledger covers natural rows, soft rows, and tactical-veto parity") {
    val observations = AuthoritySurfaceLedger.observations()
    val byId = observations.map(obs => obs.sample.id -> obs).toMap

    val naturalB15A = byId("natural-B15A")
    assertEquals(naturalB15A.release, "Suppressed", clues(naturalB15A))
    assert(naturalB15A.rejected.contains("missing_move_owner"), clues(naturalB15A))
    assert(naturalB15A.rejected.contains("position_probe_missing"), clues(naturalB15A))
    val naturalK09B = byId("natural-K09B")
    assertEquals(naturalK09B.release, "SupportedLocal")
    assert(naturalK09B.contractId.contains(PlanTaxonomy.PlanKind.SimplificationWindow.id), clues(naturalK09B))
    assertEquals(naturalK09B.contractStatus, "Releasable")
    assertEquals(naturalK09B.contractFailures, "none")
    val naturalK09F = byId("natural-K09F")
    assertEquals(naturalK09F.release, "SupportedLocal")
    assert(naturalK09F.contractId.contains(PlanTaxonomy.PlanKind.SimplificationWindow.id), clues(naturalK09F))
    assertEquals(naturalK09F.contractStatus, "Releasable")
    assertEquals(naturalK09F.contractFailures, "none")
    List("K09A-certified-coordination", "K09D-certified-coordination").foreach { id =>
      val coordination = byId(id)
      assertEquals(coordination.release, "CertifiedOwner", clues(coordination))
      assertEquals(coordination.contractId, "runtime:target_focused_coordination")
      assertEquals(coordination.contractStatus, "Releasable")
      assertEquals(coordination.contractFailures, "none")
      assert(coordination.plannerOwner.contains("WhatMattersHere:PositionProbe:target_focused_coordination_probe"), clues(coordination))
      assertEquals(coordination.primary, "the pressure is coordinated on c6.")
      assert(coordination.moveReview.contains("keep the pressure coordinated on c6"), clues(coordination))
    }
    val publicRows =
      observations.filter(obs => obs.release == "SupportedLocal" || obs.release == "CertifiedOwner")
    assert(publicRows.forall(_.contractStatus == "Releasable"), clues(publicRows.filter(_.contractStatus != "Releasable")))
    assert(publicRows.forall(_.contractFailures == "none"), clues(publicRows.filter(_.contractFailures != "none")))
    assertEquals(byId("natural-K03A").release, "Suppressed")
    assertEquals(byId("natural-K08A").release, "Suppressed")
    assertEquals(byId("natural-K09E").release, "Suppressed")
    assertEquals(byId("natural-D01A").release, "Suppressed")
    assertEquals(byId("natural-D01B").release, "Suppressed", clues(byId("natural-D01B")))
    assertEquals(byId("natural-MI2").release, "Suppressed")
    assertEquals(byId("natural-MI3").release, "Suppressed")

    val soft = byId("B15A-supported-local-soft")
    assertEquals(soft.release, "Suppressed", clues(soft))
    assertEquals(soft.primary, "-")
    assertEquals(soft.plannerOwner, "-")
    assert(soft.rejected.contains("missing_move_owner"), clues(soft))
    assert(soft.rejected.contains("position_probe_missing"), clues(soft))
    assert(!soft.leak, clues(soft))

    val cSoft = byId("K09B-supported-local-soft")
    assertEquals(cSoft.release, "Suppressed")
    assertEquals(cSoft.primary, "-")
    assertEquals(cSoft.moveReview, "-")
    assertEquals(cSoft.plannerOwner, "-")
    assert(cSoft.contractFailures.contains("witness:branch_not_proven"), clues(cSoft))
    assert(cSoft.contractFailures.contains("witness:persistence_not_stable"), clues(cSoft))

    val softVeto = byId("B15A-supported-local-veto")
    assertEquals(softVeto.release, "Suppressed")
    assertEquals(softVeto.primary, "-")
    assert(softVeto.rejected.contains("missing_move_owner"), clues(softVeto))
    assert(softVeto.rejected.contains("position_probe_missing"), clues(softVeto))
    assert(!softVeto.leak, clues(softVeto))

    val exactSurface = byId("B15A-certified-carlsbad")
    assertEquals(exactSurface.release, "Suppressed", clues(exactSurface))
    assert(exactSurface.rejected.contains("missing_move_owner"), clues(exactSurface))
    assert(exactSurface.rejected.contains("position_probe_missing"), clues(exactSurface))
    assert(!exactSurface.moveReview.contains("So the task is"), clues(exactSurface))

    val sourceB = byId("source-evans-opsahl-1950")
    assertEquals(sourceB.release, "Suppressed", clues(sourceB))
    assertEquals(sourceB.plannerOwner, "-", clues(sourceB))
    assertEquals(sourceB.primary, "-", clues(sourceB))
    assertEquals(sourceB.moveReview, "-", clues(sourceB))
    assert(sourceB.rejected.contains("position_probe_missing"), clues(sourceB))
    assertEquals(sourceB.contractStatus, "-")
    assertEquals(sourceB.contractId, "-")

    val sourceC = byId("source-carlsen-anand-2014-g6")
    assertEquals(sourceC.release, "SupportedLocal", clues(sourceC))
    assertEquals(sourceC.primary, "This exchange moves the game into the queenless branch.")
    assert(sourceC.moveReview.contains("A key idea is that this exchange moves the game into the queenless branch."), clues(sourceC))
    assert(sourceC.plannerOwner.contains("WhyThis:MoveDelta:queen_trade_shield"), clues(sourceC))
    assertEquals(sourceC.contractStatus, "Releasable")
    assertEquals(sourceC.contractId, s"subplan:${PlanTaxonomy.PlanKind.QueenTradeShield.id}")
    assertEquals(sourceC.contractFailures, "none")
    assert(!sourceC.moveReview.contains("So the task is"), clues(sourceC))
    val sourceQueenTradeCompletion = byId("source-carlsen-anand-2014-g6-queen-trade-completion")
    assertEquals(sourceQueenTradeCompletion.release, "SupportedLocal", clues(sourceQueenTradeCompletion))
    assertEquals(sourceQueenTradeCompletion.primary, "This exchange moves the game into the queenless branch.")
    assert(sourceQueenTradeCompletion.moveReview.contains("9. Qxd8+"), clues(sourceQueenTradeCompletion))
    assert(sourceQueenTradeCompletion.plannerOwner.contains("WhyThis:MoveDelta:queen_trade_shield"), clues(sourceQueenTradeCompletion))
    assertEquals(sourceQueenTradeCompletion.contractStatus, "Releasable")
    assertEquals(sourceQueenTradeCompletion.contractId, s"subplan:${PlanTaxonomy.PlanKind.QueenTradeShield.id}")
    assertEquals(sourceQueenTradeCompletion.contractFailures, "none")
    assertEquals(sourceQueenTradeCompletion.taxonomy, "source_queen_trade_boundary")

    sourceIqpInducementIds.foreach { id =>
      val naturalIqp = byId(id)
      assertEquals(naturalIqp.release, "Suppressed", clues(naturalIqp))
      assertEquals(naturalIqp.primary, "-", clues(naturalIqp))
      assertEquals(naturalIqp.moveReview, "-", clues(naturalIqp))
      assertEquals(naturalIqp.plannerOwner, "-", clues(naturalIqp))
      assertEquals(naturalIqp.taxonomy, "source_iqp_inducement")
    }

    sourceFlankClampIds.foreach { id =>
      val flankClamp = byId(id)
      assertEquals(flankClamp.release, "Suppressed", clues(flankClamp))
      assertEquals(flankClamp.primary, "-", clues(flankClamp))
      assertEquals(flankClamp.moveReview, "-", clues(flankClamp))
      assertEquals(flankClamp.plannerOwner, "-", clues(flankClamp))
      assertEquals(flankClamp.taxonomy, "source_flank_clamp")
    }

    val capablancaBadPiece = byId(sourceBadPieceLiquidationId)
    assertEquals(capablancaBadPiece.release, "SupportedLocal", clues(capablancaBadPiece))
    assertEquals(capablancaBadPiece.primary, "This trade clears the bad piece from the local branch.")
    assert(capablancaBadPiece.moveReview.contains("22. Bxd6"), clues(capablancaBadPiece))
    assert(capablancaBadPiece.plannerOwner.contains("WhyThis:MoveDelta:bad_piece_liquidation"), clues(capablancaBadPiece))
    assertEquals(capablancaBadPiece.contractStatus, "Releasable")
    assertEquals(capablancaBadPiece.contractId, s"subplan:${PlanTaxonomy.PlanKind.BadPieceLiquidation.id}")
    assertEquals(capablancaBadPiece.contractFailures, "none")
    assertEquals(capablancaBadPiece.taxonomy, "source_bad_piece_liquidation")
    assert(!capablancaBadPiece.moveReview.contains("So the task is"), clues(capablancaBadPiece))

    val badPieceLiquidation = byId("source-bad-piece-liquidation-pilot")
    assertEquals(badPieceLiquidation.release, "SupportedLocal")
    assertEquals(badPieceLiquidation.primary, "This trade clears the bad piece from the local branch.")
    assert(badPieceLiquidation.moveReview.contains("1. Ba3"), clues(badPieceLiquidation))
    assert(badPieceLiquidation.plannerOwner.contains("WhyThis:MoveDelta:bad_piece_liquidation"), clues(badPieceLiquidation))
    assertEquals(badPieceLiquidation.contractStatus, "Releasable")
    assert(badPieceLiquidation.contractId.contains(PlanTaxonomy.PlanKind.BadPieceLiquidation.id), clues(badPieceLiquidation))
    assertEquals(badPieceLiquidation.contractFailures, "none")
    assertEquals(badPieceLiquidation.taxonomy, "source_bad_piece_liquidation")

    val centralBreakTiming = byId(sourceCentralBreakTimingId)
    assertEquals(centralBreakTiming.release, "SupportedLocal", clues(centralBreakTiming))
    assertEquals(centralBreakTiming.primary, "This also plays the e4-e5 break at this moment.")
    assert(centralBreakTiming.moveReview.contains("e4-e5 break"), clues(centralBreakTiming))
    assert(centralBreakTiming.plannerOwner.contains("WhyThis:MoveDelta:central_break_timing"), clues(centralBreakTiming))
    assertEquals(centralBreakTiming.contractStatus, "Releasable")
    assert(centralBreakTiming.contractId.contains(PlanTaxonomy.PlanKind.CentralBreakTiming.id), clues(centralBreakTiming))
    assertEquals(centralBreakTiming.contractFailures, "none")
    assertEquals(centralBreakTiming.taxonomy, "source_central_break_timing")

    suppressedSourceBreakPreventionIds.foreach { id =>
      val row = byId(id)
      assertEquals(row.release, "Suppressed", clues(row))
      assertEquals(row.primary, "-", clues(row))
      assertEquals(row.plannerOwner, "-", clues(row))
      assertEquals(row.taxonomy, "source_break_prevention")
    }

    val sourceSimplification = byId("source-botvinnik-vidmar-1936-simplification-window")
    assertEquals(sourceSimplification.release, "SupportedLocal")
    assertEquals(sourceSimplification.primary, "This trade keeps the same local edge on d5.")
    assert(sourceSimplification.plannerOwner.contains("MoveDelta:target"), clues(sourceSimplification))
    assertEquals(sourceSimplification.contractStatus, "Releasable")
    assertEquals(sourceSimplification.contractId, s"subplan:${PlanTaxonomy.PlanKind.SimplificationWindow.id}")
    assertEquals(sourceSimplification.taxonomy, "source_simplification_window")

    val sourceSuppressedSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    assertEquals(sourceSuppressedSimplification.release, "Suppressed")
    assertEquals(sourceSuppressedSimplification.primary, "-")
    assertEquals(sourceSuppressedSimplification.plannerOwner, "-")
    assertEquals(sourceSuppressedSimplification.contractStatus, "-")
    assertEquals(sourceSuppressedSimplification.contractId, "-")
    assertEquals(sourceSuppressedSimplification.taxonomy, "source_simplification_window")

    val sourceStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    assertEquals(sourceStaticWeakness.release, "Suppressed")
    assertEquals(sourceStaticWeakness.primary, "-")
    assertEquals(sourceStaticWeakness.plannerOwner, "-")
    assert(sourceStaticWeakness.rejected.contains("strategic_claim_tactical_veto"), clues(sourceStaticWeakness))
    assertEquals(sourceStaticWeakness.contractStatus, "Releasable")
    assert(sourceStaticWeakness.contractId.contains("static_weakness_fixation"), clues(sourceStaticWeakness))
    assertEquals(sourceStaticWeakness.taxonomy, "source_static_weakness_fixation")

    assert(
      !byId.contains("source-aronian-andreikin-2014-defender-trade"),
      clues(byId.keySet.toList.sorted)
    )

    val iqpControl = byId("iqp-supported-local-control")
    assertEquals(iqpControl.release, "Suppressed")
    assertEquals(iqpControl.primary, "-")
    assertEquals(iqpControl.plannerOwner, "-")
    assertEquals(iqpControl.contractStatus, "-")
    assertEquals(iqpControl.contractId, "-")

    val breakPreventionControl = byId("break-prevention-supported-local-control")
    assertEquals(breakPreventionControl.release, "Suppressed")
    assertEquals(breakPreventionControl.primary, "-")
    assertEquals(breakPreventionControl.plannerOwner, "-")
    assertEquals(breakPreventionControl.contractStatus, "-")
    assertEquals(breakPreventionControl.contractId, "-")
    assertEquals(breakPreventionControl.taxonomy, "break_prevention_supported_local")

    val breakTacticalVeto = byId("break-prevention-tactical-veto")
    assertEquals(breakTacticalVeto.release, "Suppressed")
    assertEquals(breakTacticalVeto.primary, "-")
    assert(!breakTacticalVeto.leak, clues(breakTacticalVeto))

    List(
      "break-prevention-missing-witness-control",
      "break-prevention-rival-relabel-control"
    ).foreach { id =>
      val row = byId(id)
      assertEquals(row.release, "Suppressed")
      assertEquals(row.primary, "-")
      assert(!row.leak, clues(row))
    }

    val prophylaxisControl = byId("prophylaxis-restraint-supported-local-control")
    assertEquals(prophylaxisControl.release, "Suppressed")
    assertEquals(prophylaxisControl.primary, "-")
    assertEquals(prophylaxisControl.taxonomy, "prophylaxis_restraint_supported_local")
    assert(!prophylaxisControl.moveReview.contains("So the task is"), clues(prophylaxisControl))

    val prophylaxisTactical = byId("prophylaxis-restraint-tactical-veto")
    assertEquals(prophylaxisTactical.release, "Suppressed")
    assertEquals(prophylaxisTactical.primary, "-")
    assert(!prophylaxisTactical.leak, clues(prophylaxisTactical))

    List("prophylaxis-restraint-missing-witness-control", "prophylaxis-restraint-rival-relabel-control").foreach { id =>
      val row = byId(id)
      assertEquals(row.release, "Suppressed")
      assertEquals(row.primary, "-")
      assert(!row.leak, clues(row))
    }

    assert(observations.forall(!_.leak), clues(observations.filter(_.leak)))
  }

  test("B/C authority surface ledger expands candidate coverage and writes reviewable taxonomy") {
    val observations = AuthoritySurfaceLedger.observations()
    val byId = observations.map(obs => obs.sample.id -> obs).toMap

    assert(byId.contains("screen-K09C"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("screen-K08B"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("screen-K03B"), clues(byId.keySet.toList.sorted))
    assert(observations.count(_.sample.id.startsWith("screen-")) >= 10, clues(observations.map(_.sample.id)))
    assert(byId.contains("priority-MI1"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-MI4"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-MR1"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-MR2"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-MR3"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-TO1"), clues(byId.keySet.toList.sorted))
    assert(byId.contains("priority-SC2"), clues(byId.keySet.toList.sorted))
    assert(observations.count(_.sample.id.startsWith("priority-")) >= 14, clues(observations.map(_.sample.id)))
    val priorityMi1 = byId("priority-MI1")
    assertEquals(priorityMi1.release, "SupportedLocal", clues(priorityMi1))
    assert(priorityMi1.plannerOwner.contains("WhyThis:MoveDelta:target"), clues(priorityMi1))
    assertEquals(priorityMi1.contractId, s"subplan:${PlanTaxonomy.PlanKind.SimplificationWindow.id}", clues(priorityMi1))
    assertEquals(priorityMi1.contractStatus, "Releasable", clues(priorityMi1))
    assertEquals(priorityMi1.contractFailures, "none", clues(priorityMi1))

    assertEquals(
      observations.filter(_.sample.id.startsWith("natural-")).filter(_.release == "SupportedLocal").map(_.sample.id),
      List("natural-K09B", "natural-K09F")
    )
    val supportedSourceRows =
      observations
        .filter(obs => obs.sample.id.startsWith("source-") && obs.release == "SupportedLocal")
    val sourceBreakPreventionRows =
      supportedSourceRows.filter(_.taxonomy == "source_break_prevention")
    assertEquals(
      sourceBreakPreventionRows.map(_.sample.id),
      Nil
    )
    assertEquals(
      supportedSourceRows
        .map(_.sample.id),
      sourceQueenTradeBoundaryIds ++
      List(
        sourceBadPieceLiquidationId,
        "source-bad-piece-liquidation-pilot",
        sourceCentralBreakTimingId,
        "source-botvinnik-vidmar-1936-simplification-window"
      )
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id.startsWith("source-") && obs.release == "CertifiedOwner")
        .map(_.sample.id),
      Nil
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id == "iqp-supported-local-control" && obs.release == "Suppressed")
        .map(_.sample.id),
      List("iqp-supported-local-control")
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id == "break-prevention-supported-local-control" && obs.release == "Suppressed")
        .map(_.sample.id),
      List("break-prevention-supported-local-control")
    )

    val tacticalVetoRows =
      observations.filter(obs => obs.sample.reviewGroup.startsWith("negative:") && obs.release == "TacticalVeto")
    assertEquals(
      tacticalVetoRows.map(_.sample.id).sorted,
      List(
        "K09A-tactical-veto",
        "K09B-tactical-veto",
        "K09F-tactical-veto"
      )
    )
    val breakNegativeControls =
      observations.filter(obs => obs.sample.id.startsWith("break-prevention-") && obs.sample.reviewGroup.startsWith("negative:"))
    assertEquals(
      breakNegativeControls.map(obs => obs.sample.id -> obs.release).toMap,
      Map(
        "break-prevention-tactical-veto" -> "Suppressed",
        "break-prevention-missing-witness-control" -> "Suppressed",
        "break-prevention-rival-relabel-control" -> "Suppressed"
      )
    )
    val prophylaxisNegativeControls =
      observations.filter(obs =>
        obs.sample.id.startsWith("prophylaxis-restraint-") && obs.sample.reviewGroup.startsWith("negative:")
      )
    assertEquals(
      prophylaxisNegativeControls.map(obs => obs.sample.id -> obs.release).toMap,
      Map(
        "prophylaxis-restraint-tactical-veto" -> "Suppressed",
        "prophylaxis-restraint-missing-witness-control" -> "Suppressed",
        "prophylaxis-restraint-rival-relabel-control" -> "Suppressed"
      )
    )
    val tacticalFirstSuppressed =
      observations.filter(obs => obs.sample.id.startsWith("priority-") && obs.sample.reviewGroup.startsWith("negative:") && obs.release == "Suppressed")
    assertEquals(
      tacticalFirstSuppressed.map(_.sample.id).sorted,
      List("priority-MR1-tactical-veto", "priority-MR2-tactical-veto")
    )
    val priorityTO1 = byId("priority-TO1-tactical-veto")
    assertEquals(priorityTO1.release, "Other:ForcingDefense", clues(priorityTO1))
    assert(priorityTO1.plannerOwner.contains("WhyNow:ForcingDefense:threat"), clues(priorityTO1))

    val failRows = observations.filter(obs => obs.release == "Suppressed" || obs.release == "TacticalVeto")
    assert(failRows.forall(_.taxonomy != "-"), clues(failRows.filter(_.taxonomy == "-").map(_.sample.id)))

    val review = AuthoritySurfaceLedger.surfaceReviewMarkdown(observations)
    assert(review.contains("# Strategic Claim Authority Surface Ledger"), clues(review))
    assert(!review.contains("Source admitted" + " authority rows"), clues(review))
    assert(!review.contains("Natural SupportedLocal" + " search"), clues(review))
    assert(review.contains("## CertifiedOwner"), clues(review))
    assert(review.contains("## SupportedLocal"), clues(review))
    assert(review.contains("## Suppressed"), clues(review))
    assert(review.contains("## TacticalVeto"), clues(review))
    assert(
      review.contains(
        "Surface SupportedLocal fixtures: 9 rows (certified_owner_path=2, same_job_or_conversion_relabel_blocked=1, source_bad_piece_liquidation=2, source_central_break_timing=1, source_queen_trade_boundary=2, source_simplification_window=1)"
      ),
      clues(review)
    )
    assert(
      review.contains(
        "Source surface fixtures: 25 fixed rows (source_bad_piece_liquidation=2, source_break_prevention=6, source_carlsbad_fixed_target=1, source_central_break_timing=1, source_flank_clamp=4, source_iqp_inducement=6, source_queen_trade_boundary=2, source_simplification_window=2, source_static_weakness_fixation=1)"
      ),
      clues(review)
    )
    assert(review.contains("Engine-backed source admission: SourceReview only"), clues(review))
    assert(review.contains("taxonomy="), clues(review))
    assert(review.contains("contract="), clues(review))
  }

  test("source intake rows require engine admission, and fixed source fixtures enter surface matrix only") {
    val intake = SourceReview.observations(engine = None)
    assert(intake.forall(_.verdict != SourceReview.Verdict.AdmitAuthorityRow), clues(intake))
    assertEquals(
      AuthoritySurfaceLedger.sourceSurfaceFixtureIds,
      expectedSourceSurfaceFixtureIds
    )
    val sourceRows = AuthoritySurfaceLedger.observations().filter(_.sample.id.startsWith("source-"))
    assertEquals(sourceRows.map(_.sample.id), AuthoritySurfaceLedger.sourceSurfaceFixtureIds)
    assertEquals(
      sourceRows.map(_.release),
      sourceRows.map(row => expectedSourceReleases(row.sample.id))
    )
  }

  test("TacticalVeto rows require explicit veto or a releasable baseline") {
    val observations = AuthoritySurfaceLedger.observations()
    val tacticalVetoRows = observations.filter(_.release == "TacticalVeto")
    val baselineBacked = Set(
      "K09A-tactical-veto",
      "K09B-tactical-veto",
      "K09F-tactical-veto"
    )

    assert(tacticalVetoRows.nonEmpty)
    assert(
      tacticalVetoRows.forall(obs => obs.rejected.contains("strategic_claim_tactical_veto") || baselineBacked.contains(obs.sample.id)),
      clues(tacticalVetoRows)
    )
  }

  test("subset matrix runs do not overwrite canonical review artifacts") {
    val canonical = AuthoritySurfaceLedger.outputPaths(Set.empty)
    val subset = AuthoritySurfaceLedger.outputPaths(Set("B15A-certified-carlsbad"))

    assertEquals(canonical.matrix.getFileName.toString, "strategic_claim_authority_surface_ledger.tsv")
    assertEquals(canonical.review.getFileName.toString, "strategic_claim_authority_surface_review.md")
    assertEquals(subset.matrix.getFileName.toString, "strategic_claim_authority_surface_ledger_subset.tsv")
    assertEquals(subset.review.getFileName.toString, "strategic_claim_authority_surface_review_subset.md")
  }
