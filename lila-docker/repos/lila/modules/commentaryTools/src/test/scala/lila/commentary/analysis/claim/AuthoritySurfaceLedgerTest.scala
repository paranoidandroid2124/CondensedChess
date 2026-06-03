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

    assertEquals(byId("natural-B15A").release, "Suppressed", clues(byId("natural-B15A")))
    assert(byId("natural-B15A").rejected.contains("truth_contract_missing"), clues(byId("natural-B15A")))
    assertEquals(byId("natural-K09B").release, "CertifiedOwner")
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
    assert(soft.rejected.contains("truth_contract_missing"), clues(soft))
    assert(!soft.leak, clues(soft))

    val cSoft = byId("K09B-supported-local-soft")
    assertEquals(cSoft.release, "SupportedLocal")
    assertEquals(cSoft.primary, "A key idea is that this trade keeps the same local edge on e6.")
    assert(cSoft.moveReview.contains(cSoft.primary), clues(cSoft))
    assert(cSoft.plannerOwner.contains("MoveDelta"), clues(cSoft))
    assert(!cSoft.primary.startsWith("This trade"), clues(cSoft))
    assert(!cSoft.moveReview.startsWith("This trade"), clues(cSoft))

    val softVeto = byId("B15A-supported-local-veto")
    assertEquals(softVeto.release, "TacticalVeto")
    assertEquals(softVeto.primary, "-")
    assert(!softVeto.leak, clues(softVeto))

    val exactSurface = byId("B15A-certified-carlsbad")
    assertEquals(exactSurface.release, "Suppressed", clues(exactSurface))
    assert(exactSurface.rejected.contains("truth_contract_missing"), clues(exactSurface))
    assert(!exactSurface.moveReview.contains("So the task is"), clues(exactSurface))

    val sourceB = byId("source-evans-opsahl-1950")
    assertEquals(sourceB.release, "Other:MoveDelta", clues(sourceB))
    assert(sourceB.plannerOwner.contains("MoveDelta:carlsbad_fixed_target_probe"), clues(sourceB))
    assert(sourceB.primary.contains("Further probe work still targets"), clues(sourceB))
    assert(!sourceB.primary.contains("c6 is the fixed target"), clues(sourceB))
    assertEquals(sourceB.contractStatus, "Releasable")
    assert(sourceB.contractId.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id), clues(sourceB))

    val sourceC = byId("source-carlsen-anand-2014-g6")
    assertEquals(sourceC.release, "Suppressed", clues(sourceC))
    assertEquals(sourceC.primary, "-")
    assertEquals(sourceC.plannerOwner, "-")
    assertEquals(sourceC.contractStatus, "-")
    assertEquals(sourceC.contractId, "-")
    assert(!sourceC.moveReview.contains("A key idea is that"), clues(sourceC))
    assert(!sourceC.moveReview.contains("So the task is"), clues(sourceC))

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
      assertEquals(naturalIqp.primary, "This sequence leaves an isolated pawn as the local target.")
      val expectedText = PlayerFacingClaimPrefixKind.SupportedLocal.render(naturalIqp.primary)
      assert(naturalIqp.moveReview.contains(expectedText), clues(naturalIqp))
      assert(naturalIqp.plannerOwner.contains(s"WhyThis:MoveDelta:${PlayerFacingTruthModePolicy.IQPInducementProbeProofSource}"), clues(naturalIqp))
      assertEquals(naturalIqp.contractStatus, "Releasable")
      assert(naturalIqp.contractId.contains(PlanTaxonomy.PlanKind.IQPInducement.id), clues(naturalIqp))
      assertEquals(naturalIqp.taxonomy, "source_iqp_inducement")
      assert(!naturalIqp.moveReview.contains("So the task is"), clues(naturalIqp))
    }

    List(
      "source-maderna-palermo-1955-a6-a5-break-prevention" -> "...a6-a5",
      "source-camara-bazan-1960-b7-b5-break-prevention" -> "...b7-b5"
    ).foreach { case (id, breakToken) =>
      val sourceBreakPrevention = byId(id)
      assertEquals(sourceBreakPrevention.release, "SupportedLocal")
      assertEquals(sourceBreakPrevention.primary, s"This keeps $breakToken from coming right away.")
      val expectedText = PlayerFacingClaimPrefixKind.SupportedLocal.render(sourceBreakPrevention.primary)
      assert(sourceBreakPrevention.moveReview.contains(expectedText), clues(sourceBreakPrevention))
      assert(sourceBreakPrevention.plannerOwner.contains("WhyThis:MoveDelta:counterplay_axis_suppression"), clues(sourceBreakPrevention))
      assertEquals(sourceBreakPrevention.contractStatus, "Releasable")
      assert(sourceBreakPrevention.contractId.contains("neutralize_key_break"), clues(sourceBreakPrevention))
      assertEquals(sourceBreakPrevention.taxonomy, "source_break_prevention")
      assert(!sourceBreakPrevention.moveReview.contains("So the task is"), clues(sourceBreakPrevention))
    }

    val sourceSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    assertEquals(sourceSimplification.release, "Suppressed")
    assertEquals(sourceSimplification.primary, "-")
    assertEquals(sourceSimplification.plannerOwner, "-")
    assertEquals(sourceSimplification.contractStatus, "-")
    assertEquals(sourceSimplification.contractId, "-")
    assertEquals(sourceSimplification.taxonomy, "source_simplification_window")

    val sourceStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    assertEquals(sourceStaticWeakness.release, "CertifiedOwner")
    assert(sourceStaticWeakness.primary != "-")
    assert(sourceStaticWeakness.plannerOwner != "-")
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
        "source-camara-bazan-1960-b7-b5-break-prevention"
      )
    )
    sourceBreakPreventionRows.foreach { row =>
      assert(row.plannerOwner.contains("MoveDelta:counterplay_axis_suppression"), clues(row))
      assert(row.contractId.contains("neutralize_key_break"), clues(row))
      assert(row.moveReview.contains("A key idea is that "), clues(row))
      assert(!row.moveReview.contains("So the task is"), clues(row))
    }
    assertEquals(
      supportedSourceRows
        .filterNot(row => sourceBreakPreventionRows.exists(_.sample.id == row.sample.id))
        .map(_.sample.id),
      List(
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-evans-opsahl-1950-iqp-inducement",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement"
      )
    )
    assertEquals(
      observations
        .filter(obs => obs.sample.id.startsWith("source-") && obs.release == "CertifiedOwner")
        .map(_.sample.id),
      List(
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation"
      )
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
        "B15A-supported-local-veto",
        "B15A-tactical-veto",
        "B16B-tactical-veto",
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
      List("priority-MR1-tactical-veto", "priority-MR2-tactical-veto", "priority-TO1-tactical-veto")
    )

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
        "Surface SupportedLocal fixtures: source-capablanca-golombek-1939-iqp-inducement, source-evans-opsahl-1950-iqp-inducement, source-alekhine-bogoljubow-1936-iqp-inducement, source-najdorf-sergeant-1939-iqp-inducement, source-botvinnik-vidmar-1936-iqp-opening-inducement, source-maderna-palermo-1955-a6-a5-break-prevention, source-camara-bazan-1960-b7-b5-break-prevention"
      ),
      clues(review)
    )
    assert(
      review.contains(
        "Source surface fixtures: source-evans-opsahl-1950, source-carlsen-anand-2014-g6, source-capablanca-golombek-1939-iqp-inducement, source-evans-opsahl-1950-iqp-inducement, source-alekhine-bogoljubow-1936-iqp-inducement, source-najdorf-sergeant-1939-iqp-inducement, source-botvinnik-vidmar-1936-iqp-opening-inducement, source-maderna-palermo-1955-a6-a5-break-prevention, source-camara-bazan-1960-b7-b5-break-prevention, source-pfleger-maalouf-1961-a6-a5-break-prevention, source-salov-ljubojevic-1992-simplification-window, source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation"
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
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation"
      )
    )
    val sourceRows = AuthoritySurfaceLedger.observations().filter(_.sample.id.startsWith("source-"))
    assertEquals(sourceRows.map(_.sample.id), AuthoritySurfaceLedger.sourceSurfaceFixtureIds)
    assertEquals(
      sourceRows.map(_.release),
      List(
        "Other:MoveDelta",
        "Suppressed",
        "SupportedLocal",
        "SupportedLocal",
        "SupportedLocal",
        "SupportedLocal",
        "SupportedLocal",
        "SupportedLocal",
        "SupportedLocal",
        "Suppressed",
        "Suppressed",
        "CertifiedOwner"
      )
    )
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
