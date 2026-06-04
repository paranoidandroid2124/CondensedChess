package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofSourceId
import munit.FunSuite

class AdmissionUnitCatalogTest extends FunSuite:

  test("admission unit catalog encodes one-plan-kind one-proof-source work units") {
    val units = AdmissionUnitCatalog.admissionUnits

    assertEquals(
      units.map(unit => unit.planKindId -> unit.proofSource),
      List(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id -> PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
        PlanTaxonomy.PlanKind.BackwardPawnTargeting.id -> PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
        PlanTaxonomy.PlanKind.BreakPrevention.id -> ProofSourceId.CounterplayAxisSuppression.wireKey,
        PlanTaxonomy.PlanKind.ProphylaxisRestraint.id -> ProofSourceId.ProphylacticMove.wireKey,
        PlanTaxonomy.PlanKind.OpenFilePressure.id -> ProofSourceId.LocalFileEntryBind.wireKey,
        PlanTaxonomy.PlanKind.KeySquareDenial.id -> ProofSourceId.LocalFileEntryBind.wireKey,
        PlanTaxonomy.PlanKind.RookFileTransfer.id -> ProofSourceId.LocalFileEntryBind.wireKey,
        PlanTaxonomy.PlanKind.FlankClamp.id -> ProofSourceId.ColorComplexSqueezeProbe.wireKey,
        PlanTaxonomy.PlanKind.OutpostEntrenchment.id -> PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource,
        PlanTaxonomy.PlanKind.IQPInducement.id -> PlayerFacingTruthModePolicy.IQPInducementProbeProofSource,
        PlanTaxonomy.PlanKind.SimplificationWindow.id -> PlanTaxonomy.PlanKind.SimplificationWindow.id,
        PlanTaxonomy.PlanKind.DefenderTrade.id -> PlayerFacingTruthModePolicy.DefenderTradeProofSource,
        PlanTaxonomy.PlanKind.QueenTradeShield.id -> PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
        PlanTaxonomy.PlanKind.BadPieceLiquidation.id -> PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
        PlanTaxonomy.PlanKind.CentralBreakTiming.id -> PlanTaxonomy.PlanKind.CentralBreakTiming.id
      )
    )
    assertEquals(units.map(_.planKindId).distinct.size, units.size)
    assertEquals(
      units.map(unit => unit.planKindId -> unit.sourceReviewGroup).toMap,
      Map(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id -> "B:static_weakness_fixation",
        PlanTaxonomy.PlanKind.BackwardPawnTargeting.id -> "B:carlsbad_fixed_target",
        PlanTaxonomy.PlanKind.BreakPrevention.id -> "A:break_prevention",
        PlanTaxonomy.PlanKind.ProphylaxisRestraint.id -> "A:prophylaxis_restraint",
        PlanTaxonomy.PlanKind.OpenFilePressure.id -> "A:open_file_pressure",
        PlanTaxonomy.PlanKind.KeySquareDenial.id -> "A:key_square_denial",
        PlanTaxonomy.PlanKind.RookFileTransfer.id -> "A:rook_file_transfer",
        PlanTaxonomy.PlanKind.FlankClamp.id -> "A:flank_clamp",
        PlanTaxonomy.PlanKind.OutpostEntrenchment.id -> "A:outpost_entrenchment",
        PlanTaxonomy.PlanKind.IQPInducement.id -> "C:iqp_inducement",
        PlanTaxonomy.PlanKind.SimplificationWindow.id -> "C:simplification_window",
        PlanTaxonomy.PlanKind.DefenderTrade.id -> "C:defender_trade",
        PlanTaxonomy.PlanKind.QueenTradeShield.id -> "C:queen_trade_boundary",
        PlanTaxonomy.PlanKind.BadPieceLiquidation.id -> "C:bad_piece_liquidation",
        PlanTaxonomy.PlanKind.CentralBreakTiming.id -> "A:central_break_timing"
      )
    )
    assert(units.forall(_.sourceCandidateTarget == AdmissionUnitCatalog.SourceCandidateTarget(2, 5)), clues(units))
    assert(units.forall(_.maxAuthorityRows == 2), clues(units))
    assert(units.forall(_.controlledPositive.count == 1), clues(units))
    assert(units.forall(_.negativeControls.map(_.kind) == AdmissionUnitCatalog.requiredNegativeKinds), clues(units))
  }

  test("queued admission units resolve through runtime contracts without source-witness-id gates") {
    val byFamily = AdmissionUnitCatalog.admissionUnits.map(unit => unit.planKindId -> unit).toMap

    AdmissionUnitCatalog.admissionUnits.foreach { unit =>
      val contract =
        ProofContractRules
          .contractForProofFamily(unit.proofFamily)
          .getOrElse(fail(s"missing proof contract for ${unit.proofFamily}"))

      assertEquals(contract.status, ProofContractStatus.Releasable, clues(unit, contract))
      assert(contract.supportedLocalEligible, clues(unit, contract))
      assert(contract.acceptedSources.contains(unit.proofSource), clues(unit, contract))
      assert(contract.allowedScopes.contains(unit.acceptedScope), clues(unit, contract))
      assert(contract.requiredWitnesses.subsetOf(unit.requiredWitnesses), clues(unit, contract))
      assert(unit.requiredWitnesses.contains(ProofWitness.ClaimOnlySurface), clues(unit))
      assert(unit.surfaceAuthorityTiers.forall(expectedAuthorityTiers(contract).contains), clues(unit, contract))
      assert(!contract.id.toLowerCase.contains("source-"), clues(unit, contract))
      assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(unit, contract))
    }

    val positionLocalUnits =
      List(
        byFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
        byFamily(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id),
        byFamily(PlanTaxonomy.PlanKind.FlankClamp.id)
      )
    positionLocalUnits.foreach { unit =>
      assertEquals(unit.acceptedScope, PlayerFacingPacketScope.PositionLocal)
      assert(unit.controlledPositive.expectedPacket.contains("scope=PositionLocal"), clues(unit))
    }

    assert(
      byFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id).controlledPositive.expectedPacket
        .contains("authority=CertifiedOwner+SupportedLocal"),
      clues(byFamily(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id))
    )
    assert(
      byFamily(PlanTaxonomy.PlanKind.IQPInducement.id).controlledPositive.expectedPacket
        .contains("authority=SupportedLocal"),
      clues(byFamily(PlanTaxonomy.PlanKind.IQPInducement.id))
    )
    assert(
      byFamily(PlanTaxonomy.PlanKind.SimplificationWindow.id).controlledPositive.expectedPacket
        .contains("authority=SupportedLocal"),
      clues(byFamily(PlanTaxonomy.PlanKind.SimplificationWindow.id))
    )
  }

  test("admission unit catalog report fixes the execution contract for future passes") {
    val markdown = AdmissionUnitCatalog.markdown

    assert(markdown.contains("1 plan kind, 1 proof source"), clues(markdown))
    assert(markdown.contains("0-2 authority rows"), clues(markdown))
    assert(markdown.contains("surface authority"), clues(markdown))
    assert(markdown.contains("CertifiedOwner requires PV1 source-move agreement"), clues(markdown))
    assert(markdown.contains("SupportedLocal may use near-top MultiPV"), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.ExactTargetFixationProofSource), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource), clues(markdown))
    assert(markdown.contains(ProofSourceId.CounterplayAxisSuppression.wireKey), clues(markdown))
    assert(markdown.contains(ProofSourceId.ProphylacticMove.wireKey), clues(markdown))
    assert(markdown.contains(ProofSourceId.LocalFileEntryBind.wireKey), clues(markdown))
    assert(markdown.contains(PlanTaxonomy.PlanKind.KeySquareDenial.id), clues(markdown))
    assert(markdown.contains(PlanTaxonomy.PlanKind.RookFileTransfer.id), clues(markdown))
    assert(markdown.contains(ProofSourceId.ColorComplexSqueezeProbe.wireKey), clues(markdown))
    assert(markdown.contains(PlanTaxonomy.PlanKind.FlankClamp.id), clues(markdown))
    assert(markdown.contains(PlanTaxonomy.PlanKind.OutpostEntrenchment.id), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.IQPInducementProbeProofSource), clues(markdown))
    assert(markdown.contains(PlanTaxonomy.PlanKind.SimplificationWindow.id), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.DefenderTradeProofSource), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.QueenTradeShieldProofSource), clues(markdown))
    assert(markdown.contains(PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource), clues(markdown))
    assert(markdown.contains("central_break_timing"), clues(markdown))
  }

  private def expectedAuthorityTiers(contract: ProofContract): List[String] =
    List(
      Option.when(contract.certifiedEligible)("CertifiedOwner"),
      Option.when(contract.supportedLocalEligible)("SupportedLocal")
    ).flatten
