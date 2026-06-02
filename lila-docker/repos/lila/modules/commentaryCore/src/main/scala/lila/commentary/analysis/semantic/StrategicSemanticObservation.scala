package lila.commentary.analysis.semantic

import lila.commentary.analysis.structure.StructuralDelta
import lila.commentary.analysis.StrategicConceptSemantics
import lila.commentary.analysis.MoveReviewExchangeAnalyzer
import lila.commentary.analysis.semantic.StrategicObservationIds.*
import lila.commentary.{ StrategicIdeaKind, StrategicIdeaReadiness, StrategyRelationSupport }

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
    proofFamily: Option[ProofFamilyId] = None,
    relationSupport: Option[StrategyRelationSupport] = None
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
      facts: List[FactId] = Nil,
      relationSupport: Option[StrategyRelationSupport] = None
  ): StrategicSemanticObservation =
    StrategicSemanticObservation(
      id = id,
      ownerSide = ownerSide,
      focusSquares = focusSquares.distinct,
      focusZone = focusZone,
      targetSquare = targetSquare,
      source = Some(source),
      facts = facts.distinct,
      role = StrategicSemanticObservationRole.SelectorSource,
      relationSupport = relationSupport
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
            projection.factTerms.flatMap(FactId.dynamic),
        relationSupport = projection.support
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
    ideaKind: String,
    readiness: String,
    confidence: Double,
    publicLabel: String,
    surfaceRowLabel: String,
    beneficiaryPieces: List[String] = Nil,
    surfaceTargetFocus: RelationSurfaceTargetFocus = RelationSurfaceTargetFocus.Last,
    selectorPriority: Int = 0
):
  def sourceRef: EvidenceRef =
    EvidenceRef.Source(source)

  def semanticFact: FactId =
    FactId.semantic(observationId)

  def semanticRef: EvidenceRef =
    EvidenceRef.Fact(semanticFact)

  def evidenceRefs: List[EvidenceRef] =
    List(sourceRef, semanticRef)

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
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.78,
        publicLabel = "bad-piece liquidation",
        surfaceRowLabel = "Tactical relation",
        beneficiaryPieces = List("B"),
        selectorPriority = 2
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
        observationId = SemanticObservationId.OverloadSemantic,
        source = EvidenceSourceId.OverloadRelation,
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
        ideaKind = StrategicIdeaKind.KingAttackBuildUp,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.74,
        publicLabel = "Greek gift",
        surfaceRowLabel = "Tactical relation",
        beneficiaryPieces = List("B")
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
        observationId = SemanticObservationId.ZwischenzugSemantic,
        source = EvidenceSourceId.ZwischenzugRelation,
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.69,
        publicLabel = "zwischenzug",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First,
        selectorPriority = 2
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
        observationId = SemanticObservationId.ForkSemantic,
        source = EvidenceSourceId.ForkRelation,
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
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.68,
        publicLabel = "trapped piece",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First,
        selectorPriority = 1
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
        observationId = SemanticObservationId.DominationSemantic,
        source = EvidenceSourceId.DominationRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.66,
        publicLabel = "domination",
        surfaceRowLabel = "Restriction relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
        observationId = SemanticObservationId.StalemateTrapSemantic,
        source = EvidenceSourceId.StalemateTrapRelation,
        ideaKind = StrategicIdeaKind.FavorableTradeOrTransformation,
        readiness = StrategicIdeaReadiness.Ready,
        confidence = 0.76,
        publicLabel = "stalemate trap",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First,
        selectorPriority = 2
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
        observationId = SemanticObservationId.PerpetualCheckSemantic,
        source = EvidenceSourceId.PerpetualCheckRelation,
        ideaKind = StrategicIdeaKind.KingAttackBuildUp,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.72,
        publicLabel = "perpetual check",
        surfaceRowLabel = "Tactical relation",
        surfaceTargetFocus = RelationSurfaceTargetFocus.First,
        selectorPriority = 2
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
        observationId = SemanticObservationId.XRaySemantic,
        source = EvidenceSourceId.XRayRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.68,
        publicLabel = "x-ray",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
        observationId = SemanticObservationId.ClearanceSemantic,
        source = EvidenceSourceId.ClearanceRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.70,
        publicLabel = "clearance",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
        observationId = SemanticObservationId.BatterySemantic,
        source = EvidenceSourceId.BatteryRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.69,
        publicLabel = "battery",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
        observationId = SemanticObservationId.PinSemantic,
        source = EvidenceSourceId.PinRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.66,
        publicLabel = "pin",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
        observationId = SemanticObservationId.SkewerSemantic,
        source = EvidenceSourceId.SkewerRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.67,
        publicLabel = "skewer",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Interference,
        observationId = SemanticObservationId.InterferenceSemantic,
        source = EvidenceSourceId.InterferenceRelation,
        ideaKind = StrategicIdeaKind.LineOccupation,
        readiness = StrategicIdeaReadiness.Build,
        confidence = 0.67,
        publicLabel = "interference",
        surfaceRowLabel = "Line relation"
      ),
      RelationObservationDescriptor(
        relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
        observationId = SemanticObservationId.DecoySemantic,
        source = EvidenceSourceId.DecoyRelation,
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
        val deferred = normalizeMotifTag(kind)
        val compactMotif = motif.replace("_", "")
        val compactDeferred = deferred.replace("_", "")
        motif == deferred ||
          compactMotif == compactDeferred ||
          motif.startsWith(s"${deferred}_")
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

  private def normalizeMotifTag(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
