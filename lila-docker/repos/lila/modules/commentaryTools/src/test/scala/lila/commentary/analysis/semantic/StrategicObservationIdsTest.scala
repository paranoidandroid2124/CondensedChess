package lila.commentary.analysis.semantic

import lila.commentary.analysis.PlanTaxonomy
import lila.commentary.analysis.MoveReviewExchangeAnalyzer
import lila.commentary.analysis.claim.ProofContractRules
import munit.FunSuite
import scala.jdk.CollectionConverters.*

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
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/MainPathMoveDeltaClaimBuilder.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlanMatcher.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/QuestionFirstCommentaryPlanner.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/QuietMoveIntentBuilder.scala"
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

  test("support facts cannot be minted from source proof or semantic authority wire keys") {
    assertEquals(StrategicObservationIds.FactId.dynamic("source:minority_attack_semantic"), None)
    assertEquals(StrategicObservationIds.FactId.dynamic("carlsbad_fixed_target_probe"), None)
    assertEquals(StrategicObservationIds.FactId.dynamic("backward_pawn_targeting"), None)
    StrategicObservationIds.SemanticObservationId.values.foreach { id =>
      assertEquals(StrategicObservationIds.FactId.dynamic(id.wireKey), None, clues(id))
    }
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

  test("relation observation catalog covers implemented witness kinds and defers unresolved motif families") {
    val implementedKinds = RelationObservationCatalog.Implemented.map(_.relationKind)
    val implementedKindSet = implementedKinds.toSet
    val deferredKinds = RelationObservationCatalog.Deferred.map(_.relationKind)

    assertEquals(
      implementedKinds.sorted,
      MoveReviewExchangeAnalyzer.RelationKind.Implemented.sorted
    )
    assertEquals(
      RelationObservationCatalog.DeferredRelationKinds.sorted,
      MoveReviewExchangeAnalyzer.RelationKind.Deferred.sorted
    )
    assertEquals(
      RelationObservationCatalog.InventoryKinds.sorted,
      MoveReviewExchangeAnalyzer.RelationKind.All.sorted
    )
    assertEquals(
      RelationObservationCatalog.InventoryKinds.distinct.sorted,
      RelationObservationCatalog.InventoryKinds.sorted
    )
    assertEquals(
      deferredKinds.sorted,
      MoveReviewExchangeAnalyzer.RelationKind.Deferred.sorted
    )
    assertEquals(
      RelationObservationCatalog.DeferredRelationKinds,
      List(
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
        MoveReviewExchangeAnalyzer.RelationKind.Domination,
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck
      )
    )
    assertEquals(RelationObservationCatalog.DeferredRelationKinds, deferredKinds)
    assertEquals(deferredKinds.distinct, deferredKinds)
    assertEquals(
      implementedKindSet.intersect(RelationObservationCatalog.DeferredRelationKinds.toSet),
      Set.empty[String]
    )
    assert(
      RelationObservationCatalog.DeferredRelationKinds.forall(kind =>
        RelationObservationCatalog.isDeferredKind(kind) &&
          !RelationObservationCatalog.isImplementedKind(kind) &&
          RelationObservationCatalog.descriptorForKind(kind).isEmpty &&
          RelationObservationCatalog.descriptorForEvidence(Some(kind), List("source:" + kind, kind + "_semantic")).isEmpty
      )
    )
    assert(
      RelationObservationCatalog.Deferred.forall(descriptor =>
        descriptor.internalLabel.trim.nonEmpty &&
          descriptor.requiredWitness.trim.nonEmpty &&
          descriptor.deferReason.trim.nonEmpty &&
          descriptor.fallbackRationale.trim.nonEmpty &&
          RelationObservationCatalog.deferredDescriptorForKind(descriptor.relationKind).contains(descriptor)
      )
    )
    assertEquals(
      RelationObservationCatalog.Deferred.map(descriptor => descriptor.relationKind -> descriptor.fallbackLane).toMap,
      Map(
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug -> DeferredRelationFallbackLane.PracticalGuidance,
        MoveReviewExchangeAnalyzer.RelationKind.Domination -> DeferredRelationFallbackLane.ThematicFallback,
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece -> DeferredRelationFallbackLane.PracticalGuidance,
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap -> DeferredRelationFallbackLane.DiagnosticOnly,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck -> DeferredRelationFallbackLane.DiagnosticOnly
      )
    )
    assertEquals(
      RelationObservationCatalog.Deferred.map(descriptor => descriptor.relationKind -> descriptor.fallbackLabel).toMap,
      Map(
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug -> Some("move-order caution"),
        MoveReviewExchangeAnalyzer.RelationKind.Domination -> Some("key-square restriction"),
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece -> Some("piece mobility"),
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap -> None,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck -> None
      )
    )
    assertEquals(
      RelationObservationCatalog.DeferredRelationKinds.filter(RelationObservationCatalog.allowsDeferredNonRelationFallback),
      List(
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
        MoveReviewExchangeAnalyzer.RelationKind.Domination,
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece
      )
    )
    assertEquals(
      List(
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck
      ).flatMap(RelationObservationCatalog.deferredFallbackForKind).map(_.lane),
      List(DeferredRelationFallbackLane.DiagnosticOnly, DeferredRelationFallbackLane.DiagnosticOnly)
    )
    assertEquals(
      RelationObservationCatalog.DeferredRelationKinds.filter(RelationObservationCatalog.isDiagnosticOnlyDeferred),
      List(
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck
      )
    )
    val deferredMotifExpectations =
      List(
        "zwischenzug" -> Some(MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug),
        "trapped_piece_queen" -> Some(MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece),
        "domination" -> Some(MoveReviewExchangeAnalyzer.RelationKind.Domination),
        "stalemate_trap" -> Some(MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap),
        "perpetual_check" -> Some(MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck),
        "stalemate" -> None,
        "knight_domination" -> None
      )
    assertEquals(
      deferredMotifExpectations.map { case (motif, _) =>
        motif -> RelationObservationCatalog.deferredRelationKindForMotifTag(motif)
      },
      deferredMotifExpectations
    )
    assertEquals(
      RelationObservationCatalog
        .deferredFallbackForMotifTag("trapped_piece_queen")
        .map(fallback => (fallback.relationKind, fallback.lane, fallback.label)),
      Some(
        (
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          DeferredRelationFallbackLane.PracticalGuidance,
          Some("piece mobility")
        )
      )
    )
    assertEquals(
      RelationObservationCatalog
        .deferredFallbackForKind(MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck)
        .exists(_.diagnosticOnly),
      true
    )
    assertEquals(
      RelationObservationCatalog
        .deferredFallbackForKind(MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug)
        .exists(_.allowsNonRelationText),
      true
    )
    assertEquals(
      RelationObservationCatalog.ImplementedKinds,
      implementedKindSet
    )
    assertEquals(
      RelationObservationProducer.RelationKinds,
      implementedKindSet
    )
    assertEquals(
      MoveReviewExchangeAnalyzer.RelationKind.All.toSet -- RelationObservationCatalog.ImplementedKinds,
      RelationObservationCatalog.DeferredRelationKinds.toSet
    )
    assertNotEquals(
      RelationObservationCatalog.ImplementedKinds,
      MoveReviewExchangeAnalyzer.RelationKind.All.toSet
    )
    assert(
      RelationObservationCatalog.Implemented.forall(descriptor =>
        StrategicObservationIds.EvidenceSourceId.fromWireKey(descriptor.source.wireKey).contains(descriptor.source)
      )
    )

    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val xrayRefs = xray.wireEvidenceRefs
    val bothRefs = xrayRefs ++ clearance.wireEvidenceRefs

    assertEquals(xray.sourceRef.wireKey, "source:xray_relation")
    assertEquals(xray.semanticFact.wireKey, "xray_semantic")
    assertEquals(xray.semanticRef.wireKey, "xray_semantic")

    assertEquals(
      RelationObservationCatalog.descriptorForEvidence(Some(clearance.relationKind), bothRefs),
      Some(clearance)
    )
    assertEquals(
      RelationObservationCatalog.descriptorForEvidence(Some(clearance.relationKind), xrayRefs),
      None
    )
    assertEquals(
      RelationObservationCatalog.descriptorForEvidence(None, xrayRefs),
      Some(xray)
    )
    assertEquals(
      RelationObservationCatalog.descriptorForEvidence(None, bothRefs),
      None
    )
  }

  test("runtime relation admission does not consume the broad relation inventory") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val checkedFiles =
      List(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservationPipeline.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala",
        "modules/commentary/src/main/scala/lila/commentary/UserFacingPayloadSanitizer.scala"
      ).map(root.resolve(_))
    val forbidden = List("RelationKind.All", "RelationKind.Deferred")
    val offenders =
      checkedFiles.flatMap { path =>
        val rel = root.relativize(path).toString
        val text = java.nio.file.Files.readString(path)
        forbidden.flatMap(term => Option.when(text.contains(term))(s"$rel:$term"))
      }

    assertEquals(offenders, Nil)

    val semanticObservationPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservation.scala")
    val semanticObservationText = java.nio.file.Files.readString(semanticObservationPath)
    val forbiddenConstructors = List("def defenderTrade(", "def badPieceLiquidation(")
    val constructorOffenders =
      forbiddenConstructors.filter(semanticObservationText.contains)
    assertEquals(constructorOffenders, Nil)
    assert(semanticObservationText.contains("relationProjectionFromWitness"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationFocusSquaresFromWitness(witness)"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationTargetSquareFromWitness(witness)"), clues(semanticObservationPath))
    assert(!semanticObservationText.contains("relationFactTermsFromWitness(witness)"), clues(semanticObservationPath))

    val pipelineText = java.nio.file.Files.readString(checkedFiles.head)
    val forbiddenProducerNames =
      List(
        "DefenderTradeObservationProducer",
        "BadPieceLiquidationObservationProducer",
        "TacticalRelationObservationProducer"
      )
    val producerOffenders =
      forbiddenProducerNames.filter(pipelineText.contains)
    assertEquals(producerOffenders, Nil)
  }

  test("broad relation inventory is limited to catalog definition and prose helper denial") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val runtimeRoot =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis")
    val runtimeFiles =
      java.nio.file.Files
        .walk(runtimeRoot)
        .iterator()
        .asScala
        .toList
        .filter(path => path.toString.endsWith(".scala"))
    val allowedInventoryUsers =
      Set(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerProseBoundary.scala",
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/semantic/StrategicSemanticObservation.scala"
      )
    val inventoryOffenders =
      runtimeFiles.flatMap { path =>
        val rel = root.relativize(path).toString.replace('\\', '/')
        val text = java.nio.file.Files.readString(path)
        Option.when(
          text.contains("RelationObservationCatalog.InventoryKinds") &&
            !allowedInventoryUsers.contains(rel)
        )(rel)
      }
    val broadKindOffenders =
      runtimeFiles.flatMap { path =>
        val rel = root.relativize(path).toString.replace('\\', '/')
        val text = java.nio.file.Files.readString(path)
        Option.when(
          text.contains("RelationKind.All") &&
            !rel.endsWith("MoveReviewExchangeAnalyzer.scala")
        )(rel)
      }
    val proseBoundary =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerProseBoundary.scala")
      )

    assertEquals(inventoryOffenders, Nil)
    assertEquals(broadKindOffenders, Nil)
    assert(proseBoundary.contains("helper_symbol_leak_detected"), clues(proseBoundary))
    assert(!proseBoundary.contains("descriptorForEvidence"), clues(proseBoundary))
    assert(!proseBoundary.contains("descriptorForKind"), clues(proseBoundary))
  }

  test("legacy plan evidence uses deferred relation fallback labels") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val planMatcherPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlanMatcher.scala")
    val planMatcherText = java.nio.file.Files.readString(planMatcherPath)

    assert(planMatcherText.contains("deferredFallbackForKind"), clues(planMatcherPath))
    assert(!planMatcherText.contains("domination restricts enemy mobility"), clues(planMatcherPath))
  }

  test("deferred relation public prose consumers stay catalog-routed") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val requiredHooks =
      Map(
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeLexicon.scala" ->
          List("deferredMotifDeltaLabel", "deferredFallbackForMotifTag"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeMotifPrefixTable.scala" ->
          List("deferredRelationTemplates", "deferredFallbackForMotifTag"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala" ->
          List("deferredRelationCanonicalTerm", "deferredFallbackForMotifTag", "tacticalTensionMotif"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeContextBuilder.scala" ->
          List("publicThreatMotifLabel", "deferredFallbackForMotifTag"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlanMatcher.scala" ->
          List("deferredDominationLabel", "deferredFallbackForKind"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/StrategyPackBuilder.scala" ->
          List("deferredFallbackEvidenceTermForKind"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/StructurePlanArcBuilder.scala" ->
          List("deferredFallbackEvidenceTermForKind"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/UserFacingSignalSanitizer.scala" ->
          List("deferredRelationFallbackText", "deferredFallbackForKind"),
        "modules/commentaryCore/src/main/scala/lila/commentary/analysis/CommentaryIdeaSurface.scala" ->
          List("deferredFallbackForMotifTag")
      )
    val broadAdmissionTerms =
      List(
        "descriptorForEvidence",
        "descriptorForKind",
        "deferredRelationKindForMotifTag",
        "deferredFallbackLabelForMotifTag",
        "deferredFallbackLabelForKind",
        "deferredFallbackLaneForKind",
        "RelationObservationCatalog.InventoryKinds",
        "RelationKind.All",
        "RelationKind.Deferred"
      )
    val missingHooks =
      requiredHooks.toList.flatMap { case (relativePath, hooks) =>
        val path = root.resolve(relativePath)
        val text = java.nio.file.Files.readString(path)
        hooks.filterNot(text.contains).map(hook => s"$relativePath:$hook")
      }.sorted
    val broadAdmissionOffenders =
      requiredHooks.keys.toList.flatMap { relativePath =>
        val path = root.resolve(relativePath)
        val text = java.nio.file.Files.readString(path)
        broadAdmissionTerms.filter(text.contains).map(term => s"$relativePath:$term")
      }.sorted
    val rawDeferredEvidenceOffenders =
      requiredHooks.keys.toList.flatMap { relativePath =>
        val path = root.resolve(relativePath)
        val text = java.nio.file.Files.readString(path)
        List("trapped_piece_signal").filter(text.contains).map(term => s"$relativePath:$term")
      }.sorted

    assertEquals(missingHooks, Nil)
    assertEquals(broadAdmissionOffenders, Nil)
    assertEquals(rawDeferredEvidenceOffenders, Nil)
  }

  test("implemented relation catalog entries are wired to relation witness extraction") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val analyzerPath =
      root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewExchangeAnalyzer.scala")
    val analyzerText = java.nio.file.Files.readString(analyzerPath)
    val relationWitnessesStart = analyzerText.indexOf("def relationWitnesses(")
    val relationWitnessesEnd = analyzerText.indexOf(").flatten", relationWitnessesStart)
    assert(relationWitnessesStart >= 0, clues(analyzerPath))
    assert(relationWitnessesEnd > relationWitnessesStart, clues(analyzerPath))
    val relationWitnessesBody =
      analyzerText.substring(relationWitnessesStart, relationWitnessesEnd)
    val witnessHooksByKind =
      Map(
        MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade -> "defenderTradeBranch(",
        MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation -> "badPieceLiquidationBranch(",
        MoveReviewExchangeAnalyzer.RelationKind.Overload -> "overloadWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Deflection -> "deflectionWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack -> "discoveredAttackWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck -> "doubleCheckWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.BackRankMate -> "backRankMateWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.MateNet -> "mateNetWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.GreekGift -> "greekGiftWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Fork -> "forkWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.HangingPiece -> "hangingPieceWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.XRay -> "xrayWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Clearance -> "clearanceWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Battery -> "batteryWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Pin -> "pinWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Skewer -> "skewerWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Interference -> "interferenceWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Decoy -> "decoyWitness("
      )
    val missingHookDefinitions =
      witnessHooksByKind.keySet -- RelationObservationCatalog.ImplementedKinds
    val missingCatalogEntries =
      RelationObservationCatalog.ImplementedKinds -- witnessHooksByKind.keySet
    val missingExtractionHooks =
      witnessHooksByKind.toList.collect {
        case (kind, hook) if !relationWitnessesBody.contains(hook) => s"$kind:$hook"
      }.sorted
    val deferredWitnessHooks =
      Map(
        MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug -> "zwischenzugWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.Domination -> "dominationWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece -> "trappedPieceWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap -> "stalemateTrapWitness(",
        MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck -> "perpetualCheckWitness("
      )
    val deferredExtractionHooks =
      deferredWitnessHooks.toList.collect {
        case (kind, hook) if relationWitnessesBody.contains(hook) => s"$kind:$hook"
      }.sorted

    assertEquals(missingHookDefinitions, Set.empty[String])
    assertEquals(missingCatalogEntries, Set.empty[String])
    assertEquals(missingExtractionHooks, Nil)
    assertEquals(deferredExtractionHooks, Nil)
  }

  test("strategic idea evidence relation carrier admits only implemented catalog kinds") {
    val implemented =
      StrategicIdeaEvidence.from(
        ownerSide = "white",
        kind = lila.commentary.StrategicIdeaKind.LineOccupation,
        readiness = lila.commentary.StrategicIdeaReadiness.Build,
        source = StrategicObservationIds.EvidenceSourceId.XRayRelation,
        confidence = 0.72,
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = List("G6", "bad", "f5")
      )
    val deferred =
      implemented.copy(
        relationKind = None,
        relationFocusSquares = Nil
      )
    val fromDeferred =
      StrategicIdeaEvidence.from(
        ownerSide = "white",
        kind = lila.commentary.StrategicIdeaKind.LineOccupation,
        readiness = lila.commentary.StrategicIdeaReadiness.Build,
        source = StrategicObservationIds.EvidenceSourceId.XRayRelation,
        confidence = 0.72,
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug),
        relationFocusSquares = List("g6", "f5")
      )
    val fromUnknown =
      StrategicIdeaEvidence.from(
        ownerSide = "white",
        kind = lila.commentary.StrategicIdeaKind.LineOccupation,
        readiness = lila.commentary.StrategicIdeaReadiness.Build,
        source = StrategicObservationIds.EvidenceSourceId.XRayRelation,
        confidence = 0.72,
        relationKind = Some("unsupported_relation"),
        relationFocusSquares = List("g6", "f5")
      )
    val relationWithoutRelationFocus =
      StrategicIdeaEvidence.from(
        ownerSide = "white",
        kind = lila.commentary.StrategicIdeaKind.LineOccupation,
        readiness = lila.commentary.StrategicIdeaReadiness.Build,
        source = StrategicObservationIds.EvidenceSourceId.XRayRelation,
        confidence = 0.72,
        focusSquares = List("g6", "f5"),
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = Nil
      )

    assertEquals(implemented.relationKind, Some(MoveReviewExchangeAnalyzer.RelationKind.XRay))
    assertEquals(implemented.relationFocusSquares, List("g6", "f5"))
    assertEquals(fromDeferred.relationKind, deferred.relationKind)
    assertEquals(fromDeferred.relationFocusSquares, deferred.relationFocusSquares)
    assertEquals(fromUnknown.relationKind, None)
    assertEquals(fromUnknown.relationFocusSquares, Nil)
    assertEquals(relationWithoutRelationFocus.relationKind, Some(MoveReviewExchangeAnalyzer.RelationKind.XRay))
    assertEquals(relationWithoutRelationFocus.relationFocusSquares, Nil)
  }

  test("selector merge does not synthesize relation focus from generic focus squares") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val selectorText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/StrategicIdeaSelector.scala")
      )

    assert(selectorText.contains("relationFocusSquares = bestRelation.map(_.relationFocusSquares).getOrElse(Nil)"))
    assert(!selectorText.contains("else evidence.focusSquares"), clues(selectorText))
    assert(!selectorText.contains("relationFocusSquaresFor("), clues(selectorText))
  }

  test("selector merge does not synthesize relation target from generic target squares") {
    val root = java.nio.file.Paths.get("").toAbsolutePath
    val selectorText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/StrategicIdeaSelector.scala")
      )

    assert(selectorText.contains("val selectedRelationKind = bestRelation.flatMap(_.relationKind)"))
    assert(selectorText.contains("val relationTarget = bestRelation.flatMap(_.targetSquare)"))
    assert(selectorText.contains("if selectedRelationKind.nonEmpty then relationTarget"))
    assert(!selectorText.contains("relationTarget.orElse(best.targetSquare"), clues(selectorText))
  }

  test("unknown relation witnesses fail closed instead of using a selector fallback") {
    val unknown =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = "unsupported_relation",
        focusSquares = List("g6"),
        facts = List("unsupported_relation_witness"),
        lineMoves = List("b1e4")
      )

    assertEquals(RelationObservationCatalog.descriptorForKind(unknown.kind), None)
    assertEquals(StrategicSemanticObservation.relationWitness("white", unknown), None)
  }

  test("mismatched typed relation details fail closed before semantic emission") {
    val mismatched =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
        focusSquares = List("a1", "a2", "a3"),
        facts = List("pin_relation_witness", "attacker:a1", "pinned:a2", "behind:a3"),
        lineMoves = Nil,
        targetSquare = Some("a2"),
        details =
          MoveReviewExchangeAnalyzer.RelationDetails.Skewer(
            attackerSquare = "h1",
            frontSquare = "h4",
            backSquare = "h8",
            targetSquare = "h4",
            attackerRole = "rook",
            frontRole = "queen",
            backRole = "rook"
          )
      )

    assertEquals(MoveReviewExchangeAnalyzer.relationDetailsValidForKind(mismatched), false)
    assertEquals(RelationObservationCatalog.descriptorForKind(mismatched.kind).nonEmpty, true)
    assertEquals(StrategicSemanticObservation.relationWitness("white", mismatched), None)
  }

  test("semantic relation observation consumes analyzer typed details before raw witness fields") {
    val witness =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
        focusSquares = List("a1"),
        facts = List("deflection_relation_witness", "defender:a1", "defended_target:a1", "attacker:a1"),
        lineMoves = List("c1a3", "f8a3"),
        targetSquare = Some("a1"),
        details =
          MoveReviewExchangeAnalyzer.RelationDetails.Deflection(
            defenderSquare = "f8",
            targetSquare = "g7",
            attackerSquare = "a3"
          )
      )
    val observation =
      StrategicSemanticObservation.relationWitness("white", witness)

    assertEquals(observation.map(_.focusSquares), Some(List("g7", "f8", "a3")))
    assertEquals(observation.flatMap(_.targetSquare), Some("g7"))
    assertEquals(observation.exists(_.wireEvidenceRefs.contains("defender:f8")), true)
    assertEquals(observation.exists(_.wireEvidenceRefs.contains("defender:a1")), false)
    assertEquals(observation.exists(_.wireEvidenceRefs.contains("defended_target:g7")), true)
    assertEquals(observation.exists(_.wireEvidenceRefs.contains("defended_target:a1")), false)
    assertEquals(
      MoveReviewExchangeAnalyzer.relationFocusSquaresFromWitness(witness),
      List("g7", "f8", "a3")
    )
    assertEquals(
      MoveReviewExchangeAnalyzer.relationTargetSquareFromWitness(witness),
      Some("g7")
    )
  }

  test("relation witness facts cannot mint another catalog semantic admission fact") {
    val witness =
      MoveReviewExchangeAnalyzer.RelationWitness(
        kind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        focusSquares = List("b1", "d3", "g6"),
        facts = List(
          "xray_relation_witness",
          "clearance_semantic",
          "source:clearance_relation",
          "carlsbad_fixed_target_probe"
        ),
        lineMoves = List("b1g6"),
        targetSquare = Some("g6")
      )

    val observation =
      StrategicSemanticObservation.relationWitness("white", witness)

    assertEquals(observation.map(_.wireEvidenceRefs.contains("xray_semantic")), Some(true))
    assertEquals(observation.map(_.wireEvidenceRefs.contains("xray_relation_witness")), Some(true))
    assertEquals(observation.map(_.wireEvidenceRefs.contains("clearance_semantic")), Some(false))
    assertEquals(observation.map(_.wireEvidenceRefs.contains("source:clearance_relation")), Some(false))
    assertEquals(observation.map(_.wireEvidenceRefs.contains("carlsbad_fixed_target_probe")), Some(false))
  }
