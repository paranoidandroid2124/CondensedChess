package lila.commentary.analysis

import lila.commentary.MoveReviewSurfaceAuthority
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.{ ConfidenceLevel, DecisionRationale, NarrativeContext, PVDelta, TargetSquare }
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import munit.FunSuite

final class MoveReviewSupportedLocalSurfaceRowsTest extends FunSuite:

  private val ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx
  private val neutralizeCtx =
    ctx.copy(
      fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
      playedMove = Some("e2e3"),
      playedSan = Some("e3")
    )
  private val collisionCtx =
    ctx.copy(
      fen = "2b1k3/8/8/8/8/8/8/4K3 b - - 0 1",
      playedMove = Some("c8g4"),
      playedSan = Some("Bg4")
    )
  private val sanOnlyCollisionCtx =
    collisionCtx.copy(playedMove = None)
  private val playedDestinationRouteCtx =
    ctx.copy(
      playedMove = Some("e7e5"),
      playedSan = Some("e5")
    )
  private val quietKnightCtx =
    ctx.copy(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3")
    )
  private val MadernaExactFen =
    "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17"
  private val MadernaExactLines =
    List(
      VariationLine(List("e4e5", "d6e5", "f4e5", "d7e5", "c4e5"), scoreCp = 82, depth = 18),
      VariationLine(List("c1e3", "b7b5", "a5b6"), scoreCp = 36, depth = 18)
    )
  private val DirectBreakFen =
    "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"
  private val NoRankedPlans = RankedQuestionPlans(primary = None, secondary = None, rejected = Nil)

  private def withFocalTarget(base: NarrativeContext, square: String): NarrativeContext =
    val decision =
      base.decision
        .getOrElse(DecisionRationale(None, "", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Heuristic))
        .copy(focalPoint = Some(TargetSquare(square)))
    base.copy(decision = Some(decision))

  private def supportedClaimPacket(
      proofFamily: String,
      proofSource: String,
      triggerKind: String,
      anchorTerms: List[String],
      ownerSeedTerms: List[String],
      continuationTerms: List[String],
      structureTransitionTerms: List[String],
      bestDefenseMove: Option[String],
      bestDefenseBranchKey: Option[String],
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.MoveLocal,
      sameBranchState: PlayerFacingSameBranchState,
      persistence: PlayerFacingClaimPersistence,
      releaseRisks: List[String],
      suppressionReasons: List[String],
      fallbackMode: PlayerFacingClaimFallbackMode,
      exactSliceProof: Option[PlayerFacingExactSliceProof] = None,
      attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
      ontologyFamily: PlayerFacingClaimOntologyKind = PlayerFacingClaimOntologyKind.Unknown
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = attributionGrade,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = ontologyFamily
        ),
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = triggerKind,
      anchorTerms = anchorTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = bestDefenseBranchKey,
      sameBranchState = sameBranchState,
      persistence = persistence,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = ownerSeedTerms,
          continuationTerms = continuationTerms,
          structureTransitionTerms = structureTransitionTerms,
          exactSliceProof = exactSliceProof
        ),
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedNeutralizePacket(
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      proofSource: String = ProofSourceId.CounterplayAxisSuppression.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      scope: PlayerFacingPacketScope = PlayerFacingPacketScope.MoveLocal,
      anchorTerms: List[String] = List("...c5"),
      ownerSeedTerms: List[String] = List("neutralize_key_break", "...c5"),
      structureTransitionTerms: List[String] = List("...c5"),
      bestDefenseMove: Option[String] = Some("c5"),
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...c5"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = PlanTaxonomy.PlanKind.BreakPrevention.id,
      anchorTerms = anchorTerms,
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("counterplay_axis_suppression", "e4d5", "c6d5"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = bestDefenseMove,
      bestDefenseBranchKey = Some("e4d5|c6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedColorComplexPacket(
      proofFamily: String = ProofFamilyId.ColorComplexSqueeze.wireKey,
      proofSource: String = ProofSourceId.ColorComplexSqueezeProbe.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "dark", "knight", "c4"))
  ): PlayerFacingClaimPacket =
    supportedPositionProbePacket(
      proofFamily = proofFamily,
      proofSource = proofSource,
      anchorSquare = "e5",
      ownerSeedTerms =
        List(
          "e5",
          "weak_square:e5",
          "color_complex:dark",
          "minor_piece:knight_c4",
          "attacks:e5",
          "minor_piece_attack:c4-e5",
          proofSource,
          proofFamily
        ),
      continuationTerms = List("color_complex_squeeze_probe", "weak_square:e5", "best_branch:c4e5|e8f8"),
      structureTransitionTerms = List("color_complex_squeeze_probe", "weak_square:e5", "minor_piece_attack:c4-e5"),
      exactSliceProof = exactSliceProof,
      sameBranchState = sameBranchState,
      persistence = persistence,
      releaseRisks = releaseRisks,
      suppressionReasons = suppressionReasons,
      fallbackMode = fallbackMode
    )

  private def supportedPositionProbePacket(
      proofFamily: String,
      proofSource: String,
      anchorSquare: String,
      ownerSeedTerms: List[String],
      continuationTerms: List[String],
      structureTransitionTerms: List[String],
      exactSliceProof: Option[PlayerFacingExactSliceProof],
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = PlayerFacingPacketScope.PositionLocal,
      triggerKind = "position_probe",
      anchorTerms = List(anchorSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = continuationTerms,
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e8f8"),
      bestDefenseBranchKey = Some("c4e5|e8f8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.ColorComplexSqueeze
    )

  private def supportedLocalFilePacket(
      proofFamily: String = ProofFamilyId.HalfOpenFilePressure.wireKey,
      proofSource: String = ProofSourceId.LocalFileEntryBind.wireKey,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = "file_entry_denial",
      anchorTerms = List("c-file", "c6"),
      ownerSeedTerms = List("c-file", "c6"),
      continuationTerms = List("local_file_entry_bind", "c-file", "c6", "d4c6", "c7c6"),
      structureTransitionTerms = List("file-entry:c-file:c6"),
      bestDefenseMove = Some("c7c6"),
      bestDefenseBranchKey = Some("d4c6|c7c6"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedOutpostPacket(
      proofFamily: String = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily,
      proofSource: String = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource,
      square: String = "e5",
      ownerSeedTerms: List[String] = List("e5", "outpost:e5", "piece:knight", "outpost_occupation:knight:e5"),
      structureTransitionTerms: List[String] =
        List("outpost_occupation", "outpost:e5", "piece:knight", "outpost_occupation:knight:e5"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.OutpostEntrenchment.id,
      anchorTerms = List(square),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("outpost:e5", "best_branch:g4e5|e8f8", "e8f8"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e8f8"),
      bestDefenseBranchKey = Some("g4e5|e8f8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.Access
    )

  private def supportedIqpPacket(
      proofFamily: String = PlanTaxonomy.PlanKind.IQPInducement.id,
      proofSource: String = ProofSourceId.IQPInducementProbe.wireKey,
      targetSquare: String = "d5",
      structureTransitionTerms: List[String] = List("before_not_isolated:d5", "after_isolated:d5", "central_isolated_pawn"),
      ownerSeedTerms: List[String] = List("d5", "isolated_pawn:d5", "iqp_inducement"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Missing,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.IQPInducement.id,
      anchorTerms = List(targetSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = List("iqp_inducement_branch", "c4d5", "e6d5"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("e6d5"),
      bestDefenseBranchKey = Some("c4d5|e6d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedSimplificationPacket(
      proofFamily: String = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      proofSource: String = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      exchangeSquare: String = "d5",
      continuationTerms: List[String] = List("exact_trade_continuation", "exchange_square:d5", "b5d5", "f7d5"),
      structureTransitionTerms: List[String] = List("target_complex:d5", "simplification_window"),
      ownerSeedTerms: List[String] = List("simplification_window", "d5", "local_edge"),
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.SimplificationWindow.id,
      anchorTerms = List(exchangeSquare),
      ownerSeedTerms = ownerSeedTerms,
      continuationTerms = continuationTerms,
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("f7d5"),
      bestDefenseBranchKey = Some("b5d5|f7d5"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def supportedCounterplayRestraintPacket(
      proofFamily: String = ProofFamilyId.CounterplayRestraint.wireKey,
      proofSource: String = ProofSourceId.ProphylacticMove.wireKey,
      resourceToken: String = "denied_resource:break",
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      exactSliceProof: Option[PlayerFacingExactSliceProof] =
        Some(PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:break"))
  ): PlayerFacingClaimPacket =
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = PlanTaxonomy.PlanKind.ProphylaxisRestraint.id,
      anchorTerms = List(resourceToken),
      ownerSeedTerms = List(resourceToken, "prophylactic_move", "counterplay_restraint"),
      continuationTerms = List("counterplay_restraint", "f8e8", "c1c8"),
      structureTransitionTerms = List(resourceToken),
      bestDefenseMove = Some("f8e8"),
      bestDefenseBranchKey = Some("f8e8|c1c8"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode,
      attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
      ontologyFamily = PlayerFacingClaimOntologyKind.CounterplayRestraint
    )

  private def supportedExchangePacket(
      planKind: PlanTaxonomy.PlanKind,
      proofSource: String,
      structureTransitionTerms: List[String],
      exactSliceProof: Option[PlayerFacingExactSliceProof] = None,
      sameBranchState: PlayerFacingSameBranchState = PlayerFacingSameBranchState.Proven,
      persistence: PlayerFacingClaimPersistence = PlayerFacingClaimPersistence.Stable,
      releaseRisks: List[String] = Nil,
      suppressionReasons: List[String] = Nil,
      fallbackMode: PlayerFacingClaimFallbackMode = PlayerFacingClaimFallbackMode.WeakMain
  ): PlayerFacingClaimPacket =
    val proofFamily = ProofFamilyId.fromPlanKind(planKind).map(_.wireKey).getOrElse(planKind.id)
    supportedClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      triggerKind = planKind.id,
      anchorTerms = List(planKind.id),
      ownerSeedTerms = List(planKind.id, "local_branch"),
      continuationTerms = List(planKind.id, "d4c6", "d7c6"),
      structureTransitionTerms = structureTransitionTerms,
      bestDefenseMove = Some("d4c6"),
      bestDefenseBranchKey = Some("d4c6|d7c6"),
      sameBranchState = sameBranchState,
      persistence = persistence,
      exactSliceProof = exactSliceProof,
      suppressionReasons = suppressionReasons,
      releaseRisks = releaseRisks,
      fallbackMode = fallbackMode
    )

  private def mainClaim(
      packet: PlayerFacingClaimPacket,
      claimText: String
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.CounterplayReduction),
      claimText = claimText,
      anchorTerms = List("...c5"),
      evidenceLines = List("14.e4 c5 15.d5"),
      sourceKind = "prevented_plan",
      tacticalOwnership = None,
      packet = Some(packet)
    )

  private def inputs(
      packet: PlayerFacingClaimPacket = supportedNeutralizePacket(),
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      claimText: String = "This move stops the ...c5 break before Black gets counterplay."
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(mainClaim(packet, claimText)), None)),
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = truthMode,
      preventedPlansNow = Nil,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None
    )

  private def supportedQuietIntent(
      intentClass: QuietMoveIntentClass = QuietMoveIntentClass.PieceImprovement,
      anchorSquare: String = "f3",
      sourceKind: String = "piece_improvement",
      claimText: String = "This improves the knight by placing it on f3.",
      proofFamilyOverride: Option[String] = None,
      ontologyOverride: Option[PlayerFacingClaimOntologyKind] = None
  ): QuietMoveIntentClaim =
    val proofFamily = proofFamilyOverride.getOrElse(intentClass.proofFamily)
    val ontology = ontologyOverride.getOrElse(intentClass.ontologyFamily)
    val packet =
      PlayerFacingClaimPacket(
        claimGate =
          PlanEvidenceEvaluator.ClaimCertification(
            certificateStatus = PlayerFacingCertificateStatus.Valid,
            quantifier = PlayerFacingClaimQuantifier.Universal,
            modalityTier = PlayerFacingClaimModalityTier.Supports,
            attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
            stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
            provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
            ontologyFamily = ontology
          ),
        proofSource = sourceKind,
        proofFamily = proofFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        triggerKind = sourceKind,
        anchorTerms = List(anchorSquare),
        bestDefenseMove = Some("g8f6"),
        bestDefenseBranchKey = Some("g1f3|g8f6"),
        sameBranchState = PlayerFacingSameBranchState.Proven,
        persistence = PlayerFacingClaimPersistence.Stable,
        proofPathWitness =
          PlayerFacingProofPathWitness(
            ownerSeedTerms = List(anchorSquare),
            continuationTerms = List("probe_backed", "stable", "support_probe:quiet_intent"),
            structureTransitionTerms = Nil
          ),
        fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
      )
    QuietMoveIntentClaim(
      intentClass = intentClass,
      claimText = claimText,
      evidenceLine = Some(s"Line: $anchorSquare is anchored by probe-backed quiet intent."),
      sourceKind = sourceKind,
      packet = packet
    )

  private def routeNetworkInputs(
      surface: Option[RouteNetworkBindProof.SurfaceNetwork],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      heavyPieceLocalBindBlocked: Boolean = false
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      namedRouteNetworkSurface = surface
    )

  private def dualAxisInputs(
      contract: Option[TwoAxisBindProof.Contract],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic,
      heavyPieceLocalBindBlocked: Boolean = false
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      heavyPieceLocalBindBlocked = heavyPieceLocalBindBlocked,
      dualAxisBindSurface = contract
    )

  private def restrictedDefenseInputs(
      contract: Option[RestrictedDefenseConversionProof.Contract],
      truthMode: PlayerFacingTruthMode = PlayerFacingTruthMode.Strategic
  ): QuestionPlannerInputs =
    inputs().copy(
      mainBundle = None,
      truthMode = truthMode,
      restrictedDefenseConversionSurface = contract
    )

  private def certifiedRestrictedDefenseConversion(
      bestDefenseFound: Option[String] = Some("e8e7"),
      bestDefenseBranchKey: Option[String] = Some("e8e7 e1e2"),
      evidence: RestrictedDefenseConversionProof.RestrictedDefenseEvidence =
        RestrictedDefenseConversionProof.RestrictedDefenseEvidence(
          defenderResourceCount = 2,
          moveQualityCompression = true,
          counterplayScoreDrop = 125,
          preventedResourcePressure = true,
          bestReplyStable = true,
          futureSnapshotPersistence = true
        ),
      routePersistence: RestrictedDefenseConversionProof.RoutePersistence =
        RestrictedDefenseConversionProof.RoutePersistence(
          bestDefenseStable = true,
          futureSnapshotPersistent = true,
          counterplayStillCompressed = true,
          directBestDefensePresent = true,
          sameDefendedBranch = true
        ),
      failsIf: List[String] = Nil,
      moveOrderFragility: RestrictedDefenseConversionProof.MoveOrderFragility =
        RestrictedDefenseConversionProof.MoveOrderFragility(fragile = false, reasons = Nil)
  ): RestrictedDefenseConversionProof.Contract =
    RestrictedDefenseConversionProof.Contract(
      strategyHypothesis = "Convert with restricted defense",
      restrictedDefenseEvidence = evidence,
      defenderResources = List("e8e7", "e8f8"),
      bestDefenseFound = bestDefenseFound,
      bestDefenseBranchKey = bestDefenseBranchKey,
      routePersistence = routePersistence,
      failsIf = failsIf,
      moveOrderFragility = moveOrderFragility,
      confidence = if failsIf.isEmpty then 0.9 else 0.36,
      evidenceSources = List("convert_reply_multipv", "counterplay_drop:125")
    )

  private def certifiedDualAxisBind(
      breakLabel: String = "...c5",
      entryLabel: String = "b4",
      failsIf: List[String] = Nil,
      counterplayReinflationRisk: String = "bounded_dual_axis_only"
  ): TwoAxisBindProof.Contract =
    val breakAxis =
      TwoAxisBindProof.AxisDescriptor(
        kind = "break_axis",
        label = breakLabel,
        deniedResourceClass = Some("break"),
        counterplayScoreDrop = 145,
        breakNeutralizationStrength = Some(84),
        defensiveSufficiency = Some(80)
      )
    val entryAxis =
      TwoAxisBindProof.AxisDescriptor(
        kind = "entry_axis",
        label = entryLabel,
        deniedResourceClass = Some("entry_square"),
        counterplayScoreDrop = 130,
        breakNeutralizationStrength = Some(76),
        defensiveSufficiency = Some(74)
      )
    TwoAxisBindProof.Contract(
      strategyHypothesis = "Stop the break and keep the entry square closed",
      claimScope = "dual_axis_local",
      primaryAxis = Some(breakAxis),
      corroboratingAxes = List(entryAxis),
      axisIndependence =
        TwoAxisBindProof.AxisIndependence(
          primaryAxis = Some(breakLabel),
          corroboratingAxis = Some(entryLabel),
          proven = failsIf.isEmpty,
          reasons = Nil
        ),
      bindArchetype = "break_plus_entry",
      restrictionEvidence =
        TwoAxisBindProof.RestrictionEvidence(
          primaryAxis = Some(breakAxis),
          corroboratingAxes = List(entryAxis),
          primaryAxisMeasured = true,
          corroboratingAxisMeasured = true,
          restrictionDeltaMeasured = true,
          bestDefenseStable = true,
          futureSnapshotPersistence = true
        ),
      defenderResources = List("counterplay"),
      freeingResourcesRemaining = Nil,
      tacticalReleasesRemaining = Nil,
      bestDefenseFound = Some("f8e8"),
      bestDefenseBranchKey = Some("f8e8|c1c8"),
      persistenceAfterBestDefense = true,
      routeContinuity =
        TwoAxisBindProof.RouteContinuity(
          directBestDefensePresent = true,
          bestDefenseStable = true,
          futureSnapshotPersistent = true,
          convertReplyAligned = true,
          boundedContinuationVisible = true,
          sameDefendedBranch = true
        ),
      fortressRisk = false,
      moveOrderFragility = TwoAxisBindProof.MoveOrderFragility(fragile = false, reasons = Nil),
      counterplayReinflationRisk = counterplayReinflationRisk,
      claimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.RouteDenial
        ),
      failsIf = failsIf,
      confidence = if failsIf.isEmpty then 0.91 else 0.42,
      evidenceSources = List("route_denial_validation", "defense_reply_multipv")
    )

  private def safeTruthContract(playedMove: String = "c3g3"): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Best,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.SupportingVisible,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  private def topPvPracticalContext(
      fen: String,
      playedMove: String,
      playedSan: String,
      line: List[String],
      scoreCp: Int
  ) =
    ctx.copy(
      fen = fen,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      openingGoalEvaluation = None,
      engineEvidence =
        Some(
          EngineEvidence(
            depth = 18,
            variations = List(VariationLine(line, scoreCp = scoreCp, depth = 18))
          )
        )
    )

  private def topPvPracticalRows(localCtx: NarrativeContext, playedMove: String) =
    MoveReviewSupportedLocalSurfaceRows.build(
      ctx = localCtx,
      inputs = inputs().copy(mainBundle = None),
      rankedPlans = NoRankedPlans,
      truthContract = Some(safeTruthContract(playedMove))
    )

  private def neutralizePlan(
      claim: String = "This move stops the ...c5 break before Black gets counterplay.",
      proofFamily: String = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      plannerSource: String = "prevented_plan",
      namedBreak: Option[String] = Some("...c5")
  ): QuestionPlan =
    QuestionPlan(
      questionId = "q_now_counterplay_break",
      questionKind = AuthorQuestionKind.WhyNow,
      priority = 100,
      claim = claim,
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(plannerSource),
      admissibilityReasons = Nil,
      plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
      plannerSource = plannerSource,
      timingWitness =
        Some(
          QuestionPlanTimingWitness(
            proofFamily = proofFamily,
            source = plannerSource,
            namedBreak = namedBreak,
            continuationMove = Some("e4d5"),
            branchKey = Some("e4d5|c6d5"),
            witnessTokens = List("counterplay_axis_suppression")
          )
        )
    )

  private def ranked(plan: QuestionPlan): RankedQuestionPlans =
    RankedQuestionPlans(primary = Some(plan), secondary = None, rejected = Nil)

  private def centralScene(
      fen: String = MadernaExactFen,
      ply: Int = 33,
      playedMove: String = "e4e5",
      lines: List[VariationLine] = MadernaExactLines,
      phase: String = "middlegame",
      truthContract: Option[DecisiveTruthContract] = None
  ): (NarrativeContext, QuestionPlannerInputs, RankedQuestionPlans) =
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = lines,
          playedMove = Some(playedMove),
          phase = Some(phase),
          ply = ply,
          prevMove = Some(playedMove)
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val centralCtx = NarrativeContextBuilder.build(data, data.toContext, None)
    val pack = StrategyPackBuilder.build(data, centralCtx).getOrElse(fail(s"strategy pack missing for $fen"))
    val effectiveTruthContract = truthContract.orElse(Some(safeTruthContract(playedMove)))
    val centralInputs = QuestionPlannerInputsBuilder.build(centralCtx, Some(pack), effectiveTruthContract)
    val centralRanked = QuestionFirstCommentaryPlanner.plan(centralCtx, centralInputs, effectiveTruthContract)
    (centralCtx, centralInputs, centralRanked)

  test("projects a SupportedLocal neutralize_key_break timing plan into a named counterplay break row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("projects exact central_break_timing packet into a central break row") {
    val (centralCtx, centralInputs, centralRanked) = centralScene()

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the e4-e5 break at this moment."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralBreak, token = Some("e4-e5")))
    )
    assertEquals(rows.head.source, None)
    assert(!rows.exists(_.text.contains("central_break_timing")), clue(rows))
  }

  test("projects certified color-complex squeeze packet into a board-backed row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedColorComplexPacket(),
          claimText = "A minor piece keeps the color-complex pressure on e5."
        ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Color complex"))
    assertEquals(
      rows.head.text,
      "The checked line keeps the knight on c4 attacking e5 in the dark-square complex."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
    assertEquals(rows.head.source, None)
  }

  test("keeps two independent exact supported-local rows") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs =
          inputs().copy(
            mainBundle =
              Some(
                MainPathClaimBundle(
                  Some(mainClaim(supportedNeutralizePacket(), "This move stops the ...c5 break before Black gets counterplay.")),
                  Some(mainClaim(supportedColorComplexPacket(), "A minor piece keeps the color-complex pressure on e5."))
                )
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break", "Color complex"))
    assertEquals(rows.head.text, "On the checked line, this stops the ...c5 break before it appears.")
    assertEquals(
      rows(1).text,
      "The checked line keeps the knight on c4 attacking e5 in the dark-square complex."
    )
  }

  test("keeps certified typed surface row visible when packet rows fill the supported-local cap") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs =
          inputs().copy(
            mainBundle =
              Some(
                MainPathClaimBundle(
                  Some(mainClaim(supportedNeutralizePacket(), "This move stops the ...c5 break before Black gets counterplay.")),
                  Some(mainClaim(supportedColorComplexPacket(), "A minor piece keeps the color-complex pressure on e5."))
                )
              ),
            namedRouteNetworkSurface = Some(surface)
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break", "Route denial"))
    assertEquals(rows(1).text, "The checked line keeps c6 closed, takes the c-file away, and cuts off the b6 reroute.")
  }

  test("projects certified exact position-probe packets into bounded rows") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val cases =
      List(
        (
          supportedPositionProbePacket(
            proofFamily = staticWeaknessFamily,
            proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
            anchorSquare = "d6",
            ownerSeedTerms = List("d6", "fixed_target:d6", "exact_target_fixation"),
            continuationTerms = List("exact_target_fixation", "fixed_target:d6", "best_branch:f3d2|b8a6"),
            structureTransitionTerms = List("exact_target_fixation", "fixed_target:d6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
          ),
          "Fixed target",
          "The checked line keeps d6 fixed as the target."
        ),
        (
          supportedPositionProbePacket(
            proofFamily = ProofFamilyId.BackwardPawnTargeting.wireKey,
            proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
            anchorSquare = "c6",
            ownerSeedTerms = List("c6", "fixed_target:c6", "carlsbad_fixed_target_probe"),
            continuationTerms = List("carlsbad_fixed_target_probe", "fixed_target:c6", "best_branch:b4b5|a6b5"),
            structureTransitionTerms = List("carlsbad_fixed_target_probe", "fixed_target:c6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
        ),
          "Minority attack",
          "The checked line keeps c6 as the minority-attack fixed target."
        ),
        (
          supportedPositionProbePacket(
            proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
            proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
            anchorSquare = "e5",
            ownerSeedTerms = List("e5", "coordinated_target:e5", "target_focused_coordination_probe"),
            continuationTerms = List("target_focused_coordination_probe", "coordinated_target:e5"),
            structureTransitionTerms = List("target_focused_coordination_probe", "coordinated_target:e5"),
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.TargetFocusedCoordination(
                  targetSquare = "e5",
                  supportFromSquares = List("e3", "g2"),
                  targetPieces = List("target_knight")
                )
              )
          ),
          "Target coordination",
          "The checked line coordinates pressure on e5 from e3 and g2."
        )
      )

    cases.foreach { case (packet, expectedLabel, expectedText) =>
      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = ctx,
          inputs = inputs(packet = packet, claimText = expectedText),
          rankedPlans = NoRankedPlans,
          truthContract = Some(safeTruthContract())
        )

      assertEquals(rows.map(_.label), List(expectedLabel), clue(rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        clue(rows)
      )
    }
  }

  test("does not project exact position-probe proof when packet target terms mismatch") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val cases =
      List(
        supportedPositionProbePacket(
          proofFamily = staticWeaknessFamily,
          proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
          anchorSquare = "d6",
          ownerSeedTerms = List("d6", "fixed_target:e5", "exact_target_fixation"),
          continuationTerms = List("exact_target_fixation", "fixed_target:e5", "best_branch:f3d2|b8a6"),
          structureTransitionTerms = List("exact_target_fixation", "fixed_target:e5"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
        ),
        supportedPositionProbePacket(
          proofFamily = ProofFamilyId.BackwardPawnTargeting.wireKey,
          proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
          anchorSquare = "c6",
          ownerSeedTerms = List("c6", "fixed_target:c3", "carlsbad_fixed_target_probe"),
          continuationTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3", "best_branch:b4b5|a6b5"),
          structureTransitionTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
        ),
        supportedPositionProbePacket(
          proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
          proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
          anchorSquare = "e5",
          ownerSeedTerms = List("e5", "coordinated_target:d4", "target_focused_coordination_probe"),
          continuationTerms = List("target_focused_coordination_probe", "coordinated_target:d4"),
          structureTransitionTerms = List("target_focused_coordination_probe", "coordinated_target:d4"),
          exactSliceProof =
            Some(
              PlayerFacingExactSliceProof.TargetFocusedCoordination(
                targetSquare = "e5",
                supportFromSquares = List("e3", "g2"),
                targetPieces = List("target_knight")
              )
            )
        )
      )

    cases.foreach { packet =>
      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = ctx,
          inputs = inputs(packet = packet),
          rankedPlans = NoRankedPlans,
          truthContract = Some(safeTruthContract())
        )

      assertEquals(rows, Nil, clue(packet))
    }
  }

  test("does not project move-local exact target fixation as a position-probe row") {
    val staticWeaknessFamily =
      ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.StaticWeaknessFixation).map(_.wireKey).getOrElse(
        PlanTaxonomy.PlanKind.StaticWeaknessFixation.id
      )
    val moveLocal =
      supportedPositionProbePacket(
        proofFamily = staticWeaknessFamily,
        proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
        anchorSquare = "d6",
        ownerSeedTerms = List("d6", "fixed_target:d6", "exact_target_fixation"),
        continuationTerms = List("exact_target_fixation", "fixed_target:d6"),
        structureTransitionTerms = List("exact_target_fixation", "fixed_target:d6"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("d6"))
      ).copy(scope = PlayerFacingPacketScope.MoveLocal)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = moveLocal),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze terms without typed exact proof") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedColorComplexPacket(exactSliceProof = None)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze row when typed proof geometry is inconsistent") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedColorComplexPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ColorComplexSqueeze("e5", "light", "knight", "c4"))
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze proof without matching witness terms") {
    val packet = supportedColorComplexPacket()
    val strippedWitness =
      packet.proofPathWitness.copy(
        ownerSeedTerms = List("color_complex_squeeze_probe"),
        continuationTerms = List("color_complex_squeeze_probe"),
        structureTransitionTerms = List("color_complex_squeeze_probe")
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet.copy(proofPathWitness = strippedWitness)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project color-complex squeeze proof when the mirrored minor-piece role mismatches") {
    val packet = supportedColorComplexPacket()
    val mismatchedWitness =
      packet.proofPathWitness.copy(
        ownerSeedTerms =
          packet.proofPathWitness.ownerSeedTerms
            .filterNot(_.startsWith("minor_piece:")) :+ "minor_piece:bishop_c4"
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet.copy(proofPathWitness = mismatchedWitness)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses color-complex squeeze row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedColorComplexPacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects exact named route-network surface into a bounded row") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = routeNetworkInputs(Some(surface)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Route denial"))
    assertEquals(
      rows.head.text,
      "The checked line keeps c6 closed, takes the c-file away, and cuts off the b6 reroute."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("keeps broader route-chain intermediates backend-only") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "a5",
        counterplayScoreDrop = 140,
        intermediateSquare = Some("b6")
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = routeNetworkInputs(Some(surface)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses named route-network row under heavy-piece block or tactical truth mode") {
    val surface =
      RouteNetworkBindProof.SurfaceNetwork(
        file = "c-file",
        entrySquare = "c6",
        rerouteSquare = "b6",
        counterplayScoreDrop = 140
      )
    val heavyPieceBlocked =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = routeNetworkInputs(Some(surface), heavyPieceLocalBindBlocked = true),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )
    val tactical =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = routeNetworkInputs(Some(surface), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(heavyPieceBlocked, Nil)
    assertEquals(tactical, Nil)
  }

  test("projects certified dual-axis bind into a bounded break-and-entry row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = dualAxisInputs(Some(certifiedDualAxisBind())),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Break and entry"))
    assertEquals(rows.head.text, "The checked line keeps the ...c5 break shut while keeping b4 unavailable.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project uncertified or reinflation-risk dual-axis bind") {
    val uncertified =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = dualAxisInputs(Some(certifiedDualAxisBind(failsIf = List("dual_axis_burden_missing")))),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )
    val reinflationRisk =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = dualAxisInputs(Some(certifiedDualAxisBind(counterplayReinflationRisk = "high"))),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(uncertified, Nil)
    assertEquals(reinflationRisk, Nil)
  }

  test("suppresses dual-axis bind row under heavy-piece block or tactical truth mode") {
    val heavyPieceBlocked =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = dualAxisInputs(Some(certifiedDualAxisBind()), heavyPieceLocalBindBlocked = true),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )
    val tactical =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = dualAxisInputs(Some(certifiedDualAxisBind()), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(heavyPieceBlocked, Nil)
    assertEquals(tactical, Nil)
  }

  test("projects certified restricted-defense conversion into a bounded technical row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = restrictedDefenseInputs(Some(certifiedRestrictedDefenseConversion())),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Technical conversion"))
    assertEquals(rows.head.text, "The checked line keeps the conversion route intact after Ke7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project uncertified or stale restricted-defense conversion") {
    val uncertified =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs =
          restrictedDefenseInputs(
            Some(certifiedRestrictedDefenseConversion(failsIf = List("route_persistence_missing")))
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )
    val stale =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs =
          restrictedDefenseInputs(
            Some(
              certifiedRestrictedDefenseConversion(
                routePersistence =
                  RestrictedDefenseConversionProof.RoutePersistence(
                    bestDefenseStable = true,
                    futureSnapshotPersistent = false,
                    counterplayStillCompressed = true,
                    directBestDefensePresent = true,
                    sameDefendedBranch = true
                  )
              )
            )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assert(!uncertified.exists(_.label == "Technical conversion"), clue(uncertified))
    assert(!stale.exists(_.label == "Technical conversion"), clue(stale))
  }

  test("suppresses restricted-defense conversion row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs =
          restrictedDefenseInputs(
            Some(certifiedRestrictedDefenseConversion()),
            truthMode = PlayerFacingTruthMode.Tactical
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows, Nil)
  }

  test("projects exact outpost-occupation packet into a bounded row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedOutpostPacket(),
          claimText = "The move occupies the e5 outpost."
        ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Knight outpost"))
    assertEquals(rows.head.text, "The checked line puts the knight on the e5 outpost.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project outpost occupation terms without typed exact proof") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedOutpostPacket(exactSliceProof = None)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project outpost occupation proof without matching witness terms") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedOutpostPacket(
                ownerSeedTerms = List("e5", "outpost:e5", "piece:knight"),
                structureTransitionTerms = List("outpost_occupation")
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses outpost occupation row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedOutpostPacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects exact local-file entry packet into a bounded row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedLocalFilePacket(),
          claimText = "The file-entry bind keeps c6 under pressure."
        ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("File entry"))
    assertEquals(rows.head.text, "The checked line keeps pressure on c6 through the c-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project local-file entry terms without typed exact proof") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedLocalFilePacket(exactSliceProof = None)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project local-file entry proof without matching witness terms") {
    val packet = supportedLocalFilePacket()
    val strippedWitness =
      packet.proofPathWitness.copy(
        ownerSeedTerms = Nil,
        continuationTerms = List("local_file_entry_bind"),
        structureTransitionTerms = Nil
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet.copy(anchorTerms = Nil, proofPathWitness = strippedWitness)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project local-file entry proof from split file and square terms without pair witness") {
    val packet = supportedLocalFilePacket()
    val splitOnlyWitness =
      packet.proofPathWitness.copy(
        structureTransitionTerms = Nil
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet.copy(proofPathWitness = splitOnlyWitness)),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project local-file entry proof when the entry square is off the claimed file") {
    val packet =
      supportedLocalFilePacket()
        .copy(
          anchorTerms = List("c-file", "d6"),
          proofPathWitness =
            PlayerFacingProofPathWitness(
              ownerSeedTerms = List("c-file", "d6"),
              continuationTerms = List("local_file_entry_bind", "c-file", "d6", "d4d6", "c7c6"),
              structureTransitionTerms = List("file-entry:c-file:d6"),
              exactSliceProof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "d6"))
            )
        )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = packet),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses local-file entry row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedLocalFilePacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects exact IQP inducement packet into a bounded row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedIqpPacket(),
          claimText = "This sequence leaves an isolated pawn as the local target."
        ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("IQP target"))
    assertEquals(rows.head.text, "The checked line leaves d5 as an isolated pawn target.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project generic IQP terms without an induced target witness") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedIqpPacket(
                structureTransitionTerms = List("central_isolated_pawn"),
                ownerSeedTerms = List("iqp_inducement", "iqp")
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses IQP inducement row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedIqpPacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects exact simplification-window packet into a bounded row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(
          packet = supportedSimplificationPacket(),
          claimText = "This trade keeps the same local edge on d5."
        ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Simplification"))
    assertEquals(rows.head.text, "The checked line keeps the same local edge after the exchange on d5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project simplification-window prose without an exchange-square witness") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedSimplificationPacket(
                continuationTerms = List("exact_trade_continuation", "b5d5", "f7d5"),
                ownerSeedTerms = List("simplification_window", "local_edge"),
                structureTransitionTerms = List("simplification_window")
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses simplification-window row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedSimplificationPacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects exact counterplay-restraint packet into a bounded row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedCounterplayRestraintPacket()),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Counterplay restraint"))
    assertEquals(rows.head.text, "The checked line keeps the opponent's break restrained.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("does not project counterplay-restraint prose tokens without typed exact resource proof") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedCounterplayRestraintPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("counterplay window"))
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project counterplay-restraint proof without matching resource terms") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedCounterplayRestraintPacket(
                exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:route"))
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses counterplay-restraint row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedCounterplayRestraintPacket(), truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects admitted exchange ownership packets into bounded rows") {
    val cases =
      List(
        (
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms =
              List(
                "defender_trade_branch",
                "defender:c5",
                "exchange_square:d4",
                "defended_target:e5",
                "defender_removed:c5-d4",
                "target_unlocked:e5"
              ),
            exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "e5"))
          ),
          "Defender trade",
          "The checked line trades on d4 to remove the defender from c5, loosening e5."
        ),
        (
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms =
              List(
                "bad_piece_liquidation_branch",
                "bad_piece:c8",
                "exchange_square:e6",
                "bad_piece_removed:c8-e6"
              ),
            exactSliceProof = Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c8", "e6"))
          ),
          "Bad piece trade",
          "The checked line trades on e6 to clear the bad piece from c8."
        ),
        (
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
            proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
            structureTransitionTerms = List("d4c6", "d7c6", "d3d8", "e8d8", "queenless_branch", "queen_trade"),
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
              )
          ),
          "Queen trade",
          "This exchange moves the game into the queenless branch."
        )
      )

    cases.foreach { case (packet, expectedLabel, expectedText) =>
      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = ctx,
          inputs = inputs(packet = packet, claimText = expectedText),
          rankedPlans = NoRankedPlans,
          truthContract = Some(safeTruthContract())
        )

      assertEquals(rows.map(_.label), List(expectedLabel), clue(rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        clue(rows)
      )
    }
  }

  test("does not project exchange ownership labels without required witness shape") {
    val cases =
      List(
        "no defender structure" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = Nil
          ),
        "generic defender shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender_removed:c5-d4", "target_unlocked:e5")
          ),
        "generic bad-piece shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece_removed:c8-e6")
          ),
        "branch-only defender shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5")
          ),
        "branch-only bad-piece shape" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e6")
          ),
        "malformed defender square" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c-file", "exchange_square:d4", "defended_target:e5")
          ),
        "malformed bad-piece square" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e-file")
          ),
        "mismatched defender proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            structureTransitionTerms = List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "f7"))
          ),
        "mismatched bad-piece proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.BadPieceLiquidation("c8", "d5"))
          ),
        "missing defender branch" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.DefenderTrade,
            proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
            sameBranchState = PlayerFacingSameBranchState.Missing,
            structureTransitionTerms =
              List(
                "defender_trade_branch",
                "defender:c5",
                "exchange_square:d4",
                "defended_target:e5",
                "defender_removed:c5-d4",
                "target_unlocked:e5"
              )
          ),
        "unstable bad-piece branch" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.BadPieceLiquidation,
            proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
            persistence = PlayerFacingClaimPersistence.BestDefenseOnly,
            structureTransitionTerms =
              List(
                "bad_piece_liquidation_branch",
                "bad_piece:c8",
                "exchange_square:e6",
                "bad_piece_removed:c8-e6"
              )
          ),
        "missing queen proof" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
            proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
            structureTransitionTerms = Nil,
            exactSliceProof = None
          ),
        "mismatched queen terms" ->
          supportedExchangePacket(
            planKind = PlanTaxonomy.PlanKind.QueenTradeShield,
            proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
            structureTransitionTerms = List("d4c6", "d7c6", "queenless_branch"),
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8"))
              )
          )
      )

    cases.foreach { case (name, packet) =>
      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = ctx,
          inputs = inputs(packet = packet),
          rankedPlans = NoRankedPlans,
          truthContract = Some(safeTruthContract())
        )

      assertEquals(rows, Nil, clue(name -> rows))
    }
  }

  test("suppresses exchange ownership row under tactical truth mode") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs =
          inputs(
            packet =
              supportedExchangePacket(
                planKind = PlanTaxonomy.PlanKind.DefenderTrade,
                proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
                structureTransitionTerms =
                  List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5")
              ),
            truthMode = PlayerFacingTruthMode.Tactical
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("suppresses central_break_timing row when tactical truth mode vetoes SupportedLocal") {
    val truth = tacticalTruthContract(playedMove = "e4e5")
    val (centralCtx, centralInputs, centralRanked) = centralScene(truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("does not project plan-only central_break_timing review evidence") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 72, depth = 18),
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 28, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "b1c3", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
  }

  test("projects central_break_timing when the board-backed break is not best-line-exclusive") {
    val lines =
      List(
        VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 68, depth = 18),
        VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 31, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects central_break_timing without requiring a two-move branch key") {
    val lines =
      List(
        VariationLine(List("d4d5"), scoreCp = 0, depth = 18),
        VariationLine(List("b1c3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects played direct central_break_timing when top PV omits that break") {
    val lines =
      List(
        VariationLine(List("b1c3", "e8g8", "h2h3"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = DirectBreakFen, ply = 15, playedMove = "d4d5", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Central break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this also plays the d4-d5 break at this moment."
    )
  }

  test("projects diagonal central captures as central liquidation rows, not central break") {
    val diagonalCaptureFen =
      "4k3/8/8/3p4/4P3/8/8/4K3 b - - 0 1"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("e8d7"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 2, playedMove = "d5e4", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central liquidation"))
    assertEquals(rows.head.text, "The move releases central tension through d5-e4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralLiquidation, token = Some("...d5-e4")))
    )
  }

  test("prioritizes relation practical rows over central liquidation fallback") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Overloaded defender"), clue(rows))
    assert(!rows.exists(_.label == "Central liquidation"), clue(rows))
    assertEquals(rows.head.authority, Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)))
  }

  test("projects prep or challenge pawn moves as central challenge rows, not central break") {
    val prepFen =
      "rnbqkbnr/pp1ppppp/8/2p1P3/2B5/5Q2/PPPP1PPP/RNB1K1NR b KQkq - 2 4"
    val lines =
      List(
        VariationLine(List("d7d6"), scoreCp = 0, depth = 18),
        VariationLine(List("b8c6"), scoreCp = 0, depth = 18)
      )
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = prepFen, ply = 8, playedMove = "d7d6", lines = lines)

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(safeTruthContract())
      )

    assert(!rows.exists(_.label == "Central break"), clue(rows))
    assertEquals(rows.map(_.label), List("Central challenge"))
    assertEquals(rows.head.text, "The move challenges the center through d7-d6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CentralChallenge, token = Some("...d7-d6")))
    )
  }

  test("projects non-central opening pawn breaks as practical opening rows") {
    val cases =
      List(
        (
          "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
          2,
          "c7c5",
          List(VariationLine(List("c7c5", "g1f3"), scoreCp = 0, depth = 18)),
          "Sicilian c-pawn challenge is supported by the checked opening structure."
        ),
        (
          "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
          3,
          "f2f4",
          List(VariationLine(List("f2f4", "e5f4"), scoreCp = 0, depth = 18)),
          "King's Gambit f-pawn advance is on the board, but king safety still needs care."
        )
      )

    cases.foreach { case (fen, ply, playedMove, lines, expectedText) =>
      val (openingCtx, openingInputs, openingRanked) =
        centralScene(fen = fen, ply = ply, playedMove = playedMove, lines = lines, phase = "opening")

      val rows =
        MoveReviewSupportedLocalSurfaceRows.build(
          ctx = openingCtx,
          inputs = openingInputs,
          rankedPlans = openingRanked,
          truthContract = Some(safeTruthContract(playedMove))
        )

      assertEquals(rows.map(_.label), List("Opening break"), clues(playedMove, rows))
      assertEquals(rows.head.text, expectedText, clue(rows))
      assertEquals(
        rows.head.authority,
        Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
        clue(rows)
      )
    }
  }

  test("does not project opening break prose without a top-PV played-move witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        playedMove = Some("c7c5"),
        engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(List("d7d6", "g1f3"), scoreCp = 0, depth = 18)))),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Sicilian c-pawn Challenge",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("c-pawn challenge reached", "e4 contested"),
              missingEvidence = Nil,
              confidence = 0.92
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = injectedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c7c5"))
      )

    assert(!rows.exists(_.label == "Opening break"), clue(rows))
  }

  test("does not project opening break prose when the goal does not match the played pawn witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
        playedMove = Some("f2f4"),
        engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(List("f2f4", "e5f4"), scoreCp = 0, depth = 18)))),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Sicilian c-pawn Challenge",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("c-pawn challenge reached", "e4 contested"),
              missingEvidence = Nil,
              confidence = 0.92
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = injectedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f2f4"))
      )

    assert(!rows.exists(_.label == "Opening break"), clue(rows))
  }

  test("projects board-backed opening outposts as practical opening rows") {
    val dutchOutpostFen =
      "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 2 4"
    val lines =
      List(
        VariationLine(List("f6e4", "g1f3"), scoreCp = 0, depth = 18)
      )
    val (openingCtx, openingInputs, openingRanked) =
      centralScene(fen = dutchOutpostFen, ply = 8, playedMove = "f6e4", lines = lines, phase = "opening")

    assertEquals(openingCtx.openingGoalEvaluation.map(_.goalName), Some("Dutch E4 Outpost"))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = openingCtx,
        inputs = openingInputs,
        rankedPlans = openingRanked,
        truthContract = Some(safeTruthContract("f6e4"))
      )

    assertEquals(rows.map(_.label), List("Opening outpost"), clue(rows))
    assertEquals(rows.head.text, "The checked opening structure has put a knight on the e4 outpost.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project opening outpost prose without a played knight witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 2 4",
        playedMove = Some("d5d4"),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Dutch E4 Outpost",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("e4 outpost occupied"),
              missingEvidence = Nil,
              confidence = 0.9
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = injectedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("d5d4"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project opening outpost prose without a top-PV played-move witness") {
    val injectedCtx =
      ctx.copy(
        fen = "rnbqkb1r/ppp1p1pp/5n2/3p1p2/2PP4/6P1/PP2PPBP/RNBQK1NR b KQkq - 2 4",
        playedMove = Some("f6e4"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("d5d4", "g1f3"), scoreCp = 0, depth = 18))
            )
          ),
        openingGoalEvaluation =
          Some(
            OpeningGoals.Evaluation(
              goalName = "Dutch E4 Outpost",
              status = OpeningGoals.Status.Achieved,
              supportedEvidence = List("e4 outpost occupied"),
              missingEvidence = Nil,
              confidence = 0.9
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = injectedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f6e4"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV rook-pawn hook creation as a bounded practical row") {
    val flankCtx =
      ctx.copy(
        fen = "6k1/8/8/7p/8/7P/8/6K1 w - - 0 1",
        playedMove = Some("h3h4"),
        playedSan = Some("h4"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("h3h4", "g8f8"), scoreCp = 28, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = flankCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("h3h4"))
      )

    assertEquals(rows.map(_.label), List("Hook creation"), clue(rows))
    assertEquals(rows.head.text, "The checked rook-pawn move creates a flank hook on h4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV rook-pawn march as a bounded practical row") {
    val flankCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/7P/8/6K1 w - - 0 1",
        playedMove = Some("h3h4"),
        playedSan = Some("h4"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("h3h4", "g8f8"), scoreCp = 28, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = flankCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("h3h4"))
      )

    assertEquals(rows.map(_.label), List("Rook-pawn march"), clue(rows))
    assertEquals(rows.head.text, "The checked line advances the rook pawn to h4 for flank space.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook-pawn hook prose without a top-PV played-move witness") {
    val flankCtx =
      ctx.copy(
        fen = "6k1/8/8/7p/8/7P/8/6K1 w - - 0 1",
        playedMove = Some("h3h4"),
        playedSan = Some("h4"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 28, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = flankCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("h3h4"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV rook lift as a bounded practical row") {
    val liftCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1",
        playedMove = Some("h1h3"),
        playedSan = Some("Rh3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("h1h3", "g8f8"), scoreCp = 32, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = liftCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("h1h3"))
      )

    assertEquals(rows.map(_.label), List("Rook lift"), clue(rows))
    assertEquals(rows.head.text, "The checked line lifts the rook to h3 as attacking infrastructure.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook lift prose without a top-PV played-move witness") {
    val liftCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1",
        playedMove = Some("h1h3"),
        playedSan = Some("Rh3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 32, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = liftCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("h1h3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project back-rank rook shuffles as rook lift prose") {
    val shuffleCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/8/R3K3 w - - 0 1",
        playedMove = Some("a1d1"),
        playedSan = Some("Rd1"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a1d1", "g8f8"), scoreCp = 22, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = shuffleCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a1d1"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight outposts as a bounded practical row") {
    val outpostCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = Some("f3e5"),
        playedSan = Some("Ne5"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f3e5", "g8f8"), scoreCp = 42, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = outpostCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f3e5"))
      )

    assertEquals(rows.map(_.label), List("Knight outpost"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on the e5 outpost.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project knight-outpost prose without a top-PV played-move witness") {
    val outpostCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = Some("f3e5"),
        playedSan = Some("Ne5"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 42, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = outpostCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f3e5"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project unsupported knight landings as outpost prose") {
    val unsupportedCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/5N2/8/6K1 w - - 0 1",
        playedMove = Some("f3e5"),
        playedSan = Some("Ne5"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f3e5", "g8f8"), scoreCp = 28, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = unsupportedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f3e5"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project pawn-attacked knight landings as outpost prose") {
    val attackedCtx =
      ctx.copy(
        fen = "6k1/8/5p2/8/3P4/5N2/8/6K1 w - - 0 1",
        playedMove = Some("f3e5"),
        playedSan = Some("Ne5"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f3e5", "g8f8"), scoreCp = 30, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = attackedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f3e5"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV bishop-pair retention after captures as a bounded practical row") {
    val bishopPairCtx =
      ctx.copy(
        fen = "k7/5n2/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 46, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = bishopPairCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows.map(_.label), List("Bishop pair"), clue(rows))
    assertEquals(rows.head.text, "The checked capture keeps the bishop pair on the board.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project bishop-pair prose without a top-PV played-move witness") {
    val bishopPairCtx =
      ctx.copy(
        fen = "k7/5n2/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "a8b8"), scoreCp = 46, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = bishopPairCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project bishop-pair prose when the mover has only one bishop after the capture") {
    val singleBishopCtx =
      ctx.copy(
        fen = "k7/5n2/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 22, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = singleBishopCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project bishop-pair prose when both sides keep the bishop pair") {
    val mirroredPairCtx =
      ctx.copy(
        fen = "k7/1b3n1b/8/8/2B5/8/6B1/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 28, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = mirroredPairCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV opposite-colored bishop end states as a bounded practical row") {
    val oppositeBishopCtx =
      ctx.copy(
        fen = "k7/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 16, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = oppositeBishopCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows.map(_.label), List("Opposite-color bishops"), clue(rows))
    assertEquals(rows.head.text, "The checked capture leaves opposite-colored bishops on the board.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project opposite-colored bishop prose without a top-PV played-move witness") {
    val oppositeBishopCtx =
      ctx.copy(
        fen = "k7/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "a8b8"), scoreCp = 16, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = oppositeBishopCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project same-color bishop end states as opposite-colored bishop prose") {
    val sameColorBishopCtx =
      ctx.copy(
        fen = "k7/5n2/2b5/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 12, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = sameColorBishopCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project opposite-colored bishop prose while major pieces remain") {
    val majorPieceCtx =
      ctx.copy(
        fen = "k6r/5nb1/8/8/2B5/8/8/6K1 w - - 0 1",
        playedMove = Some("c4f7"),
        playedSan = Some("Bxf7+"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("c4f7", "a8b8"), scoreCp = 18, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = majorPieceCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("c4f7"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV seventh-rank rook entries as a bounded practical row") {
    val seventhCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = Some("a2a7"),
        playedSan = Some("Ra7"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a2a7", "g8f8"), scoreCp = 58, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = seventhCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a2a7"))
      )

    assertEquals(rows.map(_.label), List("Seventh-rank entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the rook on the seventh rank at a7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project seventh-rank entry prose without a top-PV played-move witness") {
    val seventhCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = Some("a2a7"),
        playedSan = Some("Ra7"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 58, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = seventhCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a2a7"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project non-seventh-rank rook moves as seventh-rank entry prose") {
    val sixthCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/8/R7/6K1 w - - 0 1",
        playedMove = Some("a2a6"),
        playedSan = Some("Ra6"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a2a6", "g8f8"), scoreCp = 42, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = sixthCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a2a6"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV rook-behind-passer entries as a bounded practical row") {
    val passerRookCtx =
      ctx.copy(
        fen = "6k1/8/1P6/8/8/R7/8/6K1 w - - 0 1",
        playedMove = Some("a3b3"),
        playedSan = Some("Rb3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a3b3", "g8f8"), scoreCp = 82, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = passerRookCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3b3"))
      )

    assertEquals(rows.map(_.label), List("Rook behind passer"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook behind the passed pawn on b6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project rook-behind-passer prose without a top-PV played-move witness") {
    val passerRookCtx =
      ctx.copy(
        fen = "6k1/8/1P6/8/8/R7/8/6K1 w - - 0 1",
        playedMove = Some("a3b3"),
        playedSan = Some("Rb3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 82, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = passerRookCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3b3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project rook-behind-passer prose when the rook is in front of the passer") {
    val frontRookCtx =
      ctx.copy(
        fen = "6k1/R7/1P6/8/8/8/8/6K1 w - - 0 1",
        playedMove = Some("a7b7"),
        playedSan = Some("Rb7"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a7b7", "g8f8"), scoreCp = 42, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = frontRookCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a7b7"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight blockades of passed pawns as a bounded practical row") {
    val blockadeCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = Some("f4d3"),
        playedSan = Some("Nd3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f4d3", "g8f8"), scoreCp = 36, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = blockadeCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f4d3"))
      )

    assertEquals(rows.map(_.label), List("Passer blockade"), clue(rows))
    assertEquals(rows.head.text, "The checked line blockades the passed pawn on d4 with the knight.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project passer-blockade prose without a top-PV played-move witness") {
    val blockadeCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = Some("f4d3"),
        playedSan = Some("Nd3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 36, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = blockadeCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f4d3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project passer-blockade prose when the pawn is not passed") {
    val blockadeCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3p1N2/8/2P5/6K1 w - - 0 1",
        playedMove = Some("f4d3"),
        playedSan = Some("Nd3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f4d3", "g8f8"), scoreCp = 20, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = blockadeCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f4d3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project knight moves away from the passer stop-square as blockade prose") {
    val blockadeCtx =
      ctx.copy(
        fen = "6k1/8/8/8/3p1N2/8/8/6K1 w - - 0 1",
        playedMove = Some("f4e2"),
        playedSan = Some("Ne2"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("f4e2", "g8f8"), scoreCp = 24, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = blockadeCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("f4e2"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV open-file major-piece entries as a bounded practical row") {
    val fileCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = Some("a3e3"),
        playedSan = Some("Re3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a3e3", "g8f8"), scoreCp = 44, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = fileCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3e3"))
      )

    assertEquals(rows.map(_.label), List("File entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook on the open e-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV semi-open-file major-piece entries as a bounded practical row") {
    val fileCtx =
      ctx.copy(
        fen = "6k1/4p3/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = Some("a3e3"),
        playedSan = Some("Re3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a3e3", "g8f8"), scoreCp = 48, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = fileCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3e3"))
      )

    assertEquals(rows.map(_.label), List("File entry"), clue(rows))
    assertEquals(rows.head.text, "The checked line places the rook on the semi-open e-file.")
  }

  test("does not project file-entry prose without a top-PV played-move witness") {
    val fileCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/R7/8/6K1 w - - 0 1",
        playedMove = Some("a3e3"),
        playedSan = Some("Re3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("g1f1", "g8f8"), scoreCp = 44, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = fileCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3e3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project file-entry prose when the destination file still has an own pawn") {
    val blockedCtx =
      ctx.copy(
        fen = "6k1/8/8/8/8/R7/4P3/6K1 w - - 0 1",
        playedMove = Some("a3e3"),
        playedSan = Some("Re3"),
        openingGoalEvaluation = None,
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations = List(VariationLine(List("a3e3", "g8f8"), scoreCp = 22, depth = 18))
            )
          )
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = blockedCtx,
        inputs = inputs().copy(mainBundle = None),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("a3e3"))
      )

    assertEquals(rows, Nil)
  }

  test("projects top-PV x-ray pressure as a bounded practical row") {
    val xrayCtx =
      topPvPracticalContext(
        fen = "k7/8/6q1/5n2/8/8/8/1B5K w - - 0 1",
        playedMove = "b1e4",
        playedSan = "Be4",
        line = List("b1e4", "a8b8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(xrayCtx, "b1e4")

    assertEquals(rows.map(_.label), List("X-ray pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line sets x-ray pressure from the bishop on e4 through f5 toward g6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project x-ray prose without a target beyond the blocker") {
    val blockedXrayCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5n2/8/8/8/1B5K w - - 0 1",
        playedMove = "b1e4",
        playedSan = "Be4",
        line = List("b1e4", "a8b8"),
        scoreCp = 42
      )

    val rows = topPvPracticalRows(blockedXrayCtx, "b1e4")

    assertEquals(rows, Nil)
  }

  test("projects top-PV back-rank mates as bounded practical rows") {
    val mateCtx =
      topPvPracticalContext(
        fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
        playedMove = "e1e8",
        playedSan = "Re8#",
        line = List("e1e8"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateCtx, "e1e8")

    assertEquals(rows.map(_.label), List("Back-rank mate"), clue(rows))
    assertEquals(rows.head.text, "The checked line ends in back-rank mate on g8 after e1e8.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project back-rank mate prose without a top-PV played-move witness") {
    val mateCtx =
      topPvPracticalContext(
        fen = "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
        playedMove = "e1e8",
        playedSan = "Re8#",
        line = List("g1f1", "e1e8"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateCtx, "e1e8")

    assertEquals(rows, Nil)
  }

  test("projects named top-PV mate patterns before broad practical rows") {
    val mateNetCtx =
      topPvPracticalContext(
        fen = "6rk/6pp/4P2N/8/8/8/8/6K1 w - - 0 1",
        playedMove = "h6f7",
        playedSan = "Nf7#",
        line = List("h6f7"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateNetCtx, "h6f7")

    assertEquals(rows.map(_.label), List("Smothered mate"), clue(rows))
    assertEquals(rows.head.text, "The checked line ends in smothered mate on h8 after h6f7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project mate-net prose without a top-PV played-move witness") {
    val mateNetCtx =
      topPvPracticalContext(
        fen = "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1",
        playedMove = "h6f7",
        playedSan = "Nf7#",
        line = List("g1f1", "h6f7"),
        scoreCp = 9999
      )

    val rows = topPvPracticalRows(mateNetCtx, "h6f7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV Greek gift entries as bounded practical rows") {
    val greekGiftCtx =
      topPvPracticalContext(
        fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1",
        playedMove = "d3h7",
        playedSan = "Bxh7+",
        line = List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(greekGiftCtx, "d3h7")

    assertEquals(rows.map(_.label), List("Greek gift"), clue(rows))
    assertEquals(rows.head.text, "The checked line starts a Greek gift sacrifice with the bishop on h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project Greek gift prose without top-PV continuation support") {
    val greekGiftCtx =
      topPvPracticalContext(
        fen = "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1",
        playedMove = "d3h7",
        playedSan = "Bxh7+",
        line = List("d3h7"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(greekGiftCtx, "d3h7")

    assertEquals(rows, Nil)
  }

  test("projects top-PV double checks as bounded practical rows") {
    val doubleCheckCtx =
      topPvPracticalContext(
        fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
        playedMove = "e4f6",
        playedSan = "Nf6++",
        line = List("e4f6"),
        scoreCp = 130
      )

    val rows = topPvPracticalRows(doubleCheckCtx, "e4f6")

    assertEquals(rows.map(_.label), List("Double check"), clue(rows))
    assertEquals(rows.head.text, "The checked line gives double check on e8 from e1 and f6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project double-check prose without a top-PV played-move witness") {
    val doubleCheckCtx =
      topPvPracticalContext(
        fen = "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
        playedMove = "e4f6",
        playedSan = "Nf6++",
        line = List("g1f1", "e4f6"),
        scoreCp = 130
      )

    val rows = topPvPracticalRows(doubleCheckCtx, "e4f6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV defender trades as bounded practical rows") {
    val defenderTradeCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          playedMove = "c1a3",
          playedSan = "Ba3",
          line = List("c1a3", "f8a3", "b3a3"),
          scoreCp = 44
        ),
        "c5"
      )

    val rows = topPvPracticalRows(defenderTradeCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Defender trade"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line trades on a3 to remove the defender from f8, loosening c5."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project defender-trade prose without a declared or structural target") {
    val defenderTradeCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "f8a3", "b3a3"),
        scoreCp = 44
      )

    val rows = topPvPracticalRows(defenderTradeCtx, "c1a3")

    assert(!rows.exists(_.label == "Defender trade"), clue(rows))
  }

  test("projects top-PV bad-piece liquidation as a bounded practical row") {
    val badPieceCtx =
      topPvPracticalContext(
        fen = "3k1b2/8/8/8/3P4/n3P3/8/2B3K1 w - - 0 1",
        playedMove = "c1a3",
        playedSan = "Bxa3",
        line = List("c1a3", "f8a3"),
        scoreCp = 22
      )

    val rows = topPvPracticalRows(badPieceCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Bad piece trade"), clue(rows))
    assertEquals(rows.head.text, "The checked line trades on a3 to clear the bad piece from c1.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project bad-piece liquidation prose for open bishop exchanges") {
    val openBishopCtx =
      topPvPracticalContext(
        fen = "5r2/4k3/8/8/3P4/2P5/8/2B3K1 w - - 0 1",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "e7f7", "a3f8", "f7f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(openBishopCtx, "c1a3")

    assert(!rows.exists(_.label == "Bad piece trade"), clue(rows))
  }

  test("projects top-PV queen trades as bounded practical rows") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("d3d8", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(queenTradeCtx, "d3d8")

    assertEquals(rows.map(_.label), List("Queen trade"), clue(rows))
    assertEquals(rows.head.text, "This exchange moves the game into the queenless branch.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project queen-trade prose without king recapture") {
    val rookRecaptureCtx =
      topPvPracticalContext(
        fen = "k2qr3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("d3d8", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(rookRecaptureCtx, "d3d8")

    assert(!rows.exists(_.label == "Queen trade"), clue(rows))
  }

  test("does not project queen-trade prose without a top-PV played-move witness") {
    val queenTradeCtx =
      topPvPracticalContext(
        fen = "3qk3/8/8/8/8/3Q4/8/6K1 w - - 0 1",
        playedMove = "d3d8",
        playedSan = "Qxd8+",
        line = List("g1f1", "e8d8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(queenTradeCtx, "d3d8")

    assertEquals(rows, Nil)
  }

  test("projects top-PV knight forks as bounded practical rows") {
    val forkCtx =
      topPvPracticalContext(
        fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
        playedMove = "d4f5",
        playedSan = "Nf5",
        line = List("d4f5"),
        scoreCp = 90
      )

    val rows = topPvPracticalRows(forkCtx, "d4f5")

    assertEquals(rows.map(_.label), List("Fork"), clue(rows))
    assertEquals(rows.head.text, "The checked line puts the knight on f5 attacking the queen on h4 and the rook on e7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project fork prose without a top-PV played-move witness") {
    val forkCtx =
      topPvPracticalContext(
        fen = "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
        playedMove = "d4f5",
        playedSan = "Nf5",
        line = List("g1f1", "d4f5"),
        scoreCp = 90
      )

    val rows = topPvPracticalRows(forkCtx, "d4f5")

    assertEquals(rows, Nil)
  }

  test("projects top-PV overloaded defenders as bounded practical rows") {
    val overloadCtx =
      topPvPracticalContext(
        fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3"),
        scoreCp = 26
      )

    val rows = topPvPracticalRows(overloadCtx, "d1d3")

    assertEquals(rows.map(_.label), List("Overloaded defender"), clue(rows))
    assertEquals(rows.head.text, "The checked line overloads the defender on f6 across d5 and h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project overload prose without a top-PV played-move witness") {
    val overloadCtx =
      topPvPracticalContext(
        fen = "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("g1f1", "d1d3"),
        scoreCp = 26
      )

    val rows = topPvPracticalRows(overloadCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV decoys as bounded practical rows") {
    val decoyCtx =
      topPvPracticalContext(
        fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "d5d3", "e2d3"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(decoyCtx, "f4d3")

    assertEquals(rows.map(_.label), List("Decoy"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line offers the knight on d3 to lure the queen from d5, then recaptures on d3."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project decoy prose without the full top-PV lure branch") {
    val decoyCtx =
      topPvPracticalContext(
        fen = "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
        playedMove = "f4d3",
        playedSan = "Nd3",
        line = List("f4d3", "a8b8"),
        scoreCp = 120
      )

    val rows = topPvPracticalRows(decoyCtx, "f4d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV deflections as bounded practical rows") {
    val deflectionCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "f8a3"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(deflectionCtx, "c1a3")

    assertEquals(rows.map(_.label), List("Deflection"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line attacks the defender on f8 from a3; after it moves, g7 loses that defense."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project deflection prose without the top-PV defender move") {
    val deflectionCtx =
      topPvPracticalContext(
        fen = "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
        playedMove = "c1a3",
        playedSan = "Ba3",
        line = List("c1a3", "d8c7"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(deflectionCtx, "c1a3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV discovered attacks as bounded practical rows") {
    val discoveredCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
        playedMove = "d3f4",
        playedSan = "Nf4",
        line = List("d3f4"),
        scoreCp = 80
      )

    val rows = topPvPracticalRows(discoveredCtx, "d3f4")

    assertEquals(rows.map(_.label), List("Discovered attack"), clue(rows))
    assertEquals(rows.head.text, "The checked line clears d3, revealing a bishop attack from b1 on h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project discovered-attack prose without a top-PV played-move witness") {
    val discoveredCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
        playedMove = "d3f4",
        playedSan = "Nf4",
        line = List("g1f1", "d3f4"),
        scoreCp = 80
      )

    val rows = topPvPracticalRows(discoveredCtx, "d3f4")

    assertEquals(rows, Nil)
  }

  test("projects top-PV hanging pieces as bounded practical rows") {
    val hangingCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5r2/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4d3",
        playedSan = "Bd3",
        line = List("c4d3"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(hangingCtx, "c4d3")

    assertEquals(rows.map(_.label), List("Hanging piece"), clue(rows))
    assertEquals(rows.head.text, "The checked line attacks the undefended rook on f5 with the bishop on d3.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project hanging-piece prose without a top-PV played-move witness") {
    val hangingCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5r2/2B5/8/8/6K1 w - - 0 1",
        playedMove = "c4d3",
        playedSan = "Bd3",
        line = List("g1f1", "c4d3"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(hangingCtx, "c4d3")

    assertEquals(rows, Nil)
  }

  test("projects target-bound trapped pieces as bounded practical rows") {
    val trappedCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1",
          playedMove = "c3g7",
          playedSan = "Bg7",
          line = List("c3g7"),
          scoreCp = 64
        ),
        "h8"
      )

    val rows = topPvPracticalRows(trappedCtx, "c3g7")

    assertEquals(rows.map(_.label), List("Trapped piece"), clue(rows))
    assertEquals(rows.head.text, "The checked line traps the rook on h8 with the bishop on g7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project trapped-piece prose without an exact focal target") {
    val trappedCtx =
      topPvPracticalContext(
        fen = "k5pr/7p/8/8/8/2B5/8/6K1 w - - 0 1",
        playedMove = "c3g7",
        playedSan = "Bg7",
        line = List("c3g7"),
        scoreCp = 64
      )

    val rows = topPvPracticalRows(trappedCtx, "c3g7")

    assertNotEquals(rows.map(_.label), List("Trapped piece"), clue(rows))
  }

  test("projects target-bound domination as bounded practical rows") {
    val dominationCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "n6k/8/B7/1K6/8/8/8/2R5 w - - 0 1",
          playedMove = "a6b7",
          playedSan = "Bb7",
          line = List("a6b7"),
          scoreCp = 42
        ),
        "a8"
      )

    val rows = topPvPracticalRows(dominationCtx, "a6b7")

    assertEquals(rows.map(_.label), List("Domination"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line dominates the knight on a8; the bishop on b7 controls b6 and c7."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects target-bound zwischenzug as a bounded practical row") {
    val zwischenzugCtx =
      withFocalTarget(
        topPvPracticalContext(
          fen = "4k3/8/8/8/3b4/5N2/8/3QK3 w - - 0 1",
          playedMove = "d1a4",
          playedSan = "Qa4+",
          line = List("d1a4"),
          scoreCp = 42
        ),
        "d4"
      )

    val rows = topPvPracticalRows(zwischenzugCtx, "d1a4")

    assertEquals(rows.map(_.label), List("Zwischenzug"), clue(rows))
    assertEquals(rows.head.text, "The checked line inserts d1a4 as a check on e8 before the recapture on d4.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("projects top-PV skewers as bounded practical rows") {
    val skewerCtx =
      topPvPracticalContext(
        fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
        playedMove = "a8a1",
        playedSan = "Ra1",
        line = List("a8a1"),
        scoreCp = 76
      )

    val rows = topPvPracticalRows(skewerCtx, "a8a1")

    assertEquals(rows.map(_.label), List("Skewer"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line lines up the rook on a1 against the queen on e1, with the rook on h1 behind it."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project skewer prose without a top-PV played-move witness") {
    val skewerCtx =
      topPvPracticalContext(
        fen = "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
        playedMove = "a8a1",
        playedSan = "Ra1",
        line = List("h8g8", "a8a1"),
        scoreCp = 76
      )

    val rows = topPvPracticalRows(skewerCtx, "a8a1")

    assertEquals(rows, Nil)
  }

  test("projects top-PV interference as a bounded practical row") {
    val interferenceCtx =
      topPvPracticalContext(
        fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "f5d6",
        playedSan = "Nd6",
        line = List("f5d6", "a8b8"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(interferenceCtx, "f5d6")

    assertEquals(rows.map(_.label), List("Interference"), clue(rows))
    assertEquals(
      rows.head.text,
      "The checked line puts the knight on d6 between the rook on d8 and the queen on d5."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project interference prose without a top-PV played-move witness") {
    val interferenceCtx =
      topPvPracticalContext(
        fen = "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
        playedMove = "f5d6",
        playedSan = "Nd6",
        line = List("g1f1", "f5d6"),
        scoreCp = 48
      )

    val rows = topPvPracticalRows(interferenceCtx, "f5d6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV battery pressure as a bounded practical row") {
    val batteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3", "a8b8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(batteryCtx, "d1d3")

    assertEquals(rows.map(_.label), List("Battery pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line forms a queen-bishop battery on the diagonal toward h7.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project battery prose without a top-PV played-move witness") {
    val batteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/8/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("g1f1", "d1d3"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(batteryCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("does not project battery prose when the target ray is blocked") {
    val blockedBatteryCtx =
      topPvPracticalContext(
        fen = "k7/7q/8/8/4P3/8/8/1B1Q2K1 w - - 0 1",
        playedMove = "d1d3",
        playedSan = "Qd3",
        line = List("d1d3", "a8b8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(blockedBatteryCtx, "d1d3")

    assertEquals(rows, Nil)
  }

  test("projects top-PV absolute pins as bounded practical rows") {
    val pinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("f8b4", "e1d1"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(pinCtx, "f8b4")

    assertEquals(rows.map(_.label), List("Pin pressure"), clue(rows))
    assertEquals(rows.head.text, "The checked line pins the knight on c3 to the king on e1.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project pin prose without a top-PV played-move witness") {
    val pinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("e8f7", "f8b4"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(pinCtx, "f8b4")

    assertEquals(rows, Nil)
  }

  test("keeps relative pins out of practical pin rows") {
    val relativePinCtx =
      topPvPracticalContext(
        fen = "4kb2/8/8/8/8/2N5/3Q4/6K1 b - - 0 1",
        playedMove = "f8b4",
        playedSan = "Bb4",
        line = List("f8b4", "g1f1"),
        scoreCp = 52
      )

    val rows = topPvPracticalRows(relativePinCtx, "f8b4")

    assertEquals(rows.map(_.label), List("X-ray pressure"), clue(rows))
    assert(!rows.exists(_.label == "Pin pressure"), clue(rows))
  }

  test("projects top-PV connected rooks as a bounded practical row") {
    val connectedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/R6R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("h1d1", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(connectedRooksCtx, "h1d1")

    assertEquals(rows.map(_.label), List("Connected rooks"), clue(rows))
    assertEquals(rows.head.text, "The checked line connects the rooks on the first rank.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project connected-rook prose without a top-PV played-move witness") {
    val connectedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/R6R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("g2f1", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(connectedRooksCtx, "h1d1")

    assertEquals(rows, Nil)
  }

  test("does not project connected-rook prose when a piece remains between the rooks") {
    val blockedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/6K1/RN5R w - - 0 1",
        playedMove = "h1d1",
        playedSan = "Rd1",
        line = List("h1d1", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(blockedRooksCtx, "h1d1")

    assertEquals(rows, Nil)
  }

  test("projects top-PV doubled rooks as a bounded practical row") {
    val doubledRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/8/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("a1a2", "g8f8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(doubledRooksCtx, "a1a2")

    assertEquals(rows.map(_.label), List("Doubled rooks"), clue(rows))
    assertEquals(rows.head.text, "The checked line doubles the rooks on the a-file.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project doubled-rook prose without a top-PV played-move witness") {
    val doubledRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/8/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("g2f1", "g8f8"),
        scoreCp = 34
      )

    val rows = topPvPracticalRows(doubledRooksCtx, "a1a2")

    assertEquals(rows, Nil)
  }

  test("does not project doubled-rook prose when a piece remains between the rooks") {
    val blockedRooksCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/R7/N7/6K1/R7 w - - 0 1",
        playedMove = "a1a2",
        playedSan = "Ra2",
        line = List("a1a2", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(blockedRooksCtx, "a1a2")

    assertEquals(rows, Nil)
  }

  test("projects top-PV connected passers as a bounded practical row") {
    val connectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 68
      )

    val rows = topPvPracticalRows(connectedCtx, "e4e5")

    assertEquals(rows.map(_.label), List("Connected passers"), clue(rows))
    assertEquals(rows.head.text, "The checked line leaves connected passers on d5 and e5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project connected-passer prose without a top-PV played-move witness") {
    val connectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("g1f1", "g8f8"),
        scoreCp = 68
      )

    val rows = topPvPracticalRows(connectedCtx, "e4e5")

    assertEquals(rows, Nil)
  }

  test("does not project disconnected pawn advances as connected-passer prose") {
    val disconnectedCtx =
      topPvPracticalContext(
        fen = "6k1/8/3p4/8/4P3/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(disconnectedCtx, "e4e5")

    assertEquals(rows, Nil)
  }

  test("does not project connected-passer prose when the opponent also has a passer") {
    val counterPasserCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/3P4/4P2p/8/8/6K1 w - - 0 1",
        playedMove = "e4e5",
        playedSan = "e5",
        line = List("e4e5", "g8f8"),
        scoreCp = 32
      )

    val rows = topPvPracticalRows(counterPasserCtx, "e4e5")

    assert(!rows.exists(_.label == "Connected passers"), clue(rows))
  }

  test("projects top-PV outside passers as a bounded practical row") {
    val outsideCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "g8f8"),
        scoreCp = 54
      )

    val rows = topPvPracticalRows(outsideCtx, "a4a5")

    assertEquals(rows.map(_.label), List("Outside passer"), clue(rows))
    assertEquals(rows.head.text, "The checked line leaves an outside passer on a5.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project outside-passer prose without a top-PV played-move witness") {
    val outsideCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("g1f1", "g8f8"),
        scoreCp = 54
      )

    val rows = topPvPracticalRows(outsideCtx, "a4a5")

    assertEquals(rows, Nil)
  }

  test("does not project lone flank passers as outside-passer prose") {
    val loneFlankCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "g8f8"),
        scoreCp = 24
      )

    val rows = topPvPracticalRows(loneFlankCtx, "a4a5")

    assert(!rows.exists(_.label == "Outside passer"), clue(rows))
  }

  test("does not project front-blocked flank passers as outside-passer prose") {
    val blockedOutsideCtx =
      topPvPracticalContext(
        fen = "k7/8/8/5P2/P7/8/8/6K1 w - - 0 1",
        playedMove = "a4a5",
        playedSan = "a5",
        line = List("a4a5", "a8b8"),
        scoreCp = 20
      )

    val rows = topPvPracticalRows(blockedOutsideCtx, "a4a5")

    assert(!rows.exists(_.label == "Outside passer"), clue(rows))
  }

  test("projects top-PV passed-pawn advances as a bounded practical row") {
    val passerCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("b5b6", "g8f8"),
        scoreCp = 74
      )

    val rows = topPvPracticalRows(passerCtx, "b5b6")

    assertEquals(rows.map(_.label), List("Passed pawn advance"), clue(rows))
    assertEquals(rows.head.text, "The checked line advances the passed pawn to b6.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project passed-pawn prose without a top-PV played-move witness") {
    val passerCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("g1f1", "g8f8"),
        scoreCp = 74
      )

    val rows = topPvPracticalRows(passerCtx, "b5b6")

    assertEquals(rows, Nil)
  }

  test("does not project passed-pawn prose when the advanced pawn is still stoppable by enemy pawns") {
    val blockedCtx =
      topPvPracticalContext(
        fen = "6k1/2p5/8/1P6/8/8/8/6K1 w - - 0 1",
        playedMove = "b5b6",
        playedSan = "b6",
        line = List("b5b6", "g8f8"),
        scoreCp = 24
      )

    val rows = topPvPracticalRows(blockedCtx, "b5b6")

    assertEquals(rows, Nil)
  }

  test("projects top-PV endgame king activation as a bounded practical row") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1d2",
        playedSan = "Kd2",
        line = List("e1d2", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(kingCtx, "e1d2")

    assertEquals(rows.map(_.label), List("King activation"), clue(rows))
    assertEquals(rows.head.text, "The checked line activates the king on d2 for the endgame.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan)),
      clue(rows)
    )
  }

  test("does not project king activation without a top-PV played-move witness") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1d2",
        playedSan = "Kd2",
        line = List("g1f1", "g8f8"),
        scoreCp = 18
      )

    val rows = topPvPracticalRows(kingCtx, "e1d2")

    assertEquals(rows, Nil)
  }

  test("does not project king activation when the king does not move closer to the center") {
    val kingCtx =
      topPvPracticalContext(
        fen = "6k1/8/8/8/8/8/8/4K3 w - - 0 1",
        playedMove = "e1f1",
        playedSan = "Kf1",
        line = List("e1f1", "g8f8"),
        scoreCp = 12
      )

    val rows = topPvPracticalRows(kingCtx, "e1f1")

    assertEquals(rows, Nil)
  }

  test("suppresses practical central rows when tactical truth mode vetoes support") {
    val diagonalCaptureFen =
      "r2q1rk1/pp2npb1/2p1b1pp/3pB3/3PN3/1BP2N2/PP3PPP/R2Q1RK1 b - - 0 15"
    val lines =
      List(
        VariationLine(List("d5e4"), scoreCp = 0, depth = 18),
        VariationLine(List("g7e5"), scoreCp = 0, depth = 18)
      )
    val truth = tacticalTruthContract(playedMove = "d5e4")
    val (centralCtx, centralInputs, centralRanked) =
      centralScene(fen = diagonalCaptureFen, ply = 30, playedMove = "d5e4", lines = lines, truthContract = Some(truth))

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = centralCtx,
        inputs = centralInputs,
        rankedPlans = centralRanked,
        truthContract = Some(truth)
      )

    assertEquals(rows, Nil)
  }

  test("projects probe-backed quiet piece improvement as a bounded practical row") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent())
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("g1f3"))
      )

    assertEquals(rows.map(_.label), List("Piece improvement"), clue(rows))
    assertEquals(rows.head.text, "The checked move improves the knight by placing it on f3.")
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("projects legal castling quiet intent as king-safety practical support") {
    val castleCtx =
      ctx.copy(
        fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
        playedMove = Some("e1g1"),
        playedSan = Some("O-O")
      )
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = castleCtx,
        inputs =
          inputs().copy(
            mainBundle = None,
            quietIntent =
              Some(
                supportedQuietIntent(
                  intentClass = QuietMoveIntentClass.KingSafety,
                  anchorSquare = "g1",
                  sourceKind = "castle_short",
                  claimText = "This castles to keep the king safer."
                )
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e1g1"))
      )

    assertEquals(rows.map(_.label), List("King safety"), clue(rows))
    assertEquals(rows.head.text, "The checked move castles to improve king safety.")
  }

  test("does not project quiet intent when the legal move misses the anchored square") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent(anchorSquare = "c4"))
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("g1f3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project quiet intent without a square anchor") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs().copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent(anchorSquare = ""))
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("g1f3"))
      )

    assertEquals(rows, Nil)
  }

  test("does not project quiet intent when packet family does not match intent class") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs().copy(
            mainBundle = None,
            quietIntent =
              Some(
                supportedQuietIntent(
                  proofFamilyOverride = Some(ProofFamilyId.KingSafety.wireKey),
                  ontologyOverride = Some(PlayerFacingClaimOntologyKind.KingSafety)
                )
              )
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("g1f3"))
      )

    assertEquals(rows, Nil)
  }

  test("suppresses quiet practical rows when tactical truth mode vetoes support") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = quietKnightCtx,
        inputs =
          inputs(truthMode = PlayerFacingTruthMode.Tactical).copy(
            mainBundle = None,
            quietIntent = Some(supportedQuietIntent())
          ),
        rankedPlans = NoRankedPlans,
        truthContract = Some(tacticalTruthContract("g1f3"))
      )

    assertEquals(rows, Nil)
  }

  test("suppresses neutralize_key_break row when tactical truth mode vetoes SupportedLocal") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(truthMode = PlayerFacingTruthMode.Tactical),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects an admitted neutralize_key_break packet claim when no timing plan is selected") {
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the ...c5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("...c5")))
    )
    assertEquals(rows.head.source, None)
  }

  test("does not project tokenless neutralize_key_break packets") {
    val tokenless =
      supportedNeutralizePacket(
        anchorTerms = Nil,
        ownerSeedTerms = List("neutralize_key_break", "counterplay_axis_suppression"),
        structureTransitionTerms = Nil,
        bestDefenseMove = None,
        exactSliceProof = None
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = tokenless),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not recover fake break tokens from packet terms without typed proof") {
    val termOnly =
      supportedNeutralizePacket(
        anchorTerms = List("...c5"),
        ownerSeedTerms = List("neutralize_key_break", "...c5"),
        structureTransitionTerms = List("...c5"),
        exactSliceProof = None
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = termOnly),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project neutralize_key_break proof without matching break terms") {
    val mismatched =
      supportedNeutralizePacket(
        anchorTerms = List("...c5"),
        ownerSeedTerms = List("neutralize_key_break", "...c5"),
        structureTransitionTerms = List("...c5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...e5"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = mismatched),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not project single-square break tokens that collide with the played move") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not use SAN-only played move text as collision authority") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("g4"),
        ownerSeedTerms = List("neutralize_key_break", "g4"),
        structureTransitionTerms = List("g4"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = sanOnlyCollisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("does not replace a played-move collision with a later anchor square") {
    val collisionPacket =
      supportedNeutralizePacket(
        anchorTerms = List("c6"),
        ownerSeedTerms = List("neutralize_key_break", "g4", "c6"),
        structureTransitionTerms = List("g4", "c6"),
        bestDefenseMove = Some("g4"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("g4"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = collisionCtx,
        inputs = inputs(packet = collisionPacket),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows, Nil)
  }

  test("projects route-shaped break tokens even when the played move occupies the destination square") {
    val routePacket =
      supportedNeutralizePacket(
        anchorTerms = List("e4-e5"),
        ownerSeedTerms = List("neutralize_key_break", "e4-e5"),
        structureTransitionTerms = List("e4-e5"),
        bestDefenseMove = Some("e4e5"),
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("e4-e5"))
      )

    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = playedDestinationRouteCtx,
        inputs = inputs(packet = routePacket),
        rankedPlans = NoRankedPlans,
        truthContract = Some(safeTruthContract())
      )

    assertEquals(rows.map(_.label), List("Counterplay break"))
    assertEquals(
      rows.head.text,
      "On the checked line, this stops the e4-e5 break before it appears."
    )
    assertEquals(
      rows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("e4-e5")))
    )
  }

  test("does not project plan plus packet neutralize rows when typed proof token differs from plan witness") {
    val mismatched =
      supportedNeutralizePacket(
        exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...e5"))
      )

    val decision =
      NeutralizeKeyBreakSurfaceGate.decideForPlanPacket(
        plan = neutralizePlan(namedBreak = Some("...c5")),
        packet = mismatched,
        ctx = ctx
      )

    assertEquals(decision.token, None)
    assertEquals(decision.rejectReason, Some(NeutralizeKeyBreakSurfaceGate.MissingNamedBreak))
  }

  test("ignores source mismatch release risk and other proof families") {
    val sourceMismatch =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofSource = "wrong_source")),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val releaseRisk =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(releaseRisks = List(PlayerFacingClaimReleaseRisk.RivalRelease))),
        rankedPlans = ranked(neutralizePlan()),
        truthContract = Some(safeTruthContract())
      )
    val otherFamily =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = ctx,
        inputs = inputs(packet = supportedNeutralizePacket(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        rankedPlans = ranked(neutralizePlan(proofFamily = ProofFamilyId.CounterplayRestraint.wireKey)),
        truthContract = Some(safeTruthContract())
      )

    assertEquals(sourceMismatch, Nil)
    assertEquals(releaseRisk, Nil)
    assertEquals(otherFamily, Nil)
  }

  test("released text comes from the named witness token rather than raw claim prose") {
    val raw =
      "The strategic point is that neutralize_key_break uses counterplay_axis_suppression on e4d5|c6d5."
    val rows =
      MoveReviewSupportedLocalSurfaceRows.build(
        ctx = neutralizeCtx,
        inputs = inputs(claimText = raw),
        rankedPlans = ranked(neutralizePlan(claim = raw)),
        truthContract = Some(safeTruthContract("e2e3"))
      )

    assertEquals(
      rows.headOption.map(_.text),
      Some("On the checked line, this stops the ...c5 break before it appears.")
    )
    assert(!rows.exists(_.text.contains("neutralize_key_break")), clue(rows))
    assert(!rows.exists(_.text.contains("counterplay_axis_suppression")), clue(rows))
    assert(!rows.exists(_.text.contains("e4d5|c6d5")), clue(rows))
  }

  private def tacticalTruthContract(playedMove: String): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(playedMove),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 280,
      swingSeverity = 280,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )
