package lila.commentary.analysis.semantic

import lila.commentary.analysis.PlanTaxonomy
import lila.commentary.analysis.claim.ProofContractRules
import munit.FunSuite

class StrategicObservationIdsTest extends FunSuite:

  test("registered selector source ids are unique and isolated from proof domains") {
    val sources = StrategicObservationIds.EvidenceSourceId.all
    val sourceKeys = sources.map(_.wireKey)

    assert(sourceKeys.size > 50, clues(sourceKeys.size, sourceKeys.sorted))
    assertEquals(sourceKeys.distinct.size, sourceKeys.size, clues(sourceKeys.diff(sourceKeys.distinct)))
    assertEquals(
      sources.flatMap(source => StrategicObservationIds.ProofSourceId.fromWireKey(source.wireKey)),
      Nil
    )
    assertEquals(
      sources.flatMap(source => StrategicObservationIds.ProofFamilyId.fromWireKey(source.wireKey)),
      Nil
    )
  }

  test("proof contract families and accepted sources resolve through typed registries") {
    val proofFamilies = StrategicObservationIds.ProofFamilyId.all.map(_.wireKey).toSet
    val proofSources = StrategicObservationIds.ProofSourceId.all.map(_.wireKey).toSet
    val selectorSources = StrategicObservationIds.EvidenceSourceId.all.map(_.wireKey).toSet
    val acceptedSourceRegistry = proofSources ++ proofFamilies ++ selectorSources

    val missingFamilies =
      ProofContractRules.contracts.map(_.proofFamily).distinct.filterNot(proofFamilies.contains)
    val missingAcceptedSources =
      ProofContractRules.contracts
        .flatMap(contract => contract.acceptedSources.map(source => contract.id -> source))
        .filterNot((_, source) => acceptedSourceRegistry.contains(source))

    assertEquals(missingFamilies, Nil)
    assertEquals(missingAcceptedSources, Nil)
  }

  test("packet-only proof families are registered even when they are not release contracts") {
    List("king_safety", "technical_conversion", "piece_improvement").foreach { family =>
      assert(StrategicObservationIds.ProofFamilyId.fromWireKey(family).nonEmpty, clues(family))
      assertEquals(ProofContractRules.contractForProofFamily(family), None, clues(family))
    }
  }

  test("proof boundary files do not own raw runtime proof ids outside the typed registry") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val registryPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicObservationIds.scala")
    val checkedFiles =
      List(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ProofContractRules.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/claim/ClaimAuthorityPolicy.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/MainPathMoveDeltaClaimBuilder.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlanMatcher.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/QuestionFirstCommentaryPlanner.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/QuietMoveIntentBuilder.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/ActiveStrategicCoachingBriefBuilder.scala"
      ).map(root.resolve(_))
    val runtimeProofIds =
      Set(
        "half_open_file_pressure",
        "neutralize_key_break",
        "counterplay_restraint",
        "trade_key_defender",
        "local_file_entry_bind",
        "counterplay_axis_suppression",
        "prophylactic_move",
        "exchange_forcing_delta",
        "target_focused_coordination_probe",
        "target_focused_coordination",
        "exact_target_fixation",
        "iqp_inducement_probe",
        "active_move_delta",
        "new_access_delta",
        "pressure_increase_delta",
        "counterplay_reduction_delta",
        "resource_removal_delta",
        "plan_advance_delta",
        "new_access",
        "pressure_increase",
        "exchange_forcing",
        "counterplay_reduction",
        "resource_removal",
        "plan_advance"
      )
    val offenders =
      checkedFiles.flatMap { path =>
        val rel = root.relativize(path).toString
        val text = java.nio.file.Files.readString(path)
        runtimeProofIds.toList.sorted.flatMap { id =>
          Option.when(path != registryPath && text.contains("\"" + id + "\""))(s"$rel:$id")
        }
      }

    assertEquals(offenders, Nil)
  }

  test("semantic observation ids do not resolve as proof source or proof family ids") {
    StrategicObservationIds.SemanticObservationId.values.foreach { id =>
      assertEquals(StrategicObservationIds.ProofSourceId.fromWireKey(id.wireKey), None, clues(id))
      assertEquals(StrategicObservationIds.ProofFamilyId.fromWireKey(id.wireKey), None, clues(id))
    }
  }

  test("support-only target pressure observation cannot materialize release authority") {
    val observation =
      StrategicSemanticObservation.supportOnly(
        id = StrategicObservationIds.SemanticObservationId.TargetPressureSemantic,
        ownerSide = "white",
        focusSquares = List("c6"),
        facts = List(
          StrategicObservationIds.FactId.semantic(
            StrategicObservationIds.SemanticObservationId.TargetPressureSemantic
          )
        )
    )

    assertEquals(observation.role, StrategicSemanticObservationRole.SupportOnly)
    assert(observation.supportOnly, clues(observation))
    assertEquals(observation.source, None)
    assertEquals(observation.proofSource, None)
    assertEquals(observation.proofFamily, None)
    assertEquals(
      ProofContractRules.contractForProofFamily(observation.id.wireKey),
      None
    )
  }

  test("support facts cannot be minted from source or proof-domain wire keys") {
    assertEquals(StrategicObservationIds.FactId.dynamic("source:minority_attack_semantic"), None)
    assertEquals(StrategicObservationIds.FactId.dynamic("carlsbad_fixed_target_probe"), None)
    assertEquals(StrategicObservationIds.FactId.dynamic("backward_pawn_targeting"), None)
    assertEquals(StrategicObservationIds.FactId.dynamic("target_pressure_delta_1").map(_.wireKey), Some("target_pressure_delta_1"))
  }

  test("minority semantic observation may become selector source but not proof authority") {
    val observation =
      StrategicSemanticObservation.selectorSource(
        id = StrategicObservationIds.SemanticObservationId.MinorityAttackSemantic,
        source = StrategicObservationIds.EvidenceSourceId.MinorityAttackSemantic,
        ownerSide = "white",
        focusSquares = List("c6"),
        facts = List(
          StrategicObservationIds.FactId.semantic(
            StrategicObservationIds.SemanticObservationId.MinorityAttackSemantic
          )
        )
    )

    assertEquals(observation.role, StrategicSemanticObservationRole.SelectorSource)
    assert(!observation.supportOnly, clues(observation))
    assertEquals(observation.wireEvidenceRefs, List("source:minority_attack_semantic", "minority_attack_semantic"))
    assertEquals(observation.proofSource, None)
    assertEquals(observation.proofFamily, None)
    assertEquals(
      ProofContractRules.contractForProofFamily(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id).map(_.proofFamily),
      Some(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id)
    )
    assertEquals(ProofContractRules.contractForProofFamily(observation.id.wireKey), None)
  }
