package lila.commentary.analysis.semantic

import lila.commentary.analysis.structure.StructuralDelta
import lila.commentary.analysis.StrategicConceptSemantics
import lila.commentary.analysis.MoveReviewExchangeAnalyzer
import lila.commentary.analysis.semantic.StrategicObservationIds.*
import lila.commentary.{ StrategicIdeaKind, StrategicIdeaReadiness }

private[commentary] enum StrategicSemanticObservationRole:
  case SupportOnly
  case SelectorSource

private[commentary] final case class StrategicSemanticObservation(
    id: SemanticObservationId,
    ownerSide: String,
    focusSquares: List[String] = Nil,
    focusZone: Option[String] = None,
    targetSquare: Option[String] = None,
    source: Option[EvidenceSourceId] = None,
    facts: List[FactId] = Nil,
    role: StrategicSemanticObservationRole = StrategicSemanticObservationRole.SupportOnly,
    proofSource: Option[ProofSourceId] = None,
    proofFamily: Option[ProofFamilyId] = None
):
  def supportOnly: Boolean =
    role == StrategicSemanticObservationRole.SupportOnly

  def evidenceRefs: List[EvidenceRef] =
    source.map(EvidenceRef.Source(_)).toList ++ facts.distinct.map(EvidenceRef.Fact(_))

  def wireEvidenceRefs: List[String] =
    evidenceRefs.map(_.wireKey).distinct

private[commentary] object StrategicSemanticObservation:

  def supportOnly(
      id: SemanticObservationId,
      ownerSide: String,
      focusSquares: List[String] = Nil,
      focusZone: Option[String] = None,
      targetSquare: Option[String] = None,
      facts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    StrategicSemanticObservation(
      id = id,
      ownerSide = ownerSide,
      focusSquares = focusSquares.distinct,
      focusZone = focusZone,
      targetSquare = targetSquare,
      facts = facts.distinct,
      role = StrategicSemanticObservationRole.SupportOnly
    )

  def selectorSource(
      id: SemanticObservationId,
      source: EvidenceSourceId,
      ownerSide: String,
      focusSquares: List[String] = Nil,
      focusZone: Option[String] = None,
      targetSquare: Option[String] = None,
      facts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    StrategicSemanticObservation(
      id = id,
      ownerSide = ownerSide,
      focusSquares = focusSquares.distinct,
      focusZone = focusZone,
      targetSquare = targetSquare,
      source = Some(source),
      facts = facts.distinct,
      role = StrategicSemanticObservationRole.SelectorSource
    )

  def minorityAttack(
      ownerSide: String,
      focusSquares: List[String],
      focusZone: Option[String],
      targets: List[String],
      structuralDelta: Option[StructuralDelta],
      supportFacts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    selectorSource(
      id = SemanticObservationId.MinorityAttackSemantic,
      source = EvidenceSourceId.MinorityAttackSemantic,
      ownerSide = ownerSide,
      focusSquares = focusSquares,
      focusZone = focusZone,
      facts =
        FactId.semantic(SemanticObservationId.MinorityAttackSemantic) ::
          (targetPressureFacts(structuralDelta, targets) ++ supportFacts).distinct
    )

  def minorityAttackFromConcept(
      ownerSide: String,
      focusSquares: List[String],
      observation: StrategicConceptSemantics.StrategicConceptObservation
  ): StrategicSemanticObservation =
    val deltaFacts =
      observation.structuralDelta.toList.flatMap(delta =>
        delta.createdTension.flatMap(edge => FactId.dynamic(s"created_tension_$edge")) ++
          delta.resolvedTension.flatMap(edge => FactId.dynamic(s"resolved_tension_$edge")) ++
          delta.newWeakPawns.flatMap(square => FactId.dynamic(s"new_weak_pawn_$square")) ++
          delta.openedFiles.flatMap(file => FactId.dynamic(s"opened_file_$file")) ++
          delta.semiOpenedFiles.flatMap(file => FactId.dynamic(s"semi_opened_file_$file")) ++
          Option
            .when(delta.targetPressureDelta > 0)(s"target_pressure_delta_${delta.targetPressureDelta}")
            .flatMap(FactId.dynamic)
            .toList ++
          Option
            .when(delta.fileAccessDelta > 0)(s"file_access_delta_${delta.fileAccessDelta}")
            .flatMap(FactId.dynamic)
            .toList
      )
      .distinct
    minorityAttack(
      ownerSide = ownerSide,
      focusSquares = focusSquares,
      focusZone = Some(observation.wing),
      targets = observation.targets,
      structuralDelta = observation.structuralDelta,
      supportFacts =
        FactId.dynamic(s"minority_attack_${observation.wing}").toList ++
          observation.primaryBreak.flatMap(move => FactId.dynamic(s"minority_break_$move")).toList ++
          observation.essentialEvidence.flatMap(evidence => FactId.dynamic(s"concept_${evidence.id}")) ++
          deltaFacts
    )

  def relationWitness(
      ownerSide: String,
      witness: MoveReviewExchangeAnalyzer.RelationWitness
  ): Option[StrategicSemanticObservation] =
    for
      projection <-
        MoveReviewExchangeAnalyzer
          .relationProjectionFromWitness(witness)
      descriptor <-
        RelationObservationCatalog
          .descriptorForKind(projection.kind)
    yield
      selectorSource(
        id = descriptor.observationId,
        source = descriptor.source,
        ownerSide = ownerSide,
        focusSquares = projection.focusSquares,
        targetSquare = projection.targetSquare,
        facts =
          descriptor.semanticFact ::
            projection.factTerms.flatMap(FactId.dynamic)
      )

  def targetPressureFacts(
      structuralDelta: Option[StructuralDelta],
      targets: List[String]
  ): List[FactId] =
    structuralDelta.toList
      .filter(delta =>
        delta.createdTension.nonEmpty ||
          delta.newWeakPawns.nonEmpty ||
          delta.newWeakSquares.nonEmpty ||
          delta.targetPressureDelta > 0
      )
      .flatMap(delta =>
        List(FactId.semantic(SemanticObservationId.TargetPressureSemantic)) ++
          targets.take(3).flatMap(target => FactId.dynamic(s"target_pressure_target_$target")) ++
          delta.createdTension.flatMap(edge => FactId.dynamic(s"target_pressure_created_tension_$edge")) ++
          delta.newWeakPawns.flatMap(square => FactId.dynamic(s"target_pressure_new_weak_pawn_$square")) ++
          delta.newWeakSquares.flatMap(square => FactId.dynamic(s"target_pressure_new_weak_square_$square")) ++
          Option
            .when(delta.targetPressureDelta > 0)(s"target_pressure_delta_${delta.targetPressureDelta}")
            .flatMap(FactId.dynamic)
            .toList
      )

private[commentary] enum RelationSurfaceTargetFocus:
  case First
  case SecondOrLast
  case Last

private[commentary] enum RelationSurfaceRowKind:
  case TacticalRelation
  case DrawResource
  case MoveOrder
  case MobilityRestriction
  case LineGeometry

private[commentary] enum DeferredRelationFallbackLane:
  case ExchangeSequence
  case MaterialTransition
  case PracticalGuidance
  case ThematicFallback
  case DiagnosticOnly

private[commentary] final case class RelationObservationDescriptor(
    relationKind: String,
    observationId: SemanticObservationId,
    source: EvidenceSourceId,
    requiredWitnessTerm: String,
    ideaKind: String,
    readiness: String,
    confidence: Double,
    publicLabel: String,
    surfaceRowLabel: String,
    surfaceRowKind: RelationSurfaceRowKind = RelationSurfaceRowKind.TacticalRelation,
    witnessOnlyMotifTag: Boolean = false,
    witnessOnlyFallbackLabel: Option[String] = None,
    beneficiaryPieces: List[String] = Nil,
    surfaceTargetFocus: RelationSurfaceTargetFocus = RelationSurfaceTargetFocus.Last,
    surfacePriority: Int = 50
):
  def sourceRef: EvidenceRef =
    EvidenceRef.Source(source)

  def semanticFact: FactId =
    FactId.semantic(observationId)

  def semanticRef: EvidenceRef =
    EvidenceRef.Fact(semanticFact)

  def witnessFact: Option[FactId] =
    FactId.dynamic(requiredWitnessTerm)

  def evidenceRefs: List[EvidenceRef] =
    List(sourceRef, semanticRef) ++ witnessFact.map(fact => EvidenceRef.Fact(fact))

  def wireEvidenceRefs: List[String] =
    evidenceRefs.map(_.wireKey)

  def fallbackTarget(focusSquares: List[String]): Option[String] =
    surfaceTargetFocus match
      case RelationSurfaceTargetFocus.First        => focusSquares.headOption
      case RelationSurfaceTargetFocus.SecondOrLast => focusSquares.lift(1).orElse(focusSquares.lastOption)
      case RelationSurfaceTargetFocus.Last         => focusSquares.lastOption

private[commentary] final case class DeferredRelationDescriptor(
    relationKind: String,
    internalLabel: String,
    requiredWitness: String,
    deferReason: String,
    fallbackLane: DeferredRelationFallbackLane,
    fallbackLabel: Option[String],
    fallbackRationale: String
)

private[commentary] final case class DeferredRelationFallback(
    relationKind: String,
    lane: DeferredRelationFallbackLane,
    label: Option[String],
    rationale: String
):
  def allowsNonRelationText: Boolean =
    lane != DeferredRelationFallbackLane.DiagnosticOnly

  def diagnosticOnly: Boolean =
    lane == DeferredRelationFallbackLane.DiagnosticOnly

private[commentary] object RelationObservationCatalog:

  val Deferred: List[DeferredRelationDescriptor] =
    Nil

  val Implemented: List[RelationObservationDescriptor] =
    List(
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
        observationId = SemanticObservationId.DefenderTradeSemantic,
        source = EvidenceSourceId.RemovingTheDefender,
        requiredWitnessTerm = "defender_trade_branch",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.82,
        publicLabel = "defender-trade",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation,
        observationId = SemanticObservationId.BadPieceLiquidationSemantic,
        source = EvidenceSourceId.CaptureExchangeTransformation,
        requiredWitnessTerm = "bad_piece_liquidation_branch",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.78,
        publicLabel = "bad-piece liquidation",
        surfaceRowLabel = "Tactical relation",
        beneficiaryPieces = List("B")
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
        observationId = SemanticObservationId.OverloadSemantic,
        source = EvidenceSourceId.OverloadRelation,
        requiredWitnessTerm = "overload_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.70,
        publicLabel = "overload",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
        observationId = SemanticObservationId.DeflectionSemantic,
        source = EvidenceSourceId.DeflectionRelation,
        requiredWitnessTerm = "deflection_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.72,
        publicLabel = "deflection",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
        observationId = SemanticObservationId.DiscoveredAttackSemantic,
        source = EvidenceSourceId.DiscoveredAttackRelation,
        requiredWitnessTerm = "discovered_attack_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.72,
        publicLabel = "discovered-attack",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
        observationId = SemanticObservationId.DoubleCheckSemantic,
        source = EvidenceSourceId.DoubleCheckRelation,
        requiredWitnessTerm = "double_check_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.73,
        publicLabel = "double-check",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
        observationId = SemanticObservationId.BackRankMateSemantic,
        source = EvidenceSourceId.BackRankMateRelation,
        requiredWitnessTerm = "back_rank_mate_relation_witness",
        ideaKind = StrategicIdeaKind.KingAttackBuildUp,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.76,
        publicLabel = "back-rank mate",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.MateNet,
        observationId = SemanticObservationId.MateNetSemantic,
        source = EvidenceSourceId.MateNet,
        requiredWitnessTerm = "mate_net_relation_witness",
        ideaKind = StrategicIdeaKind.KingAttackBuildUp,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.75,
        publicLabel = "mate net",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
        observationId = SemanticObservationId.GreekGiftSemantic,
        source = EvidenceSourceId.GreekGiftRelation,
        requiredWitnessTerm = "greek_gift_relation_witness",
        ideaKind = StrategicIdeaKind.KingAttackBuildUp,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.74,
        publicLabel = "Greek gift",
        surfaceRowLabel = "Tactical relation",
        beneficiaryPieces = List("B")
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        observationId = SemanticObservationId.StalemateTrapSemantic,
        source = EvidenceSourceId.StalemateTrapRelation,
        requiredWitnessTerm = "stalemate_trap_relation_witness",
        ideaKind = StrategicIdeaKind.CounterplaySuppression,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.74,
        publicLabel = "stalemate resource",
        surfaceRowLabel = "Draw resource",
        surfaceRowKind = RelationSurfaceRowKind.DrawResource,
        witnessOnlyMotifTag = true,
        surfacePriority = 10
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
        observationId = SemanticObservationId.PerpetualCheckSemantic,
        source = EvidenceSourceId.PerpetualCheckRelation,
        requiredWitnessTerm = "perpetual_check_relation_witness",
        ideaKind = StrategicIdeaKind.CounterplaySuppression,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.73,
        publicLabel = "perpetual-check resource",
        surfaceRowLabel = "Draw resource",
        surfaceRowKind = RelationSurfaceRowKind.DrawResource,
        witnessOnlyMotifTag = true,
        surfacePriority = 10
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
        observationId = SemanticObservationId.ForkSemantic,
        source = EvidenceSourceId.ForkRelation,
        requiredWitnessTerm = "fork_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.71,
        publicLabel = "fork",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
        observationId = SemanticObservationId.HangingPieceSemantic,
        source = EvidenceSourceId.HangingPieceRelation,
        requiredWitnessTerm = "hanging_piece_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.69,
        publicLabel = "hanging piece",
        surfaceRowLabel = "Tactical relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
        observationId = SemanticObservationId.TrappedPieceSemantic,
        source = EvidenceSourceId.TrappedPieceRelation,
        requiredWitnessTerm = "trapped_piece_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.70,
        publicLabel = "trapped-piece",
        surfaceRowLabel = "Mobility restriction",
        surfaceRowKind = RelationSurfaceRowKind.MobilityRestriction,
        witnessOnlyMotifTag = true,
        witnessOnlyFallbackLabel = Some("piece mobility"),
        surfacePriority = 20
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
        observationId = SemanticObservationId.DominationSemantic,
        source = EvidenceSourceId.DominationRelation,
        requiredWitnessTerm = "domination_relation_witness",
        ideaKind = StrategicIdeaKind.CounterplaySuppression,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.66,
        publicLabel = "key-square restriction",
        surfaceRowLabel = "Mobility restriction",
        surfaceRowKind = RelationSurfaceRowKind.MobilityRestriction,
        witnessOnlyMotifTag = true,
        witnessOnlyFallbackLabel = Some("key-square restriction"),
        surfacePriority = 20
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
        observationId = SemanticObservationId.ZwischenzugSemantic,
        source = EvidenceSourceId.ZwischenzugRelation,
        requiredWitnessTerm = "zwischenzug_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.67,
        publicLabel = "zwischenzug",
        surfaceRowLabel = "Move-order relation",
        surfaceRowKind = RelationSurfaceRowKind.MoveOrder,
        witnessOnlyMotifTag = true,
        witnessOnlyFallbackLabel = Some("move-order caution"),
        surfacePriority = 20
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        observationId = SemanticObservationId.XRaySemantic,
        source = EvidenceSourceId.XRayRelation,
        requiredWitnessTerm = "xray_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.68,
        publicLabel = "x-ray",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        observationId = SemanticObservationId.ClearanceSemantic,
        source = EvidenceSourceId.ClearanceRelation,
        requiredWitnessTerm = "clearance_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.70,
        publicLabel = "clearance",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
        observationId = SemanticObservationId.BatterySemantic,
        source = EvidenceSourceId.BatteryRelation,
        requiredWitnessTerm = "battery_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.69,
        publicLabel = "battery",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
        observationId = SemanticObservationId.PinSemantic,
        source = EvidenceSourceId.PinRelation,
        requiredWitnessTerm = "pin_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.66,
        publicLabel = "pin",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
        observationId = SemanticObservationId.SkewerSemantic,
        source = EvidenceSourceId.SkewerRelation,
        requiredWitnessTerm = "skewer_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.67,
        publicLabel = "skewer",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
        observationId = SemanticObservationId.InterferenceSemantic,
        source = EvidenceSourceId.InterferenceRelation,
        requiredWitnessTerm = "interference_relation_witness",
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.67,
        publicLabel = "interference",
        surfaceRowLabel = "Line relation",
        surfaceRowKind = RelationSurfaceRowKind.LineGeometry
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
        observationId = SemanticObservationId.DecoySemantic,
        source = EvidenceSourceId.DecoyRelation,
        requiredWitnessTerm = "decoy_relation_witness",
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.69,
        publicLabel = "decoy",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.SecondOrLast
      )
    )

  val DeferredRelationKinds: List[String] =
    Deferred.map(_.relationKind)

  val ImplementedKinds: Set[String] =
    Implemented.map(_.relationKind).toSet

  val InventoryKinds: List[String] =
    Implemented.map(_.relationKind) ++ DeferredRelationKinds

  private val byKind: Map[String, RelationObservationDescriptor] =
    Implemented.map(descriptor => descriptor.relationKind -> descriptor).toMap

  private val deferredByKind: Map[String, DeferredRelationDescriptor] =
    Deferred.map(descriptor => descriptor.relationKind -> descriptor).toMap

  private val byObservationId: Map[SemanticObservationId, RelationObservationDescriptor] =
    Implemented.map(descriptor => descriptor.observationId -> descriptor).toMap

  def descriptorForKind(kind: String): Option[RelationObservationDescriptor] =
    byKind.get(kind)

  def isImplementedKind(kind: String): Boolean =
    ImplementedKinds.contains(kind)

  def isDeferredKind(kind: String): Boolean =
    deferredByKind.contains(kind)

  def deferredDescriptorForKind(kind: String): Option[DeferredRelationDescriptor] =
    deferredByKind.get(kind)

  def deferredRelationKindForMotifTag(raw: String): Option[String] =
    val motif = normalizeMotifTag(raw)
    if motif.isEmpty then None
    else
      DeferredRelationKinds.find { kind =>
        motifTagMatchesKind(motif, kind)
      }

  def deferredFallbackForKind(kind: String): Option[DeferredRelationFallback] =
    deferredDescriptorForKind(kind).map(descriptor =>
      DeferredRelationFallback(
        relationKind = descriptor.relationKind,
        lane = descriptor.fallbackLane,
        label = descriptor.fallbackLabel.map(_.trim).filter(_.nonEmpty),
        rationale = descriptor.fallbackRationale
      )
    )

  def deferredFallbackForMotifTag(raw: String): Option[DeferredRelationFallback] =
    deferredRelationKindForMotifTag(raw).flatMap(deferredFallbackForKind)

  def pvDrawResourceOnlyMotifTag(raw: String): Boolean =
    implementedDescriptorForMotifTag(raw)(_.surfaceRowKind == RelationSurfaceRowKind.DrawResource).nonEmpty

  def relationWitnessOnlyMotifTag(raw: String): Boolean =
    implementedDescriptorForMotifTag(raw)(_.witnessOnlyMotifTag).nonEmpty

  def relationWitnessOnlyFallbackLabelForMotifTag(raw: String): Option[String] =
    implementedDescriptorForMotifTag(raw)(_.witnessOnlyMotifTag)
      .flatMap(_.witnessOnlyFallbackLabel.map(_.trim).filter(_.nonEmpty))

  private def implementedDescriptorForMotifTag(
      raw: String
  )(matches: RelationObservationDescriptor => Boolean): Option[RelationObservationDescriptor] =
    val motif = normalizeMotifTag(raw)
    if motif.isEmpty then None
    else
      Implemented
        .find(descriptor =>
          matches(descriptor) &&
            motifTagMatchesKind(motif, descriptor.relationKind)
        )

  private def motifTagMatchesKind(normalizedMotif: String, kind: String): Boolean =
    val normalizedKind = normalizeMotifTag(kind)
    val compactMotif = normalizedMotif.replace("_", "")
    val compactKind = normalizedKind.replace("_", "")
    normalizedKind.nonEmpty &&
      (normalizedMotif == normalizedKind ||
        compactMotif == compactKind ||
        normalizedMotif.startsWith(s"${normalizedKind}_"))

  def deferredFallbackEvidenceTermForKind(kind: String): Option[String] =
    deferredFallbackForKind(kind)
      .filter(_.allowsNonRelationText)
      .flatMap(_.label)
      .map(label => s"deferred_${normalizeMotifTag(label)}")
      .filterNot(_ == "deferred_")

  def allowsDeferredNonRelationFallback(kind: String): Boolean =
    deferredFallbackForKind(kind).exists(_.allowsNonRelationText)

  def isDiagnosticOnlyDeferred(kind: String): Boolean =
    deferredFallbackForKind(kind).exists(_.diagnosticOnly)

  def descriptorForObservationId(id: SemanticObservationId): Option[RelationObservationDescriptor] =
    byObservationId.get(id)

  def descriptorForEvidence(
      relationKind: Option[String],
      evidenceRefs: List[String]
  ): Option[RelationObservationDescriptor] =
    relationKind match
      case Some(kind) =>
        descriptorForKind(kind).filter(descriptor => evidenceRefsContain(descriptor, evidenceRefs))
      case None => None

  def evidenceRefsContain(
      descriptor: RelationObservationDescriptor,
      evidenceRefs: List[String]
  ): Boolean =
    descriptor.wireEvidenceRefs.forall(evidenceRefs.contains)

  private def normalizeMotifTag(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
