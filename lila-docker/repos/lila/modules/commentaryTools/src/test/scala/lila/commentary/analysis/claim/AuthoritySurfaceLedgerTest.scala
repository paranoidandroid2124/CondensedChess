package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite

class AuthoritySurfaceLedgerTest extends FunSuite:

  override val munitTimeout = scala.concurrent.duration.Duration(90, "s")

  private def compact(text: String): String =
    text.replaceAll("\\s+", " ").trim

  test("B/C authority surface ledger covers natural rows, soft rows, and tactical-veto parity") {
    val observations = AuthoritySurfaceLedger.observations()
    val byId = observations.map(obs => obs.sample.id -> obs).toMap

    assertEquals(byId("natural-B15A").release, "CertifiedOwner")
    assertEquals(byId("natural-K09B").release, "CertifiedOwner")
    assertEquals(byId("natural-K03A").release, "Suppressed")
    assertEquals(byId("natural-K08A").release, "Suppressed")
    assertEquals(byId("natural-K09E").release, "Suppressed")
    assertEquals(byId("natural-D01A").release, "Suppressed")
    assertEquals(byId("natural-D01B").release, "Suppressed")
    assertEquals(byId("natural-MI2").release, "Suppressed")
    assertEquals(byId("natural-MI3").release, "Suppressed")

    val soft = byId("B15A-supported-local-soft")
    assertEquals(soft.release, "SupportedLocal")
    assertEquals(soft.primary, "A local reading is that c6 is the fixed target.")
    assert(soft.bookmaker.contains(soft.primary), clues(soft))
    assertEquals(soft.chronicle, soft.primary)
    assert(soft.plannerOwner.contains("PositionProbe"), clues(soft))
    assert(!soft.primary.contains("The key strategic fact"), clues(soft))
    assert(!soft.bookmaker.contains("The key strategic fact"), clues(soft))

    val cSoft = byId("K09B-supported-local-soft")
    assertEquals(cSoft.release, "SupportedLocal")
    assertEquals(cSoft.primary, "A local reading is that this trade keeps the same local edge on e6.")
    assert(cSoft.bookmaker.contains(cSoft.primary), clues(cSoft))
    assertEquals(cSoft.chronicle, cSoft.primary)
    assert(cSoft.plannerOwner.contains("MoveDelta"), clues(cSoft))
    assert(!cSoft.primary.startsWith("This trade"), clues(cSoft))
    assert(!cSoft.bookmaker.startsWith("This trade"), clues(cSoft))

    val softVeto = byId("B15A-supported-local-veto")
    assertEquals(softVeto.release, "TacticalVeto")
    assertEquals(softVeto.primary, "-")
    assert(!softVeto.leak, clues(softVeto))

    val exactSurface = byId("B15A-certified-carlsbad")
    assert(exactSurface.bookmaker.contains("So the task is to keep the queenside pressure trained on c6"), clues(exactSurface))

    val sourceB = byId("source-evans-opsahl-1950")
    assertEquals(sourceB.release, "CertifiedOwner")
    assert(sourceB.plannerOwner.contains("WhatMattersHere:PositionProbe:carlsbad_fixed_target_probe"), clues(sourceB))
    assert(sourceB.primary.contains("c6 is the fixed target"), clues(sourceB))
    assertEquals(sourceB.contractStatus, "Releasable")
    assert(sourceB.contractId.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id), clues(sourceB))

    val sourceC = byId("source-carlsen-anand-2014-g6")
    assertEquals(sourceC.release, "SupportedLocal")
    assertEquals(sourceC.primary, "A local reading is that this exchange moves the game into the queenless branch.")
    assert(sourceC.bookmaker.contains(sourceC.primary), clues(sourceC))
    assertEquals(sourceC.chronicle, sourceC.primary)
    assert(sourceC.plannerOwner.contains("WhyThis:MoveDelta:queen_trade_shield"), clues(sourceC))
    assertEquals(sourceC.contractStatus, "Releasable")
    assert(sourceC.contractId.contains(PlanTaxonomy.PlanKind.QueenTradeShield.id), clues(sourceC))
    assert(!sourceC.bookmaker.contains("So the task is"), clues(sourceC))
    assert(!sourceC.chronicle.contains("So the task is"), clues(sourceC))

    val naturalIqpIds =
      List(
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-evans-opsahl-1950-iqp-inducement",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement"
      )
    naturalIqpIds.foreach { id =>
      val naturalIqp = byId(id)
      assertEquals(naturalIqp.release, "SupportedLocal")
      assertEquals(naturalIqp.primary, "A local reading is that this sequence leaves an isolated pawn as the local target.")
      assertEquals(naturalIqp.bookmaker, naturalIqp.primary)
      assertEquals(naturalIqp.chronicle, naturalIqp.primary)
      assert(naturalIqp.plannerOwner.contains(s"WhyThis:MoveDelta:${PlayerFacingTruthModePolicy.IQPInducementProbeProofSource}"), clues(naturalIqp))
      assertEquals(naturalIqp.contractStatus, "Releasable")
      assert(naturalIqp.contractId.contains(PlanTaxonomy.PlanKind.IQPInducement.id), clues(naturalIqp))
      assertEquals(naturalIqp.taxonomy, "source_iqp_inducement")
      assert(!naturalIqp.bookmaker.contains("So the task is"), clues(naturalIqp))
      assert(!naturalIqp.chronicle.contains("So the task is"), clues(naturalIqp))
    }

    List(
      "source-maderna-palermo-1955-a6-a5-break-prevention" -> "...a5",
      "source-camara-bazan-1960-b7-b5-break-prevention" -> "...b5",
      "source-pfleger-maalouf-1961-a6-a5-break-prevention" -> "...a5"
    ).foreach { case (id, breakToken) =>
      val sourceBreakPrevention = byId(id)
      assertEquals(sourceBreakPrevention.release, "SupportedLocal")
      assertEquals(sourceBreakPrevention.primary, s"A local reading is that this keeps $breakToken from coming right away.")
      assertEquals(sourceBreakPrevention.bookmaker, sourceBreakPrevention.primary)
      assertEquals(sourceBreakPrevention.chronicle, sourceBreakPrevention.primary)
      assert(sourceBreakPrevention.plannerOwner.contains("WhyThis:MoveDelta:counterplay_axis_suppression"), clues(sourceBreakPrevention))
      assertEquals(sourceBreakPrevention.contractStatus, "Releasable")
      assert(sourceBreakPrevention.contractId.contains("neutralize_key_break"), clues(sourceBreakPrevention))
      assertEquals(sourceBreakPrevention.taxonomy, "source_break_prevention")
      assert(!sourceBreakPrevention.bookmaker.contains("So the task is"), clues(sourceBreakPrevention))
      assert(!sourceBreakPrevention.chronicle.contains("So the task is"), clues(sourceBreakPrevention))
    }

    val sourceSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    assertEquals(sourceSimplification.release, "CertifiedOwner")
    assertEquals(sourceSimplification.primary, "This trade keeps the same local edge on d5.")
    assertEquals(compact(sourceSimplification.bookmaker), "This trade keeps the same local edge on d5. The practical alternative Qxd5 remains secondary here.")
    assertEquals(compact(sourceSimplification.chronicle), compact(sourceSimplification.bookmaker))
    assert(sourceSimplification.plannerOwner.contains("WhyThis:MoveDelta:target"), clues(sourceSimplification))
    assertEquals(sourceSimplification.contractStatus, "Releasable")
    assert(sourceSimplification.contractId.contains(PlanTaxonomy.PlanKind.SimplificationWindow.id), clues(sourceSimplification))
    assertEquals(sourceSimplification.taxonomy, "source_simplification_window")

    val sourceStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    assertEquals(sourceStaticWeakness.release, "CertifiedOwner")
    assertEquals(sourceStaticWeakness.primary, "This changes the position by fixing d6 as the target.")
    assertEquals(
      compact(sourceStaticWeakness.bookmaker),
      "This changes the position by fixing d6 as the target. Before the move, d6 was not yet fixed as the target on that defended branch. That same defended branch keeps the pressure fixed on d6."
    )
    assertEquals(compact(sourceStaticWeakness.chronicle), compact(sourceStaticWeakness.bookmaker))
    assert(sourceStaticWeakness.plannerOwner.contains("WhatChanged:MoveDelta:target"), clues(sourceStaticWeakness))
    assertEquals(sourceStaticWeakness.contractStatus, "Releasable")
    assert(sourceStaticWeakness.contractId.contains(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id), clues(sourceStaticWeakness))
    assertEquals(sourceStaticWeakness.taxonomy, "source_static_weakness_fixation")

    val sourceDefenderTrade = byId("source-aronian-andreikin-2014-defender-trade")
    assertEquals(sourceDefenderTrade.release, "SupportedLocal")
    assertEquals(sourceDefenderTrade.primary, "A local reading is that this exchange removes a defender on the local branch.")
    assertEquals(sourceDefenderTrade.bookmaker, sourceDefenderTrade.primary)
    assertEquals(sourceDefenderTrade.chronicle, sourceDefenderTrade.primary)
    assert(sourceDefenderTrade.plannerOwner.contains(s"MoveDelta:${PlayerFacingTruthModePolicy.DefenderTradeProofSource}"), clues(sourceDefenderTrade))
    assertEquals(sourceDefenderTrade.contractStatus, "Releasable")
    assert(sourceDefenderTrade.contractId.contains(PlanTaxonomy.PlanKind.DefenderTrade.id), clues(sourceDefenderTrade))
    assertEquals(sourceDefenderTrade.taxonomy, "source_defender_trade")
    assert(!sourceDefenderTrade.bookmaker.contains("stays best"), clues(sourceDefenderTrade))

    val iqpControl = byId("iqp-supported-local-control")
    assertEquals(iqpControl.release, "SupportedLocal")
    assertEquals(iqpControl.primary, "A local reading is that this sequence leaves an isolated pawn as the local target.")
    assert(iqpControl.bookmaker.contains(iqpControl.primary), clues(iqpControl))
    assertEquals(iqpControl.chronicle, iqpControl.primary)
    assert(iqpControl.plannerOwner.contains(s"WhyThis:MoveDelta:${PlayerFacingTruthModePolicy.IQPInducementProbeProofSource}"), clues(iqpControl))
    assertEquals(iqpControl.contractStatus, "Releasable")
    assert(iqpControl.contractId.contains(PlanTaxonomy.PlanKind.IQPInducement.id), clues(iqpControl))
    assert(!iqpControl.bookmaker.contains("So the task is"), clues(iqpControl))
    assert(!iqpControl.chronicle.contains("So the task is"), clues(iqpControl))

    val breakPreventionControl = byId("break-prevention-supported-local-control")
    assertEquals(breakPreventionControl.release, "SupportedLocal")
    assertEquals(breakPreventionControl.primary, "A local reading is that this keeps ...c5 from coming right away.")
    assertEquals(breakPreventionControl.bookmaker, breakPreventionControl.primary)
    assertEquals(breakPreventionControl.chronicle, breakPreventionControl.primary)
    assert(breakPreventionControl.plannerOwner.contains("MoveDelta:counterplay_axis_suppression"), clues(breakPreventionControl))
    assertEquals(breakPreventionControl.contractStatus, "Releasable")
    assert(breakPreventionControl.contractId.contains("neutralize_key_break"), clues(breakPreventionControl))
    assertEquals(breakPreventionControl.taxonomy, "break_prevention_supported_local")
    assert(!breakPreventionControl.bookmaker.contains("So the task is"), clues(breakPreventionControl))
    assert(!breakPreventionControl.chronicle.contains("So the task is"), clues(breakPreventionControl))

    val breakTacticalVeto = byId("break-prevention-tactical-veto")
    assertEquals(breakTacticalVeto.release, "TacticalVeto")
    assertEquals(breakTacticalVeto.primary, "-")
    assert(!breakTacticalVeto.leak, clues(breakTacticalVeto))

    List("break-prevention-missing-witness-control", "break-prevention-rival-relabel-control").foreach { id =>
      val row = byId(id)
      assertEquals(row.release, "Suppressed")
      assertEquals(row.primary, "-")
      assert(!row.leak, clues(row))
    }

    val prophylaxisControl = byId("prophylaxis-restraint-supported-local-control")
    assertEquals(prophylaxisControl.release, "SupportedLocal")
    assertEquals(
      prophylaxisControl.primary,
      "A local reading is that this slows down queenside counterplay before it gets started."
    )
    assertEquals(prophylaxisControl.bookmaker, prophylaxisControl.primary)
    assertEquals(prophylaxisControl.chronicle, prophylaxisControl.primary)
    assert(prophylaxisControl.plannerOwner.contains("MoveDelta:prophylactic_move"), clues(prophylaxisControl))
    assertEquals(prophylaxisControl.contractStatus, "Releasable")
    assert(prophylaxisControl.contractId.contains("counterplay_restraint"), clues(prophylaxisControl))
    assertEquals(prophylaxisControl.taxonomy, "prophylaxis_restraint_supported_local")
    assert(!prophylaxisControl.bookmaker.contains("So the task is"), clues(prophylaxisControl))
    assert(!prophylaxisControl.chronicle.contains("So the task is"), clues(prophylaxisControl))

    val prophylaxisTactical = byId("prophylaxis-restraint-tactical-veto")
    assertEquals(prophylaxisTactical.release, "TacticalVeto")
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

    assertEquals(observations.filter(_.sample.id.startsWith("natural-")).filter(_.release == "SupportedLocal").map(_.sample.id), Nil)
    val supportedSourceRows =
      observations
        .filter(obs => obs.sample.id.startsWith("source-") && obs.release == "SupportedLocal")
    val sourceBreakPreventionRows =
      supportedSourceRows.filter(_.taxonomy == "source_break_prevention")
    assertEquals(
      sourceBreakPreventionRows.map(_.sample.id),
      List(
        "source-maderna-palermo-1955-a6-a5-break-prevention",
        "source-camara-bazan-1960-b7-b5-break-prevention",
        "source-pfleger-maalouf-1961-a6-a5-break-prevention"
      )
    )
    sourceBreakPreventionRows.foreach { row =>
      assert(row.plannerOwner.contains("MoveDelta:counterplay_axis_suppression"), clues(row))
      assert(row.contractId.contains("neutralize_key_break"), clues(row))
      assert(row.primary.startsWith("A local reading is that "), clues(row))
      assertEquals(row.bookmaker, row.primary)
      assertEquals(row.chronicle, row.primary)
      assert(!row.bookmaker.contains("So the task is"), clues(row))
    }
    assertEquals(
      supportedSourceRows
        .filterNot(row => sourceBreakPreventionRows.exists(_.sample.id == row.sample.id))
        .map(_.sample.id),
      List(
        "source-carlsen-anand-2014-g6",
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-evans-opsahl-1950-iqp-inducement",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement",
        "source-aronian-andreikin-2014-defender-trade"
      )
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id.startsWith("source-") && obs.release == "CertifiedOwner")
        .map(_.sample.id),
      List(
        "source-evans-opsahl-1950",
        "source-salov-ljubojevic-1992-simplification-window",
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation"
      )
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id == "iqp-supported-local-control" && obs.release == "SupportedLocal")
        .map(_.sample.id),
      List("iqp-supported-local-control")
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id == "break-prevention-supported-local-control" && obs.release == "SupportedLocal")
        .map(_.sample.id),
      List("break-prevention-supported-local-control")
    )

    val tacticalVetoRows =
      observations.filter(obs => obs.sample.reviewGroup.startsWith("negative:") && obs.release == "TacticalVeto")
    assertEquals(
      tacticalVetoRows.map(_.sample.id).sorted,
      List(
        "B15A-supported-local-veto",
        "B15A-tactical-veto",
        "B16B-tactical-veto",
        "K09A-tactical-veto",
        "K09B-tactical-veto",
        "K09F-tactical-veto",
        "break-prevention-tactical-veto",
        "prophylaxis-restraint-tactical-veto"
      )
    )
    val breakNegativeControls =
      observations.filter(obs => obs.sample.id.startsWith("break-prevention-") && obs.sample.reviewGroup.startsWith("negative:"))
    assertEquals(
      breakNegativeControls.map(obs => obs.sample.id -> obs.release).toMap,
      Map(
        "break-prevention-tactical-veto" -> "TacticalVeto",
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
        "prophylaxis-restraint-tactical-veto" -> "TacticalVeto",
        "prophylaxis-restraint-missing-witness-control" -> "Suppressed",
        "prophylaxis-restraint-rival-relabel-control" -> "Suppressed"
      )
    )
    val tacticalFirstSuppressed =
      observations.filter(obs => obs.sample.id.startsWith("priority-") && obs.sample.reviewGroup.startsWith("negative:") && obs.release == "Suppressed")
    assertEquals(
      tacticalFirstSuppressed.map(_.sample.id).sorted,
      List("priority-MR1-tactical-veto", "priority-MR2-tactical-veto", "priority-TO1-tactical-veto")
    )

    val failRows = observations.filter(obs => obs.release == "Suppressed" || obs.release == "TacticalVeto")
    assert(failRows.forall(_.taxonomy != "-"), clues(failRows.filter(_.taxonomy == "-").map(_.sample.id)))

    val review = AuthoritySurfaceLedger.surfaceReviewMarkdown(observations)
    assert(review.contains("## CertifiedOwner"), clues(review))
    assert(review.contains("## SupportedLocal"), clues(review))
    assert(review.contains("## Suppressed"), clues(review))
    assert(review.contains("## TacticalVeto"), clues(review))
    assert(
      review.contains(
        "Natural SupportedLocal search: source-carlsen-anand-2014-g6, source-capablanca-golombek-1939-iqp-inducement, source-evans-opsahl-1950-iqp-inducement, source-alekhine-bogoljubow-1936-iqp-inducement, source-najdorf-sergeant-1939-iqp-inducement, source-botvinnik-vidmar-1936-iqp-opening-inducement, source-maderna-palermo-1955-a6-a5-break-prevention, source-camara-bazan-1960-b7-b5-break-prevention, source-pfleger-maalouf-1961-a6-a5-break-prevention, source-aronian-andreikin-2014-defender-trade"
      ),
      clues(review)
    )
    assert(
      review.contains(
        "Source admitted authority rows: source-evans-opsahl-1950, source-carlsen-anand-2014-g6, source-capablanca-golombek-1939-iqp-inducement, source-evans-opsahl-1950-iqp-inducement, source-alekhine-bogoljubow-1936-iqp-inducement, source-najdorf-sergeant-1939-iqp-inducement, source-botvinnik-vidmar-1936-iqp-opening-inducement, source-maderna-palermo-1955-a6-a5-break-prevention, source-camara-bazan-1960-b7-b5-break-prevention, source-pfleger-maalouf-1961-a6-a5-break-prevention, source-salov-ljubojevic-1992-simplification-window, source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation, source-aronian-andreikin-2014-defender-trade"
      ),
      clues(review)
    )
    assert(review.contains("taxonomy="), clues(review))
    assert(review.contains("contract="), clues(review))
  }

  test("source intake rows require engine admission, and exact admitted source fixtures enter matrix authority") {
    val intake = SourceReview.observations(engine = None)
    assert(intake.forall(_.verdict != SourceReview.Verdict.AdmitAuthorityRow), clues(intake))
    assertEquals(
      AuthoritySurfaceLedger.sourceAdmittedAuthorityRowIds,
      List(
        "source-evans-opsahl-1950",
        "source-carlsen-anand-2014-g6",
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-evans-opsahl-1950-iqp-inducement",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement",
        "source-maderna-palermo-1955-a6-a5-break-prevention",
        "source-camara-bazan-1960-b7-b5-break-prevention",
        "source-pfleger-maalouf-1961-a6-a5-break-prevention",
        "source-salov-ljubojevic-1992-simplification-window",
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
        "source-aronian-andreikin-2014-defender-trade"
      )
    )
    val sourceRows = AuthoritySurfaceLedger.observations().filter(_.sample.id.startsWith("source-"))
    assertEquals(sourceRows.map(_.sample.id), AuthoritySurfaceLedger.sourceAdmittedAuthorityRowIds)
    assertEquals(sourceRows.map(_.release), List("CertifiedOwner", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "SupportedLocal", "CertifiedOwner", "CertifiedOwner", "SupportedLocal"))
  }

  test("TacticalVeto rows require explicit veto or a releasable baseline") {
    val observations = AuthoritySurfaceLedger.observations()
    val tacticalVetoRows = observations.filter(_.release == "TacticalVeto")
    val baselineBacked = Set(
      "B15A-tactical-veto",
      "B16B-tactical-veto",
      "K09A-tactical-veto",
      "K09B-tactical-veto",
      "K09F-tactical-veto",
      "B15A-supported-local-veto",
      "break-prevention-tactical-veto",
      "prophylaxis-restraint-tactical-veto"
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
